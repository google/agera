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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility methods for obtaining {@link Async} instances.
 */
public final class Asyncs {

  /**
   * Returns an {@link Async} operator that delivers the input value to the receiver unmodified,
   * from a thread owned by the given {@link Executor}. The {@code executor} is assumed to never
   * throw {@link RejectedExecutionException} and will eventually run the submitted
   * {@link Runnable}s.
   */
  public static <T> Async<T, T> goTo(@NonNull final Executor executor) {
    return new GoToExecutorAsync<>(checkNotNull(executor), null, null);
  }

  /**
   * Same as {@link #goTo(Executor)}, but with an unused {@link Class} parameter for easier generic
   * type matching.
   */
  public static <T> Async<T, T> goTo(@NonNull final Executor executor,
      @SuppressWarnings("unused") @Nullable Class<T> clazz) {
    return goTo(executor);
  }

  private static final class GoToExecutorAsync<T> implements Async<T, T>, Runnable {
    private final Executor executor;

    // Extreme class/object conservation: this class doubles as a Runnable to cater for the most
    // typical use-case where this instance is used only locally in a data processing flow. There
    // will be at most one async call at a time, so no need to create a throwaway Runnable just to
    // submit it to the executor. In case an async call occurs before the executor consumes a
    // previous call, a throwaway new instance of this class (rather than an ad-hoc Runnable) will
    // be created to serve the new async call.
    @NonNull
    private final AtomicReference<T> reference;
    private volatile Receiver<T> outputReceiver;

    GoToExecutorAsync(final Executor executor, @Nullable final T value,
        @Nullable final Receiver<T> outputReceiver) {
      this.executor = executor;
      this.reference = new AtomicReference<>(value);
      this.outputReceiver = outputReceiver;
    }

    @Override
    public void async(@NonNull final T input, @NonNull final Receiver<T> outputReceiver,
        @NonNull final Condition cancelled) {
      checkNotNull(input);
      checkNotNull(outputReceiver);
      if (reference.compareAndSet(null, input)) {
        this.outputReceiver = outputReceiver;
        executor.execute(this);
      } else {
        executor.execute(new GoToExecutorAsync<>(null, input, outputReceiver));
      }
    }

    @Override
    public void run() {
      // Get receiver first, because after setting reference to null, it is not guaranteed that the
      // receiver obtained afterwards will still be paired with the retrieved value.
      Receiver<T> outputReceiver = checkNotNull(this.outputReceiver);
      T value = checkNotNull(reference.getAndSet(null));
      outputReceiver.accept(value);
    }
  }

  private Asyncs() {}
}
