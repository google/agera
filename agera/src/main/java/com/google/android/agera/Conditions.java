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
 * Utility methods for obtaining {@link Condition} instances.
 */
public final class Conditions {

  /**
   * Returns a {@link Condition} that always returns {@code true}.
   */
  @NonNull
  public static Condition trueCondition() {
    return TRUE_CONDICATE;
  }

  /**
   * Returns a {@link Condition} that always returns {@code false}.
   */
  @NonNull
  public static Condition falseCondition() {
    return FALSE_CONDICATE;
  }

  /**
   * Returns a {@link Condition} that always returns {@code value}.
   */
  @NonNull
  public static Condition staticCondition(final boolean value) {
    return value ? TRUE_CONDICATE : FALSE_CONDICATE;
  }

  /**
   * Returns a {@link Condition} that negates the given {@code condition}.
   */
  @NonNull
  public static Condition not(@NonNull final Condition condition) {
    if (condition instanceof NegatedCondition) {
      return ((NegatedCondition) condition).condition;
    }
    if (condition == TRUE_CONDICATE) {
      return FALSE_CONDICATE;
    }
    if (condition == FALSE_CONDICATE) {
      return TRUE_CONDICATE;
    }
    return new NegatedCondition(condition);
  }

  /**
   * Returns a {@link Condition} that evaluates to {@code true} if any of the given
   * {@code conditions} evaluates to {@code true}. If {@code conditions} is empty, the returned
   * {@link Condition} will always evaluate to {@code false}.
   */
  @NonNull
  public static Condition any(@NonNull final Condition... conditions) {
    return composite(conditions, falseCondition(), trueCondition());
  }

  /**
   * Returns a {@link Condition} that evaluates to {@code true} if all of the given
   * {@code conditions} evaluates to {@code true}. If {@code conditions} is empty, the returned
   * {@link Condition} will always evaluate to {@code true}.
   */
  @NonNull
  public static Condition all(@NonNull final Condition... conditions) {
    return composite(conditions, trueCondition(), falseCondition());
  }

  /**
   * Returns a {@link Condition} from a {@link Predicate} and a {@link Supplier}.
   *
   * <p>When applied the {@link Supplier} return value will be provided to the {@link Predicate} and
   * the result will be returned.
   * If {@link Predicates#truePredicate} or {@link Predicates#falsePredicate} is passed,
   * {@code supplier} will never be called.
   */
  @NonNull
  public static <T> Condition predicateAsCondition(@NonNull final Predicate<T> predicate,
      @NonNull final Supplier<? extends T> supplier) {
    if (predicate == TRUE_CONDICATE) {
      return TRUE_CONDICATE;
    }
    if (predicate == FALSE_CONDICATE) {
      return FALSE_CONDICATE;
    }
    return new PredicateCondition<>(predicate, supplier);
  }

  @NonNull
  private static Condition composite(@NonNull final Condition[] conditions,
      @NonNull final Condition defaultCondition, @NonNull final Condition definingCondition) {
    int nonDefaultCount = 0;
    Condition lastNonDefaultCondition = null;
    for (final Condition condition : conditions) {
      if (condition == definingCondition) {
        return definingCondition;
      } else if (condition != defaultCondition) {
        nonDefaultCount++;
        lastNonDefaultCondition = condition;
      }
    }
    if (nonDefaultCount == 0) {
      return defaultCondition;
    } else if (nonDefaultCount == 1) {
      return lastNonDefaultCondition;
    }
    return new CompositeCondition(conditions.clone(), definingCondition.applies());
  }

  private static final class CompositeCondition implements Condition {
    @NonNull
    private final Condition[] conditions;
    private final boolean definingResult;

    CompositeCondition(@NonNull final Condition[] conditions, final boolean definingResult) {
      this.definingResult = definingResult;
      this.conditions = checkNotNull(conditions);
    }

    @Override
    public boolean applies() {
      for (final Condition condition : conditions) {
        if (condition.applies() == definingResult) {
          return definingResult;
        }
      }
      return !definingResult;
    }
  }

  private static final class NegatedCondition implements Condition {
    @NonNull
    final Condition condition;

    NegatedCondition(@NonNull final Condition condition) {
      this.condition = condition;
    }

    @Override
    public boolean applies() {
      return !condition.applies();
    }
  }

  private static final class PredicateCondition<T> implements Condition {
    @NonNull
    private final Predicate<T> predicate;
    @NonNull
    private final Supplier<? extends T> supplier;

    PredicateCondition(@NonNull final Predicate<T> predicate,
        @NonNull final Supplier<? extends T> supplier) {
      this.predicate = checkNotNull(predicate);
      this.supplier = checkNotNull(supplier);
    }

    @Override
    public boolean applies() {
      return predicate.apply(supplier.get());
    }
  }

  private Conditions() {}
}
