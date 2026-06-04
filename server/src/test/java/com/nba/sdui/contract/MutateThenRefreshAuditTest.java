package com.nba.sdui.contract;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
 * Audit test: walks every composer's emitted action sequences and asserts no
 * chain does {@code mutate} followed by a {@code refresh} whose endpoint
 * references the legacy {@code /screen/refresh/} URL pattern.
 *
 * <p>The concern: a {@code mutate} action writes local screen state, then a
 * subsequent {@code refresh} relies on the old partial-screen merge behavior
 * to blend the mutation with the refreshed response. Under the unified channel
 * contract, {@code refresh} on a screen endpoint is always a full replace, so
 * any such chain is a latent bug.
 *
 * <p>Additionally verifies that all {@code refresh} actions with endpoints use
 * the unified URL shape, regardless of whether they follow a {@code mutate}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MutateThenRefreshAuditTest {

    private static final Pattern LEGACY_REFRESH = Pattern.compile("screen/refresh/");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<ObjectNode> allScreens = new ArrayList<>();

    @BeforeAll
    void setup() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        ParameterizedRefreshService parameterizedRefreshService = new ParameterizedRefreshService();
        SectionRefreshService sectionRefreshService = new SectionRefreshService();

        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        ObjectNode emptyScoreboard = objectMapper.createObjectNode();
        ObjectNode sb = emptyScoreboard.putObject("scoreboard");
        sb.set("games", objectMapper.createArrayNode());
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard);
        when(statsApiClient.getScoreboardForDate(any())).thenReturn(emptyScoreboard);

        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService();
        ReflectionTestUtils.setField(seasonCalendarService, "clock", clock);

        LiveComposer liveComposer = new LiveComposer(
                objectMapper, new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                sectionRefreshService, parameterizedRefreshService, seasonCalendarService);
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");
        allScreens.add(liveComposer.composeLive("trace-audit-1", "en"));

        DemoScreenComposer demoScreenComposer = new DemoScreenComposer(
                objectMapper, utils, surfaces, TestTokens.INSTANCE, parameterizedRefreshService);
        ReflectionTestUtils.setField(demoScreenComposer, "schemaVersion", "1.0");
        allScreens.add(demoScreenComposer.composeDemos("trace-audit-2", "phone", "en"));
        allScreens.add(demoScreenComposer.composeLeaders("trace-audit-3", "phone", "en"));
    }

    @Test
    void noComposerEmitsRefreshWithLegacyEndpoint() {
        List<String> violations = new ArrayList<>();
        for (ObjectNode screen : allScreens) {
            String screenId = screen.path("id").asText("?");
            collectLegacyRefreshEndpoints(screen, screenId, violations);
        }
        assertTrue(violations.isEmpty(),
                "Composers emit refresh actions with legacy /screen/refresh/ endpoints: " + violations);
    }

    @Test
    void noMutateThenRefreshChainUsesLegacyEndpoint() {
        List<String> violations = new ArrayList<>();
        for (ObjectNode screen : allScreens) {
            String screenId = screen.path("id").asText("?");
            auditActionArraysForMutateRefreshChains(screen, screenId, violations);
        }
        assertTrue(violations.isEmpty(),
                "mutate→refresh chains with legacy endpoints found: " + violations);
    }

    private void collectLegacyRefreshEndpoints(JsonNode node, String context,
                                                List<String> violations) {
        if (node == null || node.isMissingNode()) return;
        if (node.isObject()) {
            if ("refresh".equals(node.path("type").asText(null))) {
                String endpoint = node.path("endpoint").asText(null);
                if (endpoint != null && LEGACY_REFRESH.matcher(endpoint).find()) {
                    violations.add(context + " → " + endpoint);
                }
            }
            node.fields().forEachRemaining(e ->
                    collectLegacyRefreshEndpoints(e.getValue(), context, violations));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectLegacyRefreshEndpoints(child, context, violations);
            }
        }
    }

    /**
     * Walk every array in the tree and look for adjacent mutate→refresh pairs
     * where the refresh's endpoint is the legacy shape.
     */
    private void auditActionArraysForMutateRefreshChains(JsonNode node, String context,
                                                          List<String> violations) {
        if (node == null || node.isMissingNode()) return;
        if (node.isArray()) {
            for (int i = 0; i < node.size() - 1; i++) {
                JsonNode current = node.get(i);
                JsonNode next = node.get(i + 1);
                if ("mutate".equals(current.path("type").asText(null))
                        && "refresh".equals(next.path("type").asText(null))) {
                    String endpoint = next.path("endpoint").asText(null);
                    if (endpoint != null && LEGACY_REFRESH.matcher(endpoint).find()) {
                        violations.add(context + " → mutate→refresh(" + endpoint + ")");
                    }
                }
            }
            for (JsonNode child : node) {
                auditActionArraysForMutateRefreshChains(child, context, violations);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(e ->
                    auditActionArraysForMutateRefreshChains(e.getValue(), context, violations));
        }
    }
}
