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
 * Optional 'See All' or navigation action
 *
 * Optional action to retry the failed operation
 *
 * Optional action to trigger on retry tap (typically a refresh action)
 */
export interface Action {
    /**
     * For analytics actions: where to send the beacon
     */
    destinations?: Destination[];
    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     */
    endpoint?: string;
    /**
     * For analytics actions: event name
     */
    event?: string;
    /**
     * For analytics actions with onVisible trigger: impression tracking policy
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
     * For mutate actions: operation to perform on the state key
     */
    operation?: MutateOperation;
    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     */
    paramBindings?: { [key: string]: string };
    /**
     * For analytics actions: event parameters
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
 * For analytics actions with onVisible trigger: impression tracking policy
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
    OnSwipe = "onSwipe",
    OnTap = "onTap",
    OnVisible = "onVisible",
}

export enum ActionType {
    Analytics = "analytics",
    Dismiss = "dismiss",
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
    type:        RefreshType;
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
 * Root node of the atomic element tree — the rendering instructions
 *
 * Atomic UI primitive — server-composed building block for the atomic rendering layer
 */
export interface AtomicElement {
    actions?:         Action[];
    alignment?:       Alignment;
    alt?:             string;
    aspectRatio?:     number;
    backgroundColor?: string;
    /**
     * Gradient background for Container elements
     */
    backgroundGradient?: BackgroundGradient;
    buttonVariant?:      ButtonVariant;
    /**
     * Typography variant for data cells
     */
    cellVariant?: TextVariant;
    children?:    AtomicElement[];
    color?:       string;
    /**
     * DisplayGrid column definitions — display-only, non-interactive, server-ordered
     */
    columns?:   Column[];
    condition?: string;
    content?:   string;
    /**
     * Corner radius in dp/px. Applied to Container (with overflow clip) and Image elements.
     */
    cornerRadius?:   number;
    crossAlignment?: CrossAlignment;
    direction?:      UIDirection;
    disabled?:       boolean;
    falseChild?:     AtomicElement;
    fit?:            ImageFit;
    gap?:            number;
    /**
     * Typography variant for header cells
     */
    headerVariant?: TextVariant;
    height?:        number;
    icon?:          string;
    id?:            string;
    label?:         string;
    maxLines?:      number;
    orientation?:   Orientation;
    padding?:       Spacing;
    paging?:        boolean;
    placeholder?:   string;
    /**
     * DisplayGrid row data — each object maps column keys to pre-formatted display values
     */
    rows?: { [key: string]: string }[];
    /**
     * Full section object to render via SectionRouter. Only used when type is SectionSlot.
     */
    section?:       Section;
    size?:          number;
    snapAlignment?: Align;
    src?:           string;
    /**
     * Alternate row background for readability
     */
    striped?:   boolean;
    thickness?: number;
    trueChild?: AtomicElement;
    type:       UIType;
    variant?:   TextVariant;
    weight?:    TextWeight;
    width?:     number;
    [property: string]: any;
}

/**
 * Section-specific data payload
 *
 * Container for a titled list of stat lines
 *
 * Horizontal scrolling strip of content cards
 *
 * Tabbed navigation with dynamic content sections per tab
 *
 * Promotional banner with optional image and call-to-action
 *
 * Responsive row layout that places children side-by-side above a breakpoint width, and
 * stacks vertically below it
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
 * Horizontal scrolling rail of followed teams/players with circular avatars
 *
 * Titled separator/divider between groups of sections, with optional See All action
 *
 * Horizontal scrolling carousel of video thumbnails (landscape 16:9). Mobile shows
 * swipeable cards; web shows a grid with hover-to-play.
 *
 * NBA TV programming schedule with hero promo and time-slot list
 *
 * Inline subscription upsell banner with headline, body copy, and CTA
 *
 * Full-screen subscription upsell hero with multi-tier pricing and feature list
 *
 * Error state displayed when something goes wrong — bad ID, network failure, missing data,
 * etc.
 *
 * Data payload for AtomicComposite sections — ui contains rendering instructions, content
 * carries domain data
 */
export interface Data {
    /**
     * Row layout: 'horizontal' = image | name | stat inline, 'vertical' = name/image stacked
     * above stat value. Use 'vertical' for narrow viewports.
     *
     * Layout hint for field arrangement
     */
    layout?: Layout;
    stats?:  StatLineData[];
    /**
     * Table heading, e.g. 'Season Leaders'
     *
     * Short error headline, e.g. 'Something went wrong'
     */
    title?: string;
    /**
     * Optional 'See All' or navigation action
     */
    action?:       Action;
    contentType?:  ContentType;
    duration?:     string;
    headline?:     string;
    id?:           string;
    subhead?:      string;
    thumbnailUrl?: string;
    cards?:        HeroPanelData[];
    defaultTab?:   string;
    stateKey?:     string;
    tabContents?:  { [key: string]: Section[] };
    tabs?:         TabData[];
    actions?:      Action[];
    description?:  string;
    imageUrl?:     string;
    awayTeam?:     TeamData;
    /**
     * Background image URL for featured variant hero card
     */
    backgroundImageUrl?: string;
    /**
     * Badge/chip label, e.g. 'LIVE', 'FEATURED'
     */
    badgeText?: string;
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
     * Visual treatment: 'standard' for compact feed cards, 'featured' for hero-sized cards with
     * gradient/background, 'scoreboard' for compact scoreboard rows
     */
    variant?: Variant;
    /**
     * Secondary label shown above the matchup (e.g. team name, 'Recommended')
     */
    visualLabel?: string;
    /**
     * Screen width (dp) below which children stack vertically
     */
    breakpoint?: number;
    /**
     * Child sections rendered in a row (or column when collapsed)
     */
    children?: Section[];
    /**
     * Gap between children in dp/px
     */
    spacing?: number;
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
     * Total number of rows available server-side (for pagination display)
     */
    totalRows?: number;
    items?:     FollowingRailItem[];
    /**
     * Hero image for the currently airing program
     */
    heroImageUrl?: string;
    heroSubtitle?: string;
    heroTitle?:    string;
    liveNow?:      boolean;
    slots?:        NbaTvSlot[];
    ctaAction?:    Action;
    ctaLabel?:     string;
    logoUrl?:      string;
    /**
     * Optional pricing tier highlights
     */
    tiers?: SubscriptionTier[];
    /**
     * Bullet-point feature list
     */
    features?: string[];
    /**
     * Optional icon name, e.g. 'error', 'wifi_off'
     */
    icon?: string;
    /**
     * Longer explanatory text
     */
    message?: string;
    /**
     * Optional action to retry the failed operation
     */
    retryAction?: Action;
    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
     * data-binding support.
     */
    content?: { [key: string]: any };
    /**
     * Root node of the atomic element tree — the rendering instructions
     */
    ui?: AtomicElement;
    [property: string]: any;
}

/**
 * Full section object to render via SectionRouter. Only used when type is SectionSlot.
 */
export interface Section {
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
    dataBindings?:  DataBinding;
    id:             string;
    layoutHints?:   SectionLayoutHints;
    padding?:       Spacing;
    refreshPolicy?: RefreshPolicy;
    sectionStates?: SectionStates;
    /**
     * Nested interaction targets within the section
     */
    subsections?: Subsection[];
    type:         SectionType;
    [property: string]: any;
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
 * Gradient background for Container elements
 */
export interface BackgroundGradient {
    /**
     * Ordered list of color stops (hex or semantic token)
     */
    colors:     string[];
    direction?: Direction;
    [property: string]: any;
}

export enum Direction {
    Diagonal = "diagonal",
    Horizontal = "horizontal",
    Vertical = "vertical",
}

export enum ButtonVariant {
    Primary = "primary",
    Secondary = "secondary",
    Tertiary = "tertiary",
    Text = "text",
}

/**
 * Typography variant for data cells
 *
 * Typography variant for header cells
 */
export enum TextVariant {
    Body = "body",
    BodySmall = "bodySmall",
    Caption = "caption",
    Heading1 = "heading1",
    Heading2 = "heading2",
    Heading3 = "heading3",
    Label = "label",
    Score = "score",
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

export enum Align {
    Center = "center",
    End = "end",
    Start = "start",
}

export enum WidthEnum {
    Flex = "flex",
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

export enum Orientation {
    Horizontal = "horizontal",
    Vertical = "vertical",
}

export interface Spacing {
    bottom?: number;
    end?:    number;
    start?:  number;
    top?:    number;
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

export enum TextWeight {
    Bold = "bold",
    Medium = "medium",
    Regular = "regular",
    Semibold = "semibold",
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

export interface HeroPanelData {
    action?:       Action;
    contentType?:  ContentType;
    duration?:     string;
    headline:      string;
    id:            string;
    subhead?:      string;
    thumbnailUrl?: string;
    [property: string]: any;
}

export enum ContentType {
    Article = "article",
    Gallery = "gallery",
    Interactive = "interactive",
    Video = "video",
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
 * One entity (team or player) in a following rail
 */
export interface FollowingRailItem {
    action?: Action;
    /**
     * Whether this item represents a team or a player
     */
    entityType?: EntityType;
    id:          string;
    /**
     * Avatar / logo URL
     */
    imageUrl?: string;
    /**
     * Display name, e.g. 'Lakers' or 'LeBron James'
     */
    name?: string;
    /**
     * Overlay badge, e.g. 'LIVE', 'NEW'
     */
    badgeText?: string;
    /**
     * Human-readable duration, e.g. '2:34'
     */
    duration?:     string;
    subtitle?:     string;
    thumbnailUrl?: string;
    title?:        string;
    [property: string]: any;
}

/**
 * Whether this item represents a team or a player
 */
export enum EntityType {
    Player = "player",
    Team = "team",
}

/**
 * Row layout: 'horizontal' = image | name | stat inline, 'vertical' = name/image stacked
 * above stat value. Use 'vertical' for narrow viewports.
 *
 * Layout hint for field arrangement
 */
export enum Layout {
    Grid = "grid",
    Horizontal = "horizontal",
    Vertical = "vertical",
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

export interface NbaTvSlot {
    action?: Action;
    /**
     * ISO-8601 end time
     */
    endTime?: string;
    id:       string;
    isLive?:  boolean;
    /**
     * ISO-8601 start time
     */
    startTime:     string;
    subtitle?:     string;
    thumbnailUrl?: string;
    title:         string;
    [property: string]: any;
}

export enum SortDirection {
    Asc = "asc",
    Desc = "desc",
}

export interface StatLineData {
    playerId:        number;
    playerImageUrl?: string;
    playerName:      string;
    statCategory:    string;
    statLabel?:      string;
    statValue:       string;
    teamTricode?:    string;
    [property: string]: any;
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
 * Visual treatment: 'standard' for compact feed cards, 'featured' for hero-sized cards with
 * gradient/background, 'scoreboard' for compact scoreboard rows
 */
export enum Variant {
    Featured = "featured",
    Scoreboard = "scoreboard",
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
    actions?: Action[];
    id:       string;
    [property: string]: any;
}

export enum SectionType {
    AdSlot = "AdSlot",
    AtomicComposite = "AtomicComposite",
    BoxscoreTable = "BoxscoreTable",
    ContentRail = "ContentRail",
    ErrorState = "ErrorState",
    FollowingRail = "FollowingRail",
    Form = "Form",
    GamePanel = "GamePanel",
    HeroPanel = "HeroPanel",
    NbaTvSchedule = "NbaTvSchedule",
    PromoBanner = "PromoBanner",
    Row = "Row",
    SeasonLeadersTable = "SeasonLeadersTable",
    SectionHeader = "SectionHeader",
    StatLine = "StatLine",
    SubscribeBanner = "SubscribeBanner",
    SubscribeHero = "SubscribeHero",
    TabGroup = "TabGroup",
    VideoCarousel = "VideoCarousel",
}
