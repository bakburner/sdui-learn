# Plan: Update Channel Unification

> Source: post-mortem of the May 2026 games-screen calendar-strip refresh bug
> (see `docs/games-screen-spec.md`). No existing requirement section codified
> this; this plan establishes the architectural rule and aligns code to it.

## Summary

Collapse the redundant `/v1/sdui/screen/refresh/{screenId}` URL family into the
existing `/v1/sdui/screen/{screenId}` family. Establish a single, explicit
architectural rule for how SDUI screens update:

- **Screen channel** (`/v1/sdui/screen/{id}` ± query params) — always returns
  a complete `Screen`, always fully replaces the current screen on the
  client. Covers initial loads, navigation, pull-to-refresh, screen-level
  polling, form re-composition, and parameterized re-composition (date
  pickers, filters).
- **Section channel** (`/v1/sdui/section/{id}` and SSE) — returns a single
  `Section` (or a `dataBinding` patch). The client replaces that one section
  in place; everything else is structurally untouched.

There is no third channel and no "partial screen response" shape. Structural
changes to a screen's section list (insertions, removals, reorderings) flow
through the screen channel at whatever cadence the screen needs (poll,
manual, or SSE-nudged immediate refetch).

This unification turns the implicit, accidental contract that caused the
date-picker stale-section bug into an explicit, codified rule.

## Motivation

The old split between `/screen/{id}` and `/screen/refresh/{id}` was
behaviorally identical on the server but seeded two different client code
paths (`load` vs `refreshSections`). The latter inherited a
merge-and-preserve semantic intended for partial form fragments. When the
games-screen date picker dispatched a `refresh` action, it routed through
that merge path and inherited the wrong semantic, leaving stale sections on
the new date. Fixed tactically by gating on `response.id` match, but the
underlying URL/channel split is still redundant and remains a footgun. This
plan removes the footgun.

## Current State

| Aspect | Status | Notes |
|---|---|---|
| Schema support | Built | Actions already specify `endpoint` as a free-form path string. No schema change required. |
| Server URL routing | Partial | Two URL families (`/screen/{id}` and `/screen/refresh/{id}`) serve identical full-`Screen` responses for `games`, `leaders`, etc. |
| Server composers | Partial | `LiveComposer` and `DemoScreenComposer` were normalized in the May 2026 fix to return full screens with matching `id`. No partial fragments remain. |
| `ParameterizedRefreshService` | Built (but redundant) | Registry that maps `screenId → resolver`. Currently only invoked by `/refresh/` routes. Can be folded into the regular screen routes or removed entirely. |
| Android client | Partial | `refreshSections` now strict same-id full-replace. Name is misleading (the method is screen replacement, not section merging). |
| iOS client | Drift | Still runs the old merge-and-fallback path: `mergeRefreshedScreen` in `SduiScreenViewModel.swift:241` does a surgical section swap when a target section id is supplied and only falls back to `applyScreen` when it isn't. The strict same-id full-replace fix that landed on Android in May 2026 has **not** landed here yet. This plan must land it. |
| Web client | Partial | Same situation. |
| Docs | Partial | `docs/games-screen-spec.md` documents the strict refresh contract for the games screen. `AGENTS.md` does not yet codify the global rule. |
| Tests | Partial | Server + client tests cover the strict-id behavior. URL strings still reference `/refresh/`. |

## Decisions

These were open questions on first draft; resolved before execution:

- **Keep `ParameterizedRefreshService`.** It remains the single source of
  truth for "screen id ↔ composer with user params". The unified
  `/v1/sdui/screen/{id}` route calls into the registry whenever query params
  are present.
- **Rename the client screen-refetch method.** `refreshSections` →
  `replaceCurrentScreen` on all three clients. The old name actively
  misleads about the semantic (it's a full screen replace, not a section
  merge).
- **No new action types.** `type: refresh` remains the only refetch action.
  The URL shape (`/screen/{id}` vs `/section/{id}`) disambiguates channel.
  Adding `replaceScreen` / `refreshSection` action types would fragment the
  variant discipline rule in `AGENTS.md` for zero net benefit.
- **Hard cut, no deprecation window for `/v1/sdui/screen/refresh/{id}`.**
  The route is removed in the same change that introduces query-param
  support on `/v1/sdui/screen/{id}`. No alias, no warning period.

## Requirements Addressed

- [ ] **REQ-1**: A parameterized screen re-composition (date picker, filter,
      form submit) returns a full `Screen` with id matching the current
      screen and is applied via full replacement.
- [ ] **REQ-2**: There is exactly one URL family for screen fetches:
      `/v1/sdui/screen/{id}` with optional query params for user-supplied
      filter state.
- [ ] **REQ-3**: There is exactly one URL family for section-scoped updates:
      `/v1/sdui/section/{id}` and SSE channels declared in section
      `refreshPolicy`.
- [ ] **REQ-4**: No endpoint returns a "partial screen" shape. Structural
      changes to the section list flow through the screen channel only.
- [ ] **REQ-5**: Schema action `type: refresh` with a screen-shaped endpoint
      means "re-fetch this screen and full-replace"; with a section-shaped
      endpoint means "re-fetch this section in place".
- [ ] **REQ-6**: Client code paths reflect the channel boundary: one method
      for screen fetches (full replace), one for section fetches (single
      section in place).
- [ ] **REQ-7**: Pull-to-refresh, screen-level poll ticks, and parameterized
      re-composition all flow through the same screen-channel fetch carrying
      the current screen's query params. The user's parameterization
      (selected date, filter values, etc.) survives every screen refetch
      until the user explicitly changes it.
- [ ] **REQ-8**: A successful screen-channel fetch resets the screen-level
      poll timer so out-of-band fetches (pull-to-refresh, action-driven
      refresh) don't cause a double-fetch one tick later.

## Tasks

### Phase 1 — Server (URL collapse)

- [ ] Add query-param support to every existing `/v1/sdui/screen/{id}` route
      that currently has a sibling `/refresh/{id}` resolver. Routes to touch
      in `server/src/main/java/com/nba/sdui/controller/SduiController.java`:
  - `getGames`/`postGames` — accept `date`, pass through to
    `compositionService.composeLive(ctx, userParams)`.
  - `getLeaders`/`postLeaders` — accept `season`, `seasonType`, `perMode`,
    `statCategory`, pass through to
    `compositionService.composeLeaders(ctx, userParams)`.
- [ ] Update `SduiCompositionService` and the underlying composers
      (`LiveComposer`, `DemoScreenComposer`) so initial-load and parameterized
      paths share one entry point per screen — already partially done; verify
      no duplication remains.
- [ ] Wire the unified `/screen/{id}` route through
      `ParameterizedRefreshService` when query params are present so the
      resolver registry stays the single source of truth for "compose this
      screen with these params".
- [ ] **Remove** `/v1/sdui/screen/refresh/{screenId}` from
      `SduiController.java`. Specifically delete:
  - `@GetMapping` at `SduiController.java:503` (`getRefreshScreen`).
  - `@PostMapping` at `SduiController.java:513`.
  - The shared `private refreshScreen(…)` helper at `SduiController.java:523`
    if no other route uses it after the cut.
  Hard cut — no alias, no deprecation window. `ParameterizedRefreshService`
  itself stays (per Decisions); only the URL family disappears, and the
  unified `/screen/{id}` route now calls
  `ParameterizedRefreshService.refreshScreen` when query params are
  present.
- [ ] Update `server/src/test/java/com/nba/sdui/controller/SduiRefreshTransportTest.java`
      to hit the unified URL. Delete any test cases that asserted behavior
      on the removed route.
- [ ] Update `server/src/test/java/com/nba/sdui/service/LiveComposerTest.java`
      assertions for the CalendarStrip `onDateSelected.endpoint` value
      (`/v1/sdui/screen/games`).
- [ ] Update `server/src/test/java/com/nba/sdui/service/DemoScreenComposerTest.java`
      (if it asserts the form submit endpoint).

### Phase 2 — Server (action endpoints in composers)

- [ ] `LiveComposer.java:277` — change `onDateSelected.endpoint` from
      `"/v1/sdui/screen/refresh/games"` to `"/v1/sdui/screen/games"`.
- [ ] `DemoScreenComposer.java:680` — change the leaders form's submit
      `endpoint` from `"/v1/sdui/screen/refresh/stats-leaders"` to
      `"/v1/sdui/screen/leaders"` (the actual route id, not the legacy
      `stats-leaders` slug if those diverge; verify against the controller
      route map).
- [ ] Audit every other action emitted by every composer for hardcoded
      `/screen/refresh/` strings. Update each.
- [ ] Update `schema/examples/calendar-strip.json` to reflect the new
      endpoint.

### Phase 2.5 — Server contract tests (the durable enforcement layer)

The server is the canonical authority for the channel contract. Client
tests catch regressions in client behavior, but the *contract itself* lives
or dies in server tests. Add a dedicated channel-contract suite that holds
every screen and section endpoint to the rules and acts as a regression
fence for any future endpoint added to the system.

- [ ] **New test class** `server/src/test/java/com/nba/sdui/contract/ScreenChannelContractTest.java`:
  - For every screen id registered in `ParameterizedRefreshService`,
    `GET /v1/sdui/screen/{id}` returns HTTP 200, body decodes as a
    `Screen`, and `body.id == requestedId`.
  - The same call with representative query params (e.g. `?date=…`,
    `?season=…`) still returns a `Screen` with `body.id == requestedId`
    and an `AtomicComposite`/section roster reflecting the param.
  - No screen endpoint ever returns a body whose top-level shape is a
    bare `Section` or a list. (Pin the wire shape, not just the type.)
  - For each screen, walk every emitted action of `type: refresh` and
    assert the `endpoint` string matches the regex
    `^/v1/sdui/(screen|section)/[a-z0-9-]+(\?.*)?$` — never
    `/screen/refresh/…`. This is the **regression fence** that catches any
    composer slipping the legacy URL back in.
  - For each screen with parameterizable filters (games, leaders), assert
    the screen's emitted action endpoints carry forward the current query
    params so pull-to-refresh / poll / action-driven refresh replay
    correctly. Concretely: fetch `/v1/sdui/screen/games?date=2026-05-18`,
    then inspect the CalendarStrip's `onDateSelected.endpoint` and any
    `defaultRefreshPolicy` URL — both must reference `date=2026-05-18` (or
    have a documented mechanism for the client to supply it).
- [ ] **New test class** `server/src/test/java/com/nba/sdui/contract/SectionChannelContractTest.java`:
  - For every section id that the system serves via
    `GET /v1/sdui/section/{id}`, the response decodes as a single
    `Section` with `body.id == requestedId`, never as a `Screen`, never
    as a list.
  - Section responses never carry top-level `Screen` fields (`title`,
    `defaultRefreshPolicy`, `sections`, etc.).
- [ ] **Negative test in `SduiRefreshTransportTest.java`**:
  `GET /v1/sdui/screen/refresh/games` returns HTTP 404 (or whatever
  Spring's default for an unmapped path is). This pins the hard cut and
  prevents the route from being silently re-introduced.
- [ ] **Composer round-trip test** (in `LiveComposerTest.java` and
  `DemoScreenComposerTest.java`): fetch the screen with a param, take the
  emitted action endpoint string verbatim, feed it back through the
  controller, and assert the resulting screen has the same param applied
  (e.g. the CalendarStrip's `selectedDate` matches). This is the
  end-to-end version of the param-replay invariant and the test that
  would have caught the May 2026 calendar-strip bug at the server layer.
- [ ] **Static regression guard** (cheap): a unit test that scans
  `server/src/main/java/com/nba/sdui/service/*Composer.java` files for the
  literal substring `screen/refresh/` and fails if any match. This is a
  belt-and-suspenders backstop against the contract test missing a code
  path.
- [ ] **`ParameterizedRefreshService` invocation test**: when
  `/v1/sdui/screen/{id}` is called with query params, the service is
  consulted; when called without params, the regular composer entry point
  is used. Pin the routing so the registry's role is unambiguous.
- [ ] **`mutate`-then-`refresh` audit test**: walk every composer's
  emitted action sequences and assert no chain does `mutate` followed by
  a `refresh` whose endpoint would have relied on the legacy partial-
  screen merge behavior. If any do, they need to be promoted to a full
  screen refetch.

### Phase 3 — Android client

- [ ] `android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenController.kt`
      — rename `refreshSections` → `replaceCurrentScreen`. The body is
      already correct after the May 2026 fix; this is a name change plus
      Javadoc update.
- [ ] `android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt`
      — update the public delegate name.
- [ ] `android/app/src/main/java/com/nba/sdui/app/ui/GameDetailScreen.kt`
      and any other call sites — update to the new name.
- [ ] Confirm the controller remembers the current screen's query params and
      replays them on pull-to-refresh, screen-level poll, and any
      action-driven `refresh` that targets the current screen. The date
      picker path already proves this works; the audit is to make sure
      pull-to-refresh and poll-tick paths use the same mechanism rather
      than re-fetching with empty params.
- [ ] Reset the screen-level poll timer on every successful
      `replaceCurrentScreen` so pull-to-refresh and action-driven refreshes
      don't cause a double-fetch one tick later.
- [ ] Tests:
  - `SduiScreenControllerTest.kt` — update method name and the
    `REFRESH_PATH` constant. Add coverage for (a) pull-to-refresh
    preserves current query params, (b) successful refetch resets the
    poll timer.
  - `SduiRepositoryRefreshTransportTest.kt` — update URLs.
  - `CalendarStripAdapterTest.kt` — update expected endpoint.
  - `CalendarStripSchemaDecodeTest.kt` — update fixture endpoint.

### Phase 4 — iOS client

iOS still runs the legacy merge-and-fallback path, so this phase does more
than a rename — it lands the strict same-id full-replace contract that
Android already has.

- [ ] `ios/Sources/SduiCore/Runtime/SduiScreenViewModel.swift`:
  - Replace `mergeRefreshedScreen(_:targetSectionID:)` at line 241 with
    a strict same-id full-replace path. If `refreshed.id == screen?.id`,
    call `applyScreen(refreshed)`; otherwise log a warning and drop the
    response (server contract violation). The `targetSectionID`-based
    surgical-swap branch goes away — section-scoped updates flow through
    the section channel, not through this method.
  - Rename the `refresh(sectionID:endpoint:resolvedParams:)` entry point
    so its public name reflects the new behavior. Suggested: split into
    `replaceCurrentScreen(endpoint:userParams:)` (screen channel) and
    `replaceSection(sectionID:endpoint:userParams:)` (section channel),
    each calling the appropriate transport. The `ActionDispatcher`
    `refreshHandler` closure dispatches to one or the other based on the
    endpoint shape.
  - Update the docstring at line 240 (the "Mirrors Android's
    `refreshSections` merge semantics" comment) to reflect the new
    contract.
- [ ] `ios/Sources/SduiCore/Network/SduiRepository.swift` — verify it
      exposes one screen-fetch primitive and one section-fetch primitive,
      both keyed off endpoint string only. No `/refresh/`-shaped method
      names.
- [ ] `ActionDispatcher` (wherever the closure is wired) — route the
      `refreshHandler` to the screen-channel or section-channel call
      based on whether the endpoint matches `/v1/sdui/screen/…` or
      `/v1/sdui/section/…`.
- [ ] Update any call sites in `ios/App/` (or wherever the host app lives)
      to the new method names.
- [ ] Remember the current screen's query params and replay them on
      pull-to-refresh, screen-level poll, and action-driven `refresh`
      targeting the current screen. Same audit as Android.
- [ ] Reset the screen-level poll timer (`screenLevelPollTask` at
      `SduiScreenViewModel.swift:278`) on every successful
      `replaceCurrentScreen`.
- [ ] Tests:
  - `SduiScreenViewModelRefreshTests.swift` — replace the existing
    merge-semantics expectations with strict full-replace expectations.
    Add: (a) screen-channel response with matching id triggers full
    replace and drops sections omitted from the new payload; (b)
    screen-channel response with mismatched id is dropped with a warning;
    (c) section-channel response replaces only that section in place; (d)
    pull-to-refresh preserves current query params; (e) successful
    refetch resets the poll timer.
  - `SduiRepositoryRefreshTransportTests.swift` — update URLs.
  - `CalendarStripSchemaDecodingTests.swift` — update fixture endpoint.
  - `Fixtures/calendar-strip.json` — update `onDateSelected.endpoint`.

### Phase 5 — Web client

- [ ] `web/src/hooks/useSduiScreen.ts` — rename `refreshSections` →
      `replaceCurrentScreen` and update the JSDoc.
- [ ] Update any consumer components that call the hook's old method name.
- [ ] Confirm the hook (or whatever holds screen state) remembers the
      current screen's query params and replays them on pull-to-refresh,
      screen-level poll, and action-driven `refresh`. Same audit as Android
      and iOS.
- [ ] Reset the screen-level poll timer on every successful
      `replaceCurrentScreen`.
- [ ] Tests:
  - `web/src/runtime/fetchSduiScreen.test.ts` — add coverage matching the
    other clients' pull-to-refresh + poll-timer-reset cases.
  - `web/src/adapters/sectionUiAdapters.test.ts`.
  - `web/src/components/sections/__tests__/CalendarStripSchemaDecode.test.ts`.

### Phase 6 — Documentation

- [ ] **`AGENTS.md`** — add a new sub-section under §3 (Server Authority In
      Practice) or §4 (Shared Infrastructure Owns Shared Concerns) codifying
      the two channels and the "no partial screen response" rule. Suggested
      placement: new §3.7 "Update channels" or extend §3.3 "Refresh,
      polling, and live updates". Capture:
  - Screen channel definition (URL family, response shape, client semantic).
  - Section channel definition (URL family, response shape, client
    semantic).
  - The "no partial screen response" prohibition.
  - The rule that structural changes (insert/remove/reorder) flow through
    the screen channel at the appropriate cadence.
  - **Current-params replay rule**: every screen-channel fetch
    (pull-to-refresh, poll tick, action-driven `refresh` targeting the
    current screen) carries the screen's current query params. The user's
    parameterization is preserved until the user explicitly changes it.
  - **Poll-timer reset rule**: a successful screen-channel fetch resets
    the screen-level poll timer.
- [ ] **`docs/games-screen-spec.md`** — update §9 (Parameterized refresh
      contract) to reference the unified URL; add a new §11 "Update channels
      & cadence ownership" capturing the channel-grain rule and the cadence
      table.
- [ ] **`docs/client-implementors-contract.md`** — add a section
      "Update channels" describing the two channels and the
      one-method-per-channel client API expectation. Add to the conformance
      checklist:
  - "Implements screen-channel fetch with strict full-replace, validates
    response id against current screen id."
  - "Implements section-channel fetch (poll + SSE) with replace-by-id
    semantics."
  - "Remembers the current screen's query params and replays them on
    pull-to-refresh, screen-level poll, and action-driven `refresh`
    targeting the current screen."
  - "Resets the screen-level poll timer on every successful screen-channel
    fetch."
- [ ] **`docs/sdui-envelope-spec.md`** — if it currently documents the
      `/refresh/` family as a distinct concept, fold it into the unified
      screen channel description.
- [ ] **`docs/sdui-design-system.md`** — search for `refresh` URL references;
      update.
- [ ] **`docs/sdui-requirements-summary.md`** — add an entry/row in the
      status matrix for the unified update channel architecture; reference
      this plan.
- [ ] **`README.md`** — update any references to the `/refresh/` URL.
- [ ] **ADR** — write a short ADR capturing the decision:
      `docs/adr/ADR-0XX-update-channel-unification.md`. Include the
      post-mortem context (calendar-strip stale-section bug), the decision,
      the alternatives considered (keep split, introduce delta envelope),
      and the consequences.

### Phase 7 — Future-facing (not blocking this plan)

These items follow naturally from the unified architecture but don't need to
land with the URL collapse:

- [ ] **Section-scoped `loadMore` action** — formalize the pattern for
      lazy-loading additional items into an existing section (rail, list)
      that re-fetches the section endpoint with a pagination cursor. Should
      land alongside the first feature that needs pagination, not as part
      of this plan.

## Dependencies

- Android already has the strict same-id full-replace fix from the May
  2026 calendar-strip work. iOS does not (see Current State); this plan
  lands it. Web's state is to be confirmed during Phase 5.
- No schema or codegen dependencies — the schema's `Action.endpoint` is
  already a free-form path string.
- Server + all clients ship together so the URL hard-cut doesn't strand
  any platform. Coordinate the merge order: server contract tests
  (Phase 2.5) land first as the regression fence, then composers switch
  endpoints (Phase 2), then the route is removed (Phase 1), then clients
  pick up the new endpoints in their existing fetch paths (Phases 3–5).

## Open Questions

- [ ] **`mutate` action type interaction.** `mutate` writes local screen
      state without fetching. It's orthogonal to the channel question, but
      worth verifying during Phase 2 that no existing action chains do
      `mutate` → `refresh-with-partial-shape` in a way the URL collapse
      would break.
- [ ] **Section-channel response shape consistency.** Confirm
      `/v1/sdui/section/{id}` always returns a single `Section` object
      (never a `Screen`, never a list). The current code suggests it does;
      add a contract test in Phase 1 that pins this down.

## Acceptance Criteria

The plan is complete when:

- [ ] Every action emitted by every composer uses `/v1/sdui/screen/{id}`
      (with query params as needed) for screen re-composition, or
      `/v1/sdui/section/{id}` for section updates. Zero references to
      `/screen/refresh/`.
- [ ] The server route `/v1/sdui/screen/refresh/{id}` is fully removed —
      no alias, no handler stub, no test fixture references.
- [ ] `ScreenChannelContractTest` and `SectionChannelContractTest` exist
      and pin: response shape per channel, id-match invariant, action
      endpoint URL-shape regex, and parameter replay through emitted
      action endpoints.
- [ ] The static regression guard for the literal `screen/refresh/`
      substring in composer sources is green.
- [ ] The composer round-trip tests for `LiveComposer` and
      `DemoScreenComposer` cover at least one parameterized screen each
      and prove the param survives the emitted-action → controller →
      response loop.
- [ ] All three clients expose a single screen-fetch API with strict
      same-id full-replace semantics, and a single section-fetch API with
      replace-by-id semantics. No third channel.
- [ ] All three clients preserve the current screen's query params across
      pull-to-refresh, screen-level poll, and action-driven `refresh`
      targeting the current screen, with per-platform tests proving it.
- [ ] All three clients reset the screen-level poll timer on every
      successful screen-channel fetch, with per-platform tests proving it.
- [ ] `AGENTS.md` codifies the two-channel rule, the no-partial-screen
      prohibition, the current-params replay rule, and the poll-timer
      reset rule.
- [ ] An ADR captures the decision with post-mortem context.
- [ ] All existing tests pass; new tests cover the unified URL contract and
      the channel-shape invariants (screen → full Screen, section → single
      Section).
- [ ] `docs/games-screen-spec.md` and
      `docs/client-implementors-contract.md` reflect the unified
      architecture and point at the new `AGENTS.md` clauses as the canonical
      source.
- [ ] `docs/sdui-requirements-summary.md` is updated properly wtih server behavior contract