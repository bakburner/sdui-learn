package com.nba.sdui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveComposerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void composeLive_emitsCalendarStripAsFirstSection_withSeededState() throws Exception {
        LiveComposer composer = buildComposer(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = composer.composeLive("trace-1", "en");
        ArrayNode sections = (ArrayNode) screen.path("sections");
        ObjectNode calendarStrip = (ObjectNode) sections.get(0);
        ObjectNode data = (ObjectNode) calendarStrip.path("data");

        assertEquals("server:games-calendar~type=CalendarStrip", calendarStrip.path("id").asText());
        assertEquals("CalendarStrip", calendarStrip.path("type").asText());
        assertEquals("server:games-calendar", calendarStrip.path("contentSourceId").asText());
        assertEquals("games_calendar_strip", calendarStrip.path("analyticsId").asText());
        assertEquals("Games date picker", calendarStrip.path("accessibility").path("label").asText());

        assertEquals("games_selected_date", data.path("stateKey").asText());
        assertEquals(data.path("defaultDate").asText(), data.path("selectedDate").asText());
        assertEquals("2025-10-01", data.path("minDate").asText());
        assertEquals("2026-06-30", data.path("maxDate").asText());

        assertTrue(data.path("onDateSelected").isObject(), "onDateSelected must be a single Action object");
        assertEquals("onActivate", data.path("onDateSelected").path("trigger").asText());
        assertEquals("refresh", data.path("onDateSelected").path("type").asText());
        assertEquals("/v1/sdui/screen/refresh/games", data.path("onDateSelected").path("endpoint").asText());
        assertEquals(
                "{{games_selected_date}}",
                data.path("onDateSelected").path("paramBindings").path("date").asText()
        );

        assertEquals(
                data.path("defaultDate").asText(),
                screen.path("state").path("games_selected_date").asText()
        );
        assertFalse(screen.has("defaultRefreshPolicy"), "screen.defaultRefreshPolicy should be absent");
    }

    @Test
    void composeLive_usesLeagueTimeZoneForDefaultDate() throws Exception {
        LiveComposer composer = buildComposer(
                Clock.fixed(Instant.parse("2026-05-27T07:30:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = composer.composeLive("trace-2", "en");
        ObjectNode firstSection = (ObjectNode) screen.path("sections").get(0);

        assertEquals("2026-05-27", firstSection.path("data").path("defaultDate").asText());
    }

    @Test
    void registerResolvers_registersGamesResolverAndEchoesSelectedDate() throws Exception {
        ParameterizedRefreshService parameterizedRefreshService = new ParameterizedRefreshService();
        LiveComposer composer = buildComposer(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC),
                parameterizedRefreshService
        );

        ReflectionTestUtils.invokeMethod(composer, "registerResolvers");

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<ObjectNode> refreshed = parameterizedRefreshService.refreshScreen(
                "games",
                "trace-3",
                Map.of("date", "2026-05-26"),
                ctx
        );

        assertTrue(refreshed.isPresent(), "games resolver should be registered");
        ObjectNode response = refreshed.get();
        ObjectNode data = (ObjectNode) response.path("sections").get(0).path("data");
        assertEquals("2026-05-26", data.path("selectedDate").asText());
        assertEquals("2026-05-26", response.path("state").path("games_selected_date").asText());
    }

    private LiveComposer buildComposer(Clock clock) throws Exception {
        return buildComposer(clock, new ParameterizedRefreshService());
    }

    private LiveComposer buildComposer(Clock clock, ParameterizedRefreshService parameterizedRefreshService)
            throws Exception {
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard());

        SduiUtils utils = new SduiUtils(objectMapper);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils);
        SectionRefreshService sectionRefreshService = new SectionRefreshService();

        LiveComposer composer = new LiveComposer(
                objectMapper,
                statsApiClient,
                utils,
                surfaces,
                sectionRefreshService,
                parameterizedRefreshService,
                clock
        );
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");
        return composer;
    }

    private ObjectNode emptyScoreboard() {
        ObjectNode scoreboard = objectMapper.createObjectNode();
        ObjectNode scoreboardData = objectMapper.createObjectNode();
        scoreboardData.set("games", objectMapper.createArrayNode());
        scoreboard.set("scoreboard", scoreboardData);
        return scoreboard;
    }
}
