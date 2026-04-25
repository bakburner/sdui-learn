package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        root.put("fillWidth", true);
        // Header internal padding: 12pt top (air above title), 4pt bottom.
        // The vertical rhythm between the header title and the next section
        // is produced by the surface-level margin on the *next* section's
        // surface (typically `railSurface.margin.top = 16pt`), not by extra
        // padding inside the header. Keeping bottom tight here avoids the
        // "double-gap" look where header-bottom-padding + next-section-
        // top-margin + next-section-internal-top-padding all stack.
        root.set("padding", padding(16, 16, 12, 4));
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
        // supply `section.surface` on the returned envelope. The row
        // stretches to fill the available width so the promo subject
        // left-aligns and the CTA (if any) sits against the trailing edge.
        ObjectNode root = container("row", null, "center");
        root.put("fillWidth", true);
        ArrayNode rootChildren = om.createArrayNode();

        if (imageUrl != null) {
            ObjectNode img = image(imageUrl, 120, 80, "cover");
            img.put("cornerRadius", 10);
            rootChildren.add(img);
            rootChildren.add(spacer(24));
        }

        ObjectNode contentCol = container("column", null, "start");
        setFlex(contentCol, 1.0);
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
            colChildren.add(text(subhead, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 2));
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
        root.set("padding", padding(0, 0, 0, 12));
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
        // Plain container (no variant): the `elevated` variant's default
        // shadow made the inter-card gap look muddy — each card's shadow
        // bled into the 12pt gap between siblings in the rail. Dropping
        // the variant removes the shadow entirely; the card silhouette
        // is defined by its own gradient background, corner radius, and
        // the scroll container's sibling gap (not by a drop shadow on
        // neighbouring surfaces).
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        // Fix the card's outer width so the image (also 200) and any
        // full-width overlays (duration badge strip) meet the card edge
        // flush. Without this the card sizes to max(child intrinsic
        // widths) — a long 2-line headline would push the card past
        // 200, leaving an empty right gutter next to the image.
        card.put("width", 200);
        card.put("cornerRadius", 12);
        // Subtle vertical gradient so the card silhouette reads against
        // the feed background without relying on a drop shadow.
        ObjectNode cardBg = om.createObjectNode();
        ArrayNode cardBgColors = om.createArrayNode();
        cardBgColors.add(ColorTokens.SURFACE_RAISED);
        cardBgColors.add(ColorTokens.SURFACE_SUNKEN);
        cardBg.set("colors", cardBgColors);
        cardBg.put("direction", "vertical");
        card.set("background", cardBg);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode imageWrap = container("column", null, null);
            imageWrap.put("fillWidth", true);
            // Media should meet the card's top edge; any inset creates a
            // visible strip of card background above the thumbnail.
            imageWrap.set("padding", padding(0, 0, 0, 0));

            ObjectNode img = thumbnailImage(thumbnailUrl);
            img.put("fillWidth", true);
            img.put("aspectRatio", 16.0 / 9.0);
            img.set("cornerRadii", cornerRadii(12, 12, 0, 0));
            if (duration != null) {
                badge(img, durationBadge(duration), "bottomEnd");
            } else if ("video".equalsIgnoreCase(contentType)) {
                badge(img, liveBadge(), "bottomEnd");
            }
            ArrayNode imageWrapChildren = om.createArrayNode();
            imageWrapChildren.add(img);
            imageWrap.set("children", imageWrapChildren);
            children.add(imageWrap);
        }

        children.add(spacer(4));
        ObjectNode headlineEl = text(headline, "bodySmall", "semiBold", ColorTokens.TEXT_PRIMARY, 2);
        // Horizontal padding is 16pt (larger than the 8pt image inset and
        // larger than the card's 12pt cornerRadius) so the first glyph
        // clears the curved corner instead of sitting at the arc where
        // the rounded clip shaves off part of the baseline. Reads as a
        // deliberate text inset rather than a flush baseline with the
        // image's edge.
        headlineEl.set("padding", padding(16, 16, 8, 8));
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
        root.set("padding", padding(0, 0, 0, 12));
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

        ObjectNode root = container("column", null, "stretch");
        root.put("fillWidth", true);
        root.set("padding", padding(0, 0, 0, 12));
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
        grid.put("fillWidth", true);
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
            // Inline hero image treatment: 16:9 artwork that fills card
            // width, rounded top corners, square bottom edge, cover fit.
            // The `hero` ImageVariant was pruned because this surface is
            // inline-expressible.
            ObjectNode img = image(thumbnailUrl, 0, 0, "cover");
            img.put("aspectRatio", 16.0 / 9.0);
            img.put("fillWidth", true);
            img.set("cornerRadii", cornerRadii(12, 12, 0, 0));
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

    // ── GamePanel (as AtomicComposite) ───────────────────────────────────

    /**
     * Per-team input bundle for {@link #buildGamePanelComposite}. The
     * {@code score} is the *initial* value; live updates arrive via the
     * section's {@code dataBinding} writing to
     * {@code content.homeTeam.score} / {@code content.awayTeam.score},
     * which the Text leaves pick up at render time via {@code bindRef}.
     */
    public record GamePanelTeam(String tricode, int score, String logoUrl) {}

    /**
     * Build a {@link GamePanelTeam} from a raw upstream team JSON node
     * (the shape produced by stats-api and the composer mappers).
     * Falls back to {@link SduiUtils#teamLogoUrl(String)} when the
     * caller has not pre-populated {@code logoUrl}.
     */
    public GamePanelTeam gamePanelTeamFromJson(JsonNode team) {
        String tricode = team.path("teamTricode").asText("");
        int score = team.path("score").asInt(0);
        String logoUrl = team.has("logoUrl") && !team.path("logoUrl").isNull()
                ? team.path("logoUrl").asText()
                : SduiUtils.teamLogoUrl(team.path("teamId").asText(""));
        return new GamePanelTeam(tricode, score, logoUrl);
    }

    /**
     * Optional LiveClock snapshot bundle for a running game. When null,
     * the panel renders a static status Text. When non-null, the status
     * slot renders a {@code LiveClock} that resolves its snapshot via
     * {@code bindRef="clock"} — server pushes a single {@code gameClock}
     * object on every SSE tick and all three animation fields update
     * together.
     */
    public record GameClockSnapshot(int snapshotSeconds, String snapshotAtIso, boolean isRunning) {}

    /**
     * Build a GamePanel as an AtomicComposite.
     *
     * <p>The resulting section carries a pre-seeded {@code data.content}
     * dictionary that every dynamic leaf reads via {@code bindRef}:
     * <ul>
     *   <li>{@code content.gameStatusText} — status row copy.</li>
     *   <li>{@code content.homeTeam.score} / {@code content.awayTeam.score} — scores.</li>
     *   <li>{@code content.clock} — {@code {snapshotSeconds, snapshotAt,
     *       isRunning}} tuple consumed by the {@code LiveClock} leaf when
     *       the panel is live and {@code clock} is non-null.</li>
     * </ul>
     * Live games additionally attach {@link SduiUtils#buildCompositeLinescoreBindings}
     * so Ably linescore frames land in the same dictionary.
     *
     * <p>Visual treatment follows the {@code variant} argument — one of
     * {@code standard}, {@code featured}, or {@code scoreboard}. The
     * renderer-owned logic that used to live in {@code GamePanelView}
     * (SwiftUI), {@code GamePanel.kt} (Compose), and {@code GamePanel.tsx}
     * (web) is reduced to Container + Text + Image + (optional) LiveClock,
     * with any variant-specific tuning expressed as inline style
     * properties on those primitives.
     */
    public ObjectNode buildGamePanelComposite(
            String sectionId,
            String analyticsId,
            String variant,
            String gameId,
            int gameStatus,
            String gameStatusText,
            String badgeText,
            GamePanelTeam awayTeam,
            GamePanelTeam homeTeam,
            GameClockSnapshot clock,
            String navigateUri,
            ObjectNode refreshPolicy,
            ObjectNode linescoreBindings,
            ObjectNode surface) {

        boolean featured = "featured".equals(variant);
        int rootPadding = featured ? 20 : 16;
        int cornerRadius = featured ? 16 : 12;

        // Root vertical container with tap-to-navigate action.
        ObjectNode root = container("column", null, "stretch");
        root.put("id", sectionId + "-root");
        root.put("fillWidth", true);
        root.put("cornerRadius", cornerRadius);
        root.set("padding", padding(rootPadding, rootPadding, rootPadding, rootPadding));
        if (navigateUri != null) {
            root.set("actions", singleActionArray(tapNavigate(navigateUri)));
        }

        ArrayNode rootChildren = om.createArrayNode();

        if (badgeText != null && !badgeText.isEmpty()) {
            ObjectNode badge = text(badgeText, "labelSmall", "bold", "#FFFFFF", null);
            badge.put("background", "#E03131");
            badge.put("cornerRadius", 4);
            badge.set("padding", padding(8, 8, 2, 2));
            rootChildren.add(badge);
            rootChildren.add(spacer(8));
        }

        // Teams row: away | status-slot | home, spread edge-to-edge.
        ObjectNode row = container("row", "spaceBetween", "center");
        row.put("fillWidth", true);
        ArrayNode rowChildren = om.createArrayNode();

        rowChildren.add(teamColumn(awayTeam, "awayTeam"));

        // Status slot. Live games with a clock snapshot render the
        // LiveClock primitive and let the client interpolate between
        // snapshots; everything else renders the status text.
        if (gameStatus == 2 && clock != null) {
            rowChildren.add(liveClockCell(featured));
        } else {
            rowChildren.add(statusCell(gameStatusText, featured));
        }

        rowChildren.add(teamColumn(homeTeam, "homeTeam"));
        row.set("children", rowChildren);
        rootChildren.add(row);

        root.set("children", rootChildren);

        // Envelope + data.ui + data.content.
        ObjectNode section = sectionEnvelope(sectionId, analyticsId, refreshPolicy);
        ObjectNode data = wrapUi(root);
        data.set("content", buildGamePanelContent(gameStatusText, gameStatus, homeTeam, awayTeam, clock));
        section.set("data", data);

        if (linescoreBindings != null) section.set("dataBinding", linescoreBindings);
        if (surface != null) section.set("surface", surface);

        return section;
    }

    /**
     * Vertical team column: logo, tricode, score. The {@code score}
     * Text carries a {@code bindRef="<key>.score"} pointing into the
     * section's {@code content} dictionary so Ably linescore writes
     * land on the leaf without walking the tree.
     */
    private ObjectNode teamColumn(GamePanelTeam team, String contentKey) {
        ObjectNode col = container("column", "center", "center");

        ArrayNode children = om.createArrayNode();
        if (team.logoUrl() != null) {
            ObjectNode logo = image(team.logoUrl(), 48, 48, "contain");
            children.add(logo);
            children.add(spacer(4));
        }
        ObjectNode tri = text(team.tricode(), "titleMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 1);
        children.add(tri);

        ObjectNode score = text(String.valueOf(team.score()), "score", "bold", ColorTokens.TEXT_PRIMARY, 1);
        score.put("bindRef", contentKey + ".score");
        children.add(score);

        col.set("children", children);
        return col;
    }

    private ObjectNode statusCell(String gameStatusText, boolean featured) {
        ObjectNode statusText = text(gameStatusText != null ? gameStatusText : "",
                featured ? "titleSmall" : "labelSmall",
                featured ? "semiBold" : "regular",
                ColorTokens.TEXT_PRIMARY, 1);
        statusText.put("opacity", 0.7);
        statusText.put("bindRef", "gameStatusText");
        return statusText;
    }

    /**
     * LiveClock leaf for the status slot. Reads its
     * {@code (snapshotSeconds, snapshotAt, isRunning)} tuple from
     * {@code content.clock} so the server can push a single
     * {@code gameClock} object on each tick and all three animation
     * inputs update together.
     */
    private ObjectNode liveClockCell(boolean featured) {
        ObjectNode clock = om.createObjectNode();
        clock.put("type", "LiveClock");
        clock.put("variant", featured ? "titleLarge" : "titleMedium");
        clock.put("format", "m:ss");
        clock.put("tickDirection", "down");
        clock.put("bindRef", "clock");
        clock.put("snapshotSeconds", 0);
        clock.put("isRunning", false);
        return clock;
    }

    /**
     * Pre-seeded content dictionary the leaves resolve via
     * {@code bindRef}. Seeded so the first paint shows real values
     * before the first Ably frame lands; subsequent writes from
     * {@code buildCompositeLinescoreBindings} replace these entries
     * in place.
     */
    private ObjectNode buildGamePanelContent(String gameStatusText, int gameStatus,
                                             GamePanelTeam homeTeam,
                                             GamePanelTeam awayTeam,
                                             GameClockSnapshot clock) {
        ObjectNode content = om.createObjectNode();
        if (gameStatusText != null) content.put("gameStatusText", gameStatusText);
        content.put("gameStatus", gameStatus);

        ObjectNode home = om.createObjectNode();
        home.put("score", homeTeam.score());
        home.put("tricode", homeTeam.tricode());
        content.set("homeTeam", home);

        ObjectNode away = om.createObjectNode();
        away.put("score", awayTeam.score());
        away.put("tricode", awayTeam.tricode());
        content.set("awayTeam", away);

        if (clock != null) {
            ObjectNode clockNode = om.createObjectNode();
            clockNode.put("snapshotSeconds", clock.snapshotSeconds());
            if (clock.snapshotAtIso() != null) clockNode.put("snapshotAt", clock.snapshotAtIso());
            clockNode.put("isRunning", clock.isRunning());
            content.set("clock", clockNode);
        }

        return content;
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
        // Matches the buildContentRail padding rhythm: 0pt top (surface
        // margin supplies the air), 12pt bottom (so the last-card row
        // doesn't hug the next section).
        root.set("padding", padding(0, 0, 0, 12));
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
        // Plain container (no variant) — see buildContentCard for the
        // rationale. The card's silhouette is defined by its gradient
        // background + corner radius, not a drop shadow.
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        // Fix the card's outer width so the 240pt image + full-width
        // meta row (duration / live badge) meet the card edge flush.
        card.put("width", 240);
        card.put("cornerRadius", 12);
        ObjectNode cardBg = om.createObjectNode();
        ArrayNode cardBgColors = om.createArrayNode();
        cardBgColors.add(ColorTokens.SURFACE_RAISED);
        cardBgColors.add(ColorTokens.SURFACE_SUNKEN);
        cardBg.set("colors", cardBgColors);
        cardBg.put("direction", "vertical");
        card.set("background", cardBg);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode thumbContainer = container("column", null, null);
        ArrayNode thumbChildren = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode img = thumbnailImage(thumbnailUrl);
            // Image inset 8pt on start/end/top so the card face shows as
            // an equal-thickness frame on three sides. Matches the
            // buildContentCard treatment.
            img.set("padding", padding(8, 8, 8, 0));
            img.put("fillWidth", true);
            img.put("aspectRatio", 16.0 / 9.0);
            img.put("cornerRadius", 6);
            thumbChildren.add(img);
        }

        ObjectNode metaContainer = container("row", "spaceBetween", "center");
        // Start/end match the image's 8pt inset so the meta row aligns
        // with the image's left/right edges instead of the card's outer
        // edges.
        metaContainer.set("padding", padding(8, 8, 6, 6));
        ArrayNode metaChildren = om.createArrayNode();

        if (badgeText != null) {
            // Pill badge — Container-wrapped text so the background
            // actually renders (Text's own `background` is not honored
            // by any of the three atomic Text renderers). Red brand
            // pill for NEW/LIVE-style callouts.
            metaChildren.add(pillBadge(badgeText, ColorTokens.BRAND_LIVE));
        } else {
            metaChildren.add(spacer(1));
        }

        if (duration != null) {
            // Dark translucent pill for durations (matches the duration
            // chip overlaid on video card thumbnails elsewhere).
            metaChildren.add(pillBadge(duration, "#000000B3"));
        }

        metaContainer.set("children", metaChildren);
        thumbChildren.add(metaContainer);
        thumbContainer.set("children", thumbChildren);
        children.add(thumbContainer);

        ObjectNode textCol = container("column", null, null);
        // 14pt horizontal > card's 12pt cornerRadius so the title's
        // first glyph clears the rounded corner arc instead of sitting
        // at the clip edge. 10pt vertical keeps the card's top/bottom
        // rhythm unchanged.
        textCol.set("padding", padding(14, 14, 10, 10));
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

        // Root is content-only. The card chrome (sunken background,
        // rounded corners, horizontal+vertical margin from siblings) is
        // expressed on section.surface so every card-chromed composite
        // shares one surface helper. `fillWidth: true` is required for
        // the hero image and slot list to span the card's interior on
        // iOS — without it the VStack sizes to max(child intrinsic).
        ObjectNode root = container("column", null, "start");
        root.put("fillWidth", true);
        root.set("padding", padding(0, 0, 0, 16));
        ArrayNode rootChildren = om.createArrayNode();

        ObjectNode heroContainer = container("column", "end", "start");
        heroContainer.put("fillWidth", true);
        heroContainer.put("cornerRadius", 12);
        ArrayNode heroChildren = om.createArrayNode();

        if (heroImageUrl != null) {
            ObjectNode heroImg = image(heroImageUrl, 0, 200, "cover");
            heroImg.put("fillWidth", true);
            heroImg.set("cornerRadii", cornerRadii(12, 12, 0, 0));
            heroChildren.add(heroImg);
        }

        ObjectNode overlay = container("column", null, "start");
        overlay.put("fillWidth", true);
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
            overlayChildren.add(liveBadge());
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

        rootChildren.add(spacer(12));

        // Heading padding is 16pt horizontal — same inset as the slot
        // list below so the title line aligns with the row cards' outer
        // edge. Any padding the surface provides is in addition to this.
        ObjectNode heading = text("Today's Schedule", "titleSmall", "bold", ColorTokens.TEXT_PRIMARY, null);
        heading.set("padding", padding(16, 16, 4, 4));
        rootChildren.add(heading);

        ObjectNode slotList = container("column", null, "start");
        slotList.put("fillWidth", true);
        slotList.put("gap", 8);
        slotList.set("padding", padding(16, 16, 4, 0));
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

    /**
     * Generic pill badge — Container with a colored rounded-rect
     * background wrapping a small white bold label. Use for any inline
     * chip (LIVE, NEW, durations) when the color is not the LIVE red.
     * For the LIVE badge specifically, call {@link #liveBadge()}.
     *
     * <p>Expressed as a Container wrapping a Text because Text
     * backgrounds are not rendered by any client's atomic Text
     * renderer — Text draws only the foreground glyph on all three
     * platforms. Container backgrounds + corner radii are universal.
     */
    private ObjectNode pillBadge(String label, String backgroundColor) {
        ObjectNode badge = container("row", null, "center");
        badge.put("background", backgroundColor);
        badge.put("cornerRadius", 4);
        badge.set("padding", padding(6, 6, 2, 2));
        ArrayNode children = om.createArrayNode();
        children.add(text(label, "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null));
        badge.set("children", children);
        return badge;
    }

    private ObjectNode buildNbaTvSlot(String id, String title, String subtitle,
                                        String displayTime, boolean isLive,
                                        String targetUri) {
        // Row layout: [time][content fills remaining][badge, optional].
        // `contentCol.fillWidth = true` is the load-bearing bit — without
        // it the HStack distributes leftover space unpredictably (time
        // and title end up far apart, title truncates with an ellipsis
        // even though the row has plenty of room). fillWidth forces the
        // column to claim remaining horizontal space so the time hugs
        // the leading edge and the badge hugs the trailing edge while
        // the title+subtitle expand into the middle.
        ObjectNode row = container("row", null, "center");
        row.put("id", id);
        row.put("fillWidth", true);
        row.put("gap", 12);
        row.put("cornerRadius", 8);
        row.put("background", ColorTokens.SURFACE_CANVAS);
        row.set("padding", padding(12, 12, 12, 12));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode timeText = text(displayTime, "bodyMedium", "semiBold", ColorTokens.TEXT_SECONDARY, null);
        children.add(timeText);

        ObjectNode contentCol = container("column", null, "start");
        contentCol.put("fillWidth", true);
        ArrayNode contentChildren = om.createArrayNode();
        contentChildren.add(text(title, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 1));
        if (subtitle != null) {
            contentChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 1));
        }
        contentCol.set("children", contentChildren);
        children.add(contentCol);

        if (isLive) {
            children.add(liveBadge());
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
     * the rest are {@code variant: "standard"}. The carousel's {@code gap}
     * owns inter-card spacing; wrappers intentionally do not impose a
     * wider fixed slot around hosted sections.
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
            if (game.get("surface") instanceof ObjectNode slotSurface) {
                // The carousel's ScrollContainer owns inter-card spacing.
                // Embedded section margins would add to the 12pt rail gap
                // and make game cards feel much farther apart than content
                // rail cards.
                slotSurface.set("margin", padding(0, 0, 0, 0));
            }

            ObjectNode wrapper = container("column", null, null);
            ArrayNode wrapperChildren = om.createArrayNode();
            wrapperChildren.add(sectionSlot("carousel-slot-" + i, game));
            wrapper.set("children", wrapperChildren);
            kids.add(wrapper);
        }
        scroll.set("children", kids);

        return wrapAsComposite(sectionId, analyticsId, scroll);
    }

    // ── Real-app feed atomic patterns ───────────────────────────────────

    /**
     * Horizontal story rail composed from server-provided story data.
     * items: [id, label, imageUrl, badgeText, targetUri]
     */
    public ObjectNode buildStoryCircleRail(String sectionId, String analyticsId,
                                           String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 0, 12));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(16, 16, 4, 8));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(16, false);
        scroll.set("padding", padding(16, 16, 0, 0));
        ArrayNode children = om.createArrayNode();
        for (String[] item : items) {
            if (!hasRequiredValues(item, 0, 1)) continue;
            children.add(storyCircleItem(value(item, 0), value(item, 1), value(item, 2),
                    value(item, 3), value(item, 4)));
        }
        scroll.set("children", children);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Horizontal rail of tall editorial image cards with server-declared
     * scrim/text overlays.
     * cards: [id, title, subtitle, imageUrl, badgeText, targetUri]
     */
    public ObjectNode buildEditorialOverlayRail(String sectionId, String analyticsId,
                                                String title, String[][] cards) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 0, 12));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(16, 16, 4, 8));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(12, false);
        scroll.set("padding", padding(16, 16, 0, 0));
        ArrayNode children = om.createArrayNode();
        for (String[] card : cards) {
            if (!hasRequiredValues(card, 0, 1)) continue;
            children.add(editorialOverlayCard(value(card, 0), value(card, 1), value(card, 2),
                    value(card, 3), value(card, 4), value(card, 5)));
        }
        scroll.set("children", children);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Paged hero carousel for featured live/upcoming games.
     * cards: [id, badgeText, title, subtitle, keyArtUrl, awayTri, awayScore,
     * awayLogoUrl, homeTri, homeScore, homeLogoUrl, statusText, seriesText,
     * sponsorLogoUrlsCsv, targetUri]
     */
    public ObjectNode buildFeaturedLiveGameHero(String sectionId, String analyticsId,
                                                String title, String[][] cards) {
        return buildFeaturedLiveGameHero(sectionId, analyticsId, title, cards, null, null);
    }

    public ObjectNode buildFeaturedLiveGameHero(String sectionId, String analyticsId,
                                                String title, String[][] cards,
                                                ObjectNode refreshPolicy,
                                                ObjectNode dataBinding) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        List<String[]> validCards = validRows(cards, 0, 2, 5, 8);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 0, 12));
        ArrayNode rootChildren = om.createArrayNode();
        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(16, 16, 4, 8));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(12, false);
        if (validCards.size() > 1) {
            scroll.put("paging", true);
            scroll.put("snapAlignment", "center");
            ObjectNode indicator = om.createObjectNode();
            indicator.put("style", "dots");
            indicator.put("alignment", "bottomCenter");
            indicator.put("color", ColorTokens.TEXT_TERTIARY);
            indicator.put("activeColor", ColorTokens.TEXT_INVERSE);
            scroll.set("pageIndicator", indicator);
        }
        scroll.set("padding", padding(16, 16, 0, 0));

        ArrayNode scrollChildren = om.createArrayNode();
        ObjectNode content = om.createObjectNode();
        ObjectNode cardsContent = om.createObjectNode();
        for (String[] card : validCards) {
            String cardId = value(card, 0);
            scrollChildren.add(featuredLiveGameHeroCard(card));
            ObjectNode state = om.createObjectNode();
            state.put("awayScore", parseInt(value(card, 6), 0));
            state.put("homeScore", parseInt(value(card, 9), 0));
            if (value(card, 11) != null) state.put("statusText", value(card, 11));
            cardsContent.set(cardId, state);
        }
        content.set("cards", cardsContent);
        scroll.set("children", scrollChildren);
        rootChildren.add(scroll);
        root.set("children", rootChildren);

        ObjectNode section = sectionEnvelope(sectionId, analyticsId, refreshPolicy);
        ObjectNode data = wrapUi(root);
        data.set("content", content);
        section.set("data", data);
        if (dataBinding != null) section.set("dataBinding", dataBinding);
        return section;
    }

    public ObjectNode buildRealAppSectionHeader(String sectionId, String analyticsId,
                                                String title, String subtitle,
                                                String actionLabel, String actionUri) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(title, "title");

        ObjectNode root = container("row", "spaceBetween", "center");
        root.put("fillWidth", true);
        root.set("padding", padding(16, 16, 12, 4));

        ArrayNode children = om.createArrayNode();
        ObjectNode titleCol = container("column", null, "start");
        ArrayNode titleChildren = om.createArrayNode();
        titleChildren.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null));
        if (subtitle != null) {
            titleChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_TERTIARY, 1));
        }
        titleCol.set("children", titleChildren);
        children.add(titleCol);

        if (actionUri != null) {
            children.add(button(actionLabel != null ? actionLabel + " >" : "More >",
                    "text", tapNavigate(actionUri)));
        }

        root.set("children", children);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Two-column utility grid.
     * items: [id, label, subtitle, imageUrl, targetUri]
     */
    public ObjectNode buildUtilityCardGrid(String sectionId, String analyticsId,
                                           String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        List<String[]> validItems = validRows(items, 0, 1);

        ObjectNode root = container("column", null, "stretch");
        root.put("gap", 12);
        root.set("padding", padding(16, 16, 0, 16));
        ArrayNode children = om.createArrayNode();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null));
        }

        for (int i = 0; i < validItems.size(); i += 2) {
            ObjectNode row = container("row", null, "stretch");
            row.put("gap", 12);
            row.put("fillWidth", true);
            ArrayNode rowChildren = om.createArrayNode();
            ObjectNode first = utilityCard(validItems.get(i));
            setFlex(first, 1.0);
            rowChildren.add(first);
            if (i + 1 < validItems.size()) {
                ObjectNode second = utilityCard(validItems.get(i + 1));
                setFlex(second, 1.0);
                rowChildren.add(second);
            } else {
                ObjectNode filler = container("column", null, null);
                setFlex(filler, 1.0);
                rowChildren.add(filler);
            }
            row.set("children", rowChildren);
            children.add(row);
        }

        root.set("children", children);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Horizontal rail of league destination cards.
     * items: [id, label, imageUrl, targetUri]
     */
    public ObjectNode buildLeagueCardRail(String sectionId, String analyticsId,
                                          String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 0, 12));
        ArrayNode rootChildren = om.createArrayNode();
        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(16, 16, 4, 8));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(12, false);
        scroll.set("padding", padding(16, 16, 0, 0));
        ArrayNode children = om.createArrayNode();
        for (String[] item : items) {
            if (!hasRequiredValues(item, 0, 1)) continue;
            children.add(leagueCard(value(item, 0), value(item, 1), value(item, 2), value(item, 3)));
        }
        scroll.set("children", children);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Compact schedule row/list item.
     * row: [id, awayTri, awayName, awaySeed, awayScore, awayLogoUrl, homeTri,
     * homeName, homeSeed, homeScore, homeLogoUrl, statusText, seriesText,
     * broadcastLogoUrlsCsv, targetUri, overflowUri]
     */
    public ObjectNode buildGameScheduleRow(String sectionId, String analyticsId, String[] row) {
        return buildGameScheduleRow(sectionId, analyticsId, row, null, null);
    }

    public ObjectNode buildGameScheduleRow(String sectionId, String analyticsId, String[] row,
                                           ObjectNode refreshPolicy,
                                           ObjectNode dataBinding) {
        requireNonBlank(sectionId, "sectionId");
        requireRow(row, "row");
        requireRequiredValues(row, "row", 0, 1, 6);

        ObjectNode card = gameScheduleRowElement(row);
        ObjectNode wrapper = container("column", null, null);
        wrapper.set("padding", padding(16, 16, 4, 4));
        ArrayNode children = om.createArrayNode();
        children.add(card);
        wrapper.set("children", children);

        ObjectNode section = sectionEnvelope(sectionId, analyticsId, refreshPolicy);
        ObjectNode data = wrapUi(wrapper);
        ObjectNode content = om.createObjectNode();
        ObjectNode state = om.createObjectNode();
        state.put("awayScore", parseInt(value(row, 4), 0));
        state.put("homeScore", parseInt(value(row, 9), 0));
        if (value(row, 11) != null) state.put("statusText", value(row, 11));
        content.set(value(row, 0), state);
        data.set("content", content);
        section.set("data", data);
        if (dataBinding != null) section.set("dataBinding", dataBinding);
        return section;
    }

    public ObjectNode buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows) {
        return buildGameScheduleList(sectionId, analyticsId, title, rows, null, null);
    }

    public ObjectNode buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows,
                                            ObjectNode refreshPolicy,
                                            ObjectNode dataBinding) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(rows, "rows");

        List<String[]> validRows = validRows(rows, 0, 1, 6);

        ObjectNode root = container("column", null, "stretch");
        root.put("gap", 8);
        root.set("padding", padding(16, 16, 0, 12));
        ArrayNode children = om.createArrayNode();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null));
        }
        ObjectNode content = om.createObjectNode();
        for (String[] row : validRows) {
            String rowId = value(row, 0);
            children.add(gameScheduleRowElement(row));
            ObjectNode state = om.createObjectNode();
            state.put("awayScore", parseInt(value(row, 4), 0));
            state.put("homeScore", parseInt(value(row, 9), 0));
            if (value(row, 11) != null) state.put("statusText", value(row, 11));
            content.set(rowId, state);
        }
        root.set("children", children);

        ObjectNode section = sectionEnvelope(sectionId, analyticsId, refreshPolicy);
        ObjectNode data = wrapUi(root);
        data.set("content", content);
        section.set("data", data);
        if (dataBinding != null) section.set("dataBinding", dataBinding);
        return section;
    }

    private ObjectNode storyCircleItem(String id, String label, String imageUrl,
                                       String badgeText, String targetUri) {
        ObjectNode item = container("column", "center", "center");
        item.put("id", id);
        item.put("width", 76);
        if (targetUri != null) item.set("actions", singleActionArray(tapNavigate(targetUri)));
        ArrayNode children = om.createArrayNode();

        ObjectNode avatar = imageUrl != null
                ? image(imageUrl, 64, 64, "cover", null)
                : neutralInitials(label, 64, 32);
        avatar.put("cornerRadius", 32);
        if (badgeText != null) {
            avatar = overlayContainer(avatar, List.of(overlay("bottomCenter",
                    padding(0, 0, 0, 0), pillBadge(badgeText, ColorTokens.BRAND_LIVE))));
        }
        children.add(avatar);
        children.add(spacer(6));
        children.add(text(label, "labelSmall", null, ColorTokens.TEXT_PRIMARY, 1));
        item.set("children", children);
        return item;
    }

    private ObjectNode editorialOverlayCard(String id, String title, String subtitle,
                                            String imageUrl, String badgeText, String targetUri) {
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        card.put("width", 184);
        card.put("cornerRadius", 16);
        if (targetUri != null) card.set("actions", singleActionArray(tapNavigate(targetUri)));

        ObjectNode base = imageUrl != null
                ? image(imageUrl, 184, 0, "cover", null)
                : neutralInitialsRect(title, 184, 245, 16);
        base.put("fillWidth", true);
        base.put("aspectRatio", 3.0 / 4.0);
        base.put("cornerRadius", 16);

        ObjectNode scrimContent = container("column", null, "start");
        scrimContent.put("fillWidth", true);
        scrimContent.set("padding", padding(12, 12, 64, 12));
        ObjectNode gradient = gradient("#00000000", "#000000D9", "vertical");
        scrimContent.set("background", gradient);
        ArrayNode children = om.createArrayNode();
        if (badgeText != null) {
            children.add(pillBadge(badgeText, ColorTokens.BRAND_LIVE));
            children.add(spacer(6));
        }
        children.add(text(title, "titleSmall", "bold", ColorTokens.TEXT_INVERSE, 2));
        if (subtitle != null) {
            children.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_INVERSE, 2));
        }
        scrimContent.set("children", children);

        ArrayNode cardChildren = om.createArrayNode();
        cardChildren.add(overlayContainer(base, List.of(overlay("bottomStart", null, scrimContent))));
        card.set("children", cardChildren);
        return card;
    }

    private ObjectNode featuredLiveGameHeroCard(String[] card) {
        String cardId = value(card, 0);
        ObjectNode hero = container("column", null, "stretch");
        hero.put("id", cardId);
        hero.put("width", 320);
        hero.put("cornerRadius", 18);
        hero.put("background", ColorTokens.SURFACE_RAISED);
        if (value(card, 14) != null) hero.set("actions", singleActionArray(tapNavigate(value(card, 14))));

        ObjectNode art = value(card, 4) != null
                ? image(value(card, 4), 320, 0, "cover", null)
                : neutralInitialsRect(value(card, 2), 320, 180, 18);
        art.put("fillWidth", true);
        art.put("aspectRatio", 16.0 / 9.0);
        art.set("cornerRadii", cornerRadii(18, 18, 0, 0));

        ObjectNode titleOverlay = container("column", null, "start");
        titleOverlay.put("fillWidth", true);
        titleOverlay.set("padding", padding(16, 16, 48, 14));
        titleOverlay.set("background", gradient("#00000000", "#000000D9", "vertical"));
        ArrayNode overlayChildren = om.createArrayNode();
        if (value(card, 1) != null) {
            overlayChildren.add(pillBadge(value(card, 1), ColorTokens.BRAND_LIVE));
            overlayChildren.add(spacer(6));
        }
        overlayChildren.add(text(value(card, 2), "titleMedium", "bold", ColorTokens.TEXT_INVERSE, 2));
        if (value(card, 3) != null) {
            overlayChildren.add(text(value(card, 3), "bodySmall", null, ColorTokens.TEXT_INVERSE, 2));
        }
        titleOverlay.set("children", overlayChildren);

        ArrayNode heroChildren = om.createArrayNode();
        heroChildren.add(overlayContainer(art, List.of(overlay("bottomStart", null, titleOverlay))));
        heroChildren.add(heroScoreStrip(card));
        ArrayNode sponsorLogos = logoRow(value(card, 13), 48, 20);
        if (sponsorLogos.size() > 0) {
            ObjectNode sponsors = container("row", "end", "center");
            sponsors.put("gap", 8);
            sponsors.set("padding", padding(16, 16, 0, 14));
            sponsors.set("children", sponsorLogos);
            heroChildren.add(sponsors);
        }
        hero.set("children", heroChildren);
        return hero;
    }

    private ObjectNode heroScoreStrip(String[] card) {
        ObjectNode row = container("row", "spaceBetween", "center");
        row.put("fillWidth", true);
        row.set("padding", padding(16, 16, 14, 10));
        ArrayNode children = om.createArrayNode();
        children.add(heroTeam(value(card, 5), value(card, 6), value(card, 7), "cards." + value(card, 0) + ".awayScore"));

        ObjectNode center = container("column", "center", "center");
        ArrayNode centerChildren = om.createArrayNode();
        ObjectNode status = text(value(card, 11), "labelSmall", "semiBold", ColorTokens.TEXT_SECONDARY, 1);
        status.put("bindRef", "cards." + value(card, 0) + ".statusText");
        centerChildren.add(status);
        if (value(card, 12) != null) {
            centerChildren.add(text(value(card, 12), "labelSmall", null, ColorTokens.TEXT_TERTIARY, 1));
        }
        center.set("children", centerChildren);
        children.add(center);

        children.add(heroTeam(value(card, 8), value(card, 9), value(card, 10), "cards." + value(card, 0) + ".homeScore"));
        row.set("children", children);
        return row;
    }

    private ObjectNode heroTeam(String tri, String score, String logoUrl, String bindRef) {
        ObjectNode col = container("column", "center", "center");
        col.put("width", 92);
        ArrayNode children = om.createArrayNode();
        if (logoUrl != null) children.add(image(logoUrl, 44, 44, "contain", null));
        children.add(text(tri, "labelSmall", "bold", ColorTokens.TEXT_PRIMARY, 1));
        if (score != null) {
            ObjectNode scoreText = text(score, "titleLarge", "bold", ColorTokens.TEXT_PRIMARY, 1);
            scoreText.put("bindRef", bindRef);
            scoreText.put("monospacedDigits", true);
            children.add(scoreText);
        }
        col.set("children", children);
        return col;
    }

    private ObjectNode utilityCard(String[] item) {
        ObjectNode card = container("column", "center", "center");
        card.put("id", value(item, 0));
        card.put("gap", 8);
        card.put("background", ColorTokens.SURFACE_RAISED);
        card.put("cornerRadius", 14);
        card.set("padding", padding(12, 12, 16, 16));
        if (value(item, 4) != null) card.set("actions", singleActionArray(tapNavigate(value(item, 4))));

        ArrayNode children = om.createArrayNode();
        if (value(item, 3) != null) {
            children.add(image(value(item, 3), 44, 44, "contain", null));
        } else {
            children.add(neutralInitials(value(item, 1), 44, 22));
        }
        children.add(text(value(item, 1), "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 2));
        if (value(item, 2) != null) {
            children.add(text(value(item, 2), "labelSmall", null, ColorTokens.TEXT_SECONDARY, 2));
        }
        card.set("children", children);
        return card;
    }

    private ObjectNode leagueCard(String id, String label, String imageUrl, String targetUri) {
        ObjectNode card = container("column", "center", "center");
        card.put("id", id);
        card.put("width", 148);
        card.put("gap", 10);
        card.put("background", ColorTokens.SURFACE_RAISED);
        card.put("cornerRadius", 14);
        card.set("padding", padding(14, 14, 16, 16));
        if (targetUri != null) card.set("actions", singleActionArray(tapNavigate(targetUri)));

        ArrayNode children = om.createArrayNode();
        if (imageUrl != null) {
            children.add(image(imageUrl, 64, 48, "contain", null));
        } else {
            children.add(neutralInitials(label, 64, 24));
        }
        children.add(text(label, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 2));
        card.set("children", children);
        return card;
    }

    private ObjectNode gameScheduleRowElement(String[] row) {
        ObjectNode card = container("column", null, "stretch");
        card.put("id", value(row, 0));
        card.put("gap", 10);
        card.put("background", ColorTokens.SURFACE_RAISED);
        card.put("cornerRadius", 14);
        card.set("padding", padding(14, 14, 12, 12));
        if (value(row, 14) != null) card.set("actions", singleActionArray(tapNavigate(value(row, 14))));

        ArrayNode children = om.createArrayNode();
        ObjectNode matchup = container("row", "spaceBetween", "center");
        matchup.put("fillWidth", true);
        ArrayNode matchupChildren = om.createArrayNode();
        matchupChildren.add(scheduleTeam(value(row, 1), value(row, 2), value(row, 3),
                value(row, 4), value(row, 5), value(row, 0), "awayScore"));
        matchupChildren.add(scheduleStatus(value(row, 11), value(row, 12), value(row, 0)));
        matchupChildren.add(scheduleTeam(value(row, 6), value(row, 7), value(row, 8),
                value(row, 9), value(row, 10), value(row, 0), "homeScore"));
        matchup.set("children", matchupChildren);
        children.add(matchup);

        ArrayNode logos = logoRow(value(row, 13), 44, 18);
        if (logos.size() > 0 || value(row, 15) != null) {
            ObjectNode meta = container("row", "spaceBetween", "center");
            meta.put("fillWidth", true);
            ArrayNode metaChildren = om.createArrayNode();
            ObjectNode logoWrap = container("row", null, "center");
            logoWrap.put("gap", 8);
            logoWrap.set("children", logos);
            metaChildren.add(logoWrap);
            if (value(row, 15) != null) {
                metaChildren.add(button("More", "text", tapNavigate(value(row, 15))));
            }
            meta.set("children", metaChildren);
            children.add(meta);
        }

        card.set("children", children);
        return card;
    }

    private ObjectNode scheduleTeam(String tri, String name, String seed, String score,
                                    String logoUrl, String rowId, String scoreKey) {
        ObjectNode team = container("row", null, "center");
        team.put("gap", 8);
        team.put("width", 112);
        ArrayNode children = om.createArrayNode();
        if (logoUrl != null) children.add(image(logoUrl, 36, 36, "contain", null));
        ObjectNode labelCol = container("column", null, "start");
        ArrayNode labelChildren = om.createArrayNode();
        String seedPrefix = seed != null ? seed + " " : "";
        labelChildren.add(text(seedPrefix + tri, "bodyMedium", "bold", ColorTokens.TEXT_PRIMARY, 1));
        if (name != null) labelChildren.add(text(name, "labelSmall", null, ColorTokens.TEXT_SECONDARY, 1));
        labelCol.set("children", labelChildren);
        children.add(labelCol);
        if (score != null) {
            ObjectNode scoreText = text(score, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, 1);
            scoreText.put("bindRef", rowId + "." + scoreKey);
            scoreText.put("monospacedDigits", true);
            children.add(scoreText);
        }
        team.set("children", children);
        return team;
    }

    private ObjectNode scheduleStatus(String status, String seriesText, String rowId) {
        ObjectNode center = container("column", "center", "center");
        center.put("width", 76);
        ArrayNode children = om.createArrayNode();
        ObjectNode statusText = text(status != null ? status : "", "labelSmall", "semiBold",
                ColorTokens.TEXT_SECONDARY, 2);
        statusText.put("bindRef", rowId + ".statusText");
        statusText.put("textAlign", "center");
        children.add(statusText);
        if (seriesText != null) {
            ObjectNode series = text(seriesText, "labelSmall", null, ColorTokens.TEXT_TERTIARY, 2);
            series.put("textAlign", "center");
            children.add(series);
        }
        center.set("children", children);
        return center;
    }

    private ObjectNode scrollRow(int gap, boolean showIndicators) {
        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", gap);
        scroll.put("showIndicators", showIndicators);
        return scroll;
    }

    private ObjectNode overlayContainer(ObjectNode base, List<ObjectNode> overlays) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "OverlayContainer");
        node.set("base", base);
        ArrayNode arr = om.createArrayNode();
        for (ObjectNode overlay : overlays) arr.add(overlay);
        node.set("overlays", arr);
        return node;
    }

    private ObjectNode overlay(String alignment, ObjectNode inset, ObjectNode element) {
        ObjectNode overlay = om.createObjectNode();
        if (alignment != null) overlay.put("alignment", alignment);
        if (inset != null) overlay.set("inset", inset);
        overlay.set("element", element);
        return overlay;
    }

    private ObjectNode gradient(String first, String second, String direction) {
        ObjectNode gradient = om.createObjectNode();
        ArrayNode colors = om.createArrayNode();
        colors.add(first);
        colors.add(second);
        gradient.set("colors", colors);
        gradient.put("direction", direction);
        return gradient;
    }

    private ObjectNode neutralInitials(String label, int width, int radius) {
        return neutralInitialsRect(label, width, width, radius);
    }

    private ObjectNode neutralInitialsRect(String label, int width, int height, int radius) {
        ObjectNode box = container("row", "center", "center");
        box.put("width", width);
        box.put("height", height);
        box.put("cornerRadius", radius);
        box.put("background", ColorTokens.SURFACE_SUNKEN);
        ArrayNode children = om.createArrayNode();
        children.add(text(initials(label), "labelSmall", "bold", ColorTokens.TEXT_SECONDARY, 1));
        box.set("children", children);
        return box;
    }

    private ArrayNode logoRow(String csv, int width, int height) {
        ArrayNode logos = om.createArrayNode();
        if (csv == null || csv.isBlank()) return logos;
        for (String raw : csv.split(",")) {
            String url = raw.trim();
            if (!url.isEmpty()) logos.add(image(url, width, height, "contain", null));
        }
        return logos;
    }

    private static String value(String[] row, int index) {
        if (row == null || index >= row.length) return null;
        String value = row[index];
        return value == null || value.isBlank() ? null : value;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null) return fallback;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static void requireRow(String[] row, String fieldName) {
        if (row == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static void requireRows(String[][] rows, String fieldName) {
        if (rows == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static void requireRequiredValues(String[] row, String fieldName, int... indexes) {
        for (int index : indexes) {
            if (value(row, index) == null) {
                throw new IllegalArgumentException(fieldName + "[" + index + "] must not be blank");
            }
        }
    }

    private static boolean hasRequiredValues(String[] row, int... indexes) {
        if (row == null) return false;
        for (int index : indexes) {
            if (value(row, index) == null) return false;
        }
        return true;
    }

    private static List<String[]> validRows(String[][] rows, int... requiredIndexes) {
        List<String[]> valid = new ArrayList<>();
        for (String[] row : rows) {
            if (hasRequiredValues(row, requiredIndexes)) valid.add(row);
        }
        return valid;
    }

    private static String initials(String label) {
        if (label == null || label.isBlank()) return "";
        String[] parts = label.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(3, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
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

    // Allowed values for Container's direction / alignment / crossAlignment
    // fields, mirroring the Direction / Alignment / CrossAlignment enums in
    // schema/sdui-schema.json. Kept in lock-step with the schema — when an
    // enum value is added or removed there, update the set here too.
    // Clients strict-decode these fields, so a composer that
    // emits a value outside the enum crashes the client at decode time;
    // validating here surfaces the contract violation at the server-build
    // site instead of at the client.
    private static final Set<String> VALID_DIRECTIONS = Set.of("row", "column");
    private static final Set<String> VALID_ALIGNMENTS =
        Set.of("start", "center", "end", "spaceBetween", "spaceAround", "spaceEvenly");
    private static final Set<String> VALID_CROSS_ALIGNMENTS =
        Set.of("start", "center", "end", "stretch");

    ObjectNode container(String direction, String alignment, String crossAlignment) {
        validateEnum("direction", direction, VALID_DIRECTIONS);
        validateEnum("alignment", alignment, VALID_ALIGNMENTS);
        validateEnum("crossAlignment", crossAlignment, VALID_CROSS_ALIGNMENTS);

        ObjectNode node = om.createObjectNode();
        node.put("type", "Container");
        if (direction != null) node.put("direction", direction);
        if (alignment != null) node.put("alignment", alignment);
        if (crossAlignment != null) node.put("crossAlignment", crossAlignment);
        return node;
    }

    private static void validateEnum(String field, String value, Set<String> allowed) {
        if (value == null) return;
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                "Container." + field + " value '" + value
                    + "' is not in the schema enum " + allowed
                    + ". Fix the composer call site; clients strict-decode this field."
            );
        }
    }

    /**
     * Container preset that emits {@code variant}. Each platform resolves the
     * string against its ContainerVariant enum and supplies a native realization
     * (material, tonal elevation, gradient, shadow). Known values: "hero",
     * "grouped". Inline style properties may still be set on the returned
     * node and win over the variant default for axes the variant's override
     * matrix marks as {@code allow}.
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

    ObjectNode groupedContainer(String direction, String alignment, String crossAlignment) {
        return variantContainer("grouped", direction, alignment, crossAlignment);
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
     * Image preset that emits {@code variant}. Known value: "thumbnail".
     * The client resolves the string against its ImageVariant enum and
     * supplies native content mode, corner radius, and clip behaviour.
     */
    ObjectNode variantImage(String variant, String src) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Image");
        node.put("src", src);
        if (variant != null) node.put("variant", variant);
        node.put("placeholder", DEFAULT_PLACEHOLDER);
        return node;
    }

    ObjectNode thumbnailImage(String src) { return variantImage("thumbnail", src); }

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
