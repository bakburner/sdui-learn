
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
    "gameCount",
    "hasTeamGame"
})
@Generated("jsonschema2pojo")
public class DateMetadataProperty {

    /**
     * Number of games on this date.
     * 
     */
    @JsonProperty("gameCount")
    @JsonPropertyDescription("Number of games on this date.")
    @DecimalMin("0")
    private Integer gameCount;
    /**
     * True if a user-favorited team plays on this date.
     * 
     */
    @JsonProperty("hasTeamGame")
    @JsonPropertyDescription("True if a user-favorited team plays on this date.")
    private Boolean hasTeamGame;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Number of games on this date.
     * 
     */
    @JsonProperty("gameCount")
    public Integer getGameCount() {
        return gameCount;
    }

    /**
     * Number of games on this date.
     * 
     */
    @JsonProperty("gameCount")
    public void setGameCount(Integer gameCount) {
        this.gameCount = gameCount;
    }

    public DateMetadataProperty withGameCount(Integer gameCount) {
        this.gameCount = gameCount;
        return this;
    }

    /**
     * True if a user-favorited team plays on this date.
     * 
     */
    @JsonProperty("hasTeamGame")
    public Boolean getHasTeamGame() {
        return hasTeamGame;
    }

    /**
     * True if a user-favorited team plays on this date.
     * 
     */
    @JsonProperty("hasTeamGame")
    public void setHasTeamGame(Boolean hasTeamGame) {
        this.hasTeamGame = hasTeamGame;
    }

    public DateMetadataProperty withHasTeamGame(Boolean hasTeamGame) {
        this.hasTeamGame = hasTeamGame;
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

    public DateMetadataProperty withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DateMetadataProperty.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("gameCount");
        sb.append('=');
        sb.append(((this.gameCount == null)?"<null>":this.gameCount));
        sb.append(',');
        sb.append("hasTeamGame");
        sb.append('=');
        sb.append(((this.hasTeamGame == null)?"<null>":this.hasTeamGame));
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
        result = ((result* 31)+((this.gameCount == null)? 0 :this.gameCount.hashCode()));
        result = ((result* 31)+((this.hasTeamGame == null)? 0 :this.hasTeamGame.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DateMetadataProperty) == false) {
            return false;
        }
        DateMetadataProperty rhs = ((DateMetadataProperty) other);
        return ((((this.gameCount == rhs.gameCount)||((this.gameCount!= null)&&this.gameCount.equals(rhs.gameCount)))&&((this.hasTeamGame == rhs.hasTeamGame)||((this.hasTeamGame!= null)&&this.hasTeamGame.equals(rhs.hasTeamGame))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
