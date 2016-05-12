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

import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.Result.failure;

import com.google.android.agera.Result;
import com.google.android.agera.Supplier;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Abstract extension of {@link SQLiteOpenHelper} implementing a sql database {@link Supplier} to be
 * used with the {@link SqlDatabaseFunctions}.
 */
public abstract class SqlDatabaseSupplier extends SQLiteOpenHelper
    implements Supplier<Result<SQLiteDatabase>> {

  /**
   * Extending the base constructor, for overriding in concrete implementations.
   */
  public SqlDatabaseSupplier(@NonNull final Context context, @NonNull final String path,
      @Nullable final CursorFactory factory, final int version) {
    super(context, path, factory, version);
  }

  @NonNull
  @Override
  public final synchronized Result<SQLiteDatabase> get() {
    try {
      return absentIfNull(getWritableDatabase());
    } catch (final SQLException e) {
      return failure(e);
    }
  }
}
