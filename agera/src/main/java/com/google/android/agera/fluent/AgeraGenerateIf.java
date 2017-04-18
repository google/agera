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

import com.google.android.agera.Binder;
import com.google.android.agera.Condition;
import com.google.android.agera.Updatable;

/**
 * For each incoming updatable, calls a generator callback
 */
final class AgeraGenerateIf extends AgeraTracking<GeneratorTarget> {

    final Binder<Updatable, Condition> generator;

    AgeraGenerateIf(Binder<Updatable, Condition> generator) {
        this.generator = generator;
    }

    @NonNull
    @Override
    protected GeneratorTarget createWrapper(@NonNull Updatable updatable) {
        return new GeneratorTarget(updatable);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull GeneratorTarget wrapper) {
        generator.bind(wrapper, wrapper);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull GeneratorTarget wrapper) {
        wrapper.cancelled = true;
    }

}

final class GeneratorTarget implements Updatable, Condition {
    final Updatable actual;

    volatile boolean cancelled;

    GeneratorTarget(Updatable actual) {
        this.actual = actual;
    }

    @Override
    public boolean applies() {
        return !cancelled;
    }

    @Override
    public void update() {
        actual.update();
    }
}