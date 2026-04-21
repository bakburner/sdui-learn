# ADR-004: Transport and Caching Policy

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads
- Related ADRs: ADR-011 (Data Classification & Freshness Model)

## Decision

Support both `GET` and `POST` composition routes based on context and cacheability, with server-controlled `Cache-Control` headers derived from the cacheability classification defined in ADR-011.

## Context

Not all compositions have the same personalization/security/caching constraints. A single transport method is either too restrictive or unsafe.

## Scope

This ADR covers **HTTP transport governance**: method selection (GET vs POST), authentication interaction, cache header policy, and cache key hygiene. It does **not** define the cacheability classification itself (see ADR-011) or client-side storage architecture (see ADR-012).

## Policy

- `GET` allowed for cache-friendly, read-only compositions.
- `POST` used when request context is sensitive/large or composition inputs are complex.
- Authenticated requests can still use `GET`; method choice is independent from auth mechanism.

## Cache Header Policy

The server sets `Cache-Control` headers based on the response's `cacheability` field (defined in ADR-011):

| Cacheability (ADR-011) | `Cache-Control` header |
|------------------------|----------------------|
| `public` | `public, max-age=3600` |
| `contextual` | `public, max-age=1800, Vary: X-Platform, Accept-Language` |
| `personalized` | `private, max-age=300` |
| `live` | `no-store` or `private, max-age=10` |

## Caching Strategy

- Primary: section-level caching (server-side, edge-side)
- Secondary: screen snapshot caching for fast first paint/fallback
- Never use raw tokens in cache keys
- Client-side storage architecture is governed by ADR-012

## Governance

Route-level cache/method policy requires platform + backend input.

