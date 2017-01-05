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

import static com.google.android.agera.Preconditions.checkNotNull;

import android.os.Looper;
import android.support.annotation.NonNull;
import com.google.android.agera.RepositoryCompilerStates.REventSource;

/**
 * Utility methods for obtaining {@link Repository} instances.
 *
 * <p>Any {@link Repository} created by this class have to be created from a {@link Looper} thread
 * or they will throw an {@link IllegalStateException}
 */
public final class Repositories {

  /**
   * Returns a static {@link Repository} of the given {@code object}.
   */
  @NonNull
  public static <T> Repository<T> repository(@NonNull final T object) {
    return new SimpleRepository<>(object);
  }

  /**
   * Starts the declaration of a compiled repository. See more at {@link RepositoryCompilerStates}.
   */
  @NonNull
  public static <T> REventSource<T, T> repositoryWithInitialValue(@NonNull final T initialValue) {
    return RepositoryCompiler.repositoryWithInitialValue(initialValue);
  }

  /**
   * Returns a {@link MutableRepository} with the given {@code object} as the initial data.
   */
  @NonNull
  public static <T> MutableRepository<T> mutableRepository(@NonNull final T object) {
    return new SimpleRepository<>(object);
  }

  private static final class SimpleRepository<T> extends BaseObservable
      implements MutableRepository<T> {
    @NonNull
    private T reference;

    SimpleRepository(@NonNull final T reference) {
      this.reference = checkNotNull(reference);
    }

    @NonNull
    @Override
    public synchronized T get() {
      return reference;
    }

    @Override
    public void accept(@NonNull final T reference) {
      synchronized (this) {
        if (reference.equals(this.reference)) {
          // Keep the old reference to have a slight performance edge if GC is generational.
          return;
        }
        this.reference = reference;
      }
      dispatchUpdate();
    }
  }

  private Repositories() {}
}
