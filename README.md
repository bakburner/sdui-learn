# SDUI Prototype

Server-Driven UI prototype demonstrating server-composed screens with real-time updates across Android and Web. Includes For You, Scoreboard, Game Detail, Watch, Live, Leaders, Boxscore, and demo screens — all driven by a single JSON schema.

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
│       ├── components/         # SectionRouter + 19 section renderers + SectionErrorBoundary, SectionSkeleton
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

## Section Types (18)

| Type | Description | Refresh |
|------|-------------|---------|
| StatLine | Player stat rows | Poll (30s) or static |
| HeroPanel | Single content item (article/video) | Static |
| ContentRail | Horizontal scrolling content strip | Static |
| TabGroup | Tabbed navigation with state-driven content | Poll or static |
| PromoBanner | Promotional banner with CTA | Static |
| GamePanel | Game card with teams, scores, leaders. `variant: "standard"`, `"featured"` (hero-sized), `"scoreboard"` (compact row with live scores) | SSE or static |
| Row | Responsive side-by-side/stacked layout | Inherits from children |
| SectionHeader | Simple header with optional subtitle and CTA | Static |
| VideoCarousel | Horizontal scrolling video thumbnails | Static |
| NbaTvSchedule | NBA TV hero image + time-slot schedule | Static |
| SubscribeBanner | Inline subscription upsell with CTA | Static |
| SubscribeHero | Full-screen subscription upsell with pricing tiers | Static |
| AdSlot | Embedded ad placement (provider, targeting) | Static |
| BoxscoreTable | Boxscore stats table | Poll or static |
| Form | Interactive form with typed fields | Static |
| SeasonLeadersTable | Season leaders stats table | Static |
| FollowingRail | Horizontal rail of followed items | Static |
| ErrorState | Server/client error with title, message, optional retry action | Static |

## Recent Changes (2026-03-12)

- **Schema**: `SectionLayoutHints` (margins, dividers, priority) and `SectionStates` (loading skeleton, error message/retry) added to `Section`
- **Schema**: `interactive` added to `contentType` enum
- **Web**: `SectionErrorBoundary` (React Error Boundary), `SectionSkeleton` (shimmer/spinner/placeholder), `useImpressionTracking` (IntersectionObserver + dedup), `useAnalyticsContext` (dedup registry)
- **Web**: Layout hints applied in `SectionList` (margins, dividers)
- **Server**: `sectionStates` emitted on all live sections; platform header threaded through demos endpoint; no hardcoded platform defaults
- **Android**: `X-Platform: android` header sent on all requests; deserialization failures now logged; Ably log level reduced to WARN; emulator RAM increased to 2048MB
- **ADRs**: ADR-008 (layout hints) accepted as Option C; ADR-009 (impression dedup) accepted
- **Docs**: Executive summary, technical proposal, requirements summary, and `.claude.md` updated

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
| [Implementation Reference](docs/sdui-implementation-reference.md) | Impression tracking code samples (Swift/Kotlin) |
| [Prototype Prompt](prompts/SDUI_Prototype_Prompt.md) | Full description of what the prototype builds and how |
