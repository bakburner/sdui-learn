# Plan: TabGroup Atomic Header Refactor

> Source inputs: `AGENTS.md`, `schema/sdui-schema.json`, current web/iOS/Android `TabGroup` renderers, and the `data.ui` pattern now used by semantic sections such as `SubscribeHero`.

## Summary

`TabGroup` predates the stronger atomic-composite direction. It remains justified as a semantic section because it owns client-local selected-tab state and hosts different nested section arrays. Its visual tab header, however, should not keep growing bespoke renderer styling if the same UI can be expressed as an atomic tree.

This plan keeps `TabGroup` as the stateful/nested-section host, but moves optional tab-header visuals toward the existing `data.ui` convention. Do not add `tabsUi`, `tabHeaderStyle`, or other tab-specific visual schema fields.

**Status: Blocked.** Phases 2–3 require ADR-014 (dynamic conditional properties) to be accepted and implemented so that atomic tab buttons can visually reflect selected state. Phase 1 (schema/contract work) can proceed independently. Until ADR-014 lands, native renderer tab headers remain the correct approach.

## Prerequisites

| Prerequisite | Status | Notes |
|---|---|---|
| ADR-014 accepted with Option 2 or 4 | **Pending** | Without `enabledIf`/`visibleIf` or state-bound `disabled`/`selected`, an atomic header cannot visually distinguish active from inactive tabs. |
| `Conditional` wrapper evaluated as interim | **Available but impractical** | Schema already has `Conditional` with `trueChild`/`falseChild`, but N tabs × 2 subtrees is prohibitively verbose. |
| `mutate` action type verified for tab selection | **Ready** | Schema defines `ActionType: "mutate"` with `target`/`operation: "set"`/`value`. Android already fires this via `mapTabMutateAction()`. No new action type needed. |

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

- [ ] Confirm whether generated models already expose `data.ui` for `TabGroup`.
- [ ] If not, update schema/codegen so `TabGroup` can optionally carry `ui` using the existing `AtomicElement` shape.
- [ ] Document that `data.ui`, when present on `TabGroup`, represents the tab header/control surface only.
- [ ] Keep `tabs`, `stateKey`, `defaultTab`, and `tabContents` as the authoritative tab behavior contract.

Anti-pattern guards:
- Do not add `tabsUi`, `tabHeaderUi`, `tabStyle`, or `selectedTabVariant`.
- Do not make atomics host arrays of sections.
- Do not remove `TabGroup` while it still owns local selection state.

### Phase 2: Renderer Fallback Path

**Blocked on:** ADR-014 implementation (dynamic conditional properties on all platforms).

- [ ] Update web/iOS/Android `TabGroup` renderers to:
  - Render `data.ui` as the header when present.
  - Preserve the existing native tab header as the fallback when `data.ui` is absent.
  - Continue rendering `data.tabContents[selectedTab]` through the normal `SectionRouter`.
- [ ] Ensure the renderer continues observing `screenState[stateKey]` for body switching regardless of whether the header is atomic or native. The atomic header fires `mutate` actions that update screen state; the renderer must react to that state change to swap `tabContents`.
- [ ] Ensure the atomic header can trigger the same state update as native tabs.
  - Mechanism: server emits tab buttons with `onActivate → mutate(target=stateKey, operation=set, value=tabId)`. This works today.
  - Selected-state visual feedback requires ADR-014's conditional properties so atomic buttons reflect which tab is active.

Anti-pattern guards:
- Do not parse tab labels out of the atomic tree.
- Do not infer tab state from child order.
- Do not add client screen-specific branching for Watch/League Pass tabs.
- Do not add client logic that applies selected styling to atomic buttons based on state — that violates renderer-is-presentation-only.

### Phase 3: Server Composition

**Blocked on:** Phase 2 completion (renderer support for `data.ui` + ADR-014 conditional properties).

- [ ] Add a server helper that builds a `TabGroup` atomic header using existing atomics:
  - `ScrollContainer` or `Container` row
  - repeated `Button` or `Container` + `Text`
  - selected/unselected presentation driven by ADR-014 conditional properties bound to `stateKey`
  - each button carries `onActivate → mutate(target=stateKey, operation=set, value=tabId)`
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

## Resolved Questions

| Question | Answer |
|---|---|
| Can existing atomic actions express `stateKey` mutation? | **Yes.** Schema has `ActionType: "mutate"` with `target`/`operation: "set"`/`value`. Android already uses this via `mapTabMutateAction()`. No new action type needed. |
| Should selected-tab visual state use conditional atomics or a selected-state primitive? | **Conditional atomics (ADR-014 Option 4 `enabledIf`/`visibleIf`, or Option 2 widened `disabled`).** A `selected` primitive would push "what selected looks like" into the client, violating server authority over presentation. Conditional properties keep the server in control of both states. |
| Is server-authored tab-header UI needed now? | **No.** Native tab headers work well on all platforms. This plan is future-ready for when ADR-014 lands, but is not an immediate implementation target. Phase 1 (schema contract) can proceed; Phases 2–3 wait. |

## Remaining Open Questions

- [ ] Which ADR-014 option will be accepted? Option 4 (`enabledIf`/`visibleIf`) covers the tab case cleanly; Option 2 (widened `disabled`) is too narrow for selected-state styling.
- [ ] Should the `Conditional` wrapper be considered a temporary interim for 2–3 tab groups, or is it too verbose to ship even short-term?

## Completion Criteria

- `TabGroup` keeps its semantic role for selected-tab state and section hosting.
- Optional tab-header visuals use `data.ui`, not new visual fields.
- Existing payloads without `data.ui` continue to render.
- No tab-specific primitive, section, or visual escape hatch is introduced.
