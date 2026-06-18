# Building and Testing

## Prerequisites

- **JDK 21.** The Gradle build is pinned to a JetBrains Runtime 21 toolchain via
  [`gradle/gradle-daemon-jvm.properties`](../../gradle/gradle-daemon-jvm.properties)
  (`toolchainVendor=jetbrains`, `toolchainVersion=21`). If you build with a
  different JDK vendor, either install a JBR 21 or adjust that file locally.
- **Android SDK** with:
  - Platform **API 36** (the `compileSdk`)
  - Recent **build-tools** (e.g. `36.0.0`)
  - `platform-tools`
- A `local.properties` pointing at the SDK, e.g. `sdk.dir=/opt/android-sdk`
  (Android Studio creates this for you).

## Build variants

The app has one product-flavor dimension, `engine`, with two flavors:

| Flavor | Browser engine | Notes |
|--------|----------------|-------|
| `system` | Android WebView | Lighter; the default for local work. |
| `gecko` | GeckoView (nightly) | Pulls a large GeckoView dependency. |

Combined with `debug`/`release` you get variants like **`systemDebug`**,
`geckoDebug`, `systemRelease`, …

## Building

```shell
# Clone
git clone https://github.com/HarryDotMYx/Firefox-Android-TV
cd Firefox-Android-TV

# Assemble the system debug APK
./gradlew clean app:assembleSystemDebug
```

The APK is written to `app/build/outputs/apk/system/debug/app-system-debug.apk`.

In Android Studio, choose the **systemDebug** build variant.

## Running

Testing on a real Fire TV is recommended (see the developer guide). Connect over
the network:

```shell
adb connect <IP address>:5555
```

Then install via Android Studio or `adb install`. Only one development device
can be connected to a Fire TV at a time.

If you use an emulator, pick an **Android TV** image on **API 26+**. Press
`cmd + m` to simulate the menu button.

> Because `android.software.leanback` is no longer required, the app will also
> install on phone/tablet devices and emulators, though the UI is tuned for TV
> remote navigation.

## Unit tests

```shell
# A reasonable subset (system debug)
./gradlew testSystemDebug

# Just compile test sources
./gradlew :app:compileSystemDebugUnitTestKotlin
```

Tests run on the JVM with Robolectric. The current suite is **245 tests**.

### Coverage

```shell
./gradlew -Pcoverage jacocoDebugTestReport
```

Reports: `app/build/jacoco/jacoco<buildVariant>TestReport/html/index.html`.

## UI (instrumented) tests

1. Connect **exactly one** device (a real device is preferred — emulators fail
   some tests). More than one connected device will fail the run.
2. Run:

   ```shell
   ./gradlew connectedSystemDebugAndroidTest
   ```

## Lint & static analysis

```shell
./gradlew :app:lintSystemDebug   # Android lint (warnings are treated as errors)
./gradlew detekt                 # Kotlin static analysis
```

Lint uses a baseline at `app/lint-baseline.xml`; regenerate it if you
intentionally change the set of accepted findings.

## Pre-push hook

```shell
ln -s ../../quality/pre-push-recommended.sh .git/hooks/pre-push
# Bypass for doc-only changes:
git push <remote> --no-verify
```
