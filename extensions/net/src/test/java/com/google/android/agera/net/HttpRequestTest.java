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
package com.google.android.agera.net;

import static com.google.android.agera.net.HttpRequestCompiler.CONNECT_TIMEOUT_MS;
import static com.google.android.agera.net.HttpRequestCompiler.READ_TIMEOUT_MS;
import static com.google.android.agera.net.HttpRequests.httpDeleteRequest;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;
import static com.google.android.agera.net.HttpRequests.httpPostRequest;
import static com.google.android.agera.net.HttpRequests.httpPutRequest;
import static com.google.android.agera.net.test.matchers.HasHashCodeOf.hasHashCodeOf;
import static com.google.android.agera.net.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.robolectric.annotation.Config.NONE;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class HttpRequestTest {
  private static final String URL = "http://agera";
  private static final byte[] DATA = "Body data".getBytes();

  @Test
  public void shouldCreateHttpGetRequest() {
    final HttpRequest httpRequest = httpGetRequest(URL).compile();

    assertThat(httpRequest.method, is("GET"));
    assertThat(httpRequest.url, is(URL));
  }

  @Test
  public void shouldCreateHttpPostRequest() {
    final HttpRequest httpRequest = httpPostRequest(URL).compile();

    assertThat(httpRequest.method, is("POST"));
    assertThat(httpRequest.url, is(URL));
  }

  @Test
  public void shouldCreateHttpPostRequestWithData() {
    final HttpRequest httpRequest = httpPostRequest(URL).body(DATA).compile();

    assertThat(httpRequest.method, is("POST"));
    assertThat(httpRequest.url, is(URL));
    assertThat(httpRequest.body, is(DATA));
  }

  @Test
  public void shouldCreateHttpPutRequest() {
    final HttpRequest httpRequest = httpPutRequest(URL).compile();

    assertThat(httpRequest.method, is("PUT"));
    assertThat(httpRequest.url, is(URL));
  }

  @Test
  public void shouldCreateHttpPutRequestWithBody() {
    final HttpRequest httpRequest = httpPutRequest(URL).body(DATA).compile();

    assertThat(httpRequest.method, is("PUT"));
    assertThat(httpRequest.url, is(URL));
    assertThat(httpRequest.body, is(DATA));
  }

  @Test
  public void shouldCreateHttpDeleteRequest() {
    final HttpRequest httpRequest = httpDeleteRequest(URL).compile();

    assertThat(httpRequest.method, is("DELETE"));
    assertThat(httpRequest.url, is(URL));
  }

  @Test
  public void shouldCreateSetHeaderFields() {
    final HttpRequest httpRequest = httpGetRequest(URL)
        .headerField("HEADER1", "VALUE1")
        .headerField("HEADER2", "VALUE2")
        .compile();

    final Map<String, String> header = httpRequest.header;
    assertThat(header, hasEntry("HEADER1", "VALUE1"));
    assertThat(header, hasEntry("HEADER2", "VALUE2"));
  }

  @Test
  public void shouldHaveDefaultValuesForRedirectCachesAndTimeouts() {
    final HttpRequest httpRequest = httpDeleteRequest(URL).compile();

    assertThat(httpRequest.connectTimeoutMs, is(CONNECT_TIMEOUT_MS));
    assertThat(httpRequest.readTimeoutMs, is(READ_TIMEOUT_MS));
    assertThat(httpRequest.followRedirects, is(true));
    assertThat(httpRequest.useCaches, is(true));
  }

  @Test
  public void shouldDisableCaches() {
    assertThat(httpDeleteRequest(URL).noCaches().compile().useCaches, is(false));
  }

  @Test
  public void shouldDisableFollowRedirects() {
    assertThat(httpDeleteRequest(URL).noRedirects().compile().followRedirects, is(false));
  }

  @Test
  public void shouldSetReadTimeout() {
    assertThat(httpDeleteRequest(URL).readTimeoutMs(2).compile().readTimeoutMs, is(2));
  }

  @Test
  public void shouldSetConnectTimeout() {
    assertThat(httpDeleteRequest(URL).connectTimeoutMs(3).compile().connectTimeoutMs, is(3));
  }

  @Test
  public void shouldBeEqualForSameData() {
    assertThat(httpGetRequest(URL).compile(), is(httpGetRequest(URL).compile()));
  }

  @Test
  public void shouldHaveToString() {
    assertThat(httpGetRequest(URL).compile(), hasToString(not(isEmptyOrNullString())));
  }

  @Test
  public void shouldHaveSameHashcodeForSameData() {
    assertThat(httpGetRequest(URL).compile(),
        hasHashCodeOf(httpGetRequest(URL).compile()));
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(HttpRequests.class, hasPrivateConstructor());
  }
}