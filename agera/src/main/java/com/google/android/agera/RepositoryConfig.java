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

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Constants controlling some behaviors of the compiled {@link Repository}s.
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef(flag = true, value = {
    RepositoryConfig.CONTINUE_FLOW,
    RepositoryConfig.CANCEL_FLOW,
    RepositoryConfig.RESET_TO_INITIAL_VALUE,
    RepositoryConfig.SEND_INTERRUPT,
})
public @interface RepositoryConfig {

  /**
   * If a data processing flow is ongoing, allow it to finish. If this is the configuration for the
   * concurrent update, the new data processing flow will commence when the current one finishes.
   * This is the default behavior and, with a value of 0, cannot be combined with other
   * configurations.
   */
  int CONTINUE_FLOW = 0;

  /**
   * If a data processing flow is ongoing, cancel it at the earliest opportunity, which is
   * immediately after the currently running directive, or during it if {@link #SEND_INTERRUPT} is
   * used and the current operator ({@link Function}, {@link Supplier}, {@link Merger} etc.) has
   * adequate support for the thread interruption signal. If this is the configuration for a
   * repository, cancellation prevents the flow from updating the repository value, even if the last
   * run directive would have set the new value otherwise. If this is the configuration for the
   * concurrent update, the new data processing flow will commence as soon as the current one is
   * terminated, effectively redoing the data processing from the start. This behavior is implicit
   * if {@link #RESET_TO_INITIAL_VALUE} or {@link #SEND_INTERRUPT} is specified.
   */
  int CANCEL_FLOW = 1;

  /**
   * The repository value should reset to the initial value on deactivation. The reset is immediate
   * while the data processing flow, if ongoing, may terminate only after the currently running
   * directive. If this is the configuration for the concurrent update, the repository value will
   * <i>not</i> be reset, but due to the included {@link #CANCEL_FLOW} value, the ongoing flow will
   * still be cancelled.
   */
  int RESET_TO_INITIAL_VALUE = 2 | CANCEL_FLOW;

  /**
   * If a data processing flow is ongoing and in the asynchronous stage (after the first
   * {@code goTo} directive and before the {@code goLazy} directive), {@linkplain Thread#interrupt()
   * interrupt} the thread currently running the flow, to signal the current operator (function,
   * supplier, merger etc.) to stop early. The interrupt signal will not be sent if the flow is in
   * a synchronous stage, to minimize unwanted effects on the worker looper thread and the thread
   * from which the client calls {@link Repository#get()}.
   */
  int SEND_INTERRUPT = 4 | CANCEL_FLOW;
}
