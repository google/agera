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

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Map;

/**
 * An object representing a request to the http functions.
 */
public final class HttpRequest {
  @NonNull
  final String method;
  @NonNull
  final String url;
  @NonNull
  final byte[] body;
  @NonNull
  final Map<String, String> header;
  final boolean useCaches;
  final boolean followRedirects;
  final int connectTimeoutMs;
  final int readTimeoutMs;

  HttpRequest(@NonNull final String method, @NonNull final String url,
      @NonNull final byte[] body, @NonNull final Map<String, String> header,
      final boolean useCaches, final boolean followRedirects, final int connectTimeoutMs,
      final int readTimeoutMs) {
    this.method = method;
    this.url = url;
    this.body = body;
    this.header = header;
    this.useCaches = useCaches;
    this.followRedirects = followRedirects;
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HttpRequest)) {
      return false;
    }

    final HttpRequest that = (HttpRequest) o;

    return method.equals(that.method)
        && url.equals(that.url)
        && Arrays.equals(body, that.body)
        && header.equals(that.header);
  }

  @Override
  public int hashCode() {
    int result = method.hashCode();
    result = 31 * result + url.hashCode();
    result = 31 * result + Arrays.hashCode(body);
    result = 31 * result + header.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "HttpRequest{" +
        "method='" + method + '\'' +
        ", url='" + url + '\'' +
        ", body=" + Arrays.toString(body) +
        ", header=" + header +
        '}';
  }
}
