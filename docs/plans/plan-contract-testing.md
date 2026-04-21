# Plan: Contract Testing & Golden Fixtures

> Source requirements: Executive Summary gaps, contract doc conformance checklist

## Summary

Automated testing that validates the full SDUI pipeline: server composition output matches schema, client rendering matches expected output, and cross-platform consistency is maintained. Includes golden fixture snapshots for every section type, render-time baseline assertions, and schema conformance checks.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema validation | Partial | Codegen validates at build time; no runtime schema conformance test |
| Golden fixtures | Gap | No snapshot fixtures for section types |
| Server composition tests | Partial | Some integration tests exist; not covering all 42 section variants |
| Android render tests | Gap | No screenshot/snapshot tests for section renderers |
| Web render tests | Gap | No visual regression tests |
| Cross-platform parity | Gap | No assertion that Android and web render the same server response identically |
| Render-time baseline | Gap | No performance baseline or regression detection |

## Requirements Addressed

- [ ] **REQ-1**: Golden fixture for every section type (JSON snapshot of server output) — Exec Summary
- [ ] **REQ-2**: Server composition tests against golden fixtures — Exec Summary
- [ ] **REQ-3**: Client render tests (screenshot/snapshot) for all section types — Exec Summary
- [ ] **REQ-4**: Cross-platform render parity assertion — Exec Summary
- [ ] **REQ-5**: Render-time baseline and regression detection — Exec Summary

## Tasks

### Phase 1: Golden Fixture Generation
- [ ] Create `schema/fixtures/` directory
- [ ] Generate golden JSON fixture for each of the 10 section types from live server output
- [ ] Generate golden JSON fixture for each AtomicComposite variant (from kitchen sink)
- [ ] Version fixtures with a `_fixture_version` field for schema evolution tracking
- [ ] Add `make fixtures` command to regenerate from live server

### Phase 2: Server Composition Tests
- [ ] For each fixture: compose section via builder, assert output matches golden JSON
- [ ] Test that all enum values in server output exist in schema
- [ ] Test error state composition for invalid inputs (bad game ID, missing data)
- [ ] Test platform-specific composition variants (android vs web layout differences)

### Phase 3: Android Render Tests
- [ ] Add Compose Preview screenshot tests for each section type using golden fixture input
- [ ] Assert renders complete without exceptions
- [ ] Measure and record render time per section type
- [ ] Set baseline render-time thresholds (e.g., < 16ms for simple sections, < 50ms for complex)

### Phase 4: Web Render Tests
- [ ] Add Playwright or Storybook visual regression tests for each section type
- [ ] Feed golden fixture JSON into isolated component renders
- [ ] Assert renders match expected screenshots within pixel threshold
- [ ] Measure and record render time per section type

### Phase 5: CI Integration & Cross-Platform
- [ ] Add fixture validation to CI pipeline (schema validation + golden diff)
- [ ] Add render test suites to CI for Android and web
- [ ] Create cross-platform parity report: for each fixture, compare Android and web screenshot
- [ ] Add render-time regression check: fail CI if render time exceeds baseline by > 20%

## Dependencies

- Golden fixtures require running server (or committed static fixtures)
- Android render tests require Compose test infrastructure
- Web render tests require Playwright or similar
- Cross-platform parity requires both platform test suites in the same CI pipeline

## Open Questions

- [ ] Should golden fixtures be committed to the repo or generated in CI?
- [ ] What's the acceptable pixel diff threshold for visual regression?
- [ ] Should render-time baselines be absolute (ms) or relative (% regression from previous)?
- [ ] How to handle platform-specific layout differences in cross-platform parity checks?
- [ ] Should fixtures include real-time update scenarios (before/after Ably message)?
