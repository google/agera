package com.google.android.agera.rvadapter.test;

import static org.mockito.Mockito.verify;

import android.support.annotation.NonNull;
import android.support.v7.util.ListUpdateCallback;

public final class VerifyingWrappers {

  @NonNull
  public static ListUpdateCallback verifyingWrapper(
      @NonNull final ListUpdateCallback mockCallback) {
    // 'verify()' turns only the next method call on that mock to a verification call, so using
    // this wrapper to insert 'verify()' for each call when verifying the diff results.
    return new ListUpdateCallback() {
      @Override
      public void onInserted(final int position, final int count) {
        verify(mockCallback).onInserted(position, count);
      }

      @Override
      public void onRemoved(final int position, final int count) {
        verify(mockCallback).onRemoved(position, count);
      }

      @Override
      public void onMoved(final int fromPosition, final int toPosition) {
        verify(mockCallback).onMoved(fromPosition, toPosition);
      }

      @Override
      public void onChanged(final int position, final int count, final Object payload) {
        verify(mockCallback).onChanged(position, count, payload);
      }
    };
  }

  private VerifyingWrappers() {}
}
