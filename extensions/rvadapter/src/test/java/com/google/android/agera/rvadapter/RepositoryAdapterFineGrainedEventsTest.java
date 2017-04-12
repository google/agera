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
package com.google.android.agera.rvadapter;

import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static com.google.android.agera.rvadapter.test.VerifyingWrappers.verifyingWrapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

import android.support.annotation.NonNull;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import com.google.android.agera.MutableRepository;
import com.google.android.agera.UpdateDispatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
@SuppressWarnings("unchecked")
public final class RepositoryAdapterFineGrainedEventsTest {
  private static final Object REPOSITORY_VALUE_A = new Object();
  private static final Object REPOSITORY_VALUE_B = new Object();
  private static final Object STATIC_ITEM = new Object();
  private static final int PRESENTER_1_A_ITEM_COUNT = 1;
  private static final int PRESENTER_1_B_ITEM_COUNT = 3;
  private static final int STATIC_ITEM_COUNT = 2;
  private static final int PRESENTER_2_A_ITEM_COUNT = 4;
  private static final int PRESENTER_2_B_ITEM_COUNT = 5;
  private static final Object PAYLOAD_PRESENTER_1_VALUE_A_TO_B = new Object();
  private static final Object PAYLOAD_PRESENTER_1_VALUE_B_REFRESH = new Object();
  private static final Object PAYLOAD_PRESENTER_2_VALUE_B_REFRESH = new Object();

  @Mock
  private RepositoryPresenter mockPresenter1;
  @Mock
  private RepositoryPresenter mockStaticItemPresenter;
  @Mock
  private RepositoryPresenter mockPresenter2;
  @Mock
  private ListUpdateCallback fineGrainedEvents;
  @Mock
  private Runnable onChangeEvent;
  private AdapterDataObserver redirectingObserver;
  private UpdateDispatcher updateDispatcher;
  private MutableRepository repository1;
  private MutableRepository repository2;
  private RepositoryAdapter repositoryAdapter;

  @Before
  public void setUp() {
    initMocks(this);

    final Answer<Boolean> getUpdatesAnswer = new Answer<Boolean>() {
      @Override
      public Boolean answer(@NonNull final InvocationOnMock invocation) throws Throwable {
        final Object oldData = invocation.getArgument(0);
        final Object newData = invocation.getArgument(1);
        assertThat(oldData, isOneOf(REPOSITORY_VALUE_A, REPOSITORY_VALUE_B));
        assertThat(newData, isOneOf(REPOSITORY_VALUE_A, REPOSITORY_VALUE_B));
        final ListUpdateCallback callback = invocation.getArgument(2);
        if (invocation.getMock() == mockPresenter1) {
          if (oldData == REPOSITORY_VALUE_A && newData == REPOSITORY_VALUE_B) {
            applyPresenter1FromAToBChanges(callback);
          } else if (oldData == REPOSITORY_VALUE_B && newData == REPOSITORY_VALUE_A) {
            applyPresenter1FromBToAChanges(callback);
          } else if (oldData == REPOSITORY_VALUE_B && newData == REPOSITORY_VALUE_B) {
            applyPresenter1RefreshBChanges(callback);
          } else if (oldData == REPOSITORY_VALUE_A && newData == REPOSITORY_VALUE_A) {
            return false;
          }
        } else {
          if (oldData == REPOSITORY_VALUE_A && newData == REPOSITORY_VALUE_B) {
            applyPresenter2FromAToBChanges(0, callback);
          } else if (oldData == REPOSITORY_VALUE_B && newData == REPOSITORY_VALUE_A) {
            applyPresenter2FromBToAChanges(0, callback);
          } else if (oldData == REPOSITORY_VALUE_B && newData == REPOSITORY_VALUE_B) {
            applyPresenter2RefreshBChanges(0, callback);
          } else if (oldData == REPOSITORY_VALUE_A && newData == REPOSITORY_VALUE_A) {
            return false;
          }
        }
        return true;
      }
    };

    when(mockPresenter1.getItemCount(REPOSITORY_VALUE_A)).thenReturn(PRESENTER_1_A_ITEM_COUNT);
    when(mockPresenter1.getItemCount(REPOSITORY_VALUE_B)).thenReturn(PRESENTER_1_B_ITEM_COUNT);
    when(mockPresenter1.getUpdates(any(), any(), any(ListUpdateCallback.class)))
        .then(getUpdatesAnswer);

    when(mockStaticItemPresenter.getItemCount(STATIC_ITEM)).thenReturn(STATIC_ITEM_COUNT);

    when(mockPresenter2.getItemCount(REPOSITORY_VALUE_A)).thenReturn(PRESENTER_2_A_ITEM_COUNT);
    when(mockPresenter2.getItemCount(REPOSITORY_VALUE_B)).thenReturn(PRESENTER_2_B_ITEM_COUNT);
    when(mockPresenter2.getUpdates(any(), any(), any(ListUpdateCallback.class)))
        .then(getUpdatesAnswer);

    redirectingObserver = new AdapterDataObserver() {
      @Override
      public void onChanged() {
        onChangeEvent.run();
      }

      @Override
      public void onItemRangeChanged(final int positionStart, final int itemCount) {
        fineGrainedEvents.onChanged(positionStart, itemCount, null);
      }

      @Override
      public void onItemRangeChanged(final int positionStart, final int itemCount,
          final Object payload) {
        fineGrainedEvents.onChanged(positionStart, itemCount, payload);
      }

      @Override
      public void onItemRangeInserted(final int positionStart, final int itemCount) {
        fineGrainedEvents.onInserted(positionStart, itemCount);
      }

      @Override
      public void onItemRangeRemoved(final int positionStart, final int itemCount) {
        fineGrainedEvents.onRemoved(positionStart, itemCount);
      }

      @Override
      public void onItemRangeMoved(final int fromPosition, final int toPosition,
          final int itemCount) {
        assertThat(itemCount, is(1));
        fineGrainedEvents.onMoved(fromPosition, toPosition);
      }
    };

    updateDispatcher = updateDispatcher();
    repository1 = mutableRepository(REPOSITORY_VALUE_A);
    repository2 = mutableRepository(REPOSITORY_VALUE_A);

    repositoryAdapter = repositoryAdapter()
        .add(repository1, mockPresenter1)
        .addItem(STATIC_ITEM, mockStaticItemPresenter)
        .add(repository2, mockPresenter2)
        .addAdditionalObservable(updateDispatcher)
        .build();

    repositoryAdapter.startObserving();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.registerAdapterDataObserver(redirectingObserver);
  }

  private static void applyPresenter1FromAToBChanges(@NonNull final ListUpdateCallback callback) {
    // From count 1 to count 3:
    // [X] -> [Y, X]
    callback.onInserted(0, 1);
    // [Y, X] -> [Y, X']
    callback.onChanged(1, 1, PAYLOAD_PRESENTER_1_VALUE_A_TO_B);
    // [Y, X] -> [Y, X', Z]
    callback.onInserted(2, 1);
  }

  private static void applyPresenter1FromBToAChanges(@NonNull final ListUpdateCallback callback) {
    // From count 3 to count 1:
    // [Y, X', Z] -> [Y, Z]
    callback.onRemoved(1, 1);
    // [Y, Z] -> []
    callback.onRemoved(0, 2);
    // [] -> [X]
    callback.onInserted(0, 1);
  }

  private static void applyPresenter1RefreshBChanges(@NonNull final ListUpdateCallback callback) {
    // From count 3 to count 3:
    // [Y, X', Z] -> [Y', X'', Z]
    callback.onChanged(0, 2, PAYLOAD_PRESENTER_1_VALUE_B_REFRESH);
  }

  private static void applyPresenter2FromAToBChanges(
      final int offset, @NonNull final ListUpdateCallback callback) {
    // From count 4 to count 5:
    // [M, N, O, P] -> [P, M, N, O]
    callback.onMoved(3 + offset, offset);
    // [P, M, N, O] -> [P, M]
    callback.onRemoved(2 + offset, 2);
    // [P, M] -> [P, X, Y, Z, M]
    callback.onInserted(1 + offset, 3);
  }

  private static void applyPresenter2FromBToAChanges(
      final int offset, @NonNull final ListUpdateCallback callback) {
    // From count 5 to count 4:
    // [P, X, Y, Z, M] -> [M, P, X, Y, Z]
    callback.onMoved(4 + offset, offset);
    // [M, P, X, Y, Z] -> [M, P]
    callback.onRemoved(2 + offset, 3);
    // [M, P] -> [M, N, O, P]
    callback.onInserted(1 + offset, 2);
  }

  private static void applyPresenter2RefreshBChanges(
      final int offset, @NonNull final ListUpdateCallback callback) {
    // From count 5 to count 5: blanket change
    callback.onChanged(offset, 5, PAYLOAD_PRESENTER_2_VALUE_B_REFRESH);
  }

  @After
  public void tearDown() {
    verify(mockStaticItemPresenter, never()).getUpdates(
        any(), any(), any(ListUpdateCallback.class));
    repositoryAdapter.stopObserving();
    if (repositoryAdapter.hasObservers()) {
      repositoryAdapter.unregisterAdapterDataObserver(redirectingObserver);
    }
  }

  @Test
  public void shouldNotApplyPresenter1ChangesWhenNotObservedUntilGetItemCount() {
    when(mockPresenter1.getItemId(REPOSITORY_VALUE_B, 2)).thenReturn(10L);
    repositoryAdapter.unregisterAdapterDataObserver(redirectingObserver);

    repository1.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    verify(mockPresenter1, never()).getItemCount(any());

    final int itemCount = repositoryAdapter.getItemCount();

    verify(mockPresenter1, never()).getItemCount(REPOSITORY_VALUE_A);
    verify(mockPresenter1).getItemCount(REPOSITORY_VALUE_B);
    assertThat(itemCount, is(
        PRESENTER_1_B_ITEM_COUNT + STATIC_ITEM_COUNT + PRESENTER_2_A_ITEM_COUNT));
    assertThat(repositoryAdapter.getItemId(2), is(10L + STATIC_ITEM_COUNT));
    verify(mockPresenter1, never()).getUpdates(any(), any(), any(ListUpdateCallback.class));
  }

  @Test
  public void shouldNotApplyPresenter2ChangesEvenWhenObservedUntilGetItemCount() {
    when(mockPresenter2.getItemId(REPOSITORY_VALUE_B, 4)).thenReturn(100L);

    repository2.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    verify(mockPresenter1, never()).getItemCount(any());

    final int itemCount = repositoryAdapter.getItemCount();

    verify(mockPresenter2, never()).getItemCount(REPOSITORY_VALUE_A);
    verify(mockPresenter2).getItemCount(REPOSITORY_VALUE_B);
    assertThat(itemCount, is(
        PRESENTER_1_A_ITEM_COUNT + STATIC_ITEM_COUNT + PRESENTER_2_B_ITEM_COUNT));
    assertThat(repositoryAdapter.getItemId(PRESENTER_1_A_ITEM_COUNT + STATIC_ITEM_COUNT + 4),
        is(100L + STATIC_ITEM_COUNT));
    verify(mockPresenter2, never()).getUpdates(any(), any(), any(ListUpdateCallback.class));
  }

  @Test
  public void shouldApplyPresenter1ChangesWithEventsWhenObserved() {
    repositoryAdapter.getItemCount(); // usage
    when(mockPresenter1.getItemId(REPOSITORY_VALUE_B, 2)).thenReturn(10L);

    repository1.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    assertThat(repositoryAdapter.getItemCount(), is(
        PRESENTER_1_B_ITEM_COUNT + STATIC_ITEM_COUNT + PRESENTER_2_A_ITEM_COUNT));
    assertThat(repositoryAdapter.getItemId(2), is(10L + STATIC_ITEM_COUNT));
    verify(mockPresenter1).getUpdates(eq(REPOSITORY_VALUE_A), eq(REPOSITORY_VALUE_B),
        any(ListUpdateCallback.class));
    applyPresenter1FromAToBChanges(verifyingWrapper(fineGrainedEvents));
    verifyNoMoreInteractions(fineGrainedEvents);
    verify(onChangeEvent, never()).run();
  }

  @Test
  public void shouldApplyPresenter2ChangesWithEventsWhenObserved() {
    repositoryAdapter.getItemCount(); // usage
    when(mockPresenter2.getItemId(REPOSITORY_VALUE_B, 4)).thenReturn(100L);

    repository2.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    assertThat(repositoryAdapter.getItemCount(), is(
        PRESENTER_1_A_ITEM_COUNT + STATIC_ITEM_COUNT + PRESENTER_2_B_ITEM_COUNT));
    assertThat(repositoryAdapter.getItemId(PRESENTER_1_A_ITEM_COUNT + STATIC_ITEM_COUNT + 4),
        is(100L + STATIC_ITEM_COUNT));
    verify(mockPresenter2).getUpdates(eq(REPOSITORY_VALUE_A), eq(REPOSITORY_VALUE_B),
        any(ListUpdateCallback.class));
    applyPresenter2FromAToBChanges(PRESENTER_1_A_ITEM_COUNT + STATIC_ITEM_COUNT,
        verifyingWrapper(fineGrainedEvents));
    verifyNoMoreInteractions(fineGrainedEvents);
    verify(onChangeEvent, never()).run();
  }

  @Test
  public void shouldAskBothPresentersOnAdditionalObservableUpdate() {
    repository1.accept(REPOSITORY_VALUE_B);
    repository2.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.getItemCount(); // usage
    verify(mockPresenter1).getItemCount(REPOSITORY_VALUE_B); // consume method call
    verify(mockPresenter2).getItemCount(REPOSITORY_VALUE_B); // consume method call

    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.getItemCount();

    verify(mockPresenter1).getUpdates(eq(REPOSITORY_VALUE_B), eq(REPOSITORY_VALUE_B),
        any(ListUpdateCallback.class));
    verifyNoMoreInteractions(mockPresenter1); // should not have called getItemCount() again
    verify(mockPresenter2).getUpdates(eq(REPOSITORY_VALUE_B), eq(REPOSITORY_VALUE_B),
        any(ListUpdateCallback.class));
    verifyNoMoreInteractions(mockPresenter2); // should not have called getItemCount() again
    applyPresenter1RefreshBChanges(verifyingWrapper(fineGrainedEvents));
    applyPresenter2RefreshBChanges(PRESENTER_1_B_ITEM_COUNT + STATIC_ITEM_COUNT,
        verifyingWrapper(fineGrainedEvents));
    verifyNoMoreInteractions(fineGrainedEvents);
    verify(onChangeEvent, never()).run();
  }

  @Test
  public void shouldSendDataChangeEventAndDisableUpdatesUntilGetItemCount() {
    when(mockPresenter1.getUpdates(any(), any(), any(ListUpdateCallback.class))).thenReturn(false);
    repositoryAdapter.getItemCount(); // usage

    repository1.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    verify(mockPresenter1).getUpdates(eq(REPOSITORY_VALUE_A), eq(REPOSITORY_VALUE_B),
        any(ListUpdateCallback.class));
    verifyZeroInteractions(fineGrainedEvents);
    verify(onChangeEvent).run();

    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();

    verifyZeroInteractions(fineGrainedEvents);
    verifyNoMoreInteractions(onChangeEvent);

    repository2.accept(REPOSITORY_VALUE_B);
    runUiThreadTasksIncludingDelayedTasks();

    verifyZeroInteractions(fineGrainedEvents);
    verifyNoMoreInteractions(onChangeEvent);

    repositoryAdapter.getItemCount(); // usage

    repository2.accept(REPOSITORY_VALUE_A);
    runUiThreadTasksIncludingDelayedTasks();

    verifyNoMoreInteractions(onChangeEvent);
    applyPresenter2FromBToAChanges(PRESENTER_1_B_ITEM_COUNT + STATIC_ITEM_COUNT,
        verifyingWrapper(fineGrainedEvents));
    verifyNoMoreInteractions(fineGrainedEvents);
  }
}
