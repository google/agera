package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.support.annotation.NonNull;
import android.view.View;

final class ClearOnClickListener implements View.OnClickListener {
  @NonNull
  private final NotesStore notesStore;

  public ClearOnClickListener(@NonNull final NotesStore notesStore) {
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public void onClick(final View view) {
    notesStore.clearNotes();
  }
}
