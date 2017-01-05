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

import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.net.HttpRequests.httpDeleteRequest;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;
import static com.google.android.agera.net.HttpRequests.httpPostRequest;
import static com.google.android.agera.net.HttpRequests.httpPutRequest;
import static com.google.android.agera.net.test.matchers.HasPrivateConstructor.hasPrivateConstructor;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public final class HttpFunctionsTest {
  private static final String TEST_PROTOCOL = "httptest";
  private static final String TEST_URI = TEST_PROTOCOL + "://path";
  private static final HttpRequest HTTP_GET_REQUEST = httpGetRequest(TEST_URI).compile();
  private static final HttpRequest HTTP_POST_REQUEST = httpPostRequest(TEST_URI).compile();
  private static final HttpRequest HTTP_PUT_REQUEST = httpPutRequest(TEST_URI).compile();
  private static final HttpRequest HTTP_DELETE_REQUEST =
      httpDeleteRequest(TEST_URI).compile();
  private static final byte[] RESPONSE_BODY = new byte[] {2, 3, 4};
  private static final byte[] REQUEST_BODY = new byte[] {1, 2, 3};
  private static final HttpRequest HTTP_GET_REQUEST_WITH_HEADERS = httpGetRequest(TEST_URI)
      .headerField("name", "value").headerField("name2", "value2").compile();
  private static final HttpRequest HTTP_POST_WITH_BODY_REQUEST =
      httpPostRequest(TEST_URI).body(REQUEST_BODY).compile();
  private static final HttpRequest HTTP_PUT_WITH_BODY_REQUEST =
      httpPutRequest(TEST_URI).body(REQUEST_BODY).compile();
  private static final String GET_METHOD = "GET";
  private static final String POST_METHOD = "POST";
  private static final String PUT_METHOD = "PUT";
  private static final String DELETE_METHOD = "DELETE";
  private static final byte[] EMPTY_BODY = new byte[0];

  private static HttpURLConnection mockHttpURLConnection;

  @BeforeClass
  public static void onlyOnce() throws Throwable {
    mockHttpURLConnection = mock(HttpURLConnection.class);
    URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
      @Override
      public URLStreamHandler createURLStreamHandler(final String s) {
        return s.equals(TEST_PROTOCOL) ? new URLStreamHandler() {
          @Override
          protected URLConnection openConnection(final URL url) throws IOException {
            return mockHttpURLConnection;
          }
        } : null;
      }
    });
  }

  @After
  public void tearDown() {
    reset(mockHttpURLConnection);
  }

  @Test
  public void shouldPassOnGetMethod() throws Throwable {
    assertThat(httpFunction()
            .apply(HTTP_GET_REQUEST),
        is(notNullValue()));
    verify(mockHttpURLConnection).setRequestMethod(GET_METHOD);
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldPassOnPostMethod() throws Throwable {
    assertThat(httpFunction()
            .apply(HTTP_POST_REQUEST),
        is(notNullValue()));
    verify(mockHttpURLConnection).setRequestMethod(POST_METHOD);
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldPassOnPutMethod() throws Throwable {
    assertThat(httpFunction()
            .apply(HTTP_PUT_REQUEST),
        is(notNullValue()));
    verify(mockHttpURLConnection).setRequestMethod(PUT_METHOD);
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldPassOnDeleteMethod() throws Throwable {
    assertThat(httpFunction()
            .apply(HTTP_DELETE_REQUEST),
        is(notNullValue()));
    verify(mockHttpURLConnection).setRequestMethod(DELETE_METHOD);
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldGracefullyHandleProtocolExceptionForInvalidMethod() throws Throwable {
    doThrow(ProtocolException.class).when(mockHttpURLConnection).setRequestMethod(anyString());

    assertThat(httpFunction().apply(HTTP_DELETE_REQUEST).getFailure(),
        instanceOf(ProtocolException.class));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldPassOnRequestHeaders() throws Throwable {
    assertThat(httpFunction().apply(HTTP_GET_REQUEST_WITH_HEADERS), is(notNullValue()));

    verify(mockHttpURLConnection).addRequestProperty("name", "value");
    verify(mockHttpURLConnection).addRequestProperty("name2", "value2");
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldPassOnResponseHeadersAsLowerCase() throws Throwable {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);
    when(mockHttpURLConnection.getContentLength()).thenReturn(RESPONSE_BODY.length);
    final Map<String, List<String>> headerFields = new HashMap<>();
    headerFields.put("NAmE", singletonList("value"));
    headerFields.put("naMe2", singletonList("value2"));
    when(mockHttpURLConnection.getHeaderFields()).thenReturn(headerFields);

    final HttpResponse httpResponse = httpFunction().apply(HTTP_GET_REQUEST).get();

    assertThat(httpResponse.header.size(), is(2));
    assertThat(httpResponse.header, hasEntry("name", "value"));
    assertThat(httpResponse.header, hasEntry("name2", "value2"));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldNotPassOnNullResponseHeader() throws Throwable {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);
    when(mockHttpURLConnection.getContentLength()).thenReturn(RESPONSE_BODY.length);
    final Map<String, List<String>> headerFields = new HashMap<>();
    headerFields.put(null, singletonList("value"));
    headerFields.put("naMe2", singletonList("value2"));
    when(mockHttpURLConnection.getHeaderFields()).thenReturn(headerFields);

    final HttpResponse httpResponse = httpFunction().apply(HTTP_GET_REQUEST).get();

    assertThat(httpResponse.header.size(), is(1));
    assertThat(httpResponse.header, hasEntry("name2", "value2"));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldGetOutputStreamForPutWithBody() throws Throwable {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getOutputStream()).thenReturn(outputStream);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);

    assertThat(httpFunction().apply(HTTP_PUT_WITH_BODY_REQUEST), is(notNullValue()));
    verify(mockHttpURLConnection).setDoInput(true);
    verify(mockHttpURLConnection).disconnect();
    assertThat(outputStream.toByteArray(), is(REQUEST_BODY));
  }

  @Test
  public void shouldGetOutputStreamForPostWithBody() throws Throwable {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getOutputStream()).thenReturn(outputStream);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);

    assertThat(httpFunction().apply(HTTP_POST_WITH_BODY_REQUEST), is(notNullValue()));
    verify(mockHttpURLConnection).setDoInput(true);
    verify(mockHttpURLConnection).disconnect();
    assertThat(outputStream.toByteArray(), is(REQUEST_BODY));
  }

  @Test
  public void shouldGetByteArrayFromGetResponse() throws Throwable {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);
    when(mockHttpURLConnection.getContentLength()).thenReturn(RESPONSE_BODY.length);

    assertThat(httpFunction().apply(HTTP_GET_REQUEST).get().getBody(), is(RESPONSE_BODY));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldGetByteArrayFromGetResponseOfUnknownLength() throws Throwable {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);
    when(mockHttpURLConnection.getContentLength()).thenReturn(-1);

    assertThat(httpFunction().apply(HTTP_GET_REQUEST).get().getBody(), is(RESPONSE_BODY));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldGetEmptyBodyFromGetResponseOfZeroLength() throws Throwable {
    final InputStream inputStream = mock(InputStream.class);
    when(mockHttpURLConnection.getInputStream()).thenReturn(inputStream);
    when(mockHttpURLConnection.getContentLength()).thenReturn(0);

    assertThat(httpFunction().apply(HTTP_GET_REQUEST).get().getBody(), is(EMPTY_BODY));
    verify(mockHttpURLConnection).disconnect();
    verifyZeroInteractions(inputStream);
  }

  @Test
  public void shouldReturnErrorStreamForFailingInputStream() throws Throwable {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(RESPONSE_BODY);
    when(mockHttpURLConnection.getContentLength()).thenReturn(-1);
    //noinspection unchecked
    when(mockHttpURLConnection.getInputStream()).thenThrow(IOException.class);
    when(mockHttpURLConnection.getErrorStream()).thenReturn(inputStream);

    assertThat(httpFunction().apply(HTTP_GET_REQUEST).get().getBody(), is(RESPONSE_BODY));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldReturnResponseCodeAndMessage() throws Throwable {
    when(mockHttpURLConnection.getResponseCode()).thenReturn(200);
    when(mockHttpURLConnection.getResponseMessage()).thenReturn("message");

    final HttpResponse httpResponse = httpFunction().apply(HTTP_GET_REQUEST).get();

    assertThat(httpResponse.getResponseCode(), is(200));
    assertThat(httpResponse.getResponseMessage(), is("message"));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldReturnEmptyStringForNullResponseMessage() throws Throwable {
    when(mockHttpURLConnection.getResponseMessage()).thenReturn(null);

    final HttpResponse httpResponse = httpFunction().apply(HTTP_GET_REQUEST).get();

    assertThat(httpResponse.getResponseMessage(), is(""));
    verify(mockHttpURLConnection).disconnect();
  }

  @Test
  public void shouldHavePrivateConstructor() {
    assertThat(HttpFunctions.class, hasPrivateConstructor());
  }
}
