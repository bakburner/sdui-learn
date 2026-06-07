
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
import jakarta.validation.constraints.DecimalMin;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "visibility",
    "dwellMs"
})
@Generated("jsonschema2pojo")
public class ImpressionThreshold {

    /**
     * Fraction of section area that must be visible (0.5 = 50%)
     * 
     */
    @JsonProperty("visibility")
    @JsonPropertyDescription("Fraction of section area that must be visible (0.5 = 50%)")
    private Double visibility = 0.5D;
    /**
     * Milliseconds section must remain visible before impression fires
     * 
     */
    @JsonProperty("dwellMs")
    @JsonPropertyDescription("Milliseconds section must remain visible before impression fires")
    @DecimalMin("0")
    private Integer dwellMs = 1000;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Fraction of section area that must be visible (0.5 = 50%)
     * 
     */
    @JsonProperty("visibility")
    public Double getVisibility() {
        return visibility;
    }

    /**
     * Fraction of section area that must be visible (0.5 = 50%)
     * 
     */
    @JsonProperty("visibility")
    public void setVisibility(Double visibility) {
        this.visibility = visibility;
    }

    public ImpressionThreshold withVisibility(Double visibility) {
        this.visibility = visibility;
        return this;
    }

    /**
     * Milliseconds section must remain visible before impression fires
     * 
     */
    @JsonProperty("dwellMs")
    public Integer getDwellMs() {
        return dwellMs;
    }

    /**
     * Milliseconds section must remain visible before impression fires
     * 
     */
    @JsonProperty("dwellMs")
    public void setDwellMs(Integer dwellMs) {
        this.dwellMs = dwellMs;
    }

    public ImpressionThreshold withDwellMs(Integer dwellMs) {
        this.dwellMs = dwellMs;
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

    public ImpressionThreshold withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ImpressionThreshold.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("visibility");
        sb.append('=');
        sb.append(((this.visibility == null)?"<null>":this.visibility));
        sb.append(',');
        sb.append("dwellMs");
        sb.append('=');
        sb.append(((this.dwellMs == null)?"<null>":this.dwellMs));
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
        result = ((result* 31)+((this.visibility == null)? 0 :this.visibility.hashCode()));
        result = ((result* 31)+((this.dwellMs == null)? 0 :this.dwellMs.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ImpressionThreshold) == false) {
            return false;
        }
        ImpressionThreshold rhs = ((ImpressionThreshold) other);
        return ((((this.visibility == rhs.visibility)||((this.visibility!= null)&&this.visibility.equals(rhs.visibility)))&&((this.dwellMs == rhs.dwellMs)||((this.dwellMs!= null)&&this.dwellMs.equals(rhs.dwellMs))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
