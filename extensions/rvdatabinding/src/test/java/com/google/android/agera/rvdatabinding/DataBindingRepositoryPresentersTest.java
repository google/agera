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
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenters.dataBindingRepositoryPresenterOf;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ALL;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_COLLECTION;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_HANDLERS;
import static com.google.android.agera.rvdatabinding.RecycleConfig.CLEAR_ITEM;
import static com.google.android.agera.rvdatabinding.RecycleConfig.DO_NOTHING;
import static com.google.android.agera.rvdatabinding.test.VerifyingWrappers.verifyingWrapper;
import static com.google.android.agera.rvdatabinding.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.databinding.ViewDataBinding;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import com.google.android.agera.Function;
import com.google.android.agera.Functions;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenter;
import com.google.android.agera.rvdatabinding.test.DiffingLogic;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DataBindingRepositoryPresentersTest {
  private static final String STRING = "string";
  private static final String FIRST_STRING_CHARACTER = "s";
  private static final String SECOND_STRING = "string2";
  private static final Result<String> STRING_RESULT = present(STRING);
  private static final List<String> STRING_LIST = asList(STRING, SECOND_STRING);
  private static final Result<List<String>> STRING_LIST_RESULT = success(STRING_LIST);
  private static final Result<String> FAILURE = failure();
  private static final Result<List<String>> LIST_FAILURE = failure();
  private static final Object HANDLER = new Object();
  private static final Object SECOND_HANDLER = new Object();
  @LayoutRes
  private static final int LAYOUT_ID = 1;
  private static final int DYNAMIC_LAYOUT_ID = 2;
  private static final int ITEM_ID = 3;
  private static final int HANDLER_ID = 4;
  private static final int SECOND_HANDLER_ID = 5;
  private static final int COLLECTION_ID = 6;
  private static final long STABLE_ID = 2;
  @Mock
  private Function<String, Integer> layoutForItem;
  @Mock
  private Function<String, Integer> itemIdForItem;
  @Mock
  private ViewDataBinding viewDataBinding;
  @Mock
  private View view;
  @Mock
  private ListUpdateCallback listUpdateCallback;
  private ViewHolder viewHolder;

  @Before
  public void setUp() {
    initMocks(this);
    viewHolder = new ViewHolder(view) {};
    setDataBinding(viewDataBinding, LAYOUT_ID);
    setDataBinding(viewDataBinding, DYNAMIC_LAYOUT_ID);
    when(view.getTag()).thenReturn("string");
    when(layoutForItem.apply(SECOND_STRING)).thenReturn(DYNAMIC_LAYOUT_ID);
    when(itemIdForItem.apply(SECOND_STRING)).thenReturn(ITEM_ID);
  }

  @Test
  public void shouldBindRepositoryPresenterOfResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .forResult();

    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);

    verify(view).setTag(R.id.agera__rvdatabinding__item_id, ITEM_ID);
    verify(viewDataBinding).setVariable(ITEM_ID, STRING);
    verify(viewDataBinding).setVariable(HANDLER_ID, HANDLER);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, SECOND_HANDLER);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindRepositoryPresenterWithoutItem() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .forItem();

    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindRepositoryPresenterOfCollection() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forCollection(new Function<String, List<String>>() {
              @NonNull
              @Override
              public List<String> apply(@NonNull final String input) {
                return singletonList(valueOf(input.charAt(0)));
              }
            });
    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, FIRST_STRING_CHARACTER);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindRepositoryPresenterCollectionOfCollection() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());
    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, FIRST_STRING_CHARACTER);
    verify(viewDataBinding).setVariable(COLLECTION_ID, STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldHandleRecycleOfRepositoryPresenterWithoutItemId() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .onRecycle(CLEAR_ALL)
            .forItem();

    repositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldNotRecycleRepositoryPresenterOfResultWithNoRecycling() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(DO_NOTHING)
            .forResult();

    resultRepositoryPresenter.recycle(viewHolder);

    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultWithItemRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);

    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .forResult();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultWithAllRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .forResult();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultWithHandlerRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .forResult();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldNotRecycleRepositoryPresenterOfCollectionWithNoRecycling() {
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(DO_NOTHING)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());

    resultRepositoryPresenter.recycle(viewHolder);

    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfCollectionWithItemRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);

    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfCollectionWithAllRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    when(view.getTag(R.id.agera__rvdatabinding__collection_id)).thenReturn(COLLECTION_ID);
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).setVariable(COLLECTION_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }


  @Test
  public void shouldRecycleRepositoryPresenterOfCollectionWithCollectionRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    when(view.getTag(R.id.agera__rvdatabinding__collection_id)).thenReturn(COLLECTION_ID);
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_COLLECTION)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(COLLECTION_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfCollectionWithHandlerRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    when(view.getTag(R.id.agera__rvdatabinding__collection_id)).thenReturn(COLLECTION_ID);
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .collectionId(COLLECTION_ID)
            .forCollection(new StringToFirstCharStringList());

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .forResultList();

    resultListRepositoryPresenter.bind(STRING_LIST_RESULT, 1, viewHolder);

    verify(view).setTag(R.id.agera__rvdatabinding__item_id, ITEM_ID);
    verify(viewDataBinding).setVariable(ITEM_ID, SECOND_STRING);
    verify(viewDataBinding).setVariable(HANDLER_ID, HANDLER);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldNotRecycleRepositoryPresenterOfResultListWithNoRecycling() {
    final RepositoryPresenter<Result<List<String>>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(DO_NOTHING)
            .forResultList();

    resultRepositoryPresenter.recycle(viewHolder);

    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultListWithItemRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);

    final RepositoryPresenter<Result<List<String>>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .forResultList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultListWithAllRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<Result<List<String>>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .forResultList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfResultListWithHandlerRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<Result<List<String>>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .forResultList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfItemWithItemRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);

    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .forItem();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfItemWithAllRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .forItem();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfItemWithHandlerRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .forItem();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldBindRepositoryPresenterOfItem() {
    final RepositoryPresenter<String> itemRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forItem();
    itemRepositoryPresenter.bind(STRING, 0, viewHolder);
  }

  @Test
  public void shouldBindRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forList();

    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);

    verify(view).setTag(R.id.agera__rvdatabinding__item_id, ITEM_ID);
    verify(viewDataBinding).setVariable(ITEM_ID, SECOND_STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldNotRecycleRepositoryPresenterOfListWithNoRecycling() {
    final RepositoryPresenter<List<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(DO_NOTHING)
            .forList();

    resultRepositoryPresenter.recycle(viewHolder);

    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfListWithItemRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);

    final RepositoryPresenter<List<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ITEM)
            .forList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRecycleRepositoryPresenterOfListWithAllRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<List<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_ALL)
            .forList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, null);
    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }


  @Test
  public void shouldRecycleRepositoryPresenterOfListWithHandlerRecycling() {
    when(view.getTag(R.id.agera__rvdatabinding__item_id)).thenReturn(ITEM_ID);
    final RepositoryPresenter<List<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .onRecycle(CLEAR_HANDLERS)
            .forList();

    resultRepositoryPresenter.recycle(viewHolder);

    verify(viewDataBinding).setVariable(HANDLER_ID, null);
    verify(viewDataBinding).setVariable(SECOND_HANDLER_ID, null);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldReturnZeroForCountOfRepositoryPresenterOfFailedResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .forResult();
    assertThat(resultRepositoryPresenter.getItemCount(FAILURE), is(0));
  }

  @Test
  public void shouldReturnOneForCountOfRepositoryPresenterOfSuccessfulResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forResult();
    assertThat(resultRepositoryPresenter.getItemCount(STRING_RESULT), is(1));
  }

  @Test
  public void shouldReturnListSizeForCountOfRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forList();
    assertThat(listRepositoryPresenter.getItemCount(STRING_LIST), is(STRING_LIST.size()));
  }

  @Test
  public void shouldReturnZeroForCountOfRepositoryPresenterOfFailedResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forResultList();
    assertThat(resultListRepositoryPresenter.getItemCount(LIST_FAILURE), is(0));
  }

  @Test
  public void shouldReturnListSizeForCountOfRepositoryPresenterOfSuccessfulResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forResultList();
    assertThat(resultListRepositoryPresenter.getItemCount(STRING_LIST_RESULT),
        is(STRING_LIST.size()));
  }

  @Test
  public void shouldGenerateLayoutForItemOfRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layoutForItem(layoutForItem)
            .itemId(ITEM_ID)
            .forResultList();
    assertThat(resultListRepositoryPresenter.getLayoutResId(STRING_LIST_RESULT, 1),
        is(DYNAMIC_LAYOUT_ID));
  }


  @Test
  public void shouldGenerateItemIdForItemOfRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemIdForItem(itemIdForItem)
            .forResultList();
    resultListRepositoryPresenter.bind(STRING_LIST_RESULT, 1, viewHolder);
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfItem() {
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
        .forItem();
    assertThat(resultRepositoryPresenter.getItemId(STRING, 0), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forResult();
    assertThat(resultRepositoryPresenter.getItemId(STRING_RESULT, 0), is(STABLE_ID));
  }


  @Test
  public void shouldReturnStaticStableIdForRepositoryPresenterOfItem() {
    final RepositoryPresenter<String> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableId(STABLE_ID)
            .forItem();
    assertThat(resultRepositoryPresenter.getItemId(STRING, 0), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStaticStableIdForRepositoryPresenterOfResult() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableId(STABLE_ID)
            .forResult();
    assertThat(resultRepositoryPresenter.getItemId(STRING_RESULT, 0), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forResultList();
    assertThat(resultListRepositoryPresenter.getItemId(STRING_LIST_RESULT, 0), is(STABLE_ID));
  }

  @Test
  public void shouldReturnStableIdForRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .stableIdForItem(Functions.<String, Long>staticFunction(STABLE_ID))
            .forList();
    assertThat(listRepositoryPresenter.getItemId(STRING_LIST, 0), is(STABLE_ID));
  }

  @Test
  public void shouldHandleRebindWithSameData() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forItem();

    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
    reset(viewDataBinding);

    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldHandleRebindWithNewData() {
    final RepositoryPresenter<String> repositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forItem();

    repositoryPresenter.bind(STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
    reset(viewDataBinding);

    repositoryPresenter.bind(SECOND_STRING, 0, viewHolder);

    verify(viewDataBinding).setVariable(ITEM_ID, SECOND_STRING);
    verify(viewDataBinding).executePendingBindings();
    verifyNoMoreInteractions(viewDataBinding);
  }

  @Test
  public void shouldRefuseFineGrainedEventsWithoutDiffWith() {
    final RepositoryPresenter<String> presenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forItem();

    final boolean fineGrained = presenter.getUpdates("String1", "String2", listUpdateCallback);

    assertThat(fineGrained, is(false));
  }

  @Test
  public void shouldNotifyFineGrainedEventsWithDiffWith() {
    final List<String> oldData = asList("A:1", "B:2", "C:3");
    final List<String> newData = asList("B:2", "A:4", "C:5");
    final DiffingLogic diffingLogic = new DiffingLogic(oldData, newData);
    final RepositoryPresenter<List<String>> diffingPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .diffWith(diffingLogic, false)
            .forList();

    final boolean fineGrained = diffingPresenter.getUpdates(oldData, newData, listUpdateCallback);

    assertThat(fineGrained, is(true));
    DiffUtil.calculateDiff(diffingLogic, false).dispatchUpdatesTo(
        verifyingWrapper(listUpdateCallback));
    verifyNoMoreInteractions(listUpdateCallback);
  }

  @Test
  public void shouldNotifyFineGrainedEventsWithDiffWithMoveDetection() {
    final List<String> oldData = asList("A:1", "B:2", "C:3", "D:0");
    final List<String> newData = asList("B:2", "D:0", "A:4", "C:5");
    final DiffingLogic diffingLogic = new DiffingLogic(oldData, newData);
    final RepositoryPresenter<List<String>> diffingPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .diffWith(diffingLogic, true)
            .forCollection(Functions.<List<String>>identityFunction());

    final boolean fineGrained = diffingPresenter.getUpdates(oldData, newData, listUpdateCallback);

    assertThat(fineGrained, is(true));
    DiffUtil.calculateDiff(diffingLogic, true).dispatchUpdatesTo(
        verifyingWrapper(listUpdateCallback));
    verifyNoMoreInteractions(listUpdateCallback);
  }

  @Test
  public void shouldNotifySingleItemFineGrainedEventsWithDiff() {
    final Result<String> withA = success("A");
    final Result<String> withB = success("B");
    final Result<String> without = failure();
    final RepositoryPresenter<Result<String>> diffingPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .diff()
            .forResult();

    boolean fineGrained = diffingPresenter.getUpdates(withA, withB, listUpdateCallback);

    assertThat(fineGrained, is(true));
    verify(listUpdateCallback).onChanged(0, 1, null);
    verifyNoMoreInteractions(listUpdateCallback);

    fineGrained = diffingPresenter.getUpdates(withA, without, listUpdateCallback);

    assertThat(fineGrained, is(true));
    verify(listUpdateCallback).onRemoved(0, 1);
    verifyNoMoreInteractions(listUpdateCallback);

    fineGrained = diffingPresenter.getUpdates(without, withB, listUpdateCallback);

    assertThat(fineGrained, is(true));
    verify(listUpdateCallback).onInserted(0, 1);
    verifyNoMoreInteractions(listUpdateCallback);
  }

  @Test
  public void shouldNotifyBlanketChangeEventForSameObjectForOldAndNewData() {
    final List<String> oneList = asList("A:0", "B:1");
    final DiffingLogic diffingLogic = new DiffingLogic(oneList, oneList);
    final RepositoryPresenter<List<String>> diffingPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .diffWith(diffingLogic, false)
            .forList();

    final boolean fineGrained = diffingPresenter.getUpdates(oneList, oneList, listUpdateCallback);

    assertThat(fineGrained, is(true));
    verify(listUpdateCallback).onChanged(0, oneList.size(), null);
    verifyNoMoreInteractions(listUpdateCallback);
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(DataBindingRepositoryPresenters.class, hasPrivateConstructor());
  }

  private static final class StringToFirstCharStringList implements Function<String, List<String>> {
    @NonNull
    @Override
    public List<String> apply(@NonNull final String input) {
      return singletonList(valueOf(input.charAt(0)));
    }
  }
}