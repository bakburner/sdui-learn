# TBC — Governance tensions 2–4 and outstanding work

Scratchpad for picking up tomorrow. Tension 1 (Amendment A, "promotion
readiness" + shared `section.display` / `SectionContainer` wrapper) is
landed and the four reserved sections (`SubscribeHero`,
`SubscribeBanner`, `AdSlot`, `VideoPlayer`) all flow their outer chrome
through the server-driven display block. What remains is the governance
rewrite and the follow-on code migrations for Tensions 2–4.

---

## Tension 2 — Permanent sections need schema parity with `Container`

### The problem

Rule 15 says "visual configuration must be server-driven" for permanent
sections, but the schema under-specifies what the server can actually
emit. The audit's canonical example is `GamePanelDisplayConfig`:

- Exposed today: `logoSize`, `cardHeight`, `cornerRadius`, `elevation`,
  `scoreTextStyle`.
- Missing: `padding`, `shadow` (full object form), `border`, `margin`,
  `background`.

Because the schema has no knob, every client hardcodes the missing
axes. `GamePanelView.swift` still has:

```swift
.padding(variant == .featured ? 20 : 16)
.cornerRadius(CGFloat(data.displayConfig?.cornerRadius ?? (variant == .featured ? 16 : 12)))
.shadow(color: Color.black.opacity(variant == .featured ? 0.18 : 0.08),
        radius: variant == .featured ? 8 : 3,
        y:      variant == .featured ? 4 : 2)
```

That is technically rule-compliant (the renderer reads what the schema
exposes) but exactly the outcome Rule 15 was trying to prevent — three
drift sites across Android / iOS / web, each hardcoding different
numbers.

### Partial progress already landed

- The **section envelope** `section.display` now carries
  `margin / padding / background / cornerRadius / shadow / border` and
  is applied by `SectionContainer` on all three platforms. This
  covers the **outer** chrome for every permanent section (not just
  GamePanel).
- **Atomic elements** now carry `cornerRadii` (per-corner override)
  on top of the existing `cornerRadius / shadow / background / padding
  / margin / border` surface.

What's still missing:

- **`GamePanelDisplayConfig`** does not expose `padding` / `shadow`
  (object) / `margin` / `border` / `background` for the *inner* card
  — all of those are still hardcoded per variant in the three
  `GamePanelView` renderers.
- **`VideoPlayerSection.displayConfig`** exposes only `aspectRatio`
  and `height`. Everything else (player-area background, overlay
  chrome) is implicit.
- Other permanent sections (`BoxscoreTable`, `SeasonLeadersTable`,
  `Form`, `TabGroup`) do not have a `displayConfig` at all — inner
  styling is entirely renderer-owned.

### Proposed Amendment B (to add to Rule 15)

> Every permanent section's `displayConfig` (or data-level styling
> object) must expose at least the same visual surface that atomic
> `Container` exposes: `padding`, `cornerRadius` /`cornerRadii`,
> `background`, `shadow`, `border`, `margin`. Under-specification of
> these fields is a schema bug, not a license for client-side
> hardcoding. If a visual property is hardcoded in any client because
> the schema does not expose it, the remediation is a schema change,
> never a client-side default.

### Remaining work

1. Add a reusable `SectionVisualConfig` definition in
   `schema/sdui-schema.json` covering `padding`, `margin`,
   `background`, `cornerRadius`, `cornerRadii`, `shadow`, `border`.
2. Refactor `GamePanelDisplayConfig` to compose `SectionVisualConfig`
   (keep `logoSize`, `cardHeight`, `scoreTextStyle` as section-specific
   additions).
3. Do the same refactor on `VideoPlayerSection.displayConfig` and on
   any other permanent section whose data object styles its inner
   card.
4. Regenerate models (`make codegen`).
5. Port `GamePanelView` (iOS / Android / web) to read these fields
   instead of the `variant == .featured ? A : B` ternaries. The
   ternaries become composer-side bundle emissions (see Tension 4).
6. Decide: is `variant` on `GamePanel` still useful as a *structural*
   hint (different child hierarchy) or should it be dropped entirely
   once the numeric bundle is server-emitted? See Amendment D below.

---

## Tension 3 — Rule 18 criterion 2 ("platform-owned") is ambiguous

### The problem

Rule 18 criterion 2 allows a vocabulary to be client-realized when
"the set of valid realizations is owned and versioned by the
platform." That cleanly covers:

- **Platform-owned taxonomies** — SF Symbols vs Material icons vs
  `lucide-react`; `.ultraThinMaterial` / Liquid Glass / Material
  surfaces; native typography scales; IME action buttons; gesture
  recognizers.

It does **not** cleanly cover:

- **Shared design-system numerics** — `ContainerVariant.elevated`
  (cornerRadius 12, shadow 2/4), `ImageVariant.thumbnail` (radius 8),
  `ButtonVariant.primary` (padding, radius, height). The numbers are
  intentionally identical across platforms; the platform does not own
  the taxonomy. ADR-013 admitted these anyway because the
  alternative — the server emitting full numeric bundles for every
  `Container` — was wire-heavy.

Admitting (b) under the same rule as (a) is exactly the drift risk
criterion 2 was meant to prevent. The iOS `ContainerVariantResolver`
and Android `ContainerVariantResolver` and web `ContainerVariantResolver`
are all hand-authored copies of the same numeric table — any drift
between them is a bug, but there is no CI check that catches it.

### Partial progress already landed

- `schema/style-tokens.json` exists as the canonical registry.
- `scripts/validate-style-tokens.js` exists and validates something
  about the registry, but it does not currently diff the per-platform
  resolvers against the registry (it only checks internal shape).
- `schema/color-tokens.json` + `scripts/validate-color-tokens.js`
  establish the pattern we want for style tokens.

### Proposed Amendment C (rewrite Rule 18 criterion 2, extend Rule 16)

> Client-realized vocabularies fall into two categories with
> different governance:
>
> **(a) Platform-owned vocabularies.** The set of valid realizations
> is owned and versioned by the platform. Per-platform realization is
> mandatory. Cross-platform visual divergence is expected.
>
> **(b) Shared design-system vocabularies.** The semantic vocabulary
> is shared (`TextVariant`, `ContainerVariant`, `ImageVariant`,
> `ButtonVariant`). Numerics are identical across platforms *by
> definition of the token*. Client-side realization is a bandwidth
> optimization. Drift between platforms for a (b) token is a bug.
>
> Category (b) has three governance requirements that (a) does not:
>
> 1. **Single source of truth.** Numerics live in
>    `schema/style-tokens.json` (resp. `color-tokens.json`,
>    `icon-tokens.json`). Resolvers must be codegened or verified
>    against the registry at build time.
> 2. **Drift detection.** A CI check compares each platform's
>    resolver output against the registry and fails on divergence.
> 3. **Migration trigger.** When per-screen tuning pressure or
>    cross-platform drift appears on a (b) token, that token is
>    migrated to server-emitted inline fields. The token continues
>    to exist as a client-side default; server-emitted fields
>    override it when present. (This is the schema-parity path
>    from Amendment B applied retroactively.)

### Remaining work

1. Extend `scripts/validate-style-tokens.js` (or add a sibling
   script) that parses each platform's resolver file and diffs the
   numeric tables against `schema/style-tokens.json`. Fail loudly on
   divergence. Wire into CI.
2. Consider codegening the resolvers from the registry outright
   (Swift, Kotlin, TS) so hand-editing is not possible. Lower bar:
   keep hand-authored but add the CI diff from step 1.
3. Document the "migration trigger" pathway: when a token is being
   pressured per-screen, the fix is to promote the numeric fields to
   inline schema props on the primitive — not to fork the token.
   `cornerRadii` (landed today) is the first example of this
   pattern: radius was a single variant-resolved number; per-corner
   pressure moved it to an inline field.

---

## Tension 4 — Variants as "realization triggers" vs "bundle keys"

### The problem

The pattern in `GamePanelView.swift` is

```swift
.padding(variant == .featured ? 20 : 16)
.shadow(radius: variant == .featured ? 8 : 3, ...)
```

A semantic variant (`featured`) is being used as a **client-side
lookup key into a numeric table**. The client holds an implicit
`{featured: {padding: 20, shadow: 8pt}, standard: {padding: 16, shadow:
3pt}}` map. The server cannot say "featured, but with padding 24"
without a client release.

There are two coherent positions on what `variant` is, and today's
code mixes them:

- **Position 1 — Realization trigger.** The client owns the style
  bundle; server just names the variant. This is how `TextVariant`
  works: server says `titleMedium`, each platform picks its native
  font. Works cleanly when the bundle is platform-owned (Tension 3
  category (a)).
- **Position 2 — Bundle key.** The server emits the *full* numeric
  `displayConfig` inline whenever it sends the variant, using the
  variant name as a lookup into *its own* style registry. The client
  just renders whatever `displayConfig` arrives. Works cleanly when
  the variant is a shared design-system token.

### Proposed Amendment D (add to Rule 16, new subsection)

> A variant token on an **atomic primitive** (`Text.variant`,
> `Button.variant`, `Image.variant`, `Container.variant`) is a
> **client-realization trigger**: the client holds the style bundle
> keyed off the token name and renders. This is the category (b)
> path from Rule 18 criterion 2.
>
> A variant token on a **section** (`GamePanel.variant`, `Form.variant`,
> future sections) is a **style-bundle key for the server, not the
> client**: the server resolves the variant to a full numeric
> `displayConfig` and emits it inline on every response. Section
> variants are never client-side numeric lookups.
>
> A section renderer may read `variant` to decide which *layout
> structure* to use (a featured card can have a different child
> hierarchy than a standard card), but must not read `variant` to
> pick numeric style values. Numeric style values come from the
> `displayConfig` the server emits.

### Remaining work

1. Pick a position for each section variant currently in flight.
   Concretely: audit `GamePanelVariant` (`featured` vs `standard` vs
   `compact`), `FormVariant` (if any), any future section variant.
2. For each, decide:
   - **Drop.** If the variant only gates numeric style, delete the
     variant and have the server emit the right `displayConfig`
     bundle per site.
   - **Keep as structure hint.** If the variant changes child
     hierarchy (different number of rows, different logo treatment),
     keep it but strip all numeric-style reads from the renderer.
3. Update the composer to emit the full bundle. For GamePanel this
   means `buildFeaturedGamePanel` emits
   `displayConfig: { padding: {all: 20}, shadow: {...},
   cornerRadius: 16 }` inline, and `buildGamePanel` emits the
   "standard" bundle. Identical numbers, one source.
4. Retire the `variant == .featured ? A : B` ternaries in
   `GamePanelView` on all three clients.

---

## Outstanding work from this session (not a new tension)

These picked up accidentally while debugging the visual regressions
and should be finished when we resume:

- [ ] **Margin support on `AtomicElement` was marked in-progress.**
  Verify it is actually in the schema + the three renderers. If not,
  finish. (`c-atomic-margin` todo; schema shows `margin` is defined,
  but renderer wiring was never end-to-end verified — web has it,
  Android/iOS should be double-checked.)
- [ ] **Run `make codegen` on a host without the sandbox** so the
  Java POJO step completes. Client codegen (Swift / Kotlin / TS) is
  up to date, but the server's generated Java models may be stale
  for the recent `fillWidth` and `cornerRadii` additions.
- [ ] **Verify the rail-spacing / card-corner fix visually** on iOS
  after a server restart + iOS rebuild. Today's changes:
  - `railDisplay()` bumped from 8/8 to 16/16 margin top/bottom.
  - `buildContentCard` / `buildVideoCard` now emit
    `cornerRadii: {topStart: 12, topEnd: 12, bottomStart: 0,
    bottomEnd: 0}` on the card, so headline text no longer collides
    with the bottom-corner curve.
  - `cornerRadii` is wired end-to-end (schema + Swift / Kotlin / TS
    codegen + iOS `AtomicContainerView` + `AtomicImageView` + iOS
    `ContainerVariantResolver` + Android `AtomicContainer` +
    `AtomicImage` + web `AtomicContainer` + `AtomicImage`).
- [ ] **Android + web parity check** — same rebuild + manual QA on
  Android (`make dev-android`) and web (`make dev-web`) for the
  spacing + corner changes.
- [ ] **Follow-up: gradient background on `AtomicContainer`.** Still
  open from earlier in the session; needed for true PromoBanner /
  SubscribeBanner gradient parity once those sections are fully
  composed atomically.
- [ ] **Follow-up: owner sweep of `docs/appendix-kitchen-sink.md`**
  for the schema changes this session (`fillWidth`, `cornerRadii`,
  bumped `railDisplay` margin).
- [ ] **Follow-up: `GamePanel` ref-app layout redesign.** Deferred;
  will want to pair this with the Tension 2 + 4 work above since
  both touch the same renderer.

---

## Suggested order for tomorrow

1. **Tension 2 first** — it has the most immediate leverage and
   unblocks Tension 4. Add `SectionVisualConfig` to the schema,
   refactor `GamePanelDisplayConfig` to compose it, codegen, port
   renderers.
2. **Tension 4 next** — now that the schema exposes the knobs, retire
   the `variant == .featured ? ...` ternaries. This is the payoff
   for Tension 2.
3. **Tension 3 last** — it's the biggest governance change and the
   code impact (CI diff script or full codegen of resolvers) is
   orthogonal to the GamePanel work in T2/T4. Safer to land T2/T4
   first so the migration trigger in Amendment C already has at
   least one worked example to reference.
