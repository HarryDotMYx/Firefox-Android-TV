# Developer guide and differences from Android

Firefox for Android TV runs on televisions and streaming sticks. That changes a
number of assumptions you may carry over from phone/tablet Android development.

## No touch — everything is focus + D-pad

- Input comes from a **remote / D-pad**, not a touchscreen. Every interactive
  element must be **focusable** and reachable by directional navigation.
- Focus order matters. Views wire `nextFocusUp/Down/Left/Right` explicitly in
  several places (e.g. the navigation overlay toolbar and URL bar).
- A software **cursor** is used for web content that expects pointer input
  (see `webrender/cursor/`).
- The manifest declares
  `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`.

## Leanback & launchers

- TV apps are launched from the **leanback launcher** and provide a banner
  (`android:banner`). The `MainActivity` declares a `LEANBACK_LAUNCHER` intent
  filter.
- This fork sets `android.software.leanback` to **`required="false"`** and also
  keeps a normal `LAUNCHER` intent filter, so the app installs on phones and
  tablets too. The experience is still optimized for TV.

## Single-session browsing

The app is built around a **single active session** (one "tab"). Mozilla's
`SessionManager` can hold multiple sessions and the navigation overlay already
renders a "tabs" channel, but multi-tab UI is not implemented. See
[`docs/architecture/multi-tab-design.md`](../architecture/multi-tab-design.md)
for a phased plan.

## Engines: `system` vs `gecko`

The `engine` product flavor selects the browser engine:

- **`system`** uses the platform **WebView** — smaller, fewer dependencies.
- **`gecko`** uses **GeckoView** — closer to desktop Firefox, much larger
  download.

Engine-agnostic code lives in `src/main`; engine-specific bits are pulled in via
flavor dependencies.

## Fire TV specifics

- **Voice / media controls** integrate with Fire TV via the Media Session API
  (`webrender/VideoVoiceCommandMediaSession`).
- **ADM (Amazon Device Messaging)** is used for push; it requires a private API
  key and is generally only testable in core-team local builds.
- YouTube gets special handling (URI detection, back handling, a grey-screen
  workaround) under `webrender/`.

## Language & toolchain

- The source is **Kotlin**. Only auto-generated files remain in Java
  (`generated/LocaleList.java`, `utils/publicsuffix/PublicSuffixPatterns.java`,
  and `PublicSuffix.java` which reads the latter's package-private members).
- Reactive streams use **RxJava 3** + RxKotlin/RxAndroid.
- Min SDK **26**, compile SDK **36**, JDK **21**.

## Handy tips

- Press `cmd + m` in an emulator to send the **menu** button.
- Prefer a **real device** for UI tests — several fail on emulators.
- The locale system swaps the app language independently of the system locale;
  see `components/locale/LocaleManager`.
