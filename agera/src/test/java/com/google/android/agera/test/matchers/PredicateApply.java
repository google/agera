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
package com.google.android.agera.test.matchers;

import android.support.annotation.NonNull;
import com.google.android.agera.Predicate;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class PredicateApply<T> extends TypeSafeMatcher<Predicate<? super T>> {
  private final boolean value;
  @NonNull
  private final T data;

  private PredicateApply(final boolean value, @NonNull final T data) {
    this.value = value;
    this.data = data;
  }

  @Override
  protected boolean matchesSafely(final Predicate<? super T> predicate) {
    return predicate.apply(data) == value;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(value ? "applies for " : "does not apply for ").appendValue(data);
  }

  @NonNull
  @Factory
  public static <T> Matcher<Predicate<? super T>> appliesFor(@NonNull final T data) {
    return new PredicateApply<>(true, data);
  }

  @NonNull
  @Factory
  public static <T> Matcher<Predicate<? super T>> doesNotApplyFor(@NonNull final T data) {
    return new PredicateApply<>(false, data);
  }
}
