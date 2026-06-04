package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-team data on a scoreboard {@link Game}. The boxscore feed carries a
 * richer team shape (with {@code players[]} etc.) and is modelled
 * separately in the boxscore package.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreboardTeam {

    private String teamId;
    private String teamTricode;
    private String teamName;

    @Builder.Default
    private int score = 0;

    /** Optional pre-resolved logo URL; absent for most upstream payloads. */
    private String logoUrl;

    /** Optional playoff seed fallbacks (composer prefers these over playoffSeries). */
    private String playoffRank;
    private String seed;
    private String playoffSeed;
}
