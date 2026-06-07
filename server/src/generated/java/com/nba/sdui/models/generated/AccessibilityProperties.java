
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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;


/**
 * Server-provided accessibility metadata applied natively per platform
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "label",
    "role",
    "hidden",
    "headingLevel",
    "liveRegion",
    "sortOrder",
    "hint"
})
@Generated("jsonschema2pojo")
public class AccessibilityProperties {

    /**
     * Human-readable label announced by screen readers. Omit for elements whose text content is self-describing.
     * 
     */
    @JsonProperty("label")
    @JsonPropertyDescription("Human-readable label announced by screen readers. Omit for elements whose text content is self-describing.")
    private String label;
    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     * 
     */
    @JsonProperty("role")
    @JsonPropertyDescription("Semantic role override. 'none' suppresses the element's intrinsic role.")
    private AccessibilityProperties.Role role;
    /**
     * When true, element and its descendants are hidden from the accessibility tree (decorative content).
     * 
     */
    @JsonProperty("hidden")
    @JsonPropertyDescription("When true, element and its descendants are hidden from the accessibility tree (decorative content).")
    private Boolean hidden = false;
    /**
     * Heading level (1-6) for role=heading elements. Maps to aria-level (Web), accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
     * 
     */
    @JsonProperty("headingLevel")
    @JsonPropertyDescription("Heading level (1-6) for role=heading elements. Maps to aria-level (Web), accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).")
    @DecimalMin("1")
    @DecimalMax("6")
    private Integer headingLevel;
    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     * 
     */
    @JsonProperty("liveRegion")
    @JsonPropertyDescription("Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).")
    private AccessibilityProperties.LiveRegion liveRegion;
    /**
     * Override default accessibility traversal order. Lower values are visited first. Omit to use natural DOM/view order.
     * 
     */
    @JsonProperty("sortOrder")
    @JsonPropertyDescription("Override default accessibility traversal order. Lower values are visited first. Omit to use natural DOM/view order.")
    private Integer sortOrder;
    /**
     * Additional context announced after the label. Maps to accessibilityHint (iOS), contentDescription suffix (Android), aria-describedby text (Web).
     * 
     */
    @JsonProperty("hint")
    @JsonPropertyDescription("Additional context announced after the label. Maps to accessibilityHint (iOS), contentDescription suffix (Android), aria-describedby text (Web).")
    private String hint;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Human-readable label announced by screen readers. Omit for elements whose text content is self-describing.
     * 
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    /**
     * Human-readable label announced by screen readers. Omit for elements whose text content is self-describing.
     * 
     */
    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public AccessibilityProperties withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     * 
     */
    @JsonProperty("role")
    public AccessibilityProperties.Role getRole() {
        return role;
    }

    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     * 
     */
    @JsonProperty("role")
    public void setRole(AccessibilityProperties.Role role) {
        this.role = role;
    }

    public AccessibilityProperties withRole(AccessibilityProperties.Role role) {
        this.role = role;
        return this;
    }

    /**
     * When true, element and its descendants are hidden from the accessibility tree (decorative content).
     * 
     */
    @JsonProperty("hidden")
    public Boolean getHidden() {
        return hidden;
    }

    /**
     * When true, element and its descendants are hidden from the accessibility tree (decorative content).
     * 
     */
    @JsonProperty("hidden")
    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public AccessibilityProperties withHidden(Boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    /**
     * Heading level (1-6) for role=heading elements. Maps to aria-level (Web), accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
     * 
     */
    @JsonProperty("headingLevel")
    public Integer getHeadingLevel() {
        return headingLevel;
    }

    /**
     * Heading level (1-6) for role=heading elements. Maps to aria-level (Web), accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
     * 
     */
    @JsonProperty("headingLevel")
    public void setHeadingLevel(Integer headingLevel) {
        this.headingLevel = headingLevel;
    }

    public AccessibilityProperties withHeadingLevel(Integer headingLevel) {
        this.headingLevel = headingLevel;
        return this;
    }

    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     * 
     */
    @JsonProperty("liveRegion")
    public AccessibilityProperties.LiveRegion getLiveRegion() {
        return liveRegion;
    }

    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     * 
     */
    @JsonProperty("liveRegion")
    public void setLiveRegion(AccessibilityProperties.LiveRegion liveRegion) {
        this.liveRegion = liveRegion;
    }

    public AccessibilityProperties withLiveRegion(AccessibilityProperties.LiveRegion liveRegion) {
        this.liveRegion = liveRegion;
        return this;
    }

    /**
     * Override default accessibility traversal order. Lower values are visited first. Omit to use natural DOM/view order.
     * 
     */
    @JsonProperty("sortOrder")
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * Override default accessibility traversal order. Lower values are visited first. Omit to use natural DOM/view order.
     * 
     */
    @JsonProperty("sortOrder")
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public AccessibilityProperties withSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    /**
     * Additional context announced after the label. Maps to accessibilityHint (iOS), contentDescription suffix (Android), aria-describedby text (Web).
     * 
     */
    @JsonProperty("hint")
    public String getHint() {
        return hint;
    }

    /**
     * Additional context announced after the label. Maps to accessibilityHint (iOS), contentDescription suffix (Android), aria-describedby text (Web).
     * 
     */
    @JsonProperty("hint")
    public void setHint(String hint) {
        this.hint = hint;
    }

    public AccessibilityProperties withHint(String hint) {
        this.hint = hint;
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

    public AccessibilityProperties withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AccessibilityProperties.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("role");
        sb.append('=');
        sb.append(((this.role == null)?"<null>":this.role));
        sb.append(',');
        sb.append("hidden");
        sb.append('=');
        sb.append(((this.hidden == null)?"<null>":this.hidden));
        sb.append(',');
        sb.append("headingLevel");
        sb.append('=');
        sb.append(((this.headingLevel == null)?"<null>":this.headingLevel));
        sb.append(',');
        sb.append("liveRegion");
        sb.append('=');
        sb.append(((this.liveRegion == null)?"<null>":this.liveRegion));
        sb.append(',');
        sb.append("sortOrder");
        sb.append('=');
        sb.append(((this.sortOrder == null)?"<null>":this.sortOrder));
        sb.append(',');
        sb.append("hint");
        sb.append('=');
        sb.append(((this.hint == null)?"<null>":this.hint));
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
        result = ((result* 31)+((this.role == null)? 0 :this.role.hashCode()));
        result = ((result* 31)+((this.hidden == null)? 0 :this.hidden.hashCode()));
        result = ((result* 31)+((this.sortOrder == null)? 0 :this.sortOrder.hashCode()));
        result = ((result* 31)+((this.hint == null)? 0 :this.hint.hashCode()));
        result = ((result* 31)+((this.liveRegion == null)? 0 :this.liveRegion.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.headingLevel == null)? 0 :this.headingLevel.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AccessibilityProperties) == false) {
            return false;
        }
        AccessibilityProperties rhs = ((AccessibilityProperties) other);
        return (((((((((this.role == rhs.role)||((this.role!= null)&&this.role.equals(rhs.role)))&&((this.hidden == rhs.hidden)||((this.hidden!= null)&&this.hidden.equals(rhs.hidden))))&&((this.sortOrder == rhs.sortOrder)||((this.sortOrder!= null)&&this.sortOrder.equals(rhs.sortOrder))))&&((this.hint == rhs.hint)||((this.hint!= null)&&this.hint.equals(rhs.hint))))&&((this.liveRegion == rhs.liveRegion)||((this.liveRegion!= null)&&this.liveRegion.equals(rhs.liveRegion))))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.headingLevel == rhs.headingLevel)||((this.headingLevel!= null)&&this.headingLevel.equals(rhs.headingLevel))));
    }


    /**
     * Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
     * 
     */
    @Generated("jsonschema2pojo")
    public enum LiveRegion {

        POLITE("polite"),
        ASSERTIVE("assertive"),
        OFF("off");
        private final String value;
        private final static Map<String, AccessibilityProperties.LiveRegion> CONSTANTS = new HashMap<String, AccessibilityProperties.LiveRegion>();

        static {
            for (AccessibilityProperties.LiveRegion c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        LiveRegion(String value) {
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
        public static AccessibilityProperties.LiveRegion fromValue(String value) {
            AccessibilityProperties.LiveRegion constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Semantic role override. 'none' suppresses the element's intrinsic role.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Role {

        BUTTON("button"),
        IMAGE("image"),
        HEADING("heading"),
        LINK("link"),
        TAB("tab"),
        TABPANEL("tabpanel"),
        LIST("list"),
        LISTITEM("listitem"),
        TABLE("table"),
        ROW("row"),
        CELL("cell"),
        NONE("none");
        private final String value;
        private final static Map<String, AccessibilityProperties.Role> CONSTANTS = new HashMap<String, AccessibilityProperties.Role>();

        static {
            for (AccessibilityProperties.Role c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Role(String value) {
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
        public static AccessibilityProperties.Role fromValue(String value) {
            AccessibilityProperties.Role constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
