# Plan: Ad Support

> Source requirements: §9n from sdui-requirements-summary.md, ADR-007

## Summary

Formalize the `AdSlot` section type with a complete contract: provider configuration, targeting keys, size definitions, fallback/collapse behavior, and refresh policy. The `AdSlot` renderer exists on both platforms but the ad SDK integration is placeholder — no real ad requests are made yet.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Partial | `AdSlotData` exists with basic fields; no formal fallback/targeting contract |
| Server support | Partial | Composers create AdSlot sections; no targeting key injection |
| Android support | Partial | `AdSlotRenderer` renders placeholder; no GAM/ad SDK integration |
| Web support | Partial | `AdSlot` renders placeholder; no GPT/ad SDK integration |
| Documentation | Partial | AGENTS.md §15 classifies AdSlot as permanent (SDK integration) |
| Tests | Gap | No ad lifecycle tests |

## Requirements Addressed

- [ ] **REQ-1**: Formalize `AdSlotData` schema (provider, unitPath, sizes, targeting, fallback) — §9n
- [ ] **REQ-2**: Define collapse-on-empty behavior (hide section when no fill) — §9n, ADR-007
- [ ] **REQ-3**: Ad refresh policy (time-based refresh, viewability tracking) — §9n
- [ ] **REQ-4**: Android GAM SDK integration — §9n
- [ ] **REQ-5**: Web GPT SDK integration — §9n

## Tasks

### Phase 1: Schema & Codegen
- [ ] Extend `AdSlotData` with: `provider`, `unitPath`, `sizes[]`, `targeting: Map`, `fallbackBehavior: "collapse" | "placeholder"`, `refreshIntervalMs`
- [ ] Run codegen

### Phase 2: Server
- [ ] Inject targeting keys from composition context (content type, game state, user segment)
- [ ] Set ad-specific `refreshPolicy` (ad refresh interval ≠ section refresh)

### Phase 3: Android
- [ ] Integrate Google Ad Manager (GAM) SDK in `AdSlotRenderer`
- [ ] Implement ad lifecycle: load → display → refresh → collapse-on-empty
- [ ] Fire `onVisible` impression when ad fills

### Phase 4: Web
- [ ] Integrate Google Publisher Tag (GPT) in `AdSlot` component
- [ ] Implement ad lifecycle: defineSlot → display → refresh → destroy
- [ ] Handle collapse-on-empty via container height animation

### Phase 5: Documentation & Tests
- [ ] Document ad contract in schema examples
- [ ] Update Client Implementor's Contract Phase 4 with ad SDK integration notes
- [ ] Add ad lifecycle tests (load, fill, no-fill collapse, refresh)

## Dependencies

- ADR-007 (Ads Boundary & Contract) must be accepted to finalize schema fields and auction boundary
- Ad SDK credentials required for both platforms

## Open Questions

- [ ] Does the SDUI server own targeting key injection, or does the client send targeting context?
- [ ] Should ad refresh be server-controlled (via `refreshPolicy`) or SDK-controlled?
- [ ] What is the fallback UX when ad SDK is unavailable (e.g., ad blocker)?
