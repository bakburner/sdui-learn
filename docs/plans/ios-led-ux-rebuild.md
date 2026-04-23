# SDUI iOS-led UX rebuild (variants + For You + Schedule)

## Goal

Make the SDUI prototype's `/sdui/for-you` and `/sdui/schedule` screens look and behave like the iOS ref app's `FeedView` aesthetic, using two new semantic variant vocabularies rather than booleans or raw dimensions. Prototype repo only; ref apps are not touched; real data wiring is deferred.

## Non-goals (explicitly deferred)

- Renderer animations (pulse on live indicator, `contentTransition(.numericText())`, client-side clock interpolation). Separate plan.
- Rule 5 cleanup (server-emitted logo URLs instead of client-constructed from tricode). Separate plan.
- `GamePanelDisplayConfig` numeric-knob sweep (elevation etc. → semantic variants). Separate plan.
- Real CMS / scoreboard / schedule API wiring. Separate plan, gated on a Phase 1.0 API spike.
- Android ref app's button-dashboard aesthetic is not a design target. iOS `FeedView` is canonical.

## Guiding principles

- **Semantic over numeric.** New style axes land as variant enums, not booleans or pixel values. Clients resolve natively per Rule 16.
- **Rule 19 bar.** Ship only evidenced variant values. `GamePanelVariant` gets `standard` + `featured` (two sites in iOS feed). `SelectVariant` gets `dropdown` + `chips` + `segmented` (three evidenced picker idioms on iOS).
- **iOS-led, natively realized.** Wire output shaped after iOS `FeedView`; each client renders in its platform's native idioms (Compose / SwiftUI / CSS). No pixel parity expectations.
- **Rule 13.** Schema is the wire contract. New enum values go into `schema/sdui-schema.json` first, then `make codegen`, then renderers, then composers that emit them.

---

## Phase A — Schema & codegen foundation

### A.1 `GamePanelVariant` enum + `variant` on GamePanel envelope

Edit [schema/sdui-schema.json](../../schema/sdui-schema.json):

- Add a new definition:

```json
"GamePanelVariant": {
  "type": "string",
  "enum": ["standard", "featured"],
  "description": "Semantic treatment of the game card. Clients resolve natively (widths, padding, emphasis). 'standard' is the default card; 'featured' is a heightened card used as a lead item in a feed."
}
```

- Add `variant` as an optional property on the **`GamePanel` definition specifically** (not a shared Section envelope — only `GamePanel` carries this field for now). Placement mirrors the shape of `AtomicElement.variant` (schema line 427) but scoped to this section type so we don't imply every section type will grow variants. Missing `variant` is treated as `standard` at render time (renderer fallback; decoder stays strict per Rule 13).

### A.2 `SelectVariant` enum + `variant` on Form `select` field

- Add a new definition:

```json
"SelectVariant": {
  "type": "string",
  "enum": ["dropdown", "chips", "segmented"],
  "description": "How a Form single-select field is realized. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. 'segmented' is a platform segmented control."
}
```

- Add `variant` as an optional property on the `FormField` definition (schema ~line 777), **applicable only when `fieldType == "select"`**. Missing value treated as `dropdown` at render time. Note that `FormField.fieldType: "radio"` already exists and coexists with `select + variant: "segmented"` — they target different UIs (radio is a vertical stack of labeled choices; segmented is an equal-width horizontal pill row). No migration between them is in scope.

### A.3 Codegen + round-trip validation

- Run `make codegen`; verify updated [codegen/output/kotlin/SduiModels.kt](../../codegen/output/kotlin/SduiModels.kt), [ios/Sources/SduiCore/Models/SduiModels.swift](../../ios/Sources/SduiCore/Models/SduiModels.swift), [web/src/generated/SduiModels.ts](../../web/src/generated/SduiModels.ts).
- Extend [ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift](../../ios/Tests/SduiCoreTests/SduiModelsRoundTripTests.swift) with fixtures exercising both new variants (at least one `featured` GamePanel, one select with each of the three variant values).
- Equivalent smoke tests on Android / TypeScript if they exist; otherwise defer to Phase B renderer tests.

---

## Phase B — Client variant realizations (all three prototype clients)

Each client resolves the new tokens into its native idiom. No pixel parity across platforms; each client decides its own realization.

### B.1 iOS

- [ios/Sources/SduiCore/Rendering/Sections/GamePanelView.swift](../../ios/Sources/SduiCore/Rendering/Sections/GamePanelView.swift): read `section.variant`. `standard` → existing card treatment. `featured` → wider emphasis treatment consistent with iOS feed's featured card (exact width/padding is the renderer's call; SwiftUI-native).
- Select rendering in [ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift](../../ios/Sources/SduiCore/Rendering/Sections/FormSectionView.swift): resolve `variant`:
  - `dropdown` → `Menu` / `Picker(.menu)` (existing behaviour).
  - `chips` → horizontal `ScrollView` of capsule `Button`s, selected option styled via existing color tokens, `showsIndicators: false`.
  - `segmented` → `Picker(selection:) { ... }.pickerStyle(.segmented)`.

### B.2 Android

- [android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt](../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/GamePanelRenderer.kt): read `section.variant`; `featured` becomes wider / higher-emphasis per Material 3 Expressive idioms (Compose's call).
- Select rendering in the Android Form section renderer (under [android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/](../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/sections/); locate the composable handling `FormField.fieldType == "select"`). Resolve `variant`:
  - `dropdown` → existing `DropdownMenu`.
  - `chips` → `LazyRow` of Material 3 `FilterChip` with correct selected state.
  - `segmented` → Material 3 `SegmentedButton` row.

### B.3 Web

- [web/src/components/sections/GamePanel.tsx](../../web/src/components/sections/GamePanel.tsx): read `variant`; `featured` applies a wider / higher-emphasis CSS class. Realized in web idioms (no iOS mimicry).
- Form select rendering in [web/src/components/sections/Form.tsx](../../web/src/components/sections/Form.tsx): resolve `variant`:
  - `dropdown` → native `<select>`.
  - `chips` → horizontal scroll container of capsule buttons with selected state.
  - `segmented` → pill-button group.

### B.4 Fallback behaviour

All three clients: when `variant` is missing or unknown, fall back to `standard` / `dropdown` and log a debug warning. Strict decode is enforced by the generated models (Rule 13); unknown enum values at wire level surface as decode failures, which is correct.

---

## Phase C — For You composer rebuild

Edit [server/src/main/java/com/nba/sdui/service/ForYouComposer.java](../../server/src/main/java/com/nba/sdui/service/ForYouComposer.java) so `/sdui/for-you` matches iOS `FeedView` shape. Mock data sources are retained.

### C.1 Game carousel (replaces today's vertical upcoming-games list)

Today: one featured GamePanel + vertical list of 3 upcoming GamePanels + ad slots between.

Change to:

- One `AtomicComposite` containing a `ScrollContainer(direction: "row", showIndicators: false)` hosting multiple `GamePanel`s via `AtomicSectionSlot` (or whichever slot mechanism is already supported — verify during implementation; if slot can't host permanent sections, nest GamePanels at screen level and wrap with a header-only composite).
- The lead card in the carousel is emitted with `variant: "featured"`; remaining cards are `standard` (the default, so just omit `variant`).
- Horizontal gap and padding per iOS feed (~12pt spacing semantics; exact numbers are the renderer's decision given the semantic variant).

### C.2 VOD playlist section (new shape)

iOS ref app emits vertical "list" modules as `VODPlaylistView`: grouped rounded container + dividers with `.padding(.leading, 104)`.

Add a helper in [server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java](../../server/src/main/java/com/nba/sdui/service/AtomicCompositeBuilder.java):

```java
public JsonNode buildVodPlaylist(String id, String analyticsId, String header, List<VodRowSpec> rows) { ... }
```

Output shape:

- Root `Container` with surface color token, `cornerRadius: 12`, horizontal padding.
- Optional header Text row.
- For each row: horizontal `Container` with 80×52 thumb (using `Image` badge for LIVE / duration pill), title + optional subtitle column, trailing chevron Text/Icon, `onTap navigate` action.
- `Divider` between rows; the inset-left styling is the client's call (schema carries a divider, renderer idiomatizes).

Call this helper from `ForYouComposer` to emit one VOD playlist section (position it after the VOD carousel rails, matching iOS ordering where API `moduleType = list` modules follow carousel modules).

### C.3 Rail polishing

On existing content rails:

- Emit `showIndicators: false` on every horizontal `ScrollContainer` (all content rails, plus the new game carousel).
- Normalize duration-badge opacity to `0.7` (current composer uses `0.85`). If a design source disagrees, raise it; default to matching iOS ref app numeric value.
- Preserve existing `badge` alignment (`bottomEnd`) and `variant` token usage.

### C.4 Section ordering

Match iOS: games carousel → any featured events (live) → VOD carousel rails (up to 4) → VOD playlist. Ad slots are woven at existing insertion points; adjust indices to stay sensible in the new order.

### C.5 Example fixture

Add a new [schema/examples/for-you.json](../../schema/examples/for-you.json) fixture capturing the rebuilt For You shape (horizontal game carousel with one `variant: "featured"` lead, VOD carousel rails, VOD playlist section, ad slots). There is no existing For You fixture under `schema/examples/` — this one is net-new. Add it to the iOS round-trip test suite so schema drift on For You is caught by CI.

---

## Phase D — Schedule composer rebuild

Edit [server/src/main/java/com/nba/sdui/service/ScheduleComposer.java](../../server/src/main/java/com/nba/sdui/service/ScheduleComposer.java) to emit a chip-strip picker hierarchy and iOS-feed-aesthetic game rows. Mock JSON in [schema/examples/schedule-2024-25.json](../../schema/examples/schedule-2024-25.json) is retained.

### D.1 Picker migration: Season → Year → Month → Day

Replace the current two-dropdown Form with a Form carrying four `select` fields, each with `variant: "chips"`. Hierarchy mirrors Android ref app's four-level picker because iOS has no schedule to lift from, and this is the only evidenced pattern:

- `season` (short list: 2021-22 … 2024-25): `variant: "chips"`.
- `year` (two years per season): `variant: "chips"`.
- `month` (months within the selected year/season): `variant: "chips"`.
- `day` (days within the selected month): `variant: "chips"`.

**Scope note: visual-only in this plan.** The `/sdui/schedule` controller does not currently read query params (`?season=&year=&month=&day=`) and pass them to `ScheduleComposer.compose()` — today's Form dropdowns don't actually filter the response either, so the chip-strip migration is not a regression. But it also means selecting a chip will update client-side `screenState` and fire a refresh, and the refresh will return the same unfiltered response. Wiring the controller to accept params, re-filter the mock JSON, and emit a sensible lower-level default when a higher-level param changes is intentionally **deferred** to the follow-up data-wiring plan. Flag clearly in the UI that chip selection is cosmetic for now (or accept the no-op), and do not write tests that depend on filtering behaviour.

### D.2 Game row: iOS-feed aesthetic

Current row (`buildGameCard` in [server/src/main/java/com/nba/sdui/service/ScheduleComposer.java](../../server/src/main/java/com/nba/sdui/service/ScheduleComposer.java)) is already close: rounded surface, shadow, single `onTap navigate`, center-aligned team content, `monospacedDigits` on scores, `textAlign: "center"`. Confirm / adjust:

- Keep single-tap navigation to `nba://game/{id}` (no two-button pattern).
- Keep center-aligned matchup (consistent with iOS `FeedView` game cards).
- Keep surface color token + corner radius 12 + shadow (already iOS-feed-shaped).
- For non-final games (scheduled), time stays `variant: "body"`, `weight: "semiBold"`, `textAlign: "center"`. No change.

### D.3 Date section headers

Existing `SectionHeader`s between date groups stay. Confirm they render consistently above the picker Form.

---

## Phase E — Docs

- [docs/client-implementors-contract.md](../client-implementors-contract.md): add a subsection under variant resolution that documents `GamePanelVariant` and `SelectVariant`, with renderer mapping tables (SwiftUI / Compose / web) per the examples above. Cite Rule 16 + 19 anchoring.
- [docs/sdui-design-system.md](../sdui-design-system.md): add the two new vocabularies to the variant token catalogue.
- [docs/plans/sdui-refapp-implementation-plan.md](sdui-refapp-implementation-plan.md): add a short "Superseded by" note pointing at this plan for the For You / Schedule composer scope; leave the iOS ref-app integration (Phases 3 / 6 in that doc) as still-open future work.

---

## Validation

- `make codegen` clean.
- Round-trip tests pass on iOS (and equivalents on Android / web if present).
- `make ios-build`, `make dev-server`, `make dev-web`, `make dev-android` all green.
- `curl http://localhost:8080/sdui/for-you` returns the new shape; load in web dev client and Android demo to visually confirm.
- `curl http://localhost:8080/sdui/schedule` returns four chip-strip select fields + iOS-aesthetic game rows.
- iOS `SduiDemo` app renders both screens without decoder failures; check that `featured` and each `SelectVariant` value realize distinctly from `standard` / `dropdown`.

## Risk register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `AtomicSectionSlot` can't host permanent section types like `GamePanel` in a ScrollContainer | Medium | Medium | Verify during Phase C.1; fall back to rendering GamePanels as siblings with a header-only AtomicComposite label if slot hosting doesn't support it. |
| `chips` variant on Android renders poorly when option labels are wide (year labels like "2024-25") | Low | Low | Compose `FilterChip` handles this well; confirm at implementation time, adjust typography token if needed. |
| Mock data in Schedule doesn't cover enough years / months for the four-level chip picker to feel real | Medium | Low | Extend [schema/examples/schedule-2024-25.json](../../schema/examples/schedule-2024-25.json) with a few more months; if still thin, composer emits disabled chips for empty buckets. |
| Existing composers (other than these two) break on codegen if they omit `variant` on Form selects | Low | Medium | `variant` is optional at the schema level; absence is treated as `dropdown`. Verify no composer currently sets `variant` with a conflicting meaning. |
| Reviewer confusion between `FormField.fieldType: "radio"` and `FormField.fieldType: "select" + variant: "segmented"` | Low | Low | Document in `sdui-design-system.md` (Phase E) that these are distinct: `radio` is a multi-option vertical stack; `segmented` is an equal-width horizontal pill chooser. No migration is planned; both coexist. |
| Phase D.1 chip picker appears interactive but does not filter results because `/sdui/schedule` controller ignores query params | Confirmed | Low | Scope note in D.1 explicitly flags this. Controller param parsing is deferred to the data-wiring plan. Matches current behaviour (dropdowns also don't filter today). |

---

## Todos

1. **schema-variants** — Phase A.1 + A.2: Add `GamePanelVariant` and `SelectVariant` enums and `variant` properties to `schema/sdui-schema.json`.
2. **codegen-roundtrip** — Phase A.3: Run `make codegen`, regenerate Kotlin/Swift/TypeScript models, extend round-trip tests to cover both variants.
3. **ios-variants** — Phase B.1: Realize `GamePanelVariant` and `SelectVariant` natively in iOS `SduiCore` renderers.
4. **android-variants** — Phase B.2: Realize both variants natively in Android `sdui-core` renderers (Material 3 `FilterChip`, `SegmentedButton`, `DropdownMenu`).
5. **web-variants** — Phase B.3: Realize both variants in web atomic/section renderers (native select, capsule group, pill segmented).
6. **foryou-carousel** — Phase C.1: Convert `ForYouComposer` upcoming-games list into a horizontal game carousel with `variant: "featured"` on the lead card.
7. **vod-playlist** — Phase C.2: Add `buildVodPlaylist` helper to `AtomicCompositeBuilder` and emit one VOD playlist section in `ForYouComposer`.
8. **rail-polishing** — Phase C.3 + C.4: Add `showIndicators: false` to horizontal rails, normalize badge opacity to 0.7, re-order sections to match iOS feed.
9. **schedule-pickers** — Phase D.1: Replace Form dropdowns in `ScheduleComposer` with four chip-strip select fields (season/year/month/day).
10. **schedule-rows** — Phase D.2 + D.3: Confirm/adjust Schedule game rows to iOS-feed aesthetic; verify section header styling.
11. **docs** — Phase E: Update `client-implementors-contract.md`, `sdui-design-system.md`, and add supersedes note to `sdui-refapp-implementation-plan.md`.
12. **validation** — Run `make codegen`, `make ios-build`, `make dev-*` targets; curl both endpoints; verify iOS `SduiDemo`, web, and Android demo render the new shapes.
