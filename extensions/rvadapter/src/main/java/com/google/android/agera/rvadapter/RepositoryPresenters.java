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
import android.support.annotation.Nullable;
import com.google.android.agera.Repository;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPMain;

/**
 * Contains a basic implementation of {@link RepositoryPresenter} to present the content of a
 * {@link Repository}.
 */
public final class RepositoryPresenters {

  /**
   * Starts the creation of a compiled {@link RepositoryPresenter}. A compiled presenter work with
   * item objects: firstly, the repository value is converted to a list of item objects, the list
   * size determining the item count; then the layout resources, item IDs, binding, and change
   * detection are all based on the individual item objects. Client code supplies the logic for each
   * aspect of the presenter, such as:
   * <ul>
   * <li>{@link RPLayout#layoutForItem .layoutForItem(Function&lt;T, Integer>)} selects the layout
   *     resource ID for each item object;
   * <li>{@link RPMain#stableIdForItem .stableIdForItem(Function&lt;T, Long>)} provides a stable ID
   *     per item object;
   * <li>{@link RPMain#bindWith .bindWidth(Binder&lt;T, View>)} renders the item data onto the view;
   * <li>{@link RPMain#forCollection .forCollection(Function&lt;V, List&lt;T>>)} specifies the
   *     method to convert the repository value (of type {@code V}) to an item list (shortcuts are
   *     provided for repository values of type {@code T}, {@code List<T>}, {@code Result<T>} or
   *     {@code Result<List<T>>} that can be converted in typical ways).
   * </ul>
   *
   * @param type The type of the <i>item objects</i>.
   */
  @SuppressWarnings({"unchecked", "UnusedParameters"})
  @NonNull
  public static <T> RPLayout<T, RPMain<T>> repositoryPresenterOf(@Nullable final Class<T> type) {
    return new RepositoryPresenterCompiler();
  }

  private RepositoryPresenters() {}
}
