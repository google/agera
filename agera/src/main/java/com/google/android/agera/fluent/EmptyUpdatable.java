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

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An empty updatable that supports cancellation via Closeable interface
 */
final class EmptyUpdatable
extends AtomicBoolean
implements Updatable, Closeable {
    Observable parent;

    EmptyUpdatable(Observable parent) {
        this.parent = parent;
    }

    @Override
    public void update() {
        // deliveberately no op
    }

    @Override
    public void close() {
        if (compareAndSet(false, true)) {
            parent.removeUpdatable(this);
            parent = null;
        }
    }
}
