## 0.4.0 - 2019-12-17
* [Changed] Upgrade to new Flutter Plugin API "V2".  Requires flutter sdk version 1.12.  See [Upgrading pre 1.12 Android Projects](https://github.com/flutter/flutter/wiki/Upgrading-pre-1.12-Android-projects)

## 0.3.2 - 2019-10-06
* [Fixed] Resolve Android StrictMode violations; typically from accessing SharedPreferences on main-thread.

## 0.3.1 - 2019-09-20
* Fix error `FlutterMain.findBundleAppPath()`.  The plugin modified a deprecated API for flutter 1.9.1, breaking those on previous flutter versions.  Will use deprecated API for now.

## 0.3.0 - 2019-09-18
* Implement Android `JobInfo` constraints.
* Fix `NSLog` warnings casting to `long`
* Default `startOnBoot: true` in example

## 0.2.0 - 2019-03-15
* Use AndroidX.

## 0.1.2 - 2019-02-28
* Fixed bug with setting `jobServiceClass` using a reference to `HeadlessJobService.class`.  This crashes devices < api 21, since Android's `JobService` wasn't available until then.  Simply provide the class name as a `String`.

## 0.1.1 - 2018-11-21
* Fixed issue with Android headless config.

## 0.1.0

* First working implementation

## 0.0.1

* First working implementation
