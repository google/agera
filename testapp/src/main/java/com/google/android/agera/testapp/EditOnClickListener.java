package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

final class EditOnClickListener implements View.OnClickListener {
  @NonNull
  private final Note note;
  @NonNull
  private final NotesStore notesStore;

  public EditOnClickListener(@NonNull final NotesStore notesStore, @NonNull final Note note) {
    this.note = checkNotNull(note);
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public void onClick(final View view) {
    final EditText editText = new EditText(view.getContext());
    editText.setId(R.id.edit);
    editText.setText(note.getNote());
    new AlertDialog.Builder(view.getContext())
        .setTitle(R.string.edit_note)
        .setView(editText)
        .setPositiveButton(R.string.edit, new EditedOnClickListener(notesStore, note, editText))
        .create().show();
  }
}
