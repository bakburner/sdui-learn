# Web Core Library Audit — 2026-04-24

## Scope

This note summarizes a read-only review of the web client runtime in [web/src](web/src).

Review goal: check whether the web client still behaves like a platform-specific renderer for a neutral, server-owned SDUI vocabulary, rather than reintroducing client-owned routing, styling rules, assets, or transport semantics.

## High-Level Insights

- The core web architecture is mostly intact. The client still uses a generic screen fetch path, simple URI resolution, shared section routing, server-driven refresh policies, and opaque real-time binding.
- Web is ahead of Android in one important area: it already supports server-declared `onVisible` impression actions with dedup and dwell timing.
- The main drift is concentrated in prototype seams and fallback behavior, not in the core live-data path. The biggest issues are simplified action execution, incomplete section-surface support, and hardcoded image/bootstrap defaults.
- The architectural risk is gradual erosion rather than outright breakage. Small client-owned fallbacks and shortcuts make the server vocabulary less authoritative over time.

## Findings

| Priority | Simplified finding | Why it matters | Key file references |
|---|---|---|---|
| High | Web action execution still collapses the server's navigation and analytics vocabulary into prototype behavior. | The schema exposes richer action semantics such as `presentation`, `modalHeight`, and `webUrl`, but the runtime reduces navigation to `targetUri || fallbackUrl`, shows a toast instead of performing the declared navigation, and logs fire-and-forget actions instead of dispatching them to real integrations. That weakens the neutral action contract. | [web/src/generated/SduiModels.ts#L96](web/src/generated/SduiModels.ts#L96), [web/src/generated/SduiModels.ts#L115](web/src/generated/SduiModels.ts#L115), [web/src/runtime/ActionHandler.ts#L107](web/src/runtime/ActionHandler.ts#L107), [web/src/runtime/ActionHandler.ts#L108](web/src/runtime/ActionHandler.ts#L108), [web/src/runtime/ActionHandler.ts#L121](web/src/runtime/ActionHandler.ts#L121), [web/src/runtime/ActionHandler.ts#L210](web/src/runtime/ActionHandler.ts#L210), [web/src/runtime/ActionHandler.ts#L222](web/src/runtime/ActionHandler.ts#L222) |
| High | The shared `SectionContainer` does not fully honor the `section.surface.background` contract. | The generated contract says section surfaces may use solid, gradient, or image backgrounds. The shared wrapper only resolves solid and gradient forms, then returns `undefined` for the rest, even though the web background utility already supports image backgrounds. This creates a direct server-authority gap in the common wrapper. | [web/src/generated/SduiModels.ts#L1544](web/src/generated/SduiModels.ts#L1544), [web/src/components/SectionContainer.tsx#L30](web/src/components/SectionContainer.tsx#L30), [web/src/components/SectionContainer.tsx#L69](web/src/components/SectionContainer.tsx#L69), [web/src/components/SectionContainer.tsx#L85](web/src/components/SectionContainer.tsx#L85), [web/src/utils/background.ts#L43](web/src/utils/background.ts#L43), [web/src/utils/background.ts#L65](web/src/utils/background.ts#L65) |
| Medium | Several permanent section renderers still read legacy section envelope styling directly. | `SectionRouter` now wraps sections in the shared `SectionContainer`, but multiple renderers still apply `section.backgroundColor` on their own containers. That leaves two competing outer-surface paths in the web client and makes it harder to guarantee that section chrome is fully server-owned through `section.surface`. | [web/src/components/SectionRouter.tsx#L53](web/src/components/SectionRouter.tsx#L53), [web/src/components/sections/TabGroup.tsx#L21](web/src/components/sections/TabGroup.tsx#L21), [web/src/components/sections/Form.tsx#L27](web/src/components/sections/Form.tsx#L27), [web/src/components/sections/BoxscoreTable.tsx#L28](web/src/components/sections/BoxscoreTable.tsx#L28), [web/src/components/sections/BoxscoreTable.tsx#L68](web/src/components/sections/BoxscoreTable.tsx#L68), [web/src/components/sections/SeasonLeadersTable.tsx#L53](web/src/components/sections/SeasonLeadersTable.tsx#L53) |
| Medium | The web client still invents fallback images and some GamePanel presentation values client-side. | The runtime ships a hardcoded CDN fallback image, reuses it across atomic and section renderers, and still supplies fallback GamePanel background and status text when the server payload is incomplete. In a server-driven system, those assets and content decisions should stay server-owned wherever possible. | [web/src/utils/constants.ts#L9](web/src/utils/constants.ts#L9), [web/src/components/atomic/AtomicImage.tsx#L7](web/src/components/atomic/AtomicImage.tsx#L7), [web/src/components/atomic/AtomicImage.tsx#L56](web/src/components/atomic/AtomicImage.tsx#L56), [web/src/components/atomic/AtomicImage.tsx#L60](web/src/components/atomic/AtomicImage.tsx#L60), [web/src/components/sections/GamePanel.tsx#L50](web/src/components/sections/GamePanel.tsx#L50), [web/src/components/sections/GamePanel.tsx#L69](web/src/components/sections/GamePanel.tsx#L69), [web/src/components/sections/GamePanel.tsx#L86](web/src/components/sections/GamePanel.tsx#L86), [web/src/adapters/sectionUiAdapters.ts#L151](web/src/adapters/sectionUiAdapters.ts#L151), [web/src/adapters/sectionUiAdapters.ts#L152](web/src/adapters/sectionUiAdapters.ts#L152) |
| Low | The app shell still contains prototype-owned bootstrap and transport assumptions. | The initial screen URI is still hardcoded instead of coming from an init flow, the variant selector falls back to a client-owned experiment ID, and the Ably client hardcodes its token endpoint. These are understandable prototype seams, but they keep routing and transport configuration in the client. | [web/src/App.tsx#L8](web/src/App.tsx#L8), [web/src/App.tsx#L10](web/src/App.tsx#L10), [web/src/App.tsx#L48](web/src/App.tsx#L48), [web/src/runtime/AblyClient.ts#L3](web/src/runtime/AblyClient.ts#L3), [web/src/runtime/AblyClient.ts#L10](web/src/runtime/AblyClient.ts#L10) |

## Recommendations

### 1. Fix the contract breaks first

1. Make web navigation honor the full action contract, including `presentation`, `webUrl`, and modal semantics, rather than reducing everything to a toast-based placeholder.
2. Replace the prototype-only fire-and-forget logging path with a pluggable analytics dispatcher so the runtime preserves server-declared destinations and event semantics.
3. Update `SectionContainer` so `section.surface.background` supports image-backed surfaces, not just solid and gradient variants.

### 2. Finish the surface-model cleanup

1. Remove direct reads of legacy `section.backgroundColor` from permanent section renderers once `section.surface` is the intended outer-chrome path.
2. Decide whether legacy section styling fields are still compatibility paths or should now be treated as deprecated.
3. Add focused tests around `SectionContainer` so one server payload cannot render different outer chrome depending on which section type consumes it.

### 3. Reduce client-owned fallbacks

1. Remove hardcoded fallback image URLs from the core shipped path, or constrain them to clearly non-semantic placeholders that do not pretend to be server content.
2. Reduce GamePanel-specific fallback status and background behavior so more rendering comes directly from server data and `displayConfig`.
3. Audit other section and atomic renderers for similar fallback ownership drift before it spreads further.

### 4. Close the prototype seams deliberately

1. Move bootstrap URI resolution out of `App.tsx` and into a server-driven init flow when the prototype shell is hardened.
2. Externalize transport endpoints such as the Ably token URL so they are configuration, not baked-in runtime assumptions.
3. Decide whether the variant toolbar is still prototype scaffolding or part of the supported SDUI shell contract, then align the implementation with that decision.

## Suggested Next Steps

1. Web action semantics pass: navigation presentation, external URL handling, and analytics destination dispatch.
2. Shared surface pass: image backgrounds in `SectionContainer` and removal of legacy renderer-owned envelope chrome.
3. Fallback cleanup pass: image fallbacks, GamePanel defaults, bootstrap URI, and transport endpoint assumptions.

## Notes

- This was a read-only audit. No source files were modified.
- The findings focus on architecture intent and contract consistency, not just runtime correctness.
- Positive parity note: the web client already implements `onVisible` impression tracking with dedup and dwell semantics through [web/src/hooks/useImpressionTracking.ts#L21](web/src/hooks/useImpressionTracking.ts#L21), [web/src/hooks/useImpressionTracking.ts#L52](web/src/hooks/useImpressionTracking.ts#L52), and [web/src/hooks/useImpressionTracking.ts#L74](web/src/hooks/useImpressionTracking.ts#L74).