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

import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.rvadapter.RepositoryPresenters.repositoryPresenterOf;
import static com.google.android.agera.rvadapter.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Functions;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RepositoryPresentersTest {
  private static final String STRING = "string";
  private static final String SECOND_STRING = "string2";
  private static final Result<String> STRING_RESULT = present(STRING);
  private static final List<String> STRING_LIST = asList(STRING, SECOND_STRING);
  private static final Result<List<String>> STRING_LIST_RESULT = success(STRING_LIST);
  private static final Result<String> FAILURE = Result.<String>failure();
  private static final Result<List<String>> LIST_FAILURE = Result.<List<String>>failure();
  private static final int LAYOUT_ID = 0;
  private static final int DYNAMIC_LAYOUT_ID = 1;
  private static final long STABLE_ID = 2;
  @Mock
  private Binder<String, View> binder;
  @Mock
  private Receiver<View> recycler;
  @Mock
  private Function<String,Integer> layoutForItem;
  private RecyclerView.ViewHolder viewHolder;
  @Mock
  private View view;

  @Before
  public void setUp() {
    initMocks(this);
    viewHolder = new RecyclerView.ViewHolder(view){};
    when(layoutForItem.apply(SECOND_STRING)).thenReturn(DYNAMIC_LAYOUT_ID);
  }

  @Test
  public void shouldBindRepositoryPresenterOfResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResult();
    resultRepositoryPresenter.getItemCount(STRING_RESULT);
    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);
    verify(binder).bind(STRING, view);
  }

  @Test
  public void shouldBindRepositoryPresenterOfResultWithoutBinder() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .forResult();
    resultRepositoryPresenter.getItemCount(STRING_RESULT);
    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);
  }


  @Test
  public void shouldBindRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResultList();
    resultListRepositoryPresenter.getItemCount(STRING_LIST_RESULT);
    resultListRepositoryPresenter.bind(STRING_LIST_RESULT, 1, viewHolder);
    verify(binder).bind(SECOND_STRING, view);
  }

  @Test
  public void shouldBindRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forList();
    listRepositoryPresenter.getItemCount(STRING_LIST);
    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);
    verify(binder).bind(SECOND_STRING, view);
  }

  @Test
  public void shouldRecycleViewInRepositoryPresenter() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .recycleWith(recycler)
            .forList();
    listRepositoryPresenter.getItemCount(STRING_LIST);
    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);
    listRepositoryPresenter.recycle(viewHolder);
    verify(recycler).accept(view);
  }

  @Test
  public void shouldHandleRecycleWithoutRecycler() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forList();
    listRepositoryPresenter.getItemCount(STRING_LIST);
    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);
    listRepositoryPresenter.recycle(viewHolder);
  }

  @Test
  public void shouldReturnZeroForCountOfRepositoryPresenterOfFailedResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResult();
    assertThat(resultRepositoryPresenter.getItemCount(FAILURE), is(0));
  }

  @Test
  public void shouldReturnOneForCountOfRepositoryPresenterOfSuccessfulResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResult();
    assertThat(resultRepositoryPresenter.getItemCount(STRING_RESULT), is(1));
  }

  @Test
  public void shouldReturnListSizeForCountOfRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forList();
    assertThat(listRepositoryPresenter.getItemCount(STRING_LIST), is(STRING_LIST.size()));
  }

  @Test
  public void shouldReturnZeroForCountOfRepositoryPresenterOfFailedResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResultList();
    assertThat(resultListRepositoryPresenter.getItemCount(LIST_FAILURE), is(0));
  }

  @Test
  public void shouldReturnListSizeForCountOfRepositoryPresenterOfSuccessfulResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResultList();
    assertThat(resultListRepositoryPresenter.getItemCount(STRING_LIST_RESULT),
        is(STRING_LIST.size()));
  }

  @Test
  public void shouldGenerateLayoutForItemOfRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layoutForItem(layoutForItem)
            .forResultList();
    resultListRepositoryPresenter.getItemCount(STRING_LIST_RESULT);
    assertThat(resultListRepositoryPresenter.getLayoutResId(STRING_LIST_RESULT, 1),
        is(DYNAMIC_LAYOUT_ID));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(RepositoryPresenters.class, hasPrivateConstructor());
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forResult();
    resultRepositoryPresenter.getItemCount(STRING_RESULT);
    assertThat(resultRepositoryPresenter.getItemId(STRING_RESULT, 0), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forResultList();
    resultListRepositoryPresenter.getItemCount(STRING_LIST_RESULT);
    assertThat(resultListRepositoryPresenter.getItemId(STRING_LIST_RESULT, 1), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forList();
    listRepositoryPresenter.getItemCount(STRING_LIST);
    assertThat(listRepositoryPresenter.getItemId(STRING_LIST, 1), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfListWithBinder() {
    final RepositoryPresenter<List<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .bindWith(binder)
            .forList();
    resultRepositoryPresenter.getItemCount(STRING_LIST);
    assertThat(resultRepositoryPresenter.getItemId(STRING_LIST, 1), is(STABLE_ID));
  }

  @Test
  public void shouldAllowStableIdMethodForAnySuperType() {
    repositoryPresenterOf(String.class)
        .layout(LAYOUT_ID)
        .stableIdForItem(Functions.<CharSequence, Long>staticFunction(STABLE_ID))
        .forResult();
  }
}
