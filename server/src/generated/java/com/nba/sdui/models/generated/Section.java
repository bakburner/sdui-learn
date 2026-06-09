
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "type",
    "refreshPolicy",
    "dataBinding",
    "actions",
    "subsections",
    "analyticsId",
    "contentSourceId",
    "accessibility",
    "surface",
    "sectionStates",
    "stringTable",
    "data"
})
@Generated("jsonschema2pojo")
public class Section {

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
    @JsonProperty("type")
    @NotNull
    private Section.Type type;
    /**
     * Array of concurrent refresh mechanisms (max 2): at most one opaque/streaming element (sse channel or url poll, consumes dataBinding) plus at most one section-refresh element (sectionEndpoint, full-replace). A static section is a single {type:static} element. Cross-element invariants (<=1 opaque, <=1 sectionEndpoint, static-solo) are validated server-side.
     * 
     */
    @JsonProperty("refreshPolicy")
    @JsonPropertyDescription("Array of concurrent refresh mechanisms (max 2): at most one opaque/streaming element (sse channel or url poll, consumes dataBinding) plus at most one section-refresh element (sectionEndpoint, full-replace). A static section is a single {type:static} element. Cross-element invariants (<=1 opaque, <=1 sectionEndpoint, static-solo) are validated server-side.")
    @Size(max = 2)
    @Valid
    private List<RefreshPolicy> refreshPolicy = new ArrayList<RefreshPolicy>();
    @JsonProperty("dataBinding")
    @Valid
    private DataBinding dataBinding;
    /**
     * Section-level interaction actions
     * 
     */
    @JsonProperty("actions")
    @JsonPropertyDescription("Section-level interaction actions")
    @Valid
    private List<Action> actions = new ArrayList<Action>();
    /**
     * Nested interaction targets within the section
     * 
     */
    @JsonProperty("subsections")
    @JsonPropertyDescription("Nested interaction targets within the section")
    @Valid
    private List<Subsection> subsections = new ArrayList<Subsection>();
    @JsonProperty("analyticsId")
    private String analyticsId;
    /**
     * Origin identifier for the content backing this section (e.g. 'cms:article-42', 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.
     * 
     */
    @JsonProperty("contentSourceId")
    @JsonPropertyDescription("Origin identifier for the content backing this section (e.g. 'cms:article-42', 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.")
    private String contentSourceId;
    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    @JsonPropertyDescription("Server-provided accessibility metadata applied natively per platform")
    @Valid
    private AccessibilityProperties accessibility;
    /**
     * Server-driven surface spec applied by the client's SectionRouter to every semantic section — the visual wrapper beneath the section's content. Mirrors the inline-chrome vocabulary on AtomicContainer so semantic sections have schema parity with composed sections. Every client's shared SectionContainer wrapper reads these fields; semantic-section renderers do not set outer padding, margin, corner radius, shadow, border, or background themselves. The sibling `data` field carries content (including the atomic UI tree); `surface` carries the frame that sits beneath it.
     * 
     */
    @JsonProperty("surface")
    @JsonPropertyDescription("Server-driven surface spec applied by the client's SectionRouter to every semantic section \u2014 the visual wrapper beneath the section's content. Mirrors the inline-chrome vocabulary on AtomicContainer so semantic sections have schema parity with composed sections. Every client's shared SectionContainer wrapper reads these fields; semantic-section renderers do not set outer padding, margin, corner radius, shadow, border, or background themselves. The sibling `data` field carries content (including the atomic UI tree); `surface` carries the frame that sits beneath it.")
    @Valid
    private SectionSurface surface;
    /**
     * Server-declared loading and error presentation for a section. Clients render these states when applicable.
     * 
     */
    @JsonProperty("sectionStates")
    @JsonPropertyDescription("Server-declared loading and error presentation for a section. Clients render these states when applicable.")
    @Valid
    private SectionStates sectionStates;
    /**
     * Section-level map of translation key to localized string. Used by DataBindingResolver to resolve stringKeys on real-time updates.
     * 
     */
    @JsonProperty("stringTable")
    @JsonPropertyDescription("Section-level map of translation key to localized string. Used by DataBindingResolver to resolve stringKeys on real-time updates.")
    @Valid
    private StringTable stringTable;
    /**
     * Section-specific component payload (content + per-component actions + configuration). The variants are listed via anyOf so codegen reaches every component definition; per-variant enforcement is the allOf/if/then chain on Section (discriminated by Section.type). Codegen merges the anyOf members into a single super-shape per platform — every per-component property surfaces as optional on the merged type, which is the shape renderers consume after dispatching on Section.type.
     * 
     */
    @JsonProperty("data")
    @JsonPropertyDescription("Section-specific component payload (content + per-component actions + configuration). The variants are listed via anyOf so codegen reaches every component definition; per-variant enforcement is the allOf/if/then chain on Section (discriminated by Section.type). Codegen merges the anyOf members into a single super-shape per platform \u2014 every per-component property surfaces as optional on the merged type, which is the shape renderers consume after dispatching on Section.type.")
    private Object data;
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

    public Section withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public Section.Type getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(Section.Type type) {
        this.type = type;
    }

    public Section withType(Section.Type type) {
        this.type = type;
        return this;
    }

    /**
     * Array of concurrent refresh mechanisms (max 2): at most one opaque/streaming element (sse channel or url poll, consumes dataBinding) plus at most one section-refresh element (sectionEndpoint, full-replace). A static section is a single {type:static} element. Cross-element invariants (<=1 opaque, <=1 sectionEndpoint, static-solo) are validated server-side.
     * 
     */
    @JsonProperty("refreshPolicy")
    public List<RefreshPolicy> getRefreshPolicy() {
        return refreshPolicy;
    }

    /**
     * Array of concurrent refresh mechanisms (max 2): at most one opaque/streaming element (sse channel or url poll, consumes dataBinding) plus at most one section-refresh element (sectionEndpoint, full-replace). A static section is a single {type:static} element. Cross-element invariants (<=1 opaque, <=1 sectionEndpoint, static-solo) are validated server-side.
     * 
     */
    @JsonProperty("refreshPolicy")
    public void setRefreshPolicy(List<RefreshPolicy> refreshPolicy) {
        this.refreshPolicy = refreshPolicy;
    }

    public Section withRefreshPolicy(List<RefreshPolicy> refreshPolicy) {
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

    public Section withDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
        return this;
    }

    /**
     * Section-level interaction actions
     * 
     */
    @JsonProperty("actions")
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Section-level interaction actions
     * 
     */
    @JsonProperty("actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public Section withActions(List<Action> actions) {
        this.actions = actions;
        return this;
    }

    /**
     * Nested interaction targets within the section
     * 
     */
    @JsonProperty("subsections")
    public List<Subsection> getSubsections() {
        return subsections;
    }

    /**
     * Nested interaction targets within the section
     * 
     */
    @JsonProperty("subsections")
    public void setSubsections(List<Subsection> subsections) {
        this.subsections = subsections;
    }

    public Section withSubsections(List<Subsection> subsections) {
        this.subsections = subsections;
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

    public Section withAnalyticsId(String analyticsId) {
        this.analyticsId = analyticsId;
        return this;
    }

    /**
     * Origin identifier for the content backing this section (e.g. 'cms:article-42', 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.
     * 
     */
    @JsonProperty("contentSourceId")
    public String getContentSourceId() {
        return contentSourceId;
    }

    /**
     * Origin identifier for the content backing this section (e.g. 'cms:article-42', 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.
     * 
     */
    @JsonProperty("contentSourceId")
    public void setContentSourceId(String contentSourceId) {
        this.contentSourceId = contentSourceId;
    }

    public Section withContentSourceId(String contentSourceId) {
        this.contentSourceId = contentSourceId;
        return this;
    }

    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    public AccessibilityProperties getAccessibility() {
        return accessibility;
    }

    /**
     * Server-provided accessibility metadata applied natively per platform
     * 
     */
    @JsonProperty("accessibility")
    public void setAccessibility(AccessibilityProperties accessibility) {
        this.accessibility = accessibility;
    }

    public Section withAccessibility(AccessibilityProperties accessibility) {
        this.accessibility = accessibility;
        return this;
    }

    /**
     * Server-driven surface spec applied by the client's SectionRouter to every semantic section — the visual wrapper beneath the section's content. Mirrors the inline-chrome vocabulary on AtomicContainer so semantic sections have schema parity with composed sections. Every client's shared SectionContainer wrapper reads these fields; semantic-section renderers do not set outer padding, margin, corner radius, shadow, border, or background themselves. The sibling `data` field carries content (including the atomic UI tree); `surface` carries the frame that sits beneath it.
     * 
     */
    @JsonProperty("surface")
    public SectionSurface getSurface() {
        return surface;
    }

    /**
     * Server-driven surface spec applied by the client's SectionRouter to every semantic section — the visual wrapper beneath the section's content. Mirrors the inline-chrome vocabulary on AtomicContainer so semantic sections have schema parity with composed sections. Every client's shared SectionContainer wrapper reads these fields; semantic-section renderers do not set outer padding, margin, corner radius, shadow, border, or background themselves. The sibling `data` field carries content (including the atomic UI tree); `surface` carries the frame that sits beneath it.
     * 
     */
    @JsonProperty("surface")
    public void setSurface(SectionSurface surface) {
        this.surface = surface;
    }

    public Section withSurface(SectionSurface surface) {
        this.surface = surface;
        return this;
    }

    /**
     * Server-declared loading and error presentation for a section. Clients render these states when applicable.
     * 
     */
    @JsonProperty("sectionStates")
    public SectionStates getSectionStates() {
        return sectionStates;
    }

    /**
     * Server-declared loading and error presentation for a section. Clients render these states when applicable.
     * 
     */
    @JsonProperty("sectionStates")
    public void setSectionStates(SectionStates sectionStates) {
        this.sectionStates = sectionStates;
    }

    public Section withSectionStates(SectionStates sectionStates) {
        this.sectionStates = sectionStates;
        return this;
    }

    /**
     * Section-level map of translation key to localized string. Used by DataBindingResolver to resolve stringKeys on real-time updates.
     * 
     */
    @JsonProperty("stringTable")
    public StringTable getStringTable() {
        return stringTable;
    }

    /**
     * Section-level map of translation key to localized string. Used by DataBindingResolver to resolve stringKeys on real-time updates.
     * 
     */
    @JsonProperty("stringTable")
    public void setStringTable(StringTable stringTable) {
        this.stringTable = stringTable;
    }

    public Section withStringTable(StringTable stringTable) {
        this.stringTable = stringTable;
        return this;
    }

    /**
     * Section-specific component payload (content + per-component actions + configuration). The variants are listed via anyOf so codegen reaches every component definition; per-variant enforcement is the allOf/if/then chain on Section (discriminated by Section.type). Codegen merges the anyOf members into a single super-shape per platform — every per-component property surfaces as optional on the merged type, which is the shape renderers consume after dispatching on Section.type.
     * 
     */
    @JsonProperty("data")
    public Object getData() {
        return data;
    }

    /**
     * Section-specific component payload (content + per-component actions + configuration). The variants are listed via anyOf so codegen reaches every component definition; per-variant enforcement is the allOf/if/then chain on Section (discriminated by Section.type). Codegen merges the anyOf members into a single super-shape per platform — every per-component property surfaces as optional on the merged type, which is the shape renderers consume after dispatching on Section.type.
     * 
     */
    @JsonProperty("data")
    public void setData(Object data) {
        this.data = data;
    }

    public Section withData(Object data) {
        this.data = data;
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

    public Section withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Section.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("refreshPolicy");
        sb.append('=');
        sb.append(((this.refreshPolicy == null)?"<null>":this.refreshPolicy));
        sb.append(',');
        sb.append("dataBinding");
        sb.append('=');
        sb.append(((this.dataBinding == null)?"<null>":this.dataBinding));
        sb.append(',');
        sb.append("actions");
        sb.append('=');
        sb.append(((this.actions == null)?"<null>":this.actions));
        sb.append(',');
        sb.append("subsections");
        sb.append('=');
        sb.append(((this.subsections == null)?"<null>":this.subsections));
        sb.append(',');
        sb.append("analyticsId");
        sb.append('=');
        sb.append(((this.analyticsId == null)?"<null>":this.analyticsId));
        sb.append(',');
        sb.append("contentSourceId");
        sb.append('=');
        sb.append(((this.contentSourceId == null)?"<null>":this.contentSourceId));
        sb.append(',');
        sb.append("accessibility");
        sb.append('=');
        sb.append(((this.accessibility == null)?"<null>":this.accessibility));
        sb.append(',');
        sb.append("surface");
        sb.append('=');
        sb.append(((this.surface == null)?"<null>":this.surface));
        sb.append(',');
        sb.append("sectionStates");
        sb.append('=');
        sb.append(((this.sectionStates == null)?"<null>":this.sectionStates));
        sb.append(',');
        sb.append("stringTable");
        sb.append('=');
        sb.append(((this.stringTable == null)?"<null>":this.stringTable));
        sb.append(',');
        sb.append("data");
        sb.append('=');
        sb.append(((this.data == null)?"<null>":this.data));
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
        result = ((result* 31)+((this.contentSourceId == null)? 0 :this.contentSourceId.hashCode()));
        result = ((result* 31)+((this.surface == null)? 0 :this.surface.hashCode()));
        result = ((result* 31)+((this.data == null)? 0 :this.data.hashCode()));
        result = ((result* 31)+((this.accessibility == null)? 0 :this.accessibility.hashCode()));
        result = ((result* 31)+((this.refreshPolicy == null)? 0 :this.refreshPolicy.hashCode()));
        result = ((result* 31)+((this.stringTable == null)? 0 :this.stringTable.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.subsections == null)? 0 :this.subsections.hashCode()));
        result = ((result* 31)+((this.dataBinding == null)? 0 :this.dataBinding.hashCode()));
        result = ((result* 31)+((this.sectionStates == null)? 0 :this.sectionStates.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
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
        if ((other instanceof Section) == false) {
            return false;
        }
        Section rhs = ((Section) other);
        return (((((((((((((((this.contentSourceId == rhs.contentSourceId)||((this.contentSourceId!= null)&&this.contentSourceId.equals(rhs.contentSourceId)))&&((this.surface == rhs.surface)||((this.surface!= null)&&this.surface.equals(rhs.surface))))&&((this.data == rhs.data)||((this.data!= null)&&this.data.equals(rhs.data))))&&((this.accessibility == rhs.accessibility)||((this.accessibility!= null)&&this.accessibility.equals(rhs.accessibility))))&&((this.refreshPolicy == rhs.refreshPolicy)||((this.refreshPolicy!= null)&&this.refreshPolicy.equals(rhs.refreshPolicy))))&&((this.stringTable == rhs.stringTable)||((this.stringTable!= null)&&this.stringTable.equals(rhs.stringTable))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.subsections == rhs.subsections)||((this.subsections!= null)&&this.subsections.equals(rhs.subsections))))&&((this.dataBinding == rhs.dataBinding)||((this.dataBinding!= null)&&this.dataBinding.equals(rhs.dataBinding))))&&((this.sectionStates == rhs.sectionStates)||((this.sectionStates!= null)&&this.sectionStates.equals(rhs.sectionStates))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.analyticsId == rhs.analyticsId)||((this.analyticsId!= null)&&this.analyticsId.equals(rhs.analyticsId))))&&((this.actions == rhs.actions)||((this.actions!= null)&&this.actions.equals(rhs.actions))));
    }

    @Generated("jsonschema2pojo")
    public enum Type {

        TAB_GROUP("TabGroup"),
        BOXSCORE_TABLE("BoxscoreTable"),
        CALENDAR_STRIP("CalendarStrip"),
        CALENDAR_MONTH_LIST("CalendarMonthList"),
        FORM("Form"),
        AD_SLOT("AdSlot"),
        SEASON_LEADERS_TABLE("SeasonLeadersTable"),
        SUBSCRIBE_UPSELL("SubscribeUpsell"),
        ATOMIC_COMPOSITE("AtomicComposite"),
        VIDEO_PLAYER("VideoPlayer");
        private final String value;
        private final static Map<String, Section.Type> CONSTANTS = new HashMap<String, Section.Type>();

        static {
            for (Section.Type c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Type(String value) {
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
        public static Section.Type fromValue(String value) {
            Section.Type constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
