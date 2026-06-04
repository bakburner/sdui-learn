package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inner scoreboard object holding the date and game list. Wire shape:
 * {@code {"gameDate":"2026-06-04","games":[...]}}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Scoreboard {

    private String gameDate;

    @Builder.Default
    private List<Game> games = List.of();
}
