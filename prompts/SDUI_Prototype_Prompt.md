# Prompt: SDUI Platform Prototype — Multi-Platform

## Context

You are building a working prototype of a Server-Driven UI (SDUI) platform. The goal is to demonstrate every key architectural concept from the SDUI Platform Execution Roadmap in a single, buildable project with **two client consumers**: Android (Kotlin/Jetpack Compose) and Web (React/TypeScript). This is not a production system — it is a proof of concept that validates the end-to-end architecture so the team can see, touch, and evaluate every layer before committing to a full multi-platform build.

The prototype should be simple enough for one or two engineers to build in 2–3 weeks, but architecturally honest — every layer that exists in the production roadmap should be represented, even if the implementation is minimal. Cut scope by reducing the number of components and screens, not by removing architectural layers.

---

## What To Build

### The Surfaces: Scoreboard + Game Detail

Build two SDUI-powered screens that demonstrate multi-screen navigation and mixed refresh strategies:

#### Screen 1: Scoreboard (Games List)

The landing screen showing today's NBA games. Each game is a tappable row that navigates to the Game Detail screen. The server composes one `ScoreboardHeader` section per game, with live games receiving real-time score updates via Ably SSE.

#### Screen 2: Game Detail

A single-game focus screen with multiple sections demonstrating different data freshness requirements on the same screen.

The Game Detail screen includes these sections:

1. **ScoreboardHeader** — displays two team logos, team names/tricodes, and the current score. During a live game, this section updates in real time via Ably SSE (`{gameId}:linescore` channel). During final or pre-game states, it is static.

2. **StatLine list** — a vertical list of player stat rows (player name, stat category, stat value). Uses polling refresh (every 30 seconds during live games, static otherwise). The server can direct the client to poll either the SDUI endpoint or a direct NBA CDN URL for lower latency.

3. **Row** — a responsive layout section that renders two child StatLine sections side-by-side above a breakpoint (600dp) and stacked vertically below it. Demonstrates server-controlled responsive layout.

4. **ContentRail** — a horizontal scrolling strip of ContentCard items (thumbnail image, headline text, subhead text, action). Fully static, delivered with the initial response.

5. **TabGroup** — a tabbed section (e.g., "Box Score" and "Play-by-Play") where tab selection is managed by client-side state mutation. Selecting a tab swaps visible content without a server round-trip. The Box Score tab contains dynamically-populated team StatLine sections with full player stats, using poll refresh.

6. **PromoBanner** — a promotional banner with title, description, optional image, and a call-to-action. Demonstrates a static editorial section.

---

### Layer 1: The SDUI Schema (JSON)

Define a JSON schema that implements the two-tier primitive system from the roadmap.

**Atomic primitives:** Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional. Each has typed properties (e.g., Container supports direction/gap/padding/alignment; Text supports content/variant/weight/color/maxLines; Button supports label/variant/action/icon/disabled).

**Semantic section types (8 total):** ScoreboardHeader, StatLine, ContentCard, ContentRail, TabGroup, PromoBanner, GameCard, Row. Each is referenced by type name in the SDUI response — the renderer resolves the semantic type, not the atomic decomposition.

**Schema hierarchy — Screen → Section → Component:**

- A **Screen** contains: id, schemaVersion, title, analyticsId, traceId, defaultRefreshPolicy, navigation, state, and an ordered list of Sections.
- Each **Section** contains: id, type, refreshPolicy, dataBindings, actions, subsections, analyticsId, padding, backgroundColor, and a polymorphic data payload.
- **Actions** are declarative objects. Each has a trigger (onTap, onLongPress, onVisible, onSwipe) and a type (navigate, analytics, mutate, refresh, dismiss). Actions can be attached at the Section level or embedded in section data for backward compatibility.
- **Subsections** are nested interaction targets within a Section, each with an id and its own actions list.
- **State** is a map of key-value pairs at the screen level. Mutate actions modify state keys. TabGroup and Conditional components reference state keys.

**Typed data models per section type:** The schema defines typed data models for each section's payload: ScoreboardHeaderData, StatLineListData, ContentCardData, ContentRailData, TabGroupData, PromoBannerData, GameCardData, GameLeadersData, RowData. These are referenced in the Section's `data` property.

**Refresh policy per section:**

- `"refresh": { "type": "static" }` — no updates after initial load.
- `"refresh": { "type": "poll", "intervalMs": 30000, "url": "...", "dataPath": "..." }` — periodic re-fetch. Optionally includes a direct `url` (e.g., NBA CDN boxscore endpoint) and a `dataPath` for extracting nested data from the response.
- `"refresh": { "type": "sse", "channel": "{gameId}:linescore" }` — subscribe to real-time Ably channel.

**Data binding configuration per section:** Each section that receives real-time updates declares field-level bindings — which incoming message fields map to which data properties. For example, the ScoreboardHeader SSE binding maps `$.homeTeam.score` → `homeTeam.score`, `$.gameStatusText` → `gameStatusText`, etc. The data binding also supports a `stringKeys` map for i18n — translation keys for fields that are updated from real-time sources, enabling client-side localization resolution.

**Schema versioning:** Include a `schemaVersion` field in the Screen response. Clients send their supported schema version in the `X-Schema-Version` request header. The composition service responds with a compatible response.

Write the schema definition as a JSON Schema document (`schema/sdui-schema.json`). Also create a wrapper schema (`schema/sdui-all-types.json`) that references all type definitions for comprehensive code generation. Include example SDUI responses under `schema/examples/` for different game states (live, pre, final).

---

### Layer 2: Code Generation Pipeline (Schema → Typed Models)

This layer demonstrates the critical principle that **the schema is the single source of truth** and platform models are derived artifacts.

**What to build:**

A code generation step that reads the JSON Schema and produces typed models for multiple platforms:

1. **Java POJOs (primary — for Android):** Use [jsonschema2pojo](https://www.jsonschema2pojo.org/) as a Gradle plugin. It reads `schema/sdui-all-types.json` and produces Java POJOs with Jackson annotations. These are the canonical generated models consumed by the Android renderer.

2. **TypeScript interfaces (for Web):** Use [quicktype](https://quicktype.io/) to generate TypeScript interfaces from `schema/sdui-schema.json`. The web renderer imports these.

3. **Swift structs (demo only):** Use quicktype to generate Swift models, demonstrating the multi-platform promise even though no iOS renderer is built.

**Codegen commands:**

```bash
# Full codegen — both quicktype (TS/Swift/Kotlin) and jsonschema2pojo (Java)
make codegen

# Individual steps
cd codegen && bash generate.sh              # quicktype for TS, Swift, Kotlin
cd codegen && ./gradlew generateJsonSchema2Pojo  # Java POJOs
```

**Generated code is never hand-edited.** Generated outputs live in `codegen/output/` (quicktype) and `codegen/build/generated-sources/` (jsonschema2pojo). The Android project references these as source dependencies.

**Hand-written Kotlin models alongside generated Java POJOs:** The Android prototype uses hand-written Kotlin data classes (`SduiModels.kt`) for the core SDUI container types (Screen, Section, RefreshPolicy, DataBinding, Subsection, Spacing) with Jackson annotations. The generated Java POJOs are used for typed section data payloads (ScoreboardHeaderData, TeamData, StatLineListData, ContentCardData, ContentRailData, etc.). This hybrid approach gives idiomatic Kotlin ergonomics for the container layer while leveraging codegen for the data models.

---

### Layer 3: The Composition Service (Backend)

Build a Spring Boot (Java) backend service that composes SDUI responses from live NBA data.

**Endpoints:**

1. `GET /sdui/game-detail/{gameId}` — returns the full SDUI Screen response. Accepts query parameters:
   - `gameState` (pre, live, final) — controls refresh policies and fallback behavior.
   - `variant` (A, B, C, D) — controls section composition for A/B testing.
   - `X-Schema-Version` header — schema version negotiation.

2. `GET /sdui/scoreboard` — returns today's games as a list of ScoreboardHeader sections. Accepts:
   - `variant` (A, E, F) — controls scoreboard-specific variants.
   
3. `GET /stats/{gameId}` — returns player stat data for the polling section.

4. `GET /api/games/today` — returns a simplified list of today's games for client navigation UI.

5. `GET /health` — health check.

**Live data integration:** The composition service fetches live game data from the NBA Stats API (`https://stats-trafficcop-prod.nba.com/v0/api/stats/boxscore`) using an `Ocp-Apim-Subscription-Key` header (from `.env`). When live data is unavailable, it falls back to static example responses.

**Variant system:**

Game Detail variants:
- **A** (default): Standard section order.
- **B**: Swaps ContentRail and TabGroup positions.
- **C**: Removes PromoBanner and top-level StatLine sections (minimal view).
- **D**: Adds a second ContentRail ("Trending Videos") after the existing one.

Scoreboard variants:
- **A** (default): Game cards only.
- **E** ("Promo"): Adds a PromoBanner at position 0.
- **F** ("Promo + Rail"): Adds a PromoBanner at position 0, and a ContentRail after the second game card when there are more than two games.

**Real-time channel configuration:** For live games, the server sets the ScoreboardHeader section's refresh policy to `sse` with the Ably channel name `{gameId}:linescore` and configures data bindings for homeTeam.score, awayTeam.score, gameStatusText, and period.

**Direct polling URLs:** For StatLine and TabGroup sections during live games, the server can set the poll refresh policy's `url` field to a direct NBA CDN endpoint (e.g., `https://cdn.nba.com/static/json/liveData/boxscore/boxscore_{gameId}.json`) with a `dataPath` for JSON extraction. This lets the client poll the data source directly, bypassing the composition service for lower latency.

**Trace ID:** Every response includes a unique `traceId` in both the response body and the `X-Trace-Id` header for end-to-end observability.

---

### Layer 4: The Android Renderer (Kotlin/Jetpack Compose)

Build an Android application with a strict two-module architecture:

#### Module: `sdui-core` (library) — reusable SDUI infrastructure

Everything screen-agnostic that could ship as a library:

1. **Models** (`core/models/`)
   - `SduiModels.kt` — Hand-written Kotlin data classes for SduiScreen, SduiSection, RefreshPolicy, DataBinding, DataBindingPath, Subsection, Spacing, Navigation, NavigationItem. Uses Jackson annotations (`@JsonProperty`, `@JsonIgnoreProperties`).
   - `SectionDataModels.kt` — Hand-written Kotlin data classes for complex section data (TabGroupData, etc.).
   - `GeneratedConverters.kt` — Utility to bridge between hand-written Kotlin models and generated Java POJOs (e.g., `actionToSduiAction`).
   - Generated Java POJOs from jsonschema2pojo (referenced as source dependency).

2. **Network layer** (`core/data/`)
   - `SduiRepository` — Fetches SDUI responses via OkHttp. Supports game detail, scoreboard, stats, raw JSON (for direct URL polling), and today's games endpoints. Sends `X-Schema-Version` header.
   - `AblyChannelManager` — Manages Ably real-time connections with JWT token authentication via `authUrl` (NBA identity server at `https://identity.nba.com/rttoken`). Delivers linescore updates as Kotlin Flows.
   - `DataBindingResolver` — Applies incoming real-time messages to section data using the section's binding configuration. Supports JSONPath-like source paths (e.g., `$.homeTeam.score`) and dot-notation target paths. Handles missing paths gracefully (logs warning, preserves previous value). Acknowledges `stringKeys` for future i18n resolution.

3. **State management** (`core/state/`)
   - `StateManager` — Screen-level state holder backed by `MutableStateFlow<Map<String, Any>>`. Supports initialize, set, get, remove, clear operations. Compose reactivity drives automatic re-rendering.
   - `ActionHandler` — Dispatches SduiAction objects to handlers by type: navigate (returns URI), analytics (logs event), mutate (updates StateManager), refresh (triggers re-fetch), dismiss (closes overlays). Returns sealed `ActionResult` types.

4. **Section renderers** (`core/renderer/`)
   - `SectionRouter` — A `when` statement mapping section type strings to composable functions. Unknown types are skipped with a warning log. Supports all 8 section types.
   - `sections/` — One composable per section type:
     - `ScoreboardHeaderRenderer` — Team logos (via Coil AsyncImage with SVG support), tricodes, scores, game status. Clickable with primary action.
     - `StatLineRenderer` — Vertical list of player stat rows.
     - `ContentCardRenderer` — Single content card.
     - `ContentRailRenderer` — Horizontal scrolling strip of ContentCards.
     - `TabGroupRenderer` — Tabbed navigation using screen state for active tab. Tab selection fires mutate actions.
     - `PromoBannerRenderer` — Promotional banner with title, description, image, CTA.
     - `GameCardRenderer` — Game card with team logos, tricodes, scores, leaders, and visual state (PRE/LIVE/FINAL).
     - `RowRenderer` — Responsive layout rendering child sections side-by-side or stacked based on breakpoint.
   - `adapters/SectionUiAdapters.kt` — Maps raw section data to typed UI models (e.g., `mapScoreboardHeader` deserializes to ScoreboardHeaderUiModel via Jackson ObjectMapper converting to generated Java POJOs). Resolves primary actions from section-level or data-level actions.
   - `interactions/SectionInteractions.kt` — Shared helpers for resolving actions from sections and subsections. Prioritizes schema-level `section.actions` over legacy `section.data.actions`.

5. **Screen orchestration** (`core/screen/`)
   - `SduiScreenViewModel` — Generic ViewModel handling: fetch (game detail or scoreboard), pull-to-refresh, action dispatch, polling setup (with direct URL support), Ably SSE subscription, linescore data binding application. Manages its own lifecycle cleanup.
   - `SduiScreenContent` — Headless composable rendering Loading/Error/Success states. Success state renders sections in a `LazyColumn` with `PullToRefreshBox`. Does NOT include app chrome.
   - `SduiScreenUiState` — Sealed interface (Loading, Success, Error).

6. **Configuration** (`core/config/`)
   - `SduiScreenConfig` — Library-level config: baseUrl, ablyTokenUrl, screenId, gameState, variant, enableAbly, enablePolling.

**Image loading:** Uses Coil with `coil-svg` for SVG team logo support. The `SduiApplication` class registers `SvgDecoder.Factory()` as a custom `ImageLoaderFactory`.

#### Module: `app` — thin application shell

Only app-specific code:

- `SduiApplication` — Custom Application class implementing `ImageLoaderFactory` for Coil SVG support.
- `SduiConfig` — App-level config with presets (`scoreboard()`, `gameDetail(gameId)`) and `toScreenConfig()` converter. Defines `ScreenType` enum (SCOREBOARD, GAME_DETAIL).
- `MainActivity` — Single-activity navigation. Manages screen type transitions, variant selection, game picker dropdown. Resets variant to "A" when switching between screen types.
- `GameDetailScreen` — Thin composable wrapper adding TopAppBar, game picker, and screen-specific variant chips around `SduiScreenContent`.
- `GameDetailViewModel` — Thin ViewModel wrapper mapping `SduiConfig` → `SduiScreenConfig` and delegating to `SduiScreenViewModel`.
- `Theme.kt` — App-specific Material 3 theming.

**Module boundary rules:**
- Never add generic SDUI infrastructure into `app`.
- Never reference `app` types from `sdui-core`.
- `app` ViewModels must delegate to `SduiScreenViewModel`.
- `app` Composables must use `SduiScreenContent` for the section list.

---

### Layer 5: The Web Renderer (React/TypeScript)

Build a React web application that renders the same SDUI responses as the Android client, proving the multi-platform contract.

**Architecture:**

1. **Hooks** (`hooks/`)
   - `useSduiScreen` — Fetches and manages SDUI screen state.
   - `useRefreshPolicy` — Manages per-section refresh policies (static, poll, sse).

2. **Components** (`components/`)
   - `SectionRouter.tsx` — Routes section types to renderer components. Wraps sections with live data in `LiveSectionWrapper`.
   - `LiveSectionWrapper` — Applies refresh policies and data bindings to sections.
   - `TopNavigationBar` — Navigation chrome.
   - `sections/` — One component per section type: ScoreboardHeader, StatLine, ContentRail, TabGroup, PromoBanner, GameCard, Row.

3. **Runtime** (`runtime/`)
   - `AblyClient.ts` — Ably real-time client integration.
   - `ActionHandler.ts` — Action dispatch (navigate, analytics, mutate, refresh, dismiss).
   - `DataBindingApplier.ts` — Applies data bindings from SSE/poll updates.

4. **Adapters/Utils**
   - `sectionUiAdapters.ts` — Maps section data to UI models (ScoreboardHeaderUiModel, GameCardUiModel, etc.).
   - `sectionActions.ts` — Resolves section and subsection actions with same priority logic as Android.

5. **Main App** (`App.tsx`)
   - Variant selector with screen-specific options (game-detail: A/B/C/D; scoreboard: A/E/F).
   - Screen routing between scoreboard and game detail.
   - Resets variant to "A" on screen type change.
   - State management and action handling.

---

### Layer 6: Observability (Lightweight)

- The composition service assigns a unique `traceId` to every response, included in both the response body and `X-Trace-Id` header.
- Clients log the traceId on response receipt, data channel establishment, data binding resolution, and action dispatch.
- Ably connection/channel state changes are logged with channel names and failure reasons.

---

## Project Structure

```
sdui-prototype/
├── schema/
│   ├── sdui-schema.json              # JSON Schema (single source of truth)
│   ├── sdui-all-types.json           # Wrapper schema for comprehensive codegen
│   └── examples/                     # Example SDUI responses by game state
├── codegen/
│   ├── generate.sh                   # quicktype: Kotlin, Swift, TypeScript
│   ├── build.gradle.kts              # jsonschema2pojo: Java POJOs
│   └── output/
│       ├── kotlin/                   # quicktype Kotlin output
│       ├── swift/                    # quicktype Swift output (demo only)
│       └── typescript/               # quicktype TypeScript output (web uses)
├── server/
│   └── src/main/java/com/nba/sdui/
│       ├── SduiServerApplication.java
│       ├── config/WebConfig.java
│       ├── controller/
│       │   ├── SduiController.java
│       │   └── GamesController.java
│       └── service/
│           ├── SduiCompositionService.java
│           └── StatsApiClient.java
├── android/
│   ├── app/                          # Thin application shell
│   │   └── src/main/java/com/nba/sdui/app/
│   │       ├── SduiApplication.kt
│   │       ├── SduiConfig.kt
│   │       ├── MainActivity.kt
│   │       └── ui/
│   │           ├── GameDetailScreen.kt
│   │           ├── GameDetailViewModel.kt
│   │           └── theme/Theme.kt
│   ├── sdui-core/                    # Reusable SDUI library
│   │   └── src/main/java/com/nba/sdui/core/
│   │       ├── config/SduiScreenConfig.kt
│   │       ├── data/
│   │       │   ├── SduiRepository.kt
│   │       │   ├── AblyChannelManager.kt
│   │       │   └── DataBindingResolver.kt
│   │       ├── models/
│   │       │   ├── SduiModels.kt
│   │       │   ├── SectionDataModels.kt
│   │       │   └── GeneratedConverters.kt
│   │       ├── renderer/
│   │       │   ├── SectionRouter.kt
│   │       │   ├── adapters/SectionUiAdapters.kt
│   │       │   ├── interactions/SectionInteractions.kt
│   │       │   └── sections/
│   │       │       ├── ScoreboardHeaderRenderer.kt
│   │       │       ├── StatLineRenderer.kt
│   │       │       ├── ContentCardRenderer.kt
│   │       │       ├── ContentRailRenderer.kt
│   │       │       ├── TabGroupRenderer.kt
│   │       │       ├── PromoBannerRenderer.kt
│   │       │       ├── GameCardRenderer.kt
│   │       │       └── RowRenderer.kt
│   │       ├── screen/
│   │       │   ├── SduiScreenViewModel.kt
│   │       │   ├── SduiScreenContent.kt
│   │       │   ├── SduiScreenUiState.kt
│   │       │   └── SduiNavigationShell.kt
│   │       └── state/
│   │           ├── StateManager.kt
│   │           └── ActionHandler.kt
│   └── gradle/libs.versions.toml
├── web/
│   └── src/
│       ├── App.tsx
│       ├── main.tsx
│       ├── adapters/sectionUiAdapters.ts
│       ├── components/
│       │   ├── SectionRouter.tsx
│       │   ├── LiveSectionWrapper.tsx
│       │   ├── TopNavigationBar.tsx
│       │   └── sections/
│       │       ├── ScoreboardHeader.tsx
│       │       ├── StatLine.tsx
│       │       ├── ContentRail.tsx
│       │       ├── TabGroup.tsx
│       │       ├── PromoBanner.tsx
│       │       ├── GameCard.tsx
│       │       └── Row.tsx
│       ├── hooks/
│       │   ├── useSduiScreen.ts
│       │   └── useRefreshPolicy.ts
│       ├── runtime/
│       │   ├── AblyClient.ts
│       │   ├── ActionHandler.ts
│       │   └── DataBindingApplier.ts
│       └── utils/sectionActions.ts
├── docs/                             # Technical proposal & requirements
├── Makefile                          # codegen, dev, dev-server, dev-web
└── README.md
```

---

## What This Prototype Proves

| Roadmap Concept | How the Prototype Demonstrates It |
|---|---|
| Two-tier primitive system (atomic + semantic) | Schema defines both tiers; renderers map semantic types to platform-native UI |
| Code generation from single schema source | jsonschema2pojo generates Java POJOs; quicktype generates TypeScript, Swift, Kotlin from one JSON Schema; both clients consume generated models |
| Screen → Section → Component hierarchy | Full SDUI response uses the hierarchy; both renderers walk it identically |
| Multi-screen SDUI composition | Scoreboard and Game Detail are both server-composed; navigation between them is action-driven |
| Per-section refresh policies (static, poll, SSE) | Three strategies coexist on one screen; server controls which strategy each section uses |
| Flexible polling (SDUI endpoint vs direct URL) | Server directs client to poll either the composition service or a raw data feed for lower latency |
| Declarative data binding | Ably linescore messages mapped to section data via server-defined binding config |
| i18n-aware data bindings | `stringKeys` on DataBinding provide translation keys for real-time-updated fields |
| Section-level actions + subsections | Actions defined on Section objects; subsections enable granular interaction targets |
| Action system (navigate, analytics, mutate, refresh, dismiss) | Tab switches use mutate; game cards use navigate; all actions are dispatched through shared infrastructure |
| Client-side state management | TabGroup state managed by screen-level state map; Compose/React reactivity drives re-rendering |
| Schema version negotiation | Client sends version header; server includes version in response |
| Graceful handling of unknown types | Unknown section types are skipped by both renderers without crash |
| A/B variant composition | Server returns different section ordering/composition based on variant parameter — different variants for different screen types |
| Responsive server-controlled layout | Row section renders children side-by-side or stacked based on server-defined breakpoint |
| Multi-platform rendering from same contract | Android and Web renderers both consume the same SDUI response and render equivalent UI |
| Observability via trace ID | Trace ID flows from server through every client-side operation in logs |
| Module boundary discipline (Android) | sdui-core is screen-agnostic library; app is a thin shell — proves the SDUI layer is reusable |

---

## What This Prototype Intentionally Does NOT Build

- **iOS renderer** — Swift models are generated to prove multi-platform codegen, but no iOS app is built.
- **Production aggregation layer** (GraphQL, API gateway) — the composition service is called directly.
- **Figma integration** — design tokens are hardcoded in renderers.
- **Visual editing / schema playground** — out of scope.
- **Production deployment, feature flagging, rollback** — out of scope.
- **Full i18n resolution** — `stringKeys` are acknowledged in data binding but not yet resolved to translations.
- **Response caching** — Room is a dependency but full cache-first rendering is not yet implemented.
- **Contract tests** — test infrastructure (JUnit 5, MockK, Mockito) is configured but tests are not yet written.
- **Auth, rate limiting, production error handling** — the server relies on local `.env` for API keys.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Server | Spring Boot 3.x, Java 21, Jackson |
| Android | Kotlin 2.1.0, Jetpack Compose (BOM 2024.12.01), Min SDK 24 |
| Web | React 18, TypeScript, Vite |
| Real-time | Ably 1.2.40 (JWT auth via `authUrl`) |
| Image Loading | Coil 2.7.0 + coil-svg |
| Networking | OkHttp 4.12.0 |
| JSON | Jackson (Android/Server), native fetch (Web) |
| Codegen (Java) | jsonschema2pojo (Gradle plugin) |
| Codegen (TS/Swift/Kotlin) | quicktype CLI |
| Caching | Room 2.6.1 (dependency configured, not yet implemented) |
| Testing | JUnit 5, MockK 1.13.13, Mockito 5.14.2 (configured, tests pending) |

---

## External APIs

| API | Endpoint | Auth |
|---|---|---|
| Stats Boxscore | `GET https://stats-trafficcop-prod.nba.com/v0/api/stats/boxscore?gameId={gameId}` | `Ocp-Apim-Subscription-Key` header (from `.env`) |
| NBA CDN Boxscore | `GET https://cdn.nba.com/static/json/liveData/boxscore/boxscore_{gameId}.json` | None |
| Ably Token | `GET https://identity.nba.com/rttoken` | None |
| Ably Channel | `{gameId}:linescore` | JWT from token endpoint |

---

## Build Priorities

If time is constrained, build in this order — each step produces a working demo that builds on the previous:

1. **Schema + codegen pipeline + static response + section router rendering all section types** — proves the core SDUI contract end-to-end. The schema produces the models, the server produces the response, both clients render the screen. Show the generated Java, TypeScript, and Swift models side-by-side. This is the single most important demo step.

2. **Add the action system + state manager** — TabGroup becomes interactive. Game cards navigate to game detail. This proves the declarative action/state model.

3. **Add Ably SSE for ScoreboardHeader** — live scores update in real time. This proves per-section refresh policies and data binding.

4. **Add polling for StatLine + TabGroup** — a second real-time strategy on the same screen, including direct URL polling to NBA CDN. This proves mixed refresh and server-controlled polling targets.

5. **Add Scoreboard screen + multi-screen navigation** — games list with navigation actions to game detail. Proves multi-screen SDUI composition.

6. **Add variant system, PromoBanner, Row layout, screen-specific variants** — these demonstrate composition flexibility and responsive layout without renderer changes.

7. **Add response caching, contract tests, trace ID logging** — operational concerns that round out the prototype.

Each step is independently demo-able. Stop at any step and you have a working prototype that proves progressively more of the architecture.

---

## Running the Prototype

```bash
# Start everything
make dev                # Opens Terminal sessions for server and web

# Or individually
cd server && ./gradlew bootRun    # Server on http://localhost:8080
cd web && npm run dev             # Web on http://localhost:3000

# Android: open android/ in Android Studio, run on emulator
# (connects to http://10.0.2.2:8080)

# Regenerate models after schema changes
make codegen
```
