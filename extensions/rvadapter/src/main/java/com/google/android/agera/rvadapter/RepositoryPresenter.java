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

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import com.google.android.agera.Repository;
import com.google.android.agera.rvadapter.RepositoryAdapter.Builder;

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

  /**
   * Called when the given {@code holder} is recycled.
   *
   * @param holder The view holder that holds the view. If a subclass of {@link RepositoryAdapter}
   *     is used, which returns a custom view holder for this item's layout resource ID, then this
   *     object will be of that custom type.
   */
  public void recycle(@NonNull final RecyclerView.ViewHolder holder) {}

  /**
   * Produces a sequence of fine-grained events (addition, removal, changing and moving of
   * individual items) to the given {@code listUpdateCallback} capturing the changes of data to be
   * presented by this {@link RepositoryPresenter}, in response to an update. Implementation should
   * do either of the following:
   * <ul>
   * <li>Produce and dispatch the fine-grained events that accurately describe the changes, and then
   *     return true;
   * <li>Refuse to produce the fine-grained events by returning false. This is the base
   *     implementation.
   * </ul>
   *
   * <p>While a {@link RepositoryAdapter} is {@link RepositoryAdapter#hasObservers() in use} (for
   * example, attached to a {@code RecyclerView}), when it receives an update from a presented
   * {@link Repository}, the associated {@code RepositoryPresenter} will be asked to produce a
   * sequence of fine-grained events capturing the update; when the {@code RepositoryAdapter}
   * receives an update from one of the {@linkplain Builder#addAdditionalObservable additional
   * observables}, then all {@code RepositoryPresenter}s will be asked. If all affected
   * {@code RepositoryPresenter}s produced fine-grained events, then {@code RepositoryAdapter} will
   * apply the new data and notify the {@code RecyclerView} of the aggregated sequence of events;
   * otherwise the adapter will call {@link Adapter#notifyDataSetChanged()}, and pause all update
   * processing (both data and events) until fresh data is needed, hinted by the next call to
   * {@link RepositoryAdapter#getItemCount()}.
   *
   * <p>Notes for implementation:
   * <ul>
   * <li>If the update comes from the paired repository, then {@code oldData} is the previously
   *     presented repository value, and {@code newData} is the value newly obtained from the
   *     repository. The new value has not been applied and is not exposed through the adapter.
   *     Note that repositories are allowed to dispatch updates while maintaining
   *     {@code oldData.equals(newData)} or even {@code oldData == newData}, depending on the
   *     repository implementation.
   * <li>If the update comes from an additional observable, then it is guaranteed that
   *     {@code oldData == newData}, because the value is not reloaded from the repository.
   * <li>A {@code RepositoryPresenter} may not have a chance to produce fine-grained events in
   *     response to an update. This can happen when an earlier presenter returns false from this
   *     method, or the adapter is not {@link RepositoryAdapter#hasObservers() in use} (for example,
   *     not attached to any {@code RecyclerView}).
   * <li>{@code RepositoryAdapter} guarantees that, if this method is not called, then
   *     {@link #getItemCount} will be called before this {@code RepositoryPresenter} is asked to
   *     present any item from the new data. {@link #getItemCount} may be called again when there is
   *     a chance that the last data passed into the method may not be the latest.
   * <li>A {@code RepositoryPresenter} presenting a static item (added with {@link Builder#addItem})
   *     will not be able to produce any events for any item views presented for the static item.
   * </ul>
   *
   * @param oldData The data previously presented by this {@code RepositoryPresenter}.
   * @param newData The data to be presented by this {@code RepositoryPresenter} following the
   *     update.
   * @param listUpdateCallback A callback to record the sequence of fine-grained events to be
   *     dispatched via the {@code RepositoryAdapter} to the {@code RecyclerView}. To be used within
   *     this method call only. Positions and counts are relative to this presenter.
   * @return Whether the sequence of calls to {@code listUpdateCallback} during the execution of
   *     this method has completely and accurately described all changes.
   */
  public boolean getUpdates(@NonNull final T oldData, @NonNull final T newData,
      @NonNull final ListUpdateCallback listUpdateCallback) {
    return false;
  }
}
