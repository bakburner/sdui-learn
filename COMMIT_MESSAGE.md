feat: schema versioning — server routing, field stripping, client upgrade prompt

End-to-end schema versioning implementation. Server never emits fields/enums
that a client's declared schemaVersion (major.minor) cannot decode. Clients
detect force-upgrade signal and display platform-appropriate update prompt.

--- Server versioning infrastructure (new) ---

- SchemaVersion: immutable major.minor value object with comparison
- SchemaVersionConfig: Spring @ConfigurationProperties (current-version,
  min-supported-version) in application.yml
- SchemaVersionRegistry: tracks per-field/per-enum introduced-in version
- SchemaVersionFilter: post-composition JSON tree walker strips unsupported
  fields/enums for older clients
- SchemaVersionChecker: below-minimum detection, composes ErrorState
  upgrade-required response, emits X-Schema-Version-Mismatch header
- SduiController: all composition endpoints apply version check + filter
- WebConfig: CORS exposes X-Schema-Version-Mismatch header

--- Client force-upgrade detection ---

- Web: fetchSduiScreen reads header → useSduiScreen exposes upgradeRequired
  → App.tsx renders "Update Required" with reload button
- Android: SduiRepository throws SchemaVersionMismatchException → ViewModel
  emits UpgradeRequired state → SduiScreenContent renders prompt
- iOS: SduiRepository throws .upgradeRequired → ViewModel emits
  .upgradeRequired state → ScreenShell renders prompt

--- Tests ---

- SchemaVersionTest, SchemaVersionFilterTest, SchemaVersionCheckerTest
- SchemaVersionIntegrationTest (field stripping + mismatch header)
- SduiRefreshTransportTest updated with @Import for new beans

--- Documentation ---

- docs/sdui-envelope-spec.md: Schema Version Negotiation section
  (header protocol, field stripping, server config)
- docs/client-implementors-contract.md: §11.6 schema version negotiation
  (force-upgrade detection algorithm, client responsibilities)
- docs/glossary.md: Schema versioning entry added
- docs/sdui-requirements-summary.md: status Partial → Built
- docs/SDUI_Technical_Proposal_v2.md: §10 atomic count 10→12, schema
  versioning row added
- docs/SDUI_Executive_Summary_v2.md: risk table status updated
- docs/plans/server/plan-schema-versioning.md: all phases marked complete
