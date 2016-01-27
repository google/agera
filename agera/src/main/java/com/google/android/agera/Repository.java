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
 * An {@link Observable} {@link Supplier} of data.
 *
 * <p>As the data contained in the {@code Repository} changes, the added {@link Updatable}s will be
 * notified of these changes and the {@link Supplier} will return the new data.
 *
 * <p>A {@code Repository} can either be implemented as
 * <ul>
 *   <li><i>Direct</i> - The contained data is always available, or can be calculated synchronously
 *   <li><i>Deferred</i> - The contained data is calculated/fetched asynchronously
 * </ul>
 *
 * <p>A {@code Repository} thus have two primary states (in addition to the different state of the
 * data it contains)
 * <ul>
 *   <li><i>Inactive</i> - There are no added {@link Updatable}s and the {@link Repository} is
 *   <i>deferred</i>. The {@link Repository} may choose to free up data to save memory and the
 *   data it returns calling {@link Repository#get()} will not be fresh.
 *   <li><i>Active</i> - There are added {@link Updatable}s or the {@link Repository} is
 *   <i>direct</i>. The {@link Repository} is keeping it's data updated based on external events.
 * </ul>
 *
 * <p>A <i>direct</i> {@code Repository} is always <i>active</i>.
 * A <i>deferred</i> {@code Repository} is <i>inactive</i> until an {@link Updatable} is added. As
 * the {@code Repository} becomes <i>active</i>, the added {@link Updatable}s will be notified and
 * the fresh data can be fetched using {@link Supplier#get()}.
 *
 * <p>When using the {@code Repository} interface it is not possible to know if the implementation
 * is <i>direct</i> or <i>deferred</i>. It is therefore important to always add an {@link Updatable}
 * to wake up the {@code Repository} when it is needed.
 */
public interface Repository<T> extends Observable, Supplier<T> {}
