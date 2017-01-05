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
import static com.google.android.agera.Result.absentIfNull;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Utility methods for creating {@link Reservoir} instances.
 *
 * <p>Any {@link Reservoir} created by this class has to be created from a {@link Looper} thread
 * or the method will throw an {@link IllegalStateException}.
 */
public final class Reservoirs {

  /**
   * Returns a {@link Reservoir} for the given value type.
   *
   * <p>The returned reservoir uses a standard unbounded FIFO queue as its backing storage. As a
   * result, all values are accepted and no duplication checks are used, and they are dequeued in
   * the same order.
   */
  @NonNull
  public static <T> Reservoir<T> reservoirOf(
      @SuppressWarnings("unused") @Nullable final Class<T> clazz) {
    return reservoir();
  }

  /**
   * Same as {@link #reservoirOf(Class)}. This variant is useful for when the value type is more
   * readily inferrable from the context, such as when used as a variable initializer or a return
   * value, so client code could simply write, for example,
   *
   * <pre>{@code Reservoir<String> stringReservoir = reservoir();}</pre>
   *
   * where this method is statically imported. This also helps in-line creation of a reservoir whose
   * value type is generic, such as {@code List<String>}, so client code could write
   * {@code Reservoirs.<List<String>>reservoir()} instead of the less readable
   * {@code reservoirOf((Class<List<String>>) null)}.
   */
  @NonNull
  public static <T> Reservoir<T> reservoir() {
    return reservoir(new ArrayDeque<T>());
  }

  /**
   * Returns a {@link Reservoir} that uses the given {@code queue} as the backing storage for
   * enqueuing and dequeuing values. It is up to the concrete {@link Queue#offer} implementation of
   * the {@code queue} instance whether and how to accept each value to be enqueued.
   *
   * @param queue The backing storage of the reservoir. Any valid {@link Queue} implementation can
   *     be used, including non-FIFO queues such as {@link PriorityQueue}. Only these methods are
   *     used: {@link Queue#offer} for attempting to enqueue a value, {@link Queue#poll} for
   *     attempting to dequeue a value, and {@link Queue#isEmpty} for state check. All accesses are
   *     synchronized on this {@code queue} instance; if the queue must also be accessed elsewhere,
   *     those accesses must also be synchronized on this {@code queue} instance. Also note that
   *     modifications to the queue outside the {@link Reservoir} interface will not update the
   *     reservoir or its registered {@link Updatable}s.
   */
  @NonNull
  public static <T> Reservoir<T> reservoir(@NonNull final Queue<T> queue) {
    return new SynchronizedReservoir<>(checkNotNull(queue));
  }

  private static final class SynchronizedReservoir<T> extends BaseObservable
      implements Reservoir<T> {
    @NonNull
    private final Queue<T> queue;

    private SynchronizedReservoir(@NonNull final Queue<T> queue) {
      this.queue = checkNotNull(queue);
    }

    @Override
    public void accept(@NonNull T value) {
      boolean shouldDispatchUpdate;
      synchronized (queue) {
        boolean wasEmpty = queue.isEmpty();
        boolean added = queue.offer(value);
        shouldDispatchUpdate = wasEmpty && added;
      }
      if (shouldDispatchUpdate) {
        dispatchUpdate();
      }
    }

    @NonNull
    @Override
    public Result<T> get() {
      T nullableValue;
      boolean shouldDispatchUpdate;
      synchronized (queue) {
        nullableValue = queue.poll();
        shouldDispatchUpdate = !queue.isEmpty();
      }
      if (shouldDispatchUpdate) {
        dispatchUpdate();
      }
      return absentIfNull(nullableValue);
    }

    @Override
    protected void observableActivated() {
      synchronized (queue) {
        if (queue.isEmpty()) {
          return;
        }
      }
      dispatchUpdate();
    }
  }

  private Reservoirs() {}
}
