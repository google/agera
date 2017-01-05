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

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Predicates.falsePredicate;
import static com.google.android.agera.Predicates.truePredicate;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.CANCEL_FLOW;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.android.agera.test.SingleSlotDelayedExecutor;
import com.google.android.agera.test.mocks.MockUpdatable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RepositoryDisposerTest {
  private static final Object INITIAL_VALUE = new Object();
  private static final Object FIRST_VALUE = new Object();
  private static final Object SECOND_VALUE = new Object();
  private static final Object BREAK_VALUE = new Object();
  private static final Object FINAL_VALUE = new Object();

  private MockUpdatable updatable;
  private SingleSlotDelayedExecutor executor;
  @Mock
  private Predicate<Object> mockPredicate;
  @Mock
  private Receiver<Object> mockDisposer;

  @Before
  public void setUp() {
    initMocks(this);
    updatable = mockUpdatable();
    executor = new SingleSlotDelayedExecutor();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldNotDiscardInitialValue() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .check(falsePredicate()).orSkip()
        .thenGetFrom(staticSupplier(FINAL_VALUE))
        .sendDiscardedValuesTo(mockDisposer)
        .compile();

    updatable.addToObservable(repository);

    verify(mockDisposer, never()).accept(any());
  }

  @Test
  public void shouldNotDiscardPublishedValue() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .check(mockPredicate).orSkip()
        .thenTransform(staticFunction(FINAL_VALUE))
        .sendDiscardedValuesTo(mockDisposer)
        .compile();
    when(mockPredicate.apply(INITIAL_VALUE)).thenReturn(true);
    when(mockPredicate.apply(FINAL_VALUE)).thenReturn(false);

    updatable.addToObservable(repository);
    updatable.removeFromObservables();
    updatable.addToObservable(repository);

    verify(mockPredicate).apply(INITIAL_VALUE);
    verify(mockPredicate).apply(FINAL_VALUE);
    verify(mockDisposer, never()).accept(any());
  }

  @Test
  public void shouldDiscardFirstValueDueToSkipClause() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(staticSupplier(FIRST_VALUE))
        .check(falsePredicate()).orSkip()
        .thenGetFrom(staticSupplier(FINAL_VALUE))
        .sendDiscardedValuesTo(mockDisposer)
        .compile();

    updatable.addToObservable(repository);

    verify(mockDisposer).accept(FIRST_VALUE);
  }

  @Test
  public void shouldDiscardFirstValueDueToEndClause() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(staticSupplier(FIRST_VALUE))
        .check(falsePredicate()).orEnd(staticFunction(BREAK_VALUE))
        .thenGetFrom(staticSupplier(FINAL_VALUE))
        .sendDiscardedValuesTo(mockDisposer)
        .compile();

    updatable.addToObservable(repository);

    verify(mockDisposer).accept(FIRST_VALUE);
  }

  @Test
  public void shouldDiscardSecondValueDueToSkipDirective() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(staticSupplier(FIRST_VALUE))
        .check(truePredicate()).orEnd(staticFunction(BREAK_VALUE))
        .transform(staticFunction(SECOND_VALUE))
        .thenSkip()
        .sendDiscardedValuesTo(mockDisposer)
        .compile();

    updatable.addToObservable(repository);

    verify(mockDisposer).accept(SECOND_VALUE);
  }

  @Test
  public void shouldDiscardFirstValueDueToDeactivation() {
    Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(staticSupplier(FIRST_VALUE))
        .goTo(executor)
        .thenTransform(staticFunction(FINAL_VALUE))
        .onDeactivation(CANCEL_FLOW)
        .sendDiscardedValuesTo(mockDisposer)
        .compile();

    updatable.addToObservable(repository);
    updatable.removeFromObservables();
    executor.resumeOrThrow();

    verify(mockDisposer).accept(FIRST_VALUE);
  }
}
