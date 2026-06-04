# Plan: SDUI Server → SAF-Backed, Schema-Typed, Port-Ready Service

**Status:** Proposed
**Scope:** `server/` (SDUI composition service, run from this repo for now)
**Authoritative spec:** `docs/contracts/server-implementors-contract.md` — this
plan is the implementation path toward conformance with that contract (which
already names SAF + two-tier cache + request-collapsing as the production
upgrade). Every phase gate maps to its §11 conformance checklist.

**Related (SDUI, this repo):** `docs/contracts/client-implementors-contract.md`,
`docs/specs/sdui-envelope-spec.md`, `AGENTS.md`,
`docs/plans/server/sdui-performance-design-principles.md`,
`docs/plans/server/plan-server-section-caching.md` (explicitly *blocked on SAF* —
unblocked by Phase A2c), `docs/plans/plan-contract-testing.md`.

**Port target (`nba-client-backend`) — design constraints for Phase A, execution in
Phase B only:** Phase A does **not** port into the shell; it prepares a standalone
service whose shape matches the shell so Phase B is lift-and-shift. Shell docs below
are **read-only design references during A1–A3** (why package vocabulary, envelope
shape, exception/health surfaces look the way they do). Do not register modules, add
seams, or touch `integration-models` until Phase B.
`AGENTS.md` (extraction-readiness — the overriding constraint on Phase A structure),
`docs/guides/module-authoring-guide.md` (target surface checklist — copy WECS/CAS
shape, don't invent), `docs/adrs/ADR-001-module-topology-and-contracts.md` (D6/D7 —
envelope + no shared request envelope), `docs/adrs/ADR-004-cors-policy-ownership.md`
(CORS stays prototype-local until port), `docs/adrs/ADR-005-platform-signal-split.md`
(deferred load-bearing `platform`), `docs/plans/plan-request-contract-cacheability-alignment.md`.
**Phase B only:** ADR-003, `ModuleConformanceArchTest`, `ModuleBoundaryArchTest`,
`integration-models` seam registration, boot-shell wiring — see Reference documents.

**SAF adoption (`service-aggregation-framework`, Phase A2):**
`docs/INDEX.md` (entry point), `docs/ARCHITECTURE.md` (`OrchestratorFactory`,
`ServiceCall`, feature chain), `docs/CONFIGURATION.md` (`saf.services.<name>.*`),
`docs/SERVICE_NAME.md` (`serviceName` vs `id` vs cache key), `docs/CACHING.md`
(two-tier cache, `STALE_IF_ERROR`, request collapsing),
`docs/VIRTUAL-THREADS.md`, `docs/METRICS.md` (actuator/prometheus),
`docs/ERROR_HANDLING.md` (consumer-owned `@RestControllerAdvice` alongside SAF),
`docs/DEPENDENCY_STRATEGY.md` (Maven Local / `1.0.0-SNAPSHOT` wiring),
`docs/plans/plan-service-metadata-freshness.md` (`buildMetadata()` → consumer
`ResponseMeta` mapping — read before wiring envelope Step 2),
`docs/plans/plan-request-layer-ergonomics.md` (BracketParamResolver /
`X-Request-Id` backlog — SDUI keeps its own resolver for now).

---

## Reference documents (by phase)

Use this as the implementation bibliography. Paths are repo-root-relative from
each repository named in the heading.

**Phase A (A1–A3) executes entirely in `sdui-prototype`.** Shell docs in the table
below are *port-readiness design targets* — read them to match shape and constraints,
not to do shell work early. **Phase B** is the actual port into
`nba-client-backend` (future, not executed in this plan's active scope).

### SDUI wire & clients (`sdui-prototype`) — Phase A work

| Phase | Document | Role |
|-------|----------|------|
| A1–A3 | `docs/contracts/server-implementors-contract.md` | Authoritative server spec; §6 caching, §8 upstream, §10 metrics, §11 conformance checklist |
| A2 | `docs/contracts/client-implementors-contract.md` | Client cut-over (envelope, correlation header) |
| A2 | `docs/specs/sdui-envelope-spec.md` | Request envelope transport contract |
| A1–A3 | `AGENTS.md` | Schema doctrine (§1.2), ErrorState (§8.0), server authority |
| A2c | `docs/plans/server/plan-server-section-caching.md` | Section-fragment caching (unblocked by SAF in A2c) |
| A1–A3 | `docs/plans/plan-contract-testing.md` | Contract/round-trip safety net |
| A2 | `docs/plans/server/sdui-performance-design-principles.md` | Performance constraints |

### Port-readiness design target (`nba-client-backend`) — read during Phase A

These explain **why** Phase A adopts shell-like package vocabulary, envelope shape,
and module surfaces. None of this is executed in the shell until Phase B.

| Phase A use | Document | Role |
|-------------|----------|------|
| A1–A3 | `AGENTS.md` | Extraction-readiness — why `controller`/`orchestration`/`domain`/`remote` |
| A1–A3 | `docs/guides/module-authoring-guide.md` | Target surface checklist; copy WECS/CAS, don't invent |
| A2 | `docs/adrs/ADR-001-module-topology-and-contracts.md` | D7 `ResponseEnvelope` shape; D6 no shared request envelope |
| A1 | `docs/adrs/ADR-004-cors-policy-ownership.md` | Why CORS stays in prototype `WebConfig` until port |
| A2 | `docs/adrs/ADR-005-platform-signal-split.md` | Load-bearing `platform` vs `X-Analytics-Platform` |
| A2 | `docs/plans/plan-request-contract-cacheability-alignment.md` | Deferred load-bearing `platform` on WECS surfaces |
| A1–A3 | `docs/plans/plan-production-server-patterns.md` | Composition-layer patterns (not SAF transport) |

**Reference implementations (read-only during Phase A — copy shape, don't invent):**

| Module | Path | Copy for |
|--------|------|----------|
| WECS (composition) | `server/wecs-core/` | Orchestration, `ResponseMetaFactory`, exception handler, health indicator |
| WECS architecture | `server/wecs-core/docs/ARCHITECTURE.md` | Orchestration + meta-folding narrative |
| CAS (leaf) | `server/cas-core/` | Leaf upstream pattern, remote adapters |

### Port execution (`nba-client-backend`) — Phase B only

Not in active scope. Listed so Phase A decisions don't foreclose a clean port.

| Phase | Document | Role |
|-------|----------|------|
| B | `docs/adrs/ADR-003-inter-module-boundary-enforcement-and-eventing.md` | Inter-module boundary enforcement at registration |
| B | `server/nba-client-backend-server/src/test/java/com/nba/client/backend/architecture/ModuleConformanceArchTest.java` | Register `sdui-core` |
| B | `server/nba-client-backend-server/src/test/java/com/nba/client/backend/architecture/ModuleBoundaryArchTest.java` | Module isolation / domain purity |
| B | `integration-models/` | Register envelope/`meta` types; add `WatchExperienceAccess` seam |
| B | `server/nba-client-backend-server/` boot shell | Module binding, CORS (`nba.web.cors.*`), actuator groups |

### SAF adoption (`service-aggregation-framework`)

| Phase | Document | Role |
|-------|----------|------|
| A2 | `docs/INDEX.md` | Documentation entry point |
| A2 | `docs/ARCHITECTURE.md` | `OrchestratorFactory.create(timeout)`, `ServiceCall` builder, resilience chain |
| A2 | `docs/CONFIGURATION.md` | `saf.services.<name>.resilience.*`, `.cache.*`, `.collapsing.*` |
| A2 | `docs/SERVICE_NAME.md` | Per-service config key (`serviceName` ties resilience/cache/collapsing together) |
| A2 | `docs/CACHING.md` | L1/L2, `STALE_IF_ERROR`, request collapsing |
| A2 | `docs/VIRTUAL-THREADS.md` | `spring.threads.virtual.enabled=true` |
| A2 | `docs/METRICS.md` | Actuator/prometheus gate; upstream duration metrics |
| A1 | `docs/ERROR_HANDLING.md` | RFC-7807 `@RestControllerAdvice` alongside SAF (pairs with A1 exception handler) |
| A2 | `docs/DEPENDENCY_STRATEGY.md` | Maven Local dependency (`com.nba:service-aggregation-framework:1.0.0-SNAPSHOT`) |
| A2 (Step 2) | `docs/plans/plan-service-metadata-freshness.md` | `ServiceMetadata` / `ServiceCallInfo.servedStale` → `ResponseMeta` mapping |
| — | `docs/plans/plan-request-layer-ergonomics.md` | Backlog: shared envelope resolver, `X-Request-Id` (not built now) |

---

## Goal & shape

Evolve `sdui-prototype/server` from a raw-`JsonNode` "node graph" prototype into a
principled, **SAF-backed**, **schema-typed** standalone service that mirrors
`nba-client-backend`'s composition-module conventions — so it can later
lift-and-shift into the server shell as `server:sdui-core`.

The service:

- uses **SAF** (`com.nba:service-aggregation-framework`) for all upstream fetching
  (resilience, two-tier caching, request collapsing, graceful degradation,
  SLO/metadata) instead of raw OkHttp;
- composes against the **codegen-generated typed models**
  (`Screen` / `Section` / `AtomicElement`) with `schema/sdui-schema.json` as the
  wire contract (AGENTS.md §1.2), instead of stitching raw `ObjectNode` trees;
- is organized into `nba-client-backend`'s composition-module vocabulary
  (`controller` → `orchestration` → pure framework-free `domain` composer →
  `remote`/`port`), enforced by ArchUnit.

**Response envelope — adopt the *shape* early, but keep it out of the schema.**
The shell's modular-monolith law (`nba-client-backend` ADR-001 **D7**) is that
*every* response — external edge **and** every seam, all modules — is
`ResponseEnvelope<T>{data, meta}`, with `meta` the standard
`degraded`/`staleSections`/`failedSections` signal. SDUI must conform on
integration. **Decision: introduce the envelope shape in the prototype now**
(Phase A2a) rather than at the port, so the SDUI clients can adopt the
`.data`/`.meta` access pattern ahead of integration. `data` = the existing
`Screen`/`Section`; `ErrorState` sections stay the in-band failure mechanism
(orthogonal to `meta`).

Two constraints govern *how* it's introduced:

- **The envelope is a transport wrapper, NOT a schema type. Do not add it to
  `sdui-schema.json`.** The prototype is a closed world (one server, three
  first-party clients shipped in lockstep), so the wrapper is hand-written in
  each platform's shared fetch/repository layer and in the server's controller
  return type — it is not a codegen-generated model. The schema continues to
  govern the `Screen`/`Section` that sits inside `data`; strict decoding of that
  inner payload is unchanged. **This deliberately departs from AGENTS.md §1.2's
  "every emitted field in the schema first": the envelope adds no *content*
  fields — it is pure transport framing around the schema-governed body.**
  Because §1.2 is a constitutional rule, this exception is recorded as a
  first-class doctrine edit (an amendment to AGENTS.md §1.2 or a new ADR
  covering transport framing), not just a sentence in `sdui-envelope-spec.md`.
  The amendment is a deliverable in A2a — see todos.
- **`meta` is mocked until SAF lands.** Introduce the envelope with a static
  stub `meta` (`degraded:false`, empty `staleSections`/`failedSections`) so the
  wire shape clients consume is final from day one. Real `meta` is wired from
  `orchestrator.buildMetadata()` when SAF orchestration lands (A2c). Clients
  consume `.data` immediately and ignore `.meta` until a much later
  freshness/degradation milestone — no renderer or binding work is gated on it
  now.

This is a coordinated client-facing wire change (server/client contracts,
envelope spec, all three clients) — but **not** a schema change.

Sequencing: **restructure → SAF → typed models**, each behind the existing
contract/round-trip test safety net. The service keeps running from this repo;
no Gradle module split yet.

### Plan structure — Phase A prepares, Phase B ports

| | Phase A (A1–A3) — **active scope** | Phase B — **future, not executed now** |
|---|------|------|
| **Where** | `sdui-prototype/server` only | `nba-client-backend/server:sdui-core` |
| **Goal** | Port-*ready* standalone service | Lift-and-shift + shell registration |
| **Shell docs** | Read as **design target** (why shape matches WECS/CAS) | Execute against (register module, seams, boot wiring) |
| **WECS** | `WatchExperiencePort` + HTTP/stub adapter | `WatchExperienceAccess` in-process seam |
| **CORS** | Prototype `WebConfig` (standalone deployable) | Shell-owned `nba.web.cors.*` |
| **ArchUnit** | Package-level guards in this repo | `ModuleConformanceArchTest` + `ModuleBoundaryArchTest` |

Shell module-authoring references during Phase A are intentional: they explain
*constraints on today's work*, not instructions to work in the shell repo yet.

**First intermodule integration target: WECS** (`wecs-core`, the watch-experience
composition module). SDUI will source watch CTAs / stream selection / resume /
upsell / blackout messaging from WECS rather than recomputing them. To keep the
design seam-edged from day one, the standalone service fetches that data behind a
`WatchExperiencePort` (A2d); on port into the shell that port becomes the first
in-process consumption of a new, additive `WatchExperienceAccess` seam (B).

### Extraction-readiness (overriding constraint)

Per `nba-client-backend` AGENTS.md, extraction-readiness beats every other
concern: any module must be carvable into a standalone microservice **with no
change to its consumers or to SAF wiring**. This cuts two ways for SDUI and
shapes the whole plan:

- **SDUI must be carvable.** Phase A is, in effect, that proof: `sdui-core` is
  built and proven as a standalone deployable in this repo first. Controller →
  orchestration → pure `domain` composer → `remote` is a self-contained vertical;
  extraction = add a `main` + boot config, not re-home logic.
- **SDUI must survive WECS being carved out.** The WECS edge is a seam, never a
  module reference: SDUI depends on the `WatchExperienceAccess` *seam*, bound at
  runtime to `InProcessWatchExperienceAccess` (monolith) or `RemoteWatchExperienceAccess`
  (HTTP, after WECS extraction) by the boot shell via `@Profile`. SDUI code and
  its SAF `ServiceCall` do not change either way — which is exactly why the
  standalone `WatchExperiencePort` already models this boundary.
- **Boundary hygiene that keeps both true:** contracts stay framework-free in
  `integration-models` (no SAF/Spring, no `ServiceMetadata` on domain contracts);
  cross-cutting concerns (auth token, correlation id, deadline, platform/device)
  ride headers + SAF `ExecutionContext`, not a shared body DTO; cache keys are
  explicit and never embed the auth token; no module depends on another module's
  implementation. ArchUnit enforces these so drift fails the build, not review.

---

## Current state (verified)

- Composers (`HomeComposer`, `ScoreboardComposer`, …) build raw Jackson
  `ObjectNode`; most delegate UI trees to the **3431-LOC** `AtomicCompositeBuilder`,
  but **`BoxscoreComposer` and `CalendarComposer` hand-build nodes directly**
  (not via the builder). Return types are `JsonNode`/`ObjectNode`, except
  `GameDetailComposer` which wraps in a `GameDetailResult(JsonNode, String)` record.
- Generated Java POJOs exist
  (`codegen/build/generated-sources/jsonschema2pojo/com/nba/sdui/models/generated/`)
  but are **unused**; `server/build.gradle.kts` has a dead `copyGeneratedModels`
  task.
- `StatsApiClient` uses OkHttp directly — no SAF, resilience, caching, or
  parallel fetch.
- `SduiController` repeats trace-id / version-gate / cache-control /
  version-filter boilerplate in every endpoint.
- Safety net: `ComposerRoundTripTest`, `ScreenChannelContractTest`,
  `SectionChannelContractTest`, `ExampleWireContractTest`,
  `ComposerEndpointRegressionGuardTest`.

---

## Phase A1 — Architectural restructure (behavior-preserving)

> **Module surfaces (port-readiness target):** exception handler + health indicator
> patterns mirror `nba-client-backend/docs/guides/module-authoring-guide.md` steps
> 8–9 — copy WECS/CAS shape in *this repo*, not in the shell. See **Reference
> documents → Port-readiness design target**.

Reorganize `com.nba.sdui` into the target vocabulary **without changing wire
output**:

- `…controller` — thin HTTP surface (`SduiController`, `GamesController`) + edge
  DTOs only.
- `…orchestration` — new home for screen-assembly coordination (today's
  `SduiCompositionService`, `SectionRefreshService`, `ParameterizedRefreshService`).
- `…domain` — composers move here as **pure** units `(resolved data) → screen`;
  strip `@Value`/Spring where feasible. Token constants (`LayoutTokens`,
  `ColorTokens`, …), `SectionSurfaces`, and `AtomicCompositeBuilder` become a
  domain element-factory. This realizes the contract's **pipeline stages [C]–[F]**
  (experiment resolution → section composition → screen assembly) under the
  **§5.2 purity requirement**: no wall-clock/random/env reads, so section
  composition `[E]` is cacheable and screen assembly `[F]` is not. The
  controller/orchestration boundary maps to stages `[A]` (envelope decode),
  `[B]` (version gate), `[D]` (upstream fetch), `[H]` (version stripping),
  `[I]` (cache-control).
- `…remote` + `…domain.port` — introduce a `ScoreboardPort`/`StatsPort` seam;
  wrap the existing `StatsApiClient` behind a `remote` adapter (still OkHttp at
  this step).
- Keep `request`, `versioning`, `config` as cross-cutting.
- **Slim the controller:** move version gate, response headers, cache-control,
  and version filtering out of `SduiController` into module-internal Spring
  extension points so each endpoint is a 3-liner. Pin the mechanism per concern
  (so the implementer doesn't reopen the design):
  - **Version gate (request-side, may short-circuit with 426):** `HandlerInterceptor`
    registered against SDUI controller paths only.
  - **Cache-Control + version-mismatch response headers:** same `HandlerInterceptor`'s
    `postHandle` (or a `ResponseBodyAdvice` if the value depends on the body),
    keeping all header-writing in one place.
  - **Version filtering of the composed response body** (stripping fields newer
    than the client's `schemaVersion`): `ResponseBodyAdvice<Object>` scoped to
    the SDUI controller package — runs after composition, before serialization.
  - **`@ControllerAdvice` is reserved for the RFC-7807 exception handler below**
    and is not used for the boilerplate above.
  All three are module-internal (shared across SDUI endpoints, not across the
  shell). **Trace-id plumbing is deleted, not moved** — see below; it's owned
  by SAF in A2b.
- **Correlation id replaces hand-rolled trace-id (SAF-owned).** The bespoke
  `ensureTraceId` + manual `MDC.put("traceId", …)` + `setHeader("X-Trace-Id", …)`
  in `SduiController` are removed in favor of SAF's `CorrelationIdFilter`
  (extract-or-generate, format-validate, MDC under `correlationId`, echo on
  response) plus `ExecutionContext` propagation into virtual threads and (via the
  SAF client interceptor) into upstream calls. This satisfies the contract's
  trace obligations (§2.1 echo, §8.3 upstream propagation, §10.2 log correlation)
  for free. **Decision: standardize on `X-Correlation-ID` everywhere** (SAF's
  hardcoded header) so the wire name is identical in the standalone service and
  after the port — no shim, no SAF enhancement. **Correlation id is a
  transport/observability concern — it lives only in the HTTP header, never in
  the wire body.** The current schema field `Screen.traceId` (`sdui-schema.json`
  line ~1338) and the composers' `response.put("traceId", …)` duplicate the
  header value into the UI contract. **Audited consumers of the body field
  (verified, June 2026):**
  - **iOS** — `SduiScreenViewModel.replaceCurrentScreen` / `replaceSection`
    pass `screen?.traceID` into `repository.fetchScreen` / `fetchSection` to
    seed the continuation trace on refreshes.
  - **Android** — `SduiScreenController` reads `currentScreen?.traceID` /
    `screen.traceID` in five places: section refresh (line 443), raw-data poll
    (line 469), screen-loaded log (line 334), and two `DataBindingResolver`
    log-tagging call sites (lines 525, 702).
  - **Web** — does **not** read body `traceId`; `fetchSduiScreen` generates the
    trace client-side and returns it from input.
  - **Tests/fixtures** — present in fixture JSON across all three clients;
    fixtures are regenerated as part of the cut-over.
  Removing the body field is therefore **not** a "stop reading it" change on
  iOS/Android — the repository fetch result must newly expose the response
  `X-Correlation-ID` header so the view models can seed continuation traces
  from the header instead of the body. This adds a small, additive surface to
  each platform's fetch result type (e.g. an `ScreenFetchResult{screen,
  correlationId}` or trailing param) that did not previously exist.
  **Decision (unchanged):** delete `Screen.traceId` from the schema and stop
  composers emitting it rather than renaming the body field to `correlationId`.
  The coordinated change is: (a) header rename `X-Trace-Id` → `X-Correlation-ID`
  across `server-implementors-contract.md`, `client-implementors-contract.md`,
  `sdui-envelope-spec.md`, the version-mismatch response headers, and all three
  clients; (b) repositories on iOS/Android expose the response correlation
  header to view models, replacing the body-field read; (c) **removal** of the
  body `traceId` field from the schema + composers + clients. **Removal-direction
  sequencing (inverse of §1.2's additive order):**
  1. iOS / Android repositories surface the response header to view models;
     view models stop reading `screen.traceID` (still emitted by server, just
     ignored).
  2. Server composers stop emitting `response.put("traceId", …)`. Clients are
     already not reading it — server emits, clients ignore.
  3. Remove `Screen.traceId` from `sdui-schema.json`; run `make codegen`;
     update fixtures and the schema-conformance test (A3).
  Steps 1 and 2 land in the same A2a release because the prototype is a closed
  world; step 3 is the doctrine-clean tail. (SAF `CorrelationIdFilter` lands in
  A2b; the doc + client rename + schema-field removal lives in A2a so SAF
  adoption isn't gated on a wire change.)
- **Versioning is SDUI-owned, not SAF/shell.** Schema-version field stripping
  (`SchemaVersionRegistry` / `SchemaVersionFilter` / `SchemaVersionChecker` + the
  version gate) is intrinsic to SDUI's **generic section-tree response** and
  travels with the module — it does **not** hoist to SAF or to
  `nba-client-backend-server`. Typed-DTO modules (cas-core, wecs-core) version via
  URI + additive optional fields and have no equivalent. Revisit hoisting to SAF
  only if another module adopts the generic-envelope response model. On extraction
  this whole subsystem stays inside `sdui-core`.
- Add **ArchUnit** tests mirroring the shell's guardrails: no Spring in
  `…domain`; no upstream-client types (OkHttp today, RestClient in A2b) in
  `…domain`; `@RestController` only in `…controller`. The SAF-exclusion rule on
  `…domain` lands now (defensive — cheap to add before SAF arrives, and prevents
  drift in the A2b interval). The RestClient-only-in-`…remote` rule is added
  with the OkHttp→RestClient swap in A2b.
- **Exceptions → RFC-7807 (module-authoring guide §2, step 8).** Replace the
  controller's `internalServerError().build()` / `notFound()` / `badRequest()`
  with an `@Order(HIGHEST_PRECEDENCE)`, `basePackages`-scoped
  `SduiExceptionHandler` (`@RestControllerAdvice`) emitting `ProblemDetail`, plus a
  semantic hierarchy (`SduiException` base → `InvalidRequestException`=400,
  `UpstreamServiceException`=503, `UnsupportedSectionException`=400/404). **This is
  transport-level only and coexists with the composition-level failure doctrine:**
  unrecoverable *composition* failures still return an `ErrorState` **section/screen**
  (server contract §3.5/§8, AGENTS §8.0), never a ProblemDetail — ProblemDetail is
  for malformed requests, version-gate 426, and unknown screen/section ids.
- **Health → indicator, not controllers (guide §2, step 9).** Remove the hand-rolled
  literal `/health`, `/healthz`, `/readyz` endpoints from `SduiController` (an
  explicit anti-pattern) in favor of an `SduiHealthIndicator` bean + a `sdui`
  actuator health group, using `/actuator/health/{liveness,readiness}` for k8s
  probes. (Migrate any k8s manifests/Make targets that point at the old paths.)

**Gate:** full existing test suite stays green.

---

## Phase A2 — SAF integration (upstream fetching)

> **SAF adoption docs:** see **Reference documents → SAF adoption**
> (`service-aggregation-framework/docs/…` — note: that repository lives outside
> this workspace; paths there are not resolvable from `sdui-prototype`). Read
> `plan-service-metadata-freshness.md` before wiring envelope Step 2
> (`buildMetadata()` → `ResponseMeta`).

**A2 is split into four sub-phases** to keep each cut-over independently
shippable and revertible. The original A2 bundled four unrelated cut-overs
(wire-shape rename, SAF transport, real `meta` + section-fragment cache, WECS
seam) into one big-bang phase; that conflated risk profiles and rollback
stories. The split:

| Sub-phase | Scope | Touches clients? | Depends on |
|-----------|-------|------------------|------------|
| **A2a** | Wire-shape lockstep: `X-Correlation-ID` rename, `Screen.traceId` body removal, `ResponseEnvelope<T>` Step 1 (stub `meta`), AGENTS.md §1.2 amendment / ADR | **Yes** (all 3) | A1 |
| **A2b** | SAF dependency, virtual threads, OkHttp → RestClient remote adapters, `OrchestratorFactory` + `ServiceCall` per upstream, per-upstream `saf.services.*` config, contract §10.1 metrics, `CorrelationIdFilter` | No (server-only) | A2a |
| **A2c** | Real `meta` (envelope Step 2 from `orchestrator.buildMetadata()`), section-fragment cache (contract §6) — unblocks `plan-server-section-caching.md` | No (clients still ignore `.meta`) | A2b |
| **A2d** | `WatchExperiencePort` + `RestClient` adapter / profiled stub + watch-oriented composer wiring as optional SAF `ServiceCall` | No (server-only) | A2b |

A2a is the only sub-phase requiring three-client coordination. A2c and A2d are
independent of each other and may ship in either order once A2b lands.

- Add `com.nba:service-aggregation-framework:1.0.0-SNAPSHOT` (Maven Local) to
  `server/build.gradle.kts`; enable `spring.threads.virtual.enabled=true`. Add
  `publish-saf` / `sync-saf` Make targets (not yet in this repo's Makefile).
- **Context propagation API note:** SAF's `ServiceOrchestrator` already wraps each
  parallel `ServiceCall` with `ExecutionContext` internally (MDC + `DeadlineContext`
  + Observation). Any *manual* virtual-thread spawning in orchestration uses
  `ExecutionContext.capture(observationRegistry)` then the **instance**
  `ctx.wrap(supplier)` — there is no static `ExecutionContext.wrap()`.
- Replace OkHttp `remote` adapters with **`RestClient`**-based ones behind the
  `domain.port` seams (Akamai browser headers, OPIM keys preserved).
- Build a SAF `orchestration` layer: per request,
  `OrchestratorFactory.create(timeout)` + one `ServiceCall` per upstream
  (scoreboard CDN, per-date Core API, boxscore, season schedule, stats
  trafficcop), `failOnError` true for critical / false for optional,
  `.cache(key, ttl, STALE_IF_ERROR)`.
- Add `saf.services.<name>` config (scoreboard-cdn, core-api, boxscore-cdn,
  stats-trafficcop, league-schedule) with TTL/circuit-breaker/bulkhead/retry;
  expose `health,prometheus,metrics` actuators.
- **Declare per-upstream policy (contract §8.1)** for each feed: TTL (live vs
  post/final), `stale-if-error` window, criticality (`required` → `ErrorState`
  on hard failure vs `optional` → omit section), request-collapsing scope, and
  per-call timeout budget. Failure handling per §8.2 (never invent content;
  partial responses render nothing, not defaults).
- **Section-fragment caching (contract §6) — unblocks `plan-server-section-caching.md`.**
  SAF supplies the upstream-data layer (raw feed bytes, `stale-if-error`) and the
  section-fragment layer (output of a single section composer). Screen assembly
  stays **uncached** (§6.1). Section-fragment keys carry `sectionType`,
  `contentHash`, `deviceClass`, `schemaVersion`, `experimentBucket` and **must
  not** carry `theme`/`density`/`fontScale`/`formFactor`/`deviceId`/time-of-day
  (§6.2). Pick and document one i18n policy (stamp-before- vs stamp-after-cache,
  §6.3) uniformly across composers.
- Emit contract §10.1 metrics (`sdui.composition.duration`, `sdui.cache.hit/miss`
  by layer, `sdui.upstream.duration`, `sdui.version.mismatch`,
  `sdui.section.refresh`). Correlation/trace propagation into upstream calls and
  logs (§8.3 / §10.2) comes from SAF's `CorrelationIdFilter` + `ExecutionContext`
  + client interceptor — no manual plumbing.
- **Introduce the `ResponseEnvelope<T>{data, meta}` wire *shape* now — mocked
  `meta` first, real `meta` when SAF lands.** The shell mandates a uniform
  envelope at every edge/seam (`nba-client-backend` ADR-001 D7); rather than wait
  for the port, wrap SDUI's edge here so clients adopt the `.data` access pattern
  early. `data` = the composed `Screen`/`Section`. Sequence it in two steps so the
  shape is final from day one but isn't gated on SAF:
  - **Step 1 (shape, can land before SAF orchestration is wired):** the
    controller returns `ResponseEnvelope<Screen>` / `ResponseEnvelope<Section>`
    with a **static stub** `meta` (`degraded:false`, empty `staleSections` /
    `failedSections`). The envelope record is a hand-written server type (a
    framework-free `record ResponseEnvelope<T>(T data, ResponseMeta meta)`
    mirroring the shell's `integration-models` shape so the port is a type
    re-registration, not a reshape). **It is NOT added to `sdui-schema.json`** —
    it is transport framing, not a content type (see Goal & shape). Both
    screen-channel and section-channel responses wrap.
  - **Step 2 (real `meta`, once SAF orchestration is in place):** populate `meta`
    from `orchestrator.buildMetadata()` — map per-call-id `ServiceCallInfo`
    (`status` FAILED/TIMEOUT → `failedSections`; `servedStale==true` →
    `staleSections`; `partialFailure` → `degraded`). A fresh in-TTL cache hit
    (`cacheSource` L1/L2 but not `servedStale`) is **not** degraded. `ErrorState`
    sections remain the in-band composition-failure mechanism, orthogonal to
    `meta`.
  - **Clients consume `.data` immediately and ignore `.meta` until much later.**
    No renderer/binding work is gated on `meta`; the freshness/degradation
    consumer is a separate future milestone.
  - Coordinate the matching client change + doc updates (server/client
    implementors contracts, `sdui-envelope-spec`) in lockstep — **no schema
    change, no backward-compat shim.** On the Phase B port this is already
    conformant: only re-register SDUI's envelope/`meta` against the shared
    `integration-models` types — no second wire change.
- **WECS seam (first intermodule edge, A2d):** introduce `WatchExperiencePort` in
  `…domain.port` returning the existing `integration-models` watch contract shape
  (`WatchExperienceResponse`: CTA / stream / resume / upsell / messaging). In the
  standalone service back it with a `…remote` `RestClient` adapter to a WECS
  endpoint (or a profiled stub until WECS exposes one). Orchestration adds it as a
  SAF `ServiceCall` (optional, `failOnError(false)`, cached `STALE_IF_ERROR`); a
  watch-oriented composer (e.g. game-detail) maps the response into SDUI sections.
  The port is the swap point that becomes the in-process seam in Phase B.
  **Stub-rot mitigation:** the profiled stub must satisfy a contract test that
  pins it to the real `WatchExperienceResponse` DTO shape (decode round-trip plus
  required-field coverage), so it cannot drift into a `null`-returner. If WECS
  has not exposed an endpoint by the time A2d is ready to start, A2d may be
  deferred until either (a) WECS exposes an endpoint, or (b) a concrete SDUI
  surface (e.g. game-detail watch CTAs) demands the port — the goal is to avoid
  carrying a real-but-unused port + adapter as dead-ish code on `main`.
- **Deferred: load-bearing `platform` on WECS-backed surfaces (shell ADR-005).**
  Per ADR-005, "platform" is two signals: (a) **load-bearing `platform`** — device
  identity (`ios`/`android`/`web`/`roku`/`ctv`) that changes the response (stream
  selection/ordering, MediaKind STS) and is cache-keyed on CAS/WECS; and (b)
  **analytics platform** — telemetry only, the `X-Analytics-Platform` header
  (already renamed from `X-Platform` across SDUI server/clients/docs/schema),
  **never** read into composition and **never** in a cache key (reinforces §6.2).
  Today `WatchComposer` composes from stub/`StatsApiClient` and does **not** call
  WECS, so no surface is load-bearing on `platform` yet. Decision: fix the
  convention now; **defer** adding load-bearing `platform` to a surface's
  canonical (cache-keyed) query string until SDUI actually composes WECS — tracked
  in `nba-client-backend/docs/plans/plan-request-contract-cacheability-alignment.md`.
  The SDUI envelope's existing `platform[deviceClass]` / `platform[name]` fields
  are unchanged by this.

### A2a client cut-over (this repo — `ios/`, `android/`, `web/`)

The A2a wire changes are breaking and ship as one lockstep cut-over with the
server (no backward-compat shim). Each change touches the shared client
infrastructure only — repositories/runtime — never the section renderers.
**Neither change touches `sdui-schema.json` for content fields:** the body
`traceId` field is *removed* (the only schema delta), and the envelope is a
hand-written transport wrapper in each repository, not a generated type.

- **`X-Correlation-ID` rename + body-field removal + repository return-type
  change.** Repositories send/propagate `X-Correlation-ID` (was `X-Trace-Id`).
  The body `traceId` field is **removed**; correlation is header-only. Because
  iOS and Android view models currently read `screen?.traceID` to seed
  continuation traces on refreshes, **the repository fetch result must newly
  expose the response correlation header** (e.g. via a `ScreenFetchResult` /
  `SectionFetchResult` carrier or trailing param) so view models can read it
  from there instead of from the body. Web does not read body `traceId` and
  needs only the header rename. Touch points:
  - **iOS:** `ios/Sources/SduiCore/Network/SduiRepository.swift` (return type +
    header propagation); `RequestEnvelopeBuilder` (header rename);
  `SduiScreenViewModel.replaceCurrentScreen` / `replaceSection` (read
    correlation from result instead of `screen?.traceID`).
  - **Android:** `android/sdui-core/.../data/SduiRepository.kt` (return type +
    header propagation); `RequestEnvelopeBuilder` (header rename);
  `SduiScreenController` lines 232 / 334 / 443 / 469 / 525 / 702 (read
    correlation from result; `DataBindingResolver` log-tagging call sites take
    the header value instead of `screen.traceID`).
  - **Web:** `web/src/runtime/fetchSduiScreen.ts` + `RequestEnvelopeBuilder`
    (header rename only — no body-field reader to migrate).
  Regen `SduiModels.{swift,kt,ts}` via `make codegen` after `Screen.traceId` is
  dropped from `sdui-schema.json`.
- **`ResponseEnvelope<T>` adoption (hand-written wrapper, not codegen).** The
  single fetch path unwraps `.data` into the existing `Screen`/`Section`. Decode
  of the inner payload stays strict. The wrapper type
  (`{ data, meta }`) is defined **by hand** in each repository layer
  (`SduiRepository.swift` / `.kt`, `fetchSduiScreen.ts`) — it is **not** added to
  `sdui-schema.json` and **not** regenerated by `make codegen`. `.meta` is
  decoded and held but **not consumed yet** — clients use `.data` only; the
  freshness/degradation consumer is a later milestone.
- **No renderer churn.** `.data` flows to renderers exactly as the bare
  `Screen`/`Section` does today; `.meta` is parked in shared infra. Section
  renderers and adapters are untouched.
- **Per-platform test updates.** Refresh the repository/runtime fetch tests
  (`SduiScreenControllerTest.kt`, `SduiScreenViewModelRefreshTests.swift`,
  `fetchSduiScreen.test.ts`) and decode fixtures for the wrapped shape (the inner
  `Screen`/`Section` fixtures still validate against the schema).

**Gates (per sub-phase):**
- **A2a:** all three client test suites green against the wrapped edge;
  contract tests green; doctrine amendment landed (AGENTS.md §1.2 or new ADR).
- **A2b:** server-only — contract tests green; cache/circuit-breaker metrics
  visible on `/actuator/prometheus`; ArchUnit RestClient-only-in-`…remote`
  rule active; all hand-rolled trace-id plumbing deleted from
  `SduiController`.
- **A2c:** server-only — `meta` populated from real `buildMetadata()`;
  section-fragment cache hits visible in metrics; `plan-server-section-caching.md`
  is now executable.
- **A2d:** server-only — watch composer wired to `WatchExperiencePort`; stub
  contract test pinned to `WatchExperienceResponse` DTO; SAF `ServiceCall` for
  WECS observable in metrics.

---

## Phase A3 — Adopt codegen typed models (schema as the contract)

- Put the generated POJOs on the server compile classpath properly (run
  `generateJsonSchema2Pojo` as a build dependency / shared source set; delete the
  dead copy task). Regenerate via `make codegen`.
- Migrate composition from `ObjectNode` to generated `Screen` / `Section` /
  `AtomicElement` types — the element-factory returns typed objects; Jackson
  serializes. Do it **composer-by-composer** behind round-trip tests.
- **Hand-built composers (named so they're not lost in the migration):**
  `BoxscoreComposer` and `CalendarComposer` build raw `ObjectNode` trees
  *without* going through `AtomicCompositeBuilder`. They are the highest-risk
  composers for the typed-model migration because they bypass the helper layer
  entirely. Migrate them as separate, named todos rather than folding them
  into a generic "all composers" sweep.
- Producer keeps strict generated enums (valid-by-construction); routing-type
  leniency stays a client-only codegen concern.
- Add a **schema-conformance test**: every composed screen/section validates
  against `schema/sdui-schema.json`. **Expect this to surface pre-existing
  composer drift** that the round-trip tests don't catch (today's composers
  have never been validated end-to-end against the schema). Budget composer
  fixes inside A3, not just the typed-model migration.

### A3 addendum — concrete migration waves and surface inventory

Added after the runtime-tokens refactor (Concern 1) landed. The
`ObjectNode`-everywhere shape is the next strategic concern (Concern 2):
composers, `AtomicCompositeBuilder`, `SectionSurfaces`, `SduiUtils`, and the
controller edge all traffic in untyped `JsonNode` / `ObjectNode` even though
the generated POJOs (`SduiModels.java`) already exist. The risk surface that
this masks today:

- A composer can emit any field name without a compile error; only
  round-trip tests + the (still-future) schema-conformance test catch it.
- Renaming a schema field is a manual sweep across every `put("…", …)` call
  site rather than a refactor the compiler proves correct.
- Controllers return `ResponseEntity<JsonNode>`, so client-facing payload
  shape changes never trip a server-side compile.

#### Inventory (audit done at end of A2b, kept here so A3 starts from facts)

- `AtomicCompositeBuilder` — ~50 element-builder methods all returning
  `ObjectNode` (responsiveRow, header, button, image, divider, spacer,
  …). Single hottest path. Touched by every composer.
- `SectionSurfaces` — section-envelope helpers (`surface()`, `padded()`,
  `margin()`, …) all returning `ObjectNode`. Layer between composer and
  `AtomicCompositeBuilder`.
- `SduiUtils` — screen-level helpers (`baseScreen()`, `tabBar()`, …)
  returning `ObjectNode`.
- Composers (10) — `Home`, `ForYou`, `Live`, `Watch`, `Schedule`,
  `Scoreboard`, `GameDetail`, `DemoScreen`, `Calendar`, `Boxscore`.
  `Calendar` and `Boxscore` bypass `AtomicCompositeBuilder` entirely
  (see named-risk note above).
- Controllers (`SduiController` + section-channel endpoint) — return
  `ResponseEntity<JsonNode>` / `ResponseEntity<ObjectNode>` wrapped in
  `ResponseEnvelope<JsonNode>`. The transport-framing exception (AGENTS.md
  §1.2) keeps `ResponseEnvelope` hand-written, but `data` is supposed to
  be schema-bound — it isn't until A3 lands.

#### Wave plan (each wave is independently shippable + reversible)

1. **Wave 1 — leaf element factories.** Migrate
   `AtomicCompositeBuilder` method signatures one family at a time
   (`button` family, `image` family, `layout` family, …) to return the
   generated `AtomicElement` / typed children. Composers keep accepting
   `ObjectNode` from their existing calls during the wave by converting
   at the boundary. **Gate per family:** round-trip tests stay green;
   diff the serialized output bytes before/after.
2. **Wave 2 — composer-by-composer typed pipeline.** With the leaf
   factories typed, each composer migrates its tree construction to typed
   `Section` / `Screen`. Bottom-up: `Scoreboard`, `Watch`, `Home`,
   `Schedule`, `ForYou`, `Live`, `GameDetail`, `DemoScreen`,
   `Boxscore`, `Calendar` (the last two are the bypass cases — schedule
   them at the **end** of Wave 2 since they have no helper-layer scaffolding
   to lean on). `SectionSurfaces` + `SduiUtils` migrate alongside the
   composer that first exercises each helper.
3. **Wave 3 — controller-edge typed serialization.** Once every composer
   returns typed objects, change
   `ResponseEntity<JsonNode>` → `ResponseEntity<Screen>` (and
   `ResponseEntity<Section>` for the section channel). `ResponseEnvelope`
   stays hand-written (transport-framing exception) but becomes
   `ResponseEnvelope<Screen>` / `ResponseEnvelope<Section>` with a typed
   `data` field. Add the schema-conformance test as the final A3 gate.

#### Why this is sequenced after A2 (not before)

The A2 work (SAF integration, real meta, section-fragment cache) introduces
new payload shapes — typing the pipeline first would mean re-typing the
same factories every time A2 adds a field. A3 batches the typed-model
migration after the payload surface has stabilized.

**Gate:** all composers typed; schema-validation + contract tests green.

---

## Scope boundaries — kept SDUI-local / not adopted from SAF

Decisions carried from a prior session; recorded so the port doesn't reopen them:

- **Schema versioning stays an SDUI-module concern.** Not promoted to SAF, not
  adopted by the shell's typed-DTO modules — it is tied to SDUI's generic
  section-tree response (see the A1 versioning note). "Don't need, not
  cross-cutting."
- **CORS is an application/shell concern, not SAF (shell ADR-004).** Standalone,
  SDUI keeps its own `WebConfig` CORS. **On port, CORS does not travel with the
  module** — it becomes shell-owned (`nba.web.cors.*` on `nba-client-backend-server`:
  config-driven, deny-by-default, `/api/**`-scoped, exposing `X-Correlation-ID`).
  `sdui-core` owns its controllers but not global web policy. Never push CORS into
  SAF.
- **`BracketParamResolver` + `X-Request-Id` stay SDUI-local for now.** Judged
  nice-to-have and recorded as **SAF backlog** in
  `service-aggregation-framework/docs/plans/plan-request-layer-ergonomics.md`; not
  built now. SDUI keeps its own bracket-notation envelope resolver in the meantime
  (envelope parsing per server contract §4 remains SDUI-owned).

---

## Phase B — Port into `nba-client-backend` as `server:sdui-core` (future, not executed now)

> **Port execution checklist:** see **Reference documents → Port execution (Phase B
> only)**. This phase is not active scope — listed so Phase A does not foreclose
> a clean lift-and-shift.

The shell charter already names "SDUI next" and prescribes the shape
(composition module like `wecs-core`):

- New module `server:sdui-core` (`com.nba.client.backend.sdui`): `controller` +
  `orchestration` (SAF) + pure `domain` composer + `remote` (upstreams via
  `integration-clients`) + `intermodule` (consume sibling-module seams).
- **First intermodule edge — WECS (additive seam).** Today only `EntitlementAccess`
  exists (cas-core → wecs-core) and WECS exports no seam. Add a new
  `WatchExperienceAccess` seam in `integration-models/api` (additive — no existing
  contract changes), implemented by `InProcessWatchExperienceAccess` in
  `wecs-core.intermodule` wrapping `WatchExperienceOrchestrator`. `sdui-core`
  consumes it via a `WatchExperienceAccessCallFactory` in `sdui-core.intermodule`,
  which is exactly what the standalone `WatchExperiencePort` (A2) swaps to. Reuse
  the existing `com.nba.integration.contract.watch.*` DTOs — no new contract types
  needed. Extraction later swaps `InProcess…` → `Remote…` under `@Profile` with no
  consumer change.
- Wire-contract DTOs / any inter-module seams go in framework-free
  `integration-models`; register module in `ModuleConformanceArchTest`; bind in
  the `nba-client-backend-server` boot shell.
- `WatchExperienceAccessCallFactory` wraps the consumed seam in a SAF `ServiceCall`
  **and translates WECS seam exceptions into SDUI's own `UpstreamServiceException`**
  (guide §2, step 3 — never depend on the producer's exception types, so extraction is
  clean either direction).
- Because A1–A3 already match the vocabulary, this is largely lift-and-shift:
  controllers/orchestration/domain move; `remote` swaps RestClient adapters to
  `integration-clients`; the `WatchExperiencePort` swaps to the `WatchExperienceAccess`
  seam; ArchUnit + boot wiring added.

### Module-authoring conformance (per
`nba-client-backend/docs/guides/module-authoring-guide.md`)

SDUI is a **composition** module (like `wecs-core`). Build A1–A3 so it already
satisfies the guide's required surfaces, then registration in
`nba-client-backend/server/nba-client-backend-server/src/test/java/com/nba/client/backend/architecture/ModuleConformanceArchTest.java`
is the trigger that enforces the full set. See **Reference documents → Shell
module authoring** for ADRs, ArchUnit tests, and WECS/CAS reference implementations.

- ✅ `controller` + edge resolver, `orchestration` (SAF only), pure `domain`
  composer, `remote` adapters, `intermodule` call-factory — all produced in A1–A3.
- ✅ `SduiExceptionHandler` (ordered, `basePackages`-scoped, RFC-7807) + semantic
  exception hierarchy — A1.
- ✅ `SduiHealthIndicator` + `sdui` actuator health group — A1.
- ✅ SAF-only orchestration, explicit cache keys, freshness from
  `buildMetadata()` — A2.
- ✅ Library only — no `bootJar`/`main` (shell stays the sole deployable).

**Response envelope: introduced in the prototype (A2), already conformant at the
port.** Every response in the monolith — external HTTP edge **and** every
intermodule seam, across all modules — is `ResponseEnvelope<T>{data, meta}`: `data`
is module-specific, `meta` (`degraded` / `staleSections` / `failedSections`) is the
**standard** freshness/degradation signal *everywhere* (`nba-client-backend` ADR-001
D7). Uniform `meta` is what makes the monolith extractable, so SDUI must conform —
and to spare the clients a second migration, SDUI adopts the envelope **early, in
Phase A2** rather than at the port.

- **Standalone (prototype, A2 onward):** SDUI's edge wraps in
  `ResponseEnvelope<Screen>` / `ResponseEnvelope<Section>`. `data` carries the
  existing `Screen`/`Section`; `meta` carries the standard signal (SDUI's
  per-section staleness/failure maps cleanly onto `staleSections` /
  `failedSections`, derived from `orchestrator.buildMetadata()`). `ErrorState`
  sections remain the in-band composition-failure mechanism — `meta` is the
  orthogonal, uniform degradation signal, not a replacement.
- **Integrated (`sdui-core`, Phase B):** **already conformant** — the edge and any
  seam SDUI exports already return `ResponseEnvelope<T>`. The port only *registers*
  SDUI's envelope/`meta` types against the shared `integration-models` contract and
  folds consumed-seam `meta` (e.g. WECS) into SDUI's own. No second wire change.
- **This is a client-facing wire change owned at Phase A2, but NOT a schema
  change:** the SDUI clients and the wire docs
  (`server-implementors-contract.md` §3.1/§3.2,
  `client-implementors-contract.md`, `sdui-envelope-spec.md`) move from "screen
  channel returns a bare `Screen`" to "returns `ResponseEnvelope<Screen>`; clients
  read `.data` (and decode-but-ignore `.meta` for now)." The envelope is transport
  framing defined in code on both sides; it is **not** added to
  `sdui-schema.json`. Coordinated cut-over, sequenced like the
  `X-Correlation-ID` change — clean lockstep, no backward-compat shim.
- **Request side is unchanged by this:** the generic SDUI request envelope
  (`SduiRequestContext`) remains SDUI's request contract — SDUI-internal (shared
  across SDUI screens, not across modules), consistent with ADR-001's "no shared
  request envelope across modules."

SDUI *consumes* WECS's `WatchExperienceAccess` seam (which returns
`ResponseEnvelope<WatchExperienceResponse>`), unwrapping `data` into SDUI sections
and folding `meta` into its own response `meta`.

---

## Key risks / decisions

- **Element-factory rewrite is large (3431 LOC in `AtomicCompositeBuilder` —
  verified — plus the two hand-built composers `BoxscoreComposer` /
  `CalendarComposer` that bypass the builder entirely).** Mitigated by
  composer-by-composer migration behind existing round-trip/contract tests; the
  two non-builder composers are surfaced as named A3 todos so they aren't lost.
- **Three-client lockstep coordination cost (A2a).** The header rename + body
  `traceId` removal + envelope Step 1 ship as one cut-over across server, iOS,
  Android, and web with no backward-compat shim. In a closed prototype this is
  acceptable, but it requires a single coordinated PR (or a tightly-sequenced
  PR train) and a single release moment. A2b–d are server-only and don't
  reopen this window.
- **`Screen.traceId` removal is bigger than "stop reading the field."** iOS
  and Android view models read it for refresh-trace continuity (audited under
  Phase A1 todo); the repository fetch result on those platforms must newly
  expose the response `X-Correlation-ID` header. This is additive to each
  fetch result type (small), but it is real client work, not a string rename.
- **A3 schema-conformance test may surface pre-existing composer bugs.** Today
  composers build raw `ObjectNode` trees that have never been validated against
  `sdui-schema.json` end-to-end (only round-trip tests exist). The A3 conformance
  test is likely to find drift the round-trip tests don't catch. Budget time for
  composer fixes inside A3, not just the typed-model migration.
- **Generated POJO ergonomics** (mutable beans + builders +
  `additionalProperties`) — acceptable for a producer; revisit if builder noise
  hurts readability.
- **No Gradle module split yet** — service keeps running from `sdui-prototype`;
  restructure is package-level + ArchUnit only.
- **SAF via Maven Local** (`com.nba:service-aggregation-framework:1.0.0-SNAPSHOT`),
  mirroring `nba-client-backend`'s `publish-saf`/`sync-saf` pattern. **These Make
  targets do not yet exist in the `sdui-prototype` Makefile** — A2b must add them
  (publish SAF to Maven Local + a `sync` that refreshes the local snapshot)
  rather than relying on raw `gradlew publishToMavenLocal` incantations.
- **Body `traceId` consumers outside the audited surfaces.** The Phase A1
  audit covers `ios/`, `android/`, `web/`, server composers, fixtures, and
  `tools/`. If observability dashboards, log pipelines, or external integrations
  are reading the body `traceId` from response captures, the header-only switch
  may need a follow-up. Worth a sweep of any external consumer documentation
  before A2a ships.

---

## Todo checklist

### Phase A1 — restructure (server-only, no wire change) ✅

- [x] **A1** Reorganize `com.nba.sdui` into controller/orchestration/domain/remote
  (+`domain.port`); composers become pure domain units; introduce
  Stats/Scoreboard port + remote adapter wrapping existing `StatsApiClient`.
- [x] **A1** Extract version gate / cache-control headers / version-mismatch
  response headers into a module-internal `HandlerInterceptor` (request gate +
  `postHandle` for headers); extract response-body version filtering into a
  `ResponseBodyAdvice<Object>` scoped to the SDUI controller package.
  `@ControllerAdvice` is reserved for the RFC-7807 exception handler below.
  Trace-id plumbing is *not* moved — it's deleted in A2b when SAF takes over.
- [x] **A1** Add ArchUnit tests: no Spring in `…domain`; no upstream-client types
  (OkHttp now, RestClient in A2b) in `…domain`; SAF-exclusion rule on `…domain`
  (defensive — prevents drift in the A2b interval); `@RestController` only in
  `…controller`. RestClient-only-in-`…remote` rule lands with A2b.
- [x] **A1** Add `SduiExceptionHandler` (ordered, `basePackages`-scoped, RFC-7807)
  + semantic exception hierarchy, coexisting with `ErrorState`-section composition
  failures (ProblemDetail for transport/validation/version-gate/unknown-id only).
- [x] **A1** Replace literal `/health`,`/healthz`,`/readyz` controllers with an
  `SduiHealthIndicator` + `sdui` actuator health group; migrate k8s probe paths.
- [x] **A1** **Audit body-`traceId` consumers** — completed 2026-06-03.
  **No external code consumers found** in `tools/`, `helm/`, or `scripts/`.
  Confirmed in-tree client readers: iOS `SduiScreenViewModel` (refresh trace
  seed); Android `SduiScreenController` (six call sites); web does not read it.
  Doc references exist in `docs/sdui-requirements-summary.md`,
  `docs/SDUI_Technical_Proposal_v2.md`, `docs/contracts/{client,server}-implementors-contract.md`
  — these are already in scope for A2a's doc-update todos (header rename +
  `Screen.traceId` removal). **A2a scope is unchanged**; no new consumers to
  coordinate with.

### Phase A2a — wire-shape lockstep (server + 3 clients, breaking) ✅

- [x] **A2a** Amend AGENTS.md §1.2 (or land a new ADR) defining the
  transport-framing exception that lets `ResponseEnvelope<T>` exist outside the
  schema. Reference the amendment from `sdui-envelope-spec.md`. **Doctrine edit
  is a deliverable, not a footnote.** *(Landed as ADR-017.)*
- [x] **A2a** Server: rename response header `X-Trace-Id` → `X-Correlation-ID`
  (including version-mismatch responses); stop composers emitting
  `response.put("traceId", …)`; remove `Screen.traceId` from
  `sdui-schema.json`; run `make codegen`. Update
  `server-implementors-contract.md`, `client-implementors-contract.md`, and
  `sdui-envelope-spec.md`.
- [x] **A2a** Server: introduce `ResponseEnvelope<Screen>`/`<Section>` at the
  SDUI edge as a **hand-written transport wrapper (NOT in `sdui-schema.json`,
  NOT codegen)**. Ship Step 1 with a **static stub `meta`** (`degraded:false`,
  empty `staleSections` / `failedSections`). Use the field name `meta` and the
  record `ResponseEnvelope<T>(T data, ResponseMeta meta)` consistently
  throughout server code, docs, and the client wrappers.
- [x] **A2a (clients)** iOS — `SduiRepository.swift` + `RequestEnvelopeBuilder`:
  send `X-Correlation-ID`; **expose response correlation header on the fetch
  result** (additive return-type/struct change) so `SduiScreenViewModel`
  refreshes seed continuation traces from the header instead of `screen?.traceID`;
  unwrap `.data` in `fetchScreen` / `fetchSection` via a hand-written
  `{data, meta}` wrapper (decode-but-ignore `.meta`); regen `SduiModels.swift`
  via `make codegen` (Screen loses `traceId`); update
  `SduiScreenViewModelRefreshTests`.
- [x] **A2a (clients)** Android — `SduiRepository.kt` + `RequestEnvelopeBuilder`:
  send `X-Correlation-ID`; **expose response correlation header on the fetch
  result** so `SduiScreenController` (lines 232, 334, 443, 469, 525, 702) and
  `DataBindingResolver` log-tagging read correlation from the header instead of
  `currentScreen?.traceID` / `screen.traceID`; hand-written `.data` unwrap
  (ignore `.meta`); regen `SduiModels.kt`; update `SduiScreenControllerTest`.
- [x] **A2a (clients)** Web — `fetchSduiScreen.ts` + `RequestEnvelopeBuilder`:
  header rename only (no body-field reader to migrate); hand-written `.data`
  unwrap (ignore `.meta`); regen `SduiModels.ts`; update `fetchSduiScreen.test.ts`.
- [x] **A2a (clients)** Confirm **no renderer/adapter churn** — `.data` flows to
  renderers as before; `.meta` is parked in shared repository infra, unused until
  a later freshness milestone; section renderers stay untouched. Refresh fixtures
  across all three clients to drop `traceId` from `Screen` JSON.

### Phase A2b — SAF transport (server-only)

- [x] **A2b.1** Add `com.nba:service-aggregation-framework:1.0.0-SNAPSHOT` (Maven
  Local) + `publish-saf` / `sync-saf` Make targets (currently absent) + enable
  virtual threads + actuator exposure (`health,prometheus,metrics`).
- [x] **A2b.1** Delete hand-rolled trace-id plumbing from `SduiController`; adopt
  SAF `CorrelationIdFilter` + `ExecutionContext` (MDC under `correlationId`,
  echo on response, propagation into virtual threads and upstream calls via the
  SAF client interceptor).
- [x] **A2b.3** Replace OkHttp remote adapters with `RestClient` implementations
  behind `…domain.port` seams (preserve CDN/OPIM headers). Land the
  RestClient-only-in-`…remote` ArchUnit rule with this swap.
- [x] **A2b.4** Build SAF orchestration layer (`OrchestratorFactory.create(timeout)`
  + `ServiceCall` per upstream, `failOnError` true/false per criticality,
  per-upstream `saf.services.<name>.{resilience,cache,collapsing}` config,
  `STALE_IF_ERROR` per §8.1 policy declaration table). `StatsApiAdapter` now
  routes every upstream call (`scoreboard-cdn`, `core-api`, `boxscore-cdn`,
  `season-schedule`) through `OrchestratorFactory.create(5s).addService(
  ServiceCall…)` so per-service resilience, two-tier caching, and request
  collapsing wrap each fetch uniformly.
- [x] **A2b.4** Emit contract §10.1 metrics (`sdui.composition.duration`,
  `sdui.cache.hit/miss` by layer, `sdui.upstream.duration`,
  `sdui.version.mismatch`, `sdui.section.refresh`). New `SduiMetrics`
  `@Component` (in `…metrics`) centralises Micrometer registration.
  `SduiCompositionService` times every `composeX` call, `StatsApiAdapter`
  emits upstream timer + cache hit/miss counters from `ServiceMetadata`,
  `SduiController.checkVersionMismatch` increments the version-mismatch
  counter, and `SectionRefreshService.refreshSection` records section-refresh
  status. Server tests 166/166 green.
- [x] **A2b — runtime tokens (out-of-band Concern 1, landed alongside A2b)**
  Replaced compile-time `*Tokens.java` constant classes with a runtime
  `Tokens` `@Component` that loads `schema/*-tokens.json` at startup. Composers,
  `AtomicCompositeBuilder`, `SduiUtils`, and `SectionSurfaces` resolve every
  token reference via `tokens.spacing("lg")` / `tokens.color("…")` etc.;
  unknown names throw at composition time. Mechanical migration scripts retained
  at `scripts/migrate_tokens_to_runtime.py` + `scripts/migrate_tests_to_runtime_tokens.py`.
  This unblocks a future remote-updatable token registry without a schema
  change. Server tests 166/166 green.

### Phase A2c — real `meta` + section-fragment cache (server-only)

- [x] **A2c** Populate envelope Step 2: real `meta` from
  `orchestrator.buildMetadata()` — map per-call-id `ServiceCallInfo`
  (`status` FAILED/TIMEOUT → `failedSections`; `servedStale==true` →
  `staleSections`; `partialFailure` → `degraded`). A fresh in-TTL cache hit
  (`cacheSource` L1/L2 but not `servedStale`) is **not** degraded.
  Implemented as a `@RequestScope ResponseMetaCollector`
  (`server/src/main/java/com/nba/sdui/orchestration/ResponseMetaCollector.java`)
  that `StatsApiAdapter.consumeMetadata` pushes into after each
  `executeAll()`; `SduiController.envelope()` builds `ResponseMeta` from the
  collector's snapshot. Server tests 166/166 green.
- [x] **A2c** Section-fragment caching (contract §6) — unblocks
  `plan-server-section-caching.md`. Keys carry `sectionType`, `contentHash`,
  `deviceClass`, `schemaVersion`, `experimentBucket`; **must not** carry
  `theme` / `density` / `fontScale` / `formFactor` / `deviceId` / time-of-day.
  Pick and document one i18n stamp policy (before- vs after-cache, §6.3)
  uniformly across composers. Screen assembly stays uncached (§6.1).
  Implemented as a Caffeine-backed `SectionFragmentCache`
  (`server/src/main/java/com/nba/sdui/orchestration/SectionFragmentCache.java`)
  emitting `sdui.cache.hit/miss{layer=section, key=sectionType}`. Wired
  into the section channel (`SectionRefreshService`); per-composer
  integration deferred to `plan-server-section-caching.md` Phase 2.
  i18n policy: **stamp after cache** — composers stamp `stringTable` post
  section-build via `SduiUtils.stampStringTableOnSections`, so cached
  fragments are locale-neutral and `locale` is excluded from the key
  (documented in the cache class javadoc). Server tests 166/166 green.

### Phase A2d — WECS port seam (server-only)

- [ ] **A2d** Add `WatchExperiencePort` (`…domain.port`) over the existing
  `WatchExperienceResponse` contract + a `RestClient` WECS adapter or
  profiled stub; wire it as an optional SAF `ServiceCall`
  (`failOnError(false)`, cached `STALE_IF_ERROR`) and map it into a
  watch-oriented composer.
- [ ] **A2d** Pin the profiled stub with a contract test against the real
  `WatchExperienceResponse` DTO (decode round-trip + required-field coverage)
  so the stub cannot drift into a `null`-returner. **Defer A2d** if WECS still
  has no endpoint AND no concrete SDUI surface needs the port — avoid carrying
  real-but-unused code on `main`.

### Phase A3 — typed models

- [x] **A3** Wire generated `jsonschema2pojo` POJOs onto server compile classpath;
  remove dead `copyGeneratedModels` task. **Implemented** via
  `includeBuild("../codegen")` in `server/settings.gradle.kts` plus a
  source-set entry pointing at `codegen/build/generated-sources/jsonschema2pojo`
  and a `compileJava.dependsOn(":codegen:generateJsonSchema2Pojo")` wiring in
  `server/build.gradle.kts`. 74 generated classes
  (`com.nba.sdui.models.generated.*`: `Screen`, `Section`, `AtomicElement`,
  `RefreshPolicy`, `DataBinding`, `Navigation`, …) now compile into
  `server/build/classes/java/main/`. The dead `copyGeneratedModels` Copy task —
  which would have copied generated sources INTO `src/main/java`, exactly the
  anti-pattern AGENTS.md §1.2 forbids — is removed.
- [ ] **A3** Migrate element-factory + builder-using composers from `ObjectNode`
  to generated `Screen`/`Section`/`AtomicElement`, composer-by-composer behind
  round-trip tests. **Deferred** until either (a) `AtomicCompositeBuilder`
  (3434 LOC, 68 public `ObjectNode`-returning methods) gains dual-emit typed
  variants, or (b) we accept a one-shot builder flip. Composer-by-composer
  migration is gated on builder strategy.
- [ ] **A3** Migrate **`BoxscoreComposer`** (named — bypasses
  `AtomicCompositeBuilder`, builds raw `ObjectNode` directly). **Deferred**
  with the rest of the composer migration above.
- [ ] **A3** Migrate **`CalendarComposer`** (named — bypasses
  `AtomicCompositeBuilder`, builds raw `ObjectNode` directly). **Deferred**
  with the rest of the composer migration above.
- [x] **A3** Add schema-conformance test validating every composed screen/section
  against `schema/sdui-schema.json`. **Implemented** as
  `server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java`
  using `networknt/json-schema-validator` (Draft-07). Validates `Screen`
  scaffolding plus every `Section`'s outer fields (`id`, `type` enum,
  `surface`, `sectionStates`, `refreshPolicy`, `accessibility`, `stringTable`,
  …) for `LiveComposer.composeLive`, `DemoScreenComposer.composeLeaders`,
  and `DemoScreenComposer.composeDemos`. **Section.data discriminator now
  enforced**: the schema replaces the loose `Section.data` `oneOf` (which
  was ambiguous — multiple `*Data` variants validated the same payload)
  with `anyOf` (for codegen reachability) + an `allOf` chain of
  `if`/`then` clauses keyed off `Section.type` (real per-variant
  enforcement). See [docs/fixes/schema-section-data-discriminator.md](docs/fixes/schema-section-data-discriminator.md).
  Composers conformed on first run; client codegen diff was docstring-only.

### Phase B — port (future, not active scope)

- [ ] **B** (future) Port into `nba-client-backend` as `server:sdui-core`
  (controller/orchestration/domain/remote/intermodule), `integration-models`
  contracts, ArchUnit registration, boot-shell wiring.
- [ ] **B** (future) Add additive `WatchExperienceAccess` seam in
  `integration-models/api` + `InProcessWatchExperienceAccess` in `wecs-core`;
  swap `sdui-core`'s `WatchExperiencePort` to consume it via a
  `WatchExperienceAccessCallFactory` (reuse existing `contract.watch.*` DTOs).
  The factory translates WECS seam exceptions into SDUI's own
  `UpstreamServiceException` so SDUI never depends on the producer's
  exception types.
- [ ] **B** (future) Envelope already conformant (introduced in A2a) — register
  SDUI's `meta`/envelope against the shared `integration-models` contract and fold
  consumed-seam `meta` (WECS) into SDUI's own. No second wire change.
