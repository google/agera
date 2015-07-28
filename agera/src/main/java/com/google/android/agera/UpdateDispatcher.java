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
 * Passes on updates from an {@link Updatable#update} call to any {@link Updatable} added using
 * {@link Observable#addUpdatable}. It should be possible to call {@link UpdateDispatcher#update}
 * from any thread.
 *
 * <p>This interface should typically not be implemented by client code; the standard
 * implementations obtainable from {@link Observables#updateDispatcher} help implement
 * {@link Observable}s adhering to the contract.
 */
public interface UpdateDispatcher extends Observable, Updatable {}
