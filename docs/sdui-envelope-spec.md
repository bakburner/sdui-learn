# SDUI Request Envelope & Headers Spec

> **Status:** v1 implemented across server, Android, iOS, and web.
> Edge worker and experiment filtering are roadmap items.

## Overview

This spec defines the request contract for SDUI composition endpoints. It
separates client claims (the envelope in the query string) from
server-resolved context (edge-injected headers) and uses the canonical query
string as the cache key rather than HTTP `Vary` semantics.

The guiding principle: **the query string is the cache key.** Anything that
affects composition output lives in the query string. Anything that doesn't —
analytics, tracing, geo, identity — travels as headers outside the cache key.

## Versioning Layers

Two independent version dimensions:

| Layer | Location | Purpose | Change Frequency |
|---|---|---|---|
| Endpoint version | URL path (`/v1/`, `/v2/`) | Breaking wire-contract changes | Years |
| Schema version | Query param (`schemaVersion`) | Section-type / rendering contract evolution | Months |

Old app releases pin to an endpoint version indefinitely. Within an endpoint
version, schema evolves additively. A breaking change to envelope shape,
response envelope, or error format forces a new endpoint version.

### Rationale

Putting the version in the URL path rather than a header or media type means:

- CDN cache keys naturally partition by contract version.
- Old clients hitting `/v1/` and new clients hitting `/v2/` never collide.
- Server routing is compile-time (`@GetMapping("/v1/sdui/...")`) rather than
  runtime content negotiation.

## Client Request

### URL Shape

Per-surface endpoints with explicit controller methods. No generic surface
dispatcher.

```
GET /v1/sdui/screen/scoreboard?{canonical-query-string}
GET /v1/sdui/screen/game-detail/{gameId}?{canonical-query-string}
GET /v1/sdui/screen/for-you?{canonical-query-string}
GET /v1/sdui/screen/watch?{canonical-query-string}
GET /v1/sdui/screen/games?{canonical-query-string}
GET /v1/sdui/screen/schedule?{canonical-query-string}
GET /v1/sdui/screen/home?{canonical-query-string}
GET /v1/sdui/screen/leaders?{canonical-query-string}
GET /v1/sdui/screen/refresh/{handlerId}?{canonical-query-string}
GET /v1/sdui/screen/init
```

Per-surface routes are intentional: surface-specific path parameters
(`{gameId}`), per-surface caching policies, per-surface experiment registries,
and per-surface logging/metrics all attach naturally to dedicated controllers.
A generic `/{surface}` dispatcher would force runtime branching for what are
compile-time concerns.

Every route is dual-mounted as `@GetMapping` **and** `@PostMapping` to the
same handler. A GET-only or POST-only composition endpoint is a contract bug.

### Query String (cache-keyed)

Bracket-notation encoded, UTF-8, percent-encoded brackets (`%5B`/`%5D`),
lowercase hex. Deterministic key ordering across platforms for CDN cache
key stability.

```
locale=en-US
&schemaVersion=1.0
&platform[deviceClass]=phone
&platform[capabilities][sse]=true
&platform[capabilities][onFocus]=true
&market[cohort]=US_NY_METRO
&experiments[gd_tab_order_v2]=variant_b
&experiments[gd_score_card_v3]=control
```

#### Field Definitions

| Field | Required | Notes |
|---|---|---|
| `locale` | yes | BCP 47. Region required when content varies by region. |
| `schemaVersion` | yes | Within-endpoint contract version. Major.minor. |
| `platform[deviceClass]` | yes | `phone` \| `tablet` \| `tv` \| `desktop` \| `web`. Affects layout/sections. |
| `platform[capabilities][*]` | as applicable | Runtime capability flags (e.g. `sse`, `onFocus`). Affects response shape. |
| `market[cohort]` | yes | Edge-resolved or client-attested market cohort (e.g. `US_NY_METRO`, `INTL_CA`, `MARKET_UNKNOWN`). Encodes country. Drives regional content, blackouts, broadcast territory logic. Trust established via app attestation. |
| `experiments[<key>]` | optional | Client-asserted Amplitude assignments. Edge filters per surface (roadmap). |

#### Excluded from the Query String

| Field | Reason |
|---|---|
| `platform[name]` | SDUI is platform-agnostic; renderer concern, not composition concern. Moved to `X-Platform` header for analytics. |
| `platform[appVersion]` | Replaced by `schemaVersion` for contract purposes. App version is analytics-only. Moved to `X-App-Version` header. |
| `platform[osVersion]` | Does not affect response. Analytics-only. Moved to `X-OS-Version` header. |
| `device[zipCode]` | Raw zip is not a composition input. Market cohort is the resolved signal. |
| `device[countryCode]` | Subsumed by `market[cohort]`, which encodes country (e.g. `US_*`, `INTL_CA`). |

#### Rationale: What's In vs. Out

The query string is the CDN cache key. Every field in it multiplies the cache
keyspace. The rule is simple: **if a field affects composition output, it's in
the query string; if it doesn't, it's a header.**

- `locale` and `schemaVersion` directly affect which content and which contract
  the response uses — they stay.
- `deviceClass` affects layout (horizontal vs. vertical forms, card sizes) — it
  stays.
- `capabilities` affect response shape (SSE channels included or omitted) — they
  stay.
- `experiments` affect composition branching — they stay.
- `platform[name]` (`ios`/`android`/`web`) does **not** affect composition output.
  The server composes based on `deviceClass`, not platform identity. Keeping it
  in the query would triple the cache keyspace with no composition benefit.
- `appVersion` and `osVersion` are analytics signals. The composition contract
  is governed by `schemaVersion`, not the native app version.
- `market[cohort]` affects composition (regional content, blackouts, broadcast
  territories) so it belongs in the query string. Trust is established via app
  attestation (Play Integrity / App Attest), not edge IP resolution. The cohort
  encodes country, so a separate `countryCode` param is unnecessary.
- Raw geo (`zipCode`, `countryCode`) is excluded — `market[cohort]` is the
  composition-relevant signal. Raw geo is available in `X-Device-Geo` for
  analytics only.

### GET/POST Fallback

GET is the default method. When the combined query string (envelope + user
params) exceeds **8192 characters**, the client switches to POST with the
same envelope shape in a JSON body. User-supplied filter params (e.g. from
Form `paramBindings`) always ride the URL query string regardless of method
so the server reads them through `@RequestParam` on both paths.

```
// GET (normal)
GET /v1/sdui/screen/refresh/stats-leaders?perMode=Totals&season=2025-26&locale=en&...

// POST (oversized envelope)
POST /v1/sdui/screen/refresh/stats-leaders?perMode=Totals&season=2025-26
Content-Type: application/json
{
  "locale": "en",
  "schemaVersion": "1.0",
  "platform": {
    "deviceClass": "phone",
    "capabilities": { "sse": true }
  },
  "market": {
    "cohort": "US_NY_METRO"
  },
  "experiments": { ... }
}
```

#### Rationale: GET-First

- GET responses are cacheable by CDN and browser. POST responses are not.
- The 8192-char threshold is conservative — well under common URL limits
  (8KB for most CDNs, 16KB for Akamai).
- POST fallback exists only because experiment assignments can grow large
  (100+ experiments × long variant IDs). Once edge experiment filtering is
  live, POST will be rare.

### Percent-Encoding

All platforms use RFC-3986 percent-encoding (not form-urlencoding):

- Spaces → `%20` (not `+`)
- Brackets → `%5B` / `%5D`
- Unreserved set (`A-Z a-z 0-9 - _ . ~`) passes through raw
- Lowercase hex digits (`%5b` is wrong, `%5B` is correct)

This matters because the CDN cache key is byte-compared. If Android encodes
spaces as `+` and iOS encodes them as `%20`, identical requests produce
different cache entries. Every platform's `RequestEnvelopeBuilder` uses the
same encoding rule.

### Deterministic Key Ordering

User params are sorted alphabetically by key. Envelope params follow a fixed
order defined by the builder (locale → schemaVersion → platform → market →
experiments). Experiments within the map are sorted by experiment ID.

Identical inputs must produce byte-identical URLs across platforms and across
runs. The CDN cache key depends on it.

## Headers

All headers are out-of-band from the cache key. Caching is driven entirely by
the canonical query string. Headers carry analytics/tracing data (client-sent)
and resolved context (edge-injected).

### Client-Sent Headers

Used for tracing, analytics, and correlation. The BFF treats these as
informational only — never load-bearing for content decisions.

| Header | Format | Purpose |
|---|---|---|
| `X-Platform` | `android` \| `ios` \| `web` | Platform analytics |
| `X-App-Version` | semver | Client version analytics |
| `X-OS-Version` | string | OS version analytics |
| `X-Device-Id` | uuid | Correlation ID; not trusted for decisions |
| `X-Trace-Id` | uuid | Distributed tracing; inherited by parameterized refresh |
| `X-Request-Id` | uuid | Request-level log correlation and dedup |
| `Authorization` | `Bearer <token>` | Auth (out of scope for this spec) |

#### Rationale: Why Headers, Not Query Params

These fields have high cardinality (`X-Device-Id` is per-device, `X-Request-Id`
is per-request) or don't affect composition. Putting them in the query string
would fragment the CDN cache with no benefit. Headers travel alongside the
request without affecting the cache key.

`X-Trace-Id` deserves special mention: parameterized refresh inherits the
parent screen's trace ID so server logs can correlate a refresh response with
the screen that triggered it.

### Edge-Injected Headers

Reserved for future edge worker functionality. Currently no edge worker is
deployed.

| Header | Source | Purpose | Status |
|---|---|---|---|
| `X-Experiment-Context` | Edge passes through full client-asserted assignment set (base64 JSON) | Analytics / exposure logging | **Roadmap** |

> **Market cohort** moved to the query string as `market[cohort]`. Trust is
> established via app attestation (Play Integrity / App Attest), not edge IP
> resolution. See Field Definitions above.

### Trust Model

| Source | Trusted For | Trust Mechanism |
|---|---|---|
| Client envelope (query string) | Locale, schema version, device class, capabilities, experiment assignments | Standard client contract |
| Client envelope — `market[cohort]` | Regional content, blackouts, broadcast territories | App attestation (Play Integrity / App Attest) |
| Client headers | Tracing, analytics, correlation | Informational only — never load-bearing |
| Auth context (out of scope) | Identity, entitlements | OAuth / JWT |

Most envelope fields are low-risk (locale, schema version) — a malicious
client gains nothing by lying about them. `market[cohort]` is the exception:
it drives content gating, blackout enforcement, and broadcast territory
logic. App attestation ensures the value originates from a legitimate app
install, not a spoofed request. Requests failing attestation receive
`MARKET_UNKNOWN` treatment (safe default, no region-specific content).

## Server-Side Contract

### Schema Version Negotiation

The server uses `schemaVersion` from the request envelope to ensure backward
compatibility. Version format is **major.minor** (e.g. `1.0`, `1.1`, `2.0`).

| Client `schemaVersion` vs. Server | Behavior |
|---|---|
| `>= currentVersion` | Emit full response (normal path) |
| `< currentVersion` but `>= minSupportedVersion` | Strip fields and enum values introduced after client's version |
| `< minSupportedVersion` | Return `X-Schema-Version-Mismatch: upgrade-required` header + ErrorState section |

#### Response Header: `X-Schema-Version-Mismatch`

Returned when the client's declared `schemaVersion` is below the server's
minimum. Value is always `upgrade-required`. Clients must detect this header
and display a platform-appropriate update prompt.

```
HTTP/1.1 200 OK
X-Schema-Version-Mismatch: upgrade-required
Content-Type: application/json

{
  "sections": [
    {
      "id": "error-upgrade-required",
      "type": "AtomicComposite",
      "data": { ... ErrorState prompting update ... }
    }
  ]
}
```

The response body still contains a valid `SduiScreen` with an ErrorState
section so clients that don't check the header still render something
meaningful.

#### Field Stripping

When a client's version is below `currentVersion` but above
`minSupportedVersion`, the server applies post-composition filtering:

- JSON fields registered as introduced after the client's version are removed
- Enum values registered as introduced after the client's version are nulled
- The response for version N is always a strict subset of the response for
  version N+1

#### Server Configuration

```yaml
sdui:
  schema:
    current-version: "1.0"
    min-supported-version: "1.0"
```

### SduiRequestContext

The server-side POJO that every controller method receives, populated by
`BracketParamResolver` from either bracket-notation query params (GET) or
a JSON body (POST).

```
SduiRequestContext
├── locale: String = "en"
├── schemaVersion: String = "1.0"
├── traceId: String               ← from X-Trace-Id header
├── platform
│   ├── deviceClass: String
│   └── capabilities
│       ├── sse: boolean
│       └── onFocus: boolean
├── market
│   └── cohort: String = "MARKET_UNKNOWN"  ← from query string market[cohort]
├── device
│   └── deviceId: String          ← from X-Device-Id header
└── experiments: Map<String, String>
```

`BracketParamResolver` also reads `X-Request-Id` into MDC for log correlation
(not stored on the context — it's a per-request logging concern, not a
composition input).

### stripEnvelopeKeys

Controller methods that accept both envelope and user params (e.g. refresh
with `paramBindings`) strip known envelope keys from `@RequestParam` so
user-supplied values are cleanly separated.

## Platform Implementations

### RequestEnvelopeBuilder

Each platform has a `RequestEnvelopeBuilder` that produces byte-identical
query strings for the same inputs:

| Platform | File | Notes |
|---|---|---|
| Android | `android/sdui-core/.../request/RequestEnvelopeBuilder.kt` | Builder pattern, `buildQueryString()` / `buildJsonBody()` |
| iOS | `ios/Sources/SduiCore/Network/RequestEnvelopeBuilder.swift` | Struct (`RequestEnvelope`), `buildQueryString()` / `jsonBody()` |
| Web | `web/src/request/RequestEnvelopeBuilder.ts` | Class, `buildQueryString()` / `buildJsonBody()` |

All three share the same encoding rules, field order, and GET/POST threshold.

### SduiRepository / fetchSduiScreen

The single fetch primitive on each platform. Owns:

- Base URL resolution
- Envelope serialization (GET query string or POST JSON body)
- GET/POST length fallback at 8192 chars
- RFC-3986 percent-encoding
- Deterministic key ordering
- Header attachment (trace, request ID, device ID, platform analytics, edge placeholders, auth)
- `X-Trace-Id` propagation from parent fetch for parameterized refresh

| Platform | File |
|---|---|
| Android | `android/sdui-core/.../data/SduiRepository.kt` |
| iOS | `ios/Sources/SduiCore/Network/SduiRepository.swift` |
| Web | `web/src/runtime/fetchSduiScreen.ts` |

Every composition request — initial loads, navigation, pull-to-refresh,
action-driven refresh (including parameterized refresh with `paramBindings`)
— routes through this single primitive. Hand-rolled URL strings, bespoke
`fetch`/`URLRequest` calls, or per-action transports are prohibited.

## Caching

### Cache Key

```
cache_key = canonical_path + canonical_query_string
```

Akamai computes the cache key after canonicalization. No `Vary` header
reliance for resolved-context dimensions.

### Cache Layers

| Layer | Behavior |
|---|---|
| Akamai | Caches non-personalized SDUI responses on canonical URL |
| BFF tier | Caches personalized responses (Playmaker / Next Best Action) keyed on user identity |
| Client | Local cache must include same dimensions as canonical URL or use response fingerprint for invalidation |

### Personalized Responses

Surfaces with personalization (Playmaker, Next Best Action) emit:

```
Cache-Control: private, max-age=<short>
```

Akamai becomes a request shield, not a cache. Real caching happens in the
BFF tier with full key control.

## Roadmap

### Edge Worker

The edge worker (Akamai) is a planned optimization layer. With market cohort
now in the query string (trusted via app attestation), the edge worker's
remaining responsibilities are:

1. **Filter experiments:** drop `experiments[*]` entries not in the per-surface registry.
2. **Canonicalize query string:** sort all params, canonical percent-encoding, compute cache key.
3. **Forward to BFF.**

Geo resolution is no longer an edge concern — the client sends `market[cohort]`
directly, trusted via app attestation.

### Per-Surface Experiment Registry

Shared config consumed by both edge worker and BFF. Declares which experiments
affect rendering for each surface.

```yaml
surfaces:
  games:
    cache_relevant_experiments:
      - gd_tab_order_v2
      - gd_score_card_v3
      - gd_promo_slot_v1
  standings:
    cache_relevant_experiments: []
  player_detail:
    cache_relevant_experiments:
      - player_bio_layout_v1
```

Edge filters incoming `experiments[*]` to this set before computing the cache
key. BFF reads from the same source to prevent drift.

### Market Cohort Taxonomy

Versioned enum, shared between edge worker, BFF, Playmaker, and section
resolvers.

- US cohorts prefixed `US_*` (e.g. `US_NY_METRO`, `US_DEFAULT`)
- International cohorts prefixed `INTL_*` (e.g. `INTL_CA`, `INTL_DEFAULT`)
- Unresolved: `MARKET_UNKNOWN`

Cohort taxonomy ownership sits with content strategy, not platform.

### Platform Tier (Capabilities Consolidation)

> Individual boolean capability flags (`sse`, `onFocus`, ...) fragment CDN
> cache keys — every unique combination produces a distinct cache entry.

Replace with a small, server-defined platform tier (e.g. `"tier:full"`,
`"tier:passive"`) that the server maps to a capability set. Tier resolution
happens at the edge or in the client. This collapses the capability dimension
from 2^N cache entries to a small constant.

## Open Items

- Auth context shape (when personalization is in scope)
- Device attestation integration (App Attest / Play Integrity) for Phase 2
- Cohort registry storage and propagation mechanism (config service vs. static config)
- Experiment exposure logging owner (client SDK vs. server-side from `X-Experiment-Context`)

## Changelog

| Date | Change |
|---|---|
| 2026-05-04 | `market[cohort]` moved from edge-injected header to query string (composition input). Trust via app attestation, not edge IP resolution. `X-Resolved-Country` and `X-Resolved-Market-Cohort` headers removed. Edge worker geo resolution removed from roadmap. |
| 2026-05-01 | v1 implemented. Versioned paths (`/v1/`), analytics fields moved to headers, geo moved to placeholder edge headers, `X-Request-Id` added. All 4 platforms + server updated. |
