# ADR-012: Client Data Architecture

- Status: Proposed
- Date: 2026-04-15
- Decision owners: Adrian Robinson (interim), platform leads, backend leads
- Related requirements: `docs/sdui-requirements-summary.md` §9d, §9e
- Related ADRs: ADR-010 (Offline UX), ADR-011 (Data Classification & Freshness)

## Decision

Adopt a **persistent section-level blob store** (Room on Android, SwiftData on iOS, IndexedDB on Web) as the single source of truth for SDUI section data, replacing the current in-memory ViewModel-held model. Data bindings, poll responses, and mutate actions write to the store; the UI observes the store.

## Context

Today, SDUI section data lives entirely in memory:

- **Android:** `SduiScreenViewModel` holds a `StateFlow<SduiScreenUiState>` containing the full screen. SSE messages and poll responses are applied via `DataBindingResolver` directly to in-memory data, then emitted as a new StateFlow value.
- **Web:** `App.tsx` holds `screen` in React state. `useRefreshPolicy` applies updates via state setters.
- **iOS:** Not yet built.

This creates several problems:

| Problem | Impact |
|---------|--------|
| **Process death (Android)** | ViewModel destroyed. All section data lost. Full re-fetch required. |
| **App background** | System reclaims process → all data lost. |
| **No cache-then-network** | Cold start must wait for network before showing anything. |
| **ViewModel conflates concerns** | Coordinates subscriptions, timers, action routing AND owns the data. |
| **Visibility gating requires manual buffering** | Pausing SSE for off-screen sections requires explicitly buffering messages in memory — there's nowhere else for them to go. |
| **Two disconnected state systems** | `screenState` (flat map for UI interaction via `mutate`) and `section.data` (JSON blobs updated by data bindings) are separate stores with different update mechanisms. |

### The Write/Read Separation Insight

The data binding pipeline is fundamentally a **write pipeline** — SSE messages, poll responses, and mutate actions produce updated data. The UI is a **read pipeline** — it observes data and renders. Today the ViewModel sits in the middle of both. A persistent store separates these cleanly:

```
Write Side (network/actions):              Read Side (UI):
SSE message → applyBindings() ──┐
                                ├──→ store ──→ Compose collectAsState()
Poll response → extract(dataPath) ─┤          ──→ SwiftUI @Query
                                ├──→        ──→ React useSyncExternalStore()
Mutate action ──────────────────┘
```

## Decision Drivers

- Section data should survive process death and app backgrounding without re-fetching.
- The data write pipeline (bindings, polls, mutations) should be decoupled from the UI observation pipeline.
- Visibility-gated refresh should be trivial: pause the writer, the reader sees last-written data.
- Offline/cache should not require a separate storage layer — the runtime store IS the cache.
- The solution must not create a structured local schema that mirrors SDUI types (per ADR-010's rejection of Option A). Data is stored as opaque JSON blobs.

## Options Considered

### Option A: Keep In-Memory Model (Status Quo)

ViewModel holds all section data in StateFlow/useState. SSE and polls write directly to the ViewModel's in-memory state.

Pros:
- Zero additional infrastructure. Already built and working.
- No persistence layer to maintain or migrate.
- Simple mental model: data flows network → ViewModel → UI.

Cons:
- Process death loses everything. Cold start requires full re-fetch.
- Visibility gating requires manual SSE message buffering.
- Caching requires a separate layer (HTTP cache or a dedicated snapshot store).
- ViewModel conflates coordination and data ownership.
- `screenState` and `section.data` remain disconnected.

### Option B: Persistent Section-Level Blob Store (Recommended)

A platform-native store (Room / SwiftData / IndexedDB) holds section data as opaque JSON blobs keyed by `endpoint:sectionId`. Data bindings and polls write to the store. UI observes store rows via reactive queries.

Pros:
- **Process death resilient.** Store survives. UI renders immediately on recreation.
- **Offline is free.** The store IS the cache. No separate cache layer needed. Subsumes ADR-010's stale-while-offline approach.
- **Visibility gating is trivial.** Pause the writer; the reader always has last-written data. No manual message buffering.
- **No schema coupling.** Data is stored as opaque JSON blobs — no Room entity per section type, no migrations per schema change.
- **Clean separation.** ViewModel coordinates (subscriptions, timers, action routing). Store owns data. UI observes store.
- **`screenState` and `section.data` unify.** Both become rows in the same store with different key prefixes.

Cons:
- **Disk I/O on write path.** Every SSE message and poll response writes to disk. Mitigated by: Room/SwiftData WAL mode, batch writes, and small payload sizes (1–5 KB per section).
- **Reactive query overhead.** Adds overhead vs direct StateFlow. Mitigated by: single-row lookups by primary key are O(1) in SQLite.
- **New dependency.** Room (Android) and SwiftData (iOS) are standard platform libraries but add build complexity. IndexedDB is browser-native.
- **Store invalidation.** Need strategy for evicting stale entries. Mitigated by: overwrite on successful fetch, cacheability-based eviction (ADR-011).

### Option C: Persistent Screen-Level Blob Store

Store whole screen responses as single blobs keyed by endpoint. Coarser granularity than Option B.

Pros:
- Simpler schema — one row per screen, not per section.
- Matches the HTTP cache model (one cached response per URL).

Cons:
- Cannot update individual sections without rewriting the entire screen blob.
- SSE updates require read-modify-write of the full screen — inefficient for large screens.
- Visibility gating can't target individual sections — it's all-or-nothing per screen.
- Cross-section data sharing not possible.

## Evidence

- **SDUI payload sizes:** 10–50 KB per screen, 1–5 KB per section. Section-level writes are trivially small for SQLite.
- **Room write performance:** Single-row upsert with WAL mode: <1ms on modern devices. Even at 10 SSE messages/second, disk I/O is negligible.
- **Industry pattern:** Airbnb's Ghost Platform uses a section-level cache store. DoorDash's SDUI caches section data independently for partial-screen updates.
- **Existing code impact:** The primary change is in 2–3 call sites per platform where `applyBindings()` result is written. ViewModel coordination logic (timers, subscriptions, action routing) is unchanged.
- **SwiftData / Room maturity:** Both are first-party, well-documented, and widely adopted. IndexedDB is browser-native with broad support.

## Decision Outcome

**Option B** — persistent section-level blob store.

The store holds opaque JSON blobs, not typed entities. This preserves ADR-010's principle of no schema coupling while gaining persistence, clean separation, and trivial visibility gating.

### Store Schema

```sql
Table: sdui_sections
  key: TEXT PRIMARY KEY       -- "{endpoint}:{sectionId}" or "state:{endpoint}:{stateKey}"
  payload: TEXT               -- JSON blob (section data or state value)
  updatedAt: INTEGER          -- epoch millis
  cacheability: TEXT          -- from ADR-011: public|contextual|personalized|live
  screenEndpoint: TEXT        -- parent screen endpoint (for bulk eviction)
```

Platform mapping:

| Platform | Technology | Reactive observation |
|----------|-----------|---------------------|
| Android | Room `@Entity` + `@Dao` returning `Flow<CachedSection>` | `collectAsState()` in Compose |
| iOS | SwiftData `@Model` | `@Query` in SwiftUI |
| Web | IndexedDB with wrapper | `useSyncExternalStore()` or custom hook |

### ViewModel Role After This Change

The ViewModel becomes a **coordinator**, not a data holder:

| Responsibility | Before (in-memory) | After (store-backed) |
|---------------|-------|------|
| Hold section data | `_sections: StateFlow<List<Section>>` | Store owns data |
| SSE → update | `applyBindings()` → `_sections.update {}` | `applyBindings()` → `store.upsert()` |
| Poll → update | Update section in StateFlow | `store.upsert()` |
| Mutate → update | `stateManager.setState()` | `store.upsert("state:...")` |
| UI observation | UI collects ViewModel StateFlow | UI observes store via reactive query |

ViewModel still owns: subscription lifecycle, poll timer management, action routing, visibility coordination.

### Eviction Strategy

| Trigger | Behavior |
|---------|----------|
| Successful screen fetch | Overwrite all sections for that endpoint |
| Screen navigation (away) | Keep entries — they serve as cache for back navigation |
| Cacheability-based TTL | `live`: evict after 60s of no updates. `public`: keep for 24h. `contextual`/`personalized`: keep for 4h. |
| Manual clear | User clears app data → wipe store |
| Store size cap | If store exceeds 10 MB, evict oldest entries by `updatedAt` |

### Migration Path

This change is backward-compatible and can be introduced incrementally:

1. Add the store alongside the existing in-memory model.
2. Dual-write: data bindings write to both ViewModel state and store.
3. Switch UI observation from ViewModel StateFlow to store queries.
4. Remove in-memory data holding from ViewModel.

Existing visibility-gated refresh work proceeds against the in-memory model (Phase 1). When ADR-012 is accepted and implemented, the rewire is a 2–3 call site change per platform: swap the write target from `_sections.update {}` to `store.upsert()`.

## Consequences

- **Visibility-gated refresh plan** simplifies: pause the writer, store retains last data. No manual SSE message buffering needed.
- **Caching plan** (plan-caching-offline.md) simplifies: the store IS the cache. The separate `ScreenCacheDao` concept merges into the section store.
- **ADR-010 offline strategy** is enhanced: stale-while-offline reads directly from the section store instead of a separate HTTP cache snapshot.
- **iOS client** (Phase 2 of implementation plan) builds on SwiftData from day one instead of replicating the in-memory model and later migrating.
- **Cross-section data sharing** becomes possible in the future: two sections observing the same store row get consistent data.
- **Analytics queue** (ADR-010 medium-term consequence): `fireAndForget` events can be queued in a sibling table in the same store.
