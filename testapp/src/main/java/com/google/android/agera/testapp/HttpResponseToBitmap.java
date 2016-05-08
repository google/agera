/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.android.agera.Result.absentIfNull;

import com.google.android.agera.Function;
import com.google.android.agera.Result;
import com.google.android.agera.net.HttpResponse;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

final class HttpResponseToBitmap implements Function<HttpResponse, Result<Bitmap>> {
  @NonNull
  @Override
  public Result<Bitmap> apply(@NonNull final HttpResponse input) {
    final byte[] body = input.getBody();
    return absentIfNull(decodeByteArray(body, 0, body.length));
  }
}
