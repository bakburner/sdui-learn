package com.nba.sdui.tokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bundled layout token registries (spacing, corner radius, size, typography, shadow).
 * Resolves {@code token:<semantic.path>} to a per-{@code formFactor} integer.
 * Unknown keys log at debug and fall back to 0.
 */
@Component
public class LayoutTokenRegistry {

    private static final Logger log = LoggerFactory.getLogger(LayoutTokenRegistry.class);
    private static final String TOKEN_PREFIX = "token:";

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, String>> semanticMaps = new HashMap<>();
    private final Map<String, Map<String, Map<String, Integer>>> palettes = new HashMap<>();

    public LayoutTokenRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        for (String name : new String[] {
            "schema/spacing-tokens.json",
            "schema/corner-radius-tokens.json"
        }) {
            try {
                loadRegistry(name);
            } catch (Exception e) {
                log.warn("Failed to load layout token registry {}: {}", name, e.getMessage());
            }
        }
    }

    void loadRegistry(String classpathPath) throws Exception {
        ClassPathResource res = new ClassPathResource(classpathPath);
        if (!res.exists()) {
            log.debug("Layout registry missing: {}", classpathPath);
            return;
        }
        try (InputStream in = res.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            String key = registryKey(classpathPath);
            Map<String, String> semantic = new HashMap<>();
            if (root.has("semantic") && root.get("semantic").isObject()) {
                Iterator<String> it = root.get("semantic").fieldNames();
                while (it.hasNext()) {
                    String name = it.next();
                    JsonNode n = root.get("semantic").get(name);
                    if (n != null && n.has("aliasOf")) {
                        semantic.put(name, n.get("aliasOf").asText());
                    }
                }
            }
            Map<String, Map<String, Integer>> palette = new HashMap<>();
            if (root.has("palette") && root.get("palette").isObject()) {
                JsonNode p = root.get("palette");
                Iterator<String> it = p.fieldNames();
                while (it.hasNext()) {
                    String pname = it.next();
                    JsonNode perFf = p.get(pname);
                    if (!perFf.isObject()) {
                        continue;
                    }
                    Map<String, Integer> m = new HashMap<>();
                    Iterator<String> ff = perFf.fieldNames();
                    while (ff.hasNext()) {
                        String formFactor = ff.next();
                        m.put(formFactor, perFf.get(formFactor).asInt());
                    }
                    palette.put(pname, m);
                }
            }
            semanticMaps.put(key, semantic);
            palettes.put(key, palette);
        }
    }

    private static String registryKey(String classpathPath) {
        if (classpathPath.contains("spacing")) {
            return "spacing";
        }
        if (classpathPath.contains("corner-radius")) {
            return "corner";
        }
        return "other";
    }

    /**
     * Resolve a raw wire value: plain integer, or token reference, to an int for the form factor.
     */
    public int resolveInt(Object raw, String formFactor) {
        String effectiveFf = (formFactor == null || formFactor.isEmpty()) ? "phone" : formFactor;
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof String) {
            return resolveString((String) raw, effectiveFf);
        }
        return 0;
    }

    int resolveString(String wire, String formFactor) {
        if (wire == null || wire.isEmpty()) {
            return 0;
        }
        if (!wire.startsWith(TOKEN_PREFIX)) {
            // Invalid for layout scalar — treat as 0
            return 0;
        }
        String name = wire.substring(TOKEN_PREFIX.length());
        return resolveTokenName(name, formFactor);
    }

    private int resolveTokenName(String name, String formFactor) {
        for (String reg : semanticMaps.keySet()) {
            Integer v = tryResolveInRegistry(name, formFactor, reg);
            if (v != null) {
                return v;
            }
        }
        log.debug("token_resolver_missing: {}{}", TOKEN_PREFIX, name);
        return 0;
    }

    private Integer tryResolveInRegistry(String name, String formFactor, String reg) {
        Map<String, String> sem = semanticMaps.getOrDefault(reg, Collections.emptyMap());
        Map<String, Map<String, Integer>> pal = palettes.getOrDefault(reg, Collections.emptyMap());
        if (sem.isEmpty() && pal.isEmpty()) {
            return null;
        }
        return followAlias(name, formFactor, sem, pal, 0);
    }

    private Integer followAlias(
            String name,
            String formFactor,
            Map<String, String> sem,
            Map<String, Map<String, Integer>> pal,
            int depth
    ) {
        if (depth > 8) {
            return 0;
        }
        if (pal.containsKey(name)) {
            return valueForFormFactor(pal.get(name), formFactor);
        }
        if (sem.containsKey(name)) {
            String next = sem.get(name);
            if (next == null) {
                return null;
            }
            return followAlias(next, formFactor, sem, pal, depth + 1);
        }
        return null;
    }

    private int valueForFormFactor(Map<String, Integer> perFf, String formFactor) {
        if (perFf == null) {
            return 0;
        }
        if (perFf.containsKey(formFactor)) {
            return perFf.get(formFactor);
        }
        return perFf.getOrDefault("phone", 0);
    }
}
