# Plan: Lifecycle, Animation & Pagination

> Source requirements: §9d, §9h, §9k from sdui-requirements-summary.md
> **Cross-reference:** Visibility-gated refresh (the §9d runtime portion) is now built.
> See [`plan-visibility-gated-refresh.md`](plan-visibility-gated-refresh.md) for the
> poll/SSE pause-on-off-screen implementation. What remains here for §9d is the
> **eager/lazy initial-load trigger** — deferring the first data fetch until
> a section enters the viewport.

## Summary

Three related gaps that govern how sections appear, update, and extend over time: lazy loading (viewport-driven channel management), animation hints (transition effects on data change), and pagination (cursor-based load-more for long lists).

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Partial | `pauseWhenOffScreen` built (see visibility-gated-refresh plan). No lazy-load threshold, animation hint, or pagination cursor fields yet. |
| Server support | Partial | Ongoing refresh visibility-gated. No lazy initial-load; no pagination cursor in responses. |
| Android support | Partial | Poll/SSE visibility gating built. No lazy initial-load or pagination. |
| Web support | Partial | Poll/SSE visibility gating built. No lazy initial-load or pagination. |
| Documentation | Partial | Contract doc §8a covers visibility-gated refresh. Lazy/animation/pagination not yet documented. |
| Tests | Partial | Visibility-gated refresh tests written. No lifecycle or pagination tests. |

## Requirements Addressed

- [x] **REQ-1a**: Visibility-gated refresh (pause poll/SSE for off-screen sections) — §9d — see `plan-visibility-gated-refresh.md`
- [ ] **REQ-1b**: Lazy initial-load (defer first fetch until section enters viewport) — §9d
- [ ] **REQ-2**: Section-level animation hints (entry, exit, data-change transitions) — §9h
- [ ] **REQ-3**: Cursor-based pagination contract (nextCursor, hasMore) — §9k
- [ ] **REQ-4**: Load-more trigger pattern (scroll threshold or explicit button) — §9k

## Tasks

### Phase 1: Schema & Codegen
- [ ] Add `lazyLoad: boolean` to `RefreshPolicy` (defer channel open until section enters viewport)
- [ ] Add `AnimationHint` to `Section`: `{ entryEffect, exitEffect, dataChangeEffect }`
- [ ] Add `PaginationPolicy` to `Section`: `{ nextCursor, hasMore, loadMoreAction }`
- [ ] Run codegen

### Phase 2: Server
- [ ] Mark long-scroll sections with `lazyLoad: true` in refresh policy
- [ ] Add animation hints to GamePanel (score pulse), ContentRail (fade-in)
- [ ] Implement cursor-based pagination for play-by-play, stats tables

### Phase 3: Android
- [ ] Implement viewport observer for lazy load — connect/disconnect channels based on visibility
- [ ] Implement Compose animation for `entryEffect` (fade, slide) and `dataChangeEffect` (pulse)
- [ ] Implement load-more trigger in `LazyColumn` (detect end of list → fire `loadMoreAction`)

### Phase 4: Web
- [ ] Implement IntersectionObserver for lazy load — connect/disconnect channels based on visibility
- [ ] Implement CSS transitions for animation hints
- [ ] Implement scroll sentinel for load-more trigger

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract with lazy load, animation, pagination algorithms
- [ ] Add lifecycle tests (section enters viewport → channel opens; exits → channel closes)
- [ ] Add pagination tests (load page 1, scroll to trigger, load page 2)

## Dependencies

- Lazy loading depends on the RefreshOrchestrator and RealTimeManager being channel-aware (Phase 3 in contract)
- Pagination depends on server cursor implementation

## Open Questions

- [ ] Should lazy loading use a pixel-based threshold or a "next N sections" lookahead?
- [ ] Should animation hints include duration and easing, or just effect type?
- [ ] Should pagination be section-level (paginate within a section) or screen-level (paginate the section list)?
- [ ] Who owns the cursor — server (opaque token) or client (offset)?
