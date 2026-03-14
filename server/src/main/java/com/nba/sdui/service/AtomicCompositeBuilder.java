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
 * Tier 1: ErrorState, SectionHeader, PromoBanner, ContentRail, FollowingRail.
 * Tier 2: HeroPanel, VideoCarousel, StatLine, NbaTvSchedule, GamePanel (scoreboard variant).
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
            children.add(text(message, "bodyMedium", null, "#888888", null));
        }

        if (retryUri != null) {
            children.add(spacer(16));
            children.add(button("Try Again", "filled", tapNavigate(retryUri)));
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
            titleChildren.add(text(subtitle, "bodySmall", null, "#7a8baa", null));
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
                                        String bgColor, String ctaLabel,
                                        String targetUri) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        String backgroundColor = bgColor != null ? bgColor : "#2A2A5E";

        ObjectNode root = container("row", null, "center");
        root.put("backgroundColor", backgroundColor);
        root.put("cornerRadius", 12);
        root.set("padding", padding(20, 20, 20, 20));
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
            colChildren.add(text(title.toUpperCase(), "labelSmall", "bold", "#FF6B6B", null));
            colChildren.add(spacer(4));
        }
        if (headline != null) {
            colChildren.add(text(headline, "titleMedium", "bold", "#FFFFFF", null));
            colChildren.add(spacer(4));
        }
        if (subhead != null) {
            colChildren.add(text(subhead, "bodySmall", null, "#CCCCCC", null));
        }
        if (targetUri != null) {
            colChildren.add(spacer(12));
            String label = ctaLabel != null ? ctaLabel : "Learn More";
            colChildren.add(button(label, "filled", tapNavigate(targetUri)));
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
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode thumbContainer = container("column", null, null);
        thumbContainer.put("cornerRadius", 8);
        thumbContainer.put("backgroundColor", "#2A2A4A");
        ArrayNode thumbChildren = om.createArrayNode();

        if (thumbnailUrl != null) {
            thumbChildren.add(image(thumbnailUrl, 200, 112, "cover"));
        }

        ObjectNode metaContainer = container("column", null, null);
        metaContainer.set("padding", padding(4, 4, 4, 4));
        ArrayNode metaChildren = om.createArrayNode();

        if (contentType != null) {
            ObjectNode badge = text(contentType.toUpperCase(), "labelSmall", "bold", "#FFFFFF", null);
            badge.put("backgroundColor", "#FF6B6B");
            metaChildren.add(badge);
        }

        if (duration != null) {
            metaChildren.add(text(duration, "labelSmall", "semiBold", "#FFFFFF", null));
        } else {
            metaChildren.add(spacer(16));
        }

        metaContainer.set("children", metaChildren);
        thumbChildren.add(metaContainer);

        thumbContainer.set("children", thumbChildren);
        children.add(thumbContainer);

        children.add(spacer(8));
        ObjectNode headlineEl = text(headline, "bodySmall", "semiBold", "#FFFFFF", 2);
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
                    "labelSmall", "bold", "#7a8baa", null);
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
                                        String headerVariant, String cellVariant,
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
        if (headerVariant != null) grid.put("headerVariant", headerVariant);
        if (cellVariant != null) grid.put("cellVariant", cellVariant);
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
    // TIER 2 SECTIONS
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

        ObjectNode card = container("column", null, null);
        card.put("cornerRadius", 12);
        card.put("backgroundColor", "#1A1F2E");
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode imgContainer = container("column", null, null);
            ArrayNode imgChildren = om.createArrayNode();
            ObjectNode img = image(thumbnailUrl, 280, 140, "cover");
            imgChildren.add(img);
            if (duration != null) {
                ObjectNode dur = text(duration, "labelSmall", null, "#FFFFFF", null);
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
            ObjectNode badge = text(contentType.toUpperCase(), "labelSmall", "bold", "#FF6B6B", null);
            ObjectNode badgePad = padding(0, 0, 0, 4);
            badge.set("padding", badgePad);
            textChildren.add(badge);
        }

        textChildren.add(text(headline, "titleSmall", "bold", "#FFFFFF", 2));

        if (subhead != null) {
            ObjectNode sub = text(subhead, "bodySmall", null, "#AAAAAA", 2);
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
            ObjectNode titleEl = text(title, "titleMedium", "bold", "#FFFFFF", null);
            titleEl.set("padding", padding(16, 16, 4, 4));
            rootChildren.add(titleEl);
        }
        if (subtitle != null) {
            ObjectNode subEl = text(subtitle, "bodySmall", null, "#AAAAAA", null);
            subEl.set("padding", padding(16, 16, 2, 2));
            rootChildren.add(subEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", 12);
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
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        card.put("cornerRadius", 12);
        card.put("backgroundColor", "#1A1F2E");
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode thumbContainer = container("column", null, null);
        ArrayNode thumbChildren = om.createArrayNode();

        if (thumbnailUrl != null) {
            thumbChildren.add(image(thumbnailUrl, 240, 135, "cover"));
        }

        ObjectNode metaContainer = container("row", "spaceBetween", "center");
        metaContainer.set("padding", padding(6, 6, 6, 6));
        ArrayNode metaChildren = om.createArrayNode();

        if (badgeText != null) {
            ObjectNode badge = text(badgeText, "labelSmall", "bold", "#FFFFFF", null);
            badge.put("backgroundColor", "#C8102E");
            metaChildren.add(badge);
        } else {
            metaChildren.add(spacer(1));
        }

        if (duration != null) {
            ObjectNode dur = text(duration, "labelSmall", null, "#FFFFFF", null);
            dur.put("backgroundColor", "#000000B3");
            metaChildren.add(dur);
        }

        metaContainer.set("children", metaChildren);
        thumbChildren.add(metaContainer);
        thumbContainer.set("children", thumbChildren);
        children.add(thumbContainer);

        ObjectNode textCol = container("column", null, null);
        textCol.set("padding", padding(10, 10, 10, 10));
        ArrayNode textChildren = om.createArrayNode();
        textChildren.add(text(title, "bodyMedium", "semiBold", "#FFFFFF", 2));
        if (subtitle != null) {
            ObjectNode sub = text(subtitle, "bodySmall", null, "#999999", 1);
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
            nameChildren.add(text(teamTricode, "bodySmall", null, "#999999", null));
        }
        nameCol.set("children", nameChildren);
        children.add(nameCol);

        children.add(spacer(8));
        children.add(text(statCategory, "bodyMedium", null, "#AAAAAA", null));
        children.add(spacer(8));
        children.add(text(statValue, "titleMedium", "bold", "#FF6B6B", null));

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
            nameChildren.add(text(teamTricode, "bodySmall", null, "#999999", null));
        }
        nameCol.set("children", nameChildren);
        topChildren.add(nameCol);
        topRow.set("children", topChildren);
        children.add(topRow);

        ObjectNode bottomRow = container("row", "end", "center");
        bottomRow.set("padding", padding(0, 0, 4, 0));
        ArrayNode bottomChildren = om.createArrayNode();
        bottomChildren.add(text(statCategory, "bodyMedium", null, "#AAAAAA", null));
        bottomChildren.add(spacer(8));
        bottomChildren.add(text(statValue, "titleMedium", "bold", "#FF6B6B", null));
        bottomRow.set("children", bottomChildren);
        children.add(bottomRow);

        col.set("children", children);
        return col;
    }

    // ── GamePanel (scoreboard variant) ───────────────────────────────────────────────

    /**
     * Build a GamePanel (scoreboard variant) as an AtomicComposite.
     * Away team | Status | Home team layout.
     */
    public ObjectNode buildScoreboardHeader(String id, String analyticsId,
                                             String awayTricode, String awayName,
                                             String awayLogoUrl, String awayScore,
                                             String homeTricode, String homeName,
                                             String homeLogoUrl, String homeScore,
                                             String statusText, String periodLabel,
                                             String bgColor, String targetUri) {
        ObjectNode section = sectionEnvelope(id, analyticsId);

        String bg = bgColor != null ? bgColor : "#17408B";

        ObjectNode root = container("row", "spaceEvenly", "center");
        root.put("cornerRadius", 12);
        root.put("backgroundColor", bg);
        root.set("padding", padding(16, 16, 24, 24));
        if (targetUri != null) {
            root.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode rootChildren = om.createArrayNode();

        rootChildren.add(buildTeamColumn(awayTricode, awayLogoUrl, awayScore));

        ObjectNode statusCol = container("column", "center", "center");
        ArrayNode statusChildren = om.createArrayNode();
        statusChildren.add(text(statusText, "bodyMedium", "medium", "#FFFFFF", null));
        if (periodLabel != null) {
            statusChildren.add(text(periodLabel, "bodySmall", null, "#CCCCCC", null));
        }
        statusCol.set("children", statusChildren);
        rootChildren.add(statusCol);

        rootChildren.add(buildTeamColumn(homeTricode, homeLogoUrl, homeScore));

        root.set("children", rootChildren);

        ObjectNode wrapper = container("column", null, null);
        wrapper.set("padding", padding(8, 8, 0, 0));
        ArrayNode wrapChildren = om.createArrayNode();
        wrapChildren.add(root);
        wrapper.set("children", wrapChildren);
        wrapUi(section, wrapper);
        return section;
    }

    private ObjectNode buildTeamColumn(String tricode, String logoUrl, String score) {
        ObjectNode col = container("column", "center", "center");
        ArrayNode children = om.createArrayNode();

        String logo = logoUrl != null ? logoUrl : DEFAULT_PLACEHOLDER;
        ObjectNode img = image(logo, 60, 60, "contain");
        children.add(img);
        children.add(spacer(4));
        children.add(text(tricode, "bodyMedium", "bold", "#FFFFFF", null));
        children.add(text(score, "headlineLarge", "bold", "#FFFFFF", null));

        col.set("children", children);
        return col;
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
        overlay.put("backgroundGradient", 0);
        ObjectNode grad = om.createObjectNode();
        ArrayNode gradColors = om.createArrayNode();
        gradColors.add("#00000000");
        gradColors.add("#000000CC");
        grad.set("colors", gradColors);
        grad.put("direction", "vertical");
        overlay.set("backgroundGradient", grad);
        ArrayNode overlayChildren = om.createArrayNode();

        if (liveNow) {
            ObjectNode liveBadge = text("LIVE", "labelSmall", "bold", "#FFFFFF", null);
            liveBadge.put("backgroundColor", "#C8102E");
            overlayChildren.add(liveBadge);
            overlayChildren.add(spacer(6));
        }
        if (heroTitle != null) {
            overlayChildren.add(text(heroTitle, "titleLarge", "bold", "#FFFFFF", null));
        }
        if (heroSubtitle != null) {
            overlayChildren.add(text(heroSubtitle, "bodyMedium", null, "#CCCCCC", null));
        }
        overlay.set("children", overlayChildren);
        heroChildren.add(overlay);
        heroContainer.set("children", heroChildren);
        rootChildren.add(heroContainer);

        rootChildren.add(spacer(8));

        ObjectNode heading = text("Today's Schedule", "titleSmall", "bold", "#FFFFFF", null);
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
        row.put("backgroundColor", "#1A1F2E");
        row.set("padding", padding(12, 12, 12, 12));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode timeText = text(displayTime, "bodyMedium", "semiBold", "#999999", null);
        children.add(timeText);
        children.add(spacer(12));

        ObjectNode contentCol = container("column", null, null);
        ArrayNode contentChildren = om.createArrayNode();
        contentChildren.add(text(title, "bodyMedium", "semiBold", "#FFFFFF", 1));
        if (subtitle != null) {
            contentChildren.add(text(subtitle, "bodySmall", null, "#999999", 1));
        }
        contentCol.set("children", contentChildren);
        children.add(contentCol);

        if (isLive) {
            children.add(spacer(8));
            ObjectNode badge = text("LIVE", "labelSmall", "bold", "#FFFFFF", null);
            badge.put("backgroundColor", "#C8102E");
            children.add(badge);
        }

        row.set("children", children);
        return row;
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

    private void wrapUi(ObjectNode section, ObjectNode rootElement) {
        ObjectNode data = om.createObjectNode();
        data.set("ui", rootElement);
        section.set("data", data);
    }

    private ObjectNode container(String direction, String alignment, String crossAlignment) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Container");
        if (direction != null) node.put("direction", direction);
        if (alignment != null) node.put("alignment", alignment);
        if (crossAlignment != null) node.put("crossAlignment", crossAlignment);
        return node;
    }

    private ObjectNode text(String content, String variant, String weight,
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

    private ObjectNode image(String src, int width, int height, String fit) {
        return image(src, width, height, fit, DEFAULT_PLACEHOLDER);
    }

    private ObjectNode image(String src, int width, int height, String fit, String placeholder) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Image");
        node.put("src", src);
        if (width > 0) node.put("width", width);
        if (height > 0) node.put("height", height);
        if (fit != null) node.put("fit", fit);
        if (placeholder != null) node.put("placeholder", placeholder);
        return node;
    }

    private ObjectNode button(String label, String buttonVariant, ObjectNode action) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Button");
        node.put("label", label);
        if (buttonVariant != null) node.put("buttonVariant", buttonVariant);
        node.set("actions", singleActionArray(action));
        return node;
    }

    private ObjectNode spacer(int height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    private ObjectNode padding(int start, int end, int top, int bottom) {
        ObjectNode p = om.createObjectNode();
        p.put("start", start);
        p.put("end", end);
        p.put("top", top);
        p.put("bottom", bottom);
        return p;
    }

    private ObjectNode tapNavigate(String targetUri) {
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

    private ArrayNode singleActionArray(ObjectNode action) {
        ArrayNode arr = om.createArrayNode();
        arr.add(action);
        return arr;
    }
}
