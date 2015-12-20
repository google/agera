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
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Mergers.objectsUnequal;
import static com.google.android.agera.Mergers.returnSecond;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;
import static com.google.android.agera.Rex.compiledReaction;
import static com.google.android.agera.Rex.compiledRepository;
import static com.google.android.agera.RexRunner.addBindWith;
import static com.google.android.agera.RexRunner.addCheck;
import static com.google.android.agera.RexRunner.addEnd;
import static com.google.android.agera.RexRunner.addFilterSuccess;
import static com.google.android.agera.RexRunner.addGetFrom;
import static com.google.android.agera.RexRunner.addGoLazy;
import static com.google.android.agera.RexRunner.addGoTo;
import static com.google.android.agera.RexRunner.addMergeIn;
import static com.google.android.agera.RexRunner.addSendTo;
import static com.google.android.agera.RexRunner.addTransform;

import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.Executor;

@SuppressWarnings({"unchecked, rawtypes"})
final class RexCompiler implements
    ReactionCompilerStates.RFlow,
    ReactionCompilerStates.RTermination,
    RepositoryCompilerStates.RFrequency,
    RepositoryCompilerStates.RFlow,
    RepositoryCompilerStates.RTermination,
    RepositoryCompilerStates.RConfig,
    RexCompilerStates.RConfig {

  private static final ThreadLocal<RexCompiler> compilers = new ThreadLocal<>();

  @NonNull
  static <TVal> RepositoryCompilerStates.REventSource<TVal, TVal> repositoryWithInitialValue(
      @NonNull final TVal initialValue) {
    checkNotNull(Looper.myLooper());
    RexCompiler compiler = compilers.get();
    if (compiler == null) {
      compiler = new RexCompiler();
    } else {
      // Remove compiler from the ThreadLocal to prevent reuse in the middle of a compilation.
      // recycle(), called by compile(), will return the compiler here. ThreadLocal.set(null) keeps
      // the entry (with a null value) whereas remove() removes the entry; because we expect the
      // return of the compiler, don't use the heavier remove().
      compilers.set(null);
    }
    return compiler.start(initialValue);
  }

  @NonNull
  static <TVal> ReactionCompilerStates.RFlow<TVal, TVal,
      RexCompilerStates.RConfig<TVal, Reaction<TVal>, ?>, ?> reactionFor(
      @NonNull Reservoir<? extends TVal> reservoir) {
    checkNotNull(reservoir);
    RexCompiler compiler = (RexCompiler) repositoryWithInitialValue(REACTION_TRIGGER)
        .observe(reservoir)
        .onUpdatesPerLoop()
        .attemptGetFrom(reservoir).orSkip();
    compiler.reservoirForReaction = reservoir;
    return compiler;
  }

  private static void recycle(@NonNull final RexCompiler compiler) {
    compilers.set(compiler);
  }

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({NOTHING, FIRST_EVENT_SOURCE, FREQUENCY_OR_MORE_EVENT_SOURCE, FLOW,
      TERMINATE_THEN_FLOW, TERMINATE_THEN_END, CONFIG})
  private @interface Expect {}

  private static final int NOTHING = 0;
  private static final int FIRST_EVENT_SOURCE = 1;
  private static final int FREQUENCY_OR_MORE_EVENT_SOURCE = 2;
  private static final int FLOW = 3;
  private static final int TERMINATE_THEN_FLOW = 4;
  private static final int TERMINATE_THEN_END = 5;
  private static final int CONFIG = 6;

  private static final Object REACTION_TRIGGER = new Object();
  private static final Function REACTION_TRIGGER_STATIC_FUNCTION = staticFunction(REACTION_TRIGGER);

  private Object initialValue;
  private final ArrayList<Observable> eventSources = new ArrayList<>();
  private int frequency;
  private final ArrayList<Object> directives = new ArrayList<>();
  // 2x fields below: store caseExtractor and casePredicate for check(caseExtractor, casePredicate)
  // for use in terminate(); if null then terminate() is terminating an attempt directive.
  private Function caseExtractor;
  private Predicate casePredicate;
  private boolean goLazyUsed;
  private Merger notifyChecker = objectsUnequal();
  @RexConfig
  private int deactivationConfig;
  @RexConfig
  private int concurrentUpdateConfig;

  @Expect
  private int expect;
  @Nullable
  private Reservoir reservoirForReaction;

  private RexCompiler() {}

  private RexCompiler start(@NonNull final Object initialValue) {
    checkExpect(NOTHING);
    expect = FIRST_EVENT_SOURCE;
    this.initialValue = initialValue;
    return this;
  }

  private void checkExpect(@Expect final int accept) {
    checkState(expect == accept, "Unexpected compiler state");
  }

  private void checkExpect(@Expect final int accept1, @Expect final int accept2) {
    checkState(expect == accept1 || expect == accept2, "Unexpected compiler state");
  }

  private void checkCompilingRepository() {
    checkState(reservoirForReaction == null, "Unexpected use of repository-only methods");
  }

  private void checkCompilingReaction() {
    checkState(reservoirForReaction != null, "Unexpected use of reaction-only methods");
  }

  private void checkGoLazyUnused() {
    checkState(!goLazyUsed, "Unexpected occurrence of async directive after goLazy()");
  }

  //region REventSource

  @NonNull
  @Override
  public RexCompiler observe(@NonNull final Observable... observables) {
    checkCompilingRepository();
    checkExpect(FIRST_EVENT_SOURCE, FREQUENCY_OR_MORE_EVENT_SOURCE);
    for (Observable observable : observables) {
      eventSources.add(checkNotNull(observable));
    }
    expect = FREQUENCY_OR_MORE_EVENT_SOURCE;
    return this;
  }

  //endregion REventSource

  //region RFrequency

  @NonNull
  @Override
  public RexCompiler onUpdatesPer(int millis) {
    checkCompilingRepository();
    checkExpect(FREQUENCY_OR_MORE_EVENT_SOURCE);
    frequency = Math.max(0, millis);
    expect = FLOW;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler onUpdatesPerLoop() {
    return onUpdatesPer(0);
  }

  //endregion RFrequency

  //region RSyncFlow

  @NonNull
  @Override
  public RexCompiler getFrom(@NonNull final Supplier supplier) {
    // available to both rexes
    checkExpect(FLOW);
    addGetFrom(supplier, directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler mergeIn(@NonNull final Supplier supplier, @NonNull final Merger merger) {
    // available to both rexes
    checkExpect(FLOW);
    addMergeIn(supplier, merger, directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler transform(@NonNull final Function function) {
    // available to both rexes
    checkExpect(FLOW);
    addTransform(function, directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler check(@NonNull final Predicate predicate) {
    return check(identityFunction(), predicate);
  }

  @NonNull
  @Override
  public RexCompiler check(@NonNull final Function function, @NonNull final Predicate predicate) {
    // available to both rexes
    checkExpect(FLOW);
    caseExtractor = checkNotNull(function);
    casePredicate = checkNotNull(predicate);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler sendTo(@NonNull final Receiver receiver) {
    // available to both rexes
    checkExpect(FLOW);
    addSendTo(checkNotNull(receiver), directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler bindWith(@NonNull final Supplier secondValueSupplier,
      @NonNull final Binder binder) {
    // available to both rexes
    checkExpect(FLOW);
    addBindWith(secondValueSupplier, binder, directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenSkip() {
    // available to both rexes
    endFlow(true);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenEnd() {
    checkCompilingReaction();
    transform(REACTION_TRIGGER_STATIC_FUNCTION);
    endFlow(false);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenGetFrom(@NonNull final Supplier supplier) {
    checkCompilingRepository();
    getFrom(supplier);
    endFlow(false);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenMergeIn(@NonNull final Supplier supplier, @NonNull final Merger merger) {
    checkCompilingRepository();
    mergeIn(supplier, merger);
    endFlow(false);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenTransform(@NonNull final Function function) {
    checkCompilingRepository();
    transform(function);
    endFlow(false);
    return this;
  }

  private void endFlow(boolean skip) {
    addEnd(skip, directives);
    expect = CONFIG;
  }

  @NonNull
  @Override
  public RexCompiler attemptGetFrom(@NonNull final Supplier attemptSupplier) {
    // available to both rexes
    getFrom(attemptSupplier);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler attemptMergeIn(
      @NonNull final Supplier supplier, @NonNull final Merger attemptMerger) {
    // available to both rexes
    mergeIn(supplier, attemptMerger);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler attemptTransform(@NonNull final Function attemptFunction) {
    // available to both rexes
    transform(attemptFunction);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenAttemptGetFrom(@NonNull final Supplier attemptSupplier) {
    checkCompilingRepository();
    getFrom(attemptSupplier);
    expect = TERMINATE_THEN_END;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenAttemptMergeIn(
      @NonNull final Supplier supplier, @NonNull final Merger attemptMerger) {
    checkCompilingRepository();
    mergeIn(supplier, attemptMerger);
    expect = TERMINATE_THEN_END;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler thenAttemptTransform(@NonNull final Function attemptFunction) {
    checkCompilingRepository();
    transform(attemptFunction);
    expect = TERMINATE_THEN_END;
    return this;
  }

  //endregion RSyncFlow

  //region RFlow

  @NonNull
  @Override
  public RexCompiler goTo(@NonNull final Executor executor) {
    return goTo(executor, returnSecond());
  }

  @NonNull
  @Override
  public RexCompiler goTo(@NonNull final Executor executor,
      @NonNull final Merger runnableDecorator) {
    // available to both rexes
    checkExpect(FLOW);
    checkGoLazyUnused();
    addGoTo(checkNotNull(executor), checkNotNull(runnableDecorator), directives);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler goLazy() {
    checkCompilingRepository();
    checkExpect(FLOW);
    checkGoLazyUnused();
    addGoLazy(directives);
    goLazyUsed = true;
    return this;
  }

  //endregion RFlow

  //region RTermination

  @NonNull
  @Override
  public RexCompiler orSkip() {
    // available to both rexes
    terminate(null);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler orEnd() {
    checkCompilingReaction();
    terminate(REACTION_TRIGGER_STATIC_FUNCTION);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler orEnd(@NonNull final Function valueFunction) {
    checkCompilingRepository();
    terminate(valueFunction);
    return this;
  }

  private void terminate(@Nullable final Function valueFunction) {
    checkExpect(TERMINATE_THEN_FLOW, TERMINATE_THEN_END);
    if (caseExtractor != null) {
      addCheck(caseExtractor, checkNotNull(casePredicate), valueFunction, directives);
    } else {
      addFilterSuccess(valueFunction, directives);
    }
    caseExtractor = null;
    casePredicate = null;
    if (expect == TERMINATE_THEN_END) {
      endFlow(false);
    } else {
      expect = FLOW;
    }
  }

  //endregion RTermination

  //region RConfig

  @NonNull
  @Override
  public RexCompiler notifyIf(@NonNull final Merger notifyChecker) {
    checkCompilingRepository();
    checkExpect(CONFIG);
    this.notifyChecker = checkNotNull(notifyChecker);
    return this;
  }

  @NonNull
  @Override
  public RexCompiler onDeactivation(@RexConfig final int deactivationConfig) {
    // available to both rexes
    checkExpect(CONFIG);
    this.deactivationConfig = deactivationConfig;
    return this;
  }

  @NonNull
  @Override
  public RexCompiler onConcurrentUpdate(@RexConfig final int concurrentUpdateConfig) {
    // available to both rexes
    checkExpect(CONFIG);
    this.concurrentUpdateConfig = concurrentUpdateConfig;
    return this;
  }

  @NonNull
  @Override
  public Object compile() {
    // available to both rexes
    Object rex = compileRexAndReset();
    recycle(this);
    return rex;
  }

  @NonNull
  @Override
  public RexCompiler compileIntoRepositoryWithInitialValue(@NonNull final Object value) {
    checkCompilingRepository();
    Repository repository = (Repository) compileRexAndReset();
    // Don't recycle, instead sneak in the first directive and start the second repository
    addGetFrom(repository, directives);
    return start(value).observe(repository);
  }

  @NonNull
  private Object compileRexAndReset() {
    checkExpect(CONFIG);
    Object rex;
    if (reservoirForReaction == null) {
      rex = compiledRepository(initialValue, eventSources, frequency, directives, notifyChecker,
          concurrentUpdateConfig, deactivationConfig);
    } else {
      // No need to remove RESET_TO_INITIAL_VALUE bit from the config flags. It is ineligible for a
      // Reaction, but resetting will never happen on concurrent update, and when it happens on
      // deactivation no one will be listening.
      rex = compiledReaction(initialValue, reservoirForReaction, directives,
          concurrentUpdateConfig, deactivationConfig);
    }
    expect = NOTHING;
    initialValue = null;
    eventSources.clear();
    frequency = 0;
    directives.clear();
    goLazyUsed = false;
    notifyChecker = objectsUnequal();
    deactivationConfig = RexConfig.CONTINUE_FLOW;
    concurrentUpdateConfig = RexConfig.CONTINUE_FLOW;
    reservoirForReaction = null;
    return rex;
  }

  //endregion RConfig
}
