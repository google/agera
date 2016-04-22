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
package com.google.android.agera.fluent;

import android.support.annotation.NonNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

/**
 * Executes a Runnable action on an update() signal.
 */
final class AgeraDoOnUpdate extends AgeraSource<DoOnUpdateUpdatable> {
    final Runnable run;

    AgeraDoOnUpdate(@NonNull Observable source, @NonNull Runnable run) {
        super(source);
        this.run = run;
    }

    @NonNull
    @Override
    protected DoOnUpdateUpdatable createWrapper(@NonNull Updatable updatable) {
        return new DoOnUpdateUpdatable(updatable, run);
    }
}

final class DoOnUpdateUpdatable implements Updatable {
    final Updatable actual;

    final Runnable run;

    DoOnUpdateUpdatable(Updatable actual, Runnable run) {
        this.actual = actual;
        this.run = run;
    }

    @Override
    public void update() {
        run.run();

        actual.update();
    }
}
