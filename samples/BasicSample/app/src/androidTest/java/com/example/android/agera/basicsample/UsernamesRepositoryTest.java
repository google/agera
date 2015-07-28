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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

/**
 * Tests for the implementation of {@link UsernamesRepository}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UsernamesRepositoryTest {

  private UsernamesFetcher mockUsernamesFetcher;

  private UsernamesRepository usernamesRepository;

  /**
   * {@link ArgumentCaptor} is a powerful Mockito API to capture argument values and use them to
   * perform further actions or assertions on them.
   */
  private ArgumentCaptor<UsernamesFetcher.UsernamesCallback> fetcherCallbackCaptor;

  @Before
  public void setupUsernamesRepository() {
    // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
    // inject the mocks in the test the initMocks method needs to be called.
    mockUsernamesFetcher = mock(UsernamesFetcher.class);
    fetcherCallbackCaptor = ArgumentCaptor.forClass(UsernamesFetcher.UsernamesCallback.class);

    // Get a reference to the class under test
    getInstrumentation().runOnMainSync(new Runnable() {
      @Override
      public void run() {
        usernamesRepository = new UsernamesRepository(mockUsernamesFetcher);
      }
    });
  }

  @Test
  public void updateWithErrorFromUsernamesFetcher_ErrorStatus() {
    // When the repository is updated
    usernamesRepository.update();

    // And the username fetcher returns an error
    verify(mockUsernamesFetcher).getUsernames(fetcherCallbackCaptor.capture());
    fetcherCallbackCaptor.getValue().setError();

    // Then the repository has an error status and no usernames available
    assertTrue(usernamesRepository.isError());
    assertNull(usernamesRepository.get());
  }

  @Test
  public void updateWithSuccessFromUsernamesFetcher_UsernamesAvailable() {
    // Given fake usernames
    String[] usernames = new String[] {"one", "two"};

    // When the repository is updated
    usernamesRepository.update();

    // And the username fetcher returns an error
    verify(mockUsernamesFetcher).getUsernames(fetcherCallbackCaptor.capture());
    fetcherCallbackCaptor.getValue().setUsernames(usernames);

    // Then the repository has a success status and usernames are available
    assertFalse(usernamesRepository.isError());
    assertTrue(usernamesRepository.get().length == usernames.length);
  }
}
