# Plan: Aggregation-Layer Demo Features

> Source: Prototype demonstration roadmap — proving CMS/SDUI ownership boundaries,
> capability negotiation, layout-as-composition, version coexistence, translation
> resolution, cursor pagination, and two-tier analytics attribution.

## Summary

Seven additions to the SDUI prototype that collectively demonstrate the
aggregation layer's core value propositions. Each feature is independently
useful, but together they form a coherent demo narrative: a single composition
endpoint can serve platform-unique, version-negotiated, cursor-paginated,
fully-translated responses with transparent content-source attribution —
without forking feeds or requiring client releases.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Section ID derivation | Partial | IDs are manually assigned per composer; no algorithmic derivation or `contentSourceId` field |
| Capability negotiation | Partial | `Capabilities` carries `sse` and `onFocus`; no section-type or atomic-capability filtering |
| Multi-column layout | Partial | Schema supports `Container[direction=row]` + `SectionSlot`; no form-factor branching in composers |
| Per-feed version negotiation | Partial | `schemaVersion` in envelope/response; server always returns latest (see plan-schema-versioning.md) |
| Translation resolution stub | Built | `SduiUtils.stampStringTableOnSections()` resolves locale → fully resolved strings; static map backing |
| Pagination cursor | Gap | `LeadersTableData` has numeric `page`/`pageSize`/`totalRows`; no opaque cursor |
| Analytics attribution | Partial | `analyticsId` on sections; `fireAndForget` actions with `params`; no `contentSourceId` in impression events |

## Requirements Addressed

- [ ] **REQ-1**: Section IDs computed from content-source ID + section type + position, visible in response
- [ ] **REQ-2**: Capability negotiation handshake — client sends a capability tier; server composes to that tier's vocabulary
- [ ] **REQ-3**: Multi-column layout via section composition — same content, phone vs. tablet/XR responses differ by layout
- [ ] **REQ-4**: Per-feed version negotiation — v1 and v2 of the same feed coexist with different section schemas
- [ ] **REQ-5**: Translation resolution stub — aggregation calls a mock resolver; no translation keys in response
- [ ] **REQ-6**: Pagination cursor in a section — opaque cursor token, stable across re-fetches
- [ ] **REQ-7**: Analytics attribution payload — `fireAndForget` impression carries both content-source ID and section ID

---

## Feature 1: Section ID Derivation Logic

### Goal

Make the CMS/SDUI ownership boundary visible in every response. Section IDs
are derived algorithmically from `{contentSourceId}::{sectionType}::{contentSlug}`
so readers of the response can trace each section back to its content origin.

### Design Note: Stable IDs — No Positional Indices

Position-based IDs (`::0`, `::1`, `::2`) break when the server reorders
sections, inserts ad slots, or runs A/B tests — every insertion shifts
all subsequent IDs, which invalidates impression dedup, `dataBinding.target`
references, and client-side section caching.

The derivation adapts to two page archetypes:

**Feed pages** (For You, Watch, Home): Sections come from a content
pipeline where each item already has a unique content-source ID
(`cms:article-42`, `rec:game-lal-bos-20260501`). The `contentSourceId`
+ `sectionType` pair is already unique for most feed items (one article
→ one section), so no disambiguator is needed:

```
cms:article-42::AtomicComposite
rec:game-lal-bos-20260501::AtomicComposite
```

**Named-region pages** (Game Detail, Boxscore): The same content source
(e.g. `stats-api:game-0022500123`) produces multiple sections of the
same type. A **slug** disambiguates — a stable, content-internal name
for the section's role, not a positional index:

```
stats-api:game-0022500123::AtomicComposite::hero-card
stats-api:game-0022500123::AtomicComposite::top-performers
stats-api:game-0022500123::BoxscoreTable::home
```

**Rule:** the slug is only required when the same `contentSourceId` +
`sectionType` pair would otherwise collide. Position is never part of
the ID.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] Add optional `contentSourceId` field to the `Section` definition in `schema/sdui-schema.json`
  ```json
  "contentSourceId": {
    "type": "string",
    "description": "Origin identifier for the content backing this section (e.g. 'cms:article-42', 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution."
  }
  ```
- [ ] Run `make codegen` — verify field appears in Java, TypeScript, Swift, Kotlin outputs

#### Phase 2: Server
- [ ] Add `SectionIdDeriver` utility class — `server/src/main/java/com/nba/sdui/service/SectionIdDeriver.java`
  ```java
  /** Feed items: contentSourceId + sectionType is sufficient. */
  public static String derive(String contentSourceId, String sectionType) {
      return contentSourceId + "::" + sectionType;
  }

  /** Named-region pages: slug disambiguates when same source + type repeats. */
  public static String derive(String contentSourceId, String sectionType, String slug) {
      return contentSourceId + "::" + sectionType + "::" + slug;
  }
  ```
  The two-arg form is the default for feeds; the three-arg form is used
  only when the same `contentSourceId` + `sectionType` would collide.
- [ ] Update `AtomicCompositeBuilder.sectionEnvelope()` to accept optional `contentSourceId` parameter and set it on the section node
- [ ] Update `ForYouComposer` as the demo composer — call `SectionIdDeriver.derive()` for each section, set both `id` and `contentSourceId`
- [ ] Verify in `/sdui/for-you` response: every section has a derived `id` and a visible `contentSourceId`

#### Phase 3: Android
- [ ] No client changes needed — `contentSourceId` is a pass-through field; existing `SduiModels` codegen picks it up

#### Phase 4: Web
- [ ] No client changes needed — TypeScript codegen picks up the new field

#### Phase 5: Tests
- [ ] Server unit test: `SectionIdDeriver.derive("cms:article-42", "AtomicComposite")` → `"cms:article-42::AtomicComposite"` (feed form)
- [ ] Server unit test: `SectionIdDeriver.derive("stats-api:game-001", "AtomicComposite", "hero-card")` → `"stats-api:game-001::AtomicComposite::hero-card"` (named-region form)
- [ ] Integration test: fetch `/sdui/for-you` → every section has non-null `contentSourceId`

---

## Feature 2: Capability Negotiation Handshake

### Goal

Client sends a single capability **tier** (integer) in the request envelope.
Server composes to that tier's vocabulary, proving platform-unique responses
without forking feeds — while adding only one bounded cache dimension instead
of a combinatorial explosion of per-type flags.

### Capability Tier Definitions

| Tier | Section Types | Atomic Elements | Target Clients |
|------|--------------|-----------------|----------------|
| `1` | AtomicComposite | Container, Text, Image, Button, Spacer, Divider | TV, legacy, minimal clients |
| `2` | Tier 1 + TabGroup, Form, SeasonLeadersTable | Tier 1 + ScrollContainer, Conditional, DisplayGrid | Mid-capability clients |
| `3` | Tier 2 + BoxscoreTable, AdSlot, VideoPlayer, SubscribeHero, SubscribeBanner | Tier 2 + OverlayContainer, SectionSlot, LiveClock | Full-capability mobile/web |

Tiers are **cumulative** — tier 3 is a strict superset of tier 2. This makes
the cache key `capabilities[tier]=N` a single integer with 3 values, not a
combinatorial matrix.

### Cache Impact

Adds at most **3 additional cache entries per endpoint** (one per tier).
Compare with the per-type-flag alternative: 2^9 × 2^12 ≈ 2M theoretical
combinations, none of which would see meaningful cache hit rates.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] No wire-schema changes (capability tier is a request-envelope concern, not a response schema concern)

#### Phase 2: Server
- [ ] Add `CapabilityTier` enum — `server/src/main/java/com/nba/sdui/service/CapabilityTier.java`
  ```java
  public enum CapabilityTier {
      BASIC(1, Set.of("AtomicComposite"),
              Set.of("Container", "Text", "Image", "Button", "Spacer", "Divider")),
      STANDARD(2, Set.of("AtomicComposite", "TabGroup", "Form", "SeasonLeadersTable"),
              Set.of("Container", "Text", "Image", "Button", "Spacer", "Divider",
                     "ScrollContainer", "Conditional", "DisplayGrid")),
      FULL(3, Set.of("AtomicComposite", "TabGroup", "Form", "SeasonLeadersTable",
                     "BoxscoreTable", "AdSlot", "VideoPlayer", "SubscribeHero", "SubscribeBanner"),
              Set.of("Container", "Text", "Image", "Button", "Spacer", "Divider",
                     "ScrollContainer", "Conditional", "DisplayGrid",
                     "OverlayContainer", "SectionSlot", "LiveClock"));

      public final int level;
      public final Set<String> sectionTypes;
      public final Set<String> atomicTypes;
      // constructor, fromLevel(int) factory
  }
  ```
- [ ] Extend `SduiRequestContext.Platform.Capabilities` — add `private int tier = 3;` (full capability default)
- [ ] Add `CapabilityFilter` utility — `server/src/main/java/com/nba/sdui/service/CapabilityFilter.java`
  - `filterSections(ArrayNode sections, CapabilityTier tier)` — replaces unsupported section types with an `ErrorState` composite ("Upgrade to view this content")
  - `filterAtomicElements(ObjectNode ui, CapabilityTier tier)` — walks the atomic tree and replaces unsupported element types with a placeholder `Text` node
- [ ] Wire `CapabilityFilter` into `SduiCompositionService` — apply after each composer returns, before response is sent

> **Prototype shortcut:** The post-hoc filter approach (compose maximally,
> then strip) is simpler to implement for the demo but wasteful — the server
> does full work then discards parts. It also risks breaking parent layout
> assumptions when mid-tree elements are replaced with placeholders.
>
> In production, composers should **read the tier and compose to it** from
> the start (the same pattern Feature 4's `ForYouComposerV1` uses).
> The filter is acceptable as a demo-only mechanism that proves the concept
> without requiring every composer to be tier-aware on day one.
- [ ] Add `Makefile` target `make demo-capability-filter` that curls `/sdui/for-you` with `platform[capabilities][tier]=1` and `platform[capabilities][tier]=3` side by side

#### Phase 3: Android
- [ ] Update `RequestEnvelopeBuilder.kt` to include `capabilities[tier]=3` in the envelope
  - Allow override via builder method for testing reduced-capability scenarios

#### Phase 4: Web
- [ ] Update `RequestEnvelopeBuilder.ts` to include `capabilities.tier = 3`
  - Allow override via options parameter

#### Phase 5: Tests
- [ ] Server test: compose with `tier=3` (default) → all sections present
- [ ] Server test: compose with `tier=1` → only AtomicComposite sections remain; semantic sections replaced with ErrorState
- [ ] Server test: compose with `tier=2` → TabGroup/Form/SeasonLeadersTable survive; BoxscoreTable/AdSlot replaced
- [ ] Server test: tier=1 atomic tree has no LiveClock, OverlayContainer, or SectionSlot elements
- [ ] End-to-end: Android sends `tier=1` → filtered response renders without decode errors

### Open Questions
- [ ] Should filtered sections be silently removed, or replaced with an "upgrade" prompt?
- [ ] Should the tier registry live in server code only, or be documented in the schema as a non-normative annex?
- [ ] When a new section type or atomic element is added, which tier does it land in? (Proposal: new types start at tier 3; promotion to lower tiers happens after stability is proven.)

---

## Feature 3: Multi-Column Layout via Section Composition

### Goal

Same content source produces single-column on phone and multi-column on
tablet/XR. Layout is a server composition decision — the client renders
whatever tree it receives without layout branching.

### Output Shape

The wrapper takes N input sections from the flat `sections` array and
replaces them with **one `AtomicComposite` section** whose `data.ui` is a
`Container[direction=row]` with N `SectionSlot` children — each embedding
one of the original sections. This is how `SectionSlot` works: it is an
atomic element inside `data.ui`, not a top-level section wrapper.

Example: 2 sections → 1 AtomicComposite containing 2 SectionSlots.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] No schema changes — `Container[direction=row]` + `SectionSlot` already express multi-column

#### Phase 2: Server
- [ ] Add `MultiColumnWrapper` utility — `server/src/main/java/com/nba/sdui/service/MultiColumnWrapper.java`
  - `wrapIfWideFormFactor(SduiRequestContext ctx, List<ObjectNode> sections, int columnsOnWide)` — when form factor is `tablet`, `web.wide`, or `tv`, replaces the input sections with a single `AtomicComposite` whose `data.ui` is a `Container[direction=row]` containing `SectionSlot` children (one per input section). On `phone` form factor, returns sections unchanged.
  - Each column gets `flex: 1` for equal width distribution
  - Gap between columns uses `token:nba.spacing.md`
- [ ] Apply `MultiColumnWrapper` in `ForYouComposer` — wrap the content rail + editorial rail pair as a 2-column layout on wide form factors
- [ ] Apply in `HomeComposer` — wrap utility card sections as a 2-column grid on tablet
- [ ] Add `Makefile` target `make demo-multi-column` that curls `/sdui/for-you?platform[formFactor]=tablet` and `/sdui/for-you?platform[formFactor]=phone` side by side

#### Phase 3: Android
- [ ] No renderer changes — `SectionSlot` inside Container already works via the bidirectional AtomicRouter ↔ SectionRouter bridge
- [ ] Add form factor detection in `RequestEnvelopeBuilder.kt` — read screen width and send `tablet` when width ≥ 600dp

#### Phase 4: Web
- [ ] No renderer changes — SectionSlot rendering already delegates to SectionRouter
- [ ] Add form factor detection in `RequestEnvelopeBuilder.ts` — send `web.wide` when viewport ≥ 1024px, `web.narrow` below

#### Phase 5: Tests
- [ ] Server test: compose with `formFactor=phone` → sections are sequential (no row containers)
- [ ] Server test: compose with `formFactor=tablet` → target sections are wrapped in Container[direction=row] with SectionSlot children
- [ ] Visual test: render tablet response on web — two columns visible

---

## Feature 4: Per-Feed Version Negotiation

### Goal

v1 and v2 of the same feed coexist. A long-tail TV client on v1 receives a
reduced schema; a modern mobile client on v2 receives the full schema.
Demonstrates version negotiation without feed forking.

> **Note:** Complements `plan-schema-versioning.md` which covers the general
> versioning protocol. This feature focuses on the concrete demo: two versions
> of one feed running simultaneously.

### Interaction With Feature 2 (Capability Tier)

Feature 2 (tier filtering) and Feature 4 (version routing) overlap in output:
both can produce AtomicComposite-only, basic-atomic, static-only responses.
They prove **different concepts** — versioning selects content shape at
composition time; capability tier filters rendering vocabulary post-hoc.

To avoid wasted work (v2 composes full content then tier-1 strips it):

- **Capability filtering applies only when `schemaVersion` ≥ current (2.0).**
  If a client sends `schemaVersion=1.0`, the v1 composer runs and produces
  tier-1-compatible output by design — no filter pass needed.
- A client sending `tier=1` + `schemaVersion=2.0` still gets filtered, which
  is intentional: it means "I understand v2 schema but my renderer can only
  handle basic elements."
- A client sending `tier=3` + `schemaVersion=1.0` gets the v1 composer's
  output unfiltered — the v1 composer only emits tier-1 content anyway.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] No schema changes for the demo — v1 is a subset of the current schema; v2 is the current schema

#### Phase 2: Server
- [ ] Create `ForYouComposerV1` — `server/src/main/java/com/nba/sdui/service/ForYouComposerV1.java`
  - Composes the same `/sdui/for-you` feed using only `AtomicComposite` sections (no TabGroup, no Form, no semantic sections)
  - Omits `LiveClock` elements, `OverlayContainer`, and `DisplayGrid` — uses only Container, Text, Image, Button, Spacer, Divider
  - Omits `dataBinding` and `refreshPolicy.type=sse` — static only
  - Same content, simpler rendering vocabulary
- [ ] Update `SduiCompositionService.composeForYou()` to route based on `schemaVersion`:
  ```java
  public JsonNode composeForYou(SduiRequestContext ctx) {
      if ("1.0".equals(ctx.getSchemaVersion())) {
          return forYouComposerV1.composeForYou(ctx.getTraceId(), ctx.getLocale());
      }
      return forYouComposer.composeForYou(ctx.getTraceId(), ctx.getLocale());
  }
  ```
- [ ] Bump the current `ForYouComposer` response to emit `schemaVersion: "2.0"` (keep `1.0` as the v1 composer's version)
- [ ] Add `Makefile` target `make demo-version-negotiation` that curls with `schemaVersion=1.0` and `schemaVersion=2.0` and diffs the responses

#### Phase 3: Android
- [ ] No client changes for the demo — Android already sends `schemaVersion` in the envelope
- [ ] For the demo, allow overriding `schemaVersion` in a debug settings screen to toggle v1/v2

#### Phase 4: Web
- [ ] No client changes — web already sends `schemaVersion`
- [ ] Add a debug toggle in the web dev toolbar to switch between v1 and v2

#### Phase 5: Tests
- [ ] Server test: `/sdui/for-you` with `schemaVersion=1.0` → response has only `AtomicComposite` sections, no `LiveClock` elements
- [ ] Server test: `/sdui/for-you` with `schemaVersion=2.0` → response includes semantic sections and advanced atomics
- [ ] Server test: both v1 and v2 responses are valid against `sdui-schema.json`

### Dependencies
- Coordinate with `plan-schema-versioning.md` — this demo is a concrete instance of that plan's Phase 2 server routing

---

## Feature 5: Translation Resolution Stub

### Goal

Prove that the aggregation layer resolves all translations at composition time.
No translation keys appear in the response — only fully resolved strings. The
backing resolver is a mock (simulating a CrowdIn integration).

### Tasks

#### Phase 1: Schema & Codegen
- [ ] No schema changes — `stringTable` is already in the Section definition

#### Phase 2: Server
- [ ] Extract `TranslationResolver` interface — `server/src/main/java/com/nba/sdui/service/i18n/TranslationResolver.java`
  ```java
  public interface TranslationResolver {
      Map<String, String> resolve(String locale, Set<String> keys);
      String resolve(String locale, String key);
  }
  ```
- [ ] Create `CrowdInMockResolver` implementing `TranslationResolver` — backed by the existing `STRING_TABLES` static map in `SduiUtils`
- [ ] Refactor `SduiUtils.buildStringTable()` and `getLocalizedString()` to delegate to the injected `TranslationResolver`
- [ ] Add `es` and `fr` demo strings for all current sections (fill gaps in existing map)
- [ ] Log at composition time: `"Resolved {} translation keys for locale '{}' via {}"` — the log makes the mock integration visible
- [ ] Add `Makefile` target `make demo-translations` that curls `/sdui/for-you?locale=es` and `/sdui/for-you?locale=fr` and shows fully resolved strings

#### Phase 3: Android
- [ ] No client changes — strings arrive resolved; client renders them as-is

#### Phase 4: Web
- [ ] No client changes — strings arrive resolved

#### Phase 5: Tests
- [ ] Server test: compose with `locale=es` → all `stringTable` values are Spanish, no raw keys
- [ ] Server test: compose with unknown locale → falls back to English
- [ ] Server test: `CrowdInMockResolver` returns the same results as the previous static-map implementation (migration safety)

---

## Feature 6: Pagination Cursor in a Section

### Goal

Add an opaque cursor token to a paginated section, proving the contract works
inside SDUI's section model. The cursor is stable across re-fetches and the
client never parses it — it echoes it back via `paramBindings` on a `refresh`
action.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] Add `nextCursor` and `previousCursor` to `LeadersTableData` in `schema/sdui-schema.json`:
  ```json
  "nextCursor": {
    "type": "string",
    "description": "Opaque cursor for the next page. Absent on the last page. Client echoes it back via refresh paramBindings."
  },
  "previousCursor": {
    "type": "string",
    "description": "Opaque cursor for the previous page. Absent on the first page."
  }
  ```
- [ ] Run `make codegen`

#### Phase 2: Server
- [ ] Update `DemoScreenComposer.buildLeadersTable()` to emit `nextCursor` and `previousCursor`
  - Encode as Base64: `Base64(lastPlayerId + "|" + sortColumn + "|" + page)`
  - Omit `nextCursor` on the last page; omit `previousCursor` on page 1
- [ ] **Critical: Promote cursors to screen-level `state`.** `paramBindings`
  resolves from **screen state** (`context.state[key]` on web,
  `stateManager.getState(key)` on Android), **not** from section data.
  The refresh response must set cursors in `screen.state` with scoped keys
  to avoid collisions if multiple paginated sections exist:
  ```json
  {
    "state": {
      "leaders-table.nextCursor": "base64...",
      "leaders-table.previousCursor": "base64..."
    }
  }
  ```
  `composeLeadersRefresh()` already writes form values to `screen.state`
  — this follows the same pattern.
- [ ] Update `DemoScreenComposer.composeLeadersRefresh()` to accept a `cursor` param and decode it to derive the page
- [ ] Add `Next Page` / `Previous Page` buttons to the leaders table via `refresh` actions with `paramBindings`:
  ```json
  {
    "trigger": "onActivate",
    "type": "refresh",
    "target": "leaders-table",
    "paramBindings": { "cursor": "leaders-table.nextCursor" }
  }
  ```
  Note: the binding key is `leaders-table.nextCursor` (screen state), not
  bare `nextCursor` (which would collide with other paginated sections).
- [ ] Verify cursor stability: same inputs → same cursor bytes across re-fetches

#### Phase 3: Android
- [ ] No new renderer logic — `SeasonLeadersTable` renderer already supports `refresh` actions
- [ ] No paramBindings changes — resolution already reads from screen-level `StateManager`

#### Phase 4: Web
- [ ] No new component logic — refresh action with `paramBindings` already flows through `fetchSduiScreen`
- [ ] No paramBindings changes — resolution already reads from screen-level `context.state`

#### Phase 5: Tests
- [ ] Server test: first page → `nextCursor` present, `previousCursor` absent
- [ ] Server test: middle page → both cursors present
- [ ] Server test: last page → `nextCursor` absent
- [ ] Server test: decode cursor → same page/sort state as when it was encoded
- [ ] Integration test: click "Next Page" → refresh fires with cursor → new page renders

### Open Questions
- [ ] Should cursor encoding include a version byte for forward-compatibility?
- [ ] Should expired/invalid cursors return an error or silently reset to page 1?

---

## Feature 7: Analytics Attribution Payload

### Goal

Every section's `fireAndForget` impression action carries both the SDUI
section ID (layout identity) and the content-source ID (CMS origin). The
two-tier ID model is load-bearing for analytics — not just rendering.

Clients also append `renderedPosition` (the section's index in the
rendered list) at impression-fire time. This separates **stable identity**
(server-set, survives reorders) from **display position** (client-set,
reflects actual layout). Analytics can answer both "how did article-42
perform?" and "how does position 5 perform?" without one polluting the
other.

### Tasks

#### Phase 1: Schema & Codegen
- [ ] No schema changes — `fireAndForget` actions already have an open `params` object

#### Phase 2: Server
- [ ] Create `AnalyticsAttribution` utility — `server/src/main/java/com/nba/sdui/service/AnalyticsAttribution.java`
  ```java
  public static ObjectNode impressionAction(ObjectMapper om,
                                             String sectionId,
                                             String contentSourceId,
                                             String analyticsId) {
      ObjectNode action = om.createObjectNode();
      action.put("trigger", "onVisible");
      action.put("type", "fireAndForget");
      action.put("event", "section_impression");
      ObjectNode params = om.createObjectNode();
      params.put("sectionId", sectionId);
      params.put("contentSourceId", contentSourceId);
      params.put("analyticsId", analyticsId);
      action.set("params", params);
      ObjectNode impression = om.createObjectNode();
      impression.put("dedup", "once-per-screen");
      action.set("impression", impression);
      return action;
  }
  ```
- [ ] Update `AtomicCompositeBuilder.sectionEnvelope()` — when `contentSourceId` is set, automatically attach an impression action via `AnalyticsAttribution`
- [ ] **Single-owner rule:** `AnalyticsAttribution` is the sole owner of
  `onVisible`/`fireAndForget` impression actions on sections. Composers that
  want attribution pass `contentSourceId` through the builder and must **not**
  manually add duplicate impression actions on the same section. The builder
  should assert (or log a warning) if a section already has an impression
  action when `contentSourceId` is being set.
- [ ] Update `ForYouComposer` sections to pass `contentSourceId` through the builder
- [ ] Verify in `/sdui/for-you` response: every section has exactly one `fireAndForget` impression action with both IDs in `params`

#### Phase 3: Android
- [ ] When firing a `fireAndForget` impression action, append `renderedPosition` (0-based section index in the rendered list) to `action.params` before dispatching
- [ ] This is a small addition to `ImpressionTracker` (per plan-impression-tracking.md) — the tracker already knows the section's index in the LazyColumn
- [ ] Verify fired event params include `sectionId`, `contentSourceId`, and `renderedPosition`

#### Phase 4: Web
- [ ] When firing a `fireAndForget` impression action, append `renderedPosition` to `action.params` before dispatching
- [ ] `useImpressionTracking` already tracks section index via IntersectionObserver entry order — pass it through
- [ ] Verify fired event params include `sectionId`, `contentSourceId`, and `renderedPosition`

#### Phase 5: Tests
- [ ] Server test: sections with `contentSourceId` → impression action has both `sectionId` and `contentSourceId` in params (no `renderedPosition` — that's client-side)
- [ ] Server test: sections without `contentSourceId` → impression action has `sectionId` only
- [ ] Client test (web): scroll through For You → verify `renderedPosition` is appended and matches actual DOM order
- [ ] Client test (Android): scroll through For You → verify `renderedPosition` is appended and matches LazyColumn index
- [ ] Integration test: reorder sections server-side → verify `sectionId` stays stable while `renderedPosition` changes

### Dependencies
- Feature 1 (Section ID Derivation) must land first — provides `contentSourceId` on sections
- `plan-impression-tracking.md` Phase 3 (Android `ImpressionTracker`) — needed for Android impression firing

---

## Cacheability Analysis

The current CDN cache key shape is:
`path + platform[deviceClass] + platform[capabilities] + schemaVersion + locale + experiments[…] + userParams`

Each feature's impact on cache key cardinality:

| Feature | New Cache Dimensions | Cardinality | Verdict |
|---------|---------------------|-------------|--------|
| 1. Section ID derivation | None (response-only field) | **+0** | Cache-neutral |
| 2. Capability tier | `capabilities[tier]` — single integer | **×3** per endpoint | Acceptable; bounded |
| 3. Multi-column layout | `formFactor` — already a dimension | **+0** | Already paid |
| 4. Per-feed versioning | `schemaVersion` — already a dimension | **×2** per endpoint (v1 + v2) | Acceptable; bounded |
| 5. Translation resolution | `locale` — already a dimension | **+0** new dimensions | See note below |
| 6. Pagination cursor | `cursor` user param | **Per-page** (inherently uncacheable) | Acceptable for paginated data |
| 7. Analytics attribution | None (response-only params) | **+0** | Cache-neutral |

**Worst-case multiplicative impact** across all features:
`existing keys × 3 (tiers) × 2 (schema versions) = 6× current footprint`

This is bounded and manageable.

### Feature 5 scaling note

Fully-resolved strings mean zero response sharing across locales. Today 3
locales is fine. At 20+ production locales, every endpoint's cache footprint
multiplies by the locale count with no sharing.

**Prototype stance:** Accept the locale dimension as bounded (3–5 demo
locales). Document that production would likely use one of:
- Edge-level string injection (CDN worker merges a shared structural response with locale-specific string table)
- Separate cacheable string endpoint (`/sdui/strings?locale=es&v=1`) with client-side join
- `Vary: locale` CDN partition with longer TTLs on string tables

### Feature 6 pagination note

Cursor-paginated responses are inherently per-request. This is expected and
acceptable — pagination is a user-initiated action, not a cache-warm path.
The initial (cursorless) page load remains cacheable.

### Rejected alternative: per-type capability flags

The original Feature 2 design used `capabilities[supportedSectionTypes]` and
`capabilities[atomicCapabilities]` as arrays in the query string. This would
create 2^9 × 2^12 ≈ 2M theoretical cache key combinations with near-zero hit
rates. The tier-based approach collapses this to 3 values while still proving
the negotiation concept.

---

## Implementation Order

```
Feature 1 (Section ID Derivation)
    └──→ Feature 7 (Analytics Attribution)  [depends on contentSourceId]
    
Feature 2 (Capability Negotiation)          [independent]

Feature 3 (Multi-Column Layout)             [independent]

Feature 4 (Per-Feed Versioning)             [independent, coordinates with plan-schema-versioning.md]

Feature 5 (Translation Stub)                [independent, mostly refactoring existing code]

Feature 6 (Pagination Cursor)               [independent]
```

**Recommended sequence:**
1. Feature 5 — lowest risk, refactoring existing working code
2. Feature 1 — foundation for Feature 7
3. Feature 6 — self-contained schema + server + demo
4. Feature 7 — builds on Feature 1
5. Feature 3 — needs form-factor detection wired on clients
6. Feature 2 — most invasive server change (filter layer in composition pipeline)
7. Feature 4 — most effort (parallel composer), coordinates with versioning plan

## Cross-Cutting Dependencies

| This Plan | Depends On | Notes |
|-----------|-----------|-------|
| Feature 4 | `plan-schema-versioning.md` | Version routing is a concrete instance of that plan's Phase 2 |
| Feature 7 | `plan-impression-tracking.md` | Android impression firing needs ImpressionTracker |
| Feature 2 | None | Tier registry should inform `plan-schema-versioning.md` capability model; tier ↔ schema version may converge |
| Feature 3 | `plan-layout-responsive.md` | Form-factor detection shares the same envelope field |

## Open Questions

- [ ] Should the demo screen (`/sdui/demos`) showcase all seven features, or should each feature be demonstrated on a real feed screen (`/sdui/for-you`)?
- [ ] Should capability negotiation be opt-in (client sends tier only when it wants filtering) or required (missing tier = full capability assumed)?
- [ ] Should `contentSourceId` be required on all sections or optional? If required, what is the ID for purely composed sections with no external content source?
- [ ] At what locale count does fully-resolved-string caching become a problem worth solving differently? (Proposal: revisit at 10+ locales.)
