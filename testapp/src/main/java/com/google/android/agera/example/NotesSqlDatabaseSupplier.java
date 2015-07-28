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

import com.google.android.agera.database.SqlDatabaseSupplier;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

final class NotesSqlDatabaseSupplier extends SqlDatabaseSupplier {
  static final String NOTES_NOTE_COLUMN = "note";
  static final String NOTES_NOTE_ID_COLUMN = "id";
  static final String NOTES_TABLE = "notes";
  private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + NOTES_TABLE
      + " (" + NOTES_NOTE_ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + NOTES_NOTE_COLUMN + " VARCHAR(255));";
  private static final String DATABASE_NAME = "NotesDatabase";
  private static final int VERSION = 1;

  private NotesSqlDatabaseSupplier(@NonNull final Context context) {
    super(context, DATABASE_NAME, VERSION, null);
  }

  @NonNull
  public static NotesSqlDatabaseSupplier databaseSupplier(@NonNull final Context context) {
    return new NotesSqlDatabaseSupplier(context);
  }

  @Override
  public void onCreate(@NonNull final SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(CREATE_TABLE);
  }

  @Override
  public void onUpgrade(@NonNull final SQLiteDatabase sqLiteDatabase, final int i, final int i1) {}
}
