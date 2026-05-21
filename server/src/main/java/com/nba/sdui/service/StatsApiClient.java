package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Client for NBA Stats APIs.
 * 
 * Uses two sources:
 * 1. CDN endpoints (public, no auth) - for scoreboard, basic boxscore
 * 2. Stats Trafficcop (OPIM auth required) - for detailed stats
 */
@Component
public class StatsApiClient {

    private static final Logger log = LoggerFactory.getLogger(StatsApiClient.class);
    
    // Public CDN endpoints (no auth)
    private static final String CDN_BASE = "https://cdn.nba.com/static/json/liveData";
    private static final String SCOREBOARD_URL = CDN_BASE + "/scoreboard/todaysScoreboard_00.json";
    // Game IDs are 10-digit numeric strings (e.g., "0022400892")
    private static final Pattern GAME_ID_PATTERN = Pattern.compile("^\\d{10}$");
    
    // Stats Trafficcop (OPIM auth required)
    @Value("${stats.api.base-url:https://stats-trafficcop-prod.nba.com/v0/api}")
    private String statsApiBaseUrl;
    
    @Value("${stats.api.subscription-key:}")
    private String subscriptionKey;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StatsApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
     * Fetch from public CDN (no auth).
     */
    private JsonNode fetchCdnJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "SDUI-Prototype/1.0")
                .build();
        
        return executeRequest(request, url);
    }
    
    /**
     * Fetch from Stats Trafficcop API (with OPIM subscription key).
     */
    private JsonNode fetchStatsApiJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "SDUI-Prototype/1.0")
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
