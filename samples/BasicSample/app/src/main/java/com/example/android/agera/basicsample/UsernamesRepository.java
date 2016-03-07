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
import com.google.android.agera.Observable;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;

import android.support.annotation.NonNull;

/**
 * The UsernamesRepository is an {@link Observable}, implemented by extending {@link
 * BaseObservable} and a {@link Supplier<String>} of usernames. it is observed by the
 * {@link MainFragment}. It is also an {@link Updatable} as it observes the {@link
 * OnRefreshObservable} so it can fetch a new list of usernames whenever the user requests a
 * refresh. It uses the {@link UsernamesFetcher} to request a list of usernames and therefore
 * implements the {@link UsernamesFetcher.UsernamesCallback}.
 * <P />
 * We override {@link BaseObservable#observableActivated()} to trigger an update so the
 * UsernamesRepository contains the most up to date usernames. This mechanism enables the
 * UsernamesRepository to be updated as soon as {@link MainFragment} observes it (in {@link
 * MainFragment#onResume()}.
 */
public class UsernamesRepository extends BaseObservable
    implements Supplier<String[]>, Updatable, UsernamesFetcher.UsernamesCallback {

  /**
   * The usernames list. This list is the most up to date known usernames.
   */
  private String[] usernames;

  /**
   * Whether the last update resulted in an error to retrieve a new list of usernames.
   */
  private boolean lastRefreshError;

  /**
   * This is responsible for getting the list of usernames. It simulates a server call, and uses
   * a {@link UsernamesFetcher.UsernamesCallback}.
   */
  private final UsernamesFetcher usernamesFetcher;

  public UsernamesRepository(UsernamesFetcher usernamesFetcher) {
    super();
    this.usernamesFetcher = usernamesFetcher;
  }

  /**
   * @return the most up to date known list of usernames
   */
  @NonNull
  @Override
  public String[] get() {
    return usernames;
  }

  /**
   * @return true if the last update resulted in an error in retrieving a new list of usernames
   */
  public boolean isError() {
    return lastRefreshError;
  }

  /**
   * As this {@link UsernamesRepository} is set up to observe the {@link OnRefreshObservable},
   * this is triggered whenever a request has been requested.
   */
  @Override
  public void update() {
    usernamesFetcher.getUsernames(this);
  }

  /**
   * The {@link #usernamesFetcher} couldn't fetch a new list of usernames.
   */
  @Override
  public void setError() {
    lastRefreshError = true;
    dispatchUpdate();
  }

  /**
   * @param usernames The new list of usernames fetched by the {@link #usernamesFetcher}
   */
  @Override
  public void setUsernames(String[] usernames) {
    this.usernames = usernames;
    lastRefreshError = false;
    dispatchUpdate();
  }

  @Override
  protected void observableActivated() {
    // Now that this is activated, we trigger an update to ensure the repository contains up to
    // date data.
    update();
  }
}
