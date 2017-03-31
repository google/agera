package com.google.android.agera.testapp;

import android.support.annotation.NonNull;
import com.google.android.agera.Function;

class NoteGroupToID implements Function<NoteGroup, Long> {

  public static final NoteGroupToID INSTANCE = new NoteGroupToID();

  private NoteGroupToID() {}

  static NoteGroupToID noteGroupToID() {return INSTANCE;}

  @NonNull
  @Override
  public Long apply(@NonNull final NoteGroup input) {
    return input.getId();
  }
}
