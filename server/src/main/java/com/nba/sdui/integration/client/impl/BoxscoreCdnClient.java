package com.nba.sdui.integration.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.sdui.integration.client.BoxscoreClient;
import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;
import com.nba.sdui.remote.StatsApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Default {@link BoxscoreClient} that bridges the legacy raw-JSON
 * {@link StatsApiClient#getBoxscore(String)} fetcher and converts to a
 * typed {@link BoxscoreResponse} at the boundary.
 */
@Component
public class BoxscoreCdnClient implements BoxscoreClient {

    private static final Logger log = LoggerFactory.getLogger(BoxscoreCdnClient.class);

    private final StatsApiClient statsApiClient;
    private final ObjectMapper objectMapper;

    public BoxscoreCdnClient(StatsApiClient statsApiClient, ObjectMapper objectMapper) {
        this.statsApiClient = statsApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public BoxscoreResponse getBoxscore(String gameId) throws IOException {
        return toBoxscoreResponse(statsApiClient.getBoxscore(gameId));
    }

    private BoxscoreResponse toBoxscoreResponse(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, BoxscoreResponse.class);
        } catch (Exception e) {
            log.warn("Failed to decode boxscore JSON into BoxscoreResponse: {}", e.getMessage());
            return null;
        }
    }
}
