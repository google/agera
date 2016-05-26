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

import com.google.android.agera.Function;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPCompile;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

public interface DataBindingRepositoryPresenterCompilerStates {
  interface DBRPItemBinding<TVal, TRet> {
    @NonNull
    TRet itemId(@LayoutRes int itemId);

    @NonNull
    TRet itemIdForItem(@NonNull Function<TVal, Integer> itemIdForItem);
  }

  interface DBRPHandlerBinding<TRet> {
    @NonNull
    TRet handler(@LayoutRes int handlerId, @NonNull Object handler);
  }

  interface DBRPHandlerBindingCompile<TVal>
      extends RPCompile<TVal>, DBRPHandlerBinding<DBRPHandlerBindingCompile<TVal>> {}
}
