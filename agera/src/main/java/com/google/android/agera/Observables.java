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

import com.google.android.agera.Common.AsyncUpdateDispatcher;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for obtaining {@link Observable} instances.
 *
 * <p>Any {@link Observable} created by this class have to be created from a {@link Looper} thread
 * or they will throw an {@link IllegalStateException}
 *
 * <p>{@link UpdateDispatcher}s created by this class will for any injected
 * {@link UpdatablesChanged} call {@link UpdatablesChanged#firstUpdatableAdded(UpdateDispatcher)}
 * and {@link UpdatablesChanged#lastUpdatableRemoved(UpdateDispatcher)} on the thread the
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
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the
   * {@code observable} has changed, but never more often than every
   * {@code shortestUpdateWindowMillis}.
   */
  @NonNull
  public static Observable perMillisecondFilterObservable(
      final int shortestUpdateWindowMillis, @NonNull final Observable observable) {
    return new LowPassFilterObservable(shortestUpdateWindowMillis, observable);
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the
   * {@code observable} has changed, but never more often than once per {@link Looper} cycle.
   */
  @NonNull
  public static Observable perCycleFilterObservable(@NonNull final Observable observable) {
    return perMillisecondFilterObservable(0, observable);
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
   * <p>This {@link UpdateDispatcher} is useful when implementing {@link Observable} services with a
   * <i>suspended</i>/<i>active</i> life cycle.
   */
  @NonNull
  public static UpdateDispatcher updateDispatcher(
      @NonNull final UpdatablesChanged updatablesChanged) {
    return new AsyncUpdateDispatcher(updatablesChanged);
  }

  private static final class CompositeObservable implements Observable, UpdatablesChanged {
    @NonNull
    private final Observable[] observables;
    private final UpdateDispatcher updateDispatcher;

    CompositeObservable(@NonNull final Observable... observables) {
      this.observables = observables;
      this.updateDispatcher = updateDispatcher(this);
    }

    @Override
    public void firstUpdatableAdded(@NonNull final UpdateDispatcher updateDispatcher) {
      for (final Observable observable : observables) {
        observable.addUpdatable(updateDispatcher);
      }
    }

    @Override
    public void lastUpdatableRemoved(@NonNull final UpdateDispatcher updateDispatcher) {
      for (final Observable observable : observables) {
        observable.removeUpdatable(updateDispatcher);
      }
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.removeUpdatable(updatable);
    }
  }

  private static final class LowPassFilterObservable extends Handler implements Updatable,
      Observable, UpdatablesChanged {
    private static final int MSG_UPDATE = 0;

    @NonNull
    private final UpdateDispatcher updateDispatcher;
    @NonNull
    private final Observable observable;
    private final int shortestUpdateWindowMillis;

    private long lastUpdateTimestamp;

    LowPassFilterObservable(final int shortestUpdateWindowMillis,
        @NonNull final Observable observable) {
      this.shortestUpdateWindowMillis = shortestUpdateWindowMillis;
      this.observable = checkNotNull(observable);
      this.updateDispatcher = updateDispatcher(this);
    }

    @Override
    public void firstUpdatableAdded(final UpdateDispatcher updateDispatcher) {
      observable.addUpdatable(this);
    }

    @Override
    public void lastUpdatableRemoved(final UpdateDispatcher updateDispatcher) {
      observable.removeUpdatable(this);
      removeMessages(MSG_UPDATE);
    }

    @Override
    public void handleMessage(@NonNull final Message message) {
      if (message.what == MSG_UPDATE) {
        removeMessages(MSG_UPDATE);
        final long elapsedRealtimeMillis = SystemClock.elapsedRealtime();
        final long timeFromLastUpdate = elapsedRealtimeMillis - lastUpdateTimestamp;
        if (timeFromLastUpdate >= shortestUpdateWindowMillis) {
          lastUpdateTimestamp = elapsedRealtimeMillis;
          updateDispatcher.update();
        } else {
          sendEmptyMessageDelayed(MSG_UPDATE, shortestUpdateWindowMillis - timeFromLastUpdate);
        }
      }
    }

    @Override
    public void update() {
      sendEmptyMessage(MSG_UPDATE);
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.removeUpdatable(updatable);
    }
  }

  private Observables() {}
}
