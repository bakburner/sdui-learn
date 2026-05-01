# ADR-003: Composition API Contract (Request/Response)

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Define a typed composition request envelope and versioned response contract; treat this as a first-class platform API with backward compatibility rules.

## Context

Composition requires deterministic inputs (screen/entity, platform, locale, capabilities, auth context, experiments). Current ad hoc request variations increase drift and ambiguity.

## Required Request Inputs

- Screen/entity identifiers
- deviceClass / capabilities (composition inputs, query params)
- Locale (query param)
- schemaVersion (query param)
- Experiments map (query params)
- Trace ID (`X-Trace-Id` header)
- Request ID (`X-Request-Id` header)
- Platform (`X-Platform` header, analytics only)
- Auth context via `Authorization` header

## Auth Decision

- JWT/session token is passed via `Authorization` header.
- Tokens are not passed in query params.

## Response Contract

- Return explicit `schemaVersion`
- Return final experiment/variant context used for composition
- Include trace correlation IDs
- Preserve backward compatibility for additive changes

## Open Questions

- Canonical field names and required/optional matrix
- Error shape and partial composition behavior
- Compatibility window for older client versions

