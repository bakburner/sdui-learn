/**
 * Server-Driven UI schema for NBA Game Detail screens
 */
export interface SduiModels {
    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     */
    actions?:              Action[];
    analyticsId?:          string;
    defaultRefreshPolicy?: RefreshPolicy;
    id:                    string;
    navigation?:           Navigation;
    /**
     * Named overlay sections the client shows when a trigger condition arises. Keys are
     * developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed
     * sections (typically AtomicComposite). Client controls trigger timing and presentation
     * style; server controls display content.
     */
    overlays?: { [key: string]: Section };
    /**
     * URI the back button should navigate to.  Clients always show a back button; this field
     * tells them the target.  Omit for root screens (e.g. scoreboard).
     */
    parentUri?:    string;
    schemaVersion: string;
    sections:      Section[];
    state?:        { [key: string]: any };
    title?:        string;
    traceId?:      string;
    [property: string]: any;
}

/**
 * Action fired when the form is submitted
 *
 * Optional action to trigger on retry tap (typically a refresh action)
 */
export interface Action {
    /**
     * For fireAndForget actions: where to send the beacon
     */
    destinations?: Destination[];
    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     */
    endpoint?: string;
    /**
     * For fireAndForget actions: event name
     */
    event?: string;
    /**
     * Optional server-provided error message and presentation style. Client falls back to
     * generic localized string when absent
     */
    failureFeedback?: FailureFeedback;
    /**
     * For fireAndForget actions with onVisible trigger: impression tracking policy
     */
    impression?: ImpressionPolicy;
    /**
     * For toast actions: text message to display in the toast
     */
    message?: string;
    /**
     * For navigate actions with modal presentation: sheet height
     */
    modalHeight?: ModalHeight;
    /**
     * Sequence behavior when this action fails. Client applies per-type default when absent
     * (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
     */
    onFailure?: FailurePolicy;
    /**
     * For mutate actions: operation to perform on the state key
     */
    operation?: MutateOperation;
    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     */
    paramBindings?: { [key: string]: string };
    /**
     * For fireAndForget actions: event parameters
     */
    params?: { [key: string]: any };
    /**
     * For navigate actions: how the destination is presented
     */
    presentation?: NavigationPresentation;
    /**
     * For mutate actions: state key to update. For dismiss actions: what to dismiss
     * (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
     */
    target?: string;
    /**
     * For navigate actions: native deeplink URI
     */
    targetUri?: string;
    trigger:    ActionTrigger;
    type:       ActionType;
    /**
     * For mutate actions: the value to apply with the operation
     */
    value?: any;
    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     */
    webUrl?: string;
    [property: string]: any;
}

export enum Destination {
    Adobe = "adobe",
    All = "all",
    Firebase = "firebase",
    Internal = "internal",
}

/**
 * Optional server-provided error message and presentation style. Client falls back to
 * generic localized string when absent
 *
 * Optional server-provided error message and presentation style for action failures. Client
 * falls back to generic localized string when absent.
 */
export interface FailureFeedback {
    /**
     * Localized error message to display on failure
     */
    message?: string;
    /**
     * Presentation hint — clients map to closest platform-native mechanism
     */
    style?: FailureFeedbackStyle;
    [property: string]: any;
}

/**
 * Presentation hint — clients map to closest platform-native mechanism
 *
 * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
 */
export enum FailureFeedbackStyle {
    Inline = "inline",
    Snackbar = "snackbar",
    Toast = "toast",
}

/**
 * For fireAndForget actions with onVisible trigger: impression tracking policy
 *
 * Impression tracking policy for analytics actions with onVisible trigger
 */
export interface ImpressionPolicy {
    dedup?: ImpressionDedup;
    /**
     * Reset interval for once-per-interval strategy (milliseconds)
     */
    intervalMs?: number;
    threshold?:  ImpressionThreshold;
    [property: string]: any;
}

export enum ImpressionDedup {
    None = "none",
    OncePerInterval = "once-per-interval",
    OncePerScreen = "once-per-screen",
    OncePerSession = "once-per-session",
}

export interface ImpressionThreshold {
    /**
     * Milliseconds section must remain visible before impression fires
     */
    dwellMs?: number;
    /**
     * Fraction of section area that must be visible (0.5 = 50%)
     */
    visibility?: number;
    [property: string]: any;
}

/**
 * For navigate actions with modal presentation: sheet height
 */
export enum ModalHeight {
    Compact = "compact",
    Full = "full",
    Half = "half",
}

/**
 * Sequence behavior when this action fails. Client applies per-type default when absent
 * (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
 *
 * Sequence behavior when an action fails. Clients apply per-type defaults when absent:
 * navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
 */
export enum FailurePolicy {
    Continue = "continue",
    Halt = "halt",
    Silent = "silent",
}

/**
 * For mutate actions: operation to perform on the state key
 */
export enum MutateOperation {
    Append = "append",
    Increment = "increment",
    Set = "set",
    Toggle = "toggle",
}

/**
 * For navigate actions: how the destination is presented
 */
export enum NavigationPresentation {
    External = "external",
    Fullscreen = "fullscreen",
    Modal = "modal",
    Push = "push",
    Replace = "replace",
}

export enum ActionTrigger {
    OnBlur = "onBlur",
    OnFocus = "onFocus",
    OnLongPress = "onLongPress",
    OnSubmit = "onSubmit",
    OnSwipe = "onSwipe",
    OnTap = "onTap",
    OnVisible = "onVisible",
}

export enum ActionType {
    Dismiss = "dismiss",
    FireAndForget = "fireAndForget",
    Mutate = "mutate",
    Navigate = "navigate",
    Refresh = "refresh",
    Toast = "toast",
}

export interface RefreshPolicy {
    /**
     * For sse type: Ably channel name pattern (e.g., '{gameId}:linescore')
     */
    channel?: string;
    /**
     * JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
     */
    dataPath?: string;
    /**
     * For poll type: interval in milliseconds
     */
    intervalMs?: number;
    /**
     * Whether the client should pause this section's refresh when it scrolls out of the
     * viewport. Default true. Set false for critical live sections (e.g., GamePanel scores)
     * that should refresh continuously.
     */
    pauseWhenOffScreen?: boolean;
    type:                RefreshType;
    /**
     * For poll/sse type: URL to poll or connect to. If omitted, polls the SDUI endpoint.
     */
    url?: string;
    [property: string]: any;
}

export enum RefreshType {
    Poll = "poll",
    SSE = "sse",
    Static = "static",
}

export interface Navigation {
    items?: NavigationItem[];
    [property: string]: any;
}

export interface NavigationItem {
    children?:  NavigationItem[];
    icon?:      string;
    id:         string;
    label:      string;
    selected?:  boolean;
    targetUri?: string;
    [property: string]: any;
}

/**
 * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
 *
 * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a
 * thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the
 * screen-level overlays map.
 */
export interface Badge {
    /**
     * Position of the badge within the parent bounds
     */
    alignment?: BadgeAlignment;
    /**
     * The element to render as a badge
     */
    element: AtomicElement;
    [property: string]: any;
}

/**
 * Root node of the atomic element tree — the rendering instructions
 *
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 *
 * The element to render as a badge
 */
export interface AtomicElement {
    /**
     * Server-provided accessibility metadata for this atomic element
     */
    accessibility?: AccessibilityProperties;
    actions?:       Action[];
    alignment?:     Alignment;
    /**
     * Deprecated: use accessibility.label instead. Retained for backward compatibility; clients
     * prefer accessibility.label when present.
     */
    alt?:         string;
    aspectRatio?: number;
    background?:  Background | string;
    /**
     * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
     */
    badge?: Badge;
    /**
     * Responsive breakpoint in dp/px. For Container: below this screen width, direction flips
     * from row to column. Enables responsive layouts without client logic.
     */
    breakpoint?: number;
    children?:   AtomicElement[];
    color?:      string;
    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     */
    columns?:   Column[];
    condition?: string;
    content?:   string;
    /**
     * Per-corner cornerRadius override. When present, takes precedence over the single-value
     * cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also
     * absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and
     * square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to
     * UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner
     * constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius /
     * borderBottomRightRadius.
     */
    cornerRadii?: CornerRadii;
    /**
     * Corner radius in dp/px. Applied to Container (with overflow clip) and Image elements.
     */
    cornerRadius?:   number;
    crossAlignment?: CrossAlignment;
    direction?:      UIDirection;
    disabled?:       boolean;
    falseChild?:     AtomicElement;
    /**
     * When true, the element stretches along its main axis to fill the parent's available
     * width. On Image this pairs with aspectRatio to derive a height (thumbnails in a
     * fixed-width card). On Container it stretches the flex box to parent width regardless of
     * child intrinsic widths. Inline value takes precedence over any variant default; element
     * width/height, when also set, wins over fillWidth.
     */
    fillWidth?: boolean;
    fit?:       ImageFit;
    /**
     * Flex grow factor. When set on a child of a Container, the child claims proportional space
     * along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
     */
    flex?:   number;
    gap?:    number;
    height?: number;
    icon?:   string;
    id?:     string;
    label?:  string;
    /**
     * Outer space between the element and its siblings or parent edges. Applied outside the
     * element's background, border, corner radius, and shadow — use this for sibling-to-sibling
     * spacing instead of Spacer siblings when inhomogeneous gaps are needed.
     */
    margin?:   Spacing;
    maxLines?: number;
    /**
     * Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes
     * (scores, clocks).
     */
    monospacedDigits?: boolean;
    /**
     * Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded
     * states.
     */
    opacity?:     number;
    orientation?: Orientation;
    /**
     * Inner space between the element's own background/border and its content.
     */
    padding?:     Spacing;
    paging?:      boolean;
    placeholder?: string;
    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     */
    rows?: { [key: string]: string }[];
    /**
     * Full section object to render via SectionRouter. Only used when type is SectionSlot.
     */
    section?: Section;
    /**
     * Drop shadow applied to the element. Replaces elevation with richer CSS/SwiftUI shadow
     * semantics.
     */
    shadow?: Shadow;
    /**
     * Whether to show scroll indicators on ScrollContainer. Default false for clean carousel
     * presentation.
     */
    showIndicators?: boolean;
    size?:           number;
    snapAlignment?:  Align;
    src?:            string;
    /**
     * Alternate row background for readability
     */
    striped?: boolean;
    /**
     * Text alignment within the element. Used for centered headings, right-aligned numeric
     * values.
     */
    textAlign?: Align;
    thickness?: number;
    trueChild?: AtomicElement;
    type:       UIType;
    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text,
     * ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image.
     * Renderers parse this string against the primitive's enum and log a diagnostic on
     * unrecognized values.
     */
    variant?: string;
    weight?:  TextWeight;
    width?:   number;
    [property: string]: any;
}

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
export interface Data {
    defaultTab?:  string;
    stateKey?:    string;
    tabContents?: { [key: string]: Section[] };
    tabs?:        TabData[];
    actions?:     Action[];
    awayTeam?:    TeamData;
    /**
     * Badge/chip label, e.g. 'LIVE', 'FEATURED'
     */
    badgeText?: string;
    /**
     * Whether the game clock is actively ticking. When true, renderers should interpolate the
     * clock locally between SSE updates for visual continuity.
     */
    clockRunning?: boolean;
    /**
     * Server-driven visual configuration — controls all layout and styling knobs
     */
    displayConfig?: GamePanelDisplayConfig;
    /**
     * Game clock string (e.g. 'PT05M32.00S' or '5:32')
     */
    gameClock?:      string;
    gameId?:         string;
    gameLeaders?:    GameLeadersData;
    gameStatus?:     number;
    gameStatusText?: string;
    gameTimeEt?:     string;
    homeTeam?:       TeamData;
    /**
     * Current game period (quarter number)
     */
    period?: number;
    /**
     * Semantic card treatment. Missing value is treated as 'standard' at render time.
     */
    variant?: GamePanelVariant;
    /**
     * Secondary label shown above the matchup (e.g. team name, 'Recommended')
     */
    visualLabel?: string;
    /**
     * Ordered list of column definitions; clients render left-to-right
     *
     * Ordered column definitions; clients render left-to-right
     */
    columns?: BoxscoreColumnDefinition[];
    /**
     * Text shown when no player rows are available
     */
    emptyMessage?: string;
    /**
     * Player rows ordered by server (starters first, then bench)
     *
     * Player rows, pre-sorted by the server
     */
    players?: PlayerRow[];
    /**
     * Screen-state key holding the current sort direction (asc/desc)
     */
    sortDirectionStateKey?: string;
    /**
     * Screen-state key holding the current sort column key
     */
    sortStateKey?: string;
    /**
     * Hex colour for team accent
     */
    teamColor?:   string;
    teamLogoUrl?: string;
    teamName?:    string;
    /**
     * Aggregate row shown at the bottom of the table
     */
    teamTotals?: { [key: string]: any };
    /**
     * Three-letter team code, e.g. 'BOS'
     */
    teamTricode?: string;
    fields?:      FormField[];
    /**
     * Layout hint for field arrangement
     */
    layout?: Layout;
    /**
     * Action fired when the form is submitted
     */
    submitAction?: Action;
    submitLabel?:  string;
    /**
     * Ad unit path used by the ad SDK
     */
    adUnitPath?: string;
    /**
     * Whether to collapse the slot when no fill is returned
     */
    collapseOnEmpty?: boolean;
    /**
     * Disclosure label displayed above/below the ad
     */
    label?: string;
    /**
     * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
     * when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
     * width/height. Required so the stub renderer has no client-side chrome defaults.
     */
    placeholder?: Placeholder;
    /**
     * Ad network identifier, e.g. 'gam', 'amazon'
     */
    provider?: string;
    /**
     * Optional auto-refresh interval in seconds
     */
    refreshIntervalSec?: number;
    /**
     * Accepted creative sizes as [width, height] pairs
     */
    sizes?: Array<number[]>;
    /**
     * Key-value targeting hints passed to ad SDK
     */
    targeting?: { [key: string]: string };
    /**
     * Current page (1-based)
     */
    page?: number;
    /**
     * Number of rows per page
     */
    pageSize?: number;
    /**
     * Key of the column the table is currently sorted by
     */
    sortColumn?:    string;
    sortDirection?: SortDirection;
    /**
     * Secondary text, e.g. '2025-26 Regular Season – Per Game'
     */
    subtitle?: string;
    /**
     * Table heading, e.g. 'Season Leaders'
     */
    title?: string;
    /**
     * Total number of rows available server-side (for pagination display)
     */
    totalRows?:  number;
    background?: Background | string;
    ctaAction?:  Action;
    ctaLabel?:   string;
    logoUrl?:    string;
    /**
     * Optional pricing tier highlights
     */
    tiers?: SubscriptionTier[];
    /**
     * Bullet-point feature list
     */
    features?: string[];
    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
     * data-binding support.
     */
    content?: { [key: string]: any };
    /**
     * Root node of the atomic element tree — the rendering instructions
     */
    ui?:       AtomicElement;
    autoplay?: boolean;
    /**
     * Platform capabilities the player should enable. Server includes only capabilities
     * relevant to the requesting platform (via X-Platform header).
     */
    capabilities?: Capability[];
    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId
     * for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     */
    contentId?: string;
    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     */
    playerType?: PlayerType;
    [property: string]: any;
}

/**
 * Full section object to render via SectionRouter. Only used when type is SectionSlot.
 */
export interface Section {
    /**
     * Section-level accessibility metadata (landmark role, live region, heading)
     */
    accessibility?: AccessibilityProperties;
    /**
     * Section-level interaction actions
     */
    actions?:         Action[];
    analyticsId?:     string;
    backgroundColor?: string;
    /**
     * Section-specific data payload
     */
    data?:          Data;
    dataBinding?:   DataBinding;
    display?:       SectionDisplay;
    id:             string;
    layoutHints?:   SectionLayoutHints;
    padding?:       Spacing;
    refreshPolicy?: RefreshPolicy;
    sectionStates?: SectionStates;
    /**
     * Section-level map of translation key to localized string. Used by DataBindingResolver to
     * resolve stringKeys on real-time updates.
     */
    stringTable?: { [key: string]: string };
    /**
     * Nested interaction targets within the section
     */
    subsections?: Subsection[];
    type:         OverlayType;
    [property: string]: any;
}

/**
 * Position of the badge within the parent bounds
 */
export enum BadgeAlignment {
    BottomCenter = "bottomCenter",
    BottomEnd = "bottomEnd",
    BottomStart = "bottomStart",
    Center = "center",
    CenterEnd = "centerEnd",
    CenterStart = "centerStart",
    TopCenter = "topCenter",
    TopEnd = "topEnd",
    TopStart = "topStart",
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
export interface AccessibilityProperties {
    /**
     * Heading level (1-6) for role=heading elements. Maps to aria-level (Web),
     * accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
     */
    headingLevel?: number;
    /**
     * When true, element and its descendants are hidden from the accessibility tree (decorative
     * content).
     */
    hidden?: boolean;
    /**
     * Additional context announced after the label. Maps to accessibilityHint (iOS),
     * contentDescription suffix (Android), aria-describedby text (Web).
     */
    hint?: string;
    /**
     * Human-readable label announced by screen readers. Omit for elements whose text content is
     * self-describing.
     */
    label?: string;
    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
     * aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     */
    liveRegion?: LiveRegion;
    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     */
    role?: Role;
    /**
     * Override default accessibility traversal order. Lower values are visited first. Omit to
     * use natural DOM/view order.
     */
    sortOrder?: number;
    [property: string]: any;
}

/**
 * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
 * aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
 */
export enum LiveRegion {
    Assertive = "assertive",
    Off = "off",
    Polite = "polite",
}

/**
 * Semantic role override. 'none' suppresses the element's intrinsic role.
 */
export enum Role {
    Button = "button",
    Cell = "cell",
    Heading = "heading",
    Image = "image",
    Link = "link",
    List = "list",
    Listitem = "listitem",
    None = "none",
    Row = "row",
    Tab = "tab",
    Table = "table",
    Tabpanel = "tabpanel",
}

export enum Alignment {
    Center = "center",
    End = "end",
    SpaceAround = "spaceAround",
    SpaceBetween = "spaceBetween",
    SpaceEvenly = "spaceEvenly",
    Start = "start",
}

/**
 * Gradient background with ordered color stops
 *
 * Image background with optional scale and overlay
 */
export interface Background {
    /**
     * Ordered list of color stops (hex or semantic token)
     */
    colors?:    string[];
    direction?: Direction;
    /**
     * URL of the background image
     */
    imageUrl?: string;
    /**
     * Optional overlay applied on top of the image
     */
    overlay?:   BackgroundGradient | string;
    scaleType?: ScaleType;
    [property: string]: any;
}

export enum Direction {
    Diagonal = "diagonal",
    Horizontal = "horizontal",
    Vertical = "vertical",
}

/**
 * Gradient background with ordered color stops
 */
export interface BackgroundGradient {
    /**
     * Ordered list of color stops (hex or semantic token)
     */
    colors:     string[];
    direction?: Direction;
    [property: string]: any;
}

export enum ScaleType {
    Contain = "contain",
    Cover = "cover",
    Fill = "fill",
}

export interface Column {
    align?: Align;
    /**
     * Row data key
     */
    key: string;
    /**
     * Header label
     */
    label: string;
    /**
     * Fixed width (integer) or 'flex'
     */
    width?: WidthEnum | number;
    [property: string]: any;
}

/**
 * Text alignment within the element. Used for centered headings, right-aligned numeric
 * values.
 */
export enum Align {
    Center = "center",
    End = "end",
    Start = "start",
}

export enum WidthEnum {
    Flex = "flex",
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
export interface CornerRadii {
    /**
     * Bottom-trailing corner.
     */
    bottomEnd?: number;
    /**
     * Bottom-leading corner.
     */
    bottomStart?: number;
    /**
     * Top-trailing corner (top-right in LTR, top-left in RTL).
     */
    topEnd?: number;
    /**
     * Top-leading corner (top-left in LTR, top-right in RTL).
     */
    topStart?: number;
}

export enum CrossAlignment {
    Center = "center",
    End = "end",
    Start = "start",
    Stretch = "stretch",
}

export enum UIDirection {
    Column = "column",
    Row = "row",
}

export enum ImageFit {
    Contain = "contain",
    Cover = "cover",
    Fill = "fill",
    None = "none",
}

/**
 * Outer space between the element and its siblings or parent edges. Applied outside the
 * element's background, border, corner radius, and shadow — use this for sibling-to-sibling
 * spacing instead of Spacer siblings when inhomogeneous gaps are needed.
 *
 * Inner space between the element's own background/border and its content.
 *
 * Outer margin (space between section and its siblings / screen edge).
 *
 * Inner padding (space between section wrapper and its content).
 */
export interface Spacing {
    bottom?: number;
    end?:    number;
    start?:  number;
    top?:    number;
    [property: string]: any;
}

export enum Orientation {
    Horizontal = "horizontal",
    Vertical = "vertical",
}

/**
 * Drop shadow applied to the element. Replaces elevation with richer CSS/SwiftUI shadow
 * semantics.
 *
 * Drop shadow with CSS/SwiftUI semantics (radius + offset). Compose approximates via
 * elevation.
 *
 * Drop shadow applied to the section wrapper.
 */
export interface Shadow {
    /**
     * Shadow color (hex with alpha, or token reference)
     */
    color?: string;
    /**
     * Horizontal offset in dp/px
     */
    offsetX?: number;
    /**
     * Vertical offset in dp/px
     */
    offsetY?: number;
    /**
     * Blur radius in dp/px
     */
    radius?: number;
    [property: string]: any;
}

export enum UIType {
    Button = "Button",
    Conditional = "Conditional",
    Container = "Container",
    DisplayGrid = "DisplayGrid",
    Divider = "Divider",
    Image = "Image",
    ScrollContainer = "ScrollContainer",
    SectionSlot = "SectionSlot",
    Spacer = "Spacer",
    Text = "Text",
}

/**
 * Font weight tokens for atomic Text elements.
 */
export enum TextWeight {
    Bold = "bold",
    Medium = "medium",
    Regular = "regular",
    SemiBold = "semiBold",
}

export interface TeamData {
    logoUrl?:    string;
    score:       number;
    teamCity:    string;
    teamId:      number;
    teamName:    string;
    teamTricode: string;
    [property: string]: any;
}

export enum Capability {
    Airplay = "airplay",
    BackgroundAudio = "backgroundAudio",
    Chromecast = "chromecast",
    FullscreenRotation = "fullscreenRotation",
    Pip = "pip",
}

/**
 * Defines a single column in the boxscore table
 */
export interface BoxscoreColumnDefinition {
    /**
     * Whether this column should be visually emphasised (e.g., bold)
     */
    highlighted?: boolean;
    /**
     * Property key on each player's stats object that supplies this column's value
     */
    key: string;
    /**
     * Column header text displayed to the user
     */
    label: string;
    /**
     * Whether this column supports client-side sorting
     */
    sortable?: boolean;
    /**
     * Optional hint for column width (e.g. 'auto', '64px', '1fr')
     */
    width?: string;
    [property: string]: any;
}

/**
 * Server-driven visual configuration — controls all layout and styling knobs
 *
 * Server-driven visual configuration for GamePanel — replaces the hardcoded variant branch
 */
export interface GamePanelDisplayConfig {
    /**
     * Default background (pre-game, final)
     */
    background?: Background | string;
    /**
     * Badge/chip background color (hex or token)
     */
    badgeColor?: string;
    /**
     * Fixed card height in dp/px. Null/absent = auto-size
     */
    cardHeight?: number;
    /**
     * Card corner radius in dp/px
     */
    cornerRadius?: number;
    /**
     * Card elevation/shadow in dp/px
     */
    elevation?: number;
    /**
     * Background override when game is LIVE
     */
    liveBackground?: Background | string;
    /**
     * Team logo width/height in dp/px
     */
    logoSize?: number;
    /**
     * Score typography: compact = bodyLarge+Bold, prominent = headlineMedium+ExtraBold
     */
    scoreTextStyle?: ScoreTextStyle;
    aspectRatio?:    string;
    height?:         number;
    [property: string]: any;
}

/**
 * Score typography: compact = bodyLarge+Bold, prominent = headlineMedium+ExtraBold
 */
export enum ScoreTextStyle {
    Compact = "compact",
    Prominent = "prominent",
}

/**
 * One input field inside a form section
 */
export interface FormField {
    disabled?: boolean;
    fieldId:   string;
    /**
     * Input type; clients map to platform-native controls
     */
    fieldType: FieldType;
    label:     string;
    /**
     * For select/radio/checkbox field types: the available choices
     */
    options?:     FormOption[];
    placeholder?: string;
    required?:    boolean;
    /**
     * Screen-state key that holds this field's current value
     */
    stateKey: string;
    /**
     * Message to show when validation fails
     */
    validationMessage?: string;
    /**
     * Optional regex pattern for client-side validation
     */
    validationPattern?: string;
    /**
     * How to realize the control. Applies only when fieldType == 'select'. Missing value is
     * treated as 'dropdown' at render time.
     */
    variant?: SelectVariant;
    [property: string]: any;
}

/**
 * Input type; clients map to platform-native controls
 */
export enum FieldType {
    Checkbox = "checkbox",
    Date = "date",
    Number = "number",
    Radio = "radio",
    Select = "select",
    Text = "text",
    Textarea = "textarea",
    Toggle = "toggle",
}

/**
 * One selectable option within a select/radio/checkbox form field
 */
export interface FormOption {
    label: string;
    value: string;
    [property: string]: any;
}

/**
 * How to realize the control. Applies only when fieldType == 'select'. Missing value is
 * treated as 'dropdown' at render time.
 *
 * How a Form single-select field is realized by the client. 'dropdown' maps to the platform
 * menu (default). 'chips' is a horizontally-scrollable row of tappable capsules.
 * 'segmented' is a platform segmented control. Applies only when FormField.fieldType ==
 * 'select'.
 */
export enum SelectVariant {
    Chips = "chips",
    Dropdown = "dropdown",
    Segmented = "segmented",
}

export interface GameLeadersData {
    awayLeader?: GameLeaderData;
    homeLeader?: GameLeaderData;
    [property: string]: any;
}

export interface GameLeaderData {
    assists?:  number;
    name?:     string;
    points?:   number;
    rebounds?: number;
    [property: string]: any;
}

/**
 * Layout hint for field arrangement
 */
export enum Layout {
    Grid = "grid",
    Horizontal = "horizontal",
    Vertical = "vertical",
}

/**
 * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
 * when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
 * width/height. Required so the stub renderer has no client-side chrome defaults.
 */
export interface Placeholder {
    /**
     * Fill color for the empty rectangle.
     */
    backgroundColor?: string;
    /**
     * Caption rendered inside the empty rectangle (e.g. 'Advertisement').
     */
    text?: string;
    [property: string]: any;
}

/**
 * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
 */
export enum PlayerType {
    Event = "event",
    Game = "game",
    NbaTv = "nbaTv",
    Stream = "stream",
    VOD = "vod",
}

/**
 * One player row inside a boxscore table
 *
 * One ranked player row in a season leaders table
 */
export interface PlayerRow {
    actions?:      Action[];
    imageUrl?:     string;
    jerseyNumber?: string;
    /**
     * Display name (short form, e.g. 'J. Tatum')
     *
     * Display name, e.g. 'Luka Dončić'
     */
    name:      string;
    playerId:  string;
    position?: string;
    /**
     * Whether this player was in the starting lineup
     */
    starter?: boolean;
    /**
     * Stat values keyed by column key (gp, min, pts, reb, ast, etc.)
     */
    stats: { [key: string]: any };
    /**
     * Ranking position (1-based)
     */
    rank?: number;
    /**
     * Team tricode, e.g. 'LAL'
     */
    team?: string;
    [property: string]: any;
}

export enum SortDirection {
    Asc = "asc",
    Desc = "desc",
}

export interface TabData {
    id:          string;
    label:       string;
    stateKey?:   string;
    stateValue?: string;
    [property: string]: any;
}

export interface SubscriptionTier {
    /**
     * Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
     */
    badgeText?: string;
    ctaAction?: Action;
    ctaLabel?:  string;
    features?:  string[];
    id:         string;
    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     */
    name: string;
    /**
     * Strikethrough price if on sale
     */
    originalPrice?: string;
    /**
     * Display price, e.g. '$14.99/mo'
     */
    price: string;
    [property: string]: any;
}

/**
 * Semantic card treatment. Missing value is treated as 'standard' at render time.
 *
 * Semantic treatment of a GamePanel card. Clients resolve natively (widths, padding,
 * emphasis). 'standard' is the default card. 'featured' is a heightened card used as a lead
 * item in a feed or carousel.
 */
export enum GamePanelVariant {
    Featured = "featured",
    Standard = "standard",
}

export interface DataBinding {
    bindings?: DataBindingPath[];
    /**
     * Optional map of targetPath to translation key for client-side i18n resolution on bound
     * fields
     */
    stringKeys?: { [key: string]: string };
    [property: string]: any;
}

export interface DataBindingPath {
    /**
     * JSONPath in incoming message (e.g., '$.homeTeam.score')
     */
    sourcePath: string;
    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     */
    targetPath: string;
    [property: string]: any;
}

/**
 * Outer-chrome spec applied by the client's SectionRouter to every permanent section.
 * Mirrors the inline-chrome vocabulary on AtomicContainer so permanent sections have schema
 * parity with composed sections. Every client's shared SectionContainer wrapper reads these
 * fields; permanent-section renderers do not set outer padding, margin, corner radius,
 * shadow, border, or background themselves.
 */
export interface SectionDisplay {
    /**
     * Section wrapper background (solid, gradient, or image).
     */
    background?: Background | string;
    /**
     * Outer stroke applied around the section wrapper.
     */
    border?: Border;
    /**
     * Corner radius in dp/px applied to the section wrapper (with overflow clip).
     */
    cornerRadius?: number;
    /**
     * Outer margin (space between section and its siblings / screen edge).
     */
    margin?: Spacing;
    /**
     * Inner padding (space between section wrapper and its content).
     */
    padding?: Spacing;
    /**
     * Drop shadow applied to the section wrapper.
     */
    shadow?: Shadow;
    [property: string]: any;
}

/**
 * Outer stroke applied around the section wrapper.
 *
 * Outer stroke applied around a container or section.
 */
export interface Border {
    /**
     * Stroke color (hex or token)
     */
    color?: string;
    /**
     * Stroke width in dp/px
     */
    width?: number;
    [property: string]: any;
}

/**
 * Optional layout hints for section placement. Clients apply best-effort; unknown hints are
 * ignored.
 */
export interface SectionLayoutHints {
    /**
     * Render a divider line above this section
     */
    dividerAbove?: boolean;
    /**
     * Render a divider line below this section
     */
    dividerBelow?: boolean;
    /**
     * Bottom margin in dp/points
     */
    marginBottom?: number;
    /**
     * Top margin in dp/points (0 = flush)
     */
    marginTop?: number;
    /**
     * Rendering priority hint — clients may use for lazy loading or viewport priority
     */
    priority?: Priority;
    [property: string]: any;
}

/**
 * Rendering priority hint — clients may use for lazy loading or viewport priority
 */
export enum Priority {
    High = "high",
    Low = "low",
    Normal = "normal",
}

/**
 * Server-declared loading and error presentation for a section. Clients render these states
 * when applicable.
 */
export interface SectionStates {
    error?:   Error;
    loading?: Loading;
    [property: string]: any;
}

export interface Error {
    /**
     * If true, collapse the section entirely on error instead of showing error UI
     */
    hideOnError?: boolean;
    /**
     * Error message to display (e.g., 'Unable to load scores')
     */
    message?: string;
    /**
     * Optional action to trigger on retry tap (typically a refresh action)
     */
    retryAction?: Action;
    [property: string]: any;
}

export interface Loading {
    /**
     * Minimum height to reserve during loading (prevents layout shift)
     */
    minHeightDp?: number;
    /**
     * Which loading skeleton style to use
     */
    skeleton?: Skeleton;
    [property: string]: any;
}

/**
 * Which loading skeleton style to use
 */
export enum Skeleton {
    None = "none",
    Placeholder = "placeholder",
    Shimmer = "shimmer",
    Spinner = "spinner",
}

/**
 * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
 */
export interface Subsection {
    /**
     * Subsection-level accessibility metadata
     */
    accessibility?: AccessibilityProperties;
    actions?:       Action[];
    id:             string;
    [property: string]: any;
}

export enum OverlayType {
    AdSlot = "AdSlot",
    AtomicComposite = "AtomicComposite",
    BoxscoreTable = "BoxscoreTable",
    Form = "Form",
    GamePanel = "GamePanel",
    SeasonLeadersTable = "SeasonLeadersTable",
    SubscribeBanner = "SubscribeBanner",
    SubscribeHero = "SubscribeHero",
    TabGroup = "TabGroup",
    VideoPlayer = "VideoPlayer",
}
