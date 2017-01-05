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

import static com.google.android.agera.Mergers.staticMerger;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.CANCEL_FLOW;
import static com.google.android.agera.RepositoryConfig.RESET_TO_INITIAL_VALUE;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
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
import static org.robolectric.shadows.ShadowLooper.getShadowMainLooper;

import android.support.annotation.NonNull;
import com.google.android.agera.test.SingleSlotDelayedExecutor;
import com.google.android.agera.test.mocks.MockUpdatable;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

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
  private SingleSlotDelayedExecutor delayedExecutor;
  private InterruptibleMonitoredSupplier monitoredSupplier;
  @Mock
  private Supplier<Object> mockSupplier;
  private ShadowLooper looper;

  @Before
  public void setUp() {
    initMocks(this);
    updatable = mockUpdatable();
    updateDispatcher = updateDispatcher();
    delayedExecutor = new SingleSlotDelayedExecutor();
    monitoredSupplier = new InterruptibleMonitoredSupplier();
    when(mockSupplier.get()).thenReturn(UPDATED_VALUE);
    looper = getShadowMainLooper();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  private void retriggerUpdate() {
    updatable.resetUpdated();
    updateDispatcher.update();
    looper.runToEndOfTasks();
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
        .goTo(delayedExecutor)
        .thenGetFrom(mockSupplier)
        .compile();

    updatable.addToObservable(repository);
    assertThat(delayedExecutor.hasRunnable(), is(true));
    updatable.removeFromObservables();
    looper.runToEndOfTasks();
    delayedExecutor.resumeOrThrow();
    assertThat(repository, has(UPDATED_VALUE));
  }

  @Test
  public void shouldContinueFlowOnConcurrentUpdate() throws Exception {
    when(mockSupplier.get()).thenReturn(UPDATED_VALUE, ANOTHER_VALUE);
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(delayedExecutor)
        .thenGetFrom(mockSupplier)
        .compile();

    updatable.addToObservable(repository);
    assertThat(delayedExecutor.hasRunnable(), is(true));
    retriggerUpdate();
    delayedExecutor.resumeOrThrow();
    assertThat(updatable, wasUpdated());
    assertThat(repository, has(UPDATED_VALUE));

    updatable.resetUpdated();
    delayedExecutor.resumeOrThrow(); // this asserts second run started for the triggered update
    assertThat(updatable, wasUpdated());
    assertThat(repository, has(ANOTHER_VALUE));
  }

  @Test
  public void shouldCancelFlowOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(delayedExecutor)
        .thenGetFrom(mockSupplier)
        .onDeactivation(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    assertThat(delayedExecutor.hasRunnable(), is(true));
    updatable.removeFromObservables();
    looper.runToEndOfTasks();
    delayedExecutor.resumeOrThrow();
    assertThat(repository, has(INITIAL_VALUE));
    verifyZeroInteractions(mockSupplier);
  }

  @Test
  public void shouldCancelFlowOnConcurrentUpdate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(delayedExecutor)
        .thenGetFrom(mockSupplier)
        .onConcurrentUpdate(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    assertThat(delayedExecutor.hasRunnable(), is(true));
    retriggerUpdate();
    delayedExecutor.resumeOrThrow();
    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(INITIAL_VALUE));

    delayedExecutor.resumeOrThrow(); // this asserts second run started for the triggered update
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
        looper.runToEndOfTasks();
        return input;
      }
    };
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(delayedExecutor)
        .transform(cancellingFunction)
        .thenGetFrom(mockSupplier)
        .onDeactivation(CANCEL_FLOW)
        .compile();

    updatable.addToObservable(repository);
    delayedExecutor.resumeOrThrow();
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
    looper.runToEndOfTasks();
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Test
  public void shouldInterruptOnDeactivate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(newSingleThreadExecutor()) // need background thread to test interrupt
        .thenGetFrom(monitoredSupplier)
        .onDeactivation(SEND_INTERRUPT)
        .compile();

    updatable.addToObservable(repository);
    monitoredSupplier.waitForGetToStart();
    updatable.removeFromObservables();
    looper.runToEndOfTasks();
    monitoredSupplier.waitForGetToEnd();
    assertThat(monitoredSupplier.wasInterrupted(), is(true));
    assertThat(repository, has(INITIAL_VALUE));
  }

  @Ignore("Interrupt test flaky on CI server ")
  @Test
  public void shouldInterruptOnConcurrentUpdate() throws Exception {
    final Repository<Object> repository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(newSingleThreadExecutor()) // need background thread to test interrupt
        .thenGetFrom(monitoredSupplier)
        .onConcurrentUpdate(SEND_INTERRUPT)
        .compile();

    updatable.addToObservable(repository);
    monitoredSupplier.waitForGetToStart();
    retriggerUpdate();
    monitoredSupplier.waitForGetToEnd();
    assertThat(monitoredSupplier.wasInterrupted(), is(true));
    assertThat(repository, has(INITIAL_VALUE));

    looper.runToEndOfTasks(); // allows second run; asserted in waitForGetToStart()
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
      long giveUpTime = now + 20000;
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
