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

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Bitmap.createBitmap;
import static android.graphics.BitmapFactory.decodeByteArray;
import static android.os.StrictMode.ThreadPolicy;
import static android.os.StrictMode.VmPolicy;
import static android.os.StrictMode.setThreadPolicy;
import static android.os.StrictMode.setVmPolicy;
import static com.google.android.agera.Functions.functionFromListOf;
import static com.google.android.agera.Functions.parallelMap;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static com.google.android.agera.rvdatabinding.DataBindingRepositoryPresenters.dataBindingRepositoryPresenterOf;
import static com.google.android.agera.testapp.NotesStore.notesStore;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newFixedThreadPool;

import com.google.android.agera.Predicate;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;
import com.google.android.agera.net.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.List;
import java.util.concurrent.ExecutorService;

public final class NotesActivity extends Activity {
  private static final ExecutorService networkExecutor = newFixedThreadPool(5);
  private static final ExecutorService calculationExecutor = newFixedThreadPool(5);
  private static final String BASE_URL =
      "http://www.gravatar.com/avatar/4df6f4fe5976df17deeea19443d4429d?s=";
  private static final List<String> URLS =
      asList(BASE_URL + "50", BASE_URL + "100", BASE_URL + "200", BASE_URL + "400");

  private Repository<Result<Bitmap>> backgroundRepository;
  private Updatable updatable;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notes_activity);

    // Setup strict mode, no violations using Agera
    setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().build());
    setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build());

    // Create the notes store, containing all async IO
    final NotesStore notesStore = notesStore(getApplicationContext());

    // Find the clear button and wire the click listener to call the clear notes updatable
    findViewById(R.id.clear).setOnClickListener(v -> notesStore.clearNotes());

    // Find the add button and wire the click listener to show a dialog that in turn calls the add
    // note from text from the notes store when adding notes
    findViewById(R.id.add).setOnClickListener(v -> {
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

    final Adapter<ViewHolder> adapter = repositoryAdapter()
        .add(notesStore.getNotesRepository(), dataBindingRepositoryPresenterOf(Note.class)
            .layout(R.layout.text_layout)
            .itemId(com.google.android.agera.testapp.BR.note)
            .handler(com.google.android.agera.testapp.BR.click,
                (Receiver<Note>) note -> {
                  final EditText editText = new EditText(this);
                  editText.setId(R.id.edit);
                  editText.setText(note.getNote());
                  new AlertDialog.Builder(this)
                      .setTitle(R.string.edit_note)
                      .setView(editText)
                      .setPositiveButton(R.string.edit,
                          (d, i) -> notesStore.updateNote(note, editText.getText().toString()))
                      .create().show();
                })
            .handler(com.google.android.agera.testapp.BR.longClick,
                (Predicate<Note>) notesStore::deleteNote)
            .forList())
        .whileStarted(this);

    // Setup the recycler view using the repository adapter
    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.result);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    backgroundRepository = repositoryWithInitialValue(Result.<Bitmap>absent())
        .observe()
        .onUpdatesPerLoop()
        .goTo(networkExecutor)
        .getFrom(() -> URLS)
        .transform(functionFromListOf(String.class)
            .map(url -> httpGetRequest(url).compile())
            .morph(parallelMap(networkExecutor, httpFunction()))
            .filter(Result::succeeded)
            .map(Result::get)
            .filter(Result::succeeded)
            .thenMap(Result::get))
        .goTo(calculationExecutor)
        .thenTransform(functionFromListOf(HttpResponse.class)
            .morph(parallelMap(calculationExecutor, input -> {
              final byte[] body = input.getBody();
              return absentIfNull(decodeByteArray(body, 0, body.length));
            }))
            .filter(Result::succeeded)
            .map(Result::get)
            .filter(Result::succeeded)
            .map(Result::get)
            .thenApply(bitmaps -> {
              int width = 0;
              int height = 0;
              for (final Bitmap bitmap : bitmaps) {
                width += bitmap.getWidth();
                height += bitmap.getHeight();
              }
              final Bitmap background = createBitmap(width, height, ARGB_8888);
              if (background == null) {
                return failure();
              }
              final Canvas backgroundCanvas = new Canvas(background);
              width = 0;
              height = 0;
              for (final Bitmap bitmap : bitmaps) {
                backgroundCanvas.drawBitmap(bitmap, width, height, null);
                width += bitmap.getWidth();
                height += bitmap.getHeight();
              }
              return success(background);
            }))
        .onDeactivation(SEND_INTERRUPT)
        .compile();

    final ImageView imageView = (ImageView) findViewById(R.id.background);

    updatable = () -> backgroundRepository.get().ifSucceededSendTo(imageView::setImageBitmap);
  }

  @Override
  protected void onResume() {
    super.onResume();
    backgroundRepository.addUpdatable(updatable);
  }

  @Override
  protected void onPause() {
    super.onPause();
    backgroundRepository.removeUpdatable(updatable);
  }
}