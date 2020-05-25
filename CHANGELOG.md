## Unreleased
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
