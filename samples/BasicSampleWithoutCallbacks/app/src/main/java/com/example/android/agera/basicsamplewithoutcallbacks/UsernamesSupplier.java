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
package com.example.android.agera.basicsamplewithoutcallbacks;

import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This implements getting a list of usernames. A fake latency is added. The number of returned
 * usernames is controlled by a public static field, to allow us to write deterministic tests. This
 * gets the usernames on the thread it is called from.
 * <p/>
 * Typically, this would directly implement {@link Supplier<Result<List<String>>>} and {@link
 * Supplier#get()} would be a wrapper around {@link #getUsernames()}. Of course this is possible
 * only if this
 * class is controlled by the same team.
 */
public class UsernamesSupplier implements Supplier<Result<List<String>>> {

  /**
   * Config constant that determines the number of users to return. {@link #getUsernames()}
   * returns null if this is negative.
   */
  public static int NUMBER_OF_USERS = 4;

  private static final String TAG = UsernamesSupplier.class.getSimpleName();

  /**
   * This method fakes getting a list of usernames from a server. It returns null if {@link
   * #NUMBER_OF_USERS} is negative. It simulates server latency to return usernames. It is a
   * blocking call.
   */
  private List<String> getUsernames() {
    // Simulate network latency
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Log.e(TAG, e.toString());
      return null;
    }

    if (NUMBER_OF_USERS < 0) {
      return null;
    }

    String name1 = "Joe";
    String name2 = "Amanda";
    final List<String> usernames = new ArrayList<String>();
    Random random = new Random();
    for (int i = 0; i < NUMBER_OF_USERS; i++) {
      int number = random.nextInt(50);
      if (System.currentTimeMillis() % 2 == 0) {
        usernames.add(name1 + number);
      } else {
        usernames.add(name2 + number);
      }
    }

    return usernames;
  }

  @NonNull
  @Override
  public Result<List<String>> get() {
    List<String> usernames = getUsernames();
    if (usernames == null) {
      return Result.failure();
    } else {
      return Result.success(getUsernames());
    }
  }
}
