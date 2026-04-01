# ADR-005: Action Scope and Precedence

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Support actions at screen, section, and subsection/component scope with precedence:

`nested/subsection > section > screen-default`

## Context

Stakeholder feedback requires interaction on subsection elements (for example tapping home team area) and predictable behavior when parent/child actions overlap.

## Contract Rules

- Actions may be defined on:
  - screen defaults (optional)
  - section root
  - nested component/subsection nodes
- For same trigger conflict:
  - nested action takes precedence
  - section acts as fallback
  - screen-default is last fallback

## Sequence Behavior

- Multiple actions for one trigger execute in declared order.
- `fireAndForget`-before-`navigate` is recommended default sequencing.

## Open Questions

- Whether parent and child action arrays can be explicitly merged
- How to report action resolution in debug/telemetry output

