# Plan: Theming & Design Tokens

> Source requirements: §9g, §9s from sdui-requirements-summary.md

## Summary

Replace hardcoded hex color values with semantic color tokens so servers can drive theming and dark mode without client code changes. Integrate Figma design token export with a CI validation pipeline to ensure server-composed colors stay in sync with the design system.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| Schema support | Partial | Colors are literal hex strings; no token reference system |
| Server support | Partial | Composers use hardcoded hex values |
| Android support | Gap | No token resolution; applies hex colors directly |
| Web support | Gap | No token resolution; applies hex colors directly |
| Documentation | Partial | Token mapping table in Tech Proposal §2c |
| Tests | Gap | No Figma token consistency validation |

## Requirements Addressed

- [ ] **REQ-1**: Define semantic color token vocabulary (e.g., `color.primary`, `color.surface`) — §9g
- [ ] **REQ-2**: Server sends token references instead of literal hex values — §9g
- [ ] **REQ-3**: Client resolves tokens to platform-native colors (with dark mode support) — §9g
- [ ] **REQ-4**: Figma design token export → JSON token file — §9s
- [ ] **REQ-5**: CI pipeline validates server colors against Figma-exported token set — §9s

## Tasks

### Phase 1: Schema & Codegen
- [ ] Define `ColorToken` union type: `{ literal: "#hex" } | { token: "color.primary" }`
- [ ] Update all color fields in schema to accept `ColorToken`
- [ ] Run codegen

### Phase 2: Server
- [ ] Create `TokenRegistry` service that maps token names to hex values per theme
- [ ] Update composers to use token references where appropriate
- [ ] Support `X-Theme: dark` header for theme-aware composition

### Phase 3: Android
- [ ] Implement `TokenResolver` utility — resolve token names to Compose `Color` values
- [ ] Load token mappings from bundled JSON or server config endpoint
- [ ] Support dark/light theme switching via `isSystemInDarkTheme()` + token mappings
- [ ] Update all renderers to use `TokenResolver` instead of raw hex parsing

### Phase 4: Web
- [ ] Implement `TokenResolver` utility — resolve token names to CSS custom properties
- [ ] Load token mappings from bundled JSON or server config endpoint
- [ ] Support dark mode via `prefers-color-scheme` media query + token CSS variables
- [ ] Update all renderers to use CSS variables instead of inline hex colors

### Phase 5: Documentation & Tests
- [ ] Document token vocabulary in schema examples
- [ ] Build Figma token export pipeline (Figma API → JSON token file)
- [ ] Add CI step: compare server token usage against Figma-exported token set
- [ ] Update `docs/sdui-requirements-summary.md` status: §9g Gap → Built, §9s Partial → Built

## Dependencies

- Requires design team to define the semantic token vocabulary
- Figma token export requires Figma API access and token format agreement

## Open Questions

- [ ] Should tokens be resolved server-side (server sends final hex) or client-side (server sends token name)?
- [ ] How many theme variants to support initially (light/dark, or also high-contrast)?
- [ ] Should the token file be bundled in the app or fetched at runtime?
