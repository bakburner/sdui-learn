
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
 * Server-driven form section with typed fields bound to screen state
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fields",
    "submitAction",
    "submitLabel",
    "layout"
})
@Generated("jsonschema2pojo")
public class Form {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fields")
    @Valid
    @NotNull
    private List<FormField> fields = new ArrayList<FormField>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("submitAction")
    @Valid
    @NotNull
    private Action submitAction;
    @JsonProperty("submitLabel")
    private String submitLabel = "Submit";
    /**
     * Layout hint for field arrangement
     * 
     */
    @JsonProperty("layout")
    @JsonPropertyDescription("Layout hint for field arrangement")
    private Form.Layout layout = Form.Layout.fromValue("vertical");
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fields")
    public List<FormField> getFields() {
        return fields;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fields")
    public void setFields(List<FormField> fields) {
        this.fields = fields;
    }

    public Form withFields(List<FormField> fields) {
        this.fields = fields;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("submitAction")
    public Action getSubmitAction() {
        return submitAction;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("submitAction")
    public void setSubmitAction(Action submitAction) {
        this.submitAction = submitAction;
    }

    public Form withSubmitAction(Action submitAction) {
        this.submitAction = submitAction;
        return this;
    }

    @JsonProperty("submitLabel")
    public String getSubmitLabel() {
        return submitLabel;
    }

    @JsonProperty("submitLabel")
    public void setSubmitLabel(String submitLabel) {
        this.submitLabel = submitLabel;
    }

    public Form withSubmitLabel(String submitLabel) {
        this.submitLabel = submitLabel;
        return this;
    }

    /**
     * Layout hint for field arrangement
     * 
     */
    @JsonProperty("layout")
    public Form.Layout getLayout() {
        return layout;
    }

    /**
     * Layout hint for field arrangement
     * 
     */
    @JsonProperty("layout")
    public void setLayout(Form.Layout layout) {
        this.layout = layout;
    }

    public Form withLayout(Form.Layout layout) {
        this.layout = layout;
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

    public Form withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Form.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("fields");
        sb.append('=');
        sb.append(((this.fields == null)?"<null>":this.fields));
        sb.append(',');
        sb.append("submitAction");
        sb.append('=');
        sb.append(((this.submitAction == null)?"<null>":this.submitAction));
        sb.append(',');
        sb.append("submitLabel");
        sb.append('=');
        sb.append(((this.submitLabel == null)?"<null>":this.submitLabel));
        sb.append(',');
        sb.append("layout");
        sb.append('=');
        sb.append(((this.layout == null)?"<null>":this.layout));
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
        result = ((result* 31)+((this.submitAction == null)? 0 :this.submitAction.hashCode()));
        result = ((result* 31)+((this.submitLabel == null)? 0 :this.submitLabel.hashCode()));
        result = ((result* 31)+((this.layout == null)? 0 :this.layout.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.fields == null)? 0 :this.fields.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Form) == false) {
            return false;
        }
        Form rhs = ((Form) other);
        return ((((((this.submitAction == rhs.submitAction)||((this.submitAction!= null)&&this.submitAction.equals(rhs.submitAction)))&&((this.submitLabel == rhs.submitLabel)||((this.submitLabel!= null)&&this.submitLabel.equals(rhs.submitLabel))))&&((this.layout == rhs.layout)||((this.layout!= null)&&this.layout.equals(rhs.layout))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.fields == rhs.fields)||((this.fields!= null)&&this.fields.equals(rhs.fields))));
    }


    /**
     * Layout hint for field arrangement
     * 
     */
    @Generated("jsonschema2pojo")
    public enum Layout {

        VERTICAL("vertical"),
        HORIZONTAL("horizontal"),
        GRID("grid");
        private final String value;
        private final static Map<String, Form.Layout> CONSTANTS = new HashMap<String, Form.Layout>();

        static {
            for (Form.Layout c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Layout(String value) {
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
        public static Form.Layout fromValue(String value) {
            Form.Layout constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
