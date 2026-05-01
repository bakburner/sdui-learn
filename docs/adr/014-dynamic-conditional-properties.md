# ADR-014: Dynamic Conditional Properties

- Status: Proposed
- Date: 2026-05-01
- Decision owners: TBD
- Related requirements: `docs/sdui-requirements-summary.md`
- Related ADRs: [ADR-005: Action Scope and Precedence](./005-action-scope-and-precedence.md)

## Decision

Choose how to support dynamic, state-dependent properties (e.g. `enabledIf`, `visibleIf`) on atomic elements and form fields, closing the gap between the existing `Conditional` element wrapper and the industry-standard inline expression model.

## Context

The schema currently supports two mechanisms for state-dependent UI:

1. **`Conditional` element** — a structural wrapper with `condition` (dot-path into screen state), `trueChild`, and `falseChild`. Evaluated on every render cycle.
2. **Static `disabled` boolean** — on `Button` and `FormField`, set at composition time with no awareness of runtime screen state.

This creates a gap: to dynamically disable a button based on form input, the server must emit a `Conditional` wrapper with two nearly-identical subtrees (one enabled, one disabled). For a single property this is tolerable; for multiple independent conditional properties it causes combinatorial tree duplication.

The screen-state propagation chain already supports re-evaluation: form field changes write to screen state → React/Compose/SwiftUI re-renders the tree → `evaluateCondition()` runs on every render. Any new expression-based property would piggyback on this existing reactive flow.

### Current `evaluateCondition` implementation

All three platforms share the same logic: split the condition string on `.`, walk the state object, and apply truthiness coercion (`boolean` → identity, `string` → non-empty, `number` → non-zero, `null`/`undefined` → false). There is no comparison, negation, or boolean combinator support today.

## Decision Drivers

- Minimise schema/wire-contract growth — every new field or type union is a forever commitment across all clients
- Preserve server authority (AGENTS.md §1.1) — the server decides what conditions matter; clients only evaluate them
- Keep client expression evaluation small, auditable, and injection-safe
- Support the primary use case: form field interdependency (e.g. "enable State field when country is US")
- Avoid duplicating the purpose of `dataBinding` (real-time external data → element properties)

## Options Considered

### Option 1: Conditional Wrappers (Status Quo)

The server wraps any element needing a dynamic property in a `Conditional`, emitting two subtrees — one for each state.

```json
{
  "type": "Conditional",
  "condition": "form.emailValid",
  "trueChild":  { "type": "Button", "label": "Submit", "disabled": false, "actions": [...] },
  "falseChild": { "type": "Button", "label": "Submit", "disabled": true }
}
```

Pros:
- Zero schema or client changes
- Server authority fully preserved
- Already works on all three platforms

Cons:
- Verbose — duplicates the element tree for each conditional property
- Combinatorial explosion when multiple properties depend on different state keys (N conditions → 2^N subtrees)
- Does not help with `FormField.disabled` (forms use their own renderer, not atomic elements)

### Option 2: Widen `disabled` to Accept a State Path

Change `disabled` from `boolean` to `boolean | string`. When a string, the client resolves it as a dot-path against screen state using the existing `evaluateCondition()`.

```json
{ "type": "Button", "label": "Submit", "disabled": "form.hasErrors" }
```

Pros:
- Minimal schema change (one field becomes a union type)
- Reuses existing `evaluateCondition` on all three platforms (~5 LOC per renderer)
- Server still owns the decision (which state key matters)
- No tree duplication

Cons:
- Widens the wire contract for one property only — does not generalise
- Codegen must handle `boolean | string` union cleanly
- Each additional conditional property would need the same one-off treatment

### Option 3: Generic `stateBindings` Map on AtomicElement

Add an optional `stateBindings` object to `AtomicElement`. Keys are property names, values are state paths (or expressions). At render time the client resolves each binding and overrides the static field.

```json
{
  "type": "Button",
  "label": "Submit",
  "disabled": true,
  "stateBindings": {
    "disabled": "form.hasErrors",
    "opacity": "card.dimmed"
  }
}
```

Pros:
- One schema addition covers `disabled`, `opacity`, `color`, `content`, or any future property
- Server declares which properties are state-dependent
- Clean separation: static values are defaults, bindings override at runtime

Cons:
- Overlaps with `dataBinding` (which maps real-time external data → element properties via `bindRef`/`targetPath`) — risks two competing binding paths (AGENTS.md §1.4, one owner per concern)
- Requires a safe property allow-list to prevent binding `actions`, `type`, or `children`
- Expression evaluation beyond dot-path truthiness is a significant complexity jump
- Every renderer on every platform needs binding-resolution middleware

### Option 4: Named `enabledIf` / `visibleIf` Expression Properties

Add two named conditional properties to `AtomicElement` and `FormField`:

```json
{
  "type": "Button",
  "label": "Submit",
  "enabledIf": "form.emailValid",
  "visibleIf": "user.isLoggedIn"
}
```

Clients evaluate these on every render using a lightweight expression grammar:

| Expression | Example | Meaning |
|---|---|---|
| Dot-path truthiness | `"form.email"` | Truthy if non-empty/non-null/non-zero |
| Negation | `"!form.hasErrors"` | Logical NOT of truthiness |
| Equality | `"form.country == 'US'"` | String/number equality |
| Inequality | `"form.country != 'US'"` | Negated equality |
| AND | `"form.emailValid && form.termsAccepted"` | Both truthy |
| OR | `"form.hasPhone \|\| form.hasEmail"` | Either truthy |

The grammar is intentionally constrained — no arithmetic, no nested expressions, no function calls.

Pros:
- Explicit, named semantics — easy to understand, validate, and document
- Server controls the expression and which property it applies to
- `visibleIf` eliminates `Conditional` wrappers for the common show/hide case
- Expression grammar is small (~50 LOC evaluator per platform) and auditable
- Re-evaluates on every render via the existing screen-state propagation chain

Cons:
- Two new schema fields on `AtomicElement` and `FormField`
- Adds an expression parser to every client platform
- Grammar must be tightly scoped or it becomes a DSL treadmill
- Risk of pushing business logic toward the client if expressions grow unconstrained

### Option 5: Server Pre-Resolution via Refresh

The server owns all conditional logic. When client state changes, a `refresh` action sends the new state to the server, which re-composes the screen with updated static values.

```json
{ "type": "Button", "label": "Submit", "disabled": true }
```

When the form field changes, a refresh action fires. The server returns the same screen with `"disabled": false`.

Pros:
- Zero client-side expression evaluation
- Purest server authority — 100% of conditional logic is server-side
- No schema change

Cons:
- Network round-trip for every state change — perceptible latency on form interactions
- Requires optimistic UI or debouncing for acceptable UX
- Higher server load for interactive forms
- Does not work offline

## Evidence

- **Existing reactive chain works:** Form field → `onStateChange` → screen state update → full tree re-render → `evaluateCondition()` re-runs. Verified on web (React `useState`), Android (Compose `MutableState`), iOS (SwiftUI `@Published`). Any expression-based property would re-evaluate on every state change with no additional plumbing.
- **Industry precedent:** Airbnb's SDUI uses `visibleIf`/`enabledIf` with a constrained expression grammar. DoorDash uses a similar pattern for conditional rendering.
- **`Conditional` element covers ~80% of cases today** but breaks down at form-field interdependency where `FormField` is not an atomic element and cannot be wrapped in `Conditional`.

## Decision Outcome

TBD — this ADR is proposed for discussion. The recommendation is:

- **For narrow scope** (only `disabled` on forms): Option 2 is the least disruptive.
- **For industry-standard pattern**: Option 4 with a tightly constrained expression grammar.
- **For maximum server authority**: Option 5 for non-latency-sensitive cases, combined with Option 2 for form fields requiring instant feedback.
- **Avoid**: Option 3 (`stateBindings`) due to overlap with `dataBinding` and the one-owner-per-concern principle.

## Consequences

- Short term:
  - Whichever option is chosen, the `Conditional` element remains the primary mechanism for structural show/hide
  - Form-field interdependency use cases remain limited to `Conditional` wrappers or static server composition until this ADR is accepted
- Medium term:
  - Option 4 would require expression evaluator implementations on all three platforms plus codegen updates
  - Option 2 would require codegen to handle `boolean | string` union for `disabled`
- Long term:
  - Options 3 or 4 expand the client-side evaluation surface; grammar discipline is critical to avoid drift toward client-owned business logic
  - Option 5 avoids that risk but trades it for latency and server cost

## Implementation Notes

- If Option 4 is chosen, the expression grammar must be formally defined (e.g. PEG or EBNF) and shared across platforms to guarantee identical evaluation semantics
- Expression evaluation must be sandboxed — no access to element tree, actions, or anything outside the flat screen-state map
- Unknown expressions should evaluate to `false` (not crash), with a diagnostic log
- The `visibleIf` property should render nothing (not hide with CSS) when false, to match `Conditional` element behavior

## Open Questions

- Is `enabledIf` alone sufficient, or is `visibleIf` on atomic elements also needed (given that `Conditional` already handles show/hide)?
- Should the expression grammar support nested parentheses (e.g. `(a || b) && c`)?
- Should `FormField.visibleIf` be supported, or should form-field visibility remain a server composition concern?
- What is the acceptable expression evaluation cost per render cycle on low-end Android devices?

## Follow-ups

- [ ] Decide on option and update status to Accepted
- [ ] If Option 4: define formal expression grammar specification
- [ ] If Option 4: prototype expression evaluator on one platform and benchmark
- [ ] If Option 2: verify codegen handles `boolean | string` union across all four targets (Java, Kotlin, Swift, TypeScript)
- [ ] Update `AGENTS.md` if new client-realized semantics are introduced (§7.3 criteria)
