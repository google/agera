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

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Mergers.staticMerger;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.Reservoirs.reservoir;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseDeleteFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseInsertFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseQueryFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseUpdateFunction;
import static com.google.android.agera.database.SqlRequests.sqlDeleteRequest;
import static com.google.android.agera.database.SqlRequests.sqlInsertRequest;
import static com.google.android.agera.database.SqlRequests.sqlRequest;
import static com.google.android.agera.database.SqlRequests.sqlUpdateRequest;
import static com.google.android.agera.testapp.Note.note;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_NOTE_COLUMN;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_NOTE_ID_COLUMN;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_TABLE;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.databaseSupplier;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import com.google.android.agera.Function;
import com.google.android.agera.Merger;
import com.google.android.agera.Observable;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Reservoir;
import com.google.android.agera.Result;
import com.google.android.agera.database.SqlDeleteRequest;
import com.google.android.agera.database.SqlInsertRequest;
import com.google.android.agera.database.SqlUpdateRequest;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Encapsulates all database interactions for {@link Note}s. Getting a list of all notes is
 * implemented with a {@link #getNotesRepository()} Repository} that can be activated by and
 * observed from the Activity. Write operations are implemented using a {@link Reservoir} and a
 * reacting repository.
 */
final class NotesStore {
  private static final String MODIFY_NOTE_WHERE = NOTES_NOTE_ID_COLUMN + "=?";
  private static final String GET_NOTES_FROM_TABLE =
      "SELECT " + NOTES_NOTE_ID_COLUMN + ", " + NOTES_NOTE_COLUMN + " FROM " + NOTES_TABLE
          + " ORDER BY " + NOTES_NOTE_ID_COLUMN;
  private static final int ID_COLUMN_INDEX = 0;
  private static final int NOTE_COLUMN_INDEX = 1;
  private static final List<Note> INITIAL_VALUE = emptyList();

  private static NotesStore notesStore;

  @NonNull
  private final Receiver<Object> writeRequestReceiver;
  @NonNull
  private final Repository<List<Note>> notesRepository;

  private NotesStore(@NonNull final Repository<List<Note>> notesRepository,
      @NonNull final Receiver<Object> writeRequestReceiver) {
    this.writeRequestReceiver = writeRequestReceiver;
    this.notesRepository = notesRepository;
  }

  @NonNull
  public synchronized static NotesStore notesStore(@NonNull final Context applicationContext) {
    if (notesStore != null) {
      return notesStore;
    }
    // Create a thread executor to execute all database operations on.
    final Executor executor = newSingleThreadExecutor();

    // Create a database supplier that initializes the database. This is also used to supply the
    // database in all database operations.
    final NotesSqlDatabaseSupplier databaseSupplier = databaseSupplier(applicationContext);

    // Create a function that processes database write operations.
    final Function<SqlInsertRequest, Result<Long>> insertNoteFunction =
        databaseInsertFunction(databaseSupplier);
    final Function<SqlUpdateRequest, Result<Integer>> updateNoteFunction =
        databaseUpdateFunction(databaseSupplier);
    final Function<SqlDeleteRequest, Result<Integer>> deleteNoteFunction =
        databaseDeleteFunction(databaseSupplier);

    // Create a reservoir of database write requests. This will be used as the receiver of write
    // requests submitted to the NotesStore, and the event/data source of the reacting repository.
    final Reservoir<Object> writeRequestReservoir = reservoir();

    // Create a reacting repository that processes all write requests. The value of the repository
    // is unimportant, but it must be able to notify the notes repository on completing each write
    // operation. The database thread executor is single-threaded to optimize for disk I/O, but if
    // the executor can be multi-threaded, then this is the ideal place to multiply the reacting
    // repository to achieve parallelism. The notes repository should observe all these instances.
    final Number unimportantValue = 0;
    final Merger<Number, Number, Boolean> alwaysNotify = staticMerger(true);
    final Observable writeReaction = repositoryWithInitialValue(unimportantValue)
        .observe(writeRequestReservoir)
        .onUpdatesPerLoop()
        .goTo(executor)
        .attemptGetFrom(writeRequestReservoir).orSkip()
        .thenAttemptTransform(input -> {
          if (input instanceof SqlInsertRequest) {
            return insertNoteFunction.apply((SqlInsertRequest) input);
          }
          if (input instanceof SqlUpdateRequest) {
            return updateNoteFunction.apply((SqlUpdateRequest) input);
          }
          if (input instanceof SqlDeleteRequest) {
            return deleteNoteFunction.apply((SqlDeleteRequest) input);
          }
          return failure();
        }).orSkip()
        .notifyIf(alwaysNotify)
        .compile();

    // Keep the reacting repository in this lazy singleton activated for the full app life cycle.
    // This is optional -- it allows the write requests submitted when the notes repository is not
    // active to still be processed asap.
    writeReaction.addUpdatable(() -> {});

    // Create the repository of notes, wire it up to update on each database write, set it to fetch
    // notes from the database on the database thread executor.

    // Create the wired up notes store
    notesStore = new NotesStore(repositoryWithInitialValue(INITIAL_VALUE)
        .observe(writeReaction)
        .onUpdatesPerLoop()
        .goTo(executor)
        .getFrom(() -> sqlRequest().sql(GET_NOTES_FROM_TABLE).compile())
        .thenAttemptTransform(databaseQueryFunction(databaseSupplier,
            cursor -> note(cursor.getInt(ID_COLUMN_INDEX), cursor.getString(NOTE_COLUMN_INDEX))))
        .orEnd(staticFunction(INITIAL_VALUE))
        .onConcurrentUpdate(SEND_INTERRUPT)
        .onDeactivation(SEND_INTERRUPT)
        .compile(), writeRequestReservoir);
    return notesStore;
  }

  @NonNull
  public Repository<List<Note>> getNotesRepository() {
    return notesRepository;
  }

  public void insertNoteFromText(@NonNull final String noteText) {
    writeRequestReceiver.accept(sqlInsertRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .compile());
  }

  public boolean deleteNote(@NonNull final Note note) {
    writeRequestReceiver.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .where(MODIFY_NOTE_WHERE)
        .arguments(String.valueOf(note.getId()))
        .compile());
    return true;
  }

  public void updateNote(@NonNull final Note note, @NonNull final String noteText) {
    writeRequestReceiver.accept(sqlUpdateRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .where(MODIFY_NOTE_WHERE)
        .arguments(String.valueOf(note.getId()))
        .compile());
  }

  public void clearNotes() {
    writeRequestReceiver.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .compile());
  }
}
