package com.nba.sdui.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup invariant: every string constant in {@link ColorTokens} must resolve
 * against the bundled {@link TokenRegistry}. A mismatch fails Spring boot
 * rather than surfacing as a silent {@code token_resolver_missing} warning in
 * the clients at runtime.
 */
@Component
class TokenRegistryConsistencyCheck {

    private static final Logger log = LoggerFactory.getLogger(TokenRegistryConsistencyCheck.class);

    private final TokenRegistry registry;

    TokenRegistryConsistencyCheck(TokenRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    void verify() {
        List<String> missing = new ArrayList<>();
        for (Field f : ColorTokens.class.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (!Modifier.isStatic(mods) || !Modifier.isPublic(mods) || !Modifier.isFinal(mods)) continue;
            if (!f.getType().equals(String.class)) continue;
            try {
                String wire = (String) f.get(null);
                if (wire == null) continue;
                String bare = wire.startsWith("token:") ? wire.substring("token:".length()) : wire;
                if (!registry.exists(bare)) {
                    missing.add(f.getName() + " → " + wire);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "ColorTokens references unknown entries in schema/color-tokens.json: " + missing
                            + " — add them to the registry or remove the constant.");
        }
        log.info("TokenRegistryConsistencyCheck: all ColorTokens constants resolve against the bundled registry.");
    }
}
