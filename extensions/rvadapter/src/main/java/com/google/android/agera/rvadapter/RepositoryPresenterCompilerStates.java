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
import android.support.v7.util.DiffUtil;
import android.support.v7.util.DiffUtil.Callback;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryAdapter.Builder;
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
     * Specifies a {@link Function} to return a {@code layoutId} that be inflated given the item in
     * the {@link Repository}.
     */
    @NonNull
    TRet layoutForItem(@NonNull Function<TVal, Integer> layoutForItem);
  }

  /**
   * Compiler state to optionally enable fine-grained events using {@link DiffUtil}.
   */
  interface RPDiff<TVal, TRet> {

    /**
     * Specifies a key extractor {@link Function} to enable generating fine-grained events using
     * {@link DiffUtil}. Two item objects are {@linkplain Callback#areItemsTheSame the same item} if
     * their keys are {@linkplain Object#equals equal}; and they have {@linkplain
     * Callback#areContentsTheSame the same content} if they themselves are {@linkplain
     * Object#equals equal}. However, if the presenter is given the same instance of collection
     * object to generate the fine-grained events, then the presenter will consider it caused by an
     * update from an {@linkplain Builder#addAdditionalObservable additional observable}, and send a
     * blanket item change event covering all positions.
     *
     * <p>Note that {@link DiffUtil} may not be suitable for all situations due to the computation
     * complexity and that <i>the computation is likely done from the main thread</i>. If client
     * code has better alternatives, it should directly subclass {@link RepositoryPresenter} and
     * provide a custom implementation in {@link RepositoryPresenter#getUpdates}.
     *
     * @param keyForItem A function from individual item objects to objects uniquely identifying the
     *     items across any changes. Can be the same as the stable ID function, provided the
     *     function does not return the same ID for multiple items (in particular
     *     {@link RecyclerView#NO_ID}.
     * @param detectMoves The same parameter for {@link DiffUtil#calculateDiff(Callback, boolean)}.
     * @see RepositoryPresenter#getUpdates
     */
    @NonNull
    TRet diffWith(@NonNull Function<? super TVal, ?> keyForItem, boolean detectMoves);
  }

  /**
   * Compiler state to compile for the specified item container type of the associated
   * {@link Repository}.
   */
  interface RPItemCompile<TVal> {

    /**
     * Enables fine-grained events catered for the single-item special case, where the item is
     * considered always {@linkplain Callback#areItemsTheSame the same}. The only possible events
     * are:
     * <ul>
     * <li>a single item is inserted, if the old data contains no item and the new data contains the
     *     item;
     * <li>a single item is removed, if the old data contains the item and the new data contains no
     *     item;
     * <li>a single item is changed, if the old item object and the new item object are not equal,
     *     or if the {@link RepositoryPresenter#getUpdates} call is likely due to an update from an
     *     {@linkplain Builder#addAdditionalObservable additional observable}.
     * </ul>
     */
    @NonNull
    RPItemCompile<TVal> diff();

    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a single item that will be
     * bound to the {@link RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<TVal> forItem();

    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a {@link Result} where the
     * item in the {@link Result} will be bound to the {@link RecyclerView} if present.
     */
    @NonNull
    RepositoryPresenter<Result<TVal>> forResult();
  }

  /**
   * Compiler state to compile for the generic collection container type of the associated
   * {@link Repository}.
   */
  interface RPTypedCollectionCompile<TVal, TCol>
      extends RPDiff<TVal, RPTypedCollectionCompile<TVal, TCol>> {
    /**
     * Creates a {@link RepositoryPresenter} for a {@link Repository} of a type that can be
     * converted to a {@link List} of items using the {@code converter}.
     */
    @NonNull
    <TColE extends TCol> RepositoryPresenter<TColE> forCollection(
        @NonNull Function<TColE, List<TVal>> converter);
  }

  /**
   * Compiler state to specify how to bind the {@link Repository} item to the view inflated by the
   * layout.
   */
  interface RPMain<T> extends RPItemCompile<T>, RPDiff<T, RPMain<T>> {

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
     * Specifies a {@code stableId} for the given item. Called only if stable IDs are enabled with
     * {@link RepositoryAdapter#setHasStableIds}, and therefore this method is optional with a
     * default implementation of returning {@link RecyclerView#NO_ID}. If stable IDs are enabled,
     * the returned ID and the layout returned by {@link RPLayout#layoutForItem(Function)} or
     * {@link RPLayout#layout(int)} for the given item should together uniquely identify this item
     * in the whole {@link RecyclerView} throughout all changes.
     */
    @NonNull
    RPItemCompile<T> stableId(long stableId);

    /**
     * Specifies a {@link Binder} to bind the whole collection (the repository value) to an inflated
     * item view. This binder will be called before the item binder specified with
     * {@link #bindWith}, and called for each item view. This is useful for cases where the
     * collection object contains useful information for binding items, which is not copied to the
     * individual item objects.
     */
    @NonNull
    <TCol> RPTypedCollectionCompile<T, TCol> bindCollectionWith(
        @NonNull Binder<TCol, View> collectionBinder);

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
