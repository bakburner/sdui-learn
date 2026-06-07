
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
 * One ranked player row in a season leaders table
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "rank",
    "playerId",
    "name",
    "team",
    "imageUrl",
    "stats",
    "actions"
})
@Generated("jsonschema2pojo")
public class LeadersPlayerRow {

    /**
     * Ranking position (1-based)
     * (Required)
     * 
     */
    @JsonProperty("rank")
    @JsonPropertyDescription("Ranking position (1-based)")
    @NotNull
    private Integer rank;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("playerId")
    @NotNull
    private String playerId;
    /**
     * Display name, e.g. 'Luka Dončić'
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("Display name, e.g. 'Luka Don\u010di\u0107'")
    @NotNull
    private String name;
    /**
     * Team tricode, e.g. 'LAL'
     * (Required)
     * 
     */
    @JsonProperty("team")
    @JsonPropertyDescription("Team tricode, e.g. 'LAL'")
    @NotNull
    private String team;
    @JsonProperty("imageUrl")
    private String imageUrl;
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
     * Ranking position (1-based)
     * (Required)
     * 
     */
    @JsonProperty("rank")
    public Integer getRank() {
        return rank;
    }

    /**
     * Ranking position (1-based)
     * (Required)
     * 
     */
    @JsonProperty("rank")
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public LeadersPlayerRow withRank(Integer rank) {
        this.rank = rank;
        return this;
    }

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

    public LeadersPlayerRow withPlayerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    /**
     * Display name, e.g. 'Luka Dončić'
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Display name, e.g. 'Luka Dončić'
     * (Required)
     * 
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public LeadersPlayerRow withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Team tricode, e.g. 'LAL'
     * (Required)
     * 
     */
    @JsonProperty("team")
    public String getTeam() {
        return team;
    }

    /**
     * Team tricode, e.g. 'LAL'
     * (Required)
     * 
     */
    @JsonProperty("team")
    public void setTeam(String team) {
        this.team = team;
    }

    public LeadersPlayerRow withTeam(String team) {
        this.team = team;
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

    public LeadersPlayerRow withImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public LeadersPlayerRow withStats(BoxscorePlayerStatistics stats) {
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

    public LeadersPlayerRow withActions(List<Action> actions) {
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

    public LeadersPlayerRow withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(LeadersPlayerRow.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("rank");
        sb.append('=');
        sb.append(((this.rank == null)?"<null>":this.rank));
        sb.append(',');
        sb.append("playerId");
        sb.append('=');
        sb.append(((this.playerId == null)?"<null>":this.playerId));
        sb.append(',');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("team");
        sb.append('=');
        sb.append(((this.team == null)?"<null>":this.team));
        sb.append(',');
        sb.append("imageUrl");
        sb.append('=');
        sb.append(((this.imageUrl == null)?"<null>":this.imageUrl));
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
        result = ((result* 31)+((this.stats == null)? 0 :this.stats.hashCode()));
        result = ((result* 31)+((this.imageUrl == null)? 0 :this.imageUrl.hashCode()));
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.rank == null)? 0 :this.rank.hashCode()));
        result = ((result* 31)+((this.team == null)? 0 :this.team.hashCode()));
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
        if ((other instanceof LeadersPlayerRow) == false) {
            return false;
        }
        LeadersPlayerRow rhs = ((LeadersPlayerRow) other);
        return (((((((((this.stats == rhs.stats)||((this.stats!= null)&&this.stats.equals(rhs.stats)))&&((this.imageUrl == rhs.imageUrl)||((this.imageUrl!= null)&&this.imageUrl.equals(rhs.imageUrl))))&&((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name))))&&((this.rank == rhs.rank)||((this.rank!= null)&&this.rank.equals(rhs.rank))))&&((this.team == rhs.team)||((this.team!= null)&&this.team.equals(rhs.team))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.actions == rhs.actions)||((this.actions!= null)&&this.actions.equals(rhs.actions))))&&((this.playerId == rhs.playerId)||((this.playerId!= null)&&this.playerId.equals(rhs.playerId))));
    }

}
