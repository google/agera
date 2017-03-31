![Agera](https://raw.githubusercontent.com/google/agera/master/doc/images/agera.png)

Reactive Programming for Android
================================
[![Build Status](https://travis-ci.org/google/agera.svg?branch=master)](https://travis-ci.org/google/agera)
[![Coverage](https://codecov.io/gh/google/agera/branch/master/graph/badge.svg)](https://codecov.io/gh/google/agera)
[![Download](https://api.bintray.com/packages/ernstsson/Agera/agera/images/download.svg)](https://bintray.com/ernstsson/Agera/agera/_latestVersion)

Agera is a set of classes and interfaces to help write functional, asynchronous, and reactive
applications for Android.

Requires Android SDK version 9 or higher.

Usage
-----

To add a dependency using Gradle:

```
  compile 'com.google.android.agera:agera:1.3.0'
```

Learn about Agera
------------------

- [Agera Explained](https://github.com/google/agera/wiki)
- [Agera Codelab](https://codelabs.developers.google.com/codelabs/android-agera)

Experimental Sample Extensions
------------------------------------

A few experimental sample extension libraries for Agera are also provided. These are:

- Content - For `android.content` interaction, such as `BroadcastReceiver` and `SharedPreferences`
- Database - For `SQLiteDatabase` interaction
- Net - For `HTTPUrlConnection` interaction
- RVAdapter - For `RecyclerView` interaction
- RVDatabinding - For `RecyclerView` data binding interaction

To add dependencies to these using Gradle:

```
  compile 'com.google.android.agera:content:1.3.0'
  compile 'com.google.android.agera:database:1.3.0'
  compile 'com.google.android.agera:net:1.3.0'
  compile 'com.google.android.agera:rvadapter:1.3.0'
  compile 'com.google.android.agera:rvdatabinding:1.3.0'
```

FAQ: What's the relation with RxJava?
-----
See [this issue](https://github.com/google/agera/issues/20).

Links
-----

- [GitHub project](https://github.com/google/agera)
- [Issue tracker](https://github.com/google/agera/issues/new)

### (Unofficial) wiki translations

- [Chinese](https://github.com/captain-miao/AndroidAgeraTutorial/wiki)
- [Korean](https://github.com/ZeroBrain/agera-wiki-kr/wiki)

### (Unofficial) Extensions

- [retrofit-agera-call-adapter](https://github.com/drakeet/retrofit-agera-call-adapter)
