# Plan: Mobile Atomic Contract Parity Audit

## Source

This plan captures the proactive audit for places where Android and iOS parse SDUI style, layout, action, or presentation fields but ignore, silently downgrade, or only partially implement them.

## Execution Status

Status as of 2026-04-24: initial mobile execution pass complete.

- Phase 1 layout parity: implemented iOS atomic flex/breakpoint/stretch, iOS scroll paging/view-aligned snapping hooks, Android stretch behavior, Android vertical/horizontal paging, server-authored scroll spacing, and diagnostics for unsupported scroll constraints.
- Phase 2 visual box model parity: implemented iOS Kingfisher-backed image caching/fallback behavior, removed Android's hardcoded branded image fallback, kept payload placeholder retry policy, and constrained atomic background images with diagnostics.
- Phase 3 interaction/action parity: wired iOS text actions through the shared action trigger path and added Android diagnostics for decoded button icons until native icon rendering is implemented.
- Phase 4 navigation presentation parity: preserved Android navigation presentation/modalHeight metadata, implemented iOS replace navigation, and added diagnostics when modal/fullscreen presentation falls back to push because no native modal host is registered.
- Phase 5 diagnostics and guardrails: added visible diagnostics for constrained fields and removed stale iOS atomic border comments.

Verification run:

- `make ios-build`
- `./gradlew :sdui-core:clean :sdui-core:compileDebugKotlin`
- `./gradlew :sdui-core:compileDebugKotlin`

Remaining follow-up:

- Add dedicated contract tests/snapshots for the high-risk fields listed in each phase.
- Implement Android button icon rendering instead of warning-only diagnostics once active payloads emit button icons.
- Add native modal/fullscreen host support for navigation presentation if the POC needs more than logged push fallback.
- Add inline failure feedback hosts if active payloads require `failureFeedback.style: inline`.

Related plans:

- [Mobile Core Library Audit Findings](plan-mobile-core-library-audit-findings.md)
- [Atomic Element Box Model Unification](plan-atomic-element-box-model-unification.md)
- [Layout Responsive](plan-layout-responsive.md)

Out of scope:

- Web cleanup. Track web-specific findings in [Web Core Library Audit](web-core-library-audit-2026-04-24.md).
- Native `GamePanel` renderer work. Game cards are server-composed `AtomicComposite` payloads.

## Findings Inventory

| Priority | Finding | Platforms | Classification |
|---|---|---|---|
| High | Image variants are only partially applied. `thumbnail`-style values can be treated as unknown container variants, and image variant `cornerRadius`, `clip`, and `fillWidth` are not fully honored. | Android, iOS | Partial implementation |
| High | Atomic background image support is inconsistent. Section surfaces can render image backgrounds, but atomic element background images are not fully supported. | Android, verify iOS | Silent ignore / parity gap |
| High | Container layout fields are incomplete: `flex`, `breakpoint`, `crossAlignment: stretch`, scroll `paging`, and `snapAlignment`. | Android, iOS | Silent ignore / downgrade |
| High | Navigation presentation metadata is parsed but downgraded. Modal/fullscreen/replace/modalHeight behavior is not consistently honored. | Android, iOS | Silent downgrade |
| Medium | Some wrapper atomics do not apply their own box model, including `Conditional`, `Spacer`, and `SectionSlot` on iOS. | iOS | Silent ignore |
| Medium | Text actions are not wired on iOS. | iOS | Silent ignore |
| Medium | Button `icon` is ignored on Android. | Android | Silent ignore |
| Medium | `Image.placeholder` behavior is not aligned with the chosen mobile policy. | Android, iOS | Parity gap |
| Medium | Android `ScrollContainer` has hardcoded inner padding and may conflate `showIndicators` with scroll enablement. | Android | Hardcoded behavior / semantic mismatch |
| Medium | `failureFeedback.style` is ignored or simplified. | iOS, verify Android host behavior | Silent ignore |
| Low | Shadow color/offset fields are approximated on Android via elevation/radius only. | Android | Documented downgrade |
| Low | Unknown badge alignment and accessibility roles can fall back silently. | Android | Silent fallback |
| Low | Variant override diagnostics may be too quiet for QA because some style drops log at debug level. | Android, iOS | Low-visibility diagnostics |
| Low | iOS comments mention atomic borders even though `AtomicElement` has no `border` field in the schema. | iOS docs/comments | Documentation drift |

## Phase 0: Contract Classification

### What To Decide

For each finding, classify the behavior as one of:

1. **Must implement now**: field is in active payloads or blocks current UX.
2. **Must log and track**: field is in the schema, not yet required by payloads, but should not fail silently.
3. **Intentional platform approximation**: behavior differs by platform for a documented native reason.
4. **Remove or constrain from schema/composition**: field should not be emitted for that element type.

### Initial Recommendations

| Finding | Classification | Phase 0 rationale / implementation handoff |
|---|---|---|
| Image variants are only partially applied. `thumbnail`-style values can be treated as unknown container variants, and image variant `cornerRadius`, `clip`, and `fillWidth` are not fully honored. | **Must implement now** | `thumbnail` is an active server-emitted image variant for content/video/VOD cards, and the style-token registry defines native corner radius, crop, clip, and caller-owned sizing behavior. Keep image variant resolution image-specific; do not route image variant strings through container variant semantics. |
| Atomic background image support is inconsistent. Section surfaces can render image backgrounds, but atomic element background images are not fully supported. | **Remove or constrain from schema/composition** | No active server composition currently emits an atomic `background.imageURL`; active atomic backgrounds are solid/gradient, while section surfaces already own image background rendering. For this POC, constrain `AtomicElement.background` to solid/gradient or composer-owned image children instead of adding another atomic background-image path. Keep section-surface image backgrounds in scope for existing surface handling. |
| Container layout fields are incomplete: `flex`, `breakpoint`, `crossAlignment: stretch`, scroll `paging`, and `snapAlignment`. | **Must implement now** | `flex`, `breakpoint`, and `crossAlignment: stretch` are in the schema and active server composition uses responsive rows and stretch alignment. iOS does not apply `flex`/`breakpoint`, and both mobile renderers downgrade stretch. Scroll `paging`/`snapAlignment` are not active payload requirements yet, but because they are generic layout fields, unsupported values should be warned/tracked until implemented. |
| Navigation presentation metadata is parsed but downgraded. Modal/fullscreen/replace/modalHeight behavior is not consistently honored. | **Must implement now** | `presentation: modal` is emitted by the demo subscribe CTA, and both generated mobile models decode `presentation`/`modalHeight`. iOS currently only distinguishes `external` from push; Android action adaptation drops presentation metadata entirely. Missing or default presentation may keep push behavior, but explicit modal/fullscreen/replace/modalHeight must not silently normalize to push. |
| Some wrapper atomics do not apply their own box model, including `Conditional`, `Spacer`, and `SectionSlot` on iOS. | **Remove or constrain from schema/composition** | Treat these as structural wrappers unless a concrete payload need proves otherwise. `SectionSlot` embeds full sections whose surfaces are owned by `SectionContainer`, so applying generic atomic chrome risks dual surface ownership. `Spacer` should remain a dimension-only layout primitive. `Conditional` should be transparent control flow unless the schema/composer explicitly allows wrapper chrome later. |
| Text actions are not wired on iOS. | **Must log and track** | `actions` is schema-level on `AtomicElement`, but no active server payload inspected requires text actions. Until wired, iOS should diagnose `Text.actions` rather than silently dropping them; if composers start emitting tappable text, promote to must-implement. |
| Button `icon` is ignored on Android. | **Must log and track** | `icon` is part of the atomic button contract and iOS renders it through `IconTokenResolver`, but active inspected server payloads do not emit atomic button icons. Android should warn/track ignored icons until the renderer maps tokens to Material icons. |
| `Image.placeholder` behavior is not aligned with the chosen mobile policy. | **Must implement now** | Server helpers actively emit `placeholder` on images. iOS ignores the placeholder and renders a neutral rectangle on failure, while Android uses payload placeholder but falls back to a hardcoded branded URL when absent. Mobile should use payload placeholders for first failure, avoid client-owned branded fallbacks, stop retrying after fallback failure, and render a neutral placeholder/empty frame when no payload placeholder exists. |
| Android `ScrollContainer` has hardcoded inner padding and may conflate `showIndicators` with scroll enablement. | **Must implement now** | `showIndicators: false` is active in carousel payloads. Android currently applies hardcoded horizontal padding and sets `userScrollEnabled = element.showIndicators != false`, so clean-carousel indicator policy can disable scrolling. Padding should be server-authored through atomic padding/gap, and indicators must not control scroll enablement. |
| `failureFeedback.style` is ignored or simplified. | **Must implement now** | The schema defines `snackbar`, `toast`, and `inline`, and demo form submission actively emits `failureFeedback.style: snackbar`. iOS currently presents all halt feedback as an error toast; Android carries the style through adaptation but host behavior still needs verification. Implement nearest platform-native mapping or a logged unsupported-style fallback. |
| Shadow color/offset fields are approximated on Android via elevation/radius only. | **Intentional platform approximation** | Android's Material elevation model is an acceptable platform-native approximation for shadow semantics in this POC. Document the approximation in renderer/test expectations; do not chase exact SwiftUI/CSS color and offset parity unless design later requires it. |
| Unknown badge alignment and accessibility roles can fall back silently. | **Must log and track** | Badge alignment is a strict schema enum, but Android still has a defensive default. Accessibility roles such as list/table/row/cell may not have exact native mappings and are currently ignored. Keep renderable fallbacks, but warn/track unsupported decoded roles or defensive badge defaults so QA can distinguish intentional approximation from dropped contract. |
| Variant override diagnostics may be too quiet for QA because some style drops log at debug level. | **Must log and track** | The style-token diagnostics registry currently marks `variant_override_blocked` and `variant_resolver_missing` as debug-level. That is acceptable for production noise, but QA/dev builds need visible diagnostics for dropped or unrecognized variant semantics. |
| iOS comments mention atomic borders even though `AtomicElement` has no `border` field in the schema. | **Remove or constrain from schema/composition** | Do not add an atomic border implementation just because comments mention it. Remove stale comments or constrain contract prose so `border` remains section/variant-owned unless `AtomicElement.border` is deliberately added to the schema and codegen. |

### Handoff Notes

- Web cleanup remains out of scope for this plan; web files may be used only as behavioral references.
- Native `GamePanel` renderer work remains out of scope. Game cards are server-composed `AtomicComposite` payloads, so fixes should land in generic atomic schema/composition/rendering paths.
- This POC does not need backward-compatibility shims for current-branch payloads. Prefer removing or constraining speculative contract surface over layering long-lived fallbacks around unneeded fields.
- Every **Must log and track** item should gain a visible dev/test diagnostic before later implementation, not a silent renderer fallback.

### Verification

- Update this plan with a classification per finding before implementation.
- Add a short comment or test expectation for every behavior intentionally left as an approximation.

## Phase 1: Mobile Atomic Layout Parity

### What To Implement

1. Implement or explicitly constrain `flex` on mobile container children.
2. Implement `crossAlignment: stretch` so it stretches children instead of mapping to center/leading behavior.
3. Implement or explicitly reject `breakpoint` on iOS containers.
4. Align `ScrollContainer` behavior:
   - `paging`
   - `snapAlignment`
   - `alignment`
   - `crossAlignment`
   - `showIndicators` without changing whether scrolling is enabled.
5. Remove hardcoded Android scroll content padding or make it server-authored through the atomic element model.

### Files To Inspect/Edit

- `ios/Sources/SduiCore/Rendering/Atomic/AtomicContainerView.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicScrollContainerView.swift`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicContainer.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicScrollContainer.kt`
- `web/src/components/atomic/AtomicContainer.tsx` only as behavioral reference, not implementation scope.

### Anti-Pattern Guards

- Do not add section-specific layout fixes for generic atomic layout bugs.
- Do not keep hardcoded rail/card spacing in client renderers when the server can emit padding/gap.
- Do not silently treat unsupported layout fields as center/default behavior.

### Verification

- Add mobile fixtures or unit/snapshot tests for:
  - `spaceBetween`
  - `spaceAround`
  - `spaceEvenly`
  - `flex`
  - `crossAlignment: stretch`
  - scroll `paging`
  - scroll `snapAlignment`
- Grep mobile renderers for decoded layout fields that are never referenced.

## Phase 2: Atomic Visual Box Model Parity

### What To Implement

1. Align image variant handling:
   - `aspectRatio`
   - `fit`
   - `cornerRadius`
   - `clip`
   - `fillWidth`
2. Prevent image variants from being routed through container variant semantics.
3. Implement or explicitly reject atomic `background` image support.
4. Apply payload-provided image fallback policy from the mobile core audit plan:
   - use `placeholder` for any image load error,
   - no hardcoded branded fallback URLs,
   - stop retrying if fallback also fails,
   - show neutral placeholder/empty frame when no payload fallback exists.
5. Decide whether iOS wrapper atomics should apply their own box model:
   - `Conditional`
   - `Spacer`
   - `SectionSlot`

### Files To Inspect/Edit

- `ios/Sources/SduiCore/Rendering/Atomic/AtomicBoxModifier.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicImageView.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicConditionalView.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicSpacerView.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicSectionSlotView.swift`
- `ios/Sources/SduiCore/Rendering/ImageVariantResolver.swift`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicBox.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicImage.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/ImageVariantResolver.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/adapters/BackgroundViewModel.kt`

### Anti-Pattern Guards

- Do not use branded client fallback images.
- Do not call container variant resolution for image-only variants.
- Do not silently ignore image background payloads if the schema continues to allow them.
- Do not add a second per-renderer box model path.

### Verification

- Add fixtures/tests for:
  - `Image` `variant: thumbnail`
  - image `placeholder` success after primary failure
  - fallback image failure
  - atomic background image
  - gradient background
  - asymmetric `cornerRadii`
  - wrapper element padding/margin/background, if wrapper chrome is supported.

## Phase 3: Atomic Interaction And Action Field Parity

### What To Implement

1. Wire text actions on iOS if actions are valid on all `AtomicElement` types.
2. Verify multi-action behavior on mobile primitives:
   - images
   - buttons
   - containers
   - text
3. Add Android button icon rendering.
4. Align unsupported trigger behavior:
   - `onTap`
   - `onLongPress`
   - `onVisible`
   - any unsupported trigger should log or be rejected, not silently disappear.
5. Map `failureFeedback.style` or explicitly document the mobile fallback behavior.

### Files To Inspect/Edit

- `ios/Sources/SduiCore/Rendering/Atomic/AtomicTextView.swift`
- `ios/Sources/SduiCore/Rendering/Atomic/AtomicButtonView.swift`
- `ios/Sources/SduiCore/Rendering/RenderingHelpers.swift`
- `ios/Sources/SduiCore/Runtime/ActionDispatcher.swift`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicButton.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicImage.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt`

### Anti-Pattern Guards

- Do not special-case analytics in primitive renderers.
- Do not silently execute only the first action if the contract says ordered action sequences are supported.
- Do not drop failure presentation hints without a logged limitation.

### Verification

- Add tests for:
  - text `onTap`
  - button icon
  - multiple action sequence order
  - unsupported trigger logging
  - `failureFeedback.style`

## Phase 4: Navigation Presentation Parity

### What To Implement

1. Apply the navigation policy from [Mobile Core Library Audit Findings](plan-mobile-core-library-audit-findings.md):
   - no silent dropping of `presentation` or `modalHeight`,
   - default/missing presentation keeps internal navigation,
   - `external` opens externally when possible,
   - `modal` uses sheet/modal host when available,
   - unsupported presentation logs and falls back only when safe.
2. Extend iOS beyond `external` vs push if modal/fullscreen/replace are in active payloads.
3. Extend Android adapter/handler/result types to preserve the same metadata.

### Files To Inspect/Edit

- `ios/Sources/SduiCore/Runtime/ActionDispatcher.swift`
- `ios/Sources/SduiCore/Runtime/NavCoordinator.swift`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/adapters/ActionAdapter.kt`
- `android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt`

### Verification

- Add tests for:
  - `presentation: external`
  - `presentation: modal`
  - `presentation: fullscreen`
  - `presentation: replace`
  - `modalHeight`
  - unsupported presentation fallback/error behavior.

## Phase 5: Diagnostics And Contract Guardrails

### What To Implement

1. Add a consistent mobile diagnostic strategy for unsupported-but-decoded fields.
2. Promote important style drops from debug-only logs to warning-level logs in dev/test builds.
3. Add a contract test matrix that verifies every schema style field is either:
   - implemented,
   - intentionally approximated,
   - explicitly rejected/logged,
   - not emitted by server composition for that element type.
4. Fix stale comments that claim unsupported fields exist, especially atomic border comments on iOS.

### Files To Inspect/Edit

- `schema/sdui-schema.json`
- `schema/style-tokens.json`
- `ios/Sources/SduiCore/Rendering/**`
- `android/sdui-core/src/main/java/com/nba/sdui/core/renderer/**`
- `ios/Tests/SduiCoreTests/**`
- Android test sources, if present.

### Verification

- Add grep checks for high-risk fields:
  - `flex`
  - `breakpoint`
  - `snapAlignment`
  - `paging`
  - `placeholder`
  - `modalHeight`
  - `failureFeedback.style`
  - `icon`
  - `cornerRadii`
  - `BackgroundViewModel.Image`
- Run mobile test targets through the repo `Makefile` where available.

## Suggested Execution Order

1. Phase 0: classify findings.
2. Phase 1: layout parity.
3. Phase 2: visual box model parity.
4. Phase 3: interaction/action field parity.
5. Phase 4: navigation presentation parity.
6. Phase 5: diagnostics and guardrails.

## Definition Of Done

- Every audited schema style/presentation field has an implementation, logged limitation, documented approximation, or server-side constraint.
- Android and iOS no longer silently drop active payload style fields.
- Server-composed `AtomicComposite` layouts render consistently across mobile clients.
- New contract tests cover the high-risk fields identified by this audit.
