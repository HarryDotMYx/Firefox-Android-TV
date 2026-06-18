# FAQ

### Which devices does the app run on?

Anything on **Android 8.0 / API 26** or newer. Because
`android.software.leanback` is no longer required, it installs on **phones and
tablets** in addition to **Android TV / Fire TV**. The UI is still designed for
remote/D-pad navigation.

### Why won't it install on my old Fire TV / emulator?

The `minSdk` is **26**. Devices or emulator images older than Android 8.0 will
reject the APK.

### Which JDK do I need to build?

**JDK 21.** The Gradle daemon is configured for a JetBrains Runtime 21 toolchain
(`gradle/gradle-daemon-jvm.properties`). If you use a different vendor, install a
JBR 21 or adjust that file locally.

### `system` vs `gecko` — which build should I use?

Use **`systemDebug`** for everyday work (Android WebView, small and fast). The
`gecko` flavor uses GeckoView and downloads a much larger dependency.

### Is the project Java or Kotlin?

**Kotlin.** The legacy Java was migrated to Kotlin (or removed when it was dead
code). Only auto-generated files remain in Java: `generated/LocaleList.java`,
`utils/publicsuffix/PublicSuffixPatterns.java`, and `PublicSuffix.java` (which
reads the generated file's package-private members that Kotlin cannot access).

### Can I have multiple tabs?

Not yet. The data layer (`SessionManager`) supports it and the overlay already
lists sessions, but there is no multi-tab UI. A phased plan is in
[`docs/architecture/multi-tab-design.md`](../architecture/multi-tab-design.md).

### How do I change the search engine?

The browser bundles many engines and `UrlUtils.createSearchUrl` honours
`Settings.defaultSearchEngineName`. There is no TV picker UI yet — see
[Search Engines](Search-Engines).

### Why does the keyboard pop up when I press back?

By design. Returning to the overlay from a web page focuses the URL bar and opens
the soft keyboard so you can immediately type a new address or query.

### The unit tests pass but lint fails — why?

Lint runs with warnings-as-errors and uses a baseline (`app/lint-baseline.xml`).
If the baseline has drifted, some pre-existing findings can resurface. Fix the
finding or regenerate the baseline.

### Where do I report bugs or security issues?

Open a [GitHub issue](https://github.com/HarryDotMYx/Firefox-Android-TV/issues),
or file a security issue via the link in the
[README](https://github.com/HarryDotMYx/Firefox-Android-TV#getting-involved).
