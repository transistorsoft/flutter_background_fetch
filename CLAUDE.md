# background_fetch

Flutter plugin providing periodic background-fetch callbacks for iOS and Android, including Android headless (post-termination) execution.

**Repo:** https://github.com/transistorsoft/flutter_background_fetch
**Author:** Transistor Software
**License:** MIT

## Architecture

This is a standard Flutter plugin with platform-specific implementations:

```
lib/background_fetch.dart          # Dart API (single file) — BackgroundFetch, BackgroundFetchConfig, TaskConfig
ios/background_fetch/              # iOS plugin (Obj-C)
  Sources/background_fetch/        #   BackgroundFetchPlugin.m + include/ headers
  Package.swift                    #   SwiftPM support
android/src/main/java/.../         # Android plugin (Java)
  BackgroundFetchPlugin.java       #   FlutterPlugin entry point
  BackgroundFetchModule.java       #   Core logic (method/event channels)
  HeadlessTask.java                #   Background isolate management
example/                           # Example Flutter app
```

**Native dependencies:**
- iOS: `TSBackgroundFetch` (~> 4.2) via CocoaPods or SwiftPM
- Android: `com.transistorsoft:tsbackgroundfetch` (4.0.+) via Maven Central

## Build & Test

```bash
# Example app — Android
cd example && flutter run

# Example app — iOS
cd example && flutter run -d iphone

# iOS pod install (from example)
cd example/ios && pod install

# Dart analysis
flutter analyze

# Simulate background fetch on Android
adb shell cmd jobscheduler run -f com.transistorsoft.flutter.backgroundfetch.demo 999
```

There are no automated unit/integration tests in this repo. Verification is manual via the example app.

## iOS Notes

- **Podspec:** `ios/background_fetch.podspec` — reads version from `pubspec.yaml`
- **SwiftPM:** `ios/background_fetch/Package.swift` — depends on `transistorsoft/background-fetch` from GitHub
- **Header path:** `ios/background_fetch/Sources/background_fetch/include/background_fetch/BackgroundFetchPlugin.h`
- **Deployment target:** iOS 15.0
- CocoaPods requires `:linkage => :static` in consumer Podfiles
- Privacy manifest at `ios/background_fetch/Sources/background_fetch/PrivacyInfo.xcprivacy`

## Android Notes

- **Min SDK:** 21, **Compile SDK:** 36 (overridable via `rootProject.ext.compileSdkVersion`)
- **Java 17** required
- `safeExtGet` pattern in `build.gradle` lets consuming apps override SDK versions
- No custom Maven repository needed (published to Maven Central)

## Git Conventions

- Prefix commits: `[iOS]`, `[Android]`, `[RELEASE]`
- CHANGELOG.md updated with every release
- Version in `pubspec.yaml` is the single source of truth (podspec reads it)

## Release Checklist

1. Bump `version` in `pubspec.yaml`
2. Update `CHANGELOG.md`
3. `flutter pub publish`
