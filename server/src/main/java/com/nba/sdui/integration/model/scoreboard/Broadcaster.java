package com.nba.sdui.integration.model.scoreboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single broadcaster entry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Broadcaster {
    private String broadcasterDisplay;
    private String broadcasterAbbreviation;
}
