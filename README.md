![Agera](https://github.com/google/agera/blob/master/doc/images/agera.png)
Reactive Programming for Android
================================

Agera is a set of classes and interfaces to help write functional, asynchronous, and reactive 
applications for Android.

Requires Android SDK version 9 or higher.

Versions
--------

The latest version of Agera is 1.0.0-RC1

Usage
-----

To add a dependency using Gradle:

```
  compile 'com.google.android.agera:agera:1.0.0-RC1'
```

Learn about Agera
------------------

- [Agera Explained](https://github.com/google/agera/wiki)

Experimental Sample Extensions
------------------------------------

A few experimental sample extension libraries for Agera are also provided. These are:

- Content - For `android.content` interaction, such as `BroadcastReceiver` and `SharedPreferences`
- Database - For `SQLiteDatabase` interaction
- Net - For `HTTPUrlConnection` interaction
- RVAdapter - For `RecyclerView` interaction

To add dependencies to these using Gradle:

```
  compile 'com.google.android.agera:content:1.0.0-RC1'
  compile 'com.google.android.agera:database:1.0.0-RC1'
  compile 'com.google.android.agera:net:1.0.0-RC1'
  compile 'com.google.android.agera:rvadapter:1.0.0-RC1'
```

FAQ: What's the relation with RxJava?
-----
See [this issue](https://github.com/google/agera/issues/20).

Links
-----

- [GitHub project](https://github.com/google/agera)
- [Issue tracker](https://github.com/google/agera/issues/new)
