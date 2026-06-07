
package com.nba.sdui.models.generated;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Generated("jsonschema2pojo")
public enum Capability {

    PIP("pip"),
    CHROMECAST("chromecast"),
    AIRPLAY("airplay"),
    BACKGROUND_AUDIO("backgroundAudio"),
    FULLSCREEN_ROTATION("fullscreenRotation");
    private final String value;
    private final static Map<String, Capability> CONSTANTS = new HashMap<String, Capability>();

    static {
        for (Capability c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    Capability(String value) {
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
    public static Capability fromValue(String value) {
        Capability constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
