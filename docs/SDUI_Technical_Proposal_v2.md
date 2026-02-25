# SDUI Platform - Technical Proposal (v2)

**A developer-oriented proposal for SDUI architecture, runtime behavior, and implementation patterns aligned to the current requirements baseline.**

---

> **Scope note (normative):** The requirements summary is the source of truth for commitments. This proposal describes technical implementation direction and examples aligned to those commitments.

## Revision History

| Date | Summary |
|---|---|
| 2026-02-20 | Initial v2 — full technical proposal aligned with requirements baseline and ADR-driven decisions. Covers schema, data binding, action system, screen state, codegen, platform coverage, and gap analysis (9i–9o). Appendix A/B with typed request envelope and expanded response examples. |
| 2026-02-24 | Added i18n section (9p) with server-resolved default + `stringKeys` on data bindings. Removed `layoutHints` from schema and appendix examples (moved to ADR-008 for evaluation). Added locale transport policy to 9o and 9p. |
| 2026-02-25 | Added `stringKeys` to binding JSON examples in section 3 and Appendix B. Added JSON snippet with explanation to i18n section (9p). Added revision history. |
| 2026-02-25 | Established platform-aware composition as settled architectural position: shared schema, shared data pipeline, per-platform-family composition. Renamed `fallbackUrl` → `webUrl` in action contract (section 4). Updated platform coverage (section 8). |

---

## Current Governance (This Document)

- Requirements define **what** must be true. ADRs capture **why** and final cross-functional decisions.
- This proposal is technical and implementation-focused.
- Areas mapped to pending ADRs are directional until cross-functional approval is complete.

Current decisions reflected here:

- Composition ownership: SDUI composer/aggregator is the semantic source of truth.
- **Platform-aware composition: shared schema and data pipeline; per-platform-family composition. Not an ADR — the only practical architecture for phone, tablet, web, and TV.**
- Action precedence: nested/subsection > section > screen-default.
- Navigate actions: `targetUri` for native deeplinks, `webUrl` for web — both are first-class, not primary/fallback.
- Experiment model: support client hint and server-authoritative assignment; server assignment wins on conflict.
- Transport policy: support GET and POST by route context and cacheability needs.
- Ad boundary: auction/targeting is delegated; SDUI carries ad placement contract.
- Caching direction: section-first caching with optional screen snapshot caching.

---

## What SDUI Is

Server-Driven UI is an architecture where the server controls what sections appear on screen, their order, their data, and interaction behavior. Clients render the contract with native platform UI systems (SwiftUI, Jetpack Compose, React).

The server owns **composition**. Platform teams own **rendering quality** (native motion, accessibility, focus/gesture behavior, platform idioms).

This is not a shared runtime or webview wrapper. It is a shared schema + composition contract with native renderers per platform.

### Platform-Aware Composition (Settled Position)

The SDUI schema is universal — all platforms share the same vocabulary of section types, action types, and data models. However, the composition layer produces **platform-family-specific responses**. A tvOS 10-foot UI, a phone, and a web browser have fundamentally different information density, interaction models, and layout requirements. Serving identical responses to all platforms forces a lowest-common-denominator experience.

**Architecture:**

```
┌─────────────────────────────────────────┐
│            SDUI Schema                   │
│  (shared vocabulary, codegen, contract)  │
└──────────────────┬──────────────────────┘
                   │
     ┌─────────────┼─────────────┐
     ▼             ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  Mobile  │ │   Web    │ │    TV    │
│ Composer │ │ Composer │ │ Composer │
└─────┬────┘ └─────┬────┘ └────┬─────┘
      │            │            │
      ▼            ▼            ▼
  Platform-tailored responses using
  the same section types and structure
```

**What is shared across all platforms:**

- Schema definitions (JSON Schema) — the universal contract
- Codegen (typed models per platform from one schema)
- Upstream data fetching and transformation — one integration pipeline
- Section-type semantics — a `ScoreboardHeader` means the same thing everywhere
- Action and data binding structure

**What differs per platform family:**

- Which sections are composed and in what order
- Information density and layout intent
- Action URIs (`targetUri` for native deeplinks, `webUrl` for web)
- Image dimensions and density parameters
- Interaction triggers (touch vs. D-pad vs. hover)

The client identifies its platform family via the request envelope (`platform.name`, `platform.deviceClass`). The composition service routes to the appropriate composer. The cost is additional server-side composition logic — this is acceptable and is where the investment should go. The alternative (one response for all platforms) produces a mediocre experience everywhere.

---

## 1. Core Architecture

### Data Flow

```
Client App -> Composition Service -> Upstream Data/Eligibility/Experiment Systems
```

1. Client requests a screen with schema version and request context.
2. Composition service assembles sections, resolves variants/context, and returns a schema-compliant response.
3. Client router maps sections to native renderers.
4. Runtime systems execute refresh policy, data bindings, and action chains.

### Server Controls vs Client Controls


| Server controls                     | Client controls                     |
| ----------------------------------- | ----------------------------------- |
| section composition and ordering    | native component rendering          |
| section payload fields and defaults | platform gesture/focus handling     |
| refresh policies and bindings       | platform networking primitives      |
| action definitions and sequencing   | integration with nav/analytics SDKs |
| experiment/variant composition      | platform accessibility mechanics    |


### Composition Ownership Requirement

SDUI semantic composition is owned by the SDUI composer/aggregator. CoreAPI-derived composition can be used as transitional input, but is not the long-term semantic owner.

Reference: ADR-002

### Response Hierarchy

Every response follows `Screen -> Section -> Component`, where each section can carry data, refresh policy, bindings, actions, and optional layout hints.

```json
{
  "screen": {
    "id": "game-detail",
    "schemaVersion": "1.1",
    "sections": [
      {
        "id": "scoreboard-001",
        "type": "ScoreboardHeader",
        "data": { "...": "..." },
        "refreshPolicy": { "type": "sse", "channel": "{gameId}:linescore" },
        "dataBindings": { "bindings": [ { "sourcePath": "$.home.score", "targetPath": "home.score" } ] },
        "actions": [ { "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0022500384/boxscore" } ]
      }
    ]
  }
}
```

---

## 2. Schema Design

### Key Schema Decisions

- Schema is the contract source of truth.
- Schema defines semantic section types, not atomic layout primitives.
- Codegen generates typed models only, not UI code.
- Schema evolution is additive-first and version-aware.
- Unknown section/action types must degrade gracefully (skip/no-op).
- **Subsection actions are required** at nested component level.
- **Request context is contract input** and must be typed.

### Schema Fragment Example

```json
{
  "$id": "https://nba.example/sdui-schema.json",
  "definitions": {
    "Section": {
      "type": "object",
      "required": ["id", "type", "data"],
      "properties": {
        "id": { "type": "string" },
        "type": { "type": "string" },
        "data": { "type": "object" },
        "refreshPolicy": { "$ref": "#/definitions/RefreshPolicy" },
        "dataBindings": { "$ref": "#/definitions/DataBindings" },
        "actions": {
          "type": "array",
          "items": { "$ref": "#/definitions/Action" }
        },
        "subsections": {
          "type": "array",
          "items": { "$ref": "#/definitions/Subsection" }
        }
      }
    }
  }
}
```

---

## 3. Data Binding System

The server declares how section fields stay fresh after initial render.

### Refresh Policy Types


| Type     | Mechanism           | Typical use           |
| -------- | ------------------- | --------------------- |
| `static` | no refresh          | editorial content     |
| `poll`   | interval HTTP fetch | slower-changing stats |
| `sse`    | realtime channel    | live score/clock      |


### Field-Level Binding Example

```json
"dataBindings": {
  "bindings": [
    { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
    { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" },
    { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" },
    { "sourcePath": "$.period", "targetPath": "period" }
  ],
  "stringKeys": {
    "gameStatusText": "game_status_text"
  }
}
```

`stringKeys` is optional — omit when no bound fields need client-side translation. See [9p. Internationalization](#9p-internationalization-i18n).

Binding semantics must be behaviorally identical across Android, iOS, and Web through shared fixtures and contract tests.

---

## 4. Action System

Actions are supported at three scopes:

- screen defaults (optional)
- section actions
- nested/subsection actions

### Triggers


| Trigger       | Fires when                |
| ------------- | ------------------------- |
| `onTap`       | touch/click/remote select |
| `onLongPress` | hold gesture              |
| `onVisible`   | enters viewport           |
| `onSwipe`     | directional swipe         |
| `onFocus`     | TV focus enters item      |


### Action Types


| Type        | Purpose              | Key fields                                 |
| ----------- | -------------------- | ------------------------------------------ |
| `navigate`  | route/deeplink       | `targetUri` (native deeplink), `webUrl` (web equivalent) |
| `analytics` | fire beacon          | `eventName`, `eventParams`, `destinations` |
| `mutate`    | update screen state  | `stateKey`, `stateValue`                   |
| `dismiss`   | close overlay/screen | `target`                                   |
| `refresh`   | force fetch          | `target`                                   |


### Precedence and Composability

- Precedence rule: nested/subsection > section > screen-default.
- A single trigger can execute multiple actions in sequence.
- Analytics-first is recommended before navigation/dismiss

Reference: ADR-005

### Precedence Example

```json
{
  "screenDefaults": {
    "actions": [{ "trigger": "onTap", "type": "analytics", "eventName": "screen_tap" }]
  },
  "section": {
    "actions": [{ "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/0022500384" }],
    "subsections": [
      {
        "id": "home-team-hotspot",
        "actions": [
          { "trigger": "onTap", "type": "analytics", "eventName": "home_team_tap" },
          { "trigger": "onTap", "type": "navigate", "targetUri": "nba://team/1610612752" }
        ]
      }
    ]
  }
}
```

---

## 5. Screen-Level State Management

Screen state handles cross-section UI state that changes locally through `mutate` actions.

Examples:

- selected tab
- expanded/collapsed section ids
- selected filter chip

```json
{
  "state": {
    "selectedTab": "recap",
    "expandedCards": []
  }
}
```

---

## 6. Client Infrastructure - Written Once

Each client platform builds these systems once:


| System             | Responsibility                                | Inputs                           |
| ------------------ | --------------------------------------------- | -------------------------------- |
| Section Router     | section type -> native renderer mapping       | `section.type`                   |
| State Manager      | apply binding patches, hold observable models | `refreshPolicy`, `dataBindings`  |
| Action Executor    | dispatch action chains to app systems         | `actions`                        |
| Screen State Store | shared mutable screen state                   | `screen.state`, `mutate` actions |
| Channel Manager    | polling/SSE lifecycle and reconnect policy    | section refresh config           |


### Section Router Example (Android)

```kotlin
@Composable
fun SectionRouter(section: SduiSection, onAction: (SduiAction) -> Unit) {
    when (section.type) {
        "ScoreboardHeader" -> ScoreboardHeaderRenderer(section, onAction)
        "StatLine" -> StatLineRenderer(section, onAction)
        "ContentRail" -> ContentRailRenderer(section, onAction)
        "AdSlot" -> AdSlotRenderer(section, onAction)
        else -> Unit // Unknown type -> graceful skip
    }
}
```

### Section Renderer Example (Android)

```kotlin
@Composable
fun ScoreboardHeaderRenderer(section: SduiSection, onAction: (SduiAction) -> Unit) {
    val data = mapScoreboard(section)
    Row(horizontalArrangement = Arrangement.SpaceBetween) {
        TeamColumn(team = data.awayTeam, onTap = { onActionTap(section, "awayTeam", onAction) })
        Text(data.gameStatusText)
        TeamColumn(team = data.homeTeam, onTap = { onActionTap(section, "homeTeam", onAction) })
    }
}
```

---

## 7. Codegen Pipeline

```
schema/sdui-schema.json
  -> jsonschema2pojo -> Java models (server + Android)
  -> quicktype -> Swift models (iOS)
  -> quicktype -> TypeScript models (Web)
```

- Schema remains the source of truth.
- Generated models are never hand-edited.
- Renderers map generated models to platform-native UI components.

---

## 8. Platform Coverage

Each platform family receives a tailored composition from the server while sharing the same schema, codegen, and data pipeline. The client sends its platform identity in the request envelope; the composition service routes to the appropriate composer.

| Platform family | Platforms              | Renderer                 | Transport                    | Interaction model         | Composition profile |
| --------------- | ---------------------- | ------------------------ | ---------------------------- | ------------------------- | ------------------- |
| Mobile          | Android phone/tablet   | Jetpack Compose          | OkHttp + SSE                 | touch gestures            | Full section set, responsive breakpoints |
| Mobile          | iOS phone/tablet       | SwiftUI                  | URLSession + SSE/EventSource | touch gestures            | Full section set, responsive breakpoints |
| Web             | Browser                | React                    | fetch + SSE/EventSource      | mouse/touch/keyboard      | Wide layout, `webUrl` preferred for navigation |
| TV              | tvOS, Fire TV          | Compose/SwiftUI variants | platform-specific            | D-pad focus + select      | Reduced sections, large art, simplified interactions |


---

## 9. Gaps - Production Capabilities Still To Lock

### 9i. Impression Deduplication

- Finalize threshold and dwell policy.
- Finalize dedup scope (screen/session/server policy model).

Reference: ADR-009

### 9j. A/B Testing Integration

- Support client hint and server-authoritative assignment.
- On conflict, server-authoritative assignment wins.
- Response must echo final assignment used.

Reference: ADR-006

### 9m. Form-Factor Layout Manager

- Server provides layout intent.
- Client applies native best-fit rules.
- Contract defines fallback behavior for unsupported hints.

Reference: ADR-008

### 9n. Ad Support as First-Class Primitive

- Represent ads as explicit SDUI primitive (for example `AdSlot`).
- Define semantic ad fields and failure/no-fill behavior (provider, ad unit path, sizes, targeting, collapse behavior).
- Keep ad auction/targeting delegated outside SDUI contract execution.

Reference: ADR-007

### 9o. Composition Input and Request Model

Composition must support a typed request envelope:

- screen/entity identifiers
- platform/app version/device class
- locale/region/timezone
- auth context (`Authorization` header)
- entitlement/restriction context
- experiment hints/assignments
- client capabilities
- traceId

Transport and caching:

- Support GET and POST based on screen/section context.
- Never put auth tokens in query params.
- Cacheability classes: `public`, `contextual`, `personalized`, `live`.

References: ADR-003, ADR-004

### 9p. Internationalization (i18n)

The composition server pre-translates all text in the initial response based on locale (`locale` query parameter on GET, body field on POST; default `en`). However, real-time updates (Ably SSE) and direct CDN polling bypass the server — the DataBindingApplier writes external source values directly into section data with no translation opportunity.

**Proposed implementation: server default + optional string keys on data bindings.**

- **Initial load:** Server resolves locale from the request and sends pre-translated text in all string fields. Clients render as-is.
- **Binding updates from external sources:** An optional `stringKeys` map on `DataBinding` allows the server to attach translation keys to any bound field where the external source may deliver untranslated strings. After applying a binding, the client checks for a corresponding string key and resolves via platform-native i18n (Android `strings.xml`, iOS `Localizable.strings`, web i18n library). Falls back to the raw value if no local translation exists.

**Schema addition:**

- Add optional `stringKeys: { [targetPath]: string }` map to the `DataBinding` definition.
- General-purpose mechanism — applies to any string field arriving via a binding (status text, player names, team names, labels, etc.). The server decides which fields need keys.

```json
"dataBindings": {
  "bindings": [
    { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
    { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" }
  ],
  "stringKeys": {
    "gameStatusText": "game_status_text"
  }
}
```

In this example, `homeTeam.score` is numeric and needs no translation. `gameStatusText` may arrive untranslated from the SSE source, so the server attaches a string key. After applying the binding, the client resolves `game_status_text` via platform-native i18n; if no local translation exists, the raw value is used.

**Rejected alternatives:**

- Upstream localization (route Ably/CDN through translation layer) — unacceptable fanout infrastructure cost.
- Server-side SSE proxy (subscribe, translate, re-publish) — adds latency and infrastructure for no benefit.
- Format templates (server sends patterns, client fills values) — cannot guarantee all translatable content fits a template.

---

## 10. Requirement Status


| Requirement                    | Status  | ADR     | Notes                                               |
| ------------------------------ | ------- | ------- | --------------------------------------------------- |
| Core composition model         | Partial | ADR-002 | implemented directionally, transition still active  |
| Request context envelope       | Gap     | ADR-003, ADR-004 | final schema and compatibility policy pending |
| Action scope and precedence    | Partial | ADR-005 | rule set clear; needs broader fixtures              |
| Experiment conflict handling   | Partial | ADR-006 | rule defined; integration shape pending             |
| Ad primitive contract          | Gap     | ADR-007 | required field policy + fallback semantics pending  |
| Layout manager contract        | Gap     | ADR-008 | cross-form-factor hints and fallback policy pending |
| Impression semantics           | Gap     | ADR-009 | threshold/dwell/dedup governance pending            |
| Contract testing/observability | Gap     | —       | broader test corpus + dashboards pending            |
| Internationalization (i18n)    | Gap     | —       | server-resolved default + string keys on bindings   |


---

## ADR Approvals Pending

All pending ADRs are tracked in the Requirement Status table above. Until approved, those requirements remain directional and may be refined.


---

## 11. Non-Normative Context: Risks, Rationale, and Next Steps

1. Schema governance is permanent platform work.
2. Debugging crosses composition, schema, codegen, runtime, and renderer layers.
3. Cold start overhead requires caching and strict performance budgets.
4. Contract-test depth is the primary control against cross-platform drift.
5. Runtime binding consistency across platforms is mandatory.

### 11a. Why Build It Anyway

The alternative is duplicated platform composition logic and drift in feature behavior. SDUI centralizes composition velocity while preserving native rendering quality.

### 11b. Recommended Next Steps

1. Lock request envelope and transport/cache policy (ADR-003, ADR-004).
2. Lock action scope/precedence and experiment model (ADR-005, ADR-006).
3. Ship subsection actions + request-envelope schema updates with fixtures.
4. Introduce ad primitive (ADR-007) and resolve layout strategy (ADR-008).
5. Finalize impression semantics and enforce analytics/runtime conformance (ADR-009).

---

## Appendix A: Composition Request Example (Typed Envelope)

```json
{
  "screenId": "game-detail",
  "entity": { "type": "game", "id": "0022500384" },
  "platform": {
    "name": "android",
    "appVersion": "8.3.0",
    "osVersion": "14",
    "deviceClass": "phone",
    "capabilities": {
      "sse": true,
      "onFocus": false
    }
  },
  "entitlements": { "leaguePass": true, "blackoutEligible": false },
  "experiments": { "gd_tab_order_v2": "client_hint_variant_b" },
  "traceId": "8d68d9d5-4512-4ff4-8d55-1f1b20ea4e11"
}
```

---

## Appendix B: Complete SDUI Response Example (Expanded)

```json
{
  "meta": {
    "traceId": "8d68d9d5-4512-4ff4-8d55-1f1b20ea4e11",
    "schemaVersion": "1.1",
    "assignment": {
      "gd_tab_order_v2": "server_variant_a"
    },
    "cacheability": "contextual"
  },
  "screen": {
    "id": "game-detail",
    "state": {
      "selectedTab": "recap",
      "expandedCards": []
    },
    "actions": [
      {
        "trigger": "onTap",
        "type": "analytics",
        "eventName": "screen_tap",
        "eventParams": { "screenId": "game-detail" }
      }
    ],
    "sections": [
      {
        "id": "scoreboard-001",
        "type": "ScoreboardHeader",
        "data": {
          "homeTeam": {
            "teamId": "1610612752",
            "teamTricode": "NYK",
            "teamName": "New York Knicks",
            "score": 104,
            "logoUrl": "https://cdn.nba.com/logos/nba/1610612752/global/L/logo.svg"
          },
          "awayTeam": {
            "teamId": "1610612751",
            "teamTricode": "BKN",
            "teamName": "Brooklyn Nets",
            "score": 98,
            "logoUrl": "https://cdn.nba.com/logos/nba/1610612751/global/L/logo.svg"
          },
          "gameStatusText": "4:32 Q3",
          "period": 3
        },
        "refreshPolicy": {
          "type": "sse",
          "channel": "0022500384:linescore",
          "retryMs": 1500
        },
        "dataBindings": {
          "bindings": [
            { "sourcePath": "$.homeTeam.score", "targetPath": "homeTeam.score" },
            { "sourcePath": "$.awayTeam.score", "targetPath": "awayTeam.score" },
            { "sourcePath": "$.gameStatusText", "targetPath": "gameStatusText" },
            { "sourcePath": "$.period", "targetPath": "period" }
          ],
          "stringKeys": {
            "gameStatusText": "game_status_text"
          }
        },
        "actions": [
          {
            "trigger": "onTap",
            "type": "analytics",
            "eventName": "scoreboard_tapped",
            "eventParams": {
              "gameId": "0022500384",
              "sectionId": "scoreboard-001"
            }
          },
          {
            "trigger": "onTap",
            "type": "navigate",
            "targetUri": "nba://game/0022500384/boxscore",
            "webUrl": "https://www.nba.com/game/0022500384"
          }
        ],
        "subsections": [
          {
            "id": "home-team-hotspot",
            "actions": [
              {
                "trigger": "onTap",
                "type": "analytics",
                "eventName": "home_team_tapped",
                "eventParams": { "teamId": "1610612752" }
              },
              {
                "trigger": "onTap",
                "type": "navigate",
                "targetUri": "nba://team/1610612752"
              }
            ]
          }
        ]
      },
      {
        "id": "stats-001",
        "type": "StatLine",
        "data": {
          "title": "Top Performers",
          "players": [
            { "name": "Jalen Brunson", "team": "NYK", "stats": { "points": 28, "assists": 7, "rebounds": 3 } },
            { "name": "Mikal Bridges", "team": "BKN", "stats": { "points": 22, "assists": 4, "rebounds": 5 } }
          ]
        },
        "refreshPolicy": {
          "type": "poll",
          "intervalSec": 30,
          "url": "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_0022500384.json"
        },
        "actions": [
          {
            "trigger": "onVisible",
            "type": "analytics",
            "eventName": "section_impression",
            "eventParams": { "sectionId": "stats-001", "policy": "once_per_screen" }
          }
        ]
      },
      {
        "id": "tab-group-001",
        "type": "TabGroup",
        "data": {
          "tabs": [
            { "id": "recap", "label": "Recap" },
            { "id": "boxscore", "label": "Box Score" },
            { "id": "play-by-play", "label": "Play-by-Play" }
          ]
        },
        "actions": [
          { "trigger": "onTap", "type": "mutate", "stateKey": "selectedTab", "stateValue": "boxscore" },
          { "trigger": "onTap", "type": "analytics", "eventName": "tab_selected", "eventParams": { "tabId": "boxscore" } }
        ]
      },
      {
        "id": "adslot-001",
        "type": "AdSlot",
        "data": {
          "provider": "gam",
          "ad_unit_path": "/21234567/sports/nba/homepage_top",
          "sizes": [[320, 50], [728, 90]],
          "targeting": {
            "section": "homepage",
            "content_type": "live_game"
          },
          "collapse_on_empty": true
        },
        "refreshPolicy": { "type": "static" }
      },
      {
        "id": "content-rail-001",
        "type": "ContentRail",
        "data": {
          "title": "More Games Tonight",
          "items": [
            { "id": "game-1", "title": "LAL vs GSW", "subtitle": "7:30 PM ET" },
            { "id": "game-2", "title": "BOS vs MIA", "subtitle": "8:00 PM ET" }
          ]
        },
        "refreshPolicy": { "type": "static" },
        "actions": [
          { "trigger": "onTap", "type": "navigate", "targetUri": "nba://game/{itemId}" }
        ]
      }
    ]
  }
}
```

This expanded example demonstrates:

- typed request envelope as composition input
- server-echoed experiment assignment
- screen/section/subsection action scopes
- precedence-compatible nested actions
- mixed refresh policies (sse, poll, static)
- field-level data binding patch behavior
- ad section as explicit primitive
- screen state mutation with analytics chaining

