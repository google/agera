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

import static com.google.android.agera.rvadapter.LayoutPresenters.layout;
import static com.google.android.agera.rvadapter.LayoutPresenters.layoutPresenterFor;
import static com.google.android.agera.rvadapter.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.support.annotation.LayoutRes;
import android.view.View;
import com.google.android.agera.Receiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LayoutPresentersTest {
  @LayoutRes
  private static final int LAYOUT_ID = 0;
  @Mock
  private Receiver<View> binder;
  @Mock
  private Receiver<View> recycler;
  @Mock
  private View view;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void shouldReturnLayoutIdForCompiledLayout() {
    final LayoutPresenter layoutPresenter =
        layoutPresenterFor(LAYOUT_ID)
            .build();

    assertThat(layoutPresenter.getLayoutResId(), is(LAYOUT_ID));
  }

  @Test
  public void shouldReturnLayoutIdForLayout() {
    final LayoutPresenter layoutPresenter = layout(LAYOUT_ID);

    assertThat(layoutPresenter.getLayoutResId(), is(LAYOUT_ID));
  }

  @Test
  public void shouldBindCompiledLayout() {
    final LayoutPresenter layoutPresenter =
        layoutPresenterFor(LAYOUT_ID)
            .bindWith(binder)
            .build();

    layoutPresenter.bind(view);

    verify(binder).accept(view);
  }

  @Test
  public void shouldBindCompiledLayoutWithoutBinder() {
    final LayoutPresenter layoutPresenter =
        layoutPresenterFor(LAYOUT_ID)
            .build();

    layoutPresenter.bind(view);
  }

  @Test
  public void shouldBindLayoutWithoutBinder() {
    final LayoutPresenter layoutPresenter = layout(LAYOUT_ID);

    layoutPresenter.bind(view);
  }

  @Test
  public void shouldRecycleViewInCompiledLayout() {
    final LayoutPresenter layoutPresenter =
        layoutPresenterFor(LAYOUT_ID)
            .bindWith(binder)
            .recycleWith(recycler)
            .build();

    layoutPresenter.recycle(view);

    verify(recycler).accept(view);
  }

  @Test
  public void shouldHandleRecycleWithoutRecyclerInCompiledLayout() {
    final LayoutPresenter layoutPresenter =
        layoutPresenterFor(LAYOUT_ID)
            .bindWith(binder)
            .build();

    layoutPresenter.recycle(view);
  }

  @Test
  public void shouldHandleRecycleWithoutRecyclerInLayout() {
    final LayoutPresenter layoutPresenter = layout(LAYOUT_ID);

    layoutPresenter.recycle(view);
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(LayoutPresenters.class, hasPrivateConstructor());
  }
}
