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

import static com.google.android.agera.Conditions.falseCondition;
import static com.google.android.agera.Conditions.trueCondition;
import static com.google.android.agera.Predicates.all;
import static com.google.android.agera.Predicates.any;
import static com.google.android.agera.Predicates.conditionAsPredicate;
import static com.google.android.agera.Predicates.emptyString;
import static com.google.android.agera.Predicates.equalTo;
import static com.google.android.agera.Predicates.falsePredicate;
import static com.google.android.agera.Predicates.instanceOf;
import static com.google.android.agera.Predicates.not;
import static com.google.android.agera.Predicates.truePredicate;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.PredicateApply.appliesFor;
import static com.google.android.agera.test.matchers.PredicateApply.doesNotApplyFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public final class PredicatesTest {
  private static final String ITEM = "item";
  private static final String OTHER_ITEM = "otheritem";

  @Mock
  private Predicate<Object> mockPredicateFalse;
  @Mock
  private Predicate<Object> mockPredicateTrue;
  @Mock
  private Condition mockCondition;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockPredicateTrue.apply(Mockito.any())).thenReturn(true);
  }

  @Test
  public void shouldReturnTruePredicateForTrueConditionInConditionAsPredicate() {
    assertThat(conditionAsPredicate(trueCondition()), sameInstance(truePredicate()));
  }

  @Test
  public void shouldReturnFalsePredicateForFalseConditionInConditionAsPredicate() {
    assertThat(conditionAsPredicate(falseCondition()), sameInstance(falsePredicate()));
  }

  @Test
  public void shouldReturnFalseForConditionWithFalseInConditionAsPredicate() {
    assertThat(conditionAsPredicate(mockCondition), doesNotApplyFor(new Object()));
    verify(mockCondition).applies();
  }

  @Test
  public void shouldNegateTruePredicate() {
    assertThat(not(truePredicate()), sameInstance(falsePredicate()));
  }

  @Test
  public void shouldNegateFalsePredicate() {
    assertThat(not(falsePredicate()), sameInstance(truePredicate()));
  }

  @Test
  public void shouldNegateNonStaticFalseCondition() {
    assertThat(not(mockPredicateFalse), appliesFor(new Object()));
  }

  @Test
  public void shouldNegateNonStaticTrueCondition() {
    assertThat(not(mockPredicateTrue), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldNegatePredicateTwice() {
    assertThat(not(not(truePredicate())), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnOriginalPredicateIfNegatedTwice() {
    assertThat(not(not(mockPredicateFalse)), is(sameInstance(mockPredicateFalse)));
  }

  @Test
  public void shouldReturnTrueForEmptyStringInEmptyStringPredicate() {
    assertThat(emptyString(), appliesFor(""));
  }

  @Test
  public void shouldReturnFalseForStringInEmptyStringPredicate() {
    assertThat(emptyString(), doesNotApplyFor("A"));
  }

  @Test
  public void shouldReturnFalseForIncorrectInstanceInInstanceOfPredicate() {
    assertThat(instanceOf(Integer.class), doesNotApplyFor(1L));
  }

  @Test
  public void shouldReturnTrueForCorrectInstanceInInstanceOfPredicate() {
    assertThat(instanceOf(Long.class), appliesFor(1L));
  }

  @Test
  public void shouldReturnTrueForAllWithNoConditions() {
    assertThat(all(), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnOriginalPredicateIfAllOfOne() {
    assertThat(all(mockPredicateFalse), is(sameInstance(mockPredicateFalse)));
  }

  @Test
  public void shouldReturnTrueForAllWithTrueConditions() {
    assertThat(all(truePredicate(), truePredicate()), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAllWithOneFalseCondition() {
    assertThat(all(truePredicate(), falsePredicate()), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAnyWithNoConditions() {
    assertThat(any(), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldReturnOriginalPredicateIfAnyOfOne() {
    assertThat(any(mockPredicateFalse), is(sameInstance(mockPredicateFalse)));
  }

  @Test
  public void shouldReturnTrueForAnyWithOneTrueCondition() {
    assertThat(any(truePredicate(), falsePredicate()), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAnyWithNoTrueCondition() {
    assertThat(any(falsePredicate(), falsePredicate()), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForEqualToWhenNotEqual() {
    assertThat(equalTo(ITEM), doesNotApplyFor(OTHER_ITEM));
  }

  @Test
  public void shouldReturnTrueForEqualToWhenEqual() {
    assertThat(equalTo(ITEM), appliesFor(ITEM));
  }

  public void shouldReturnTrueForAnyWithNonStaticOneTrueCondition() {
    assertThat(any(mockPredicateTrue, mockPredicateFalse), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAnyWithNonStaticNoTrueCondition() {
    assertThat(any(mockPredicateFalse, mockPredicateFalse), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldReturnTrueForAllWithNonStaticTrueConditions() {
    assertThat(all(mockPredicateTrue, mockPredicateTrue), appliesFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAllWithNonStaticOneFalseCondition() {
    assertThat(all(mockPredicateTrue, mockPredicateFalse), doesNotApplyFor(new Object()));
  }

  @Test
  public void shouldReturnFalseForAllWithNonStaticOneStaticFalseCondition() {
    assertThat(all(mockPredicateTrue, falsePredicate()), sameInstance(falsePredicate()));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Predicates.class, hasPrivateConstructor());
  }
}
