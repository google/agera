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

import static com.google.android.agera.Common.IDENTITY_FUNCTION;
import static com.google.android.agera.Common.TRUE_CONDICATE;
import static com.google.android.agera.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
final class FunctionCompiler implements FunctionCompilerStates.FList, FunctionCompilerStates.FItem {
  @NonNull
  private final List<Function> functions;

  FunctionCompiler() {
    this.functions = new ArrayList<>();
  }

  private void addFunction(@NonNull final Function function) {
    if (function != IDENTITY_FUNCTION) {
      functions.add(function);
    }
  }

  @NonNull
  @Override
  public Function thenApply(@NonNull final Function function) {
    addFunction(function);
    return createFunction();
  }

  @NonNull
  private Function createFunction() {
    if (functions.isEmpty()) {
      return IDENTITY_FUNCTION;
    }
    return new ChainFunction(functions.toArray(new Function[functions.size()]));
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FList unpack(@NonNull final Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FItem apply(@NonNull final Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FList morph(@NonNull Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FList filter(@NonNull final Predicate filter) {
    if (filter != TRUE_CONDICATE) {
      addFunction(new FilterFunction(filter));
    }
    return this;
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FList limit(final int limit) {
    addFunction(new LimitFunction(limit));
    return this;
  }

  @NonNull
  @Override
  public FunctionCompilerStates.FList map(@NonNull final Function function) {
    if (function != IDENTITY_FUNCTION) {
      addFunction(new MapFunction(function));
    }
    return this;
  }

  @NonNull
  @Override
  public Function thenMap(@NonNull final Function function) {
    map(function);
    return createFunction();
  }

  @NonNull
  @Override
  public Function thenFilter(@NonNull final Predicate filter) {
    filter(filter);
    return createFunction();
  }

  @NonNull
  @Override
  public Function thenLimit(final int limit) {
    limit(limit);
    return createFunction();
  }

  private static final class LimitFunction<T> implements Function<List<T>, List<T>> {
    private final int limit;

    LimitFunction(final int limit) {
      this.limit = limit;
    }

    @NonNull
    @Override
    public List<T> apply(@NonNull final List<T> input) {
      if (input.size() < limit) {
        return input;
      }
      if (limit <= 0) {
        return emptyList();
      }
      return new ArrayList<>(input.subList(0, limit));
    }
  }

  private static final class MapFunction<F, T> implements Function<List<F>, List<T>> {
    @NonNull
    private final Function<F, T> function;

    MapFunction(@NonNull final Function<F, T> function) {
      this.function = checkNotNull(function);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public List<T> apply(@NonNull final List<F> input) {
      if (input.isEmpty()) {
        return emptyList();
      }
      final List<T> result = new ArrayList(input.size());
      for (final F item : input) {
        result.add(function.apply(item));
      }
      return result;
    }
  }

  private static final class ChainFunction implements Function {
    @NonNull
    private final Function[] functions;

    ChainFunction(@NonNull final Function[] functions) {
      this.functions = checkNotNull(functions);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(@NonNull final Object input) {
      Object item = input;
      for (final Function function : functions) {
        item = function.apply(item);
      }
      return item;
    }
  }

  private static final class FilterFunction<T> implements Function<List<T>, List<T>> {
    @NonNull
    private final Predicate filter;

    FilterFunction(@NonNull final Predicate filter) {
      this.filter = checkNotNull(filter);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public List<T> apply(@NonNull final List<T> input) {
      if (input.isEmpty()) {
        return emptyList();
      }
      final List<T> result = new ArrayList(input.size());
      for (final T item : input) {
        if (filter.apply(item)) {
          result.add(item);
        }
      }
      return result;
    }
  }
}
