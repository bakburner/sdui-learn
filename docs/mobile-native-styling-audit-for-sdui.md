# Mobile Native Audit for SDUI

## Executive Summary

This audit reverse-engineers the implicit mobile design system and interaction model from the NBA Android and Apple codebases, then compares those patterns against current SDUI prototype capabilities.

Top conclusion: the prototype already demonstrates core SDUI mechanics (typed schema, section routing, actions, polling/SSE hooks, server composition), but it is still one step below native parity for mobile/tablet layout composition. The biggest missing capabilities are layout managers (multi-column, grid, placement), stronger state/data-binding semantics, and production-grade performance/observability/testing.

The highest-leverage path is:
1. add layout/composition primitives first,
2. tighten action/state/binding semantics,
3. implement performance and observability guardrails,
4. then automate this audit with a human-approved findings pipeline.

## Scope and Exclusions

### In scope (mobile only)
- Android mobile app and tablet surfaces:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/settings.gradle`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/build.gradle`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet/build.gradle`
- Apple iOS app and widget UI patterns:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA.xcodeproj/project.pbxproj`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-widget`

### Explicitly excluded
- Non-mobile targets and services:
  - tvOS / androidTV: `NBA-tv`, Android `:tv`, `:whitelabel:tv`
  - visionOS / watchOS: `NBA-visionOS`, `NBA-watchos`, Android `:xr`
  - push/live-activity services and flows (`NBA-pushNotificationService`, `NBAGameLiveActivity`)

## 1) Mobile Module Inventory

### Android inventory (in scope)
- `:app` (primary mobile shell), Compose + ViewBinding + DataBinding:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/build.gradle`
- `:amazonTablet` (tablet variant), XML/View stack:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet/build.gradle`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet/src/main/res/layout/activity_main.xml`
- Shared mobile UI/runtime modules used by app surfaces:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/realtimegames`

### Apple inventory (in scope)
- `NBA-ios` target (phone/tablet target family):
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA.xcodeproj/project.pbxproj`
- `NBA-widgetExtension` target present, but classic home-screen widget scaffold is currently commented and live-activity implementation is excluded by scope:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-widget/NBAWidgetBundle.swift`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-widget/Widgets/NBAWidget.swift`

### Build/UI tech by module
- Android `:app`: Compose-first with hybrid legacy view support.
- Android `:amazonTablet`: View/XML-centric shell.
- Apple `NBA-ios`: mixed UIKit + SwiftUI (with `UIHostingController` bridges).
- Apple widget target: WidgetKit/SwiftUI artifacts exist, but active home-screen widget implementation is not currently enabled.

## 2) Aesthetics / Styling Audit (Derived from Code)

### Android styling system
- Design token core:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaColors.kt`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaTypography.kt`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaShapes.kt`
- Color semantics are strongly tokenized (`canvas`, `surface`, `textPrimary`, `tint`, `live`, `positive`, `negative`, etc.).
- Typography is broad and explicit (display/headline/title/body/label/button/score/data families).
- Shape primitives center on `4dp` radius (`RoundedCornerShape(4.dp)`) with selective larger values (12dp sheets, 24dp tracks).

### Apple styling system
- Semantic color naming and server-name mapping:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUI/Sources/NBAUI/Color/Color+Extensions.swift`
- Font taxonomy and server-name mapping:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUI/Sources/NBAUI/Fonts/Font+NBA.swift`
- Design spacing baseline and tablet constants:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/DesignSystem/Styles.swift`
- Apple code similarly uses semantic token mapping with a consistent naming contract for server-fed styles.

### Repeated value clusters (candidate cross-platform tokens)
- **Spacing cluster**: 4, 8, 16, 20, 32
  - Android examples in `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/main/res/values/dimens.xml`
  - Apple examples in `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/DesignSystem/Styles.swift`
- **Radius cluster**: 4 default; 12 for sheets; 16+ for special cases
  - Android: `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaShapes.kt`
  - Android sheets: `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaBottomSheetLayout.kt`
  - Apple sheets: `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/Utility/HalfSheet.swift`
- **Typography semantic clusters**: headline/display/body/label/button/data maps in both platforms.

### Inconsistencies / hardcoded outliers
- Android dimension outliers (highly feature-specific constants such as `72dp`, `98dp`, `124dp`, `152dp`, `385dp`) indicate local layout tuning versus reusable tokens:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/main/res/values/dimens.xml`
- Apple hardcoded, non-semantic color/spacing values in feature controllers:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/GameDetails/GameDetailsViewController.swift`

## 3) Layout / System Audit

### Patterns observed in native mobile
- Linear and stacking primitives:
  - Android `Row`/`Column`/`Box` patterns in `libs/compose/common`
  - Apple `UIStackView`, SwiftUI `VStack`/`HStack`/`ZStack`
- Scroll containers:
  - Android `LazyColumn`, `LazyRow`, `PullToRefreshBox`:
    - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenContent.kt` (prototype baseline)
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/HorizontalPagerWithTabs.kt`
  - Apple collection/table driven feeds:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/TabbedNavigation/TabbedNavigationViewController.swift`
- Grid/table layouts:
  - Android bidirectional/pinned table:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/lazy/table/LazyTable.kt`
  - Apple compositional collection layouts:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/StatsAndStandings/StatsAndStandingsCollectionViewLayout.swift`
- Overlay/sheet systems:
  - Android modal bottom sheet:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaBottomSheetLayout.kt`
  - Apple sheet detents (`medium`, `large`):
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/Utility/HalfSheet.swift`
- Tablet-aware layout containers:
  - Apple size-class tablet container:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/Profile/ProfileTabletContainer.swift`
  - Android tablet variant app module:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet`

### Complex patterns not currently representable in prototype SDUI
- Multi-column tablet placement and constrained content-width layouts.
- Grid/table layouts with sticky rows/columns and z-index pinning.
- Detent-based sheets and richer modal presentation policy.
- Scroll-aware/sticky/animated section behaviors.

## 4) Component Inventory and SDUI Primitive Candidates

### Reusable components (mobile-native evidence)
- Android reusable UI library:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common`
  - Representative components: `NbaButton.kt`, `NbaTextField.kt`, `HorizontalPagerWithTabs.kt`, `NbaBottomSheetLayout.kt`
- Apple reusable UI packages:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUIComponents/Sources/NBAUIComponents`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/DesignSystem/Atoms`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/DesignSystem/Molecules`

### Candidate SDUI semantic primitives
- Keep/extend existing primitives already in schema:
  - `Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`
  - `/Users/adrianrobinson/Projects/sdui-prototype/schema/sdui-schema.json`
- Add missing semantically-rich primitives:
  - `GridContainer` / `TableContainer` (pinned headers/columns support)
  - `SheetContainer` (detent/presentation policy)
  - `TabContainer` with paged and swipe behaviors
  - `InputField` / `SearchField` / `FormGroup`
  - `StateView` (`loading`, `error`, `empty`) with standardized fallback behavior

## 5) Interaction / State Audit

### Navigation, tabs, gestures
- Android app and compose libraries show URI-driven navigation and tab-pager patterns.
- Apple relies heavily on coordinator/service + tab container + swipe gestures:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAServices/Sources/NBAServices/Services/NavigationService/NavigationService.swift`
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/TabbedNavigation/TabbedNavigationViewController.swift`

### Input/search/forms
- Android: reusable text fields and search patterns:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaTextField.kt`
- Apple: reusable `UISearchBar` wrapper and form molecules:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUIComponents/Sources/NBAUIComponents/SearchBar.swift`

### IAP / subscription / entitlement interactions (key concept)
- Mobile apps contain strong entitlement and upsell/paywall flows that influence UI and navigation decisions:
  - Apple examples:
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/VideoPlayer/VideoPlayerPaywallView.swift`
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/VideoPlayer/VideoPlayerCore+PaywallResolution.swift`
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Services/OpinService.swift`
  - Android examples (mobile app test and feature evidence):
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/OnboardingPaywallUITests.kt`
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/UpsellBannerUiTests.kt`
    - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/LeaguePassCardUiTests.kt`
- SDUI implication:
  - SDUI should not own store transaction plumbing directly, but it must express entitlement-aware rendering and action branching.
  - Required semantics include entitlement predicates (`isEntitled`, `requiresSubscription`), paywall variant selection, offer/product identifiers, and success/failure callbacks that re-evaluate section visibility.

### Loading/error/empty states
- Android native pattern: `UIState` + reusable state container:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaLazyLayout.kt`
- Prototype SDUI currently handles loading/error at screen-level and simpler section-level fallbacks:
  - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenContent.kt`

### Real-time update patterns
- Android native real-time repository + Ably wrapper:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/realtimegames/src/main/java/com/nba/realtimegames/LiveGameRepository.kt`
- Apple mobile live score refresh cadence and merge logic:
  - `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/LiveScore/LiveScoreCore.swift`
- Prototype SDUI real-time mechanisms:
  - SSE/poll policy in schema:
    - `/Users/adrianrobinson/Projects/sdui-prototype/schema/sdui-schema.json`
  - Android orchestration:
    - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt`

## 6) SDUI Gap Analysis vs Current Prototype

### Current capability baseline (prototype)
- Schema types and actions:
  - `/Users/adrianrobinson/Projects/sdui-prototype/schema/sdui-schema.json`
- Android section routing:
  - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SectionRouter.kt`
- Android screen orchestration/poll/SSE:
  - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt`
- Action semantics:
  - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt`
- Data-binding utility:
  - `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/data/DataBindingResolver.kt`
- Server composition:
  - `/Users/adrianrobinson/Projects/sdui-prototype/server/src/main/java/com/nba/sdui/service/SduiCompositionService.java`
- Web prototype refresh:
  - `/Users/adrianrobinson/Projects/sdui-prototype/web/src/hooks/useRefreshPolicy.ts`

### Gap matrix

#### Schema requirements (missing/weak)
- No first-class layout model (placements, breakpoints, multi-column/grid composition).
- No form/input/search semantics.
- No explicit accessibility metadata.
- No section-level presentation semantics (sheet/modal/sticky).
- No first-class entitlement/paywall schema semantics (entitlement predicates, offer metadata, purchase outcome handlers).

#### Renderer/runtime requirements
- Android/web render a mostly linear section list.
- No native-equivalent grid/table or pinned-cell layout manager in SDUI runtime.
- No robust section-level fallback policy when bindings/updates fail.

#### Action/state/data-binding requirements
- Action model is present but still prototype-level (e.g., refresh action result not deeply wired to target-specific refetch orchestration semantics).
- Data-binding utility exists, but update integration is still partially hardcoded in live score path.
- Missing transform/validation contract for bound values and failure telemetry.
- Missing entitlement-state contract (server hint + client verification + post-purchase state reconciliation).

#### Performance/observability/testing requirements
- No formal caching/offline strategy in runtime path.
- Limited runtime telemetry beyond logs/trace IDs.
- No comprehensive contract tests for schema-renderer agreement.
- No visual regression coverage across variants/breakpoints.

## 7) Candidate Layout Managers / Composition Primitives

This section prioritizes additions needed to close the largest native parity gaps.

### P0 (must-have)
- **`PlacementLayoutManager`** (header/main/footer/aside slotting)
  - Evidence: tablet and mixed shell composition needs across mobile apps.
  - Schema implication: `screen.layout.placements` + section references.
  - Runtime implication: slot-aware rendering pipeline instead of strict list ordering.
- **`ResponsiveLayoutManager`** (phone/tablet breakpoints)
  - Evidence: Apple size-class and tablet width constraints (`ProfileTabletContainer`, `Styles.Tablet`).
  - Schema implication: form-factor layout maps (`compact`, `regular`, etc.).
  - Runtime implication: deterministic breakpoint selector and fallback resolution.
- **`GridTableLayoutManager`** (grids + pinned rows/columns)
  - Evidence: Android `LazyTable`, Apple compositional grid patterns.
  - Schema implication: columns/rows/pin/spans/section-spacing controls.
  - Runtime implication: performant cell virtualization and pinning behavior.

### P1 (high value)
- **`SheetPresentationManager`**
  - Evidence: Android and Apple both use configurable sheet patterns.
  - Schema implication: detents, grabber, dismiss/backdrop behavior.
  - Runtime implication: platform adapter for modal-bottom-sheet policies.
- **`StickyScrollLayoutManager`**
  - Evidence: native sticky tab/header and offset-aware interactions.
  - Schema implication: sticky flags + scroll trigger metadata.
  - Runtime implication: scroll observer integration and pinned rendering.
- **`InputFormLayoutManager`**
  - Evidence: reusable search/text-field/form controls in both stacks.
  - Schema implication: form field types, validation, submit actions.
  - Runtime implication: validation/dirty state and keyboard/focus handling.

### P2 (important but can follow)
- **`AnimatedTransitionManager`** for section and state transitions.
- **`WidgetSurfaceAdapter`** for future non-live-activity widget payload specialization.
- **`AdvancedStateViewManager`** for standardized loading/error/empty skeleton policies.

## Not Yet Accounted For

This list is explicit and intentionally strict.

- Tablet-first composition patterns (width constraints, split layouts, breakpoints).
- Pinned table/grid semantics and sticky section mechanics.
- First-class input/search/form schema support.
- First-class IAP/entitlement/paywall schema + state contract.
- Structured accessibility contract in schema and runtime.
- Deterministic action chaining/transaction semantics.
- Binding transforms, type guarantees, and observable failure handling.
- Production-grade telemetry (section render timings, action outcomes, binding success/failure rates).
- Contract + visual regression testing strategy.

## Prioritized Roadmap (P0/P1/P2)

### P0 (Foundational work)
- Add schema + runtime support for:
  - placement layout
  - responsive breakpoints
  - grid/table manager
- Add entitlement-aware SDUI contract:
  - section-level visibility predicates and paywall/upsell metadata
  - action outcomes for purchase success/failure/restore and re-render triggers
- Upgrade binding integration from ad hoc updates to unified resolver path.
- Define testable contract for unknown/missing section behavior and binding failure fallback.

### P1 (Quick wins + high-value extensions)
- Introduce sheet presentation primitives and sticky behaviors.
- Add form/search primitives with action wiring and validation.
- Add telemetry hooks for section/action/binding execution.

### P2 (Scale and hardening)
- Add animation/transitions and richer adaptive rendering variants.
- Add snapshot/visual regression and broader integration suites.
- Extend audit-to-codegen pipeline with strict approval gates.

## Suggested First 5 Implementation Tickets

1. **Schema: add `layout` and `placements`**
   - Extend `Screen` schema with placement slots and section references.
   - Add form-factor-aware layout selection fields.

2. **Android runtime: implement `PlacementLayoutManager` + breakpoint resolver**
   - Update `SduiScreenContent` to render by layout slots, not only a flat `LazyColumn`.

3. **Schema + runtime: add entitlement/paywall contract**
   - Add section predicates and offer metadata (`requiresEntitlement`, `paywallVariant`, `offerId`, `productId`).
   - Add action result hooks (`onPurchaseSuccess`, `onPurchaseFailure`, `onRestoreSuccess`) and state reconciliation rules.

4. **Schema + runtime: introduce `GridTable` section**
   - Minimum viable fields: columns, spacing, pinning config, cell payload.
   - Add renderer routing and fallback behavior for unsupported configs.

5. **Binding runtime hardening**
   - Route live score updates through `DataBindingResolver` end-to-end.
   - Add typed transform hooks and failure telemetry.

## Automation Follow-Through (Human-in-the-Loop)

The next phase after this report should automate extraction/synthesis, but keep human approval mandatory before any code generation.

### Approved findings contract (proposed)
- Required fields:
  - `findingId`, `platform`, `category`, `severity`, `evidencePaths`, `nativePattern`, `sduiGap`, `recommendation`, `proposedTicket`
- Required workflow states:
  - `draft -> reviewed -> approved -> eligible_for_codegen`
- Hard gate:
  - codegen only consumes findings in `eligible_for_codegen`.

### Audit app MVP in this repo
- Location:
  - `/Users/adrianrobinson/Projects/sdui-prototype/tools/mobile-audit`
- MVP responsibilities:
  - extract module/style/layout/component/interaction signals,
  - synthesize gap matrix,
  - emit markdown + structured findings,
  - support reviewer approval workflow.
- Non-goals:
  - autonomous commits or auto-merged PRs.

## Evidence Index (Primary Files)

### Android native evidence
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/settings.gradle`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/build.gradle`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet/build.gradle`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/amazonTablet/src/main/res/layout/activity_main.xml`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/main/res/values/dimens.xml`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaColors.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaTypography.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/theme/NbaShapes.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaButton.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaTextField.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/HorizontalPagerWithTabs.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/common/NbaBottomSheetLayout.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/compose/src/main/java/com/nba/compose/lazy/table/LazyTable.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/libs/realtimegames/src/main/java/com/nba/realtimegames/LiveGameRepository.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/OnboardingPaywallUITests.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/UpsellBannerUiTests.kt`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/android/app/src/test/java/com/nba/nextgen/iap/LeaguePassCardUiTests.kt`

### Apple native evidence
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA.xcodeproj/project.pbxproj`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/DesignSystem/Styles.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/Profile/ProfileTabletContainer.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/TabbedNavigation/TabbedNavigationViewController.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/GameDetails/GameDetailsViewController.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/LiveScore/LiveScoreCore.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Views/Utility/HalfSheet.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/VideoPlayer/VideoPlayerPaywallView.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Features/VideoPlayer/VideoPlayerCore+PaywallResolution.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-ios/Services/OpinService.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUI/Sources/NBAUI/Color/Color+Extensions.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUI/Sources/NBAUI/Fonts/Font+NBA.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAServices/Sources/NBAServices/Services/NavigationService/NavigationService.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/Shared/NBAUIComponents/Sources/NBAUIComponents/SearchBar.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-widget/NBAWidgetBundle.swift`
- `/Users/adrianrobinson/Projects/SDUI-BL-MS/apple/NBA-widget/Widgets/NBAWidget.swift`

### Prototype capability evidence
- `/Users/adrianrobinson/Projects/sdui-prototype/schema/sdui-schema.json`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SectionRouter.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenContent.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/data/DataBindingResolver.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/android/sdui-core/src/main/java/com/nba/sdui/core/data/SduiRepository.kt`
- `/Users/adrianrobinson/Projects/sdui-prototype/server/src/main/java/com/nba/sdui/controller/SduiController.java`
- `/Users/adrianrobinson/Projects/sdui-prototype/server/src/main/java/com/nba/sdui/service/SduiCompositionService.java`
- `/Users/adrianrobinson/Projects/sdui-prototype/web/src/hooks/useSduiScreen.ts`
- `/Users/adrianrobinson/Projects/sdui-prototype/web/src/hooks/useRefreshPolicy.ts`
