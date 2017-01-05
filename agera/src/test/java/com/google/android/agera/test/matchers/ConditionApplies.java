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
import com.google.android.agera.Condition;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public final class ConditionApplies extends TypeSafeMatcher<Condition> {
  private final boolean value;

  private ConditionApplies(final boolean value) {
    this.value = value;
  }

  @Override
  protected boolean matchesSafely(final Condition condition) {
    return condition.applies() == value;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(value ? "applies " : "does not apply");
  }

  @NonNull
  @Factory
  public static Matcher<Condition> applies() {
    return new ConditionApplies(true);
  }

  @NonNull
  @Factory
  public static Matcher<Condition> doesNotApply() {
    return new ConditionApplies(false);
  }
}
