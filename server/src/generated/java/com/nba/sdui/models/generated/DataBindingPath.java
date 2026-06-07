
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "sourcePath",
    "targetPath",
    "transform"
})
@Generated("jsonschema2pojo")
public class DataBindingPath {

    /**
     * JSONPath in incoming message (e.g., '$.homeTeam.score')
     * (Required)
     * 
     */
    @JsonProperty("sourcePath")
    @JsonPropertyDescription("JSONPath in incoming message (e.g., '$.homeTeam.score')")
    @NotNull
    private String sourcePath;
    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     * (Required)
     * 
     */
    @JsonProperty("targetPath")
    @JsonPropertyDescription("Dot-path to component property (e.g., 'homeScore.content')")
    @NotNull
    private String targetPath;
    /**
     * Optional server-declared transform applied by shared client binding infrastructure before writing the target value. liveClockSnapshot normalizes clock payload values into { snapshotSeconds, snapshotAt, isRunning }.
     * 
     */
    @JsonProperty("transform")
    @JsonPropertyDescription("Optional server-declared transform applied by shared client binding infrastructure before writing the target value. liveClockSnapshot normalizes clock payload values into { snapshotSeconds, snapshotAt, isRunning }.")
    private DataBindingPath.Transform transform;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * JSONPath in incoming message (e.g., '$.homeTeam.score')
     * (Required)
     * 
     */
    @JsonProperty("sourcePath")
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * JSONPath in incoming message (e.g., '$.homeTeam.score')
     * (Required)
     * 
     */
    @JsonProperty("sourcePath")
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public DataBindingPath withSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }

    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     * (Required)
     * 
     */
    @JsonProperty("targetPath")
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * Dot-path to component property (e.g., 'homeScore.content')
     * (Required)
     * 
     */
    @JsonProperty("targetPath")
    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public DataBindingPath withTargetPath(String targetPath) {
        this.targetPath = targetPath;
        return this;
    }

    /**
     * Optional server-declared transform applied by shared client binding infrastructure before writing the target value. liveClockSnapshot normalizes clock payload values into { snapshotSeconds, snapshotAt, isRunning }.
     * 
     */
    @JsonProperty("transform")
    public DataBindingPath.Transform getTransform() {
        return transform;
    }

    /**
     * Optional server-declared transform applied by shared client binding infrastructure before writing the target value. liveClockSnapshot normalizes clock payload values into { snapshotSeconds, snapshotAt, isRunning }.
     * 
     */
    @JsonProperty("transform")
    public void setTransform(DataBindingPath.Transform transform) {
        this.transform = transform;
    }

    public DataBindingPath withTransform(DataBindingPath.Transform transform) {
        this.transform = transform;
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

    public DataBindingPath withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DataBindingPath.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("sourcePath");
        sb.append('=');
        sb.append(((this.sourcePath == null)?"<null>":this.sourcePath));
        sb.append(',');
        sb.append("targetPath");
        sb.append('=');
        sb.append(((this.targetPath == null)?"<null>":this.targetPath));
        sb.append(',');
        sb.append("transform");
        sb.append('=');
        sb.append(((this.transform == null)?"<null>":this.transform));
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
        result = ((result* 31)+((this.targetPath == null)? 0 :this.targetPath.hashCode()));
        result = ((result* 31)+((this.transform == null)? 0 :this.transform.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.sourcePath == null)? 0 :this.sourcePath.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataBindingPath) == false) {
            return false;
        }
        DataBindingPath rhs = ((DataBindingPath) other);
        return (((((this.targetPath == rhs.targetPath)||((this.targetPath!= null)&&this.targetPath.equals(rhs.targetPath)))&&((this.transform == rhs.transform)||((this.transform!= null)&&this.transform.equals(rhs.transform))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.sourcePath == rhs.sourcePath)||((this.sourcePath!= null)&&this.sourcePath.equals(rhs.sourcePath))));
    }


    /**
     * Optional server-declared transform applied by shared client binding infrastructure before writing the target value. liveClockSnapshot normalizes clock payload values into { snapshotSeconds, snapshotAt, isRunning }.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Transform {

        LIVE_CLOCK_SNAPSHOT("liveClockSnapshot");
        private final String value;
        private final static Map<String, DataBindingPath.Transform> CONSTANTS = new HashMap<String, DataBindingPath.Transform>();

        static {
            for (DataBindingPath.Transform c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Transform(String value) {
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
        public static DataBindingPath.Transform fromValue(String value) {
            DataBindingPath.Transform constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
