# Plan: Experimentation & A/B Testing

> Source requirements: §9j from sdui-requirements-summary.md, ADR-006

## Summary

Finalize the experiment assignment flow so the server can compose different screen variants based on experiment group membership. The variant mechanism works (A/B/C/D variants demonstrated), but the integration point between the experimentation service and the composition service is not formalized.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Built | `variant` query parameter supported |
| Server support | Partial | Supports variant-based composition; no experiment service integration |
| Android support | Partial | Sends variant parameter; no experiment SDK integration |
| Web support | Partial | Sends variant parameter; no experiment SDK integration |
| Documentation | Partial | ADR-006 proposes server-authoritative model |
| Tests | Gap | No experiment assignment tests |

## Requirements Addressed

- [ ] **REQ-1**: Server-authoritative experiment assignment (server decides variant) — §9j, ADR-006
- [ ] **REQ-2**: Client-hint experiment context (client sends experiment SDK state) — §9j
- [ ] **REQ-3**: Conflict resolution when client and server disagree — ADR-006
- [ ] **REQ-4**: Exposure logging for experiment analytics — §9j

## Tasks

### Phase 1: Schema & Codegen
- [ ] Add `experimentAssignments` field to response metadata (which experiments are active)
- [ ] Add `experimentHint` to request envelope (client-side experiment SDK state)

### Phase 2: Server
- [ ] Define experiment service integration interface
- [ ] Implement variant resolution: experiment service → variant selection → composition
- [ ] Log experiment exposure events for analytics

### Phase 3: Android
- [ ] Send experiment SDK state as `experimentHint` in request envelope
- [ ] Log experiment exposure from response metadata

### Phase 4: Web
- [ ] Send experiment SDK state as `experimentHint` in request envelope
- [ ] Log experiment exposure from response metadata

### Phase 5: Documentation & Tests
- [ ] Accept ADR-006 with finalized conflict resolution rules
- [ ] Add experiment variant tests (same user → consistent variant across requests)
- [ ] Update `docs/sdui-requirements-summary.md` status: §9j Partial → Built

## Dependencies

- ADR-006 (Experiment Assignment) must be accepted
- Experiment service API must be defined (external dependency)

## Open Questions

- [ ] Which experiment service will be used (internal, LaunchDarkly, Optimizely)?
- [ ] Should experiment assignment be per-screen or per-session?
- [ ] How does experiment assignment interact with caching (different variants = different cache keys)?
