
package com.nba.sdui.models.generated;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Generated("jsonschema2pojo")
public enum Destination {

    ADOBE("adobe"),
    FIREBASE("firebase"),
    INTERNAL("internal"),
    ALL("all");
    private final String value;
    private final static Map<String, Destination> CONSTANTS = new HashMap<String, Destination>();

    static {
        for (Destination c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    Destination(String value) {
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
    public static Destination fromValue(String value) {
        Destination constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
