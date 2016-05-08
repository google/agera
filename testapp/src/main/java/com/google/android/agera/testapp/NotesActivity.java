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
import static android.os.StrictMode.ThreadPolicy;
import static android.os.StrictMode.VmPolicy;
import static android.os.StrictMode.setThreadPolicy;
import static android.os.StrictMode.setVmPolicy;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;
import static com.google.android.agera.rvadapter.RepositoryAdapter.repositoryAdapter;
import static com.google.android.agera.testapp.NotesStore.notesStore;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Function;
import com.google.android.agera.Merger;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;
import com.google.android.agera.Updatable;
import com.google.android.agera.net.HttpRequest;
import com.google.android.agera.net.HttpResponse;
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
  public static final UrlToHttpRequest URL_TO_HTTP_REQUEST = new UrlToHttpRequest();
  public static final HttpResponseToBitmap HTTP_RESPONSE_TO_BITMAP = new HttpResponseToBitmap();
  private static final String BACKGROUND_URL =
      "http://www.gravatar.com/avatar/4df6f4fe5976df17deeea19443d4429d";
  private RepositoryAdapter adapter;
  private NotesStore notesStore;
  private Repository<Result<Bitmap>> backgroundRepository;
  private ExecutorService networkExecutor;
  private ExecutorService calculationExecutor;
  private Receiver<Bitmap> setBackgroundReceiver;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.notes_activity);

    // Setup strict mode, no violations using Agera
    setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().build());
    setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build());

    // Create the notes store, containing all async IO
    notesStore = notesStore(getApplicationContext());

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
        editText.setId(R.id.edit);
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
        .add(notesStore.getNotesRepository(), new NotePresenter())
        .build();

    // Setup the recycler view using the repository adapter
    final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.result);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));

    networkExecutor = newSingleThreadExecutor();
    calculationExecutor = newSingleThreadExecutor();

    setBackgroundReceiver = new ImageViewBitmapReceiver((ImageView) findViewById(R.id.background));

    final Supplier<Integer> sizeSupplier = new Supplier<Integer>() {
      @NonNull
      @Override
      public Integer get() {
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.max(displayMetrics.heightPixels, displayMetrics.widthPixels);
      }
    };

    final Merger<Integer, String, String> sizedUrlMerger = new Merger<Integer, String, String>() {
      @NonNull
      @Override
      public String merge(@NonNull final Integer integer, @NonNull final String s) {
        return s + "?s=" + integer;
      }
    };

    final Supplier<String> backgroundUrlSupplier = staticSupplier(BACKGROUND_URL);

    backgroundRepository = repositoryWithInitialValue(Result.<Bitmap>absent())
        .observe()
        .onUpdatesPerLoop()
        .getFrom(sizeSupplier)
        .goTo(networkExecutor)
        .mergeIn(backgroundUrlSupplier, sizedUrlMerger)
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
  protected void onDestroy() {
    super.onDestroy();
    // Close the notes store and the associated database
    networkExecutor.shutdown();
    calculationExecutor.shutdown();
  }

  @Override
  public void update() {
    backgroundRepository.get().ifSucceededSendTo(setBackgroundReceiver);
  }

  private static final class UrlToHttpRequest implements Function<String, HttpRequest> {
    @NonNull
    @Override
    public HttpRequest apply(@NonNull final String input) {
      return httpGetRequest(input).compile();
    }
  }

  private static final class HttpResponseToBitmap
      implements Function<HttpResponse, Result<Bitmap>> {
    @NonNull
    @Override
    public Result<Bitmap> apply(@NonNull final HttpResponse input) {
      final byte[] body = input.getBody();
      return absentIfNull(decodeByteArray(body, 0, body.length));
    }
  }

  private static class ImageViewBitmapReceiver implements Receiver<Bitmap> {
    @NonNull
    private final ImageView imageView;

    public ImageViewBitmapReceiver(@NonNull final ImageView imageView) {
      this.imageView = imageView;
    }

    @Override
    public void accept(@NonNull final Bitmap value) {
      imageView.setImageBitmap(value);
    }
  }

  // Presents each note in the repository as a text view in the recycler view
  private final class NotePresenter extends RepositoryPresenter<List<Note>> {
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
      TextView view = (TextView) holder.itemView;
      view.setText(note.getNote());
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          final EditText editText = new EditText(view.getContext());
          editText.setId(R.id.edit);
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