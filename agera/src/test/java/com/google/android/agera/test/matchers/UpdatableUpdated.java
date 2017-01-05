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
import com.google.android.agera.test.mocks.MockUpdatable;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

public final class UpdatableUpdated extends TypeSafeMatcher<MockUpdatable> {
  private static final UpdatableUpdated WAS_UPDATED = new UpdatableUpdated(true);
  private static final UpdatableUpdated WAS_NOT_UPDATED = new UpdatableUpdated(false);

  private final boolean updated;

  private UpdatableUpdated(final boolean updated) {
    this.updated = updated;
  }

  @Override
  protected boolean matchesSafely(@NonNull final MockUpdatable updatable) {
    return updated == updatable.wasUpdated();
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(updated ? "was updated" : "was not updated");
  }

  @Override
  protected void describeMismatchSafely(final MockUpdatable item,
      final Description mismatchDescription) {
    mismatchDescription.appendText(updated ? "was not updated" : "was updated");
  }

  @NonNull
  @Factory
  public static UpdatableUpdated wasUpdated() {
    return WAS_UPDATED;
  }

  @NonNull
  @Factory
  public static UpdatableUpdated wasNotUpdated() {
    return WAS_NOT_UPDATED;
  }
}
