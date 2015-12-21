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

import static com.google.android.agera.Common.FALSE_PREDICATE;
import static com.google.android.agera.Common.TRUE_PREDICATE;
import static com.google.android.agera.Preconditions.checkNotNull;

import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Utility methods for obtaining predicate {@link Function} instances.
 */
public final class Predicates {
  private static final Function<CharSequence, Boolean> EMPTY_STRING_PREDICATE =
      new EmptyStringPredicate();

  /**
   * Returns a predicate {@link Function} that always returns {@code true}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Function<T, Boolean> truePredicate() {
    return (Function<T, Boolean>) TRUE_PREDICATE;
  }

  /**
   * Returns a predicate {@link Function} that always returns {@code false}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Function<T, Boolean> falsePredicate() {
    return (Function<T, Boolean>) FALSE_PREDICATE;
  }

  /**
   * Returns a predicate {@link Function} that indicates whether {@code object} is equal to the
   * predicate {@link Function} input.
   */
  @NonNull
  public static <T> Function<T, Boolean> equalTo(@NonNull final T object) {
    return new EqualToPredicate<>(object);
  }

  /**
   * Returns a predicate {@link Function} that indicates whether the predicate {@link Function}
   * input is an instance of {@code type}.
   */
  @NonNull
  public static <T> Function<T, Boolean> instanceOf(@NonNull final Class<?> type) {
    return new InstanceOfPredicate<>(type);
  }

  /**
   * Returns a predicate {@link Function} that indicates whether the predicate {@link Function}
   * input is an empty {@link CharSequence}.
   */
  @NonNull
  public static Function<CharSequence, Boolean> emptyString() {
    return EMPTY_STRING_PREDICATE;
  }

  /**
   * Returns a predicate {@link Function} that negates {@code predicate}.
   */
  @NonNull
  public static <T> Function<T, Boolean> not(final Function<T, Boolean> predicate) {
    if (predicate instanceof NegatedPredicate) {
      return ((NegatedPredicate<T>) predicate).predicate;
    }
    if (predicate == truePredicate()) {
      return falsePredicate();
    }
    if (predicate == falsePredicate()) {
      return truePredicate();
    }
    return new NegatedPredicate<>(predicate);
  }

  /**
   * Returns a predicate {@link Function} that evaluates to {@code true} if any of the given
   * {@code predicates} evaluates to {@code true}. If {@code predicates} is empty, the returned
   * predicate {@link Function} will always evaluate to {@code false}.
   */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  @NonNull
  public static <T> Function<T, Boolean> any(
      @NonNull final Function<? super T, Boolean>... predicates) {
    return (Function<T, Boolean>) composite(predicates, falsePredicate(), truePredicate(), true);
  }

  /**
   * Returns a predicate {@link Function} that evaluates to {@code true} if all of the given
   * {@code predicate} evaluates to {@code true}. If {@code predicates} is empty, the returned
   * predicate {@link Function} will always evaluate to {@code true}.
   */
  @SafeVarargs
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Function<T, Boolean> all(
      @NonNull final Function<? super T, Boolean>... predicates) {
    return (Function<T, Boolean>) composite(predicates, truePredicate(), falsePredicate(), false);
  }

  @SuppressWarnings("unchecked")
  private static Function<Object, Boolean> composite(@NonNull final Function[] predicates,
      @NonNull final Function defaultPredicate, @NonNull final Function definingPredicate,
      final boolean definingResult) {
    int nonDefaultCount = 0;
    Function lastNonDefaultPredicate = null;
    for (final Function predicate : predicates) {
      if (predicate == definingPredicate) {
        return definingPredicate;
      } else if (predicate != defaultPredicate) {
        nonDefaultCount++;
        lastNonDefaultPredicate = predicate;
      }
    }
    if (nonDefaultCount == 0) {
      return defaultPredicate;
    } else if (nonDefaultCount == 1) {
      return lastNonDefaultPredicate;
    }
    return new CompositePredicate<>(predicates.clone(), definingResult);
  }

  private static final class CompositePredicate<T> implements Function<T, Boolean> {
    @NonNull
    private final Function<T, Boolean>[] predicates;
    private final boolean definingResult;

    CompositePredicate(@NonNull final Function<T, Boolean>[] predicates,
        final boolean definingResult) {
      this.definingResult = definingResult;
      this.predicates = checkNotNull(predicates);
    }

    @Override
    public Boolean apply(@NonNull final T value) {
      for (final Function<T, Boolean> predicate : predicates) {
        if (predicate.apply(value) == definingResult) {
          return definingResult;
        }
      }
      return !definingResult;
    }
  }

  private static final class EmptyStringPredicate implements Function<CharSequence, Boolean> {

    @Override
    public Boolean apply(@NonNull final CharSequence input) {
      return TextUtils.isEmpty(input);
    }
  }

  private static final class NegatedPredicate<T> implements Function<T, Boolean> {
    @NonNull
    private final Function<T, Boolean> predicate;

    NegatedPredicate(@NonNull final Function<T, Boolean> predicate) {
      this.predicate = checkNotNull(predicate);
    }

    @Override
    public Boolean apply(@NonNull final T t) {
      return !predicate.apply(t);
    }
  }

  private static final class InstanceOfPredicate<T> implements Function<T, Boolean> {
    @NonNull
    private final Class<?> type;

    InstanceOfPredicate(@NonNull final Class<?> type) {
      this.type = checkNotNull(type);
    }

    @Override
    public Boolean apply(@NonNull final T input) {
      return type.isAssignableFrom(input.getClass());
    }
  }

  private static final class EqualToPredicate<T> implements Function<T, Boolean> {
    @NonNull
    private final T object;

    EqualToPredicate(@NonNull final T object) {
      this.object = checkNotNull(object);
    }

    @Override
    public Boolean apply(@NonNull final T input) {
      return input.equals(object);
    }
  }

  private Predicates() {}
}
