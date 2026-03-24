# Plan: Accessibility

> Source requirements: §9a from sdui-requirements-summary.md

## Summary

Add server-driven accessibility metadata to every renderable element so screen readers, VoiceOver, and TalkBack can describe SDUI screens without client-side hardcoding. The schema already defines `AccessibilityProperties` (label, role, hidden, headingLevel, liveRegion, sortOrder, hint) on Section, Subsection, and AtomicElement. Codegen outputs include the types. Renderers on both platforms now consume these properties. Remaining work is live-region behavior for real-time score updates and automated testing.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `AccessibilityProperties` on Section, Subsection, AtomicElement |
| Server support | Built | Composers populate accessibility fields |
| Android support | Built | `AccessibilityExt.kt` applies `semantics {}` across all renderers |
| Web support | Built | `accessibility.ts` applies ARIA attributes across all renderers |
| Documentation | Built | Client Implementor's Contract §14 covers accessibility mapping |
| Tests | Gap | No automated screen-reader tests or accessibility audit |

## Requirements Addressed

- [x] **REQ-1**: Schema fields for accessibility descriptors (label, role, hidden, headingLevel, liveRegion, sortOrder, hint) — §9a
- [x] **REQ-2**: Android Compose `semantics {}` integration — §9a
- [x] **REQ-3**: Web ARIA attribute mapping — §9a
- [ ] **REQ-4**: Live-region behavior for real-time score updates — §9a
- [ ] **REQ-5**: Automated accessibility testing (axe-core, Espresso accessibility checks) — §9a

## Tasks

### Phase 1: Schema & Codegen
- [x] Add `AccessibilityProperties` to schema — `schema/sdui-schema.json`
- [x] Run codegen: `cd codegen && ./generate.sh`
- [x] Verify generated outputs (Java, TypeScript, Swift)

### Phase 2: Server
- [x] Populate `accessibility` properties in composers
- [ ] Add `liveRegion: "polite"` to GamePanel score elements for real-time updates
- [ ] Add `liveRegion: "assertive"` to ErrorState sections

### Phase 3: Android
- [x] Create `AccessibilityExt.kt` shared utility
- [x] Apply to all 8 section renderers + 9 atomic renderers
- [ ] Wire `liveRegion` to Compose `LiveRegionMode.Polite` / `Assertive`
- [ ] Add Espresso accessibility checks to UI tests

### Phase 4: Web
- [x] Create `accessibility.ts` shared utility
- [x] Apply ARIA attributes to all section + atomic components
- [ ] Wire `liveRegion` to `aria-live="polite"` / `"assertive"`
- [ ] Add axe-core integration tests

### Phase 5: Documentation & Tests
- [x] Document accessibility contract in Client Implementor's Contract §14
- [ ] Add accessibility testing guidance to contract doc
- [ ] Create golden accessibility snapshots for screen reader testing

## Dependencies

- None — schema and renderer work is complete. Remaining tasks are incremental.

## Open Questions

- [ ] Should `liveRegion` be applied at the section level (GamePanel) or at the specific atomic element containing the score text?
- [ ] What is the target WCAG compliance level (AA or AAA)?
