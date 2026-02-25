package com.nba.sdui.core.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Section data types that are not yet fully replaced by codegen due to
 * nested SduiSection references (tab contents). All other section data
 * types (ScoreboardHeaderData, StatLineListData, ContentRailData, etc.)
 * come from codegen: com.nba.sdui.models.generated.*
 */

// ============ TabGroup ============

@JsonIgnoreProperties(ignoreUnknown = true)
data class TabGroupData(
    @JsonProperty("stateKey") val stateKey: String,
    @JsonProperty("defaultTab") val defaultTab: String,
    @JsonProperty("tabs") val tabs: List<TabData> = emptyList(),
    @JsonProperty("tabContents") val tabContents: Map<String, List<SduiSection>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TabData(
    @JsonProperty("id") val id: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("stateKey") val stateKey: String? = null,
    @JsonProperty("stateValue") val stateValue: String? = null
)
