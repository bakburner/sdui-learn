# Plan: Visibility-Gated Section Refresh

> Source requirements: §9d from sdui-requirements-summary.md, AGENTS.md Rule 9
> Related: plan-lifecycle-animation-pagination.md (lazy loading), plan-impression-tracking.md (visibility detection), ADR-009
> ADR dependencies: ADR-011 (cacheability drives pause defaults), ADR-012 (store-backed model simplifies implementation — see §ADR-012 Rewire)

## Summary

Pause polling and SSE processing for sections that are scrolled out of the viewport. Resume immediately when the section re-enters. This prevents unnecessary network traffic, battery drain, and wasted CPU on data-binding updates the user cannot see.

## Problem

Today both Android and web clients run **all** polling and SSE subscriptions for the entire screen lifecycle, regardless of whether a section is visible. A game detail screen with 10+ sections opens 10 SSE channels and multiple poll timers on load — even sections the user hasn't scrolled to yet.

From §9d:
> *"This is critical for the game detail page which could have 10+ sections — you don't want to open 10 SSE connections simultaneously on screen load."*

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `pauseWhenOffScreen` added to `RefreshPolicy` (boolean, default true) |
| Server support | Built | `pauseWhenOffScreen: false` on all SSE GamePanel sections in Scoreboard/GameDetail/LiveComposer |
| Android polling | Built (visibility-gated) | `SduiScreenViewModel.setupPolling()` gates on `_isAppForeground` + `visibilityTracker.awaitNearViewport()` |
| Android SSE | Built (visibility-gated) | Ably handler buffers messages when off-screen; applies on resume |
| Web polling | Built (visibility-gated) | `useRefreshPolicy` `enabled` gated by `useSectionVisibility` + `useAppVisibility` |
| Web SSE | Built (Option A) | SSE unsubscribes on `enabled=false`, resubscribes on `enabled=true` |
| Web visibility detection | Built | `useSectionVisibility` — single IntersectionObserver, 1.5× lookahead, 500ms debounce |
| Android visibility detection | Built | `SectionVisibilityTracker` — LazyListState-based with buffer zone + debounce |
| iOS polling | Built (visibility-gated) | `PollingDriver.awaitGate()` gates on `foregroundActive` + `visibleSections` + `pauseWhenOffScreen` |
| iOS SSE | Built (visibility-gated) | `AblyChannelManager.channelVisibility` gates message processing; `alwaysRefreshSections` bypasses for `pauseWhenOffScreen: false` |
| iOS visibility detection | Built | `SectionVisibilityTracker` — `.onAppear`/`.onDisappear` on LazyVStack items, 500ms exit debounce |
| iOS scene phase | Built | `handleScenePhase()` pauses polling + disconnects Ably on `.background`, resumes on `.active` |

## Design Decisions

### D1: Server-controlled opt-out via schema (AtomicComposites); client-autonomous for semantic sections

Add to `RefreshPolicy`:

```json
"pauseWhenOffScreen": { "type": "boolean", "default": true }
```

- **`true` (default):** Client pauses refresh when section leaves viewport. Covers the majority of sections.
- **`false`:** Client keeps refreshing regardless of visibility. Reserved for critical sections where stale data on scroll-back is unacceptable (e.g., a live game score pinned near the top that may briefly scroll off during fast fling).

**Why a schema field and not purely client-side?** The distinction is between semantic and opaque sections:

- **Semantic sections** (GamePanel, BoxscoreTable, etc.) — the client knows what these are and can make autonomous pause decisions. A client could reasonably decide "GamePanel is always-on" without server instruction.
- **AtomicComposites** — the client has no semantic knowledge. It renders an opaque tree. Only the server knows whether an AtomicComposite contains a critical live indicator or a static promo card. The schema field gives the server a way to communicate this intent for opaque sections.

The server also uses ADR-011 cacheability as a signal: `live` cacheability sections default to `pauseWhenOffScreen: false`. The schema field overrides when needed.

### D2: Viewport threshold with debounce

Use a **single 1.5× viewport lookahead** with a **500ms debounce before pausing**.

- **Enter:** Section is within 1.5× viewport distance → resume refresh **immediately** (no debounce on resume).
- **Exit:** Section leaves the 1.5× zone → start a 500ms debounce timer. If the section re-enters within 500ms (fast scroll bounce), cancel the timer and stay active. If the timer expires, pause refresh.

The debounce replaces the previous dual-threshold hysteresis design (1.5× enter, 2× exit), which would have required two `IntersectionObserver` instances with race conditions. A single observer with a debounce timer is simpler and achieves the same goal.

Platform mapping:
- **Web:** Single `IntersectionObserver` with `rootMargin: "50% 0px"` + 500ms `setTimeout` on exit.
- **Android Compose:** `LazyListState.layoutInfo.visibleItemsInfo` + buffer calculation. Debounce via `delay(500)` in coroutine before emitting `false`.
- **iOS SwiftUI:** `.onAppear` / `.onDisappear` on `LazyVStack` items (SwiftUI applies ~1 screen lookahead by default). Debounce via `Task.sleep` before signaling pause.

### D3: Pause vs. disconnect semantics

| Refresh type | On pause | On resume |
|-------------|----------|-----------|
| **Poll** | Cancel pending timer; do NOT fire in-flight request | Fire immediately, then resume interval timer |
| **SSE (Ably)** | Keep channel subscribed but **stop applying** incoming messages to section data | Apply any buffered message (latest only), resume normal processing |

**Rationale for SSE keep-alive:** Ably channel subscribe/unsubscribe has overhead (handshake, auth). Frequent connect/disconnect during scrolling would degrade performance. Instead, stay subscribed but skip the `DataBindingResolver.applyBindings()` call. On resume, apply only the **most recent** buffered message — not the full backlog — to avoid a cascade of re-renders.

### D4: Interaction with stale-section tracking

The existing `_staleSections` mechanism (Android) marks sections stale after repeated poll failures. Visibility-gated pause is **not** a stale condition — a paused section should show its last-known data without a stale indicator. Only poll/SSE failures trigger staleness.

### D5: App background/foreground lifecycle

Befor tackling scroll-based visibility, implement **app-level lifecycle pause** — when the app goes to background, pause ALL polls and SSE processing. This is higher impact than scroll-based pausing (affects 100% of sections, not just off-screen ones) and simpler to implement.

| Platform | Signal | Action |
|----------|--------|--------|
| **Android** | `Lifecycle.Event.ON_STOP` | Cancel all poll timers. Stop applying SSE messages. |
| **iOS** | `ScenePhase.background` | Cancel all poll `Task`s. Stop applying SSE messages. |
| **Web** | `document.visibilitychange` → `hidden` | Clear all poll intervals. Stop applying SSE messages. |

On foreground resume: fire an immediate poll for all visible `poll`-type sections, resume SSE processing. SSE channels stay subscribed (Ably manages its own background behavior).

This should be **Phase 0** of the implementation — it's simpler and provides a superset of the benefit for the backgrounded case.

### D6: TabGroup nested sections

TabGroup renders multiple tabs but only one tab is active/visible at a time. The rule:

- **Active tab's sections:** follow normal viewport visibility rules (pause if scrolled off-screen).
- **Inactive tab's sections:** always paused, regardless of DOM/Compose position. When the user switches tabs via `mutate` action, the newly active tab's sections resume; the previously active tab's sections pause.
- **Implementation:** TabGroup exposes which tab index is active (from `screenState`). `SectionVisibilityTracker` / `useSectionVisibility` checks both viewport position AND tab activity. A section is "near viewport" only if its parent tab is active AND it's within the viewport buffer.

## Tasks

### Phase 0: App Background/Foreground Lifecycle

- [x] **Android:** Observe lifecycle in `GameDetailScreen`; on `ON_STOP` set `_isAppForeground=false` which gates poll loops and SSE `applyBindings()`. On `ON_START` resume. Implemented in `SduiScreenViewModel.onAppBackgrounded()/onAppForegrounded()`.
- [x] **Web:** Created `useAppVisibility` hook using `useSyncExternalStore` + `document.visibilitychange`. Wired into `LiveSectionWrapper` — `enabled` includes `isAppVisible`.
- [x] **Unit tests:** Web `useAppVisibility.test.ts` verifies background/foreground transitions. Android `VisibilityGatedRefreshTest.kt` verifies foreground flag gates poll/SSE.

### Phase 1: Schema & Codegen

- [x] Add `pauseWhenOffScreen` (boolean, default `true`) to `RefreshPolicy` in `schema/sdui-schema.json`
- [x] Added `pauseWhenOffScreen` to `SduiModels.ts` (TypeScript) and `SduiModels.kt` (Kotlin) manually. Full codegen blocked on `quicktype` installation.
- [x] Run `cd codegen && ./generate.sh` — all platforms regenerated (TypeScript, Swift, Java POJOs). Kotlin uses hand-written model.

### Phase 2: Server

- [x] Set `pauseWhenOffScreen: false` on `GamePanel` sections in `ScoreboardComposer`, `GameDetailComposer`, and `LiveComposer` (all SSE refresh policies).
- [x] All other sections use the default (`true`) — no changes needed
- [x] Verified: `pauseWhenOffScreen` field present in codegen output for all platforms (TypeScript, Swift, Java). Server composers set `false` on GamePanel SSE sections.

### Phase 3: Web Client

- [x] **Extract visibility hook** — `web/src/hooks/useSectionVisibility.ts`
  - Single `IntersectionObserver` with `rootMargin: "50% 0px"` for 1.5× lookahead
  - Returns `isNearViewport: boolean`
  - 500ms debounce on exit (setTimeout; cancel if re-enters within window)
- [x] **Refactor `LiveSectionWrapper`** — `web/src/components/LiveSectionWrapper.tsx`
  - Added `sectionRef` + `useSectionVisibility` + `useAppVisibility` inside wrapper
  - Compute `enabled = hasRefreshPolicy && isAppVisible && (pauseWhenOffScreen ? isNearViewport : true)`
  - Wrapped children in `<div ref={sectionRef}>` for IntersectionObserver
- [x] **SSE subscription behavior resolved (Option A):** Setting `enabled=false` unsubscribes; `enabled=true` resubscribes. Ably reconnect on existing WebSocket is ~100ms. Option B (keep-alive + buffer) deferred unless latency is measured as a problem.
- [x] **Unit tests** — `useSectionVisibility.test.ts`, `useAppVisibility.test.ts`, `useRefreshPolicy.test.ts`, `LiveSectionWrapper.test.tsx`
  - Mock `IntersectionObserver` → verify enter/exit/debounce behavior
  - Verify poll timers stop on `enabled=false`, SSE unsubscribes on disable
  - Verify `pauseWhenOffScreen: false` sections pass `enabled=true` even when off-screen

### Phase 4: Android Client

- [x] **Visibility tracker** — `android/sdui-core/.../state/SectionVisibilityTracker.kt`
  - Compose utility that tracks which section IDs are near the viewport
  - Uses `LazyListState.layoutInfo` + buffer zone calculation. Exposes `isNearViewport(sectionId)` and `awaitNearViewport(sectionId)`
  - 500ms debounced exit; immediate entry
- [x] **Wire into `SduiScreenViewModel`**
  - `setupPolling()`: waits for `_isAppForeground` + `awaitNearViewport()` before each poll tick when `pauseWhenOffScreen` is true
  - SSE handler: checks visibility before `applyBindings()`; buffers latest message per section; applies buffered message on visibility resume via `visibleSections` collector
- [x] **SSE pause** — in Ably message handler:
  - Checks visibility before calling `DataBindingResolver.applyBindings()`
  - Buffers latest message per section; applies on re-entry via `visibleSections` flow collector
- [x] **Wire into `SduiScreenContent`** — added `visibilityTracker` param, `rememberLazyListState()`, and `visibilityTracker.Observe()` call. `GameDetailScreen` passes `viewModel.visibilityTracker`.
- [x] **Unit tests** — `SectionVisibilityTrackerTest.kt`, `VisibilityGatedRefreshTest.kt`
  - Verify visibility flow transitions (enter/exit, immediate/debounced)
  - Verify `awaitNearViewport` suspends until section enters viewport
  - Verify poll gate logic: foreground × visibility × pauseWhenOffScreen
  - Verify SSE buffer pattern (latest message wins, apply on resume)

### Phase 5: Documentation

- [x] Update `docs/plans/client-implementors-contract.md` — added §8a (Visibility-Gated Refresh) with pseudocode algorithms + Phase 3 build checklist items 17a/17b
- [x] Update `docs/sdui-requirements-summary.md` §9d — status changed from Gap to Partial (Built). Status matrix updated.
- [x] Update `plan-lifecycle-animation-pagination.md` — added cross-reference, split REQ-1 into REQ-1a (built) and REQ-1b (remaining), updated status table

## Pseudocode

### Web — `useSectionVisibility`

```typescript
function useSectionVisibility(ref: RefObject<HTMLElement>): boolean {
  const [isNear, setIsNear] = useState(false);
  const exitTimer = useRef<ReturnType<typeof setTimeout>>();
  
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          // Resume immediately — no debounce on entry
          clearTimeout(exitTimer.current);
          setIsNear(true);
        } else {
          // Debounce exit by 500ms to absorb scroll bounce
          exitTimer.current = setTimeout(() => setIsNear(false), 500);
        }
      },
      { rootMargin: '50% 0px' }  // 1.5× viewport lookahead
    );
    if (ref.current) observer.observe(ref.current);
    return () => {
      observer.disconnect();
      clearTimeout(exitTimer.current);
    };
  }, [ref]);
  return isNear;
}
```

### Web — `LiveSectionWrapper` wiring

Note: this requires refactoring the existing `LiveSectionWrapper` which currently
derives `enabled` internally. The visibility hook is added *inside* the wrapper,
not passed from a parent.

```typescript
function LiveSectionWrapper({ section, children }) {
  const ref = useRef<HTMLDivElement>(null);
  const isNear = useSectionVisibility(ref);
  const hasRefreshPolicy = Boolean(
    section.refreshPolicy?.type && section.refreshPolicy.type !== 'static'
  );
  const pause = section.refreshPolicy?.pauseWhenOffScreen ?? true;
  const enabled = hasRefreshPolicy && (pause ? isNear : true);

  useRefreshPolicy({ section, enabled });

  return <div ref={ref}>{children}</div>;
}
```

### Android — ViewModel polling with visibility gate

```kotlin
private fun setupPolling(screen: SduiScreen, visibilityTracker: SectionVisibilityTracker) {
    screen.sections.filter { it.refreshPolicy?.type == "poll" }.forEach { section ->
        val pause = section.refreshPolicy?.pauseWhenOffScreen ?: true
        pollingJobs[section.id] = viewModelScope.launch {
            while (isActive) {
                // Gate: check visibility BEFORE the fetch, not just at loop top
                if (pause) {
                    visibilityTracker.awaitNearViewport(section.id)
                }
                val result = repository.fetchRawJson(section.refreshPolicy!!.url!!)
                // apply result...
                
                // Gate again before sleeping — if section went off-screen during
                // the fetch, suspend immediately instead of waiting a full interval
                // then fetching once more before checking.
                if (pause && !visibilityTracker.isNearViewport(section.id)) {
                    continue  // loops back to awaitNearViewport
                }
                delay(section.refreshPolicy!!.intervalMs!!.toLong())
            }
        }
    }
}
```

## Success Metrics

| Metric | Measurement | Target |
|--------|-------------|--------|
| Active poll timers on game detail | Count `pollingJobs` entries with active timers vs total | Reduce from N (all sections) to 2–3 (visible sections only) |
| SSE message processing on game detail | Count `applyBindings()` calls per minute | Reduce by ~60% (only processing visible sections) |
| Battery impact during passive viewing | Android Battery Historian: wake lock duration | Measurable reduction in CPU wake time |
| Scroll-back stale flicker | Manual QA: scroll away, wait 10s, scroll back | No visible stale flash — data should be ≤500ms behind live |

## Dependencies

- Existing `IntersectionObserver` infrastructure on web (from impression tracking)
- Existing `pollingJobs` map on Android (from `SduiScreenViewModel`)
- Schema codegen pipeline
- **ADR-011** (Data Classification & Freshness): cacheability classes inform default pause behavior
- **ADR-012** (Client Data Architecture): if accepted, simplifies SSE pause implementation (see below)

## Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Scroll jank from frequent observer callbacks | Low | 500ms debounce on exit + `IntersectionObserver` is off-main-thread on web |
| User scrolls back to stale section | Medium | Immediate poll on re-entry; 1.5× lookahead pre-warms; 500ms debounce prevents premature pause |
| Ably resubscribe latency (web Option A) | Low | Ably resubscribe on existing WebSocket is ~100ms. Move to Option B if measurable. |
| TabGroup nested sections | Low | Design D6: inactive tab sections always paused; active tab follows viewport rules |

## ADR-012 Rewire

This plan is designed against the current **in-memory model** (data bindings write to ViewModel StateFlow). If ADR-012 (persistent section-level blob store) is accepted, the rewire is minimal:

| Aspect | Current (in-memory) | After ADR-012 (store-backed) |
|--------|--------------------|--------------------------|
| **Poll pause** | Cancel timer (same) | Cancel timer (same) |
| **Poll resume** | Fetch → update StateFlow | Fetch → `store.upsert()` |
| **SSE pause (web)** | Unsubscribe or buffer in ref | Stop calling `store.upsert()` |
| **SSE resume (web)** | Resubscribe or apply buffered msg | Resume `store.upsert()` — store has last-written data |
| **SSE pause (Android)** | Buffer latest message in memory | Stop calling `store.upsert()` |
| **SSE resume (Android)** | Apply buffered message | Resume `store.upsert()` — store has last-written data |

The store eliminates the need for manual SSE message buffering. The store IS the buffer. Pause the writer; the reader always has last-written data. The diff is 2–3 call sites per platform.

## Open Questions

- [ ] Should the lookahead threshold be configurable per-screen from the server (e.g., `viewportBuffer: 1.5`) or fixed client-side?
- [ ] Should we emit a `refreshPaused` / `refreshResumed` analytics event for observability?
- [ ] ~~For SSE, should we buffer the last N messages or strictly the last one?~~ Resolved: if ADR-012 is accepted, the store handles this. If not, buffer strictly the last one — multiple bindings all apply from the same message.
