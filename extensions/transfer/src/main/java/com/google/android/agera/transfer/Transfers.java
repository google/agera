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
package com.google.android.agera.transfer;

import static com.google.android.agera.Executors.currentLooperExecutor;

import com.google.android.agera.Receiver;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

public final class Transfers {
  private Transfers() {}

  public static <T> Updatable transfer(@NonNull final Executor executor,
      @NonNull final Supplier<T> supplier, @NonNull final Receiver<T> receiver) {
    final Executor currentLooperExecutor = currentLooperExecutor();
    return new Updatable() {
      @Override
      public void update() {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            final T value = supplier.get();
            currentLooperExecutor.execute(new Runnable() {
              @Override
              public void run() {
                receiver.accept(value);
              }
            });
          }
        });
      }
    };
  }
}
