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

import java.util.Queue;

/**
 * An {@link Observable} process acting on the values received through {@link Receiver#accept}.
 *
 * <p>On receiving a value, a {@link Reaction} runs a process and notifies its {@link Updatable}
 * clients when the process changes any state, but it is up to the implementation to choose whether,
 * when and how to react to each value, and whether or when to notify of updates, and what an update
 * actually entails.
 *
 * <p>A predefined type in this package that complies with the contract of a {@link Reaction} will
 * implement this interface and define the actual reaction and the meaning of an update. An example
 * is the {@link MutableRepository}, which reacts to a received value by storing it for retrieval by
 * its clients, and defines an update as the availability of a new (unequal) value. A more complex
 * example is the {@link Reservoir}, which collects the received values using a {@link Queue},
 * therefore delegating the decision whether or how to react to each value to the
 * {@link Queue#offer} method, and notifies of the availability of the stored values one by one as
 * they become ready to be dequeued using {@link Reservoir#get}.
 *
 * <p>Notwithstanding the predefined subtypes, this base interface adds no extra value to
 * {@link Observable} and {@link Receiver} separately. If client code has an {@code Observable} and
 * a {@code Receiver} as separate objects, even with a similar conceptual connection, there is no
 * need to wrap the two into a {@link Reaction}. This interface is mainly for client code which
 * creates a <i>compiled reaction</i> using one of the factory methods in {@link Reactions} to be
 * able to use the compiled object as both an {@code Observable} and a {@code Receiver}.
 */
public interface Reaction<T> extends Observable, Receiver<T> {}
