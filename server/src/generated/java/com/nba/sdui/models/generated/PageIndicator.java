
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
import jakarta.validation.constraints.Pattern;


/**
 * Server-declared scroll page indicator presentation for ScrollContainer. Clients own local scroll state only to realize the declared affordance.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "style",
    "alignment",
    "color",
    "activeColor"
})
@Generated("jsonschema2pojo")
public class PageIndicator {

    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal bar segments.
     * (Required)
     * 
     */
    @JsonProperty("style")
    @JsonPropertyDescription("Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal bar segments.")
    @NotNull
    private PageIndicator.Style style;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("alignment")
    @NotNull
    private com.nba.sdui.models.generated.Badge.BadgeAlignment alignment = com.nba.sdui.models.generated.Badge.BadgeAlignment.fromValue("bottomEnd");
    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("color")
    @JsonPropertyDescription("A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|token:[A-Za-z0-9][A-Za-z0-9_.-]*)$")
    private String color;
    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("activeColor")
    @JsonPropertyDescription("A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.")
    @Pattern(regexp = "^(#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}|token:[A-Za-z0-9][A-Za-z0-9_.-]*)$")
    private String activeColor;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal bar segments.
     * (Required)
     * 
     */
    @JsonProperty("style")
    public PageIndicator.Style getStyle() {
        return style;
    }

    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal bar segments.
     * (Required)
     * 
     */
    @JsonProperty("style")
    public void setStyle(PageIndicator.Style style) {
        this.style = style;
    }

    public PageIndicator withStyle(PageIndicator.Style style) {
        this.style = style;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("alignment")
    public com.nba.sdui.models.generated.Badge.BadgeAlignment getAlignment() {
        return alignment;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("alignment")
    public void setAlignment(com.nba.sdui.models.generated.Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
    }

    public PageIndicator withAlignment(com.nba.sdui.models.generated.Badge.BadgeAlignment alignment) {
        this.alignment = alignment;
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

    public PageIndicator withColor(String color) {
        this.color = color;
        return this;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("activeColor")
    public String getActiveColor() {
        return activeColor;
    }

    /**
     * A color value on the wire. Either a literal hex color (#RRGGBB or #RRGGBBAA) or a semantic token reference of the form 'token:<dot.separated.path>' (e.g. 'token:nba.color.primary.50', 'token:nba.label.accent.brand'). Clients resolve token references at render time against their bundled color registry, picking a light or dark value based on the OS color scheme. Unknown tokens log 'token_resolver_missing' and fall back to the caller's default.
     * 
     */
    @JsonProperty("activeColor")
    public void setActiveColor(String activeColor) {
        this.activeColor = activeColor;
    }

    public PageIndicator withActiveColor(String activeColor) {
        this.activeColor = activeColor;
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

    public PageIndicator withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(PageIndicator.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("style");
        sb.append('=');
        sb.append(((this.style == null)?"<null>":this.style));
        sb.append(',');
        sb.append("alignment");
        sb.append('=');
        sb.append(((this.alignment == null)?"<null>":this.alignment));
        sb.append(',');
        sb.append("color");
        sb.append('=');
        sb.append(((this.color == null)?"<null>":this.color));
        sb.append(',');
        sb.append("activeColor");
        sb.append('=');
        sb.append(((this.activeColor == null)?"<null>":this.activeColor));
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
        result = ((result* 31)+((this.style == null)? 0 :this.style.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.alignment == null)? 0 :this.alignment.hashCode()));
        result = ((result* 31)+((this.color == null)? 0 :this.color.hashCode()));
        result = ((result* 31)+((this.activeColor == null)? 0 :this.activeColor.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof PageIndicator) == false) {
            return false;
        }
        PageIndicator rhs = ((PageIndicator) other);
        return ((((((this.style == rhs.style)||((this.style!= null)&&this.style.equals(rhs.style)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.alignment == rhs.alignment)||((this.alignment!= null)&&this.alignment.equals(rhs.alignment))))&&((this.color == rhs.color)||((this.color!= null)&&this.color.equals(rhs.color))))&&((this.activeColor == rhs.activeColor)||((this.activeColor!= null)&&this.activeColor.equals(rhs.activeColor))));
    }


    /**
     * Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal bar segments.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Style {

        DOTS("dots"),
        DASHES("dashes");
        private final String value;
        private final static Map<String, PageIndicator.Style> CONSTANTS = new HashMap<String, PageIndicator.Style>();

        static {
            for (PageIndicator.Style c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Style(String value) {
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
        public static PageIndicator.Style fromValue(String value) {
            PageIndicator.Style constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
