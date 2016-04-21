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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Signals to update() are delivered on the specified looper.
 */
final class AgeraObserveOnLooper extends AgeraTracking<ObserveOnLooper> {

    final Observable source;

    final Handler handler;

    AgeraObserveOnLooper(Observable source, Looper looper) {
        this.source = source;
        this.handler = new Handler(looper);
    }

    @NonNull
    @Override
    protected ObserveOnLooper createWrapper(@NonNull Updatable updatable) {
        return new ObserveOnLooper(updatable, handler);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull ObserveOnLooper wrapper) {
        source.addUpdatable(wrapper);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull ObserveOnLooper wrapper) {
        wrapper.cancelled = true;
        source.removeUpdatable(wrapper);
    }
}

final class ObserveOnLooper
    extends AtomicLong
        implements Updatable, Runnable {

    final Updatable actual;

    final Handler handler;

    volatile boolean cancelled;

    ObserveOnLooper(Updatable actual, Handler handler) {
        this.actual = actual;
        this.handler = handler;
    }


    @Override
    public void run() {
        long c = get();

        Updatable u = actual;

        for (;;) {
            for (long i = 0; i < c; i++) {
                if (cancelled) {
                    return;
                }

                u.update();
            }

            c = addAndGet(-c);
            if (c == 0L) {
                break;
            }
        }
    }

    @Override
    public void update() {
        if (getAndIncrement() == 0) {
            handler.post(this);
        }
    }
}