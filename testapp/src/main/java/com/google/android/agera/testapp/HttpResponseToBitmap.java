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
