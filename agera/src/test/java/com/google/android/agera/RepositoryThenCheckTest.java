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
package com.google.android.agera;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.agera.test.SingleSlotDelayedExecutor;
import com.google.android.agera.test.mocks.MockUpdatable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.SocketException;

import static com.google.android.agera.Functions.staticFunction;
import static com.google.android.agera.Predicates.equalTo;
import static com.google.android.agera.Repositories.mutableRepository;
import static com.google.android.agera.Repositories.repositoryWithInitialValue;
import static com.google.android.agera.Result.absent;
import static com.google.android.agera.Result.failure;
import static com.google.android.agera.Result.present;
import static com.google.android.agera.Result.success;
import static com.google.android.agera.Suppliers.staticSupplier;
import static com.google.android.agera.test.matchers.SupplierGives.has;
import static com.google.android.agera.test.matchers.UpdatableUpdated.wasNotUpdated;
import static com.google.android.agera.test.mocks.MockUpdatable.mockUpdatable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.annotation.Config.NONE;

@Config(manifest = NONE)
@RunWith(RobolectricTestRunner.class)
public final class RepositoryThenCheckTest {
  private static final char LOADING = 'L';
  private static final int REQUEST = 42;
  private static final Supplier<Integer> REQUEST_SUPPLIER = staticSupplier(REQUEST);
  private static final String CACHE_KEY = "cacheKey";
  private static final char IN_MEMORY_CACHED_ITEM = 'i';
  private static final Result<Character> PRESENT_IN_MEMORY_RESULT = present(IN_MEMORY_CACHED_ITEM);
  private static final Result<Character> ABSENT_IN_MEMORY_RESULT = absent();
  private static final char DISK_CACHED_FRESH_ITEM = 'f';
  private static final Result<Character> SUCCESSFUL_DISK_CACHED_FRESH_RESULT =
      success(DISK_CACHED_FRESH_ITEM);
  private static final char DISK_CACHED_STALE_ITEM = 's';
  private static final Result<Character> SUCCESSFUL_DISK_CACHED_STALE_RESULT =
      success(DISK_CACHED_STALE_ITEM);
  private static final Result<Character> FAILED_DISK_CACHE_RESULT = failure(new IOException());
  private static final char DISK_FAILURE = 'D';
  private static final Uri NETWORK_REQUEST_URI = Uri.parse("http://then.check");
  private static final char NETWORK_RESPONSE = 'r';
  private static final Result<Character> SUCCESSFUL_RESPONSE_RESULT = success(NETWORK_RESPONSE);
  private static final Result<Character> FAILED_RESPONSE_RESULT = failure(new SocketException());
  private static final Result<Character> INVALID_RESPONSE_RESULT = failure(new RuntimeException());
  private static final char NETWORK_FAILURE = 'N';

  private static final Predicate<Result<Character>> WAS_PRESENT =
      new Predicate<Result<Character>>() {
        @Override
        public boolean apply(@NonNull final Result<Character> value) {
          return value.isPresent();
        }
      };
  private static final Function<Result<Character>, Character> THE_SUCCESSFUL_DISK_CACHE_RESULT =
      new Function<Result<Character>, Character>() {
        @NonNull
        @Override
        public Character apply(@NonNull final Result<Character> input) {
          return input.orElse(DISK_FAILURE);
        }
      };
  private static final Predicate<Character> WAS_FRESH = equalTo(DISK_CACHED_FRESH_ITEM);

  private Repository<Character> repository;
  private Repository<Result<Character>> resultRepository;
  private MockUpdatable updatable;
  private SingleSlotDelayedExecutor diskIoExecutor;
  private SingleSlotDelayedExecutor networkIoExecutor;
  @Mock
  private Function<Integer, String> mockRequestToCacheKeyFunction;
  @Mock
  private Function<String, Result<Character>> mockInMemoryCache;
  @Mock
  private Function<String, Result<Character>> mockDiskCache;
  @Mock
  private Function<Integer, Uri> mockRequestToNetworkRequestFunction;
  @Mock
  private Function<Uri, Result<Character>> mockNetworkRequestFunction;
  @Mock
  private Predicate<Object> mockNetworkResponseValid;

  @Before
  public void setUp() {
    initMocks(this);
    when(mockRequestToCacheKeyFunction.apply(REQUEST)).thenReturn(CACHE_KEY);
    when(mockRequestToNetworkRequestFunction.apply(REQUEST)).thenReturn(NETWORK_REQUEST_URI);

    updatable = mockUpdatable();
    diskIoExecutor = new SingleSlotDelayedExecutor();
    networkIoExecutor = new SingleSlotDelayedExecutor();
    final MutableRepository<Integer> currentRequestVariable = mutableRepository(0);

    // This repository can end with DISK_FAILURE and can skip updating the result if the network
    // response is invalid. Tested combinations:
    // - thenAttempt*().or*().thenCheck()
    // - thenCheck().orContinue()
    // - thenCheck().orSkip()
    repository = repositoryWithInitialValue(LOADING)
        .observe()
        .onUpdatesPerLoop()
        .getFrom(REQUEST_SUPPLIER)
        .sendTo(currentRequestVariable)
        .transform(mockRequestToCacheKeyFunction)
        .thenAttemptTransform(mockInMemoryCache).orContinue()
        .goTo(diskIoExecutor)
        .getFrom(currentRequestVariable)
        .transform(mockRequestToCacheKeyFunction)
        .thenAttemptTransform(mockDiskCache).orEnd(staticFunction(DISK_FAILURE))
        .thenCheck(WAS_FRESH).orContinue()
        .goTo(networkIoExecutor)
        .getFrom(currentRequestVariable)
        .transform(mockRequestToNetworkRequestFunction)
        .thenAttemptTransform(mockNetworkRequestFunction).orEnd(staticFunction(NETWORK_FAILURE))
        .thenCheck(mockNetworkResponseValid).orSkip()
        .compile();

    // This repository will not end with disk failure, but can expose INVALID_RESPONSE_RESULT if
    // the network response is invalid. Tested combinations:
    // - then*().thenCheck()
    // - thenCheck().orContinue()
    // - thenCheck().orEnd()
    resultRepository = repositoryWithInitialValue(Result.<Character>absent())
        .observe()
        .onUpdatesPerLoop()
        .getFrom(REQUEST_SUPPLIER)
        .sendTo(currentRequestVariable)
        .transform(mockRequestToCacheKeyFunction)
        .thenTransform(mockInMemoryCache)
        .thenCheck(WAS_PRESENT).orContinue()
        .goTo(diskIoExecutor)
        .getFrom(currentRequestVariable)
        .transform(mockRequestToCacheKeyFunction)
        .thenTransform(mockDiskCache)
        .thenCheck(THE_SUCCESSFUL_DISK_CACHE_RESULT, WAS_FRESH).orContinue()
        .goTo(networkIoExecutor)
        .getFrom(currentRequestVariable)
        .transform(mockRequestToNetworkRequestFunction)
        .thenTransform(mockNetworkRequestFunction)
        .thenCheck(mockNetworkResponseValid).orEnd(staticFunction(INVALID_RESPONSE_RESULT))
        .compile();
  }

  @After
  public void tearDown() {
    updatable.removeFromObservables();
  }

  @Test
  public void shouldUseInMemoryCachedItemIfPresent() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(PRESENT_IN_MEMORY_RESULT);

    updatable.addToObservable(repository);

    assertThat(repository, has(IN_MEMORY_CACHED_ITEM));
  }

  @Test
  public void shouldUseInMemoryCachedResultIfPresent() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(PRESENT_IN_MEMORY_RESULT);

    updatable.addToObservable(resultRepository);

    assertThat(resultRepository, has(PRESENT_IN_MEMORY_RESULT));
  }

  @Test
  public void shouldUseDiskCachedItemIfFresh() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_FRESH_RESULT);

    updatable.addToObservable(repository);
    diskIoExecutor.resumeOrThrow();

    assertThat(repository, has(DISK_CACHED_FRESH_ITEM));
  }

  @Test
  public void shouldUseDiskCachedResultIfFresh() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_FRESH_RESULT);

    updatable.addToObservable(resultRepository);
    diskIoExecutor.resumeOrThrow();

    assertThat(resultRepository, has(SUCCESSFUL_DISK_CACHED_FRESH_RESULT));
  }

  @Test
  public void shouldReportDiskFailureItemIfDiskCacheFails() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(FAILED_DISK_CACHE_RESULT);

    updatable.addToObservable(repository);
    diskIoExecutor.resumeOrThrow();

    assertThat(repository, has(DISK_FAILURE));
  }

  @Test
  public void shouldUseNetworkResponseItemIfCacheIsStale() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(SUCCESSFUL_RESPONSE_RESULT);
    when(mockNetworkResponseValid.apply(NETWORK_RESPONSE)).thenReturn(true);

    updatable.addToObservable(repository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(repository, has(NETWORK_RESPONSE));
  }

  @Test
  public void shouldUseNetworkResponseResultIfCacheIsStale() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(SUCCESSFUL_RESPONSE_RESULT);
    when(mockNetworkResponseValid.apply(SUCCESSFUL_RESPONSE_RESULT)).thenReturn(true);

    updatable.addToObservable(resultRepository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(resultRepository, has(SUCCESSFUL_RESPONSE_RESULT));
  }

  @Test
  public void shouldReportNetworkFailureItemIfNetworkFails() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(FAILED_RESPONSE_RESULT);

    updatable.addToObservable(repository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(repository, has(NETWORK_FAILURE));
  }

  @Test
  public void shouldReportNetworkFailureResultIfNetworkFails() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(FAILED_RESPONSE_RESULT);
    when(mockNetworkResponseValid.apply(FAILED_RESPONSE_RESULT)).thenReturn(true);

    updatable.addToObservable(resultRepository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(resultRepository, has(FAILED_RESPONSE_RESULT));
  }

  @Test
  public void shouldSkipUpdateIfNetworkResponseIsInvalid() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(SUCCESSFUL_RESPONSE_RESULT);
    when(mockNetworkResponseValid.apply(NETWORK_RESPONSE)).thenReturn(false);

    updatable.addToObservable(repository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(updatable, wasNotUpdated());
    assertThat(repository, has(LOADING));
  }

  @Test
  public void shouldReportInvalidResponseResultIfNetworkResponseIsInvalid() {
    when(mockInMemoryCache.apply(CACHE_KEY)).thenReturn(ABSENT_IN_MEMORY_RESULT);
    when(mockDiskCache.apply(CACHE_KEY)).thenReturn(SUCCESSFUL_DISK_CACHED_STALE_RESULT);
    when(mockNetworkRequestFunction.apply(NETWORK_REQUEST_URI))
        .thenReturn(SUCCESSFUL_RESPONSE_RESULT);
    when(mockNetworkResponseValid.apply(SUCCESSFUL_RESPONSE_RESULT)).thenReturn(false);

    updatable.addToObservable(resultRepository);
    diskIoExecutor.resumeOrThrow();
    networkIoExecutor.resumeOrThrow();

    assertThat(resultRepository, has(INVALID_RESPONSE_RESULT));
  }
}
