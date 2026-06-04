# Fix: `Section.data` discriminator (Draft-07 `if`/`then`/`else`)

**Status:** Shipped
**Surfaced by:** [server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java](server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java) (commit `1184511`)
**Scope:** `schema/sdui-schema.json` only. Composers already conformed; client codegen diff was docstring-only (18 lines across all three platforms).

## Shipped solution (summary)

Replaced `Section.data`'s `oneOf` with the pair:

- **`anyOf`** of the same 11 `*Data` `$ref`s — keeps quicktype's reachability
  walker happy so every `*Data` definition stays in the schema graph (without
  this, dropping `oneOf` would orphan all 11 definitions and quicktype would
  delete ~5275 lines of client model code, including every `*Data`
  field). `anyOf` validates as long as ≥1 branch matches, so it does NOT
  raise the original "valid to N of M" ambiguity error.
- **`allOf`** chain of `if`/`then` clauses keyed off `Section.type` — the
  real discriminator. Each clause only enforces its `data` sub-schema when
  `type` matches the corresponding `const`.

In-test `stripSectionDataOneOf` workaround removed. Conformance test
(`SchemaConformanceTest`) now validates the full schema as-shipped, 167/167
green on the server suite.

## Problem

`Section.data` is modeled as a `oneOf` over 11 variants
([schema/sdui-schema.json](schema/sdui-schema.json#L1032-L1045)):

```json
"data": {
  "description": "Section-specific data payload",
  "oneOf": [
    { "$ref": "#/definitions/TabGroupData" },
    { "$ref": "#/definitions/BoxscoreTableData" },
    { "$ref": "#/definitions/CalendarStripData" },
    { "$ref": "#/definitions/CalendarMonthListData" },
    { "$ref": "#/definitions/FormData" },
    { "$ref": "#/definitions/AdSlotData" },
    { "$ref": "#/definitions/LeadersTableData" },
    { "$ref": "#/definitions/SubscribeBannerData" },
    { "$ref": "#/definitions/SubscribeHeroData" },
    { "$ref": "#/definitions/AtomicCompositeData" },
    { "$ref": "#/definitions/VideoPlayerData" }
  ]
}
```

`oneOf` requires the payload to validate against **exactly one** branch. In
practice the `*Data` variants are loose enough that multiple branches accept
the same JSON, so validation fails with messages like:

```
$.sections[N].data: must be valid to one and only one schema,
but 2 are valid with indexes 'X, Y'
```

The root cause is that the schema has no discriminator binding `Section.type`
(enum: `TabGroup`, `BoxscoreTable`, `CalendarStrip`, `CalendarMonthList`,
`Form`, `AdSlot`, `SeasonLeadersTable`, `SubscribeBanner`, `SubscribeHero`,
`AtomicComposite`, `VideoPlayer`) to its matching `*Data` definition. JSON
Schema Draft-07 has no `discriminator` keyword (that's OpenAPI 3 / Draft-2020
`discriminator`); the idiomatic Draft-07 expression is `allOf` + chained
`if`/`then`.

Today's workaround: `SchemaConformanceTest.stripSectionDataOneOf` patches the
schema in-memory to make `Section.data` a permissive `object`, so the
structural-conformance check stops at the `data` boundary. Everything outside
`Section.data` is fully validated.

## Fix (as shipped)

Replace the `oneOf` on `Section.data` with `anyOf` (codegen reachability) +
an `allOf` chain of `if`/`then` clauses keyed off `type` (real enforcement).
The shape of each `if`/`then` clause is:

```json
{
  "if":   { "properties": { "type": { "const": "TabGroup" } } },
  "then": { "properties": { "data": { "$ref": "#/definitions/TabGroupData" } } }
}
```

Full replacement for the `Section` definition's `data` property + sibling `allOf`:

```json
"data": {
  "description": "Section-specific data payload. The variants are listed via anyOf so codegen reaches every *Data definition; per-variant enforcement is the allOf/if/then chain below (discriminated by Section.type).",
  "anyOf": [
    { "$ref": "#/definitions/TabGroupData" },
    { "$ref": "#/definitions/BoxscoreTableData" },
    { "$ref": "#/definitions/CalendarStripData" },
    { "$ref": "#/definitions/CalendarMonthListData" },
    { "$ref": "#/definitions/FormData" },
    { "$ref": "#/definitions/AdSlotData" },
    { "$ref": "#/definitions/LeadersTableData" },
    { "$ref": "#/definitions/SubscribeBannerData" },
    { "$ref": "#/definitions/SubscribeHeroData" },
    { "$ref": "#/definitions/AtomicCompositeData" },
    { "$ref": "#/definitions/VideoPlayerData" }
  ]
}
```

plus, as a sibling of `properties` on the `Section` definition:

```json
"allOf": [
  { "if": { "properties": { "type": { "const": "TabGroup" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/TabGroupData" } } } },
  { "if": { "properties": { "type": { "const": "BoxscoreTable" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/BoxscoreTableData" } } } },
  { "if": { "properties": { "type": { "const": "CalendarStrip" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/CalendarStripData" } } } },
  { "if": { "properties": { "type": { "const": "CalendarMonthList" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/CalendarMonthListData" } } } },
  { "if": { "properties": { "type": { "const": "Form" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/FormData" } } } },
  { "if": { "properties": { "type": { "const": "AdSlot" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/AdSlotData" } } } },
  { "if": { "properties": { "type": { "const": "SeasonLeadersTable" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/LeadersTableData" } } } },
  { "if": { "properties": { "type": { "const": "SubscribeBanner" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/SubscribeBannerData" } } } },
  { "if": { "properties": { "type": { "const": "SubscribeHero" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/SubscribeHeroData" } } } },
  { "if": { "properties": { "type": { "const": "AtomicComposite" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/AtomicCompositeData" } } } },
  { "if": { "properties": { "type": { "const": "VideoPlayer" } } },
    "then": { "properties": { "data": { "$ref": "#/definitions/VideoPlayerData" } } } }
]
```

Notes on the mapping:

- `SeasonLeadersTable` (the enum value on `Section.type`) maps to
  `LeadersTableData` (the definition name). Every other pair is a direct
  `<TypeName>` → `<TypeName>Data` match.
- `AtomicComposite` already wraps the generic atomic UI tree; its branch keeps
  the existing `AtomicCompositeData` definition intact.

### Why `allOf` + `if`/`then`, not `oneOf`

- **`if`/`then` is per-clause.** Each clause only enforces its own
  `data` schema when `type` matches. There's no cross-branch interaction,
  so loose `*Data` shapes can't accidentally cross-validate.
- **`else` is unnecessary.** The `type` enum on `Section` already constrains
  the value to the 11 known kinds, so a missing `else` cannot leak unknown
  types past validation.
- **No new keywords.** `allOf`/`if`/`then` are all Draft-07 native — no
  validator-library upgrade required (`networknt/json-schema-validator`
  1.4.0 supports them).

## Roll-out

1. Apply the schema change above to [schema/sdui-schema.json](schema/sdui-schema.json).
2. Run `make codegen` per AGENTS.md §1.2 / §10.4 so generated Java/Kotlin/
   Swift/TS models stay in sync. The discriminator block uses no new fields,
   so codegen output should not move except for the dropped `oneOf` in any
   description text.
3. Copy updated token JSON files to client bundles per AGENTS.md §10.4 (no
   change here since this fix is in the main schema, not in `*-tokens.json`).
4. Delete `SchemaConformanceTest.stripSectionDataOneOf` and the call site
   in `setup()`; the test will then validate `Section.data` against the
   correct per-`type` sub-schema with no in-test patching.
5. Re-run `./gradlew test --rerun-tasks` from `server/`.

## Expected fallout

The conformance test will now exercise every `*Data` variant for real. Two
classes of result are possible:

- **Clean green** — composers already emit shapes that match their
  declared `*Data` definitions. No further work.
- **Composer drift** — one or more composers emit a payload that doesn't
  match its declared `*Data` definition (e.g. missing required field,
  unexpected key shape, wrong nesting). Each surfaced violation is a
  pre-existing bug — the fix is a composer change, not a schema relaxation.
  Per AGENTS.md §1.2 the schema is the wire contract; composers conform to
  the schema, not the other way around.

If the second case dominates, budget composer fixes alongside the schema
change in the same PR — the conformance test must stay green on `main`.

## Out of scope

- Tightening the `*Data` definitions themselves (e.g. adding
  `additionalProperties: false`, locking down array shapes). That's a
  separate hardening pass; the discriminator fix alone is enough to make
  `oneOf` ambiguity disappear.
- Migrating composers from `ObjectNode` to generated typed POJOs. That's
  Phase A3's remaining deferred work (see
  [docs/plans/server/plan-server-saf-codegen-port-readiness.md](docs/plans/server/plan-server-saf-codegen-port-readiness.md)),
  gated on `AtomicCompositeBuilder` strategy.

## References

- AGENTS.md §1.2 (schema as the wire contract)
- AGENTS.md §10.4 (documentation sync after schema changes)
- [docs/plans/server/plan-server-saf-codegen-port-readiness.md](docs/plans/server/plan-server-saf-codegen-port-readiness.md) — Phase A3
- Commit `1184511` — schema-conformance test that surfaced the defect
- [server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java](server/src/test/java/com/nba/sdui/contract/SchemaConformanceTest.java)
