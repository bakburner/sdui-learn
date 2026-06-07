package com.nba.sdui.domain.tokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runtime façade over the bundled design-system token JSON files.
 *
 * <p>The token JSON in {@code schema/*-tokens.json} is the single source of
 * truth for every design-system token name. This bean loads each family's
 * JSON at startup and exposes typed lookups that return the canonical wire
 * form (e.g. {@code "token:nba.spacing.lg"}, {@code "sdui:home"}). Composers
 * call {@link #spacing(String)}, {@link #color(String)}, etc., instead of
 * referencing compile-time constants — so adding or removing a token in JSON
 * is a single-edit change with no Java rebuild required to use it.
 *
 * <p>This intentionally mirrors the client model: each platform parses the
 * same JSON at startup and resolves token references at render time. Future
 * remote-updatable token registries land transparently — the JSON source can
 * move from classpath to a remote endpoint without changing any composer.
 *
 * <p>Unknown names throw {@link IllegalArgumentException} at composition
 * time. The server is intentionally stricter than clients here: client
 * resolvers log + neutral-default, server composition fails fast so the
 * surface never ships an unresolvable wire string.
 */
@Component
public class Tokens {

    private static final Logger log = LoggerFactory.getLogger(Tokens.class);

    private static final String SPACING_JSON = "schema/spacing-tokens.json";
    private static final String RADIUS_JSON = "schema/corner-radius-tokens.json";
    private static final String SHADOW_JSON = "schema/shadow-tokens.json";
    private static final String TYPOGRAPHY_JSON = "schema/typography-tokens.json";
    private static final String MOTION_JSON = "schema/motion-tokens.json";
    private static final String ICON_JSON = "schema/icon-tokens.json";

    private final TokenRegistry colors;

    private final Set<String> spacing = new LinkedHashSet<>();
    private final Set<String> radius = new LinkedHashSet<>();
    private final Set<String> shadow = new LinkedHashSet<>();
    private final Set<String> typography = new LinkedHashSet<>();
    private final Set<String> motionEasing = new LinkedHashSet<>();
    private final Set<String> motionDuration = new LinkedHashSet<>();
    private final Set<String> icons = new LinkedHashSet<>();

    public Tokens(ObjectMapper om, TokenRegistry colors) {
        this.colors = colors;
        loadFamily(om, SPACING_JSON, "spacing", spacing);
        loadFamily(om, RADIUS_JSON, "radius", radius);
        loadFamily(om, SHADOW_JSON, "shadows", shadow);
        loadFamily(om, TYPOGRAPHY_JSON, "variants", typography);
        loadMotion(om);
        loadIcons(om);
        log.info(
                "Tokens loaded: spacing={}, radius={}, shadow={}, typography={}, motion(easing={}, duration={}), icons={}",
                spacing.size(),
                radius.size(),
                shadow.size(),
                typography.size(),
                motionEasing.size(),
                motionDuration.size(),
                icons.size());
    }

    // ── Family lookups ─────────────────────────────────────────────────

    public String spacing(String name) {
        return require("nba.spacing." + name, spacing, "spacing");
    }

    public String radius(String name) {
        return require("nba.radius." + name, radius, "radius");
    }

    public String shadow(String name) {
        return require("nba.shadow." + name, shadow, "shadow");
    }

    public String typography(String variant) {
        return require("nba.typography." + variant, typography, "typography");
    }

    public String motionEasing(String name) {
        return require("nba.motion.easing." + name, motionEasing, "motion.easing");
    }

    public String motionDuration(String name) {
        return require("nba.motion.duration." + name, motionDuration, "motion.duration");
    }

    /**
     * Resolve a color by its canonical JSON key (e.g. {@code "nba.bg.primary"},
     * {@code "nba.label.accent.live"}). Delegates to {@link TokenRegistry}
     * which enforces existence against {@code schema/color-tokens.json}.
     */
    public String color(String key) {
        return colors.canonicalize(key);
    }

    /** Returns the wire form for an icon (e.g. {@code "sdui:home"}). */
    public String icon(String name) {
        String key = "sdui:" + name;
        if (!icons.contains(key)) {
            throw new IllegalArgumentException("unknown icon token: " + name);
        }
        return key;
    }

    // ── Existence checks (for tests / consistency) ─────────────────────

    public Set<String> spacingNames() { return Set.copyOf(spacing); }
    public Set<String> radiusNames() { return Set.copyOf(radius); }
    public Set<String> shadowNames() { return Set.copyOf(shadow); }
    public Set<String> typographyVariants() { return Set.copyOf(typography); }
    public Set<String> iconNames() { return Set.copyOf(icons); }

    // ── Loaders ────────────────────────────────────────────────────────

    private void loadFamily(ObjectMapper om, String resource, String topKey, Set<String> sink) {
        JsonNode root = readJson(om, resource);
        JsonNode family = root.path(topKey);
        if (!family.isObject() || family.isEmpty()) {
            throw new IllegalStateException(resource + " missing '" + topKey + "' object");
        }
        Iterator<String> names = family.fieldNames();
        while (names.hasNext()) {
            String n = names.next();
            if (n.startsWith("$")) continue;
            sink.add(n);
        }
    }

    private void loadMotion(ObjectMapper om) {
        JsonNode root = readJson(om, MOTION_JSON);
        JsonNode easing = root.path("easing");
        JsonNode duration = root.path("duration");
        if (!easing.isObject() || !duration.isObject()) {
            throw new IllegalStateException(MOTION_JSON + " missing easing/duration objects");
        }
        easing.fieldNames().forEachRemaining(motionEasing::add);
        duration.fieldNames().forEachRemaining(motionDuration::add);
    }

    private void loadIcons(ObjectMapper om) {
        JsonNode root = readJson(om, ICON_JSON);
        JsonNode tokens = root.path("tokens");
        if (!tokens.isObject() || tokens.isEmpty()) {
            throw new IllegalStateException(ICON_JSON + " missing 'tokens' object");
        }
        tokens.fieldNames().forEachRemaining(icons::add);
    }

    private JsonNode readJson(ObjectMapper om, String resource) {
        ClassPathResource res = new ClassPathResource(resource);
        if (!res.exists()) {
            throw new IllegalStateException("missing token JSON resource: " + resource);
        }
        try (InputStream in = res.getInputStream()) {
            return om.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read " + resource, e);
        }
    }

    private static String require(String fullKey, Set<String> family, String familyName) {
        if (!family.contains(fullKey)) {
            throw new IllegalArgumentException("unknown " + familyName + " token: " + fullKey);
        }
        return "token:" + fullKey;
    }
}
