package com.nba.sdui.core.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SDUI Screen - Top-level container for a full page/view.
 * 
 * This is a hand-written Kotlin model for the prototype.
 * In production, this would be generated from the JSON Schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SduiScreen(
    @JsonProperty("id") val id: String,
    @JsonProperty("schemaVersion") val schemaVersion: String,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("analyticsId") val analyticsId: String? = null,
    @JsonProperty("traceId") val traceId: String? = null,
    @JsonProperty("defaultRefreshPolicy") val defaultRefreshPolicy: RefreshPolicy? = null,
    @JsonProperty("navigation") val navigation: Navigation? = null,
    @JsonProperty("state") val state: Map<String, Any>? = null,
    @JsonProperty("sections") val sections: List<SduiSection> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Navigation(
    @JsonProperty("items") val items: List<NavigationItem> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NavigationItem(
    @JsonProperty("id") val id: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("icon") val icon: String? = null,
    @JsonProperty("targetUri") val targetUri: String? = null,
    @JsonProperty("selected") val selected: Boolean = false,
    @JsonProperty("children") val children: List<NavigationItem> = emptyList()
)

/**
 * SDUI Section - Self-contained, reusable unit within a screen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SduiSection(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("analyticsId") val analyticsId: String? = null,
    @JsonProperty("refreshPolicy") val refreshPolicy: RefreshPolicy? = null,
    @JsonProperty("dataBindings") val dataBinding: DataBinding? = null,
    @JsonProperty("actions") val actions: List<Map<String, Any?>>? = null,
    @JsonProperty("subsections") val subsections: List<Subsection>? = null,
    @JsonProperty("padding") val padding: Spacing? = null,
    @JsonProperty("backgroundColor") val backgroundColor: String? = null,
    @JsonProperty("data") val data: Map<String, Any?>? = null
)

/**
 * Refresh Policy - Defines how a section receives updates.
 * 
 * Supports multiple update strategies:
 * - static: No updates
 * - poll: Periodic polling (to SDUI endpoint or direct URL)
 * - sse: Real-time via Ably channel
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RefreshPolicy(
    @JsonProperty("type") val type: String, // "static", "poll", "sse"
    @JsonProperty("intervalMs") val intervalMs: Int? = null,
    @JsonProperty("url") val url: String? = null, // Direct URL to poll (if null, polls SDUI endpoint)
    @JsonProperty("channel") val channel: String? = null, // Ably channel name
    @JsonProperty("dataPath") val dataPath: String? = null // JSONPath to extract data from response
)

/**
 * Data Binding - Maps real-time message fields to component properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DataBinding(
    @JsonProperty("bindings") val bindings: List<DataBindingPath> = emptyList(),
    @JsonProperty("stringKeys") val stringKeys: Map<String, String>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataBindingPath(
    @JsonProperty("sourcePath") val sourcePath: String,
    @JsonProperty("targetPath") val targetPath: String
)

/**
 * Subsection - Nested interaction target within a section.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Subsection(
    @JsonProperty("id") val id: String,
    @JsonProperty("actions") val actions: List<Map<String, Any?>>? = null
)

/**
 * Spacing - Padding/margin values.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Spacing(
    @JsonProperty("top") val top: Int = 0,
    @JsonProperty("bottom") val bottom: Int = 0,
    @JsonProperty("start") val start: Int = 0,
    @JsonProperty("end") val end: Int = 0
)

/**
 * Common enums for styling.
 */
enum class TextVariant {
    HEADING1, HEADING2, HEADING3, BODY, BODY_SMALL, CAPTION, LABEL, SCORE
}

enum class ButtonVariant {
    PRIMARY, SECONDARY, TERTIARY, TEXT
}
