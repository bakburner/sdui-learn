# Plan: NL-Driven Composite Prototyping

> Source requirements: Task spec (NL-driven composite prototyping), AGENTS.md §1.2 (schema as contract), §3.3 (server authority)

## Status

> **Not started.** All phases are greenfield.

## Summary

Add a non-production admin interface that lets developers author AtomicComposite
sections via natural language prompts, backed by AWS Bedrock. Composites flow
through a `CompositeRegistry` abstraction that the existing composition pipeline
reads from. Production deployments use a read-only registry backed by the
existing compiled composers; non-prod deployments add a mutable in-memory
registry and admin REST endpoints. The NL layer translates English prompts into
validated AtomicComposite JSON using the schema + atom catalog as context.

## Current State

| Aspect | Status | Notes |
|--------|--------|-------|
| CompositeRegistry abstraction | Gap | Composers build sections inline; no registry indirection exists |
| Admin endpoints | Gap | No admin controller, no `@Profile` gating, no `AdminAuthFilter` |
| Schema validation (server-side) | Gap | No `atomic-composite.schema.json` in server resources; schema lives in `schema/sdui-schema.json` at repo root |
| NL translation layer | Gap | No Bedrock integration; no `BedrockClient` bean found |
| Tests | Gap | Existing tests are unit-level (`AtomicCompositeBuilderFeedModulesTest`, `SduiRefreshTransportTest`, `LayoutTokenRegistryTest`) — no admin or registry tests |

## Assumptions Surfaced From Codebase

1. **No `AtomicComposite` POJO exists.** Composites are `ObjectNode` trees built
   imperatively by `AtomicCompositeBuilder` (2760 LOC). The "class" is the
   builder; the wire shape is `{ id, type:"AtomicComposite", data: { ui: AtomicElement } }`.
   The registry will store `JsonNode` (ObjectNode), not a typed POJO.

2. **No existing admin infrastructure.** No `@Profile` annotations, no
   `AdminAuthFilter`, no admin endpoints anywhere in the server. All must be
   created from scratch.

3. **No `BedrockClient` bean.** AWS SDK is not in `build.gradle.kts`. The
   `BedrockNlTranslator` implementation will stub the client with a TODO.

4. **Schema source of truth** is `schema/sdui-schema.json` (repo root). The
   `AtomicCompositeData` definition (line 582) and `AtomicElement` definition
   are there. The server copies token JSONs into classpath at build time via
   `processResources` in `build.gradle.kts` — the same pattern can copy the
   schema for runtime validation.

5. **Composition is screen-level.** `SduiCompositionService` delegates to
   per-screen composers (`GameDetailComposer`, `ForYouComposer`, etc.) which
   call `AtomicCompositeBuilder` methods. There is no resolver that looks up
   composites by name — composers build them imperatively. The registry
   integration point is `SduiCompositionService`, which can optionally resolve
   a section from the registry before falling through to compiled composers.

6. **Package conventions:** `com.nba.sdui.service` (composers, builders),
   `com.nba.sdui.controller` (REST), `com.nba.sdui.config` (Spring config),
   `com.nba.sdui.request` (request envelope). New packages will follow this
   pattern: `com.nba.sdui.prototyping` (registry, admin), `com.nba.sdui.prototyping.nl` (NL layer).

7. **Test conventions:** JUnit Jupiter, Mockito, `@WebMvcTest` for controller
   tests. No base test class — tests are standalone. Test resources live under
   `server/src/test/resources/schema/`.

## Requirements Addressed

- [ ] **REQ-1**: `CompositeRegistry` interface with get/put/remove/list/version keyed by NRN
- [ ] **REQ-2**: `InMemoryCompositeRegistry` — `@Profile("!prod")`, ConcurrentHashMap-backed
- [ ] **REQ-3**: `ReadOnlyCompositeRegistry` — `@Profile("prod")`, delegates to compiled artifacts
- [ ] **REQ-4**: Wire existing composition pipeline to read through registry
- [ ] **REQ-5**: Admin endpoints (PUT/GET/DELETE composites, POST preview, POST validate) gated by `@Profile("!prod")` + `sdui.admin.enabled`
- [ ] **REQ-6**: `AdminAuthFilter` stub with TODO
- [ ] **REQ-7**: Schema validation of incoming specs before registry writes
- [ ] **REQ-8**: `NlToSpecTranslator` interface + `BedrockNlTranslator` (stub client)
- [ ] **REQ-9**: NL endpoint (POST /admin/composites/nl) — returns spec + preview, no auto-commit
- [ ] **REQ-10**: Tests — registry semantics, admin auth/validation/preview isolation, NL prompt structure, prod profile 404s
- [ ] **REQ-11**: `docs/sdui-prototyping.md` documentation
- [ ] **REQ-12**: End-to-end integration test (NL prompt → spec → preview → commit)

## File Plan

### Phase 1: CompositeRegistry Abstraction

New files:

- `server/src/main/java/com/nba/sdui/prototyping/CompositeRegistry.java`
  - Interface: `get(String nrn) → Optional<JsonNode>`, `put(String nrn, JsonNode spec)`, `remove(String nrn) → boolean`, `list(String namespace) → Map<String, JsonNode>`, `version(String nrn) → long`
  - NRN format validation: `nrn:nba:composite:<name>-<version>`

- `server/src/main/java/com/nba/sdui/prototyping/InMemoryCompositeRegistry.java`
  - `@Component @Profile("!prod")`
  - `ConcurrentHashMap<String, RegistryEntry>` where `RegistryEntry` holds `JsonNode spec` + `long version` (AtomicLong counter)
  - `list(namespace)` filters by NRN prefix matching

- `server/src/main/java/com/nba/sdui/prototyping/ReadOnlyCompositeRegistry.java`
  - `@Component @Profile("prod")`
  - `get()` returns empty (compiled composers don't register by NRN — this is the forward-compatible seam)
  - `put()` / `remove()` throw `UnsupportedOperationException`
  - `list()` returns empty map
  - `version()` returns -1

Modified files:

- `server/src/main/java/com/nba/sdui/service/SduiCompositionService.java`
  - Inject `CompositeRegistry` (optional — `@Autowired(required = false)`)
  - No behavioral change in this phase. The registry exists but is not consulted yet. Phase 1 is structural only.

### Phase 2: Registry Read-Through Integration

Modified files:

- `server/src/main/java/com/nba/sdui/service/SduiCompositionService.java`
  - Add a package-private method `resolveFromRegistry(String sectionId) → Optional<JsonNode>` that checks the registry
  - Composers that build named sections (e.g. `buildErrorState`, `buildSectionHeader`) can optionally check the registry for an NRN-matched override before building from scratch
  - **Constraint**: this is read-only; no mutation of the production compose path. If the registry returns empty, the existing compiled path runs unchanged.

### Phase 3: Schema Validation

New files:

- `server/src/main/java/com/nba/sdui/prototyping/CompositeSchemaValidator.java`
  - `@Component`
  - Loads schema from classpath (`schemas/atomic-composite.schema.json`) at init
  - `validate(JsonNode spec) → ValidationResult` where `ValidationResult` has `boolean valid`, `List<ValidationError> errors`
  - `ValidationError`: `String path`, `String message`, `String expected`, `String actual`
  - Uses `com.networknt:json-schema-validator` (lightweight, already Jackson-based) — **new dependency, flagged**

- `server/src/main/resources/schemas/atomic-composite.schema.json`
  - **Generated starter** extracted from `schema/sdui-schema.json` definitions `AtomicCompositeData` + `AtomicElement` + `Section` (the subset needed to validate a complete AtomicComposite section envelope)
  - TODO flag for manual review

Modified files:

- `server/build.gradle.kts`
  - Add `implementation("com.networknt:json-schema-validator:1.4.0")` — **flagged dependency**
  - Add schema copy to `processResources` (same pattern as token JSONs):
    ```kotlin
    from("${projectDir}/../schema") {
        include("sdui-schema.json")
        into("schemas")
    }
    ```

### Phase 4: Admin Endpoints

New files:

- `server/src/main/java/com/nba/sdui/prototyping/AdminCompositeController.java`
  - `@RestController @Profile("!prod") @ConditionalOnProperty("sdui.admin.enabled")`
  - `PUT /admin/composites/{nrn}` — validate spec → put in registry → return 200 with stored spec
  - `GET /admin/composites/{nrn}` — return spec or 404
  - `GET /admin/composites?ns={ns}` — list by namespace
  - `DELETE /admin/composites/{nrn}` — remove from registry → return 204 or 404
  - `POST /admin/composites/preview` — body `{ spec, sampleContext }` → render SDUI tree via `AtomicCompositeBuilder` without registry write → return rendered tree
  - `POST /admin/composites/validate` — body: AtomicComposite JSON → return `{ valid, errors }`

- `server/src/main/java/com/nba/sdui/prototyping/AdminAuthFilter.java`
  - `@Component @Profile("!prod") @ConditionalOnProperty("sdui.admin.enabled")`
  - `OncePerRequestFilter` that checks requests to `/admin/**`
  - **Stub implementation**: TODO — currently passes all requests. Log a warning on every request that auth is not enforced.

- `server/src/main/java/com/nba/sdui/prototyping/PreviewRenderer.java`
  - Takes a raw `JsonNode` spec and a `Map<String, Object> sampleContext`
  - Wraps the spec in a minimal screen envelope (`{ screenId, sections: [spec] }`) for preview
  - Applies `sampleContext` values to any `bindRef` paths in the tree
  - Returns the rendered SDUI tree without side effects

- `server/src/main/java/com/nba/sdui/prototyping/dto/PreviewRequest.java`
  - `JsonNode spec`, `Map<String, Object> sampleContext`

- `server/src/main/java/com/nba/sdui/prototyping/dto/ValidationResponse.java`
  - `boolean valid`, `List<ValidationError> errors`

- `server/src/main/java/com/nba/sdui/prototyping/dto/NlRequest.java`
  - `String prompt`, `String namespace`, `Map<String, Object> sampleContext`

- `server/src/main/java/com/nba/sdui/prototyping/dto/NlResponse.java`
  - `JsonNode spec`, `JsonNode preview`, `List<ValidationError> validationErrors`

Modified files:

- `server/src/main/resources/application.yml`
  - Add: `sdui.admin.enabled: false` (default off)

### Phase 5: NL Translation Layer

New files:

- `server/src/main/java/com/nba/sdui/prototyping/nl/NlToSpecTranslator.java`
  - Interface: `translate(String prompt, NlContext ctx) → SpecResult`
  - `NlContext`: `String namespace`, `JsonNode schema`, `List<AtomicElementSignature> atomCatalog`, `Map<String, Object> sampleContext`
  - `SpecResult`: `JsonNode spec`, `boolean valid`, `List<ValidationError> errors`

- `server/src/main/java/com/nba/sdui/prototyping/nl/NlContext.java`
  - Context object carrying schema, atom catalog, namespace

- `server/src/main/java/com/nba/sdui/prototyping/nl/SpecResult.java`
  - Result wrapper: spec + validation outcome

- `server/src/main/java/com/nba/sdui/prototyping/nl/BedrockNlTranslator.java`
  - `@Component @Profile("!prod") @ConditionalOnProperty("sdui.admin.enabled")`
  - Constructor injects `CompositeSchemaValidator`, `ObjectMapper`, schema resource
  - `translate()` implementation:
    1. Build system prompt: "You are an SDUI composer. Output valid JSON only. No markdown fences. No explanation."
    2. Build user prompt: inject (a) the `AtomicCompositeData` + `AtomicElement` JSON schema subset, (b) atom catalog (type names + prop signatures from the schema `AtomicElement.properties`), (c) the user's natural language prompt
    3. Call `BedrockClient.invokeModel()` — **stub**: TODO, returns a hardcoded sample composite for now
    4. Parse response as JSON; on parse failure, return structured error
    5. Run `CompositeSchemaValidator.validate()` on parsed JSON
    6. Return `SpecResult` with spec + validation outcome

- `server/src/main/java/com/nba/sdui/prototyping/nl/AtomCatalogBuilder.java`
  - Reads `AtomicElement` definition from schema
  - Extracts the set of valid `type` values and their associated property signatures
  - Produces a compact catalog string for LLM prompt injection
  - **V1**: full catalog injection. TODO: follow-up for lazy/filtered catalog when atom count grows large

- `server/src/main/java/com/nba/sdui/prototyping/nl/BedrockClientStub.java`
  - `@Component @Profile("!prod") @ConditionalOnProperty("sdui.admin.enabled")`
  - Stub bean — logs invocation, returns a canned response
  - TODO: replace with real `software.amazon.awssdk:bedrockruntime` integration

### Phase 6: NL Endpoint

Modified files:

- `server/src/main/java/com/nba/sdui/prototyping/AdminCompositeController.java`
  - Add `POST /admin/composites/nl` — body `NlRequest { prompt, namespace, sampleContext? }`
  - Calls `NlToSpecTranslator.translate()` with assembled `NlContext`
  - Calls `PreviewRenderer` with the generated spec + sampleContext
  - Returns `NlResponse { spec, preview, validationErrors? }`
  - **Does NOT auto-commit**. Caller reviews and PUTs via the existing endpoint.

### Phase 7: Tests

New files:

- `server/src/test/java/com/nba/sdui/prototyping/InMemoryCompositeRegistryTest.java`
  - get/put/remove semantics
  - list by namespace filtering
  - version counter increments on put
  - get after remove returns empty
  - NRN format validation rejects malformed keys

- `server/src/test/java/com/nba/sdui/prototyping/CompositeSchemaValidatorTest.java`
  - Valid AtomicComposite spec passes
  - Missing required `data.ui` fails with structured error
  - Unknown AtomicElement type fails
  - Errors include path, message, expected, actual

- `server/src/test/java/com/nba/sdui/prototyping/AdminCompositeControllerTest.java`
  - `@WebMvcTest(AdminCompositeController.class)` with `@ActiveProfiles("dev")`
  - PUT → GET round-trip
  - PUT with invalid spec returns 400 + validation errors
  - POST /preview does not mutate registry (PUT count before == after)
  - POST /validate returns structured errors
  - DELETE returns 204 / 404
  - Auth filter is invoked (mock `AdminAuthFilter`, verify filter chain)

- `server/src/test/java/com/nba/sdui/prototyping/AdminEndpointsProdProfileTest.java`
  - `@SpringBootTest @ActiveProfiles("prod")`
  - All `/admin/**` routes return 404

- `server/src/test/java/com/nba/sdui/prototyping/nl/BedrockNlTranslatorTest.java`
  - Mock `BedrockClientStub` to return controlled JSON
  - Verify prompt structure includes schema subset + atom catalog + user prompt
  - Verify JSON-only parsing (reject markdown-fenced responses)
  - Verify validation runs before `SpecResult` is returned
  - Verify validation errors propagate on malformed LLM output

- `server/src/test/java/com/nba/sdui/prototyping/NlCompositeIntegrationTest.java`
  - `@SpringBootTest @ActiveProfiles("dev")` with `sdui.admin.enabled=true`
  - End-to-end: POST /admin/composites/nl → receive spec + preview → PUT /admin/composites/{nrn} → GET confirms committed
  - Uses stubbed Bedrock client (returns a known-valid composite)

Test resources:

- `server/src/test/resources/fixtures/valid-atomic-composite.json` — minimal valid AtomicComposite section
- `server/src/test/resources/fixtures/invalid-atomic-composite-missing-ui.json` — spec missing `data.ui`
- `server/src/test/resources/fixtures/invalid-atomic-composite-bad-type.json` — unknown element type

### Phase 8: Documentation

New files:

- `docs/sdui-prototyping.md`
  - How to enable: set `spring.profiles.active=dev` + `sdui.admin.enabled=true`
  - NL loop walkthrough: prompt → Bedrock → spec → validate → preview → commit
  - Security posture: admin endpoints are non-prod only, auth filter is TODO stub, all state is in-memory and ephemeral
  - How to disable for prod: `@Profile("prod")` excludes all admin beans; `ReadOnlyCompositeRegistry` rejects mutations
  - Follow-up notes: persistent registry (Redis/DB), real Bedrock client, lazy atom catalog, multi-user versioning

## Dependency Audit

| Dependency | Status | Justification |
|------------|--------|---------------|
| `com.networknt:json-schema-validator` | **New — flagged** | JSON Schema validation against `AtomicCompositeData` definition. Lightweight, Jackson-native. Alternative: hand-rolled validation (not recommended — schema is complex). |
| `software.amazon.awssdk:bedrockruntime` | **Deferred** | Stubbed in v1. Real integration is a follow-up. No dependency added now. |
| Spring Boot, Jackson, OkHttp | Existing | Already in `build.gradle.kts` |

## Constraints Checklist

- [ ] Production read paths unchanged beyond optional registry injection
- [ ] No new dependencies added without flag (json-schema-validator flagged above)
- [ ] All admin functionality removable via profile + config flag
- [ ] Package conventions followed (`com.nba.sdui.prototyping`, `com.nba.sdui.prototyping.nl`)
- [ ] AtomicRouter and upstream request handling untouched

## Open Questions

- [ ] Should the `ReadOnlyCompositeRegistry` eventually index compiled composer outputs by NRN so `get()` works in prod for read-only lookups? (Current plan: returns empty; compiled artifacts are not registered by name.)
- [ ] Should `InMemoryCompositeRegistry` support TTL or max-entry limits to prevent unbounded memory growth in long-running dev sessions?
- [ ] What Bedrock model ID should the real implementation target? (Claude 3 Sonnet is likely, but needs confirmation.)
- [ ] Should the atom catalog be versioned alongside schema version, or is a single catalog sufficient for v1?
- [ ] For large atom catalogs, should v2 implement filtered/lazy injection (e.g., LLM asks "what atoms exist for layout?" and only those get injected)?

## Out of Scope

- Production registry persistence (Redis/DB-backed) — interface only; noted as follow-up in `ReadOnlyCompositeRegistry`
- Multi-user collaboration / spec versioning beyond simple `version()` counter
- UI for the NL interface — backend only
- Real AWS Bedrock integration — stubbed; follow-up task
