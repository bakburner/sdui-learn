
package com.nba.sdui.models.generated;

import java.util.HashMap;
import java.util.LinkedHashMap;
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


/**
 * Optional server-provided error message and presentation style for action failures. Client falls back to generic localized string when absent.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "message",
    "style"
})
@Generated("jsonschema2pojo")
public class FailureFeedback {

    /**
     * Localized error message to display on failure
     * 
     */
    @JsonProperty("message")
    @JsonPropertyDescription("Localized error message to display on failure")
    private String message;
    /**
     * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
     * 
     */
    @JsonProperty("style")
    @JsonPropertyDescription("Presentation hint for failure feedback. Clients map to closest platform-native mechanism.")
    private FailureFeedback.FailureFeedbackStyle style;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Localized error message to display on failure
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * Localized error message to display on failure
     * 
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public FailureFeedback withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
     * 
     */
    @JsonProperty("style")
    public FailureFeedback.FailureFeedbackStyle getStyle() {
        return style;
    }

    /**
     * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
     * 
     */
    @JsonProperty("style")
    public void setStyle(FailureFeedback.FailureFeedbackStyle style) {
        this.style = style;
    }

    public FailureFeedback withStyle(FailureFeedback.FailureFeedbackStyle style) {
        this.style = style;
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

    public FailureFeedback withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(FailureFeedback.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("style");
        sb.append('=');
        sb.append(((this.style == null)?"<null>":this.style));
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
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.style == null)? 0 :this.style.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof FailureFeedback) == false) {
            return false;
        }
        FailureFeedback rhs = ((FailureFeedback) other);
        return ((((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message)))&&((this.style == rhs.style)||((this.style!= null)&&this.style.equals(rhs.style))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }


    /**
     * Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum FailureFeedbackStyle {

        SNACKBAR("snackbar"),
        TOAST("toast"),
        INLINE("inline");
        private final String value;
        private final static Map<String, FailureFeedback.FailureFeedbackStyle> CONSTANTS = new HashMap<String, FailureFeedback.FailureFeedbackStyle>();

        static {
            for (FailureFeedback.FailureFeedbackStyle c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        FailureFeedbackStyle(String value) {
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
        public static FailureFeedback.FailureFeedbackStyle fromValue(String value) {
            FailureFeedback.FailureFeedbackStyle constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
