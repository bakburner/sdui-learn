
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
 * Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets clients expose variant selection without hardcoding experiment ids or option vocabularies.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "experimentId",
    "options"
})
@Generated("jsonschema2pojo")
public class ExperimentVariants {

    /**
     * Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key back to the server as part of the experiments map on subsequent requests.
     * (Required)
     * 
     */
    @JsonProperty("experimentId")
    @JsonPropertyDescription("Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key back to the server as part of the experiments map on subsequent requests.")
    @NotNull
    private String experimentId;
    /**
     * Ordered list of variants the client may choose from.
     * (Required)
     * 
     */
    @JsonProperty("options")
    @JsonPropertyDescription("Ordered list of variants the client may choose from.")
    @Valid
    @NotNull
    private List<ExperimentVariantOption> options = new ArrayList<ExperimentVariantOption>();
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key back to the server as part of the experiments map on subsequent requests.
     * (Required)
     * 
     */
    @JsonProperty("experimentId")
    public String getExperimentId() {
        return experimentId;
    }

    /**
     * Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key back to the server as part of the experiments map on subsequent requests.
     * (Required)
     * 
     */
    @JsonProperty("experimentId")
    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public ExperimentVariants withExperimentId(String experimentId) {
        this.experimentId = experimentId;
        return this;
    }

    /**
     * Ordered list of variants the client may choose from.
     * (Required)
     * 
     */
    @JsonProperty("options")
    public List<ExperimentVariantOption> getOptions() {
        return options;
    }

    /**
     * Ordered list of variants the client may choose from.
     * (Required)
     * 
     */
    @JsonProperty("options")
    public void setOptions(List<ExperimentVariantOption> options) {
        this.options = options;
    }

    public ExperimentVariants withOptions(List<ExperimentVariantOption> options) {
        this.options = options;
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

    public ExperimentVariants withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExperimentVariants.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("experimentId");
        sb.append('=');
        sb.append(((this.experimentId == null)?"<null>":this.experimentId));
        sb.append(',');
        sb.append("options");
        sb.append('=');
        sb.append(((this.options == null)?"<null>":this.options));
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
        result = ((result* 31)+((this.options == null)? 0 :this.options.hashCode()));
        result = ((result* 31)+((this.experimentId == null)? 0 :this.experimentId.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ExperimentVariants) == false) {
            return false;
        }
        ExperimentVariants rhs = ((ExperimentVariants) other);
        return ((((this.options == rhs.options)||((this.options!= null)&&this.options.equals(rhs.options)))&&((this.experimentId == rhs.experimentId)||((this.experimentId!= null)&&this.experimentId.equals(rhs.experimentId))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
