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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.agera.Common.StaticProducer;
import com.google.android.agera.FunctionCompilerStates.FItem;
import com.google.android.agera.FunctionCompilerStates.FList;
import java.util.List;

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

  private Functions() {}
}
