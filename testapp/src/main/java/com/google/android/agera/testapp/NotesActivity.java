/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static android.os.StrictMode.ThreadPolicy;
import static android.os.StrictMode.VmPolicy;
import static android.os.StrictMode.setThreadPolicy;
import static android.os.StrictMode.setVmPolicy;
import static com.google.android.agera.testapp.NotesStore.notesStore;
import static com.google.android.agera.testapp.UrlToBitmapReactionFactory.urlToBitmapReactionFactory;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Reaction;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Updatable;
import com.google.android.agera.rvadapter.RepositoryAdapter;
import com.google.android.agera.rvadapter.RepositoryPresenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;

public final class NotesActivity extends Activity implements Updatable {
  private static final String BACKGROUND_URL =
      "http://www.gravatar.com/avatar/4df6f4fe5976df17deeea19443d4429d";
  private RepositoryAdapter adapter;
  private NotesStore notesStore;
  private Reaction<String> backgroundReaction;
  private ExecutorService networkExecutor;
  private ExecutorService calculationExecutor;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notes_activity);

    // Setup strict mode, no violations using Agera
    setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());
    setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().penaltyDeath().build());

    // Create the notes store, containing all async IO
    notesStore = notesStore(this);

    // Find the clear button and wire the click listener to call the clear notes updatable
    findViewById(R.id.clear).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View view) {
        notesStore.clearNotes();
      }
    });

    // Find the add button and wire the click listener to show a dialog that in turn calls the add
    // note from text from the notes store when adding notes
    findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(@NonNull final View view) {
        final EditText editText = new EditText(view.getContext());
        new AlertDialog.Builder(view.getContext())
            .setTitle(R.string.add_note)
            .setView(editText)
            .setPositiveButton(R.string.add, new OnClickListener() {
              @Override
              public void onClick(@NonNull final DialogInterface dialogInterface, final int i) {
                notesStore.insertNoteFromText(editText.getText().toString());
              }
            })
            .create().show();
      }
    });

    // Create a repository adapter, wiring up the notes repository from the store with a presenter
    adapter = repositoryAdapter()
        .add(notesStore.notesRepository(), new NotePresenter())
        .build();

    // Setup the recycler view using the repository adapter
    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.result);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    final Receiver<Bitmap> setBackgroundReceiver = new Receiver<Bitmap>() {
      @Override
      public void accept(@NonNull final Bitmap value) {
        final ImageView viewById = (ImageView) findViewById(R.id.background);
        viewById.setImageBitmap(value);
      }
    };
    networkExecutor = newSingleThreadExecutor();
    calculationExecutor = newSingleThreadExecutor();
    backgroundReaction = urlToBitmapReactionFactory(networkExecutor, calculationExecutor)
            .createUrlToBitmapReaction(setBackgroundReceiver);
  }

  @Override
  protected void onResume() {
    super.onResume();
    // The adapter is dormant before start observing is called
    adapter.startObserving();
    backgroundReaction.addUpdatable(this);
    final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
    final int size = Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
    backgroundReaction.accept(BACKGROUND_URL + "?s=" + size);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Start observing needs to be paired with stop observing
    adapter.stopObserving();
    backgroundReaction.removeUpdatable(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // Close the notes store and the associated database
    notesStore.close();
    networkExecutor.shutdown();
    calculationExecutor.shutdown();
  }

  @Override
  public void update() {}

  // Presents each note in the repository as a text view in the recycler view
  private final class NotePresenter extends RepositoryPresenter<List<Note>> {
    @Override
    public int getItemCount(@NonNull final Repository<List<Note>> repository) {
      return repository.get().size();
    }

    @Override
    public int getLayoutResId(@NonNull final Repository<List<Note>> repository, final int index) {
      return R.layout.text_layout;
    }

    @Override
    public void bind(@NonNull final Repository<List<Note>> repository, final int index,
        @NonNull final RecyclerView.ViewHolder holder) {
      final Note note = repository.get().get(index);
      TextView view = (TextView) holder.itemView;
      view.setText(note.getNote());
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          final EditText editText = new EditText(view.getContext());
          editText.setText(note.getNote());
          new Builder(view.getContext())
              .setTitle(R.string.edit_note)
              .setView(editText)
              .setPositiveButton(R.string.edit, new OnClickListener() {
                @Override
                public void onClick(@NonNull final DialogInterface dialogInterface, final int i) {
                  notesStore.updateNote(note, editText.getText().toString());
                }
              })
              .create().show();
        }
      });
      view.setOnLongClickListener(new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
          notesStore.deleteNote(note);
          return true;
        }
      });
    }
  }
}