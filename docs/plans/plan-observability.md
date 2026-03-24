# Plan: Observability & Visual Debugging

> Source requirements: §9l from sdui-requirements-summary.md

## Summary

Runtime observability for the SDUI pipeline: trace IDs for request correlation, visual debug overlay for inspecting section metadata on-device, performance metrics collection, and dashboards. The server already emits `traceId` in responses; clients need to surface it and build tooling around it.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Server traceId | Built | `traceId` included in every SDUI response |
| Android observability | Gap | No debug overlay, no metrics collection, traceId logged but not surfaced |
| Web observability | Gap | No debug overlay, no dev-tools panel |
| Dashboards | Gap | No Grafana/Datadog dashboards for SDUI pipeline |
| Documentation | Partial | Contract doc mentions traceId; no observability guide |
| Tests | Gap | No assertion that traceId propagates end-to-end |

## Requirements Addressed

- [ ] **REQ-1**: Visual debug overlay (tap/click to inspect section metadata) — §9l
- [ ] **REQ-2**: Request/response tracing with traceId correlation — §9l
- [ ] **REQ-3**: Performance metrics (render time per section, network latency, cache hit rate) — §9l
- [ ] **REQ-4**: Operational dashboard for SDUI health — §9l

## Tasks

### Phase 1: Debug Overlay — Android
- [ ] Create `SduiDebugOverlay` composable (activated via shake gesture or dev menu)
- [ ] On section long-press: show popup with section type, section ID, traceId, refresh policy, data bindings
- [ ] Show real-time channel status (connected/disconnected/error) per active SSE subscription

### Phase 2: Debug Overlay — Web
- [ ] Create `SduiDebugPanel` React component (activated via keyboard shortcut or URL param `?sdui-debug=1`)
- [ ] On section hover+Alt: show tooltip with section metadata
- [ ] Show network waterfall: fetch timing, channel open/close events

### Phase 3: Metrics Collection
- [ ] Instrument section render time (start→end of composable/component render per section)
- [ ] Instrument network latency per `fetchScreen` call
- [ ] Instrument cache hit/miss rate
- [ ] Instrument real-time message processing time (Ably message received → UI updated)
- [ ] Emit metrics to a pluggable sink (console in dev, external collector in prod)

### Phase 4: Server Dashboard
- [ ] Define key metrics: p50/p95/p99 composition time, error rate by section type, cache hit rate
- [ ] Create Grafana dashboard template (or equivalent) with SDUI-specific panels
- [ ] Add server-side metrics for composition time per section type
- [ ] Add alert rules: composition error rate > 1%, p99 > 500ms

### Phase 5: Documentation & Tests
- [ ] Document debug overlay activation and usage in Client Implementor's Contract
- [ ] Document metrics schema (metric name, dimensions, units)
- [ ] Add test: traceId from server response is logged on client
- [ ] Add test: debug overlay renders section metadata accurately

## Dependencies

- traceId already in server responses (no server changes for Phase 1-2)
- Metrics sink is pluggable — no hard dependency on specific observability stack

## Open Questions

- [ ] Which observability stack? (Grafana + Prometheus, Datadog, New Relic, custom?)
- [ ] Should debug overlay be available in release builds behind a feature flag?
- [ ] Should metrics be sampled in production (e.g., 10% of sessions)?
- [ ] Should the debug overlay show the raw JSON of a section on tap?
