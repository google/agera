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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public final class RepositoryPresenterTest {

  @Test
  public void shouldReturnDefaultItemId() throws Exception {
    assertThat(new TestRepositoryPresenter().getItemId(new Object(), 0), is(RecyclerView.NO_ID));
  }

  private static final class TestRepositoryPresenter extends RepositoryPresenter<Object> {
    @Override
    public int getItemCount(@NonNull final Object data) {
      return 0;
    }

    @Override
    public int getLayoutResId(@NonNull final Object data, final int index) {
      return 0;
    }

    @Override
    public void bind(@NonNull final Object data, final int index,
        @NonNull final RecyclerView.ViewHolder holder) {}
  }
}
