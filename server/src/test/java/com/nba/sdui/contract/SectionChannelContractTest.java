package com.nba.sdui.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Channel-contract tests for the section channel ({@code /v1/sdui/section/{id}}).
 *
 * <p>Pins the invariant that section endpoints always return a single
 * {@code Section} JSON object — never a {@code Screen}, never a list.
 * Section responses must not carry top-level Screen fields such as
 * {@code title}, {@code defaultRefreshPolicy}, or {@code sections}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SectionChannelContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SectionRefreshService sectionRefreshService;
    private String liveGamesSectionId;

    @BeforeAll
    void setup() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        sectionRefreshService = new SectionRefreshService();
        ParameterizedRefreshService parameterizedRefreshService = new ParameterizedRefreshService();

        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        ObjectNode emptyScoreboard = objectMapper.createObjectNode();
        ObjectNode sb = emptyScoreboard.putObject("scoreboard");
        sb.set("games", objectMapper.createArrayNode());
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard);
        when(statsApiClient.getScoreboardForDate(any())).thenReturn(emptyScoreboard);

        SduiUtils utils = new SduiUtils(objectMapper);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils);
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService();
        ReflectionTestUtils.setField(seasonCalendarService, "clock", clock);

        LiveComposer liveComposer = new LiveComposer(
                objectMapper, statsApiClient, utils, surfaces,
                sectionRefreshService, parameterizedRefreshService, seasonCalendarService);
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(liveComposer, "registerResolvers");

        liveGamesSectionId = SectionIdDeriver.derive("stats-api:live-games", "GameScheduleList");
    }

    @Test
    void liveGamesSection_returnsASingleSectionObject_notAScreen() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");

        Optional<JsonNode> result = sectionRefreshService.refreshSection(liveGamesSectionId, ctx);
        assertTrue(result.isPresent(), "section resolver must be registered for live-games");

        JsonNode section = result.get();
        assertTrue(section.isObject(), "section response must be a JSON object");
        assertFalse(section.isArray(), "section response must not be an array");

        assertSectionShape(section);
    }

    @Test
    void liveGamesSection_doesNotCarryScreenLevelFields() {
        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");

        Optional<JsonNode> result = sectionRefreshService.refreshSection(liveGamesSectionId, ctx);
        assertTrue(result.isPresent());

        JsonNode section = result.get();
        assertNoScreenFields(section);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void assertSectionShape(JsonNode section) {
        assertTrue(section.has("id"), "section must have an 'id' field");
        assertTrue(section.has("type"), "section must have a 'type' field");
        assertFalse(section.has("sections"),
                "section must not have a 'sections' field (that's a Screen shape)");
    }

    private void assertNoScreenFields(JsonNode section) {
        assertFalse(section.has("title"),
                "section response must not carry top-level 'title' (Screen field)");
        assertFalse(section.has("defaultRefreshPolicy"),
                "section response must not carry 'defaultRefreshPolicy' (Screen field)");
        assertFalse(section.has("sections"),
                "section response must not carry 'sections' (Screen field)");
        assertFalse(section.has("schemaVersion"),
                "section response must not carry 'schemaVersion' (Screen field)");
        assertFalse(section.has("navigation"),
                "section response must not carry 'navigation' (Screen field)");
    }
}
