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

import static org.mockito.MockitoAnnotations.initMocks;

import android.support.annotation.NonNull;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class LayoutPresenterTest {

  @Mock
  private View view;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void shouldDoNothingOnRecycleByDefault() {
    new TestLayoutPresenter().recycle(view);
  }

  private static final class TestLayoutPresenter extends LayoutPresenter {
    @Override
    public int getLayoutResId() {
      return 0;
    }

    @Override
    public void bind(@NonNull final View view) {}
  }
}
