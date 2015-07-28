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
 * A {@link Repository} that can receive new data through {@link Receiver#accept(Object)}.
 *
 * <p>If the new data does not {@linkplain Object#equals equal} to the old data, the added
 * {@link Updatable}s will be notified. {@link MutableRepository#accept(Object)} can be called on
 * any thread.
 */
public interface MutableRepository<T> extends Repository<T>, Receiver<T> {}
