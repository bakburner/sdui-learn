package com.nba.sdui.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Port for scoreboard-shaped reads (today's scoreboard + per-date scoreboards).
 *
 * <p>Domain composers depend on this port; the wiring lives in
 * {@code com.nba.sdui.remote} (see {@code StatsApiAdapter}). Return types are
 * {@link JsonNode} to preserve the current pre-codegen wire shape; the typed
 * migration is Phase A3 of the SAF/codegen plan.
 */
public interface ScoreboardPort {

    /** Today's scoreboard from the public CDN. */
    JsonNode getScoreboard() throws IOException;

    /**
     * Scoreboard for a specific date. Returns {@code null} when the upstream
     * is unavailable (e.g. missing OPIM key, off-season date) — callers must
     * render an empty/no-games state rather than synthesize fake content.
     */
    JsonNode getScoreboardForDate(LocalDate date) throws IOException;
}
