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

import static com.google.android.agera.Reactions.reactionTo;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RexConfig.CANCEL_FLOW;
import static com.google.android.agera.RexConfig.CONTINUE_FLOW;
import static com.google.android.agera.RexConfig.RESET_TO_INITIAL_VALUE;
import static com.google.android.agera.RexConfig.SEND_INTERRUPT;
import static com.google.android.agera.test.MockAsync.mockAsync;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.android.agera.test.MockAsync;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RexUsageTest {
  private static final String STRING_A = "STRING_A";
  private static final String STRING_B = "STRING_B";
  private static final int INTEGER_1 = 1;
  private static final int INTEGER_2 = 2;
  private static final double DOUBLE_3 = 3.0;
  private static final double DOUBLE_4 = 4.0;

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

  private MockAsync<String, String> async1;
  private MockAsync<String, String> async2;
  private MockAsync<String, String> async3;

  private MutableRepository<String> stringVariable;
  private MutableRepository<Integer> integerVariable;
  private MutableRepository<Double> doubleVariable;

  @Before
  public void setUp() {
    initMocks(this);
    async1 = mockAsync();
    async2 = mockAsync();
    async3 = mockAsync();

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
        .async(async1)
        .transform(mockStringToIntegerAttempt)
        .transform(mockRecoverIntegerToString)
        .async(async2)
        .attemptTransform(mockStringToIntegerAttempt).orSkip()
        .attemptGetFrom(mockIntegerAttemptSupplier).orSkip()
        .attemptMergeIn(mockDoubleSupplier, mockNumbersToStringAttempt).orSkip()
        .async(async3)
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

  @Test
  public void shouldCompileComplexReaction() {
    assertThat(reactionTo(String.class)
        .transform(mockStringToInteger)
        .mergeIn(mockDoubleSupplier, mockNumbersToString)
        .async(async1)
        .transform(mockStringToIntegerAttempt)
        .transform(mockRecoverIntegerToString)
        .async(async2)
        .attemptTransform(mockStringToIntegerAttempt).orSkip()
        .attemptGetFrom(mockIntegerAttemptSupplier).orSkip()
        .attemptMergeIn(mockDoubleSupplier, mockNumbersToStringAttempt).orSkip()
        .async(async3)
        .thenEnd()
        .onDeactivation(SEND_INTERRUPT | RESET_TO_INITIAL_VALUE)
        .onConcurrentUpdate(SEND_INTERRUPT | RESET_TO_INITIAL_VALUE)
        .compile(), not(nullValue()));
  }
}
