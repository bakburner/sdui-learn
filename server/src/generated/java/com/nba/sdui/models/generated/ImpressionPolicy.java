
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


/**
 * Impression tracking policy for analytics actions with onVisible trigger
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "dedup",
    "threshold",
    "intervalMs"
})
@Generated("jsonschema2pojo")
public class ImpressionPolicy {

    @JsonProperty("dedup")
    private ImpressionPolicy.ImpressionDedup dedup;
    @JsonProperty("threshold")
    @Valid
    private ImpressionThreshold threshold;
    /**
     * Reset interval for once-per-interval strategy (milliseconds)
     * 
     */
    @JsonProperty("intervalMs")
    @JsonPropertyDescription("Reset interval for once-per-interval strategy (milliseconds)")
    private Integer intervalMs;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("dedup")
    public ImpressionPolicy.ImpressionDedup getDedup() {
        return dedup;
    }

    @JsonProperty("dedup")
    public void setDedup(ImpressionPolicy.ImpressionDedup dedup) {
        this.dedup = dedup;
    }

    public ImpressionPolicy withDedup(ImpressionPolicy.ImpressionDedup dedup) {
        this.dedup = dedup;
        return this;
    }

    @JsonProperty("threshold")
    public ImpressionThreshold getThreshold() {
        return threshold;
    }

    @JsonProperty("threshold")
    public void setThreshold(ImpressionThreshold threshold) {
        this.threshold = threshold;
    }

    public ImpressionPolicy withThreshold(ImpressionThreshold threshold) {
        this.threshold = threshold;
        return this;
    }

    /**
     * Reset interval for once-per-interval strategy (milliseconds)
     * 
     */
    @JsonProperty("intervalMs")
    public Integer getIntervalMs() {
        return intervalMs;
    }

    /**
     * Reset interval for once-per-interval strategy (milliseconds)
     * 
     */
    @JsonProperty("intervalMs")
    public void setIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
    }

    public ImpressionPolicy withIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
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

    public ImpressionPolicy withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ImpressionPolicy.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("dedup");
        sb.append('=');
        sb.append(((this.dedup == null)?"<null>":this.dedup));
        sb.append(',');
        sb.append("threshold");
        sb.append('=');
        sb.append(((this.threshold == null)?"<null>":this.threshold));
        sb.append(',');
        sb.append("intervalMs");
        sb.append('=');
        sb.append(((this.intervalMs == null)?"<null>":this.intervalMs));
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
        result = ((result* 31)+((this.threshold == null)? 0 :this.threshold.hashCode()));
        result = ((result* 31)+((this.intervalMs == null)? 0 :this.intervalMs.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.dedup == null)? 0 :this.dedup.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ImpressionPolicy) == false) {
            return false;
        }
        ImpressionPolicy rhs = ((ImpressionPolicy) other);
        return (((((this.threshold == rhs.threshold)||((this.threshold!= null)&&this.threshold.equals(rhs.threshold)))&&((this.intervalMs == rhs.intervalMs)||((this.intervalMs!= null)&&this.intervalMs.equals(rhs.intervalMs))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.dedup == rhs.dedup)||((this.dedup!= null)&&this.dedup.equals(rhs.dedup))));
    }

    @Generated("jsonschema2pojo")
    public enum ImpressionDedup {

        NONE("none"),
        ONCE_PER_SCREEN("once-per-screen"),
        ONCE_PER_SESSION("once-per-session"),
        ONCE_PER_INTERVAL("once-per-interval");
        private final String value;
        private final static Map<String, ImpressionPolicy.ImpressionDedup> CONSTANTS = new HashMap<String, ImpressionPolicy.ImpressionDedup>();

        static {
            for (ImpressionPolicy.ImpressionDedup c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ImpressionDedup(String value) {
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
        public static ImpressionPolicy.ImpressionDedup fromValue(String value) {
            ImpressionPolicy.ImpressionDedup constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
