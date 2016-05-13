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
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;

import com.google.android.agera.Function;
import com.google.android.agera.Merger;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility methods for obtaining database querying {@link Function} instances.
 */
public final class SqlDatabaseFunctions {

  /**
   * Creates a sql query {@link Function}.
   */
  @NonNull
  public static <T> Function<SqlRequest, Result<List<T>>> databaseQueryFunction(
      @NonNull final Supplier<Result<SQLiteDatabase>> database,
      @NonNull Function<Cursor, T> rowMap) {
    return new DatabaseFunction<>(database, new DatabaseQueryMerger<>(rowMap));
  }

  /**
   * Creates a sql insert {@link Function}.
   */
  @NonNull
  public static Function<SqlInsertRequest, Result<Long>> databaseInsertFunction(
      @NonNull final Supplier<Result<SQLiteDatabase>> database) {
    return new DatabaseFunction<>(database, new DatabaseInsertMerger());
  }

  /**
   * Creates a sql update {@link Function}.
   */
  @NonNull
  public static Function<SqlUpdateRequest, Result<Integer>> databaseUpdateFunction(
      @NonNull final Supplier<Result<SQLiteDatabase>> database) {
    return new DatabaseFunction<>(database, new DatabaseUpdateMerger());
  }

  /**
   * Creates a sql delete {@link Function}.
   */
  @NonNull
  public static Function<SqlDeleteRequest, Result<Integer>> databaseDeleteFunction(
      @NonNull final Supplier<Result<SQLiteDatabase>> database) {
    return new DatabaseFunction<>(database, new DatabaseDeleteMerger());
  }

  private static final class DatabaseInsertMerger
      implements Merger<SQLiteDatabase, SqlInsertRequest, Result<Long>> {

    @NonNull
    @Override
    public Result<Long> merge(@NonNull final SQLiteDatabase database,
        @NonNull final SqlInsertRequest input) {
      try {
        return success(database.insertWithOnConflict(input.table, null, input.contentValues,
            input.conflictAlgorithm));
      } catch (final SQLException e) {
        return failure(e);
      }
    }
  }

  private static final class DatabaseUpdateMerger
      implements Merger<SQLiteDatabase, SqlUpdateRequest, Result<Integer>> {

    @NonNull
    @Override
    public Result<Integer> merge(@NonNull final SQLiteDatabase database,
        @NonNull final SqlUpdateRequest input) {
      try {
        return success(database.updateWithOnConflict(input.table, input.contentValues,
            input.where, input.arguments, input.conflictAlgorithm));
      } catch (final SQLException e) {
        return failure(e);
      }
    }
  }

  private static final class DatabaseDeleteMerger
      implements Merger<SQLiteDatabase, SqlDeleteRequest, Result<Integer>> {

    @NonNull
    @Override
    public Result<Integer> merge(@NonNull final SQLiteDatabase database,
        @NonNull final SqlDeleteRequest input) {
      try {
        return success(database.delete(input.table, input.where, input.arguments));
      } catch (final SQLException e) {
        return failure(e);
      }
    }
  }

  private static final class DatabaseQueryMerger<T>
      implements Merger<SQLiteDatabase, SqlRequest, Result<List<T>>> {
    @NonNull
    private final Function<Cursor, T> cursorToItem;

    private DatabaseQueryMerger(@NonNull final Function<Cursor, T> cursorToItem) {
      this.cursorToItem = checkNotNull(cursorToItem);
    }

    @NonNull
    @Override
    public Result<List<T>> merge(@NonNull final SQLiteDatabase database,
        @NonNull final SqlRequest input) {
      try {
        final Cursor cursor = database.rawQuery(input.sql, input.arguments);
        try {
          final int count = cursor.getCount();
          if (count == 0) {
            return success(Collections.<T>emptyList());
          }
          final List<T> items = new ArrayList<>(count);
          while (cursor.moveToNext()) {
            items.add(cursorToItem.apply(cursor));
          }
          return success(items);
        } finally {
          cursor.close();
        }
      } catch (final SQLException e) {
        return failure(e);
      }
    }
  }

  static final class DatabaseFunction<R, T> implements Function<R, Result<T>> {
    @NonNull
    private final Supplier<Result<SQLiteDatabase>> databaseSupplier;
    @NonNull
    private final Merger<SQLiteDatabase, R, Result<T>> databaseWithSqlArgument;

    DatabaseFunction(@NonNull final Supplier<Result<SQLiteDatabase>> databaseSupplier,
        @NonNull final Merger<SQLiteDatabase, R, Result<T>>
            databaseWithSqlArgumentMerger) {
      this.databaseSupplier = checkNotNull(databaseSupplier);
      this.databaseWithSqlArgument = checkNotNull(databaseWithSqlArgumentMerger);
    }

    @NonNull
    @Override
    public Result<T> apply(@NonNull final R sqlArguments) {
      return databaseSupplier.get().ifSucceededAttemptMerge(sqlArguments, databaseWithSqlArgument);
    }
  }

  private SqlDatabaseFunctions() {}
}
