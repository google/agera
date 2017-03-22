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

import android.support.annotation.NonNull;
import java.util.List;

public final class NoteGroup {
  @NonNull
  private final String name;
  private final long id;
  @NonNull
  private final List<Note> notes;

  private NoteGroup(@NonNull final String name, final long id, @NonNull final List<Note> notes) {
    this.name = name;
    this.id = id;
    this.notes = notes;
  }

  @NonNull
  public static NoteGroup noteGroup(
      @NonNull final String name, final long id, @NonNull final List<Note> notes) {
    return new NoteGroup(name, id, notes);
  }

  public long getId() {
    return id;
  }

  @NonNull
  public String getName() {
    return name;
  }

  @NonNull
  public List<Note> getNotes() {
    return notes;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    final NoteGroup noteGroup = (NoteGroup) o;

    if (id != noteGroup.id) { return false; }
    return notes.equals(noteGroup.notes);

  }

  @Override
  public int hashCode() {
    int result = (int) (id ^ (id >>> 32));
    result = 31 * result + notes.hashCode();
    return result;
  }
}
