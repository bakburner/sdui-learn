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
    schemaVersion:         string;
    sections:              Section[];
    state?:                { [key: string]: any };
    title?:                string;
    traceId?:              string;
    parentUri?:            string;
    [property: string]: any;
}

/**
 * Action fired when the form is submitted
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
 */
export interface Data {
    awayTeam?:       TeamData;
    clock?:          string;
    gameStatus?:     number;
    gameStatusText?: string;
    homeTeam?:       TeamData;
    period?:         number;
    stats?:          StatLineData[];
    title?:          string;
    action?:         Action;
    contentType?:    ContentType;
    duration?:       string;
    headline?:       string;
    id?:             string;
    subhead?:        string;
    thumbnailUrl?:   string;
    cards?:          ContentCardData[];
    defaultTab?:     string;
    stateKey?:       string;
    tabContents?:    { [key: string]: Section[] };
    tabs?:           TabData[];
    actions?:        Action[];
    description?:    string;
    imageUrl?:       string;
    gameId?:         string;
    gameLeaders?:    GameLeadersData;
    gameTimeEt?:     string;
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
     */
    columns?: BoxscoreColumnDefinition[];
    /**
     * Text shown when no player rows are available
     */
    emptyMessage?: string;
    /**
     * Player rows ordered by server (starters first, then bench)
     */
    players?: BoxscorePlayerRow[];
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
    [property: string]: any;
}

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
    padding?:       Spacing;
    refreshPolicy?: RefreshPolicy;
    /**
     * Nested interaction targets within the section
     */
    subsections?: Subsection[];
    type:         Type;
    [property: string]: any;
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

export interface ContentCardData {
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
 * Layout hint for field arrangement
 */
export enum Layout {
    Grid = "grid",
    Horizontal = "horizontal",
    Vertical = "vertical",
}

/**
 * One player row inside a boxscore table
 */
export interface BoxscorePlayerRow {
    actions?:      Action[];
    imageUrl?:     string;
    jerseyNumber?: string;
    /**
     * Display name (short form, e.g. 'J. Tatum')
     */
    name:      string;
    playerId:  string;
    position?: string;
    /**
     * Whether this player was in the starting lineup
     */
    starter?: boolean;
    stats:    { [key: string]: any };
    [property: string]: any;
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

export interface Spacing {
    bottom?: number;
    end?:    number;
    start?:  number;
    top?:    number;
    [property: string]: any;
}

/**
 * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
 */
export interface Subsection {
    actions?: Action[];
    id:       string;
    [property: string]: any;
}

export enum Type {
    BoxscoreTable = "BoxscoreTable",
    ContentCard = "ContentCard",
    ContentRail = "ContentRail",
    Form = "Form",
    GameCard = "GameCard",
    PromoBanner = "PromoBanner",
    Row = "Row",
    ScoreboardHeader = "ScoreboardHeader",
    StatLine = "StatLine",
    TabGroup = "TabGroup",
}
