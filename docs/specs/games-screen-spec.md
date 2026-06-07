# Games Screen & CalendarStrip — Specification

Status: draft (captures decisions ratified during the May 2026 games-screen
refresh). Eventual home is `AGENTS.md` once the patterns generalize beyond a
single screen.

This document defines the contract for the Games screen and its CalendarStrip
date picker. It is paired with the implementation in
`server/src/main/java/com/nba/sdui/service/LiveComposer.java` and
`android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/CalendarStripRenderer.kt`.

---

## 1. Screen identity

- Screen `id`: `"games"`.
- Owned by `LiveComposer`.
- Screen-channel endpoint: `/v1/sdui/screen/games` (with optional
  `?date=YYYY-MM-DD` for parameterized re-composition).
- Section-channel endpoint (live-games SSE/poll only):
  `/v1/sdui/section/{liveSectionId}`.

## 2. Section roster & ordering

The Games screen emits sections in this order, with sections omitted when
empty (see §6):

1. **CalendarStrip** — always present, first section.
2. **GameScheduleList "Live Now"** — emitted only when at least one game has
   `gameStatus == 2`. SSE-bound per row.
3. **GameScheduleList "Upcoming"** — emitted only when at least one game has
   `gameStatus == 1`. Static refresh.
4. **GameScheduleList "Final"** — emitted only when at least one game has
   `gameStatus >= 3`. Static refresh.

No featured/hero card. No mock placeholder cards in either path.

## 3. CalendarStrip wire contract

```json
{
  "id": "server-games-calendar__type-CalendarStrip",
  "type": "CalendarStrip",
  "contentSourceId": "server:games-calendar",
  "analyticsId": "games_calendar_strip",
  "accessibility": { "label": "Games date picker" },
  "data": {
    "stateKey": "games_selected_date",
    "selectedDate": "2026-05-26",
    "defaultDate": "2026-05-26",
    "minDate": "2025-10-01",
    "maxDate": "2026-06-30",
    "onDateSelected": {
      "trigger": "onActivate",
      "type": "refresh",
      "endpoint": "/v1/sdui/screen/games",
      "paramBindings": { "date": "{{games_selected_date}}" }
    }
  }
}
```

Field rules:

- All dates are ISO `YYYY-MM-DD`, ET-anchored. No locale-formatted display
  strings on the wire.
- `stateKey` is the `screenState` slot the renderer writes on tap. Other
  sections on the same screen may read it via `{{games_selected_date}}`
  bindings.
- `selectedDate` is which cell renders highlighted on this composition.
- `defaultDate` is "today" from the authoritative source (§4) and drives the
  secondary "today" affordance on that cell when selection has moved
  elsewhere.
- `onDateSelected` is a **single** Action object, not an array.
- When the month label is tapped, CalendarStrip dispatches `expandedAction`
  (a `navigate` action to `nba://calendar`).

## 4. Default date derivation (server)

**Rule:** the screen's `defaultDate` is the value of `scoreboard.gameDate` in
the CDN `todaysScoreboard_00.json` response.

- The CDN's `gameDate` field already accounts for the league's late-night
  roll-forward (a 10pm PT tipoff still counts as the prior calendar day
  until the league rolls over). Wall-clock midnight ET does not.
- The server fetches the CDN scoreboard once per Games-screen composition.
  That single response serves both as the default-date source and as the
  games payload when the user is viewing today.
- If the CDN call fails or `gameDate` is missing/unparseable, fall back to
  `SeasonCalendarService.currentLeagueDate()` (wall-clock ET).
- Never advance the default past what the CDN says. If the user opens the
  screen and the CDN's `gameDate` is two days from now, that is the default
  date and the strip shows that day's games.

## 5. Per-date data sourcing

| Selected date | Source | Why |
|---|---|---|
| Equals `defaultDate` | CDN `todaysScoreboard_00.json` (reused) | Realtime, cacheable, SSE-keyed, no auth |
| Any other date | NBA Core API `gameCardFeed?gamedate=MM/DD/YYYY&platform=web` | Per-date query support |

- Core API requires `Ocp-Apim-Subscription-Key` (env: `CORE_API_OPIM_SUBSCRIPTION_KEY`).
- Core API response is normalized to the CDN scoreboard shape
  (`{ scoreboard: { gameDate, games: [...] } }`) by
  `StatsApiClient.normalizeCoreApiFeedToScoreboard`.
- If the key is missing or the call fails, `getScoreboardForDate` returns
  `null`. Composers do **not** synthesize mock content — they emit the empty
  state (§6).

## 6. Empty-state semantics

- If `gameStatus` partitioning yields zero games for the selected date, the
  screen emits only the CalendarStrip. No "Live Now"/"Upcoming"/"Final"
  headers, no synthetic cards, no "no games today" copy.
- The empty state is honest by design. Calendar cells that point at dates
  with no games render nothing below the strip.
- Mock fallbacks (`buildMockScoreboardForDate`, `addMockSections`,
  `buildMockLiveScheduleList`) are forbidden on this screen.

## 7. Game status partitioning

From the normalized scoreboard's `games[].gameStatus`:

| `gameStatus` | Section |
|---|---|
| `1` | Upcoming |
| `2` | Live |
| `>= 3` | Final |

Default if missing: `1` (Upcoming). Each game appears in exactly one
section.

## 8. Stable section identity

Section IDs are stable across requests. The CalendarStrip and each
GameScheduleList slug derive their IDs deterministically from
`(contentSourceId, sectionType[, slug])` via `SectionIdDeriver`. This is
load-bearing — the merge-by-id refresh contract in §9 depends on it.

Examples:

- `server-games-calendar__type-CalendarStrip`
- `stats-api-live-games__type-GameScheduleList`
- `stats-api-scoreboard__type-GameScheduleList__slug-upcomingGames`
- `stats-api-scoreboard__type-GameScheduleList__slug-finalGames`

The id does not change when a section's content changes (e.g. 0 games vs 5
games). It also does not change between today's view and another date's
view — the slot is the same; only its contents differ.

## 9. Parameterized refresh contract

When the CalendarStrip dispatches `onDateSelected`:

1. Renderer writes the new date into `screenState["games_selected_date"]`.
2. Client resolves `paramBindings.date` from that state value.
3. Client calls `/v1/sdui/screen/games?date=YYYY-MM-DD` via the shared
   envelope/transport (screen channel — see `AGENTS.md` §3.8).
4. Server's `ParameterizedRefreshService` invokes the `games` resolver,
   which calls `LiveComposer.composeLive(traceId, locale, date)`.
5. Server returns a **complete** Screen with `id == "games"`.
6. Client checks `response.id == currentScreen.id`. If yes, it fully
   replaces the screen via `replaceCurrentScreen()`. If no, the response
   is dropped with a warning.

Hard rules:

- **Parameterized refresh always returns the same screen it was invoked
  against.** A response with a different `id` is a server contract bug.
  The client refuses to apply it.
- **Full replacement, not merge.** Sections present on the previous screen
  but absent from the response are dropped (e.g. the `upcoming_games`
  section disappears when the user picks a date that has only completed
  games).
- **No "partial fragment" mode.** If a feature wants to refresh a single
  section, it uses the section channel (`/v1/sdui/section/{id}`) with that
  section's own refresh policy. It does not return a partial screen.
- **Forms that re-render after submit** also return the full screen
  (`composeLeadersRefresh` returns `id: "leaders"` with the full Form +
  LeadersTable roster), so the same replacement contract applies.
- **Current-params replay.** Pull-to-refresh, screen-level poll ticks, and
  action-driven `refresh` targeting the current screen all carry the
  screen's current `date` param. The user's selected date survives every
  screen refetch until the user explicitly picks a different date.

## 10. Localization & formatting split

The wire stays canonical (machine-readable); display formatting is the
client's job per platform-native locale realization (per AGENTS.md §7.1).

| Concern | Wire format | Client treatment |
|---|---|---|
| Game date (selected/default) | ISO `YYYY-MM-DD` | Used for state + paging, never displayed verbatim |
| CalendarStrip day-of-week column order | Not on wire | Client derives from `WeekFields.of(Locale.getDefault()).firstDayOfWeek` |
| Weekday header labels ("Sun", "Mon", …) | Not on wire | Client formats via `DayOfWeek.getDisplayName(SHORT, Locale)` |
| Month/year label ("May 2026") | Not on wire | Client formats from `LocalDate` using `Locale` |
| Cell number ("26") | Not on wire | Client renders `LocalDate.dayOfMonth` |
| Game tipoff time ("8:30 pm ET") | Server-provided in `gameStatusText` plus structured time fields | Client may reformat per locale conventions, but the timezone is always ET (NBA convention) |
| Live game clock ("Q3 4:22") | Structured fields + SSE updates | Client renders via LiveClock element |

The CalendarStrip is intentionally locale-realized on the client. The
server does not emit pre-formatted display strings for dates or weekdays.

Game status strings (`status.final`, `status.live`, `period.q1`, …) ride
the standard `stringTable` mechanism that
`SduiUtils.stampStringTableOnSections` stamps onto sections during
composition.

## 11. Update channels & cadence ownership

The Games screen uses both update channels defined in `AGENTS.md` §3.8.
The canonical rules live there; this section documents the Games-specific
cadence assignments.

### Channel usage

| Surface | Channel | Cadence | Mechanism |
|---|---|---|---|
| Full screen (initial load, date change, pull-to-refresh) | Screen (`/v1/sdui/screen/games`) | Manual / on-demand | User action or pull-to-refresh triggers `replaceCurrentScreen` |
| Screen-level poll | Screen (`/v1/sdui/screen/games?date=…`) | `defaultRefreshPolicy` (server-declared; currently static on Games) | Poll timer with current-params replay |
| Live Now section data | Section (SSE via `refreshPolicy.channel`) | Real-time | Ably channel pushes; `dataBinding` applies in place |
| Section re-composition (e.g. game status transitions) | Section (`/v1/sdui/section/{id}`) | `refreshPolicy.sectionEndpoint` poll | Periodic poll; section replaced in place |

### Invariants

- The screen channel always returns a full `Screen` with `id == "games"`.
  The client full-replaces; there is no merge.
- The section channel returns a single `Section` with matching `id`. The
  client replaces only that section.
- A successful screen-channel fetch resets the screen-level poll timer.
- Pull-to-refresh carries the current `date` param (current-params replay
  rule from §3.8).

## 12. Form-factor / paging behavior (client)

- The CalendarStrip uses a horizontal pager with one week per page.
- Initial page is the **0-based** index of the week containing
  `selectedDate`, computed via `weekIndexOf(anchor, selectedDate,
  firstDayOfWeek)`. `weeksBetween(...)` returns a 1-based count and is for
  `totalWeeks`; do not use it for page indices.
- The pager's anchor is `effectiveMinDate(minDate, selectedDate,
  firstDayOfWeek)`, snapped to the previous-or-same `firstDayOfWeek`.
- Cells outside `[minDate, maxDate]` render as empty placeholders.
- All paging math is `LocalDate`-only (no timezone) so the visible window
  stays stable across DST and across the user's device timezone changes.

## 13. Out-of-season / malformed input handling (server)

In `LiveComposer.resolveRequestedDate(override, today)`:

- Null or blank override → use `today`.
- Override fails `LocalDate.parse` → log warn, use `today`.
- Override is before `seasonStart` → log warn, clamp to `today`.
- Override is after `seasonEnd` → log warn, clamp to `today`.

The strip's `minDate`/`maxDate` should already keep the client from picking
invalid dates, but the server defends against malformed `date` params from
any source.

## 14. CalendarStrip date metadata

The CalendarStrip emits a `data.dateMetadata` map alongside the date fields.
Keys are ISO `YYYY-MM-DD` strings; values are objects describing per-date
context for the renderer. Only in-season dates with at least one game
appear; dates outside the season window (`minDate..maxDate`) are filtered
server-side regardless of what upstream returns.

```json
"dateMetadata": {
  "2025-10-22": { "gameCount": 11, "hasTeamGame": false },
  "2025-10-23": { "gameCount": 7,  "hasTeamGame": false }
}
```

Field rules:

- `gameCount` — integer count of league games on that date. Sourced from
  the CDN `scheduleLeagueV2.json` (one request per composition, no per-date
  fan-out).
- `hasTeamGame` — boolean. Reserved for a future favorite-team highlight.
  Currently always `false`; clients should read but not depend on it being
  populated.
- Missing date keys mean "no information"; clients render the cell normally
  without a dot/marker.
- Empty `dateMetadata` (CDN unreachable) is not a composition failure; the
  CalendarStrip still renders with no per-date markers.

The server emits the map; clients realize the visual treatment per platform
(per AGENTS.md §7.1 — neutral semantic, native realization).

## 15. Game-card visual treatment

GameScheduleList rows use a flush, secondary-background treatment owned by
`SectionSurfaces.gameCardFlushSurface()`:

- `cornerRadius: 0`
- `background: "token:nba.bg.secondary"`
- No shadow, no outer padding (the row owns its inner padding).

Row composition (top to bottom, when present):

1. **Status badge** — short status text (`statusText`) rendered as a
   centered badge.
2. **Series row** — `seriesText` (e.g. "ECF · Game 3"), centered, only
   emitted when upstream supplies a non-blank `seriesText`.
3. **Matchup row** — away team column · score · home team column.
4. **Broadcast row** — `broadcastText` (e.g. "ESPN"), centered, only emitted
   when upstream supplies broadcaster information. The composer resolves
   broadcaster text from `broadcasters.nationalTvBroadcasters[0]` or a flat
   `broadcasterText` field.

Series and broadcast strings come from upstream as-is (server-owned
content); clients do not invent fallback copy. Missing fields collapse the
row entirely rather than rendering an empty slot.

## 16. Ad-slot placement

Exactly one `AdSlot` section is inserted into the Games screen per
composition, governed by a server-owned placement rule:

| Total games for selected date | AdSlot |
|---|---|
| 0 | not emitted |
| 1 | inserted after the (single) game section |
| 2+ | inserted after the section containing the **2nd** game in roster order (live → upcoming → final) |

The ad is inserted at a section boundary, never inside a status group. If
the 2nd game falls within a section that holds multiple games, the ad
appears below that whole section.

AdSlot payload:

- `provider: "gam"`
- `adUnitPath: "/nba/games_screen"`
- `sizes: [[320, 50], [728, 90]]`
- `targeting: { section: "games", position: "games_screen" }`
- `surface: SectionSurfaces.adSlotSurface()`
- `collapseOnEmpty: true` — when GAM returns no fill, the section
  visually collapses; the renderer does not invent placeholder copy.
- `placeholder` is server-emitted (label "Advertisement", tertiary bg
  token). No client-bundled ad creative.

Dimensions are payload-owned. Per AGENTS.md §6.4, clients never invent ad
reservation sizes.

## 17. Cross-cutting rules these decisions tighten

These rules elaborate or instantiate clauses in `AGENTS.md` rather than
add new doctrine:

- §1.1 Server authority — the default date and the per-date game list are
  server-composed, not client-inferred.
- §3.3 Refresh, polling, live updates — parameterized refresh is a
  server-declared semantic and the client honors the strict same-screen
  contract.
- §3.8 Update channels — the Games screen uses the screen channel for
  date-based re-composition and the section channel for live-game SSE.
- §3.6 Composers emit design-system tokens — the CalendarStrip's chrome
  (gaps, padding, colors) flows through tokens; date numbers stay raw
  because they're computed from `LocalDate` at render time.
- §4.1 One fetch path — `SduiRepository.fetchScreen` carries the
  parameterized refresh; no hand-rolled URLs.
- §7.1 Platform-native realization of neutral semantics — date display,
  weekday labels, and pager mechanics are client-realized; the server only
  emits ISO dates and the season window.
- §8 Fallback doctrine — empty state is renderability-preserving; mocks
  that invent meaning are forbidden.
