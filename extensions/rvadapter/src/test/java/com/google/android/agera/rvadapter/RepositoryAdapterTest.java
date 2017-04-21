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

import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.Repositories.repository;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.agera.MutableRepository;
import com.google.android.agera.Repository;
import com.google.android.agera.UpdateDispatcher;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
@SuppressWarnings("unchecked")
public final class RepositoryAdapterTest {
  private static final int MULTI_ITEM_COUNT = 3;
  private static final int STATIC_ITEM_COUNT = 6;
  private static final List<String> REPOSITORY_LIST = asList("a", "b", "c");
  private static final String REPOSITORY_ITEM = "d";
  private static final String ALTERNATIVE_REPOSITORY_ITEM = "e";
  private static final String ITEM = "f";
  @LayoutRes
  private static final int LAYOUT_ID = 3;
  @Mock
  private RepositoryPresenter repositoryPresenter;
  @Mock
  private RepositoryPresenter secondRepositoryPresenter;
  @Mock
  private RepositoryPresenter singleItemRepositoryPresenter;
  @Mock
  private RepositoryPresenter multiItemRepositoryPresenter;
  @Mock
  private RepositoryPresenter zeroItemRepositoryPresenter;
  @Mock
  private LayoutPresenter layoutPresenter;
  @Mock
  private LayoutPresenter secondLayoutPresenter;
  @Mock
  private ViewHolder viewHolder;
  @Mock
  private ViewGroup viewGroup;
  @Mock
  private Context context;
  @Mock
  private LayoutInflater layoutInflater;
  @Mock
  private View view;
  @Mock
  private Activity activity;
  @Mock
  private Application application;
  @Mock
  private AdapterDataObserver observer;
  private UpdateDispatcher updateDispatcher;
  private MutableRepository repository;
  private Repository secondRepository;
  private RepositoryAdapter repositoryAdapter;
  private RepositoryAdapter repositoryAdapterWithoutStatic;
  private Adapter repositoryAdapterWhileResumed;
  private Adapter repositoryAdapterWhileStarted;

  @Before
  public void setUp() {
    initMocks(this);
    updateDispatcher = updateDispatcher();
    repository = mutableRepository(REPOSITORY_ITEM);
    secondRepository = repository(REPOSITORY_LIST);

    when(activity.getApplication()).thenReturn(application);
    when(viewGroup.getContext()).thenReturn(context);
    when(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).thenReturn(layoutInflater);
    when(layoutInflater.inflate(LAYOUT_ID, viewGroup, false)).thenReturn(view);
    when(repositoryPresenter.getItemCount(REPOSITORY_ITEM)).thenReturn(1);
    when(secondRepositoryPresenter.getItemCount(REPOSITORY_LIST)).thenReturn(3);
    when(singleItemRepositoryPresenter.getItemCount(ITEM)).thenReturn(1);
    when(multiItemRepositoryPresenter.getItemCount(ITEM)).thenReturn(MULTI_ITEM_COUNT);
    when(zeroItemRepositoryPresenter.getItemCount(ITEM)).thenReturn(0);

    repositoryAdapter = repositoryAdapter()               // total | static | stable IDs assigned
        .add(repository, repositoryPresenter)             //     1 | 0      |
        .add(secondRepository, secondRepositoryPresenter) //     4 | 0      |
        .addLayout(layoutPresenter)                       //     5 | 1      | [@4] 0
        .addItem(ITEM, singleItemRepositoryPresenter)     //     6 | 2      | [@5] 1
        .addItem(ITEM, multiItemRepositoryPresenter)      //     9 | 5      | [@6-8] 2-4
        .addLayout(secondLayoutPresenter)                 //    10 | 6      | [@9] 5
        .addItem(ITEM, zeroItemRepositoryPresenter)       //    10 | 6      |
        .addAdditionalObservable(updateDispatcher)
        .build();
    repositoryAdapterWithoutStatic = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .build();
  }

  @After
  public void tearDown() {
    verify(singleItemRepositoryPresenter, never()).getItemId(any(), anyInt());
    verify(multiItemRepositoryPresenter, never()).getItemId(any(), anyInt());
    verify(zeroItemRepositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test
  public void shouldReturnItemCountFromPresenters() {
    assertThat(repositoryAdapter.getItemCount(), is(10));
  }

  @Test
  public void shouldReturnItemIdFromFirstPresenter() {
    when(repositoryPresenter.getItemId(REPOSITORY_ITEM, 0)).thenReturn(10L);
    assertThat(repositoryAdapter.getItemId(0), is(10L + STATIC_ITEM_COUNT));

    verify(secondRepositoryPresenter, never()).getItemId(any(), anyInt());
  }


  @Test
  public void shouldReturnItemIdFromFirstPresenterWithoutStatic() {
    when(repositoryPresenter.getItemId(REPOSITORY_ITEM, 0)).thenReturn(10L);
    assertThat(repositoryAdapterWithoutStatic.getItemId(0), is(10L));
  }

  @Test
  public void shouldReturnItemIdFromSecondPresenter() {
    when(secondRepositoryPresenter.getItemId(REPOSITORY_LIST, 0)).thenReturn(11L);
    assertThat(repositoryAdapter.getItemId(1), is(11L + STATIC_ITEM_COUNT));

    verify(repositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test
  public void shouldReturnItemIdForStaticItems() {
    // See comments at repositoryAdapter initialization in setUp()
    for (int i = 4; i <= 9; i++) {
      assertThat(repositoryAdapter.getItemId(i), is(i - 4L));
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void shouldThrowExceptionForOutOfBoundsIndex() {
    repositoryAdapter.getItemId(10);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void shouldThrowExceptionForNegativeIndex() {
    repositoryAdapter.getItemId(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionForAdapterWithoutRepositories() {
    repositoryAdapter()
        .addAdditionalObservable(updateDispatcher)
        .build();
  }

  @Test
  public void shouldReturnItemViewTypeFromFirstPresenter() {
    when(repositoryPresenter.getLayoutResId(REPOSITORY_ITEM, 0)).thenReturn(1);
    assertThat(repositoryAdapter.getItemViewType(0), is(1));
  }

  @Test
  public void shouldReturnItemViewTypeFromSecondPresenter() {
    when(secondRepositoryPresenter.getLayoutResId(REPOSITORY_LIST, 0)).thenReturn(2);
    assertThat(repositoryAdapter.getItemViewType(1), is(2));
  }

  @Test
  public void shouldReturnItemViewTypeFromLayoutPresenter() {
    when(layoutPresenter.getLayoutResId()).thenReturn(34);
    assertThat(repositoryAdapter.getItemViewType(4), is(34));
  }

  @Test
  public void shouldReturnItemViewTypeFromItemPresenter() {
    when(multiItemRepositoryPresenter.getLayoutResId(ITEM, 2)).thenReturn(42);
    assertThat(repositoryAdapter.getItemViewType(8), is(42));
  }

  @Test
  public void shouldCreateViewHolder() {
    assertThat(repositoryAdapter.onCreateViewHolder(viewGroup, LAYOUT_ID).itemView, is(view));
  }

  @Test
  public void shouldNotifyChangeOnStartObservingAfterUsage() {
    repositoryAdapter.registerAdapterDataObserver(observer);
    repositoryAdapter.getItemCount(); // "usage".

    // Because the adapter is not observing any data change, after the usage, it doesn't know of any
    // change of data, so for extra guarantee, it'll notify change as soon as it starts observing.
    repositoryAdapter.startObserving();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    verify(observer).onChanged();
  }

  @Test
  public void shouldNotifyChangeOnAdditionalObservablesUpdateWhenObservingAndObserved() {
    repositoryAdapter.startObserving();
    repositoryAdapter.registerAdapterDataObserver(observer);
    runUiThreadTasksIncludingDelayedTasks();
    verify(observer, never()).onChanged();
    repositoryAdapter.getItemCount(); // usage before event

    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    verify(observer).onChanged();
  }

  @Test
  public void shouldCallRecycleForOnViewRecycled() {
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    repositoryAdapter.onBindViewHolder(viewHolder, 0);

    repositoryAdapter.onViewRecycled(viewHolder);

    verify(repositoryPresenter).recycle(viewHolder);
  }

  @Test
  public void shouldCallRecycleForOnFailedToRecycleView() {
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    repositoryAdapter.onBindViewHolder(viewHolder, 0);

    repositoryAdapter.onFailedToRecycleView(viewHolder);

    verify(repositoryPresenter).recycle(viewHolder);
  }

  @Test
  public void shouldCallRecycleForOnViewRecycledForSecondPresenter() {
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    repositoryAdapter.onBindViewHolder(viewHolder, 1);

    repositoryAdapter.onViewRecycled(viewHolder);

    verify(secondRepositoryPresenter).recycle(viewHolder);
  }

  @Test
  public void shouldCallRecycleForOnFailedToRecycleViewForSecondPresenter() {
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    repositoryAdapter.onBindViewHolder(viewHolder, 1);

    repositoryAdapter.onFailedToRecycleView(viewHolder);

    verify(secondRepositoryPresenter).recycle(viewHolder);
  }


  @Test
  public void shouldCallRecycleForOnViewRecycledForLayoutPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 4);

    repositoryAdapter.onViewRecycled(viewHolder);

    verify(layoutPresenter).recycle(view);
  }

  @Test
  public void shouldCallRecycleForOnFailedToRecycleViewForLayoutPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 4);

    repositoryAdapter.onFailedToRecycleView(viewHolder);

    verify(layoutPresenter).recycle(view);
  }

  @Test
  public void shouldCallRecycleForOnViewRecycledForItemPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 5);

    repositoryAdapter.onViewRecycled(viewHolder);

    verify(singleItemRepositoryPresenter).recycle(viewHolder);
  }

  @Test
  public void shouldCallRecycleForOnFailedToRecycleViewForItemPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();
    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 5);

    repositoryAdapter.onFailedToRecycleView(viewHolder);

    verify(singleItemRepositoryPresenter).recycle(viewHolder);
  }

  @Test
  public void shouldBindLayoutPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 4);

    verify(layoutPresenter).bind(view);
  }

  @Test
  public void shouldBindItemPresenter() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    final ViewHolder viewHolder = new ViewHolder(view) {};
    repositoryAdapter.onBindViewHolder(viewHolder, 7);

    verify(multiItemRepositoryPresenter).bind(ITEM, 1, viewHolder);
  }

  @Test
  public void shouldUpdateOnChangingRepositoryWhenObserving() {
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repositoryAdapter.startObserving();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    repositoryAdapter.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(ALTERNATIVE_REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldNotUpdateOnChangingRepositoryWhenNotObserving() {
    repositoryAdapter.getItemCount(); //Trigger a refresh

    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();

    repositoryAdapter.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldUpdateOnChangingRepositoryWhenStarted() {
    repositoryAdapterWhileStarted = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapterWhileStarted.getItemCount(); //Trigger a refresh

    setActivityToStarted();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToStopped();

    repositoryAdapterWhileStarted.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(ALTERNATIVE_REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldNotUpdateOnChangingRepositoryWhenNotStarted() {
    repositoryAdapterWhileStarted = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    repositoryAdapterWhileStarted.getItemCount(); //Trigger a refresh

    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();

    repositoryAdapterWhileStarted.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldUpdateOnChangingRepositoryWhenResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    when(repositoryPresenter.getItemCount(ALTERNATIVE_REPOSITORY_ITEM)).thenReturn(1);
    repositoryAdapterWhileResumed.getItemCount(); //Trigger a refresh

    setActivityToResumed();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToPaused();

    repositoryAdapterWhileResumed.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(ALTERNATIVE_REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldNotUpdateOnChangingRepositoryWhenNotResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    repositoryAdapterWhileResumed.getItemCount(); //Trigger a refresh

    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();

    repositoryAdapterWhileResumed.onBindViewHolder(viewHolder, 0);

    verify(repositoryPresenter).bind(REPOSITORY_ITEM, 0, viewHolder);
  }

  @Test
  public void shouldDoNothingOnActivityCreatedForWhileResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToCreated();
  }

  @Test
  public void shouldDoNothingOnActivityDestroyedForWhileResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToDestroyed();
  }

  @Test
  public void shouldDoNothingOnActivityCreatedForWhileStarted() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToCreated();
  }

  @Test
  public void shouldDoNothingOnActivityDestroyedForWhileStarted() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToDestroyed();
  }

  @Test
  public void shouldDoNothingOnActivityStoppedForWhileResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToStopped();
  }

  @Test
  public void shouldDoNothingOnActivityStartedForWhileResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToStarted();
  }

  @Test
  public void shouldDoNothingOnActivityResumedForWhileStarted() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToResumed();
  }

  @Test
  public void shouldDoNothingOnActivityPausedForWhileStarted() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToPaused();
  }


  @Test
  public void shouldDoNothingOnSaveInstanceStateForWhileResumed() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileResumed(activity);
    runUiThreadTasksIncludingDelayedTasks();
    saveActivityInstanceState();
  }

  @Test
  public void shouldDoNothingOnSaveInstanceStateForWhileStarted() {
    repositoryAdapterWhileResumed = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .whileStarted(activity);
    runUiThreadTasksIncludingDelayedTasks();
    saveActivityInstanceState();
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToCreated() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityCreated(activity, null);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToDestroyed() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityDestroyed(activity);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToResumed() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityResumed(activity);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToPaused() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityPaused(activity);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToStarted() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityStarted(activity);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void setActivityToStopped() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityStopped(activity);
  }

  @TargetApi(ICE_CREAM_SANDWICH)
  private void saveActivityInstanceState() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivitySaveInstanceState(activity, null);
  }
}
