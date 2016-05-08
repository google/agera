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

import android.content.ContentValues;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * An immutable object representing a sql update request.
 */
public final class SqlUpdateRequest {
  @NonNull
  final ContentValues contentValues;
  @NonNull
  final String[] arguments;
  @NonNull
  final String table;
  @NonNull
  final String where;
  final int conflictAlgorithm;

  SqlUpdateRequest(@NonNull final ContentValues contentValues, @NonNull final String[] arguments,
      @NonNull final String table, @NonNull final String where, final int conflictAlgorithm) {
    this.conflictAlgorithm = conflictAlgorithm;
    this.table = checkNotNull(table);
    this.where = checkNotNull(where);
    this.contentValues = checkNotNull(contentValues);
    this.arguments = checkNotNull(arguments);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SqlUpdateRequest)) {
      return false;
    }

    final SqlUpdateRequest that = (SqlUpdateRequest) o;

    return conflictAlgorithm == that.conflictAlgorithm
        && contentValues.equals(that.contentValues)
        && Arrays.equals(arguments, that.arguments)
        && table.equals(that.table)
        && where.equals(that.where);
  }

  @Override
  public int hashCode() {
    int result = contentValues.hashCode();
    result = 31 * result + Arrays.hashCode(arguments);
    result = 31 * result + table.hashCode();
    result = 31 * result + where.hashCode();
    result = 31 * result + conflictAlgorithm;
    return result;
  }

  @Override
  public String toString() {
    return "SqlUpdateRequest{" +
        "contentValues=" + contentValues +
        ", arguments=" + Arrays.toString(arguments) +
        ", table='" + table + '\'' +
        ", where='" + where + '\'' +
        ", conflictAlgorithm=" + conflictAlgorithm +
        '}';
  }
}
