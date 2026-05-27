package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Client for NBA Stats APIs.
 *
 * <p>Uses three sources:
 * <ol>
 *   <li>CDN endpoints (public, no auth) — for today's scoreboard, basic boxscore</li>
 *   <li>Stats Trafficcop (OPIM auth required) — for detailed stats</li>
 *   <li>NBA Core API (OPIM auth required) — for per-date scoreboards via
 *       {@code /cp/api/v1.9/feeds/gamecardfeed?gamedate=MM/DD/YYYY&platform=web}.
 *       Response shape (modules→cards→cardData) is normalized to the CDN
 *       scoreboard shape by {@link #normalizeCoreApiFeedToScoreboard}.</li>
 * </ol>
 *
 * <p>Per-date fetches require {@code CORE_API_OPIM_SUBSCRIPTION_KEY} in the
 * environment. When the key is missing or the upstream call fails, the
 * per-date path returns {@code null} — callers must render an empty/no-games
 * state rather than synthesizing fake content.
 */
@Component
public class StatsApiClient {

    private static final Logger log = LoggerFactory.getLogger(StatsApiClient.class);

    // Public CDN endpoints (no auth)
    private static final String CDN_BASE = "https://cdn.nba.com/static/json/liveData";
    private static final String SCOREBOARD_URL = CDN_BASE + "/scoreboard/todaysScoreboard_00.json";
    private static final String LEAGUE_SCHEDULE_URL =
            "https://cdn.nba.com/static/json/staticData/scheduleLeagueV2.json";
    // Game date format used in the league schedule JSON
    private static final DateTimeFormatter SCHEDULE_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
    private static final String CORE_API_GAMECARDFEED_PATH = "/cp/api/v1.9/feeds/gamecardfeed";
    private static final String CORE_API_PLATFORM_VALUE = "web";
    // Game IDs are 10-digit numeric strings (e.g., "0022400892")
    private static final Pattern GAME_ID_PATTERN = Pattern.compile("^\\d{10}$");

    // Stats Trafficcop (OPIM auth required)
    @Value("${stats.api.base-url:https://stats-trafficcop-prod.nba.com/v0/api}")
    private String statsApiBaseUrl;

    @Value("${stats.api.subscription-key:}")
    private String subscriptionKey;

    // NBA Core API (OPIM auth required — per-date scoreboards)
    @Value("${core.api.base-url:https://core-api.nba.com}")
    private String coreApiBaseUrl;

    @Value("${core.api.subscription-key:}")
    private String coreApiSubscriptionKey;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SeasonCalendarService seasonCalendarService;

    public StatsApiClient(ObjectMapper objectMapper, SeasonCalendarService seasonCalendarService) {
        this.objectMapper = objectMapper;
        this.seasonCalendarService = seasonCalendarService;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Fetch today's scoreboard (all games) from public CDN.
     */
    public JsonNode getScoreboard() throws IOException {
        return fetchCdnJson(SCOREBOARD_URL);
    }

    /**
     * Fetch boxscore data for a specific game from public CDN.
     * 
     * @param gameId The game ID (e.g., "0022400892")
     * @return Boxscore JSON or null if unavailable
     */
    public JsonNode getBoxscore(String gameId) throws IOException {
        validateGameId(gameId);
        String url = CDN_BASE + "/boxscore/boxscore_" + gameId + ".json";
        return fetchCdnJson(url);
    }
    
    /**
     * Fetch detailed boxscore from Stats Trafficcop API (requires OPIM key).
     * 
     * @param gameId The game ID
     * @return Detailed boxscore or null if unavailable
     */
    public JsonNode getDetailedBoxscore(String gameId) throws IOException {
        validateGameId(gameId);
        if (subscriptionKey == null || subscriptionKey.isBlank()) {
            log.warn("Stats API subscription key not configured, falling back to CDN");
            return getBoxscore(gameId);
        }
        
        String url = statsApiBaseUrl + "/stats/boxscore?gameId=" + gameId;
        return fetchStatsApiJson(url);
    }
    
    /**
     * Fetch linescore data from Stats Trafficcop API.
     * 
     * @param gameId The game ID
     * @return Linescore data or null if unavailable
     */
    public JsonNode getLinescore(String gameId) throws IOException {
        validateGameId(gameId);
        if (subscriptionKey == null || subscriptionKey.isBlank()) {
            log.warn("Stats API subscription key not configured");
            return null;
        }
        
        String url = statsApiBaseUrl + "/stats/linescore?gameId=" + gameId;
        return fetchStatsApiJson(url);
    }
    
    /**
     * Fetch the full season schedule from CDN and return a map of league-date to game count.
     *
     * <p>Uses the CDN {@code scheduleLeagueV2.json} static file — a single request for the
     * entire season, avoiding per-date fan-out. Dates with zero games are omitted from the
     * returned map. Returns an empty map if the endpoint is unavailable or parsing fails so
     * callers can render an empty {@code dateMetadata} node rather than failing composition.
     *
     * <p>The returned map keys are ISO-8601 local dates (league timezone, i.e. ET) matching
     * the format emitted by {@link SeasonCalendarService#currentLeagueDate()}.
     */
    public Map<LocalDate, Integer> getSeasonGameCounts() throws IOException {
        JsonNode schedule = fetchCdnJson(LEAGUE_SCHEDULE_URL);
        return parseGameCounts(schedule);
    }

    /**
     * Parse a {@code scheduleLeagueV2.json} response into a map of date → game count.
     * Package-private for unit testing without HTTP.
     */
    Map<LocalDate, Integer> parseGameCounts(JsonNode schedule) {
        if (schedule == null) return Collections.emptyMap();
        JsonNode gameDates = schedule.path("leagueSchedule").path("gameDates");
        if (!gameDates.isArray()) return Collections.emptyMap();

        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (JsonNode dateNode : gameDates) {
            String gameDateStr = dateNode.path("gameDate").asText(null);
            if (gameDateStr == null || gameDateStr.isBlank()) continue;
            JsonNode games = dateNode.path("games");
            if (!games.isArray() || games.size() == 0) continue;
            try {
                LocalDate date = LocalDate.parse(gameDateStr, SCHEDULE_DATE_FMT);
                counts.put(date, games.size());
            } catch (Exception e) {
                log.warn("Could not parse gameDate '{}' in season schedule", gameDateStr);
            }
        }
        return counts;
    }

    /**
     * Find a specific game from the scoreboard.
     * 
     * @param gameId The game ID
     * @return Game data or null if not found
     */
    public JsonNode getGameFromScoreboard(String gameId) throws IOException {
        validateGameId(gameId);
        JsonNode scoreboard = getScoreboard();
        if (scoreboard == null) {
            return null;
        }
        
        JsonNode games = scoreboard.path("scoreboard").path("games");
        if (games.isMissingNode() || !games.isArray()) {
            return null;
        }
        
        for (JsonNode game : games) {
            if (gameId.equals(game.path("gameId").asText())) {
                return game;
            }
        }
        
        log.warn("Game {} not found in today's scoreboard", gameId);
        return null;
    }
    
    /**
     * Fetch the scoreboard for a specific date.
     *
     * <p>When {@code date} equals today's league date (ET), delegates to
     * {@link #getScoreboard()} which hits the CDN — the canonical live-data
     * source that SSE channels are keyed against. For any other date, calls
     * the NBA Core API {@code gameCardFeed} endpoint and normalizes via
     * {@link #normalizeCoreApiFeedToScoreboard}.
     *
     * <p>Returns {@code null} if the Core API call fails, the key is not
     * configured, or the response cannot be normalized. Callers must render an
     * empty/no-games state — composers must not synthesize fake content.
     *
     * <p>The returned JSON is normalized to the same
     * {@code { "scoreboard": { "games": [...] } }} shape that
     * {@link #getScoreboard()} returns.
     *
     * @param date the game date (ISO-8601 format, e.g. 2026-05-26)
     * @return scoreboard JSON or null if unavailable
     */
    public JsonNode getScoreboardForDate(LocalDate date) throws IOException {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }

        if (date.equals(seasonCalendarService.currentLeagueDate())) {
            return getScoreboard();
        }

        if (coreApiSubscriptionKey == null || coreApiSubscriptionKey.isBlank()) {
            log.warn("core.api.subscription-key not configured; per-date scoreboard for {} will return null", date);
            return null;
        }

        DateTimeFormatter mmDdYyyy = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        String url = coreApiBaseUrl
                + CORE_API_GAMECARDFEED_PATH
                + "?gamedate=" + URLEncoder.encode(date.format(mmDdYyyy), StandardCharsets.UTF_8)
                + "&platform=" + CORE_API_PLATFORM_VALUE;

        JsonNode feed = fetchCoreApiJson(url);
        JsonNode normalized = normalizeCoreApiFeedToScoreboard(feed, date);
        if (normalized == null) {
            log.warn("Core API gameCardFeed returned null or unexpected shape for date={}", date);
        }
        return normalized;
    }

    /**
     * Fetch from NBA Core API (with OPIM subscription key, no browser headers).
     */
    private JsonNode fetchCoreApiJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Ocp-Apim-Subscription-Key", coreApiSubscriptionKey)
                .build();
        return executeRequest(request, url);
    }

    /**
     * Normalize a Core API {@code gameCardFeed} response into the CDN-scoreboard
     * shape ({@code {"scoreboard":{"games":[...]}}}) that {@link LiveComposer}
     * already consumes.
     *
     * <p>Maps {@code modules[].cards[].cardData} (filtered to
     * {@code cardType == "game"}) into {@code scoreboard.games[]}. Non-game cards
     * (ads, ratings, etc.) are dropped. The wrapped game objects pass through
     * verbatim — they are field-compatible with the CDN scoreboard shape.
     *
     * @param feed the raw Core API JSON response (may be null)
     * @param date the requested game date (used for the {@code gameDate} field)
     * @return normalized scoreboard JSON, or null if the feed is null or has no modules array
     */
    JsonNode normalizeCoreApiFeedToScoreboard(JsonNode feed, LocalDate date) {
        if (feed == null) return null;
        JsonNode modules = feed.path("modules");
        if (!modules.isArray()) return null;

        ObjectNode scoreboard = objectMapper.createObjectNode();
        ObjectNode scoreboardData = objectMapper.createObjectNode();
        scoreboardData.put("gameDate", date.toString());
        ArrayNode games = objectMapper.createArrayNode();

        for (JsonNode module : modules) {
            JsonNode cards = module.path("cards");
            if (!cards.isArray()) continue;
            for (JsonNode card : cards) {
                if (!"game".equals(card.path("cardType").asText())) continue;
                JsonNode cardData = card.path("cardData");
                if (cardData.isMissingNode() || !cardData.isObject()) continue;
                games.add(cardData);
            }
        }

        scoreboardData.set("games", games);
        scoreboard.set("scoreboard", scoreboardData);
        return scoreboard;
    }

    // Akamai (cdn.nba.com) rejects non-browser User-Agents with 403. The CDN
    // also requires Origin + Referer matching nba.com — minimum tested set
    // that passes is UA + Origin + Referer; UA alone or UA + Origin still 403.
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";
    private static final String NBA_ORIGIN = "https://www.nba.com";
    private static final String NBA_REFERER = "https://www.nba.com/";

    /**
     * Fetch from public CDN (no auth).
     */
    private JsonNode fetchCdnJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "application/json,text/plain,*/*")
                .header("Origin", NBA_ORIGIN)
                .header("Referer", NBA_REFERER)
                .build();

        return executeRequest(request, url);
    }

    /**
     * Fetch from Stats Trafficcop API (with OPIM subscription key).
     */
    private JsonNode fetchStatsApiJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Accept", "application/json,text/plain,*/*")
                .header("Origin", NBA_ORIGIN)
                .header("Referer", NBA_REFERER)
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .build();

        return executeRequest(request, url);
    }
    
    private JsonNode executeRequest(Request request, String url) throws IOException {
        log.debug("Fetching: {}", url);
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("API returned status {} for {}", response.code(), url);
                return null;
            }
            
            String body = response.body() != null ? response.body().string() : null;
            if (body == null || body.isEmpty()) {
                return null;
            }
            
            return objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    private void validateGameId(String gameId) {
        if (gameId == null || !GAME_ID_PATTERN.matcher(gameId).matches()) {
            throw new IllegalArgumentException("Invalid game ID format: " + gameId);
        }
    }
}
