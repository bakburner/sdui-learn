package com.nba.sdui.tokens;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.nba.sdui.domain.tokens.LayoutTokenRegistry;

/**
 * Unit tests for {@link LayoutTokenRegistry}.
 *
 * <p>Covers numeric pass-through, semantic resolution against the 4-column
 * form-factor matrix (phone / tablet / tv / web), unknown-token / non-token /
 * null inputs (lenient resolver, never throws), and form-factor fallback when
 * the requested column is missing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LayoutTokenRegistryTest {

    private LayoutTokenRegistry registry;

    @BeforeAll
    void buildRegistry() {
        this.registry = new LayoutTokenRegistry(new ObjectMapper());
    }

    @Test
    void registryLoadsFromClasspath() {
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", "phone"),
                "nba.spacing.md should resolve to 12 on phone if classpath load succeeded");
    }

    @Test
    void numericInputPassesThrough() {
        assertEquals(12, registry.resolveInt(12, "phone"));
        assertEquals(0, registry.resolveInt(0, "tablet"));
        assertEquals(99, registry.resolveInt(99, "web"));
    }

    @Test
    void spacingMdResolvesPerFormFactor() {
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", "phone"));
        assertEquals(15, registry.resolveInt("token:nba.spacing.md", "tablet"));
        assertEquals(18, registry.resolveInt("token:nba.spacing.md", "tv"));
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", "web"));
    }

    @Test
    void spacingLgResolvesPerFormFactor() {
        assertEquals(16, registry.resolveInt("token:nba.spacing.lg", "phone"));
        assertEquals(20, registry.resolveInt("token:nba.spacing.lg", "tablet"));
        assertEquals(24, registry.resolveInt("token:nba.spacing.lg", "tv"));
        assertEquals(16, registry.resolveInt("token:nba.spacing.lg", "web"));
    }

    @Test
    void radiusFullResolvesOnEveryFormFactor() {
        for (String ff : new String[] {"phone", "tablet", "tv", "web"}) {
            assertEquals(9999, registry.resolveInt("token:nba.radius.full", ff),
                    "nba.radius.full should resolve to 9999 for " + ff);
        }
    }

    @Test
    void radiusMdResolvesPerFormFactor() {
        for (String ff : new String[] {"phone", "tablet", "tv", "web"}) {
            assertEquals(12, registry.resolveInt("token:nba.radius.md", ff),
                    "nba.radius.md is form-factor-flat at 12");
        }
    }

    @Test
    void unknownTokenNameReturnsZero() {
        assertEquals(0, registry.resolveInt("token:totally.fictional.name", "phone"));
    }

    @Test
    void tokenPrefixOnNonexistentNameReturnsZero() {
        assertEquals(0, registry.resolveInt("token:nba.spacing.does-not-exist", "tablet"));
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

    @Test
    void unknownFormFactorFallsBackToPhone() {
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", "smartwatch"),
                "unknown form factors fall back to the phone column");
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", ""),
                "empty form factor coerces to phone");
        assertEquals(12, registry.resolveInt("token:nba.spacing.md", null),
                "null form factor coerces to phone");
    }
}
