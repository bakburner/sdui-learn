package com.nba.sdui.core.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * AtomicElement — a server-composed UI primitive.
 *
 * These are the building blocks of the atomic rendering layer. The server
 * composes trees of these elements and sends them as the `data.root` of an
 * `AtomicComposite` section. The client's AtomicRouter renders the tree
 * using platform-native primitives.
 *
 * Types: Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid, SectionSlot
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessibilityProperties(
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("role") val role: String? = null,
    @JsonProperty("hidden") val hidden: Boolean? = null,
    @JsonProperty("headingLevel") val headingLevel: Int? = null,
    @JsonProperty("liveRegion") val liveRegion: String? = null,
    @JsonProperty("sortOrder") val sortOrder: Int? = null,
    @JsonProperty("hint") val hint: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AtomicElement(
    @JsonProperty("type") val type: String,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("accessibility") val accessibility: AccessibilityProperties? = null,

    // Container properties
    @JsonProperty("children") val children: List<AtomicElement>? = null,
    @JsonProperty("direction") val direction: String? = null, // "row" | "column"
    @JsonProperty("alignment") val alignment: String? = null, // "start" | "center" | "end" | "spaceBetween" | "spaceAround" | "spaceEvenly"
    @JsonProperty("crossAlignment") val crossAlignment: String? = null, // "start" | "center" | "end" | "stretch"
    @JsonProperty("gap") val gap: Int? = null,
    @JsonProperty("padding") val padding: Spacing? = null,
    @JsonProperty("margin") val margin: Spacing? = null,
    @JsonProperty("background") val background: Any? = null,
    @JsonProperty("cornerRadius") val cornerRadius: Int? = null,
    @JsonProperty("cornerRadii") val cornerRadii: CornerRadii? = null,

    // Text properties
    @JsonProperty("content") val content: String? = null,
    @JsonProperty("variant") val variant: String? = null, // TextVariant
    @JsonProperty("weight") val weight: String? = null, // TextWeight
    @JsonProperty("color") val color: String? = null,
    @JsonProperty("maxLines") val maxLines: Int? = null,

    // Image properties
    @JsonProperty("src") val src: String? = null,
    @JsonProperty("aspectRatio") val aspectRatio: Float? = null,
    @JsonProperty("fit") val fit: String? = null, // "cover" | "contain" | "fill" | "none"
    @JsonProperty("placeholder") val placeholder: String? = null,
    @JsonProperty("alt") val alt: String? = null,
    @JsonProperty("width") val width: Int? = null,
    @JsonProperty("height") val height: Int? = null,

    // Button properties
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("icon") val icon: String? = null,
    @JsonProperty("disabled") val disabled: Boolean? = null,

    // Divider properties
    @JsonProperty("orientation") val orientation: String? = null, // "horizontal" | "vertical"
    @JsonProperty("thickness") val thickness: Int? = null,

    // Spacer properties
    @JsonProperty("size") val size: Int? = null,

    // ScrollContainer properties
    @JsonProperty("paging") val paging: Boolean? = null,
    @JsonProperty("snapAlignment") val snapAlignment: String? = null, // "start" | "center" | "end"

    // Conditional properties
    @JsonProperty("condition") val condition: String? = null,
    @JsonProperty("trueChild") val trueChild: AtomicElement? = null,
    @JsonProperty("falseChild") val falseChild: AtomicElement? = null,

    // DisplayGrid properties
    @JsonProperty("columns") val columns: List<DisplayGridColumn>? = null,
    @JsonProperty("rows") val rows: List<Map<String, String>>? = null,
    @JsonProperty("striped") val striped: Boolean? = null,

    // SectionSlot — embedded section delegated back to SectionRouter
    @JsonProperty("section") val section: Map<String, Any?>? = null,

    // Layout properties
    @JsonProperty("flex") val flex: Float? = null, // flex grow factor for Container children
    @JsonProperty("breakpoint") val breakpoint: Int? = null, // responsive direction flip threshold (dp)
    @JsonProperty("fillWidth") val fillWidth: Boolean? = null,

    // Actions (applicable to any interactive element)
    @JsonProperty("actions") val actions: List<Map<String, Any?>>? = null,

    // Phase 0.4 styling properties
    @JsonProperty("opacity") val opacity: Double? = null,
    @JsonProperty("shadow") val shadow: Shadow? = null,
    @JsonProperty("badge") val badge: Badge? = null,
    @JsonProperty("textAlign") val textAlign: String? = null,
    @JsonProperty("showIndicators") val showIndicators: Boolean? = null,
    @JsonProperty("monospacedDigits") val monospacedDigits: Boolean? = null
)

/**
 * Per-corner cornerRadius override. When present, takes precedence over the
 * single-value `cornerRadius`; any corner key omitted falls back to `cornerRadius`
 * (or 0 if that is also absent). Maps to Compose RoundedCornerShape's four-corner
 * constructor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CornerRadii(
    @JsonProperty("topStart") val topStart: Int? = null,
    @JsonProperty("topEnd") val topEnd: Int? = null,
    @JsonProperty("bottomStart") val bottomStart: Int? = null,
    @JsonProperty("bottomEnd") val bottomEnd: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Shadow(
    @JsonProperty("color") val color: String? = null,
    @JsonProperty("radius") val radius: Double? = null,
    @JsonProperty("offsetX") val offsetX: Double? = null,
    @JsonProperty("offsetY") val offsetY: Double? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Badge(
    @JsonProperty("element") val element: AtomicElement? = null,
    @JsonProperty("alignment") val alignment: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BackgroundGradient(
    @JsonProperty("colors") val colors: List<String>,
    @JsonProperty("direction") val direction: String? = "vertical" // "horizontal" | "vertical" | "diagonal"
)

sealed class Background {
    data class Solid(val color: String) : Background()
    data class Gradient(val gradient: BackgroundGradient) : Background()
    data class Image(val imageUrl: String, val scaleType: String = "cover",
                     val overlay: Background? = null) : Background()
}

@Suppress("UNCHECKED_CAST")
fun parseBackground(raw: Any?): Background? {
    if (raw == null) return null
    if (raw is String) return Background.Solid(raw)
    if (raw is Map<*, *>) {
        val map = raw as Map<String, Any?>
        if (map.containsKey("imageUrl")) {
            return Background.Image(
                imageUrl = map["imageUrl"] as String,
                scaleType = (map["scaleType"] as? String) ?: "cover",
                overlay = parseBackground(map["overlay"])
            )
        }
        if (map.containsKey("colors")) {
            val colors = (map["colors"] as? List<*>)?.filterIsInstance<String>() ?: return null
            val direction = (map["direction"] as? String) ?: "vertical"
            return Background.Gradient(BackgroundGradient(colors, direction))
        }
    }
    return null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class DisplayGridColumn(
    @JsonProperty("key") val key: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("align") val align: String? = "start", // "start" | "center" | "end"
    @JsonProperty("width") val width: Any? = null // Int (fixed dp) or "flex"
)
