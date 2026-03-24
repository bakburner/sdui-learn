# Plan: Internationalization & String Resolution

> Source requirements: §9p from sdui-requirements-summary.md

## Summary

The current SDUI pipeline resolves all strings server-side — the server composes localized text directly into section payloads. This is the settled default for static content. The gap is `stringKeys` on data bindings: the ability for real-time data updates to reference externally translated strings (e.g., Ably sends a status code; the client resolves it to a localized label).

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Server-resolved strings | Built | All text in server responses is pre-localized |
| Client string resolution | Gap | `stringKeys` field on data bindings not implemented |
| Schema support | Gap | No `stringKeys` field in DataBinding schema |
| Android support | Gap | DataBindingResolver applies raw values only |
| Web support | Gap | DataBindingResolver applies raw values only |
| Documentation | Partial | Contract doc §6 mentions server-resolved strings; no stringKeys spec |

## Requirements Addressed

- [ ] **REQ-1**: `stringKeys` mapping on data bindings (real-time value → localized string) — §9p
- [ ] **REQ-2**: Client-side string resolution from a string table — §9p
- [ ] **REQ-3**: Fallback chain (stringKey → server-resolved default → raw value) — §9p
- [ ] **REQ-4**: RTL layout support where applicable — §9p

## Tasks

### Phase 1: Schema & Codegen
- [ ] Add `stringKeys: Map<String, String>` to `DataBinding` (maps target path → string key)
- [ ] Add `stringTable: Map<String, String>` to screen-level response (localized string map)
- [ ] Run codegen

### Phase 2: Server
- [ ] Populate `stringTable` with localized strings for dynamic statuses (e.g., `"status.final" → "Final"`)
- [ ] Set `stringKeys` on data bindings where real-time messages send status codes, not display strings
- [ ] Accept `Accept-Language` header and compose `stringTable` in the requested locale

### Phase 3: Android
- [ ] Extend `DataBindingResolver.applyBindings()` to check `stringKeys` for each target path
- [ ] If stringKey exists: look up value in `stringTable`, fall back to raw value if not found
- [ ] Log warning when stringKey lookup fails (per AGENTS.md §12)

### Phase 4: Web
- [ ] Extend `DataBindingResolver.applyBindings()` with same stringKey lookup logic
- [ ] Wire `stringTable` from screen response into resolver context

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract with stringKey resolution algorithm
- [ ] Add pseudocode to DataBindingResolver algorithm in contract doc
- [ ] Add test: Ably message with status code → stringKey resolves to localized string
- [ ] Add test: missing stringKey → falls back to raw value

## Dependencies

- Depends on DataBindingResolver (already implemented on both platforms)
- Depends on server locale detection (Accept-Language header parsing)

## Open Questions

- [ ] Should `stringTable` be screen-level or section-level? (Screen-level avoids duplication)
- [ ] Should the string table be cacheable separately from section data?
- [ ] How does RTL layout interact with AtomicComposite container direction?
- [ ] Should stringKeys support parameterized strings (e.g., `"wins.format" → "{0} Wins"`)?
