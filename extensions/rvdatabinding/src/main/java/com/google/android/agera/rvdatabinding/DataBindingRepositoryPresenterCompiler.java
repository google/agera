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

import static android.support.v4.util.Pair.create;
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.rvadapter.RepositoryPresenters.repositoryPresenterOf;
import static com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPHandlerBindingCompile;

import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPItemBinding;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
final class DataBindingRepositoryPresenterCompiler
    implements DBRPItemBinding, DBRPHandlerBindingCompile, RPLayout {
  @NonNull
  private final List<Pair<Integer, Object>> handlers;
  private Function<Object, Integer> layoutFactory;
  private Function itemId;

  DataBindingRepositoryPresenterCompiler() {
    this.handlers = new ArrayList<>();
  }

  @NonNull
  @Override
  public Object handler(@LayoutRes final int handlerId, @NonNull final Object handler) {
    handlers.add(create(handlerId, handler));
    return this;
  }

  @NonNull
  @Override
  public Object itemId(@LayoutRes final int itemId) {
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
  public RepositoryPresenter<List<Object>> forList() {
    return repositoryPresenterOf(null)
        .layoutForItem(layoutFactory)
        .bindWith(new ViewBinder(itemId, new ArrayList<>(handlers)))
        .forList();
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<Object>> forResult() {
    return repositoryPresenterOf(Object.class)
        .layoutForItem(layoutFactory)
        .bindWith(new ViewBinder(itemId, new ArrayList<>(handlers)))
        .forResult();
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List<Object>>> forResultList() {
    return repositoryPresenterOf(null)
        .layoutForItem(layoutFactory)
        .bindWith(new ViewBinder(itemId, new ArrayList<>(handlers)))
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

  private static final class ViewBinder implements Binder<Object, View> {
    private final Function<Object, Integer> itemId;
    @NonNull
    private final List<Pair<Integer, Object>> handlers;

    ViewBinder(@NonNull final Function<Object, Integer> itemId,
        @NonNull final List<Pair<Integer, Object>> handlers) {
      this.itemId = itemId;
      this.handlers = checkNotNull(handlers);
    }

    @Override
    public void bind(@NonNull final Object item, @NonNull final View view) {
      final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
      viewDataBinding.setVariable(itemId.apply(item), item);
      for (final Pair<Integer, Object> handler : handlers) {
        viewDataBinding.setVariable(handler.first, handler.second);
      }
      viewDataBinding.executePendingBindings();
    }
  }
}
