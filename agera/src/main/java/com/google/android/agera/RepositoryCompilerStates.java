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

import android.support.annotation.NonNull;
import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Container of the compiler state interfaces supporting the declaration of {@link Repository}s
 * using the type-safe declarative language.
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
 *   <li>{@link RFlow#check(Predicate) check(p)}.<i>term</i>
 *   <li>{@link RFlow#check(Function, Predicate) check(f, p)}.<i>term</i>
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
 */
public interface RepositoryCompilerStates {

  // Note on documentation grammar: most method summaries for flow directives use an infinitive verb
  // phrase ("do something") instead of the usual 3rd-person grammar ("does something"). This is
  // because the full sentence for these method summaries are "this method specifies that the next
  // step of the flow should do something", rather than "this method does something".

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
    RFlow<TVal, TStart, ?> onUpdatesPer(int millis);

    /**
     * Specifies that multiple updates from the event sources per worker looper loop should start
     * only one data processing flow.
     */
    @NonNull
    RFlow<TVal, TStart, ?> onUpdatesPerLoop();
  }

  /**
   * Compiler state allowing to specify the next directive of the data processing flow.
   *
   * @param <TVal> Value type of the repository.
   * @param <TPre> The output value type of the previous directive.
   */
  interface RFlow<TVal, TPre, TSelf extends RFlow<TVal, TPre, TSelf>>
      extends RSyncFlow<TVal, TPre, TSelf> {
    // Methods whose return types need subtyping (due to no "generic type of generic type" in Java):

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, ?> getFrom(@NonNull Supplier<TCur> supplier);

    @NonNull
    @Override
    <TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    @NonNull
    @Override
    RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        RFlow<TVal, Throwable, ?>> thenAttemptGetFrom(
            @NonNull Supplier<? extends Result<? extends TVal>> attemptSupplier);

    @NonNull
    @Override
    <TAdd, TCur> RFlow<TVal, TCur, ?> mergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, TCur> merger);

    @NonNull
    @Override
    <TAdd, TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    @NonNull
    @Override
    <TAdd> RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        RFlow<TVal, Throwable, ?>> thenAttemptMergeIn(
            @NonNull Supplier<TAdd> supplier,
            @NonNull Merger<? super TPre, ? super TAdd,
                ? extends Result<? extends TVal>> attemptMerger);

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, ?> transform(@NonNull Function<? super TPre, TCur> function);

    @NonNull
    @Override
    <TCur> RTermination<TVal, Throwable, RFlow<TVal, TCur, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    @NonNull
    @Override
    RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        RFlow<TVal, Throwable, ?>> thenAttemptTransform(
            @NonNull Function<? super TPre, ? extends Result<? extends TVal>> attemptFunction);

    // Asynchronous directives:

    /**
     * Go to the given {@code executor} to continue the data processing flow. The executor is
     * assumed to never throw {@link RejectedExecutionException}. Synchronous executors are
     * supported but the risk of stack overflow will be higher. Note that when the executor resumes
     * the flow, the directives that follow are run sequentially within the same
     * {@link Runnable#run()} call, until the flow completes or the next {@code goTo()} or, if
     * applicable, {@code goLazy()} directive is reached. Depending on the directives and operators
     * used, this may starve the executor. If necessary, use additional {@code goTo()} directives
     * with the same executor to achieve fairness.
     */
    @NonNull
    TSelf goTo(@NonNull Executor executor);

    /**
     * Suspend the data processing flow and notify the registered {@link Updatable}s of updates.
     * The remaining of the flow will be run synchronously <i>and uninterruptibly</i> the first time
     * {@link Repository#get()} is called, to produce the new repository value lazily. After this
     * directive, {@link #goTo(Executor)} is no longer available, and all further operators should
     * be fairly lightweight in order not to block the callers of {@code get()} for too long.
     */
    @NonNull
    RSyncFlow<TVal, TPre, ?> goLazy();
  }

  /**
   * Compiler state allowing to specify the final synchronous steps of the data processing flow.
   *
   * @param <TVal> Value type of the repository.
   * @param <TPre> The output value type of the previous directive.
   * @param <TSelf> Self-type; for Java compiler type inference only.
   */
  interface RSyncFlow<TVal, TPre, TSelf extends RSyncFlow<TVal, TPre, TSelf>> {

    /**
     * Ignore the input value, and use the value newly obtained from the given supplier as the
     * output value.
     */
    @NonNull
    <TCur> RSyncFlow<TVal, TCur, ?> getFrom(@NonNull Supplier<TCur> supplier);

    /**
     * Like {@link #getFrom}, ignore the input value and attempt to get the new value from the given
     * supplier. If the attempt fails, terminate the data processing flow by sending the failure to
     * the termination clause that follows; otherwise take the successful value as the output of
     * this directive.
     */
    @NonNull
    <TCur>
    RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    /**
     * Take the input value and the value newly obtained from the given supplier, merge them using
     * the given merger, and use the resulting value as the output value.
     */
    @NonNull
    <TAdd, TCur> RSyncFlow<TVal, TCur, ?> mergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, TCur> merger);

    /**
     * Like {@link #mergeIn}, take the input value and the value newly obtained from the given
     * supplier, and attempt to merge them using the given merger. If the attempt fails, terminate
     * the data processing flow by sending the failure to the termination clause that follows;
     * otherwise take the successful value as the output of this directive.
     *
     * <p>This method is agnostic of the return type of the {@code supplier}. If it itself is
     * fallible, the {@code merger} is held responsible for processing the failure, which may choose
     * to pass the failure on as the result of the merge.
     */
    @NonNull
    <TAdd, TCur>
    RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    /**
     * Transform the input value using the given function into the output value.
     */
    @NonNull
    <TCur> RSyncFlow<TVal, TCur, ?> transform(@NonNull Function<? super TPre, TCur> function);

    /**
     * Like {@link #transform}, attempt to transform the input value using the given function. If
     * the attempt fails, terminate the data processing flow by sending the failure to the
     * termination clause that follows; otherwise take the successful value as the output of this
     * directive.
     */
    @NonNull
    <TCur> RTermination<TVal, Throwable, ? extends RSyncFlow<TVal, TCur, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    /**
     * Check the input value with the given predicate. If the predicate applies, continue the data
     * processing flow with the same value, otherwise terminate the flow with the termination clause
     * that follows. The termination clause takes the input value as its input.
     */
    @NonNull
    RTermination<TVal, TPre, TSelf> check(@NonNull Predicate<? super TPre> predicate);

    /**
     * Use the case-function to compute a case value out of the input value and check it with the
     * given predicate. If the predicate applies to the case value, continue the data processing
     * flow with the <i>input value</i>, otherwise terminate the flow with the termination clause
     * that follows. The termination clause takes the <i>case value</i> as its input.
     */
    @NonNull
    <TCase> RTermination<TVal, TCase, TSelf> check(
        @NonNull Function<? super TPre, TCase> caseFunction,
        @NonNull Predicate<? super TCase> casePredicate);

    /**
     * Send the input value to the given receiver, and then pass on the input value as the output of
     * this directive, not modifying it.
     *
     * <p>Typical uses of this directive include reporting progress and/or errors in the UI,
     * starting a side process, logging, profiling and debugging, etc. The {@link Receiver#accept}
     * method is called synchronously, which means its execution blocks the rest of the data
     * processing flow. If the flow is to cancel with {@linkplain RepositoryConfig#SEND_INTERRUPT
     * the interrupt signal}, the receiver may also see the signal.
     *
     * <p>The receiver does not have to use the input value, but if it does and it moves onto a
     * different thread for processing the input value, note that the data processing flow does not
     * guarantee value immutability or concurrent access for this receiver. For this reason, for a
     * UI-calling receiver invoked from a background thread, implementation should extract any
     * necessary data from the input value, and post the immutable form of it to the main thread for
     * the UI calls, so the UI modifications are main-thread-safe while the data processing flow can
     * continue concurrently.
     *
     * <p>Note that the blocking semantics of this directive should not be taken as the permission
     * to mutate the input in a way that affects the rest of the flow -- the appropriate directive
     * for that purpose is {@code transform}, with a function that returns the same input instance
     * after mutation.
     */
    @NonNull
    TSelf sendTo(@NonNull Receiver<? super TPre> receiver);

    /**
     * Send the input value and the value from the given supplier to the given binder, and then pass
     * on the input value as the output of this directive, not modifying it.
     *
     * <p>The same usage notes for {@link #sendTo} apply to this directive.
     */
    @NonNull
    <TAdd> TSelf bindWith(@NonNull Supplier<TAdd> secondValueSupplier,
        @NonNull Binder<? super TPre, ? super TAdd> binder);

    /**
     * End the data processing flow but without using the output value and without notifying the
     * registered {@link Updatable}s.
     */
    @NonNull
    RConfig<TVal> thenSkip();

    /**
     * Perform the {@link #getFrom} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    RConfig<TVal> thenGetFrom(@NonNull Supplier<? extends TVal> supplier);

    /**
     * Perform the {@link #attemptGetFrom} directive and use the successful output value as the new
     * value of the compiled repository, with notification if necessary. If the attempt fails,
     * either terminate the data processing flow, or continue onto the next directive for recovery,
     * depending on the clause that follows.
     */
    @NonNull
    RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        ? extends RSyncFlow<TVal, Throwable, ?>> thenAttemptGetFrom(
            @NonNull Supplier<? extends Result<? extends TVal>> attemptSupplier);

    /**
     * Perform the {@link #mergeIn} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    <TAdd> RConfig<TVal> thenMergeIn(@NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, ? extends TVal> merger);

    /**
     * Perform the {@link #attemptMergeIn} directive and use the successful output value as the new
     * value of the compiled repository, with notification if necessary. If the attempt fails,
     * either terminate the data processing flow, or continue onto the next directive for recovery,
     * depending on the clause that follows.
     */
    @NonNull
    <TAdd> RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        ? extends RSyncFlow<TVal, Throwable, ?>> thenAttemptMergeIn(
            @NonNull Supplier<TAdd> supplier,
            @NonNull Merger<? super TPre, ? super TAdd,
                ? extends Result<? extends TVal>> attemptMerger);

    /**
     * Perform the {@link #transform} directive and use the output value as the new value of the
     * compiled repository, with notification if necessary.
     */
    @NonNull
    RConfig<TVal> thenTransform(
        @NonNull Function<? super TPre, ? extends TVal> function);

    /**
     * Perform the {@link #attemptTransform} directive and use the successful output value as the
     * new value of the compiled repository, with notification if necessary. If the attempt fails,
     * either terminate the data processing flow, or continue onto the next directive for recovery,
     * depending on the clause that follows.
     */
    @NonNull
    RTerminationOrContinue<TVal, Throwable, RConfig<TVal>,
        ? extends RSyncFlow<TVal, Throwable, ?>> thenAttemptTransform(
            @NonNull Function<? super TPre, ? extends Result<? extends TVal>> attemptFunction);
  }

  /**
   * Compiler state allowing to terminate the data processing flow following a failed check.
   *
   * @param <TVal> Value type of the repository.
   * @param <TTerm> Value type from which to terminate the flow.
   * @param <TRet> Compiler state to return to.
   */
  interface RTermination<TVal, TTerm, TRet> {

    /**
     * If the previous check failed, skip the rest of the data processing flow, and do not notify
     * any registered {@link Updatable}s.
     */
    @NonNull
    TRet orSkip();

    /**
     * If the previous check failed, terminate the data processing flow and update the compiled
     * repository's value to the resulting value of applying the given function to the input of this
     * termination clause, with notification if necessary.
     */
    @NonNull
    TRet orEnd(@NonNull Function<? super TTerm, ? extends TVal> valueFunction);
  }

  /**
   * Compiler state allowing to terminate or continue the data processing flow following a failed
   * attempt to produce the new value of the repository.
   *
   * @param <TVal> Value type of the repository.
   * @param <TTerm> Value type from which to terminate the flow.
   * @param <TRet> Compiler state to return to if the flow is terminated.
   * @param <TCon> Compiler state to return to if the flow is to continue.
   */

  interface RTerminationOrContinue<TVal, TTerm, TRet, TCon>
      extends RTermination<TVal, TTerm, TRet> {

    /**
     * If the previous attempt failed, continue with the rest of the data processing flow, using the
     * {@linkplain Result#getFailure() failure} as the input value to the next directive. Otherwise,
     * end the data processing flow and use the successful output value from the attempt as the new
     * value of the compiled repository, with notification if necessary.
     */
    @NonNull
    TCon orContinue();
  }

  /**
   * Compiler state allowing to configure and end the declaration of the repository.
   *
   * @param <TVal> Repository value type.
   */
  interface RConfig<TVal> {

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
     * {@code goLazy()}. Also, technically the {@link RepositoryConfig#RESET_TO_INITIAL_VALUE}
     * deactivation configuration would also update the repository value, and therefore the
     * {@code checker} will be consulted, but because the reset happens only when the repository is
     * deactivated, even if the checker returns true, there is no {@link Updatable} to receive the
     * notification.
     */
    @NonNull
    RConfig<TVal> notifyIf(@NonNull Merger<? super TVal, ? super TVal, Boolean> checker);

    /**
     * Specifies the behaviors when this repository is deactivated, i.e. from being observed to not
     * being observed. The default behavior is {@link RepositoryConfig#CONTINUE_FLOW}.
     *
     * @param deactivationConfig A bitwise combination of the constants in {@link RepositoryConfig}.
     */
    @NonNull
    RConfig<TVal> onDeactivation(@RepositoryConfig int deactivationConfig);

    /**
     * Specifies the behaviors when an update is observed from an event source while a data
     * processing flow is ongoing. The default behavior is {@link RepositoryConfig#CONTINUE_FLOW}.
     *
     * @param concurrentUpdateConfig A bitwise combination of the constants in
     *     {@link RepositoryConfig}.
     */
    @NonNull
    RConfig<TVal> onConcurrentUpdate(@RepositoryConfig int concurrentUpdateConfig);

    /**
     * Specifies a {@code disposer} to handle any intermediate values discarded from the data
     * processing flow. Intermediate values may be discarded due to the rest of the flow being
     * skipped by a skip directive or a termination clause ({@code thenSkip()}, {@code orSkip()} and
     * {@code orEnd()}), or cancelled in compliance with the deactivation or concurrent update
     * configuration. The disposer may want to check if the values should be disposed of in a proper
     * way, such as calling {@link Closeable#close()} on all {@link Closeable} values.
     *
     * <p>In each occasion where an intermediate value is discarded, the disposer will receive the
     * value computed by the last completed directive. If the last directive was an attempt that
     * failed, the disposer will receive the failed {@link Result}.
     *
     * <p>The compiled repository will only avoid sending the current repository value (accessible
     * from the {@link Repository#get()} method) to the disposer, if it happens to be {@code ==}
     * (reference-equal) to the intermediate value being discarded. This ensures that the value,
     * still exposed through {@link Repository#get()}, is not incorrectly disposed. However, no
     * similar attempt will be made for the repository's initial value. If the initial value is
     * disposable and {@link RepositoryConfig#RESET_TO_INITIAL_VALUE} is used, this may lead to
     * incorrect disposal of the initial value. The disposer should take care of this case
     * explicitly, or the repository should be designed to only publish values that need not be
     * disposed of.
     *
     * @param disposer The {@link Receiver} to receive the discarded intermediate values.
     */
    @NonNull
    RConfig<TVal> sendDiscardedValuesTo(@NonNull Receiver<Object> disposer);

    /**
     * Compiles a {@link Repository} that exhibits the previously defined behaviors.
     */
    @NonNull
    Repository<TVal> compile();

    /**
     * Compiles a repository that exhibits the previously defined behaviors, and starts compiling
     * a new repository with the given initial value (which can be of a different type) that uses
     * the former repository as the first event source and the first data source.
     *
     * <p>This method provides a shortcut for the following code:
     *
     * <pre>
     * {@literal Repository<TVal>} subRepository = ….compile();
     * {@literal Repository<TVal2>} mainRepository = repositoryWithInitialValue(value)
     *     .observe(subRepository)
     *     .… // additional event sources and frequency which can be defined after this method
     *     .getFrom(subRepository) // first directive
     *     .… // rest of data processing flow, configuration, compile()
     * }</pre>
     *
     * The repository compiled by this method (the {@code subRepository}) therefore acts as a
     * buffer for the next repository to compile (the {@code mainRepository}), with its own event
     * sources and data processing flow. This simplifies or shortens the flow of the new repository,
     * and is typically useful if different parts of the overall data processing flow depend on
     * different event sources and data sources, and it is beneficial to cache the intermediate
     * values between parts.
     *
     * <p>However, due to the {@code getFrom} directive at the start of this new data processing
     * flow, the next repository to compile has no access to its previous value. Additionally, the
     * former repository is not exposed anywhere else. If this is undesirable, consider using the
     * full form, where the former repository is explicitly compiled.
     */
    @NonNull
    <TVal2> RFrequency<TVal2, TVal> compileIntoRepositoryWithInitialValue(@NonNull TVal2 value);
  }
}
