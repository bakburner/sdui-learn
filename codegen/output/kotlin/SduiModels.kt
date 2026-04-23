// To parse the JSON, install Klaxon and do:
//
//   val sduiModels = SduiModels.fromJson(jsonString)

package com.nba.sdui.models.quicktype

import com.beust.klaxon.*

private fun <T> Klaxon.convert(k: kotlin.reflect.KClass<*>, fromJson: (JsonValue) -> T, toJson: (T) -> String, isUnion: Boolean = false) =
    this.converter(object: Converter {
        @Suppress("UNCHECKED_CAST")
        override fun toJson(value: Any)        = toJson(value as T)
        override fun fromJson(jv: JsonValue)   = fromJson(jv) as Any
        override fun canConvert(cls: Class<*>) = cls == k.java || (isUnion && cls.superclass == k.java)
    })

private val klaxon = Klaxon()
    .convert(Destination::class,            { Destination.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(FailureFeedbackStyle::class,   { FailureFeedbackStyle.fromValue(it.string!!) },   { "\"${it.value}\"" })
    .convert(ImpressionDedup::class,        { ImpressionDedup.fromValue(it.string!!) },        { "\"${it.value}\"" })
    .convert(ModalHeight::class,            { ModalHeight.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(FailurePolicy::class,          { FailurePolicy.fromValue(it.string!!) },          { "\"${it.value}\"" })
    .convert(MutateOperation::class,        { MutateOperation.fromValue(it.string!!) },        { "\"${it.value}\"" })
    .convert(NavigationPresentation::class, { NavigationPresentation.fromValue(it.string!!) }, { "\"${it.value}\"" })
    .convert(ActionTrigger::class,          { ActionTrigger.fromValue(it.string!!) },          { "\"${it.value}\"" })
    .convert(ActionType::class,             { ActionType.fromValue(it.string!!) },             { "\"${it.value}\"" })
    .convert(RefreshType::class,            { RefreshType.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(BadgeAlignment::class,         { BadgeAlignment.fromValue(it.string!!) },         { "\"${it.value}\"" })
    .convert(LiveRegion::class,             { LiveRegion.fromValue(it.string!!) },             { "\"${it.value}\"" })
    .convert(Role::class,                   { Role.fromValue(it.string!!) },                   { "\"${it.value}\"" })
    .convert(Alignment::class,              { Alignment.fromValue(it.string!!) },              { "\"${it.value}\"" })
    .convert(Direction::class,              { Direction.fromValue(it.string!!) },              { "\"${it.value}\"" })
    .convert(ScaleType::class,              { ScaleType.fromValue(it.string!!) },              { "\"${it.value}\"" })
    .convert(Align::class,                  { Align.fromValue(it.string!!) },                  { "\"${it.value}\"" })
    .convert(WidthEnum::class,              { WidthEnum.fromValue(it.string!!) },              { "\"${it.value}\"" })
    .convert(CrossAlignment::class,         { CrossAlignment.fromValue(it.string!!) },         { "\"${it.value}\"" })
    .convert(UIDirection::class,            { UIDirection.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(ImageFit::class,               { ImageFit.fromValue(it.string!!) },               { "\"${it.value}\"" })
    .convert(Orientation::class,            { Orientation.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(UIType::class,                 { UIType.fromValue(it.string!!) },                 { "\"${it.value}\"" })
    .convert(TextWeight::class,             { TextWeight.fromValue(it.string!!) },             { "\"${it.value}\"" })
    .convert(Capability::class,             { Capability.fromValue(it.string!!) },             { "\"${it.value}\"" })
    .convert(ScoreTextStyle::class,         { ScoreTextStyle.fromValue(it.string!!) },         { "\"${it.value}\"" })
    .convert(FieldType::class,              { FieldType.fromValue(it.string!!) },              { "\"${it.value}\"" })
    .convert(Layout::class,                 { Layout.fromValue(it.string!!) },                 { "\"${it.value}\"" })
    .convert(PlayerType::class,             { PlayerType.fromValue(it.string!!) },             { "\"${it.value}\"" })
    .convert(SortDirection::class,          { SortDirection.fromValue(it.string!!) },          { "\"${it.value}\"" })
    .convert(Priority::class,               { Priority.fromValue(it.string!!) },               { "\"${it.value}\"" })
    .convert(Skeleton::class,               { Skeleton.fromValue(it.string!!) },               { "\"${it.value}\"" })
    .convert(OverlayType::class,            { OverlayType.fromValue(it.string!!) },            { "\"${it.value}\"" })
    .convert(BackgroundUnion::class,        { BackgroundUnion.fromJson(it) },                  { it.toJson() }, true)
    .convert(Overlay::class,                { Overlay.fromJson(it) },                          { it.toJson() }, true)
    .convert(WidthUnion::class,             { WidthUnion.fromJson(it) },                       { it.toJson() }, true)

/**
 * Server-Driven UI schema for NBA Game Detail screens
 */
data class SduiModels (
    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     */
    val actions: List<Action>? = null,

    @Json(name = "analyticsId")
    val analyticsID: String? = null,

    val defaultRefreshPolicy: RefreshPolicy? = null,
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
    @Json(name = "parentUri")
    val parentURI: String? = null,

    val schemaVersion: String,
    val sections: List<Section>,
    val state: Map<String, Any?>? = null,
    val title: String? = null,

    @Json(name = "traceId")
    val traceID: String? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<SduiModels>(json)
    }
}

/**
 * Action fired when the form is submitted
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
    @Json(name = "targetUri")
    val targetURI: String? = null,

    val trigger: ActionTrigger,
    val type: ActionType,

    /**
     * For mutate actions: the value to apply with the operation
     */
    val value: Any? = null,

    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     */
    @Json(name = "webUrl")
    val webURL: String? = null
)

enum class Destination(val value: String) {
    Adobe("adobe"),
    All("all"),
    Firebase("firebase"),
    Internal("internal");

    companion object {
        public fun fromValue(value: String): Destination = when (value) {
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
        public fun fromValue(value: String): FailureFeedbackStyle = when (value) {
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
    @Json(name = "intervalMs")
    val intervalMS: Long? = null,

    val threshold: ImpressionThreshold? = null
)

enum class ImpressionDedup(val value: String) {
    None("none"),
    OncePerInterval("once-per-interval"),
    OncePerScreen("once-per-screen"),
    OncePerSession("once-per-session");

    companion object {
        public fun fromValue(value: String): ImpressionDedup = when (value) {
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
    @Json(name = "dwellMs")
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
        public fun fromValue(value: String): ModalHeight = when (value) {
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
        public fun fromValue(value: String): FailurePolicy = when (value) {
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
        public fun fromValue(value: String): MutateOperation = when (value) {
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
        public fun fromValue(value: String): NavigationPresentation = when (value) {
            "external"   -> External
            "fullscreen" -> Fullscreen
            "modal"      -> Modal
            "push"       -> Push
            "replace"    -> Replace
            else         -> throw IllegalArgumentException()
        }
    }
}

enum class ActionTrigger(val value: String) {
    OnBlur("onBlur"),
    OnFocus("onFocus"),
    OnLongPress("onLongPress"),
    OnSubmit("onSubmit"),
    OnSwipe("onSwipe"),
    OnTap("onTap"),
    OnVisible("onVisible");

    companion object {
        public fun fromValue(value: String): ActionTrigger = when (value) {
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
        public fun fromValue(value: String): ActionType = when (value) {
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
    @Json(name = "intervalMs")
    val intervalMS: Long? = null,

    /**
     * Whether the client should pause this section's refresh when it scrolls out of the
     * viewport. Default true. Set false for critical live sections (e.g., GamePanel scores)
     * that should refresh continuously.
     */
    val pauseWhenOffScreen: Boolean? = null,

    val type: RefreshType,

    /**
     * For poll/sse type: URL to poll or connect to. If omitted, polls the SDUI endpoint.
     */
    val url: String? = null
)

enum class RefreshType(val value: String) {
    Poll("poll"),
    SSE("sse"),
    Static("static");

    companion object {
        public fun fromValue(value: String): RefreshType = when (value) {
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
    val id: String,
    val label: String,
    val selected: Boolean? = null,

    @Json(name = "targetUri")
    val targetURI: String? = null
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
    val element: AtomicElement
)

/**
 * Root node of the atomic element tree — the rendering instructions
 *
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 *
 * The element to render as a badge
 */
data class AtomicElement (
    /**
     * Server-provided accessibility metadata for this atomic element
     */
    val accessibility: AccessibilityProperties? = null,

    val actions: List<Action>? = null,
    val alignment: Alignment? = null,

    /**
     * Deprecated: use accessibility.label instead. Retained for backward compatibility; clients
     * prefer accessibility.label when present.
     */
    val alt: String? = null,

    val aspectRatio: Double? = null,
    val background: BackgroundUnion? = null,

    /**
     * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
     */
    val badge: Badge? = null,

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
     * Corner radius in dp/px. Applied to Container (with overflow clip) and Image elements.
     */
    val cornerRadius: Long? = null,

    val crossAlignment: CrossAlignment? = null,
    val direction: UIDirection? = null,
    val disabled: Boolean? = null,
    val falseChild: AtomicElement? = null,
    val fit: ImageFit? = null,

    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space
     * along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     */
    val flex: Double? = null,

    val gap: Long? = null,
    val height: Long? = null,
    val icon: String? = null,
    val id: String? = null,
    val label: String? = null,
    val maxLines: Long? = null,

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
    val padding: Spacing? = null,
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
     * Drop shadow applied to the element. Replaces elevation with richer CSS/SwiftUI shadow
     * semantics.
     */
    val shadow: Shadow? = null,

    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel
     * presentation.
     */
    val showIndicators: Boolean? = null,

    val size: Long? = null,
    val snapAlignment: Align? = null,
    val src: String? = null,

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
    val trueChild: AtomicElement? = null,
    val type: UIType,

    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text,
     * ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image.
     * Renderers parse this string against the primitive's enum and log a diagnostic on
     * unrecognized values.
     */
    val variant: String? = null,

    val weight: TextWeight? = null,
    val width: Long? = null
)

/**
 * Section-specific data payload
 *
 * Tabbed navigation with dynamic content sections per tab
 *
 * Typed tabular data for an NBA-style boxscore (one per team)
 *
 * Server-driven form section with typed fields bound to screen state
 *
 * Ad placement primitive — carries placement semantics while delegating auction/targeting
 * to ad-platform SDKs (see ADR-007)
 *
 * Sortable, paginated table of season statistical leaders (league-wide)
 *
 * Inline subscription upsell banner with headline, body copy, and CTA
 *
 * Full-screen subscription upsell hero with multi-tier pricing and feature list
 *
 * Data payload for AtomicComposite sections — ui contains rendering instructions, content
 * carries domain data
 *
 * Video player section — platform SDK integration (DRM, HLS, ad insertion). Same
 * justification as SubscribeHero (StoreKit/Play Billing) and AdSlot (GAM).
 */
data class Data (
    val defaultTab: String? = null,
    val stateKey: String? = null,
    val tabContents: Map<String, List<Section>>? = null,
    val tabs: List<TabData>? = null,
    val actions: List<Action>? = null,
    val awayTeam: TeamData? = null,

    /**
     * Badge/chip label, e.g. 'LIVE', 'FEATURED'
     */
    val badgeText: String? = null,

    /**
     * Whether the game clock is actively ticking. When true, renderers should interpolate the
     * clock locally between SSE updates for visual continuity.
     */
    val clockRunning: Boolean? = null,

    /**
     * Server-driven visual configuration — controls all layout and styling knobs
     */
    val displayConfig: GamePanelDisplayConfig? = null,

    /**
     * Game clock string (e.g. 'PT05M32.00S' or '5:32')
     */
    val gameClock: String? = null,

    @Json(name = "gameId")
    val gameID: String? = null,

    val gameLeaders: GameLeadersData? = null,
    val gameStatus: Long? = null,
    val gameStatusText: String? = null,
    val gameTimeEt: String? = null,
    val homeTeam: TeamData? = null,

    /**
     * Current game period (quarter number)
     */
    val period: Long? = null,

    /**
     * Secondary label shown above the matchup (e.g. team name, 'Recommended')
     */
    val visualLabel: String? = null,

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

    @Json(name = "teamLogoUrl")
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
     * Ad network identifier, e.g. 'gam', 'amazon'
     */
    val provider: String? = null,

    /**
     * Optional auto-refresh interval in seconds
     */
    @Json(name = "refreshIntervalSec")
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

    val background: BackgroundUnion? = null,
    val ctaAction: Action? = null,
    val ctaLabel: String? = null,

    @Json(name = "logoUrl")
    val logoURL: String? = null,

    /**
     * Optional pricing tier highlights
     */
    val tiers: List<SubscriptionTier>? = null,

    /**
     * Bullet-point feature list
     */
    val features: List<String>? = null,

    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
     * data-binding support.
     */
    val content: Map<String, Any?>? = null,

    /**
     * Root node of the atomic element tree — the rendering instructions
     */
    val ui: AtomicElement? = null,

    val autoplay: Boolean? = null,

    /**
     * Platform capabilities the player should enable. Server includes only capabilities
     * relevant to the requesting platform (via X-Platform header).
     */
    val capabilities: List<Capability>? = null,

    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId
     * for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     */
    @Json(name = "contentId")
    val contentID: String? = null,

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

    @Json(name = "analyticsId")
    val analyticsID: String? = null,

    val backgroundColor: String? = null,

    /**
     * Section-specific data payload
     */
    val data: Data? = null,

    val dataBinding: DataBinding? = null,
    val id: String,
    val layoutHints: SectionLayoutHints? = null,
    val padding: Spacing? = null,
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

    val type: OverlayType
)

/**
 * Position of the badge within the parent bounds
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
        public fun fromValue(value: String): BadgeAlignment = when (value) {
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
        public fun fromValue(value: String): LiveRegion = when (value) {
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
        public fun fromValue(value: String): Role = when (value) {
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

enum class Alignment(val value: String) {
    Center("center"),
    End("end"),
    SpaceAround("spaceAround"),
    SpaceBetween("spaceBetween"),
    SpaceEvenly("spaceEvenly"),
    Start("start");

    companion object {
        public fun fromValue(value: String): Alignment = when (value) {
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
 * Default background (pre-game, final)
 *
 * Shared background type — solid color, gradient, or image with overlay
 *
 * Background override when game is LIVE
 */
sealed class BackgroundUnion {
    class BackgroundValue(val value: Background) : BackgroundUnion()
    class StringValue(val value: String)         : BackgroundUnion()

    public fun toJson(): String = klaxon.toJsonString(when (this) {
        is BackgroundValue -> this.value
        is StringValue     -> this.value
    })

    companion object {
        public fun fromJson(jv: JsonValue): BackgroundUnion = when (jv.inside) {
            is JsonObject -> BackgroundValue(jv.obj?.let { klaxon.parseFromJsonObject<Background>(it) }!!)
            is String     -> StringValue(jv.string!!)
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
    @Json(name = "imageUrl")
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
        public fun fromValue(value: String): Direction = when (value) {
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

    public fun toJson(): String = klaxon.toJsonString(when (this) {
        is BackgroundGradientValue -> this.value
        is StringValue             -> this.value
    })

    companion object {
        public fun fromJson(jv: JsonValue): Overlay = when (jv.inside) {
            is JsonObject -> BackgroundGradientValue(jv.obj?.let { klaxon.parseFromJsonObject<BackgroundGradient>(it) }!!)
            is String     -> StringValue(jv.string!!)
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
    val colors: List<String>,

    val direction: Direction? = null
)

enum class ScaleType(val value: String) {
    Contain("contain"),
    Cover("cover"),
    Fill("fill");

    companion object {
        public fun fromValue(value: String): ScaleType = when (value) {
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
    val key: String,

    /**
     * Header label
     */
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
        public fun fromValue(value: String): Align = when (value) {
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

    public fun toJson(): String = klaxon.toJsonString(when (this) {
        is EnumValue    -> this.value
        is IntegerValue -> this.value
    })

    companion object {
        public fun fromJson(jv: JsonValue): WidthUnion = when (jv.inside) {
            is String       -> EnumValue(jv.string?.let { WidthEnum.fromValue(it) }!!)
            is Int, is Long -> IntegerValue((jv.int?.toLong() ?: jv.longValue)!!)
            else            -> throw IllegalArgumentException()
        }
    }
}

enum class WidthEnum(val value: String) {
    Flex("flex");

    companion object {
        public fun fromValue(value: String): WidthEnum = when (value) {
            "flex" -> Flex
            else   -> throw IllegalArgumentException()
        }
    }
}

enum class CrossAlignment(val value: String) {
    Center("center"),
    End("end"),
    Start("start"),
    Stretch("stretch");

    companion object {
        public fun fromValue(value: String): CrossAlignment = when (value) {
            "center"  -> Center
            "end"     -> End
            "start"   -> Start
            "stretch" -> Stretch
            else      -> throw IllegalArgumentException()
        }
    }
}

enum class UIDirection(val value: String) {
    Column("column"),
    Row("row");

    companion object {
        public fun fromValue(value: String): UIDirection = when (value) {
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
        public fun fromValue(value: String): ImageFit = when (value) {
            "contain" -> Contain
            "cover"   -> Cover
            "fill"    -> Fill
            "none"    -> None
            else      -> throw IllegalArgumentException()
        }
    }
}

enum class Orientation(val value: String) {
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object {
        public fun fromValue(value: String): Orientation = when (value) {
            "horizontal" -> Horizontal
            "vertical"   -> Vertical
            else         -> throw IllegalArgumentException()
        }
    }
}

data class Spacing (
    val bottom: Long? = null,
    val end: Long? = null,
    val start: Long? = null,
    val top: Long? = null
)

/**
 * Drop shadow applied to the element. Replaces elevation with richer CSS/SwiftUI shadow
 * semantics.
 *
 * Drop shadow with CSS/SwiftUI semantics (radius + offset). Compose approximates via
 * elevation.
 */
data class Shadow (
    /**
     * Shadow color (hex with alpha)
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
    val radius: Double? = null
)

enum class UIType(val value: String) {
    Button("Button"),
    Conditional("Conditional"),
    Container("Container"),
    DisplayGrid("DisplayGrid"),
    Divider("Divider"),
    Image("Image"),
    ScrollContainer("ScrollContainer"),
    SectionSlot("SectionSlot"),
    Spacer("Spacer"),
    Text("Text");

    companion object {
        public fun fromValue(value: String): UIType = when (value) {
            "Button"          -> Button
            "Conditional"     -> Conditional
            "Container"       -> Container
            "DisplayGrid"     -> DisplayGrid
            "Divider"         -> Divider
            "Image"           -> Image
            "ScrollContainer" -> ScrollContainer
            "SectionSlot"     -> SectionSlot
            "Spacer"          -> Spacer
            "Text"            -> Text
            else              -> throw IllegalArgumentException()
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
        public fun fromValue(value: String): TextWeight = when (value) {
            "bold"     -> Bold
            "medium"   -> Medium
            "regular"  -> Regular
            "semiBold" -> SemiBold
            else       -> throw IllegalArgumentException()
        }
    }
}

data class TeamData (
    @Json(name = "logoUrl")
    val logoURL: String? = null,

    val score: Long,
    val teamCity: String,

    @Json(name = "teamId")
    val teamID: Long,

    val teamName: String,
    val teamTricode: String
)

enum class Capability(val value: String) {
    Airplay("airplay"),
    BackgroundAudio("backgroundAudio"),
    Chromecast("chromecast"),
    FullscreenRotation("fullscreenRotation"),
    Pip("pip");

    companion object {
        public fun fromValue(value: String): Capability = when (value) {
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
    val key: String,

    /**
     * Column header text displayed to the user
     */
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

/**
 * Server-driven visual configuration — controls all layout and styling knobs
 *
 * Server-driven visual configuration for GamePanel — replaces the hardcoded variant branch
 */
data class GamePanelDisplayConfig (
    /**
     * Default background (pre-game, final)
     */
    val background: BackgroundUnion? = null,

    /**
     * Badge/chip background color (hex)
     */
    val badgeColor: String? = null,

    /**
     * Fixed card height in dp/px. Null/absent = auto-size
     */
    val cardHeight: Long? = null,

    /**
     * Card corner radius in dp/px
     */
    val cornerRadius: Long? = null,

    /**
     * Card elevation/shadow in dp/px
     */
    val elevation: Long? = null,

    /**
     * Background override when game is LIVE
     */
    val liveBackground: BackgroundUnion? = null,

    /**
     * Team logo width/height in dp/px
     */
    val logoSize: Long? = null,

    /**
     * Score typography: compact = bodyLarge+Bold, prominent = headlineMedium+ExtraBold
     */
    val scoreTextStyle: ScoreTextStyle? = null,

    val aspectRatio: String? = null,
    val height: Long? = null
)

/**
 * Score typography: compact = bodyLarge+Bold, prominent = headlineMedium+ExtraBold
 */
enum class ScoreTextStyle(val value: String) {
    Compact("compact"),
    Prominent("prominent");

    companion object {
        public fun fromValue(value: String): ScoreTextStyle = when (value) {
            "compact"   -> Compact
            "prominent" -> Prominent
            else        -> throw IllegalArgumentException()
        }
    }
}

/**
 * One input field inside a form section
 */
data class FormField (
    val disabled: Boolean? = null,

    @Json(name = "fieldId")
    val fieldID: String,

    /**
     * Input type; clients map to platform-native controls
     */
    val fieldType: FieldType,

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
    val stateKey: String,

    /**
     * Message to show when validation fails
     */
    val validationMessage: String? = null,

    /**
     * Optional regex pattern for client-side validation
     */
    val validationPattern: String? = null
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
        public fun fromValue(value: String): FieldType = when (value) {
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
    val label: String,
    val value: String
)

data class GameLeadersData (
    val awayLeader: GameLeaderData? = null,
    val homeLeader: GameLeaderData? = null
)

data class GameLeaderData (
    val assists: Long? = null,
    val name: String? = null,
    val points: Long? = null,
    val rebounds: Long? = null
)

/**
 * Layout hint for field arrangement
 */
enum class Layout(val value: String) {
    Grid("grid"),
    Horizontal("horizontal"),
    Vertical("vertical");

    companion object {
        public fun fromValue(value: String): Layout = when (value) {
            "grid"       -> Grid
            "horizontal" -> Horizontal
            "vertical"   -> Vertical
            else         -> throw IllegalArgumentException()
        }
    }
}

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
        public fun fromValue(value: String): PlayerType = when (value) {
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

    @Json(name = "imageUrl")
    val imageURL: String? = null,

    val jerseyNumber: String? = null,

    /**
     * Display name (short form, e.g. 'J. Tatum')
     *
     * Display name, e.g. 'Luka Dončić'
     */
    val name: String,

    @Json(name = "playerId")
    val playerID: String,

    val position: String? = null,

    /**
     * Whether this player was in the starting lineup
     */
    val starter: Boolean? = null,

    /**
     * Stat values keyed by column key (gp, min, pts, reb, ast, etc.)
     */
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
        public fun fromValue(value: String): SortDirection = when (value) {
            "asc"  -> Asc
            "desc" -> Desc
            else   -> throw IllegalArgumentException()
        }
    }
}

data class TabData (
    val id: String,
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
    val id: String,

    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     */
    val name: String,

    /**
     * Strikethrough price if on sale
     */
    val originalPrice: String? = null,

    /**
     * Display price, e.g. '$14.99/mo'
     */
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
    val sourcePath: String,

    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     */
    val targetPath: String
)

/**
 * Optional layout hints for section placement. Clients apply best-effort; unknown hints are
 * ignored.
 */
data class SectionLayoutHints (
    /**
     * Render a divider line above this section
     */
    val dividerAbove: Boolean? = null,

    /**
     * Render a divider line below this section
     */
    val dividerBelow: Boolean? = null,

    /**
     * Bottom margin in dp/points
     */
    val marginBottom: Long? = null,

    /**
     * Top margin in dp/points (0 = flush)
     */
    val marginTop: Long? = null,

    /**
     * Rendering priority hint — clients may use for lazy loading or viewport priority
     */
    val priority: Priority? = null
)

/**
 * Rendering priority hint — clients may use for lazy loading or viewport priority
 */
enum class Priority(val value: String) {
    High("high"),
    Low("low"),
    Normal("normal");

    companion object {
        public fun fromValue(value: String): Priority = when (value) {
            "high"   -> High
            "low"    -> Low
            "normal" -> Normal
            else     -> throw IllegalArgumentException()
        }
    }
}

/**
 * Server-declared loading and error presentation for a section. Clients render these states
 * when applicable.
 */
data class SectionStates (
    val error: Error? = null,
    val loading: Loading? = null
)

data class Error (
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
    val retryAction: Action? = null
)

data class Loading (
    /**
     * Minimum height to reserve during loading (prevents layout shift)
     */
    @Json(name = "minHeightDp")
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
        public fun fromValue(value: String): Skeleton = when (value) {
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
    val id: String
)

enum class OverlayType(val value: String) {
    AdSlot("AdSlot"),
    AtomicComposite("AtomicComposite"),
    BoxscoreTable("BoxscoreTable"),
    Form("Form"),
    GamePanel("GamePanel"),
    SeasonLeadersTable("SeasonLeadersTable"),
    SubscribeBanner("SubscribeBanner"),
    SubscribeHero("SubscribeHero"),
    TabGroup("TabGroup"),
    VideoPlayer("VideoPlayer");

    companion object {
        public fun fromValue(value: String): OverlayType = when (value) {
            "AdSlot"             -> AdSlot
            "AtomicComposite"    -> AtomicComposite
            "BoxscoreTable"      -> BoxscoreTable
            "Form"               -> Form
            "GamePanel"          -> GamePanel
            "SeasonLeadersTable" -> SeasonLeadersTable
            "SubscribeBanner"    -> SubscribeBanner
            "SubscribeHero"      -> SubscribeHero
            "TabGroup"           -> TabGroup
            "VideoPlayer"        -> VideoPlayer
            else                 -> throw IllegalArgumentException()
        }
    }
}
