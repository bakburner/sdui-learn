# SDUI Glossary

Canonical short definitions for the project's cross-platform vocabulary.
Entries describe **architectural concepts** that mean the same thing on
iOS, Android, and Web.

Doctrine terminology (server authority, client-realized semantics,
fallback rules, variant discipline, etc.) is out of scope here; this
glossary is strictly about runtime, wire, and design-system vocabulary.

---

## Composition & UI granularity

| Term | Definition |
| --- | --- |
| **Section** | Top-level unit of refresh and visibility. Carries `id`, `type`, `data`, `sectionStates`, and (optionally) `refreshPolicy`. The smallest chunk the server can replace independently and the granularity at which live updates, error boundaries, and impression dedup operate. |
| **Element** | Any node inside a section's `data.ui` tree (`Container`, `Text`, `Image`, …), distinct from the enclosing `Section`. No `id`/`refreshPolicy` on the node itself. |
| **Atomic primitive** | `AtomicElement` `type` from the schema enum: `Container`, `Text`, `Image`, `Button`, `Spacer`, `Divider`, `ScrollContainer`, `Conditional`, `DisplayGrid`, `SectionSlot`, `LiveClock`, `OverlayContainer`. Each has one `AtomicRouter` branch per platform. Product-specific layout belongs in `AtomicComposite` trees, not new primitives, unless the schema gains a type. |
| **AtomicComposite section** | Section whose `type` is `"AtomicComposite"` and whose `data.ui` is a server-composed tree of atomic primitives rather than a fixed-shape payload. The default section type for stateless layout surfaces. Lets the server build new product surfaces without shipping new client renderers. |
| **Semantic section** | A section type with a dedicated client renderer (not `AtomicComposite`). The allowed set is the schema `Section` enum minus `AtomicComposite`. Justified by client-owned state, SDK hosting, or runtime lifecycle. |
| **Surface** | Design-system tier-adaptive container (e.g. `hero` surface, `canvas` surface). Realized natively per OS tier (Liquid Glass on iOS 26+, blur-fallback below; Material 3 expressive on Android; CSS filter on Web). Distinct from `Section`: a surface is presentational, a section is structural. See `docs/sdui-design-system.md` §4. |
| **Section frame** / **outer frame** | The wrapper styling owned by `SectionContainer` (margin, padding, background, cornerRadius, shadow, border via `section.surface`, plus error/loading scaffolding). Renderers do not paint the frame themselves; ownership of this layer is shared infrastructure, not per-section. See ADR-015 and `docs/sdui-design-system.md §2` for the cascade. |
| **Skeleton** | Loading-state placeholder shape declared via `sectionStates.loading.skeleton`. One of `shimmer` / `spinner` / `placeholder` / `none`. |
| **ErrorState** | The section's server-declared error presentation (`message`, `retryAction`, `hideOnError`) under `sectionStates.error`. Schema name avoids colliding with host `Error` types in generated code. |

---

## Section surface vocabulary

The named factory methods on `SectionSurfaces`
(`server/src/main/java/com/nba/sdui/service/SectionSurfaces.java`) compose
canonical `Section.surface` shapes. Each factory expresses a tuned
design-system rhythm (margins, padding, background, corner radius, shadow) so
composers pick a named treatment instead of inlining margin/padding constants.
Section outer chrome flows through `SectionContainer` per ADR-015
(`docs/adr/015-section-chrome-single-ownership.md`); see also
`docs/sdui-design-system.md` §2 (Box-model cascade).

All scalar values below are emitted as `token:nba.…` references on the wire
(per AGENTS.md §3.6); clients resolve them per form factor against the
bundled spacing/radius registries. Phone-resolved pixel equivalents are
shown in parentheses.

| Factory | Shape |
| --- | --- |
| **`defaultSurface()`** | Card-chromed: `nba.spacing.lg` margin all sides (phone 16) + raised secondary background + `nba.radius.md` corner radius (phone 12) + soft drop shadow (inline struct: 6px radius, y=2 — between `nba.shadow.sm` and `nba.shadow.md`, no exact token). Default for permanent sections. |
| **`flushSurface()`** | Empty surface block. Renders edge-to-edge with no margin, padding, or background. |
| **`cardSurface()`** | Sunken card chrome: `nba.spacing.lg` margin (phone 16) + tertiary background + `nba.radius.md` corner radius (phone 12), no shadow. For non-rail composites that should sit inside a card. |
| **`railSurface()`** | Vertical margin only (`nba.spacing.lg` top/bottom; phone 16). For flush-edge composites whose root container owns its own inner chrome; the `lg` step pairs with `defaultSurface`'s `lg` to produce 2× `lg` of air at module boundaries. |
| **`sectionHeaderSurface()`** | Header rhythm: `nba.spacing.lg` top (phone 16), `nba.spacing.md` bottom (phone 12). Combined with the rail's `lg` top margin gives a `lg + md` (28pt on phone) header→rail gap so the title reads as belonging to its rail without looking flush. |
| **`adSlotSurface()`** | Ad chrome: sharp corners (radius 0, §3.6 exception #1), no shadow, `nba.spacing.lg` margin all sides, asymmetric inner padding (`lg` vertical, `md` horizontal) for disclosure-label rhythm. |
| **`gamePanelSurface()`** | `defaultSurface()` + diagonal raised→promo gradient background. Token-backed so themes resolve. For matchup cards across all GamePanel sites. |
| **`subscribeSurface(top, bottom, padding)`** | `defaultSurface()` + vertical gradient (caller-supplied colors) + symmetric padding. The `padding` parameter is a raw integer because callers pass values (20, 24) that fall between `nba.spacing.lg` and `nba.spacing.xl`; promote to a new spacing token if callsites converge. For subscription upsell sections (SubscribeBanner, SubscribeHero). |
| **`videoPlayerSurface()`** | Flush dark background (`#1A1F2E`, raw hex — no `nba.bg.player` token in the registry), no margin, no corners. Embedded-player presentation; sizing owned by renderer / `data.displayConfig`. |
| **`secondaryStripSurface()`** | Full-bleed strip: token secondary background + token padding (`sm` top, `md` end, `xs` bottom, `md` start), no corners or margin. |
| **`stripSurfaceWithoutBackground()`** | Same strip padding as `secondaryStripSurface()` but no explicit background — sits on the screen's default surface. |

---

## Wire contract

| Term | Definition |
| --- | --- |
| **Schema** | `schema/sdui-schema.json`. The single source of truth for the wire contract. All client and server types are generated from it. |
| **Codegen** | The build step that emits Swift, Kotlin, TypeScript, and Java models from the schema. Run via `make codegen`. Generated files are checked in; do not hand-edit. |
| **Envelope** (`RequestEnvelope`) | The structured query/POST body for every composition fetch. Query fields: `locale`, `schemaVersion`, `platform[deviceClass]`, `platform[capabilities]`, and `experiments`. Analytics context (`X-Platform`, `X-App-Version`, `X-OS-Version`, `X-Trace-Id`, `X-Request-Id`) travels as headers. Geo context (`X-Resolved-Country`, `X-Resolved-Market-Cohort`) is injected by edge infrastructure. Serialized as bracket-notation query params for GET and as a JSON body of the same shape for POST; flips to POST when the encoded query exceeds 8192 chars. The single transport contract every composition request rides on, owned by `RequestEnvelopeBuilder` per platform. |
| **Fetch primitive** | The one shared screen-fetch entry point on every client (`SduiRepository.fetchScreen` on iOS/Android, `fetchSduiScreen` on web). Owns baseURL resolution, envelope serialization, GET/POST length-fallback, RFC-3986 percent-encoding, deterministic key ordering, and `X-Trace-Id` propagation. Every composition request — initial loads, navigation, pull-to-refresh, action-driven `refresh` — routes through it. |
| **Parameterized refresh** | A `refresh` action whose `paramBindings` resolve Screen-state values into user filter params (e.g. `season=2025-26`, `perMode=Totals`) that travel on the URL query string regardless of HTTP method. Routes through the shared fetch primitive so envelope, encoding, length fallback, and trace-ID inheritance all apply. |
| **Endpoint** | A server-relative path that returns an SDUI screen payload. Endpoints are server-owned; clients never hardcode them. |
| **Fixture** | A schema-validated example screen JSON kept under `ios/Tests/.../Fixtures/` and `schema/examples/`. Used for round-trip decoder tests and to seed the demo server. |
| **Trace ID** (`X-Trace-Id`) | Per-request UUID emitted on every outbound SDUI request. Reused inside the client as the active-fetch identity so a newer fetch can dethrone an older one without inventing a parallel ID. Parameterized refreshes inherit their parent screen's trace ID so server logs correlate refresh responses with the screens that triggered them. |
| **Schema versioning** | Server-side version routing based on the client's declared `schemaVersion` (major.minor). When a client's version is below `currentVersion`, the server strips fields/enums introduced after that version. When below `minSupportedVersion`, the server returns `X-Schema-Version-Mismatch: upgrade-required` header and an ErrorState section. Clients detect the header and display a platform-appropriate upgrade prompt. Config: `sdui.schema.current-version` / `min-supported-version` in `application.yml`. |

---

## Action system

| Term | Definition |
| --- | --- |
| **Action** | Server-declared command attached to an interactive or visible element. Always has a `type` and a `trigger` plus type-specific payload fields. Canonical wire names are `targetUri` / `webUrl` for `navigate`, `target` / `operation` / `value` for `mutate`, and `target` for `refresh`. Executes through the platform's Dispatcher / Handler. |
| **Trigger** | When an action fires: `onActivate` (preferred), `onTap` (deprecated alias for onActivate), `onLongPress`, `onVisible`, `onSwipe`, `onFocus`, `onBlur`, `onSubmit`. `onVisible` routes through Impression policy for dedup. Current hosting: `onActivate`/`onTap` and `onVisible` dispatch at element level on web, Android, and iOS; `onLongPress` dispatches at element level on Android/iOS and is not hosted on web atomics; `onFocus`/`onBlur` dispatch on focusable primitives; `onSubmit` is form-context only; `onSwipe` remains `ScrollContainer`-level rather than a generic atomic trigger. |
| **onActivate** | Preferred activation trigger. Neutral name for tap, click, keyboard Enter, or TV select. Replaces legacy `onTap`. |
| **Beacon** | An emitted `fireAndForget` event. Cross-platform synonym for "an analytics ping". Has no on-screen effect, so during local testing it is verifiable only via the Action logger. |
| **Failure policy** | Sequence-control verb on a failed action: `halt` / `continue` / `silent`. When the server omits `onFailure`, per-type defaults apply (navigate → halt; mutate/refresh → continue; fireAndForget/dismiss/toast → silent). See ADR-005. |
| **Failure feedback** | Server-provided error message + presentation hint (`snackbar` / `toast` / `inline`) shown when a halted action surfaces an error. |
| **Param bindings** | Mustache-style template values in a `refresh` action's `paramBindings` map (e.g. `{ "season": "{{form_season}}" }`). Resolved against Screen state at dispatch time and handed to the shared fetch primitive as user-params; the primitive (not the action handler) is the only thing that builds URL strings. See **Parameterized refresh**. |

### Action types

| Type | Effect |
| --- | --- |
| **`navigate`** | Change route (push / replace / external / modal / fullscreen). |
| **`fireAndForget`** | Emit a Beacon (analytics event). No UI side effect. |
| **`mutate`** | Apply a `set` / `toggle` / `increment` / `append` operation to Screen state. `set` with `value: null` removes the key. |
| **`refresh`** | Re-fetch a section or full screen, optionally with Param bindings. |
| **`dismiss`** | Close the current presented host (modal / sheet / overlay). |
| **`toast`** | Show a transient banner. |

---

## State, data, and binding

| Term | Definition |
| --- | --- |
| **Screen state** | Runtime per-screen key-value map. `mutate` actions read/write it; `paramBindings` resolve against it; refresh responses merge into it. The client-owned counterpart to server-emitted `data`. |
| **Data binding** (`DataBindingPath`) | Declarative mapping from a path in an incoming live-data payload to a path inside a section's `data`. The binding runtime walks these and patches sections with structural sharing so unrelated subtrees keep their object identity. |
| **BindRef** | Dot-path on an atomic primitive (`Text`, `Button`, `Image`, `LiveClock`) that points into the enclosing `AtomicComposite`'s `data.content` object. Renderers resolve the leaf's canonical field from `data.content[bindRef]` at render time and fall back to the inline value when the path is absent (Text → `content`, Button → `label`, Image → `src`, LiveClock → `{snapshotSeconds, snapshotAt, isRunning}`). Lets composers reshape the `ui` tree without breaking real-time updates: live bindings continue to write into `content.*`, and any leaf carrying a `bindRef` to that path picks up the new value automatically. |
| **String table** | Section-scoped translation map (`stringTable: { key → localized }`) emitted by the server. Live-data bindings can deliver string keys instead of resolved strings; the runtime swaps them through the section's table at apply time. |
| **Mutate operation** | Verb a `mutate` action applies to Screen state: `set`, `toggle`, `increment`, or `append`. Defaults to `set` when omitted. `set` with `value: null` removes the key. |

---

## Lifecycle & live data

| Term | Definition |
| --- | --- |
| **Refresh policy** | Section-level `{ type: static | poll | sse, intervalMs?, url?, sectionEndpoint?, channel?, dataPath?, pauseWhenOffScreen? }` block declaring how the section stays fresh. For `poll`, either `url` (raw data overlay via `dataBinding`) or `sectionEndpoint` (SDUI section re-fetch); `sectionEndpoint` takes precedence when both are set. The shared lifecycle infrastructure honours it; renderers do not schedule their own refreshes. |
| **Live data** / **realtime** | Umbrella term for the three push paths the runtime supports: server-sent events (SSE), interval polling, and Ably channels. All three feed the same Data-binding pipeline. |
| **`sectionEndpoint`** | Server-relative SDUI path on `RefreshPolicy` (e.g. `/v1/sdui/section/…`) polled on `intervalMs`; response is a single `Section` that replaces the existing section; client re-evaluates the new section's `refreshPolicy` (enables poll→SSE transition). Distinct from `url` (raw data overlay) and from action-triggered `paramBindings` refresh. |
| **`defaultRefreshPolicy`** | Optional field on `Screen`; when `type: poll`, client runs a screen-level poll loop via `fetchScreen`; mutually exclusive with section-level `sectionEndpoint` polls on the same screen. |
| **Impression policy** | Per-`fireAndForget` dedup config attached to `onVisible` actions. Determines whether a beacon fires on every visibility entry, once per session, once per `dedupKey`, etc. |

---

## Design system

For the long-form treatment of these terms, see `docs/sdui-design-system.md`.

| Term | Definition |
| --- | --- |
| **Style token** | Named layout / spacing / typography primitive registered in `style-tokens.json` (Layer 2 in the design-system three-layer model). Resolved per platform to a native value. |
| **Color token** | Named color reference registered in `color-tokens.json`. Resolved per scheme (light / dark / contrast) and may have OS-tier-specific variants. |
| **Variant** | Predefined visual treatment that maps to a native idiom (`ContainerVariant`, `ButtonVariant`, `TextVariant`, …). The preferred way to express semantic visual intent; new variant values must clear a strict governance bar. |
| **Tier** / **OS-version tiering** | Variants and surfaces realize differently across OS tiers (e.g. Liquid Glass iOS 26+ vs blur fallback below; Material 3 expressive on Android 14+ vs M3 below). Tier is a property of the runtime realization, not the wire payload. |
| **Inline style primitive** | A directly-expressible property (`padding`, `cornerRadius`, `background.color`, …). Layer 1 of the three-layer style model; used to express fine-grained intent that doesn't yet warrant a named variant. |
| **Override matrix** | The precedence stack for resolving styling: `style token < variant < inline override`. Inline overrides win. See `docs/sdui-design-system.md` §4. |
| **Scrim** | Contrast layer painted between an image base and overlaid foreground content (text, badges, CTAs) inside an `OverlayContainer`. Anchored to the `color.overlay.scrim` semantic token; typically realized as the bottom-of-media transparent-to-scrim gradient produced server-side by `AtomicCompositeBuilder.mediaBottomScrimGradient()`. Required (by static-shape contract test) on any `OverlayContainer` whose base is an `Image` and whose overlays carry `Text`, so foreground copy stays legible regardless of the image content. |

---

## Box model & layout

| Term | Definition |
| --- | --- |
| **`SizingMode`** | Enum (`hug` / `fill` / `fixed`) controlling how an atomic element sizes along one axis. `hug` sizes to content (default), `fill` stretches to parent, `fixed` uses the explicit `width`/`height` value. Replaces the deprecated `fillWidth` boolean. |
| **`widthMode` / `heightMode`** | Per-element sizing overrides using `SizingMode`. `widthMode: "fill"` is the canonical successor to `fillWidth: true`. When both are present, `widthMode` wins. |
| **`minWidth` / `maxWidth` / `minHeight` / `maxHeight`** | Constraint fields on atomic elements. Accept `LayoutScalar` (raw int or token string). Let the server express responsive clamping without requiring layout-wrap heuristics. |
| **`layoutWrap`** | Boolean on `Container`. When `true`, enables flex-wrap: children that overflow the main axis wrap to the next line. Realized per-platform (CSS `flex-wrap`, Compose `FlowRow`/`FlowColumn`, SwiftUI `WrappingFlexLayout`). |
| **`crossAxisGap`** | `LayoutScalar` gap between wrapped lines when `layoutWrap` is `true`. Falls back to `gap` when absent. Ignored when `layoutWrap` is `false`. |
| **`alignSelf`** | Per-child `CrossAlignment` override. When set, wins over parent `crossAlignment` for this child. Matches CSS `align-self` and Figma per-child alignment semantics. |
| **`backgrounds`** | Ordered array of `Background` layers on an atomic element. Index 0 is the bottommost layer (Figma convention); higher indices paint on top. Supersedes the singular `background` field (which is now deprecated). |
| **`shadows`** | Ordered array of `Shadow` layers. Index 0 is the outermost; higher indices are closer to the element. Maps to CSS `box-shadow` list. Supersedes the singular `shadow` field. |
| **Inner shadow** | A `Shadow` with `type: "inner"`. Rendered as an inset shadow (CSS `inset`, SwiftUI `.inner`). Platforms without native inner-shadow support fall back to drop with a diagnostic. |

---

## Naming patterns

These prefixes and suffixes carry meaning; reading a class name like
`AtomicScrollContainer` should immediately tell you both layers it
participates in.

| Pattern | Concept | Examples |
| --- | --- | --- |
| **`Atomic*`** | Lives in the atomic-primitive layer; server-composable. | `AtomicComposite`, `AtomicRouter`, `AtomicScrollContainer`, `AtomicOverlayContainer`, `AtomicDisplayGrid`, `AtomicConditional` |
| **`Sdui*`** | Project-namespaced runtime type that would otherwise collide with native/runtime symbols. | `SduiAction`, `SduiActionLogger`, `SduiError`, `SduiRepository`, `SduiCore`, `SduiScreenViewModel` |
| **`*Composer`** | Server-side role: builds an SDUI screen response from data + composition rules. | `DemoScreenComposer`, `ForYouComposer`, `ScheduleComposer`, `AtomicCompositeBuilder` |
| **`*Router`** | Client-side dispatch role: maps a `type` field to its renderer. | `SectionRouter`, `AtomicRouter` |
| **`*Container`** | Wrapper that owns the frame / shared concerns around its child. | `SectionContainer`, `AtomicScrollContainer`, `AtomicOverlayContainer` |
| **`*Boundary`** | Error-isolation wrapper at a granularity (catches and surfaces failures inside its scope). | `SectionErrorBoundary` |
| **`*Dispatcher`** / **`*Handler`** | Action-execution role; one owner per platform. | `ActionDispatcher` (iOS), `ActionHandler` (Android, Web) |
| **`*Tracker`** | Stateful observer of a runtime signal; emits dedup/lifecycle events. | `ImpressionTracker`, `SectionVisibilityTracker` |
| **`*Driver`** | Long-running runtime executor that owns its own scheduling. | `PollingDriver` |
| **`*Coordinator`** | Owns navigation / external dispatch decisions. | `NavCoordinator` |
| **`*State`** | Observable per-scope state container. | `ScreenState` |
| **`*Logger`** | Debug-gated logging facade for a subsystem. | `SduiActionLogger`, `actionLogger.ts` |

---

## See also

| Document | Covers |
| --- | --- |
| `docs/sdui-design-system.md` | Full Surface / Variant / Token model. |
| `docs/SDUI_Technical_Proposal_v2.md` | Architecture overview, dual-layer model, and capability roadmap. |
| `docs/client-implementors-contract.md` | Section / Action / lifecycle obligations on each client. |
| `docs/adr/` | Architectural decisions referenced above. |
