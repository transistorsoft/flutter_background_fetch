# Flutter background_fetch

[![](https://dl.dropboxusercontent.com/s/nm4s5ltlug63vv8/logo-150-print.png?dl=1)](https://www.transistorsoft.com)

By [**Transistor Software**](https://www.transistorsoft.com), creators of [**Flutter Background Geolocation**](https://www.transistorsoft.com/shop/products/flutter-background-geolocation)

------------------------------------------------------------------------------

Background Fetch is a *very* simple plugin which will awaken an app in the background about **every 15 minutes**, providing a short period of background running-time.  This plugin will execute your provided `callbackFn` whenever a background-fetch event occurs.

:new: Background Fetch now provides a [__`scheduleTask`__](#executing-custom-tasks) method for scheduling arbitrary "one-shot" or periodic tasks.

### iOS
- There is **no way** to increase the rate which a fetch-event occurs and this plugin sets the rate to the most frequent possible &mdash; you will **never** receive an event faster than **15 minutes**.  The operating-system will automatically throttle the rate the background-fetch events occur based upon usage patterns.  Eg: if user hasn't turned on their phone for a long period of time, fetch events will occur less frequently.
- [__`scheduleTask`__](#executing-custom-tasks) seems only to fire when the device is plugged into power.  **`scheduleTask` is designed for low-priority tasks and will never run as frequently as you desire**.
- The default `fetch` task will run far more frequently.
- ⚠️ When your app is **terminated**, iOS *no longer fires events* &mdash; There is *no such thing* as **`stopOnTerminate: false`** for iOS.
- iOS can task *days* before Apple's machine-learning algorithm settles in and begins regularly firing events.  Do not sit staring at your logs waiting for an event to fire.  If your *simulated events* work, that's all you need to know that everything is correctly configured.
- If the user doesn't open your *iOS* app for long periods of time, *iOS* will **stop firing events**.

### Android
- The Android plugin provides a [Headless](https://pub.dartlang.org/documentation/background_fetch/latest/background_fetch/BackgroundFetchConfig/enableHeadless.html) implementation allowing you to continue handling events even after app-termination.


# Contents

- ### 📚 [API Documentation](https://pub.dartlang.org/documentation/background_fetch/latest/background_fetch/BackgroundFetch-class.html)
- ### [Installing the Plugin](#-installing-the-plugin)
- ### [Setup Guides](#-setup-guides)
- ### [Example](#-example)
- ### [Debugging](#-debugging)
- ### [Demo Application](#-demo-application)

## 🔷 Installing the plugin

📂 **`pubspec.yaml`**:

```yaml
dependencies:
  background_fetch: '^1.0.3'
```

### Or latest from Git:

```yaml
dependencies:
  background_fetch:
    git:
      url: https://github.com/transistorsoft/flutter_background_fetch
```

## 🔷 Setup Guides

- [iOS](https://github.com/transistorsoft/flutter_background_fetch/blob/master/help/INSTALL-IOS.md)
- [Android](https://github.com/transistorsoft/flutter_background_fetch/blob/master/help/INSTALL-ANDROID.md)


## 🔷 Example

```dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:background_fetch/background_fetch.dart';

// [Android-only] This "Headless Task" is run when the Android app 
// is terminated with enableHeadless: true
void backgroundFetchHeadlessTask(HeadlessTask task) async {
  String taskId = task.taskId;
  bool isTimeout = task.timeout;
  if (isTimeout) {
    // This task has exceeded its allowed running-time.  
    // You must stop what you're doing and immediately .finish(taskId)
    print("[BackgroundFetch] Headless task timed-out: $taskId");
    BackgroundFetch.finish(taskId);
    return;
  }  
  print('[BackgroundFetch] Headless event received.');
  // Do your work here...
  BackgroundFetch.finish(taskId);
}

void main() {
  // Enable integration testing with the Flutter Driver extension.
  // See https://flutter.io/testing/ for more info.
  runApp(new MyApp());

  // Register to receive BackgroundFetch events after app is terminated.
  // Requires {stopOnTerminate: false, enableHeadless: true}
  BackgroundFetch.registerHeadlessTask(backgroundFetchHeadlessTask);
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _enabled = true;
  int _status = 0;
  List<DateTime> _events = [];

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    // Configure BackgroundFetch.
    int status = await BackgroundFetch.configure(BackgroundFetchConfig(
        minimumFetchInterval: 15,
        stopOnTerminate: false,
        enableHeadless: true,
        requiresBatteryNotLow: false,
        requiresCharging: false,
        requiresStorageNotLow: false,
        requiresDeviceIdle: false,
        requiredNetworkType: NetworkType.NONE
    ), (String taskId) async {  // <-- Event handler
      // This is the fetch-event callback.
      print("[BackgroundFetch] Event received $taskId");
      setState(() {
        _events.insert(0, new DateTime.now());
      });
      // IMPORTANT:  You must signal completion of your task or the OS can punish your app
      // for taking too long in the background.
      BackgroundFetch.finish(taskId);
    }, (String taskId) async {  // <-- Task timeout handler.
      // This task has exceeded its allowed running-time.  You must stop what you're doing and immediately .finish(taskId)
      print("[BackgroundFetch] TASK TIMEOUT taskId: $taskId");
      BackgroundFetch.finish(taskId);
    });
    print('[BackgroundFetch] configure success: $status');
    setState(() {
      _status = status;
    });        

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
  }

  void _onClickEnable(enabled) {
    setState(() {
      _enabled = enabled;
    });
    if (enabled) {
      BackgroundFetch.start().then((int status) {
        print('[BackgroundFetch] start success: $status');
      }).catchError((e) {
        print('[BackgroundFetch] start FAILURE: $e');
      });
    } else {
      BackgroundFetch.stop().then((int status) {
        print('[BackgroundFetch] stop success: $status');
      });
    }
  }

  void _onClickStatus() async {
    int status = await BackgroundFetch.status;
    print('[BackgroundFetch] status: $status');
    setState(() {
      _status = status;
    });
  }
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('BackgroundFetch Example', style: TextStyle(color: Colors.black)),
          backgroundColor: Colors.amberAccent,
          brightness: Brightness.light,
          actions: <Widget>[
            Switch(value: _enabled, onChanged: _onClickEnable),
          ]
        ),
        body: Container(
          color: Colors.black,
          child: new ListView.builder(
              itemCount: _events.length,
              itemBuilder: (BuildContext context, int index) {
                DateTime timestamp = _events[index];
                return InputDecorator(
                    decoration: InputDecoration(
                        contentPadding: EdgeInsets.only(left: 10.0, top: 10.0, bottom: 0.0),
                        labelStyle: TextStyle(color: Colors.amberAccent, fontSize: 20.0),
                        labelText: "[background fetch event]"
                    ),
                    child: new Text(timestamp.toString(), style: TextStyle(color: Colors.white, fontSize: 16.0))
                );
              }
          ),
        ),
        bottomNavigationBar: BottomAppBar(
          child: Row(
            children: <Widget>[
              RaisedButton(onPressed: _onClickStatus, child: Text('Status')),
              Container(child: Text("$_status"), margin: EdgeInsets.only(left: 20.0))
            ]
          )
        ),
      ),
    );
  }
}
```

### Executing Custom Tasks

In addition to the default background-fetch task defined by `BackgroundFetch.configure`, you may also execute your own arbitrary "oneshot" or periodic tasks (iOS requires additional [Setup Instructions](./help/INSTALL-IOS.md)).  However, all events will be fired into the Callback provided to **`BackgroundFetch#configure`**:

### ⚠️ iOS:  
- `scheduleTask` on *iOS* seems only to run when the device is plugged into power.
- `scheduleTask` on *iOS* are designed for *low-priority* tasks, such as purging cache files &mdash; they tend to be **unreliable for mission-critical tasks**.  `scheduleTask` will *never* run as frequently as you want.
- The default `fetch` event is much more reliable and fires far more often.
- `scheduleTask` on *iOS* stop when the *user* terminates the app.  There is no such thing as `stopOnTerminate: false` for *iOS*.

```dart
// Step 1:  Configure BackgroundFetch as usual.
int status = await BackgroundFetch.configure(BackgroundFetchConfig(
  minimumFetchInterval: 15
), (String taskId) async {  // <-- Event callback.
  // This is the fetch-event callback.
  print("[BackgroundFetch] taskId: $taskId");

  // Use a switch statement to route task-handling.
  switch (taskId) {
    case 'com.transistorsoft.customtask':
      print("Received custom task");
      break;
    default:
      print("Default fetch task");
  }
  // Finish, providing received taskId.
  BackgroundFetch.finish(taskId);
}, (String taskId) async {  // <-- Event timeout callback
  // This task has exceeded its allowed running-time.  You must stop what you're doing and immediately .finish(taskId)
  print("[BackgroundFetch] TIMEOUT taskId: $taskId");
  BackgroundFetch.finish(taskId);
});

// Step 2:  Schedule a custom "oneshot" task "com.transistorsoft.customtask" to execute 5000ms from now.
BackgroundFetch.scheduleTask(TaskConfig(
  taskId: "com.transistorsoft.customtask",
  delay: 5000  // <-- milliseconds
));
```

## 🔷 Debugging

### iOS

#### :new: Simulating events for `BGTaskScheduler` API for iOS 13+

- ⚠️ At the time of writing, the new task simulator does not yet work in Simulator; Only real devices.
- After running your app in XCode, Click the `[||]` button to initiate a *Breakpoint*.
- In the console `(lldb)`, paste the following command (**Note:**  use cursor up/down keys to cycle through previously run commands):
```obj-c
e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"com.transistorsoft.fetch"]
```
- Click the `[ > ]` button to continue.  The task will execute and the Callback function provided to **`BackgroundFetch.configure`** will receive the event.


![](https://dl.dropboxusercontent.com/s/zr7w3g8ivf71u32/ios-simulate-bgtask-pause.png?dl=1)

![](https://dl.dropboxusercontent.com/s/87c9uctr1ka3s1e/ios-simulate-bgtask-paste.png?dl=1)

![](https://dl.dropboxusercontent.com/s/bsv0avap5c2h7ed/ios-simulate-bgtask-play.png?dl=1)

#### Simulating task-timeout events

- Only the new `BGTaskScheduler` api supports *simulated* task-timeout events.  To simulate a task-timeout, your `fetchCallback` must not call `BackgroundFetch.finish(taskId)`:

```dart
BackgroundFetch.configure(BackgroundFetchConfig(
  minimumFetchInterval: 15
), (String taskId) async {  // <-- Event callback.
  // This is the fetch-event callback.
  print("[BackgroundFetch] taskId: $taskId"); 
  //BackgroundFetch.finish(taskId); // <-- Disable .finish(taskId) when simulating an iOS task timeout
}, (String taskId) async {  // <-- Event timeout callback
  // This task has exceeded its allowed running-time.  You must stop what you're doing and immediately .finish(taskId)
  print("[BackgroundFetch] TIMEOUT taskId: $taskId");
  BackgroundFetch.finish(taskId);
});
```

- Now simulate an iOS task timeout as follows, in the same manner as simulating an event above:
```obj-c
e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateExpirationForTaskWithIdentifier:@"com.transistorsoft.fetch"]
```

#### Old `BackgroundFetch` API
- Simulate background fetch events in XCode using **`Debug->Simulate Background Fetch`**
- iOS can take some hours or even days to start a consistently scheduling background-fetch events since iOS schedules fetch events based upon the user's patterns of activity.  If *Simulate Background Fetch* works, your can be **sure** that everything is working fine.  You just need to wait.

### Android

- Observe plugin logs in `$ adb logcat`:
```bash
$ adb logcat *:S flutter:V, TSBackgroundFetch:V
```
![](https://dl.dropbox.com/s/fmrk5i06lojf9yh/android-adb-logcat-TSBackgroundFetch.png?dl=1))

- Simulate a background-fetch event on a device (insert *&lt;your.application.id&gt;*) (only works for sdk `21+`:
```bash
$ adb shell cmd jobscheduler run -f <your.application.id> 999
```

- For devices with sdk `<21`, simulate a "Headless" event with (insert *&lt;your.application.id&gt;*)
```bash
$ adb shell am broadcast -a <your.application.id>.event.BACKGROUND_FETCH

```

## 🔷 Demo Application

This repo contains an `/example` folder.  Clone this repo and open the `/example` folder in Android Studio.

## 🔷 Implementation

### iOS

Implements [performFetchWithCompletionHandler](https://developer.apple.com/library/ios/documentation/UIKit/Reference/UIApplicationDelegate_Protocol/Reference/Reference.html#//apple_ref/occ/intfm/UIApplicationDelegate/application:performFetchWithCompletionHandler:), firing a custom event subscribed-to in cordova plugin.

### Android

Android implements background fetch using two different mechanisms, depending on the Android SDK version.  Where the SDK version is `>= LOLLIPOP`, the new [`JobScheduler`](https://developer.android.com/reference/android/app/job/JobScheduler.html) API is used.  Otherwise, the old [`AlarmManager`](https://developer.android.com/reference/android/app/AlarmManager.html) will be used.

Unlike iOS, the Android implementation *can* continue to operate after application terminate (`stopOnTerminate: false`) or device reboot (`startOnBoot: true`).

## 🔷 Licence

The MIT License

Copyright (c) 2018 Chris Scott, Transistor Software <chris@transistorsoft.com>
https://www.transistorsoft.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

