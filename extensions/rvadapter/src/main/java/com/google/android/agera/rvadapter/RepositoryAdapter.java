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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.google.android.agera.Observable;
import com.google.android.agera.Repository;
import com.google.android.agera.Updatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

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
    final List<Part> parts = new ArrayList<>();
    @NonNull
    final List<Observable> observables = new ArrayList<>();
    int staticItemCount = 0;

    /**
     * Specifies that the {@link RepositoryAdapter} being built should present the given
     * {@code repository} next (after all previously added repositories, items and static layouts),
     * using the given {@code presenter} for any presentation logic.
     *
     * @param repository The repository to be presented. This can be the same as a previously added
     *     repository; this makes the resulting {@link RepositoryAdapter} present the same data in
     *     different positions and/or different ways.
     * @param presenter The repository presenter associated with the {@code repository} at this
     *     position of the {@link RepositoryAdapter}.
     * @return This instance, for chaining.
     */
    @NonNull
    public <T> Builder add(@NonNull final Repository<T> repository,
        @NonNull final RepositoryPresenter<T> presenter) {
      parts.add(new RepositoryPart(repository, presenter));
      observables.add(repository);
      return this;
    }

    /**
     * Specifies that the {@link RepositoryAdapter} being built should present the given static
     * {@code item} next (after all previously added repositories, static items and layouts),
     * using the given {@code presenter} for any presentation logic. Added items will be considered
     * static, in the following semantics:
     * <ul>
     * <li>The number of item views to be presented is decided immediately by calling
     *     {@code presenter.getItemCount(item)}; if this method returns 0 then this item-presenter
     *     pair is not added to the {@link RepositoryAdapter} at all.
     * <li>{@link RepositoryPresenter#getItemId} is not called; stable IDs of the presented item
     *     views are generated by internal logic based on the position of this presenter and the
     *     index of the item view within this presenter.
     * </ul>
     * <p>NOTE: adding an item with this method (as with {@link #addLayout}) will offset the stable
     * IDs in other {@link RepositoryPresenter}s.
     *
     * @param item A static item. This can be the same as a previously added item; this makes the
     *     resulting {@link RepositoryAdapter} present the same data in different positions and/or
     *     different ways.
     * @param presenter The repository presenter associated with the {@code item} at this position
     *     of the {@link RepositoryAdapter}.
     * @return This instance, for chaining.
     */
    @NonNull
    public <T> Builder addItem(@NonNull final T item,
        @NonNull final RepositoryPresenter<T> presenter) {
      final int thisItemCount = presenter.getItemCount(item);
      if (thisItemCount > 0) {
        parts.add(new ItemPart(staticItemCount, thisItemCount, item, presenter));
        staticItemCount += thisItemCount;
      }
      return this;
    }

    /**
     * Specifies that the {@link RepositoryAdapter} being built should present the given
     * {@link LayoutPresenter} next (after all previously added repositories, static items and
     * layouts).
     *
     * <p>NOTE: adding a layout with this method (as with {@link #addItem}) will offset the stable
     * IDs in other {@link RepositoryPresenter}s.
     *
     * @param presenter The layout presenter to be presented at this position of the
     *     {@link RepositoryAdapter}.
     * @return This instance, for chaining.
     */
    @NonNull
    public Builder addLayout(@NonNull final LayoutPresenter presenter) {
      parts.add(new LayoutPart(staticItemCount, presenter));
      staticItemCount++;
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
    private abstract static class AdapterActivityLifecycleCallbacks
        implements ActivityLifecycleCallbacks {
      @NonNull
      final Activity activity;
      @NonNull
      final RepositoryAdapter repositoryAdapter;

      AdapterActivityLifecycleCallbacks(@NonNull final Activity activity,
          @NonNull final RepositoryAdapter repositoryAdapter) {
        this.activity = checkNotNull(activity);
        this.repositoryAdapter = repositoryAdapter;
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

      WhileStartedActivityLifecycleCallbacks(final Activity activity,
          @NonNull final RepositoryAdapter repositoryAdapter) {
        super(activity, repositoryAdapter);
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

      WhileResumedActivityLifecycleCallbacks(final Activity activity,
          @NonNull final RepositoryAdapter repositoryAdapter) {
        super(activity, repositoryAdapter);
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

  private final int partCount;
  private final int staticItemCount;
  @NonNull
  private final Part[] parts;
  @NonNull
  private final Map<ViewHolder, Part> partForViewHolder;
  @NonNull
  private final Observable observable;
  @NonNull
  private final int[] endPositions;

  private boolean dataInvalid;
  private int resolvedPartIndex;
  private int resolvedItemIndex;

  public RepositoryAdapter(@NonNull final Builder builder) {
    final int partCount = builder.parts.size();
    checkArgument(partCount > 0, "Must add at least one part");

    this.partCount = partCount;
    this.staticItemCount = builder.staticItemCount;
    this.parts = builder.parts.toArray(new Part[partCount]);
    this.partForViewHolder = new IdentityHashMap<>();
    this.observable = compositeObservable(
        builder.observables.toArray(new Observable[builder.observables.size()]));
    this.endPositions = new int[partCount];
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
      for (int i = 0; i < partCount; i++) {
        lastEndPosition += parts[i].getItemCount();
        endPositions[i] = lastEndPosition;
      }
      dataInvalid = false;
    }
    return endPositions[partCount - 1];
  }

  @Override
  public final int getItemViewType(final int position) {
    resolveIndices(position);
    return parts[resolvedPartIndex].getLayoutResId(resolvedItemIndex);
  }

  @Override
  public final long getItemId(final int position) {
    resolveIndices(position);
    return parts[resolvedPartIndex].getItemId(resolvedItemIndex, staticItemCount);
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
    final Part part = parts[resolvedPartIndex];
    partForViewHolder.put(holder, part);
    part.bind(resolvedItemIndex, holder);
  }

  @Override
  public boolean onFailedToRecycleView(final ViewHolder holder) {
    recycle(holder);
    return super.onFailedToRecycleView(holder);
  }

  @Override
  public void onViewRecycled(final ViewHolder holder) {
    recycle(holder);
  }

  private void recycle(@NonNull final ViewHolder holder) {
    final Part part = partForViewHolder.remove(holder);
    if (part != null) {
      part.recycle(holder);
    }
  }

  /**
   * Converts the given overall adapter {@code position} into {@link #resolvedPartIndex}
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

    resolvedPartIndex = arrayIndex;
    resolvedItemIndex = arrayIndex == 0 ? position : position - endPositions[arrayIndex - 1];
  }

  private abstract static class Part {
    abstract int getItemCount();

    abstract long getItemId(final int index, final long staticItemCount);

    @LayoutRes
    abstract int getLayoutResId(final int index);

    abstract void bind(final int index, @NonNull final ViewHolder holder);

    abstract void recycle(@NonNull final ViewHolder holder);
  }

  @SuppressWarnings("unchecked")
  private static final class RepositoryPart extends Part {
    @NonNull
    private final Repository repository;
    @NonNull
    private final RepositoryPresenter presenter;
    @NonNull
    private Object data;

    private RepositoryPart(
        @NonNull final Repository repository,
        @NonNull final RepositoryPresenter presenter) {
      this.repository = repository;
      this.presenter = presenter;
      this.data = repository.get();
    }

    @Override
    int getItemCount() {
      data = repository.get();
      return presenter.getItemCount(data);
    }

    @Override
    long getItemId(final int index, final long staticItemCount) {
      return presenter.getItemId(data, index) + staticItemCount;
    }

    @Override
    int getLayoutResId(final int index) {
      return presenter.getLayoutResId(data, index);
    }

    @Override
    void bind(final int index, @NonNull final ViewHolder holder) {
      presenter.bind(data, index, holder);
    }

    @Override
    void recycle(@NonNull final ViewHolder holder) {
      presenter.recycle(holder);
    }
  }

  @SuppressWarnings("unchecked")
  private static final class ItemPart extends Part {
    private final long itemIdOffset;
    private final int itemCount;
    @NonNull
    private final Object item;
    @NonNull
    private final RepositoryPresenter presenter;

    private ItemPart(final long itemIdOffset, final int itemCount,
        @NonNull final Object item, @NonNull final RepositoryPresenter presenter) {
      this.itemIdOffset = itemIdOffset;
      this.itemCount = itemCount;
      this.item = item;
      this.presenter = presenter;
    }

    @Override
    int getItemCount() {
      return itemCount;
    }

    @Override
    long getItemId(final int index, final long staticItemCount) {
      return itemIdOffset + index;
    }

    @Override
    int getLayoutResId(final int index) {
      return presenter.getLayoutResId(item, index);
    }

    @Override
    void bind(final int index, @NonNull final ViewHolder holder) {
      presenter.bind(item, index, holder);
    }

    @Override
    void recycle(@NonNull final ViewHolder holder) {
      presenter.recycle(holder);
    }
  }

  private static final class LayoutPart extends Part {
    private final long itemId;
    @NonNull
    private final LayoutPresenter presenter;

    private LayoutPart(final long itemId, @NonNull final LayoutPresenter presenter) {
      this.itemId = itemId;
      this.presenter = presenter;
    }

    @Override
    int getItemCount() {
      return 1;
    }

    @Override
    long getItemId(final int index, final long staticItemCount) {
      return itemId;
    }

    @Override
    int getLayoutResId(final int index) {
      return presenter.getLayoutResId();
    }

    @Override
    void bind(final int index, @NonNull final ViewHolder holder) {
      presenter.bind(holder.itemView);
    }

    @Override
    void recycle(@NonNull final ViewHolder holder) {
      presenter.recycle(holder.itemView);
    }
  }
}
