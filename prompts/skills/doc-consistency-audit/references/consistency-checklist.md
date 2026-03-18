# Consistency Checklist

For each document in the audit list, verify every item below. Mark any inconsistency for fixing.

## Counts

- [ ] Section type count matches schema enum length (currently 9)
- [ ] Atomic element type count matches schema enum length (currently 10)
- [ ] Action type count matches schema enum length (currently 6)
- [ ] Renderer count matches number of section renderer files per platform
- [ ] "Migrated to atomic" count matches number of types in AtomicCompositeBuilder (currently 9)
- [ ] "Permanent sections" count matches schema section enum minus AtomicComposite (currently 8)

## Type Lists

- [ ] Every section type name listed matches schema enum exactly (PascalCase)
- [ ] Every action type name listed matches schema enum exactly (camelCase)
- [ ] Every atomic element type name listed matches schema enum exactly (PascalCase)
- [ ] No pruned/deleted types appear (ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule, Row, RowData)
- [ ] Migrated types list is complete: ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule
- [ ] Permanent types list is complete: BoxscoreTable, SeasonLeadersTable, FormRenderer, TabGroup, GamePanel, SubscribeHero, SubscribeBanner, AdSlot

## Terminology

- [ ] No occurrences of `analytics` as an action type (should be `fireAndForget`)
- [ ] No occurrences of `Tier 1`, `Tier 2`, `Tier 3` as classification labels (retired)
- [ ] No occurrences of `Row` as a section type (replaced by Container with flex/breakpoint)
- [ ] `AtomicComposite` always PascalCase
- [ ] `SectionSlot` always PascalCase when referring to the type
- [ ] `fireAndForget` always camelCase

## Architecture Claims

- [ ] Dual-layer model correctly described (section layer + atomic layer + bridge)
- [ ] AtomicComposite bridge description is accurate
- [ ] SectionSlot bridge description is accurate
- [ ] Performance contract numbers are consistent (depth 6, children 20, nodes 50)
- [ ] SubscribeBanner/SubscribeHero classified as permanent (future IAP SDK)

## Cross-Document Dedup

- [ ] No two documents contradict each other on the same fact
- [ ] Feature status (Built/Gap/Partial) is consistent across Executive Summary and Requirements Summary
- [ ] ADRs reference current type names, not stale ones
