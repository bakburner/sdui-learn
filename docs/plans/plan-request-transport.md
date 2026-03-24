# Plan: Request & Transport Contract

> Source requirements: §9o from sdui-requirements-summary.md, ADR-003, ADR-004

## Summary

Define and implement a formal request envelope schema and transport governance so all platforms send consistent context (platform, locale, auth, device, experiment hints) and the server can make composition decisions with full fidelity. GET/POST routing and cacheability rules must be formalized.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Gap | No typed request envelope schema |
| Server support | Partial | Reads `X-Platform`, `X-Schema-Version`; other context ad hoc |
| Android support | Partial | Sends platform + schema version headers |
| Web support | Partial | Sends platform + schema version headers via proxy |
| Documentation | Partial | Contract doc §11 lists required headers |
| Tests | Gap | No request envelope conformance tests |

## Requirements Addressed

- [ ] **REQ-1**: Typed request envelope schema (platform, locale, timezone, auth context) — §9o
- [ ] **REQ-2**: GET for cacheable screens, POST for personalized screens — ADR-004
- [ ] **REQ-3**: Cache-Control header governance per endpoint type — ADR-004
- [ ] **REQ-4**: Experiment hint header or body field — §9j, ADR-006
- [ ] **REQ-5**: Device context (form factor, screen density, capabilities) — §9o

## Tasks

### Phase 1: Schema & Codegen
- [ ] Define `SduiRequest` schema (JSON Schema for request body/headers)
  - [ ] Required: `platform`, `schemaVersion`
  - [ ] Optional: `locale`, `timezone`, `experimentHints`, `deviceContext`
- [ ] Run codegen for typed request models

### Phase 2: Server
- [ ] Implement request envelope parsing in `SduiController`
- [ ] Use locale/timezone for time-aware composition (game status formatting)
- [ ] Define GET (cacheable, no body) vs POST (personalized, body) route mapping
- [ ] Set appropriate `Cache-Control` headers per route

### Phase 3: Android
- [ ] Build `RequestEnvelopeBuilder` utility — collect platform, locale, timezone, device info
- [ ] Switch to POST for personalized endpoints (e.g., `/sdui/for-you`)
- [ ] Keep GET for cacheable endpoints (e.g., `/sdui/scoreboard`)

### Phase 4: Web
- [ ] Build `RequestEnvelopeBuilder` utility — collect platform, locale from browser APIs
- [ ] Update Express proxy to forward all envelope headers
- [ ] Switch to POST for personalized endpoints

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract §11 with full envelope spec
- [ ] Add request conformance tests (verify all required headers present)
- [ ] Add cache header tests (verify GET routes return `Cache-Control`, POST routes don't)

## Dependencies

- ADR-003 (Composition API Contract) must be accepted to finalize request schema
- ADR-004 (Transport & Caching) must be accepted to finalize GET/POST governance
- ADR-006 (Experiment Assignment) influences experiment hint field shape

## Open Questions

- [ ] Should experiment hints travel as headers or in a POST body?
- [ ] What device context fields are required vs optional?
- [ ] Should the server validate the request envelope strictly or tolerate missing optional fields?
