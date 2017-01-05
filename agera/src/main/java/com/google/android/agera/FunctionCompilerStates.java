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
package com.google.android.agera;

import android.support.annotation.NonNull;
import java.util.Comparator;
import java.util.List;

/**
 * Container of the compiler state interfaces supporting the declaration of {@link Function}s
 * using the type-safe declarative language.
 */
public interface FunctionCompilerStates {

  /**
   * Methods allowed in both the {@link FItem} and {@link FList} compiler states.
   */
  interface FBase<TPrev, TFrom> {

    /**
     * Adds a {@link Function} to the behavior chain to be applied to the item.
     *
     * @param function the function to apply to the item
     */
    @NonNull
    <TTo> FItem<TTo, TFrom> apply(@NonNull Function<? super TPrev, TTo> function);

    /**
     * Adds a {@link Function} to the end of the behavior chain to be applied to the item.
     *
     * @param function the function to apply to the data
     */
    @NonNull
    <TTo> Function<TFrom, TTo> thenApply(@NonNull Function<? super TPrev, TTo> function);
  }

  /**
   * Compiler state allowing to specify how the {@link Function} should modify single items.
   */
  interface FItem<TPrev, TFrom> extends FBase<TPrev, TFrom> {
    /**
     * Adds a {@link Function} to the behavior chain to unpack an item into a {@link List}, allowing
     * list behaviors to be used from this point on.
     *
     * @param function the unpack function
     */
    @NonNull
    <TTo> FList<TTo, List<TTo>, TFrom> unpack(@NonNull Function<? super TPrev, List<TTo>> function);
  }

  /**
   * Compiler state allowing to specify how the {@link Function} should modify {@link List}s.
   */
  interface FList<TPrev, TPrevList, TFrom> extends FBase<TPrevList, TFrom> {

    /**
     * Adds a {@link Function} to the behavior chain to change the entire list to a new list.
     *
     * <p>The {@code morph} directive is functionally equivalent to {@code apply}, which treats the
     * input list as a single item. But {@code morph} is aware of the list-typed output and allows
     * list behaviors to follow immediately. Since the only difference between {@link #apply} and
     * {@code morph} is the next state of the compiler, {@code thenMorph} does not exist since
     * {@link #thenApply} can be used in its place.
     *
     * @param function the function to apply to the list
     */
    @NonNull
    <TTo> FList<TTo, List<TTo>, TFrom> morph(@NonNull Function<List<TPrev>, List<TTo>> function);

    /**
     * Adds a {@link Function} to the behavior chain to map each item into a new type.
     *
     * @param function the function to apply to each item to create a new list
     */
    @NonNull
    <TTo> FList<TTo, List<TTo>, TFrom> map(@NonNull Function<TPrev, TTo> function);

    /**
     * Adds a {@link Function} to the end of the behavior chain to map each item into a new type.
     *
     * @param function the function to apply to each item to create a new list
     */
    @NonNull
    <TTo> Function<TFrom, List<TTo>> thenMap(@NonNull Function<? super TPrev, TTo> function);

    /**
     * Adds a {@link Predicate} to the behavior chain to filter out items.
     *
     * @param filter the predicate to filter by
     */
    @NonNull
    FList<TPrev, TPrevList, TFrom> filter(@NonNull Predicate<? super TPrev> filter);

    /**
     * Adds a max number of item limit to the behavior chain.
     *
     * @param limit the max number of items the list is limited to
     */
    @NonNull
    FList<TPrev, TPrevList, TFrom> limit(int limit);

    /**
     * Adds a {@link Comparator} to the behavior chain to sort the items.
     *
     * @param comparator the comparator to sort the items
     */
    @NonNull
    FList<TPrev, TPrevList, TFrom> sort(@NonNull Comparator<TPrev> comparator);

    /**
     * Adds a {@link Predicate} to the end of the behavior chain to filter out items.
     *
     * @param filter the predicate to filter by
     */
    @NonNull
    Function<TFrom, TPrevList> thenFilter(@NonNull Predicate<? super TPrev> filter);

    /**
     * Adds a max number of item limit to the end of the behavior chain.
     *
     * @param limit the max number of items the list is limited to
     */
    @NonNull
    Function<TFrom, TPrevList> thenLimit(int limit);

    /**
     * Adds a {@link Comparator} to the behavior chain to sort the items.
     *
     * @param comparator the comparator to sort the items
     */
    @NonNull
    Function<TFrom, TPrevList> thenSort(@NonNull Comparator<TPrev> comparator);
  }
}
