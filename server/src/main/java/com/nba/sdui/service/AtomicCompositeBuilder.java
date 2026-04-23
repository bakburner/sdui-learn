package com.nba.sdui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds AtomicComposite sections using atomic element trees.
 *
 * Each method returns a complete section envelope (id, type, analyticsId, refreshPolicy, data)
 * with {@code type: "AtomicComposite"} and {@code data.ui} containing the rendering tree.
 *
 * Migrated section types (server-composed, no client renderers):
 * ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail,
 * HeroPanel, VideoCarousel, StatLine, NbaTvSchedule.
 */
public class AtomicCompositeBuilder {

    private final ObjectMapper om;

    public AtomicCompositeBuilder(ObjectMapper objectMapper) {
        this.om = objectMapper;
    }

    // ── ErrorState ──────────────────────────────────────────────────────

    public ObjectNode buildErrorState(String sectionId, String title, String message,
                                       String icon, String retryUri) {
        ObjectNode section = sectionEnvelope(sectionId, null);

        String emoji = switch (icon) {
            case "wifi_off" -> "\uD83D\uDCE1";
            case "not_found" -> "\uD83D\uDD0D";
            case "timeout" -> "⏱\uFE0F";
            default -> "⚠\uFE0F";
        };

        ObjectNode root = container("column", "center", "center");
        root.set("padding", padding(16, 16, 32, 32));
        ArrayNode children = om.createArrayNode();

        children.add(text(emoji, "titleLarge", null, null, null));
        children.add(spacer(12));
        children.add(text(title, "titleMedium", "bold", null, null));

        if (message != null && !message.isBlank()) {
            children.add(spacer(8));
            children.add(text(message, "bodyMedium", null, ColorTokens.TEXT_SECONDARY, null));
        }

        if (retryUri != null) {
            children.add(spacer(16));
            children.add(button("Try Again", "primary", tapNavigate(retryUri)));
        }

        root.set("children", children);
        wrapUi(section, root);
        return section;
    }

    // ── SectionHeader ───────────────────────────────────────────────────

    public ObjectNode buildSectionHeader(String id, String title,
                                          String subtitle, String actionLabel,
                                          String actionUri) {
        ObjectNode section = sectionEnvelope(id, null);

        ObjectNode root = container("row", "spaceBetween", "center");
        root.set("padding", padding(16, 16, 12, 12));
        ArrayNode children = om.createArrayNode();

        if (subtitle != null) {
            ObjectNode titleCol = container("column", null, null);
            ArrayNode titleChildren = om.createArrayNode();
            titleChildren.add(text(title, "titleMedium", "bold", null, null));
            titleChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_TERTIARY, null));
            titleCol.set("children", titleChildren);
            children.add(titleCol);
        } else {
            children.add(text(title, "titleMedium", "bold", null, null));
        }

        if (actionUri != null) {
            String label = actionLabel != null ? actionLabel : "See All";
            children.add(button(label, "text", tapNavigate(actionUri)));
        }

        root.set("children", children);
        wrapUi(section, root);
        return section;
    }

    // ── PromoBanner ─────────────────────────────────────────────────────

    public ObjectNode buildPromoBanner(String id, String analyticsId,
                                        String title, String headline,
                                        String subhead, String imageUrl,
                                        String ctaLabel, String targetUri) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        // Emit a bare root container — outer chrome (background, padding,
        // radius, shadow, margin) is owned exclusively by the shared
        // SectionContainer wrapper via `section.surface`. Callers must
        // supply `section.surface` on the returned envelope.
        ObjectNode root = bannerContainer("row", null, "center");
        ArrayNode rootChildren = om.createArrayNode();

        if (imageUrl != null) {
            ObjectNode img = image(imageUrl, 64, 64, "contain");
            img.put("cornerRadius", 8);
            rootChildren.add(img);
            rootChildren.add(spacer(16));
        }

        ObjectNode contentCol = container("column", null, null);
        ArrayNode colChildren = om.createArrayNode();

        if (title != null) {
            colChildren.add(text(title.toUpperCase(), "labelSmall", "bold", ColorTokens.BRAND_LIVE, null));
            colChildren.add(spacer(4));
        }
        if (headline != null) {
            colChildren.add(text(headline, "titleMedium", "bold", ColorTokens.TEXT_INVERSE, null));
            colChildren.add(spacer(4));
        }
        if (subhead != null) {
            colChildren.add(text(subhead, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null));
        }
        if (targetUri != null) {
            colChildren.add(spacer(12));
            String label = ctaLabel != null ? ctaLabel : "Learn More";
            colChildren.add(button(label, "primary", tapNavigate(targetUri)));
        }

        contentCol.set("children", colChildren);
        rootChildren.add(contentCol);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    // ── ContentRail ─────────────────────────────────────────────────────

    /**
     * Build a ContentRail as an AtomicComposite.
     * Each card is composed as an atomic Container with Image + Text children.
     *
     * @param cards  Array of [id, headline, subhead, thumbnailUrl, contentType, duration, targetUri]
     */
    public ObjectNode buildContentRail(String id, String analyticsId,
                                        String title, String[][] cards) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 12, 12));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            ObjectNode titlePad = om.createObjectNode();
            titlePad.put("start", 16); titlePad.put("end", 16);
            titlePad.put("top", 8); titlePad.put("bottom", 8);
            titleEl.set("padding", titlePad);
            rootChildren.add(titleEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", 12);
        scroll.put("showIndicators", false);
        ArrayNode scrollChildren = om.createArrayNode();

        for (String[] c : cards) {
            scrollChildren.add(buildContentCard(c[0], c[1], c[2], c[3], c[4], c[5], c[6]));
        }

        scroll.set("children", scrollChildren);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    private ObjectNode buildContentCard(String id, String headline, String subhead,
                                          String thumbnailUrl, String contentType,
                                          String duration, String targetUri) {
        ObjectNode card = elevatedContainer("column", null, null);
        card.put("id", id);
        // Fix the card's outer width so the image (also 200) and any
        // full-width overlays (duration badge strip) meet the card edge
        // flush. Without this the card sizes to max(child intrinsic
        // widths) — a long 2-line headline would push the card past
        // 200, leaving an empty right gutter next to the image.
        card.put("width", 200);
        // Rounded top, square bottom. The `elevated` variant's default 12pt
        // radius on all four corners pulled the bottom-left corner curve up
        // into the headline's first glyph, clipping the leading letter.
        // Squaring the bottom corners gives the headline full-width breathing
        // room without reducing the card's visual weight (rounded-top +
        // squared-bottom is a standard news/streaming-card silhouette).
        card.set("cornerRadii", cornerRadii(12, 12, 0, 0));
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode img = thumbnailImage(thumbnailUrl);
            // Card is the sole width anchor; the image stretches to the
            // card's interior and derives its height from aspectRatio.
            // This avoids duplicating the 200pt number on both card and
            // image, and lets the rail re-theme by tuning card.width in
            // one place.
            img.put("fillWidth", true);
            img.put("aspectRatio", 16.0 / 9.0);
            // Card outer clip (elevated variant + cornerRadii) owns the
            // rounded top corners — suppress the thumbnail variant's 8pt
            // radius so the image meets the card edge flush.
            img.put("cornerRadius", 0);
            if (duration != null) {
                badge(img, durationBadge(duration), "bottomEnd");
            } else if ("video".equalsIgnoreCase(contentType)) {
                badge(img, liveBadge(), "bottomEnd");
            }
            children.add(img);
        }

        children.add(spacer(8));
        ObjectNode headlineEl = text(headline, "bodySmall", "semiBold", ColorTokens.TEXT_PRIMARY, 2);
        headlineEl.set("padding", padding(8, 8, 8, 8));
        children.add(headlineEl);

        card.set("children", children);
        return card;
    }

    // ── FollowingRail ───────────────────────────────────────────────────

    /**
     * Build a FollowingRail as an AtomicComposite.
     * Each item is a circular avatar + name label.
     *
     * @param items  Array of [id, name, imageUrl, entityType, targetUri]
     */
    public ObjectNode buildFollowingRail(String id, String analyticsId,
                                          String title, String[][] items) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 12, 12));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            ObjectNode titlePad = om.createObjectNode();
            titlePad.put("start", 16); titlePad.put("end", 16);
            titlePad.put("top", 4); titlePad.put("bottom", 4);
            titleEl.set("padding", titlePad);
            rootChildren.add(titleEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", 16);
        scroll.put("showIndicators", false);
        ArrayNode scrollChildren = om.createArrayNode();

        for (String[] item : items) {
            scrollChildren.add(buildFollowingItem(item[0], item[1], item[2], item[4]));
        }

        scroll.set("children", scrollChildren);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    private ObjectNode buildFollowingItem(String id, String name, String imageUrl,
                                            String targetUri) {
        ObjectNode item = container("column", "center", "center");
        item.put("id", id);
        if (targetUri != null) {
            item.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        if (imageUrl != null) {
            ObjectNode img = image(imageUrl, 56, 56, "cover");
            img.put("cornerRadius", 28);
            children.add(img);
        } else {
            ObjectNode fallback = text(name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase(),
                    "labelSmall", "bold", ColorTokens.TEXT_TERTIARY, null);
            children.add(fallback);
        }

        children.add(spacer(4));
        ObjectNode nameEl = text(name, "labelSmall", null, null, 1);
        nameEl.put("maxLines", 1);
        children.add(nameEl);

        item.set("children", children);
        return item;
    }

    // ── DisplayGrid ───────────────────────────────────────────────────

    /**
     * Build a DisplayGrid as an AtomicComposite.
     *
     * @param columns  Array of [key, label, align] — align is "start", "center", or "end"
     * @param rows     Array of Maps mapping column keys to pre-formatted display values
     */
    public ObjectNode buildDisplayGrid(String id, String analyticsId,
                                        String title,
                                        String[][] columns, String[][] rows,
                                        boolean striped) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 12, 12));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            ObjectNode titlePad = om.createObjectNode();
            titlePad.put("start", 16); titlePad.put("end", 16);
            titlePad.put("top", 8); titlePad.put("bottom", 8);
            titleEl.set("padding", titlePad);
            rootChildren.add(titleEl);
        }

        ObjectNode grid = om.createObjectNode();
        grid.put("type", "DisplayGrid");
        grid.put("id", id + "-grid");
        grid.put("striped", striped);

        ArrayNode colArray = om.createArrayNode();
        for (String[] col : columns) {
            ObjectNode colNode = om.createObjectNode();
            colNode.put("key", col[0]);
            colNode.put("label", col[1]);
            if (col.length > 2 && col[2] != null) colNode.put("align", col[2]);
            colArray.add(colNode);
        }
        grid.set("columns", colArray);

        ArrayNode rowArray = om.createArrayNode();
        for (String[] row : rows) {
            ObjectNode rowNode = om.createObjectNode();
            for (int i = 0; i < columns.length && i < row.length; i++) {
                rowNode.put(columns[i][0], row[i]);
            }
            rowArray.add(rowNode);
        }
        grid.set("rows", rowArray);

        rootChildren.add(grid);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ADDITIONAL MIGRATED SECTIONS
    // ══════════════════════════════════════════════════════════════════════

    // ── HeroPanel ────────────────────────────────────────────────────────

    /**
     * Build a HeroPanel as an AtomicComposite.
     * Single content card: thumbnail + optional duration badge + content type + headline + subhead.
     */
    public ObjectNode buildHeroPanel(String id, String analyticsId,
                                      String headline, String subhead,
                                      String thumbnailUrl, String contentType,
                                      String duration, String targetUri) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode card = heroContainer("column", null, null);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode imgContainer = container("column", null, null);
            ArrayNode imgChildren = om.createArrayNode();
            ObjectNode img = heroImage(thumbnailUrl);
            imgChildren.add(img);
            if (duration != null) {
                ObjectNode dur = text(duration, "labelSmall", null, ColorTokens.TEXT_INVERSE, null);
                dur.set("padding", padding(8, 8, 0, 0));
                imgChildren.add(dur);
            }
            imgContainer.set("children", imgChildren);
            children.add(imgContainer);
        }

        ObjectNode textCol = container("column", null, null);
        textCol.set("padding", padding(12, 12, 12, 12));
        ArrayNode textChildren = om.createArrayNode();

        if (contentType != null) {
            ObjectNode badge = text(contentType.toUpperCase(), "labelSmall", "bold", ColorTokens.BRAND_LIVE, null);
            ObjectNode badgePad = padding(0, 0, 0, 4);
            badge.set("padding", badgePad);
            textChildren.add(badge);
        }

        textChildren.add(text(headline, "titleSmall", "bold", ColorTokens.TEXT_PRIMARY, 2));

        if (subhead != null) {
            ObjectNode sub = text(subhead, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 2);
            sub.set("padding", padding(0, 0, 4, 0));
            textChildren.add(sub);
        }

        textCol.set("children", textChildren);
        children.add(textCol);
        card.set("children", children);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(16, 16, 8, 8));
        ArrayNode rootChildren = om.createArrayNode();
        rootChildren.add(card);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    // ── VideoCarousel ────────────────────────────────────────────────────

    /**
     * Build a VideoCarousel as an AtomicComposite.
     * Title + subtitle + horizontal scroll of video thumbnail cards.
     *
     * @param items  Array of [id, title, subtitle, thumbnailUrl, duration, badgeText, targetUri]
     */
    public ObjectNode buildVideoCarousel(String id, String analyticsId,
                                          String title, String subtitle,
                                          String[][] items) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 8, 8));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            titleEl.set("padding", padding(16, 16, 4, 4));
            rootChildren.add(titleEl);
        }
        if (subtitle != null) {
            ObjectNode subEl = text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null);
            subEl.set("padding", padding(16, 16, 2, 2));
            rootChildren.add(subEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", 12);
        scroll.put("showIndicators", false);
        ArrayNode scrollChildren = om.createArrayNode();

        for (String[] item : items) {
            scrollChildren.add(buildVideoCard(item[0], item[1], item[2],
                    item[3], item[4], item[5], item[6]));
        }

        scroll.set("children", scrollChildren);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    private ObjectNode buildVideoCard(String id, String title, String subtitle,
                                        String thumbnailUrl, String duration,
                                        String badgeText, String targetUri) {
        ObjectNode card = elevatedContainer("column", null, null);
        card.put("id", id);
        // Fix the card's outer width so the 240pt image + full-width
        // meta row (duration / live badge) meet the card edge flush.
        // See buildContentCard for the rationale.
        card.put("width", 240);
        // Rounded top, square bottom — same rationale as buildContentCard:
        // prevents the bottom-corner curve from clipping into the title's
        // first glyph and matches the news/streaming-card silhouette.
        card.set("cornerRadii", cornerRadii(12, 12, 0, 0));
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode thumbContainer = container("column", null, null);
        ArrayNode thumbChildren = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode img = thumbnailImage(thumbnailUrl);
            // Card owns the width; image stretches + derives height from
            // aspect ratio. See buildContentCard for the rationale.
            img.put("fillWidth", true);
            img.put("aspectRatio", 16.0 / 9.0);
            // Card outer clip owns the rounded top; suppress the thumbnail
            // variant's 8pt radius so the image meets the card edge cleanly.
            img.put("cornerRadius", 0);
            thumbChildren.add(img);
        }

        ObjectNode metaContainer = container("row", "spaceBetween", "center");
        metaContainer.set("padding", padding(6, 6, 6, 6));
        ArrayNode metaChildren = om.createArrayNode();

        if (badgeText != null) {
            ObjectNode badge = text(badgeText, "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null);
            badge.put("background", ColorTokens.BRAND_LIVE);
            metaChildren.add(badge);
        } else {
            metaChildren.add(spacer(1));
        }

        if (duration != null) {
            ObjectNode dur = text(duration, "labelSmall", null, ColorTokens.TEXT_INVERSE, null);
            dur.put("background", "#000000B3");
            metaChildren.add(dur);
        }

        metaContainer.set("children", metaChildren);
        thumbChildren.add(metaContainer);
        thumbContainer.set("children", thumbChildren);
        children.add(thumbContainer);

        ObjectNode textCol = container("column", null, null);
        textCol.set("padding", padding(10, 10, 10, 10));
        ArrayNode textChildren = om.createArrayNode();
        textChildren.add(text(title, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 2));
        if (subtitle != null) {
            ObjectNode sub = text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 1);
            sub.set("padding", padding(0, 0, 2, 0));
            textChildren.add(sub);
        }
        textCol.set("children", textChildren);
        children.add(textCol);

        card.set("children", children);
        return card;
    }

    // ── StatLine ─────────────────────────────────────────────────────────

    /**
     * Build a StatLine section as an AtomicComposite.
     * Supports horizontal (inline row) and vertical (stacked) layout.
     *
     * @param layout  "horizontal" or "vertical"
     * @param stats   Array of [playerId, playerName, teamTricode, statCategory, statValue, playerImageUrl]
     */
    public ObjectNode buildStatLine(String id, String analyticsId,
                                     String title, String layout,
                                     String[][] stats) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(16, 16, 8, 8));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.set("padding", padding(0, 0, 0, 12));
            rootChildren.add(titleEl);
        }

        boolean isVertical = "vertical".equals(layout);
        for (String[] stat : stats) {
            rootChildren.add(buildStatRow(stat[0], stat[1], stat[2], stat[3], stat[4],
                    stat.length > 5 ? stat[5] : null, isVertical));
        }

        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    /**
     * Overload that accepts pre-built stat ObjectNodes from dynamic API data.
     * Each node should have: playerName, teamTricode, statCategory, statValue, playerImageUrl (optional).
     */
    public ObjectNode buildStatLineFromNodes(String id, String analyticsId,
                                              String title, String layout,
                                              ArrayNode statsNodes) {
        int count = statsNodes != null ? statsNodes.size() : 0;
        String[][] stats = new String[count][];
        for (int i = 0; i < count; i++) {
            var node = statsNodes.get(i);
            stats[i] = new String[]{
                String.valueOf(node.path("playerId").asInt()),
                node.path("playerName").asText(""),
                node.path("teamTricode").asText(""),
                node.path("statCategory").asText(""),
                node.path("statValue").asText(""),
                node.has("playerImageUrl") ? node.path("playerImageUrl").asText() : null
            };
        }
        return buildStatLine(id, analyticsId, title, layout, stats);
    }

    private ObjectNode buildStatRow(String playerId, String playerName, String teamTricode,
                                      String statCategory, String statValue,
                                      String playerImageUrl, boolean isVertical) {
        if (isVertical) {
            return buildStatRowVertical(playerId, playerName, teamTricode,
                    statCategory, statValue, playerImageUrl);
        }
        return buildStatRowHorizontal(playerId, playerName, teamTricode,
                statCategory, statValue, playerImageUrl);
    }

    private ObjectNode buildStatRowHorizontal(String playerId, String playerName,
                                                String teamTricode, String statCategory,
                                                String statValue, String playerImageUrl) {
        ObjectNode row = container("row", null, "center");
        row.set("padding", padding(0, 0, 6, 6));
        ArrayNode children = om.createArrayNode();

        if (playerImageUrl != null) {
            ObjectNode img = image(playerImageUrl, 40, 40, "cover");
            img.put("cornerRadius", 20);
            children.add(img);
            children.add(spacer(12));
        }

        ObjectNode nameCol = container("column", null, null);
        ArrayNode nameChildren = om.createArrayNode();
        nameChildren.add(text(playerName, "bodyLarge", "medium", null, null));
        if (teamTricode != null) {
            nameChildren.add(text(teamTricode, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null));
        }
        nameCol.set("children", nameChildren);
        children.add(nameCol);

        children.add(spacer(8));
        children.add(text(statCategory, "bodyMedium", null, ColorTokens.TEXT_SECONDARY, null));
        children.add(spacer(8));
        children.add(text(statValue, "titleMedium", "bold", ColorTokens.BRAND_LIVE, null));

        row.set("children", children);
        return row;
    }

    private ObjectNode buildStatRowVertical(String playerId, String playerName,
                                              String teamTricode, String statCategory,
                                              String statValue, String playerImageUrl) {
        ObjectNode col = container("column", null, null);
        col.set("padding", padding(0, 0, 6, 6));
        ArrayNode children = om.createArrayNode();

        ObjectNode topRow = container("row", null, "center");
        ArrayNode topChildren = om.createArrayNode();

        if (playerImageUrl != null) {
            ObjectNode img = image(playerImageUrl, 40, 40, "cover");
            img.put("cornerRadius", 20);
            topChildren.add(img);
            topChildren.add(spacer(12));
        }

        ObjectNode nameCol = container("column", null, null);
        ArrayNode nameChildren = om.createArrayNode();
        nameChildren.add(text(playerName, "bodyLarge", "medium", null, null));
        if (teamTricode != null) {
            nameChildren.add(text(teamTricode, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null));
        }
        nameCol.set("children", nameChildren);
        topChildren.add(nameCol);
        topRow.set("children", topChildren);
        children.add(topRow);

        ObjectNode bottomRow = container("row", "end", "center");
        bottomRow.set("padding", padding(0, 0, 4, 0));
        ArrayNode bottomChildren = om.createArrayNode();
        bottomChildren.add(text(statCategory, "bodyMedium", null, ColorTokens.TEXT_SECONDARY, null));
        bottomChildren.add(spacer(8));
        bottomChildren.add(text(statValue, "titleMedium", "bold", ColorTokens.BRAND_LIVE, null));
        bottomRow.set("children", bottomChildren);
        children.add(bottomRow);

        col.set("children", children);
        return col;
    }

    // ── GamePanelDisplayConfig presets ────────────────────────────────

    public ObjectNode standardConfig() {
        ObjectNode config = om.createObjectNode();
        config.put("background", ColorTokens.SURFACE_CANVAS);
        return config;
    }

    public ObjectNode featuredConfig(String bgImageUrl, String[] liveBgGradientColors) {
        ObjectNode config = om.createObjectNode();
        config.put("logoSize", 56);
        config.put("cardHeight", 200);
        config.put("cornerRadius", 16);
        config.put("elevation", 6);
        config.put("scoreTextStyle", "prominent");
        if (bgImageUrl != null) {
            ObjectNode bgImage = om.createObjectNode();
            bgImage.put("imageUrl", bgImageUrl);
            config.set("background", bgImage);
        } else {
            config.put("background", ColorTokens.PALETTE_BLUE_30);
        }
        if (liveBgGradientColors != null) {
            ObjectNode grad = om.createObjectNode();
            ArrayNode colors = om.createArrayNode();
            for (String c : liveBgGradientColors) colors.add(c);
            grad.set("colors", colors);
            grad.put("direction", "horizontal");
            config.set("liveBackground", grad);
        }
        config.put("badgeColor", ColorTokens.BRAND_LIVE);
        return config;
    }

    public ObjectNode scoreboardConfig(String bgColor) {
        ObjectNode config = om.createObjectNode();
        config.put("logoSize", 60);
        config.put("scoreTextStyle", "prominent");
        config.put("background", bgColor != null ? bgColor : ColorTokens.BRAND_NBA);
        return config;
    }

    // ── NbaTvSchedule ────────────────────────────────────────────────────

    /**
     * Build an NbaTvSchedule as an AtomicComposite.
     * Hero banner with gradient overlay + time-slot list.
     *
     * @param slots  Array of [id, title, subtitle, displayTime, isLive, targetUri]
     */
    public ObjectNode buildNbaTvSchedule(String id, String analyticsId,
                                          String heroImageUrl, String heroTitle,
                                          String heroSubtitle, boolean liveNow,
                                          String[][] slots) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        ObjectNode root = container("column", null, null);
        root.put("background", ColorTokens.SURFACE_SUNKEN);
        root.put("cornerRadius", 12);
        ArrayNode rootChildren = om.createArrayNode();

        ObjectNode heroContainer = container("column", "end", null);
        heroContainer.put("cornerRadius", 12);
        heroContainer.set("padding", padding(16, 16, 8, 8));
        ArrayNode heroChildren = om.createArrayNode();

        if (heroImageUrl != null) {
            ObjectNode heroImg = image(heroImageUrl, 0, 200, "cover");
            heroImg.put("fillWidth", true);
            heroImg.put("cornerRadius", 12);
            heroChildren.add(heroImg);
        }

        ObjectNode overlay = container("column", null, null);
        overlay.set("padding", padding(16, 16, 16, 16));
        ObjectNode grad = om.createObjectNode();
        ArrayNode gradColors = om.createArrayNode();
        gradColors.add("#00000000");
        gradColors.add("#000000CC");
        grad.set("colors", gradColors);
        grad.put("direction", "vertical");
        overlay.set("background", grad);
        ArrayNode overlayChildren = om.createArrayNode();

        if (liveNow) {
            ObjectNode liveBadge = text("LIVE", "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null);
            liveBadge.put("background", ColorTokens.BRAND_LIVE);
            overlayChildren.add(liveBadge);
            overlayChildren.add(spacer(6));
        }
        if (heroTitle != null) {
            overlayChildren.add(text(heroTitle, "titleLarge", "bold", ColorTokens.TEXT_INVERSE, null));
        }
        if (heroSubtitle != null) {
            overlayChildren.add(text(heroSubtitle, "bodyMedium", null, ColorTokens.TEXT_SECONDARY, null));
        }
        overlay.set("children", overlayChildren);
        heroChildren.add(overlay);
        heroContainer.set("children", heroChildren);
        rootChildren.add(heroContainer);

        rootChildren.add(spacer(8));

        ObjectNode heading = text("Today's Schedule", "titleSmall", "bold", ColorTokens.TEXT_PRIMARY, null);
        heading.set("padding", padding(16, 16, 4, 4));
        rootChildren.add(heading);

        ObjectNode slotList = container("column", null, null);
        slotList.put("gap", 8);
        slotList.set("padding", padding(16, 16, 0, 0));
        ArrayNode slotChildren = om.createArrayNode();

        for (String[] slot : slots) {
            slotChildren.add(buildNbaTvSlot(slot[0], slot[1], slot[2],
                    slot[3], "true".equals(slot[4]), slot[5]));
        }

        slotList.set("children", slotChildren);
        rootChildren.add(slotList);
        root.set("children", rootChildren);
        wrapUi(section, root);
        return section;
    }

    private ObjectNode buildNbaTvSlot(String id, String title, String subtitle,
                                        String displayTime, boolean isLive,
                                        String targetUri) {
        ObjectNode row = container("row", null, "center");
        row.put("id", id);
        row.put("fillWidth", true);
        row.put("cornerRadius", 8);
        row.put("background", ColorTokens.SURFACE_CANVAS);
        row.set("padding", padding(12, 12, 12, 12));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode timeText = text(displayTime, "bodyMedium", "semiBold", ColorTokens.TEXT_SECONDARY, null);
        children.add(timeText);
        children.add(spacer(12));

        ObjectNode contentCol = container("column", null, null);
        ArrayNode contentChildren = om.createArrayNode();
        contentChildren.add(text(title, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 1));
        if (subtitle != null) {
            contentChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 1));
        }
        contentCol.set("children", contentChildren);
        children.add(contentCol);

        if (isLive) {
            children.add(spacer(8));
            ObjectNode badge = text("LIVE", "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null);
            badge.put("background", ColorTokens.BRAND_LIVE);
            children.add(badge);
        }

        row.set("children", children);
        return row;
    }

    // ── Phase 0.4 styling helpers ───────────────────────────────────────

    /** Attach a drop shadow to any element node. */
    public ObjectNode shadow(ObjectNode element, String color, double radius, double offsetX, double offsetY) {
        ObjectNode s = om.createObjectNode();
        if (color != null) s.put("color", color);
        s.put("radius", radius);
        s.put("offsetX", offsetX);
        s.put("offsetY", offsetY);
        element.set("shadow", s);
        return element;
    }

    /** Attach a shadow with default dark color. */
    public ObjectNode shadow(ObjectNode element) {
        return shadow(element, "#00000014", 4, 0, 2);
    }

    /** Attach a badge overlay to a parent element. */
    public ObjectNode badge(ObjectNode parent, ObjectNode badgeElement, String alignment) {
        ObjectNode b = om.createObjectNode();
        b.set("element", badgeElement);
        if (alignment != null) b.put("alignment", alignment);
        parent.set("badge", b);
        return parent;
    }

    /** Set opacity on an element (0.0 = transparent, 1.0 = opaque). */
    public ObjectNode opacity(ObjectNode element, double value) {
        element.put("opacity", value);
        return element;
    }

    /** Set text alignment: "start", "center", or "end". */
    public ObjectNode textAlign(ObjectNode element, String align) {
        element.put("textAlign", align);
        return element;
    }

    /** Enable monospaced/tabular digit rendering on a text element. */
    public ObjectNode monospacedDigits(ObjectNode element) {
        element.put("monospacedDigits", true);
        return element;
    }

    /** Set showIndicators flag on a ScrollContainer element. */
    public ObjectNode showIndicators(ObjectNode element, boolean show) {
        element.put("showIndicators", show);
        return element;
    }

    /** Build a duration badge element (dark background pill with white text). */
    public ObjectNode durationBadge(String duration) {
        ObjectNode bg = container("row", "center", "center");
        bg.put("cornerRadius", 4);
        bg.put("background", "#000000B3");
        // iOS ref app uses 0.7 for duration pill opacity; normalised here so
        // Android + web match without each platform inventing its own value.
        opacity(bg, 0.7);
        bg.set("padding", padding(4, 4, 2, 2));
        ArrayNode children = om.createArrayNode();
        children.add(text(duration, "labelSmall", "semiBold", ColorTokens.TEXT_INVERSE, null));
        bg.set("children", children);
        return bg;
    }

    /** Build a "LIVE" badge element (red pill with white text). */
    public ObjectNode liveBadge() {
        ObjectNode bg = container("row", "center", "center");
        bg.put("cornerRadius", 4);
        bg.put("background", ColorTokens.BRAND_LIVE);
        bg.set("padding", padding(6, 6, 2, 2));
        ArrayNode children = om.createArrayNode();
        children.add(text("LIVE", "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null));
        bg.set("children", children);
        return bg;
    }

    // ── Atomic element helpers ──────────────────────────────────────────

    /**
     * Build a SectionSlot element that embeds a full section object inside
     * an atomic tree. At render time the client delegates this back to
     * SectionRouter, completing the bidirectional bridge.
     */
    public ObjectNode sectionSlot(String id, ObjectNode section) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "SectionSlot");
        if (id != null) node.put("id", id);
        node.set("section", section);
        return node;
    }

    /**
     * Public convenience: wrap a root element as a full AtomicComposite section.
     */
    public ObjectNode wrapAsComposite(String sectionId, String analyticsId,
                                       ObjectNode rootElement) {
        ObjectNode section = sectionEnvelope(sectionId, analyticsId);
        wrapUi(section, rootElement);
        return section;
    }

    /**
     * Build a vertical VOD playlist composite — a grouped rounded surface
     * hosting a list of VOD rows (thumbnail, title, subtitle, chevron)
     * separated by dividers.
     *
     * <p>Shape mirrors the iOS ref app's {@code VODPlaylistView}: one
     * surface Container, optional header, N rows, N-1 inline dividers.
     * Each row is wrapped in {@code singleActionArray(tapNavigate(uri))}.
     *
     * @param sectionId    section id for the enclosing AtomicComposite
     * @param analyticsId  analyticsId for the enclosing AtomicComposite
     * @param header       optional header text; pass {@code null} to omit
     * @param rows         ordered list of row specs: {@code [id, title,
     *                     subtitle-or-null, thumbnailUrl, durationLabel-or-null,
     *                     isLive ("true"/"false"), targetUri]}
     */
    public ObjectNode buildVodPlaylist(String sectionId, String analyticsId,
                                       String header, String[][] rows) {
        ObjectNode surface = groupedContainer("column", null, null);
        surface.put("cornerRadius", 12);
        surface.set("padding", padding(0, 0, 4, 4));

        ArrayNode surfaceChildren = om.createArrayNode();
        if (header != null) {
            ObjectNode headerEl = text(header, "titleSmall", "semiBold", null, null);
            ObjectNode headerPad = padding(16, 16, 12, 8);
            headerEl.set("padding", headerPad);
            surfaceChildren.add(headerEl);
        }

        for (int i = 0; i < rows.length; i++) {
            String[] r = rows[i];
            String rowId = r[0];
            String title = r[1];
            String subtitle = (r.length > 2) ? r[2] : null;
            String thumbUrl = (r.length > 3) ? r[3] : null;
            String duration = (r.length > 4) ? r[4] : null;
            boolean isLive = (r.length > 5) && Boolean.parseBoolean(r[5]);
            String targetUri = (r.length > 6) ? r[6] : null;

            surfaceChildren.add(buildVodRow(rowId, title, subtitle, thumbUrl,
                    duration, isLive, targetUri));
            if (i < rows.length - 1) {
                surfaceChildren.add(vodDivider());
            }
        }
        surface.set("children", surfaceChildren);

        ObjectNode wrapper = container("column", null, null);
        wrapper.set("padding", padding(16, 16, 0, 0));
        ArrayNode wrapperKids = om.createArrayNode();
        wrapperKids.add(surface);
        wrapper.set("children", wrapperKids);

        return wrapAsComposite(sectionId, analyticsId, wrapper);
    }

    private ObjectNode buildVodRow(String id, String title, String subtitle,
                                   String thumbnailUrl, String duration,
                                   boolean isLive, String targetUri) {
        ObjectNode row = container("row", null, "center");
        row.put("id", id);
        row.put("gap", 12);
        row.set("padding", padding(12, 12, 10, 10));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
        }

        ArrayNode children = om.createArrayNode();
        if (thumbnailUrl != null) {
            ObjectNode thumb = thumbnailImage(thumbnailUrl);
            thumb.put("width", 80);
            thumb.put("height", 52);
            thumb.put("cornerRadius", 6);
            if (isLive) {
                badge(thumb, liveBadge(), "topStart");
            } else if (duration != null) {
                badge(thumb, durationBadge(duration), "bottomEnd");
            }
            children.add(thumb);
        }

        ObjectNode titleCol = container("column", null, "start");
        titleCol.put("gap", 2);
        setFlex(titleCol, 1.0);
        ArrayNode titleKids = om.createArrayNode();
        titleKids.add(text(title, "bodyMedium", "semiBold", null, 2));
        if (subtitle != null && !subtitle.isEmpty()) {
            titleKids.add(text(subtitle, "labelSmall", null, ColorTokens.TEXT_SECONDARY, 1));
        }
        titleCol.set("children", titleKids);
        children.add(titleCol);

        // Trailing chevron — semantic icon token; each client resolves to its native glyph.
        ObjectNode chevron = om.createObjectNode();
        chevron.put("type", "Text");
        chevron.put("content", "›");
        chevron.put("variant", "titleMedium");
        chevron.put("color", ColorTokens.TEXT_SECONDARY);
        children.add(chevron);

        row.set("children", children);
        return row;
    }

    private ObjectNode vodDivider() {
        ObjectNode d = om.createObjectNode();
        d.put("type", "Divider");
        d.put("thickness", 1);
        // Inset-left styling matches the iOS grouped-list idiom (divider starts
        // where text begins, not where thumbnail begins). Clients that can't
        // honour leading padding on a Divider render it edge-to-edge.
        ObjectNode pad = padding(104, 0, 0, 0);
        d.set("padding", pad);
        return d;
    }

    /**
     * Build a horizontally-scrolling carousel of GamePanel sections.
     *
     * <p>The first game is stamped with {@code variant: "featured"} and
     * given a wider wrapping container; the rest are {@code variant:
     * "standard"}. The carousel itself hides scroll indicators so cards
     * read as a curated rail. Width of each wrapper (280 vs 200) is the
     * server's composition call — {@code variant} controls only internal
     * emphasis (padding, corner radius, shadow) on each client.
     *
     * @param sectionId      section id for the enclosing AtomicComposite
     * @param analyticsId    analyticsId for the enclosing AtomicComposite
     * @param gameSections   ordered list of GamePanel sections to embed;
     *                       each must have a mutable "data" object node
     */
    public ObjectNode buildGameCarousel(String sectionId, String analyticsId,
                                        java.util.List<ObjectNode> gameSections) {
        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", 12);
        scroll.put("showIndicators", false);
        scroll.set("padding", padding(16, 16, 4, 4));

        ArrayNode kids = om.createArrayNode();
        for (int i = 0; i < gameSections.size(); i++) {
            ObjectNode game = gameSections.get(i);
            boolean isFeatured = (i == 0);

            ObjectNode data = (ObjectNode) game.get("data");
            if (data != null) {
                data.put("variant", isFeatured ? "featured" : "standard");
            }

            ObjectNode wrapper = container("column", null, null);
            wrapper.put("width", isFeatured ? 280 : 200);
            ArrayNode wrapperChildren = om.createArrayNode();
            wrapperChildren.add(sectionSlot("carousel-slot-" + i, game));
            wrapper.set("children", wrapperChildren);
            kids.add(wrapper);
        }
        scroll.set("children", kids);

        return wrapAsComposite(sectionId, analyticsId, scroll);
    }

    private ObjectNode sectionEnvelope(String id, String analyticsId) {
        return sectionEnvelope(id, analyticsId, null);
    }

    /**
     * Overload that accepts a custom refreshPolicy node (for poll, sse, etc.).
     * Falls back to static if refreshPolicy is null.
     */
    ObjectNode sectionEnvelope(String id, String analyticsId, ObjectNode refreshPolicy) {
        ObjectNode section = om.createObjectNode();
        section.put("id", id);
        section.put("type", "AtomicComposite");
        if (analyticsId != null) section.put("analyticsId", analyticsId);
        section.set("refreshPolicy", refreshPolicy != null ? refreshPolicy
                : om.createObjectNode().put("type", "static"));
        return section;
    }

    /** Attach sectionStates metadata to a section built by this builder. */
    public void attachSectionStates(ObjectNode section, ObjectNode sectionStates) {
        if (sectionStates != null) section.set("sectionStates", sectionStates);
    }

    /** Attach a refreshPolicy to an already-built section. */
    public void attachRefreshPolicy(ObjectNode section, ObjectNode refreshPolicy) {
        if (refreshPolicy != null) section.set("refreshPolicy", refreshPolicy);
    }

    /**
     * Attach an atomic root element to a section as its {@code data.ui}
     * payload, constructing a fresh {@code data} object. Used by the
     * AtomicComposite builders above.
     */
    private void wrapUi(ObjectNode section, ObjectNode rootElement) {
        section.set("data", wrapUi(rootElement));
    }

    /**
     * Build the {@code data} object for a section whose full visible surface
     * is expressed as an atomic tree under {@code data.ui}. Shared with
     * permanent-section composers (SubscribeBanner / SubscribeHero /
     * VideoPlayer) that use the same on-wire shape as AtomicComposite but
     * additionally expose top-level domain-data fields (ctaAction, tiers,
     * playerType, ...) the client reads alongside the tree.
     */
    ObjectNode wrapUi(ObjectNode rootElement) {
        ObjectNode data = om.createObjectNode();
        data.set("ui", rootElement);
        return data;
    }

    ObjectNode container(String direction, String alignment, String crossAlignment) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Container");
        if (direction != null) node.put("direction", direction);
        if (alignment != null) node.put("alignment", alignment);
        if (crossAlignment != null) node.put("crossAlignment", crossAlignment);
        return node;
    }

    /**
     * Container preset that emits {@code variant}. Each platform resolves the
     * string against its ContainerVariant enum and supplies a native realization
     * (material, tonal elevation, gradient, shadow). Known values: "hero",
     * "elevated", "banner", "subtle", "grouped", "overlay". Inline style
     * properties may still be set on the returned node and win over the
     * variant default for axes the variant's override matrix marks as
     * {@code allow}.
     */
    ObjectNode variantContainer(String variant, String direction,
                                String alignment, String crossAlignment) {
        ObjectNode node = container(direction, alignment, crossAlignment);
        if (variant != null) node.put("variant", variant);
        return node;
    }

    ObjectNode heroContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("hero", direction, alignment, crossAlignment);
    }

    ObjectNode elevatedContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("elevated", direction, alignment, crossAlignment);
    }

    ObjectNode bannerContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("banner", direction, alignment, crossAlignment);
    }

    ObjectNode subtleContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("subtle", direction, alignment, crossAlignment);
    }

    ObjectNode groupedContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("grouped", direction, alignment, crossAlignment);
    }

    ObjectNode overlayContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("overlay", direction, alignment, crossAlignment);
    }

    /**
     * Build a responsive row Container that replaces the old Row section type.
     * Children are flexed equally (flex=1) and the direction flips row→column at the breakpoint.
     * fillWidth is true by default to maintain parity with the old Row renderer.
     */
    ObjectNode responsiveRow(int gap, int breakpoint) {
        ObjectNode node = container("row", null, null);
        node.put("gap", gap);
        node.put("breakpoint", breakpoint);
        node.put("fillWidth", true);
        return node;
    }

    /** Set flex on an element node (used on children of a Container for proportional sizing). */
    void setFlex(ObjectNode element, double flex) {
        element.put("flex", flex);
    }

    ObjectNode text(String content, String variant, String weight,
                             String color, Integer maxLines) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Text");
        node.put("content", content);
        if (variant != null) node.put("variant", variant);
        if (weight != null) node.put("weight", weight);
        if (color != null) node.put("color", color);
        if (maxLines != null) node.put("maxLines", maxLines);
        return node;
    }

    private static final String DEFAULT_PLACEHOLDER =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    ObjectNode image(String src, int width, int height, String fit) {
        return image(src, width, height, fit, DEFAULT_PLACEHOLDER);
    }

    ObjectNode image(String src, int width, int height, String fit, String placeholder) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Image");
        node.put("src", src);
        if (width > 0) node.put("width", width);
        if (height > 0) node.put("height", height);
        if (fit != null) node.put("fit", fit);
        if (placeholder != null) node.put("placeholder", placeholder);
        return node;
    }

    /**
     * Image preset that emits {@code variant}. Known values: "hero",
     * "thumbnail", "logo". Each platform resolves the string against its
     * ImageVariant enum and supplies native aspect ratio, content mode,
     * corner radius, and clip behaviour.
     */
    ObjectNode variantImage(String variant, String src) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Image");
        node.put("src", src);
        if (variant != null) node.put("variant", variant);
        node.put("placeholder", DEFAULT_PLACEHOLDER);
        return node;
    }

    ObjectNode heroImage(String src) { return variantImage("hero", src); }

    ObjectNode thumbnailImage(String src) { return variantImage("thumbnail", src); }

    ObjectNode logoImage(String src) { return variantImage("logo", src); }

    ObjectNode button(String label, String variant, ObjectNode action) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Button");
        node.put("label", label);
        if (variant != null) node.put("variant", variant);
        node.set("actions", singleActionArray(action));
        return node;
    }

    ObjectNode spacer(int height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    ObjectNode padding(int start, int end, int top, int bottom) {
        ObjectNode p = om.createObjectNode();
        p.put("start", start);
        p.put("end", end);
        p.put("top", top);
        p.put("bottom", bottom);
        return p;
    }

    /**
     * Builds an AtomicElement.cornerRadii block with all four corners set.
     * Used for cards that want asymmetric rounding (e.g. rounded-top +
     * squared-bottom content cards). An explicit 0 on a corner means
     * "square" — it is preserved through the wire (it is not treated as
     * "omitted, fall back to cornerRadius") per the schema contract.
     */
    private ObjectNode cornerRadii(int topStart, int topEnd, int bottomStart, int bottomEnd) {
        ObjectNode r = om.createObjectNode();
        r.put("topStart", topStart);
        r.put("topEnd", topEnd);
        r.put("bottomStart", bottomStart);
        r.put("bottomEnd", bottomEnd);
        return r;
    }

    ObjectNode tapNavigate(String targetUri) {
        ObjectNode action = om.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", targetUri);
        return action;
    }

    /** Navigate action with explicit failure policy and optional feedback. */
    private ObjectNode tapNavigate(String targetUri, String onFailure, ObjectNode failureFeedback) {
        ObjectNode action = tapNavigate(targetUri);
        if (onFailure != null) action.put("onFailure", onFailure);
        if (failureFeedback != null) action.set("failureFeedback", failureFeedback);
        return action;
    }

    /** Apply failure semantics to any action node. */
    private ObjectNode withFailurePolicy(ObjectNode action, String onFailure, ObjectNode failureFeedback) {
        if (onFailure != null) action.put("onFailure", onFailure);
        if (failureFeedback != null) action.set("failureFeedback", failureFeedback);
        return action;
    }

    /** Build a failureFeedback object. */
    private ObjectNode failureFeedback(String message, String style) {
        ObjectNode fb = om.createObjectNode();
        if (message != null) fb.put("message", message);
        if (style != null) fb.put("style", style);
        return fb;
    }

    ArrayNode singleActionArray(ObjectNode action) {
        ArrayNode arr = om.createArrayNode();
        arr.add(action);
        return arr;
    }
}
