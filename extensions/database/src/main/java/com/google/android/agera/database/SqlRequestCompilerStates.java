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
import android.support.annotation.Nullable;

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
     * Adds a {@code column} with a {@link String} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable String value);

    /**
     * Adds a {@code column} with a {@link Byte} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Byte value);

    /**
     * Adds a {@code column} with a {@link Short} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Short value);

    /**
     * Adds a {@code column} with a {@link Integer} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Integer value);

    /**
     * Adds a {@code column} with a {@link Long} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Long value);

    /**
     * Adds a {@code column} with a {@link Float} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Float value);

    /**
     * Adds a {@code column} with a {@link Double} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Double value);

    /**
     * Adds a {@code column} with a {@link Boolean} {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable Boolean value);

    /**
     * Adds a {@code column} with a {@code byte} array {@code value}.
     */
    @NonNull
    T column(@NonNull String column, @Nullable byte[] value);

    /**
     * Adds an empty {@code column}.
     */
    @NonNull
    T emptyColumn(@NonNull String column);
  }

  /**
   * Compiler state allowing to add conflict algorithm.
   * <p>
   * The default algorithm aborts the current SQL statement with an SQLITE_CONSTRAINT
   * error and backs out any changes made.
   */
  interface DBConflict<T> {

    /**
     * When a constraint violation occurs, the command aborts with a return code SQLITE_CONSTRAINT.
     */
    @NonNull
    T failOnConflict();

    /**
     * When a constraint violation occurs, the one row that contains the constraint violation is not
     * inserted or changed.
     */
    @NonNull
    T ignoreOnConflict();

    /**
     * When a UNIQUE constraint violation occurs, the pre-existing rows that are causing the
     * constraint violation are removed prior to inserting or updating the current row.
     */
    @NonNull
    T replaceOnConflict();
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
  interface DBArgumentCompile<T, TC> extends DBArgument<TC>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify sql arguments, conflict algorithm or compile.
   */
  interface DBArgumentConflictCompile<T, TCc> extends DBArgument<TCc>, DBConflictCompile<T> {}

  /**
   * Compiler state allowing to specify columns, a conflict algorithm or compile.
   */
  interface DBColumnConflictCompile<T, TSelf extends DBColumnConflictCompile<T, TSelf>>
      extends DBColumn<TSelf>, DBConflictCompile<T> {}

  /**
   * Compiler state allowing to specify a where clause or compile.
   */
  interface DBWhereCompile<T, TAc> extends DBWhere<TAc>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify a conflict algorithm or compile.
   */
  interface DBConflictCompile<T> extends DBConflict<DBCompile<T>>, DBCompile<T> {}

  /**
   * Compiler state allowing to specify a conflict algorithm, a where clause or compile.
   */
  interface DBWhereConflictCompile<T, TAcc> extends DBWhere<TAcc>, DBConflictCompile<T> {}

  /**
   * Compiler state allowing to specify a column, conflict algorithm, where clause or compile.
   */
  interface DBColumnWhereConflictCompile<T, TAac, TSelf
      extends DBColumnWhereConflictCompile<T, TAac,TSelf>>
      extends DBColumn<TSelf>, DBWhereConflictCompile<T, TAac> {}
}
