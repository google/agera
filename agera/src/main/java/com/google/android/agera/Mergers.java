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
import com.google.android.agera.Common.StaticProducer;

/**
 * Utility methods for obtaining {@link Merger} instances.
 */
public final class Mergers {

  private static final ObjectsUnequalMerger OBJECTS_UNEQUAL_MERGER = new ObjectsUnequalMerger();

  /**
   * Returns a {@link Merger} that outputs the given {@code value} regardless of the input values.
   */
  @NonNull
  public static <TFirst, TSecond, TTo> Merger<TFirst, TSecond, TTo> staticMerger(
      @NonNull final TTo value) {
    return new StaticProducer<>(value);
  }

  /**
   * Returns a {@link Merger} that outputs the <i>negated</i> result of {@link Object#equals} called
   * on the first input value, using the second input value as the argument of that call.
   */
  @NonNull
  public static Merger<Object, Object, Boolean> objectsUnequal() {
    return OBJECTS_UNEQUAL_MERGER;
  }

  private static final class ObjectsUnequalMerger implements Merger<Object, Object, Boolean> {
    @NonNull
    @Override
    public Boolean merge(@NonNull final Object oldValue, @NonNull final Object newValue) {
      return !oldValue.equals(newValue);
    }
  }

  private Mergers() {}
}
