package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Structured broadcaster block on a scoreboard {@link Game}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Broadcasters {

    @Builder.Default
    private List<Broadcaster> nationalTvBroadcasters = List.of();
}
