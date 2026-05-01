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
GET /v1/sdui/scoreboard?{canonical-query-string}
GET /v1/sdui/game-detail/{gameId}?{canonical-query-string}
GET /v1/sdui/for-you?{canonical-query-string}
GET /v1/sdui/watch?{canonical-query-string}
GET /v1/sdui/games?{canonical-query-string}
GET /v1/sdui/schedule?{canonical-query-string}
GET /v1/sdui/home?{canonical-query-string}
GET /v1/sdui/leaders?{canonical-query-string}
GET /v1/sdui/refresh/{screenId}?{canonical-query-string}
GET /v1/sdui/init
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
| `experiments[<key>]` | optional | Client-asserted Amplitude assignments. Edge filters per surface (roadmap). |

#### Excluded from the Query String

| Field | Reason |
|---|---|
| `platform[name]` | SDUI is platform-agnostic; renderer concern, not composition concern. Moved to `X-Platform` header for analytics. |
| `platform[appVersion]` | Replaced by `schemaVersion` for contract purposes. App version is analytics-only. Moved to `X-App-Version` header. |
| `platform[osVersion]` | Does not affect response. Analytics-only. Moved to `X-OS-Version` header. |
| `device[zipCode]` | Server-resolved at edge. Client claims not trusted for geo. |
| `device[countryCode]` | Server-resolved at edge. Client claims not trusted for geo. |

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
- Geo (`zipCode`, `countryCode`) cannot be trusted from client claims. The edge
  worker resolves the real geo from the client IP. Putting client-claimed geo in
  the query would cache on untrusted data.

### GET/POST Fallback

GET is the default method. When the combined query string (envelope + user
params) exceeds **8192 characters**, the client switches to POST with the
same envelope shape in a JSON body. User-supplied filter params (e.g. from
Form `paramBindings`) always ride the URL query string regardless of method
so the server reads them through `@RequestParam` on both paths.

```
// GET (normal)
GET /v1/sdui/refresh/stats-leaders?perMode=Totals&season=2025-26&locale=en&...

// POST (oversized envelope)
POST /v1/sdui/refresh/stats-leaders?perMode=Totals&season=2025-26
Content-Type: application/json
{
  "locale": "en",
  "schemaVersion": "1.0",
  "platform": {
    "deviceClass": "phone",
    "capabilities": { "sse": true }
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
order defined by the builder (locale → schemaVersion → platform → experiments).
Experiments within the map are sorted by experiment ID.

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

Resolved by the edge worker (Akamai) and forwarded to the BFF as trusted
(signed or mTLS-verified). The BFF **must not** trust client-supplied versions
of these headers.

| Header | Source | Purpose | Status |
|---|---|---|---|
| `X-Resolved-Country` | Edge resolves clientIP → ISO 3166-1 alpha-2 | Compliance, legal, analytics | **Placeholder** (clients send `"US"`) |
| `X-Resolved-Market-Cohort` | Edge resolves ZIP → US cohort or country → intl cohort; `MARKET_UNKNOWN` on failure | Content decisions, blackouts, personalization | **Placeholder** (clients send `"MARKET_UNKNOWN"`) |
| `X-Experiment-Context` | Edge passes through full client-asserted assignment set (base64 JSON) | Analytics / exposure logging | **Roadmap** |

> **Current state:** No edge worker is deployed yet. Clients send placeholder
> values for `X-Resolved-Country` (`"US"`) and `X-Resolved-Market-Cohort`
> (`"MARKET_UNKNOWN"`). The server POJO (`SduiRequestContext.resolvedCountry`,
> `SduiRequestContext.resolvedMarketCohort`) is wired and ready so the shape
> doesn't change when the edge layer ships.

### Trust Model

| Source | Trusted For | Not Trusted For |
|---|---|---|
| Client envelope (query string) | Locale, schema version, device class, capabilities, experiment assignments | Geo, identity, entitlements |
| Client headers | Tracing, analytics, correlation | Decisions affecting content |
| Edge-injected headers | Geo (country, market cohort), experiment context | — |
| Auth context (out of scope) | Identity, entitlements | — |

Client headers carry claims and identifiers. Edge-injected headers carry
resolved truth. The BFF trusts the second category and treats the first as
informational. Any decision affecting content, entitlements, or compliance
flows through edge-resolved values, not client claims.

## Server-Side Contract

### SduiRequestContext

The server-side POJO that every controller method receives, populated by
`BracketParamResolver` from either bracket-notation query params (GET) or
a JSON body (POST).

```
SduiRequestContext
├── locale: String = "en"
├── schemaVersion: String = "1.0"
├── traceId: String               ← from X-Trace-Id header
├── resolvedCountry: String       ← from X-Resolved-Country header
├── resolvedMarketCohort: String  ← from X-Resolved-Market-Cohort header
├── platform
│   ├── deviceClass: String
│   └── capabilities
│       ├── sse: boolean
│       └── onFocus: boolean
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

The edge worker (Akamai) is the planned trust boundary. It transforms the
client request into a canonical, cache-keyable URL with resolved context:

1. **Resolve country:** clientIP → countryCode (ISO 3166-1 alpha-2). Always present.
2. **Resolve market cohort:**
   - `country == US` → ZIP → US market cohort (e.g. `US_NY_METRO`)
   - `country != US` → country → intl market cohort (e.g. `INTL_CA`, `INTL_DEFAULT`)
   - Resolution failure → `MARKET_UNKNOWN`
3. **Filter experiments:** drop `experiments[*]` entries not in the per-surface registry.
4. **Canonicalize query string:** sort all params, canonical percent-encoding, compute cache key.
5. **Inject resolved-context headers** (signed or mesh-trusted).
6. **Forward to BFF.**

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
| 2026-05-01 | v1 implemented. Versioned paths (`/v1/`), analytics fields moved to headers, geo moved to placeholder edge headers, `X-Request-Id` added. All 4 platforms + server updated. |
