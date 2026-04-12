# iOS Setup

## Cocoapods

If you're not using *Swift Package Manager*, you **must** adjust the `use_frameworks!` declaration to add the following property in your `ios/Podfile`:

`use_frameworks!` should already exist &mdash; just append `:linkage => :static`:

```ruby
target 'Runner' do
  use_frameworks! :linkage => :static   # <-- append :linkage => :static
  .
  .
  .
end
```

:warning: If you *don't* add the above line, you're going to get the following error when running `flutter build ios`

```console
-> Installing background_fetch (1.5.0)
      - Running pre install hooks
    [!] The 'Pods-Runner' target has transitive dependencies that include statically linked binaries:
    (/Users/chris/workspace/background-geolocation/flutter/tmp/fetch_foo/ios/Pods/TSBackgroundFetch/TSBackgroundFetch.xcframework)
```

## Configure Background Capabilities

- Select the root of your project.  Select **Capabilities** tab.  Enable **Background Modes** and enable the following mode:

- [x] Background fetch
- [x] Background processing (Only if you intend to use `BackgroundFetch.scheduleTask`)

![](https://dl.dropboxusercontent.com/s/9vik5kxoklk63ob/ios-setup-background-modes.png?dl=1)


## Configure `Info.plist`
1.  Open your __`Info.plist`__ and add the key *"Permitted background task scheduler identifiers"*

![](https://dl.dropboxusercontent.com/s/t5xfgah2gghqtws/ios-setup-permitted-identifiers.png?dl=1)

2.  Add the **required identifier `com.transistorsoft.fetch`**.

![](https://dl.dropboxusercontent.com/s/kwdio2rr256d852/ios-setup-permitted-identifiers-add.png?dl=1)

3.  If you intend to execute your own [custom tasks](#executing-custom-tasks) via **`BackgroundFetch.scheduleTask`**, you must add those custom identifiers as well.  For example, if you intend to execute a custom **`taskId: 'com.transistorsoft.customtask'`**, you must add the identifier **`com.transistorsoft.customtask`** to your *"Permitted background task scheduler identifiers"*, as well.

:warning: Your custom  task identifiers **MUST** be prefixed with `com.transistorsoft.`.

```dart
BackgroundFetch.scheduleTask(TaskConfig(
  taskId: 'com.transistorsoft.customtask',
  delay: 60 * 60 * 1000  //  In one hour (milliseconds) 
));

```

## Flutter 3.41+ UIScene lifecycle

If your app has adopted Flutter's UIScene lifecycle, the plugin application lifecycle fallback can be delivered too late for `BGTaskScheduler`.
iOS requires all launch handlers to be registered before application launch finishes.  In that case, call
`BackgroundFetchPlugin.registerBackgroundTasksIfNeeded()` from your real `AppDelegate.didFinishLaunchingWithOptions` before returning from `super`.

```swift
import Flutter
import UIKit
import background_fetch

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    BackgroundFetchPlugin.registerBackgroundTasksIfNeeded()
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
  }
}
```

<!--
## Privacy Manifest



Apple now requires apps provide a [Privacy Manifest for "sensitive" APIs](https://developer.apple.com/documentation/bundleresources/privacy_manifest_files/describing_use_of_required_reason_api?language=objc) which could be abused for "fingerprinting" a user for malicious marketing activity.

If your app does not yet have a *Privacy Manifest* (__`PrivacyInfo.xcprivacy`__), create one now:

<details>
    <summary>ℹ️ Click here for detailed instructions...</summary>

- In XCode, __`File -> New -> File...`__:

![](https://dl.dropboxusercontent.com/scl/fi/n28028i3fbrxd67u491w2/file-new-PrivacyInfo.png?rlkey=sc7s1lyy8fli2c1hz2cfa4cpm&dl=1)

- Be sure to enable your `Targets: [x] YourApp`:

![](https://dl.dropboxusercontent.com/scl/fi/pmbfn5jypvns6r5pyhnui/file-new-PrivacyInfo-targets.png?rlkey=epvjffar23bxgyi9xax9ys40i&dl=1)


</details>


It's best to edit this file's XML manually.
- :open_file_folder: `ios/PrivacyInfo.xcprivacy`
- Add the following block within the `NSPrivacyAccessedAPITypes` `<array>` container:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">

<plist version="1.0">
<dict>
    <key>NSPrivacyAccessedAPITypes</key>
    <array>
        <dict>
            <key>NSPrivacyAccessedAPIType</key>
            <string>NSPrivacyAccessedAPICategoryUserDefaults</string>

            <key>NSPrivacyAccessedAPITypeReasons</key>
            <array>
                <string>CA92.1</string>
            </array>
        </dict>        
    </array>
</dict>
</plist>
```

-->
