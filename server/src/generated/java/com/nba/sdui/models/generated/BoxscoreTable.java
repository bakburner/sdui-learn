
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
 * Typed tabular data for an NBA-style boxscore (one per team)
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "teamTricode",
    "teamName",
    "teamColor",
    "teamLogoUrl",
    "columns",
    "players",
    "teamTotals",
    "sortStateKey",
    "sortDirectionStateKey",
    "emptyMessage"
})
@Generated("jsonschema2pojo")
public class BoxscoreTable {

    /**
     * Three-letter team code, e.g. 'BOS'
     * (Required)
     * 
     */
    @JsonProperty("teamTricode")
    @JsonPropertyDescription("Three-letter team code, e.g. 'BOS'")
    @NotNull
    private String teamTricode;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("teamName")
    @NotNull
    private String teamName;
    /**
     * Hex colour for team accent
     * 
     */
    @JsonProperty("teamColor")
    @JsonPropertyDescription("Hex colour for team accent")
    private String teamColor;
    @JsonProperty("teamLogoUrl")
    private String teamLogoUrl;
    /**
     * Ordered list of column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    @JsonPropertyDescription("Ordered list of column definitions; clients render left-to-right")
    @Valid
    @NotNull
    private List<BoxscoreColumnDefinition> columns = new ArrayList<BoxscoreColumnDefinition>();
    /**
     * Player rows ordered by server (starters first, then bench)
     * (Required)
     * 
     */
    @JsonProperty("players")
    @JsonPropertyDescription("Player rows ordered by server (starters first, then bench)")
    @Valid
    @NotNull
    private List<BoxscorePlayerRow> players = new ArrayList<BoxscorePlayerRow>();
    /**
     * Key-value map of stat abbreviation to value for one player row
     * 
     */
    @JsonProperty("teamTotals")
    @JsonPropertyDescription("Key-value map of stat abbreviation to value for one player row")
    @Valid
    private BoxscorePlayerStatistics teamTotals;
    /**
     * Screen-state key holding the current sort column key
     * 
     */
    @JsonProperty("sortStateKey")
    @JsonPropertyDescription("Screen-state key holding the current sort column key")
    private String sortStateKey;
    /**
     * Screen-state key holding the current sort direction (asc/desc)
     * 
     */
    @JsonProperty("sortDirectionStateKey")
    @JsonPropertyDescription("Screen-state key holding the current sort direction (asc/desc)")
    private String sortDirectionStateKey;
    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    @JsonPropertyDescription("Text shown when no player rows are available")
    private String emptyMessage;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Three-letter team code, e.g. 'BOS'
     * (Required)
     * 
     */
    @JsonProperty("teamTricode")
    public String getTeamTricode() {
        return teamTricode;
    }

    /**
     * Three-letter team code, e.g. 'BOS'
     * (Required)
     * 
     */
    @JsonProperty("teamTricode")
    public void setTeamTricode(String teamTricode) {
        this.teamTricode = teamTricode;
    }

    public BoxscoreTable withTeamTricode(String teamTricode) {
        this.teamTricode = teamTricode;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("teamName")
    public String getTeamName() {
        return teamName;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("teamName")
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public BoxscoreTable withTeamName(String teamName) {
        this.teamName = teamName;
        return this;
    }

    /**
     * Hex colour for team accent
     * 
     */
    @JsonProperty("teamColor")
    public String getTeamColor() {
        return teamColor;
    }

    /**
     * Hex colour for team accent
     * 
     */
    @JsonProperty("teamColor")
    public void setTeamColor(String teamColor) {
        this.teamColor = teamColor;
    }

    public BoxscoreTable withTeamColor(String teamColor) {
        this.teamColor = teamColor;
        return this;
    }

    @JsonProperty("teamLogoUrl")
    public String getTeamLogoUrl() {
        return teamLogoUrl;
    }

    @JsonProperty("teamLogoUrl")
    public void setTeamLogoUrl(String teamLogoUrl) {
        this.teamLogoUrl = teamLogoUrl;
    }

    public BoxscoreTable withTeamLogoUrl(String teamLogoUrl) {
        this.teamLogoUrl = teamLogoUrl;
        return this;
    }

    /**
     * Ordered list of column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    public List<BoxscoreColumnDefinition> getColumns() {
        return columns;
    }

    /**
     * Ordered list of column definitions; clients render left-to-right
     * (Required)
     * 
     */
    @JsonProperty("columns")
    public void setColumns(List<BoxscoreColumnDefinition> columns) {
        this.columns = columns;
    }

    public BoxscoreTable withColumns(List<BoxscoreColumnDefinition> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Player rows ordered by server (starters first, then bench)
     * (Required)
     * 
     */
    @JsonProperty("players")
    public List<BoxscorePlayerRow> getPlayers() {
        return players;
    }

    /**
     * Player rows ordered by server (starters first, then bench)
     * (Required)
     * 
     */
    @JsonProperty("players")
    public void setPlayers(List<BoxscorePlayerRow> players) {
        this.players = players;
    }

    public BoxscoreTable withPlayers(List<BoxscorePlayerRow> players) {
        this.players = players;
        return this;
    }

    /**
     * Key-value map of stat abbreviation to value for one player row
     * 
     */
    @JsonProperty("teamTotals")
    public BoxscorePlayerStatistics getTeamTotals() {
        return teamTotals;
    }

    /**
     * Key-value map of stat abbreviation to value for one player row
     * 
     */
    @JsonProperty("teamTotals")
    public void setTeamTotals(BoxscorePlayerStatistics teamTotals) {
        this.teamTotals = teamTotals;
    }

    public BoxscoreTable withTeamTotals(BoxscorePlayerStatistics teamTotals) {
        this.teamTotals = teamTotals;
        return this;
    }

    /**
     * Screen-state key holding the current sort column key
     * 
     */
    @JsonProperty("sortStateKey")
    public String getSortStateKey() {
        return sortStateKey;
    }

    /**
     * Screen-state key holding the current sort column key
     * 
     */
    @JsonProperty("sortStateKey")
    public void setSortStateKey(String sortStateKey) {
        this.sortStateKey = sortStateKey;
    }

    public BoxscoreTable withSortStateKey(String sortStateKey) {
        this.sortStateKey = sortStateKey;
        return this;
    }

    /**
     * Screen-state key holding the current sort direction (asc/desc)
     * 
     */
    @JsonProperty("sortDirectionStateKey")
    public String getSortDirectionStateKey() {
        return sortDirectionStateKey;
    }

    /**
     * Screen-state key holding the current sort direction (asc/desc)
     * 
     */
    @JsonProperty("sortDirectionStateKey")
    public void setSortDirectionStateKey(String sortDirectionStateKey) {
        this.sortDirectionStateKey = sortDirectionStateKey;
    }

    public BoxscoreTable withSortDirectionStateKey(String sortDirectionStateKey) {
        this.sortDirectionStateKey = sortDirectionStateKey;
        return this;
    }

    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    public String getEmptyMessage() {
        return emptyMessage;
    }

    /**
     * Text shown when no player rows are available
     * 
     */
    @JsonProperty("emptyMessage")
    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    public BoxscoreTable withEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
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

    public BoxscoreTable withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BoxscoreTable.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("teamTricode");
        sb.append('=');
        sb.append(((this.teamTricode == null)?"<null>":this.teamTricode));
        sb.append(',');
        sb.append("teamName");
        sb.append('=');
        sb.append(((this.teamName == null)?"<null>":this.teamName));
        sb.append(',');
        sb.append("teamColor");
        sb.append('=');
        sb.append(((this.teamColor == null)?"<null>":this.teamColor));
        sb.append(',');
        sb.append("teamLogoUrl");
        sb.append('=');
        sb.append(((this.teamLogoUrl == null)?"<null>":this.teamLogoUrl));
        sb.append(',');
        sb.append("columns");
        sb.append('=');
        sb.append(((this.columns == null)?"<null>":this.columns));
        sb.append(',');
        sb.append("players");
        sb.append('=');
        sb.append(((this.players == null)?"<null>":this.players));
        sb.append(',');
        sb.append("teamTotals");
        sb.append('=');
        sb.append(((this.teamTotals == null)?"<null>":this.teamTotals));
        sb.append(',');
        sb.append("sortStateKey");
        sb.append('=');
        sb.append(((this.sortStateKey == null)?"<null>":this.sortStateKey));
        sb.append(',');
        sb.append("sortDirectionStateKey");
        sb.append('=');
        sb.append(((this.sortDirectionStateKey == null)?"<null>":this.sortDirectionStateKey));
        sb.append(',');
        sb.append("emptyMessage");
        sb.append('=');
        sb.append(((this.emptyMessage == null)?"<null>":this.emptyMessage));
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
        result = ((result* 31)+((this.teamName == null)? 0 :this.teamName.hashCode()));
        result = ((result* 31)+((this.emptyMessage == null)? 0 :this.emptyMessage.hashCode()));
        result = ((result* 31)+((this.teamTotals == null)? 0 :this.teamTotals.hashCode()));
        result = ((result* 31)+((this.sortStateKey == null)? 0 :this.sortStateKey.hashCode()));
        result = ((result* 31)+((this.columns == null)? 0 :this.columns.hashCode()));
        result = ((result* 31)+((this.players == null)? 0 :this.players.hashCode()));
        result = ((result* 31)+((this.sortDirectionStateKey == null)? 0 :this.sortDirectionStateKey.hashCode()));
        result = ((result* 31)+((this.teamTricode == null)? 0 :this.teamTricode.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.teamLogoUrl == null)? 0 :this.teamLogoUrl.hashCode()));
        result = ((result* 31)+((this.teamColor == null)? 0 :this.teamColor.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BoxscoreTable) == false) {
            return false;
        }
        BoxscoreTable rhs = ((BoxscoreTable) other);
        return ((((((((((((this.teamName == rhs.teamName)||((this.teamName!= null)&&this.teamName.equals(rhs.teamName)))&&((this.emptyMessage == rhs.emptyMessage)||((this.emptyMessage!= null)&&this.emptyMessage.equals(rhs.emptyMessage))))&&((this.teamTotals == rhs.teamTotals)||((this.teamTotals!= null)&&this.teamTotals.equals(rhs.teamTotals))))&&((this.sortStateKey == rhs.sortStateKey)||((this.sortStateKey!= null)&&this.sortStateKey.equals(rhs.sortStateKey))))&&((this.columns == rhs.columns)||((this.columns!= null)&&this.columns.equals(rhs.columns))))&&((this.players == rhs.players)||((this.players!= null)&&this.players.equals(rhs.players))))&&((this.sortDirectionStateKey == rhs.sortDirectionStateKey)||((this.sortDirectionStateKey!= null)&&this.sortDirectionStateKey.equals(rhs.sortDirectionStateKey))))&&((this.teamTricode == rhs.teamTricode)||((this.teamTricode!= null)&&this.teamTricode.equals(rhs.teamTricode))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.teamLogoUrl == rhs.teamLogoUrl)||((this.teamLogoUrl!= null)&&this.teamLogoUrl.equals(rhs.teamLogoUrl))))&&((this.teamColor == rhs.teamColor)||((this.teamColor!= null)&&this.teamColor.equals(rhs.teamColor))));
    }

}
