package com.nba.sdui.domain.port;

import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Port for non-scoreboard stats reads (boxscore, season schedule).
 *
 * <p>Domain composers depend on this port; the wiring lives in
 * {@code com.nba.sdui.remote} (see {@code StatsApiAdapter}). Boxscore is
 * served as a typed {@link BoxscoreResponse} so SAF's L2 cache stays
 * within its {@code com.nba.*} polymorphic-type allowlist.
 *
 * <p>TODO: re-evaluate the {@code *Port} suffix. See note on
 * {@link ScoreboardPort} — nba-client-backend prefers role-named domain
 * seams ({@code *Lookup}, {@code *Counter}, {@code *Classifier}) and uses
 * {@code *Port} only as a fallback. {@code BoxscoreReader} or splitting
 * this into role-named seams may fit better.
 */
public interface StatsPort {

    /** Boxscore for a specific game from the public CDN. */
    BoxscoreResponse getBoxscore(String gameId) throws IOException;

    /**
     * Map of league-date → game count for the current season, derived from
     * the public CDN league schedule. Empty map if unavailable.
     */
    Map<LocalDate, Integer> getSeasonGameCounts() throws IOException;
}
