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

import static com.google.android.agera.Functions.identityFunction;
import static com.google.android.agera.Observables.compositeObservable;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;
import static com.google.android.agera.RepositoryConfig.CANCEL_FLOW;
import static com.google.android.agera.RepositoryConfig.RESET_TO_INITIAL_VALUE;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.WorkerHandler.MSG_CALL_ACKNOWLEDGE_CANCEL;
import static com.google.android.agera.WorkerHandler.MSG_CALL_MAYBE_START_FLOW;
import static com.google.android.agera.WorkerHandler.workerHandler;
import static java.lang.Thread.currentThread;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

@SuppressWarnings({"rawtypes", "unchecked"})
final class CompiledRepository extends BaseObservable
    implements Repository, Updatable, Runnable {

  @NonNull
  static Repository compiledRepository(
      @NonNull final Object initialValue,
      @NonNull final List<Observable> eventSources,
      final int frequency,
      @NonNull final List<Object> directives,
      @NonNull final Merger<Object, Object, Boolean> notifyChecker,
      @RepositoryConfig final int concurrentUpdateConfig,
      @RepositoryConfig final int deactivationConfig,
      @NonNull final Receiver discardedValuesDisposer) {
    final Object[] directiveArray = directives.toArray();
    return new CompiledRepository(initialValue, compositeObservable(frequency,
        eventSources.toArray(new Observable[eventSources.size()])),
        directiveArray, notifyChecker, deactivationConfig, concurrentUpdateConfig,
        discardedValuesDisposer);
  }

  //region Invariants

  @NonNull
  private final Object initialValue;
  @NonNull
  private final Observable eventSource;
  @NonNull
  private final Object[] directives;
  @NonNull
  private final Merger<Object, Object, Boolean> notifyChecker;
  @RepositoryConfig
  private final int deactivationConfig;
  @RepositoryConfig
  private final int concurrentUpdateConfig;
  @NonNull
  private final Receiver discardedValuesDisposer;
  @NonNull
  private final WorkerHandler workerHandler;

  CompiledRepository(
      @NonNull final Object initialValue,
      @NonNull final Observable eventSource,
      @NonNull final Object[] directives,
      @NonNull final Merger<Object, Object, Boolean> notifyChecker,
      @RepositoryConfig final int deactivationConfig,
      @RepositoryConfig final int concurrentUpdateConfig,
      @NonNull final Receiver discardedValuesDisposer) {
    this.initialValue = initialValue;
    this.currentValue = initialValue;
    this.intermediateValue = initialValue; // non-final field but with @NonNull requirement
    this.eventSource = eventSource;
    this.directives = directives;
    this.notifyChecker = notifyChecker;
    this.deactivationConfig = deactivationConfig;
    this.concurrentUpdateConfig = concurrentUpdateConfig;
    this.discardedValuesDisposer = discardedValuesDisposer;
    this.workerHandler = workerHandler();
  }

  //endregion Invariants

  //region Data processing flow states

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({IDLE, RUNNING, CANCEL_REQUESTED, PAUSED_AT_GO_TO, PAUSED_AT_GO_LAZY, RUNNING_LAZILY})
  private @interface RunState {}

  private static final int IDLE = 0;
  private static final int RUNNING = 1;
  private static final int CANCEL_REQUESTED = 2;
  private static final int PAUSED_AT_GO_TO = 3;
  private static final int PAUSED_AT_GO_LAZY = 4;
  private static final int RUNNING_LAZILY = 5;

  @RunState
  private int runState = IDLE;
  private boolean restartNeeded;
  /** Index of the last goTo()/goLazy() directive, for resuming, or -1 for other directives. */
  private int lastDirectiveIndex = -1;
  /** The current value to be exposed through the repository's get method. */
  @NonNull
  private Object currentValue;
  /** The intermediate value computed by the executed part of the flow. */
  @NonNull
  private Object intermediateValue;
  /** The thread currently running a directive that can be interrupted. */
  @Nullable
  private Thread currentThread;

  //endregion Data processing flow states

  //region Starting and requesting cancellation
  // - All methods in this region are called from the Worker Looper thread, but reading and writing
  //   states that might be accessed from a different thread are still synchronized.

  @Override
  protected void observableActivated() {
    eventSource.addUpdatable(this);
    maybeStartFlow();
  }

  @Override
  protected void observableDeactivated() {
    eventSource.removeUpdatable(this);
    maybeCancelFlow(deactivationConfig, false);
  }

  @Override
  public void update() {
    maybeCancelFlow(concurrentUpdateConfig, true);
    maybeStartFlow();
  }

  /**
   * Called on the worker looper thread. Starts the data processing flow if it's not running. This
   * also cancels the lazily-executed part of the flow if the run state is "paused at lazy".
   */
  void maybeStartFlow() {
    synchronized (this) {
      if (runState == IDLE || runState == PAUSED_AT_GO_LAZY) {
        runState = RUNNING;
        lastDirectiveIndex = -1; // this could be pointing at the goLazy directive
        restartNeeded = false;
      } else {
        if (runState == CANCEL_REQUESTED) {
          // flow may still be processing the previous deactivation;
          // make sure to restart
          restartNeeded = true;
        }
        return; // flow already running or scheduled to restart, do not continue
      }
    }
    intermediateValue = currentValue;
    runFlowFrom(0, false);
  }

  /**
   * Called on the worker looper thread. Depending on the {@code config}, cancels the data
   * processing flow, resets the value, and/or sends the interrupt signal to the thread currently
   * processing a getFrom/mergeIn/transform instruction of the flow.
   *
   * @param scheduleRestart Whether to schedule a restart if a current flow is canceled.
   */
  private void maybeCancelFlow(@RepositoryConfig final int config, final boolean scheduleRestart) {
    synchronized (this) {
      if (runState == RUNNING || runState == PAUSED_AT_GO_TO) {
        restartNeeded = scheduleRestart;

        // If config forbids cancellation, exit now after scheduling the restart, to skip the
        // cancellation request.
        if ((config & CANCEL_FLOW) == 0) {
          return;
        }

        runState = CANCEL_REQUESTED;

        if ((config & SEND_INTERRUPT) == SEND_INTERRUPT && currentThread != null) {
          currentThread.interrupt();
        }
      }

      // Resetting to the initial value should be done even if the flow is not running.
      if (!scheduleRestart && (config & RESET_TO_INITIAL_VALUE) == RESET_TO_INITIAL_VALUE) {
        setNewValueLocked(initialValue);
      }
    }
  }

  //endregion Starting and requesting cancellation

  //region Acknowledging cancellation and restarting
  // - Apart from handleMessage(), other methods in this region can be called from a thread that is
  //   not the Worker Looper thread.

  /**
   * Checks if the current data processing flow has been requested cancellation. Acknowledges the
   * request if so. This must be called while locked in a synchronized context.
   *
   * @return Whether the data processing flow is cancelled.
   */
  private boolean checkCancellationLocked() {
    if (runState == CANCEL_REQUESTED) {
      workerHandler.obtainMessage(MSG_CALL_ACKNOWLEDGE_CANCEL, this).sendToTarget();
      return true;
    }
    return false;
  }

  /**
   * Called by the worker handler.
   */
  void acknowledgeCancel() {
    boolean shouldStartFlow = false;
    Object discardedIntermediateValue = null;
    synchronized (this) {
      if (runState == CANCEL_REQUESTED) {
        runState = IDLE;
        if (intermediateValue != currentValue) {
          discardedIntermediateValue = intermediateValue;
          intermediateValue = currentValue; // GC the intermediate value but keep field non-null.
        }
        shouldStartFlow = restartNeeded;
      }
    }
    if (discardedIntermediateValue != null) {
      discardedValuesDisposer.accept(discardedIntermediateValue);
    }
    if (shouldStartFlow) {
      maybeStartFlow();
    }
  }

  /**
   * Checks if the data processing flow needs restarting, and restarts it if so. This must be called
   * while locked in a synchronized context and after the previous data processing flow has
   * completed.
   */
  private void checkRestartLocked() {
    if (restartNeeded) {
      workerHandler.obtainMessage(MSG_CALL_MAYBE_START_FLOW, this).sendToTarget();
    }
  }

  //endregion Acknowledging cancellation and restarting

  //region Running directives
  // The directive creation methods are interleaved here so the index-to-operator relation is clear.

  private static final int END = 0;
  private static final int GET_FROM = 1;
  private static final int MERGE_IN = 2;
  private static final int TRANSFORM = 3;
  private static final int CHECK = 4;
  private static final int GO_TO = 5;
  private static final int GO_LAZY = 6;
  private static final int SEND_TO = 7;
  private static final int BIND = 8;
  private static final int FILTER_SUCCESS = 9;
  private static final int FILTER_FAILURE = 10;

  /**
   * @param asynchronously Whether this flow is run asynchronously. True after the first goTo and
   *     before goLazy. This is to omit unnecessarily locking the synchronized context to check for
   *     cancellation, because if the flow is run synchronously, cancellation requests theoretically
   *     cannot be delivered here.
   */
  private void runFlowFrom(final int index, final boolean asynchronously) {
    final Object[] directives = this.directives;
    final int length = directives.length;
    int i = index;
    while (0 <= i && i < length) {
      final int directiveType = (Integer) directives[i];
      if (asynchronously || directiveType == GO_TO || directiveType == GO_LAZY) {
        // Check cancellation before running the next directive. This needs to be done while locked.
        // For goTo and goLazy, because they need to change the states and suspend the flow, they
        // need the lock and are therefore treated specially here.
        synchronized (this) {
          if (checkCancellationLocked()) {
            break;
          }
          if (directiveType == GO_TO) {
            setPausedAtGoToLocked(i);
            // the actual executor delivery is done below, outside the lock, to eliminate any
            // deadlock possibility.
          } else if (directiveType == GO_LAZY) {
            setLazyAndEndFlowLocked(i);
            return;
          }
        }
      }

      // A table-switch on a handful of options is a good compromise in code size and runtime
      // performance comparing to a full-fledged double-dispatch pattern with subclasses.
      switch (directiveType) {
        case GET_FROM:
          i = runGetFrom(directives, i);
          break;
        case MERGE_IN:
          i = runMergeIn(directives, i);
          break;
        case TRANSFORM:
          i = runTransform(directives, i);
          break;
        case CHECK:
          i = runCheck(directives, i);
          break;
        case GO_TO:
          i = runGoTo(directives, i);
          break;
        case SEND_TO:
          i = runSendTo(directives, i);
          break;
        case BIND:
          i = runBindWith(directives, i);
          break;
        case FILTER_SUCCESS:
          i = runFilterSuccess(directives, i);
          break;
        case FILTER_FAILURE:
          i = runFilterFailure(directives, i);
          break;
        case END:
          i = runEnd(directives, i);
          break;
        // Missing GO_LAZY but it has already been dealt with in the synchronized block above.
      }
    }
  }

  static void addGetFrom(@NonNull final Supplier supplier,
      @NonNull final List<Object> directives) {
    directives.add(GET_FROM);
    directives.add(supplier);
  }

  private int runGetFrom(@NonNull final Object[] directives, final int index) {
    final Supplier supplier = (Supplier) directives[index + 1];
    intermediateValue = checkNotNull(supplier.get());
    return index + 2;
  }

  static void addMergeIn(@NonNull final Supplier supplier, @NonNull final Merger merger,
      @NonNull final List<Object> directives) {
    directives.add(MERGE_IN);
    directives.add(supplier);
    directives.add(merger);
  }

  private int runMergeIn(@NonNull final Object[] directives, final int index) {
    final Supplier supplier = (Supplier) directives[index + 1];
    final Merger merger = (Merger) directives[index + 2];
    intermediateValue = checkNotNull(merger.merge(intermediateValue, supplier.get()));
    return index + 3;
  }

  static void addTransform(@NonNull final Function function,
      @NonNull final List<Object> directives) {
    directives.add(TRANSFORM);
    directives.add(function);
  }

  private int runTransform(@NonNull final Object[] directives, final int index) {
    final Function function = (Function) directives[index + 1];
    intermediateValue = checkNotNull(function.apply(intermediateValue));
    return index + 2;
  }

  static void addCheck(@NonNull final Function caseFunction,
      @NonNull final Predicate casePredicate,
      @Nullable final Function terminatingValueFunction,
      @NonNull final List<Object> directives) {
    directives.add(CHECK);
    directives.add(caseFunction);
    directives.add(casePredicate);
    directives.add(terminatingValueFunction);
  }

  private int runCheck(@NonNull final Object[] directives, final int index) {
    final Function caseFunction = (Function) directives[index + 1];
    final Predicate casePredicate = (Predicate) directives[index + 2];
    final Function terminatingValueFunction = (Function) directives[index + 3];

    final Object caseValue = caseFunction.apply(intermediateValue);
    if (casePredicate.apply(caseValue)) {
      return index + 4;
    } else {
      runTerminate(caseValue, terminatingValueFunction);
      return -1;
    }
  }

  static void addGoTo(@NonNull final Executor executor, @NonNull final List<Object> directives) {
    directives.add(GO_TO);
    directives.add(executor);
  }

  private int runGoTo(@NonNull final Object[] directives, final int index) {
    Executor executor = (Executor) directives[index + 1];
    executor.execute(this);
    return -1;
  }

  private static int continueFromGoTo(@NonNull final Object[] directives, final int index) {
    checkState(directives[index].equals(GO_TO), "Inconsistent directive state for goTo");
    return index + 2;
  }

  static void addGoLazy(@NonNull final List<Object> directives) {
    directives.add(GO_LAZY);
  }

  private static int continueFromGoLazy(@NonNull final Object[] directives, final int index) {
    checkState(directives[index].equals(GO_LAZY), "Inconsistent directive state for goLazy");
    return index + 1;
  }

  static void addSendTo(@NonNull final Receiver receiver, @NonNull final List<Object> directives) {
    directives.add(SEND_TO);
    directives.add(receiver);
  }

  private int runSendTo(@NonNull final Object[] directives, final int index) {
    Receiver receiver = (Receiver) directives[index + 1];
    receiver.accept(intermediateValue);
    return index + 2;
  }

  static void addBindWith(@NonNull final Supplier supplier, @NonNull final Binder binder,
      @NonNull final List<Object> directives) {
    directives.add(BIND);
    directives.add(supplier);
    directives.add(binder);
  }

  private int runBindWith(@NonNull final Object[] directives, final int index) {
    final Supplier supplier = (Supplier) directives[index + 1];
    final Binder binder = (Binder) directives[index + 2];
    binder.bind(intermediateValue, supplier.get());
    return index + 3;
  }

  static void addFilterSuccess(
      @Nullable final Function terminatingValueFunction, @NonNull final List<Object> directives) {
    directives.add(FILTER_SUCCESS);
    directives.add(terminatingValueFunction);
  }

  private int runFilterSuccess(@NonNull final Object[] directives, final int index) {
    final Function terminatingValueFunction = (Function) directives[index + 1];
    final Result tryValue = (Result) intermediateValue;
    if (tryValue.succeeded()) {
      intermediateValue = tryValue.get();
      return index + 2;
    } else {
      runTerminate(tryValue.getFailure(), terminatingValueFunction);
      return -1;
    }
  }

  static void addFilterFailure(@NonNull final List<Object> directives) {
    directives.add(FILTER_FAILURE);
  }

  private int runFilterFailure(@NonNull final Object[] directives, final int index) {
    final Result tryValue = (Result) intermediateValue;
    if (tryValue.succeeded()) {
      runTerminate(tryValue.get(), identityFunction());
      return -1;
    } else {
      intermediateValue = tryValue.getFailure();
      return index + 1;
    }
  }

  private void runTerminate(@NonNull final Object caseValue,
      @Nullable final Function terminatingValueFunction) {
    if (terminatingValueFunction == null) {
      skipAndEndFlow();
    } else {
      setNewValueAndEndFlow(checkNotNull(terminatingValueFunction.apply(caseValue)));
    }
  }

  static void addEnd(final boolean skip, @NonNull final List<Object> directives) {
    directives.add(END);
    directives.add(skip);
  }

  private int runEnd(@NonNull final Object[] directives, final int index) {
    final boolean skip = (Boolean) directives[index + 1];
    if (skip) {
      skipAndEndFlow();
    } else {
      setNewValueAndEndFlow(intermediateValue);
    }
    return -1;
  }

  //endregion Running directives

  //region Completing, pausing and resuming flow

  private void skipAndEndFlow() {
    Object discardedIntermediateValue = null;
    synchronized (this) {
      runState = IDLE;
      if (intermediateValue != currentValue) {
        discardedIntermediateValue = intermediateValue;
        intermediateValue = currentValue; // GC the intermediate value but keep field non-null.
      }
      checkRestartLocked();
    }
    if (discardedIntermediateValue != null) {
      discardedValuesDisposer.accept(discardedIntermediateValue);
    }
  }

  private synchronized void setNewValueAndEndFlow(@NonNull final Object newValue) {
    Object discardedIntermediateValue = null;
    synchronized (this) {
      final boolean wasRunningLazily = runState == RUNNING_LAZILY;
      runState = IDLE;
      if (intermediateValue != newValue) {
        discardedIntermediateValue = intermediateValue;
        intermediateValue = newValue; // GC the intermediate value but keep field non-null.
      }
      if (wasRunningLazily) {
        currentValue = newValue; // Don't notify if this new value is produced lazily
      } else {
        setNewValueLocked(newValue); // May notify otherwise
      }
      checkRestartLocked();
    }
    if (discardedIntermediateValue != null) {
      discardedValuesDisposer.accept(discardedIntermediateValue);
    }
  }

  private void setNewValueLocked(@NonNull final Object newValue) {
    final boolean shouldNotify = notifyChecker.merge(currentValue, newValue);
    currentValue = newValue;
    if (shouldNotify) {
      dispatchUpdate();
    }
  }

  private void setPausedAtGoToLocked(final int resumeIndex) {
    lastDirectiveIndex = resumeIndex;
    runState = PAUSED_AT_GO_TO;
  }

  /** Called from the executor of a goTo instruction to continue processing. */
  @Override
  public void run() {
    final Thread myThread = currentThread();
    final int index;
    synchronized (this) {
      index = lastDirectiveIndex;
      checkState(runState == PAUSED_AT_GO_TO || runState == CANCEL_REQUESTED,
          "Illegal call of Runnable.run()");
      lastDirectiveIndex = -1;

      if (checkCancellationLocked()) {
        return;
      }
      runState = RUNNING;
      // allow thread interruption (set this when still holding the lock)
      currentThread = myThread;
    }
    // leave the synchronization lock to run the rest of the flow
    runFlowFrom(continueFromGoTo(directives, index), true);
    // consume any unconsumed interrupted flag
    Thread.interrupted();
    // disallow interrupting the current thread, but chances are the next directive has started
    // asynchronously, so check currentThread is still this thread. This also works if a goTo
    // directive is given a synchronous executor, in which case the next part of the flow will
    // have been completed by now and currentThread will have been reset by that invocation of
    // runFlowFrom().
    synchronized (this) {
      if (currentThread == myThread) {
        currentThread = null;
      }
    }
  }

  private void setLazyAndEndFlowLocked(final int resumeIndex) {
    lastDirectiveIndex = resumeIndex;
    runState = PAUSED_AT_GO_LAZY;
    dispatchUpdate();
    checkRestartLocked();
  }

  @NonNull
  @Override
  public synchronized Object get() {
    if (runState == PAUSED_AT_GO_LAZY) {
      final int index = lastDirectiveIndex;
      runState = RUNNING_LAZILY;
      runFlowFrom(continueFromGoLazy(directives, index), false);
    }
    return currentValue;
  }

  //endregion Completing, pausing and resuming flow
}
