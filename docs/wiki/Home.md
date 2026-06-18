# Firefox for Android TV — Wiki

Welcome to the project wiki. Firefox for Android TV is a browser for
discovering and watching web video on the big screen, originally built for
Amazon Fire TV / Android TV. This fork (maintained by
[PGMAFX](https://github.com/HarryDotMYx)) has been modernized — fully Kotlin
source, current tooling, and install support for **all Android form factors**.

## Pages

- **[Building and Testing](Building-and-Testing)** — set up the toolchain and
  run builds & tests.
- **[Developer guide and differences from Android](Developer-guide-and-differences-from-Android)** —
  what makes a TV browser different (remote/D-pad focus, leanback, no touch).
- **[Architecture](Architecture)** — how the app is structured (MVVM, DI,
  sessions, screens).
- **[Search Engines](Search-Engines)** — how bundled search engines work and
  how to add one.
- **[FAQ](FAQ)** — common questions.

## Quick facts

| | |
|---|---|
| Min SDK | 26 (Android 8.0) |
| Compile SDK | 36 |
| Language | Kotlin (only generated files remain Java) |
| Build | Gradle 9 · AGP 9 · Kotlin 2.3 · JDK 21 |
| Reactive | RxJava 3 |
| Flavors | `system` (WebView) · `gecko` (GeckoView) |
| Form factors | Phone · Tablet · Android TV · Fire TV |

> ℹ️ These pages live in the repository under `docs/wiki/` so they are
> version-controlled. To publish them to the GitHub Wiki tab, copy the files
> into the `*.wiki.git` repository (the page file name becomes the page title).
