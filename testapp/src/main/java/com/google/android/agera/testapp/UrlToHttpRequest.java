package com.google.android.agera.testapp;

import static com.google.android.agera.net.HttpRequests.httpGetRequest;

import com.google.android.agera.Function;
import com.google.android.agera.net.HttpRequest;

import android.support.annotation.NonNull;

final class UrlToHttpRequest implements Function<String, HttpRequest> {
  @NonNull
  @Override
  public HttpRequest apply(@NonNull final String input) {
    return httpGetRequest(input).compile();
  }
}
