
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "top",
    "bottom",
    "start",
    "end"
})
@Generated("jsonschema2pojo")
public class Spacing {

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("top")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object top;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottom")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object bottom;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("start")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object start;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("end")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object end;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("top")
    public Object getTop() {
        return top;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("top")
    public void setTop(Object top) {
        this.top = top;
    }

    public Spacing withTop(Object top) {
        this.top = top;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottom")
    public Object getBottom() {
        return bottom;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottom")
    public void setBottom(Object bottom) {
        this.bottom = bottom;
    }

    public Spacing withBottom(Object bottom) {
        this.bottom = bottom;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("start")
    public Object getStart() {
        return start;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("start")
    public void setStart(Object start) {
        this.start = start;
    }

    public Spacing withStart(Object start) {
        this.start = start;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("end")
    public Object getEnd() {
        return end;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("end")
    public void setEnd(Object end) {
        this.end = end;
    }

    public Spacing withEnd(Object end) {
        this.end = end;
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

    public Spacing withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Spacing.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("top");
        sb.append('=');
        sb.append(((this.top == null)?"<null>":this.top));
        sb.append(',');
        sb.append("bottom");
        sb.append('=');
        sb.append(((this.bottom == null)?"<null>":this.bottom));
        sb.append(',');
        sb.append("start");
        sb.append('=');
        sb.append(((this.start == null)?"<null>":this.start));
        sb.append(',');
        sb.append("end");
        sb.append('=');
        sb.append(((this.end == null)?"<null>":this.end));
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
        result = ((result* 31)+((this.start == null)? 0 :this.start.hashCode()));
        result = ((result* 31)+((this.end == null)? 0 :this.end.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.top == null)? 0 :this.top.hashCode()));
        result = ((result* 31)+((this.bottom == null)? 0 :this.bottom.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Spacing) == false) {
            return false;
        }
        Spacing rhs = ((Spacing) other);
        return ((((((this.start == rhs.start)||((this.start!= null)&&this.start.equals(rhs.start)))&&((this.end == rhs.end)||((this.end!= null)&&this.end.equals(rhs.end))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.top == rhs.top)||((this.top!= null)&&this.top.equals(rhs.top))))&&((this.bottom == rhs.bottom)||((this.bottom!= null)&&this.bottom.equals(rhs.bottom))));
    }

}
