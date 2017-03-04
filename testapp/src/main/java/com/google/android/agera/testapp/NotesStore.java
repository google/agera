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

import static android.text.TextUtils.isEmpty;
import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.RepositoryConfig.SEND_INTERRUPT;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseDeleteFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseInsertFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseQueryFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseUpdateFunction;
import static com.google.android.agera.database.SqlRequests.sqlDeleteRequest;
import static com.google.android.agera.database.SqlRequests.sqlInsertRequest;
import static com.google.android.agera.database.SqlRequests.sqlRequest;
import static com.google.android.agera.database.SqlRequests.sqlUpdateRequest;
import static com.google.android.agera.testapp.Note.note;
import static com.google.android.agera.testapp.NoteGroup.noteGroup;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_NOTE_COLUMN;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_NOTE_ID_COLUMN;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.NOTES_TABLE;
import static com.google.android.agera.testapp.NotesSqlDatabaseSupplier.databaseSupplier;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.Context;
import android.support.annotation.NonNull;
import com.google.android.agera.Function;
import com.google.android.agera.Receiver;
import com.google.android.agera.Repository;
import com.google.android.agera.Result;
import com.google.android.agera.UpdateDispatcher;
import com.google.android.agera.database.SqlDatabaseSupplier;
import com.google.android.agera.database.SqlDeleteRequest;
import com.google.android.agera.database.SqlInsertRequest;
import com.google.android.agera.database.SqlUpdateRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;

/**
 * Encapsulates all database interactions for {@link Note}s. Getting a list of all notes is
 * implemented with a {@link #getNotesRepository()} Repository} that can be activated by and
 * observed from the Activity.
 */
final class NotesStore {
  private static final String MODIFY_NOTE_WHERE = NOTES_NOTE_ID_COLUMN + "=?";
  private static final String GET_NOTES_FROM_TABLE =
      "SELECT " + NOTES_NOTE_ID_COLUMN + ", " + NOTES_NOTE_COLUMN + " FROM " + NOTES_TABLE
          + " ORDER BY " + NOTES_NOTE_COLUMN;
  private static final int ID_COLUMN_INDEX = 0;
  private static final int NOTE_COLUMN_INDEX = 1;
  private static final List<NoteGroup> INITIAL_VALUE = emptyList();
  @NonNull
  private static final Executor STORE_EXECUTOR = newSingleThreadExecutor();

  @NonNull
  private final Repository<List<NoteGroup>> notesRepository;
  @NonNull
  private final Receiver<SqlInsertRequest> insert;
  @NonNull
  private final Receiver<SqlUpdateRequest> update;
  @NonNull
  private final Receiver<SqlDeleteRequest> delete;
  @NonNull
  private final SqlDatabaseSupplier databaseSupplier;

  private NotesStore(@NonNull final Repository<List<NoteGroup>> notesRepository,
      @NonNull final Receiver<SqlInsertRequest> insert,
      @NonNull final Receiver<SqlUpdateRequest> update,
      @NonNull final Receiver<SqlDeleteRequest> delete,
      @NonNull final SqlDatabaseSupplier databaseSupplier) {
    this.insert = insert;
    this.update = update;
    this.delete = delete;
    this.notesRepository = notesRepository;
    this.databaseSupplier = databaseSupplier;
  }

  @NonNull
  synchronized static NotesStore notesStore(@NonNull final Context applicationContext) {
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

    final UpdateDispatcher updateDispatcher = updateDispatcher();

    final Receiver<SqlDeleteRequest> delete = value -> STORE_EXECUTOR.execute(() -> {
      deleteNoteFunction.apply(value);
      updateDispatcher.update();
    });

    final Receiver<SqlUpdateRequest> update = value -> STORE_EXECUTOR.execute(() -> {
      updateNoteFunction.apply(value);
      updateDispatcher.update();
    });

    final Receiver<SqlInsertRequest> insert = value -> STORE_EXECUTOR.execute(() -> {
      insertNoteFunction.apply(value);
      updateDispatcher.update();
    });

    // Create the wired up notes store
    return new NotesStore(repositoryWithInitialValue(INITIAL_VALUE)
        .observe(updateDispatcher)
        .onUpdatesPerLoop()
        .goTo(STORE_EXECUTOR)
        .getFrom(() -> sqlRequest().sql(GET_NOTES_FROM_TABLE).compile())
        .attemptTransform(databaseQueryFunction(databaseSupplier,
            cursor -> note(cursor.getInt(ID_COLUMN_INDEX), cursor.getString(NOTE_COLUMN_INDEX))))
        .orEnd(staticFunction(INITIAL_VALUE))
        .thenTransform(notes -> {
          final Map<Character, List<Note>> notesGroupsData = new TreeMap<>();
          for (final Note note : notes) {
            final String noteText = note.getNote();
            final char groupId = isEmpty(noteText) ? 0 : noteText.charAt(0);
            List<Note> notesGroupData = notesGroupsData.get(groupId);
            if (notesGroupData == null) {
              notesGroupData = new ArrayList<>();
              notesGroupsData.put(groupId, notesGroupData);
            }
            notesGroupData.add(note);
          }
          final List<NoteGroup> notesGroups = new ArrayList<>();
          for (final Map.Entry<Character, List<Note>> groupData : notesGroupsData.entrySet()) {
            notesGroups.add(noteGroup(
                String.valueOf(groupData.getKey()),
                groupData.getKey(),
                groupData.getValue()));
          }
          return notesGroups;
        })
        .onConcurrentUpdate(SEND_INTERRUPT)
        .onDeactivation(SEND_INTERRUPT)
        .compile(), insert, update, delete, databaseSupplier);
  }

  @NonNull
  Repository<List<NoteGroup>> getNotesRepository() {
    return notesRepository;
  }

  void insertNoteFromText(@NonNull final String noteText) {
    insert.accept(sqlInsertRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .compile());
  }

  void deleteNote(@NonNull final Note note) {
    delete.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .where(MODIFY_NOTE_WHERE)
        .arguments(valueOf(note.getId()))
        .compile());
  }

  void updateNote(@NonNull final Note note, @NonNull final String noteText) {
    update.accept(sqlUpdateRequest()
        .table(NOTES_TABLE)
        .column(NOTES_NOTE_COLUMN, noteText)
        .where(MODIFY_NOTE_WHERE)
        .arguments(valueOf(note.getId()))
        .compile());
  }

  void clearNotes() {
    delete.accept(sqlDeleteRequest()
        .table(NOTES_TABLE)
        .compile());
  }

  void closeDatabase() {
    databaseSupplier.close();
  }
}
