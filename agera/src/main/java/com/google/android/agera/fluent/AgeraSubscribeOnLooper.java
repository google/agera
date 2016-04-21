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

/**
 * Subscribes on a specified looper.
 */
final class AgeraSubscribeOnLooper extends Agera {

    final Observable source;

    final Handler handler;

    AgeraSubscribeOnLooper(Observable source, Looper looper) {
        this.source = source;
        this.handler = new Handler(looper);
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                source.addUpdatable(updatable);
            }
        });
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                source.removeUpdatable(updatable);
            }
        });
    }
}
