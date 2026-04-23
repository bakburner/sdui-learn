feat: rename SectionDisplay→SectionSurface, atomic-compose reserved SDK sections, remove Kotlin codegen

## Summary

Renames the section envelope's outer-chrome block from `display` / `SectionDisplay`
to `surface` / `SectionSurface` across schema, all three client platforms, server
composers, and governance docs. Refactors the three reserved SDK sections
(`SubscribeBanner`, `SubscribeHero`, `VideoPlayerStub`) to render entirely from a
server-composed `data.ui` atomic tree instead of client-hardcoded fields. Removes
the unused Kotlin codegen target.

## Schema (sdui-schema.json)

- `Section.display` → `Section.surface`; `SectionDisplay` → `SectionSurface`
- `SubscribeBannerData` / `SubscribeHeroData`: replaced flat fields (`title`,
  `subtitle`, `background`, `logoUrl`, `ctaLabel`, `features`) with `ui`
  (AtomicElement tree), `ctaAction`, and `tiers`
- `VideoPlayerData`: updated to match reserved-section contract

## Server (Java composers)

- `SduiUtils.defaultSectionDisplay()` → `defaultSurface()`; all helpers renamed
- `DemoScreenComposer` / `WatchComposer`: build SubscribeBanner, SubscribeHero,
  and VideoPlayer via `AtomicCompositeBuilder` atomic trees instead of flat fields
- `ForYouComposer`, `GameDetailComposer`, `LiveComposer`, `ScoreboardComposer`:
  migrated to `surface` field

## Web (TypeScript / React)

- `SectionContainer` reads `section.surface.*` instead of `section.display.*`
- `SubscribeBanner`, `SubscribeHero`, `VideoPlayerStub` renderers now walk
  `data.ui` atomic tree; client-hardcoded chrome removed
- Deleted `AtomicElement.ts` (consolidated into generated models + barrel export)
- `SduiModels.ts` regenerated; `SectionDisplay` → `SectionSurface`
- Test updates for `LiveSectionWrapper`, `useAppVisibility`, `useRefreshPolicy`

## iOS (Swift)

- `SduiModels.swift`: `SectionDisplay` → `SectionSurface`; subscribe/video models
  updated to `ui` atomic tree
- `SectionContainer` / `SectionRouter`: read `section.surface`
- `SubscribeBannerView`, `SubscribeHeroView`, `VideoPlayerStubView`: render from
  `data.ui`; removed hardcoded chrome

## Android (Kotlin)

- `SduiModels.kt`: `SectionDisplay` → `SectionSurface`; subscribe/video models
  updated
- `SectionContainer` / `SectionRouter`: read `section.surface`
- `SubscribeBannerRenderer`, `SubscribeHeroRenderer`, `VideoPlayerStub`: render
  from `data.ui`

## Codegen

- Removed Kotlin codegen target (`codegen/output/kotlin/SduiModels.kt` deleted)
- `generate.sh` updated to drop Kotlin output step

## Docs & Governance

- **AGENTS.md §15.3**: renamed "chrome" → "surface" throughout; added naming
  rationale (`surface` vs `display`)
- **New**: `docs/sdui-reserved-sdk-sections.md` — documents the `data.ui` atomic
  tree pattern for reserved SDK sections
- Minor updates to `client-implementors-contract.md`, `refapp-sdui-comparison.md`,
  `ios-led-ux-rebuild.md`, `sdui-refapp-implementation-plan.md`
- `tbc.md`: updated references to `surface`

---

52 files changed, 1146 insertions(+), 3516 deletions(-)
Branch: feature/arobinson/update_007
