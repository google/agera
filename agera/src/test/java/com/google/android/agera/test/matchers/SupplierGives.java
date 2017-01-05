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
import com.google.android.agera.Supplier;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class SupplierGives<T> extends TypeSafeMatcher<Supplier<T>> {
  @NonNull
  private final T value;

  private SupplierGives(@NonNull final T value) {
    this.value = value;
  }

  @Override
  protected boolean matchesSafely(@NonNull final Supplier<T> reference) {
    return value.equals(reference.get());
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("gives value ").appendValue(value);
  }

  @Override
  protected void describeMismatchSafely(final Supplier<T> supplier,
      final Description description) {
    description.appendText("was ").appendValue(supplier.get());
  }

  @NonNull
  @Factory
  public static <T> Matcher<Supplier<T>> gives(@NonNull final T value) {
    return new SupplierGives<>(value);
  }

  @NonNull
  @Factory
  public static <T> Matcher<Supplier<T>> has(@NonNull final T value) {
    return new SupplierGives<>(value);
  }
}
