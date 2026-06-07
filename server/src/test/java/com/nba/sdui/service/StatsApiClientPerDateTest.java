package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiClient;

class StatsApiClientPerDateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getScoreboardForDate_nullDate_throws() {
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(Clock.systemUTC());
        StatsApiClient client = new StatsApiClient(objectMapper, seasonCalendarService);

        assertThrows(IllegalArgumentException.class, () -> client.getScoreboardForDate(null));
    }

    @Test
    void getScoreboardForDate_today_delegatesToGetScoreboard() throws Exception {
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        StatsApiClient spyClient = spy(new StatsApiClient(objectMapper, seasonCalendarService));

        ObjectNode stubbedScoreboard = objectMapper.createObjectNode();
        doReturn(stubbedScoreboard).when(spyClient).getScoreboard();

        JsonNode result = spyClient.getScoreboardForDate(seasonCalendarService.currentLeagueDate());

        verify(spyClient).getScoreboard();
        assertEquals(stubbedScoreboard, result);
    }

    @Test
    void getScoreboardForDate_nonToday_withoutSubscriptionKey_returnsNull() throws Exception {
        // core.api.subscription-key defaults to empty string via @Value when not set,
        // so the per-date path returns null — callers render an empty/no-games state
        // rather than synthesizing fake content.
        SeasonCalendarService seasonCalendarService = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T14:00:00Z"), ZoneOffset.UTC)
        );
        StatsApiClient client = new StatsApiClient(objectMapper, seasonCalendarService);

        assertNull(client.getScoreboardForDate(LocalDate.parse("2026-03-15")));
        assertNull(client.getScoreboardForDate(LocalDate.parse("2026-04-15")));
    }

    @Test
    void normalizeCoreApiFeedToScoreboard_passesThroughGameCards_filtersNonGameCards() throws Exception {
        StatsApiClient client = new StatsApiClient(new ObjectMapper(), new SeasonCalendarService());

        String feedJson = """
                {
                  "modules": [
                    {
                      "moduleType": "list",
                      "cards": [
                        {
                          "cardType": "game",
                          "cardData": {
                            "gameId": "0022400001",
                            "gameStatus": 2,
                            "gameStatusText": "Q3 4:22",
                            "awayTeam": {"teamId":"1610612738","teamTricode":"BOS","teamName":"Celtics","score":72},
                            "homeTeam": {"teamId":"1610612747","teamTricode":"LAL","teamName":"Lakers","score":68}
                          }
                        },
                        {
                          "cardType": "ad",
                          "cardData": {"adSlotId": "ad-1"}
                        },
                        {
                          "cardType": "game",
                          "cardData": {
                            "gameId": "0022400002",
                            "gameStatus": 3,
                            "gameStatusText": "Final",
                            "awayTeam": {"teamId":"1610612744","teamTricode":"GSW","teamName":"Warriors","score":110},
                            "homeTeam": {"teamId":"1610612756","teamTricode":"PHX","teamName":"Suns","score":104}
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
        JsonNode feed = new ObjectMapper().readTree(feedJson);

        JsonNode normalized = client.normalizeCoreApiFeedToScoreboard(feed, LocalDate.parse("2026-03-15"));

        assertNotNull(normalized);
        assertEquals("2026-03-15", normalized.path("scoreboard").path("gameDate").asText());
        JsonNode games = normalized.path("scoreboard").path("games");
        assertEquals(2, games.size(), "should drop non-game cards");
        assertEquals("0022400001", games.get(0).path("gameId").asText());
        assertEquals(2, games.get(0).path("gameStatus").asInt());
        assertEquals("BOS", games.get(0).path("awayTeam").path("teamTricode").asText());
        assertEquals("0022400002", games.get(1).path("gameId").asText());
    }

    @Test
    void normalizeCoreApiFeedToScoreboard_returnsNullForNullFeed() {
        StatsApiClient client = new StatsApiClient(new ObjectMapper(), new SeasonCalendarService());
        assertNull(client.normalizeCoreApiFeedToScoreboard(null, LocalDate.parse("2026-03-15")));
    }

    @Test
    void normalizeCoreApiFeedToScoreboard_returnsNullForMissingModules() throws Exception {
        StatsApiClient client = new StatsApiClient(new ObjectMapper(), new SeasonCalendarService());
        JsonNode feed = new ObjectMapper().readTree("{}");
        assertNull(client.normalizeCoreApiFeedToScoreboard(feed, LocalDate.parse("2026-03-15")));
    }
}
