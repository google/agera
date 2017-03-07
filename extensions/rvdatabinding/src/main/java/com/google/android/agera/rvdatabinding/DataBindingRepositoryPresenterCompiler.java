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

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.rvadapter.RepositoryPresenters.repositoryPresenterOf;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ITEM;
import static com.google.android.agera.rvdatabinding.RecycleConfig.DO_NOTHING;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPHandlerStableIdRecycleCompile;
import com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPItemBinding;
import java.util.List;

@SuppressWarnings("unchecked")
final class DataBindingRepositoryPresenterCompiler
    implements DBRPItemBinding, DBRPHandlerStableIdRecycleCompile, RPLayout {
  @NonNull
  private final SparseArray<Object> handlers;
  private Function<Object, Integer> layoutFactory;
  private Function itemId;
  @NonNull
  private Function<Object, Long> stableIdForItem = staticFunction(RecyclerView.NO_ID);
  @RecycleConfig
  private int recycleConfig = DO_NOTHING;

  DataBindingRepositoryPresenterCompiler() {
    this.handlers = new SparseArray<>();
  }

  @NonNull
  @Override
  public Object handler(final int handlerId, @NonNull final Object handler) {
    handlers.put(handlerId, handler);
    return this;
  }

  @NonNull
  @Override
  public Object itemId(final int itemId) {
    this.itemId = staticFunction(itemId);
    return this;
  }

  @NonNull
  @Override
  public Object itemIdForItem(@NonNull final Function itemIdForItem) {
    this.itemId = checkNotNull(itemIdForItem);
    return this;
  }

  @NonNull
  @Override
  public RepositoryPresenter forItem() {
    return repositoryPresenterOf(null)
        .layoutForItem(layoutFactory)
        .stableIdForItem(stableIdForItem)
        .bindWith(new ViewBinder(itemId, handlers))
        .recycleWith(new ViewRecycler(recycleConfig, handlers))
        .forItem();
  }

  @NonNull
  @Override
  public RepositoryPresenter<List<Object>> forList() {
    return repositoryPresenterOf(null)
        .layoutForItem(layoutFactory)
        .stableIdForItem(stableIdForItem)
        .bindWith(new ViewBinder(itemId, handlers))
        .recycleWith(new ViewRecycler(recycleConfig, handlers))
        .forList();
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<Object>> forResult() {
    return repositoryPresenterOf(Object.class)
        .layoutForItem(layoutFactory)
        .stableIdForItem(stableIdForItem)
        .bindWith(new ViewBinder(itemId, handlers))
        .recycleWith(new ViewRecycler(recycleConfig, handlers))
        .forResult();
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List<Object>>> forResultList() {
    return repositoryPresenterOf(null)
        .layoutForItem(layoutFactory)
        .stableIdForItem(stableIdForItem)
        .bindWith(new ViewBinder(itemId, handlers))
        .recycleWith(new ViewRecycler(recycleConfig, handlers))
        .forResultList();
  }

  @NonNull
  @Override
  public Object layout(@LayoutRes int layoutId) {
    this.layoutFactory = staticFunction(layoutId);
    return this;
  }

  @NonNull
  @Override
  public Object layoutForItem(@NonNull Function layoutForItem) {
    this.layoutFactory = checkNotNull(layoutForItem);
    return this;
  }

  @NonNull
  @Override
  public Object stableIdForItem(@NonNull final Function stableIdForItem) {
    this.stableIdForItem = stableIdForItem;
    return this;
  }

  @NonNull
  @Override
  public Object onRecycle(@RecycleConfig final int recycleConfig) {
    this.recycleConfig = recycleConfig;
    return this;
  }

  private static final class ViewBinder implements Binder<Object, View> {
    private final Function<Object, Integer> itemId;
    @NonNull
    private final SparseArray<Object> handlers;

    ViewBinder(@NonNull final Function<Object, Integer> itemId,
        @NonNull final SparseArray<Object> handlers) {
      this.itemId = itemId;
      this.handlers = checkNotNull(handlers);
    }

    @Override
    public void bind(@NonNull final Object item, @NonNull final View view) {
      final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
      final Integer itemVariable = itemId.apply(item);
      viewDataBinding.setVariable(itemVariable, item);
      view.setTag(R.id.agera__rvdatabinding__item_id, itemVariable);
      for (int i = 0; i < handlers.size(); i++) {
        final int variableId = handlers.keyAt(i);
        viewDataBinding.setVariable(variableId, handlers.valueAt(i));
      }
      viewDataBinding.executePendingBindings();
    }
  }

  private static final class ViewRecycler implements Receiver<View> {
    @RecycleConfig
    private final int recycleConfig;
    @NonNull
    private SparseArray<Object> handlers;

    ViewRecycler(
        @RecycleConfig final int recycleConfig,
        @NonNull final SparseArray<Object> handlers) {
      this.recycleConfig = recycleConfig;
      this.handlers = checkNotNull(handlers);
    }

    @Override
    public void accept(@NonNull final View view) {
      if (recycleConfig != 0) {
        final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
        if ((recycleConfig & CLEAR_ITEM) != 0) {
          final Object tag = view.getTag(R.id.agera__rvdatabinding__item_id);
          view.setTag(R.id.agera__rvdatabinding__item_id, null);
          if (tag instanceof Integer) {
            viewDataBinding.setVariable((int) tag, null);
          }
        }
        if ((recycleConfig & CLEAR_HANDLERS) != 0) {
          for (int i = 0; i < handlers.size(); i++) {
            viewDataBinding.setVariable(handlers.keyAt(i), null);
          }
        }
        viewDataBinding.executePendingBindings();
      }
    }
  }
}
