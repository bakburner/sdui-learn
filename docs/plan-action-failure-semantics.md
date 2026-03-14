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

## Design Rationale: Hybrid Server-Declared + Client Defaults (Option C)

Three approaches were evaluated:

| Option | Approach | Trade-off |
|---|---|---|
| **A** | All failure policy hardcoded in client code per action type | Simplest to ship, but policy is frozen at app release time; every change requires coordinated multi-platform releases — the exact problem this plan exists to solve |
| **B** | Server declares `onFailure` on every action; no client defaults | Fully server-driven, but adds mandatory schema complexity and breaks older clients that don't understand the field |
| **C (chosen)** | Server *may* declare `onFailure`; client applies sensible per-type defaults when the field is absent | Ships as fast as Option A (defaults match the table below), gains server tunability for free, and is backward-compatible |

**Why Option C:**

1. **Consistent with SDUI philosophy.** Behavior should be server-governed. Hardcoding policy in every client is the same pattern that created the cross-platform drift described in ADR-001.
2. **Zero app-release policy changes.** The server can override the default for a specific action (e.g., make `refresh` halt on a critical live-game section) without touching any client code.
3. **Minimal schema cost.** One optional enum field (`onFailure`) and one optional object (`failureFeedback`) on `SduiAction`. Older clients that ignore these fields get the correct defaults automatically.
4. **Eliminates the per-type policy lookup table from every client.** The executor reads one field — or falls back to a small default map. No platform-specific behavioral interpretation.
5. **Future-proofs against "just this one action" requests.** The inevitable product ask — "navigate should *continue* for this one specific deep link" — becomes a server config change instead of a triple-platform release.

---

## Design Decisions

### Schema: `onFailure` and `failureFeedback`

Two optional fields are added to every `SduiAction`:

```json
{
  "type": "navigate",
  "targetUri": "nba://game/123",
  "onFailure": "halt",
  "failureFeedback": {
    "message": "This game is not available yet",
    "style": "snackbar"
  }
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `onFailure` | `"halt"` \| `"continue"` \| `"silent"` | No | Sequence behavior when this action fails. Client uses per-type default (see table below) when absent. |
| `failureFeedback` | `{ message?: string, style?: "snackbar" \| "toast" \| "inline" }` | No | Server-provided error message and presentation style. Client falls back to its generic localized string when absent. |

`onFailure` values:

| Value | Meaning |
|---|---|
| `"halt"` | Stop sequence execution, show error feedback |
| `"continue"` | Log warning, proceed to next action |
| `"silent"` | Swallow failure silently, proceed (no log to user) |

### Per-Action-Type Default Failure Behavior

These defaults apply when `onFailure` is absent. The server can override any of them per action.

| Action Type | Default `onFailure` | Default User Feedback | Sequence Effect |
|---|---|---|---|
| **analytics** | `silent` | None | **Continue** — never blocks the sequence |
| **mutate** | `continue` | None | **Continue** — non-destructive state miss |
| **navigate** | `halt` | Platform-native error (Snackbar / banner / toast) with server message or generic localized fallback | **Halt** — remaining actions do not execute |
| **refresh** | `continue` | Show stale indicator on the affected section | **Continue** — stale data is better than no data |
| **dismiss** | `silent` | None | **Continue** — idempotent by nature |
| **toast** | `silent` | None — best effort | **Continue** — cosmetic, non-critical |

### Sequence Execution Rules

Actions on a single trigger execute **in declared order** (per existing requirements). The failure contract adds:

1. **The executor reads `onFailure` from the action.** If the field is present, its value (`halt`, `continue`, `silent`) determines sequence behavior. If absent, the client applies the per-type default from the table above.

2. **`silent` actions never block.** A failure is swallowed with no user-visible feedback. The sequence always continues. Default for: `analytics`, `dismiss`, `toast`.

3. **`halt` failures stop the sequence.** Execution stops. No subsequent actions fire. User feedback is shown (server-provided `failureFeedback.message` if present, otherwise generic localized fallback). Default for: `navigate`.

4. **`continue` failures log and proceed.** A warning is logged, any type-specific side effect applies (e.g., stale indicator for refresh), and the sequence moves to the next action. Default for: `mutate`, `refresh`.

5. **Already-fired actions are committed.** There is no rollback. If analytics fires successfully and then navigate fails, the analytics beacon is already sent. This is intentional — analytics captures what the user *attempted*, not what succeeded.

6. **Navigate *success* also halts.** Regardless of `onFailure`, a successful navigate takes over the screen, so subsequent actions are moot. This is why the recommended pattern is analytics-before-navigation sequencing.

### Sequence Flow Diagram

```
trigger fires → execute actions[0..N] in order
  │
  for each action[i]:
    resolve policy = action.onFailure ?? default_for(action.type)
    execute action[i]
    │
    ├─ success + action is navigate → HALT (navigation takes over UI)
    ├─ success + action is not navigate → continue to [i+1]
    └─ failure:
         ├─ policy is "silent"   → swallow → continue to [i+1]
         ├─ policy is "continue" → log warn, apply side-effect → continue to [i+1]
         └─ policy is "halt"     → show feedback (server msg or fallback) → HALT
```

**Note:** Navigate *success* also halts the sequence — navigation takes over the screen, so subsequent actions are moot. This is why the existing recommendation is "analytics-before-navigation" sequencing: fire beacons before the navigate action, because nothing runs after it regardless of outcome.

### Navigate Failure — User Feedback

When a navigate action fails and its resolved policy is `halt`, the client displays an error message:

1. **Server-provided message** (`failureFeedback.message`): used when present. Allows the server to provide context-specific copy (e.g., "This game is not available yet").
2. **Generic localized fallback**: used when `failureFeedback` is absent. Each platform provides its own localized string.

| Platform | Mechanism | Fallback Message |
|---|---|---|
| Android | `Snackbar` (short duration) | `"Unable to open page"` (localized via `strings.xml`) |
| iOS | `UIAlertController` or notification banner | `"Unable to open page"` (localized via `Localizable.strings`) |
| Web | Toast notification (auto-dismiss) | `"Unable to open page"` (localized via i18n bundle) |

The `failureFeedback.style` field (`snackbar`, `toast`, `inline`) is a hint. Clients map it to the closest platform-native mechanism. If unrecognized or absent, the platform default is used.

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
| Navigate — missing URI | Returns `ActionResult.Error`, no user feedback | Return error result + show feedback (server `failureFeedback.message` or localized fallback) |
| Analytics — any failure | Not possible in current impl (just logs) | Already correct — keep fire-and-forget |
| Mutate — missing stateKey | Returns `ActionResult.Error` | Log warning, return no-op result, continue |
| Refresh — network failure | Parameterized refresh catches exception, shows toast | Change toast to stale indicator on section |
| Sequence execution | `handleAction()` in ViewModel handles one action at a time — no multi-action sequencing | Add `executeSequence()` that reads `action.onFailure` (with per-type fallback) to determine halt/continue/silent |
| `onFailure` / `failureFeedback` fields | Not in model | Codegen from updated schema; executor reads them |

### Android — `SduiScreenViewModel.kt`

| Behavior | Current | Required |
|---|---|---|
| Multi-action dispatch | Caller dispatches individual actions in a loop with no failure awareness | ViewModel should call `ActionHandler.executeSequence()` and observe the halt signal |

### Web — `ActionHandler.ts`

| Behavior | Current | Required |
|---|---|---|
| Navigate — missing URI | Silent return | Show feedback (server message or generic toast) |
| Analytics — any failure | Console log only | Already correct |
| Mutate — missing stateKey | Silently skips | Add `console.warn`, already continues |
| Refresh — network failure | Shows toast "Refresh failed" | Change to stale indicator on section |
| Sequence execution | `App.tsx` calls handler once per action — no multi-action sequencing | Add `executeActionSequence()` that reads `action.onFailure` (with per-type fallback) to determine halt/continue/silent |
| `onFailure` / `failureFeedback` fields | Not in TypeScript types | Codegen from updated schema; executor reads them |

### Web — `App.tsx`

| Behavior | Current | Required |
|---|---|---|
| Multi-action dispatch | Handles one action at a time via `handleAction` callback | Wire through to `executeActionSequence` |

---

## Implementation Plan

### Phase 1: Schema + Codegen

**Scope**: `sdui-schema.json`, codegen output (Kotlin, TypeScript, Swift)

| Step | File | Change |
|---|---|---|
| 1.1 | `schema/sdui-schema.json` | Add optional `onFailure` enum (`halt`, `continue`, `silent`) to `SduiAction` definition |
| 1.2 | `schema/sdui-schema.json` | Add optional `failureFeedback` object (`{ message?: string, style?: "snackbar" \| "toast" \| "inline" }`) to `SduiAction` definition |
| 1.3 | `codegen/` | Run `./generate.sh` — produces updated Kotlin data classes, TypeScript types, Swift structs |
| 1.4 | `schema/examples/` | Add example action payloads: one with `onFailure` override, one without (showing default behavior) |

### Phase 2: Define the Contract (Documentation)

**Scope**: Requirements Summary, Technical Proposal

| Step | Document | Change |
|---|---|---|
| 2.1 | `sdui-requirements-summary.md` §4 | Add "Action Failure Semantics" subsection after "Composability" with the per-type default table, `onFailure` / `failureFeedback` schema fields, sequence rules, and navigate-halts-sequence rule |
| 2.2 | `sdui-requirements-summary.md` §4 | Add "Sequence Execution Contract" subsection specifying: declared order, `onFailure` resolution (field → per-type default), committed-no-rollback |
| 2.3 | `SDUI_Technical_Proposal_v2.md` §4 | Add "Failure Behavior" subsection after "Precedence and Composability" with the same rules, schema fields, and platform-specific feedback table |
| 2.4 | `SDUI_Technical_Proposal_v2.md` §4 | Update the existing sequence diagram to show the `onFailure` resolution flow with a failure branch |

### Phase 3: Sequence Executor (Android)

**Scope**: `ActionHandler.kt`, `SduiScreenViewModel.kt`

| Step | File | Change |
|---|---|---|
| 3.1 | `ActionHandler.kt` | Add `resolveFailurePolicy(action): FailurePolicy` — reads `action.onFailure`, falls back to per-type default map |
| 3.2 | `ActionHandler.kt` | Add `executeSequence(actions: List<SduiAction>, stateManager: StateManager): List<ActionResult>` that iterates actions in order, calling `resolveFailurePolicy()` to determine halt/continue/silent on failure |
| 3.3 | `ActionHandler.kt` | Update `handleNavigate` to distinguish resolvable vs unresolvable URIs — return `ActionResult.NavigateResult` on success, `ActionResult.NavigateError(uri, reason)` on failure |
| 3.4 | `ActionHandler.kt` | Update `handleMutate` — on missing state key, log warning and return `ActionResult.MutateNoOp(key)` instead of `ActionResult.Error` |
| 3.5 | `ActionHandler.kt` | Update `handleRefresh` — on failure, return `ActionResult.RefreshStale(sectionId)` instead of showing toast |
| 3.6 | `SduiScreenViewModel.kt` | Replace single-action `handleAction()` with `handleActions(actions: List<SduiAction>)` that delegates to `ActionHandler.executeSequence()` |
| 3.7 | `SduiScreenViewModel.kt` | On `NavigateError` result with `halt` policy, show feedback: use `action.failureFeedback.message` if present, otherwise emit `Snackbar` with localized "Unable to open page" |
| 3.8 | `SduiScreenViewModel.kt` | On `RefreshStale` result, mark the affected section as stale in UI state |
| 3.9 | Section renderers | Where renderers call `onAction` with a single action, update call sites that have multi-action triggers to pass the full action list |

### Phase 4: Sequence Executor (Web)

**Scope**: `ActionHandler.ts`, `App.tsx`

| Step | File | Change |
|---|---|---|
| 4.1 | `ActionHandler.ts` | Add `resolveFailurePolicy(action): FailurePolicy` — reads `action.onFailure`, falls back to per-type default map |
| 4.2 | `ActionHandler.ts` | Add `executeActionSequence(actions: Action[], context: ActionContext): { executed: Action[], halted: boolean }` with same `resolveFailurePolicy` logic |
| 4.3 | `ActionHandler.ts` | Update `handleNavigate` to return success/failure. On failure with `halt` policy, call `showToast(action.failureFeedback?.message ?? "Unable to open page")` |
| 4.4 | `ActionHandler.ts` | Update `handleRefresh` — on failure, call `context.onSectionStale(sectionId)` instead of `showToast` |
| 4.5 | `ActionHandler.ts` | Add `onSectionStale` to `ActionContext` interface |
| 4.6 | `App.tsx` | Implement `handleSectionStale` callback — toggle a `stale` flag on the section in state |
| 4.7 | `App.tsx` | Update `handleAction` to use `executeActionSequence` when the trigger has multiple actions |
| 4.8 | `SectionRouter.tsx` | Render stale indicator when a section is marked stale |

### Phase 5: Server Composer Support

**Scope**: Spring Boot server action builders

| Step | File | Change |
|---|---|---|
| 5.1 | Server action builders | Add `onFailure` and `failureFeedback` to action builder API so composers can set per-action overrides |
| 5.2 | Kitchen sink screen | Add example actions demonstrating `onFailure` overrides (e.g., a refresh action with `"halt"`, a navigate with `"continue"` and custom `failureFeedback`) |

---

## Files Impacted

| File | Phase | Change Type |
|---|---|---|
| `schema/sdui-schema.json` | 1 | Add `onFailure` enum + `failureFeedback` object to `SduiAction` |
| `codegen/` | 1 | Run `./generate.sh` — updated Kotlin, TypeScript, Swift models |
| `schema/examples/` | 1 | Add example payloads with and without `onFailure` overrides |
| `docs/sdui-requirements-summary.md` | 2 | Add failure semantics + sequence contract subsections |
| `docs/SDUI_Technical_Proposal_v2.md` | 2 | Add failure behavior subsection + update sequence diagram |
| `android/.../state/ActionHandler.kt` | 3 | Add `resolveFailurePolicy`, `executeSequence`, new result subtypes, failure handling |
| `android/.../screen/SduiScreenViewModel.kt` | 3 | Wire sequence execution, server/fallback feedback on halt, stale state |
| `android/.../renderer/sections/*` | 3 | Update multi-action call sites |
| `web/src/runtime/ActionHandler.ts` | 4 | Add `resolveFailurePolicy`, `executeActionSequence`, `onSectionStale`, server/fallback feedback |
| `web/src/App.tsx` | 4 | Wire sequence execution, stale callback |
| `web/src/components/SectionRouter.tsx` | 4 | Render stale indicator |
| Server action builders | 5 | `onFailure` + `failureFeedback` in builder API |
| Kitchen sink screen | 5 | Example actions with `onFailure` overrides |

---

## Out of Scope (Deferred)

| Item | Reason |
|---|---|
| Retry mechanisms (exponential backoff, manual retry) | Deferred to later phase per decision |
| Custom SDUI observability beacon (`sdui_action_error`) | Covered by existing APM instrumentation (New Relic / Datadog) at warn log level |
| iOS implementation | iOS renderer not yet built; will follow same contract when implemented |
| Server-customized stale indicator (`sectionStates.stale`) | Generic stale indicator first; server-controlled stale message/timestamp deferred to a later phase |

---

## Verification

| Check | Method |
|---|---|
| Schema validates | Add action with `onFailure` and `failureFeedback` to example payloads; run schema validation |
| Codegen produces fields | After `./generate.sh`, verify `onFailure` and `failureFeedback` appear in Kotlin, TypeScript, and Swift output |
| Navigate failure shows platform error | Manually trigger a navigate action with an unresolvable `nba://invalid/path` URI. Verify Snackbar (Android) / toast (web) appears with generic fallback. |
| Server-provided error message displayed | Send a navigate action with `failureFeedback: { message: "Game not available" }`. Verify the server message is shown instead of the generic fallback. |
| `onFailure` override respected | Send a navigate action with `"onFailure": "continue"`. Verify the sequence continues past the failed navigate instead of halting. |
| Default applies when `onFailure` absent | Send a navigate action without `onFailure`. Verify it halts (the per-type default). |
| Analytics failure doesn't block | Intentionally break analytics dispatch (e.g., null event name). Verify subsequent navigate action still fires. |
| Mutate failure continues | Send a mutate action with a non-existent state key. Verify warning logged, sequence continues, no crash. |
| Refresh failure shows stale | Kill the server mid-poll. Verify stale indicator appears on the section, existing data remains visible. |
| Navigate halts sequence | Define a trigger with `[analytics, navigate, mutate]`. Verify analytics fires, navigate executes (success or fail), mutate does **not** execute. |
| Committed analytics on navigate failure | Same 3-action sequence with a bad navigate URI. Verify the analytics beacon was sent even though navigate failed. |
| Backward compatibility | Send actions without `onFailure` or `failureFeedback` fields. Verify client behaves identically to pre-change behavior (per-type defaults). |
