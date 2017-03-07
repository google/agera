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

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Receiver;

/**
 * Container of the compiler state interfaces supporting the creation of a {@link
 * LayoutPresenter}.
 */
public interface LayoutPresenterCompilerStates {

  /**
   * Compiler state to specify how to update the view inflated by the layout.
   */
  interface LPLayoutBinder<TRet> {

    /**
     * Specifies a {@link Receiver} to bind the inflated {@code View} with.
     */
    @NonNull
    TRet bindWith(@NonNull Receiver<View> binder);
  }

  /**
   * Compiler state to specify how to recycle the {@code View}.
   */
  interface LPRecycle<TRet> {

    /**
     * Specifies a {@link Receiver} to recycle the {@code View}.
     */
    @NonNull
    TRet recycleWith(@NonNull Receiver<View> recycler);
  }

  interface LPCompile {

    /**
     * Creates a {@link LayoutPresenter} that will be bound to the {@link RecyclerView}.
     */
    @NonNull
    LayoutPresenter compile();
  }

  /**
   * Compiler state allowing to specify view ecycler or compile.
   */
  interface LPRecycleCompile extends LPRecycle<LPCompile>, LPCompile {}

  /**
   * Compiler state allowing to specify view updater, view recycler or compile.
   */
  interface LPBinderRecycleCompile extends LPRecycleCompile,
      LPLayoutBinder<LPRecycleCompile> {}
}
