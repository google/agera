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

import com.google.android.agera.FunctionCompilerStates.FItem;
import com.google.android.agera.FunctionCompilerStates.FList;

import android.support.annotation.NonNull;

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
    compiler.directives.clear();
    compilers.set(compiler);
  }

  private static final Integer APPLY = 0;
  private static final Integer FILTER = 1;
  private static final Integer LIMIT = 2;
  private static final Integer SORT = 3;
  private static final Integer MAP = 4;

  @NonNull
  private final List<Object> directives;

  FunctionCompiler() {
    this.directives = new ArrayList<>();
  }

  private void addFunction(@NonNull final Function function) {
    if (function != IDENTITY_FUNCTION) {
      directives.add(APPLY);
      directives.add(function);
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
    if (directives.isEmpty()) {
      return IDENTITY_FUNCTION;
    }
    final Object[] newDirectives = directives.toArray(new Object[directives.size()]);
    recycle(this);
    return new CompiledFunction(newDirectives);
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
      directives.add(FILTER);
      directives.add(checkNotNull(filter));
    }
    return this;
  }

  @NonNull
  @Override
  public FList limit(final int limit) {
    directives.add(LIMIT);
    directives.add(limit);
    return this;
  }

  @NonNull
  @Override
  public FList sort(@NonNull final Comparator comparator) {
    directives.add(SORT);
    directives.add(checkNotNull(comparator));
    return this;
  }

  @NonNull
  @Override
  public FList sort() {
    directives.add(SORT);
    directives.add(null);
    return this;
  }

  @NonNull
  @Override
  public FList map(@NonNull final Function function) {
    if (function != IDENTITY_FUNCTION) {
      directives.add(MAP);
      directives.add(checkNotNull(function));
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

  @NonNull
  @Override
  public Function thenSort() {
    sort();
    return createFunction();
  }

  private static final class CompiledFunction implements Function {
    @NonNull
    private final Object[] directives;

    CompiledFunction(@NonNull final Object[] directives) {
      this.directives = checkNotNull(directives);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public Object apply(@NonNull final Object input) {
      Object item = input;
      int i = 0;
      final int length = directives.length;
      while (i < length) {
        final Object type = directives[i++];
        final Object command = directives[i++];
        if (type == APPLY) {
          item = ((Function) command).apply(item);
        } else if (type == MAP) {
          final List list = (List) item;
          final int size = list.size();
          if (size <= 0) {
            item = emptyList();
          } else if (list instanceof FunctionCompilerList) {
            final Function function = (Function) command;
            for (int j = 0; j < size; j++) {
              list.set(j, function.apply(list.get(j)));
            }
          } else {
            final Function function = (Function) command;
            final List result = new FunctionCompilerList(size);
            for (int j = 0; j < size; j++) {
              result.add(function.apply(list.get(j)));
            }
            item = result;
          }
        } else if (type == FILTER) {
          final List list = (List) item;
          int size = list.size();
          if (size <= 0) {
            item = emptyList();
          } else if (list instanceof FunctionCompilerList) {
            final Predicate predicate = (Predicate) command;
            int from = 0;
            int to = 0;

            for (; from < size; from++) {
              final Object listItem = list.get(from);
              if (predicate.apply(listItem)) {
                if (from > to) {
                  list.set(to, listItem);
                }
                to++;
              }
            }
            ((FunctionCompilerList) list).removeRange(to, size);
          } else {
            final Predicate predicate = (Predicate) command;
            final List result = new FunctionCompilerList(size);
            for (int j = 0; j < size; j++) {
              final Object listItem = list.get(j);
              if (predicate.apply(listItem)) {
                result.add(listItem);
              }
            }
            item = result;
          }
        } else if (type == LIMIT) {
          final List list = (List) item;
          final int limit = (int) command;
          if (limit <= 0) {
            item = emptyList();
          } else {
            final int size = list.size();
            if (size < limit) {
              item = input;
            } else if (list instanceof FunctionCompilerList) {
              ((FunctionCompilerList) list).removeRange(limit, size);
            } else {
              final List newList = new FunctionCompilerList(limit);
              for (int copied = 0; copied < limit; copied++) {
                newList.add(list.get(copied));
              }
              item = newList;
            }
          }
        } else if (type == SORT) {
          List list = (List) item;
          final List output = list instanceof FunctionCompilerList
              ? ((List) item) : new FunctionCompilerList(list);
          if (command != null) {
            Collections.sort(output, (Comparator) command);
          } else {
            Collections.sort(output);
          }
          item = output;
        }
      }
      return item;
    }

    private static final class FunctionCompilerList<E> extends ArrayList<E> {
      FunctionCompilerList(@NonNull final List<E> list) {
        super(list);
      }

      FunctionCompilerList(final int size) {
        super(size);
      }

      @Override
      public void removeRange(final int fromIndex, final int toIndex) {
        super.removeRange(fromIndex, toIndex);
      }
    }
  }
}
