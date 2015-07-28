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

import static com.google.android.agera.database.SqlRequestCompiler.SQL_DELETE_REQUEST;
import static com.google.android.agera.database.SqlRequestCompiler.SQL_INSERT_REQUEST;
import static com.google.android.agera.database.SqlRequestCompiler.SQL_REQUEST;
import static com.google.android.agera.database.SqlRequestCompiler.SQL_UPDATE_REQUEST;

import com.google.android.agera.database.SqlRequestCompilerStates.DBArgumentCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumn;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBColumnWhereCompile;
import com.google.android.agera.database.SqlRequestCompilerStates.DBSql;
import com.google.android.agera.database.SqlRequestCompilerStates.DBTable;
import com.google.android.agera.database.SqlRequestCompilerStates.DBWhereCompile;

import android.support.annotation.NonNull;

/**
 * Creates sql requests.
 */
public final class SqlRequests {
  /**
   * Starts the creation of a {@link SqlRequest}.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static DBSql<DBArgumentCompile<SqlRequest>> sqlRequest() {
    return new SqlRequestCompiler(SQL_REQUEST);
  }

  /**
   * Starts the creation of a {@link SqlDeleteRequest}.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static DBTable<DBWhereCompile<SqlDeleteRequest>> sqlDeleteRequest() {
    return new SqlRequestCompiler(SQL_DELETE_REQUEST);
  }

  /**
   * Starts the creation of a {@link SqlDeleteRequest}.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static DBTable<DBColumn<DBColumnCompile<SqlInsertRequest, ?>>> sqlInsertRequest() {
    return new SqlRequestCompiler(SQL_INSERT_REQUEST);
  }

  /**
   * Starts the creation of a {@link SqlUpdateRequest}.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static DBTable<DBColumn<DBColumnWhereCompile<SqlUpdateRequest, ?>>> sqlUpdateRequest() {
    return new SqlRequestCompiler(SQL_UPDATE_REQUEST);
  }

  private SqlRequests() {}
}
