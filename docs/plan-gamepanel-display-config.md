# Plan: GamePanel DisplayConfig — Server-Driven Visual Configuration

**Date:** 2025-03-17
**Status:** Draft
**Scope:** Schema, Server, Android, Web

---

## TL;DR

Replace the hardcoded `variant` branch (`standard` / `featured` / `scoreboard`) with a server-driven `displayConfig` object on `GamePanelData`. The client collapses three separate renderers into one unified renderer that reads visual properties (background, logo size, card height, score text style, etc.) from the server payload. Background supports three forms: solid color, gradient, or image with overlay. The kitchen-sink AtomicComposite scoreboard is retired in favor of a real GamePanel section with scoreboard-style displayConfig. Ably SSE live updates remain wired to the section's `refreshPolicy`/`dataBindings` — no change needed since displayConfig is purely visual.

## Decisions

- **No backward compatibility** — prototype; replace `variant` entirely, don't deprecate
- **All 3 variants unified** — standard, featured, and scoreboard all become displayConfig presets
- **Kitchen-sink AtomicComposite scoreboard retired** — replaced by real GamePanel section with compact displayConfig
- **Ably SSE unchanged** — live updates already work via `refreshPolicy`/`dataBindings` on the Section; displayConfig is orthogonal
- **Background is a 3-way union** — solid hex string, `BackgroundGradient` object, or `BackgroundImage` object (with overlay)
- **`BackgroundImage` definition is shared** — added to schema alongside `BackgroundGradient` for reuse by any section type

## Steps

### Phase 1: Schema (blocks all other phases)

1. Add `BackgroundImage` definition to `sdui-schema.json`:
   - `imageUrl` (string, required), `scaleType` (enum: cover/fill/contain, default cover), `overlay` (oneOf: solid hex string or `BackgroundGradient`)
2. Add `GamePanelDisplayConfig` definition to `sdui-schema.json`:
   - `logoSize` (integer, default 32), `cardHeight` (integer, nullable = auto-size), `cornerRadius` (integer, default 12), `elevation` (integer, default 0), `scoreTextStyle` (enum: compact/prominent, default compact), `showTeamNames` (boolean, default false), `showLeaders` (boolean, default true), `showBroadcaster` (boolean, default true), `background` (oneOf: string | BackgroundGradient | BackgroundImage), `liveBackground` (oneOf: string | BackgroundGradient | BackgroundImage), `badgeColor` (string, hex)
3. Replace `variant`, `backgroundImageUrl` on `GamePanelData` with `displayConfig` ($ref GamePanelDisplayConfig). Remove `variant` enum entirely.
4. Run codegen: `cd codegen && ./generate.sh`

### Phase 2: Server — *parallel with Phase 3 after Phase 1*

5. Add `GamePanelDisplayConfig` helper on `AtomicCompositeBuilder` (or a new utility) that builds the displayConfig JSON node from parameters. Create preset methods: `standardConfig()`, `featuredConfig(bgColor, liveBgGradient, bgImageUrl)`, `scoreboardConfig(bgColor)`.

6. Update `DemoScreenComposer.buildDemoGamePanel()` — add `displayConfig` using `standardConfig()`. Remove `variant` field.

7. Update `DemoScreenComposer.buildDemoFeaturedGamePanel()` — add `displayConfig` using `featuredConfig(...)` with gradient + background image. Remove `variant` and `backgroundImageUrl` fields.

8. Update `DemoScreenComposer.buildDemoGamePanelScoreboard()` — **replace** `atomicBuilder.buildScoreboardHeader(...)` call with a real GamePanel section that has `displayConfig` using `scoreboardConfig(...)`. This retires the AtomicComposite scoreboard from kitchen-sink.

9. Update `ScoreboardComposer.buildScoreboardRowSection()` — replace `data.put("variant", "scoreboard")` with `displayConfig` using `scoreboardConfig()`. Ably `refreshPolicy`/`dataBindings` remain unchanged.

10. Update `GameDetailComposer.buildGamePanelScoreboardFromLive()` — same as step 9. Replace `data.put("variant", "scoreboard")` with `displayConfig`. Ably wiring stays.

11. Remove `AtomicCompositeBuilder.buildScoreboardHeader()` and `buildTeamColumn()` methods — no longer needed.

12. Update example JSON files (`game-detail-pre.json`, `game-detail-live.json`, `game-detail-final.json`) — replace `variant` with `displayConfig` in any GamePanel sections.

### Phase 3: Android — *parallel with Phase 2 after Phase 1*

13. Add `GamePanelDisplayConfig` model to `SectionUiAdapters.kt` (or a new file):
    ```kotlin
    data class GamePanelDisplayConfig(
        logoSize: Int = 32, cardHeight: Int? = null, cornerRadius: Int = 12,
        elevation: Int = 0, scoreTextStyle: String = "compact",
        showTeamNames: Boolean = false, showLeaders: Boolean = true,
        showBroadcaster: Boolean = true, background: Any? = null,
        liveBackground: Any? = null, badgeColor: String? = null
    )
    ```
    Parse `background`/`liveBackground` into a sealed class: `SolidColor(hex)`, `Gradient(BackgroundGradient)`, `BgImage(url, scaleType, overlay)`.

14. Update `GamePanelUiModel` — replace `variant: String` and `backgroundImageUrl: String?` with `displayConfig: GamePanelDisplayConfig`.

15. Update `mapGamePanel()` — parse `displayConfig` from `data["displayConfig"]`. Apply defaults for any missing fields.

16. **Merge** `StandardGamePanelContent` and `FeaturedGamePanelContent` into a single `GamePanelContent` composable in `GamePanelRenderer.kt`:
    - Read all visual values from `model.displayConfig`
    - Logo size: `model.displayConfig.logoSize.dp`
    - Card height: if non-null, `Modifier.height(config.cardHeight.dp)` else auto
    - Corner radius: `RoundedCornerShape(config.cornerRadius.dp)`
    - Elevation: `CardDefaults.cardElevation(config.elevation.dp)`
    - Score text: if `prominent` use `headlineMedium` + `ExtraBold`, else `bodyLarge` + `Bold`
    - Team names: show/hide based on `config.showTeamNames`
    - Leaders/broadcaster: show/hide based on `config.showLeaders`/`config.showBroadcaster`
    - Background: resolve `config.background` (or `config.liveBackground` when LIVE) into a Compose `Brush`, `Color`, or `AsyncImage` + overlay `Brush`
    - Badge color: `config.badgeColor?.let { parseColor(it) } ?: ...`

17. Remove `FeaturedGamePanelContent` and `StandardGamePanelContent` — dead code after merge.

18. Remove the `if (model.variant == "featured")` branch in `GamePanelRenderer`.

### Phase 4: Web — *parallel with Phase 2+3 after Phase 1*

19. Update `GamePanelUiModel` in `sectionUiAdapters.ts` — replace `variant: string` and `backgroundImageUrl?: string` with `displayConfig: GamePanelDisplayConfig` interface.

20. Update `mapGamePanel()` — parse `displayConfig` from raw data. Apply defaults.

21. **Merge** `StandardGamePanelView` and `FeaturedGamePanelView` into a single `GamePanelView` component in `GamePanel.tsx`:
    - Compute inline styles from `displayConfig` values (logo width/height, borderRadius, minHeight, background CSS)
    - Background image: render as `backgroundImage: url(...)` with `backgroundSize: cover` + overlay via pseudo-element or nested div with gradient
    - Gradient: render as `background: linear-gradient(...)`
    - Solid: render as `background: #hex`
    - `liveBackground` overrides `background` when `visualState === 'LIVE'`

22. Remove `FeaturedGamePanelView`, `StandardGamePanelView`, `standardStyles`, `featuredStyles` — all replaced by config-driven styles.

23. Remove the `if (model.variant === 'featured')` branch in `GamePanel`.

### Phase 5: Cleanup & Verification

24. Remove `variant` enum from schema (already done in step 3). Verify codegen output has no `variant` references.

25. Search codebase for any remaining `variant` references in GamePanel context — remove all.

26. Rebuild and test all screens:
    - Kitchen-sink: verify standard GamePanel, featured GamePanel, and scoreboard all render correctly with displayConfig
    - Scoreboard screen (live data): verify Ably SSE updates still flow and scores update in real-time
    - Game-detail screen: verify scoreboard header renders from displayConfig GamePanel, not AtomicComposite
    - Web: verify same screens render identically

## Relevant Files

**Schema & codegen:**
- `schema/sdui-schema.json` — add `BackgroundImage`, `GamePanelDisplayConfig` definitions; modify `GamePanelData`
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
- `android/sdui-core/src/.../models/AtomicElement.kt` — reuse `BackgroundGradient` model

**Web:**
- `web/src/components/sections/GamePanel.tsx` — merge two views, remove hardcoded style objects
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
8. `python3 prompts/skills/doc-consistency-audit/extract-facts.py` — schema facts still clean

## Scope Boundaries

**Included:**
- Schema: `GamePanelDisplayConfig`, `BackgroundImage` definitions
- Server: all 5 GamePanel builder methods converted to use displayConfig
- Android + Web: unified single-path renderer
- Kitchen-sink AtomicComposite scoreboard → real GamePanel section
- `AtomicCompositeBuilder.buildScoreboardHeader()` removed
- Ably SSE verified working (no changes needed — it's on the Section, not the data)

**Excluded:**
- Other section types — displayConfig pattern could apply (SubscribeHero, SubscribeBanner) but that's a separate task
- iOS — future platform, will pick up schema changes automatically via codegen
- Accessibility — covered by separate plan-accessibility.md
- New visual configs beyond what exists today (e.g., animation, shadow) — add later as needed
