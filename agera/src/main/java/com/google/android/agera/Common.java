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

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

final class Common {
  static final Function IDENTITY_FUNCTION = new IdentityFunction();
  static final StaticCondicate TRUE_CONDICATE = new StaticCondicate(true);
  static final StaticCondicate FALSE_CONDICATE = new StaticCondicate(false);
  private static final ThreadLocal<WeakReference<UpdateDispatcherHandler>> handlers =
      new ThreadLocal<>();

  @NonNull
  static UpdateDispatcherHandler getHandler() {
    WeakReference<UpdateDispatcherHandler> handlerReference = handlers.get();
    UpdateDispatcherHandler handler = handlerReference != null ? handlerReference.get() : null;
    if (handler == null) {
      handler = new UpdateDispatcherHandler();
      handlers.set(new WeakReference<>(handler));
    }
    return handler;
  }

  private static final class IdentityFunction implements Function {
    @NonNull
    @Override
    public Object apply(@NonNull final Object from) {
      return from;
    }
  }

  static final class StaticCondicate implements Condition, Predicate {
    private final boolean staticValue;

    private StaticCondicate(final boolean staticValue) {
      this.staticValue = staticValue;
    }

    @Override
    public boolean apply(@NonNull final Object value) {
      return staticValue;
    }

    @Override
    public boolean applies() {
      return staticValue;
    }
  }

  static final class StaticProducer<TFirst, TSecond, TTo>
      implements Supplier<TTo>, Function<TFirst, TTo>, Merger<TFirst, TSecond, TTo> {
    @NonNull
    private final TTo staticValue;

    StaticProducer(@NonNull final TTo staticValue) {
      this.staticValue = checkNotNull(staticValue);
    }

    @NonNull
    @Override
    public TTo apply(@NonNull final TFirst input) {
      return staticValue;
    }

    @NonNull
    @Override
    public TTo merge(@NonNull final TFirst o, @NonNull final TSecond o2) {
      return staticValue;
    }

    @NonNull
    @Override
    public TTo get() {
      return staticValue;
    }
  }

  static final class AsyncUpdateDispatcher implements UpdateDispatcher {
    private static final Object[] NO_UPDATABLES_OR_HANDLERS = new Object[0];

    @Nullable
    private final UpdatablesChanged updatablesChanged;
    @NonNull
    private final UpdateDispatcherHandler handler;

    @NonNull
    private Object[] updatablesAndHandlers;
    private int size;

    AsyncUpdateDispatcher(@Nullable final UpdatablesChanged updatablesChanged) {
      checkState(Looper.myLooper() != null, "Can only be created on a Looper thread");
      this.updatablesChanged = updatablesChanged;
      this.handler = getHandler();
      this.updatablesAndHandlers = NO_UPDATABLES_OR_HANDLERS;
      this.size = 0;
    }

    @Override
    public synchronized void addUpdatable(@NonNull final Updatable updatable) {
      checkState(Looper.myLooper() != null, "Can only be added on a Looper thread");
      add(updatable, getHandler());
      if (size == 1) {
        handler.sendAdded(this);
      }
    }

    @Override
    public synchronized void removeUpdatable(@NonNull final Updatable updatable) {
      remove(updatable);
      if (size == 0) {
        handler.sendRemoved(this);
      }
    }

    @Override
    public void update() {
      handler.sendUpdate(this);
    }

    private void add(final Updatable updatable, final Handler handler) {
      boolean added = false;
      for (int index = 0; index < updatablesAndHandlers.length; index += 2) {
        if (updatablesAndHandlers[index] == updatable) {
          throw new IllegalStateException("Updatable already added, cannot add.");
        }
        if (updatablesAndHandlers[index] == null && !added) {
          updatablesAndHandlers[index] = updatable;
          updatablesAndHandlers[index + 1] = handler;
          added = true;
        }
      }
      if (!added) {
        final int newIndex = updatablesAndHandlers.length;
        updatablesAndHandlers = Arrays.copyOf(updatablesAndHandlers,
            Math.max(newIndex * 2, newIndex + 2));
        updatablesAndHandlers[newIndex] = updatable;
        updatablesAndHandlers[newIndex + 1] = handler;
      }
      size++;
    }

    private void remove(final Updatable updatable) {
      for (int index = 0; index < updatablesAndHandlers.length; index += 2) {
        if (updatablesAndHandlers[index] == updatable) {
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
        final UpdateDispatcherHandler handler =
            (UpdateDispatcherHandler) updatablesAndHandlers[index + 1];
        if (updatable != null) {
          if (handler.getLooper() == Looper.myLooper()) {
            updatable.update();
          } else {
            handler.callUpdatable(updatable);
          }
        }
      }
    }

    void callFirstUpdatableAdded() {
      if (updatablesChanged != null) {
        updatablesChanged.firstUpdatableAdded(this);
      }
    }

    void callLastUpdatableRemoved() {
      if (updatablesChanged != null) {
        updatablesChanged.lastUpdatableRemoved(this);
      }
    }
  }

  private static final class UpdateDispatcherHandler extends Handler implements Executor {
    public static final int MSG_FIRST_ADDED = 0;
    public static final int MSG_LAST_REMOVED = 1;
    public static final int MSG_UPDATE = 2;
    public static final int MSG_CALL_UPDATABLE = 3;

    void sendUpdate(AsyncUpdateDispatcher asyncUpdateDispatcher) {
      obtainMessage(MSG_UPDATE, asyncUpdateDispatcher).sendToTarget();
    }

    void sendAdded(AsyncUpdateDispatcher asyncUpdateDispatcher) {
      obtainMessage(MSG_FIRST_ADDED, asyncUpdateDispatcher).sendToTarget();
    }

    void sendRemoved(AsyncUpdateDispatcher asyncUpdateDispatcher) {
      obtainMessage(MSG_LAST_REMOVED, asyncUpdateDispatcher).sendToTarget();
    }

    void callUpdatable(Updatable updatable) {
      obtainMessage(MSG_CALL_UPDATABLE, updatable).sendToTarget();
    }

    @Override
    public void handleMessage(final Message message) {
      switch (message.what) {
        case MSG_UPDATE:
          ((AsyncUpdateDispatcher) message.obj).sendUpdate();
          break;
        case MSG_FIRST_ADDED:
          ((AsyncUpdateDispatcher) message.obj).callFirstUpdatableAdded();
          break;
        case MSG_LAST_REMOVED:
          ((AsyncUpdateDispatcher) message.obj).callLastUpdatableRemoved();
          break;
        case MSG_CALL_UPDATABLE:
          ((Updatable) message.obj).update();
          break;
        default:
      }
    }

    @Override
    public void execute(@NonNull final Runnable command) {
      if (!post(command)) {
        throw new RejectedExecutionException();
      }
    }
  }

  private Common() {}
}
