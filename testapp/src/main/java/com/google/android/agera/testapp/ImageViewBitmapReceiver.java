package com.google.android.agera.testapp;

import static com.google.android.agera.Preconditions.checkNotNull;

import com.google.android.agera.Receiver;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.widget.ImageView;

final class ImageViewBitmapReceiver implements Receiver<Bitmap> {
  @NonNull
  private final ImageView imageView;

  public ImageViewBitmapReceiver(@NonNull final ImageView imageView) {
    this.imageView = checkNotNull(imageView);
  }

  @Override
  public void accept(@NonNull final Bitmap value) {
    imageView.setImageBitmap(value);
  }
}
