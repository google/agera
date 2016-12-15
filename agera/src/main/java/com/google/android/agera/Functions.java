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

import static com.google.android.agera.Common.FAILED_RESULT;
import static com.google.android.agera.Common.NULL_OPERATOR;
import static com.google.android.agera.FunctionCompiler.functionCompiler;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Result.present;
import static java.util.Collections.emptyList;

import com.google.android.agera.Common.StaticProducer;
import com.google.android.agera.FunctionCompilerStates.FItem;
import com.google.android.agera.FunctionCompilerStates.FList;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Utility methods for obtaining {@link Function} instances.
 */
public final class Functions {

  /**
   * Returns a {@link Function} that returns {@code object} as the result of each
   * {@link Function#apply} function call.
   */
  @NonNull
  public static <F, T> Function<F, T> staticFunction(@NonNull final T object) {
    return new StaticProducer<>(object);
  }

  /**
   * Returns a {@link Function} that returns the result of {@code supplier} as the result of each
   * {@link Function#apply} function call.
   */
  @NonNull
  public static <F, T> Function<F, T> supplierAsFunction(
      @NonNull final Supplier<? extends T> supplier) {
    return new SupplierAsFunction<>(supplier);
  }

  /**
   * Returns a {@link Function} that passes on the {@link Function} input as output.
   */
  @NonNull
  public static <T> Function<T, T> identityFunction() {
    @SuppressWarnings("unchecked")
    final Function<T, T> identityFunction = (Function<T, T>) NULL_OPERATOR;
    return identityFunction;
  }

  /**
   * Starts describing {@link Function} that starts with a single item.
   *
   * @return the next {@link FunctionCompilerStates} state
   */
  @NonNull
  @SuppressWarnings({"unchecked", "UnusedParameters"})
  public static <F> FItem<F, F> functionFrom(@Nullable Class<F> from) {
    return functionCompiler();
  }

  /**
   * Starts describing a {@link Function} that starts with a {@link List} of items.
   *
   * @return the next {@link FunctionCompilerStates} state
   */
  @NonNull
  @SuppressWarnings({"unchecked", "UnusedParameters"})
  public static <F> FList<F, List<F>, List<F>> functionFromListOf(
      @Nullable final Class<F> from) {
    return functionCompiler();
  }

  /**
   * Returns a {@link Function} to map each item into a new type in parallel on the provided {@link
   * ExecutorService}. <p> NOTE: If the {@link Function} is already executing on the provided {@link
   * ExecutorService} it is important that it is executing on at least two threads, or the method
   * will deadlock. Since the method will only parallelize to as many threads that are available in
   * the {@link ExecutorService} it is never recommended to use this method with an {@link
   * ExecutorService} running on less than two threads (or three if the compiled {@link Function} is
   * running on it as well). Since parallelizing comes at a memory and computational overhead, it's
   * recommended only to use this to map with a long running {@link Function} (such as fetching data
   * over slow IO in parallel).
   */
  @NonNull
  public static <F, T> Function<List<F>, List<Result<T>>> parallelMap(
      @NonNull final ExecutorService executor, @NonNull final Function<F, T> function) {
    return new ParallelMapFunction<>(executor, function);
  }

  /**
   * Returns a {@link Function} that wraps a {@link Throwable} in a
   * {@link Result#failure(Throwable)}).
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static <T> Function<Throwable, Result<T>> failedResult() {
    return (Function<Throwable, Result<T>>) FAILED_RESULT;
  }

  private static final class SupplierAsFunction<F, T> implements Function<F, T> {
    @NonNull
    private final Supplier<? extends T> supplier;

    SupplierAsFunction(@NonNull final Supplier<? extends T> supplier) {
      this.supplier = checkNotNull(supplier);
    }

    @NonNull
    @Override
    public T apply(@NonNull F from) {
      return supplier.get();
    }
  }

  private static final class ParallelMapFunction<F, T>
      implements Function<List<F>, List<Result<T>>> {
    @NonNull
    private final Function<F, T> function;
    @NonNull
    private final ExecutorService executorService;

    ParallelMapFunction(@NonNull final ExecutorService executorService,
        @NonNull final Function<F, T> function) {
      this.executorService = checkNotNull(executorService);
      this.function = checkNotNull(function);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public List<Result<T>> apply(@NonNull final List<F> input) {
      if (input.isEmpty()) {
        return emptyList();
      }
      final List<Future<T>> futureResult = new ArrayList(input.size());
      for (final F item : input) {
        futureResult.add(executorService.submit(new Callable<T>() {
          @Override
          public T call() throws Exception {
            return function.apply(item);
          }
        }));
      }
      final List<Result<T>> result = new ArrayList(input.size());
      final int size = futureResult.size();
      for (int i = 0; i < size; i++) {
        final Future<T> item = futureResult.get(i);
        try {
          result.add(present(item.get()));
        } catch (final ExecutionException e) {
          result.add(Result.<T>failure(e.getCause()));
        } catch (final InterruptedException e) {
          result.add(Result.<T>failure(e));
        }
      }
      return result;
    }
  }

  private Functions() {}
}
