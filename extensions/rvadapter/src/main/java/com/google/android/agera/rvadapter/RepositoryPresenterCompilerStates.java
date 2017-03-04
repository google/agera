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
package com.google.android.agera.rvadapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import java.util.List;

/**
 * Container of the compiler state interfaces supporting the creation of a {@link
 * RepositoryPresenter}.
 */
public interface RepositoryPresenterCompilerStates {

  /**
   * Compiler state to specify what layout to use.
   */
  interface RPLayout<TVal, TRet> {

    /**
     * Specifies the {@code layoutId} for the layout to inflate for each item in the associated
     * {@link Repository}.
     */
    @NonNull
    TRet layout(@LayoutRes int layoutId);

    /**
     * Specifies a {@link Function} to return a @{code layoutId} that be inflated given the item in
     * the {@link Repository}.
     */
    @NonNull
    TRet layoutForItem(@NonNull Function<TVal, Integer> layoutForItem);
  }

  /**
   * Compiler state to compile for the specified item container type of the associated
   * {@link Repository}.
   */
  interface RPItemCompile<TVal> {

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a single item that will be
     * bound to the {@link RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<TVal> forItem();

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link Result} where the
     * item in the {@link Result} will be bound to the @{link RecyclerView} if present.
     */
    @NonNull
    RepositoryPresenter<Result<TVal>> forResult();
  }

  /**
   * Compiler state to compile for the pre-defined collection container type of the associated
   * {@link Repository}.
   */
  interface RPSpecificCollectionCompile<TVal> {

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link List} where each
     * item in the {@link List} will be bound to the {@link RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<List<TVal>> forList();

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link Result} containing
     * a {@link List} where each item in the {@link List} will be bound to the {@link
     * RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<Result<List<TVal>>> forResultList();
  }

  /**
   * Compiler state to compile for the generic collection container type of the associated
   * {@link Repository}.
   */
  interface RPCollectionCompile<TVal> {
    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a type that can be
     * converted to a {@link List} of items using the {@code converter}.
     */
    @NonNull
    <TCol> RepositoryPresenter<TCol> forCollection(@NonNull Function<TCol, List<TVal>> converter);
  }

  /**
   * Compiler state to compile for the generic collection container type of the associated
   * {@link Repository}.
   */
  interface RPTypedCollectionCompile<TVal, TCol> {
    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a type that can be
     * converted to a {@link List} of items using the {@code converter}.
     */
    @NonNull
    RepositoryPresenter<TCol> forCollection(@NonNull Function<TCol, List<TVal>> converter);
  }

  /**
   * Compiler state to specify how to bind the {@link Repository} item to the view inflated by the
   * layout.
   */
  interface RPMain<T> extends RPItemCompile<T>,
      RPSpecificCollectionCompile<T>, RPCollectionCompile<T> {

    /**
     * Specifies a {@link Binder} to bind a single item in the {@link Repository} to an inflated
     * {@code View}.
     */
    @NonNull
    RPMain<T> bindWith(@NonNull Binder<T, View> viewBinder);

    /**
     * Specifies a {@link Receiver} to recycle the {@code View}.
     */
    @NonNull
    RPMain<T> recycleWith(@NonNull Receiver<View> recycler);

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
    RPMain<T> stableIdForItem(@NonNull Function<? super T, Long> stableIdForItem);

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
     * Specifies a {@link Binder} to bind a single item in the {@link Repository} to an inflated
     * {@code View}.
     */
    @NonNull
    <TCol> RPTypedCollectionCompile<T, TCol> bindCollectionWith(
        @NonNull Binder<TCol, View> collectionBinder);
  }
}
