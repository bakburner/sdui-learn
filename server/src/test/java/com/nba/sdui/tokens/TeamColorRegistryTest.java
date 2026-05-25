package com.nba.sdui.tokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-validation tests for the team color section in {@code schema/color-tokens.json}.
 *
 * <p>Asserts structural integrity of bundled team palettes and UI mode mappings:
 * every mode reference resolves to a palette entry, every literal hex is valid,
 * and all 30 teams resolve for all 4 semantic tokens in both themes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeamColorRegistryTest {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Set<String> VALID_ROLES = Set.of("primary", "secondary", "tertiary");
    private static final List<String> SEMANTIC_TOKENS = List.of(
            "nba.team.bg", "nba.team.label", "nba.team.accent", "nba.team.accent-label");
    private static final List<String> THEMES = List.of("light", "dark");

    private JsonNode teamSection;
    private JsonNode palettes;
    private JsonNode modes;
    private JsonNode semantic;

    @BeforeAll
    void loadTeamSection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("schema/color-tokens.json")) {
            assertNotNull(is, "color-tokens.json must be on the classpath");
            JsonNode root = mapper.readTree(is);
            teamSection = root.path("team");
            assertFalse(teamSection.isMissingNode(), "team section must exist");
            palettes = teamSection.path("palettes");
            modes = teamSection.path("modes");
            semantic = teamSection.path("semantic");
        }
    }

    @Test
    void allThirtyTeamsHavePalettes() {
        assertEquals(30, palettes.size(), "expected 30 team palettes");
    }

    @Test
    void everyPaletteHasPrimaryAndSecondary() {
        palettes.fieldNames().forEachRemaining(teamId -> {
            JsonNode palette = palettes.get(teamId);
            assertTrue(palette.has("primary"),
                    teamId + " palette missing primary");
            assertTrue(palette.has("secondary"),
                    teamId + " palette missing secondary");
            palette.fieldNames().forEachRemaining(role -> {
                assertTrue(VALID_ROLES.contains(role),
                        teamId + " has unexpected role: " + role);
                String hex = palette.get(role).asText();
                assertTrue(HEX_COLOR.matcher(hex).matches(),
                        teamId + "." + role + " is not a valid hex color: " + hex);
            });
        });
    }

    @Test
    void modeRoleReferencesResolveAgainstPalettes() {
        modes.fieldNames().forEachRemaining(modeName -> {
            JsonNode mode = modes.get(modeName);
            mode.fieldNames().forEachRemaining(key -> {
                if (key.startsWith("$") || key.equals("_default")) return;
                JsonNode value = mode.get(key);
                if (value.isTextual()) {
                    String role = value.asText();
                    assertTrue(VALID_ROLES.contains(role),
                            modeName + "." + key + " references unknown role: " + role);
                    JsonNode palette = palettes.get(key);
                    assertNotNull(palette,
                            modeName + " references team " + key + " which has no palette");
                    assertTrue(palette.has(role),
                            modeName + "." + key + " references role " + role
                                    + " but palette has no such entry");
                }
            });
        });
    }

    @Test
    void modeLiteralHexValuesAreValid() {
        modes.fieldNames().forEachRemaining(modeName -> {
            JsonNode mode = modes.get(modeName);
            mode.fieldNames().forEachRemaining(key -> {
                JsonNode value = mode.get(key);
                if (value.isObject() && value.has("value")) {
                    String hex = value.get("value").asText();
                    assertTrue(HEX_COLOR.matcher(hex).matches(),
                            modeName + "." + key + " has invalid literal hex: " + hex);
                }
            });
        });
    }

    @Test
    void sacDarkAccentPreservesCSVOverride() {
        JsonNode darkAccent = modes.path("team-accent--dark");
        JsonNode sacValue = darkAccent.path("sac");
        assertTrue(sacValue.isObject() && sacValue.has("value"),
                "sac dark accent must be a literal value object");
        assertEquals("#BEC9CF", sacValue.get("value").asText(),
                "sac dark accent must preserve the CSV hex override exactly");
    }

    @Test
    void allFourSemanticTokensAreMapped() {
        for (String token : SEMANTIC_TOKENS) {
            assertTrue(semantic.has(token), "semantic section missing " + token);
        }
    }

    @Test
    void everySemanticTokenResolvesForAllTeamsAndThemes() {
        Set<String> teamIds = new LinkedHashSet<>();
        palettes.fieldNames().forEachRemaining(teamIds::add);

        for (String token : SEMANTIC_TOKENS) {
            JsonNode entry = semantic.get(token);
            for (String theme : THEMES) {
                String modeName;
                if (entry.has("mode")) {
                    modeName = entry.get("mode").asText();
                } else {
                    modeName = entry.path(theme).asText(null);
                    assertNotNull(modeName,
                            token + " has no mode mapping for theme " + theme);
                }

                JsonNode mode = modes.get(modeName);
                assertNotNull(mode, token + " references missing mode: " + modeName);

                for (String teamId : teamIds) {
                    JsonNode teamValue = mode.get(teamId);
                    JsonNode defaultValue = mode.get("_default");
                    assertTrue(teamValue != null || defaultValue != null,
                            token + " / " + modeName + " has no value for "
                                    + teamId + " and no _default");
                }
            }
        }
    }

    @Test
    void modeDefaultRoleReferencesAreValid() {
        modes.fieldNames().forEachRemaining(modeName -> {
            JsonNode mode = modes.get(modeName);
            JsonNode defaultVal = mode.get("_default");
            if (defaultVal != null && defaultVal.isTextual()) {
                String role = defaultVal.asText();
                assertTrue(VALID_ROLES.contains(role),
                        modeName + "._default references unknown role: " + role);
                palettes.fieldNames().forEachRemaining(teamId -> {
                    JsonNode palette = palettes.get(teamId);
                    if (!mode.has(teamId)) {
                        assertTrue(palette.has(role),
                                modeName + "._default=" + role + " but " + teamId
                                        + " palette has no " + role);
                    }
                });
            }
        });
    }
}
