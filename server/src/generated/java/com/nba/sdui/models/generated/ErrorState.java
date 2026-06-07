
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


/**
 * Server-declared error-state shape rendered by section error boundaries. Named `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's native error protocol (e.g. `Swift.Error`).
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "message",
    "retryAction",
    "retryLabel",
    "hideOnError"
})
@Generated("jsonschema2pojo")
public class ErrorState {

    /**
     * Error message to display (e.g., 'Unable to load scores')
     * 
     */
    @JsonProperty("message")
    @JsonPropertyDescription("Error message to display (e.g., 'Unable to load scores')")
    private String message;
    @JsonProperty("retryAction")
    @Valid
    private Action retryAction;
    /**
     * Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a neutral default when omitted.
     * 
     */
    @JsonProperty("retryLabel")
    @JsonPropertyDescription("Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a neutral default when omitted.")
    private String retryLabel;
    /**
     * If true, collapse the section entirely on error instead of showing error UI
     * 
     */
    @JsonProperty("hideOnError")
    @JsonPropertyDescription("If true, collapse the section entirely on error instead of showing error UI")
    private Boolean hideOnError = false;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Error message to display (e.g., 'Unable to load scores')
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * Error message to display (e.g., 'Unable to load scores')
     * 
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorState withMessage(String message) {
        this.message = message;
        return this;
    }

    @JsonProperty("retryAction")
    public Action getRetryAction() {
        return retryAction;
    }

    @JsonProperty("retryAction")
    public void setRetryAction(Action retryAction) {
        this.retryAction = retryAction;
    }

    public ErrorState withRetryAction(Action retryAction) {
        this.retryAction = retryAction;
        return this;
    }

    /**
     * Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a neutral default when omitted.
     * 
     */
    @JsonProperty("retryLabel")
    public String getRetryLabel() {
        return retryLabel;
    }

    /**
     * Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a neutral default when omitted.
     * 
     */
    @JsonProperty("retryLabel")
    public void setRetryLabel(String retryLabel) {
        this.retryLabel = retryLabel;
    }

    public ErrorState withRetryLabel(String retryLabel) {
        this.retryLabel = retryLabel;
        return this;
    }

    /**
     * If true, collapse the section entirely on error instead of showing error UI
     * 
     */
    @JsonProperty("hideOnError")
    public Boolean getHideOnError() {
        return hideOnError;
    }

    /**
     * If true, collapse the section entirely on error instead of showing error UI
     * 
     */
    @JsonProperty("hideOnError")
    public void setHideOnError(Boolean hideOnError) {
        this.hideOnError = hideOnError;
    }

    public ErrorState withHideOnError(Boolean hideOnError) {
        this.hideOnError = hideOnError;
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

    public ErrorState withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ErrorState.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("retryAction");
        sb.append('=');
        sb.append(((this.retryAction == null)?"<null>":this.retryAction));
        sb.append(',');
        sb.append("retryLabel");
        sb.append('=');
        sb.append(((this.retryLabel == null)?"<null>":this.retryLabel));
        sb.append(',');
        sb.append("hideOnError");
        sb.append('=');
        sb.append(((this.hideOnError == null)?"<null>":this.hideOnError));
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
        result = ((result* 31)+((this.retryAction == null)? 0 :this.retryAction.hashCode()));
        result = ((result* 31)+((this.hideOnError == null)? 0 :this.hideOnError.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.retryLabel == null)? 0 :this.retryLabel.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof ErrorState) == false) {
            return false;
        }
        ErrorState rhs = ((ErrorState) other);
        return ((((((this.retryAction == rhs.retryAction)||((this.retryAction!= null)&&this.retryAction.equals(rhs.retryAction)))&&((this.hideOnError == rhs.hideOnError)||((this.hideOnError!= null)&&this.hideOnError.equals(rhs.hideOnError))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message))))&&((this.retryLabel == rhs.retryLabel)||((this.retryLabel!= null)&&this.retryLabel.equals(rhs.retryLabel))));
    }

}
