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
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPItemCompile;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPMain;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPTypedCollectionCompile;
import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings({"unchecked, rawtypes"})
final class RepositoryPresenterCompiler implements
    RPLayout, RPMain, RPItemCompile, RPTypedCollectionCompile {
  @NonNull
  private static final Function<Object, Object> NO_KEY_FOR_ITEM = identityFunction();
  @NonNull
  private static final Function<Object, Object> SAME_KEY_FOR_ITEM = staticFunction(new Object());
  @NonNull
  private Function<Object, Integer> layoutForItem;
  @NonNull
  private Binder binder = nullBinder();
  @NonNull
  private Receiver recycler = nullReceiver();
  @NonNull
  private Function<Object, Long> stableIdForItem = staticFunction(RecyclerView.NO_ID);
  @NonNull
  private Function<Object, Object> keyForItem = NO_KEY_FOR_ITEM;
  private boolean detectMoves;
  @NonNull
  private Binder collectionBinder = nullBinder();

  @NonNull
  @Override
  public RepositoryPresenter forItem() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        keyForItem, detectMoves, itemAsList(), collectionBinder);
  }

  @NonNull
  @Override
  public RepositoryPresenter<List> forList() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        keyForItem, detectMoves, (Function) identityFunction(),
        collectionBinder);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result> forResult() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        keyForItem, detectMoves, (Function) resultAsList(),
        collectionBinder);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List>> forResultList() {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        keyForItem, detectMoves, (Function) resultListAsList(),
        collectionBinder);
  }

  @NonNull
  @Override
  public RPTypedCollectionCompile bindCollectionWith(@NonNull final Binder collectionBinder) {
    this.collectionBinder = collectionBinder;
    return this;
  }

  @NonNull
  @Override
  public RepositoryPresenter forCollection(@NonNull final Function converter) {
    return new CompiledRepositoryPresenter(layoutForItem, binder, stableIdForItem, recycler,
        keyForItem, detectMoves, converter, collectionBinder);
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
  public RPMain bindWith(@NonNull final Binder binder) {
    this.binder = binder;
    return this;
  }

  @NonNull
  @Override
  public RPMain stableIdForItem(@NonNull final Function stableIdForItem) {
    this.stableIdForItem = stableIdForItem;
    return this;
  }

  @NonNull
  @Override
  public RPItemCompile stableId(final long stableId) {
    this.stableIdForItem(staticFunction(stableId));
    return this;
  }

  @NonNull
  @Override
  public RPMain recycleWith(@NonNull final Receiver recycler) {
    this.recycler = recycler;
    return this;
  }

  @NonNull
  @Override
  public RPMain diffWith(@NonNull final Function keyForItem, final boolean detectMoves) {
    this.keyForItem = keyForItem;
    this.detectMoves = detectMoves;
    return this;
  }

  @NonNull
  @Override
  public RPItemCompile diff() {
    this.keyForItem = SAME_KEY_FOR_ITEM;
    this.detectMoves = false;
    return this;
  }

  private static final class CompiledRepositoryPresenter extends RepositoryPresenter {
    @NonNull
    private final Function<Object, List<Object>> converter;
    @NonNull
    private final Binder<Object, View> collectionBinder;
    @NonNull
    private final Function<Object, Integer> layoutId;
    @NonNull
    private final Binder<Object, View> binder;
    @NonNull
    private final Function<Object, Long> stableIdForItem;
    @NonNull
    private final Receiver<View> recycler;
    private final boolean enableDiff;

    @NonNull
    private final Function<Object, Object> keyForItem;
    private final boolean detectMoves;
    @NonNull
    private WeakReference<Object> dataRef = new WeakReference<>(null);
    @NonNull
    private List items = emptyList();

    CompiledRepositoryPresenter(
        @NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<Object, View> binder,
        @NonNull final Function<Object, Long> stableIdForItem,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<Object, Object> keyForItem,
        final boolean detectMoves,
        @NonNull final Function<Object, List<Object>> converter,
        @NonNull final Binder<Object, View> collectionBinder) {
      this.collectionBinder = collectionBinder;
      this.converter = converter;
      this.layoutId = layoutId;
      this.binder = binder;
      this.stableIdForItem = stableIdForItem;
      this.recycler = recycler;
      this.enableDiff = keyForItem != NO_KEY_FOR_ITEM;
      this.keyForItem = keyForItem;
      this.detectMoves = detectMoves;
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
      final Object item = getItems(data).get(index);
      final View view = holder.itemView;
      bind(data, item, view);
    }

    protected void bind(@NonNull final Object data, @NonNull final Object item,
        @NonNull final View view) {
      collectionBinder.bind(data, view);
      binder.bind(item, view);
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

    @Override
    public boolean getUpdates(@NonNull final Object oldData, @NonNull final Object newData,
        @NonNull final ListUpdateCallback listUpdateCallback) {
      if (!enableDiff) {
        return false;
      }

      if (oldData == newData) {
        // Consider this an additional observable update; send blanket change event
        final int itemCount = getItemCount(oldData);
        listUpdateCallback.onChanged(0, itemCount, null);
        return true;
      }

      // Do proper diffing.
      final List oldItems = getItems(oldData);
      final List newItems = getItems(newData); // This conveniently saves newData to dataRef.
      DiffUtil.calculateDiff(new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
          return oldItems.size();
        }

        @Override
        public int getNewListSize() {
          return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(final int oldItemPosition, final int newItemPosition) {
          final Object oldKey = keyForItem.apply(oldItems.get(oldItemPosition));
          final Object newKey = keyForItem.apply(newItems.get(newItemPosition));
          return oldKey.equals(newKey);
        }

        @Override
        public boolean areContentsTheSame(final int oldItemPosition, final int newItemPosition) {
          final Object oldItem = oldItems.get(oldItemPosition);
          final Object newItem = newItems.get(newItemPosition);
          return oldItem.equals(newItem);
        }
      }, detectMoves).dispatchUpdatesTo(listUpdateCallback);
      return true;
    }
  }
}
