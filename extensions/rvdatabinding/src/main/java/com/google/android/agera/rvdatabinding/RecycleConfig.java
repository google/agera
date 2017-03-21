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

import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import com.google.android.agera.Repository;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Constants controlling the recycle behavior of the compiled data binding
 * {@link RepositoryPresenter}.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true, value = {
    RecycleConfig.DO_NOTHING,
    RecycleConfig.CLEAR_ITEM,
    RecycleConfig.CLEAR_COLLECTION,
    RecycleConfig.CLEAR_HANDLERS,
    RecycleConfig.CLEAR_ALL,
})
public @interface RecycleConfig {
  /**
   * When the {@link RecyclerView} recycles a view, do nothing. This is the default behavior.
   */
  int DO_NOTHING = 0;

  /**
   * When the {@link RecyclerView} recycles a view, reset the item from the {@link Repository}
   * to {@code null}.
   */
  int CLEAR_ITEM = 1;

  /**
   * When the {@link RecyclerView} recycles a view, reset and all handlers to {@code null}.
   */
  int CLEAR_HANDLERS = 1 << 1;

  /**
   * When the {@link RecyclerView} recycles a view, reset the collection from the
   * {@link Repository} to {@code null}.
   */
  int CLEAR_COLLECTION = 1 << 2;

  /**
   * When the {@link RecyclerView} recycles a view, rebind all variables to {@code null}.
   */
  int CLEAR_ALL = CLEAR_ITEM | CLEAR_COLLECTION | CLEAR_HANDLERS;
}
