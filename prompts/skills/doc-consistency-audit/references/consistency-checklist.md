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
- [ ] Permanent types list is complete: BoxscoreTable, SeasonLeadersTable, Form, TabGroup, GamePanel, SubscribeHero, SubscribeBanner, AdSlot

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
- [ ] The Requirements Summary "ADR Approvals Pending" table reflects current ADR statuses

## Revision History

- [ ] Modified documents have updated revision history entries
- [ ] Revision entries accurately describe the changes made
- [ ] All revision history tables are sorted **newest-first** (descending date order)
- [ ] New entries are inserted at the top of the table (after the header row), not appended at the bottom
