
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
 * Server-declared loading and error presentation for a section. Clients render these states when applicable.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "loading",
    "error"
})
@Generated("jsonschema2pojo")
public class SectionStates {

    @JsonProperty("loading")
    @Valid
    private Loading loading;
    /**
     * Server-declared error-state shape rendered by section error boundaries. Named `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's native error protocol (e.g. `Swift.Error`).
     * 
     */
    @JsonProperty("error")
    @JsonPropertyDescription("Server-declared error-state shape rendered by section error boundaries. Named `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's native error protocol (e.g. `Swift.Error`).")
    @Valid
    private ErrorState error;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("loading")
    public Loading getLoading() {
        return loading;
    }

    @JsonProperty("loading")
    public void setLoading(Loading loading) {
        this.loading = loading;
    }

    public SectionStates withLoading(Loading loading) {
        this.loading = loading;
        return this;
    }

    /**
     * Server-declared error-state shape rendered by section error boundaries. Named `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's native error protocol (e.g. `Swift.Error`).
     * 
     */
    @JsonProperty("error")
    public ErrorState getError() {
        return error;
    }

    /**
     * Server-declared error-state shape rendered by section error boundaries. Named `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's native error protocol (e.g. `Swift.Error`).
     * 
     */
    @JsonProperty("error")
    public void setError(ErrorState error) {
        this.error = error;
    }

    public SectionStates withError(ErrorState error) {
        this.error = error;
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

    public SectionStates withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(SectionStates.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("loading");
        sb.append('=');
        sb.append(((this.loading == null)?"<null>":this.loading));
        sb.append(',');
        sb.append("error");
        sb.append('=');
        sb.append(((this.error == null)?"<null>":this.error));
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
        result = ((result* 31)+((this.loading == null)? 0 :this.loading.hashCode()));
        result = ((result* 31)+((this.error == null)? 0 :this.error.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof SectionStates) == false) {
            return false;
        }
        SectionStates rhs = ((SectionStates) other);
        return ((((this.loading == rhs.loading)||((this.loading!= null)&&this.loading.equals(rhs.loading)))&&((this.error == rhs.error)||((this.error!= null)&&this.error.equals(rhs.error))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
