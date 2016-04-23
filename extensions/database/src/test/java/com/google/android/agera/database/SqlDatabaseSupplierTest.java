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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.annotation.Config.NONE;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public final class SqlDatabaseSupplierTest {
  private static final String DATABASE_NAME = "database";

  @Test
  public void shouldSupplyDatabase() throws Throwable {
    final SqlDatabaseSupplier sqlDatabaseSupplier = new SqlDatabaseSupplier(
        application.getApplicationContext(), DATABASE_NAME, null, 1) {
      @Override
      public void onCreate(final SQLiteDatabase sqLiteDatabase) {}

      @Override
      public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int i, final int i1) {}
    };

    assertThat(sqlDatabaseSupplier.get().get(), instanceOf(SQLiteDatabase.class));
  }

  @Test
  public void shouldSupplyFailureIfFailingOnCreate() throws Throwable {
    final SqlDatabaseSupplier sqlDatabaseSupplier = new SqlDatabaseSupplier(
        application.getApplicationContext(), DATABASE_NAME, null, 1) {
      @Override
      public void onCreate(final SQLiteDatabase sqLiteDatabase) {
        throw new SQLException();
      }

      @Override
      public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int i, final int i1) {}
    };

    assertThat(sqlDatabaseSupplier.get().failed(), is(true));
  }
}
