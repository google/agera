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

import static com.google.android.agera.Common.NULL_OPERATOR;
import static com.google.android.agera.Common.TRUE_CONDICATE;
import static com.google.android.agera.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;

import android.support.annotation.NonNull;
import com.google.android.agera.FunctionCompilerStates.FItem;
import com.google.android.agera.FunctionCompilerStates.FList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings({"unchecked, rawtypes"})
final class FunctionCompiler implements FList, FItem {
  private static final ThreadLocal<FunctionCompiler> compilers = new ThreadLocal<>();

  @NonNull
  static FunctionCompiler functionCompiler() {
    FunctionCompiler compiler = compilers.get();
    if (compiler == null) {
      compiler = new FunctionCompiler();
    } else {
      // Remove compiler from the ThreadLocal to prevent reuse in the middle of a compilation.
      // recycle(), called by compile(), will return the compiler here. ThreadLocal.set(null) keeps
      // the entry (with a null value) whereas remove() removes the entry; because we expect the
      // return of the compiler, don't use the heavier remove().
      compilers.set(null);
    }
    return compiler;
  }

  private static void recycle(@NonNull final FunctionCompiler compiler) {
    compiler.functions.clear();
    compilers.set(compiler);
  }

  @NonNull
  private final List<Function> functions;

  FunctionCompiler() {
    this.functions = new ArrayList<>();
  }

  private void addFunction(@NonNull final Function function) {
    if (function != NULL_OPERATOR) {
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
      return NULL_OPERATOR;
    }
    final Function[] newFunctions = functions.toArray(new Function[functions.size()]);
    recycle(this);
    return new ChainFunction(newFunctions);
  }

  @NonNull
  @Override
  public FList unpack(@NonNull final Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FItem apply(@NonNull final Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FList morph(@NonNull final Function function) {
    addFunction(function);
    return this;
  }

  @NonNull
  @Override
  public FList filter(@NonNull final Predicate filter) {
    if (filter != TRUE_CONDICATE) {
      addFunction(new FilterFunction(filter));
    }
    return this;
  }

  @NonNull
  @Override
  public FList limit(final int limit) {
    addFunction(new LimitFunction(limit));
    return this;
  }

  @NonNull
  @Override
  public FList sort(@NonNull final Comparator comparator) {
    addFunction(new SortFunction(comparator));
    return this;
  }

  @NonNull
  @Override
  public FList map(@NonNull final Function function) {
    if (function != NULL_OPERATOR) {
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

  @NonNull
  @Override
  public Function thenSort(@NonNull final Comparator comparator) {
    sort(comparator);
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

  private static final class SortFunction<T> implements Function<List<T>, List<T>> {
    @NonNull
    private final Comparator comparator;

    SortFunction(@NonNull final Comparator comparator) {
      this.comparator = checkNotNull(comparator);
    }

    @NonNull
    @Override
    public List<T> apply(@NonNull final List<T> input) {
      final List<T> output = new ArrayList<>(input);
      Collections.sort(output, comparator);
      return output;
    }
  }
}
