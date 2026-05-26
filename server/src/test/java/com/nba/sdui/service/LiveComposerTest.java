package com.nba.sdui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        assertEquals("/v1/sdui/screen/games", data.path("onDateSelected").path("endpoint").asText());
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

    @Test
    void composeLive_withSelectedDateOverride_passesDateToStatsApiClient() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-x", "en", "2026-03-15");

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(fixture.statsApiClient).getScoreboardForDate(dateCaptor.capture());
        assertEquals(LocalDate.parse("2026-03-15"), dateCaptor.getValue());

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-03-15", calendarData.path("selectedDate").asText());
        assertEquals("2026-03-15", screen.path("state").path("games_selected_date").asText());
        assertEquals("2026-05-26", calendarData.path("defaultDate").asText());
    }

    @Test
    void composeLive_withNullDateOverride_reusesCachedTodayScoreboard() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-y", "en", null);

        // When the selection is today, composer reuses the CDN scoreboard
        // it already fetched to resolve the default date — no extra Core API call.
        verify(fixture.statsApiClient).getScoreboard();
        verify(fixture.statsApiClient, never()).getScoreboardForDate(any(LocalDate.class));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
        assertEquals("2026-05-26", calendarData.path("defaultDate").asText());
        assertEquals("2026-05-26", screen.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_withBlankDateOverride_reusesCachedTodayScoreboard() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-blank", "en", "   ");

        verify(fixture.statsApiClient).getScoreboard();
        verify(fixture.statsApiClient, never()).getScoreboardForDate(any(LocalDate.class));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
        assertEquals("2026-05-26", calendarData.path("defaultDate").asText());
        assertEquals("2026-05-26", screen.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_withMalformedDateOverride_fallsBackToToday() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-malformed", "en", "not-a-date");

        verify(fixture.statsApiClient).getScoreboard();
        verify(fixture.statsApiClient, never()).getScoreboardForDate(any(LocalDate.class));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
        assertEquals("2026-05-26", screen.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_withDateBeforeSeasonStart_fallsBackToToday() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-before", "en", "2025-09-15");

        verify(fixture.statsApiClient).getScoreboard();
        verify(fixture.statsApiClient, never()).getScoreboardForDate(any(LocalDate.class));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
        assertEquals("2026-05-26", screen.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_withDateAfterSeasonEnd_fallsBackToToday() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = fixture.composer.composeLive("trace-after", "en", "2026-08-15");

        verify(fixture.statsApiClient).getScoreboard();
        verify(fixture.statsApiClient, never()).getScoreboardForDate(any(LocalDate.class));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
        assertEquals("2026-05-26", screen.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_defaultDate_prefersCdnScoreboardGameDateOverWallClock() throws Exception {
        // CDN says today is 2026-05-26 even though the wall clock has rolled
        // to 2026-05-28 — composer must trust the CDN's authoritative gameDate.
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-28T17:00:00Z"), ZoneOffset.UTC)
        );
        ObjectNode cdnWithGameDate = objectMapper.createObjectNode();
        ObjectNode sb = cdnWithGameDate.putObject("scoreboard");
        sb.put("gameDate", "2026-05-26");
        sb.set("games", objectMapper.createArrayNode());
        when(fixture.statsApiClient.getScoreboard()).thenReturn(cdnWithGameDate);

        ObjectNode screen = fixture.composer.composeLive("trace-cdn-date", "en", null);

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("defaultDate").asText());
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
    }

    @Test
    void composeLive_whenNoGamesForDate_emitsOnlyCalendarStrip() throws Exception {
        // No mock fallback: empty scoreboard → just the CalendarStrip, no game sections.
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class))).thenReturn(null);

        ObjectNode screen = fixture.composer.composeLive("trace-empty", "en", "2026-03-15");
        ArrayNode sections = (ArrayNode) screen.path("sections");

        assertEquals(1, sections.size(), "only the CalendarStrip should be emitted when no games exist");
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
    }

    @Test
    void composeLive_withInSeasonOverride_partitionsRealGames() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(2, 1, 3));

        ObjectNode screen = fixture.composer.composeLive("trace-z", "en", "2026-03-15");
        ArrayNode sections = (ArrayNode) screen.path("sections");

        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("live_games", sections.get(1).path("analyticsId").asText());
        assertEquals("upcoming_games", sections.get(2).path("analyticsId").asText());
        assertEquals("final_games", sections.get(3).path("analyticsId").asText());
    }

    @Test
    void registerResolvers_withInSeasonDateParam_passesDateToFetcher() throws Exception {
        ParameterizedRefreshService parameterizedRefreshService = new ParameterizedRefreshService();
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC),
                parameterizedRefreshService
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(2));

        ReflectionTestUtils.invokeMethod(fixture.composer, "registerResolvers");

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<ObjectNode> refreshed = parameterizedRefreshService.refreshScreen(
                "games",
                "trace-q",
                Map.of("date", "2026-03-15"),
                ctx
        );

        assertTrue(refreshed.isPresent(), "games resolver should be registered");
        ArrayNode sections = (ArrayNode) refreshed.get().path("sections");
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("live_games", sections.get(1).path("analyticsId").asText());
    }

    private LiveComposer buildComposer(Clock clock) throws Exception {
        return buildFixture(clock, new ParameterizedRefreshService()).composer;
    }

    private LiveComposer buildComposer(Clock clock, ParameterizedRefreshService parameterizedRefreshService)
            throws Exception {
        return buildFixture(clock, parameterizedRefreshService).composer;
    }

    private ComposerFixture buildFixture(Clock clock) throws Exception {
        return buildFixture(clock, new ParameterizedRefreshService());
    }

    private ComposerFixture buildFixture(Clock clock, ParameterizedRefreshService parameterizedRefreshService)
            throws Exception {
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(clock);
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getScoreboard()).thenReturn(emptyScoreboard());
        when(statsApiClient.getScoreboardForDate(any(LocalDate.class))).thenReturn(emptyScoreboard());

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
                seasonCalendarService
        );
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");
        return new ComposerFixture(composer, statsApiClient);
    }

    private ObjectNode emptyScoreboard() {
        ObjectNode scoreboard = objectMapper.createObjectNode();
        ObjectNode scoreboardData = objectMapper.createObjectNode();
        scoreboardData.set("games", objectMapper.createArrayNode());
        scoreboard.set("scoreboard", scoreboardData);
        return scoreboard;
    }

    private ObjectNode scoreboardWithGames(int... statuses) {
        ObjectNode scoreboard = objectMapper.createObjectNode();
        ObjectNode scoreboardData = objectMapper.createObjectNode();
        ArrayNode games = objectMapper.createArrayNode();
        for (int i = 0; i < statuses.length; i++) {
            int status = statuses[i];
            ObjectNode game = objectMapper.createObjectNode();
            game.put("gameId", String.format("0022600%03d", i + 1));
            game.put("gameStatus", status);
            game.put("gameStatusText", switch (status) {
                case 2 -> "Q2 06:11";
                case 1 -> "8:00 PM ET";
                default -> "Final";
            });

            ObjectNode away = objectMapper.createObjectNode();
            away.put("teamTricode", "AWY" + i);
            away.put("teamName", "Away " + i);
            away.put("teamId", String.valueOf(1610612700 + i));
            away.put("score", 100 + i);
            game.set("awayTeam", away);

            ObjectNode home = objectMapper.createObjectNode();
            home.put("teamTricode", "HME" + i);
            home.put("teamName", "Home " + i);
            home.put("teamId", String.valueOf(1610612710 + i));
            home.put("score", 98 + i);
            game.set("homeTeam", home);
            games.add(game);
        }
        scoreboardData.set("games", games);
        scoreboard.set("scoreboard", scoreboardData);
        return scoreboard;
    }

    private record ComposerFixture(LiveComposer composer, StatsApiClient statsApiClient) {}
}
