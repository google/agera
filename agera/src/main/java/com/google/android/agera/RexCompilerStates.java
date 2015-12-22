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

import java.util.concurrent.Executor;

/**
 * Base compiler states for {@link RepositoryCompilerStates} and {@link ReactionCompilerStates}. For
 * conciseness in addressing a "{@link Repository} or {@link Reaction}" to be compiled, the
 * documentation here refers to one as a <i>rex</i>, as in "<code>Re*</code>".
 */
public interface RexCompilerStates {

  // Note on documentation grammar: most method summaries here use an infinitive verb phrase ("do
  // something") instead of the usual 3rd-person grammar ("does something"). This is because the
  // full sentence for these method summaries are "this method specifies that the next step of the
  // flow should do something", rather than "this method does something".

  /**
   * Base compiler state allowing to specify the next directive of the data processing flow.
   *
   * @param <TPre> The output value type of the previous directive.
   * @param <TCfg> The compiler state after the flow ends.
   * @param <TSelf> The self-type.
   */
  interface RFlowBase<TPre, TCfg, TSelf extends RFlowBase<TPre, TCfg, TSelf>>
      extends RSyncFlowBase<TPre, TCfg, TSelf> {
    /**
     * Invoke the given {@code async} operation to transform the input value asynchronously, and
     * when the operation completes, use its output value as the output value of this directive.
     *
     * <p>The asynchronous operation can end from any thread of its choice, but must end eventually,
     * or the data processing flow will be permanently blocked.
     *
     * <p>The directives that follow will be run sequentially within the call of the output
     * receiver's {@link Receiver#accept} method, until the flow completes, the next {@code async()}
     * directive leaves the current thread, or, if applicable, the {@code goLazy()} directive runs.
     * Depending on the asynchronous operation, this may starve a resource (such as an
     * {@link Executor} with limited threads). Use multiple, strategically placed {@code async()}
     * directives to achieve fairness.
     *
     * <p>It is possible to end an asynchronous operation synchronously by calling the output
     * receiver before {@link Async#async} returns. This is supported, but the risk of stack
     * overflow will be higher.
     *
     * <p>If the data processing flow is to be cancelled during the asynchronous operation, any
     * thread interrupt signal cannot be delivered after {@link Async#async} returns, but the
     * asynchronous operation can poll the cancellation {@link Condition} that will be provided to
     * {@link Async#async}.
     */
    @NonNull
    <TCur> RFlowBase<TCur, TCfg, ?> async(@NonNull Async<TPre, TCur> async);
  }

  /**
   * Base compiler state allowing to specify the next synchronous directive of the data processing
   * flow.
   *
   * @param <TPre> The output value type of the previous directive.
   * @param <TCfg> The compiler state after the flow ends.
   * @param <TSelf> The self-type.
   */
  interface RSyncFlowBase<TPre, TCfg, TSelf extends RSyncFlowBase<TPre, TCfg, TSelf>> {
    /**
     * Ignore the input value, and use the value newly obtained from the given supplier as the
     * output value.
     */
    @NonNull
    <TCur> RSyncFlowBase<TCur, TCfg, ?> getFrom(@NonNull Supplier<TCur> supplier);

    /**
     * Like {@link #getFrom}, ignore the input value and attempt to get the new value from the given
     * supplier. If the attempt fails, terminate the data processing flow by sending the failure to
     * the termination clause that follows; otherwise take the successful value as the output of
     * this directive.
     */
    @NonNull
    <TCur>
    RTerminationBase<Throwable, ? extends RSyncFlowBase<TCur, TCfg, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    /**
     * Take the input value and the value newly obtained from the given supplier, merge them using
     * the given merger, and use the resulting value as the output value.
     */
    @NonNull
    <TAdd, TCur> RSyncFlowBase<TCur, TCfg, ?> mergeIn(@NonNull Supplier<TAdd> supplier,
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
    RTerminationBase<Throwable, ? extends RSyncFlowBase<TCur, TCfg, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    /**
     * Transform the input value using the given function into the output value.
     */
    @NonNull
    <TCur> RSyncFlowBase<TCur, TCfg, ?> transform(@NonNull Function<? super TPre, TCur> function);

    /**
     * Like {@link #transform}, attempt to transform the input value using the given function. If
     * the attempt fails, terminate the data processing flow by sending the failure to the
     * termination clause that follows; otherwise take the successful value as the output of this
     * directive.
     */
    @NonNull
    <TCur> RTerminationBase<Throwable, ? extends RSyncFlowBase<TCur, TCfg, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    /**
     * Check the input value with the given predicate. If the predicate applies, continue the data
     * processing flow with the same value, otherwise terminate the flow with the termination clause
     * that follows. The termination clause takes the input value as its input.
     */
    @NonNull
    RTerminationBase<TPre, TSelf> check(@NonNull Predicate<? super TPre> predicate);

    /**
     * Use the case-function to compute a case value out of the input value and check it with the
     * given predicate. If the predicate applies to the case value, continue the data processing
     * flow with the <i>input value</i>, otherwise terminate the flow with the termination clause
     * that follows. The termination clause takes the <i>case value</i> as its input.
     */
    @NonNull
    <TCase> RTerminationBase<TCase, TSelf> check(
        @NonNull Function<? super TPre, TCase> caseFunction,
        @NonNull Predicate<? super TCase> casePredicate);

    /**
     * Send the input value to the given receiver, and then pass on the input value as the output of
     * this directive, not modifying it.
     *
     * <p>Typical uses of this directive include reporting progress and/or errors in the UI,
     * starting a side process, logging, profiling and debugging, etc. The {@link Receiver#accept}
     * method is called synchronously, which means its execution blocks the rest of the data
     * processing flow. If the flow is to cancel with {@linkplain RexConfig#SEND_INTERRUPT the
     * interrupt signal}, the receiver may also see the signal.
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
     * <p>The same usage notes for {@link #sendTo} apply to this method.
     */
    @NonNull
    <TAdd> TSelf bindWith(@NonNull Supplier<TAdd> secondValueSupplier,
        @NonNull Binder<? super TPre, ? super TAdd> binder);

    /**
     * End the data processing flow but without using the output value and without notifying the
     * registered {@link Updatable}s.
     */
    @NonNull
    TCfg thenSkip();
  }

  /**
   * Base compiler state allowing to terminate the data processing flow following a failed check.
   *
   * @param <TTerm> Value type to terminate from.
   * @param <TRet> Compiler state to return to.
   */
  interface RTerminationBase<TTerm, TRet> {
    /**
     * If the previous check failed, skip the rest of the data processing flow, and do not notify
     * any registered {@link Updatable}s.
     */
    @NonNull
    TRet orSkip();
  }

  /**
   * Compiler state allowing to configure and compile the rex.
   *
   * @param <TVal> Value type of the rex.
   * @param <TRex> The compiled rex type.
   * @param <TSelf> The self-type.
   */
  interface RConfig<TVal, TRex, TSelf extends RConfig<TVal, TRex, TSelf>> {
    /**
     * Specifies the behaviors when this rex is deactivated, i.e. from being observed to not being
     * observed. The default behavior is {@link RexConfig#CONTINUE_FLOW}.
     *
     * <p>Note that if an ongoing {@link Reaction} flow is cancelled due to a non-default
     * configuration here, the value dequeued from the backing {@link Reservoir} will not be
     * enqueued back, so this may result in loss of data. Use any non-default configuration with
     * caution.
     *
     * @param deactivationConfig A bitwise combination of the constants in {@link RexConfig}.
     */
    @NonNull
    TSelf onDeactivation(@RexConfig int deactivationConfig);

    /**
     * Specifies the behaviors when an update is observed from an event source while a data
     * processing flow is ongoing. The default behavior is {@link RexConfig#CONTINUE_FLOW}.
     *
     * <p>Note that because the sole event source of a compiled {@link Reaction} is the backing
     * reservoir, which would immediately notify of the availability of the next value when the
     * previous one is dequeued, concurrent update is frequent. If an ongoing flow is cancelled due
     * to a non-default configuration here, the dequeued value will not be enqueued back, so this
     * may result in loss of data. Use any non-default configuration with caution.
     *
     * @param concurrentUpdateConfig A bitwise combination of the constants in {@link RexConfig}.
     */
    @NonNull
    TSelf onConcurrentUpdate(@RexConfig int concurrentUpdateConfig);

    /**
     * Compiles a rex that exhibits the previously defined behaviors.
     */
    @NonNull
    TRex compile();
  }
}
