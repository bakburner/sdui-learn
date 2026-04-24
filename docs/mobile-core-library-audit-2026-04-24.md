# Mobile Core Library Audit — 2026-04-24

## Scope

This note summarizes a read-only review of the Android core library in [android/sdui-core](android/sdui-core) and the iOS core library in [ios/Sources/SduiCore](ios/Sources/SduiCore).

Review goal: check whether both libraries still behave like platform-specific renderers for a neutral, server-owned SDUI vocabulary, rather than reintroducing client-owned screen logic, styling rules, or transport semantics.

## High-Level Insights

- The core architecture is mostly intact. Both platforms still use generic screen fetch paths, simple URI resolution, shared section routers, and opaque real-time message binding.
- Android has more contract drift than iOS. The biggest issues are missing interaction semantics, client-invented fallbacks, and places where server-supplied visuals are ignored.
- iOS is cleaner overall, but it still has some stale compatibility paths that work against the newer `section.surface` and request-envelope model.
- The main risk is not basic correctness. The main risk is architectural erosion: small client-side fallbacks and shortcuts make the server vocabulary less authoritative over time.

## Findings

| Priority | Simplified finding | Why it matters | Key file references |
|---|---|---|---|
| High | Android does not appear to fire server-declared `onVisible` actions. | The server can declare visibility-triggered analytics semantics, but Android currently seems to use visibility only for poll/SSE gating, not for action dispatch. That breaks cross-platform action semantics. | [SduiScreenContent.kt#L156](android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenContent.kt#L156), [SectionVisibilityTracker.kt#L50](android/sdui-core/src/main/java/com/nba/sdui/core/state/SectionVisibilityTracker.kt#L50), [ActionHandler.kt#L37](android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt#L37), [ScreenShell.swift#L152](ios/Sources/SduiCore/Rendering/ScreenShell.swift#L152), [ScreenShell.swift#L163](ios/Sources/SduiCore/Rendering/ScreenShell.swift#L163) |
| High | Android decodes navigation presentation metadata but drops it before execution. | The wire contract supports things like external navigation and modal presentation, but Android currently collapses navigation down to `targetUri` or `webUrl`. That weakens the neutral action vocabulary. | [SduiModels.kt#L196](android/sdui-core/src/main/java/com/nba/sdui/core/models/generated/SduiModels.kt#L196), [ActionAdapter.kt#L13](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/adapters/ActionAdapter.kt#L13), [ActionHandler.kt#L152](android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt#L152), [ActionDispatcher.swift#L141](ios/Sources/SduiCore/Runtime/ActionDispatcher.swift#L141) |
| High | Android ignores server-provided GamePanel image backgrounds and substitutes a hardcoded navy fill. | This is a direct server-authority break: the server provides a background image, but Android renders a fixed brand-colored fallback instead. iOS does render the image-backed path. | [GamePanelRenderer.kt#L79](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt#L79), [RenderingHelpers.swift#L38](ios/Sources/SduiCore/Rendering/RenderingHelpers.swift#L38) |
| Medium | Android still invents image assets client-side using hardcoded fallback URLs. | The SDUI contract says images should come from the server. Hardcoded CDN fallbacks inside the core library reintroduce asset ownership on the client. | [AtomicImage.kt#L21](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicImage.kt#L21), [AtomicImage.kt#L40](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/atomic/AtomicImage.kt#L40), [SduiDefaults.kt#L12](android/sdui-core/src/main/java/com/nba/sdui/core/config/SduiDefaults.kt#L12), [SduiImageDefaults.kt#L13](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SduiImageDefaults.kt#L13) |
| Medium | Android invents fallback content values like `AWY` and `HME` for team tricodes. | Even small content fallbacks move semantic ownership away from the server. In a server-driven system, missing data should usually stay missing or come from a server-composed fallback section. | [SectionUiAdapters.kt#L116](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/adapters/SectionUiAdapters.kt#L116), [SectionUiAdapters.kt#L117](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/adapters/SectionUiAdapters.kt#L117) |
| Medium | GamePanel visuals are still partly client-owned on both platforms. | Featured styling, badge colors, text colors, and fallback surfaces still come from client logic in places where the server should ideally own appearance through `displayConfig` and section surface data. | [GamePanelRenderer.kt#L68](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt#L68), [GamePanelRenderer.kt#L89](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt#L89), [GamePanelRenderer.kt#L132](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt#L132), [GamePanelView.swift#L25](ios/Sources/SduiCore/Rendering/Sections/GamePanelView.swift#L25), [GamePanelView.swift#L48](ios/Sources/SduiCore/Rendering/Sections/GamePanelView.swift#L48) |
| Medium | iOS still applies legacy section padding/background outside the shared section surface wrapper. | This creates a second outer-chrome path alongside `SectionContainer`, which makes it harder to guarantee that section wrappers are fully server-owned and consistent across platforms. | [ScreenShell.swift#L133](ios/Sources/SduiCore/Rendering/ScreenShell.swift#L133), [ScreenShell.swift#L134](ios/Sources/SduiCore/Rendering/ScreenShell.swift#L134), [SectionContainer.swift#L30](ios/Sources/SduiCore/Rendering/SectionContainer.swift#L30), [SectionContainer.kt#L20](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SectionContainer.kt#L20) |
| Medium | SSE staleness timing is not aligned across platforms. | Android waits before marking SSE sections stale, while iOS marks them stale immediately on disconnect/failure. If that difference is not intentional, users can get different freshness behavior for the same screen. | [SduiScreenViewModel.kt#L53](android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt#L53), [SduiScreenViewModel.kt#L430](android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt#L430), [SduiScreenViewModel.swift#L426](ios/Sources/SduiCore/Runtime/SduiScreenViewModel.swift#L426) |
| Low | iOS still exposes a stale `variant -> gameState` repository API. | This does not appear to be a live bug today, but it is misleading API surface in a library that is supposed to use neutral request-envelope concepts. | [SduiRepository.swift#L33](ios/Sources/SduiCore/Network/SduiRepository.swift#L33), [SduiRepository.swift#L37](ios/Sources/SduiCore/Network/SduiRepository.swift#L37) |
| Low | Android still has at least one renderer reading legacy section envelope styling directly. | This suggests the migration to shared `section.surface` ownership is incomplete. | [SeasonLeadersTableRenderer.kt#L70](android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/SeasonLeadersTableRenderer.kt#L70) |

## Recommendations

### 1. Fix the contract breaks first

1. Add Android support for server-declared `onVisible` action dispatch, using the existing visibility tracker and the same impression-dedup model already present on iOS.
2. Carry Android `presentation` and `modalHeight` through `ActionAdapter` and `ActionHandler`, then map them to platform-native navigation behavior.
3. Make Android GamePanel honor server-provided image backgrounds instead of substituting a hardcoded fill.

### 2. Remove client-owned semantic fallbacks

1. Remove hardcoded Android image fallback URLs from the shipped core path, or constrain them to clearly non-semantic placeholders that do not pretend to be server content.
2. Remove client-invented GamePanel tricodes like `AWY` and `HME`.
3. Reduce GamePanel-specific visual defaults so more of the rendering comes directly from `displayConfig` and section surface data.

### 3. Finish the surface-model cleanup

1. Move iOS away from legacy `section.padding` and `section.backgroundColor` wrappers in `ScreenShell` if `section.surface` is now the intended outer-chrome contract.
2. Audit Android permanent-section renderers for remaining direct reads of legacy envelope styling.
3. Decide whether legacy envelope styling fields are still supported compatibility paths or should now be treated as deprecated.

### 4. Tighten parity expectations between platforms

1. Decide whether SSE staleness should be immediate or delayed, then make both platforms match.
2. Add contract tests around action semantics, especially `onVisible`, external navigation, modal presentation, and failure policy behavior.
3. Add parity checks for section wrapper ownership so the same server payload does not get two different outer-chrome interpretations.

## Suggested Next Steps

1. Android action semantics pass: `onVisible`, navigation presentation, and external URL handling.
2. Android visual-authority pass: GamePanel background images and image fallback behavior.
3. Shared surface cleanup pass: remove legacy outer-chrome paths and add tests around `section.surface` ownership.

## Notes

- This was a read-only audit. No source files were modified.
- The findings focus on architecture intent and contract consistency, not just runtime correctness.