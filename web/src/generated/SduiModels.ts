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
    /**
     * Server-exposed A/B / experiment variants available for this screen. Clients read
     * `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
     * to the server on subsequent requests. Omit for screens without active experiments.
     */
    variants?: ExperimentVariants;
    [property: string]: any;
}

/**
 * Action fired when the form is submitted
 *
 * Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
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

/**
 * Event that fires the action. Prefer onActivate for primary activation (tap, keyboard
 * Enter/Space, accessibility activate). onTap is a deprecated alias for onActivate.
 */
export enum ActionTrigger {
    OnActivate = "onActivate",
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
     * viewport. Default true. Set false for critical live sections (e.g., live-score panels)
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
 * One server-composed overlay layer positioned over an OverlayContainer base element.
 */
export interface AtomicOverlay {
    /**
     * Position of the overlay within the base element bounds.
     */
    alignment?: BadgeAlignment;
    /**
     * Atomic element to render in this overlay layer.
     */
    element: AtomicElement;
    /**
     * Optional edge offsets from the aligned base bounds.
     */
    inset?: Spacing;
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
 * Atomic tree describing the banner's full visible surface. Renderer walks this tree
 * exactly as an AtomicComposite would; no client-side chrome defaults are permitted. See
 * AGENTS.md §15.1.
 *
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 *
 * The element to render as a badge
 *
 * OverlayContainer base element. Rendered first and sized by its own atomic box model.
 *
 * Atomic element to render in this overlay layer.
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
    alt?: string;
    /**
     * Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
     */
    aspectRatio?: number | AspectRatioEnum;
    background?:  Background | string;
    /**
     * Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
     */
    badge?: Badge;
    /**
     * OverlayContainer base element. Rendered first and sized by its own atomic box model.
     */
    base?: AtomicElement;
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
    bindRef?: string;
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
     * Corner radius: dp/px or layout token. Applied to Container (with overflow clip) and Image
     * elements.
     */
    cornerRadius?:   number | string;
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
    flex?: number;
    /**
     * LiveClock display format. Clients realize using their platform's tabular-numerals
     * typography (equivalent to TextVariant.score).
     */
    format?: Format;
    /**
     * Gap between flex children (row/column), or grid gap where applicable.
     */
    gap?: number | string;
    /**
     * Fixed height in dp/px or layout token.
     */
    height?: number | string;
    icon?:   string;
    id?:     string;
    /**
     * LiveClock: whether the clock is actively ticking. When true, clients run a local tick
     * loop at their platform-native refresh cadence (~10Hz) and update the displayed value.
     * When false, clients render snapshotSeconds verbatim.
     */
    isRunning?: boolean;
    label?:     string;
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
     * OverlayContainer layers rendered over the base element in server order.
     */
    overlays?: AtomicOverlay[];
    /**
     * Inner space between the element's own background/border and its content.
     */
    padding?: Spacing;
    /**
     * Optional ScrollContainer page indicator. Clients render it only when declared.
     */
    pageIndicator?: PageIndicator;
    paging?:        boolean;
    placeholder?:   string;
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
    /**
     * LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients
     * compute elapsed = now - snapshotAt and derive the displayed value. Required when type ==
     * 'LiveClock'.
     */
    snapshotAt?: Date;
    /**
     * LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients
     * interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
     */
    snapshotSeconds?: number;
    src?:             string;
    /**
     * LiveClock: optional clamp. For direction 'down', clock holds at this value once reached.
     * For direction 'up', clock holds once reached. Omit to disable the clamp.
     */
    stopAtSeconds?: number;
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
    /**
     * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
     * (default 0); 'up' increments from snapshotSeconds with no upper bound unless
     * stopAtSeconds is set.
     */
    tickDirection?: TickDirection;
    trueChild?:     AtomicElement;
    type:           UIType;
    /**
     * Named variant preset. The vocabulary depends on the element's type: TextVariant for Text,
     * ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image.
     * Renderers parse this string against the primitive's enum and log a diagnostic on
     * unrecognized values.
     */
    variant?: string;
    weight?:  TextWeight;
    /**
     * Fixed width in dp/px or layout token.
     */
    width?: number | string;
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
export interface Data {
    defaultTab?:  string;
    stateKey?:    string;
    tabContents?: { [key: string]: Section[] };
    tabs?:        TabData[];
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
    totalRows?: number;
    /**
     * Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
     */
    ctaAction?: Action;
    /**
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
     * not used by the renderer, which reads the visible price copy out of `ui`.
     *
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
     * not used by the renderer.
     */
    tiers?: SubscriptionTier[];
    /**
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
    ui?: AtomicElement;
    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
     * data-binding support.
     */
    content?:  { [key: string]: any };
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
    contentId?:     string;
    displayConfig?: DisplayConfig;
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
    surface?:     SectionSurface;
    type:         OverlayType;
    [property: string]: any;
}

/**
 * Position of the badge within the parent bounds
 *
 * Position of the overlay within the base element bounds.
 *
 * Position of the indicator within the ScrollContainer bounds.
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
export interface Spacing {
    bottom?: number | string;
    end?:    number | string;
    start?:  number | string;
    top?:    number | string;
    [property: string]: any;
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

export enum AspectRatioEnum {
    The11 = "1:1",
    The169 = "16:9",
    The219 = "21:9",
    The32 = "3:2",
    The43 = "4:3",
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
    bottomEnd?: number | string;
    /**
     * Bottom-leading corner.
     */
    bottomStart?: number | string;
    /**
     * Top-trailing corner (top-right in LTR, top-left in RTL).
     */
    topEnd?: number | string;
    /**
     * Top-leading corner (top-left in LTR, top-right in RTL).
     */
    topStart?: number | string;
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
 * LiveClock display format. Clients realize using their platform's tabular-numerals
 * typography (equivalent to TextVariant.score).
 */
export enum Format {
    HMmSs = "h:mm:ss",
    MSs = "m:ss",
    MmSs = "mm:ss",
}

export enum Orientation {
    Horizontal = "horizontal",
    Vertical = "vertical",
}

/**
 * Optional ScrollContainer page indicator. Clients render it only when declared.
 *
 * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local
 * scroll state only to realize the declared affordance.
 */
export interface PageIndicator {
    /**
     * Active indicator color.
     */
    activeColor?: string;
    /**
     * Position of the indicator within the ScrollContainer bounds.
     */
    alignment: BadgeAlignment;
    /**
     * Inactive indicator color.
     */
    color?: string;
    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
     * bar segments.
     */
    style: Style;
    [property: string]: any;
}

/**
 * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
 * bar segments.
 */
export enum Style {
    Dashes = "dashes",
    Dots = "dots",
}

/**
 * Drop shadow applied to the element. Replaces elevation with richer CSS/SwiftUI shadow
 * semantics.
 *
 * Drop shadow with CSS/SwiftUI semantics (radius + offset). Compose approximates via
 * elevation.
 *
 * Drop shadow applied to the surface.
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

/**
 * LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
 * (default 0); 'up' increments from snapshotSeconds with no upper bound unless
 * stopAtSeconds is set.
 */
export enum TickDirection {
    Down = "down",
    Up = "up",
}

export enum UIType {
    Button = "Button",
    Conditional = "Conditional",
    Container = "Container",
    DisplayGrid = "DisplayGrid",
    Divider = "Divider",
    Image = "Image",
    LiveClock = "LiveClock",
    OverlayContainer = "OverlayContainer",
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

export interface DisplayConfig {
    aspectRatio?: string;
    height?:      number;
    [property: string]: any;
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
 * menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies
 * only when FormField.fieldType == 'select'.
 */
export enum SelectVariant {
    Chips = "chips",
    Dropdown = "dropdown",
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
    /**
     * Optional server-declared transform applied by shared client binding infrastructure before
     * writing the target value. liveClockSnapshot normalizes clock payload values into {
     * snapshotSeconds, snapshotAt, isRunning }.
     */
    transform?: Transform;
    [property: string]: any;
}

/**
 * Optional server-declared transform applied by shared client binding infrastructure before
 * writing the target value. liveClockSnapshot normalizes clock payload values into {
 * snapshotSeconds, snapshotAt, isRunning }.
 */
export enum Transform {
    LiveClockSnapshot = "liveClockSnapshot",
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
    /**
     * Server-declared error presentation for this section.
     */
    error?:   ErrorState;
    loading?: Loading;
    [property: string]: any;
}

/**
 * Server-declared error presentation for this section.
 *
 * Server-declared error-state shape rendered by section error boundaries. Named
 * `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's
 * native error protocol (e.g. `Swift.Error`).
 */
export interface ErrorState {
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
    /**
     * Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a
     * neutral default when omitted.
     */
    retryLabel?: string;
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

/**
 * Server-driven surface spec applied by the client's SectionRouter to every semantic
 * section — the visual wrapper beneath the section's content. Mirrors the inline-chrome
 * vocabulary on AtomicContainer so semantic sections have schema parity with composed
 * sections. Every client's shared SectionContainer wrapper reads these fields;
 * semantic-section renderers do not set outer padding, margin, corner radius, shadow,
 * border, or background themselves. The sibling `data` field carries content (including the
 * atomic UI tree); `surface` carries the frame that sits beneath it.
 */
export interface SectionSurface {
    /**
     * Surface background (solid, gradient, or image).
     */
    background?: Background | string;
    /**
     * Outer stroke applied around the surface.
     */
    border?: Border;
    /**
     * Corner radius: dp/px or layout token, applied to the surface (with overflow clip).
     */
    cornerRadius?: number | string;
    /**
     * Outer margin (space between the surface and its siblings / screen edge).
     */
    margin?: Spacing;
    /**
     * Inner padding (space between the surface edge and the content it wraps).
     */
    padding?: Spacing;
    /**
     * Drop shadow applied to the surface.
     */
    shadow?: Shadow;
    [property: string]: any;
}

/**
 * Outer stroke applied around the surface.
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

export enum OverlayType {
    AdSlot = "AdSlot",
    AtomicComposite = "AtomicComposite",
    BoxscoreTable = "BoxscoreTable",
    Form = "Form",
    SeasonLeadersTable = "SeasonLeadersTable",
    SubscribeBanner = "SubscribeBanner",
    SubscribeHero = "SubscribeHero",
    TabGroup = "TabGroup",
    VideoPlayer = "VideoPlayer",
}

/**
 * Server-exposed A/B / experiment variants available for this screen. Clients read
 * `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
 * to the server on subsequent requests. Omit for screens without active experiments.
 *
 * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets
 * clients expose variant selection without hardcoding experiment ids or option vocabularies.
 */
export interface ExperimentVariants {
    /**
     * Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key
     * back to the server as part of the experiments map on subsequent requests.
     */
    experimentId: string;
    /**
     * Ordered list of variants the client may choose from.
     */
    options: ExperimentVariantOption[];
    [property: string]: any;
}

/**
 * One variant within an experiment.
 */
export interface ExperimentVariantOption {
    /**
     * Optional longer description shown alongside the label.
     */
    description?: string;
    /**
     * Variant identifier (e.g. `A`, `B`). Opaque to clients.
     */
    id: string;
    /**
     * Human-readable label rendered in variant pickers.
     */
    label: string;
    [property: string]: any;
}
