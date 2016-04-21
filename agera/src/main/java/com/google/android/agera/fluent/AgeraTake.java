package com.google.android.agera.fluent;

import android.support.annotation.NonNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes the first N update() signals and then removes itself from the source Observable.
 */
final class AgeraTake extends AgeraTracking<TakeUpdatable> {

    final Observable source;

    final long n;

    AgeraTake(@NonNull Observable source, long n) {
        this.source = source;
        this.n = n;
    }

    @NonNull
    @Override
    protected TakeUpdatable createWrapper(@NonNull Updatable updatable) {
        return new TakeUpdatable(updatable, n, this);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull TakeUpdatable wrapper) {
        source.addUpdatable(wrapper);
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull TakeUpdatable wrapper) {
        wrapper.remove();
    }
}

final class TakeUpdatable
extends AtomicBoolean
implements Updatable {
    final Updatable actual;

    final Observable parent;

    long remaining;

    TakeUpdatable(Updatable actual, long n, Observable parent) {
        this.actual = actual;
        this.remaining = n;
        this.parent = parent;
    }


    @Override
    public void update() {
        long r = remaining;

        if (r > 0L) {
            actual.update();
            remaining = --r;
        }
        if (r == 0) {
            remove();
        }
    }

    void remove() {
        if (!get() && compareAndSet(false, true)) {
            parent.removeUpdatable(actual);
        }
    }
}
