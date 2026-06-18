# Firefox for Android TV

[![codecov](https://codecov.io/gh/HarryDotMYx/Firefox-Android-TV/branch/master/graph/badge.svg)](https://codecov.io/gh/HarryDotMYx/Firefox-Android-TV)

A browser for discovering and watching web video on the big screen — fast, and
just right for watching video at home. Originally built for Amazon Fire TV /
Android TV.

> **This fork (maintained by [PGMAFX](https://github.com/HarryDotMYx)) has been
> modernized.** See [What's new](#whats-new) below.

[Get it on Amazon Fire TV][amazon link]

---

## What's new

This fork brings the codebase up to date and broadens where it runs:

- **Fully Kotlin source.** The legacy Java was migrated to idiomatic Kotlin (or
  removed where it was dead code). Only auto-generated files remain in Java.
- **Modern toolchain & dependencies** — Kotlin 2.3, Android Gradle Plugin 9,
  Gradle 9, **RxJava 3** (migrated off the end-of-life RxJava 2), and AndroidX
  throughout.
- **Installs on every Android form factor.** `android.software.leanback` is no
  longer required, so the app installs on **phones and tablets** as well as
  Android TV / Fire TV. The UI is still designed for TV remote / D-pad
  navigation.
- **Worldwide search.** Search ships with many engines out of the box: Google,
  Bing, DuckDuckGo, Yahoo, Yandex, Baidu, Ecosia, Brave, Startpage, Wikipedia
  and YouTube.
- **Quality-of-life fixes**, e.g. the soft keyboard now opens automatically on
  the URL bar when returning from a web page.

> A multi-tab design plan lives in
> [`docs/architecture/multi-tab-design.md`](docs/architecture/multi-tab-design.md).

## Requirements

- **Android 8.0 / API 26** or newer (`minSdk = 26`). Older devices and emulator
  images will reject the APK during install.
- Works on phones, tablets, Android TV and Fire TV.

## Getting involved

This code is open source and positive contributions are welcome — pull
requests, bug reports, ideas and (security) code reviews. Before contributing,
please read the [Community Participation Guidelines][participation].

- [Guide to Contributing][contribute] (**new contributors start here!**)
- Open issues: https://github.com/HarryDotMYx/Firefox-Android-TV/issues
  - [`good first issues`][good first] · [`help wanted`][help]
  - [File a security issue][sec issue]
- Project wiki: https://github.com/HarryDotMYx/Firefox-Android-TV/wiki

## Build instructions

**Dependencies**

- **JDK 21** — the Gradle build runs on a JetBrains Runtime 21 toolchain (see
  [`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties)).
- **Android SDK** with the `compileSdk` platform (API 36) and build-tools
  installed.

**Steps**

1. Clone the repository:

   ```shell
   git clone https://github.com/HarryDotMYx/Firefox-Android-TV
   ```

2. Open the project in Android Studio, or build from the command line:

   ```shell
   ./gradlew clean app:assembleSystemDebug
   ```

3. In Android Studio, select the **systemDebug** build variant.

### Running

It is recommended to test directly on a Fire TV — see the [developer
guide][dev guide]. Connect with:

```shell
adb connect <IP address>:5555
```

Then install via Android Studio or `adb`. Only a single development device can
be connected to a Fire TV at a time. If using an emulator, an Android TV image
running API 26 or newer is recommended; you can press `cmd + m` to simulate a
menu button press.

### Unit testing

Run a reasonable subset of the unit tests:

```shell
./gradlew testSystemDebug
```

Generate code-coverage reports:

```shell
./gradlew -Pcoverage jacocoDebugTestReport
```

Reports are written to
`app/build/jacoco/jacoco<buildVariant>TestReport/html/index.html`.

### UI testing

1. Connect exactly **one** device (a real device is preferred — emulators fail
   some tests). The next step fails if more than one device is connected.
2. Run:

   ```shell
   ./gradlew connectedSystemDebugAndroidTest
   ```

### Pre-push hooks

To run tests locally before pushing, use the provided hook. From the project
root:

```shell
ln -s ../../quality/pre-push-recommended.sh .git/hooks/pre-push
```

To push without running the hook (e.g. doc-only updates):

```shell
git push <remote> --no-verify
```

### Release builds

Release builds can be produced in Android Studio or from the command line:

```shell
./gradlew assembleSystemRelease            # unsigned build
./gradlew assembleSystemRelease -PnoValidate  # skip production-readiness checks
```

(See the [release checklist][release checklist] for the full process.)

#### API keys

Some services require an API key built into the APK.

1. Add a `<project-dir>/.<service>_debug` file with your key, e.g.
   `<project-dir>/.sentry_dsn_debug`.
   - To enable Sentry on debug builds, also set the `isEnabled` check in
     `SentryIntegration` to `true` (upload is disabled by default in dev
     builds).
2. The Gradle output is the only way to verify the key was added (it does not
   check validity):

   ```
   Sentry DSN (debug): Added from /…/.sentry_dsn_debug
   ```

   versus:

   ```
   Sentry DSN (debug): X_X
   ```

Supported services: `sentry_dsn`, `pocket_key`.

##### Amazon Device Messaging (ADM)

Testing ADM requires a private API key tied to the app on the Amazon store
dashboard, so it is generally only available in local builds for core team
members. To use ADM in debug builds, place the key in
`<project-dir>/app/src/main/assets/api_key.txt`. Amazon provides an API key
automatically for production builds. See the [ADM integration doc][adm] for
details.

## License

```
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/
```

[amazon link]: https://www.amazon.com/dp/B078B5YMPD/ref=sr_1_1
[dev guide]: https://github.com/HarryDotMYx/Firefox-Android-TV/wiki/Developer-guide-and-differences-from-Android
[contribute]: https://github.com/mozilla-mobile/shared-docs/blob/master/android/CONTRIBUTING.md
[participation]: https://www.mozilla.org/en-US/about/governance/policies/participation/
[good first]: https://github.com/HarryDotMYx/Firefox-Android-TV/labels/good%20first%20issue
[help]: https://github.com/HarryDotMYx/Firefox-Android-TV/labels/help%20wanted
[release checklist]: https://github.com/HarryDotMYx/Firefox-Android-TV/blob/master/.github/ISSUE_TEMPLATE/---relman-checklist.md
[sec issue]: https://bugzilla.mozilla.org/enter_bug.cgi?product=Firefox%20for%20FireTV&component=Security%3A%20General
[adm]: https://developer.amazon.com/docs/adm/integrate-your-app.html#store-your-api-key-as-an-asset
