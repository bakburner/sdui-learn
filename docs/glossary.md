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
| **Element** | Any node inside a section's `data.ui` tree. Distinguishes the atomic-tree nodes (`Box`, `Text`, `Image`, …) from the surrounding `Section`. An element has no `id`/`refreshPolicy` of its own. |
| **Atomic primitive** | Leaf-level renderable element type emitted in `data.ui`. The primitive set is `Box`, `Stack`, `Row`, `Column`, `Text`, `Image`, `Button`, `ScrollContainer`, `OverlayContainer`, `ConditionalView`, `DisplayGrid`. Every primitive is server-composable and has a single client renderer per platform. Anything more product-specific belongs inside an `AtomicComposite`, not a new primitive. |
| **AtomicComposite** | Section type whose `data.ui` is a server-composed tree of atomic primitives rather than a fixed-shape payload. The default section type for stateless layout surfaces. Lets the server build new product surfaces without shipping new client renderers. |
| **Permanent section** | A section type with a dedicated client renderer (i.e. *not* an `AtomicComposite`). Allowed only for cases the atomic primitive set cannot express today; the project maintains an explicit inventory of permitted permanent sections. "Permanent" does not mean forever — sections graduate to `AtomicComposite` as the primitive set grows. |
| **Surface** | Design-system tier-adaptive container (e.g. `hero` surface, `canvas` surface). Realized natively per OS tier (Liquid Glass on iOS 26+, blur-fallback below; Material 3 expressive on Android; CSS filter on Web). Distinct from `Section`: a surface is presentational, a section is structural. See `docs/sdui-design-system.md` §5. |
| **Chrome** / **outer chrome** | The wrapper styling owned by `SectionContainer` (margins, dividers, frame, error/loading scaffolding). Renderers do not paint chrome themselves; ownership of this layer is shared infrastructure, not per-section. |
| **Skeleton** | Loading-state placeholder shape declared via `sectionStates.loading.skeleton`. One of `shimmer` / `spinner` / `placeholder` / `none`. |
| **ErrorState** | The section's server-declared error presentation (`message`, `retryAction`, `hideOnError`) under `sectionStates.error`. Named `ErrorState` rather than `Error` so the generated client type doesn't shadow each runtime's native error protocol (e.g. `Swift.Error`). |

---

## Wire contract

| Term | Definition |
| --- | --- |
| **Schema** | `schema/sdui-schema.json`. The single source of truth for the wire contract. All client and server types are generated from it. |
| **Codegen** | The build step that emits Swift, Kotlin, TypeScript, and Java models from the schema. Run via `make codegen`. Generated files are checked in; do not hand-edit. |
| **Envelope** (`RequestEnvelope`) | The structured query/POST body for a screen fetch. Encapsulates the request parameters and decides between GET-with-querystring and POST-with-body based on size threshold. |
| **Endpoint** | A server-relative path that returns an SDUI screen payload. Endpoints are server-owned; clients never hardcode them. |
| **Fixture** | A schema-validated example screen JSON kept under `ios/Tests/.../Fixtures/` and `schema/examples/`. Used for round-trip decoder tests and to seed the demo server. |
| **Trace ID** (`X-Trace-Id`) | Per-request UUID emitted on every outbound SDUI request. Reused inside the client as the active-fetch identity so a newer fetch can dethrone an older one without inventing a parallel ID. |

---

## Action system

| Term | Definition |
| --- | --- |
| **Action** | Server-declared command attached to an interactive or visible element. Always has a `type` and a `trigger` plus type-specific payload fields. Executes through the platform's Dispatcher / Handler. |
| **Trigger** | When an action fires: `onTap`, `onLongPress`, `onVisible`, `onSwipe`. `onVisible` triggers route through Impression policy for dedup; the others fire on every occurrence. |
| **Beacon** | An emitted `fireAndForget` event. Cross-platform synonym for "an analytics ping". Has no on-screen effect, so during local testing it is verifiable only via the Action logger. |
| **Failure policy** | Sequence-control verb on a failed action: `halt` / `continue` / `silent`. When the server omits `onFailure`, per-type defaults apply (navigate → halt; mutate/refresh → continue; fireAndForget/dismiss/toast → silent). See ADR-005. |
| **Failure feedback** | Server-provided error message + presentation hint (`snackbar` / `toast` / `inline`) shown when a halted action surfaces an error. |
| **Param bindings** | Mustache-style template values in a `refresh` action's `paramBindings` map (e.g. `{ "season": "{{form_season}}" }`). Resolved against Screen state at dispatch time and folded into the refresh URL's query string. |

### Action types

| Type | Effect |
| --- | --- |
| **`navigate`** | Change route (push / replace / external / modal / fullscreen). |
| **`fireAndForget`** | Emit a Beacon (analytics event). No UI side effect. |
| **`mutate`** | Apply a `set` / `merge` / `delete` operation to Screen state. |
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
| **Mutate operation** | Verb a `mutate` action applies to Screen state: `set` (replace), `merge` (object-merge into an existing entry), `delete` (remove the key). Defaults to `set` when omitted. |

---

## Lifecycle & live data

| Term | Definition |
| --- | --- |
| **Refresh policy** | Section-level `{ source: poll\|sse, intervalSec, endpoint, … }` block declaring how the section should re-fetch itself. The shared lifecycle infrastructure honours it; renderers do not schedule their own refreshes. |
| **Live data** / **realtime** | Umbrella term for the three push paths the runtime supports: server-sent events (SSE), interval polling, and Ably channels. All three feed the same Data-binding pipeline. |
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

---

## Naming patterns

These prefixes and suffixes carry meaning; reading a class name like
`AtomicScrollContainer` should immediately tell you both layers it
participates in.

| Pattern | Concept | Examples |
| --- | --- | --- |
| **`Atomic*`** | Lives in the atomic-primitive layer; server-composable. | `AtomicComposite`, `AtomicRouter`, `AtomicScrollContainer`, `AtomicOverlayContainer`, `AtomicDisplayGrid`, `AtomicConditionalView` |
| **`Sdui*`** | Project-namespaced runtime type that would otherwise collide with native/runtime symbols. | `SduiAction`, `SduiActionLogger`, `SduiError`, `SduiRepository`, `SduiCore`, `SduiScreenViewModel` |
| **`*Composer`** | Server-side role: builds an SDUI screen response from data + composition rules. | `DemoScreenComposer`, `ForYouComposer`, `ScheduleComposer`, `AtomicCompositeBuilder` |
| **`*Router`** | Client-side dispatch role: maps a `type` field to its renderer. | `SectionRouter`, `AtomicRouter` |
| **`*Container`** | Wrapper that owns chrome / shared concerns around its child. | `SectionContainer`, `AtomicScrollContainer`, `AtomicOverlayContainer` |
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
