package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory snapshot of {@code schema/color-tokens.json}. Loaded once at
 * application startup from the classpath; exposes lookups used by composers
 * (to validate tokens before they are emitted) and by tests.
 *
 * <p>The registry has two tiers:
 * <ul>
 *   <li><b>palette</b> — primitives keyed by {@code color.<family>.<step>}
 *       carrying literal hex light/dark pairs.</li>
 *   <li><b>semantic</b> — aliases keyed by semantic path (e.g.
 *       {@code color.primary.50}, {@code color.brand.nba}) that point at
 *       a palette primitive (or at another semantic token that ultimately
 *       resolves to one).</li>
 * </ul>
 *
 * <p>Clients resolve tokens at render time against their own hand-mirrored
 * snapshots of the same registry — the server does not emit resolved hex
 * on the wire; it emits token references.
 */
@Service
public class TokenRegistry {

    private static final Logger log = LoggerFactory.getLogger(TokenRegistry.class);
    private static final String COLOR_TOKENS_RESOURCE = "schema/color-tokens.json";
    private static final String TOKEN_PREFIX = "token:";

    private final ObjectMapper objectMapper;

    private final Map<String, PaletteEntry> palette = new ConcurrentHashMap<>();
    private final Map<String, String> semantic = new ConcurrentHashMap<>();
    private final Set<String> allTokenNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Autowired
    public TokenRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        try (InputStream stream = new ClassPathResource(COLOR_TOKENS_RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(stream);
            loadPalette(root.path("palette"));
            loadSemantic(root.path("semantic"));
            log.info("TokenRegistry loaded {} palette primitives and {} semantic tokens from {}",
                    palette.size(), semantic.size(), COLOR_TOKENS_RESOURCE);
        } catch (IOException e) {
            log.error("TokenRegistry failed to load {} — composers will reject every token reference",
                    COLOR_TOKENS_RESOURCE, e);
        }
    }

    private void loadPalette(JsonNode paletteNode) {
        if (!paletteNode.isObject()) return;
        Iterator<Map.Entry<String, JsonNode>> it = paletteNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            String light = v.path("light").asText(null);
            String dark = v.path("dark").asText(null);
            if (light != null && dark != null) {
                palette.put(e.getKey(), new PaletteEntry(light, dark));
                allTokenNames.add(e.getKey());
            }
        }
    }

    private void loadSemantic(JsonNode semanticNode) {
        if (!semanticNode.isObject()) return;
        Iterator<Map.Entry<String, JsonNode>> it = semanticNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String aliasOf = e.getValue().path("aliasOf").asText(null);
            if (aliasOf != null && !aliasOf.isBlank()) {
                semantic.put(e.getKey(), aliasOf);
                allTokenNames.add(e.getKey());
            }
        }
    }

    /**
     * True when {@code name} identifies a palette primitive or semantic alias
     * that resolves to one.
     */
    public boolean exists(String name) {
        if (name == null || name.isBlank()) return false;
        return followAlias(name, new HashSet<>()) != null;
    }

    /**
     * Accepts a bare token name (e.g. {@code color.primary.50}) or a
     * fully-qualified wire reference (e.g. {@code token:color.primary.50})
     * and returns the wire form with the {@code token:} prefix attached.
     * Rejects unknown tokens by throwing {@link IllegalArgumentException};
     * call {@link #exists(String)} first if the caller needs a non-throwing
     * pre-check.
     */
    public String canonicalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("token name must not be blank");
        }
        String bare = name.startsWith(TOKEN_PREFIX) ? name.substring(TOKEN_PREFIX.length()) : name;
        if (!exists(bare)) {
            throw new IllegalArgumentException("unknown color token: " + name);
        }
        return TOKEN_PREFIX + bare;
    }

    /** The complete set of names resolvable by this registry (palette + semantic). */
    public Set<String> knownTokens() {
        return Collections.unmodifiableSet(allTokenNames);
    }

    private PaletteEntry followAlias(String name, Set<String> seen) {
        if (!seen.add(name) || seen.size() > 16) return null;
        PaletteEntry p = palette.get(name);
        if (p != null) return p;
        String next = semantic.get(name);
        if (next == null) return null;
        return followAlias(next, seen);
    }

    /** Literal hex pair for a palette primitive. Exposed for tests; composers
     * should emit token references, not resolved hex. */
    record PaletteEntry(String light, String dark) {}
}
