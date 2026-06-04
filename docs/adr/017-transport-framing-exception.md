# ADR-017: Transport-framing exception for `ResponseEnvelope<T>`

- Status: Accepted
- Date: 2026-06-03
- Decision owners: Adrian Robinson (interim), platform leads
- Related requirements: `AGENTS.md` §1.2 (Schema is the contract); `docs/specs/sdui-envelope-spec.md`
- Related ADRs: ADR-003 (Composition API contract), ADR-004 (Transport and caching policy), ADR-016 (Update channel unification)

## Decision

Allow a hand-written, narrowly-scoped transport envelope —
`ResponseEnvelope<T>(T data, ResponseMeta meta)` — at the SDUI controller
edge, **outside** `schema/sdui-schema.json` and **outside** the codegen
pipeline. The inner `data` payload (`Screen`, `Section`) remains 100%
schema-bound and codegen-driven. Each client mirrors the envelope with a
hand-written `{data, meta}` wrapper that unwraps `.data` for renderers
and decodes-but-ignores `.meta` for now. Real freshness metadata lands in
a later phase (A2c); Step 1 ships a static-stub `meta`.

`ResponseMeta` carries exactly three fields in this phase:

```
ResponseMeta
├── degraded:        boolean   (default false)
├── staleSections:   string[]  (default [])
└── failedSections:  string[]  (default [])
```

## Context

`AGENTS.md` §1.2 (Schema is the contract) requires every emitted wire
field to live in `schema/sdui-schema.json` and route through codegen.
That rule keeps UI semantics — section types, action shapes, token
strings, refresh policies — under one validated, generated contract on
every platform.

Transport framing is a different concern. Correlation IDs, partial-
failure metadata, and staleness flags do not describe **what** UI was
composed; they describe **how** the wire frame was assembled and
delivered. Forcing transport framing through the schema would:

- Couple transport evolution to UI codegen runs. Every freshness-metadata
  iteration would force a schema bump, a `make codegen` run on four
  language outputs, and a coordinated client release — for fields that
  do not change a single rendered pixel.
- Fragment the cache keyspace if framing fields ever drift into composition
  inputs (the schema-bound surface is observable to caches).
- Complicate schema review by mixing UI taxonomy changes with transport-
  envelope changes in the same review surface.

The status quo before this ADR was a body-level `traceId` field on
`Screen` carried inside the schema. Correlation, however, is already
handled at the HTTP layer (request/response header), and adding a second
body-level home for it created two sources of truth and a wire-shape
contract that did not match the actual transport behavior.

## Decision Drivers

- The schema-as-contract rule is load-bearing for UI semantics; loosening
  it for transport framing risks future drift unless the exception is
  named, scoped, and bounded.
- Transport framing must evolve on a faster cadence than UI semantics;
  static-stub `meta` today, real partial-failure metadata in A2c, future
  staleness/freshness signals beyond that.
- Clients already need a thin transport adapter for header handling and
  correlation propagation; mirroring `{data, meta}` in that adapter is
  cheap and keeps renderer code unchanged.
- The codegen output should remain UI-focused so that schema review and
  generated-model diffs stay legible.

## Options Considered

### Option A: Put `ResponseEnvelope` and `ResponseMeta` in the schema (rejected)

Add the envelope and meta fields to `schema/sdui-schema.json` and let
codegen generate them on every platform.

Pros:

- One uniform rule (everything wire-bound is schema-bound).
- Static type guarantees on `meta` across all four platforms automatically.

Cons:

- Forces transport-framing iteration through the schema review and
  codegen pipeline, which is sized for UI taxonomy changes (months) rather
  than transport iteration (days).
- Bloats generated UI models with non-UI types.
- Risks drift if transport semantics ever cross into composition inputs.

### Option B: Hand-written transport envelope, schema-bound `data` (chosen)

Define `ResponseEnvelope<T>(T data, ResponseMeta meta)` and `ResponseMeta`
in server code at the controller edge. Hand-write `{data, meta}` wrappers
on each client. Inner `data` decodes through the existing generated
models.

Pros:

- Schema stays UI-focused; codegen output stays UI-focused.
- Transport framing iterates without coordinated codegen runs.
- The exception is small, named, and bounded by anti-scope rules.

Cons:

- Transport types are duplicated across four hand-written wrappers (server
  + three clients). Mitigated by the surface being tiny and stable
  (3 fields).

### Option C: No envelope; keep body `traceId` and add ad-hoc framing fields per client (rejected)

Continue carrying transport framing inline on `Screen` / `Section` payloads.

Pros:

- No new wrapper.

Cons:

- Re-introduces the original problem: transport framing inside the schema,
  with the codegen and review costs that come with it.
- No structural place for partial-failure metadata; future additions
  pollute the UI surface.

## Decision Outcome

Option B. The exception is granted to the **outer transport wrapper
only** and is documented in `AGENTS.md` §1.2 under the "Transport-framing
exception" subsection.

## Scope (what this exception permits)

- Exactly one outer `{data, meta}` wrapper at the controller edge.
- The `ResponseMeta` shape itself, hand-written, schema-shaped.
- Each client mirrors with a hand-written `{data, meta}` wrapper that
  unwraps `.data` and decodes-but-ignores `.meta` for now.
- Future additions to `ResponseMeta` (when freshness metadata lands).

## Anti-scope (what this exception forbids)

- **No nested envelopes.** Exactly one outer `{data, meta}`. A
  section-channel response is `{data: Section, meta: ResponseMeta}`,
  not `{data: {data: Section, meta: ...}, meta: ...}`.
- **No per-section frames.** Sections live inside `data` and remain
  schema-bound. The exception does not authorize a `{data, meta}` frame
  per section.
- **No ad-hoc fields outside `meta`.** Any new wire-framing addition
  (degradation flags, staleness, freshness, partial-failure) rides in
  `meta`. Anywhere else is a schema change.
- **Not a backdoor for un-schema'd UI fields.** The exception applies to
  the outermost transport wrapper only. Anything inside `.data` IS
  schema-bound and codegen-driven.

## Consequences

- Codegen output stays UI-focused. Generated models on Java, Kotlin,
  Swift, and TypeScript do not gain transport-framing types.
- Transport framing evolves separately from UI semantics. Adding a new
  `meta` field requires hand-edits to four wrappers (server + three
  clients) but no schema bump and no codegen run.
- The body-level `traceId` field on `Screen` is removed. Correlation
  lives in the `X-Correlation-ID` response header only.
- Each new wire-framing addition must ride in `meta`. PRs that add
  transport-framing fields anywhere other than `meta` (or that nest the
  envelope) fail review.
- Client renderers consume `.data` exactly as before; no renderer code
  changes from this ADR.

## Implementation Notes

- **Server:** `ResponseEnvelope` and `ResponseMeta` live in
  `com.nba.sdui.controller`. The SDUI controller wraps every screen-channel
  and section-channel response. Static-stub `meta` (Step 1, A2a) until
  partial-failure metadata lands in A2c.
- **Clients:** Each platform adds a hand-written transport wrapper that
  unwraps `.data` for the existing repository / renderer code path and
  decodes-but-ignores `.meta` for now. Wrapper lives next to the
  per-platform `SduiRepository` implementation.
- **Compatibility:** Hard cut. The body-level `traceId` field is removed
  from `Screen` in the same change that introduces the envelope. All
  platforms ship together; no external consumers depend on body
  `traceId`.

## Related

- `AGENTS.md` §1.2 — "Transport-framing exception" subsection
- `docs/specs/sdui-envelope-spec.md` — response body shape, header
  rename (`X-Correlation-ID`)
- `docs/contracts/server-implementors-contract.md` — envelope wrap at
  controller edge, header echo
- `docs/contracts/client-implementors-contract.md` — `.data` unwrap and
  decode-but-ignore `.meta`
- `docs/plans/server/plan-server-saf-codegen-port-readiness.md` — A2a
  (server + schema + codegen, this change), A2c (real `meta` values)
