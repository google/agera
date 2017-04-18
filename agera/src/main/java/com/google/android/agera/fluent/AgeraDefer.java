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
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

/**
 * Defers the creation of the actual Observable till an Updatable registers.
 */
final class AgeraDefer extends AgeraTracking<DeferUpdatable> {

    final Supplier<? extends Observable> supplier;

    AgeraDefer(Supplier<? extends Observable> supplier) {
        this.supplier = supplier;
    }


    @NonNull
    @Override
    protected DeferUpdatable createWrapper(@NonNull Updatable updatable) {
        Observable source = supplier.get();
        return new DeferUpdatable(updatable, source);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull DeferUpdatable wrapper) {
        wrapper.add();
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull DeferUpdatable wrapper) {
        wrapper.remove();
    }
}

final class DeferUpdatable
implements Updatable {
    final Updatable actual;

    final Observable source;

    DeferUpdatable(Updatable actual, Observable source) {
        this.actual = actual;
        this.source = source;
    }

    @Override
    public void update() {
        actual.update();
    }

    void add() {
        source.addUpdatable(this);
    }

    void remove() {
        source.removeUpdatable(this);
    }
}
