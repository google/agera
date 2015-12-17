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
package com.google.android.agera.testapp;

import static android.graphics.BitmapFactory.decodeByteArray;
import static com.google.android.agera.Executors.currentLooperExecutor;
import static com.google.android.agera.Reactions.reactionTo;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.RexConfig.SEND_INTERRUPT;
import static com.google.android.agera.net.HttpFunctions.httpFunction;
import static com.google.android.agera.net.HttpRequests.httpGetRequest;

import com.google.android.agera.Function;
import com.google.android.agera.Reaction;
import com.google.android.agera.Receiver;
import com.google.android.agera.Result;
import com.google.android.agera.net.HttpRequest;
import com.google.android.agera.net.HttpResponse;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

final class UrlToBitmapReactionFactory {
  private static final UrlToHttpRequest URL_TO_HTTP_REQUEST = new UrlToHttpRequest();
  private static final HttpResponseToBitmap HTTP_RESPONSE_TO_BITMAP = new HttpResponseToBitmap();

  @NonNull
  private final Executor networkExecutor;
  @NonNull
  private final Executor calculationExecutor;

  private UrlToBitmapReactionFactory(@NonNull final Executor networkExecutor,
      @NonNull final Executor calculationExecutor) {
    this.networkExecutor = networkExecutor;
    this.calculationExecutor = calculationExecutor;
  }

  public static UrlToBitmapReactionFactory urlToBitmapReactionFactory(
      final ExecutorService networkExecutor, final ExecutorService calculationExecutor) {
    return new UrlToBitmapReactionFactory(networkExecutor,
        calculationExecutor);
  }

  public Reaction<String> createUrlToBitmapReaction(@NonNull final Receiver<Bitmap> receiver) {
    return reactionTo(String.class)
        .goTo(networkExecutor)
        .transform(URL_TO_HTTP_REQUEST)
        .attemptTransform(httpFunction()).orSkip()
        .goTo(calculationExecutor)
        .attemptTransform(HTTP_RESPONSE_TO_BITMAP).orSkip()
        .goTo(currentLooperExecutor())
        .sendTo(receiver)
        .thenEnd()
        .onDeactivation(SEND_INTERRUPT)
        .compile();
  }

  private static final class UrlToHttpRequest implements Function<String, HttpRequest> {
    @NonNull
    @Override
    public HttpRequest apply(@NonNull final String input) {
      return httpGetRequest(input).compile();
    }
  }

  private static final class HttpResponseToBitmap
      implements Function<HttpResponse, Result<Bitmap>> {
    @NonNull
    @Override
    public Result<Bitmap> apply(@NonNull final HttpResponse input) {
      final byte[] body = input.getBody();
      return absentIfNull(decodeByteArray(body, 0, body.length));
    }
  }
}
