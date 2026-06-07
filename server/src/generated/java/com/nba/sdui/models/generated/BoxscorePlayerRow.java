
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
 * One player row inside a boxscore table
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "playerId",
    "name",
    "position",
    "jerseyNumber",
    "imageUrl",
    "starter",
    "stats",
    "actions"
})
@Generated("jsonschema2pojo")
public class BoxscorePlayerRow {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("playerId")
    @NotNull
    private String playerId;
    /**
     * Display name (short form, e.g. 'J. Tatum')
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Display name (short form, e.g. 'J. Tatum')")
    @NotNull
    private String name;
    @JsonProperty("position")
    private String position;
    @JsonProperty("jerseyNumber")
    private String jerseyNumber;
    @JsonProperty("imageUrl")
    private String imageUrl;
    /**
     * Whether this player was in the starting lineup
     * 
     */
    @JsonProperty("starter")
    @JsonPropertyDescription("Whether this player was in the starting lineup")
    private Boolean starter = false;
    /**
     * Key-value map of stat abbreviation to value for one player row
     * (Required)
     * 
     */
    @JsonProperty("stats")
    @JsonPropertyDescription("Key-value map of stat abbreviation to value for one player row")
    @Valid
    @NotNull
    private BoxscorePlayerStatistics stats;
    @JsonProperty("actions")
    @Valid
    private List<Action> actions = new ArrayList<Action>();
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("playerId")
    public String getPlayerId() {
        return playerId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("playerId")
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public BoxscorePlayerRow withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    /**
     * Display name (short form, e.g. 'J. Tatum')
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Display name (short form, e.g. 'J. Tatum')
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public BoxscorePlayerRow withName(String name) {
        this.name = name;
        return this;
    }

    @JsonProperty("position")
    public String getPosition() {
        return position;
    }

    @JsonProperty("position")
    public void setPosition(String position) {
        this.position = position;
    }

    public BoxscorePlayerRow withPosition(String position) {
        this.position = position;
        return this;
    }

    @JsonProperty("jerseyNumber")
    public String getJerseyNumber() {
        return jerseyNumber;
    }

    @JsonProperty("jerseyNumber")
    public void setJerseyNumber(String jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public BoxscorePlayerRow withJerseyNumber(String jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
        return this;
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BoxscorePlayerRow withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    /**
     * Whether this player was in the starting lineup
     * 
     */
    @JsonProperty("starter")
    public Boolean getStarter() {
        return starter;
    }

    /**
     * Whether this player was in the starting lineup
     * 
     */
    @JsonProperty("starter")
    public void setStarter(Boolean starter) {
        this.starter = starter;
    }

    public BoxscorePlayerRow withStarter(Boolean starter) {
        this.starter = starter;
        return this;
    }

    /**
     * Key-value map of stat abbreviation to value for one player row
     * (Required)
     * 
     */
    @JsonProperty("stats")
    public BoxscorePlayerStatistics getStats() {
        return stats;
    }

    /**
     * Key-value map of stat abbreviation to value for one player row
     * (Required)
     * 
     */
    @JsonProperty("stats")
    public void setStats(BoxscorePlayerStatistics stats) {
        this.stats = stats;
    }

    public BoxscorePlayerRow withStats(BoxscorePlayerStatistics stats) {
        this.stats = stats;
        return this;
    }

    @JsonProperty("actions")
    public List<Action> getActions() {
        return actions;
    }

    @JsonProperty("actions")
    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public BoxscorePlayerRow withActions(List<Action> actions) {
        this.actions = actions;
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

    public BoxscorePlayerRow withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BoxscorePlayerRow.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("playerId");
        sb.append('=');
        sb.append(((this.playerId == null)?"<null>":this.playerId));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("position");
        sb.append('=');
        sb.append(((this.position == null)?"<null>":this.position));
        sb.append(',');
        sb.append("jerseyNumber");
        sb.append('=');
        sb.append(((this.jerseyNumber == null)?"<null>":this.jerseyNumber));
        sb.append(',');
        sb.append("imageUrl");
        sb.append('=');
        sb.append(((this.imageUrl == null)?"<null>":this.imageUrl));
        sb.append(',');
        sb.append("starter");
        sb.append('=');
        sb.append(((this.starter == null)?"<null>":this.starter));
        sb.append(',');
        sb.append("stats");
        sb.append('=');
        sb.append(((this.stats == null)?"<null>":this.stats));
        sb.append(',');
        sb.append("actions");
        sb.append('=');
        sb.append(((this.actions == null)?"<null>":this.actions));
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
        result = ((result* 31)+((this.starter == null)? 0 :this.starter.hashCode()));
        result = ((result* 31)+((this.stats == null)? 0 :this.stats.hashCode()));
        result = ((result* 31)+((this.imageUrl == null)? 0 :this.imageUrl.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.jerseyNumber == null)? 0 :this.jerseyNumber.hashCode()));
        result = ((result* 31)+((this.position == null)? 0 :this.position.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.actions == null)? 0 :this.actions.hashCode()));
        result = ((result* 31)+((this.playerId == null)? 0 :this.playerId.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BoxscorePlayerRow) == false) {
            return false;
        }
        BoxscorePlayerRow rhs = ((BoxscorePlayerRow) other);
        return ((((((((((this.starter == rhs.starter)||((this.starter!= null)&&this.starter.equals(rhs.starter)))&&((this.stats == rhs.stats)||((this.stats!= null)&&this.stats.equals(rhs.stats))))&&((this.imageUrl == rhs.imageUrl)||((this.imageUrl!= null)&&this.imageUrl.equals(rhs.imageUrl))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.jerseyNumber == rhs.jerseyNumber)||((this.jerseyNumber!= null)&&this.jerseyNumber.equals(rhs.jerseyNumber))))&&((this.position == rhs.position)||((this.position!= null)&&this.position.equals(rhs.position))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.actions == rhs.actions)||((this.actions!= null)&&this.actions.equals(rhs.actions))))&&((this.playerId == rhs.playerId)||((this.playerId!= null)&&this.playerId.equals(rhs.playerId))));
    }

}
