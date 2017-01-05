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
package com.google.android.agera.content;

import static com.google.android.agera.content.ContentObservables.broadcastObservable;
import static com.google.android.agera.content.ContentObservables.sharedPreferencesObservable;
import static com.google.android.agera.content.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.content.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.content.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import com.google.android.agera.ActivationHandler;
import com.google.android.agera.content.test.matchers.UpdatableUpdated;
import com.google.android.agera.content.test.mocks.MockUpdatable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class ContentObservablesTest {
  private static final String TEST_KEY = "test key";
  private static final String NOT_TEST_KEY = "not test key";
  private static final String TEST_ACTION = "TEST_ACTION";
  private static final String PRIMARY_ACTION = "PRIMARY_ACTION";

  private MockUpdatable updatable;
  private MockUpdatable secondUpdatable;
  private ArgumentCaptor<OnSharedPreferenceChangeListener> sharedPreferenceListenerCaptor;
  @Mock
  private ActivationHandler mockActivationHandler;
  @Mock
  private SharedPreferences sharedPreferences;

  @Before
  public void setUp() {
    initMocks(this);
    sharedPreferenceListenerCaptor =
        ArgumentCaptor.forClass(OnSharedPreferenceChangeListener.class);
    doNothing().when(sharedPreferences).registerOnSharedPreferenceChangeListener(
        sharedPreferenceListenerCaptor.capture());
    updatable = mockUpdatable();
    secondUpdatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
    secondUpdatable.removeFromObservables();
  }

  @Test
  public void shouldUpdateSharedPreferencesWhenKeyChanges() {
    updatable.addToObservable(sharedPreferencesObservable(sharedPreferences, TEST_KEY));

    sharedPreferenceListenerCaptor.getValue()
        .onSharedPreferenceChanged(sharedPreferences, TEST_KEY);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotUpdateSharedPreferencesWhenOtherKeyChanges() {
    updatable.addToObservable(sharedPreferencesObservable(sharedPreferences, TEST_KEY));

    sharedPreferenceListenerCaptor.getValue()
        .onSharedPreferenceChanged(sharedPreferences, NOT_TEST_KEY);

    assertThat(updatable, UpdatableUpdated.wasNotUpdated());
  }

  @Test
  public void shouldBeAbleToObserveBroadcasts() {
    updatable.addToObservable(broadcastObservable(getApplication(), TEST_ACTION));
  }

  @Test
  public void shouldUpdateForBroadcast() {
    updatable.addToObservable(broadcastObservable(getApplication(), TEST_ACTION));

    sendBroadcast(new Intent(TEST_ACTION));

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldUpdateForSecondaryBroadcast() {
    updatable.addToObservable(broadcastObservable(getApplication(), PRIMARY_ACTION, TEST_ACTION));

    sendBroadcast(new Intent(TEST_ACTION));

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotGetUpdateFromBroadcastForEmptyFilter() {
    updatable.addToObservable(broadcastObservable(getApplication()));

    sendBroadcast(new Intent(TEST_ACTION));

    assertThat(updatable, UpdatableUpdated.wasNotUpdated());
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(ContentObservables.class, hasPrivateConstructor());
  }

  private void sendBroadcast(final Intent intent) {
    getApplication().sendBroadcast(intent);
  }

  private static Application getApplication() {
    return RuntimeEnvironment.application;
  }
}
