# SDUI: cross-client parity — screen chrome, error escape, tokens, wire assets, actions

Unify server-owned screen chrome (`contentInsets`, composited app bar), fix fetch-failure
navigation traps on all clients, align navigate/actions to schema wire names, and close
gaps in TabGroup, team logos, subscribe-hero contrast, and relative asset URLs.

---

## Summary

- **Server** emits `contentInsets`, prepends token-based app-bar `AtomicComposite` sections,
  and uses CDN team logos in demo/kitchen-sink surfaces; subscribe hero uses dark-surface
  color tokens instead of theme-inverted labels.
- **Schema** adds `Screen.contentInsets`, documents `TabGroupData.ui`, and clarifies
  `Screen.title` as composition-only input.
- **Android / iOS / Web** apply `contentInsets`, retain `shellScreen` on fetch failure for
  bottom-nav + back/home escape, resolve layout spacing via tokens in client chrome, and
  share `WireUrlResolver` for `/sdui-demo/…` paths.
- **Actions** use canonical `webUrl` (legacy `fallbackUrl` stripped at example load and
  rejected by strict decode); mutate/refresh fields preserved through adapters.

---

## Schema & examples

- `schema/sdui-schema.json`
  - Add optional `Screen.contentInsets` (`Spacing` with semantic layout tokens).
  - Add optional `TabGroupData.ui` (`AtomicElement` for tab header row).
  - Document `Screen.title` as legacy composition input for app-bar prepend (not client-rendered).
- Regenerated models (Kotlin / Swift / TypeScript / Java via `make codegen`).
- `schema/examples/*.json` and synced `ios/Tests/SduiCoreTests/Fixtures/*.json`:
  - Expanded kitchen-sink / for-you / game-detail payloads.
  - `fallbackUrl` → `webUrl` on navigate actions.

---

## Server

### Screen composition utilities (`SduiUtils`)

- `ensureScreenContentInsets()` — default feed padding (`spacing.md` horizontal, `spacing.lg` bottom) when omitted.
- `prependAppBarHeaderIfNeeded()` — first-section `AtomicComposite` app bar from `title` / `parentUri`; strips top-level `title`.
- `applyTabDestinationNavigation()` — bottom-nav tabs: wire `navigation`, remove `title`.
- `loadExampleJsonFile()` + `normalizeLegacyNavigateFields()` — rewrite `fallbackUrl` → `webUrl` when loading cached examples.
- `teamLogoUrl()` — CDN PNG URLs for team marks (used across composers).

### Composers

- **Demo / kitchen sink** — game card and schedule team logos use `SduiUtils.teamLogoUrl()` instead of `/sdui-demo/team.svg` placeholders.
- **Watch / demo subscribe** — hero and tier copy use `TEXT_ON_DARK_MEDIA`, `LABEL_ACCENT_GOLD_ON_DARK`, `SURFACE_TIER_ON_DARK`, `TEXT_DIM_ON_DARK` (readable on fixed dark gradients in light app theme).
- **Game detail, boxscore, live, schedule, scoreboard, for-you, home** — `contentInsets`, app-bar prepend, and/or `parentUri` where applicable.
- **`AtomicCompositeBuilder`** — app-bar header builder; team logo fallback via `teamLogoUrl`.

### Color tokens (`ColorTokens`)

- Rename/clarify `TEXT_ON_BRAND` → `TEXT_ON_DARK_MEDIA` (`nba.label-dark.primary`).
- Add subscribe-on-dark tokens: `LABEL_ACCENT_GOLD_ON_DARK`, `SURFACE_TIER_ON_DARK`, `TEXT_DIM_ON_DARK`.

### Tests

- `ForYouSectionIdDerivationTest` — asserts `contentInsets` on composed screens.
- `ExampleWireContractTest` (new) — example JSON wire contract checks.

### Config

- `server/.env.example` (new) — local env template.

---

## Android (`sdui-core` + app)

### Fetch failure escape

- `SduiScreenViewModel.shellScreen` — last successful `currentScreen` kept on error.
- `SduiScreenContent` — error/upgrade UI with **Go back** / **Home** + **Retry**; `BackHandler` for system back.
- `GameDetailScreen` — `SduiNavigationShell` driven by `shellScreen.navigation`; `onBack` → bootstrap URI.
- `activity-compose` dependency for `BackHandler`.

### Layout & tokens

- Apply `screen.contentInsets` on success feed (`LayoutTokenResolver`).
- Error/upgrade chrome spacing via `token:nba.spacing.{sm,lg}` (no hardcoded `16.dp` / `8.dp`).

### Renderers & infrastructure

- **`IconTokenResolver`** — Material icon map; fix `List` / import clashes; `material-icons-core`.
- **`ColorTokenResolver`** — parse `rgba(r,g,b,a)` wire literals (subscribe tier card backgrounds).
- **`TabGroupRenderer`** — token-based tab colors; strict `TabGroupData` adapter (optional `ui` header).
- **`WireUrlResolver`** (new) + `LocalSduiWireAssetBaseUrl` — absolutize `/sdui-demo/…` against API base.
- Atomic renderers (`AtomicBox`, `AtomicButton`, `AtomicImage`, `AtomicText`, `AtomicContainer`,
  `AtomicOverlayContainer`) — layout tokens, wire asset base, interaction/action wiring.
- **`SduiNavigationShell`** — shell navigation polish.
- **`SectionContainer`** / **`SectionUiAdapters`** — adapter and inset alignment.

### Actions & state

- `ActionHandler` — navigate via `targetUri` / `webUrl`; tests for legacy `fallbackUrl` rejection.
- `StateManager` / tests — schema-aligned mutate paths.
- **`ActionExecutor`** — `selectActions(trigger)` for onActivate/onTap/onSubmit/etc.; atomics route non-activate triggers through batch executor.
- **`FormRenderer`** — submit dispatches via `LocalActionExecutor` / `dispatchActions` (schema `onSubmit` trigger).
- **`build.gradle.kts`** — `androidx.activity:activity-compose` (BackHandler), `material-icons-core` (IconTokenResolver).

---

## iOS (`SduiCore` + demo)

### Fetch failure escape

- `SduiScreenViewModel.shellScreen` — retained on successful load.
- `ScreenShell` — `SduiNavigationShell` wraps loading/error/upgrade/loaded; `ErrorView` + `UpgradeRequiredView`
  with back/home + retry; `navigateBack()` (pop → `parentURI` → `sduiNavigateHome`).
- `NavCoordinator` — `sduiNavigateHome` environment for root 404 escape.
- `SduiDemoApp` — sets `sduiNavigateHome` to pop stack and reload bootstrap endpoint.

### Layout & tokens

- `edgeInsets(from: screen.contentInsets)` on scroll feed.
- `ClientChromeSpacing` — error/upgrade UI uses `LayoutTokenResolver` (`spacing.sm/md/lg`).

### Renderers & infrastructure

- **`WireUrlResolver`** (new) + tests — same contract as Android/web.
- **`TabGroupView`** — strict adapter, optional `ui` header row, token colors.
- **`AtomicImageView`**, **`AtomicButtonView`**, **`AtomicOverlayContainerView`** — wire URLs, overlays.
- **`BindRefResolver`**, **`RenderingHelpers`**, **`LayoutTokenResolver`** — binding and inset helpers.
- **`ActionDispatcher`**, **`ScreenState`** — action/state alignment with schema.
- **`AtomicTriggerDispatchTests`** — `onSubmit` batch-executor coverage.

---

## Web

### Fetch failure escape

- `useSduiScreen` — `shellScreen` state; clear `screen` on endpoint change; keep shell on failure.
- `App.tsx` — bottom nav from `shellScreen`; error/upgrade panel with Go back/Home + Retry;
  `bootstrapUri` for home escape; no full-screen trap on `error && !screen`.

### Layout & tokens

- Apply `screen.contentInsets` on main feed.
- Error/loading chrome spacing via `resolveLayoutScalar('token:nba.spacing.*')`.

### Renderers & infrastructure

- **`WireUrlResolver`** + `WireAssetBaseUrlProvider` — resolve demo/static paths.
- **`atomicActionHandlers.ts`** (new) — shared activate/action execution for atomics.
- **`TabGroup`** — strict adapter, token tab padding, optional `ui` header.
- **`SectionContainer`**, atomics (`AtomicBox`, `AtomicButton`, `AtomicContainer`, `AtomicImage`,
  `AtomicText`, `AtomicOverlayContainer`) — tokens, wire assets, `getActivateActions` cleanup.
- **`sectionUiAdapters`** — drop legacy `section.data.actions`; tests added.
- **`Form`** — native `<form onSubmit>`; submit as action batch (`onSubmit` trigger).
- **`BoxscoreTable`** — remove client-invented margin/radius chrome (`width: 100%` only).
- **`ActionHandler`** — `webUrl` / `targetUri`; legacy `fallbackUrl` ignored in tests.
- **`sectionActions`** — trigger selection aligned with atomic handlers.
- **`LayoutTokenResolver`** — minor registry/resolver updates + tests.
- **`atomicSizing.test.ts`** (new) — atomic sizing contract tests (Vitest config: see known failures).
- **`AtomicOverlayContainer.test`**, **`overlayScrimWireContract.test`** — overlay wire contract tweaks.
- **`vite.config`** — dev proxy/static paths for demo assets.
- **`package-lock.json`** — prune extraneous optional platform deps from lockfile.

---

## Docs & tooling

- `docs/sdui-setup.md` — expanded setup (Android emulator flags, env, workflows).
- `docs/client-implementors-contract.md`, `docs/glossary.md`, `docs/SDUI_Technical_Proposal_v2.md`,
  `docs/sdui-requirements-summary.md` — `contentInsets`, `webUrl`, TabGroup `ui`, app-bar model.
- `Makefile` — lighter default emulator flags (`EMU_MEMORY`, `EMU_CORES`, `EMU_GPU`); improved `dev-android` boot wait.
- `README.md` — link to `docs/sdui-setup.md`; clarify `make dev` / `ios-run`.
- `.gitignore` — ignore `server/bin/`.

---

## Tests

| Area | Notes |
|------|--------|
| Android | `ActionHandlerTest`, `StateManagerTest`, `LayoutTokenResolverTest`, `WireUrlResolverTest` |
| iOS | Fixture sync; `WireUrlResolverTests`; existing atomic trigger tests |
| Web | `ActionHandler.test`, `LayoutTokenResolver.test`, `sectionUiAdapters.test`, `TabGroup.test`, `WireUrlResolver.test` |
| Server | `ForYouSectionIdDerivationTest`, `ExampleWireContractTest` |

**Known pre-existing / environment failures (not introduced by this change set):**

- Android `SduiRepositoryRefreshTransportTest` NPE (`RequestEnvelopeBuilder.kt:189`) on some environments.
- Web `atomicSizing.test.ts` — Vitest/oxc parse error on JSX in `.ts` file (rename to `.tsx` or fix config).
- iOS `make ios-test` requires full Xcode (not Command Line Tools only).

---

## Known limitations / follow-ups

- **`nba://video/playoffs-bos-mia-g3`** (and similar) may still 404 until a server route/composer exists;
  clients now allow escape (back/home/retry) instead of trapping the user.
- **Subscribe hero** server tokens landed; verify all subscribe surfaces in `WatchComposer` / `DemoScreenComposer`
  use `TEXT_ON_DARK_MEDIA` consistently.
- Sweep remaining client hardcoded px/dp outside wire-driven `layoutHints` where semantics exist in the token registry.

---

## Verification

```bash
make codegen
make server-test    # may hit unrelated failures
make android-test   # see known failure above
make web-test       # exclude or fix atomicSizing.test.ts if needed
make ios-test       # requires Xcode
```

Manual: load kitchen sink → game cards show CDN team logos; navigate to invalid URI → bottom nav +
Go back/Home; subscribe hero badge readable in dark mode on Android.

---

## Staged coverage audit (121 files)

All pending changes are staged (`git add -A`). Every path maps to a section above:

| Category | Staged paths (count) | Changelog section |
|----------|----------------------|-------------------|
| Schema + examples | `schema/sdui-schema.json`, `schema/examples/*` (9) | Schema & examples |
| Codegen models | `android/.../SduiModels.kt`, `ios/.../SduiModels.swift`, `web/.../SduiModels.ts` (3) | Schema & examples |
| Server Java + tests | `server/src/main/...` (12), `server/src/test/...` (2), `server/.env.example` (1) | Server |
| Android core + app | `android/sdui-core/**` (28), `android/app/.../GameDetailScreen.kt` (1) | Android |
| iOS core + demo + fixtures | `ios/Sources/**` (16), `ios/SduiDemo/**` (1), `ios/Tests/**` (12) | iOS |
| Web src + lockfile | `web/src/**` (32), `web/package-lock.json`, `web/vite.config.ts` (2) | Web |
| Docs | `docs/*.md` (5) | Docs & tooling |
| Repo meta | `Makefile`, `README.md`, `.gitignore`, `COMMIT_MESSAGE.md` (4) | Docs & tooling / this file |

**No unstaged or untracked files remain** after staging.
