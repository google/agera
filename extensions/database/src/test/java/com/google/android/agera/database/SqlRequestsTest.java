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
import static com.google.android.agera.database.test.matchers.HasHashCodeOf.hasHashCodeOf;
import static com.google.android.agera.database.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.robolectric.annotation.Config.NONE;

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
  public void shouldNotBeEqualForDifferentArguments() {
    assertThat(sqlRequest, not(equalTo(sqlRequest2)));
  }

  @Test
  public void shouldBeEqualForSameInstance() {
    assertThat(sqlRequest, equalTo(sqlRequest));
  }

  @Test
  public void shouldNotBeEqualForOtherType() {
    assertThat(sqlRequest, not(equalTo(new Object())));
  }

  @Test
  public void shouldBeEqualForSameArgumentsButDifferentInstance() {
    assertThat(sqlRequest, equalTo(sqlRequest().sql(SQL_QUERY).compile()));
  }

  @Test
  public void shouldHaveSameHashcodeForSameQueryStringButDifferentInstance() {
    assertThat(sqlRequest, hasHashCodeOf(sqlRequest().sql(SQL_QUERY).compile()));
  }

  @Test
  public void shouldNotBeEqualForDifferentArgumentsForDelete() {
    assertThat(sqlDeleteRequest, not(equalTo(sqlDeleteRequest2)));
  }

  @Test
  public void shouldBeEqualForSameInstanceForDelete() {
    assertThat(sqlDeleteRequest, equalTo(sqlDeleteRequest));
  }

  @Test
  public void shouldNotBeEqualForOtherTypeForDelete() {
    assertThat(sqlDeleteRequest, not(equalTo(new Object())));
  }

  @Test
  public void shouldBeEqualForSameArgumentsButDifferentInstanceForDelete() {
    assertThat(sqlDeleteRequest, equalTo(sqlDeleteRequest().table(TABLE).compile()));
  }

  @Test
  public void shouldHaveSameHashcodeForSameQueryStringButDifferentInstanceForDelete() {
    assertThat(sqlDeleteRequest, hasHashCodeOf(sqlDeleteRequest().table(TABLE).compile()));
  }

  @Test
  public void shouldNotBeEqualForDifferentArgumentsForUpdate() {
    assertThat(sqlUpdateRequest, not(equalTo(sqlUpdateRequest2)));
  }

  @Test
  public void shouldBeEqualForSameInstanceForUpdate() {
    assertThat(sqlUpdateRequest, equalTo(sqlUpdateRequest));
  }

  @Test
  public void shouldNotBeEqualForOtherTypeForUpdate() {
    assertThat(sqlUpdateRequest, not(equalTo(new Object())));
  }

  @Test
  public void shouldBeEqualForSameArgumentsButDifferentInstanceForUpdate() {
    assertThat(sqlUpdateRequest,
        equalTo(sqlUpdateRequest().table(TABLE).column("column", "value4").compile()));
  }

  @Test
  public void shouldHaveSameHashcodeForSameQueryStringButDifferentInstanceForUpdate() {
    assertThat(sqlUpdateRequest,
        hasHashCodeOf(sqlUpdateRequest().table(TABLE).column("column", "value4").compile()));
  }

  @Test
  public void shouldNotBeEqualForDifferentArgumentsForInsert() {
    assertThat(sqlInsertRequest, not(equalTo(sqlInsertRequest2)));
  }

  @Test
  public void shouldBeEqualForSameInstanceForInsert() {
    assertThat(sqlInsertRequest, equalTo(sqlInsertRequest));
  }

  @Test
  public void shouldNotBeEqualForOtherTypeForInsert() {
    assertThat(sqlInsertRequest, not(equalTo(new Object())));
  }

  @Test
  public void shouldBeEqualForSameArgumentsButDifferentInstanceForInsert() {
    assertThat(sqlInsertRequest,
        equalTo(sqlInsertRequest().table(TABLE).column("column", "value").compile()));
  }

  @Test
  public void shouldHaveSameHashcodeForSameQueryStringButDifferentInstanceForInsert() {
    assertThat(sqlInsertRequest,
        hasHashCodeOf(sqlInsertRequest().table(TABLE).column("column", "value").compile()));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(SqlRequests.class, hasPrivateConstructor());
  }
}
