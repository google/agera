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

import com.google.android.agera.Binder;
import com.google.android.agera.Function;
import com.google.android.agera.Result;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

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
  @Mock
  private Binder<String, View> binder;
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
    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);
    verify(binder).bind(STRING, view);
  }

  @Test
  public void shouldBindRepositoryPresenterOfResultWithoutBinder() {
    final RepositoryPresenter<Result<String>> resultRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .forResult();
    resultRepositoryPresenter.bind(STRING_RESULT, 0, viewHolder);
  }


  @Test
  public void shouldBindRepositoryPresenterOfResultList() {
    final RepositoryPresenter<Result<List<String>>> resultListRepositoryPresenter =
        repositoryPresenterOf(String.class)
            .layout(LAYOUT_ID)
            .bindWith(binder)
            .forResultList();
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
    listRepositoryPresenter.bind(STRING_LIST, 1, viewHolder);
    verify(binder).bind(SECOND_STRING, view);
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
    assertThat(resultListRepositoryPresenter.getLayoutResId(STRING_LIST_RESULT, 1),
        is(DYNAMIC_LAYOUT_ID));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(RepositoryPresenters.class, hasPrivateConstructor());
  }
}
