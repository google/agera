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
package com.google.android.agera.testapp;

import android.support.annotation.NonNull;

public final class Note {
  private final int id;
  @NonNull
  private final String note;

  private Note(final int id, @NonNull final String note) {
    this.id = id;
    this.note = note;
  }

  @NonNull
  public static Note note(final int id, @NonNull final String note) {
    return new Note(id, note);
  }

  public long getId() {
    return id;
  }

  @NonNull
  public String getNote() {
    return note;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Note note1 = (Note) o;
    return id == note1.id && note.equals(note1.note);
  }

  @Override
  public int hashCode() {
    return 31 * id + note.hashCode();
  }
}
