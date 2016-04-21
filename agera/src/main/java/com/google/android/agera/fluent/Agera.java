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

import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.agera.Binder;
import com.google.android.agera.Condition;
import com.google.android.agera.Function;
import com.google.android.agera.Observable;
import com.google.android.agera.Receiver;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

import java.util.concurrent.Executor;

/**
 * Fluent API entry point and base class.
 */
public abstract class Agera implements Observable {

    // ************************************************************************
    // Factory methods
    // ************************************************************************

    /**
     * Returns an Agera which signals exactly one update().
     * @return the Agera instance
     */
    public static Agera just() {
        return new AgeraJust();
    }

    /**
     * Returns an Agera which calls the update() the given number of times.
     * @param count the number of times to signal
     * @return the Agera instance
     */
    public static Agera range(int count) {
        return new AgeraRange(count);
    }

    /**
     * Returns an Agera which never calls update().
     * @return the Agera instance
     */
    public static Agera empty() {
        return new AgeraEmpty();
    }

    /**
     * Wraps a generic Observable into an Agera or returns it if it is already an Agera.
     * @param observable the Observable to wrap
     * @return the Agera instance
     */
    public static Agera wrap(@NonNull Observable observable) {
        if (observable instanceof Agera) {
            return (Agera)observable;
        }
        return new AgeraWrapper(observable);
    }

    /**
     * For each updatable, the given receiver is called which then can call
     * update() as it sees fit.
     * @param generator the generator that receives each updatable
     * @return the Agera instance
     */
    public static Agera generate(@NonNull Receiver<Updatable> generator) {
        return new AgeraGenerate(generator);
    }

    /**
     * For each updatable, the given receiver is called with the updatable
     * and a condition telling if the Updatable is still interested in
     * receiving update() calls.
     *
     * @param generator the generator that receives each updatable and condition pairs
     * @return the Agera instance
     */
    public static Agera generate(@NonNull Binder<Updatable, Condition> generator) {
        return new AgeraGenerateIf(generator);
    }

    /**
     * Calls the supplier to return an Observable for each registering Updatable
     * and registers the Updatable with that specific Observable.
     * @param supplier the supplier of Observables
     * @return the Agera instance
     */
    public static Agera defer(@NonNull Supplier<? extends Observable> supplier) {
        return new AgeraDefer(supplier);
    }

    /**
     * Merges the update() signals from all source Observables.
     * @param sources the source Observables
     * @return the new Agera instance
     */
    public static Agera merge(@NonNull Observable... sources) {
        if (sources.length == 0) {
            return empty();
        } else
        if (sources.length == 1) {
            return wrap(sources[0]);
        }
        return new AgeraMerge(sources);
    }

    // ************************************************************************
    // Instance methods
    // ************************************************************************

    /**
     * Makes sure the addUpdatable and removeUpdatable for this Agera is called
     * on the main thread.
     * @return the Agera instance
     */
    public final Agera subscribeOnMain() {
        return subscribeOn(Looper.getMainLooper());
    }

    /**
     * Makes sure the addUpdatable and removeUpdatable for this Agera is called
     * on the specified Executor.
     * @param executor the Executor to use
     * @return the Agera instance
     */
    public final Agera subscribeOn(@NonNull Executor executor) {
        return new AgeraSubscribeOnExecutor(this, executor);
    }

    /**
     * Makes sure the addUpdatable and removeUpdatable for this Agera is called
     * on the specified Looper.
     * @param looper the looper to use
     * @return the Agera instance
     */
    public final Agera subscribeOn(@NonNull Looper looper) {
        return new AgeraSubscribeOnLooper(this, looper);
    }

    /**
     * Makes sure update() signals are called on the main thread.
     * @return the Agera instance
     */
    public final Agera observeOnMain() {
        return observeOn(Looper.getMainLooper());
    }

    /**
     * Makes sure update() signals are called on the specified Executor.
     * @param executor the Executor to use
     * @return the Agera instance
     */
    public final Agera observeOn(@NonNull Executor executor) {
        return new AgeraObserveOnExecutor(this, executor);
    }

    /**
     * Makes sure update() signals are called on the specified Looper.
     * @param looper the Looper to use
     * @return the Agera instance
     */
    public final Agera observeOn(@NonNull Looper looper) {
        return new AgeraObserveOnLooper(this, looper);
    }

    /**
     * Skips the first N update() signals.
     * @param n the number of signals to skip
     * @return the Agera instance
     */
    public final Agera skip(long n) {
        return new AgeraSkip(this, n);
    }

    /**
     * Takes the first N update() signals and disconnects the Updatable.
     * @param limit the number of signals to let through
     * @return the Agera instance
     */
    public final Agera take(long limit) {
        return new AgeraTake(this, limit);
    }

    /**
     * Let's the update() signal pass through if the condition holds.
     * @param condition the condition to check before each update() signal is forwarded
     * @return the Agera instance
     */
    public final Agera filter(@NonNull Condition condition) {
        return new AgeraFilter(this, condition);
    }

    /**
     * For each input update() signal, asks the Supplier for an Observable,
     * registers with it and merges update() signals from these inner Observables
     * into a sequence of update() calls.
     * @param mapper the supplier that returns an Observable, called for each main update() signal
     * @return the Agera instance
     */
    public final Agera flatMap(@NonNull Supplier<Observable> mapper) {
        return new AgeraFlatMap(this, mapper);
    }

    /**
     * Allows a fluent conversion from this Agera into an arbitrary type via a converter function.
     * @param converter the converter, receives this Agera as its parameter
     * @param <U> the result type
     * @return the result returned by the converter function
     */
    public final <U> U as(@NonNull Function<? super Agera, ? extends U> converter) {
        return converter.apply(this);
    }

    /**
     * Allows fluent composition via a composer function that receives this Agera
     * instance and must return some Agera instance.
     *
     * @param composer the function that receives this Agera and must return some Agera instance
     * @return the Agera returned by the composer function
     */
    public final Agera compose(@NonNull Function<? super Agera, ? extends Agera> composer) {
        return as(composer);
    }
}
