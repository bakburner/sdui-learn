/**
 * Server-Driven UI schema for NBA Game Detail screens
 */
export interface SduiModels {
    analyticsId?:          string;
    defaultRefreshPolicy?: RefreshPolicy;
    id:                    string;
    navigation?:           Navigation;
    schemaVersion:         string;
    sections:              Section[];
    state?:                { [key: string]: any };
    title?:                string;
    traceId?:              string;
    [property: string]: any;
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

export interface Action {
    /**
     * For analytics actions: event name
     */
    eventName?: string;
    /**
     * For analytics actions: event parameters
     */
    eventParams?: { [key: string]: any };
    /**
     * For navigate actions: web fallback if deeplink fails
     */
    fallbackUrl?: string;
    /**
     * For refresh actions: specific section to refresh (null = full screen)
     */
    sectionId?: string;
    /**
     * For mutate actions: state key to update
     */
    stateKey?: string;
    /**
     * For mutate actions: new value for the state key
     */
    stateValue?: any;
    /**
     * For navigate actions: deeplink URI
     */
    targetUri?: string;
    trigger:    ActionTrigger;
    type:       ActionType;
    [property: string]: any;
}

export enum ActionTrigger {
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
    ContentCard = "ContentCard",
    ContentRail = "ContentRail",
    GameCard = "GameCard",
    PromoBanner = "PromoBanner",
    Row = "Row",
    ScoreboardHeader = "ScoreboardHeader",
    StatLine = "StatLine",
    TabGroup = "TabGroup",
}
