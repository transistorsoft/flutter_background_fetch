import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/material.dart';

const _PLUGIN_PATH = "com.transistorsoft/flutter_background_fetch";
const _METHOD_CHANNEL_NAME = "$_PLUGIN_PATH/methods";
const _EVENT_CHANNEL_NAME = "$_PLUGIN_PATH/events";

/// Configuration
///
/// ```dart
/// BackgroundFetch.configure(BackgroundFetchConfig(
///   minimumFetchInterval: 15,
///   stopOnTerminate: false,
///   startOnBoot: true,
///   enableHeadless: true
/// ), () {
///   // This callback is typically fired every 15 minutes while in the background.
///   print('[BackgroundFetch] Event received.');
///   // IMPORTANT:  You must signal completion of your fetch task or the OS could
///   // punish your app for spending much time in the background.
///   BackgroundFetch.finish();
/// })
///
class BackgroundFetchConfig {
  /// This job doesn't care about network constraints, either any or none.
  static const int NETWORK_TYPE_NONE = 0;

  /// This job requires network connectivity.
  static const int NETWORK_TYPE_ANY = 1;

  /// This job requires network connectivity that is unmetered.
  static const int NETWORK_TYPE_UNMETERED = 2;

  /// This job requires network connectivity that is not roaming.
  static const int NETWORK_TYPE_NOT_ROAMING = 3;

  /// This job requires network connectivity that is a cellular network.
  static const int NETWORK_TYPE_CELLULAR = 4;

  /// The minimum interval in minutes to execute background fetch events.
  ///
  /// Defaults to `15` minutes. Note: Background-fetch events will never occur at a frequency higher than every 15 minutes. Apple uses a secret algorithm to adjust the frequency of fetch events, presumably based upon usage patterns of the app. Fetch events can occur less often than your configured `minimumFetchInterval`.
  ///
  int minimumFetchInterval;

  /// __Android only__: Set `false` to continue background-fetch events after user terminates the app. Default to `true`.
  bool stopOnTerminate;

  /// __Android only__: Set `true` to initiate background-fetch events when the device is rebooted. Defaults to `false`.
  ///
  /// ❗ NOTE: [startOnBoot] requires [stopOnTerminate]: `false`.
  ///
  bool startOnBoot;

  /// __Android only__: Set `true` to automatically relaunch the application (if it was terminated).
  ///
  /// The application will launch to the foreground then immediately minimize. Defaults to `false`.
  bool forceReload;

  /// __Android only__: Set true to enable the Headless mechanism, for handling fetch events after app termination.
  ///
  /// See also:  [BackgroundFetch.registerHeadlessTask].
  ///
  /// * :open_file_folder: **`lib/main.dart`**
  ///
  /// ```dart
  /// import 'dart:async';
  /// import 'package:flutter/material.dart';
  /// import 'package:flutter/services.dart';
  ///
  /// import 'package:background_fetch/background_fetch.dart';
  ///
  /// // This "Headless Task" is run when app is terminated.
  /// void backgroundFetchHeadlessTask() async {
  ///   print('[BackgroundFetch] Headless event received.');
  ///   BackgroundFetch.finish();
  /// }
  ///
  /// void main() {
  ///   // Enable integration testing with the Flutter Driver extension.
  ///   // See https://flutter.io/testing/ for more info.
  ///   runApp(new MyApp());
  ///
  ///   // Register to receive BackgroundFetch events after app is terminated.
  ///   // Requires {stopOnTerminate: false, enableHeadless: true}
  ///   BackgroundFetch.registerHeadlessTask(backgroundFetchHeadlessTask);
  /// }
  /// ```
  bool enableHeadless;

  /// [Android only] Set detailed description of the kind of network your job requires.
  ///
  /// If your job doesn't need a network connection, you don't need to use this option, as the default is [[BackgroundFetch.NEWORK_TYPE_NONE]].
  ///
  /// Calling this method defines network as a strict requirement for your job. If the network requested is not available your job will never run.
  ///
  /// | NetworkType                                      | Description                                                          |
  ///	|--------------------------------------------------|----------------------------------------------------------------------|
  ///	| [BackgroundFetchConfig.NETWORK_TYPE_NONE]        | This job doesn't care about network constraints, either any or none. |
  ///	| [BackgroundFetchConfig.NETWORK_TYPE_ANY]  	     | This job requires network connectivity.                              |
  ///	| [BackgroundFetchConfig.NETWORK_TYPE_CELLULAR]    | This job requires network connectivity that is a cellular network.   |
  ///	| [BackgroundFetchConfig.NETWORK_TYPE_UNMETERED]   | This job requires network connectivity that is unmetered.            |
  ///	| [BackgroundFetchConfig.NETWORK_TYPE_NOT_ROAMING] | This job requires network connectivity that is not roaming.          |
  ///
  int requiredNetworkType;

  ///
  /// [Android only] Specify that to run this job, the device's battery level must not be low.
  ///
  ///This defaults to false. If true, the job will only run when the battery level is not low, which is generally the point where the user is given a "low battery" warning.
  ///
  bool requiresBatteryNotLow;

  ///
  /// [Android only] Specify that to run this job, the device's available storage must not be low.
  ///
  /// This defaults to false. If true, the job will only run when the device is not in a low storage state, which is generally the point where the user is given a "low storage" warning.
  ///
  bool requiresStorageNotLow;

  ///
  /// [Android only] Specify that to run this job, the device must be charging (or be a non-battery-powered device connected to permanent power, such as Android TV devices). This defaults to false.
  ///
  bool requiresCharging;

  ///
  /// [Android only] When set true, ensure that this job will not run if the device is in active use.
  ///
  /// The default state is false: that is, the for the job to be runnable even when someone is interacting with the device.
  ///
  /// This state is a loose definition provided by the system. In general, it means that the device is not currently being used interactively, and has not been in use for some time. As such, it is a good time to perform resource heavy jobs. Bear in mind that battery usage will still be attributed to your application, and surfaced to the user in battery stats.
  ///
  bool requiresDeviceIdle;

  BackgroundFetchConfig(
      {this.minimumFetchInterval,
      this.stopOnTerminate,
      this.startOnBoot,
      this.forceReload,
      this.enableHeadless,
      this.requiredNetworkType,
      this.requiresBatteryNotLow,
      this.requiresStorageNotLow,
      this.requiresCharging,
      this.requiresDeviceIdle});

  Map toMap() {
    Map config = {};
    if (minimumFetchInterval != null)
      config['minimumFetchInterval'] = minimumFetchInterval;
    if (stopOnTerminate != null) config['stopOnTerminate'] = stopOnTerminate;
    if (startOnBoot != null) config['startOnBoot'] = startOnBoot;
    if (forceReload != null) config['forceReload'] = forceReload;
    if (enableHeadless != null) config['enableHeadless'] = enableHeadless;
    if (requiredNetworkType != null)
      config['requiredNetworkType'] = requiredNetworkType;
    if (requiresBatteryNotLow != null)
      config['requiresBatteryNotLow'] = requiresBatteryNotLow;
    if (requiresStorageNotLow != null)
      config['requiresStorageNotLow'] = requiresStorageNotLow;
    if (requiresCharging != null) config['requiresCharging'] = requiresCharging;
    if (requiresDeviceIdle != null)
      config['requiresDeviceIdle'] = requiresBatteryNotLow;

    return config;
  }
}

/// BackgroundFetch API
///
/// Background Fetch is a *very* simple plugin which will awaken an app in the background about **every 15 minutes**, providing a short period of background running-time.  This plugin will execute your provided `callbackFn` whenever a background-fetch event occurs.
///
/// There is **no way** to increase the rate which a fetch-event occurs and this plugin sets the rate to the most frequent possible &mdash; you will **never** receive an event faster than **15 minutes**.
/// The operating-system will automatically throttle the rate the background-fetch events occur based upon usage patterns.  Eg: if user hasn't turned on their phone for a long period of time, fetch events will occur less frequently.
///
/// The Android plugin provides an [BackgroundFetchConfig.enableHeadless] mechanism allowing you to continue handling events even after app-termination (see **[BackgroundFetchConfig.enableHeadless]**).
///
/// ```dart
/// BackgroundFetch.configure(BackgroundFetchConfig(
///   minimumFetchInterval: 15,
///   stopOnTerminate: false,
///   startOnBoot: true
/// ), () {
///   // This callback is typically fired every 15 minutes while in the background.
///   print('[BackgroundFetch] Event received.');
///   // IMPORTANT:  You must signal completion of your fetch task or the OS could
///   // punish your app for spending much time in the background.
///   BackgroundFetch.finish();
/// })
/// ```
///
class BackgroundFetch {
  /// See [status].  Background updates are unavailable and the user cannot enable them again. For example, this status can occur when parental controls are in effect for the current user.
  static const int STATUS_RESTRICTED = 0;

  /// See [status].  The user explicitly disabled background behavior for this app or for the whole system.
  static const int STATUS_DENIED = 1;

  /// See [status].  Background updates are available for the app.
  static const int STATUS_AVAILABLE = 2;

  /// See [finish].  New data was successfully downloaded.
  static const int FETCH_RESULT_NEW_DATA = 0;

  /// See [finish].  There was no new data to download.
  static const int FETCH_RESULT_NO_DATA = 1;

  /// See [finish].  An attempt to download data was made but that attempt failed.
  static const int FETCH_RESULT_FAILED = 2;

  static const MethodChannel _methodChannel =
      const MethodChannel(_METHOD_CHANNEL_NAME);

  static const EventChannel _eventChannel =
      const EventChannel(_EVENT_CHANNEL_NAME);

  static Stream<dynamic> _eventsFetch;

  /// Configures the plugin's [BackgroundFetchConfig] and `callback` Function. This `callback` will fire each time a background-fetch event occurs (typically every 15 min).
  ///
  /// ```dart
  /// BackgroundFetch.configure(BackgroundFetchConfig(
  ///   minimumFetchInterval: 15,
  ///   stopOnTerminate: false,
  ///   startOnBoot: true
  /// ), () {
  ///   // This callback is typically fired every 15 minutes while in the background.
  ///   print('[BackgroundFetch] Event received.');
  ///   // IMPORTANT:  You must signal completion of your fetch task or the OS could punish your app for
  ///   // spending much time in the background.
  ///   BackgroundFetch.finish(BackgroundFetch.FETCH_RESULT_NEW_DATA);
  /// })
  /// ```
  static Future<int> configure(
      BackgroundFetchConfig config, Function callback) {
    if (_eventsFetch == null) {
      _eventsFetch = _eventChannel.receiveBroadcastStream();

      _eventsFetch.listen((dynamic v) {
        callback();
      });
    }
    Completer completer = new Completer<int>();

    _methodChannel
        .invokeMethod('configure', config.toMap())
        .then((dynamic status) {
      completer.complete(status);
    }).catchError((dynamic e) {
      completer.completeError(e.details);
    });

    return completer.future;
  }

  /// Start the background-fetch API.
  ///
  /// Your `callback` Function provided to [configure] will be executed each time a background-fetch event occurs. NOTE the [configure] method automatically calls [start]. You do not have to call this method after you first [configure] the plugin.
  ///
  static Future<int> start() {
    Completer completer = new Completer<int>();
    _methodChannel.invokeMethod('start').then((dynamic status) {
      completer.complete(status);
    }).catchError((dynamic e) {
      completer.completeError(e.details);
    });
    return completer.future;
  }

  /// Stop the background-fetch API from firing fetch events.
  ///
  /// Your `callback` provided to [configure] will no longer be executed.
  static Future<int> stop() async {
    int status = await _methodChannel.invokeMethod('stop');
    return status;
  }

  static Future<int> get status async {
    int status = await _methodChannel.invokeMethod('status');
    return status;
  }

  /// Signal to the OS that your fetch-event is complete.
  ///
  /// You __MUST__ call `finish` in your fetch `callback` provided to [configure] in order to signal to the OS that your fetch action is complete. iOS provides only 30s of background-time for a fetch-event -- if you exceed this 30s, the OS will punish your app for spending too much time in the background.
  ///
  /// Valid values for fetchResult (int):
  ///
  /// | Name                                    | Value  |
  /// |-----------------------------------------|--------|
  /// | [BackgroundFetch.FETCH_RESULT_NEW_DATA] | `0`    |
  /// | [BackgroundFetch.FETCH_RESULT_FAILED]   | `1`    |
  /// | [BackgroundFetch.FETCH_RESULT_NO_DATA]  | `2`    |
  ///
  ///
  static void finish([int fetchResult]) {
    if (fetchResult == null) {
      fetchResult = FETCH_RESULT_NEW_DATA;
    }
    _methodChannel.invokeMethod('finish', fetchResult);
  }

  /// __Android-only__:  Registers a global function to execute when your app has been terminated.
  ///
  /// **Note:** requires [BackgroundFetchConfig.stopOnTerminate] `false` and [BackgroundFetchConfig.enableHeadless] `true`.
  ///
  /// # Example
  /// * :open_file_folder: **`lib/main.dart`**
  ///
  /// ```dart
  /// import 'dart:async';
  /// import 'package:flutter/material.dart';
  /// import 'package:flutter/services.dart';
  ///
  /// import 'package:background_fetch/background_fetch.dart';
  ///
  /// // This "Headless Task" is run when app is terminated.
  /// void backgroundFetchHeadlessTask() async {
  ///   print('[BackgroundFetch] Headless event received.');
  ///   BackgroundFetch.finish();
  /// }
  ///
  /// void main() {
  ///   // Enable integration testing with the Flutter Driver extension.
  ///   // See https://flutter.io/testing/ for more info.
  ///   runApp(new MyApp());
  ///
  ///   // Register to receive BackgroundFetch events after app is terminated.
  ///   // Requires {stopOnTerminate: false, enableHeadless: true}
  ///   BackgroundFetch.registerHeadlessTask(backgroundFetchHeadlessTask);
  /// }
  /// ```
  /// # Setup
  ///
  /// If you intend to use the SDK's Android *Headless* mechanism, you must perform the following additional setup:
  ///
  /// Create either `Application.kt` or `Application.java` in the same directory as `MainActivity`.
  ///
  /// - For `Application.kt`, use the following:
  ///
  /// ```java
  /// import com.transistorsoft.flutter.backgroundgeolocation.BackgroundFetchPlugin;
  ///
  /// class Application : FlutterApplication(), PluginRegistrantCallback {
  ///   override fun onCreate() {
  ///     super.onCreate();
  ///     BackgroundFetchPlugin.setPluginRegistrant(this);
  ///   }
  ///
  ///   override fun registerWith(registry: PluginRegistry) {
  ///     GeneratedPluginRegistrant.registerWith(registry);
  ///   }
  /// }
  /// ```
  ///
  /// - For `Application.java`, use the following:
  ///
  /// ```java
  /// import com.transistorsoft.flutter.backgroundgeolocation.BackgroundFetchPlugin;
  ///
  /// public class Application extends FlutterApplication implements PluginRegistrantCallback {
  ///   @Override
  ///   public void onCreate() {
  ///     super.onCreate();
  ///     BackgroundFetchPlugin.setPluginRegistrant(this);
  ///   }
  ///
  ///   @Override
  ///   public void registerWith(PluginRegistry registry) {
  ///     GeneratedPluginRegistrant.registerWith(registry);
  ///   }
  /// }
  /// ```
  ///
  /// Now edit `AndroidManifest.xml` and provide a reference to your custom `Application` class:
  /// ```xml
  ///     <application
  ///         android:name=".Application"
  ///         ...
  /// ```
  ///
  static Future<bool> registerHeadlessTask(Function callback) async {
    Completer completer = new Completer<bool>();

    // Two callbacks:  the provided headless-task + _headlessRegistrationCallback
    List<int> args = [
      PluginUtilities.getCallbackHandle(_headlessCallbackDispatcher)
          .toRawHandle(),
      PluginUtilities.getCallbackHandle(callback).toRawHandle()
    ];

    _methodChannel
        .invokeMethod('registerHeadlessTask', args)
        .then((dynamic success) {
      completer.complete(true);
    }).catchError((error) {
      String message = error.toString();
      print('[BackgroundFetch registerHeadlessTask] ‼️ $message');
      completer.complete(false);
    });
    return completer.future;
  }
}

/// Headless Callback Dispatcher
///
void _headlessCallbackDispatcher() {
  WidgetsFlutterBinding.ensureInitialized();
  const MethodChannel _headlessChannel =
      MethodChannel("$_PLUGIN_PATH/headless", JSONMethodCodec());

  _headlessChannel.setMethodCallHandler((MethodCall call) async {
    final args = call.arguments;

    // Run the headless-task.
    try {
      final Function callback = PluginUtilities.getCallbackFromHandle(
          CallbackHandle.fromRawHandle(args['callbackId']));
      if (callback == null) {
        print(
            '[BackgroundFetch _headlessCallbackDispatcher] ERROR: Failed to get callback from handle: $args');
        return;
      }
      callback();
    } catch (e, stacktrace) {
      print('[BackgroundFetch _headlessCallbackDispather] ‼️ Callback error: ' +
          e.toString());
      print(stacktrace);
    }
  });
  // Signal to native side that the client dispatcher is ready to receive events.
  _headlessChannel.invokeMethod('initialized');
}
