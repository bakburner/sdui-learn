
package com.nba.sdui.models.generated;

import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Per-corner cornerRadius override. When present, takes precedence over the single-value cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius / borderBottomRightRadius.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "topStart",
    "topEnd",
    "bottomStart",
    "bottomEnd"
})
@Generated("jsonschema2pojo")
public class CornerRadii {

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topStart")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object topStart;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topEnd")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object topEnd;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomStart")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object bottomStart;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomEnd")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object bottomEnd;

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topStart")
    public Object getTopStart() {
        return topStart;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topStart")
    public void setTopStart(Object topStart) {
        this.topStart = topStart;
    }

    public CornerRadii withTopStart(Object topStart) {
        this.topStart = topStart;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topEnd")
    public Object getTopEnd() {
        return topEnd;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("topEnd")
    public void setTopEnd(Object topEnd) {
        this.topEnd = topEnd;
    }

    public CornerRadii withTopEnd(Object topEnd) {
        this.topEnd = topEnd;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomStart")
    public Object getBottomStart() {
        return bottomStart;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomStart")
    public void setBottomStart(Object bottomStart) {
        this.bottomStart = bottomStart;
    }

    public CornerRadii withBottomStart(Object bottomStart) {
        this.bottomStart = bottomStart;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomEnd")
    public Object getBottomEnd() {
        return bottomEnd;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("bottomEnd")
    public void setBottomEnd(Object bottomEnd) {
        this.bottomEnd = bottomEnd;
    }

    public CornerRadii withBottomEnd(Object bottomEnd) {
        this.bottomEnd = bottomEnd;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CornerRadii.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("topStart");
        sb.append('=');
        sb.append(((this.topStart == null)?"<null>":this.topStart));
        sb.append(',');
        sb.append("topEnd");
        sb.append('=');
        sb.append(((this.topEnd == null)?"<null>":this.topEnd));
        sb.append(',');
        sb.append("bottomStart");
        sb.append('=');
        sb.append(((this.bottomStart == null)?"<null>":this.bottomStart));
        sb.append(',');
        sb.append("bottomEnd");
        sb.append('=');
        sb.append(((this.bottomEnd == null)?"<null>":this.bottomEnd));
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
        result = ((result* 31)+((this.bottomEnd == null)? 0 :this.bottomEnd.hashCode()));
        result = ((result* 31)+((this.topEnd == null)? 0 :this.topEnd.hashCode()));
        result = ((result* 31)+((this.bottomStart == null)? 0 :this.bottomStart.hashCode()));
        result = ((result* 31)+((this.topStart == null)? 0 :this.topStart.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CornerRadii) == false) {
            return false;
        }
        CornerRadii rhs = ((CornerRadii) other);
        return (((((this.bottomEnd == rhs.bottomEnd)||((this.bottomEnd!= null)&&this.bottomEnd.equals(rhs.bottomEnd)))&&((this.topEnd == rhs.topEnd)||((this.topEnd!= null)&&this.topEnd.equals(rhs.topEnd))))&&((this.bottomStart == rhs.bottomStart)||((this.bottomStart!= null)&&this.bottomStart.equals(rhs.bottomStart))))&&((this.topStart == rhs.topStart)||((this.topStart!= null)&&this.topStart.equals(rhs.topStart))));
    }

}
