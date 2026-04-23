# SDUI Prototype — Development Rules

These rules govern all SDUI work in this repository. They exist to ensure the
server retains full control of the UI and that client releases are never
required for layout, content, or data-flow changes.

> **Building a new client?** See
> [`docs/client-implementors-contract.md`](docs/client-implementors-contract.md)
> for the platform-agnostic architecture blueprint, build checklist, and
> pseudocode algorithms. The rules below are the constraints; the contract
> is the construction guide.

> **Agent file-access conventions (standing instruction):**
> `docs/appendix-kitchen-sink.md` is **owner-maintained and off-limits to agents**.
> Do not read, grep, edit, or propose changes to this file. Do not reference its
> line numbers or contents. When a schema, composer, or variant change would
> affect appendix snippets, leave a note in the relevant plan or PR saying the
> appendix needs a manual sweep by the owner — do not attempt the sweep yourself.
> The file is too large to load into context profitably; the owner updates it
> out-of-band after related changes land.

> **Build / test commands (standing instruction):**
> Always prefer the repo-root `Makefile` targets over raw `xcodebuild`,
> `gradlew`, `npm`, or `swift build` invocations. The Makefile sets
> environment defaults (`SDUI_DISABLE_ABLY=1` for Intel-Mac / CI iOS
> builds, scheme / destination / derived-data paths, log filters) that
> are load-bearing on some hosts — running the underlying tool directly
> is a common source of spurious failures (e.g. `no such module 'Ably'`
> on iOS when `SDUI_DISABLE_ABLY` is not set). Known targets:
>
> - `make codegen` — regenerate typed models from `schema/sdui-schema.json`.
> - `make dev-server` / `make dev-web` / `make dev-android` / `make dev` — run
>   the server, web client, Android client, or all three in dev mode.
> - `make stop` / `make stop-server` / `make stop-web` / `make stop-android` /
>   `make ios-stop` — stop running processes started by the `dev-*` targets.
> - `make ios-build` — compile `SduiCore` against the iPhone 15 Pro Max
>   simulator with Ably gated out (the default). Use this for compile
>   checks from agent tasks.
> - `make ios-test` / `make ios-test-clean` — run the `SduiCore` test
>   suite; the `-clean` variant wipes `DerivedData` first when a cached
>   swiftmodule is causing stale diagnostics.
> - `make ios-demo-project` / `make ios-run` — regenerate the SduiDemo
>   Xcode project from `project.yml` and launch it in the simulator.
>
> If a target you need is missing, add it to the Makefile rather than
> teaching the agent a bespoke incantation. Override defaults per-invocation
> with `VAR=value make target` (e.g. `SDUI_DISABLE_ABLY=0 make ios-build` on
> arm64 hosts).

---

## 1. Schema Is the Single Source of Truth

- **All section types** must be defined in `schema/sdui-schema.json`.
- After modifying the schema, **always regenerate** models by running
  `make codegen` from the repo root.
- Generated code lives in the following locations — never edit those files
  by hand, regenerate them with `make codegen`:
  - Java POJOs (Spring server only): `codegen/build/generated-sources/jsonschema2pojo/`
  - Kotlin models (Android client): `android/sdui-core/src/main/java/com/nba/sdui/core/models/generated/SduiModels.kt`
  - Swift models (iOS client): `ios/Sources/SduiCore/Models/SduiModels.swift`
  - TypeScript models (web client): `web/src/generated/SduiModels.ts` (consumed via the `@sdui/models` Vite / tsconfig alias)

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

- `ErrorState` is a server-composed `AtomicComposite` section
  (built by `AtomicCompositeBuilder.buildErrorState()`). It is a
  first-class section in every response, just like `GamePanel`.
- When an error occurs (bad game ID, network failure, missing data),
  the server **or** client should produce an `ErrorState` section rather
  than showing a blank screen or falling back to a hardcoded default.
- Clients should **never** fall back to a hardcoded ID (e.g. a default
  gameId) — instead they surface an ErrorState.

## 7. Section Routers Must Handle Unknown Types Gracefully

- Each platform's section router — `SectionRouter.kt` (Android),
  `SectionRouter.tsx` (web), `SectionRouter.swift` (iOS) — must
  silently skip unknown section types with a debug/warning log.
- The `SUPPORTED_SECTION_TYPES` set on Android, the `switch` in
  `SectionRenderer` on web, and the `switch section.type` in iOS's
  `SectionRouter` are the **accepted** coupling points where new
  types require a client update — this is by design.

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

## 13. Schema Is the Server-Client Contract

The JSON schema at `schema/sdui-schema.json` is the wire-level contract
between server and clients. Every field, every enum value, every type
is part of that contract. Clients decode strictly on purpose: if the
server emits a value the schema does not describe, the client fails
loudly rather than degrading silently into a blank section or a
misrendered widget.

### Invariants

- **Server output must conform to the schema.** Before composition code
  emits a new enum value, a new field, or a new type, it goes into
  `schema/sdui-schema.json` first. Clients are regenerated
  (`make codegen`) from the schema; hand-edited client models must be
  kept in sync.
- **Strict decoders are intentional.** `jsonschema2pojo` (Java),
  `quicktype` (Kotlin / TypeScript), and Swift `Codable` all generate
  strict deserializers that throw on unknown enum values. That
  behaviour is load-bearing: it converts contract violations into
  test-visible crashes instead of hard-to-diagnose silent render
  failures in production.
- **Unknown values on the wire mean one of two things.** Either the
  server is emitting something the schema forbids (schema update
  missing — fix the schema) or the client is older than the schema it
  is receiving (forward-compatibility case — the render-time fallbacks
  named in Rule 16 / Rule 18 apply at the renderer layer, not at the
  decoder).
- **The schema is documentation of intent.** It is not just a build
  input — it is what future engineers read to understand what the
  server can say. Keep descriptions honest, keep enums tight, mark
  deprecated values explicitly.

### When adding a new enum value

1. Add the value to the schema enum.
2. Run `make codegen` to regenerate typed models across platforms.
3. Update each platform's renderer (or add a fallback) so the new
   value renders something sensible before the server starts emitting
   it.
4. Only then update the server to emit the value.

### Relationship to other rules

- **Rule 18** decides *whether* a given semantic lives in the schema
  at all. Server-driven decisions may not need a schema field;
  client-realized semantic vocabularies always do.
- **Rule 16** decides what each client *does* with a known schema
  value at render time.
- This rule is the pipe between them: it keeps the vocabulary the
  server emits aligned with the vocabulary the clients expect.

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
      Does it require client-owned state, a platform SDK,
      or is it a reserved SDK integration point (see 15.1)?
       ├─ YES → Section renderer (permanent section)
       │    Examples of justified reasons:
       │    • Platform SDK integration (IAP → SubscribeHero/SubscribeBanner,
       │      ad SDK → AdSlot, video SDK → VideoPlayer)
       │    • Network-driven real-time state (Ably SSE subscriptions →
       │      GamePanel, BoxscoreTable)
       │    • Complex client interaction state (tab selection → TabGroup,
       │      form validation → Form, sort/filter → SeasonLeadersTable)
       └─ NO → AtomicComposite — push it server-side
```

### 15.1 Promotion readiness — when a permanent section is justified

A section is promoted to permanent-renderer status when **one** of:

1. **The justifying behavior exists today** — the client already owns
   state, a platform SDK, or a network-driven lifecycle that cannot
   be abstracted into the server contract.
2. **The section is a reserved SDK integration point** — a future
   platform SDK (IAP, ads, video, ...) is known to require a stable
   client-owned mount point, and the product has committed to that
   integration. The section type is pinned in the schema so the
   server can address it, emit analytics against it, and include it
   in composition decisions today — even before the SDK lands.

Sections promoted under criterion (2) are subject to an additional
constraint: **until the SDK lands, the renderer MUST render only
server-emitted content, exactly as an `AtomicComposite` section would**.
No client-side chrome defaults — no hardcoded padding, radii, shadows,
colors, gradients, or copy. The section's visible output is a function
of the server payload and nothing else. When the SDK lands, the
renderer grows to host the SDK's content *inside* server-emitted
chrome, not to replace it.

A permanent section that accumulates client-side chrome defaults
before its justifying SDK lands has, in effect, been misclassified —
remediation is to strip the chrome back to server-emitted values,
not to demote the section type.

### 15.2 Single rectangle for fixed-pixel SDK dimensions

When a speculative-SDK section reserves a **fixed-pixel rectangle**
(e.g. ad creative sizing), the server emits that rectangle exactly
once in the payload and both the pre-SDK placeholder and the SDK
itself read from the same field. The renderer MUST NOT invent
fallback dimensions.

Canonical case: `AdSlot`. `AdSlotData.sizes[0]` is the reservation
rectangle. The placeholder shown before fill (or on `collapseOnEmpty:
false` misses) uses the same `sizes[0]`. Renderer does not define a
default height. A payload missing `sizes` is a schema violation and
must fail at the decoder, not render at a client-invented size.

Sections whose dimensions are viewport-dependent (video, subscribe
surfaces, content-sized cards) are **out of scope** for this clause —
they size themselves from the composition they carry, as any
responsive layout does. The "single rectangle" rule applies only
where the SDK will claim a specific pixel reservation.

### 15.3 Schema parity for permanent-section surfaces

Permanent sections MUST have schema parity with `AtomicContainer` for
the surface that wraps their content. The `Section.surface` block
exposes the same vocabulary — `margin`, `padding`, `background`,
`cornerRadius`, `shadow`, `border` — so the server can dictate the
section's wrapping surface uniformly across every permanent section.

Naming note: the field is `surface` (not `display`) because `data` —
its sibling on the section envelope — already carries the section's
display content (atomic UI tree, table rows, form fields). Calling the
sibling field "display" would have doubled the semantics; `surface`
keeps the split clean: `data` = content, `surface` = the visual
wrapper beneath it.

Every client's `SectionRouter` wraps permanent-section output in a
shared `SectionContainer` wrapper that reads `section.surface.*` and
applies it platform-natively. Permanent section renderers MUST NOT
set their own outer padding, margin, corner radius, shadow, border,
or background — the wrapper owns all of it. Renderers are responsible
only for the **inner content** of the section (the slot contents,
the card grid, the form fields, etc.).

**Default surface** for sections that do not explicitly set
`surface`: composers call a shared helper (e.g.
`SduiUtils.defaultSurface()`) that emits a sensible default
block. Composers MAY override per-section. Clients MUST NOT invent
defaults — a section with no `surface` payload and no helper-emitted
default renders flush, without chrome.

This rule exists so the entire app's rhythm (card inset, elevation,
corner style) can shift via a single server-side helper change,
without a client release and without per-renderer drift.

### Rationale

Nine section types were migrated to `AtomicComposite` because they had
**zero client-owned state** — the server fully controlled their layout and
content. The nine permanent sections remain because each one requires
behaviour the server cannot own:

| Section            | Justifying behavior                              | Criterion |
|--------------------|--------------------------------------------------|-----------|
| GamePanel          | Ably SSE subscription, live score state machine  | 15.1 (1)  |
| BoxscoreTable      | Real-time data binding, expandable rows          | 15.1 (1)  |
| SeasonLeadersTable | Sort/filter interaction state                    | 15.1 (1)  |
| TabGroup           | Tab selection state, nested section hosting     | 15.1 (1)  |
| Form               | Validation state, platform keyboard integration  | 15.1 (1)  |
| SubscribeHero      | Reserved for IAP SDK (StoreKit / Play Billing)   | 15.1 (2)  |
| SubscribeBanner    | Reserved for IAP SDK                              | 15.1 (2)  |
| AdSlot             | Reserved for ad SDK (GAM / Amazon); fixed-pixel reservation via `sizes[0]` — see §15.2 | 15.1 (2) |
| VideoPlayer        | Reserved for video SDK (HLS/DASH, PiP, AirPlay / Chromecast, background audio)         | 15.1 (2) |

Sections under criterion 15.1 (2) render only server-emitted content
until their SDK lands — see §15.1. All permanent sections are wrapped
by the shared `SectionContainer` and read outer chrome from
`section.surface` — see §15.3.

Even for permanent sections, **visual configuration must be server-driven**.
The renderer reads styling knobs (colors, sizes, layout flags) from the
server payload — it never hardcodes visuals as a function of screen
identity, section data, or client state (e.g. "`GamePanel` on
`GameDetail` is always red"). The section exists only because it owns
client-side *behaviour*, not because it owns *appearance*.

This does **not** prohibit resolving server-emitted semantic tokens
(the `variant` wire property, resolved against `TextVariant`,
`ButtonVariant`, `ContainerVariant`, or `ImageVariant` depending on the
element's `type`) into platform-native idioms. That mapping is required
— see Rule 16.

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

## 16. Platform-Native Realization of Semantic Tokens

The server emits **semantic tokens** on a uniform `variant: string`
property of each atomic element (e.g. `variant: "titleMedium"` on a
`Text`, `variant: "primary"` on a `Button`, and — once ADR-013 lands —
`variant: "heroCard"` on a `Container` or `variant: "hero"` on an
`Image`), plus parallel token vocabularies such as `iconName: "play"`.
Which enum the `variant` string is parsed against (`TextVariant`,
`ButtonVariant`, `ContainerVariant`, `ImageVariant`) is determined by
the element's `type`. Each client is responsible for resolving those
tokens into its platform's **current design language**.

- **Expected**: iOS renders `titleMedium` with SF Pro typography and
  uses platform materials (`.ultraThinMaterial`, Liquid Glass on iOS
  26+) for surface variants. Android renders it with Roboto/NBA
  typography and uses Material surfaces (tonal elevation, Material 3
  Expressive where available). Web uses its own conventions (CSS
  surface mixins, `backdrop-filter`). An iOS app is expected to look
  like iOS; an Android app is expected to look like Android.
- **Cross-platform visual divergence is expected**, not a bug. Pixel
  parity across platforms is **not** a goal. Cross-platform
  screenshot diffing is not a meaningful regression signal for token
  output — use per-platform screenshot tests against that platform's
  design-system spec.
- **OS-version tiering is permitted** inside the renderer. Choosing
  Liquid Glass on iOS 26+ vs. `.ultraThinMaterial` on iOS 17–25 vs.
  a solid gradient fallback on older OSes is presentation, not
  business logic. Each tier must provide a reasonable fallback
  (silent rendering failures on old OSes are not acceptable).
- **This does not relax Rule 14.** Mapping a semantic token to a
  platform-native realization is presentation-only. Branching on
  screen identity, section data, or client runtime state ("if
  `screenId == GameDetail`, use red card") remains forbidden — that
  is business logic, not token resolution.
- **Existing precedents** (the pattern is already validated in
  production code):
  - `TextVariant` → `AtomicTextView.font(for:)` (iOS),
    `AtomicText.mapTypographyVariant` (Android), `variantStyles`
    (web).
  - `ButtonVariant` → platform-idiomatic button rendering on each
    client.
  - `IconTokenResolver` + `schema/icon-tokens.json` → platform-native
    icon lookup.
- **Pixel-parity exceptions** (brand takeover, launch moment,
  sponsor-locked surface) are expressed as explicit design-system
  tokens (a `parity` variant family) or intentional inline
  overrides — not by relaxing this rule.

### Interaction tokens

The same principle applies beyond visual styling. Action triggers
(`onTap`, `onLongPress`, `onVisible`, `onSwipe`, `onFocus`, `onBlur`,
`onSubmit`, ...) are **semantic interaction intents** — the server
declares *what the user did*, and each client realizes that intent
using its platform's native gesture or event system.

- `onTap` → tap recognizer (iOS), `Modifier.clickable` (Compose),
  click event (web).
- `onLongPress` → long-press recognizer (iOS/Compose), `contextmenu`
  or synthesized long-press (web).
- `onSubmit` → form submit gesture: keyboard Return / IME "Go"
  action on iOS and Android, `<form onSubmit>` on web (Enter key
  *and* submit button).
- `onVisible` → prefetch / visibility APIs on each platform
  (`UICollectionView` prefetch, `LazyListState` observers,
  `IntersectionObserver`).

As with visual tokens:

- **Cross-platform behavioural divergence is expected.** iOS may
  fire `onSubmit` from the keyboard "Go" button; Android from the
  IME action; web from Enter key *and* a submit button. That is
  correct platform-native realization, not a bug.
- **The schema enumerates the known intents.** Adding a new
  intent (e.g. `onScrollEnd`, `onPullToRefresh`) is a schema
  change that every client then maps to its best native
  equivalent.
- **Rule 13 still applies.** The schema enum must cover every
  value the server emits. This subsection does not relax strict
  decoding — it describes what each client does *after* decode,
  not how decode behaves.

See `docs/adr/013-style-tokens-for-atomic-primitives.md` for the
full style-token system, override matrix, OS-version tier map, and
governance model.

## 17. Code Comments: Describe the Code, Cite Business Constraints Only

Code comments and schema descriptions describe **what the code does**
and **what invariant it upholds**. They must **not** cite internal
engineering conventions or AI/agent coding guidelines. They **must**
cite business, product, legal, or compliance constraints when those
drive the code — 100% of the time, with enough context for a future
reader to find the source of truth.

### Do NOT reference in code or schema descriptions

- Rule numbers from this document (e.g. "per Rule 10", "Rule 4 —
  opaque JSON").
- Links, citations, or section pointers to `AGENTS.md`, agent
  personas in `prompts/agents/`, or skills in `prompts/skills/`.
- ADR numbers used as prose justification ("per ADR-005 this must…").
  Cross-references to an ADR as documentation of *where* a design is
  specified are fine in docs; they do not belong in source comments.
- Internal engineering norms written as rules ("the platform team
  requires…", "the architecture rules say…").

If the comment's meaning depends on the reader having read this
document, the explanation is in the wrong place. Explain the *why*
in the governance document; explain the *what* and the *invariant*
in the comment.

### DO reference in code (always)

Business-driven constraints must be cited in the code that implements
them, with enough pointer information that a future reader can find
the source of truth. Examples of constraints that must carry a
citation:

- **Contractual constraints.** "Per the broadcaster rights agreement
  (2026 regional package), blackout games must not expose streaming
  URLs in this section."
- **Brand guidelines.** "NBA brand guideline §4.2: team tricodes are
  always uppercase when rendered next to the team logo."
- **Legal / compliance.** "COPPA: ad targeting parameters are
  forbidden when the requester's declared age is < 13." / "GDPR:
  `deviceId` must be dropped from request envelopes after 90 days of
  inactivity."
- **Partner / sponsor constraints.** "Sponsor lock-up: the `presented
  by` row must render above the fold on game-detail during playoffs
  (2026 deal, expires 2027-06-30)."
- **Upstream data quirks.** "Stats API returns `null` team tricode
  for a 30-minute window around roster moves (ticket STATS-4812);
  fall back to the logo URL as the identifier."
- **Product-decision intent** that the code itself cannot convey
  (why a value is clamped, why a fallback exists, why a sort key is
  descending, why a refresh is coalesced).

### Rationale

Comments citing internal rule numbers rot: the rules get renumbered,
reorganized, or replaced, and the code comment drifts from reality.
Business-constraint citations **do not rot the same way** — they
point to a durable external source (a contract, a policy, a brand
document) that the engineering team does not control. Those
citations are the load-bearing part of the comment; leaving them out
makes the code unsafe to change.

### Examples

Before / after for rule-citation removal:

| Before | After |
|---|---|
| `/// Rule 10: Simple prefix swap, no special-case branching.` | `/// Simple prefix swap, no special-case branching for individual screens.` |
| `// TODO(Rule 2): bootstrap URI should come from /sdui/init.` | `// TODO: bootstrap URI should come from /sdui/init.` |
| `/// Values must cover every string the server emits (AGENTS.md Rule 13).` | *(Remove. The strict decoder already enforces this; the schema description should just describe what the enum is.)* |

Before / after for business-constraint citation:

| Before | After |
|---|---|
| `// Hide streaming URL for certain games.` | `// Blackout policy (broadcaster rights agreement, 2026 regional package): streaming URLs must be suppressed for games in the viewer's home market. See Product brief "Blackout v3" for the rule set.` |
| `// Clamp age to 13.` | `// COPPA: targeted advertising is forbidden when declared age < 13. Code path must return the un-targeted creative instead of returning 400. See Legal/Privacy/COPPA-ads.md.` |

## 18. Server-Driven vs Client-Realized Work Split

The default site for any per-platform decision is the **server**. The
server reads the `X-Platform` header and the request envelope's
`capabilities`, `osVersion`, and `deviceClass`, and composes a response
tailored to that platform. This is the position that protects the
primary KPI of this architecture — **fewer client releases** — by
keeping per-platform variability in a place we can redeploy in hours
instead of weeks.

Client-realized platform mapping is a **named exception**, reserved
for a small, stable set of semantic vocabularies where server-side
enumeration would force the server to track platform trivia (new SF
Symbols, new Material icons, new Liquid Glass materials, new IME
behaviours) that the client owns natively anyway. Rule 16 lists the
exceptions that exist today. New client-realized vocabularies require
explicit justification — see "Criteria for adding a new
client-realized vocabulary" below.

### The decision tree

| The server is choosing… | Decision site | Mechanism | Examples |
|---|---|---|---|
| **Content selection** — which sections, copy, or CTAs appear on this screen | **Server** (default) | Compose per-platform using `X-Platform` | Show `SubscribeHero` on iOS/Android, `SubscribeBanner` on web. Show "Download the app" only on web. Hide a `LiveActivity` CTA on non-iOS. |
| **Capability gating** — does this client's runtime support the delivery mechanism, SDK, or OS feature the section requires | **Server** (default) | Read `capabilities.*` and `osVersion` from the envelope and compose a compatible response | If `capabilities.sse = false`, set `refreshPolicy.type = "interval"`. Omit a section that needs iOS 17 when `osVersion < 17`. |
| **Asset-format selection** — the client needs a concrete URL or asset and only the server knows which format the platform can consume | **Server** (default) | Server emits the per-platform URL / asset | iOS gets the HLS manifest; Android gets the DASH manifest. iOS gets an APNs push target; Android gets FCM. |
| **Presentation of a known semantic intent** — how a thing *looks* or *feels* once the client already knows what it is | **Client** (named exception, per Rule 16) | Schema carries a neutral semantic token; each platform resolves to its native idiom | The uniform `variant` wire property resolved against `TextVariant`, `ButtonVariant`, `ContainerVariant` (ADR-013), `ImageVariant` (ADR-013) per the element's `type`; `iconName` / `sdui:*` icon tokens; `ActionTrigger` (Rule 16 "Interaction tokens" subsection) |

### When in doubt: server

If a case does not cleanly fit the "Presentation of a known semantic
intent" row, it is server-driven. Pushing a decision to the client
should be a deliberate, argued choice — not a default. More logic in
the client means more client releases, more drift across platforms,
and more ways for older app versions to fall out of sync with server
composition.

### Criteria for adding a new client-realized vocabulary

A new vocabulary may be added to the "Rule 16 exceptions" list only if
**all** of the following hold:

1. **Pure presentation.** The vocabulary carries no business content,
   no legal or compliance policy, and no capability gating. It
   describes *how* something looks or feels, not *what* is shown or
   *whether* it is shown.
2. **Platform-owned realization.** The set of valid realizations is
   owned and versioned by the platform (SF Symbols, Material icons,
   Material 3 surfaces, SwiftUI `.ultraThinMaterial`, CSS
   `backdrop-filter`), so server-side enumeration would put the
   server onto the platform's upgrade treadmill.
3. **Stable vocabulary.** The neutral semantics change much more
   slowly than their platform realizations. Typography scales and
   button roles change rarely; the SF Symbol or Material drawable that
   realizes them ships with every OS release.
4. **Documented fallback.** Unknown values fall back to a sensible
   default at render time — a new token from a newer server must not
   crash an older client (see Rule 13 on strict decode vs renderer
   fallback).
5. **Tier map documented.** Per-OS-version tiers (e.g. Liquid Glass on
   iOS 26+, `.ultraThinMaterial` on iOS 17–25) are captured in the
   registry, with current-plus-one-back as the coverage floor (per
   ADR-013).

### Scaling rationale (N + M vs N × M)

Client-realized tokens cost **N + M**: one shared vocabulary of N
semantics, one resolver per platform (M platforms). Adding a new token
is a single addition on each side. Server-per-platform realizations
cost **N × M**: every new token requires a platform branch in every
composer that emits it. The multiplicative cost grows with every new
surface, not just with new tokens.

For *content, capability, and asset* decisions the N × M cost is
acceptable because those decisions are business or policy decisions
the server has to make anyway — the only question is how many
branches. For *presentation* decisions the multiplicative cost is pure
overhead: the server does not need to be in the loop, and putting it
there means every new SF Symbol shipped by Apple becomes a server
deploy instead of a client-local resolver update.

### The same theme, different axis (Rule 15)

Rule 15 expresses the same default-toward-server stance on a different
axis: **rendering responsibility**. AtomicComposite is the default;
dedicated client section renderers are the named exception, justified
only when the section must own state, integrate a platform SDK, manage
a network-driven lifecycle, or drive animation / IAP / ad / real-time
concerns the server cannot control at composition time. Both rules
share the construction: *server by default; client when a concrete,
documented criterion is met.* Changes that push work toward the client
in either rule should meet an explicit bar, not happen by drift.

### Relationship to other rules

- **Rule 13** keeps the schema aligned with whatever the server emits
  under this rule.
- **Rule 14** still governs the client side: once a neutral semantic
  arrives, the renderer maps it to native views without branching on
  screen identity or runtime state. This rule answers *where* the
  branching happens; Rule 14 answers *what* the client is allowed to
  do with the result.
- **Rule 15** is the rendering-responsibility counterpart (above).
- **Rule 16** is the implementation of the "Client" column of the
  decision tree — how each platform realizes the neutral semantics it
  receives.

## 19. Minimize Variant Proliferation Within Semantic Vocabularies

Once a client-realized vocabulary exists under Rule 18 (`TextVariant`,
`ContainerVariant`, `ImageVariant`, `ButtonVariant`, icon tokens,
`ActionTrigger`, ...), new **values** in that vocabulary
are not free. Each value expands the wire-level contract, requires
per-platform resolver maintenance, and becomes operationally expensive
to remove once apps ship — strict decoders (Rule 13) and the
app-store long tail make removal a deprecation exercise, not a code
change.

The default posture is therefore: **prefer expressible over named**.
A design requirement that can be met by existing inline style
properties on the primitive — or by extending the inline-property
vocabulary with one additional orthogonal prop — is preferable to a
new variant value, because inline properties are:

- **Orthogonal.** `padding` and `cornerRadius` combine cleanly; neither
  needs to know about the other. Variants, by contrast, create a
  taxonomy that must be audited for overlap.
- **Composable without combinatorial explosion.** N inline properties
  express many combinations; N variants express N concepts.
- **Server-tunable without a client release.** Bumping a padding value
  is a composer change; renaming a variant is a client release on
  every platform plus a deprecation window.

### Before adding a new variant value

1. **Expressibility check.** Can the visual treatment be produced by
   the current inline-property set on the primitive? If yes → use
   inline props. Do not add a variant value.
2. **Abstraction check.** If not currently expressible, could **one
   additional orthogonal inline property** — usable across many
   primitives, not specific to the one case in front of you — close
   the gap? If yes → add the inline property; prefer it to a variant
   value. Example: a schema-level `elevation` number is orthogonal;
   a `ContainerVariant.elevatedSomething` is not.
3. **Inexpressibility check.** Only when (1) and (2) both fail —
   because the treatment requires a platform SDK, an OS-version-
   specific API, runtime theme resolution, multi-layer compositing,
   or an interaction state that cannot be serialized as JSON
   primitives — is a new variant value a candidate.
4. **Evidence bar.** Whatever survives (3) must still clear the
   ≥2-site evidence bar documented in the governing ADR (e.g.
   ADR-013 for style tokens). Aggregation alone (DRY-ing a repeated
   inline-prop bag) is a weak justification; inexpressibility is the
   strong one.

### Removing weakly-justified values

Values proposed speculatively, or that fail the expressibility check
in hindsight, **must be removed before they ship**, not retained
"just in case." Schema values are asymmetrically expensive: trivial
to add on day one, operationally costly to remove after apps ship.
That asymmetry is intentional — it pushes the bar to "do we need
this now," not "might we need this later." A single-value enum is
usually a signal to drop the enum entirely until a second evidenced
value emerges.

### Rationale

This rule protects two invariants the architecture depends on:

- **Composition stays the primary expressiveness mechanism.** Rule 15
  (AtomicComposite as default) and Rule 18 (server-driven by default)
  both depend on a small, orthogonal primitive vocabulary being enough
  to compose most designs server-side. If the primitive's style
  vocabulary sprawls, the combinatorial base gets fuzzy and
  composition stops being the cheap path.
- **Client release cadence stays low.** Every new variant value is a
  client-release coupling point — Rule 18's whole thesis. Keeping the
  vocabulary tight keeps the coupling surface tight.

### Examples

- **Good.** "Designers want a 16px horizontal gutter around cards" →
  extend the composer to emit `padding.horizontal = 16`. No new
  variant. (This is why `ContainerVariant.inset` does not exist.)
- **Good.** "Designers want grouped-list chrome with row-aligned
  dividers" → new `ContainerVariant.grouped`. The SwiftUI
  `.insetGrouped` / Material `ListItem` grouping behaviour is
  inexpressible as inline bg + radius + padding, has ≥2 ref-app
  sites, and is a platform idiom each client realizes natively.
- **Bad.** "Designers want an `emphasized` divider that is 1pt instead
  of 0.5pt" → that is inline `thickness`, not a variant. Do not add.
- **Bad.** "Designers want an `avatar` image variant with a circle
  clip" → expressible via `cornerRadius` (or a future `shape` prop) +
  `objectFit`. Do not add unless a platform-native avatar treatment
  (dynamic content-aware framing, platform placeholder glyphs)
  cannot be captured inline.

### Relationship to other rules

- **Rule 13** is why removal is hard and therefore why the bar is
  high: strict decoders reject unknown values, so once a value ships
  it is part of the forever-contract.
- **Rule 15** is the same minimization stance at a different layer —
  composition over new section types.
- **Rule 16** governs how each shipped value is realized; this rule
  governs whether a value gets shipped in the first place.
- **Rule 18** establishes the named-exception model for client-realized
  vocabularies; this rule keeps those vocabularies honest after they
  exist.
