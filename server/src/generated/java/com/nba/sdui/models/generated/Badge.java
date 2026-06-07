
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
import jakarta.validation.constraints.NotNull;


/**
 * Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the screen-level overlays map.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "element",
    "alignment"
})
@Generated("jsonschema2pojo")
public class Badge {

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("element")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    @NotNull
    private AtomicElement element;
    @JsonProperty("alignment")
    private Badge.BadgeAlignment alignment = Badge.BadgeAlignment.fromValue("bottomEnd");
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("element")
    public AtomicElement getElement() {
        return element;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("element")
    public void setElement(AtomicElement element) {
        this.element = element;
    }

    public Badge withElement(AtomicElement element) {
        this.element = element;
        return this;
    }

    @JsonProperty("alignment")
    public Badge.BadgeAlignment getAlignment() {
        return alignment;
    }

    @JsonProperty("alignment")
    public void setAlignment(Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
    }

    public Badge withAlignment(Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
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

    public Badge withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Badge.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("element");
        sb.append('=');
        sb.append(((this.element == null)?"<null>":this.element));
        sb.append(',');
        sb.append("alignment");
        sb.append('=');
        sb.append(((this.alignment == null)?"<null>":this.alignment));
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
        result = ((result* 31)+((this.alignment == null)? 0 :this.alignment.hashCode()));
        result = ((result* 31)+((this.element == null)? 0 :this.element.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Badge) == false) {
            return false;
        }
        Badge rhs = ((Badge) other);
        return ((((this.alignment == rhs.alignment)||((this.alignment!= null)&&this.alignment.equals(rhs.alignment)))&&((this.element == rhs.element)||((this.element!= null)&&this.element.equals(rhs.element))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

    @Generated("jsonschema2pojo")
    public enum BadgeAlignment {

        TOP_START("topStart"),
        TOP_CENTER("topCenter"),
        TOP_END("topEnd"),
        CENTER_START("centerStart"),
        CENTER("center"),
        CENTER_END("centerEnd"),
        BOTTOM_START("bottomStart"),
        BOTTOM_CENTER("bottomCenter"),
        BOTTOM_END("bottomEnd");
        private final String value;
        private final static Map<String, Badge.BadgeAlignment> CONSTANTS = new HashMap<String, Badge.BadgeAlignment>();

        static {
            for (Badge.BadgeAlignment c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        BadgeAlignment(String value) {
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
        public static Badge.BadgeAlignment fromValue(String value) {
            Badge.BadgeAlignment constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
