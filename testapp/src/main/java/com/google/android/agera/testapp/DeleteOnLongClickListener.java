package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.support.annotation.NonNull;
import android.view.View;

final class DeleteOnLongClickListener implements View.OnLongClickListener {
  @NonNull
  private final Note note;
  @NonNull
  private final NotesStore notesStore;

  public DeleteOnLongClickListener(@NonNull final NotesStore notesStore,
      @NonNull final Note note) {
    this.note = checkNotNull(note);
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public boolean onLongClick(final View view) {
    notesStore.deleteNote(note);
    return true;
  }
}
