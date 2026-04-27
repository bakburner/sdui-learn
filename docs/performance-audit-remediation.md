# SDUI Renderer Performance Audit — Remediation Plan

**Date:** 2026-04-25
**Scope:** Android (Kotlin/Compose), iOS (Swift/SwiftUI), Web (React/TypeScript)
**Focus:** Memory leaks, main-thread blocking, unnecessary re-renders, unbounded growth

---

## Summary

Three systemic patterns appear across all platforms:

1. **Unbounded maps/buffers** — SSE message buffers, failure counters, and miss
   counters grow without eviction across screen navigations.
2. **Main-thread JSON work on the real-time path** — All three platforms perform
   synchronous serialization in the data-binding hot path at SSE message
   frequency.
3. **Missing list keys/identity** — Positional indices instead of stable keys
   cause unnecessary re-renders and state leakage when list contents change.

---

## Critical Issues

### C-1 · Android — Table renderers use eager `forEach` instead of `LazyColumn`

**Files:**
- `android/sdui-core/…/renderer/sections/BoxscoreTableRenderer.kt` (lines 139–165)
- `android/sdui-core/…/renderer/sections/SeasonLeadersTableRenderer.kt` (lines 162–210)

**Problem:** `sortedPlayers.forEach { … Row(…) }` composes every row eagerly.
BoxscoreTable renders 12–15 rows; SeasonLeadersTable can exceed 50. All rows
and their nested children stay in memory and recompose together.

**Impact:** Main-thread jank on scroll; full recomposition when any state changes;
memory bloat from keeping all row hierarchies alive.

**Remediation:** Replace `forEach` with `LazyColumn` + `itemsIndexed` with stable
keys derived from player identity.

---

### C-2 · Android — `AtomicScrollContainer` missing stable keys

**File:** `android/sdui-core/…/renderer/atomic/AtomicScrollContainer.kt`
(lines 113, 136, 153, 176)

**Problem:** Four `itemsIndexed(children) { _, child -> … }` call sites use
positional indexing without keys. When the children list changes order or items
are inserted/removed, Compose reuses the wrong composable state.

**Impact:** Visual glitches (content in wrong slot), state leakage between items,
unnecessary recomposition.

**Remediation:** Provide a `key` lambda based on element identity
(`element.id` or a stable hash).

---

### C-3 · Android — `sseMessageBuffer` grows without bound

**File:** `android/sdui-core/…/screen/SduiScreenViewModel.kt` (line 80, 500–510)

**Problem:** `sseMessageBuffer` is a `mutableMapOf` that accumulates SSE messages
for off-screen sections. No eviction policy or TTL. If the app is backgrounded
during a long-running game the buffer grows indefinitely.

**Impact:** Memory leak proportional to message rate × background duration.

**Remediation:** Clear the buffer in `stopAbly()` and/or apply a
time-based eviction policy (e.g. 5-minute TTL per entry).

---

### C-4 · Android — `pollFailureCounts` and `consecutiveMissCounts` never cleared

**Files:**
- `android/sdui-core/…/screen/SduiScreenViewModel.kt` (line 310)
- `android/sdui-core/…/data/DataBindingResolver.kt` (lines 30–45)

**Problem:** Both maps accumulate entries keyed by section ID or binding path.
`stopAllPolling()` clears polling jobs but not `pollFailureCounts`.
`resetCounters()` on the DataBindingResolver is never called when sections exit.

**Impact:** Unbounded map growth across screen navigations.

**Remediation:**
- Add `pollFailureCounts.clear()` to `stopAllPolling()`.
- Call `dataBindingResolver.resetCounters(sectionId)` when a section is removed.

---

### C-5 · iOS — JSON serialization on `@MainActor` in data-binding hot path

**File:** `ios/Sources/SduiCore/Screen/SduiScreenViewModel.swift` (lines 457–490)

**Problem:** `JSONSerialization.data()` and `JSONSerialization.jsonObject()` run
synchronously on the main actor for every Ably message. Multiple encode/decode
cycles block UI updates.

**Impact:** Visible jank on live scoreboards with high-frequency updates.

**Remediation:** Move JSON transformations to a background queue
(`Task.detached(priority: .userInitiated)`) and await the result.

---

### C-6 · iOS — URLSession tasks not cancellable on screen dismissal

**File:** `ios/Sources/SduiCore/Network/SduiRepository.swift` (line 53)

**Problem:** `URLSession.shared.data(for:)` tasks are fire-and-forget. When a
screen is dismissed mid-fetch, the response completion may reference deallocated
state and bandwidth is wasted.

**Impact:** Potential crashes; unnecessary network traffic.

**Remediation:** Store the active fetch as a `Task` property and cancel it in
`deinit` or on screen dismissal.

---

### C-7 · Web — Deep-clone on every data-binding update

**File:** `web/src/runtime/DataBindingApplier.ts` (line 37)

**Problem:** `JSON.parse(JSON.stringify(sectionData))` performs a full
synchronous deep clone of the entire section data object on every binding update.
Destroys structural sharing and kills downstream memoization.

**Impact:** Blocks the main thread at SSE frequency; forces new object references
for every field even when most are unchanged.

**Remediation:** Use shallow cloning (`structuredClone` or `Object.assign`) that
copies only the touched paths, or apply immutable updates with a helper like
`immer`.

---

### C-8 · Web — Module-level event listener never removed

**File:** `web/src/hooks/useAppVisibility.ts` (line 22)

**Problem:** A `visibilitychange` listener is added at module load and never
removed. Callbacks in the `listeners` set accumulate as subscribers are added
without cleanup.

**Impact:** Callback set grows over the app lifetime.

**Remediation:** Move listener setup into a managed initialization function with
a corresponding teardown, or use a `WeakRef`-based subscriber set.

---

## High-Severity Issues

### H-1 · Android — `AtomicLiveClock` 100 ms tick loop

**File:** `android/sdui-core/…/renderer/atomic/AtomicLiveClock.kt` (lines 62–70)

**Problem:** Every live clock runs `while(true) { delay(100L) }` — 10 wakeups
per second. Five clocks on a game-detail screen produce 50 wakeups/sec sustained
for the duration of a game (~3 hours).

**Impact:** Battery drain; coroutine churn.

**Remediation:** Increase the interval to 500 ms (sufficient for MM:SS display)
and pause the loop when the composable is not visible.

---

### H-2 · Android — `AtomicImage` fallback state resets on URL change

**File:** `android/sdui-core/…/renderer/atomic/AtomicImage.kt` (lines 43–44)

**Problem:** `currentSrc` and `triedFallback` are both remembered with
`remember(boundSrc)`. When data binding changes `boundSrc`, both states are
recreated, resetting the fallback flag. If the URL oscillates this can cause an
infinite image-fetch loop.

**Impact:** Continuous network requests and recomposition.

**Remediation:** Use independent `remember` blocks and sync to `boundSrc` via
`LaunchedEffect`.

---

### H-3 · Android — TabGroup and Form render children without keys

**Files:**
- `android/sdui-core/…/renderer/sections/TabGroupRenderer.kt` (lines 52–59)
- `android/sdui-core/…/renderer/sections/FormRenderer.kt` (lines 38–51)

**Problem:** `forEach` without a `key` composable wrapper means Compose reuses
state by position when tabs switch or form fields reorder.

**Impact:** State leakage between tabs; form field values rendered in wrong slots.

**Remediation:** Wrap each child in `key(section.id)` or `key(field.stateKey)`.

---

### H-4 · Android — Ably listener unsubscribe timing risk

**File:** `android/sdui-core/…/data/AblyChannelManager.kt` (lines 177–197)

**Problem:** `awaitClose { channel.unsubscribe(messageListener) }` depends on
Flow cancellation running the close block. If the collecting coroutine is
cancelled abruptly the listener may not be unsubscribed.

**Impact:** Leaked listeners; `activeChannels` map growth.

**Remediation:** Wrap the `awaitClose` body in `try/catch` and also clean up
in `disconnect()` as a safety net.

---

### H-5 · Android — Background `AsyncImage` without size constraints

**File:** `android/sdui-core/…/renderer/SectionContainer.kt` (lines 88–97)

**Problem:** Background images use `Modifier.fillMaxSize()` with no placeholder,
error handler, or explicit size hint. Oversized images are loaded unscaled.

**Impact:** Potential OOM for large background images.

**Remediation:** Add `placeholder` and `error` painters; consider explicit size
hints if available from the payload.

---

### H-6 · Android — `SectionVisibilityTracker` blocking debounce

**File:** `android/sdui-core/…/state/SectionVisibilityTracker.kt` (lines 76–92)

**Problem:** Uses `delay()` inside `LaunchedEffect` to debounce section exits,
blocking the coroutine for 500 ms on every visibility change.

**Impact:** Delayed visibility tracking; coroutine starvation under rapid scroll.

**Remediation:** Debounce via a `MutableSharedFlow` with `debounce()` operator
instead of a blocking `delay()`.

---

### H-7 · iOS — `ScreenState.values` is not `Equatable`

**File:** `ios/Sources/SduiCore/Screen/ScreenState.swift` (lines 13–50)

**Problem:** `values: [String: Any]` on an `@Observable` class has no equatable
comparison. Any mutation to any key triggers re-evaluation of all subscribers,
i.e. the entire screen.

**Impact:** Single form-field keystroke redraws every section.

**Remediation:** Implement per-key observation or switch `values` to a type-erased
equatable dictionary.

---

### H-8 · iOS — `ForEach` allocates a new array every body evaluation

**File:** `ios/Sources/SduiCore/Rendering/ScreenShell.swift` (line 70)

**Problem:** `Array(screen.sections.enumerated())` creates a temporary array on
every SwiftUI body evaluation. SwiftUI sees a "new" view hierarchy even when
sections are unchanged.

**Impact:** Prevents view identity caching; unnecessary re-layout of all sections.

**Remediation:** Use index-based `ForEach(0..<screen.sections.count, id: \.self)`
or give sections stable `Identifiable` conformance.

---

### H-9 · iOS — Inconsistent weak self in `PollingDriver`

**File:** `ios/Sources/SduiCore/Network/PollingDriver.swift` (lines 87, 141)

**Problem:** Some `Task` closures capture `self` strongly while others use
`[weak self]`. Strong captures across isolation boundaries can keep polling alive
after the owning screen is dismissed.

**Impact:** Retain cycle; polling continues for deallocated screens.

**Remediation:** Use `[weak self]` consistently in all `Task` closures.

---

### H-10 · iOS — Ably listener closure may leak

**File:** `ios/Sources/SduiCore/Network/AblyChannelManager.swift` (lines 165–214)

**Problem:** The Ably event listener closure captures `[channelName]` and may
implicitly retain `self`. The `onTermination` handler defers cleanup via `Task`;
if the owning object deallocates first the listener dangles.

**Impact:** Ably listeners accumulate across navigations.

**Remediation:** Capture `[weak self]` in the listener closure and nil-check
before processing messages.

---

### H-11 · iOS — `KFImage` without max size constraints

**File:** `ios/Sources/SduiCore/Rendering/Atomic/AtomicImageView.swift` (lines 35–50)

**Problem:** `KFImage(url).resizable()` with no `.frame(maxWidth:maxHeight:)`
means an oversized remote image is loaded at full resolution.

**Impact:** OOM on low-memory devices for large images.

**Remediation:** Add device-relative max frame constraints.

---

### H-12 · Web — Color token resolution not memoized

**File:** `web/src/components/SectionContainer.tsx` (lines 31–37, 71–75)

**Problem:** `resolveColorToken()` walks `PALETTE` and `SEMANTIC` maps on every
call with no caching. Gradient backgrounds with 3+ stops multiply the cost.

**Impact:** N lookups × M sections × every render cycle.

**Remediation:** Memoize resolved colors keyed by `(scheme, tokenName)` or cache
the resolved CSS string per surface.

---

### H-13 · Web — `useRefreshPolicy` receives unstable object reference

**File:** `web/src/hooks/useRefreshPolicy.ts` (lines 79–89)

**Problem:** A spread-object `{ ...section, refreshPolicy: effectivePolicy }` is
passed as a prop, creating a new reference every render. The hook interprets each
new reference as a config change and restarts polling/SSE.

**Impact:** Unnecessary SSE reconnects and polling restarts.

**Remediation:** Memoize the section object with `useMemo` or destructure stable
scalar properties into the hook's dependency array.

---

### H-14 · Web — Images missing `width`/`height` and `loading="lazy"`

**File:** `web/src/components/atomic/AtomicImage.tsx` (lines 68–79)

**Problem:** `<img>` tags have no `width`/`height` attributes and no
`loading="lazy"`. The browser cannot reserve space before load, and all images
are fetched immediately regardless of viewport position.

**Impact:** Cumulative Layout Shift (CLS); wasted bandwidth for off-screen images.

**Remediation:** Emit `width`/`height` from the server payload (or derive from
`aspectRatio`) and add `loading="lazy"` for images below the fold.

---

### H-15 · Web — Nested `setTimeout` in impression tracking not cleaned on unmount

**File:** `web/src/hooks/useImpressionTracking.ts` (lines 56–70)

**Problem:** A nested `setTimeout` inside the dwell timer is not tracked by the
cleanup ref. If the component unmounts during the inner delay, the action fires
on a stale section.

**Impact:** Stale impression actions; potential React state-update-on-unmounted
warnings.

**Remediation:** Track the inner timeout ID and clear it in the `useEffect`
cleanup function, or use an `AbortController` token.

---

### H-16 · Web — Atomic components not wrapped in `React.memo()`

**Files:** `web/src/components/atomic/AtomicContainer.tsx`,
`AtomicText.tsx`, `AtomicButton.tsx`, and other atomic primitives.

**Problem:** Atomic components re-render whenever any parent prop changes because
they have no memoization boundary.

**Impact:** Full subtree re-render on any ancestor change; multiplied across
screens with 50+ atomic elements.

**Remediation:** Wrap each atomic primitive in `React.memo()` with appropriate
comparison for the `element` prop.

---

## Medium-Severity Issues

### M-1 · Android — `ObjectMapper` created per Ably token request

**File:** `android/sdui-core/…/data/AblyChannelManager.kt` (line 133)

Reuse the shared `ObjectMapper` instance instead of constructing a new one per
token parse.

---

### M-2 · iOS — Form field state not hydrated until `onAppear`

**File:** `ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift`
(lines 84–92)

`@State` initializes as empty string, then updates in `onAppear`, causing a
layout shift. Initialize in `init` from `screenState`.

---

### M-3 · iOS — `DisplayGrid` does not implement `Equatable`

**File:** `ios/Sources/SduiCore/Rendering/Atomic/AtomicDisplayGridView.swift`

Grid re-renders on every body evaluation because columns and rows have no
equatable comparison.

---

### M-4 · Web — Color token resolution loop in gradient backgrounds

**File:** `web/src/components/SectionContainer.tsx` (lines 71–75)

Resolved gradient CSS string is not memoized; iterates and resolves each stop
color on every render.

---

### M-5 · Web — `useImpressionTracking` `fireAction` recreated on parent re-render

**File:** `web/src/hooks/useImpressionTracking.ts` (lines 25–41)

`onAction` dependency causes `fireAction` to be recreated whenever the parent
re-renders, tearing down and re-registering IntersectionObservers.

---

### M-6 · Web — `JSON.parse` on every SSE message

**File:** `web/src/runtime/AblyClient.ts` (line 65)

Check whether Ably already delivers parsed JSON in `message.data` before
redundantly parsing a string.

---

### M-7 · iOS — `onAppear` warning log for unknown elements fires per scroll

**File:** `ios/Sources/SduiCore/Rendering/Atomic/AtomicRouter.swift` (lines 35–42)

Log at route time rather than in `onAppear` to avoid repeated log spam during
scroll.

---

---

## UX Snappiness Issues

A second pass focused on user-visible jitter, flicker, layout shifts, and
transitions rather than raw throughput or memory.

### Cross-Platform Patterns

Three systemic UX patterns appear on all platforms:

1. **Unstable view identity** — `ForEach`/`itemsIndexed` keyed by array offset
   instead of element ID causes SwiftUI/Compose to destroy and recreate views
   on reorder or insertion, producing visible flicker.
2. **No state transitions** — Loading→loaded, tab switches, and screen
   navigation all cut instantly with no fade or animation.
3. **Real-time updates rebuild entire sections** — SSE/Ably data-binding
   replaces the full section object, resetting image loaders and forcing
   full-subtree re-renders.

---

### UX-1 · Android — No explicit Coil memory/disk cache configuration

**File:** `android/app/src/main/java/com/nba/sdui/app/SduiApplication.kt`
(lines 1–15)

**Symptom:** Images re-fetch on rotation, configuration changes, or when the same
URL appears in a new section.

**Root cause:** `ImageLoader.Builder` is created with only `SvgDecoder` — no
explicit `MemoryCache` or `DiskCache`. Defaults are small.

**Remediation:** Configure 25 % heap memory cache and 100 MB disk cache.

---

### UX-2 · Android — Image fallback triggers visible blank frame

**File:** `android/sdui-core/…/renderer/atomic/AtomicImage.kt` (lines 43–73)

**Symptom:** When primary image fails, user sees empty space then a placeholder
pops in.

**Root cause:** No placeholder shown during initial load; `onError` updates
`currentSrc` state which triggers full recomposition.

**Remediation:** Show a server-provided placeholder immediately via `AsyncImage`'s
`placeholder` parameter; only swap `currentSrc` on explicit error.

---

### UX-3 · Android — Screen state updates cause full-screen recomposition

**File:** `android/sdui-core/…/screen/SduiScreenViewModel.kt` (lines 545–550)

**Symptom:** Every SSE message or form keystroke triggers a new `SduiScreenUiState
.Success` emission. All sections recompose even if only one section changed.

**Root cause:** `updateSectionInScreen` copies the entire `screen.sections` list
and emits a new monolithic state.

**Remediation:** Introduce per-section `StateFlow` emissions so only the affected
section recomposes. For form input, hold local state until blur/submit.

---

### UX-4 · Android — Conditional element transitions are abrupt

**File:** `android/sdui-core/…/renderer/atomic/AtomicConditional.kt` (lines 24–36)

**Symptom:** When a condition flips, child content pops in/out with no animation.

**Root cause:** Pure `if/else` branch with no `AnimatedContent` or `Crossfade`.

**Remediation:** Wrap the conditional in `AnimatedContent(targetState =
conditionMet)`.

---

### UX-5 · Android — Tab switch and navigation have no animation

**Files:**
- `android/sdui-core/…/renderer/sections/TabGroupRenderer.kt` (lines 35–60)
- `android/sdui-core/…/screen/SduiNavigationShell.kt`

**Symptom:** Tab content swaps instantly (brief blank flash if new content has
images). Screen navigation is a hard cut.

**Root cause:** No `AnimatedContent` or transition spec applied.

**Remediation:** Wrap tab content in `AnimatedContent(targetState =
activeTabIndex)`. Add `slideInHorizontally`/`fadeIn` transition spec to
screen-level state changes.

---

### UX-6 · Android — SSE buffer flush causes cascading recompositions

**File:** `android/sdui-core/…/screen/SduiScreenViewModel.kt` (lines 459–510)

**Symptom:** When the app returns to foreground, buffered messages apply one at a
time — 5 sections = 5 sequential state emissions = 5 recomposition frames.

**Root cause:** Each `applyAblyMessage` emits a new `UiState.Success`
independently.

**Remediation:** Batch all buffered messages into a single state emission.

---

### UX-7 · Android — Skeleton default height mismatches real content

**File:** `android/sdui-core/…/renderer/SectionSkeleton.kt` (lines 20–110)

**Symptom:** Skeleton shimmer renders at 80 dp default, then real content at
200 dp causes a visible layout jump.

**Root cause:** `minHeightDP` defaults to 80 when the server omits it.

**Remediation:** Server should emit accurate `minHeightDP` per section. Client
should cache the previous rendered height as a heuristic fallback.

---

### UX-8 · Android — BoxscoreTable player sorting re-runs on every recomposition

**File:** `android/sdui-core/…/renderer/sections/BoxscoreTableRenderer.kt`
(lines 87–90)

**Symptom:** Sorting is snappy for small rosters but drops frames for 20+ players
when SSE data arrives (because `model.players` is a new reference).

**Root cause:** `remember(model.players, …)` uses reference equality; a
`data.copy(players = …)` creates a new list even with identical content.

**Remediation:** Use structural comparison keys (e.g. `players.hashCode()`,
`players.size`) or memoize the sort output via `derivedStateOf`.

---

### UX-9 · Android — `bindRef` resolution runs on every recomposition for every leaf

**Files:**
- `android/sdui-core/…/renderer/atomic/AtomicText.kt` (lines 40–48)
- `android/sdui-core/…/renderer/atomic/AtomicImage.kt` (lines 37–40)

**Symptom:** In AtomicComposite sections with real-time binding, every leaf
re-resolves its bind path when the parent `CompositionLocal` updates, even if the
specific bound value didn't change.

**Root cause:** `LocalCompositeContent` is scoped to the entire composite section.

**Remediation:** Memoize the resolved value per element with `remember` keyed on
the specific bound path's value, not the entire composite content map.

---

### UX-10 · Android — Color token resolution not cached

**File:** `android/sdui-core/…/renderer/ColorTokenResolver.kt`

**Symptom:** Dark mode toggle re-resolves every color token across all
composables simultaneously.

**Root cause:** `resolve()` is a pure function called inline in view bodies with
no caching.

**Remediation:** Add an in-memory cache keyed by `(token, isDark)`.

---

### UX-11 · iOS — `ForEach` identity uses `\.offset` in multiple locations

**Files:**
- `ios/Sources/SduiCore/Rendering/ScreenShell.swift` (line 70)
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicScrollContainerView.swift` (line 64)
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicContainerView.swift` (line 39)

**Symptom:** When sections or children reorder or insert, SwiftUI destroys and
recreates views (including KFImage instances) instead of moving them.

**Root cause:** `id: \.offset` is positional, not stable.

**Remediation:** Use `id: \.element.id` (sections) or `id: \.id` (atomic
children) for stable identity.

---

### UX-12 · iOS — Real-time data update triggers full section rebuild

**File:** `ios/Sources/SduiCore/Screen/SduiScreenViewModel.swift` (lines 435–452)

**Symptom:** When Ably data arrives for a live scoreboard, the entire section
flickers — images flash white momentarily before content reappears.

**Root cause:** The VM does a JSON encode → mutate dict → decode roundtrip,
producing a new `Section` value. SwiftUI treats it as a new view, resetting
`KFImage` loading state.

**Remediation:** Mutate the section's `data` in place without a JSON roundtrip, or
apply `.id(section.id)` so view identity is keyed on the stable section ID rather
than the section value.

---

### UX-13 · iOS — Form field text starts empty then populates (layout shift)

**File:** `ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift`
(lines 84–91)

**Symptom:** Text fields render blank for one frame, then snap to the pre-filled
value.

**Root cause:** `@State private var text: String = ""` is always empty on first
render; `.onAppear` sets the real value.

**Remediation:** Initialize `@State` in `init` from `screenState`.

---

### UX-14 · iOS — No transition between loading and loaded states

**File:** `ios/Sources/SduiCore/Rendering/ScreenShell.swift` (lines 57–87)

**Symptom:** Screen snaps from `ProgressView("Loading…")` to full content with no
fade.

**Root cause:** No `.transition()` modifier applied to the `Group` switch.

**Remediation:** Apply `.transition(.opacity)` and wrap the state change in
`withAnimation`.

---

### UX-15 · iOS — Image placeholder size mismatch causes content jump

**File:** `ios/Sources/SduiCore/Rendering/Atomic/AtomicImageView.swift`
(lines 38–41)

**Symptom:** `ProgressView()` placeholder is ~50 pt, but the final image fills
its `aspectRatio` frame (e.g. 200 pt). Content below jumps when the image loads.

**Root cause:** Placeholder doesn't inherit the image's aspect ratio frame.

**Remediation:** Give the placeholder a `Color.gray.opacity(0.2)` background that
matches the image's `aspectRatio` dimensions.

---

### UX-16 · iOS — Tab switch and navigation have no animation

**Files:**
- `ios/Sources/SduiCore/Rendering/Sections/TabGroupView.swift` (lines 40–41)
- `ios/Sources/SduiCore/Navigation/NavCoordinator.swift` (line 25)

**Symptom:** Tab content swaps instantly with a blank flash. Navigation `push`
has no animation.

**Root cause:** No `.transition()` on tab content; `path.append` not wrapped in
`withAnimation`.

**Remediation:** Add `.transition(.opacity)` on tab content with
`.animation(.easeInOut, value: selectedTab)`. Wrap `path.append` in
`withAnimation`.

---

### UX-17 · iOS — BoxscoreTable logo re-fetches on every data update

**File:** `ios/Sources/SduiCore/Rendering/Sections/BoxscoreTableView.swift`
(lines 17–20)

**Symptom:** Team logo flickers each time the scoreboard updates via Ably.

**Root cause:** Full section rebuild (UX-12) resets KFImage state even though
the logo URL didn't change.

**Remediation:** Extract logo into a separate view keyed by URL (`TeamLogoView`)
so SwiftUI preserves it across parent data changes.

---

### UX-18 · iOS — SeasonLeadersTable re-sorts on every render

**File:** `ios/Sources/SduiCore/Rendering/Sections/SeasonLeadersTableView.swift`
(lines 67–80)

**Symptom:** Large tables jank briefly on sort because `sortedPlayers()` runs in
the view body on every render pass.

**Root cause:** Computed property called inline without caching.

**Remediation:** Cache sorted result with `@State` and update only via
`.onChange(of: sortKey)`.

---

### UX-19 · iOS — Shimmer animation starts with a jump

**File:** `ios/Sources/SduiCore/Rendering/SectionSkeleton.swift` (lines 71–78)

**Symptom:** Shimmer gradient has a brief visible jump when it starts.

**Root cause:** `shimmerOffset` jumps from `-1` to `2` on first `.onAppear`
without easing.

**Remediation:** Delay the animation start by one frame or use an eased initial
transition.

---

### UX-20 · iOS — Form keystroke updates entire screen state

**File:** `ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift`
(lines 57–61)

**Symptom:** Typing feels laggy on large screens because each keystroke updates
`screenState`, triggering re-evaluation of all observers.

**Root cause:** `onChange(of: text)` calls `screenState.set()` synchronously.

**Remediation:** Debounce state propagation (300 ms) or hold local form state
until blur/submit.

---

### UX-21 · Web — Navigation shows blank screen during fetch

**File:** `web/src/App.tsx` (lines 145–160)

**Symptom:** Clicking a navigation link immediately clears the current screen and
shows a spinner for 2–3 seconds.

**Root cause:** `setScreenState({})` runs in a `useEffect` on `currentUri`
change, wiping the old screen before the new one arrives.

**Remediation:** Keep the old screen rendered (with an overlay spinner or opacity
fade) until the new screen data arrives.

---

### UX-22 · Web — Tab switch shows brief blank frame

**File:** `web/src/components/sections/TabGroup.tsx` (lines 48–62)

**Symptom:** Old tab content disappears instantly; new tab takes a render cycle to
appear.

**Root cause:** React unmounts the old `SectionList` and mounts the new one with
no transition.

**Remediation:** Use CSS `transition: opacity 200ms` on the tab content container,
or render both panes with absolute positioning and cross-fade.

---

### UX-23 · Web — Color scheme toggle flashes wrong colors (~100 ms)

**File:** `web/src/utils/ColorTokenResolver.ts`

**Symptom:** Toggling dark/light mode shows a brief flash of the wrong palette.

**Root cause:** React state update triggers re-render, but the `data-theme` DOM
attribute (which CSS custom properties depend on) isn't updated until after React
commits.

**Remediation:** Set `document.documentElement.setAttribute('data-theme', scheme)`
synchronously in `setColorSchemePreference()` before any React state update.

---

### UX-24 · Web — Form input re-renders all sections on every keystroke

**File:** `web/src/components/sections/Form.tsx` (lines 31–160)

**Symptom:** Typing feels sluggish on complex screens.

**Root cause:** `onStateChange` fires on every `onChange`, bubbling to `App.tsx`
which re-renders all children.

**Remediation:** Debounce `onStateChange` (300 ms) or hold local form state and
propagate on blur/submit.

---

### UX-25 · Web — Live data binding causes full section flicker

**File:** `web/src/components/LiveSectionWrapper.tsx` (lines 59–82)

**Symptom:** Score updates flash the entire section (images, headers, stats) even
when only one stat changed.

**Root cause:** `applyDataBindings()` returns an all-new object (JSON deep clone);
React sees every prop as changed.

**Remediation:** Use shallow/structural clone that preserves unchanged references,
or add `React.memo()` boundaries at the leaf level.

---

### UX-26 · Web — BoxscoreTable player headshots cause per-row layout shift

**File:** `web/src/components/sections/BoxscoreTable.tsx` (lines 148–155)

**Symptom:** Each row shifts vertically as its player image loads.

**Root cause:** `<img>` has no explicit `width`/`height`; browser cannot allocate
space before load.

**Remediation:** Set explicit pixel `width` and `height` (e.g. `48×48`) on player
headshot images.

---

### UX-27 · Web — Toast notifications pop abruptly with no queue

**File:** `web/src/runtime/ActionHandler.ts` (lines 288–327)

**Symptom:** Toasts appear/disappear without smooth animation; rapid actions
produce overlapping toasts.

**Root cause:** Raw DOM manipulation (`createElement`/`appendChild`) bypasses
React batching. No toast queue or stacking logic.

**Remediation:** Replace with a React-managed toast container (portal + state
array) with CSS opacity transitions and queue limit.

---

### UX-28 · Web — Skeleton shimmer misses `will-change` hint

**File:** `web/src/components/SectionSkeleton.tsx` (lines 50–70)

**Symptom:** Shimmer animation stutters on slower devices.

**Root cause:** No `will-change: transform` hint; browser doesn't promote the
element to a compositor layer.

**Remediation:** Add `willChange: 'transform'` to the shimmer animation style.

---

## Remediation Priority

| Priority | IDs | Rationale |
|---|---|---|
| **P0 — Memory leaks** | C-3, C-4, C-8, H-4, H-10 | Unbounded growth degrades over time; hardest to diagnose in production. |
| **P1 — Main-thread blocking** | C-5, C-7, H-1 | Directly visible as jank on live scoreboards. |
| **P2 — Virtualization** | C-1, C-2, H-3 | Large lists without recycling cause scroll-performance cliffs. |
| **P3 — Re-render flicker** | UX-3, UX-6, UX-12, UX-25, H-7, H-8, H-12, H-13, H-16 | Full-section rebuilds and deep clones produce visible flicker on live updates. |
| **P4 — View identity** | UX-11, UX-8, UX-9, UX-17 | Unstable keys destroy/recreate views unnecessarily; compounds with image reloads. |
| **P5 — Transitions** | UX-5, UX-14, UX-16, UX-21, UX-22 | Loading, tab, and navigation cuts feel unpolished without fade/slide. |
| **P6 — Input responsiveness** | UX-3, UX-13, UX-20, UX-24 | Form keystrokes propagate to entire screen; lag on complex screens. |
| **P7 — Image loading** | UX-1, UX-2, UX-15, UX-26, H-14, H-11 | Missing caches, placeholders, and dimensions cause flicker and layout shift. |
| **P8 — Polish** | UX-4, UX-7, UX-10, UX-19, UX-23, UX-27, UX-28, H-6, H-9, M-1–M-7 | Lower blast radius but contribute to perceived sluggishness. |
