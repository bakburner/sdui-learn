
package com.nba.sdui.models.generated;

import java.util.ArrayList;
import java.util.Arrays;
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "trigger",
    "type",
    "targetUri",
    "webUrl",
    "presentation",
    "modalHeight",
    "event",
    "params",
    "destinations",
    "impression",
    "target",
    "operation",
    "value",
    "endpoint",
    "paramBindings",
    "message",
    "onFailure",
    "failureFeedback"
})
@Generated("jsonschema2pojo")
public class Action {

    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * (Required)
     * 
     */
    @JsonProperty("trigger")
    @JsonPropertyDescription("Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.")
    @NotNull
    private Action.ActionTrigger trigger;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    @NotNull
    private Action.ActionType type;
    /**
     * For navigate actions: native deeplink URI
     * 
     */
    @JsonProperty("targetUri")
    @JsonPropertyDescription("For navigate actions: native deeplink URI")
    private String targetUri;
    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     * 
     */
    @JsonProperty("webUrl")
    @JsonPropertyDescription("For navigate actions: web-equivalent URL (first-class, not a fallback)")
    private String webUrl;
    @JsonProperty("presentation")
    private Action.NavigationPresentation presentation;
    @JsonProperty("modalHeight")
    private Action.ModalHeight modalHeight;
    /**
     * For fireAndForget actions: event name
     * 
     */
    @JsonProperty("event")
    @JsonPropertyDescription("For fireAndForget actions: event name")
    private String event;
    /**
     * For fireAndForget actions: event parameters
     * 
     */
    @JsonProperty("params")
    @JsonPropertyDescription("For fireAndForget actions: event parameters")
    @Valid
    private Params params;
    /**
     * For fireAndForget actions: where to send the beacon
     * 
     */
    @JsonProperty("destinations")
    @JsonPropertyDescription("For fireAndForget actions: where to send the beacon")
    @Valid
    private List<Destination> destinations = new ArrayList<Destination>(Arrays.asList(Destination.fromValue("all")));
    /**
     * Impression tracking policy for analytics actions with onVisible trigger
     * 
     */
    @JsonProperty("impression")
    @JsonPropertyDescription("Impression tracking policy for analytics actions with onVisible trigger")
    @Valid
    private ImpressionPolicy impression;
    /**
     * For mutate actions: state key to update. For dismiss actions: what to dismiss (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
     * 
     */
    @JsonProperty("target")
    @JsonPropertyDescription("For mutate actions: state key to update. For dismiss actions: what to dismiss (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)")
    private String target;
    @JsonProperty("operation")
    private Action.MutateOperation operation;
    /**
     * For mutate actions: the value to apply with the operation
     * 
     */
    @JsonProperty("value")
    @JsonPropertyDescription("For mutate actions: the value to apply with the operation")
    private Object value;
    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     * 
     */
    @JsonProperty("endpoint")
    @JsonPropertyDescription("For refresh actions: target URL (defaults to current screen endpoint if omitted)")
    private String endpoint;
    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     * 
     */
    @JsonProperty("paramBindings")
    @JsonPropertyDescription("For refresh actions: map of query param name to screen state key, resolved at action time")
    @Valid
    private ParamBindings paramBindings;
    /**
     * For toast actions: text message to display in the toast
     * 
     */
    @JsonProperty("message")
    @JsonPropertyDescription("For toast actions: text message to display in the toast")
    private String message;
    /**
     * Sequence behavior when an action fails. Clients apply per-type defaults when absent: navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
     * 
     */
    @JsonProperty("onFailure")
    @JsonPropertyDescription("Sequence behavior when an action fails. Clients apply per-type defaults when absent: navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.")
    private Action.FailurePolicy onFailure;
    /**
     * Optional server-provided error message and presentation style for action failures. Client falls back to generic localized string when absent.
     * 
     */
    @JsonProperty("failureFeedback")
    @JsonPropertyDescription("Optional server-provided error message and presentation style for action failures. Client falls back to generic localized string when absent.")
    @Valid
    private FailureFeedback failureFeedback;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * (Required)
     * 
     */
    @JsonProperty("trigger")
    public Action.ActionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * (Required)
     * 
     */
    @JsonProperty("trigger")
    public void setTrigger(Action.ActionTrigger trigger) {
        this.trigger = trigger;
    }

    public Action withTrigger(Action.ActionTrigger trigger) {
        this.trigger = trigger;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public Action.ActionType getType() {
        return type;
    }

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("type")
    public void setType(Action.ActionType type) {
        this.type = type;
    }

    public Action withType(Action.ActionType type) {
        this.type = type;
        return this;
    }

    /**
     * For navigate actions: native deeplink URI
     * 
     */
    @JsonProperty("targetUri")
    public String getTargetUri() {
        return targetUri;
    }

    /**
     * For navigate actions: native deeplink URI
     * 
     */
    @JsonProperty("targetUri")
    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }

    public Action withTargetUri(String targetUri) {
        this.targetUri = targetUri;
        return this;
    }

    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     * 
     */
    @JsonProperty("webUrl")
    public String getWebUrl() {
        return webUrl;
    }

    /**
     * For navigate actions: web-equivalent URL (first-class, not a fallback)
     * 
     */
    @JsonProperty("webUrl")
    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public Action withWebUrl(String webUrl) {
        this.webUrl = webUrl;
        return this;
    }

    @JsonProperty("presentation")
    public Action.NavigationPresentation getPresentation() {
        return presentation;
    }

    @JsonProperty("presentation")
    public void setPresentation(Action.NavigationPresentation presentation) {
        this.presentation = presentation;
    }

    public Action withPresentation(Action.NavigationPresentation presentation) {
        this.presentation = presentation;
        return this;
    }

    @JsonProperty("modalHeight")
    public Action.ModalHeight getModalHeight() {
        return modalHeight;
    }

    @JsonProperty("modalHeight")
    public void setModalHeight(Action.ModalHeight modalHeight) {
        this.modalHeight = modalHeight;
    }

    public Action withModalHeight(Action.ModalHeight modalHeight) {
        this.modalHeight = modalHeight;
        return this;
    }

    /**
     * For fireAndForget actions: event name
     * 
     */
    @JsonProperty("event")
    public String getEvent() {
        return event;
    }

    /**
     * For fireAndForget actions: event name
     * 
     */
    @JsonProperty("event")
    public void setEvent(String event) {
        this.event = event;
    }

    public Action withEvent(String event) {
        this.event = event;
        return this;
    }

    /**
     * For fireAndForget actions: event parameters
     * 
     */
    @JsonProperty("params")
    public Params getParams() {
        return params;
    }

    /**
     * For fireAndForget actions: event parameters
     * 
     */
    @JsonProperty("params")
    public void setParams(Params params) {
        this.params = params;
    }

    public Action withParams(Params params) {
        this.params = params;
        return this;
    }

    /**
     * For fireAndForget actions: where to send the beacon
     * 
     */
    @JsonProperty("destinations")
    public List<Destination> getDestinations() {
        return destinations;
    }

    /**
     * For fireAndForget actions: where to send the beacon
     * 
     */
    @JsonProperty("destinations")
    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }

    public Action withDestinations(List<Destination> destinations) {
        this.destinations = destinations;
        return this;
    }

    /**
     * Impression tracking policy for analytics actions with onVisible trigger
     * 
     */
    @JsonProperty("impression")
    public ImpressionPolicy getImpression() {
        return impression;
    }

    /**
     * Impression tracking policy for analytics actions with onVisible trigger
     * 
     */
    @JsonProperty("impression")
    public void setImpression(ImpressionPolicy impression) {
        this.impression = impression;
    }

    public Action withImpression(ImpressionPolicy impression) {
        this.impression = impression;
        return this;
    }

    /**
     * For mutate actions: state key to update. For dismiss actions: what to dismiss (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
     * 
     */
    @JsonProperty("target")
    public String getTarget() {
        return target;
    }

    /**
     * For mutate actions: state key to update. For dismiss actions: what to dismiss (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
     * 
     */
    @JsonProperty("target")
    public void setTarget(String target) {
        this.target = target;
    }

    public Action withTarget(String target) {
        this.target = target;
        return this;
    }

    @JsonProperty("operation")
    public Action.MutateOperation getOperation() {
        return operation;
    }

    @JsonProperty("operation")
    public void setOperation(Action.MutateOperation operation) {
        this.operation = operation;
    }

    public Action withOperation(Action.MutateOperation operation) {
        this.operation = operation;
        return this;
    }

    /**
     * For mutate actions: the value to apply with the operation
     * 
     */
    @JsonProperty("value")
    public Object getValue() {
        return value;
    }

    /**
     * For mutate actions: the value to apply with the operation
     * 
     */
    @JsonProperty("value")
    public void setValue(Object value) {
        this.value = value;
    }

    public Action withValue(Object value) {
        this.value = value;
        return this;
    }

    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     * 
     */
    @JsonProperty("endpoint")
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * For refresh actions: target URL (defaults to current screen endpoint if omitted)
     * 
     */
    @JsonProperty("endpoint")
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Action withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     * 
     */
    @JsonProperty("paramBindings")
    public ParamBindings getParamBindings() {
        return paramBindings;
    }

    /**
     * For refresh actions: map of query param name to screen state key, resolved at action time
     * 
     */
    @JsonProperty("paramBindings")
    public void setParamBindings(ParamBindings paramBindings) {
        this.paramBindings = paramBindings;
    }

    public Action withParamBindings(ParamBindings paramBindings) {
        this.paramBindings = paramBindings;
        return this;
    }

    /**
     * For toast actions: text message to display in the toast
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * For toast actions: text message to display in the toast
     * 
     */
    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    public Action withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Sequence behavior when an action fails. Clients apply per-type defaults when absent: navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
     * 
     */
    @JsonProperty("onFailure")
    public Action.FailurePolicy getOnFailure() {
        return onFailure;
    }

    /**
     * Sequence behavior when an action fails. Clients apply per-type defaults when absent: navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
     * 
     */
    @JsonProperty("onFailure")
    public void setOnFailure(Action.FailurePolicy onFailure) {
        this.onFailure = onFailure;
    }

    public Action withOnFailure(Action.FailurePolicy onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    /**
     * Optional server-provided error message and presentation style for action failures. Client falls back to generic localized string when absent.
     * 
     */
    @JsonProperty("failureFeedback")
    public FailureFeedback getFailureFeedback() {
        return failureFeedback;
    }

    /**
     * Optional server-provided error message and presentation style for action failures. Client falls back to generic localized string when absent.
     * 
     */
    @JsonProperty("failureFeedback")
    public void setFailureFeedback(FailureFeedback failureFeedback) {
        this.failureFeedback = failureFeedback;
    }

    public Action withFailureFeedback(FailureFeedback failureFeedback) {
        this.failureFeedback = failureFeedback;
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

    public Action withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Action.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("trigger");
        sb.append('=');
        sb.append(((this.trigger == null)?"<null>":this.trigger));
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(((this.type == null)?"<null>":this.type));
        sb.append(',');
        sb.append("targetUri");
        sb.append('=');
        sb.append(((this.targetUri == null)?"<null>":this.targetUri));
        sb.append(',');
        sb.append("webUrl");
        sb.append('=');
        sb.append(((this.webUrl == null)?"<null>":this.webUrl));
        sb.append(',');
        sb.append("presentation");
        sb.append('=');
        sb.append(((this.presentation == null)?"<null>":this.presentation));
        sb.append(',');
        sb.append("modalHeight");
        sb.append('=');
        sb.append(((this.modalHeight == null)?"<null>":this.modalHeight));
        sb.append(',');
        sb.append("event");
        sb.append('=');
        sb.append(((this.event == null)?"<null>":this.event));
        sb.append(',');
        sb.append("params");
        sb.append('=');
        sb.append(((this.params == null)?"<null>":this.params));
        sb.append(',');
        sb.append("destinations");
        sb.append('=');
        sb.append(((this.destinations == null)?"<null>":this.destinations));
        sb.append(',');
        sb.append("impression");
        sb.append('=');
        sb.append(((this.impression == null)?"<null>":this.impression));
        sb.append(',');
        sb.append("target");
        sb.append('=');
        sb.append(((this.target == null)?"<null>":this.target));
        sb.append(',');
        sb.append("operation");
        sb.append('=');
        sb.append(((this.operation == null)?"<null>":this.operation));
        sb.append(',');
        sb.append("value");
        sb.append('=');
        sb.append(((this.value == null)?"<null>":this.value));
        sb.append(',');
        sb.append("endpoint");
        sb.append('=');
        sb.append(((this.endpoint == null)?"<null>":this.endpoint));
        sb.append(',');
        sb.append("paramBindings");
        sb.append('=');
        sb.append(((this.paramBindings == null)?"<null>":this.paramBindings));
        sb.append(',');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("onFailure");
        sb.append('=');
        sb.append(((this.onFailure == null)?"<null>":this.onFailure));
        sb.append(',');
        sb.append("failureFeedback");
        sb.append('=');
        sb.append(((this.failureFeedback == null)?"<null>":this.failureFeedback));
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
        result = ((result* 31)+((this.failureFeedback == null)? 0 :this.failureFeedback.hashCode()));
        result = ((result* 31)+((this.destinations == null)? 0 :this.destinations.hashCode()));
        result = ((result* 31)+((this.onFailure == null)? 0 :this.onFailure.hashCode()));
        result = ((result* 31)+((this.trigger == null)? 0 :this.trigger.hashCode()));
        result = ((result* 31)+((this.type == null)? 0 :this.type.hashCode()));
        result = ((result* 31)+((this.params == null)? 0 :this.params.hashCode()));
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.target == null)? 0 :this.target.hashCode()));
        result = ((result* 31)+((this.paramBindings == null)? 0 :this.paramBindings.hashCode()));
        result = ((result* 31)+((this.presentation == null)? 0 :this.presentation.hashCode()));
        result = ((result* 31)+((this.modalHeight == null)? 0 :this.modalHeight.hashCode()));
        result = ((result* 31)+((this.endpoint == null)? 0 :this.endpoint.hashCode()));
        result = ((result* 31)+((this.webUrl == null)? 0 :this.webUrl.hashCode()));
        result = ((result* 31)+((this.targetUri == null)? 0 :this.targetUri.hashCode()));
        result = ((result* 31)+((this.impression == null)? 0 :this.impression.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.event == null)? 0 :this.event.hashCode()));
        result = ((result* 31)+((this.operation == null)? 0 :this.operation.hashCode()));
        result = ((result* 31)+((this.value == null)? 0 :this.value.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Action) == false) {
            return false;
        }
        Action rhs = ((Action) other);
        return ((((((((((((((((((((this.failureFeedback == rhs.failureFeedback)||((this.failureFeedback!= null)&&this.failureFeedback.equals(rhs.failureFeedback)))&&((this.destinations == rhs.destinations)||((this.destinations!= null)&&this.destinations.equals(rhs.destinations))))&&((this.onFailure == rhs.onFailure)||((this.onFailure!= null)&&this.onFailure.equals(rhs.onFailure))))&&((this.trigger == rhs.trigger)||((this.trigger!= null)&&this.trigger.equals(rhs.trigger))))&&((this.type == rhs.type)||((this.type!= null)&&this.type.equals(rhs.type))))&&((this.params == rhs.params)||((this.params!= null)&&this.params.equals(rhs.params))))&&((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message))))&&((this.target == rhs.target)||((this.target!= null)&&this.target.equals(rhs.target))))&&((this.paramBindings == rhs.paramBindings)||((this.paramBindings!= null)&&this.paramBindings.equals(rhs.paramBindings))))&&((this.presentation == rhs.presentation)||((this.presentation!= null)&&this.presentation.equals(rhs.presentation))))&&((this.modalHeight == rhs.modalHeight)||((this.modalHeight!= null)&&this.modalHeight.equals(rhs.modalHeight))))&&((this.endpoint == rhs.endpoint)||((this.endpoint!= null)&&this.endpoint.equals(rhs.endpoint))))&&((this.webUrl == rhs.webUrl)||((this.webUrl!= null)&&this.webUrl.equals(rhs.webUrl))))&&((this.targetUri == rhs.targetUri)||((this.targetUri!= null)&&this.targetUri.equals(rhs.targetUri))))&&((this.impression == rhs.impression)||((this.impression!= null)&&this.impression.equals(rhs.impression))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.event == rhs.event)||((this.event!= null)&&this.event.equals(rhs.event))))&&((this.operation == rhs.operation)||((this.operation!= null)&&this.operation.equals(rhs.operation))))&&((this.value == rhs.value)||((this.value!= null)&&this.value.equals(rhs.value))));
    }


    /**
     * Event that fires the action. onActivate is the primary activation trigger and covers tap, keyboard Enter/Space, and accessibility activate uniformly across platforms.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum ActionTrigger {

        ON_ACTIVATE("onActivate"),
        ON_LONG_PRESS("onLongPress"),
        ON_VISIBLE("onVisible"),
        ON_SWIPE("onSwipe"),
        ON_FOCUS("onFocus"),
        ON_BLUR("onBlur"),
        ON_SUBMIT("onSubmit");
        private final String value;
        private final static Map<String, Action.ActionTrigger> CONSTANTS = new HashMap<String, Action.ActionTrigger>();

        static {
            for (Action.ActionTrigger c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ActionTrigger(String value) {
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
        public static Action.ActionTrigger fromValue(String value) {
            Action.ActionTrigger constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum ActionType {

        NAVIGATE("navigate"),
        FIRE_AND_FORGET("fireAndForget"),
        MUTATE("mutate"),
        REFRESH("refresh"),
        DISMISS("dismiss"),
        TOAST("toast");
        private final String value;
        private final static Map<String, Action.ActionType> CONSTANTS = new HashMap<String, Action.ActionType>();

        static {
            for (Action.ActionType c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ActionType(String value) {
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
        public static Action.ActionType fromValue(String value) {
            Action.ActionType constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }


    /**
     * Sequence behavior when an action fails. Clients apply per-type defaults when absent: navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
     * 
     */
    @Generated("jsonschema2pojo")
    public enum FailurePolicy {

        HALT("halt"),
        CONTINUE("continue"),
        SILENT("silent");
        private final String value;
        private final static Map<String, Action.FailurePolicy> CONSTANTS = new HashMap<String, Action.FailurePolicy>();

        static {
            for (Action.FailurePolicy c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        FailurePolicy(String value) {
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
        public static Action.FailurePolicy fromValue(String value) {
            Action.FailurePolicy constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum ModalHeight {

        COMPACT("compact"),
        HALF("half"),
        FULL("full");
        private final String value;
        private final static Map<String, Action.ModalHeight> CONSTANTS = new HashMap<String, Action.ModalHeight>();

        static {
            for (Action.ModalHeight c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        ModalHeight(String value) {
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
        public static Action.ModalHeight fromValue(String value) {
            Action.ModalHeight constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum MutateOperation {

        SET("set"),
        TOGGLE("toggle"),
        INCREMENT("increment"),
        APPEND("append");
        private final String value;
        private final static Map<String, Action.MutateOperation> CONSTANTS = new HashMap<String, Action.MutateOperation>();

        static {
            for (Action.MutateOperation c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        MutateOperation(String value) {
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
        public static Action.MutateOperation fromValue(String value) {
            Action.MutateOperation constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

    @Generated("jsonschema2pojo")
    public enum NavigationPresentation {

        PUSH("push"),
        MODAL("modal"),
        FULLSCREEN("fullscreen"),
        REPLACE("replace"),
        EXTERNAL("external");
        private final String value;
        private final static Map<String, Action.NavigationPresentation> CONSTANTS = new HashMap<String, Action.NavigationPresentation>();

        static {
            for (Action.NavigationPresentation c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        NavigationPresentation(String value) {
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
        public static Action.NavigationPresentation fromValue(String value) {
            Action.NavigationPresentation constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
