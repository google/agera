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

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import com.google.android.agera.database.SqlRequestCompilerStates.DBArgumentCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnWhereCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBSql;
import com.google.android.agera.database.SqlRequestCompilerStates.DBTable;

import android.content.ContentValues;
import android.support.annotation.NonNull;

final class SqlRequestCompiler
    implements DBTable, DBSql, DBArgumentCompile, DBColumnCompile, DBColumnWhereCompile {
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

  SqlRequestCompiler(final int type) {
    this.type = type;
    this.where = "";
    this.contentValues = new ContentValues();
    this.arguments = NO_ARGUMENTS;
    this.compiled = false;
    this.table = "";
    this.query = "";
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
  public Object column(@NonNull final String column, @NonNull final String value) {
    checkState(!compiled, ERROR_MESSAGE);
    contentValues.put(checkNotNull(column), checkNotNull(value));
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
  public Object compile() {
    checkState(!compiled, ERROR_MESSAGE);
    this.compiled = true;
    switch (type) {
      case SQL_DELETE_REQUEST:
        return new SqlDeleteRequest(arguments, table, where);
      case SQL_INSERT_REQUEST:
        return new SqlInsertRequest(contentValues, table);
      case SQL_UPDATE_REQUEST:
        return new SqlUpdateRequest(contentValues, arguments, table, where);
      default:
        return new SqlRequest(arguments, query);
    }
  }
}
