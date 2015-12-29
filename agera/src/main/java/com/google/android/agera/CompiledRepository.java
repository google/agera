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

import static com.google.android.agera.Observables.compositeObservable;
import static com.google.android.agera.Observables.perMillisecondFilterObservable;
import static com.google.android.agera.Observables.updateDispatcher;

import android.support.annotation.NonNull;

import java.util.List;

@SuppressWarnings("unchecked")
final class CompiledRepository implements Repository {
  @NonNull
  protected final CompiledRepositoryRunner runner;
  @NonNull
  private final UpdateDispatcher updateDispatcher;

  private CompiledRepository(@NonNull final CompiledRepositoryRunner runner) {
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
      @RepositoryConfig final int concurrentUpdateConfig,
      @RepositoryConfig final int deactivationConfig) {
    Observable eventSource = perMillisecondFilterObservable(frequency,
        compositeObservable(eventSources.toArray(new Observable[eventSources.size()])));
    Object[] directiveArray = directives.toArray();
    CompiledRepositoryRunner runner = new CompiledRepositoryRunner(initialValue, eventSource,
        directiveArray, notifyChecker, deactivationConfig, concurrentUpdateConfig);
    return new CompiledRepository(runner);
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
}
