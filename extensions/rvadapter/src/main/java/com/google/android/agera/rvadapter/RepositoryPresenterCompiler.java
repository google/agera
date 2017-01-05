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

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Preconditions.checkNotNull;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPLayout;
import com.google.android.agera.rvadapter.RepositoryPresenterCompilerStates.RPViewBinderRecycleStableIdCompile;
import java.util.List;

@SuppressWarnings({"unchecked, rawtypes"})
final class RepositoryPresenterCompiler implements RPLayout, RPViewBinderRecycleStableIdCompile {
  @NonNull
  private static final NullHandler NULL_HANDLER = new NullHandler();
  private Function<Object, Integer> layoutForItem;
  @NonNull
  private Binder binder = NULL_HANDLER;
  @NonNull
  private Receiver recycler = NULL_HANDLER;
  @NonNull
  private Function<Object, Long> stableIdForItem = staticFunction(RecyclerView.NO_ID);

  @NonNull
  @Override
  public RepositoryPresenter<List> forList() {
    return new ListBasicRepositoryPresenter(layoutForItem, binder, recycler, stableIdForItem);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result> forResult() {
    return new SingleResultRepositoryPresenter(layoutForItem, binder, recycler, stableIdForItem);
  }

  @NonNull
  @Override
  public RepositoryPresenter<Result<List>> forResultList() {
    return new ListResultRepositoryPresenter(layoutForItem, binder, recycler, stableIdForItem);
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
  public Object recycleWith(@NonNull final Receiver recycler) {
    this.recycler = recycler;
    return this;
  }

  private static final class NullHandler implements Binder, Receiver {
    @Override
    public void bind(@NonNull Object o, @NonNull Object o2) {}

    @Override
    public void accept(@NonNull final Object o) {}
  }

  private abstract static class BasicRepositoryPresenter<TVal, T>
      extends RepositoryPresenter<T> {
    @NonNull
    private final Function<Object, Integer> layoutId;
    @NonNull
    private final Binder<TVal, View> binder;
    @NonNull
    private final Function<TVal, Long> stableIdForItem;
    @NonNull
    private final Receiver<View> recycler;

    BasicRepositoryPresenter(@NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<TVal, View> binder,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<TVal, Long> stableIdForItem) {
      this.layoutId = checkNotNull(layoutId);
      this.binder = checkNotNull(binder);
      this.recycler = checkNotNull(recycler);
      this.stableIdForItem = stableIdForItem;
    }

    @NonNull
    protected abstract TVal getValue(@NonNull final T data, final int index);

    @Override
    public final int getLayoutResId(@NonNull final T data, final int index) {
      return layoutId.apply(getValue(data, index));
    }

    @Override
    public long getItemId(@NonNull T data, int index) {
      return stableIdForItem.apply(getValue(data, index));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void bind(@NonNull final T data, final int index,
        @NonNull final RecyclerView.ViewHolder holder) {
      binder.bind(getValue(data, index), holder.itemView);
    }

    @Override
    public void recycle(@NonNull final RecyclerView.ViewHolder holder) {
      super.recycle(holder);
      recycler.accept(holder.itemView);
    }
  }

  private static final class ListBasicRepositoryPresenter<T>
      extends BasicRepositoryPresenter<T, List<T>> {

    public ListBasicRepositoryPresenter(@NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<T, View> binder,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<T, Long> stableIdForItem) {
      super(layoutId, binder, recycler, stableIdForItem);
    }

    @Override
    public int getItemCount(@NonNull final List<T> data) {
      return data.size();
    }

    @NonNull
    @Override
    protected T getValue(@NonNull final List<T> data, final int index) {
      return data.get(index);
    }
  }

  private abstract static class ResultRepositoryPresenter<TVal, T>
      extends BasicRepositoryPresenter<TVal, Result<T>> {

    ResultRepositoryPresenter(@NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<TVal, View> binder,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<TVal, Long> stableIdForItem) {
      super(layoutId, binder, recycler, stableIdForItem);
    }

    @NonNull
    @Override
    protected TVal getValue(@NonNull Result<T> data, int index) {
      return getResultValue(data.get(), index);
    }

    @Override
    public final int getItemCount(@NonNull final Result<T> data) {
      return data.failed() ? 0 : getResultCount(data.get());
    }

    protected abstract int getResultCount(@NonNull T data);

    @NonNull
    protected abstract TVal getResultValue(@NonNull T data, int index);
  }

  private static final class SingleResultRepositoryPresenter<T>
      extends ResultRepositoryPresenter<T, T> {
    public SingleResultRepositoryPresenter(@NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<T, View> binder,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<T, Long> stableIdForItem) {
      super(layoutId, binder, recycler, stableIdForItem);
    }

    @Override
    protected int getResultCount(@NonNull final T data) {
      return 1;
    }

    @NonNull
    @Override
    protected T getResultValue(@NonNull final T data, final int index) {
      return data;
    }
  }

  private static final class ListResultRepositoryPresenter<T>
      extends ResultRepositoryPresenter<T, List<T>> {

    public ListResultRepositoryPresenter(@NonNull final Function<Object, Integer> layoutId,
        @NonNull final Binder<T, View> binder,
        @NonNull final Receiver<View> recycler,
        @NonNull final Function<T, Long> stableIdForItem) {
      super(layoutId, binder, recycler, stableIdForItem);
    }

    @Override
    protected int getResultCount(@NonNull final List<T> data) {
      return data.size();
    }

    @NonNull
    @Override
    protected T getResultValue(@NonNull final List<T> data, final int index) {
      return data.get(index);
    }
  }
}
