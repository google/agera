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

import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.net.HttpResponse.httpResponse;
import static java.util.Locale.US;

import android.support.annotation.NonNull;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class UrlConnectionHttpFunction implements Function<HttpRequest, Result<HttpResponse>> {
  private static final int CONTENT_BUFFER_SIZE = 1024;
  private static final byte[] EMPTY_BODY = new byte[0];

  @Override
  @NonNull
  public Result<HttpResponse> apply(@NonNull final HttpRequest request) {
    try {
      final HttpURLConnection connection =
          (HttpURLConnection) new URL(request.url).openConnection();
      try {
        return success(getHttpResponseResult(request, connection));
      } finally {
        connection.disconnect();
      }
    } catch (final IOException exception) {
      return failure(exception);
    }
  }

  @NonNull
  private HttpResponse getHttpResponseResult(final @NonNull HttpRequest request,
      @NonNull final HttpURLConnection connection) throws IOException {
    connection.setConnectTimeout(request.connectTimeoutMs);
    connection.setReadTimeout(request.readTimeoutMs);
    connection.setInstanceFollowRedirects(request.followRedirects);
    connection.setUseCaches(request.useCaches);
    connection.setDoInput(true);
    connection.setRequestMethod(request.method);
    for (final Entry<String, String> headerField : request.header.entrySet()) {
      connection.addRequestProperty(headerField.getKey(), headerField.getValue());
    }
    final byte[] body = request.body;
    if (body.length > 0) {
      connection.setDoOutput(true);
      final OutputStream out = connection.getOutputStream();
      try {
        out.write(body);
      } finally {
        out.close();
      }
    }
    final String responseMessage = connection.getResponseMessage();
    return httpResponse(connection.getResponseCode(),
        responseMessage != null ? responseMessage : "",
        getHeader(connection), getByteArray(connection));
  }

  @NonNull
  private static Map<String, String> getHeader(@NonNull final HttpURLConnection connection) {
    final Map<String, String> headers = new HashMap<>();
    for (final Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
      final String key = header.getKey();
      if (key != null) {
        headers.put(key.toLowerCase(US), header.getValue().get(0));
      }
    }
    return headers;
  }

  @NonNull
  private byte[] getByteArray(@NonNull final HttpURLConnection connection) throws IOException {
    final int contentLength = connection.getContentLength();
    if (contentLength == 0) {
      return EMPTY_BODY;
    }
    final InputStream inputStream = getInputStream(connection);
    try {
      final int capacity = contentLength < 0 ? CONTENT_BUFFER_SIZE : contentLength;
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] buffer = new byte[capacity];
      while (true) {
        int r = inputStream.read(buffer);
        if (r == -1) {
          break;
        }
        out.write(buffer, 0, r);
      }
      return out.toByteArray();
    } finally {
      inputStream.close();
    }
  }

  @NonNull
  private static InputStream getInputStream(@NonNull final HttpURLConnection connection) {
    try {
      return connection.getInputStream();
    } catch (final IOException e) {
      return connection.getErrorStream();
    }
  }
}
