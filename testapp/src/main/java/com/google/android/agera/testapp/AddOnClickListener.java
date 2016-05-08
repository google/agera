package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.EditText;

final class AddOnClickListener implements View.OnClickListener {
  @NonNull
  private final NotesStore notesStore;

  public AddOnClickListener(@NonNull final NotesStore notesStore) {
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public void onClick(@NonNull final View view) {
    final EditText editText = new EditText(view.getContext());
    editText.setId(R.id.edit);
    new AlertDialog.Builder(view.getContext())
        .setTitle(R.string.add_note)
        .setView(editText)
        .setPositiveButton(R.string.add, new AddedOnClickListener(notesStore, editText))
        .create().show();
  }
}
