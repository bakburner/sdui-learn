# Plan: Impression Tracking & Analytics

> Source requirements: §9i from sdui-requirements-summary.md, ADR-009 (Accepted)

## Summary

Implement visibility-based impression firing with configurable deduplication across all platforms. Web implementation is complete; Android and iOS need the same IntersectionObserver-equivalent behavior using platform viewport APIs.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `ImpressionPolicy` with dedup, visibilityThreshold, dwellMs |
| Server support | Built | Composers attach impression actions to sections |
| Android support | Gap | No viewport visibility tracking or dedup registry |
| Web support | Built | `useImpressionTracking` hook with IntersectionObserver + dedup |
| Documentation | Built | Client Implementor's Contract §9 covers algorithm |
| Tests | Gap | No automated impression-fire tests |

## Requirements Addressed

- [x] **REQ-1**: Schema `ImpressionPolicy` with dedup strategies — §9i, ADR-009
- [x] **REQ-2**: Web impression tracking (IntersectionObserver, dwell timer, dedup) — §9i
- [ ] **REQ-3**: Android impression tracking (Compose visibility, dwell timer, dedup) — §9i
- [ ] **REQ-4**: Dedup strategies: once-per-screen, once-per-session, once-per-interval, none — ADR-009

## Tasks

### Phase 1: Schema & Codegen
- [x] `ImpressionPolicy` already in schema — no changes needed

### Phase 2: Server
- [x] Impression actions attached to sections — no changes needed

### Phase 3: Android
- [ ] Implement `ImpressionTracker` — `android/sdui-core/src/.../state/ImpressionTracker.kt`
  - [ ] Use Compose `onGloballyPositioned` + `LocalView` to detect viewport intersection
  - [ ] Implement dwell timer (default 1000ms)
  - [ ] Implement dedup registry (in-memory map keyed by `sectionId:eventName`)
  - [ ] Support all 4 dedup strategies
- [ ] Wire `ImpressionTracker` into `SduiScreenContent` for all rendered sections
- [ ] Fire `fireAndForget` actions through existing `ActionHandler`

### Phase 4: Web
- [x] `useImpressionTracking` hook complete — no changes needed
- [ ] Add impression tracking tests (mock IntersectionObserver, verify dedup)

### Phase 5: Documentation & Tests
- [ ] Update `docs/sdui-requirements-summary.md` status: §9i Partial → Built
- [ ] Add impression tracking unit tests on Android
- [ ] Add integration test: scroll through screen → verify correct impressions fired with dedup

## Dependencies

- ADR-009 is accepted — no blockers

## Open Questions

- [ ] Should the dedup registry be scoped to ViewModel lifecycle (cleared on screen exit) or app lifecycle?
- [ ] How should impression tracking interact with TabGroup — fire for tabs that are rendered but off-screen?
