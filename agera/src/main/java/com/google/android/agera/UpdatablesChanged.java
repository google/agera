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
 * Receives events of the {@link UpdateDispatcher} created with
 * {@link Observables#updateDispatcher(UpdatablesChanged)} when the first {@link Updatable} is added
 * and the last {@link Updatable} is removed.
 *
 * <p>Typically an {@link Observable} service implemented using a {@link UpdateDispatcher} only
 * needs to be updated if it has clients of its own. By starting to listen to updates from its
 * clients on {@link #firstUpdatableAdded} and stopping on {@link #firstUpdatableAdded}, the service
 * of the service can implement a <i>suspended</i>/<i>active</i> life cycle,
 * saving memory and execution time when not needed.
 */
public interface UpdatablesChanged {

  /**
   * Called when the the {@code updateDispatcher} changes state from having no {@link Updatable}s to
   * having at least one {@link Updatable}.
   */
  void firstUpdatableAdded(UpdateDispatcher updateDispatcher);

  /**
   * Called when the the {@code updateDispatcher} changes state from having {@link Updatable}s to
   * no longer having {@link Updatable}s.
   */
  void lastUpdatableRemoved(UpdateDispatcher updateDispatcher);
}
