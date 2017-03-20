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

import static com.google.android.agera.Binders.nullBinder;
import static com.google.android.agera.Functions.identityFunction;
import static com.google.android.agera.Functions.itemAsList;
import static com.google.android.agera.Functions.resultAsList;
import static com.google.android.agera.Functions.resultListAsList;
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Receivers.nullReceiver;
import static java.util.Collections.emptyList;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPViewBinderRecycleItemCompile;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPViewBinderRecycleStableIdCompile;
import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings({"unchecked, rawtypes"})
final class RepositoryPresenterCompiler implements RPLayout, RPViewBinderRecycleStableIdCompile,
    RPViewBinderRecycleItemCompile {
  @NonNull
  private Function<Object, Integer> layoutForItem;
  @NonNull
  private Binder binder = nullBinder();
  @NonNull
  private Receiver recycler = nullReceiver();
  @NonNull
  private Function<Object, Long> stableIdForItem = staticFunction(RecyclerView.NO_ID);

  @NonNull
  @Override
  public RepositoryPresenter forItem() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        itemAsList());
  }

  @NonNull
  @Override
  public RepositoryPresenter<List> forList() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        (Function) identityFunction());
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result> forResult() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        (Function) resultAsList());
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List>> forResultList() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        (Function) resultListAsList());
  }

  @NonNull
  @Override
  public Object layout(@LayoutRes final int layoutId) {
    this.layoutForItem = staticFunction(layoutId);
    return this;
  }

  @NonNull
  @Override
  public Object layoutForItem(@NonNull final Function layoutForItem) {
    this.layoutForItem = checkNotNull(layoutForItem);
    return this;
  }

  @NonNull
  @Override
  public Object bindWith(@NonNull final Binder binder) {
    this.binder = binder;
    return this;
  }

  @NonNull
  @Override
  public Object stableIdForItem(@NonNull final Function stableIdForItem) {
    this.stableIdForItem = stableIdForItem;
    return this;
  }

  @NonNull
  @Override
  public Object stableId(final long stableId) {
    this.stableIdForItem(staticFunction(stableId));
    return this;
  }

  @NonNull
  @Override
  public Object recycleWith(@NonNull final Receiver recycler) {
    this.recycler = recycler;
    return this;
  }

  private static final class CompiledRepositoryPresenter extends RepositoryPresenter {
    @NonNull
    private final Function<Object, List<Object>> converter;
    @NonNull
    private final Function<Object, Integer> layoutId;
    @NonNull
    private final Binder<Object, View> binder;
    @NonNull
    private final Function<Object, Long> stableIdForItem;
    @NonNull
    private final Receiver<View> recycler;
    @NonNull
    private WeakReference<Object> dataRef = new WeakReference<>(null);
    @NonNull
    private List items = emptyList();

    CompiledRepositoryPresenter(
        @NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<Object, View> binder,
        @NonNull final Function<Object, Long> stableIdForItem,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<Object, List<Object>> converter) {
      this.converter = converter;
      this.layoutId = layoutId;
      this.binder = binder;
      this.stableIdForItem = stableIdForItem;
      this.recycler = recycler;
    }

    @Override
    public int getItemCount(@NonNull final Object data) {
      return getItems(data).size();
    }

    @Override
    public int getLayoutResId(@NonNull final Object data, final int index) {
      return layoutId.apply(getItems(data).get(index));
    }

    @Override
    public void bind(@NonNull final Object data, final int index,
        @NonNull final RecyclerView.ViewHolder holder) {
      binder.bind(getItems(data).get(index), holder.itemView);
    }

    @Override
    public void recycle(@NonNull final RecyclerView.ViewHolder holder) {
      recycler.accept(holder.itemView);
    }

    @Override
    public long getItemId(@NonNull final Object data, final int index) {
      return stableIdForItem.apply(getItems(data).get(index));
    }

    @NonNull
    private List getItems(@NonNull final Object data) {
      if (this.dataRef.get() != data) {
        items = converter.apply(data);
        this.dataRef = new WeakReference<>(data);
      }
      return items;
    }
  }
}
