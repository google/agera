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
