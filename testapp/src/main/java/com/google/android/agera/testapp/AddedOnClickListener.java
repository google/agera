package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.widget.EditText;

final class AddedOnClickListener implements DialogInterface.OnClickListener {
  @NonNull
  private final EditText editText;
  @NonNull
  private final NotesStore notesStore;

  public AddedOnClickListener(@NonNull final NotesStore notesStore,
      @NonNull final EditText editText) {
    this.editText = checkNotNull(editText);
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public void onClick(@NonNull final DialogInterface dialogInterface, final int i) {
    notesStore.insertNoteFromText(editText.getText().toString());
  }
}
