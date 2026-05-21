package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON-shape snapshots for feed-module helpers (Phase 4–6) — no renderer or codegen.
 */
class AtomicCompositeBuilderFeedModulesTest {

    private static final String OVERLAY_SCRIM_TOKEN = "token:nba.effect.scrim";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicCompositeBuilder builder = new AtomicCompositeBuilder(objectMapper);

    @Test
    void buildMediaOverlayCard_emitsAtomicCompositeWithDataUi() {
        ObjectNode section = builder.buildMediaOverlayCard(
                "test-media",
                "test_media",
                "https://example.com/media/warriors-thunder.jpg",
                "Saturday's Stacked Slate",
                "11 games, no League Pass",
                "View Games",
                "nba://schedule/saturday",
                "LIVE",
                "nba://share/saturday-slate",
                "nba://audio/saturday-slate");

        assertCompositeEnvelope(section);
        assertEquals("OverlayContainer", section.path("data").path("ui").path("type").asText());
        assertTrue(section.path("data").path("ui").path("base").path("src").isTextual());
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertNoOutlinedOverMediaVariant(section);
        assertOverlayImageStacksHaveReadableTextBacking(section.path("data").path("ui"));
    }

    @Test
    void buildFeaturedLiveGameHero_multiCard_emitsPagedScrollWithDotsFromSharedCarouselPath() {
        String bUrl = "https://example.com/broadcast/sponsor.png";
        String[][] cards = {
                {"c1", "LIVE", "A at B", "Dek", "https://example.com/art/1.jpg",
                        "A", "1", "https://example.com/la.png", "B", "2", "https://example.com/lb.png",
                        "Q1", "0-0", bUrl, "nba://game/c1", "nba://game/c1/overflow"},
                {"c2", "LIVE", "C at D", "Dek2", "https://example.com/art/2.jpg",
                        "C", "1", "https://example.com/lc.png", "D", "2", "https://example.com/ld.png",
                        "Q1", "0-0", bUrl, "nba://game/c2", "nba://game/c2/overflow"}
        };
        ObjectNode section = builder.buildFeaturedLiveGameHero("h", "h", null, cards);
        JsonNode scroll = firstScrollUnderUi(section);
        assertTrue(scroll.path("paging").asBoolean());
        assertEquals("center", scroll.path("snapAlignment").asText());
        assertEquals("dots", scroll.path("pageIndicator").path("style").asText());
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertNoOutlinedOverMediaVariant(section);
        assertOverlayImageStacksHaveReadableTextBacking(section.path("data").path("ui"));
    }

    @Test
    void buildFeaturedLiveGameHero_singleCard_omitsPagingAndIndicator() {
        String[][] cards = {
                {"c1", "LIVE", "A at B", "Dek", "https://example.com/art/1.jpg",
                        "A", "1", "https://example.com/la.png", "B", "2", "https://example.com/lb.png",
                        "Q1", "0-0", "https://example.com/b.png", "nba://game/c1", null}
        };
        ObjectNode section = builder.buildFeaturedLiveGameHero("h", "h", null, cards);
        JsonNode scroll = firstScrollUnderUi(section);
        assertFalse(scroll.path("paging").asBoolean(false));
        assertTrue(scroll.path("pageIndicator").isMissingNode() || scroll.get("pageIndicator").isNull());
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertNoOutlinedOverMediaVariant(section);
        assertOverlayImageStacksHaveReadableTextBacking(section.path("data").path("ui"));
    }

    @Test
    void buildFeaturedLiveGameHero_usesServerBroadcastAndOverflowUris() {
        String bUrl = "https://broadcaster.example/sponsor.png";
        String main = "nba://game/main-1";
        String ov = "nba://game/main-1/overflow";
        String[][] cards = {
                {"c1", "LIVE", "A at B", "Dek", "https://example.com/art/1.jpg",
                        "A", "1", "https://example.com/la.png", "B", "2", "https://example.com/lb.png",
                        "Q1", "0-0", bUrl, main, ov}
        };
        ObjectNode section = builder.buildFeaturedLiveGameHero("h", "h", null, cards);
        String json = section.toString();
        assertTrue(json.contains(bUrl), "broadcaster image URL from composer input");
        assertTrue(json.contains(ov), "overflow action target from composer input");
        assertTrue(json.contains(main), "card tap target from composer input");
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertNoOutlinedOverMediaVariant(section);
    }

    @Test
    void buildStoryCircleRail_emitsAtomicCompositeWithoutLegacyBadge() {
        String[][] items = {
                {"s1", "Warriors", "https://example.com/story/w1.png", "LIVE", "nba://story/w1"}
        };
        ObjectNode section = builder.buildStoryCircleRail("sec", "a", "Stories", items);
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertOverlayImageStacksHaveReadableTextBacking(section.path("data").path("ui"));
    }

    @Test
    void buildEditorialOverlayRail_emitsScrimBackedOverlayCopy() {
        String[][] cards = {
                {"e1", "Big headline", "Dek line", "https://example.com/ed/1.jpg", "NEW", "nba://ed/1"}
        };
        ObjectNode section = builder.buildEditorialOverlayRail("sec", "a", "Editorial", cards);
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
        assertOverlayImageStacksHaveReadableTextBacking(section.path("data").path("ui"));
    }

    @Test
    void buildSectionHeaderComposite_emitsAtomicComposite() {
        ObjectNode section = builder.buildSectionHeaderComposite(
                "hdr", "a", "Latest", null, "More", "nba://latest/more");
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
    }

    @Test
    void buildUtilityCardGrid_emitsAtomicComposite() {
        String[][] items = {
                {"u1", "Standings", "West order", "https://example.com/ic1.png", "nba://u1"},
                {"u2", "Leaders", null, "https://example.com/ic2.png", "nba://u2"}
        };
        ObjectNode section = builder.buildUtilityCardGrid("sec", "a", "Around", items);
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
    }

    @Test
    void buildLeagueCardRail_emitsAtomicComposite() {
        String[][] items = {
                {"l1", "WNBA", "https://example.com/l1.png", "nba://league/wnba"}
        };
        ObjectNode section = builder.buildLeagueCardRail("sec", "a", "Other leagues", items);
        assertCompositeEnvelope(section);
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
    }

    @Test
    void buildGameScheduleRow_emitsAtomicCompositeWithContent() {
        String[] row = {
                "g1",
                "NYK", "Knicks", "3", "109", "https://example.com/nyk.png",
                "BOS", "Celtics", "2", "132", "https://example.com/bos.png",
                "Final", "BOS leads 1-0",
                "https://example.com/broadcast/lp.png",
                "nba://game/g1",
                "nba://game/g1/more"
        };
        ObjectNode section = builder.buildGameScheduleRow("sec", "a", row);
        assertCompositeEnvelope(section);
        assertNotNull(section.path("data").get("content"));
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
    }

    @Test
    void buildGameScheduleList_emitsAtomicCompositeWithContent() {
        String[][] rows = {
                {
                        "g1",
                        "NYK", "Knicks", "3", "109", "https://example.com/nyk.png",
                        "BOS", "Celtics", "2", "132", "https://example.com/bos.png",
                        "Final", null,
                        null,
                        "nba://game/g1",
                        null
                }
        };
        ObjectNode section = builder.buildGameScheduleList("sec", "a", "Thursday", rows);
        assertCompositeEnvelope(section);
        assertNotNull(section.path("data").get("content"));
        assertNoLegacyBadgeField(section);
        assertNoClientFabricatedNbaCdnUrls(section);
    }

    private static JsonNode firstScrollUnderUi(ObjectNode section) {
        return section.path("data").path("ui").path("children").path(0);
    }

    private static void assertCompositeEnvelope(ObjectNode section) {
        assertEquals("AtomicComposite", section.path("type").asText());
        assertTrue(section.has("data"));
        assertTrue(section.path("data").has("ui"));
    }

    private static void assertNoLegacyBadgeField(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            assertFalse(node.has("badge"), "legacy AtomicElement.badge must not be emitted by in-scope helpers");
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                assertNoLegacyBadgeField(node.get(names.next()));
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                assertNoLegacyBadgeField(c);
            }
        }
    }

    /**
     * Guards against server-side fabrication of NBA CDN URLs from identifiers — tests only pass
     * example.com / non-nba host image inputs plus {@code nba://} deeplinks.
     */
    private static void assertNoClientFabricatedNbaCdnUrls(JsonNode section) {
        String raw = section.toString();
        assertFalse(raw.contains("cdn.nba.com"), "output must not invent cdn.nba.com URLs");
        assertFalse(raw.contains("nba.com/"), "output must not invent nba.com web URLs");
        assertFalse(raw.contains("https://nba"), "output must not invent https://nba… URLs");
    }

    private static void assertNoOutlinedOverMediaVariant(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            if ("Button".equals(node.path("type").asText())) {
                assertFalse("outlinedOverMedia".equals(node.path("variant").asText()),
                        "outlinedOverMedia variant is forbidden for on-media CTAs");
            }
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                assertNoOutlinedOverMediaVariant(node.get(names.next()));
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                assertNoOutlinedOverMediaVariant(c);
            }
        }
    }

    /**
     * For every {@code OverlayContainer} whose base is an {@code Image}, any {@code Text} node in an
     * overlay subtree must sit under a solid or scrim gradient backing (chip badges use solid fills;
     * title/copy uses {@link #OVERLAY_SCRIM_TOKEN} gradients).
     */
    private static void assertOverlayImageStacksHaveReadableTextBacking(JsonNode root) {
        List<JsonNode> overlays = new ArrayList<>();
        collectOverlayContainers(root, overlays);
        for (JsonNode oc : overlays) {
            if (!"Image".equals(oc.path("base").path("type").asText())) {
                continue;
            }
            JsonNode layers = oc.get("overlays");
            if (layers == null || !layers.isArray()) {
                continue;
            }
            for (JsonNode layer : layers) {
                JsonNode el = layer.get("element");
                if (el == null) {
                    continue;
                }
                List<JsonNode> texts = new ArrayList<>();
                collectElementsOfType(el, "Text", texts);
                for (JsonNode textNode : texts) {
                    assertTrue(
                            textHasContrastAncestor(el, textNode),
                            "Text over image must have scrim or solid backing: " + textNode);
                }
            }
        }
    }

    private static void collectOverlayContainers(JsonNode node, List<JsonNode> out) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            if ("OverlayContainer".equals(node.path("type").asText())) {
                out.add(node);
            }
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                collectOverlayContainers(node.get(names.next()), out);
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                collectOverlayContainers(c, out);
            }
        }
    }

    private static void collectElementsOfType(JsonNode node, String type, List<JsonNode> out) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            if (type.equals(node.path("type").asText())) {
                out.add(node);
            }
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                collectElementsOfType(node.get(names.next()), type, out);
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                collectElementsOfType(c, type, out);
            }
        }
    }

    private static boolean textHasContrastAncestor(JsonNode overlayRoot, JsonNode textNode) {
        Deque<JsonNode> stack = new ArrayDeque<>();
        if (!findPathToText(overlayRoot, textNode, stack)) {
            return false;
        }
        // Iterator order: text leaf at logical index 0, overlay element root at end.
        ArrayList<JsonNode> path = new ArrayList<>(stack);
        for (int i = 1; i < path.size(); i++) {
            if (nodeProvidesTextContrast(path.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean findPathToText(JsonNode current, JsonNode target, Deque<JsonNode> path) {
        path.push(current);
        if (current == target) {
            return true;
        }
        if (current.isObject()) {
            Iterator<String> names = current.fieldNames();
            while (names.hasNext()) {
                JsonNode child = current.get(names.next());
                if (findPathToText(child, target, path)) {
                    return true;
                }
            }
        } else if (current.isArray()) {
            for (JsonNode c : current) {
                if (findPathToText(c, target, path)) {
                    return true;
                }
            }
        }
        path.pop();
        return false;
    }

    private static boolean nodeProvidesTextContrast(JsonNode node) {
        JsonNode bg = node.get("background");
        if (bg == null || bg.isNull() || bg.isMissingNode()) {
            return false;
        }
        if (bg.isTextual()) {
            String s = bg.asText();
            return !s.isBlank();
        }
        if (bg.isObject()) {
            JsonNode colors = bg.get("colors");
            if (colors != null && colors.isArray() && colors.size() >= 2) {
                String last = colors.get(colors.size() - 1).asText("");
                if (last.contains("overlay.scrim") || last.equals(OVERLAY_SCRIM_TOKEN)) {
                    return true;
                }
                // Production `mediaBottomScrimGradient` emits a hardcoded
                // ARGB gradient (alpha-byte first) ending in an opaque-dark
                // color rather than the token, because `overlay.scrim`
                // inverts in dark mode and defeats the darkening intent.
                // Accept any gradient whose final color is an opaque-dark
                // hex value as a valid scrim backing.
                return isOpaqueDarkHex(last);
            }
        }
        return false;
    }

    /**
     * Returns true for ARGB or RGB hex strings whose alpha byte is high enough
     * (≥ 0xC0) to provide a darkening scrim. 6-char #RRGGBB is always opaque.
     */
    private static boolean isOpaqueDarkHex(String hex) {
        if (hex == null || !hex.startsWith("#")) {
            return false;
        }
        String body = hex.substring(1);
        if (body.length() == 6) {
            return true; // #RRGGBB → fully opaque
        }
        if (body.length() == 8) {
            try {
                int alpha = Integer.parseInt(body.substring(0, 2), 16);
                return alpha >= 0xC0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }
}
