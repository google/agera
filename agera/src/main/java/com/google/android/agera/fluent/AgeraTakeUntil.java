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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Let's the main source's update() signals through until the secondary
 * Observable signals its first update().
 */
final class AgeraTakeUntil extends AgeraTracking<TakeUntilUpdatable> {
    final Observable source;

    final Observable other;

    AgeraTakeUntil(Observable source, Observable other) {
        this.source = source;
        this.other = other;
    }

    @NonNull
    @Override
    protected TakeUntilUpdatable createWrapper(@NonNull Updatable updatable) {
        return new TakeUntilUpdatable(updatable, this, other);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull TakeUntilUpdatable wrapper) {
        wrapper.connect(source);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull TakeUntilUpdatable wrapper) {
        wrapper.disconnect();
    }
}

final class TakeUntilUpdatable
extends AtomicInteger
implements Updatable {
    final Updatable actual;

    final AgeraTakeUntil parent;

    final Observable other;

    final Updatable otherUpdatable;

    static final int ADDING_OTHER = 1;
    static final int ADDING_MAIN = 2;
    static final int ADDED = 3;
    static final int REMOVED = 4;

    TakeUntilUpdatable(Updatable actual, AgeraTakeUntil parent, Observable other) {
        this.actual = actual;
        this.parent = parent;
        this.other = other;
        this.otherUpdatable = new Updatable() {
            @Override
            public void update() {
                updateOther();
            }
        };
    }

    @Override
    public void update() {
        actual.update();
    }

    void updateOther() {
        if (getAndSet(REMOVED) == ADDED) {
            parent.removeUpdatable(actual);
            other.removeUpdatable(otherUpdatable);
        }
    }

    void connect(Observable source) {
        if (compareAndSet(0, ADDING_OTHER)) {
            other.addUpdatable(otherUpdatable);
            if (compareAndSet(ADDING_OTHER, ADDING_MAIN)) {
                source.addUpdatable(this);
                if (!compareAndSet(ADDING_MAIN, ADDED)) {
                    parent.removeUpdatable(actual);
                    other.removeUpdatable(otherUpdatable);
                }
            } else {
                other.removeUpdatable(otherUpdatable);
            }
        }
    }

    void disconnect() {
        if (getAndSet(REMOVED) == ADDED) {
            other.removeUpdatable(otherUpdatable);
        }
    }
}
