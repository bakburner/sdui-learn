
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "bindings",
    "stringKeys"
})
@Generated("jsonschema2pojo")
public class DataBinding {

    @JsonProperty("bindings")
    @Valid
    private List<DataBindingPath> bindings = new ArrayList<DataBindingPath>();
    /**
     * Optional map of targetPath to translation key for client-side i18n resolution on bound fields
     * 
     */
    @JsonProperty("stringKeys")
    @JsonPropertyDescription("Optional map of targetPath to translation key for client-side i18n resolution on bound fields")
    @Valid
    private StringKeys stringKeys;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    @JsonProperty("bindings")
    public List<DataBindingPath> getBindings() {
        return bindings;
    }

    @JsonProperty("bindings")
    public void setBindings(List<DataBindingPath> bindings) {
        this.bindings = bindings;
    }

    public DataBinding withBindings(List<DataBindingPath> bindings) {
        this.bindings = bindings;
        return this;
    }

    /**
     * Optional map of targetPath to translation key for client-side i18n resolution on bound fields
     * 
     */
    @JsonProperty("stringKeys")
    public StringKeys getStringKeys() {
        return stringKeys;
    }

    /**
     * Optional map of targetPath to translation key for client-side i18n resolution on bound fields
     * 
     */
    @JsonProperty("stringKeys")
    public void setStringKeys(StringKeys stringKeys) {
        this.stringKeys = stringKeys;
    }

    public DataBinding withStringKeys(StringKeys stringKeys) {
        this.stringKeys = stringKeys;
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

    public DataBinding withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DataBinding.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("bindings");
        sb.append('=');
        sb.append(((this.bindings == null)?"<null>":this.bindings));
        sb.append(',');
        sb.append("stringKeys");
        sb.append('=');
        sb.append(((this.stringKeys == null)?"<null>":this.stringKeys));
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
        result = ((result* 31)+((this.stringKeys == null)? 0 :this.stringKeys.hashCode()));
        result = ((result* 31)+((this.bindings == null)? 0 :this.bindings.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof DataBinding) == false) {
            return false;
        }
        DataBinding rhs = ((DataBinding) other);
        return ((((this.stringKeys == rhs.stringKeys)||((this.stringKeys!= null)&&this.stringKeys.equals(rhs.stringKeys)))&&((this.bindings == rhs.bindings)||((this.bindings!= null)&&this.bindings.equals(rhs.bindings))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
