# Action Failure Semantics — Cross-Cutting Plan

> **Date**: 2026-03-13
> **Status**: Draft
> **Scope**: Schema, requirements, Android (`ActionHandler` + `SduiScreenViewModel`), Web (`ActionHandler.ts` + `App.tsx`), iOS (future), Technical Proposal
> **Depends on**: Requirements Summary §4 (Action System), ADR-005 (Action Scope and Precedence)

---

## Problem Statement

The SDUI action system defines how actions fire (triggers, types, sequencing, precedence) but never defines what happens when an action **fails**. Today each platform handles failures differently:

- **Android** `ActionHandler.handle()` returns `ActionResult.Error(message)` for missing `targetUri` or `stateKey` — but the caller (`SduiScreenViewModel.handleAction`) emits it as a generic result with no user feedback or sequence control.
- **Web** `executeAction()` silently logs failures via `console.warn` / `console.error` and continues. Navigate failures show a toast only for parameterized refresh errors.
- **iOS** not yet built — will inherit whatever pattern is documented (or invent its own).

Without a defined failure contract, the same server payload produces different user experiences across platforms. This defeats the SDUI promise of consistent cross-platform behavior.

---

## Design Decisions

### Per-Action-Type Failure Behavior

| Action Type | On Failure | User Feedback | Sequence Effect |
|---|---|---|---|
| **analytics** | Fire-and-forget | None — silent | **Continue** — never blocks the sequence |
| **mutate** | Log warning, no-op | None — silent | **Continue** — non-destructive state miss |
| **navigate** | URI doesn't resolve or is malformed | Platform-native error feedback (Android Snackbar, iOS banner, web toast) with generic localized message | **Halt** — remaining actions in the sequence do not execute |
| **refresh** | Network error or non-2xx response | Show stale indicator on the affected section | **Continue** — stale data is better than no data |
| **dismiss** | Target not found (no modal/overlay open) | None — silent | **Continue** — idempotent by nature |
| **toast** | Display fails (edge case) | None — best effort | **Continue** — cosmetic, non-critical |

### Sequence Execution Rules

Actions on a single trigger execute **in declared order** (per existing requirements). The failure contract adds:

1. **Analytics actions never block.** An analytics failure (network error, malformed params) is swallowed silently. The sequence always continues past analytics.

2. **Navigate failures halt the sequence.** If a navigate action fails (URI scheme not registered, deep link not resolvable, webUrl unreachable), execution stops. No subsequent actions in the sequence fire.

3. **All other action types continue on failure.** Mutate, refresh, dismiss, and toast failures log a warning and the sequence proceeds to the next action.

4. **Already-fired actions are committed.** There is no rollback. If analytics fires successfully and then navigate fails, the analytics beacon is already sent. This is intentional — analytics captures what the user *attempted*, not what succeeded.

5. **Post-navigate-halt behavior.** When a navigate action fails and halts the sequence, all actions that already executed (analytics, mutate, etc.) are committed. All remaining actions — including non-navigate actions that appear after the failed navigate — do **not** execute.

### Sequence Flow Diagram

```
trigger fires → execute actions[0..N] in order
  │
  ├─ action[i] is analytics → execute, ignore failure → continue to [i+1]
  ├─ action[i] is mutate    → execute, log warning on failure → continue to [i+1]
  ├─ action[i] is refresh   → execute, show stale on failure → continue to [i+1]
  ├─ action[i] is dismiss   → execute, ignore failure → continue to [i+1]
  ├─ action[i] is toast     → execute, ignore failure → continue to [i+1]
  └─ action[i] is navigate  → execute
       ├─ success → HALT sequence (navigation takes over the UI)
       └─ failure → show platform-native error → HALT sequence
```

**Note:** Navigate *success* also halts the sequence — navigation takes over the screen, so subsequent actions are moot. This is why the existing recommendation is "analytics-before-navigation" sequencing: fire beacons before the navigate action, because nothing runs after it regardless of outcome.

### Navigate Failure — User Feedback

Clients use their platform's native transient error mechanism with a generic localized string:

| Platform | Mechanism | Message |
|---|---|---|
| Android | `Snackbar` (short duration) | `"Unable to open page"` (localized via `strings.xml`) |
| iOS | `UIAlertController` or notification banner | `"Unable to open page"` (localized via `Localizable.strings`) |
| Web | Toast notification (auto-dismiss) | `"Unable to open page"` (localized via i18n bundle) |

The server does **not** provide error messages for action failures. The client owns the error UX.

### Refresh Failure — Stale Indicator

When a `refresh` action fails (network error, non-2xx, timeout):

1. The section retains its current data (last successful response).
2. A stale indicator is shown on the section (e.g., subtle "Last updated X min ago" label, dimmed opacity, or a small warning icon — platform-appropriate).
3. Retry mechanisms (exponential backoff, manual pull-to-refresh) are **deferred to a later phase**.

### Mutate Failure — Silent No-Op

When a `mutate` action references a state key that doesn't exist or applies an invalid operation:

1. Log a warning with the state key and operation for debugging.
2. Do not modify state.
3. Sequence continues.

This is safe because mutate is a local operation — there's no side effect to roll back.

### Observability

Action failures are captured by existing mobile agent instrumentation (New Relic, Datadog, etc.) via standard error/warning log levels. The action handler logs:

- **Level**: `warn` for all action failures
- **Content**: action type, trigger, failure reason, section ID (if available)

No custom SDUI-specific analytics beacon is needed for action errors — APM tooling already aggregates warning-level logs with platform context (device, OS version, app version, screen).

---

## Current State Audit

### Android — `ActionHandler.kt`

| Behavior | Current | Required |
|---|---|---|
| Navigate — missing URI | Returns `ActionResult.Error`, no user feedback | Return error result + show `Snackbar` |
| Analytics — any failure | Not possible in current impl (just logs) | Already correct — keep fire-and-forget |
| Mutate — missing stateKey | Returns `ActionResult.Error` | Log warning, return no-op result, continue |
| Refresh — network failure | Parameterized refresh catches exception, shows toast | Change toast to stale indicator on section |
| Sequence execution | `handleAction()` in ViewModel handles one action at a time — no multi-action sequencing | Add `executeSequence(actions, stateManager)` with halt-on-navigate-failure |

### Android — `SduiScreenViewModel.kt`

| Behavior | Current | Required |
|---|---|---|
| Multi-action dispatch | Caller dispatches individual actions in a loop with no failure awareness | ViewModel should call `ActionHandler.executeSequence()` and observe the halt signal |

### Web — `ActionHandler.ts`

| Behavior | Current | Required |
|---|---|---|
| Navigate — missing URI | Silent return | Show toast with generic error message |
| Analytics — any failure | Console log only | Already correct |
| Mutate — missing stateKey | Silently skips | Add `console.warn`, already continues |
| Refresh — network failure | Shows toast "Refresh failed" | Change to stale indicator on section |
| Sequence execution | `App.tsx` calls handler once per action — no multi-action sequencing | Add `executeActionSequence(actions, context)` with halt-on-navigate-failure |

### Web — `App.tsx`

| Behavior | Current | Required |
|---|---|---|
| Multi-action dispatch | Handles one action at a time via `handleAction` callback | Wire through to `executeActionSequence` |

---

## Implementation Plan

### Phase 1: Define the Contract (Documentation)

**Scope**: Requirements Summary, Technical Proposal

| Step | Document | Change |
|---|---|---|
| 1.1 | `sdui-requirements-summary.md` §4 | Add "Action Failure Semantics" subsection after "Composability" with the per-type failure table, sequence rules, and navigate-halts-sequence rule |
| 1.2 | `sdui-requirements-summary.md` §4 | Add "Sequence Execution Contract" subsection specifying: declared order, analytics-never-blocks, navigate-halts, committed-no-rollback |
| 1.3 | `SDUI_Technical_Proposal_v2.md` §4 | Add "Failure Behavior" subsection after "Precedence and Composability" with the same rules and platform-specific feedback table |
| 1.4 | `SDUI_Technical_Proposal_v2.md` §4 | Update the existing sequence diagram to show the analytics-then-navigate pattern with a failure branch |

### Phase 2: Sequence Executor (Android)

**Scope**: `ActionHandler.kt`, `SduiScreenViewModel.kt`

| Step | File | Change |
|---|---|---|
| 2.1 | `ActionHandler.kt` | Add `executeSequence(actions: List<SduiAction>, stateManager: StateManager): List<ActionResult>` that iterates actions in order, halting on navigate (success or failure) |
| 2.2 | `ActionHandler.kt` | Update `handleNavigate` to distinguish resolvable vs unresolvable URIs — return `ActionResult.NavigateResult` on success, `ActionResult.NavigateError(uri, reason)` on failure |
| 2.3 | `ActionHandler.kt` | Update `handleMutate` — on missing state key, log warning and return `ActionResult.MutateNoOp(key)` instead of `ActionResult.Error` |
| 2.4 | `ActionHandler.kt` | Update `handleRefresh` — on failure, return `ActionResult.RefreshStale(sectionId)` instead of showing toast |
| 2.5 | `SduiScreenViewModel.kt` | Replace single-action `handleAction()` with `handleActions(actions: List<SduiAction>)` that delegates to `ActionHandler.executeSequence()` |
| 2.6 | `SduiScreenViewModel.kt` | On `NavigateError` result, emit a `Snackbar` event with localized "Unable to open page" |
| 2.7 | `SduiScreenViewModel.kt` | On `RefreshStale` result, mark the affected section as stale in UI state |
| 2.8 | Section renderers | Where renderers call `onAction` with a single action, update call sites that have multi-action triggers to pass the full action list |

### Phase 3: Sequence Executor (Web)

**Scope**: `ActionHandler.ts`, `App.tsx`

| Step | File | Change |
|---|---|---|
| 3.1 | `ActionHandler.ts` | Add `executeActionSequence(actions: Action[], context: ActionContext): { executed: Action[], halted: boolean }` with same halt-on-navigate logic |
| 3.2 | `ActionHandler.ts` | Update `handleNavigate` to return success/failure. On failure, call `showToast("Unable to open page")` |
| 3.3 | `ActionHandler.ts` | Update `handleRefresh` — on failure, call `context.onSectionStale(sectionId)` instead of `showToast` |
| 3.4 | `ActionHandler.ts` | Add `onSectionStale` to `ActionContext` interface |
| 3.5 | `App.tsx` | Implement `handleSectionStale` callback — toggle a `stale` flag on the section in state |
| 3.6 | `App.tsx` | Update `handleAction` to use `executeActionSequence` when the trigger has multiple actions |
| 3.7 | `SectionRouter.tsx` | Render stale indicator when a section is marked stale |

### Phase 4: Schema Alignment (Optional)

No schema changes are required for the failure contract — it is purely a client-side behavioral specification. However, if the stale indicator needs server-controlled customization in the future (e.g., custom stale message per section), the `SectionStates` definition already has the right extension point:

```json
"sectionStates": {
  "stale": {
    "message": "Scores may be delayed",
    "showTimestamp": true
  }
}
```

This is **deferred** — the initial implementation uses a generic platform-native stale indicator.

---

## Files Impacted

| File | Phase | Change Type |
|---|---|---|
| `docs/sdui-requirements-summary.md` | 1 | Add failure semantics + sequence contract subsections |
| `docs/SDUI_Technical_Proposal_v2.md` | 1 | Add failure behavior subsection + update sequence diagram |
| `android/.../state/ActionHandler.kt` | 2 | Add `executeSequence`, new result subtypes, failure handling |
| `android/.../screen/SduiScreenViewModel.kt` | 2 | Wire sequence execution, Snackbar on navigate error, stale state |
| `android/.../renderer/sections/*` | 2 | Update multi-action call sites |
| `web/src/runtime/ActionHandler.ts` | 3 | Add `executeActionSequence`, `onSectionStale`, navigate error toast |
| `web/src/App.tsx` | 3 | Wire sequence execution, stale callback |
| `web/src/components/SectionRouter.tsx` | 3 | Render stale indicator |

---

## Out of Scope (Deferred)

| Item | Reason |
|---|---|
| Retry mechanisms (exponential backoff, manual retry) | Deferred to later phase per decision |
| Server-defined error messages on actions | Client owns error UX with generic localized strings |
| Custom SDUI observability beacon (`sdui_action_error`) | Covered by existing APM instrumentation (New Relic / Datadog) at warn log level |
| iOS implementation | iOS renderer not yet built; will follow same contract when implemented |
| Schema changes for `sectionStates.stale` | Generic stale indicator first; server-customized stale messaging deferred |

---

## Verification

| Check | Method |
|---|---|
| Navigate failure shows platform error | Manually trigger a navigate action with an unresolvable `nba://invalid/path` URI. Verify Snackbar (Android) / toast (web) appears. |
| Analytics failure doesn't block | Intentionally break analytics dispatch (e.g., null event name). Verify subsequent navigate action still fires. |
| Mutate failure continues | Send a mutate action with a non-existent state key. Verify warning logged, sequence continues, no crash. |
| Refresh failure shows stale | Kill the server mid-poll. Verify stale indicator appears on the section, existing data remains visible. |
| Navigate halts sequence | Define a trigger with `[analytics, navigate, mutate]`. Verify analytics fires, navigate executes (success or fail), mutate does **not** execute. |
| Committed analytics on navigate failure | Same 3-action sequence with a bad navigate URI. Verify the analytics beacon was sent even though navigate failed. |
