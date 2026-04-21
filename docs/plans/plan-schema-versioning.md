# Plan: Schema Versioning

> Source requirements: §9f from sdui-requirements-summary.md

## Summary

Implement version negotiation so clients and server can evolve independently. The `X-Schema-Version` header is sent today but the server ignores it — all clients receive the latest schema output. Multi-version response routing and capability negotiation are needed to prevent deserialization errors when clients lag behind server schema evolution.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Partial | `schemaVersion` field in response; no versioning protocol |
| Server support | Partial | Reads `X-Schema-Version` header; always returns latest |
| Android support | Partial | Sends `X-Schema-Version: 1.0`; strict enum deserialization |
| Web support | Partial | Sends `X-Schema-Version: 1.0`; lenient JSON parsing |
| Documentation | Partial | Contract doc §11 mentions header; no versioning protocol |
| Tests | Gap | No version mismatch tests |

## Requirements Addressed

- [ ] **REQ-1**: Server routes responses based on client schema version — §9f
- [ ] **REQ-2**: Capability negotiation for additive schema changes — §9f
- [ ] **REQ-3**: Graceful handling of unknown enum values on strict clients — §9f
- [ ] **REQ-4**: Deprecation protocol for removed fields/types — §9f

## Tasks

### Phase 1: Schema & Codegen
- [ ] Define version negotiation rules: additive-only changes are backward-compatible
- [ ] Add `minSchemaVersion` and `maxSchemaVersion` to server config
- [ ] Document version compatibility matrix
- [ ] Document binding path resilience rules (§3a of requirements) as part of the compatibility matrix — removed fields must not break existing binding declarations

### Phase 2: Server
- [ ] Read `X-Schema-Version` header and route to version-appropriate composer
- [ ] Strip unknown fields when responding to older schema versions
- [ ] Add version mismatch warning header (`X-Schema-Version-Mismatch: true`)

### Phase 3: Android
- [ ] Handle unknown enum values gracefully (fallback to default instead of exception)
- [ ] Log warning on version mismatch header
- [ ] Display update prompt if server requires higher schema version

### Phase 4: Web
- [ ] Handle unknown enum values gracefully (already lenient; add logging)
- [ ] Log warning on version mismatch header

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract §11 with versioning protocol
- [ ] Add version mismatch tests (client sends v1.0, server returns v2.0 fields)
- [ ] Add backward-compatibility tests (client sends v2.0, server returns v1.0-compatible response)

## Dependencies

- Requires a decision on version numbering strategy (semver vs. integer increment)
- Requires catalog of which schema changes are backward-compatible (additive) vs. breaking

## Open Questions

- [ ] Should the server maintain multiple composer versions or strip fields dynamically?
- [ ] How many versions back should the server support (N-1? N-2?)?
- [ ] Should `schemaVersion` use semver (1.0.0) or simple integer (1, 2, 3)?
