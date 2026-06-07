
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
import jakarta.validation.constraints.Pattern;


/**
 * Outer stroke applied around a container or section.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "width",
    "color"
})
@Generated("jsonschema2pojo")
public class Border {

    /**
     * Stroke width in dp/px
     * 
     */
    @JsonProperty("width")
    @JsonPropertyDescription("Stroke width in dp/px")
    private Double width = 1.0D;
    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    @JsonPropertyDescription("A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|token:[A-Za-z0-9][A-Za-z0-9_.-]*)$")
    private String color;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Stroke width in dp/px
     * 
     */
    @JsonProperty("width")
    public Double getWidth() {
        return width;
    }

    /**
     * Stroke width in dp/px
     * 
     */
    @JsonProperty("width")
    public void setWidth(Double width) {
        this.width = width;
    }

    public Border withWidth(Double width) {
        this.width = width;
        return this;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    public void setColor(String color) {
        this.color = color;
    }

    public Border withColor(String color) {
        this.color = color;
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

    public Border withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Border.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("width");
        sb.append('=');
        sb.append(((this.width == null)?"<null>":this.width));
        sb.append(',');
        sb.append("color");
        sb.append('=');
        sb.append(((this.color == null)?"<null>":this.color));
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
        result = ((result* 31)+((this.width == null)? 0 :this.width.hashCode()));
        result = ((result* 31)+((this.color == null)? 0 :this.color.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Border) == false) {
            return false;
        }
        Border rhs = ((Border) other);
        return ((((this.width == rhs.width)||((this.width!= null)&&this.width.equals(rhs.width)))&&((this.color == rhs.color)||((this.color!= null)&&this.color.equals(rhs.color))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
