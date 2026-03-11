# SDUI Platform — Executive Summary v2

**Server-Driven UI for Cross-Platform Dynamic Experiences**

---

## The Opportunity

Today, changing the layout or content arrangement on any app screen requires a code change, PR review, QA cycle, and app store release — a process that takes days to weeks. This means every layout experiment requires engineering capacity and a release cycle, making it impractical to test whether surfacing stats above editorial, or highlighting a promo banner for League Pass subscribers, actually moves engagement. The missed opportunity is not layout control itself — it is the ability to run rapid experiments with UI variants across different user cohorts and measure what drives engagement, conversion, and retention. During the NBA season, the inability to rearrange game detail experiences in near real time is a competitive disadvantage.

**Server-Driven UI (SDUI)** moves layout and composition control to the server. The server decides what appears on screen and in what order. The client apps render those instructions using platform-native components and the existing design system. The result: variant experiments ship in hours instead of sprints, targeted to specific cohorts, measured in real time, and iterated without app store releases.

---

## What It Enables


| Capability                                     | Today                                      | With SDUI                                                     |
| ---------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------- |
| Change UI layout on a live surface             | Days–weeks (code + release cycle)          | < 1 hour (server-side change)                                 |
| Cross-platform consistency for the same screen | Diverges over time across 3+ codebases     | Single schema contract, identical structure everywhere        |
| A/B test a layout variant                      | Requires client code per variant + release | Server selects variant per user, no client change             |
| Mix real-time and static content on one screen | Custom per-screen engineering              | Declarative per-section refresh policies in the schema        |
| Add a new section type to a screen             | Engineering sprint per platform            | Schema update + thin renderer wiring (~30 lines per platform) |


---

## Prototype Status: Feasibility Validated

The prototype has moved beyond architectural planning into working, demonstrable software. The following table summarizes what has been built and proven functional.


| Area                        | What Was Built                                                                                                                                                                                                                                                                                            | Status                 |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| **JSON Schema**             | Complete schema defining 8 atomic primitives, 20 semantic section types, 6 action types, 3 refresh policies, field-level data bindings, and screen-level state management                                                                                                                                 | **Done**               |
| **Code generation**         | Multi-platform pipeline: jsonschema2pojo produces Java POJOs with Jackson annotations; quicktype generates Swift structs and TypeScript types — all from a single schema source                                                                                                                           | **Done**               |
| **Composition service**     | Spring Boot service assembling SDUI responses from live NBA data (Stats API + CDN). Supports demo/live modes, per-section refresh policies, Ably channel resolution, A/B variant composition, and trace ID observability                                                                                  | **Done**               |
| **Android renderer**        | Full Kotlin/Compose renderer in a reusable `sdui-core` library module. 20 section renderers (ScoreboardHeader, StatLine, HeroPanel, ContentRail, TabGroup, PromoBanner, GamePanel, FeaturedGamePanel, VideoCarousel, NbaTvSchedule, SubscribeBanner, SubscribeHero, AdSlot, BoxscoreTable, Form, Row, SectionHeader, FollowingRail, SeasonLeadersTable, ErrorState). Real-time Ably integration, polling with direct CDN support, StateFlow-based state management, generic action handler, server-driven image fallback via Coil `SubcomposeAsyncImage` | **Done**               |
| **Web renderer**            | React/TypeScript renderer with 20 section renderers, refresh policy hooks, action handler, data binding applier, and server-driven image fallback via `onError` handlers. Polling works; Ably SSE partially integrated                                                                                                                                         | **Done** (partial SSE) |
| **A/B variant composition** | Server-side variant system demonstrated: 4 variants (A/B/C/D) reorder, add, or remove sections from the same screen with no client changes                                                                                                                                                                | **Done**               |
| **Real-time score updates** | Live scoreboard updating via Ably SSE with JSONPath-based field-level data bindings on Android. Polling fallback for stats sections with configurable intervals                                                                                                                                           | **Done**               |
| **Graceful degradation**    | Unknown section types skipped without crash. Live data unavailable falls back to static examples                                                                                                                                                                                                          | **Done**               |
| **iOS renderer**            | Architecture validated via Android; not yet built                                                                                                                                                                                                                                                         | **Not started**        |
| **Test coverage**           | No unit, integration, or contract tests                                                                                                                                                                                                                                                                   | **Gap**                |


### Key Architectural Concepts Demonstrated

Every concept from the original roadmap's prototype validation checklist has been proven in working code:

- **Two-tier primitives** — atomic (Container, Text, Image, Button, etc.) and semantic (ScoreboardHeader, StatLine, TabGroup, etc.) both defined in schema and rendered natively
- **Single-schema codegen** — one JSON Schema produces typed models for Java, Swift, and TypeScript
- **Screen → Section → Component hierarchy** — full response walked by section routers on both platforms
- **Per-section refresh policies** — static, polling (configurable interval + direct CDN URL), and SSE (Ably channels) coexisting on a single screen
- **Declarative data binding** — real-time Ably messages mapped to component fields via server-defined JSONPath bindings
- **Action system** — navigate, analytics, mutate, dismiss, refresh all dispatched generically through a single handler
- **Schema version negotiation** — client sends version header; server includes version in response
- **Graceful unknown type handling** — unrecognized section types skipped without crash
- **A/B variant composition** — server returns different section ordering/content per variant parameter
- **Tabular data with domain-typed stats** — semantic `BoxscoreTable` section with frozen player column, horizontally scrollable stat columns, client-side sorting via screen state, and frozen totals row. Generic `Form` section for settings pickers (season, season type) driving parameterized refresh. `SeasonLeadersTable` demonstrating form-driven parameterized server refresh with surgical section merge (new data merges into existing sections without full screen re-render to preserve form state)
- **Platform-aware composition** — `X-Platform` header drives per-platform field values (e.g., form layout orientation). Shared schema, shared data pipeline, per-platform-family composition tailoring
- **Server-driven image fallback** — composition service provides `fallbackThumbnailUrl` on image-bearing sections; clients handle load errors gracefully using platform-native mechanisms (web `onError`, Android Coil `SubcomposeAsyncImage` error slot)

---

## Discovery Phase Findings

### Mobile Native Audit

A reverse-engineering audit of the production Android and iOS codebases compared native patterns against prototype capabilities. The audit confirms the prototype covers what is needed for the Game Detail beachhead and identifies capabilities required for future surface expansion.

**Prototype covers the beachhead:**

- Typed schema with semantic section types
- Section routing and render dispatch
- Action system (navigate, analytics, mutate, dismiss, refresh)
- Mixed refresh policies (static, polling, SSE) on a single screen
- Server-driven composition with variant support

**Capabilities needed for surface expansion (not blockers for Game Detail):**


| Capability                                          | Needed When                                                 | Effort      |
| --------------------------------------------------- | ----------------------------------------------------------- | ----------- |
| **Layout managers** (placement slots, multi-column) | Tablet-optimized surfaces, surfaces with sidebar navigation | Medium      |
| **Grid/table with pinned rows and columns**         | Standings, full box score surface                           | Medium–High |
| **Entitlement/paywall awareness**                   | Surfaces with premium content gating                        | Medium      |
| **Form/input/search primitives**                    | Search surfaces, settings, profile editing                  | Low–Medium  |

> **Update (2026-03-04):** Grid/table and Form/input capabilities are now designed and implemented. `BoxscoreTable` demonstrates semantic tabular data rendering with client-side sort; `Form` demonstrates extensible settings pickers with parameterized server refresh; `SeasonLeadersTable` demonstrates form-driven parameterized refresh with surgical section merge. These capabilities feed into M4 surface expansion (standings, roster, stats leaders).
| **Responsive/form-factor layouts**                  | Tablet and desktop surface variants                         | Medium      |


Each of these is additive to the current schema — they are introduced when a new surface requires them, not preemptively.

---

## Why Build, Not Buy

We evaluated off-the-shelf SDUI platforms (DivKit by Yandex, Nativeblocks) and concluded they don't meet our requirements:

- **No mixed refresh strategies.** Our game detail screen needs the scoreboard updating in real time (SSE) while editorial content below it is cached for hours. Off-the-shelf platforms treat the entire screen as one data unit.
- **No data binding to our services.** Vendor platforms serve layout from their backend, but live game data lives in our infrastructure. This creates a dual-source-of-truth problem.
- **Limited design system integration.** Vendor rendering engines replace native components rather than wrapping them, breaking design system consistency.

**The prototype validates this decision.** The working Ably SSE integration with per-section refresh policies — the single hardest requirement — is functioning and would not have been achievable with any evaluated vendor platform.

---

## How It Works (High Level)

**Client App → API Aggregation / Composition Layer → Data Services + Targeting + Personalization**

1. The client requests a screen (e.g., game detail for a specific game).
2. The **aggregation/composition layer** assembles the response — fetching content from NBA data services, applying targeting and personalization decisions (including experiment variants), and assigning per-section refresh policies (static, poll with interval + URL, or SSE with Ably channel).
3. The client **renderer** interprets the response — mapping each section type to a native design system component, establishing real-time data channels where needed, and rendering the screen.

The server controls *what* appears and *in what order*. Platform teams control *how* each component looks, feels, and animates natively.

---

## Timeline: Vertical Slices to Production

The prototype has already completed the equivalent of the original Phase 0 (Foundation) and most of Phase 1 (Single-Platform PoC) — roughly 14 weeks of planned work. The timeline below starts from today and is organized around **vertical slices**, each delivering a shippable, end-to-end outcome. Testing and hardening are built into each slice, not gated as a separate phase.

### Milestone 1: Game Detail on Android — Staging Ready (Weeks 1–4)

Harden what exists into a staging-deployable vertical slice.


| Deliverable                                                                                        | Owner               |
| -------------------------------------------------------------------------------------------------- | ------------------- |
| Contract tests for all 7 section types (golden SDUI response fixtures + assertions)                | Core team           |
| Data binding hardened end-to-end through `DataBindingResolver` (remove hardcoded live score paths) | Core team           |
| Performance baseline: measure SDUI render time vs. native on Android                               | Core team + Android |
| Composition service deployed to staging with live NBA data                                         | Core team           |
| Caching layer: Room-backed response cache with stale-while-revalidate                              | Core team           |


**Exit: Game Detail rendering from SDUI on Android in staging with live scores, contract tests passing, performance baselined.**

### Milestone 2: Game Detail — All Platforms in Staging (Weeks 5–12)

Expand to iOS and complete web. Each platform renderer is a vertical deliverable — iOS and web can be built in parallel.


| Deliverable                                                                              | Owner                     |
| ---------------------------------------------------------------------------------------- | ------------------------- |
| iOS renderer: SwiftUI component registry + Ably integration (using Android as reference) | iOS team (1–2 engineers)  |
| Web renderer: complete Ably SSE integration, match Android real-time capabilities        | Web team (1 engineer)     |
| Per-platform contract tests using shared golden fixtures                                 | Each platform team + Core |
| A/B targeting: composition service queries targeting service for variant resolution      | Core team                 |
| Observability: render timing dashboards, data channel health monitoring                  | Core team                 |


**Exit: Game Detail rendering from SDUI on all three platforms in staging. A/B variants verified. Contract tests passing on all platforms.**

### Milestone 3: Game Detail — Production (Weeks 13–18)

Ship Game Detail as the first SDUI-powered production surface.


| Deliverable                                                          | Owner              |
| -------------------------------------------------------------------- | ------------------ |
| Progressive rollout: feature-flagged 5% → 25% → 100% with monitoring | Cross-functional   |
| Automated rollback triggers on error rate thresholds                 | Core team + DevOps |
| CDN caching for static sections, real-time bypass for live sections  | Core team          |
| On-call runbook: SDUI-specific debugging with trace ID lookup        | Core team          |
| Schema v1.0: formalized, versioned, backward-compatible              | Core team          |


**Exit: Game Detail fully SDUI-powered in production on all platforms. Error rates within SLA. Real-time channels stable under live event load.**

### Milestone 4: Second Surface + Schema Evolution (Weeks 19–24)

Expand SDUI to a second surface, introducing schema capabilities as the new surface demands them.


| Deliverable                                                                                           | Owner               |
| ----------------------------------------------------------------------------------------------------- | ------------------- |
| Second surface selected and scoped (e.g., Scoreboard, Home Feed, or Player Profile)                   | Product + Core team |
| Schema additions driven by surface needs (layout primitives, new section types, grid/table if needed) | Core team           |
| Renderers extended for new section types (~30 lines per type per platform)                            | Platform teams      |
| Design system token integration (semantic color/typography/spacing references in schema)              | Core team + Design  |


**Exit: Second surface in production. Schema proven extensible beyond a single surface.**

### Summary


| Milestone                     | Timeline | What Ships                                                       |
| ----------------------------- | -------- | ---------------------------------------------------------------- |
| **M1: Android Staging**       | Week 4   | Game Detail on Android in staging, tested, performance baselined |
| **M2: All Platforms Staging** | Week 12  | Game Detail on Android + iOS + Web in staging with A/B variants  |
| **M3: Production**            | Week 18  | Game Detail in production on all platforms                       |
| **M4: Second Surface**        | Week 24  | Second SDUI-powered surface in production                        |

> **Update (2026-03-04):** Boxscore table and form sections are now built and functional on Web and Android. BoxscoreTable demonstrates semantic tabular data rendering with client-side sort; Form demonstrates extensible settings pickers with parameterized server refresh. SeasonLeadersTable adds form-driven server refresh with section-level merge. These capabilities feed into M4 surface expansion (standings, roster, stats leaders).


**Time to first production surface: ~18 weeks (4.5 months).** Second surface by week 24 (6 months). The original v1 estimate of 32 weeks to production assumed building from scratch; the prototype has already retired that risk. Capabilities identified in the mobile native audit (layout managers, entitlement, grid/table) are introduced when a surface requires them — they do not gate the beachhead.

---

## Resourcing

### Core SDUI Team (M1–M4)


| Role                       | Count | Focus                                                       |
| -------------------------- | ----- | ----------------------------------------------------------- |
| SDUI Architect / Tech Lead | 1     | Schema design, architecture decisions, cross-team alignment |
| Backend Engineers          | 2     | Composition service, targeting integration, data channels   |
| SDET / QA Engineer         | 1     | Contract testing, visual regression, integration testing    |
| DevOps Engineer            | 1     | Codegen, tooling, observability, deployment                 |


**Total core team: 5–6 engineers.**

### Platform Team Commitment


| Milestone                  | Platform Commitment                             | Duration |
| -------------------------- | ----------------------------------------------- | -------- |
| M1 (Android Staging)       | Core team only — Android renderer already built | 4 weeks  |
| M2 (All Platforms Staging) | iOS: 1–2 engineers. Web: 1 engineer.            | 8 weeks  |
| M3 (Production)            | 1 engineer per platform for rollout support     | 6 weeks  |
| M4 (Second Surface)        | 1 engineer per platform for new section types   | 6 weeks  |


The prototype demonstrates that the renderer is thin wiring (~30 lines per section type) mapping SDUI data to existing design system components. Platform teams own their renderer code and quality — the core team provides the schema contract, reference implementation, and tooling.

---

## Key Risks and Mitigations


| Risk                                  | Impact                                                | Mitigation                                                                                                                                                                           | Status                                                          |
| ------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------- |
| **Platform team adoption resistance** | Renderer work deprioritized or deviates from contract | Leadership endorsement before asking for platform resources. Platform teams retain full control of native rendering. Working Android prototype demonstrates the thin renderer model. | Prototype reduces this risk — teams can see the working example |
| **Schema evolution complexity**       | Breaking changes to older app versions                | Additive-only change policy. Version negotiation protocol implemented in prototype.                                                                                                  | Version header working; governance process needed at M3         |
| **Performance overhead**              | 50–150ms added vs. native screens                     | Client-side response caching for instant re-renders. Performance budgets enforced in CI.                                                                                             | Benchmarking in M1; caching in M1                               |
| **Real-time channel reliability**     | Stale scores during live events                       | Fallback cascade: SSE → polling → last-known data with staleness indicator.                                                                                                          | Ably SSE working on Android; fallback cascade needed by M3      |
| **Testing gaps**                      | Regressions ship undetected across platforms          | Contract tests built into each milestone using shared golden fixtures. Not a separate phase — each slice ships with its own tests.                                                   | Contract tests in M1                                            |
| **Debugging across layers**           | Longer mean-time-to-resolution for UI bugs            | End-to-end trace IDs from composition through rendering. On-call runbook by M3.                                                                                                      | Trace IDs implemented; dashboards in M2, runbook in M3          |


---

## Success Metrics


| Metric                                      | Target                   |
| ------------------------------------------- | ------------------------ |
| Time from UI change decision to production  | < 1 hour                 |
| Cross-platform parity for identical changes | < 24 hours               |
| SDUI render performance vs. native          | Within 10%               |
| SDUI error/fallback rate                    | < 0.1% of page loads     |
| Real-time data freshness during live events | Updates within 2 seconds |
| A/B test velocity on SDUI surfaces          | 3x current throughput    |
| Developer satisfaction (renderer teams)     | > 4/5 quarterly          |


---

## Resolved Decisions

Decisions that were open in v1 and have been resolved through prototype work:


| Decision                         | Resolution                                        | Evidence                                                                                              |
| -------------------------------- | ------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| API aggregation layer technology | **REST composition**                              | Composition service built as Spring Boot REST API; JSON Schema typing provides sufficient type safety |
| Schema format                    | **JSON Schema**                                   | Single schema drives codegen for Java, Swift, and TypeScript                                          |
| Real-time transport              | **Ably**                                          | Ably SDK integrated on Android with JWT auth callback; channel pattern `{gameId}:linescore` working   |
| Codegen tooling                  | **jsonschema2pojo (Java) + quicktype (Swift/TS)** | Pipeline produces typed models; Java POJOs used in production server and Android renderer             |


---

## Decision Continuity from v1

To preserve continuity with the original executive summary, this section explicitly tracks what happened to v1 open decisions:


| v1 Decision Topic                    | Current Status                    |
| ------------------------------------ | --------------------------------- |
| API aggregation layer technology     | **Resolved** (REST composition)   |
| Beachhead surface                    | **TBD**                           |
| Composition ownership and transition | **ADR pending**                   |
| Request/contract model               | **ADR pending**                   |
| GET/POST + caching policy            | **ADR pending**                   |
| Action scope and precedence          | **ADR pending**                   |
| Experiment assignment strategy       | **ADR pending**                   |
| Ads boundary and contract            | **ADR pending**                   |
| Form-factor layout manager           | **ADR pending**                   |


---

## Immediate Next Steps (Milestone 1)

1. **Contract tests** — Define golden SDUI response fixtures for all section types. Build assertions validating schema-renderer agreement on Android. Integrate into CI.
2. **Data binding hardening** — Route all live score updates through `DataBindingResolver` end-to-end, eliminating hardcoded mapping paths.
3. **Performance baseline** — Measure SDUI render time vs. native on Android. Establish performance budget.
4. **Caching** — Room-backed response cache with stale-while-revalidate for instant re-renders on screen re-entry.
5. **Staging deployment** — Composition service deployed to staging environment with live NBA data.
6. **iOS team alignment** — Present working Android prototype to iOS leads, secure commitment for Milestone 2 renderer build.



