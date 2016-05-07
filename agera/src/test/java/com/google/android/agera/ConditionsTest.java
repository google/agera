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

import static com.google.android.agera.Conditions.all;
import static com.google.android.agera.Conditions.any;
import static com.google.android.agera.Conditions.falseCondition;
import static com.google.android.agera.Conditions.not;
import static com.google.android.agera.Conditions.predicateAsCondition;
import static com.google.android.agera.Conditions.staticCondition;
import static com.google.android.agera.Conditions.trueCondition;
import static com.google.android.agera.Predicates.falsePredicate;
import static com.google.android.agera.Predicates.truePredicate;
import static com.google.android.agera.test.matchers.ConditionApplies.applies;
import static com.google.android.agera.test.matchers.ConditionApplies.doesNotApply;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public final class ConditionsTest {
  private static final int VALUE = 1;

  @Mock
  private Condition mockConditionFalse;
  @Mock
  private Condition mockConditionTrue;
  @Mock
  private Predicate<Integer> mockPredicateFalse;
  @Mock
  private Predicate<Integer> mockPredicateTrue;
  @Mock
  private Supplier<Integer> mockValueSupplier;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockValueSupplier.get()).thenReturn(VALUE);
    when(mockConditionTrue.applies()).thenReturn(true);
    when(mockPredicateTrue.apply(anyInt())).thenReturn(true);
  }

  @Test
  public void shouldReturnTrueForTrueCondition() {
    assertThat(trueCondition(), applies());
  }

  @Test
  public void shouldReturnFalseForFalseCondition() {
    assertThat(falseCondition(), doesNotApply());
  }

  @Test
  public void shouldReturnTrueForTrueStaticCondition() {
    assertThat(staticCondition(true), sameInstance(trueCondition()));
  }

  @Test
  public void shouldReturnFalseForFalseStaticCondition() {
    assertThat(staticCondition(false), sameInstance(falseCondition()));
  }

  @Test
  public void shouldNegateTrueCondition() {
    assertThat(not(trueCondition()), sameInstance(falseCondition()));
  }

  @Test
  public void shouldNegateFalseCondition() {
    assertThat(not(falseCondition()), sameInstance(trueCondition()));
  }

  @Test
  public void shouldNegateNonStaticFalseCondition() {
    assertThat(not(mockConditionFalse), applies());
  }

  @Test
  public void shouldNegateNonStaticTrueCondition() {
    assertThat(not(mockConditionTrue), doesNotApply());
  }

  @Test
  public void shouldReturnOriginalConditionIfNegatedTwice() {
    assertThat(not(not(mockConditionFalse)), is(sameInstance(mockConditionFalse)));
  }

  @Test
  public void shouldReturnTrueForAllWithNoConditions() {
    assertThat(all(), sameInstance(trueCondition()));
  }

  @Test
  public void shouldReturnOriginalConditionIfAllOfOne() {
    assertThat(all(mockConditionFalse), is(sameInstance(mockConditionFalse)));
  }

  @Test
  public void shouldReturnTrueForAllWithTrueConditions() {
    assertThat(all(trueCondition(), trueCondition()), sameInstance(trueCondition()));
  }

  @Test
  public void shouldReturnFalseForAllWithOneFalseCondition() {
    assertThat(all(trueCondition(), falseCondition()), sameInstance(falseCondition()));
  }

  @Test
  public void shouldReturnTrueForAllWithNonStaticTrueConditions() {
    assertThat(all(mockConditionTrue, mockConditionTrue), applies());
  }

  @Test
  public void shouldReturnFalseForAllWithNonStaticOneFalseCondition() {
    assertThat(all(mockConditionTrue, mockConditionFalse), doesNotApply());
  }

  @Test
  public void shouldReturnFalseForAllWithNonStaticOneStaticFalseCondition() {
    assertThat(all(mockConditionTrue, falseCondition()), sameInstance(falseCondition()));
  }

  @Test
  public void shouldReturnFalseForAnyWithNoConditions() {
    assertThat(any(), sameInstance(falseCondition()));
  }

  @Test
  public void shouldReturnOriginalConditionIfAnyOfOne() {
    assertThat(any(mockConditionFalse), is(sameInstance(mockConditionFalse)));
  }

  @Test
  public void shouldReturnTrueForAnyWithOneTrueCondition() {
    assertThat(any(trueCondition(), falseCondition()), sameInstance(trueCondition()));
  }

  @Test
  public void shouldReturnFalseForAnyWithNoTrueCondition() {
    assertThat(any(falseCondition(), falseCondition()), sameInstance(falseCondition()));
  }

  @Test
  public void shouldReturnTrueForAnyWithNonStaticOneTrueCondition() {
    assertThat(any(mockConditionTrue, mockConditionFalse), applies());
  }

  @Test
  public void shouldReturnFalseForAnyWithNonStaticNoTrueCondition() {
    assertThat(any(mockConditionFalse, mockConditionFalse), doesNotApply());
  }

  @Test
  public void shouldReturnTrueForTruePredicateAsCondition() {
    assertThat(predicateAsCondition(truePredicate(), mockValueSupplier),
        sameInstance(trueCondition()));
  }

  @Test
  public void shouldReturnFalseForFalsePredicateAsCondition() {
    assertThat(predicateAsCondition(falsePredicate(), mockValueSupplier),
        sameInstance(falseCondition()));
  }

  @Test
  public void shouldPassSupplierObjectToPredicateForTruePredicateAsCondition() {
    assertThat(predicateAsCondition(mockPredicateTrue, mockValueSupplier), applies());
  }

  @Test
  public void shouldPassSupplierObjectToPredicateForFalsePredicateAsCondition() {
    assertThat(predicateAsCondition(mockPredicateFalse, mockValueSupplier), doesNotApply());
    verify(mockPredicateFalse).apply(VALUE);
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Conditions.class, hasPrivateConstructor());
  }
}