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
import static com.google.android.agera.RepositoryConfig.CANCEL_FLOW;
import static com.google.android.agera.RepositoryConfig.CONTINUE_FLOW;
import static com.google.android.agera.RepositoryConfig.RESET_TO_INITIAL_VALUE;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.android.agera.test.SingleSlotDelayedExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class CompiledRepositoryUsageTest {
  private static final String STRING_A = "STRING_A";
  private static final int INTEGER_1 = 1;
  private static final double DOUBLE_3 = 3.0;

  @Mock
  private Supplier<String> mockStringSupplier;
  @Mock
  private Supplier<Result<Integer>> mockIntegerAttemptSupplier;
  @Mock
  private Supplier<Double> mockDoubleSupplier;
  @Mock
  private Function<String, Integer> mockStringToInteger;
  @Mock
  private Function<String, Result<Integer>> mockStringToIntegerAttempt;
  @Mock
  private Function<Integer, Double> mockIntegerToDouble;
  @Mock
  private Function<Integer, Result<Double>> mockIntegerToDoubleAttempt;
  @Mock
  private Function<Result<Integer>, String> mockRecoverIntegerToString;
  @Mock
  private Merger<Number, Number, String> mockNumbersToString;
  @Mock
  private Merger<Number, Number, Result<String>> mockNumbersToStringAttempt;
  @Mock
  private Merger<Object, Object, Boolean> mockChecker;

  private SingleSlotDelayedExecutor delayedExecutor1;
  private SingleSlotDelayedExecutor delayedExecutor2;
  private SingleSlotDelayedExecutor delayedExecutor3;

  private MutableRepository<String> stringVariable;
  private MutableRepository<Integer> integerVariable;
  private MutableRepository<Double> doubleVariable;

  @Before
  public void setUp() {
    initMocks(this);
    delayedExecutor1 = new SingleSlotDelayedExecutor();
    delayedExecutor2 = new SingleSlotDelayedExecutor();
    delayedExecutor3 = new SingleSlotDelayedExecutor();

    stringVariable = mutableRepository(STRING_A);
    integerVariable = mutableRepository(INTEGER_1);
    doubleVariable = mutableRepository(DOUBLE_3);
  }

  @Test
  public void shouldCompileComplexRepository() {
    assertThat(repositoryWithInitialValue(STRING_A)
        .observe(stringVariable, integerVariable, doubleVariable)
        .onUpdatesPer(5000)
        .getFrom(mockStringSupplier)
        .transform(mockStringToInteger)
        .mergeIn(mockDoubleSupplier, mockNumbersToString)
        .goTo(delayedExecutor1)
        .transform(mockStringToIntegerAttempt)
        .transform(mockRecoverIntegerToString)
        .goTo(delayedExecutor2)
        .attemptTransform(mockStringToIntegerAttempt).orSkip()
        .attemptGetFrom(mockIntegerAttemptSupplier).orSkip()
        .attemptMergeIn(mockDoubleSupplier, mockNumbersToStringAttempt).orSkip()
        .goTo(delayedExecutor3)
        .thenGetFrom(mockStringSupplier)
        .notifyIf(mockChecker)
        .onDeactivation(SEND_INTERRUPT | RESET_TO_INITIAL_VALUE)
        .onConcurrentUpdate(SEND_INTERRUPT | RESET_TO_INITIAL_VALUE)
        .compileIntoRepositoryWithInitialValue(INTEGER_1)
        .onUpdatesPerLoop()
        .attemptTransform(mockStringToIntegerAttempt).orSkip()
        .sendTo(integerVariable)
        .attemptMergeIn(integerVariable, mockNumbersToStringAttempt).orSkip()
        .thenTransform(mockStringToInteger)
        .notifyIf(mockChecker)
        .onDeactivation(CONTINUE_FLOW)
        .onConcurrentUpdate(CANCEL_FLOW)
        .compile(), not(nullValue()));
  }
}
