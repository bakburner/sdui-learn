# Plan: Layout & Responsive

> Source requirements: §9m, §9b from sdui-requirements-summary.md, ADR-008 (Accepted, Option C)

## Summary

Complete the hybrid layout manager (server hints + client layout engine) across all platforms. `SectionLayoutHints` schema and Web implementation are done. Android renderer wiring is the remaining gap. Client-side responsive breakpoints within a platform family (e.g., phone vs tablet) are deferred pending further design.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `SectionLayoutHints` with margins, dividers, priority, minWidth |
| Server support | Built | Composers populate layout hints |
| Android support | Gap | Schema parsed but renderer does not apply hints |
| Web support | Built | `SectionRouter` applies margins, dividers, priority from hints |
| Documentation | Built | ADR-008 documents Option C decision |
| Tests | Gap | No layout hint application tests |

## Requirements Addressed

- [x] **REQ-1**: Schema `SectionLayoutHints` (margins, dividers, priority, minWidth) — §9m, ADR-008
- [x] **REQ-2**: Web SectionRouter applies hints — §9m
- [ ] **REQ-3**: Android SectionRouter applies hints — §9m
- [ ] **REQ-4**: Client-side responsive breakpoints for within-family adaptation — §9b

## Tasks

### Phase 1: Schema & Codegen
- [x] `SectionLayoutHints` in schema — no changes needed
- [ ] Optional: Add `breakpoint` responsive hint if within-family adaptation is designed

### Phase 2: Server
- [x] Layout hints populated — no changes needed

### Phase 3: Android
- [ ] Update `SduiScreenContent` to read `section.layoutHints` — `android/sdui-core/src/.../screen/`
  - [ ] Apply `topMargin`, `bottomMargin` as Compose `Modifier.padding()`
  - [ ] Render `showDividerAbove` / `showDividerBelow` as `HorizontalDivider()`
  - [ ] Respect `priority` for section ordering (sort sections by priority if present)
  - [ ] Apply `minWidth` constraint where applicable

### Phase 4: Web
- [x] Layout hints applied — no changes needed
- [ ] Add responsive breakpoint support if within-family adaptation is designed

### Phase 5: Documentation & Tests
- [ ] Update `docs/sdui-requirements-summary.md` status: §9m Partial → Built
- [ ] Add layout hint application tests on Android (verify margins, dividers render correctly)

## Dependencies

- ADR-008 is accepted (Option C) — no blockers for Android wiring
- Within-family responsive breakpoints (§9b) need a design decision before implementation

## Open Questions

- [ ] Should `priority` override server-provided section order, or is it only for tie-breaking?
- [ ] How should `minWidth` interact with Compose's `fillMaxWidth()` default?
- [ ] Is within-family responsive adaptation (phone vs tablet) a Phase 2 item or deferred to post-launch?
