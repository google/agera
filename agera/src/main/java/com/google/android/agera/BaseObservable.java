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

import static android.os.SystemClock.elapsedRealtime;
import static com.google.android.agera.Preconditions.checkState;
import static com.google.android.agera.WorkerHandler.MSG_LAST_REMOVED;
import static com.google.android.agera.WorkerHandler.MSG_UPDATE;
import static com.google.android.agera.WorkerHandler.workerHandler;

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
  private static final Object[] NO_UPDATABLES_OR_HANDLERS = new Object[0];
  @NonNull
  private final WorkerHandler handler;
  private final int shortestUpdateWindowMillis;

  @NonNull
  private Object[] updatablesAndHandlers;
  private int size;
  private long lastUpdateTimestamp;

  protected BaseObservable() {
    this(0);
  }

  BaseObservable(final int shortestUpdateWindowMillis) {
    checkState(Looper.myLooper() != null, "Can only be created on a Looper thread");
    this.shortestUpdateWindowMillis = shortestUpdateWindowMillis;
    this.handler = workerHandler();
    this.updatablesAndHandlers = NO_UPDATABLES_OR_HANDLERS;
    this.size = 0;
  }

  @Override
  public synchronized final void addUpdatable(@NonNull final Updatable updatable) {
    checkState(Looper.myLooper() != null, "Can only be added on a Looper thread");
    add(updatable, workerHandler());
    if (size == 1) {
      if (handler.hasMessages(MSG_LAST_REMOVED, this)) {
        handler.removeMessages(MSG_LAST_REMOVED, this);
      } else {
        handler.obtainMessage(WorkerHandler.MSG_FIRST_ADDED, this).sendToTarget();
      }
    }
  }

  @Override
  public synchronized final void removeUpdatable(@NonNull final Updatable updatable) {
    checkState(Looper.myLooper() != null, "Can only be removed on a Looper thread");
    remove(updatable);
    if (size == 0) {
      handler.obtainMessage(MSG_LAST_REMOVED, this).sendToTarget();
      handler.removeMessages(MSG_UPDATE, this);
    }
  }

  /**
   * Notifies all registered {@link Updatable}s.
   */
  protected final void dispatchUpdate() {
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
    handler.removeMessages(WorkerHandler.MSG_UPDATE, this);
    final long elapsedRealtimeMillis =
        shortestUpdateWindowMillis > 0 ? elapsedRealtime() : 0;
    final long timeFromLastUpdate = elapsedRealtimeMillis - lastUpdateTimestamp;
    if (timeFromLastUpdate >= shortestUpdateWindowMillis) {
      lastUpdateTimestamp = elapsedRealtimeMillis;
      for (int index = 0; index < updatablesAndHandlers.length; index = index + 2) {
        final Updatable updatable = (Updatable) updatablesAndHandlers[index];
        final WorkerHandler handler =
            (WorkerHandler) updatablesAndHandlers[index + 1];
        if (updatable != null) {
          if (handler.getLooper() == Looper.myLooper()) {
            handler.removeMessages(WorkerHandler.MSG_CALL_UPDATABLE, updatable);
            updatable.update();
          } else if (!handler.hasMessages(WorkerHandler.MSG_CALL_UPDATABLE, updatable)) {
            handler.obtainMessage(WorkerHandler.MSG_CALL_UPDATABLE, updatable).sendToTarget();
          }
        }
      }
    } else {
      handler.sendMessageDelayed(handler.obtainMessage(WorkerHandler.MSG_UPDATE, this),
          shortestUpdateWindowMillis - timeFromLastUpdate);
    }
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
}
