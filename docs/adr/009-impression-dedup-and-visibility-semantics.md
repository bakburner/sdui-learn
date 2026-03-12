# ADR-009: Impression Dedup and Visibility Semantics

- Status: Accepted
- Date: 2026-02-20
- Accepted: 2026-03-11
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Impression tracking implemented at the router level using `IntersectionObserver` (web) with the following semantics:

- **Visibility detection:** `IntersectionObserver` with 50% threshold (browser-native, parent-relative)
- **Dwell time:** 1000ms default before impression fires
- **Dedup strategies:** `once-per-screen` (default), `once-per-interval`, `none` — server-defined per analytics action
- **Dispatch:** Individual beacons per section (no batching in v1)
- **Scope:** Router-level — every section type (including future/unknown) gets tracking automatically

## Context

Impression behavior is currently split between recommendation notes and open questions. These choices affect analytics correctness across mobile, TV, and web and require platform/backend alignment.

## Scope

- TV `onFocus` behavior (`impression` vs separate `browse` event)
- Dwell timer behavior when modal overlays section (`pause` vs `cancel`)
- Nested scroll visibility math (compound vs parent-relative)
- Dispatch strategy (individual beacons vs batched)

## Accepted Decisions

- Keep `onFocus` as a separate browse event by default
- Start with parent-relative visibility (browser-native `IntersectionObserver`); iterate if analytics gaps appear
- Fire beacons individually for v1 simplicity
- Pause on modal overlay not implemented in v1 (noted as follow-up)

## Why ADR

These are not implementation details; they define analytics semantics and reporting quality, and must be stable and auditable across platforms.

