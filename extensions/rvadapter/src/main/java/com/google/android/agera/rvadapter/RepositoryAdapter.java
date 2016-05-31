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
package com.google.android.agera.rvadapter;

import static com.google.android.agera.Observables.compositeObservable;
import static com.google.android.agera.Preconditions.checkArgument;
import static com.google.android.agera.Preconditions.checkNotNull;

import com.google.android.agera.Observable;
import com.google.android.agera.Repository;
import com.google.android.agera.Updatable;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A specialized {@link RecyclerView.Adapter} that presents the data from a sequence of
 * {@link Repository Repositories}. The sequence is static but each repository can be presented in
 * zero or more item views as specified by their associated {@link RepositoryPresenter}s.
 *
 * <p>This adapter is also an {@link Updatable}. It will observe the repositories as well as any
 * additional {@link Observable}s given at creation, and pass on the {@link #update}s as
 * {@linkplain #notifyDataSetChanged data set changes}. The adapter will only be observing the
 * {@link Observable}s between the calls to {@link #startObserving()} and {@link #stopObserving()}.
 * Typically {@link #startObserving()} is called in {@link Activity#onStart} and
 * {@link #stopObserving} in {@link Activity#onStop}. Calls to {@link #startObserving()} and
 * {@link #stopObserving} has to be paired. If {@link #stopObserving()} is not called the
 * {@code RepositoryAdapter} will stay active and will potentially leak memory or be unnecessarily
 * wasteful with system resources.
 *
 * <p>This adapter can be subclassed to handle special requirements, such as creating custom view
 * holders, handling item view lifecycle events, and implementing additional interfaces.
 */
public class RepositoryAdapter extends RecyclerView.Adapter<ViewHolder>
    implements Updatable {

  /**
   * Builds a {@link RepositoryAdapter}.
   */
  @NonNull
  public static Builder repositoryAdapter() {
    return new Builder();
  }

  /**
   * Builds a {@link RepositoryAdapter}.
   */
  public static final class Builder {
    @NonNull
    final List<Repository<Object>> repositories = new ArrayList<>();
    @NonNull
    final List<RepositoryPresenter<Object>> presenters = new ArrayList<>();
    @NonNull
    final List<Observable> observables = new ArrayList<>();

    /**
     * Specifies that the {@link RepositoryAdapter} being built should present the given
     * {@code repository} next (after all previously added repositories), using the given
     * {@code presenter} for any presentation logic.
     *
     * @param repository The repository to be presented. This can be the same as a previously added
     *     repository; this makes the resulting {@link RepositoryAdapter} present the same data in
     *     different positions and/or different ways.
     * @param presenter The repository presenter associated with the {@code repository} at this
     *     position.
     * @return This instance, for chaining.
     */
    @NonNull
    public <T> Builder add(@NonNull final Repository<T> repository,
        @NonNull final RepositoryPresenter<T> presenter) {
      @SuppressWarnings("unchecked")
      final Repository<Object> untypedRepository = (Repository<Object>) checkNotNull(repository);
      repositories.add(untypedRepository);
      @SuppressWarnings("unchecked")
      final RepositoryPresenter<Object> untypedPresenter =
          (RepositoryPresenter<Object>) checkNotNull(presenter);
      presenters.add(untypedPresenter);
      observables.add(untypedRepository);
      return this;
    }

    /**
     * Specifies that the {@link RepositoryAdapter} being built should also observe the given
     * {@code observable} during its active time (when it {@link #hasObservers()}), in addition to
     * the repositories added via {@link #add}. Events from this observable will also trigger a data
     * set change. Use this when events that could happen without affecting any repository data may
     * affect how the presenters work.
     *
     * @param observable The observable to be observed. This should not be any repository added via
     *     {@link #add}; they will be observed automatically.
     * @return This instance, for chaining.
     */
    @NonNull
    public Builder addAdditionalObservable(@NonNull final Observable observable) {
      observables.add(checkNotNull(observable));
      return this;
    }

    /**
     * Builds the {@link RepositoryAdapter} that presents the provided repositories in order and
     * observes the repositories as well as any additional observables. If a subclass of
     * {@link RepositoryAdapter} is needed to cover special requirements, client code should create
     * a new instance of the subclass, passing this builder to the base constructor
     * {@link RepositoryAdapter#RepositoryAdapter(Builder)}.
     */
    @NonNull
    public RepositoryAdapter build() {
      return new RepositoryAdapter(this);
    }

    /**
     * Builds the {@link RepositoryAdapter} that presents the provided repositories in order and
     * observes the repositories as well as any additional observables while the provided
     * {@link Activity} is resumed (between {@link Activity#onResume()} and
     * {@link Activity#onPause()}).
     * <p>
     * Note: Can only be called from {@link Activity#onCreate} ()}
     */
    @TargetApi(14)
    @NonNull
    public Adapter<ViewHolder> whileResumed(@NonNull final Activity activity) {
      final RepositoryAdapter repositoryAdapter = new RepositoryAdapter(this);
      activity.getApplication().registerActivityLifecycleCallbacks(
          new WhileResumedActivityLifecycleCallbacks(activity, repositoryAdapter));
      return repositoryAdapter;
    }

    /**
     * Builds the {@link RepositoryAdapter} that presents the provided repositories in order and
     * observes the repositories as well as any additional observables while the provided
     * {@link Activity} is started (between (between {@link Activity#onStart()} and
     * {@link Activity#onStop()}).
     * <p>
     * Note: Can only be called from {@link Activity#onCreate} ()}
     */
    @TargetApi(14)
    @NonNull
    public Adapter<ViewHolder> whileStarted(@NonNull final Activity activity) {
      final RepositoryAdapter repositoryAdapter = new RepositoryAdapter(this);
      activity.getApplication().registerActivityLifecycleCallbacks(
          new WhileStartedActivityLifecycleCallbacks(activity, repositoryAdapter));
      return repositoryAdapter;
    }

    private Builder() {}

    @TargetApi(14)
    private static abstract class AdapterActivityLifecycleCallbacks
        implements ActivityLifecycleCallbacks {
      @NonNull
      private final Activity activity;

      protected AdapterActivityLifecycleCallbacks(@NonNull final Activity activity) {
        this.activity = checkNotNull(activity);
      }

      @Override
      public final void onActivityCreated(final Activity activity,
          final Bundle savedInstanceState) {}

      @Override
      public void onActivityStarted(final Activity activity) {}

      @Override
      public void onActivityResumed(final Activity activity) {}

      @Override
      public void onActivityPaused(final Activity activity) {}

      @Override
      public void onActivityStopped(final Activity activity) {}

      @Override
      public final void onActivitySaveInstanceState(final Activity activity,
          final Bundle outState) {}

      @Override
      public final void onActivityDestroyed(final Activity anyActivity) {
        if (activity == anyActivity) {
          activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
      }
    }

    private static class WhileStartedActivityLifecycleCallbacks
        extends AdapterActivityLifecycleCallbacks {
      private final Activity activity;
      private final RepositoryAdapter repositoryAdapter;

      public WhileStartedActivityLifecycleCallbacks(final Activity activity,
          final RepositoryAdapter repositoryAdapter) {
        super(activity);
        this.activity = activity;
        this.repositoryAdapter = repositoryAdapter;
      }

      @Override
      public void onActivityStarted(final Activity anyActivity) {
        if (anyActivity == activity) {
          repositoryAdapter.startObserving();
        }
      }

      @Override
      public void onActivityStopped(final Activity anyActivity) {
          if (anyActivity == activity) {
            repositoryAdapter.stopObserving();
          }
      }
    }

    private static class WhileResumedActivityLifecycleCallbacks
        extends AdapterActivityLifecycleCallbacks {
      private final Activity activity;
      private final RepositoryAdapter repositoryAdapter;

      public WhileResumedActivityLifecycleCallbacks(final Activity activity,
          final RepositoryAdapter repositoryAdapter) {
        super(activity);
        this.activity = activity;
        this.repositoryAdapter = repositoryAdapter;
      }

      @Override
      public void onActivityResumed(final Activity anyActivity) {
        if (anyActivity == activity) {
          repositoryAdapter.startObserving();
        }
      }

      @Override
      public void onActivityPaused(final Activity anyActivity) {
        if (anyActivity == activity) {
          repositoryAdapter.stopObserving();
        }
      }
    }
  }

  private final int repositoryCount;
  @NonNull
  private final Repository<Object>[] repositories;
  @NonNull
  private final Object[] data;
  @NonNull
  private final RepositoryPresenter<Object>[] presenters;
  @NonNull
  private final Observable observable;
  @NonNull
  private final int[] endPositions;

  private boolean dataInvalid;
  private int resolvedRepositoryIndex;
  private int resolvedItemIndex;

  public RepositoryAdapter(@NonNull final Builder builder) {
    final int count = builder.repositories.size();
    checkArgument(count > 0, "Must add at least one repository");
    checkArgument(builder.presenters.size() == count,
        "Unexpected repository and presenter count mismatch");

    @SuppressWarnings("unchecked")
    final Repository<Object>[] repositories = builder.repositories.toArray(
        (Repository<Object>[]) new Repository[count]);

    @SuppressWarnings("unchecked")
    final RepositoryPresenter<Object>[] presenters = builder.presenters.toArray(
        (RepositoryPresenter<Object>[]) new RepositoryPresenter[count]);

    final Observable[] observables =
        builder.observables.toArray(new Observable[builder.observables.size()]);
    this.data = new Object[count];
    this.repositoryCount = count;
    this.repositories = repositories;
    this.presenters = presenters;
    this.observable = compositeObservable(observables);
    this.endPositions = new int[count];
    this.dataInvalid = true;
  }

  /**
   * Starts observing any {@link Repository} added. Calls to this method must be paired with calls
   * to {@link #stopObserving()}.
   */
  public final void startObserving() {
    observable.addUpdatable(this);
    update();
  }

  /**
   * Stops observing any {@link Repository} added. Calls to this method must be paired with calls to
   * {@link #startObserving()}.
   */
  public final void stopObserving() {
    observable.removeUpdatable(this);
  }

  /**
   * Invalidates the data set so {@link RecyclerView} will schedule a rebind of all data.
   */
  @Override
  public final void update() {
    dataInvalid = true;
    notifyDataSetChanged();
  }

  @Override
  public final int getItemCount() {
    if (dataInvalid) {
      int lastEndPosition = 0;
      for (int i = 0; i < repositoryCount; i++) {
        data[i] = repositories[i].get();
        lastEndPosition += presenters[i].getItemCount(data[i]);
        endPositions[i] = lastEndPosition;
      }
      dataInvalid = false;
    }
    return endPositions[repositoryCount - 1];
  }

  @Override
  public final int getItemViewType(final int position) {
    resolveIndices(position);
    int resolvedRepositoryIndex = this.resolvedRepositoryIndex;
    int resolvedItemIndex = this.resolvedItemIndex;
    return presenters[resolvedRepositoryIndex].getLayoutResId(
        data[resolvedRepositoryIndex], resolvedItemIndex);
  }

  @Override
  public final long getItemId(final int position) {
    resolveIndices(position);
    int resolvedRepositoryIndex = this.resolvedRepositoryIndex;
    int resolvedItemIndex = this.resolvedItemIndex;
    return presenters[resolvedRepositoryIndex].getItemId(
        data[resolvedRepositoryIndex], resolvedItemIndex);
  }

  /**
   * Creates a new view holder holding the view inflated from the provided {@code layoutResourceId}.
   * This implementation inflates the view using the {@code parent}'s context and creates a holder
   * that adds no value to the base class {@link ViewHolder}. Override this method for
   * any special requirements.
   */
  @Override
  public ViewHolder onCreateViewHolder(final ViewGroup parent,
      final int layoutResourceId) {
    return new RecyclerView.ViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(layoutResourceId, parent, false)) {};
  }

  @Override
  public final void onBindViewHolder(final ViewHolder holder, final int position) {
    resolveIndices(position);
    int resolvedRepositoryIndex = this.resolvedRepositoryIndex;
    int resolvedItemIndex = this.resolvedItemIndex;
    presenters[resolvedRepositoryIndex].bind(
        data[resolvedRepositoryIndex], resolvedItemIndex, holder);
  }

  /**
   * Converts the given overall adapter {@code position} into {@link #resolvedRepositoryIndex}
   * and {@link #resolvedItemIndex}.
   */
  private void resolveIndices(final int position) {
    int itemCount = getItemCount(); // This conveniently rebuilds endPositions if necessary.
    if (position < 0 || position >= itemCount) {
      throw new IndexOutOfBoundsException(
          "Asked for position " + position + " while count is " + itemCount);
    }

    int arrayIndex = Arrays.binarySearch(endPositions, position);
    if (arrayIndex >= 0) {
      // position is the end position of repositories[arrayIndex], so it falls in the range
      // of the next repository that advances past it (there may be some empty repositories).
      do {
        arrayIndex++;
      } while (endPositions[arrayIndex] == position); // will not OOB after the initial bound check.
    } else {
      // position is before the end position of repositories[~arrayIndex], so it falls in the
      // range of the repository at ~arrayIndex.
      arrayIndex = ~arrayIndex;
    }

    resolvedRepositoryIndex = arrayIndex;
    resolvedItemIndex = arrayIndex == 0 ? position : position - endPositions[arrayIndex - 1];
  }
}
