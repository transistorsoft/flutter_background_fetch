# CHANGELOG

## 1.1.0 - 2022-04-06
* [iOS] Fix type-error when .start() raises an error (#281)
* [Android] Better error-handling when headlessTask is registered incorrectly (#242)
* [Android] Android 12 compatibility:  Add new required permission android.permission.SCHEDULE_EXACT_ALARM

## 1.0.3 - 2021-11-24
* [Fixed][Android] Fix typo related to requiredNetworkType, causing null pointer error.

## 1.0.2 - 2021-11-10
* [Changed][Android] Remove deprectated `jcenter` repository.  Replaced with `mavenCentral`.

## 1.0.1 - 2021-06-30
* [Changed][Android] Allow multiple calls to .configure to allow re-configuring the fetch task.  Existing task will be cancelled and a new periodic fetch task re-scheduled according to new config.
* [Changed][Android] Ignore initial fetch task fired immediately.
* [Changed][Android] `android:exported="false"` on `BootReceiver` to resolve reported security analysis.

## 1.0.0 - 2021-06-09
* Release 1.0.0-nullsafety.3 as 1.0.0

## 1.0.0-nullsafety.3 - 2021-06-09
* [Fixed][Android] null check in FetchStreamHandler that mEventSink != null
* [Changed][Android] Add new logic block to isMainActivityActive:  compare launchActivityName with task.baseActivity.getClassName()

## 1.0.0-nullsafety.2 - 2021-04-01
* [Fixed][Android] Flutter 2 broke Android Headless Task with Null-pointer exception.

## 1.0.0-nullsafety.1 - 2021-02-18
* [Fixed][Android] Fix `java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String com.transistorsoft.tsbackgroundfetch.BGTask.getTaskId()' on a null object reference`

## 1.0.0-nullsafety.0 - 2021-02-15
* [Changed] Implement [null-safety](https://dart.dev/null-safety) (Thanks to @GinoTerlouw).

## 0.7.0 - 2021-02-11
* [Added][iOS] Implement two new iOS options for `BackgroundFetch.scheduleTask`:
    - `bool requiresNetworkConnectivity`
    - `bool requiresCharging` (previously Android-only).
    
* [Changed][iOS] Migrate `TSBackgroundFetch.framework` to new `.xcframework` for *MacCatalyst* support with new Apple silcon.

### :warning: Breaking Change:  Requires `cocoapods >= 1.10+`.

*iOS'* new `.xcframework` requires *cocoapods >= 1.10+*:

```bash
$ pod --version
// if < 1.10.0
$ sudo gem install cocoapods
```

* [Added] task-timeout callback.  `BackgroundFetch.configure` now accepts a 3rd argument `onTimeout` callback.  This callback will be executed when the operating system has signalled your task has expired before your task has called `BackgroundFetch.finish(taskId)`.  You must stop whatever you're task is doing and execute `BackgroundFetch.finish(taskId)` immediately.
```dart
BackgroundFetch.configure(BackgroundFetchConfig(
  minimumFetchInterval: 15
), (String taskId) {  // <-- task callback.
  print("[BackgroundFetch] taskId: $taskId");
  BackgroundFetch.finish(taskId);
}, (String taskId) {  // <-- NEW:  task-timeout callback.
  // This task has exceeded its allowed running-time.  You must stop what you're doing immediately finish(taskId)
  print("[BackgroundFetch] TIMEOUT taskId: $taskId");
  BackgroundFetch.finish(taskId);
});
```

### :warning: [Android] Breaking Change For Android Headless-task
- Since the registered Android headless-task (`BackgroundFetch.registerHeadlessTask`) can only receive a single parameter, your headless-task will now receive a `HeadlessTask task` instance rather than `String taskId` **in order to differentiate task-timeout events**.

__OLD__
```dart
void myBackgroundFetchHeadlessTask(String taskId) async { // <-- OLD:  String taskId
  print("[BackgroundFetch] Headless task: $taskId");
  BackgroundFetch.finish(taskId);
}
BackgroundFetch.registerHeadlessTask(myBackgroundFetchHeadlessTask);
```
__NEW__
```dart
void myBackgroundFetchHeadlessTask(HeadlessTask task) async { // <-- NEW:  HeadlessTask now provided.
  String taskId = task.taskId;    // <-- NEW:  Get taskId from HeadlessTask
  bool isTimeout = task.timeout;  // <-- NEW:  true if this task has timed-out.
  if (isTimeout) {
    // This task has exceeded its allowed running-time.  You must stop what you're doing immediately finish(taskId)
    print("[BackgroundFetch] Headless TIMEOUT: $taskId");
    BackgroundFetch.finish(taskId);
    return;
  }
  print("[BackgroundFetch] Headless task: $taskId");
  BackgroundFetch.finish(taskId);
}
BackgroundFetch.registerHeadlessTask(myBackgroundFetchHeadlessTask);
```

## 0.6.0 - 2020-06-11
* [Fixed][Android] `com.android.tools.build:gradle:4.0.0` no longer allows "*direct local aar dependencies*".  The Android Setup now requires a custom __`maven url`__ to be added to your app's root __`android/build.gradle`__:

```diff
allprojects {
    repositories {
        google()
        jcenter()
+       maven {
+           // [required] background_fetch
+           url "${project(':background_fetch').projectDir}/libs"
+       }
    }
}
```

## 0.5.6
* [Fixed][Android] using `forceAlarmManager: true` fails to restart fetch events after reboot.
* [Fixed] Android check `wakeLock.isHeld()` before executing `wakeLock.release()`.

## 0.5.5 - 2020-03-24
* [Fixed] [iOS] bug with `start` plugin after executing `stop`.

## 0.5.4 - 2020-02-22

* [Fixed] [Android] Add `@Keep` annotation to `HeadlessTask.java` to prevent minifying this classs in release builds since the SDK uses reflection to find this class.

## 0.5.3 - 2020-02-21
* [Fixed] [Android] `stopOnTerminate` not cancelling scheduled job / Alarm when fired task fired after terminate.

## 0.5.2 - 2020-02-20
* [Android] Fix Android NPE in `hasTaskId` for case where plugin is installed first time in had previous version of plugin

## 0.5.1 - 2020-02-19

## Minor Breaking Change for iOS Setup 

* [iOS] It's no longer necessary to `registerBGProcessingTask` in `AppDelegate.m` for tasks registered for use with `#scheduleTask`.  The SDK now reads the App `.plist` and automatically registers those tasks found in  *"Permitted background task scheduler identifiers"*.  Remove **all** code in your `AppDelegate.m` that references `TSBackgroundFetch`.
![](https://dl.dropboxusercontent.com/s/t5xfgah2gghqtws/ios-setup-permitted-identifiers.png?dl=1)


## 0.5.0 - 2020-02-03
* [Added] [Android] New option `forceAlarmManager` for bypassing `JobScheduler` mechanism in favour of `AlarmManager` for more precise scheduling task execution.
* [Changed] Migrate iOS deprecated "background-fetch" API to new [BGTaskScheduler](https://developer.apple.com/documentation/backgroundtasks/bgtaskscheduler?language=objc).  See new required steps in iOS Setup.
* [Added] Added new `BackgroundFetch.scheduleTask` method for scheduling custom "onehot" and periodic tasks in addition to the default fetch-task.
```dart
BackgroundFetch.configure(BackgroundFetchConfig(
  minimumFetchInterval: 15,
  stopOnTerminate: false
), (String taskId) {  // <-- [NEW] taskId provided to Callback
  print("[BackgroundFetch] taskId: $taskId");
  switch(taskId) {
    case 'foo':
      // Handle scheduleTask 'foo'
      break;
    default:
      // Handle default fetch event.
      break;
  }
  BackgroundFetch.finish(taskId);  // <-- [NEW] Provided taskId to #finish method.
});

// This event will end up in Callback provided to #configure above.
BackgroundFetch.scheduleTask(TaskConfig(
  taskId: 'foo',  //<-- required
  delay: 60000,
  periodic: false  
));
```

## Breaking Changes
* With the introduction of ability to execute custom tasks via `#scheduleTask`, all tasks are executed in the Callback provided to `#configure`.  As a result, this Callback is now provided an argument `String taskId`.  This `taskId` must now be provided to the `#finish` method, so that the SDK knows *which* task is being `#finish`ed.

```dart
BackgroundFetch.configure(BackgroundFetchConfig(
  minimumFetchInterval: 15,
  stopOnTerminate: false
), (String taskId) {  // <-- [NEW] taskId provided to Callback
  print("[BackgroundFetch] taskId: $taskId");
  BackgroundFetch.finish(taskId);  // <-- [NEW] Provided taskId to #finish method.
});
```

And with the Headless Task, as well:
```dart
/// This "Headless Task" is run when app is terminated.
void backgroundFetchHeadlessTask(String taskId) async {  // <-- 1.  Headless task receives String taskId
  print("[BackgroundFetch] Headless event received: $taskId");
  
  BackgroundFetch.finish(taskId);  // <-- 2.  #finish with taskId here as well.
}

void main() {
  // Enable integration testing with the Flutter Driver extension.
  // See https://flutter.io/testing/ for more info.
  runApp(new MyApp());

  // Register to receive BackgroundFetch events after app is terminated.
  // Requires {stopOnTerminate: false, enableHeadless: true}
  BackgroundFetch.registerHeadlessTask(backgroundFetchHeadlessTask);
}
``` 


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
