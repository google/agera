package com.google.android.agera.rvadapter.test;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import com.google.android.agera.Function;
import java.util.List;

/**
 * A function to extract the key from string item objects (simply the first two characters) as well
 * as a DiffUtil.Callback that uses the same key logic to check {@link #areItemsTheSame(int, int)}.
 * For verifying compiled presenter diffing functionality.
 */
public final class DiffingLogic extends DiffUtil.Callback
    implements Function<String, String> {
  @NonNull
  private final List<String> oldData;
  @NonNull
  private final List<String> newData;

  public DiffingLogic(@NonNull final List<String> oldData, @NonNull final List<String> newData) {
    this.oldData = oldData;
    this.newData = newData;
  }

  @NonNull
  @Override
  public String apply(@NonNull final String input) {
    // Function as key extractor
    return input.substring(0, 2);
  }

  @Override
  public int getOldListSize() {
    return oldData.size();
  }

  @Override
  public int getNewListSize() {
    return newData.size();
  }

  @Override
  public boolean areItemsTheSame(final int oldItemPosition, final int newItemPosition) {
    final String oldItem = oldData.get(oldItemPosition);
    final String newItem = newData.get(newItemPosition);
    return apply(oldItem).equals(apply(newItem));
  }

  @Override
  public boolean areContentsTheSame(final int oldItemPosition, final int newItemPosition) {
    final String oldItem = oldData.get(oldItemPosition);
    final String newItem = newData.get(newItemPosition);
    return oldItem.equals(newItem);
  }
}
