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
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class HasPrivateConstructor extends TypeSafeMatcher<Class<?>> {
  private static final HasPrivateConstructor INSTANCE = new HasPrivateConstructor();

  private HasPrivateConstructor() {
  }

  @Override
  protected boolean matchesSafely(final Class<?> clazz) {
    try {
      final Constructor<?> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      constructor.newInstance();
      return Modifier.isPrivate(constructor.getModifiers());
    } catch (final Exception ignored) {
    }
    return false;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("should have private constructor");
  }

  @NonNull
  @Factory
  public static Matcher<Class<?>> hasPrivateConstructor() {
    return INSTANCE;
  }
}
