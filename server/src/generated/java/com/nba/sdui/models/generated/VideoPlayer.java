
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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


/**
 * Video player section — reserved SDK integration point for DRM / HLS / ad insertion. `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders before the SDK is integrated and will serve as the loading/error placeholder afterwards.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "playerType",
    "contentId",
    "autoplay",
    "capabilities",
    "ui",
    "displayConfig"
})
@Generated("jsonschema2pojo")
public class VideoPlayer {

    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     * (Required)
     * 
     */
    @JsonProperty("playerType")
    @JsonPropertyDescription("Discriminator for SDK player variant. Client passes contentId to the matching SDK method.")
    @NotNull
    private VideoPlayer.PlayerType playerType;
    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     * (Required)
     * 
     */
    @JsonProperty("contentId")
    @JsonPropertyDescription("Content identifier \u2014 interpreted by playerType (gameId for game, mediaId for vod, eventId for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.")
    @NotNull
    private String contentId;
    @JsonProperty("autoplay")
    private Boolean autoplay = true;
    /**
     * Platform capabilities the player should enable. Server includes only capabilities relevant to the requesting platform (via X-Analytics-Platform header).
     * 
     */
    @JsonProperty("capabilities")
    @JsonPropertyDescription("Platform capabilities the player should enable. Server includes only capabilities relevant to the requesting platform (via X-Analytics-Platform header).")
    @Valid
    private List<Capability> capabilities = new ArrayList<Capability>();
    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("ui")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement ui;
    @JsonProperty("displayConfig")
    @Valid
    private DisplayConfig displayConfig;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     * (Required)
     * 
     */
    @JsonProperty("playerType")
    public VideoPlayer.PlayerType getPlayerType() {
        return playerType;
    }

    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     * (Required)
     * 
     */
    @JsonProperty("playerType")
    public void setPlayerType(VideoPlayer.PlayerType playerType) {
        this.playerType = playerType;
    }

    public VideoPlayer withPlayerType(VideoPlayer.PlayerType playerType) {
        this.playerType = playerType;
        return this;
    }

    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     * (Required)
     * 
     */
    @JsonProperty("contentId")
    public String getContentId() {
        return contentId;
    }

    /**
     * Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
     * (Required)
     * 
     */
    @JsonProperty("contentId")
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public VideoPlayer withContentId(String contentId) {
        this.contentId = contentId;
        return this;
    }

    @JsonProperty("autoplay")
    public Boolean getAutoplay() {
        return autoplay;
    }

    @JsonProperty("autoplay")
    public void setAutoplay(Boolean autoplay) {
        this.autoplay = autoplay;
    }

    public VideoPlayer withAutoplay(Boolean autoplay) {
        this.autoplay = autoplay;
        return this;
    }

    /**
     * Platform capabilities the player should enable. Server includes only capabilities relevant to the requesting platform (via X-Analytics-Platform header).
     * 
     */
    @JsonProperty("capabilities")
    public List<Capability> getCapabilities() {
        return capabilities;
    }

    /**
     * Platform capabilities the player should enable. Server includes only capabilities relevant to the requesting platform (via X-Analytics-Platform header).
     * 
     */
    @JsonProperty("capabilities")
    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public VideoPlayer withCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
        return this;
    }

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

    public VideoPlayer withUi(AtomicElement ui) {
        this.ui = ui;
        return this;
    }

    @JsonProperty("displayConfig")
    public DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    @JsonProperty("displayConfig")
    public void setDisplayConfig(DisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
    }

    public VideoPlayer withDisplayConfig(DisplayConfig displayConfig) {
        this.displayConfig = displayConfig;
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

    public VideoPlayer withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(VideoPlayer.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("playerType");
        sb.append('=');
        sb.append(((this.playerType == null)?"<null>":this.playerType));
        sb.append(',');
        sb.append("contentId");
        sb.append('=');
        sb.append(((this.contentId == null)?"<null>":this.contentId));
        sb.append(',');
        sb.append("autoplay");
        sb.append('=');
        sb.append(((this.autoplay == null)?"<null>":this.autoplay));
        sb.append(',');
        sb.append("capabilities");
        sb.append('=');
        sb.append(((this.capabilities == null)?"<null>":this.capabilities));
        sb.append(',');
        sb.append("ui");
        sb.append('=');
        sb.append(((this.ui == null)?"<null>":this.ui));
        sb.append(',');
        sb.append("displayConfig");
        sb.append('=');
        sb.append(((this.displayConfig == null)?"<null>":this.displayConfig));
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
        result = ((result* 31)+((this.capabilities == null)? 0 :this.capabilities.hashCode()));
        result = ((result* 31)+((this.ui == null)? 0 :this.ui.hashCode()));
        result = ((result* 31)+((this.displayConfig == null)? 0 :this.displayConfig.hashCode()));
        result = ((result* 31)+((this.playerType == null)? 0 :this.playerType.hashCode()));
        result = ((result* 31)+((this.contentId == null)? 0 :this.contentId.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.autoplay == null)? 0 :this.autoplay.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof VideoPlayer) == false) {
            return false;
        }
        VideoPlayer rhs = ((VideoPlayer) other);
        return ((((((((this.capabilities == rhs.capabilities)||((this.capabilities!= null)&&this.capabilities.equals(rhs.capabilities)))&&((this.ui == rhs.ui)||((this.ui!= null)&&this.ui.equals(rhs.ui))))&&((this.displayConfig == rhs.displayConfig)||((this.displayConfig!= null)&&this.displayConfig.equals(rhs.displayConfig))))&&((this.playerType == rhs.playerType)||((this.playerType!= null)&&this.playerType.equals(rhs.playerType))))&&((this.contentId == rhs.contentId)||((this.contentId!= null)&&this.contentId.equals(rhs.contentId))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.autoplay == rhs.autoplay)||((this.autoplay!= null)&&this.autoplay.equals(rhs.autoplay))));
    }


    /**
     * Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum PlayerType {

        GAME("game"),
        VOD("vod"),
        EVENT("event"),
        NBA_TV("nbaTv"),
        STREAM("stream");
        private final String value;
        private final static Map<String, VideoPlayer.PlayerType> CONSTANTS = new HashMap<String, VideoPlayer.PlayerType>();

        static {
            for (VideoPlayer.PlayerType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        PlayerType(String value) {
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
        public static VideoPlayer.PlayerType fromValue(String value) {
            VideoPlayer.PlayerType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
