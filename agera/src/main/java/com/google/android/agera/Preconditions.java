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

import android.support.annotation.NonNull;

/**
 * Precondition checks.
 */
public final class Preconditions {
  public static void checkState(final boolean expression, @NonNull final String errorMessage) {
    if (!expression) {
      throw new IllegalStateException(errorMessage);
    }
  }

  public static void checkArgument(final boolean expression, @NonNull final String errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  @SuppressWarnings("ConstantConditions")
  @NonNull
  public static <T> T checkNotNull(@NonNull final T object) {
    if (object == null) {
      throw new NullPointerException();
    }
    return object;
  }

  private Preconditions() {}
}
