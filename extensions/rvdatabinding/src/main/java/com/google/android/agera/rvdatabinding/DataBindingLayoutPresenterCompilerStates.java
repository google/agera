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
import com.google.android.agera.rvadapter.LayoutPresenter;
import com.google.android.agera.rvadapter.LayoutPresenterCompilerStates.LPCompile;

/**
 * Container of the compiler state interfaces supporting the creation of a data binding
 * {@link LayoutPresenter}.
 */
public interface DataBindingLayoutPresenterCompilerStates {

  /**
   * Compiler state to specify index independent handlers from the given {@code layout}.
   */
  interface DBLPHandlerBinding<TRet> {
    /**
     * Specifies what {@code handler} is associated with the {@code handlerId} in the previously
     * given {@code layout}.
     */
    @NonNull
    TRet handler(int handlerId, @NonNull Object handler);
  }

  /**
   * Compiler state to specify a recycle config.
   */
  interface DBRPRecycle<TRet> {
    /**
     * Specifies what {@code handler} is associated with the {@code handlerId} in the previously
     * given {@code layout}.
     */
    @NonNull
    TRet onRecycle(@RecycleConfig int recycleConfig);
  }

  /**
   * Compiler state allowing to specify handlers, stable id, recycle strategy or compile.
   */
  interface DBLPHandlerRecycleCompile
      extends DBLPHandlerBinding<DBLPHandlerRecycleCompile>, DBLPRecycleCompile {}

  /**
   * Compiler state allowing to specify recycle strategy or compile.
   */
  interface DBLPRecycleCompile extends LPCompile, DBRPRecycle<LPCompile> {}
}

