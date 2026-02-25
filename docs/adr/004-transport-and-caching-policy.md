# ADR-004: Transport and Caching Policy

- Status: Proposed
- Date: 2026-02-20
- Decision owners: Adrian Robinson (interim), platform leads, backend leads

## Decision

Support both `GET` and `POST` composition routes based on context and cacheability, with section-first caching and optional screen snapshot caching.

## Context

Not all compositions have the same personalization/security/caching constraints. A single transport method is either too restrictive or unsafe.

## Policy

- `GET` allowed for cache-friendly, read-only compositions.
- `POST` used when request context is sensitive/large or composition inputs are complex.
- Authenticated requests can still use `GET`; method choice is independent from auth mechanism.

## Cacheability Classes

- `public`: edge-cacheable
- `contextual`: edge-cacheable with normalized vary keys
- `personalized`: private cache only / no shared edge cache
- `live`: no cache or very short-lived cache

## Caching Strategy

- Primary: section-level caching
- Secondary: screen snapshot caching for fast first paint/fallback
- Never use raw tokens in cache keys

## Governance

Route-level cache/method policy requires platform + backend input.

