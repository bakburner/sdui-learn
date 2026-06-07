
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
 * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own width/height. Required so the stub renderer has no client-side chrome defaults.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "backgroundColor",
    "text"
})
@Generated("jsonschema2pojo")
public class Placeholder {

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("backgroundColor")
    @JsonPropertyDescription("A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|token:[A-Za-z0-9][A-Za-z0-9_.-]*)$")
    private String backgroundColor;
    /**
     * Caption rendered inside the empty rectangle (e.g. 'Advertisement').
     * 
     */
    @JsonProperty("text")
    @JsonPropertyDescription("Caption rendered inside the empty rectangle (e.g. 'Advertisement').")
    private String text;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("backgroundColor")
    public String getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("backgroundColor")
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Placeholder withBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    /**
     * Caption rendered inside the empty rectangle (e.g. 'Advertisement').
     * 
     */
    @JsonProperty("text")
    public String getText() {
        return text;
    }

    /**
     * Caption rendered inside the empty rectangle (e.g. 'Advertisement').
     * 
     */
    @JsonProperty("text")
    public void setText(String text) {
        this.text = text;
    }

    public Placeholder withText(String text) {
        this.text = text;
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

    public Placeholder withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Placeholder.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("backgroundColor");
        sb.append('=');
        sb.append(((this.backgroundColor == null)?"<null>":this.backgroundColor));
        sb.append(',');
        sb.append("text");
        sb.append('=');
        sb.append(((this.text == null)?"<null>":this.text));
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
        result = ((result* 31)+((this.backgroundColor == null)? 0 :this.backgroundColor.hashCode()));
        result = ((result* 31)+((this.text == null)? 0 :this.text.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Placeholder) == false) {
            return false;
        }
        Placeholder rhs = ((Placeholder) other);
        return ((((this.backgroundColor == rhs.backgroundColor)||((this.backgroundColor!= null)&&this.backgroundColor.equals(rhs.backgroundColor)))&&((this.text == rhs.text)||((this.text!= null)&&this.text.equals(rhs.text))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
