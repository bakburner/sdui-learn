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

*Implementation reference for SDUI platform — February 2025*
