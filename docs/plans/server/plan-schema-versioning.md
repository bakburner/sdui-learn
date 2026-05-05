# Plan: Schema Versioning (Server-Side)

> Source requirements: §9f from sdui-requirements-summary.md

## Summary

Version-aware composition ensures the server never emits values that a client's declared `schemaVersion` cannot decode. The server reads `schemaVersion` (major.minor format) from the request envelope, strips fields/enums introduced after the client's version, and signals force-upgrade when a client falls below the minimum supported version.

Per AGENTS.md §1.3, strict decoding is intentional — unknown enum values for closed wire shapes are contract violations. The correct defense is server-side: the server must not send values the client's declared version doesn't support.

## Status: Complete

All requirements implemented and tested.

- [x] **REQ-1**: Server routes responses based on client schema version
- [x] **REQ-2**: Compatibility matrix defines additive vs. breaking changes
- [x] **REQ-3**: Mismatch/force-upgrade signal when client is too far behind

## Design Principle

> **Server owns the responsibility.** Clients decode strictly (§1.3). The server
> must never emit enum values, field names, or structural shapes that the
> client's declared `schemaVersion` does not define. Version routing is a
> server concern, not a client resilience opportunity.

## Implementation

### Phase 1: Compatibility Matrix & Config
- [x] Version numbering: major.minor (major = breaking, minor = additive)
- [x] Current schema cataloged as version `1.0`
- [x] `sdui.schema.min-supported-version` and `sdui.schema.current-version` in `application.yml`
- [x] Change types classified: additive (new optional fields, new section types) vs. breaking (new required fields, new enum values for closed shapes, removed fields)

### Phase 2: Server Version Routing
- [x] `SchemaVersionFilter` strips fields/enum values introduced after client's declared version
- [x] `SchemaVersionChecker` returns `X-Schema-Version-Mismatch: upgrade-required` + ErrorState when below minimum
- [x] `SchemaVersionRegistry` tracks per-field/per-enum introduced-in version
- [x] All composition endpoints apply version check and filter

### Phase 3: Force-Upgrade Signal (Client Work)
- [x] Web: `fetchSduiScreen` reads header → `useSduiScreen` exposes `upgradeRequired` → App renders update prompt
- [x] Android: `SduiRepository` throws `SchemaVersionMismatchException` → ViewModel emits `UpgradeRequired` state → Composable renders prompt
- [x] iOS: `SduiRepository` throws `.upgradeRequired` → ViewModel emits `.upgradeRequired` state → ScreenShell renders prompt

### Phase 4: Documentation & Tests
- [x] `docs/sdui-envelope-spec.md` updated with version negotiation protocol
- [x] Client Implementor's Contract updated with versioning expectations
- [x] Integration tests: field stripping, mismatch header, backward-compatibility subset

## Design Decisions

| Decision | Resolution |
|----------|------------|
| Version format | major.minor (e.g. `1.0`, `1.1`, `2.0`) |
| Registry approach | Per-field/per-enum "introduced in version" registry (`SchemaVersionRegistry`) |
| Support window | N-1 for prototype |
| Stripping approach | Dynamic JSON tree-walk post-composition (`SchemaVersionFilter`) |

## Key Files

| File | Purpose |
|------|--------|
| `server/src/main/java/com/nba/sdui/versioning/SchemaVersion.java` | Immutable major.minor value object with comparison |
| `server/src/main/java/com/nba/sdui/versioning/SchemaVersionConfig.java` | Spring config properties (`sdui.schema.*`) |
| `server/src/main/java/com/nba/sdui/versioning/SchemaVersionRegistry.java` | Tracks which fields/enums were introduced per version |
| `server/src/main/java/com/nba/sdui/versioning/SchemaVersionFilter.java` | Post-composition JSON tree walker that strips unsupported fields |
| `server/src/main/java/com/nba/sdui/versioning/SchemaVersionChecker.java` | Below-minimum detection + ErrorState composition |
| `server/src/main/resources/application.yml` | `sdui.schema.current-version` / `min-supported-version` |
