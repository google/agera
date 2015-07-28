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

import com.google.android.agera.BaseObservable.Worker;
import com.google.android.agera.Observables.LowPassFilterObservable;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

final class Common {
  static final Function IDENTITY_FUNCTION = new IdentityFunction();
  static final StaticCondicate TRUE_CONDICATE = new StaticCondicate(true);
  static final StaticCondicate FALSE_CONDICATE = new StaticCondicate(false);

  private static final ThreadLocal<WeakReference<WorkerHandler>> handlers = new ThreadLocal<>();

  @NonNull
  static WorkerHandler workerHandler() {
    final WeakReference<WorkerHandler> handlerReference = handlers.get();
    WorkerHandler handler = handlerReference != null ? handlerReference.get() : null;
    if (handler == null) {
      handler = new WorkerHandler();
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

  private static final class StaticCondicate implements Condition, Predicate {
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

  /**
   * Shared per-thread worker Handler behind internal logic of various Agera classes.
   */
  static final class WorkerHandler extends Handler {
    static final int MSG_FIRST_ADDED = 0;
    static final int MSG_LAST_REMOVED = 1;
    static final int MSG_UPDATE = 2;
    static final int MSG_CALL_UPDATABLE = 3;
    static final int MSG_CALL_MAYBE_START_FLOW = 4;
    static final int MSG_CALL_ACKNOWLEDGE_CANCEL = 5;
    static final int MSG_CALL_LOW_PASS_UPDATE = 6;

    @Override
    public void handleMessage(final Message message) {
      switch (message.what) {
        case MSG_UPDATE:
          ((Worker) message.obj).sendUpdate();
          break;
        case MSG_FIRST_ADDED:
          ((Worker) message.obj).callFirstUpdatableAdded();
          break;
        case MSG_LAST_REMOVED:
          ((Worker) message.obj).callLastUpdatableRemoved();
          break;
        case MSG_CALL_UPDATABLE:
          ((Updatable) message.obj).update();
          break;
        case MSG_CALL_MAYBE_START_FLOW:
          ((CompiledRepository) message.obj).maybeStartFlow();
          break;
        case MSG_CALL_ACKNOWLEDGE_CANCEL:
          ((CompiledRepository) message.obj).acknowledgeCancel();
          break;
        case MSG_CALL_LOW_PASS_UPDATE:
          ((LowPassFilterObservable) message.obj).lowPassUpdate();
          break;
        default:
      }
    }
  }

  private Common() {}
}
