# ADR-001: SDUI Runtime vs Legacy Card Refactor

- Status: Proposed
- Date: 2026-02-20
- Decision owners: 

## Revision History

| Date | Summary |
|---|---|
| 2026-02-20 | Initial draft — decision, context, options, and recommendation. |
| 2026-02-27 | Trimmed evidence to summary table with appendix. Removed "What Requires an App Update" and "Organizational Drivers" sections. Merged defect risk into Option A cons. Added library-vs-inline comparison for new section types. |
| 2026-03-13 | Qualified "new section types" cost model; added atomic composition layer as Option B advantage (reflects implemented AtomicComposite + 10-element atomic primitives). |

## Decision

Build a new SDUI rendering runtime/library and migrate incrementally via adapters, rather than refactoring existing legacy `module/card` client pipelines in place.

## Context

CoreAPI `module/card` and proposed SDUI `screen/section` share a similar containment hierarchy. The structural vocabulary is intentionally similar to reduce migration risk. The difference is not JSON shape — it is replacing platform-specific behavioral interpretation with a shared execution contract.

| Aspect | Feed API (`module/card`) | SDUI (`screen/section`) |
|--------|--------------------------|-------------------------|
| Containment hierarchy | Feed → Module → Card | Screen → Section → Component |
| Same structural idea? | **Yes** | **Yes** |
| Behavioral contract | Absent — each client interprets | Explicit (`actions`, `refreshPolicy`, `dataBindings`, `state`) |
| Cross-platform governance | None — drift detected at integration | Schema + codegen + contract tests |

**Key question:** refactor existing pipelines in place (Option A) or introduce a new runtime and migrate by surface (Option B)?

## Existing Feed API Capabilities

The feed API already handles significant server-side control. Any honest evaluation must acknowledge what is already deployable without an app update today:

| Capability | Already server-side? | Mechanism |
|------------|---------------------|-----------|
| Add / remove / reorder modules | **Yes** | Feed composition in CoreAPI |
| Swap a module by placement | **Yes** | Playmaker (`placementId`) |
| Change data payloads (scores, stats, copy) | **Yes** | CoreAPI data layer |
| Change navigation destinations | **Yes** | `resourceLocator` (`resourceUrl` / `resourceId` / `resourceType`) |
| Ad slot positioning | **Yes** | `adPlacement` modules in feed response |
| Tab structure and ordering | **Yes** | Tab config endpoint |

### What the feed API does *not* govern today

| Gap | Current state | Impact |
|-----|---------------|--------|
| Interaction behavior (tap, swipe, long-press) | Hard-coded per `cardType` in each client | Behavior change = app update on every platform |
| Refresh / live-data policy | Each client implements its own polling/socket logic | Inconsistent update cadence across platforms |
| Analytics event contract | Client-defined, diverges silently | Unreliable cross-platform metrics |
| Field-level data binding | Clients extract fields by convention, not contract | Silent breakage when payload shape changes |
| Screen state + mutations | No server concept; clients manage locally | Identical state logic reimplemented per platform |
| Cross-platform behavior governance | None | Same card behaves differently on each platform |

## Evidence: Platform Inconsistency and Coupling

The same card types produce different behavior on each platform because there is no shared behavioral contract. Representative examples:

| Platform | Example | Impact |
|----------|---------|--------|
| Apple | `leaguePassCard` — separate cell/action wiring in `FeedViewController` + `LeaguePassCardView` | Entitlement and purchase flow logic reimplemented independently |
| Android | `leaguePassCard` — entitlement, SKU, A/B variant, purchase flow all in `LeaguePassCardView.kt` | Same card, different behavioral assumptions than Apple |
| cweb | `dynamicCTA` — `DynamicCard` union **omits** it entirely | Card type silently missing on one platform |
| Web | Layout/ad logic derived from first-card type in module (`DynamicContentCarousel.js`) | Rendering path coupled to card identity, not contract |

Every client independently maps card/module identity to rendering logic via type-switch architectures (`JsonWrapper.kt`, `Base.swift`, `moduleFactory.tsx`, `DynamicContentCarousel.js`). Clients derive behavior from card identity instead of a semantic contract — for example, `UpsellCtaCardData` is effectively empty server-side yet each platform implements distinct CTA behavior independently.

See [Appendix: Evidence Details](#appendix-evidence-details) for full per-platform breakdowns.

**Note on new section types:** Both options use semantic section types, so introducing a wholly new visual component *with client-owned state* (e.g., sort controls, form input, frozen-column tables) requires a renderer update and app release regardless of approach. The difference is where that renderer code lives and how it is maintained.

However, the SDUI runtime (Option B) enables a second path: **AtomicComposite** sections composed entirely from server-defined primitives (`Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`, `DisplayGrid`). These require zero client code — the server describes layout and content using a fixed set of atomic elements, and the client renders them generically. New visual components that are primarily presentational (promo banners, content rails, stat lines, hero panels) can ship without an app release. Only sections that need truly native interaction patterns remain in the semantic tier.

## Options

### Option A: Refactor existing card pipelines in place

| | |
|---|---|
| **Pros** | Lower immediate upfront investment; leverages existing runtime paths |
| **Cons** | Preserves type-switch coupling; requires distributed edits per behavior change; higher regression risk; continues cross-platform inconsistency. Specific risks: cross-platform contract mismatch (e.g., `dynamicCTA` decoded differently on each platform — high severity), behavior-heavy card divergence (e.g., `leaguePassCard` entitlement logic — high), coupling amplification from synced changes across decoders/unions/mappers (medium). |

New section type renderers are added inline to each platform's existing codebase — mixed with legacy type-switch paths, no shared structure or test patterns across platforms, and no guarantee that the same section type is implemented consistently.

### Option B: New SDUI runtime + incremental adapters (recommended)

| | |
|---|---|
| **Pros** | Centralized behavior semantics; clear contract/versioning; isolated migration risk per surface; cross-platform consistency; atomic composition layer enables new visual components without app release |
| **Cons** | Upfront runtime build cost; temporary dual-stack during migration; requires schema governance; adds a library dependency with its own release cycle |

New section type renderers are added to a dedicated SDUI library per platform — isolated from legacy code, following a consistent pattern (router registration + typed model mapping), and testable against shared contract fixtures. The library boundary enforces that renderers conform to the schema contract rather than ad-hoc platform conventions. Additionally, the library's atomic rendering layer lets the server compose entirely new layouts from a fixed set of primitives — no renderer code or app release required. The tradeoff is an additional dependency to version and release.

## Recommendation

Adopt **Option B**: new SDUI runtime/library with incremental migration.

| Phase | Scope |
|-------|-------|
| 1 | Adapter/composer layer to emit SDUI `screen/section` from CoreAPI source |
| 2 | Migrate low-risk presentational surfaces (content rails, basic CTA) |
| 3 | Migrate complex semantic surfaces (`dynamicCTA`, `leaguePassCard`, live game states) |
| 4 | Retire legacy card-type rendering paths per surface after stabilization |

Phases 2–3 are accelerated by the atomic primitives layer: presentational sections can be migrated to server-composed `AtomicComposite` rather than requiring per-section client renderers, keeping the client surface area stable while the server gains full compositional freedom.

## Consequences

| Timeframe | Outcome |
|-----------|---------|
| Short term | Dual-stack operational overhead |
| Medium term | Fewer platform-specific branches; safer feature evolution |
| Long term | Consistent behavioral contracts across all platforms; server-composed atomic layouts eliminate app releases for presentational changes |

---

## Appendix: Evidence Details

### Same card type, different behavior per platform

| Card type | Apple | Android | Web / cweb |
|-----------|-------|---------|------------|
| `dynamicCTA` | `CardType.cardData(...)` in `Base.swift` | Dedicated `DynamicCtaPresenter` + card state model | cweb `DynamicCard` union **omits** it entirely |
| `leaguePassCard` | Separate cell/action wiring in `FeedViewController` + `LeaguePassCardView` | Entitlement, SKU, A/B variant, purchase flow in `LeaguePassCardView.kt` | Page-specific preview path in `GamesView.js` |

### Type-switch rendering architecture

Every client independently maps card/module identity to rendering logic:

| Platform | Pattern | Location |
|----------|---------|----------|
| Android | `cardType` → `FeedItem` subtype registry | `JsonWrapper.kt` |
| Apple | `cardType` → model switch | `Base.swift` |
| cweb | `switch(moduleType)` + `switch(cardType)` | `moduleFactory.tsx` |
| Web | Card-type-specialized carousel branches | `DynamicContentCarousel.js` |

### Server contract gaps force client assumptions

- `UpsellCtaCardData` is effectively empty server-side; clients still implement distinct CTA behavior and UI assumptions independently.
- Clients derive behavior from card identity (e.g., `cardType === 'game'`) instead of a semantic contract.

### Platform-specific API contract branching

| Platform | Default | Effect |
|----------|---------|--------|
| Web | `platform: 'web'` | Different payload behavior |
| cweb | `platform: 'ced'` | Different payload behavior |
| Web watch | Layout/ad logic based on first-card type in module | Platform-specific rendering paths |

