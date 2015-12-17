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
 * A supplier of data. Semantically, this could be a factory, generator, builder, or something else
 * entirely. No guarantees are implied by this interface.
 */
public interface Supplier<T> {

  /**
   * Returns an instance of the appropriate type. The returned object may or may not be a new
   * instance, depending on the implementation.
   */
  @NonNull
  T get();
}
