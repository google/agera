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
import android.support.annotation.Nullable;
import com.google.android.agera.Repository;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPMain;

/**
 * Contains concrete implementations of {@link RepositoryPresenter} to present the content of a
 * {@link Repository} using the Android data binding library.
 * <p>
 * The Android data binding library allows for binding a view model to a view in layout xml. The
 * implementation below takes a {@code layoutId} referring to the layout xml resource, an
 * itemId referring to the item or items in the associated {@link Repository} and a set of handlers
 * associated with handler ids.
 * <p>
 * The following layout from the data binding documentation refers to both an item and a handler
 * <pre>
 * {@code
 * <!--item_layout.xml-->
 * <?xml version="1.0" encoding="utf-8"?>
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *   <data>
 *     <variable name="handlers" type="com.example.Handlers"/>
 *     <variable name="user" type="com.example.User"/>
 *   </data>
 *   <LinearLayout
 *     android:orientation="vertical"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent">
 *   <TextView android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     android:text="@{user.firstName}"
 *     android:onClick="@{user.isFriend ? handlers.onClickFriend : handlers.onClickEnemy}"/>
 *   <TextView android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     android:text="@{user.lastName}"
 *     android:onClick="@{user.isFriend ? handlers.onClickFriend : handlers.onClickEnemy}"/>
 *   </LinearLayout>
 * </layout>
 * }
 * </pre>
 * The following call would bind {@code user} to each item in the list and {@code handlers} to the
 * provided parameter.
 * <pre>
 * {@code
 * DataBindingRepositoryPresenters.dataBindingRepositoryPresenterOf(User.class)
 *   .layout(R.layout.item_layout)
 *   .itemId(BR.user)
 *   .handler(BR.handlers, new Handlers())
 *   .forList();
 * }
 * </pre>
 * <p> See the data binding library documentation for details.
 */
public final class DataBindingRepositoryPresenters {

  /**
   * Starts the creation of a {@link RepositoryPresenter} using the Android data binding library.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static <T> RPLayout<T, DBRPMain<T>> dataBindingRepositoryPresenterOf(
      @Nullable final Class<T> type) {
    return new DataBindingRepositoryPresenterCompiler();
  }

  private DataBindingRepositoryPresenters() {}
}
