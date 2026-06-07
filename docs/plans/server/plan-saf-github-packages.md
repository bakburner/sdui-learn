# Plan: Resolve SAF from GitHub Packages instead of vendored Maven Local

## Background

SAF (`com.nba:service-aggregation-framework:1.0.0-SNAPSHOT`) is consumed by
every module in this repo (`integration-models`, `integration-clients`,
`integration-clients-test-mock`, `server:cas-core`, `server:wecs-core`,
`server:nba-client-backend-server`). Today it is resolved via `mavenLocal()`
and a two-step pre-build dance:

1. `make publish-saf` → clones / cds into `../service-aggregation-framework`,
   runs `./gradlew publishToMavenLocal` → writes
   `~/.m2/repository/com/nba/service-aggregation-framework/1.0.0-SNAPSHOT/…`.
2. `make sync-saf` → `cp -r ~/.m2/repository/com/nba/service-aggregation-framework
   nba-client-backend/.m2/repository/com/nba/` so the Dockerfile's
   `COPY .m2/repository/com/nba /root/.m2/repository/com/nba` has something to
   copy.

Both `up-test-profile` and `up-no-profile` depend on `sync-saf` as a Make
prerequisite. The in-tree `.m2/` directory is gitignored, so a fresh CI
checkout has no `.m2/` at all and the Dockerfile build is currently brittle
in any context where `make sync-saf` has not run.

SAF now publishes to GitHub Packages (`maven.pkg.github.com/NBA/saf`,
auth via `GITHUB_ACTOR` / `GITHUB_TOKEN` or `gpr.user` / `gpr.key`). That
removes the prerequisite: any consumer with a PAT scoped for
`read:packages` can resolve the SNAPSHOT directly. This plan switches
nba-client-backend onto that resolution path while keeping `mavenLocal()`
as an opt-in fast path for SAF iteration.

## Goals

- CI and Docker builds resolve SAF straight from GitHub Packages with no
  vendored `.m2/` and no `publish-saf`/`sync-saf` precondition.
- Local non-Docker builds keep working through `mavenLocal()` for anyone
  actively iterating on SAF source — `./gradlew publishToMavenLocal` in
  the SAF repo still shortcircuits the round-trip.
- Repository declaration is centralized in `settings.gradle`
  (`dependencyResolutionManagement { repositories { … } }`) so per-module
  `mavenLocal()` lines disappear.
- The `com.nba:service-aggregation-framework:1.0.0-SNAPSHOT` coordinate
  stays the same — no source code changes in any module.

## Non-goals

- Pinning to a non-SNAPSHOT SAF release. (When SAF cuts a stable
  `1.0.0` or higher, that is a separate version-bump PR.)
- Migrating other repos (sdui-prototype, etc.) — they can adopt the
  same pattern but are out of scope here.
- Removing IntelliJ's external Gradle link to SAF for developers who
  rely on it during SAF authoring. That stays a local IDE choice;
  this plan only touches build wiring.

## Constraints / risks to manage

- **SNAPSHOT freshness.** GH Packages SNAPSHOTs are timestamped; without
  `--refresh-dependencies` Gradle will cache them for up to 24h. For a
  module under active development that can mask "did my SAF change
  actually land". Mitigation: document the `--refresh-dependencies`
  habit; optionally configure a shorter `cacheChangingModulesFor` for
  the SAF coordinate only.
- **Secret plumbing in Docker.** The Dockerfile build currently has no
  network credentials. Gradle inside the build container must reach
  `maven.pkg.github.com` and authenticate. Solution: BuildKit secret
  mounts (`--secret id=gpr_user`, `--secret id=gpr_token`) consumed
  inside the `RUN ./gradlew bootJar` line. Compose drives that via the
  `secrets:` block or by passing `--secret id=…,env=…` through
  `docker compose build`.
- **PAT distribution for devs.** Every developer needs a GitHub PAT
  with `read:packages` scope (or the org SSO equivalent) in either
  `~/.gradle/gradle.properties` (`gpr.user=` / `gpr.key=`) or shell env
  (`GITHUB_ACTOR` / `GITHUB_TOKEN`). Document this once in README +
  `DOCKER.md`.
- **First-run failure mode for new devs.** Without a PAT, all Gradle
  builds will fail at dependency resolution. Surface a clear error in
  README's "Quick start" section so it's the first thing a new dev
  reads.

## Work items

### 1. Centralize repositories in `settings.gradle`

- Add `dependencyResolutionManagement` block declaring, in order:
  1. `mavenLocal()` (for SAF dev iteration; harmless when empty)
  2. GitHub Packages for `com.nba:service-aggregation-framework`,
     scoped via `content { includeModule('com.nba',
     'service-aggregation-framework') }`
  3. `mavenCentral()`
- Set `repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS` so a
  stray `repositories { … }` in any module surfaces as an error rather
  than silently shadowing the central config.
- Credentials read from `gpr.user`/`gpr.key` first, falling back to
  `GITHUB_ACTOR`/`GITHUB_TOKEN`, matching SAF's own publish block.

### 2. Strip per-module `repositories` blocks

Files to clean up (each currently declares `mavenLocal()` + sometimes
`mavenCentral()`):

- `build.gradle` (root) — keep only `mavenCentral()` inside the
  `subprojects { repositories … }` block, or remove the block entirely
  once `dependencyResolutionManagement` is authoritative.
- `integration-models/build.gradle`
- `integration-clients/build.gradle`
- `integration-clients-test-mock/build.gradle`
- `server/cas-core/build.gradle`
- `server/wecs-core/build.gradle`
- `server/nba-client-backend-server/build.gradle`

Dependency coordinates (`com.nba:service-aggregation-framework:1.0.0-SNAPSHOT`)
stay exactly as they are.

### 3. Docker build: drop vendored `.m2/` copy, add BuildKit secret mount

`server/nba-client-backend-server/Dockerfile`:

- Delete `COPY .m2/repository/com/nba /root/.m2/repository/com/nba`.
- Change the build line to mount the PAT as a secret and pass it to
  Gradle:
  ```dockerfile
  # syntax=docker/dockerfile:1.4  (already supported by buildx/compose)
  RUN --mount=type=secret,id=gpr_user --mount=type=secret,id=gpr_token \
      GPR_USER="$(cat /run/secrets/gpr_user)" \
      GPR_TOKEN="$(cat /run/secrets/gpr_token)" \
      ./gradlew :server:nba-client-backend-server:bootJar \
        -Pgpr.user="$GPR_USER" -Pgpr.key="$GPR_TOKEN" \
        -x test --no-daemon
  ```
- Confirm the `# syntax=` directive is present (or rely on buildx
  default).

### 4. Compose wiring for the secret

Both `docker-compose.profile-test-local-infra.yml` and
`docker-compose.no-profile-local-infra.yml`:

- Add a top-level `secrets:` block pointing at env vars from `.env`:
  ```yaml
  secrets:
    gpr_user:
      environment: GITHUB_ACTOR
    gpr_token:
      environment: GITHUB_TOKEN
  ```
- On the service that does `build:`, add:
  ```yaml
  build:
    context: .
    dockerfile: server/nba-client-backend-server/Dockerfile
    secrets:
      - gpr_user
      - gpr_token
  ```
- Document the two required env vars in `.env.example` (and the README's
  Docker section).

### 5. `.dockerignore` defensive entry

Add `.m2/` to `.dockerignore` so a stray local `.m2/` never bleeds
into a build context after the explicit `COPY` line is gone.

### 6. Makefile cleanup

- Remove `sync-saf` from `up-test-profile` / `up-no-profile`
  prerequisites.
- Drop the `sync-saf` target entirely (its only consumer was the two
  `up-*` targets).
- Keep `publish-saf` as a dev convenience for the SAF-iteration path,
  but rename to `publish-saf-local` and add a help line clarifying it
  is only needed when consuming an unpublished SAF SNAPSHOT through
  `mavenLocal()`.
- Update the `help` text accordingly.

### 7. Delete the vendored Maven Local tree

```
nba-client-backend/.m2/
```

It is gitignored, so this is a local-filesystem cleanup only — no
commit changes. Mention in the PR description so other devs know to
remove their copy.

### 8. Documentation sweep

- `README.md` — replace the "publish-saf / sync-saf / Maven Local"
  section with a short "GitHub Packages setup" section: PAT scope,
  where to put credentials (`~/.gradle/gradle.properties` is the
  recommended path), and the `--refresh-dependencies` tip for picking
  up new SAF SNAPSHOTs.
- `DOCKER.md` — explain the BuildKit secret mounts and the two env
  vars expected in `.env`.
- `AGENTS.md` — update the Make-targets line that currently mentions
  `publish-saf | sync-saf` to reflect the new shape.

### 9. CI pipeline

(Out of immediate scope — flag for the pipeline owner.) The CI build
needs `GITHUB_ACTOR` + `GITHUB_TOKEN` exposed to whatever step calls
`docker buildx build`/`docker compose build`. If CI runs `./gradlew`
directly outside Docker, same two env vars must be set in the runner
environment.

## Validation

After the change:

1. **Cold-machine simulation.** From a fresh clone, with no
   `~/.m2/repository/com/nba/service-aggregation-framework`, with only
   `GITHUB_ACTOR` + `GITHUB_TOKEN` set:
   - `./gradlew test` succeeds across all six modules.
   - `make up-test-profile` builds the Docker image and starts the
     stack; `curl http://localhost:8083/actuator/health` returns UP.
   - `make up-no-profile` (with `.env` populated) does the same.
2. **SAF-iteration path still works.** From a state where SAF has a
   new method published to `mavenLocal()` only (not yet pushed to
   GH Packages): `./gradlew --refresh-dependencies test` resolves the
   local SNAPSHOT and consumers compile.
3. **Wrong/absent credentials fail loudly.** Unsetting `GITHUB_TOKEN`
   produces a clear "401 Unauthorized" or "Could not resolve" error,
   not a silent fallback to a stale cached SNAPSHOT.
4. **`smoke-personas.sh` still passes** at whatever level it passes
   today (this change does not touch persona data).

## Sequencing

```
1. settings.gradle dependencyResolutionManagement       (1 file)
2. strip per-module repositories blocks                 (7 files)
   → ./gradlew test (local, with ~/.m2 SAF still present)
3. delete local .m2/                                     (local cleanup)
   → ./gradlew --refresh-dependencies test (resolves from GH Packages)
4. Dockerfile: drop COPY, add secret mount               (1 file)
5. docker-compose: add secrets blocks                    (2 files)
6. .dockerignore: add .m2/                               (1 file)
   → make up-test-profile + make up-no-profile (local Docker check)
7. Makefile cleanup                                      (1 file)
8. README + DOCKER.md + AGENTS.md sweep                  (3 files)
9. CI pipeline note (handed off, not done here)
```

Steps 1–3 are the smallest reversible chunk that lets a local
non-Docker build prove GH Packages resolution works end-to-end. Steps
4–6 are the Docker chunk; step 7 is the convenience cleanup; step 8
is the doc sweep.

## Dependencies

- A working GitHub PAT with `read:packages` for `NBA/saf`. Without
  this the plan cannot be validated locally.
- SAF SNAPSHOT actually present in GH Packages (already true per
  conversation context).

## Out of scope (recorded as follow-up tickets)

- IDE-side cleanup. `.idea/` still references SAF as an external
  Gradle project for developers who want SAF source navigation. If
  the team wants the IDE to stop loading SAF as a sibling module,
  that's a separate "decouple IntelliJ project layout from SAF
  source" task.
- Switching from SNAPSHOT to a versioned release. Worth doing once
  SAF stabilizes its API surface; until then SNAPSHOT is the
  pragmatic shape.
- Publishing this repo's own libraries (`integration-models`,
  `integration-clients`) to GH Packages. They are consumed in-process
  today; only relevant if/when modules extract to standalone services
  (ADR-001 open question).
