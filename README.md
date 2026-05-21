# SDUI Prototype

Server-Driven UI prototype demonstrating server-composed screens with real-time updates across Android, iOS, and Web. Includes For You, Scoreboard, Game Detail, Watch, Live, Leaders, Boxscore, and demo screens — all driven by a single JSON schema.

## Start Here

| I want to… | Start with |
|------------|------------|
| **Build a new client** (iOS, Flutter, TV, desktop) | [Client Implementor's Contract](docs/client-implementors-contract.md) — platform-agnostic blueprint with build phases, pseudocode algorithms, and conformance checklist |
| **Set up local tooling / prerequisites** | [Development Setup](docs/sdui-setup.md) — prerequisites, dependency bootstrap, secrets, and Makefile device overrides |
| **Extend the Android client** | [android/sdui-core/](android/sdui-core/) — renderers, state, data binding. See [Section Types](#section-types-9-in-schema-8-permanent--atomiccomposite) below for what exists |
| **Extend the Web client** | [web/src/](web/src/) — React components, hooks, runtime. Same section types reference |
| **Extend the iOS client** | [ios/Sources/SduiCore/](ios/Sources/SduiCore/) — SwiftUI renderers, state, data binding. Same section types reference |
| **Add a new server-composed screen** | [server/src/](server/src/) — add a composer, register an endpoint. Zero client changes needed |
| **Add a new section type** | Read [AGENTS.md](AGENTS.md) first — most things should be `AtomicComposite`. Only add a section renderer if client-owned state is required |
| **Understand the schema** | [schema/sdui-schema.json](schema/sdui-schema.json) — the contract. Hit `curl http://localhost:8080/v1/sdui/demos` for a live 42-section example |
| **Understand the rules** | [AGENTS.md](AGENTS.md) — development rules that govern all SDUI work |
| **See what's built vs. what's gap** | [Requirements Summary §10](docs/sdui-requirements-summary.md) — status matrix |

## Design Philosophy

**The server controls _what_ appears and _in what order_. Platform teams control _how_ each component looks, feels, and animates natively.**

Five principles guide every decision:

1. **Schema is source of truth.** `schema/sdui-schema.json` defines the contract. Run `make codegen` after changes. Generated models are never hand-edited.
2. **Atomic for layout, semantic for domain logic.** Atomic primitives (`Container`, `Text`, `Image`, etc.) handle server-composed layout with zero client logic. Semantic section types (`BoxscoreTable`, `TabGroup`) exist only when client-owned state or complex interaction is required. Default to atomic; promote to semantic only when you must.
3. **No hardcoded URLs or screen-type enums on clients.** Every screen is a generic `fetchScreen(endpoint)`. Endpoints are resolved from server-provided URIs (`nba://` → `/v1/sdui/`). New screens require zero client code.
4. **Graceful degradation everywhere.** Unknown section types, unknown atomic elements, and unknown action types are skipped with a log — never crash. Stale cached responses are better than blank screens.
5. **Decision checklist before adding client code:** Can the server compose it? Can a schema/action change solve it? Can it be an `AtomicComposite`? Only if none of those work, add a client renderer.

### Dual-Layer Rendering Model

The architecture uses two complementary rendering layers:

- **Section layer** — Named domain renderers (`BoxscoreTable`, `TabGroup`, `Form`, etc.) with client-owned state (sort, scroll position, form input). Routed by `SectionRouter`.
- **Atomic layer** — 12 atomic element types (`Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`, `DisplayGrid`, `OverlayContainer`, `SectionSlot`, `LiveClock`) with zero client business logic. Routed by `AtomicRouter`.
- **Bridge** — `AtomicComposite` section type: `SectionRouter` delegates to `AtomicRouter`. `SectionSlot` element: `AtomicRouter` delegates back to `SectionRouter` (e.g., embed an `AdSlot` inside an atomic layout).

Use semantic sections when client-side state or interaction is needed. Use atomic composition when the server can fully describe the layout.

## Architecture

**Shared schema, platform-aware composition.** The SDUI schema is universal — all platforms share the same vocabulary of section types, action types, and data models. The composition service produces per-platform-family responses tailored to each surface's information density, interaction model, and layout.

```
Schema (shared) → Codegen (per-platform models) → Composition (per-platform-family) → Renderers (native)
```

## Project Structure

```
sdui-prototype/
├── schema/                     # JSON Schema (source of truth)
│   ├── sdui-schema.json        # Schema definition
│   ├── sdui-all-types.json     # Wrapper for comprehensive codegen
│   └── examples/               # Example SDUI responses
├── codegen/                    # Code generation pipeline
│   ├── build.gradle.kts        # jsonschema2pojo (Java POJOs)
│   └── generate.sh             # quicktype (TypeScript, Swift, Kotlin)
├── server/                     # Spring Boot composition service (Java)
│   └── src/main/java/com/nba/sdui/
│       ├── controller/         # SduiController, GamesController
│       └── service/            # SduiCompositionService, WatchComposer, ForYouComposer, ScoreboardComposer, DemoScreenComposer, LiveComposer
├── android/                    # Android application
│   ├── app/                    # Thin app shell (navigation, config, theme)
│   └── sdui-core/              # Reusable SDUI library (renderers, state, data)
├── web/                        # React/TypeScript web client
│   └── src/
│       ├── components/         # SectionRouter + 8 semantic section renderers + AtomicRouter + SectionErrorBoundary, SectionSkeleton
│       ├── hooks/              # useSduiScreen, useRefreshPolicy, useImpressionTracking, useAnalyticsContext
│       └── runtime/            # AblyClient, ActionHandler, DataBindingApplier
├── ios/                        # iOS / SwiftUI client (Swift Package + SduiDemo app)
│   ├── Sources/SduiCore/       # SectionRouter + 8 semantic section views + AtomicRouter + navigation shell
│   ├── Tests/SduiCoreTests/    # Model round-trips, fixtures, action dispatcher, impression tracker
│   └── SduiDemo/               # XcodeGen-based demo host (bootstraps nba://for-you)
├── docs/                       # Technical proposal & requirements
└── prompts/                    # Prototype generation prompt
```

## Quick Start

### SDUI Composition Server

A shared SDUI composition server is deployed and accessible to all developers on
the corporate VPN:

```
https://sdui-prototype.tools.internal.nba.com
```

All clients default to this server. **No local server setup is required** to run
any client unless you are actively developing server-side composers.

### Running Clients

#### Web

```bash
make dev-web-remote
# Open http://localhost:3000
```

The Express dev server proxies `/api/*` and `/sdui-demo/*` requests to the
deployed composition server automatically. Vite serves the React app with HMR.

#### Android

```bash
make dev-android-remote
```

The target boots an emulator if needed, installs the app, launches it, and tails
SDUI logs. The app points at the deployed composition server.

#### iOS

```bash
make dev-ios-remote
```

The target builds `SduiDemo`, installs it on the configured simulator, launches
it against the deployed composition server, and tails SDUI logs.

### Local Server Development

If you're developing server-side composers and need to test against a local
Spring Boot instance, use the local Make targets. They encode the platform-specific
hostnames for you (`localhost` for web/iOS, `10.0.2.2` for the Android emulator).
Client-specific backend inputs stay separate so web, Android, and iOS can run
concurrently against different endpoints when needed.

```bash
# One-time server env setup
cp server/.env.example server/.env   # Add your STATS_API_KEY

# Start the local composition server
make dev-server

# Pick the client you want to run against that local server
make dev-web-local
make dev-android-local
make dev-ios-local
```

Remote equivalents are available when you want the deployed backend:

```bash
make dev-web-remote
make dev-android-remote
make dev-ios-remote
```

The Makefile variables are intentionally client-specific:

| Client | Local target | Remote target | Override variable |
|--------|--------------|---------------|-------------------|
| Web | `make dev-web-local` | `make dev-web-remote` | `SDUI_WEB_LOCAL_SERVER` / `SDUI_WEB_REMOTE_SERVER` |
| Android | `make dev-android-local` | `make dev-android-remote` | `SDUI_ANDROID_LOCAL_SERVER` / `SDUI_ANDROID_REMOTE_SERVER` |
| iOS | `make dev-ios-local` | `make dev-ios-remote` | `SDUI_IOS_LOCAL_SERVER` / `SDUI_IOS_REMOTE_SERVER` |

### Start Everything (Local Server + Web)

```bash
make dev    # Starts server + web in separate Terminal sessions
```

### Test the API

```bash
# Game detail with live data (use any valid gameId)
curl "http://localhost:8080/v1/sdui/game-detail/{gameId}?schemaVersion=1.0&gameState=live"

# Scoreboard (today's games)
curl "http://localhost:8080/v1/sdui/scoreboard?schemaVersion=1.0"

# Game detail variant B (reordered sections)
curl "http://localhost:8080/v1/sdui/game-detail/{gameId}?schemaVersion=1.0&gameState=live&variant=B"

# Scoreboard variant E (promo banner)
curl "http://localhost:8080/v1/sdui/scoreboard?schemaVersion=1.0&variant=E"
```

### Regenerate Models After Schema Changes

```bash
make codegen
```

## Screens

| Screen | Endpoint | Description |
|--------|----------|-------------|
| Scoreboard | `GET /v1/sdui/scoreboard` | Today's games as tappable rows. Live games update via Ably SSE. |
| Game Detail | `GET /v1/sdui/game-detail/{gameId}` | Single game: scoreboard header, stats, tabs, editorial content. |
| For You | `GET /v1/sdui/for-you` | Personalised content feed with games, editorial, and promos. |
| Watch | `GET /v1/sdui/watch` | Video hub with Featured, NBA TV, and League Pass tabs. |
| Live / Games | `GET /v1/sdui/live` | Live scoreboard with real-time game panels. |
| Leaders | `GET /v1/sdui/leaders` | Season leaders table with form-driven season/type filters. |
| Kitchen Sink | `GET /v1/sdui/demos` | Demo screen showcasing all section types with sample data. |
| Boxscore | `GET /v1/sdui/boxscore/{gameId}` | Boxscore tables for a specific game (home and away). |
| Refresh | `GET /v1/sdui/refresh/{screenId}` | Parameterized refresh endpoint for form-driven section updates. |

## Section Types (9 in schema: 8 permanent + AtomicComposite)

### Semantic Sections (client-owned renderers)

| Type | Description | Refresh |
|------|-------------|---------|
| TabGroup | Tabbed navigation with state-driven content | Poll or static |
| BoxscoreTable | Boxscore stats table | Poll or static |
| Form | Interactive form with typed fields | Static |
| SeasonLeadersTable | Season leaders stats table | Static |
| SubscribeBanner | Inline subscription upsell with CTA | Static |
| SubscribeHero | Full-screen subscription upsell with pricing tiers | Static |
| AdSlot | Embedded ad placement (provider, targeting) | Static |
| VideoPlayer | Platform video SDK host with playerType discriminator (game / vod / event / nbaTv / stream) and capability list (pip, chromecast, airplay, backgroundAudio, fullscreenRotation) | Static (player drives its own timeline) |

### Server-composed Atomic Surfaces (delivered as `AtomicComposite` — no client renderers)

| Surface | Description |
|---------|-------------|
| Stat line | Player stat rows |
| Hero panel | Single content item (article/video) |
| Content rail | Horizontal scrolling content strip |
| Promo banner | Promotional banner with CTA |
| Section header | Simple header with optional subtitle and CTA |
| Video carousel | Horizontal scrolling video thumbnails |
| NBA TV schedule | NBA TV hero image + time-slot schedule |
| Following rail | Horizontal rail of followed items |
| Error state | Server/client error with title, message, optional retry action |
| Game panel | Game card (teams, scores, status) composed with `LiveClock` for real-time game clocks |

### Atomic Primitives (server-composed, rendered by AtomicRouter)

| Element | Purpose |
|---------|---------|  
| Container | Flex layout (row/column) with padding, gap, alignment, background gradient, `flex` (child weight), `breakpoint` (responsive direction flip) |
| Text | Styled text with variant, color, weight, maxLines |
| Image | Remote image with alt text, dimensions, content scale |
| Button | Interactive element with label, actions, variant |
| Spacer | Fixed-size empty space |
| Divider | Horizontal/vertical line separator |
| ScrollContainer | Scrollable region (horizontal/vertical) with paging + snap |
| Conditional | State-driven if/else branching (`condition` evaluated against screen state) |
| DisplayGrid | Display-only text grid — zero interactivity (sort/filter/expand → use a section) |
| OverlayContainer | Foreground content stacked over a background (image, gradient, scrim) with corner clipping and badge/tag overlays |
| SectionSlot | Embed a full section renderer inside an atomic tree (bridge back to SectionRouter) |
| LiveClock | Client-side ticking clock driven by server-provided snapshot fields (snapshotSeconds, snapshotAt, isRunning, tickDirection, stopAtSeconds, format) |

**Performance contract:** max depth 6, max children/container 20, max nodes 50. Server validates; clients have defensive depth guards.

## Recent Changes

- **Layout constraint & sizing overhaul** (2026-04-30) — `SizingMode` enum (hug/fill/fixed), `widthMode`/`heightMode` fields, `minWidth`/`maxWidth`/`minHeight`/`maxHeight` constraints, `layoutWrap` for flex-wrap, `crossAxisGap`, per-child `alignSelf`. Multi-layer `backgrounds` and `shadows` arrays with inner-shadow support (`Shadow.type: "inner"`). Deprecated `fillWidth`, singular `background`, singular `shadow`. Updated `LayoutTokenResolver` across all platforms for corrected `md=12` base. Replaced all broken loremflickr demo image URLs with same-origin `DemoImageUrls` SVGs.
- **Per-section error handling** (2026-04-01) — `SectionErrorBoundary` on Android (catch-at-dispatch + pre-validation) and web (React ErrorBoundary). `SectionSkeleton` with 4 generic styles. Typed `SectionStates` model. Retry budget (client-side, default 5). `hideOnError` support. Error-handling contract rewritten.
- **Accessibility** (2026-03-26) — `AccessibilityProperties` on Section, Subsection, AtomicElement. Android Compose `semantics{}`, web ARIA attributes, iOS `.accessibilityLabel`/traits. All 8 semantic section renderers and 12 atomic primitives wired on every platform.
- **Request transport envelope** (2026-03-24) — `SduiRequestContext` POJO + `BracketParamResolver` on server. `RequestEnvelopeBuilder` on Android, iOS, and web. GET-first with bracket-notation params, POST fallback.
- **i18n** (2026-03-24) — Section-level `stringTable` stamped by server per locale. Clients consume from each section.
- **Experiments / A/B testing** (2026-03-24) — ADR-006 Accepted. Client-authoritative `experiments` map in request envelope. Server resolves at composition time.
- **Atomic rendering layer** (2026-03-13) — Dual-layer architecture: 12 atomic element types (10 rendering primitives + SectionSlot bridge + LiveClock), AtomicRouter on Android, iOS, and web, AtomicComposite bridge section type, server-side AtomicCompositeBuilder
- **10 section types migrated to atomic** (2026-03-13) — ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule, GamePanel now served as AtomicComposite (schema definitions pruned)
- **SectionSlot bidirectional bridge** — Atomic trees can embed full section renderers (recursion guard: depth 2)
- **DisplayGrid** — Non-interactive server-ordered text grid primitive
- **Kitchen sink appendix** — Full 42-section demo response documented

## Variants

| Screen | Variant | Description |
|--------|---------|-------------|
| Game Detail | A (default) | Standard section order |
| Game Detail | B | ContentRail and TabGroup swapped |
| Game Detail | C | StatLine and PromoBanner removed |
| Game Detail | D | Second ContentRail added |
| Scoreboard | A (default) | Game cards only |
| Scoreboard | E | PromoBanner at top |
| Scoreboard | F | PromoBanner + ContentRail after second game |

## Refresh Strategies

| Strategy | Use Case | Example |
|----------|----------|---------|
| `static` | Content that doesn't change | Editorial rails, promo banners |
| `poll` | Periodic refresh from SDUI server | Player stats (transformed data) |
| `poll` + `url` | Direct polling to data feed | Boxscore from CDN (lower latency) |
| `sse` | Real-time via Ably channel | Live scores |

## Tech Stack

| Layer | Technology |
|-------|------------|
| Server | Spring Boot 3.x, Java 21, Jackson |
| Android | Kotlin 2.1.0, Jetpack Compose (BOM 2024.12.01), Min SDK 24 |
| iOS | Swift 5.9, SwiftUI, Xcode 15.4+ (XcodeGen-managed demo target), Deployment target iOS 17 |
| Web | React 18, TypeScript, Vite |
| Real-time | Ably 1.2.40 (JWT auth via authUrl) |
| Image loading | Coil 2.7.0 + coil-svg |
| Networking | OkHttp 4.12.0 |
| Codegen (Java) | jsonschema2pojo (Gradle plugin) |
| Codegen (TS/Swift) | quicktype CLI |

## Documentation

| Document | Purpose |
|----------|---------|
| [Executive Summary](docs/SDUI_Executive_Summary_v2.md) | Business case, prototype status, timeline, resourcing |
| [Technical Proposal](docs/SDUI_Technical_Proposal_v2.md) | Architecture, schema design, runtime behavior, requirement status |
| [Requirements Summary](docs/sdui-requirements-summary.md) | Full requirements, gap analysis, ADR tracking |
| [Kitchen Sink Appendix](docs/appendix-kitchen-sink.md) | Full 42-section demo response (Android platform) |
| [Client Implementor's Contract](docs/client-implementors-contract.md) | Platform-agnostic build guide for new clients (any language/framework) |
| [Development Setup](docs/sdui-setup.md) | Prerequisites, secrets, dependency bootstrap, and Makefile runtime overrides |
| [ADR Index](docs/adr/README.md) | Architecture Decision Records (001–013) |
| [Accessibility Plan](docs/plans/plan-accessibility.md) | Accessibility strategy and implementation plan |
