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
import static com.google.android.agera.Mergers.staticMerger;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RexConfig.CANCEL_FLOW;
import static com.google.android.agera.RexConfig.RESET_TO_INITIAL_VALUE;
import static com.google.android.agera.RexConfig.SEND_INTERRUPT;
import static com.google.android.agera.test.MockAsync.mockAsync;
import static com.google.android.agera.test.matchers.ConditionApplies.applies;
import static com.google.android.agera.test.matchers.SupplierGives.has;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasNotUpdated;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

import com.google.android.agera.test.MockAsync;
import com.google.android.agera.test.mocks.MockUpdatable;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RepositoryConfigTest {
  private static final Object INITIAL_VALUE = "INITIAL_VALUE";
  private static final Object UPDATED_VALUE = "UPDATED_VALUE";
  private static final Object ANOTHER_VALUE = "ANOTHER_VALUE";
  private static final Object RESUMED_VALUE = "RESUMED_VALUE";
  private static final Object UNEXPECTED_VALUE = "UNEXPECTED_VALUE";

  private MockUpdatable updatable;
  private UpdateDispatcher updateDispatcher;
  private MockAsync<Object, Object> async;
  private InterruptibleMonitoredSupplier monitoredSupplier;
  @Mock
  private Supplier<Object> mockSupplier;

  @Before
  public void setUp() {
    initMocks(this);
    updatable = mockUpdatable();
    updateDispatcher = updateDispatcher();
    async = mockAsync();
    monitoredSupplier = new InterruptibleMonitoredSupplier();
    when(mockSupplier.get()).thenReturn(UPDATED_VALUE);
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  private void retriggerUpdate() {
    updatable.resetUpdated();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
  }

  @Test
  public void shouldNotNotifyWhenValueUnchanged() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .thenGetFrom(mockSupplier)
        .compile();

    updatable.addToObservable(repository);
    retriggerUpdate();
    assertThat(updatable, wasNotUpdated());
    verify(mockSupplier, times(2)).get();
  }

  @Test
  public void shouldNotifyWhenValueUnchangedButAlwaysNotify() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .thenGetFrom(mockSupplier)
        .notifyIf(staticMerger(true))
        .compile();

    updatable.addToObservable(repository);
    retriggerUpdate();
    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldContinueFlowOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(async)
        .thenGetFrom(mockSupplier)
        .compile();

    updatable.addToObservable(repository);
    assertThat(async.wasCalled(), is(true));
    updatable.removeFromObservables();
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(repository, has(UPDATED_VALUE));
  }

  @Test
  public void shouldContinueFlowOnConcurrentUpdate() throws Exception {
    when(mockSupplier.get()).thenReturn(UPDATED_VALUE, ANOTHER_VALUE);
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(async)
        .thenGetFrom(mockSupplier)
        .compile();

    updatable.addToObservable(repository);
    assertThat(async.wasCalled(), is(true));
    retriggerUpdate();
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(updatable, wasUpdated());
    assertThat(repository, has(UPDATED_VALUE));

    updatable.resetUpdated();
    // this asserts second run started for the triggered update
    async.expectAndOutput(UPDATED_VALUE, UPDATED_VALUE);
    assertThat(updatable, wasUpdated());
    assertThat(repository, has(ANOTHER_VALUE));
  }

  @Test
  public void shouldCancelFlowOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(async)
        .thenGetFrom(mockSupplier)
        .onDeactivation(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    assertThat(async.wasCalled(), is(true));
    updatable.removeFromObservables();
    assertThat(async.cancelled(), applies());
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(repository, has(INITIAL_VALUE));
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldCancelFlowOnConcurrentUpdate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(async)
        .thenGetFrom(mockSupplier)
        .onConcurrentUpdate(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    assertThat(async.wasCalled(), is(true));
    retriggerUpdate();
    assertThat(async.cancelled(), applies());
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_VALUE));

    // this asserts second run started for the triggered update
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(updatable, wasUpdated());
    assertThat(repository, has(UPDATED_VALUE));
  }

  @Test
  public void shouldCancelFlowMidFlow() throws Exception {
    final Function<Object, Object> cancellingFunction = new Function<Object, Object>() {
      @NonNull
      @Override
      public Object apply(@NonNull Object input) {
        // Sneak in a deactivation here to test cancellation mid-flow.
        updatable.removeFromObservables();
        return input;
      }
    };
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(async)
        .transform(cancellingFunction)
        .thenGetFrom(mockSupplier)
        .onDeactivation(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    async.expectAndOutput(INITIAL_VALUE, INITIAL_VALUE);
    assertThat(repository, has(INITIAL_VALUE));
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldResetToInitialValueOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .thenGetFrom(mockSupplier)
        .onDeactivation(RESET_TO_INITIAL_VALUE)
        .compile();

    updatable.addToObservable(repository);
    assertThat(repository, has(UPDATED_VALUE));
    updatable.removeFromObservables();
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldInterruptOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(goTo(newSingleThreadExecutor())) // need background thread to test interrupt
        .thenGetFrom(monitoredSupplier)
        .onDeactivation(SEND_INTERRUPT)
        .compile();

    updatable.addToObservable(repository);
    monitoredSupplier.waitForGetToStart();
    updatable.removeFromObservables();
    monitoredSupplier.waitForGetToEnd();
    assertThat(monitoredSupplier.wasInterrupted(), is(true));
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldInterruptOnConcurrentUpdate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .async(goTo(newSingleThreadExecutor())) // need background thread to test interrupt
        .thenGetFrom(monitoredSupplier)
        .onConcurrentUpdate(SEND_INTERRUPT)
        .compile();

    updatable.addToObservable(repository);
    monitoredSupplier.waitForGetToStart();
    retriggerUpdate();
    monitoredSupplier.waitForGetToEnd();
    assertThat(monitoredSupplier.wasInterrupted(), is(true));
    assertThat(repository, has(INITIAL_VALUE));

    runUiThreadTasksIncludingDelayedTasks(); // allows second run; asserted in waitForGetToStart()
    monitoredSupplier.waitForGetToStart().resumeIfWaiting().waitForGetToEnd();
    assertThat(repository, has(RESUMED_VALUE));
  }

  private static final class InterruptibleMonitoredSupplier implements Supplier<Object> {
    private static final int ENDED = 0;
    private static final int STARTED = 1;
    private static final int RESUMED = 2;

    private int state = ENDED;
    private boolean interrupted;

    @NonNull
    @Override
    public synchronized Object get() {
      interrupted = false;
      state = STARTED;
      notifyAll();
      try {
        return waitForState(RESUMED) ? RESUMED_VALUE : UNEXPECTED_VALUE;
      } catch (InterruptedException e) {
        interrupted = true;
        return UNEXPECTED_VALUE;
      } finally {
        state = ENDED;
        notifyAll();
      }
    }

    public synchronized InterruptibleMonitoredSupplier waitForGetToStart()
        throws InterruptedException {
      assertThat("monitoredSupplier.get() should start", waitForState(STARTED));
      return this;
    }

    public synchronized InterruptibleMonitoredSupplier resumeIfWaiting() {
      state = RESUMED;
      notifyAll();
      return this;
    }

    public synchronized void waitForGetToEnd() throws InterruptedException {
      assertThat("monitoredSupplier.get() should end", waitForState(ENDED));
    }

    private boolean waitForState(int waitForState) throws InterruptedException {
      long now = System.currentTimeMillis();
      long giveUpTime = now + 5000;
      while (state != waitForState && now < giveUpTime) {
        wait(giveUpTime - now);
        now = System.currentTimeMillis();
      }
      return state == waitForState;
    }

    public synchronized boolean wasInterrupted() {
      return interrupted;
    }
  }

}
