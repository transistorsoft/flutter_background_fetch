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

##### :warning: Failure to perform the step above will result in a **build error**

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
+   ext.kotlin_version = '1.3.0' // Must use 1.3.0 or higher.
+   ext {
+       compileSdkVersion   = 28                // or higher
+       targetSdkVersion    = 28                // or higher
+       appCompatVersion    = "1.1.0"           // or higher
+   }

    repositories {
        google()
        jcenter()
    }

    dependencies {
+        classpath 'com.android.tools.build:gradle:3.3.1' // Must use 3.3.1 or higher
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

# Ensure AndroidX compatibility
dependencies {
     testImplementation 'junit:junit:4.12'
-    androidTestImplementation 'com.android.support.test:runner:1.0.2'
-    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
+    androidTestImplementation 'androidx.test:runner:1.1.1'                   // or higher
+    androidTestImplementation 'androidx.test.espresso:espresso-core:3.1.1'   // or higher
}
```

## Headless Mechanism with `enableHeadless: true`

### `Flutter >= 1.12`
- If you've upgraded your Flutter SDK to `1.12` (or higher) **AND** [Upgraded Your Android Project](https://github.com/flutter/flutter/wiki/Upgrading-pre-1.12-Android-projects), there are no additional steps required &mdash; everything is now automatic.

### `Flutter < 1.12`

If you intend to use the SDK's Android *Headless* mechanism, you must perform the following additional setup:

Create either `Application.kt` or `Application.java` in the same directory as `MainActivity`.

:warning: Replace `package your.app.name` with your app's package name.  If you don't know your *package name*, you can find it at the 1st line in `MainActivity.java`.

- For `Application.kt`, use the following:

```kotlin
package your.app.name;  // <-- replace this

import com.transistorsoft.flutter.backgroundfetch.BackgroundFetchPlugin;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugins.GeneratedPluginRegistrant;

class Application : FlutterApplication(), PluginRegistry.PluginRegistrantCallback {
  override fun onCreate() {
    super.onCreate();
    BackgroundFetchPlugin.setPluginRegistrant(this);
  }

  override fun registerWith(registry: PluginRegistry) {
    GeneratedPluginRegistrant.registerWith(registry);
  }
}
```

- For `Application.java`, use the following:

```java
package your.app.name;  // <-- replace this

import com.transistorsoft.flutter.backgroundfetch.BackgroundFetchPlugin;

import io.flutter.app.FlutterApplication;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class Application extends FlutterApplication implements PluginRegistry.PluginRegistrantCallback {
  @Override
  public void onCreate() {
    super.onCreate();
    BackgroundFetchPlugin.setPluginRegistrant(this);
  }

  @Override
  public void registerWith(PluginRegistry registry) {
    GeneratedPluginRegistrant.registerWith(registry);
  }
}
```

Now edit `AndroidManifest.xml` and provide a reference to your custom `Application` class:
```xml
    <application
        android:name=".Application"
        ...
    
```

