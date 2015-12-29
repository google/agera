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

/**
 * A buffering mechanism that enqueues the values received via the {@link Receiver} interface and
 * offers them back through the {@link Repository} interface.
 *
 * <p>Values can be enqueued using {@link #accept}. It is up to the implementation, typically the
 * backing store, to decide whether and how to enqueue each value. Values are dequeued using
 * {@link #get}. This is a fallible operation (an <i>attempt</i> that returns a {@link Result}).
 * Calling {@link #get} when the reservoir is empty yields {@link Result#absent()}.
 *
 * <p>The {@link Updatable}s observing this reservoir will be updated when a value is enqueued
 * while the reservoir is empty, or when a value is dequeued so the next enqueued value is exposed,
 * but <i>not when the last value is dequeued so the reservoir becomes empty</i>. In other words, an
 * update from this reservoir means the availability of the next value to be dequeued. Additionally,
 * when an updatable <i>activates</i> the reservoir, i.e. turns it from unobserved to observed, if
 * it is already non-empty, the updatable will receive an out-of-band update immediately (subject to
 * {@code Looper} processing delay). Subsequently added updatables will not receive this special
 * call: it is assumed that the availability of the currently exposed value will have been notified
 * to existing updatables, and that a consumer will dequeue the value soon.
 *
 * <p>Multiple {@linkplain RepositoryCompilerStates compiled repositories} using the same reservoir
 * as their shared event source and data source can be a simple way to achieve parallelism with load
 * balancing.
 */
public interface Reservoir<T> extends Receiver<T>, Repository<Result<T>> {}
