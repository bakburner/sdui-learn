# Server Implementor's Contract

> **Audience.** Engineers building a production SDUI composition server (with
> real upstream aggregation, caching, and observability). This document
> defines **the contracts and patterns the server must satisfy** so any client
> built to [`client-implementors-contract.md`](client-implementors-contract.md)
> can consume its responses without modification.
>
> **Status.** This is a **specification**, not a design. It states what must
> be true about the server's surface area, response shape, and pipeline
> ordering. The actual implementation — service framework, caching topology,
> aggregation library, deployment shape — is **TBD** and intentionally not
> prescribed here. The prototype in [`server/`](../../server/) is one
> implementation; a production build will use stronger patterns (SAF,
> two-tier cache, request collapsing, etc.) once selected.
>
> **Companion documents.** The wire contract lives in
> [`sdui-requirements-summary.md`](../sdui-requirements-summary.md); governance
> rules live in [`AGENTS.md`](../../AGENTS.md); performance principles live in
> [`plans/server/sdui-performance-design-principles.md`](../plans/server/sdui-performance-design-principles.md);
> the section-caching plan lives in
> [`plans/server/plan-server-section-caching.md`](../plans/server/plan-server-section-caching.md).

---

## 1. Priority Order

When rules below feel in tension, resolve them in this order:

1. **Wire contract correctness** — every response decodes against the schema
2. **Determinism** — same inputs produce byte-identical bytes across pods and runs
3. **Cacheability** — composition is a pure function of envelope + user params + upstream data
4. **Server authority** — content, routing, refresh, and asset URIs originate server-side
5. **Operational hygiene** — observability, version gating, graceful upstream failure

A lower rule never weakens a higher one.

---

## 2. The Contract Surface

A conformant SDUI server exposes **exactly four** request shapes. Anything
else is out of contract.

| # | Channel | URL family | Returns | Cache-Control |
|---|--------|-----------|---------|---------------|
| 1 | Screen channel | `GET\|POST /v1/sdui/screen/{screenId}[?userParams]` | Complete `Screen` JSON | Per endpoint (live → `no-cache`, editorial → `max-age=N`) |
| 2 | Section channel | `GET\|POST /v1/sdui/section/{sectionId}` | Single `Section` JSON (never a `Screen`) | `no-cache` |
| 3 | Raw data channel | `GET /v1/api/...` | Opaque JSON the client applies via `dataBinding` | `no-cache` (live), `max-age=N` (static) |
| 4 | Bootstrap | `GET /v1/sdui/screen/init` | Bootstrap navigation URI (clients do not hardcode starting screen) | `max-age` short |

### 2.1 Required properties of every channel

- **Dual mount.** Channels 1 and 2 must be mounted as `GET` *and* `POST` to
  the same handler. A `GET`-only or `POST`-only composition endpoint is a
  contract bug. POST exists because envelope serialization can exceed the
  8192-char URL threshold (see §4.2).
- **Idempotency.** Composition channels are pure functions of the request.
  Two requests with the same envelope and user params must produce the same
  response bytes. Server-internal state (cache, time-of-day) must not leak
  into the response unless the response also documents that volatility via
  `refreshPolicy` or `Cache-Control`.
- **Correlation propagation.** `X-Correlation-ID` from the request must be
  echoed in the response and carried into every downstream call (upstream
  fetches, section re-composition, mutate→refresh chains). If absent on the
  request, the server generates one and returns it.

### 2.2 Forbidden surface

- A third "partial screen" or "screen patch" channel
- A per-section endpoint that returns a `Screen` envelope
- A screen endpoint that returns anything other than a full `Screen`
  (no diffing, no "only changed sections" responses — the screen channel is
  always a full replace; see AGENTS.md §3.8)
- Hardcoded per-platform endpoint paths (`/v1/sdui/android/...`, etc.) —
  platform variation rides in the envelope, not the path
- Mutating endpoints in the composition surface — mutations go through the
  `mutate` action type, which reaches the server via the screen or section
  channel with side-effect parameters

---

## 3. Response Shape Obligations

### 3.1 Screen channel must emit a complete `Screen`

Every screen response carries:

- `id` — must match the requested `screenId`; mismatches are a contract bug
  (clients drop mismatched responses)
- `sections[]` — the full ordered list; structural changes (insertions,
  removals, reorderings) happen by emitting a new full list, never by patch
- `defaultRefreshPolicy` — optional single `RefreshPolicy` object; when
  present, type is `static`, `poll`, or `sse`. This field remains single-object
  even though section `refreshPolicy` is an array. A non-static
  `defaultRefreshPolicy` and any section element with
  `refreshPolicy[].sectionEndpoint` on the same screen are **mutually
  exclusive** (the server must not emit both)

Correlation lives in the `X-Correlation-ID` response header only; there is
no body-level `traceId` field on `Screen`.

### 3.1.1 Response envelope wrap

Every screen-channel and section-channel response is wrapped in a
hand-written transport envelope at the controller edge:
`ResponseEnvelope<T>(T data, ResponseMeta meta)`. `data` carries the
schema-bound payload; `meta` carries transport framing
(`degraded`, `staleSections`, `failedSections`). The current
implementation emits a static stub (`degraded: false`, empty arrays);
real partial-failure metadata lands in a later phase. The envelope is
**outside** `schema/sdui-schema.json` and outside codegen; see ADR-017
and AGENTS.md §1.2 ("Transport-framing exception") for the rules.

### 3.2 Section channel must emit a single `Section`

- The response body is a `Section` JSON object, not wrapped in a `Screen`
  envelope
- The section's `id` must equal the requested `sectionId`
- The server may return `404` (no resolver), `400` (resolver exists but
  cannot compose this id), or `200` with a filtered `Section`

### 3.2.1 Section channel is screen-independent

The section channel composes one section in isolation. It must satisfy
these properties:

- **No parent-screen dependency.** A section-channel request must compose
  successfully without the server loading, referencing, or reconstructing
  any `Screen`. The dispatcher receives `sectionId` + request envelope
  and nothing else; no `screenId` is accepted, inferred, or required.
- **Standalone callability.** Any caller in possession of a valid
  `sectionId` may invoke the endpoint directly, whether or not that
  section is currently mounted in any screen and whether or not any
  `refreshPolicy[].sectionEndpoint` element ever pointed at it.
  `refreshPolicy[].sectionEndpoint` is a *scheduling* mechanism for
  automatic client polling; it is not a precondition for the endpoint's
  existence or correctness.
- **`sectionId` is the self-sufficient composition key.** The id format
  (`{sanitizedContentSource}__type-{SectionType}[__slug-{camelCaseName}]`,
  per §3.4) must
  encode every upstream identifier the resolver needs (gameId, leagueId,
  feed key, etc.). Resolvers must not depend on ambient screen state,
  caller-supplied screen context, or prior screen-channel requests to
  resolve the id.
- **Envelope-only context.** The only contextual inputs a section
  resolver may read are the request envelope fields enumerated in §4.1
  (deviceClass, capabilities, locale, schemaVersion, experiments,
  market.cohort, correlationId). Anything else is a contract violation.
- **Same-id invariant under repeat composition.** Composing the same
  `sectionId` via the section channel and via a full screen-channel
  response (where that section appears in `sections[]`) must yield the
  same `Section` body for identical envelope + upstream state. The
  section channel is not a different composition path; it is the same
  composer invoked for one section.

### 3.3 Tokens, not raw values, on the wire

Per AGENTS.md §3.6, every payload emits `token:nba.*` strings for spacing,
radius, typography, motion, shadow, color, and `sdui:*` strings for icons.
The narrow exceptions are documented there. The server must never emit:

- Raw hex colors except as the inline escape hatch where no token exists
- Raw pixel/dp/sp dimensions for design-system properties
- Platform-native icon names (`home`, `sports_basketball`)
- Resolved typography styles (`{fontSize: 14, fontWeight: 600}`) — emit the
  semantic token, not the resolved bundle

A server-side token registry (analogous to the prototype's
[`TokenRegistry`](../../server/src/main/java/com/nba/sdui/service/TokenRegistry.java))
**must validate at startup** that every constant referenced by composer code
resolves against the bundled token JSON. Mismatches fail boot; they are not
runtime warnings.

### 3.4 IDs are stable and positional indices are forbidden

Section IDs must be derived from
`{sanitizedContentSource}__type-{SectionType}` with an optional
`__slug-{camelCaseName}` disambiguator. The wire format is BEM-classic
(`__` between groups, `-` inside groups). `contentSource` is sanitized
by replacing every character outside `[A-Za-z0-9-]` with `-` and
collapsing runs (trailing dashes on prefix-only sources are preserved
so resolver `startsWith` matching works). `slug` is strict
lowerCamelCase matching `^[a-z][a-zA-Z0-9]*$` — kebab-case slugs are
rejected at composition time. The format is CSS-selector safe so
clients can reflect ids into DOM class names without escaping. Position
in the section array must never appear in the ID. This guarantees that
surgical section replacement (section channel) and SSE-driven section
patches address the same logical unit even after the server reorders
sections.

### 3.5 Error states are first-class sections

Per AGENTS.md §8.0, an unrecoverable composition failure (bad ID, upstream
unavailable, missing data) returns an `ErrorState` section (or a `Screen`
containing one), **not** a hardcoded fallback payload, an empty screen, or
the last-known-good response. The client decides whether to retain its
prior screen; the server never substitutes invented content.

---

## 4. The Request Envelope

### 4.1 Envelope is the only composition input the server reads from transport

Every composition request carries one canonical envelope. Composers read
from a parsed request context with these fields:

| Field | Source | Purpose |
|-------|--------|---------|
| `platform.deviceClass` | Envelope (`platform[deviceClass]`) | Composition input — `phone`, `tablet`, `tv`, `web` |
| `platform.capabilities.sse` | Envelope (`platform[capabilities][sse]`) | Composition input — whether server may emit SSE refresh policies |
| `platform.capabilities.onFocus` | Envelope | Composition input — visibility-trigger capability |
| `locale` | Envelope | Composition input — language for `stringTable` stamping |
| `schemaVersion` | Envelope | Composition input — drives field stripping (§7) |
| `experiments[name]=variant` | Envelope | Composition input — A/B variant selection |
| `market.cohort` | Envelope | Composition input — geo/market segmentation (server-attested) |
| `traceId` | `X-Correlation-ID` header **only** | Observability only — header echoed on every response; **never** a body field |
| `device.deviceId` | `X-Device-Id` header **only** | Observability only — **never a cache key**, intentionally per-user-fragmented |

User-supplied filter/sort/date params (`?date=...`, `?perMode=...`, form
submits) are **separate** from the envelope. They ride the URL query string
on both GET and POST.

### 4.2 GET-first, POST-fallback at 8192 chars

- Default to `GET` so CDN edges can cache
- When the envelope query alone exceeds 8192 characters, the *same envelope
  shape* moves to a JSON body on the same path; user params stay on the URL
  query
- One resolver handles both bodies; composers see one parsed envelope

### 4.3 Envelope serialization is byte-deterministic

- Bracket-notation: `platform[deviceClass]=phone&platform[capabilities][sse]=true`
- RFC-3986 percent-encoding for both halves of the query
- Deterministic key ordering (envelope keys fixed by builder; user params
  sorted alphabetically by key)
- Identical inputs across platforms and pods must produce byte-identical
  URLs — the CDN cache key depends on it

### 4.4 What is *not* an envelope field

These are **client-local** and intentionally absent from the envelope.
Adding them would fragment the cache keyspace without composition benefit:

- `formFactor` (orientation, viewport, breakpoint) — resolved client-side
- `theme` (light/dark) — clients pick at render time from `token:*` references
- `density`, `fontScale` — platform-native
- `timezone` for display formatting — clients format locally; the server
  may use league time (ET) as a constant if it affects composition

---

## 5. The Composition Pipeline

Every composition request flows through these stages in this order. The
order is part of the contract because it determines caching key shape and
the meaning of each layer.

```
Request
  │
  ▼
[A] Envelope decode + validation
  │   • Parse bracket params or JSON body; reject malformed
  │   • Stamp correlationId from X-Correlation-ID; bind to MDC for downstream logs
  ▼
[B] Version gate
  │   • If client schemaVersion < minimum → 426 / X-Schema-Version-Mismatch
  │   • Otherwise carry version into stage [G]
  ▼
[C] Experiment resolution
  │   • Resolve all variant assignments for this screen
  │   • Variants become composition inputs; their identity participates in
  │     cache keys downstream
  ▼
[D] Upstream data fetch  ──── (cached, request-collapsed, stale-if-error)
  │   • Multiple upstreams may run in parallel
  │   • Failure → either retry, serve stale, or return ErrorState (per
  │     composer policy; never invent content)
  ▼
[E] Section composition  ──── (cacheable per section, see §6)
  │   • Each section built from upstream data + envelope inputs
  │   • Composers emit token:* strings, never raw values
  │   • Section IDs derived per §3.4
  ▼
[F] Screen assembly       ──── (must NOT be cached)
  │   • Order sections, attach navigation, attach defaultRefreshPolicy
  │   • Variant selection finalized here
  ▼
[G] i18n stringTable stamping
  │   • Either stamps strings into sections (locale-specific output) or
  │     attaches a stringTable the client resolves (locale-neutral output)
  │   • Order vs. stage [E] determines whether locale is a section-cache
  │     key dimension — see §6.3
  ▼
[H] Schema-version field stripping
  │   • Walk response; remove fields and null enum values introduced after
  │     client schemaVersion
  │   • Output decodes cleanly against the client's older schema
  ▼
[I] Cache-Control header emission
  │   • Live game state → no-cache
  │   • Editorial/static → max-age=N, public
  │   • Personalized → max-age=N, private
  ▼
Response
```

### 5.1 Stage ordering rules

- **[B] before [C]** — never run experiment resolution for clients the
  server is about to reject
- **[D] before [E]** — sections compose from cached upstream data; an
  upstream miss must not silently fall through to inventing data
- **[E] separate from [F]** — section composition is cacheable; screen
  assembly is not (variant selection and `defaultRefreshPolicy` choice run
  per request)
- **[G] and [H] always last** — stamping and stripping mutate the assembled
  response; their position determines what each upstream cache layer holds

### 5.2 Pipeline purity requirement

Stages [C] through [F] must be **pure functions** of `(envelope, user
params, upstream data, experiment assignments)`. No wall-clock reads, no
random number generation, no environment lookups, no Jackson serialization
quirks (sort map keys, fix number formatting). Determinism is the property
that makes section-fragment caching safe.

---

## 6. Caching Contract

The server is free to choose its caching topology, but the **layering and
key shape** are part of the contract because they're what makes the
production system viable at scale.

### 6.1 Three layers, three different lifetimes

| Layer | What it caches | TTL gradient | Bypassable |
|-------|----------------|--------------|------------|
| **Upstream data** | Raw bytes from each backend feed | Match upstream volatility (live 5–15s, editorial 10–30 min) | `stale-if-error` required |
| **Section fragments** | Output of a single section composer | Match section volatility, not screen volatility | Per-section bypass via debug header |
| **Screen assembly** | **Nothing** | n/a | Always fresh |

The first two layers are *optional* for a prototype but **required** for any
deployment carrying non-trivial production traffic. The third layer is
*forbidden* — caching screen assembly breaks experiment routing and
defeats the dynamic ordering that makes SDUI valuable.

### 6.2 Required cache key dimensions

Section-fragment cache keys **must** include:

- `sectionType`
- `contentHash` — fast hash of the upstream input data the section reads
- `deviceClass`
- `schemaVersion` — different versions emit structurally different bytes
- `experimentBucket` — for sections whose composition branches on a variant;
  experiment-invariant sections may use a placeholder

Section-fragment cache keys **must NOT** include:

- `theme`, `density`, `fontScale`, `formFactor` — all client-resolved per
  §4.4
- `deviceId` — high-cardinality and would defeat the cache entirely
- Time-of-day — composers must be deterministic per §5.2; volatility lives
  in TTLs, not keys

### 6.3 Locale and the stringTable stamping decision

The server picks one of two policies for i18n; both satisfy the contract:

- **Stamp-before-cache.** Section fragments are locale-specific; key
  includes `locale`. Higher correctness, lower hit ratio (divides by locale
  count).
- **Stamp-after-cache.** Section fragments carry `stringTable` references;
  key excludes `locale`; stamping runs on every cache hit. One fragment
  serves all locales.

Either is acceptable but the choice must be **documented in the server
build** and consistent across all composers. Mixing strategies per composer
is a contract bug.

### 6.4 Determinism is testable

The server build must include a test that:

1. Composes the same `(screenId, envelope, user params)` twice
2. Asserts the two response payloads are byte-identical
3. Asserts the derived section-cache keys are byte-identical

A non-deterministic composer is a P0 defect because it silently destroys
cache hit rates.

---

## 7. Schema Versioning

Per AGENTS.md §1.3, clients decode strictly. The server therefore must not
emit fields or enum values the client's `schemaVersion` cannot decode.

### 7.1 Field stripping pipeline

The server maintains a registry of `(field path → version introduced)` and
`(field path → enum value → version introduced)`. After stage [F] in the
pipeline:

1. Walk the response tree (depth-limited, e.g. 50 levels)
2. For each field introduced *after* the client's `schemaVersion`: remove
   it (or set to null)
3. For each enum-typed field where the chosen value was introduced *after*
   the client's version: null the field (clients see absence, not an
   unknown value)

### 7.2 Version gate

Before composition (stage [B]):

- If client `schemaVersion < server.minimumSupportedVersion`: reject with
  `426 Upgrade Required` and emit `X-Schema-Version-Mismatch` so clients
  can surface a force-upgrade prompt
- Otherwise: carry the version into stripping

### 7.3 Forward compatibility for new enum values

When the server introduces a new section type, atomic type, or enum value:

1. Update `schema/sdui-schema.json` and re-run `make codegen`
2. Update the version registry with the new path/value and its introducing
   version
3. Only then begin emitting the new value — and only to clients on a
   schemaVersion ≥ the introducing version

This is the server-side mirror of AGENTS.md §1.2's client release sequence.

---

## 8. Upstream / Aggregation Contract

The server's job at the aggregation layer is to absorb upstream variance so
the composition layer can be deterministic.

### 8.1 Per-upstream policy declaration

For each upstream feed the server depends on, the build must declare:

- **TTL** (live vs post/final variants where applicable)
- **`stale-if-error` window** — how long to serve cached data after the
  upstream starts failing
- **Criticality** — `required` (composition fails without it → `ErrorState`)
  vs `optional` (compose without it, omit the section)
- **Request-collapsing scope** — concurrent identical fetches must
  deduplicate to a single upstream call
- **Timeout budget** — per-call timeout that respects the overall
  composition latency target

### 8.2 Failure handling

- **Required upstream failure with no stale data:** compose an `ErrorState`
  section (or screen). Never invent content. Never fall back to a stale
  hardcoded example payload.
- **Optional upstream failure:** omit the dependent section(s) and continue
  composing. Log at WARN.
- **Partial response from upstream:** composer treats missing fields as
  absent (renders nothing) rather than filling defaults that imply data.

### 8.3 Upstream identity in trace context

Every upstream call carries the request's `X-Correlation-ID` so server logs
correlate composition latency with the upstream fetches it triggered. This
is non-optional for any production deployment.

---

## 9. Refresh Channel Obligations

### 9.1 Two channels, one URL family each

Per AGENTS.md §3.8:

- **Screen channel** (`/v1/sdui/screen/{id}`) — full-screen replace.
  Handles: initial load, navigation, pull-to-refresh, screen-level poll
  tick, action-driven `refresh` targeting the current screen, parameterized
  re-composition (form submit, date picker).
- **Section channel** (`/v1/sdui/section/{id}`) — single-section replace.
  Handles: section-level polls via a `refreshPolicy[]` poll element carrying
  `sectionEndpoint`, and mutate → section refresh chains.

There is no third channel. A section may run one opaque element
(`type: "sse"` + `channel` or `type: "poll"` + `url`) and one section-refresh
element (`type: "poll"` + `sectionEndpoint`) concurrently through the bounded
`refreshPolicy[]` array (`maxItems: 2`). `dataBinding` stays section-level and
binds to the single opaque element only; section-refresh responses are full
section replacements.

Draft-07 enforces the array shape (`maxItems`, element type/enum). The server
additionally validates cross-element invariants: ≤1 opaque element,
≤1 section-refresh element, and `type: "static"` as a solo terminal element.

On section-channel `404`, the expected client behavior is: mark section stale,
stop that section's poll, and retain the section node on screen.

### 9.2 Section channel dispatcher

The server resolves a `sectionId` to a composer via either:

- **Prefix match** (e.g. `stats-api:game-` → GameDetailComposer): preferred
  when many section IDs share a content source
- **Exact match** for named-region surfaces

Resolution must be O(1) or O(log n). A linear scan over registered
composers is acceptable for tens of resolvers; production-scale registries
should use a prefix tree.

### 9.3 Parameterized refresh

When a screen endpoint receives user query params (e.g.
`?date=2025-05-28`), the server re-composes the screen with those params
substituted. This is **the same handler** as the unparameterized request,
not a separate endpoint. Caching keys must include the user params.

### 9.4 Cache-Control per refresh class

| Endpoint class | Cache-Control |
|----------------|---------------|
| Live game data (boxscore, live games screen) | `no-cache` |
| Editorial / static (home, watch, schedule) | `max-age=60s` to `max-age=300s`, `public` |
| Personalized (for-you, leaders with user-specific sorts) | `max-age=60s` to `max-age=120s`, `private` |
| Bootstrap (`/v1/sdui/screen/init`) | `max-age=60s`, `public` |
| Section channel | always `no-cache` |
| Raw data channel | per-feed; same gradient as upstream TTL |

Adding `private` makes the response non-CDN-cacheable. Use it only when the
response actually varies by user.

---

## 10. Observability Contract

A production server build must surface enough signal to debug pipeline
problems without reading source.

### 10.1 Required metrics

- `sdui.composition.duration{screenId, sectionType, cached=true|false}` — p50/p95
- `sdui.cache.hit{layer=upstream|section, key}` and `.miss{...}`
- `sdui.upstream.duration{serviceName, status}` — p50/p95
- `sdui.version.mismatch{clientVersion}` — count of rejected requests
- `sdui.section.refresh{sectionId, status}` — section-channel dispatch outcomes

### 10.2 Required log correlation

- Every log line in a composition request carries `correlationId`, `requestId`,
  `screenId`
- Every upstream call logs the same `correlationId` so the composition fan-out
  is reconstructible from logs alone

### 10.3 Required headers

- `X-Correlation-ID` — echoed on every response
- `X-Schema-Version` — server's current schema version
- `X-Schema-Version-Mismatch` — present when the client's version is below
  the server's minimum supported version

---

## 11. Conformance Checklist

A new server passes contract when **all** of these hold:

### Channels & shape

- [ ] Screen and section channels mounted as both `GET` and `POST` to the same handler
- [ ] Screen responses always carry the full `sections[]` (no diffs, no patches)
- [ ] Section responses carry one `Section`, not a `Screen`
- [ ] Response `id` matches request `id` on both channels
- [ ] Section channel composes with `sectionId` + envelope only — no `screenId` accepted, inferred, or required
- [ ] Section channel works for any registered `sectionId` regardless of whether it is currently mounted in a screen or referenced by a `refreshPolicy[]` `sectionEndpoint` element
- [ ] Section-channel body for a given `sectionId` matches the same section as it appears in a screen-channel response for identical envelope + upstream state
- [ ] `X-Correlation-ID` echoed on every response

### Determinism

- [ ] Two requests with identical envelope + user params produce byte-identical responses
- [ ] Section-fragment cache keys are byte-identical across pods for identical inputs
- [ ] Composers contain no time-of-day, random, or environment reads (lints, not just tests)

### Token discipline

- [ ] No raw hex colors emitted (other than the documented escape hatch)
- [ ] No raw pixel/dp dimensions on design-system properties
- [ ] No platform-native icon names — only `sdui:*` tokens
- [ ] Token registry validates every constant at boot; mismatch fails startup

### Envelope handling

- [ ] Same handler reads envelope from bracket-query (GET) and JSON body (POST)
- [ ] `deviceClass` is the only platform input read from the envelope
- [ ] `X-Device-Id` never appears in cache keys or composition inputs
- [ ] User params travel on URL query on both GET and POST

### Pipeline ordering

- [ ] Version gate runs before composition
- [ ] Experiment resolution runs before section composition
- [ ] Section composition is cached; screen assembly is not
- [ ] i18n stamping policy (pre-cache or post-cache) is uniform across composers
- [ ] Schema-version field stripping runs last (after assembly + stamping)

### Failure modes

- [ ] Required upstream failure with no stale data → `ErrorState` section, not invented content
- [ ] Unknown `sectionId` on section channel → 404
- [ ] Sub-minimum schemaVersion → 426 + `X-Schema-Version-Mismatch`
- [ ] Unknown screenId → 404 (not an empty `Screen`)

### Refresh channels

- [ ] Section channel always `no-cache`
- [ ] Live-data screens are `no-cache`; editorial screens carry `max-age`
- [ ] `defaultRefreshPolicy` and section-level `sectionEndpoint` elements are mutually exclusive per screen

### Schema versioning

- [ ] New schema fields are only emitted to clients on `schemaVersion ≥` introducing version
- [ ] Field-stripping walk is depth-limited

### Observability

- [ ] Composition latency, cache hit ratio, and upstream latency emitted as metrics
- [ ] Every log line in a composition request carries `correlationId`

### Tests

- [ ] Schema round-trip test: every example fixture composes → decodes → re-emits identically
- [ ] Channel contract tests: screen channel returns `Screen`, section channel returns `Section`
- [ ] Endpoint regression guard: every documented endpoint is mounted with both `GET` and `POST`
- [ ] Determinism test: same inputs → identical bytes

---

## 12. What This Document Does *Not* Specify

The following are intentionally left to the implementation:

- **Service framework / language.** Spring Boot, Quarkus, Go, Node, Rust — all
  acceptable provided the channels above are correctly mounted.
- **Cache substrate.** Caffeine, Redis, two-tier (Caffeine + Redis), CDN-only —
  the contract demands the *layering* and *key shape*, not the technology.
- **Aggregation library.** SAF, hand-rolled OkHttp with request-collapsing,
  reactive streams, etc.
- **Deployment topology.** Pod count, autoscaling rules, fleet sizing — see
  the production capacity document referenced in
  [`plan-server-section-caching.md`](../plans/server/plan-server-section-caching.md).
- **Composer ergonomics.** Builder pattern (as in
  [`AtomicCompositeBuilder`](../../server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java)),
  template language, code generation — all acceptable provided emitted
  payloads honor §3.
- **Concrete TTL values.** The contract specifies a *gradient* (live short,
  editorial long); product tuning sets the actual numbers.

The prototype under [`server/`](../../server/) shows one workable
implementation of every contract item above. Treat it as **a worked
example**, not a specification.

---

## 13. Relationship to Other Documents

| Document | Role |
|----------|------|
| [`AGENTS.md`](../../AGENTS.md) | Governance rules cited throughout this contract |
| [`sdui-requirements-summary.md`](../sdui-requirements-summary.md) | Wire contract: what each schema field means |
| [`client-implementors-contract.md`](client-implementors-contract.md) | Mirror image — what clients are required to do with these responses |
| [`sdui-envelope-spec.md`](../specs/sdui-envelope-spec.md) | Detailed envelope serialization rules |
| [`plans/server/sdui-performance-design-principles.md`](../plans/server/sdui-performance-design-principles.md) | The 11 principles a server build must honor for production load |
| [`plans/server/plan-server-section-caching.md`](../plans/server/plan-server-section-caching.md) | Plan for adding section-fragment caching to the prototype (blocked on SAF) |
| [`plans/plan-aggregation-demo-features.md`](../plans/plan-aggregation-demo-features.md) | Capability tiers, section ID derivation, multi-column layout, feed-version negotiation |

---

## Revision History

| Date | Summary |
|---|---|
| 2026-05-28 | Initial draft. Extracted contracts from the prototype server (`server/`) and existing governance docs. Specifies channels, envelope, pipeline ordering, caching layering, token discipline, schema versioning, refresh channels, observability, and the conformance checklist. Implementation patterns (framework, cache substrate, aggregation library) intentionally left TBD. |
