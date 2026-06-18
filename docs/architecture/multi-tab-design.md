# Design Plan: Multi-Tab Support for Firefox for Android TV

Status: **Proposal** (no code changes yet)

Author: generated design plan
Date: 2026-06-17

## 1. Goal

Allow the user to have multiple open tabs (sessions) at once and switch between
them, instead of the current single-active-session model. The user should be
able to:

- Open a new tab (blank / home).
- Open a link in a new tab.
- See a list of open tabs and switch to any of them.
- Close a tab.
- Have the currently selected tab restored on app resume.

This is a **large feature** and must be validated on real Android TV / Fire TV
hardware because it heavily touches remote (D-pad) focus navigation.

## 2. Current state (what already exists)

The groundwork is partially present, which lowers the cost:

| Component | File | Relevance |
|-----------|------|-----------|
| `SessionManager` (Mozilla AC) | injected via `ServiceLocator` | Already capable of holding **multiple** sessions; today we only drive one. |
| `SessionRepo` | `session/SessionRepo.kt` | Exposes `session` (singular, the selected one) plus a `_sessions: BehaviorSubject<List<Session>>` list that is already maintained. |
| `SessionObserverHelper` | `session/SessionObserverHelper.kt` | Observes the `SessionManager` and pushes updates into `SessionRepo`. |
| Tabs channel | `navigationoverlay/NavigationOverlayViewModel.kt` (`tabsChannel`, `TileSource.TABS`) | The navigation overlay **already renders a channel of open sessions** as tiles, with `id = session.id`. Today selecting one is not wired to switch tabs. |
| `EngineViewCache` | `webrender/EngineViewCache.kt` | Caches the `EngineView`. Currently assumes a single engine view. |
| `ScreenController` / `ScreenControllerStateMachine` | root package | Drives which fragment is visible (`WEB_RENDER`, `NAVIGATION_OVERLAY`, ...). |

So the data layer is ~70% there; the gaps are **tab selection / creation / close
actions**, **per-tab EngineView management**, and **UI affordances**.

## 3. Proposed architecture

### 3.1 Session/tab model (data layer)

1. Treat the AC `SessionManager` as the single source of truth for the tab list
   and the selected tab. Stop assuming a single session in `SessionRepo`.
2. Add use cases to `SessionRepo` (delegating to AC `TabsUseCases` /
   `SessionManager`):
   - `addTab(url: String? = null, selected: Boolean = true)`
   - `selectTab(sessionId: String)`
   - `removeTab(sessionId: String)`
   - `removeAllTabs()`
3. Expose:
   - `selectedSession: Observable<Session>` (replaces the implicit single `session`).
   - `tabs: Observable<List<Session>>` (already present as `_sessions`).
   - `selectedTabId: Observable<String>`.
4. `SessionObserverHelper` should also observe **selection changes** and per-tab
   state (URL, title, loading, canGoBack) for the *selected* tab only, to keep
   the toolbar driven by the active tab.

### 3.2 EngineView management (the hardest part)

The current `EngineViewCache` returns one `EngineView`. Multi-tab needs one
rendering surface per visible tab. Two options:

- **Option A — Single EngineView, swap session (recommended first step).**
  Keep one `EngineView` and call `engineView.render(selectedSession.engineSession)`
  when the selected tab changes. AC supports re-binding an `EngineView` to a
  different `EngineSession`. This avoids holding many heavy WebView/GeckoView
  instances in memory (important on low-RAM Fire TV sticks). Background tabs are
  "frozen" (their `EngineSession` persists state) and re-rendered on selection.
- **Option B — One EngineView per tab.** Higher memory cost; risky on TV
  hardware. Not recommended initially.

Decision: **Option A.** `EngineViewCache` becomes `getEngineView()` returning the
shared view; `WebRenderFragment` re-renders it against the selected session on
selection change.

### 3.3 UI / interaction

1. **Tabs channel becomes interactive.** The existing tabs channel in the
   navigation overlay already lists sessions. Wire tile click →
   `SessionRepo.selectTab(tile.id)` then `ScreenController.showBrowserScreen()`.
2. **New tab affordance.** Add a "＋ New tab" toolbar button (or a tile at the
   front of the tabs channel) → `SessionRepo.addTab()` and focus the URL bar
   (reusing the existing auto-keyboard-on-focus behavior).
3. **Close tab affordance.** Reuse the existing tile-removal mechanism
   (`DefaultChannel.removeTileEvents`, already wired for pinned tiles) for the
   tabs channel → `SessionRepo.removeTab(id)`.
4. **Open-link-in-new-tab.** Long-press / context action on a link, or a toolbar
   toggle. Optional for v1.
5. **Tab counter** in the toolbar showing the number of open tabs.

### 3.4 Back / exit behavior

Update `ScreenControllerStateMachine.getNewStateBackPress`:

- In `WEB_RENDER`: webview back first (unchanged).
- In `NAVIGATION_OVERLAY` with `canGoBack == false`:
  - If more than one tab is open → do **not** exit; instead select the previous
    tab or stay in the overlay.
  - If only one tab is open → `EXIT_APP` (unchanged).

This is the main behavioral change and must be tested carefully on-device.

## 4. Telemetry

Add events (Glean / existing telemetry): `tab_opened`, `tab_closed`,
`tab_switched`, and a `tab_count` metric sampled at session end. Mirror the
existing pattern in `telemetry/TelemetryIntegration.kt`.

## 5. Rollout plan (incremental, each independently shippable)

1. **Data layer**: introduce multi-session use cases in `SessionRepo` + observe
   selection. No UI change; default behavior identical (one tab).
   - ✅ **Done:** `SessionRepo.addSession(url)` creates a new session, adds it to
     the `SessionManager` (selected), and returns it — the missing "new tab"
     primitive. Covered by `SessionRepoTest`. Tab *switching* already exists via
     `SessionRepo.selectSession` / `ScreenController.selectSession`, and the
     overlay already renders open sessions in its tabs channel.
   - ✅ **Done:** `SessionRepo.removeSession(session)` removes a session from the
     `SessionManager` (the "close tab" primitive). Covered by `SessionRepoTest`.
   - ⏭️ Still to do: expose the selected session as an observable (currently the
     selected session is read on demand via `sessionManager.selectedSession`).
2. **EngineView swap**: re-render selected session (Option A). Verify single-tab
   parity on-device.
3. **Tab switching**: make the existing tabs channel select tabs.
4. **New tab + close tab** affordances.
5. **Back/exit** state-machine change.
6. **Open-in-new-tab + tab counter + telemetry** (polish).

Steps 1–2 are invisible refactors that de-risk the rest. Ship and test each on
hardware before proceeding.

## 6. Risks & open questions

- **Memory** on low-end Fire TV sticks — Option A mitigates but background tab
  count should be capped (e.g., max N tabs, evict oldest engine session).
- **Focus navigation** — TV D-pad focus order across a growing tabs channel
  needs careful `nextFocusXxx` wiring and on-device testing.
- **State restoration** — AC `SessionManager` snapshot persistence must be
  enabled so tabs survive process death.
- **YouTube/`VideoVoiceCommandMediaSession`** assumes one active session; audit
  for multi-tab interactions (media should pause on tab switch).
- **Testing**: requires a physical device / emulator with leanback; cannot be
  validated in a headless CI-only environment.

## 7. Effort estimate

Rough order: **2–4 weeks** of focused work including on-device QA, dominated by
steps 2 (EngineView) and 5 (back/exit + focus navigation), not the data layer.
