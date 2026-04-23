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

        // Hero video carousel
        sections.add(buildVideoCarousel("featured-highlights", "Tonight's Highlights", null,
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
                }));

        // Subscribe banner (inline upsell)
        sections.add(buildSubscribeBanner("watch-subscribe-inline",
                "NBA League Pass",
                "Watch every out-of-market game live or on demand",
                FALLBACK_THUMB,
                "Subscribe Now", "nba://subscribe/league-pass"));

        // NBA Originals carousel
        sections.add(buildVideoCarousel("featured-originals", "NBA Originals", null,
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
                }));

        // Ad slot mid-page
        sections.add(buildAdSlot("watch-ad-mid", "watch/featured/mid"));

        // Content rail for articles
        sections.add(buildContentRail("featured-stories", "Trending Stories",
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
                }));

        return sections;
    }

    // ── NBA TV tab sections ────────────────────────────────────────────

    private ArrayNode buildNbaTvSections() {
        ArrayNode sections = objectMapper.createArrayNode();

        // NBA TV Schedule (hero + time-slot list)
        sections.add(buildNbaTvSchedule());

        // Video carousel of recent NBA TV shows
        sections.add(buildVideoCarousel("nbatv-shows", "Popular Shows", null,
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
                }));

        // Ad slot
        sections.add(buildAdSlot("nbatv-ad", "watch/nbatv/mid"));

        // Classic games carousel
        sections.add(buildVideoCarousel("nbatv-classics", "Classic Games", "Relive the greatest moments",
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
                }));

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

        // Video carousel for condensed games
        sections.add(buildVideoCarousel("lp-condensed", "Condensed Games", "Full games in ~12 minutes",
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
                }));

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
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildContentRail(String id, String title, String[][] cards) {
        ObjectNode section = atomicBuilder.buildContentRail(id, null, title, cards);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildPromoBanner(String id, String headline, String subhead,
                                         String backgroundUrl, String targetUri) {
        ObjectNode section = atomicBuilder.buildPromoBanner(id, null, null, headline, subhead,
                null, "Learn More", targetUri);
        section.set("display", utils.subscribeCardDisplay(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                20));
        return section;
    }

    private ObjectNode buildGamePanel(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "watch-game-" + gameId);
        section.put("type", "GamePanel");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.set("homeTeam", mapTeam(game.path("homeTeam")));
        data.set("awayTeam", mapTeam(game.path("awayTeam")));

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + gameId);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("display", utils.gamePanelDisplay());
        return section;
    }

    private ObjectNode mockGamePanel(String id, String awayTri, int awayTeamId,
                                     String homeTri, int homeTeamId,
                                     String statusText, int gameStatus) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "GamePanel");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", id);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", statusText);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", awayTeamId);
        away.put("teamTricode", awayTri);
        away.put("teamName", awayTri);
        away.put("teamCity", awayTri);
        away.put("score", gameStatus == 2 ? 55 : 0);
        away.put("logoUrl", SduiUtils.teamLogoUrl(awayTeamId));
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeTeamId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", gameStatus == 2 ? 48 : 0);
        home.put("logoUrl", SduiUtils.teamLogoUrl(homeTeamId));
        data.set("homeTeam", home);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + id);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("display", utils.gamePanelDisplay());
        return section;
    }

    // ── New section type builders ──────────────────────────────────────

    /**
     * VideoCarousel section — horizontal row of video thumbnails.
     * items: [id, title, subtitle, thumbnailUrl, duration, badgeText, actionUri]
     */

    private ObjectNode buildVideoCarousel(String id, String title, String subtitle,
                                           String[][] items) {
        ObjectNode section = atomicBuilder.buildVideoCarousel(id, null, title, subtitle, items);
        section.set("display", utils.railDisplay());
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
        section.set("display", utils.railDisplay());
        return section;
    }

    /**
     * SubscribeBanner section — inline subscription upsell.
     */
    private ObjectNode buildSubscribeBanner(String id, String title, String subtitle,
                                             String backgroundUrl, String ctaLabel,
                                             String ctaUri) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "SubscribeBanner");
        section.set("refreshPolicy", staticPolicy());
        section.set("display", utils.subscribeCardDisplay(
                ColorTokens.BRAND_NBA,
                "#862633",
                20));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", title);
        if (subtitle != null) data.put("subtitle", subtitle);
        if (backgroundUrl != null) {
            ObjectNode bgImage = objectMapper.createObjectNode();
            bgImage.put("imageUrl", backgroundUrl);
            data.set("background", bgImage);
        }

        data.put("ctaLabel", ctaLabel);

        ObjectNode ctaAction = objectMapper.createObjectNode();
        ctaAction.put("trigger", "onTap");
        ctaAction.put("type", "navigate");
        ctaAction.put("targetUri", ctaUri);
        data.set("ctaAction", ctaAction);

        section.set("data", data);
        return section;
    }

    /** SubscribeHero section — full-screen upsell with pricing tiers. */
    private ObjectNode buildSubscribeHero() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "lp-subscribe-hero");
        section.put("type", "SubscribeHero");
        section.set("refreshPolicy", staticPolicy());
        section.set("display", utils.subscribeCardDisplay(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                24));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "NBA League Pass");
        data.put("subtitle", "Your courtside seat to every game");
        ObjectNode heroBg = objectMapper.createObjectNode();
        heroBg.put("imageUrl", FALLBACK_THUMB);
        data.set("background", heroBg);
        data.put("logoUrl", FALLBACK_THUMB);

        ArrayNode features = objectMapper.createArrayNode();
        features.add("Watch every out-of-market game live or on demand");
        features.add("Choose home or away broadcast feeds");
        features.add("NBA TV included with all plans");
        features.add("Condensed game replays — full game in ~12 minutes");
        data.set("features", features);

        ArrayNode tiers = objectMapper.createArrayNode();

        ObjectNode standard = objectMapper.createObjectNode();
        standard.put("id", "lp-standard");
        standard.put("name", "League Pass");
        standard.put("price", "$14.99/mo");
        standard.put("badgeText", "MOST POPULAR");
        ArrayNode stdFeatures = objectMapper.createArrayNode();
        stdFeatures.add("Live & on-demand out-of-market games");
        stdFeatures.add("NBA TV included");
        standard.set("features", stdFeatures);
        standard.put("ctaLabel", "Subscribe");
        ObjectNode stdAction = objectMapper.createObjectNode();
        stdAction.put("trigger", "onTap");
        stdAction.put("type", "navigate");
        stdAction.put("targetUri", "nba://subscribe/league-pass/standard");
        standard.set("ctaAction", stdAction);
        tiers.add(standard);

        ObjectNode premium = objectMapper.createObjectNode();
        premium.put("id", "lp-premium");
        premium.put("name", "League Pass Premium");
        premium.put("price", "$22.99/mo");
        premium.put("badgeText", "BEST VALUE");
        ArrayNode premFeatures = objectMapper.createArrayNode();
        premFeatures.add("Everything in League Pass");
        premFeatures.add("No in-game ads on select feeds");
        premFeatures.add("Up to 4 simultaneous streams");
        premium.set("features", premFeatures);
        premium.put("ctaLabel", "Subscribe");
        ObjectNode premAction = objectMapper.createObjectNode();
        premAction.put("trigger", "onTap");
        premAction.put("type", "navigate");
        premAction.put("targetUri", "nba://subscribe/league-pass/premium");
        premium.set("ctaAction", premAction);
        tiers.add(premium);

        data.set("tiers", tiers);
        section.set("data", data);
        return section;
    }

    /** AdSlot section — ad placement primitive (ADR-007). */
    private ObjectNode buildAdSlot(String id, String adUnitPath) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "AdSlot");
        section.set("refreshPolicy", staticPolicy());
        section.set("display", utils.defaultSectionDisplay());

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

    private ObjectNode mapTeam(JsonNode team) {
        ObjectNode mapped = objectMapper.createObjectNode();
        mapped.put("teamId", team.path("teamId").asInt());
        mapped.put("teamTricode", team.path("teamTricode").asText(""));
        mapped.put("teamName", team.path("teamName").asText(""));
        mapped.put("teamCity", team.path("teamCity").asText(""));
        mapped.put("score", team.path("score").asInt(0));
        int wins = team.path("wins").asInt(0);
        int losses = team.path("losses").asInt(0);
        mapped.put("record", wins + "-" + losses);
        mapped.put("logoUrl", SduiUtils.teamLogoUrl(team.path("teamId").asText()));
        return mapped;
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
