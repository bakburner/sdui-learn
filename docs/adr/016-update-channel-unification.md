# ADR-016: Update Channel Unification

- Status: Accepted
- Date: 2026-05-26
- Decision owners: Adrian Robinson (interim), platform leads
- Related requirements: `docs/sdui-requirements-summary.md` §9o (URL namespace), `AGENTS.md` §3.8
- Related ADRs: ADR-003 (Composition API contract), ADR-004 (Transport and caching policy)

## Decision

Collapse the `/v1/sdui/screen/refresh/{handlerId}` URL family into the existing `/v1/sdui/screen/{id}` family. Codify two explicit update channels — screen channel and section channel — with no partial-screen response shape, strict same-id full-replace on the screen channel, replace-by-id on the section channel, current-params replay, and poll-timer reset on successful fetch. The legacy URL is hard-cut with no deprecation window.

## Context

In May 2026, the games-screen CalendarStrip date picker produced stale sections after a date change. Post-mortem root cause:

1. **Dual URL families.** The server exposed `/v1/sdui/screen/{id}` (initial loads) and `/v1/sdui/screen/refresh/{id}` (parameterized re-composition). Both returned identical full `Screen` responses — the split was naming-only, with no behavioral difference on the server.

2. **Two client code paths.** The URL split seeded two client methods: `loadScreen` (initial) and `refreshSections` (parameterized). The latter inherited a merge-and-preserve semantic originally intended for partial form-fragment merging. When the CalendarStrip dispatched a `refresh` action, it routed through the merge path, which preserved sections from the previous date's response alongside the new date's sections — a stale-data bug.

3. **Misleading method names.** `refreshSections` implied per-section granularity; in reality it replaced the entire screen. The name concealed the actual contract and encouraged the wrong mental model.

The tactical fix (strict same-id full-replace gated on `response.id` match) landed on Android immediately and prevented the stale-section bug from recurring. However, the redundant URL family remained, and iOS still carried the legacy merge path. This ADR formalizes the removal of the footgun.

## Decision Drivers

- The dual URL family created a contract surface that implied behavioral differentiation where none existed on the server.
- Client code paths bifurcated around the URL distinction, each accumulating subtly different semantics over time.
- The merge path on the "refresh" code path was a latent contract violation on iOS (and potentially web) waiting to surface on the next parameterized screen.
- Server contract tests cannot reliably prevent URL-family reintroduction without a codified architectural rule.

## Options Considered

### Option A: Keep the split, fix client-side merge logic only

Fix each client's `refreshSections` to do strict same-id full-replace (as already done on Android). Leave both URL families alive.

Pros:
- Minimal server change.
- No coordination across clients for URL migration.

Cons:
- Contract redundancy remains — two URLs with identical behavior invite future divergence.
- The footgun persists: a new client or a new developer on an existing client encounters both URLs and reasonably assumes they differ.
- Server contract tests must guard two families forever for no benefit.

### Option B: Introduce a delta-envelope wire shape for partial updates

Create a third response shape that carries only changed sections (insertions, removals, patches) as a diff against the current screen.

Pros:
- Reduces wire payload for screens with many sections where only one changed.
- Enables fine-grained structural updates without full-screen re-composition.

Cons:
- Doubles the response-shape contract (full screen + delta envelope) with all the decode/apply/test surface that entails.
- `dataBinding` already covers narrow live-data deltas at the field level; the section channel covers single-section structural updates. The gap this fills (multi-section structural diff) has no current product driver.
- Significantly increases client complexity (diff application, conflict resolution, ordering guarantees).

### Option C: Add `replaceScreen` / `refreshSection` action types (rejected)

Disambiguate channel via the action `type` field rather than the URL shape.

Pros:
- Makes the channel explicit at the action declaration site.

Cons:
- The URL shape already disambiguates unambiguously (`/screen/{id}` vs `/section/{id}`).
- Adding action types fragments the variant discipline rule (`AGENTS.md` §11) — two new enum values with no behavioral difference from the existing `refresh` type that already carries `endpoint`.
- The server would still serve both channels from the same two URL families; the action type would be redundant information.

## Evidence

- **Server:** `LiveComposer` and `DemoScreenComposer` both returned full `Screen` responses on both URL families. Zero behavioral distinction on the server side.
- **Android:** `SduiScreenController.refreshSections` already enforced strict same-id full-replace after the May 2026 fix. The method name was the only remaining artifact of the legacy contract.
- **iOS:** `SduiScreenViewModel.mergeRefreshedScreen` still performed surgical section swap when a `targetSectionID` was supplied — the merge path that caused the original bug. This was a latent contract violation awaiting a trigger.
- **Server contract tests:** `ScreenChannelContractTest`, `SectionChannelContractTest`, `ComposerEndpointRegressionGuardTest`, `ParameterizedRefreshRoutingTest`, `ComposerRoundTripTest`, and `MutateThenRefreshAuditTest` now enforce the unified architecture (30 tests passing).

## Decision Outcome

Option C from the plan (unify onto one screen-channel URL family + one section-channel URL family). Hard-cut the legacy URL; no deprecation window.

## Consequences

- Short term:
  - Client API rename churn (`refreshSections` → `replaceCurrentScreen`) across Android, iOS, and web. Mechanical, non-behavioral.
  - No migration window — the legacy `/v1/sdui/screen/refresh/{id}` route is removed in the same change that adds query-param support to `/v1/sdui/screen/{id}`. All clients ship together.
- Medium term:
  - One channel per grain (screen vs. section) becomes a codified rule in `AGENTS.md` §3.8, enforced by server contract tests. Future endpoints cannot accidentally recreate the dual-family footgun.
  - The latent iOS merge-path contract bug is resolved by removing the merge code path entirely.
- Long term:
  - The server contract test suite (`ScreenChannelContractTest`, `SectionChannelContractTest`, `ComposerEndpointRegressionGuardTest`) acts as the durable regression fence. Any composer that emits a `/screen/refresh/` URL or any controller that re-introduces the route will fail CI.
  - The two-channel model is stable and extensible. If a future product need requires a structural-diff envelope, it can be evaluated against the existing clean baseline rather than against a legacy bifurcated architecture.

## Implementation Notes

- **Rollout:** Coordinated across server + three clients. Server contract tests land first (regression fence), then composer endpoint strings change, then the route is removed, then clients pick up the new endpoints in their existing fetch paths. Merge order prevents a window where any platform hits a dead URL.
- **Compatibility strategy:** Hard cut. No alias, no warning period. The prototype ships all platforms together; no external consumers of the legacy URL exist.
- **Ownership and governance:**
  - Screen-channel URL shape: server `SduiController` + `ParameterizedRefreshService`.
  - Section-channel URL shape: server `SectionRefreshService`.
  - Client screen-channel method: `replaceCurrentScreen` on each platform's ViewModel/Store.
  - Client section-channel method: `replaceSection` (or platform equivalent) on each platform.
  - Regression fence: `server/src/test/java/com/nba/sdui/contract/` test suite.

## Follow-ups

- [x] Server: remove `/v1/sdui/screen/refresh/{id}` route
- [x] Server: composers emit unified `/v1/sdui/screen/{id}` endpoints
- [x] Server: contract test suite established (6 test classes, 30 tests)
- [x] Android: rename `refreshSections` → `replaceCurrentScreen`
- [x] iOS: replace `mergeRefreshedScreen` with strict full-replace
- [x] Web: rename `refreshSections` → `replaceCurrentScreen`
- [x] Documentation: `AGENTS.md` §3.8, client contract, games-screen spec, envelope spec updated
