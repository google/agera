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

import static com.google.android.agera.Reactions.reactionTo;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import com.google.android.agera.test.mocks.MockUpdatable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class ReactionsTest {
  private static final double DOUBLE_VALUE = 3D;

  private MockUpdatable updatable;
  @Mock
  private Receiver<Double> mockDoubleReceiver;

  @Before
  public void setUp() {
    initMocks(this);
    updatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldGetUpdateFromReaction() {
    final Reaction<Double> reaction = reactionTo(Double.class)
        .sendTo(mockDoubleReceiver)
        .thenEnd()
        .compile();
    updatable.addToObservable(reaction);

    reaction.accept(DOUBLE_VALUE);

    verify(mockDoubleReceiver).accept(DOUBLE_VALUE);
    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Reactions.class, hasPrivateConstructor());
  }
}
