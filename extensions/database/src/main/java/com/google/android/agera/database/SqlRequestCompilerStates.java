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

import android.support.annotation.NonNull;

/**
 * Container of the compiler state interfaces supporting the creation of sql requests.
 */
public interface SqlRequestCompilerStates {

  /**
   * Compiler state allowing to specify what raw sql to execute/query.
   */
  interface DBSql<T> {

    /**
     * Sets sql string.
     */
    @NonNull
    T sql(@NonNull String sql);
  }

  /**
   * Compiler state allowing to specify what table to operate on.
   */
  interface DBTable<T> {

    /**
     * Sets a table.
     */
    @NonNull
    T table(@NonNull String table);
  }

  /**
   * Compiler state allowing to specify what items to update/delete.
   */
  interface DBWhere<T> {

    /**
     * Sets a where clause for update/delete instructions.
     */
    @NonNull
    T where(@NonNull String where);
  }

  /**
   * Compiler state allowing to add arguments.
   */
  interface DBArgument<T> {
    @NonNull
    T arguments(@NonNull String... arguments);
  }

  /**
   * Compiler state allowing to add columns.
   */
  interface DBColumn<T> {

    /**
     * Adds a {@code column} with a {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @NonNull String value);

    /**
     * Adds an empty {@code column}.
     */
    @NonNull
    T emptyColumn(@NonNull String column);
  }

  /**
   * Compiler state to compile the sql request.
   */
  interface DBCompile<T> {

    /**
     * Compiles a sql request that containing the previously specified data.
     */
    @NonNull
    T compile();
  }

  /**
   * Compiler state allowing to specify sql arguments or compile.
   */
  interface DBArgumentCompile<T> extends DBArgument<DBCompile<T>>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify columns or compile.
   */
  interface DBColumnCompile<T, TSelf extends DBColumnCompile<T, TSelf>>
      extends DBColumn<TSelf>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify a where clause or compile.
   */
  interface DBWhereCompile<T> extends DBWhere<DBArgumentCompile<T>>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify a column, where clause or compile.
   */
  interface DBColumnWhereCompile<T, TSelf extends DBColumnWhereCompile<T, TSelf>>
      extends DBColumn<TSelf>, DBWhereCompile<T> {}
}
