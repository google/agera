/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import com.google.android.agera.Repository;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

/**
 * Contains logic to present the content of a {@link Repository}.
 */
public abstract class RepositoryPresenter<T> {
  /**
   * Returns the number of adapter items needed to present the data. This method may be called very
   * frequently; implementation should be very lightweight.
   */
  public abstract int getItemCount(@NonNull T data);

  /**
   * Returns the stable ID for the {@code index}-th item to present the data. Called only if stable
   * IDs are enabled with {@link RepositoryAdapter#setHasStableIds
   * RepositoryAdapter.setHasStableIds(true)}, and therefore this method is optional with a default
   * implementation of returning {@link RecyclerView#NO_ID}. If stable IDs are enabled, this ID and
   * the item's {@linkplain #getLayoutResId layout resource ID} should together uniquely identify
   * this item in the whole {@link RecyclerView} throughout all changes.
   *
   * @param index The item index between 0 (incl.) and {@link #getItemCount} (excl.).
   */
  public long getItemId(@NonNull final T data, final int index) {
    return RecyclerView.NO_ID;
  }

  /**
   * Returns the layout resource ID to inflate the view for the {@code index}-th item to present the
   * data.
   *
   * @param index The item index between 0 (incl.) and {@link #getItemCount} (excl.).
   */
  public abstract @LayoutRes int getLayoutResId(@NonNull T data, int index);

  /**
   * Binds the {@code index}-th item to present the data into the item view held in the given
   * {@code holder}. The view is inflated from the layout resource specified by
   * {@link #getLayoutResId}, but may have been previously bound to a different index, different
   * data, and/or with a different presenter. Therefore, implementation should take care of
   * resetting the view state.
   *
   * @param index The item index between 0 (incl.) and {@link #getItemCount} (excl.).
   * @param holder The view holder that holds the view. If a subclass of {@link RepositoryAdapter}
   *     is used, which returns a custom view holder for this item's layout resource ID, then this
   *     object will be of that custom type.
   */
  public abstract void bind(@NonNull T data, int index, @NonNull RecyclerView.ViewHolder holder);
}
