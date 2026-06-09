
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "type",
    "intervalMs",
    "url",
    "sectionEndpoint",
    "channel",
    "dataPath",
    "pauseWhenOffScreen"
})
@Generated("jsonschema2pojo")
public class RefreshPolicy {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    @NotNull
    private RefreshPolicy.RefreshType type;
    /**
     * For poll type: interval in milliseconds
     * 
     */
    @JsonProperty("intervalMs")
    @JsonPropertyDescription("For poll type: interval in milliseconds")
    @DecimalMin("1000")
    private Integer intervalMs;
    /**
     * For poll type: external URL to poll (e.g. CDN stats endpoint). Data is applied via dataBinding. Mutually exclusive with sectionEndpoint; if both are present, sectionEndpoint takes precedence.
     * 
     */
    @JsonProperty("url")
    @JsonPropertyDescription("For poll type: external URL to poll (e.g. CDN stats endpoint). Data is applied via dataBinding. Mutually exclusive with sectionEndpoint; if both are present, sectionEndpoint takes precedence.")
    private String url;
    /**
     * For poll type: server-relative SDUI path to re-fetch this section (e.g. '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'). The response is a single Section object that replaces this section in place; the client then re-evaluates the new section's refreshPolicy (enabling poll→SSE transition). Mutually exclusive with url; sectionEndpoint takes precedence when both are present.
     * 
     */
    @JsonProperty("sectionEndpoint")
    @JsonPropertyDescription("For poll type: server-relative SDUI path to re-fetch this section (e.g. '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'). The response is a single Section object that replaces this section in place; the client then re-evaluates the new section's refreshPolicy (enabling poll\u2192SSE transition). Mutually exclusive with url; sectionEndpoint takes precedence when both are present.")
    private String sectionEndpoint;
    /**
     * For sse type: subscription channel name pattern (e.g., '{gameId}:linescore'). Transport binding is a client implementation detail.
     * 
     */
    @JsonProperty("channel")
    @JsonPropertyDescription("For sse type: subscription channel name pattern (e.g., '{gameId}:linescore'). Transport binding is a client implementation detail.")
    private String channel;
    /**
     * JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
     * 
     */
    @JsonProperty("dataPath")
    @JsonPropertyDescription("JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')")
    private String dataPath;
    /**
     * Whether the client should pause this section's refresh when it scrolls out of the viewport. Default true. Set false for critical live sections (e.g., live-score panels) that should refresh continuously.
     * 
     */
    @JsonProperty("pauseWhenOffScreen")
    @JsonPropertyDescription("Whether the client should pause this section's refresh when it scrolls out of the viewport. Default true. Set false for critical live sections (e.g., live-score panels) that should refresh continuously.")
    private Boolean pauseWhenOffScreen = true;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public RefreshPolicy.RefreshType getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(RefreshPolicy.RefreshType type) {
        this.type = type;
    }

    public RefreshPolicy withType(RefreshPolicy.RefreshType type) {
        this.type = type;
        return this;
    }

    /**
     * For poll type: interval in milliseconds
     * 
     */
    @JsonProperty("intervalMs")
    public Integer getIntervalMs() {
        return intervalMs;
    }

    /**
     * For poll type: interval in milliseconds
     * 
     */
    @JsonProperty("intervalMs")
    public void setIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
    }

    public RefreshPolicy withIntervalMs(Integer intervalMs) {
        this.intervalMs = intervalMs;
        return this;
    }

    /**
     * For poll type: external URL to poll (e.g. CDN stats endpoint). Data is applied via dataBinding. Mutually exclusive with sectionEndpoint; if both are present, sectionEndpoint takes precedence.
     * 
     */
    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    /**
     * For poll type: external URL to poll (e.g. CDN stats endpoint). Data is applied via dataBinding. Mutually exclusive with sectionEndpoint; if both are present, sectionEndpoint takes precedence.
     * 
     */
    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public RefreshPolicy withUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * For poll type: server-relative SDUI path to re-fetch this section (e.g. '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'). The response is a single Section object that replaces this section in place; the client then re-evaluates the new section's refreshPolicy (enabling poll→SSE transition). Mutually exclusive with url; sectionEndpoint takes precedence when both are present.
     * 
     */
    @JsonProperty("sectionEndpoint")
    public String getSectionEndpoint() {
        return sectionEndpoint;
    }

    /**
     * For poll type: server-relative SDUI path to re-fetch this section (e.g. '/v1/sdui/section/stats-api:game-123::AtomicComposite::scoreboard'). The response is a single Section object that replaces this section in place; the client then re-evaluates the new section's refreshPolicy (enabling poll→SSE transition). Mutually exclusive with url; sectionEndpoint takes precedence when both are present.
     * 
     */
    @JsonProperty("sectionEndpoint")
    public void setSectionEndpoint(String sectionEndpoint) {
        this.sectionEndpoint = sectionEndpoint;
    }

    public RefreshPolicy withSectionEndpoint(String sectionEndpoint) {
        this.sectionEndpoint = sectionEndpoint;
        return this;
    }

    /**
     * For sse type: subscription channel name pattern (e.g., '{gameId}:linescore'). Transport binding is a client implementation detail.
     * 
     */
    @JsonProperty("channel")
    public String getChannel() {
        return channel;
    }

    /**
     * For sse type: subscription channel name pattern (e.g., '{gameId}:linescore'). Transport binding is a client implementation detail.
     * 
     */
    @JsonProperty("channel")
    public void setChannel(String channel) {
        this.channel = channel;
    }

    public RefreshPolicy withChannel(String channel) {
        this.channel = channel;
        return this;
    }

    /**
     * JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
     * 
     */
    @JsonProperty("dataPath")
    public String getDataPath() {
        return dataPath;
    }

    /**
     * JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
     * 
     */
    @JsonProperty("dataPath")
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public RefreshPolicy withDataPath(String dataPath) {
        this.dataPath = dataPath;
        return this;
    }

    /**
     * Whether the client should pause this section's refresh when it scrolls out of the viewport. Default true. Set false for critical live sections (e.g., live-score panels) that should refresh continuously.
     * 
     */
    @JsonProperty("pauseWhenOffScreen")
    public Boolean getPauseWhenOffScreen() {
        return pauseWhenOffScreen;
    }

    /**
     * Whether the client should pause this section's refresh when it scrolls out of the viewport. Default true. Set false for critical live sections (e.g., live-score panels) that should refresh continuously.
     * 
     */
    @JsonProperty("pauseWhenOffScreen")
    public void setPauseWhenOffScreen(Boolean pauseWhenOffScreen) {
        this.pauseWhenOffScreen = pauseWhenOffScreen;
    }

    public RefreshPolicy withPauseWhenOffScreen(Boolean pauseWhenOffScreen) {
        this.pauseWhenOffScreen = pauseWhenOffScreen;
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

    public RefreshPolicy withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RefreshPolicy.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("intervalMs");
        sb.append('=');
        sb.append(((this.intervalMs == null)?"<null>":this.intervalMs));
        sb.append(',');
        sb.append("url");
        sb.append('=');
        sb.append(((this.url == null)?"<null>":this.url));
        sb.append(',');
        sb.append("sectionEndpoint");
        sb.append('=');
        sb.append(((this.sectionEndpoint == null)?"<null>":this.sectionEndpoint));
        sb.append(',');
        sb.append("channel");
        sb.append('=');
        sb.append(((this.channel == null)?"<null>":this.channel));
        sb.append(',');
        sb.append("dataPath");
        sb.append('=');
        sb.append(((this.dataPath == null)?"<null>":this.dataPath));
        sb.append(',');
        sb.append("pauseWhenOffScreen");
        sb.append('=');
        sb.append(((this.pauseWhenOffScreen == null)?"<null>":this.pauseWhenOffScreen));
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
        result = ((result* 31)+((this.sectionEndpoint == null)? 0 :this.sectionEndpoint.hashCode()));
        result = ((result* 31)+((this.intervalMs == null)? 0 :this.intervalMs.hashCode()));
        result = ((result* 31)+((this.channel == null)? 0 :this.channel.hashCode()));
        result = ((result* 31)+((this.pauseWhenOffScreen == null)? 0 :this.pauseWhenOffScreen.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.url == null)? 0 :this.url.hashCode()));
        result = ((result* 31)+((this.dataPath == null)? 0 :this.dataPath.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof RefreshPolicy) == false) {
            return false;
        }
        RefreshPolicy rhs = ((RefreshPolicy) other);
        return (((((((((this.sectionEndpoint == rhs.sectionEndpoint)||((this.sectionEndpoint!= null)&&this.sectionEndpoint.equals(rhs.sectionEndpoint)))&&((this.intervalMs == rhs.intervalMs)||((this.intervalMs!= null)&&this.intervalMs.equals(rhs.intervalMs))))&&((this.channel == rhs.channel)||((this.channel!= null)&&this.channel.equals(rhs.channel))))&&((this.pauseWhenOffScreen == rhs.pauseWhenOffScreen)||((this.pauseWhenOffScreen!= null)&&this.pauseWhenOffScreen.equals(rhs.pauseWhenOffScreen))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.url == rhs.url)||((this.url!= null)&&this.url.equals(rhs.url))))&&((this.dataPath == rhs.dataPath)||((this.dataPath!= null)&&this.dataPath.equals(rhs.dataPath))));
    }

    @Generated("jsonschema2pojo")
    public enum RefreshType {

        STATIC("static"),
        POLL("poll"),
        SSE("sse");
        private final String value;
        private final static Map<String, RefreshPolicy.RefreshType> CONSTANTS = new HashMap<String, RefreshPolicy.RefreshType>();

        static {
            for (RefreshPolicy.RefreshType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        RefreshType(String value) {
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
        public static RefreshPolicy.RefreshType fromValue(String value) {
            RefreshPolicy.RefreshType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
