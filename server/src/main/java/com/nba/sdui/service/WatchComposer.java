package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public WatchComposer(ObjectMapper objectMapper,
                         StatsApiClient statsApiClient,
                         SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    public JsonNode composeWatch(String traceId, String locale) {
        log.info("Composing Watch screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "watch");
        response.put("title", "Watch");
        response.put("analyticsId", "watch");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("watch"));

        ArrayNode sections = objectMapper.createArrayNode();

        // Single TabGroup section that contains the three tabs
        sections.add(buildTabGroup());

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Tab group ──────────────────────────────────────────────────────

    private ObjectNode buildTabGroup() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "watch-tabs");
        section.put("type", "TabGroup");
        section.put("analyticsId", "watch_tabs");
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
        sections.add(buildAdSlot("watch-ad-top", "watch/featured/top"));

        addVideoCarousel(sections, "featured-highlights", "Tonight's Highlights", null,
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

        // Subscribe banner (inline upsell)
        sections.add(buildSubscribeBanner("watch-subscribe-inline",
                "NBA League Pass",
                "Watch every out-of-market game live or on demand",
                FALLBACK_THUMB,
                "Subscribe Now", "nba://subscribe/league-pass"));

        addVideoCarousel(sections, "featured-originals", "NBA Originals", null,
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
        sections.add(buildAdSlot("watch-ad-mid", "watch/featured/mid"));

        addContentRail(sections, "featured-stories", "Trending Stories",
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

        addVideoCarousel(sections, "nbatv-shows", "Popular Shows", null,
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

        sections.add(buildAdSlot("nbatv-ad", "watch/nbatv/mid"));

        addVideoCarousel(sections, "nbatv-classics", "Classic Games", "Relive the greatest moments",
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

        // Subscribe hero (full-screen upsell with pricing tiers)
        sections.add(buildSubscribeHero());

        sections.add(buildSectionHeader("lp-live-header", "Live Games", null, null));

        // Pull real games if available
        addLiveGamePanels(sections);

        addVideoCarousel(sections, "lp-condensed", "Condensed Games", "Full games in ~12 minutes",
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
        sections.add(buildAdSlot("lp-ad", "watch/leaguepass/mid"));

        return sections;
    }

    private void addLiveGamePanels(ArrayNode sections) {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard != null) {
                JsonNode games = scoreboard.path("scoreboard").path("games");
                int count = 0;
                for (JsonNode g : games) {
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

    private ObjectNode buildSectionHeader(String id, String title,
                                           String actionLabel, String actionUri) {
        ObjectNode section = atomicBuilder.buildSectionHeader(id, title, null, actionLabel, actionUri);
        section.set("surface", utils.sectionHeaderSurface());
        return section;
    }

    private ObjectNode buildSectionHeader(String id, String title, String subtitle,
                                           String actionLabel, String actionUri) {
        ObjectNode section = atomicBuilder.buildSectionHeader(id, title, subtitle, actionLabel, actionUri);
        section.set("surface", utils.sectionHeaderSurface());
        return section;
    }

    /**
     * Emit a titled content rail as TWO sections — a SectionHeader followed
     * by a title-less rail. The SectionHeader's surface
     * ({@link SduiUtils#sectionHeaderSurface()}) is the single source of
     * truth for the title→rail gap app-wide, so every screen has the same
     * header rhythm. Callers that pass a null title get just the rail.
     */
    private void addContentRail(ArrayNode sections, String id, String title, String[][] cards) {
        if (title != null) {
            sections.add(buildSectionHeader(id + "-header", title, null, null));
        }
        ObjectNode section = atomicBuilder.buildContentRail(id, null, null, cards);
        section.set("surface", utils.railSurface());
        sections.add(section);
    }

    /**
     * Emit a titled video carousel as TWO sections — a SectionHeader
     * (carrying both title and subtitle) followed by a title-less carousel.
     * Same rationale as {@link #addContentRail}: one header surface, one
     * consistent app-wide rhythm.
     */
    private void addVideoCarousel(ArrayNode sections, String id, String title,
                                   String subtitle, String[][] items) {
        if (title != null) {
            sections.add(buildSectionHeader(id + "-header", title, subtitle, null, null));
        }
        ObjectNode section = atomicBuilder.buildVideoCarousel(id, null, null, null, items);
        section.set("surface", utils.railSurface());
        sections.add(section);
    }

    private ObjectNode buildPromoBanner(String id, String headline, String subhead,
                                         String backgroundUrl, String targetUri) {
        ObjectNode section = atomicBuilder.buildPromoBanner(id, null, null, headline, subhead,
                null, "Learn More", targetUri);
        section.set("surface", utils.subscribeSurface(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                20));
        return section;
    }

    private ObjectNode buildGamePanel(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);
        return atomicBuilder.buildGamePanelComposite(
                "watch-game-" + gameId,
                null,
                "standard",
                gameId,
                gameStatus,
                game.path("gameStatusText").asText(""),
                null,
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam")),
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam")),
                null,
                "nba://game/" + gameId,
                staticPolicy(),
                null,
                utils.gamePanelSurface());
    }

    private ObjectNode mockGamePanel(String id, String awayTri, int awayTeamId,
                                     String homeTri, int homeTeamId,
                                     String statusText, int gameStatus) {
        int awayScore = gameStatus == 2 ? 55 : 0;
        int homeScore = gameStatus == 2 ? 48 : 0;
        return atomicBuilder.buildGamePanelComposite(
                id,
                null,
                "standard",
                id,
                gameStatus,
                statusText,
                null,
                new AtomicCompositeBuilder.GamePanelTeam(awayTri, awayScore, SduiUtils.teamLogoUrl(awayTeamId)),
                new AtomicCompositeBuilder.GamePanelTeam(homeTri, homeScore, SduiUtils.teamLogoUrl(homeTeamId)),
                null,
                "nba://game/" + id,
                staticPolicy(),
                null,
                utils.gamePanelSurface());
    }

    // ── New section type builders ──────────────────────────────────────

    /**
     * VideoCarousel section — horizontal row of video thumbnails.
     * items: [id, title, subtitle, thumbnailUrl, duration, badgeText, actionUri]
     */

    private ObjectNode buildVideoCarousel(String id, String title, String subtitle,
                                           String[][] items) {
        ObjectNode section = atomicBuilder.buildVideoCarousel(id, null, title, subtitle, items);
        section.set("surface", utils.railSurface());
        return section;
    }

    /** NbaTvSchedule section — hero promo + time-slot list. */
    private ObjectNode buildNbaTvSchedule() {
        String[][] slots = {
                {"slot-1", "NBA GameTime", "Highlights & analysis", "19:00", "true", "nba://watch/nbatv-live"},
                {"slot-2", "NBA Inside Stuff", "Player features", "20:00", "false", "nba://watch/inside-stuff"},
                {"slot-3", "CLE vs NYK", "Eastern Conference matchup", "20:30", "false", "nba://game/0022400612"},
                {"slot-4", "NBA Action", "Weekly highlights show", "23:00", "false", "nba://watch/nba-action"}
        };
        ObjectNode section = atomicBuilder.buildNbaTvSchedule(
                "nbatv-schedule", null,
                FALLBACK_THUMB,
                "NBA GameTime",
                "LIVE — Nightly highlights & analysis",
                true, slots);
        section.set("surface", utils.cardSurface());
        return section;
    }

    /**
     * SubscribeBanner section — inline subscription upsell.
     *
     * Reserved SDK integration point: the visible surface is expressed as an
     * atomic tree under {@code data.ui}. {@code data.ctaAction} is retained as
     * the pre-SDK fallback action; once the IAP SDK lands, it takes over the
     * CTA tap and reads the IAP product identifiers from a future
     * {@code data.tiers} list.
     */
    private ObjectNode buildSubscribeBanner(String id, String title, String subtitle,
                                             String backgroundUrl, String ctaLabel,
                                             String ctaUri) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "SubscribeBanner");
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", utils.subscribeSurface(
                ColorTokens.BRAND_NBA,
                "#862633",
                20));

        ObjectNode ctaAction = objectMapper.createObjectNode();
        ctaAction.put("trigger", "onTap");
        ctaAction.put("type", "navigate");
        ctaAction.put("targetUri", ctaUri);

        ObjectNode root = atomicBuilder.container("column", "start", "start");
        root.put("gap", 4);
        ArrayNode children = objectMapper.createArrayNode();
        children.add(atomicBuilder.text(title, "titleMedium", "bold", "#FFFFFF", null));
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodySmall", null, "rgba(255,255,255,0.85)", null));
        }
        children.add(atomicBuilder.spacer(8));
        children.add(atomicBuilder.button(ctaLabel, "primary", ctaAction.deepCopy()));
        root.set("children", children);

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.set("ctaAction", ctaAction);
        section.set("data", data);
        return section;
    }

    /**
     * SubscribeHero section — full-screen upsell with pricing tiers.
     *
     * Reserved SDK integration point: the full visible surface (logo, title,
     * subtitle, feature list, tier cards) is expressed as an atomic tree
     * under {@code data.ui}. {@code data.tiers} is retained for the future
     * IAP SDK to bind product identifiers; the renderer reads nothing from
     * it today.
     */
    private ObjectNode buildSubscribeHero() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "lp-subscribe-hero");
        section.put("type", "SubscribeHero");
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", utils.subscribeSurface(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                24));

        ObjectNode root = buildSubscribeHeroUi(
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
     * Build the atomic tree for a SubscribeHero. Extracted so both the Watch
     * composer and the demo composer can share the same visual composition.
     */
    private ObjectNode buildSubscribeHeroUi(String title, String subtitle, String logoUrl,
                                             String[] features, TierSpec[] tierSpecs) {
        ObjectNode root = atomicBuilder.container("column", "start", "center");
        root.put("gap", 6);
        ArrayNode children = objectMapper.createArrayNode();

        if (logoUrl != null) {
            children.add(atomicBuilder.image(logoUrl, 0, 48, "contain"));
            children.add(atomicBuilder.spacer(8));
        }
        children.add(atomicBuilder.text(title, "headlineSmall", "bold", "#FFFFFF", null));
        if (subtitle != null) {
            children.add(atomicBuilder.text(subtitle, "bodyMedium", null, "rgba(255,255,255,0.8)", null));
        }
        children.add(atomicBuilder.spacer(12));

        ObjectNode featuresCol = atomicBuilder.container("column", "start", "start");
        featuresCol.put("gap", 6);
        ArrayNode featureChildren = objectMapper.createArrayNode();
        for (String feature : features) {
            ObjectNode row = atomicBuilder.container("row", "start", "center");
            row.put("gap", 8);
            ArrayNode rowChildren = objectMapper.createArrayNode();
            rowChildren.add(atomicBuilder.text("✓", "bodyMedium", "bold", "#FFFFFF", null));
            rowChildren.add(atomicBuilder.text(feature, "bodyMedium", null, "rgba(255,255,255,0.85)", null));
            row.set("children", rowChildren);
            featureChildren.add(row);
        }
        featuresCol.set("children", featureChildren);
        children.add(featuresCol);

        children.add(atomicBuilder.spacer(16));

        ObjectNode tiersCol = atomicBuilder.container("column", "start", "start");
        tiersCol.put("gap", 12);
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
        card.put("gap", 4);
        card.put("background", "rgba(255,255,255,0.1)");
        card.put("cornerRadius", 12);
        card.set("padding", atomicBuilder.padding(16, 16, 16, 16));
        card.put("fillWidth", true);

        ArrayNode cardChildren = objectMapper.createArrayNode();
        if (t.badgeText != null) {
            cardChildren.add(atomicBuilder.text(t.badgeText, "labelSmall", "bold", "#FFDD00", null));
        }
        cardChildren.add(atomicBuilder.text(t.name, "titleMedium", "bold", "#FFFFFF", null));
        cardChildren.add(atomicBuilder.text(t.price, "titleLarge", "bold", "#FFFFFF", null));

        if (t.features != null) {
            for (String f : t.features) {
                cardChildren.add(atomicBuilder.text("• " + f, "bodySmall", null, "rgba(255,255,255,0.85)", null));
            }
        }
        cardChildren.add(atomicBuilder.spacer(8));

        ObjectNode tierAction = objectMapper.createObjectNode();
        tierAction.put("trigger", "onTap");
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
    private ObjectNode buildAdSlot(String id, String adUnitPath) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "AdSlot");
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", utils.defaultSurface());

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
        placeholder.put("backgroundColor", "token:color.surface.sunken");
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
