package com.nba.sdui.integration.model.boxscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Boxscore statistics shape — used for both per-player and per-team
 * totals (the upstream emits the same object structure in both places).
 * Primitive defaults (0 / 0.0) match the Jackson-on-missing behavior the
 * legacy {@code path(...).asInt()} reads relied on.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BoxscoreStatistics {
    /** ISO-8601 duration, e.g. {@code PT34M12.00S}. */
    private String minutes;
    private int points;
    private int reboundsTotal;
    private int reboundsOffensive;
    private int reboundsDefensive;
    private int assists;
    private int steals;
    private int blocks;
    private int turnovers;
    private int foulsPersonal;
    private int fieldGoalsMade;
    private int fieldGoalsAttempted;
    private double fieldGoalsPercentage;
    private int threePointersMade;
    private int threePointersAttempted;
    private double threePointersPercentage;
    private int freeThrowsMade;
    private int freeThrowsAttempted;
    private double freeThrowsPercentage;
    /** Upstream emits as decimal; cast via {@code (int)} at use site if needed. */
    private double plusMinusPoints;
}
