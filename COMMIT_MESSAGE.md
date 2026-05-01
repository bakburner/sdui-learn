SDUI: Envelope Spec v1 implementation + doc consistency audit

--- Envelope Spec v1 (server + all clients) ---

- Server: /v1/ URL prefix on all composition routes, BracketParamResolver
  reads X-Request-Id into MDC, removed X-Schema-Version header fallback,
  SduiRequestContext.Platform reduced to deviceClass + capabilities only,
  top-level resolvedCountry/resolvedMarketCohort from edge headers
- Android: RequestEnvelopeBuilder emits only locale/schemaVersion/
  deviceClass/capabilities/experiments as query params; SduiRepository
  sends X-Trace-Id, X-Request-Id, X-Device-Id, X-Platform, X-App-Version,
  X-OS-Version, X-Resolved-Country, X-Resolved-Market-Cohort, Authorization
- iOS: Same envelope shape; SduiRepository sends same header set;
  UriResolver sduiPrefix = "/v1/sdui/"
- Web: Same envelope shape; fetchSduiScreen sends same header set;
  SDUI_PATH_PREFIX = '/v1/sdui/'

--- Envelope contract documentation ---

- Created docs/sdui-envelope-spec.md — full envelope contract reference
  covering versioning layers, URL shape, query fields, excluded fields,
  GET/POST fallback, percent-encoding, deterministic ordering, headers,
  edge-injected headers, trust model, server contract, caching, roadmap

--- Doc consistency audit (16 inconsistencies fixed) ---

- AGENTS.md §3.4: Rewrote platform identity section (X-Platform header +
  deviceClass/capabilities query params); §4.1.1 bracket params updated
- README.md: All curls → /v1/sdui/, screens table updated, schemaVersion
  as query param instead of X-Schema-Version header
- docs/SDUI_Executive_Summary_v2.md: Envelope fields and platform-aware
  composition description
- docs/SDUI_Technical_Proposal_v2.md: URI resolution convention, endpoint
  JSON examples
- docs/sdui-requirements-summary.md: Transport decisions, mermaid diagram,
  dual-mounted route, schema versioning mechanism
- docs/client-implementors-contract.md: Curl example, UriResolver, §11
  wire shape table, headers table, conformance C11, pseudocode, cache key
- docs/glossary.md: Envelope definition updated
- docs/adr/003-composition-api-contract.md: Required inputs list
- prompts/agents/client-builder.agent.md: Platform identity rule
- docs/plans/plan-aggregation-demo-features.md: Cache key formula

--- Test updates ---

- All platform envelope/transport tests updated for new query shape,
  /v1/sdui/ prefix, and header assertions
- Server: SduiRefreshTransportTest updated
- iOS: RequestEnvelopeBuilderTests, SduiRepositoryRefreshTransportTests,
  UriResolverTests, ActionDispatcherTests, PollingDriverTests
- Android: SduiRepositoryRefreshTransportTest
- Web: fetchSduiScreen.test.ts (176/176 pass)
