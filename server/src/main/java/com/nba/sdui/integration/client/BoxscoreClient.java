package com.nba.sdui.integration.client;

import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;

import java.io.IOException;

/**
 * Typed boxscore upstream client. Wraps the public NBA CDN
 * {@code /boxscore/boxscore_<gameId>.json} endpoint and returns a
 * fully-typed {@link BoxscoreResponse} so SAF's L2 cache can serialize
 * the value under its {@code com.nba.*} polymorphic-type allowlist.
 */
public interface BoxscoreClient {
    /**
     * Fetch the boxscore for the given game id.
     *
     * @return parsed envelope; {@code null} if the upstream returned no
     *         payload (e.g. game not found, network failure swallowed
     *         by the underlying client).
     */
    BoxscoreResponse getBoxscore(String gameId) throws IOException;
}
