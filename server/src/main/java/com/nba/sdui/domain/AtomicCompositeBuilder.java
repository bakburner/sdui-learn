package com.nba.sdui.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.AtomicComposite;
import com.nba.sdui.models.generated.AtomicElement;
import com.nba.sdui.models.generated.AtomicOverlay;
import com.nba.sdui.models.generated.Badge;
import com.nba.sdui.models.generated.Column;
import com.nba.sdui.models.generated.Content;
import com.nba.sdui.models.generated.CornerRadii;
import com.nba.sdui.models.generated.DataBinding;
import com.nba.sdui.models.generated.ExperimentVariantOption;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Row;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;
import com.nba.sdui.models.generated.Spacing;

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
    private final Tokens tokens;

    public AtomicCompositeBuilder(ObjectMapper objectMapper, Tokens tokens) {
        this.om = objectMapper;
        this.tokens = tokens;
    }

    private Section bindSection(ObjectNode n) {
        return om.convertValue(n, Section.class);
    }

    private AtomicElement bindElement(ObjectNode n) {
        return om.convertValue(n, AtomicElement.class);
    }

    private Spacing bindSpacing(ObjectNode n) {
        return om.convertValue(n, Spacing.class);
    }

    private Action bindAction(ObjectNode n) {
        return om.convertValue(n, Action.class);
    }

    private AtomicComposite bindAtomicComposite(ObjectNode n) {
        return om.convertValue(n, AtomicComposite.class);
    }

    private ObjectNode toObjectNode(Object pojo) {
        return (ObjectNode) om.valueToTree(pojo);
    }

    /** Build a typed Section envelope with id and type=AtomicComposite. */
    private Section newSection(String id, String analyticsId) {
        Section section = new Section();
        section.setId(id);
        section.setType(Section.Type.fromValue("AtomicComposite"));
        if (analyticsId != null) section.setAnalyticsId(analyticsId);
        section.setRefreshPolicy(new RefreshPolicy()
                .withType(RefreshPolicy.RefreshType.fromValue("static")));
        return section;
    }

    /** Variant accepting a typed RefreshPolicy (null leaves the static default in place). */
    private Section newSection(String id, String analyticsId, RefreshPolicy refreshPolicy) {
        Section section = newSection(id, analyticsId);
        if (refreshPolicy != null) {
            section.setRefreshPolicy(refreshPolicy);
        }
        return section;
    }

    /** Wrap a typed root element as the section's AtomicComposite data payload. */
    private void wrapUi(Section section, AtomicElement root) {
        AtomicComposite data = new AtomicComposite();
        data.setUi(root);
        section.setData(data);
    }

    // ── ErrorState ──────────────────────────────────────────────────────

    public Section buildErrorState(String sectionId, String title, String message,
                                       String icon, String retryUri) {
        Section section = newSection(sectionId, null);

        String emoji = switch (icon) {
            case "wifi_off" -> "\uD83D\uDCE1";
            case "not_found" -> "\uD83D\uDD0D";
            case "timeout" -> "⏱\uFE0F";
            default -> "⚠\uFE0F";
        };

        AtomicElement root = container("column", "center", "center")
                .withPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                        tokens.spacing("xl"), tokens.spacing("xl")));
        List<AtomicElement> children = new ArrayList<>();

        children.add(text(emoji, "titleLarge", null, null, null));
        children.add(spacer(tokens.spacing("md")));
        children.add(text(title, "titleMedium", "bold", null, null));

        if (message != null && !message.isBlank()) {
            children.add(spacer(tokens.spacing("md")));
            children.add(text(message, "bodyMedium", null, tokens.color("nba.label.secondary"), null));
        }

        if (retryUri != null) {
            children.add(spacer(tokens.spacing("lg")));
            children.add(button("Try Again", "primary", tapNavigate(retryUri)));
        }

        root.setChildren(children);
        wrapUi(section, root);
        return section;
    }

    // ── SectionHeader ───────────────────────────────────────────────────

    public Section buildSectionHeader(String id, String title,
                                          String subtitle, String actionLabel,
                                          String actionUri) {
        Section section = newSection(id, null);

        AtomicElement root = container("row", "spaceBetween", "center");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        // Header internal padding: nba.spacing.md top (air above title),
        // nba.spacing.xs bottom. The vertical rhythm between the header
        // title and the next section is produced by the surface-level
        // margin on the *next* section's surface (typically
        // `railSurface.margin.top = nba.spacing.lg`), not by extra padding
        // inside the header. Keeping bottom tight here avoids the
        // "double-gap" look where header-bottom-padding + next-section-
        // top-margin + next-section-internal-top-padding all stack.
        root.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("md"), tokens.spacing("xs")));
        List<AtomicElement> children = new ArrayList<>();

        if (subtitle != null) {
            AtomicElement titleCol = container("column", null, null);
            List<AtomicElement> titleChildren = new ArrayList<>();
            AtomicElement titleText = text(title, "titleMedium", "bold", null, null);
            AccessibilityHelper.addHeading(titleText, title, 2);
            titleChildren.add(titleText);
            titleChildren.add(text(subtitle, "bodySmall", null, tokens.color("nba.label.tertiary"), null));
            titleCol.setChildren(titleChildren);
            children.add(titleCol);
        } else {
            AtomicElement titleText = text(title, "titleMedium", "bold", null, null);
            AccessibilityHelper.addHeading(titleText, title, 2);
            children.add(titleText);
        }

        if (actionUri != null) {
            String label = actionLabel != null ? actionLabel : "See All";
            children.add(button(label, "text", tapNavigate(actionUri)));
        }

        root.setChildren(children);
        wrapUi(section, root);
        return section;
    }

    // ── PromoBanner ─────────────────────────────────────────────────────

    public Section buildPromoBanner(String id, String analyticsId,
                                        String title, String headline,
                                        String subhead, String imageUrl,
                                        String ctaLabel, String targetUri) {
        // Default text-color triplet matches the historical gradient-surface
        // styling used by Watch / Scoreboard / Demo. Callers on solid-card
        // surfaces (e.g. the Games-screen promo on `bg.secondary`) should
        // use the overload below and pass `TEXT_PRIMARY` so the headline
        // tracks the surface across light/dark themes.
        return buildPromoBanner(id, analyticsId, title, headline, subhead,
                imageUrl, ctaLabel, targetUri,
                tokens.color("nba.label.accent.live"),
                tokens.color("nba.label-inverted.primary"),
                tokens.color("nba.label.secondary"));
    }

    public Section buildPromoBanner(String id, String analyticsId,
                                        String title, String headline,
                                        String subhead, String imageUrl,
                                        String ctaLabel, String targetUri,
                                        String titleColorToken,
                                        String headlineColorToken,
                                        String subheadColorToken) {
        Section section = newSection(id, analyticsId);

        // Emit a bare root container — outer chrome (background, padding,
        // radius, shadow, margin) is owned exclusively by the shared
        // SectionContainer wrapper via `section.surface`. Callers must
        // supply `section.surface` on the returned envelope. The row
        // stretches to fill the available width so the promo subject
        // left-aligns and the CTA (if any) sits against the trailing edge.
        AtomicElement root = container("row", null, "center");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (imageUrl != null) {
            AtomicElement img = image(imageUrl, 120, 80, "cover");
            img.setCornerRadius(tokens.radius("md"));
            AccessibilityHelper.addImage(img, headline != null ? headline : "Promo image");
            rootChildren.add(img);
            rootChildren.add(spacer(tokens.spacing("lg")));
        }

        AtomicElement contentCol = container("column", null, "start");
        setFlex(contentCol, 1.0);
        List<AtomicElement> colChildren = new ArrayList<>();

        if (title != null) {
            colChildren.add(text(title.toUpperCase(), "labelSmall", "bold", titleColorToken, null));
            colChildren.add(spacer(tokens.spacing("sm")));
        }
        if (headline != null) {
            colChildren.add(text(headline, "titleMedium", "bold", headlineColorToken, null));
            colChildren.add(spacer(tokens.spacing("sm")));
        }
        if (subhead != null) {
            colChildren.add(text(subhead, "bodySmall", null, subheadColorToken, 2));
        }
        if (targetUri != null) {
            colChildren.add(spacer(tokens.spacing("md")));
            String label = ctaLabel != null ? ctaLabel : "Learn More";
            colChildren.add(button(label, "primary", tapNavigate(targetUri)));
        }

        contentCol.setChildren(colChildren);
        rootChildren.add(contentCol);
        root.setChildren(rootChildren);
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
    public Section buildContentRail(String id, String analyticsId,
                                        String title, String[][] cards) {
        Section section = newSection(id, analyticsId);

        AtomicElement root = container("column", null, null);
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("md"), tokens.spacing("md")));
            rootChildren.add(titleEl);
        }

        AtomicElement scroll = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("ScrollContainer"))
                .withDirection(AtomicElement.Direction.fromValue("row"))
                .withGap(tokens.spacing("md"));
        scroll.setShowIndicators(false);
        List<AtomicElement> scrollChildren = new ArrayList<>();

        for (String[] c : cards) {
            scrollChildren.add(buildContentCard(c[0], c[1], c[2], c[3], c[4], c[5], c[6]));
        }

        scroll.setChildren(scrollChildren);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
        wrapUi(section, root);
        return section;
    }

    private AtomicElement buildContentCard(String id, String headline, String subhead,
                                          String thumbnailUrl, String contentType,
                                          String duration, String targetUri) {
        // Plain container (no variant): the `elevated` variant's default
        // shadow made the inter-card gap look muddy — each card's shadow
        // bled into the 12pt gap between siblings in the rail. Dropping
        // the variant removes the shadow entirely; the card silhouette
        // is defined by its own gradient background, corner radius, and
        // the scroll container's sibling gap (not by a drop shadow on
        // neighbouring surfaces).
        AtomicElement card = container("column", null, null);
        card.setId(id);
        // Fix the card's outer width so the image (also 200) and any
        // full-width overlays (duration badge strip) meet the card edge
        // flush. Without this the card sizes to max(child intrinsic
        // widths) — a long 2-line headline would push the card past
        // 200, leaving an empty right gutter next to the image.
        card.setWidth(200);
        card.setCornerRadius(0);
        // Subtle vertical gradient so the card silhouette reads against
        // the feed background without relying on a drop shadow.
        card.setBackground(gradient(tokens.color("nba.bg.secondary"),
                tokens.color("nba.bg.tertiary"), "vertical"));
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, headline);
        }
        List<AtomicElement> children = new ArrayList<>();

        if (thumbnailUrl != null) {
            AtomicElement imageWrap = container("column", null, null);
            imageWrap.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            // Media should meet the card's top edge; any inset creates a
            // visible strip of card background above the thumbnail.
            imageWrap.setPadding(padding(0, 0, 0, 0));

            AtomicElement img = thumbnailImage(thumbnailUrl);
            img.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            img.setAspectRatio(16.0 / 9.0);
            AccessibilityHelper.addImage(img, headline);
            if (duration != null) {
                badge(img, durationBadge(duration), "bottomEnd");
            } else if ("video".equalsIgnoreCase(contentType)) {
                badge(img, liveBadge(), "bottomEnd");
            }
            imageWrap.setChildren(List.of(img));
            children.add(imageWrap);
        }

        children.add(spacer(tokens.spacing("sm")));
        AtomicElement headlineEl = text(headline, "bodySmall", "semiBold",
                tokens.color("nba.label.primary"), 2);
        headlineEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("sm"), tokens.spacing("md")));
        children.add(headlineEl);

        card.setChildren(children);
        return card;
    }

    // ── FollowingRail ───────────────────────────────────────────────────

    /**
     * Build a FollowingRail as an AtomicComposite.
     * Each item is a circular avatar + name label.
     *
     * @param items  Array of [id, name, imageUrl, entityType, targetUri]
     */
    public Section buildFollowingRail(String id, String analyticsId,
                                          String title, String[][] items) {
        Section section = newSection(id, analyticsId);

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("sm"), tokens.spacing("sm")));
            rootChildren.add(titleEl);
        }

        AtomicElement scroll = scrollContainer("row", tokens.spacing("lg"), false);
        List<AtomicElement> scrollChildren = new ArrayList<>();

        for (String[] item : items) {
            scrollChildren.add(buildFollowingItem(item[0], item[1], item[2], item[4]));
        }

        scroll.setChildren(scrollChildren);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
        wrapUi(section, root);
        return section;
    }

    private AtomicElement buildFollowingItem(String id, String name, String imageUrl,
                                            String targetUri) {
        AtomicElement item = container("column", "center", "center");
        item.setId(id);
        if (targetUri != null) {
            item.setActions(singleActionArray(tapNavigate(targetUri)));
        }
        List<AtomicElement> children = new ArrayList<>();

        if (imageUrl != null) {
            AtomicElement img = image(imageUrl, 56, 56, "cover");
            img.setCornerRadius(tokens.radius("lg"));
            AccessibilityHelper.addImage(img, name + " avatar");
            children.add(img);
        } else {
            AtomicElement fallback = text(
                    name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase(),
                    "labelSmall", "bold", tokens.color("nba.label.tertiary"), null);
            children.add(fallback);
        }

        children.add(spacer(tokens.spacing("sm")));
        AtomicElement nameEl = text(name, "labelSmall", null, null, 1);
        nameEl.setMaxLines(1);
        children.add(nameEl);

        item.setChildren(children);
        return item;
    }

    // ── DisplayGrid ───────────────────────────────────────────────────

    /**
     * Build a DisplayGrid as an AtomicComposite.
     *
     * @param columns  Array of [key, label, align] — align is "start", "center", or "end"
     * @param rows     Array of Maps mapping column keys to pre-formatted display values
     */
    public Section buildDisplayGrid(String id, String analyticsId,
                                        String title,
                                        String[][] columns, String[][] rows,
                                        boolean striped) {
        Section section = newSection(id, analyticsId);

        AtomicElement root = container("column", null, "stretch");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("md"), tokens.spacing("md")));
            rootChildren.add(titleEl);
        }

        AtomicElement grid = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("DisplayGrid"));
        grid.setId(id + "-grid");
        grid.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        grid.setStriped(striped);

        List<Column> colList = new ArrayList<>();
        for (String[] col : columns) {
            Column c = new Column().withKey(col[0]).withLabel(col[1]);
            if (col.length > 2 && col[2] != null) c.setAlign(Column.Align.fromValue(col[2]));
            colList.add(c);
        }
        grid.setColumns(colList);

        List<Row> rowList = new ArrayList<>();
        for (String[] row : rows) {
            Row r = new Row();
            for (int i = 0; i < columns.length && i < row.length; i++) {
                r.setAdditionalProperty(columns[i][0], row[i]);
            }
            rowList.add(r);
        }
        grid.setRows(rowList);

        rootChildren.add(grid);
        root.setChildren(rootChildren);
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
    public Section buildHeroPanel(String id, String analyticsId,
                                      String headline, String subhead,
                                      String thumbnailUrl, String contentType,
                                      String duration, String targetUri) {
        Section section = newSection(id, analyticsId);

        AtomicElement card = heroContainer("column", null, null);
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, headline);
        }
        List<AtomicElement> children = new ArrayList<>();

        if (thumbnailUrl != null) {
            AtomicElement imgContainer = container("column", null, null);
            List<AtomicElement> imgChildren = new ArrayList<>();
            // Inline hero image treatment: 16:9 artwork that fills card
            // width, rounded top corners, square bottom edge, cover fit.
            // The `hero` ImageVariant was pruned because this surface is
            // inline-expressible.
            AtomicElement img = image(thumbnailUrl, 0, 0, "cover");
            img.setAspectRatio(16.0 / 9.0);
            img.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            img.setCornerRadii(cornerRadiiOf(tokens.radius("lg"), tokens.radius("lg"), 0, 0));
            AccessibilityHelper.addImage(img, headline);
            imgChildren.add(img);
            if (duration != null) {
                AtomicElement dur = text(duration, "labelSmall", null,
                        tokens.color("nba.label-inverted.primary"), null);
                dur.setPadding(padding(tokens.spacing("sm"), tokens.spacing("sm"), 0, 0));
                imgChildren.add(dur);
            }
            imgContainer.setChildren(imgChildren);
            children.add(imgContainer);
        }

        AtomicElement textCol = container("column", null, null);
        textCol.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("md"), tokens.spacing("md")));
        List<AtomicElement> textChildren = new ArrayList<>();

        if (contentType != null) {
            AtomicElement badge = text(contentType.toUpperCase(), "labelSmall", "bold",
                    tokens.color("nba.label.accent.live"), null);
            badge.setPadding(padding(0, 0, 0, tokens.spacing("xs")));
            textChildren.add(badge);
        }

        textChildren.add(text(headline, "titleSmall", "bold", tokens.color("nba.label.primary"), 2));

        if (subhead != null) {
            AtomicElement sub = text(subhead, "bodySmall", null,
                    tokens.color("nba.label.secondary"), 2);
            sub.setPadding(padding(0, 0, tokens.spacing("xs"), 0));
            textChildren.add(sub);
        }

        textCol.setChildren(textChildren);
        children.add(textCol);
        card.setChildren(children);

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("sm"), tokens.spacing("sm")));
        root.setChildren(List.of(card));
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
     * Build a {@link GamePanelTeam} from the typed scoreboard
     * {@link com.nba.sdui.integration.model.scoreboard.ScoreboardTeam}.
     */
    public GamePanelTeam gamePanelTeam(com.nba.sdui.integration.model.scoreboard.ScoreboardTeam team) {
        if (team == null) {
            return new GamePanelTeam("", 0, SduiUtils.teamLogoUrl(""));
        }
        String tricode = team.getTeamTricode() != null ? team.getTeamTricode() : "";
        int score = team.getScore();
        String logoUrl = team.getLogoUrl() != null && !team.getLogoUrl().isBlank()
                ? team.getLogoUrl()
                : SduiUtils.teamLogoUrl(team.getTeamId() != null ? team.getTeamId() : "");
        return new GamePanelTeam(tricode, score, logoUrl);
    }

    /**
     * Build a {@link GamePanelTeam} from the typed boxscore
     * {@link com.nba.sdui.integration.model.boxscore.BoxscoreTeam}.
     */
    public GamePanelTeam gamePanelTeam(com.nba.sdui.integration.model.boxscore.BoxscoreTeam team) {
        if (team == null) {
            return new GamePanelTeam("", 0, SduiUtils.teamLogoUrl(""));
        }
        String tricode = team.getTeamTricode() != null ? team.getTeamTricode() : "";
        int score = team.getScore() != null ? team.getScore() : 0;
        String teamIdStr = team.getTeamId() != null ? String.valueOf(team.getTeamId()) : "";
        String logoUrl = SduiUtils.teamLogoUrl(teamIdStr);
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

    public Section buildGamePanelComposite(
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
            RefreshPolicy refreshPolicy,
            DataBinding linescoreBindings,
            SectionSurface surface) {
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
    public Section buildGamePanelComposite(
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
            RefreshPolicy refreshPolicy,
            DataBinding linescoreBindings,
            SectionSurface surface) {
        boolean featured = "featured".equals(variant);
        // Featured uses 20px padding — no exact token exists (§3.6 exception: no design-system token).
        Object rootPadding = featured ? 20 : tokens.spacing("lg");
        String cornerRadiusToken = featured ? tokens.radius("lg") : tokens.radius("md");

        // Root vertical container with tap-to-navigate action.
        AtomicElement root = container("column", null, "stretch");
        root.setId(sectionId + "-root");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setCornerRadius(cornerRadiusToken);
        root.setPadding(padding(rootPadding, rootPadding, rootPadding, rootPadding));
        if (navigateUri != null) {
            root.setActions(singleActionArray(tapNavigate(navigateUri)));
            String a11yLabel = awayTeam.tricode() + " vs " + homeTeam.tricode();
            if (gameStatusText != null) a11yLabel += ", " + gameStatusText;
            AccessibilityHelper.addButton(root, a11yLabel);
        }

        List<AtomicElement> rootChildren = new ArrayList<>();

        if (badgeText != null && !badgeText.isEmpty()) {
            AtomicElement badge = text(badgeText, "labelSmall", "bold", "#FFFFFF", null);
            badge.setBackground("#E03131");
            badge.setCornerRadius(tokens.radius("sm"));
            badge.setPadding(padding(tokens.spacing("sm"), tokens.spacing("sm"),
                    tokens.spacing("xs"), tokens.spacing("xs")));
            rootChildren.add(badge);
            rootChildren.add(spacer(tokens.spacing("md")));
        }

        // Optional series/context row above the matchup (e.g. "BOS leads series 3-2").
        if (seriesText != null && !seriesText.isEmpty()) {
            AtomicElement seriesEl = text(seriesText, "labelSmall", null,
                    tokens.color("nba.label.tertiary"), 1);
            seriesEl.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
            rootChildren.add(seriesEl);
            rootChildren.add(spacer(tokens.spacing("sm")));
        }

        // Teams row: away | status-slot | home, spread edge-to-edge.
        AtomicElement row = container("row", "spaceBetween", "center");
        row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        List<AtomicElement> rowChildren = new ArrayList<>();

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
        row.setChildren(rowChildren);
        rootChildren.add(row);

        root.setChildren(rootChildren);

        // Envelope + data.ui + data.content.
        Section section = newSection(sectionId, analyticsId, refreshPolicy);
        AtomicComposite data = new AtomicComposite().withUi(root);
        data.setContent(buildGamePanelContent(gameStatusText, gameStatus, homeTeam, awayTeam, clock));
        section.setData(data);

        if (linescoreBindings != null) {
            section.setDataBinding(linescoreBindings);
        }
        if (surface != null) {
            section.setSurface(surface);
        }

        return section;
    }

    /**
     * Vertical team column: logo, tricode, score. The {@code score}
     * Text carries a {@code bindRef="<key>.score"} pointing into the
     * section's {@code content} dictionary so Ably linescore writes
     * land on the leaf without walking the tree.
     */
    private AtomicElement teamColumn(GamePanelTeam team, String contentKey) {
        AtomicElement col = container("column", "center", "center");

        List<AtomicElement> children = new ArrayList<>();
        if (team.logoUrl() != null) {
            AtomicElement logo = image(team.logoUrl(), 48, 48, "contain");
            AccessibilityHelper.addImage(logo, team.tricode() + " logo");
            children.add(logo);
            children.add(spacer(tokens.spacing("sm")));
        }
        AtomicElement tri = text(team.tricode(), "titleMedium", "semiBold",
                tokens.color("nba.label.primary"), 1);
        children.add(tri);

        AtomicElement score = text(String.valueOf(team.score()), "score", "bold",
                tokens.color("nba.label.primary"), 1);
        score.setBindRef(contentKey + ".score");
        children.add(score);

        col.setChildren(children);
        return col;
    }

    private AtomicElement statusCell(String gameStatusText, boolean featured) {
        AtomicElement statusText = text(gameStatusText != null ? gameStatusText : "",
                featured ? "titleSmall" : "labelSmall",
                featured ? "semiBold" : "regular",
                tokens.color("nba.label.primary"), 1);
        statusText.setOpacity(0.7);
        statusText.setBindRef("gameStatusText");
        return statusText;
    }

    /**
     * LiveClock leaf for the status slot. Reads its
     * {@code (snapshotSeconds, snapshotAt, isRunning)} tuple from
     * {@code content.clock} so the server can push a single
     * {@code gameClock} object on each tick and all three animation
     * inputs update together.
     */
    private AtomicElement liveClockCell(boolean featured) {
        AtomicElement clock = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("LiveClock"));
        clock.setVariant(featured ? "titleLarge" : "titleMedium");
        clock.setFormat(AtomicElement.Format.fromValue("m:ss"));
        clock.setTickDirection(AtomicElement.TickDirection.fromValue("down"));
        clock.setBindRef("clock");
        clock.setSnapshotSeconds(0);
        clock.setIsRunning(false);
        return clock;
    }

    /**
     * Pre-seeded content dictionary the leaves resolve via
     * {@code bindRef}. Seeded so the first paint shows real values
     * before the first Ably frame lands; subsequent writes from
     * {@code buildCompositeLinescoreBindings} replace these entries
     * in place.
     */
    private Content buildGamePanelContent(String gameStatusText, int gameStatus,
                                             GamePanelTeam homeTeam,
                                             GamePanelTeam awayTeam,
                                             GameClockSnapshot clock) {
        Content content = new Content();
        if (gameStatusText != null) content.setAdditionalProperty("gameStatusText", gameStatusText);
        content.setAdditionalProperty("gameStatus", gameStatus);

        Map<String, Object> home = new LinkedHashMap<>();
        home.put("score", homeTeam.score());
        home.put("tricode", homeTeam.tricode());
        content.setAdditionalProperty("homeTeam", home);

        Map<String, Object> away = new LinkedHashMap<>();
        away.put("score", awayTeam.score());
        away.put("tricode", awayTeam.tricode());
        content.setAdditionalProperty("awayTeam", away);

        if (clock != null) {
            Map<String, Object> clockMap = new LinkedHashMap<>();
            clockMap.put("snapshotSeconds", clock.snapshotSeconds());
            if (clock.snapshotAtIso() != null) clockMap.put("snapshotAt", clock.snapshotAtIso());
            clockMap.put("isRunning", clock.isRunning());
            content.setAdditionalProperty("clock", clockMap);
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
    public Section buildVideoCarousel(String id, String analyticsId,
                                          String title, String subtitle,
                                          String[][] items) {
        Section section = newSection(id, analyticsId);

        AtomicElement root = container("column", null, null);
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement titleEl = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            titleEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("xs")));
            rootChildren.add(titleEl);
        }
        if (subtitle != null) {
            AtomicElement subEl = text(subtitle, "bodySmall", null,
                    tokens.color("nba.label.secondary"), null);
            subEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("xs")));
            rootChildren.add(subEl);
        }

        AtomicElement scroll = scrollContainer("row", tokens.spacing("md"), false);
        List<AtomicElement> scrollChildren = new ArrayList<>();

        for (String[] item : items) {
            scrollChildren.add(buildVideoCard(item[0], item[1], item[2],
                    item[3], item[4], item[5], item[6]));
        }

        scroll.setChildren(scrollChildren);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
        wrapUi(section, root);
        return section;
    }

    private AtomicElement buildVideoCard(String id, String title, String subtitle,
                                        String thumbnailUrl, String duration,
                                        String badgeText, String targetUri) {
        // Plain container (no variant) — see buildContentCard for the
        // rationale. The card's silhouette is defined by its gradient
        // background + corner radius, not a drop shadow.
        AtomicElement card = container("column", null, null);
        card.setId(id);
        // Fix the card's outer width so the 240pt image + full-width
        // meta row (duration / live badge) meet the card edge flush.
        card.setWidth(240);
        card.setCornerRadius(0);
        card.setBackground(gradient(tokens.color("nba.bg.secondary"),
                tokens.color("nba.bg.tertiary"), "vertical"));
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, title);
        }
        List<AtomicElement> children = new ArrayList<>();

        AtomicElement thumbContainer = container("column", null, null);
        List<AtomicElement> thumbChildren = new ArrayList<>();

        if (thumbnailUrl != null) {
            AtomicElement img = thumbnailImage(thumbnailUrl);
            img.setPadding(padding(tokens.spacing("sm"), tokens.spacing("sm"),
                    tokens.spacing("sm"), 0));
            img.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            img.setAspectRatio(16.0 / 9.0);
            AccessibilityHelper.addImage(img, title);
            thumbChildren.add(img);
        }

        AtomicElement metaContainer = container("row", "spaceBetween", "center");
        // Start/end match the image's 8pt inset so the meta row aligns
        // with the image's left/right edges instead of the card's outer
        // edges.
        metaContainer.setPadding(padding(tokens.spacing("sm"), tokens.spacing("sm"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        List<AtomicElement> metaChildren = new ArrayList<>();

        if (badgeText != null) {
            // Pill badge — Container-wrapped text so the background
            // actually renders (Text's own `background` is not honored
            // by any of the three atomic Text renderers). Red brand
            // pill for NEW/LIVE-style callouts.
            metaChildren.add(pillBadgeTyped(badgeText, tokens.color("nba.label.accent.live")));
        } else {
            metaChildren.add(spacer(tokens.spacing("xs")));
        }

        if (duration != null) {
            // Dark translucent pill for durations (matches the duration
            // chip overlaid on video card thumbnails elsewhere).
            metaChildren.add(pillBadgeTyped(duration, "#000000B3"));
        }

        metaContainer.setChildren(metaChildren);
        thumbChildren.add(metaContainer);
        thumbContainer.setChildren(thumbChildren);
        children.add(thumbContainer);

        AtomicElement textCol = container("column", null, null);
        textCol.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("sm"), tokens.spacing("sm")));
        List<AtomicElement> textChildren = new ArrayList<>();
        textChildren.add(text(title, "bodyMedium", "semiBold",
                tokens.color("nba.label.primary"), 2));
        if (subtitle != null) {
            AtomicElement sub = text(subtitle, "bodySmall", null,
                    tokens.color("nba.label.secondary"), 1);
            sub.setPadding(padding(0, 0, tokens.spacing("xs"), 0));
            textChildren.add(sub);
        }
        textCol.setChildren(textChildren);
        children.add(textCol);

        card.setChildren(children);
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
    public Section buildStatLine(String id, String analyticsId,
                                     String title, String layout,
                                     String[][] stats) {
        Section section = newSection(id, analyticsId);

        AtomicElement root = container("column", null, null);
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("sm"), tokens.spacing("sm")));
        root.setGap(tokens.spacing("xs"));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement titleEl = text(title, "titleMedium", "bold", null, null);
            titleEl.setPadding(padding(0, 0, 0, tokens.spacing("sm")));
            rootChildren.add(titleEl);
        }

        boolean isVertical = "vertical".equals(layout);
        for (String[] stat : stats) {
            rootChildren.add(buildStatRow(stat[0], stat[1], stat[2], stat[3], stat[4],
                    stat.length > 5 ? stat[5] : null, isVertical));
        }

        root.setChildren(rootChildren);
        wrapUi(section, root);
        return section;
    }

    /**
     * Overload that accepts pre-built stat ObjectNodes from dynamic API data.
     * Each node should have: playerName, teamTricode, statCategory, statValue, playerImageUrl (optional).
     */
    public Section buildStatLineFromNodes(String id, String analyticsId,
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

    private AtomicElement buildStatRow(String playerId, String playerName, String teamTricode,
                                      String statCategory, String statValue,
                                      String playerImageUrl, boolean isVertical) {
        if (isVertical) {
            return buildStatRowVertical(playerId, playerName, teamTricode,
                    statCategory, statValue, playerImageUrl);
        }
        return buildStatRowHorizontal(playerId, playerName, teamTricode,
                statCategory, statValue, playerImageUrl);
    }

    private AtomicElement buildStatRowHorizontal(String playerId, String playerName,
                                                String teamTricode, String statCategory,
                                                String statValue, String playerImageUrl) {
        return buildCompactStatRow(playerId, playerName, teamTricode, statCategory, statValue, playerImageUrl);
    }

    private AtomicElement buildStatRowVertical(String playerId, String playerName,
                                              String teamTricode, String statCategory,
                                              String statValue, String playerImageUrl) {
        return buildCompactStatRow(playerId, playerName, teamTricode, statCategory, statValue, playerImageUrl);
    }

    /**
     * Single compact row: [headshot] [name / team] [STAT VALUE]
     * Name+team stack vertically in a flex column; stat is right-aligned.
     */
    private AtomicElement buildCompactStatRow(String playerId, String playerName,
                                            String teamTricode, String statCategory,
                                            String statValue, String playerImageUrl) {
        AtomicElement row = container("row", null, "center");
        row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        row.setGap(tokens.spacing("sm"));
        List<AtomicElement> children = new ArrayList<>();

        if (playerImageUrl != null) {
            AtomicElement img = image(playerImageUrl, 32, 32, "cover");
            img.setCornerRadius(tokens.radius("full"));
            AccessibilityHelper.addImage(img, playerName + " headshot");
            children.add(img);
        }

        AtomicElement nameCol = container("column", null, null);
        nameCol.setFlex(1.0);
        List<AtomicElement> nameChildren = new ArrayList<>();
        nameChildren.add(text(playerName, "bodyMedium", "medium", null, null));
        if (teamTricode != null) {
            nameChildren.add(text(teamTricode, "labelSmall", null,
                    tokens.color("nba.label.secondary"), null));
        }
        nameCol.setChildren(nameChildren);
        children.add(nameCol);

        AtomicElement statGroup = container("row", null, "center");
        statGroup.setGap(tokens.spacing("xs"));
        List<AtomicElement> statChildren = new ArrayList<>();
        statChildren.add(text(statCategory, "bodySmall", null,
                tokens.color("nba.label.secondary"), null));
        statChildren.add(text(statValue, "titleSmall", "bold",
                tokens.color("nba.label.accent.live"), null));
        statGroup.setChildren(statChildren);
        children.add(statGroup);

        row.setChildren(children);
        return row;
    }

    // ── NbaTvSchedule ────────────────────────────────────────────────────

    /**
     * Build an NbaTvSchedule as an AtomicComposite.
     * Hero banner with gradient overlay + time-slot list.
     *
     * @param slots  Array of [id, title, subtitle, displayTime, isLive, targetUri]
     */
    public Section buildNbaTvSchedule(String id, String analyticsId,
                                          String heroImageUrl, String heroTitle,
                                          String heroSubtitle, boolean liveNow,
                                          String[][] slots) {
        Section section = newSection(id, analyticsId);

        // Root is content-only. The card chrome (sunken background,
        // rounded corners, horizontal+vertical margin from siblings) is
        // expressed on section.surface so every card-chromed composite
        // shares one surface helper. `widthMode: "fill"` is required for
        // the hero image and slot list to span the card's interior on
        // iOS — without it the VStack sizes to max(child intrinsic).
        AtomicElement root = container("column", null, "start");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(0, 0, 0, tokens.spacing("lg")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        AtomicElement heroContainer = container("column", "end", "start");
        heroContainer.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        heroContainer.setCornerRadius(0);
        List<AtomicElement> heroChildren = new ArrayList<>();

        if (heroImageUrl != null) {
            AtomicElement heroImg = image(heroImageUrl, 0, 200, "cover");
            heroImg.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            AccessibilityHelper.addImage(heroImg, heroTitle != null ? heroTitle : "NBA TV");
            heroChildren.add(heroImg);
        }

        AtomicElement overlay = container("column", null, "start");
        overlay.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        overlay.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("lg"), tokens.spacing("lg")));
        overlay.setBackground(gradient("#00000000", "#000000CC", "vertical"));
        List<AtomicElement> overlayChildren = new ArrayList<>();

        if (liveNow) {
            overlayChildren.add(liveBadge());
            overlayChildren.add(spacer(tokens.spacing("sm")));
        }
        if (heroTitle != null) {
            overlayChildren.add(text(heroTitle, "titleLarge", "bold",
                    tokens.color("nba.label-dark.primary"), null));
        }
        if (heroSubtitle != null) {
            overlayChildren.add(text(heroSubtitle, "bodyMedium", null,
                    tokens.color("nba.label-dark.primary"), null));
        }
        overlay.setChildren(overlayChildren);
        heroChildren.add(overlay);
        heroContainer.setChildren(heroChildren);
        rootChildren.add(heroContainer);

        rootChildren.add(spacer(tokens.spacing("md")));

        // Heading padding is 16pt horizontal — same inset as the slot
        // list below so the title line aligns with the row cards' outer
        // edge. Any padding the surface provides is in addition to this.
        AtomicElement heading = text("Today's Schedule", "titleSmall", "bold",
                tokens.color("nba.label.primary"), null);
        heading.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        AccessibilityHelper.addHeading(heading, "Today's Schedule", 3);
        rootChildren.add(heading);

        AtomicElement slotList = container("column", null, "start");
        slotList.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        slotList.setGap(tokens.spacing("sm"));
        slotList.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("xs"), 0));
        List<AtomicElement> slotChildren = new ArrayList<>();

        for (String[] slot : slots) {
            slotChildren.add(buildNbaTvSlot(slot[0], slot[1], slot[2],
                    slot[3], "true".equals(slot[4]), slot[5]));
        }

        slotList.setChildren(slotChildren);
        rootChildren.add(slotList);
        root.setChildren(rootChildren);
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
    private AtomicElement buildNbaTvSlot(String id, String title, String subtitle,
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
        AtomicElement row = container("row", null, "center");
        row.setId(id);
        row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        row.setGap(tokens.spacing("md"));
        row.setCornerRadius(tokens.radius("md"));
        row.setBackground(tokens.color("nba.bg.primary"));
        row.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("md"), tokens.spacing("md")));
        if (targetUri != null) {
            row.setActions(singleActionArray(tapNavigate(targetUri)));
        }
        List<AtomicElement> children = new ArrayList<>();

        AtomicElement timeText = text(displayTime, "bodyMedium", "semiBold",
                tokens.color("nba.label.secondary"), null);
        children.add(timeText);

        AtomicElement contentCol = container("column", null, "start");
        contentCol.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        contentCol.setFlex(1.0);
        List<AtomicElement> contentChildren = new ArrayList<>();
        contentChildren.add(text(title, "bodyMedium", "semiBold",
                tokens.color("nba.label.primary"), 1));
        if (subtitle != null) {
            contentChildren.add(text(subtitle, "bodySmall", null,
                    tokens.color("nba.label.secondary"), 1));
        }
        contentCol.setChildren(contentChildren);
        children.add(contentCol);

        if (isLive) {
            children.add(liveBadge());
        }

        row.setChildren(children);
        return row;
    }

    // ── Phase 0.4 styling helpers ───────────────────────────────────────

    // ── Multi-background / multi-shadow helpers (Workstream B) ──────────

    /**
     * Create a Shadow value with an explicit {@code type} field.
     *
     * @param type    "drop" (outer) or "inner" (inset)
     * @param color   shadow color (hex with alpha, or token reference)
     * @param offsetX horizontal offset in dp/px
     * @param offsetY vertical offset in dp/px
     * @param radius  blur radius in dp/px
     */
    private Map<String, Object> shadowWithTypeNode(String type, String color, int offsetX, int offsetY, int radius) {
        Map<String, Object> s = new LinkedHashMap<>();
        if (type != null) s.put("type", type);
        if (color != null) s.put("color", color);
        s.put("offsetX", offsetX);
        s.put("offsetY", offsetY);
        s.put("radius", radius);
        return s;
    }

    /** Build a duration badge element (dark background pill with white text). */
    private AtomicElement durationBadgeNode(String duration) {
        AtomicElement bg = container("row", "center", "center");
        bg.setCornerRadius(tokens.radius("sm"));
        bg.setBackground("#000000B3");
        // iOS ref app uses 0.7 for duration pill opacity; normalised here so
        // Android + web match without each platform inventing its own value.
        bg.setOpacity(0.7);
        bg.setPadding(padding(tokens.spacing("xs"), tokens.spacing("xs"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        bg.setChildren(List.of(
                text(duration, "labelSmall", "semiBold",
                        tokens.color("nba.label-inverted.primary"), null)));
        return bg;
    }

    /** Build a "LIVE" badge element (red pill with white text). */
    private AtomicElement liveBadgeNode() {
        AtomicElement bg = container("row", "center", "center");
        bg.setCornerRadius(tokens.radius("sm"));
        bg.setBackground(tokens.color("nba.label.accent.live"));
        bg.setPadding(padding(tokens.spacing("xs"), tokens.spacing("xs"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        bg.setChildren(List.of(
                text("LIVE", "labelSmall", "bold",
                        tokens.color("nba.label-inverted.primary"), null)));
        return bg;
    }

    // ── Atomic element helpers ──────────────────────────────────────────

    /**
     * Public convenience: wrap a root element as a full AtomicComposite section.
     */
    public Section wrapAsComposite(String sectionId, String analyticsId,
                                   AtomicElement rootElement) {
        Section section = newSection(sectionId, analyticsId);
        if (rootElement != null) wrapUi(section, rootElement);
        return section;
    }

    /**
     * Build a vertical VOD playlist composite — a grouped rounded surface
     * hosting a list of VOD rows (thumbnail, title, subtitle, chevron)
     * separated by dividers.
     *
     * <p>Shape mirrors the iOS ref app's {@code VODPlaylistView}: one
     * surface Container, optional header, N rows, N-1 inline dividers.
     * Each row is wrapped in {@code singleActionArrayNode(tapNavigateNode(uri))}.
     *
     * @param sectionId    section id for the enclosing AtomicComposite
     * @param analyticsId  analyticsId for the enclosing AtomicComposite
     * @param header       optional header text; pass {@code null} to omit
     * @param rows         ordered list of row specs: {@code [id, title,
     *                     subtitle-or-null, thumbnailUrl, durationLabel-or-null,
     *                     isLive ("true"/"false"), targetUri]}
     */
    public Section buildVodPlaylist(String sectionId, String analyticsId,
                                       String header, String[][] rows) {
        AtomicElement surface = groupedContainer("column", null, null);
        surface.setCornerRadius(tokens.radius("lg"));
        surface.setPadding(padding(0, 0, tokens.spacing("xs"), tokens.spacing("xs")));

        List<AtomicElement> surfaceChildren = new ArrayList<>();
        if (header != null) {
            AtomicElement headerEl = text(header, "titleSmall", "semiBold", null, null);
            headerEl.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("md"), tokens.spacing("sm")));
            AccessibilityHelper.addHeading(headerEl, header, 3);
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
        surface.setChildren(surfaceChildren);

        AtomicElement wrapper = container("column", null, null);
        wrapper.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
        List<AtomicElement> wrapperKids = new ArrayList<>();
        wrapperKids.add(surface);
        wrapper.setChildren(wrapperKids);

        return wrapAsComposite(sectionId, analyticsId, wrapper);
    }

    private AtomicElement buildVodRow(String id, String title, String subtitle,
                                   String thumbnailUrl, String duration,
                                   boolean isLive, String targetUri) {
        AtomicElement row = container("row", null, "center");
        row.setId(id);
        row.setGap(tokens.spacing("md"));
        row.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("sm"), tokens.spacing("sm")));
        if (targetUri != null) {
            row.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(row, title);
        }

        List<AtomicElement> children = new ArrayList<>();
        if (thumbnailUrl != null) {
            AtomicElement thumb = thumbnailImage(thumbnailUrl);
            thumb.setWidth(80);
            thumb.setHeight(52);
            thumb.setCornerRadius(tokens.radius("sm"));
            AccessibilityHelper.addImage(thumb, title);
            if (isLive) {
                badge(thumb, liveBadge(), "topStart");
            } else if (duration != null) {
                badge(thumb, durationBadge(duration), "bottomEnd");
            }
            children.add(thumb);
        }

        AtomicElement titleCol = container("column", null, "start");
        titleCol.setGap(tokens.spacing("xs"));
        setFlex(titleCol, 1.0);
        List<AtomicElement> titleKids = new ArrayList<>();
        titleKids.add(text(title, "bodyMedium", "semiBold", null, 2));
        if (subtitle != null && !subtitle.isEmpty()) {
            titleKids.add(text(subtitle, "labelSmall", null,
                    tokens.color("nba.label.secondary"), 1));
        }
        titleCol.setChildren(titleKids);
        children.add(titleCol);

        // Trailing chevron — semantic icon token; each client resolves to its native glyph.
        AtomicElement chevron = new AtomicElement().withType(AtomicElement.Type.fromValue("Text"));
        chevron.setContent("›");
        chevron.setVariant("titleMedium");
        chevron.setColor(tokens.color("nba.label.secondary"));
        children.add(chevron);

        row.setChildren(children);
        return row;
    }

    private AtomicElement vodDivider() {
        AtomicElement d = new AtomicElement().withType(AtomicElement.Type.fromValue("Divider"));
        d.setThickness(1);
        // Inset-left styling matches the iOS grouped-list idiom (divider starts
        // where text begins, not where thumbnail begins). Clients that can't
        // honour leading padding on a Divider render it edge-to-edge.
        d.setPadding(padding(104, 0, 0, 0));
        return d;
    }

    // ── Real-app feed atomic patterns ───────────────────────────────────

    /**
     * Horizontal story rail composed from server-provided story data.
     * items: [id, label, imageUrl, badgeText, targetUri]
     */
    public Section buildStoryCircleRail(String sectionId, String analyticsId,
                                           String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement header = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            header.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("sm")));
            rootChildren.add(header);
        }

        AtomicElement scroll = scrollContainer("row", 14, false);
        scroll.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
        List<AtomicElement> children = new ArrayList<>();
        for (String[] item : items) {
            if (!hasRequiredValues(item, 0, 1)) continue;
            children.add(storyCircleItem(value(item, 0), value(item, 1), value(item, 2),
                    value(item, 3), value(item, 4)));
        }
        scroll.setChildren(children);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
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
    public Section buildCinematicHeroCarousel(String sectionId, String analyticsId,
                                                  String[][] slides) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(slides, "slides");

        AtomicElement root = container("column", null, null);
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        List<AtomicElement> rootChildren = new ArrayList<>();

        boolean multiSlide = slides.length > 1;
        List<AtomicElement> scrollChildren = new ArrayList<>();

        for (String[] slide : slides) {
            if (!hasRequiredValues(slide, 0, 1, 3)) continue;
            scrollChildren.add(cinematicHeroSlide(
                    value(slide, 0), value(slide, 1), value(slide, 2),
                    value(slide, 3), value(slide, 4), value(slide, 5),
                    value(slide, 6)));
        }

        if (multiSlide) {
            AtomicElement scroll = scrollContainer("row", 0, false);
            scroll.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            Map<String, Object> indicator = new LinkedHashMap<>();
            indicator.put("style", "dashes");
            indicator.put("alignment", "bottomCenter");
            indicator.put("color", "#FFFFFF66");
            indicator.put("activeColor", "#FFFFFFFF");
            // paging/snapAlignment/pageIndicator currently flow as additional props
            // (ScrollContainer schema doesn't expose them as typed fields yet).
            scroll.setAdditionalProperty("paging", true);
            scroll.setAdditionalProperty("snapAlignment", "center");
            scroll.setAdditionalProperty("pageIndicator", indicator);
            scroll.setChildren(scrollChildren);
            rootChildren.add(scroll);
        } else if (!scrollChildren.isEmpty()) {
            rootChildren.add(scrollChildren.get(0));
        }

        root.setChildren(rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    private AtomicElement cinematicHeroSlide(String id, String imageUrl, String badgeText,
                                            String title, String subtitle,
                                            String ctaLabel, String targetUri) {
        // Outer wrapper — no corner radius (full-bleed)
        AtomicElement slide = container("column", null, "stretch");
        slide.setId(id);
        slide.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));

        if (targetUri != null) {
            slide.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(slide, title);
        }

        // Background image fills full width, 16:9 aspect
        AtomicElement bgImage = image(imageUrl, 0, 0, "cover");
        bgImage.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        bgImage.setAspectRatio(16.0 / 9.0);
        AccessibilityHelper.addImage(bgImage, title);

        // Scrim overlay fills the full image height so the gradient scales
        // proportionally on any viewport. Content pushes to the bottom via
        // alignment="end". No fixed top padding — the gradient itself provides
        // the visual fade from transparent at top to dark at bottom.
        AtomicElement scrimOverlay = container("column", "end", "start");
        scrimOverlay.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimOverlay.setHeightMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimOverlay.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                0, tokens.spacing("xl")));
        scrimOverlay.setBackground(gradient("#00000000", "#000000E6", "vertical"));
        List<AtomicElement> scrimChildren = new ArrayList<>();

        if (badgeText != null) {
            scrimChildren.add(pillBadgeTyped(badgeText, tokens.color("nba.label.accent.live")));
            scrimChildren.add(spacer(tokens.spacing("md")));
        }
        scrimChildren.add(text(title, "headlineSmall", "bold",
                tokens.color("nba.label-dark.primary"), 2));
        if (subtitle != null) {
            scrimChildren.add(spacer(tokens.spacing("sm")));
            scrimChildren.add(text(subtitle, "bodyMedium", null,
                    tokens.color("nba.label-dark.primary"), 2));
        }
        if (ctaLabel != null) {
            scrimChildren.add(spacer(tokens.spacing("md")));
            AtomicElement cta = bindElement(buttonNode(ctaLabel, "secondary",
                    targetUri != null ? tapNavigateNode(targetUri) : null));
            cta.setColor(tokens.color("nba.label-dark.primary"));
            cta.setBackground("#00000000");
            scrimChildren.add(cta);
        }
        scrimOverlay.setChildren(scrimChildren);

        // Compose using OverlayContainer: image base + bottom scrim overlay
        slide.setChildren(List.of(
                overlayContainer(bgImage, List.of(overlay("bottomStart", null, scrimOverlay)))));
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
    public Section buildOverlayStoryRail(String sectionId, String analyticsId,
                                             String title, String[][] cards) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement header = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            header.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("sm")));
            rootChildren.add(header);
        }

        AtomicElement scroll = scrollContainer("row", 14, false);
        scroll.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
        List<AtomicElement> children = new ArrayList<>();
        for (String[] card : cards) {
            if (!hasRequiredValues(card, 0, 1)) continue;
            children.add(overlayStoryCard(value(card, 0), value(card, 1),
                    value(card, 2), value(card, 3), value(card, 4)));
        }
        scroll.setChildren(children);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    private AtomicElement overlayStoryCard(String id, String title, String imageUrl,
                                          String badgeText, String targetUri) {
        String radius = tokens.radius("md");
        AtomicElement card = container("column", null, null);
        card.setId(id);
        card.setWidth(180);
        card.setCornerRadius(radius);
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, title);
        }

        // Full-bleed portrait image as base (3:4 aspect ratio)
        AtomicElement base = imageUrl != null
                ? image(imageUrl, 180, 0, "cover")
                : neutralInitialsRect(title, 180, 240, radius);
        base.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        base.setAspectRatio(3.0 / 4.0);
        base.setCornerRadius(radius);
        if (imageUrl != null) AccessibilityHelper.addImage(base, title);

        // Bottom scrim overlay with title text — dark black gradient for readability
        AtomicElement scrimContent = container("column", "end", "start");
        scrimContent.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimContent.setHeightMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimContent.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                0, tokens.spacing("md")));
        scrimContent.setBackground(gradient("#00000000", "#000000CC", "vertical"));
        scrimContent.setCornerRadii(cornerRadiiOf(0, 0, radius, radius));
        scrimContent.setChildren(List.of(
                text(title, "titleSmall", "bold",
                        tokens.color("nba.label-inverted.primary"), 3)));

        // Top-left "NEW" badge overlay (if present)
        List<AtomicOverlay> overlays = new ArrayList<>();
        overlays.add(overlay("bottomStart", null, scrimContent));
        if (badgeText != null) {
            AtomicElement badgePill = pillBadgeTyped(badgeText, tokens.color("nba.label.accent.live"));
            Spacing badgeInset = padding(tokens.spacing("sm"), 0, tokens.spacing("sm"), 0);
            overlays.add(overlay("topStart", badgeInset, badgePill));
        }

        card.setChildren(List.of(overlayContainer(base, overlays)));
        return card;
    }

    /**
     * Horizontal rail of tall editorial image cards with server-declared
     * scrim/text overlays.
     * cards: [id, title, subtitle, imageUrl, badgeText, targetUri]
     */
    public Section buildEditorialOverlayRail(String sectionId, String analyticsId,
                                                String title, String[][] cards) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();

        if (title != null) {
            AtomicElement header = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            header.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("sm")));
            rootChildren.add(header);
        }

        AtomicElement scroll = scrollContainer("row", 14, false);
        scroll.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
        List<AtomicElement> children = new ArrayList<>();
        for (String[] card : cards) {
            if (!hasRequiredValues(card, 0, 1)) continue;
            children.add(editorialOverlayCard(value(card, 0), value(card, 1), value(card, 2),
                    value(card, 3), value(card, 4), value(card, 5)));
        }
        scroll.setChildren(children);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
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
    public Section buildFeaturedLiveGameHero(String sectionId, String analyticsId,
                                                String title, String[][] cards) {
        return buildFeaturedLiveGameHero(sectionId, analyticsId, title, cards, null, null);
    }

    public Section buildFeaturedLiveGameHero(String sectionId, String analyticsId,
                                                String title, String[][] cards,
                                                RefreshPolicy refreshPolicy,
                                                DataBinding dataBinding) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(cards, "cards");

        List<String[]> validCards = validRows(cards, 0, 2, 5, 8);

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();
        if (title != null) {
            AtomicElement header = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            header.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("sm")));
            rootChildren.add(header);
        }

        boolean singleCard = validCards.size() == 1;
        List<AtomicElement> scrollChildren = new ArrayList<>();
        Map<String, Object> cardsContent = new LinkedHashMap<>();
        for (String[] card : validCards) {
            String cardId = value(card, 0);
            scrollChildren.add(featuredLiveGameHeroCard(card, singleCard));
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("awayScore", parseInt(value(card, 6), 0));
            state.put("homeScore", parseInt(value(card, 9), 0));
            if (value(card, 11) != null) state.put("statusText", value(card, 11));
            cardsContent.put(cardId, state);
        }

        // Single-card hero: emit a flush container so the card fills its
        // surface end-to-end. Multi-card hero: paged horizontal scroll
        // with peeking edges and a dot indicator.
        if (singleCard) {
            AtomicElement wrapper = container("column", null, "stretch");
            wrapper.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
            wrapper.setChildren(new ArrayList<>(scrollChildren));
            rootChildren.add(wrapper);
        } else {
            AtomicElement scroll = pagedHorizontalScrollTyped(tokens.spacing("md"),
                    validCards.size(),
                    padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0),
                    "bottomCenter", null, null);
            scroll.setChildren(scrollChildren);
            rootChildren.add(scroll);
        }
        root.setChildren(rootChildren);

        Section section = newSection(sectionId, analyticsId, refreshPolicy);
        AtomicComposite data = new AtomicComposite();
        data.setUi(root);
        Content typedContent = new Content();
        typedContent.setAdditionalProperty("cards", cardsContent);
        data.setContent(typedContent);
        section.setData(data);
        if (dataBinding != null) {
            section.setDataBinding(dataBinding);
        }
        return section;
    }

    public Section buildSectionHeaderComposite(String sectionId, String analyticsId,
                                                  String title, String subtitle,
                                                  String actionLabel, String actionUri) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(title, "title");

        AtomicElement root = container("row", "spaceBetween", "center");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("md"), tokens.spacing("xs")));

        List<AtomicElement> children = new ArrayList<>();
        AtomicElement titleCol = container("column", null, "start");
        List<AtomicElement> titleChildren = new ArrayList<>();
        AtomicElement titleEl = text(title.toUpperCase(Locale.ROOT), "titleMedium", "bold",
                tokens.color("nba.label.primary"), null);
        AccessibilityHelper.addHeading(titleEl, title, 2);
        titleChildren.add(titleEl);
        if (subtitle != null) {
            titleChildren.add(text(subtitle, "bodySmall", null,
                    tokens.color("nba.label.tertiary"), 1));
        }
        titleCol.setChildren(titleChildren);
        children.add(titleCol);

        if (actionUri != null) {
            AtomicElement more = button(actionLabel != null ? actionLabel + " >" : "More >",
                    "text", tapNavigate(actionUri));
            more.setColor(tokens.color("nba.label.accent.brand"));
            children.add(more);
        }

        root.setChildren(children);
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
    public Section buildAppBarHeaderComposite(String sectionId, String analyticsId,
                                                 String title, String backUri) {
        requireNonBlank(sectionId, "sectionId");

        AtomicElement root = container("row", "start", "center");
        root.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        root.setPadding(padding(
                tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("sm"), tokens.spacing("xs")));

        List<AtomicElement> children = new ArrayList<>();
        if (backUri != null && !backUri.isBlank()) {
            AtomicElement back = appBarIconButton(tokens.icon("back"), tapNavigate(backUri));
            AccessibilityHelper.addButton(back, "Back");
            children.add(back);
            children.add(hSpacer(tokens.spacing("sm")));
        }
        if (title != null && !title.isBlank()) {
            AtomicElement titleEl = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            AccessibilityHelper.addHeading(titleEl, title, 1);
            children.add(titleEl);
        }

        root.setChildren(children);
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
    public Section buildVariantChipsComposite(String sectionId, String analyticsId,
                                                  String currentUri, String experimentId,
                                                  List<ExperimentVariantOption> options,
                                                  String activeVariantId) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(currentUri, "currentUri");
        requireNonBlank(experimentId, "experimentId");

        AtomicElement scroll = scrollContainer("row", 8, false);
        scroll.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        scroll.setPadding(padding(
                tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("xs"), tokens.spacing("xs")));

        List<AtomicElement> chips = new ArrayList<>();
        boolean anyMatch = false;
        for (ExperimentVariantOption option : options) {
            String id = option.getId();
            if (id == null || id.isBlank()) continue;
            if (id.equals(activeVariantId)) anyMatch = true;
        }
        for (int i = 0; i < options.size(); i++) {
            ExperimentVariantOption option = options.get(i);
            String id = option.getId();
            String label = option.getLabel() != null ? option.getLabel() : id;
            if (id == null || id.isBlank()) continue;

            boolean isSelected = id.equals(activeVariantId)
                    || (!anyMatch && i == 0);

            String targetUri = withReplacedExperimentParam(currentUri, experimentId, id);
            AtomicElement chip = button(label, isSelected ? "primary" : "secondary",
                    tapNavigate(targetUri));
            AccessibilityHelper.addButton(chip, label + (isSelected ? " (selected)" : ""));
            chips.add(chip);
        }
        scroll.setChildren(chips);

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
    public Section buildUtilityCardGrid(String sectionId, String analyticsId,
                                           String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        List<String[]> validItems = validRows(items, 0, 1);

        AtomicElement root = container("column", null, "stretch");
        root.setGap(tokens.spacing("md"));
        root.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, tokens.spacing("lg")));
        List<AtomicElement> children = new ArrayList<>();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold", tokens.color("nba.label.primary"), null));
        }

        for (int i = 0; i < validItems.size(); i += 2) {
            AtomicElement row = container("row", null, "stretch");
            row.setGap(tokens.spacing("md"));
            row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            List<AtomicElement> rowChildren = new ArrayList<>();
            AtomicElement first = utilityCard(validItems.get(i));
            setFlex(first, 1.0);
            rowChildren.add(first);
            if (i + 1 < validItems.size()) {
                AtomicElement second = utilityCard(validItems.get(i + 1));
                setFlex(second, 1.0);
                rowChildren.add(second);
            } else {
                AtomicElement filler = container("column", null, null);
                setFlex(filler, 1.0);
                rowChildren.add(filler);
            }
            row.setChildren(rowChildren);
            children.add(row);
        }

        root.setChildren(children);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /**
     * Horizontal rail of league destination cards.
     * items: [id, label, imageUrl, targetUri]
     */
    public Section buildLeagueCardRail(String sectionId, String analyticsId,
                                          String title, String[][] items) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(items, "items");

        AtomicElement root = container("column", null, null);
        root.setPadding(padding(0, 0, 0, tokens.spacing("md")));
        List<AtomicElement> rootChildren = new ArrayList<>();
        if (title != null) {
            AtomicElement header = text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null);
            header.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    tokens.spacing("xs"), tokens.spacing("sm")));
            rootChildren.add(header);
        }

        AtomicElement scroll = scrollContainer("row", tokens.spacing("md"), false);
        scroll.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
        List<AtomicElement> children = new ArrayList<>();
        for (String[] item : items) {
            if (!hasRequiredValues(item, 0, 1)) continue;
            children.add(leagueCard(value(item, 0), value(item, 1), value(item, 2), value(item, 3)));
        }
        scroll.setChildren(children);
        rootChildren.add(scroll);
        root.setChildren(rootChildren);
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
    public Section buildGameScheduleRow(String sectionId, String analyticsId, String[] row) {
        return buildGameScheduleRow(sectionId, analyticsId, row, null, null);
    }

    public Section buildGameScheduleRow(String sectionId, String analyticsId, String[] row,
                                           RefreshPolicy refreshPolicy,
                                           DataBinding dataBinding) {
        requireNonBlank(sectionId, "sectionId");
        requireRow(row, "row");
        requireRequiredValues(row, "row", 0, 1, 6);

        AtomicElement card = gameScheduleRowElement(row);
        AtomicElement wrapper = container("column", null, null);
        wrapper.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        List<AtomicElement> children = new ArrayList<>();
        children.add(card);
        wrapper.setChildren(children);

        Section section = newSection(sectionId, analyticsId, refreshPolicy);
        AtomicComposite composite = new AtomicComposite();
        composite.setUi(wrapper);
        Content content = new Content();
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("awayScore", parseInt(value(row, 4), 0));
        state.put("homeScore", parseInt(value(row, 9), 0));
        if (value(row, 11) != null) state.put("statusText", value(row, 11));
        content.setAdditionalProperty(value(row, 0), state);
        composite.setContent(content);
        section.setData(composite);
        if (dataBinding != null) section.setDataBinding(dataBinding);
        return section;
    }

    public Section buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows) {
        return buildGameScheduleList(sectionId, analyticsId, title, rows, null, null);
    }

    public Section buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows,
                                            RefreshPolicy refreshPolicy,
                                            DataBinding dataBinding) {
        return buildGameScheduleList(sectionId, analyticsId, title, rows, refreshPolicy, dataBinding, null);
    }

    /**
     * Build a GameScheduleList composite with optional per-row live clock snapshots.
     * When a row's ID appears in {@code clockSnapshots}, the status column renders a
     * {@code LiveClock} element that counts down via the SSE data binding channel.
     *
     * @param clockSnapshots map from rowId to clock snapshot (null for no live clocks)
     */
    public Section buildGameScheduleList(String sectionId, String analyticsId,
                                            String title, String[][] rows,
                                            RefreshPolicy refreshPolicy,
                                            DataBinding dataBinding,
                                            java.util.Map<String, GameClockSnapshot> clockSnapshots) {
        requireNonBlank(sectionId, "sectionId");
        requireRows(rows, "rows");

        List<String[]> validRows = validRows(rows, 0, 1, 6);

        AtomicElement root = container("column", null, "stretch");
        root.setGap(tokens.spacing("md"));
        root.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"), 0, tokens.spacing("md")));
        List<AtomicElement> children = new ArrayList<>();
        if (title != null) {
            children.add(text(title, "titleMedium", "bold",
                    tokens.color("nba.label.primary"), null));
        }
        Content content = new Content();
        for (String[] row : validRows) {
            String rowId = value(row, 0);
            GameClockSnapshot clock = clockSnapshots != null ? clockSnapshots.get(rowId) : null;
            children.add(gameScheduleRowElement(row, clock));
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("awayScore", parseInt(value(row, 4), 0));
            state.put("homeScore", parseInt(value(row, 9), 0));
            if (value(row, 11) != null) state.put("statusText", value(row, 11));
            if (clock != null) {
                Map<String, Object> clockState = new LinkedHashMap<>();
                clockState.put("snapshotSeconds", clock.snapshotSeconds());
                clockState.put("snapshotAt", clock.snapshotAtIso());
                clockState.put("isRunning", clock.isRunning());
                state.put("clock", clockState);
            }
            content.setAdditionalProperty(rowId, state);
        }
        root.setChildren(children);

        Section section = newSection(sectionId, analyticsId, refreshPolicy);
        AtomicComposite composite = new AtomicComposite();
        composite.setUi(root);
        composite.setContent(content);
        section.setData(composite);
        if (dataBinding != null) section.setDataBinding(dataBinding);
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
    public Section buildMediaOverlayCard(String sectionId, String analyticsId,
                                            String imageUrl, String title, String subtitle,
                                            String ctaLabel, String ctaTargetUri,
                                            String topStartBadgeText,
                                            String shareActionUri, String audioActionUri) {
        requireNonBlank(sectionId, "sectionId");
        requireNonBlank(imageUrl, "imageUrl");
        requireNonBlank(title, "title");

        String radius = tokens.radius("md");
        AtomicElement base = image(imageUrl, 0, 0, "cover");
        base.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        base.setAspectRatio(16.0 / 9.0);
        base.setCornerRadius(radius);
        AccessibilityHelper.addImage(base, title);

        List<AtomicOverlay> layers = new ArrayList<>();

        AtomicElement copyCol = container("column", "end", "start");
        copyCol.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        copyCol.setHeightMode(AtomicElement.SizingMode.fromValue("fill"));
        copyCol.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("lg"), tokens.spacing("lg")));
        copyCol.setBackground(mediaBottomScrimGradient());
        copyCol.setCornerRadii(cornerRadiiOf(0, 0, radius, radius));
        List<AtomicElement> copyChildren = new ArrayList<>();
        copyChildren.add(text(title, "titleMedium", "bold",
                tokens.color("nba.label-dark.primary"), 3));
        if (subtitle != null) {
            copyChildren.add(text(subtitle, "bodySmall", null,
                    tokens.color("nba.label-dark.primary"), 3));
        }
        if (ctaLabel != null && ctaTargetUri != null) {
            AtomicElement cta = button(ctaLabel, "secondary", tapNavigate(ctaTargetUri));
            cta.setColor(tokens.color("nba.label-dark.primary"));
            cta.setBackground("#00000000");
            copyChildren.add(spacer(tokens.spacing("md")));
            copyChildren.add(cta);
        }
        copyCol.setChildren(copyChildren);
        layers.add(overlay("bottomStart", null, copyCol));

        if (topStartBadgeText != null) {
            String t = topStartBadgeText.trim();
            AtomicElement pill = t.equalsIgnoreCase("LIVE")
                    ? liveBadge()
                    : pillBadgeTyped(topStartBadgeText, tokens.color("nba.label.accent.brand"));
            layers.add(overlay("topStart",
                    padding(tokens.spacing("md"), tokens.spacing("md"), 0, 0), pill));
        }

        if (shareActionUri != null || audioActionUri != null) {
            AtomicElement iconRow = container("row", "end", "center");
            iconRow.setGap(tokens.spacing("sm"));
            List<AtomicElement> iconKids = new ArrayList<>();
            if (audioActionUri != null) {
                iconKids.add(mediaOverlayIconButton(tokens.icon("video"), tapNavigate(audioActionUri)));
            }
            if (shareActionUri != null) {
                iconKids.add(mediaOverlayIconButton(tokens.icon("share"), tapNavigate(shareActionUri)));
            }
            iconRow.setChildren(iconKids);
            layers.add(overlay("topEnd",
                    padding(tokens.spacing("md"), tokens.spacing("md"), 0, 0), iconRow));
        }

        AtomicElement root = overlayContainer(base, layers);
        return wrapAsComposite(sectionId, analyticsId, root);
    }

    /** Icon-only control on light app-bar surfaces (back affordance). */
    private AtomicElement appBarIconButton(String iconToken, Action action) {
        AtomicElement b = new AtomicElement().withType(AtomicElement.Type.fromValue("Button"));
        b.setLabel("");
        b.setIcon(iconToken);
        b.setVariant("text");
        b.setColor(tokens.color("nba.label.primary"));
        b.setActions(singleActionArray(action));
        return b;
    }

    /** Icon-only control on dark media scrims (video hero overlays). */
    private AtomicElement mediaOverlayIconButton(String iconToken, Action action) {
        AtomicElement b = new AtomicElement().withType(AtomicElement.Type.fromValue("Button"));
        b.setLabel("");
        b.setIcon(iconToken);
        b.setVariant("text");
        b.setColor(tokens.color("nba.label-dark.primary"));
        b.setActions(singleActionArray(action));
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

    private AtomicElement storyCircleItem(String id, String label, String imageUrl,
                                          String badgeText, String targetUri) {
        AtomicElement item = container("column", "center", "center");
        item.setId(id);
        item.setWidth(82);
        if (targetUri != null) {
            item.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(item, label);
        }
        List<AtomicElement> children = new ArrayList<>();

        int inner = 70;
        // Story-rail convention: every avatar wears a red ring regardless of
        // whether it also carries a notification badge ("LIVE", "NEW", …).
        // The ring is the rail-wide visual signature (matches the real NBA
        // app's Following rail and our parity plan's "red circle around the
        // images in the top rail" target). The badge is an independent
        // overlay layered on the bottom-center of the avatar.
        int ring = 3;
        AtomicElement avatar = imageUrl != null
                ? image(imageUrl, inner, inner, "cover")
                : neutralInitials(label, inner, inner / 2);
        avatar.setCornerRadius(inner / 2);
        // Defense in depth: opaque inner fill so a missing image doesn't
        // reveal the BRAND_LIVE ring color through to the whole disc.
        avatar.setBackground(tokens.color("nba.bg.tertiary"));
        if (badgeText != null) {
            avatar = overlayContainer(avatar, List.of(overlay("bottomCenter",
                    padding(0, 0, 0, 0),
                    pillBadgeTyped(badgeText, tokens.color("nba.label.accent.live")))));
        }
        AtomicElement ringWrap = container("row", "center", "center");
        ringWrap.setWidth(inner + ring * 2);
        ringWrap.setHeight(inner + ring * 2);
        ringWrap.setCornerRadius((inner + ring * 2) / 2);
        ringWrap.setBackground(tokens.color("nba.label.accent.live"));
        ringWrap.setPadding(padding(ring, ring, ring, ring));
        ringWrap.setChildren(List.of(avatar));
        children.add(ringWrap);
        children.add(spacer(tokens.spacing("sm")));
        children.add(text(label, "labelSmall", null, tokens.color("nba.label.primary"), 1));
        item.setChildren(children);
        return item;
    }

    private AtomicElement editorialOverlayCard(String id, String title, String subtitle,
                                              String imageUrl, String badgeText, String targetUri) {
        String radius = tokens.radius("md");
        AtomicElement card = container("column", null, null);
        card.setId(id);
        card.setWidth(200);
        card.setCornerRadius(radius);
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, title);
        }

        AtomicElement base = imageUrl != null
                ? image(imageUrl, 200, 0, "cover")
                : neutralInitialsRect(title, 200, 268, radius);
        base.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        base.setAspectRatio(3.0 / 4.0);
        base.setCornerRadius(radius);
        if (imageUrl != null) AccessibilityHelper.addImage(base, title);

        AtomicElement scrimContent = container("column", "end", "start");
        scrimContent.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimContent.setHeightMode(AtomicElement.SizingMode.fromValue("fill"));
        scrimContent.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("md"), tokens.spacing("md")));
        scrimContent.setBackground(mediaBottomScrimGradient());
        scrimContent.setCornerRadii(cornerRadiiOf(0, 0, radius, radius));
        List<AtomicElement> children = new ArrayList<>();
        if (badgeText != null) {
            children.add(pillBadgeTyped(badgeText, tokens.color("nba.label.accent.live")));
            children.add(spacer(tokens.spacing("sm")));
        }
        children.add(text(title, "titleSmall", "bold",
                tokens.color("nba.label-dark.primary"), 3));
        if (subtitle != null) {
            children.add(text(subtitle, "bodySmall", null,
                    tokens.color("nba.label-dark.primary"), 3));
        }
        scrimContent.setChildren(children);

        card.setChildren(List.of(
                overlayContainer(base, List.of(overlay("bottomStart", null, scrimContent)))));
        return card;
    }

    /** Top-right overflow control on hero key art; action must be server-declared. */
    private AtomicElement heroOverflowButton(Action navigateAction) {
        AtomicElement b = new AtomicElement().withType(AtomicElement.Type.fromValue("Button"));
        b.setLabel("⋯");
        b.setVariant("text");
        b.setColor(tokens.color("nba.label-dark.primary"));
        b.setActions(singleActionArray(navigateAction));
        return b;
    }

    private AtomicElement featuredLiveGameHeroCard(String[] card, boolean stretchToParentWidth) {
        String cardId = value(card, 0);
        int heroRadius = 0;
        AtomicElement hero = container("column", null, "stretch");
        hero.setId(cardId);
        // Single-card section: card fills its surface (no fixed width).
        // Multi-card paged carousel: cards snap at a fixed width so a
        // peek of the next card is visible at the edge.
        if (stretchToParentWidth) {
            hero.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        } else {
            hero.setWidth(338);
        }
        hero.setCornerRadius(heroRadius);
        hero.setBackground(tokens.color("nba.bg.secondary"));
        shadow(hero);
        if (value(card, 14) != null) hero.setActions(singleActionArray(tapNavigate(value(card, 14))));
        AccessibilityHelper.addButton(hero, value(card, 2));

        AtomicElement art = value(card, 4) != null
                ? image(value(card, 4), 0, 0, "cover")
                : neutralInitialsRect(value(card, 2), 338, 190, heroRadius);
        art.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        art.setAspectRatio(16.0 / 9.0);
        if (value(card, 4) != null) AccessibilityHelper.addImage(art, value(card, 2));

        AtomicElement titleOverlay = container("column", "end", "start");
        titleOverlay.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        titleOverlay.setHeightMode(AtomicElement.SizingMode.fromValue("fill"));
        titleOverlay.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("md"), tokens.spacing("md")));
        titleOverlay.setBackground(mediaBottomScrimGradient());
        List<AtomicElement> overlayChildren = new ArrayList<>();
        if (value(card, 1) != null) {
            overlayChildren.add(pillBadgeTyped(value(card, 1), tokens.color("nba.label.accent.live")));
            overlayChildren.add(spacer(tokens.spacing("sm")));
        }
        overlayChildren.add(text(value(card, 2), "titleMedium", "bold",
                tokens.color("nba.label-dark.primary"), 2));
        if (value(card, 3) != null) {
            overlayChildren.add(text(value(card, 3), "bodySmall", null,
                    tokens.color("nba.label-dark.primary"), 2));
        }
        titleOverlay.setChildren(overlayChildren);

        List<AtomicOverlay> artOverlays = new ArrayList<>();
        artOverlays.add(overlay("bottomStart", null, titleOverlay));
        if (value(card, 15) != null) {
            artOverlays.add(overlay("topEnd",
                    padding(tokens.spacing("sm"), tokens.spacing("sm"), 0, 0),
                    heroOverflowButton(tapNavigate(value(card, 15)))));
        }

        List<AtomicElement> heroChildren = new ArrayList<>();
        heroChildren.add(overlayContainer(art, artOverlays));
        heroChildren.add(heroScoreStrip(card));
        List<AtomicElement> sponsorLogos = logoRowTyped(value(card, 13), 48, 20);
        if (!sponsorLogos.isEmpty()) {
            heroChildren.add(cardHairlineDivider());
            AtomicElement sponsors = container("row", "end", "center");
            sponsors.setGap(tokens.spacing("sm"));
            sponsors.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                    0, tokens.spacing("md")));
            sponsors.setChildren(sponsorLogos);
            heroChildren.add(sponsors);
        }
        hero.setChildren(heroChildren);
        return hero;
    }

    private AtomicElement heroScoreStrip(String[] card) {
        AtomicElement row = container("row", "spaceBetween", "center");
        row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        row.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("md"), tokens.spacing("sm")));
        List<AtomicElement> children = new ArrayList<>();
        children.add(heroTeam(value(card, 5), value(card, 6), value(card, 7),
                "cards." + value(card, 0) + ".awayScore", false));

        AtomicElement center = container("column", "center", "center");
        center.setWidth(88);
        List<AtomicElement> centerChildren = new ArrayList<>();
        boolean liveCue = value(card, 1) != null;
        if (liveCue) {
            AtomicElement statusLine = container("row", "center", "center");
            statusLine.setGap(tokens.spacing("xs"));
            List<AtomicElement> slKids = new ArrayList<>();
            slKids.add(liveStatusDot());
            AtomicElement status = text(value(card, 11), "labelSmall", "semiBold",
                    tokens.color("nba.label.secondary"), 1);
            status.setBindRef("cards." + value(card, 0) + ".statusText");
            slKids.add(status);
            statusLine.setChildren(slKids);
            centerChildren.add(statusLine);
        } else {
            AtomicElement status = text(value(card, 11), "labelSmall", "semiBold",
                    tokens.color("nba.label.secondary"), 1);
            status.setBindRef("cards." + value(card, 0) + ".statusText");
            centerChildren.add(status);
        }
        if (value(card, 12) != null) {
            centerChildren.add(text(value(card, 12), "labelSmall", null,
                    tokens.color("nba.label.tertiary"), 1));
        }
        center.setChildren(centerChildren);
        children.add(center);

        children.add(heroTeam(value(card, 8), value(card, 9), value(card, 10),
                "cards." + value(card, 0) + ".homeScore", true));
        row.setChildren(children);
        return row;
    }

    /**
     * Away: logo/name toward outer edge, score toward center. Home: score toward center, logo/name outer.
     */
    private AtomicElement heroTeam(String tri, String score, String logoUrl, String bindRef, boolean homeSide) {
        AtomicElement row = container("row", "center", "center");
        row.setGap(tokens.spacing("sm"));
        row.setWidth(homeSide ? 108 : 112);
        List<AtomicElement> stackChildren = new ArrayList<>();
        if (logoUrl != null) {
            AtomicElement logoImg = image(logoUrl, 44, 44, "contain");
            AccessibilityHelper.addImage(logoImg, tri + " logo");
            stackChildren.add(logoImg);
        }
        stackChildren.add(text(tri, "labelSmall", "bold", tokens.color("nba.label.primary"), 1));
        AtomicElement nameCol = container("column", "center", "center");
        nameCol.setChildren(stackChildren);

        AtomicElement scoreText = null;
        if (score != null) {
            scoreText = text(score, "titleLarge", "bold", tokens.color("nba.label.primary"), 1);
            scoreText.setBindRef(bindRef);
            scoreText.setMonospacedDigits(true);
        }

        List<AtomicElement> rowChildren = new ArrayList<>();
        if (homeSide) {
            if (scoreText != null) rowChildren.add(scoreText);
            rowChildren.add(nameCol);
        } else {
            rowChildren.add(nameCol);
            if (scoreText != null) rowChildren.add(scoreText);
        }
        row.setChildren(rowChildren);
        return row;
    }

    private AtomicElement utilityCard(String[] item) {
        AtomicElement card = container("column", "center", "center");
        card.setId(value(item, 0));
        card.setGap(tokens.spacing("sm"));
        // Fixed height + widthMode:fill gives the grid balanced cells across rows
        // even when subtitles wrap differently. Width comes from the parent
        // row's flex distribution; height is locked to keep the grid rhythm
        // visually stable on mobile.
        card.setHeight(132);
        card.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        card.setBackground(tokens.color("nba.bg.secondary"));
        card.setCornerRadius(tokens.radius("md"));
        card.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("lg"), tokens.spacing("md")));
        shadow(card);
        if (value(item, 4) != null) {
            card.setActions(singleActionArray(tapNavigate(value(item, 4))));
            AccessibilityHelper.addButton(card, value(item, 1));
        }

        List<AtomicElement> children = new ArrayList<>();
        if (value(item, 3) != null) {
            AtomicElement img = image(value(item, 3), 44, 44, "contain");
            AccessibilityHelper.addImage(img, value(item, 1) + " icon");
            children.add(img);
        } else {
            children.add(neutralInitials(value(item, 1), 44, 22));
        }
        children.add(text(value(item, 1), "bodyMedium", "semiBold",
                tokens.color("nba.label.primary"), 2));
        if (value(item, 2) != null) {
            children.add(text(value(item, 2), "labelSmall", null,
                    tokens.color("nba.label.secondary"), 2));
        }
        card.setChildren(children);
        return card;
    }

    private AtomicElement leagueCard(String id, String label, String imageUrl, String targetUri) {
        AtomicElement card = container("column", "center", "center");
        card.setId(id);
        card.setWidth(160);
        card.setGap(tokens.spacing("sm"));
        card.setBackground(tokens.color("nba.bg.secondary"));
        card.setCornerRadius(tokens.radius("md"));
        card.setPadding(padding(tokens.spacing("md"), tokens.spacing("md"),
                tokens.spacing("lg"), tokens.spacing("lg")));
        shadow(card);
        if (targetUri != null) {
            card.setActions(singleActionArray(tapNavigate(targetUri)));
            AccessibilityHelper.addButton(card, label);
        }

        List<AtomicElement> children = new ArrayList<>();
        if (imageUrl != null) {
            AtomicElement logo = image(imageUrl, 72, 56, "contain");
            logo.setAspectRatio(4.0 / 3.0);
            AccessibilityHelper.addImage(logo, label + " logo");
            children.add(logo);
        } else {
            children.add(neutralInitials(label, 72, 28));
        }
        children.add(text(label, "bodyMedium", "semiBold",
                tokens.color("nba.label.primary"), 2));
        card.setChildren(children);
        return card;
    }

    private AtomicElement gameScheduleRowElement(String[] row) {
        return gameScheduleRowElement(row, null);
    }

    private AtomicElement gameScheduleRowElement(String[] row, GameClockSnapshot clock) {
        AtomicElement card = container("column", null, "stretch");
        card.setId(value(row, 0));
        card.setGap(tokens.spacing("sm"));
        // Refreshed card treatment: flush tile — secondary background, no corner radius, no
        // shadow. Inter-card spacing is owned by the parent list `gap` rather than per-card
        // margin, so cards stack with consistent vertical rhythm without each renderer
        // having to set its own outer margin.
        card.setBackground("token:nba.bg.secondary");
        card.setCornerRadius(0);
        card.setPadding(padding(tokens.spacing("lg"), tokens.spacing("lg"),
                tokens.spacing("lg"), tokens.spacing("lg")));
        if (value(row, 14) != null) {
            card.setActions(singleActionArray(tapNavigate(value(row, 14))));
            String awayTri = value(row, 1) != null ? value(row, 1) : "";
            String homeTri = value(row, 6) != null ? value(row, 6) : "";
            String status = value(row, 11) != null ? value(row, 11) : "";
            AccessibilityHelper.addButton(card, awayTri + " vs " + homeTri + ", " + status);
        }

        List<AtomicElement> children = new ArrayList<>();

        // Matchup row laid out left-to-right:
        //   awayTeamBlock  |  awayScore  |  statusColumn  |  homeScore  |  homeTeamBlock
        // The status column now owns the series-record subtitle (e.g. "NYK wins 4-0")
        // below the status text, matching the production NBA card.
        AtomicElement matchup = container("row", "spaceBetween", "center");
        matchup.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        List<AtomicElement> matchupChildren = new ArrayList<>();
        matchupChildren.add(scheduleTeamBlock(
                value(row, 1), value(row, 2), value(row, 3), value(row, 5), /* awayLeading */ true));
        matchupChildren.add(scheduleScoreText(value(row, 4), value(row, 0), "awayScore"));
        matchupChildren.add(scheduleStatus(value(row, 11), value(row, 0), clock, value(row, 12)));
        matchupChildren.add(scheduleScoreText(value(row, 9), value(row, 0), "homeScore"));
        matchupChildren.add(scheduleTeamBlock(
                value(row, 6), value(row, 7), value(row, 8), value(row, 10), /* awayLeading */ false));
        matchup.setChildren(matchupChildren);
        children.add(matchup);

        // Optional broadcast/info row below the matchup (e.g. "ESPN").
        if (value(row, 13) != null) {
            AtomicElement broadcastRow = container("row", "center", "center");
            broadcastRow.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
            List<AtomicElement> broadcastChildren = new ArrayList<>();
            broadcastChildren.add(text(value(row, 13), "labelSmall", null,
                    tokens.color("nba.label.tertiary"), 1));
            broadcastRow.setChildren(broadcastChildren);
            children.add(broadcastRow);
        }

        // Card footer: League Pass entry (left) + overflow More button (right). Rendered
        // on every card; the overflow button's tap target is omitted when slot 15 is
        // null but the icon still occupies the same screen position for visual rhythm.
        children.add(cardHairlineDivider());
        children.add(scheduleFooterRow(value(row, 15)));

        card.setChildren(children);
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
    private AtomicElement scheduleTeamBlock(
            String tri, String name, String seed, String logoUrl, boolean awayLeading) {
        AtomicElement col = container("column", "center", "center");
        col.setWidth(72);
        List<AtomicElement> children = new ArrayList<>();
        if (logoUrl != null) {
            AtomicElement logoImg = image(logoUrl, 48, 48, "contain", null);
            AccessibilityHelper.addImage(logoImg, (tri != null ? tri : "") + " logo");
            children.add(logoImg);
            children.add(spacer(tokens.spacing("xs")));
        }
        String seedPrefix = seed != null && !seed.isBlank() ? seed + " " : "";
        children.add(text(seedPrefix + (tri != null ? tri : ""),
                "titleMedium", "semiBold", tokens.color("nba.label.primary"), 1));
        if (name != null && !name.isBlank()) {
            children.add(text(name, "labelSmall", null,
                    tokens.color("nba.label.secondary"), 1));
        }
        col.setChildren(children);
        return col;
    }

    /**
     * Score text element for a schedule row's center cluster. Returns an empty
     * fixed-width spacer when {@code score} is null so pre-game cards (no scores yet) keep
     * the row's center status horizontally aligned across cards.
     */
    private AtomicElement scheduleScoreText(String score, String rowId, String scoreKey) {
        if (score == null) {
            // Placeholder spacer so the status column stays in the same screen-x position
            // whether or not the game has a score yet. Width sized to roughly match a
            // 3-digit score rendered in the (compact) "score" typography token.
            AtomicElement placeholder = container("column", "center", "center");
            placeholder.setWidth(32);
            placeholder.setChildren(new ArrayList<>());
            return placeholder;
        }
        AtomicElement scoreText = text(score, "score", "bold",
                tokens.color("nba.label.primary"), 1);
        scoreText.setBindRef(rowId + "." + scoreKey);
        scoreText.setMonospacedDigits(true);
        scoreText.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
        return scoreText;
    }

    private AtomicElement scheduleStatus(String status, String rowId) {
        return scheduleStatus(status, rowId, null, null);
    }

    private AtomicElement scheduleStatus(String status, String rowId,
                                      GameClockSnapshot clock) {
        return scheduleStatus(status, rowId, clock, null);
    }

    /**
     * Center column of a schedule row: status text (or LiveClock when game is live),
     * optionally followed by a series-record subtitle (e.g. "NYK wins 4-0") for playoff
     * games whose upstream payload carries {@code seriesText}.
     */
    private AtomicElement scheduleStatus(String status, String rowId,
                                      GameClockSnapshot clock, String seriesText) {
        AtomicElement center = container("column", "center", "center");
        center.setWidth(88);
        List<AtomicElement> children = new ArrayList<>();
        String raw = status != null ? status : "";
        boolean liveCue = raw.toUpperCase(Locale.ROOT).contains("LIVE") || clock != null;

        if (clock != null) {
            // Live game: render a LiveClock that counts down via SSE binding
            AtomicElement clockEl = new AtomicElement().withType(AtomicElement.Type.fromValue("LiveClock"));
            clockEl.setVariant("labelSmall");
            clockEl.setFormat(AtomicElement.Format.fromValue("m:ss"));
            clockEl.setTickDirection(AtomicElement.TickDirection.fromValue("down"));
            clockEl.setBindRef(rowId + ".clock");
            clockEl.setSnapshotSeconds(clock.snapshotSeconds());
            clockEl.setIsRunning(clock.isRunning());
            AtomicElement statusLine = container("row", "center", "center");
            statusLine.setGap(tokens.spacing("xs"));
            List<AtomicElement> sl = new ArrayList<>();
            sl.add(liveStatusDot());
            sl.add(clockEl);
            statusLine.setChildren(sl);
            children.add(statusLine);
        } else {
            AtomicElement statusText = text(raw, "labelSmall", "semiBold",
                    tokens.color("nba.label.secondary"), 2);
            statusText.setBindRef(rowId + ".statusText");
            statusText.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
            if (liveCue) {
                AtomicElement statusLine = container("row", "center", "center");
                statusLine.setGap(tokens.spacing("xs"));
                List<AtomicElement> sl = new ArrayList<>();
                sl.add(liveStatusDot());
                sl.add(statusText);
                statusLine.setChildren(sl);
                children.add(statusLine);
            } else {
                children.add(statusText);
            }
        }
        if (seriesText != null && !seriesText.isBlank()) {
            AtomicElement sub = text(seriesText, "labelSmall", "semiBold",
                    tokens.color("nba.label.tertiary"), 2);
            sub.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
            children.add(sub);
        }
        center.setChildren(children);
        return center;
    }

    /**
     * Card footer for a schedule row: League Pass CTA on the left, overflow "more"
     * button on the right. Both are placeholders today — League Pass routes to the
     * commerce surface; the more button is a no-op when {@code overflowUri} is null.
     */
    private AtomicElement scheduleFooterRow(String overflowUri) {
        AtomicElement row = container("row", "spaceBetween", "center");
        row.setWidthMode(AtomicElement.SizingMode.fromValue("fill"));
        List<AtomicElement> kids = new ArrayList<>();

        // Left cluster: yellow circular play badge + "League Pass" label.
        AtomicElement left = container("row", "start", "center");
        left.setGap(tokens.spacing("sm"));
        List<AtomicElement> leftKids = new ArrayList<>();
        leftKids.add(leaguePassPlayBadge());
        leftKids.add(text("League Pass", "labelMedium", "semiBold",
                tokens.color("nba.label.primary"), 1));
        left.setChildren(leftKids);
        left.setActions(singleActionArray(tapNavigate("nba://commerce/leaguepass")));
        AccessibilityHelper.addButton(left, "League Pass");
        kids.add(left);

        // Right: overflow "more" button. When no overflowUri is provided the icon still
        // renders for layout rhythm but carries no action.
        AtomicElement more = new AtomicElement().withType(AtomicElement.Type.fromValue("Button"));
        more.setLabel("");
        more.setIcon(tokens.icon("more"));
        more.setVariant("text");
        more.setColor(tokens.color("nba.label.secondary"));
        if (overflowUri != null) {
            more.setActions(singleActionArray(tapNavigate(overflowUri)));
        }
        kids.add(more);

        row.setChildren(kids);
        return row;
    }

    /**
     * Yellow filled circle (NBA brand) with a centered white play triangle. Composed
     * inline from a colored container + glyph so no bundled asset is required.
     */
    private AtomicElement leaguePassPlayBadge() {
        AtomicElement badge = container("column", "center", "center");
        badge.setWidth(24);
        badge.setHeight(24);
        badge.setCornerRadius(12);
        badge.setBackground(tokens.color("nba.label.accent.brand"));
        List<AtomicElement> kids = new ArrayList<>();
        kids.add(text("\u25B6", "labelSmall", "bold",
                tokens.color("nba.label-inverted.primary"), 1));
        badge.setChildren(kids);
        return badge;
    }

    /** 6dp live indicator dot ({@code color("nba.label.accent.live")}). */
    private AtomicElement liveStatusDot() {
        AtomicElement dot = container("row", "center", "center");
        dot.setWidth(6);
        dot.setHeight(6);
        dot.setCornerRadius(tokens.radius("sm"));
        dot.setBackground(tokens.color("nba.label.accent.live"));
        dot.setChildren(new ArrayList<>());
        return dot;
    }

    private AtomicElement cardHairlineDivider() {
        AtomicElement d = new AtomicElement().withType(AtomicElement.Type.fromValue("Divider"));
        d.setThickness(1);
        d.setColor(tokens.color("nba.divider.subtle"));
        return d;
    }

    /**
     * Typed variant: paged horizontal carousel. Schema does not expose
     * {@code paging}/{@code snapAlignment}/{@code pageIndicator} as typed
     * fields on AtomicElement, so they ride through as additional properties
     * (same wire shape as the ObjectNode variant).
     */
    private AtomicElement pagedHorizontalScrollTyped(Object gap, int childCount, Spacing padding,
                                                     String indicatorAlignment,
                                                     String inactiveDotColor, String activeDotColor) {
        AtomicElement scroll = scrollContainer("row", gap, false);
        if (padding != null) scroll.setPadding(padding);
        if (childCount > 1) {
            scroll.setPaging(true);
            scroll.setSnapAlignment(AtomicElement.SnapAlignment.fromValue("center"));
            Map<String, Object> indicator = new LinkedHashMap<>();
            indicator.put("style", "dots");
            indicator.put("alignment", indicatorAlignment != null ? indicatorAlignment : "bottomCenter");
            indicator.put("color", inactiveDotColor != null ? inactiveDotColor : tokens.color("nba.label.tertiary"));
            indicator.put("activeColor", activeDotColor != null ? activeDotColor : tokens.color("nba.label-inverted.primary"));
            scroll.setAdditionalProperty("pageIndicator", indicator);
        }
        return scroll;
    }

    private Map<String, Object> gradient(String first, String second, String direction) {
        Map<String, Object> gradient = new LinkedHashMap<>();
        gradient.put("colors", List.of(first, second));
        gradient.put("direction", direction);
        return gradient;
    }

    private AtomicElement neutralInitials(String label, int width, Object radius) {
        return neutralInitialsRect(label, width, width, radius);
    }

    private AtomicElement neutralInitialsRect(String label, int width, int height, Object radius) {
        AtomicElement box = container("row", "center", "center");
        box.setWidth(width);
        box.setHeight(height);
        if (radius != null) box.setCornerRadius(radius);
        box.setBackground(tokens.color("nba.bg.tertiary"));
        box.setChildren(List.of(
                text(initials(label), "labelSmall", "bold",
                        tokens.color("nba.label.secondary"), 1)));
        return box;
    }

    /** Typed variant: returns logos as a typed AtomicElement list for use in typed containers. */
    private List<AtomicElement> logoRowTyped(String csv, int width, int height) {
        List<AtomicElement> logos = new ArrayList<>();
        if (csv == null || csv.isBlank()) return logos;
        for (String raw : csv.split(",")) {
            String url = raw.trim();
            if (!url.isEmpty()) logos.add(image(url, width, height, "contain"));
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

    private ObjectNode containerNode(String direction, String alignment, String crossAlignment) {
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
    private ObjectNode variantContainerNode(String variant, String direction,
                                String alignment, String crossAlignment) {
        ObjectNode node = containerNode(direction, alignment, crossAlignment);
        if (variant != null) node.put("variant", variant);
        return node;
    }

    private ObjectNode heroContainerNode(String direction, String alignment, String crossAlignment) {
        return variantContainerNode("hero", direction, alignment, crossAlignment);
    }

    private ObjectNode groupedContainerNode(String direction, String alignment, String crossAlignment) {
        return variantContainerNode("grouped", direction, alignment, crossAlignment);
    }

    /**
     * Build a responsive row Container that replaces the old Row section type.
     * Children are flexed equally (flex=1) and the direction flips row→column at the breakpoint.
     * widthMode is "fill" by default to maintain parity with the old Row renderer.
     */
    private ObjectNode responsiveRowNode(Object gap, int breakpoint) {
        ObjectNode node = containerNode("row", null, null);
        putLayoutScalar(node, "gap", gap);
        node.put("breakpoint", breakpoint);
        node.put("widthMode", "fill");
        return node;
    }

    private ObjectNode textNode(String content, String variant, String weight,
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

    private ObjectNode imageNode(String src, int width, int height, String fit) {
        return imageNode(src, width, height, fit, DEFAULT_PLACEHOLDER);
    }

    private ObjectNode imageNode(String src, int width, int height, String fit, String placeholder) {
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
    private ObjectNode variantImageNode(String variant, String src) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Image");
        node.put("src", src);
        if (variant != null) node.put("variant", variant);
        node.put("placeholder", DEFAULT_PLACEHOLDER);
        return node;
    }

    private ObjectNode thumbnailImageNode(String src) { return variantImageNode("thumbnail", src); }

    private ObjectNode buttonNode(String label, String variant, ObjectNode action) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Button");
        node.put("label", label);
        if (variant != null) node.put("variant", variant);
        node.set("actions", singleActionArrayNode(action));
        return node;
    }

    private ObjectNode spacerNode(String height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    /** Spacer with a raw pixel height (§3.6 exception: no design-system token exists). */
    private ObjectNode spacerNode(int height) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("height", height);
        return node;
    }

    private ObjectNode hSpacerNode(String width) {
        ObjectNode node = om.createObjectNode();
        node.put("type", "Spacer");
        node.put("width", width);
        return node;
    }

    private ObjectNode paddingNode(Object start, Object end, Object top, Object bottom) {
        ObjectNode p = om.createObjectNode();
        putLayoutScalar(p, "start", start);
        putLayoutScalar(p, "end", end);
        putLayoutScalar(p, "top", top);
        putLayoutScalar(p, "bottom", bottom);
        return p;
    }

    /** Typed variant of {@link #cornerRadiiOf} — only typed CornerRadii is used. */
    public CornerRadii cornerRadiiOf(Object topStart, Object topEnd,
                                     Object bottomStart, Object bottomEnd) {
        CornerRadii r = new CornerRadii();
        if (topStart != null) r.setTopStart(normalizeLayoutScalar(topStart));
        if (topEnd != null) r.setTopEnd(normalizeLayoutScalar(topEnd));
        if (bottomStart != null) r.setBottomStart(normalizeLayoutScalar(bottomStart));
        if (bottomEnd != null) r.setBottomEnd(normalizeLayoutScalar(bottomEnd));
        return r;
    }

    /** Construct a typed ScrollContainer AtomicElement. */
    public AtomicElement scrollContainer(String direction, Object gap, boolean showIndicators) {
        AtomicElement el = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("ScrollContainer"));
        if (direction != null) el.setDirection(AtomicElement.Direction.fromValue(direction));
        if (gap != null) el.setGap(gap);
        el.setShowIndicators(showIndicators);
        return el;
    }

    /** Puts a LayoutScalar value (int or "token:..." string) into a node. */
    private void putLayoutScalar(ObjectNode node, String key, Object value) {
        if (value instanceof String s) {
            node.put(key, s);
        } else if (value instanceof Number n) {
            node.put(key, n.intValue());
        }
    }

    private ObjectNode tapNavigateNode(String targetUri) {
        ObjectNode action = om.createObjectNode();
        action.put("trigger", "onActivate");
        action.put("type", "navigate");
        action.put("targetUri", targetUri);
        return action;
    }

    /** Navigate action with explicit failure policy and optional feedback. */
    private ObjectNode tapNavigateNode(String targetUri, String onFailure, ObjectNode failureFeedback) {
        ObjectNode action = tapNavigateNode(targetUri);
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

    private ArrayNode singleActionArrayNode(ObjectNode action) {
        ArrayNode arr = om.createArrayNode();
        arr.add(action);
        return arr;
    }

    // ── Typed public surface ────────────────────────────────────────────
    //
    // Public API — composers consume typed POJOs. Implementations delegate
    // to the private *Node helpers above. The valueToTree shims at the
    // boundary are builder-internal and disappear in a future sweep that
    // refactors the *Node bodies to be typed end-to-end.

    public AtomicElement container(String direction, String alignment, String crossAlignment) {
        return bindElement(containerNode(direction, alignment, crossAlignment));
    }

    public AtomicElement variantContainer(String variant, String direction,
                                          String alignment, String crossAlignment) {
        return bindElement(variantContainerNode(variant, direction, alignment, crossAlignment));
    }

    public AtomicElement heroContainer(String direction, String alignment, String crossAlignment) {
        return bindElement(heroContainerNode(direction, alignment, crossAlignment));
    }

    public AtomicElement groupedContainer(String direction, String alignment, String crossAlignment) {
        return bindElement(groupedContainerNode(direction, alignment, crossAlignment));
    }

    public AtomicElement responsiveRow(Object gap, int breakpoint) {
        return bindElement(responsiveRowNode(gap, breakpoint));
    }

    public AtomicElement text(String content, String variant, String weight,
                              String color, Integer maxLines) {
        return bindElement(textNode(content, variant, weight, color, maxLines));
    }

    public AtomicElement image(String src, int width, int height, String fit) {
        return bindElement(imageNode(src, width, height, fit));
    }

    public AtomicElement image(String src, int width, int height, String fit, String placeholder) {
        return bindElement(imageNode(src, width, height, fit, placeholder));
    }

    public AtomicElement variantImage(String variant, String src) {
        return bindElement(variantImageNode(variant, src));
    }

    public AtomicElement thumbnailImage(String src) {
        return bindElement(thumbnailImageNode(src));
    }

    public AtomicElement button(String label, String variant, Action action) {
        return bindElement(buttonNode(label, variant, action == null ? null : toObjectNode(action)));
    }

    public AtomicElement spacer(String height) {
        return bindElement(spacerNode(height));
    }

    public AtomicElement spacer(int height) {
        return bindElement(spacerNode(height));
    }

    public AtomicElement hSpacer(String width) {
        return bindElement(hSpacerNode(width));
    }

    public AtomicElement liveBadge() {
        return liveBadgeNode();
    }

    public AtomicElement durationBadge(String duration) {
        return durationBadgeNode(duration);
    }

    /** Generic pill badge with custom background color (used for NEW/LIVE chips, durations). */
    public AtomicElement pillBadgeTyped(String label, String backgroundColor) {
        AtomicElement badge = container("row", null, "center");
        badge.setBackground(backgroundColor);
        badge.setCornerRadius(tokens.radius("sm"));
        badge.setPadding(padding(tokens.spacing("xs"), tokens.spacing("xs"),
                tokens.spacing("xs"), tokens.spacing("xs")));
        badge.setChildren(List.of(text(label, "labelSmall", "bold",
                tokens.color("nba.label-inverted.primary"), null)));
        return badge;
    }

    /** Typed AtomicOverlay constructor for OverlayContainer.overlays[]. */
    public AtomicOverlay overlay(String alignment, Spacing inset, AtomicElement element) {
        AtomicOverlay o = new AtomicOverlay();
        if (alignment != null) o.setAlignment(Badge.BadgeAlignment.fromValue(alignment));
        if (inset != null) o.setInset(inset);
        o.setElement(element);
        return o;
    }

    /** Typed OverlayContainer atomic element (base image + ordered overlay layers). */
    public AtomicElement overlayContainer(AtomicElement base, List<AtomicOverlay> overlays) {
        AtomicElement el = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("OverlayContainer"));
        el.setBase(base);
        if (overlays != null) el.setOverlays(overlays);
        return el;
    }

    public AtomicElement sectionSlot(String id, Section section) {
        AtomicElement el = new AtomicElement()
                .withType(AtomicElement.Type.fromValue("SectionSlot"));
        if (id != null) el.setId(id);
        if (section != null) el.setSection(section);
        return el;
    }

    public AtomicComposite wrapUi(AtomicElement rootElement) {
        AtomicComposite data = new AtomicComposite();
        if (rootElement != null) data.setUi(rootElement);
        return data;
    }

    public Spacing padding(Object start, Object end, Object top, Object bottom) {
        return bindSpacing(paddingNode(start, end, top, bottom));
    }

    public Action tapNavigate(String targetUri) {
        return bindAction(tapNavigateNode(targetUri));
    }

    public List<Action> singleActionArray(Action action) {
        List<Action> list = new ArrayList<>();
        if (action != null) list.add(action);
        return list;
    }

    // ── Typed style mutators (chainable: return the mutated element) ────

    public AtomicElement opacity(AtomicElement element, double value) {
        if (element != null) element.setOpacity(value);
        return element;
    }

    public AtomicElement textAlign(AtomicElement element, String align) {
        if (element != null && align != null) {
            element.setTextAlign(AtomicElement.TextAlign.fromValue(align));
        }
        return element;
    }

    public AtomicElement monospacedDigits(AtomicElement element) {
        if (element != null) element.setMonospacedDigits(true);
        return element;
    }

    public AtomicElement showIndicators(AtomicElement element, boolean show) {
        if (element != null) element.setShowIndicators(show);
        return element;
    }

    public AtomicElement shadow(AtomicElement element, String color, double radius,
                                double offsetX, double offsetY) {
        if (element != null) {
            ObjectNode s = om.createObjectNode();
            if (color != null) s.put("color", color);
            s.put("radius", radius);
            s.put("offsetX", offsetX);
            s.put("offsetY", offsetY);
            element.setShadow(s);
        }
        return element;
    }

    public AtomicElement shadow(AtomicElement element) {
        return shadow(element, "#00000014", 4, 0, 2);
    }

    public AtomicElement shadows(AtomicElement element, List<Object> shadowList) {
        if (element != null && shadowList != null) {
            element.setShadows(shadowList);
        }
        return element;
    }

    /**
     * Build a Shadow value with an explicit {@code type} field. The wire
     * shape is token-or-struct (Object); inline structs are represented as
     * an ObjectNode here because no generated Shadow POJO exists yet.
     */
    public Object shadowWithType(String type, String color, int offsetX, int offsetY, int radius) {
        return shadowWithTypeNode(type, color, offsetX, offsetY, radius);
    }

    public AtomicElement backgrounds(AtomicElement element, List<Object> layers) {
        if (element == null) return null;
        for (Object layer : layers) {
            if (!(layer instanceof String) && !(layer instanceof ObjectNode)) {
                throw new IllegalArgumentException(
                    "backgrounds layer must be String (color) or ObjectNode (gradient/image), got: "
                        + (layer == null ? "null" : layer.getClass().getName()));
            }
        }
        element.setBackgrounds(layers);
        return element;
    }

    public AtomicElement badge(AtomicElement parent, AtomicElement badgeElement, String alignment) {
        if (parent != null && badgeElement != null) {
            Badge b = new Badge();
            b.setElement(badgeElement);
            if (alignment != null) {
                b.setAlignment(Badge.BadgeAlignment.fromValue(alignment));
            }
            parent.setBadge(b);
        }
        return parent;
    }

    // ── Typed layout mutators ──────────────────────────────────────────

    public void setFlex(AtomicElement element, double flex) {
        if (element != null) element.setFlex(flex);
    }

    public void widthMode(AtomicElement element, String mode) {
        validateEnum("widthMode", mode, VALID_SIZE_MODES);
        if (element != null && mode != null) {
            element.setWidthMode(AtomicElement.SizingMode.fromValue(mode));
        }
    }

    public void heightMode(AtomicElement element, String mode) {
        validateEnum("heightMode", mode, VALID_SIZE_MODES);
        if (element != null && mode != null) {
            element.setHeightMode(AtomicElement.SizingMode.fromValue(mode));
        }
    }

    public void minWidth(AtomicElement element, Object scalar) {
        if (element != null) element.setMinWidth(normalizeLayoutScalar(scalar));
    }

    public void maxWidth(AtomicElement element, Object scalar) {
        if (element != null) element.setMaxWidth(normalizeLayoutScalar(scalar));
    }

    public void minHeight(AtomicElement element, Object scalar) {
        if (element != null) element.setMinHeight(normalizeLayoutScalar(scalar));
    }

    public void maxHeight(AtomicElement element, Object scalar) {
        if (element != null) element.setMaxHeight(normalizeLayoutScalar(scalar));
    }

    public void layoutWrap(AtomicElement element, boolean wrap) {
        if (element != null) element.setLayoutWrap(wrap);
    }

    public void crossAxisGap(AtomicElement element, Object scalar) {
        if (element != null) element.setCrossAxisGap(normalizeLayoutScalar(scalar));
    }

    public void alignSelf(AtomicElement element, String alignment) {
        validateEnum("alignSelf", alignment, VALID_ALIGN_SELF);
        if (element != null && alignment != null) {
            element.setAlignSelf(AtomicElement.CrossAlignment.fromValue(alignment));
        }
    }

    public void attachSectionStates(Section section, ObjectNode sectionStates) {
        if (section == null || sectionStates == null) return;
        section.setSectionStates(om.convertValue(sectionStates,
                com.nba.sdui.models.generated.SectionStates.class));
    }

    public void attachRefreshPolicy(Section section, RefreshPolicy refreshPolicy) {
        if (section == null || refreshPolicy == null) return;
        section.setRefreshPolicy(refreshPolicy);
    }

    /**
     * Normalize a LayoutScalar value (int or token string) for typed setters
     * that accept Object. Numbers are stored as Integer to match JSON
     * serialization shape.
     */
    private static Object normalizeLayoutScalar(Object value) {
        if (value instanceof Number n) return n.intValue();
        return value;
    }
}
