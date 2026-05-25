package com.nba.sdui.tokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bundled layout token registries (spacing, corner radius, motion, shadow, typography, font).
 * Resolves {@code token:<semantic.path>} to a per-{@code formFactor} integer.
 * Unknown keys log at debug and fall back to 0.
 */
@Component
public class LayoutTokenRegistry {

    private static final Logger log = LoggerFactory.getLogger(LayoutTokenRegistry.class);
    private static final String TOKEN_PREFIX = "token:";
    private static final String SCHEMA_PATH = "schema/sdui-schema.json";
    private static final String SPACING_PATH = "schema/spacing-tokens.json";
    private static final String CORNER_PATH = "schema/corner-radius-tokens.json";
    private static final String MOTION_PATH = "schema/motion-tokens.json";
    private static final String SHADOW_PATH = "schema/shadow-tokens.json";
    private static final String FONT_PATH = "schema/font-tokens.json";
    private static final String TYPOGRAPHY_PATH = "schema/typography-tokens.json";

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Map<String, Integer>>> tokenMaps = new HashMap<>();
    private final Set<String> schemaTextVariants = new LinkedHashSet<>();
    private final Set<String> fontFamilyRefs = new LinkedHashSet<>();

    public LayoutTokenRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        loadTextVariantsFromSchema();
        for (String name : new String[] {
            SPACING_PATH,
            CORNER_PATH,
            MOTION_PATH,
            SHADOW_PATH,
            FONT_PATH,
            TYPOGRAPHY_PATH
        }) {
            try {
                loadRegistry(name);
            } catch (Exception e) {
                log.warn("Failed to load layout token registry {}: {}", name, e.getMessage(), e);
                throw new IllegalStateException("Failed to initialize layout token registry: " + name, e);
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
            if ("motion".equals(key)) {
                validateMotionRegistry(root);
                return;
            }
            if ("shadow".equals(key)) {
                validateShadowRegistry(root);
                return;
            }
            if ("font".equals(key)) {
                validateFontRegistry(root);
                return;
            }
            if ("typography".equals(key)) {
                validateTypographyRegistry(root);
                return;
            }
            JsonNode familyNode = familyNode(root, key);
            if (familyNode == null || !familyNode.isObject()) {
                tokenMaps.put(key, new HashMap<>());
                return;
            }

            Map<String, Map<String, Integer>> tokens = new HashMap<>();
            Iterator<String> it = familyNode.fieldNames();
            while (it.hasNext()) {
                String tokenName = it.next();
                JsonNode perFf = familyNode.get(tokenName);
                if (!perFf.isObject()) {
                    continue;
                }
                Map<String, Integer> m = new HashMap<>();
                Iterator<String> ff = perFf.fieldNames();
                while (ff.hasNext()) {
                    String formFactor = ff.next();
                    m.put(formFactor, perFf.get(formFactor).asInt());
                }
                tokens.put(tokenName, m);
            }
            tokenMaps.put(key, tokens);
        }
    }

    private JsonNode familyNode(JsonNode root, String key) {
        if ("spacing".equals(key)) {
            return root.get("spacing");
        }
        if ("corner".equals(key)) {
            return root.has("radius") ? root.get("radius") : root.get("cornerRadius");
        }
        return null;
    }

    private static String registryKey(String classpathPath) {
        if (classpathPath.contains("spacing")) {
            return "spacing";
        }
        if (classpathPath.contains("corner-radius")) {
            return "corner";
        }
        if (classpathPath.contains("motion")) {
            return "motion";
        }
        if (classpathPath.contains("shadow")) {
            return "shadow";
        }
        if (classpathPath.contains("font")) {
            return "font";
        }
        if (classpathPath.contains("typography")) {
            return "typography";
        }
        return "other";
    }

    private void loadTextVariantsFromSchema() {
        try {
            ClassPathResource res = new ClassPathResource(SCHEMA_PATH);
            if (!res.exists()) {
                throw new IllegalArgumentException("sdui-schema.json missing at " + SCHEMA_PATH);
            }
            try (InputStream in = res.getInputStream()) {
                JsonNode root = objectMapper.readTree(in);
                JsonNode variants = root.path("definitions").path("TextVariant").path("enum");
                if (!variants.isArray() || variants.isEmpty()) {
                    throw new IllegalArgumentException("TextVariant enum missing or empty in " + SCHEMA_PATH);
                }
                schemaTextVariants.clear();
                for (JsonNode variant : variants) {
                    if (!variant.isTextual()) {
                        throw new IllegalArgumentException("Non-string TextVariant enum value in " + SCHEMA_PATH);
                    }
                    schemaTextVariants.add(variant.asText());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load TextVariant enum from schema", e);
        }
    }

    private void validateFontRegistry(JsonNode root) {
        JsonNode families = root.path("families");
        if (!families.isObject() || families.isEmpty()) {
            throw new IllegalArgumentException("font-tokens.json missing families object");
        }
        fontFamilyRefs.clear();
        Iterator<String> familyNames = families.fieldNames();
        while (familyNames.hasNext()) {
            String familyRef = familyNames.next();
            JsonNode familyNode = families.get(familyRef);
            if (!familyNode.isObject()) {
                throw new IllegalArgumentException("font family entry must be object: " + familyRef);
            }
            fontFamilyRefs.add(familyRef);
        }
    }

    private void validateTypographyRegistry(JsonNode root) {
        if (fontFamilyRefs.isEmpty()) {
            throw new IllegalArgumentException("font-tokens.json must load before typography-tokens.json");
        }

        JsonNode categories = root.path("categories");
        if (!categories.isObject() || categories.isEmpty()) {
            throw new IllegalArgumentException("typography-tokens.json missing categories object");
        }
        JsonNode variants = root.path("variants");
        if (!variants.isObject() || variants.isEmpty()) {
            throw new IllegalArgumentException("typography-tokens.json missing variants object");
        }

        Set<String> categoryRefs = new LinkedHashSet<>();
        Iterator<String> categoryNames = categories.fieldNames();
        while (categoryNames.hasNext()) {
            String categoryRef = categoryNames.next();
            JsonNode category = categories.get(categoryRef);
            if (!category.isObject()) {
                throw new IllegalArgumentException("typography category must be object: " + categoryRef);
            }
            JsonNode familyRefNode = category.get("familyRef");
            if (familyRefNode == null || !familyRefNode.isTextual()) {
                throw new IllegalArgumentException("typography category missing familyRef: " + categoryRef);
            }
            String familyRef = familyRefNode.asText();
            if (!fontFamilyRefs.contains(familyRef)) {
                throw new IllegalArgumentException(
                    "typography category familyRef does not resolve in font-tokens.json: "
                        + categoryRef + " -> " + familyRef
                );
            }
            categoryRefs.add(categoryRef);
        }

        for (String textVariant : schemaTextVariants) {
            String tokenName = "nba.typography." + textVariant;
            if (!variants.has(tokenName)) {
                throw new IllegalArgumentException(
                    "typography variant missing for TextVariant enum value: " + textVariant
                );
            }
        }

        Iterator<String> variantNames = variants.fieldNames();
        while (variantNames.hasNext()) {
            String variantToken = variantNames.next();
            JsonNode variant = variants.get(variantToken);
            if (!variant.isObject()) {
                throw new IllegalArgumentException("typography variant must be object: " + variantToken);
            }

            JsonNode categoryRefNode = variant.get("categoryRef");
            if (categoryRefNode == null || !categoryRefNode.isTextual()) {
                throw new IllegalArgumentException("typography variant missing categoryRef: " + variantToken);
            }
            String categoryRef = categoryRefNode.asText();
            if (!categoryRefs.contains(categoryRef)) {
                throw new IllegalArgumentException(
                    "typography variant categoryRef does not resolve: "
                        + variantToken + " -> " + categoryRef
                );
            }

            JsonNode size = variant.get("size");
            if (size == null || !size.isObject()) {
                throw new IllegalArgumentException("typography variant missing size matrix: " + variantToken);
            }
            if (!size.path("phone").isNumber()
                || !size.path("tablet").isNumber()
                || !size.path("tv").isNumber()
                || size.get("web") == null) {
                throw new IllegalArgumentException(
                    "typography variant size must define phone/tablet/tv/web: " + variantToken
                );
            }

            JsonNode web = size.get("web");
            if (web.isNumber()) {
                continue;
            }
            if (!web.isObject()
                || !web.path("min").isNumber()
                || !web.path("max").isNumber()
                || !web.path("minVw").isNumber()
                || !web.path("maxVw").isNumber()) {
                throw new IllegalArgumentException(
                    "typography web envelope must define min/max/minVw/maxVw numbers: " + variantToken
                );
            }

            double min = web.path("min").asDouble();
            double max = web.path("max").asDouble();
            double minVw = web.path("minVw").asDouble();
            double maxVw = web.path("maxVw").asDouble();
            if (min > max) {
                throw new IllegalArgumentException(
                    "typography web envelope min must be <= max: " + variantToken
                );
            }
            if (minVw >= maxVw) {
                throw new IllegalArgumentException(
                    "typography web envelope minVw must be < maxVw: " + variantToken
                );
            }
        }
    }

    private void validateMotionRegistry(JsonNode root) {
        JsonNode easing = root.get("easing");
        if (easing == null || !easing.isObject()) {
            throw new IllegalArgumentException("motion-tokens.json missing easing object");
        }
        Iterator<String> easingNames = easing.fieldNames();
        while (easingNames.hasNext()) {
            String name = easingNames.next();
            if (!easing.get(name).isTextual()) {
                throw new IllegalArgumentException("motion easing must be string: " + name);
            }
        }

        JsonNode duration = root.get("duration");
        if (duration == null || !duration.isObject()) {
            throw new IllegalArgumentException("motion-tokens.json missing duration object");
        }
        Iterator<String> durationNames = duration.fieldNames();
        while (durationNames.hasNext()) {
            String name = durationNames.next();
            JsonNode perFf = duration.get(name);
            if (!perFf.isObject()
                || !perFf.path("phone").isNumber()
                || !perFf.path("tablet").isNumber()
                || !perFf.path("tv").isNumber()
                || !perFf.path("web").isNumber()) {
                throw new IllegalArgumentException("motion duration must define phone/tablet/tv/web numbers: " + name);
            }
        }
    }

    private void validateShadowRegistry(JsonNode root) {
        JsonNode shadows = root.get("shadows");
        if (shadows == null || !shadows.isObject()) {
            throw new IllegalArgumentException("shadow-tokens.json missing shadows object");
        }
        Iterator<String> shadowNames = shadows.fieldNames();
        while (shadowNames.hasNext()) {
            String name = shadowNames.next();
            JsonNode shadow = shadows.get(name);
            if (!shadow.isObject()
                || !shadow.path("type").isTextual()
                || !shadow.path("color").isTextual()
                || !shadow.path("radius").isNumber()
                || !shadow.path("offsetX").isNumber()
                || !shadow.path("offsetY").isNumber()) {
                throw new IllegalArgumentException("shadow token is malformed: " + name);
            }
        }
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
        for (String reg : tokenMaps.keySet()) {
            Integer v = tryResolveInRegistry(name, formFactor, reg);
            if (v != null) {
                return v;
            }
        }
        log.debug("token_resolver_missing: {}{}", TOKEN_PREFIX, name);
        return 0;
    }

    private Integer tryResolveInRegistry(String name, String formFactor, String reg) {
        Map<String, Map<String, Integer>> tokens = tokenMaps.get(reg);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        Map<String, Integer> perFf = tokens.get(name);
        return perFf == null ? null : valueForFormFactor(perFf, formFactor);
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
