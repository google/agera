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
package com.google.android.agera.test.mocks;

import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

import android.support.annotation.NonNull;
import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;
import java.util.ArrayList;
import java.util.List;

public final class MockUpdatable implements Updatable {
  private final List<Observable> observables;

  private boolean updated;

  private MockUpdatable() {
    this.observables = new ArrayList<>();
    this.updated = false;
  }

  @NonNull
  public static MockUpdatable mockUpdatable() {
    return new MockUpdatable();
  }

  @Override
  public void update() {
    updated = true;
  }

  public boolean wasUpdated() {
    runUiThreadTasksIncludingDelayedTasks();
    return updated;
  }

  public void resetUpdated() {
    runUiThreadTasksIncludingDelayedTasks();
    updated = false;
  }

  public void addToObservable(@NonNull final Observable observable) {
    observable.addUpdatable(this);
    observables.add(observable);
    runUiThreadTasksIncludingDelayedTasks();
  }

  public void removeFromObservables() {
    for (final Observable observable : observables) {
      observable.removeUpdatable(this);
    }
    observables.clear();
  }
}
