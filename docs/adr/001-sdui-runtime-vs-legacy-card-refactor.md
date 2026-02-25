# ADR-001: SDUI Runtime vs Legacy Card Refactor

- Status: Proposed
- Date: 2026-02-20
- Decision owners: 

## Decision

Build a new SDUI rendering runtime/library and migrate incrementally via adapters, rather than refactoring existing legacy `module/card` client pipelines in place.

## Context

CoreAPI `module/card` structures and proposed SDUI `screen/section` are similar in shape, but current clients are tightly coupled to platform-specific card contracts and renderer branching.

The key decision is whether to:

- Option A: Refactor existing client pipelines in place
- Option B: Introduce a new SDUI runtime and migrate by surface

### Not New Hierarchy, New Runtime Contract

This proposal is intentionally similar in structural vocabulary to reduce migration risk, but materially different in runtime semantics and governance.

- Similar: hierarchical content containers (`module/card` and `screen/section`).
- Different: SDUI standardizes behavior semantics (refresh, data binding, action/state handling, and versioned contract governance) that are currently interpreted differently per platform.
- Decision implication: the main change is not JSON shape, it is replacing platform-specific interpretation with a shared execution contract.

## Evidence: High-Signal Inconsistency and Tight Coupling

### 1) Same card type, different interface contracts by platform

- `dynamicCTA`:
  - Apple decodes `dynamicCTA` through `CardType.cardData(...)` in `apple/Shared/NBAFeedDomain/Sources/NBAFeedDomain/Base.swift`.
  - Android uses a dedicated `DynamicCtaPresenter` and card state model in `android/app/src/main/java/com/nba/nextgen/feed/cards/dynamiccta/DynamicCtaPresenter.kt`.
  - cweb `DynamicCard` union omits `dynamicCTA` in `cweb/src/types/dynamicCards/index.ts`.
- `leaguePassCard`:
  - Android includes entitlement checks, SKU/product lookup, A/B variant rendering, and purchase flow in `android/app/src/main/java/com/nba/nextgen/feed/cards/leaguepass/LeaguePassCardView.kt`.
  - Apple has separate league pass cell/action wiring in `apple/NBA-ios/Features/Feed/FeedViewController.swift` and `apple/NBA-ios/Features/Feed/Cards/LeaguePassCard/LeaguePassCardView.swift`.
  - Web uses a page-specific league pass preview path in `web/src/views/games/GamesView.js` rather than a shared card semantic runtime.

### 2) Type-switch rendering architecture across clients

- Android uses a `cardType -> FeedItem subtype` registry in `android/libs/base/src/main/java/com/nba/base/json/JsonWrapper.kt`.
- Apple uses `cardType -> model` switch logic in `apple/Shared/NBAFeedDomain/Sources/NBAFeedDomain/Base.swift`.
- cweb uses both `switch(module.moduleType)` and `switch(card.cardType)` in `cweb/src/components/DynamicModules/moduleFactory.tsx`.
- Web uses card-type-specialized branches in dynamic carousel rendering in `web/src/components/DynamicContentCarousel/DynamicContentCarousel/DynamicContentCarousel.js`.

Representative patterns:

- `switch (card.cardType) { ... }`
- `switch (module.moduleType) { ... }`
- `if (cardType === 'nbaTvCollectionCard') { ... }`

### 3) Server contract gaps force client assumptions

- `UpsellCtaCardData` is effectively empty in server domain modeling (`CoreAPI/src/apps/server/NBA.NextGen.CorePlatform.Domain/Feeds/CardData/UpsellCtaCardData.cs`), while clients still implement distinct CTA behavior and UI assumptions.
- Multiple clients derive behavior from card identity instead of semantic contract, e.g. filtering for `cardType === 'game'` in `web/src/utils/map-game-cards-to-game-objects.js` and transform logic in `cweb/src/api/core/events/events.ts`.

### 4) Platform-specific API contract branching exists today

- Web defaults `platform: 'web'` in `web/src/api/core-api.js`.
- cweb defaults `platform = 'ced'` in game details requests in `cweb/src/api/core/events/events.ts`.
- Web watch pages apply layout and ad logic based on first-card type in a module (e.g., collection-card special handling) in `web/src/views/watch/featured/WatchFeaturedView.js` and `web/src/components/DynamicContentCarousel/DynamicContentCarousel/DynamicContentCarousel.js`.

This already creates contract divergence and test matrix expansion.

## Organizational Drivers and Governance

- Cross-platform visibility gap: most contributors optimize for a single client (Android, iOS/tvOS, web, or cweb), so interface drift for "the same card" is easy to miss until late integration.
- Governance opportunity: move to a contract-first model where semantic changes are reviewed against a shared SDUI schema and cross-platform conformance checks.
- Contract stabilization: define one versioned semantic contract for initialization/update behavior instead of platform-specific interpretation of card payloads.
- Outcome: improved consistency, clearer ownership of breaking vs non-breaking changes, and fewer regressions caused by silent cross-platform divergence.

## Options

### Option A: Refactor existing card pipelines in place

Pros:

- Lower immediate upfront platform investment.
- Leverages existing runtime paths and components.

Cons:

- Preserves card-type switch architecture and coupling.
- Requires many distributed edits per new semantic behavior.
- Higher regression risk across existing surfaces.
- Continues interface inconsistency for "same card" across platforms.

#### Defect Risk Profile (Option A)

- Cross-platform contract mismatch for the same card:
  - `dynamicCTA` is decoded/handled differently across Apple, Android, and cweb (`Base.swift`, `DynamicCtaPresenter.kt`, cweb dynamic card union).
- Behavior-heavy card divergence:
  - `leaguePassCard` includes entitlement, purchase, and variant logic in different platform-specific implementations (Android vs Apple vs Web page-specific flow).
- Platform request contract branching:
  - `platform: 'web'` vs `platform = 'ced'` defaults can return different payload behavior and increase regression/test matrix complexity.
- Coupling amplification:
  - Any "align behavior across platforms" refactor requires synchronized changes in decoders, unions/enums, mappers, and render switch points, increasing defect probability.

### Option B: New SDUI runtime + incremental adapters (recommended)

Pros:

- Centralized semantics (`refreshPolicy`, `dataBindings`, actions, state).
- Better cohesion and lower coupling.
- Clear contract/versioning path.
- Isolated migration risk and rollback per surface.
- Better long-term cross-platform consistency.

Cons:

- Upfront runtime build cost.
- Temporary dual-stack complexity during migration.
- Requires schema governance and conformance testing.

## Recommendation

Adopt Option B: create a new SDUI runtime/library and migrate incrementally.

Suggested rollout:

1. Keep CoreAPI as initial source; add an adapter/composer to emit SDUI `screen/section`, or leverage a new endpoint if needed to satisfy the data and business logic required to initialize and update views.
2. Migrate low-risk presentational surfaces first (content rails/cards, basic CTA).
3. Migrate complex semantic surfaces next (`dynamicCTA`, `upsellCTA`, `leaguePassCard`, event/game live states) with explicit SDUI primitive extensions.
4. Retire legacy card-type rendering paths per surface after stabilization.

## Consequences

- Short term: dual-stack operational overhead.
- Medium term: fewer platform-specific card branches and safer feature evolution.
- Long term: more consistent interface contracts across Android, iOS/tvOS, web, and cweb.

