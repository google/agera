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

import static com.google.android.agera.Asyncs.goTo;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class AsyncsTest {
  private static final Integer VALUE = 53;
  private static final Integer ANOTHER_VALUE = 54;

  @Mock
  private Executor mockExecutor;
  @Mock
  private Condition mockCondition;
  @Mock
  private Receiver<Integer> mockValueReceiver;
  @Mock
  private Receiver<Integer> mockAnotherValueReceiver;
  @Captor
  private ArgumentCaptor<Runnable> runnableCaptor;

  private Async<Integer, Integer> goToExecutor;
  private Async<Integer, Integer> goToExecutor2;

  @Before
  public void setUp() {
    initMocks(this);
    goToExecutor = goTo(mockExecutor);
    goToExecutor2 = goTo(mockExecutor, Integer.class);
  }

  @Test
  public void shouldGoToExecutor() {
    goToExecutor.async(VALUE, mockValueReceiver, mockCondition);

    verify(mockExecutor).execute(runnableCaptor.capture());
    verifyZeroInteractions(mockValueReceiver);

    runnableCaptor.getValue().run();

    verify(mockValueReceiver).accept(VALUE);
    verifyZeroInteractions(mockCondition);
  }

  @Test
  public void shouldGoToExecutorMultipleEntry() {
    goToExecutor2.async(VALUE, mockValueReceiver, mockCondition);
    goToExecutor2.async(ANOTHER_VALUE, mockAnotherValueReceiver, mockCondition);

    verify(mockExecutor, times(2)).execute(runnableCaptor.capture());
    verifyZeroInteractions(mockValueReceiver, mockAnotherValueReceiver);

    for (final Runnable runnable : runnableCaptor.getAllValues()) {
      runnable.run();
    }

    verify(mockValueReceiver).accept(VALUE);
    verify(mockAnotherValueReceiver).accept(ANOTHER_VALUE);
    verifyZeroInteractions(mockCondition);
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Asyncs.class, hasPrivateConstructor());
  }
}
