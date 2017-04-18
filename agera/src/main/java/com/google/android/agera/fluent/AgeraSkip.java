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
 * Skips the first N signals.
 */
final class AgeraSkip extends AgeraSource<SkipUpdatable> {
    final long n;

    AgeraSkip(Observable source, long n) {
        super(source);
        this.n = n;
    }

    @NonNull
    @Override
    protected SkipUpdatable createWrapper(@NonNull Updatable updatable) {
        return new SkipUpdatable(updatable, n);
    }
}

final class SkipUpdatable implements Updatable {
    final Updatable actual;
    long remaining;

    SkipUpdatable(Updatable actual, long remaining) {
        this.actual = actual;
        this.remaining = remaining;
    }

    @Override
    public void update() {
        long r = remaining;
        if (r == 0L) {
            actual.update();
            return;
        }
        remaining = r - 1;
    }
}
