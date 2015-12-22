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
package com.google.android.agera.requester;

import static com.google.android.agera.Preconditions.checkNotNull;

import com.google.android.agera.Receiver;
import com.google.android.agera.Supplier;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

public final class Requesters {

  public static <T> Requester<T> executorRequester(@NonNull final Executor executor,
      @NonNull final Supplier<T> supplier) {
    return new ExecutorRequester<>(executor, supplier);
  }

  private static final class ExecutorRequester<T> implements Requester<T> {
    @NonNull
    private final Executor executor;
    @NonNull
    private final Supplier<T> supplier;

    public ExecutorRequester(@NonNull final Executor executor,
        @NonNull final Supplier<T> supplier) {
      this.executor = checkNotNull(executor);
      this.supplier = checkNotNull(supplier);
    }

    @Override
    public void request(@NonNull final T input, @NonNull final Receiver<T> callback) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          callback.accept(supplier.get());
        }
      });
    }
  }

  private Requesters() {}
}
