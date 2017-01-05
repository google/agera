/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.android.agera;

import static com.google.android.agera.Mergers.staticMerger;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.Repositories.repository;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.SupplierGives.has;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasNotUpdated;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import android.support.annotation.NonNull;
import com.google.android.agera.test.mocks.MockUpdatable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RepositoriesTest {
  private static final int INITIAL_INT_VALUE = 0;
  private static final int INT_VALUE = 2;
  private static final String INITIAL_STRING_VALUE = "init";
  private static final String STRING_VALUE = "string";
  private static final Result<String> RESULT_STRING_VALUE = success(STRING_VALUE);
  private static final List<Integer> INITIAL_VALUE = singletonList(1);
  private static final List<Integer> LIST = asList(1, 2, 3);
  private static final List<Integer> OTHER_LIST = asList(4, 5);
  private static final List<Integer> LIST_AND_OTHER_LIST = asList(1, 2, 3, 4, 5);
  private static final List<Integer> LIST_PLUS_TWO = asList(3, 4, 5);

  private MutableRepository<List<Integer>> listSource;
  private MutableRepository<List<Integer>> otherListSource;
  private MockUpdatable updatable;
  private UpdateDispatcher updateDispatcher;
  @Mock
  private Receiver<Integer> mockIntegerReceiver;
  @Mock
  private Receiver<List<Integer>> mockIntegerListReceiver;
  @Mock
  private Binder<List<Integer>, String> mockIntegerListStringBinder;
  @Mock
  private Supplier<List<Integer>> mockIntegerListSupplier;
  @Mock
  private Supplier<String> mockStringSupplier;
  @Mock
  private Supplier<Result<String>> mockResultStringSupplier;
  @Mock
  private Supplier<Result<String>> mockFailedResultStringSupplier;
  @Mock
  private Function<String, Result<String>> mockResultStringFunction;
  @Mock
  private Function<String, Result<String>> mockFailedResultStringFunction;
  @Mock
  private Predicate<List<Integer>> mockIntegerListPredicate;
  @Mock
  private Function<List<Integer>, Integer> mockIntegerListToIntValueFunction;
  @Mock
  private Merger<String, String, Result<String>> mockResultStringMerger;
  @Mock
  private Merger<String, String, Result<String>> mockFailedResultStringMerger;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockIntegerListSupplier.get()).thenReturn(LIST);
    when(mockStringSupplier.get()).thenReturn(STRING_VALUE);
    when(mockResultStringSupplier.get()).thenReturn(RESULT_STRING_VALUE);
    when(mockFailedResultStringSupplier.get()).thenReturn(Result.<String>failure());
    when(mockResultStringFunction.apply(INITIAL_STRING_VALUE)).thenReturn(RESULT_STRING_VALUE);
    when(mockFailedResultStringFunction.apply(INITIAL_STRING_VALUE))
        .thenReturn(Result.<String>failure());
    when(mockResultStringMerger.merge(INITIAL_STRING_VALUE, STRING_VALUE))
        .thenReturn(RESULT_STRING_VALUE);
    when(mockFailedResultStringMerger.merge(INITIAL_STRING_VALUE, STRING_VALUE))
        .thenReturn(Result.<String>failure());
    when(mockIntegerListToIntValueFunction.apply(Matchers.<List<Integer>>any()))
        .thenReturn(INT_VALUE);
    updateDispatcher = updateDispatcher();
    listSource = mutableRepository(LIST);
    otherListSource = mutableRepository(OTHER_LIST);
    updatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldCreateStaticRepository() {
    assertThat(repository(1), has(1));
  }

  @Test
  public void shouldNotGetUpdatesFromStaticRepository() {
    final Repository<Integer> repository = repository(INITIAL_INT_VALUE);

    updatable.addToObservable(repository);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldBePossibleToObserveMutableRepository() {
    final Repository<Integer> repository = mutableRepository(INITIAL_INT_VALUE);

    updatable.addToObservable(repository);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldGetUpdateFromChangedMutableRepository() {
    final MutableRepository<Integer> repository = mutableRepository(INITIAL_INT_VALUE);
    updatable.addToObservable(repository);

    repository.accept(INT_VALUE);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotGetUpdateFromMutableRepositoryChangedToSameValue() {
    final MutableRepository<Integer> repository = mutableRepository(INITIAL_INT_VALUE);
    updatable.addToObservable(repository);

    repository.accept(INITIAL_INT_VALUE);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldGetUpdateFromRepositoryChangedToNewValue() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenGetFrom(listSource)
        .compile();
    updatable.addToObservable(repository);

    listSource.accept(OTHER_LIST);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotGetUpdateFromRepositoryChangedToSameValue() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(LIST)
        .observe()
        .onUpdatesPerLoop()
        .thenGetFrom(listSource)
        .compile();
    updatable.addToObservable(repository);

    listSource.accept(LIST);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldGetUpdateFromAlwaysNotifyRepositoryChangedToSameValue() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(LIST)
        .observe()
        .onUpdatesPerLoop()
        .thenGetFrom(listSource)
        .notifyIf(staticMerger(true))
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldContainCorrectValueForLazyRepository() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .goLazy()
        .thenGetFrom(listSource)
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST));
  }

  @Test
  public void shouldContainCorrectValueForTransformInExecutor() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .goTo(new SyncExecutor())
        .getFrom(listSource)
        .thenTransform(new AddTwoForEachFunction())
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST_PLUS_TWO));
  }

  @Test
  public void shouldContainCorrectValueForTransformInExecutorMidFlow() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(listSource)
        .goTo(new SyncExecutor())
        .thenTransform(new AddTwoForEachFunction())
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST_PLUS_TWO));
  }

  @Test
  public void shouldContainCorrectValueForLazyTransformMidFlow() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(listSource)
        .goLazy()
        .thenTransform(new AddTwoForEachFunction())
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST_PLUS_TWO));
  }

  @Test
  public void shouldBeAbleToObserveRepository() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenGetFrom(listSource)
        .compile();
    updatable.addToObservable(repository);
  }

  @Test
  public void shouldMergeInSecondSource() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(listSource)
        .thenMergeIn(otherListSource, new ListMerger<Integer, Integer, Integer>())
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST_AND_OTHER_LIST));
  }

  @Test
  public void shouldUpdateOnExplicitObservable() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .thenGetFrom(mockIntegerListSupplier)
        .compile();

    updatable.addToObservable(repository);
    updateDispatcher.update();

    assertThat(repository, has(LIST));
    verify(mockIntegerListSupplier, times(2)).get();
  }

  @Test
  public void shouldReturnDataWithGoLazyOnDemand() {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .goLazy()
        .thenGetFrom(mockIntegerListSupplier)
        .compile();

    updatable.addToObservable(repository);

    verifyZeroInteractions(mockIntegerListSupplier);
    assertThat(repository, has(LIST));
    verify(mockIntegerListSupplier).get();
  }

  @Test
  public void shouldCompileIntoNextRepository() throws Exception {
    final Repository<Integer> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenGetFrom(listSource)
        .compileIntoRepositoryWithInitialValue(INITIAL_INT_VALUE)
        .onUpdatesPerLoop()
        .thenTransform(mockIntegerListToIntValueFunction)
        .compile();

    updatable.addToObservable(repository);

    verify(mockIntegerListToIntValueFunction, atLeastOnce()).apply(LIST);
    assertThat(repository, has(INT_VALUE));
  }

  @Test
  public void shouldSendTo() throws Exception {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .sendTo(mockIntegerListReceiver)
        .thenSkip()
        .compile();

    updatable.addToObservable(repository);

    verify(mockIntegerListReceiver).accept(INITIAL_VALUE);
    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldBindWith() throws Exception {
    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .bindWith(mockStringSupplier, mockIntegerListStringBinder)
        .thenSkip()
        .compile();

    updatable.addToObservable(repository);

    verify(mockIntegerListStringBinder).bind(INITIAL_VALUE, STRING_VALUE);
    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldGetSuccessfulValueFromThenAttemptGetFrom() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptGetFrom(mockResultStringSupplier).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasUpdated());
    assertThat(repository, has(STRING_VALUE));
  }

  @Test
  public void shouldNotUpdateForSkippedFailedValueFromThenAttemptGetFrom() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptGetFrom(mockFailedResultStringSupplier).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_STRING_VALUE));
  }

  @Test
  public void shouldGetSuccessfulValueFromThenAttemptTransform() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptTransform(mockResultStringFunction).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasUpdated());
    assertThat(repository, has(STRING_VALUE));
  }

  @Test
  public void shouldNotUpdateForSkippedFailedValueFromThenAttemptTransform() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptTransform(mockFailedResultStringFunction).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_STRING_VALUE));
  }

  @Test
  public void shouldGetSuccessfulValueFromThenAttemptMergeIn() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptMergeIn(mockStringSupplier, mockResultStringMerger).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasUpdated());
    assertThat(repository, has(STRING_VALUE));
  }

  @Test
  public void shouldNotUpdateForSkippedFailedValueFromThenAttemptMergeIn() {
    final Repository<String> repository = repositoryWithInitialValue(INITIAL_STRING_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptMergeIn(mockStringSupplier, mockFailedResultStringMerger).orSkip()
        .compile();

    updatable.addToObservable(repository);

    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_STRING_VALUE));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Repositories.class, hasPrivateConstructor());
  }

  private final static class ListMerger<A extends C, B extends C, C>
      implements Merger<List<A>, List<B>, List<C>> {
    @NonNull
    @Override
    public List<C> merge(@NonNull final List<A> firstList,
        @NonNull final List<B> secondList) {
      final List<C> result = new ArrayList<>(firstList.size() + secondList.size());
      result.addAll(firstList);
      result.addAll(secondList);
      return result;
    }
  }

  private static final class AddTwoForEachFunction
      implements Function<List<Integer>, List<Integer>> {
    @NonNull
    @Override
    public List<Integer> apply(@NonNull final List<Integer> integers) {
      final List<Integer> result = new ArrayList<>();
      for (final Integer integer : integers) {
        result.add(integer + 2);
      }
      return result;
    }
  }

  private static class SyncExecutor implements Executor {
    @Override
    public void execute(@NonNull final Runnable command) {
      command.run();
    }
  }
}
