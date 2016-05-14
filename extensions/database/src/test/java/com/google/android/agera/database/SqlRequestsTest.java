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

import static com.google.android.agera.database.SqlRequests.sqlDeleteRequest;
import static com.google.android.agera.database.SqlRequests.sqlInsertRequest;
import static com.google.android.agera.database.SqlRequests.sqlRequest;
import static com.google.android.agera.database.SqlRequests.sqlUpdateRequest;
import static com.google.android.agera.database.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.robolectric.annotation.Config.NONE;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = NONE)
public final class SqlRequestsTest {
  private static final String SQL_QUERY = "SELECT * FROM test ORDER BY column";
  private static final String SQL_QUERY_2 = "SELECT * FROM test2 ORDER BY column";
  private static final String TABLE = "test";
  private static final String TABLE_2 = "test2";
  private SqlRequest sqlRequest;
  private SqlDeleteRequest sqlDeleteRequest;
  private SqlUpdateRequest sqlUpdateRequest;
  private SqlInsertRequest sqlInsertRequest;
  private SqlRequest sqlRequest2;
  private SqlDeleteRequest sqlDeleteRequest2;
  private SqlInsertRequest sqlInsertRequest2;
  private SqlUpdateRequest sqlUpdateRequest2;

  @Before
  public void setUp() {
    sqlRequest = sqlRequest()
        .sql(SQL_QUERY)
        .compile();
    sqlRequest2 = sqlRequest()
        .sql(SQL_QUERY_2)
        .compile();
    sqlDeleteRequest = sqlDeleteRequest()
        .table(TABLE)
        .compile();
    sqlDeleteRequest2 = sqlDeleteRequest()
        .table(TABLE_2)
        .compile();
    sqlUpdateRequest = sqlUpdateRequest()
        .table(TABLE)
        .column("column", "value4")
        .compile();
    sqlUpdateRequest2 = sqlUpdateRequest()
        .table(TABLE_2)
        .column("column", "value4")
        .compile();
    sqlInsertRequest = sqlInsertRequest()
        .table(TABLE)
        .column("column", "value")
        .compile();
    sqlInsertRequest2 = sqlInsertRequest()
        .table(TABLE_2)
        .column("column", "value")
        .compile();
  }

  @Test
  public void shouldCreateStringRepresentationForRequest() {
    assertThat(sqlRequest, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldCreateStringRepresentationForDelete() {
    assertThat(sqlDeleteRequest, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldCreateStringRepresentationForInsert() {
    assertThat(sqlInsertRequest, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldCreateStringRepresentationForUpdate() {
    assertThat(sqlUpdateRequest, hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldVerifyEqualsForSqlRequest() {
    EqualsVerifier.forClass(SqlRequest.class).verify();
  }

  @Test
  public void shouldVerifyEqualsForSqlDeleteRequest() {
    EqualsVerifier.forClass(SqlDeleteRequest.class).verify();
  }

  @Test
  public void shouldVerifyEqualsForSqlUpdateRequest() {
    EqualsVerifier.forClass(SqlUpdateRequest.class).verify();
  }

  @Test
  public void shouldVerifyEqualsForSqlInsertRequest() {
    EqualsVerifier.forClass(SqlInsertRequest.class).verify();
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(SqlRequests.class, hasPrivateConstructor());
  }
}
