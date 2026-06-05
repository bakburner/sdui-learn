package com.nba.sdui.service;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.ForYouComposer;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;
import com.nba.sdui.domain.SectionIdDeriver;

/**
 * Composition tests for the For You visual refresh: feed order, editorial hero,
 * and live-game refresh semantics on Tonight's Games.
 */
class ForYouVisualCompositionTest {

    private ForYouComposer composer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        composer = newComposer(new StatsApiClient(objectMapper, new SeasonCalendarService()));
    }

    private ForYouComposer newComposer(StatsApiClient statsApiClient) {
        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        return new ForYouComposer(objectMapper, new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                new SectionRefreshService());
    }

    @Test
    void firstContentModuleAfterStoryRailIsFeaturedHero() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        ArrayNode sections = (ArrayNode) response.get("sections");

        assertEquals("feed:for-you-following", sections.get(0).path("contentSourceId").asText());
        JsonNode hero = sections.get(1);
        assertEquals("cms:for-you-featured-hero", hero.path("contentSourceId").asText());
        assertEquals("AtomicComposite", hero.path("type").asText());
        assertEquals("for_you_featured_hero", hero.path("analyticsId").asText());
    }

    @Test
    void featuredHeroHasImageScrimAndCta() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        JsonNode hero = findSectionByContentSourceId(response, "cms:for-you-featured-hero");

        String tree = hero.path("data").path("ui").toString();
        assertTrue(tree.contains("Image"), "Hero must include background image");
        assertTrue(tree.contains("\"direction\":\"vertical\"") && tree.contains("#000000E6"),
                "Hero must include vertical scrim gradient for readable overlay copy");
        assertTrue(tree.contains("See Story") || tree.contains("Watch"),
                "Hero must include server-emitted CTA label");
        assertTrue(tree.contains("headlineSmall"),
                "Hero headline should use headlineSmall typography");
        assertTrue(tree.contains("Button"), "Hero must include CTA button");
    }

    @Test
    void topStoriesHeaderHasMoreCta() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        JsonNode header = findSectionByAnalyticsId(response, "for_you_top_stories_header");
        String tree = header.path("data").path("ui").toString();
        assertTrue(tree.contains("More"), "Top Stories header should expose More CTA");
        assertTrue(tree.contains("nba://news"), "More CTA must use server-provided navigate target");
    }

    @Test
    void tonightsGamesHeroSupportsLiveRefreshWhenMockLivePresent() throws Exception {
        StatsApiClient statsApiClient = mock(StatsApiClient.class);
        when(statsApiClient.getScoreboard()).thenThrow(new IOException("forced demo fallback"));
        ForYouComposer demoComposer = newComposer(statsApiClient);

        JsonNode response = objectMapper.valueToTree(demoComposer.composeForYou("test-trace-id", "en"));
        JsonNode gamesHero = findSectionByContentSourceId(response, "stats-api:scoreboard");

        assertEquals("AtomicComposite", gamesHero.path("type").asText());
        assertEquals("poll", gamesHero.path("refreshPolicy").path("type").asText(),
                "Mock fallback hero must poll the section endpoint, not SSE");
        String sectionEndpoint = gamesHero.path("refreshPolicy").path("sectionEndpoint").asText("");
        assertTrue(sectionEndpoint.contains("tonights-games-hero"),
                "Poll must target the Tonight's Games hero section id");
        assertTrue(gamesHero.path("dataBinding").isMissingNode()
                        || gamesHero.path("dataBinding").path("bindings").isEmpty(),
                "Mock fallback must not open Ably linescore bindings");
    }

    @Test
    void feedOrderMatchesVisualRefreshPlan() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        ArrayNode sections = (ArrayNode) response.get("sections");

        String[] expectedAnalyticsOrder = {
                "for_you_following",
                "for_you_featured_hero",
                "for_you_top_stories_header",
                "for_you_top_stories",
                "for_you_ad_1",
                "for_you_tonights_games_header",
                "for_you_tonights_games_hero",
                "for_you_trending_header",
                "for_you_trending",
                "for_you_ad_2",
                "for_you_lp_picks_header",
                "for_you_lp_picks",
                "for_you_ad_3",
                "for_you_other_leagues",
                "for_you_around_league_header",
                "for_you_around_league",
                "for_you_vod_playlist"
        };

        assertEquals(expectedAnalyticsOrder.length, sections.size());
        for (int i = 0; i < expectedAnalyticsOrder.length; i++) {
            assertEquals(expectedAnalyticsOrder[i], sections.get(i).path("analyticsId").asText(),
                    "Section index " + i);
        }
    }

    @Test
    void allSectionsRetainDerivedIdsAndContentSourceIds() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            String contentSourceId = section.path("contentSourceId").asText("");
            assertFalse(contentSourceId.isBlank());
            assertTrue(sectionId.contains(contentSourceId));
            assertTrue(SectionIdDeriver.isDerived(sectionId));
            assertFalse(sectionId.matches(".*~slug=\\d+$"));
        }
    }

    @Test
    void contentInsetsUnchanged() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        JsonNode insets = response.get("contentInsets");
        assertNotNull(insets);
        assertEquals(TestTokens.INSTANCE.spacing("md"), insets.path("start").asText());
        assertEquals(TestTokens.INSTANCE.spacing("md"), insets.path("end").asText());
        assertEquals(TestTokens.INSTANCE.spacing("lg"), insets.path("bottom").asText());
    }

    private JsonNode findSectionByContentSourceId(JsonNode response, String contentSourceId) {
        for (JsonNode section : response.withArray("sections")) {
            if (contentSourceId.equals(section.path("contentSourceId").asText())) {
                return section;
            }
        }
        throw new AssertionError("No section with contentSourceId: " + contentSourceId);
    }

    private JsonNode findSectionByAnalyticsId(JsonNode response, String analyticsId) {
        for (JsonNode section : response.withArray("sections")) {
            if (analyticsId.equals(section.path("analyticsId").asText())) {
                return section;
            }
        }
        throw new AssertionError("No section with analyticsId: " + analyticsId);
    }
}
