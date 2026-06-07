
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


/**
 * Server-driven surface spec applied by the client's SectionRouter to every semantic section — the visual wrapper beneath the section's content. Mirrors the inline-chrome vocabulary on AtomicContainer so semantic sections have schema parity with composed sections. Every client's shared SectionContainer wrapper reads these fields; semantic-section renderers do not set outer padding, margin, corner radius, shadow, border, or background themselves. The sibling `data` field carries content (including the atomic UI tree); `surface` carries the frame that sits beneath it.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "margin",
    "padding",
    "background",
    "cornerRadius",
    "shadow",
    "border"
})
@Generated("jsonschema2pojo")
public class SectionSurface {

    @JsonProperty("margin")
    @Valid
    private Spacing margin;
    @JsonProperty("padding")
    @Valid
    private Spacing padding;
    /**
     * Shared background type — solid color, gradient, or image with overlay
     * 
     */
    @JsonProperty("background")
    @JsonPropertyDescription("Shared background type \u2014 solid color, gradient, or image with overlay")
    private Object background;
    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    @JsonPropertyDescription("Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).")
    private Object cornerRadius;
    /**
     * Either a full Shadow struct or a shorthand token. Clients expand shorthand tokens to the full Shadow struct at resolve time.
     * 
     */
    @JsonProperty("shadow")
    @JsonPropertyDescription("Either a full Shadow struct or a shorthand token. Clients expand shorthand tokens to the full Shadow struct at resolve time.")
    private Object shadow;
    /**
     * Outer stroke applied around a container or section.
     * 
     */
    @JsonProperty("border")
    @JsonPropertyDescription("Outer stroke applied around a container or section.")
    @Valid
    private Border border;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("margin")
    public Spacing getMargin() {
        return margin;
    }

    @JsonProperty("margin")
    public void setMargin(Spacing margin) {
        this.margin = margin;
    }

    public SectionSurface withMargin(Spacing margin) {
        this.margin = margin;
        return this;
    }

    @JsonProperty("padding")
    public Spacing getPadding() {
        return padding;
    }

    @JsonProperty("padding")
    public void setPadding(Spacing padding) {
        this.padding = padding;
    }

    public SectionSurface withPadding(Spacing padding) {
        this.padding = padding;
        return this;
    }

    /**
     * Shared background type — solid color, gradient, or image with overlay
     * 
     */
    @JsonProperty("background")
    public Object getBackground() {
        return background;
    }

    /**
     * Shared background type — solid color, gradient, or image with overlay
     * 
     */
    @JsonProperty("background")
    public void setBackground(Object background) {
        this.background = background;
    }

    public SectionSurface withBackground(Object background) {
        this.background = background;
        return this;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    public Object getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Absolute layout value: raw dp/px integer, or a semantic layout token reference token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per platform.formFactor against bundled spacing/corner/size/typography/shadow registries. Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
     * 
     */
    @JsonProperty("cornerRadius")
    public void setCornerRadius(Object cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public SectionSurface withCornerRadius(Object cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /**
     * Either a full Shadow struct or a shorthand token. Clients expand shorthand tokens to the full Shadow struct at resolve time.
     * 
     */
    @JsonProperty("shadow")
    public Object getShadow() {
        return shadow;
    }

    /**
     * Either a full Shadow struct or a shorthand token. Clients expand shorthand tokens to the full Shadow struct at resolve time.
     * 
     */
    @JsonProperty("shadow")
    public void setShadow(Object shadow) {
        this.shadow = shadow;
    }

    public SectionSurface withShadow(Object shadow) {
        this.shadow = shadow;
        return this;
    }

    /**
     * Outer stroke applied around a container or section.
     * 
     */
    @JsonProperty("border")
    public Border getBorder() {
        return border;
    }

    /**
     * Outer stroke applied around a container or section.
     * 
     */
    @JsonProperty("border")
    public void setBorder(Border border) {
        this.border = border;
    }

    public SectionSurface withBorder(Border border) {
        this.border = border;
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

    public SectionSurface withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SectionSurface.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("margin");
        sb.append('=');
        sb.append(((this.margin == null)?"<null>":this.margin));
        sb.append(',');
        sb.append("padding");
        sb.append('=');
        sb.append(((this.padding == null)?"<null>":this.padding));
        sb.append(',');
        sb.append("background");
        sb.append('=');
        sb.append(((this.background == null)?"<null>":this.background));
        sb.append(',');
        sb.append("cornerRadius");
        sb.append('=');
        sb.append(((this.cornerRadius == null)?"<null>":this.cornerRadius));
        sb.append(',');
        sb.append("shadow");
        sb.append('=');
        sb.append(((this.shadow == null)?"<null>":this.shadow));
        sb.append(',');
        sb.append("border");
        sb.append('=');
        sb.append(((this.border == null)?"<null>":this.border));
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
        result = ((result* 31)+((this.border == null)? 0 :this.border.hashCode()));
        result = ((result* 31)+((this.padding == null)? 0 :this.padding.hashCode()));
        result = ((result* 31)+((this.margin == null)? 0 :this.margin.hashCode()));
        result = ((result* 31)+((this.shadow == null)? 0 :this.shadow.hashCode()));
        result = ((result* 31)+((this.background == null)? 0 :this.background.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.cornerRadius == null)? 0 :this.cornerRadius.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SectionSurface) == false) {
            return false;
        }
        SectionSurface rhs = ((SectionSurface) other);
        return ((((((((this.border == rhs.border)||((this.border!= null)&&this.border.equals(rhs.border)))&&((this.padding == rhs.padding)||((this.padding!= null)&&this.padding.equals(rhs.padding))))&&((this.margin == rhs.margin)||((this.margin!= null)&&this.margin.equals(rhs.margin))))&&((this.shadow == rhs.shadow)||((this.shadow!= null)&&this.shadow.equals(rhs.shadow))))&&((this.background == rhs.background)||((this.background!= null)&&this.background.equals(rhs.background))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.cornerRadius == rhs.cornerRadius)||((this.cornerRadius!= null)&&this.cornerRadius.equals(rhs.cornerRadius))));
    }

}
