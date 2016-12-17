package com.google.android.agera.testapp;

import static android.os.StrictMode.setThreadPolicy;
import static android.os.StrictMode.setVmPolicy;

import android.app.Application;
import android.os.StrictMode.ThreadPolicy;
import android.os.StrictMode.VmPolicy;

public final class NotesApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().build());
    setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build());
  }
}
