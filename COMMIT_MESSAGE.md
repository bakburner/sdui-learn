SDUI: Move market cohort from edge header to envelope query string

--- market[cohort] as composition input (all platforms + server) ---

- Server: SduiRequestContext gains nested Market class with cohort field
  (default "MARKET_UNKNOWN"), initialized eagerly to avoid NPE; removed
  X-Resolved-Country and X-Resolved-Market-Cohort header reading from
  BracketParamResolver
- Android: RequestEnvelopeBuilder emits market[cohort] in query string
  and POST body; default "MARKET_UNKNOWN"
- iOS: RequestEnvelope gains marketCohort property; emitted in query
  string and POST body; tests updated
- Web: RequestEnvelopeBuilder emits market[cohort] in query string and
  POST body; Market interface added

--- Trust model change ---

- Market cohort trust shifts from edge IP resolution to app attestation
  (Play Integrity / App Attest). Requests failing attestation receive
  MARKET_UNKNOWN treatment. Edge worker geo resolution removed from
  roadmap.

--- Documentation ---

- docs/sdui-envelope-spec.md: market[cohort] field added to spec,
  trust model rewritten, edge worker scope narrowed, changelog updated
- All client builders document fixed envelope ordering for CDN cache
  key determinism

--- Unrelated (included) ---

- docs/plans/plan-server-section-caching.md: new plan for three-layer
  server-side caching (upstream data, section fragments, screen assembly)
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
