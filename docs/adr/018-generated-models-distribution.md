# ADR-018: Generated Models Distribution — In-Tree Today, Packaged Tomorrow

- Status: Accepted
- Date: 2026-06-07
- Decision owners: Backend, Cross-Platform
- Related requirements: `docs/sdui-requirements-summary.md`
- Related ADRs: ADR-003 (Composition API Contract), ADR-017 (Transport-framing exception)

## Decision

Each client commits its own view of the generated models in its source tree
(`server/src/generated/java/`, `web/src/generated/SduiModels.ts`,
`ios/Sources/SduiCore/Models/SduiModels.swift`,
`android/sdui-core/src/main/java/com/nba/sdui/core/models/generated/SduiModels.kt`),
produced from `schema/sdui-schema.json` by `codegen/generate.sh`.

When triggers in *Reassessment criteria* are met, the JVM artifact (and only
the JVM artifact) graduates to a published Maven package
(`com.nba.sdui:sdui-models:<schemaVersion>`) on GitHub Packages. Non-JVM
targets stay in-tree until each one has its own packaging story
(SwiftPM / npm) and a real second consumer.

## Context

Per AGENTS.md §1.2, `schema/sdui-schema.json` is the wire contract; clients
are forbidden from hand-editing generated models. That doctrine leaves the
*transport* of those models open: the schema can reach a client's compiler
either as in-tree generated source or as a versioned binary artifact.

Until 2026-06-07 the server consumed Java POJOs through a Gradle composite
build (`includeBuild("../codegen")`). That broke the main-build CI workflow:
the Docker build context only ships `server/` + `schema/`, so the composite
reference (`Included build '/codegen' does not exist`) failed at Gradle
settings init. Switching the server to commit its generated tree
(`server/src/generated/java/`) fixed CI and aligned the server with the
pattern the other three clients already follow.

The fix is right for today, but it is not the long-run answer. As the
schema gains real version cadence and additional JVM consumers appear, a
published artifact becomes the better transport.

## Decision Drivers

- AGENTS.md §1.2 — schema is the wire contract; generated models are a
  per-client artifact, not a build-time side effect.
- Self-contained client builds — no client compile must reach into a
  sibling directory.
- Atomic schema↔code pairing — every commit pins a client's view of the
  schema to its hand-written code.
- Explicit version pinning — when a client needs a specific schema
  version, that should be visible in `build.gradle` / `Package.swift` /
  `package.json`, not implicit in a git SHA.
- Transport cost — publishing N artifacts (Maven, SwiftPM, npm) is only
  worth it when there is a real second consumer for each.

## Options Considered

### Option A: In-tree forever

Every client always commits its generated view of the schema. No
packaging step.

Pros:
- Zero infrastructure overhead.
- Reviews see the diff; bisect works cleanly.
- One discipline (`make codegen && git diff --exit-code` in CI) closes
  the drift loop.

Cons:
- Schema bumps require an N-client codegen sweep in one PR.
- A second JVM consumer (extracted service, second backend module)
  pays the full schema-bump cost twice.
- Version pinning is implicit in the git SHA — clients that want to
  pin "schema v1.4.0" cannot express that.

### Option B: Composite build (status quo before this ADR)

Server consumes `codegen/` via Gradle `includeBuild`; generated
sources live under `codegen/build/`.

Pros:
- No checked-in generated files; the build is always fresh.

Cons:
- Breaks any sealed build context (Docker, extracted module, fresh
  clone CI).
- Generated view is invisible at review time.
- Couples the server build graph to the codegen project layout.
- AGENTS.md §1.2 says generated models are "checked-in"; composite
  build contradicts that.

Rejected on 2026-06-07 — see commit `8eb9b62`.

### Option C: Published artifact for every target

Publish `sdui-models` to Maven (JVM), npm (web), SwiftPM (iOS), and
ship Kotlin via a Maven artifact (Android).

Pros:
- Explicit version pinning everywhere.
- Server, web, iOS, Android can independently pin schema versions.
- The codegen project becomes a real publisher with a version line.

Cons:
- Four registries to operate, four credential setups, four release
  pipelines.
- For a single consumer per target (today), the registry round-trip
  is pure overhead.
- SwiftPM / npm publishing for a one-consumer artifact is usually
  not worth doing.

### Option D: Hybrid — in-tree by default, packaged JVM when triggers fire

Stay in-tree everywhere today. Graduate only the JVM artifact to
GitHub Packages (`com.nba.sdui:sdui-models:<schemaVersion>`) when
the JVM consumer count or schema cadence warrants it. Non-JVM
targets stay in-tree until each one has its own packaging story.

Pros:
- Keeps the cheap path cheap.
- Adds publishing only where there is a second JVM consumer.
- Doctrine (schema is the wire contract; clients commit their
  generated view) survives unchanged — only the *transport* of the
  JVM view changes.
- One-line server change at graduation:
  `srcDir(...)` → `implementation("com.nba.sdui:sdui-models:1.4.0")`.

Cons:
- Asymmetry — JVM is packaged while web / iOS / Android stay in-tree.
- Requires deciding (and documenting) when to graduate.

## Evidence

- The `codegen/` Gradle project already has its own publishable surface
  — output package `com.nba.sdui.models.generated` does not depend on
  anything server-internal.
- The server consumes generated POJOs as a plain `srcDir(...)` today.
  Swapping that for `implementation("com.nba.sdui:sdui-models:1.4.0")`
  is a one-line change.
- SAF (`com.nba:service-aggregation-framework`) already uses GitHub
  Packages, so the publish/consume pattern is precedented in this
  repo's CI (`.github/workflows/main-build.yaml`,
  `.github/workflows/pr-check.yaml`).
- The four current clients each have one consumer of their generated
  view today.

## Decision Outcome

**Option D.**

Today: in-tree everywhere. The 2026-06-07 server migration to
`server/src/generated/java/` is the canonical state for all four
clients.

When triggers below fire for the JVM artifact, graduate it (and only
it) to GitHub Packages. Non-JVM targets re-evaluate independently
when their own triggers fire.

## Consequences

- Short term:
  - No infrastructure changes. The current in-tree pattern stays.
  - CI guard `make codegen && git diff --exit-code` (when added)
    catches contributors who change the schema without regenerating.
- Medium term:
  - When the JVM publishing trigger fires, expect a one-PR change:
    add `maven-publish` + `publishing` block to
    `codegen/build.gradle.kts`, swap the server's `srcDir(...)` for
    a versioned dependency, and delete `server/src/generated/java/`
    from the repo.
- Long term:
  - The schema version becomes a first-class part of every JVM
    consumer's dependency manifest.
  - Non-JVM targets may follow if their consumer count or version
    cadence warrants it; each is a separate graduation decision.

## Reassessment criteria

Graduate the JVM artifact when **any** of the following holds:

1. A second in-repo JVM module needs the generated POJOs (e.g. an
   extracted SDUI service, a second backend, a JVM-based codegen
   verifier).
2. The schema starts versioning explicitly (`schemaVersion` field
   bumps, semver tags on `schema/`) and consumers want to pin
   versions independently of any one git SHA.
3. The repo splits — the server, an extracted service, or codegen
   itself moves to its own repo and needs to consume the schema
   without a sibling-directory checkout.

For non-JVM targets, the same triggers apply per-target with the
relevant registry (npm for web, SwiftPM for iOS, Maven for Android).

## Implementation Notes

- Today's wiring (`server/build.gradle.kts`, `codegen/build.gradle.kts`,
  `codegen/generate.sh`) is the canonical in-tree shape. Do not
  reintroduce `includeBuild("../codegen")` or move generated files
  back under `codegen/build/`.
- When graduating the JVM artifact:
  - Add `maven-publish` and a `publishing { repositories { maven {
    name = "GitHubPackages-SDUI"; url =
    "https://maven.pkg.github.com/NBA/sdui-prototype" } } }` block to
    `codegen/build.gradle.kts`.
  - Wire the publish step into a dedicated workflow that fires only
    on schema-touching tags (not every PR — drift between SHA and
    artifact is the failure mode to avoid).
  - Replace `server/src/generated/java/` with a versioned dependency
    in `server/build.gradle.kts`. Repository setup mirrors SAF
    (`server/settings.gradle.kts`).
  - Delete the in-tree `server/src/generated/java/` tree in the same
    PR that introduces the dependency.
- Avoid `-SNAPSHOT` consumption from any non-development client.
  Snapshots reintroduce the silent-drift problem the in-tree shape
  currently solves.
- Do not graduate non-JVM targets opportunistically. Each non-JVM
  graduation is its own ADR-update.

## Open Questions

- Is the GitHub Packages registry the right home long-term, or should
  the schema artifact eventually live in an internal Artifactory /
  Nexus mirror? (Deferred — same question as SAF's registry choice.)
- Should the schema itself ship as a JSON-only artifact alongside the
  Java POJOs, so non-JVM clients can also pin a version? (Deferred
  until non-JVM consumer counts justify it.)

## Follow-ups

- [ ] Add a CI step on schema-touching PRs:
      `make codegen && git diff --exit-code` to catch missed
      regeneration.
- [ ] Add the Docker build (`server/Dockerfile`) to PR checks so the
      composite-build-style break that motivated this ADR cannot
      sneak past pre-merge again.
- [ ] When trigger 1, 2, or 3 fires for the JVM artifact, open a
      PR that implements the *Implementation Notes* graduation steps
      and update this ADR's status to *Accepted, partially superseded*.
