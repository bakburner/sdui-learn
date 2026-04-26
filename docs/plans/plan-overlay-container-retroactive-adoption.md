# Plan: Retroactive `OverlayContainer` Adoption For Legacy Badge / Scrim Patterns

> Source inputs: `AGENTS.md`, ADR-013, `schema/sdui-schema.json`, `docs/sdui-design-system.md`, current renderers (`web/src/components/atomic/`, `ios/Sources/SduiCore/Rendering/Atomic/`, `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/`), and current emit sites in `server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java`.

## Summary

`OverlayContainer` (with `base` + `overlays[]`) is now the canonical schema primitive for layered atomic UI on web, iOS, and Android. Several pre-`OverlayContainer` patterns still exist in the codebase — most notably the legacy `badge` convenience field on `AtomicElement`, a sibling-text "duration over thumbnail" pattern in the hero panel, an inline duration pill in the video meta row, and remaining `Container.background.imageUrl` fixtures.

This plan retroactively migrates those patterns to `OverlayContainer` so the wire-level vocabulary for "thing on top of an image" is **one** primitive, not three. Where a legacy schema field is fully redundant with `OverlayContainer.overlays`, we deprecate it and remove it after one release cycle.

**This plan is explicitly out of scope for `plan-feed-atomic-composite-parity.md`** (which targets new feed modules). It is a parallel cleanup workstream that rides on the same renderer support `OverlayContainer` already ships.

## Phase 0: Documentation Discovery

| Source | Findings |
|--------|----------|
| `AGENTS.md` §1.1 | Server authority over content/structure. Migrating from `badge` to `overlays[]` is a server emit change; clients keep accepting both during the deprecation window. |
| `AGENTS.md` §11 | New variants must clear the §11.4 stability bar. This plan adds **no** new variants. |
| ADR-013 (style tokens for atomic primitives) | `AtomicBox` is sole owner of wire-emitted box-model props. `OverlayContainer.overlays[]` already routes overlay layers through that pipeline; the legacy `badge` field has its own `applyBadge` / inline path that bypasses the standard overlay layering. Consolidating onto `overlays[]` reduces parallel paths. |
| `schema/sdui-schema.json` | `badge` is a documented field on `AtomicElement` (`schema/sdui-schema.json:548`) typed as `Badge`. `OverlayContainer` defines `base` + `overlays[]` with explicit `alignment`, `padding`, and `element`. The two layering paths overlap: `badge` is a single-layer special case of `overlays[]`. |
| `web/src/components/atomic/AtomicBox.tsx` (`:259`), `AtomicContainer.tsx` (`:71`), `AtomicImage.tsx` (`:83`) | All three apply `element.badge` via `<AtomicBoxBadge>` independently of the `OverlayContainer` rendering path. |
| `ios/Sources/SduiCore/Rendering/Atomic/AtomicBoxModifier.swift` (`:66`) | Applies badge via `applyBadge(element.badge, …)` modifier. |
| `android/sdui-core/.../renderer/atomic/AtomicBox.kt` (`:79–94`) | Applies badge inside the box modifier with its own alignment switch. |

## Pre-`OverlayContainer` Patterns To Address

| Pattern | Where it lives | Today's behavior | Why migrate |
|---------|----------------|------------------|-------------|
| **Legacy `badge` schema field** | `AtomicElement.badge` (schema), three renderers, `AtomicCompositeBuilder.badge(...)` helper, `buildContentCard` × 2, `buildVodRow` × 2 | Single-badge convenience field with its own renderer pipeline. | Strict subset of `OverlayContainer.overlays[]`. Maintains a parallel layering pipeline for no extra capability. |
| **Sibling "duration text" in hero panel** | `AtomicCompositeBuilder.buildHeroPanel` (`AtomicCompositeBuilder.java` ≈L437–440) | Adds a `TEXT_INVERSE` text element as a column sibling of the image. The intent is "duration on the thumbnail," but the text actually renders below the image in column flow. | Visual intent does not match emitted structure; `OverlayContainer` realizes the intent. |
| **Duration pill in video meta row** | `AtomicCompositeBuilder.buildVideoCard` (`AtomicCompositeBuilder.java` ≈L823–826) | Pill in a meta row beneath the thumbnail. Code comment acknowledges that other video-card paths overlay the duration on the thumbnail. | Inconsistent with sibling helper that overlays duration on thumbnail; should land on one policy. |
| **`Container.background.imageUrl` for image-dominant cards** | Fixtures and `web/src/utils/background.ts` | Mobile clients deliberately constrain background images (see `AtomicBox.kt:136`, `ContainerVariantResolver.swift:212/241`). Web still resolves it. | This is the explicitly-deprecated mobile path. Where current composers still emit it for image-dominant content, migrate to `OverlayContainer`. Where it appears only in legacy fixtures (`docs/appendix-kitchen-sink.md`), document as deprecated. |

## Atomic Precedence Assessment

| Candidate | Decision | Flag |
|-----------|----------|------|
| Legacy `badge` field deprecation | Deprecate, then remove after one release | `OverlayContainer.overlays[]` is strictly more general. |
| Sibling-text overlay in hero panel | Migrate to `OverlayContainer` | Side effect of pre-overlay primitive era. |
| Inline duration pill in video meta row | Pick a single policy (overlay on thumbnail vs meta row), make consistent | Match real NBA app behavior. |
| `Container.background.imageUrl` for image-dominant cards | Migrate live emit sites to `OverlayContainer` | Mobile policy already constrains this. |
| Pre-existing `OverlayContainer` call sites | Leave alone | Already canonical: `mediaOverlayCard`, `storyCircleItem`, `editorialOverlayCard`, `featuredLiveGameHeroCard`. |

## Requirements Addressed

- [ ] One wire-level vocabulary for layered atomic UI (`OverlayContainer.base` + `overlays[]`).
- [ ] Eliminate parallel renderer pipelines for layering (`badge` field vs `overlays[]`).
- [ ] Make "duration over thumbnail" a server-emitted overlay everywhere it appears, not a sibling text or meta row.
- [ ] Remove live emission of `Container.background.imageUrl` for image-dominant card contexts.
- [ ] Hold the line on AGENTS.md: server-owned URLs, copy, actions, and overlay structure.

## Non-Goals

- Do not change the `OverlayContainer` schema, its renderers, or its alignment vocabulary.
- Do not introduce a new variant or primitive (no `OverlayBadge`, no `BadgedImage`, no shorthand for "image with one corner badge").
- Do not change real-video, ad SDK, or app shell behavior.
- Do not retune visual fidelity in this plan — that is `plan-feed-atomic-composite-parity.md` Phase 5 / 6.
- Do not migrate fixtures that are pure historical/proto kitchen-sink fodder unless a live composer still emits the same shape.

## Tasks

### Phase 1: Inventory + Migration Spec

- [ ] Confirm the full set of `badge` emit sites in `AtomicCompositeBuilder` (current grep: lines ≈247, 249, 1331, 1333; helper at ≈1172).
- [ ] Confirm there are no other `badge` emitters across the server tree (`rg "\.badge\(" server/src/main`).
- [ ] Confirm the full set of `Container.background.imageUrl` emit sites in **live composers** (not docs/fixtures): `rg '"background"\s*:\s*\{[^}]*"imageUrl"' server/src/main`.
- [ ] Document the migration recipe in this plan:
  - `badge(parent, badgeElement, alignment)` → `overlayContainer(parent, List.of(overlay(alignment, padding, badgeElement)))`. The parent's wire-emitted box-model props move to the `overlayContainer`'s `base` (or stay on the parent — `OverlayContainer.base` does not double up box-model). The padding inside `overlay(...)` controls where the badge sits within the alignment region.
  - `buildHeroPanel` duration sibling → `overlayContainer(image, List.of(overlay("bottomEnd", padding(8,8,0,0), durationBadge(duration))))`.
  - `buildVideoCard` duration: foreman decision in Phase 1 (overlay on thumbnail vs keep meta row). Default recommendation: **overlay on thumbnail** to match the sibling helper and the real NBA app.
  - `Container.background.imageUrl` → `OverlayContainer` with an `Image` base whose `src` carries the URL, plus any text/scrim/badge children expressed as `overlays[]`.

Verification:
- [ ] Migration recipe is captured in this section before Phase 2 dispatch.
- [ ] `rg "AtomicCompositeBuilder\.badge\(|\.badge\(" server/src` matches the inventory.
- [ ] No new helper named `legacyBadge`, `badgedImage`, or similar appears in the recipe.

Anti-pattern guards:
- Do not introduce a parallel "convenience" wrapper for the migration (e.g., a `badgeOverlay(...)` helper). Use `overlayContainer(...)` directly so the call sites are obviously migrated.
- Do not silently leave `parent.set("badge", b)` in any new code path.

### Phase 2: Migrate Server Emit Sites Off `badge` And Sibling-Text Patterns

- [ ] `buildContentCard` (`AtomicCompositeBuilder.java` ≈L246–249): replace both `badge(img, …)` calls with `overlayContainer(img, List.of(overlay(...)))`.
- [ ] `buildVodRow` (`AtomicCompositeBuilder.java` ≈L1331–1333): replace both `badge(thumb, …)` calls with `overlayContainer(thumb, List.of(overlay(...)))`.
- [ ] `buildHeroPanel` (`AtomicCompositeBuilder.java` ≈L437–440): replace duration-as-sibling-text with `overlayContainer(image, List.of(overlay("bottomEnd", padding(8,8,0,0), durationBadge(duration))))`.
- [ ] `buildVideoCard` (`AtomicCompositeBuilder.java` ≈L823–826): apply Phase 1 policy decision. If "overlay on thumbnail" wins, remove the meta-row pill emission and add the overlay on the thumbnail composition above; if "meta row" wins, leave as-is and document the policy in the helper Javadoc.
- [ ] Audit `rg '"background"\s*:\s*\{[^}]*"imageUrl"' server/src/main` and migrate any **live** composer emit sites. Skip historical kitchen-sink fixtures and `docs/appendix-kitchen-sink.md` proto content.
- [ ] Once all four server call sites are migrated, mark `AtomicCompositeBuilder.badge(ObjectNode, ObjectNode, String)` with a `@Deprecated` annotation and a Javadoc note: *"Legacy single-badge convenience predating `OverlayContainer`. Use `overlayContainer(base, overlays)` instead. Will be removed after Phase 4."*

Verification:
- [ ] `rg "\.badge\(" server/src/main` returns zero matches in `AtomicCompositeBuilder.java` outside the deprecated helper definition itself.
- [ ] No new `parent.set("badge", b)` writes anywhere in the server tree.
- [ ] No `Container.background.imageUrl` writes from live composers (only fixtures or test data).
- [ ] Snapshot tests for migrated helpers continue to assert `AtomicComposite` envelope shape.

Anti-pattern guards:
- Do not change visual properties (corner radius, padding, alignment) during this migration. The goal is structural equivalence, then visual fidelity is `plan-feed-atomic-composite-parity.md` Phase 5.
- Do not introduce client-derived URLs in the migrated overlay base images. Image URLs come from the existing helper inputs.

### Phase 3: Update Fixtures And Tests

- [ ] Regenerate / hand-update any `schema/examples/` fixtures whose JSON snapshots changed because of the migration.
- [ ] Add fixture coverage for at least one migrated case (preferably `buildContentCard` with duration overlay) under `server/src/test/.../AtomicCompositeBuilderFeedModulesTest.java` or a new sibling test class.
- [ ] Sync iOS golden fixtures (`make ios-fixtures-sync`) — owned by the user, not the agent.
- [ ] Add a server test asserting `buildContentCard(...)` emits `OverlayContainer` for the duration/live badge layer (no `badge` field on the wire).
- [ ] Add a server test asserting `buildHeroPanel(...)` emits `OverlayContainer` (no sibling-text duration in column flow).

Verification:
- [ ] `rg '"badge"\s*:\s*\{' server/src/test` returns zero matches in tests asserting wire shape (existing legacy assertions are fine to leave during the deprecation window if they exist for renderer compatibility).
- [ ] `make codegen` is **not** required; no schema field is added in Phase 3.

Anti-pattern guards:
- Do not add fixtures that emit both `badge` and `overlays[]` on the same parent.

### Phase 4: Deprecate The `badge` Schema Field

After Phase 2 + Phase 3 ship and one release cycle of mixed coexistence has passed (so any external producers / tests can migrate):

- [ ] Mark `AtomicElement.badge` `Deprecated` in `schema/sdui-schema.json` (description prefix: *"Deprecated. Use `OverlayContainer.overlays[]`."*).
- [ ] Run `make codegen`; confirm Web TS / iOS Swift / Android Kotlin codegen all carry the deprecation note.
- [ ] Remove the per-renderer `badge` rendering path:
  - `web/src/components/atomic/AtomicBox.tsx` (`AtomicBoxBadge` and conditional)
  - `web/src/components/atomic/AtomicContainer.tsx` (badge fragment)
  - `web/src/components/atomic/AtomicImage.tsx` (badge fragment)
  - `ios/Sources/SduiCore/Rendering/Atomic/AtomicBoxModifier.swift` (`applyBadge`)
  - `android/sdui-core/.../renderer/atomic/AtomicBox.kt` (badge `Box` block)
- [ ] Remove `badge: Badge?` and `Badge` type from generated models / schema once renderers no longer reference it. (Codegen will re-emit when the schema field is removed.)
- [ ] Remove `AtomicCompositeBuilder.badge(ObjectNode, ObjectNode, String)` and the deprecated `Badge` type if no longer referenced.

Verification:
- [ ] `rg "element\.badge|element\?\.badge|\.badge\(" web/src ios android server/src` is empty (apart from comments noting the removal).
- [ ] `rg "Badge\?|Badge\(" ios android web/src/generated server/src` returns no atomic-element badge references.
- [ ] `make codegen && make ios-fixtures-sync` complete cleanly (user-run).

Anti-pattern guards:
- Do not remove the `badge` schema field in the same release as the renderer migration. Strict-decode would reject any client that still emits the field on the wire mid-migration.
- Do not relax strict decode to "ignore unknown fields" as part of this plan; the deprecation window is the migration aid, not a relaxation.

## Rollout / Sequencing

| Track | Phase | Phase scope | Owner | Notes |
|-------|-------|-------------|-------|-------|
| A | 1, 2, 3 | Migrate all four `badge(...)` call sites and sibling-text duration to `OverlayContainer`; deprecate `AtomicCompositeBuilder.badge(...)`; deprecate-but-keep schema `badge` field | Server + composer | Single PR; renderer-compatible; no schema change. |
| B | 4 | Remove the `badge` schema field, `Badge` type, and per-renderer `applyBadge` paths | Schema + 3 renderers | Separate PR after Track A ships and one release cycle has passed. |

Track A is low-risk and low-scope. Track B requires the codegen + three-platform discipline used elsewhere in the repo.

## Dependencies

- Schema/codegen pipeline must be used in Phase 4 when removing `badge` from `AtomicElement`. Phases 1–3 are server-only.
- `OverlayContainer` renderer support on web, iOS, and Android is already shipped (Phase 2 of the parity plan validated this).
- This plan does not depend on, nor block, the parity plan's Phase 5 or Phase 6.

## Open Questions

- [ ] Phase 1 policy decision: should `buildVideoCard` overlay duration on the thumbnail (consistency with `buildContentCard` + the real NBA app) or keep it in the meta row? Default recommendation: overlay on thumbnail.
- [ ] Are there any external producers (test fixtures, partner-emitted JSON, contract tests in other repos) that still emit the `badge` field on the wire? If yes, the Phase 4 schema removal must coordinate with them.
- [ ] Should `Container.background.imageUrl` be removed entirely from the schema in a later cleanup, or kept for web-only marketing/promo background patterns? Outside this plan; capture as a separate proposal.

## Completion Criteria

- All live `AtomicCompositeBuilder` emit sites use `OverlayContainer` for layered UI.
- The `AtomicCompositeBuilder.badge(...)` helper is deprecated (Track A) and then removed (Track B).
- The `AtomicElement.badge` schema field is deprecated (Track A) and then removed via codegen (Track B).
- Per-renderer `applyBadge` / badge fragments are removed in Track B.
- No live composer emits `Container.background.imageUrl` for image-dominant cards.
- All verification commands pass.
