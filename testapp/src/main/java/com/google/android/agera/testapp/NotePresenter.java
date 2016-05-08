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

import com.google.android.agera.rvadapter.RepositoryPresenter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.List;

final class NotePresenter extends RepositoryPresenter<List<Note>> {
  @NonNull
  private final NotesStore notesStore;

  public NotePresenter(@NonNull final NotesStore notesStore) {
    this.notesStore = checkNotNull(notesStore);
  }

  @Override
  public int getItemCount(@NonNull final List<Note> notes) {
    return notes.size();
  }

  @Override
  public int getLayoutResId(@NonNull final List<Note> notes, final int index) {
    return R.layout.text_layout;
  }

  @Override
  public void bind(@NonNull final List<Note> notes, final int index,
      @NonNull final RecyclerView.ViewHolder holder) {
    final Note note = notes.get(index);
    final TextView view = (TextView) holder.itemView;
    view.setText(note.getNote());
    view.setOnClickListener(new EditOnClickListener(notesStore, note));
    view.setOnLongClickListener(new DeleteOnLongClickListener(notesStore, note));
  }
}
