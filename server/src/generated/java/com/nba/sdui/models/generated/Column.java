
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
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "key",
    "label",
    "align",
    "width"
})
@Generated("jsonschema2pojo")
public class Column {

    /**
     * Row data key
     * (Required)
     * 
     */
    @JsonProperty("key")
    @JsonPropertyDescription("Row data key")
    @NotNull
    private String key;
    /**
     * Header label
     * (Required)
     * 
     */
    @JsonProperty("label")
    @JsonPropertyDescription("Header label")
    @NotNull
    private String label;
    @JsonProperty("align")
    private Column.Align align = Column.Align.fromValue("start");
    /**
     * Fixed width (integer) or 'flex'
     * 
     */
    @JsonProperty("width")
    @JsonPropertyDescription("Fixed width (integer) or 'flex'")
    private Object width;
    @JsonIgnore
    @Valid
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * Row data key
     * (Required)
     * 
     */
    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    /**
     * Row data key
     * (Required)
     * 
     */
    @JsonProperty("key")
    public void setKey(String key) {
        this.key = key;
    }

    public Column withKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Header label
     * (Required)
     * 
     */
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    /**
     * Header label
     * (Required)
     * 
     */
    @JsonProperty("label")
    public void setLabel(String label) {
        this.label = label;
    }

    public Column withLabel(String label) {
        this.label = label;
        return this;
    }

    @JsonProperty("align")
    public Column.Align getAlign() {
        return align;
    }

    @JsonProperty("align")
    public void setAlign(Column.Align align) {
        this.align = align;
    }

    public Column withAlign(Column.Align align) {
        this.align = align;
        return this;
    }

    /**
     * Fixed width (integer) or 'flex'
     * 
     */
    @JsonProperty("width")
    public Object getWidth() {
        return width;
    }

    /**
     * Fixed width (integer) or 'flex'
     * 
     */
    @JsonProperty("width")
    public void setWidth(Object width) {
        this.width = width;
    }

    public Column withWidth(Object width) {
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

    public Column withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Column.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("key");
        sb.append('=');
        sb.append(((this.key == null)?"<null>":this.key));
        sb.append(',');
        sb.append("label");
        sb.append('=');
        sb.append(((this.label == null)?"<null>":this.label));
        sb.append(',');
        sb.append("align");
        sb.append('=');
        sb.append(((this.align == null)?"<null>":this.align));
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
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.align == null)? 0 :this.align.hashCode()));
        result = ((result* 31)+((this.key == null)? 0 :this.key.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Column) == false) {
            return false;
        }
        Column rhs = ((Column) other);
        return ((((((this.width == rhs.width)||((this.width!= null)&&this.width.equals(rhs.width)))&&((this.label == rhs.label)||((this.label!= null)&&this.label.equals(rhs.label))))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.align == rhs.align)||((this.align!= null)&&this.align.equals(rhs.align))))&&((this.key == rhs.key)||((this.key!= null)&&this.key.equals(rhs.key))));
    }

    @Generated("jsonschema2pojo")
    public enum Align {

        START("start"),
        CENTER("center"),
        END("end");
        private final String value;
        private final static Map<String, Column.Align> CONSTANTS = new HashMap<String, Column.Align>();

        static {
            for (Column.Align c: values()) {
                CONSTANTS.put(c.value, c);
            }
        }

        Align(String value) {
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
        public static Column.Align fromValue(String value) {
            Column.Align constant = CONSTANTS.get(value);
            if (constant == null) {
                throw new IllegalArgumentException(value);
            } else {
                return constant;
            }
        }

    }

}
