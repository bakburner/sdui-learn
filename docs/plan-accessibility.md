# SDUI Accessibility — Cross-Cutting Plan

> **Date**: 2026-03-13
> **Status**: Draft
> **Scope**: Schema, Android (Jetpack Compose), Web (React/TSX), iOS (SwiftUI — future), Server composition
> **Depends on**: Atomic Primitives Plan, Requirements Summary, WCAG 2.1 AA
> **Related**: ADR-005 (Action Scope and Precedence), Action Failure Semantics Plan

---

## Problem Statement

The SDUI platform currently has **no systematic accessibility strategy**. Accessibility today is:

- **Ad hoc** — individual developers add `contentDescription` (Android) or `alt` (Web) where they remember to.
- **Inconsistent** — the same semantic concept (team logo) gets an accessibility label in one renderer and `alt=""` in another.
- **ID-as-label** — the atomic layer uses `element.id` as the accessibility label for images (`AtomicImage.kt` line 44: `contentDescription = element.id`, `AtomicImage.tsx` line 38: `alt={element.id ?? ''}`), which produces meaningless labels like `"hero-bg-image"`.
- **Missing at the schema level** — no accessibility properties exist on `AtomicElement`, `Section`, or `Subsection`. Accessibility metadata cannot be server-driven.
- **Platform-divergent** — Android permanent section renderers apply `contentDescription` to some renderers; Web uses `alt` on images but only one renderer applies `role="button"`; iOS hasn't been built yet and will inherit whatever exists. (9 former section types are now server-composed AtomicComposite — their accessibility is handled at the atomic layer.)

This defeats the SDUI promise: if the server controls UI composition, it must also control accessibility semantics. A screen reader user on Android, iOS, and Web should encounter equivalent experiences for the same server payload.

---

## Current State Audit

### Android — Permanent Section Renderers (8 renderers)

| Renderer | contentDescription | semantics{} blocks | Roles / Grouping |
|---|---|---|---|
| GamePanelRenderer | ✅ tricode for logos (4 instances); `"${teamName} logo"` for scoreboard-style displayConfig | ❌ | ❌ |
| BoxscoreTableRenderer | ✅ player name for headshots | ❌ | ❌ |
| SubscribeBannerRenderer | ⚠️ `contentDescription = null` (decorative) | ❌ | ❌ |
| TabGroupRenderer | ❌ | ❌ | ❌ |
| AdSlotRenderer | ❌ (3rd party SDK) | ❌ | ❌ |
| SeasonLeadersTableRenderer | ❌ | ❌ | ❌ |
| FormRenderer | ❌ | ❌ | ❌ |
| SubscribeHeroRenderer | ❌ | ❌ | ❌ |

**Summary**: 2 of 8 permanent section renderers provide image descriptions. Zero use Compose `semantics {}` blocks for roles, headings, or grouping. No traversal ordering. No live-region support. (9 former section renderers — StatLine, HeroPanel, ContentRail, PromoBanner, SectionHeader, VideoCarousel, NbaTvSchedule, FollowingRail, ErrorState — have been removed; those types are now server-composed AtomicComposite and their accessibility is handled at the atomic layer.)

### Android — Atomic Primitives (9 renderers)

| Primitive | Accessibility |
|---|---|
| AtomicImage | `contentDescription = element.id` (meaningless) |
| AtomicButton | `label` text likely read; no explicit role override |
| AtomicText | Compose `Text` is natively readable — no issues |
| AtomicContainer | No `semantics {}` — children not grouped |
| AtomicDataTable | No column/row header association for screen readers |
| AtomicDivider | No `clearAndSetSemantics {}` — incorrectly focusable |
| AtomicSpacer | No `clearAndSetSemantics {}` — incorrectly focusable |
| AtomicScrollContainer | No scroll semantics announcement |
| AtomicConditional | Pass-through — delegates to child |

### Web — Permanent Section Renderers (8 renderers)

| Renderer | alt text | ARIA roles | ARIA labels |
|---|---|---|---|
| GamePanel | ✅ tricode for logos; teamName for scoreboard-style displayConfig | ❌ | ❌ |
| BoxscoreTable | ✅ tricode for team logo, player name for headshots | ❌ | ❌ |
| SubscribeHero | ⚠️ `alt=""` for bg, `"NBA League Pass"` for logo | ❌ | ❌ |
| SubscribeBanner | ⚠️ `alt=""` for image | ❌ | ❌ |
| SeasonLeadersTable | ❌ | ❌ | ❌ |
| TabGroup | ❌ | ❌ | ❌ |
| AdSlot | ❌ (3rd party SDK) | ❌ | ❌ |
| FormRenderer | ❌ | ❌ | ❌ |

**Summary**: 2 renderers provide meaningful alt text. 2 renderers use `alt=""` (decorative treatment). No `aria-live`, no `aria-expanded`, no landmark roles. (9 former section renderers have been removed — those types are now server-composed AtomicComposite and their accessibility is handled at the atomic layer.)

### Web — Atomic Primitives (9 renderers)

| Primitive | Accessibility |
|---|---|
| AtomicImage | `alt={element.id ?? ''}` (meaningless) |
| AtomicButton | `<button>` is natively accessible; no `aria-label` if icon-only |
| AtomicText | `<span>` is readable — no issues |
| AtomicContainer | `<div>` with no role — children not grouped |
| AtomicDataTable | No `<table>` semantics (likely renders as divs) |
| AtomicDivider | `<hr>` is natively ignored — acceptable if styled as presentational |
| AtomicSpacer | No `aria-hidden="true"` — screen reader may pause on empty div |
| AtomicScrollContainer | No `aria-label` for scroll region |
| AtomicConditional | Pass-through — delegates to child |

---

## Design Principles

1. **Server is the authority.** The server has semantic context (it knows a Container is a "game card" or a "promo banner"). The server MUST provide accessibility metadata. Clients apply it natively.

2. **Graceful degradation.** If the server omits accessibility properties, clients fall back to sensible platform defaults (not `element.id`).

3. **Decorative vs. informative is a server decision.** The server marks images as decorative (`accessibilityHidden: true`) or informative (with `accessibilityLabel`). Clients must not guess.

4. **Section renderers own domain-aware accessibility.** Section renderers have typed data models (player names, team names, scores). They apply contextual accessibility from their data — the server doesn't need to duplicate this into accessibility fields.

5. **Atomic layer gets explicit metadata.** Atomic elements are generic — the server MUST provide accessibility labels, roles, and hidden flags because the client has no semantic context for generic primitives.

6. **Platform-native patterns always.** Android uses `semantics {}`, iOS uses `.accessibilityLabel()`, Web uses ARIA attributes. Never invent a custom accessibility system.

---

## Phase 1 — Schema Extension

### 1.1 Add `AccessibilityProperties` definition

```json
"AccessibilityProperties": {
  "type": "object",
  "description": "Server-provided accessibility metadata applied natively per platform",
  "properties": {
    "label": {
      "type": "string",
      "description": "Human-readable label announced by screen readers. Omit for elements whose text content is self-describing."
    },
    "role": {
      "type": "string",
      "enum": ["button", "image", "heading", "link", "tab", "tabpanel", "list", "listitem", "table", "row", "cell", "none"],
      "description": "Semantic role override. 'none' suppresses the element's intrinsic role."
    },
    "hidden": {
      "type": "boolean",
      "default": false,
      "description": "When true, element and its descendants are hidden from the accessibility tree (decorative content)."
    },
    "headingLevel": {
      "type": "integer",
      "minimum": 1,
      "maximum": 6,
      "description": "Heading level (1-6) for role=heading elements. Maps to aria-level (Web), accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android)."
    },
    "liveRegion": {
      "type": "string",
      "enum": ["polite", "assertive", "off"],
      "description": "Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS)."
    },
    "sortOrder": {
      "type": "integer",
      "description": "Override default accessibility traversal order. Lower values are visited first. Omit to use natural DOM/view order."
    },
    "hint": {
      "type": "string",
      "description": "Additional context announced after the label. Maps to accessibilityHint (iOS), contentDescription suffix (Android), aria-describedby text (Web)."
    }
  }
}
```

### 1.2 Add `accessibility` to `AtomicElement`

In the `AtomicElement` definition, add:

```json
"accessibility": {
  "$ref": "#/definitions/AccessibilityProperties",
  "description": "Server-provided accessibility metadata for this atomic element"
}
```

This goes alongside existing properties like `background`, `padding`, etc.

### 1.3 Add `accessibility` to `Section`

In the `Section` definition, add:

```json
"accessibility": {
  "$ref": "#/definitions/AccessibilityProperties",
  "description": "Section-level accessibility metadata (landmark role, live region, heading)"
}
```

### 1.4 Add `accessibility` to `Subsection`

In the `Subsection` definition, add:

```json
"accessibility": {
  "$ref": "#/definitions/AccessibilityProperties",
  "description": "Subsection-level accessibility metadata"
}
```

### 1.5 Deprecate `alt` on AtomicElement

The existing `alt` property on `AtomicElement` (used by Image type) is superseded by `accessibility.label`. During transition:

- If `accessibility.label` is present, use it.
- If absent but `alt` is present, fall back to `alt`.
- If both absent, use empty string (decorative).

`alt` will be marked with `"deprecated": true` in the schema description.

---

## Phase 2 — Android Implementation (Jetpack Compose)

### 2.1 Shared extension: `Modifier.applyAccessibility()`

Create `android/sdui-core/.../util/AccessibilityExt.kt`:

```kotlin
fun Modifier.applyAccessibility(a11y: AccessibilityProperties?): Modifier {
    if (a11y == null) return this

    if (a11y.hidden == true) {
        return this.clearAndSetSemantics {}  // hide from tree
    }

    return this.semantics(mergeDescendants = false) {
        a11y.label?.let { contentDescription = it }
        a11y.role?.let { role ->
            when (role) {
                "button" -> this.role = Role.Button
                "image" -> this.role = Role.Image
                "tab" -> this.role = Role.Tab
                "heading" -> heading()
                // Other roles: applied but may need custom SemanticsPropertyKey
                else -> { /* no-op for unsupported roles */ }
            }
        }
        a11y.headingLevel?.let { heading() }  // Compose heading() has no level granularity
        a11y.liveRegion?.let { region ->
            when (region) {
                "polite" -> liveRegion = LiveRegionMode.Polite
                "assertive" -> liveRegion = LiveRegionMode.Assertive
                else -> { }
            }
        }
        a11y.sortOrder?.let {
            // Compose uses traversalIndex for ordering
            traversalIndex = it.toFloat()
        }
        a11y.hint?.let { stateDescription = it }
    }
}
```

### 2.2 Apply to all 9 atomic primitives

Each atomic renderer wraps its root composable with `.applyAccessibility(element.accessibility)`:

**AtomicImage.kt** — Replace `contentDescription = element.id`:
```kotlin
AsyncImage(
    model = element.src,
    contentDescription = element.accessibility?.label
        ?: element.alt
        ?: "",  // decorative fallback
    modifier = modifier.applyAccessibility(element.accessibility),
    // ...
)
```

**AtomicContainer.kt** — Group descendants when accessibility label is present:
```kotlin
val a11y = element.accessibility
val rootModifier = modifier.applyAccessibility(a11y)
val mergeDescendants = a11y?.label != null

Box/Column/Row(
    modifier = if (mergeDescendants)
        rootModifier.semantics(mergeDescendants = true) {}
    else rootModifier
) { /* children */ }
```

**AtomicButton.kt** — Use label for icon-only buttons:
```kotlin
Button(
    modifier = modifier.applyAccessibility(element.accessibility),
    // ...
) {
    // Screen readers read element.label by default for text buttons.
    // For icon-only buttons, accessibility.label provides the spoken label.
}
```

**AtomicSpacer.kt / AtomicDivider.kt** — Always hide from accessibility tree:
```kotlin
Spacer(
    modifier = modifier.clearAndSetSemantics {}  // Never focusable
)
```

**AtomicDataTable.kt** — Add column/row header association:
```kotlin
Row(modifier = Modifier.semantics { heading() }) {
    // Header cells
}
// Data rows: each cell gets stateDescription = "${columnLabel}: ${value}"
```

**AtomicScrollContainer.kt** — Add scroll semantics:
```kotlin
LazyRow/LazyColumn(
    modifier = modifier
        .applyAccessibility(element.accessibility)
        .semantics { collectionInfo = CollectionInfo(rowCount, colCount) },
    // ...
)
```

**AtomicConditional.kt** — Pass-through, no changes needed (delegates to child).

**AtomicText.kt** — Apply accessibility (heading levels, live regions):
```kotlin
Text(
    text = element.content ?: "",
    modifier = modifier.applyAccessibility(element.accessibility),
    // ...
)
```

### 2.3 Apply to 19 section renderers

Section renderers already have typed data (player names, team names, scores). The strategy is:

1. **Apply `section.accessibility`** to the root composable of each renderer via `.applyAccessibility(section.accessibility)`.
2. **Keep existing `contentDescription` values** from data models — these are domain-aware and correct.
3. **Add missing `contentDescription` where data exists** but wasn't previously used (ContentRail titles, SectionHeader text, ErrorState messages).
4. **Add `semantics(mergeDescendants = true)`** to interactive card containers (GamePanel tap target, HeroPanel tap target) so screen readers announce them as a single entity.
5. **Add `semantics { heading() }`** to SectionHeader titles.
6. **Add `liveRegion`** to GamePanel scoreboard-style score text (scores update during live games).

**Priority fixes per renderer:**

| Renderer | Fix |
|---|---|
| GamePanelRenderer | Wrap tappable card in `semantics(mergeDescendants = true) { contentDescription = "$away vs $home" }`; add `liveRegion = LiveRegionMode.Polite` to score text for scoreboard-style displayConfig |
| HeroPanelRenderer | Add `mergeDescendants = true` to card; role = Button when action present |
| ContentRailRenderer | Add `contentDescription` to rail item images (use item title) |
| TabGroupRenderer | Compose `TabRow` supports accessibility natively — verify `selected` state is announced |
| SectionHeaderRenderer | Add `semantics { heading() }` to title text |
| SeasonLeadersTableRenderer | Add table semantics (column/row header association) |
| FormRenderer | Add label association (`labelFor`) between field labels and inputs |
| ErrorStateRenderer | Add `liveRegion = LiveRegionMode.Polite` to error message, `role = Button` to retry button |
| SubscribeHeroRenderer | Add `contentDescription` to hero image (use model title) |

### 2.4 Model changes

Update the generated model classes to include the `AccessibilityProperties` type:

```kotlin
data class AccessibilityProperties(
    val label: String? = null,
    val role: String? = null,
    val hidden: Boolean? = null,
    val headingLevel: Int? = null,
    val liveRegion: String? = null,
    val sortOrder: Int? = null,
    val hint: String? = null
)
```

Add `val accessibility: AccessibilityProperties? = null` to:
- `AtomicElement`
- `Section`
- `Subsection`

---

## Phase 3 — Web Implementation (React / TypeScript)

### 3.1 Shared utility: `accessibilityProps()`

Create `web/src/utils/accessibility.ts`:

```typescript
import type { AccessibilityProperties } from '../types/sdui';

export function accessibilityProps(
  a11y: AccessibilityProperties | undefined
): Record<string, string | number | boolean | undefined> {
  if (!a11y) return {};

  if (a11y.hidden) {
    return { 'aria-hidden': true, tabIndex: -1 };
  }

  const props: Record<string, string | number | boolean | undefined> = {};

  if (a11y.label) props['aria-label'] = a11y.label;
  if (a11y.role && a11y.role !== 'none') props['role'] = a11y.role;
  if (a11y.role === 'none') props['role'] = 'presentation';
  if (a11y.role === 'heading' && a11y.headingLevel) {
    props['role'] = 'heading';
    props['aria-level'] = a11y.headingLevel;
  }
  if (a11y.liveRegion && a11y.liveRegion !== 'off') {
    props['aria-live'] = a11y.liveRegion;
  }
  if (a11y.hint) props['aria-describedby'] = a11y.hint;
  // sortOrder: applied via CSS `order` or tabIndex at the component level, not here

  return props;
}
```

### 3.2 Apply to all 9 atomic primitives

**AtomicImage.tsx** — Replace `alt={element.id ?? ''}`:
```tsx
<img
  src={element.src}
  alt={element.accessibility?.label ?? element.alt ?? ''}
  {...(element.accessibility?.hidden ? { 'aria-hidden': true } : {})}
  // ...
/>
```

**AtomicContainer.tsx** — Spread accessibility props on root div:
```tsx
<div
  style={containerStyle}
  {...accessibilityProps(element.accessibility)}
>
  {children}
</div>
```

**AtomicButton.tsx** — Icon-only buttons get `aria-label`:
```tsx
<button
  style={buttonStyle}
  aria-label={element.accessibility?.label ?? element.label}
  {...accessibilityProps(element.accessibility)}
>
  {/* icon and/or label */}
</button>
```

**AtomicSpacer.tsx / AtomicDivider.tsx** — Always hidden:
```tsx
<div style={spacerStyle} aria-hidden="true" />
// or
<hr style={dividerStyle} aria-hidden="true" />
```

**AtomicDataTable.tsx** — Use semantic `<table>` HTML:
```tsx
<table {...accessibilityProps(element.accessibility)}>
  <thead>
    <tr>
      {columns.map(col => <th scope="col" key={col.key}>{col.label}</th>)}
    </tr>
  </thead>
  <tbody>
    {rows.map((row, i) => (
      <tr key={i}>
        {columns.map(col => <td key={col.key}>{row[col.key]}</td>)}
      </tr>
    ))}
  </tbody>
</table>
```

**AtomicScrollContainer.tsx** — Add `role="list"` and `aria-label`:
```tsx
<div
  role="list"
  aria-label={element.accessibility?.label ?? 'Scrollable content'}
  style={scrollStyle}
  {...accessibilityProps(element.accessibility)}
>
  {children.map((child, i) => (
    <div key={i} role="listitem">{renderChild(child)}</div>
  ))}
</div>
```

**AtomicText.tsx** — Heading levels:
```tsx
const a11y = element.accessibility;
if (a11y?.role === 'heading' && a11y.headingLevel) {
  const Tag = `h${a11y.headingLevel}` as keyof JSX.IntrinsicElements;
  return <Tag style={textStyle} {...accessibilityProps(a11y)}>{element.content}</Tag>;
}
return <span style={textStyle} {...accessibilityProps(a11y)}>{element.content}</span>;
```

**AtomicConditional.tsx** — Pass-through, no changes needed.

### 3.3 Apply to 19 section renderers

Strategy mirrors Android:

1. **Spread `accessibilityProps(section.accessibility)`** on the root `<div>` of each renderer.
2. **Fix empty `alt=""` on non-decorative images** — use data model values (title, player name, etc.).
3. **Add ARIA roles** to interactive elements (cards with actions → `role="button"`, tab groups → `role="tablist"`, etc.).
4. **Add `aria-live="polite"`** to live-updating content (scoreboard scores, game status text).
5. **Add landmark roles** where appropriate (`role="region"` with `aria-label` for major content areas).

**Priority fixes per renderer:**

| Renderer | Fix |
|---|---|
| GamePanel | `role="button"` on clickable cards; `aria-label="${away} vs ${home}"`; `aria-live="polite"` on score region for scoreboard-style displayConfig |
| HeroPanel | Already has `role="button"` — add `aria-label={model.headline}` |
| ContentRail | Change `alt=""` to `alt={item.title}` for thumbnails |
| TabGroup | Add `role="tablist"`, `role="tab"`, `aria-selected` |
| VideoCarousel | Change `alt=""` to `alt={video.title}` for thumbnails |
| PromoBanner | Change `alt=""` to `alt={model.title}` for hero image |
| NbaTvSchedule | Change `alt=""` to `alt={heroTitle}` for hero image |
| SubscribeBanner | Change `alt=""` to `alt={model.title}` |
| SectionHeader | Use `<h2>` or heading role for title |
| SeasonLeadersTable | Use `<table>`, `<th scope="col">`, `<th scope="row">` |
| FormRenderer | `<label htmlFor>` association for all inputs, `aria-required` |
| ErrorState | `role="alert"` or `aria-live="assertive"` for error messages |
| SubscribeHero | Background image `alt=""` is correct; ensure CTA button has `aria-label` |

### 3.4 Type changes

Add to `web/src/types/sdui.ts`:

```typescript
export interface AccessibilityProperties {
  label?: string;
  role?: 'button' | 'image' | 'heading' | 'link' | 'tab' | 'tabpanel'
       | 'list' | 'listitem' | 'table' | 'row' | 'cell' | 'none';
  hidden?: boolean;
  headingLevel?: 1 | 2 | 3 | 4 | 5 | 6;
  liveRegion?: 'polite' | 'assertive' | 'off';
  sortOrder?: number;
  hint?: string;
}
```

Add `accessibility?: AccessibilityProperties` to:
- `AtomicElement`
- `Section`
- `Subsection`

---

## Phase 4 — iOS Implementation (SwiftUI — Future)

When iOS implementation begins, follow the same pattern:

### 4.1 Shared extension: `.applyAccessibility()`

```swift
extension View {
    func applyAccessibility(_ a11y: AccessibilityProperties?) -> some View {
        guard let a11y = a11y else { return AnyView(self) }

        var view: AnyView = AnyView(self)

        if a11y.hidden == true {
            return AnyView(view.accessibilityHidden(true))
        }

        if let label = a11y.label {
            view = AnyView(view.accessibilityLabel(label))
        }
        if let role = a11y.role {
            switch role {
            case "button": view = AnyView(view.accessibilityAddTraits(.isButton))
            case "image": view = AnyView(view.accessibilityAddTraits(.isImage))
            case "heading": view = AnyView(view.accessibilityAddTraits(.isHeader))
            case "link": view = AnyView(view.accessibilityAddTraits(.isLink))
            default: break
            }
        }
        if let hint = a11y.hint {
            view = AnyView(view.accessibilityHint(hint))
        }
        if let sortOrder = a11y.sortOrder {
            view = AnyView(view.accessibilitySortPriority(Double(sortOrder)))
        }

        return view
    }
}
```

### 4.2 Codegen

The `codegen/` module generates Swift types from the schema. Add `AccessibilityProperties` to the generator so `AtomicElement`, `Section`, and `Subsection` include it in the generated Swift models.

---

## Phase 5 — Server Composition Guidelines

The server is the authority for atomic layer accessibility. Composition rules:

### 5.1 Atomic Composition Rules

| Element Type | Server MUST provide | Server SHOULD provide |
|---|---|---|
| Container (layout) | Nothing (transparent wrapper) | `label` when grouping creates a logical unit (e.g., "Game card: Lakers at Celtics") |
| Container (semantic) | `label` + `role` | `hint` |
| Text | Nothing (self-describing) | `role: "heading"` + `headingLevel` for section titles |
| Image (informative) | `label` | — |
| Image (decorative) | `hidden: true` | — |
| Button (text) | Nothing (label text is sufficient) | — |
| Button (icon-only) | `label` | `hint` for non-obvious actions |
| Spacer | `hidden: true` (always) | — |
| Divider | `hidden: true` (always) | — |
| ScrollContainer | `label` describing content | — |
| Conditional | Nothing (pass-through) | — |
| DataTable | `label` for table purpose | — |

### 5.2 Section-Level Rules

Sections have typed data models, so client renderers can derive most accessibility from domain data. The server provides `accessibility` on a Section only when:

- The section needs a **landmark role** (e.g., `role: "region"` with a label for a scoreboard area).
- The section contains **live-updating content** that should announce changes (`liveRegion: "polite"`).
- The section should be **hidden from the accessibility tree** entirely (`hidden: true` for decorative chrome).
- The section's default **traversal order** needs overriding (`sortOrder`).

### 5.3 Subsection-Level Rules

Subsections are nested interaction targets (e.g., tappable team area). The server provides `accessibility` on a Subsection when:

- The subsection's tap target needs a **descriptive label** beyond what the surrounding section provides.
- The subsection should announce a **role** (typically `button` or `link`).

---

## Phase 6 — Testing & Validation

### 6.1 Automated

| Tool | Platform | Purpose |
|---|---|---|
| **axe-core** | Web | Automated WCAG 2.1 AA checks in CI |
| **Espresso AccessibilityChecks** | Android | `AccessibilityChecks.enable()` in UI tests |
| **Accessibility Inspector** | iOS/macOS | Manual + Xcode audit |
| **Lighthouse** | Web | CI accessibility score gate (target ≥ 90) |

### 6.2 Manual

- **TalkBack** (Android) — full feed walkthrough, verify every card is announced correctly.
- **VoiceOver** (iOS) — equivalent walkthrough when iOS implementation exists.
- **VoiceOver** (macOS) / screen reader (NVDA/JAWS) — web walkthrough.
- **Keyboard navigation** (Web) — Tab/Shift+Tab through all interactive elements, verify focus ring visibility.
- **Switch Control** (iOS/Android) — verify all actions are reachable.

### 6.3 Acceptance Criteria

- Every informative image has a non-empty, meaningful label.
- Every decorative image is hidden from the accessibility tree.
- Every interactive element (button, link, tappable card) is reachable via screen reader and keyboard.
- Every heading uses the correct heading level (not skipping levels).
- Live scores announce changes via live regions.
- Tab groups announce selected state.
- Tables have associated column and row headers.
- No accessibility violations at WCAG 2.1 AA level (automated check).

---

## WCAG 2.1 AA Coverage Matrix

| WCAG Criterion | Relevant To | Covered By |
|---|---|---|
| **1.1.1** Non-text Content | Images, icons | `accessibility.label` on informative images; `hidden: true` on decorative |
| **1.3.1** Info and Relationships | Tables, headings, forms | Semantic HTML (`<table>`, `<h2>`), `role`, `headingLevel`, `<label>` association |
| **1.3.2** Meaningful Sequence | All content | Natural DOM order + `sortOrder` override |
| **1.4.3** Contrast (Minimum) | Text, icons | Out of scope for this plan (design system concern) — but note server-driven colors must meet 4.5:1 |
| **2.1.1** Keyboard | Interactive elements | `role="button"` on clickable divs, `tabIndex` management |
| **2.4.3** Focus Order | All interactive | `sortOrder` maps to `tabIndex` (Web), `traversalIndex` (Android), `accessibilitySortPriority` (iOS) |
| **2.4.6** Headings and Labels | Section headers | `role: "heading"` + `headingLevel` |
| **3.3.2** Labels or Instructions | Forms | `<label htmlFor>` (Web), `labelFor` semantics (Android) |
| **4.1.2** Name, Role, Value | All interactive | `label` (name), `role` (role), live region / state description (value) |
| **4.1.3** Status Messages | Scores, errors | `liveRegion: "polite"` for scores, `liveRegion: "assertive"` for errors |

---

## Implementation Sequence

| Step | Phase | Description | Effort |
|---|---|---|---|
| 1 | Schema | Add `AccessibilityProperties` definition + wire to AtomicElement, Section, Subsection | S |
| 2 | Codegen | Update codegen to emit `AccessibilityProperties` in Kotlin, Swift, TypeScript | S |
| 3 | Android | Create `AccessibilityExt.kt` shared extension | S |
| 4 | Android | Update 9 atomic primitives to use `applyAccessibility()` | M |
| 5 | Android | Fix ad-hoc `contentDescription` in 19 section renderers + add missing semantics | L |
| 6 | Web | Create `accessibility.ts` utility | S |
| 7 | Web | Update 9 atomic primitives with ARIA attributes | M |
| 8 | Web | Fix `alt=""` misuse + add ARIA roles to 19 section renderers | L |
| 9 | Server | Update server composition to emit `accessibility` properties | M |
| 10 | Testing | Add axe-core to web CI, Espresso AccessibilityChecks to Android CI | M |
| 11 | Testing | Manual screen reader walkthrough on both platforms | M |
| 12 | iOS | Implement when iOS renderer work begins | L |

**Dependencies**: Steps 1-2 must complete before 3-8. Step 9 (server) can start after step 1. Steps 10-11 validate 3-8.

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Server teams don't populate `accessibility` fields | Atomic layer remains inaccessible | Fallback chain (accessibility.label → alt → "") + linting rule in server to flag missing labels on Image elements |
| `headingLevel` doesn't map cleanly to Android Compose | Compose `heading()` is boolean, no levels | Accept limitation; Android screen readers treat all headings equally. Document in ADR if this becomes a problem. |
| `sortOrder` conflicts with natural reading flow | Confusing screen reader navigation | Server composition guidelines strongly discourage `sortOrder` except for skip-to-content patterns |
| `liveRegion` overuse causes announcement fatigue | Screen reader users overwhelmed | Server guidelines limit `liveRegion` to GamePanel scoreboard-style scores and ErrorState messages only |
| AdSlot accessibility not controllable | 3rd party SDK owns rendering | Document as out-of-scope; ad SDK compliance is vendor responsibility. Wrap with `aria-label="Advertisement"` / `semantics { contentDescription = "Advertisement" }` on the container. |

---

## Open Questions

1. **Should `AccessibilityProperties.label` support localization keys or literal strings?** Current design: literal strings (server resolves locale before generating the payload). Confirm with localization team.

2. **Should `hidden` automatically apply to all Spacer and Divider elements at the schema level, or should the server always emit it explicitly?** Current design: clients hardcode `aria-hidden` / `clearAndSetSemantics {}` for Spacer and Divider regardless of server payload. Server can still send `hidden: false` to override (e.g., a meaningful separator announced as "section break").

3. **Should the schema define an `accessibilityGroup` property** (separate from Container's implicit grouping) to merge descendants? Current design: a Container with an `accessibility.label` merges descendants. If no label, children are individually traversable. Is this sufficient?
