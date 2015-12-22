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
package com.google.android.agera.test;

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import com.google.android.agera.Async;
import com.google.android.agera.Condition;
import com.google.android.agera.Receiver;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class MockAsync<TFrom, TTo> implements Async<TFrom, TTo> {
  @Nullable
  private TFrom input;
  @Nullable
  private Receiver<TTo> outputReceiver;
  @Nullable
  private Condition cancelled;
  @Nullable
  private Throwable throwableForStackTrace;

  private MockAsync() {}

  public static <TFrom, TTo> MockAsync<TFrom, TTo> mockAsync() {
    return new MockAsync<>();
  }

  @Override
  public void async(@NonNull TFrom input, @NonNull Receiver<TTo> outputReceiver,
      @NonNull Condition cancelled) {
    if (this.input != null) {
      throw new IllegalStateException("MockAsync cannot queue more than one run",
          throwableForStackTrace);
    }

    this.input = checkNotNull(input);
    this.outputReceiver = checkNotNull(outputReceiver);
    this.cancelled = checkNotNull(cancelled);
    this.throwableForStackTrace = new Throwable("Last queued at this stack trace");
  }

  public boolean wasCalled() {
    return input != null;
  }

  @NonNull
  public Condition cancelled() {
    return checkNotNull(cancelled);
  }

  public void expectAndOutput(@NonNull final TFrom expectedInput, @NonNull final TTo outputValue) {
    checkState(expectedInput.equals(input), "Input mismatch");
    checkNotNull(outputValue);
    Receiver<TTo> outputReceiver = checkNotNull(this.outputReceiver);

    input = null;
    this.outputReceiver = null;
    cancelled = null;
    throwableForStackTrace = null;

    outputReceiver.accept(outputValue);
  }
}
