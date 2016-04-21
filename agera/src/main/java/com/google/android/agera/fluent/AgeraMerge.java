package com.google.android.agera.fluent;

import android.support.annotation.NonNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

/**
 * Merges the update() calls of many Observable sources.
 */
final class AgeraMerge extends AgeraTracking<FlatMapUpdatable> {
    final Observable[] sources;

    AgeraMerge(Observable[] sources) {
        this.sources = sources;
    }

    @NonNull
    @Override
    protected FlatMapUpdatable createWrapper(@NonNull Updatable updatable) {
        Supplier<Observable> supplier = new Supplier<Observable>() {
            int i;
            @NonNull
            @Override
            public Observable get() {
                return sources[i++];
            }
        };
        return new FlatMapUpdatable(updatable, supplier);
    }

    @Override
    protected void onAdd(@NonNull Updatable updatable, @NonNull FlatMapUpdatable wrapper) {
        for (int i = 0; i < sources.length; i++) {
            wrapper.update();
        }
    }

    @Override
    protected void onRemove(@NonNull Updatable updatable, @NonNull FlatMapUpdatable wrapper) {
        wrapper.removeAll();
    }
}

