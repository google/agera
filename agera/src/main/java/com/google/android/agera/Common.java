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
import static com.google.android.agera.Result.failure;

import android.support.annotation.NonNull;

final class Common {
  static final Function<Throwable, ? extends Result<?>> FAILED_RESULT = new FailedResult<>();
  static final NullOperator NULL_OPERATOR = new NullOperator();
  static final StaticCondicate TRUE_CONDICATE = new StaticCondicate(true);
  static final StaticCondicate FALSE_CONDICATE = new StaticCondicate(false);

  private static final class NullOperator implements Function, Receiver, Binder {
    @NonNull
    @Override
    public Object apply(@NonNull final Object from) {
      return from;
    }

    @Override
    public void accept(@NonNull final Object value) {}

    @Override
    public void bind(@NonNull final Object o, @NonNull final Object o2) {}
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

  private static final class FailedResult<T> implements Function<Throwable, Result<T>> {
    @NonNull
    @Override
    public Result<T> apply(@NonNull final Throwable input) {
      return failure(input);
    }
  }

  private Common() {}
}
