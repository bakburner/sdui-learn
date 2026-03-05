# PLAN — Generic URI Routing & Demo Screens

> **Goal**: Eliminate hardcoded screen-type enums so new SDUI screens require
> **zero client changes** — then prove it by adding demo screens purely on the
> server.

---

## Background

Both clients (Web and Android) currently match on a finite set of screen types
(`scoreboard`, `game-detail`).  Adding a new tab or screen requires:

1. A server endpoint + composition method *(expected)*
2. Client-side enum additions, route branches, variant maps, title fallbacks,
   back-button guards, and refresh logic *(should be unnecessary)*

Point (2) undermines SDUI's core value.  This plan performs a **one-time
client refactor** (Phases 1–3) that replaces all hardcoded screen-type routing
with a generic URI convention, then adds demo screens purely on the server
(Phase 4) with zero client changes.

### URI Convention

```
nba://{path}  →  GET /sdui/{path}
```

Special cases (backwards-compatible):
| URI                        | Endpoint                                     |
|----------------------------|----------------------------------------------|
| `nba://scoreboard`         | `GET /sdui/scoreboard?variant=…`             |
| `nba://game/{id}`          | `GET /sdui/game-detail/{id}?gameState=…&variant=…` |
| `nba://boxscore/{id}`      | `GET /sdui/boxscore/{id}`                    |
| `nba://demos`              | `GET /sdui/demos`                            |
| `nba://{anything-else}`    | `GET /sdui/{anything-else}`                  |

The server returns a `Screen` JSON; the client renders its `sections` array.
No client code is aware of _what_ a screen is — only _how_ to render sections.

---

## Phase 1 — Schema: Add `parentUri` to Screen

> **Files**: `schema/sdui-schema.json`, `codegen/output/typescript/SduiModels.ts`,
> `android/sdui-core/.../models/SduiModels.kt`

### Why

The back button should **always be visible** (mobile app convention).  The
target of the back button today is hardcoded to the scoreboard.  `parentUri`
lets the server tell the client where "back" should go, enabling proper
breadcrumb-style navigation for any screen depth.

### Steps

#### Step 1.1 — Schema

In `schema/sdui-schema.json`, add `parentUri` to the `Screen` definition
(after `traceId`):

```jsonc
// schema/sdui-schema.json  — Screen.properties
"parentUri": {
  "type": "string",
  "description": "URI the back button should navigate to.  Clients always show a back button; this field tells them the target.  Omit for root screens (e.g. scoreboard)."
}
```

> Location: `definitions.Screen.properties` (currently line ~707).
> `required` array stays as-is — `parentUri` is optional.

#### Step 1.2 — TypeScript Model (codegen output)

The generated `SduiModels` interface in
`codegen/output/typescript/SduiModels.ts` already has a `[property: string]: any`
index signature, so `screen.parentUri` is accessible today without changes.
However, for first-class IDE support, add the field explicitly:

```ts
// codegen/output/typescript/SduiModels.ts — inside SduiModels
parentUri?: string;
```

#### Step 1.3 — Kotlin Model

In `android/sdui-core/.../models/SduiModels.kt`, add to the `SduiScreen`
data class:

```kotlin
@JsonProperty("parentUri") val parentUri: String? = null,
```

> `@JsonIgnoreProperties(ignoreUnknown = true)` is already present, so older
> server payloads missing the field won't break deserialization.

#### Step 1.4 — Validate

Run `ajv` against existing schema examples to confirm no regression:
```bash
npx ajv validate -s schema/sdui-schema.json -d schema/examples/boxscore.json
```

#### Step 1.5 — Update Requirements Summary

In `docs/sdui-requirements-summary.md`:

1. **Add `parentUri` to Schema Design (§2).**  In the "Key Schema Decisions"
   bullet list, add:
   > **Server-driven back navigation** — `Screen.parentUri` (optional) tells
   > the client where the back button should navigate.  Omit for root screens.
   > Clients always show the back button on non-root screens.

2. **Update the "Server controls" table (§1, ~line 131).**  Add a row:
   > | Back navigation target (`parentUri`) | Back button rendering and gesture |

3. **Update the status matrix (§10).**  Change the "Composition API contract"
   row from **Gap** to **Partial**:
   > | Composition API contract (auth, method, cacheability) | **Partial** | URI convention defined (`nba://{path}` → `GET /sdui/{path}`); auth and cacheability still gap |

4. **Add "Prototype Concessions" subsection** (after §2 Schema Design, before
   §3 Server-Driven Actions):
   > ### Prototype Concessions
   >
   > | Concession | Rationale | Migration Path |
   > |-----------|-----------|----------------|
   > | Android `GENERIC` screen-type enum | Scoreboard and game-detail have pre-existing Ably/polling transport wiring that is coupled to screen type. Making transport fully server-driven (`refreshPolicy` + `realtimeChannel`/`realtimeTopic` fields) doesn't advance the demo goal. | Remove enum entirely; derive all transport behavior from `refreshPolicy` fields in the server response. `GENERIC` branches become the only branches. |
   > | `resolveEndpoint()` special case (`nba://game/{id}` → `game-detail/{id}?gameState=live`) | Preserves backward compatibility with the existing game-detail endpoint path and required query parameter. | Server normalizes URIs so the mapping is a straight `nba://` → `/sdui/` prefix swap with no special cases. |
   > | Variant selector is a developer tool | Variant chips are hardcoded client-side UI, not server-driven. Real A/B would use experiment assignment. | Remove variant selector; server resolves variant via experiment assignment header. |

5. **Add revision history entry:**
   > | 2026-03-04 | Added `parentUri` to Screen contract. Updated status matrix for composition API contract (Gap → Partial). Added Prototype Concessions subsection. |

#### Step 1.6 — Update Technical Proposal

In `docs/SDUI_Technical_Proposal_v2.md`:

1. **Add `parentUri` to the response hierarchy JSON snippet (~line 132).**
   Inside the `"screen"` object, after `"id"`:
   ```json
   "parentUri": "nba://scoreboard",
   ```

2. **Add a "Client URI Resolution" note** after the "Response Hierarchy"
   subsection (~line 145):
   > ### Client URI Resolution Convention
   >
   > Navigate action URIs use the `nba://` scheme.  Clients resolve these to
   > server endpoints using a simple convention:
   >
   > ```
   > nba://{path}  →  GET /sdui/{path}
   > ```
   >
   > Special case: `nba://game/{id}` → `GET /sdui/game-detail/{id}?gameState=live`
   > (preserves backward compatibility with the existing game-detail endpoint).
   >
   > This convention means new screens require **no client code changes** —
   > adding a server endpoint and including its URI in navigation items or
   > action targets is sufficient.

3. **Add revision history entry:**
   > | 2026-03-04 | Added `parentUri` to Screen response contract and example. Added client URI resolution convention. |

---

## Phase 2 — Web: Generic URI Routing

> **Files**: `web/src/hooks/useSduiScreen.ts`, `web/src/App.tsx`
>
> **No server changes.**

### Current Pain Points

| File | Problem |
|------|---------|
| `useSduiScreen.ts` | `screenType: 'scoreboard' \| 'game-detail'` literal union; URL built with `if/else` |
| `App.tsx` L28 | `route` state typed as `{ screenType: 'scoreboard' \| 'game-detail'; gameId?: string }` |
| `App.tsx` L62–79 | `handleUriNavigate` pattern-matches only two URIs; everything else → toast |
| `App.tsx` L130 | Header title falls back to hardcoded string per screen type |
| `App.tsx` L133–148 | Variants array selected by screen type — hardcoded lists |

### Steps

#### Step 2.1 — Refactor `useSduiScreen.ts`

Replace the options interface and URL builder:

```ts
// BEFORE
interface UseSduiScreenOptions {
  screenType: 'scoreboard' | 'game-detail';
  gameId?: string;
  gameState?: 'pre' | 'live' | 'final';
  variant?: string;
}

// AFTER
interface UseSduiScreenOptions {
  /** Server endpoint path, e.g. "/sdui/scoreboard" or "/sdui/game-detail/0042300102?gameState=live" */
  endpoint: string;
  variant?: string;
}
```

URL builder becomes:

```ts
const separator = endpoint.includes('?') ? '&' : '?';
const url = `/api${endpoint}${separator}variant=${variant}`;
```

The `fetchScreen` `useCallback` deps simplify to `[endpoint, variant]`.

#### Step 2.2 — Add `resolveEndpoint` helper (App.tsx)

A pure function that converts a URI to a server endpoint path:

```ts
/**
 * Convert an nba:// URI to a server endpoint path.
 *
 *   nba://scoreboard        → /sdui/scoreboard
 *   nba://game/0042300102   → /sdui/game-detail/0042300102?gameState=live
 *   nba://boxscore/00423... → /sdui/boxscore/0042300102
 *   nba://demos             → /sdui/demos
 *   nba://anything/else     → /sdui/anything/else
 */
function resolveEndpoint(uri: string): string {
  const path = uri.replace(/^nba:\/\//, '');

  // Special case: game/{id} → game-detail/{id}?gameState=live
  const gameMatch = path.match(/^game\/(.+)/);
  if (gameMatch) {
    return `/sdui/game-detail/${gameMatch[1]}?gameState=live`;
  }

  return `/sdui/${path}`;
}
```

#### Step 2.3 — Refactor route state (App.tsx)

Replace the typed route object with a single URI string:

```ts
// BEFORE
const [route, setRoute] = useState<{ screenType: 'scoreboard' | 'game-detail'; gameId?: string }>({
  screenType: 'scoreboard',
});

// AFTER
const [currentUri, setCurrentUri] = useState('nba://scoreboard');
```

Wire `useSduiScreen` to derive from the URI:

```ts
const endpoint = resolveEndpoint(currentUri);
const { screen, loading, error, refetch } = useSduiScreen({ endpoint, variant });
```

#### Step 2.4 — Simplify `handleUriNavigate`

```ts
const handleUriNavigate = useCallback((uri: string) => {
  setCurrentUri(uri);
}, []);
```

That's it.  No pattern matching, no toast fallback.  Every `nba://` URI
becomes a screen fetch.

#### Step 2.5 — Back button via `parentUri`

Add a back button to the header when `screen.parentUri` is set:

```tsx
<header style={styles.header}>
  {screen.parentUri && (
    <button
      style={styles.backButton}
      onClick={() => handleUriNavigate(screen.parentUri)}
      aria-label="Back"
    >
      ←
    </button>
  )}
  <h1 style={styles.title}>{screen.title || 'NBA'}</h1>
  <span style={styles.schemaVersion}>Schema v{screen.schemaVersion}</span>
</header>
```

> Title comes from `screen.title` (server-driven). The hardcoded
> `"Today's Games"` / `"Game Detail"` fallbacks are removed.

#### Step 2.6 — Dynamic variant selector

Instead of hardcoded variant arrays per screen type, derive variants from
the current URI's known presets, or hide the bar entirely for unknown screens:

```ts
const VARIANT_PRESETS: Record<string, Array<{ id: string; label: string; description: string }>> = {
  'nba://scoreboard': [
    { id: 'A', label: 'Default', description: 'Standard scoreboard' },
    { id: 'E', label: 'Promo', description: 'Promo banner at top' },
    { id: 'F', label: 'Promo + Rail', description: 'Promo banner + content rail' },
  ],
};

// Only show variant bar for screens that have presets
const variants = VARIANT_PRESETS[currentUri] ?? [];
```

Game-detail variants use a prefix match:

```ts
function getVariants(uri: string) {
  if (uri.startsWith('nba://game/')) {
    return GAME_DETAIL_VARIANTS;
  }
  return VARIANT_PRESETS[uri] ?? [];
}
```

#### Step 2.7 — Reset variant on navigation

```ts
useEffect(() => {
  setVariant('A');
}, [currentUri]);
```

(Already exists keyed on `route.screenType`; change dep to `currentUri`.)

#### Step 2.8 — Responsive layout (remove hardcoded mobile width)

The current `container` style forces `maxWidth: 480` with `margin: '0 auto'`,
making the web app always look like a phone simulator regardless of viewport.
This is inaccurate — the server owns layout composition, not the client's CSS.

Remove the fixed width and make the container fluid:

```ts
// BEFORE
container: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: 480,
    margin: '0 auto',
    backgroundColor: '#0f0f23',
},

// AFTER
container: {
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    maxWidth: 1200,
    margin: '0 auto',
    padding: '0 16px',
    backgroundColor: '#0f0f23',
},
```

> `maxWidth: 1200` provides a reasonable content width ceiling for desktop
> while remaining responsive.  The server's section composition determines
> the actual visual layout — not the client wrapper.

#### Step 2.9 — Add `X-Platform` header to requests

In `useSduiScreen.ts`, add a platform header so the server can compose
platform-appropriate layouts:

```ts
const response = await fetch(url, {
  headers: {
    'X-Schema-Version': '1.0',
    'X-Platform': 'web',
  },
});
```

> For the prototype the server ignores this header, but the plumbing exists
> for platform-aware composition per ADR-008.  Android already sends its
> own platform identifier.

#### Step 2.10 — Smoke test

1. Start server + web dev.
2. Verify scoreboard loads at boot.
3. **Desktop viewport**: Content fills available width up to 1200px (not locked to 480px).
4. Tap a game card → game-detail loads (via `nba://game/{id}`).
5. Navigation bar items that resolve to unknown endpoints → server 404 → error state (not a toast).
6. Back button shows on game-detail (server sets `parentUri: "nba://scoreboard"`).
7. Verify `X-Platform: web` header in DevTools Network tab.

---

## Phase 3 — Android: Generic URI Routing

> **Files**: `SduiConfig.kt`, `SduiRepository.kt`, `SduiScreenViewModel.kt`,
> `GameDetailScreen.kt`, `MainActivity.kt`
>
> **No server changes.**

### Step 3.1 — `SduiRepository.kt`: Add generic `fetchScreen`

```kotlin
/**
 * Fetch any SDUI screen by its resolved server path.
 *
 * @param path  Path relative to baseUrl, e.g. "/sdui/demos"
 */
suspend fun fetchScreen(
    path: String,
    variant: String = "A"
): SduiScreen = withContext(Dispatchers.IO) {
    val separator = if (path.contains("?")) "&" else "?"
    val url = "$baseUrl$path${separator}variant=$variant"

    Log.d(TAG, "Fetching screen: $url")

    val request = Request.Builder()
        .url(url)
        .header("X-Schema-Version", SCHEMA_VERSION)
        .build()

    val response = httpClient.newCall(request).execute()
    if (!response.isSuccessful) {
        throw SduiException("Failed to fetch screen: ${response.code}")
    }

    val body = response.body?.string()
        ?: throw SduiException("Empty response body")

    try {
        objectMapper.readValue(body, SduiScreen::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse screen response", e)
        throw SduiException("Failed to parse screen response: ${e.message}")
    }
}
```

> The existing `getGameDetail` and `getScoreboard` methods stay for backward
> compatibility — they are called by the existing `loadScreen` /
> `loadScoreboard` methods and include Ably/polling-specific config.

### Step 3.2 — `SduiConfig.kt`: Add URI-based factory

```kotlin
data class SduiConfig(
    val screenType: ScreenType,
    val gameId: String? = null,
    val variant: String = "A",
    val enableAbly: Boolean,
    val enablePolling: Boolean,
    val uri: String? = null          // ← new
) {
    enum class ScreenType { SCOREBOARD, GAME_DETAIL, GENERIC }  // ← GENERIC added

    companion object {
        // existing factories unchanged …

        /**
         * Factory for any server-driven screen.
         * No Ably/polling — the server's refreshPolicy drives data updates.
         */
        fun fromUri(uri: String) = SduiConfig(
            screenType = ScreenType.GENERIC,
            uri = uri,
            enableAbly = false,
            enablePolling = false
        )
    }
}
```

> **Prototype concession: `GENERIC` enum.**  The `GENERIC` screen type is a
> pragmatic compromise.  The pure SDUI approach would eliminate the enum
> entirely and derive all behavior (Ably, polling, static) from the server's
> `refreshPolicy` response — making every screen truly URI-driven with zero
> client classification.  That refactor requires adding `realtimeChannel` /
> `realtimeTopic` fields to the schema and reworking the ViewModel's
> transport setup, which doesn't advance the prototype's demo goal.
>
> The `GENERIC` value is additive (doesn't break existing paths), appears in
> exactly 4 `when` branches, and provides a clean migration path: when the
> enum is eventually removed, the `GENERIC` branches become the *only*
> branches.  See the requirements summary for the full trade-off analysis.

### Step 3.3 — `SduiScreenViewModel.kt`: Add `loadFromUri`

Add a `resolveEndpoint` companion function and a new load method:

```kotlin
companion object {
    // ... existing constants ...

    /**
     * Convert nba:// URI to server endpoint path.
     */
    fun resolveEndpoint(uri: String): String {
        val path = uri.removePrefix("nba://")
        val gameMatch = Regex("^game/(.+)").find(path)
        if (gameMatch != null) {
            return "/sdui/game-detail/${gameMatch.groupValues[1]}?gameState=live"
        }
        return "/sdui/$path"
    }
}

/**
 * Load a screen from a generic nba:// URI.
 */
fun loadFromUri(uri: String, sectionId: String? = null) {
    currentEndpoint = uri   // store URI for refresh
    currentScreenId = uri

    viewModelScope.launch {
        if (sectionId == null) {
            _uiState.value = SduiScreenUiState.Loading
        }
        try {
            val endpoint = resolveEndpoint(uri)
            Log.d(TAG, "Loading from URI: $uri → $endpoint")
            val screen = repository.fetchScreen(endpoint, config.variant)
            currentScreen = screen

            screen.state?.forEach { (key, value) ->
                stateManager.setState(key, value)
            }
            _uiState.value = SduiScreenUiState.Success(screen)
            if (config.enablePolling) setupPolling(screen)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from URI: $uri", e)
            _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

Update `refresh()` to handle the generic case:

```kotlin
fun refresh() {
    val id = currentScreenId ?: return
    viewModelScope.launch {
        _isRefreshing.value = true
        try {
            val screen = when (currentEndpoint) {
                ENDPOINT_SCOREBOARD -> repository.getScoreboard(config.variant)
                ENDPOINT_GAME_DETAIL -> repository.getGameDetail(id, "live", config.variant)
                else -> repository.fetchScreen(resolveEndpoint(currentEndpoint), config.variant)
            }
            currentScreen = screen
            screen.state?.forEach { (k, v) -> stateManager.setState(k, v) }
            _uiState.value = SduiScreenUiState.Success(screen)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh failed", e)
        } finally {
            _isRefreshing.value = false
        }
    }
}
```

Also update the polling fallback in `setupPolling` similarly (the `else`
branch inside the poll job that currently switches on `currentEndpoint`).

### Step 3.4 — `GameDetailScreen.kt`: Generic branch + parentUri back

**LaunchedEffect** — add `GENERIC` branch:

```kotlin
LaunchedEffect(config) {
    when (config.screenType) {
        SduiConfig.ScreenType.SCOREBOARD -> viewModel.loadScoreboard()
        SduiConfig.ScreenType.GAME_DETAIL -> viewModel.loadGameDetail(config.gameId ?: "")
        SduiConfig.ScreenType.GENERIC -> viewModel.loadFromUri(config.uri ?: "")
    }
}
```

**RefreshResult handler** — add `GENERIC` branch:

```kotlin
is ActionHandler.ActionResult.RefreshResult -> {
    when (config.screenType) {
        SduiConfig.ScreenType.SCOREBOARD -> viewModel.loadScoreboard(result.sectionId)
        SduiConfig.ScreenType.GAME_DETAIL -> viewModel.loadGameDetail(config.gameId ?: "", result.sectionId)
        SduiConfig.ScreenType.GENERIC -> viewModel.loadFromUri(config.uri ?: "", result.sectionId)
    }
}
```

**onRetry** — same pattern (`GENERIC -> viewModel.loadFromUri(config.uri ?: "")`).

**TopAppBar title** — use server-driven title:

```kotlin
TopAppBar(
    title = {
        Text(
            text = screenState.screen?.title
                ?: when (config.screenType) {
                    SduiConfig.ScreenType.SCOREBOARD -> "Today's Games"
                    SduiConfig.ScreenType.GAME_DETAIL -> "Game: ${config.gameId}"
                    SduiConfig.ScreenType.GENERIC -> "NBA"
                }
        )
    },
    ...
)
```

> Server-driven `screen.title` is preferred; hardcoded strings are last-resort
> fallbacks for SCOREBOARD/GAME_DETAIL during load.

**Back button** — always show; use `parentUri` as target:

```kotlin
navigationIcon = {
    val parentUri = (uiState as? SduiScreenUiState.Success)?.screen?.parentUri
    if (config.screenType != SduiConfig.ScreenType.SCOREBOARD) {
        IconButton(onClick = {
            if (parentUri != null) onNavigateUri(parentUri) else onBack()
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
    }
}
```

> Scoreboard is the root — no back button.  All other screens show it.
> `parentUri` from the server determines the target; fallback is `onBack()`
> which the app can default to scoreboard.

**Variant chips** — hide for GENERIC screens (server controls layout):

```kotlin
if (config.screenType != SduiConfig.ScreenType.GENERIC) {
    Row(...) { /* variant chips */ }
}
```

### Step 3.5 — `MainActivity.kt`: Generic `else` branch

```kotlin
onNavigateUri = { targetUri ->
    when {
        targetUri.startsWith("nba://game/") -> {
            val gameId = targetUri.removePrefix("nba://game/")
            currentConfig = SduiConfig.gameDetail(gameId)
        }
        targetUri == "nba://scoreboard" -> {
            currentConfig = SduiConfig.scoreboard()
        }
        else -> {
            // Generic server-driven screen — no client enum needed
            currentConfig = SduiConfig.fromUri(targetUri)
        }
    }
}
```

The `else` branch replaces the snackbar toast.  Any `nba://` URI the server
puts in a `NavigationItem.targetUri` or `Action.targetUri` now
works out of the box.

Update `onVariantChange` similarly — add a `GENERIC` branch that is a no-op
(or preserves `fromUri` with the same uri).

### Step 3.6 — Smoke test (Android)

1. Build and run.
2. Scoreboard loads at boot (unchanged).
3. Tap game card → game-detail, back → scoreboard (unchanged).
4. Nav items that point to `nba://demos` etc. will trigger the generic branch.
5. Back button on demo screens uses `parentUri → nba://scoreboard`.

---

## Phase 4 — Server: Demo Screens (Zero Client Changes)

> **Files**: `SduiCompositionService.java`, `SduiController.java`
>
> **No web or Android changes.**

After Phases 1–3, any new `nba://{path}` endpoint is a server-only addition.

### Step 4.1 — Update `buildNavigation()`

Replace the dead **Teams** and **Standings** items with functional demo
items:

```java
// Replace "Teams" with "Demos"
ObjectNode demos = objectMapper.createObjectNode();
demos.put("id", "demos");
demos.put("label", "Demos");
demos.put("icon", "widgets");
demos.put("targetUri", "nba://demos");
demos.put("selected", "demos".equals(activeScreenId));
items.add(demos);

// Replace "Standings" with "Box Score"
ObjectNode boxscore = objectMapper.createObjectNode();
boxscore.put("id", "boxscore");
boxscore.put("label", "Box Score");
boxscore.put("icon", "table_chart");
boxscore.put("targetUri", "nba://boxscore/0042300102");
boxscore.put("selected", activeScreenId != null && activeScreenId.startsWith("boxscore"));
items.add(boxscore);
```

> The `selected` logic for `gamesSelected` should also include `boxscore`
> check to keep Games highlighted for boxscore sub-screens if desired, or
> leave it as-is to highlight Box Score tab independently.

### Step 4.2 — Add `parentUri` to all screen responses

In each `compose*` method, add `parentUri` where appropriate:

| Method | `parentUri` value |
|--------|-------------------|
| `composeScoreboard()` | *(omit — root screen)* |
| `composeGameDetail()` | `"nba://scoreboard"` |
| `composeBoxscore()` | `"nba://scoreboard"` |
| `composeDemos()` (new) | `"nba://scoreboard"` |

Example for `composeGameDetail`:
```java
response.put("parentUri", "nba://scoreboard");
```

### Step 4.3 — `composeDemos()` — Kitchen Sink Screen

Create a new composition method that showcases **all 10 semantic section
types** with static mock data.  This is the primary demo screen.

```java
public JsonNode composeDemos(String traceId) {
    ObjectNode screen = objectMapper.createObjectNode();
    screen.put("id", "demos");
    screen.put("schemaVersion", "1.0");
    screen.put("title", "SDUI Section Types");
    screen.put("analyticsId", "demos-kitchen-sink");
    screen.put("traceId", traceId);
    screen.put("parentUri", "nba://scoreboard");

    // Static refresh — no polling, no Ably
    ObjectNode refreshPolicy = objectMapper.createObjectNode();
    refreshPolicy.put("type", "static");
    screen.set("defaultRefreshPolicy", refreshPolicy);

    screen.set("navigation", buildNavigation("demos"));

    ArrayNode sections = objectMapper.createArrayNode();

    // 1. ScoreboardHeader — mock matchup
    sections.add(buildDemoScoreboardHeader());

    // 2. StatLine — mock player leaders
    sections.add(buildDemoStatLine());

    // 3. PromoBanner — mock promotional card
    sections.add(buildDemoPromoBanner());

    // 4. ContentCard — standalone card
    sections.add(buildDemoContentCard());

    // 5. ContentRail — horizontal scroll of cards
    sections.add(buildDemoContentRail());

    // 6. GameCard — mock game tile
    sections.add(buildDemoGameCard());

    // 7. Row — key-value row
    sections.add(buildDemoRow());

    // 8. TabGroup — tabs wrapping sub-sections
    sections.add(buildDemoTabGroup());

    // 9. BoxscoreTable — stats table
    sections.add(buildDemoBoxscoreTable());

    // 10. Form — interactive form with dropdowns
    sections.add(buildDemoForm());

    screen.set("sections", sections);
    return screen;
}
```

Each `buildDemo*` helper creates a self-contained section with inline mock
data.  No external API calls — everything is static.

#### Mock Data Guidelines

- **ScoreboardHeader**: Lakers vs Celtics, period 3, 89-94
- **StatLine**: 3 mock players with PTS/REB/AST
- **PromoBanner**: "Welcome to SDUI" with gradient background
- **ContentCard**: Single highlight card with thumbnail
- **ContentRail**: 4 cards (Top Plays, Player Spotlight, etc.)
- **GameCard**: Single mock game with team logos
- **Row**: League leader row (label/value pair)
- **TabGroup**: Two tabs ("Overview", "Stats") each with a ContentCard child
- **BoxscoreTable**: 3 players, 5 stat columns
- **Form**: Two dropdowns (Season, Season Type) with a submit action

### Step 4.4 — Controller endpoint

```java
@GetMapping(value = "/sdui/demos", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<JsonNode> getDemos(
        @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
        HttpServletResponse response) {

    String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
    MDC.put("traceId", traceId);
    log.info("SDUI demos request: schemaVersion={}", schemaVersion);

    try {
        JsonNode screenResponse = compositionService.composeDemos(traceId);
        response.setHeader("X-Trace-Id", traceId);
        response.setHeader("X-Schema-Version", "1.0");
        return ResponseEntity.ok(screenResponse);
    } catch (Exception e) {
        log.error("Error composing demos screen", e);
        return ResponseEntity.internalServerError().build();
    } finally {
        MDC.clear();
    }
}
```

### Step 4.5 — Smoke test

1. Restart server.
2. **Web**: Click "Demos" nav tab → demos screen renders all 10 section types.
3. **Android**: Tap "Demos" nav tab → demos screen renders (generic branch).
4. **Box Score tab**: Already works via existing `/sdui/boxscore/{gameId}`.
5. Back button → `parentUri` navigates to scoreboard.
6. Verify no client code was changed in this phase.

---

## Phase 5 — Schema Example & Validation

> **Files**: `schema/examples/demos.json`

### Step 5.1 — Export demo response

```bash
curl -s http://localhost:8080/sdui/demos | python3 -m json.tool > schema/examples/demos.json
```

### Step 5.2 — Validate

```bash
npx ajv validate -s schema/sdui-schema.json -d schema/examples/demos.json
```

Fix any validation errors (typically `required` field mismatches or `oneOf`
discrimination issues).

### Step 5.3 — Commit

```bash
git add schema/examples/demos.json
git commit -m "feat(schema): add demos kitchen-sink example"
```

---

## Commit Strategy

| Phase | Commit message | Scope |
|-------|---------------|-------|
| 1 | `feat(schema): add parentUri to Screen` | schema, codegen, Kotlin model, docs |
| 2 | `refactor(web): generic URI routing` | web only |
| 3 | `refactor(android): generic URI routing` | android only |
| 4 | `feat(server): demos kitchen-sink screen + nav` | server only |
| 5 | `feat(schema): add demos example` | schema only |

---

## Summary of Files Changed

### Phase 1 (Schema + Docs)
| File | Change |
|------|--------|
| `schema/sdui-schema.json` | Add `parentUri` string property to `Screen` |
| `codegen/output/typescript/SduiModels.ts` | Add `parentUri?: string` |
| `android/sdui-core/.../models/SduiModels.kt` | Add `parentUri: String? = null` |
| `docs/sdui-requirements-summary.md` | Add `parentUri` to schema decisions, server-controls table, status matrix |
| `docs/SDUI_Technical_Proposal_v2.md` | Add `parentUri` to response example, add URI resolution convention |

### Phase 2 (Web)
| File | Change |
|------|--------|
| `web/src/hooks/useSduiScreen.ts` | Replace `screenType` with `endpoint` string; add `X-Platform: web` header |
| `web/src/App.tsx` | URI-based route state, `resolveEndpoint()`, back button, dynamic variants, responsive layout (`maxWidth: 1200`) |

### Phase 3 (Android)
| File | Change |
|------|--------|
| `android/sdui-core/.../data/SduiRepository.kt` | Add `fetchScreen(path)` method |
| `android/sdui-core/.../screen/SduiScreenViewModel.kt` | Add `loadFromUri()`, `resolveEndpoint()`, update `refresh()` + polling |
| `android/app/.../SduiConfig.kt` | Add `GENERIC` enum value, `uri` field, `fromUri()` factory |
| `android/app/.../ui/GameDetailScreen.kt` | `GENERIC` branches, parentUri back button, hide variants |
| `android/app/.../MainActivity.kt` | Generic `else` branch in `onNavigateUri` |

### Phase 4 (Server — zero client changes)
| File | Change |
|------|--------|
| `server/.../service/SduiCompositionService.java` | `composeDemos()`, `buildDemo*()` helpers, update `buildNavigation()`, add `parentUri` to compose methods |
| `server/.../controller/SduiController.java` | Add `/sdui/demos` endpoint |

### Phase 5 (Schema example)
| File | Change |
|------|--------|
| `schema/examples/demos.json` | New — exported demo response |

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Existing scoreboard/game-detail behavior regresses | Dedicated `loadScreen`/`loadScoreboard` paths remain; generic path is additive |
| Ably/polling breaks for generic screens | Generic screens use static refresh by default; Ably/polling only for SCOREBOARD/GAME_DETAIL configs |
| Android ViewModel key collision | `key` parameter includes full config (screen type + uri + variant) |
| Web proxy doesn't forward new paths | `/api/*` proxy already strips prefix and forwards to server — any path works |
| `parentUri` absent from old responses | Field is optional; back button falls back to `onBack()` callback |

---

## Success Criteria

After all 5 phases:

- [ ] Web and Android render scoreboard + game-detail identically to today
- [ ] "Demos" nav tab loads a kitchen-sink screen with all 10 section types
- [ ] "Box Score" nav tab loads the boxscore screen
- [ ] Back button works on all non-root screens
- [ ] **Phase 4 required zero web/Android code changes**
- [ ] Schema validates for all example files
- [ ] Each phase is independently committable and testable
