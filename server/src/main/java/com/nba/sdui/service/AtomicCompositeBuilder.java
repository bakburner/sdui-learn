package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XL, LayoutTokens.SPACING_XL));
        ArrayNode children = om.createArrayNode();

        children.add(text(emoji, "titleLarge", null, null, null));
        children.add(spacer(LayoutTokens.SPACING_MD));
        children.add(text(title, "titleMedium", "bold", null, null));

        if (message != null && !message.isBlank()) {
            children.add(spacer(LayoutTokens.SPACING_MD));
            children.add(text(message, "bodyMedium", null, ColorTokens.TEXT_SECONDARY, null));
        }

        if (retryUri != null) {
            children.add(spacer(LayoutTokens.SPACING_LG));
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
        root.put("widthMode", "fill");
        // Header internal padding: nba.spacing.md top (air above title),
        // nba.spacing.xs bottom. The vertical rhythm between the header
        // title and the next section is produced by the surface-level
        // margin on the *next* section's surface (typically
        // `railSurface.margin.top = nba.spacing.lg`), not by extra padding
        // inside the header. Keeping bottom tight here avoids the
        // "double-gap" look where header-bottom-padding + next-section-
        // top-margin + next-section-internal-top-padding all stack.
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_XS));
        ArrayNode children = om.createArrayNode();

        if (subtitle != null) {
            ObjectNode titleCol = container("column", null, null);
            ArrayNode titleChildren = om.createArrayNode();
            ObjectNode titleText = text(title, "titleMedium", "bold", null, null);
            AccessibilityHelper.addHeading(om, titleText, title, 2);
            titleChildren.add(titleText);
            titleChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_TERTIARY, null));
            titleCol.set("children", titleChildren);
            children.add(titleCol);
        } else {
            ObjectNode titleText = text(title, "titleMedium", "bold", null, null);
            AccessibilityHelper.addHeading(om, titleText, title, 2);
            children.add(titleText);
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
        root.put("widthMode", "fill");
        ArrayNode rootChildren = om.createArrayNode();

        if (imageUrl != null) {
            ObjectNode img = image(imageUrl, 120, 80, "cover");
            img.put("cornerRadius", LayoutTokens.RADIUS_MD);
            AccessibilityHelper.addImage(om, img, headline != null ? headline : "Promo image");
            rootChildren.add(img);
            rootChildren.add(spacer(LayoutTokens.SPACING_LG));
        }

        ObjectNode contentCol = container("column", null, "start");
        setFlex(contentCol, 1.0);
        ArrayNode colChildren = om.createArrayNode();

        if (title != null) {
            colChildren.add(text(title.toUpperCase(), "labelSmall", "bold", ColorTokens.BRAND_LIVE, null));
            colChildren.add(spacer(LayoutTokens.SPACING_SM));
        }
        if (headline != null) {
            colChildren.add(text(headline, "titleMedium", "bold", ColorTokens.TEXT_INVERSE, null));
            colChildren.add(spacer(LayoutTokens.SPACING_SM));
        }
        if (subhead != null) {
            colChildren.add(text(subhead, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 2));
        }
        if (targetUri != null) {
            colChildren.add(spacer(LayoutTokens.SPACING_MD));
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
        root.put("widthMode", "fill");
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
            rootChildren.add(titleEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", LayoutTokens.SPACING_MD);
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
        card.put("cornerRadius", 0);
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
            AccessibilityHelper.addButton(om, card, headline);
        }
        ArrayNode children = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode imageWrap = container("column", null, null);
            imageWrap.put("widthMode", "fill");
            // Media should meet the card's top edge; any inset creates a
            // visible strip of card background above the thumbnail.
            imageWrap.set("padding", padding(0, 0, 0, 0));

            ObjectNode img = thumbnailImage(thumbnailUrl);
            img.put("widthMode", "fill");
            img.put("aspectRatio", 16.0 / 9.0);
            AccessibilityHelper.addImage(om, img, headline);
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

        children.add(spacer(LayoutTokens.SPACING_SM));
        ObjectNode headlineEl = text(headline, "bodySmall", "semiBold", ColorTokens.TEXT_PRIMARY, 2);
        headlineEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_MD));
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
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM));
            rootChildren.add(titleEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", LayoutTokens.SPACING_LG);
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
            img.put("cornerRadius", LayoutTokens.RADIUS_LG);
            AccessibilityHelper.addImage(om, img, name + " avatar");
            children.add(img);
        } else {
            ObjectNode fallback = text(name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase(),
                    "labelSmall", "bold", ColorTokens.TEXT_TERTIARY, null);
            children.add(fallback);
        }

        children.add(spacer(LayoutTokens.SPACING_SM));
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
        root.put("widthMode", "fill");
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
            rootChildren.add(titleEl);
        }

        ObjectNode grid = om.createObjectNode();
        grid.put("type", "DisplayGrid");
        grid.put("id", id + "-grid");
        grid.put("widthMode", "fill");
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
            AccessibilityHelper.addButton(om, card, headline);
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
            img.put("widthMode", "fill");
            img.set("cornerRadii", cornerRadii(LayoutTokens.RADIUS_LG, LayoutTokens.RADIUS_LG, 0, 0));
            AccessibilityHelper.addImage(om, img, headline);
            imgChildren.add(img);
            if (duration != null) {
                ObjectNode dur = text(duration, "labelSmall", null, ColorTokens.TEXT_INVERSE, null);
                dur.set("padding", padding(LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, 0, 0));
                imgChildren.add(dur);
            }
            imgContainer.set("children", imgChildren);
            children.add(imgContainer);
        }

        ObjectNode textCol = container("column", null, null);
        textCol.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
        ArrayNode textChildren = om.createArrayNode();

        if (contentType != null) {
            ObjectNode badge = text(contentType.toUpperCase(), "labelSmall", "bold", ColorTokens.BRAND_LIVE, null);
            ObjectNode badgePad = padding(0, 0, 0, LayoutTokens.SPACING_XS);
            badge.set("padding", badgePad);
            textChildren.add(badge);
        }

        textChildren.add(text(headline, "titleSmall", "bold", ColorTokens.TEXT_PRIMARY, 2));

        if (subhead != null) {
            ObjectNode sub = text(subhead, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 2);
            sub.set("padding", padding(0, 0, LayoutTokens.SPACING_XS, 0));
            textChildren.add(sub);
        }

        textCol.set("children", textChildren);
        children.add(textCol);
        card.set("children", children);

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM));
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
     * Initial {@code isRunning} flag emitted on fresh composer responses.
     * Always {@code false}: a server-composed snapshot is paused at the
     * upstream {@code gameClock} value (or 0 if upstream is missing) with
     * {@code snapshotAt = now}. Local interpolation only starts when an
     * Ably linescore frame writes a fresh snapshot whose {@code clockRunning}
     * resolves to {@code true} — without an Ably update, the clock stays
     * frozen at the seeded value rather than running away from a stale
     * snapshot. Kept as a named constant so all four composer call sites
     * (Live/GameDetail/Scoreboard/DemoScreen) trivially pass the same
     * value.
     */
    public static final boolean INITIAL_CLOCK_RUNNING = false;

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
        return buildGamePanelComposite(sectionId, analyticsId, variant, gameId, gameStatus,
                gameStatusText, badgeText, null, awayTeam, homeTeam, clock,
                navigateUri, refreshPolicy, linescoreBindings, surface);
    }

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
            String seriesText,
            GamePanelTeam awayTeam,
            GamePanelTeam homeTeam,
            GameClockSnapshot clock,
            String navigateUri,
            ObjectNode refreshPolicy,
            ObjectNode linescoreBindings,
            ObjectNode surface) {
        boolean featured = "featured".equals(variant);
        // Featured uses 20px padding — no exact token exists (§3.6 exception: no design-system token).
        Object rootPadding = featured ? 20 : LayoutTokens.SPACING_LG;
        String cornerRadiusToken = featured ? LayoutTokens.RADIUS_LG : LayoutTokens.RADIUS_MD;

        // Root vertical container with tap-to-navigate action.
        ObjectNode root = container("column", null, "stretch");
        root.put("id", sectionId + "-root");
        root.put("widthMode", "fill");
        root.put("cornerRadius", cornerRadiusToken);
        root.set("padding", padding(rootPadding, rootPadding, rootPadding, rootPadding));
        if (navigateUri != null) {
            root.set("actions", singleActionArray(tapNavigate(navigateUri)));
            String a11yLabel = awayTeam.tricode() + " vs " + homeTeam.tricode();
            if (gameStatusText != null) a11yLabel += ", " + gameStatusText;
            AccessibilityHelper.addButton(om, root, a11yLabel);
        }

        ArrayNode rootChildren = om.createArrayNode();

        if (badgeText != null && !badgeText.isEmpty()) {
            ObjectNode badge = text(badgeText, "labelSmall", "bold", "#FFFFFF", null);
            badge.put("background", "#E03131");
            badge.put("cornerRadius", LayoutTokens.RADIUS_SM);
            badge.set("padding", padding(LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
            rootChildren.add(badge);
            rootChildren.add(spacer(LayoutTokens.SPACING_MD));
        }

        // Optional series/context row above the matchup (e.g. "BOS leads series 3-2").
        if (seriesText != null && !seriesText.isEmpty()) {
            ObjectNode seriesEl = text(seriesText, "labelSmall", null, ColorTokens.TEXT_TERTIARY, 1);
            seriesEl.put("textAlign", "center");
            rootChildren.add(seriesEl);
            rootChildren.add(spacer(LayoutTokens.SPACING_SM));
        }

        // Teams row: away | status-slot | home, spread edge-to-edge.
        ObjectNode row = container("row", "spaceBetween", "center");
        row.put("widthMode", "fill");
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
            AccessibilityHelper.addImage(om, logo, team.tricode() + " logo");
            children.add(logo);
            children.add(spacer(LayoutTokens.SPACING_SM));
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
        root.put("widthMode", "fill");
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            titleEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
            rootChildren.add(titleEl);
        }
        if (subtitle != null) {
            ObjectNode subEl = text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null);
            subEl.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
            rootChildren.add(subEl);
        }

        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", LayoutTokens.SPACING_MD);
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
        card.put("cornerRadius", 0);
        ObjectNode cardBg = om.createObjectNode();
        ArrayNode cardBgColors = om.createArrayNode();
        cardBgColors.add(ColorTokens.SURFACE_RAISED);
        cardBgColors.add(ColorTokens.SURFACE_SUNKEN);
        cardBg.set("colors", cardBgColors);
        cardBg.put("direction", "vertical");
        card.set("background", cardBg);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, card, title);
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode thumbContainer = container("column", null, null);
        ArrayNode thumbChildren = om.createArrayNode();

        if (thumbnailUrl != null) {
            ObjectNode img = thumbnailImage(thumbnailUrl);
            img.set("padding", padding(LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, 0));
            img.put("widthMode", "fill");
            img.put("aspectRatio", 16.0 / 9.0);
            AccessibilityHelper.addImage(om, img, title);
            thumbChildren.add(img);
        }

        ObjectNode metaContainer = container("row", "spaceBetween", "center");
        // Start/end match the image's 8pt inset so the meta row aligns
        // with the image's left/right edges instead of the card's outer
        // edges.
        metaContainer.set("padding", padding(LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
        ArrayNode metaChildren = om.createArrayNode();

        if (badgeText != null) {
            // Pill badge — Container-wrapped text so the background
            // actually renders (Text's own `background` is not honored
            // by any of the three atomic Text renderers). Red brand
            // pill for NEW/LIVE-style callouts.
            metaChildren.add(pillBadge(badgeText, ColorTokens.BRAND_LIVE));
        } else {
            metaChildren.add(spacer(LayoutTokens.SPACING_XS));
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
        textCol.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM));
        ArrayNode textChildren = om.createArrayNode();
        textChildren.add(text(title, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 2));
        if (subtitle != null) {
            ObjectNode sub = text(subtitle, "bodySmall", null, ColorTokens.TEXT_SECONDARY, 1);
            sub.set("padding", padding(0, 0, LayoutTokens.SPACING_XS, 0));
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
        root.put("widthMode", "fill");
        root.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM));
        root.put("gap", LayoutTokens.SPACING_XS);
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_SM));
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
        return buildCompactStatRow(playerId, playerName, teamTricode, statCategory, statValue, playerImageUrl);
    }

    private ObjectNode buildStatRowVertical(String playerId, String playerName,
                                              String teamTricode, String statCategory,
                                              String statValue, String playerImageUrl) {
        return buildCompactStatRow(playerId, playerName, teamTricode, statCategory, statValue, playerImageUrl);
    }

    /**
     * Single compact row: [headshot] [name / team] [STAT VALUE]
     * Name+team stack vertically in a flex column; stat is right-aligned.
     */
    private ObjectNode buildCompactStatRow(String playerId, String playerName,
                                            String teamTricode, String statCategory,
                                            String statValue, String playerImageUrl) {
        ObjectNode row = container("row", null, "center");
        row.put("widthMode", "fill");
        row.put("gap", LayoutTokens.SPACING_SM);
        ArrayNode children = om.createArrayNode();

        if (playerImageUrl != null) {
            ObjectNode img = image(playerImageUrl, 32, 32, "cover");
            img.put("cornerRadius", LayoutTokens.RADIUS_FULL);
            AccessibilityHelper.addImage(om, img, playerName + " headshot");
            children.add(img);
        }

        ObjectNode nameCol = container("column", null, null);
        nameCol.put("flex", 1);
        ArrayNode nameChildren = om.createArrayNode();
        nameChildren.add(text(playerName, "bodyMedium", "medium", null, null));
        if (teamTricode != null) {
            nameChildren.add(text(teamTricode, "labelSmall", null, ColorTokens.TEXT_SECONDARY, null));
        }
        nameCol.set("children", nameChildren);
        children.add(nameCol);

        ObjectNode statGroup = container("row", null, "center");
        statGroup.put("gap", LayoutTokens.SPACING_XS);
        ArrayNode statChildren = om.createArrayNode();
        statChildren.add(text(statCategory, "bodySmall", null, ColorTokens.TEXT_SECONDARY, null));
        statChildren.add(text(statValue, "titleSmall", "bold", ColorTokens.BRAND_LIVE, null));
        statGroup.set("children", statChildren);
        children.add(statGroup);

        row.set("children", children);
        return row;
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
        // shares one surface helper. `widthMode: "fill"` is required for
        // the hero image and slot list to span the card's interior on
        // iOS — without it the VStack sizes to max(child intrinsic).
        ObjectNode root = container("column", null, "start");
        root.put("widthMode", "fill");
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_LG));
        ArrayNode rootChildren = om.createArrayNode();

        ObjectNode heroContainer = container("column", "end", "start");
        heroContainer.put("widthMode", "fill");
        heroContainer.put("cornerRadius", 0);
        ArrayNode heroChildren = om.createArrayNode();

        if (heroImageUrl != null) {
            ObjectNode heroImg = image(heroImageUrl, 0, 200, "cover");
            heroImg.put("widthMode", "fill");
            AccessibilityHelper.addImage(om, heroImg, heroTitle != null ? heroTitle : "NBA TV");
            heroChildren.add(heroImg);
        }

        ObjectNode overlay = container("column", null, "start");
        overlay.put("widthMode", "fill");
        overlay.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG));
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
            overlayChildren.add(spacer(LayoutTokens.SPACING_SM));
        }
        if (heroTitle != null) {
            overlayChildren.add(text(heroTitle, "titleLarge", "bold", ColorTokens.TEXT_ON_DARK_MEDIA, null));
        }
        if (heroSubtitle != null) {
            overlayChildren.add(text(heroSubtitle, "bodyMedium", null, ColorTokens.TEXT_ON_DARK_MEDIA, null));
        }
        overlay.set("children", overlayChildren);
        heroChildren.add(overlay);
        heroContainer.set("children", heroChildren);
        rootChildren.add(heroContainer);

        rootChildren.add(spacer(LayoutTokens.SPACING_MD));

        // Heading padding is 16pt horizontal — same inset as the slot
        // list below so the title line aligns with the row cards' outer
        // edge. Any padding the surface provides is in addition to this.
        ObjectNode heading = text("Today's Schedule", "titleSmall", "bold", ColorTokens.TEXT_PRIMARY, null);
        heading.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
        AccessibilityHelper.addHeading(om, heading, "Today's Schedule", 3);
        rootChildren.add(heading);

        ObjectNode slotList = container("column", null, "start");
        slotList.put("widthMode", "fill");
        slotList.put("gap", LayoutTokens.SPACING_SM);
        slotList.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, 0));
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
        badge.put("cornerRadius", LayoutTokens.RADIUS_SM);
        badge.set("padding", padding(LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
        ArrayNode children = om.createArrayNode();
        children.add(text(label, "labelSmall", "bold", ColorTokens.TEXT_INVERSE, null));
        badge.set("children", children);
        return badge;
    }

    private ObjectNode buildNbaTvSlot(String id, String title, String subtitle,
                                        String displayTime, boolean isLive,
                                        String targetUri) {
        // Row layout: [time][content fills remaining][badge, optional].
        //
        // `contentCol.flex = 1` is the load-bearing bit. `widthMode: "fill"`
        // alone is not sufficient on iOS / Android: the AtomicContainer's
        // flex stack only treats a child as "flexible" (i.e. claims
        // leftover main-axis space) when it has a non-null `flex` weight.
        // Without flex weight, the HStack/Row sizes to its natural width
        // and the outer widthMode:fill frame centers the whole stack, so time +
        // title + subtitle render visually centered inside the surface and
        // the LIVE badge floats next to the title instead of pinning to
        // the trailing edge. With `flex: 1`, the content column claims the
        // remaining horizontal space so the time hugs the leading edge,
        // the badge hugs the trailing edge, and the title + subtitle
        // expand into the middle and remain left-justified.
        ObjectNode row = container("row", null, "center");
        row.put("id", id);
        row.put("widthMode", "fill");
        row.put("gap", LayoutTokens.SPACING_MD);
        row.put("cornerRadius", LayoutTokens.RADIUS_MD);
        row.put("background", ColorTokens.SURFACE_CANVAS);
        row.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
        }
        ArrayNode children = om.createArrayNode();

        ObjectNode timeText = text(displayTime, "bodyMedium", "semiBold", ColorTokens.TEXT_SECONDARY, null);
        children.add(timeText);

        ObjectNode contentCol = container("column", null, "start");
        contentCol.put("widthMode", "fill");
        contentCol.put("flex", 1.0);
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

    /** Attach a drop shadow to any element node (singular, deprecated). */
    public ObjectNode shadow(ObjectNode element, String color, double radius, double offsetX, double offsetY) {
        ObjectNode s = om.createObjectNode();
        if (color != null) s.put("color", color);
        s.put("radius", radius);
        s.put("offsetX", offsetX);
        s.put("offsetY", offsetY);
        element.set("shadow", s);
        return element;
    }

    /** Attach a shadow with default dark color (singular, deprecated). */
    public ObjectNode shadow(ObjectNode element) {
        return shadow(element, "#00000014", 4, 0, 2);
    }

    // ── Multi-background / multi-shadow helpers (Workstream B) ──────────

    /**
     * Set the {@code backgrounds} array on an element. Each layer can be:
     * <ul>
     *   <li>A {@link String} — solid color (hex or token reference)</li>
     *   <li>An {@link ObjectNode} — gradient or image background object</li>
     * </ul>
     * Index 0 is the bottommost layer (Figma convention); higher indices
     * paint on top. Clients that receive both {@code background} and
     * {@code backgrounds} prefer the array form.
     */
    public ObjectNode backgrounds(ObjectNode element, List<Object> layers) {
        ArrayNode arr = om.createArrayNode();
        for (Object layer : layers) {
            if (layer instanceof String s) {
                arr.add(s);
            } else if (layer instanceof ObjectNode obj) {
                arr.add(obj);
            } else {
                throw new IllegalArgumentException(
                    "backgrounds layer must be String (color) or ObjectNode (gradient/image), got: "
                        + (layer == null ? "null" : layer.getClass().getName()));
            }
        }
        element.set("backgrounds", arr);
        return element;
    }

    /**
     * Set the {@code shadows} array on an element. Index 0 is the outermost
     * shadow (Figma convention); higher indices are closer to the element.
     * Clients that receive both {@code shadow} and {@code shadows} prefer
     * the array form.
     */
    public ObjectNode shadows(ObjectNode element, List<ObjectNode> shadowList) {
        ArrayNode arr = om.createArrayNode();
        for (ObjectNode s : shadowList) {
            arr.add(s);
        }
        element.set("shadows", arr);
        return element;
    }

    /**
     * Create a Shadow ObjectNode with an explicit {@code type} field.
     *
     * @param type    "drop" (outer) or "inner" (inset)
     * @param color   shadow color (hex with alpha, or token reference)
     * @param offsetX horizontal offset in dp/px
     * @param offsetY vertical offset in dp/px
     * @param radius  blur radius in dp/px
     */
    public ObjectNode shadowWithType(String type, String color, int offsetX, int offsetY, int radius) {
        ObjectNode s = om.createObjectNode();
        if (type != null) s.put("type", type);
        if (color != null) s.put("color", color);
        s.put("offsetX", offsetX);
        s.put("offsetY", offsetY);
        s.put("radius", radius);
        return s;
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
        bg.put("cornerRadius", LayoutTokens.RADIUS_SM);
        bg.put("background", "#000000B3");
        // iOS ref app uses 0.7 for duration pill opacity; normalised here so
        // Android + web match without each platform inventing its own value.
        opacity(bg, 0.7);
        bg.set("padding", padding(LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
        ArrayNode children = om.createArrayNode();
        children.add(text(duration, "labelSmall", "semiBold", ColorTokens.TEXT_INVERSE, null));
        bg.set("children", children);
        return bg;
    }

    /** Build a "LIVE" badge element (red pill with white text). */
    public ObjectNode liveBadge() {
        ObjectNode bg = container("row", "center", "center");
        bg.put("cornerRadius", LayoutTokens.RADIUS_SM);
        bg.put("background", ColorTokens.BRAND_LIVE);
        bg.set("padding", padding(LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
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
        surface.put("cornerRadius", LayoutTokens.RADIUS_LG);
        surface.set("padding", padding(0, 0, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));

        ArrayNode surfaceChildren = om.createArrayNode();
        if (header != null) {
            ObjectNode headerEl = text(header, "titleSmall", "semiBold", null, null);
            ObjectNode headerPad = padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_SM);
            headerEl.set("padding", headerPad);
            AccessibilityHelper.addHeading(om, headerEl, header, 3);
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
        wrapper.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
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
        row.put("gap", LayoutTokens.SPACING_MD);
        row.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM));
        if (targetUri != null) {
            row.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, row, title);
        }

        ArrayNode children = om.createArrayNode();
        if (thumbnailUrl != null) {
            ObjectNode thumb = thumbnailImage(thumbnailUrl);
            thumb.put("width", 80);
            thumb.put("height", 52);
            thumb.put("cornerRadius", LayoutTokens.RADIUS_SM);
            AccessibilityHelper.addImage(om, thumb, title);
            if (isLive) {
                badge(thumb, liveBadge(), "topStart");
            } else if (duration != null) {
                badge(thumb, durationBadge(duration), "bottomEnd");
            }
            children.add(thumb);
        }

        ObjectNode titleCol = container("column", null, "start");
        titleCol.put("gap", LayoutTokens.SPACING_XS);
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
        scroll.put("gap", LayoutTokens.SPACING_MD);
        scroll.put("showIndicators", false);
        scroll.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));

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
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_SM));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(14, false);
        scroll.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
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

    // ── Cinematic Hero Carousel ────────────────────────────────────────

    /**
     * Full-bleed paged hero carousel (NBA.com-style). Each slide is an edge-to-edge
     * background image with a bottom dark-gradient scrim overlaying headline, subtitle,
     * optional badge, and optional CTA button. No rounded corners — slides fill the
     * full viewport width.
     *
     * <p>slides: [id, imageUrl, badge, title, subtitle, ctaLabel, targetUri]
     *
     * <p>Unlike {@link #buildHeroPanel} (which produces a rounded card with text below),
     * this produces a cinematic full-bleed treatment with text overlaid on the image.
     */
    public ObjectNode buildCinematicHeroCarousel(String sectionId, String analyticsId,
                                                  String[][] slides) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(slides, "slides");

        ObjectNode root = container("column", null, null);
        root.put("widthMode", "fill");
        ArrayNode rootChildren = om.createArrayNode();

        boolean multiSlide = slides.length > 1;
        ArrayNode scrollChildren = om.createArrayNode();

        for (String[] slide : slides) {
            if (!hasRequiredValues(slide, 0, 1, 3)) continue;
            scrollChildren.add(cinematicHeroSlide(
                    value(slide, 0), value(slide, 1), value(slide, 2),
                    value(slide, 3), value(slide, 4), value(slide, 5),
                    value(slide, 6)));
        }

        if (multiSlide) {
            ObjectNode scroll = scrollRow(0, false);
            scroll.put("widthMode", "fill");
            scroll.put("paging", true);
            scroll.put("snapAlignment", "center");
            ObjectNode indicator = om.createObjectNode();
            indicator.put("style", "dashes");
            indicator.put("alignment", "bottomCenter");
            indicator.put("color", "#FFFFFF66");
            indicator.put("activeColor", "#FFFFFFFF");
            scroll.set("pageIndicator", indicator);
            scroll.set("children", scrollChildren);
            rootChildren.add(scroll);
        } else if (!scrollChildren.isEmpty()) {
            rootChildren.add(scrollChildren.get(0));
        }

        root.set("children", rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    private ObjectNode cinematicHeroSlide(String id, String imageUrl, String badgeText,
                                           String title, String subtitle,
                                           String ctaLabel, String targetUri) {
        // Outer wrapper — no corner radius (full-bleed)
        ObjectNode slide = container("column", null, "stretch");
        slide.put("id", id);
        slide.put("widthMode", "fill");

        if (targetUri != null) {
            slide.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, slide, title);
        }

        // Background image fills full width, 16:9 aspect
        ObjectNode bgImage = image(imageUrl, 0, 0, "cover", null);
        bgImage.put("widthMode", "fill");
        bgImage.put("aspectRatio", 16.0 / 9.0);
        AccessibilityHelper.addImage(om, bgImage, title);

        // Scrim overlay fills the full image height so the gradient scales
        // proportionally on any viewport. Content pushes to the bottom via
        // alignment="end". No fixed top padding — the gradient itself provides
        // the visual fade from transparent at top to dark at bottom.
        ObjectNode scrimOverlay = container("column", "end", "start");
        scrimOverlay.put("widthMode", "fill");
        scrimOverlay.put("heightMode", "fill");
        scrimOverlay.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, LayoutTokens.SPACING_XL));
        scrimOverlay.set("background", gradient("#00000000", "#000000E6", "vertical"));
        ArrayNode scrimChildren = om.createArrayNode();

        if (badgeText != null) {
            scrimChildren.add(pillBadge(badgeText, ColorTokens.BRAND_LIVE));
            scrimChildren.add(spacer(LayoutTokens.SPACING_MD));
        }
        scrimChildren.add(text(title, "headlineSmall", "bold", ColorTokens.TEXT_ON_DARK_MEDIA, 2));
        if (subtitle != null) {
            scrimChildren.add(spacer(LayoutTokens.SPACING_SM));
            scrimChildren.add(text(subtitle, "bodyMedium", null, ColorTokens.TEXT_ON_DARK_MEDIA, 2));
        }
        if (ctaLabel != null) {
            scrimChildren.add(spacer(LayoutTokens.SPACING_MD));
            ObjectNode cta = button(ctaLabel, "secondary",
                    targetUri != null ? tapNavigate(targetUri) : null);
            cta.put("color", ColorTokens.TEXT_ON_DARK_MEDIA);
            cta.put("background", "#00000000");
            scrimChildren.add(cta);
        }
        scrimOverlay.set("children", scrimChildren);

        // Compose using OverlayContainer: image base + bottom scrim overlay
        ArrayNode slideChildren = om.createArrayNode();
        slideChildren.add(overlayContainer(bgImage, List.of(overlay("bottomStart", null, scrimOverlay))));
        slide.set("children", slideChildren);
        return slide;
    }

    // ── Overlay Story Rail ──────────────────────────────────────────────

    /**
     * Horizontal scrolling rail of tall portrait story cards (NBA.com-style).
     * Each card is a 3:4 aspect image with text overlaid at the bottom via a dark
     * gradient scrim. An optional "NEW" badge pill appears at the top-left corner.
     *
     * <p>cards: [id, title, imageUrl, badgeText, targetUri]
     *
     * <p>Unlike {@link #buildContentRail} (which uses square thumbnails with text below),
     * this produces tall portrait cards with all text overlaid on the image.
     */
    public ObjectNode buildOverlayStoryRail(String sectionId, String analyticsId,
                                             String title, String[][] cards) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        ObjectNode root = container("column", null, null);
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_SM));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(14, false);
        scroll.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
        ArrayNode children = om.createArrayNode();
        for (String[] card : cards) {
            if (!hasRequiredValues(card, 0, 1)) continue;
            children.add(overlayStoryCard(value(card, 0), value(card, 1),
                    value(card, 2), value(card, 3), value(card, 4)));
        }
        scroll.set("children", children);
        rootChildren.add(scroll);
        root.set("children", rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    private ObjectNode overlayStoryCard(String id, String title, String imageUrl,
                                         String badgeText, String targetUri) {
        String radius = LayoutTokens.RADIUS_MD;
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        card.put("width", 180);
        card.put("cornerRadius", radius);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, card, title);
        }

        // Full-bleed portrait image as base (3:4 aspect ratio)
        ObjectNode base = imageUrl != null
                ? image(imageUrl, 180, 0, "cover", null)
                : neutralInitialsRect(title, 180, 240, radius);
        base.put("widthMode", "fill");
        base.put("aspectRatio", 3.0 / 4.0);
        base.put("cornerRadius", radius);
        if (imageUrl != null) AccessibilityHelper.addImage(om, base, title);

        // Bottom scrim overlay with title text — dark black gradient for readability
        ObjectNode scrimContent = container("column", "end", "start");
        scrimContent.put("widthMode", "fill");
        scrimContent.put("heightMode", "fill");
        scrimContent.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, 0, LayoutTokens.SPACING_MD));
        scrimContent.set("background", gradient("#00000000", "#000000CC", "vertical"));
        scrimContent.set("cornerRadii", cornerRadii(0, 0, radius, radius));
        ArrayNode scrimChildren = om.createArrayNode();
        scrimChildren.add(text(title, "titleSmall", "bold", ColorTokens.TEXT_INVERSE, 3));
        scrimContent.set("children", scrimChildren);

        // Top-left "NEW" badge overlay (if present)
        List<ObjectNode> overlays = new ArrayList<>();
        overlays.add(overlay("bottomStart", null, scrimContent));
        if (badgeText != null) {
            ObjectNode badgePill = pillBadge(badgeText, ColorTokens.BRAND_LIVE);
            ObjectNode badgeInset = padding(LayoutTokens.SPACING_SM, 0, LayoutTokens.SPACING_SM, 0);
            overlays.add(overlay("topStart", badgeInset, badgePill));
        }

        ArrayNode cardChildren = om.createArrayNode();
        cardChildren.add(overlayContainer(base, overlays));
        card.set("children", cardChildren);
        return card;
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
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();

        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_SM));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(14, false);
        scroll.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
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
     * sponsorLogoUrlsCsv, targetUri, heroOverflowUri]
     * <p>
     * Broadcaster/sponsor logo URLs in {@code sponsorLogoUrlsCsv} must be
     * server-provided (comma-separated). {@code targetUri} is the card tap
     * target; {@code heroOverflowUri} is optional top-right overflow affordance
     * — both must come from composer inputs, never from client-side
     * derivation from game or team identity.
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
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();
        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_SM));
            rootChildren.add(header);
        }

        boolean singleCard = validCards.size() == 1;
        ArrayNode scrollChildren = om.createArrayNode();
        ObjectNode content = om.createObjectNode();
        ObjectNode cardsContent = om.createObjectNode();
        for (String[] card : validCards) {
            String cardId = value(card, 0);
            scrollChildren.add(featuredLiveGameHeroCard(card, singleCard));
            ObjectNode state = om.createObjectNode();
            state.put("awayScore", parseInt(value(card, 6), 0));
            state.put("homeScore", parseInt(value(card, 9), 0));
            if (value(card, 11) != null) state.put("statusText", value(card, 11));
            cardsContent.set(cardId, state);
        }
        content.set("cards", cardsContent);

        // Single-card hero: emit a flush container so the card fills its
        // surface end-to-end. Multi-card hero: paged horizontal scroll
        // with peeking edges and a dot indicator.
        if (singleCard) {
            ObjectNode wrapper = container("column", null, "stretch");
            wrapper.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
            ArrayNode wrapperChildren = om.createArrayNode();
            for (int i = 0; i < scrollChildren.size(); i++) wrapperChildren.add(scrollChildren.get(i));
            wrapper.set("children", wrapperChildren);
            rootChildren.add(wrapper);
        } else {
            ObjectNode scroll = pagedHorizontalScroll(LayoutTokens.SPACING_MD, validCards.size(), padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0),
                    "bottomCenter", null, null);
            scroll.set("children", scrollChildren);
            rootChildren.add(scroll);
        }
        root.set("children", rootChildren);

        ObjectNode section = sectionEnvelope(sectionId, analyticsId, refreshPolicy);
        ObjectNode data = wrapUi(root);
        data.set("content", content);
        section.set("data", data);
        if (dataBinding != null) section.set("dataBinding", dataBinding);
        return section;
    }

    public ObjectNode buildSectionHeaderComposite(String sectionId, String analyticsId,
                                                  String title, String subtitle,
                                                  String actionLabel, String actionUri) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(title, "title");

        ObjectNode root = container("row", "spaceBetween", "center");
        root.put("widthMode", "fill");
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_XS));

        ArrayNode children = om.createArrayNode();
        ObjectNode titleCol = container("column", null, "start");
        ArrayNode titleChildren = om.createArrayNode();
        ObjectNode titleEl = text(title.toUpperCase(Locale.ROOT), "titleMedium", "bold",
                ColorTokens.TEXT_PRIMARY, null);
        AccessibilityHelper.addHeading(om, titleEl, title, 2);
        titleChildren.add(titleEl);
        if (subtitle != null) {
            titleChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_TERTIARY, 1));
        }
        titleCol.set("children", titleChildren);
        children.add(titleCol);

        if (actionUri != null) {
            ObjectNode more = button(actionLabel != null ? actionLabel + " >" : "More >",
                    "text", tapNavigate(actionUri));
            more.put("color", ColorTokens.BRAND_NBA);
            children.add(more);
        }

        root.set("children", children);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Screen-level app bar as an {@code AtomicComposite} section (first row in
     * {@code sections}). Spacing uses layout tokens so clients resolve height via
     * {@code LayoutTokenResolver}. Omit on bottom-nav tab roots — the selected tab
     * label is sufficient.
     *
     * @param title   optional headline (e.g. game detail); pass null to omit
     * @param backUri optional {@code nba://} target for the back affordance
     */
    public ObjectNode buildAppBarHeaderComposite(String sectionId, String analyticsId,
                                                 String title, String backUri) {
        requireNonBlank(sectionId, "sectionId");

        ObjectNode root = container("row", "start", "center");
        root.put("widthMode", "fill");
        root.set("padding", padding(
                LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD,
                LayoutTokens.SPACING_SM, LayoutTokens.SPACING_XS));

        ArrayNode children = om.createArrayNode();
        if (backUri != null && !backUri.isBlank()) {
            ObjectNode back = appBarIconButton(IconTokens.BACK, tapNavigate(backUri));
            AccessibilityHelper.addButton(om, back, "Back");
            children.add(back);
            children.add(hSpacer(LayoutTokens.SPACING_SM));
        }
        if (title != null && !title.isBlank()) {
            ObjectNode titleEl = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            AccessibilityHelper.addHeading(om, titleEl, title, 1);
            children.add(titleEl);
        }

        root.set("children", children);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Horizontally-scrollable row of variant-selection chips.
     *
     * <p>Each chip is a {@code Button} atomic whose {@code onActivate} fires a
     * {@code navigate} action pointing at {@code currentUri} with the experiment
     * query param replaced by the chip's variant id. The currently active variant
     * renders with the {@code primary} button variant; others render as
     * {@code secondary} so the active chip is visually distinguished. Clients do
     * not need any variant-aware code: they just dispatch the navigate action and
     * the server returns the screen composed for the selected variant.
     *
     * <p>Bracket characters in the query param key are percent-encoded so the
     * emitted URI is parseable by client URL libraries (Foundation's
     * {@code URL(string:)} rejects unencoded {@code [} / {@code ]}).
     */
    public ObjectNode buildVariantChipsComposite(String sectionId, String analyticsId,
                                                  String currentUri, String experimentId,
                                                  ArrayNode options, String activeVariantId) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(currentUri, "currentUri");
        requireNonBlank(experimentId, "experimentId");

        ObjectNode scroll = scrollRow(8, false);
        scroll.put("widthMode", "fill");
        scroll.set("padding", padding(
                LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD,
                LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));

        ArrayNode chips = om.createArrayNode();
        boolean anyMatch = false;
        for (var option : options) {
            String id = option.path("id").asText();
            if (id == null || id.isBlank()) continue;
            if (id.equals(activeVariantId)) anyMatch = true;
        }
        for (var option : options) {
            String id = option.path("id").asText();
            String label = option.path("label").asText(id);
            if (id == null || id.isBlank()) continue;

            boolean isSelected = id.equals(activeVariantId)
                    || (!anyMatch && options.get(0) == option);

            String targetUri = withReplacedExperimentParam(currentUri, experimentId, id);
            ObjectNode chip = button(label, isSelected ? "primary" : "secondary", tapNavigate(targetUri));
            AccessibilityHelper.addButton(om, chip, label + (isSelected ? " (selected)" : ""));
            chips.add(chip);
        }
        scroll.set("children", chips);

        return wrapAsComposite(sectionId, analyticsId, scroll);
    }

    /**
     * Returns {@code uri} with the experiment query param set to {@code variantId},
     * replacing any existing value for the same experiment. Brackets in the param
     * key are percent-encoded so clients can parse the resulting URI without an
     * extra escaping step.
     */
    private String withReplacedExperimentParam(String uri, String experimentId, String variantId) {
        String paramKey = "experiments%5B" + experimentId + "%5D";
        String pair = paramKey + "=" + variantId;
        int qIndex = uri.indexOf('?');
        if (qIndex < 0) {
            return uri + "?" + pair;
        }
        String base = uri.substring(0, qIndex);
        String existingQuery = uri.substring(qIndex + 1);
        StringBuilder rebuilt = new StringBuilder();
        boolean replaced = false;
        for (String part : existingQuery.split("&")) {
            if (part.isEmpty()) continue;
            // Treat encoded and unencoded forms of `experiments[<id>]` as the same key.
            if (part.startsWith(paramKey + "=") || part.startsWith("experiments[" + experimentId + "]=")) {
                if (!replaced) {
                    if (rebuilt.length() > 0) rebuilt.append('&');
                    rebuilt.append(pair);
                    replaced = true;
                }
            } else {
                if (rebuilt.length() > 0) rebuilt.append('&');
                rebuilt.append(part);
            }
        }
        if (!replaced) {
            if (rebuilt.length() > 0) rebuilt.append('&');
            rebuilt.append(pair);
        }
        return base + "?" + rebuilt;
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
        root.put("gap", LayoutTokens.SPACING_MD);
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, LayoutTokens.SPACING_LG));
        ArrayNode children = om.createArrayNode();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null));
        }

        for (int i = 0; i < validItems.size(); i += 2) {
            ObjectNode row = container("row", null, "stretch");
            row.put("gap", LayoutTokens.SPACING_MD);
            row.put("widthMode", "fill");
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
        root.set("padding", padding(0, 0, 0, LayoutTokens.SPACING_MD));
        ArrayNode rootChildren = om.createArrayNode();
        if (title != null) {
            ObjectNode header = text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null);
            header.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_SM));
            rootChildren.add(header);
        }

        ObjectNode scroll = scrollRow(LayoutTokens.SPACING_MD, false);
        scroll.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
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
     * broadcastLogoUrlsCsv, targetUri, overflowUri] — {@code targetUri} and
     * {@code overflowUri} must be supplied by the composer from server data. The UI does not
     * render broadcast images (index 13 is ignored for layout); composers may still send a CSV
     * for future use.
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
        wrapper.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_XS, LayoutTokens.SPACING_XS));
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
        return buildGameScheduleList(sectionId, analyticsId, title, rows, refreshPolicy, dataBinding, null);
    }

    /**
     * Build a GameScheduleList composite with optional per-row live clock snapshots.
     * When a row's ID appears in {@code clockSnapshots}, the status column renders a
     * {@code LiveClock} element that counts down via the SSE data binding channel.
     *
     * @param clockSnapshots map from rowId to clock snapshot (null for no live clocks)
     */
    public ObjectNode buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows,
                                            ObjectNode refreshPolicy,
                                            ObjectNode dataBinding,
                                            java.util.Map<String, GameClockSnapshot> clockSnapshots) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(rows, "rows");

        List<String[]> validRows = validRows(rows, 0, 1, 6);

        ObjectNode root = container("column", null, "stretch");
        root.put("gap", LayoutTokens.SPACING_SM);
        root.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, LayoutTokens.SPACING_MD));
        ArrayNode children = om.createArrayNode();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_PRIMARY, null));
        }
        ObjectNode content = om.createObjectNode();
        for (String[] row : validRows) {
            String rowId = value(row, 0);
            GameClockSnapshot clock = clockSnapshots != null ? clockSnapshots.get(rowId) : null;
            children.add(gameScheduleRowElement(row, clock));
            ObjectNode state = om.createObjectNode();
            state.put("awayScore", parseInt(value(row, 4), 0));
            state.put("homeScore", parseInt(value(row, 9), 0));
            if (value(row, 11) != null) state.put("statusText", value(row, 11));
            if (clock != null) {
                ObjectNode clockState = om.createObjectNode();
                clockState.put("snapshotSeconds", clock.snapshotSeconds());
                clockState.put("snapshotAt", clock.snapshotAtIso());
                clockState.put("isRunning", clock.isRunning());
                state.set("clock", clockState);
            }
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

    /**
     * Full-bleed static image card with scrim, title/dek, optional outlined CTA
     * ({@code secondary} button + on-media text color), optional top-start badge,
     * and optional share/audio icon actions. Emits an {@code AtomicComposite} envelope.
     * <p>
     * This path uses an atomic {@link #image} base only; it does <strong>not</strong> mount a
     * {@code VideoPlayer}. Callers that need in-card video playback must compose a separate
     * {@code VideoPlayer} reservation with payload-owned dimensions (AGENTS.md §6.4).
     *
     * @param topStartBadgeText optional LIVE/NEW (or other) copy for a top-leading pill
     * @param shareActionUri    optional; when non-null, emits a share icon with this navigate target
     * @param audioActionUri   optional; when non-null, emits an audio-state icon with this target
     */
    public ObjectNode buildMediaOverlayCard(String sectionId, String analyticsId,
                                            String imageUrl, String title, String subtitle,
                                            String ctaLabel, String ctaTargetUri,
                                            String topStartBadgeText,
                                            String shareActionUri, String audioActionUri) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(imageUrl, "imageUrl");
        requireNonBlank(title, "title");

        String radius = LayoutTokens.RADIUS_MD;
        ObjectNode base = image(imageUrl, 0, 0, "cover", null);
        base.put("widthMode", "fill");
        base.put("aspectRatio", 16.0 / 9.0);
        base.put("cornerRadius", radius);
        AccessibilityHelper.addImage(om, base, title);

        List<ObjectNode> layers = new ArrayList<>();

        ObjectNode copyCol = container("column", "end", "start");
        copyCol.put("widthMode", "fill");
        copyCol.put("heightMode", "fill");
        copyCol.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG));
        copyCol.set("background", mediaBottomScrimGradient());
        copyCol.set("cornerRadii", cornerRadii(0, 0, radius, radius));
        ArrayNode copyChildren = om.createArrayNode();
        copyChildren.add(text(title, "titleMedium", "bold", ColorTokens.TEXT_ON_DARK_MEDIA, 3));
        if (subtitle != null) {
            copyChildren.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_ON_DARK_MEDIA, 3));
        }
        if (ctaLabel != null && ctaTargetUri != null) {
            ObjectNode cta = button(ctaLabel, "secondary", tapNavigate(ctaTargetUri));
            cta.put("color", ColorTokens.TEXT_ON_DARK_MEDIA);
            cta.put("background", "#00000000");
            copyChildren.add(spacer(LayoutTokens.SPACING_MD));
            copyChildren.add(cta);
        }
        copyCol.set("children", copyChildren);
        layers.add(overlay("bottomStart", null, copyCol));

        if (topStartBadgeText != null) {
            String t = topStartBadgeText.trim();
            ObjectNode pill = t.equalsIgnoreCase("LIVE")
                    ? liveBadge()
                    : pillBadge(topStartBadgeText, ColorTokens.BRAND_NBA);
            layers.add(overlay("topStart", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, 0, 0), pill));
        }

        if (shareActionUri != null || audioActionUri != null) {
            ObjectNode iconRow = container("row", "end", "center");
            iconRow.put("gap", LayoutTokens.SPACING_SM);
            ArrayNode iconKids = om.createArrayNode();
            if (audioActionUri != null) {
                iconKids.add(mediaOverlayIconButton(IconTokens.VIDEO, tapNavigate(audioActionUri)));
            }
            if (shareActionUri != null) {
                iconKids.add(mediaOverlayIconButton(IconTokens.SHARE, tapNavigate(shareActionUri)));
            }
            iconRow.set("children", iconKids);
            layers.add(overlay("topEnd", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, 0, 0), iconRow));
        }

        ObjectNode root = overlayContainer(base, layers);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /** Icon-only control on light app-bar surfaces (back affordance). */
    private ObjectNode appBarIconButton(String iconToken, ObjectNode action) {
        ObjectNode b = om.createObjectNode();
        b.put("type", "Button");
        b.put("label", "");
        b.put("icon", iconToken);
        b.put("variant", "text");
        b.put("color", ColorTokens.TEXT_PRIMARY);
        b.set("actions", singleActionArray(action));
        return b;
    }

    /** Icon-only control on dark media scrims (video hero overlays). */
    private ObjectNode mediaOverlayIconButton(String iconToken, ObjectNode action) {
        ObjectNode b = om.createObjectNode();
        b.put("type", "Button");
        b.put("label", "");
        b.put("icon", iconToken);
        b.put("variant", "text");
        b.put("color", ColorTokens.TEXT_ON_DARK_MEDIA);
        b.set("actions", singleActionArray(action));
        return b;
    }

    /**
     * Vertical gradient for bottom media scrim: transparent to dark black.
     * Uses hardcoded ARGB rather than the overlay.scrim token because the token
     * inverts in dark mode (becomes white), defeating the purpose of a darkening scrim.
     */
    private ObjectNode mediaBottomScrimGradient() {
        ObjectNode g = om.createObjectNode();
        ArrayNode colors = om.createArrayNode();
        colors.add("#00000000");
        colors.add("#99000000");
        colors.add("#F0000000");
        g.set("colors", colors);
        g.put("direction", "vertical");
        return g;
    }

    private ObjectNode storyCircleItem(String id, String label, String imageUrl,
                                       String badgeText, String targetUri) {
        ObjectNode item = container("column", "center", "center");
        item.put("id", id);
        item.put("width", 82);
        if (targetUri != null) {
            item.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, item, label);
        }
        ArrayNode children = om.createArrayNode();

        int inner = 70;
        // Story-rail convention: every avatar wears a red ring regardless of
        // whether it also carries a notification badge ("LIVE", "NEW", …).
        // The ring is the rail-wide visual signature (matches the real NBA
        // app's Following rail and our parity plan's "red circle around the
        // images in the top rail" target). The badge is an independent
        // overlay layered on the bottom-center of the avatar.
        int ring = 3;
        ObjectNode avatar = imageUrl != null
                ? image(imageUrl, inner, inner, "cover", null)
                : neutralInitials(label, inner, inner / 2);
        avatar.put("cornerRadius", inner / 2);
        // Defense in depth: opaque inner fill so a missing image doesn't
        // reveal the BRAND_LIVE ring color through to the whole disc.
        avatar.put("background", ColorTokens.SURFACE_SUNKEN);
        if (badgeText != null) {
            avatar = overlayContainer(avatar, List.of(overlay("bottomCenter",
                    padding(0, 0, 0, 0), pillBadge(badgeText, ColorTokens.BRAND_LIVE))));
        }
        ObjectNode ringWrap = container("row", "center", "center");
        ringWrap.put("width", inner + ring * 2);
        ringWrap.put("height", inner + ring * 2);
        ringWrap.put("cornerRadius", (inner + ring * 2) / 2);
        ringWrap.put("background", ColorTokens.BRAND_LIVE);
        ringWrap.set("padding", padding(ring, ring, ring, ring));
        ArrayNode ringKids = om.createArrayNode();
        ringKids.add(avatar);
        ringWrap.set("children", ringKids);
        children.add(ringWrap);
        children.add(spacer(LayoutTokens.SPACING_SM));
        children.add(text(label, "labelSmall", null, ColorTokens.TEXT_PRIMARY, 1));
        item.set("children", children);
        return item;
    }

    private ObjectNode editorialOverlayCard(String id, String title, String subtitle,
                                            String imageUrl, String badgeText, String targetUri) {
        String radius = LayoutTokens.RADIUS_MD;
        ObjectNode card = container("column", null, null);
        card.put("id", id);
        card.put("width", 200);
        card.put("cornerRadius", radius);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, card, title);
        }

        ObjectNode base = imageUrl != null
                ? image(imageUrl, 200, 0, "cover", null)
                : neutralInitialsRect(title, 200, 268, radius);
        base.put("widthMode", "fill");
        base.put("aspectRatio", 3.0 / 4.0);
        base.put("cornerRadius", radius);
        if (imageUrl != null) AccessibilityHelper.addImage(om, base, title);

        ObjectNode scrimContent = container("column", "end", "start");
        scrimContent.put("widthMode", "fill");
        scrimContent.put("heightMode", "fill");
        scrimContent.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
        scrimContent.set("background", mediaBottomScrimGradient());
        scrimContent.set("cornerRadii", cornerRadii(0, 0, radius, radius));
        ArrayNode children = om.createArrayNode();
        if (badgeText != null) {
            children.add(pillBadge(badgeText, ColorTokens.BRAND_LIVE));
            children.add(spacer(LayoutTokens.SPACING_SM));
        }
        children.add(text(title, "titleSmall", "bold", ColorTokens.TEXT_ON_DARK_MEDIA, 3));
        if (subtitle != null) {
            children.add(text(subtitle, "bodySmall", null, ColorTokens.TEXT_ON_DARK_MEDIA, 3));
        }
        scrimContent.set("children", children);

        ArrayNode cardChildren = om.createArrayNode();
        cardChildren.add(overlayContainer(base, List.of(overlay("bottomStart", null, scrimContent))));
        card.set("children", cardChildren);
        return card;
    }

    /** Top-right overflow control on hero key art; action must be server-declared. */
    private ObjectNode heroOverflowButton(ObjectNode navigateAction) {
        ObjectNode b = om.createObjectNode();
        b.put("type", "Button");
        b.put("label", "⋯");
        b.put("variant", "text");
        b.put("color", ColorTokens.TEXT_ON_DARK_MEDIA);
        b.set("actions", singleActionArray(navigateAction));
        return b;
    }

    private ObjectNode featuredLiveGameHeroCard(String[] card, boolean stretchToParentWidth) {
        String cardId = value(card, 0);
        int heroRadius = 0;
        ObjectNode hero = container("column", null, "stretch");
        hero.put("id", cardId);
        // Single-card section: card fills its surface (no fixed width).
        // Multi-card paged carousel: cards snap at a fixed width so a
        // peek of the next card is visible at the edge.
        if (stretchToParentWidth) {
            hero.put("widthMode", "fill");
        } else {
            hero.put("width", 338);
        }
        hero.put("cornerRadius", heroRadius);
        hero.put("background", ColorTokens.SURFACE_RAISED);
        shadow(hero);
        if (value(card, 14) != null) hero.set("actions", singleActionArray(tapNavigate(value(card, 14))));
        AccessibilityHelper.addButton(om, hero, value(card, 2));

        ObjectNode art = value(card, 4) != null
                ? image(value(card, 4), 0, 0, "cover", null)
                : neutralInitialsRect(value(card, 2), 338, 190, heroRadius);
        art.put("widthMode", "fill");
        art.put("aspectRatio", 16.0 / 9.0);
        if (value(card, 4) != null) AccessibilityHelper.addImage(om, art, value(card, 2));

        ObjectNode titleOverlay = container("column", "end", "start");
        titleOverlay.put("widthMode", "fill");
        titleOverlay.put("heightMode", "fill");
        titleOverlay.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD));
        titleOverlay.set("background", mediaBottomScrimGradient());
        ArrayNode overlayChildren = om.createArrayNode();
        if (value(card, 1) != null) {
            overlayChildren.add(pillBadge(value(card, 1), ColorTokens.BRAND_LIVE));
            overlayChildren.add(spacer(LayoutTokens.SPACING_SM));
        }
        overlayChildren.add(text(value(card, 2), "titleMedium", "bold", ColorTokens.TEXT_ON_DARK_MEDIA, 2));
        if (value(card, 3) != null) {
            overlayChildren.add(text(value(card, 3), "bodySmall", null, ColorTokens.TEXT_ON_DARK_MEDIA, 2));
        }
        titleOverlay.set("children", overlayChildren);

        List<ObjectNode> artOverlays = new ArrayList<>();
        artOverlays.add(overlay("bottomStart", null, titleOverlay));
        if (value(card, 15) != null) {
            artOverlays.add(overlay("topEnd", padding(LayoutTokens.SPACING_SM, LayoutTokens.SPACING_SM, 0, 0), heroOverflowButton(tapNavigate(value(card, 15)))));
        }

        ArrayNode heroChildren = om.createArrayNode();
        heroChildren.add(overlayContainer(art, artOverlays));
        heroChildren.add(heroScoreStrip(card));
        ArrayNode sponsorLogos = logoRow(value(card, 13), 48, 20);
        if (sponsorLogos.size() > 0) {
            heroChildren.add(cardHairlineDivider());
            ObjectNode sponsors = container("row", "end", "center");
            sponsors.put("gap", LayoutTokens.SPACING_SM);
            sponsors.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, LayoutTokens.SPACING_MD));
            sponsors.set("children", sponsorLogos);
            heroChildren.add(sponsors);
        }
        hero.set("children", heroChildren);
        return hero;
    }

    private ObjectNode heroScoreStrip(String[] card) {
        ObjectNode row = container("row", "spaceBetween", "center");
        row.put("widthMode", "fill");
        row.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_SM));
        ArrayNode children = om.createArrayNode();
        children.add(heroTeam(value(card, 5), value(card, 6), value(card, 7),
                "cards." + value(card, 0) + ".awayScore", false));

        ObjectNode center = container("column", "center", "center");
        center.put("width", 88);
        ArrayNode centerChildren = om.createArrayNode();
        boolean liveCue = value(card, 1) != null;
        if (liveCue) {
            ObjectNode statusLine = container("row", "center", "center");
            statusLine.put("gap", LayoutTokens.SPACING_XS);
            ArrayNode slKids = om.createArrayNode();
            slKids.add(liveStatusDot());
            ObjectNode status = text(value(card, 11), "labelSmall", "semiBold", ColorTokens.TEXT_SECONDARY, 1);
            status.put("bindRef", "cards." + value(card, 0) + ".statusText");
            slKids.add(status);
            statusLine.set("children", slKids);
            centerChildren.add(statusLine);
        } else {
            ObjectNode status = text(value(card, 11), "labelSmall", "semiBold", ColorTokens.TEXT_SECONDARY, 1);
            status.put("bindRef", "cards." + value(card, 0) + ".statusText");
            centerChildren.add(status);
        }
        if (value(card, 12) != null) {
            centerChildren.add(text(value(card, 12), "labelSmall", null, ColorTokens.TEXT_TERTIARY, 1));
        }
        center.set("children", centerChildren);
        children.add(center);

        children.add(heroTeam(value(card, 8), value(card, 9), value(card, 10),
                "cards." + value(card, 0) + ".homeScore", true));
        row.set("children", children);
        return row;
    }

    /**
     * Away: logo/name toward outer edge, score toward center. Home: score toward center, logo/name outer.
     */
    private ObjectNode heroTeam(String tri, String score, String logoUrl, String bindRef, boolean homeSide) {
        ObjectNode row = container("row", "center", "center");
        row.put("gap", LayoutTokens.SPACING_SM);
        row.put("width", homeSide ? 108 : 112);
        ArrayNode stackChildren = om.createArrayNode();
        if (logoUrl != null) {
            ObjectNode logoImg = image(logoUrl, 44, 44, "contain", null);
            AccessibilityHelper.addImage(om, logoImg, tri + " logo");
            stackChildren.add(logoImg);
        }
        stackChildren.add(text(tri, "labelSmall", "bold", ColorTokens.TEXT_PRIMARY, 1));
        ObjectNode nameCol = container("column", "center", "center");
        nameCol.set("children", stackChildren);

        ObjectNode scoreText = null;
        if (score != null) {
            scoreText = text(score, "titleLarge", "bold", ColorTokens.TEXT_PRIMARY, 1);
            scoreText.put("bindRef", bindRef);
            scoreText.put("monospacedDigits", true);
        }

        ArrayNode rowChildren = om.createArrayNode();
        if (homeSide) {
            if (scoreText != null) rowChildren.add(scoreText);
            rowChildren.add(nameCol);
        } else {
            rowChildren.add(nameCol);
            if (scoreText != null) rowChildren.add(scoreText);
        }
        row.set("children", rowChildren);
        return row;
    }

    private ObjectNode utilityCard(String[] item) {
        ObjectNode card = container("column", "center", "center");
        card.put("id", value(item, 0));
        card.put("gap", LayoutTokens.SPACING_SM);
        // Fixed height + widthMode:fill gives the grid balanced cells across rows
        // even when subtitles wrap differently. Width comes from the parent
        // row's flex distribution; height is locked to keep the grid rhythm
        // visually stable on mobile.
        card.put("height", 132);
        card.put("widthMode", "fill");
        card.put("background", ColorTokens.SURFACE_RAISED);
        card.put("cornerRadius", LayoutTokens.RADIUS_MD);
        card.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_MD));
        shadow(card);
        if (value(item, 4) != null) {
            card.set("actions", singleActionArray(tapNavigate(value(item, 4))));
            AccessibilityHelper.addButton(om, card, value(item, 1));
        }

        ArrayNode children = om.createArrayNode();
        if (value(item, 3) != null) {
            ObjectNode img = image(value(item, 3), 44, 44, "contain", null);
            AccessibilityHelper.addImage(om, img, value(item, 1) + " icon");
            children.add(img);
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
        card.put("width", 160);
        card.put("gap", LayoutTokens.SPACING_SM);
        card.put("background", ColorTokens.SURFACE_RAISED);
        card.put("cornerRadius", LayoutTokens.RADIUS_MD);
        card.set("padding", padding(LayoutTokens.SPACING_MD, LayoutTokens.SPACING_MD, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG));
        shadow(card);
        if (targetUri != null) {
            card.set("actions", singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(om, card, label);
        }

        ArrayNode children = om.createArrayNode();
        if (imageUrl != null) {
            ObjectNode logo = image(imageUrl, 72, 56, "contain", null);
            logo.put("aspectRatio", 4.0 / 3.0);
            AccessibilityHelper.addImage(om, logo, label + " logo");
            children.add(logo);
        } else {
            children.add(neutralInitials(label, 72, 28));
        }
        children.add(text(label, "bodyMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 2));
        card.set("children", children);
        return card;
    }

    private ObjectNode gameScheduleRowElement(String[] row) {
        return gameScheduleRowElement(row, null);
    }

    private ObjectNode gameScheduleRowElement(String[] row, GameClockSnapshot clock) {
        ObjectNode card = container("column", null, "stretch");
        card.put("id", value(row, 0));
        card.put("gap", LayoutTokens.SPACING_SM);
        // Refreshed card treatment: flush tile — secondary background, no corner radius, no
        // shadow. Cards sit edge-to-edge in the schedule list; inter-card spacing is owned
        // by the parent list gap rather than per-card margin.
        card.put("background", "token:nba.bg.secondary");
        card.put("cornerRadius", 0);
        card.set("padding", padding(LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG));
        if (value(row, 14) != null) {
            card.set("actions", singleActionArray(tapNavigate(value(row, 14))));
            String awayTri = value(row, 1) != null ? value(row, 1) : "";
            String homeTri = value(row, 6) != null ? value(row, 6) : "";
            String status = value(row, 11) != null ? value(row, 11) : "";
            AccessibilityHelper.addButton(om, card, awayTri + " vs " + homeTri + ", " + status);
        }

        ArrayNode children = om.createArrayNode();

        // Matchup row laid out left-to-right:
        //   awayTeamBlock  |  awayScore  |  statusColumn  |  homeScore  |  homeTeamBlock
        // The status column now owns the series-record subtitle (e.g. "NYK wins 4-0")
        // below the status text, matching the production NBA card.
        ObjectNode matchup = container("row", "spaceBetween", "center");
        matchup.put("widthMode", "fill");
        ArrayNode matchupChildren = om.createArrayNode();
        matchupChildren.add(scheduleTeamBlock(
                value(row, 1), value(row, 2), value(row, 3), value(row, 5), /* awayLeading */ true));
        matchupChildren.add(scheduleScoreText(value(row, 4), value(row, 0), "awayScore"));
        matchupChildren.add(scheduleStatus(value(row, 11), value(row, 0), clock, value(row, 12)));
        matchupChildren.add(scheduleScoreText(value(row, 9), value(row, 0), "homeScore"));
        matchupChildren.add(scheduleTeamBlock(
                value(row, 6), value(row, 7), value(row, 8), value(row, 10), /* awayLeading */ false));
        matchup.set("children", matchupChildren);
        children.add(matchup);

        // Optional broadcast/info row below the matchup (e.g. "ESPN").
        if (value(row, 13) != null) {
            ObjectNode broadcastRow = container("row", "center", "center");
            broadcastRow.put("widthMode", "fill");
            ArrayNode broadcastChildren = om.createArrayNode();
            broadcastChildren.add(text(value(row, 13), "labelSmall", null, ColorTokens.TEXT_TERTIARY, 1));
            broadcastRow.set("children", broadcastChildren);
            children.add(broadcastRow);
        }

        // Card footer: League Pass entry (left) + overflow More button (right). Rendered
        // on every card; the overflow button's tap target is omitted when slot 15 is
        // null but the icon still occupies the same screen position for visual rhythm.
        children.add(cardHairlineDivider());
        children.add(scheduleFooterRow(value(row, 15)));

        card.set("children", children);
        return card;
    }

    /**
     * Team block for a schedule row: logo + tricode/name stacked vertically. Mirrors the
     * production NBA card layout where logos sit at the outer edges of the row and the
     * name/seed reads inboard. Scores live in the center score cluster rather than the
     * team block so that the matchup row reads as
     * {@code [team] [score] [status] [score] [team]}.
     *
     * @param awayLeading {@code true} when this block is the away team (leftmost cell);
     *                    {@code false} for the home team (rightmost cell). Currently used
     *                    only for accessibility framing.
     */
    private ObjectNode scheduleTeamBlock(
            String tri, String name, String seed, String logoUrl, boolean awayLeading) {
        ObjectNode col = container("column", "center", "center");
        col.put("width", 72);
        ArrayNode children = om.createArrayNode();
        if (logoUrl != null) {
            ObjectNode logoImg = image(logoUrl, 48, 48, "contain", null);
            AccessibilityHelper.addImage(om, logoImg, (tri != null ? tri : "") + " logo");
            children.add(logoImg);
            children.add(spacer(LayoutTokens.SPACING_XS));
        }
        String seedPrefix = seed != null && !seed.isBlank() ? seed + " " : "";
        children.add(text(seedPrefix + (tri != null ? tri : ""),
                "titleMedium", "semiBold", ColorTokens.TEXT_PRIMARY, 1));
        if (name != null && !name.isBlank()) {
            children.add(text(name, "labelSmall", null, ColorTokens.TEXT_SECONDARY, 1));
        }
        col.set("children", children);
        return col;
    }

    /**
     * Score text element for a schedule row's center cluster. Returns an empty
     * fixed-width spacer when {@code score} is null so pre-game cards (no scores yet) keep
     * the row's center status horizontally aligned across cards.
     */
    private ObjectNode scheduleScoreText(String score, String rowId, String scoreKey) {
        if (score == null) {
            // Placeholder spacer so the status column stays in the same screen-x position
            // whether or not the game has a score yet. Width sized to roughly match a
            // 3-digit score rendered in the (compact) "score" typography token.
            ObjectNode placeholder = container("column", "center", "center");
            placeholder.put("width", 32);
            placeholder.set("children", om.createArrayNode());
            return placeholder;
        }
        ObjectNode scoreText = text(score, "score", "bold", ColorTokens.TEXT_PRIMARY, 1);
        scoreText.put("bindRef", rowId + "." + scoreKey);
        scoreText.put("monospacedDigits", true);
        scoreText.put("textAlign", "center");
        return scoreText;
    }

    private ObjectNode scheduleStatus(String status, String rowId) {
        return scheduleStatus(status, rowId, null, null);
    }

    private ObjectNode scheduleStatus(String status, String rowId,
                                      GameClockSnapshot clock) {
        return scheduleStatus(status, rowId, clock, null);
    }

    /**
     * Center column of a schedule row: status text (or LiveClock when game is live),
     * optionally followed by a series-record subtitle (e.g. "NYK wins 4-0") for playoff
     * games whose upstream payload carries {@code seriesText}.
     */
    private ObjectNode scheduleStatus(String status, String rowId,
                                      GameClockSnapshot clock, String seriesText) {
        ObjectNode center = container("column", "center", "center");
        center.put("width", 88);
        ArrayNode children = om.createArrayNode();
        String raw = status != null ? status : "";
        boolean liveCue = raw.toUpperCase(Locale.ROOT).contains("LIVE") || clock != null;

        if (clock != null) {
            // Live game: render a LiveClock that counts down via SSE binding
            ObjectNode clockNode = om.createObjectNode();
            clockNode.put("type", "LiveClock");
            clockNode.put("variant", "labelSmall");
            clockNode.put("format", "m:ss");
            clockNode.put("tickDirection", "down");
            clockNode.put("bindRef", rowId + ".clock");
            clockNode.put("snapshotSeconds", clock.snapshotSeconds());
            clockNode.put("isRunning", clock.isRunning());
            ObjectNode statusLine = container("row", "center", "center");
            statusLine.put("gap", LayoutTokens.SPACING_XS);
            ArrayNode sl = om.createArrayNode();
            sl.add(liveStatusDot());
            sl.add(clockNode);
            statusLine.set("children", sl);
            children.add(statusLine);
        } else {
            ObjectNode statusText = text(raw, "labelSmall", "semiBold",
                    ColorTokens.TEXT_SECONDARY, 2);
            statusText.put("bindRef", rowId + ".statusText");
            statusText.put("textAlign", "center");
            if (liveCue) {
                ObjectNode statusLine = container("row", "center", "center");
                statusLine.put("gap", LayoutTokens.SPACING_XS);
                ArrayNode sl = om.createArrayNode();
                sl.add(liveStatusDot());
                sl.add(statusText);
                statusLine.set("children", sl);
                children.add(statusLine);
            } else {
                children.add(statusText);
            }
        }
        if (seriesText != null && !seriesText.isBlank()) {
            ObjectNode sub = text(seriesText, "labelSmall", "semiBold",
                    ColorTokens.TEXT_TERTIARY, 2);
            sub.put("textAlign", "center");
            children.add(sub);
        }
        center.set("children", children);
        return center;
    }

    /**
     * Compact text link for schedule overflow (smaller than {@code Button} text variant).
     * Broadcast logos are not rendered here — URLs in row[13] are ignored for layout.
     */
    private ObjectNode scheduleMoreLink(ObjectNode navigateAction) {
        ObjectNode t = text("More", "bodySmall", "semiBold", ColorTokens.PALETTE_BLUE_50, null);
        t.set("actions", singleActionArray(navigateAction));
        return t;
    }

    /**
     * Card footer for a schedule row: League Pass CTA on the left, overflow "more"
     * button on the right. Both are placeholders today — League Pass routes to the
     * commerce surface; the more button is a no-op when {@code overflowUri} is null.
     */
    private ObjectNode scheduleFooterRow(String overflowUri) {
        ObjectNode row = container("row", "spaceBetween", "center");
        row.put("widthMode", "fill");
        ArrayNode kids = om.createArrayNode();

        // Left cluster: yellow circular play badge + "League Pass" label.
        ObjectNode left = container("row", "start", "center");
        left.put("gap", LayoutTokens.SPACING_SM);
        ArrayNode leftKids = om.createArrayNode();
        leftKids.add(leaguePassPlayBadge());
        leftKids.add(text("League Pass", "labelMedium", "semiBold",
                ColorTokens.TEXT_PRIMARY, 1));
        left.set("children", leftKids);
        left.set("actions", singleActionArray(tapNavigate("nba://commerce/leaguepass")));
        AccessibilityHelper.addButton(om, left, "League Pass");
        kids.add(left);

        // Right: overflow "more" button. When no overflowUri is provided the icon still
        // renders for layout rhythm but carries no action.
        ObjectNode more = om.createObjectNode();
        more.put("type", "Button");
        more.put("label", "");
        more.put("icon", IconTokens.MORE);
        more.put("variant", "text");
        more.put("color", ColorTokens.TEXT_SECONDARY);
        if (overflowUri != null) {
            more.set("actions", singleActionArray(tapNavigate(overflowUri)));
        }
        kids.add(more);

        row.set("children", kids);
        return row;
    }

    /**
     * Yellow filled circle (NBA brand) with a centered white play triangle. Composed
     * inline from a colored container + glyph so no bundled asset is required.
     */
    private ObjectNode leaguePassPlayBadge() {
        ObjectNode badge = container("column", "center", "center");
        badge.put("width", 24);
        badge.put("height", 24);
        badge.put("cornerRadius", 12);
        badge.put("background", ColorTokens.BRAND_NBA);
        ArrayNode kids = om.createArrayNode();
        kids.add(text("\u25B6", "labelSmall", "bold", ColorTokens.TEXT_INVERSE, 1));
        badge.set("children", kids);
        return badge;
    }

    /** 6dp live indicator dot ({@link ColorTokens#BRAND_LIVE}). */
    private ObjectNode liveStatusDot() {
        ObjectNode dot = container("row", "center", "center");
        dot.put("width", 6);
        dot.put("height", 6);
        dot.put("cornerRadius", LayoutTokens.RADIUS_SM);
        dot.put("background", ColorTokens.BRAND_LIVE);
        dot.set("children", om.createArrayNode());
        return dot;
    }

    private ObjectNode cardHairlineDivider() {
        ObjectNode d = om.createObjectNode();
        d.put("type", "Divider");
        d.put("thickness", 1);
        d.put("color", ColorTokens.BORDER_SUBTLE);
        return d;
    }

    private ObjectNode scrollRow(Object gap, boolean showIndicators) {
        ObjectNode scroll = om.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        putLayoutScalar(scroll, "gap", gap);
        scroll.put("showIndicators", showIndicators);
        return scroll;
    }

    /**
     * Paged horizontal carousel: sets {@code paging}, {@code snapAlignment},
     * and {@code pageIndicator} only when {@code childCount > 1}. Dot colors
     * default to tertiary/inverse; pass non-null overrides to customize.
     */
    private ObjectNode pagedHorizontalScroll(Object gap, int childCount, ObjectNode padding,
                                            String indicatorAlignment,
                                            String inactiveDotColor, String activeDotColor) {
        ObjectNode scroll = scrollRow(gap, false);
        if (padding != null) {
            scroll.set("padding", padding);
        }
        if (childCount > 1) {
            scroll.put("paging", true);
            scroll.put("snapAlignment", "center");
            ObjectNode indicator = om.createObjectNode();
            indicator.put("style", "dots");
            indicator.put("alignment", indicatorAlignment != null ? indicatorAlignment : "bottomCenter");
            indicator.put("color", inactiveDotColor != null ? inactiveDotColor : ColorTokens.TEXT_TERTIARY);
            indicator.put("activeColor", activeDotColor != null ? activeDotColor : ColorTokens.TEXT_INVERSE);
            scroll.set("pageIndicator", indicator);
        }
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

    private ObjectNode neutralInitials(String label, int width, Object radius) {
        return neutralInitialsRect(label, width, width, radius);
    }

    private ObjectNode neutralInitialsRect(String label, int width, int height, Object radius) {
        ObjectNode box = container("row", "center", "center");
        box.put("width", width);
        box.put("height", height);
        putLayoutScalar(box, "cornerRadius", radius);
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
        return sectionEnvelope(id, analyticsId, null, null);
    }

    /**
     * Overload that accepts a custom refreshPolicy node (for poll, sse, etc.).
     * Falls back to static if refreshPolicy is null.
     */
    ObjectNode sectionEnvelope(String id, String analyticsId, ObjectNode refreshPolicy) {
        return sectionEnvelope(id, analyticsId, refreshPolicy, null);
    }

    /**
     * Full overload that accepts refreshPolicy and contentSourceId.
     * Falls back to static if refreshPolicy is null.
     */
    ObjectNode sectionEnvelope(String id, String analyticsId, ObjectNode refreshPolicy, String contentSourceId) {
        ObjectNode section = om.createObjectNode();
        section.put("id", id);
        section.put("type", "AtomicComposite");
        if (analyticsId != null) section.put("analyticsId", analyticsId);
        if (contentSourceId != null) section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", refreshPolicy != null ? refreshPolicy
                : om.createObjectNode().put("type", "static"));
        return section;
    }

    /**
     * Full builder that derives section ID from contentSourceId and handles envelope setup.
     * Centralizes ID derivation + contentSourceId assignment.
     *
     * @param contentSourceId origin identifier (e.g. "cms:article-42", "stats-api:leaders-2025")
     * @param sectionType     section type (typically "AtomicComposite")
     * @param analyticsId     analytics identifier (optional)
     * @param refreshPolicy   refresh policy node (optional, defaults to static)
     * @return section envelope with derived ID and contentSourceId field
     */
    ObjectNode sectionEnvelopeWithDerivedId(String contentSourceId, String sectionType,
                                            String analyticsId, ObjectNode refreshPolicy) {
        String sectionId = SectionIdDeriver.derive(contentSourceId, sectionType);
        ObjectNode section = om.createObjectNode();
        section.put("id", sectionId);
        section.put("type", sectionType);
        section.put("contentSourceId", contentSourceId);
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
     * semantic-section composers (SubscribeBanner / SubscribeHero /
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

    // Allowed values for widthMode / heightMode, mirroring the SizeMode enum
    // in schema/sdui-schema.json.
    private static final Set<String> VALID_SIZE_MODES = Set.of("hug", "fill", "fixed");

    // Allowed values for alignSelf, reuses the CrossAlignment enum.
    private static final Set<String> VALID_ALIGN_SELF = Set.of("start", "center", "end", "stretch");

    // Allowed values for DataBindingPath.transform, mirroring the Transform
    // enum in schema/sdui-schema.json. Kept in lock-step with the schema —
    // when an enum value is added or removed there, update the set here too.
    static final Set<String> VALID_BINDING_TRANSFORMS = Set.of("liveClockSnapshot");

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
     * Validate a {@code DataBindingPath.transform} value against the schema
     * enum before it is emitted on the wire. Mirrors {@link #validateEnum}
     * but lives at package scope so {@link SduiUtils#bindingPath(String,
     * String, String)} can guard its arbitrary-string overload at the
     * composer-build site instead of crashing strict-decoding clients.
     */
    static void validateTransform(String value) {
        if (value == null) return;
        if (!VALID_BINDING_TRANSFORMS.contains(value)) {
            throw new IllegalArgumentException(
                "DataBindingPath.transform value '" + value
                    + "' is not in the schema enum " + VALID_BINDING_TRANSFORMS
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
     * widthMode is "fill" by default to maintain parity with the old Row renderer.
     */
    ObjectNode responsiveRow(Object gap, int breakpoint) {
        ObjectNode node = container("row", null, null);
        putLayoutScalar(node, "gap", gap);
        node.put("breakpoint", breakpoint);
        node.put("widthMode", "fill");
        return node;
    }

    /** Set flex on an element node (used on children of a Container for proportional sizing). */
    void setFlex(ObjectNode element, double flex) {
        element.put("flex", flex);
    }

    /** Set width sizing mode on an element: "hug", "fill", or "fixed". */
    void widthMode(ObjectNode element, String mode) {
        validateEnum("widthMode", mode, VALID_SIZE_MODES);
        element.put("widthMode", mode);
    }

    /** Set height sizing mode on an element: "hug", "fill", or "fixed". */
    void heightMode(ObjectNode element, String mode) {
        validateEnum("heightMode", mode, VALID_SIZE_MODES);
        element.put("heightMode", mode);
    }

    /** Set minWidth (LayoutScalar: integer or token string). */
    void minWidth(ObjectNode element, Object scalar) {
        putLayoutScalar(element, "minWidth", scalar);
    }

    /** Set maxWidth (LayoutScalar: integer or token string). */
    void maxWidth(ObjectNode element, Object scalar) {
        putLayoutScalar(element, "maxWidth", scalar);
    }

    /** Set minHeight (LayoutScalar: integer or token string). */
    void minHeight(ObjectNode element, Object scalar) {
        putLayoutScalar(element, "minHeight", scalar);
    }

    /** Set maxHeight (LayoutScalar: integer or token string). */
    void maxHeight(ObjectNode element, Object scalar) {
        putLayoutScalar(element, "maxHeight", scalar);
    }

    /** Enable flex-wrap on a Container element. */
    void layoutWrap(ObjectNode element, boolean wrap) {
        element.put("layoutWrap", wrap);
    }

    /** Set cross-axis gap for wrapped layouts (LayoutScalar: integer or token string). */
    void crossAxisGap(ObjectNode element, Object scalar) {
        putLayoutScalar(element, "crossAxisGap", scalar);
    }

    /** Set per-child cross-axis alignment override: "start", "center", "end", or "stretch". */
    void alignSelf(ObjectNode element, String alignment) {
        validateEnum("alignSelf", alignment, VALID_ALIGN_SELF);
        element.put("alignSelf", alignment);
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

    /** ORB-safe: same-origin static asset (see web/public/sdui-demo, server static/sdui-demo). */
    private static final String DEFAULT_PLACEHOLDER = DemoImageUrls.placeholderTiny();

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

    ObjectNode spacer(String height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    /** Spacer with a raw pixel height (§3.6 exception: no design-system token exists). */
    ObjectNode spacer(int height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    ObjectNode hSpacer(String width) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("width", width);
        return node;
    }

    ObjectNode padding(Object start, Object end, Object top, Object bottom) {
        ObjectNode p = om.createObjectNode();
        putLayoutScalar(p, "start", start);
        putLayoutScalar(p, "end", end);
        putLayoutScalar(p, "top", top);
        putLayoutScalar(p, "bottom", bottom);
        return p;
    }

    /**
     * Builds an AtomicElement.cornerRadii block with all four corners set.
     * Used for cards that want asymmetric rounding (e.g. rounded-top +
     * squared-bottom content cards). An explicit 0 on a corner means
     * "square" — it is preserved through the wire (it is not treated as
     * "omitted, fall back to cornerRadius") per the schema contract.
     */
    private ObjectNode cornerRadii(Object topStart, Object topEnd, Object bottomStart, Object bottomEnd) {
        ObjectNode r = om.createObjectNode();
        putLayoutScalar(r, "topStart", topStart);
        putLayoutScalar(r, "topEnd", topEnd);
        putLayoutScalar(r, "bottomStart", bottomStart);
        putLayoutScalar(r, "bottomEnd", bottomEnd);
        return r;
    }

    /** Puts a LayoutScalar value (int or "token:..." string) into a node. */
    private void putLayoutScalar(ObjectNode node, String key, Object value) {
        if (value instanceof String s) {
            node.put(key, s);
        } else if (value instanceof Number n) {
            node.put(key, n.intValue());
        }
    }

    ObjectNode tapNavigate(String targetUri) {
        ObjectNode action = om.createObjectNode();
        action.put("trigger", "onActivate");
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
