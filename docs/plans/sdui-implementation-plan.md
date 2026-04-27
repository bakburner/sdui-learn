# SDUI Implementation Plan: Form Factor, A11y, Localization, onActivate

## Context for the executing session

You are picking up an SDUI codebase that has shipped a working three-layer
design system: inline primitives, variants, color tokens. The system is
production-quality for phone-only, light-touch use cases. We are extending
it to hold up under the platforms it actually has to ship on. **This
program’s first delivery is iOS and Android (phone + tablet) and web.
TV and Roku are out of scope and deferred** — the wire still lists a
`tv` form factor for future use, but no client or composer work targets
TV/Roku in these phases.

The reference docs have been updated. Read these first, in order:

1. **`sdui-design-system.md`** (replacement document — already drafted) —
   the new contract. Critical sections:
   - §0 — KPI is "% of UI changes that ship via server deploy"
   - §3 Layer 1 revisited — semantic size/spacing tokens replace pixels
   - §4 — `onActivate` replaces `onTap`
   - §5.5 — form factor as first-class axis
   - §6 — accessibility floor on atomic primitives
   - §7 — i18n / RTL contract
2. **`sdui-technical-proposal.md`** — existing implementation doc, mostly
   intact. The atomic vs. typed-section split in §2a–§2b is unchanged.
3. **`schema/sdui-schema.json`** — current wire schema. Several fields are
   being added.

**Program constraints (this repo):** There is no external production
client base yet — **breaking wire changes are acceptable** when they
speed up the contract. Once external apps consume the schema, reinstate
longer deprecation windows as in the general rules below.

- **MVP target:** **phone + tablet** (iOS, Android) and **web** (narrow
  at **768px** vs. wide). **TV and Roku** are out of scope (deferred).
- **Registries** ship **bundled** in clients, not fetched at runtime, for
  this program.
- **Clients** always send **`formFactor`** on every request; production
  must not rely on a missing value.
- **CI:** contract lints (e.g. raw pixels, `onTap` in composers) are
  **warn-only** until explicitly upgraded to `error`.

The KPI everything ladders to: **% of UI changes that ship via server
deploy without a client release.** Every change in this plan exists to
either expand that share or prevent it from collapsing under form-factor
fragmentation. If you find yourself adding complexity that doesn't ladder
to that KPI, stop and re-evaluate.

---

## Hard rules — these do not change

- **Backward compatibility** — *Target end state:* every wire-format change
  goes through a two-release deprecation window; older clients keep
  rendering; older payloads keep decoding. *This prototype until external
  consumers exist:* breaking changes are allowed; see “Program constraints”
  above.
- **Strict decoders, lenient resolvers.** Schema decode is strict. Variant
  and token resolution is lenient (unknown values fall through to defaults
  with a logged diagnostic, never a crash).
- **Renderers are presentation-only.** No parent→child style cascade.
  Compositional reasoning happens server-side in composers, not client-side
  in renderers.
- **Cross-platform pixel parity is not a goal.** Per-platform screenshot
  review only. A diff that says "iOS looks different from Android" is
  *expected*.
- **Section-level decode failure is recoverable.** A bad section is
  skipped and logged. The screen renders the rest.

---

## Phase ordering

The phases are ordered by dependency, not by visibility. Phase 1 unblocks
2 and 3. Phase 2 unblocks 4. Do not parallelize across phases without
reading the dependency notes inline.

Within each phase, work is split into **schema/server work** and **per-platform
client work** (iOS / Android / Web for **phone and tablet**, plus **web**).
Client work can parallelize across platforms once the schema lands.
**TV/Roku are deferred** (no parallel track in this program).

---

## Phase 1: `onActivate` rename (smallest, unblocks others)

**Goal:** Replace `onTap` with `onActivate` throughout the schema and
codebase. `onTap` becomes a deprecated alias.

**Why first:** Trivial diff, unblocks all subsequent phases that touch
the action vocabulary. Touches every renderer but in a mechanical way
that is easy to review.

**Why this matters for the KPI:** Keyboard and accessibility paths need a
neutral activation name, not “tap.” Without `onActivate`, those surfaces
stay easier to force into typed sections. **TV/Roku are deferred**, but
the trigger name stays forward-compatible for a later TV program.

### Schema work

1. Add `onActivate` to the `ActionTrigger` enum in
   `schema/sdui-schema.json`. Keep `onTap`.
2. Document `onTap` as deprecated in the schema's `description` field
   for that enum value: "Deprecated alias for `onActivate`." (In a
   product with external clients, pair with a two-release removal
   policy; **this prototype** may remove `onTap` sooner.)
3. Regenerate codegen for all platforms (`schema/codegen/*`). Both
   values will appear in the generated enums.

### Server work

1. Audit all composers (`composers/*Composer.kt` or equivalent) for
   `onTap` emissions. Update to `onActivate`.
2. Add a CI lint that warns (not fails) when `onTap` is emitted in new
   composer code.
3. Update example payloads in tests and fixtures.

### Client work (per platform)

For each of iOS, Android, web:

1. In the action executor, accept both `onTap` and `onActivate` as
   triggers. Map both to the same handler.
2. Bind the `onActivate` trigger to the platform-appropriate inputs:
   - **iOS**: tap gesture, VoiceOver double-tap, Switch Control activation.
   - **Android**: `Modifier.clickable`, TalkBack double-tap.
   - **Web**: click, Enter, Space keyboard activation.
   - **TV / Roku (deferred program):** D-pad center / select will use the
     same `onActivate` binding when that work starts.
3. Log a `deprecated_trigger_used` diagnostic at debug severity when
   `onTap` is received. Do not log in production builds.
4. Update unit tests and snapshot tests.

### Acceptance

- All composers emit `onActivate`. CI lint warns on `onTap`.
- All clients accept both. No production behavior change.
- (TV/Roku remote activation is deferred; not a gate for this program.)

### Estimate

Schema: 0.5 day. Server: 1 day. Client: 1 day per platform (3 days
total, parallelizable). End-to-end: 2 calendar days with parallel
client work.

---

## Phase 2: Semantic size and spacing tokens (form-factor foundation)

**Goal:** Introduce `size-tokens.json`, `spacing-tokens.json`,
`typography-tokens.json`, `corner-radius-tokens.json`, and
`shadow-tokens.json` registries. Migrate composers off raw pixel
emissions. Implement form-factor-aware token resolution on each client.

**Why second:** Phase 3 (form factor) depends on this. The form-factor
classifier is meaningless if the schema still emits pixels — the client
has nothing to vary.

**Why this matters for the KPI:** Without semantic tokens, the same
atomic payload that works on phone breaks on **tablet and web** (wrong
scale, density, and whitespace) and will later break on TV. Atomic
composition stays phone-only. Estimated KPI
collapse: 60% atomic share on phone → ~20% effective atomic share
across the platform footprint.

### Schema work

1. Define the registry shape in `schema/size-tokens.json`,
   `schema/spacing-tokens.json`, `schema/typography-tokens.json`,
   `schema/corner-radius-tokens.json`, `schema/shadow-tokens.json`.
   Mirror the two-tier (palette + semantic) shape of `color-tokens.json`.
   Each palette entry is a per-form-factor map. Form factors:
   `phone`, `phone.landscape`, `tablet`, `tv`, `web.narrow`, `web.wide`.

2. Define semantic aliases. Initial set, sized to the §3 Layer 1
   primitives in the design system doc:
   - **Spacing**: `xs`, `sm`, `md`, `lg`, `xl`, `xxl`.
   - **Sizes**: `avatar.sm`, `avatar.md`, `avatar.lg`,
     `logo.team.sm`, `logo.team.md`, `logo.team.lg`,
     `icon.sm`, `icon.md`, `icon.lg`,
     `thumbnail.sm`, `thumbnail.md`.
   - **Corner radii**: `radius.sm`, `radius.md`, `radius.lg`,
     `radius.full`.
   - **Shadows**: `shadow.sm`, `shadow.md`, `shadow.lg`.

3. Update the `AtomicElement` schema to accept either a token reference
   string or a raw number for: `padding`, `gap`, `cornerRadius`,
   `width`, `height`, `shadow`. Keep raw numbers as a temporary escape
   hatch with a CI warning.

4. Add a new field `size: { width?, height?, minWidth?, minHeight?,
   maxWidth?, maxHeight? }` that accepts tokens or constraint primitives
   (`fill`, `wrap`, `flex`).

5. Add `aspectRatio` as a string field accepting `"16:9"`, `"4:3"`,
   `"1:1"`, etc. (already in the design system doc; ensure schema
   reflects.)

6. Add `shape: "circle" | "rounded" | "square"` to `Container` and
   `Image`. When `shape: "circle"`, clients compute `radius = width/2`
   automatically.

### Server work

1. Build the `TokenRegistry` Spring component for each new registry,
   following the existing `ColorTokenRegistry` pattern with
   `TokenRegistryConsistencyCheck`.

2. Migrate composers off pixel emissions. Order:
   - `AtomicCompositeBuilder` — the highest-leverage migration. Most
     atomic surfaces flow through this.
   - `ForYouComposer` — current example payload `for-you.json` is
     pixel-heavy.
   - `GameDetailComposer`, `ScheduleComposer`, `LiveComposer`.
   - `DemoScreenComposer` last.

3. Add a CI lint that warns when raw pixel values are emitted from
   composers. Specific properties to lint: `padding`, `gap`,
   `cornerRadius`, `width`, `height`, top-level numeric values in
   `shadow`. Whitelist for genuinely-pixel-driven cases (e.g., 1px
   dividers, hairlines).

4. Update fixtures and tests to use semantic tokens.

### Client work (per platform)

For each of iOS, Android, web (**phone and tablet** in scope for
native; web narrow/wide in scope):

1. **Bundle** the new registries with the client app (same approach as
   color tokens in this program; remote registry fetch is out of scope
   until needed).

2. Implement `SizeTokenResolver`, `SpacingTokenResolver`, etc., each
   mirroring the existing `ColorTokenResolver` pattern. Each resolver:
   - Accepts a token reference string.
   - Selects the form-factor row matching the client's current form
     factor.
   - Returns the resolved value (px, dp, pt as platform-appropriate).
   - Logs `token_resolver_missing` for unknown tokens; falls back to
     a documented neutral default.

3. Update atomic primitive renderers to call resolvers when the value
   is a string token; pass through when it's a raw number.

4. For `shape: "circle"`, automatically compute corner radius from
   resolved width.

### Acceptance

- All registries land with a starter token set covering at least the
  §3 Layer 1 inventory.
- All major composers emit semantic tokens; CI warns on remaining
  pixel emissions.
- Same atomic payload renders correctly on **phone and tablet** simulators
  / emulators and **web** at narrow and wide breakpoints, with no pixel
  changes in the payload (resolution varies by `formFactor`).
- **TV: deferred** — not a gate for Phase 2 acceptance.
- Existing color-token tests pass; new size/spacing-token tests pass.

### Open questions for design partners

- **Which form-factor breakpoints define `phone.landscape` vs. `tablet`?**
  **Working assumption:** aspect ratio + minimum dimension. Phones in
  landscape do not become tablets; foldables: folded = `phone`, unfolded =
  `tablet` with refresh on fold. iPad in narrow split may classify as
  `phone.landscape` — confirm with design.
- **Web narrow vs. wide:** **768px** width breakpoint (Tailwind `md`-style).
- **Do we need `web.narrow.landscape`?** Punt for now.
- **TV type scale (when TV ships):** Interim **1.6×** body / **2.0×** headline
  vs. phone until design replaces — **not in this program.**

### Estimate

Schema: 1 day. Server (registries + composers + lint): 5 days.
Client: 3 days per platform (9 days total, parallelizable).
End-to-end: 8 calendar days with parallel client work.

---

## Phase 3: Form-factor classification + composition routing

**Goal:** Each client identifies its form factor and sends it in the
request envelope. The composition service routes to a form-factor-aware
composer. Token resolution uses the form factor.

**Why third:** Depends on Phase 2 tokens. Without those, having a form
factor doesn't help the client — there's nothing form-factor-aware to
resolve.

**Why this matters for the KPI:** Form factor unlocks **tablet and web
wide** (and later TV) without forcing each surface into a typed section.
A `ForYouComposer` that selects different section order for tablet than
for phone from the same composer keeps that surface in the atomic share.
**TV and Roku are out of this program** — the same envelope field will
be reused when TV ships.

### Schema work

1. Extend `RequestEnvelope.platform` with `formFactor: string` (enum:
   `phone`, `phone.landscape`, `tablet`, `tv`, `web.narrow`, `web.wide`).
   **`formFactor` is required.** Clients **always** send it on every
   request. The `tv` value remains in the enum for a future TV/Roku
   program; this program does not ship TV/Roku clients.

2. Document the classification rules in the schema's `description`
   field for `formFactor`.

### Server work

1. Read `formFactor` from the envelope. It must be present. If decode
   or routing receives a missing value (e.g. tests or misconfigured
   client), treat as a bug: **default only in local/dev test helpers,
   not as silent production behavior.** Do **not** log `missing_form_factor`
   in production — production clients are required to send the field;
   missing → fix the client.

2. Build a `FormFactorAwareComposer` interface that lets a composer
   declare which form factors it supports and emit different content
   per form factor. Most composers will be form-factor-agnostic for
   the atomic tree itself (token resolution handles that on the client)
   but will *select different sections* per form factor:
   - **Tablet:** enable multi-column layouts (the wider variants in
     `ScrollContainer`); more columns / hero width as appropriate.
   - **Web.wide:** enable hover and wider layouts via responsive variant
     declarations; **768px** breakpoint for narrow vs. wide.
   - **Future TV (out of scope here):** would drop dense tables, favor
     large-art + linear focus; document for the deferred TV program only.

3. Update `ForYouComposer` first as the reference implementation. Same
   semantic sections; different selection and order per form factor
   (e.g. **tablet** vs **phone** layout density). Do not gate on TV
   behavior in this program.

4. Add a CI lint: composers that conditionally branch on form factor
   must declare the form factors they support in metadata. Composers
   that don't branch are form-factor-agnostic (handled fine by token
   resolution alone).

### Client work (per platform)

For each of iOS, Android, web:

1. Implement `FormFactorClassifier` — takes screen dimensions, density,
   input modality, and platform metadata; returns the form-factor
   string.
   - **iOS**: `UIDevice.current.userInterfaceIdiom`, screen size, trait
     collection. iPad in split-screen narrow may classify as
     `phone.landscape` (TBD with design).
   - **Android**: `WindowMetrics`, smallest screen width buckets (phone
     vs. tablet; **no TV in this program**).
   - **Web**: viewport width breakpoints, pointer-event detection,
     hover-capability detection. Update on resize.

2. **Always** send `formFactor` in the request envelope (required on
   every call from production and integration-test clients).

3. Pass the form factor to all token resolvers (Phase 2 dependency).

4. On significant form-factor changes (foldable unfold, web resize
   crossing breakpoint), refresh the screen. Existing pull-to-refresh
   path is sufficient.

### TV / Roku (deferred program)

**Not in this plan.** A future program should cover: D-pad focus and
document order for all interactive atomics, overscan-safe root insets
(~5% TV), D-pad center → `onActivate`, 10-foot type via
form-factor-resolved tokens, and (if needed) a **Roku**-specific
form factor vs. generic `tv` — Roku punted with TV.

### Acceptance

- **Every** request from iOS, Android, and web includes **`formFactor`**
  (no silent production default).
- Server `ForYouComposer` (or the chosen reference) produces **visibly
  different** section selection and/or order for **tablet** vs. **phone**
  when mocked via envelope on staging. **Web** `web.narrow` vs. `web.wide`
  (768px breakpoint) shows distinct layout/behavior.
- **Tablet** clients render with tablet-resolved spacing/sizes from the
  same payload phone receives; token resolution, not a second tree, is
  the first lever.
- **No TV/Roku** acceptance items in this program.

**Resolved (working assumptions; confirm with design if needed):**
- **Foldables:** folded = `phone`, unfolded = `tablet`, refresh on
  fold.
- **iPad split narrow:** may classify as `phone.landscape` (yes as default
  until design refines).
- **Web:** **768px** for narrow vs. wide.
- **Roku:** deferred with TV; may add `roku` to the form-factor enum later
  if needed.

### Estimate

Schema: 0.5 day. Server: 4 days (FormFactorAwareComposer + ForYouComposer
reference impl + lint). Client iOS/Android/web (phone + tablet + web):
3 days each (9 days, parallelizable). **No TV line item.**
End-to-end: **~9–10** calendar days with parallel work (slightly under
the prior 12 with TV).

---

## Phase 4: Accessibility floor on atomic primitives

**Goal:** Every atomic element accepts an optional `a11y` block. Composers
emit accessible labels for information-bearing atomic content. Each
client renders a11y semantics natively.

**Why fourth:** Independent of Phases 1–3 in principle, but **Phase 3**
delivers **web keyboard** focus semantics that share the same `a11y.role`
data as future TV focus (deferred). Sequencing fourth keeps the schema
changes batched.

**Why this matters for the KPI:** App store reviewers reject inaccessible
apps. The biggest atomic surfaces (For You feed, schedules) carry images
and tap targets — without a11y schema, they fail review and have to be
rebuilt as typed sections to attach platform-specific a11y modifiers.
Estimated KPI impact: prevents collapse of editorial / hero / feed
surfaces from atomic into typed.

### Schema work

1. Add `a11y` block to `AtomicElement`:

```json
"a11y": {
  "type": "object",
  "properties": {
    "label":      { "type": "string" },
    "hint":       { "type": "string" },
    "role":       { "enum": ["button", "link", "image", "header", "text", "none"] },
    "hidden":     { "type": "boolean" },
    "liveRegion": { "enum": ["polite", "assertive"] }
  }
}
```

2. Document inferred defaults per element type in the schema
   description (see design system doc §6).

### Server work

1. Audit composers for atomic elements that need explicit `a11y.label`:
   - `Image` elements that convey information (logos, headshots,
     thumbnails with no adjacent caption).
   - `Container` elements with `actions` (the whole card is tappable).
   - `Text` is usually fine without explicit a11y (label inferred from
     `content`); exceptions for ambiguous content.

2. Add a CI lint: `Image` with `actions` and no `a11y.label` fails.
   `Container` with `actions` and no `a11y.label` fails (unless the
   container holds a child with a label).

3. Update `ForYouComposer`, `GameDetailComposer`, etc. to emit labels.
   Worked example payloads in design system doc §12 are the reference.

### Client work (per platform)

For each of iOS, Android, web:

1. Read `a11y` from atomic elements. Apply per-platform:
   - **iOS**: `accessibilityLabel`, `accessibilityHint`,
     `accessibilityTraits`, `accessibilityElementsHidden`,
     `accessibilityLiveRegion`.
   - **Android**: `Modifier.semantics { contentDescription = …;
     role = …; liveRegion = … }`. Use `clearAndSetSemantics` for
     `hidden: true`.
   - **Web**: `aria-label`, `aria-describedby`, `role`, `aria-hidden`,
     `aria-live`.

2. Implement inferred defaults per the design system doc.

3. Add accessibility snapshot tests:
   - **iOS**: Accessibility Inspector audit on key screens.
   - **Android**: Accessibility Scanner audit on key screens.
   - **Web**: axe-core audit on key screens.

### Acceptance

- All atomic elements accept `a11y`. CI lints on missing labels.
- VoiceOver / TalkBack / NVDA can navigate the For You feed and read
  out information meaningfully.
- Accessibility audits pass on the reference screens (For You, Game
  Detail) at the same threshold as existing typed sections.

### What this phase explicitly does *not* deliver

- Live region announcements for live scores. That's a typed-section
  concern (see design system doc §6 last subsection).
- Custom focus-traversal order for non-linear grids. Atomic
  composition stays document-order only; non-linear is a typed
  section.

### Estimate

Schema: 0.5 day. Server: 2 days (lint + composer audits).
Client: 2 days per platform (6 days, parallelizable).
End-to-end: 4 calendar days with parallel work.

---

## Phase 5: Localization and RTL

**Goal:** Atomic composition supports locale-aware composition and
right-to-left layout without composer authors having to think about it.

**Why fifth:** Independent of other phases technically; bundles cleanly
with Phase 4's schema additions because they share the same
`AtomicElement` extension surface.

**Why this matters for the KPI:** International audiences (Arabic,
Hebrew, Japanese, etc.) can't be served without RTL and locale-aware
composition. Without this, every internationalized surface is forced
into a typed section that bakes in localization manually. With this,
the composer-level locale handling already in place for typed sections
extends cleanly to atomic.

### Schema work

1. Confirm all edge insets in the schema use `start`/`end` (already
   true). Document RTL semantics in the schema description.

2. Add `rtl: "mirror" | "preserve"` to the icon registry (separate
   file, e.g. `schema/icon-registry.json`). Document which icons mirror
   in RTL (back chevron, undo, external-link, etc.) and which don't
   (logos, brand marks).

3. Add `locale` to the request envelope (already exists per ADR-003;
   confirm and document).

### Server work

1. Audit composers for hardcoded English strings in atomic emissions.
   All `Text.content`, `Button.label`, and `a11y.label` values must
   route through the existing locale-resolution pipeline.

2. For data-binding-driven text (live scores, etc.), the existing
   `stringKeys` mechanism (technical proposal §9p) covers the gap.
   Confirm coverage on atomic elements specifically.

3. CI lint: composer code emitting string literals into `Text.content`,
   `Button.label`, or `a11y.label` fails unless the literal is a
   non-translatable identifier (e.g. team tricode, jersey number).

### Client work (per platform)

For each of iOS, Android, web:

1. Verify `start`/`end` insets flip correctly in RTL locales. iOS uses
   `.layoutDirection`; Android uses layoutDirection from locale; web
   uses `dir="rtl"` or CSS logical properties (`padding-inline-start`
   etc.).

2. Verify `direction: "row"` containers reverse child order in RTL.

3. Verify `ScrollContainer direction: "row"` scrolls right-to-left in
   RTL.

4. Implement icon mirroring: when an icon is in the registry with
   `rtl: "mirror"` and the layout direction is RTL, apply a horizontal
   flip transform.

5. Test with at least one RTL locale (Arabic) on each platform.

### Acceptance

- Atomic surfaces render correctly in Arabic with all chrome
  (chevrons, scroll directions, edge insets) flipped.
- All user-facing strings flow through the locale pipeline.
- CI lint catches new string literals in composers.

### What this phase explicitly does *not* deliver

- Vertical Japanese text or other non-LTR/RTL writing modes.
- Pluralization rules — already handled server-side in composers
  (existing pattern).

### Estimate

Schema: 0.5 day. Server: 2 days (audit + lint).
Client: 2 days per platform (6 days, parallelizable).
End-to-end: 4 calendar days with parallel work.

---

## Phase 6: Variant realization extension to form factor

**Goal:** Update `style-tokens.json` and per-platform variant resolvers
to declare per-form-factor realizations of `hero`, `grouped`,
`thumbnail`, `primary`, `secondary`, etc.

**Why sixth:** Depends on Phase 3 (form-factor classifier on the client).
Variant realizations need to know the form factor to pick the right
realization row.

**Why this matters for the KPI:** Without this, `variant: "hero"`
renders correctly on phone but is **under-padded or wrong scale on tablet
and web wide** (and would be wrong on TV when that program ships).
Composers fall back to inline overrides, fragmenting the
variant surface and blocking the path off raw pixels.

### Schema work

1. Extend `style-tokens.json` realization shape from
   `{platform}.{tier}` to `{platform}.{tier}.{formFactor}`. Backward
   compat: a missing `formFactor` row falls back to `phone`.

2. Document the new shape in schema and in design system doc §11.

### Per-platform work (the substantive work of this phase)

For each variant × platform × OS-tier × form-factor combination:

1. Specify the realization (intent statement, not pixel spec).
2. Implement in the platform's variant resolver
   (`ContainerVariantResolver.kt`, `.swift`, `.ts`).
3. Verify dark mode at every tier × form-factor.
4. Per-platform per-form-factor screenshot review.

   Initial scope (do not boil the ocean — these are the high-leverage
   combinations). **This program:** **phone, tablet, web** — **omit `tv`**
   realizations until the TV program. The enum may still include `tv` for
   forward compatibility.

- `ContainerVariant.hero`: **phone, tablet, web.wide** (skip
  `phone.landscape` and `web.narrow` initially — use phone fallback).
  **TV deferred.**
- `ContainerVariant.grouped`: **phone, tablet**. Web: single realization
  across breakpoints initially.
- `ImageVariant.thumbnail`: **phone, tablet**. Web single realization.
  **TV deferred.**
- `ButtonVariant.primary` and `secondary`: **phone, tablet**. Web
  single realization. **TV deferred.**
- `TextVariant.*`: **phone, tablet** typography scales (handled by
  typography-tokens, not full variant realization). **TV deferred.**

### Acceptance

- `hero`, `grouped`, `thumbnail`, `primary`, `secondary` render
  idiomatically on **phone, tablet, and web** for the scoped form factors.
- **Tablet** `hero` has tablet-appropriate insets and type scale.
- **TV** focus halo and 10-foot type — **deferred program.**
- Per-form-factor screenshot review with design (**Julie** as default
  design owner for sign-off unless reassigned).


### Estimate

Schema: 0.5 day. Per-platform per-variant: 1 day each. The full matrix
(5 variants × 3 platforms × form factors) is large; **this program omits
`tv` rows**, so effort is **below** a TV-inclusive rollout. Realistically
**~2–3 weeks** parallel across platforms (mostly mechanical copy of phone
realizations + design tweaks, **Julie** or design delegate for
screenshot sign-off).

---

## Phase 7 (deferred): Diagnostics, lint, and observability hardening

After phases 1–6 land, harden the contract:

1. Implement `section_decode_failed` diagnostic on all platforms (see
   design system doc §10).
2. Surface variant override rates in dashboards (>30% triggers review
   per §13 governance).
3. Track atomic-share KPI: % of UI changes shipping via server deploy
   vs. client release. Build a dashboard. This is the system's primary
   success metric.
4. Add Figma export pipeline for token round-trip (deferred per
   technical proposal §2c).

This phase doesn't unblock anything; it makes the system maintainable
at scale.

### Estimate

2 weeks.

---

## Cross-cutting: testing strategy

The dimensionality (platform × OS-tier × form-factor × theme) makes
exhaustive screenshot testing infeasible. The strategy is:

1. **Schema contract tests**: every example payload must decode on
   every platform. CI fails on decode regression.
2. **Renderer unit tests**: per-primitive, per-platform. Token
   resolution, variant resolution, override-matrix behavior. Mock
   form factor and theme.
3. **Per-platform per-form-factor snapshot tests**: one snapshot per
   `(platform, OS-tier representative, form-factor, theme)` tuple
   for each reference screen. Bounded combinatorics: pick *one*
   representative OS-tier per platform, not every tier. Roughly (this
   program — **no TV**):
   - iOS: latest tier × **phone + tablet** representative form factors
     × 2 themes
   - Android: same
   - Web: **narrow + wide** × 2 themes
   - Total: bounded snapshots per reference screen. **+TV** when that
     program runs.
4. **Cross-platform contract tests**: identical payload, decoded on
   each platform, verifies semantic equivalence (right number of
   elements, right tokens resolved, right action bindings) — not
   pixel parity.
5. **Accessibility audits**: per-platform per-screen. Run on CI for
   reference screens.
6. **Manual TV testing** — **deferred** with the TV/Roku program. When
   TV ships, add a manual D-pad plan; it cannot be fully automated.

---

## What this plan does *not* do

The following are explicitly out of scope. If a question lands that
needs one of them, the answer is "typed section, not atomic":

- Live region announcements for ticking scores.
- Custom focus-traversal order for non-linear grids.
- Pinch-to-zoom, swipe-to-dismiss, drag-to-reorder.
- Video/audio playback surfaces.
- Billing flows.
- Ad SDK integration.
- Maps, sensors, camera.
- Vertical writing-mode (Japanese tate-gaki).

These are typed sections by design. Trying to extend atomic composition
to cover them is the failure mode the dual-layer architecture exists to
prevent. If you find yourself adding capabilities to atomic composition
that ladder to one of the above, stop and write a typed section spec
instead.

---

## Suggested calendar

If staffed with one server engineer + one engineer per client platform
(iOS, Android, web), working in parallel ( **phone + tablet + web**;
**no TV engineer** in this program):

- Week 1: Phase 1 (`onActivate`).
- Weeks 2–3: Phase 2 (semantic tokens; bundled registries).
- Weeks 4–5: Phase 3 (form factor + composition routing; **required
  `formFactor`**; tablet + web, not TV).
- Week 6: Phase 4 (a11y) + Phase 5 (i18n) in parallel.
- Weeks 7–9: Phase 6 (variant realization × form factor, **no `tv` row**
  in initial scope).
- Weeks 10–11: Phase 7 (hardening) + buffer.

Critical path: Phase 1 → Phase 2 → Phase 3 → Phase 6. Other phases
parallelize. **TV/Roku** follow a separate program after this track.

---

## Things to flag back if encountered

If during execution any of the following come up, escalate rather than
guess:

1. A composer that *requires* pixel-perfect output across platforms
   (brand-takeover moment, marketing module with exact spec). The design
   system doc currently has no escape hatch for pixel parity.
   Recommendation: typed section, not atomic — but flag for design
   sign-off.
2. A typed section where >70% of its rendering could plausibly be
   atomic but it's currently typed for one specific reason (e.g.
   one-line of state). Flag — may indicate a typed-section that could
   migrate to atomic + a small typed wrapper, expanding the KPI.
3. Form factors not in the §5.5 enumeration (foldable in transition
   state, vehicle infotainment, smart watch). Flag — do not silently
   add to the enum.
4. Any case where strict decode would crash a consumer because of
   a new wire field. **In this repo** breaking changes are allowed until
   external clients exist; still prefer **additive-first** in the
   long-lived contract. If not additive, call it out in reviews.
5. **TV**-specific focus beyond document order — **deferred**; when TV
   ships, that’s still a candidate for **typed** sections, not new atomic
   focus-order APIs.

---

## End-state diff summary

When all phases are complete, the system has:

- `onActivate` everywhere; `onTap` deprecated and removed in following
  release.
- Six token registries (color, size, spacing, typography, corner-radius,
  shadow), all but color form-factor-aware.
- Form-factor classifier on every client; envelope **always** includes
  required `formFactor` from production clients.
- Composition service routes to form-factor-aware composers.
- `a11y` block on every atomic primitive; CI enforces label coverage.
- RTL support throughout atomic composition; icon-mirroring registry.
- Variant realizations specified per form factor for the high-leverage
  variants.
- Diagnostics for decode failure, variant override blocked, token
  missing, deprecated trigger.
- Per-platform per-form-factor snapshot test coverage.
- Atomic-share KPI dashboard tracking the percentage of UI changes
  shipping via server deploy vs. client release.

The reference docs are the source of truth for the contract; this plan
is the path to closing the gap between current state and that contract.
