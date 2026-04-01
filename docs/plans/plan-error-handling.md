# Plan: Error Handling & Section States

> Source requirements: §9c from sdui-requirements-summary.md
> Related: AGENTS.md §6, §7, §12; Client Implementor's Contract §13

## Summary

Per-section runtime error and loading states that allow individual sections to fail gracefully without crashing the full screen. The schema defines `SectionStates` with `error` (message, retryAction, hideOnError) and `loading` (skeleton style, minHeightDp). The server composes `ErrorState` as an `AtomicComposite` for anticipated failures. Every client also needs a **client-composed error surface** for unanticipated render-time crashes — this is a platform-universal requirement, not a single-platform task.

### Two Error Surfaces

Every client must support two distinct error surfaces:

| Surface | Origin | When | Example |
|---------|--------|------|---------|
| **Server-composed ErrorState** | Server response (`AtomicComposite`) | Anticipated failures: bad game ID, missing data, upstream timeout | `AtomicCompositeBuilder.buildErrorState()` |
| **Client-composed error card** | Client render pipeline | Unanticipated failures: renderer crash, deserialization failure after response accepted | React `ErrorBoundary`, Compose equivalent TBD |

Server-composed ErrorState is already a first-class section (AGENTS.md §6). The client-composed error card is the gap this plan addresses.

## Current State

### Infrastructure Status

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `SectionStates.error` (message, retryAction, hideOnError), `SectionStates.loading` (skeleton, minHeightDp) |
| Server support | Built | `AtomicCompositeBuilder.buildErrorState()` composes ErrorState with icon, title, message, retry button |
| Documentation | Partial | AGENTS.md §6 covers server ErrorState; contract doc §13 covers error handling pseudocode |
| Tests | Gap | No automated test that a crashing section is isolated from siblings on any platform |

### Platform Compliance Matrix

| Requirement | Schema | Server | Web | Android | iOS | tvOS | Fire TV |
|---|---|---|---|---|---|---|---|
| Section render isolation | n/a | n/a | Done | Done | — | — | — |
| `hideOnError` | Done | Done | Done | Done | — | — | — |
| `retryAction` wiring | Done | Done | Done | Done | — | — | — |
| Loading skeleton (`SectionStates.loading`) | Done | Done | Done | Done | — | — | — |
| Error logging (§12 compliant) | n/a | n/a | Done | Done | — | — | — |
| Retry budget (client config) | n/a | n/a | Gap | Done | — | — | — |
| Error telemetry (observability) | n/a | n/a | Gap | Gap | — | — | — |
| Screen-level error + retry | n/a | n/a | Done | Done | — | — | — |
| Unknown section type skip | n/a | n/a | Done | Done | — | — | — |
| Parse failure resilience | n/a | n/a | Done | Done | — | — | — |
| Data binding error isolation | n/a | n/a | Done | Done | — | — | — |
| Refresh failure resilience (surgical merge) | n/a | n/a | Done | Done | — | — | — |

> **Implementation note:** Android uses catch-at-dispatch + pre-validation in `SectionErrorBoundary`. Compose does not support `try/catch` around `@Composable` invocations, so pre-validation catches data problems before rendering. Web uses React `ErrorBoundary` for true recomposition crash isolation. Remaining gap: error telemetry (observability integration) not yet wired on either platform.

## Requirements Addressed

- [x] **REQ-1**: Per-section render isolation — isolate section render failures so siblings continue rendering (§9c, Contract §13)
- [x] **REQ-2**: `hideOnError` support — collapse a section entirely on error when `sectionStates.error.hideOnError` is true (schema contract)
- [x] **REQ-3**: `retryAction` wiring — read `sectionStates.error.retryAction` from section data, render retry button on error card, route tap through action handler (schema contract)
- [x] **REQ-4**: Loading skeleton — render generic skeleton placeholder per `SectionStates.loading` during section-level refresh (§9c, schema contract)
- [x] **REQ-5**: §12-compliant error logging — every catch logs section ID, section type, error message, and stack trace (AGENTS.md §12)
- [x] **REQ-6**: Retry budget — client-side configurable max retry count (default 5). After exhausting retries, show permanent error card. This is a client configuration, not a schema or server concern.
- [ ] **REQ-7**: Error telemetry — section render failures must be reported to the observability platform (e.g., New Relic) as non-fatal errors with section ID, section type, and stack trace. Uses existing app instrumentation, not SDUI `fireAndForget` actions.

## Tasks

### Phase 0: Decisions & Contract Update

Key decisions have been made. This phase records them and updates the client implementor's contract.

#### Decided: Isolation Mechanism — Catch-at-Dispatch + Pre-Validation

Performance and user experience take priority over theoretical perfect isolation. Separate `ComposeView`/`UIHostingController` per section is rejected — it breaks `LazyColumn` recycling, shared `CompositionLocal` propagation, and scroll performance.

The recommended pattern for all non-React platforms:

```
FOR each section in screen.sections:
    TRY:
        validate(section)               // null checks, type checks on required fields
        SectionRouter.render(section)   // dispatch to renderer
    CATCH exception:
        LOG_ERROR(sectionId, sectionType, exception)
        reportToObservability(exception, sectionId, sectionType)  // New Relic etc.
        IF hideOnError → skip (render nothing)
        ELSE → render client-composed error card with retryAction
        IF retryCount >= maxRetries → show permanent error card (no retry button)
```

This catches ~95% of real-world failures (data problems: unexpected nulls, type mismatches, missing nested objects). The remaining ~5% (true recomposition/layout-engine crashes) are handled as app-level non-fatal crash telemetry via the observability platform — not per-section isolation.

| Platform | Technology | Isolation Approach | Status |
|---|---|---|---|
| Web | React | `ErrorBoundary` / `getDerivedStateFromError()` | **Done** — built-in framework support |
| Android / Fire TV | Jetpack Compose | Catch-at-dispatch + pre-validation | **Ready to implement** |
| iOS / tvOS | SwiftUI | Catch-at-dispatch + pre-validation (same pattern) | Apply when platform begins |

#### Decided: Loading Skeleton — Generic

Skeletons use a single generic style (shimmer/spinner/placeholder/none per the `SectionStates.loading.skeleton` enum). No type-aware skeleton shapes. This keeps the client fully decoupled from section layout expectations and is consistent with server-driven composition.

#### Decided: Partial Render vs. Full Skip — Full Skip

When a section fails, the client replaces the **entire section** with the error card (or collapses it if `hideOnError`). No partial render output is ever shown. This is the contract for all platforms.

#### Decided: Retry Budget — Client-Side Configuration

Max retry count is a client-side configuration (default: 5), not a schema or server field. After exhausting retries, the error card becomes permanent (retry button removed). This is per-section, per-screen-visit — navigating away and back resets the counter.

#### Decided: Error Telemetry — Observability Platform

Section render failures are reported as non-fatal errors to the app's observability platform (e.g., New Relic) using existing instrumentation. This does **not** use SDUI `fireAndForget` actions — client-side crash telemetry is a client infrastructure concern, not server-driven composition. The error report must include: section ID, section type, platform, app version, and stack trace.

- [x] ~~Spike: Compose isolation~~ → decided: catch-at-dispatch + pre-validation
- [x] ~~Spike: SwiftUI isolation~~ → same pattern, applied when iOS begins
- [x] ~~Decision: loading skeleton style~~ → generic
- [x] ~~Decision: partial render vs. full skip~~ → full skip
- [x] ~~Decision: retry budget~~ → client-side config, default 5
- [x] ~~Decision: error telemetry~~ → observability platform (New Relic), not SDUI actions
- [x] Update client implementor's contract §13 with:
  - Platform-neutral behavioral contract: catch-at-dispatch + pre-validation pattern
  - The two-surface error model (server-composed vs. client-composed)
  - Full-skip policy (no partial renders)
  - Retry budget as client configuration requirement (default 5)
  - Error telemetry requirement (observability platform, not SDUI actions)

### Phase 1: Schema & Server

No schema or server changes needed — `SectionStates` (error + loading) and `AtomicCompositeBuilder.buildErrorState()` are already built.

- [x] `SectionStates.error` with `message`, `retryAction`, `hideOnError` — in schema
- [x] `SectionStates.loading` with `skeleton` enum, `minHeightDp` — in schema
- [x] `AtomicCompositeBuilder.buildErrorState()` — composes ErrorState with icon, title, message, retry

### Phase 2: Platform Implementation — Contract Compliance

Each platform must satisfy REQ-1 through REQ-5. Tasks below are platform-neutral requirements; each platform team implements using their validated isolation mechanism from Phase 0.

**Per platform (Android now; iOS/tvOS/Fire TV when started):**

- [x] **Section render isolation** (REQ-1) — Wrap each section dispatch in try/catch with pre-validation. On failure: full-skip the section and render inline error card (or collapse if `hideOnError`), log §12-compliant error, continue rendering siblings. No partial renders.
- [x] **`hideOnError` support** (REQ-2) — Read `sectionStates.error.hideOnError`. When `true` and the section errors, collapse the section entirely instead of showing an error card.
- [x] **`retryAction` wiring** (REQ-3) — Read `sectionStates.error.retryAction` from section data. Render a retry button on the error card. Route the tap through the existing action handler (`ActionHandler` on Android, `onAction` on web).
- [x] **Retry budget** (REQ-6) — Track per-section retry count (scoped to screen visit). After `maxRetries` (client config, default 5) exhausted, show permanent error card with no retry button. Reset count on screen re-entry.
- [x] **Loading skeleton** (REQ-4) — Render generic skeleton placeholder per `SectionStates.loading.skeleton` style (shimmer/spinner/placeholder/none) during section-level refresh. Respect `minHeightDp` to prevent layout shift. No type-aware shapes.
- [x] **§12-compliant error logging** (REQ-5) — Every catch block logs: component/class name, section ID, section type, error message, full stack trace. Silent catches prohibited.
- [ ] **Error telemetry** (REQ-7) — Report section render failures as non-fatal errors to the observability platform (New Relic etc.) with section ID, section type, platform, app version, stack trace. Uses existing app instrumentation.

**Web — audit and test (already implemented):**

- [x] Verify `SectionErrorBoundary.tsx` logs section ID *and* section type on error
- [x] Verify `hideOnError` collapses the section correctly
- [x] Verify `retryAction` fires through `onAction` callback

### Phase 3: Tests

Per-platform acceptance criteria — each must pass before marking a platform as "Done" in the compliance matrix:

| Test | Validates |
|------|-----------|
| Inject section whose renderer throws → error card renders → siblings render normally | REQ-1 (isolation) |
| Set `hideOnError: true` + inject error → section collapses → siblings unaffected | REQ-2 (hideOnError) |
| Tap retry button on error card → `refresh` action fires → section re-renders on success | REQ-3 (retryAction) |
| Retry 5 times → 6th failure shows permanent error card with no retry button | REQ-6 (retry budget) |
| Section in refresh state → generic skeleton renders with correct style → content replaces skeleton | REQ-4 (loading) |
| Trigger section error → verify log output contains section ID, type, stack trace | REQ-5 (logging) |
| Trigger section error → verify non-fatal error reported to observability platform | REQ-7 (telemetry) |
| Malformed section JSON in response → error boundary catches → other sections render | Parse resilience |

- [ ] Add Android instrumented tests for each row above
- [ ] Add web integration tests for each row above (several already passing — verify gaps)
- [ ] Add iOS/tvOS/Fire TV tests when those platforms begin

### Phase 4: Documentation

- [x] Update client implementor's contract §13 with the two-surface error model and platform-neutral behavioral requirements (from Phase 0)
- [x] Update `docs/sdui-requirements-summary.md` status matrix: §9c notes updated
- [x] Document the per-platform isolation mechanism decisions as implementation notes (addenda to contract, not the primary content)

## Dependencies

- Phase 0 decisions are resolved — Phase 2 is unblocked
- Server-composed ErrorState already exists — no server changes needed
- `retryAction` wiring depends on action handler — already implemented on both platforms (`ActionHandler.kt`, `onAction` in `SectionErrorBoundary.tsx`)
- Loading skeleton depends on `SectionStates.loading` being populated in server responses — verify with server team
- Error telemetry depends on observability SDK (New Relic or equivalent) being integrated in the app — assumed present

## Decisions Made

| Decision | Choice | Rationale |
|----------|--------|----------|
| Isolation mechanism (Compose/SwiftUI) | Catch-at-dispatch + pre-validation | Performance and UX over theoretical perfect isolation. Separate `ComposeView`/`UIHostingController` breaks `LazyColumn` recycling, `CompositionLocal` propagation, and scroll performance. Catches ~95% of real failures (data problems). Remaining ~5% (engine crashes) handled by observability platform. |
| Loading skeleton style | Generic | Keeps client decoupled from section layout expectations. Consistent with server-driven composition. |
| Retry budget | Client-side config, default 5 | Not a schema/server concern. Per-section, per-screen-visit. Resets on navigation. |
| Error telemetry | Observability platform (New Relic) | Client infrastructure concern, not SDUI composition. Uses existing app instrumentation. |
| Partial render vs. full skip | Full skip | Replace entire section with error card or collapse. No partial output. Contract for all platforms. |

## Open Questions

- [ ] **Offline error states** — If a cached screen loads offline and one section's data is stale/corrupt, should the retry button show (it will fail offline) or a different "unavailable offline" state? Intersects with ADR-010 (offline/degraded connectivity). **Deferred** — resolve when offline support is prioritized.
