package com.nba.sdui.integration.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.sdui.integration.client.ScoreboardClient;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import com.nba.sdui.remote.StatsApiClient;
import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link ScoreboardClient} bridging the legacy {@link StatsApiClient}
 * raw-JSON fetcher and converting at the boundary into typed
 * {@link ScoreboardResponse} DTOs.
 *
 * <p>The conversion lives here (not inside {@code StatsApiClient}) so the
 * underlying fetcher can stay focused on HTTP + per-feed normalization
 * while typed callers — and SAF's L2 cache — see only {@code com.nba.sdui.*}
 * types that satisfy SAF's polymorphic-type allowlist.
 */
@Component
public class ScoreboardCdnClient implements ScoreboardClient {

    private static final Logger log = LoggerFactory.getLogger(ScoreboardCdnClient.class);

    private final StatsApiClient statsApiClient;
    private final ObjectMapper objectMapper;

    public ScoreboardCdnClient(StatsApiClient statsApiClient, ObjectMapper objectMapper) {
        this.statsApiClient = statsApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScoreboardResponse getTodayScoreboard() throws IOException {
        return toScoreboardResponse(statsApiClient.getScoreboard());
    }

    @Override
    public ScoreboardResponse getScoreboardForDate(LocalDate date) throws IOException {
        return toScoreboardResponse(statsApiClient.getScoreboardForDate(date));
    }

    private ScoreboardResponse toScoreboardResponse(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, ScoreboardResponse.class);
        } catch (Exception e) {
            log.warn("Failed to decode scoreboard JSON into ScoreboardResponse: {}", e.getMessage());
            return null;
        }
    }
}
