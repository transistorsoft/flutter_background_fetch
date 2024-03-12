# Android Setup

## `AndroidManifest`

Flutter seems to have a problem with 3rd-party Android libraries which merge their own `AndroidManifest.xml` into the application, particularly the `android:label` attribute.

##### :open_file_folder: `android/app/src/main/AndroidManifest.xml`:

```diff
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
+    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.helloworld">

    <application
+        tools:replace="android:label"
         android:name="io.flutter.app.FlutterApplication"
         android:label="flutter_background_geolocation_example"
         android:icon="@mipmap/ic_launcher">
</manifest>

```

##### ⚠️ Failure to perform the step above will result in a **build error**

```
Execution failed for task ':app:processDebugManifest'.
> Manifest merger failed : Attribute application@label value=(hello_world) from AndroidManifest.xml:17:9-36
    is also present at [tslocationmanager-2.13.3.aar] AndroidManifest.xml:24:18-50 value=(@string/app_name).
    Suggestion: add 'tools:replace="android:label"' to <application> element at AndroidManifest.xml:15:5-38:19 to override.
```

## :open_file_folder: `android/build.gradle`
- Add the following **required** `maven` repo urls:

```diff
allprojects {
    repositories {
        google()
        mavenCentral()
+       // [required] background_fetch
+       maven { url "${project(':background_fetch').projectDir}/libs" }
    }
}
```

- #### If you're using `flutter >= 3.19.0` ([New Android Architecture](https://docs.flutter.dev/release/breaking-changes/flutter-gradle-plugin-apply)):


```diff
+ext {
+    compileSdkVersion   = 33                // or higher / as desired
+    targetSdkVersion    = 33                // or higher / as desired
+    appCompatVersion    = "1.4.2"           // or higher / as desired
+ }

```

- #### Otherwise for `flutter < 3.19.0` (Old Android Architecture):

```diff
buildscript {
    ext.kotlin_version = '1.3.72'               // or latest
+   ext {
+       compileSdkVersion   = 33                // or higher / as desired
+       targetSdkVersion    = 33                // or higher / as desired
+       appCompatVersion    = "1.4.2"           // or higher / as desired
+   }
}


```

## :open_file_folder: `android/app/build.gradle`

:exclamation: __DO NOT OMIT ANY OF THE FOLLOWING CHANGES__ :exclamation:

```diff
android {
+   compileSdkVersion rootProject.ext.compileSdkVersion
    .
    .
    .
    defaultConfig {
        .
        .
        .
+       targetSdkVersion rootProject.ext.targetSdkVersion
    }
}
```

## Precise event-scheduling with `forceAlarmManager: true`:

**Only** If you wish to use precise scheduling of events with __`forceAlarmManager: true`__, *Android 14 (SDK 34)*, has restricted usage of ["`AlarmManager` exact alarms"](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms).  To continue using precise timing of events with *Android 14*, you can manually add this permission to your __`AndroidManifest`__.  Otherwise, the plugin will gracefully fall-back to "*in-exact* `AlarmManager` scheduling":

:open_file_folder: In your `AndroidManifest`, add the following permission (**exactly as-shown**):

```xml
  <manifest>
      <uses-permission android:minSdkVersion="34" android:name="android.permission.USE_EXACT_ALARM" />
      .
      .
      .
  </manifest>
```
:warning: It has been announced that *Google Play Store* [has plans to impose greater scrutiny](https://support.google.com/googleplay/android-developer/answer/13161072?sjid=3640341614632608469-NA) over usage of this permission (which is why the plugin does not automatically add it).


