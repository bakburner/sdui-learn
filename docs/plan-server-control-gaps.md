# Plan: Server-Controlled Layout, Error States & Analytics

**Date:** 2026-03-11  
**Author:** Adrian Robinson  
**Status:** Draft  
**Related ADRs:** [ADR-008](adr/008-form-factor-layout-manager.md), [ADR-009](adr/009-impression-dedup-and-visibility-semantics.md), [ADR-005](adr/005-action-scope-and-precedence.md)

---

## Summary

Four changes across three layers (schema → codegen → client) to close gaps where the server should control section behavior without requiring store releases. The changes follow the existing SDUI pattern: server declares intent via schema fields, codegen propagates types, clients interpret generically.

| # | Fix | Priority | Risk Without Fix |
|---|-----|----------|-----------------|
| 1 | Fix operator precedence in `needsLiveWrapper` | P1 | Medium — subtle runtime bug |
| 2 | Server-controlled layout hints on `SectionList` | P1 | Medium — layout changes force release |
| 3 | Server-controlled error/loading states per section | P1 | Medium — UX changes per section force release |
| 4 | Router-level analytics/tracking hook | P2 | Low-Medium — new section types miss tracking |

---

## Design Decisions

### Layout: Option C (Hybrid) from ADR-008

Structural layout via semantic types (`Row`, future `Grid`/`SplitPane`); inter-section fine-tuning via `layoutHints`. Avoids over-engineering while giving server control over the most impactful layout knobs.

### Visibility: Server-side composition (no client-side conditions)

Client-side visibility expressions were considered and rejected. The server already controls which sections appear via composition — feature flags, A/B gating, user-segment filtering, and time-based conditions are all resolvable server-side at composition time. Adding a client-side condition evaluator would duplicate server responsibility and violate the core SDUI principle that the server owns composition. If a narrow need for state-gated visibility emerges (e.g., show/hide based on screen state that changes after delivery), it can be revisited then.

### Error/Loading: Per-section, not per-component

Matches the section-level granularity philosophy. Individual components within a section don't get independent error states.

### Analytics: Router-level, not component-level

Ensures every section type (including future ones) gets tracking automatically. Per-component tracking can be layered on top for subsection-level events.

---

## Step 1: Fix operator precedence in `needsLiveWrapper` (P1 — bug fix)

**Files:** `web/src/components/SectionRouter.tsx` (L126–L130)

### Problem

The `needsLiveWrapper` boolean expression uses `&&` and `||` without explicit parentheses. Due to `&&` binding tighter than `||`, the expression evaluates correctly **by accident**, but is fragile and misleading. Future edits or additional conditions could silently break it.

### Change

```diff
  const needsLiveWrapper = Boolean(
-   section.refreshPolicy?.type && section.refreshPolicy.type !== 'static' ||
-   section.dataBindings?.bindings?.length ||
-   defaultRefreshPolicy?.type && defaultRefreshPolicy.type !== 'static'
+   (section.refreshPolicy?.type && section.refreshPolicy.type !== 'static') ||
+   (section.dataBindings?.bindings?.length) ||
+   (defaultRefreshPolicy?.type && defaultRefreshPolicy.type !== 'static')
  );
```

**No functional behavior change** — this is a correctness/clarity fix only.

---

## Step 2: Server-controlled layout hints on `SectionList` (P1)

### Why

Today `SectionList` renders sections in a bare `<>` fragment with no server-controlled spacing, dividers, or layout hints. Any layout change between sections (add a divider, change spacing, switch to 2-column on tablet) requires an app release. ADR-008 is **Proposed** with three options; this plan adopts **Option C (Hybrid)** — semantic layout types already work (`Row` exists), and we add lightweight layout hints for inter-section spacing/dividers.

### 2a — Schema change

**File:** `schema/sdui-schema.json`

Add a `SectionLayoutHints` definition:

```json
"SectionLayoutHints": {
  "type": "object",
  "description": "Optional layout hints for section placement. Clients apply best-effort; unknown hints are ignored.",
  "properties": {
    "marginTop": {
      "type": "integer",
      "description": "Top margin in dp/points (0 = flush)"
    },
    "marginBottom": {
      "type": "integer",
      "description": "Bottom margin in dp/points"
    },
    "dividerAbove": {
      "type": "boolean",
      "description": "Render a divider line above this section",
      "default": false
    },
    "dividerBelow": {
      "type": "boolean",
      "description": "Render a divider line below this section",
      "default": false
    },
    "priority": {
      "type": "string",
      "enum": ["high", "normal", "low"],
      "default": "normal",
      "description": "Rendering priority hint — clients may use for lazy loading or viewport priority"
    }
  }
}
```

Add to `Section.properties`:

```json
"layoutHints": { "$ref": "#/definitions/SectionLayoutHints" }
```

**Design note:** This is intentionally narrow (spacing + dividers + priority). Structural layout (columns, grids, split-panes) stays with semantic section types like `Row` — per ADR-008 Option B, which is already implemented. This avoids over-engineering the hints while giving the server control over the most common layout changes that currently require releases.

### 2b — Codegen

Run `codegen/generate.sh`.

### 2c — Client: `SectionList` wrapper

**File:** `web/src/components/SectionRouter.tsx` (L143–L167)

Replace the bare fragment with a layout-aware wrapper per section:

```tsx
{sections.map((section) => (
  <div
    key={section.id}
    style={{
      marginTop: section.layoutHints?.marginTop ?? 0,
      marginBottom: section.layoutHints?.marginBottom ?? 0,
    }}
  >
    {section.layoutHints?.dividerAbove && <hr className="sdui-divider" />}
    <SectionRouter
      section={section}
      state={state}
      onAction={onAction}
      onStateChange={onStateChange}
      defaultRefreshPolicy={defaultRefreshPolicy}
    />
    {section.layoutHints?.dividerBelow && <hr className="sdui-divider" />}
  </div>
))}
```

Also apply the same in `App.tsx` where sections are mapped directly (it doesn't use `SectionList`).

### 2d — ADR-008 update

Update `docs/adr/008-form-factor-layout-manager.md` status from **Proposed** to **Accepted: Option C (Hybrid)** with the decision that structural layout uses semantic types (`Row`, future `Grid`/`SplitPane`) and inter-section hints use `layoutHints`.

---

## Step 3: Server-controlled error/loading states per section (P1)

### Why

Today, error and loading handling is only at the screen level in `App.tsx`. If a single section's live data fetch fails, there's no per-section error UI — the `LiveSectionWrapper` silently keeps stale data. The server can't customize error messages, retry labels, or skeleton types without an app release. Individual section components all hardcode their own `"No X data"` strings.

**Note:** The `ErrorState` section type already exists (built on Web and Android). It handles the case where the server knows at composition time that data is unavailable and explicitly composes an error section. The `sectionStates` proposal here is complementary — it handles what the client should show when a section's *live data* fails at runtime (SSE disconnect, poll 500), which the server can't know at composition time.

### 3a — Schema change

**File:** `schema/sdui-schema.json`

Add a `SectionStates` definition:

```json
"SectionStates": {
  "type": "object",
  "description": "Server-declared loading and error presentation for a section. Clients render these states when applicable.",
  "properties": {
    "loading": {
      "type": "object",
      "properties": {
        "skeleton": {
          "type": "string",
          "enum": ["shimmer", "spinner", "placeholder", "none"],
          "default": "shimmer",
          "description": "Which loading skeleton style to use"
        },
        "minHeightDp": {
          "type": "integer",
          "description": "Minimum height to reserve during loading (prevents layout shift)"
        }
      }
    },
    "error": {
      "type": "object",
      "properties": {
        "message": {
          "type": "string",
          "description": "Error message to display (e.g., 'Unable to load scores')"
        },
        "retryAction": {
          "$ref": "#/definitions/Action",
          "description": "Optional action to trigger on retry tap (typically a refresh action)"
        },
        "hideOnError": {
          "type": "boolean",
          "default": false,
          "description": "If true, collapse the section entirely on error instead of showing error UI"
        }
      }
    }
  }
}
```

Add to `Section.properties`:

```json
"sectionStates": { "$ref": "#/definitions/SectionStates" }
```

### 3b — Codegen

Run `codegen/generate.sh`.

### 3c — Client: `SectionErrorBoundary` component

**New file:** `web/src/components/SectionErrorBoundary.tsx`

A React Error Boundary class component that:
- Catches render errors in section components
- Reads the section's `sectionStates.error` to display the server-provided error message and optional retry button
- Falls back to a generic "Something went wrong" if no server-provided message
- Supports `hideOnError` to collapse the section entirely

### 3d — Client: `SectionSkeleton` component

**New file:** `web/src/components/SectionSkeleton.tsx`

Renders a shimmer/spinner/placeholder based on `sectionStates.loading.skeleton`. Respects `minHeightDp` for layout stability.

### 3e — Client: wire into `SectionRouter`

**File:** `web/src/components/SectionRouter.tsx`

Wrap `SectionRenderer` in `SectionErrorBoundary`:

```tsx
<SectionErrorBoundary
  sectionStates={section.sectionStates}
  sectionId={section.id}
  onAction={onAction}
>
  <SectionRenderer ... />
</SectionErrorBoundary>
```

Wire loading skeleton into `LiveSectionWrapper` — when `liveData` is `undefined` and a refresh is in-flight, render `<SectionSkeleton>` instead of passing `undefined` to children.

### 3f — Server

**File:** `server/src/main/java/com/nba/sdui/service/GameDetailComposer.java`

Add `sectionStates` to sections with live data, e.g.:

```json
{
  "id": "scoreboard-header-001",
  "type": "ScoreboardHeader",
  "sectionStates": {
    "loading": { "skeleton": "shimmer", "minHeightDp": 180 },
    "error": {
      "message": "Unable to load live scores",
      "retryAction": { "trigger": "onTap", "type": "refresh", "target": "scoreboard-header-001" },
      "hideOnError": false
    }
  },
  "data": { ... }
}
```

---

## Step 4: Router-level analytics/tracking hook (P2)

### Why

Today, `onVisible` actions and `ImpressionPolicy` are defined in the schema but never executed on the client — `handleAnalytics` in `ActionHandler.ts` is a `console.log` stub. Each new section type would need to independently implement impression tracking, which is fragile and will be missed. A router-level hook makes this automatic for all sections. Aligns with ADR-009's direction.

### 4a — Client: `useImpressionTracking` hook

**New file:** `web/src/hooks/useImpressionTracking.ts`

Uses `IntersectionObserver` to detect when a section's DOM element enters the viewport.

**Parameters:**
- `ref` — React ref to the section DOM node
- `sectionId` — section identifier for dedup
- `analyticsId` — optional analytics identifier
- `actions[]` — section actions (filtered to `trigger: 'onVisible'`)
- `onAction` — action dispatcher

**Behavior:**
- When intersection ratio meets the `impression.threshold.visibilityFraction` (default 0.5) for `impression.threshold.dwellMs` (default 1000ms), fire the analytics action via `executeAction`
- Respect `impression.dedup`:
  - `once-per-screen` — fire once, disconnect observer
  - `once-per-interval` — fire, wait `intervalMs`, re-arm
- Clean up observer on unmount

### 4b — Client: `useAnalyticsContext` hook

**New file:** `web/src/hooks/useAnalyticsContext.ts`

Provides a shared dedup registry (`Map<sectionId, lastFiredTimestamp>`) via React context, so `once-per-screen` dedup works across re-renders and `once-per-interval` can reset.

### 4c — Client: wire into `SectionRouter`

**File:** `web/src/components/SectionRouter.tsx`

Wrap each section's output in a `<div ref={trackingRef}>` and call `useImpressionTracking` with the section's actions:

```tsx
const trackingRef = useRef<HTMLDivElement>(null);

useImpressionTracking({
  ref: trackingRef,
  sectionId: section.id,
  analyticsId: section.analyticsId,
  actions: section.actions ?? [],
  onAction,
});

return (
  <div ref={trackingRef}>
    {/* existing render logic */}
  </div>
);
```

This happens at the router level, so **every section type — including future unknown types with a fallback renderer — gets impression tracking automatically**.

### 4d — ActionHandler enhancement

**File:** `web/src/runtime/ActionHandler.ts`

Replace the `console.log` stub in `handleAnalytics` with proper dispatch logic:
- Read `action.destinations` array (`adobe`, `firebase`, `internal`, `all`)
- Route to the appropriate beacon endpoint
- For the prototype, log structured JSON; document the integration point for production analytics SDKs

### 4e — ADR-009 update

Update `docs/adr/009-impression-dedup-and-visibility-semantics.md` status from **Proposed** to **Accepted** with the implemented decisions:
- `IntersectionObserver`-based visibility
- Parent-relative (browser-native)
- Individual beacon fire
- Pause not implemented in v1 (noted as follow-up)

---

## Verification

| Step | Test |
|------|------|
| 1. Operator precedence | No functional change. Run existing manual tests; visual diff confirms only grouping added. |
| 2. Layout hints | Add `layoutHints: { "marginTop": 24, "dividerAbove": true }` to a section. Verify spacing and divider render. Verify omitting `layoutHints` matches current behavior. |
| 3. Error/loading | Simulate a section render error via malformed data. Verify error boundary shows server message + retry. Stop SSE mid-stream; verify loading skeleton appears. |
| 4. Analytics | Add `onVisible` analytics action to a section. Scroll into view. Verify event fires after dwell threshold. Scroll away and back — verify `once-per-screen` dedup prevents re-fire. |

---

## Affected Files Summary

### Schema & Codegen
- `schema/sdui-schema.json` — add `SectionLayoutHints`, `SectionStates` definitions; add `layoutHints`, `sectionStates` to `Section`
- `codegen/generate.sh` — re-run to regenerate all platform models
- `codegen/output/typescript/SduiModels.ts` — regenerated
- `codegen/output/kotlin/SduiModels.kt` — regenerated
- `codegen/output/swift/SduiModels.swift` — regenerated

### Web Client
- `web/src/components/SectionRouter.tsx` — parentheses fix, error boundary wrapper, tracking ref
- `web/src/components/LiveSectionWrapper.tsx` — loading state awareness
- `web/src/components/SectionErrorBoundary.tsx` — **new**
- `web/src/components/SectionSkeleton.tsx` — **new**
- `web/src/runtime/ActionHandler.ts` — analytics dispatch enhancement
- `web/src/hooks/useImpressionTracking.ts` — **new**
- `web/src/hooks/useAnalyticsContext.ts` — **new**
- `web/src/App.tsx` — layout hints on direct section mapping

### Server
- `server/src/main/java/com/nba/sdui/service/GameDetailComposer.java` — add `sectionStates` to live sections

### Documentation
- `docs/adr/008-form-factor-layout-manager.md` — update status to Accepted (Option C)
- `docs/adr/009-impression-dedup-and-visibility-semantics.md` — update status to Accepted

---

## Sequencing

```
Step 1 (bug fix)          ─── can ship immediately, no dependencies
Step 2 (layout hints)     ─── schema → codegen → SectionList/App.tsx → server → ADR-008
Step 3 (error/loading)    ─── schema → codegen → ErrorBoundary + Skeleton → SectionRouter + LiveWrapper → server
Step 4 (analytics)        ─── hooks → SectionRouter → ActionHandler → ADR-009
```

Steps 2 and 3 share a schema change + codegen step and can be batched into a single schema update. Step 4 is independent and can proceed in parallel.
