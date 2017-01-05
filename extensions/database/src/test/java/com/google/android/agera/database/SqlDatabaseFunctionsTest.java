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
import static android.database.sqlite.SQLiteDatabase.create;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseDeleteFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseInsertFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseQueryFunction;
import static com.google.android.agera.database.SqlDatabaseFunctions.databaseUpdateFunction;
import static com.google.android.agera.database.SqlRequests.sqlDeleteRequest;
import static com.google.android.agera.database.SqlRequests.sqlInsertRequest;
import static com.google.android.agera.database.SqlRequests.sqlRequest;
import static com.google.android.agera.database.SqlRequests.sqlUpdateRequest;
import static com.google.android.agera.database.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.robolectric.annotation.Config.NONE;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import com.google.android.agera.Supplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public final class SqlDatabaseFunctionsTest {
  private static final String SELECT_TABLE = "SELECT * FROM test";
  private static final String HAS_VALUE = "SELECT * FROM test WHERE column='value'";
  private static final String TABLE = "test";
  private static final String INVALID_TABLE = "test invalid$";
  private static final String SQL_QUERY = "SELECT * FROM test ORDER BY column";
  private static final String INVALID_QUERY = "invalid query";
  private static final String SQL_QUERY_FOR_ARGUMENT = "SELECT * FROM test WHERE column=?";
  private static final String NON_MATCHING_SQL_QUERY =
      "SELECT * FROM test WHERE column='a' ORDER BY column";
  private static final Supplier<Result<SQLiteDatabase>> FAILURE =
      staticSupplier(Result.<SQLiteDatabase>failure(new Exception()));
  private static final CursorStringFunction CURSOR_STRING_FUNCTION = new CursorStringFunction();
  public static final String COLUMN = "column";

  private SQLiteDatabase database;
  private Supplier<Result<SQLiteDatabase>> databaseSupplier;

  @Before
  public void setUp() {
    database = create(null);
    database.execSQL("CREATE TABLE test (column varchar(255))");
    database.execSQL("INSERT INTO test (column) VALUES ('value1')");
    database.execSQL("INSERT INTO test (column) VALUES ('value2')");
    database.execSQL("INSERT INTO test (column) VALUES ('value3')");
    databaseSupplier = staticSupplier(success(database));
  }

  @After
  public void tearDown() {
    database.close();
  }

  @Test
  public void shouldGetValuesForDatabaseQuery() throws Throwable {
    assertThat(databaseQueryFunction(databaseSupplier, CURSOR_STRING_FUNCTION)
            .apply(sqlRequest()
                .sql(SQL_QUERY)
                .compile()).get(),
        contains("value1", "value2", "value3"));
  }

  @Test
  public void shouldNotGetValuesForDatabaseQueryWithNonMatchingWhere() throws Throwable {
    assertThat(databaseQueryFunction(databaseSupplier, CURSOR_STRING_FUNCTION)
            .apply(sqlRequest()
                .sql(NON_MATCHING_SQL_QUERY)
                .compile()).get(),
        empty());
  }

  @Test
  public void shouldReturnFailureForInvalidQuery() {
    assertThat(databaseQueryFunction(databaseSupplier, CURSOR_STRING_FUNCTION)
            .apply(sqlRequest()
                .sql(INVALID_QUERY)
                .compile()).getFailure(),
        instanceOf(SQLException.class));
  }

  @Test
  public void shouldPassArgumentsToDatabaseQuery() throws Throwable {
    assertThat(databaseQueryFunction(databaseSupplier, CURSOR_STRING_FUNCTION)
            .apply(sqlRequest()
                .sql(SQL_QUERY_FOR_ARGUMENT)
                .arguments("value2").compile()).get(),
        contains("value2"));
  }

  @Test
  public void shouldReturnErrorForFailedDatabaseCreationInQuery() throws Throwable {
    assertThat(databaseQueryFunction(FAILURE, CURSOR_STRING_FUNCTION)
            .apply(sqlRequest()
                .sql(SQL_QUERY_FOR_ARGUMENT)
                .arguments("value2")
                .compile()).failed(),
        is(true));
  }

  @Test
  public void shouldClearTableForDeleteWithoutArguments() throws Throwable {
    assertThat(databaseDeleteFunction(databaseSupplier)
            .apply(sqlDeleteRequest()
                .table(TABLE)
                .compile()).get(),
        is(3));
    assertDatabaseEmpty();
  }

  @Test
  public void shouldReturnFailureForInvalidDelete() {
    assertThat(databaseDeleteFunction(databaseSupplier)
            .apply(sqlDeleteRequest()
                .table(INVALID_TABLE)
                .compile()).getFailure(),
        instanceOf(SQLException.class));
  }

  @Test
  public void shouldPassArgumentsToDatabaseDelete() throws Throwable {
    assertThat(databaseDeleteFunction(databaseSupplier)
            .apply(sqlDeleteRequest()
                .table(TABLE)
                .where("column=?")
                .arguments("value2")
                .compile()).get(),
        is(1));
  }

  @Test
  public void shouldReturnErrorForFailedDatabaseCreationInDelete() throws Throwable {
    assertThat(databaseDeleteFunction(FAILURE)
            .apply(sqlDeleteRequest()
                .table(TABLE)
                .where("column=?")
                .arguments("value2")
                .compile()).failed(),
        is(true));
  }

  @Test
  public void shouldUpdateTableForUpdateWithoutArguments() throws Throwable {
    assertThat(databaseUpdateFunction(databaseSupplier)
            .apply(sqlUpdateRequest()
                .table(TABLE)
                .column("column", "value4")
                .compile()).get(),
        is(3));
  }

  @Test
  public void shouldReturnFailureForInvalidUpdate() {
    assertThat(databaseUpdateFunction(databaseSupplier)
            .apply(sqlUpdateRequest()
                .table(INVALID_TABLE)
                .emptyColumn("column")
                .compile()).getFailure(),
        instanceOf(SQLException.class));
  }

  @Test
  public void shouldPassArgumentsToDatabaseUpdate() throws Throwable {
    assertThat(databaseUpdateFunction(databaseSupplier)
            .apply(sqlUpdateRequest()
                .table(TABLE)
                .column("column", "value4")
                .where("column=?")
                .arguments("value3")
                .compile()).get(),
        is(1));
  }

  @Test
  public void shouldReturnErrorForFailedDatabaseCreationInUpdate() throws Throwable {
    assertThat(databaseUpdateFunction(FAILURE)
            .apply(sqlUpdateRequest()
                .table(TABLE)
                .column("column", "value4")
                .where("column=?")
                .arguments("value3")
                .compile()).failed(),
        is(true));
  }

  @Test
  public void shouldReturnFailureForInvalidInsert() {
    assertThat(databaseInsertFunction(databaseSupplier)
            .apply(sqlInsertRequest()
                .table(INVALID_TABLE)
                .emptyColumn("column")
                .compile()).getFailure(),
        instanceOf(SQLException.class));
  }

  @Test
  public void shouldPassArgumentsToDatabaseInsert() throws Throwable {
    assertThat(databaseInsertFunction(databaseSupplier)
            .apply(sqlInsertRequest()
                .table(TABLE)
                .column("column", "value")
                .compile()).succeeded(),
        is(true));
    assertDatabaseContainsValue();
  }

  @Test
  public void shouldAddFailConflictAlgorithmForUpdate() {
    assertThat(sqlUpdateRequest()
                .table(TABLE)
                .column("column", "value4")
                .where("column=?")
                .arguments("value3")
                .failOnConflict()
                .compile().conflictAlgorithm,
        is(CONFLICT_FAIL));
  }

  @Test
  public void shouldAddReplaceConflictAlgorithmForUpdate() {
    assertThat(sqlUpdateRequest()
            .table(TABLE)
            .column("column", "value4")
            .where("column=?")
            .arguments("value3")
            .replaceOnConflict()
            .compile().conflictAlgorithm,
        is(CONFLICT_REPLACE));
  }

  @Test
  public void shouldAddIgnoreConflictAlgorithmForUpdate() {
    assertThat(sqlUpdateRequest()
            .table(TABLE)
            .column("column", "value4")
            .where("column=?")
            .arguments("value3")
            .ignoreOnConflict()
            .compile().conflictAlgorithm,
        is(CONFLICT_IGNORE));
  }

  @Test
  public void shouldNotAddConflictAlgorithmForUpdate() {
    assertThat(sqlUpdateRequest()
            .table(TABLE)
            .column("column", "value4")
            .where("column=?")
            .arguments("value3")
            .compile().conflictAlgorithm,
        is(CONFLICT_NONE));
  }

  @Test
  public void shouldNotAddConflictAlgorithmForInsert() {
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .emptyColumn("column")
            .compile().conflictAlgorithm,
        is(CONFLICT_NONE));
  }

  @Test
  public void shouldAddFailConflictAlgorithmForInsert() {
    assertThat(sqlInsertRequest()
                .table(TABLE)
                .emptyColumn("column")
                .failOnConflict()
                .compile().conflictAlgorithm,
        is(CONFLICT_FAIL));
  }

  @Test
  public void shouldAddIgnoreConflictAlgorithmForInsert() {
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .emptyColumn("column")
            .ignoreOnConflict()
            .compile().conflictAlgorithm,
        is(CONFLICT_IGNORE));
  }

  @Test
  public void shouldAddReplaceConflictAlgorithmForInsert() {
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .emptyColumn("column")
            .replaceOnConflict()
            .compile().conflictAlgorithm,
        is(CONFLICT_REPLACE));
  }

  @Test
  public void shouldReturnErrorForFailedDatabaseCreationInInsert() throws Throwable {
    assertThat(databaseInsertFunction(FAILURE)
            .apply(sqlInsertRequest()
                .table(TABLE)
                .column("column", "value")
                .compile()).failed(),
        is(true));
  }

  @Test
  public void shouldAddBooleanColumnForInsert() {
    final boolean value = true;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsBoolean(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullBooleanColumnForInsert() {
    final Boolean nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsBoolean(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddStringColumnForInsert() {
    final String value = "string";
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsString(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullStringColumnForInsert() {
    final String nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsString(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddByteColumnForInsert() {
    final byte value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsByte(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullByteColumnForInsert() {
    final Byte nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsByte(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddIntegerColumnForInsert() {
    final int value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsInteger(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullIntegerColumnForInsert() {
    final Integer nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsInteger(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddShortColumnForInsert() {
    final short value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsShort(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullShortColumnForInsert() {
    final Short nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsShort(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddDoubleColumnForInsert() {
    final double value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsDouble(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullDoubleColumnForInsert() {
    final Double nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsDouble(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddFloatColumnForInsert() {
    final float value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsFloat(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullFloatColumnForInsert() {
    final Float nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsFloat(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddLongColumnForInsert() {
    final long value = 2;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsLong(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullLongColumnForInsert() {
    final Long nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsLong(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldAddByteArrayColumnForInsert() {
    final byte[] value = "value".getBytes();
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, value)
            .compile().contentValues.getAsByteArray(COLUMN),
        is(value));
  }

  @Test
  public void shouldAddNullByteArrayColumnForInsert() {
    final byte[] nullValue = null;
    assertThat(sqlInsertRequest()
            .table(TABLE)
            .column(COLUMN, nullValue)
            .compile().contentValues.getAsByteArray(COLUMN),
        is(nullValue));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(SqlDatabaseFunctions.class, hasPrivateConstructor());
  }

  private static class CursorStringFunction implements Function<Cursor, String> {
    @NonNull
    @Override
    public String apply(@NonNull final Cursor input) {
      return input.getString(input.getColumnIndex("column"));
    }
  }

  private void assertDatabaseEmpty() {
    final Cursor cursor = database.rawQuery(SELECT_TABLE, null);
    try {
      assertThat(cursor.getCount(), is(0));
    } finally {
      cursor.close();
    }
  }

  private void assertDatabaseContainsValue() {
    final Cursor cursor = database.rawQuery(HAS_VALUE, null);
    try {
      assertThat(cursor.getCount(), is(1));
    } finally {
      cursor.close();
    }
  }
}
