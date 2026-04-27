SDUI: unify refresh transport, add onActivate, harden images, add production patterns

Unify parameterized refresh with the same envelope + query encoding as other
SDUI fetches. Introduce `onActivate` as the primary action trigger (retain
`onTap` as a deprecated alias) across schema, server, and clients. Bundle a
last-resort image for failed loads on iOS, add same-origin demo assets, and
record production server architecture patterns and the design-system
implementation plan. Refresh governing docs; remove
`COMMIT_MESSAGE_VIOLATIONS.md`.

--- Phase 1: onActivate (schema + all clients) ---

- `ActionTrigger` adds `onActivate`; `onTap` kept with schema description
  (deprecated alias).
- iOS: `isPrimaryActivation`; `SectionInteractions` default `onActivate` with
  `onTap` fallback; `deprecated_trigger_used` via `os.Logger` when `onTap` is
  dispatched.
- Android: enum + `SectionInteractions` / `ActionHandler` same semantics;
  debug log for legacy `onTap`.
- Web: `sectionActions` + `ActionHandler` + `BoxscoreTable` align; dev-only
  deprecation log for `onTap`.
- Server composers emit `onActivate`; `make lint-sdui-warn` /
  `scripts/warn-onTap-in-composers.sh` warn if `put("trigger", "onTap")` returns.
- Schema examples + iOS fixtures: triggers migrated to `onActivate`.

--- Parameterized refresh transport (server + all clients) ---

- Dual-mount `GET` and `POST` on `/sdui/refresh/{screenId}`; shared
  `refreshScreen` implementation.
- `stripEnvelopeKeys` on flat query maps so user filter params stay distinct
  from bracket-notation envelope fields (`platform[name]`, `device[...]`, etc.).

--- Android ---

- `SduiRepository.fetchScreen` accepts `userParams` and `traceIdOverride`;
  user params in the query with the envelope; RFC-3986-aligned encoding.
- `RequestEnvelopeBuilder`: shared `percentEncode` (spaces `%20`, not `+`).
- `ActionHandler.handleRefresh` yields structured `ParameterizedRefreshResult`
  for the `ViewModel`.
- `SduiScreenViewModel` routes parameterized refresh through `fetchScreen` and
  merges the response; `GameDetailScreen` updated.
- `SduiRepositoryRefreshTransportTest` (OkHttp) validates refresh wire shape.
- `AtomicText` / `AtomicScrollContainer` / ad slot and other UI touch-ups as
  in branch.

--- iOS ---

- `SduiRepository.fetchScreen` with `userParams` / `traceID`; `buildRequest` merge,
  POST fallback, deterministic encoding.
- `SduiScreenViewModel` parameterized refresh uses `fetchScreen` + trace reuse.
- `TabGroupView`: `scrollToActiveTab` leading vs. trailing anchor so the first
  tab is not cropped.
- `SduiRepositoryRefreshTransportTests` (URLProtocol) for refresh transport.
- `AtomicImageView`: grey loading placeholder; bundled
  `SduiImageLastResortFallback.png` when `src` and wire `placeholder` both fail
  (Swift package `Resources`).
- `Package.swift`: process `Resources` for the fallback asset.

--- Web ---

- `fetchSduiScreen` for initial load and refresh (GET-first, POST fallback,
  `userParams` on query, `X-Trace-Id`); `useSduiScreen` + `ActionHandler` use it.
- `fetchSduiScreen.test.ts` for encoding assertions.
- `AtomicScrollContainer` / `AtomicText` / AdSlot + related updates.

--- Demo assets + server ---

- `DemoImageUrls` and static `/sdui-demo/*` (server `resources/static`, web
  `public/sdui-demo`) for ORB-safe kitchen-sink images.

--- Contract / tests (server) ---

- `SduiRefreshTransportTest` (MockMvc) for `/sdui/refresh/{screenId}`.

--- Docs + plans ---

- `docs/plans/plan-production-server-patterns.md` (new): production server
  architecture — 11 patterns (typed composition model, single-round pipeline,
  SAF bridge, screen+section composers, variant resolution, builder DSL, screen
  state contract, refresh/partial response, aggregation modules with interface
  extraction seams, data binding factory, request envelope cache key strategy).
  Includes deployment topology (collocated modular monolith), capacity model
  (10K RPS baseline, 2s P50 upstream latency, ZGC, stale-while-revalidate),
  seam enforcement (Gradle multi-module + ArchUnit + package visibility), and
  CDN cache key strategy (CompositionContext vs RequestMetadata split, edge
  cohort resolution, 75-95% CDN hit rate projections).
- `docs/plans/sdui-implementation-plan.md` (new): phased roadmap (form factor,
  semantic tokens, a11y, i18n, variant realization × form factor); program
  scope (phone, tablet, web; TV/Roku deferred).
- `docs/sdui-design-system.md`: major rewrite — adds semantic size/spacing
  tokens (§3 Layer 1 revisited), `onActivate` action vocabulary (§4),
  form factor as first-class axis (§5.5), accessibility floor on atomic
  primitives (§6), i18n/RTL contract (§7), per-form-factor variant
  realization (§5.5.1), override matrices, six-registry model (§11),
  worked example (§12), governance rules (§13).
- `AGENTS.md`, `README.md`, `client-implementors-contract`, technical proposal,
  executive summary, requirements summary, glossary, prompts and skills:
  aligned to schema and transport rules; remove `COMMIT_MESSAGE_VIOLATIONS.md`
  (superseded by this file).

--- Makefile ---

- `test` target (server, Android, web, iOS); `lint-sdui-warn` for composer
  `onTap` guardrail.

--- Verification (run before commit) ---

- `make test` (or `server-test`, `android-test`, `web-test`, `ios-test`).
- Spot-check: leaders / stats refresh with form filters; Watch Featured tab
  visible on iOS; tap/activate on demo screens; image failure shows bundled
  fallback (iOS).
