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
    @JsonProperty("parentUri") val parentUri: String? = null,
    @JsonProperty("defaultRefreshPolicy") val defaultRefreshPolicy: RefreshPolicy? = null,
    @JsonProperty("navigation") val navigation: Navigation? = null,
    @JsonProperty("variants") val variants: SduiVariants? = null,
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
 * Variants wrapper provided by the server for A/B experimentation.
 * Contains the experiment ID (key for the experiments map) and available options.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SduiVariants(
    @JsonProperty("experimentId") val experimentId: String,
    @JsonProperty("options") val options: List<SduiVariant> = emptyList()
)

/**
 * Variant descriptor provided by the server for A/B experimentation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SduiVariant(
    @JsonProperty("id") val id: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("description") val description: String? = null
)

/**
 * SDUI Section - Self-contained, reusable unit within a screen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SduiSection(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("analyticsId") val analyticsId: String? = null,
    @JsonProperty("accessibility") val accessibility: AccessibilityProperties? = null,
    @JsonProperty("refreshPolicy") val refreshPolicy: RefreshPolicy? = null,
    @JsonProperty("dataBinding") val dataBinding: DataBinding? = null,
    @JsonProperty("actions") val actions: List<Map<String, Any?>>? = null,
    @JsonProperty("subsections") val subsections: List<Subsection>? = null,
    @JsonProperty("padding") val padding: Spacing? = null,
    @JsonProperty("backgroundColor") val backgroundColor: String? = null,
    @JsonProperty("display") val display: SectionDisplay? = null,
    @JsonProperty("layoutHints") val layoutHints: SectionLayoutHints? = null,
    @JsonProperty("sectionStates") val sectionStates: SectionStates? = null,
    @JsonProperty("stringTable") val stringTable: Map<String, String>? = null,
    @JsonProperty("data") val data: Map<String, Any?>? = null
)

/**
 * Outer-chrome spec applied by SectionRouter to every permanent section.
 * Mirrors AtomicContainer's inline-chrome vocabulary so permanent
 * sections have schema parity with composed sections. The shared
 * `SectionContainer` composable reads these fields; permanent-section
 * renderers never set their own outer padding, margin, corner radius,
 * shadow, border, or background. See AGENTS.md §15.3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SectionDisplay(
    @JsonProperty("margin") val margin: Spacing? = null,
    @JsonProperty("padding") val padding: Spacing? = null,
    // Raw JSON node — may be a string (token/hex) or an object with
    // `colors` + `direction` (gradient) or `imageUrl` (image). Parse
    // with `parseBackground(...)` at render time (same helper the
    // atomic Container uses).
    @JsonProperty("background") val background: Any? = null,
    @JsonProperty("cornerRadius") val cornerRadius: Int? = null,
    @JsonProperty("shadow") val shadow: Shadow? = null,
    @JsonProperty("border") val border: Border? = null
)

/**
 * Outer stroke applied around a container or section.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Border(
    @JsonProperty("color") val color: String? = null,
    @JsonProperty("width") val width: Double? = null
)

/**
 * Layout hints for section placement. Clients apply best-effort; unknown hints are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SectionLayoutHints(
    @JsonProperty("marginTop") val marginTop: Int? = null,
    @JsonProperty("marginBottom") val marginBottom: Int? = null,
    @JsonProperty("dividerAbove") val dividerAbove: Boolean? = null,
    @JsonProperty("dividerBelow") val dividerBelow: Boolean? = null,
    @JsonProperty("priority") val priority: String? = null
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
    @JsonProperty("dataPath") val dataPath: String? = null, // JSONPath to extract data from response
    @JsonProperty("pauseWhenOffScreen") val pauseWhenOffScreen: Boolean = true // Pause refresh when section scrolls off-screen
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
    @JsonProperty("accessibility") val accessibility: AccessibilityProperties? = null,
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

/**
 * Section States — server-declared loading and error presentation.
 * Clients render these states when applicable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SectionStates(
    @JsonProperty("loading") val loading: SectionLoading? = null,
    @JsonProperty("error") val error: SectionError? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SectionLoading(
    @JsonProperty("skeleton") val skeleton: String? = null, // "shimmer", "spinner", "placeholder", "none"
    @JsonProperty("minHeightDp") val minHeightDp: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SectionError(
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("retryAction") val retryAction: Map<String, Any?>? = null,
    @JsonProperty("hideOnError") val hideOnError: Boolean? = false
)
