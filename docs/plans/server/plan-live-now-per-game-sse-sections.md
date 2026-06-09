# Plan: Per-Game Card Sections with Lifecycle Refresh Policies

> Source: SDUI_Technical_Proposal_v2.md §3 (Data Binding System), AGENTS.md §1.2
> (schema-first), §1.4 (one owner per concern), §3.3 (refresh is server-driven),
> §3.8 (update channels), §4.4 (shared binding infra), §11 (variant discipline).
>
> ADR alignment: ADR-016 (update-channel unification — section channel is the
> right vehicle for single-section refresh). **New ADR required:
> ADR-019 "Concurrent section refresh mechanisms"** (confirmed next free number;
> highest existing is ADR-018) because this changes the wire shape of
> `Section.refreshPolicy` and the §3.3/§3.8 doctrine.

## Summary

Today the Games screen's game lists are composed as a handful of grouped
`GameScheduleList` sections (one for "Live Now", one for "Upcoming", one for
"Final"), each containing N rows. The "Live Now" group carries a single
`refreshPolicy.channel` pinned to the *first* live game's linescore channel, so
only that one row updates in real-time; every other game refreshes only on the
next full screen composition.

This plan reshapes the Games screen so that **every game is its own card
section** with a **state-dependent refresh policy** that drives the game through
its own lifecycle:

- **Pregame** — a 5-minute **section refresh** that re-composes the card and
  lets it transition itself into the live state when the game tips off.
- **Live** — an **SSE** subscription for high-volatility score/clock updates,
  running **concurrently** with a 1-minute **section refresh** that reconciles
  low-volatility fields and eventually flips the card to the final state.
- **Final** — **static**: no refresh, no traffic.

Expressing "SSE *and* a section refresh at the same time" requires
`Section.refreshPolicy` to become a **bounded array of refresh mechanisms**
(max 2: at most one opaque/streaming source plus at most one section refresh).
This is the one schema change in the plan.

## Background

### Why this plan exists

A prior change attempt introduced `DataBinding.channels: string[]` (plural) on
the schema, plus per-client union/fan-out logic that subscribed to N Ably
channels for a single section. That work was reverted (see the working-tree
revert in `LiveComposer.buildScheduleListBindings`) because:

1. **Doctrine deviation.** `SDUI_Technical_Proposal_v2.md §3` and both
   implementor contracts state the subscription channel belongs to
   `refreshPolicy`. `DataBinding` maps payload → view model and does not own
   transport. (The reverted code emitted `channels` via `additionalProperties`,
   i.e. a field not in the schema at all — a §1.2/§1.3 violation on its own.)
2. **Cross-platform complexity.** All three clients had to grow identical
   "union `refreshPolicy.channel` with `dataBinding.channels`" helpers,
   parallel task groups, channel-aware dedupe, and channel-aware policy keys.
3. **No second consumer.** Only one composer needed it. A schema field added
   for one caller is a smell.

### Why per-game cards with a refresh-policy array is the right answer

- **One channel per section stays true.** Each card subscribes to at most one
  SSE channel. We are *not* reintroducing multi-channel-per-section fan-out
  (see [Non-Goals](#non-goals)). The array adds *concurrency across mechanism
  kinds* (one stream + one section refresh), not multiple streams.
- **The lifecycle is server-owned and self-driving.** A pregame card polls
  itself into liveness; a live card reconciles and flips itself to final;
  a final card goes quiet. The client never decides any of this from content
  (§3.3) — it just re-evaluates the policy the server returns.
- **It solves the "new live game" discovery gap for free.** Because each
  pregame game is already its own polling card, a game that tips off mid-session
  re-composes into a live card on its next 5-minute tick — no screen-level poll,
  no client screen-identity logic.
- **The screen becomes a flat list of card sections** in roster order
  (live, then upcoming, then final) — no group containers, no band headers.
  Existing clients already render sibling top-level sections, so there is zero
  new structural surface.

## Why an array (not an overloaded single policy)

A single `RefreshPolicy` object has one `type` discriminator, and every client
branches on it exclusively (web `useRefreshPolicy`: the poll effect early-returns
unless `type === 'poll'`; the SSE effect early-returns unless `type === 'sse'`).
A section therefore runs exactly one mechanism today, so "SSE + section refresh"
is not expressible.

Two ways to add concurrency were considered:

- **Reuse the existing fields on one object** (`channel` next to `sectionEndpoint`
  + `intervalMs`) and redefine clients to run both. No schema change, but `type`
  stops faithfully describing behavior — an `sse`-typed policy that *also* polls
  is a latent footgun every future reader must know about.
- **Make `refreshPolicy` a bounded array** (chosen). Each element stays an
  internally-consistent `RefreshPolicy`; concurrency is explicit on the wire;
  clients **reuse their existing per-`type` logic** by iterating elements rather
  than reinterpreting what `sse` means. This screen now makes a multi-stage
  per-card lifecycle a recurring, first-class concept, which justifies the
  one-time contract change over permanently overloading `type` (§11).

### The array shape and its invariants

`Section.refreshPolicy` becomes an array of `RefreshPolicy`. Exactly two
mechanism kinds may run concurrently:

| Element kind | Wire fields | Payload | Consumes `dataBinding`? |
|---|---|---|---|
| **Opaque / streaming** | `type: sse` + `channel` (or `type: poll` + `url` + `intervalMs`) | opaque frame/JSON → patches fields | **Yes** |
| **Section refresh** | `type: poll` + `sectionEndpoint` + `intervalMs` | a full composed `Section` → replaces in place | No (full replace) |

Invariants (enforced in schema + validated server-side, not just documented):

- **≤ 1 opaque element** per section, so `dataBinding` maps unambiguously to
  "the streaming source."
- **≤ 1 section-refresh element** per section.
- ⇒ **`maxItems: 2`.**
- **`static` is terminal and solo** — a static section is a single
  `{type: static}` element (or simply omits `refreshPolicy`); it never coexists
  with the other kinds.

### How `dataBinding` maps to the array

`dataBinding` stays a **section-level** field (it is section-level for every live
section today — boxscore, etc.; moving it into `refreshPolicy` would split a
single concern across two places, a §1.4 regression). It binds to the **single
opaque element**:

- **Opaque elements** (`sse channel`, `url` poll) deliver opaque payloads that
  the client maps through `section.dataBinding` (`applyDataBindings`). With the
  ≤1-opaque invariant, there is exactly one such source, so the binding target
  is unambiguous.
- **Section-refresh elements** (`sectionEndpoint`) return a fully composed
  `Section` and **never** consult `dataBinding`. The replacement carries its own
  `data`, `refreshPolicy`, and `dataBinding`.

For a live card: the SSE element carries the linescore bindings
(`$.homeTeam.score → content.{gameId}.homeScore`, clock, etc.); the 1-minute
`sectionEndpoint` element returns a freshly-composed card (scores baked in by
the server, and the eventual `type: static` flip) and needs no binding. Pregame
and final cards have no opaque element, so they carry no `dataBinding`.

**In-place replace resets accumulated SSE patches (intended, not data loss).**
The SSE element patches the card's live data in place between `sectionEndpoint`
ticks; when the 60s full-replace lands, it swaps the whole `section.data` with
server-baked scores, discarding the interim SSE patches. This is correct: the
replacement is the newer truth (composed from the same upstream the SSE frames
came from), and SSE resumes patching the fresh baseline immediately. The only
requirement is that the replace and the patch stream target the *same* section
state so the reset is a clean reseed rather than a flicker between two sources
(see the Phase 2 stability note on keying the SSE effect off the stable
`channel`, not array identity).

## Lifecycle: game status → emitted `refreshPolicy`

The composer keys off upstream `gameStatus` (1 = pregame, 2 = live, 3+ = final):

```json
// Pregame (status 1) — section refresh only; self-transitions to live
"refreshPolicy": [
  { "type": "poll", "sectionEndpoint": "/v1/sdui/section/<cardId>", "intervalMs": 300000 }
]

// Live (status 2) — opaque SSE + section refresh, concurrently
"refreshPolicy": [
  { "type": "sse",  "channel": "<gameId>:linescore" },
  { "type": "poll", "sectionEndpoint": "/v1/sdui/section/<cardId>", "intervalMs": 60000 }
]

// Final (status 3+) — terminal
"refreshPolicy": [ { "type": "static" } ]
```

Each section-refresh tick re-runs composition for that one game and returns the
card at *whatever its current status is now*, so the card walks
pregame → live → final on its own. `pauseWhenOffScreen` stays per-element
(`false` on the live SSE element to keep scores flowing off-screen; default
`true` elsewhere).

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema | **Change required** | `Section.refreshPolicy` is a single `RefreshPolicy` object; must become a bounded array (`maxItems: 2`) with the opaque/section-refresh invariants |
| Codegen | **Regen required** | Java/Kotlin/Swift/TS models regenerate `Section.refreshPolicy` to a list type |
| Clients (web/iOS/Android) | **Change required** | Refresh infra branches on a single `policy.type`; must iterate the array and run ≤1 opaque + ≤1 section-refresh concurrently. Also: policy fingerprint key, screen-default guard, visibility gating per element |
| Server `LiveComposer` | Partial | Emits grouped `GameScheduleList` sections; live channel pinned to first live game. Must emit one card section per game with a status-derived policy array (flat list, no headers) |
| `AtomicCompositeBuilder` | **Change required** | Builds rows/clock snapshots (single-card builder stays internal — no schema change there), **but** `newSection` stamps a default single-object `static` `refreshPolicy`; that line and every server `setRefreshPolicy(...)` call site must emit a single-element array once the field is a list |
| All other composers | **Change required (mechanical)** | The array migration is global: `BoxscoreComposer`, `ForYouComposer`, `GameDetailComposer`, `ScoreboardComposer`, `WatchComposer`, `ScheduleComposer`, `DemoScreenComposer`, and `AtomicCompositeBuilder` all call `setRefreshPolicy(<single object>)` and won't compile until wrapped in a single-element array. Behavior is unchanged; only the wire shape moves to an array of one. (`Screen.defaultRefreshPolicy` call sites are unaffected — that stays a single object.) |
| Section-refresh resolver | Partial | One resolver registered for `stats-api:live-games` returns the whole group. Must become a per-card resolver keyed by a `gameId`-bearing prefix that composes the card at its current status |
| Ad-slot (`insertGamesScreenAdSlot`) | Partial | Index math assumes CalendarStrip @ 0, Promo @ 1, then one section per *status group*. Per-card (flat) sections change the index map |
| Tests / fixtures | Partial | `SchemaConformanceTest`, `SectionChannelContractTest`, `MutateThenRefreshAuditTest` rely on the single-section shape and single-object policy |
| Docs | **Sync required** | AGENTS.md §3.3/§3.8, both implementor contracts, and the envelope/section specs describe a single-object policy and one-mechanism-per-section |

## Requirements Addressed

- **REQ-1**: Every live game's score/clock updates in real-time via its own
  per-game SSE channel — `SDUI_Technical_Proposal_v2.md §3`, ADR-016.
- **REQ-2**: A pregame game transitions itself into the live state via a
  5-minute section refresh — closes the "new live game discovery" gap without a
  screen-level poll or client screen-identity logic (§3.3).
- **REQ-3**: A live game reconciles low-volatility fields and flips to final via
  a concurrent 1-minute section refresh running alongside SSE.
- **REQ-4**: A final game is static — no refresh traffic.
- **REQ-5**: Channel ownership stays on `refreshPolicy`; `dataBinding` remains
  mapping-only and binds to the single opaque element — AGENTS.md §1.4, §4.4.
- **REQ-6**: No multi-channel-per-section fan-out; the array is bounded
  (`maxItems: 2`, ≤1 opaque, ≤1 section-refresh) — AGENTS.md §11.

## Decision: screen layout (decided)

**Every game is its own card section**, emitted as a flat list in roster order
(live, then upcoming, then final). **No band headers** — the previous
`GameScheduleList` band titles ("Live Now" / "Upcoming" / "Final") are dropped:

```
[CalendarStrip]
[Promo]
[GameCard gameId=A]   ← live      refreshPolicy: [sse, sectionEndpoint@60s]
[GameCard gameId=B]   ← live      refreshPolicy: [sse, sectionEndpoint@60s]
[GameCard gameId=C]   ← pregame   refreshPolicy: [sectionEndpoint@300s]
[GameCard gameId=D]   ← pregame   refreshPolicy: [sectionEndpoint@300s]
[GameCard gameId=E]   ← final     refreshPolicy: [static]
[AdSlot]
```

This uses the existing top-level sections contract with zero new client
structural surface and no new section types.

**In-place transition is intended (decided).** A section refresh replaces a card
*in place*; it does not move the card within the roster. A pregame card that tips
off becomes live (live score + SSE) but keeps its current list position until the
next full screen composition re-sorts the roster — and that re-sort only happens
on a full screen fetch (pull-to-refresh or navigation). This is the accepted
behavior, not a defect: the card's data and refresh policy are correct
immediately; only its ordering waits for a pull-down. We deliberately do **not**
add a screen-level re-sort poll (it would be mutually exclusive with the per-card
`sectionEndpoint` polls under §3.3).

## Tasks

> Ordering follows §1.2: schema first, then codegen, then clients learn the new
> shape, then the server emits it, then resolver, tests, docs, smoke. This
> prevents a window where the server emits an array the clients can't decode.

### Phase 1 — Schema & codegen

- [ ] Change `Section.refreshPolicy` in `schema/sdui-schema.json` from a single
  `RefreshPolicy` to an array of `RefreshPolicy` with `"maxItems": 2`.
- [ ] Encode the invariants the JSON Schema can express (`maxItems: 2`) and add
  a server-side validator for the cross-element constraints Draft-07 can't
  express cleanly (≤1 element with `channel`/`url`; ≤1 element with
  `sectionEndpoint`; `static` only as a solo element). **Wire it where it
  actually runs:** `SchemaConformanceTest`
  (`server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java`)
  already validates every composed `Screen`/`Section` against the schema; the
  `maxItems: 2` bound is caught there for free, and the cross-element validator
  should run over the same composed sections in that test so the invariants are
  enforced on every composer fixture rather than living as an unexecuted
  standalone check. (Draft-07 catches `maxItems`/type/enum; the validator
  covers the rest.)
- [ ] Keep `Screen.defaultRefreshPolicy` a **single** `RefreshPolicy` object —
  a screen has one default and the screen channel is always a full replace;
  only sections need concurrent multi-mechanism refresh. Document this
  asymmetry.
- [ ] Run `make codegen`; verify `Section.refreshPolicy` regenerates to a list
  type in Java/Kotlin/Swift/TS (`server/src/generated/java/`,
  `android/.../SduiModels.kt`, `ios/.../SduiModels.swift`,
  `web/src/generated/SduiModels.ts`).
- [ ] **Global mechanical migration (server won't compile until done).** The
  field flip makes every `section.setRefreshPolicy(<single object>)` call site
  a type error. Wrap each in a single-element array, behavior unchanged. Known
  sites: `AtomicCompositeBuilder.newSection` (the default `static` policy at
  lines ~50–60, hit by nearly every AtomicComposite section), plus
  `BoxscoreComposer`, `ForYouComposer`, `GameDetailComposer`, `WatchComposer`
  (×4), `ScheduleComposer`, and `DemoScreenComposer`. Leave the
  `Screen.setDefaultRefreshPolicy(...)` sites alone — that field stays a single
  object. This pass is the reason the schema change is screen-global, not
  games-only; the games-screen lifecycle policies are then just the first
  *multi*-element arrays.

### Phase 2 — Client refresh infrastructure (web / iOS / Android)

- [ ] Update each platform's refresh driver to **iterate the array** and run
  each element through the existing per-`type` logic concurrently:
  - Web `useRefreshPolicy` (`web/src/hooks/useRefreshPolicy.ts`): split the
    single `policy` into the opaque element and the section-refresh element;
    run the SSE/`url` effect and the poll effect off their respective elements.
  - iOS / Android: mirror via their `AblyChannelManager` / `ablyJobs` +
    section-poll equivalents.
- [ ] **Migrate every web consumer of the now-array `refreshPolicy`, not just
  `useRefreshPolicy`.** `getEffectiveRefreshPolicy` (`useRefreshPolicy.ts`)
  returns a single `RefreshPolicy | undefined` today; its return shape changes
  and so do its callers: `LiveSectionWrapper`, the `useLiveData` hook (same
  file — a second, easily-missed consumer), and `SectionRouter.tsx`. Audit all
  three plus `sectionPolicyKey` so nothing still assumes a single object.
- [ ] Enforce the client-side invariant defensively: pick at most one opaque
  element and at most one section-refresh element; log and ignore extras.
- [ ] `dataBinding` continues to apply only to opaque-element payloads
  (`applyDataBindings` on `onUpdate`); `sectionEndpoint` stays full-replace
  (`onSectionReplace`). No change to the binding applier itself.
- [ ] Fix the policy fingerprint key (`sectionPolicyKey` on web) to hash **all**
  elements, so a pregame→live policy change remounts the driver. Mirror on the
  other platforms' remount/restart keys.
- [ ] Update the screen-default-vs-`sectionEndpoint` mutual-exclusion guard
  (`LiveSectionWrapper`, lines ~64–72) to test "does any element carry a
  `sectionEndpoint`."
- [ ] **SSE-resubscribe stability (avoid 60s churn).** `useRefreshPolicy`'s SSE
  effect currently depends on the `policy` object identity
  (`useRefreshPolicy.ts:158–199`). With a live card running SSE *and* a 60s
  `sectionEndpoint`, every full-replace produces a **new** policy array object;
  if the SSE effect keys off array identity it will tear down and resubscribe
  the Ably channel every 60 seconds (connection churn + dropped frames during
  the gap). Key the SSE effect off the stable `channel` **string** (and the
  poll effect off `sectionEndpoint`/`intervalMs`), not the array/object
  reference. Mirror the same stability rule on iOS/Android subscription restart
  keys.
- [ ] **Restructure `LiveSectionWrapper` from one policy to per-element drivers.**
  The wrapper today derives one `effectivePolicy`, one `pauseWhenOffScreen`, one
  `enabled`, and passes a single policy to a single `useRefreshPolicy`
  (`LiveSectionWrapper.tsx:60–136`). Running ≤1 opaque + ≤1 section-refresh with
  **per-element** `pauseWhenOffScreen` (live SSE = `false`, poll = `true`) means
  the single `enabled` gate must become per-element. Decide and document one of:
  (a) the wrapper invokes the driver once per element, each with its own
  `enabled` gate; or (b) `useRefreshPolicy` accepts the array and owns
  per-element gating internally. Either way the wrapper holds one `liveData`
  state that the opaque `onUpdate` patches and the `onSectionReplace` reseeds —
  do not split `liveData` per element.

### Phase 3 — Server `LiveComposer`

- [ ] Add `buildGameCardSection(Game game)` that emits one `Section` per game,
  with content from the existing `gameToRow` / clock-snapshot path scoped to a
  single game, and a **status-derived** `refreshPolicy` array:
  - status 1 → `[ poll(sectionEndpoint, 300_000, pauseWhenOffScreen=true) ]`
  - status 2 → `[ sse(channel="{gameId}:linescore", pauseWhenOffScreen=false),
    poll(sectionEndpoint, 60_000, pauseWhenOffScreen=true) ]` plus the
    single-game `dataBinding` (linescore frame → `content.{gameId}.*`).
  - status 3+ → `[ static ]`, no `dataBinding`.
  - `id` derived from a **dedicated** `gameId`-bearing content source
    `games:card-{gameId}` (→ sanitized prefix `games-card-`) so the resolver
    can invert it (Phase 4). **Do not reuse `stats-api:game-{gameId}`** — that
    source/prefix is already owned by `GameDetailComposer`'s resolver
    (`stats-api-game-`), and the section-refresh registry is keyed by prefix
    only, so a second registration there would clobber game detail (or vice
    versa) depending on `@PostConstruct` order. `games:card-` is collision-free
    (nothing else sanitizes to a `games-` prefix) and inverts cleanly
    (numeric gameIds carry no `__`/`-`). If the cards should look identical to a
    game-detail card later, share the *builder method*, not the resolver
    registration.
  - `sectionEndpoint` = `/v1/sdui/section/{cardId}`.
- [ ] In `composeLive`, replace the grouped `buildLiveScheduleList` /
  `buildScheduleList(...)` calls with a single flat pass:
  `games.forEach(g -> sections.add(buildGameCardSection(g)))` in roster order
  (live, then upcoming, then final). No header sections.
- [ ] Update `insertGamesScreenAdSlot`: games now occupy one section each; the
  "after Nth game" rule counts **game-card sections**, offset only by
  CalendarStrip + Promo (no headers). With per-card sections there are no status
  *groups* to split, so the old "never split a group" concern dissolves — pick
  the literal "after the Nth card" placement.
- [ ] Delete `buildLiveScheduleList`, `buildEmptyLiveScheduleList`, and the
  grouped `buildScheduleList` once nothing references them. (`buildEmptyLive…`
  is no longer reachable: an empty roster emits no cards; "all live games
  concluded" is handled per-card by the final/static flip or 404.)
- [ ] Remove the now-dead `stats-api:live-games` resolver registration in
  `LiveComposer`'s `@PostConstruct` (the lambda returning the old grouped
  list). Its replacement is the per-card resolver from Phase 4. Any test that
  references the `stats-api-live-games__type-GameScheduleList` id (e.g.
  `SectionChannelContractTest`) must move to a `games-card-{gameId}` id.

### Phase 4 — Section-refresh resolver

Use a **single prefix resolver** keyed by the per-game content source, not
per-game dynamic registration (`SectionRefreshService` has no deregistration
API; the registry is a plain prefix map with longest-prefix-wins matching):

- [ ] Register one resolver at `SectionIdDeriver.prefixFor("games:card-")`
  (→ `games-card-`). This **replaces** the `stats-api:live-games` registration
  removed in Phase 3 (don't leave a dead resolver pointing at deleted
  `buildLiveScheduleList`/`buildEmptyLiveScheduleList` methods). It is a
  **dedicated** prefix, intentionally distinct from `GameDetailComposer`'s
  `stats-api-game-` registration so the two resolvers never share a registry key
  (the prefix map overwrites on a duplicate key, and `@PostConstruct` order is
  not guaranteed). Verified collision-free: no other composer's content source
  sanitizes to a `games-` prefix.
- [ ] In the resolver, invert the `gameId` from the section id
  (`SectionIdDeriver.parse(id).source()` → strip the `games-card-` prefix),
  fetch that game, and return `buildGameCardSection(game)` composed at its
  **current** status. This is what lets a pregame card self-transition: the
  5-minute tick returns a live card once the game has tipped off, and the
  client re-evaluates the new policy array.
- [ ] 404 path: if the `gameId` is not present in the slate (bad id / misroute),
  the section channel returns 404. **Unified cross-platform behavior (decided):
  the client marks the section stale and stops that section's poll, but RETAINS
  the node on screen** — it does not remove the card. This matches the existing
  iOS (`stalenessTracker.markStale` + `polling.stop`) and Android
  (`_staleSections + sectionId`, stop drivers) behavior; web was previously the
  odd one out (it removed the node via `onSectionGone`) and is brought into
  line. A final game stays in the slate and returns a static card, so it is
  *not* a 404 case. (The unreachable web `onSectionGone` removal plumbing is
  deleted as part of this alignment.)

### Phase 5 — Tests

- [ ] New `LiveComposer` unit tests:
  - Given games across all three statuses, `composeLive` emits one card section
    per game in roster order (flat list, no headers).
  - A status-1 card emits `refreshPolicy == [poll(sectionEndpoint, 300000)]`.
  - A status-2 card emits `[sse(channel="{gameId}:linescore"),
    poll(sectionEndpoint, 60000)]` and a single-game `dataBinding`.
  - A status-3 card emits `[static]` and no `dataBinding`.
  - Regression pin: no section emits `dataBinding.channels` (the reverted field
    lived in `additionalProperties`; assert it is absent there).
  - Array-bound pin: no section's `refreshPolicy` exceeds 2 elements, has >1
    opaque element, or >1 `sectionEndpoint` element.
- [ ] Section-refresh resolver tests: requesting a card id returns the card at
  the game's current status; an unknown `gameId` returns empty (404).
- [ ] Update `SchemaConformanceTest`, `SectionChannelContractTest`, and
  `MutateThenRefreshAuditTest` for the array shape and the new section roster.
- [ ] Update any pre-recorded fixtures under `schema/examples/`.

### Phase 6 — Documentation sync (required by §10.4)

- [ ] AGENTS.md §3.3 and §3.8: describe a section running an SSE subscription
  **and** a section-refresh poll concurrently via the `refreshPolicy` array;
  state the bounded-array invariants.
- [ ] `docs/contracts/client-implementors-contract.md` and
  `docs/contracts/server-implementors-contract.md`: update the `refreshPolicy`
  shape (array), the `dataBinding`-binds-to-opaque-element rule, and the
  `Screen.defaultRefreshPolicy` (single object) asymmetry.
- [ ] Envelope/section specs under `docs/specs/`: same.
- [ ] Author **ADR-019** (concurrent section refresh mechanisms): context,
  the array decision vs. overloading `type`, the bounded-array invariants, the
  `Screen.defaultRefreshPolicy`-stays-single asymmetry, and consequences.

### Phase 7 — Cross-platform smoke

- [ ] Web (`make dev-web-local`): each live card receives independent Ably
  updates *and* re-composes every 60s; a pregame card flips to live on its 5-min
  tick; a finished game goes static. Verify distinct per-card policy keys.
- [ ] iOS (`make dev-ios-local`): each live card subscribes via
  `AblyChannelManager` and runs its concurrent section poll; per-card visibility
  pausing works per element.
- [ ] Android (`make dev-android-local`): each live card has its own `ablyJobs`
  entry plus a section-poll job; per-card buffering on scroll-out works.

## Scaling considerations

This design materially changes the cost profile versus the grouped approach.
It is the intended trade for REQ-1 (real-time for *every* live game), but the
costs should be explicit:

- **Subscriptions scale linearly with the live slate.** The old design held one
  SSE subscription for the whole "Live Now" group; this opens one SSE channel
  **per live game**. The live SSE element is `pauseWhenOffScreen=false`, so those
  subscriptions stay open even when the card scrolls off-screen — a 12–15-game
  night means 12–15 concurrent Ably channels per client.
- **Section-refresh fetch volume.** Each visible live card re-fetches
  `/v1/sdui/section/{cardId}` every 60s and each pregame card every 5min.
- **Mitigations already in the design:** `pauseWhenOffScreen=true` on the poll
  elements suppresses off-screen polls; `SectionFragmentCache` collapses
  concurrent identical section recompositions server-side (absorbs compute, not
  request count). Per-card SSE is the goal, not a regression.
- **Escape hatch if it bites:** server-side fan-in to one aggregated upstream
  channel (see Non-Goals) cuts upstream load without a wire change — it's
  composer-side only and invisible to clients.

## Non-Goals

- **Multi-channel-per-section subscription / `DataBinding.channels`.** Explicitly
  rejected. The array allows ≤1 opaque element (≤1 SSE channel) per section; it
  does not reintroduce fan-out across multiple channels in one section.
- **Making `Screen.defaultRefreshPolicy` an array.** A screen has one default
  and the screen channel is a full replace; concurrency is a section concern.
- **Server-side fan-in to one aggregated upstream channel.** Possible future
  optimization, invisible to the wire (composer-side only).
- **Re-sorting a card within the roster without a full screen fetch.** A card
  transitions its own state/policy in place; roster order is re-evaluated only on
  a full screen fetch (pull-to-refresh / navigation). No screen-level re-sort
  poll is added (decided).
- **Band headers / group titles.** Dropped — the screen is a flat card list
  (decided).

## Rollout

This is a wire-contract change, so it ships coordinated across server + three
clients (unlike the earlier single-screen plan). **It is also a screen-global
migration, not a games-only change:** flipping `Section.refreshPolicy` to an
array makes every composer's `setRefreshPolicy(...)` and
`AtomicCompositeBuilder.newSection`'s default `static` policy single-element
arrays (Phase 1 mechanical pass), and every client `refreshPolicy` consumer must
decode an array — so *all* screens move to the array shape simultaneously, even
though only the games screen emits *multi*-element arrays. Order per §1.2: land
the schema + codegen + the server mechanical migration and the client
array-handling first (clients must decode the array before the server emits it),
then switch `LiveComposer` to emit per-card multi-element array policies, then
the resolver, then docs. Because every section previously emitted only
single-object policies, the array form is the new truth on first emit (old
clients strict-decode-fail on it per §1.3, which is why the ship is coordinated);
there is no mixed-shape window if the merge order above is respected.

## Resolved Decisions

- **Layout:** flat list of per-game card sections in roster order; **no band
  headers / group titles.**
- **Re-sorting:** cards transition state/policy in place; roster order is
  re-evaluated only on a full screen fetch (pull-to-refresh / navigation). No
  screen-level re-sort poll.
- **Static cards:** final cards emit the explicit `[ { "type": "static" } ]`
  form (not an omitted `refreshPolicy`); assert this canonical shape in the
  contract test.
- **Section 404 handling (unified):** on a section-channel 404 the client marks
  the section stale and stops that section's poll but RETAINS the node on
  screen. All three platforms behave identically; web's prior node-removal
  (`onSectionGone`) path is deleted.
