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

import static com.google.android.agera.Mergers.staticMerger;
import static com.google.android.agera.Observables.compositeObservable;
import static com.google.android.agera.Observables.perMillisecondFilterObservable;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Preconditions.checkState;
import static java.lang.Boolean.TRUE;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

@SuppressWarnings("unchecked")
final class Rex implements Repository, Reaction {
  private static final Merger<Object, Object, Boolean> ALWAYS_NOTIFY = staticMerger(TRUE);
  @Nullable
  private final Receiver reservoir;
  @NonNull
  protected final RexRunner runner;
  @NonNull
  private final UpdateDispatcher updateDispatcher;

  private Rex(@NonNull final RexRunner runner, final @Nullable Receiver reservoir) {
    this.reservoir = reservoir;
    this.runner = runner;
    this.updateDispatcher = updateDispatcher(runner);
    runner.setUpdateDispatcher(updateDispatcher);
  }

  @NonNull
  static Repository compiledRepository(
      @NonNull final Object initialValue,
      @NonNull final List<Observable> eventSources,
      final int frequency,
      @NonNull final List<Object> directives,
      @NonNull final Merger<Object, Object, Boolean> notifyChecker,
      final @RexConfig int concurrentUpdateConfig,
      final @RexConfig int deactivationConfig) {
    Observable eventSource = perMillisecondFilterObservable(frequency,
        compositeObservable(eventSources.toArray(new Observable[eventSources.size()])));
    Object[] directiveArray = directives.toArray();
    RexRunner runner = new RexRunner(initialValue, eventSource, directiveArray,
        notifyChecker, deactivationConfig, concurrentUpdateConfig);
    return new Rex(runner, null);
  }

  @NonNull
  static Reaction compiledReaction(
      @NonNull final Object triggerValue,
      @NonNull final Reservoir reservoir,
      @NonNull final List<Object> directives,
      final @RexConfig int concurrentUpdateConfig,
      final @RexConfig int deactivationConfig) {
    Object[] directiveArray = directives.toArray();
    RexRunner runner = new RexRunner(triggerValue, reservoir, directiveArray,
        ALWAYS_NOTIFY, deactivationConfig, concurrentUpdateConfig);
    return new Rex(runner, reservoir);
  }

  @Override
  public void addUpdatable(@NonNull final Updatable updatable) {
    updateDispatcher.addUpdatable(updatable);
  }

  @Override
  public void removeUpdatable(@NonNull final Updatable updatable) {
    updateDispatcher.removeUpdatable(updatable);
  }

  @NonNull
  @Override
  public Object get() {
    return runner.getValue();
  }

  @Override
  public void accept(@NonNull Object value) {
    checkState(reservoir != null, "Invalid use of repository as reaction");
    reservoir.accept(value);
  }
}
