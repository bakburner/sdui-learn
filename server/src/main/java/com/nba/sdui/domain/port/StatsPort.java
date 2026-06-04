package com.nba.sdui.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Port for non-scoreboard stats reads (boxscore, season schedule).
 *
 * <p>Domain composers depend on this port; the wiring lives in
 * {@code com.nba.sdui.remote} (see {@code StatsApiAdapter}). Return types are
 * {@link JsonNode} / map-of-counts to match current usage; the typed migration
 * is Phase A3 of the SAF/codegen plan.
 */
public interface StatsPort {

    /** Boxscore for a specific game from the public CDN. */
    JsonNode getBoxscore(String gameId) throws IOException;

    /**
     * Map of league-date → game count for the current season, derived from
     * the public CDN league schedule. Empty map if unavailable.
     */
    Map<LocalDate, Integer> getSeasonGameCounts() throws IOException;
}
