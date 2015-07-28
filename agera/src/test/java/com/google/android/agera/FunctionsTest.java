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

import static com.google.android.agera.Functions.functionFrom;
import static com.google.android.agera.Functions.functionFromListOf;
import static com.google.android.agera.Functions.identityFunction;
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Functions.supplierAsFunction;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

import android.support.annotation.NonNull;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class FunctionsTest {
  private static final int VALUE = 42;
  private static final int VALUE_PLUS_TWO = 44;
  private static final int RECOVER_VALUE = 43;
  private static final String INPUT_STRING = "input";
  private static final List<String> INPUT_LIST = asList("some", "strings", "for", "testing");
  private static final Result<Integer> SUCCESS = success(VALUE);
  @SuppressWarnings("ThrowableInstanceNeverThrown")
  private static final Throwable THROWABLE = new Throwable();
  private static final Result<Integer> FAILURE = failure(THROWABLE);
  private static final Result<Integer> RECOVER_SUCCESS = success(RECOVER_VALUE);

  @Mock
  private Function<Integer, Result<Integer>> mockDivideTenFunction;
  @Mock
  private Function<Integer, Integer> mockPlusTwoFunction;
  @Mock
  private Function<Throwable, Result<Integer>> mockTryRecoverFunction;
  @Mock
  private Function<Throwable, Integer> mockRecoverFunction;
  @Mock
  private Supplier<String> mockSupplier;

  @Before
  public void setup() {
    initMocks(this);
    when(mockRecoverFunction.apply(THROWABLE)).thenReturn(RECOVER_VALUE);
    when(mockTryRecoverFunction.apply(THROWABLE)).thenReturn(RECOVER_SUCCESS);
    when(mockPlusTwoFunction.apply(anyInt())).thenReturn(VALUE_PLUS_TWO);
    when(mockDivideTenFunction.apply(eq(2))).thenReturn(success(5));
    when(mockDivideTenFunction.apply(eq(0))).thenReturn(FAILURE);
    when(mockSupplier.get()).thenReturn(INPUT_STRING);
  }

  @Test
  public void shouldReturnObjectFromStaticFunction() {
    assertThat(staticFunction(INPUT_STRING).apply(new Object()), is(sameInstance(INPUT_STRING)));
  }

  @Test
  public void shouldReturnObjectFromSupplierForSupplierAsFunction() {
    assertThat(supplierAsFunction(mockSupplier).apply(new Object()),
        is(sameInstance(INPUT_STRING)));
  }

  @Test
  public void shouldReturnFromObject() {
    assertThat(Functions.<String>identityFunction().apply(INPUT_STRING),
        is(sameInstance(INPUT_STRING)));
  }

  @Test
  public void shouldBeASingleton() {
    assertThat(identityFunction(), is(sameInstance(identityFunction())));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Functions.class, hasPrivateConstructor());
  }

  @Test
  public void shouldCreateFunctionFromItemToItem() {
    final Function<String, Integer> function = functionFrom(String.class)
        .apply(new DoubleString())
        .thenApply(new StringLength());

    assertThat(function.apply(INPUT_STRING), is(10));
  }

  @Test
  public void shouldCreateFunctionFromItemToItemViaList() {
    final Function<String, String> function = functionFrom(String.class)
        .apply(new DoubleString())
        .unpack(new StringToListChar())
        .morph(new SortList<Character>())
        .limit(5)
        .filter(new CharacterFilter('n'))
        .thenApply(new CharacterListToString());

    assertThat(function.apply(INPUT_STRING), is("nn"));
  }

  @Test
  public void shouldCreateFunctionFromListToItem() {
    final Function<List<String>, Integer> function = functionFromListOf(String.class)
        .limit(3)
        .map(new StringLength())
        .thenApply(new SumOfIntegersInList());

    assertThat(function.apply(INPUT_LIST), is(14));
  }

  @Test
  public void shouldCreateFunctionFromItemToList() {
    final Function<String, List<Character>> function = functionFrom(String.class)
        .apply(new DoubleString())
        .unpack(new StringToListChar())
        .thenLimit(7);

    assertThat(function.apply(INPUT_STRING), contains('i', 'n', 'p', 'u', 't', 'i', 'n'));
  }

  @Test
  public void shouldCreateFunctionFromItemToListWithThenFilter() {
    final Function<String, List<Character>> function = functionFrom(String.class)
        .apply(new DoubleString())
        .unpack(new StringToListChar())
        .thenFilter(new CharacterFilter('n'));

    assertThat(function.apply(INPUT_STRING), contains('n', 'n'));
  }

  @Test
  public void shouldCreateFunctionFromListToList() {
    final Function<List<String>, List<Integer>> function = functionFromListOf(String.class)
        .limit(3)
        .thenMap(new StringLength());

    assertThat(function.apply(INPUT_LIST), contains(4, 7, 3));
  }

  @Test
  public void shouldCreateFunctionFromListToListWithZeroLimit() {
    final Function<List<String>, List<Integer>> function = functionFromListOf(String.class)
        .limit(0)
        .thenMap(new StringLength());

    assertThat(function.apply(INPUT_LIST), Matchers.<Integer>emptyIterable());
  }

  @Test
  public void shouldCreateFunctionFromListToListWithLimitLargerThanInputList() {
    final Function<List<String>, List<Integer>> function = functionFromListOf(String.class)
        .limit(INPUT_LIST.size() * 2)
        .thenMap(new StringLength());

    assertThat(function.apply(INPUT_LIST), contains(4, 7, 3, 7));
  }

  private static final class DoubleString implements Function<String, String> {
    @NonNull
    @Override
    public String apply(@NonNull final String input) {
      return input + input;
    }
  }

  private static final class StringLength implements Function<String, Integer> {
    @NonNull
    @Override
    public Integer apply(@NonNull final String input) {
      return input.length();
    }
  }

  private static final class StringToListChar implements Function<String, List
      <Character>> {
    @NonNull
    @Override
    public List<Character> apply(@NonNull final String input) {
      List<Character> list = new ArrayList<>();
      for (final char c : input.toCharArray()) {
        list.add(c);
      }
      return list;
    }
  }

  private static final class SortList<T extends Comparable<T>>
      implements Function<List<T>, List<T>> {
    @NonNull
    @Override
    public List<T> apply(@NonNull final List<T> input) {
      final List<T> output = new ArrayList<>(input);
      Collections.sort(output);
      return output;
    }
  }

  private static final class CharacterFilter implements Predicate<Character> {
    private final char character;

    public CharacterFilter(final char p) {
      character = p;
    }

    @Override
    public boolean apply(@NonNull final Character value) {
      return value.equals(character);
    }
  }

  private final class CharacterListToString implements Function<List<Character>, String> {

    @NonNull
    @Override
    public String apply(@NonNull final List<Character> input) {
      final StringBuilder stringBuilder = new StringBuilder();
      for (final char character : input) {
        stringBuilder.append(character);
      }
      return stringBuilder.toString();
    }
  }

  private static final class SumOfIntegersInList implements Function<List<Integer>, Integer> {
    @NonNull
    @Override
    public Integer apply(@NonNull final List<Integer> input) {
      int output = 0;
      for (final int integer : input) {
        output += integer;
      }
      return output;
    }
  }
}
