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
import com.google.android.agera.Common.StaticProducer;

/**
 * Utility methods for obtaining {@link Supplier} instances.
 */
public final class Suppliers {

  /**
   * Returns a {@link Supplier} that always supplies the given {@code object} when its
   * {@link Supplier#get()} is called.
   */
  @NonNull
  public static <T> Supplier<T> staticSupplier(@NonNull final T object) {
    return new StaticProducer<>(object);
  }

  /**
   * Returns a {@link Supplier} that always supplies the value returned from the given
   * {@link Function} {@code function} when called with {@code from}.
   */
  @NonNull
  public static <T, F> Supplier<T> functionAsSupplier(
      @NonNull final Function<F, T> function, @NonNull final F from) {
    return new FunctionToSupplierConverter<>(function, from);
  }

  private static final class FunctionToSupplierConverter<T, F> implements Supplier<T> {
    @NonNull
    private final Function<F, T> function;
    @NonNull
    private final F from;

    private FunctionToSupplierConverter(@NonNull final Function<F, T> function,
        @NonNull final F from) {
      this.function = checkNotNull(function);
      this.from = checkNotNull(from);
    }

    @NonNull
    @Override
    public T get() {
      return function.apply(from);
    }
  }

  private Suppliers() {}
}
