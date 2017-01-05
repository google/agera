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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import com.google.android.agera.database.SqlDatabaseSupplier;

final class NotesSqlDatabaseSupplier extends SqlDatabaseSupplier {
  static final String NOTES_NOTE_COLUMN = "note";
  static final String NOTES_NOTE_ID_COLUMN = "id";
  static final String NOTES_TABLE = "notes";
  private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + NOTES_TABLE
      + " (" + NOTES_NOTE_ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + NOTES_NOTE_COLUMN + " VARCHAR(255));";
  private static final String DATABASE_NAME = "NotesDatabase";
  private static final int VERSION = 1;
  @NonNull
  private static final String[] VALUES = {
      "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1", "i1", "j1", "k1", "l1", "m1", "n1",
      "o1", "p1", "q1", "r1", "s1", "t1", "u1", "v1", "w1", "x1", "y1", "z1",
      "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2", "i2", "j2", "k2", "l2", "m2", "n2",
      "o2", "p2", "q2", "r2", "s2", "t2", "u2", "v2", "w2", "x2", "y2", "z2",
      "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3", "i3", "j3", "k3", "l3", "m3", "n3",
      "o3", "p3", "q3", "r3", "s3", "t3", "u3", "v3", "w3", "x3", "y3", "z3",
      "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4", "i4", "j4", "k4", "l4", "m4", "n4",
      "o4", "p4", "q4", "r4", "s4", "t4", "u4", "v4", "w4", "x4", "y4", "z4",
      "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5", "i5", "j5", "k5", "l5", "m5", "n5",
      "o5", "p5", "q5", "r5", "s5", "t5", "u5", "v5", "w5", "x5", "y5", "z5"
  };

  private NotesSqlDatabaseSupplier(@NonNull final Context context) {
    super(context, DATABASE_NAME, null, VERSION);
  }

  @NonNull
  public static NotesSqlDatabaseSupplier databaseSupplier(@NonNull final Context context) {
    return new NotesSqlDatabaseSupplier(context);
  }

  @Override
  public void onCreate(@NonNull final SQLiteDatabase sqLiteDatabase) {
    sqLiteDatabase.execSQL(CREATE_TABLE);
    for (final String value : VALUES) {
      final ContentValues contentValues = new ContentValues();
      contentValues.put(NOTES_NOTE_COLUMN, value);
      sqLiteDatabase.insert(NOTES_TABLE, null, contentValues);
    }
  }

  @Override
  public void onUpgrade(@NonNull final SQLiteDatabase sqLiteDatabase, final int i, final int i1) {}
}
