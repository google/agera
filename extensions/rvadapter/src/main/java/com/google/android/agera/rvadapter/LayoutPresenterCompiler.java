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

import static com.google.android.agera.Receivers.nullReceiver;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import com.google.android.agera.Receiver;
import com.google.android.agera.rvadapter.LayoutPresenterCompilerStates.LPBinderRecycleCompile;
import com.google.android.agera.rvadapter.LayoutPresenterCompilerStates.LPCompile;
import com.google.android.agera.rvadapter.LayoutPresenterCompilerStates.LPRecycleCompile;

@SuppressWarnings({"unchecked, rawtypes"})
final class LayoutPresenterCompiler implements LPBinderRecycleCompile {
  @NonNull
  private Receiver recycler = nullReceiver();
  @LayoutRes
  private final int layoutId;
  @NonNull
  private Receiver<View> updater = nullReceiver();

  LayoutPresenterCompiler(@LayoutRes final int layoutId) {
    this.layoutId = layoutId;
  }

  @NonNull
  @Override
  public LayoutPresenter compile() {
    return new CompiledLayoutPresenter(layoutId, recycler, updater);
  }

  @NonNull
  @Override
  public LPRecycleCompile bindWith(@NonNull final Receiver<View> binder) {
    this.updater = binder;
    return this;
  }

  @NonNull
  @Override
  public LPCompile recycleWith(@NonNull final Receiver<View> recycler) {
    this.recycler = recycler;
    return this;
  }

  private static final class CompiledLayoutPresenter extends LayoutPresenter {
    @LayoutRes
    private int layoutId;
    @NonNull
    private Receiver<View> recycler;
    @NonNull
    private Receiver<View> updater;

    CompiledLayoutPresenter(
        @LayoutRes final int layoutId,
        @NonNull final Receiver<View> recycler,
        @NonNull final Receiver<View> updater) {
      this.layoutId = layoutId;
      this.recycler = recycler;
      this.updater = updater;
    }

    @Override
    public int getLayoutResId() {
      return layoutId;
    }

    @Override
    public void bind(@NonNull final View view) {
      updater.accept(view);
    }

    @Override
    public void recycle(@NonNull final View view) {
      recycler.accept(view);
    }
  }
}
