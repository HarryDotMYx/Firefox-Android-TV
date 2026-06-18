# Architecture

A high-level map of how the app is put together. For the formal decisions, see
the ADRs under [`docs/architecture/`](../architecture/).

## Pattern: MVVM

The app follows **MVVM** (see
[`docs/architecture/adr-0003-mvvm-architecture.md`](../architecture/adr-0003-mvvm-architecture.md)):

- **View** — `Activity`/`Fragment` + view binding.
- **ViewModel** — exposes observable state; no Android framework dependencies
  where avoidable.
- **Repo / UseCase** — data and business logic.

State is exposed as **RxJava 3** `Observable`s (and some `LiveData`).

## Dependency injection: ServiceLocator

DI is done with a hand-rolled **`ServiceLocator`**
([ADR-0001](../architecture/adr-0001-dependency-injection.md)), reachable from a
`Context` via the `serviceLocator` extension. It lazily builds and holds
singletons such as `sessionRepo`, `pinnedTileRepo`, `searchEngineManager`,
`screenController`, `fxaRepo`, etc.

ViewModels are created through **`ViewModelFactory`**, wrapped by
**`FirefoxViewModelProviders.of(...)`**, so constructor arguments can be
injected:

```kotlin
val vm = FirefoxViewModelProviders.of(this).get(ToolbarViewModel::class.java)
```

## Screens & navigation

`ScreenController` + `ScreenControllerStateMachine` decide which fragment is
visible. The active screens are:

```
NAVIGATION_OVERLAY · WEB_RENDER · SETTINGS · FXA_PROFILE
```

The state machine maps **menu** and **back** presses to transitions
(`ADD_OVERLAY`, `REMOVE_OVERLAY`, `SHOW_BROWSER`, `EXIT_APP`, …). For example,
pressing back from `WEB_RENDER` adds the overlay; pressing back in the overlay
either navigates back or exits.

## Sessions

`session/SessionRepo` wraps Mozilla's `SessionManager` and exposes browser
`State` (URL, loading, back/forward enabled, …) and the list of sessions as
observables. `SessionObserverHelper` keeps the repo in sync with the manager.

## The navigation overlay

`navigationoverlay/` contains the home/overlay UI:

- `NavigationOverlayFragment` — the overlay screen and its channels (pinned
  tiles, news, sports, music, tabs).
- `ToolbarViewModel` / `ToolbarUiController` — the top toolbar and URL bar.
- `InlineAutocompleteEditText` (in `widget/`) — the URL input with inline
  domain autocomplete.

## Web rendering

`webrender/` hosts the engine view and browser chrome: `WebRenderFragment`,
`EngineViewCache`, the software cursor (`cursor/`), error pages (`ErrorPage`),
and Fire TV media-session/voice handling.

## Search

`search/SearchEngineManagerFactory` builds a `SearchEngineManager` from bundled
OpenSearch plugins in `assets/searchplugins/`. `UrlUtils.createSearchUrl`
honours the user's selected engine. See [Search Engines](Search-Engines).

## Localization

`components/locale/` manages switching the app language independently of the
system locale. `LocaleManager` (a singleton) persists the choice, corrects
incoming `Configuration`s, and exposes the current locale; `Locales` and the
`LocaleAware*` base classes apply it to activities/fragments/the application.

## Telemetry

`telemetry/` wraps event/ping telemetry. Some of it still uses the legacy
telemetry library (marked deprecated, pending a Glean migration).
