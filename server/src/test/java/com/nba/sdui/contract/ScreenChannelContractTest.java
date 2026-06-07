package com.nba.sdui.contract;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.DemoScreenComposer;
import com.nba.sdui.domain.composer.LiveComposer;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;

/**
 * Channel-contract tests for every screen served by the SDUI composition layer.
 *
 * <p>Pins the following invariants per the update-channel-unification plan:
 * <ul>
 *   <li>Screen endpoints return a JSON object with an {@code id} matching the
 *       requested screen ID and a {@code sections} array — never a bare
 *       {@code Section} or list.</li>
 *   <li>Parameterized screen endpoints return the same shape with the param
 *       applied.</li>
 *   <li>Every emitted action of {@code type: refresh} whose {@code endpoint}
 *       is present matches the regex
 *       {@code ^/v1/sdui/(screen|section)/[a-z0-9-]+(\?.*)?$} — never the
 *       legacy {@code /screen/refresh/…} shape.</li>
 *   <li>For parameterizable screens (games, leaders), the emitted action
 *       endpoints reference the unified screen URL.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScreenChannelContractTest {

    private static final Pattern VALID_ENDPOINT = Pattern.compile(
            "^/v1/sdui/(screen|section)/[a-z0-9-]+(/[a-z0-9-]+)*(\\?.*)?$");
    private static final Pattern LEGACY_REFRESH = Pattern.compile("screen/refresh/");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LiveComposer liveComposer;
    private DemoScreenComposer demoScreenComposer;
    private ParameterizedRefreshService parameterizedRefreshService;

    @BeforeAll
    void setup() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        parameterizedRefreshService = new ParameterizedRefreshService();

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
                new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                sectionRefreshService, parameterizedRefreshService, seasonCalendarService);
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(liveComposer, "registerResolvers");

        demoScreenComposer = new DemoScreenComposer(
                utils, surfaces, TestTokens.INSTANCE, parameterizedRefreshService);
        ReflectionTestUtils.setField(demoScreenComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(demoScreenComposer, "registerParameterizedRefreshResolvers");
    }

    // ── Shape invariant: screen responses are always {id, sections[]} ────

    @Test
    void gamesScreen_returnsScreenShapeWithMatchingId() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(liveComposer.composeLive("trace-1", "en"));
        assertScreenShape(screen, "games");
    }

    @Test
    void gamesScreen_withDateParam_returnsScreenShapeWithMatchingId() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<com.nba.sdui.models.generated.Screen> result = parameterizedRefreshService.refreshScreen(
                "games", "trace-2", Map.of("date", "2026-05-18"), ctx);
        assertTrue(result.isPresent());
        assertScreenShape((ObjectNode) objectMapper.valueToTree(result.get()), "games");
    }

    @Test
    void leadersScreen_returnsScreenShapeWithMatchingId() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeLeaders("trace-3", "phone", "en"));
        assertScreenShape(screen, "leaders");
    }

    @Test
    void leadersScreen_withParams_returnsScreenShapeWithMatchingId() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<com.nba.sdui.models.generated.Screen> result = parameterizedRefreshService.refreshScreen(
                "leaders", "trace-4",
                Map.of("season", "2024-25", "seasonType", "regular",
                        "perMode", "per_game", "statCategory", "pts"), ctx);
        assertTrue(result.isPresent());
        assertScreenShape((ObjectNode) objectMapper.valueToTree(result.get()), "leaders");
    }

    @Test
    void demosScreen_returnsScreenShapeWithMatchingId() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeDemos("trace-5", "phone", "en"));
        assertScreenShape(screen, "demos");
    }

    // ── No screen endpoint returns a bare Section or list ────────────────

    @Test
    void gamesScreen_neverReturnsBareSection() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(liveComposer.composeLive("trace-shape-1", "en"));
        assertFalse(screen.has("type") && !screen.has("sections"),
                "screen response must not look like a bare Section (has 'type' without 'sections')");
    }

    @Test
    void leadersScreen_neverReturnsBareSection() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeLeaders("trace-shape-2", "phone", "en"));
        assertFalse(screen.has("type") && !screen.has("sections"),
                "screen response must not look like a bare Section");
    }

    // ── Regression fence: no refresh action references /screen/refresh/ ──

    @Test
    void gamesScreen_allRefreshEndpoints_useUnifiedScreenUrl() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(liveComposer.composeLive("trace-fence-1", "en"));
        assertNoLegacyRefreshEndpoints(screen, "games");
    }

    @Test
    void gamesScreen_withDateParam_allRefreshEndpoints_useUnifiedScreenUrl() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<com.nba.sdui.models.generated.Screen> result = parameterizedRefreshService.refreshScreen(
                "games", "trace-fence-2", Map.of("date", "2026-05-18"), ctx);
        assertTrue(result.isPresent());
        assertNoLegacyRefreshEndpoints((ObjectNode) objectMapper.valueToTree(result.get()), "games (parameterized)");
    }

    @Test
    void leadersScreen_allRefreshEndpoints_useUnifiedScreenUrl() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeLeaders("trace-fence-3", "phone", "en"));
        assertNoLegacyRefreshEndpoints(screen, "leaders");
    }

    @Test
    void demosScreen_allRefreshEndpoints_useUnifiedScreenUrl() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeDemos("trace-fence-4", "phone", "en"));
        assertNoLegacyRefreshEndpoints(screen, "demos");
    }

    // ── Parameterized action endpoints reference the unified URL ─────────

    @Test
    void gamesCalendarStrip_onDateSelected_usesUnifiedEndpoint() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(liveComposer.composeLive("trace-cal-1", "en"));
        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        String endpoint = calendarData.path("onDateSelected").path("endpoint").asText();
        assertEquals("/v1/sdui/screen/games", endpoint);
    }

    @Test
    void leadersForm_submitAction_usesUnifiedEndpoint() {
        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(demoScreenComposer.composeLeaders("trace-form-1", "phone", "en"));
        ArrayNode sections = (ArrayNode) screen.path("sections");
        String endpoint = null;
        for (JsonNode section : sections) {
            if ("Form".equals(section.path("type").asText())) {
                endpoint = section.path("data").path("submitAction").path("endpoint").asText();
                break;
            }
        }
        assertNotNull(endpoint, "Form section must be present in leaders screen");
        assertEquals("/v1/sdui/screen/leaders", endpoint);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void assertScreenShape(ObjectNode screen, String expectedId) {
        assertEquals(expectedId, screen.path("id").asText(),
                "screen.id must match the requested screen ID");
        assertTrue(screen.has("sections"), "screen must have a 'sections' field");
        assertTrue(screen.path("sections").isArray(), "screen.sections must be an array");
        assertFalse(screen.isArray(), "screen response must not be a bare array");
    }

    private void assertNoLegacyRefreshEndpoints(ObjectNode screen, String screenLabel) {
        List<String> violations = new ArrayList<>();
        collectRefreshEndpoints(screen, violations);
        assertTrue(violations.isEmpty(),
                screenLabel + " screen contains legacy /screen/refresh/ endpoints: " + violations);
    }

    private void collectRefreshEndpoints(JsonNode node, List<String> violations) {
        if (node == null || node.isMissingNode()) return;

        if (node.isObject()) {
            if ("refresh".equals(node.path("type").asText(null))) {
                String endpoint = node.path("endpoint").asText(null);
                if (endpoint != null) {
                    if (LEGACY_REFRESH.matcher(endpoint).find()) {
                        violations.add(endpoint);
                    }
                    assertTrue(VALID_ENDPOINT.matcher(endpoint).matches(),
                            "refresh endpoint must match the unified URL pattern: " + endpoint);
                }
            }
            node.fields().forEachRemaining(entry ->
                    collectRefreshEndpoints(entry.getValue(), violations));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectRefreshEndpoints(child, violations);
            }
        }
    }
}
