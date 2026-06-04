package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-team seeds on a playoff scoreboard {@link Game}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayoffSeries {
    private String awayTeamSeed;
    private String homeTeamSeed;
}
