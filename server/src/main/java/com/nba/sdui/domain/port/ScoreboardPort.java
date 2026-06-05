package com.nba.sdui.domain.port;

import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Port for scoreboard-shaped reads (today's scoreboard + per-date scoreboards).
 *
 * <p>Domain composers depend on this port; the wiring lives in
 * {@code com.nba.sdui.remote} (see {@code StatsApiAdapter}). Returns typed
 * DTOs from {@link com.nba.sdui.integration.model.scoreboard}; the integration
 * package mirrors {@code nba-client-backend/integration-models} and will lift
 * to a shared Gradle module when extraction lands.
 *
 * <p>TODO: re-evaluate the {@code *Port} suffix. nba-client-backend's dominant
 * convention for domain seams is role-named ({@code TeamMarketLookup},
 * {@code ActiveStreamCounter}, {@code SkuClassifier}) and reserves
 * {@code *Port} only for "wrap an external system as a domain seam" cases
 * where no clearer role applies (one example: {@code OpinPort}). This
 * interface aggregates two upstreams + SAF orchestration, so {@code *Port}
 * may still be the right fit, but consider e.g. {@code ScoreboardReader}
 * when the cross-codebase audit is done.
 */
public interface ScoreboardPort {

    /** Today's scoreboard from the public CDN. Returns {@code null} when unavailable. */
    ScoreboardResponse getScoreboard() throws IOException;

    /**
     * Scoreboard for a specific date. Returns {@code null} when the upstream
     * is unavailable (e.g. missing OPIM key, off-season date) — callers must
     * render an empty/no-games state rather than synthesize fake content.
     */
    ScoreboardResponse getScoreboardForDate(LocalDate date) throws IOException;
}
