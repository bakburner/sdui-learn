# Plan: Section-Level SDUI Refresh

## Summary

Introduce proper ownership boundaries for refresh policy so that screen-level
periodic reloads live at the screen level and section-level refresh targets only
the section that needs it. Adds a `sectionEndpoint` field to `RefreshPolicy` so
a pre-game scoreboard section can poll the SDUI server for a re-composed section
and transition automatically to SSE when the game goes live — without reloading
the entire screen.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema — `section.refreshPolicy` poll + `url` (CDN data) | Built | Works on Android, iOS, web |
| Schema — `section.refreshPolicy` poll + no `url` (full-screen reload) | Built (wrong level) | All clients trigger full `applyScreen`; web logs warning and bails |
| Schema — `screen.defaultRefreshPolicy` | Partially built | Field exists in schema and is decoded on all clients; emitted by `DemoScreenComposer` only. Client runtimes do **not** act on it for screen-level periodic reloads yet. |
| Schema — `sectionEndpoint` field on `RefreshPolicy` | **Gap** | Not in schema |
| Server — section-level endpoint | **Gap** | No `/v1/sdui/section/{sectionId}` endpoint |
| Server — `screen.defaultRefreshPolicy` emitted for pre-game screens | **Gap** | Not emitted by game-detail / scoreboard composers |
| Android — `sectionEndpoint` poll path | **Gap** | Falls through to full-screen reload |
| Android — remove url-less section → full-screen-reload path | **Gap** | Should be moved to screen level |
| Android — screen-level `defaultRefreshPolicy` handler | **Gap** | Field is decoded but not acted upon |
| iOS — `sectionEndpoint` poll path | **Gap** | Falls through to full-screen reload via `PollingDriver.screenEndpoint` |
| iOS — remove url-less section → full-screen-reload path | **Gap** | Should be moved to screen level |
| iOS — screen-level `defaultRefreshPolicy` handler | **Gap** | Field is decoded but not acted upon |
| Web — `sectionEndpoint` poll path | **Gap** | Logs warning and returns false; no SDUI fetch at all |
| Web — screen-level `defaultRefreshPolicy` handler | **Gap** | `getEffectiveRefreshPolicy` threads the field through `LiveSectionWrapper` but no screen-level polling loop exists |

## Design

### Refresh policy ownership

| Scope | Field | `type` values | Purpose |
|-------|-------|--------------|---------|
| Screen | `screen.defaultRefreshPolicy` | `poll`, `static` | Re-fetch the whole screen on an interval |
| Section | `section.refreshPolicy` + `url` | `poll` | Fetch CDN/raw data → apply via `dataBinding` |
| Section | `section.refreshPolicy` + `sectionEndpoint` | `poll` | Fetch this section from SDUI → replace section + re-evaluate its policy |
| Section | `section.refreshPolicy` + `channel` | `sse` | Subscribe to Ably channel → `dataBinding` |

The url-less section poll path (currently: full-screen reload) is **removed** from
section-level handlers. Full-screen periodic reload moves entirely to
`screen.defaultRefreshPolicy`.

### Mutual exclusivity: screen refresh vs. sectionEndpoint

**`sectionEndpoint` section-level refresh and a non-static `screen.defaultRefreshPolicy`
MUST NOT appear on the same screen.** They are incompatible modes:

- When the screen has a non-static `defaultRefreshPolicy` (`poll`), the whole screen
  is periodically re-fetched. Every section — including the scoreboard — is replaced by
  the server's new composition. Adding per-section `sectionEndpoint` polls on top of
  this creates duplicate refreshes, conflicting lifecycle ownership, and confusing
  race conditions between the screen-level result and the section-level result arriving
  at different times.
- When sections need targeted replacement via `sectionEndpoint`, the screen must be
  declared `static` (no screen-level periodic reload). The sections that need it own
  their own refresh cycle independently.

Sections may still carry `url`-based direct polls (raw CDN data overlay) or SSE
subscriptions (`channel`) alongside a non-static screen `defaultRefreshPolicy` —
those are data overlays, not section replacements, and do not conflict.

**Server requirement:** composers that emit `sectionEndpoint` on any section MUST
set `screen.defaultRefreshPolicy.type = "static"`. `GameDetailComposer` is the
reference implementation of this rule.

**Client requirement:** if a client receives a screen where `defaultRefreshPolicy.type`
is non-static AND one or more sections carry `sectionEndpoint`, the client MUST:
1. Log a warning identifying the screen and the offending section IDs.
2. Skip the `sectionEndpoint` poll for those sections (do not start the coroutine /
   task / timeout).
3. Treat those sections as refreshed by the screen-level policy.

### Wire shape — `sectionEndpoint`

```json
"refreshPolicy": {
  "type": "poll",
  "sectionEndpoint": "/v1/sdui/section/stats-api:game-0022500123::AtomicComposite::scoreboard",
  "intervalMs": 300000,
  "pauseWhenOffScreen": true
}
```

- `sectionEndpoint` is a server-relative path. Clients resolve it against
  `SduiConfig.baseURL` using the same mechanism as `SduiRepository.fetchScreen(endpoint:)` —
  no new URL-building logic is required.
- When the server re-composes the section and the game is now live, it returns the
  section with `refreshPolicy.type: "sse"`. The client stops the poll and starts the
  Ably subscription for that section only.
- `url` and `sectionEndpoint` are mutually exclusive. Clients MUST treat a policy
  with both fields as a `sectionEndpoint` poll (precedence rule, not a decode error).
  The schema description must note this since JSON Schema cannot enforce XOR natively.

### Section endpoint contract

`GET /v1/sdui/section/{sectionId}` returns a **single `Section` JSON object** — the
same shape as one element of the `sections` array in a full screen response. It is
**not wrapped in a screen envelope**; this is a deliberate departure from every other
SDUI endpoint and must be documented at the endpoint and in this plan.

It accepts the standard SDUI envelope (platform, schemaVersion, locale) so the
server can compose correctly for the requesting client. Both `@GetMapping` and
`@PostMapping` are required per the dual-mount rule (AGENTS.md §4.1.1).

The section ID is stable and deterministic (produced by `SectionIdDeriver`), so
it can be embedded in the `refreshPolicy` at compose time.

### Section endpoint error semantics

Clients MUST handle non-2xx responses from the section endpoint as follows:

| Response | Client behaviour |
|----------|-----------------|
| 404 | Stop polling this section; mark it stale. Do **not** fall back to full-screen reload. |
| 5xx / network error | Apply exponential backoff (same as CDN poll failures); mark stale after `POLL_FAILURE_THRESHOLD` consecutive failures. |
| Schema version mismatch (`X-Schema-Version` disagreement) | Treat as a 5xx for retry purposes; log at warning level. |
| New section has no `refreshPolicy` | Stop the poll; section is now static. No further action. |

### `SectionRefreshService` routing

`SectionRefreshService` routes incoming section IDs to composer methods. Section IDs
follow the `SectionIdDeriver` format: `contentSourceId::sectionType[::slug]`. The
routing strategy is a **content-source-type registry** — a map from content-source
prefix pattern to a resolver lambda:

```
"stats-api:game-*"  → GameDetailComposer::refreshSection(gameId, sectionType, slug, context)
"cms:article-*"     → ContentComposer::refreshSection(articleId, sectionType, slug, context)
```

The service extracts the content-source ID from the section ID, matches it against
the registry, and invokes the resolver. If no prefix matches, the endpoint returns
**404** (not a 500) — the section ID is unknown to the server, which is a valid
content-lifecycle outcome (e.g., game ended).

Composers must not be aware of the routing registry; each registers a resolver
lambda at startup (e.g., via `@PostConstruct`). The registry is owned by
`SectionRefreshService`.

### `restartRealtimeForSection` — scoped teardown

The existing `restartRealtime(for screen:)` tears down **all** subscriptions.
Phase 3–4 require a targeted variant that only touches one section's entries:

- Cancel / remove the poll job / task for `sectionId` from the job/task map.
- Cancel / remove the Ably task for `sectionId` if present.
- Remove `sectionId` from `sectionAblyChannels` / `alwaysRefreshSections`.
- Start the new section's policy (poll or SSE) using the existing `startPolling` /
  `startSSE` helpers.
- Do **not** touch any other section's subscription.

This must be implemented as an explicit helper on both Android and iOS before the
`sectionEndpoint` poll handler calls it. Inline ad-hoc teardown is prohibited.

> **Ordering constraint:** the current poll job for a section MUST be cancelled
> **before** the new section is merged into `screen.sections`. A tick firing on the
> old policy after the merge would call `updateSectionData` on a section that now
> has a different (potentially SSE) policy, producing stale data.

### Web — policy re-evaluation via `key` remount

React hook teardown is driven by component unmount, not by calling a function.
When a section's `refreshPolicy` changes (e.g., poll → SSE after a `sectionEndpoint`
fetch), `LiveSectionWrapper` must receive a new `key` prop so React unmounts and
remounts the component, tearing down the old `useRefreshPolicy` instance and starting
a fresh one with the new policy.

The `key` should be derived from a stable policy fingerprint, e.g.:

```ts
key={`${section.id}::${section.refreshPolicy?.type ?? 'static'}::${section.refreshPolicy?.channel ?? section.refreshPolicy?.sectionEndpoint ?? ''}`}
```

Without a `key` change, the old poll timer keeps running alongside the new SSE
subscription.

### Screen-level poll lifecycle

When `applyScreen` / `loadScreen` is called again (pull-to-refresh, navigation
re-entry), the screen-level poll coroutine / timer from the previous load MUST be
cancelled before starting a new one. Failure to cancel it leaves two competing
poll loops running against the same endpoint.

### `paramBindings` — boundary with action-triggered refresh

`sectionEndpoint` (this plan) and the `paramBindings` field on a `refresh` action
are distinct mechanisms and must not be confused:

- `sectionEndpoint` — **client-timer-driven**; the client decides when to fetch,
  using `intervalMs` and `pauseWhenOffScreen` gating.
- `paramBindings` on a `refresh` action — **server-action-triggered**; the server
  declares when to fetch, in response to a user interaction or an action sequence.

Both use `SduiRepository.fetchSection` / `fetchScreen` under the hood, but the
triggering ownership is different. Do not conflate them during implementation.

### iOS `PollEvent` enum impact

iOS `PollingDriver` currently yields `.success(PollSuccess(..., isDirect: Bool))`.
Adding `sectionSuccess` requires a new associated-value case on `PollEvent`:

```swift
case sectionSuccess(sectionID: String, section: Section)
```

All `switch` statements on `PollEvent` in `SduiScreenViewModel` must be updated,
including the existing `isDirect: false` (full-screen) case which this plan removes.
Exhaustive switch checking will catch any missed sites at compile time.

## Tasks

### Phase 1 — Schema

- [x] Add `sectionEndpoint` (`type: string`) to `RefreshPolicy` definition in
      `schema/sdui-schema.json`. Add a description noting it is mutually exclusive
      with `url`; `sectionEndpoint` takes precedence when both are present.
- [x] Update `url` field description — remove "If omitted, polls the SDUI endpoint"
      (that behaviour is being removed)
- [x] Run `make codegen` — regenerates Java, Kotlin, Swift, TypeScript models

### Phase 2 — Server

- [x] Add `GET` + `POST /v1/sdui/section/{sectionId}` to `SduiController.java`
  - Accepts standard `SduiRequestContext` envelope
  - Delegates to `SectionRefreshService` (see routing design above)
  - Returns a single `Section` JSON node — **not** wrapped in a screen envelope
  - 404 when no prefix matches; document this explicitly at the handler
- [x] Implement `SectionRefreshService` with a content-source-prefix registry.
  `GameDetailComposer` registers its resolver at startup. Returns 404 (not 500) for
  unknown section IDs.
- [x] `GameDetailComposer`: pre-game scoreboard `refreshPolicy` emits
  `sectionEndpoint` instead of url-less poll:
  ```java
  refreshPolicy.put("type", "poll");
  refreshPolicy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
  refreshPolicy.put("intervalMs", 300_000);
  refreshPolicy.put("pauseWhenOffScreen", true);
  ```
- [x] `GameDetailComposer`: emit `screen.defaultRefreshPolicy` with
  `type: static` for live/post games (no screen-level polling needed when SSE
  handles the section)
- [x] Any screen that previously relied on url-less section polls for full-screen
  refresh: emit `screen.defaultRefreshPolicy` with `type: poll` + `intervalMs`
  instead — N/A; no other screen relied on url-less section polls

### Phase 3 — Android

- [x] `SduiRepository`: add `fetchSection(path, envelope)` → returns a single
      decoded `Section` object
- [x] Add `restartRealtimeForSection(sectionId)` to `SduiScreenViewModel` —
      cancels the existing poll/SSE job for that section only, then starts the new
      section's policy. Must cancel the old job **before** merging the new section
      into `screen.sections` (see ordering constraint above).
- [x] `SduiScreenViewModel.setupPolling`: when `policy.sectionEndpoint != null`:
  1. Fetch `sectionEndpoint` via `repository.fetchSection()`
  2. Call `restartRealtimeForSection(sectionId)` before merging
  3. Call `refreshSections(listOf(newSection))` — merges by ID, replaces in place
  4. The new section's policy is started inside `restartRealtimeForSection`
  - Apply error semantics from the table above (404 → stop; 5xx → backoff + stale)
- [x] `SduiScreenViewModel.setupPolling`: **remove** the url-less →
      `fetchScreen` → `applyScreen` fallback path from the section poll loop
- [x] `SduiScreenViewModel.applyScreen`: add handler for
      `screen.defaultRefreshPolicy` — start a screen-level poll coroutine that calls
      `loadFromEndpoint(currentEndpoint)` on the configured interval. Cancel any
      existing screen-level poll coroutine before starting a new one.
- [ ] Add unit tests:
  - `sectionEndpoint` happy-path poll and section replacement
  - Poll → SSE policy transition
  - 404 response → section marked stale, polling stopped
  - 5xx response → exponential backoff, stale after threshold
  - `applyScreen` called again (pull-to-refresh) cancels and restarts screen-level poll

### Phase 4 — iOS

- [x] `SduiRepository`: add `fetchSection(path:, envelope:)` → returns a decoded
      `Section`
- [x] Add `PollEvent.sectionSuccess(sectionID: String, section: Section)` case to
      `PollingDriver`. Update all exhaustive `switch` statements on `PollEvent` in
      `SduiScreenViewModel` — including the `isDirect: false` / `screenEndpoint` case
      that this plan removes.
- [x] `PollingDriver.start(...)`: add `sectionEndpoint: String?` parameter. When
      set, fetch via `repository.fetchSection()` and yield `.sectionSuccess`; remove
      the `screenEndpoint` / full-screen fallback path from `PollingDriver`.
- [x] Add `restartRealtimeForSection(_ sectionID: String)` to
      `SduiScreenViewModel` — cancels existing poll/SSE task for that section only,
      then starts the new policy. Cancel before merging (see ordering constraint).
- [x] `SduiScreenViewModel.handlePollEvent`: handle `.sectionSuccess` —
      call `restartRealtimeForSection` before merging, then replace section in
      `screen.sections`. Apply error semantics (404 → stop; 5xx → backoff + stale).
- [x] `SduiScreenViewModel.startPolling`: remove `screenEndpoint` parameter.
- [x] `SduiScreenViewModel.applyScreen`: handle `screen.defaultRefreshPolicy`
      with `type: poll` — start a screen-level `Task` that calls `load()` on the
      configured interval. Cancel any existing screen-level task before starting.
- [ ] Add tests in `SduiCoreTests`:
  - `sectionEndpoint` poll and section replacement
  - Poll → SSE policy transition
  - 404 response → stale, poll stopped
  - 5xx / backoff behaviour
  - `PollEvent` switch exhaustiveness (compile-time, but note in test plan)

### Phase 5 — Web

- [x] `SduiRepository` (web): add `fetchSection(path, envelope)` → returns a
      `Section` object
- [x] `useRefreshPolicy`:
  - When `policy.sectionEndpoint` is set: fetch via `fetchSection`, call a new
    `onSectionReplace(section)` callback instead of `onUpdate(data)`
  - **Remove** the `!pollUrl → warn + return false` path entirely
  - Update hook signature: `onSectionReplace?: (section: Section) => void`
  - Apply error semantics (404 → stop polling; 5xx → backoff + stale)
- [x] `LiveSectionWrapper` / `SectionRouter`: derive a policy fingerprint `key` for
      each section so React remounts `LiveSectionWrapper` when the policy changes
      (e.g., poll → SSE). Key formula: `{sectionId}::{type}::{channel|sectionEndpoint|''}`.
      Without this, the old poll timer runs alongside the new subscription.
- [x] `useSduiScreen`: handle `onSectionReplace` — merge section by ID into screen
      state, then re-key the section component to trigger remount of `useRefreshPolicy`.
- [x] `useSduiScreen`: add handler for `screen.defaultRefreshPolicy` with
      `type: poll` — `setInterval` that calls `loadScreen(endpoint)` and replaces
      screen state. Clear the interval in cleanup and whenever `applyScreen` reruns
      (pull-to-refresh, navigation).
- [ ] Update `useRefreshPolicy.test.ts`:
  - ~~Add `sectionEndpoint` happy-path and policy-transition cases~~ (precedence test done)
  - Add 404 and 5xx error-semantics cases
  - Remove the test that asserts the no-url warning behaviour

## Out of Scope

- Section-level SSE policy transition without a poll (e.g., an Ably meta-channel
  that notifies clients when a game starts) — future work
- `sectionRefresh` as a separate enum value — explicitly rejected; `poll` +
  `sectionEndpoint` is sufficient and avoids enum growth
- Caching section responses — covered by `plan-caching-offline.md`
- `paramBindings` on `refresh` actions — action-triggered server fetches, separate
  from this plan's timer-driven `sectionEndpoint` mechanism (see boundary note above)
