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

import static android.databinding.DataBinderMapper.setDataBinding;
import static com.google.android.agera.rvdatabinding.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.rvdatabinding.DataBindingLayoutPresenters.dataBindingLayoutPresenterFor;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ALL;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ITEM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.rvadapter.LayoutPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DataBindingLayoutPresentersTest {
  private static final Object HANDLER = new Object();
  private static final Object SECOND_HANDLER = new Object();
  @LayoutRes
  private static final int LAYOUT_ID = 1;
  private static final int HANDLER_ID = 4;
  private static final int SECOND_HANDLER_ID = 5;
  @Mock
  private Binder<String, View> binder;
  @Mock
  private ViewDataBinding viewDataBinding;
  @Mock
  private View view;

  @Before
  public void setUp() {
    initMocks(this);
    setDataBinding(viewDataBinding, LAYOUT_ID);
    when(view.getTag()).thenReturn("string");
  }


  @Test
  public void shouldReturnLayoutForLayoutResId() {
    final LayoutPresenter layoutPresenter =
        dataBindingLayoutPresenterFor(LAYOUT_ID)
            .onRecycle(CLEAR_ITEM)
            .build();

    assertThat(layoutPresenter.getLayoutResId(), is(LAYOUT_ID));
  }

  @Test
  public void shouldDoNothingWithLayoutPresenterOnRecycleItemOnlyRecycling() {
    final LayoutPresenter layoutPresenter =
        dataBindingLayoutPresenterFor(LAYOUT_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .build();

    layoutPresenter.recycle(view);

    verifyZeroInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleLayoutPresenterWithAllRecycling() {
    final LayoutPresenter layoutPresenter =
        dataBindingLayoutPresenterFor(LAYOUT_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .build();

    layoutPresenter.recycle(view);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleLayoutPresenterWithHandlerRecycling() {
    final LayoutPresenter layoutPresenter =
        dataBindingLayoutPresenterFor(LAYOUT_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .build();

    layoutPresenter.recycle(view);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindLayoutPresenter() {
    final LayoutPresenter layoutPresenter =
        dataBindingLayoutPresenterFor(LAYOUT_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .build();

    layoutPresenter.bind(view);

    verify(viewDataBinding).setVariable(HANDLER_ID, HANDLER);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, SECOND_HANDLER);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(DataBindingLayoutPresenters.class, hasPrivateConstructor());
  }
}