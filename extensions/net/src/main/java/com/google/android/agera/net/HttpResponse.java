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
import static com.google.android.agera.Result.absent;
import static com.google.android.agera.Result.absentIfNull;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static java.util.Locale.US;

import android.support.annotation.NonNull;
import com.google.android.agera.Function;
import com.google.android.agera.Result;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * An object representing a response from the http functions.
 */
public final class HttpResponse {
  @NonNull
  private static final ExtractCharsetFromContentType
      CHARSET_FROM_CONTENT_TYPE = new ExtractCharsetFromContentType();
  @NonNull
  private static final String CONTENT_TYPE = "Content-Type";
  @NonNull
  private static final String DEFAULT_CHARSET = "UTF-8";

  private final int responseCode;
  @NonNull
  private final String responseMessage;
  @NonNull
  private final byte[] body;
  @NonNull
  final Map<String, String> header;

  private HttpResponse(final int responseCode, @NonNull final String responseMessage,
      @NonNull final Map<String, String> header, @NonNull final byte[] body) {
    this.responseCode = responseCode;
    this.responseMessage = checkNotNull(responseMessage);
    this.header = checkNotNull(header);
    this.body = checkNotNull(body);
  }

  /**
   * Returns a new {@code HttpResponse}. Clients should only use this method for testing purposes.
   * It is normally only called by the http {@link Function}. When created it is assume that header
   * field keys in {@code header} is first changed to lower case {@link Locale#US}.
   */
  @NonNull
  public static HttpResponse httpResponse(final int responseCode,
      @NonNull final String responseMessage,
      @NonNull final Map<String, String> header, @NonNull final byte[] body) {
    return new HttpResponse(responseCode, responseMessage, header, body);
  }

  /**
   * Returns the response body.
   */
  @NonNull
  public byte[] getBody() {
    return body;
  }

  /**
   * Returns a {@link Result} of the body as a {@link String} based on the content type and
   * character encoding in the response. If the content could not be decoded into a string this
   * will return a {@link Result#failure()}.
   */
  @NonNull
  public Result<String> getBodyString() {
    try {
      return success(new String(body, getHeaderFieldValue(CONTENT_TYPE)
          .ifSucceededAttemptMap(CHARSET_FROM_CONTENT_TYPE)
          .orElse(DEFAULT_CHARSET)));
    } catch (final UnsupportedEncodingException e) {
      return failure(e);
    }
  }

  /**
   * Returns the response code.
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Returns the response message.
   */
  @NonNull
  public String getResponseMessage() {
    return responseMessage;
  }

  /**
   * Returns a {@link Result} of a header field value for the given {@code field}. If the response
   * doesn't contain the field a {@link Result#absent()} is returned.
   */
  @NonNull
  public Result<String> getHeaderFieldValue(@NonNull final String field) {
    return absentIfNull(header.get(field.toLowerCase(US)));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HttpResponse)) {
      return false;
    }

    final HttpResponse that = (HttpResponse) o;

    return responseCode == that.responseCode
        && responseMessage.equals(that.responseMessage)
        && Arrays.equals(body, that.body)
        && header.equals(that.header);
  }

  @Override
  public int hashCode() {
    int result = responseCode;
    result = 31 * result + responseMessage.hashCode();
    result = 31 * result + Arrays.hashCode(body);
    result = 31 * result + header.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "HttpResponse{" +
        "responseCode=" + responseCode +
        ", responseMessage='" + responseMessage + '\'' +
        ", body=" + Arrays.toString(body) +
        ", header=" + header +
        '}';
  }

  static final class ExtractCharsetFromContentType implements Function<String, Result<String>> {
    private static final String CHARSET = "charset=";

    @NonNull
    @Override
    public Result<String> apply(@NonNull final String contentType) {
      final String[] parameters = contentType.split(";");
      for (final String parameter : parameters) {
        final String trimmedLowerCaseParameter = parameter.trim().toLowerCase(US);
        if (trimmedLowerCaseParameter.startsWith(CHARSET)) {
          return present(trimmedLowerCaseParameter.substring(CHARSET.length()));
        }
      }
      return absent();
    }
  }
}
