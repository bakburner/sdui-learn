# ADR-009: Impression Dedup and Visibility Semantics

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Track impression-dedup and visibility semantics as a dedicated cross-platform decision set under one ADR.

## Context

Impression behavior is currently split between recommendation notes and open questions. These choices affect analytics correctness across mobile, TV, and web and require platform/backend alignment.

## Scope

- TV `onFocus` behavior (`impression` vs separate `browse` event)
- Dwell timer behavior when modal overlays section (`pause` vs `cancel`)
- Nested scroll visibility math (compound vs parent-relative)
- Dispatch strategy (individual beacons vs batched)

## Current Direction (Pending Approval)

- Keep `onFocus` as a separate browse event by default
- Pause dwell timer when covered by modal
- Start with parent-relative visibility; iterate if analytics gaps appear
- Fire beacons individually for v1 simplicity

## Why ADR

These are not implementation details; they define analytics semantics and reporting quality, and must be stable and auditable across platforms.

