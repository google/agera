package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import com.google.android.agera.rvadapter.RepositoryPresenter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.util.List;

// Presents each note in the repository as a text view in the recycler view
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
