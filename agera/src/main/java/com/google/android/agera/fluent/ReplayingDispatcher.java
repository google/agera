package com.google.android.agera.fluent;

import android.support.annotation.NonNull;

import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Replays all update() signals to Updatables that have been received
 * by the ReplayingDispatcher.
 */
public final class ReplayingDispatcher extends AgeraTracking<ReplayingUpdatable> implements UpdateDispatcher {

    final AtomicLong count = new AtomicLong();

    @NonNull
    @Override
    protected ReplayingUpdatable createWrapper(@NonNull Updatable updatable) {
        return new ReplayingUpdatable(updatable, this);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull ReplayingUpdatable wrapper) {
        wrapper.drain();
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull ReplayingUpdatable wrapper) {
        wrapper.cancelled = true;
    }

    @Override
    public void update() {
        count.getAndIncrement();
        for (ReplayingUpdatable bu : map.values()) {
            bu.drain();
        }
    }
}

final class ReplayingUpdatable
extends AtomicInteger {
    final Updatable actual;

    final ReplayingDispatcher parent;

    volatile boolean cancelled;

    long index;


    ReplayingUpdatable(Updatable actual, ReplayingDispatcher parent) {
        this.actual = actual;
        this.parent = parent;
    }

    void drain() {
        if (getAndIncrement() != 0) {
            return;
        }

        int missed = 1;

        for (;;) {

            long i = index;
            long r = parent.count.get();

            while (i != r) {
                if (cancelled) {
                    break;
                }
                actual.update();
                i++;
            }

            if (cancelled) {
                break;
            }

            index = i;

            missed = addAndGet(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

}

