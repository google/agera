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
import com.google.android.agera.Function;
import com.google.android.agera.Result;

/**
 * Utility methods for obtaining http requesting {@link Function} instances.
 *
 * <p>A failing call to these functions will result in a {@link Result#failure()}. If the http
 * request returns a response code, it will be contained in a {@link HttpResponse} in a
 * {@link Result#success(Object)}. This also applies to failing response codes.
 * {@link HttpResponse} can be used to check for failing responses.
 */
public final class HttpFunctions {
  private static final UrlConnectionHttpFunction HTTP_FUNCTION = new UrlConnectionHttpFunction();

  /**
   * Creates a default http {@link Function} that returns a {@link Result} with a
   * {@link HttpResponse} from a {@link HttpRequest}.
   */
  @NonNull
  public static Function<HttpRequest, Result<HttpResponse>> httpFunction() {
    return HTTP_FUNCTION;
  }

  private HttpFunctions() {}
}
