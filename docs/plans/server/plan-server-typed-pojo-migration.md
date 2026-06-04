# Plan — Server typed-POJO migration (A3 unblock)

**Owner:** server
**Status:** Proposed
**Parent plan:** [plan-server-saf-codegen-port-readiness.md](plan-server-saf-codegen-port-readiness.md) — Phase A3, deferred composer-migration bullets
**Predecessors:** A3 step 1 (commit `b884cda`) + A3 step 2 (commit `1184511`) +
schema discriminator fix (commit `126f4c8`)

## Goal

Eliminate `ObjectNode` from SDUI composition. Composers, the
`AtomicCompositeBuilder`, and every helper that today returns `ObjectNode` /
`JsonNode` move to the generated POJOs (`Screen`, `Section`, `AtomicElement`,
`AtomicCompositeData`, `BoxscoreTableData`, `CalendarStripData`,
`LeadersTableData`, …) checked into
`codegen/build/generated-sources/jsonschema2pojo/com/nba/sdui/models/generated/`.

End state:

- `AtomicCompositeBuilder` returns `Section` / `AtomicElement` / `Spacing` /
  `Action` from every public method.
- Every composer's `composeXxx(...)` method returns `Screen` (was `ObjectNode`
  / `JsonNode`).
- `SduiCompositionService` and `SduiController` propagate `Screen` to the
  envelope without `treeToValue` / `valueToTree` round-trips.
- `BoxscoreComposer` and `CalendarComposer` (which today bypass the builder
  and hand-roll raw `ObjectNode`) build their `*Data` payloads from typed
  POJOs.
- `ObjectMapper` + raw Jackson tree usage survives only at: the wire boundary
  (Jackson serializes the typed `Screen` for the response), upstream JSON
  decode (`StatsApiAdapter` reading `JsonNode` from the stats API is fine —
  that's parsing untrusted JSON, not generating known-shape payloads), and
  real-time `dataBinding` / `Tokens` registry loading (loading static JSON
  config). Composition itself contains zero ObjectNode.

## Why one-shot, doctrine-aligned

[AGENTS.md](../../AGENTS.md) §1.4 ("one owner per concern") and §1.2 ("schema
is the contract") both point the same direction: the generated POJOs are
the source of truth, the schema-conformance test (`SchemaConformanceTest`,
commit `1184511`) confirms they faithfully describe the wire shape, and
ObjectNode-based composition is legacy state that shadows that contract.

A "dual-emit" approach (typed siblings beside ObjectNode methods) would
double the builder's API surface for the duration of the migration and
create two parallel impls that must stay in sync — the §1.4 anti-pattern.
We pay the rewrite cost once and end with a single owner.

The size objection (~9000 LOC across builder + composers) is the *file*
total. The actual edit surface is mechanical:

- 67 builder method bodies: swap `om.createObjectNode().put(...)` for typed
  POJO setters / `withX(...)` builders. Field names match JSON keys 1:1
  (jsonschema2pojo convention), so the rewrite is line-for-line.
- ~190 composer call sites: change `ObjectNode foo = atomicBuilder.x(...)`
  to `AtomicElement foo = atomicBuilder.x(...)` (one symbol change), plus
  translate downstream `foo.set("k", v)` mutations to `foo.setK(v)` /
  `foo.withK(v)`.
- 2 hand-rolled composers (Boxscore, Calendar) get rewritten in the same
  style.

**Reviewability is preserved** by structuring the work as a stack of small
commits on one branch (one per composer + one for the builder + one for
each hand-rolled composer + one for the entry-point return-type flip), so
each commit's diff is reviewable in isolation even though the whole stack
lands together.

## Inventory

### Builder API surface (`AtomicCompositeBuilder.java`, 3434 LOC)

**Public methods returning ObjectNode** (67 total — 62 unique names + overloads):

| Tier | Returns today | Returns after | Methods |
|------|---------------|---------------|---------|
| Section composites (build full `Section`) | `ObjectNode` (Section envelope) | `Section` | `buildErrorState`, `buildSectionHeader`, `buildPromoBanner` (×2), `buildContentRail`, `buildFollowingRail`, `buildDisplayGrid`, `buildHeroPanel`, `buildGamePanelComposite` (×2), `buildVideoCarousel`, `buildStatLine`, `buildStatLineFromNodes`, `buildNbaTvSchedule`, `buildVodPlaylist`, `buildGameCarousel`, `buildStoryCircleRail`, `buildCinematicHeroCarousel`, `buildOverlayStoryRail`, `buildEditorialOverlayRail`, `buildFeaturedLiveGameHero` (×2), `buildSectionHeaderComposite`, `buildAppBarHeaderComposite`, `buildVariantChipsComposite`, `buildUtilityCardGrid`, `buildLeagueCardRail`, `buildGameScheduleRow` (×2), `buildGameScheduleList` (×3), `buildMediaOverlayCard`, `wrapAsComposite` — **34 methods** |
| Section helpers | `ObjectNode` | `Section` | `sectionEnvelope` (×2), `sectionEnvelopeWithDerivedId` — **3 methods** |
| Atomic primitives (UI tree nodes) | `ObjectNode` | `AtomicElement` | `container`, `variantContainer`, `heroContainer`, `groupedContainer`, `responsiveRow`, `text`, `image` (×2), `variantImage`, `thumbnailImage`, `button`, `spacer` (×2), `hSpacer`, `liveBadge`, `durationBadge`, `monospacedDigits`, `wrapUi`, `sectionSlot`, `badge`, `opacity`, `textAlign`, `showIndicators`, `shadow` (×2), `shadows`, `shadowWithType`, `backgrounds` — **27 methods** |
| Layout/struct values | `ObjectNode` | `Spacing` | `padding` — **1 method** |
| Action values | `ObjectNode` | `Action` | `tapNavigate` — **1 method** |
| Action arrays | `ArrayNode` | `List<Action>` | `singleActionArray` — **1 method** |
| Mutators (void) | mutates `ObjectNode` arg | mutates typed arg | `setFlex`, `widthMode`, `heightMode`, `minWidth`, `maxWidth`, `minHeight`, `maxHeight`, `layoutWrap`, `crossAxisGap`, `alignSelf`, `attachSectionStates`, `attachRefreshPolicy` — **12 methods** |

**Private helpers also returning ObjectNode** (must move with their callers):
`buildContentCard`, `buildFollowingItem`, `teamColumn`, `statusCell`,
`liveClockCell`, `buildGamePanelContent`, `buildVideoCard`, `buildStatRow`
(× variants), `buildCompactStatRow`, `pillBadge`, `buildNbaTvSlot`,
`buildVodRow`, `vodDivider`, `cinematicHeroSlide`, `overlayStoryCard`,
`appBarIconButton`, `mediaOverlayIconButton`, `mediaBottomScrimGradient`,
`storyCircleItem`, `editorialOverlayCard`, `heroOverflowButton`,
`featuredLiveGameHeroCard`, `heroScoreStrip`, `heroTeam`, `utilityCard` —
**~25 methods**.

### Composer call sites (190 total across 8 builder-using composers)

| Composer | LOC | `atomicBuilder.` calls | Hand-rolled? |
|----------|-----|------------------------|--------------|
| `BoxscoreComposer` | 346 | 0 | **Yes** — builds raw `ObjectNode` directly |
| `CalendarComposer` | 147 | 0 | **Yes** — builds raw `ObjectNode` directly |
| `ScheduleComposer` | 299 | 2 | No |
| `LiveComposer` | 747 | 4 | No |
| `ScoreboardComposer` | 293 | 6 | No |
| `GameDetailComposer` | 1225 | 22 | No |
| `ForYouComposer` | 707 | 23 | No |
| `WatchComposer` | 700 | 37 | No |
| `HomeComposer` | 494 | 39 | No |
| `DemoScreenComposer` | 1370 | 60 | No |

### Composer entry points (12 public `composeXxx`)

| Composer | Method | Returns today | Returns after |
|----------|--------|---------------|---------------|
| `LiveComposer` | `composeLive(traceId, locale[, dateOverride])` | `ObjectNode` | `Screen` |
| `HomeComposer` | `composeHome(traceId, locale)` | `JsonNode` | `Screen` |
| `ForYouComposer` | `composeForYou(...)` | `JsonNode`/`ObjectNode` | `Screen` |
| `WatchComposer` | `composeWatch(...)` | `JsonNode`/`ObjectNode` | `Screen` |
| `ScoreboardComposer` | `composeScoreboard(...)` | `JsonNode`/`ObjectNode` | `Screen` |
| `ScheduleComposer` | `composeSchedule(...)` | `JsonNode`/`ObjectNode` | `Screen` |
| `GameDetailComposer` | `composeGameDetail(gameId, ...)` | `JsonNode`/`ObjectNode` | `Screen` |
| `BoxscoreComposer` | `composeBoxscore(gameId, traceId, locale)` | `JsonNode`/`ObjectNode` | `Screen` |
| `CalendarComposer` | `composeCalendar(...)` (×2 overloads) | `JsonNode`/`ObjectNode` | `Screen` |
| `DemoScreenComposer` | `composeDemos(...)`, `composeLeaders(...)` | `JsonNode`/`ObjectNode` | `Screen` |

### Downstream callers (must accept `Screen`)

- `SduiCompositionService` (12 `composeXxx` methods, returns `JsonNode` today)
- `SduiController` (12 controller methods, builds `ResponseEnvelope<JsonNode>`
  from compose results)
- `ResponseEnvelope` — already typed-friendly (Jackson serializes `Screen`
  the same as `JsonNode`); call sites switch to `ResponseEnvelope<Screen>`.
- `AtomicCompositeBuilderFeedModulesTest` (one builder unit test) — test
  becomes type-checked, asserts move from `node.path("type").asText()` to
  `screen.getType()`-style.

### Test surface (167 tests must stay green)

- `ComposerRoundTripTest` — golden byte-for-byte comparison. **Primary
  drift detector.** Each composer migration must produce identical wire
  bytes.
- `SchemaConformanceTest` — already validates the schema contract on every
  composed screen. Will keep validating as we rewrite.
- `ScreenChannelContractTest`, `SectionChannelContractTest` — controller
  integration; should be unaffected because Jackson serializes `Screen` to
  the same wire JSON.
- `AtomicCompositeBuilderFeedModulesTest` — direct builder unit test;
  rewritten as part of the builder migration commit.

## Strategy

### Branch and commit shape

One branch (`feature/arobinson/typed-pojo-migration` or similar). Series of
focused commits, each independently green:

```
1. Migrate AtomicCompositeBuilder to typed return types
2. Migrate ScheduleComposer (smallest builder client, 2 sites)
3. Migrate LiveComposer (4 sites)
4. Migrate ScoreboardComposer (6 sites)
5. Migrate GameDetailComposer (22 sites)
6. Migrate ForYouComposer (23 sites)
7. Migrate WatchComposer (37 sites)
8. Migrate HomeComposer (39 sites)
9. Migrate DemoScreenComposer (60 sites)
10. Rewrite BoxscoreComposer (hand-rolled, 346 LOC)
11. Rewrite CalendarComposer (hand-rolled, 147 LOC)
12. Flip SduiCompositionService + SduiController return types to Screen
13. Drop ObjectMapper composition fields where no longer needed
```

**Each commit must leave `./gradlew test --rerun-tasks` 167/167 green.**
Round-trip and conformance tests are the safety net.

### Per-step pattern

Every commit follows the same pattern:

1. Run round-trip test, capture green baseline.
2. Apply the type change.
3. Run round-trip test. Failures = drift; fix in the same commit before
   moving on.
4. Run full suite.
5. Commit with a message describing exactly what migrated and what stayed
   ObjectNode (during the migration, intermediate states are mixed; that's
   OK as long as the wire bytes don't change).

### Bridging between commits

Because builder migrates first (commit 1) but composers still call it
through commit 9, **the builder must temporarily expose `ObjectNode`-shaped
adapters** for the duration. Pattern:

```java
// commit 1 — public surface returns typed POJOs:
public Section buildContentRail(...) { /* typed impl */ }

// composer-side bridge (lives in the *composer* during its migration window,
// removed when the composer migrates):
ObjectNode rail = objectMapper.valueToTree(atomicBuilder.buildContentRail(...));
```

The bridge is a one-line `valueToTree` shim, **inside the composer**, not
inside the builder. It vanishes when the composer migrates. By commit 9
every bridge is gone; commit 12 removes any remaining ObjectMapper plumbing
that fell out of use.

This avoids the dual-API anti-pattern: `AtomicCompositeBuilder` has one
typed surface from commit 1 onward; ObjectNode appears only in
to-be-deleted composer-local glue.

### What about typed `Section.data`?

The schema discriminator fix (commit `126f4c8`) keyed `Section.data` to
`Section.type` via `allOf`+`if`/`then`, but jsonschema2pojo emits
`Section.data` as `Object` because Draft-07 conditionals don't translate
to a static type. **Composer code sets `section.setData(typedDataObject)`
where `typedDataObject` is the concrete `*Data` POJO** (`AtomicCompositeData`,
`BoxscoreTableData`, etc.). Jackson serializes the typed POJO into the
`Object` slot correctly. The compiler doesn't enforce the type/data
relationship — that's where `SchemaConformanceTest` continues to earn its
keep.

### What about `AtomicElement` polymorphism?

`AtomicElement` is a single flat class with every variant's fields union'd
(jsonschema2pojo flattens `anyOf` element types). The `type` field
(enum `text` | `image` | `container` | …) discriminates which fields are
relevant. Composer code sets `type` + the type-relevant fields and leaves
the rest null; Jackson with `@JsonInclude(NON_NULL)` (the codegen default)
serializes only the populated fields — same wire output as today. No code
gymnastics required.

### What about `Object`-typed token-or-struct unions?

Fields like `AtomicElement.gap`, `background`, `width/height`, `aspectRatio`
are `Object` because the schema accepts either a token string
(`"token:nba.spacing.lg"`) or a structured `LayoutScalar` / `Background`
POJO. Composers set them to a `String` token (most common today) or to
the typed struct. Jackson serializes each correctly.

## Acceptance criteria

- `grep -rn "ObjectNode\|JsonNode" src/main/java/com/nba/sdui/domain/` returns
  matches only in: parsing helpers that consume upstream stats-API JSON
  (`StatsApiAdapter` and friends, where input is genuinely untrusted JSON),
  the `Tokens` registry loader (loads static JSON), and `dataBinding` /
  real-time payload paths (real-time messages are opaque per AGENTS.md §3.3
  / §4.4).
- `AtomicCompositeBuilder.java` declares no method returning `ObjectNode`
  or `ArrayNode` — every public + private method returns a generated POJO,
  `Spacing`, `Action`, `List<Action>`, or `void`.
- `composeXxx` methods on every composer return `Screen`.
- `SduiCompositionService` returns `Screen` (not `JsonNode`) from each
  `composeXxx` method; `SduiController` builds `ResponseEnvelope<Screen>`.
- `ComposerRoundTripTest` is green (byte-for-byte wire output unchanged).
- `SchemaConformanceTest` is green.
- Full server suite ≥ 167 tests, 0 failures.
- No `objectMapper.valueToTree(...)` or `objectMapper.treeToValue(...)`
  remains in composition code (search constraint).

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Generated POJO `withX(...)` builders return `this`, but `setX(...)` voids — easy to chain `setX` and accidentally drop values | Use `withX(...)` for chained construction; reserve `setX` for single mutations. Code review checks. |
| `AtomicElement` field name mismatches (e.g. `crossAlignment` JSON field vs camelCase) | jsonschema2pojo names match JSON 1:1 by default; verify by reading generated source per migration commit. |
| `Section.data: Object` lets us assign the wrong `*Data` type by accident | `SchemaConformanceTest` catches this immediately because the discriminator fix now enforces per-`type` data shape. |
| Round-trip test golden was captured with ObjectNode field-ordering quirks | If Jackson serializes typed POJOs in a different field order, regenerate the golden in the same commit and call out the diff in the commit message. (`@JsonPropertyOrder` on generated classes should preserve order; verify.) |
| Hand-rolled `BoxscoreComposer` / `CalendarComposer` use shapes we haven't modeled in `BoxscoreTableData` / `CalendarStripData` | Schema-conformance test would catch this today; if any field is missing, fix the schema first (its own micro-PR, then re-run codegen). |
| Public method count temporarily looks like dual API (commit 1 has typed builder + composers still calling typed methods through ObjectNode bridges) | Bridge is a composer-local one-liner per call site, **not** a duplicate builder method. The builder has one surface from commit 1. |

## Out of scope

- Schema tightening beyond what's already shipped (`additionalProperties:
  false`, locking down array shapes inside `*Data` variants). Separate hardening pass.
- Migrating `Tokens` registry, `dataBinding`, or real-time payload handling
  off ObjectNode — these are genuine "parse opaque JSON" call sites, not
  composition.
- Touching `integration-models` / `integration-clients` / WECS port seam
  (A2d, separately deferred).
- Any client-side change. The wire format is unchanged by definition;
  client codegen models are already regenerated per
  [AGENTS.md](../../AGENTS.md) §10.4 each schema change.

## References

- [AGENTS.md](../../AGENTS.md) §1.2 (schema is the contract), §1.4
  (one owner per concern), §10.4 (post-codegen sync)
- [docs/plans/server/plan-server-saf-codegen-port-readiness.md](plan-server-saf-codegen-port-readiness.md) — Phase A3 deferred items
- [docs/fixes/schema-section-data-discriminator.md](../fixes/schema-section-data-discriminator.md) — the
  conformance backstop that makes typed `Section.data` safe
- Commits `b884cda` (POJOs on classpath), `1184511` (conformance test),
  `126f4c8` (discriminator)
- [server/src/main/java/com/nba/sdui/domain/AtomicCompositeBuilder.java](../../server/src/main/java/com/nba/sdui/domain/AtomicCompositeBuilder.java)
- [server/src/main/java/com/nba/sdui/domain/composer/](../../server/src/main/java/com/nba/sdui/domain/composer/)
- [codegen/build/generated-sources/jsonschema2pojo/com/nba/sdui/models/generated/](../../codegen/build/generated-sources/jsonschema2pojo/com/nba/sdui/models/generated/)
