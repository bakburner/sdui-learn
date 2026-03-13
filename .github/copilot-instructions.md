# SDUI Prototype — Copilot Instructions

## Project

Server-Driven UI prototype: JSON schema → server composition → multi-platform native rendering (Android Compose, Web React, iOS SwiftUI future).

## Architecture

**Dual-layer rendering model:**

- **Section layer**: Named domain renderers (`BoxscoreTable`, `GamePanel`, `FormRenderer`, etc.) with client-owned state. Routed by `SectionRouter`.
- **Atomic layer**: 9 server-composed primitives (`Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`, `DisplayGrid`) defined as `AtomicElement` in schema.
- **Bridge**: `AtomicComposite` section type — `SectionRouter` delegates to `AtomicRouter`.

## Key Rules

1. **Schema is source of truth** — `schema/sdui-schema.json`. Run `cd codegen && ./generate.sh` after changes.
2. **No hardcoded URLs/paths on clients** — all endpoints resolved via `fetchScreen()`.
3. **No client-side screen-type enums** — every screen is generic `fetchScreen(endpoint)`.
4. **Unknown types degrade gracefully** — both `SectionRouter` and `AtomicRouter` skip unknowns with a log.
5. **Never silently swallow exceptions** — log with context before returning null/fallback.

## Atomic Layer

- Files: `renderer/atomic/` (Android), `components/atomic/` (Web). Never mix with `sections/`.
- Naming: all atomic files use `Atomic` prefix. Existing section names unchanged.
- **Performance contract**: max depth 6, max children/container 20, max nodes 50. Server validates; client has defensive depth guard.
- **DisplayGrid** is display-only text grid — zero interactivity. Any sort/filter/expand → use a section.

## Tier Classification

- **Tier 1** (5 → migrate to atomic): ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail
- **Tier 2** (6 → evaluate after Tier 1): HeroPanel, ScoreboardHeader, StatLine, VideoCarousel, GamePanel, NbaTvSchedule
- **Tier 3** (6 → stay native forever): BoxscoreTable, SeasonLeadersTable, FormRenderer, TabGroup, SubscribeHero/SubscribeBanner, AdSlot

## Module Boundary (Android)

- `sdui-core` — reusable SDUI infra (models, network, state, renderers, screen orchestration)
- `app` — thin shell (config, navigation, chrome). One-way dep: `app` → `sdui-core`.

## Tech Stack

- **Server**: Spring Boot 3, Java 17+, Jackson
- **Android**: Kotlin 2.1, Compose BOM 2024.12, Coil, Ably SDK, Room
- **Web**: React, TypeScript, Vite
- **Schema**: JSON Schema → codegen (jsonschema2pojo for Kotlin/Java)

## Decision Checklist Before Adding Client Code

1. Can this be solved by server composition only?
2. Can this be solved by schema/action payload changes?
3. Can it be a server-composed `AtomicComposite` instead of a new section?
4. If client code is needed, can it go in shared infra rather than a specific renderer?

## Key Docs

- `docs/atomic-primitives-analysis.md` — hybrid architecture plan
- `docs/adr/` — architecture decision records (010 = offline strategy)
- `docs/SDUI_Technical_Proposal_v2.md` — technical proposal
- `docs/sdui-accessibility-plan.md` — accessibility plan
- `docs/action-failure-semantics-plan.md` — action failure contract
