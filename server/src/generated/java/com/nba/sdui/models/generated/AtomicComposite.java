
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
 * Component payload for AtomicComposite sections — ui contains rendering instructions, content carries domain data
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ui",
    "content"
})
@Generated("jsonschema2pojo")
public class AtomicComposite {

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("ui")
    @JsonPropertyDescription("Atomic UI primitive \u2014 server-composed building block for the atomic rendering layer")
    @Valid
    @NotNull
    private AtomicElement ui;
    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future data-binding support.
     * 
     */
    @JsonProperty("content")
    @JsonPropertyDescription("Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future data-binding support.")
    @Valid
    private Content content;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("ui")
    public AtomicElement getUi() {
        return ui;
    }

    /**
     * Atomic UI primitive — server-composed building block for the atomic rendering layer
     * (Required)
     * 
     */
    @JsonProperty("ui")
    public void setUi(AtomicElement ui) {
        this.ui = ui;
    }

    public AtomicComposite withUi(AtomicElement ui) {
        this.ui = ui;
        return this;
    }

    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future data-binding support.
     * 
     */
    @JsonProperty("content")
    public Content getContent() {
        return content;
    }

    /**
     * Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future data-binding support.
     * 
     */
    @JsonProperty("content")
    public void setContent(Content content) {
        this.content = content;
    }

    public AtomicComposite withContent(Content content) {
        this.content = content;
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

    public AtomicComposite withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AtomicComposite.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("ui");
        sb.append('=');
        sb.append(((this.ui == null)?"<null>":this.ui));
        sb.append(',');
        sb.append("content");
        sb.append('=');
        sb.append(((this.content == null)?"<null>":this.content));
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
        result = ((result* 31)+((this.ui == null)? 0 :this.ui.hashCode()));
        result = ((result* 31)+((this.content == null)? 0 :this.content.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof AtomicComposite) == false) {
            return false;
        }
        AtomicComposite rhs = ((AtomicComposite) other);
        return ((((this.ui == rhs.ui)||((this.ui!= null)&&this.ui.equals(rhs.ui)))&&((this.content == rhs.content)||((this.content!= null)&&this.content.equals(rhs.content))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))));
    }

}
