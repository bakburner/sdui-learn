# ADR-002: Composition Ownership and Transition

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

SDUI semantic composition is owned by the SDUI composer/aggregator. CoreAPI-derived composition may be used as a transitional input only when necessary.

## Context

Current clients implement platform-specific card/module interpretation. This causes contract drift, duplicated logic, and inconsistent behavior for logically equivalent cards.

## Options

### Option A: Keep composition primarily in existing CoreAPI/card pipelines

Pros:
- Lower short-term backend investment
- Reuses current logic paths

Cons:
- Preserves coupling and cross-platform drift
- Slows semantic contract stabilization

### Option B: Composer-owned semantics (recommended)

Pros:
- Single semantic source of truth
- Clear governance and versioning path
- Better cross-platform consistency

Cons:
- Requires composition-service maturity
- Temporary transition complexity

## Outcome

Adopt Option B. Use CoreAPI adapters only as tactical bridges for parity gaps and retire them per surface when composer-native logic is ready.

## Transition Notes

- Avoid introducing new long-term features on legacy composition unless required.
- Evaluate legacy reuse case-by-case.
- Track adapter usage and burn-down by screen/surface.

