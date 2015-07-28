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

/**
 * Container of the compiler state interfaces supporting the creation of a {@link HttpRequest}.
 */
public interface HttpRequestCompilerStates {

  /**
   * Compiler state to specify what body to use for a post/put request.
   */
  interface HTBody {

    /**
     * Adds a body to the {@link HttpRequest}.
     */
    @NonNull
    HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile body(
        @NonNull byte[] body);
  }

  /**
   * Compiler state to compile the {@link HttpRequest}.
   */
  interface HTCompile {

    /**
     * Compiles a {@link HttpRequest} that containing the previously specified data.
     */
    @NonNull
    HttpRequest compile();
  }

  /**
   * Compiler state allowing to specify header fields or compile.
   */
  interface HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
      extends HTCachesConnectionTimeoutReadTimeoutCompile {

    /**
     * Adds a header field to the {@link HttpRequest}.
     */
    @NonNull
    HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile headerField(
        @NonNull String name, @NonNull String value);


    /**
     * Turns off follow redirects.
     */
    @NonNull
    HTCachesConnectionTimeoutReadTimeoutCompile noRedirects();
  }

  /**
   * Compiler state allowing to specify read timeout or compile.
   */
  interface HTReadTimeoutCompile extends HTCompile {

    /**
     * Sets a read timeout in milliseconds.
     */
    @NonNull
    HTCompile readTimeoutMs(int readTimeoutMs);
  }

  /**
   * Compiler state allowing to specify connection timeout, read timeout or compile.
   */
  interface HTConnectionTimeoutReadTimeoutCompile extends HTReadTimeoutCompile {

    /**
     * Sets a connection timeout in milliseconds.
     */
    @NonNull
    HTReadTimeoutCompile connectTimeoutMs(int connectionTimeoutMs);
  }

  /**
   * Compiler state allowing to specify not to use caches, connection timeouts, read timeout or
   * compile.
   */
  interface HTCachesConnectionTimeoutReadTimeoutCompile
      extends HTConnectionTimeoutReadTimeoutCompile {

    /**
     * Turns off http caches.
     */
    @NonNull
    HTConnectionTimeoutReadTimeoutCompile noCaches();
  }

  /**
   * Compiler state allowing to specify body, header fields or compile.
   */
  interface HTBodyHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile
      extends HTBody, HTHeaderFieldRedirectsCachesConnectionTimeoutReadTimeoutCompile {}
}
