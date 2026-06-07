package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outer envelope for the NBA scoreboard payload. The CDN
 * {@code todaysScoreboard_00.json} returns
 * {@code {"meta":{...},"scoreboard":{...}}}; the core-api
 * {@code gameCardFeed} response is normalized into the same shape.
 *
 * <p>{@link #getGames()} is a convenience accessor that drills through the
 * inner {@link Scoreboard} so composers don't repeat the nested traversal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreboardResponse {

    private Scoreboard scoreboard;

    /** Returns the inner games list, or empty if the envelope or list is absent. */
    @JsonIgnore
    public List<Game> getGames() {
        if (scoreboard == null || scoreboard.getGames() == null) {
            return List.of();
        }
        return scoreboard.getGames();
    }

    /** Returns the inner gameDate, or null if the envelope is absent. */
    @JsonIgnore
    public String getGameDate() {
        return scoreboard == null ? null : scoreboard.getGameDate();
    }
}
