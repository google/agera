package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.widget.EditText;

final class EditedOnClickListener implements DialogInterface.OnClickListener {
  @NonNull
  private final Note note;
  @NonNull
  private final EditText editText;
  @NonNull
  private final NotesStore notesStore;

  public EditedOnClickListener(@NonNull final NotesStore notesStore, @NonNull final Note note,
      @NonNull final EditText editText) {
    this.note = checkNotNull(note);
    this.editText = checkNotNull(editText);
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public void onClick(@NonNull final DialogInterface dialogInterface, final int i) {
    notesStore.updateNote(note, editText.getText().toString());
  }
}
