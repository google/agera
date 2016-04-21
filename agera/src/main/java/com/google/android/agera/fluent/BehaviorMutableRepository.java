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

import com.google.android.agera.MutableRepository;
import com.google.android.agera.Updatable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds onto a value and signals Updatables if the value is updated.
 */
public final class BehaviorMutableRepository<T> extends AgeraTracking<BehaviorUpdatable> implements MutableRepository<T> {

    volatile T value;

    @NonNull
    @Override
    protected BehaviorUpdatable createWrapper(@NonNull Updatable updatable) {
        return new BehaviorUpdatable(updatable);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull BehaviorUpdatable wrapper) {
        if (value != null) {
            wrapper.update();
        }
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull BehaviorUpdatable wrapper) {
        wrapper.cancelled = true;
    }

    @Override
    public void accept(@NonNull T value) {
        this.value = value;
        for (BehaviorUpdatable bu : map.values()) {
            bu.update();
        }
    }

    @NonNull
    @Override
    public T get() {
        return value;
    }
}

final class BehaviorUpdatable
extends AtomicLong
implements Updatable {

    final Updatable actual;

    volatile boolean cancelled;

    BehaviorUpdatable(Updatable actual) {
        this.actual = actual;
    }

    @Override
    public void update() {
        if (getAndIncrement() == 0) {
            long c = 1;

            for (;;) {
                for (long i = 0; i < c; i++) {
                    if (cancelled) {
                        return;
                    }
                    actual.update();
                }

                c = addAndGet(-c);
                if (c == 0L) {
                    break;
                }
            }
        }
    }
}


