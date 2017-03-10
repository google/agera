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

import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.DO_NOTHING;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import com.google.android.agera.rvadapter.LayoutPresenter;
import com.google.android.agera.rvadapter.RepositoryAdapter;

/**
 * Contains concrete implementations of {@link LayoutPresenter} to present a layout in a
 * {@link RepositoryAdapter} {@link RecyclerView} using the Android data binding library.
 * <p>
 * The Android data binding library allows for binding a view model to a view in layout xml. The
 * implementation below takes a {@code layoutId} referring to the layout xml resource and a set of
 * handlers associated with handler ids.
 * <p>
 * See the data binding library documentation for details.
 */
public final class DataBindingLayoutPresenters {

  /**
   * Starts the creation of a {@link LayoutPresenter} using the Android data binding library.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static Builder dataBindingLayoutPresenterFor(@LayoutRes final int layoutId) {
    return new Builder(layoutId);
  }

  @SuppressWarnings("unchecked")
  public static final class Builder {
    @NonNull
    private final SparseArray<Object> handlers;
    @LayoutRes
    private final int layoutId;
    @RecycleConfig
    private int recycleConfig = DO_NOTHING;

    private Builder(final int layoutId) {
      this.layoutId = layoutId;
      this.handlers = new SparseArray<>();
    }

    @NonNull
    public Builder handler(final int handlerId, @NonNull final Object handler) {
      handlers.put(handlerId, handler);
      return this;
    }

    @NonNull
    public Builder onRecycle(@RecycleConfig final int recycleConfig) {
      this.recycleConfig = recycleConfig;
      return this;
    }

    @NonNull
    public LayoutPresenter build() {
      return new DataBindingLayoutPresenter(handlers, layoutId, recycleConfig);
    }

    private static class DataBindingLayoutPresenter extends LayoutPresenter {
      @NonNull
      private final SparseArray<Object> handlers;
      private final int layoutId;
      private final int recycleConfig;

      DataBindingLayoutPresenter(@NonNull final SparseArray<Object> handlers,
          final int layoutId, final int recycleConfig) {
        this.handlers = handlers;
        this.layoutId = layoutId;
        this.recycleConfig = recycleConfig;
      }

      @Override
      public int getLayoutResId() {
        return layoutId;
      }

      @Override
      public void bind(@NonNull final View view) {
        final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
        for (int i = 0; i < handlers.size(); i++) {
          final int variableId = handlers.keyAt(i);
          viewDataBinding.setVariable(variableId, handlers.get(variableId));
        }
        viewDataBinding.executePendingBindings();
      }

      @Override
      public void recycle(@NonNull final View view) {
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

  private DataBindingLayoutPresenters() {}
}
