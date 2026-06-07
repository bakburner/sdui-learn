
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
import jakarta.validation.constraints.NotNull;


/**
 * Defines a single column in the boxscore table
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "label",
    "sortable",
    "highlighted",
    "width"
})
@Generated("jsonschema2pojo")
public class BoxscoreColumnDefinition {

    /**
     * Property key on each player's stats object that supplies this column's value
     * (Required)
     * 
     */
    @JsonProperty("key")
    @JsonPropertyDescription("Property key on each player's stats object that supplies this column's value")
    @NotNull
    private String key;
    /**
     * Column header text displayed to the user
     * (Required)
     * 
     */
    @JsonProperty("label")
    @JsonPropertyDescription("Column header text displayed to the user")
    @NotNull
    private String label;
    /**
     * Whether this column supports client-side sorting
     * 
     */
    @JsonProperty("sortable")
    @JsonPropertyDescription("Whether this column supports client-side sorting")
    private Boolean sortable = true;
    /**
     * Whether this column should be visually emphasised (e.g., bold)
     * 
     */
    @JsonProperty("highlighted")
    @JsonPropertyDescription("Whether this column should be visually emphasised (e.g., bold)")
    private Boolean highlighted = false;
    /**
     * Optional hint for column width (e.g. 'auto', '64px', '1fr')
     * 
     */
    @JsonProperty("width")
    @JsonPropertyDescription("Optional hint for column width (e.g. 'auto', '64px', '1fr')")
    private String width;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Property key on each player's stats object that supplies this column's value
     * (Required)
     * 
     */
    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    /**
     * Property key on each player's stats object that supplies this column's value
     * (Required)
     * 
     */
    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    public BoxscoreColumnDefinition withKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Column header text displayed to the user
     * (Required)
     * 
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    /**
     * Column header text displayed to the user
     * (Required)
     * 
     */
    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public BoxscoreColumnDefinition withLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Whether this column supports client-side sorting
     * 
     */
    @JsonProperty("sortable")
    public Boolean getSortable() {
        return sortable;
    }

    /**
     * Whether this column supports client-side sorting
     * 
     */
    @JsonProperty("sortable")
    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    public BoxscoreColumnDefinition withSortable(Boolean sortable) {
        this.sortable = sortable;
        return this;
    }

    /**
     * Whether this column should be visually emphasised (e.g., bold)
     * 
     */
    @JsonProperty("highlighted")
    public Boolean getHighlighted() {
        return highlighted;
    }

    /**
     * Whether this column should be visually emphasised (e.g., bold)
     * 
     */
    @JsonProperty("highlighted")
    public void setHighlighted(Boolean highlighted) {
        this.highlighted = highlighted;
    }

    public BoxscoreColumnDefinition withHighlighted(Boolean highlighted) {
        this.highlighted = highlighted;
        return this;
    }

    /**
     * Optional hint for column width (e.g. 'auto', '64px', '1fr')
     * 
     */
    @JsonProperty("width")
    public String getWidth() {
        return width;
    }

    /**
     * Optional hint for column width (e.g. 'auto', '64px', '1fr')
     * 
     */
    @JsonProperty("width")
    public void setWidth(String width) {
        this.width = width;
    }

    public BoxscoreColumnDefinition withWidth(String width) {
        this.width = width;
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

    public BoxscoreColumnDefinition withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BoxscoreColumnDefinition.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null)?"<null>":this.key));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("sortable");
        sb.append('=');
        sb.append(((this.sortable == null)?"<null>":this.sortable));
        sb.append(',');
        sb.append("highlighted");
        sb.append('=');
        sb.append(((this.highlighted == null)?"<null>":this.highlighted));
        sb.append(',');
        sb.append("width");
        sb.append('=');
        sb.append(((this.width == null)?"<null>":this.width));
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
        result = ((result* 31)+((this.width == null)? 0 :this.width.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.sortable == null)? 0 :this.sortable.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.key == null)? 0 :this.key.hashCode()));
        result = ((result* 31)+((this.highlighted == null)? 0 :this.highlighted.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof BoxscoreColumnDefinition) == false) {
            return false;
        }
        BoxscoreColumnDefinition rhs = ((BoxscoreColumnDefinition) other);
        return (((((((this.width == rhs.width)||((this.width!= null)&&this.width.equals(rhs.width)))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.sortable == rhs.sortable)||((this.sortable!= null)&&this.sortable.equals(rhs.sortable))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.key == rhs.key)||((this.key!= null)&&this.key.equals(rhs.key))))&&((this.highlighted == rhs.highlighted)||((this.highlighted!= null)&&this.highlighted.equals(rhs.highlighted))));
    }

}
