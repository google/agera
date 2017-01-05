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

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
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
    return compositeObservable(0, observables);
  }

  @NonNull
  static Observable compositeObservable(final int shortestUpdateWindowMillis,
      @NonNull final Observable... observables) {
    if (observables.length == 0) {
      return new CompositeObservable(shortestUpdateWindowMillis);
    }

    if (observables.length == 1) {
      final Observable singleObservable = observables[0];
      if (singleObservable instanceof CompositeObservable
          && ((CompositeObservable) singleObservable).shortestUpdateWindowMillis == 0) {
        return new CompositeObservable(shortestUpdateWindowMillis,
            ((CompositeObservable) singleObservable).observables);
      } else {
        return new CompositeObservable(shortestUpdateWindowMillis, singleObservable);
      }
    }

    final List<Observable> flattenedDedupedObservables = new ArrayList<>();
    for (final Observable observable : observables) {
      if (observable instanceof CompositeObservable
          && ((CompositeObservable) observable).shortestUpdateWindowMillis == 0) {
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
    return new CompositeObservable(shortestUpdateWindowMillis,
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
   * {@code observables} has changed, but never more often than every
   * {@code shortestUpdateWindowMillis}.
   */
  @NonNull
  public static Observable perMillisecondObservable(
      final int shortestUpdateWindowMillis, @NonNull final Observable... observables) {
    return compositeObservable(shortestUpdateWindowMillis, observables);
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the
   * {@code observable} has changed, but never more often than once per {@link Looper} cycle.
   */
  @NonNull
  public static Observable perLoopObservable(@NonNull final Observable... observables) {
    return compositeObservable(observables);
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

  private static final class CompositeObservable extends BaseObservable implements Updatable {
    @NonNull
    private final Observable[] observables;

    CompositeObservable(final int shortestUpdateWindowMillis,
        @NonNull final Observable... observables) {
      super(shortestUpdateWindowMillis);
      this.observables = observables;
    }

    @Override
    protected void observableActivated() {
      for (final Observable observable : observables) {
        observable.addUpdatable(this);
      }
    }

    @Override
    protected void observableDeactivated() {
      for (final Observable observable : observables) {
        observable.removeUpdatable(this);
      }
    }

    @Override
    public void update() {
      dispatchUpdate();
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

  private static final class AsyncUpdateDispatcher extends BaseObservable
      implements UpdateDispatcher {

    @Nullable
    private final ActivationHandler activationHandler;

    private AsyncUpdateDispatcher(@Nullable final ActivationHandler activationHandler) {
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
