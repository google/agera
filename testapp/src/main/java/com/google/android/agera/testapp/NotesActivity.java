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
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static com.google.android.agera.testapp.NotesStore.notesStore;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Updatable;
import com.google.android.agera.rvadapter.RepositoryAdapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.widget.ImageView;

import java.util.concurrent.Executor;

public final class NotesActivity extends Activity implements Updatable {
  private static final UrlToHttpRequest URL_TO_HTTP_REQUEST = new UrlToHttpRequest();
  private static final HttpResponseToBitmap HTTP_RESPONSE_TO_BITMAP = new HttpResponseToBitmap();
  private static final Executor networkExecutor = newSingleThreadExecutor();
  private static final Executor calculationExecutor = newSingleThreadExecutor();
  private static final String BACKGROUND_URL =
      "http://www.gravatar.com/avatar/4df6f4fe5976df17deeea19443d4429d";

  private RepositoryAdapter adapter;
  private Repository<Result<Bitmap>> backgroundRepository;
  private Receiver<Bitmap> setBackgroundReceiver;

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
    findViewById(R.id.clear).setOnClickListener(new ClearOnClickListener(notesStore));

    // Find the add button and wire the click listener to show a dialog that in turn calls the add
    // note from text from the notes store when adding notes
    findViewById(R.id.add).setOnClickListener(new AddOnClickListener(notesStore));

    // Create a repository adapter, wiring up the notes repository from the store with a presenter
    adapter = repositoryAdapter()
        .add(notesStore.getNotesRepository(), new NotePresenter(notesStore))
        .build();

    // Setup the recycler view using the repository adapter
    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.result);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    setBackgroundReceiver = new ImageViewBitmapReceiver((ImageView) findViewById(R.id.background));

    final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

    backgroundRepository = repositoryWithInitialValue(Result.<Bitmap>absent())
        .observe()
        .onUpdatesPerLoop()
        .goTo(networkExecutor)
        .getFrom(staticSupplier(BACKGROUND_URL
            + "?s=" + Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels)))
        .transform(URL_TO_HTTP_REQUEST)
        .attemptTransform(httpFunction()).orSkip()
        .goTo(calculationExecutor)
        .thenTransform(HTTP_RESPONSE_TO_BITMAP)
        .onDeactivation(SEND_INTERRUPT)
        .compile();
  }

  @Override
  protected void onResume() {
    super.onResume();
    // The adapter is dormant before start observing is called
    adapter.startObserving();
    backgroundRepository.addUpdatable(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Start observing needs to be paired with stop observing
    adapter.stopObserving();
    backgroundRepository.removeUpdatable(this);
  }

  @Override
  public void update() {
    backgroundRepository.get().ifSucceededSendTo(setBackgroundReceiver);
  }
}