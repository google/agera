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
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Given each update() signal, geretates an Observable and merges their
 * update() calls into a single sequence of update() signals.
 */
final class AgeraFlatMap extends AgeraTracking<FlatMapUpdatable> {

    final Observable source;

    final Supplier<Observable> mapper;

    AgeraFlatMap(@NonNull Observable source, Supplier<Observable> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @NonNull
    @Override
    protected FlatMapUpdatable createWrapper(@NonNull Updatable updatable) {
        return new FlatMapUpdatable(updatable, mapper);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull FlatMapUpdatable wrapper) {
        source.addUpdatable(wrapper);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull FlatMapUpdatable wrapper) {
        wrapper.removeAll();
        source.removeUpdatable(wrapper);
    }
}

final class FlatMapUpdatable
extends AtomicLong implements Updatable {
    final Updatable actual;

    final Supplier<Observable> mapper;

    List<InnerUpdatable> inner;

    FlatMapUpdatable(Updatable actual, Supplier<Observable> mapper) {
        this.actual = actual;
        this.mapper = mapper;
    }

    @Override
    public void update() {

        Observable innerSource = mapper.get();

        InnerUpdatable innerUpdatable = new InnerUpdatable(this, innerSource);

        synchronized (this) {
            List<InnerUpdatable> list = inner;
            if (list == null) {
                return;
            }
            list.add(innerUpdatable);
        }

        innerUpdatable.add();
    }

    void removeAll() {
        List<InnerUpdatable> list;

        synchronized (this) {
            list = inner;
            inner = null;
        }

        if (list != null) {
            for (InnerUpdatable innerUpdatable : list) {
                innerUpdatable.remove();
            }
        }
    }

    void innerUpdate() {
        if (getAndIncrement() == 0) {
            long c = 1;

            for (;;) {
                for (long i = 0; i < c; i++) {
                    actual.update();
                }

                c = getAndAdd(-c);
                if (c == 0L) {
                    break;
                }
            }
        }
    }
}

final class InnerUpdatable
extends AtomicInteger
implements Updatable {
    final FlatMapUpdatable parent;

    final Observable innerSource;

    static final int ADDING = 1;
    static final int ADDED = 2;
    static final int REMOVED = 3;

    InnerUpdatable(FlatMapUpdatable parent, Observable innerSource) {
        this.parent = parent;
        this.innerSource = innerSource;
    }

    @Override
    public void update() {
        parent.innerUpdate();
    }

    void add() {
        if (compareAndSet(0, ADDING)) {
            innerSource.addUpdatable(this);
            if (!compareAndSet(ADDING, ADDED)) {
                innerSource.removeUpdatable(this);
            }
        }
    }

    void remove() {
        if (getAndSet(REMOVED) == ADDED) {
            innerSource.removeUpdatable(this);
        }
    }
}
