package com.nba.sdui.integration.model.boxscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxscoreTeam {
    private Integer teamId;
    private String teamTricode;
    private String teamName;
    private String teamCity;
    private Integer score;
    @Builder.Default
    private List<BoxscorePlayer> players = List.of();
    private BoxscoreStatistics statistics;
}
