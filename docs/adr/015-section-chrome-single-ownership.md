# ADR-015: Section Chrome Single Ownership

- Status: Accepted
- Date: 2026-05-25
- Decision owners: Adrian Robinson (interim), platform leads, backend leads
- Related requirements: `docs/sdui-requirements-summary.md` §9m, design system §2 (Box-model cascade)
- Related ADRs: ADR-008 (superseded), ADR-013 (variant tokens — orthogonal, see relationship below)
- Supersedes: ADR-008 (Form-Factor Layout Manager)

## Decision

Section outer chrome (margin, padding, background, cornerRadius, shadow, border) is owned exclusively by `SectionContainer` reading `Section.surface`. The legacy `SectionLayoutHints` field — which gave the server a parallel path for emitting margins and dividers — is removed from the schema, server composers, and all client renderers.

## Context

ADR-008 introduced `SectionLayoutHints` as a server-side fine-tuning escape hatch alongside structural section composition. At the time, the `layoutHints` envelope was a reasonable hedge: the architecture was young, `Section.surface` did not yet exist as a first-class carrier for section outer chrome, and the team wanted a low-cost way for composers to declare inter-section margins and dividers without inventing a full layout system.

The hedge became redundant. Once `Section.surface` landed — carrying margin, padding, background, cornerRadius, shadow, and border — and `SectionContainer` was wired on iOS, Android, and web as the exclusive consumer of `section.surface`, the same envelope (the section's outer rectangle) had two server-side ways to declare its margins. Three client-side screen shells each had to compose the two paths, stacking `layoutHints.marginTop` additively with `surface.margin.top` with no declared precedence.

This is the architectural drift that AGENTS.md §1.4 (one owner per concern) is designed to prevent: two code paths owning the same concern, with the second path justified only by precedent ("it was here first") rather than by capability the first path lacks.

The cleanup consolidates onto one path: `Section.surface` consumed by `SectionContainer`.

## Decision Drivers

- **One owner per concern (AGENTS.md §1.4).** Section outer chrome should have exactly one ownership path. Two parallel mechanisms — `Section.surface` and `Section.layoutHints` — muddied the section level of the chrome cascade.
- **The cascade has three levels, not four.** The box-model cascade is `Screen.contentInsets` → `Section.surface` → `AtomicBox`. A parallel `layoutHints` mechanism created a shadow fourth level between screen insets and the section surface, with no documented precedence rule for how the two section-level paths composed.
- **Minimal live usage.** An emit-site audit found three composer call sites, of which only one set a non-zero value (`HomeComposer.composeMoreGamesRow` — `marginTop: 16`). The `dividerAbove`/`dividerBelow` fields had zero composer emit sites. The `priority` field had zero composer emit sites and zero client read sites.
- **Redundant client wiring.** Each client wrapped sections in platform-specific `layoutHints`-derived chrome (web: margin `<div>` wrappers; Android: `Spacer` + `HorizontalDivider` blocks; iOS: `VStack { Divider(); ZStack { ... }.padding(top, bottom); Divider() }` blocks). Consolidating onto `SectionContainer` removed ~100 LOC of duplicated wiring across three clients.
- **Wire-contract hygiene (AGENTS.md §11.4).** `priority` was a speculative field with zero usage. Removing it before it hardened into the forever-contract is cheaper than governing an unused field indefinitely.

## Options Considered

### Option A: Promote `priority` to a top-level concern

Keep the `SectionLayoutHints` envelope, drop the unused margin and divider fields, and promote `priority` to a first-class section hint for viewport prioritization or lazy loading.

Pros:
- Preserves a single field from the existing type, avoiding the perception of wasted work.
- Gives future lazy-loading or viewport-priority consumers a ready-made schema field.

Cons:
- `priority` had zero composer emit sites and zero client read sites. Promoting an unused field expands the wire contract without product evidence.
- The shape of a future priority mechanism is unknown — it might be a boolean (`defer`), a positional signal, or something else entirely. Baking in `high`/`normal`/`low` now risks the wrong abstraction (AGENTS.md §11.4).
- Keeping the envelope alive for one field preserves the maintenance surface on all three clients for zero current value.

### Option B: Move dividers under `section.surface`

Express divider-above / divider-below via `Section.surface.border` (an inset top or bottom border on the section surface).

Pros:
- Keeps divider semantics first-class at the section level.
- Reuses the existing `surface.border` schema shape — no new types needed.

Cons:
- `dividerAbove` / `dividerBelow` had zero composer emit sites. Adding a migration path for unused fields is over-engineering.
- The same visual outcome is expressible via atomic composition: a composer that wants an inter-section line emits a top border on the next section's `surface.border`, or places a 1pt `Container` at the section boundary using the existing atomic primitives. No standalone divider mechanism is needed at the section level.
- The cascade doc (`docs/sdui-design-system.md §2`) documents the composition approach as the recommended pattern.

### Option C: Delete `SectionLayoutHints` entirely (chosen)

Migrate the one non-zero `marginTop` site to `Section.surface.margin.top`. Drop the all-zero emit sites. Remove the `SectionLayoutHints` definition from the schema, regenerate codegen, and remove the `layoutHints` read paths from all three client screen shells.

Pros:
- One chrome path: `Section.surface` consumed by `SectionContainer`. No overlap, no precedence ambiguity.
- Removes ~100 LOC of duplicated wrapping logic across three clients.
- Wire contract shrinks — one fewer type definition, one fewer optional field on `Section`.
- Dividers and inter-section spacing remain expressible via the cascade (`surface.margin`, `surface.border`) and atomic composition.

Cons:
- Schema break — clients on schema versions before this change that decode `Section.layoutHints` strictly will fail if they encounter a payload without the field they expect. Mitigated by the schema-version mechanism (see Consequences).
- Composers lose a dedicated "inter-section margin" convenience; they now express margins through `surface.margin`, which is slightly more verbose but strictly more capable (four-edge support, token references).

## Evidence

Concrete repo facts supporting the decision:

- **Schema diff:** Removed the `Section.layoutHints` optional property and the entire `SectionLayoutHints` definition (`marginTop`, `marginBottom`, `dividerAbove`, `dividerBelow`, `priority`) from `schema/sdui-schema.json`.
- **Server emit-site audit:** 3 emit sites pre-cleanup, all in tabbed semantic sections:
  - `HomeComposer.composeMoreGamesRow` — non-zero `marginTop: 16`, migrated to `surface.margin.top: "token:nba.spacing.lg"` (composer sets the margin field directly on the existing `flushSurface()` ObjectNode; per AGENTS.md §3.6 the wire form is the spacing-token reference, not the raw integer).
  - `GameDetailComposer.composeTabs` — all-zero block, deleted.
  - `WatchComposer.composeTabs` — all-zero block, deleted.
- **Client diff:**
  - Web: `SectionRouter.tsx` (`SectionList` margin wrapper) and `App.tsx` (standalone-section margin wrapper) — removed ~35 lines of `layoutHints`-derived `<div>` wrapping.
  - Android: `SduiScreenContent.kt` (`SectionItem`) — removed ~30 lines of `Spacer` + `HorizontalDivider` conditional blocks.
  - iOS: `ScreenShell.swift` (`SectionLayout`) — removed ~20 lines of `.padding` + `Divider()` wiring.
- **Test results:** All four test suites passed after the change (130 iOS tests, 220 web tests, full Android build, full server build).
- **Active model:** Documented in `docs/sdui-design-system.md §2` (Box-model cascade) and cross-referenced from `docs/client-implementors-contract.md §4a`.

## Decision Outcome

Option C. Single ownership path for section outer chrome: `Section.surface` consumed by `SectionContainer`. The `SectionLayoutHints` type is removed from the wire contract.

## Consequences

- **Short term:** Schema break — clients on schema versions before 2026-05-25 that previously decoded `Section.layoutHints` will encounter payloads without the field. Mitigation: this is a prototype repo with coordinated client releases. Production rollout would gate on a schema-version bump using the existing `X-Schema-Version-Mismatch: upgrade-required` mechanism (the server returns the header when a client's declared `schemaVersion` is below the minimum; clients surface an upgrade prompt).
- **Medium term:** Server composers express inter-section spacing through `Section.surface.margin`. Composer ergonomics are supported by the existing surface factory vocabulary (`flushSurface()`, `cardSurface()`, `railSurface()`, etc.) — Step 5 of this cleanup extracts those into a dedicated `SectionSurfaces` class for discoverability.
- **Long term:** The box-model cascade (`Screen.contentInsets` → `Section.surface` → `AtomicBox`) is a stable architectural anchor. Future layout features — multi-column section grids, placement slots (§9b in requirements summary) — layer cleanly above the cascade rather than alongside it, because each cascade level has one owner and one consumer.

## Implementation Notes

- **Rollout:** Single PR scope — schema deletion + server composer migration + three client renderer changes + codegen run, landed atomically. No transitional dual-path period.
- **Compatibility strategy:** Relies on the schema-version mismatch mechanism for breaking-change rollout. Wire form is a one-way migration: clients on the new schema decode the new shape; old clients (older schema) won't see the new envelope from the server because the server stops emitting `layoutHints`.
- **Ownership and governance:**
  - Section outer chrome: `SectionContainer` (per platform) reading `Section.surface`.
  - Inter-section spacing: composers via `Section.surface.margin`.
  - Inline divider lines (where designed): atomic composition via thin `Container` primitives, or `Section.surface.border` for border-style separators.

## Relationship to ADR-013

ADR-013 introduced typed per-primitive variant enums (`ContainerVariant`, `ImageVariant`, etc.) for atomic primitives. Variant tokens are orthogonal to `SectionSurface`. Section-level chrome remains exclusively inline: `Section.surface` carries margin, padding, background, cornerRadius, shadow, and border by value (with token references for scalars), not by variant token. ADR-013's variant mechanism applies only to atomic elements — a `Container` with `variant: "hero"` is an atomic-level concern, not a section-level one. The two systems share no decision boundary; this ADR does not affect or constrain ADR-013's variant vocabulary.

## Open Questions

- If the prototype migrates to a multi-column section grid in the future, where does the column-level outer chrome live? The cascade currently has no column level. A likely answer is that a `Grid` layout section owns column chrome as part of its structural composition, analogous to how `Container` owns flex-child layout today — but the question is deferred until a real multi-column consumer drives the shape.
- Should `Section.surface` gain a variant mechanism (analogous to ADR-013's `ContainerVariant`) for named section-level presets? Current position: no. The server-side surface factories (`cardSurface()`, `railSurface()`, etc.) achieve the same DRY benefit without enlarging the wire contract, and the factory set can evolve without client releases. Revisit if cross-platform section-surface consistency becomes a governance problem.

## Follow-ups

- [x] Schema cleanup — remove `section.padding`, `section.backgroundColor` (Step 1)
- [x] Cascade documentation — `docs/sdui-design-system.md §2` (Step 2)
- [x] Field removal — `SectionLayoutHints` deleted from schema, composers, clients (Step 3)
- [x] Successor ADR — this document (Step 4)
- [ ] Surface vocabulary extraction — `SectionSurfaces` factory class (Step 5, in flight)
