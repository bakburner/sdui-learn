package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link StatsApiClient#parseGameCounts(JsonNode)}.
 *
 * <p>Exercises the season-schedule normalization path without HTTP — the
 * client constructs a representative {@code scheduleLeagueV2.json} subtree
 * and asserts that the parser:
 * <ul>
 *   <li>extracts an ET-local {@code LocalDate} from the {@code "MM/dd/yyyy HH:mm:ss"} input,</li>
 *   <li>maps it to the number of {@code games[]} entries on that date,</li>
 *   <li>omits dates with zero games,</li>
 *   <li>silently skips malformed entries rather than failing composition.</li>
 * </ul>
 */
class StatsApiClientSeasonScheduleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StatsApiClient client = new StatsApiClient(
            objectMapper, new SeasonCalendarService(java.time.Clock.systemUTC()));

    @Test
    void parseGameCounts_returnsDateToGameCountMap() {
        ObjectNode schedule = buildSchedule(
                gameDate("10/22/2025 00:00:00", 11),
                gameDate("10/23/2025 00:00:00", 7),
                gameDate("10/24/2025 00:00:00", 0)
        );

        Map<LocalDate, Integer> counts = client.parseGameCounts(schedule);

        assertEquals(2, counts.size(), "zero-game dates should be omitted");
        assertEquals(11, counts.get(LocalDate.of(2025, 10, 22)));
        assertEquals(7, counts.get(LocalDate.of(2025, 10, 23)));
        assertFalse(counts.containsKey(LocalDate.of(2025, 10, 24)));
    }

    @Test
    void parseGameCounts_returnsEmptyForNullInput() {
        assertTrue(client.parseGameCounts(null).isEmpty());
    }

    @Test
    void parseGameCounts_returnsEmptyWhenGameDatesMissing() {
        ObjectNode schedule = objectMapper.createObjectNode();
        schedule.set("leagueSchedule", objectMapper.createObjectNode());
        assertTrue(client.parseGameCounts(schedule).isEmpty());
    }

    @Test
    void parseGameCounts_skipsMalformedDates() {
        ObjectNode schedule = buildSchedule(
                gameDate("not-a-date", 5),
                gameDate("11/01/2025 00:00:00", 4)
        );

        Map<LocalDate, Integer> counts = client.parseGameCounts(schedule);

        assertEquals(1, counts.size());
        assertEquals(4, counts.get(LocalDate.of(2025, 11, 1)));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ObjectNode buildSchedule(ObjectNode... gameDates) {
        ObjectNode schedule = objectMapper.createObjectNode();
        ObjectNode leagueSchedule = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        for (ObjectNode gd : gameDates) arr.add(gd);
        leagueSchedule.set("gameDates", arr);
        schedule.set("leagueSchedule", leagueSchedule);
        return schedule;
    }

    private ObjectNode gameDate(String gameDate, int gameCount) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("gameDate", gameDate);
        ArrayNode games = objectMapper.createArrayNode();
        for (int i = 0; i < gameCount; i++) {
            ObjectNode game = objectMapper.createObjectNode();
            game.put("gameId", String.format("00226%05d", i));
            games.add(game);
        }
        node.set("games", games);
        return node;
    }
}
