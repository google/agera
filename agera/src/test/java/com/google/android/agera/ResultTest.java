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

import static com.google.android.agera.Result.absent;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public final class ResultTest {
  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Throwable THROWABLE = new Throwable();
  private static final int VALUE = 42;
  private static final String STRING_VALUE = "stringvalue";
  private static final int OTHER_VALUE = 1;
  private static final float FLOAT_VALUE = 2;
  private static final Result<Integer> SUCCESS_WITH_VALUE = success(VALUE);
  private static final Result<Integer> SUCCESS_WITH_OTHER_VALUE = success(OTHER_VALUE);
  private static final Result<Float> SUCCESS_WITH_FLOAT_VALUE = success(FLOAT_VALUE);
  private static final Result<Integer> FAILURE_WITH_THROWABLE = failure(THROWABLE);
  private static final Result<Integer> FAILURE = failure();
  private static final Result<Integer> PRESENT_WITH_VALUE = present(VALUE);
  private static final Result<Integer> ABSENT = absent();

  @Mock
  private Function<Integer, Result<Integer>> mockSucceededValueFunction;
  @Mock
  private Function<Integer, Result<Integer>> mockFailedFunction;
  @Mock
  private Function<Integer, Integer> mockValueFunction;
  @Mock
  private Function<Throwable, Integer> mockRecoverValueFunction;
  @Mock
  private Function<Throwable, Result<Integer>> mockAttemptRecoverValueFunction;
  @Mock
  private Binder<Integer, String> mockBinder;
  @Mock
  private Merger<Integer, String, Float> mockMerger;
  @Mock
  private Merger<Integer, String, Result<Float>> mockAttemptMerger;
  @Mock
  private Supplier<String> mockSupplier;
  @Mock
  private Receiver<Integer> mockReceiver;
  @Mock
  private Receiver<Throwable> mockThrowableReceiver;
  @Mock
  private Binder<Throwable, String> mockThrowableStringBinder;
  @Mock
  private Supplier<Integer> mockOtherValueSupplier;
  @Mock
  private Supplier<Result<Integer>> mockOtherValueSuccessfulAttemptSupplier;
  @Mock
  private Supplier<Result<Integer>> mockOtherValueFailingAttemptSupplier;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockSupplier.get()).thenReturn(STRING_VALUE);
    when(mockMerger.merge(anyInt(), anyString())).thenReturn(FLOAT_VALUE);
    when(mockAttemptMerger.merge(anyInt(), anyString())).thenReturn(SUCCESS_WITH_FLOAT_VALUE);
    when(mockRecoverValueFunction.apply(any(Throwable.class))).thenReturn(VALUE);
    when(mockAttemptRecoverValueFunction.apply(any(Throwable.class)))
        .thenReturn(SUCCESS_WITH_VALUE);
    when(mockSucceededValueFunction.apply(anyInt())).thenReturn(SUCCESS_WITH_VALUE);
    when(mockValueFunction.apply(anyInt())).thenReturn(VALUE);
    when(mockFailedFunction.apply(anyInt())).thenReturn(FAILURE_WITH_THROWABLE);
    when(mockOtherValueSupplier.get()).thenReturn(OTHER_VALUE);
    when(mockOtherValueSuccessfulAttemptSupplier.get()).thenReturn(SUCCESS_WITH_OTHER_VALUE);
    when(mockOtherValueFailingAttemptSupplier.get()).thenReturn(FAILURE);
  }

  @Test(expected = FailedResultException.class)
  public void shouldThrowExceptionForGetOfFailure() {
    FAILURE_WITH_THROWABLE.get();
  }

  @Test(expected = FailedResultException.class)
  public void shouldThrowExceptionForGetOfAbsent() {
    ABSENT.get();
  }

  @Test
  public void shouldReturnValueForGetOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.get(), equalTo(VALUE));
  }

  @Test
  public void shouldReturnFalseForSucceededOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.succeeded(), equalTo(false));
  }

  @Test
  public void shouldReturnTrueForSucceeded() {
    assertThat(SUCCESS_WITH_VALUE.succeeded(), equalTo(true));
  }

  @Test
  public void shouldReturnTrueForFailedOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.failed(), equalTo(true));
  }

  @Test
  public void shouldReturnFalseForFailedOfSucceeded() {
    assertThat(SUCCESS_WITH_VALUE.failed(), equalTo(false));
  }

  @Test
  public void shouldReturnFalseForIsPresentOfAbsent() {
    assertThat(ABSENT.isPresent(), equalTo(false));
  }

  @Test
  public void shouldReturnTrueForIsPresentWithValue() {
    assertThat(PRESENT_WITH_VALUE.isPresent(), equalTo(true));
  }

  @Test
  public void shouldReturnTrueForIsAbsentOfAbsent() {
    assertThat(ABSENT.isAbsent(), equalTo(true));
  }

  @Test
  public void shouldReturnFalseForIsAbsentWithValue() {
    assertThat(PRESENT_WITH_VALUE.isAbsent(), equalTo(false));
  }

  @Test
  public void shouldReturnFalseForIsAbsentOfNonAbsentFailure() {
    assertThat(FAILURE.isAbsent(), equalTo(false));
  }

  @Test
  public void shouldReturnAbsentForFailureWithAbsentFailure() {
    assertThat(Result.<Integer>failure(ABSENT.getFailure()), sameInstance(ABSENT));
  }

  @Test
  public void shouldReturnAbsentForOfNullableWithNull() {
    assertThat(Result.<Integer>absentIfNull(null), sameInstance(ABSENT));
  }

  @Test
  public void shouldReturnPresentWithValueForOfNullableWithValue() {
    assertThat(absentIfNull(VALUE), equalTo(PRESENT_WITH_VALUE));
  }

  @Test
  public void shouldReturnExceptionForGetFailureOfFailure() {
    assertThat(FAILURE.getFailure(), notNullValue());
  }

  @Test
  public void shouldReturnExceptionForGetFailureOfFailureOfExplicitException() {
    assertThat(failure(THROWABLE).getFailure(), sameInstance(THROWABLE));
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionForGetFailureOfSuccess() {
    SUCCESS_WITH_VALUE.getFailure();
  }

  @Test
  public void shouldReturnValueForOrElseOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.orElse(OTHER_VALUE), equalTo(VALUE));
  }

  @Test
  public void shouldReturnValueForOrGetFromOfSuccessWithValue() {
    assertThat(SUCCESS_WITH_VALUE.orGetFrom(mockOtherValueSupplier), equalTo(VALUE));
    verifyZeroInteractions(mockOtherValueSupplier);
  }

  @Test
  public void shouldReturnSameInstanceForOrAttemptGetFromOfSuccessWithValue() {
    assertThat(SUCCESS_WITH_VALUE.orAttemptGetFrom(mockOtherValueSuccessfulAttemptSupplier),
        sameInstance(SUCCESS_WITH_VALUE));
    verifyZeroInteractions(mockOtherValueSuccessfulAttemptSupplier);
  }

  @Test
  public void shouldReturnElseValueForOrElseOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.orElse(OTHER_VALUE), equalTo(OTHER_VALUE));
  }

  @Test
  public void shouldReturnOtherValueForOrGetFromOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.orGetFrom(mockOtherValueSupplier), equalTo(OTHER_VALUE));
  }

  @Test
  public void shouldReturnSuccessWithOtherValueForOrAttemptGetFromOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.orAttemptGetFrom(mockOtherValueSuccessfulAttemptSupplier),
        sameInstance(SUCCESS_WITH_OTHER_VALUE));
  }

  @Test
  public void shouldReturnOtherResultForOrAttemptGetFromOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.orAttemptGetFrom(mockOtherValueFailingAttemptSupplier),
        sameInstance(FAILURE));
  }

  @Test
  public void shouldApplySendIfSucceeded() {
    SUCCESS_WITH_VALUE.ifSucceededSendTo(mockReceiver);

    verify(mockReceiver).accept(VALUE);
  }

  @Test
  public void shouldNotApplySendIfFailed() {
    FAILURE_WITH_THROWABLE.ifSucceededSendTo(mockReceiver);

    verifyZeroInteractions(mockReceiver);
  }

  @Test
  public void shouldApplyBindIfSucceeded() {
    SUCCESS_WITH_VALUE.ifSucceededBind(STRING_VALUE, mockBinder);

    verify(mockBinder).bind(VALUE, STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindIfFailed() {
    FAILURE_WITH_THROWABLE.ifSucceededBind(STRING_VALUE, mockBinder);

    verifyZeroInteractions(mockSupplier);
    verifyZeroInteractions(mockBinder);
  }

  @Test
  public void shouldApplyBindFromIfSucceeded() {
    SUCCESS_WITH_VALUE.ifSucceededBindFrom(mockSupplier, mockBinder);

    verify(mockBinder).bind(VALUE, STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindFromIfFailed() {
    FAILURE_WITH_THROWABLE.ifSucceededBindFrom(mockSupplier, mockBinder);

    verifyZeroInteractions(mockSupplier);
    verifyZeroInteractions(mockBinder);
  }

  @Test
  public void shouldApplySendIfFailed() {
    failure(THROWABLE).ifFailedSendTo(mockThrowableReceiver);

    verify(mockThrowableReceiver).accept(THROWABLE);
  }

  @Test
  public void shouldApplySendIfFailedExceptAbsent() {
    failure(THROWABLE).ifNonAbsentFailureSendTo(mockThrowableReceiver);

    verify(mockThrowableReceiver).accept(THROWABLE);
  }

  @Test
  public void shouldNotApplySendIfSucceededIfAbsent() {
    SUCCESS_WITH_VALUE.ifNonAbsentFailureSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldNotApplySendIfFailedIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldNotApplySendIfFailedExceptAbsentIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldNotApplySendIfFailedExceptAbsentIfAbsent() {
    absent().ifNonAbsentFailureSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldApplySendIfFailedAbsent() {
    absent().ifAbsentFailureSendTo(mockThrowableReceiver);

    verify(mockThrowableReceiver).accept(absent().getFailure());
  }

  @Test
  public void shouldNotApplySendIfFailedAbsentIfAbsent() {
    failure(THROWABLE).ifAbsentFailureSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldNotApplySendIfFailedAbsentIfSucceeded() {
    failure(THROWABLE).ifAbsentFailureSendTo(mockThrowableReceiver);

    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldApplyBindIfFailed() {
    failure(THROWABLE).ifFailedBind(STRING_VALUE, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(THROWABLE, STRING_VALUE);
  }

  @Test
  public void shouldApplyBindIfFailedExceptAbsent() {
    failure(THROWABLE).ifNonAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(THROWABLE, STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindIfSucceededIfAbsent() {
    SUCCESS_WITH_VALUE.ifNonAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldNotApplyBindIfFailedIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldNotApplyBindIfFailedExceptAbsentIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldNotApplyBindIfFailedExceptAbsentIfAbsent() {
    absent().ifNonAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldApplyBindIfFailedAbsent() {
    absent().ifAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(absent().getFailure(), STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindIfFailedAbsentIfAbsent() {
    failure(THROWABLE).ifAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldNotApplyBindIfFailedAbsentIfSucceeded() {
    failure(THROWABLE).ifAbsentFailureBind(STRING_VALUE, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
  }

  @Test
  public void shouldApplyBindFromIfFailed() {
    failure(THROWABLE).ifFailedBindFrom(mockSupplier, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(THROWABLE, STRING_VALUE);
  }

  @Test
  public void shouldApplyBindFromIfFailedExceptAbsent() {
    failure(THROWABLE).ifNonAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(THROWABLE, STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindFromIfSucceededIfAbsent() {
    SUCCESS_WITH_VALUE.ifNonAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldNotApplyBindFromIfFailedIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldNotApplyBindFromIfFailedExceptAbsentIfSucceeded() {
    SUCCESS_WITH_VALUE.ifFailedBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldNotApplyBindFromIfFailedExceptAbsentIfAbsent() {
    absent().ifNonAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldApplyBindFromIfFailedAbsent() {
    absent().ifAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verify(mockThrowableStringBinder).bind(absent().getFailure(), STRING_VALUE);
  }

  @Test
  public void shouldNotApplyBindFromIfFailedAbsentIfAbsent() {
    failure(THROWABLE).ifAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldNotApplyBindFromIfFailedAbsentIfSucceeded() {
    failure(THROWABLE).ifAbsentFailureBindFrom(mockSupplier, mockThrowableStringBinder);

    verifyZeroInteractions(mockThrowableStringBinder);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldAllowForChainedCallsToSendIfFailed() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededSendTo(mockReceiver),
        sameInstance(SUCCESS_WITH_VALUE));
  }

  @Test
  public void shouldAllowForChainedCallsToSendIfSucceeded() {
    assertThat(SUCCESS_WITH_VALUE.ifFailedSendTo(mockThrowableReceiver),
        sameInstance(SUCCESS_WITH_VALUE));
    verifyZeroInteractions(mockThrowableReceiver);
  }

  @Test
  public void shouldAllowForChainedCallsToBind() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededBind(STRING_VALUE, mockBinder),
        sameInstance(SUCCESS_WITH_VALUE));
  }

  @Test
  public void shouldAllowForChainedCallsToBindFrom() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededBindFrom(mockSupplier, mockBinder),
        sameInstance(SUCCESS_WITH_VALUE));
  }

  @Test
  public void shouldReturnFailureForMapOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededMap(mockValueFunction),
        sameInstance(FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockValueFunction);
  }

  @Test
  public void shouldApplyFunctionForMapOfSuccess() {
    assertThat(SUCCESS_WITH_OTHER_VALUE.ifSucceededMap(mockValueFunction),
        equalTo(SUCCESS_WITH_VALUE));
    verify(mockValueFunction).apply(OTHER_VALUE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnFailureForMergeOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededMerge(STRING_VALUE, mockMerger),
        sameInstance((Result) FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockValueFunction);
  }

  @Test
  public void shouldApplyFunctionForMergeOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededMerge(STRING_VALUE, mockMerger),
        equalTo(SUCCESS_WITH_FLOAT_VALUE));
    verify(mockMerger).merge(VALUE, STRING_VALUE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnFailureForMergeFromOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededMergeFrom(mockSupplier, mockMerger),
        sameInstance((Result) FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockValueFunction);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldApplyFunctionForMergeFromOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededMergeFrom(mockSupplier, mockMerger),
        equalTo(SUCCESS_WITH_FLOAT_VALUE));
    verify(mockMerger).merge(VALUE, STRING_VALUE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnFailureForAttemptMergeOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededAttemptMerge(STRING_VALUE, mockAttemptMerger),
        sameInstance((Result) FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockValueFunction);
  }

  @Test
  public void shouldApplyFunctionForAttemptMergeOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededAttemptMerge(STRING_VALUE, mockAttemptMerger),
        equalTo(SUCCESS_WITH_FLOAT_VALUE));
    verify(mockAttemptMerger).merge(VALUE, STRING_VALUE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldReturnFailureForAttemptMergeFromOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededAttemptMergeFrom(mockSupplier, mockAttemptMerger),
        sameInstance((Result) FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockValueFunction);
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldApplyFunctionForAttemptMergeFromOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededAttemptMergeFrom(mockSupplier, mockAttemptMerger),
        equalTo(SUCCESS_WITH_FLOAT_VALUE));
    verify(mockAttemptMerger).merge(VALUE, STRING_VALUE);
  }

  @Test
  public void shouldReturnFailureForFlatMapOfFailed() {
    assertThat(FAILURE_WITH_THROWABLE.ifSucceededAttemptMap(mockSucceededValueFunction),
        sameInstance(FAILURE_WITH_THROWABLE));
    verifyZeroInteractions(mockSucceededValueFunction);
  }

  @Test
  public void shouldReturnValueOfSucceededFromAppliedFunctionForFlatMapOfSucceeded() {
    assertThat(SUCCESS_WITH_OTHER_VALUE.ifSucceededAttemptMap(mockSucceededValueFunction),
        equalTo(SUCCESS_WITH_VALUE));
    verify(mockSucceededValueFunction).apply(OTHER_VALUE);
  }

  @Test
  public void shouldReturnFailureIfAttemptFromAppliedFunctionForFlatMapOfValueReturnsFailure() {
    assertThat(SUCCESS_WITH_VALUE.ifSucceededAttemptMap(mockFailedFunction),
        equalTo(FAILURE_WITH_THROWABLE));
    verify(mockFailedFunction).apply(VALUE);
  }

  @Test
  public void shouldReturnRecoverSuccessForAttemptRecoverOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.attemptRecover(mockAttemptRecoverValueFunction),
        equalTo(SUCCESS_WITH_VALUE));
    verify(mockAttemptRecoverValueFunction).apply(THROWABLE);
  }

  @Test
  public void shouldReturnRecoverValueForRecoverOfFailure() {
    assertThat(FAILURE_WITH_THROWABLE.recover(mockRecoverValueFunction), equalTo(VALUE));
    verify(mockRecoverValueFunction).apply(THROWABLE);
  }

  @Test
  public void shouldReturnSuccessForAttemptRecoverOfSuccess() {
    assertThat(SUCCESS_WITH_OTHER_VALUE.attemptRecover(mockAttemptRecoverValueFunction),
        equalTo(SUCCESS_WITH_OTHER_VALUE));
    verifyZeroInteractions(mockRecoverValueFunction);
  }

  @Test
  public void shouldReturnValueForRecoverOfSuccess() {
    assertThat(SUCCESS_WITH_VALUE.recover(mockRecoverValueFunction), equalTo(VALUE));
    verifyZeroInteractions(mockRecoverValueFunction);
  }

  @Test
  public void shouldNotBeEqualForDifferentValues() {
    assertThat(SUCCESS_WITH_VALUE, not(equalTo(SUCCESS_WITH_OTHER_VALUE)));
  }

  @Test
  public void shouldBeEqualForSameValue() {
    assertThat(SUCCESS_WITH_VALUE, equalTo(success(VALUE)));
  }

  @Test
  public void shouldReturnValueForOrNullOnSuccess() {
    assertThat(SUCCESS_WITH_VALUE.orNull(), equalTo(VALUE));
  }

  @Test
  public void shouldReturnNullForOrNullOnFailure() {
    assertThat(FAILURE_WITH_THROWABLE.orNull(), nullValue());
  }

  @Test
  public void shouldReturnNullForFailureOrNullOnSuccess() {
    assertThat(SUCCESS_WITH_VALUE.failureOrNull(), nullValue());
  }

  @Test
  public void shouldReturnSameThrowableForFailureOrNullOnFailureWithExplicitThrowable() {
    assertThat(FAILURE_WITH_THROWABLE.failureOrNull(), sameInstance(THROWABLE));
  }

  @Test
  public void shouldReturnSomethingForFailureOrNullOnFailure() {
    assertThat(FAILURE.failureOrNull(), notNullValue());
  }

  @Test
  public void shouldReturnNullPointerExceptionForFailureOrNullOnAbsent() {
    assertThat(ABSENT.failureOrNull(), instanceOf(NullPointerException.class));
  }

  @Test
  public void shouldReturnTrueForContainsOfSameValue() {
    assertThat(SUCCESS_WITH_VALUE.contains(VALUE), is(true));
  }

  @Test
  public void shouldReturnFalseForContainsOfOtherValue() {
    assertThat(SUCCESS_WITH_OTHER_VALUE.contains(VALUE), is(false));
  }

  @Test
  public void shouldReturnFalseForContainsOfFailure() {
    assertThat(ABSENT.contains(VALUE), is(false));
  }

  @Test
  public void shouldBeSingletonForFailureWithoutExplicitThrowable() {
    assertThat(failure(), equalTo(failure()));
  }

  @Test
  public void shouldVerifyEqualsForSqlDeleteRequest() {
    EqualsVerifier.forClass(Result.class).verify();
  }

  @Test
  public void shouldPrintStringRepresentationForSuccess() {
    assertThat(SUCCESS_WITH_VALUE, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldPrintStringRepresentationForFailure() {
    assertThat(FAILURE, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldPrintStringRepresentationForFailureWithExplicitThrowable() {
    assertThat(FAILURE_WITH_THROWABLE, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldPrintStringRepresentationForAbsent() {
    assertThat(ABSENT, hasToString(not(isEmptyOrNullString())));
  }
}
