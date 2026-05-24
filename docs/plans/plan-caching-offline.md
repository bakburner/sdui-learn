# Plan: Caching & Offline

> Source requirements: §9e, §9r from sdui-requirements-summary.md, ADR-010

## Summary

Implement stale-while-revalidate caching and offline-first fallback so SDUI screens load instantly on cold start and remain usable when the network is degraded or absent. This is a Milestone 1 blocker — staging readiness requires cached responses for the beachhead surface.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `RefreshPolicy` defines poll/SSE/static; no cache-specific schema |
| Server support | Built | Responses include `Cache-Control` headers |
| Android support | Gap | Room entities defined conceptually; no cache-then-network implementation |
| Web support | Gap | No IndexedDB or service worker caching |
| Documentation | Built | Client Implementor's Contract §12 defines cache strategy |
| Tests | Gap | No cache hit/miss/stale tests |

## Requirements Addressed

- [ ] **REQ-1**: Cache complete screen responses keyed by endpoint — §9e
- [ ] **REQ-2**: Serve stale data on network failure (stale-while-revalidate) — §9e
- [ ] **REQ-3**: Fire-and-forget local queue for analytics during offline — §9r, ADR-010
- [ ] **REQ-4**: Cold-start latency < 100ms from local cache — §9e
- [ ] **REQ-5**: Cache invalidation on successful fetch — §9e

## Tasks

### Phase 1: Schema & Codegen
- [ ] No schema changes needed — caching is a client-side concern
- [ ] Optional: add `cachePolicy` field to `RefreshPolicy` for server-controlled TTL hints

### Phase 2: Server
- [ ] Ensure all SDUI endpoints return appropriate `Cache-Control` headers
- [ ] Add `ETag` support for conditional requests (304 Not Modified)

### Phase 3: Android
- [ ] Implement `ScreenCacheDao` with Room — `android/sdui-core/src/.../cache/`
  - [ ] Entity: `CachedScreen(endpoint: String PK, payload: String, cachedAt: Long)`
  - [ ] DAO: `getByEndpoint()`, `insert()`, `deleteOlderThan()`
- [ ] Update `SduiRepository.fetchScreen()` with cache-then-network strategy:
  1. Check Room cache — return immediately if present
  2. Fetch from network in background
  3. On success: update cache + update UI
  4. On failure: keep stale data visible, log error
- [ ] Add `fireAndForget` queue for offline analytics events (store in Room, flush on reconnect)

### Phase 4: Web
- [ ] Implement IndexedDB cache wrapper — `web/src/runtime/ScreenCache.ts`
- [ ] Update `useSduiScreen` hook with cache-then-network strategy
- [ ] Add service worker for offline fallback (return cached SDUI responses)
- [ ] Add `fireAndForget` analytics queue (store in IndexedDB, flush on `navigator.onLine`)

### Phase 5: Documentation & Tests
- [ ] Update `docs/sdui-requirements-summary.md` status: §9e Gap → Partial/Built
- [ ] Add cache integration tests (cache hit, cache miss, stale serve, cache invalidation)
- [ ] Add offline simulation tests (airplane mode, network timeout)

## Dependencies

- ADR-010 (Offline & Degraded Connectivity) should be accepted before finalizing `fireAndForget` queue semantics
- ADR-004 (Transport & Caching) should clarify GET/POST governance and cache header strategy

## Open Questions

- [ ] Should cache TTL be server-controlled (via response header or schema field) or client-controlled?
- [ ] Should stale cache entries ever be explicitly evicted, or only overwritten on successful fetch?
- [ ] What is the maximum cache size budget per platform?
- [ ] Should the `fireAndForget` queue have a max depth or age limit?
