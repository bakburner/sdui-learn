# Plan: Internationalization & String Resolution

> Source requirements: §9p from sdui-requirements-summary.md

## Summary

The current SDUI pipeline resolves all strings server-side — the server composes localized text directly into section payloads. This is the settled default for static content. The gap is `stringKeys` on data bindings: the ability for real-time data updates to reference externally translated strings (e.g., Ably sends a status code; the client resolves it to a localized label).

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Server-resolved strings | Built | All text in server responses is pre-localized |
| Locale query parameter | Built | `?locale=es` on GET, defaults to `en` |
| Screen-level stringTable | Built | Server populates per-locale stringTable on every screen response |
| Client string resolution | Built | DataBindingResolver resolves stringKeys via stringTable |
| Schema support | Built | `stringKeys` on DataBinding, `stringTable` on Screen |
| Android support | Built | DataBindingResolver.applyBindings() resolves stringKeys |
| Web support | Built | applyDataBindings() resolves stringKeys |
| Documentation | Partial | Contract doc §6 mentions server-resolved strings; no stringKeys spec |

## Requirements Addressed

- [x] **REQ-1**: `stringKeys` mapping on data bindings (real-time value → localized string) — §9p
- [x] **REQ-2**: Client-side string resolution from a string table — §9p
- [x] **REQ-3**: Fallback chain (stringKey → server-resolved default → raw value) — §9p
- [ ] **REQ-4**: RTL layout support where applicable — §9p

## Tasks

### Phase 1: Schema & Codegen
- [x] Add `stringKeys: Map<String, String>` to `DataBinding` (maps target path → string key)
- [x] Add `stringTable: Map<String, String>` to screen-level response (localized string map)
- [ ] Run codegen

### Phase 2: Server
- [x] Populate `stringTable` with localized strings for dynamic statuses (e.g., `"status.final" → "Final"`)
- [ ] Set `stringKeys` on data bindings where real-time messages send status codes, not display strings
- [x] Accept `locale` query parameter (e.g. `?locale=es`) and compose `stringTable` in the requested locale

### Phase 3: Android
- [x] Extend `DataBindingResolver.applyBindings()` to check `stringKeys` for each target path
- [x] If stringKey exists: look up value in `stringTable`, fall back to raw value if not found
- [x] Log warning when stringKey lookup fails (per AGENTS.md §12)

### Phase 4: Web
- [x] Extend `DataBindingResolver.applyBindings()` with same stringKey lookup logic
- [x] Wire `stringTable` from screen response into resolver context

### Phase 5: Documentation & Tests
- [ ] Update Client Implementor's Contract with stringKey resolution algorithm
- [ ] Add pseudocode to DataBindingResolver algorithm in contract doc
- [ ] Add test: Ably message with status code → stringKey resolves to localized string
- [ ] Add test: missing stringKey → falls back to raw value

## Dependencies

- Depends on DataBindingResolver (already implemented on both platforms)
- ~~Depends on server locale detection (Accept-Language header parsing)~~ → Uses `locale` query parameter per §9o requirements

## Open Questions

- [ ] Should `stringTable` be screen-level or section-level? (Screen-level avoids duplication)
- [ ] Should the string table be cacheable separately from section data?
- [ ] How does RTL layout interact with AtomicComposite container direction?
- [ ] Should stringKeys support parameterized strings (e.g., `"wins.format" → "{0} Wins"`)?
