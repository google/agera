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

import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.test.matchers.SupplierGives.has;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasNotUpdated;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.android.agera.test.mocks.MockUpdatable;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RepositoryTerminationTest {
  private static final List<Integer> INITIAL_LIST = asList(1, 2, 3);
  private static final List<Integer> LIST = asList(4, 5);
  private static final List<Integer> OTHER_LIST = asList(6, 7);

  private static final int INITIAL_VALUE = 8;
  private static final int VALUE = 42;
  private static final Result<Integer> SUCCESS_WITH_VALUE = success(VALUE);
  private static final Result<Integer> FAILURE = failure();

  private MutableRepository<List<Integer>> listSource;
  private MutableRepository<List<Integer>> otherListSource;
  private MockUpdatable updatable;
  @Mock
  private Predicate<List<Integer>> mockPredicate;
  @Mock
  private Receiver<Object> mockReceiver;
  @Mock
  private Function<List<Integer>, List<Integer>> mockFunction;
  @Mock
  private Function<List<Integer>, List<Integer>> mockOtherFunction;
  @Mock
  private Supplier<Result<Integer>> mockAttemptSupplier;

  @Before
  public void setUp() {
    initMocks(this);
    listSource = mutableRepository(LIST);
    otherListSource = mutableRepository(OTHER_LIST);
    updatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldEndAfterFailedCheck() {
    when(mockPredicate.apply(anyListOf(Integer.class))).thenReturn(true);
    when(mockPredicate.apply(INITIAL_LIST)).thenReturn(false);
    when(mockFunction.apply(INITIAL_LIST)).thenReturn(LIST);

    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_LIST)
        .observe()
        .onUpdatesPerLoop()
        .check(mockPredicate).orEnd(mockFunction)
        .thenGetFrom(otherListSource)
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST));
  }

  @Test
  public void shouldSkipAfterFailedCheck() {
    when(mockPredicate.apply(anyListOf(Integer.class))).thenReturn(true);
    when(mockPredicate.apply(INITIAL_LIST)).thenReturn(false);

    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_LIST)
        .observe()
        .onUpdatesPerLoop()
        .check(mockPredicate).orSkip()
        .thenGetFrom(otherListSource)
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(INITIAL_LIST));
    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldNotSkipAfterPassedCheck() {
    when(mockPredicate.apply(anyListOf(Integer.class))).thenReturn(true);

    final Repository<List<Integer>> repository = repositoryWithInitialValue(INITIAL_LIST)
        .observe()
        .onUpdatesPerLoop()
        .check(mockPredicate).orSkip()
        .thenGetFrom(listSource)
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(LIST));
    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldSkipWhenAttemptGetFromFails() {
    when(mockAttemptSupplier.get()).thenReturn(FAILURE);

    final Repository<Integer> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .attemptGetFrom(mockAttemptSupplier).orSkip()
        .thenTransform(Functions.<Integer>identityFunction())
        .compile();

    updatable.addToObservable(repository);

    verify(mockAttemptSupplier).get();
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldNotSkipWhenAttemptGetFromSucceeds() {
    when(mockAttemptSupplier.get()).thenReturn(SUCCESS_WITH_VALUE);

    final Repository<Integer> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .attemptGetFrom(mockAttemptSupplier).orSkip()
        .thenTransform(Functions.<Integer>identityFunction())
        .compile();

    updatable.addToObservable(repository);

    assertThat(repository, has(VALUE));
  }
}
