/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.google.android.agera.rvdatabinding;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.google.android.agera.Function;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryAdapter;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPDiff;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPItemCompile;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import java.util.List;

/**
 * Container of the compiler state interfaces supporting the creation of a data binding
 * {@link RepositoryPresenter}.
 */
public interface DataBindingRepositoryPresenterCompilerStates {

  /**
   * Compiler state to specify how to bind the {@code View} using data binding.
   */
  interface DBRPMain<T> extends RPItemCompile<T>, RPDiff<T, DBRPMain<T>> {

    /**
     * Specifies a data binding {@code itemId} from the previously given {@code layout} to bind a
     * single item in the {@link Repository}.
     */
    @NonNull
    DBRPMain<T> itemId(int itemId);

    /**
     * Specifies a {@link Function} to return a data binding {@code itemId} from the previously
     * given {@code layout} to bind a single item in the {@link Repository}.
     */
    @NonNull
    DBRPMain<T> itemIdForItem(@NonNull Function<T, Integer> itemIdForItem);

    /**
     * Specifies what {@code handler} is associated with the {@code handlerId} in the previously
     * given {@code layout}.
     */
    @NonNull
    DBRPMain<T> handler(int handlerId, @NonNull Object handler);

    /**
     * Specifies what {@code handler} is associated with the {@code handlerId} in the previously
     * given {@code layout}.
     */
    @NonNull
    DBRPMain<T> onRecycle(@RecycleConfig int recycleConfig);

    /**
     * Specifies a data binding {@code itemId} from the previously given {@code layout} to bind the
     * whole collection (the repository value) to.
     */
    @NonNull
    DBRPMain<T> collectionId(int collectionId);

    /**
     * Specifies a {@link Function} providing a stable id for the given item. Called only if stable
     * IDs are enabled with {@link RepositoryAdapter#setHasStableIds}, and therefore this method is
     * optional with a default implementation of returning {@link RecyclerView#NO_ID}. If stable IDs
     * are enabled, the returned ID and the layout returned by
     * {@link RPLayout#layoutForItem(Function)} or {@link RPLayout#layout(int)} for the given item
     * should together uniquely identify this item in the whole {@link RecyclerView} throughout all
     * changes.
     */
    @NonNull
    DBRPMain<T> stableIdForItem(@NonNull Function<? super T, Long> stableIdForItem);

    /**
     * Specifies a {@code stable:Id} for the given item. Called only if stable IDs are enabled with
     * {@link RepositoryAdapter#setHasStableIds}, and therefore this method is optional with a
     * default implementation of returning {@link RecyclerView#NO_ID}. If stable IDs are enabled,
     * the returned ID and the layout returned by {@link RPLayout#layoutForItem(Function)} or
     * {@link RPLayout#layout(int)} for the given item should together uniquely identify this item
     * in the whole {@link RecyclerView} throughout all changes.
     */
    @NonNull
    RPItemCompile<T> stableId(long stableId);

    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a {@link List} where each
     * item in the {@link List} will be bound to the {@link RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<List<T>> forList();

    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a {@link Result} containing
     * a {@link List} where each item in the {@link List} will be bound to the {@link
     * RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<Result<List<T>>> forResultList();

    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a type that can be
     * converted to a {@link List} of items using the {@code converter}.
     */
    @NonNull
    <TCol> RepositoryPresenter<TCol> forCollection(@NonNull Function<TCol, List<T>> converter);
  }
}

