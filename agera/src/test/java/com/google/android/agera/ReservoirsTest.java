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

import static com.google.android.agera.Reservoirs.reservoir;
import static com.google.android.agera.Reservoirs.reservoirOf;
import static com.google.android.agera.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static com.google.android.agera.test.matchers.ReservoirGives.givesAbsentValueOf;
import static com.google.android.agera.test.matchers.ReservoirGives.givesPresentValue;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasNotUpdated;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Robolectric.flushForegroundThreadScheduler;
import static org.robolectric.annotation.Config.NONE;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.agera.test.mocks.MockUpdatable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class ReservoirsTest {
  private static final String STRING_A = "STRING_A";
  private static final String STRING_B = "STRING_B";
  private static final Integer INTEGER_1 = 1;
  private static final Integer INTEGER_2 = 2;

  private MockQueue mockQueue;
  private Reservoir<String> stringReservoir;
  private Reservoir<Integer> integerReservoir;
  private Reservoir<Object> customQueueReservoir;
  private MockUpdatable updatable;
  private MockUpdatable anotherUpdatable;

  @Before
  public void setUp() {
    mockQueue = new MockQueue();
    stringReservoir = reservoirOf(String.class);
    integerReservoir = reservoir();
    customQueueReservoir = reservoir(mockQueue);
    updatable = mockUpdatable();
    anotherUpdatable = mockUpdatable();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
    anotherUpdatable.removeFromObservables();
  }

  @Test
  public void shouldGiveAbsentValueFromEmptyReservoir() throws Exception {
    assertThat(integerReservoir, givesAbsentValueOf(Integer.class));
  }

  @Test
  public void shouldQueueValues() throws Exception {
    stringReservoir.accept(STRING_A);
    stringReservoir.accept(STRING_A);
    stringReservoir.accept(STRING_B);
    stringReservoir.accept(STRING_B);

    assertThat(stringReservoir, givesPresentValue(STRING_A));
    assertThat(stringReservoir, givesPresentValue(STRING_A));
    assertThat(stringReservoir, givesPresentValue(STRING_B));
    assertThat(stringReservoir, givesPresentValue(STRING_B));
    assertThat(stringReservoir, givesAbsentValueOf(String.class));
  }

  @Test
  public void shouldNotGetUpdateFromEmptyReservoir() throws Exception {
    updatable.addToObservable(stringReservoir);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldGetUpdateOnFirstValue() throws Exception {
    updatable.addToObservable(integerReservoir);
    give(integerReservoir, INTEGER_1);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldGetUpdateOnFirstAcceptedValue() throws Exception {
    mockQueue.reject(STRING_A);
    updatable.addToObservable(customQueueReservoir);

    give(customQueueReservoir, STRING_A);
    assertThat(updatable, wasNotUpdated());

    give(customQueueReservoir, STRING_B);
    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldGetUpdateFromFirstUpdatableOnRegisteringToNonEmptyReservoir() throws Exception {
    give(stringReservoir, STRING_A);

    updatable.addToObservable(stringReservoir);
    anotherUpdatable.addToObservable(stringReservoir);

    assertThat(updatable, wasUpdated());
    assertThat(anotherUpdatable, wasNotUpdated());
  }

  @Test
  public void shouldNotGetUpdateOnSecondValueWithoutDequeuingFirst() throws Exception {
    updatable.addToObservable(stringReservoir);
    give(stringReservoir, STRING_A);
    updatable.resetUpdated();
    give(stringReservoir, STRING_B);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldGetUpdateOnDequeuingNonLastValue() throws Exception {
    updatable.addToObservable(integerReservoir);
    give(integerReservoir, INTEGER_1);
    give(integerReservoir, INTEGER_2);
    updatable.resetUpdated();

    retrieveFrom(integerReservoir);

    assertThat(updatable, wasUpdated());
  }

  @Test
  public void shouldNotGetUpdateOnDequeuingLastValue() throws Exception {
    updatable.addToObservable(integerReservoir);
    give(integerReservoir, INTEGER_1);
    updatable.resetUpdated();

    retrieveFrom(integerReservoir);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldUseCustomQueue() throws Exception {
    mockQueue.reject(INTEGER_1).prioritize(STRING_A);
    give(customQueueReservoir, INTEGER_2);
    give(customQueueReservoir, STRING_B);
    give(customQueueReservoir, INTEGER_1);
    give(customQueueReservoir, STRING_A);

    assertThat(customQueueReservoir, givesPresentValue((Object) STRING_A));
    assertThat(customQueueReservoir, givesPresentValue((Object) INTEGER_2));
    assertThat(customQueueReservoir, givesPresentValue((Object) STRING_B));
    assertThat(customQueueReservoir, givesAbsentValueOf(Object.class));
  }

  @Test
  public void shouldNotGetUpdateWhenCustomQueuePrioritizesAnotherValue() throws Exception {
    mockQueue.prioritize(INTEGER_2);
    updatable.addToObservable(customQueueReservoir);
    give(customQueueReservoir, INTEGER_1);
    updatable.resetUpdated();

    give(customQueueReservoir, INTEGER_2);

    assertThat(updatable, wasNotUpdated());
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(Reservoirs.class, hasPrivateConstructor());
  }

  private <T> void give(@NonNull final Reservoir<T> reservoir, @NonNull final T value) {
    reservoir.accept(value);
    flushForegroundThreadScheduler();
  }

  private <T> Result<T> retrieveFrom(@NonNull final Reservoir<T> reservoir) {
    Result<T> value = reservoir.get();
    flushForegroundThreadScheduler();
    return value;
  }

  private static final class MockQueue implements Queue<Object> {

    private final Set<Object> toReject = new HashSet<>();
    private final Set<Object> toPrioritize = new HashSet<>();
    private final ArrayDeque<Object> store = new ArrayDeque<>();

    public MockQueue reject(@NonNull final Object o) {
      toReject.add(o);
      return this;
    }

    public MockQueue prioritize(@NonNull final Object o) {
      toPrioritize.add(o);
      return this;
    }

    @Override
    public boolean isEmpty() {
      return store.isEmpty();
    }

    @Override
    public boolean offer(@NonNull final Object o) {
      if (toReject.contains(o)) {
        return false;
      }
      if (toPrioritize.contains(o)) {
        store.offerFirst(o);
      } else {
        store.offerLast(o);
      }
      return true;
    }

    @Nullable
    @Override
    public Object poll() {
      return store.pollFirst();
    }

    @Override
    public boolean add(@NonNull final Object o) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean addAll(@NonNull final Collection<?> collection) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public void clear() {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean contains(@NonNull final Object object) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean containsAll(@NonNull final Collection<?> ignore) {
      throw new AssertionError("Unexpected");
    }

    @NonNull
    @Override
    public Iterator<Object> iterator() {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean remove(Object object) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean removeAll(@NonNull final Collection<?> ignore) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public boolean retainAll(@NonNull final Collection<?> ignore) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public int size() {
      throw new AssertionError("Unexpected");
    }

    @NonNull
    @Override
    public Object[] toArray() {
      throw new AssertionError("Unexpected");
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull final T[] ignore) {
      throw new AssertionError("Unexpected");
    }

    @Override
    public Object remove() {
      throw new AssertionError("Unexpected");
    }

    @Override
    public Object element() {
      throw new AssertionError("Unexpected");
    }

    @Override
    public Object peek() {
      throw new AssertionError("Unexpected");
    }
  }
}
