# Plan: TabGroup Atomic Header Refactor

> Source inputs: `AGENTS.md`, `schema/sdui-schema.json`, current web/iOS/Android `TabGroup` renderers, and the `data.ui` pattern now used by semantic sections such as `SubscribeHero`.

## Summary

`TabGroup` predates the stronger atomic-composite direction. It remains justified as a semantic section because it owns client-local selected-tab state and hosts different nested section arrays. Its visual tab header, however, should not keep growing bespoke renderer styling if the same UI can be expressed as an atomic tree.

This plan keeps `TabGroup` as the stateful/nested-section host, but moves optional tab-header visuals toward the existing `data.ui` convention. Do not add `tabsUi`, `tabHeaderStyle`, or other tab-specific visual schema fields.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Section type | Semantic | `TabGroup` is listed as a justified semantic section for tab selection state and nested section hosting. |
| Visual header | Renderer-owned | Web/iOS/Android each render their own tab bar directly. |
| Body content | Server-owned | `data.tabContents` contains arrays of sections. Those sections can already be `AtomicComposite` or semantic sections. |
| Atomic UI support | Partial | Semantic sections can use `data.ui`, but `TabGroup` renderers do not currently look for optional `data.ui`. |

## Decision

- Keep `TabGroup` as a semantic section for local selected-tab state and nested section hosting.
- Use the existing `data.ui` pattern if the tab header needs server-authored visuals.
- Do not invent new tab-specific visual fields.
- Do not move nested section arrays into atomics. `tabContents` remains the section-hosting contract.

## Tasks

### Phase 1: Contract Classification

- [ ] Confirm whether generated models already expose `data.ui` for `TabGroupData`.
- [ ] If not, update schema/codegen so `TabGroupData` can optionally carry `ui` using the existing `AtomicElement` shape.
- [ ] Document that `data.ui`, when present on `TabGroup`, represents the tab header/control surface only.
- [ ] Keep `tabs`, `stateKey`, `defaultTab`, and `tabContents` as the authoritative tab behavior contract.

Anti-pattern guards:
- Do not add `tabsUi`, `tabHeaderUi`, `tabStyle`, or `selectedTabVariant`.
- Do not make atomics host arrays of sections.
- Do not remove `TabGroup` while it still owns local selection state.

### Phase 2: Renderer Fallback Path

- [ ] Update web/iOS/Android `TabGroup` renderers to:
  - Render `data.ui` as the header when present.
  - Preserve the existing native tab header as the fallback when `data.ui` is absent.
  - Continue rendering `data.tabContents[selectedTab]` through the normal `SectionRouter`.
- [ ] Ensure the atomic header can trigger the same state update as native tabs.
  - Preferred: server emits tab buttons with existing action semantics that mutate `stateKey`.
  - If current atomic actions cannot express that cleanly, stop and add a separate plan for generic state-mutation actions rather than adding tab-specific behavior.

Anti-pattern guards:
- Do not parse tab labels out of the atomic tree.
- Do not infer tab state from child order.
- Do not add client screen-specific branching for Watch/League Pass tabs.

### Phase 3: Server Composition

- [ ] Add a server helper that builds a `TabGroup` atomic header using existing atomics:
  - `ScrollContainer` or `Container` row
  - repeated `Button` or `Container` + `Text`
  - selected/unselected presentation driven by server-declared state/action semantics
- [ ] Keep `tabContents` as ordinary nested section arrays.
- [ ] Add one demo fixture that includes `TabGroup.data.ui` and one fixture that omits it to verify fallback behavior.

Anti-pattern guards:
- Do not introduce a new `AtomicTabs` primitive.
- Do not add tab-only variants unless they are stable, reusable primitive variants and pass the normal schema/codegen/client-resolver process.

### Phase 4: Verification

- [ ] `make codegen` if schema changes.
- [ ] Web build and focused tab test.
- [ ] Android compile.
- [ ] iOS build or model round-trip test.
- [ ] Verify tab selection still updates `ScreenState`.
- [ ] Verify nested sections still render through existing routers.
- [ ] Verify fallback native tab header still works when `data.ui` is absent.

## Open Questions

- [ ] Can existing atomic actions already express `stateKey` mutation cleanly enough for tab selection?
- [ ] Should selected-tab visual state be represented by conditional atomics bound to screen state, or by a small generic selected-state primitive/property?
- [ ] Is server-authored tab-header UI actually needed now, or should we simply document that `TabGroup` is a justified older semantic section and leave renderer visuals alone?

## Completion Criteria

- `TabGroup` keeps its semantic role for selected-tab state and section hosting.
- Optional tab-header visuals use `data.ui`, not new visual fields.
- Existing payloads without `data.ui` continue to render.
- No tab-specific primitive, section, or visual escape hatch is introduced.
