
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
 * One input field inside a form section
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "fieldId",
    "fieldType",
    "variant",
    "label",
    "placeholder",
    "stateKey",
    "required",
    "disabled",
    "options",
    "validationPattern",
    "validationMessage"
})
@Generated("jsonschema2pojo")
public class FormField {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fieldId")
    @NotNull
    private String fieldId;
    /**
     * Input type; clients map to platform-native controls
     * (Required)
     * 
     */
    @JsonProperty("fieldType")
    @JsonPropertyDescription("Input type; clients map to platform-native controls")
    @NotNull
    private FormField.FieldType fieldType;
    /**
     * How a Form single-select field is realized by the client. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies only when FormField.fieldType == 'select'.
     * 
     */
    @JsonProperty("variant")
    @JsonPropertyDescription("How a Form single-select field is realized by the client. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies only when FormField.fieldType == 'select'.")
    private FormField.SelectVariant variant;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("label")
    @NotNull
    private String label;
    @JsonProperty("placeholder")
    private String placeholder;
    /**
     * Screen-state key that holds this field's current value
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    @JsonPropertyDescription("Screen-state key that holds this field's current value")
    @NotNull
    private String stateKey;
    @JsonProperty("required")
    private Boolean required = false;
    @JsonProperty("disabled")
    private Boolean disabled = false;
    /**
     * For select/radio/checkbox field types: the available choices
     * 
     */
    @JsonProperty("options")
    @JsonPropertyDescription("For select/radio/checkbox field types: the available choices")
    @Valid
    private List<FormOption> options = new ArrayList<FormOption>();
    /**
     * Optional regex pattern for client-side validation
     * 
     */
    @JsonProperty("validationPattern")
    @JsonPropertyDescription("Optional regex pattern for client-side validation")
    private String validationPattern;
    /**
     * Message to show when validation fails
     * 
     */
    @JsonProperty("validationMessage")
    @JsonPropertyDescription("Message to show when validation fails")
    private String validationMessage;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fieldId")
    public String getFieldId() {
        return fieldId;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("fieldId")
    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public FormField withFieldId(String fieldId) {
        this.fieldId = fieldId;
        return this;
    }

    /**
     * Input type; clients map to platform-native controls
     * (Required)
     * 
     */
    @JsonProperty("fieldType")
    public FormField.FieldType getFieldType() {
        return fieldType;
    }

    /**
     * Input type; clients map to platform-native controls
     * (Required)
     * 
     */
    @JsonProperty("fieldType")
    public void setFieldType(FormField.FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FormField withFieldType(FormField.FieldType fieldType) {
        this.fieldType = fieldType;
        return this;
    }

    /**
     * How a Form single-select field is realized by the client. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies only when FormField.fieldType == 'select'.
     * 
     */
    @JsonProperty("variant")
    public FormField.SelectVariant getVariant() {
        return variant;
    }

    /**
     * How a Form single-select field is realized by the client. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies only when FormField.fieldType == 'select'.
     * 
     */
    @JsonProperty("variant")
    public void setVariant(FormField.SelectVariant variant) {
        this.variant = variant;
    }

    public FormField withVariant(FormField.SelectVariant variant) {
        this.variant = variant;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public FormField withLabel(String label) {
        this.label = label;
        return this;
    }

    @JsonProperty("placeholder")
    public String getPlaceholder() {
        return placeholder;
    }

    @JsonProperty("placeholder")
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public FormField withPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * Screen-state key that holds this field's current value
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    public String getStateKey() {
        return stateKey;
    }

    /**
     * Screen-state key that holds this field's current value
     * (Required)
     * 
     */
    @JsonProperty("stateKey")
    public void setStateKey(String stateKey) {
        this.stateKey = stateKey;
    }

    public FormField withStateKey(String stateKey) {
        this.stateKey = stateKey;
        return this;
    }

    @JsonProperty("required")
    public Boolean getRequired() {
        return required;
    }

    @JsonProperty("required")
    public void setRequired(Boolean required) {
        this.required = required;
    }

    public FormField withRequired(Boolean required) {
        this.required = required;
        return this;
    }

    @JsonProperty("disabled")
    public Boolean getDisabled() {
        return disabled;
    }

    @JsonProperty("disabled")
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public FormField withDisabled(Boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    /**
     * For select/radio/checkbox field types: the available choices
     * 
     */
    @JsonProperty("options")
    public List<FormOption> getOptions() {
        return options;
    }

    /**
     * For select/radio/checkbox field types: the available choices
     * 
     */
    @JsonProperty("options")
    public void setOptions(List<FormOption> options) {
        this.options = options;
    }

    public FormField withOptions(List<FormOption> options) {
        this.options = options;
        return this;
    }

    /**
     * Optional regex pattern for client-side validation
     * 
     */
    @JsonProperty("validationPattern")
    public String getValidationPattern() {
        return validationPattern;
    }

    /**
     * Optional regex pattern for client-side validation
     * 
     */
    @JsonProperty("validationPattern")
    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public FormField withValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
        return this;
    }

    /**
     * Message to show when validation fails
     * 
     */
    @JsonProperty("validationMessage")
    public String getValidationMessage() {
        return validationMessage;
    }

    /**
     * Message to show when validation fails
     * 
     */
    @JsonProperty("validationMessage")
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public FormField withValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
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

    public FormField withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FormField.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("fieldId");
        sb.append('=');
        sb.append(((this.fieldId == null)?"<null>":this.fieldId));
        sb.append(',');
        sb.append("fieldType");
        sb.append('=');
        sb.append(((this.fieldType == null)?"<null>":this.fieldType));
        sb.append(',');
        sb.append("variant");
        sb.append('=');
        sb.append(((this.variant == null)?"<null>":this.variant));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("placeholder");
        sb.append('=');
        sb.append(((this.placeholder == null)?"<null>":this.placeholder));
        sb.append(',');
        sb.append("stateKey");
        sb.append('=');
        sb.append(((this.stateKey == null)?"<null>":this.stateKey));
        sb.append(',');
        sb.append("required");
        sb.append('=');
        sb.append(((this.required == null)?"<null>":this.required));
        sb.append(',');
        sb.append("disabled");
        sb.append('=');
        sb.append(((this.disabled == null)?"<null>":this.disabled));
        sb.append(',');
        sb.append("options");
        sb.append('=');
        sb.append(((this.options == null)?"<null>":this.options));
        sb.append(',');
        sb.append("validationPattern");
        sb.append('=');
        sb.append(((this.validationPattern == null)?"<null>":this.validationPattern));
        sb.append(',');
        sb.append("validationMessage");
        sb.append('=');
        sb.append(((this.validationMessage == null)?"<null>":this.validationMessage));
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
        result = ((result* 31)+((this.validationPattern == null)? 0 :this.validationPattern.hashCode()));
        result = ((result* 31)+((this.validationMessage == null)? 0 :this.validationMessage.hashCode()));
        result = ((result* 31)+((this.variant == null)? 0 :this.variant.hashCode()));
        result = ((result* 31)+((this.options == null)? 0 :this.options.hashCode()));
        result = ((result* 31)+((this.disabled == null)? 0 :this.disabled.hashCode()));
        result = ((result* 31)+((this.label == null)? 0 :this.label.hashCode()));
        result = ((result* 31)+((this.placeholder == null)? 0 :this.placeholder.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.fieldType == null)? 0 :this.fieldType.hashCode()));
        result = ((result* 31)+((this.required == null)? 0 :this.required.hashCode()));
        result = ((result* 31)+((this.fieldId == null)? 0 :this.fieldId.hashCode()));
        result = ((result* 31)+((this.stateKey == null)? 0 :this.stateKey.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FormField) == false) {
            return false;
        }
        FormField rhs = ((FormField) other);
        return (((((((((((((this.validationPattern == rhs.validationPattern)||((this.validationPattern!= null)&&this.validationPattern.equals(rhs.validationPattern)))&&((this.validationMessage == rhs.validationMessage)||((this.validationMessage!= null)&&this.validationMessage.equals(rhs.validationMessage))))&&((this.variant == rhs.variant)||((this.variant!= null)&&this.variant.equals(rhs.variant))))&&((this.options == rhs.options)||((this.options!= null)&&this.options.equals(rhs.options))))&&((this.disabled == rhs.disabled)||((this.disabled!= null)&&this.disabled.equals(rhs.disabled))))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.placeholder == rhs.placeholder)||((this.placeholder!= null)&&this.placeholder.equals(rhs.placeholder))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.fieldType == rhs.fieldType)||((this.fieldType!= null)&&this.fieldType.equals(rhs.fieldType))))&&((this.required == rhs.required)||((this.required!= null)&&this.required.equals(rhs.required))))&&((this.fieldId == rhs.fieldId)||((this.fieldId!= null)&&this.fieldId.equals(rhs.fieldId))))&&((this.stateKey == rhs.stateKey)||((this.stateKey!= null)&&this.stateKey.equals(rhs.stateKey))));
    }


    /**
     * Input type; clients map to platform-native controls
     * 
     */
    @Generated("jsonschema2pojo")
    public enum FieldType {

        TEXT("text"),
        NUMBER("number"),
        DATE("date"),
        SELECT("select"),
        RADIO("radio"),
        CHECKBOX("checkbox"),
        TOGGLE("toggle"),
        TEXTAREA("textarea");
        private final String value;
        private final static Map<String, FormField.FieldType> CONSTANTS = new HashMap<String, FormField.FieldType>();

        static {
            for (FormField.FieldType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        FieldType(String value) {
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
        public static FormField.FieldType fromValue(String value) {
            FormField.FieldType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * How a Form single-select field is realized by the client. 'dropdown' maps to the platform menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies only when FormField.fieldType == 'select'.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum SelectVariant {

        DROPDOWN("dropdown"),
        CHIPS("chips");
        private final String value;
        private final static Map<String, FormField.SelectVariant> CONSTANTS = new HashMap<String, FormField.SelectVariant>();

        static {
            for (FormField.SelectVariant c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        SelectVariant(String value) {
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
        public static FormField.SelectVariant fromValue(String value) {
            FormField.SelectVariant constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
