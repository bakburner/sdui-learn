Apply SDUI renderer performance audit remediation across clients

Address the issues catalogued in `docs/performance-audit-remediation.md`,
focused on the three systemic patterns it flagged: unbounded SSE/polling
buffers, synchronous JSON serialization on the data-binding hot path, and
positional list identity that destroys view state on reorder. Touches all
three client renderers; no schema or server changes.

--- Cross-platform memory + lifetime ---

- Bound the Android `sseMessageBuffer` with a 5-minute TTL, prune
  expired entries on every write, clear on `stopAbly()`, and clear
  `pollFailureCounts` on `stopAllPolling()` so off-screen sections and
  failure counters can no longer accumulate across screen navigations
  (C-3, C-4).
- Convert iOS `ScreenState` to per-key `KeySlot` reference cells under
  `@Observable`, replacing the coarse `[String: Any]` dictionary so a
  single keystroke no longer invalidates every observer of the screen
  (H-7). Typed convenience setters now cast their value to `Any?` at the
  call boundary so Swift overload resolution forwards them to the
  generic setter instead of recursing into themselves.
- Convert web `useAppVisibility` to a module-level `useSyncExternalStore`
  source with `register`/`unregister` semantics so the listener set
  drains as React tears down subscribers (C-8).
- Wrap Android Ably `awaitClose` cleanup in try/catch and add a
  defensive cleanup pass in `disconnect()` so abrupt collector
  cancellation can no longer leak channel listeners or grow
  `activeChannels` (H-4).
- Capture `[weak self]` in the iOS Ably listener closure and nil-check
  before processing messages so dangling listeners can no longer outlive
  the owning manager (H-10).
- Use `[weak self]` consistently in iOS `PollingDriver` `Task` closures
  so polling can no longer outlive the owning screen (H-9).
- Store the active iOS screen fetch as a cancellable `Task` guarded by
  an `NSLock`, cancel it on subsequent fetches, and cancel in `deinit`
  so in-flight responses no longer reference deallocated state (C-6).

--- Cross-platform main-thread budget ---

- Move iOS `SduiScreenViewModel` JSON work off `@MainActor` via
  `Task.detached(priority: .userInitiated)`. The fast path applies
  incoming Ably section updates by mutating `data` through a typed
  `DataClass` + `Section.with(data:)` value, keeping section value
  identity stable; the JSON merge stays as a fallback for paths the
  typed surface does not yet cover (C-5, UX-12).
- Replace `JSON.parse(JSON.stringify(...))` deep clones in web
  `DataBindingApplier` with a structural-sharing `setValueByPathImmutable`
  that only copies touched paths (and their array/object ancestors), so
  unchanged subtrees keep stable references for downstream `React.memo`
  (C-7, UX-25 parity).
- Slow Android `AtomicLiveClock` to 500 ms ticks (sufficient for MM:SS)
  and pause the loop when the composable is not visible (H-1).
- Add an in-memory cache for resolved web color tokens keyed by
  `(scheme, tokenName)` so `SectionContainer` and gradient backgrounds
  stop walking the palette/semantic maps on every render (H-12, M-4).
- Reuse the shared `ObjectMapper` in Android `AblyChannelManager` token
  parsing instead of constructing one per request (M-1).

--- Android list identity + recomposition ---

- Replace eager `forEach` row composition in `BoxscoreTableRenderer`
  and `SeasonLeadersTableRenderer` with `LazyColumn` + `itemsIndexed`
  keyed on player identity (C-1).
- Add `key` lambdas based on element identity to all four
  `AtomicScrollContainer` `itemsIndexed` call sites (C-2).
- Wrap each child of `TabGroupRenderer` and `FormRenderer` in
  `key(section.id)` / `key(field.stateKey)` so state no longer leaks
  across tab switches or form-field reorders (H-3).
- Memoize `BoxscoreTableRenderer`'s sort via `derivedStateOf` keyed on
  structural player identity instead of reference equality, so SSE
  updates that produce a new list reference but identical contents stop
  re-sorting (UX-8).
- Memoize per-leaf `bindRef` resolution in `AtomicText` and
  `AtomicImage` with `remember` keyed on the resolved bound value
  rather than the entire composite content map, so a single binding
  update no longer re-resolves every leaf in the section (UX-9).

--- iOS view identity + image stability ---

- Replace positional `id: \.offset` `ForEach` keys in `ScreenShell`,
  `AtomicScrollContainerView`, and `AtomicContainerView` with stable
  element identity so reorder/insert no longer destroys and recreates
  KFImage views (UX-11, H-8).
- Add `Equatable` conformance to `EquatableDisplayGrid`, `Column`, and
  `WidthUnion` so `AtomicDisplayGridView` stops re-evaluating on every
  body pass (M-3).
- Constrain `AtomicImageView`'s `KFImage` with a device-relative max
  frame and give the placeholder the image's `aspectRatio` background
  so oversized remote images no longer load at full resolution and
  content stops jumping when the image resolves (H-11, UX-15).
- Extract `BoxscoreTableView`'s team logo into a separate URL-keyed
  view so live data updates no longer reset its KFImage state (UX-17).
- Cache `SeasonLeadersTableView`'s sort in `@State` and update via
  `.onChange(of: sortKey)` so render-time sorting stops dropping
  frames on large rosters (UX-18).
- Initialize `FormSectionView`'s `@State` text from `screenState` in
  `init` instead of waiting for `onAppear`, removing the first-frame
  empty-text layout shift (M-2, UX-13).
- Add `.transition(.opacity)` + `withAnimation` to `ScreenShell`'s
  loading→loaded switch and `TabGroupView`'s tab content; wrap
  `NavCoordinator.path.append` in `withAnimation` (UX-14, UX-16).

--- Web re-render + transition fixes ---

- Reshape `useRefreshPolicy` to take stable scalar inputs (`sectionId`
  and the individual `refreshPolicy` fields) so the spread-section
  reference passed from `LiveSectionWrapper` no longer restarts polling
  or SSE every render (H-13).
- Wrap `AtomicContainer`, `AtomicText`, `AtomicButton`, and
  `AtomicImage` in `React.memo` with a shared `areAtomicPropsEqual`
  comparator so atomic primitives stop re-rendering on every parent
  prop change (H-16).
- Track the nested `setTimeout` ID in `useImpressionTracking`'s cleanup
  ref (and gate the dwell timer behind an `AbortController` token) so
  the inner timeout no longer fires on stale sections after unmount
  (H-15).
- Add `width`, `height`, and `loading="lazy"` to the `<img>` tags in
  `AtomicImage` and `BoxscoreTable` so the browser can reserve space
  before load and skip off-screen fetches (H-14, UX-26).
- Preserve the previous `screen` while a new fetch is in flight in
  `App.tsx`: navigation now overlays a spinner on the existing screen,
  same-screen updates merge into the existing `sections`, and errors
  surface as a banner instead of wiping the screen (UX-21).
- Add a CSS opacity transition on `TabGroup`'s tab content container
  so tab switches cross-fade rather than blank-frame (UX-22).
- Set `document.documentElement.setAttribute('data-theme', scheme)`
  synchronously in `setColorSchemePreference()` (and on first mount in
  `main.tsx`) so toggling color scheme no longer flashes the wrong
  palette before React commits (UX-23).
- Replace the raw-DOM toast in `ActionHandler.ts` with a module-level
  `ToastStore` + `ToastHost` portal subscribed via
  `useSyncExternalStore`, capping the visible queue and animating via
  CSS opacity transitions (UX-27).
- Add `willChange: 'transform'` to `SectionSkeleton`'s shimmer so the
  animation promotes to a compositor layer on slower devices (UX-28).
- Hold `Form.tsx` field state locally and propagate to `onStateChange`
  on debounce/blur so each keystroke no longer bubbles to `App.tsx`
  and re-renders every section (UX-3, UX-24).

--- Android image + transition polish ---

- Configure Coil's shared `ImageLoader` with a 25%-heap memory cache
  and a 100 MB disk cache so image decode/fetch survives configuration
  changes and rail repeats (UX-1).
- Decouple `AtomicImage`'s `currentSrc` and `triedFallback` `remember`
  blocks and sync via `LaunchedEffect` so an oscillating bound URL
  can no longer thrash the fallback flag into an infinite fetch loop
  (H-2, UX-2).
- Add `placeholder` and `error` painters to `SectionContainer`'s
  background `AsyncImage` so an oversized or failed background can no
  longer OOM or render blank (H-5).
- Refactor `SectionVisibilityTracker` to use `MutableSharedFlow` with
  `debounce()` for exits and immediate emission for entries inside a
  single `CoroutineScope`, replacing the `LaunchedEffect` + `delay()`
  loop that blocked the coroutine (H-6).
- Wrap `AtomicConditional` in `AnimatedContent(targetState =
  conditionMet)` so condition flips fade rather than pop (UX-4).
- Wrap `TabGroupRenderer` content and `SduiNavigationShell` screen
  changes in `AnimatedContent` with `slideInHorizontally` + `fadeIn`
  so tab swaps and navigation transition rather than cut (UX-5).
- Batch buffered SSE messages into a single state emission on
  foreground via `flushSseBufferBatched`, replacing the
  one-update-per-message cascade (UX-6).
- Cache empirically measured section heights in
  `SectionSkeletonHeightCache` and apply them as a min-height floor
  during skeleton rendering so the shimmer→content swap stops jumping
  when the server omits `minHeightDP` (UX-7).

--- Follow-up cleanups bundled into this commit ---

While verifying the iOS active-fetch cancellation work, two long-standing
papercuts surfaced; both are folded in here because they directly touch
files the audit modified and would otherwise leave the surface in a worse
state than before:

- Schema: rename the `error` state shape from `Error` to `ErrorState` so
  the generated client type stops shadowing each runtime's native error
  protocol (notably `Swift.Error`). The schema property name stays
  `error`; only the generated type name changes. All three clients
  regenerated; renderer code touches `sectionStates?.error` as a property
  and required no edits. The iOS `Swift.Error` qualifications on
  `SduiRepository`/`DataBindingApplier` were dropped now that `Error`
  resolves to the protocol again.
- iOS `SduiRepository`: replace the freshly-introduced
  `activeScreenFetchID: UUID` with the existing per-request
  `traceID` (the one already sent as `X-Trace-Id`). Lifting it from
  `attachAuthHeaders` into `fetchScreen` lets a single value drive both
  the header and the active-fetch identity, so the active-fetch slot
  reuses an established concept rather than inventing a parallel one.

--- Action-pipeline debug logging ---

Add gated debug logging to the action system on all three clients so
local-build sessions can visually verify that actions — especially
`fireAndForget`, which has no on-screen side effect — are actually
firing:

- iOS `ActionDispatcher` emits per-handler `os.Logger.debug(...)` lines
  with the resolved payload, including a "fired" / "deduped" log on the
  `onVisible` impression-tracker path that was previously silent. `os`
  unified logging keeps these out of release telemetry.
- Android introduces `SduiActionLogger`, gated on `BuildConfig.DEBUG`
  for `sdui-core` (`buildConfig = true` enabled), and routes
  per-handler logs through it under a single `SDUI/Action` tag.
  `Log.w` warning paths stay unconditional.
- Web introduces `runtime/actionLogger.ts`, gated on
  `import.meta.env.DEV`. Replaces the ad-hoc `[Action]` /
  `[FireAndForget]` `console.log`s with `actionLog`/`actionWarn`/
  `actionError` so the production bundle stays quiet.

All three loggers expose a runtime override (`enabled` / equivalent)
for hosts that need to capture telemetry from a release build during
dogfooding.

--- TraceId consistency across clients ---

Thread the screen-level `traceId` through data-binding and polling paths
so log correlation survives the full request lifecycle on all platforms:

- Web: pass `traceId` from the screen response through `SectionRouter`
  → `LiveSectionWrapper` → `applyDataBindings`. All `[DataBinding]`
  console logs now include a `trace=<id>` tag when available.
- Android: add `traceId` parameter to `SduiRepository.fetchRawJson()`
  and send it as the `X-Trace-Id` header so polling requests reuse the
  screen's trace context instead of generating a fresh one.
- iOS: add `traceID` parameter to `PollingDriver.start()` and pass it
  through to `SduiRepository.fetchRawJson()`. Update both call sites
  in `SduiScreenViewModel` (`startPolling` and `refresh`) to forward
  `screen?.traceID`.

--- Verification ---

Verified across all three platforms:

- `make codegen` — clean regen (Java, Kotlin, Swift, TS).
- Web: `npm --prefix web test` (9 files, 95 tests) +
  `npm --prefix web run build` — pass.
- Android: `./gradlew :sdui-core:test :app:compileDebugKotlin` —
  BUILD SUCCESSFUL.
- iOS: `make ios-test` (66 tests, 0 failures) + `make ios-build` —
  Build Succeeded.
