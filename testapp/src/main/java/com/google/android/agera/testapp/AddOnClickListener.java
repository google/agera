/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
