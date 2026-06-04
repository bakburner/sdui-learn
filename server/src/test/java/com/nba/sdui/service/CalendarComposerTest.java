package com.nba.sdui.service;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.composer.CalendarComposer;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.LiveComposer;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SectionRefreshService;

class CalendarComposerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void composeCalendar_emitsCalendarMonthListWithRequiredFieldsAndNavigateAction() throws Exception {
        CalendarComposer composer = buildCalendarComposer(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = (ObjectNode) composer.composeCalendar("trace-calendar-1", "en");
        ArrayNode sections = (ArrayNode) screen.path("sections");
        ObjectNode section = (ObjectNode) sections.get(0);
        ObjectNode data = (ObjectNode) section.path("data");

        assertEquals("calendar", screen.path("id").asText());
        assertEquals(1, sections.size());
        assertEquals("CalendarMonthList", section.path("type").asText());

        assertEquals("calendar_selected_date", data.path("stateKey").asText());
        assertTrue(data.has("selectedDate"));
        assertTrue(data.has("defaultDate"));
        assertTrue(data.has("onDateSelected"));

        assertEquals("navigate", data.path("onDateSelected").path("type").asText());
        assertTrue(data.path("onDateSelected").path("targetUri").asText().contains("nba://games"));
    }

    @Test
    void composeCalendar_withDateParam_usesProvidedSelectedDate() {
        CalendarComposer composer = buildCalendarComposer(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = (ObjectNode) composer.composeCalendar("trace-calendar-2", "en", "2026-03-15");
        ObjectNode data = (ObjectNode) screen.path("sections").get(0).path("data");

        assertEquals("2026-03-15", data.path("selectedDate").asText());
        assertEquals("2026-05-26", data.path("defaultDate").asText());
    }

    @Test
    void composeCalendar_dateMetadata_emittedFromSeasonGameCounts() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(clock);
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getSeasonGameCounts()).thenReturn(java.util.Map.of(
                LocalDate.of(2025, 10, 22), 11,
                LocalDate.of(2025, 10, 23), 7,
                // Out-of-season date — must be filtered by isInSeason() guard.
                LocalDate.of(2020, 1, 1), 5
        ));

        CalendarComposer composer = new CalendarComposer(
                objectMapper, seasonCalendarService, new StatsApiAdapter(statsApiClient), new SduiUtils(objectMapper, TestTokens.INSTANCE));
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");

        ObjectNode screen = (ObjectNode) composer.composeCalendar("trace-calendar-meta", "en");
        ObjectNode data = (ObjectNode) screen.path("sections").get(0).path("data");
        ObjectNode meta = (ObjectNode) data.path("dateMetadata");

        // In-season dates emitted as expected.
        assertEquals(11, meta.path("2025-10-22").path("gameCount").asInt());
        assertEquals(false, meta.path("2025-10-22").path("hasTeamGame").asBoolean());
        assertEquals(7, meta.path("2025-10-23").path("gameCount").asInt());

        // Out-of-season date never reaches the wire.
        assertTrue(meta.path("2020-01-01").isMissingNode());
    }

    @Test
    void composeCalendar_dateMetadata_emptyWhenSeasonScheduleUnavailable() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(clock);
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getSeasonGameCounts()).thenThrow(new java.io.IOException("CDN down"));

        CalendarComposer composer = new CalendarComposer(
                objectMapper, seasonCalendarService, new StatsApiAdapter(statsApiClient), new SduiUtils(objectMapper, TestTokens.INSTANCE));
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");

        ObjectNode screen = (ObjectNode) composer.composeCalendar("trace-calendar-fail", "en");
        ObjectNode data = (ObjectNode) screen.path("sections").get(0).path("data");
        ObjectNode meta = (ObjectNode) data.path("dateMetadata");

        // Empty metadata still emitted; composition does not fail when the CDN is unreachable.
        assertTrue(meta.isObject());
        assertEquals(0, meta.size());
    }

    @Test
    void liveComposer_calendarStrip_containsExpandedActionNavigateToCalendar() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC);
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(clock);

        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard("2026-05-26"));
        when(statsApiClient.getScoreboardForDate(LocalDate.parse("2026-05-26")))
                .thenReturn(emptyScoreboard("2026-05-26"));

        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);

        LiveComposer liveComposer = new LiveComposer(
                objectMapper,
                new StatsApiAdapter(statsApiClient),
                utils,
                surfaces, TestTokens.INSTANCE,
                new SectionRefreshService(),
                new ParameterizedRefreshService(),
                seasonCalendarService
        );
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");

        ObjectNode screen = liveComposer.composeLive("trace-live-1", "en");
        ObjectNode data = (ObjectNode) screen.path("sections").get(0).path("data");

        assertTrue(data.has("expandedAction"));
        assertEquals("onActivate", data.path("expandedAction").path("trigger").asText());
        assertEquals("navigate", data.path("expandedAction").path("type").asText());
        assertEquals("nba://calendar", data.path("expandedAction").path("targetUri").asText());
    }

    private CalendarComposer buildCalendarComposer(Clock clock) {
        try {
            SeasonCalendarService seasonCalendarService = new SeasonCalendarService(clock);
            StatsApiClient statsApiClient = mock(StatsApiClient.class);
            when(statsApiClient.getSeasonGameCounts()).thenReturn(java.util.Collections.emptyMap());
            CalendarComposer composer = new CalendarComposer(
                    objectMapper, seasonCalendarService, new StatsApiAdapter(statsApiClient), new SduiUtils(objectMapper, TestTokens.INSTANCE));
            ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");
            return composer;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectNode emptyScoreboard(String gameDate) {
        ObjectNode scoreboard = objectMapper.createObjectNode();
        ObjectNode scoreboardData = objectMapper.createObjectNode();
        scoreboardData.put("gameDate", gameDate);
        scoreboardData.set("games", objectMapper.createArrayNode());
        scoreboard.set("scoreboard", scoreboardData);
        return scoreboard;
    }
}
