# SDUI Prototype — Development Rules

These rules govern all SDUI work in this repository. Their purpose is to keep
the server authoritative over UI semantics so layout, content, assets,
navigation, refresh policy, and data flow can change without requiring client
releases.

> **Building a new client?** See
> [client-implementors-contract.md](client-implementors-contract.md) for the
> platform-agnostic architecture blueprint and build checklist. This file states
> the constraints; the contract explains how to implement them.

> **Agent file-access conventions:**
> `docs/appendix-kitchen-sink.md` is owner-maintained and off-limits to agents.
> Do not read, grep, edit, or propose changes to that file. If related work
> would require appendix updates, leave a note that the owner must sweep it
> manually.

> **Build / test commands:**
> Prefer repo-root `Makefile` targets over raw `xcodebuild`, `gradlew`, `npm`,
> or `swift build` invocations. If a required target is missing, add it to the
> `Makefile` rather than teaching the agent a bespoke incantation.

## 0. Priority Order

When rules feel in tension, resolve them in this order:

1. **Server authority over semantics**
2. **Schema as the wire contract**
3. **Shared client infrastructure owns shared concerns**
4. **Renderers are presentation-only**
5. **Client exceptions are narrow and explicitly justified**

The lower rule never weakens the higher one.

## 1. Constitutional Rules

### 1.1 Server authority is the default

- The server owns content, routing, assets, refresh semantics, action semantics,
  and data flow by default.
- If a decision does not cleanly fit a named client exception, it belongs on the
  server.
- Clients may realize server-declared semantics; they may not substitute their
  own semantics.

### 1.2 Schema is the contract

- `schema/sdui-schema.json` is the wire-level contract between server and
  clients.
- Every emitted field, type, and enum value must exist in the schema first.
- When adding a new enum value, follow this sequence: (1) add to the schema,
  (2) run `make codegen`, (3) update each platform's renderer or add a
  fallback so the new value renders sensibly, (4) only then update the server
  to emit the value. This prevents a window where the server emits values that
  clients cannot decode.
- After schema changes, always run `make codegen` (see repo `Makefile` /
  `codegen/generate.sh`).
- **Checked-in generated models** must be **produced only** by the codegen
  pipeline, not by ad-hoc PR edits. Outputs live at:
  - Java: `codegen/build/generated-sources/jsonschema2pojo/`
  - Kotlin: `android/sdui-core/src/main/java/com/nba/sdui/core/models/generated/SduiModels.kt`
  - Swift: `ios/Sources/SduiCore/Models/SduiModels.swift`
  - TypeScript: `web/src/generated/SduiModels.ts`
- **Post-processing is part of that pipeline** — e.g. `codegen/generate.sh`
  runs quicktype (Swift/TS/Kotlin) and **deterministic** `sed` steps so routing
  types stay forward-compatible. New mechanical transforms go **in the script**
  (or schema), not as one-off edits to the model files.
- **Forbidden:** editing the files above to “patch” types or enums without
  changing `schema/sdui-schema.json` and re-running the full generate step.

### 1.3 Strict decoding is intentional

- Clients decode strictly on purpose.
- Unknown server values for **closed wire shapes** (e.g. wrong enum for a
  strict field) are contract violations, not hidden graceful degradation.
- **Open-ended wire strings** (e.g. `token:…` presentational references) must
  decode; **unknown token names** are handled in **resolvers** (log +
  neutral default) — that does not relax JSON decoding.
- Renderer fallbacks happen after successful decode; they do not weaken the
  schema contract.

### 1.4 One owner per concern

Each concern must have one owner:

- URI resolution: shared repository / resolver path
- Live-data mapping: shared data-binding infrastructure
- Action execution: shared action infrastructure
- Outer section chrome: shared `SectionContainer`
- Server content and semantics: server composition

If two code paths own the same concern, treat that as architectural drift.

## 2. Closed Client Exceptions

Client exceptions are closed, not open-ended. Client code is justified only when
it fits one of the categories below.

### 2.1 Allowed exception classes

1. **Client-owned interaction state**
   - Examples: tab selection, form validation, sort / filter state.
2. **Platform SDK hosting**
   - Examples: IAP, ads, video SDKs.
3. **Runtime lifecycle ownership**
   - Examples: SSE subscriptions, connection state, native visibility APIs,
     keyboard / IME integration.
4. **Platform-native realization of neutral semantics**
   - Examples: typography variants, button variants, icon tokens, image
     variants, gesture realization of `onActivate` (and legacy `onTap`) /
     `onLongPress` / `onVisible` / `onSubmit`.

Clarifying boundary:

- **Runtime lifecycle ownership** means the client owns timing, subscription,
  local observation, connection handling, or SDK/session management that the
  server cannot execute at composition time.
- **Platform-native realization** means the server already chose the meaning and
  the client is choosing only how that meaning is expressed with native
  platform idioms.

If a change does not fit one of those classes, it is server-driven by default.

### 2.2 Exception ownership does not expand sideways

Owning one part of a surface does not grant ownership of adjacent concerns.

- A section renderer that owns runtime state does not also own card chrome.
- A token resolver that owns presentation does not also own content policy.
- A permanent section that owns SDK hosting does not also own fallback copy,
  dimensions, or assets the server could have sent.

### 2.3 Forbidden false exceptions

The following are **not** valid client exceptions:

- Inventing missing content, labels, or identifiers
- Inventing backup URIs, endpoints, or bootstrap destinations
- Inventing image URLs, or unapproved branded client assets that look like
  real server-pushed content (see §3.2 for the narrow **approved** static
  last-resort case)
- Downgrading a richer schema contract into a simpler local behavior
- Adding per-renderer outer chrome where shared infrastructure already owns it
- Recreating section-specific business logic that could have been expressed in
  composition, schema, or shared action infrastructure

## 3. Server Authority In Practice

### 3.1 No hardcoded paths, routes, or screen IDs

- Clients must not hardcode SDUI endpoints such as `/sdui/scoreboard` or
  `/sdui/game-detail/...`.
- Navigation URIs come from the server.
- URI resolution is a simple prefix swap from `nba://` to `/sdui/...` with no
  screen-specific branching.
- Client-side screen enums are prohibited.

### 3.2 Images are server-provided

- Every image URL must originate from the server payload.
- Clients must not construct CDN URLs from local patterns.
- **Default rule:** last-resort renderability fallbacks (load failure, wire
  `placeholder` failure) should be **generic** — visually distinguishable from
  real content (e.g. neutral block, empty frame), not branded art that could be
  mistaken for a server asset.
- **Narrow exception:** a **client-bundled**, **non-wire** “cannot render
  image” tile may use **product-approved** static art (documented in code, same
  on every build) when product explicitly requires brand continuity for broken
  images. It must not substitute for URL-backed images the server *could* have
  sent; it is for **renderability only** when all URLs failed.

### 3.3 Refresh, polling, and live updates are server-driven

- `refreshPolicy` tells the client when and how to refresh.
- Clients must not decide which screens poll, which sections subscribe, or what
  interval to use based on screen identity.
- Real-time payloads are opaque JSON and are applied only through shared
  data-binding infrastructure.

### 3.4 Platform identity travels in the request envelope

- Every client identifies its platform through the request envelope, not
  through a dedicated header. `RequestEnvelopeBuilder` emits `platform[name]`
  (and `platform.deviceClass`) as a bracket-notation query parameter on GET
  requests and as `platform.name` inside the JSON body on POST. See §4.1.1.
- Allowed `platform[name]` values are `android`, `ios`, `web`.
- Server composition reads the platform via `SduiRequestContext` /
  `BracketParamResolver`. Composers must never assume a platform via
  `defaultValue`; missing platforms must compose safely (e.g. form layout
  falls back to vertical).
- Do not set `X-Platform` on outbound requests.
- Platform-specific composition decisions are resolved from the envelope, not
  from hardcoded strings.
- **`formFactor` (roadmap):** the wire will carry a client-declared form factor
  in the same envelope (see implementation plan). Until that is required end to
  end, clients may use a **documented default** (e.g. `phone`) for
  form-factor-scoped **presentational** resolution (e.g. layout tokens) —
  **temporary;** do not use it to override server content choices.

### 3.5 Action semantics are server-declared

- Action meaning comes from the schema and payload, not client inference.
- Shared action infrastructure must preserve schema-declared semantics through
  execution.
- If the schema exposes a semantic field, either the client carries it to the
  execution boundary or the gap is tracked as a known limitation with a
  reference in the code and a resolution target.
- Shared action infrastructure must not silently drop, collapse, downgrade, or
  reinterpret richer schema-declared semantics into simpler local behavior.
- If a platform cannot yet honor a declared action field, that limitation must
  be explicit and temporary rather than silently normalized into a weaker
  runtime contract.

## 4. Shared Infrastructure Owns Shared Concerns

### 4.1 One fetch path

- `SduiRepository` exposes one generic screen-fetch method and `fetchRawJson()`
  for direct polling URLs.
- Dedicated repository methods for specific screens are prohibited.
- **Every** composition request — initial loads, navigation transitions,
  pull-to-refresh, action-driven `refresh` (including parameterized refresh
  with `paramBindings`), and any future variant — routes through that single
  fetch primitive. Hand-rolled URL strings, bespoke `fetch`/`URLRequest`
  calls, or per-action transports for composition responses are prohibited.

### 4.1.1 Request envelope is the transport contract

Every SDUI composition request shares one transport shape, owned by the
shared envelope builder (`RequestEnvelopeBuilder` on each platform) and the
single fetch primitive in `SduiRepository`. This contract is non-negotiable
because it is what makes GET requests cacheable on the CDN, makes POST
requests interchangeable with GET requests on the server, and makes
`X-Trace-Id` correlation across screens, sections, and refreshes possible
at all.

The contract:

- **Bracket-notation query params for the envelope.** `platform`, `device`,
  `experiments`, `locale`, `schemaVersion`, and `gameState` are serialized
  as `platform[name]=ios`, `device[countryCode]=US`,
  `experiments[exp_id]=variant_b`, etc. Same bytes on every platform.
- **GET-first, POST fallback at 8192 chars.** When the envelope query
  exceeds the threshold, the *same envelope shape* moves to a JSON body
  on the same path. The server reads it through one resolver
  (`BracketParamResolver` → `SduiRequestContext`).
- **User-supplied filter params ride the URL query string regardless of
  method.** Form submits, refresh `paramBindings`, and any other
  caller-provided values participate in the GET/POST length decision but
  are always on the URL so the server reads them through `@RequestParam`
  on either side.
- **RFC-3986 percent-encoding for both halves of the query.** The user
  params and the envelope params encode through the same rule; handwritten
  string concatenation (`"$key=$value"`) is prohibited because it silently
  corrupts spaces, ampersands, and non-ASCII bytes.
- **Deterministic key ordering.** User params are sorted by key; envelope
  ordering is fixed by the builder. Identical inputs produce byte-identical
  URLs across platforms and across runs — the CDN cache key depends on it.
- **`X-Trace-Id` propagates from the parent fetch.** A parameterized
  refresh inherits its screen's trace ID so server logs correlate the
  refresh response with the screen that triggered it.

Every server route that accepts an SDUI envelope must be dual-mounted as
`@GetMapping` *and* `@PostMapping` to the same handler. A GET-only or
POST-only composition endpoint is a contract bug.

The systemic rule that follows from this contract:

> If a feature needs to fetch composition data, the only correct answer is
> to call the shared fetch primitive with an envelope and (optionally)
> user params. Anything else — building URL strings, calling `fetch` /
> `URLRequest` / `OkHttp` directly, prepending base URLs by hand — silently
> opts out of the contract and is prohibited.

### 4.2 Section wrappers own outer chrome

- Permanent sections are wrapped in a shared `SectionContainer` that applies
  `section.surface`.
- Permanent section renderers must not set their own outer padding, margin,
  corner radius, shadow, border, or background.
- If `section.surface` is omitted, clients render flush unless the server emits
  a default surface.
- If legacy section envelope styling fields still exist as compatibility paths,
  they must be treated as compatibility-only and must not become a second
  long-term ownership path for outer chrome. Legacy compatibility paths should
  have a documented removal or migration target. Indefinite dual-path
  maintenance is itself a form of drift.

### 4.3 Shared action helpers own interaction execution

- Reuse shared action helpers for `onTap`, `onVisible`, `onSubmit`, and similar
  triggers.
- Do not parse or execute action semantics ad hoc inside individual renderers.

### 4.4 Shared binding infrastructure owns live-data mapping

- Ably / real-time messages remain opaque payloads.
- `dataBinding` is the only mapping mechanism from incoming payloads to section
  data.
- No duplicate per-section mapping logic outside the shared binding path.

## 5. Renderers Are Presentation-Only

- Renderers map server data to native views.
- They may own layout, local view state justified by an allowed exception, and
  platform-native presentation.
- They may not add business logic, routing rules, content selection, semantic
  fallbacks, or screen-specific behavior that the server could have expressed.

Before editing a renderer, ask:

1. Can the server compose this instead?
2. Can the schema or action payload express this instead?
3. Can shared infrastructure own this instead?
4. Is this truly justified by a named client exception?

If the answer to 1, 2, or 3 is yes, do not add section-specific renderer logic.

## 6. Permanent Sections Are Exceptions, Not Defaults

- The default is server-composed `AtomicComposite`.
- A dedicated client section renderer is justified only for client-owned state,
  platform SDK hosting, or runtime lifecycle ownership the server cannot own at
  composition time.
- Reserved SDK sections render only server-emitted content until their SDK lands.
- SDK reservation rectangles come from the payload; clients do not invent
  fallback dimensions.

### 6.1 Decision test for adding or keeping a permanent section

Ask the questions in this order:

1. Is the UI stateless, ≤80 LOC, and free of platform SDK dependencies — i.e.
   expressible as a server-composed atomic tree?
2. If not, does the section require client-owned interaction state that cannot
   be serialized and round-tripped cleanly through the server?
3. If not, does it need to host a platform SDK with a stable client-owned mount
   point?
4. If not, does it manage a runtime lifecycle the server cannot own at
   composition time, such as subscriptions, local visibility observation, or
   keyboard / IME behavior?

If the answer to all four is no, it should not be a permanent section.

### 6.2 Reserved SDK sections remain server-authored until the SDK lands

- A reserved SDK section exists because a future SDK will need a stable client
  mount point, not because the client is allowed to style the surface early.
- Until the SDK lands, the renderer behaves like a thin host for server-emitted
  content.
- No client-owned outer chrome, fallback copy, padding, gradients, radii,
  shadows, or placeholder assets are justified merely because the section is
  reserved.

Owning the future mount point does not authorize owning the present-day UI.

### 6.3 Permanent-section surface ownership stays shared

- Permanent sections still read their outer wrapper from `section.surface`.
- The shared `SectionContainer` owns that wrapper uniformly across every
  permanent section.
- A permanent section may own only its inner content and the client-side
  behavior that justified its existence.

If a permanent renderer starts setting its own outer chrome, the section has
crossed its exception boundary.

### 6.4 Reservation rectangles and fixed dimensions come from the payload

- Where an SDK will claim a fixed-pixel rectangle, the payload is the single
  source of truth for that reservation.
- The pre-SDK placeholder and the eventual SDK read the same server-emitted
  dimensions.
- A missing reservation rectangle is a contract problem, not an invitation for
  the client to invent a default size.

### 6.5 Permanent-section inventories are expected to evolve

- The exact list of permanent sections may change over time.
- That inventory is not the doctrine; it is the current application of the
  doctrine.
- When product or runtime realities change, update the registry in `AGENTS.md`
  rather than stretching the exception model informally in code.

### 6.6 Current permanent-section inventory

The current set of justified permanent sections is:

- `BoxscoreTable` — real-time binding plus expandable interaction state
- `SeasonLeadersTable` — sort / filter interaction state
- `TabGroup` — tab selection state and nested section hosting
- `Form` — validation state and platform keyboard integration
- `SubscribeHero` — reserved IAP SDK mount point
- `SubscribeBanner` — reserved IAP SDK mount point
- `AdSlot` — reserved ad SDK mount point with payload-owned reservation sizes
- `VideoPlayer` — reserved video SDK mount point

This list is descriptive of the current architecture, not immutable doctrine.
The governing question is still whether each section satisfies the exception
tests above.

## 7. Client-Realized Semantics Are Narrow

- Neutral semantic tokens may be realized platform-natively.
- This applies to visual tokens and interaction intents.
- Cross-platform visual or gesture differences are acceptable when they preserve
  the same server-declared meaning.
- Client-realized vocabularies must stay small, stable, and purely presentational.

When in doubt: if the client is choosing **what** is shown or **whether** it is
shown, it is not a token-realization problem and belongs on the server.

### 7.1 What counts as a legitimate client-realized semantic

A semantic may be client-realized only when all of the following hold:

1. The server has already chosen the meaning.
2. The remaining client work is purely how that meaning is expressed on the
   platform.
3. The realization space is owned by the platform or runtime rather than by the
   product domain.
4. The semantic can vary by platform without changing business meaning.

Examples:

- Typography and button variants
- Icon token lookup
- Container and image presentation variants
- Gesture realization of `onActivate` and legacy `onTap`, `onLongPress`,
  `onVisible`, `onSubmit`, and similar interaction intents

Counterexamples:

- Deciding whether a CTA appears
- Choosing which refresh mechanism a section should use
- Deciding whether a screen should navigate externally or internally
- Selecting which asset URL or content payload should be shown

### 7.2 Interaction intents are part of the same exception model

- Interaction triggers are server-declared semantics.
- Each platform realizes them with its native event system.
- Cross-platform implementation details may differ, but the semantic contract
  must stay equivalent: the same server-declared intent must trigger the same
  category of client behavior (dispatch the same action type, honor the same
  dedup policy, respect the same failure semantics) even if the platform
  mechanism differs.

This means a platform may differ in mechanism while still being wrong if it
drops or downgrades the semantic.

### 7.3 Criteria for adding a new client-realized vocabulary

Before adding a new client-realized vocabulary, require all of the following:

1. **Pure presentation or interaction realization**
   - It describes how something is rendered or realized, not what is chosen.
2. **Platform-owned realization space**
   - The valid realizations are dictated by the client platform, OS, or native
     runtime APIs.
3. **Stable neutral meaning**
   - The semantic changes more slowly than its platform-specific realization.
4. **Documented renderer fallback**
   - After **successful** decode, older clients can apply a neutral default in
     resolvers (e.g. unknown **presentational** token name in a `token:…`
     string). **Failed JSON decode** (e.g. wrong type, invalid enum for a closed
     field) remains a contract violation — not a resolver fallback.
5. **Clear server/client split**
   - The server still chooses the meaning; the client is not choosing business
     policy.

If that bar is not met, keep the decision server-driven.

### 7.4 Why this exception exists at all

The point of client-realized semantics is to avoid putting the server on the
upgrade treadmill for platform trivia.

- Good use: let the client map a stable semantic token to evolving platform
  primitives.
- Bad use: push product, policy, or content decisions into the client and call
  them presentation.

The exception exists to reduce coupling, not to justify more client ownership.

### 7.5 Scaling rationale

This exception should reduce total coupling, not move server work into the
client under a nicer name.

- A stable shared semantic vocabulary plus one resolver per platform scales more
  cleanly than server-side per-platform branching for every realization detail.
- That benefit applies only to platform-owned presentation or interaction
  realization.
- For content, policy, capability gating, routing, assets, and delivery
  semantics, server-side branching is still the right cost to pay because the
  server owns those decisions anyway.

If the new vocabulary would make the client decide business meaning rather than
native realization, the scaling argument does not apply.

## 8. Fallback Doctrine

### 8.0 Error states are first-class sections

- `ErrorState` is a server-composed `AtomicComposite` section.
- When an error occurs (bad game ID, network failure, missing data), the server
  or client should produce an `ErrorState` section rather than showing a blank
  screen or falling back to a hardcoded default.
- Clients must never fall back to a hardcoded identifier (e.g. a default
  gameId) — they surface an `ErrorState` instead.

### 8.1 Allowed fallback

Allowed fallbacks preserve renderability without inventing meaning.

Examples:

- Render nothing for an omitted optional field
- Render flush when no `surface` is present
- Use a neutral platform default for an unknown **presentational** token
  (string decoded successfully; name unknown to resolver)
- Surface an `ErrorState` section instead of inventing replacement content

### 8.2 Forbidden fallback

Forbidden fallbacks invent semantics the server did not send.

Examples:

- Fake team identifiers, labels, or status text
- Client-owned bootstrap URIs or backup routes
- Hardcoded **URLs** to branded art that pretend to be server-pushed content
  (see §3.2 for the narrow **approved** bundled last-resort tile when all URLs
  failed)
- Card chrome, copy, or dimensions invented by a renderer that could have been
  server-emitted

Use this distinction during review:

- If the fallback preserves rendering, it may be acceptable.
- If the fallback changes meaning, it is architectural drift.

## 9. Review Questions For Detecting Erosion

Use these questions during code authoring and review:

1. What semantic is the server declaring here?
2. Which single layer is allowed to own this concern?
3. Does this client code realize server intent, or substitute client intent?
4. Is this fallback preserving renderability, or inventing meaning?
5. If this is client-owned, which exception class justifies it?
6. If the schema already exposes a field for this, is the client preserving it?
7. Would this change require a client release for something the server should
   have been able to change alone?

If any answer points to client substitution rather than realization, the design
has drifted.

## 10. Operational Rules

### 10.1 Error handling

- Never swallow exceptions silently.
- Catch blocks that return `null`, a fallback, or an `ErrorState` must log enough
  context to diagnose the failure.

### 10.2 Unknown types

- Section routers must ignore unknown section types with a debug or warning log.
- Unknown section types are a forward-compatibility case at render time, not a
  reason to hard-fail the screen.

### 10.3 Comments

- Comments describe code behavior and invariants.
- Business, legal, partner, or upstream-data constraints must be cited directly
  in the code they justify.

**Do not reference in code:**

- Rule numbers from this document (e.g. "per Rule 10", "Rule 4")
- Links or citations to `AGENTS.md`, agent personas, or skills files
- ADR numbers used as prose justification ("per ADR-005 this must…")
- Internal engineering norms phrased as rule citations

**Do cite in code:**

- Contractual constraints (e.g. broadcaster rights, blackout policies)
- Brand guidelines with section references
- Legal / compliance requirements (COPPA, GDPR)
- Partner / sponsor constraints with expiration dates
- Upstream data quirks with ticket references
- Product-decision intent the code itself cannot convey

## 11. Variant Discipline

- Prefer expressible over named.
- Use inline props or one additional orthogonal prop before adding new variant
  values.
- A new client-realized vocabulary value is justified only when the behavior is
  not expressible as server-composed data and is truly platform-owned.
- Speculative values that fail that test should be removed before shipping.
- Every new variant value expands the wire-level forever-contract and multiplies
  renderer maintenance obligations across all clients.

### 11.1 Decision framework for new variant values

Before adding a new variant value, ask:

1. Can the desired treatment already be expressed through existing inline
   properties?
2. If not, would one additional orthogonal property solve the problem more
   cleanly than a named variant?
3. If not, is the treatment genuinely inexpressible without a platform-owned
   semantic token?
4. If yes, is there evidence that the new value represents a stable reusable
   semantic rather than a one-off design request?

Only after passing those steps should a new variant value be considered.

### 11.2 Prefer orthogonal composition over taxonomy growth

- Inline properties compose; named variants proliferate.
- Inline properties are easier for the server to tune without a client release.
- Variant values enlarge the wire contract and create long-lived maintenance
  obligations across all clients.

The default posture is therefore: if composition can express it, do not name it.

### 11.3 Strong vs weak justifications

Strong justification for a variant value:

- The behavior is not expressible as inline data
- The realization is platform-owned
- The semantic is reused across multiple surfaces

Weak justification for a variant value:

- It removes repeated inline prop bags
- It feels cleaner in one renderer
- It is speculative and may become useful later

Weak justifications are not enough for contract growth.

### 11.4 Removing weakly justified values

- Variant values are cheap to add early and expensive to remove after clients
  ship.
- If a value was introduced speculatively or fails the expressibility test in
  hindsight, remove it before it hardens into the forever-contract.
- A single-value enum is often a sign that the semantic should not yet exist.

### 11.5 Relationship to the exception model

- Variant discipline protects the boundary around client-realized semantics.
- It keeps the set of named client exceptions small enough that composition
  remains the primary expression tool.
- If the variant space sprawls, server authority becomes fuzzier and more design
  work migrates into client code by accident.

## 12. Summary Doctrine

This architecture succeeds only if the client remains a narrow, disciplined
runtime for server-owned semantics.

- The server chooses meaning.
- The schema defines the vocabulary.
- Shared infrastructure owns shared mechanics.
- Renderers realize presentation.
- Exceptions are narrow.
- Fallbacks preserve rendering, not meaning.

When in doubt: move authority toward the server, not toward the client.