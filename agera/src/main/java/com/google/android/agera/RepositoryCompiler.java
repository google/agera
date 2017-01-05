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

import static com.google.android.agera.Common.NULL_OPERATOR;
import static com.google.android.agera.CompiledRepository.addBindWith;
import static com.google.android.agera.CompiledRepository.addCheck;
import static com.google.android.agera.CompiledRepository.addEnd;
import static com.google.android.agera.CompiledRepository.addFilterFailure;
import static com.google.android.agera.CompiledRepository.addFilterSuccess;
import static com.google.android.agera.CompiledRepository.addGetFrom;
import static com.google.android.agera.CompiledRepository.addGoLazy;
import static com.google.android.agera.CompiledRepository.addGoTo;
import static com.google.android.agera.CompiledRepository.addMergeIn;
import static com.google.android.agera.CompiledRepository.addSendTo;
import static com.google.android.agera.CompiledRepository.addTransform;
import static com.google.android.agera.CompiledRepository.compiledRepository;
import static com.google.android.agera.Functions.identityFunction;
import static com.google.android.agera.Mergers.objectsUnequal;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.concurrent.Executor;

@SuppressWarnings({"unchecked, rawtypes"})
final class RepositoryCompiler implements
    RepositoryCompilerStates.RFrequency,
    RepositoryCompilerStates.RFlow,
    RepositoryCompilerStates.RTerminationOrContinue,
    RepositoryCompilerStates.RConfig {

  private static final ThreadLocal<RepositoryCompiler> compilers = new ThreadLocal<>();

  @NonNull
  static <TVal> RepositoryCompilerStates.REventSource<TVal, TVal> repositoryWithInitialValue(
      @NonNull final TVal initialValue) {
    checkNotNull(Looper.myLooper());
    RepositoryCompiler compiler = compilers.get();
    if (compiler == null) {
      compiler = new RepositoryCompiler();
    } else {
      // Remove compiler from the ThreadLocal to prevent reuse in the middle of a compilation.
      // recycle(), called by compile(), will return the compiler here. ThreadLocal.set(null) keeps
      // the entry (with a null value) whereas remove() removes the entry; because we expect the
      // return of the compiler, don't use the heavier remove().
      compilers.set(null);
    }
    return compiler.start(initialValue);
  }

  private static void recycle(@NonNull final RepositoryCompiler compiler) {
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
  @RepositoryConfig
  private int deactivationConfig;
  @RepositoryConfig
  private int concurrentUpdateConfig;
  @NonNull
  private Receiver discardedValueDisposer = NULL_OPERATOR;

  @Expect
  private int expect;

  private RepositoryCompiler() {}

  @NonNull
  private RepositoryCompiler start(@NonNull final Object initialValue) {
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

  private void checkGoLazyUnused() {
    checkState(!goLazyUsed, "Unexpected occurrence of async directive after goLazy()");
  }

  //region REventSource

  @NonNull
  @Override
  public RepositoryCompiler observe(@NonNull final Observable... observables) {
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
  public RepositoryCompiler onUpdatesPer(int millis) {
    checkExpect(FREQUENCY_OR_MORE_EVENT_SOURCE);
    frequency = Math.max(0, millis);
    expect = FLOW;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler onUpdatesPerLoop() {
    return onUpdatesPer(0);
  }

  //endregion RFrequency

  //region RSyncFlow

  @NonNull
  @Override
  public RepositoryCompiler getFrom(@NonNull final Supplier supplier) {
    checkExpect(FLOW);
    addGetFrom(supplier, directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler mergeIn(@NonNull final Supplier supplier,
      @NonNull final Merger merger) {
    checkExpect(FLOW);
    addMergeIn(supplier, merger, directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler transform(@NonNull final Function function) {
    checkExpect(FLOW);
    addTransform(function, directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler check(@NonNull final Predicate predicate) {
    return check(identityFunction(), predicate);
  }

  @NonNull
  @Override
  public RepositoryCompiler check(
      @NonNull final Function function, @NonNull final Predicate predicate) {
    checkExpect(FLOW);
    caseExtractor = checkNotNull(function);
    casePredicate = checkNotNull(predicate);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler sendTo(@NonNull final Receiver receiver) {
    checkExpect(FLOW);
    addSendTo(checkNotNull(receiver), directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler bindWith(@NonNull final Supplier secondValueSupplier,
      @NonNull final Binder binder) {
    checkExpect(FLOW);
    addBindWith(secondValueSupplier, binder, directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenSkip() {
    endFlow(true);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenGetFrom(@NonNull final Supplier supplier) {
    getFrom(supplier);
    endFlow(false);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenMergeIn(
      @NonNull final Supplier supplier, @NonNull final Merger merger) {
    mergeIn(supplier, merger);
    endFlow(false);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenTransform(@NonNull final Function function) {
    transform(function);
    endFlow(false);
    return this;
  }

  private void endFlow(final boolean skip) {
    addEnd(skip, directives);
    expect = CONFIG;
  }

  @NonNull
  @Override
  public RepositoryCompiler attemptGetFrom(@NonNull final Supplier attemptSupplier) {
    getFrom(attemptSupplier);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler attemptMergeIn(
      @NonNull final Supplier supplier, @NonNull final Merger attemptMerger) {
    mergeIn(supplier, attemptMerger);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler attemptTransform(@NonNull final Function attemptFunction) {
    transform(attemptFunction);
    expect = TERMINATE_THEN_FLOW;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenAttemptGetFrom(@NonNull final Supplier attemptSupplier) {
    getFrom(attemptSupplier);
    expect = TERMINATE_THEN_END;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenAttemptMergeIn(
      @NonNull final Supplier supplier, @NonNull final Merger attemptMerger) {
    mergeIn(supplier, attemptMerger);
    expect = TERMINATE_THEN_END;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler thenAttemptTransform(@NonNull final Function attemptFunction) {
    transform(attemptFunction);
    expect = TERMINATE_THEN_END;
    return this;
  }

  //endregion RSyncFlow

  //region RFlow

  @NonNull
  @Override
  public RepositoryCompiler goTo(@NonNull final Executor executor) {
    checkExpect(FLOW);
    checkGoLazyUnused();
    addGoTo(executor, directives);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler goLazy() {
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
  public RepositoryCompiler orSkip() {
    terminate(null);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler orEnd(@NonNull final Function valueFunction) {
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

  @NonNull
  @Override
  public RepositoryCompiler orContinue() {
    checkExpect(TERMINATE_THEN_END);
    addFilterFailure(directives);
    expect = FLOW;
    return this;
  }

  //endregion RTermination

  //region RConfig

  @NonNull
  @Override
  public RepositoryCompiler notifyIf(@NonNull final Merger notifyChecker) {
    checkExpect(CONFIG);
    this.notifyChecker = checkNotNull(notifyChecker);
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler onDeactivation(@RepositoryConfig final int deactivationConfig) {
    checkExpect(CONFIG);
    this.deactivationConfig = deactivationConfig;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler onConcurrentUpdate(@RepositoryConfig final int concurrentUpdateConfig) {
    checkExpect(CONFIG);
    this.concurrentUpdateConfig = concurrentUpdateConfig;
    return this;
  }

  @NonNull
  @Override
  public RepositoryCompiler sendDiscardedValuesTo(@NonNull final Receiver disposer) {
    checkExpect(CONFIG);
    discardedValueDisposer = checkNotNull(disposer);
    return this;
  }

  @NonNull
  @Override
  public Repository compile() {
    Repository repository = compileRepositoryAndReset();
    recycle(this);
    return repository;
  }

  @NonNull
  @Override
  public RepositoryCompiler compileIntoRepositoryWithInitialValue(@NonNull final Object value) {
    Repository repository = compileRepositoryAndReset();
    // Don't recycle, instead sneak in the first directive and start the second repository
    addGetFrom(repository, directives);
    return start(value).observe(repository);
  }

  @NonNull
  private Repository compileRepositoryAndReset() {
    checkExpect(CONFIG);
    Repository repository = compiledRepository(initialValue, eventSources, frequency, directives,
        notifyChecker, concurrentUpdateConfig, deactivationConfig, discardedValueDisposer);
    expect = NOTHING;
    initialValue = null;
    eventSources.clear();
    frequency = 0;
    directives.clear();
    goLazyUsed = false;
    notifyChecker = objectsUnequal();
    deactivationConfig = RepositoryConfig.CONTINUE_FLOW;
    concurrentUpdateConfig = RepositoryConfig.CONTINUE_FLOW;
    discardedValueDisposer = NULL_OPERATOR;
    return repository;
  }

  //endregion RConfig
}
