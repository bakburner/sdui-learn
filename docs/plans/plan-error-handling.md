# Plan: Error Handling & Section States

> Source requirements: §9c from sdui-requirements-summary.md

## Summary

Per-section runtime error and loading states that allow individual sections to fail gracefully without crashing the full screen. The web client has `SectionErrorBoundary` and `SectionSkeleton`; Android needs equivalent wiring. The server already composes `ErrorState` as an `AtomicComposite`, but clients also need local fallback for render-time crashes.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Partial | `ErrorState` exists as server-composed AtomicComposite |
| Server support | Built | `AtomicCompositeBuilder.buildErrorState()` handles bad game ID, missing data |
| Android support | Gap | No per-section error boundary; a section crash can break the LazyColumn |
| Web support | Built | `SectionErrorBoundary` (React ErrorBoundary per section), `SectionSkeleton` for loading |
| Documentation | Partial | AGENTS.md §6 covers server ErrorState; contract doc §8 covers error handling |
| Tests | Gap | No test that a crashing section is isolated from siblings |

## Requirements Addressed

- [ ] **REQ-1**: Per-section error boundary on Android (isolate render failures) — §9c
- [ ] **REQ-2**: Per-section loading skeleton on Android — §9c
- [ ] **REQ-3**: Retry action on section-level errors — §9c
- [ ] **REQ-4**: Validate web error/loading states match server contract — §9c

## Tasks

### Phase 1: Android Error Boundary
- [ ] Create `SectionErrorBoundary` composable that wraps each section with `runCatching`
- [ ] On catch: render inline error card with section ID, error summary, retry button
- [ ] Log error with section type, section ID, and stack trace (per AGENTS.md §12)

### Phase 2: Android Loading Skeleton
- [ ] Create `SectionSkeleton` composable that renders shimmer placeholder per section type
- [ ] Wire skeleton display during initial fetch and section-level refresh

### Phase 3: Retry Infrastructure
- [ ] Add `retryAction` field to server-composed ErrorState sections
- [ ] Wire retry button to ActionDispatcher (fire `refresh` action targeting the failed section)
- [ ] Implement retry with exponential backoff for network errors

### Phase 4: Web Validation
- [ ] Audit `SectionErrorBoundary.tsx` — ensure it logs section ID and type
- [ ] Audit `SectionSkeleton.tsx` — ensure it handles all section types
- [ ] Add integration test: inject crashing section → verify sibling renders

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract §8 with Android-specific error boundary pattern
- [ ] Add Android instrumented test: malformed section JSON → error boundary renders
- [ ] Add web test: crashing section → error boundary catches, siblings unaffected

## Dependencies

- Server-composed ErrorState already exists (no server changes needed for Phase 1-2)
- Retry action depends on ActionDispatcher (already implemented on both platforms)

## Open Questions

- [ ] Should the error boundary show a generic card or attempt to render the server-composed ErrorState?
- [ ] Should loading skeletons be section-type-aware (different shapes) or generic shimmer?
- [ ] How many retries before giving up and showing a permanent error card?
