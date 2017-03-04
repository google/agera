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
import com.google.android.agera.Function;
import com.google.android.agera.Repository;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPCompile;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPStableId;

/**
 * Container of the compiler state interfaces supporting the creation of a data binding
 * {@link RepositoryPresenter}.
 */
public interface DataBindingRepositoryPresenterCompilerStates {

  /**
   * Compiler state to specify how to bind the {@code View} using data binding.
   */
  interface DBRPItemBinding<TVal, TRet> {

    /**
     * Specifies a data binding @{code itemId} from the previously given {@code layout} to bind a
     * single item in the {@link Repository}.
     */
    @NonNull
    TRet itemId(int itemId);

    /**
     * Specifies a {@link Function} to return a data binding @{code itemId} from the previously
     * given {@code layout} to bind a single item in the {@link Repository}.
     */
    @NonNull
    TRet itemIdForItem(@NonNull Function<TVal, Integer> itemIdForItem);
  }

  /**
   * Compiler state to specify index independent handlers from the given {@code layout}.
   */
  interface DBRPHandlerBinding<TRet> {
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
  interface DBRPHandlerStableIdRecycleCompile<TVal>
      extends DBRPHandlerBinding<DBRPHandlerStableIdRecycleCompile<TVal>>,
      DBRPStableIdRecycleCompile<TVal> {}

  /**
   * Compiler state allowing to specify stable id, recycle strategy or compile.
   */
  interface DBRPStableIdRecycleCompile<TVal>
      extends RPStableId<TVal, DBRPRecycleCompile<TVal>>, DBRPRecycleCompile<TVal> {}

  /**
   * Compiler state allowing to specify recycle strategy or compile.
   */
  interface DBRPRecycleCompile<TVal> extends RPCompile<TVal>, DBRPRecycle<RPCompile<TVal>> {}
}
