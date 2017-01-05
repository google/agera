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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.concurrent.Executor;

public final class SingleSlotDelayedExecutor implements Executor {
  @Nullable
  private Runnable runnable;

  @Override
  public void execute(@NonNull final Runnable command) {
    assertThat("delayedExecutor cannot queue more than one Runnable", runnable, is(nullValue()));
    runnable = command;
  }

  public boolean hasRunnable() {
    return runnable != null;
  }

  public void resumeOrThrow() {
    final Runnable runnable = this.runnable;
    assertThat("delayedExecutor should have queued a Runnable for resumeOrThrow()",
        runnable, is(notNullValue()));
    this.runnable = null;
    //noinspection ConstantConditions
    runnable.run();
  }
}
