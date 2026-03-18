# SDUI Prototype — Development Rules

These rules govern all SDUI work in this repository. They exist to ensure the
server retains full control of the UI and that client releases are never
required for layout, content, or data-flow changes.

---

## 1. Schema Is the Single Source of Truth

- **All section types** must be defined in `schema/sdui-schema.json`.
- After modifying the schema, **always regenerate** models by running
  `cd codegen && ./generate.sh`.
- Generated code lives in `codegen/output/{kotlin,swift,typescript}` — never
  edit those files by hand.

## 2. No Hardcoded URLs or Paths on Clients

- Clients (Android, iOS, web) must **never** contain hardcoded server paths
  such as `/sdui/game-detail/`, `/sdui/scoreboard`, or `/stats/`.
- All endpoint resolution flows through **one generic method**
  (`fetchScreen()` in the repository / a URI resolver on the server).
- Navigation URIs (e.g. `nba://for-you`) come from the server's `navigation`
  payload; clients must not embed default URIs.

## 3. No Client-Side Screen-Type Enums

- There must be **no** `ScreenType` enum (`SCOREBOARD`, `GAME_DETAIL`, etc.)
  on any client.
- Every screen is fetched via a generic `fetchScreen(endpoint)` call.
  The server's response (`refreshPolicy`, `navigation`, data bindings)
  drives all runtime behaviour — not a client enum.

## 4. Ably / Real-Time Messages Are Opaque JSON

- Ably messages must be treated as **opaque `Map<String, Any?>`** (or the
  platform equivalent) — never as typed data classes like `LinescoreUpdate`.
- The section's `dataBindings` configuration (source → target path mappings)
  is the **only** mechanism for applying real-time updates to section data.
- `DataBindingResolver.applyBindings()` is the single code path for mapping
  incoming messages to section data. No duplicate logic elsewhere.

## 5. Images Are Always Server-Provided

- Every image URL must originate from the server response (e.g.
  `thumbnailUrl`, `fallbackThumbnailUrl`, `imageUrl`).
- Clients must **never** construct image URLs from patterns like
  `cdn.nba.com/logos/{tricode}.png`.

## 6. Error States Are First-Class Sections

- `ErrorState` is a section type in the schema, just like
  `GamePanel` or `ContentRail`.
- When an error occurs (bad game ID, network failure, missing data),
  the server **or** client should produce an `ErrorState` section rather
  than showing a blank screen or falling back to a hardcoded default.
- Clients should **never** fall back to a hardcoded ID (e.g. a default
  gameId) — instead they surface an ErrorState.

## 7. Section Routers Must Handle Unknown Types Gracefully

- Both `SectionRouter.kt` (Android) and `SectionRouter.tsx` (web) must
  silently skip unknown section types with a debug/warning log.
- The `SUPPORTED_SECTION_TYPES` set on Android and the `switch` in
  `SectionRenderer` on web are the **accepted** coupling points where
  new types require a client update — this is by design.

## 8. One Fetch Path, No Dedicated Repo Methods

- `SduiRepository` should expose **one** generic screen-fetch method
  (`fetchScreen(path, variant)`) plus `fetchRawJson()` for direct
  polling URLs.
- Dedicated methods like `getGameDetail()` or `getScoreboard()` with
  hardcoded paths are prohibited.

## 9. Refresh & Polling Are Server-Driven

- `refreshPolicy` on each section tells the client *how* and *when*
  to refresh (poll interval, SSE channel, direct URL).
- Clients must **not** hard-code polling intervals or decide which
  sections to poll based on screen type.

## 10. URI Resolution

- `resolveEndpoint(uri)` converts an `nba://` URI to a server path
  (`/sdui/{path}`).  It must be a **simple prefix swap** with no
  special-case branching for specific screens.

## 11. Platform Header Is Never Hardcoded

- Every client must send `X-Platform` on every request (e.g. `android`,
  `web`, `ios`).
- The server must **never** assume a platform via `defaultValue`. Use
  `required = false` with no default — composition logic must treat a
  missing platform safely (e.g. form layout falls back to vertical).
- Platform-specific composition decisions (form layout direction, section
  density) are resolved from this header, not from hardcoded strings.

## 12. Never Swallow Exceptions Silently

- Catch blocks that return `null` or a fallback **must** log the
  exception with enough context to diagnose the failure (class name,
  section ID, error message).
- Example: the `convert<T>()` helper in `SectionUiAdapters.kt` logs
  the target type and full stack trace before returning null.
- Silent `catch (_: Exception) { null }` patterns are prohibited.

## 13. Schema Enums Must Cover Server Output

- When the server sends a value for an enum-typed field (e.g.
  `contentType`), that value **must** exist in the schema's enum list.
- jsonschema2pojo generates strict enum deserializers that throw on
  unknown values, which cascades into silent section render failures.
- Before adding a new enum value to server composition, add it to the
  schema first, then run `make codegen`.

## 14. Renderers Are Presentation-Only

- Renderers map server data to native views — layout and visual styling.
  They must **not** contain business logic, behaviour branching, or
  conditional flows that could be expressed server-side.
- Prefer **composition-only changes** on the server over touching a
  renderer when reusing an existing section type.
- Resolve interaction behaviour through **shared action infrastructure**
  (router / executor / helpers), not per-section custom parsing.
- Reuse shared action helpers for `onTap`, `onVisible`, and similar
  triggers across section types.

### Decision Checklist Before Editing a Renderer

1. Can this be solved by **server composition only**?
2. Can this be solved by **schema / action payload changes** only?
3. If client code is required, can it go in **shared infrastructure**
   rather than a specific section renderer?
4. For simple/stateless UI, can it be expressed as a server-composed
   **`AtomicComposite`** instead of a new section type?

If (1), (2), or (4) is true → do **not** add section-specific client
behaviour.

## 15. Section vs AtomicComposite — When Client Code Is Justified

The default is **server-composed `AtomicComposite`**. A dedicated client
section renderer is the exception, justified only when the client must
own state or integrate a platform SDK that cannot be abstracted into the
server contract.

### Decision Tree

```
Is the UI stateless and ≤80 LOC with no platform SDK dependency?
 ├─ YES → AtomicComposite (server-composed)
 └─ NO
      Does it require client-owned state or a platform SDK?
       ├─ YES → Section renderer (permanent section)
       │    Examples of justified reasons:
       │    • Platform SDK integration (IAP → SubscribeHero/SubscribeBanner,
       │      ad SDK → AdSlot)
       │    • Network-driven real-time state (Ably SSE subscriptions →
       │      GamePanel, BoxscoreTable)
       │    • Complex client interaction state (tab selection → TabGroup,
       │      form validation → FormRenderer, sort/filter → SeasonLeadersTable)
       └─ NO → AtomicComposite — push it server-side
```

### Rationale

Nine section types were migrated to `AtomicComposite` because they had
**zero client-owned state** — the server fully controlled their layout and
content. The eight permanent sections remain because each one requires
behaviour the server cannot own:

| Section            | Why it stays client-side                        |
|--------------------|--------------------------------------------------|
| GamePanel          | Ably SSE subscription, live score state machine  |
| BoxscoreTable      | Real-time data binding, expandable rows          |
| SeasonLeadersTable | Sort/filter interaction state                    |
| TabGroup           | Tab selection state, nested section hosting      |
| FormRenderer       | Validation state, platform keyboard integration  |
| SubscribeHero      | Platform IAP SDK integration                     |
| SubscribeBanner    | Platform IAP SDK integration                     |
| AdSlot             | Platform ad SDK lifecycle                        |

Even for permanent sections, **visual configuration must be server-driven**.
The renderer reads styling knobs (colors, sizes, layout flags) from the
server payload — it never hardcodes visual variants. The section exists
only because it owns client-side *behaviour*, not because it owns *appearance*.

### AtomicComposite Limits

- Max depth: **6**
- Max children per container: **20**
- Max total nodes: **50**

Server validates these limits at composition time; clients have a
defensive depth guard.

### DisplayGrid Special Case

- Tabular data that is **display-only, server-ordered** → `DisplayGrid`
  atomic primitive.
- Tabular data with **any interactivity** (sort / filter / expand) →
  section renderer, not DisplayGrid.
