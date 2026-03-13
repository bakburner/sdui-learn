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
 * Types: Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AtomicElement(
    @JsonProperty("type") val type: String,
    @JsonProperty("id") val id: String? = null,

    // Container properties
    @JsonProperty("children") val children: List<AtomicElement>? = null,
    @JsonProperty("direction") val direction: String? = null, // "row" | "column"
    @JsonProperty("alignment") val alignment: String? = null, // "start" | "center" | "end" | "spaceBetween" | "spaceAround" | "spaceEvenly"
    @JsonProperty("crossAlignment") val crossAlignment: String? = null, // "start" | "center" | "end" | "stretch"
    @JsonProperty("gap") val gap: Int? = null,
    @JsonProperty("padding") val padding: Spacing? = null,
    @JsonProperty("backgroundColor") val backgroundColor: String? = null,
    @JsonProperty("backgroundGradient") val backgroundGradient: BackgroundGradient? = null,
    @JsonProperty("cornerRadius") val cornerRadius: Int? = null,

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
    @JsonProperty("buttonVariant") val buttonVariant: String? = null, // "primary" | "secondary" | "tertiary" | "text"
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
    @JsonProperty("headerVariant") val headerVariant: String? = null,
    @JsonProperty("cellVariant") val cellVariant: String? = null,
    @JsonProperty("striped") val striped: Boolean? = null,

    // Actions (applicable to any interactive element)
    @JsonProperty("actions") val actions: List<Map<String, Any?>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BackgroundGradient(
    @JsonProperty("colors") val colors: List<String>,
    @JsonProperty("direction") val direction: String? = "vertical" // "horizontal" | "vertical" | "diagonal"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DisplayGridColumn(
    @JsonProperty("key") val key: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("align") val align: String? = "start", // "start" | "center" | "end"
    @JsonProperty("width") val width: Any? = null // Int (fixed dp) or "flex"
)
