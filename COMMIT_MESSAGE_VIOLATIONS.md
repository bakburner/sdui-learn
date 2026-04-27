Fix architectural violations and sync documentation

Correct three server-authority violations identified during a styling
audit, add the corresponding schema/server changes, and sync docs to
reflect the current renderer count and atomic element inventory.

--- Violation 1: Branded fallback image (§3.2) ---

- Delete dead `SduiImageDefaults.kt` (hardcoded LOGOMAN_URL CDN link).

--- Violation 2: Client-owned bootstrap URI (§3.1) ---

- Add `/sdui/init` server endpoint returning bootstrapUri + schemaVersion.
- All three clients fetch on launch; local constant renamed to
  `FALLBACK_BOOTSTRAP_URI` (degraded-connectivity only).

--- Violation 3: Branded error boundary styling (§8.0) ---

- Add `retryLabel` field to ErrorState in the schema; run codegen.
- Neutralize all three error boundaries: remove branded colors/icons,
  use platform-neutral surface tokens, read `retryLabel` from payload.

--- Documentation sync ---

- README, Executive Summary, Technical Proposal, Requirements Summary,
  Client Implementors Contract: update atomic element count (11→12),
  permanent section renderer count (9→8), add OverlayContainer.
- Sync agent and skill prompt files to match current architecture.
- Add `docs/glossary.md`.
