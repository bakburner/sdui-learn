# ADR-006: Experiment Assignment Model

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Support both client-provided experiment hints and server-authoritative assignment. When both exist, composer-authoritative assignment wins.

## Context

Teams need short-term flexibility and long-term cross-platform consistency. Fully client-assigned variants risk drift; fully server-assigned variants may require new integrations.

## Model

- Client may send experiment/variant hints.
- Composer may resolve/override assignment via trusted experiment service.
- Response echoes final assignment used for composition.
- Analytics events include final assignment context.

## Why

- Enables incremental rollout without blocking
- Reduces platform drift over time
- Improves trust in reporting and attribution

## Open Questions

- Conflict policy when client hint differs from server assignment
- Assignment caching location and TTL
- Required vs optional assignment fields in request envelope

