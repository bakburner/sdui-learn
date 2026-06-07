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
public class BoxscorePlayer {
    private Integer personId;
    /** "0" or "1"; some payloads serialize as the strings the upstream uses. */
    private String played;
    private String notPlayingReason;
    private String name;
    private String nameI;
    private String firstName;
    private String familyName;
    private String position;
    private String jerseyNum;
    /** "0" or "1" — upstream uses string. */
    private String starter;
    private BoxscoreStatistics statistics;
}
