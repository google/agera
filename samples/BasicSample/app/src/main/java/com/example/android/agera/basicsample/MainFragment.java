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

import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * The MainFragment contains a {@link ListView} that displays a list of usernames. The usernames
 * are loaded in {@link Fragment#onResume()} and there is a {@link SwipeRefreshLayout} allowing the
 * user to manually refresh the list of usernames.
 * <P >
 * This is set up as an {@link Updatable} as changes in the {@link UsernamesRepository} should
 * trigger the MainFragment to redraw itself.
 */
public class MainFragment extends Fragment implements Updatable {

  /**
   * The {@link SwipeRefreshLayout.OnRefreshListener} is also an {@link Observable}. It is
   * observed by the {@link UsernamesRepository}, an update is triggered whenever
   * {@link SwipeRefreshLayout.OnRefreshListener#onRefresh()} is fired.
   */
  private OnRefreshObservable refreshObservable;

  /**
   * The {@link UsernamesRepository} takes care of providing the data to this fragment. It is an
   * {@link Updatable} because changes in the {@link OnRefreshObservable} require that it updates
   * its list of usernames. It is also an {@link Observable} and is observed by this MainFragment.
   */
  private UsernamesRepository usernamesRepository;

  private ListAdapter listAdapter;

  private ListView listView;

  private SwipeRefreshLayout swipeRefreshLayout;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.main_frag, container, false);

    listView = (ListView) root.findViewById(R.id.list);

    // Set pull to refresh as an observable and attach it to the view
    refreshObservable = new OnRefreshObservable();
    swipeRefreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.refresh_layout);
    swipeRefreshLayout.setColorSchemeColors(
        ContextCompat.getColor(getActivity(), R.color.colorPrimary),
        ContextCompat.getColor(getActivity(), R.color.colorAccent),
        ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
    swipeRefreshLayout.setOnRefreshListener(refreshObservable);

    // Initialise the repository
    usernamesRepository = new UsernamesRepository(new UsernamesFetcher());

    return root;
  }

  @Override
  public void onResume() {
    super.onResume();

    // We make sure the repository observes the refresh listener
    refreshObservable.addUpdatable(usernamesRepository);

    /**
     * We make sure the main fragment observes the repository. This will also trigger the
     * repository to update itself, via
     * {@link UsernamesRepository#firstUpdatableAdded(UpdateDispatcher)}.
     */
    usernamesRepository.addUpdatable(this);

    /**
     * We update the UI to show the data is being updated. We need to wait for the
     * {@link swipeRefreshLayout} to be ready before asking it to show itself as refreshing.
     */
    swipeRefreshLayout.post(new Runnable() {
      @Override
      public void run() {
        swipeRefreshLayout.setRefreshing(true);
      }
    });
  }

  @Override
  public void onPause() {
    super.onPause();
    // We remove the observations to avoid triggering updates when they aren't needed
    refreshObservable.removeUpdatable(usernamesRepository);
    usernamesRepository.removeUpdatable(this);
  }

  /**
   * As this MainFragment is observing the {@link UsernamesRepository}, this is triggered
   * whenever the {@link UsernamesRepository} updates itself.
   */
  @Override
  public void update() {
    /**
     * We update the UI to show the data has been updated. We need to wait for the
     * {@link swipeRefreshLayout} to be ready before asking it to show itself as not refreshing.
     */
    swipeRefreshLayout.post(new Runnable() {
      @Override
      public void run() {
        swipeRefreshLayout.setRefreshing(false);
      }
    });

    // Check error status
    if (usernamesRepository.isError()) {
      // Show error message, do not update list as we still want to show the last known list of
      // usernames
      Toast.makeText(getContext(), getResources().getString(R.string.error),
          Toast.LENGTH_LONG).show();
    } else {
      // Update the list of usernames
      listAdapter = new ArrayAdapter<String>(getContext(),
          android.R.layout.simple_list_item_1, usernamesRepository.get());
      listView.setAdapter(listAdapter);
    }
  }
}
