
package com.nba.sdui.models.generated;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;


/**
 * SDUI All Types
 * <p>
 * Wrapper schema that references all SDUI types for complete codegen
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "screen",
    "section",
    "action",
    "atomicElement",
    "atomicComposite",
    "refreshPolicy",
    "dataBinding",
    "dataBindingPath",
    "spacing",
    "state",
    "textVariant",
    "textWeight",
    "actionType",
    "actionTrigger",
    "refreshType",
    "direction",
    "alignment",
    "crossAlignment",
    "imageFit",
    "tabData",
    "navigation",
    "navigationItem",
    "tabGroup",
    "subsection",
    "subscribeUpsell",
    "subscriptionTier",
    "boxscoreTable",
    "calendarStrip",
    "calendarMonthList",
    "form",
    "adSlot",
    "seasonLeadersTable",
    "videoPlayer"
})
@Generated("jsonschema2pojo")
public class SDUIAllTypes {

    @JsonProperty("screen")
    @Valid
    private Screen screen;
    @JsonProperty("section")
    @Valid
    private Section section;
    @JsonProperty("action")
    @Valid
    private Action action;
    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("atomicElement")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement atomicElement;
    /**
     * Component payload for AtomicComposite sections — ui contains rendering instructions, content carries domain data
     * 
     */
    @JsonProperty("atomicComposite")
    @JsonPropertyDescription("Component payload for AtomicComposite sections \u2014 ui contains rendering instructions, content carries domain data")
    @Valid
    private AtomicComposite atomicComposite;
    @JsonProperty("refreshPolicy")
    @Valid
    private RefreshPolicy refreshPolicy;
    @JsonProperty("dataBinding")
    @Valid
    private DataBinding dataBinding;
    @JsonProperty("dataBindingPath")
    @Valid
    private DataBindingPath dataBindingPath;
    @JsonProperty("spacing")
    @Valid
    private Spacing spacing;
    /**
     * Screen-level state map for interactive patterns
     * 
     */
    @JsonProperty("state")
    @JsonPropertyDescription("Screen-level state map for interactive patterns")
    @Valid
    private State state;
    /**
     * Material3 typography scale plus the NBA-specific `score` variant (monospaced digits for live score / clock rendering).
     * 
     */
    @JsonProperty("textVariant")
    @JsonPropertyDescription("Material3 typography scale plus the NBA-specific `score` variant (monospaced digits for live score / clock rendering).")
    private SDUIAllTypes.TextVariant textVariant;
    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("textWeight")
    @JsonPropertyDescription("Font weight tokens for atomic Text elements.")
    private AtomicElement.TextWeight textWeight;
    @JsonProperty("actionType")
    private Action.ActionType actionType;
    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * 
     */
    @JsonProperty("actionTrigger")
    @JsonPropertyDescription("Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.")
    private Action.ActionTrigger actionTrigger;
    @JsonProperty("refreshType")
    private RefreshPolicy.RefreshType refreshType;
    @JsonProperty("direction")
    private AtomicElement.Direction direction;
    @JsonProperty("alignment")
    private AtomicElement.Alignment alignment;
    @JsonProperty("crossAlignment")
    private AtomicElement.CrossAlignment crossAlignment;
    @JsonProperty("imageFit")
    private AtomicElement.ImageFit imageFit;
    @JsonProperty("tabData")
    @Valid
    private TabData tabData;
    @JsonProperty("navigation")
    @Valid
    private Navigation navigation;
    @JsonProperty("navigationItem")
    @Valid
    private NavigationItem navigationItem;
    /**
     * Tabbed navigation with dynamic content sections per tab. Optional ui is the tab header/control row only; tabContents hosts nested sections. Tab selection uses section.subsections mutate actions.
     * 
     */
    @JsonProperty("tabGroup")
    @JsonPropertyDescription("Tabbed navigation with dynamic content sections per tab. Optional ui is the tab header/control row only; tabContents hosts nested sections. Tab selection uses section.subsections mutate actions.")
    @Valid
    private TabGroup tabGroup;
    /**
     * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
     * 
     */
    @JsonProperty("subsection")
    @JsonPropertyDescription("Nested interaction target within a section (e.g., tappable team area inside a scoreboard)")
    @Valid
    private Subsection subsection;
    /**
     * Subscription upsell. Reserved SDK integration point: the upsell's visible chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP product identifiers the SDK will later bind to; `ctaAction` is the optional pre-SDK fallback action. Layout intent (inline banner vs. full-screen hero) is expressed by the inner `ui` atomic tree and the section's outer `surface`, not by component identity.
     * 
     */
    @JsonProperty("subscribeUpsell")
    @JsonPropertyDescription("Subscription upsell. Reserved SDK integration point: the upsell's visible chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP product identifiers the SDK will later bind to; `ctaAction` is the optional pre-SDK fallback action. Layout intent (inline banner vs. full-screen hero) is expressed by the inner `ui` atomic tree and the section's outer `surface`, not by component identity.")
    @Valid
    private SubscribeUpsell subscribeUpsell;
    @JsonProperty("subscriptionTier")
    @Valid
    private SubscriptionTier subscriptionTier;
    /**
     * Typed tabular data for an NBA-style boxscore (one per team)
     * 
     */
    @JsonProperty("boxscoreTable")
    @JsonPropertyDescription("Typed tabular data for an NBA-style boxscore (one per team)")
    @Valid
    private BoxscoreTable boxscoreTable;
    /**
     * Platform-native horizontal date picker. All ISO date fields are ET-anchored (America/New_York) calendar days (YYYY-MM-DD). defaultDate is the league's current anchor/focus date — typically today in ET during the regular season, but may be a future date during offseason or breaks (e.g. the regular-season opener). Clients display defaultDate as-is and never compare it to device time.
     * 
     */
    @JsonProperty("calendarStrip")
    @JsonPropertyDescription("Platform-native horizontal date picker. All ISO date fields are ET-anchored (America/New_York) calendar days (YYYY-MM-DD). defaultDate is the league's current anchor/focus date \u2014 typically today in ET during the regular season, but may be a future date during offseason or breaks (e.g. the regular-season opener). Clients display defaultDate as-is and never compare it to device time.")
    @Valid
    private CalendarStrip calendarStrip;
    /**
     * Vertically-scrollable month-grid calendar with per-date game metadata. All date fields are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current anchor date; drives the 'today' visual highlight and the initial scroll position.
     * 
     */
    @JsonProperty("calendarMonthList")
    @JsonPropertyDescription("Vertically-scrollable month-grid calendar with per-date game metadata. All date fields are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current anchor date; drives the 'today' visual highlight and the initial scroll position.")
    @Valid
    private CalendarMonthList calendarMonthList;
    /**
     * Server-driven form section with typed fields bound to screen state
     * 
     */
    @JsonProperty("form")
    @JsonPropertyDescription("Server-driven form section with typed fields bound to screen state")
    @Valid
    private Form form;
    /**
     * Ad placement primitive — carries placement semantics while delegating auction/targeting to ad-platform SDKs (see ADR-007)
     * 
     */
    @JsonProperty("adSlot")
    @JsonPropertyDescription("Ad placement primitive \u2014 carries placement semantics while delegating auction/targeting to ad-platform SDKs (see ADR-007)")
    @Valid
    private AdSlot adSlot;
    /**
     * Sortable, paginated table of season statistical leaders (league-wide)
     * 
     */
    @JsonProperty("seasonLeadersTable")
    @JsonPropertyDescription("Sortable, paginated table of season statistical leaders (league-wide)")
    @Valid
    private SeasonLeadersTable seasonLeadersTable;
    /**
     * Video player section — reserved SDK integration point for DRM / HLS / ad insertion. `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders before the SDK is integrated and will serve as the loading/error placeholder afterwards.
     * 
     */
    @JsonProperty("videoPlayer")
    @JsonPropertyDescription("Video player section \u2014 reserved SDK integration point for DRM / HLS / ad insertion. `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders before the SDK is integrated and will serve as the loading/error placeholder afterwards.")
    @Valid
    private VideoPlayer videoPlayer;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("screen")
    public Screen getScreen() {
        return screen;
    }

    @JsonProperty("screen")
    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public SDUIAllTypes withScreen(Screen screen) {
        this.screen = screen;
        return this;
    }

    @JsonProperty("section")
    public Section getSection() {
        return section;
    }

    @JsonProperty("section")
    public void setSection(Section section) {
        this.section = section;
    }

    public SDUIAllTypes withSection(Section section) {
        this.section = section;
        return this;
    }

    @JsonProperty("action")
    public Action getAction() {
        return action;
    }

    @JsonProperty("action")
    public void setAction(Action action) {
        this.action = action;
    }

    public SDUIAllTypes withAction(Action action) {
        this.action = action;
        return this;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("atomicElement")
    public AtomicElement getAtomicElement() {
        return atomicElement;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("atomicElement")
    public void setAtomicElement(AtomicElement atomicElement) {
        this.atomicElement = atomicElement;
    }

    public SDUIAllTypes withAtomicElement(AtomicElement atomicElement) {
        this.atomicElement = atomicElement;
        return this;
    }

    /**
     * Component payload for AtomicComposite sections — ui contains rendering instructions, content carries domain data
     * 
     */
    @JsonProperty("atomicComposite")
    public AtomicComposite getAtomicComposite() {
        return atomicComposite;
    }

    /**
     * Component payload for AtomicComposite sections — ui contains rendering instructions, content carries domain data
     * 
     */
    @JsonProperty("atomicComposite")
    public void setAtomicComposite(AtomicComposite atomicComposite) {
        this.atomicComposite = atomicComposite;
    }

    public SDUIAllTypes withAtomicComposite(AtomicComposite atomicComposite) {
        this.atomicComposite = atomicComposite;
        return this;
    }

    @JsonProperty("refreshPolicy")
    public RefreshPolicy getRefreshPolicy() {
        return refreshPolicy;
    }

    @JsonProperty("refreshPolicy")
    public void setRefreshPolicy(RefreshPolicy refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
    }

    public SDUIAllTypes withRefreshPolicy(RefreshPolicy refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
        return this;
    }

    @JsonProperty("dataBinding")
    public DataBinding getDataBinding() {
        return dataBinding;
    }

    @JsonProperty("dataBinding")
    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    public SDUIAllTypes withDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
        return this;
    }

    @JsonProperty("dataBindingPath")
    public DataBindingPath getDataBindingPath() {
        return dataBindingPath;
    }

    @JsonProperty("dataBindingPath")
    public void setDataBindingPath(DataBindingPath dataBindingPath) {
        this.dataBindingPath = dataBindingPath;
    }

    public SDUIAllTypes withDataBindingPath(DataBindingPath dataBindingPath) {
        this.dataBindingPath = dataBindingPath;
        return this;
    }

    @JsonProperty("spacing")
    public Spacing getSpacing() {
        return spacing;
    }

    @JsonProperty("spacing")
    public void setSpacing(Spacing spacing) {
        this.spacing = spacing;
    }

    public SDUIAllTypes withSpacing(Spacing spacing) {
        this.spacing = spacing;
        return this;
    }

    /**
     * Screen-level state map for interactive patterns
     * 
     */
    @JsonProperty("state")
    public State getState() {
        return state;
    }

    /**
     * Screen-level state map for interactive patterns
     * 
     */
    @JsonProperty("state")
    public void setState(State state) {
        this.state = state;
    }

    public SDUIAllTypes withState(State state) {
        this.state = state;
        return this;
    }

    /**
     * Material3 typography scale plus the NBA-specific `score` variant (monospaced digits for live score / clock rendering).
     * 
     */
    @JsonProperty("textVariant")
    public SDUIAllTypes.TextVariant getTextVariant() {
        return textVariant;
    }

    /**
     * Material3 typography scale plus the NBA-specific `score` variant (monospaced digits for live score / clock rendering).
     * 
     */
    @JsonProperty("textVariant")
    public void setTextVariant(SDUIAllTypes.TextVariant textVariant) {
        this.textVariant = textVariant;
    }

    public SDUIAllTypes withTextVariant(SDUIAllTypes.TextVariant textVariant) {
        this.textVariant = textVariant;
        return this;
    }

    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("textWeight")
    public AtomicElement.TextWeight getTextWeight() {
        return textWeight;
    }

    /**
     * Font weight tokens for atomic Text elements.
     * 
     */
    @JsonProperty("textWeight")
    public void setTextWeight(AtomicElement.TextWeight textWeight) {
        this.textWeight = textWeight;
    }

    public SDUIAllTypes withTextWeight(AtomicElement.TextWeight textWeight) {
        this.textWeight = textWeight;
        return this;
    }

    @JsonProperty("actionType")
    public Action.ActionType getActionType() {
        return actionType;
    }

    @JsonProperty("actionType")
    public void setActionType(Action.ActionType actionType) {
        this.actionType = actionType;
    }

    public SDUIAllTypes withActionType(Action.ActionType actionType) {
        this.actionType = actionType;
        return this;
    }

    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * 
     */
    @JsonProperty("actionTrigger")
    public Action.ActionTrigger getActionTrigger() {
        return actionTrigger;
    }

    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * 
     */
    @JsonProperty("actionTrigger")
    public void setActionTrigger(Action.ActionTrigger actionTrigger) {
        this.actionTrigger = actionTrigger;
    }

    public SDUIAllTypes withActionTrigger(Action.ActionTrigger actionTrigger) {
        this.actionTrigger = actionTrigger;
        return this;
    }

    @JsonProperty("refreshType")
    public RefreshPolicy.RefreshType getRefreshType() {
        return refreshType;
    }

    @JsonProperty("refreshType")
    public void setRefreshType(RefreshPolicy.RefreshType refreshType) {
        this.refreshType = refreshType;
    }

    public SDUIAllTypes withRefreshType(RefreshPolicy.RefreshType refreshType) {
        this.refreshType = refreshType;
        return this;
    }

    @JsonProperty("direction")
    public AtomicElement.Direction getDirection() {
        return direction;
    }

    @JsonProperty("direction")
    public void setDirection(AtomicElement.Direction direction) {
        this.direction = direction;
    }

    public SDUIAllTypes withDirection(AtomicElement.Direction direction) {
        this.direction = direction;
        return this;
    }

    @JsonProperty("alignment")
    public AtomicElement.Alignment getAlignment() {
        return alignment;
    }

    @JsonProperty("alignment")
    public void setAlignment(AtomicElement.Alignment alignment) {
        this.alignment = alignment;
    }

    public SDUIAllTypes withAlignment(AtomicElement.Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @JsonProperty("crossAlignment")
    public AtomicElement.CrossAlignment getCrossAlignment() {
        return crossAlignment;
    }

    @JsonProperty("crossAlignment")
    public void setCrossAlignment(AtomicElement.CrossAlignment crossAlignment) {
        this.crossAlignment = crossAlignment;
    }

    public SDUIAllTypes withCrossAlignment(AtomicElement.CrossAlignment crossAlignment) {
        this.crossAlignment = crossAlignment;
        return this;
    }

    @JsonProperty("imageFit")
    public AtomicElement.ImageFit getImageFit() {
        return imageFit;
    }

    @JsonProperty("imageFit")
    public void setImageFit(AtomicElement.ImageFit imageFit) {
        this.imageFit = imageFit;
    }

    public SDUIAllTypes withImageFit(AtomicElement.ImageFit imageFit) {
        this.imageFit = imageFit;
        return this;
    }

    @JsonProperty("tabData")
    public TabData getTabData() {
        return tabData;
    }

    @JsonProperty("tabData")
    public void setTabData(TabData tabData) {
        this.tabData = tabData;
    }

    public SDUIAllTypes withTabData(TabData tabData) {
        this.tabData = tabData;
        return this;
    }

    @JsonProperty("navigation")
    public Navigation getNavigation() {
        return navigation;
    }

    @JsonProperty("navigation")
    public void setNavigation(Navigation navigation) {
        this.navigation = navigation;
    }

    public SDUIAllTypes withNavigation(Navigation navigation) {
        this.navigation = navigation;
        return this;
    }

    @JsonProperty("navigationItem")
    public NavigationItem getNavigationItem() {
        return navigationItem;
    }

    @JsonProperty("navigationItem")
    public void setNavigationItem(NavigationItem navigationItem) {
        this.navigationItem = navigationItem;
    }

    public SDUIAllTypes withNavigationItem(NavigationItem navigationItem) {
        this.navigationItem = navigationItem;
        return this;
    }

    /**
     * Tabbed navigation with dynamic content sections per tab. Optional ui is the tab header/control row only; tabContents hosts nested sections. Tab selection uses section.subsections mutate actions.
     * 
     */
    @JsonProperty("tabGroup")
    public TabGroup getTabGroup() {
        return tabGroup;
    }

    /**
     * Tabbed navigation with dynamic content sections per tab. Optional ui is the tab header/control row only; tabContents hosts nested sections. Tab selection uses section.subsections mutate actions.
     * 
     */
    @JsonProperty("tabGroup")
    public void setTabGroup(TabGroup tabGroup) {
        this.tabGroup = tabGroup;
    }

    public SDUIAllTypes withTabGroup(TabGroup tabGroup) {
        this.tabGroup = tabGroup;
        return this;
    }

    /**
     * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
     * 
     */
    @JsonProperty("subsection")
    public Subsection getSubsection() {
        return subsection;
    }

    /**
     * Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
     * 
     */
    @JsonProperty("subsection")
    public void setSubsection(Subsection subsection) {
        this.subsection = subsection;
    }

    public SDUIAllTypes withSubsection(Subsection subsection) {
        this.subsection = subsection;
        return this;
    }

    /**
     * Subscription upsell. Reserved SDK integration point: the upsell's visible chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP product identifiers the SDK will later bind to; `ctaAction` is the optional pre-SDK fallback action. Layout intent (inline banner vs. full-screen hero) is expressed by the inner `ui` atomic tree and the section's outer `surface`, not by component identity.
     * 
     */
    @JsonProperty("subscribeUpsell")
    public SubscribeUpsell getSubscribeUpsell() {
        return subscribeUpsell;
    }

    /**
     * Subscription upsell. Reserved SDK integration point: the upsell's visible chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP product identifiers the SDK will later bind to; `ctaAction` is the optional pre-SDK fallback action. Layout intent (inline banner vs. full-screen hero) is expressed by the inner `ui` atomic tree and the section's outer `surface`, not by component identity.
     * 
     */
    @JsonProperty("subscribeUpsell")
    public void setSubscribeUpsell(SubscribeUpsell subscribeUpsell) {
        this.subscribeUpsell = subscribeUpsell;
    }

    public SDUIAllTypes withSubscribeUpsell(SubscribeUpsell subscribeUpsell) {
        this.subscribeUpsell = subscribeUpsell;
        return this;
    }

    @JsonProperty("subscriptionTier")
    public SubscriptionTier getSubscriptionTier() {
        return subscriptionTier;
    }

    @JsonProperty("subscriptionTier")
    public void setSubscriptionTier(SubscriptionTier subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }

    public SDUIAllTypes withSubscriptionTier(SubscriptionTier subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
        return this;
    }

    /**
     * Typed tabular data for an NBA-style boxscore (one per team)
     * 
     */
    @JsonProperty("boxscoreTable")
    public BoxscoreTable getBoxscoreTable() {
        return boxscoreTable;
    }

    /**
     * Typed tabular data for an NBA-style boxscore (one per team)
     * 
     */
    @JsonProperty("boxscoreTable")
    public void setBoxscoreTable(BoxscoreTable boxscoreTable) {
        this.boxscoreTable = boxscoreTable;
    }

    public SDUIAllTypes withBoxscoreTable(BoxscoreTable boxscoreTable) {
        this.boxscoreTable = boxscoreTable;
        return this;
    }

    /**
     * Platform-native horizontal date picker. All ISO date fields are ET-anchored (America/New_York) calendar days (YYYY-MM-DD). defaultDate is the league's current anchor/focus date — typically today in ET during the regular season, but may be a future date during offseason or breaks (e.g. the regular-season opener). Clients display defaultDate as-is and never compare it to device time.
     * 
     */
    @JsonProperty("calendarStrip")
    public CalendarStrip getCalendarStrip() {
        return calendarStrip;
    }

    /**
     * Platform-native horizontal date picker. All ISO date fields are ET-anchored (America/New_York) calendar days (YYYY-MM-DD). defaultDate is the league's current anchor/focus date — typically today in ET during the regular season, but may be a future date during offseason or breaks (e.g. the regular-season opener). Clients display defaultDate as-is and never compare it to device time.
     * 
     */
    @JsonProperty("calendarStrip")
    public void setCalendarStrip(CalendarStrip calendarStrip) {
        this.calendarStrip = calendarStrip;
    }

    public SDUIAllTypes withCalendarStrip(CalendarStrip calendarStrip) {
        this.calendarStrip = calendarStrip;
        return this;
    }

    /**
     * Vertically-scrollable month-grid calendar with per-date game metadata. All date fields are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current anchor date; drives the 'today' visual highlight and the initial scroll position.
     * 
     */
    @JsonProperty("calendarMonthList")
    public CalendarMonthList getCalendarMonthList() {
        return calendarMonthList;
    }

    /**
     * Vertically-scrollable month-grid calendar with per-date game metadata. All date fields are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current anchor date; drives the 'today' visual highlight and the initial scroll position.
     * 
     */
    @JsonProperty("calendarMonthList")
    public void setCalendarMonthList(CalendarMonthList calendarMonthList) {
        this.calendarMonthList = calendarMonthList;
    }

    public SDUIAllTypes withCalendarMonthList(CalendarMonthList calendarMonthList) {
        this.calendarMonthList = calendarMonthList;
        return this;
    }

    /**
     * Server-driven form section with typed fields bound to screen state
     * 
     */
    @JsonProperty("form")
    public Form getForm() {
        return form;
    }

    /**
     * Server-driven form section with typed fields bound to screen state
     * 
     */
    @JsonProperty("form")
    public void setForm(Form form) {
        this.form = form;
    }

    public SDUIAllTypes withForm(Form form) {
        this.form = form;
        return this;
    }

    /**
     * Ad placement primitive — carries placement semantics while delegating auction/targeting to ad-platform SDKs (see ADR-007)
     * 
     */
    @JsonProperty("adSlot")
    public AdSlot getAdSlot() {
        return adSlot;
    }

    /**
     * Ad placement primitive — carries placement semantics while delegating auction/targeting to ad-platform SDKs (see ADR-007)
     * 
     */
    @JsonProperty("adSlot")
    public void setAdSlot(AdSlot adSlot) {
        this.adSlot = adSlot;
    }

    public SDUIAllTypes withAdSlot(AdSlot adSlot) {
        this.adSlot = adSlot;
        return this;
    }

    /**
     * Sortable, paginated table of season statistical leaders (league-wide)
     * 
     */
    @JsonProperty("seasonLeadersTable")
    public SeasonLeadersTable getSeasonLeadersTable() {
        return seasonLeadersTable;
    }

    /**
     * Sortable, paginated table of season statistical leaders (league-wide)
     * 
     */
    @JsonProperty("seasonLeadersTable")
    public void setSeasonLeadersTable(SeasonLeadersTable seasonLeadersTable) {
        this.seasonLeadersTable = seasonLeadersTable;
    }

    public SDUIAllTypes withSeasonLeadersTable(SeasonLeadersTable seasonLeadersTable) {
        this.seasonLeadersTable = seasonLeadersTable;
        return this;
    }

    /**
     * Video player section — reserved SDK integration point for DRM / HLS / ad insertion. `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders before the SDK is integrated and will serve as the loading/error placeholder afterwards.
     * 
     */
    @JsonProperty("videoPlayer")
    public VideoPlayer getVideoPlayer() {
        return videoPlayer;
    }

    /**
     * Video player section — reserved SDK integration point for DRM / HLS / ad insertion. `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders before the SDK is integrated and will serve as the loading/error placeholder afterwards.
     * 
     */
    @JsonProperty("videoPlayer")
    public void setVideoPlayer(VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
    }

    public SDUIAllTypes withVideoPlayer(VideoPlayer videoPlayer) {
        this.videoPlayer = videoPlayer;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public SDUIAllTypes withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SDUIAllTypes.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("screen");
        sb.append('=');
        sb.append(((this.screen == null)?"<null>":this.screen));
        sb.append(',');
        sb.append("section");
        sb.append('=');
        sb.append(((this.section == null)?"<null>":this.section));
        sb.append(',');
        sb.append("action");
        sb.append('=');
        sb.append(((this.action == null)?"<null>":this.action));
        sb.append(',');
        sb.append("atomicElement");
        sb.append('=');
        sb.append(((this.atomicElement == null)?"<null>":this.atomicElement));
        sb.append(',');
        sb.append("atomicComposite");
        sb.append('=');
        sb.append(((this.atomicComposite == null)?"<null>":this.atomicComposite));
        sb.append(',');
        sb.append("refreshPolicy");
        sb.append('=');
        sb.append(((this.refreshPolicy == null)?"<null>":this.refreshPolicy));
        sb.append(',');
        sb.append("dataBinding");
        sb.append('=');
        sb.append(((this.dataBinding == null)?"<null>":this.dataBinding));
        sb.append(',');
        sb.append("dataBindingPath");
        sb.append('=');
        sb.append(((this.dataBindingPath == null)?"<null>":this.dataBindingPath));
        sb.append(',');
        sb.append("spacing");
        sb.append('=');
        sb.append(((this.spacing == null)?"<null>":this.spacing));
        sb.append(',');
        sb.append("state");
        sb.append('=');
        sb.append(((this.state == null)?"<null>":this.state));
        sb.append(',');
        sb.append("textVariant");
        sb.append('=');
        sb.append(((this.textVariant == null)?"<null>":this.textVariant));
        sb.append(',');
        sb.append("textWeight");
        sb.append('=');
        sb.append(((this.textWeight == null)?"<null>":this.textWeight));
        sb.append(',');
        sb.append("actionType");
        sb.append('=');
        sb.append(((this.actionType == null)?"<null>":this.actionType));
        sb.append(',');
        sb.append("actionTrigger");
        sb.append('=');
        sb.append(((this.actionTrigger == null)?"<null>":this.actionTrigger));
        sb.append(',');
        sb.append("refreshType");
        sb.append('=');
        sb.append(((this.refreshType == null)?"<null>":this.refreshType));
        sb.append(',');
        sb.append("direction");
        sb.append('=');
        sb.append(((this.direction == null)?"<null>":this.direction));
        sb.append(',');
        sb.append("alignment");
        sb.append('=');
        sb.append(((this.alignment == null)?"<null>":this.alignment));
        sb.append(',');
        sb.append("crossAlignment");
        sb.append('=');
        sb.append(((this.crossAlignment == null)?"<null>":this.crossAlignment));
        sb.append(',');
        sb.append("imageFit");
        sb.append('=');
        sb.append(((this.imageFit == null)?"<null>":this.imageFit));
        sb.append(',');
        sb.append("tabData");
        sb.append('=');
        sb.append(((this.tabData == null)?"<null>":this.tabData));
        sb.append(',');
        sb.append("navigation");
        sb.append('=');
        sb.append(((this.navigation == null)?"<null>":this.navigation));
        sb.append(',');
        sb.append("navigationItem");
        sb.append('=');
        sb.append(((this.navigationItem == null)?"<null>":this.navigationItem));
        sb.append(',');
        sb.append("tabGroup");
        sb.append('=');
        sb.append(((this.tabGroup == null)?"<null>":this.tabGroup));
        sb.append(',');
        sb.append("subsection");
        sb.append('=');
        sb.append(((this.subsection == null)?"<null>":this.subsection));
        sb.append(',');
        sb.append("subscribeUpsell");
        sb.append('=');
        sb.append(((this.subscribeUpsell == null)?"<null>":this.subscribeUpsell));
        sb.append(',');
        sb.append("subscriptionTier");
        sb.append('=');
        sb.append(((this.subscriptionTier == null)?"<null>":this.subscriptionTier));
        sb.append(',');
        sb.append("boxscoreTable");
        sb.append('=');
        sb.append(((this.boxscoreTable == null)?"<null>":this.boxscoreTable));
        sb.append(',');
        sb.append("calendarStrip");
        sb.append('=');
        sb.append(((this.calendarStrip == null)?"<null>":this.calendarStrip));
        sb.append(',');
        sb.append("calendarMonthList");
        sb.append('=');
        sb.append(((this.calendarMonthList == null)?"<null>":this.calendarMonthList));
        sb.append(',');
        sb.append("form");
        sb.append('=');
        sb.append(((this.form == null)?"<null>":this.form));
        sb.append(',');
        sb.append("adSlot");
        sb.append('=');
        sb.append(((this.adSlot == null)?"<null>":this.adSlot));
        sb.append(',');
        sb.append("seasonLeadersTable");
        sb.append('=');
        sb.append(((this.seasonLeadersTable == null)?"<null>":this.seasonLeadersTable));
        sb.append(',');
        sb.append("videoPlayer");
        sb.append('=');
        sb.append(((this.videoPlayer == null)?"<null>":this.videoPlayer));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.boxscoreTable == null)? 0 :this.boxscoreTable.hashCode()));
        result = ((result* 31)+((this.tabData == null)? 0 :this.tabData.hashCode()));
        result = ((result* 31)+((this.subsection == null)? 0 :this.subsection.hashCode()));
        result = ((result* 31)+((this.atomicElement == null)? 0 :this.atomicElement.hashCode()));
        result = ((result* 31)+((this.screen == null)? 0 :this.screen.hashCode()));
        result = ((result* 31)+((this.section == null)? 0 :this.section.hashCode()));
        result = ((result* 31)+((this.atomicComposite == null)? 0 :this.atomicComposite.hashCode()));
        result = ((result* 31)+((this.navigation == null)? 0 :this.navigation.hashCode()));
        result = ((result* 31)+((this.spacing == null)? 0 :this.spacing.hashCode()));
        result = ((result* 31)+((this.dataBindingPath == null)? 0 :this.dataBindingPath.hashCode()));
        result = ((result* 31)+((this.textVariant == null)? 0 :this.textVariant.hashCode()));
        result = ((result* 31)+((this.textWeight == null)? 0 :this.textWeight.hashCode()));
        result = ((result* 31)+((this.subscribeUpsell == null)? 0 :this.subscribeUpsell.hashCode()));
        result = ((result* 31)+((this.action == null)? 0 :this.action.hashCode()));
        result = ((result* 31)+((this.state == null)? 0 :this.state.hashCode()));
        result = ((result* 31)+((this.subscriptionTier == null)? 0 :this.subscriptionTier.hashCode()));
        result = ((result* 31)+((this.direction == null)? 0 :this.direction.hashCode()));
        result = ((result* 31)+((this.calendarStrip == null)? 0 :this.calendarStrip.hashCode()));
        result = ((result* 31)+((this.refreshPolicy == null)? 0 :this.refreshPolicy.hashCode()));
        result = ((result* 31)+((this.calendarMonthList == null)? 0 :this.calendarMonthList.hashCode()));
        result = ((result* 31)+((this.navigationItem == null)? 0 :this.navigationItem.hashCode()));
        result = ((result* 31)+((this.adSlot == null)? 0 :this.adSlot.hashCode()));
        result = ((result* 31)+((this.seasonLeadersTable == null)? 0 :this.seasonLeadersTable.hashCode()));
        result = ((result* 31)+((this.videoPlayer == null)? 0 :this.videoPlayer.hashCode()));
        result = ((result* 31)+((this.actionTrigger == null)? 0 :this.actionTrigger.hashCode()));
        result = ((result* 31)+((this.actionType == null)? 0 :this.actionType.hashCode()));
        result = ((result* 31)+((this.crossAlignment == null)? 0 :this.crossAlignment.hashCode()));
        result = ((result* 31)+((this.form == null)? 0 :this.form.hashCode()));
        result = ((result* 31)+((this.imageFit == null)? 0 :this.imageFit.hashCode()));
        result = ((result* 31)+((this.tabGroup == null)? 0 :this.tabGroup.hashCode()));
        result = ((result* 31)+((this.dataBinding == null)? 0 :this.dataBinding.hashCode()));
        result = ((result* 31)+((this.refreshType == null)? 0 :this.refreshType.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.alignment == null)? 0 :this.alignment.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SDUIAllTypes) == false) {
            return false;
        }
        SDUIAllTypes rhs = ((SDUIAllTypes) other);
        return (((((((((((((((((((((((((((((((((((this.boxscoreTable == rhs.boxscoreTable)||((this.boxscoreTable!= null)&&this.boxscoreTable.equals(rhs.boxscoreTable)))&&((this.tabData == rhs.tabData)||((this.tabData!= null)&&this.tabData.equals(rhs.tabData))))&&((this.subsection == rhs.subsection)||((this.subsection!= null)&&this.subsection.equals(rhs.subsection))))&&((this.atomicElement == rhs.atomicElement)||((this.atomicElement!= null)&&this.atomicElement.equals(rhs.atomicElement))))&&((this.screen == rhs.screen)||((this.screen!= null)&&this.screen.equals(rhs.screen))))&&((this.section == rhs.section)||((this.section!= null)&&this.section.equals(rhs.section))))&&((this.atomicComposite == rhs.atomicComposite)||((this.atomicComposite!= null)&&this.atomicComposite.equals(rhs.atomicComposite))))&&((this.navigation == rhs.navigation)||((this.navigation!= null)&&this.navigation.equals(rhs.navigation))))&&((this.spacing == rhs.spacing)||((this.spacing!= null)&&this.spacing.equals(rhs.spacing))))&&((this.dataBindingPath == rhs.dataBindingPath)||((this.dataBindingPath!= null)&&this.dataBindingPath.equals(rhs.dataBindingPath))))&&((this.textVariant == rhs.textVariant)||((this.textVariant!= null)&&this.textVariant.equals(rhs.textVariant))))&&((this.textWeight == rhs.textWeight)||((this.textWeight!= null)&&this.textWeight.equals(rhs.textWeight))))&&((this.subscribeUpsell == rhs.subscribeUpsell)||((this.subscribeUpsell!= null)&&this.subscribeUpsell.equals(rhs.subscribeUpsell))))&&((this.action == rhs.action)||((this.action!= null)&&this.action.equals(rhs.action))))&&((this.state == rhs.state)||((this.state!= null)&&this.state.equals(rhs.state))))&&((this.subscriptionTier == rhs.subscriptionTier)||((this.subscriptionTier!= null)&&this.subscriptionTier.equals(rhs.subscriptionTier))))&&((this.direction == rhs.direction)||((this.direction!= null)&&this.direction.equals(rhs.direction))))&&((this.calendarStrip == rhs.calendarStrip)||((this.calendarStrip!= null)&&this.calendarStrip.equals(rhs.calendarStrip))))&&((this.refreshPolicy == rhs.refreshPolicy)||((this.refreshPolicy!= null)&&this.refreshPolicy.equals(rhs.refreshPolicy))))&&((this.calendarMonthList == rhs.calendarMonthList)||((this.calendarMonthList!= null)&&this.calendarMonthList.equals(rhs.calendarMonthList))))&&((this.navigationItem == rhs.navigationItem)||((this.navigationItem!= null)&&this.navigationItem.equals(rhs.navigationItem))))&&((this.adSlot == rhs.adSlot)||((this.adSlot!= null)&&this.adSlot.equals(rhs.adSlot))))&&((this.seasonLeadersTable == rhs.seasonLeadersTable)||((this.seasonLeadersTable!= null)&&this.seasonLeadersTable.equals(rhs.seasonLeadersTable))))&&((this.videoPlayer == rhs.videoPlayer)||((this.videoPlayer!= null)&&this.videoPlayer.equals(rhs.videoPlayer))))&&((this.actionTrigger == rhs.actionTrigger)||((this.actionTrigger!= null)&&this.actionTrigger.equals(rhs.actionTrigger))))&&((this.actionType == rhs.actionType)||((this.actionType!= null)&&this.actionType.equals(rhs.actionType))))&&((this.crossAlignment == rhs.crossAlignment)||((this.crossAlignment!= null)&&this.crossAlignment.equals(rhs.crossAlignment))))&&((this.form == rhs.form)||((this.form!= null)&&this.form.equals(rhs.form))))&&((this.imageFit == rhs.imageFit)||((this.imageFit!= null)&&this.imageFit.equals(rhs.imageFit))))&&((this.tabGroup == rhs.tabGroup)||((this.tabGroup!= null)&&this.tabGroup.equals(rhs.tabGroup))))&&((this.dataBinding == rhs.dataBinding)||((this.dataBinding!= null)&&this.dataBinding.equals(rhs.dataBinding))))&&((this.refreshType == rhs.refreshType)||((this.refreshType!= null)&&this.refreshType.equals(rhs.refreshType))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.alignment == rhs.alignment)||((this.alignment!= null)&&this.alignment.equals(rhs.alignment))));
    }


    /**
     * Material3 typography scale plus the NBA-specific `score` variant (monospaced digits for live score / clock rendering).
     * 
     */
    @Generated("jsonschema2pojo")
    public enum TextVariant {

        DISPLAY_LARGE("displayLarge"),
        DISPLAY_MEDIUM("displayMedium"),
        DISPLAY_SMALL("displaySmall"),
        HEADLINE_LARGE("headlineLarge"),
        HEADLINE_MEDIUM("headlineMedium"),
        HEADLINE_SMALL("headlineSmall"),
        TITLE_LARGE("titleLarge"),
        TITLE_MEDIUM("titleMedium"),
        TITLE_SMALL("titleSmall"),
        BODY_LARGE("bodyLarge"),
        BODY_MEDIUM("bodyMedium"),
        BODY_SMALL("bodySmall"),
        LABEL_LARGE("labelLarge"),
        LABEL_MEDIUM("labelMedium"),
        LABEL_SMALL("labelSmall"),
        SCORE("score");
        private final String value;
        private final static Map<String, SDUIAllTypes.TextVariant> CONSTANTS = new HashMap<String, SDUIAllTypes.TextVariant>();

        static {
            for (SDUIAllTypes.TextVariant c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        TextVariant(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }

        @JsonValue
        public String value() {
            return this.value;
        }

        @JsonCreator
        public static SDUIAllTypes.TextVariant fromValue(String value) {
            SDUIAllTypes.TextVariant constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
