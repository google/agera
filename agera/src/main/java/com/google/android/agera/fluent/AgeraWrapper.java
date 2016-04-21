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
 * Wraps an arbitrary Agera Observable and exposes fluent services over it.
 */
final class AgeraWrapper extends Agera {
    final Observable source;


    AgeraWrapper(@NonNull Observable source) {
        this.source = source;
    }

    @Override
    public void addUpdatable(@NonNull Updatable updatable) {
        source.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull Updatable updatable) {
        source.removeUpdatable(updatable);
    }
}
