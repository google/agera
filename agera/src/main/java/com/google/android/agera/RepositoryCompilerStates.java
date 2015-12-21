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

import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Container of the compiler state interfaces supporting the declaration of {@link Repository}s
 * using the type-safe declarative language.
 *
 * <h3>Overview</h3>
 *
 * The declaration of a compiled repository starts with specifying the <i>initial value</i> with
 * {@link Repositories#repositoryWithInitialValue}, ends with calling {@link RConfig#compile()}, and
 * consists of the following sections in between:
 * <ul>
 * <li>Event sources - {@code observe()};
 * <li>Frequency of reaction - {@code onUpdatesPer()}, {@code onUpdatesPerLoop()};
 * <li>Data processing flow - {@code getFrom()}, {@code mergeIn()}, {@code transform()} etc.;
 * <li>Miscellaneous configurations - {@code onDeactivation()}, {@code onConcurrentUpdate()} etc.
 * </ul>
 *
 * The repository observes the given event sources at the given frequency, and updates its value in
 * response to the event source updates using the data processing flow. The flow is run when the
 * repository is activated (transitions from not observed to observed), and when any event source
 * notifies of updates while the repository is active. When the flow updates the repository value,
 * the client {@link Updatable}s are notified.
 *
 * <h3>Data processing flow</h3>
 *
 * The data processing flow consists of <i>directive</i>s. Each directive accepts an input value and
 * produces an output value for the next directive. The input value type to the first directive is
 * the repository value type, and so is the output value type of the last directive, one that starts
 * with {@code then}. The compiler state interfaces help guarantee type safety using generic type
 * parameters, with input type contravariance (the next directive can accept a supertype of what the
 * previous directive produces) and output type covariance (the last directive can produce a subtype
 * of the repository value type) whenever possible.
 *
 * <p>When the data processing flow is run, the current repository value exposed through
 * {@link Repository#get()} is used as the input value to the first directive. This may be the
 * repository's initial value if the flow has not updated the value before, or the repository was
 * {@linkplain RexConfig#RESET_TO_INITIAL_VALUE reset}. The directives are run sequentially to
 * transform this input value. The data processing flow <i>ends</i> normally after a {@code then}
 * directive that produces a final value is run, or when a <i>termination clause</i> is run which
 * {@code end}s the flow early with a value, in which case the repository value is updated and the
 * registered {@link Updatable}s are notified. The flow can also be terminated abruptly if a
 * {@code thenSkip()} directive is run or a termination clause is run which {@code skip}s the rest
 * of the flow, in which case the repository <i>skips</i> updating its value and notifying of such
 * update.
 *
 * <h3>Asynchronous programming</h3>
 *
 * The repository must be compiled on a {@link Looper} thread (typically the main thread). The
 * looper becomes the <i>worker looper</i> of this repository, and all these processes are run from
 * that looper thread: client {@link Updatable} registration and de-registration; processing and
 * low-pass filtering (if using {@link RFrequency#onUpdatesPer onUpdatesPer(millis)}) the event
 * source updates, and starting the data processing flow.
 *
 * <p>The data processing flow is not required to complete synchronously on the looper thread.
 * Special directives {@code goTo()} and {@code goLazy()} enable asynchronous programming. They do
 * not change the input value; they control the continuation of the flow at runtime:
 * {@code goTo(executor)} sends the remainder of the execution onto the {@link Executor}, and
 * {@code goLazy()} pauses the execution until {@link Repository#get()} is called.
 *
 * <p>After a {@code goTo()} directive frees up the worker looper thread to process other events,
 * the repository may be concurrently deactivated (becoming unobserved) or notified of updates from
 * its event sources. In the latter case, a data processing flow re-run is scheduled but not started
 * in parallel with the ongoing flow, to reduce race conditions. The repository can be configured to
 * cancel the ongoing flow on deactivation and on concurrent update. This helps preserve resources
 * (in case of deactivation) and start the re-run sooner (in case of concurrent updates). A
 * cancelled flow is suppressed from changing the repository value or notifying of any updates.
 * Cancellation behaviors are configurable with {@link RConfig#onDeactivation} and
 * {@link RConfig#onConcurrentUpdate}.
 *
 * <p>At the {@code goLazy()} directive, the registered {@link Updatable}s are notified in case of
 * any update, but it is up to the remaining directives whether to actually update the repository
 * value. The flow resumes synchronously on the thread from which {@link Repository#get()} is called
 * and, because {@code get()} must produce a value, any cancellation signal is ignored from this
 * point on. On the other hand, if the repository receives an update from its event sources before
 * {@code get()} resumes the paused flow, the paused state and the saved intermediate value is
 * discarded, the remaining directives are never run, and the flow restarts immediately. Calling
 * {@code get()} after the flow restarts and before it arrives at {@code goLazy()} again returns the
 * previous repository value. Because of the last point, using {@code goLazy()} strategically can
 * improve program performance.
 *
 * <h3>List of directives</h3>
 *
 * <b>Variables:</b> s: supplier; fs: fallible supplier; m: merger; fm: fallible merger;
 * f: function; ff: fallible function; p: predicate; r: receiver; b: binder; e: executor; v: value.
 * <ul>
 *   <li>({@link RFlow#thenGetFrom then}){@link RFlow#getFrom GetFrom(s)}
 *   <li>({@link RFlow#thenMergeIn then}){@link RFlow#mergeIn MergeIn(s, m)}
 *   <li>({@link RFlow#thenTransform then}){@link RFlow#transform Transform(f)}
 *   <li>({@link RFlow#thenAttemptGetFrom then}){@link
 *       RFlow#attemptGetFrom AttemptGetFrom(fs)}.<i>term</i>
 *   <li>({@link RFlow#thenAttemptMergeIn then}){@link
 *       RFlow#attemptMergeIn AttemptMergeIn(s, fm)}.<i>term</i>
 *   <li>({@link RFlow#thenAttemptTransform then}){@link
 *       RFlow#attemptTransform AttemptTransform(ff)}.<i>term</i>
 *   <li>{@link RFlow#check(Function) check(p)}.<i>term</i>
 *   <li>{@link RFlow#check(Function, Function) check(f, p)}.<i>term</i>
 *   <li>{@link RFlow#sendTo sendTo(r)}
 *   <li>{@link RFlow#bindWith bindWith(s, b)}
 *   <li>{@link RFlow#goTo goTo(e)}
 *   <li>{@link RFlow#goLazy goLazy()}
 *   <li>{@link RFlow#thenSkip thenSkip()}
 * </ul>
 * where <i>term</i> (the termination clause) is one of:
 * <ul>
 *   <li>{@link RTermination#orSkip orSkip()}
 *   <li>{@link RTermination#orEnd orEnd(f)}
 * </ul>
 *
 * <h3>Tips</h3>
 *
 * <b>Efficiency.</b> No two directives will be run at the same time and all thread synchronization
 * regarding the intermediate values are handled by the data processing flow runner. For performance
 * and memory efficiency reasons, it is encouraged that the intermediary {@link Function}s and
 * {@link Merger}s reuse the input objects whenever appropriate (for example, instead of returning
 * a new {@code List} in response to an input {@code List}, try taking in a mutable
 * {@code ArrayList}, mutating it, and outputting the same object; in other words, the operators are
 * explicitly allowed to be un-pure), and let the final directive(s) produce a preferably immutable
 * model object, or in general, an object that is detached from any intermediate values, therefore
 * safe to be used as the exposed value of the repository.
 */
public interface RepositoryCompilerStates extends RexCompilerStates {

  /**
   * Compiler state allowing to specify the event source of the repository.
   *
   * @param <TVal> Value type of the repository.
   * @param <TStart> Value type at the start of the data processing flow. May be different from
   *     {@code TVal} when chain-building a repository that starts with
   *     {@link RConfig#compileIntoRepositoryWithInitialValue}.
   */
  interface REventSource<TVal, TStart> {

    /**
     * Specifies the event source of the compiled repository.
     */
    @NonNull
    RFrequency<TVal, TStart> observe(@NonNull Observable... observables);
  }

  /**
   * Compiler state allowing to specify the frequency of invoking the data processing flow.
   *
   * @param <TVal> Value type of the repository.
   * @param <TStart> Value type at the start of the data processing flow.
   */
  interface RFrequency<TVal, TStart> extends REventSource<TVal, TStart> {

    /**
     * Specifies the minimum timeout to wait since starting the previous data processing flow,
     * before starting another flow to respond to updates from the event sources. Flows will not be
     * started more frequent than if {@link #onUpdatesPerLoop()} were used, even if the given
     * timeout is sufficiently small.
     */
    @NonNull
    RFlow<TVal, TStart, RConfig<TVal, Repository<TVal>, ?>, ?> onUpdatesPer(int millis);

    /**
     * Specifies that multiple updates from the event sources per worker looper loop should start
     * only one data processing flow.
     */
    @NonNull
    RFlow<TVal, TStart, RConfig<TVal, Repository<TVal>, ?>, ?> onUpdatesPerLoop();
  }

  /**
   * Compiler state allowing to specify the next directive of the data processing flow.
   *
   * @param <TVal> Value type of the repository.
   * @param <TPre> The output value type of the previous directive.
   */
  interface RFlow<TVal, TPre, TCfg extends RConfig<TVal, Repository<TVal>, ?>,
          TSelf extends RFlow<TVal, TPre, TCfg, TSelf>>
      extends RSyncFlow<TVal, TPre, TCfg, TSelf>, RFlowBase<TPre, TCfg, TSelf> {
    // Methods whose return types need subtyping (due to no "generic type of generic type" in Java):

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, TCfg, ?> getFrom(@NonNull Supplier<TCur> supplier);

    @NonNull
    @Override
    <TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    @NonNull
    @Override
    <TAdd, TCur> RFlow<TVal, TCur, TCfg, ?> mergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, TCur> merger);

    @NonNull
    @Override
    <TAdd, TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, TCfg, ?> transform(@NonNull Function<? super TPre, TCur> function);

    @NonNull
    @Override
    <TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    // For Repositories only:

    /**
     * Suspend the data processing flow and notify the registered {@link Updatable}s of updates.
     * The remaining of the flow will be run synchronously <i>and uninterruptibly</i> the first time
     * {@link Repository#get()} is called, to produce the new repository value lazily. After this
     * directive, {@link #goTo(Executor)} is no longer available, and all further operators should
     * be fairly lightweight in order not to block the callers of {@code get()} for too long.
     */
    @NonNull
    RSyncFlow<TVal, TPre, TCfg, ?> goLazy();
  }

  /**
   * Compiler state allowing to specify the final synchronous steps of the data processing flow.
   *
   * @param <TVal> Value type of the repository.
   * @param <TPre> The output value type of the previous directive.
   * @param <TCfg> RConfig compiler state type; for Java compiler type inference only.
   * @param <TSelf> Self-type; for Java compiler type inference only.
   */
  interface RSyncFlow<TVal, TPre, TCfg extends RConfig<TVal, Repository<TVal>, ?>,
      TSelf extends RSyncFlow<TVal, TPre, TCfg, TSelf>> extends RSyncFlowBase<TPre, TCfg, TSelf> {
    // Methods whose return types need subtyping (due to no "generic type of generic type" in Java):

    @NonNull
    @Override
    <TCur> RSyncFlow<TVal, TCur, TCfg, ?> getFrom(@NonNull Supplier<TCur> supplier);

    @NonNull
    @Override
    <TCur>
    RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, TCfg, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    @NonNull
    @Override
    <TAdd, TCur> RSyncFlow<TVal, TCur, TCfg, ?> mergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, TCur> merger);

    @NonNull
    @Override
    <TAdd, TCur>
    RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, TCfg, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    @NonNull
    @Override
    <TCur> RSyncFlow<TVal, TCur, TCfg, ?> transform(@NonNull Function<? super TPre, TCur> function);

    @NonNull
    @Override
    <TCur> RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, TCfg, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    @NonNull
    @Override
    RTermination<TVal, TPre, TSelf> check(@NonNull Function<? super TPre, Boolean> predicate);

    @NonNull
    @Override
    <TCase> RTermination<TVal, TCase, TSelf> check(
        @NonNull Function<? super TPre, TCase> caseFunction,
        @NonNull Function<? super TCase, Boolean> casePredicate);

    // For Repositories only:

    /**
     * Perform the {@link #getFrom} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    RConfig<TVal, Repository<TVal>, ?> thenGetFrom(@NonNull Supplier<? extends TVal> supplier);

    /**
     * Perform the {@link #attemptGetFrom} directive and use the successful output value as the new
     * value of the compiled repository, with notification if necessary.
     */
    @NonNull
    RTermination<TVal, Throwable, RConfig<TVal, Repository<TVal>, ?>> thenAttemptGetFrom(
            @NonNull Supplier<? extends Result<? extends TVal>> attemptSupplier);

    /**
     * Perform the {@link #mergeIn} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    <TAdd> RConfig<TVal, Repository<TVal>, ?> thenMergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, ? extends TVal> merger);

    /**
     * Perform the {@link #attemptMergeIn} directive and use the successful output value as the new
     * value of the compiled repository, with notification if necessary.
     */
    @NonNull
    <TAdd> RTermination<TVal, Throwable, RConfig<TVal, Repository<TVal>, ?>> thenAttemptMergeIn(
            @NonNull Supplier<TAdd> supplier,
            @NonNull Merger<? super TPre, ? super TAdd,
                ? extends Result<? extends TVal>> attemptMerger);

    /**
     * Perform the {@link #transform} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    RConfig<TVal, Repository<TVal>, ?> thenTransform(
        @NonNull Function<? super TPre, ? extends TVal> function);

    /**
     * Perform the {@link #attemptTransform} directive and use the successful output value as the
     * new value of the compiled repository, with notification if necessary.
     */
    @NonNull
    RTermination<TVal, Throwable, RConfig<TVal, Repository<TVal>, ?>> thenAttemptTransform(
            @NonNull Function<? super TPre, ? extends Result<? extends TVal>> attemptFunction);
  }

  /**
   * Compiler state allowing to terminate the data processing flow following a failed check.
   *
   * @param <TVal> Value type of the repository.
   * @param <TTerm> Value type from which to terminate the flow.
   * @param <TRet> Compiler state to return to.
   */
  interface RTermination<TVal, TTerm, TRet> extends RTerminationBase<TTerm, TRet> {
    // For Repositories only:

    /**
     * If the previous check failed, terminate the data processing flow and update the compiled
     * repository's value to the resulting value of applying the given function to the input of this
     * termination clause, with notification if necessary.
     */
    @NonNull
    TRet orEnd(@NonNull Function<? super TTerm, ? extends TVal> valueFunction);
  }

  /**
   * Compiler state allowing to configure and end the declaration of the repository.
   *
   * @param <TVal> Repository value type.
   * @param <TRex> Repository type; for Java compiler type inference only.
   * @param <TSelf> Self-type; for Java compiler type inference only.
   */
  interface RConfig<TVal, TRex extends Repository<TVal>, TSelf extends RConfig<TVal, TRex, TSelf>>
      extends RexCompilerStates.RConfig<TVal, TRex, TSelf> {
    // For Repositories only:

    /**
     * Specifies that this repository should notify the registered {@link Updatable}s if and only if
     * the given {@code checker} returns {@link Boolean#TRUE}. Every time the data processing flow
     * ends with a new repository value, the checker is called with the old repository value as the
     * first argument and the new value the second. The return value determines whether this update
     * should generate a notification. The default behavior is to notify of the update when the new
     * value is different as per {@link Object#equals}.
     *
     * <p>Note that the {@code goLazy()} directive will always generate a notification, as a
     * preventative measure to handle a potentially different value which is unknown at the time of
     * {@code goLazy()}. Also, technically the {@link RexConfig#RESET_TO_INITIAL_VALUE} deactivation
     * configuration would also update the repository value, and therefore the {@code checker} will
     * be consulted, but because the reset happens only when the repository is deactivated, even if
     * the checker returns true, there is no receiving {@link Updatable} of the notification.
     */
    @NonNull
    TSelf notifyIf(@NonNull Merger<? super TVal, ? super TVal, Boolean> checker);

    /**
     * Compiles a repository that exhibits the previously defined behaviors, and starts compiling
     * a new repository with the given initial value (which can be of a different type) that uses
     * the former repository as the first event source and the first data source.
     *
     * <p>This method provides a shortcut for the following code:
     *
     * <pre>{@literal
     * Repository<TVal> subRepository = ….compile();
     * Repository<TVal2> mainRepository = repositoryWithInitialValue(value)
     *     .observe(subRepository)
     *     .… // additional event sources and frequency which can be defined after this method
     *     .getFrom(subRepository) // first directive
     *     .… // rest of data processing flow, configuration, compile()
     * }</pre>
     *
     * The compiled repository ({@code subRepository}) therefore acts as a buffer, with its own
     * event sources and data processing flow, which simplifies or shortens the flow of the new
     * repository ({@code mainRepository}). This is typically useful if different parts of the
     * overall data processing flow depend on different event sources and data sources, and it is
     * beneficial to cache the intermediate values between parts.
     *
     * <p>However, due to the {@code getFrom} directive at the start of this new data processing
     * flow, it has no access to the previous value of this new repository. Additionally, the former
     * repository is not exposed anywhere else. If this is undesirable, consider using the full
     * form, where the former repository is explicitly compiled.
     */
    @NonNull
    <TVal2> RFrequency<TVal2, TVal> compileIntoRepositoryWithInitialValue(@NonNull TVal2 value);
  }
}
