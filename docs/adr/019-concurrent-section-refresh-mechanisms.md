# ADR-019: Concurrent section refresh mechanisms

- Status: Accepted
- Date: 2026-06-09
- Decision owners: Backend, Cross-Platform
- Related requirements: `AGENTS.md` §3.3, §3.8; `docs/plans/server/plan-live-now-per-game-sse-sections.md`
- Related ADRs: ADR-016 (Update channel unification), ADR-017 (Transport-framing exception)

## Decision

`Section.refreshPolicy` is a bounded `RefreshPolicy[]` (`maxItems: 2`) that
allows one opaque mechanism (`sse channel` or `poll url`) and one section-refresh
mechanism (`poll sectionEndpoint`) to run concurrently, while
`Screen.defaultRefreshPolicy` stays a single `RefreshPolicy` object.

## Context

Games-screen lifecycle requirements need a section to stream volatile fields
(score/clock) and independently reconcile low-volatility fields / status
transitions. A live game card must run both:

- continuous SSE (`channel`) for in-place `dataBinding` patches, and
- periodic section recomposition (`sectionEndpoint`) for full replacement.

An earlier attempt added `DataBinding.channels` (multi-channel fan-out per
section). That approach was reverted because it moved transport ownership away
from `refreshPolicy`, increased cross-platform complexity, and introduced
un-schema'd behavior pressure around binding/transport boundaries.

At the same time, overloading a single `RefreshPolicy` object to carry both
`channel` and `sectionEndpoint` would make `type` ambiguous and force every
client to reinterpret existing `type` branching.

## Decision Drivers

- Keep refresh transport ownership in `refreshPolicy`, not `dataBinding`.
- Make concurrent behavior explicit on the wire, not implicit in overloaded
  `type` semantics.
- Preserve existing per-policy-kind client logic by iterating elements rather
  than redefining one element's meaning.
- Keep section and screen refresh semantics asymmetric by design: section-level
  concurrency is required; screen-level default remains one full-replace policy.
- Align section-channel failure behavior across platforms.

## Options Considered

### Option A: Reintroduce `DataBinding.channels` fan-out

Pros:
- No `Section.refreshPolicy` shape change required.
- Multiple subscriptions per section are directly representable.

Cons:
- Violates ownership boundary (`dataBinding` maps payloads; it does not own
  transport).
- Reintroduces fan-out complexity on all clients for one use case.
- Expands contract surface in the wrong place and weakens doctrine clarity.

### Option B: Overload one `RefreshPolicy` object with mixed fields

Pros:
- Avoids changing `Section.refreshPolicy` from object to array.
- Minimal schema shape churn.

Cons:
- `type` no longer faithfully describes behavior.
- Existing client `type` branches become easy to misuse and hard to reason about.
- Hidden concurrency invites future regressions.

### Option C: Bounded `Section.refreshPolicy[]` with explicit invariants (chosen)

Pros:
- Concurrency is explicit and bounded on the wire.
- Existing per-kind client logic composes naturally by iterating elements.
- `dataBinding` remains section-level and unambiguous (single opaque source).
- Supports live-card lifecycle (SSE + section refresh) without new channels.

Cons:
- Global schema shape migration for all section emitters and client decoders.
- Requires server-side cross-element validation beyond Draft-07.

## Evidence

- Existing clients already branch independently for `poll` and `sse`; iterating
  elements reuses that logic with less semantic reinterpretation risk.
- Draft-07 can enforce `maxItems` and element schemas, but cannot cleanly enforce
  cross-element constraints like "≤1 opaque + ≤1 section-refresh + static solo."
- Games-screen lifecycle specifically needs concurrent stream + reconcile behavior
  to move pregame -> live -> final without screen-level polling.

## Decision Outcome

Option C is adopted.

`Section.refreshPolicy` becomes a bounded array with these invariants:

- At most one opaque element:
  - `type: "sse"` + `channel`, or
  - `type: "poll"` + `url` + `intervalMs`.
- At most one section-refresh element:
  - `type: "poll"` + `sectionEndpoint` + `intervalMs`.
- `type: "static"` is terminal and solo.

`Screen.defaultRefreshPolicy` remains a single object. Screen-channel semantics
stay full-replace.

Section-channel `404` handling is unified: clients mark the section stale, stop
that section's poll, and retain the section node on screen.

## Consequences

- Short term:
  - All section refresh-policy emitters move from single object to array shape.
  - All clients decode and orchestrate section refresh by iterating array
    elements with defensive warn+ignore behavior for invalid extras.
  - Web's prior section-removal path on section `404` is removed in favor of
    stale+retain.
- Medium term:
  - Per-game cards can stream scores while reconciling status and low-volatility
    fields on independent cadences.
  - Cross-platform refresh behavior converges around one bounded model.
- Long term:
  - The contract keeps section concurrency explicit while containing growth
    pressure with fixed cardinality and server validation.
  - Additional section lifecycles can reuse the same model without inventing a
    third update channel.

## Implementation Notes

- Schema:
  - `Section.refreshPolicy` is `RefreshPolicy[]` with `maxItems: 2`.
  - `Screen.defaultRefreshPolicy` stays `RefreshPolicy` (single object).
- Validation:
  - Keep Draft-07 validation for shape/type/enum.
  - Add server-side cross-element validation for bounded-array invariants.
- Runtime:
  - `dataBinding` remains section-level and applies only to opaque payloads.
  - `sectionEndpoint` responses replace full sections and do not consume
    `dataBinding`.
- Channel model:
  - No new update channel is introduced; section concurrency still runs through
    the existing section channel.

## Open Questions

- Should the cross-element invariant validator remain server-only, or should a
  schema-generation step add machine-readable annotation metadata for tooling?
- If future requirements demand more than two concurrent mechanisms, should the
  contract expand cardinality or introduce a distinct typed sub-structure?

## Follow-ups

- [ ] Keep AGENTS and implementor contracts aligned with bounded-array language.
- [ ] Ensure contract tests assert array invariants and static-solo behavior.
- [ ] Ensure all clients implement unified section `404` stale+retain behavior.
