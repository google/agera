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

import com.google.android.agera.Condition;
import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

/**
 * Filters update() signals if the condition evaluates to false.
 */
final class AgeraFilter extends AgeraSource<FilterUpdatable> {
    final Condition condition;

    AgeraFilter(@NonNull Observable source, @NonNull Condition condition) {
        super(source);
        this.condition = condition;
    }

    @NonNull
    @Override
    protected FilterUpdatable createWrapper(@NonNull Updatable updatable) {
        return new FilterUpdatable(updatable, condition);
    }
}

final class FilterUpdatable implements Updatable {
    final Updatable actual;

    final Condition condition;

    FilterUpdatable(Updatable actual, Condition condition) {
        this.actual = actual;
        this.condition = condition;
    }

    @Override
    public void update() {
        if (condition.applies()) {
            actual.update();
        }
    }
}
