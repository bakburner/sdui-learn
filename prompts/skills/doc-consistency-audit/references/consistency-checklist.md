# Consistency Checklist

For each document in the audit list, verify every item below. Mark any inconsistency for fixing.

## Counts

- [ ] Section type count matches schema enum length (currently 9)
- [ ] Atomic element type count matches schema enum length (currently 12)
- [ ] Action type count matches schema enum length (currently 6)
- [ ] Action trigger count matches schema enum length (currently 7)
- [ ] Renderer count matches number of section renderer files per platform
- [ ] "Semantic sections" count matches schema section enum minus AtomicComposite (currently 8)
- [ ] AGENTS.md section count matches number of top-level `## N.` headings (currently 13)

## Type Lists

- [ ] Every section type name listed matches schema enum exactly (PascalCase)
- [ ] Every action type name listed matches schema enum exactly (camelCase)
- [ ] Every action trigger listed matches schema enum exactly (camelCase, includes `onSubmit`)
- [ ] Every `variant` value on a `Button`-type element matches the `ButtonVariant` schema enum exactly (`primary`, `secondary`, `tertiary`, `text`); no `filled` appears as an enum value
- [ ] Every `variant` value on a `Text`-type element matches the `TextVariant` schema enum exactly
- [ ] Every `variant` value on a `Container`-type element matches the `ContainerVariant` schema enum exactly
- [ ] Every `variant` value on an `Image`-type element matches the `ImageVariant` schema enum exactly
- [ ] No document references the old per-primitive property names `buttonVariant`, `containerVariant`, `imageVariant`, `headerVariant`, or `cellVariant` — the wire property is the uniform `variant: string`
- [ ] Every atomic element type name listed matches schema enum exactly (PascalCase)
- [ ] No pruned/deleted types appear (ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule, GamePanel, Row, RowData)
- [ ] Migrated types list is complete: ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail, HeroPanel, StatLine, VideoCarousel, NbaTvSchedule, GamePanel
- [ ] Permanent types list is complete: BoxscoreTable, SeasonLeadersTable, Form, TabGroup, SubscribeHero, SubscribeBanner, AdSlot, VideoPlayer
- [ ] Icon references in contract-facing docs use neutral `sdui:*` tokens (e.g. `sdui:home`, `sdui:basketball`), not raw Material Symbols names (`home`, `sports_basketball`, `play_circle`)

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
- [ ] Feature status (Built/Gap/Partial) is consistent across Executive Summary, Technical Proposal, and Requirements Summary
- [ ] ADRs reference current type names, not stale ones

## Feature Status (most commonly missed category)

Compare every status table entry against actual code state. Recent commits frequently close gaps without updating docs.

- [ ] Every requirement marked **Gap** is actually unimplemented (no code exists)
- [ ] Every requirement marked **Partial** has incomplete implementation (not fully built)
- [ ] Every requirement marked **Built** has working code on at least the platforms claimed
- [ ] Request context envelope status matches code reality (`SduiRequestContext.java`, `RequestEnvelopeBuilder.kt/.ts`)
- [ ] Internationalization (i18n) status matches code reality (`stringTable` in schema, `stampStringTableOnSections` in server, client consumption)
- [ ] Experiment/A/B testing status matches code reality (ADR-006 status, server experiment resolution, client assignment transport)
- [ ] Composition API contract status matches code reality (`BracketParamResolver`, GET/POST support)
- [ ] Status is consistent across **all three** status tables: Executive Summary feature table, Technical Proposal §10, Requirements Summary §10
- [ ] New features implemented since last audit are reflected in the feature tables (check `git log` for recent `feat:` commits)

## ADR Status Tracking

- [ ] Every ADR's status in doc tables matches the actual status in the ADR file header
- [ ] ADRs marked "Proposed" in docs are actually still proposed (not silently accepted)
- [ ] ADRs marked "Accepted" in docs are actually accepted
- [ ] The Executive Summary "Decision Continuity" table reflects current ADR statuses (not stale "ADR pending")
- [ ] The Requirements Summary "ADR Status Summary" table reflects current ADR statuses (note: table was renamed from "ADR Approvals Pending")
- [ ] Recently added ADRs (ADR-011 data classification, ADR-012 client data architecture, ADR-013 style tokens) appear in every ADR matrix

## Revision History

- [ ] Modified documents have updated revision history entries
- [ ] Revision entries accurately describe the changes made
- [ ] All revision history tables are sorted **newest-first** (descending date order)
- [ ] New entries are inserted at the top of the table (after the header row), not appended at the bottom
