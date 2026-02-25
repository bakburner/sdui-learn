# SDUI Prototype

Server-Driven UI prototype demonstrating real-time game score updates across Android and Web for NBA Scoreboard and Game Detail screens.

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
│       └── service/            # SduiCompositionService, StatsApiClient
├── android/                    # Android application
│   ├── app/                    # Thin app shell (navigation, config, theme)
│   └── sdui-core/              # Reusable SDUI library (renderers, state, data)
├── web/                        # React/TypeScript web client
│   └── src/
│       ├── components/         # SectionRouter + 7 section renderers
│       ├── hooks/              # useSduiScreen, useRefreshPolicy
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
# Game detail with live data
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/game-detail/0042300102?gameState=live"

# Scoreboard (today's games)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/scoreboard"

# Game detail variant B (reordered sections)
curl -H "X-Schema-Version: 1.0" \
  "http://localhost:8080/sdui/game-detail/0042300102?gameState=live&variant=B"

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

## Section Types (8)

| Type | Description | Refresh |
|------|-------------|---------|
| ScoreboardHeader | Team logos, tricodes, scores, game status | SSE (live) or static |
| StatLine | Player stat rows | Poll (30s) or static |
| ContentCard | Single content item (article/video) | Static |
| ContentRail | Horizontal scrolling content strip | Static |
| TabGroup | Tabbed navigation with state-driven content | Poll or static |
| PromoBanner | Promotional banner with CTA | Static |
| GameCard | Game card with teams, scores, leaders | SSE or static |
| Row | Responsive side-by-side/stacked layout | Inherits from children |

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
| [Technical Proposal](docs/SDUI_Technical_Proposal_v2.md) | Architecture, schema design, runtime behavior, requirement status |
| [Requirements Summary](docs/sdui-requirements-summary.md) | Full requirements, gap analysis, ADR tracking |
| [Implementation Reference](docs/sdui-implementation-reference.md) | Impression tracking code samples (Swift/Kotlin) |
| [Prototype Prompt](prompts/SDUI_Prototype_Prompt.md) | Full description of what the prototype builds and how |
