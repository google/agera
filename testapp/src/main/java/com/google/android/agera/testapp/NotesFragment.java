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

import static android.graphics.BitmapFactory.decodeByteArray;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenters.dataBindingRepositoryPresenterOf;
import static com.google.android.agera.testapp.NotesStore.notesStore;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Predicate;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;
import com.google.android.agera.rvadapter.RepositoryAdapter;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.concurrent.Executor;

public final class NotesFragment extends Fragment {
  private static final Executor networkExecutor = newSingleThreadExecutor();
  private static final Executor calculationExecutor = newSingleThreadExecutor();

  private Repository<Result<Bitmap>> backgroundRepository;
  private Updatable updatable;
  private RepositoryAdapter adapter;

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container,
      @Nullable final Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.notes_fragment, container, false);

    // Create the notes store, containing all async IO
    final NotesStore notesStore = notesStore(getContext().getApplicationContext());

    // Find the clear button and wire the click listener to call the clear notes updatable
    view.findViewById(R.id.clear).setOnClickListener(v -> notesStore.clearNotes());

    // Find the add button and wire the click listener to show a dialog that in turn calls the add
    // note from text from the notes store when adding notes
    view.findViewById(R.id.add).setOnClickListener(v -> {
      final EditText editText = new EditText(v.getContext());
      editText.setId(R.id.edit);
      new AlertDialog.Builder(v.getContext())
          .setTitle(R.string.add_note)
          .setView(editText)
          .setPositiveButton(R.string.add, (d, i) -> {
            notesStore.insertNoteFromText(editText.getText().toString());
          })
          .create().show();
    });

    adapter = repositoryAdapter()
        .add(notesStore.getNotesRepository(), dataBindingRepositoryPresenterOf(Note.class)
            .layout(R.layout.text_layout)
            .itemId(BR.note)
            .stableIdForItem(input -> (long) input.getId())
            .handler(BR.click,
                (Receiver<Note>) note -> {
                  final EditText editText = new EditText(getContext());
                  editText.setId(R.id.edit);
                  editText.setText(note.getNote());
                  new AlertDialog.Builder(getContext())
                      .setTitle(R.string.edit_note)
                      .setView(editText)
                      .setPositiveButton(R.string.edit,
                          (d, i) -> notesStore.updateNote(note, editText.getText().toString()))
                      .create().show();
                })
            .handler(BR.longClick,
                (Predicate<Note>) notesStore::deleteNote)
            .forList())
        .build();
    adapter.setHasStableIds(true);

    // Setup the recycler view using the repository adapter
    final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.result);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

    backgroundRepository = repositoryWithInitialValue(Result.<Bitmap>absent())
        .observe()
        .onUpdatesPerLoop()
        .goTo(networkExecutor)
        .getFrom(() -> "http://www.gravatar.com/avatar/4df6f4fe5976df17deeea19443d4429d?s="
            + Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels))
        .transform(url -> httpGetRequest(url).compile())
        .attemptTransform(httpFunction()).orEnd(Result::failure)
        .goTo(calculationExecutor)
        .thenTransform(input -> {
          final byte[] body = input.getBody();
          return absentIfNull(decodeByteArray(body, 0, body.length));
        })
        .onDeactivation(SEND_INTERRUPT)
        .compile();

    final ImageView imageView = (ImageView) view.findViewById(R.id.background);

    updatable = () -> backgroundRepository.get().ifSucceededSendTo(imageView::setImageBitmap);
    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    adapter.startObserving();
    backgroundRepository.addUpdatable(updatable);
  }

  @Override
  public void onStop() {
    super.onStop();
    backgroundRepository.removeUpdatable(updatable);
    adapter.stopObserving();
  }
}
