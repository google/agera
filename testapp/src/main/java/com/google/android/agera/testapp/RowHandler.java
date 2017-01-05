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
package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.RecyclerView.RecycledViewPool;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.MutableRepository;
import com.google.android.agera.Receiver;
import com.google.android.agera.rvadapter.RepositoryAdapter;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

final class RowHandler<TRow, TRowItems> extends OnScrollListener
    implements Binder<TRow, View>, Receiver<View> {
  @NonNull
  private final Set<RepositoryAdapter> startedAdapters;
  @NonNull
  private final Map<Long, Parcelable> itemRowStates;
  @NonNull
  private final Map<Adapter, MutableRepository<TRowItems>> adapterRepositories;
  @NonNull
  private final Map<Adapter, Long> previousStableIds;
  @NonNull
  private final Function<TRow, TRowItems> data;
  @NonNull
  private final Function<TRow, Long> stableId;
  @NonNull
  private final Function<TRow, RepositoryPresenter<TRowItems>> presenter;
  @NonNull
  private final Function<TRow, LayoutManager> layoutManager;
  @NonNull
  private final RecycledViewPool pool;

  private RowHandler(@NonNull final Function<TRow, TRowItems> data,
      @NonNull final Function<TRow, Long> stableId,
      @NonNull final Function<TRow, RepositoryPresenter<TRowItems>> presenter,
      @NonNull final Function<TRow, LayoutManager> layoutManager) {
    this.data = checkNotNull(data);
    this.stableId = checkNotNull(stableId);
    this.presenter = checkNotNull(presenter);
    this.layoutManager = layoutManager;
    this.itemRowStates = new IdentityHashMap<>();
    this.previousStableIds = new IdentityHashMap<>();
    this.adapterRepositories = new IdentityHashMap<>();
    this.startedAdapters = new HashSet<>();
    this.pool = new RecycledViewPool();
  }

  @NonNull
  static <TRow, TRowItems> RowHandler<TRow, TRowItems> rowBinder(
      @NonNull final Function<TRow, LayoutManager> layoutManager,
      @NonNull final Function<TRow, Long> stableIdFunction,
      @NonNull final Function<TRow, TRowItems> dataFunction,
      @NonNull final Function<TRow, RepositoryPresenter<TRowItems>> presenterFromView) {
    return new RowHandler<>(dataFunction, stableIdFunction, presenterFromView, layoutManager);
  }

  @Override
  public void bind(@NonNull final TRow row, @NonNull final View view) {
    final RecyclerView recyclerView = (RecyclerView) view;
    final long id = stableId.apply(row);
    final TRowItems rowItems = data.apply(row);
    if (!(recyclerView.getAdapter() instanceof RepositoryAdapter)) {
      final MutableRepository<TRowItems> newRepository = mutableRepository(rowItems);
      final RepositoryPresenter<TRowItems> newPresenter = presenter.apply(row);
      final RepositoryAdapter newAdapter = repositoryAdapter()
          .add(newRepository, newPresenter)
          .build();
      recyclerView.setRecycledViewPool(pool);
      recyclerView.setLayoutManager(layoutManager.apply(row));
      adapterRepositories.put(newAdapter, newRepository);
      startedAdapters.add(newAdapter);
      newAdapter.setHasStableIds(true);
      recyclerView.setAdapter(newAdapter);
      recyclerView.addOnScrollListener(this);
      previousStableIds.put(newAdapter, id);
      newAdapter.startObserving();
    } else {
      final RepositoryAdapter adapter = (RepositoryAdapter) recyclerView.getAdapter();
      adapterRepositories.get(adapter).accept(rowItems);
      previousStableIds.put(adapter, id);
      if (!startedAdapters.contains(adapter)) {
        adapter.startObserving();
        startedAdapters.add(adapter);
      }
    }
    final LayoutManager layoutManager = recyclerView.getLayoutManager();
    final Parcelable state = itemRowStates.get(id);
    layoutManager.onRestoreInstanceState(state);
    if (state == null) {
      layoutManager.scrollToPosition(0);
    }
  }

  @Override
  public void onScrollStateChanged(final RecyclerView recyclerView, final int newState) {
    if (recyclerView != null
        && recyclerView.getLayoutManager() != null) {
      itemRowStates.put(previousStableIds.get(recyclerView.getAdapter()),
          recyclerView.getLayoutManager().onSaveInstanceState());
    }
  }

  @Override
  public void accept(@NonNull final View view) {
    final RecyclerView recyclerView = (RecyclerView) view;
    final RepositoryAdapter adapter = (RepositoryAdapter) recyclerView.getAdapter();
    if (startedAdapters.remove(adapter)) {
      adapter.stopObserving();
    }
  }
}
