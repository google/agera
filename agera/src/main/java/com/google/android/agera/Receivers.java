/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.android.agera.Common.NULL_OPERATOR;

import android.support.annotation.NonNull;

/**
 * Utility methods for obtaining {@link Receiver} instances.
 */
public final class Receivers {
  
  /**
   * Returns a {@link Receiver} that does nothing.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static <T> Receiver<T> nullReceiver() {
    return NULL_OPERATOR;
  }

  private Receivers() {}
}
