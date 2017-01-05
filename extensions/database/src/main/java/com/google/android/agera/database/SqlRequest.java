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

import android.support.annotation.NonNull;
import java.util.Arrays;

/**
 * An immutable object representing a sql request.
 */
public final class SqlRequest {
  @NonNull
  final String[] arguments;
  @NonNull
  final String sql;

  SqlRequest(@NonNull final String[] arguments, final @NonNull String sql) {
    this.sql = checkNotNull(sql);
    this.arguments = checkNotNull(arguments);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SqlRequest)) {
      return false;
    }

    final SqlRequest that = (SqlRequest) o;

    return Arrays.equals(arguments, that.arguments)
        && sql.equals(that.sql);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(arguments);
    result = 31 * result + sql.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "SqlRequest{" +
        "arguments=" + Arrays.toString(arguments) +
        ", sql='" + sql + '\'' +
        '}';
  }
}
