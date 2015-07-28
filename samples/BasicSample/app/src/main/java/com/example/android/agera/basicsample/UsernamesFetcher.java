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

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This implements getting a list of usernames. A fake latency is added. The number of returned
 * usernames is controlled by a public static field, to allow us to write deterministic tests.
 *
 */
public class UsernamesFetcher {

  /**
   * Config constant that determines the number of users to return.
   * {@link #getUsernames(UsernamesCallback)} fires an error if this is negative.
   */
  public static int NUMBER_OF_USERS = 4;

  /**
   * This method fakes getting a list of usernames from a server. It fires
   * {@link UsernamesCallback#setError} if {@link #NUMBER_OF_USERS} is negative. It simulates
   * server latency to return usernames.
   */
  public void getUsernames(final UsernamesCallback callback) {
    if (NUMBER_OF_USERS < 0) {
      callback.setError();
      return;
    }

    Handler h = new Handler();
    Runnable r = new Runnable() {
      @Override
      public void run() {
        // Create a fake list of usernames
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
        callback.setUsernames(usernames.toArray(new String[usernames.size()]));
      }
    };

    // Simulate network latency
    h.postDelayed(r, 2000);
  }

  public interface UsernamesCallback {
    void setError();
    void setUsernames(String[] usernames);
  }
}
