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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Subscribes on the given ExecutorService to the wrapper source.
 */
final class AgeraSubscribeOnExecutor extends AgeraTracking<Trampoline> {

    final Observable source;

    final Executor executor;

    AgeraSubscribeOnExecutor(Observable source, Executor executor) {
        this.source = source;
        this.executor = executor;
    }


    @NonNull
    @Override
    protected Trampoline createWrapper(@NonNull Updatable updatable) {
        return new Trampoline(new ConcurrentLinkedQueue<Runnable>(), executor);
    }

    @Override
    protected void onAdd(@NonNull final Updatable updatable, @NonNull Trampoline wrapper) {
        wrapper.offer(new Runnable() {
            @Override
            public void run() {
                source.addUpdatable(updatable);
            }
        });
    }

    @Override
    protected void onRemove(@NonNull final Updatable updatable, @NonNull Trampoline wrapper) {
        wrapper.offer(new Runnable() {
            @Override
            public void run() {
                source.removeUpdatable(updatable);
            }
        });
    }
}

