package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.nba.sdui.domain.AccessibilityHelper.*;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;

/**
 * Composes the "Watch" SDUI screen — video / streaming hub organised into tabs.
 *
 * Layout:
 *   TabGroup with 3 tabs: Featured, NBA TV, League Pass
 *   Each tab contains a mix of ContentRails, GamePanels and PromoBanners.
 */
@Component
public class WatchComposer {

    private static final Logger log = LoggerFactory.getLogger(WatchComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final ScoreboardPort scoreboardPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public WatchComposer(ObjectMapper objectMapper,
                         ScoreboardPort scoreboardPort,
                         SduiUtils utils,
                         SectionSurfaces surfaces,
                         Tokens tokens) {
        this.objectMapper = objectMapper;
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
    }

    public Screen composeWatch(String traceId, String locale) {
        log.info("Composing Watch screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "watch");
        response.put("analyticsId", "watch");
        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "watch");

        ArrayNode sections = objectMapper.createArrayNode();

        // Single TabGroup section that contains the three tabs
        sections.add(buildTabGroup());

        response.set("sections", sections);

        // Subnav is edge-to-edge under top navigation — no horizontal screen inset.
        ObjectNode insets = objectMapper.createObjectNode();
        insets.put("bottom", tokens.spacing("lg"));
        response.set("contentInsets", insets);

        utils.stampStringTableOnSections(response, locale);
        try {
            return objectMapper.treeToValue(response, Screen.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to bind composed Watch screen to Screen.class", e);
        }
    }

    // ── Tab group ──────────────────────────────────────────────────────

    private ObjectNode buildTabGroup() {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "TabGroup");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "TabGroup");
        section.put("analyticsId", "watch_tabs");
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "watch_active_tab");
        data.put("defaultTab", "featured");

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add(buildTab("watch-featured", "Featured", "watch_active_tab", "featured"));
        tabs.add(buildTab("watch-nbatv", "NBA TV", "watch_active_tab", "nbatv"));
        tabs.add(buildTab("watch-leaguepass", "League Pass", "watch_active_tab", "leaguepass"));
        data.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();
        tabContents.set("featured", buildFeaturedSections());
        tabContents.set("nbatv", buildNbaTvSections());
        tabContents.set("leaguepass", buildLeaguePassSections());
        data.set("tabContents", tabContents);

        section.set("data", data);
        section.set("subsections", utils.tabSelectSubsections(tabs, "watch_active_tab"));
        section.set("surface", objectMapper.valueToTree(surfaces.stripSurfaceWithoutBackground()));

        return section;
    }

    private ObjectNode buildTab(String id, String label, String stateKey, String stateValue) {
        ObjectNode tab = objectMapper.createObjectNode();
        tab.put("id", id);
        tab.put("label", label);
        tab.put("stateKey", stateKey);
        tab.put("stateValue", stateValue);
        return tab;
    }

    // ── Featured tab sections ─────────────────────────────────────────

    private ArrayNode buildFeaturedSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // Ad slot at top
        sections.add(buildAdSlot("watch/featured/top"));

        addVideoCarousel(sections, "cms:watch-highlights", "Tonight's Highlights", null,
                new String[][]{
                        {"fh-1", "Game Recap: BOS vs LAL", "Full game highlights",
                                FALLBACK_THUMB,
                                "9:45", null, "nba://video/bos-lal-recap"},
                        {"fh-2", "Clutch Moments", "The best clutch plays tonight",
                                FALLBACK_THUMB,
                                "4:12", null, "nba://video/clutch-moments"},
                        {"fh-3", "Rookie Watch", "Top rookie performances",
                                FALLBACK_THUMB,
                                "3:20", "NEW", "nba://video/rookie-watch"},
                        {"fh-4", "Top 10 Plays", "Last night's best moments",
                                FALLBACK_THUMB,
                                "2:45", null, "nba://video/top10-plays"}
                });

        // Inline subscription upsell
        sections.add(buildSubscribeUpsellBanner(
                "NBA League Pass",
                "Watch every out-of-market game live or on demand",
                FALLBACK_THUMB,
                "Subscribe Now", "nba://subscribe/league-pass"));

        addVideoCarousel(sections, "cms:watch-originals", "NBA Originals", null,
                new String[][]{
                        {"fo-1", "Open Court", "Inside the NBA culture",
                                FALLBACK_THUMB,
                                "22:00", null, "nba://video/open-court"},
                        {"fo-2", "NBA Countdown", "Pre-game analysis",
                                FALLBACK_THUMB,
                                "15:30", null, "nba://video/countdown"},
                        {"fo-3", "The Reel", "Behind-the-scenes with players",
                                FALLBACK_THUMB,
                                "18:45", null, "nba://video/the-reel"}
                });

        // Ad slot mid-page
        sections.add(buildAdSlot("watch/featured/mid"));

        addContentRail(sections, "cms:watch-stories", "Trending Stories",
                new String[][]{
                        {"fs-1", "Trade Deadline Preview", "Who's on the move?",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/trade-deadline"},
                        {"fs-2", "All-Star Voting Update", "Current vote leaders",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/allstar-voting"},
                        {"fs-3", "Playoff Picture", "Updated standings breakdown",
                                FALLBACK_THUMB,
                                "article", null, "nba://article/playoff-picture"}
                });

        return sections;
    }

    // ── NBA TV tab sections ────────────────────────────────────────────

    private ArrayNode buildNbaTvSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // NBA TV Schedule (hero + time-slot list)
        sections.add(buildNbaTvSchedule());

        addVideoCarousel(sections, "cms:nbatv-shows", "Popular Shows", null,
                new String[][]{
                        {"nbatv-s1", "NBA GameTime", "Nightly highlights & analysis",
                                FALLBACK_THUMB,
                                "60:00", null, "nba://watch/gametime"},
                        {"nbatv-s2", "NBA Inside Stuff", "Player features and interviews",
                                FALLBACK_THUMB,
                                "30:00", null, "nba://watch/inside-stuff"},
                        {"nbatv-s3", "NBA Action", "Weekly highlights show",
                                FALLBACK_THUMB,
                                "30:00", null, "nba://watch/nba-action"}
                });

        sections.add(buildAdSlot("watch/nbatv/mid"));

        addVideoCarousel(sections, "cms:nbatv-classics", "Classic Games", "Relive the greatest moments",
                new String[][]{
                        {"nbatv-c1", "2016 Finals Game 7", "CLE vs GSW — The Block, The Shot, The Stop",
                                FALLBACK_THUMB,
                                "2:48:00", null, "nba://video/2016-finals-g7"},
                        {"nbatv-c2", "Kobe's 81 Points", "Jan 22, 2006 vs Toronto Raptors",
                                FALLBACK_THUMB,
                                "2:15:00", null, "nba://video/kobe-81"},
                        {"nbatv-c3", "2013 Finals Game 6", "MIA vs SAS — Ray Allen's clutch three",
                                FALLBACK_THUMB,
                                "2:35:00", null, "nba://video/2013-finals-g6"}
                });

        return sections;
    }

    // ── League Pass tab sections ───────────────────────────────────────

    private ArrayNode buildLeaguePassSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // Full-screen subscription upsell with pricing tiers
        sections.add(buildSubscribeUpsellHero());

        sections.add(buildSectionHeader("live-games", "Live Games", null, null));

        // Pull real games if available
        addLiveGamePanels(sections);

        addVideoCarousel(sections, "cms:lp-condensed", "Condensed Games", "Full games in ~12 minutes",
                new String[][]{
                        {"lp-c1", "BOS vs MIA Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "12:34", null, "nba://video/bos-mia-condensed"},
                        {"lp-c2", "DEN vs PHX Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "11:58", null, "nba://video/den-phx-condensed"},
                        {"lp-c3", "LAL vs GSW Condensed", "Full condensed game",
                                FALLBACK_THUMB,
                                "12:10", null, "nba://video/lal-gsw-condensed"}
                });

        // Ad slot
        sections.add(buildAdSlot("watch/leaguepass/mid"));

        return sections;
    }

    private void addLiveGamePanels(ArrayNode sections) {
        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();
            if (scoreboard != null) {
                int count = 0;
                for (Game g : scoreboard.getGames()) {
                    if (count >= 4) break;
                    sections.add(buildGamePanel(g));
                    count++;
                }
                if (count > 0) return;
            }
        } catch (Exception e) {
            log.warn("Failed to build live game cards: {}", e.getMessage());
        }

        // Mock fallback
        sections.add(mockGamePanel("lp-g1", "GSW", 1610612744, "LAC", 1610612746, "Q2 8:30", 2));
        sections.add(mockGamePanel("lp-g2", "MIA", 1610612748, "NYK", 1610612752, "8:00 PM ET", 1));
    }

    // ── Reusable section builders ──────────────────────────────────────

    private ObjectNode buildSectionHeader(String slug, String title,
                                           String actionLabel, String actionUri) {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug + "-header");
        Section section = atomicBuilder.buildSectionHeader(sectionId, title, null, actionLabel, actionUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.sectionHeaderSurface());
        return objectMapper.valueToTree(section);
    }

    private ObjectNode buildSectionHeader(String slug, String title, String subtitle,
                                           String actionLabel, String actionUri) {
        String contentSourceId = "feed:watch";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug + "-header");
        Section section = atomicBuilder.buildSectionHeader(sectionId, title, subtitle, actionLabel, actionUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.sectionHeaderSurface());
        return objectMapper.valueToTree(section);
    }

    /**
     * Emit a titled content rail as TWO sections — a SectionHeader followed
     * by a title-less rail. The SectionHeader's surface
     * ({@link SduiUtils#sectionHeaderSurface()}) is the single source of
     * truth for the title→rail gap app-wide, so every screen has the same
     * header rhythm. Callers that pass a null title get just the rail.
     */
    private void addContentRail(ArrayNode sections, String contentSourceId, String title, String[][] cards) {
        if (title != null) {
            sections.add(buildSectionHeader(contentSourceId, title, null, null));
        }
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildContentRail(sectionId, null, null, cards);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        sections.add(objectMapper.valueToTree(section));
    }

    /**
     * Emit a titled video carousel as TWO sections — a SectionHeader
     * (carrying both title and subtitle) followed by a title-less carousel.
     * Same rationale as {@link #addContentRail}: one header surface, one
     * consistent app-wide rhythm.
     */
    private void addVideoCarousel(ArrayNode sections, String contentSourceId, String title,
                                   String subtitle, String[][] items) {
        if (title != null) {
            sections.add(buildSectionHeader(contentSourceId, title, subtitle, null, null));
        }
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildVideoCarousel(sectionId, null, null, null, items);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        sections.add(objectMapper.valueToTree(section));
    }

    private ObjectNode buildPromoBanner(String contentSourceId, String headline, String subhead,
                                         String backgroundUrl, String targetUri) {
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildPromoBanner(sectionId, null, null, headline, subhead,
                null, "Learn More", targetUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.subscribeSurface(
                "#0C1B3A",
                tokens.color("nba.label.accent.brand"),
                20));
        return objectMapper.valueToTree(section);
    }

    private ObjectNode buildGamePanel(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        String contentSourceId = "stats-api:game-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        int gameStatus = game.getGameStatus();
        Section section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "standard",
                gameId,
                gameStatus,
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
                null,
                atomicBuilder.gamePanelTeam(game.getAwayTeam()),
                atomicBuilder.gamePanelTeam(game.getHomeTeam()),
                null,
                "nba://game/" + gameId,
                staticPolicy(),
                null,
                objectMapper.valueToTree(surfaces.gamePanelSurface()));
        section.setContentSourceId(contentSourceId);
        return objectMapper.valueToTree(section);
    }

    private ObjectNode mockGamePanel(String mockId, String awayTri, int awayTeamId,
                                     String homeTri, int homeTeamId,
                                     String statusText, int gameStatus) {
        String contentSourceId = "stats-api:game-" + mockId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        int awayScore = gameStatus == 2 ? 55 : 0;
        int homeScore = gameStatus == 2 ? 48 : 0;
        Section section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "standard",
                mockId,
                gameStatus,
                statusText,
                null,
                new AtomicCompositeBuilder.GamePanelTeam(awayTri, awayScore, SduiUtils.teamLogoUrl(awayTeamId)),
                new AtomicCompositeBuilder.GamePanelTeam(homeTri, homeScore, SduiUtils.teamLogoUrl(homeTeamId)),
                null,
                "nba://game/" + mockId,
                staticPolicy(),
                null,
                objectMapper.valueToTree(surfaces.gamePanelSurface()));
        section.setContentSourceId(contentSourceId);
        return objectMapper.valueToTree(section);
    }

    // ── New section type builders ──────────────────────────────────────

    /** NbaTvSchedule section — hero promo + time-slot list. */
    private ObjectNode buildNbaTvSchedule() {
        String contentSourceId = "cms:nbatv-schedule";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] slots = {
                {"slot-1", "NBA GameTime", "Highlights & analysis", "19:00", "true", "nba://watch/nbatv-live"},
                {"slot-2", "NBA Inside Stuff", "Player features", "20:00", "false", "nba://watch/inside-stuff"},
                {"slot-3", "CLE vs NYK", "Eastern Conference matchup", "20:30", "false", "nba://game/0022400612"},
                {"slot-4", "NBA Action", "Weekly highlights show", "23:00", "false", "nba://watch/nba-action"}
        };
        Section section = atomicBuilder.buildNbaTvSchedule(
                sectionId, null,
                FALLBACK_THUMB,
                "NBA GameTime",
                "LIVE — Nightly highlights & analysis",
                true, slots);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.cardSurface());
        return objectMapper.valueToTree(section);
    }

    /**
     * SubscribeUpsell section, inline-banner layout.
     *
     * Reserved SDK integration point: the visible surface is expressed as an
     * atomic tree under {@code data.ui}. {@code data.ctaAction} is retained as
     * the pre-SDK fallback action; once the IAP SDK lands, it takes over the
     * CTA tap and reads the IAP product identifiers from a future
     * {@code data.tiers} list.
     */
    private ObjectNode buildSubscribeUpsellBanner(String title, String subtitle,
                                             String backgroundUrl, String ctaLabel,
                                             String ctaUri) {
        String contentSourceId = "product:league-pass-banner";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeUpsell");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "SubscribeUpsell");
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", objectMapper.valueToTree(surfaces.subscribeSurface(
                tokens.color("nba.label.accent.brand"),
                "#862633",
                20)));

        ObjectNode ctaAction = objectMapper.createObjectNode();
        ctaAction.put("trigger", "onActivate");
        ctaAction.put("type", "navigate");
        ctaAction.put("targetUri", ctaUri);

        ObjectNode root = atomicBuilder.container("column", "start", "start");
        root.put("widthMode", "fill");
        root.put("gap", tokens.spacing("sm"));
        ArrayNode children = objectMapper.createArrayNode();
        ObjectNode titleText = atomicBuilder.text(title, "titleMedium", "bold", "#FFFFFF", null);
        addHeading(objectMapper, titleText, title, 2);
        children.add(titleText);
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodySmall", null, "rgba(255,255,255,0.85)", null));
        }
        children.add(atomicBuilder.spacer(tokens.spacing("md")));
        children.add(atomicBuilder.button(ctaLabel, "primary", ctaAction.deepCopy()));
        root.set("children", children);

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.set("ctaAction", ctaAction);
        section.set("data", data);
        return section;
    }

    /**
     * SubscribeUpsell section, full-screen-hero layout with pricing tiers.
     *
     * Reserved SDK integration point: the full visible surface (logo, title,
     * subtitle, feature list, tier cards) is expressed as an atomic tree
     * under {@code data.ui}. {@code data.tiers} is retained for the future
     * IAP SDK to bind product identifiers; the renderer reads nothing from
     * it today.
     */
    private ObjectNode buildSubscribeUpsellHero() {
        String contentSourceId = "product:league-pass";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeUpsell");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "SubscribeUpsell");
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", objectMapper.valueToTree(surfaces.subscribeSurface(
                "#0C1B3A",
                tokens.color("nba.label.accent.brand"),
                24)));

        ObjectNode root = buildSubscribeUpsellHeroUi(
                "NBA League Pass",
                "Your courtside seat to every game",
                FALLBACK_THUMB,
                new String[]{
                        "Watch every out-of-market game live or on demand",
                        "Choose home or away broadcast feeds",
                        "NBA TV included with all plans",
                        "Condensed game replays — full game in ~12 minutes"
                },
                new TierSpec[]{
                        new TierSpec("League Pass", "$14.99/mo", "MOST POPULAR",
                                new String[]{"Live & on-demand out-of-market games", "NBA TV included"},
                                "Subscribe", "nba://subscribe/league-pass/standard"),
                        new TierSpec("League Pass Premium", "$22.99/mo", "BEST VALUE",
                                new String[]{"Everything in League Pass",
                                        "No in-game ads on select feeds",
                                        "Up to 4 simultaneous streams"},
                                "Subscribe", "nba://subscribe/league-pass/premium")
                });

        ArrayNode tiers = objectMapper.createArrayNode();
        tiers.add(tierProductId("lp-standard", "League Pass", "$14.99/mo"));
        tiers.add(tierProductId("lp-premium", "League Pass Premium", "$22.99/mo"));

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.set("tiers", tiers);
        section.set("data", data);
        return section;
    }

    /**
     * Build the atomic tree for a SubscribeUpsell hero layout. Extracted so
     * both the Watch composer and the demo composer can share the same
     * visual composition.
     */
    private ObjectNode buildSubscribeUpsellHeroUi(String title, String subtitle, String logoUrl,
                                             String[] features, TierSpec[] tierSpecs) {
        ObjectNode root = atomicBuilder.container("column", "start", "center");
        root.put("gap", 8); // §3.6: no semantic spacing token for 8
        root.put("widthMode", "fill");
        ArrayNode children = objectMapper.createArrayNode();

        if (logoUrl != null) {
            ObjectNode logoImg = atomicBuilder.image(logoUrl, 0, 64, "contain");
            addHidden(objectMapper, logoImg);
            children.add(logoImg);
            children.add(atomicBuilder.spacer(tokens.spacing("md")));
        }
        ObjectNode heroTitle = atomicBuilder.text(title, "headlineMedium", "bold", tokens.color("nba.label-dark.primary"), null);
        addHeading(objectMapper, heroTitle, title, 2);
        children.add(heroTitle);
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodyLarge", null, tokens.color("nba.label-dark.primary"), null));
        }
        children.add(atomicBuilder.spacer(tokens.spacing("lg")));

        ObjectNode featuresCol = atomicBuilder.container("column", "start", "start");
        featuresCol.put("gap", 8); // §3.6: no semantic spacing token for 8
        featuresCol.put("widthMode", "fill");
        ArrayNode featureChildren = objectMapper.createArrayNode();
        for (String feature : features) {
            ObjectNode row = atomicBuilder.container("row", "start", "center");
            row.put("gap", 8); // §3.6: no semantic spacing token for 8
            row.put("widthMode", "fill");
            ArrayNode rowChildren = objectMapper.createArrayNode();
            rowChildren.add(atomicBuilder.text("✓", "bodyLarge", "bold", "token:nba.color.feedback.success.70", null));
            ObjectNode featureText = atomicBuilder.text(feature, "bodyLarge", null, tokens.color("nba.label-dark.primary"), null);
            featureText.put("flex", 1);
            rowChildren.add(featureText);
            row.set("children", rowChildren);
            featureChildren.add(row);
        }
        featuresCol.set("children", featureChildren);
        children.add(featuresCol);

        children.add(atomicBuilder.spacer(20));

        ObjectNode tiersCol = atomicBuilder.container("column", "start", "stretch");
        tiersCol.put("gap", tokens.spacing("lg"));
        tiersCol.put("widthMode", "fill");
        ArrayNode tierChildren = objectMapper.createArrayNode();
        for (TierSpec t : tierSpecs) {
            tierChildren.add(buildTierUi(t));
        }
        tiersCol.set("children", tierChildren);
        children.add(tiersCol);

        root.set("children", children);
        return root;
    }

    /** Build an atomic Container that visually represents one subscription tier. */
    private ObjectNode buildTierUi(TierSpec t) {
        ObjectNode card = atomicBuilder.container("column", "start", "start");
        card.put("gap", 6); // §3.6: no semantic spacing token for 6
        card.put("background", tokens.color("nba.color.t-white.10"));
        card.put("cornerRadius", tokens.radius("lg"));
        card.set("padding", atomicBuilder.padding(
                22, // §3.6: no semantic spacing token for 22
                22, // §3.6: no semantic spacing token for 22
                20, // §3.6: no semantic spacing token for 20
                20  // §3.6: no semantic spacing token for 20
        ));
        card.put("widthMode", "fill");

        ArrayNode cardChildren = objectMapper.createArrayNode();
        if (t.badgeText != null) {
            cardChildren.add(atomicBuilder.text(t.badgeText, "labelMedium", "bold", tokens.color("nba.color.secondary.70"), null));
        }
        ObjectNode tierName = atomicBuilder.text(t.name, "titleLarge", "bold", tokens.color("nba.label-dark.primary"), null);
        addHeading(objectMapper, tierName, t.name, 3);
        cardChildren.add(tierName);
        cardChildren.add(atomicBuilder.text(t.price, "headlineSmall", "bold", tokens.color("nba.label-dark.primary"), null));

        if (t.features != null) {
            for (String f : t.features) {
                cardChildren.add(atomicBuilder.text("• " + f, "bodyMedium", null, tokens.color("nba.label-dark.primary"), null));
            }
        }
        cardChildren.add(atomicBuilder.spacer(10));

        ObjectNode tierAction = objectMapper.createObjectNode();
        tierAction.put("trigger", "onActivate");
        tierAction.put("type", "navigate");
        tierAction.put("targetUri", t.ctaUri);
        cardChildren.add(atomicBuilder.button(t.ctaLabel, "primary", tierAction));

        card.set("children", cardChildren);
        return card;
    }

    private ObjectNode tierProductId(String id, String name, String price) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", id);
        n.put("name", name);
        n.put("price", price);
        return n;
    }

    /** Compact value object describing one subscription tier for the hero UI builder. */
    private static final class TierSpec {
        final String name, price, badgeText;
        final String[] features;
        final String ctaLabel, ctaUri;

        TierSpec(String name, String price, String badgeText,
                 String[] features, String ctaLabel, String ctaUri) {
            this.name = name;
            this.price = price;
            this.badgeText = badgeText;
            this.features = features;
            this.ctaLabel = ctaLabel;
            this.ctaUri = ctaUri;
        }
    }

    /** AdSlot section — ad placement primitive (ADR-007). */
    private ObjectNode buildAdSlot(String adUnitPath) {
        String contentSourceId = "ads:gam-" + adUnitPath.replace("/", "-");
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "AdSlot");
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", objectMapper.valueToTree(surfaces.adSlotSurface()));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("provider", "gam");
        data.put("adUnitPath", "/21765378015/nba.com/" + adUnitPath);

        ArrayNode sizes = objectMapper.createArrayNode();
        ArrayNode size320 = objectMapper.createArrayNode();
        size320.add(320); size320.add(50);
        sizes.add(size320);
        ArrayNode size728 = objectMapper.createArrayNode();
        size728.add(728); size728.add(90);
        sizes.add(size728);
        data.set("sizes", sizes);

        ObjectNode targeting = objectMapper.createObjectNode();
        targeting.put("section", "watch");
        data.set("targeting", targeting);

        data.put("collapseOnEmpty", true);
        data.put("label", "Advertisement");

        ObjectNode placeholder = objectMapper.createObjectNode();
        placeholder.put("backgroundColor", "token:nba.bg.tertiary");
        placeholder.put("text", "Advertisement");
        data.set("placeholder", placeholder);

        section.set("data", data);
        return section;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
