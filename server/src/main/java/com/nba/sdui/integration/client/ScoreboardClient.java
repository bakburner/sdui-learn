package com.nba.sdui.integration.client;

import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Client for NBA scoreboard feeds. Implementations fetch from the public
 * CDN ({@code todaysScoreboard_00.json}) for today's games and from the
 * core-api {@code gameCardFeed} (normalized to the same shape) for other
 * dates.
 *
 * <p>Returns {@code null} when the upstream is unavailable (e.g. missing
 * OPIM key, off-season date). Callers must surface an empty/no-games
 * state rather than synthesize fake content.
 */
public interface ScoreboardClient {

    /** Today's scoreboard from the CDN. */
    ScoreboardResponse getTodayScoreboard() throws IOException;

    /**
     * Scoreboard for a specific date. Today's date short-circuits to
     * {@link #getTodayScoreboard()}; other dates hit the core-api.
     */
    ScoreboardResponse getScoreboardForDate(LocalDate date) throws IOException;
}
