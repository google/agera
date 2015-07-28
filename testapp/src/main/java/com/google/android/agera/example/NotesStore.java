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
package com.google.android.agera.example;

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Reactions.reactionTo;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RexConfig.SEND_INTERRUPT;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseDeleteFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseInsertFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseQueryFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseUpdateFunction;
import static com.google.android.agera.database.SqlRequests.sqlDeleteRequest;
import static com.google.android.agera.database.SqlRequests.sqlInsertRequest;
import static com.google.android.agera.database.SqlRequests.sqlRequest;
import static com.google.android.agera.database.SqlRequests.sqlUpdateRequest;
import static com.google.android.agera.example.Note.note;
import static com.google.android.agera.example.NotesSqlDatabaseSupplier.NOTES_NOTE_COLUMN;
import static com.google.android.agera.example.NotesSqlDatabaseSupplier.NOTES_NOTE_ID_COLUMN;
import static com.google.android.agera.example.NotesSqlDatabaseSupplier.NOTES_TABLE;
import static com.google.android.agera.example.NotesSqlDatabaseSupplier.databaseSupplier;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Function;
import com.google.android.agera.Reaction;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.database.SqlDeleteRequest;
import com.google.android.agera.database.SqlInsertRequest;
import com.google.android.agera.database.SqlUpdateRequest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Encapsulates all database interaction. Insert, update, delete, clear notes use {@link Reaction}.
 * Getting a list of all notes is implemented with a {@link Repository} that can be observed from
 * the Activity.
 */
final class NotesStore {
  private static final String MODIFY_NOTE_WHERE = NOTES_NOTE_ID_COLUMN + "=?";
  private static final String GET_NOTES_FROM_TABLE =
      "SELECT " + NOTES_NOTE_ID_COLUMN + ", " + NOTES_NOTE_COLUMN + " FROM " + NOTES_TABLE
          + " ORDER BY " + NOTES_NOTE_ID_COLUMN;
  private static final int ID_COLUMN_INDEX = 0;
  private static final int NOTE_COLUMN_INDEX = 1;
  private static final List<Note> INITIAL_VALUE = emptyList();
  @NonNull
  private final NotesSqlDatabaseSupplier databaseSupplier;
  @NonNull
  private final ExecutorService executor;
  @NonNull
  private final Receiver<SqlInsertRequest> insertNoteFromTextReceiver;
  @NonNull
  private final Receiver<SqlUpdateRequest> updateNoteReceiver;
  @NonNull
  private final Receiver<SqlDeleteRequest> deleteNoteReceiver;
  @NonNull
  private final Repository<List<Note>> notesRepository;

  private NotesStore(@NonNull final NotesSqlDatabaseSupplier databaseSupplier,
      @NonNull final ExecutorService executor,
      @NonNull final Repository<List<Note>> notesRepository,
      @NonNull final Receiver<SqlInsertRequest> insertNoteFromTextReceiver,
      @NonNull final Receiver<SqlUpdateRequest> updateNoteReceiver,
      @NonNull final Receiver<SqlDeleteRequest> deleteNoteReceiver) {
    this.databaseSupplier = databaseSupplier;
    this.executor = executor;
    this.insertNoteFromTextReceiver = insertNoteFromTextReceiver;
    this.deleteNoteReceiver = deleteNoteReceiver;
    this.notesRepository = notesRepository;
    this.updateNoteReceiver = updateNoteReceiver;
  }

  @NonNull
  public static NotesStore notesStore(@NonNull final Context context) {
    // Create a thread executor to execute all database operations on
    final ExecutorService executor = newSingleThreadExecutor();

    // Create a database supplier that initializes the database. This is also used to supply the
    // database in all database operations
    final NotesSqlDatabaseSupplier databaseSupplier = databaseSupplier(context);

    // Create a receiver that inserts a note on the database thread executor
    final Reaction<SqlInsertRequest> insertReaction = reactionTo(SqlInsertRequest.class)
        .goTo(executor)
        .attemptTransform(databaseInsertFunction(databaseSupplier)).orSkip()
        .thenEnd()
        .compile();

    // Create a reaction that updates a note on the database thread executor
    final Reaction<SqlUpdateRequest> updateReaction = reactionTo(SqlUpdateRequest.class)
        .goTo(executor)
        .attemptTransform(databaseUpdateFunction(databaseSupplier)).orSkip()
        .thenEnd()
        .compile();

    // Create a receiver that deletes a note on the database thread executor
    final Reaction<SqlDeleteRequest> deleteReaction = reactionTo(SqlDeleteRequest.class)
        .goTo(executor)
        .attemptTransform(databaseDeleteFunction(databaseSupplier)).orSkip()
        .thenEnd()
        .compile();

    // Create a function to map each row to a note
    final Function<Cursor, Note> cursorToNote = new Function<Cursor, Note>() {
      @NonNull
      @Override
      public Note apply(@NonNull final Cursor cursor) {
        return note(cursor.getInt(ID_COLUMN_INDEX), cursor.getString(NOTE_COLUMN_INDEX));
      }
    };

    // Create the repository of notes, wire it up to be updated on database reactions, set it to
    // fetch notes from the database on the executor
    final Repository<List<Note>> notesRepository = repositoryWithInitialValue(INITIAL_VALUE)
        .observe(insertReaction, updateReaction, deleteReaction)
        .onUpdatesPerLoop()
        .goTo(executor)
        .getFrom(staticSupplier(sqlRequest().sql(GET_NOTES_FROM_TABLE).compile()))
        .thenAttemptTransform(databaseQueryFunction(databaseSupplier, cursorToNote))
        .orEnd(staticFunction(INITIAL_VALUE))
        .onConcurrentUpdate(SEND_INTERRUPT)
        .onDeactivation(SEND_INTERRUPT)
        .compile();

    // Create the wired up notes store
    return new NotesStore(databaseSupplier, executor,
        notesRepository, insertReaction, updateReaction, deleteReaction);
  }

  @NonNull
  public Repository<List<Note>> notesRepository() {
    return notesRepository;
  }

  public void insertNoteFromText(@NonNull final String noteText) {
    insertNoteFromTextReceiver.accept(sqlInsertRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .compile());
  }

  public void deleteNote(@NonNull final Note note) {
    deleteNoteReceiver.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .where(MODIFY_NOTE_WHERE)
        .arguments(valueOf(note.getId()))
        .compile());
  }

  public void updateNote(@NonNull final Note note, @NonNull final String noteText) {
    final ContentValues contentValues = new ContentValues();
    contentValues.put(NOTES_NOTE_COLUMN, noteText);
    updateNoteReceiver.accept(sqlUpdateRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .where(MODIFY_NOTE_WHERE)
        .arguments(valueOf(note.getId()))
        .compile());
  }

  public void clearNotes() {
    deleteNoteReceiver.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .compile());
  }

  public void close() {
    executor.shutdownNow();
    databaseSupplier.close();
  }
}
