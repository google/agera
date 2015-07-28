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

import static com.google.android.agera.Reservoirs.reservoirOf;

import com.google.android.agera.ReactionCompilerStates.RFlow;
import com.google.android.agera.RexCompilerStates.RConfig;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Utility methods for creating {@link Reaction} instances.
 *
 * <p>Any {@link Reaction} created by this class has to be created from a {@link Looper} thread
 * or the method will throw an {@link IllegalStateException}.
 */
public final class Reactions {

  /**
   * Starts the declaration of a compiled reaction to react to objects of the given type, which uses
   * a standard {@link Reservoirs#reservoirOf(Class) reservoir} as the backing storage. See more at
   * {@link ReactionCompilerStates}.
   */
  public static <T> RFlow<T, T, RConfig<T, Reaction<T>, ?>, ?> reactionTo(
      @Nullable final Class<T> clazz) {
    return reactionFor(reservoirOf(clazz));
  }

  /**
   * Starts the declaration of a compiled reaction to react to objects placed in the given
   * {@code reservoir}, using it as the backing storage. See more at {@link ReactionCompilerStates}.
   */
  public static <T> RFlow<T, T, RConfig<T, Reaction<T>, ?>, ?> reactionFor(
      @NonNull final Reservoir<T> reservoir) {
    return RexCompiler.reactionFor(reservoir);
  }

  private Reactions() {}
}
