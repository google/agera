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
import static com.google.android.agera.Repositories.repository;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;
import static org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks;

import com.google.android.agera.MutableRepository;
import com.google.android.agera.Repository;
import com.google.android.agera.UpdateDispatcher;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.AdapterDataObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public final class RepositoryAdapterTest {
  private static final List<String> REPOSITORY_LIST = asList("a", "b", "c");
  private static final String REPOSITORY_ITEM = "d";
  private static final String ALTERNATIVE_REPOSITORY_ITEM = "e";
  @LayoutRes
  public static final int LAYOUT_ID = 3;
  @Mock
  private RepositoryPresenter repositoryPresenter;
  @Mock
  private RepositoryPresenter secondRepositoryPresenter;
  @Mock
  private RecyclerView.ViewHolder viewHolder;
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

    repositoryAdapter = repositoryAdapter()
        .add(repository, repositoryPresenter)
        .add(secondRepository, secondRepositoryPresenter)
        .addAdditionalObservable(updateDispatcher)
        .build();
  }

  @Test
  public void shouldReturnItemCountFromPresenters() {
    assertThat(repositoryAdapter.getItemCount(), is(4));
  }

  @Test
  public void shouldReturnItemIdFromFirstPresenter() {
    when(repositoryPresenter.getItemId(REPOSITORY_ITEM, 0)).thenReturn(1L);
    assertThat(repositoryAdapter.getItemId(0), is(1L));

    verify(secondRepositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test
  public void shouldReturnItemIdFromSecondPresenter() {
    when(secondRepositoryPresenter.getItemId(REPOSITORY_LIST, 0)).thenReturn(2L);
    assertThat(repositoryAdapter.getItemId(1), is(2L));

    verify(repositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void shouldThrowExceptionForOutOfBoundsIndex() {
    repositoryAdapter.getItemId(4);
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

    verify(secondRepositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test
  public void shouldReturnItemViewTypeFromSecondPresenter() {
    when(secondRepositoryPresenter.getLayoutResId(REPOSITORY_LIST, 0)).thenReturn(2);
    assertThat(repositoryAdapter.getItemViewType(1), is(2));

    verify(repositoryPresenter, never()).getItemId(any(), anyInt());
  }

  @Test
  public void shouldCreateViewHolder() {
    assertThat(repositoryAdapter.onCreateViewHolder(viewGroup, LAYOUT_ID).itemView, is(view));
  }

  @Test
  public void shouldUpdateOnAdditionalObservablesWhenObserving() {
    repositoryAdapter.registerAdapterDataObserver(observer);

    repositoryAdapter.startObserving();
    updateDispatcher.update();
    runUiThreadTasksIncludingDelayedTasks();
    repositoryAdapter.stopObserving();

    verify(observer, times(2)).onChanged();
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

    setActivityToVisible();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToInvisible();

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

    setActivityToActive();
    repository.accept(ALTERNATIVE_REPOSITORY_ITEM);
    runUiThreadTasksIncludingDelayedTasks();
    setActivityToInactive();

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

    private void setActivityToActive() {
      final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
          forClass(ActivityLifecycleCallbacks.class);

     verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityResumed(activity);
  }

  private void setActivityToInactive() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityPaused(activity);
  }

  private void setActivityToVisible() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityStarted(activity);
  }

  private void setActivityToInvisible() {
    final ArgumentCaptor<ActivityLifecycleCallbacks> captor =
        forClass(ActivityLifecycleCallbacks.class);

    verify(application).registerActivityLifecycleCallbacks(captor.capture());

    final ActivityLifecycleCallbacks callbacks = captor.getValue();

    callbacks.onActivityStopped(activity);
  }
}
