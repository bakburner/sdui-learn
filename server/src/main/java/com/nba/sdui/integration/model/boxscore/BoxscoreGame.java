package com.nba.sdui.integration.model.boxscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxscoreGame {
    private String gameId;
    private int gameStatus;
    private String gameStatusText;
    private String gameClock;
    private BoxscoreTeam homeTeam;
    private BoxscoreTeam awayTeam;
}
