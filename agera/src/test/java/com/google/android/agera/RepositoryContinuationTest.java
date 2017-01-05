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

import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.test.matchers.SupplierGives.has;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

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
public final class RepositoryContinuationTest {
  private static final int INITIAL_VALUE = 8;
  private static final Result<Integer> SUPPLIER_SUCCESSFUL_RESULT = success(100);
  private static final Result<Integer> MERGER_SUCCESSFUL_RESULT = success(200);
  private static final Result<Integer> FUNCTION_SUCCESSFUL_RESULT = success(300);
  private static final Result<Integer> SUPPLIER_FAILED_RESULT = failure(new Throwable("-100"));
  private static final Result<Integer> MERGER_FAILED_RESULT = failure(new Throwable("-200"));
  private static final Result<Integer> FUNCTION_FAILED_RESULT = failure(new Throwable("-300"));
  private static final Supplier<Integer> SECOND_SUPPLIER = staticSupplier(400);
  private static final int RECOVERY_VALUE = 42;

  private Repository<Integer> repository;
  private MockUpdatable updatable;
  @Mock
  private Supplier<Result<Integer>> mockAttemptSupplier;
  @Mock
  private Merger<Throwable, Integer, Result<Integer>> mockAttemptMerger;
  @Mock
  private Function<Throwable, Result<Integer>> mockAttemptFunction;
  @Mock
  private Function<Throwable, Integer> mockRecoveryFunction;

  @Before
  public void setUp() {
    initMocks(this);
    updatable = mockUpdatable();
    repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe()
        .onUpdatesPerLoop()
        .thenAttemptGetFrom(mockAttemptSupplier).orContinue()
        .thenAttemptMergeIn(SECOND_SUPPLIER, mockAttemptMerger).orContinue()
        .thenAttemptTransform(mockAttemptFunction).orContinue()
        .thenTransform(mockRecoveryFunction)
        .compile();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldProduceSupplierResultIfSupplierSucceeds() {
    when(mockAttemptSupplier.get()).thenReturn(SUPPLIER_SUCCESSFUL_RESULT);

    updatable.addToObservable(repository);

    assertThat(repository, has(SUPPLIER_SUCCESSFUL_RESULT.get()));
    verifyNoMoreInteractions(mockAttemptMerger, mockAttemptFunction, mockRecoveryFunction);
  }

  @Test
  public void shouldProduceMergerResultIfMergerSucceeds() {
    when(mockAttemptSupplier.get()).thenReturn(SUPPLIER_FAILED_RESULT);
    when(mockAttemptMerger.merge(SUPPLIER_FAILED_RESULT.getFailure(), SECOND_SUPPLIER.get()))
        .thenReturn(MERGER_SUCCESSFUL_RESULT);

    updatable.addToObservable(repository);

    assertThat(repository, has(MERGER_SUCCESSFUL_RESULT.get()));
    verifyNoMoreInteractions(mockAttemptFunction, mockRecoveryFunction);
  }

  @Test
  public void shouldProduceFunctionResultIfFunctionSucceeds() {
    when(mockAttemptSupplier.get()).thenReturn(SUPPLIER_FAILED_RESULT);
    when(mockAttemptMerger.merge(SUPPLIER_FAILED_RESULT.getFailure(), SECOND_SUPPLIER.get()))
        .thenReturn(MERGER_FAILED_RESULT);
    when(mockAttemptFunction.apply(MERGER_FAILED_RESULT.getFailure()))
        .thenReturn(FUNCTION_SUCCESSFUL_RESULT);

    updatable.addToObservable(repository);

    assertThat(repository, has(FUNCTION_SUCCESSFUL_RESULT.get()));
    verifyNoMoreInteractions(mockRecoveryFunction);
  }

  @Test
  public void shouldProduceRecoveryResultIfAllAttemptsFail() {
    when(mockAttemptSupplier.get()).thenReturn(SUPPLIER_FAILED_RESULT);
    when(mockAttemptMerger.merge(SUPPLIER_FAILED_RESULT.getFailure(), SECOND_SUPPLIER.get()))
        .thenReturn(MERGER_FAILED_RESULT);
    when(mockAttemptFunction.apply(MERGER_FAILED_RESULT.getFailure()))
        .thenReturn(FUNCTION_FAILED_RESULT);
    when(mockRecoveryFunction.apply(FUNCTION_FAILED_RESULT.getFailure()))
        .thenReturn(RECOVERY_VALUE);

    updatable.addToObservable(repository);

    assertThat(repository, has(RECOVERY_VALUE));
  }
}
