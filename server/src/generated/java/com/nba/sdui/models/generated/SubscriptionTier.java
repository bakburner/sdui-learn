
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "price",
    "originalPrice",
    "badgeText",
    "features",
    "ctaLabel",
    "ctaAction"
})
@Generated("jsonschema2pojo")
public class SubscriptionTier {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private String id;
    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Tier name, e.g. 'League Pass', 'League Pass Premium'")
    @NotNull
    private String name;
    /**
     * Display price, e.g. '$14.99/mo'
     * (Required)
     * 
     */
    @JsonProperty("price")
    @JsonPropertyDescription("Display price, e.g. '$14.99/mo'")
    @NotNull
    private String price;
    /**
     * Strikethrough price if on sale
     * 
     */
    @JsonProperty("originalPrice")
    @JsonPropertyDescription("Strikethrough price if on sale")
    private String originalPrice;
    /**
     * Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
     * 
     */
    @JsonProperty("badgeText")
    @JsonPropertyDescription("Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'")
    private String badgeText;
    @JsonProperty("features")
    @Valid
    private List<String> features = new ArrayList<String>();
    @JsonProperty("ctaLabel")
    private String ctaLabel;
    @JsonProperty("ctaAction")
    @Valid
    private Action ctaAction;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionTier withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Tier name, e.g. 'League Pass', 'League Pass Premium'
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public SubscriptionTier withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Display price, e.g. '$14.99/mo'
     * (Required)
     * 
     */
    @JsonProperty("price")
    public String getPrice() {
        return price;
    }

    /**
     * Display price, e.g. '$14.99/mo'
     * (Required)
     * 
     */
    @JsonProperty("price")
    public void setPrice(String price) {
        this.price = price;
    }

    public SubscriptionTier withPrice(String price) {
        this.price = price;
        return this;
    }

    /**
     * Strikethrough price if on sale
     * 
     */
    @JsonProperty("originalPrice")
    public String getOriginalPrice() {
        return originalPrice;
    }

    /**
     * Strikethrough price if on sale
     * 
     */
    @JsonProperty("originalPrice")
    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public SubscriptionTier withOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
        return this;
    }

    /**
     * Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
     * 
     */
    @JsonProperty("badgeText")
    public String getBadgeText() {
        return badgeText;
    }

    /**
     * Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
     * 
     */
    @JsonProperty("badgeText")
    public void setBadgeText(String badgeText) {
        this.badgeText = badgeText;
    }

    public SubscriptionTier withBadgeText(String badgeText) {
        this.badgeText = badgeText;
        return this;
    }

    @JsonProperty("features")
    public List<String> getFeatures() {
        return features;
    }

    @JsonProperty("features")
    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public SubscriptionTier withFeatures(List<String> features) {
        this.features = features;
        return this;
    }

    @JsonProperty("ctaLabel")
    public String getCtaLabel() {
        return ctaLabel;
    }

    @JsonProperty("ctaLabel")
    public void setCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
    }

    public SubscriptionTier withCtaLabel(String ctaLabel) {
        this.ctaLabel = ctaLabel;
        return this;
    }

    @JsonProperty("ctaAction")
    public Action getCtaAction() {
        return ctaAction;
    }

    @JsonProperty("ctaAction")
    public void setCtaAction(Action ctaAction) {
        this.ctaAction = ctaAction;
    }

    public SubscriptionTier withCtaAction(Action ctaAction) {
        this.ctaAction = ctaAction;
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

    public SubscriptionTier withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SubscriptionTier.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(((this.id == null)?"<null>":this.id));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("price");
        sb.append('=');
        sb.append(((this.price == null)?"<null>":this.price));
        sb.append(',');
        sb.append("originalPrice");
        sb.append('=');
        sb.append(((this.originalPrice == null)?"<null>":this.originalPrice));
        sb.append(',');
        sb.append("badgeText");
        sb.append('=');
        sb.append(((this.badgeText == null)?"<null>":this.badgeText));
        sb.append(',');
        sb.append("features");
        sb.append('=');
        sb.append(((this.features == null)?"<null>":this.features));
        sb.append(',');
        sb.append("ctaLabel");
        sb.append('=');
        sb.append(((this.ctaLabel == null)?"<null>":this.ctaLabel));
        sb.append(',');
        sb.append("ctaAction");
        sb.append('=');
        sb.append(((this.ctaAction == null)?"<null>":this.ctaAction));
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
        result = ((result* 31)+((this.features == null)? 0 :this.features.hashCode()));
        result = ((result* 31)+((this.originalPrice == null)? 0 :this.originalPrice.hashCode()));
        result = ((result* 31)+((this.price == null)? 0 :this.price.hashCode()));
        result = ((result* 31)+((this.ctaAction == null)? 0 :this.ctaAction.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.id == null)? 0 :this.id.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.badgeText == null)? 0 :this.badgeText.hashCode()));
        result = ((result* 31)+((this.ctaLabel == null)? 0 :this.ctaLabel.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SubscriptionTier) == false) {
            return false;
        }
        SubscriptionTier rhs = ((SubscriptionTier) other);
        return ((((((((((this.features == rhs.features)||((this.features!= null)&&this.features.equals(rhs.features)))&&((this.originalPrice == rhs.originalPrice)||((this.originalPrice!= null)&&this.originalPrice.equals(rhs.originalPrice))))&&((this.price == rhs.price)||((this.price!= null)&&this.price.equals(rhs.price))))&&((this.ctaAction == rhs.ctaAction)||((this.ctaAction!= null)&&this.ctaAction.equals(rhs.ctaAction))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.id == rhs.id)||((this.id!= null)&&this.id.equals(rhs.id))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.badgeText == rhs.badgeText)||((this.badgeText!= null)&&this.badgeText.equals(rhs.badgeText))))&&((this.ctaLabel == rhs.ctaLabel)||((this.ctaLabel!= null)&&this.ctaLabel.equals(rhs.ctaLabel))));
    }

}
