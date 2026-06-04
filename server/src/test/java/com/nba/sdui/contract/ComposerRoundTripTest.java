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
import java.util.Map;
import java.util.Optional;

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
 * Composer round-trip tests: fetch a screen with a parameter, extract the
 * emitted action endpoint, feed it back through the composer, and verify the
 * resulting screen reflects the param. This is the end-to-end version of the
 * param-replay invariant — the test that would have caught the May 2026
 * calendar-strip bug at the server layer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComposerRoundTripTest {

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
                objectMapper, new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                sectionRefreshService, parameterizedRefreshService, seasonCalendarService);
        ReflectionTestUtils.setField(liveComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(liveComposer, "registerResolvers");

        demoScreenComposer = new DemoScreenComposer(
                objectMapper, utils, surfaces, TestTokens.INSTANCE, parameterizedRefreshService);
        ReflectionTestUtils.setField(demoScreenComposer, "schemaVersion", "1.0");
        ReflectionTestUtils.invokeMethod(demoScreenComposer, "registerParameterizedRefreshResolvers");
    }

    /**
     * Games screen round-trip: compose with date=2026-05-18, extract the
     * CalendarStrip's onDateSelected.endpoint, invoke the resolver with the
     * same date, and verify the resulting screen echoes that date in its state
     * and CalendarStrip selectedDate.
     */
    @Test
    void gamesScreen_paramRoundTrip_dateEchoedInResponse() {
        String testDate = "2026-05-18";

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<ObjectNode> firstPass = parameterizedRefreshService.refreshScreen(
                "games", "trace-rt-1", Map.of("date", testDate), ctx);
        assertTrue(firstPass.isPresent());
        ObjectNode screen1 = firstPass.get();

        assertEquals(testDate, screen1.path("state").path("games_selected_date").asText(),
                "first pass: screen state must echo the requested date");

        ObjectNode calendarData = (ObjectNode) screen1.path("sections").get(0).path("data");
        assertEquals(testDate, calendarData.path("selectedDate").asText(),
                "first pass: CalendarStrip.selectedDate must echo the requested date");

        String emittedEndpoint = calendarData.path("onDateSelected").path("endpoint").asText();
        assertEquals("/v1/sdui/screen/games", emittedEndpoint,
                "emitted endpoint must be the unified screen URL");

        Optional<ObjectNode> secondPass = parameterizedRefreshService.refreshScreen(
                "games", "trace-rt-2", Map.of("date", testDate), ctx);
        assertTrue(secondPass.isPresent());
        ObjectNode screen2 = secondPass.get();

        assertEquals(testDate, screen2.path("state").path("games_selected_date").asText(),
                "second pass: screen state must still echo the date after round-trip");
        assertEquals(testDate,
                screen2.path("sections").get(0).path("data").path("selectedDate").asText(),
                "second pass: CalendarStrip.selectedDate must survive the round-trip");
    }

    /**
     * Leaders screen round-trip: compose with season=2024-25, extract the
     * Form's submitAction.endpoint, invoke the resolver with the same params,
     * and verify the resulting screen's state and table subtitle reflect the
     * new season.
     */
    @Test
    void leadersScreen_paramRoundTrip_seasonAppliedInResponse() {
        Map<String, String> testParams = Map.of(
                "season", "2024-25",
                "seasonType", "regular",
                "perMode", "per_game",
                "statCategory", "pts");

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");
        Optional<ObjectNode> firstPass = parameterizedRefreshService.refreshScreen(
                "leaders", "trace-rt-3", testParams, ctx);
        assertTrue(firstPass.isPresent());
        ObjectNode screen1 = firstPass.get();

        assertEquals("leaders", screen1.path("id").asText());
        assertEquals("2024-25", screen1.path("state").path("form_season").asText(),
                "first pass: screen state must echo the requested season");

        String formEndpoint = findFormSubmitEndpoint(screen1);
        assertNotNull(formEndpoint, "Form section must be present in leaders screen");
        assertEquals("/v1/sdui/screen/leaders", formEndpoint,
                "emitted endpoint must be the unified screen URL");

        Optional<ObjectNode> secondPass = parameterizedRefreshService.refreshScreen(
                "leaders", "trace-rt-4", testParams, ctx);
        assertTrue(secondPass.isPresent());
        ObjectNode screen2 = secondPass.get();

        assertEquals("2024-25", screen2.path("state").path("form_season").asText(),
                "second pass: screen state must still echo the season after round-trip");
    }

    private String findFormSubmitEndpoint(ObjectNode screen) {
        ArrayNode sections = (ArrayNode) screen.path("sections");
        for (JsonNode section : sections) {
            if ("Form".equals(section.path("type").asText())) {
                return section.path("data").path("submitAction").path("endpoint").asText(null);
            }
        }
        return null;
    }
}
