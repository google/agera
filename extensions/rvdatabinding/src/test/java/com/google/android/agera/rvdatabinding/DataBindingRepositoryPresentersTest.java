package com.google.android.agera.rvdatabinding;

import static android.databinding.DataBinderMapper.setDataBinding;
import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.rvadapter.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenters.dataBindingRepositoryPresenterOf;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import com.google.android.agera.rvadapter.RepositoryPresenter;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DataBindingRepositoryPresentersTest {
  private static final String STRING = "string";
  private static final String SECOND_STRING = "string2";
  private static final Result<String> STRING_RESULT = present(STRING);
  private static final List<String> STRING_LIST = asList(STRING, SECOND_STRING);
  private static final Result<List<String>> STRING_LIST_RESULT = success(STRING_LIST);
  private static final Result<String> FAILURE = Result.<String>failure();
  private static final Result<List<String>> LIST_FAILURE = Result.<List<String>>failure();
  private static final Object HANDLER = new Object();
  private static final Object SECOND_HANDLER = new Object();
  private static final int LAYOUT_ID = 1;
  private static final int DYNAMIC_LAYOUT_ID = 2;
  private static final int ITEM_ID = 3;
  private static final int DYNAMIC_ITEM_ID = 4;
  private static final int HANDLER_ID = 5;
  private static final int SECOND_HANDLER_ID = 6;
  @Mock
  private Binder<String, View> binder;
  @Mock
  private Function<String, Integer> layoutForItem;
  @Mock
  private Function<String, Integer> itemIdForItem;
  @Mock
  private ViewDataBinding viewDataBinding;
  @Mock
  private View view;
  private RecyclerView.ViewHolder viewHolder;

  @Before
  public void setUp() {
    initMocks(this);
    viewHolder = new RecyclerView.ViewHolder(view){};
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
  }

  @Test
  public void shouldBindRepositoryPresenterOfResultWithoutBinder() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .handler(HANDLER_ID, HANDLER)
            .handler(SECOND_HANDLER_ID, SECOND_HANDLER)
            .forResult();
    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);
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
  }

  @Test
  public void shouldBindRepositoryPresenterOfList() {
    final RepositoryPresenter<List<String>> listRepositoryPresenter =
        dataBindingRepositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .itemId(ITEM_ID)
            .forList();
    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);
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
  public void shouldHavePrivateConstructor() {
    assertThat(DataBindingRepositoryPresenters.class, hasPrivateConstructor());
  }
}