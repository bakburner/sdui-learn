# Plan: Server-Side Section-Level Caching

> Source: SDUI composition responses have wider cache-key spaces than raw JSON
> feeds, requiring server-side caching at finer granularity than full-screen
> responses.

> **Capacity model**: The traffic projections and CPU/fleet impact of section-level
> caching are integrated into the production server patterns document:
> `nba-client-backend/docs/plans/plan-production-server-patterns.md` — see the
> "Cache key cardinality", "Section-level composition caching", "Throughput model",
> and "Fleet sizing" sections. This plan covers architecture, requirements, and
> implementation tasks; the production patterns doc covers derived capacity metrics.

## Summary

Implement three-layer server-side caching for SDUI composition: upstream data
caching (SAF `ServiceCall`), composed section fragment caching (section builder
output), and uncached screen assembly. This keeps composition latency low even
when CDN hit rates drop due to platform × variant × locale key proliferation,
and is complementary to the client-side caching in `plan-caching-offline.md`.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Upstream data caching | Gap | `StatsApiClient` uses OkHttp with no application-level caching; every request hits upstream |
| Section fragment caching | Gap | Composers rebuild every section from scratch on each request |
| Screen assembly | Built | Composers assemble `sections[]` array; no caching needed at this layer |
| HTTP response caching | Built | `Cache-Control` headers set per endpoint in `SduiController` |
| SAF integration | Gap | Prototype server uses throwaway OkHttp; SAF migration not yet started |
| Metrics | Gap | No cache hit/miss observability for composition |

## Problem Statement

## Traffic Projections

### Current baseline

The current system sees ~5K RPS at origin after two caching layers absorb
the majority of client demand:

- **CDN edge**: ~80% offload (aggressive 5-minute TTLs for most feeds,
  15-second polling for live game data)
- **Internal Varnish proxy**: ~60% offload of what passes the CDN

Total client-facing demand before caching: ~62.5K RPS
(`5K / 0.20 / 0.40 ≈ 62.5K`). Combined offload: ~92%.

### SDUI cache-key fragmentation

SDUI adds envelope dimensions that multiply on top of the existing business
parameters (gameId, date, teamId, playerId, tabId, etc.) that the current
system already uses:

| Envelope dimension | Approximate cardinality |
|--------------------|------------------------|
| `deviceClass` | 3 (phone, tablet, desktop) |
| `capabilities` | 2–4 (sse, onFocus combos) |
| `locale` | 3–5 |
| `schemaVersion` | 2–3 (during rollouts) |
| `experiments` | 2–8 (per-screen, not global) |

These dimensions multiply the business-param keyspace by ~100–500× in theory.
In practice, traffic is heavily Zipfian — a small number of key combinations
absorb the vast majority of requests:

- **~70% of traffic** clusters on `phone × en × control/no-experiment` —
  effectively the same keyspace as today. CDN offload for this majority
  slice stays high at ~75%.
- **~30% is long-tail** (tablet, other locales, active experiment variants).
  CDN offload for this slice is poor, ~10–20%.
- Experiments are typically scoped to specific screens, not applied globally,
  so the experiment multiplier affects a subset of endpoints, not all traffic.

### Projected origin RPS

The new architecture eliminates the internal Varnish proxy layer. Blended CDN
offload under SDUI: `(0.70 × 0.75) + (0.30 × 0.15) ≈ 57%`.

| Scenario | CDN offload | Origin RPS |
|----------|------------|------------|
| Current (CDN + Varnish) | 92% | ~5K |
| SDUI optimistic (Vary-header tuning, edge normalization) | ~65% | ~22K |
| **SDUI realistic (blended Zipfian)** | **~57%** | **~27K** |
| SDUI pessimistic (high experiment coverage) | ~45% | ~34K |

**Design target: 25–30K RPS at origin** — roughly 5–6× the current origin load,
driven by losing the Varnish layer and CDN fragmentation from envelope dimensions.

### Why section-level caching is a hard requirement

Without server-side caching below the HTTP layer, 27K RPS at origin with
3–5 upstreams per screen fans out to **80K–135K upstream calls per second**,
and every request pays full composition CPU for sections whose inputs haven't
changed.

With section-level caching:

- **Upstream data** (Layer 1) is fetched once per TTL interval per unique key,
  regardless of how many origin requests share that data. Request collapsing
  further deduplicates concurrent cache misses into a single backend call.
- **Section fragments** (Layer 2) are composed once per unique input, then served
  from L1 Caffeine (microseconds) or L2 Redis (1–5ms). Non-live sections
  (content rails, promo banners, editorial) hit ratios >95%.
- **Effective upstream fan-out** drops to the number of unique upstream keys
  that expire per second — a few hundred calls/second even at 27K RPS origin.

This makes section-level caching the application-layer replacement for the
CDN + Varnish offload that SDUI's wider key space erodes.

## Architecture

```
Layer 1: Upstream Data         Layer 2: Section Fragments       Layer 3: Screen Assembly
┌──────────────────────┐      ┌──────────────────────────┐     ┌─────────────────────┐
│ SAF ServiceCall      │      │ CachedSectionBuilder     │     │ Composer             │
│                      │      │                          │     │                      │
│ stats-api:boxscore   │─────▶│ gamepanel:{gameId}:{dc}  │────▶│ sections[] array     │
│   TTL: 10s (live)    │      │   TTL: 10s               │     │ + navigation         │
│   TTL: 5min (post)   │      │                          │     │ + variants           │
│                      │      │ content-rail:{locale}    │────▶│ + traceId            │
│ capi:content-rail    │─────▶│   TTL: 10min             │     │                      │
│   TTL: 10min         │      │                          │     │ (always fresh)       │
│                      │      │ promo-banner:{locale}    │────▶│                      │
│ capi:promo           │─────▶│   TTL: 30min             │     │                      │
│   TTL: 30min         │      │                          │     │                      │
└──────────────────────┘      └──────────────────────────┘     └─────────────────────┘
  L1 Caffeine + L2 Redis        L1 Caffeine + L2 Redis           No caching
```

## Requirements

- [ ] **REQ-1**: Upstream data fetches cached via SAF `ServiceCall` with per-service TTLs
- [ ] **REQ-2**: Composed section fragments cached via SAF `TwoTierCacheService` with deterministic cache keys
- [ ] **REQ-3**: Screen assembly remains uncached — variant selection and section ordering always run fresh
- [ ] **REQ-4**: Cache keys are deterministic across pods — same inputs produce same key on every instance
- [ ] **REQ-5**: Live-game sections use short TTLs (5–15s); static sections use long TTLs (10–30min)
- [ ] **REQ-6**: Stale-if-error strategy on upstream calls so composition degrades gracefully when backends fail
- [ ] **REQ-7**: Cache hit/miss metrics per layer and per section type, published to Micrometer

## Tasks

### Phase 1: SAF Migration of Upstream Clients

Prerequisite: SDUI composition server is rebased onto SAF (replacing throwaway
OkHttp `StatsApiClient`).

- [ ] Replace `StatsApiClient` with SAF `ServiceCall` definitions for each upstream
- [ ] Define cache keys and TTLs per upstream source:
  - `boxscore:{gameId}` — 10s live, 5min post/final
  - `scoreboard:today` — 30s
  - `content-rail:{locale}:{region}` — 10min
  - `promo:{locale}` — 30min
  - `broadcaster:{gameId}` — 1hr
  - `player-stats:{gameId}` — 10s live, 5min post
- [ ] Set `CacheStrategyName.STALE_IF_ERROR` on all upstream calls
- [ ] Set `failOnError(false)` on non-critical upstreams (promo, content-rail)
- [ ] Verify request collapsing activates for concurrent identical upstream calls

### Phase 2: Section Fragment Cache Service

- [ ] Create `CachedSectionBuilder` service wrapping SAF `TwoTierCacheService`
- [ ] Define cache key format: `section:{sectionType}:{contentHash}:{deviceClass}:{locale}`
  - `contentHash` is a fast hash (murmur3 or xxhash) of the upstream input data relevant to that section
  - Keeps keys deterministic without serializing entire input objects as keys
- [ ] Integrate into composers — wrap each `buildXxxSection()` call:
  ```java
  ObjectNode section = cachedSections.getOrCompose(
      "section:gamepanel:" + hash(game) + ":" + deviceClass,
      ttlForGameState(gameState),
      () -> buildGamePanelSection(game, gameId));
  ```
- [ ] Apply to all section builders, with TTL varying by section volatility:
  - Live-updating sections (game panel, stat line, boxscore): match upstream TTL (5–15s)
  - Editorial sections (content rail, promo banner): 10–30min
  - Static sections (error state, section headers): 1hr+
- [ ] Sections with `refreshPolicy: "sse"` still cache the initial composition; SSE data binding updates happen client-side without re-composing

### Phase 3: Cache Key Determinism Validation

- [ ] Add integration test: same inputs on two requests produce identical cache keys
- [ ] Add integration test: different deviceClass values produce different cache keys
- [ ] Add integration test: section fragment cache returns byte-identical JSON across cache hits
- [ ] Verify no non-determinism in Jackson serialization (field ordering, number formatting)

### Phase 4: Metrics & Observability

- [ ] Tag all cache operations with `layer=upstream|section`, `sectionType`, `gameState`
- [ ] Publish to Micrometer:
  - `sdui.cache.hit{layer,sectionType}` / `sdui.cache.miss{layer,sectionType}`
  - `sdui.composition.duration{sectionType,cached=true|false}`
  - `sdui.upstream.duration{serviceName,cached=true|false}`
- [ ] Add Grafana dashboard panel: cache hit rate by section type, composition p50/p95 with and without cache
- [ ] Alert on sustained cache miss rate > 80% for any section type (indicates key drift or TTL misconfiguration)

### Phase 5: TTL Tuning & Operational Controls

- [ ] Externalize TTLs to YAML config (SAF `saf.services.*.cache.ttl` pattern)
- [ ] Add per-section-type TTL overrides in config:
  ```yaml
  sdui:
    section-cache:
      game-panel:
        ttl-live: 10s
        ttl-post: 300s
      content-rail:
        ttl: 600s
      promo-banner:
        ttl: 1800s
  ```
- [ ] Support cache bypass header (`X-SDUI-Cache-Bypass: true`) for debugging/admin
- [ ] Support per-section-type cache invalidation via actuator endpoint

## Dependencies

- SAF migration of the SDUI server (not yet planned — currently throwaway code)
- `plan-observability.md` Phase 4 (server dashboard) for Grafana integration
- `plan-caching-offline.md` is complementary — client-side; this plan is server-side

## Interaction with Client-Side Caching

Client-side caching (plan-caching-offline.md) caches the **full screen response**
for offline/cold-start. Server-side section caching reduces the **cost of
producing** that response. They compose well:

- Client sends request → CDN miss → server checks section fragment cache →
  section cache miss → server checks upstream data cache → upstream miss → fetch
- On subsequent request: CDN may hit; if not, section fragments and upstream
  data are likely cached, producing a fast server response that the client
  then caches locally

## Open Questions

- [ ] Should section fragment cache keys include `schemaVersion`? Only needed if the same section type produces structurally different output across schema versions.
- [ ] Should `contentHash` use the raw upstream JSON bytes or a normalized/sorted representation? Raw is faster; normalized is more stable across upstream serialization changes.
- [ ] What is the L1 (Caffeine) size budget per pod for section fragments? Sections are ~2–10KB each; 1000 cached sections ≈ 2–10MB.
- [ ] Should section fragments be cached as serialized JSON bytes (saves re-serialization) or as `ObjectNode` (saves re-parsing for assembly)? Bytes are more cache-efficient; ObjectNode avoids a parse on hit.
