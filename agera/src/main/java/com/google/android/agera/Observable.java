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
 * Notifies added {@link Updatable}s when something happens.
 *
 * <p>Addition and removal of {@link Updatable}s has to be balanced. Multiple add of the same
 * {@link Updatable} is not allowed and shall result in an {@link IllegalStateException}. Removing
 * non-added {@link Updatable}s shall also result in an {@link IllegalStateException}.
 * Forgetting to remove an {@link Updatable} may result in memory/resource leaks.
 *
 * <p>Without any {@link Updatable}s added an {@code Observable} may temporarily be
 * <i>inactive</i>. {@code Observable} implementations that provide values, perhaps through a
 * {@link Supplier}, do not guarantee an up to date value when <i>inactive</i>. In order to ensure
 * that the {@code Observable} is <i>active</i>, add an {@link Updatable}.
 *
 * <p>Added {@link Updatable}s shall be called back on the same thread they were added from.
 */
public interface Observable {

  /**
   * Adds {@code updatable} to the {@code Observable}.
   *
   * @throws IllegalStateException if the {@link Updatable} was already added or if it was called
   * from a non-Looper thread
   */
  void addUpdatable(@NonNull Updatable updatable);

  /**
   * Removes {@code updatable} from the {@code Observable}.
   *
   * @throws IllegalStateException if the {@link Updatable} was not added
   */
  void removeUpdatable(@NonNull Updatable updatable);
}
