
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


/**
 * Subscription upsell. Reserved SDK integration point: the upsell's visible chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP product identifiers the SDK will later bind to; `ctaAction` is the optional pre-SDK fallback action. Layout intent (inline banner vs. full-screen hero) is expressed by the inner `ui` atomic tree and the section's outer `surface`, not by component identity.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ui",
    "ctaAction",
    "tiers"
})
@Generated("jsonschema2pojo")
public class SubscribeUpsell {

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("ui")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement ui;
    @JsonProperty("ctaAction")
    @Valid
    private Action ctaAction;
    /**
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands; not used by the renderer, which reads the visible price copy out of `ui`.
     * 
     */
    @JsonProperty("tiers")
    @JsonPropertyDescription("IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands; not used by the renderer, which reads the visible price copy out of `ui`.")
    @Valid
    private List<SubscriptionTier> tiers = new ArrayList<SubscriptionTier>();
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("ui")
    public AtomicElement getUi() {
        return ui;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("ui")
    public void setUi(AtomicElement ui) {
        this.ui = ui;
    }

    public SubscribeUpsell withUi(AtomicElement ui) {
        this.ui = ui;
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

    public SubscribeUpsell withCtaAction(Action ctaAction) {
        this.ctaAction = ctaAction;
        return this;
    }

    /**
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands; not used by the renderer, which reads the visible price copy out of `ui`.
     * 
     */
    @JsonProperty("tiers")
    public List<SubscriptionTier> getTiers() {
        return tiers;
    }

    /**
     * IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands; not used by the renderer, which reads the visible price copy out of `ui`.
     * 
     */
    @JsonProperty("tiers")
    public void setTiers(List<SubscriptionTier> tiers) {
        this.tiers = tiers;
    }

    public SubscribeUpsell withTiers(List<SubscriptionTier> tiers) {
        this.tiers = tiers;
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

    public SubscribeUpsell withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SubscribeUpsell.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("ui");
        sb.append('=');
        sb.append(((this.ui == null)?"<null>":this.ui));
        sb.append(',');
        sb.append("ctaAction");
        sb.append('=');
        sb.append(((this.ctaAction == null)?"<null>":this.ctaAction));
        sb.append(',');
        sb.append("tiers");
        sb.append('=');
        sb.append(((this.tiers == null)?"<null>":this.tiers));
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
        result = ((result* 31)+((this.tiers == null)? 0 :this.tiers.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.ui == null)? 0 :this.ui.hashCode()));
        result = ((result* 31)+((this.ctaAction == null)? 0 :this.ctaAction.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SubscribeUpsell) == false) {
            return false;
        }
        SubscribeUpsell rhs = ((SubscribeUpsell) other);
        return (((((this.tiers == rhs.tiers)||((this.tiers!= null)&&this.tiers.equals(rhs.tiers)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.ui == rhs.ui)||((this.ui!= null)&&this.ui.equals(rhs.ui))))&&((this.ctaAction == rhs.ctaAction)||((this.ctaAction!= null)&&this.ctaAction.equals(rhs.ctaAction))));
    }

}
