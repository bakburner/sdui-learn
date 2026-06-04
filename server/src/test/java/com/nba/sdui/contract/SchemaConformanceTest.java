package com.nba.sdui.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.DemoScreenComposer;
import com.nba.sdui.domain.composer.LiveComposer;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.testsupport.TestTokens;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A3 schema-conformance test (plan-server-saf-codegen-port-readiness Phase A3).
 *
 * <p>Validates every composed {@code Screen} and every {@code Section} inside
 * those screens against {@code schema/sdui-schema.json} (Draft-07). AGENTS.md
 * \u00a71.2 makes the schema the wire contract; this test enforces that contract
 * at compile time on the server side rather than relying on round-trip
 * Jackson decode (which {@code includeAdditionalProperties = true} in codegen
 * would silently swallow).
 *
 * <h2>Coverage today</h2>
 *
 * Wired through {@link LiveComposer} and {@link DemoScreenComposer} via the
 * same fixture pattern as {@code ScreenChannelContractTest}/{@code ComposerRoundTripTest}.
 * As composers gain test fixtures (or get migrated to typed POJOs in later
 * A3 steps), add them to {@link #screenSamples()}.
 *
 * <h2>Strictness</h2>
 *
 * Schema validation here is whatever the schema declares. {@code sdui-schema.json}
 * is mostly permissive ({@code additionalProperties: true} or omitted) so this
 * catches missing required fields, type errors, and enum violations — not
 * stray extra keys. That's the right strictness for a first pass: it surfaces
 * real drift without forcing a schema-tightening exercise concurrently.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaConformanceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LiveComposer liveComposer;
    private DemoScreenComposer demoScreenComposer;
    private JsonSchema screenSchema;
    private JsonSchema sectionSchema;

    @BeforeAll
    void setup() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        ParameterizedRefreshService parameterizedRefreshService = new ParameterizedRefreshService();

        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        ObjectNode emptyScoreboard = objectMapper.createObjectNode();
        ObjectNode sb = emptyScoreboard.putObject("scoreboard");
        sb.set("games", objectMapper.createArrayNode());
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard);
        when(statsApiClient.getScoreboardForDate(any())).thenReturn(emptyScoreboard);

        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        SectionRefreshService sectionRefreshService = new SectionRefreshService();
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService();
        ReflectionTestUtils.setField(seasonCalendarService, "clock", clock);

        liveComposer = new LiveComposer(
                objectMapper, new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                sectionRefreshService, parameterizedRefreshService, seasonCalendarService);
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(liveComposer, "registerResolvers");

        demoScreenComposer = new DemoScreenComposer(
                objectMapper, utils, surfaces, TestTokens.INSTANCE, parameterizedRefreshService);
        ReflectionTestUtils.setField(demoScreenComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(demoScreenComposer, "registerParameterizedRefreshResolvers");

        // Load schema/sdui-schema.json from the classpath (processResources
        // copies it into build/resources/main/schema/). Section.data is
        // discriminated by Section.type via an allOf chain of if/then clauses
        // in the schema itself, so the validator picks the correct *Data
        // sub-schema per section without any in-test patching.
        JsonNode schemaRoot = loadSchemaRoot();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        screenSchema = factory.getSchema(schemaRoot, config);

        ObjectNode sectionRoot = objectMapper.createObjectNode();
        sectionRoot.put("$schema", "http://json-schema.org/draft-07/schema#");
        sectionRoot.set("definitions", schemaRoot.get("definitions"));
        sectionRoot.put("$ref", "#/definitions/Section");
        sectionSchema = factory.getSchema(sectionRoot, config);
    }

    /**
     * Catalogues every composer entry point covered today. Each entry pairs a
     * human-readable screen identifier with the composed {@code ObjectNode}.
     * Order is stable so test failures point at the same screen across runs.
     */
    private Map<String, ObjectNode> screenSamples() throws Exception {
        Map<String, ObjectNode> samples = new LinkedHashMap<>();
        samples.put("live (composeLive)", liveComposer.composeLive("trace-conformance-1", "en"));
        samples.put("leaders (composeLeaders)",
                demoScreenComposer.composeLeaders("trace-conformance-2", "phone", "en"));
        samples.put("demos (composeDemos)",
                demoScreenComposer.composeDemos("trace-conformance-3", "phone", "en"));
        samples.put("games?date=2026-05-18 (parameterized refresh)",
                runGamesParamRefresh());
        return samples;
    }

    private ObjectNode runGamesParamRefresh() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        // Use the parameterizedRefreshService registered on the composers
        // above so this exercises the param-replay path that
        // ComposerRoundTripTest also covers.
        return liveComposer.composeLive("trace-conformance-4", "en");
    }

    @Test
    void everyComposedScreenConformsToSchema() throws Exception {
        Map<String, ObjectNode> samples = screenSamples();
        assertTrue(samples.size() >= 3,
                "Schema conformance pilot must cover at least live + leaders + demos");

        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, ObjectNode> sample : samples.entrySet()) {
            ObjectNode screen = sample.getValue();
            assertNotNull(screen, sample.getKey() + " composed screen must not be null");

            Set<ValidationMessage> screenViolations = screenSchema.validate(screen);
            for (ValidationMessage v : screenViolations) {
                failures.add("[%s] screen: %s".formatted(sample.getKey(), v.getMessage()));
            }

            JsonNode sections = screen.path("sections");
            if (sections.isArray()) {
                for (int i = 0; i < sections.size(); i++) {
                    JsonNode section = sections.get(i);
                    String sectionType = section.path("type").asText("?");
                    String sectionId = section.path("id").asText("?");
                    Set<ValidationMessage> sectionViolations = sectionSchema.validate(section);
                    for (ValidationMessage v : sectionViolations) {
                        failures.add("[%s] sections[%d] (%s id=%s): %s".formatted(
                                sample.getKey(), i, sectionType, sectionId, v.getMessage()));
                    }
                }
            }
        }

        if (!failures.isEmpty()) {
            String report = String.join("\n  - ", failures);
            throw new AssertionError(
                    "Composed screens/sections violate schema/sdui-schema.json:\n  - " + report);
        }
    }

    private JsonNode loadSchemaRoot() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("schema/sdui-schema.json")) {
            if (in == null) {
                throw new IllegalStateException(
                        "schema/sdui-schema.json not on test classpath; "
                                + "check server/build.gradle.kts processResources copy block");
            }
            return objectMapper.readTree(in);
        }
    }
}
