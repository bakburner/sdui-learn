
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
import jakarta.validation.constraints.Pattern;


/**
 * Vertically-scrollable month-grid calendar with per-date game metadata. All date fields are ET-anchored (America/New_York) ISO YYYY-MM-DD. defaultDate is the league's current anchor date; drives the 'today' visual highlight and the initial scroll position.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "stateKey",
    "selectedDate",
    "defaultDate",
    "minDate",
    "maxDate",
    "dateMetadata",
    "onDateSelected"
})
@Generated("jsonschema2pojo")
public class CalendarMonthList {

    /**
     * Screen-state key for the selected ISO date.
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    @JsonPropertyDescription("Screen-state key for the selected ISO date.")
    @NotNull
    private String stateKey;
    /**
     * ISO YYYY-MM-DD (ET) for initial selection.
     * (Required)
     * 
     */
    @JsonProperty("selectedDate")
    @JsonPropertyDescription("ISO YYYY-MM-DD (ET) for initial selection.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    @NotNull
    private String selectedDate;
    /**
     * ISO YYYY-MM-DD (ET) for the league's current anchor date.
     * (Required)
     * 
     */
    @JsonProperty("defaultDate")
    @JsonPropertyDescription("ISO YYYY-MM-DD (ET) for the league's current anchor date.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    @NotNull
    private String defaultDate;
    /**
     * ISO YYYY-MM-DD (ET) for the earliest selectable date.
     * 
     */
    @JsonProperty("minDate")
    @JsonPropertyDescription("ISO YYYY-MM-DD (ET) for the earliest selectable date.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String minDate;
    /**
     * ISO YYYY-MM-DD (ET) for the latest selectable date.
     * 
     */
    @JsonProperty("maxDate")
    @JsonPropertyDescription("ISO YYYY-MM-DD (ET) for the latest selectable date.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String maxDate;
    /**
     * Map of ISO date string to metadata for that date. Only dates with games are present; absent dates have zero games.
     * 
     */
    @JsonProperty("dateMetadata")
    @JsonPropertyDescription("Map of ISO date string to metadata for that date. Only dates with games are present; absent dates have zero games.")
    @Valid
    private DateMetadata dateMetadata;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("onDateSelected")
    @Valid
    @NotNull
    private Action onDateSelected;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Screen-state key for the selected ISO date.
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    public String getStateKey() {
        return stateKey;
    }

    /**
     * Screen-state key for the selected ISO date.
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public CalendarMonthList withStateKey(String stateKey) {
        this.stateKey = stateKey;
        return this;
    }

    /**
     * ISO YYYY-MM-DD (ET) for initial selection.
     * (Required)
     * 
     */
    @JsonProperty("selectedDate")
    public String getSelectedDate() {
        return selectedDate;
    }

    /**
     * ISO YYYY-MM-DD (ET) for initial selection.
     * (Required)
     * 
     */
    @JsonProperty("selectedDate")
    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public CalendarMonthList withSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
        return this;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the league's current anchor date.
     * (Required)
     * 
     */
    @JsonProperty("defaultDate")
    public String getDefaultDate() {
        return defaultDate;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the league's current anchor date.
     * (Required)
     * 
     */
    @JsonProperty("defaultDate")
    public void setDefaultDate(String defaultDate) {
        this.defaultDate = defaultDate;
    }

    public CalendarMonthList withDefaultDate(String defaultDate) {
        this.defaultDate = defaultDate;
        return this;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the earliest selectable date.
     * 
     */
    @JsonProperty("minDate")
    public String getMinDate() {
        return minDate;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the earliest selectable date.
     * 
     */
    @JsonProperty("minDate")
    public void setMinDate(String minDate) {
        this.minDate = minDate;
    }

    public CalendarMonthList withMinDate(String minDate) {
        this.minDate = minDate;
        return this;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the latest selectable date.
     * 
     */
    @JsonProperty("maxDate")
    public String getMaxDate() {
        return maxDate;
    }

    /**
     * ISO YYYY-MM-DD (ET) for the latest selectable date.
     * 
     */
    @JsonProperty("maxDate")
    public void setMaxDate(String maxDate) {
        this.maxDate = maxDate;
    }

    public CalendarMonthList withMaxDate(String maxDate) {
        this.maxDate = maxDate;
        return this;
    }

    /**
     * Map of ISO date string to metadata for that date. Only dates with games are present; absent dates have zero games.
     * 
     */
    @JsonProperty("dateMetadata")
    public DateMetadata getDateMetadata() {
        return dateMetadata;
    }

    /**
     * Map of ISO date string to metadata for that date. Only dates with games are present; absent dates have zero games.
     * 
     */
    @JsonProperty("dateMetadata")
    public void setDateMetadata(DateMetadata dateMetadata) {
        this.dateMetadata = dateMetadata;
    }

    public CalendarMonthList withDateMetadata(DateMetadata dateMetadata) {
        this.dateMetadata = dateMetadata;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("onDateSelected")
    public Action getOnDateSelected() {
        return onDateSelected;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("onDateSelected")
    public void setOnDateSelected(Action onDateSelected) {
        this.onDateSelected = onDateSelected;
    }

    public CalendarMonthList withOnDateSelected(Action onDateSelected) {
        this.onDateSelected = onDateSelected;
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

    public CalendarMonthList withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CalendarMonthList.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("stateKey");
        sb.append('=');
        sb.append(((this.stateKey == null)?"<null>":this.stateKey));
        sb.append(',');
        sb.append("selectedDate");
        sb.append('=');
        sb.append(((this.selectedDate == null)?"<null>":this.selectedDate));
        sb.append(',');
        sb.append("defaultDate");
        sb.append('=');
        sb.append(((this.defaultDate == null)?"<null>":this.defaultDate));
        sb.append(',');
        sb.append("minDate");
        sb.append('=');
        sb.append(((this.minDate == null)?"<null>":this.minDate));
        sb.append(',');
        sb.append("maxDate");
        sb.append('=');
        sb.append(((this.maxDate == null)?"<null>":this.maxDate));
        sb.append(',');
        sb.append("dateMetadata");
        sb.append('=');
        sb.append(((this.dateMetadata == null)?"<null>":this.dateMetadata));
        sb.append(',');
        sb.append("onDateSelected");
        sb.append('=');
        sb.append(((this.onDateSelected == null)?"<null>":this.onDateSelected));
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
        result = ((result* 31)+((this.defaultDate == null)? 0 :this.defaultDate.hashCode()));
        result = ((result* 31)+((this.minDate == null)? 0 :this.minDate.hashCode()));
        result = ((result* 31)+((this.dateMetadata == null)? 0 :this.dateMetadata.hashCode()));
        result = ((result* 31)+((this.maxDate == null)? 0 :this.maxDate.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.selectedDate == null)? 0 :this.selectedDate.hashCode()));
        result = ((result* 31)+((this.onDateSelected == null)? 0 :this.onDateSelected.hashCode()));
        result = ((result* 31)+((this.stateKey == null)? 0 :this.stateKey.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CalendarMonthList) == false) {
            return false;
        }
        CalendarMonthList rhs = ((CalendarMonthList) other);
        return (((((((((this.defaultDate == rhs.defaultDate)||((this.defaultDate!= null)&&this.defaultDate.equals(rhs.defaultDate)))&&((this.minDate == rhs.minDate)||((this.minDate!= null)&&this.minDate.equals(rhs.minDate))))&&((this.dateMetadata == rhs.dateMetadata)||((this.dateMetadata!= null)&&this.dateMetadata.equals(rhs.dateMetadata))))&&((this.maxDate == rhs.maxDate)||((this.maxDate!= null)&&this.maxDate.equals(rhs.maxDate))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.selectedDate == rhs.selectedDate)||((this.selectedDate!= null)&&this.selectedDate.equals(rhs.selectedDate))))&&((this.onDateSelected == rhs.onDateSelected)||((this.onDateSelected!= null)&&this.onDateSelected.equals(rhs.onDateSelected))))&&((this.stateKey == rhs.stateKey)||((this.stateKey!= null)&&this.stateKey.equals(rhs.stateKey))));
    }

}
