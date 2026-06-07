
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "skeleton",
    "minHeightDp"
})
@Generated("jsonschema2pojo")
public class Loading {

    /**
     * Which loading skeleton style to use
     * 
     */
    @JsonProperty("skeleton")
    @JsonPropertyDescription("Which loading skeleton style to use")
    private Loading.Skeleton skeleton = Loading.Skeleton.fromValue("shimmer");
    /**
     * Minimum height to reserve during loading (prevents layout shift)
     * 
     */
    @JsonProperty("minHeightDp")
    @JsonPropertyDescription("Minimum height to reserve during loading (prevents layout shift)")
    private Integer minHeightDp;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Which loading skeleton style to use
     * 
     */
    @JsonProperty("skeleton")
    public Loading.Skeleton getSkeleton() {
        return skeleton;
    }

    /**
     * Which loading skeleton style to use
     * 
     */
    @JsonProperty("skeleton")
    public void setSkeleton(Loading.Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public Loading withSkeleton(Loading.Skeleton skeleton) {
        this.skeleton = skeleton;
        return this;
    }

    /**
     * Minimum height to reserve during loading (prevents layout shift)
     * 
     */
    @JsonProperty("minHeightDp")
    public Integer getMinHeightDp() {
        return minHeightDp;
    }

    /**
     * Minimum height to reserve during loading (prevents layout shift)
     * 
     */
    @JsonProperty("minHeightDp")
    public void setMinHeightDp(Integer minHeightDp) {
        this.minHeightDp = minHeightDp;
    }

    public Loading withMinHeightDp(Integer minHeightDp) {
        this.minHeightDp = minHeightDp;
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

    public Loading withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Loading.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("skeleton");
        sb.append('=');
        sb.append(((this.skeleton == null)?"<null>":this.skeleton));
        sb.append(',');
        sb.append("minHeightDp");
        sb.append('=');
        sb.append(((this.minHeightDp == null)?"<null>":this.minHeightDp));
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
        result = ((result* 31)+((this.skeleton == null)? 0 :this.skeleton.hashCode()));
        result = ((result* 31)+((this.minHeightDp == null)? 0 :this.minHeightDp.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Loading) == false) {
            return false;
        }
        Loading rhs = ((Loading) other);
        return ((((this.skeleton == rhs.skeleton)||((this.skeleton!= null)&&this.skeleton.equals(rhs.skeleton)))&&((this.minHeightDp == rhs.minHeightDp)||((this.minHeightDp!= null)&&this.minHeightDp.equals(rhs.minHeightDp))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }


    /**
     * Which loading skeleton style to use
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Skeleton {

        SHIMMER("shimmer"),
        SPINNER("spinner"),
        PLACEHOLDER("placeholder"),
        NONE("none");
        private final String value;
        private final static Map<String, Loading.Skeleton> CONSTANTS = new HashMap<String, Loading.Skeleton>();

        static {
            for (Loading.Skeleton c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Skeleton(String value) {
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
        public static Loading.Skeleton fromValue(String value) {
            Loading.Skeleton constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
