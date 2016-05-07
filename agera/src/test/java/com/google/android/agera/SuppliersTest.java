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

import static com.google.android.agera.Suppliers.functionAsSupplier;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.SupplierGives.gives;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public final class SuppliersTest {
  private static final Object ITEM = new Object();
  private static final Object RETURN_ITEM = new Object();

  @Mock
  private Function<Object, Object> mockFunction;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockFunction.apply(ITEM)).thenReturn(RETURN_ITEM);
  }

  @Test
  public void shouldRunFactoryWithFromObjectAndReturnFactoryOutputForFunctionWithSupplier() {
    assertThat(functionAsSupplier(mockFunction, ITEM), gives(RETURN_ITEM));
  }

  @Test
  public void shouldReturnStaticSupplierValue() {
    assertThat(staticSupplier(ITEM), gives(ITEM));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Suppliers.class, hasPrivateConstructor());
  }
}
