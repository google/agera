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

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.google.android.agera.rvadapter.LayoutPresenter;
import com.google.android.agera.rvadapter.RepositoryAdapter;
import com.google.android.agera.rvdatabinding.DataBindingLayoutPresenterCompilerStates.DBLPHandlerRecycleCompile;

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
  public static DBLPHandlerRecycleCompile dataBindingLayoutPresenterFor(
      @LayoutRes final int layoutId) {
    return new DataBindingLayoutPresenterCompiler(layoutId);
  }

  private DataBindingLayoutPresenters() {}
}
