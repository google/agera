package com.google.android.agera.fluent;

import android.support.annotation.NonNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;

/**
 * Takes the first N update() signals and then removes itself from the source Observable.
 */
final class AgeraTake extends AgeraSource<TakeUpdatable> {

    final long n;

    AgeraTake(@NonNull Observable source, long n) {
        super(source);
        this.n = n;
    }

    @NonNull
    @Override
    protected TakeUpdatable createWrapper(@NonNull Updatable updatable) {
        return new TakeUpdatable(updatable, n, this);
    }
}

final class TakeUpdatable implements Updatable {
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
            parent.removeUpdatable(actual);
        }
    }
}
