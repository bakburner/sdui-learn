# ADR-006: Experiment Assignment Model

- Status: Accepted
- Date: 2026-02-20
- Accepted: 2026-03-30
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Experiment assignment is **fully client-authoritative**. Clients resolve variant assignments via an experiment SDK (Amplitude) at app start and send them as the `experiments` field in the request envelope. The composition server trusts and uses these assignments directly. The server has no kill switch, no response echo, and no exposure logging — all of those responsibilities belong to the client.

## Context

Amplitude SDK performs experiment bucketing on-device. Clients already fire Amplitude beacons with assignment data, so the client is the single source of truth for which variant a user is in. Server-side override would break attribution — the client would report variant B to Amplitude while the server composed variant A.

## Model

- Client resolves experiment assignments via experiment SDK at app start (per-session).
- Client sends `experiments` map (key → variant) in the request envelope — these are **assignments**, not hints.
- Server **trusts** the assignments and uses them for composition branching via `resolveVariant(experimentId, default)`.
- **Kill switch is client-side.** To disable a variant, the client stops sending that experiment. The server never sees it and falls back to default.
- **No response echo.** The client already knows its assignments — the server does not echo them back.
- **Exposure tracking is client-side** via fire-and-forget actions. No server-side exposure logging.
- Analytics attribution flows through Amplitude beacons, which the client fires independently.

## Why

- Amplitude is the assignment source — client is inherently authoritative
- Client already reports assignments to Amplitude via beacons, so analytics attribution is naturally consistent
- Server does not need its own experiment resolution service — just reads the map and branches
- Client-side kill switch is simpler and maintains single ownership — no server-side disabled-variant storage or propagation needed
- No echo avoids redundant data and a response schema field that adds no value
- No server exposure logging avoids duplicate tracking (client already tracks via fire-and-forget)

## Caching

Experiment assignments travel as query parameters (`experiments[exp_id]=variant_b`), so they are naturally part of the URL and the CDN cache key. Different assignments produce different cache entries. No `Vary` header handling needed.

## Migration

The legacy `variant` query param has been removed. Existing variant functionality is retained via `experiments[variant]` — a placeholder experiment key that exercises the real experiment code path.

