
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
 * Tabbed navigation with dynamic content sections per tab. Optional ui is the tab header/control row only; tabContents hosts nested sections. Tab selection uses section.subsections mutate actions.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ui",
    "tabs",
    "defaultTab",
    "stateKey",
    "tabContents"
})
@Generated("jsonschema2pojo")
public class TabGroup {

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * 
     */
    @JsonProperty("ui")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    private AtomicElement ui;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tabs")
    @Valid
    @NotNull
    private List<TabData> tabs = new ArrayList<TabData>();
    @JsonProperty("defaultTab")
    private String defaultTab;
    @JsonProperty("stateKey")
    private String stateKey;
    @JsonProperty("tabContents")
    @Valid
    private TabContents tabContents;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

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

    public TabGroup withUi(AtomicElement ui) {
        this.ui = ui;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tabs")
    public List<TabData> getTabs() {
        return tabs;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("tabs")
    public void setTabs(List<TabData> tabs) {
        this.tabs = tabs;
    }

    public TabGroup withTabs(List<TabData> tabs) {
        this.tabs = tabs;
        return this;
    }

    @JsonProperty("defaultTab")
    public String getDefaultTab() {
        return defaultTab;
    }

    @JsonProperty("defaultTab")
    public void setDefaultTab(String defaultTab) {
        this.defaultTab = defaultTab;
    }

    public TabGroup withDefaultTab(String defaultTab) {
        this.defaultTab = defaultTab;
        return this;
    }

    @JsonProperty("stateKey")
    public String getStateKey() {
        return stateKey;
    }

    @JsonProperty("stateKey")
    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public TabGroup withStateKey(String stateKey) {
        this.stateKey = stateKey;
        return this;
    }

    @JsonProperty("tabContents")
    public TabContents getTabContents() {
        return tabContents;
    }

    @JsonProperty("tabContents")
    public void setTabContents(TabContents tabContents) {
        this.tabContents = tabContents;
    }

    public TabGroup withTabContents(TabContents tabContents) {
        this.tabContents = tabContents;
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

    public TabGroup withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TabGroup.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("ui");
        sb.append('=');
        sb.append(((this.ui == null)?"<null>":this.ui));
        sb.append(',');
        sb.append("tabs");
        sb.append('=');
        sb.append(((this.tabs == null)?"<null>":this.tabs));
        sb.append(',');
        sb.append("defaultTab");
        sb.append('=');
        sb.append(((this.defaultTab == null)?"<null>":this.defaultTab));
        sb.append(',');
        sb.append("stateKey");
        sb.append('=');
        sb.append(((this.stateKey == null)?"<null>":this.stateKey));
        sb.append(',');
        sb.append("tabContents");
        sb.append('=');
        sb.append(((this.tabContents == null)?"<null>":this.tabContents));
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
        result = ((result* 31)+((this.tabs == null)? 0 :this.tabs.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.ui == null)? 0 :this.ui.hashCode()));
        result = ((result* 31)+((this.tabContents == null)? 0 :this.tabContents.hashCode()));
        result = ((result* 31)+((this.defaultTab == null)? 0 :this.defaultTab.hashCode()));
        result = ((result* 31)+((this.stateKey == null)? 0 :this.stateKey.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TabGroup) == false) {
            return false;
        }
        TabGroup rhs = ((TabGroup) other);
        return (((((((this.tabs == rhs.tabs)||((this.tabs!= null)&&this.tabs.equals(rhs.tabs)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.ui == rhs.ui)||((this.ui!= null)&&this.ui.equals(rhs.ui))))&&((this.tabContents == rhs.tabContents)||((this.tabContents!= null)&&this.tabContents.equals(rhs.tabContents))))&&((this.defaultTab == rhs.defaultTab)||((this.defaultTab!= null)&&this.defaultTab.equals(rhs.defaultTab))))&&((this.stateKey == rhs.stateKey)||((this.stateKey!= null)&&this.stateKey.equals(rhs.stateKey))));
    }

}
