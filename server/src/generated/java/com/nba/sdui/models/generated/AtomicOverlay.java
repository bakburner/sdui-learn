
package com.nba.sdui.models.generated;

import java.util.LinkedHashMap;
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


/**
 * One server-composed overlay layer positioned over an OverlayContainer base element.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "alignment",
    "inset",
    "element"
})
@Generated("jsonschema2pojo")
public class AtomicOverlay {

    @JsonProperty("alignment")
    private com.nba.sdui.models.generated.Badge.BadgeAlignment alignment = com.nba.sdui.models.generated.Badge.BadgeAlignment.fromValue("bottomEnd");
    @JsonProperty("inset")
    @Valid
    private Spacing inset;
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
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("alignment")
    public com.nba.sdui.models.generated.Badge.BadgeAlignment getAlignment() {
        return alignment;
    }

    @JsonProperty("alignment")
    public void setAlignment(com.nba.sdui.models.generated.Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
    }

    public AtomicOverlay withAlignment(com.nba.sdui.models.generated.Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @JsonProperty("inset")
    public Spacing getInset() {
        return inset;
    }

    @JsonProperty("inset")
    public void setInset(Spacing inset) {
        this.inset = inset;
    }

    public AtomicOverlay withInset(Spacing inset) {
        this.inset = inset;
        return this;
    }

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

    public AtomicOverlay withElement(AtomicElement element) {
        this.element = element;
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

    public AtomicOverlay withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AtomicOverlay.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("alignment");
        sb.append('=');
        sb.append(((this.alignment == null)?"<null>":this.alignment));
        sb.append(',');
        sb.append("inset");
        sb.append('=');
        sb.append(((this.inset == null)?"<null>":this.inset));
        sb.append(',');
        sb.append("element");
        sb.append('=');
        sb.append(((this.element == null)?"<null>":this.element));
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
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.alignment == null)? 0 :this.alignment.hashCode()));
        result = ((result* 31)+((this.inset == null)? 0 :this.inset.hashCode()));
        result = ((result* 31)+((this.element == null)? 0 :this.element.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AtomicOverlay) == false) {
            return false;
        }
        AtomicOverlay rhs = ((AtomicOverlay) other);
        return (((((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties)))&&((this.alignment == rhs.alignment)||((this.alignment!= null)&&this.alignment.equals(rhs.alignment))))&&((this.inset == rhs.inset)||((this.inset!= null)&&this.inset.equals(rhs.inset))))&&((this.element == rhs.element)||((this.element!= null)&&this.element.equals(rhs.element))));
    }

}
