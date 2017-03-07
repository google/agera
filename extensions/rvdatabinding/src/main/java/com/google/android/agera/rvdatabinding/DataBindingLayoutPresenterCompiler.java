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

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.rvadapter.LayoutPresenters.layoutPresenterFor;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.DO_NOTHING;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.SparseArray;
import android.view.View;
import com.google.android.agera.Receiver;
import com.google.android.agera.rvadapter.LayoutPresenter;
import com.google.android.agera.rvadapter.LayoutPresenterCompilerStates.LPCompile;
import com.google.android.agera.rvdatabinding.DataBindingLayoutPresenterCompilerStates.DBLPHandlerRecycleCompile;

@SuppressWarnings("unchecked")
final class DataBindingLayoutPresenterCompiler implements DBLPHandlerRecycleCompile {
  @NonNull
  private final SparseArray<Object> handlers;
  @LayoutRes
  private final int layoutId;
  @RecycleConfig
  private int recycleConfig = DO_NOTHING;

  DataBindingLayoutPresenterCompiler(final int layoutId) {
    this.layoutId = layoutId;
    this.handlers = new SparseArray<>();
  }

  @NonNull
  @Override
  public DBLPHandlerRecycleCompile handler(
      final int handlerId, @NonNull final Object handler) {
    handlers.put(handlerId, handler);
    return this;
  }

  @NonNull
  @Override
  public LayoutPresenter compile() {
    return layoutPresenterFor(layoutId)
        .bindWith(new ViewBinder(handlers))
        .recycleWith(new ViewRecycler(recycleConfig, handlers))
        .compile();
  }

  @NonNull
  @Override
  public LPCompile onRecycle(@RecycleConfig final int recycleConfig) {
    this.recycleConfig = recycleConfig;
    return this;
  }

  private static final class ViewBinder implements Receiver<View> {
    @NonNull
    private final SparseArray<Object> handlers;

    ViewBinder(@NonNull final SparseArray<Object> handlers) {
      this.handlers = checkNotNull(handlers);
    }

    @Override
    public void accept(@NonNull final View view) {
      final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
      for (int i = 0; i < handlers.size(); i++) {
        final int variableId = handlers.keyAt(i);
        viewDataBinding.setVariable(variableId, handlers.get(variableId));
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
      if ((recycleConfig & CLEAR_HANDLERS) != 0) {
        final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
        for (int i = 0; i < handlers.size(); i++) {
          viewDataBinding.setVariable(handlers.keyAt(i), null);
        }
        viewDataBinding.executePendingBindings();
      }
    }
  }
}
