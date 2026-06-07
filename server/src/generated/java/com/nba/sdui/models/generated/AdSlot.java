
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Ad placement primitive — carries placement semantics while delegating auction/targeting to ad-platform SDKs (see ADR-007)
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "provider",
    "adUnitPath",
    "sizes",
    "targeting",
    "collapseOnEmpty",
    "refreshIntervalSec",
    "label",
    "placeholder"
})
@Generated("jsonschema2pojo")
public class AdSlot {

    /**
     * Ad network identifier, e.g. 'gam', 'amazon'
     * (Required)
     * 
     */
    @JsonProperty("provider")
    @JsonPropertyDescription("Ad network identifier, e.g. 'gam', 'amazon'")
    @NotNull
    private String provider;
    /**
     * Ad unit path used by the ad SDK
     * (Required)
     * 
     */
    @JsonProperty("adUnitPath")
    @JsonPropertyDescription("Ad unit path used by the ad SDK")
    @NotNull
    private String adUnitPath;
    /**
     * Accepted creative sizes as [width, height] pairs
     * (Required)
     * 
     */
    @JsonProperty("sizes")
    @JsonPropertyDescription("Accepted creative sizes as [width, height] pairs")
    @Valid
    @NotNull
    private List<List<Integer>> sizes = new ArrayList<List<Integer>>();
    /**
     * Key-value targeting hints passed to ad SDK
     * 
     */
    @JsonProperty("targeting")
    @JsonPropertyDescription("Key-value targeting hints passed to ad SDK")
    @Valid
    private Targeting targeting;
    /**
     * Whether to collapse the slot when no fill is returned
     * 
     */
    @JsonProperty("collapseOnEmpty")
    @JsonPropertyDescription("Whether to collapse the slot when no fill is returned")
    private Boolean collapseOnEmpty = true;
    /**
     * Optional auto-refresh interval in seconds
     * 
     */
    @JsonProperty("refreshIntervalSec")
    @JsonPropertyDescription("Optional auto-refresh interval in seconds")
    private Integer refreshIntervalSec;
    /**
     * Disclosure label displayed above/below the ad
     * 
     */
    @JsonProperty("label")
    @JsonPropertyDescription("Disclosure label displayed above/below the ad")
    private String label = "Advertisement";
    /**
     * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own width/height. Required so the stub renderer has no client-side chrome defaults.
     * 
     */
    @JsonProperty("placeholder")
    @JsonPropertyDescription("Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses when collapseOnEmpty is false). Shares dimensions with sizes[0] \u2014 never carries its own width/height. Required so the stub renderer has no client-side chrome defaults.")
    @Valid
    private Placeholder placeholder;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Ad network identifier, e.g. 'gam', 'amazon'
     * (Required)
     * 
     */
    @JsonProperty("provider")
    public String getProvider() {
        return provider;
    }

    /**
     * Ad network identifier, e.g. 'gam', 'amazon'
     * (Required)
     * 
     */
    @JsonProperty("provider")
    public void setProvider(String provider) {
        this.provider = provider;
    }

    public AdSlot withProvider(String provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Ad unit path used by the ad SDK
     * (Required)
     * 
     */
    @JsonProperty("adUnitPath")
    public String getAdUnitPath() {
        return adUnitPath;
    }

    /**
     * Ad unit path used by the ad SDK
     * (Required)
     * 
     */
    @JsonProperty("adUnitPath")
    public void setAdUnitPath(String adUnitPath) {
        this.adUnitPath = adUnitPath;
    }

    public AdSlot withAdUnitPath(String adUnitPath) {
        this.adUnitPath = adUnitPath;
        return this;
    }

    /**
     * Accepted creative sizes as [width, height] pairs
     * (Required)
     * 
     */
    @JsonProperty("sizes")
    public List<List<Integer>> getSizes() {
        return sizes;
    }

    /**
     * Accepted creative sizes as [width, height] pairs
     * (Required)
     * 
     */
    @JsonProperty("sizes")
    public void setSizes(List<List<Integer>> sizes) {
        this.sizes = sizes;
    }

    public AdSlot withSizes(List<List<Integer>> sizes) {
        this.sizes = sizes;
        return this;
    }

    /**
     * Key-value targeting hints passed to ad SDK
     * 
     */
    @JsonProperty("targeting")
    public Targeting getTargeting() {
        return targeting;
    }

    /**
     * Key-value targeting hints passed to ad SDK
     * 
     */
    @JsonProperty("targeting")
    public void setTargeting(Targeting targeting) {
        this.targeting = targeting;
    }

    public AdSlot withTargeting(Targeting targeting) {
        this.targeting = targeting;
        return this;
    }

    /**
     * Whether to collapse the slot when no fill is returned
     * 
     */
    @JsonProperty("collapseOnEmpty")
    public Boolean getCollapseOnEmpty() {
        return collapseOnEmpty;
    }

    /**
     * Whether to collapse the slot when no fill is returned
     * 
     */
    @JsonProperty("collapseOnEmpty")
    public void setCollapseOnEmpty(Boolean collapseOnEmpty) {
        this.collapseOnEmpty = collapseOnEmpty;
    }

    public AdSlot withCollapseOnEmpty(Boolean collapseOnEmpty) {
        this.collapseOnEmpty = collapseOnEmpty;
        return this;
    }

    /**
     * Optional auto-refresh interval in seconds
     * 
     */
    @JsonProperty("refreshIntervalSec")
    public Integer getRefreshIntervalSec() {
        return refreshIntervalSec;
    }

    /**
     * Optional auto-refresh interval in seconds
     * 
     */
    @JsonProperty("refreshIntervalSec")
    public void setRefreshIntervalSec(Integer refreshIntervalSec) {
        this.refreshIntervalSec = refreshIntervalSec;
    }

    public AdSlot withRefreshIntervalSec(Integer refreshIntervalSec) {
        this.refreshIntervalSec = refreshIntervalSec;
        return this;
    }

    /**
     * Disclosure label displayed above/below the ad
     * 
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    /**
     * Disclosure label displayed above/below the ad
     * 
     */
    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public AdSlot withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own width/height. Required so the stub renderer has no client-side chrome defaults.
     * 
     */
    @JsonProperty("placeholder")
    public Placeholder getPlaceholder() {
        return placeholder;
    }

    /**
     * Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own width/height. Required so the stub renderer has no client-side chrome defaults.
     * 
     */
    @JsonProperty("placeholder")
    public void setPlaceholder(Placeholder placeholder) {
        this.placeholder = placeholder;
    }

    public AdSlot withPlaceholder(Placeholder placeholder) {
        this.placeholder = placeholder;
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

    public AdSlot withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AdSlot.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("provider");
        sb.append('=');
        sb.append(((this.provider == null)?"<null>":this.provider));
        sb.append(',');
        sb.append("adUnitPath");
        sb.append('=');
        sb.append(((this.adUnitPath == null)?"<null>":this.adUnitPath));
        sb.append(',');
        sb.append("sizes");
        sb.append('=');
        sb.append(((this.sizes == null)?"<null>":this.sizes));
        sb.append(',');
        sb.append("targeting");
        sb.append('=');
        sb.append(((this.targeting == null)?"<null>":this.targeting));
        sb.append(',');
        sb.append("collapseOnEmpty");
        sb.append('=');
        sb.append(((this.collapseOnEmpty == null)?"<null>":this.collapseOnEmpty));
        sb.append(',');
        sb.append("refreshIntervalSec");
        sb.append('=');
        sb.append(((this.refreshIntervalSec == null)?"<null>":this.refreshIntervalSec));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("placeholder");
        sb.append('=');
        sb.append(((this.placeholder == null)?"<null>":this.placeholder));
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
        result = ((result* 31)+((this.refreshIntervalSec == null)? 0 :this.refreshIntervalSec.hashCode()));
        result = ((result* 31)+((this.targeting == null)? 0 :this.targeting.hashCode()));
        result = ((result* 31)+((this.sizes == null)? 0 :this.sizes.hashCode()));
        result = ((result* 31)+((this.provider == null)? 0 :this.provider.hashCode()));
        result = ((result* 31)+((this.adUnitPath == null)? 0 :this.adUnitPath.hashCode()));
        result = ((result* 31)+((this.collapseOnEmpty == null)? 0 :this.collapseOnEmpty.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.placeholder == null)? 0 :this.placeholder.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AdSlot) == false) {
            return false;
        }
        AdSlot rhs = ((AdSlot) other);
        return ((((((((((this.refreshIntervalSec == rhs.refreshIntervalSec)||((this.refreshIntervalSec!= null)&&this.refreshIntervalSec.equals(rhs.refreshIntervalSec)))&&((this.targeting == rhs.targeting)||((this.targeting!= null)&&this.targeting.equals(rhs.targeting))))&&((this.sizes == rhs.sizes)||((this.sizes!= null)&&this.sizes.equals(rhs.sizes))))&&((this.provider == rhs.provider)||((this.provider!= null)&&this.provider.equals(rhs.provider))))&&((this.adUnitPath == rhs.adUnitPath)||((this.adUnitPath!= null)&&this.adUnitPath.equals(rhs.adUnitPath))))&&((this.collapseOnEmpty == rhs.collapseOnEmpty)||((this.collapseOnEmpty!= null)&&this.collapseOnEmpty.equals(rhs.collapseOnEmpty))))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.placeholder == rhs.placeholder)||((this.placeholder!= null)&&this.placeholder.equals(rhs.placeholder))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
