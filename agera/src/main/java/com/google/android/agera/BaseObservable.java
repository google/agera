/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.agera;

import static com.google.android.agera.WorkerHandler.MSG_LAST_REMOVED;
import static com.google.android.agera.WorkerHandler.MSG_UPDATE;
import static com.google.android.agera.WorkerHandler.workerHandler;
import static com.google.android.agera.Preconditions.checkState;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * A partial implementation of {@link Observable} that adheres to the threading contract between
 * {@link Observable}s and {@link Updatable}s. Subclasses can use {@link #observableActivated()} and
 * {@link #observableDeactivated()} to control the activation and deactivation of this observable,
 * and to send out notifications to client updatables with {@link #dispatchUpdate()}.
 *
 * <p>For cases where subclassing {@link BaseObservable} is impossible, for example when the
 * potential class already has a base class, consider using {@link Observables#updateDispatcher()}
 * to help implement the {@link Observable} interface.
 */
public abstract class BaseObservable implements Observable {
  @NonNull
  private final Worker worker;

  protected BaseObservable() {
    checkState(Looper.myLooper() != null, "Can only be created on a Looper thread");
    worker = new Worker(this);
  }

  @Override
  public final void addUpdatable(@NonNull final Updatable updatable) {
    checkState(Looper.myLooper() != null, "Can only be added on a Looper thread");
    worker.addUpdatable(updatable);
  }

  @Override
  public final void removeUpdatable(@NonNull final Updatable updatable) {
    checkState(Looper.myLooper() != null, "Can only be removed on a Looper thread");
    worker.removeUpdatable(updatable);
  }

  /**
   * Notifies all registered {@link Updatable}s.
   */
  protected final void dispatchUpdate() {
    worker.dispatchUpdate();
  }

  /**
   * Called from the worker looper thread when this {@link Observable} is activated by transitioning
   * from having no client {@link Updatable}s to having at least one client {@link Updatable}.
   */
  protected void observableActivated() {}

  /**
   * Called from the worker looper thread when this {@link Observable} is deactivated by
   * transitioning from having at least one client {@link Updatable} to having no client
   * {@link Updatable}s.
   */
  protected void observableDeactivated() {}

  /**
   * Worker and synchronization lock behind a {@link BaseObservable}.
   */
  static final class Worker {
    @NonNull
    private static final Object[] NO_UPDATABLES_OR_HANDLERS = new Object[0];

    @NonNull
    private final BaseObservable baseObservable;
    @NonNull
    private final WorkerHandler handler;

    @NonNull
    private Object[] updatablesAndHandlers;
    private int size;

    Worker(@NonNull final BaseObservable baseObservable) {
      this.baseObservable = baseObservable;
      this.handler = workerHandler();
      this.updatablesAndHandlers = NO_UPDATABLES_OR_HANDLERS;
      this.size = 0;
    }

    synchronized void addUpdatable(@NonNull final Updatable updatable) {
      add(updatable, workerHandler());
      if (size == 1) {
        if (handler.hasMessages(MSG_LAST_REMOVED, this)) {
          handler.removeMessages(MSG_LAST_REMOVED, this);
        } else {
          handler.obtainMessage(WorkerHandler.MSG_FIRST_ADDED, this).sendToTarget();
        }
      }
    }

    synchronized void removeUpdatable(@NonNull final Updatable updatable) {
      remove(updatable);
      if (size == 0) {
        handler.obtainMessage(MSG_LAST_REMOVED, this).sendToTarget();
      }
    }

    void dispatchUpdate() {
      if (!handler.hasMessages(MSG_UPDATE, this)) {
        handler.obtainMessage(MSG_UPDATE, this).sendToTarget();
      }
    }

    private void add(@NonNull final Updatable updatable, @NonNull final Handler handler) {
      int indexToAdd = -1;
      for (int index = 0; index < updatablesAndHandlers.length; index += 2) {
        if (updatablesAndHandlers[index] == updatable) {
          throw new IllegalStateException("Updatable already added, cannot add.");
        }
        if (updatablesAndHandlers[index] == null) {
          indexToAdd = index;
        }
      }
      if (indexToAdd == -1) {
        indexToAdd = updatablesAndHandlers.length;
        updatablesAndHandlers = Arrays.copyOf(updatablesAndHandlers,
            indexToAdd < 2 ? 2 : indexToAdd * 2);
      }
      updatablesAndHandlers[indexToAdd] = updatable;
      updatablesAndHandlers[indexToAdd + 1] = handler;
      size++;
    }

    private void remove(@NonNull final Updatable updatable) {
      for (int index = 0; index < updatablesAndHandlers.length; index += 2) {
        if (updatablesAndHandlers[index] == updatable) {
          ((WorkerHandler) updatablesAndHandlers[index + 1]).removeMessages(
              WorkerHandler.MSG_CALL_UPDATABLE, updatable);
          updatablesAndHandlers[index] = null;
          updatablesAndHandlers[index + 1] = null;
          size--;
          return;
        }
      }
      throw new IllegalStateException("Updatable not added, cannot remove.");
    }

    synchronized void sendUpdate() {
      for (int index = 0; index < updatablesAndHandlers.length; index = index + 2) {
        final Updatable updatable = (Updatable) updatablesAndHandlers[index];
        final WorkerHandler handler =
            (WorkerHandler) updatablesAndHandlers[index + 1];
        if (updatable != null) {
          if (handler.getLooper() == Looper.myLooper()) {
            updatable.update();
          } else if (!handler.hasMessages(WorkerHandler.MSG_CALL_UPDATABLE, updatable)) {
            handler.obtainMessage(WorkerHandler.MSG_CALL_UPDATABLE, updatable).sendToTarget();
          }
        }
      }
    }

    void callFirstUpdatableAdded() {
      baseObservable.observableActivated();
    }

    void callLastUpdatableRemoved() {
      baseObservable.observableDeactivated();
    }
  }
}
