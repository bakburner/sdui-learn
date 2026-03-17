# Plan: GamePanel DisplayConfig — Server-Driven Visual Configuration

**Date:** 2025-03-17
**Status:** Draft
**Scope:** Schema, Server, Android, Web

---

## TL;DR

Replace the hardcoded `variant` branch (`standard` / `featured` / `scoreboard`) with a server-driven `displayConfig` object on `GamePanelData`. The client collapses three separate renderers into one unified renderer that reads visual properties (background, logo size, card height, score text style, etc.) from the server payload.

As part of this work, introduce a shared **`Background`** union type in the schema — a single definition that supports solid color, gradient, or image with overlay. This replaces the current scattered approach (`backgroundColor`, `backgroundGradient`, `backgroundImageUrl` as separate properties on different types) with one consistent concept reused across `AtomicElement`, `GamePanelDisplayConfig`, `TabGroupData`, `SubscribeHeroData`, and `SubscribeBannerData`.

The kitchen-sink AtomicComposite scoreboard is retired in favor of a real GamePanel section with scoreboard-style displayConfig. Ably SSE live updates remain wired to the section's `refreshPolicy`/`dataBindings` — no change needed since displayConfig is purely visual.

## Decisions

- **No backward compatibility** — prototype; replace `variant` entirely, don't deprecate
- **All 3 variants unified** — standard, featured, and scoreboard all become displayConfig presets
- **Kitchen-sink AtomicComposite scoreboard retired** — replaced by real GamePanel section with compact displayConfig
- **Ably SSE unchanged** — live updates already work via `refreshPolicy`/`dataBindings` on the Section; displayConfig is orthogonal
- **`Background` is a shared schema definition** — a `oneOf` union (solid hex string, `BackgroundGradient`, `BackgroundImage`) defined once and referenced everywhere. Replaces the current scattered `backgroundColor` / `backgroundGradient` / `backgroundImageUrl` properties across multiple types
- **`BackgroundImage` is new** — added to schema with `imageUrl`, `scaleType`, `overlay`
- **`BackgroundGradient` is promoted** — moved from inline `AtomicElement` property to a top-level `$ref`-able definition
- **No show/hide booleans on displayConfig** — visibility of optional sections (leaders, broadcaster) is driven by **data presence**, not redundant boolean flags. If the server omits `gameLeaders` the client shows nothing; if it omits `broadcaster` the client shows nothing. Team names are always shown (`teamName` is required on `TeamData`). This avoids the anti-pattern of sending data alongside a flag that says "don't render the data I just sent you."

## Steps

### Phase 1: Schema (blocks all other phases)

1. Promote `BackgroundGradient` to a top-level definition in `sdui-schema.json` (currently inline on `AtomicElement`). Keep `colors` (string array, required) + `direction` (enum: horizontal/vertical/diagonal, default vertical).

2. Add `BackgroundImage` definition to `sdui-schema.json`:
   - `imageUrl` (string, required), `scaleType` (enum: cover/fill/contain, default cover), `overlay` (oneOf: solid hex string or `{ "$ref": "#/definitions/BackgroundGradient" }`)

3. Add shared `Background` definition — a `oneOf` union:
   ```json
   "Background": {
     "oneOf": [
       { "type": "string", "description": "Solid hex color (e.g. #1A1F2E)" },
       { "$ref": "#/definitions/BackgroundGradient" },
       { "$ref": "#/definitions/BackgroundImage" }
     ]
   }
   ```

4. Migrate existing types to use `{ "$ref": "#/definitions/Background" }`:
   - **`AtomicElement`** — replace `backgroundColor` (string) + `backgroundGradient` (inline object) with single `background` property
   - **`SubscribeHeroData`** — replace `backgroundImageUrl` (string) with `background`
   - **`SubscribeBannerData`** — replace `backgroundImageUrl` (string) with `background`
   - ~~`TabGroupData`~~ — *no change needed*; `TabGroupData` does not have `backgroundColor` (it's on `Section`, which is out of scope)

5. Add `GamePanelDisplayConfig` definition to `sdui-schema.json`:
   - `logoSize` (integer, default 32), `cardHeight` (integer, nullable = auto-size), `cornerRadius` (integer, default 12), `elevation` (integer, default 0), `scoreTextStyle` (enum: compact/prominent, default compact), `background` (`$ref` Background), `liveBackground` (`$ref` Background), `badgeColor` (string, hex)
   - **No show/hide booleans** — `showTeamNames`, `showLeaders`, `showBroadcaster` are intentionally omitted. Team names are always shown (required field). Leaders and broadcaster visibility is driven by data presence: if the server omits `gameLeaders` or `broadcaster` from `GamePanelData`, the client renders nothing for that section.

6. Replace `variant`, `backgroundImageUrl` on `GamePanelData` with `displayConfig` ($ref GamePanelDisplayConfig). Remove `variant` enum entirely.

7. Run codegen: `cd codegen && ./generate.sh`

### Phase 2: Server — *parallel with Phase 3 after Phase 1*

8. Update `AtomicCompositeBuilder` to use the new `background` property (instead of separate `backgroundColor`/`backgroundGradient`) when building AtomicComposite sections. All existing sections that set `backgroundColor` or `backgroundGradient` must switch to the unified `background` field.

9. Add `GamePanelDisplayConfig` helper on `AtomicCompositeBuilder` (or a new utility) that builds the displayConfig JSON node from parameters. Create preset methods: `standardConfig()`, `featuredConfig(bgColor, liveBgGradient, bgImageUrl)`, `scoreboardConfig(bgColor)`.

10. Update `DemoScreenComposer.buildDemoGamePanel()` — add `displayConfig` using `standardConfig()`. Remove `variant` field.

11. Update `DemoScreenComposer.buildDemoFeaturedGamePanel()` — add `displayConfig` using `featuredConfig(...)` with gradient + background image. Remove `variant` and `backgroundImageUrl` fields.

12. Update `DemoScreenComposer.buildDemoGamePanelScoreboard()` — **replace** `atomicBuilder.buildScoreboardHeader(...)` call with a real GamePanel section that has `displayConfig` using `scoreboardConfig(...)`. This retires the AtomicComposite scoreboard from kitchen-sink.

13. Update `ScoreboardComposer.buildScoreboardRowSection()` — replace `data.put("variant", "scoreboard")` with `displayConfig` using `scoreboardConfig()`. Ably `refreshPolicy`/`dataBindings` remain unchanged.

14. Update `GameDetailComposer.buildGamePanelScoreboardFromLive()` — same as step 13. Replace `data.put("variant", "scoreboard")` with `displayConfig`. Ably wiring stays.

15. Update server composers for `SubscribeHero` / `SubscribeBanner` — replace `backgroundImageUrl` with `background` using `BackgroundImage` object.

16. ~~Update server composers for `TabGroup`~~ — *no-op*; no server composer sets `backgroundColor` on `TabGroupData`.

17. Remove `AtomicCompositeBuilder.buildScoreboardHeader()` and `buildTeamColumn()` methods — no longer needed.

18. Update example JSON files (`game-detail-pre.json`, `game-detail-live.json`, `game-detail-final.json`) — replace `variant` with `displayConfig` in GamePanel sections; replace old background properties with `background` in all section types.

### Phase 3: Android — *parallel with Phase 2 after Phase 1*

19. Add shared `Background` sealed class to `AtomicElement.kt` (or a new `Background.kt`):
    ```kotlin
    sealed class Background {
        data class Solid(val color: String) : Background()
        data class Gradient(val gradient: BackgroundGradient) : Background()
        data class Image(val imageUrl: String, val scaleType: String = "cover",
                         val overlay: Background? = null) : Background()
    }
    ```
    Add `parseBackground(raw: Any?): Background?` helper that inspects JSON type (string → Solid, object with `colors` → Gradient, object with `imageUrl` → Image).

20. Update `AtomicContainer` rendering — replace `backgroundColor` / `backgroundGradient` reads with unified `background` → `parseBackground()` → resolve to Compose `Color`, `Brush`, or `AsyncImage` + overlay.

21. Add `GamePanelDisplayConfig` model to `SectionUiAdapters.kt`:
    ```kotlin
    data class GamePanelDisplayConfig(
        val logoSize: Int = 32, val cardHeight: Int? = null, val cornerRadius: Int = 12,
        val elevation: Int = 0, val scoreTextStyle: String = "compact",
        val background: Background? = null,
        val liveBackground: Background? = null, val badgeColor: String? = null
    )
    ```

22. Update `GamePanelUiModel` — replace `variant: String` and `backgroundImageUrl: String?` with `displayConfig: GamePanelDisplayConfig`.

23. Update `mapGamePanel()` — parse `displayConfig` from `data["displayConfig"]`. Use `parseBackground()` for background fields. Apply defaults for any missing fields.

24. **Merge** `StandardGamePanelContent` and `FeaturedGamePanelContent` into a single `GamePanelContent` composable in `GamePanelRenderer.kt`:
    - Read all visual values from `model.displayConfig`
    - Logo size: `model.displayConfig.logoSize.dp`
    - Card height: if non-null, `Modifier.height(config.cardHeight.dp)` else auto
    - Corner radius: `RoundedCornerShape(config.cornerRadius.dp)`
    - Elevation: `CardDefaults.cardElevation(config.elevation.dp)`
    - Score text: if `prominent` use `headlineMedium` + `ExtraBold`, else `bodyLarge` + `Bold`
    - Team names: always shown (`teamName` is required on `TeamData`)
    - Leaders: shown when `model.leaderLines` is non-empty (server controls by including/omitting `gameLeaders`)
    - Broadcaster: shown when `model.broadcaster` is non-null (server controls by including/omitting `broadcaster`)
    - Background: resolve `config.background` (or `config.liveBackground` when LIVE) via shared `Background` sealed class
    - Badge color: `config.badgeColor?.let { parseColor(it) } ?: ...`

25. Remove `FeaturedGamePanelContent` and `StandardGamePanelContent` — dead code after merge.

26. Remove the `if (model.variant == "featured")` branch in `GamePanelRenderer`.

27. Update `SubscribeHeroRenderer` / `SubscribeBannerRenderer` — read `background` instead of `backgroundImageUrl`, use `parseBackground()`.

### Phase 4: Web — *parallel with Phase 2+3 after Phase 1*

28. Add shared `resolveBackgroundCSS(bg: Background): CSSProperties` helper in a new `background.ts` util:
    - String → `{ background: '#hex' }`
    - BackgroundGradient → `{ background: 'linear-gradient(...)' }`
    - BackgroundImage → `{ backgroundImage: 'url(...)', backgroundSize: 'cover' }` + overlay handling

29. Update `AtomicContainer` web rendering — replace `backgroundColor`/`backgroundGradient` reads with unified `background` → `resolveBackgroundCSS()`.

30. Update `GamePanelUiModel` in `sectionUiAdapters.ts` — replace `variant: string` and `backgroundImageUrl?: string` with `displayConfig: GamePanelDisplayConfig` interface.

31. Update `mapGamePanel()` — parse `displayConfig` from raw data. Apply defaults.

32. **Merge** `StandardGamePanelView` and `FeaturedGamePanelView` into a single `GamePanelView` component in `GamePanel.tsx`:
    - Use `resolveBackgroundCSS()` for `displayConfig.background` / `liveBackground`
    - Compute inline styles from `displayConfig` values (logo width/height, borderRadius, minHeight)
    - `liveBackground` overrides `background` when `visualState === 'LIVE'`

33. Remove `FeaturedGamePanelView`, `StandardGamePanelView`, `standardStyles`, `featuredStyles` — all replaced by config-driven styles.

34. Remove the `if (model.variant === 'featured')` branch in `GamePanel`.

35. Update `SubscribeHero` / `SubscribeBanner` web renderers — read `background` instead of `backgroundImageUrl`, use `resolveBackgroundCSS()`.

### Phase 5: Cleanup & Verification

36. Remove `variant` enum from schema (already done in step 6). Verify codegen output has no `variant` references.

37. Search codebase for remaining `variant` references in GamePanel context — remove all.

38. Search codebase for remaining `backgroundColor`, `backgroundGradient`, `backgroundImageUrl` references — verify all migrated to `background`.

39. Rebuild and test all screens:
    - Kitchen-sink: verify standard GamePanel, featured GamePanel, and scoreboard all render correctly with displayConfig
    - All AtomicComposite sections with backgrounds (NbaTvSchedule, HeroPanel, etc.) still render correctly with unified `background`
    - SubscribeHero / SubscribeBanner render background images via `Background` type
    - Scoreboard screen (live data): verify Ably SSE updates still flow and scores update in real-time
    - Game-detail screen: verify scoreboard header renders from displayConfig GamePanel, not AtomicComposite
    - Web: verify same screens render identically

## Relevant Files

**Schema & codegen:**
- `schema/sdui-schema.json` — add `Background`, `BackgroundImage`, `GamePanelDisplayConfig` definitions; promote `BackgroundGradient`; migrate `AtomicElement`, `TabGroupData`, `SubscribeHeroData`, `SubscribeBannerData`, `GamePanelData`
- `codegen/generate.sh` — run after schema changes
- `codegen/output/kotlin/`, `codegen/output/typescript/`, `codegen/output/swift/` — verify generated output

**Server:**
- `server/src/.../service/DemoScreenComposer.java` — `buildDemoGamePanel()`, `buildDemoFeaturedGamePanel()`, `buildDemoGamePanelScoreboard()`
- `server/src/.../service/ScoreboardComposer.java` — `buildScoreboardRowSection()`
- `server/src/.../service/GameDetailComposer.java` — `buildGamePanelScoreboardFromLive()`
- `server/src/.../service/AtomicCompositeBuilder.java` — remove `buildScoreboardHeader()` and `buildTeamColumn()`

**Android:**
- `android/sdui-core/src/.../renderer/sections/GamePanelRenderer.kt` — merge two composables into one config-driven renderer
- `android/sdui-core/src/.../renderer/adapters/SectionUiAdapters.kt` — update `GamePanelUiModel`, `mapGamePanel()`, add `GamePanelDisplayConfig`
- `android/sdui-core/src/.../models/AtomicElement.kt` — add `Background` sealed class, `parseBackground()` helper; migrate `AtomicContainer` background handling
- `android/sdui-core/src/.../renderer/atomic/AtomicContainer.kt` — use unified `background` property
- `android/sdui-core/src/.../renderer/sections/SubscribeHeroRenderer.kt` — migrate to `background`
- `android/sdui-core/src/.../renderer/sections/SubscribeBannerRenderer.kt` — migrate to `background`

**Web:**
- `web/src/utils/background.ts` — new shared `resolveBackgroundCSS()` helper
- `web/src/components/sections/GamePanel.tsx` — merge two views, remove hardcoded style objects
- `web/src/components/atomic/AtomicContainer.tsx` — use unified `background` property
- `web/src/components/sections/SubscribeHero.tsx` — migrate to `background`
- `web/src/components/sections/SubscribeBanner.tsx` — migrate to `background`
- `web/src/adapters/sectionUiAdapters.ts` — update `GamePanelUiModel`, `mapGamePanel()`

**Example JSON:**
- `schema/examples/game-detail-pre.json`, `game-detail-live.json`, `game-detail-final.json` — update GamePanel sections

## Verification

1. `cd codegen && ./generate.sh` — codegen succeeds with no errors
2. `cd server && ./gradlew bootRun` — server starts; hit `/sdui/demos` and verify GamePanel sections in JSON response have `displayConfig` instead of `variant`
3. `make dev-android` — kitchen-sink screen: standard card (compact, dark bg), featured card (gradient, large logos, team names), scoreboard (centered row, NBA blue bg) all render from one renderer
4. `make dev-web` — same verification on web
5. Hit `/sdui/scoreboard` endpoint — verify Ably `refreshPolicy` and `dataBindings` still present on live game sections
6. Navigate to game-detail screen with a live game — verify scores update via Ably SSE
7. `grep -rn 'variant.*featured\|variant.*scoreboard\|variant.*standard' android/ web/ server/ schema/` — zero hits (no remaining variant references in GamePanel context)
9. `grep -rn 'backgroundColor\|backgroundGradient\|backgroundImageUrl' android/ web/ server/ schema/` — zero hits (all migrated to `background`)
8. `python3 prompts/skills/doc-consistency-audit/extract-facts.py` — schema facts still clean

## Scope Boundaries

**Included:**
- Schema: shared `Background` union type, `BackgroundGradient` (promoted), `BackgroundImage` (new), `GamePanelDisplayConfig`
- Schema migration: `AtomicElement`, `SubscribeHeroData`, `SubscribeBannerData`, `GamePanelData` all use `background` ref (`TabGroupData` unchanged — no existing background property)
- Server: all GamePanel builder methods converted to use displayConfig; all composers migrated to unified `background`
- Android + Web: shared `Background` model/helper + unified single-path GamePanel renderer
- Kitchen-sink AtomicComposite scoreboard → real GamePanel section
- `AtomicCompositeBuilder.buildScoreboardHeader()` removed
- Ably SSE verified working (no changes needed — it's on the Section, not the data)

**Excluded:**
- Other section types — displayConfig pattern could apply (SubscribeHero, SubscribeBanner) but that's a separate task
- iOS — future platform, will pick up schema changes automatically via codegen
- Accessibility — covered by separate plan-accessibility.md
- New visual configs beyond what exists today (e.g., animation, shadow) — add later as needed
