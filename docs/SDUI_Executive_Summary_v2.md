# SDUI Platform — Executive Summary v2

**Server-Driven UI for Cross-Platform Dynamic Experiences**

---

## The Opportunity

Today, changing the layout or content arrangement on any app screen requires a code change, PR review, QA cycle, and app store release — a process that takes days to weeks. This means every layout experiment requires engineering capacity and a release cycle, making it impractical to test whether surfacing stats above editorial, or highlighting a promo banner for League Pass subscribers, actually moves engagement. The missed opportunity is not layout control itself — it is the ability to run rapid experiments with UI variants across different user cohorts and measure what drives engagement, conversion, and retention. During the NBA season, the inability to rearrange live game experiences in near real time is a competitive disadvantage.

**Server-Driven UI (SDUI)** moves layout and composition control to the server. The server decides what appears on screen and in what order. The client apps render those instructions using platform-native components and the existing design system. The result: variant experiments ship in hours instead of sprints, targeted to specific cohorts, measured in real time, and iterated without app store releases.

---

## Key Results

### **Objective 1: Deliver changes at NBA season pace**
- KR1: Ship UI layout changes to production in < 1 hour (vs. current days-to-weeks)
- KR2: Enable server-side rollbacks and hotfixes without app releases
- KR3: Majority of UI changes ship without client releases by end of M4

### **Objective 2: Eliminate cross-platform drift and coordination overhead**
- KR1: Zero behavioral drift on SDUI surfaces (single schema = single source of truth)
- KR2: Measurably reduce client PRs and cross-platform coordination overhead per sprint
- KR3: Consolidate business logic from 3 platform implementations to 1 server implementation for SDUI surfaces

### **Objective 3: Accelerate from design to production**
- KR1: Increase A/B experimentation velocity 3x on SDUI surfaces vs. current throughput
- KR2: Ship new layout variants using existing atomic building blocks without client development
- KR3: Reduce change failure rate through centralized, testable server-side composition

---

## What It Enables


| Capability                                     | Today                                      | With SDUI                                                     |
| ---------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------- |
| Change UI layout on a live surface             | Days–weeks (code + release cycle)          | < 1 hour (server-side change)                                 |
| Cross-platform consistency for the same screen | Diverges over time across 3+ codebases     | Single schema contract, identical structure everywhere        |
| A/B test a layout variant                      | Requires client code per variant + release | Server selects variant per user, no client change             |
| Mix real-time and static content on one screen | Custom per-screen engineering              | Declarative per-section refresh policies in the schema        |
| Add a new section type to a screen             | Engineering sprint per platform            | Schema update + thin renderer wiring (~30 lines per platform) |
| Ship a new promotional layout matching Figma   | Design → code → release per platform      | Designer builds in Figma, server composes matching atomic tree — no app release |


---

## Prototype Status: Feasibility Validated

The prototype has moved beyond architectural planning into working, demonstrable software. The following table summarizes what has been built and proven functional.


| Area                        | Description                                                                                                                                                                                                                                                                                               | Status                 |
| --------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------- |
| **JSON Schema**             | Complete schema defining 12 atomic element types (10 rendering primitives + SectionSlot bridge + LiveClock), 9 section types (8 permanent + AtomicComposite), 6 action types, 8 action triggers, 3 refresh policies, field-level data bindings, and screen-level state management. | **Done**               |
| **Code generation**         | Multi-platform pipeline: jsonschema2pojo produces Java POJOs with Jackson annotations; quicktype generates Swift structs and TypeScript types — all from a single schema source                                                                                                                           | **Done**               |
| **Composition service**     | Spring Boot service assembling SDUI responses from live NBA data (Stats API + CDN). Supports demo/live modes, per-section refresh policies, Ably channel resolution, A/B variant composition, request context envelope (`SduiRequestContext`), section-level i18n (`stringTable`), and trace ID observability | **Done**               |
| **Android renderer**        | Kotlin/Compose `sdui-core` library. 8 semantic section renderers + AtomicComposite routing. Ably real-time, polling, StateFlow state management, action handler, image fallback, icon tokens, server-declared navigation shell | **Done**               |
| **Web renderer**            | React/TypeScript with 8 section renderers + AtomicComposite routing, refresh policies, action handler, data binding, image fallback, icon tokens, server-declared navigation. Polling works; Ably SSE partially integrated | **Done** (partial SSE) |
| **A/B variant composition** | Server-side variant system demonstrated: 4 variants (A/B/C/D) reorder, add, or remove sections from the same screen with no client changes                                                                                                                                                                | **Done**               |
| **Real-time score updates** | Live scoreboard updating via Ably SSE with JSONPath-based field-level data bindings on Android. Polling fallback for stats sections with configurable intervals                                                                                                                                           | **Done**               |
| **Graceful degradation**    | Unknown section types skipped without crash. Live data unavailable falls back to static examples                                                                                                                                                                                                          | **Done**               |
| **Atomic rendering layer**  | 12 atomic element types composed by server and rendered by `AtomicRouter` on all platforms. `AtomicComposite` bridges section and atomic layers. Stateless surfaces (headers, rails, heroes, promos, schedules, error states) are server-composed with `bindRef` data resolution and `LiveClock` for live animation | **Done**               |
| **Request context envelope** | Typed `SduiRequestContext` with bracket-notation GET params and POST fallback. `RequestEnvelopeBuilder` on all platforms. Query fields: locale, schemaVersion, deviceClass, capabilities, experiments. Headers: X-Platform, X-App-Version, X-OS-Version, X-Trace-Id, X-Request-Id | **Done**               |
| **Experiment assignment**   | Client-authoritative model (ADR-006). Clients send `experiments` map; server resolves for composition branching. Kill switch client-side. Exposure tracking via fireAndForget | **Done**               |
| **Internationalization (i18n)** | Server pre-translates all text per locale. Section-level `stringTable` for binding-time resolution. Parameterized strings via atomic decomposition | **Done**               |
| **iOS renderer**            | SwiftUI `SduiCore` package. 8 section views + AtomicComposite routing, navigation shell, icon tokens, envelope builder, impression tracker, error boundary, polling. Ably gated for x86_64 simulators | **Done** (partial SSE) |
| **Test coverage**           | Unit/integration tests exist on server, Android, iOS, and web (transport, refresh, schema contract); comprehensive golden-fixture suite + CI dashboards pending | **Partial**            |


### Key Architectural Concepts Demonstrated

Every concept from the original roadmap's prototype validation checklist has been proven in working code:

- **Two-tier primitives** — 12 atomic primitives and 8+ semantic sections coexist via `AtomicComposite`. Sections own SDK integration and client state; stateless surfaces are server-composed atomic trees deployable without app releases
- **Design system integration** — A three-layer token architecture ([`sdui-design-system.md`](sdui-design-system.md)) bridges Figma and rendered output: inline primitives (padding, radius, shadow), named variants resolved per-platform (Liquid Glass on iOS, Material 3 on Android, CSS on web), and color tokens with light/dark adaptation. Designers can build compositions in Figma; the server produces matching atomic trees without client code. When Apple or Google ship a new design language, client resolvers update once and every token-driven screen picks up the new look
- **Single-schema codegen** — one JSON Schema → typed models for Java, Swift, and TypeScript
- **Per-section refresh policies** — static, polling, and SSE coexisting on a single screen
- **Declarative data binding** — real-time messages mapped to fields via server-defined JSONPath bindings
- **Action system** — 6 action types, 8 triggers dispatched through a single handler with precedence (nested > section > screen)
- **A/B variant composition** — server returns different section ordering/content per experiment variant
- **Domain-typed tabular data** — `BoxscoreTable` with frozen columns/sort, `Form` with parameterized refresh, `SeasonLeadersTable` with surgical section merge
- **Platform-aware composition** — `deviceClass` in the request envelope drives per-family tailoring
- **Server-driven image fallback** — `fallbackThumbnailUrl` with platform-native error handling

---

**Capabilities needed for surface expansion :**


| Capability                                          | Needed When                                                 | Effort      |
| --------------------------------------------------- | ----------------------------------------------------------- | ----------- |
| **Layout managers** (placement slots, multi-column) | Tablet-optimized surfaces, surfaces with sidebar navigation | Medium      |
| **Grid/table with pinned rows and columns**         | Standings, full box score surface                           | **Done** — `BoxscoreTable` and `SeasonLeadersTable` built on Android, iOS, and web |
| **Entitlement/paywall awareness**                   | Surfaces with premium content gating                        | Medium      |
| **Form/input/search primitives**                    | Search surfaces, settings, profile editing                  | **Done** — `Form` built on Android, iOS, and web |
| **Responsive/form-factor layouts**                  | Tablet and desktop surface variants                         | Medium      |
| **Design-system style tokens (containers)**         | Platform-native container polish (iOS materials / Liquid Glass, Android Material surfaces, web CSS mixins) as named design-system concepts rather than bags of inline properties. Extends the existing typography/button token pattern to hero cards, surface chrome, etc. | **Done** — ADR-013 accepted and shipped |


Each of these is additive to the current schema — they are introduced when a new surface requires them, not preemptively.

---

## Why Build, Not Buy

We evaluated off-the-shelf SDUI platforms (DivKit by Yandex, Nativeblocks) and concluded they don't meet our requirements:

- **No mixed refresh strategies.** Our game screens needs the scoreboard updating in real time (SSE) while editorial content below it is cached for hours. Off-the-shelf platforms treat the entire screen as one data unit.
- **No data binding to our services.** Vendor platforms serve layout from their backend, but live game data lives in our infrastructure. This creates a dual-source-of-truth problem.
- **Limited design system integration.** Vendor rendering engines replace native components rather than wrapping them, breaking design system consistency.

**The prototype validates this decision.** The working Ably SSE integration with per-section refresh policies — the single hardest requirement — is functioning and would not have been achievable with any evaluated vendor platform.

---

## How It Works (High Level)

**Client App → API Aggregation / Composition Layer → Data Services + Targeting + Personalization**

1. The client requests a screen (e.g., a scoreboard or game detail).
2. The **aggregation/composition layer** assembles the response — fetching content from NBA data services, applying targeting and personalization decisions (including experiment variants), and assigning per-section refresh policies (static, poll with interval + URL, or SSE with Ably channel).
3. The client **renderer** interprets the response — mapping each section type to a native design system component, establishing real-time data channels where needed, and rendering the screen.

The server controls *what* appears and *in what order*. Platform teams control *how* each component looks, feels, and animates natively.

---

## Timeline: Vertical Slices to Production

The prototype has already completed the equivalent of the original Phase 0 (Foundation) and most of Phase 1 (Single-Platform PoC). The timeline below is organized around **vertical slices**, each delivering a shippable, end-to-end outcome. Testing and hardening are built into each slice, not gated as a separate phase.

### Milestone 1: Beachhead Surface on Android — Staging Ready

Harden what exists into a staging-deployable vertical slice.


| Deliverable                                                                                        | Owner               |
| -------------------------------------------------------------------------------------------------- | ------------------- |
| Contract tests for all 9 section types (golden SDUI response fixtures + assertions)                | SDUI Engineering Team           |
| Data binding hardened end-to-end through `DataBindingResolver` (remove hardcoded live score paths) | SDUI Engineering Team           |
| Performance baseline: measure SDUI render time vs. native on Android                               | SDUI Engineering Team + Android |
| Composition service deployed to staging with live NBA data                                         | SDUI Engineering Team           |
| Caching layer: platform-appropriate response cache with stale-while-revalidate                     | SDUI Engineering Team           |


**Exit: Beachhead surface rendering from SDUI on Android in staging with live scores, contract tests passing, performance baselined.**

### Milestone 2: Beachhead Surface — All Platforms in Staging

Expand to iOS and complete web. Each platform renderer is a vertical deliverable — iOS and web can be built in parallel.


| Deliverable                                                                              | Owner                     |
| ---------------------------------------------------------------------------------------- | ------------------------- |
| iOS renderer: SwiftUI component registry + Ably integration (using Android as reference) | iOS team (1–2 engineers)  |
| Web renderer: complete Ably SSE integration, match Android real-time capabilities        | Web team (1 engineer)     |
| Per-platform contract tests using shared golden fixtures                                 | Each platform team + SDUI Engineering Team |
| A/B targeting: clients send experiment assignments in the request envelope; composition branches on the `experiments` map | SDUI Engineering Team                 |
| Observability: render timing dashboards, data channel health monitoring                  | SDUI Engineering Team                 |


**Exit: Beachhead surface rendering from SDUI on all three platforms in staging. A/B variants verified. Contract tests passing on all platforms.**

### Milestone 3: Beachhead Surface — Production

Ship the beachhead surface as the first SDUI-powered production surface.


| Deliverable                                                          | Owner              |
| -------------------------------------------------------------------- | ------------------ |
| Progressive rollout: feature-flagged 5% → 25% → 100% with monitoring | Cross-functional   |
| Automated rollback triggers on error rate thresholds                 | SDUI Engineering Team + DevOps |
| CDN caching for static sections, real-time bypass for live sections  | SDUI Engineering Team          |
| On-call runbook: SDUI-specific debugging with trace ID lookup        | SDUI Engineering Team          |
| Schema v1.0: formalized, versioned, backward-compatible              | SDUI Engineering Team          |


**Exit: Beachhead surface fully SDUI-powered in production on all platforms. Error rates within SLA. Real-time channels stable under live event load.**

### Milestone 4: Second Surface + Schema Evolution

Expand SDUI to a second surface, introducing schema capabilities as the new surface demands them.


| Deliverable                                                                                           | Owner               |
| ----------------------------------------------------------------------------------------------------- | ------------------- |
| Second surface selected and scoped (e.g., Scoreboard, Home Feed, or Player Profile)                   | Product + SDUI Engineering Team |
| Schema additions driven by surface needs (layout primitives, new section types, grid/table if needed) | SDUI Engineering Team           |
| Renderers extended for new section types (~30 lines per type per platform)                            | Platform teams      |
| Design system token integration (semantic color/typography/spacing references in schema)              | SDUI Engineering Team + Design  |


**Exit: Second surface in production. Schema proven extensible beyond a single surface.**

### Summary


| Milestone                     | What Ships                                                       |
| ----------------------------- | ---------------------------------------------------------------- |
| **M1: Android Staging**       | Beachhead surface on Android in staging, tested, performance baselined |
| **M2: All Platforms Staging** | Beachhead surface on Android + iOS + Web in staging with A/B variants  |
| **M3: Production**            | Beachhead surface in production on all platforms                       |
| **M4: Second Surface**        | Second SDUI-powered surface in production                             |

The prototype has already retired the foundational technical risk. Capabilities identified in the mobile native audit (layout managers, entitlement, grid/table) are introduced when a surface requires them — they do not gate the beachhead.

---

## Resourcing

### SDUI Engineering Team (M1–M4)


| Role                       | Count | Focus                                                       |
| -------------------------- | ----- | ----------------------------------------------------------- |
| SDUI Architect / Tech Lead | 1     | Schema design, architecture decisions, cross-team alignment |
| Backend Engineers          | 2     | Composition service, targeting integration, data channels   |
| SDET / QA Engineer         | 1     | Contract testing, visual regression, integration testing    |
| DevOps Engineer            | 1     | Codegen, tooling, observability, deployment                 |


**Total core team: 5–6 engineers.**

### Platform Team Commitment


| Milestone                  | Platform Commitment                             |
| -------------------------- | ----------------------------------------------- |
| M1 (Android Staging)       | SDUI Engineering Team only — Android renderer already built |
| M2 (All Platforms Staging) | iOS: 1–2 engineers. Web: 1 engineer.            |
| M3 (Production)            | 1 engineer per platform for rollout support     |
| M4 (Second Surface)        | 1 engineer per platform for new section types   |


The prototype demonstrates that the renderer is thin wiring (~30 lines per section type) mapping SDUI data to existing design system components. Platform teams own their renderer code and quality — the core team provides the schema contract, reference implementation, and tooling.

---

## Key Risks and Mitigations


| Risk                                  | Impact                                                | Mitigation                                                                                                                                                                           | Status                                                          |
| ------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------- |
| **Platform team adoption resistance** | Renderer work deprioritized or deviates from contract | Leadership endorsement before asking for platform resources. Platform teams retain full control of native rendering. Working Android prototype demonstrates the thin renderer model. | Prototype reduces this risk — teams can see the working example |
| **Schema evolution complexity**       | Breaking changes to older app versions                | Additive-only change policy. Version negotiation protocol implemented: server routes on `schemaVersion`, strips unsupported fields, signals force-upgrade via `X-Schema-Version-Mismatch` header. All clients detect and prompt. | Built — governance process needed at M3         |
| **Performance overhead**              | 50–150ms added vs. native screens                     | Client-side response caching for instant re-renders. Performance budgets enforced in CI.                                                                                             | Benchmarking in M1; caching in M1                               |
| **Real-time channel reliability**     | Stale scores during live events                       | Failure modes enumerated in ADR-010 §Two-Phase Staleness. Per-section staleness tracking (§3b) with SSE → poll fallback cascade.                                                     | Ably SSE working on Android; fallback cascade needed by M3      |
| **Testing gaps**                      | Regressions ship undetected across platforms          | Contract tests built into each milestone using shared golden fixtures. Not a separate phase — each slice ships with its own tests.                                                   | Contract tests in M1                                            |
| **Debugging across layers**           | Longer mean-time-to-resolution for UI bugs            | End-to-end trace IDs from composition through rendering. On-call runbook by M3.                                                                                                      | Trace IDs implemented; dashboards in M2, runbook in M3          |


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
| Composition ownership and transition | **ADR-002 Proposed**              |
| Request/contract model               | **ADR-003 Proposed**              |
| GET/POST + caching policy            | **ADR-004 Proposed**              |
| Action scope and precedence          | **ADR-005 Proposed**              |
| Experiment assignment strategy       | **ADR-006 Accepted** (2026-03-30) |
| Ads boundary and contract            | **ADR-007 Proposed**              |
| Form-factor layout manager           | **ADR-008 Accepted** (Option C — Hybrid) |
| Impression dedup and visibility      | **ADR-009 Accepted** (2026-03-11) |
| Offline and degraded connectivity    | **ADR-010 Proposed**              |
| Data classification and freshness    | **ADR-011 Proposed** (draft)      |
| Client data architecture             | **ADR-012 Proposed** (draft)      |
| Style tokens for atomic primitives   | **ADR-013 Accepted**              |
| Dynamic conditional properties       | **ADR-014 Proposed**              |


---

## Immediate Next Steps (Milestone 1)

1. **Contract tests** — Define golden SDUI response fixtures for all section types. Build assertions validating schema-renderer agreement on Android. Integrate into CI.
2. **Data binding hardening** — Route all live score updates through `DataBindingResolver` end-to-end, eliminating hardcoded mapping paths.
3. **Performance baseline** — Measure SDUI render time vs. native on Android. Establish performance budget.
4. **Caching** — Platform-appropriate response cache with stale-while-revalidate for instant re-renders on screen re-entry.
5. **Staging deployment** — Composition service deployed to staging environment with live NBA data.
6. **iOS hardening** — iOS renderer is functional (SwiftUI package, navigation shell, impression tracker, envelope). Close out remaining gaps: Ably SSE on Apple silicon simulators, parity pass on contract tests, design-system polish.

---

## Revision History

| Date | Summary |
|---|---|
| 2026-05-05 | Doc consistency audit. Schema evolution risk status updated: "Version header working" → "Built" (server version routing + field stripping + client force-upgrade prompt). |
| 2026-04-27 | Doc consistency audit: trigger counts (7 → 8), ADR-010 row added to Decision Continuity, terminology sync. |
| 2026-04-27 | Feature table: second column title → `Description`. Schema version + platform bullets: envelope-only (`platform[name]`, `schemaVersion`). |
| 2026-04-26 | Doc consistency audit. Stripped historical migration narrative — no more "former section types" framing. JSON Schema, Android renderer, Atomic rendering layer, and Two-tier primitives bullets describe current state only. Atomic element count corrected 11 → 12 (`OverlayContainer` added). Milestone 1 contract-test row corrected 10 → 9 section types. |
| 2026-04-25 | Doc consistency audit. Decision Continuity table: “ADR pending” → explicit ADR-00N Proposed for composition ownership, request/contract, GET/POST caching, action scope, ads boundary. |
| 2026-04-24 | Doc consistency audit. Fixed `Client Implementor's Contract` links indirectly referenced from README/agent guidance, updated request-envelope support to include iOS, aligned capability table rows with iOS runtime parity (`BoxscoreTable`, `SeasonLeadersTable`, `Form`), changed Milestone 2 A/B ownership to the client-authoritative request-envelope model, and marked ADR-013 as Accepted/shipped in current-status tables. |
| 2026-04-21 | Doc consistency audit. Section count 9 → 10 (VideoPlayer added as semantic section — platform video SDK). Atomic primitive count 9 → 10 (SectionSlot explicitly enumerated). iOS renderer status **Not started** → **Done (partial SSE)**; reflects live SwiftUI package with section router, atomic router, navigation shell, envelope, impression tracker. Milestone 1 contract-test row 9 → 10. Decision Continuity table adds ADR-011 / ADR-012 and updates ADR-013 to Accepted. Immediate Next Steps replaces "iOS team alignment" with "iOS hardening". |
| 2026-04-20 | Formalized **platform-native realization** as architectural premise: the shared schema carries semantic tokens (`titleMedium`, `primary`, `card`) and each client resolves them into its platform's current design language — iOS idioms (platform materials, Liquid Glass on supported OS versions), Material on Android (tonal elevation, Material 3 Expressive where available), web conventions on web. Cross-platform visual divergence is expected; pixel parity is explicitly not a goal. Added style-token capability row to "Capabilities needed for surface expansion" table referencing ADR-013. |
| 2026-04-01 | Doc consistency audit. Decision Continuity table: form-factor layout manager ADR pending → ADR-008 Accepted (Option C); added ADR-009 Accepted row (impression dedup). |
| 2026-03-30 | Doc consistency audit. Added feature rows for request context envelope, experiment assignment (ADR-006 Accepted), and i18n (section-level stringTable). Updated composition service description. Updated Decision Continuity table (experiment assignment: ADR pending → ADR-006 Accepted). |
| 2026-03-26 | Added Key Results section (OKRs) positioned after "The Opportunity" for executive visibility. Three objectives covering delivery velocity, cross-platform coordination, and design-to-production acceleration. Removed redundant "Success Metrics" section. |
| 2026-03-18 | Beachhead surface generalized — milestones no longer assume Game Detail (remains TBD per Decision Continuity). Caching strategy made platform-agnostic (removed Room dependency). Capabilities table updated: Grid/table and Form/input marked as Done. Revision history moved to top and consolidated from intermittent update notes. |
| 2026-03-13 | Added atomic rendering layer to "What Was Built" table. Updated JSON Schema row (8 → 10 atomic element types). Expanded Two-tier primitives concept to describe AtomicComposite bridge, SectionSlot bidirectional delegation, and DisplayGrid. Reflects Phases 1–5 of atomic primitives implementation. |
| 2026-03-12 | Four server-control gaps closed: `SectionLayoutHints` (server-controlled inter-section margins, dividers, priority; ADR-008 accepted Option C), `SectionStates` (loading skeletons and error messages per section), impression tracking (`useImpressionTracking` with `IntersectionObserver`; ADR-009 accepted), bug fixes (`interactive` contentType enum, `X-Platform` header threading, deserialization failure logging on Android). |
| 2026-03-11 | ErrorState section type built on Web and Android. Server composes explicit error sections (title, message, icon, retry action) at composition time. Client-side visibility expressions evaluated and deferred — server-side composition handles section show/hide. |
| 2026-03-04 | Grid/table and Form/input capabilities designed and implemented. `BoxscoreTable` (semantic tabular data with client-side sort), `Form` (extensible settings pickers with parameterized server refresh), `SeasonLeadersTable` (form-driven refresh with surgical section merge) built on Web and Android. |



