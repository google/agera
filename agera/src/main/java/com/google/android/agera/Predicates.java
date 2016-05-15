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

import static com.google.android.agera.Common.FALSE_CONDICATE;
import static com.google.android.agera.Common.TRUE_CONDICATE;
import static com.google.android.agera.Preconditions.checkNotNull;

import android.support.annotation.NonNull;

/**
 * Utility methods for obtaining {@link Predicate} instances.
 */
public final class Predicates {
  private static final Predicate<CharSequence> EMPTY_STRING_PREDICATE = new EmptyStringPredicate();

  /**
   * Returns a {@link Predicate} from a {@link Condition}.
   *
   * <p>When applied the {@link Predicate} input parameter will be ignored and the result of
   * {@code condition} will be returned.
   */
  @NonNull
  public static <T> Predicate<T> conditionAsPredicate(@NonNull final Condition condition) {
    if (condition == TRUE_CONDICATE) {
      return truePredicate();
    }
    if (condition == FALSE_CONDICATE) {
      return falsePredicate();
    }
    return new ConditionAsPredicate<>(condition);
  }

  /**
   * Returns a {@link Predicate} that always returns {@code true}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> truePredicate() {
    return TRUE_CONDICATE;
  }

  /**
   * Returns a {@link Predicate} that always returns {@code false}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> falsePredicate() {
    return FALSE_CONDICATE;
  }

  /**
   * Returns a {@link Predicate} that indicates whether {@code object} is equal to the
   * {@link Predicate} input.
   */
  @NonNull
  public static <T> Predicate<T> equalTo(@NonNull final T object) {
    return new EqualToPredicate<>(object);
  }

  /**
   * Returns a {@link Predicate} that indicates whether the {@link Predicate} input is an
   * instance of {@code type}.
   */
  @NonNull
  public static <T> Predicate<T> instanceOf(@NonNull final Class<?> type) {
    return new InstanceOfPredicate<>(type);
  }

  /**
   * Returns a {@link Predicate} that indicates whether the {@link Predicate} input is an
   * empty {@link CharSequence}.
   */
  @NonNull
  public static Predicate<CharSequence> emptyString() {
    return EMPTY_STRING_PREDICATE;
  }

  /**
   * Returns a {@link Predicate} that negates {@code predicate}.
   */
  @NonNull
  public static <T> Predicate<T> not(@NonNull final Predicate<T> predicate) {
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
   * Returns a {@link Predicate} that evaluates to {@code true} if any of the given
   * {@code predicates} evaluates to {@code true}. If {@code predicates} is empty, the returned
   * {@link Predicate} will always evaluate to {@code false}.
   */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  @NonNull
  public static <T> Predicate<T> any(@NonNull final Predicate<? super T>... predicates) {
    return composite(predicates, falsePredicate(), truePredicate(), true);
  }

  /**
   * Returns a {@link Predicate} that evaluates to {@code true} if all of the given
   * {@code conditions} evaluates to {@code true}. If {@code conditions} is empty, the returned
   * {@link Condition} will always evaluate to {@code true}.
   */
  @SafeVarargs
  @NonNull
  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> all(@NonNull final Predicate<? super T>... predicates) {
    return composite(predicates, truePredicate(), falsePredicate(), false);
  }

  @SuppressWarnings("unchecked")
  @NonNull
  private static Predicate composite(@NonNull final Predicate[] predicates,
      @NonNull final Predicate defaultPredicate, @NonNull final Predicate definingPredicate,
      final boolean definingResult) {
    int nonDefaultCount = 0;
    Predicate lastNonDefaultPredicate = null;
    for (final Predicate predicate : predicates) {
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

  private static final class CompositePredicate<T> implements Predicate<T> {
    @NonNull
    private final Predicate<T>[] predicates;
    private final boolean definingResult;

    CompositePredicate(@NonNull final Predicate<T>[] predicates, final boolean definingResult) {
      this.definingResult = definingResult;
      this.predicates = checkNotNull(predicates);
    }

    @Override
    public boolean apply(@NonNull final T value) {
      for (final Predicate<T> predicate : predicates) {
        if (predicate.apply(value) == definingResult) {
          return definingResult;
        }
      }
      return !definingResult;
    }
  }

  private static final class EmptyStringPredicate implements Predicate<CharSequence> {

    @Override
    public boolean apply(@NonNull final CharSequence input) {
      return input.length() == 0;
    }
  }

  private static final class NegatedPredicate<T> implements Predicate<T> {
    @NonNull
    private final Predicate<T> predicate;

    NegatedPredicate(@NonNull final Predicate<T> predicate) {
      this.predicate = checkNotNull(predicate);
    }

    @Override
    public boolean apply(@NonNull final T t) {
      return !predicate.apply(t);
    }
  }

  private static final class ConditionAsPredicate<T> implements Predicate<T> {
    @NonNull
    private final Condition condition;

    ConditionAsPredicate(@NonNull final Condition condition) {
      this.condition = checkNotNull(condition);
    }

    @Override
    public boolean apply(@NonNull T input) {
      return condition.applies();
    }
  }

  private static final class InstanceOfPredicate<T> implements Predicate<T> {
    @NonNull
    private final Class<?> type;

    InstanceOfPredicate(@NonNull final Class<?> type) {
      this.type = checkNotNull(type);
    }

    @Override
    public boolean apply(@NonNull final T input) {
      return type.isAssignableFrom(input.getClass());
    }
  }

  private static final class EqualToPredicate<T> implements Predicate<T> {
    @NonNull
    private final T object;

    EqualToPredicate(@NonNull final T object) {
      this.object = checkNotNull(object);
    }

    @Override
    public boolean apply(@NonNull final T input) {
      return input.equals(object);
    }
  }

  private Predicates() {}
}
