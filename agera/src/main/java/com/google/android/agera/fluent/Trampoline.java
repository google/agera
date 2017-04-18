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

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Makes sure tasks offered are executed in a FIFO order on an Executor.
 */
public final class Trampoline
        extends AtomicInteger
        implements Runnable {

    final Queue<Runnable> queue;

    final Executor executor;

    public Trampoline(@NonNull Queue<Runnable> queue, @NonNull Executor executor) {
        this.queue = queue;
        this.executor = executor;
    }

    @Override
    public void run() {
        do {

            queue.poll().run();

        } while (decrementAndGet() != 0);
    }

    public void offer(@NonNull Runnable task) {
        queue.offer(task);

        if (getAndIncrement() == 0) {
            executor.execute(this);
        }
    }
}
