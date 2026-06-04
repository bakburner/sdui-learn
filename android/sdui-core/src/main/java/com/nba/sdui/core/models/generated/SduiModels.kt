// To parse the JSON, install jackson-module-kotlin and do:
//
//   val sduiModels = SduiModels.fromJson(jsonString)

package com.nba.sdui.core.models.generated

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.*
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.*


@Suppress("UNCHECKED_CAST")
private fun <T> ObjectMapper.convert(k: kotlin.reflect.KClass<*>, fromJson: (JsonNode) -> T, toJson: (T) -> String, isUnion: Boolean = false) = registerModule(SimpleModule().apply {
    addSerializer(k.java as Class<T>, object : StdSerializer<T>(k.java as Class<T>) {
            override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider) = gen.writeRawValue(toJson(value))
    })
    addDeserializer(k.java as Class<T>, object : StdDeserializer<T>(k.java as Class<T>) {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext) = fromJson(p.readValueAsTree())
    })
})

val mapper = jacksonObjectMapper().apply {
    propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    convert(Destination::class,            { Destination.fromValue(it.asText()) },            { "\"${it.value}\"" })
    convert(FailureFeedbackStyle::class,   { FailureFeedbackStyle.fromValue(it.asText()) },   { "\"${it.value}\"" })
    convert(ImpressionDedup::class,        { ImpressionDedup.fromValue(it.asText()) },        { "\"${it.value}\"" })
    convert(ModalHeight::class,            { ModalHeight.fromValue(it.asText()) },            { "\"${it.value}\"" })
    convert(FailurePolicy::class,          { FailurePolicy.fromValue(it.asText()) },          { "\"${it.value}\"" })
    convert(MutateOperation::class,        { MutateOperation.fromValue(it.asText()) },        { "\"${it.value}\"" })
    convert(NavigationPresentation::class, { NavigationPresentation.fromValue(it.asText()) }, { "\"${it.value}\"" })
    convert(ActionTrigger::class,          { ActionTrigger.fromValue(it.asText()) },          { "\"${it.value}\"" })
    convert(ActionType::class,             { ActionType.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(RefreshType::class,            { RefreshType.fromValue(it.asText()) },            { "\"${it.value}\"" })
    convert(BadgeAlignment::class,         { BadgeAlignment.fromValue(it.asText()) },         { "\"${it.value}\"" })
    convert(LiveRegion::class,             { LiveRegion.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(Role::class,                   { Role.fromValue(it.asText()) },                   { "\"${it.value}\"" })
    convert(CrossAlignment::class,         { CrossAlignment.fromValue(it.asText()) },         { "\"${it.value}\"" })
    convert(Alignment::class,              { Alignment.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(AspectRatioEnum::class,        { AspectRatioEnum.fromValue(it.asText()) },        { "\"${it.value}\"" })
    convert(Direction::class,              { Direction.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(ScaleType::class,              { ScaleType.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(Align::class,                  { Align.fromValue(it.asText()) },                  { "\"${it.value}\"" })
    convert(WidthEnum::class,              { WidthEnum.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(UIDirection::class,            { UIDirection.fromValue(it.asText()) },            { "\"${it.value}\"" })
    convert(ImageFit::class,               { ImageFit.fromValue(it.asText()) },               { "\"${it.value}\"" })
    convert(Format::class,                 { Format.fromValue(it.asText()) },                 { "\"${it.value}\"" })
    convert(SizingMode::class,             { SizingMode.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(Orientation::class,            { Orientation.fromValue(it.asText()) },            { "\"${it.value}\"" })
    convert(Style::class,                  { Style.fromValue(it.asText()) },                  { "\"${it.value}\"" })
    convert(ShadowType::class,             { ShadowType.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(TickDirection::class,          { TickDirection.fromValue(it.asText()) },          { "\"${it.value}\"" })
    convert(TextWeight::class,             { TextWeight.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(Capability::class,             { Capability.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(FieldType::class,              { FieldType.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(SelectVariant::class,          { SelectVariant.fromValue(it.asText()) },          { "\"${it.value}\"" })
    convert(Layout::class,                 { Layout.fromValue(it.asText()) },                 { "\"${it.value}\"" })
    convert(PlayerType::class,             { PlayerType.fromValue(it.asText()) },             { "\"${it.value}\"" })
    convert(SortDirection::class,          { SortDirection.fromValue(it.asText()) },          { "\"${it.value}\"" })
    convert(Transform::class,              { Transform.fromValue(it.asText()) },              { "\"${it.value}\"" })
    convert(Skeleton::class,               { Skeleton.fromValue(it.asText()) },               { "\"${it.value}\"" })
    convert(LayoutScalar::class,           { LayoutScalar.fromJson(it) },                     { it.toJson() }, true)
    convert(AspectRatioUnion::class,       { AspectRatioUnion.fromJson(it) },                 { it.toJson() }, true)
    convert(BackgroundUnion::class,        { BackgroundUnion.fromJson(it) },                  { it.toJson() }, true)
    convert(Overlay::class,                { Overlay.fromJson(it) },                          { it.toJson() }, true)
    convert(WidthUnion::class,             { WidthUnion.fromJson(it) },                       { it.toJson() }, true)
    convert(ShadowOrToken::class,          { ShadowOrToken.fromJson(it) },                    { it.toJson() }, true)
}

/**
 * Server-Driven UI schema for NBA Game Detail screens
 */
data class SduiModels (
    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     */
    val actions: List<Action>? = null,

    @get:JsonProperty("analyticsId")@field:JsonProperty("analyticsId")
    val analyticsID: String? = null,

    /**
     * Outer padding around the scrollable section feed (start/end/top/bottom). Server emits
     * semantic layout tokens (e.g. token:nba.spacing.md); clients resolve via
     * LayoutTokenResolver. Omit only when the screen is intentionally edge-to-edge.
     */
    val contentInsets: Spacing? = null,

    val defaultRefreshPolicy: RefreshPolicy? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    val navigation: Navigation? = null,

    /**
     * Named overlay sections the client shows when a trigger condition arises. Keys are
     * developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed
     * sections (typically AtomicComposite). Client controls trigger timing and presentation
     * style; server controls display content.
     */
    val overlays: Map<String, Section>? = null,

    /**
     * URI the back button should navigate to.  Clients always show a back button; this field
     * tells them the target.  Omit for root screens (e.g. scoreboard).
     */
    @get:JsonProperty("parentUri")@field:JsonProperty("parentUri")
    val parentURI: String? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val schemaVersion: String,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val sections: List<Section>,

    val state: Map<String, Any?>? = null,

    /**
     * Legacy headline consumed at composition time to build the first AtomicComposite app-bar
     * section (see prependAppBarHeader). Not rendered from screen.title by clients. Omit on
     * bottom-nav tab destinations.
     */
    val title: String? = null,

    /**
     * Server-exposed A/B / experiment variants available for this screen. Clients read
     * `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
     * to the server on subsequent requests. Omit for screens without active experiments.
     */
    val variants: ExperimentVariants? = null
) {
    fun toJson() = mapper.writeValueAsString(this)

    companion object {
        fun fromJson(json: String) = mapper.readValue<SduiModels>(json)
    }
}

/**
 * Action dispatched when the month label is tapped. Conventionally a navigate action to the
 * full calendar screen. When absent, the month label is not tappable.
 *
 * Singular action executed after the renderer writes the tapped date into stateKey via
 * onStateChange. Conventionally a refresh action with paramBindings.
 *
 * Action dispatched after writing the selected date into stateKey. Conventionally a
 * navigate action back to the games screen.
 *
 * Action fired when the form is submitted
 *
 * Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
 *
 * Optional action to trigger on retry tap (typically a refresh action)
 */
data class Action (
    /**
     * For fireAndForget actions: where to send the beacon
     */
    val destinations: List<Destination>? = null,

    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     */
    val endpoint: String? = null,

    /**
     * For fireAndForget actions: event name
     */
    val event: String? = null,

    /**
     * Optional server-provided error message and presentation style. Client falls back to
     * generic localized string when absent
     */
    val failureFeedback: FailureFeedback? = null,

    /**
     * For fireAndForget actions with onVisible trigger: impression tracking policy
     */
    val impression: ImpressionPolicy? = null,

    /**
     * For toast actions: text message to display in the toast
     */
    val message: String? = null,

    /**
     * For navigate actions with modal presentation: sheet height
     */
    val modalHeight: ModalHeight? = null,

    /**
     * Sequence behavior when this action fails. Client applies per-type default when absent
     * (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
     */
    val onFailure: FailurePolicy? = null,

    /**
     * For mutate actions: operation to perform on the state key
     */
    val operation: MutateOperation? = null,

    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     */
    val paramBindings: Map<String, String>? = null,

    /**
     * For fireAndForget actions: event parameters
     */
    val params: Map<String, Any?>? = null,

    /**
     * For navigate actions: how the destination is presented
     */
    val presentation: NavigationPresentation? = null,

    /**
     * For mutate actions: state key to update. For dismiss actions: what to dismiss
     * (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
     */
    val target: String? = null,

    /**
     * For navigate actions: native deeplink URI
     */
    @get:JsonProperty("targetUri")@field:JsonProperty("targetUri")
    val targetURI: String? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val trigger: ActionTrigger,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val type: ActionType,

    /**
     * For mutate actions: the value to apply with the operation
     */
    val value: Any? = null,

    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     */
    @get:JsonProperty("webUrl")@field:JsonProperty("webUrl")
    val webURL: String? = null
)

enum class Destination(val value: String) {
    Adobe("adobe"),
    All("all"),
    Firebase("firebase"),
    Internal("internal");

    companion object {
        fun fromValue(value: String): Destination = when (value) {
            "adobe"    -> Adobe
            "all"      -> All
            "firebase" -> Firebase
            "internal" -> Internal
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * Optional server-provided error message and presentation style. Client falls back to
 * generic localized string when absent
 *
 * Optional server-provided error message and presentation style for action failures. Client
 * falls back to generic localized string when absent.
 */
data class FailureFeedback (
    /**
     * Localized error message to display on failure
     */
    val message: String? = null,

    /**
     * Presentation hint — clients map to closest platform-native mechanism
     */
    val style: FailureFeedbackStyle? = null
)

/**
 * Presentation hint — clients map to closest platform-native mechanism
 *
 * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
 */
enum class FailureFeedbackStyle(val value: String) {
    Inline("inline"),
    Snackbar("snackbar"),
    Toast("toast");

    companion object {
        fun fromValue(value: String): FailureFeedbackStyle = when (value) {
            "inline"   -> Inline
            "snackbar" -> Snackbar
            "toast"    -> Toast
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * For fireAndForget actions with onVisible trigger: impression tracking policy
 *
 * Impression tracking policy for analytics actions with onVisible trigger
 */
data class ImpressionPolicy (
    val dedup: ImpressionDedup? = null,

    /**
     * Reset interval for once-per-interval strategy (milliseconds)
     */
    @get:JsonProperty("intervalMs")@field:JsonProperty("intervalMs")
    val intervalMS: Long? = null,

    val threshold: ImpressionThreshold? = null
)

enum class ImpressionDedup(val value: String) {
    None("none"),
    OncePerInterval("once-per-interval"),
    OncePerScreen("once-per-screen"),
    OncePerSession("once-per-session");

    companion object {
        fun fromValue(value: String): ImpressionDedup = when (value) {
            "none"              -> None
            "once-per-interval" -> OncePerInterval
            "once-per-screen"   -> OncePerScreen
            "once-per-session"  -> OncePerSession
            else                -> throw IllegalArgumentException()
        }
    }
}

data class ImpressionThreshold (
    /**
     * Milliseconds section must remain visible before impression fires
     */
    @get:JsonProperty("dwellMs")@field:JsonProperty("dwellMs")
    val dwellMS: Long? = null,

    /**
     * Fraction of section area that must be visible (0.5 = 50%)
     */
    val visibility: Double? = null
)

/**
 * For navigate actions with modal presentation: sheet height
 */
enum class ModalHeight(val value: String) {
    Compact("compact"),
    Full("full"),
    Half("half");

    companion object {
        fun fromValue(value: String): ModalHeight = when (value) {
            "compact" -> Compact
            "full"    -> Full
            "half"    -> Half
            else      -> throw IllegalArgumentException()
        }
    }
}

/**
 * Sequence behavior when this action fails. Client applies per-type default when absent
 * (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
 *
 * Sequence behavior when an action fails. Clients apply per-type defaults when absent:
 * navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
 */
enum class FailurePolicy(val value: String) {
    Continue("continue"),
    Halt("halt"),
    Silent("silent");

    companion object {
        fun fromValue(value: String): FailurePolicy = when (value) {
            "continue" -> Continue
            "halt"     -> Halt
            "silent"   -> Silent
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * For mutate actions: operation to perform on the state key
 */
enum class MutateOperation(val value: String) {
    Append("append"),
    Increment("increment"),
    Set("set"),
    Toggle("toggle");

    companion object {
        fun fromValue(value: String): MutateOperation = when (value) {
            "append"    -> Append
            "increment" -> Increment
            "set"       -> Set
            "toggle"    -> Toggle
            else        -> throw IllegalArgumentException()
        }
    }
}

/**
 * For navigate actions: how the destination is presented
 */
enum class NavigationPresentation(val value: String) {
    External("external"),
    Fullscreen("fullscreen"),
    Modal("modal"),
    Push("push"),
    Replace("replace");

    companion object {
        fun fromValue(value: String): NavigationPresentation = when (value) {
            "external"   -> External
            "fullscreen" -> Fullscreen
            "modal"      -> Modal
            "push"       -> Push
            "replace"    -> Replace
            else         -> throw IllegalArgumentException()
        }
    }
}

/**
 * Event that fires the action. Prefer onActivate for primary activation (tap, keyboard
 * Enter/Space, accessibility activate). onTap is a deprecated alias for onActivate.
 */
enum class ActionTrigger(val value: String) {
    OnActivate("onActivate"),
    OnBlur("onBlur"),
    OnFocus("onFocus"),
    OnLongPress("onLongPress"),
    OnSubmit("onSubmit"),
    OnSwipe("onSwipe"),
    OnTap("onTap"),
    OnVisible("onVisible");

    companion object {
        fun fromValue(value: String): ActionTrigger = when (value) {
            "onActivate"  -> OnActivate
            "onBlur"      -> OnBlur
            "onFocus"     -> OnFocus
            "onLongPress" -> OnLongPress
            "onSubmit"    -> OnSubmit
            "onSwipe"     -> OnSwipe
            "onTap"       -> OnTap
            "onVisible"   -> OnVisible
            else          -> throw IllegalArgumentException()
        }
    }
}

enum class ActionType(val value: String) {
    Dismiss("dismiss"),
    FireAndForget("fireAndForget"),
    Mutate("mutate"),
    Navigate("navigate"),
    Refresh("refresh"),
    Toast("toast");

    companion object {
        fun fromValue(value: String): ActionType = when (value) {
            "dismiss"       -> Dismiss
            "fireAndForget" -> FireAndForget
            "mutate"        -> Mutate
            "navigate"      -> Navigate
            "refresh"       -> Refresh
            "toast"         -> Toast
            else            -> throw IllegalArgumentException()
        }
    }
}

/**
 * Outer padding around the scrollable section feed (start/end/top/bottom). Server emits
 * semantic layout tokens (e.g. token:nba.spacing.md); clients resolve via
 * LayoutTokenResolver. Omit only when the screen is intentionally edge-to-edge.
 *
 * Outer space between the element and its siblings or parent edges. Applied outside the
 * element's background, border, corner radius, and shadow — use this for sibling-to-sibling
 * spacing instead of Spacer siblings when inhomogeneous gaps are needed.
 *
 * Optional edge offsets from the aligned base bounds.
 *
 * Inner space between the element's own background/border and its content.
 *
 * Outer margin (space between the surface and its siblings / screen edge).
 *
 * Inner padding (space between the surface edge and the content it wraps).
 */
data class Spacing (
    val bottom: LayoutScalar? = null,
    val end: LayoutScalar? = null,
    val start: LayoutScalar? = null,
    val top: LayoutScalar? = null
)

/**
 * Absolute layout value: raw dp/px integer, or a semantic layout token reference
 * token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per
 * platform.formFactor against bundled spacing/corner/size/typography/shadow registries.
 * Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
 *
 * Bottom-trailing corner.
 *
 * Bottom-leading corner.
 *
 * Top-trailing corner (top-right in LTR, top-left in RTL).
 *
 * Top-leading corner (top-left in LTR, top-right in RTL).
 *
 * Corner radius: dp/px or layout token. Applied to Container (with overflow clip) and Image
 * elements.
 *
 * Gap between wrapped lines when layoutWrap is true. Falls back to gap when absent. Ignored
 * when layoutWrap is false.
 *
 * Gap between flex children (row/column), or grid gap where applicable.
 *
 * Fixed height in dp/px or layout token.
 *
 * Maximum height constraint in dp/px or layout token.
 *
 * Maximum width constraint in dp/px or layout token.
 *
 * Minimum height constraint in dp/px or layout token.
 *
 * Minimum width constraint in dp/px or layout token.
 *
 * Fixed width in dp/px or layout token.
 *
 * Corner radius: dp/px or layout token, applied to the surface (with overflow clip).
 */
sealed class LayoutScalar {
    class IntegerValue(val value: Long)  : LayoutScalar()
    class StringValue(val value: String) : LayoutScalar()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is IntegerValue -> this.value
        is StringValue  -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): LayoutScalar = when (jn) {
            is IntNode, is LongNode -> IntegerValue(mapper.treeToValue(jn))
            is TextNode             -> StringValue(mapper.treeToValue(jn))
            else                    -> throw IllegalArgumentException()
        }
    }
}

data class RefreshPolicy (
    /**
     * For sse type: Ably channel name pattern (e.g., '{gameId}:linescore')
     */
    val channel: String? = null,

    /**
     * JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
     */
    val dataPath: String? = null,

    /**
     * For poll type: interval in milliseconds
     */
    @get:JsonProperty("intervalMs")@field:JsonProperty("intervalMs")
    val intervalMS: Long? = null,

    /**
     * Whether the client should pause this section's refresh when it scrolls out of the
     * viewport. Default true. Set false for critical live sections (e.g., live-score panels)
     * that should refresh continuously.
     */
    val pauseWhenOffScreen: Boolean? = null,

    /**
     * For poll type: server-relative SDUI path to re-fetch this section (e.g.
     * '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'). The response is a
     * single Section object that replaces this section in place; the client then re-evaluates
     * the new section's refreshPolicy (enabling poll→SSE transition). Mutually exclusive with
     * url; sectionEndpoint takes precedence when both are present.
     */
    val sectionEndpoint: String? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val type: RefreshType,

    /**
     * For poll type: external URL to poll (e.g. CDN stats endpoint). Data is applied via
     * dataBinding. Mutually exclusive with sectionEndpoint; if both are present,
     * sectionEndpoint takes precedence.
     */
    val url: String? = null
)

enum class RefreshType(val value: String) {
    Poll("poll"),
    SSE("sse"),
    Static("static");

    companion object {
        fun fromValue(value: String): RefreshType = when (value) {
            "poll"   -> Poll
            "sse"    -> SSE
            "static" -> Static
            else     -> throw IllegalArgumentException()
        }
    }
}

data class Navigation (
    val items: List<NavigationItem>? = null
)

data class NavigationItem (
    val children: List<NavigationItem>? = null,
    val icon: String? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    val selected: Boolean? = null,

    @get:JsonProperty("targetUri")@field:JsonProperty("targetUri")
    val targetURI: String? = null
)

/**
 * One server-composed overlay layer positioned over an OverlayContainer base element.
 */
data class AtomicOverlay (
    /**
     * Position of the overlay within the base element bounds.
     */
    val alignment: BadgeAlignment? = null,

    /**
     * Atomic element to render in this overlay layer.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val element: AtomicElement,

    /**
     * Optional edge offsets from the aligned base bounds.
     */
    val inset: Spacing? = null
)

/**
 * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
 *
 * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a
 * thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the
 * screen-level overlays map.
 */
data class Badge (
    /**
     * Position of the badge within the parent bounds
     */
    val alignment: BadgeAlignment? = null,

    /**
     * The element to render as a badge
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val element: AtomicElement
)

/**
 * Optional atomic tree for the tab header row. When present, renderers walk it via
 * AtomicRouter; when absent, a minimal platform-native tab row is used. Selected-tab
 * styling in atomics requires state-bound conditionals (ADR-014).
 *
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 *
 * The element to render as a badge
 *
 * OverlayContainer base element. Rendered first and sized by its own atomic box model.
 *
 * Atomic element to render in this overlay layer.
 *
 * Atomic tree describing the banner's full visible surface. Renderer walks this tree
 * exactly as an AtomicComposite would; no client-side chrome defaults are permitted. See
 * AGENTS.md §15.1.
 *
 * Atomic tree describing the hero's full visible surface — logo, title, subtitle, feature
 * list, tier cards, CTAs. Renderer walks this tree exactly as an AtomicComposite would.
 *
 * Root node of the atomic element tree — the rendering instructions
 *
 * Atomic tree describing the pre-SDK placeholder surface (e.g. play-glyph column framed at
 * the player's aspectRatio). Renderer walks this tree exactly as an AtomicComposite would;
 * no client-side chrome defaults are permitted. Once the video SDK lands this tree becomes
 * the loading/error placeholder the SDK overlays.
 */
data class AtomicElement (
    /**
     * Server-provided accessibility metadata for this atomic element
     */
    val accessibility: AccessibilityProperties? = null,

    val actions: List<Action>? = null,
    val alignment: Alignment? = null,

    /**
     * Per-child cross-axis alignment override. When set, wins over parent crossAlignment for
     * this child (matches Figma and CSS align-self semantics).
     */
    val alignSelf: CrossAlignment? = null,

    /**
     * Deprecated: use accessibility.label instead. Retained for backward compatibility; clients
     * prefer accessibility.label when present.
     */
    val alt: String? = null,

    /**
     * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
     */
    val aspectRatio: AspectRatioUnion? = null,

    /**
     * DEPRECATED — use backgrounds (array) for new payloads. Single background. If both
     * background and backgrounds are present, backgrounds wins.
     */
    val background: BackgroundUnion? = null,

    /**
     * Ordered array of background layers. Index 0 is the bottommost layer (Figma convention);
     * higher indices paint on top. Web renderers must reverse the array when mapping to CSS
     * background shorthand (CSS is top-to-bottom). When absent, falls back to singular
     * background field.
     */
    val backgrounds: List<BackgroundUnion>? = null,

    /**
     * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
     */
    val badge: Badge? = null,

    /**
     * OverlayContainer base element. Rendered first and sized by its own atomic box model.
     */
    val base: AtomicElement? = null,

    /**
     * Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers
     * resolve the leaf's canonical live field from `data.content[bindRef]` at render time and
     * fall back to the inline value when the path is absent. Canonical field per type: Text →
     * content, Button → label, Image → src, LiveClock → an object with {snapshotSeconds,
     * snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than
     * in a centrally-declared path-into-tree) lets composers reshape the ui tree without
     * breaking real-time updates; data-bindings on the section envelope continue to write into
     * `content.*`.
     */
    val bindRef: String? = null,

    /**
     * Responsive breakpoint in dp/px. For Container: below this screen width, direction flips
     * from row to column. Enables responsive layouts without client logic.
     */
    val breakpoint: Long? = null,

    val children: List<AtomicElement>? = null,
    val color: String? = null,

    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     */
    val columns: List<Column>? = null,

    val condition: String? = null,
    val content: String? = null,

    /**
     * Per-corner cornerRadius override. When present, takes precedence over the single-value
     * cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also
     * absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and
     * square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to
     * UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner
     * constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius /
     * borderBottomRightRadius.
     */
    val cornerRadii: CornerRadii? = null,

    /**
     * Corner radius: dp/px or layout token. Applied to Container (with overflow clip) and Image
     * elements.
     */
    val cornerRadius: LayoutScalar? = null,

    val crossAlignment: CrossAlignment? = null,

    /**
     * Gap between wrapped lines when layoutWrap is true. Falls back to gap when absent. Ignored
     * when layoutWrap is false.
     */
    val crossAxisGap: LayoutScalar? = null,

    val direction: UIDirection? = null,
    val disabled: Boolean? = null,
    val falseChild: AtomicElement? = null,
    val fit: ImageFit? = null,

    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space
     * along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     */
    val flex: Double? = null,

    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals
     * typography (equivalent to TextVariant.score).
     */
    val format: Format? = null,

    /**
     * Gap between flex children (row/column), or grid gap where applicable.
     */
    val gap: LayoutScalar? = null,

    /**
     * Fixed height in dp/px or layout token.
     */
    val height: LayoutScalar? = null,

    /**
     * Sizing behavior along the height axis. 'hug' = intrinsic, 'fill' = stretch to parent,
     * 'fixed' = use explicit height value.
     */
    val heightMode: SizingMode? = null,

    val icon: String? = null,
    val id: String? = null,

    /**
     * LiveClock: whether the clock is actively ticking. When true, clients run a local tick
     * loop at their platform-native refresh cadence (~10Hz) and update the displayed value.
     * When false, clients render snapshotSeconds verbatim.
     */
    @get:JsonProperty("isRunning")@field:JsonProperty("isRunning")
    val isRunning: Boolean? = null,

    val label: String? = null,

    /**
     * When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to
     * the next line. Only meaningful on Container elements.
     */
    val layoutWrap: Boolean? = null,

    /**
     * Outer space between the element and its siblings or parent edges. Applied outside the
     * element's background, border, corner radius, and shadow — use this for sibling-to-sibling
     * spacing instead of Spacer siblings when inhomogeneous gaps are needed.
     */
    val margin: Spacing? = null,

    /**
     * Maximum height constraint in dp/px or layout token.
     */
    val maxHeight: LayoutScalar? = null,

    val maxLines: Long? = null,

    /**
     * Maximum width constraint in dp/px or layout token.
     */
    val maxWidth: LayoutScalar? = null,

    /**
     * Minimum height constraint in dp/px or layout token.
     */
    val minHeight: LayoutScalar? = null,

    /**
     * Minimum width constraint in dp/px or layout token.
     */
    val minWidth: LayoutScalar? = null,

    /**
     * Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes
     * (scores, clocks).
     */
    val monospacedDigits: Boolean? = null,

    /**
     * Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded
     * states.
     */
    val opacity: Double? = null,

    val orientation: Orientation? = null,

    /**
     * OverlayContainer layers rendered over the base element in server order.
     */
    val overlays: List<AtomicOverlay>? = null,

    /**
     * Inner space between the element's own background/border and its content.
     */
    val padding: Spacing? = null,

    /**
     * Optional ScrollContainer page indicator. Clients render it only when declared.
     */
    val pageIndicator: PageIndicator? = null,

    val paging: Boolean? = null,
    val placeholder: String? = null,

    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     */
    val rows: List<Map<String, String>>? = null,

    /**
     * Full section object to render via SectionRouter. Only used when type is SectionSlot.
     */
    val section: Section? = null,

    /**
     * DEPRECATED — use shadows (array) for new payloads. Single shadow. If both shadow and
     * shadows are present, shadows wins.
     */
    val shadow: ShadowOrToken? = null,

    /**
     * Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention);
     * higher indices are closer to the element. Maps directly to CSS box-shadow list order.
     * When absent, falls back to singular shadow field.
     */
    val shadows: List<ShadowOrToken>? = null,

    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel
     * presentation.
     */
    val showIndicators: Boolean? = null,

    val size: Long? = null,
    val snapAlignment: Align? = null,

    /**
     * LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients
     * compute elapsed = now - snapshotAt and derive the displayed value. Required when type ==
     * 'LiveClock'.
     */
    val snapshotAt: String? = null,

    /**
     * LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients
     * interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
     */
    val snapshotSeconds: Long? = null,

    val src: String? = null,

    /**
     * LiveClock: optional clamp. For direction 'down', clock holds at this value once reached.
     * For direction 'up', clock holds once reached. Omit to disable the clamp.
     */
    val stopAtSeconds: Long? = null,

    /**
     * Alternate row background for readability
     */
    val striped: Boolean? = null,

    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric
     * values.
     */
    val textAlign: Align? = null,

    val thickness: Long? = null,

    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
     * (default 0); 'up' increments from snapshotSeconds with no upper bound unless
     * stopAtSeconds is set.
     */
    val tickDirection: TickDirection? = null,

    val trueChild: AtomicElement? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val type: String,

    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text,
     * ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image.
     * Renderers parse this string against the primitive's enum and log a diagnostic on
     * unrecognized values.
     */
    val variant: String? = null,

    val weight: TextWeight? = null,

    /**
     * Fixed width in dp/px or layout token.
     */
    val width: LayoutScalar? = null,

    /**
     * Sizing behavior along the width axis. 'hug' = intrinsic, 'fill' = stretch to parent,
     * 'fixed' = use explicit width value.
     */
    val widthMode: SizingMode? = null
)

/**
 * Section-specific data payload
 *
 * Tabbed navigation with dynamic content sections per tab. Optional ui is the tab
 * header/control row only; tabContents hosts nested sections. Tab selection uses
 * section.subsections mutate actions.
 *
 * Typed tabular data for an NBA-style boxscore (one per team)
 *
 * Platform-native horizontal date picker. All ISO date fields are ET-anchored
 * (America/New_York) calendar days (YYYY-MM-DD). defaultDate is the league's current
 * anchor/focus date — typically today in ET during the regular season, but may be a future
 * date during offseason or breaks (e.g. the regular-season opener). Clients display
 * defaultDate as-is and never compare it to device time.
 *
 * Vertically-scrollable month-grid calendar with per-date game metadata. All date fields
 * are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current
 * anchor date; drives the 'today' visual highlight and the initial scroll position.
 *
 * Server-driven form section with typed fields bound to screen state
 *
 * Ad placement primitive — carries placement semantics while delegating auction/targeting
 * to ad-platform SDKs (see ADR-007)
 *
 * Sortable, paginated table of season statistical leaders (league-wide)
 *
 * Inline subscription upsell banner. Reserved SDK integration point: the banner's visible
 * chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play
 * Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP
 * product identifiers the SDK will later bind to; `ctaAction` is the (pre-SDK) fallback
 * action.
 *
 * Full-screen subscription upsell hero. Reserved SDK integration point — same contract as
 * SubscribeBannerData: `ui` carries the full visible composition; `tiers` carries IAP
 * product identifiers the SDK will bind to post-landing.
 *
 * Data payload for AtomicComposite sections — ui contains rendering instructions, content
 * carries domain data
 *
 * Video player section — reserved SDK integration point for DRM / HLS / ad insertion.
 * `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK
 * reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders
 * before the SDK is integrated and will serve as the loading/error placeholder afterwards.
 */
data class Data (
    val defaultTab: String? = null,

    /**
     * Screen-state key that holds the selected ISO date
     *
     * Screen-state key for the selected ISO date.
     */
    val stateKey: String? = null,

    val tabContents: Map<String, List<Section>>? = null,
    val tabs: List<TabData>? = null,

    /**
     * Optional atomic tree for the tab header row. When present, renderers walk it via
     * AtomicRouter; when absent, a minimal platform-native tab row is used. Selected-tab
     * styling in atomics requires state-bound conditionals (ADR-014).
     *
     * Atomic tree describing the banner's full visible surface. Renderer walks this tree
     * exactly as an AtomicComposite would; no client-side chrome defaults are permitted. See
     * AGENTS.md §15.1.
     *
     * Atomic tree describing the hero's full visible surface — logo, title, subtitle, feature
     * list, tier cards, CTAs. Renderer walks this tree exactly as an AtomicComposite would.
     *
     * Root node of the atomic element tree — the rendering instructions
     *
     * Atomic tree describing the pre-SDK placeholder surface (e.g. play-glyph column framed at
     * the player's aspectRatio). Renderer walks this tree exactly as an AtomicComposite would;
     * no client-side chrome defaults are permitted. Once the video SDK lands this tree becomes
     * the loading/error placeholder the SDK overlays.
     */
    val ui: AtomicElement? = null,

    /**
     * Ordered list of column definitions; clients render left-to-right
     *
     * Ordered column definitions; clients render left-to-right
     */
    val columns: List<BoxscoreColumnDefinition>? = null,

    /**
     * Text shown when no player rows are available
     */
    val emptyMessage: String? = null,

    /**
     * Player rows ordered by server (starters first, then bench)
     *
     * Player rows, pre-sorted by the server
     */
    val players: List<PlayerRow>? = null,

    /**
     * Screen-state key holding the current sort direction (asc/desc)
     */
    val sortDirectionStateKey: String? = null,

    /**
     * Screen-state key holding the current sort column key
     */
    val sortStateKey: String? = null,

    /**
     * Hex colour for team accent
     */
    val teamColor: String? = null,

    @get:JsonProperty("teamLogoUrl")@field:JsonProperty("teamLogoUrl")
    val teamLogoURL: String? = null,

    val teamName: String? = null,

    /**
     * Aggregate row shown at the bottom of the table
     */
    val teamTotals: Map<String, Any?>? = null,

    /**
     * Three-letter team code, e.g. 'BOS'
     */
    val teamTricode: String? = null,

    /**
     * ISO YYYY-MM-DD (ET) for the league's current anchor date. Drives the default-cell visual
     * highlight. Server-authoritative — not always today; may be a future date during offseason
     * or breaks.
     *
     * ISO YYYY-MM-DD (ET) for the league's current anchor date.
     */
    val defaultDate: String? = null,

    /**
     * Action dispatched when the month label is tapped. Conventionally a navigate action to the
     * full calendar screen. When absent, the month label is not tappable.
     */
    val expandedAction: Action? = null,

    /**
     * ISO YYYY-MM-DD (ET) for the latest selectable date (e.g. season/finals end). Absent means
     * unbounded.
     *
     * ISO YYYY-MM-DD (ET) for the latest selectable date.
     */
    val maxDate: String? = null,

    /**
     * ISO YYYY-MM-DD (ET) for the earliest selectable date (e.g. season start). Absent means
     * unbounded; clients pick a sensible default window.
     *
     * ISO YYYY-MM-DD (ET) for the earliest selectable date.
     */
    val minDate: String? = null,

    /**
     * Singular action executed after the renderer writes the tapped date into stateKey via
     * onStateChange. Conventionally a refresh action with paramBindings.
     *
     * Action dispatched after writing the selected date into stateKey. Conventionally a
     * navigate action back to the games screen.
     */
    val onDateSelected: Action? = null,

    /**
     * ISO YYYY-MM-DD (ET) for initial selection. Falls back here when screenState[stateKey] is
     * absent; otherwise the state value wins.
     *
     * ISO YYYY-MM-DD (ET) for initial selection.
     */
    val selectedDate: String? = null,

    /**
     * Map of ISO date string to metadata for that date. Only dates with games are present;
     * absent dates have zero games.
     */
    val dateMetadata: Map<String, DateMetadatum>? = null,

    val fields: List<FormField>? = null,

    /**
     * Layout hint for field arrangement
     */
    val layout: Layout? = null,

    /**
     * Action fired when the form is submitted
     */
    val submitAction: Action? = null,

    val submitLabel: String? = null,

    /**
     * Ad unit path used by the ad SDK
     */
    val adUnitPath: String? = null,

    /**
     * Whether to collapse the slot when no fill is returned
     */
    val collapseOnEmpty: Boolean? = null,

    /**
     * Disclosure label displayed above/below the ad
     */
    val label: String? = null,

    /**
     * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
     * when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
     * width/height. Required so the stub renderer has no client-side chrome defaults.
     */
    val placeholder: Placeholder? = null,

    /**
     * Ad network identifier, e.g. 'gam', 'amazon'
     */
    val provider: String? = null,

    /**
     * Optional auto-refresh interval in seconds
     */
    @get:JsonProperty("refreshIntervalSec")@field:JsonProperty("refreshIntervalSec")
    val refreshIntervalSEC: Long? = null,

    /**
     * Accepted creative sizes as [width, height] pairs
     */
    val sizes: List<List<Long>>? = null,

    /**
     * Key-value targeting hints passed to ad SDK
     */
    val targeting: Map<String, String>? = null,

    /**
     * Current page (1-based)
     */
    val page: Long? = null,

    /**
     * Number of rows per page
     */
    val pageSize: Long? = null,

    /**
     * Key of the column the table is currently sorted by
     */
    val sortColumn: String? = null,

    val sortDirection: SortDirection? = null,

    /**
     * Secondary text, e.g. '2025-26 Regular Season – Per Game'
     */
    val subtitle: String? = null,

    /**
     * Table heading, e.g. 'Season Leaders'
     */
    val title: String? = null,

    /**
     * Total number of rows available server-side (for pagination display)
     */
    val totalRows: Long? = null,

    /**
     * Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
     */
    val ctaAction: Action? = null,

    /**
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
     * not used by the renderer, which reads the visible price copy out of `ui`.
     *
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
     * not used by the renderer.
     */
    val tiers: List<SubscriptionTier>? = null,

    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
     * data-binding support.
     */
    val content: Map<String, Any?>? = null,

    val autoplay: Boolean? = null,

    /**
     * Platform capabilities the player should enable. Server includes only capabilities
     * relevant to the requesting platform (via X-Analytics-Platform header).
     */
    val capabilities: List<Capability>? = null,

    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId
     * for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     */
    @get:JsonProperty("contentId")@field:JsonProperty("contentId")
    val contentID: String? = null,

    val displayConfig: DisplayConfig? = null,

    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     */
    val playerType: PlayerType? = null
)

/**
 * Full section object to render via SectionRouter. Only used when type is SectionSlot.
 */
data class Section (
    /**
     * Section-level accessibility metadata (landmark role, live region, heading)
     */
    val accessibility: AccessibilityProperties? = null,

    /**
     * Section-level interaction actions
     */
    val actions: List<Action>? = null,

    @get:JsonProperty("analyticsId")@field:JsonProperty("analyticsId")
    val analyticsID: String? = null,

    /**
     * Origin identifier for the content backing this section (e.g. 'cms:article-42',
     * 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.
     */
    @get:JsonProperty("contentSourceId")@field:JsonProperty("contentSourceId")
    val contentSourceID: String? = null,

    /**
     * Section-specific data payload
     */
    val data: Data? = null,

    val dataBinding: DataBinding? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    val refreshPolicy: RefreshPolicy? = null,
    val sectionStates: SectionStates? = null,

    /**
     * Section-level map of translation key to localized string. Used by DataBindingResolver to
     * resolve stringKeys on real-time updates.
     */
    val stringTable: Map<String, String>? = null,

    /**
     * Nested interaction targets within the section
     */
    val subsections: List<Subsection>? = null,

    val surface: SectionSurface? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val type: String
)

/**
 * Position of the badge within the parent bounds
 *
 * Position of the overlay within the base element bounds.
 *
 * Position of the indicator within the ScrollContainer bounds.
 */
enum class BadgeAlignment(val value: String) {
    BottomCenter("bottomCenter"),
    BottomEnd("bottomEnd"),
    BottomStart("bottomStart"),
    Center("center"),
    CenterEnd("centerEnd"),
    CenterStart("centerStart"),
    TopCenter("topCenter"),
    TopEnd("topEnd"),
    TopStart("topStart");

    companion object {
        fun fromValue(value: String): BadgeAlignment = when (value) {
            "bottomCenter" -> BottomCenter
            "bottomEnd"    -> BottomEnd
            "bottomStart"  -> BottomStart
            "center"       -> Center
            "centerEnd"    -> CenterEnd
            "centerStart"  -> CenterStart
            "topCenter"    -> TopCenter
            "topEnd"       -> TopEnd
            "topStart"     -> TopStart
            else           -> throw IllegalArgumentException()
        }
    }
}

/**
 * Section-level accessibility metadata (landmark role, live region, heading)
 *
 * Server-provided accessibility metadata applied natively per platform
 *
 * Server-provided accessibility metadata for this atomic element
 *
 * Subsection-level accessibility metadata
 */
data class AccessibilityProperties (
    /**
     * Heading level (1-6) for role=heading elements. Maps to aria-level (Web),
     * accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
     */
    val headingLevel: Long? = null,

    /**
     * When true, element and its descendants are hidden from the accessibility tree (decorative
     * content).
     */
    val hidden: Boolean? = null,

    /**
     * Additional context announced after the label. Maps to accessibilityHint (iOS),
     * contentDescription suffix (Android), aria-describedby text (Web).
     */
    val hint: String? = null,

    /**
     * Human-readable label announced by screen readers. Omit for elements whose text content is
     * self-describing.
     */
    val label: String? = null,

    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
     * aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     */
    val liveRegion: LiveRegion? = null,

    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     */
    val role: Role? = null,

    /**
     * Override default accessibility traversal order. Lower values are visited first. Omit to
     * use natural DOM/view order.
     */
    val sortOrder: Long? = null
)

/**
 * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
 * aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
 */
enum class LiveRegion(val value: String) {
    Assertive("assertive"),
    Off("off"),
    Polite("polite");

    companion object {
        fun fromValue(value: String): LiveRegion = when (value) {
            "assertive" -> Assertive
            "off"       -> Off
            "polite"    -> Polite
            else        -> throw IllegalArgumentException()
        }
    }
}

/**
 * Semantic role override. 'none' suppresses the element's intrinsic role.
 */
enum class Role(val value: String) {
    Button("button"),
    Cell("cell"),
    Heading("heading"),
    Image("image"),
    Link("link"),
    Listitem("listitem"),
    None("none"),
    RoleList("list"),
    Row("row"),
    Tab("tab"),
    Table("table"),
    Tabpanel("tabpanel");

    companion object {
        fun fromValue(value: String): Role = when (value) {
            "button"   -> Button
            "cell"     -> Cell
            "heading"  -> Heading
            "image"    -> Image
            "link"     -> Link
            "listitem" -> Listitem
            "none"     -> None
            "list"     -> RoleList
            "row"      -> Row
            "tab"      -> Tab
            "table"    -> Table
            "tabpanel" -> Tabpanel
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * Per-child cross-axis alignment override. When set, wins over parent crossAlignment for
 * this child (matches Figma and CSS align-self semantics).
 */
enum class CrossAlignment(val value: String) {
    Center("center"),
    End("end"),
    Start("start"),
    Stretch("stretch");

    companion object {
        fun fromValue(value: String): CrossAlignment = when (value) {
            "center"  -> Center
            "end"     -> End
            "start"   -> Start
            "stretch" -> Stretch
            else      -> throw IllegalArgumentException()
        }
    }
}

enum class Alignment(val value: String) {
    Center("center"),
    End("end"),
    SpaceAround("spaceAround"),
    SpaceBetween("spaceBetween"),
    SpaceEvenly("spaceEvenly"),
    Start("start");

    companion object {
        fun fromValue(value: String): Alignment = when (value) {
            "center"       -> Center
            "end"          -> End
            "spaceAround"  -> SpaceAround
            "spaceBetween" -> SpaceBetween
            "spaceEvenly"  -> SpaceEvenly
            "start"        -> Start
            else           -> throw IllegalArgumentException()
        }
    }
}

/**
 * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
 */
sealed class AspectRatioUnion {
    class DoubleValue(val value: Double)        : AspectRatioUnion()
    class EnumValue(val value: AspectRatioEnum) : AspectRatioUnion()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is DoubleValue -> this.value
        is EnumValue   -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): AspectRatioUnion = when (jn) {
            is DoubleNode -> DoubleValue(mapper.treeToValue(jn))
            is TextNode   -> EnumValue(mapper.treeToValue(jn))
            else          -> throw IllegalArgumentException()
        }
    }
}

enum class AspectRatioEnum(val value: String) {
    The11("1:1"),
    The169("16:9"),
    The219("21:9"),
    The32("3:2"),
    The43("4:3");

    companion object {
        fun fromValue(value: String): AspectRatioEnum = when (value) {
            "1:1"  -> The11
            "16:9" -> The169
            "21:9" -> The219
            "3:2"  -> The32
            "4:3"  -> The43
            else   -> throw IllegalArgumentException()
        }
    }
}

/**
 * DEPRECATED — use backgrounds (array) for new payloads. Single background. If both
 * background and backgrounds are present, backgrounds wins.
 *
 * Shared background type — solid color, gradient, or image with overlay
 *
 * Surface background (solid, gradient, or image).
 */
sealed class BackgroundUnion {
    class BackgroundValue(val value: Background) : BackgroundUnion()
    class StringValue(val value: String)         : BackgroundUnion()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is BackgroundValue -> this.value
        is StringValue     -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): BackgroundUnion = when (jn) {
            is ObjectNode -> BackgroundValue(mapper.treeToValue(jn))
            is TextNode   -> StringValue(mapper.treeToValue(jn))
            else          -> throw IllegalArgumentException()
        }
    }
}

/**
 * Gradient background with ordered color stops
 *
 * Image background with optional scale and overlay
 */
data class Background (
    /**
     * Ordered list of color stops (hex or semantic token)
     */
    val colors: List<String>? = null,

    val direction: Direction? = null,

    /**
     * URL of the background image
     */
    @get:JsonProperty("imageUrl")@field:JsonProperty("imageUrl")
    val imageURL: String? = null,

    /**
     * Optional overlay applied on top of the image
     */
    val overlay: Overlay? = null,

    val scaleType: ScaleType? = null
)

enum class Direction(val value: String) {
    Diagonal("diagonal"),
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object {
        fun fromValue(value: String): Direction = when (value) {
            "diagonal"   -> Diagonal
            "horizontal" -> Horizontal
            "vertical"   -> Vertical
            else         -> throw IllegalArgumentException()
        }
    }
}

/**
 * Optional overlay applied on top of the image
 */
sealed class Overlay {
    class BackgroundGradientValue(val value: BackgroundGradient) : Overlay()
    class StringValue(val value: String)                         : Overlay()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is BackgroundGradientValue -> this.value
        is StringValue             -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): Overlay = when (jn) {
            is ObjectNode -> BackgroundGradientValue(mapper.treeToValue(jn))
            is TextNode   -> StringValue(mapper.treeToValue(jn))
            else          -> throw IllegalArgumentException()
        }
    }
}

/**
 * Gradient background with ordered color stops
 */
data class BackgroundGradient (
    /**
     * Ordered list of color stops (hex or semantic token)
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val colors: List<String>,

    val direction: Direction? = null
)

enum class ScaleType(val value: String) {
    Contain("contain"),
    Cover("cover"),
    Fill("fill");

    companion object {
        fun fromValue(value: String): ScaleType = when (value) {
            "contain" -> Contain
            "cover"   -> Cover
            "fill"    -> Fill
            else      -> throw IllegalArgumentException()
        }
    }
}

data class Column (
    val align: Align? = null,

    /**
     * Row data key
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val key: String,

    /**
     * Header label
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    /**
     * Fixed width (integer) or 'flex'
     */
    val width: WidthUnion? = null
)

/**
 * Text alignment within the element. Used for centered headings, right-aligned numeric
 * values.
 */
enum class Align(val value: String) {
    Center("center"),
    End("end"),
    Start("start");

    companion object {
        fun fromValue(value: String): Align = when (value) {
            "center" -> Center
            "end"    -> End
            "start"  -> Start
            else     -> throw IllegalArgumentException()
        }
    }
}

/**
 * Fixed width (integer) or 'flex'
 */
sealed class WidthUnion {
    class EnumValue(val value: WidthEnum) : WidthUnion()
    class IntegerValue(val value: Long)   : WidthUnion()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is EnumValue    -> this.value
        is IntegerValue -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): WidthUnion = when (jn) {
            is TextNode             -> EnumValue(mapper.treeToValue(jn))
            is IntNode, is LongNode -> IntegerValue(mapper.treeToValue(jn))
            else                    -> throw IllegalArgumentException()
        }
    }
}

enum class WidthEnum(val value: String) {
    Flex("flex");

    companion object {
        fun fromValue(value: String): WidthEnum = when (value) {
            "flex" -> Flex
            else   -> throw IllegalArgumentException()
        }
    }
}

/**
 * Per-corner cornerRadius override. When present, takes precedence over the single-value
 * cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also
 * absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and
 * square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to
 * UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner
 * constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius /
 * borderBottomRightRadius.
 */
data class CornerRadii (
    /**
     * Bottom-trailing corner.
     */
    val bottomEnd: LayoutScalar? = null,

    /**
     * Bottom-leading corner.
     */
    val bottomStart: LayoutScalar? = null,

    /**
     * Top-trailing corner (top-right in LTR, top-left in RTL).
     */
    val topEnd: LayoutScalar? = null,

    /**
     * Top-leading corner (top-left in LTR, top-right in RTL).
     */
    val topStart: LayoutScalar? = null
)

enum class UIDirection(val value: String) {
    Column("column"),
    Row("row");

    companion object {
        fun fromValue(value: String): UIDirection = when (value) {
            "column" -> Column
            "row"    -> Row
            else     -> throw IllegalArgumentException()
        }
    }
}

enum class ImageFit(val value: String) {
    Contain("contain"),
    Cover("cover"),
    Fill("fill"),
    None("none");

    companion object {
        fun fromValue(value: String): ImageFit = when (value) {
            "contain" -> Contain
            "cover"   -> Cover
            "fill"    -> Fill
            "none"    -> None
            else      -> throw IllegalArgumentException()
        }
    }
}

/**
 * LiveClock display format. Clients realize using their platform's tabular-numerals
 * typography (equivalent to TextVariant.score).
 */
enum class Format(val value: String) {
    HMmSs("h:mm:ss"),
    MSs("m:ss"),
    MmSs("mm:ss");

    companion object {
        fun fromValue(value: String): Format = when (value) {
            "h:mm:ss" -> HMmSs
            "m:ss"    -> MSs
            "mm:ss"   -> MmSs
            else      -> throw IllegalArgumentException()
        }
    }
}

/**
 * Sizing behavior along the height axis. 'hug' = intrinsic, 'fill' = stretch to parent,
 * 'fixed' = use explicit height value.
 *
 * Sizing behavior along one axis. 'hug' sizes to content (default). 'fill' stretches to
 * parent available space. 'fixed' uses the explicit width/height value.
 *
 * Sizing behavior along the width axis. 'hug' = intrinsic, 'fill' = stretch to parent,
 * 'fixed' = use explicit width value.
 */
enum class SizingMode(val value: String) {
    Fill("fill"),
    Fixed("fixed"),
    Hug("hug");

    companion object {
        fun fromValue(value: String): SizingMode = when (value) {
            "fill"  -> Fill
            "fixed" -> Fixed
            "hug"   -> Hug
            else    -> throw IllegalArgumentException()
        }
    }
}

enum class Orientation(val value: String) {
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object {
        fun fromValue(value: String): Orientation = when (value) {
            "horizontal" -> Horizontal
            "vertical"   -> Vertical
            else         -> throw IllegalArgumentException()
        }
    }
}

/**
 * Optional ScrollContainer page indicator. Clients render it only when declared.
 *
 * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local
 * scroll state only to realize the declared affordance.
 */
data class PageIndicator (
    /**
     * Active indicator color.
     */
    val activeColor: String? = null,

    /**
     * Position of the indicator within the ScrollContainer bounds.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val alignment: BadgeAlignment,

    /**
     * Inactive indicator color.
     */
    val color: String? = null,

    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
     * bar segments.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val style: Style
)

/**
 * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
 * bar segments.
 */
enum class Style(val value: String) {
    Dashes("dashes"),
    Dots("dots");

    companion object {
        fun fromValue(value: String): Style = when (value) {
            "dashes" -> Dashes
            "dots"   -> Dots
            else     -> throw IllegalArgumentException()
        }
    }
}

/**
 * DEPRECATED — use shadows (array) for new payloads. Single shadow. If both shadow and
 * shadows are present, shadows wins.
 *
 * Either a full Shadow struct or a shorthand token. Clients expand shorthand tokens to the
 * full Shadow struct at resolve time.
 *
 * Drop shadow applied to the surface.
 */
sealed class ShadowOrToken {
    class ShadowValue(val value: Shadow) : ShadowOrToken()
    class StringValue(val value: String) : ShadowOrToken()

    fun toJson(): String = mapper.writeValueAsString(when (this) {
        is ShadowValue -> this.value
        is StringValue -> this.value
    })

    companion object {
        fun fromJson(jn: JsonNode): ShadowOrToken = when (jn) {
            is ObjectNode -> ShadowValue(mapper.treeToValue(jn))
            is TextNode   -> StringValue(mapper.treeToValue(jn))
            else          -> throw IllegalArgumentException()
        }
    }
}

/**
 * Shadow effect with CSS/SwiftUI semantics (radius + offset). Compose approximates via
 * elevation. Use 'type' to distinguish drop vs inner shadows.
 */
data class Shadow (
    /**
     * Shadow color (hex with alpha, or token reference)
     */
    val color: String? = null,

    /**
     * Horizontal offset in dp/px
     */
    val offsetX: Double? = null,

    /**
     * Vertical offset in dp/px
     */
    val offsetY: Double? = null,

    /**
     * Blur radius in dp/px
     */
    val radius: Double? = null,

    /**
     * Shadow type. 'drop' is an outer shadow (default, backward-compatible). 'inner' is an
     * inset shadow. Platforms without first-class inner shadow support fall back to drop with a
     * diagnostic.
     */
    val type: ShadowType? = null
)

/**
 * Shadow type. 'drop' is an outer shadow (default, backward-compatible). 'inner' is an
 * inset shadow. Platforms without first-class inner shadow support fall back to drop with a
 * diagnostic.
 */
enum class ShadowType(val value: String) {
    Drop("drop"),
    Inner("inner");

    companion object {
        fun fromValue(value: String): ShadowType = when (value) {
            "drop"  -> Drop
            "inner" -> Inner
            else    -> throw IllegalArgumentException()
        }
    }
}

/**
 * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
 * (default 0); 'up' increments from snapshotSeconds with no upper bound unless
 * stopAtSeconds is set.
 */
enum class TickDirection(val value: String) {
    Down("down"),
    Up("up");

    companion object {
        fun fromValue(value: String): TickDirection = when (value) {
            "down" -> Down
            "up"   -> Up
            else   -> throw IllegalArgumentException()
        }
    }
}


/**
 * Font weight tokens for atomic Text elements.
 */
enum class TextWeight(val value: String) {
    Bold("bold"),
    Medium("medium"),
    Regular("regular"),
    SemiBold("semiBold");

    companion object {
        fun fromValue(value: String): TextWeight = when (value) {
            "bold"     -> Bold
            "medium"   -> Medium
            "regular"  -> Regular
            "semiBold" -> SemiBold
            else       -> throw IllegalArgumentException()
        }
    }
}

enum class Capability(val value: String) {
    Airplay("airplay"),
    BackgroundAudio("backgroundAudio"),
    Chromecast("chromecast"),
    FullscreenRotation("fullscreenRotation"),
    Pip("pip");

    companion object {
        fun fromValue(value: String): Capability = when (value) {
            "airplay"            -> Airplay
            "backgroundAudio"    -> BackgroundAudio
            "chromecast"         -> Chromecast
            "fullscreenRotation" -> FullscreenRotation
            "pip"                -> Pip
            else                 -> throw IllegalArgumentException()
        }
    }
}

/**
 * Defines a single column in the boxscore table
 */
data class BoxscoreColumnDefinition (
    /**
     * Whether this column should be visually emphasised (e.g., bold)
     */
    val highlighted: Boolean? = null,

    /**
     * Property key on each player's stats object that supplies this column's value
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val key: String,

    /**
     * Column header text displayed to the user
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    /**
     * Whether this column supports client-side sorting
     */
    val sortable: Boolean? = null,

    /**
     * Optional hint for column width (e.g. 'auto', '64px', '1fr')
     */
    val width: String? = null
)

data class DateMetadatum (
    /**
     * Number of games on this date.
     */
    val gameCount: Long? = null,

    /**
     * True if a user-favorited team plays on this date.
     */
    val hasTeamGame: Boolean? = null
)

data class DisplayConfig (
    val aspectRatio: String? = null,
    val height: Long? = null
)

/**
 * One input field inside a form section
 */
data class FormField (
    val disabled: Boolean? = null,

    @get:JsonProperty("fieldId", required=true)@field:JsonProperty("fieldId", required=true)
    val fieldID: String,

    /**
     * Input type; clients map to platform-native controls
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val fieldType: FieldType,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    /**
     * For select/radio/checkbox field types: the available choices
     */
    val options: List<FormOption>? = null,

    val placeholder: String? = null,
    val required: Boolean? = null,

    /**
     * Screen-state key that holds this field's current value
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val stateKey: String,

    /**
     * Message to show when validation fails
     */
    val validationMessage: String? = null,

    /**
     * Optional regex pattern for client-side validation
     */
    val validationPattern: String? = null,

    /**
     * How to realize the control. Applies only when fieldType == 'select'. Missing value is
     * treated as 'dropdown' at render time.
     */
    val variant: SelectVariant? = null
)

/**
 * Input type; clients map to platform-native controls
 */
enum class FieldType(val value: String) {
    Checkbox("checkbox"),
    Date("date"),
    Number("number"),
    Radio("radio"),
    Select("select"),
    Text("text"),
    Textarea("textarea"),
    Toggle("toggle");

    companion object {
        fun fromValue(value: String): FieldType = when (value) {
            "checkbox" -> Checkbox
            "date"     -> Date
            "number"   -> Number
            "radio"    -> Radio
            "select"   -> Select
            "text"     -> Text
            "textarea" -> Textarea
            "toggle"   -> Toggle
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * One selectable option within a select/radio/checkbox form field
 */
data class FormOption (
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val value: String
)

/**
 * How to realize the control. Applies only when fieldType == 'select'. Missing value is
 * treated as 'dropdown' at render time.
 *
 * How a Form single-select field is realized by the client. 'dropdown' maps to the platform
 * menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies
 * only when FormField.fieldType == 'select'.
 */
enum class SelectVariant(val value: String) {
    Chips("chips"),
    Dropdown("dropdown");

    companion object {
        fun fromValue(value: String): SelectVariant = when (value) {
            "chips"    -> Chips
            "dropdown" -> Dropdown
            else       -> throw IllegalArgumentException()
        }
    }
}

/**
 * Layout hint for field arrangement
 */
enum class Layout(val value: String) {
    Grid("grid"),
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object {
        fun fromValue(value: String): Layout = when (value) {
            "grid"       -> Grid
            "horizontal" -> Horizontal
            "vertical"   -> Vertical
            else         -> throw IllegalArgumentException()
        }
    }
}

/**
 * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
 * when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
 * width/height. Required so the stub renderer has no client-side chrome defaults.
 */
data class Placeholder (
    /**
     * Fill color for the empty rectangle.
     */
    val backgroundColor: String? = null,

    /**
     * Caption rendered inside the empty rectangle (e.g. 'Advertisement').
     */
    val text: String? = null
)

/**
 * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
 */
enum class PlayerType(val value: String) {
    Event("event"),
    Game("game"),
    NbaTv("nbaTv"),
    Stream("stream"),
    VOD("vod");

    companion object {
        fun fromValue(value: String): PlayerType = when (value) {
            "event"  -> Event
            "game"   -> Game
            "nbaTv"  -> NbaTv
            "stream" -> Stream
            "vod"    -> VOD
            else     -> throw IllegalArgumentException()
        }
    }
}

/**
 * One player row inside a boxscore table
 *
 * One ranked player row in a season leaders table
 */
data class PlayerRow (
    val actions: List<Action>? = null,

    @get:JsonProperty("imageUrl")@field:JsonProperty("imageUrl")
    val imageURL: String? = null,

    val jerseyNumber: String? = null,

    /**
     * Display name (short form, e.g. 'J. Tatum')
     *
     * Display name, e.g. 'Luka Dončić'
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val name: String,

    @get:JsonProperty("playerId", required=true)@field:JsonProperty("playerId", required=true)
    val playerID: String,

    val position: String? = null,

    /**
     * Whether this player was in the starting lineup
     */
    val starter: Boolean? = null,

    /**
     * Stat values keyed by column key (gp, min, pts, reb, ast, etc.)
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val stats: Map<String, Any?>,

    /**
     * Ranking position (1-based)
     */
    val rank: Long? = null,

    /**
     * Team tricode, e.g. 'LAL'
     */
    val team: String? = null
)

enum class SortDirection(val value: String) {
    Asc("asc"),
    Desc("desc");

    companion object {
        fun fromValue(value: String): SortDirection = when (value) {
            "asc"  -> Asc
            "desc" -> Desc
            else   -> throw IllegalArgumentException()
        }
    }
}

data class TabData (
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String,

    val stateKey: String? = null,
    val stateValue: String? = null
)

data class SubscriptionTier (
    /**
     * Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
     */
    val badgeText: String? = null,

    val ctaAction: Action? = null,
    val ctaLabel: String? = null,
    val features: List<String>? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val name: String,

    /**
     * Strikethrough price if on sale
     */
    val originalPrice: String? = null,

    /**
     * Display price, e.g. '$14.99/mo'
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val price: String
)

data class DataBinding (
    val bindings: List<DataBindingPath>? = null,

    /**
     * Optional map of targetPath to translation key for client-side i18n resolution on bound
     * fields
     */
    val stringKeys: Map<String, String>? = null
)

data class DataBindingPath (
    /**
     * JSONPath in incoming message (e.g., '$.homeTeam.score')
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val sourcePath: String,

    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val targetPath: String,

    /**
     * Optional server-declared transform applied by shared client binding infrastructure before
     * writing the target value. liveClockSnapshot normalizes clock payload values into {
     * snapshotSeconds, snapshotAt, isRunning }.
     */
    val transform: Transform? = null
)

/**
 * Optional server-declared transform applied by shared client binding infrastructure before
 * writing the target value. liveClockSnapshot normalizes clock payload values into {
 * snapshotSeconds, snapshotAt, isRunning }.
 */
enum class Transform(val value: String) {
    LiveClockSnapshot("liveClockSnapshot");

    companion object {
        fun fromValue(value: String): Transform = when (value) {
            "liveClockSnapshot" -> LiveClockSnapshot
            else                -> throw IllegalArgumentException()
        }
    }
}

/**
 * Server-declared loading and error presentation for a section. Clients render these states
 * when applicable.
 */
data class SectionStates (
    /**
     * Server-declared error presentation for this section.
     */
    val error: ErrorState? = null,

    val loading: Loading? = null
)

/**
 * Server-declared error presentation for this section.
 *
 * Server-declared error-state shape rendered by section error boundaries. Named
 * `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's
 * native error protocol (e.g. `Swift.Error`).
 */
data class ErrorState (
    /**
     * If true, collapse the section entirely on error instead of showing error UI
     */
    val hideOnError: Boolean? = null,

    /**
     * Error message to display (e.g., 'Unable to load scores')
     */
    val message: String? = null,

    /**
     * Optional action to trigger on retry tap (typically a refresh action)
     */
    val retryAction: Action? = null,

    /**
     * Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a
     * neutral default when omitted.
     */
    val retryLabel: String? = null
)

data class Loading (
    /**
     * Minimum height to reserve during loading (prevents layout shift)
     */
    @get:JsonProperty("minHeightDp")@field:JsonProperty("minHeightDp")
    val minHeightDP: Long? = null,

    /**
     * Which loading skeleton style to use
     */
    val skeleton: Skeleton? = null
)

/**
 * Which loading skeleton style to use
 */
enum class Skeleton(val value: String) {
    None("none"),
    Placeholder("placeholder"),
    Shimmer("shimmer"),
    Spinner("spinner");

    companion object {
        fun fromValue(value: String): Skeleton = when (value) {
            "none"        -> None
            "placeholder" -> Placeholder
            "shimmer"     -> Shimmer
            "spinner"     -> Spinner
            else          -> throw IllegalArgumentException()
        }
    }
}

/**
 * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
 */
data class Subsection (
    /**
     * Subsection-level accessibility metadata
     */
    val accessibility: AccessibilityProperties? = null,

    val actions: List<Action>? = null,

    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String
)

/**
 * Server-driven surface spec applied by the client's SectionRouter to every semantic
 * section — the visual wrapper beneath the section's content. Mirrors the inline-chrome
 * vocabulary on AtomicContainer so semantic sections have schema parity with composed
 * sections. Every client's shared SectionContainer wrapper reads these fields;
 * semantic-section renderers do not set outer padding, margin, corner radius, shadow,
 * border, or background themselves. The sibling `data` field carries content (including the
 * atomic UI tree); `surface` carries the frame that sits beneath it.
 */
data class SectionSurface (
    /**
     * Surface background (solid, gradient, or image).
     */
    val background: BackgroundUnion? = null,

    /**
     * Outer stroke applied around the surface.
     */
    val border: Border? = null,

    /**
     * Corner radius: dp/px or layout token, applied to the surface (with overflow clip).
     */
    val cornerRadius: LayoutScalar? = null,

    /**
     * Outer margin (space between the surface and its siblings / screen edge).
     */
    val margin: Spacing? = null,

    /**
     * Inner padding (space between the surface edge and the content it wraps).
     */
    val padding: Spacing? = null,

    /**
     * Drop shadow applied to the surface.
     */
    val shadow: ShadowOrToken? = null
)

/**
 * Outer stroke applied around the surface.
 *
 * Outer stroke applied around a container or section.
 */
data class Border (
    /**
     * Stroke color (hex or token)
     */
    val color: String? = null,

    /**
     * Stroke width in dp/px
     */
    val width: Double? = null
)


/**
 * Server-exposed A/B / experiment variants available for this screen. Clients read
 * `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
 * to the server on subsequent requests. Omit for screens without active experiments.
 *
 * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets
 * clients expose variant selection without hardcoding experiment ids or option vocabularies.
 */
data class ExperimentVariants (
    /**
     * Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key
     * back to the server as part of the experiments map on subsequent requests.
     */
    @get:JsonProperty("experimentId", required=true)@field:JsonProperty("experimentId", required=true)
    val experimentID: String,

    /**
     * Ordered list of variants the client may choose from.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val options: List<ExperimentVariantOption>
)

/**
 * One variant within an experiment.
 */
data class ExperimentVariantOption (
    /**
     * Optional longer description shown alongside the label.
     */
    val description: String? = null,

    /**
     * Variant identifier (e.g. `A`, `B`). Opaque to clients.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val id: String,

    /**
     * Human-readable label rendered in variant pickers.
     */
    @get:JsonProperty(required=true)@field:JsonProperty(required=true)
    val label: String
)
