package com.nba.sdui.service;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.LiveComposer;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;

class LiveComposerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void composeLive_emitsCalendarStripAsFirstSection_withSeededState() throws Exception {
        LiveComposer composer = buildComposer(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(composer.composeLive("trace-1", "en"));
        ArrayNode sections = (ArrayNode) screen.path("sections");
        ObjectNode calendarStrip = (ObjectNode) sections.get(0);
        ObjectNode data = (ObjectNode) calendarStrip.path("data");

        assertEquals("server-games-calendar__type-CalendarStrip", calendarStrip.path("id").asText());
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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(composer.composeLive("trace-2", "en"));
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
        Optional<com.nba.sdui.models.generated.Screen> refreshed = parameterizedRefreshService.refreshScreen(
                "games",
                "trace-3",
                Map.of("date", "2026-05-26"),
                ctx
        );

        assertTrue(refreshed.isPresent(), "games resolver should be registered");
        ObjectNode response = (ObjectNode) objectMapper.valueToTree(refreshed.get());
        ObjectNode data = (ObjectNode) response.path("sections").get(0).path("data");
        assertEquals("2026-05-26", data.path("selectedDate").asText());
        assertEquals("2026-05-26", response.path("state").path("games_selected_date").asText());
    }

    @Test
    void composeLive_withSelectedDateOverride_passesDateToStatsApiClient() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-x", "en", "2026-03-15"));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-y", "en", null));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-blank", "en", "   "));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-malformed", "en", "not-a-date"));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-before", "en", "2025-09-15"));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-after", "en", "2026-08-15"));

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

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-cdn-date", "en", null));

        ObjectNode calendarData = (ObjectNode) screen.path("sections").get(0).path("data");
        assertEquals("2026-05-26", calendarData.path("defaultDate").asText());
        assertEquals("2026-05-26", calendarData.path("selectedDate").asText());
    }

    @Test
    void composeLive_whenNoGamesForDate_emitsCalendarStripAndPromo() throws Exception {
        // No mock fallback: empty scoreboard → CalendarStrip + League Pass promo,
        // no game sections and no AdSlot.
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class))).thenReturn(null);

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-empty", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        assertEquals(2, sections.size(),
                "only the CalendarStrip and League Pass promo should be emitted when no games exist");
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("games_screen_promo_banner", sections.get(1).path("analyticsId").asText());
    }

    @Test
    void composeLive_withInSeasonOverride_partitionsRealGames() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(2, 1, 3));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-z", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        // Layout: CalendarStrip, League Pass promo, then one card section per game.
        // 1 live + 1 upcoming + 1 final = 3 cards → ad placed after the 2nd card.
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("games_screen_promo_banner", sections.get(1).path("analyticsId").asText());
        assertEquals("games_game_0022600001", sections.get(2).path("analyticsId").asText());
        assertEquals("games_game_0022600002", sections.get(3).path("analyticsId").asText());
        assertEquals("AdSlot", sections.get(4).path("type").asText());
        assertEquals("games_game_0022600003", sections.get(5).path("analyticsId").asText());
    }

    @Test
    void composeLive_withZeroGames_emitsNoAdSlot() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class))).thenReturn(null);

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-noad-0", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        for (int i = 0; i < sections.size(); i++) {
            assertFalse(
                    "AdSlot".equals(sections.get(i).path("type").asText()),
                    "no AdSlot expected when there are zero games"
            );
        }
    }

    @Test
    void composeLive_withSingleGame_emitsAdSlotAfterFirstSection() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(1));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-ad-1", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        // Layout: CalendarStrip, League Pass promo, one game card, AdSlot.
        // 1 game (upcoming) → ad inserted after the only card.
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("games_screen_promo_banner", sections.get(1).path("analyticsId").asText());
        assertEquals("games_game_0022600001", sections.get(2).path("analyticsId").asText());
        assertEquals("AdSlot", sections.get(3).path("type").asText());
        assertEquals(4, sections.size());
    }

    @Test
    void composeLive_adSlot_emitsServerOwnedReservationSizes() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(2, 2));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-ad-sizes", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        // Layout: CalendarStrip, League Pass promo, two game cards, AdSlot.
        // 2 live games → ad after the 2nd card.
        ObjectNode adSlot = (ObjectNode) sections.get(4);
        assertEquals("AdSlot", adSlot.path("type").asText());
        ObjectNode adData = (ObjectNode) adSlot.path("data");
        assertEquals("gam", adData.path("provider").asText());
        ArrayNode sizes = (ArrayNode) adData.path("sizes");
        assertEquals(2, sizes.size());
        assertEquals(320, sizes.get(0).get(0).asInt());
        assertEquals(50, sizes.get(0).get(1).asInt());
        assertEquals(728, sizes.get(1).get(0).asInt());
        assertEquals(90, sizes.get(1).get(1).asInt());
        assertEquals("games", adData.path("targeting").path("section").asText());
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
        Optional<com.nba.sdui.models.generated.Screen> refreshed = parameterizedRefreshService.refreshScreen(
                "games",
                "trace-q",
                Map.of("date", "2026-03-15"),
                ctx
        );

        assertTrue(refreshed.isPresent(), "games resolver should be registered");
        ArrayNode sections = (ArrayNode) ((ObjectNode) objectMapper.valueToTree(refreshed.get())).path("sections");
        assertEquals("CalendarStrip", sections.get(0).path("type").asText());
        assertEquals("games_screen_promo_banner", sections.get(1).path("analyticsId").asText());
        assertEquals("games_game_0022600001", sections.get(2).path("analyticsId").asText());
    }

    @Test
    void composeLive_emitsOneCardPerGameInRosterOrder_flatListNoHeaders() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(1, 3, 2));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-roster", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        assertEquals("games_game_0022600003", sections.get(2).path("analyticsId").asText(), "live card comes first");
        assertEquals("games_game_0022600001", sections.get(3).path("analyticsId").asText(), "pregame card comes after live");
        assertEquals("AdSlot", sections.get(4).path("type").asText(), "ad slot stays after second game card");
        assertEquals("games_game_0022600002", sections.get(5).path("analyticsId").asText(), "final card comes last");

        for (JsonNode section : sections) {
            assertFalse("GameScheduleList".equals(section.path("type").asText()),
                    "games screen should emit flat game-card sections, not grouped schedule-list headers");
        }
    }

    @Test
    void composeLive_statusPoliciesAndDataBinding_followPerGameLifecycle() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(1, 2, 3));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-policy", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");

        ObjectNode liveCard = findSectionByAnalyticsId(sections, "games_game_0022600002");
        ObjectNode pregameCard = findSectionByAnalyticsId(sections, "games_game_0022600001");
        ObjectNode finalCard = findSectionByAnalyticsId(sections, "games_game_0022600003");

        ArrayNode pregamePolicy = (ArrayNode) pregameCard.path("refreshPolicy");
        assertEquals(1, pregamePolicy.size());
        assertEquals("poll", pregamePolicy.get(0).path("type").asText());
        assertEquals(300000, pregamePolicy.get(0).path("intervalMs").asInt());
        assertTrue(pregamePolicy.get(0).path("sectionEndpoint").asText().contains("/v1/sdui/section/"));
        assertFalse(pregameCard.has("dataBinding"), "pregame card should not carry dataBinding");

        ArrayNode livePolicy = (ArrayNode) liveCard.path("refreshPolicy");
        assertEquals(2, livePolicy.size());
        assertEquals("sse", livePolicy.get(0).path("type").asText());
        assertEquals("0022600002:linescore", livePolicy.get(0).path("channel").asText());
        assertEquals("poll", livePolicy.get(1).path("type").asText());
        assertEquals(60000, livePolicy.get(1).path("intervalMs").asInt());
        assertTrue(livePolicy.get(1).path("sectionEndpoint").asText().contains("/v1/sdui/section/"));
        assertTrue(liveCard.has("dataBinding"), "live card should carry single-game dataBinding");
        ArrayNode bindings = (ArrayNode) liveCard.path("dataBinding").path("bindings");
        assertTrue(bindings.size() > 0);
        for (JsonNode binding : bindings) {
            assertTrue(binding.path("targetPath").asText().startsWith("content.0022600002."),
                    "live card dataBinding should only target its own game payload");
        }

        ArrayNode finalPolicy = (ArrayNode) finalCard.path("refreshPolicy");
        assertEquals(1, finalPolicy.size());
        assertEquals("static", finalPolicy.get(0).path("type").asText());
        assertFalse(finalCard.has("dataBinding"), "final card should not carry dataBinding");
    }

    @Test
    void composeLive_neverEmitsDataBindingChannels_andRefreshPolicyBoundsHold() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboardForDate(any(LocalDate.class)))
                .thenReturn(scoreboardWithGames(2, 2, 1, 3));

        ObjectNode screen = (ObjectNode) objectMapper.valueToTree(fixture.composer.composeLive("trace-bounds", "en", "2026-03-15"));
        ArrayNode sections = (ArrayNode) screen.path("sections");
        for (JsonNode section : sections) {
            JsonNode refreshPolicy = section.path("refreshPolicy");
            if (!refreshPolicy.isArray()) {
                continue;
            }
            assertTrue(refreshPolicy.size() <= 2, "refreshPolicy must never exceed 2 elements");

            int opaqueCount = 0;
            int sectionEndpointCount = 0;
            for (JsonNode policy : refreshPolicy) {
                if (policy.hasNonNull("channel") || policy.hasNonNull("url")) {
                    opaqueCount++;
                }
                if (policy.hasNonNull("sectionEndpoint")) {
                    sectionEndpointCount++;
                }
            }
            assertTrue(opaqueCount <= 1, "refreshPolicy must have <=1 opaque element");
            assertTrue(sectionEndpointCount <= 1, "refreshPolicy must have <=1 sectionEndpoint element");

            JsonNode dataBinding = section.path("dataBinding");
            if (dataBinding.isObject()) {
                assertFalse(dataBinding.has("channels"),
                        "regression: no section should emit dataBinding.channels");
            }
        }
    }

    @Test
    void sectionRefreshResolver_returnsGameCardAtCurrentStatus() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboard())
                .thenReturn(scoreboardWithGames(1, 2, 3));
        ReflectionTestUtils.invokeMethod(fixture.composer, "registerResolvers");

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");

        String pregameId = SectionIdDeriver.derive("games:card-0022600001", "AtomicComposite");
        Optional<JsonNode> pregameResult = fixture.sectionRefreshService.refreshSection(pregameId, ctx);
        assertTrue(pregameResult.isPresent());
        assertEquals("poll", pregameResult.get().path("refreshPolicy").get(0).path("type").asText());
        assertEquals(300000, pregameResult.get().path("refreshPolicy").get(0).path("intervalMs").asInt());

        String liveId = SectionIdDeriver.derive("games:card-0022600002", "AtomicComposite");
        Optional<JsonNode> liveResult = fixture.sectionRefreshService.refreshSection(liveId, ctx);
        assertTrue(liveResult.isPresent());
        assertEquals("sse", liveResult.get().path("refreshPolicy").get(0).path("type").asText());
        assertEquals("poll", liveResult.get().path("refreshPolicy").get(1).path("type").asText());

        String finalId = SectionIdDeriver.derive("games:card-0022600003", "AtomicComposite");
        Optional<JsonNode> finalResult = fixture.sectionRefreshService.refreshSection(finalId, ctx);
        assertTrue(finalResult.isPresent());
        assertEquals("static", finalResult.get().path("refreshPolicy").get(0).path("type").asText());
    }

    @Test
    void sectionRefreshResolver_unknownGameId_returnsEmpty() throws Exception {
        ComposerFixture fixture = buildFixture(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        when(fixture.statsApiClient.getScoreboard()).thenReturn(scoreboardWithGames(2, 1));
        ReflectionTestUtils.invokeMethod(fixture.composer, "registerResolvers");

        SduiRequestContext ctx = new SduiRequestContext();
        ctx.setLocale("en");

        String unknownId = SectionIdDeriver.derive("games:card-0099999999", "AtomicComposite");
        Optional<JsonNode> result = fixture.sectionRefreshService.refreshSection(unknownId, ctx);
        assertTrue(result.isEmpty(), "unknown game id should resolve to empty/404");
    }

    private ObjectNode findSectionByAnalyticsId(ArrayNode sections, String analyticsId) {
        for (JsonNode section : sections) {
            if (analyticsId.equals(section.path("analyticsId").asText())) {
                return (ObjectNode) section;
            }
        }
        throw new AssertionError("missing section with analyticsId=" + analyticsId);
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

        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        SectionRefreshService sectionRefreshService = new SectionRefreshService();

        LiveComposer composer = new LiveComposer(
                new StatsApiAdapter(statsApiClient),
                utils,
                surfaces, TestTokens.INSTANCE,
                sectionRefreshService,
                parameterizedRefreshService,
                seasonCalendarService
        );
        ReflectionTestUtils.setField(composer, "schemaVersion", "1.0");
        return new ComposerFixture(composer, statsApiClient, sectionRefreshService);
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

    private record ComposerFixture(
            LiveComposer composer,
            StatsApiClient statsApiClient,
            SectionRefreshService sectionRefreshService) {}
}
