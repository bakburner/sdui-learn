# ADR-007: Ads Boundary and Contract

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Ad auction and targeting remain delegated to ad platform/SDK boundaries. SDUI carries ad placement semantics as contract data.

## Context

Stakeholders requested clear ad support in SDUI without overloading composition with ad-network-specific business logic.

## Contract Direction

- Represent ads as first-class placement primitives (for example `AdSlot`).
- Minimum fields:
  - placement identifier
  - slot type/size
  - refresh behavior
  - fallback/no-fill policy
- Keep ad-network internals outside SDUI schema where possible.

## Why

- Maintains separation of concerns
- Keeps SDUI portable across platforms/ad providers
- Avoids hardcoding ad auction logic in composition layer

## Open Questions

- Mandatory metadata set for all platforms
- Fallback behavior standardization
- Which targeting hints are allowed in SDUI contract vs resolved elsewhere

