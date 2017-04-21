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

import static com.google.android.agera.Functions.identityFunction;
import static com.google.android.agera.Functions.itemAsList;
import static com.google.android.agera.Functions.resultAsList;
import static com.google.android.agera.Functions.resultListAsList;
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_COLLECTION;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ITEM;
import static com.google.android.agera.rvdatabinding.RecycleConfig.DO_NOTHING;
import static java.util.Collections.emptyList;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPItemCompile;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenterCompilerStates.DBRPMain;
import java.lang.ref.WeakReference;
import java.util.List;

@SuppressWarnings("unchecked")
final class DataBindingRepositoryPresenterCompiler
    implements DBRPMain, RPLayout, RPItemCompile {
  @NonNull
  private static final Function<Object, Object> NO_KEY_FOR_ITEM = identityFunction();
  @NonNull
  private static final Function<Object, Object> SAME_KEY_FOR_ITEM = staticFunction(new Object());
  private static final int BR_NO_ID = -1;
  @NonNull
  private final SparseArray<Object> handlers;
  private Function<Object, Integer> layoutFactory;
  private Function<Object, Integer> itemId = staticFunction(BR_NO_ID);
  private int collectionId = BR_NO_ID;
  @NonNull
  private Function<Object, Long> stableIdForItem = staticFunction(RecyclerView.NO_ID);
  @RecycleConfig
  private int recycleConfig = DO_NOTHING;
  @NonNull
  private Function<Object, Object> keyForItem = NO_KEY_FOR_ITEM;
  private boolean detectMoves;

  DataBindingRepositoryPresenterCompiler() {
    this.handlers = new SparseArray<>();
  }

  @NonNull
  @Override
  public DBRPMain handler(final int handlerId, @NonNull final Object handler) {
    handlers.put(handlerId, handler);
    return this;
  }

  @NonNull
  @Override
  public DBRPMain itemId(final int itemId) {
    this.itemId = staticFunction(itemId);
    return this;
  }

  @NonNull
  @Override
  public DBRPMain itemIdForItem(@NonNull final Function itemIdForItem) {
    this.itemId = checkNotNull(itemIdForItem);
    return this;
  }

  @NonNull
  @Override
  public DBRPMain diffWith(@NonNull final Function keyForItem, final boolean detectMoves) {
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

  @NonNull
  @Override
  public RepositoryPresenter forItem() {
    return new CompiledRepositoryPresenter(itemId, layoutFactory, stableIdForItem,
        handlers, recycleConfig, itemAsList(), collectionId, keyForItem, detectMoves);
  }

  @NonNull
  @Override
  public RepositoryPresenter<List<Object>> forList() {
    return new CompiledRepositoryPresenter(itemId, layoutFactory, stableIdForItem,
        handlers, recycleConfig, (Function) identityFunction(), collectionId, keyForItem,
        detectMoves);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<Object>> forResult() {
    return new CompiledRepositoryPresenter(itemId, layoutFactory, stableIdForItem,
        handlers, recycleConfig, (Function) resultAsList(), collectionId, keyForItem, detectMoves);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List<Object>>> forResultList() {
    return new CompiledRepositoryPresenter(itemId, layoutFactory,
        stableIdForItem, handlers, recycleConfig, (Function) resultListAsList(), collectionId,
        keyForItem, detectMoves);
  }

  @NonNull
  @Override
  public RepositoryPresenter forCollection(@NonNull final Function converter) {
    return new CompiledRepositoryPresenter(itemId, layoutFactory, stableIdForItem,
        handlers, recycleConfig, converter, collectionId, keyForItem, detectMoves);
  }

  @NonNull
  @Override
  public Object layout(@LayoutRes int layoutId) {
    this.layoutFactory = staticFunction(layoutId);
    return this;
  }

  @NonNull
  @Override
  public Object layoutForItem(@NonNull final Function layoutForItem) {
    this.layoutFactory = checkNotNull(layoutForItem);
    return this;
  }

  @NonNull
  @Override
  public DBRPMain stableIdForItem(@NonNull final Function stableIdForItem) {
    this.stableIdForItem = stableIdForItem;
    return this;
  }

  @NonNull
  @Override
  public RPItemCompile stableId(final long stableId) {
    this.stableIdForItem = staticFunction(stableId);
    return this;
  }

  @NonNull
  @Override
  public DBRPMain onRecycle(@RecycleConfig final int recycleConfig) {
    this.recycleConfig = recycleConfig;
    return this;
  }

  @NonNull
  @Override
  public DBRPMain collectionId(final int collectionId) {
    this.collectionId = collectionId;
    return this;
  }

  private static final class CompiledRepositoryPresenter extends RepositoryPresenter {
    @NonNull
    private final Function<Object, Integer> itemId;
    @NonNull
    private final Function<Object, List<Object>> converter;
    @NonNull
    private final Function<Object, Integer> layoutId;
    @NonNull
    private final Function<Object, Long> stableIdForItem;
    @RecycleConfig
    private final int recycleConfig;
    private final int collectionId;
    @NonNull
    private final SparseArray<Object> handlers;
    private final boolean enableDiff;
    @NonNull
    private final Function<Object, Object> keyForItem;
    private final boolean detectMoves;

    @NonNull
    private WeakReference<Object> dataRef = new WeakReference<>(null);
    @NonNull
    private List items = emptyList();

    CompiledRepositoryPresenter(
        @NonNull final Function<Object, Integer> itemId,
        @NonNull final Function<Object, Integer> layoutId,
        @NonNull final Function<Object, Long> stableIdForItem,
        @NonNull final SparseArray<Object> handlers,
        final int recycleConfig,
        @NonNull final Function<Object, List<Object>> converter,
        final int collectionId,
        @NonNull final Function<Object, Object> keyForItem,
        final boolean detectMoves) {
      this.itemId = itemId;
      this.collectionId = collectionId;
      this.converter = converter;
      this.layoutId = layoutId;
      this.stableIdForItem = stableIdForItem;
      this.recycleConfig = recycleConfig;
      this.handlers = handlers;
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
      final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
      final Integer itemVariable = itemId.apply(item);
      if (itemVariable != BR_NO_ID) {
        viewDataBinding.setVariable(itemVariable, item);
        view.setTag(R.id.agera__rvdatabinding__item_id, itemVariable);
      }
      if (collectionId != BR_NO_ID) {
        viewDataBinding.setVariable(collectionId, data);
        view.setTag(R.id.agera__rvdatabinding__collection_id, collectionId);
      }
      for (int i = 0; i < handlers.size(); i++) {
        final int variableId = handlers.keyAt(i);
        viewDataBinding.setVariable(variableId, handlers.valueAt(i));
      }
      viewDataBinding.executePendingBindings();
    }

    @Override
    public void recycle(@NonNull final RecyclerView.ViewHolder holder) {
      if (recycleConfig != 0) {
        final View view = holder.itemView;
        final ViewDataBinding viewDataBinding = DataBindingUtil.bind(view);
        if ((recycleConfig & CLEAR_ITEM) != 0) {
          final Object tag = view.getTag(R.id.agera__rvdatabinding__item_id);
          view.setTag(R.id.agera__rvdatabinding__item_id, null);
          if (tag instanceof Integer) {
            viewDataBinding.setVariable((int) tag, null);
          }
        }
        if ((recycleConfig & CLEAR_COLLECTION) != 0) {
          final Object collectionTag = view.getTag(R.id.agera__rvdatabinding__collection_id);
          view.setTag(R.id.agera__rvdatabinding__collection_id, null);
          if (collectionTag instanceof Integer) {
            viewDataBinding.setVariable((int) collectionTag, null);
          }
        }
        if ((recycleConfig & CLEAR_HANDLERS) != 0) {
          for (int i = 0; i < handlers.size(); i++) {
            viewDataBinding.setVariable(handlers.keyAt(i), null);
          }
        }
        viewDataBinding.executePendingBindings();
      }
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
