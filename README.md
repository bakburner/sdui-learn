# SDUI Prototype

Server-Driven UI prototype demonstrating server-composed screens with real-time updates across Android and Web. Includes For You, Scoreboard, Game Detail, Watch, Live, Leaders, Boxscore, and demo screens — all driven by a single JSON schema.

## Design Philosophy

**The server controls _what_ appears and _in what order_. Platform teams control _how_ each component looks, feels, and animates natively.**

Five principles guide every decision:

1. **Schema is source of truth.** `schema/sdui-schema.json` defines the contract. Run `make codegen` after changes. Generated models are never hand-edited.
2. **Atomic for layout, semantic for domain logic.** Atomic primitives (`Container`, `Text`, `Image`, etc.) handle server-composed layout with zero client logic. Semantic section types (`GamePanel`, `BoxscoreTable`) exist only when client-owned state or complex interaction is required. Default to atomic; promote to semantic only when you must.
3. **No hardcoded URLs or screen-type enums on clients.** Every screen is a generic `fetchScreen(endpoint)`. Endpoints are resolved from server-provided URIs (`nba://` → `/sdui/`). New screens require zero client code.
4. **Graceful degradation everywhere.** Unknown section types, unknown atomic elements, and unknown action types are skipped with a log — never crash. Stale cached responses are better than blank screens.
5. **Decision checklist before adding client code:** Can the server compose it? Can a schema/action change solve it? Can it be an `AtomicComposite`? Only if none of those work, add a client renderer.

### Dual-Layer Rendering Model

The architecture uses two complementary rendering layers:

- **Section layer** — Named domain renderers (`BoxscoreTable`, `GamePanel`, `Form`, `TabGroup`, etc.) with client-owned state (sort, scroll position, form input). Routed by `SectionRouter`.
- **Atomic layer** — 10 atomic element types (`Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`, `DisplayGrid`, `SectionSlot`) with zero client business logic. Routed by `AtomicRouter`.
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
│       ├── components/         # SectionRouter + 8 permanent section renderers + AtomicRouter + SectionErrorBoundary, SectionSkeleton
│       ├── hooks/              # useSduiScreen, useRefreshPolicy, useImpressionTracking, useAnalyticsContext
│       └── runtime/            # AblyClient, ActionHandler, DataBindingApplier
├── docs/                       # Technical proposal & requirements
└── prompts/                    # Prototype generation prompt
```

## Quick Start

### Start Everything

```bash
make dev    # Opens Terminal sessions for server and web
```

### Or Individually

```bash
# Server (http://localhost:8080)
cd server
cp .env.template .env   # Add your STATS_API_KEY
./gradlew bootRun

# Web (http://localhost:3000)
cd web
npm install
npm run dev

# Android — open android/ in Android Studio, run on emulator
# (connects to http://10.0.2.2:8080)
```

### Test the API

```bash
# Game detail with live data (use any valid gameId)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/game-detail/{gameId}?gameState=live"

# Scoreboard (today's games)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/scoreboard"

# Game detail variant B (reordered sections)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/game-detail/{gameId}?gameState=live&variant=B"

# Scoreboard variant E (promo banner)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/scoreboard?variant=E"
```

### Regenerate Models After Schema Changes

```bash
make codegen
```

## Screens

| Screen | Endpoint | Description |
|--------|----------|-------------|
| Scoreboard | `GET /sdui/scoreboard` | Today's games as tappable rows. Live games update via Ably SSE. |
| Game Detail | `GET /sdui/game-detail/{gameId}` | Single game: scoreboard header, stats, tabs, editorial content. |
| For You | `GET /sdui/for-you` | Personalised content feed with games, editorial, and promos. |
| Watch | `GET /sdui/watch` | Video hub with Featured, NBA TV, and League Pass tabs. |
| Live / Games | `GET /sdui/live` | Live scoreboard with real-time game panels. |
| Leaders | `GET /sdui/leaders` | Season leaders table with form-driven season/type filters. |
| Kitchen Sink | `GET /sdui/demos` | Demo screen showcasing all section types with sample data. |
| Boxscore | `GET /sdui/boxscore/{gameId}` | Boxscore tables for a specific game (home and away). |
| Refresh | `GET /sdui/refresh/{screenId}` | Parameterized refresh endpoint for form-driven section updates. |

## Section Types (9 in schema: 8 permanent + AtomicComposite)

### Permanent Sections (client-owned renderers)

| Type | Description | Refresh |
|------|-------------|---------|
| TabGroup | Tabbed navigation with state-driven content | Poll or static |
| GamePanel | Game card with teams, scores, leaders. Server-driven `displayConfig` controls layout (logo size, card height, score style, background). Scoreboard rows, featured hero cards, and standard cards are all composed via `displayConfig` — no client branching. | SSE or static |
| BoxscoreTable | Boxscore stats table | Poll or static |
| Form | Interactive form with typed fields | Static |
| SeasonLeadersTable | Season leaders stats table | Static |
| SubscribeBanner | Inline subscription upsell with CTA | Static |
| SubscribeHero | Full-screen subscription upsell with pricing tiers | Static |
| AdSlot | Embedded ad placement (provider, targeting) | Static |

### Migrated to Atomic (server-composed AtomicComposite — no client renderers)

| Type | Description |
|------|-------------|
| StatLine | Player stat rows |
| HeroPanel | Single content item (article/video) |
| ContentRail | Horizontal scrolling content strip |
| PromoBanner | Promotional banner with CTA |
| SectionHeader | Simple header with optional subtitle and CTA |
| VideoCarousel | Horizontal scrolling video thumbnails |
| NbaTvSchedule | NBA TV hero image + time-slot schedule |
| FollowingRail | Horizontal rail of followed items |
| ErrorState | Server/client error with title, message, optional retry action |

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
| SectionSlot | Embed a full section renderer inside an atomic tree (bridge back to SectionRouter) |

**Performance contract:** max depth 6, max children/container 20, max nodes 50. Server validates; clients have defensive depth guards.

## Recent Changes (2026-03-13)

- **Atomic rendering layer** — Dual-layer architecture: 10 atomic element types (9 rendering primitives + SectionSlot bridge), AtomicRouter on Android and Web, AtomicComposite bridge section type, server-side AtomicCompositeBuilder
- **9 section types migrated to atomic** — ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule now served as AtomicComposite (schema definitions pruned)
- **SectionSlot bidirectional bridge** — Atomic trees can embed full section renderers (recursion guard: depth 2)
- **ScoreboardHeader consolidated** — Merged into GamePanel; compact scoreboard row is now driven by `displayConfig` (no variant branching)
- **DisplayGrid** — Non-interactive server-ordered text grid primitive
- **Governance docs updated** — Executive Summary, Technical Proposal (§2a, §8, §9s, §10), Requirements Summary all reflect dual-layer model
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
| [ADR Index](docs/adr/README.md) | Architecture Decision Records (001–010) |
| [Accessibility Plan](docs/plan-accessibility.md) | Accessibility strategy and implementation plan |
