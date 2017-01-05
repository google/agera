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
import com.google.android.agera.net.HttpRequestCompilerStates.HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile;
import com.google.android.agera.net.HttpRequestCompilerStates.HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile;

/**
 * Creates instances of {@link HttpRequest}.
 */
public final class HttpRequests {

  /**
   * Starts the creation of a GET {@link HttpRequest}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
  httpGetRequest(@NonNull final String url) {
    return new HttpRequestCompiler("GET", url);
  }

  /**
   * Starts the creation of a PUT {@link HttpRequest}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
  httpPutRequest(@NonNull final String url) {
    return new HttpRequestCompiler("PUT", url);
  }

  /**
   * Starts the creation of a POST {@link HttpRequest}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
  httpPostRequest(@NonNull final String url) {
    return new HttpRequestCompiler("POST", url);
  }

  /**
   * Starts the creation of a DELETE {@link HttpRequest}.
   */
  @NonNull
  @SuppressWarnings("unchecked")
  public static HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
  httpDeleteRequest(@NonNull final String url) {
    return new HttpRequestCompiler("DELETE", url);
  }

  private HttpRequests() {}
}
