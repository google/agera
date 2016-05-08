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
import static com.google.android.agera.WorkerHandler.workerHandler;

import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for obtaining {@link Observable} instances.
 *
 * <p>Any {@link Observable} created by this class have to be created from a {@link Looper} thread
 * or they will throw an {@link IllegalStateException}
 *
 * <p>{@link UpdateDispatcher}s created by this class will for any injected
 * {@link ActivationHandler} call {@link ActivationHandler#observableActivated(UpdateDispatcher)}
 * and {@link ActivationHandler#observableDeactivated(UpdateDispatcher)} on the thread the
 * {@link UpdateDispatcher} was created on.
 */
public final class Observables {

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that any of the
   * {@code observables} have changed.
   */
  @NonNull
  public static Observable compositeObservable(@NonNull final Observable... observables) {
    if (observables.length == 0) {
      return new CompositeObservable();
    }

    if (observables.length == 1) {
      final Observable singleObservable = observables[0];
      if (singleObservable instanceof CompositeObservable) {
        return new CompositeObservable(
            ((CompositeObservable) singleObservable).observables);
      } else {
        return new CompositeObservable(singleObservable);
      }
    }

    final List<Observable> flattenedDedupedObservables = new ArrayList<>();
    for (final Observable observable : observables) {
      if (observable instanceof CompositeObservable) {
        for (Observable subObservable : ((CompositeObservable) observable).observables) {
          if (!flattenedDedupedObservables.contains(subObservable)) {
            flattenedDedupedObservables.add(subObservable);
          }
        }
      } else {
        if (!flattenedDedupedObservables.contains(observable)) {
          flattenedDedupedObservables.add(observable);
        }
      }
    }
    return new CompositeObservable(
        flattenedDedupedObservables.toArray(new Observable[flattenedDedupedObservables.size()]));
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that any of the
   * {@code observables} have changed only if the {@code condition} applies.
   */
  @NonNull
  public static Observable conditionalObservable(
      @NonNull final Condition condition, @NonNull final Observable... observables) {
    return new ConditionalObservable(compositeObservable(observables), condition);
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the
   * {@code observable} has changed, but never more often than every
   * {@code shortestUpdateWindowMillis}.
   */
  @NonNull
  public static Observable perMillisecondObservable(
      final int shortestUpdateWindowMillis, @NonNull final Observable observable) {
    return new LowPassFilterObservable(shortestUpdateWindowMillis, observable);
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the
   * {@code observable} has changed, but never more often than once per {@link Looper} cycle.
   */
  @NonNull
  public static Observable perLoopObservable(@NonNull final Observable observable) {
    return perMillisecondObservable(0, observable);
  }

  /**
   * Returns an asynchronous {@link UpdateDispatcher}.
   *
   * <p>{@link UpdateDispatcher#update()} can be called from any thread
   * {@link UpdateDispatcher#addUpdatable(Updatable)} and
   * {@link UpdateDispatcher#removeUpdatable(Updatable)} can only be called from {@link Looper}
   * threads. Any added {@link Updatable} will be called on the thread they were added from.
   */
  @NonNull
  public static UpdateDispatcher updateDispatcher() {
    return new AsyncUpdateDispatcher(null);
  }

  /**
   * Returns an asynchronous {@link UpdateDispatcher}.
   *
   * <p>See {@link #updateDispatcher()}
   *
   * <p>{@code updatablesChanged} will be called on the same thread as the {@link UpdateDispatcher}
   * was created from when the first {@link Updatable} was added / last {@link Updatable} was
   * removed.
   *
   * <p>This {@link UpdateDispatcher} is useful when implementing {@link Observable} services with
   * an <i>active</i>/<i>inactive</i> lifecycle.
   */
  @NonNull
  public static UpdateDispatcher updateDispatcher(
      @NonNull final ActivationHandler activationHandler) {
    return new AsyncUpdateDispatcher(activationHandler);
  }

  private static final class CompositeObservable implements Observable {
    @NonNull
    private static final Updatable[] NO_UPDATABLES = new Updatable[0];
    @NonNull
    private final Observable[] observables;
    @NonNull
    private Updatable[] updatables;

    CompositeObservable(@NonNull final Observable... observables) {
      this.observables = observables;
      this.updatables = NO_UPDATABLES;
    }

    @Override
    public synchronized void addUpdatable(@NonNull final Updatable updatable) {
      int indexToAdd = -1;
      Updatable localUpdatable = null;
      for (int index = 0; index < updatables.length; index += 2) {
        if (updatables[index] == updatable) {
          throw new IllegalStateException("Updatable already added, cannot add.");
        }
        if (updatables[index] == null) {
          indexToAdd = index;
        }
      }
      if (indexToAdd == -1) {
        indexToAdd = updatables.length;
        updatables = Arrays.copyOf(updatables, indexToAdd < 2 ? 2 : indexToAdd * 2);
      }
      updatables[indexToAdd] = updatable;
      localUpdatable = new WrapperUpdatable(updatable);
      updatables[indexToAdd + 1] = localUpdatable;
      for (final Observable observable : observables) {
        observable.addUpdatable(localUpdatable);
      }
    }

    @Override
    public synchronized void removeUpdatable(@NonNull final Updatable updatable) {
      for (int index = 0; index < updatables.length; index += 2) {
        if (updatables[index] == updatable) {
          for (final Observable observable : observables) {
            observable.removeUpdatable(updatables[index + 1]);
          }
          updatables[index] = null;
          updatables[index + 1] = null;
          return;
        }
      }
      throw new IllegalStateException("Updatable not added, cannot remove.");

    }
  }

  private static final class WrapperUpdatable implements Updatable {
    @NonNull
    private final Updatable updatable;

    public WrapperUpdatable(@NonNull final Updatable updatable) {
      this.updatable = updatable;
    }

    @Override
    public void update() {
      updatable.update();
    }
  }

  private static final class ConditionalObservable extends BaseObservable implements Updatable {
    @NonNull
    private final Observable observable;
    @NonNull
    private final Condition condition;

    ConditionalObservable(@NonNull final Observable observable,
        @NonNull final Condition condition) {
      this.observable = checkNotNull(observable);
      this.condition = checkNotNull(condition);
    }

    @Override
    protected void observableActivated() {
      observable.addUpdatable(this);
    }

    @Override
    protected void observableDeactivated() {
      observable.removeUpdatable(this);
    }

    @Override
    public void update() {
      if (condition.applies()) {
        dispatchUpdate();
      }
    }
  }

  static final class LowPassFilterObservable extends BaseObservable implements Updatable {
    @NonNull
    private final Observable observable;
    @NonNull
    private final WorkerHandler workerHandler;
    private final int shortestUpdateWindowMillis;

    private long lastUpdateTimestamp;

    LowPassFilterObservable(final int shortestUpdateWindowMillis,
        @NonNull final Observable observable) {
      this.shortestUpdateWindowMillis = shortestUpdateWindowMillis;
      this.observable = checkNotNull(observable);
      this.workerHandler = workerHandler();
    }

    @Override
    protected void observableActivated() {
      observable.addUpdatable(this);
    }

    @Override
    protected void observableDeactivated() {
      observable.removeUpdatable(this);
      workerHandler.removeMessages(WorkerHandler.MSG_CALL_LOW_PASS_UPDATE, this);
    }

    @Override
    public void update() {
      workerHandler.sendMessageDelayed(
          workerHandler.obtainMessage(WorkerHandler.MSG_CALL_LOW_PASS_UPDATE, this), (long) 0);
    }

    void lowPassUpdate() {
      workerHandler.removeMessages(WorkerHandler.MSG_CALL_LOW_PASS_UPDATE, this);
      final long elapsedRealtimeMillis = SystemClock.elapsedRealtime();
      final long timeFromLastUpdate = elapsedRealtimeMillis - lastUpdateTimestamp;
      if (timeFromLastUpdate >= shortestUpdateWindowMillis) {
        lastUpdateTimestamp = elapsedRealtimeMillis;
        dispatchUpdate();
      } else {
        workerHandler.sendMessageDelayed(
            workerHandler.obtainMessage(WorkerHandler.MSG_CALL_LOW_PASS_UPDATE, this),
            shortestUpdateWindowMillis - timeFromLastUpdate);
      }
    }
  }

  private static final class AsyncUpdateDispatcher extends BaseObservable
      implements UpdateDispatcher {

    @Nullable
    private final ActivationHandler activationHandler;

    private AsyncUpdateDispatcher(@Nullable ActivationHandler activationHandler) {
      this.activationHandler = activationHandler;
    }

    @Override
    protected void observableActivated() {
      if (activationHandler != null) {
        activationHandler.observableActivated(this);
      }
    }

    @Override
    protected void observableDeactivated() {
      if (activationHandler != null) {
        activationHandler.observableDeactivated(this);
      }
    }

    @Override
    public void update() {
      dispatchUpdate();
    }
  }

  private Observables() {}
}
