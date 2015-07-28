/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.example.android.agera.basicsample;

import com.google.android.agera.BaseObservable;
import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import android.support.v4.widget.SwipeRefreshLayout;

/**
 * This implements {@link SwipeRefreshLayout.OnRefreshListener} so it can be attached to a
 * {@link SwipeRefreshLayout}. It also extends {@link BaseObservable} so changes in here can be
 * passed on to its observer (ie an {@link Updatable}, using an {@link UpdateDispatcher}.
 */
public class OnRefreshObservable extends BaseObservable
    implements SwipeRefreshLayout.OnRefreshListener {

  /**
   * Triggered when the associated {@link SwipeRefreshLayout} is refreshed by the user. The event
   * is passed on to the observers, using the {@link UpdateDispatcher} provided by {@link
   * BaseObservable}.
   */
  @Override
  public void onRefresh() {
    dispatchUpdate();
  }
}