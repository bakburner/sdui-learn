package com.nba.sdui.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.domain.port.StatsPort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Adapter wiring the domain ports to the existing {@link StatsApiClient}.
 *
 * <p>Lets composers depend on {@link ScoreboardPort} / {@link StatsPort}
 * without pulling the HTTP client into the domain. The client is
 * {@code RestClient}-backed as of A2b.3.
 */
@Component
public class StatsApiAdapter implements ScoreboardPort, StatsPort {

    private final StatsApiClient client;

    public StatsApiAdapter(StatsApiClient client) {
        this.client = client;
    }

    @Override
    public JsonNode getScoreboard() throws IOException {
        return client.getScoreboard();
    }

    @Override
    public JsonNode getScoreboardForDate(LocalDate date) throws IOException {
        return client.getScoreboardForDate(date);
    }

    @Override
    public JsonNode getBoxscore(String gameId) throws IOException {
        return client.getBoxscore(gameId);
    }

    @Override
    public Map<LocalDate, Integer> getSeasonGameCounts() throws IOException {
        return client.getSeasonGameCounts();
    }
}
