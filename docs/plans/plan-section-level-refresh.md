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
| Schema — `screen.defaultRefreshPolicy` | Built | Field exists in schema; not emitted by any composer yet |
| Schema — `sectionEndpoint` field on `RefreshPolicy` | **Gap** | Not in schema |
| Server — section-level endpoint | **Gap** | No `/v1/sdui/section/{sectionId}` endpoint |
| Server — `screen.defaultRefreshPolicy` emitted for pre-game screens | **Gap** | Not emitted |
| Android — `sectionEndpoint` poll path | **Gap** | Falls through to full-screen reload |
| Android — remove url-less section → full-screen-reload path | **Gap** | Should be moved to screen level |
| iOS — `sectionEndpoint` poll path | **Gap** | Falls through to full-screen reload |
| iOS — remove url-less section → full-screen-reload path | **Gap** | Should be moved to screen level |
| Web — `sectionEndpoint` poll path | **Gap** | Logs warning and returns false; no SDUI fetch at all |
| Web — screen-level `defaultRefreshPolicy` handler | **Gap** | No screen-level periodic reload in web runtime |

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

### Wire shape — `sectionEndpoint`

```json
"refreshPolicy": {
  "type": "poll",
  "sectionEndpoint": "/v1/sdui/section/stats-api:game-0022500123::AtomicComposite::scoreboard",
  "intervalMs": 300000,
  "pauseWhenOffScreen": true
}
```

- `sectionEndpoint` is a server-relative path. Clients resolve it against their
  configured base URL the same way they resolve any other SDUI path.
- When the server re-composes the section and the game is now live, it returns the
  section with `type: "sse"`. The client stops the poll and starts the Ably
  subscription for that section only.
- `url` and `sectionEndpoint` are mutually exclusive. Clients MUST treat a policy
  with both fields as a `sectionEndpoint` poll.

### Section endpoint contract

`GET /v1/sdui/section/{sectionId}` returns a single `Section` JSON object — the
same shape as one element of the `sections` array in a full screen response. It
accepts the standard SDUI envelope (platform, schemaVersion, locale) so the
server can compose correctly for the requesting client.

The section ID is stable and deterministic (produced by `SectionIdDeriver`), so
it can be embedded in the `refreshPolicy` at compose time.

## Tasks

### Phase 1 — Schema

- [ ] Add `sectionEndpoint` (`type: string`) to `RefreshPolicy` definition in
      `schema/sdui-schema.json`
- [ ] Update `url` field description — remove "If omitted, polls the SDUI
      endpoint" (that behaviour is being removed)
- [ ] Run `make codegen` — regenerates Java, Kotlin, Swift, TypeScript models

### Phase 2 — Server

- [ ] Add `GET` + `POST /v1/sdui/section/{sectionId}` to `SduiController.java`
  - Accepts standard `SduiRequestContext` envelope
  - Delegates to a new `SectionRefreshService` that routes by section ID prefix
    to the correct composer method (e.g. `stats-api:game-*::*::scoreboard` →
    `GameDetailComposer.buildGamePanelScoreboardFromLive`)
  - Returns a single `Section` JSON node (not wrapped in a screen envelope)
- [ ] `GameDetailComposer`: pre-game scoreboard `refreshPolicy` emits
  `sectionEndpoint` instead of url-less poll
  ```java
  refreshPolicy.put("type", "poll");
  refreshPolicy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
  refreshPolicy.put("intervalMs", 300_000);
  refreshPolicy.put("pauseWhenOffScreen", true);
  ```
- [ ] `GameDetailComposer`: emit `screen.defaultRefreshPolicy` with
  `type: static` for live/post games (no screen-level polling needed when SSE
  handles the section)
- [ ] Any screen that previously relied on url-less section polls for full-screen
  refresh: emit `screen.defaultRefreshPolicy` with `type: poll` + `intervalMs`
  instead

### Phase 3 — Android

- [ ] `SduiRepository`: add `fetchSection(path, envelope)` → returns a single
      decoded `Section` object
- [ ] `SduiScreenViewModel.setupPolling`: when `policy.sectionEndpoint != null`:
  1. Fetch `sectionEndpoint` via `repository.fetchSection()`
  2. Call `refreshSections(listOf(newSection))` — merges by ID, replaces in place
  3. Re-run `startPolling` / `startSSE` for the **new** section's policy only
  4. Cancel the current poll job for this section (it will be replaced by the new
     policy's job)
- [ ] `SduiScreenViewModel.setupPolling`: **remove** the url-less →
      `fetchScreen` → `applyScreen` fallback path from the section poll loop
- [ ] `SduiScreenViewModel.applyScreen`: add handler for
      `screen.defaultRefreshPolicy` — start a screen-level poll coroutine that
      calls `loadFromEndpoint(currentEndpoint)` on the configured interval
- [ ] Add unit tests for the `sectionEndpoint` poll path and the policy
      transition (poll → SSE after section refresh)

### Phase 4 — iOS

- [ ] `SduiRepository`: add `fetchSection(path:, envelope:)` → returns a decoded
      `Section`
- [ ] `PollingDriver.start(...)`: add `sectionEndpoint: String?` parameter
- [ ] `PollingDriver` poll task: when `sectionEndpoint` is set, fetch section via
      `repository.fetchSection()`; yield a new `PollEvent.sectionSuccess` variant
      carrying the `Section` value
- [ ] `SduiScreenViewModel.handlePollEvent`: handle `sectionSuccess` — replace
      section in `screen.sections`, call `restartRealtime(for:)` **scoped** to
      that section only (stop old poll / sse for the section, start new policy)
- [ ] `SduiScreenViewModel.startPolling`: **remove** `screenEndpoint` parameter
      and the non-direct → full-screen path from `PollingDriver`
- [ ] `SduiScreenViewModel.applyScreen`: handle `screen.defaultRefreshPolicy`
      with `type: poll` — start a screen-level `Task` that calls `load()` on the
      configured interval
- [ ] Add tests in `SduiCoreTests` for section replacement and policy transition

### Phase 5 — Web

- [ ] `SduiRepository` (web): add `fetchSection(path, envelope)` → returns a
      `Section` object
- [ ] `useRefreshPolicy`:
  - When `policy.sectionEndpoint` is set: fetch section, call a new
    `onSectionReplace(section)` callback instead of `onUpdate(data)`
  - **Remove** the `!pollUrl → warn + return false` path entirely
  - Update hook signature to accept `onSectionReplace?: (section: Section) => void`
- [ ] Screen store / `useSduiScreen`: handle `onSectionReplace` — merge section
      by ID into screen state, then re-evaluate the new section's refresh policy
      (unmount old `useRefreshPolicy` instance, mount new one with updated policy)
- [ ] `useSduiScreen`: add handler for `screen.defaultRefreshPolicy` with
      `type: poll` — `setInterval` that calls `loadScreen(endpoint)` and replaces
      screen state
- [ ] Update `useRefreshPolicy.test.ts` — add `sectionEndpoint` cases; remove
      the existing test that asserts the no-url warning behaviour

## Out of Scope

- Section-level SSE policy transition without a poll (e.g., an Ably meta-channel
  that notifies clients when a game starts) — future work
- `sectionRefresh` as a separate enum value — explicitly rejected; `poll` +
  `sectionEndpoint` is sufficient and avoids enum growth
- Caching section responses — covered by `plan-caching-offline.md`
