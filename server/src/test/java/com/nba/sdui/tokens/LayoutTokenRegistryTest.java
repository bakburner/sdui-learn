package com.nba.sdui.tokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LayoutTokenRegistry}.
 *
 * <p>Covers numeric pass-through, semantic-alias resolution per form factor,
 * unknown-token / non-token / null inputs (lenient resolver, never throws),
 * and the form-factor fallback path when a palette row is missing for the
 * requested form factor.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LayoutTokenRegistryTest {

    private LayoutTokenRegistry registry;

    @BeforeAll
    void buildRegistry() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        this.registry = new LayoutTokenRegistry(mapper);
        // Inject a synthetic registry whose only palette entry has a `phone`
        // row, so we can prove the form-factor fallback path explicitly.
        registry.loadRegistry("schema/test-fallback-tokens.json");
    }

    // ── Pass-through and basic resolution ──────────────────────────────

    /** Confirms the registry actually loads from the runtime classpath. */
    @Test
    void registryLoadsFromClasspath() {
        assertEquals(12, registry.resolveInt("token:spacing.md", "phone"),
                "spacing.md should resolve to 12 on phone if classpath load succeeded");
    }

    @Test
    void numericInputPassesThrough() {
        assertEquals(12, registry.resolveInt(12, "phone"));
        assertEquals(0, registry.resolveInt(0, "tablet"));
        assertEquals(99, registry.resolveInt(99, "web.wide"));
    }

    @Test
    void spacingMdResolvesPerFormFactor() {
        assertEquals(12, registry.resolveInt("token:spacing.md", "phone"));
        assertEquals(14, registry.resolveInt("token:spacing.md", "tablet"));
        assertEquals(16, registry.resolveInt("token:spacing.md", "tv"));
        assertEquals(14, registry.resolveInt("token:spacing.md", "web.wide"));
    }

    @Test
    void radiusFullResolvesThroughAliasOnEveryFormFactor() {
        for (String ff : new String[] {
                "phone", "phone.landscape", "tablet", "tv", "web.narrow", "web.wide"
        }) {
            assertEquals(999, registry.resolveInt("token:radius.full", ff),
                    "radius.full should alias to 999 for " + ff);
        }
    }

    // ── Lenient resolver: never throws on bad input ────────────────────

    @Test
    void unknownTokenNameReturnsZero() {
        assertEquals(0, registry.resolveInt("token:totally.fictional.name", "phone"));
    }

    @Test
    void tokenPrefixOnNonexistentNameReturnsZero() {
        assertEquals(0, registry.resolveInt("token:spacing.does-not-exist", "tablet"));
    }

    @Test
    void nonTokenStringReturnsZero() {
        assertEquals(0, registry.resolveInt("12", "phone"),
                "raw numeric strings are not a layout scalar shape");
        assertEquals(0, registry.resolveInt("hello", "phone"));
    }

    @Test
    void nullInputReturnsZero() {
        assertEquals(0, registry.resolveInt(null, "phone"));
    }

    // ── Form-factor fallback ───────────────────────────────────────────

    @Test
    void missingFormFactorRowFallsBackToPhone() {
        // Synthetic palette only carries a `phone` row; resolution on tablet/tv/web
        // must fall back to phone instead of throwing or returning 0.
        assertEquals(7, registry.resolveInt("token:test.fallback.only", "tablet"));
        assertEquals(7, registry.resolveInt("token:test.fallback.only", "tv"));
        assertEquals(7, registry.resolveInt("token:test.fallback.only", "web.wide"));
        assertEquals(7, registry.resolveInt("token:test.fallback.only", "phone.landscape"));
        // And exact match on phone still works.
        assertEquals(7, registry.resolveInt("token:test.fallback.only", "phone"));
    }
}
