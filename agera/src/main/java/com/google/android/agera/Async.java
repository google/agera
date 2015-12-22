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
 * Encapsulates an operation that produces a result for each input asynchronously.
 */
public interface Async<TFrom, TTo> {

  /**
   * Performs the asynchronous operation on the given {@code input} value and eventually supplies
   * the output value to the given {@code outputReceiver}.
   *
   * @param outputReceiver A {@link Receiver} to receive the output value. The lifetime of an
   *     asynchronous operation starts from the beginning of this method call and ends at the
   *     invocation of {@link Receiver#accept} on this receiver. Outside this lifetime, using any
   *     objects given to this method can lead to undefined behavior.
   * @param cancelled A {@link Condition} allowing the implementation to check whether the client of
   *     this asynchronous operation has requested cancellation. The asynchronous operation still
   *     must call {@link Receiver#accept} to acknowledge the end of the operation, and therefore a
   *     value type able to represent a cancelled operation is required to take advantage of this
   *     signal, for example {@link Result}.
   */
  void async(@NonNull TFrom input, @NonNull Receiver<TTo> outputReceiver,
      @NonNull Condition cancelled);
}
