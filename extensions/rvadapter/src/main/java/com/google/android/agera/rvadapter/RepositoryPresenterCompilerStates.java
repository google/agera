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

import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

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
   * Compiler state to specify how to bind the {@link Repository} item to the view inflated by the
   * layout.
   */
  interface RPViewBinder<TVal, TRet> {

    /**
     * Specifies a {@link Binder} to bind a single item in the {@link Repository} to an inflated
     * {@code View}.
     */
    @NonNull
    TRet bindWith(@NonNull Binder<TVal, View> viewBinder);
  }

  /**
   * Compiler state to specify how to recycle the {@code View}.
   */
  interface RPRecycle<TRet> {

    /**
     * Specifies a {@link Receiver} to recycle the {@code View}.
     */
    @NonNull
    TRet recycleWith(@NonNull Receiver<View> recycler);
  }

  /**
   * Compiler state to specify how to generate stable IDs when {@link
   * android.support.v7.widget.RecyclerView.Adapter#setHasStableIds(boolean)} is true.
   */
  interface RPStableId<TVal, TRet> {

    /**
     * Specifies a {@link Function} providing a stable id for the given item. Called only if stable
     * IDs are enabled with {@link RepositoryAdapter#setHasStableIds RepositoryAdapter.setHasStableIds(true)},
     * and therefore this method is optional with a default implementation of returning {@link
     * RecyclerView#NO_ID}. If stable IDs are enabled, the returned ID and the layout returned by
     * {@link RPLayout#layoutForItem(Function)} or {@link RPLayout#layout(int)} for the given item
     * should together uniquely identify this item in the whole {@link RecyclerView} throughout all
     * changes.
     */
    @NonNull
    TRet stableIdForItem(@NonNull Function<? super TVal, Long> stableIdForItem);
  }

  /**
   * Compiler state to create the @{link RepositoryPresenter}.
   */
  interface RPCompile<TVal> {

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link List} where each
     * item in the {@link List} will be bound to the {@link RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<List<TVal>> forList();

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link Result} where the
     * item in the {@link Result} will be bound to the @{link RecyclerView} if present.
     */
    @NonNull
    RepositoryPresenter<Result<TVal>> forResult();

    /**
     * Creates a {@link RepositoryPresenter} for a @{link Repository} of a {@link Result} containing
     * a {@link List} where each item in the {@link List} will be bound to the {@link
     * RecyclerView}.
     */
    @NonNull
    RepositoryPresenter<Result<List<TVal>>> forResultList();
  }

  /**
   * Compiler state allowing to specify Recycle or compile.
   */
  interface RPRecycleCompile<TVal> extends RPRecycle<RPCompile<TVal>>, RPCompile<TVal> {}

  /**
   * Compiler state allowing to specify view binder, view recycler, function or compile.
   */
  interface RPViewBinderRecycleCompile<TVal> extends RPRecycleCompile<TVal>,
      RPViewBinder<TVal, RPRecycleCompile<TVal>> {}

  /**
   * Compiler state allowing to specify view binder, view recycler, stable id function or compile.
   */
  interface RPViewBinderRecycleStableIdCompile<TVal> extends RPViewBinderRecycleCompile<TVal>,
      RPStableId<TVal, RPViewBinderRecycleCompile<TVal>> {}
}
