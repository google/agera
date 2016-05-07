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

import static com.google.android.agera.Preconditions.checkArgument;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

public final class PreconditionsTest {

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIllegalStateExceptionForCheckStateWithFalseExpression() {
    checkState(false, "");
  }

  @Test
  public void shouldNotThrowExceptionForCheckStateWithTrueExpression() {
    checkState(true, "");
  }

  @Test
  public void shouldNotThrowExceptionForCheckArgumentWithTrueExpression() {
    checkArgument(true, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionForCheckArgumentWithFalseExpression() {
    checkArgument(false, "");
  }

  @Test(expected = NullPointerException.class)
  public void shouldThrowNullPointerExceptionForCheckNotNullWithNull() {
    checkNotNull(null);
  }

  @Test
  public void shouldNotThrowExceptionForCheckNotNullWithValidObject() {
    checkNotNull(new Object());
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Preconditions.class, hasPrivateConstructor());
  }
}