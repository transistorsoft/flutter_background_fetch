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

## `android/gradle.properties`

Ensure your app is [migrated to use AndroidX](https://flutter.dev/docs/development/packages-and-plugins/androidx-compatibility).

:open_file_folder: `android/gradle.properties`:

```diff
org.gradle.jvmargs=-Xmx1536M
+android.enableJetifier=true
+android.useAndroidX=true
```

## `android/build.gradle`

As an app grows in complexity and imports a variety of 3rd-party modules, it helps to provide some key **"Global Gradle Configuration Properties"** which all modules can align their requested dependency versions to.  `background_fetch` **is aware** of these variables and will align itself to them when detected.

:open_file_folder: `android/build.gradle`:

```diff
buildscript {
+   ext.kotlin_version = '1.3.72'               // or latest
+   ext {
+       compileSdkVersion   = 30                // or latest
+       targetSdkVersion    = 30                // or latest
+       appCompatVersion    = "1.1.0"           // or latest
+   }

    repositories {
        google()
        jcenter()
    }

    dependencies {
+        classpath 'com.android.tools.build:gradle:3.3.1' // Must use 3.3.1 or higher.  4.x is fine.
    }
}

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

## `android/app/build.gradle`

In addition, you should take advantage of the *Global Configuration Properties* **yourself**, replacing hard-coded values in your `android/app/build.gradle` with references to these variables:

:open_file_folder: `android/app/build.gradle`:

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


