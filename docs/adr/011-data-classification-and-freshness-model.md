# ADR-011: Data Classification & Freshness Model

- Status: Proposed
- Date: 2026-04-15
- Decision owners: Adrian Robinson (interim), platform leads, backend leads
- Related requirements: `docs/sdui-requirements-summary.md` §9d, §9e
- Related ADRs: ADR-004 (Transport Policy), ADR-010 (Offline UX), ADR-012 (Client Data Architecture)

## Decision

Define a cacheability classification for SDUI responses and a two-phase freshness model that independently tracks layout staleness and data channel staleness. This ADR is the single owner of these definitions — other ADRs reference it.

## Context

SDUI content varies widely in time-sensitivity. A live game score stales in seconds; a schedule stales in hours; an editorial promo stales in days. Multiple systems need a shared vocabulary for classifying and reasoning about data freshness:

- **Transport** (ADR-004) uses cacheability to set HTTP cache headers and choose GET vs POST.
- **Offline UX** (ADR-010) uses cacheability to decide what's safe to show stale vs. what shows a placeholder.
- **Client data architecture** (ADR-012) uses cacheability for store eviction policy.
- **Visibility-gated refresh** uses cacheability to determine default pause behavior.
- **Data binding pipeline** needs a model for reasoning about which layer is stale when things degrade.

Previously, cacheability classes were defined inline in ADR-004 and referenced by ADR-010. The two-phase staleness model was buried in ADR-010's implementation notes. Both are cross-cutting concerns that belong in a single, independently referenceable decision.

## Decision Drivers

- Multiple ADRs and plans reference freshness semantics — a single source of truth prevents drift.
- The two-phase model (layout vs. data channel) is a distinct architectural insight that affects client behavior across all platforms.
- Cacheability classification is a data modeling concern, not a transport or offline concern.

## Cacheability Classes

Every SDUI response carries a `cacheability` field classifying its freshness requirements:

| Class | Meaning | Cache behavior | Stale display |
|-------|---------|----------------|---------------|
| `public` | No personalization, broadly shareable | Edge-cacheable, long TTL | Show stale with timestamp |
| `contextual` | Varies by context (locale, game state) but not per-user | Edge-cacheable with normalized vary keys | Show stale with indicator |
| `personalized` | Per-user content (subscription state, preferences) | Private cache only, no shared edge cache | Show stale with indicator (personalization may be outdated) |
| `live` | Real-time content (scores, play-by-play) | No cache or very short TTL | Placeholder: "Live data unavailable" when stale |

The server sets `cacheability` at response level. Clients use it for:

- **Transport** (ADR-004): `public` → aggressive `Cache-Control`, `live` → `no-store` or short `max-age`.
- **Offline display** (ADR-010): `live` stale sections show placeholder, others show stale content with indicator.
- **Store eviction** (ADR-012): `live` entries can be evicted aggressively, `public` entries persist longer.
- **Visibility-gated refresh**: `live` sections default to never pausing; `public`/`contextual` are safe to pause.

## Two-Phase Freshness Model

SDUI screens have two independently-stale layers:

1. **Layout freshness** — the section tree, display config, binding declarations, action definitions. This is the initial `/sdui/` response. It changes on server deployments or composition logic changes (hours to days).

2. **Data channel freshness** — SSE/Ably messages and poll responses that update section data fields via `dataBindings`. This changes in real time (seconds to minutes).

These layers can be stale in different combinations, producing distinct failure modes that require different client behaviors.

### Failure Mode Matrix

| # | Layout | Data Channel | Mode | Client Behavior |
|---|--------|-------------|------|-----------------|
| **F1** | Fresh | Connected | **Normal** | No indicator. Bindings apply normally. |
| **F2** | Fresh | SSE disconnected | **Channel lost** | Section-level staleness badge on affected sections. Ably SDK auto-reconnects with exponential backoff. After **30 seconds** of sustained disconnection, escalate to poll fallback if `refreshPolicy.url` exists for that section. Clear badge when SSE reconnects or poll succeeds. |
| **F3** | Fresh | Poll failing | **Poll degraded** | Track consecutive poll failures per section. After **2 consecutive failures**, show section-level staleness badge. Exponential backoff to poll interval: double on each failure, cap at **30 seconds**. Restore original `intervalMs` on success. Clear badge on next successful poll. |
| **F4** | Cached | SSE connected | **Layout drift** | Apply bindings normally. Binding path resilience absorbs shape drift — missing source paths keep previous values, consecutive-miss counter provides observability. Show **screen-level** offline banner ("Showing cached content"). Sections receiving live SSE updates function correctly for paths that still align. |
| **F5** | Cached | All disconnected | **Full offline** | Screen-level offline banner. Per-section behavior follows cacheability rules: `live` sections show placeholder, others show stale content with indicator. No data updates arrive. Pull-to-refresh retries the full screen fetch. |
| **F6** | Cached | Poll succeeding | **Partial recovery** | Screen-level offline banner (layout cached). Sections receiving successful poll updates clear their section-level staleness badge — fresh data despite stale layout. |

### Key Principles

1. **Screen-level and section-level indicators are complementary.** Screen banner = layout is from cache. Section badge = specific data channel is down. Both can display simultaneously.

2. **Binding path resilience absorbs layout–data shape drift (F4).** Missing source paths keep previous values rather than crashing. The consecutive-miss counter (threshold: 3) logs warnings for observability without degrading the user experience.

3. **Cacheability drives stale-display rules.** `live` content that cannot be refreshed shows a placeholder, not stale values. Other classes show stale content with appropriate indicators.

4. **Freshness is per-section, not per-screen.** A screen can have sections in different freshness states simultaneously — one section receiving live SSE updates while another's poll is failing.

## Consequences

- ADR-004 is scoped strictly to transport. It references this ADR for cacheability definitions.
- ADR-010 is scoped strictly to offline UX. It references this ADR for the freshness model and staleness display rules.
- ADR-012 (client data architecture) uses cacheability for eviction policy.
- The visibility-gated refresh plan uses cacheability to determine default pause behavior per section.
- Future ADRs that need freshness semantics reference this document, not ADR-004 or ADR-010.
