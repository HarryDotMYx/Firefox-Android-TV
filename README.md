> ⚠️ This fork (maintained by **PGMAFX**) has been modernized:
> - The app source is now **fully Kotlin** (the legacy Java was migrated/removed; only auto-generated files remain in Java).
> - Tooling and dependencies are up to date (Kotlin 2.3, Android Gradle Plugin 9, Gradle 9, **RxJava 3**, AndroidX).
> - It installs on **all Android form factors** — phones, tablets and TV — not just Android TV / Fire TV (`android.software.leanback` is no longer required). The UI is still optimized for TV remote navigation.
> - Search ships with many **worldwide engines** (Google, Bing, DuckDuckGo, Yahoo, Yandex, Baidu, Ecosia, Brave, Startpage, Wikipedia, YouTube).

# Firefox for Android TV

[![Task Status](https://github.taskcluster.net/v1/repository/HarryDotMYx/Firefox-Android-TV/master/badge.svg)](https://github.taskcluster.net/v1/repository/HarryDotMYx/Firefox-Android-TV/master/latest)
[![codecov](https://codecov.io/gh/HarryDotMYx/Firefox-Android-TV/branch/master/graph/badge.svg)](https://codecov.io/gh/HarryDotMYx/Firefox-Android-TV)

_Fast for good, just right for watching video at home. A browser for
discovering and watching web video on the big screen TV for users to install on
their Android TV devices._

[Get it on Amazon Fire TV][amazon link]

## Getting Involved
Our code is open source and we encourage all positive contributions! We love pull
requests, bug reports, ideas, (security) code reviews and other kinds of contributions.
Before you contribute, please read the [Community Participation
Guidelines](https://www.mozilla.org/en-US/about/governance/policies/participation/).

* [Guide to Contributing][contribute] (**new contributors start here!**)
* Open issues: https://github.com/HarryDotMYx/Firefox-Android-TV/issues
  * [`good first issues`][good first] | [`help wanted`][help]
  * [File a security issue][sec issue]
* Project wiki: https://github.com/HarryDotMYx/Firefox-Android-TV/wiki
* Mailing list:
[firefox-focus-public@](https://mail.mozilla.org/listinfo/firefox-focus-public)

## Build instructions
Dependencies:
- JDK 21 (the Gradle build runs on a JetBrains Runtime 21 toolchain — see `gradle/gradle-daemon-jvm.properties`)
- Android SDK with the `compileSdk` platform (API 36) and build-tools installed

1. Clone the repository:

  ```shell
  git clone https://github.com/HarryDotMYx/Firefox-Android-TV
  ```

1. Import the project into Android Studio or build on the command line:

  ```shell
  ./gradlew clean app:assembleSystemDebug
  ```

1. Make sure to select the right build variant in Android Studio: **systemDebug**

### Running
It is recommended to test directly on a Fire TV: see the [developer guide][dev guide] for more info.
You can connect with:
```shell
adb connect <IP address>:5555
```

And then install via Android Studio or adb. Only a single development device
can be connected to a Fire TV at a time. This codebase requires
**Android 8.0 / API 26** or newer (`minSdk = 26`), so devices and emulator
images older than that will reject the APK during install.

Because `android.software.leanback` is no longer required, the app also installs
on **phones and tablets** (alongside Android TV / Fire TV); the interface is
still designed for TV remote/D-pad navigation.

If using an emulator, an Android TV device image running API 26 or newer is
recommended. You can press `cmd + m` to simulate a menu button press.

### Unit Testing
To run a reasonable subset of the unit tests, we recommend:
```sh
./gradlew testSystemDebug
```

To generate code coverage reports, run:
```sh
./gradlew -Pcoverage jacocoDebugTestReport
```

Reports can be found at `app/build/jacoco/jacoco<buildVariant>TestReport/html/index.html`

### UI Testing
To run all UI tests, follow these steps

1. Connect to one device
  - Either use `adb connect` for a real device, or start an emulator instance using AVD
  - Prefer a real device (emulators will fail some tests)
  - The next step will fail if you are connected to more than one device
1. Run `./gradlew connectedSystemDebugAndroidTest` from the command line
  - Aliasing this command is recommended

### Pre-push hooks
To reduce review turn-around time, we'd like all pushes to run tests locally. We'd
recommend you use our provided pre-push hook in `quality/pre-push-recommended.sh`.
Using this hook will guarantee your hook gets updated as the repository changes.
This hook tries to run as much as possible without taking too much time.

To add it, run this command from the project root:
```sh
ln -s ../../quality/pre-push-recommended.sh .git/hooks/pre-push
```

To push without running the pre-push hook (e.g. doc updates):
```sh
git push <remote> --no-verify
```

### Release process
(See [this doc](https://github.com/HarryDotMYx/Firefox-Android-TV/blob/master/.github/ISSUE_TEMPLATE/---relman-checklist.md) 
for a description of our release process)

### Building release builds
Release builds can be built in Android Studio or via the command line:
```sh
./gradlew assembleSystemRelease # unsigned build
```

These builds will run validation checks that the build is ready for a production release. If you
do not want to run these checks (e.g. building release builds for local debugging), you can add this
argument:
```sh
./gradlew assembleSystemRelease -PnoValidate
```

#### API keys
Certain services require an API key, so you'll need to build with the key to use them in the apk.

1. To build with the API key (for services such as Sentry), add a `<project-dir>/.<service>_debug`
file with your key, for example, `<project-dir>/.sentry_dsn_debug`

    1. To enable Sentry on Debug builds, additionally replace the `isEnabled` value check in
    `SentryIntegration` value with true (upload is disabled by default in dev builds).

2. Verify the key add was successful. The gradle output is the only way to verify this (although
it won't indicate if the key is valid). You will see a message in the gradle output
indicating the key was added:

`Sentry DSN (debug): Added from /Users/mcomella/dev/moz/firefox-tv/.sentry_dsn_debug`

As opposed to:

`Sentry DSN (debug): X_X`

API services currently supported are:
* sentry_dsn
* pocket_key

##### Amazon Device Messaging (ADM) API key
We suspect **ADM access is only available in local builds for core team members** because testing
ADM requires access to a private API key that is connected to our app on the Amazon store dashboard.

To use ADM in debug builds, there must be a `<project-dir>/app/src/main/assets/api_key.txt` file
that contains the api key. It is necessary that it is in the project's assets folder.
Amazon will automatically provide an API key for production builds. See
[ADM integration doc][adm] for more details. If you're on the core team, we share debug keys: ask
another developer for access.

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

[amazon link]: https://www.amazon.com/dp/B078B5YMPD/ref=sr_1_1
[dev guide]: https://github.com/HarryDotMYx/Firefox-Android-TV/wiki/Developer-guide-and-differences-from-Android
[contribute]: https://github.com/mozilla-mobile/shared-docs/blob/master/android/CONTRIBUTING.md
[good first]: https://github.com/HarryDotMYx/Firefox-Android-TV/labels/good%20first%20issue
[help]: https://github.com/HarryDotMYx/Firefox-Android-TV/labels/help%20wanted
[sec issue]: https://bugzilla.mozilla.org/enter_bug.cgi?assigned_to=nobody%40mozilla.org&bug_file_loc=http%3A%2F%2F&bug_ignored=0&bug_severity=normal&bug_status=NEW&cf_fx_iteration=---&cf_fx_points=---&component=Security%3A%20General&contenttypemethod=autodetect&contenttypeselection=text%2Fplain&defined_groups=1&flag_type-4=X&flag_type-607=X&flag_type-791=X&flag_type-800=X&flag_type-803=X&form_name=enter_bug&groups=firefox-core-security&maketemplate=Remember%20values%20as%20bookmarkable%20template&op_sys=Unspecified&priority=--&product=Firefox%20for%20FireTV&rep_platform=Unspecified&target_milestone=---&version=unspecified
[adm]: https://developer.amazon.com/docs/adm/integrate-your-app.html#store-your-api-key-as-an-asset
