package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single game entry from the scoreboard feed.
 *
 * <p>Only fields consumed by SDUI composers are modelled; the upstream
 * payload carries additional optional fields that are ignored via
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Game {

    private String gameId;

    /** 1 = pre, 2 = live, 3 = final. */
    private int gameStatus;

    private String gameStatusText;

    /** ISO-8601 duration (e.g. {@code PT04M32.00S}); blank for non-live. */
    private String gameClock;

    private ScoreboardTeam awayTeam;
    private ScoreboardTeam homeTeam;

    /** Optional flat broadcaster string. */
    private String broadcasterText;

    /** Optional structured broadcaster block. */
    private Broadcasters broadcasters;

    /** Optional series text (e.g. {@code "WCF, 3-2"}). */
    private String seriesText;

    /** Optional playoff series block carrying per-team seeds. */
    private PlayoffSeries playoffSeries;
}
