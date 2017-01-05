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

import static com.google.android.agera.Result.success;
import static com.google.android.agera.net.HttpResponse.httpResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public final class HttpResponseTest {
  private static final String DEFAULT_CHARSET = "UTF-8";
  private static final String UTF16_CHARSET = "UTF-16";
  private static final String BODY_STRING = "body stringÿ";
  private static final String CONTENT_TYPE = "content-type";
  private static final int FAILED_RESPONSE_CODE = 404;
  private static final String FAILED_RESPONSE_MESSAGE = "failure";
  private static final int SUCCESSFUL_RESPONSE_CODE = 202;
  private static final String SUCCESSFUL_RESPONSE_MESSAGE = "success";
  private static final HashMap<String, String> HEADERS = new HashMap<>();
  private static final String TEXT_PLAIN_CHARSET_INVALID = "text/plain; charset=invalid";
  private HttpResponse failedResponse;
  private HttpResponse successfulHttpResponse;
  private byte[] defaultCharsetBody;
  private byte[] utf16CharsetBody;

  @Before
  public void setUp() throws Exception {
    defaultCharsetBody = BODY_STRING.getBytes(DEFAULT_CHARSET);
    utf16CharsetBody = BODY_STRING.getBytes(UTF16_CHARSET);
    failedResponse = httpResponse(FAILED_RESPONSE_CODE, FAILED_RESPONSE_MESSAGE,
        HEADERS, defaultCharsetBody);
    successfulHttpResponse = httpResponse(SUCCESSFUL_RESPONSE_CODE, SUCCESSFUL_RESPONSE_MESSAGE,
        HEADERS, defaultCharsetBody);
  }

  @After
  public void tearDown() {
    HEADERS.clear();
  }

  @Test
  public void shouldHaveResponseCodeForFailure() {
    assertThat(failedResponse.getResponseCode(), is(FAILED_RESPONSE_CODE));
  }

  @Test
  public void shouldHaveResponseCodeForSuccess() {
    assertThat(successfulHttpResponse.getResponseCode(), is(SUCCESSFUL_RESPONSE_CODE));
  }

  @Test
  public void shouldHaveResponseMessageForFailure() {
    assertThat(failedResponse.getResponseMessage(), is(FAILED_RESPONSE_MESSAGE));
  }

  @Test
  public void shouldHaveResponseMessageForSuccess() {
    assertThat(successfulHttpResponse.getResponseMessage(), is(SUCCESSFUL_RESPONSE_MESSAGE));
  }

  @Test
  public void shouldGetSuccessBodyStringForSuccess() throws Throwable {
    assertThat(successfulHttpResponse.getBodyString().get(), is(BODY_STRING));
  }

  @Test
  public void shouldGetBodyStringForDefaultCharsetWithContentTypeHeader() throws Throwable {
    final HashMap<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE, "text/plain");
    final HttpResponse httpResponse = httpResponse(SUCCESSFUL_RESPONSE_CODE,
        SUCCESSFUL_RESPONSE_MESSAGE, headers, defaultCharsetBody);

    assertThat(httpResponse.getBodyString().get(), is(BODY_STRING));
  }

  @Test
  public void shouldGetBodyStringForCustomCharsetWithContentTypeHeader() throws Throwable {
    final HashMap<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE, "text/plain; charset=" + UTF16_CHARSET);
    final HttpResponse httpResponse = httpResponse(SUCCESSFUL_RESPONSE_CODE,
        SUCCESSFUL_RESPONSE_MESSAGE, headers, utf16CharsetBody);

    assertThat(httpResponse.getBodyString(), is(success(BODY_STRING)));
  }

  @Test
  public void shouldGetFailureForInvalidCharset() {
    final HashMap<String, String> headers = new HashMap<>();
    headers.put(CONTENT_TYPE, TEXT_PLAIN_CHARSET_INVALID);
    final HttpResponse httpResponse = httpResponse(SUCCESSFUL_RESPONSE_CODE,
        SUCCESSFUL_RESPONSE_MESSAGE, headers, defaultCharsetBody);

    assertThat(httpResponse.getBodyString().getFailure(),
        instanceOf(UnsupportedEncodingException.class));
  }

  @Test
  public void shouldGetAbsentForAbsentHeaderField() {
    assertThat(successfulHttpResponse.getHeaderFieldValue("absentfield").isAbsent(), is(true));
  }

  @Test
  public void shouldGetCaseInsensitiveHeaders() {
    final Map<String, String> headers = new HashMap<>();
    final String headerContent = "headercontent";
    headers.put("header", headerContent);
    final HttpResponse httpResponse = httpResponse(SUCCESSFUL_RESPONSE_CODE,
        SUCCESSFUL_RESPONSE_MESSAGE, headers, defaultCharsetBody);

    assertThat(httpResponse.getHeaderFieldValue("hEaDeR").get(), is(headerContent));
  }

  @Test
  public void shouldVerifyEquals() {
    EqualsVerifier.forClass(HttpResponse.class).verify();
  }

  @Test
  public void shouldCreateStringRepresentation() {
    assertThat(successfulHttpResponse, hasToString(not(isEmptyOrNullString())));
  }
}