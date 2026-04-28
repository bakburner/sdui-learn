# Production Server Architecture — Design Patterns

> Derived from prototype evolution + SAF integration analysis.
> These patterns govern **business and composition logic only** — transport,
> caching, resilience, collapsing, metrics, and request correlation are SAF
> concerns and are not duplicated here.

## Deployment Topology: Collocated Modular Monolith

The SDUI service is a **single deployable** containing two internal layers:
a composition layer and collocated aggregation modules. This topology is
chosen for 10K RPS performance — eliminating internal network hops,
serialization overhead, and redundant caching between tiers.

```
┌───────────────────────────────────────────────────────────┐
│  SDUI Service (single deployable)                         │
│                                                           │
│  ┌───────────────────────────────────────────────────┐    │
│  │  Composition Layer                                │    │
│  │  ScreenComposers → SectionComposers → Builder DSL │    │
│  │  Own: UI tree assembly, variant transforms,       │    │
│  │       refresh policy, bindings, token enforcement,│    │
│  │       response envelope                           │    │
│  └────────────────────┬──────────────────────────────┘    │
│                       │ in-process call via interface      │
│  ┌────────────────────▼──────────────────────────────┐    │
│  │  SAF Orchestration (library)                      │    │
│  │  Own: parallel execution, L1/L2 caching,          │    │
│  │       resilience, collapsing, metrics,            │    │
│  │       correlation, deadlines                      │    │
│  └────────────────────┬──────────────────────────────┘    │
│                       │ ServiceCall<T>                     │
│  ┌────────────────────▼──────────────────────────────┐    │
│  │  Aggregation Modules (in-process)                 │    │
│  │  ┌────────────┐ ┌─────────────┐ ┌─────────────┐  │    │
│  │  │  game-agg  │ │ content-agg │ │schedule-agg │  │    │
│  │  └─────┬──────┘ └──────┬──────┘ └──────┬──────┘  │    │
│  │  Own: upstream API calls, data joins,             │    │
│  │       transformation, domain object shaping       │    │
│  └────────┼───────────────┼───────────────┼──────────┘    │
└───────────┼───────────────┼───────────────┼───────────────┘
            │               │               │  HTTP to upstreams
      ┌─────▼───┐     ┌─────▼───┐     ┌─────▼───┐
      │Stats API│     │CMS / DAM│     │Stats API│
      └─────────┘     └─────────┘     └─────────┘
```

### Why collocated (not MSA) at 10K RPS

- **Zero ser/deser between aggregation and composition.** Domain objects pass
  by reference. At 10K RPS with ~4 calls per screen, that eliminates ~40K
  serialize/deserialize cycles per second and significant GC pressure.
- **Single L1 cache.** SAF's Caffeine cache holds upstream responses. Both
  aggregation and composition share one JVM heap — no redundant caching.
- **Maximal request collapsing.** One collapse boundary at the upstream edge.
  MSA would collapse at two tiers (SDUI→agg, agg→upstream).
- **Single JVM to tune.** One heap, one GC config (ZGC/Shenandoah), one
  set of virtual thread pools.
- **Latency.** Each internal network hop adds 2-5ms at P50, worse at P99.
  For 100-200ms response time targets, burning 5-10% on internal topology
  is waste.

### Failure isolation without network boundaries

- SAF **bulkhead** limits concurrent calls per aggregation module
- SAF **circuit breaker** fails fast when a module's upstream is degraded
- Java 21 **virtual threads** — a blocked upstream costs ~1KB, not a
  platform thread
- SAF **timeout + deadline propagation** prevents cascading stalls

### Extraction triggers (when to break out a module)

Aggregation modules extract into separate services **only** when:

1. **Shared consumer** — another BFF or service needs the same aggregation
2. **Divergent scaling** — one module needs 10x the resources of others
3. **Independent deploy cadence** — module changes on a different schedule
   and the team structure supports it

Extraction is safe because module boundaries are interface-based (Pattern 9).
Swapping an in-process implementation for an HTTP client changes nothing in
the composition layer or SAF configuration.

### Three-layer boundary

| Aggregation Modules Own | SAF Owns (library) | Composition Layer Owns |
|---|---|---|
| Upstream API integration | Parallel call orchestration | Which aggregates to request |
| Data joins and transformation | Two-tier caching (L1/L2) | What to do when an aggregate is absent |
| Domain object shaping | Circuit breaker, retry, bulkhead | Screen and section assembly |
| Source-specific error handling | Request collapsing + dedup | Variant resolution and transforms |
| Content sourcing (CMS, stats, media) | Correlation ID propagation | Refresh policy declaration |
| | Deadline tracking | Data binding wiring |
| | Metrics + SLO tracking | Token enforcement |
| | Graceful shutdown | Response envelope shape |

The composition layer receives **domain objects** (`GameSummary`,
`ContentFeed`, `ScheduleResponse`) via in-process interface calls — never
raw upstream payloads. SAF wraps these calls with caching, resilience, and
collapsing identically whether the implementation is in-process or remote.

---

## Pattern 1: Typed Composition Model

**Problem:** Prototype builds everything as raw `ObjectNode`. Field-name typos
are runtime failures. Refactors are unsafe. IDE support is nonexistent.

**Pattern:** Java records for every wire-level structure.

```
ScreenEnvelope     — id, title, analyticsId, traceId, schemaVersion,
                     navigation, state, sections[], overlays[], refreshPolicy
SectionEnvelope    — id, type, analyticsId, surface, refreshPolicy,
                     data{ui, content, dataBinding}
AtomicElement      — sealed hierarchy: Container, Text, Image, Button,
                     Spacer, ScrollContainer, LiveClock, SectionSlot,
                     Badge, Divider
ColorToken         — enum wrapping validated token references
SurfacePreset      — enum of shared outer-chrome definitions
RefreshPolicy      — sealed: Static, Sse, Poll, Parameterized
ScreenState        — typed map of server-initialized, client-round-tripped keys
```

Jackson serializes at the HTTP boundary. Composers never touch `ObjectNode`.

---

## Pattern 2: Single-Round Composition Pipeline

**Problem:** Composers must not mix I/O with UI assembly. Data fetching and
composition must be separate phases.

**Pattern:** Declare requirements → SAF fetches in parallel (from collocated
aggregation modules) → compose from results. Single round is the default
because aggregation modules return pre-joined domain objects — the composition
layer rarely needs to "discover" an entity ID from one response to fetch
another.

```
Declare: Composer lists which aggregation modules it needs
  → SAF parallel fetch (in-process, L1-cached, resilient)
  → Domain objects available

Compose: Pure function over (context + domain objects) → ScreenEnvelope
```

**Interface:**

```java
interface ScreenComposer {
    List<DataRequirement<?>> requirements(CompositionContext ctx);

    ScreenEnvelope compose(CompositionContext ctx, ResolvedData data);
}
```

- `requirements` — declares which aggregation modules to call; all fetched
  in one parallel SAF round (in-process, L1-cached at microsecond latency)
- `compose` — zero I/O, pure assembly, directly unit-testable

A `CompositionOrchestrator` calls `safOrchestrator.create()` once per screen.

**Escape hatch:** If a screen genuinely needs a second round (rare — it means
the aggregation module boundary is wrong), the composer can implement:

```java
default List<DataRequirement<?>> conditionalRequirements(
        CompositionContext ctx, ResolvedData round1) {
    return List.of();
}
```

A second round is a smell. If it happens often, the aggregation module should
be providing a richer response, not forcing the composition layer to do
multi-step orchestration.

---

## Pattern 3: Data Requirement as SAF Bridge

**Problem:** Composers should not know how to call aggregation modules
directly, manage caching, or handle upstream failures.

**Pattern:** `DataRequirement` is a typed descriptor that maps composition
intent to a SAF `ServiceCall`. The composer declares *what* it needs; the
pipeline handles *how*.

```java
record DataRequirement<T>(
    String id,             // "game-detail", "tonight-scoreboard"
    String serviceName,    // aggregation module name in SAF YAML
    Supplier<T> call,      // in-process call to aggregation module
    String cacheKey,       // explicit, per SAF convention
    boolean critical        // SAF failOnError
) {}
```

- `serviceName` maps to an aggregation module, not a raw upstream API
- Cache strategy and TTL are configured in SAF YAML per module — the
  composer does not specify them
- Each requirement maps 1:1 to a SAF `ServiceCall`
- Composers never call `OrchestratorFactory` directly
- **SAF doesn't care** whether the `Supplier<T>` calls an in-process module
  or a remote HTTP client — caching, resilience, and collapsing work
  identically either way. This is the extraction seam.

**Absence handling:** When a non-critical requirement resolves to empty, the
composer omits the section or emits `ErrorState`. Aggregation modules handle
their own upstream degradation. The composition layer only decides: include
the section, skip it, or show an error.

---

## Pattern 4: Screen Composers + Section Composers (two-tier)

**Problem:** Prototype has cross-composer injection (`GameDetailComposer`
injecting `BoxscoreComposer`) and duplicated extraction logic (building hero
cards from scoreboard data in multiple places).

**Pattern:** Separate **screen composers** (orchestrators) from **section
composers** (reusable building blocks).

```
ScreenComposer (per screen)
  → declares which aggregation services it needs
  → receives domain objects (GameSummary, ContentFeed, etc.)
  → calls SectionComposers to map domain objects to UI sections
  → assembles ScreenEnvelope (sections array, navigation, state)

SectionComposer (per reusable section type)
  → pure function: (domain object, tokens, surface) → SectionEnvelope
  → examples: GamePanelSection, BoxscoreTabSection, HeroCarouselSection,
    ContentRailSection, EditorialOverlaySection
  → no data fetching, no data transformation, no screen-level concerns
```

Screen composers own the *what* and *order*. Section composers own the *how*
for each reusable unit. A section composer is used by multiple screen composers
without injection hacks.

Because aggregation services return domain-shaped objects, section composers
are thin mappers — `GameSummary → GamePanel atomic tree` — with no data
extraction or transformation logic. If a section composer needs to parse or
join data, the aggregation service response is under-shaped.

---

## Pattern 5: Variant Resolution (two-type model)

**Problem:** Some variants change which data is fetched (compositional). Others
just reorder sections (presentational). A single post-composition decorator
can't handle both.

**Pattern:** Two variant types, applied at different pipeline stages.

```java
interface VariantTransform {
    String experimentId();
    String variantId();
}

interface CompositionalVariant extends VariantTransform {
    /** Add/modify data requirements before fetch */
    List<DataRequirement<?>> augmentRequirements(
        List<DataRequirement<?>> base, CompositionContext ctx);
}

interface PresentationalVariant extends VariantTransform {
    /** Mutate assembled response after composition */
    ScreenEnvelope apply(ScreenEnvelope base, CompositionContext ctx);
}
```

- **Compositional** — applied before SAF fetch rounds (e.g., variant D adds a
  trending content requirement)
- **Presentational** — applied after `compose()` returns (e.g., variant B
  swaps section order, variant C removes sections)

Composers don't `switch` on variant strings. Variants register against
experiment IDs and the pipeline applies them.

---

## Pattern 6: Builder DSL with Type Safety

**Problem:** `AtomicCompositeBuilder` is a convenience layer over Jackson. Colors
are strings. Missing fields are silent nulls.

**Pattern:** Typed builder that enforces schema constraints at compile time.

```java
// Colors: enum, not String
Text.builder()
    .content("Q3 4:32")
    .variant(Typography.BODY_MEDIUM)
    .color(ColorToken.TEXT_PRIMARY)     // ← not a String
    .maxLines(1)
    .build();

// Containers: typed direction + alignment
Container.column()
    .alignment(MainAxis.SPACE_BETWEEN)
    .crossAlignment(CrossAxis.CENTER)
    .children(awayTeam, statusSlot, homeTeam)
    .padding(Spacing.of(12, 16, 12, 16))
    .build();

// Sections: return SectionEnvelope, not ObjectNode
SectionEnvelope header = SectionHeader.builder()
    .id("tonight-header")
    .title("Tonight's Games")
    .actionLabel("See Schedule")
    .actionUri("nba://scoreboard")
    .surface(SurfacePreset.HEADER)
    .build();
```

`ColorToken` validates against `TokenRegistry` at construction. Raw hex is
rejected. Palette escape hatch: `ColorToken.palette("blue.30")`.

---

## Pattern 7: Screen State Contract

**Problem:** Parameterized refresh uses ad-hoc `Map<String, String>`. No
contract for which keys exist, who initializes them, or what the client
round-trips.

**Pattern:** Typed `ScreenState` per screen, emitted by composer, round-tripped
by client on refresh/submit.

```java
record BoxscoreState(
    String team,
    String awaySortCol,
    String awaySortDir,
    String homeSortCol,
    String homeSortDir
) implements ScreenState {}

record LeadersState(
    String season,
    String seasonType,
    String perMode,
    String statCategory
) implements ScreenState {}
```

- Server initializes defaults in `compose()`
- Client mutates locally (sort, filter, tab selection)
- Client sends full state on parameterized refresh
- Server deserializes to typed state, uses it to compose refresh response

---

## Pattern 8: Refresh & Partial Response

**Problem:** Parameterized refresh returns a partial screen response but there's
no formal contract distinguishing full vs. partial.

**Pattern:** `ScreenEnvelope` carries a `responseType` discriminator.

```
responseType: "full"    — complete screen, client replaces everything
responseType: "partial" — sections array replaces by ID, state merges
```

Refresh handlers are co-located with their screen composer:

```java
interface RefreshableScreenComposer extends ScreenComposer {
    ScreenEnvelope handleRefresh(
        ScreenState state, Map<String, String> params, CompositionContext ctx);
}
```

The `handleRefresh` method returns a `partial` envelope. The composition
pipeline validates that every section ID in the partial response exists in the
original screen.

---

## Pattern 9: Aggregation Modules with Interface Extraction Seams

**Problem:** Prototype uses `DemoImageUrls`, inline mock arrays, and
`StatsApiClient` interchangeably. Data transformation is tangled with
composition.

**Pattern:** Each aggregation concern gets a typed interface with an
in-process implementation. The interface is the extraction seam — swappable
to a remote HTTP client without changing the composition layer.

```java
// Interface — the extraction seam
interface GameAggregation {
    GameSummary getGameDetail(String gameId);
    ScoreboardSummary getTonightScoreboard();
    BoxscoreSummary getBoxscore(String gameId);
}

interface ContentAggregation {
    ContentFeed getEditorialFeed(String category, int limit);
    PromoCard getActivePromo(String placement);
}

interface ScheduleAggregation {
    ScheduleResponse getSchedule(String season, String month);
}

interface MediaAggregation {
    VodCatalog getVodPlaylist(String category);
}
```

```java
// In-process implementation (default — collocated in same JVM)
@Component
class GameAggregationModule implements GameAggregation {
    private final StatsApiHttpClient statsClient;  // calls upstream

    @Override
    public GameSummary getGameDetail(String gameId) {
        var raw = statsClient.getBoxscore(gameId);  // HTTP to upstream
        return GameSummary.fromRaw(raw);            // transform here, not in composer
    }
}
```

```java
// Remote implementation (future — when extraction trigger fires)
@Component
@Profile("remote-game-agg")
class GameAggregationRemoteClient implements GameAggregation {
    private final RestClient restClient;

    @Override
    public GameSummary getGameDetail(String gameId) {
        return restClient.get()
            .uri("/games/{id}/detail", gameId)
            .retrieve()
            .body(GameSummary.class);
    }
}
```

SAF wraps both implementations identically:

```java
ServiceCall.<GameSummary>builder()
    .id("game-detail")
    .serviceName("game-aggregation")
    .call(() -> gameAggregation.getGameDetail(gameId))  // ← in-process or remote
    .cache("game:" + gameId)
    .failOnError(true)
    .build();
```

| SAF Service Name | Module | Criticality | Notes |
|---|---|---|---|
| `game-aggregation` | `GameAggregationModule` | Critical | Game screens fail without it |
| `content-aggregation` | `ContentAggregationModule` | Optional | Graceful degradation |
| `schedule-aggregation` | `ScheduleAggregationModule` | Optional | Graceful degradation |
| `media-aggregation` | `MediaAggregationModule` | Optional | Graceful degradation |

Composers reference these by SAF service name in `DataRequirement`.
No content is hardcoded. No data transformation in the composition layer.
`DemoImageUrls`, `StatsApiClient`, and mock arrays disappear entirely.

**Gradle module structure (enforces seams at compile time — see Seam
Enforcement below):**

```
server/
  settings.gradle.kts
  sdui-model/               # domain objects + aggregation interfaces
    src/.../model/           # GameSummary, ContentFeed, ScoreboardSummary, etc.
    src/.../aggregation/api/ # GameAggregation, ContentAggregation interfaces
  sdui-aggregation-game/     # depends on: sdui-model only
    src/.../aggregation/game/
  sdui-aggregation-content/  # depends on: sdui-model only
    src/.../aggregation/content/
  sdui-aggregation-schedule/ # depends on: sdui-model only
    src/.../aggregation/schedule/
  sdui-aggregation-media/    # depends on: sdui-model only
    src/.../aggregation/media/
  sdui-composition/          # depends on: sdui-model only (NOT aggregation impls)
    src/.../composition/     # ScreenComposers, SectionComposers, Builder DSL
  sdui-app/                  # depends on: ALL modules (Spring assembly point)
    src/.../SduiApplication.java
    src/.../config/
```

Each `aggregation/*` package is a self-contained module. Extraction into a
separate service means moving the package, adding an HTTP controller on the
new service, and swapping the `@Component` implementation behind the
interface. The composition layer and SAF config don't change.

---

## Pattern 10: Data Binding Factory

**Problem:** `buildCompositeLinescoreBindings()` is a single helper for one data
shape. Real-time binding needs to generalize beyond linescore.

**Pattern:** Typed binding builders per real-time data shape.

```java
sealed interface BindingTemplate permits LinescoreBinding, PlayByPlayBinding, ... {
    String channel(String entityId);
    List<BindingPath> paths();
    ObjectNode preSeed(SourceData data);
}

record LinescoreBinding() implements BindingTemplate {
    String channel(String gameId) { return gameId + ":linescore"; }
    List<BindingPath> paths() { return List.of(
        new BindingPath("$.homeTeam.score", "content.homeTeam.score"),
        new BindingPath("$.awayTeam.score", "content.awayTeam.score"),
        new BindingPath("$.gameClock", "content.clock", "liveClockSnapshot")
    ); }
}
```

Each binding template declares its Ably channel pattern, source→target path
mappings, and how to pre-seed content for instant first paint.

---

## Pattern 11: Request Envelope Cache Key Strategy

**Problem:** The request envelope carries every field in the URL query string,
including per-device identity and high-cardinality geo data. This makes every
URL globally unique, defeating CDN caching entirely — 0% hit rate. The
deterministic key ordering, RFC-3986 encoding, and GET/POST duality were
built specifically to enable CDN caching, but metadata fields in the URL
prevent it from working.

**Pattern:** Split the envelope into **composition context** (URL query — forms
the cache key) and **request metadata** (headers — excluded from cache key).
A field belongs in the cache key only if the server's composition output
changes when that field changes.

### Field classification

**URL query params (cache key):**

| Param | Example | Why it's a composition input |
|---|---|---|
| `platform[name]` | `android` | Different sections per platform (web vs mobile vs TV) |
| `platform[deviceClass]` | `phone` | Layout grid — phone, tablet, TV, web (ADR-008) |
| `locale` | `en` | Content language |
| `schemaVersion` | `1.0` | Schema vocabulary contract — determines what the client can decode |
| `gameState` | `live` | Composition variant: live, pre, post, final, blackout |
| `experiments[<id>]` | `experiments[gd_tab_v2]=variant_b` | A/B experiment branches |
| `device[countryCode]` | `US` | Different leagues, sponsors, legal constraints per market |
| `device[cohort]` | `ny_metro` | Geo cohort (1 of ~200 US DMAs). Edge worker resolves zip → cohort before origin. Drives regional content, promos, broadcast territory logic. |
| User params | `teamId=1610612737` | Form submits, filter bindings, refresh paramBindings. Sorted by key. |

**Request headers (metadata — not in cache key):**

| Header | Example | Why it's metadata |
|---|---|---|
| `Authorization` | `Bearer eyJ...` | Identity/entitlement. Already a header today. |
| `X-Trace-Id` | `abc-123-def` | Request correlation for logging. Already a header today. |
| `X-Device-Id` | `d9f2e...` | Anonymous identity fallback when no JWT. Not a composition input. |
| `X-Client-Version` | `android/8.3.0/14` | App version + OS version. Analytics and crash triage. Not a composition input — `schemaVersion` covers client capability. Different OS/app versions have renderers for the same schema vocabulary. |
| `X-Device-Geo` | `US/NY/10001` | Raw country + region + zip. Available for logging/analytics. The composition-relevant signal is the resolved `cohort` in the URL, not the raw zip. |
| `X-Capabilities` | `sse=true,onFocus=true` | Client runtime capabilities. The server sends `refreshPolicy` regardless — the client decides how to realize it (SSE vs polling) based on its own capabilities. |

### Cohort resolution

The client sends `device[zipCode]` to the edge. An edge worker (CloudFront
Functions, Fastly Compute, or Cloudflare Workers) resolves the zip to one of
~200 US DMA cohorts via a static lookup table, then rewrites the URL:

```
Client:  /sdui/home?...&device[zipCode]=10001
Edge:    /sdui/home?...&device[cohort]=ny_metro    ← CDN caches on this
Origin:  sees device[cohort]=ny_metro in CompositionContext
```

The lookup table is a small static artifact (~200 entries) updated by a
separate pipeline when broadcast territory assignments change. The edge
worker is stateless — it reads the table and rewrites, with no external calls.

International markets use `device[countryCode]` directly (already low
cardinality). The cohort field is primarily a US concern driven by DMA
broadcast territories.

### Cache key cardinality

```
platform[name](4: android, ios, web, tv)
  × deviceClass(4: phone, tablet, tv, web)
  × locale(5)
  × gameState(5)
  × experiments(32 — 5 experiments × 2 variants)
  × countryCode(10)
  × cohort(200)
= ~25.6M theoretical maximum

Realistic (not all combinations exist — no tv+ios, etc.): ~30,000-50,000 keys per screen
```

At 10K RPS across ~50K keys with a 10s CDN TTL on live screens, each key
averages ~2 hits per TTL window. Marquee cohorts (NY, LA, Chicago on
game night) hit much more heavily. Weighted CDN hit rate: **75-85%**.

Static screens (standings, news, schedule) with 30-60s TTL: **90-95%** hit rate.

### Server-side enforcement

The boundary between composition inputs and request metadata is enforced in
the type system, not just documented:

```java
// Resolved from URL query params — the only input composers receive.
public record CompositionContext(
    String locale,
    String schemaVersion,
    String gameState,
    String platformName,
    String deviceClass,
    String countryCode,
    String cohort,
    Map<String, String> experiments
) {}

// Resolved from headers — available for logging, analytics,
// and link-gating resolution. NOT injected into screen/section composers.
public record RequestMetadata(
    String traceId,
    String deviceId,
    String authToken,
    String clientVersion,
    DeviceGeo geo,
    Capabilities capabilities
) {}
```

Screen and section composers receive `CompositionContext` only. If a composer
accesses `RequestMetadata`, it is visible in code review and flagged by an
ArchUnit test:

```java
@ArchTest
static final ArchRule composers_must_not_access_request_metadata =
    noClasses().that().resideInAPackage("..composer..")
        .should().dependOnClassesThat()
        .haveSimpleName("RequestMetadata")
        .because("Composers use CompositionContext (cache key). " +
                 "RequestMetadata is excluded from the CDN cache key.");
```

**One exception:** A link composer that builds navigation action URIs may
access `RequestMetadata.geo` to resolve gating decisions (e.g., blackout)
into `gameState` values on the outbound URI. This is the single place where
geo gating resolution lives — it happens at link composition on the parent
screen, not at target screen composition.

### CDN cache behavior

| Screen | CDN TTL | Scope | Effective keys | Expected hit rate |
|---|---|---|---|---|
| `/sdui/home` | 30s | Shared (all cohorts see regional content) | ~10,000 | 90-95% |
| `/sdui/scoreboard` | 10s | Shared + gameState | ~15,000 | 80-90% |
| `/sdui/game-detail/{id}` | 10s | Shared + gameState (blackout resolved at parent) | ~5,000/game | 75-85% |
| `/sdui/standings` | 60s | Shared | ~500 | 95%+ |
| `/sdui/news` | 30s | Shared | ~5,000 | 90-95% |

### What this changes from today

| Field | Today | Production | Change |
|---|---|---|---|
| `platform[name]` | URL query | URL query | None |
| `platform[deviceClass]` | URL query | URL query | None |
| `platform[osVersion]` | URL query | `X-Client-Version` header | **Move** |
| `platform[appVersion]` | URL query | `X-Client-Version` header | **Move** |
| `platform[capabilities][sse]` | URL query | `X-Capabilities` header | **Move** |
| `platform[capabilities][onFocus]` | URL query | `X-Capabilities` header | **Move** |
| `locale` | URL query | URL query | None |
| `schemaVersion` | URL query | URL query | None |
| `gameState` | URL query | URL query | None |
| `experiments[]` | URL query | URL query | None |
| `device[deviceId]` | URL query | `X-Device-Id` header | **Move** |
| `device[zipCode]` | URL query | Edge → `device[cohort]` + `X-Device-Geo` header | **Resolve + move raw** |
| `device[countryCode]` | URL query | URL query | None |
| `device[region]` | URL query | `X-Device-Geo` header | **Move** (cohort captures broadcast territory) |

Six fields move from URL query to headers. Zero fields are dropped — the
server still receives everything via `BracketParamResolver`, which already
supports header fallback. `RequestMetadata` is populated from headers;
`CompositionContext` from URL query params.

---

## Production Pipeline (revised)

```
Client Request
  │  URL: /sdui/home?platform[name]=android&...&device[zipCode]=10001
  │  Headers: Authorization, X-Trace-Id, X-Device-Id, X-Client-Version, X-Capabilities
  │
  ▼
Edge Worker (CloudFront Functions / Fastly Compute)
  │  Resolves: zipCode → cohort (ny_metro)
  │  Rewrites URL: device[zipCode]=10001 → device[cohort]=ny_metro
  │  Passes raw geo to X-Device-Geo header
  │
  ▼
CDN Cache Lookup (keyed on rewritten URL — ~30-50K keys/screen)
  │  HIT (75-95%) → serve cached response, skip origin
  │  MISS → forward to origin
  │
  ▼
BracketParamResolver
  │  URL query → CompositionContext (locale, platform, experiments, cohort, ...)
  │  Headers  → RequestMetadata  (traceId, deviceId, clientVersion, geo, capabilities)
  │
  ▼
CompositionRouter → selects ScreenComposer
  │  (receives CompositionContext only)
  │
  ▼
ScreenComposer.requirements(ctx)
  │  + CompositionalVariant.augmentRequirements()
  ▼
SAF single round  (parallel fetch from aggregation modules, in-process, L1-cached)
  │
  ▼
ScreenComposer.compose(ctx, domainObjects)
  │  calls SectionComposers (pure, domain object → UI tree)
  │  uses typed Builder DSL
  │  attaches RefreshPolicy, DataBinding, Surface, State
  │  link composers may access RequestMetadata.geo for gating → gameState on outbound URIs
  ▼
PresentationalVariant.apply(response)
  │
  ▼
ScreenEnvelope → Jackson serialization → HTTP response + Cache-Control headers
  │
  ▼
CDN stores response (keyed on rewritten URL, TTL from Cache-Control)
```

Single SAF round is the norm. Aggregation modules return pre-joined domain
objects via in-process calls. L1 cache hits resolve in microseconds. If a
screen consistently needs a second round, the aggregation module API is
under-designed.

---

## Capacity Model

### Upstream data source assumptions

The prototype hardcodes most content but the data access patterns reveal the
production upstream topology. These assumptions drive the capacity math.

| Aggregation Module | Assumed Upstream | Response Size | P50 Latency | P99 Latency | Throughput Ceiling | Cache TTL |
|---|---|---|---|---|---|---|
| `game-aggregation` (scoreboard) | Stats / scores service — aggregates live game data from multiple internal sources. Database-backed, joins across services. | ~50 KB | 2s | 4s | 500 RPS | 10s (live), 5m (final) |
| `game-aggregation` (boxscore) | Stats service — per-game detailed boxscore with player stats. Multi-source joins. | ~80 KB | 2s | 4s | 500 RPS | 10s (live), 5m (final) |
| `content-aggregation` | CMS / DAM API — editorial content, articles, story circles. Database + asset pipeline. | ~30 KB | 2s | 4s | 300 RPS | 2m |
| `schedule-aggregation` | Schedule service — season schedule, game times. Database-backed. | ~120 KB | 2s | 4s | 300 RPS | 15m |
| `media-aggregation` | Video / VOD catalog API — playlist metadata, thumbnail URLs. Media platform integration. | ~40 KB | 2s | 4s | 200 RPS | 5m |
| `promo-aggregation` | Campaign / promo service — active banners, CTAs. Rules engine + database. | ~5 KB | 2s | 4s | 300 RPS | 10m |

**Key assumptions:**
- **2s P50, 4s P99 across the board.** These are internal service calls, not
  CDN-backed. Upstreams do database queries, join across sources, hit their
  own dependencies, and serialize large responses. 2s is the planning
  baseline with margin for error.
- Aggregation modules may themselves make 2-3 internal calls to build one
  domain object. The 2s P50 is the **total aggregation latency** including
  those internal joins.
- All upstreams return JSON. No binary/protobuf.
- Auth adds ~50-100ms on first call (OAuth token cached after that).
- Upstream throughput ceilings are low — these are shared services serving
  multiple consumers, not dedicated to SDUI.
- Upstream response sizes are **pre-aggregation** — aggregation modules may
  strip, join, or reshape. Domain object size is ~30-60% of raw upstream
  response.
- **SAF timeout per service: 5s** (2.5× P50). Calls exceeding this are
  circuit-broken.

### Composition cost model

Your intuition is correct: **composition is CPU-bound, aggregation is I/O-bound.**

More precisely:

| Work Type | Bound By | Why |
|---|---|---|
| Atomic tree construction | **CPU** | Building 200-500 typed objects per screen, allocating containers, resolving tokens, wiring bindRefs. Pure computation. |
| Jackson serialization | **CPU + memory** | Serializing the typed object graph to JSON. Allocation-heavy (temporary byte buffers, string encoding). At 50-75 KB average output, this is significant GC pressure. |
| Upstream HTTP calls | **I/O (network)** | Waiting for upstream response bytes. At 2s P50, each cache miss blocks a virtual thread for 2-4s. Virtual threads make this free in terms of platform threads, but the number of concurrent in-flight calls is significant for memory and connection pool sizing. |
| SAF L1 cache reads | **Memory (read)** | Caffeine lookups are ~100ns. Bounded by L1 cache heap footprint, not CPU. |
| SAF L2 cache reads (Redis) | **I/O (network)** | Redis RTT ~0.5-2ms local, ~5ms cross-AZ. Only on L1 miss. |
| Domain object creation in aggregation | **CPU + memory** | Parsing upstream JSON into typed domain objects. Jackson deserialization is allocation-heavy. But this happens only on cache miss, so volume is low. |

**Measured composition cost from prototype (extrapolated to typed model):**

| Screen Type | Sections | Atomic Elements | Estimated CPU Time | Response Size |
|---|---|---|---|---|
| For You (home) | 16 | 300-400 | 3-5ms | 70-80 KB |
| Game Detail (live) | 7-8 | 150-250 | 2-3ms | 45-55 KB |
| Scoreboard (10 games) | 12-14 | 250-350 | 2-4ms | 35-45 KB |
| Boxscore | 1 (deep) | 200-300 | 2-3ms | 55-65 KB |
| Schedule (full season) | 80+ | 500-800 | 5-8ms | 120-140 KB |
| Watch | 1 (deep TabGroup) | 200-300 | 3-4ms | 50-60 KB |

**Weighted average:** Assuming traffic distribution of 40% home, 25% game
detail, 15% scoreboard, 10% boxscore, 5% schedule, 5% other:
- **Weighted CPU time: ~3.2ms per request**
- **Weighted response size: ~58 KB** (~12 KB gzipped)
- **Weighted memory per in-flight request: ~180 KB** (object graph during
  composition, ~3× response size)

### SAF caching impact

SAF's L1 cache is the single biggest throughput multiplier. With collocated
aggregation modules, cache reads are in-process Caffeine lookups (~100ns).

| Cache Tier | Hit Rate (steady state) | Latency | Effect |
|---|---|---|---|
| L1 (Caffeine) — live game data | 85-95% during games | ~100ns | Eliminates almost all upstream calls for scoreboard/boxscore |
| L1 (Caffeine) — editorial/CMS | 95-99% | ~100ns | CMS content changes hourly; 2m TTL means near-perfect hit rate |
| L1 (Caffeine) — schedule | 99%+ | ~100ns | Schedule changes rarely; 15m TTL |
| L2 (Redis) — cross-pod | 80-90% on L1 miss | 0.5-2ms | Prevents duplicate upstream calls across pods |

**Effective upstream call rate at 10K RPS:**

| Aggregation Module | Calls/sec (pre-cache) | L1 Hit Rate | L1 Misses/sec | Collapsing Factor | Actual Upstream Calls/sec |
|---|---|---|---|---|---|
| game (scoreboard) | ~6,500 | 90% | 650 | 10:1 (many screens share same scoreboard) | ~65 |
| game (boxscore) | ~3,500 | 85% | 525 | 5:1 (per-gameId collapsing) | ~105 |
| content | ~4,000 | 97% | 120 | 3:1 | ~40 |
| schedule | ~500 | 99% | 5 | 2:1 | ~3 |
| media | ~500 | 95% | 25 | 2:1 | ~13 |
| promo | ~4,000 | 98% | 80 | 5:1 | ~16 |
| **Total** | **~19,000** | | **~1,405** | | **~242** |

At 10K RPS composition, the upstream APIs see **~250 actual requests/sec** —
well within every upstream's throughput ceiling. SAF request collapsing is
critical here: 650 near-simultaneous L1 misses for the same scoreboard
collapse to ~65 actual upstream calls.

### Throughput model

**CPU budget:**

At 3.2ms average CPU per request, one CPU core can handle:
```
1000ms / 3.2ms = ~312 requests/sec/core
```

For 10K RPS:
```
10,000 / 312 = ~32 CPU cores needed for composition alone
```

Add overhead (GC, serialization, framework, SAF orchestration): **~1.5× multiplier**
```
32 × 1.5 = ~48 CPU cores effective requirement
```

**Memory budget:**

| Component | Per-Pod Memory | Notes |
|---|---|---|
| JVM heap (composition working set) | 200-400 MB | In-flight request objects. At 10K RPS with 3.2ms/req, ~32 concurrent compositions × 180 KB = ~6 MB working set. But GC headroom needs 50-100× live set. |
| SAF L1 cache (Caffeine) | 500 MB - 1 GB | ~10,000 cached entries × 50-80 KB average = 500-800 MB. This is the dominant memory consumer. |
| In-flight upstream responses | 50-100 MB | At P99: ~100 concurrent upstream calls × 50-120 KB response = 5-12 MB live data. But Jackson parse buffers add ~5-10× overhead during deserialization. |
| JVM metaspace + stack | 200-300 MB | Class metadata, virtual thread stacks (~1 KB each, hundreds active during cache miss storms) |
| Response serialization buffers | 100-200 MB | Jackson output buffers, gzip compression buffers, Netty write buffers |
| **Total per pod** | **2 - 3 GB heap** | ZGC recommended for low-pause at this scale |

**I/O budget (virtual threads):**

With 2s P50 upstream latency, the concurrency math changes significantly:

| Metric | Value |
|---|---|
| Actual upstream calls/sec (post-cache, post-collapse) | ~250/sec across fleet |
| Per-pod upstream calls/sec (10 pods) | ~25/sec/pod |
| Upstream P50 latency | 2s |
| Upstream P99 latency | 4s |
| **Concurrent in-flight upstream calls per pod (P50)** | **25/sec × 2s = ~50 concurrent** |
| **Concurrent in-flight upstream calls per pod (P99)** | **25/sec × 4s = ~100 concurrent** |
| Virtual thread cost per blocked call | ~1 KB stack |
| Virtual thread memory at P99 | ~100 KB per pod (trivial) |
| HTTP connection pool per upstream service | 50-100 connections per pod |
| Platform threads needed | ~4-8 (carrier threads for virtual thread scheduler) |
| Redis connections (L2 cache) | Pool of 20-50 per pod |

Virtual threads make the thread dimension free, but **connection pool sizing
matters.** At P99, each pod holds ~100 concurrent upstream HTTP connections.
With 6 upstream services, that's ~17 concurrent connections per upstream per
pod. Connection pools of 50-100 per service give comfortable headroom.

**Timeout and deadline impact:**

With 5s SAF timeout (2.5× P50), a worst-case cache miss on a critical
service adds 5s to the SDUI response time. This is unacceptable for client
UX. The defense layers:

1. **L1 cache at 90%+ hit rate** — most requests never wait for an upstream
2. **SAF stale-if-error** — on timeout, serve the previous cached response
   (stale but fast) rather than blocking the client for 5s
3. **Non-critical services degrade gracefully** — content, media, promo
   sections are omitted on timeout, not blocking
4. **Only game-aggregation is critical** — and it has a 10s cache TTL during
   live games, meaning each pod makes ~1 scoreboard call per 10s (not 25/sec)

**Realistic per-pod upstream concurrency during live games:**

| Service | TTL | Calls/10s/pod | Concurrency (at 2s latency) |
|---|---|---|---|
| game (scoreboard) | 10s | 1 | ~0.2 |
| game (boxscore, 10 active games) | 10s | 10 | ~2 |
| content | 2m | 0.08 | ~0.02 |
| schedule | 15m | 0.01 | ~0.002 |
| media | 5m | 0.03 | ~0.006 |
| promo | 10m | 0.02 | ~0.004 |
| **Total per pod** | | **~11** | **~2.2 concurrent** |

With SAF collapsing, the real concurrency is very low. The 50-100 concurrent
figure only applies during L1 TTL expiry storms (all pods expire the same
key simultaneously). SAF request collapsing mitigates this to 1 actual call
per key per pod.

### Resource sizing

**Per-pod sizing (assumes Kubernetes or equivalent):**

| Resource | Value | Rationale |
|---|---|---|
| CPU | 4 vCPU | At ~312 req/sec/core, handles ~1,250 RPS per pod |
| Memory | 4 GB heap + 1 GB off-heap | L1 cache + in-flight upstream responses + composition working set + GC headroom |
| JVM flags | `-XX:+UseZGC -Xmx4g -Xms4g` | ZGC for <1ms pause times. Extra GB vs previous estimate covers in-flight upstream response buffering at 2s latency. |

**Fleet sizing for 10K RPS:**

| Scenario | Pods | Total CPU | Total Memory | Notes |
|---|---|---|---|---|
| **Steady state** (10K RPS) | 10 | 40 vCPU | 50 GB | ~1K RPS/pod with headroom |
| **Peak** (15K RPS, game night) | 14 | 56 vCPU | 70 GB | 1.5× headroom |
| **N-1 resilience** | 12 | 48 vCPU | 60 GB | Survives 1 pod failure at 10K RPS |
| **Burst** (20K RPS, playoffs) | 18 | 72 vCPU | 90 GB | HPA scales on CPU |

**Autoscaling policy:** HPA on CPU utilization, target 60%. At 60% of 4 vCPU
= 2.4 cores utilized, each pod handles ~750 RPS with comfortable headroom.
Scale-up trigger at 65%, scale-down at 45%.

### Throughput ceiling analysis

**What limits us before CPU does:**

| Bottleneck | Ceiling | Why | Mitigation |
|---|---|---|---|
| Upstream throughput ceilings | 200-500 RPS per service | Shared internal services, not dedicated to SDUI | SAF collapsing + caching → ~250 total actual calls/sec. Well under ceiling if upstreams are healthy. |
| Upstream latency (2s P50) on cache miss | Client sees 2s+ for cold-cache screens | First request after TTL expiry waits for full upstream round-trip | SAF stale-if-error serves previous value instantly; background refresh updates cache. Critical path for UX. |
| L1 TTL expiry storm | All pods expire same key → burst of concurrent upstream calls | 10 pods × 1 call = 10 simultaneous calls per key | SAF request collapsing (per-pod) + L2 Redis (cross-pod) reduce to 1 actual call per expiry cycle |
| L1 cache churn (live games) | ~1M entries/sec Caffeine throughput | Caffeine benchmark limit | At ~19K reads/sec + ~1.4K writes/sec, we're at <3% of Caffeine's capacity |
| Network egress | ~580 MB/sec at 10K RPS × 58 KB avg | NIC / load balancer | Gzip reduces to ~120 MB/sec. Standard 10 Gbps NIC handles this |
| Jackson serialization | ~50K objects/sec/core (estimate) | Allocation pressure + GC | At 10K RPS × 350 avg nodes = 3.5M nodes/sec → ~70 cores. ZGC + object pooling mitigates |
| Connection pool exhaustion | HTTP pool drained during upstream slow period | 2s latency × burst of cache misses | Pool of 50-100 per service per pod, SAF bulkhead caps concurrent calls |

**Revised throughput ceiling with Jackson as the true bottleneck:**

Jackson serialization is actually the tightest constraint, not raw tree
construction. Serializing 350 typed objects per response at 10K RPS = 3.5M
object serializations/sec. With ZGC and 48 cores:

```
Practical ceiling: ~15-18K RPS before serialization GC pressure
dominates
```

Beyond 18K RPS, consider:
- Response payload caching (cache the serialized JSON bytes for identical
  composition inputs — CDN-level concern)
- Partial screen caching (cache frequently-reused section JSON fragments)
- Moving to a more allocation-efficient serializer (jackson-blackbird,
  or pre-computed byte arrays for static sections)

### Likely production limit

**Conservative estimate: 12-15K RPS per cluster (10-12 pods, 4 vCPU / 5 GB each)**

This accounts for:
- Real-world GC pauses (ZGC <1ms but not zero)
- Traffic skew (game nights concentrate load on scoreboard/game-detail)
- L1 cache misses during TTL expiry storms (all pods expire same key
  simultaneously → burst of 2s upstream calls)
- Serialization allocation pressure at sustained load
- HPA lag during burst ramp-up (30-60s to provision new pods)
- **2s upstream latency on every cache miss** — in-flight response buffering
  during storms is the new secondary memory constraint

**The critical UX constraint: cache miss latency.**

With 2s upstream P50, a cold-cache request takes:
```
2s (upstream) + 3ms (composition) + 1ms (serialization) ≈ 2s total
```

This is **unacceptable as a client-facing response time.** The defense is
SAF's stale-if-error + stale-while-revalidate cache strategy:

| Scenario | Client Sees | How |
|---|---|---|
| L1 cache hit (90%+) | ~5ms | Cached domain object → composition → response |
| L1 miss, stale available | ~5ms | Serve stale value, background-refresh from upstream |
| L1 miss, no stale (cold start) | ~2s | Unavoidable. First request after deploy or cache flush. |
| L1 miss, upstream timeout (5s) | ~5ms + ErrorState | Stale-if-error serves last good value. If no stale: ErrorState section. |

**The 2s cold-start penalty is the price of slow upstreams.** Mitigation:
- **Cache warming on pod startup** — pre-fetch critical keys (scoreboard,
  tonight's games) before the pod enters the load balancer
- **L2 Redis** — new pods read from Redis (0.5-2ms) instead of upstream (2s)
  if another pod has already cached the value
- **Stale-while-revalidate** — the client never sees the 2s latency after
  the first request, because TTL expiry triggers background refresh while
  serving the stale value

With cache warming + L2 + stale-while-revalidate, the 2s upstream latency
is invisible to clients in steady state. It only surfaces on:
1. First request after a full deploy (cache empty across all pods + Redis)
2. First request for a brand-new entity (new game ID, new article)

**Production estimate with CDN layer (Pattern 11): 100-200K+ user-facing RPS**

With the cache key strategy from Pattern 11 (identity and metadata in
headers, cohort-resolved geo, ~30-50K keys per screen), the CDN absorbs
the majority of traffic:

| Screen Type | CDN TTL | CDN Hit Rate | Origin Miss Rate |
|---|---|---|---|
| Static (standings, schedule) | 30-60s | 90-95% | 5-10% |
| Editorial (home, news) | 30s | 85-90% | 10-15% |
| Live (scoreboard, game-detail) | 10s | 75-85% | 15-25% |
| **Weighted average** | | **80-90%** | **10-20%** |

At 100K user-facing RPS with 85% CDN hit rate:
- CDN serves: 85K RPS (edge, <50ms latency)
- Origin receives: 15K RPS → within the 12-15K cluster ceiling
- 3-4 pods handle steady state; 6-8 for peak game nights

At 200K user-facing RPS (playoff peak):
- CDN serves: 170K RPS
- Origin receives: 30K RPS → requires ~24 pods or CDN TTL increase

The 2s upstream latency is invisible to CDN-served requests. Users see
edge latency (<50ms) for cache hits, and the origin's stale-while-revalidate
strategy ensures even CDN misses are fast (the origin serves from L1 cache
while background-refreshing from upstream).

**The CDN is the primary scaling lever, not pod count.** Adding pods gives
linear capacity growth. The CDN gives multiplicative capacity growth — each
10% CDN hit rate improvement reduces origin load by the same 10% of total
user traffic.

### Correction on your characterization

> Atomic creation is CPU-bound and data aggregation is memory-bound or
> I/O-bound

Almost right. More precisely:

| Work | Primary Bound | Secondary Bound | Notes |
|---|---|---|---|
| Atomic tree construction | **CPU** | Memory (allocation) | Building the typed object graph. Pure computation + heap allocation. |
| Jackson serialization | **CPU + Memory** (GC) | — | This is the hidden bottleneck. Serializing 350 objects × 50-75 KB output generates significant allocation churn. ZGC is essential. |
| Upstream data fetch | **I/O (network)** | — | Dominated by upstream latency (2s P50). Virtual threads make thread cost zero, but each in-flight call holds ~50-120 KB of response buffer memory for the duration. At 2s hold time, this adds up during cache miss storms. |
| SAF L1 cache read | **Memory (read)** | — | Caffeine is CPU-trivial. Bounded by cache heap footprint. |
| SAF L2 cache read (Redis) | **I/O (network)** | — | Redis RTT 0.5-2ms. Critical for avoiding 2s upstream calls on L1 miss when another pod already has the value. |
| Aggregation module transformation | **CPU** | Memory | Parsing upstream JSON → domain objects. Jackson deserialization allocates heavily. At 2s upstream latency, ~500 concurrent parses in progress across the fleet at any moment. |

The key insight: **aggregation is I/O-bound on cache miss but effectively
free on cache hit** (which is 90%+ of the time). At steady state, the
system is CPU-bound by composition and serialization, not I/O-bound by
upstream calls. Memory is the secondary constraint — not from composition
objects, but from **in-flight upstream response buffering during cache miss
storms** at 2s latency.

---

## Seam Enforcement

Three enforcement layers catch violations at different stages. The Gradle
module split is the one that matters most — it's the only enforcement that
cannot be overridden with `@SuppressWarnings` or a well-intentioned comment.

### Layer 1: Gradle Module Dependencies (compile-time)

The Gradle module structure in Pattern 9 makes wrong dependencies a
**compilation error**. The critical constraints:

```kotlin
// sdui-composition/build.gradle.kts
dependencies {
    implementation(project(":sdui-model"))  // interfaces + domain objects ONLY
    // NO dependency on sdui-aggregation-* modules
}
```

```kotlin
// sdui-aggregation-game/build.gradle.kts
dependencies {
    implementation(project(":sdui-model"))  // domain objects it must return
    // NO dependency on sdui-composition
    // NO dependency on other sdui-aggregation-* modules
}
```

```kotlin
// sdui-app/build.gradle.kts (assembly point — wires everything at runtime)
dependencies {
    implementation(project(":sdui-model"))
    implementation(project(":sdui-composition"))
    implementation(project(":sdui-aggregation-game"))
    implementation(project(":sdui-aggregation-content"))
    implementation(project(":sdui-aggregation-schedule"))
    implementation(project(":sdui-aggregation-media"))
}
```

**What this makes impossible at compile time:**

| Violation | Why it fails |
|---|---|
| Composer calls `NbaStatsClient.getBoxscore()` | `sdui-composition` has no dependency on `sdui-aggregation-game` |
| Aggregation module builds `SectionEnvelope` | `sdui-aggregation-game` has no dependency on `sdui-composition` |
| `game-agg` calls `content-agg` internals | No lateral dependencies between aggregation modules |
| Upstream HTTP client leaks into composition | Client class not on composition classpath |

### Layer 2: ArchUnit Tests (CI-time)

ArchUnit catches structural rules that Gradle modules cannot express — type
usage restrictions, annotation placement, and semantic boundaries.

```java
class ArchitectureSeamTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.nba.sdui");

    @Test
    void composition_layer_never_uses_raw_jackson_nodes() {
        noClasses()
            .that().resideInAPackage("..composition..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.fasterxml.jackson.databind.node.ObjectNode")
            .orShould().dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.fasterxml.jackson.databind.node.ArrayNode")
            .check(classes);
    }

    @Test
    void composers_do_not_call_saf_orchestrator_directly() {
        noClasses()
            .that().resideInAPackage("..composition..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.nba.saf.orchestrator.OrchestratorFactory")
            .orShould().dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.nba.saf.orchestrator.ServiceOrchestrator")
            .check(classes);
    }

    @Test
    void aggregation_modules_do_not_expose_http_endpoints() {
        noClasses()
            .that().resideInAPackage("..aggregation..")
            .should().beAnnotatedWith(RestController.class)
            .orShould().beAnnotatedWith(Controller.class)
            .check(classes);
    }

    @Test
    void aggregation_modules_do_not_depend_on_each_other() {
        slices()
            .matching("..aggregation.(*)..")
            .should().notDependOnEachOther()
            .check(classes);
    }

    @Test
    void domain_objects_have_no_framework_dependencies() {
        noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "com.nba.saf..",
                "..composition..",
                "..aggregation.game..",
                "..aggregation.content..",
                "..aggregation.schedule..",
                "..aggregation.media..")
            .check(classes);
    }

    @Test
    void aggregation_interfaces_live_in_model_module() {
        classes()
            .that().areInterfaces()
            .and().haveSimpleNameEndingWith("Aggregation")
            .should().resideInAPackage("..aggregation.api..")
            .check(classes);
    }
}
```

**What ArchUnit catches that Gradle cannot:**

| Rule | Violation it prevents |
|---|---|
| No `ObjectNode` in composition | Typed model bypass |
| No direct SAF orchestrator access | Composers skipping the pipeline |
| No `@RestController` in aggregation | Role confusion (agg module exposing endpoints) |
| No lateral aggregation deps | Hidden coupling between modules |
| Domain objects framework-free | Model purity — no Spring/SAF leakage |

### Layer 3: Visibility (design-time)

Aggregation module internals are **package-private**. Only the interface
(which lives in `sdui-model`) is public.

```java
// sdui-model — PUBLIC interface (the contract)
public interface GameAggregation {
    GameSummary getGameDetail(String gameId);
    ScoreboardSummary getTonightScoreboard();
}

// sdui-aggregation-game — package-private implementation
@Component
class GameAggregationModule implements GameAggregation {
    private final NbaStatsClient statsClient;  // also package-private
    // ...
}

// sdui-aggregation-game — package-private upstream client
@Component
class NbaStatsClient {
    // invisible outside this package
}
```

Even within `sdui-app` (which has all modules on its classpath), the
implementation classes cannot be referenced directly — they're package-private.
Spring discovers them via component scanning; application code uses only the
interface.

### Enforcement cascade

```
Developer writes code
        │
        ▼
  Can it compile?  ───No──→  Gradle module boundary violation
        │                     (wrong module on classpath)
        Yes
        │
        ▼
  Does CI pass?    ───No──→  ArchUnit rule violation
        │                     (wrong type usage or annotation)
        Yes
        │
        ▼
  Can it access    ───No──→  Package-private visibility
  the internal?               (implementation class not public)
        │
        Yes
        │
        ▼
  Code is within seams ✓
```

Gradle modules are the hardest boundary. ArchUnit catches semantic drift.
Visibility prevents reaching into internals even with classpath access.

---

## What Stays From the Prototype

These patterns are already correct and carry forward unchanged:

- **Single responsibility within the monolith** — composition layer composes
  UI; aggregation modules own upstream data; SAF owns transport infrastructure
- **Request envelope contract** — bracket-notation params, GET/POST duality,
  deterministic key ordering. Production evolution: split into
  `CompositionContext` (URL cache key) and `RequestMetadata` (headers) per
  Pattern 11.
- **Dual-mounted endpoints** — `@GetMapping` + `@PostMapping` on same handler
- **Edge cohort resolution** — zip → DMA cohort rewrite at CDN edge, already
  operational. Pattern 11 leverages this for CDN-cacheable cache keys.
- **Surface catalogue** — finite set of shared outer-chrome presets
- **Refresh policy vocabulary** — static, sse, poll, parameterized
- **Navigation as composition** — server-built, client-rendered
- **Token registry** — loaded from schema at boot, validated at composition time
- **Section envelope shape** — id, type, analyticsId, data{ui, content,
  dataBinding}, surface, refreshPolicy
