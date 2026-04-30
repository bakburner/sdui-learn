SDUI: layout overhaul (sizing/wrap/shadows), tokenize md=12, replace loremflickr, doc audit

--- Schema: layout constraint & sizing overhaul ---

- SizingMode enum (hug/fill/fixed) + widthMode/heightMode fields
- minWidth, maxWidth, minHeight, maxHeight constraint fields
- layoutWrap boolean + crossAxisGap for flex-wrap containers
- alignSelf per-child cross-axis override
- backgrounds[] array (multi-layer, deprecates singular background)
- shadows[] array (multi-layer, deprecates singular shadow)
- Shadow.type: "drop" | "inner" (inner shadow support)
- fillWidth, background, shadow marked DEPRECATED in schema descriptions

--- Client implementations ---

- iOS: WrappingFlexLayout.swift, AtomicBoxModifier updated for alignSelf,
  sizingMode, min/max constraints, multi-shadows, multi-backgrounds
- Android: AtomicBox/AtomicContainer updated, LayoutTokenResolver md=12
- Web: AtomicBox/AtomicContainer/AtomicOverlayContainer updated,
  LayoutTokenResolver md=12, background.ts multi-layer util

--- Spacing token correction (md=12 base) ---

- GameDetailComposer overlay: cornerRadius → RADIUS_LG, spacer1 → SPACING_LG
- AtomicCompositeBuilder.buildStatRowVertical: inter-column spacer
  SPACING_MD → SPACING_LG (stat rows were cramped)
- LayoutTokenResolver on all platforms: md resolves to 12 (was 8)

--- Replace broken loremflickr.com image URLs ---

- DemoImageUrls.java: added hero(), cardTall(), thumb(), avatar() helpers
- HomeComposer, ForYouComposer, DemoScreenComposer: all loremflickr URLs
  replaced with same-origin DemoImageUrls SVGs

--- Doc consistency audit ---

- docs/sdui-design-system.md: spacing md 8→12, radius md 8→12
- docs/glossary.md: added "Box model & layout" section (SizingMode,
  widthMode/heightMode, min/max, layoutWrap, crossAxisGap, alignSelf,
  backgrounds array, shadows array, inner shadow)
- docs/client-implementors-contract.md: AtomicBox pseudocode updated for
  widthMode/heightMode, shadows[], backgrounds[], min/max constraints,
  layoutWrap/crossAxisGap/alignSelf
- README.md: added "Layout constraint & sizing overhaul" to Recent Changes

--- Verification ---

- `./gradlew compileJava` passes
- `grep -rn loremflickr server/src/` returns zero hits
