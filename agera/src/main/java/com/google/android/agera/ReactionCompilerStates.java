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

/**
 * Container of the compiler state interfaces supporting the declaration of {@link Reaction}s
 * using the type-safe declarative language.
 *
 * <p>A compiled {@link Reaction} uses a {@link Reservoir} as the queuing mechanism for the
 * received values, and the same <i>{@linkplain RepositoryCompilerStates data processing flow}</i>
 * component as the compiled repository to process the values in that reservoir and notify of its
 * own updates. In essence, a compiled reaction bridges a reservoir and a compiled repository, using
 * the former to implement the {@link Receiver} side and the latter the {@link Observable} side.
 *
 * <p>Unlike a compiled repository which allows client code to specify the event sources, a compiled
 * reaction takes the backing reservoir as its sole event source and first data source. Therefore,
 * after starting the compiled reaction declaration with a method in {@link Reactions}, which
 * specifies the backing reservoir, the declaration of the data processing flow immediately follows.
 * The declaration ends with the optional configurations and
 * {@link RexCompilerStates.RConfig#compile() compile()}.
 *
 * <p>There is no initial value for a compiled reaction; the data processing flow takes a dequeued
 * value as its input, and if the reservoir is empty, the flow will not continue. The final output
 * value is also unimportant; there is no built-in storage for it. The flow only needs to decide
 * whether to <i>end</i> the flow normally (with notification), or <i>skip</i> the notification.
 * Therefore, the directive set for a compiled reaction is smaller than that for a compiled
 * repository. All final value producing directives and termination clauses are reduced to
 * {@code thenEnd()} / {@code end()}, and the lazy computation directive {@code goLazy()} is
 * unnecessary.
 *
 * <p>Apart from the aforementioned differences, all other semantics are identical to a compiled
 * repository. See {@link RepositoryCompilerStates} for more information.
 *
 * <h3>List of directives</h3>
 *
 * <b>Variables:</b> s: supplier; fs: fallible supplier; m: merger; fm: fallible merger;
 * f: function; ff: fallible function; p: predicate; r: receiver; b: binder; e: executor.
 * <ul>
 *   <li>{@link RFlow#getFrom getFrom(s)}
 *   <li>{@link RFlow#mergeIn mergeIn(s, m)}
 *   <li>{@link RFlow#transform transform(f)}
 *   <li>{@link RFlow#attemptGetFrom attemptGetFrom(fs)}.<i>term</i>
 *   <li>{@link RFlow#attemptMergeIn attemptMergeIn(s, fm)}.<i>term</i>
 *   <li>{@link RFlow#attemptTransform attemptTransform(ff)}.<i>term</i>
 *   <li>{@link RFlow#check(Predicate) check(p)}.<i>term</i>
 *   <li>{@link RFlow#check(Function, Predicate) check(f, p)}.<i>term</i>
 *   <li>{@link RFlow#sendTo sendTo(r)}
 *   <li>{@link RFlow#bindWith bindWith(s, b)}
 *   <li>{@link RFlow#goTo goTo(e)}
 *   <li>{@link RFlow#thenSkip thenSkip()}
 *   <li>{@link RFlow#thenEnd thenEnd()}
 * </ul>
 * where <i>term</i> (the termination clause) is one of:
 * <ul>
 *   <li>{@link RTermination#orSkip orSkip()}
 *   <li>{@link RTermination#orEnd orEnd()}
 * </ul>
 */
public interface ReactionCompilerStates extends RexCompilerStates {

  /**
   * Compiler state allowing to specify the next directive of the data processing flow.
   *
   * @param <TVal> Value type of the reaction.
   * @param <TPre> The output value type of the previous directive.
   * @param <TCfg> RConfig compiler state type; for Java compiler type inference only.
   * @param <TSelf> Self-type; for Java compiler type inference only.
   */
  interface RFlow<TVal, TPre, TCfg extends RConfig<TVal, Reaction<TVal>, ?>,
      TSelf extends RFlow<TVal, TPre, TCfg, TSelf>> extends RFlowBase<TPre, TCfg, TSelf> {
    // Methods whose return types need subtyping (due to no "generic type of generic type" in Java):

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, TCfg, ?> getFrom(@NonNull Supplier<TCur> supplier);

    @NonNull
    @Override
    <TCur> RTermination<Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptGetFrom(
        @NonNull Supplier<Result<TCur>> attemptSupplier);

    @NonNull
    @Override
    <TAdd, TCur> RFlow<TVal, TCur, TCfg, ?> mergeIn(
        @NonNull Supplier<TAdd> supplier, @NonNull Merger<? super TPre, ? super TAdd, TCur> merger);

    @NonNull
    @Override
    <TAdd, TCur> RTermination<Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptMergeIn(
        @NonNull Supplier<TAdd> supplier,
        @NonNull Merger<? super TPre, ? super TAdd, Result<TCur>> attemptMerger);

    @NonNull
    @Override
    <TCur> RFlow<TVal, TCur, TCfg, ?> transform(
        @NonNull Function<? super TPre, TCur> function);

    @NonNull
    @Override
    <TCur> RTermination<Throwable, RFlow<TVal, TCur, TCfg, ?>> attemptTransform(
        @NonNull Function<? super TPre, Result<TCur>> attemptFunction);

    @NonNull
    @Override
    RTermination<TPre, TSelf> check(@NonNull Predicate<? super TPre> predicate);

    @NonNull
    @Override
    <TCase> RTermination<TCase, TSelf> check(
        @NonNull Function<? super TPre, TCase> caseFunction,
        @NonNull Predicate<? super TCase> casePredicate);

    // For Reactions only:

    /**
     * End the data processing flow and notify the registered {@link Updatable}s.
     */
    @NonNull
    RConfig<TVal, Reaction<TVal>, ?> thenEnd();
  }

  /**
   * Compiler state allowing to terminate the data processing flow following a failed check.
   *
   * @param <TTerm> Value type from which to terminate the flow.
   * @param <TRet> Compiler state to return to.
   */
  interface RTermination<TTerm, TRet> extends RTerminationBase<TTerm, TRet> {
    // For Reactions only:

    /**
     * If the previous check failed, terminate the data processing flow and notify the registered
     * {@link Updatable}s.
     */
    @NonNull
    TRet orEnd();
  }
}
