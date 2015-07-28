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

import static android.os.Looper.myLooper;
import static com.google.android.agera.Conditions.falseCondition;
import static com.google.android.agera.Conditions.trueCondition;
import static com.google.android.agera.Observables.compositeObservable;
import static com.google.android.agera.Observables.conditionalObservable;
import static com.google.android.agera.Observables.perLoopObservable;
import static com.google.android.agera.Observables.perMillisecondObservable;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;
import static org.robolectric.internal.ShadowExtractor.extract;
import static org.robolectric.shadows.ShadowLooper.idleMainLooper;

import com.google.android.agera.test.mocks.MockUpdatable;

import android.app.Application;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.Scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class ObservablesTest {
  private static final int FILTER_TIME = 10000;

  private Observable compositeObservableOfMany;
  private Observable chainedCompositeObservableOfOne;
  private Observable chainedCompositeObservable;
  private UpdateDispatcher firstUpdateDispatcher;
  private UpdateDispatcher secondUpdateDispatcher;
  private UpdateDispatcher thirdUpdateDispatcher;
  private Observable trueConditionalObservable;
  private Observable falseConditionalObservable;
  private MockUpdatable updatable;
  private MockUpdatable secondUpdatable;
  private Scheduler scheduler;
  private UpdateDispatcher updateDispatcher;
  @Mock
  private ActivationHandler mockActivationHandler;
  private UpdateDispatcher updateDispatcherWithUpdatablesChanged;

  @Before
  public void setUp() {
    initMocks(this);
    //noinspection ConstantConditions
    scheduler = ((ShadowLooper) extract(myLooper())).getScheduler();
    updateDispatcherWithUpdatablesChanged = updateDispatcher(mockActivationHandler);
    updateDispatcher = updateDispatcher();
    firstUpdateDispatcher = updateDispatcher();
    secondUpdateDispatcher = updateDispatcher();
    thirdUpdateDispatcher = updateDispatcher();
    trueConditionalObservable = conditionalObservable(trueCondition(), firstUpdateDispatcher);
    falseConditionalObservable = conditionalObservable(falseCondition(), firstUpdateDispatcher);
    compositeObservableOfMany = compositeObservable(firstUpdateDispatcher,
        secondUpdateDispatcher);
    chainedCompositeObservableOfOne = compositeObservable(
        compositeObservable(firstUpdateDispatcher));
    chainedCompositeObservable = compositeObservable(compositeObservable(firstUpdateDispatcher,
        secondUpdateDispatcher), thirdUpdateDispatcher);
    updatable = mockUpdatable();
    secondUpdatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
    secondUpdatable.removeFromObservables();
  }

  @Test
  public void shouldUpdateFromFirstObservablesInCompositeOfMany() {
    updatable.addToObservable(compositeObservableOfMany);

    firstUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateFromSecondObservablesInCompositeOfMany() {
    updatable.addToObservable(compositeObservableOfMany);

    secondUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateFromFirstObservablesInChainedCompositeOfOne() {
    updatable.addToObservable(chainedCompositeObservableOfOne);

    firstUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateFromFirstChainInChainedComposite() {
    updatable.addToObservable(chainedCompositeObservable);

    secondUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateFromSecondChainInChainedComposite() {
    updatable.addToObservable(chainedCompositeObservable);

    thirdUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotUpdateConditionalObservableForFalseCondition() {
    updatable.addToObservable(trueConditionalObservable);

    firstUpdateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateConditionalObservableForTrueCondition() {
    updatable.addToObservable(falseConditionalObservable);

    firstUpdateDispatcher.update();

    assertThat(updatable, not(wasUpdated()));
  }

  @Test
  public void shouldBeAbleToCreateEmptyObservable() {
    assertThat(compositeObservable(), notNullValue());
  }

  @Test
  public void shouldCallFirstAddedForUpdateDispatcher() {
    updatable.addToObservable(updateDispatcherWithUpdatablesChanged);

    verify(mockActivationHandler).observableActivated(updateDispatcherWithUpdatablesChanged);
  }

  @Test
  public void shouldCallFirstAddedOnceOnlyForUpdateDispatcher() {
    updatable.addToObservable(updateDispatcherWithUpdatablesChanged);
    mockUpdatable().addToObservable(updateDispatcherWithUpdatablesChanged);

    verify(mockActivationHandler).observableActivated(updateDispatcherWithUpdatablesChanged);
  }

  @Test
  public void shouldCallLastRemovedForUpdateDispatcher() {
    updatable.addToObservable(updateDispatcherWithUpdatablesChanged);

    verify(mockActivationHandler).observableActivated(updateDispatcherWithUpdatablesChanged);
  }

  @Test
  public void shouldCallLastRemovedOnceOnlyForUpdateDispatcher() {
    updatable.addToObservable(updateDispatcherWithUpdatablesChanged);
    secondUpdatable.addToObservable(updateDispatcherWithUpdatablesChanged);

    updatable.removeFromObservables();
    secondUpdatable.removeFromObservables();

    verify(mockActivationHandler).observableActivated(updateDispatcherWithUpdatablesChanged);
  }


  @Test
  public void shouldUpdateAllUpdatablesWhenUpdateFromSameThreadForUpdateDispatcher() {
    updatable.addToObservable(updateDispatcher);
    secondUpdatable.addToObservable(updateDispatcher);

    updateDispatcher.update();

    assertThat(updatable, wasUpdated());
    assertThat(secondUpdatable, wasUpdated());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIllegalArgumentExceptionIfAddingTheSameUpdatableTwice() {
    updatable.addToObservable(updateDispatcher);
    updatable.addToObservable(updateDispatcher);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIllegalArgumentExceptionIfRemovingANotAddedUpdatable() {
    updateDispatcher.removeUpdatable(updatable);
  }

  @Test
  public void shouldUpdatePerCycleObservable() {
    updatable.addToObservable(perLoopObservable(updateDispatcher));

    updateDispatcher.update();

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdatePerMillisecondObservable() {
    final long expectedDelayedTime = scheduler.getCurrentTime() + FILTER_TIME;
    updatable.addToObservable(perMillisecondObservable(FILTER_TIME, updateDispatcher));

    updateDispatcher.update();
    idleMainLooper(FILTER_TIME);

    assertThat(updatable, wasUpdated());
    assertThat(scheduler.getCurrentTime(), greaterThanOrEqualTo(expectedDelayedTime));
  }

  @Test
  public void shouldHandleManyObservables() {
    final int numberOfObservables = 10;
    for (int passes = 0; passes < 3; passes++) {
      List<MockUpdatable> mockUpdatables = new ArrayList<>(numberOfObservables);
      for (int i = 0; i < numberOfObservables; i++) {
        MockUpdatable mockUpdatable = mockUpdatable();
        mockUpdatables.add(mockUpdatable);
        mockUpdatable.addToObservable(updateDispatcher);
      }
      Collections.shuffle(mockUpdatables);
      for (MockUpdatable mockUpdatable : mockUpdatables) {
        mockUpdatable.removeFromObservables();
      }
    }
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Observables.class, hasPrivateConstructor());
  }

  private void sendBroadcast(final Intent intent) {
    getApplication().sendBroadcast(intent);
  }

  private static Application getApplication() {
    return RuntimeEnvironment.application;
  }
}
