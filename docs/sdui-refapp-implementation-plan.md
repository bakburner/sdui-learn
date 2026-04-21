# SDUI Reference App Integration — Implementation Plan

**Goal:** Extend the SDUI prototype and integrate it into both the iOS and Android video SDK reference apps, replacing hardcoded content discovery with server-driven screens while preserving native video and auth boundaries.

**Prerequisite reading:** [refapp-sdui-comparison.md](refapp-sdui-comparison.md) — gap analysis and replacement map.

---

## Phase 0: Schema & Codegen Foundations

*Resolve schema gaps before any client work begins. All client work depends on these.*

### 0.1 — Add `VideoPlayer` Section Type to Schema (Gap 1, P0)

**File:** `schema/sdui-schema.json`

Add `VideoPlayer` to the `sectionType` enum and define `VideoPlayerData`:

```json
{
  "VideoPlayerData": {
    "type": "object",
    "properties": {
      "playerType": {
        "type": "string",
        "enum": ["game", "vod", "event", "nbaTv", "stream"]
      },
      "contentId": {
        "type": "string",
        "description": "Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs."
      },
      "autoplay": { "type": "boolean", "default": true },
      "capabilities": {
        "type": "array",
        "items": {
          "type": "string",
          "enum": ["pip", "chromecast", "airplay", "backgroundAudio", "fullscreenRotation"]
        },
        "description": "Platform capabilities the player should enable. Extensible — new capabilities added to the enum, not as new boolean fields."
      },
      "displayConfig": {
        "type": "object",
        "properties": {
          "aspectRatio": { "type": "string", "default": "16:9" },
          "height":      { "type": "integer" }
        }
      }
    },
    "required": ["playerType", "contentId"]
  }
}
```

**Design rationale:**
- **`contentId`** replaces `mediaId` / `gameId` / `eventId` — these were mutually exclusive (only one is meaningful per `playerType`), so a single field with the discriminator on `playerType` eliminates silent misuse. The server composes `contentId` knowing the `playerType`; the client passes it to the appropriate SDK method.
- **`capabilities`** replaces `supportsPip` / `supportsChromecast` — an extensible string-enum array follows the same pattern as the schema's existing trigger and action enums. Adding AirPlay, background audio, or any future capability is a schema enum addition, not a new boolean field. The server includes only the capabilities relevant to the requesting platform (via `X-Platform` header).

**Justification:** Platform SDK integration (video SDK owns player lifecycle, DRM, HLS, ad insertion). Same category as `SubscribeHero` (StoreKit / Play Billing) and `AdSlot` (GAM). See comparison doc §4 Gap 1.

**After editing schema:** Run `cd codegen && ./generate.sh` to regenerate Kotlin, Swift, and TypeScript models.

### 0.2 — Add Overlay Composition to Screen (Gap 2, P1)

**File:** `schema/sdui-schema.json`

Add optional `overlays` property to the top-level `Screen` definition:

```json
{
  "overlays": {
    "type": "object",
    "additionalProperties": {
      "$ref": "#/definitions/Section"
    },
    "description": "Named overlay sections the client shows when a trigger condition arises. Keys are developer-defined state names (e.g. 'couchRightsWarning'). Values are AtomicComposite sections."
  }
}
```

Client contract: when an SDK callback or local condition fires, the client looks up the overlay by key and presents it as a platform-appropriate modal/sheet. Server controls the display content; client controls trigger timing and presentation style.

### 0.3 — Define Cross-Platform Icon Vocabulary (Gap 3, P2)

**New file:** `schema/icon-tokens.json`

```json
{
  "tokens": {
    "sdui:play":     { "sf": "play.fill",                       "material": "PlayArrow",       "web": "play_arrow" },
    "sdui:back":     { "sf": "chevron.left",                    "material": "ArrowBack",       "web": "arrow_back" },
    "sdui:settings": { "sf": "gearshape",                       "material": "Settings",        "web": "settings" },
    "sdui:expand":   { "sf": "chevron.down",                    "material": "KeyboardArrowDown","web": "expand_more" },
    "sdui:collapse": { "sf": "chevron.up",                      "material": "KeyboardArrowUp", "web": "expand_less" },
    "sdui:check":    { "sf": "checkmark",                       "material": "Check",           "web": "check" },
    "sdui:warning":  { "sf": "exclamationmark.triangle",        "material": "Warning",         "web": "warning" },
    "sdui:live":     { "sf": "antenna.radiowaves.left.and.right","material": "Sensors",        "web": "sensors" },
    "sdui:person":   { "sf": "person.circle",                   "material": "AccountCircle",   "web": "account_circle" },
    "sdui:close":    { "sf": "xmark",                           "material": "Close",           "web": "close" }
  }
}
```

Clients resolve `icon` fields through a platform-specific lookup. Unknown tokens fall back to `sdui:warning` or are hidden.

### 0.4 — Visual Styling Properties (Ref App Fidelity)

**File:** `schema/sdui-schema.json`

The ref apps use visual effects that the schema cannot currently express. These are leaf property additions — no structural changes.

#### 0.4a — Opacity on `AtomicElement`

Add to `AtomicElement.properties`:

```json
"opacity": { "type": "number", "minimum": 0, "maximum": 1, "default": 1 }
```

Enables duration badge overlays (`opacity: 0.7` on a dark background behind text) and faded states. Live pulse animation is a **renderer guideline** — the server sends `isLive: true`, the renderer applies a platform-appropriate pulse to elements with reduced opacity.

#### 0.4b — Shadow on `Container`

Add `Shadow` definition and reference it from `Container`:

```json
"Shadow": {
  "type": "object",
  "properties": {
    "color":   { "type": "string", "default": "#00000014" },
    "radius":  { "type": "number", "default": 4 },
    "offsetX": { "type": "number", "default": 0 },
    "offsetY": { "type": "number", "default": 2 }
  }
}
```

Add to `AtomicElement.properties`:

```json
"shadow": { "$ref": "#/definitions/Shadow" }
```

Replaces the `elevation: integer` on `GamePanelDisplayConfig` with a richer model. Ref app uses `shadow(color: .black.opacity(0.08), radius: 4, y: 2)` — this matches exactly.

The schema models CSS/SwiftUI shadow semantics (radius + offset). Compose approximates — this is intentional: two of three platforms get exact shadows, Compose gets a reasonable approximation.

Renderer mapping:
- **SwiftUI:** `.shadow(color: Color(hex: shadow.color), radius: shadow.radius, x: shadow.offsetX, y: shadow.offsetY)` — exact match
- **Compose:** `Modifier.shadow(elevation = (shadow.radius * 1.5).dp, ambientColor = parseColor(shadow.color), spotColor = parseColor(shadow.color))` — approximation; Compose `shadow()` takes elevation (not blur radius), so scale accordingly. Offset is not supported by Compose's `shadow()` modifier and is ignored.
- **Web:** `box-shadow: ${offsetX}px ${offsetY}px ${radius}px ${color}` — exact match

#### 0.4c — Badge Positioning on `Container` and `Image`

Named `Badge` (not `Overlay`) to avoid collision with the screen-level `overlays` map from Phase 0.2 — that's for SDK-triggered modals; this is for z-positioned child elements like "LIVE" pills and duration labels.

Add `Badge` definition:

```json
"Badge": {
  "type": "object",
  "properties": {
    "element":   { "$ref": "#/definitions/AtomicElement" },
    "alignment": { "type": "string", "enum": ["topStart", "topCenter", "topEnd", "centerStart", "center", "centerEnd", "bottomStart", "bottomCenter", "bottomEnd"], "default": "bottomEnd" }
  },
  "required": ["element"]
}
```

Add to `AtomicElement.properties`:

```json
"badge": { "$ref": "#/definitions/Badge" }
```

Enables elements positioned over images (e.g., "LIVE" pill at bottom-right of a thumbnail, duration label at bottom-right of a VOD card). The ref app uses `.overlay(alignment: .bottomTrailing) { badge }` — this maps directly.

Renderer mapping:
- **SwiftUI:** `.overlay(alignment: mapAlignment(badge.alignment)) { AtomicRouter(badge.element, ...) }`
- **Compose:** `Box { content; Box(Modifier.align(mapAlignment(badge.alignment))) { AtomicRouter(badge.element, ...) } }`
- **Web:** `position: relative` parent + `position: absolute` child with `top/bottom/left/right` from alignment

#### 0.4d — Text Alignment on `Text`

Add to the `Text`-relevant properties (within `AtomicElement`):

```json
"textAlign": { "type": "string", "enum": ["start", "center", "end"], "default": "start" }
```

Used for centered error messages, centered headings, and right-aligned numeric values.

#### 0.4e — Scroll Indicators on `ScrollContainer`

Add to `ScrollContainer`-relevant properties:

```json
"showIndicators": { "type": "boolean", "default": false }
```

Ref app carousels hide scroll indicators for clean visual presentation. Default `false` matches the most common design pattern.

#### 0.4f — Monospaced Digits on `Text`

Add to `Text`-relevant properties:

```json
"monospacedDigits": { "type": "boolean", "default": false }
```

Prevents game clock and score text from shifting left/right as digits change width. Alternatively, the existing `"score"` typography variant can imply monospaced digits in the renderer — but an explicit flag gives server control over any numeric text.

Renderer mapping:
- **SwiftUI:** `.monospacedDigit()`
- **Compose:** `FontFamily.Monospace` or `fontFeatureSettings = "tnum"`
- **Web:** `font-variant-numeric: tabular-nums`

### 0.5 — Renderer Animation Guidelines (No Schema Changes)

These visual effects are **renderer responsibilities**, not schema properties. Document as guidelines for all clients:

| Effect | Server sends | Renderer does |
|---|---|---|
| **Live pulse** | `isLive: true` on GamePanel data | Animate opacity 0.3→1.0 on the live indicator element, repeating with autoreversal. iOS: `.animation(.easeInOut(duration: 0.8).repeatForever(autoreverses: true))`. Compose: `infiniteTransition.animateFloat()`. |
| **Numeric transitions** | Updated score/clock via SSE data binding | Apply content transition on value change. iOS: `.contentTransition(.numericText())`. Compose: `AnimatedContent`. Web: CSS `transition`. |
| **Color mixing** | Pre-computed hex colors | Server computes gradient tints from team colors at composition time. No runtime color math. |
| **Image load states** | `imageUrl` + `fallbackUrl` on Image elements | Renderer shows loading placeholder → success image, or falls back to `fallbackUrl` on failure. iOS: `AsyncImage { phase in }`. Compose: Coil `AsyncImage`. |
| **Pull-to-refresh** | `refreshPolicy: { type: "poll" }` on screen | Renderer adds platform pull-to-refresh gesture. iOS: `.refreshable {}`. Compose: `pullRefresh()`. |

### 0.6 — Regenerate All Models

```bash
cd codegen && ./generate.sh
```

Verify output in `codegen/output/{kotlin,swift,typescript}`.

### 0.7 — Validate Generated Models Against Real Server JSON

Before building renderers on top of the generated models, verify they actually deserialize:

1. Start the SDUI server locally.
2. `curl http://localhost:8080/sdui/for-you > test-response.json`
3. **Swift:** Write a minimal test that loads `test-response.json` and calls `JSONDecoder().decode(SduiModels.self, from: data)`. Verify all section types, atomic elements, and the new `VideoPlayer` / `overlays` / `badge` / `shadow` / `opacity` fields round-trip correctly.
4. **Kotlin:** Run the existing Android test suite against the updated models.
5. **TypeScript:** Run the existing web build to confirm type-checking passes.

Fix any deserialization issues (enum mismatches, missing fields, incorrect nesting) before proceeding to Phase 1.

Commit generated output alongside schema changes.

**Phase 0 deliverables:** Updated schema (VideoPlayer section, overlays, icon tokens, 6 styling properties), renderer animation guidelines, regenerated and validated models for all three platforms.

---

## Phase 1: SDUI Prototype Server Enhancements

*All work in this phase lives in the SDUI prototype's `server/` directory. Test independently via `curl`, the existing web client, and the existing Android SDUI client — no ref app code is touched.*

### 1.0 — API Spike: Investigate NBA CMS/VOD Endpoint Coverage

Before building VideoFeedComposer, verify what the existing `StatsApiClient` can access:

- Does it reach the CMS/featured-feed endpoints that the iOS ref app's `LiveCMSService` calls?
- Does it reach VOD/content metadata endpoints?
- Are any new API client methods or auth configurations needed?

Outcome: a clear list of endpoints available, any new `StatsApiClient` methods needed, and whether VOD data can be served or must be mocked initially.

### 1.1 — Video Feed Composer

**New file:** `server/src/main/java/com/nba/sdui/service/VideoFeedComposer.java`

*Depends on 1.0 — scope may be reduced if CMS/VOD endpoints aren't accessible.*

Composes the `nba://for-you` screen with:

- **Game carousel** — `ScrollContainer` (horizontal) containing `GamePanel` sections for live/upcoming games. Server fetches from NBA Core API.
- **Featured content rail** — `ScrollContainer` (horizontal) with AtomicComposite cards (image + title + duration badge). Uses the new `opacity` and `badge` styling from Phase 0.4.
- **VOD playlist** — `Container` (vertical) with VOD card AtomicComposites (thumbnail, title, duration). Cards use `shadow`, `cornerRadius`, and image `badge` for positioning.

Section ordering, content mix, and card layouts are fully server-controlled.

**Data sources:** NBA Core API endpoints, configured in `application.properties`. If 1.0 reveals missing endpoints, use mock/static data initially and flag for follow-up.

**Verify:** `curl http://localhost:8080/sdui/for-you` returns valid SDUI response. Load in web client and Android SDUI client to confirm rendering of new styling properties (shadows, badges, opacity).

### 1.2 — Schedule Composer

**New file:** `server/src/main/java/com/nba/sdui/service/ScheduleComposer.java`

Composes the schedule browsing screen using:

- **Form section** with season/month/day pickers (the `Form` section type already supports parameterized refresh — perfect for hierarchical drill-down).
- **Game list** — vertical `Container` of game items as AtomicComposites, each with a `navigate` action to `nba://game/{gameId}`.

The server calls NBA schedule APIs directly.

**Verify:** `curl http://localhost:8080/sdui/schedule` returns valid SDUI response. Confirm Form → parameterized refresh → updated game list works in web client.

### 1.3 — Stream Info Composer (conditional)

**New file:** `server/src/main/java/com/nba/sdui/service/StreamInfoComposer.java`

If the server can access stream metadata APIs:

- Compose stream list as AtomicComposite rows (stream type, resolution, language)
- Each row has `navigate` action to `nba://stream/{streamId}`

If stream APIs require on-device auth tokens that can't proxy through the server, this composer is deferred.

**Verify:** `curl http://localhost:8080/sdui/streams/{gameId}` returns stream list.

### 1.4 — Game Detail Composer Enhancement

**Existing file:** `server/src/main/java/com/nba/sdui/service/GameDetailComposer.java`

Extend to include the new `VideoPlayer` section and overlays:

```
Screen {
  sections: [
    VideoPlayer { playerType: "game", contentId: "00123", autoplay: true, capabilities: ["pip"] },
    GamePanel { ... },
    TabGroup {
      tabs: [
        { label: "Box Score", sections: [BoxscoreTable] },
        { label: "Highlights", sections: [ScrollContainer of VOD cards] }
      ]
    }
  ],
  overlays: {
    "couchRightsWarning": AtomicComposite { ... },
    "couchRightsExpired": AtomicComposite { ... },
    "unentitled": AtomicComposite { ... }
  }
}
```

**Verify:** `curl http://localhost:8080/sdui/game-detail/00123` returns response with `VideoPlayer` section, `overlays` map, and `TabGroup` with nested content. Web client renders everything except the VideoPlayer section (which it skips gracefully per Rule 7). Android SDUI client renders the same.

### 1.5 — Player-Adjacent Content Pattern (Gap 6)

Within `GameDetailComposer`, the highlights tab uses existing SDUI primitives — no new schema:

1. **`ScrollContainer`** of VOD card `AtomicComposite`s (each card shows thumbnail, title, duration).
2. Each card carries a **`mutate` action** → `{ "targetPath": "screenState.selectedHighlight", "value": "<vodId>" }`.
3. The `VideoPlayer` section's `contentId` is a **`Conditional`** expression: `{ "source": "screenState.selectedHighlight", "fallback": "<defaultVodId>" }`.
4. When the user taps a card, the client's `ActionDispatcher` executes the `mutate`, the `Conditional` re-evaluates, and the `VideoPlayer` renderer reloads with the new content.

This resolves Gap 6 without cross-section state binding — `mutate` + `Conditional` are already in the schema.

**Verify:** `curl` the game-detail response. Confirm each VOD card has a `mutate` action with `targetPath: "screenState.selectedHighlight"` and the `VideoPlayer` section's `contentId` is a `Conditional` referencing the same state key.

### 1.6 — Update Existing Android SDUI Client Renderers for New Styling

Update renderers in `android/sdui-core/` to support the Phase 0.4 schema additions:

- `AtomicContainer.kt` — add `shadow` modifier, `overlay` child positioning
- `AtomicText.kt` — add `textAlign`, `monospacedDigits`
- `AtomicImage.kt` — add `opacity`, `overlay`
- `AtomicScrollContainer.kt` — add `showIndicators` flag
- `AtomicRouter.kt` — pass through `opacity`
- `SectionRouter.kt` — add `VideoPlayer` to the type dispatch (stub renderer that displays section data as a placeholder card until SDK integration)

**Verify:** Run the Android SDUI demo app. Confirm new styling properties render correctly on the video feed and game detail screens from Phase 1.1–1.4.

### 1.7 — Update Web Client Renderers for New Styling

Update renderers in `web/src/components/` to support the Phase 0.4 schema additions:

- Atomic renderers — add CSS equivalents for `opacity`, `box-shadow`, `text-align`, `font-variant-numeric: tabular-nums`, badge absolute positioning
- `SectionRouter.tsx` — add `VideoPlayer` to the switch (stub renderer showing a placeholder)

**Verify:** Run the web client. Confirm all new screens render with correct styling.

**Phase 1 deliverables:** 3 new composers + 1 enhanced composer + updated renderers on Android and web for new styling properties. All verified independently against the running SDUI server — no ref app code touched.

---

## Phase 2: iOS SDUI Runtime (Build in Prototype)

*Build the iOS SDUI client inside the prototype repo as `clients/ios/`, parallel to `android/sdui-core/` and `web/`. Includes a standalone demo app for testing against the SDUI server — no ref app dependency. See comparison doc §8 for the SwiftUI ↔ Compose mapping reference.*

### 2.1 — Project Structure

Create `clients/ios/` in the SDUI prototype with a Swift Package (`SduiCore`) and a lightweight demo app:

```
clients/ios/
  SduiCore/                        ← Swift Package (importable by any iOS app)
    Package.swift
    Sources/SduiCore/
      Models/
        SduiModels.swift           ← copy from codegen/output/swift/
      Network/
        SduiRepository.swift       ← URLSession, fetchScreen(path:)
        UriResolver.swift          ← nba:// → /sdui/ prefix swap
      Rendering/
        SduiScreenView.swift       ← top-level screen host
        SectionRouter.swift        ← dispatches section.type → renderer
        Atomic/
          AtomicRouter.swift       ← dispatches element.uiType → renderer
          AtomicContainer.swift    ← HStack / VStack with flex, background, padding
          AtomicText.swift         ← Text + font/color/lineLimit mapping
          AtomicImage.swift        ← AsyncImage + fallback
          AtomicButton.swift       ← Button + style variants
          AtomicSpacer.swift       ← Spacer with fixed frame
          AtomicDivider.swift      ← Divider / Rectangle for vertical
          AtomicScrollContainer.swift ← ScrollView + LazyHStack/LazyVStack
          AtomicConditional.swift  ← if/else on state condition
          AtomicDisplayGrid.swift  ← Grid / LazyVGrid
          AtomicSectionSlot.swift  ← delegate to SectionRouter
        Sections/
          GamePanelRenderer.swift
          VideoPlayerStub.swift    ← placeholder card (no SDK — just shows contentId + playerType)
      State/
        ActionDispatcher.swift     ← navigate / mutate / refresh / dismiss / toast / fireAndForget
        StateManager.swift         ← @Observable screen state
      Helpers/
        SduiHelpers.swift          ← Color(hex:), icon token resolver, accessibility
  SduiDemo/                        ← standalone demo app (like Android's demo app)
    SduiDemoApp.swift              ← @main, points at localhost SDUI server
    ContentView.swift              ← NavigationStack + SduiScreenView
```

The `SduiCore` package has **no dependency on NBAVideoKit or any ref app code**. The `VideoPlayerStub` renders a placeholder card showing the player type and content ID — enough to verify the server response is correct. Ref app integration (Phase 3) will swap the stub for a real SDK wrapper.
```

### 2.2 — Network Layer (~70 LOC)

**`SduiRepository.swift`** — Reference: `android/sdui-core/.../data/SduiRepository.kt`

```swift
@Observable
class SduiRepository {
    private let baseURL: URL
    private let platform = "ios"

    func fetchScreen(path: String) async throws -> SduiScreen {
        var request = URLRequest(url: baseURL.appending(path: path))
        request.setValue(platform, forHTTPHeaderField: "X-Platform")
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(SduiScreen.self, from: data)
    }
}
```

**`UriResolver.swift`** — Simple prefix swap. No special-case branching (per AGENTS.md Rule 10):

```swift
func resolveEndpoint(uri: String) -> String {
    uri.replacingOccurrences(of: "nba://", with: "/sdui/")
}
```

### 2.3 — Rendering Core (~500 LOC)

Each atomic renderer translates directly from its Android counterpart. The comparison doc §8 has the complete mapping table. Key files:

**`AtomicRouter.swift`** — Reference: `AtomicRouter.kt`

```swift
@ViewBuilder
func AtomicRouter(element: AtomicElement, screenState: ScreenState, onAction: @escaping (Action) -> Void, depth: Int = 0) -> some View {
    if depth > 6 { EmptyView() }
    else {
        switch element.uiType {
        case .container:       AtomicContainer(element: element, screenState: screenState, onAction: onAction, depth: depth)
        case .text:            AtomicText(element: element)
        case .image:           AtomicImage(element: element)
        case .button:          AtomicButton(element: element, onAction: onAction)
        case .spacer:          AtomicSpacer(element: element)
        case .divider:         AtomicDivider(element: element)
        case .scrollContainer: AtomicScrollContainer(element: element, screenState: screenState, onAction: onAction, depth: depth)
        case .conditional:     AtomicConditional(element: element, screenState: screenState, onAction: onAction, depth: depth)
        case .displayGrid:     AtomicDisplayGrid(element: element)
        case .sectionSlot:     AtomicSectionSlot(element: element, screenState: screenState, onAction: onAction)
        default:               EmptyView() // Graceful skip — unknown type
        }
    }
}
```

**`AtomicContainer.swift`** — Reference: `AtomicContainer.kt` (164 LOC Android). The most complex atomic renderer:

- Read `direction` (row / column) → `HStack` / `VStack`
- Apply `background` (solid / gradient / image) via `.background()`
- Apply `padding` via `.padding(EdgeInsets(...))`
- Apply `cornerRadius` via `.clipShape(RoundedRectangle(...))`
- Iterate `children`, rendering each through `AtomicRouter` at `depth + 1`
- Apply flex via `.frame(maxWidth: .infinity)` for equal distribution

**`SduiScreenView.swift`** — Reference: `SduiScreenComposable.kt`. The top-level host:

```swift
struct SduiScreenView: View {
    let endpoint: String
    @State private var screen: SduiScreen?
    @State private var error: Error?
    @State private var stateManager = StateManager()

    var body: some View {
        if let screen {
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(screen.sections, id: \.id) { section in
                        SectionRouter(section: section, screenState: stateManager.state, onAction: handleAction)
                    }
                }
            }
        } else if let error {
            SduiErrorView(message: error.localizedDescription) {
                self.error = nil
                self.screen = nil   // triggers .task reload
            }
        } else {
            ProgressView()
                .task {
                    do {
                        screen = try await SduiRepository().fetchScreen(path: endpoint)
                    } catch {
                        Logger.sdui.error("SduiScreenView: fetch failed for \(endpoint): \(error)")
                        self.error = error
                    }
                }
        }
    }
}
```

> Per AGENTS.md Rules 6 & 12: errors produce a visible `SduiErrorView` (server-composed `ErrorState` when available, client fallback otherwise) and are always logged with context. Silent `try?` swallowing is prohibited.

### 2.4 — Section Renderers

**`SectionRouter.swift`** — Routes to the appropriate renderer. Unknown types are skipped with a debug log (per AGENTS.md Rule 7):

- `AtomicComposite` → `AtomicRouter(section.data.ui, ...)`
- `GamePanel` → `GamePanelRenderer` (existing game card logic, ~120 LOC)
- `VideoPlayer` → `VideoPlayerStub` (placeholder card, no SDK dependency)
- `TabGroup`, `Form`, `BoxscoreTable`, etc. → built iteratively in later phases

**`VideoPlayerStub.swift`** — Placeholder for testing without a video SDK dependency:

```swift
struct VideoPlayerStub: View {
    let data: VideoPlayerData

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: "play.rectangle.fill")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("VideoPlayer: \(data.playerType)")
                .font(.headline)
            Text("Content: \(data.contentId)")
                .font(.caption)
                .foregroundStyle(.secondary)
            if let caps = data.capabilities, !caps.isEmpty {
                Text("Capabilities: \(caps.joined(separator: ", "))")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(parseAspectRatio(data.displayConfig?.aspectRatio), contentMode: .fit)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
```

This is sufficient to verify the full server → client pipeline for VideoPlayer sections. The ref app integration phase (Phase 3) swaps this stub for a real `NBAVideoKit` wrapper.

### 2.5 — Action & State Layer (~130 LOC)

**`ActionDispatcher.swift`** — Reference: `ActionHandler.kt`. Handles all 6 action types:

| Action | iOS Implementation |
|---|---|
| `navigate` | Push onto `NavigationStack` path, or `UIApplication.shared.open(url:)` for deep links |
| `fireAndForget` | `URLSession.shared.data(for:)` fire-and-forget (analytics) |
| `mutate` | Update key in `StateManager` |
| `refresh` | Re-call `fetchScreen()` or fetch `directUrl` |
| `dismiss` | `dismiss()` environment action |
| `toast` | Custom toast overlay or `UINotificationFeedbackGenerator` |

**`StateManager.swift`** — Reference: `StateManager.kt`. `@Observable` class holding `[String: Any]` screen state. `mutate` actions update keys; `Conditional` elements read keys.

### 2.6 — Helpers (~40 LOC)

- `Color(hex:)` extension for parsing hex color strings from server
- Icon token resolver: read `schema/icon-tokens.json` mapping, return SF Symbol name
- Accessibility modifiers: map `AccessibilityProperties` → `.accessibilityLabel()`, `.accessibilityAddTraits()`, `.accessibilityHidden()`

### 2.7 — Polling Refresh (~40 LOC)

**`RefreshPolicyHandler.swift`** — Reference: `useRefreshPolicy.ts` (web). For sections with `refreshPolicy.type = "poll"`, schedule a `Task.sleep(for:)` loop that re-fetches the section's `directUrl` at the server-specified interval:

```swift
func startPolling(for section: Section) async {
    guard let policy = section.refreshPolicy, policy.type == "poll",
          let interval = policy.intervalMs, let url = policy.directUrl else { return }
    while !Task.isCancelled {
        try? await Task.sleep(for: .milliseconds(interval))
        do {
            let updated = try await SduiRepository().fetchRawJson(url: url)
            await MainActor.run { sectionStore.update(section.id, with: updated) }
        } catch {
            Logger.sdui.warning("Poll failed for section \(section.id): \(error)")
        }
    }
}
```

### 2.8 — Ably SSE Real-Time (~100 LOC)

**`AblyChannelManager.swift`** (~60 LOC) — Reference: `AblyChannelManager.kt`. Subscribe to channels specified by `refreshPolicy.sseChannel`. Incoming messages are opaque `[String: Any]` (per AGENTS.md Rule 4).

**`DataBindingResolver.swift`** (~40 LOC) — Reference: `DataBindingResolver.kt`. The single code path for applying real-time messages to section data via `dataBindings` source → target path mappings:

```swift
func applyBindings(message: [String: Any], section: Section) -> Section {
    var updatedData = section.data
    for binding in section.dataBindings ?? [] {
        if let value = message[keyPath: binding.sourcePath] {
            updatedData.set(keyPath: binding.targetPath, value: value)
        }
    }
    return section.with(data: updatedData)
}
```

**Phase 2 deliverables:** ~990 LOC iOS SDUI runtime as `SduiCore` Swift Package in `clients/ios/`. All 10 atomic renderers + SectionRouter + network + actions + state + polling + SSE + VideoPlayer stub. Verified by running the `SduiDemo` app against the local SDUI server — loads all screens from Phase 1 (video feed, schedule, game detail) and renders them with correct styling. Polling and SSE update live sections in real-time. No ref app dependency.

---

## Phase 3: iOS Ref App Integration

*Wire the new SDUI runtime into the existing ref app, replacing hardcoded feed and content layers.*

### 3.1 — Replace Feed Layer

| Remove | Replace With |
|---|---|
| `FeedView.swift` | `SduiScreenView(endpoint: "/sdui/for-you")` |
| `FeedViewModel.swift` | Deleted — `SduiRepository.fetchScreen()` replaces dual-source fetch |
| `GameCarouselView.swift` | Deleted — server composes `ScrollContainer` with `GamePanel` children |
| `GameListView.swift` | Deleted — server composes vertical `Container` |
| `VODCarouselView.swift` | Deleted — server composes `ScrollContainer` with video cards |
| `VODCardView.swift` | Deleted — server composes AtomicComposite card |
| `VODPlaylistView.swift` | Deleted — server composes vertical list with `Divider` |

### 3.2 — Replace Models and Services

| Remove | Replace With |
|---|---|
| `RefAppFeed.swift` | Codegen `SduiModels.swift` |
| `RefAppGameCard.swift` | Codegen models — server provides `teamLogoUrl` directly |
| `RefAppVODCard.swift` | Codegen models |
| `CMSService.swift` | `SduiRepository` protocol |
| `LiveCMSService.swift` | Deleted — API keys and endpoints move to server config |
| `MockCMSService.swift` | `SduiRepository` with mock JSON (load from bundle for previews) |

### 3.3 — Replace GameCardView

`GameCardView.swift` (~200 LOC) is mostly replaced by `GamePanel` AtomicComposite composition. The ~40 LOC client-side clock timer moves to `GamePanelRenderer.swift` as a rendering quality feature (see comparison doc §4 Gap 4).

### 3.4 — Update HomeView

`HomeView.swift` keeps its toolbar, tab bar, and auth gating. The body content changes from `FeedView()` to `SduiScreenView(endpoint: resolveEndpoint(selectedTab.uri))`. Tab URIs come from the server's `navigation` payload (per AGENTS.md Rule 2).

### 3.5 — Update Navigation

Replace typed `NavigationStack` destinations with a generic SDUI endpoint pattern:

```swift
NavigationStack(path: $router.path) {
    SduiScreenView(endpoint: "/sdui/for-you")
        .navigationDestination(for: String.self) { endpoint in
            SduiScreenView(endpoint: endpoint)
        }
}
```

The `ActionDispatcher` pushes endpoint strings onto the navigation path when handling `navigate` actions.

### 3.6 — Add Real VideoPlayerRenderer (Swap Stub for SDK Wrapper)

Replace `VideoPlayerStub` with a real renderer that wraps `NBAVideoKit`:

```swift
struct VideoPlayerRenderer: View {
    let data: VideoPlayerData
    let overlays: [String: Section]?
    @State private var overlayKey: String?

    var body: some View {
        ZStack {
            NBAVideoKitPlayer(playerType: data.playerType, contentId: data.contentId, ...)
                .aspectRatio(parseAspectRatio(data.displayConfig?.aspectRatio), contentMode: .fit)
                .onCouchRightsCallback { state in
                    overlayKey = state.rawValue
                }
            if let key = overlayKey, let overlay = overlays?[key] {
                OverlaySheet {
                    SectionRouter(section: overlay, ...)
                }
            }
        }
    }
}
```

This lives in the ref app (not in `SduiCore`) since it depends on `NBAVideoKit`. The `SduiCore` package's `SectionRouter` accepts an optional `sectionRendererOverrides` dictionary so the ref app can inject its real `VideoPlayerRenderer` at the integration point.

### 3.7 — Preserve Native Boundaries

These files are **not touched** — they remain native:

| File | Reason |
|---|---|
| `VideoView.swift` / `VideoViewModel.swift` | NBAVideoKit SDK integration (until `VideoPlayerRenderer` is stable) |
| `LoginFormView.swift` / `LoginViewModel.swift` / `ProfileView.swift` | CIAM auth — security boundary |
| `SettingsView.swift` / `VideoConfigSheet.swift` | Developer tooling |
| `CouchRightsSheet.swift` | Moves to server-composed overlay (Phase 5) |

**Phase 3 deliverables:** iOS ref app running with server-driven feed. ~890 LOC removed, ~20 LOC integration glue added. Video and auth unchanged.

---

## Phase 4: Android Ref App Integration

*Import the existing `sdui-core` library and replace hardcoded screens.*

### 4.1 — Add sdui-core Dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":sdui-core"))
}
```

Copy  or symlink the `sdui-core` module from the SDUI prototype into the Android ref app. The library is already built with all 10 atomic renderers + 9 section renderers.

### 4.2 — Wire SduiRepository into Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SduiModule {
    @Provides @Singleton
    fun provideSduiRepository(okHttpClient: OkHttpClient): SduiRepository =
        SduiRepository(okHttpClient, baseUrl = BuildConfig.SDUI_BASE_URL)
}
```

### 4.3 — Replace Schedule Layer

| Remove | Replace With |
|---|---|
| `ScheduleScreen.kt` | `SduiScreenView(endpoint = "/sdui/schedule")` |
| `ScheduleViewModel.kt` | Deleted — server `Form` section handles season/month/day drill-down |
| `Season.kt`, `Month.kt`, `DayItem.kt` | Deleted — server provides picker options |

### 4.4 — Replace Button Dashboard (UserInfoScreen)

The current `UserInfoScreen` is a grid of hardcoded buttons for launching video players. Replace with server-composed content:

| Current | SDUI Replacement |
|---|---|
| "Watch Game" button | AtomicComposite card with `navigate` action → `nba://game/{id}` |
| "Watch Event" button | AtomicComposite card with `navigate` action → `nba://event/{id}` |
| "Watch NBATV" button | AtomicComposite card with `navigate` action → `nba://nbatv` |
| "Browse Schedule" button | AtomicComposite card with `navigate` action → `nba://schedule` |

Server-composed cards can include images, descriptions, and dynamic content IDs — no app release needed to change game IDs.

### 4.5 — Add VideoPlayer Section Renderer

**New file in the ref app module** (not sdui-core — keeps the SDK-agnostic library free of NBA Video SDK dependency): `VideoPlayerRenderer.kt`

Wraps existing SDK Composables:

```kotlin
@Composable
fun VideoPlayerRenderer(section: Section, screenState: ScreenState, onAction: (Action) -> Unit) {
    val data = section.data as VideoPlayerData
    val hasPip = data.capabilities?.contains("pip") == true
    val hasChromecast = data.capabilities?.contains("chromecast") == true
    when (data.playerType) {
        "game"   -> GamePlayerWithStreamSelector(gameId = data.contentId, supportsPip = hasPip, ...)
        "vod"    -> VodPlayerWithStreamSelector(mediaId = data.contentId, ...)
        "event"  -> EventPlayerWithStreamSelector(eventId = data.contentId, ...)
        "nbaTv"  -> NbaTvPlayer(supportsChromecast = hasChromecast, ...)
        "stream" -> VideoPlayer(streamUrl = data.contentId, ...)
    }
}
```

Register via **override injection** (consistent with iOS Phase 3.6) — `sdui-core`'s `SectionRouter` accepts a `sectionRendererOverrides` map so host apps can supply SDK-dependent renderers without modifying the library:

```kotlin
// In the ref app's DI module:
val overrides = mapOf(
    SectionType.VIDEO_PLAYER to @Composable { section, state, onAction ->
        VideoPlayerRenderer(section, state, onAction)
    }
)

// SduiScreenView passes overrides to SectionRouter:
SectionRouter(section, screenState, onAction, sectionRendererOverrides = overrides)
```

### 4.6 — Update Navigation

Replace `NavHost` string routes with generic SDUI endpoint routing:

```kotlin
NavHost(navController, startDestination = "sdui/{endpoint}") {
    composable("sdui/{endpoint}") { backStackEntry ->
        val endpoint = backStackEntry.arguments?.getString("endpoint") ?: "/sdui/for-you"
        SduiScreenView(endpoint = endpoint, ...)
    }
}
```

`ActionHandler` handles `navigate` actions by pushing endpoints onto the NavController.

### 4.7 — Preserve Native Boundaries

| Component | Reason |
|---|---|
| `GamePlayerActivity` + 5 player Composables | Video SDK integration — `VideoPlayerRenderer` wraps these |
| `CiamApi` + auth flow | Security boundary |
| Hilt DI modules | Framework infrastructure |

**Phase 4 deliverables:** Android ref app running with server-driven screens. ~780-1240 LOC removed, ~50 LOC integration glue added. `sdui-core` library imported, not rebuilt.

---

## Phase 5: Cross-Cutting Enhancements

*Improvements that benefit both platforms after the core integration is stable.*

### 5.1 — Overlay Composition (Gap 2)

**Server:** Enhance `GameDetailComposer` to include `overlays` map with couch rights variants.

**iOS:** Present overlays as `.sheet()` when `VideoPlayerRenderer` triggers `overlayKey`.

**Android:** Present overlays as `ModalBottomSheet` when video SDK callback fires.

**Result:** Remove `CouchRightsSheet.swift` (iOS, 50 LOC) and `VideoBottomSheet.kt` (Android, ~60 LOC). Overlay content becomes server-composed — text, images, and actions can change without app releases.

### 5.2 — Icon Token Resolution (Gap 3)

**iOS:** Add `SduiIconResolver.swift` (~20 LOC) that maps `sdui:play` → `Image(systemName: "play.fill")`.

**Android:** Add `SduiIconResolver.kt` (~20 LOC) that maps `sdui:play` → `Icons.Default.PlayArrow`.

**Web:** Add `SduiIconResolver.ts` (~15 LOC) that maps `sdui:play` → Material Icon name.

Server can now include icons in AtomicComposite buttons and navigation items.

### 5.3 — Clock Interpolation Guideline (Gap 4)

**Publish as renderer guideline** (not schema change):

> When `GamePanelData.clockRunning == true`, the `GamePanelRenderer` should start a 1-second local timer that decrements the displayed game clock value. Stop the timer when an SSE update arrives (which provides the authoritative clock value) or when `clockRunning` flips to `false`. Apply platform-appropriate content transition animation to the clock text.

Add `clockRunning: Boolean` to `GamePanelData` in the schema if not already present.

### 5.4 — Animation Guidelines (Gap 5)

**Publish as renderer guideline:**

> - When a bound numeric value changes (e.g., score), apply platform-appropriate content transition: `.contentTransition(.numericText())` on iOS, `AnimatedContent` on Android.
> - When a live indicator is present (`isLive == true`), apply a pulse animation to the indicator element.
> - Transitions should use 300ms duration with ease-in-out timing.

---

## Phase 6: Validation & Cleanup

### 6.1 — End-to-End Smoke Tests

| Test | Expected Result |
|---|---|
| Launch iOS ref app → Home tab | Server-composed feed loads with game carousels and VOD rails |
| Tap game card (iOS) | Navigates to game detail with inline `VideoPlayer` + `GamePanel` + tabs |
| Launch Android ref app → Home | Server-composed content cards replace button grid |
| Tap "Browse Schedule" (Android) | SDUI `Form` section with date pickers, game list below |
| Modify server game IDs | Both apps show updated content without rebuild |
| Add new AtomicComposite section on server | Both apps render new section without app update |
| Kill server → launch app | Error state section displayed (not blank screen, not crash) |
| Unknown section type in response | Skipped silently with debug log (both platforms) |

### 6.2 — Delete Dead Code

**iOS — remove these files after SDUI integration is verified:**

- `FeedView.swift`, `FeedViewModel.swift`
- `GameCarouselView.swift`, `GameListView.swift`
- `VODCarouselView.swift`, `VODCardView.swift`, `VODPlaylistView.swift`
- `RefAppFeed.swift`, `RefAppGameCard.swift`, `RefAppVODCard.swift`
- `CMSService.swift`, `LiveCMSService.swift`, `MockCMSService.swift`
- `CoreAPIModels.swift`, `FeaturedFeedModels.swift`
- `mock_featured_feed.json`, `mock_games_feed.json`

**Android — remove (if applicable):**

- `ScheduleScreen.kt`, `ScheduleViewModel.kt`
- `Season.kt`, `Month.kt`, `DayItem.kt`
- Hardcoded button definitions in `UserInfoScreen.kt`

### 6.3 — Update Documentation

- Update `docs/client-implementors-contract.md` with iOS-specific notes from the build
- Add iOS to the list of supported clients in project README
- Document the `VideoPlayer` section type in the schema documentation
- Document the overlay composition pattern

---

## Summary: Effort by Phase

| Phase | Scope | New LOC | LOC Removed | Duration Dependency |
|---|---|---|---|---|
| **0 — Schema** | Schema + codegen + styling properties + model validation | ~80 (schema edits) | 0 | None — start here |
| **1 — Prototype Server + Client Updates** | API spike + 3 new + 1 enhanced composer, Android + web renderer updates for new styling | ~800 | 0 | Phase 0 |
| **2 — iOS Runtime (in prototype)** | SwiftUI SDUI rendering library + demo app + polling + SSE | ~990 | 0 | Phase 0 |
| **3 — iOS Ref App Integration** | Import `SduiCore`, wire into ref app, swap VideoPlayer stub for SDK wrapper | ~100 (glue + real renderer) | ~890 | Phase 1 + 2 |
| **4 — Android Ref App Integration** | Import sdui-core + glue, swap VideoPlayer stub for SDK wrapper | ~120 (renderer + glue) | ~780–1240 | Phase 1 |
| **5 — Cross-Cutting** | Overlay composition, icon resolution, clock/animation guidelines | ~100 | ~110 | Phase 3 + 4 |
| **6 — Validation** | Tests + cleanup + docs | ~0 | ~100 (dead code) | Phase 5 |
| **Total** | | **~2,190** | **~1,880–2,340** | |

### Parallelization

Phases 0–2 are **pure SDUI prototype work** — no ref app code touched. You can verify everything independently before moving to integration.

- **Phase 1** (server + existing client updates) and **Phase 2** (iOS runtime) can run in parallel — both depend only on Phase 0.
- **Phase 3** (iOS ref app) and **Phase 4** (Android ref app) can run in parallel — each depends on Phase 1 plus its platform runtime.
- **Phase 5** depends on both platforms being integrated.

```
Phase 0 (Schema)                                          SDUI PROTOTYPE ONLY
    ├── Phase 1 (Server + Android/Web renderer updates)  ─────────────────────
    │                                                      ────────────────────
    └── Phase 2 (iOS Runtime + Demo App)                  ─────────────────────
                                                           ════════════════════
         Phase 1 + 2 complete → verify all screens in       REF APP INTEGRATION
         web client, Android demo, iOS demo independently
                                                           ════════════════════
              ├── Phase 3 (iOS Ref App Integration)  ──┐
              └── Phase 4 (Android Ref App Integration) ├── Phase 5 ── Phase 6
```

### Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Swift codegen models don't deserialize correctly | Medium | High | Test `JSONDecoder` against real server responses early in Phase 2 |
| NBAVideoKit API surface doesn't fit `VideoPlayerRenderer` contract | Low | High | VideoPlayer stub in Phase 2 verifies the data contract; real SDK wrapper deferred to Phase 3 |
| Server can't access stream metadata APIs (blocks Stream Info Composer) | Medium | Low | Defer Phase 1.3; Android stream layer stays native |
| `LiteweightCIAMService` auth tokens needed for SDUI requests | Low | Medium | Proxy auth header through `SduiRepository.fetchScreen()` |
| AtomicComposite depth/node limits hit by complex feed layouts | Low | Medium | Monitor at composition time; split into multiple sections if needed |
