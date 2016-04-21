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

import com.google.android.agera.Receiver;
import com.google.android.agera.Updatable;

/**
 * For each incoming updatable, calls a generator callback
 */
final class AgeraGenerate extends AgeraTracking<Updatable> {

    final Receiver<Updatable> generator;

    AgeraGenerate(Receiver<Updatable> generator) {
        this.generator = generator;
    }


    @NonNull
    @Override
    protected Updatable createWrapper(@NonNull Updatable updatable) {
        return updatable;
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull Updatable wrapper) {
        generator.accept(updatable);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull Updatable wrapper) {
        // nothing to do
    }
}
