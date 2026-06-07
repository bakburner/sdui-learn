
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "schemaVersion",
    "title",
    "analyticsId",
    "parentUri",
    "defaultRefreshPolicy",
    "navigation",
    "state",
    "actions",
    "contentInsets",
    "sections",
    "overlays",
    "variants"
})
@Generated("jsonschema2pojo")
public class Screen {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private String id;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schemaVersion")
    @NotNull
    private String schemaVersion;
    /**
     * Legacy headline consumed at composition time to build the first AtomicComposite app-bar section (see prependAppBarHeader). Not rendered from screen.title by clients. Omit on bottom-nav tab destinations.
     * 
     */
    @JsonProperty("title")
    @JsonPropertyDescription("Legacy headline consumed at composition time to build the first AtomicComposite app-bar section (see prependAppBarHeader). Not rendered from screen.title by clients. Omit on bottom-nav tab destinations.")
    private String title;
    @JsonProperty("analyticsId")
    private String analyticsId;
    /**
     * URI the back button should navigate to.  Clients always show a back button; this field tells them the target.  Omit for root screens (e.g. scoreboard).
     * 
     */
    @JsonProperty("parentUri")
    @JsonPropertyDescription("URI the back button should navigate to.  Clients always show a back button; this field tells them the target.  Omit for root screens (e.g. scoreboard).")
    private String parentUri;
    @JsonProperty("defaultRefreshPolicy")
    @Valid
    private RefreshPolicy defaultRefreshPolicy;
    @JsonProperty("navigation")
    @Valid
    private Navigation navigation;
    /**
     * Screen-level state map for interactive patterns
     * 
     */
    @JsonProperty("state")
    @JsonPropertyDescription("Screen-level state map for interactive patterns")
    @Valid
    private State state;
    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     * 
     */
    @JsonProperty("actions")
    @JsonPropertyDescription("Screen-level actions (e.g. analytics beacons, lifecycle hooks)")
    @Valid
    private List<Action> actions = new ArrayList<Action>();
    @JsonProperty("contentInsets")
    @Valid
    private Spacing contentInsets;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sections")
    @Valid
    @NotNull
    private List<Section> sections = new ArrayList<Section>();
    /**
     * Named overlay sections the client shows when a trigger condition arises. Keys are developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed sections (typically AtomicComposite). Client controls trigger timing and presentation style; server controls display content.
     * 
     */
    @JsonProperty("overlays")
    @JsonPropertyDescription("Named overlay sections the client shows when a trigger condition arises. Keys are developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed sections (typically AtomicComposite). Client controls trigger timing and presentation style; server controls display content.")
    @Valid
    private Overlays overlays;
    /**
     * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets clients expose variant selection without hardcoding experiment ids or option vocabularies.
     * 
     */
    @JsonProperty("variants")
    @JsonPropertyDescription("Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets clients expose variant selection without hardcoding experiment ids or option vocabularies.")
    @Valid
    private ExperimentVariants variants;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Screen withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schemaVersion")
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("schemaVersion")
    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Screen withSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    /**
     * Legacy headline consumed at composition time to build the first AtomicComposite app-bar section (see prependAppBarHeader). Not rendered from screen.title by clients. Omit on bottom-nav tab destinations.
     * 
     */
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    /**
     * Legacy headline consumed at composition time to build the first AtomicComposite app-bar section (see prependAppBarHeader). Not rendered from screen.title by clients. Omit on bottom-nav tab destinations.
     * 
     */
    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public Screen withTitle(String title) {
        this.title = title;
        return this;
    }

    @JsonProperty("analyticsId")
    public String getAnalyticsId() {
        return analyticsId;
    }

    @JsonProperty("analyticsId")
    public void setAnalyticsId(String analyticsId) {
        this.analyticsId = analyticsId;
    }

    public Screen withAnalyticsId(String analyticsId) {
        this.analyticsId = analyticsId;
        return this;
    }

    /**
     * URI the back button should navigate to.  Clients always show a back button; this field tells them the target.  Omit for root screens (e.g. scoreboard).
     * 
     */
    @JsonProperty("parentUri")
    public String getParentUri() {
        return parentUri;
    }

    /**
     * URI the back button should navigate to.  Clients always show a back button; this field tells them the target.  Omit for root screens (e.g. scoreboard).
     * 
     */
    @JsonProperty("parentUri")
    public void setParentUri(String parentUri) {
        this.parentUri = parentUri;
    }

    public Screen withParentUri(String parentUri) {
        this.parentUri = parentUri;
        return this;
    }

    @JsonProperty("defaultRefreshPolicy")
    public RefreshPolicy getDefaultRefreshPolicy() {
        return defaultRefreshPolicy;
    }

    @JsonProperty("defaultRefreshPolicy")
    public void setDefaultRefreshPolicy(RefreshPolicy defaultRefreshPolicy) {
        this.defaultRefreshPolicy = defaultRefreshPolicy;
    }

    public Screen withDefaultRefreshPolicy(RefreshPolicy defaultRefreshPolicy) {
        this.defaultRefreshPolicy = defaultRefreshPolicy;
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

    public Screen withNavigation(Navigation navigation) {
        this.navigation = navigation;
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

    public Screen withState(State state) {
        this.state = state;
        return this;
    }

    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     * 
     */
    @JsonProperty("actions")
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Screen-level actions (e.g. analytics beacons, lifecycle hooks)
     * 
     */
    @JsonProperty("actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public Screen withActions(List<Action> actions) {
        this.actions = actions;
        return this;
    }

    @JsonProperty("contentInsets")
    public Spacing getContentInsets() {
        return contentInsets;
    }

    @JsonProperty("contentInsets")
    public void setContentInsets(Spacing contentInsets) {
        this.contentInsets = contentInsets;
    }

    public Screen withContentInsets(Spacing contentInsets) {
        this.contentInsets = contentInsets;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sections")
    public List<Section> getSections() {
        return sections;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("sections")
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public Screen withSections(List<Section> sections) {
        this.sections = sections;
        return this;
    }

    /**
     * Named overlay sections the client shows when a trigger condition arises. Keys are developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed sections (typically AtomicComposite). Client controls trigger timing and presentation style; server controls display content.
     * 
     */
    @JsonProperty("overlays")
    public Overlays getOverlays() {
        return overlays;
    }

    /**
     * Named overlay sections the client shows when a trigger condition arises. Keys are developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed sections (typically AtomicComposite). Client controls trigger timing and presentation style; server controls display content.
     * 
     */
    @JsonProperty("overlays")
    public void setOverlays(Overlays overlays) {
        this.overlays = overlays;
    }

    public Screen withOverlays(Overlays overlays) {
        this.overlays = overlays;
        return this;
    }

    /**
     * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets clients expose variant selection without hardcoding experiment ids or option vocabularies.
     * 
     */
    @JsonProperty("variants")
    public ExperimentVariants getVariants() {
        return variants;
    }

    /**
     * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets clients expose variant selection without hardcoding experiment ids or option vocabularies.
     * 
     */
    @JsonProperty("variants")
    public void setVariants(ExperimentVariants variants) {
        this.variants = variants;
    }

    public Screen withVariants(ExperimentVariants variants) {
        this.variants = variants;
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

    public Screen withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Screen.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("schemaVersion");
        sb.append('=');
        sb.append(((this.schemaVersion == null)?"<null>":this.schemaVersion));
        sb.append(',');
        sb.append("title");
        sb.append('=');
        sb.append(((this.title == null)?"<null>":this.title));
        sb.append(',');
        sb.append("analyticsId");
        sb.append('=');
        sb.append(((this.analyticsId == null)?"<null>":this.analyticsId));
        sb.append(',');
        sb.append("parentUri");
        sb.append('=');
        sb.append(((this.parentUri == null)?"<null>":this.parentUri));
        sb.append(',');
        sb.append("defaultRefreshPolicy");
        sb.append('=');
        sb.append(((this.defaultRefreshPolicy == null)?"<null>":this.defaultRefreshPolicy));
        sb.append(',');
        sb.append("navigation");
        sb.append('=');
        sb.append(((this.navigation == null)?"<null>":this.navigation));
        sb.append(',');
        sb.append("state");
        sb.append('=');
        sb.append(((this.state == null)?"<null>":this.state));
        sb.append(',');
        sb.append("actions");
        sb.append('=');
        sb.append(((this.actions == null)?"<null>":this.actions));
        sb.append(',');
        sb.append("contentInsets");
        sb.append('=');
        sb.append(((this.contentInsets == null)?"<null>":this.contentInsets));
        sb.append(',');
        sb.append("sections");
        sb.append('=');
        sb.append(((this.sections == null)?"<null>":this.sections));
        sb.append(',');
        sb.append("overlays");
        sb.append('=');
        sb.append(((this.overlays == null)?"<null>":this.overlays));
        sb.append(',');
        sb.append("variants");
        sb.append('=');
        sb.append(((this.variants == null)?"<null>":this.variants));
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
        result = ((result* 31)+((this.schemaVersion == null)? 0 :this.schemaVersion.hashCode()));
        result = ((result* 31)+((this.parentUri == null)? 0 :this.parentUri.hashCode()));
        result = ((result* 31)+((this.defaultRefreshPolicy == null)? 0 :this.defaultRefreshPolicy.hashCode()));
        result = ((result* 31)+((this.contentInsets == null)? 0 :this.contentInsets.hashCode()));
        result = ((result* 31)+((this.variants == null)? 0 :this.variants.hashCode()));
        result = ((result* 31)+((this.title == null)? 0 :this.title.hashCode()));
        result = ((result* 31)+((this.sections == null)? 0 :this.sections.hashCode()));
        result = ((result* 31)+((this.navigation == null)? 0 :this.navigation.hashCode()));
        result = ((result* 31)+((this.overlays == null)? 0 :this.overlays.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.state == null)? 0 :this.state.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.analyticsId == null)? 0 :this.analyticsId.hashCode()));
        result = ((result* 31)+((this.actions == null)? 0 :this.actions.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Screen) == false) {
            return false;
        }
        Screen rhs = ((Screen) other);
        return (((((((((((((((this.schemaVersion == rhs.schemaVersion)||((this.schemaVersion!= null)&&this.schemaVersion.equals(rhs.schemaVersion)))&&((this.parentUri == rhs.parentUri)||((this.parentUri!= null)&&this.parentUri.equals(rhs.parentUri))))&&((this.defaultRefreshPolicy == rhs.defaultRefreshPolicy)||((this.defaultRefreshPolicy!= null)&&this.defaultRefreshPolicy.equals(rhs.defaultRefreshPolicy))))&&((this.contentInsets == rhs.contentInsets)||((this.contentInsets!= null)&&this.contentInsets.equals(rhs.contentInsets))))&&((this.variants == rhs.variants)||((this.variants!= null)&&this.variants.equals(rhs.variants))))&&((this.title == rhs.title)||((this.title!= null)&&this.title.equals(rhs.title))))&&((this.sections == rhs.sections)||((this.sections!= null)&&this.sections.equals(rhs.sections))))&&((this.navigation == rhs.navigation)||((this.navigation!= null)&&this.navigation.equals(rhs.navigation))))&&((this.overlays == rhs.overlays)||((this.overlays!= null)&&this.overlays.equals(rhs.overlays))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.state == rhs.state)||((this.state!= null)&&this.state.equals(rhs.state))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.analyticsId == rhs.analyticsId)||((this.analyticsId!= null)&&this.analyticsId.equals(rhs.analyticsId))))&&((this.actions == rhs.actions)||((this.actions!= null)&&this.actions.equals(rhs.actions))));
    }

}
