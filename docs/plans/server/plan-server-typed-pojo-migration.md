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
`AtomicComposite`, `BoxscoreTable`, `CalendarStrip`,
`SeasonLeadersTable`, …) checked into
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

## Verification of structural assumptions

A pre-flight pass verified the assumptions this plan rests on so we don't
discover surprises mid-migration:

- ✅ **`ResponseEnvelope<T>` is already generic** ([server/src/main/java/com/nba/sdui/controller/ResponseEnvelope.java](../../server/src/main/java/com/nba/sdui/controller/ResponseEnvelope.java)).
  The flip from `ResponseEnvelope<JsonNode>` → `ResponseEnvelope<Screen>`
  is a parameter change, not a record change.
- ✅ **`@JsonPropertyOrder` is on every generated POJO** (`Screen`,
  `Section`, `AtomicElement` confirmed; codegen-uniform). Wire field order
  is deterministic across `treeToValue` round-trips.
- ⚠️ **`ComposerRoundTripTest` is NOT a byte-for-byte golden test.** It is
  targeted field assertions (parameterized-refresh date echo, form-state
  echo, refresh-endpoint URLs). The plan originally claimed otherwise. The
  real safety net is **`SchemaConformanceTest`** (structural correctness)
  + the targeted assertions in `ComposerRoundTripTest` /
  `ScreenChannelContractTest`. That's enough — schema conformance catches
  missing required fields, type mismatches, and enum violations across
  every composed screen. Per-commit verification adds a manual
  pretty-printed-output diff (see Per-step pattern below).
- ❌ **`SectionSurfaces` was missing from the inventory** — 14
  ObjectNode-returning methods that feed `Section.surface`. Migrated
  alongside the builder at the tail of the stack (commit 11) so composers
  bridge it the same way they bridge the builder, rather than forcing
  every still-ObjectNode composer to wrap typed `SectionSurface` values
  with `valueToTree`.
- ✅ **Composer post-call mutation pattern is trivial to translate.**
  The pattern `header.put("contentSourceId", x); header.set("surface", y);`
  on a builder result becomes `header.setContentSourceId(x);
  header.setSurface(y);` on a typed `Section`. No structural complication.

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

### `SectionSurfaces` API surface ([server/src/main/java/com/nba/sdui/domain/SectionSurfaces.java](../../server/src/main/java/com/nba/sdui/domain/SectionSurfaces.java), 14 methods)

All 14 public methods return `ObjectNode` representing a `SectionSurface`
shape, and every caller stores the result into `section.surface` (or
`section.set("surface", ...)` today). Migrating these to return
`SectionSurface` is a small, isolated commit and lets composers call
typed surfaces directly:

`defaultSurface`, `adSlotSurface`, `flushSurface`, `gameCardFlushSurface`,
`secondaryStripSurface`, `stripSurfaceWithoutBackground`, `subscribeSurface`,
`promoCardSurface`, `videoPlayerSurface`, `cardSurface`, `railSurface`,
`sectionHeaderSurface`, `gamePanelSurface` — **14 methods**.

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

- **`SchemaConformanceTest`** — *primary structural backstop.* Validates
  every composed screen against `schema/sdui-schema.json` (Draft-07 +
  discriminator), catching missing required fields, type mismatches,
  enum violations, and (post-discriminator-fix) wrong `*Data` shapes.
- `ComposerRoundTripTest` — *NOT* byte-for-byte golden. Targeted
  assertions on parameterized-refresh date echo, form-state echo, and
  refresh-endpoint URLs. Catches parameterized-refresh regressions; will
  not catch a renamed-but-still-valid field.
- `ScreenChannelContractTest`, `SectionChannelContractTest` — controller
  integration; expected unaffected because Jackson serializes `Screen` to
  the same wire JSON (`@JsonPropertyOrder` on every generated POJO).
- `AtomicCompositeBuilderFeedModulesTest` — direct builder unit test;
  rewritten as part of the builder migration commit.
- **Per-commit manual safety check (not in the test suite)** — before each
  commit, capture the pretty-printed JSON output of every relevant composer
  to a tmp file pre- and post-change, `diff` them, and require the diff to
  be empty (or to be limited to a known-acceptable change like field
  ordering, called out in the commit message). Cheap and sharper than the
  test suite for catching unintentional shape drift.

## Strategy

### Branch and commit shape

One branch (`feature/arobinson/typed-pojo-migration` or similar). Series
of focused commits, each independently green. **Composers migrate first,
builder and surfaces last.** This avoids the dual-API trap: both the
builder and `SectionSurfaces` keep a single `ObjectNode`-returning surface
throughout the composer migrations, and composers bridge with
`objectMapper.treeToValue(node, Section.class)` (or `SectionSurface.class`)
at each call site. Each round-trip surfaces a small, well-defined set of
semantically-transparent wire changes (empty collections, schema-default
scalars, `@JsonPropertyOrder`-driven reordering — see "Expected diff
categories" below); these are reviewed against that allowlist per commit
rather than expected to be byte-identical. Once every composer is typed,
the surfaces commit and the builder commit just delete the `treeToValue`
shims their callers were doing.

```
 1. Migrate ScheduleComposer (2 builder sites). composeSchedule → Screen.
    Bridge each atomicBuilder.* and surfaces.* call with treeToValue.
 2. Migrate LiveComposer (4 sites)
 3. Migrate ScoreboardComposer (6 sites)
 4. Migrate GameDetailComposer (22 sites)
 5. Migrate ForYouComposer (23 sites)
 6. Migrate WatchComposer (37 sites)
 7. Migrate HomeComposer (39 sites)
 8. Migrate DemoScreenComposer (60 sites)
 9. Rewrite BoxscoreComposer (hand-rolled, 346 LOC). composeBoxscore → Screen.
10. Rewrite CalendarComposer (hand-rolled, 147 LOC). composeCalendar → Screen.
11. Migrate SectionSurfaces (14 ObjectNode → SectionSurface methods).
    Small commit — deletes the surface treeToValue shims in every composer.
12. Migrate AtomicCompositeBuilder to typed return types. **Largest commit
    in the stack.** Rewrites the bodies of 67 public methods + ~25 private
    helpers (3434 LOC) from `om.createObjectNode().put("k", v)` to
    `new Section().setK(v)` / `withK(v)`, then deletes the builder
    treeToValue shims in every composer.
13. Flip SduiCompositionService + SduiController to ResponseEnvelope<Screen>.
14. (Follow-up, optional) Add type-safe Section.setData(*Data) overloads
    to enforce the discriminator at compile time. Out of scope for the
    main migration.
```

**Each commit must leave `./gradlew test --rerun-tasks` 167/167 green.**
The safety net per commit: `SchemaConformanceTest` (structural) +
`ComposerRoundTripTest` parameterized-refresh assertions + a manual
pretty-printed-output diff (see Per-step pattern).

### Per-step pattern

Every commit follows the same pattern:

1. Capture pre-change baseline. Add a temporary `@Test` next to
   `ComposerRoundTripTest` that mirrors its setup (mock `Clock`,
   `StatsApiClient`, `SduiUtils`, `SectionSurfaces`,
   `SectionRefreshService`, `ParameterizedRefreshService`,
   `SeasonCalendarService` + `ReflectionTestUtils.setField(composer,
   "schemaVersion", "1.0")`), call `composeXxx` for the relevant composer,
   and write
   `objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)`
   to `/tmp/sdui-pre-<step>.json`. Composers are Spring beans with
   non-trivial dependencies, so a one-shot `main` won't work — the test
   harness is the only realistic mechanic. Delete the temporary test as
   part of the migration commit.
2. Run `./gradlew test --rerun-tasks`, confirm 167/167 green baseline.
3. Apply the type change.
4. Re-run the same temporary test to capture post-change output to
   `/tmp/sdui-post-<step>.json`; `diff` it against the pre-change file.
   The diff is **expected to be non-empty** because round-tripping
   through generated POJOs surfaces three categories of
   semantically-transparent wire changes (see "Expected diff
   categories" below). Confirm every line of the diff falls into one of
   those categories. Any line that doesn't = real semantic drift; fix
   in the same commit.
5. Run `./gradlew test --rerun-tasks`. Failures = drift; fix in the same
   commit before moving on.
6. Drop the `private final ObjectMapper objectMapper;` field if nothing
   in the touched composer file uses it anymore (composer-by-composer
   cleanup, not a single trailing commit).
7. Commit with a message describing exactly what migrated and what stayed
   ObjectNode.

### Expected diff categories

Round-tripping a composer through `treeToValue(node, Screen.class)` /
`treeToValue(node, Section.class)` / `treeToValue(node, SectionSurface.class)`
produces three classes of wire change. **All are semantically
transparent** — confirmed against Android, iOS, and web reference
clients; each uses null-coalescing or safe navigation so absent vs.
empty (or absent vs. schema-default) produces identical behavior.

1. **Empty collections materialize.** jsonschema2pojo emits
   `private List<Action> actions = new ArrayList<>();` (and similarly for
   `subsections` on `Section`, `children` on `AtomicElement`,
   `NavigationItem`, etc.). Combined with Jackson's `NON_NULL`
   inclusion, empty arrays now appear on the wire wherever they used to
   be absent. Diff manifestation: `+ "actions": []`, `+ "subsections":
   []`, `+ "children": []`.
2. **Schema-default scalars materialize.** jsonschema2pojo bakes JSON
   Schema `default` values into Java field initializers
   (`private Boolean pauseWhenOffScreen = true;`,
   `private Double opacity = 1.0;`, `private String label =
   "Advertisement";`, etc.). Round-trip emits the default whether the
   composer set it or not. Diff manifestation: `+ "pauseWhenOffScreen":
   true`, `+ "opacity": 1.0`.
3. **Field reordering.** `@JsonPropertyOrder` on the generated POJO
   overrides whatever order the composer used when building the
   `ObjectNode`. Same keys, same values, different sequence. JSON Schema
   makes object key order non-significant, so this is a no-op for any
   conformant parser.

**Anything outside these three categories is real drift** and must be
investigated before commit. Easiest sieve:

```bash
diff /tmp/sdui-pre-<step>.json /tmp/sdui-post-<step>.json |
  grep -vE '"(actions|subsections|children|pauseWhenOffScreen|opacity|label|aspectRatio|width|hidden|hideOnError|isRunning|monospacedDigits|showIndicators|layoutWrap|sortable|highlighted|starter|required|disabled|collapseOnEmpty|visibility|dwellMs)"'
```

What's left should be empty (modulo `@JsonPropertyOrder`-driven
reordering, which shows as paired add/remove of the same line).

If a future commit introduces a fourth category, document it here
before continuing the migration.

### Bridging between commits

Because **composers migrate first (commits 1-10) while both the builder
and `SectionSurfaces` migrate last (commits 11-12)**, each composer carries
two bridges at every relevant call site for the duration of its commit
window:

```java
// during commits 1-10 — builder and surfaces both still return ObjectNode:
ObjectNode railNode = atomicBuilder.buildContentRail(...);
Section rail = objectMapper.treeToValue(railNode, Section.class);
rail.setSurface(
    objectMapper.treeToValue(surfaces.railSurface(), SectionSurface.class));
rail.setContentSourceId("feed:home");
sections.add(rail);                          // List<Section>

// after commit 11 — surfaces typed; surface bridge gone:
ObjectNode railNode = atomicBuilder.buildContentRail(...);
Section rail = objectMapper.treeToValue(railNode, Section.class);
rail.setSurface(surfaces.railSurface());
rail.setContentSourceId("feed:home");
sections.add(rail);

// after commit 12 — builder typed; both bridges gone:
Section rail = atomicBuilder.buildContentRail(...);
rail.setSurface(surfaces.railSurface());
rail.setContentSourceId("feed:home");
sections.add(rail);
```

The `treeToValue` round-trip is deterministic across runs (every
generated POJO has `@JsonPropertyOrder`), but does **not** produce
byte-identical output to the pre-migration `ObjectNode` build path —
see "Expected diff categories" above for the three classes of
semantically-transparent wire changes the round-trip introduces.
Commits 11 and 12 are the bridge-deletion commits: commit 11 sweeps the
surface bridge across every composer; commit 12 sweeps the builder bridge
and rewrites the 67 builder method bodies.

This avoids the dual-API anti-pattern: `AtomicCompositeBuilder` and
`SectionSurfaces` each have **one** ObjectNode-returning surface through
their respective migration commits, then **one** typed surface afterward.
Never two surfaces in parallel on either component.

### What about typed `Section.data`?

The schema discriminator fix (commit `126f4c8`) keyed `Section.data` to
`Section.type` via `allOf`+`if`/`then`, but jsonschema2pojo emits
`Section.data` as `Object` because Draft-07 conditionals don't translate
to a static type. **Composer code sets `section.setData(typedDataObject)`
where `typedDataObject` is the concrete component POJO** (`AtomicComposite`,
`BoxscoreTable`, etc.). Jackson serializes the typed POJO into the
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

- ObjectNode/JsonNode is permitted only in this allow-list of files
  (carved out as genuine "parse opaque JSON" call sites, not composition):
  - `StatsApiAdapter` and any other upstream-feed adapters consuming the
    stats API JSON
  - `Tokens` registry loader (loads static JSON config at startup)
  - `dataBinding` / real-time payload handlers (real-time messages are
    opaque per [AGENTS.md](../../AGENTS.md) §3.3 / §4.4)
  Search constraint:
  ```
  grep -rn "ObjectNode\|JsonNode" server/src/main/java/com/nba/sdui/domain/ \
    | grep -vE "(StatsApiAdapter|/feed/|TokensLoader|DataBinding|RealtimePayload)\.java"
  ```
  must return zero matches. New legitimate ObjectNode users (rare)
  must be added to the allow-list in this acceptance criterion in the
  same commit.
- `AtomicCompositeBuilder.java` declares no method returning `ObjectNode`
  or `ArrayNode` — every public + private method returns a generated POJO,
  `Spacing`, `Action`, `List<Action>`, or `void`.
- `SectionSurfaces.java` declares no method returning `ObjectNode` — every
  public method returns `SectionSurface`.
- `composeXxx` methods on every composer return `Screen`.
- `SduiCompositionService` returns `Screen` (not `JsonNode`) from each
  `composeXxx` method; `SduiController` builds `ResponseEnvelope<Screen>`.
- `ComposerRoundTripTest` is green (parameterized-refresh + form-state
  assertions hold).
- `SchemaConformanceTest` is green (the *primary* structural backstop).
- For each commit, the manual pre/post pretty-printed output diff was
  empty or limited to documented intentional changes.
- Full server suite ≥ 167 tests, 0 failures.
- No `objectMapper.valueToTree(...)` or `objectMapper.treeToValue(...)`
  remains in composition code (search constraint).

## Risks and mitigations

| Risk | Mitigation |
|------|------------|
| Generated POJO `withX(...)` builders return `this`, but `setX(...)` voids — easy to chain `setX` and accidentally drop values | Use `withX(...)` for chained construction; reserve `setX` for single mutations. Code review checks. |
| `AtomicElement` field name mismatches (e.g. `crossAlignment` JSON field vs camelCase) | jsonschema2pojo names match JSON 1:1 by default; verify by reading generated source per migration commit. |
| `Section.data: Object` lets us assign the wrong `*Data` type by accident | `SchemaConformanceTest` catches this immediately because the discriminator fix now enforces per-`type` data shape. |
| Field ordering changes when going through `treeToValue` round-trip | Verified: every generated POJO has `@JsonPropertyOrder`; Jackson re-serializes deterministically across runs. Per-commit diff against the "Expected diff categories" allowlist catches any deviation outside the three known classes. |
| `treeToValue` round-trip alters wire bytes (empty arrays, schema-default scalars, field reordering) | Confirmed semantically transparent against Android, iOS, and web reference clients (each uses null-coalescing / safe navigation). Documented as the three expected diff categories; per-commit grep against that allowlist is the sieve for real regressions. Payload-size cost is ~1–2% pretty-printed and negligible gzipped. |
| Hand-rolled `BoxscoreComposer` / `CalendarComposer` use shapes we haven't modeled in `BoxscoreTable` / `CalendarStrip` | Schema-conformance test would catch this today; if any field is missing, fix the schema first (its own micro-PR, then re-run codegen). |
| Public method count temporarily looks like dual API (composers using treeToValue against ObjectNode-returning builder + surfaces during commits 1-10) | Bridge is a composer-local one-liner per call site, **not** a duplicate builder/surfaces method. Both `AtomicCompositeBuilder` and `SectionSurfaces` keep one ObjectNode-returning surface through commits 1-10, then flip to one typed surface in commits 11-12. Never two surfaces in parallel on either component. |

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
