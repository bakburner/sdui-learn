# SDUI Platform - Future Enhancements

This document captures enhancements that would evolve the current SDUI prototype into a production-grade, multi-platform server-driven UI system. Each enhancement is motivated by patterns proven at scale in the industry.

---

## 1. Screen Layouts and Placements

### Current State
A `Screen` contains a flat, ordered `sections` array. Section order is the only form of layout: sections render top-to-bottom in sequence.

### Enhancement
Introduce a **Layout** abstraction within screens. A layout defines named **placements** (e.g. `nav`, `main`, `footer`, `sidebar`), each of which references one or more sections by `id`. Sections move to a top-level `sections` array on the response, and layouts reference them.

```json
{
  "sections": [ ... ],
  "screens": [{
    "id": "root",
    "layout": {
      "type": "SingleColumnLayout",
      "placements": {
        "header": [{ "sectionId": "scoreboard" }],
        "main": [{ "sectionId": "stat-line" }, { "sectionId": "content-rail" }],
        "footer": [{ "sectionId": "promo-banner" }]
      }
    }
  }]
}
```

### Benefits
- The server controls where sections appear, not just in what order.
- The same section can be placed in multiple slots without duplicating data.
- New layout types (two-column, grid) are additive; old clients ignore unknown layouts and fall back to a default.

---

## 2. Form-Factor Layouts

### Current State
One layout for all devices. No distinction between phone portrait, tablet landscape, or desktop.

### Enhancement
Add **per-breakpoint layout** support. The screen provides a `layoutsPerFormFactor` map:

```json
"layoutsPerFormFactor": {
  "compact": { "type": "SingleColumnLayout", "placements": { ... } },
  "wide":    { "type": "TwoColumnLayout", "placements": { ... } }
}
```

Clients pick the layout that matches their current breakpoint/rotation. Rules for breakpoint selection (width thresholds, orientation) live in the client framework.

### Benefits
- Tablet and desktop get purpose-built layouts from the server without client releases.
- Mobile portrait and landscape can differ (e.g. landscape puts scoreboard and content side-by-side).

---

## 3. Screen Presentation Modes

### Current State
Every screen is rendered full-screen. There is no server-driven way to present a screen as a modal, bottom sheet, or popover.

### Enhancement
Add a `presentationMode` field to the screen:

```json
"screenProperties": {
  "presentationMode": "fullScreen" | "modal" | "bottomSheet" | "popover"
}
```

Client frameworks use this to wrap the screen in the appropriate container. Unknown modes fall back to full screen.

### Benefits
- Confirmation dialogs, detail overlays, and promo sheets are driven from the server without feature-specific client code.
- Enables multi-screen flows (e.g. a response returns two screens; the second opens as a bottom sheet).

---

## 4. Section Render Variants (SectionComponentType)

### Current State
Each section `type` maps 1:1 to a single client component. There is no way for the server to say "use the same data model but render it differently."

### Enhancement
Introduce a `componentType` (or `renderVariant`) field on each section. A section has a data type and a separate rendering key:

```json
{
  "id": "title-1",
  "type": "Title",
  "componentType": "PLUS_TITLE",
  "data": { "title": "Featured Game", "subtitle": "Playoff Spotlight" }
}
```

The client uses `componentType` to pick a component. If unrecognized, it falls back to the default component for that data `type`.

### Benefits
- One data model reused with different visual treatments (e.g. premium vs standard).
- Experiments can swap rendering without changing data or adding new section types.
- Backward compatible: old clients that don't know the new `componentType` fall back to the base.

---

## 5. Section Reuse by Reference

### Current State
Section data is inline inside the screen's `sections` array. If the same section should appear in two places (e.g. header and a tab), the data must be duplicated.

### Enhancement
Sections live in a top-level `sections` map (keyed by id). Layouts and placements reference sections by id, not by embedding them.

### Benefits
- Smaller payloads when a section appears in multiple placements or tabs.
- Single source of truth for section data; updates (e.g. from poll/SSE) propagate everywhere.

---

## 6. Section Container: Status and Logging

### Current State
A section has `analyticsId` and data. There is no explicit section-level status (loading, error, empty) or structured logging envelope.

### Enhancement
Wrap each section in a `SectionContainer`:

```json
{
  "sectionId": "stat-line",
  "status": "SUCCESS" | "LOADING" | "ERROR" | "EMPTY",
  "loggingData": { "impressionId": "...", "requestId": "..." },
  "section": { ... }
}
```

Client frameworks render appropriate states (skeleton, error message, empty state) based on `status`. Logging data is attached to every impression and interaction event automatically.

### Benefits
- Partial failure: one section can error without crashing the screen.
- Consistent loading / empty / error states across all clients.
- Structured analytics without per-section custom logging.

---

## 7. Extensible Action Types

### Current State
Action types are a fixed enum: `navigate`, `analytics`, `mutate`, `refresh`, `dismiss`. Adding a new type requires schema + codegen + client changes.

### Enhancement
Adopt an extensible action model. Keep a set of "core" actions that the framework handles universally (navigate, scroll to section, dismiss, refresh). Allow features to register **custom action types** with a feature-scoped handler:

```json
{
  "trigger": "onTap",
  "type": "custom",
  "customType": "add_to_calendar",
  "payload": { "eventId": "game-0042300102", "startTime": "..." }
}
```

Client frameworks route `custom` actions to a handler registry. Unknown custom types are logged and no-oped gracefully.

### Benefits
- Features can add behavior without modifying the core schema or framework.
- Core actions stay stable and backward compatible.
- Custom handlers can contain feature-specific business logic.

---

## 8. Nested / Composable Sections

### Current State
Sections are flat; the only nesting is TabGroup's `tabContents` (sections within a tab).

### Enhancement
Allow any section to declare `children` sections. This enables composition (e.g. a "card" section containing a title section and a stat-line section) without creating a new section type for every combination.

### Benefits
- Reduces the number of section types; the server composes from building blocks.
- Layout flexibility: a parent section can arrange child sections in rows, columns, or grids.

---

## 9. Design System Integration

### Current State
Styles are hard-coded in each client's section components. The server sends a few overrides (`backgroundColor`, `padding`). No design tokens or theme references.

### Enhancement
Define a set of **design tokens** (colors, typography scale, spacing scale, elevation) in the schema as semantic names:

```json
"style": {
  "background": "surface.primary",
  "textColor": "text.onSurface",
  "spacing": "md"
}
```

Clients resolve tokens via their design system. Actual values stay in the client; tokens are the contract.

### Benefits
- Server controls emphasis and hierarchy without dictating pixels.
- Dark mode, accessibility overrides, and rebrand are handled by updating the client token map.
- Consistent appearance across platforms.

---

## 10. WYSIWYG / No-Code Editing

### Current State
Server responses are hand-built in Java or loaded from JSON example files. No tooling for non-engineers.

### Enhancement
Build a visual editor (internal tool) that lets product managers and designers assemble screens from a section library, configure placements, and preview the result across form factors. The editor outputs valid SDUI responses that the composition service can serve.

### Benefits
- Reduces time-to-ship for content and layout changes from days to minutes.
- Non-engineers can create and test experiences without code.
- Increases adoption of the SDUI platform across the organization.

---

## Priority Guidance

| Enhancement | Effort | Impact | Suggested Phase |
|---|---|---|---|
| Screen layouts and placements | Medium | High | Phase 2 |
| Form-factor layouts | Medium | High | Phase 2 |
| Section render variants | Low | Medium | Phase 2 |
| Section reuse by reference | Low | Medium | Phase 2 |
| Screen presentation modes | Low | Medium | Phase 3 |
| Section container (status/logging) | Medium | High | Phase 3 |
| Extensible action types | Medium | Medium | Phase 3 |
| Nested / composable sections | High | High | Phase 4 |
| Design system integration | Medium | High | Phase 4 |
| WYSIWYG / no-code editing | High | High | Phase 5+ |

---

## Relationship to Current Prototype

The current prototype demonstrates the foundation: a shared schema, codegen pipeline, section-based rendering, server-driven actions, and per-section refresh policies. The enhancements above are **additive** -- each can be introduced without breaking existing clients, following the same principle that unknown types are gracefully skipped. Together they move the system from a single-screen prototype to a scalable, multi-platform SDUI platform.
