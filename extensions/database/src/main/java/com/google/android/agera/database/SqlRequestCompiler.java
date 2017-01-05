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
package com.google.android.agera.database;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_NONE;
import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;
import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.agera.database.SqlRequestCompilerStates.DBArgumentCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBArgumentConflictCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnConflictCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnWhereConflictCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBSql;
import com.google.android.agera.database.SqlRequestCompilerStates.DBTable;
import com.google.android.agera.database.SqlRequestCompilerStates.DBWhereCompile;

@SuppressWarnings({"unchecked, rawtypes"})
final class SqlRequestCompiler
    implements DBTable, DBSql, DBArgumentCompile, DBColumnConflictCompile, DBWhereCompile,
    DBColumnWhereConflictCompile, DBArgumentConflictCompile {
  static final int SQL_REQUEST = 0;
  static final int SQL_DELETE_REQUEST = 1;
  static final int SQL_UPDATE_REQUEST = 2;
  static final int SQL_INSERT_REQUEST = 3;
  @NonNull
  private static final String[] NO_ARGUMENTS = new String[] {};
  @NonNull
  private static final String ERROR_MESSAGE = "Sql compiler cannot be reused";

  private final int type;
  @NonNull
  private final ContentValues contentValues;

  @NonNull
  private String[] arguments;
  @NonNull
  private String table;
  @NonNull
  private String query;
  @NonNull
  private String where;
  private boolean compiled;
  private int conflictAlgorithm;

  SqlRequestCompiler(final int type) {
    this.type = type;
    this.where = "";
    this.contentValues = new ContentValues();
    this.arguments = NO_ARGUMENTS;
    this.compiled = false;
    this.table = "";
    this.query = "";
    this.conflictAlgorithm = CONFLICT_NONE;
  }

  @NonNull
  @Override
  public Object table(@NonNull final String table) {
    checkState(!compiled, ERROR_MESSAGE);
    this.table = table;
    return this;
  }

  @NonNull
  @Override
  public SqlRequestCompiler sql(@NonNull final String query) {
    checkState(!compiled, ERROR_MESSAGE);
    this.query = query;
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final String value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Byte value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Short value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Integer value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Long value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Float value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Double value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final Boolean value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object column(@NonNull final String column, @Nullable final byte[] value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), value);
    return this;
  }

  @NonNull
  @Override
  public Object emptyColumn(@NonNull final String column) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.putNull(checkNotNull(column));
    return this;
  }

  @NonNull
  @Override
  public Object where(@NonNull final String where) {
    checkState(!compiled, ERROR_MESSAGE);
    this.where = where;
    return this;
  }

  @NonNull
  @Override
  public Object arguments(@NonNull final String... arguments) {
    checkState(!compiled, ERROR_MESSAGE);
    this.arguments = arguments.clone();
    return this;
  }

  @NonNull
  @Override
  public Object failOnConflict() {
    checkState(!compiled, ERROR_MESSAGE);
    conflictAlgorithm = CONFLICT_FAIL;
    return this;
  }

  @NonNull
  @Override
  public Object ignoreOnConflict() {
    checkState(!compiled, ERROR_MESSAGE);
    conflictAlgorithm = CONFLICT_IGNORE;
    return this;
  }

  @NonNull
  @Override
  public Object replaceOnConflict() {
    checkState(!compiled, ERROR_MESSAGE);
    conflictAlgorithm = CONFLICT_REPLACE;
    return this;
  }

  @NonNull
  @Override
  public Object compile() {
    checkState(!compiled, ERROR_MESSAGE);
    this.compiled = true;
    switch (type) {
      case SQL_DELETE_REQUEST:
        return new SqlDeleteRequest(arguments, table, where);
      case SQL_INSERT_REQUEST:
        return new SqlInsertRequest(contentValues, table, conflictAlgorithm);
      case SQL_UPDATE_REQUEST:
        return new SqlUpdateRequest(contentValues, arguments, table, where, conflictAlgorithm);
      default:
        return new SqlRequest(arguments, query);
    }
  }
}
