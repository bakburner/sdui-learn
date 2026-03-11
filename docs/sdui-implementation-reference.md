# SDUI Platform — Implementation Reference

> Code samples and detailed implementation guidance for the SDUI platform. Referenced from the main [Requirements Summary](sdui-requirements-summary.md).

---

## Impression Tracking — Client-Side Implementation

The dedup tracking lives in a dedicated `SDUIImpressionTracker`, used by the `SDUIActionExecutor`. The renderer doesn't know about dedup — it reports visibility events as usual.

### Swift (iOS / tvOS)

```swift
@MainActor
final class SDUIImpressionTracker {
    
    private var firedImpressions: [String: Set<String>] = [:]  // scope → set of event keys
    private var dwellTimers: [String: Task<Void, Never>] = []
    
    private let screenId: String
    private let sessionId: String
    
    init(screenId: String, sessionId: String) {
        self.screenId = screenId
        self.sessionId = sessionId
    }
    
    /// Called by the action executor before dispatching an analytics action
    /// with an impression policy. Returns true if the beacon should fire.
    func shouldFire(action: AnalyticsAction, sectionId: String) -> Bool {
        guard let impression = action.impression else {
            return true  // No impression policy → always fire
        }
        
        let eventKey = "\(sectionId):\(action.event)"
        
        switch impression.dedup {
        case .none:
            return true
            
        case .oncePerScreen:
            let scope = "screen:\(screenId)"
            return insertIfAbsent(scope: scope, key: eventKey)
            
        case .oncePerSession:
            let scope = "session:\(sessionId)"
            return insertIfAbsent(scope: scope, key: eventKey)
            
        case .oncePerInterval:
            let scope = "interval:\(screenId)"
            if firedImpressions[scope]?.contains(eventKey) == true {
                return false
            }
            insertIfAbsent(scope: scope, key: eventKey)
            let interval = impression.intervalMs ?? 30_000
            Task {
                try? await Task.sleep(nanoseconds: UInt64(interval) * 1_000_000)
                firedImpressions[scope]?.remove(eventKey)
            }
            return true
        }
    }
    
    /// Starts a dwell timer for a section. Calls completion when threshold met.
    func startDwell(
        sectionId: String,
        dwellMs: Int,
        onThresholdMet: @escaping () -> Void
    ) {
        let key = "dwell:\(sectionId)"
        dwellTimers[key]?.cancel()
        
        dwellTimers[key] = Task {
            try? await Task.sleep(nanoseconds: UInt64(dwellMs) * 1_000_000)
            guard !Task.isCancelled else { return }
            onThresholdMet()
        }
    }
    
    /// Cancels dwell timer when section drops below visibility threshold.
    func cancelDwell(sectionId: String) {
        let key = "dwell:\(sectionId)"
        dwellTimers[key]?.cancel()
        dwellTimers.removeValue(forKey: key)
    }
    
    /// Resets screen-scoped impressions (call on screen re-entry).
    func resetScreenScope() {
        firedImpressions.removeValue(forKey: "screen:\(screenId)")
    }
    
    @discardableResult
    private func insertIfAbsent(scope: String, key: String) -> Bool {
        if firedImpressions[scope]?.contains(key) == true {
            return false
        }
        firedImpressions[scope, default: []].insert(key)
        return true
    }
}
```

### Kotlin (Android / Fire TV)

```kotlin
class SDUIImpressionTracker(
    private val screenId: String,
    private val sessionId: String,
    private val scope: CoroutineScope
) {
    private val firedImpressions = mutableMapOf<String, MutableSet<String>>()
    private val dwellJobs = mutableMapOf<String, Job>()

    fun shouldFire(action: AnalyticsAction, sectionId: String): Boolean {
        val impression = action.impression ?: return true
        val eventKey = "$sectionId:${action.event}"

        return when (impression.dedup) {
            Dedup.NONE -> true

            Dedup.ONCE_PER_SCREEN -> {
                val scope = "screen:$screenId"
                insertIfAbsent(scope, eventKey)
            }

            Dedup.ONCE_PER_SESSION -> {
                val scope = "session:$sessionId"
                insertIfAbsent(scope, eventKey)
            }

            Dedup.ONCE_PER_INTERVAL -> {
                val intervalScope = "interval:$screenId"
                if (firedImpressions[intervalScope]?.contains(eventKey) == true) {
                    return false
                }
                insertIfAbsent(intervalScope, eventKey)
                val interval = impression.intervalMs ?: 30_000L
                scope.launch {
                    delay(interval)
                    firedImpressions[intervalScope]?.remove(eventKey)
                }
                true
            }
        }
    }

    fun startDwell(
        sectionId: String,
        dwellMs: Long,
        onThresholdMet: () -> Unit
    ) {
        val key = "dwell:$sectionId"
        dwellJobs[key]?.cancel()
        dwellJobs[key] = scope.launch {
            delay(dwellMs)
            onThresholdMet()
        }
    }

    fun cancelDwell(sectionId: String) {
        val key = "dwell:$sectionId"
        dwellJobs[key]?.cancel()
        dwellJobs.remove(key)
    }

    fun resetScreenScope() {
        firedImpressions.remove("screen:$screenId")
    }

    private fun insertIfAbsent(scope: String, key: String): Boolean {
        if (firedImpressions[scope]?.contains(key) == true) return false
        firedImpressions.getOrPut(scope) { mutableSetOf() }.add(key)
        return true
    }
}
```

---

## Visibility Detection Wiring

The renderer needs to report **how much** of a section is visible, not just that it appeared. This requires intersection observation.

### Swift — GeometryReader + visibility calculation

```swift
.onGeometryChange(for: CGFloat.self) { proxy in
    let sectionFrame = proxy.frame(in: .global)
    let screenBounds = UIScreen.main.bounds
    let intersection = sectionFrame.intersection(screenBounds)
    return intersection.isNull ? 0 : (intersection.height / sectionFrame.height)
} action: { visibleFraction in
    handleVisibility(sectionId: section.id, fraction: visibleFraction)
}
```

### Compose — LazyListState + visible item tracking

```kotlin
val listState = rememberLazyListState()

LaunchedEffect(listState) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo }
        .collect { visibleItems ->
            visibleItems.forEach { item ->
                val totalSize = item.size.toFloat()
                val viewportStart = listState.layoutInfo.viewportStartOffset
                val viewportEnd = listState.layoutInfo.viewportEndOffset
                val visibleStart = maxOf(item.offset, viewportStart)
                val visibleEnd = minOf(item.offset + item.size, viewportEnd)
                val visibleFraction = (visibleEnd - visibleStart) / totalSize

                handleVisibility(sectionId = item.key, fraction = visibleFraction)
            }
        }
}
```

---

## Schema Definition — Analytics Impression Field

Add the `impression` field to the `AnalyticsAction` definition in `sdui-schema.json`:

```json
{
  "AnalyticsAction": {
    "type": "object",
    "required": ["type", "event"],
    "properties": {
      "type":         { "const": "analytics" },
      "event":        { "type": "string" },
      "params":       { "type": "object", "additionalProperties": true },
      "destinations": {
        "type": "array",
        "items": { "type": "string", "enum": ["adobe", "firebase", "internal", "all"] },
        "default": ["all"]
      },
      "impression": {
        "type": "object",
        "description": "Impression tracking policy. Only relevant for onVisible triggers. Omit for non-impression analytics.",
        "properties": {
          "dedup": {
            "type": "string",
            "enum": ["none", "once-per-screen", "once-per-session", "once-per-interval"],
            "default": "once-per-screen"
          },
          "threshold": {
            "type": "object",
            "properties": {
              "visibility": {
                "type": "number",
                "minimum": 0,
                "maximum": 1,
                "default": 0.5,
                "description": "Fraction of section area that must be visible (0.5 = 50%)"
              },
              "dwellMs": {
                "type": "integer",
                "minimum": 0,
                "default": 1000,
                "description": "Milliseconds section must remain visible before impression fires"
              }
            }
          },
          "intervalMs": {
            "type": "integer",
            "description": "Reset interval for once-per-interval strategy (milliseconds)"
          }
        }
      }
    }
  }
}
```

---

## BoxscoreTable — Client Rendering Patterns

> **Reference guidance** — non-normative. Each platform team owns their rendering implementation. These patterns are recommendations based on prototype experience and platform best practices.

The `BoxscoreTable` section type contains domain-typed player statistics for one team. The client renders a table with a frozen player column (name, headshot, jersey number) on the left, horizontally scrollable stat columns, sortable column headers, and a frozen totals row at the bottom.

### Web (React / CSS)

**Frozen player column** — CSS `position: sticky` on the first column:

```css
.boxscore-table-container {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.boxscore-table th:first-child,
.boxscore-table td:first-child {
  position: sticky;
  left: 0;
  z-index: 1;
  background: var(--surface-primary);
}

.boxscore-totals-row {
  position: sticky;
  bottom: 0;
  background: var(--surface-secondary);
  font-weight: 600;
}
```

**Sort state** — reads from screen state via `mutate` actions:

```tsx
const sortCol = screenState[data.sortStateKey] as string;
const sortDir = screenState[data.sortDirectionStateKey] as string;

const sortedPlayers = useMemo(() => {
  const played = data.players.filter(p => p.played);
  const dnp = data.players.filter(p => !p.played);
  const sorted = [...played].sort((a, b) => {
    const aVal = a.statistics[sortCol] ?? 0;
    const bVal = b.statistics[sortCol] ?? 0;
    return sortDir === 'asc' ? aVal - bVal : bVal - aVal;
  });
  return [...sorted, ...dnp]; // DNP players always at bottom
}, [data.players, sortCol, sortDir]);
```

**Column header tap** fires a `mutate` action to update the sort state key. The sort column and direction live in `Screen.state`, so they survive poll refreshes that replace the section data.

### Android (Jetpack Compose)

**Frozen player column** — fixed-width composable alongside horizontally scrollable stat columns:

```kotlin
@Composable
fun BoxscoreTableRenderer(
    data: BoxscoreTableData,
    screenState: StateFlow<Map<String, Any>>,
    onAction: (Action) -> Unit
) {
    val state by screenState.collectAsState()
    val sortCol = state[data.sortStateKey] as? String ?: "points"
    val sortDir = state[data.sortDirectionStateKey] as? String ?: "desc"

    val sortedPlayers = remember(data.players, sortCol, sortDir) {
        val (played, dnp) = data.players.partition { it.played }
        val sorted = played.sortedWith(
            if (sortDir == "asc") compareBy { it.statistics.getValue(sortCol) }
            else compareByDescending { it.statistics.getValue(sortCol) }
        )
        sorted + dnp
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Frozen player column
        Column(modifier = Modifier.width(160.dp)) {
            HeaderCell("PLAYER")
            sortedPlayers.forEach { player ->
                PlayerCell(
                    name = player.nameAbbreviated,
                    headshotUrl = player.headshotUrl,
                    jerseyNum = player.jerseyNum,
                    position = player.position
                )
            }
            TotalsLabelCell()
        }

        // Scrollable stat columns
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            StatHeaderRow(sortCol, sortDir, onAction)
            sortedPlayers.forEach { player ->
                StatValueRow(player.statistics)
            }
            TotalsValueRow(data.teamTotals)
        }
    }
}
```

**Row virtualization** — for larger datasets (standings, league leaders), wrap the player rows in a `LazyColumn`. For boxscore (~15 rows), eager rendering is acceptable.

### iOS (SwiftUI)

**Frozen player column** with horizontal scroll for stats:

```swift
struct BoxscoreTableView: View {
    let data: BoxscoreTableData
    @EnvironmentObject var screenState: ScreenStateManager

    private var sortCol: String {
        screenState.value(for: data.sortStateKey) as? String ?? "points"
    }
    private var sortDir: String {
        screenState.value(for: data.sortDirectionStateKey) as? String ?? "desc"
    }

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            // Frozen player column
            VStack(spacing: 0) {
                Text("PLAYER").font(.caption).bold()
                ForEach(sortedPlayers) { player in
                    PlayerCell(player: player)
                }
                TotalsLabelCell()
            }
            .frame(width: 160)

            // Scrollable stat columns
            ScrollView(.horizontal, showsIndicators: false) {
                VStack(spacing: 0) {
                    StatHeaderRow(sortCol: sortCol, sortDir: sortDir)
                    ForEach(sortedPlayers) { player in
                        StatValueRow(statistics: player.statistics)
                    }
                    TotalsValueRow(totals: data.teamTotals)
                }
            }
        }
    }
}
```

**Gesture disambiguation** — on mobile, horizontal scroll of the stat columns must not conflict with screen-level vertical scroll or tab swipe gestures. This is a platform-owned UX concern handled via gesture recognizer priority.

### Cross-Platform Notes

- **DNP players**: rendered at the bottom of the sorted list with `notPlayingReason` text and no stat values
- **Empty state**: when `emptyMessage` is non-null, render it in place of the table (pre-game, no data)
- **Team totals row**: always frozen at the bottom, visually distinct (bold/background), excluded from sort
- **Combined value formatting**: clients decide display format (e.g., "10-23" for FGM-A, "43.5%" for FG%) — this is a rendering decision, not a server concern
- **Sort state survival**: sort column and direction live in `Screen.state`; when a live poll replaces section data, the client re-sorts the new data using the existing state values

---

## Image Fallback — Client-Side Implementation

When external image URLs return 404s or fail to load, the server-provided `fallbackThumbnailUrl` field is used as a recovery image. This pattern is used across all image-bearing section types.

### Web (React)

```tsx
const fallbackUrl = data.fallbackThumbnailUrl;

<img
  src={item.imageUrl}
  onError={(e) => {
    const img = e.currentTarget;
    if (fallbackUrl && img.src !== fallbackUrl) img.src = fallbackUrl;
  }}
/>
```

The guard `img.src !== fallbackUrl` prevents an infinite error loop if the fallback URL also fails.

### Android (Jetpack Compose / Coil 2.7.0)

```kotlin
val fallbackUrl = (section.data as? Map<*, *>)
    ?.get("fallbackThumbnailUrl")?.toString()

SubcomposeAsyncImage(
    model = imageUrl,
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = Modifier.fillMaxWidth().height(200.dp),
    error = {
        if (fallbackUrl != null) {
            AsyncImage(
                model = fallbackUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
)
```

`SubcomposeAsyncImage` (from `coil-compose`) provides an `error` composable slot that renders only when the primary image fails. The error slot renders a standard `AsyncImage` with the fallback URL.

### Sections Using This Pattern

VideoCarousel, HeroPanel, ContentRail, NbaTvSchedule, PromoBanner, FollowingRail, SubscribeBanner, SubscribeHero.

---

## Section Merge (Surgical Refresh) — Form Submission Pattern

When a `Form` section submits a parameterized refresh, the server returns a response containing updated sections. Rather than replacing the entire screen (which would reset form state and scroll position), the client performs a **surgical section merge**:

### Android

```kotlin
fun refreshSections(newSections: List<SduiSection>) {
    val current = _sections.value.toMutableList()
    for (incoming in newSections) {
        val idx = current.indexOfFirst { it.id == incoming.id }
        if (idx >= 0) current[idx] = incoming else current.add(incoming)
    }
    _sections.value = current
}
```

Matching sections are replaced by ID; new sections are appended. Non-matching existing sections (including the Form itself) are preserved, maintaining field selections and scroll state.

### Web

```typescript
const isSectionUpdateRef = useRef(false);

// On form submit response:
isSectionUpdateRef.current = true;
setSections(prev => {
  const merged = [...prev];
  for (const incoming of newSections) {
    const idx = merged.findIndex(s => s.id === incoming.id);
    if (idx >= 0) merged[idx] = incoming;
    else merged.push(incoming);
  }
  return merged;
});
```

The `isSectionUpdateRef` flag prevents the merge from being treated as a full screen load (which would re-trigger initial animations or scroll resets).

---

## ErrorState — First-Class Error Rendering

The `ErrorState` section type allows the server to inject a structured error directly into the section stream. This avoids opaque client-side error screens and keeps error presentation server-driven.

### Schema Shape

```json
{
  "id": "error-fetch-failed",
  "sectionType": "ErrorState",
  "data": {
    "title": "Something went wrong",
    "message": "We couldn't load this content. Please try again.",
    "icon": "error",
    "retryAction": {
      "actionType": "refresh",
      "targetSectionIds": ["error-fetch-failed"]
    }
  }
}
```

The `icon` and `retryAction` fields are optional. When `retryAction` is present, the renderer shows a retry button that dispatches the action through the standard action handler.

### Server Helper

```java
// SduiUtils.buildErrorSection(id, title, message)
SduiSection error = SduiUtils.buildErrorSection(
    "error-standings",
    "Standings unavailable",
    "The standings service is not responding."
);
```

### Android (Compose)

`ErrorStateRenderer.kt` renders the section as a centered column with icon, title, message, and optional retry button:

```kotlin
@Composable
fun ErrorStateRenderer(section: SduiSection, onAction: (SduiAction) -> Unit) {
    val data = section.data
    Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        data["icon"]?.let { Icon(painterResource(resolveIcon(it)), contentDescription = null) }
        Text(data["title"], style = MaterialTheme.typography.titleMedium)
        Text(data["message"], style = MaterialTheme.typography.bodyMedium)
        data["retryAction"]?.let { action ->
            Button(onClick = { onAction(action.toSduiAction()) }) { Text("Retry") }
        }
    }
}
```

### Web (React)

`ErrorState.tsx` mirrors the same structure:

```tsx
export function ErrorState({ section, onAction }: SectionProps) {
  const { title, message, icon, retryAction } = section.data;
  return (
    <div className="error-state">
      {icon && <span className={`icon-${icon}`} />}
      <h3>{title}</h3>
      <p>{message}</p>
      {retryAction && (
        <button onClick={() => onAction(retryAction)}>Retry</button>
      )}
    </div>
  );
}
```

---

*Implementation reference for SDUI platform — February 2025*
