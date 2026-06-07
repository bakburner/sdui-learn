# Plan: Port SDUI Server → `nba-client-backend` as `server:sdui-core`

**Status:** Proposed — gated on external readiness (see Trigger conditions).
**Scope:** Lift the standalone SDUI service (`sdui-prototype/server`) into the
`nba-client-backend` modular monolith as a new composition module, plus an
additive WECS seam that satisfies the deferred WECS integration.
**Predecessor:** `archive/plan-server-saf-codegen-port-readiness.md` (phases A1,
A2a, A2b, A2c, A3 complete; A2d intentionally deferred and folded into this
plan's W1).

**Authoritative spec:** `docs/contracts/server-implementors-contract.md` §11
conformance checklist (still applies — port preserves conformance).

---

## Why this plan exists

The predecessor plan (`plan-server-saf-codegen-port-readiness.md`) reorganized
`sdui-prototype/server` into the shell's composition-module shape, adopted SAF
for upstream fetching, threaded a real `ResponseEnvelope<T>{data, meta}` through
to all three clients, and migrated the entire composition pipeline to typed
generated POJOs. The server was deliberately *built* port-ready in this repo
first so the port itself becomes lift-and-shift.

Two strands were left open at predecessor close-out:

1. **W1 (was A2d) — WECS port seam.** Held until either WECS exposes an
   endpoint *or* a concrete SDUI surface (e.g. game-detail watch CTAs) demands
   it. Without one of those, the port + stub would be dead code on `main`.
2. **W2 (was Phase B) — actual port into `nba-client-backend` as
   `server:sdui-core`.** Held until the shell is ready to receive the module.

This plan is the path through both. The work is small, well-defined, and
load-bearing for the modular-monolith story — but it is not active until the
trigger conditions below are met. Open this plan, don't start it, until then.

## Trigger conditions

| Strand | Start when … |
|---|---|
| **W1 (WECS port seam, standalone)** | WECS exposes a `WatchExperienceResponse` endpoint OR a concrete SDUI surface (game-detail watch CTAs, stream selection, resume, upsell, blackout messaging) needs the data the seam would carry. |
| **W2 (port into `nba-client-backend`)** | `nba-client-backend` is ready to receive a new composition module: `ModuleConformanceArchTest` accepts new registrations, `integration-models` accepts additive types, boot shell accepts new `@Profile` bindings. |

W1 and W2 are independent. W1 may ship first (standalone service uses the port,
shell-side seam follows later) or W2 may ship first (port the module without
WECS wiring, add the seam in a follow-up). Doing both at once is also fine —
W1's `WatchExperiencePort` was deliberately designed as the swap point for the
W2 `WatchExperienceAccess` seam, so the migration cost is the same either way.

## What is NOT in scope

- **No further code changes in `sdui-prototype/server`** beyond W1. The
  predecessor plan brought the standalone service to its target shape;
  anything else is a separate plan.
- **No schema changes.** `ResponseEnvelope<T>` stays a transport wrapper
  outside the schema (ADR-017); the W2 port re-registers it against the
  shared `integration-models` type — no second wire change.
- **No client work.** The three clients consume `.data` and decode-but-ignore
  `.meta` exactly as they do today; the port is invisible at the wire.

---

## W1 — `WatchExperiencePort` (standalone, in `sdui-prototype/server`)

Add a domain port for the watch experience and wire it through SAF, so a
watch-oriented composer can source CTAs / stream selection / resume / upsell /
blackout messaging from a single seam instead of recomputing them. The port
becomes the swap point for W2's in-process seam.

### Surfaces

| Surface | Location | Shape |
|---|---|---|
| Port interface | `server/src/main/java/com/nba/sdui/domain/port/WatchExperiencePort.java` | `WatchExperienceResponse fetch(WatchExperienceRequest)` |
| HTTP adapter | `server/src/main/java/com/nba/sdui/remote/RestClientWatchExperienceAdapter.java` | `RestClient`-based; `@Profile("!stub")` |
| Profiled stub | `server/src/main/java/com/nba/sdui/remote/StubWatchExperienceAdapter.java` | `@Profile("stub")`; deterministic shape |
| SAF wiring | `…orchestration` | `ServiceCall` (`saf.services.watch-experience.*`, `failOnError(false)`, `STALE_IF_ERROR`) |
| Composer integration | Whichever composer demands it (likely `GameDetailComposer`) | Maps `WatchExperienceResponse` → typed `Section`s |
| Stub contract test | `server/src/test/java/com/nba/sdui/remote/StubWatchExperienceContractTest.java` | Decode round-trip + required-field coverage against the real DTO |

### Done when

- `WatchExperiencePort` exists, has a real adapter, has a stub adapter, has a
  contract test that pins the stub to the real DTO shape.
- The port is wired as an **optional** SAF `ServiceCall` (`failOnError(false)`,
  cached `STALE_IF_ERROR`) — degraded watch data must not 500 the screen.
- At least one composer maps the response into typed `Section`s; if a surface
  triggered W1, that surface composes through the port.
- ArchUnit rules already in place (no Spring in `…domain`, RestClient only in
  `…remote`) stay green.
- Server tests stay 167/0/0 plus the new stub contract test.
- `sdui.upstream.duration{service=watch-experience}` is observable on
  `/actuator/prometheus`.

### Risks

- **Stub rot.** Mitigated by the required contract test against
  `WatchExperienceResponse`. A stub that drifts into a `null`-returner fails
  the test.
- **`platform` becomes load-bearing.** If the composer surfaces are
  `platform`-sensitive (stream selection by `ios`/`android`/`web`), the
  envelope's `platform[deviceClass]` already participates in the cache key
  per contract §6.2. Confirm the composer keys correctly; do not silently
  drop the field.

---

## W2 — Port into `nba-client-backend` as `server:sdui-core`

Lift `sdui-prototype/server` into the shell as a composition module, alongside
`wecs-core` and `cas-core`. Because the standalone service was built to the
shell's vocabulary (`controller` / `orchestration` / `domain` / `remote` /
`intermodule`), this is a structural lift — not a rewrite.

### Surfaces to add in `nba-client-backend`

| Surface | Location | Purpose |
|---|---|---|
| Module | `server/sdui-core/` | New library module (no `bootJar`/`main`) |
| Module package root | `com.nba.client.backend.sdui` | Mirrors `wecs-core` package root convention |
| Controller | `…sdui.controller.SduiController` | HTTP edge, returns `ResponseEnvelope<Screen>` |
| Orchestration | `…sdui.orchestration.*` | SAF-only |
| Domain (pure) | `…sdui.domain.*` | Composers; no Spring, no SAF, no clients |
| Remote adapters | `…sdui.remote.*` | `RestClient` via `integration-clients` |
| Intermodule (consume WECS) | `…sdui.intermodule.WatchExperienceAccessCallFactory` | Wraps WECS seam in a SAF `ServiceCall` |
| Architecture registration | `nba-client-backend/server/nba-client-backend-server/src/test/java/com/nba/client/backend/architecture/ModuleConformanceArchTest.java` | Register `sdui-core` |
| Architecture boundaries | same file + `ModuleBoundaryArchTest.java` | Module isolation + domain purity |
| Boot shell wiring | `server/nba-client-backend-server/` | Module binding, `@Profile` bindings, actuator groups |

### Surfaces to add in `integration-models` (additive only)

| Type | Purpose |
|---|---|
| `com.nba.integration.seam.WatchExperienceAccess` | New seam interface SDUI consumes |
| (Reuse) `com.nba.integration.contract.watch.WatchExperienceResponse` and friends | Already exists; no contract changes |

### Surfaces to add in `server:wecs-core`

| Type | Purpose |
|---|---|
| `…wecs.intermodule.InProcessWatchExperienceAccess` | Implements the new seam by wrapping the existing `WatchExperienceOrchestrator` |

### Surfaces that move out of `sdui-prototype/server`

Everything currently under `com.nba.sdui.*` moves into `com.nba.client.backend.sdui.*`
with the same package vocabulary. The only structural changes are:

- `WebConfig` CORS — **does not travel.** Shell owns CORS via
  `nba.web.cors.*` (ADR-004); `sdui-core` keeps its controllers, the shell
  applies the global policy.
- `application.yml` — global keys (server port, actuator, logging) move to
  the shell; SDUI-specific keys (`saf.services.scoreboard-cdn.*`, etc.)
  travel with the module.
- The standalone `WatchExperiencePort` (from W1) becomes the consumer of the
  in-process `WatchExperienceAccess` seam: the HTTP adapter is replaced by
  the `WatchExperienceAccessCallFactory` under `@Profile("in-process")`; the
  remote `RestClient` adapter survives under `@Profile("remote")` for the
  post-WECS-extraction future.

### Done when

- `nba-client-backend` `./gradlew :server:sdui-core:test` is green.
- `ModuleConformanceArchTest` registers `sdui-core` and passes.
- `ModuleBoundaryArchTest` enforces SDUI's domain purity (no Spring, no
  SAF, no sibling-module references in `…domain`).
- `nba-client-backend` `./gradlew :server:nba-client-backend-server:bootRun`
  starts with the SDUI controllers mounted and the WECS seam bound to
  `InProcessWatchExperienceAccess`.
- Three-client smoke against `nba-client-backend-server` shows identical
  `ResponseEnvelope<Screen>` shape (`.data` + `.meta`) to the standalone
  service — no second wire change for clients.
- `WatchExperienceAccessCallFactory` translates seam exceptions into SDUI's
  own `UpstreamServiceException` so SDUI never depends on the producer's
  exception types (module-authoring guide §2 step 3).
- `sdui-prototype/server` is either retired or kept as a sandbox; the
  authoritative SDUI server is now `nba-client-backend` `:server:sdui-core`.

### Risks

- **Drift between standalone and shell-bound versions** during the port
  window. Mitigated by doing W2 as one PR train, not a long-running
  branch.
- **Boot-shell config conflicts** with cas-core / wecs-core (port collisions,
  actuator-group collisions, `saf.services.*` key collisions). Mitigated
  by reading `wecs-core`'s `application.yml` first and naming SDUI's
  services with non-overlapping prefixes (`saf.services.scoreboard-cdn.*`
  vs WECS's `saf.services.mediakind.*`).
- **`WatchExperienceAccess` seam shape** has to satisfy both the standalone
  HTTP adapter (already exists) AND in-process WECS consumption. Mitigated
  by reusing the existing `WatchExperienceResponse` DTO unchanged — the
  seam interface is the only additive surface.
- **Extraction-readiness regression.** If the port introduces a SAF type
  on a domain contract, or `sdui-core` accidentally depends on
  `wecs-core` (vs the seam), ArchUnit fails the build. This is the
  intended safety net.

---

## Open questions

- **Does `sdui-prototype/server` stay alive after W2?** Two reasonable
  answers: (a) retire it (single source of truth); (b) keep it as a
  sandbox for schema experimentation that doesn't yet belong in the
  shell. Decide at W2-start.
- **Does the prototype `Makefile` retain `bootRun` targets after W2?**
  Probably yes for client-only iteration against a fixture server, but
  document the shift so contributors know `nba-client-backend` is the
  authoritative deployable.

## Out of scope

- Any further composition / schema / token work in this repo. The
  predecessor plan closed those.
- Carving `sdui-core` back out into its own deployable. The whole point
  of extraction-readiness is that the module *can* be carved later; this
  plan does not carve it.
- Changes to the SDUI wire contract. Clients don't move.

## Todo checklist

### W1 — `WatchExperiencePort` (standalone, in this repo)

- [ ] **W1** Add `WatchExperiencePort` interface in `…domain.port`.
- [ ] **W1** Add `RestClientWatchExperienceAdapter` in `…remote`
  (`@Profile("!stub")`).
- [ ] **W1** Add `StubWatchExperienceAdapter` in `…remote` (`@Profile("stub")`)
  with a deterministic response shape.
- [ ] **W1** Wire the port as an optional SAF `ServiceCall`
  (`saf.services.watch-experience.{resilience,cache,collapsing}`,
  `failOnError(false)`, `STALE_IF_ERROR`).
- [ ] **W1** Map `WatchExperienceResponse` into typed `Section`s in whichever
  composer triggered W1.
- [ ] **W1** Add `StubWatchExperienceContractTest` pinning the stub to the
  real `WatchExperienceResponse` DTO (decode round-trip + required-field
  coverage).
- [ ] **W1** Verify `sdui.upstream.duration{service=watch-experience}` is
  observable on `/actuator/prometheus`; tests stay 167+/0/0.

### W2 — Port into `nba-client-backend`

- [ ] **W2** Create `server/sdui-core/` module in `nba-client-backend`
  with the standard package skeleton
  (`com.nba.client.backend.sdui.{controller,orchestration,domain,remote,intermodule}`).
- [ ] **W2** Lift controllers / orchestration / domain composers / remote
  adapters from `sdui-prototype/server` into the new module, preserving the
  typed `Screen` pipeline and `ResponseEnvelope<Screen>` edge shape.
- [ ] **W2** Register `sdui-core` in `ModuleConformanceArchTest`; ensure
  `ModuleBoundaryArchTest` enforces SDUI's domain purity.
- [ ] **W2** Add the additive `WatchExperienceAccess` seam in
  `integration-models/api`; reuse existing `contract.watch.*` DTOs unchanged.
- [ ] **W2** Add `InProcessWatchExperienceAccess` in `wecs-core.intermodule`
  wrapping the existing `WatchExperienceOrchestrator`.
- [ ] **W2** Add `WatchExperienceAccessCallFactory` in `sdui-core.intermodule`
  that wraps the seam in a SAF `ServiceCall` and translates seam exceptions
  into SDUI's `UpstreamServiceException`.
- [ ] **W2** Swap `sdui-core`'s `WatchExperiencePort` binding: shell binds it
  to the in-process factory under `@Profile("in-process")`; the existing
  `RestClient` adapter stays available under `@Profile("remote")` for the
  post-WECS-extraction future.
- [ ] **W2** Migrate SDUI-specific config (`saf.services.*`) from
  `sdui-prototype/server/application.yml` into `sdui-core`'s module config;
  CORS stays in the shell.
- [ ] **W2** Boot `nba-client-backend-server` with the new module; three-client
  smoke against the shell shows identical `ResponseEnvelope<Screen>` to the
  standalone service.
- [ ] **W2** Decide and document the fate of `sdui-prototype/server`
  (retire vs sandbox) and update its `Makefile` accordingly.
