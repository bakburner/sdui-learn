# Plan: Experimentation & A/B Testing

> Source requirements: §9j from sdui-requirements-summary.md, ADR-006

## Summary

The client is fully authoritative for experiment assignments. Clients resolve assignments (via Amplitude or any future experiment SDK) at app start and send them as `experiments[experimentId]=variantName` in the request envelope. The server trusts these assignments and uses them for composition branching — it has no kill switch, no echo, and no exposure logging. Kill switches are client-side: the client simply stops sending a disabled experiment. Exposure events are tracked client-side via fire-and-forget actions.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Request envelope | Done | `experiments` map in `SduiRequestContext`, bracket-notation transport (plan-request-transport) |
| Server variant resolution | Done | `resolveVariant(experimentId, default)` on `SduiRequestContext`; experiment constants in `SduiCompositionService` |
| Android envelope | Done | `RequestEnvelopeBuilder.experiments()` sends map via bracket-notation GET |
| Web envelope | Done | `RequestEnvelopeBuilder.experiments()` sends map via bracket-notation GET |
| Variant → experiments migration | Done | `variant` param removed; `experiments[variant]` placeholder retains existing functionality |
| Exposure tracking | Done | Fire-and-forget action infrastructure exists on all clients |
| Documentation | Partial | ADR-006 needs update to reflect simplified client-authoritative model |
| Tests | Deferred | Demo server — tracked in §10 Status Matrix |

## Decisions

### D1: Client is fully authoritative

**Decision:** The client owns all experiment assignment, kill switch, and exposure tracking responsibilities. The server is a pure consumer of the experiments map — it reads assignments and branches composition, nothing more.

- **Assignment:** Client resolves via experiment SDK (Amplitude or other) at app start.
- **Kill switch:** Client-side. To disable a variant, the client stops sending that experiment in the envelope. Server never sees it, falls back to default.
- **Exposure tracking:** Client-side via fire-and-forget actions. No server-side exposure logging — tracking once is sufficient.
- **No response echo:** Server does not echo assignments back. The client already knows what it sent.

### D2: Per-session assignment

**Decision:** Experiment assignments are resolved once at app start and persist for the session. The same assignments are sent on every request during that session.

### D3: Experiments are part of the cache key

**Decision:** Because experiments travel as query parameters (`experiments[exp_id]=variant_b`), they are naturally part of the URL and therefore part of the CDN cache key. Different assignments produce different cache entries.

### D4: `experiments[variant]` replaces `variant` query param

**Decision:** The legacy `variant` query param is removed. Existing variant functionality is retained via `experiments[variant]` — a placeholder experiment key that maps to the same server-side resolution path. This exercises the real experiment code path even for development/demo variant switching.

## Requirements Addressed

- [x] **REQ-1**: Client-authoritative experiment assignment — §9j, ADR-006
- [x] **REQ-2**: Experiment assignments in request envelope — §9j
- [x] **REQ-3**: Exposure logging via fire-and-forget actions (client-side) — §9j

## Tasks

### Phase 1: Server (Done)
- [x] ~~`experiments` map in `SduiRequestContext`~~ — done in plan-request-transport
- [x] ~~`resolveVariant(experimentId, default)` reads experiments map~~ — done in plan-request-transport
- [x] ~~`variant` param removed; `experiments[variant]` used as placeholder~~ — done in plan-request-transport

### Phase 2: Android (Done)
- [x] ~~`RequestEnvelopeBuilder.experiments()` sends map~~ — done in plan-request-transport

### Phase 3: Web (Done)
- [x] ~~`RequestEnvelopeBuilder.experiments()` sends map~~ — done in plan-request-transport

### Phase 4: Documentation
- [ ] Update ADR-006 to reflect simplified model (no server kill switch, no echo, no server exposure logging)
- [ ] Accept ADR-006 (change status from Proposed to Accepted)
- [ ] Update §9j in requirements summary with resolved decisions
- [ ] Update §10 Status Matrix: A/B testing row Partial → Built

## Dependencies

None — all code work is complete. Only documentation remains.

## Open Questions

- [x] Which experiment service will be used? **Decision: Amplitude. Client is authoritative (D1). SDK integration deferred — not part of this plan.**
- [x] Should experiment assignment be per-screen or per-session? **Decision: Per-session — resolved at app start (D2).**
- [x] How does experiment assignment interact with caching? **Decision: Experiments are query params = natural cache key (D3).**
