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

import static com.google.android.agera.Result.present;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.agera.Reservoir;
import com.google.android.agera.Result;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Similar to {@link SupplierGives} but takes care not to dequeue another item from
 * the {@link Reservoir} that failed to match.
 */
public final class ReservoirGives<T> extends TypeSafeMatcher<Reservoir<T>> {
  @NonNull
  private final Result<T> value;

  @Nullable
  private Reservoir<T> lastFailedMatch;
  @Nullable
  private Result<T> lastUnequalValue;

  private ReservoirGives(@NonNull final Result<T> value) {
    this.value = value;
  }

  @NonNull
  @Factory
  public static <T> Matcher<Reservoir<T>> givesPresentValue(final T value) {
    return new ReservoirGives<>(present(value));
  }

  @NonNull
  @Factory
  public static <T> Matcher<Reservoir<T>> givesAbsentValueOf(
      @SuppressWarnings("unused") @Nullable final Class<T> ofClass) {
    return new ReservoirGives<>(Result.<T>absent());
  }

  @Override
  protected boolean matchesSafely(@NonNull final Reservoir<T> item) {
    final Result<T> got = item.get();
    if (value.equals(got)) {
      if (lastFailedMatch == item) {
        lastFailedMatch = null;
      }
      return true;
    }
    lastFailedMatch = item;
    lastUnequalValue = got;
    return false;
  }

  @Override
  public void describeTo(@NonNull final Description description) {
    description.appendText("gives value ").appendValue(value);
  }

  @Override
  protected void describeMismatchSafely(@NonNull final Reservoir<T> item,
      @NonNull final Description mismatchDescription) {
    if (item == lastFailedMatch) {
      mismatchDescription.appendText("got ").appendValue(lastUnequalValue);
    } else {
      mismatchDescription.appendText("got a wrong value from reservoir " + item);
    }
  }
}
