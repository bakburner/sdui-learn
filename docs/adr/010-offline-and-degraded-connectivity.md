# ADR-010: Offline and Degraded Connectivity Strategy

- Status: Proposed
- Date: 2026-03-13
- Decision owners: Adrian Robinson (interim), platform leads, backend leads
- Related requirements: `docs/sdui-requirements-summary.md`
- Related ADRs: ADR-004 (Transport and Caching Policy)

## Decision

Adopt **Option B (Stale-While-Offline with Platform Cache)** — serve the last-known-good SDUI response from platform HTTP cache or a lightweight disk snapshot when the network is unavailable, and surface a non-blocking connectivity indicator. Do not build a dedicated offline database, content-sync pipeline, or offline-first architecture at this stage.

## Context

SDUI is inherently server-driven: the composition service produces the UI tree, and clients render it. When the network is unavailable, there is no server to drive the UI. Today, a cold launch with no connectivity shows a blank screen or a system error — neither is acceptable for a production app.

The challenge is proportionality. An NBA app's content (scores, schedules, video) is time-sensitive and loses value quickly when stale. Investing heavily in offline-first infrastructure (local DB, sync engine, conflict resolution) is high-cost relative to the value stale sports content provides. At the same time, showing *something* — even slightly stale — is meaningfully better than showing nothing.

### Current Behavior

- **Android**: OkHttp's HTTP cache respects `Cache-Control` headers from ADR-004, but the app does not explicitly surface cached content on network failure. Composition requests fail with an unhandled exception or generic error.
- **Web**: Browser HTTP cache provides some implicit offline capability, but the SPA fails to load if the initial HTML/JS bundle isn't cached. No service worker is configured. API failures show a spinner or crash.
- **iOS**: Not yet built.

### Key Constraints

- SDUI payloads are JSON trees (10–50 KB typical for a screen). They are small and cheap to cache.
- Content is highly time-sensitive: live scores stale in seconds, schedules in hours, editorial content in days.
- The app is consumption-oriented (read-only). There are no offline write/sync conflicts to resolve.
- The `cacheability` field from ADR-004 (`public`, `contextual`, `personalized`, `live`) already classifies staleness risk per response.

## Decision Drivers

- Users should see *something* on app launch with no connectivity, not a blank screen.
- Stale sports content (yesterday's scores, last-fetched schedule) is better than no content, provided it's clearly marked.
- Implementation cost must be proportional to value — offline sports content degrades quickly.
- The solution must not conflict with ADR-004's caching policy or add new server-side requirements.
- Platform-native patterns should be preferred over custom infrastructure.

## Options Considered

### Option A: Offline-First with Local Database

Full offline support via a local database (Room on Android, Core Data/SwiftData on iOS, IndexedDB on Web). The app syncs SDUI responses to a structured local store. On launch, always read from local DB first, then refresh from network.

Pros:
- Best possible offline experience — app is fully functional without network.
- Enables background sync and pre-fetching.
- Supports offline analytics queuing natively.

Cons:
- **High implementation cost.** Requires a local schema mirroring SDUI response shapes, migration strategy, sync conflict resolution, and cache invalidation logic per cacheability class.
- **Marginal value for sports content.** Scores and standings stale within minutes/hours. Users rarely need a full offline NBA app experience. The ROI of full offline support is low relative to the engineering investment.
- **Maintenance burden.** Every schema change must be mirrored in the local DB schema. Every new section type needs a storage mapping. This becomes permanent ongoing cost.
- **Conflict with server authority.** The SDUI model is server-authoritative by design. An offline-first local DB creates a second source of truth, complicating reasoning about what the user sees.

### Option B: Stale-While-Offline with Platform Cache (Recommended)

Serve the last successful SDUI response from platform HTTP cache or a simple disk snapshot when the network is unavailable. Show a connectivity indicator and allow pull-to-refresh to retry.

Pros:
- **Low implementation cost.** Leverages existing HTTP cache (OkHttp, URLSession, browser cache) and adds a thin snapshot layer for cold-launch resilience.
- **Proportional to value.** Users see the last-fetched content (which may be minutes to hours old) rather than nothing. Good enough for schedule lookups, checking yesterday's results, or browsing editorial content.
- **No schema coupling.** Cached content is opaque JSON — no local schema to maintain, no migrations, no sync logic.
- **Preserves server authority.** The server remains the single source of truth. Cached content is explicitly marked as stale, not presented as current.
- **Compatible with ADR-004.** The `cacheability` field on responses already classifies staleness risk. The client can use it to decide what's safe to show stale vs. what should show a "content unavailable" placeholder (e.g., `live` cacheability content is not shown stale — display "Live data unavailable" instead).

Cons:
- **Not truly offline.** A cold install with no prior cache shows an error/empty state — there's no pre-seeded content.
- **Stale content risk.** Users might see yesterday's scores without realizing they're stale if the indicator is too subtle.
- **No background pre-fetch.** Content is only cached on user-initiated fetches, not proactively.

### Option C: Service Worker (Web) + Stale-While-Revalidate (Mobile)

Web: register a service worker that caches the SPA shell and API responses. Mobile: identical to Option B but with the addition of a stale-while-revalidate HTTP strategy where cached responses are served immediately and refreshed in the background.

Pros:
- Adds true offline support for the Web SPA (shell + cached API responses).
- Stale-while-revalidate gives users instant first paint from cache with seamless background updates.
- Progressive enhancement — works even if the service worker fails to install.

Cons:
- **Service worker complexity.** Cache management, versioning, and invalidation adds non-trivial web-specific complexity.
- **Stale-while-revalidate on mobile requires interceptor changes.** OkHttp supports it natively (`CacheControl.FORCE_CACHE`), but integrating it into the SDUI data pipeline requires explicit handling in the repository/ViewModel layer.
- **Same cold-launch gap as Option B** — no pre-seeded content.

### Option D: Pre-Seeded Bundled Content

Ship a bundled JSON snapshot of the default screen (e.g., the "For You" feed) in the app binary. On first launch with no connectivity, render the bundled content (clearly marked as a placeholder). Subsequent launches use HTTP cache (Option B).

Pros:
- Eliminates the cold-install-no-network blank screen problem.
- Very low runtime cost — it's a static asset.

Cons:
- **Bundled content stales at app release frequency.** A snapshot from the build date is stale within hours for sports content.
- **Increases app binary size** (typically 10–50 KB — minimal).
- **Maintenance cost.** Bundled snapshot must be updated each release or automated in CI.
- **Limited scope.** Only covers one screen. Multi-screen pre-seeding multiplies bundle size and maintenance.

## Evidence

- **SDUI payload sizes**: Typical screen response is 10–50 KB JSON. Full feed with 15 sections averages ~35 KB. These are trivially cacheable in any platform HTTP cache (OkHttp default: 10 MB; browser cache: ~50 MB; URLSession: configurable).
- **Content half-life**: Live game data stales in seconds. Schedules stale in hours. Editorial/promo content stales in days. The `cacheability` field (ADR-004) captures this gradient.
- **Competitive apps**: ESPN, Yahoo Sports, and The Score all use stale-cache approaches with connectivity banners. None offer meaningful offline-first experiences for sports content.
- **Existing infrastructure**: OkHttp HTTP cache is already configured on Android. ADR-004 defines `Cache-Control` header policy. The client infrastructure for Option B is 80% in place.

## Decision Outcome

**Option B** is chosen as the primary strategy, with elements of **Option D** as an optional enhancement.

Option B is proportional to the value of offline sports content. It leverages existing platform cache infrastructure, requires minimal new code, preserves server authority, and provides a meaningfully better experience than a blank screen. The `cacheability` field from ADR-004 provides the staleness classification needed to make per-section decisions about what's safe to show offline.

Option D (pre-seeded content) is recommended as a low-cost addition for v2 to eliminate the cold-install gap, but is not required for the initial implementation.

Options A and C are deferred. Option A's cost is disproportionate to value for a sports content app. Option C is valuable for the Web platform specifically and may be revisited when Web offline support becomes a priority, but is not justified for the initial rollout.

## Consequences

- Short term:
  - Platform teams implement a cache-fallback path in the SDUI data pipeline (repository/ViewModel layer) that detects network failure and serves the last cached response.
  - A non-blocking connectivity indicator (banner/snackbar) is shown when serving stale content.
  - Sections with `cacheability: "live"` that are stale show a "Data unavailable — pull to refresh" placeholder instead of stale data.
  - Actions that require network (navigate to deep link, refresh, mutate) show a "No connection" message on failure rather than silently failing.
- Medium term:
  - Analytics queue: fire-and-forget analytics actions are queued locally and flushed when connectivity resumes (prevents analytics data loss during brief outages).
  - Web: evaluate service worker addition (Option C) for SPA shell caching.
- Long term:
  - If offline demand grows (e.g., international users with poor connectivity), revisit Option A. The stale-cache approach does not preclude a future migration to a local DB — the server-authoritative model is preserved either way.

## Implementation Notes

### Android

```kotlin
// Repository layer — cache fallback on network failure
suspend fun fetchScreen(screenId: String): SduiScreenResponse {
    return try {
        api.getScreen(screenId)  // OkHttp caches successful responses per Cache-Control
    } catch (e: IOException) {
        // Network failure — attempt cache hit
        val cached = cache.getCachedScreen(screenId)
        if (cached != null) {
            cached.copy(isStale = true)  // Flag for UI staleness indicator
        } else {
            throw OfflineNoContentException(screenId)
        }
    }
}
```

The `isStale` flag drives a persistent banner: "You're offline — showing last updated content." Sections with `cacheability: "live"` and `isStale = true` render the placeholder ("Live data unavailable") instead of stale data.

### Web

```typescript
// Fetch wrapper — cache fallback
async function fetchScreen(screenId: string): Promise<SduiScreenResponse> {
  try {
    const res = await fetch(`/api/screens/${screenId}`);
    if (!res.ok) throw new Error(res.statusText);
    return res.json();
  } catch {
    // Attempt cache hit (Cache API or in-memory LRU)
    const cached = await screenCache.get(screenId);
    if (cached) return { ...cached, isStale: true };
    throw new OfflineNoContentError(screenId);
  }
}
```

### Staleness UX Rules

| Cacheability | Offline Behavior |
|---|---|
| `public` | Show stale content with timestamp ("Last updated 2h ago") |
| `contextual` | Show stale content with staleness indicator |
| `personalized` | Show stale content with staleness indicator (personalization may be outdated) |
| `live` | Show placeholder: "Live data unavailable — pull to refresh" |

### Scope

- **In scope**: Feed screens, game detail, standings, schedule — any composed screen.
- **Out of scope**: Video playback (requires streaming), real-time score updates (requires SSE/WebSocket), purchase flows (requires network).

## Open Questions

- Should the staleness banner show absolute time ("Last updated 3:42 PM") or relative time ("Updated 2 hours ago")? Relative is more intuitive but requires client-side clock management.
- Should `personalized` content be shown stale, or should it fall back to a generic (non-personalized) cached version if available? Showing stale personalized content could expose outdated entitlement states.
- What is the maximum acceptable staleness before cached content is discarded entirely? (Suggest: 24 hours for non-live content, immediate discard for `live`.)

## Follow-ups

- [ ] Android: Implement cache-fallback path in SDUI repository layer
- [ ] Android: Add offline connectivity banner composable
- [ ] Android: Add per-section staleness placeholder for `live` cacheability
- [ ] Web: Implement cache-fallback in fetch wrapper
- [ ] Web: Add offline banner component
- [ ] Web: Evaluate service worker for SPA shell caching (Option C)
- [ ] Server: Ensure all composition responses include appropriate `Cache-Control` headers per ADR-004
- [ ] Analytics: Implement local queue for analytics events during offline periods
- [ ] (v2) Evaluate pre-seeded bundled content (Option D) for cold-install scenario
