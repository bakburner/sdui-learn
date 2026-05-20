# Plan: Action Trigger Coverage & Mutate Schema Alignment

> Source: code audit of action dispatch surfaces (web + Android) against
> `schema/sdui-schema.json` `Action`, `ActionTrigger`, and `MutateOperation`.
> Related rules: AGENTS.md §3.5 (action semantics are server-declared) and
> §1.3 (strict decoding for closed wire shapes).

## Summary

The schema declares 8 action triggers and a typed `mutate` action shape
(`target` / `operation` / `value`), but the runtime wiring on web and
Android only honors a subset:

- Atomic primitives execute `onActivate` / `onTap` only; `onLongPress`,
  `onSubmit`, `onSwipe`, `onFocus`, `onBlur` decode but never reach the
  executor.
- Web's mutate handler reads legacy `stateKey` / `stateValue` instead of
  the schema's `target` / `value`, so server-emitted mutate actions in
  the wire shape are no-ops on web.
- Web's navigate handler reads legacy `fallbackUrl` instead of the
  schema's `webUrl`.
- Android's mutate handler ignores `operation` (`set` / `toggle` /
  `increment` / `append`) and only does set/remove. iOS already
  implements all four; web/Android need parity.
- Android's adapter renames the wire `webURL` field to an internal
  `fallbackUrl` slot, hiding the schema-canonical name from the runtime.
- Atomic batch dispatch on Android relies on a `LocalActionExecutor`
  provider that the demo app supplies but library hosts may not, causing
  silent fallback to per-action dispatch and loss of cross-action
  failure-policy semantics.

This plan brings runtime behavior in line with the wire contract, with
narrow client exceptions only where platforms genuinely cannot honor a
trigger.

## Current State

| Aspect | Web | Android | iOS | Notes |
|---|---|---|---|---|
| `onActivate` / `onTap` dispatch | Built | Built | Built | `getActivateActions` / `SectionInteractions` |
| `onVisible` dispatch | Gap (atomic) | Gap (atomic) | Built (atomic) | iOS atomic primitives apply onVisible via `RenderingHelpers.swift`; web/Android only dispatch via section-level impression tracker
| `onLongPress` dispatch | N/A (not hosted) | Gap | Gap | Web library treats as no-op; native platforms gap |
| `onSubmit` dispatch | Partial | Partial | Partial | `Form` calls submitAction directly; no generic onSubmit handler on other elements |
| `onFocus` / `onBlur` dispatch | Gap | Gap | Gap | Only used as local UX (form draft commit on web blur); no payload-driven dispatch |
| `onSwipe` dispatch | Gap | Gap | Gap | ScrollContainer/carousels do not surface this |
| Mutate `target` (schema) | Gap | Built | Built | Web reads legacy `stateKey`; Android adapter maps `target` → `stateKey`; iOS uses `target` directly |
| Mutate `value` (schema) | Gap | Built | Built | Web reads legacy `stateValue` |
| Mutate `operation` (set/toggle/increment/append) | Gap | Gap | Built | Web/Android default to set/remove only; iOS implements all 4 in `ScreenState.apply` |
| Navigate `webUrl` (schema) | Gap | Gap (alias) | Built | Web reads legacy `fallbackUrl`; Android adapter renames `webURL` → `fallbackUrl` internally |
| Atomic batch executor on every host | Partial | Partial | Built | iOS dispatcher owned by VM; web/Android primitives can bypass batch |

## Requirements Addressed

- [ ] REQ-1: Atomic primitives dispatch all schema triggers that map to a
      platform-native gesture/event, in declared order, through the
      shared executor — AGENTS.md §3.5, §7.2.
- [ ] REQ-2: Mutate action consumes wire fields `target` / `operation` /
      `value` on every platform — AGENTS.md §1.2, §1.3.
- [ ] REQ-3: Mutate honors all `MutateOperation` enum values, with a
      documented neutral fallback for unknown values — AGENTS.md §8.1.
- [ ] REQ-4: Navigate consumes wire field `webUrl` (and `targetUri`) on
      every platform; legacy `fallbackUrl` removed.
- [ ] REQ-5: Atomic batch dispatch is the default code path on every
      platform, not a host-supplied opt-in, so cross-action failure
      policy always applies.
- [ ] REQ-6: Triggers a platform cannot host (e.g. `onLongPress` on web,
      `onSwipe` outside carousels) are explicitly logged and dropped at
      the renderer with a documented reason, not silently ignored.

## Tasks

### Phase 1: Schema & codegen audit
- [ ] Confirm `Action` schema is the source of truth for `target`,
      `operation`, `value`, `webUrl`, and trigger enum; no schema change
      required.
- [ ] Re-run `make codegen` to confirm generated models on web, Android,
      iOS already expose the canonical names; record any drift.
- [ ] Add a contract test that asserts every `ActionTrigger` enum value
      has either (a) a renderer dispatch path or (b) a documented "not
      hosted on this platform" entry.

### Phase 2: Web — schema-aligned action handler (legacy removed)
- [ ] Update `web/src/runtime/ActionHandler.ts`:
  - [ ] `handleMutate` reads `action.target` / `action.value`; **remove**
        `stateKey` / `stateValue` paths entirely (no legacy tolerance).
  - [ ] Implement all `MutateOperation` values (`set`, `toggle`,
        `increment`, `append`). For type mismatches (e.g. `increment`
        on non-numeric, `append` on non-array, `toggle` on non-boolean,
        missing current value), apply a neutral no-op and emit a
        `console.warn`. Add a `// TODO(action-mutate): review type-
        mismatch semantics across platforms` code comment at the
        fallback site.
  - [ ] `handleNavigate` reads `action.webUrl` (with `targetUri`
        fallback for native deeplinks); **remove** `fallbackUrl`
        entirely.
- [ ] Audit server composers under `server/src/main/java/...` for any
      remaining emission of legacy names. Phase 1 confirmed the precise
      scope: `stateKey` / `stateValue` are still emitted in
      `ScheduleComposer`, `DemoScreenComposer`, `GameDetailComposer`,
      `WatchComposer`, `BoxscoreComposer`. **No** server emission of
      `fallbackUrl` or a `sectionId` payload field exists.
- [ ] Add unit tests covering every mutate operation, the target/value
      shape, and each type-mismatch no-op path.

### Phase 3: Web — atomic trigger coverage
- [ ] Generalize `getActivateActions` into a `selectActions(trigger)`
      helper.
- [ ] `onLongPress` is **not hosted** on the web renderer library.
      Decoded actions with this trigger are silently ignored at the
      atomic-dispatch layer (debug-log only). Document in the trigger
      matrix.
- [ ] Wire `onSubmit` on `AtomicButton` when nested inside a `<form>`;
      route `Form` section's `submitAction` through the same path.
- [ ] Wire `onFocus` / `onBlur` on focusable atomic primitives
      (`AtomicButton`, `AtomicContainer` with `tabIndex`,
      `AtomicTextField` if present).
- [ ] Document `onSwipe` as carousel-only at the section level
      (`ScrollContainer`); add explicit "not hosted at element level"
      log on atomic primitives.

### Phase 4: Android — schema-aligned action handler (legacy removed)
- [ ] Update `ActionAdapter.toSduiAction()` so `SduiAction` carries the
      schema-named `value` / `target` / `webUrl` directly; **remove**
      the `stateKey` / `stateValue` / `sectionId` / `fallbackUrl`
      aliases (no legacy tolerance).
- [ ] Update `ActionHandler.handleMutate` to read `target` and apply
      all `MutateOperation` values via `StateManager`. Type mismatches
      use a neutral no-op + warn log with a
      `// TODO(action-mutate): review type-mismatch semantics across
      platforms` code comment at each fallback site.
- [ ] Extend `StateManager` with `toggle`, `increment`, `append`
      operations; keep `set` (and null-value delete) as today.
- [ ] Update all callers (`ScreenViewModel`, navigation, etc.) to use
      the renamed fields.
- [ ] Add unit tests for each operation and for unknown-operation
      fallback (neutral default + log).

### Phase 5: Android — atomic trigger coverage
- [ ] Generalize `getActivateActions` into `selectActions(trigger)`.
- [ ] Wire `onLongPress` via `Modifier.combinedClickable`'s
      `onLongClick` on `AtomicContainer` / `AtomicImage` / `AtomicText`.
- [ ] Wire `onFocus` / `onBlur` via `Modifier.onFocusChanged` on
      focusable atomic primitives.
- [ ] Wire `onSubmit` for the `Form` section's submit button through the
      shared executor (today the renderer calls `submitAction`
      directly).
- [ ] Document `onSwipe` as `ScrollContainer`-only.

### Phase 6: Atomic batch dispatch is the default
- [ ] Web: have `App.tsx` (and any future host) provide a single
      executor context; remove per-primitive `onAction` fallbacks that
      bypass `executeActionSequence`.
- [ ] Android: install a default `LocalActionExecutor` provider inside
      `SduiScreenContent` so library hosts get batch semantics out of
      the box; demo app override stays for analytics tagging.
- [ ] iOS already routes through `ActionDispatcher.execute(_:)` via the
      view-model. Add a contract test that asserts atomic primitives
      hand multi-action sequences to the dispatcher rather than
      iterating per-action.
- [ ] Add a contract test that asserts every primitive activation calls
      the batch executor when ≥ 2 actions share a trigger.

### Phase 7: iOS — trigger coverage parity & type-mismatch logging
- [ ] Mutate handler already aligned (`ScreenState.apply` covers all 4
      ops). Add a warn-level `Logger` line and a
      `// TODO(action-mutate): review type-mismatch semantics across
      platforms` code comment at each silent fallback in
      `ScreenState.apply` (e.g. `toggle` on non-bool, `append` to a
      type that is neither array nor string, `increment` with
      non-numeric current value).
- [ ] Generalize `SectionInteractions.primaryAction(trigger:)` /
      `subsectionPrimaryAction(trigger:)` usage so atomic primitives
      can request actions for any `ActionTrigger`.
- [ ] Wire `onLongPress` on `AtomicContainer` / `AtomicImage` /
      `AtomicText` using `LongPressGesture` / `.onLongPressGesture`.
- [ ] Wire `onFocus` / `onBlur` via `@FocusState` on focusable atomic
      primitives (`AtomicButton`, `AtomicTextField`).
- [ ] Route `Form` section `submitAction` through the dispatcher's
      `execute` path the same way Android/web will; remove any direct
      dispatch.
- [ ] Document `onSwipe` as `ScrollContainer`-only (carousel
      `DragGesture`); not hosted at the element level.
- [ ] Strip the deprecated `.onTap` alias log from `ActionDispatcher`
      once the trigger matrix asserts coverage (out-of-scope-safe
      tidy-up).

### Phase 8: Documentation
- [ ] Update `docs/glossary.md` `Trigger` entry to reflect actual
      platform hosting (which triggers are dispatched at element level
      vs section level vs not hosted, including `onLongPress` = web
      not-hosted).
- [ ] Update `docs/client-implementors-contract.md` action section with
      the canonical wire field names and the trigger-coverage matrix
      (web / Android / iOS).
- [ ] Update `docs/sdui-requirements-summary.md` action-system status to
      reference this plan.
- [ ] Owner sweep flag for `docs/appendix-kitchen-sink.md` (do not edit
      — note required).

### Phase 9: Tests

**Existing coverage baseline:**

| Platform | Action-system tests today | Notes |
|---|---|---|
| iOS | Partial | `ScreenStateTests` covers set / toggle / increment (int+double) / append (array+string) / nil-op-as-set. `ActionDispatcherTests` covers mutate-set, toast, refresh, dismiss, fireAndForget (onActivate + legacy onTap), onVisible dedup, navigate halt. |
| Android | **None** | No `ActionHandlerTest`, `StateManagerTest`, or `ActionAdapterTest` files exist. This phase creates the suites. |
| Web | **None** | No `ActionHandler.test.ts` exists. This phase creates the suite. |

**Work:**

- [ ] **Web (new suite)** — `web/src/runtime/ActionHandler.test.ts`:
  - [ ] Mutate ops: `set`, `toggle`, `increment`, `append` happy paths.
  - [ ] Mutate type-mismatch no-op + `console.warn` assertion for each
        op (toggle on non-bool, increment on non-numeric, append on
        non-array/non-string, missing current value).
  - [ ] Navigate with `webUrl` only, `targetUri` only, both — assert
        legacy `fallbackUrl` is rejected/ignored.
  - [ ] Trigger dispatch matrix on atomic primitives: each supported
        trigger fires the matching actions in declared order;
        `onLongPress` emits the documented debug log and dispatches
        nothing.
  - [ ] Failure-policy halt across a 2+ action `onActivate` sequence.
- [ ] **Android (new suites)** — under
      `android/sdui-core/src/test/java/com/nba/sdui/core/state/`:
  - [ ] `ActionHandlerTest.kt` — sequence execution, per-type failure
        defaults, halt semantics, navigate field reads, mutate target/
        operation/value reads.
  - [ ] `StateManagerTest.kt` — all 4 mutate ops + type-mismatch warn
        log fallback.
  - [ ] `ActionAdapterTest.kt` — wire `target`/`value`/`webUrl` survive
        adapter round-trip; legacy aliases removed.
  - [ ] Trigger dispatch matrix via a renderer-level test on at least
        one atomic primitive (long-press, focus/blur, submit).
- [ ] **iOS (additions to existing suites)**:
  - [ ] Extend `ScreenStateTests` with type-mismatch assertions (warn
        log via captured `Logger`).
  - [ ] Extend `ActionDispatcherTests` with `webURL`-only and
        `targetURI`+`webURL` navigate cases.
  - [ ] New trigger dispatch tests for longPress / focus / blur /
        submit once the atomic primitives wire them.
- [ ] **Schema contract test (all platforms)**: every server-emitted
      action in fixture payloads passes JSON Schema validation against
      canonical field names — no `stateKey`/`stateValue`/`fallbackUrl`
      escape into wire output.
- [ ] **Trigger-coverage contract test**: enumerate
      `ActionTrigger` values from the generated model; assert each is
      either wired in the platform's dispatch table or listed in the
      documented "not hosted on this platform" registry (web
      `onLongPress`, etc.).

## Dependencies

- AGENTS.md §1.2 schema-first sequence: schema → codegen → renderer →
  server. We are working backwards (renderer alignment to existing
  schema), so no schema change is required, but Phase 2 server cleanup
  must follow Phase 1 codegen confirmation.
- `make codegen` must succeed before Phase 2/4 land. `make codegen-tools`
  installs `quicktype` automatically and is a dependency of `make
  codegen`.

## Risks & Open Questions

- **Breaking-change posture: legacy names are removed outright on every
  platform** (web `stateKey`/`stateValue`/`fallbackUrl`, Android
  `stateKey`/`stateValue`/`sectionId`/`fallbackUrl`). Per project
  direction we are not maintaining a tolerance window; any server-side
  emitter still using legacy names must be fixed in Phase 2's server
  audit before the client cutover lands.
- `onSubmit` overlap: `Form` already invokes its own `submitAction`. The
  plan routes it through the executor on every platform so failure
  policy and analytics behave like any other trigger; confirm no
  callsite relies on the synchronous return value.
- `onSwipe` direction semantics are not yet in the schema. Until they
  are, swipe support is limited to `ScrollContainer` page-change events
  and explicitly out of scope for atomic primitives.
- `append` / `increment` / `toggle` type mismatches use the proposed
  neutral no-op + warn log default and ship with a `TODO(action-
  mutate)` code comment so each platform's resolver can be revisited
  together once we have real-world payloads.

## Out of Scope

- Schema additions (no new triggers, no new action types).
- New section types or section-level action precedence changes.
- Changes to `onVisible` impression policy (covered by
  `plan-impression-tracking.md`).
