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

import static com.google.android.agera.Preconditions.checkNotNull;
import static com.google.android.agera.Preconditions.checkState;

import android.support.annotation.NonNull;
import com.google.android.agera.net.HttpRequestCompilerStates.HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTCachesConnectionTimeoutReadTimeoutCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTConnectionTimeoutReadTimeoutCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTReadTimeoutCompile;
import java.util.HashMap;
import java.util.Map;

final class HttpRequestCompiler
    implements HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile {
  static final int CONNECT_TIMEOUT_MS = 2500;
  static final int READ_TIMEOUT_MS = 2500;
  @NonNull
  private static final String ERROR_MESSAGE = "Http request compiler cannot be reused";
  @NonNull
  private static final byte[] EMPTY_BODY = new byte[0];

  @NonNull
  private final String method;
  @NonNull
  private final Map<String, String> header;
  @NonNull
  private final String url;

  @NonNull
  private byte[] body;
  private boolean compiled;
  private boolean useCaches;
  private boolean followRedirects;
  private int connectTimeoutMs;
  private int readTimeoutMs;

  HttpRequestCompiler(@NonNull final String method, @NonNull final String url) {
    this.compiled = false;
    this.followRedirects = true;
    this.useCaches = true;
    this.connectTimeoutMs = CONNECT_TIMEOUT_MS;
    this.readTimeoutMs = READ_TIMEOUT_MS;
    this.url = url;
    this.method = checkNotNull(method);
    this.header = new HashMap<>();
    this.body = EMPTY_BODY;
  }

  @NonNull
  @Override
  public HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile body(
      @NonNull final byte[] body) {
    checkState(!compiled, ERROR_MESSAGE);
    this.body = checkNotNull(body);
    return this;
  }

  @NonNull
  @Override
  public HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile headerField(
      @NonNull final String name, @NonNull final String value) {
    checkState(!compiled, ERROR_MESSAGE);
    header.put(name, value);
    return this;
  }

  @NonNull
  @Override
  public HTConnectionTimeoutReadTimeoutCompile noCaches() {
    checkState(!compiled, ERROR_MESSAGE);
    useCaches = false;
    return this;
  }

  @NonNull
  @Override
  public HTReadTimeoutCompile connectTimeoutMs(final int connectTimeoutMs) {
    checkState(!compiled, ERROR_MESSAGE);
    this.connectTimeoutMs = connectTimeoutMs;
    return this;
  }

  @NonNull
  @Override
  public HTCompile readTimeoutMs(final int readTimeoutMs) {
    checkState(!compiled, ERROR_MESSAGE);
    this.readTimeoutMs = readTimeoutMs;
    return this;
  }

  @NonNull
  @Override
  public HTCachesConnectionTimeoutReadTimeoutCompile noRedirects() {
    checkState(!compiled, ERROR_MESSAGE);
    followRedirects = false;
    return this;
  }

  @NonNull
  @Override
  public HttpRequest compile() {
    checkState(!compiled, ERROR_MESSAGE);
    compiled = true;
    return new HttpRequest(method, url, body, header, useCaches, followRedirects, connectTimeoutMs,
        readTimeoutMs);
  }
}
