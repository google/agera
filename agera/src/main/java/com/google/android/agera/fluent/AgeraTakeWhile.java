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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Let's update() signals pass through while the condition returns true or removes itself
 * from the parent otherwise.
 */
final class AgeraTakeWhile extends AgeraTracking<TakeWhileUpdatable> {
    final Observable source;

    final Condition condition;

    AgeraTakeWhile(Observable source, Condition condition) {
        this.source = source;
        this.condition = condition;
    }

    @NonNull
    @Override
    protected TakeWhileUpdatable createWrapper(@NonNull Updatable updatable) {
        return new TakeWhileUpdatable(updatable, condition, this);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull TakeWhileUpdatable wrapper) {
        source.addUpdatable(wrapper);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull TakeWhileUpdatable wrapper) {
        wrapper.remove();
    }
}

final class TakeWhileUpdatable
extends AtomicBoolean implements Updatable {

    final Updatable actual;

    final Condition condition;

    final AgeraTakeWhile parent;

    TakeWhileUpdatable(Updatable actual, Condition condition, AgeraTakeWhile parent) {
        this.actual = actual;
        this.condition = condition;
        this.parent = parent;
    }


    @Override
    public void update() {
        if (condition.applies()) {
            actual.update();
        } else {
            remove();
        }
    }

    void remove() {
        if (!get() && compareAndSet(false, true)) {
            parent.removeUpdatable(actual);
        }
    }
}
