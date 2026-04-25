Doc consistency audit + dark-mode theme toggle + rendering fixes

Doc-consistency-audit pass after GamePanel migration to AtomicComposite, plus
dark-mode theme toggle across all three clients and rendering refinements.

--- Doc fixes ---

- Section counts: 9 permanent → 8 permanent (GamePanel migrated)
- Atomic element counts: 10 → 11 (LiveClock added to coverage tables)
- Migrated type counts: 9 → 10 (GamePanel added to all migrated-type lists)
- Conformance checklist: remapped Rule 1–18 → AGENTS.md §1.2–§11 section refs
- client-builder.agent.md: updated from "19 rules" to "13 sections (§0–§12)"
- web/README.md: directory tree updated to match actual section renderers
- Exec Summary: ADR statuses updated from "ADR pending" to specific ADR refs
- Requirements Summary: atomic layer row updated with LiveClock + GamePanel
- schema/sdui-schema.json: removed Rule 19 citation per §10.3
- sdui-design-system.md: added implementation-status callouts (§2 capability
  gating, §4 override matrices, §7 diagnostics) and §11 Future Enhancements
  table for 6 planned-but-unimplemented features.
- SDUI_Technical_Proposal_v2.md: §1 capability-gating annotated as
  plumbed-not-consumed; §2c Figma CI pipeline marked planned-not-built;
  §8 style-tokens row qualified with diagnostic coverage status.
- sdui-requirements-summary.md: §9g override-matrix and diagnostic coverage
  qualified to match actual implementation state.
- Revision history entries added to Tech Proposal, Exec Summary, Req Summary.

--- Dark-mode theme toggle ---

- Android: dark/light toggle button in top bar; `rememberSaveable` theme state
  in MainActivity; desugaring enabled for java.time APIs.
- iOS: toolbar toggle in ScreenShell; `@AppStorage("sdui_color_scheme")`
  persistence; `.preferredColorScheme()` applied to view hierarchy.
- Web: `setColorSchemePreference()` / `initializeColorSchemePreference()` with
  localStorage persistence; `data-theme` attribute on `<html>`; full light-mode
  CSS custom property set in nba-tokens.css; toggle button in App header.

--- Color token & rendering fixes ---

- color-tokens.json + all 3 client ColorTokenResolvers: swapped
  `color.surface.canvas` (grey.10→grey.5) and `color.surface.raised`
  (grey.0→grey.10) so cards have visible contrast against the canvas in both
  light and dark modes.
- Server: AtomicCompositeBuilder — gamePanelSurface gradient now uses
  token-backed colors (SURFACE_RAISED/SURFACE_PROMO) instead of hardcoded hex,
  so dark-mode cards render correctly.
- Server: AtomicCompositeBuilder — content-card thumbnail padding zeroed
  (flush top edge), corner radii 6→12, column crossAlignment set to "start",
  subhead maxLines capped at 2, DisplayGrid fillWidth + stretch alignment.
- Web: TopNavigationBar — horizontal scroll overflow for narrow viewports.
- Web: AtomicDisplayGrid — `table-layout: fixed` + full-width AtomicBox.
- Web: ColorTokenResolver — dark-mode detection uses Material scheme
  luminance heuristic instead of raw `isSystemInDarkTheme`.
- Android: ColorTokenResolver — uses `MaterialTheme` luminance instead of
  `isSystemInDarkTheme()` for scheme selection.

--- Deleted stale files ---

- docs/KineticSemanticVariables.csv
- docs/mobile-core-library-audit-2026-04-24.md
- docs/plans/ios-led-ux-rebuild.md
- docs/plans/plan-atomic-element-box-model-unification.md
- docs/plans/plan-mobile-atomic-contract-parity-audit.md
- docs/plans/sdui-refapp-implementation-plan.md

