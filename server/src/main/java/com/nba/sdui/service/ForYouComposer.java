package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composes the "For You" SDUI screen using the same atomic composite
 * vocabulary the Kitchen demo screen exercises. The intent is parity:
 * if a composite renders correctly in Kitchen it renders correctly here,
 * because the surface, image-shape, and helper choices match.
 *
 * <pre>
 *   1.  StoryCircleRail             – Following avatars (square images, live/new badges)
 *   2.  CinematicHeroCarousel       – full-bleed editorial hero (image + scrim + CTA)
 *   3.  SectionHeaderComposite      – "TOP STORIES"
 *   4.  AtomicComposite             – dense vertical headline list (thumb + title)
 *   5.  AdSlot                      – mid_feed_1
 *   6.  SectionHeaderComposite      – "TONIGHT'S GAMES" + "See Schedule >"
 *   7.  FeaturedLiveGameHero        – live/upcoming games (SSE when API live; poll on mock fallback)
 *   8.  SectionHeaderComposite      – "TRENDING NOW"
 *   9.  ContentRail                 – trending articles/videos
 *  10.  AdSlot                      – mid_feed_2
 *  11.  SectionHeaderComposite      – "LEAGUE PASS PICKS" + "Browse League Pass >"
 *  12.  ContentRail                 – LP picks
 *  13.  AdSlot                      – mid_feed_3
 *  14.  LeagueCardRail              – other league destinations
 *  15.  SectionHeaderComposite      – "AROUND THE LEAGUE"
 *  16.  UtilityCardGrid             – Standings / Stats Leaders / …
 *  17.  AtomicComposite             – VOD playlist (grouped-list rows)
 * </pre>
 *
 * <p>Surface policy: card-chromed composites (hero, rails, grid, vod
 * playlist) get {@code railSurface()} — margins only, no outer card.
 * Section headers get {@code sectionHeaderSurface()}. AdSlots use
 * {@code adSlotSurface()} for server-owned card chrome.
 * No composite is wrapped in {@code gamePanelSurface()} — that surface
 * was for the legacy chrome-less {@code GamePanel} section type.
 */
@Component
public class ForYouComposer {

    /**
     * Hero key-art fallback (16:9). Uses same-origin placeholder SVGs
     * for reliable rendering in the demo.
     * Server-driven; clients never derive this URL.
     */
    private static final String FALLBACK_KEY_ART_LIVE = DemoImageUrls.hero("arena");
    private static final String FALLBACK_KEY_ART_UPCOMING = DemoImageUrls.hero("court");

    /**
     * Square fallback for story-circle items (avatars).
     */
    private static final String FALLBACK_STORY_AVATAR = DemoImageUrls.avatar("story");

    private static final Logger log = LoggerFactory.getLogger(ForYouComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final AtomicCompositeBuilder atomicBuilder;
    private final SectionRefreshService sectionRefreshService;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ForYouComposer(ObjectMapper objectMapper,
                          StatsApiClient statsApiClient,
                          SduiUtils utils,
                          SectionSurfaces surfaces,
                          SectionRefreshService sectionRefreshService) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.surfaces = surfaces;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
        this.sectionRefreshService = sectionRefreshService;
    }

    @PostConstruct
    private void registerSectionResolvers() {
        String sectionId = SectionIdDeriver.derive(
                "stats-api:scoreboard", "AtomicComposite", "tonights-games-hero");
        sectionRefreshService.registerResolver(sectionId, (id, ctx) -> buildTonightsGamesHero());
    }

    public JsonNode composeForYou(String traceId, String locale) {
        log.info("Composing For You screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "for-you");
        response.put("analyticsId", "for_you");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "for-you");

        ArrayNode sections = objectMapper.createArrayNode();

        sections.add(buildFollowingStoryCircleRail());
        sections.add(buildFeaturedEditorialHero());

        sections.add(buildSectionHeaderComposite("top-stories-header",
                "for_you_top_stories_header",
                "Top Stories", null, "More", "nba://news"));
        sections.add(buildTopStoriesArticleList());

        sections.add(buildAdSlot("for-you-ad-1", "for_you_ad_1",
                "/21234567/sports/nba/homepage_mid1", "mid_feed_1"));

        sections.add(buildSectionHeaderComposite("tonights-games-header",
                "for_you_tonights_games_header",
                "Tonight's Games", null, "See Schedule", "nba://scoreboard"));
        sections.add(buildTonightsGamesHero());

        sections.add(buildSectionHeaderComposite("trending-header",
                "for_you_trending_header",
                "Trending Now", null, null, null));
        sections.add(buildTrendingRail());

        sections.add(buildAdSlot("for-you-ad-2", "for_you_ad_2",
                "/21234567/sports/nba/homepage_mid2", "mid_feed_2"));

        sections.add(buildSectionHeaderComposite("lp-picks-header",
                "for_you_lp_picks_header",
                "League Pass Picks", null, "Browse League Pass", "nba://leaguepass"));
        sections.add(buildLeaguePassPicksRail());

        sections.add(buildAdSlot("for-you-ad-3", "for_you_ad_3",
                "/21234567/sports/nba/homepage_mid3", "mid_feed_3"));

        sections.add(buildOtherLeaguesRail());

        sections.add(buildSectionHeaderComposite("around-league-header",
                "for_you_around_league_header",
                "Around the League", null, null, null));
        sections.add(buildAroundTheLeagueUtilityGrid());

        sections.add(buildVodPlaylistSection());

        response.set("sections", sections);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    /**
     * Story circles use SQUARE images (team logos or square stock images).
     * Non-square sources (e.g. 260x190 player headshots) interact poorly
     * with the circular crop + overlay-pill stacking — keep this rail
     * square-source-only and the composite's overlay alignment stays
     * predictable across platforms.
     */
    private ObjectNode buildFollowingStoryCircleRail() {
        String contentSourceId = "feed:for-you-following";
        // [id, label, imageUrl, badgeText, targetUri]
        String[][] items = {
                {"story-ecf-g4", "ECF Game 4 Clash", DemoImageUrls.avatar("ecf-g4"),
                        "LIVE", "nba://video/ecf-g4-preview"},
                {"story-all-nba", "Kia All-NBA Teams", DemoImageUrls.avatar("all-nba"),
                        "NEW", "nba://article/all-nba-teams"},
                {"story-wcf", "Best Of WCF Game 4", DemoImageUrls.avatar("wcf-g4"),
                        null, "nba://video/wcf-g4-highlights"},
                {"story-okc-sa", "Inside OKC vs SA", DemoImageUrls.avatar("okc-sa"),
                        null, "nba://article/okc-spurs-finals"},
                {"story-knicks", "Knicks on brink", SduiUtils.teamLogoUrl("1610612752"),
                        null, "nba://team/1610612752"},
                {"story-playoffs", "Playoff Pulse", FALLBACK_STORY_AVATAR,
                        null, "nba://playoffs"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildStoryCircleRail(
                sectionId, "for_you_following", "Following", items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    /**
     * Full-bleed editorial hero at the top of the feed (NBA app home style).
     * slides: [id, imageUrl, badge, title, subtitle, ctaLabel, targetUri]
     */
    private ObjectNode buildFeaturedEditorialHero() {
        String contentSourceId = "cms:for-you-featured-hero";
        String[][] slides = {
                {"feat-hero-1", DemoImageUrls.hero("cleveland-ecf"),
                        "FEATURED",
                        "HIGH STAKES IN CLEVELAND",
                        "Knicks one win from the Finals; Cavs fight to extend the series (8 ET, ESPN).",
                        "See Story", "nba://article/knicks-cavs-g4-preview"},
                {"feat-hero-2", DemoImageUrls.hero("all-nba"),
                        "BREAKING",
                        "ALL-NBA TEAMS ANNOUNCED",
                        "SGA, Jokić & Wembanyama headline the Kia All-NBA First Team.",
                        "See Story", "nba://article/all-nba-teams"},
                {"feat-hero-3", DemoImageUrls.hero("spurs-force"),
                        "LIVE",
                        "SPURS PLAY WITH FORCE IN GAME 4 WIN",
                        "San Antonio's intensity sets up a pivotal West Finals swing.",
                        "Watch", "nba://video/spurs-game4-recap"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildCinematicHeroCarousel(
                sectionId, "for_you_featured_hero", slides);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.flushSurface());
        return section;
    }

    /**
     * Single paged hero carousel that combines the live game (if any)
     * with the next few upcoming games. Replaces the legacy compact
     * GamePanel carousel — the cards are bigger, key-art-led, and use
     * the same FeaturedLiveGameHero composite the Kitchen screen does.
     */
    private ObjectNode buildTonightsGamesHero() {
        String contentSourceId = "stats-api:scoreboard";
        List<String[]> cards = new ArrayList<>();
        ObjectNode liveBindings = null;

        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard != null) {
                JsonNode games = scoreboard.path("scoreboard").path("games");
                if (games.isArray()) {
                    JsonNode liveGame = null;
                    List<JsonNode> upcoming = new ArrayList<>();
                    for (JsonNode game : games) {
                        int status = game.path("gameStatus").asInt(1);
                        if (status == 2 && liveGame == null) liveGame = game;
                        else if (status == 1 && upcoming.size() < 3) upcoming.add(game);
                    }
                    if (liveGame != null) {
                        cards.add(buildHeroCardFromGame(liveGame, true));
                        liveBindings = buildHeroLinescoreBindings(cards.get(0)[0]);
                    }
                    for (JsonNode game : upcoming) {
                        cards.add(buildHeroCardFromGame(game, false));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build tonight's games hero from live data: {}", e.getMessage());
        }

        boolean fromMockFallback = false;
        if (cards.isEmpty()) {
            fromMockFallback = true;
            cards.add(buildMockHeroCard("hero-game-1", "LIVE",
                    "Lakers at Celtics", "Fourth-quarter finish on NBA TV",
                    "LAL", "89", "1610612747",
                    "BOS", "94", "1610612738",
                    "Q4 2:15", "BOS leads 3-2",
                    "0022400777"));
            cards.add(buildMockHeroCard("hero-game-2", "UP NEXT",
                    "Thunder at Nuggets", "Coverage begins at 10:00 PM ET",
                    "OKC", null, "1610612760",
                    "DEN", null, "1610612743",
                    "10:00 PM ET", "Season series tied",
                    "0022400778"));
            cards.add(buildMockHeroCard("hero-game-3", "UP NEXT",
                    "Warriors at Heat", "Tonight on NBA TV",
                    "GSW", null, "1610612744",
                    "MIA", null, "1610612748",
                    "7:30 PM ET", null,
                    "0022400779"));
        }

        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "tonights-games-hero");
        ObjectNode refreshPolicy;
        ObjectNode dataBindingForHero = liveBindings;
        if (fromMockFallback) {
            // Demo fallback cards use synthetic hero ids — poll the section endpoint
            // instead of opening Ably (same pattern as LiveComposer mock live list).
            refreshPolicy = mockHeroSectionPollPolicy(sectionId);
            dataBindingForHero = null;
        } else if (liveBindings != null) {
            refreshPolicy = sseRefreshPolicy(cards.get(0)[0]);
        } else {
            refreshPolicy = staticPolicy();
            dataBindingForHero = null;
        }

        ObjectNode section = atomicBuilder.buildFeaturedLiveGameHero(
                sectionId,
                "for_you_tonights_games_hero",
                null,
                cards.toArray(new String[0][0]),
                refreshPolicy,
                dataBindingForHero);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private String[] buildHeroCardFromGame(JsonNode game, boolean live) {
        String gameId = game.path("gameId").asText("0000000000");
        AtomicCompositeBuilder.GamePanelTeam away =
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam"));
        AtomicCompositeBuilder.GamePanelTeam home =
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam"));
        String heroId = "hero-game-" + gameId;
        return new String[]{
                heroId,
                live ? "LIVE" : "UP NEXT",
                away.tricode() + " at " + home.tricode(),
                live ? "Live now on NBA TV" : "Tonight on NBA TV",
                live ? FALLBACK_KEY_ART_LIVE : FALLBACK_KEY_ART_UPCOMING,
                away.tricode(), Integer.toString(away.score()), away.logoUrl(),
                home.tricode(), Integer.toString(home.score()), home.logoUrl(),
                game.path("gameStatusText").asText(""),
                null,
                // Slot 13 (sponsorLogoUrlsCsv) intentionally left null — the
                // demo doesn't have real broadcaster art. The broadcaster
                // mention is already carried in the subtitle ("Live now on
                // NBA TV" / "Tonight on NBA TV"). Restore this slot once we
                // can source actual broadcaster logos server-side.
                null,
                "nba://game/" + gameId,
                "nba://game/" + gameId + "/actions"
        };
    }

    private String[] buildMockHeroCard(String id, String badge, String title, String subtitle,
                                       String awayTri, String awayScore, String awayTeamId,
                                       String homeTri, String homeScore, String homeTeamId,
                                       String statusText, String seriesText, String gameId) {
        return new String[]{
                id, badge, title, subtitle,
                "LIVE".equals(badge) ? FALLBACK_KEY_ART_LIVE : FALLBACK_KEY_ART_UPCOMING,
                awayTri, awayScore, SduiUtils.teamLogoUrl(awayTeamId),
                homeTri, homeScore, SduiUtils.teamLogoUrl(homeTeamId),
                statusText, seriesText,
                // See note in buildHeroCardFromGame — sponsor logos omitted
                // until we have real broadcaster art.
                null,
                "nba://game/" + gameId,
                "nba://game/" + gameId + "/actions"
        };
    }

    /**
     * Per-card linescore bindings for the live hero card. Targets the
     * section's {@code content.cards.<heroCardId>.*} state — the same
     * keys the rendered Text leaves resolve via {@code bindRef}, which
     * is evaluated against {@code section.data.content}. Target paths
     * must include the {@code content.} prefix because the resolver
     * writes them literally into {@code section.data}; a path without
     * the prefix lands at {@code section.data.cards.X.…}, a sibling
     * of {@code data.content} that no leaf reads.
     */
    private ObjectNode buildHeroLinescoreBindings(String heroCardId) {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();
        bindings.add(utils.bindingPath("$.awayTeam.score",
                "content.cards." + heroCardId + ".awayScore"));
        bindings.add(utils.bindingPath("$.homeTeam.score",
                "content.cards." + heroCardId + ".homeScore"));
        bindings.add(utils.bindingPath("$.gameStatusText",
                "content.cards." + heroCardId + ".statusText"));
        dataBinding.set("bindings", bindings);
        return dataBinding;
    }

    /**
     * Vertical headline list with thumbnails — denser than the editorial overlay rail.
     * articles: [id, headline, imageUrl, targetUri]
     */
    private ObjectNode buildTopStoriesArticleList() {
        String contentSourceId = "cms:top-stories";
        String[][] articles = {
                {"top-1", "Starting 5: Spurs roll, plus a Knicks-Cavs preview",
                        DemoImageUrls.thumb("starting5"), "nba://article/starting-5"},
                {"top-2", "Spurs play with force and intensity in Game 4 win",
                        DemoImageUrls.thumb("spurs-g4"), "nba://article/spurs-game4"},
                {"top-3", "What to watch for as Knicks seek sweep over Cavs",
                        DemoImageUrls.thumb("knicks-cavs"), "nba://article/knicks-cavs-preview"},
                {"top-4", "Spurs, Thunder delivering thrills in West Finals",
                        DemoImageUrls.thumb("west-finals"), "nba://article/west-finals"},
                {"top-5", "SGA, Jokić, Wembanyama mark All-NBA First Team",
                        DemoImageUrls.thumb("all-nba"), "nba://article/all-nba-teams"}
        };

        ObjectNode root = atomicBuilder.container("column", null, null);
        root.put("widthMode", "fill");
        ArrayNode children = objectMapper.createArrayNode();

        for (int i = 0; i < articles.length; i++) {
            String id = articles[i][0];
            String headline = articles[i][1];
            String imageUrl = articles[i][2];
            String targetUri = articles[i][3];

            ObjectNode row = atomicBuilder.container("row", "start", "center");
            row.put("id", id);
            row.put("widthMode", "fill");
            row.set("padding", atomicBuilder.padding(
                    LayoutTokens.SPACING_LG,
                    LayoutTokens.SPACING_LG,
                    LayoutTokens.SPACING_MD,
                    LayoutTokens.SPACING_MD
            ));
            row.set("actions", atomicBuilder.singleActionArray(atomicBuilder.tapNavigate(targetUri)));

            ArrayNode rowChildren = objectMapper.createArrayNode();
            ObjectNode img = atomicBuilder.image(imageUrl, 120, 68, "cover");
            img.put("cornerRadius", LayoutTokens.RADIUS_SM);
            AccessibilityHelper.addImage(objectMapper, img, headline);
            rowChildren.add(img);

            ObjectNode textCol = atomicBuilder.container("column", null, "start");
            textCol.set("padding", atomicBuilder.padding(
                    LayoutTokens.SPACING_MD, 0, 0, 0));
            textCol.put("flex", 1);
            ArrayNode textChildren = objectMapper.createArrayNode();
            textChildren.add(atomicBuilder.text(headline, "bodyMedium", "semiBold",
                    ColorTokens.TEXT_PRIMARY, 3));
            textCol.set("children", textChildren);
            rowChildren.add(textCol);

            row.set("children", rowChildren);
            children.add(row);

            if (i < articles.length - 1) {
                ObjectNode divider = atomicBuilder.container("column", null, null);
                divider.put("widthMode", "fill");
                divider.set("padding", atomicBuilder.padding(
                        LayoutTokens.SPACING_LG, LayoutTokens.SPACING_LG, 0, 0));
                ArrayNode dividerKids = objectMapper.createArrayNode();
                ObjectNode line = atomicBuilder.container("row", null, null);
                line.put("widthMode", "fill");
                line.put("height", 1);
                line.put("background", ColorTokens.BORDER_SUBTLE);
                dividerKids.add(line);
                divider.set("children", dividerKids);
                children.add(divider);
            }
        }

        root.set("children", children);
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.wrapAsComposite(sectionId, "for_you_top_stories", root);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildOtherLeaguesRail() {
        String contentSourceId = "cms:other-leagues";
        // [id, label, imageUrl, targetUri]
        String[][] items = {
                {"league-gleague", "G League", FALLBACK_STORY_AVATAR, "nba://league/gleague"},
                {"league-bal", "BAL", FALLBACK_STORY_AVATAR, "nba://league/bal"},
                {"league-wnba", "WNBA", FALLBACK_STORY_AVATAR, "nba://league/wnba"},
                {"league-2k", "NBA 2K League", FALLBACK_STORY_AVATAR, "nba://league/2k"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildLeagueCardRail(
                sectionId, "for_you_other_leagues",
                "Other Leagues", items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildTrendingRail() {
        String contentSourceId = "rec:trending";
        // [id, headline, subhead, imageUrl, kind, badge, targetUri]
        String[][] cards = {
                {"tr-1", "Best Of East Finals Game 4",
                        "Knicks push Cleveland to the brink",
                        DemoImageUrls.cardWide("ecf-g4"),
                        "video", "4:02", "nba://video/ecf-g4-highlights"},
                {"tr-2", "Inside the West Finals chess match",
                        "Spurs vs Thunder — what changes in Game 5",
                        DemoImageUrls.cardWide("west-finals"),
                        "article", null, "nba://article/west-finals-preview"},
                {"tr-3", "All-NBA First Team reactions",
                        "SGA, Jokić, Wembanyama lead the class",
                        DemoImageUrls.cardWide("all-nba"),
                        "article", null, "nba://article/all-nba-teams"},
                {"tr-4", "Coach Mic: Steve Kerr after Game 4",
                        "Warriors look ahead in a tight series",
                        DemoImageUrls.cardWide("kerr"),
                        "video", "2:18", "nba://video/kerr-postgame"},
                {"tr-5", "Playoff bracket update",
                        "Every series on one screen",
                        DemoImageUrls.cardWide("bracket"),
                        "article", null, "nba://article/playoff-bracket"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildContentRail(
                sectionId, "for_you_trending", null, cards);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildLeaguePassPicksRail() {
        String contentSourceId = "cms:league-pass-picks";
        // [id, headline, subhead, imageUrl, kind, badge, targetUri]
        String[][] cards = {
                {"lp-1", "Full replay: Knicks at Cavs",
                        "East Finals Game 4",
                        DemoImageUrls.cardWide("knicks-cavs"),
                        "video", "2:28:00", "nba://video/nyk-cle-g4-full"},
                {"lp-2", "Condensed: Spurs at Thunder",
                        "West Finals Game 4",
                        DemoImageUrls.cardWide("spurs-thunder"),
                        "video", "12:40", "nba://video/sas-okc-g4-condensed"},
                {"lp-3", "All-22 angles: Wembanyama block",
                        "Momentum swing in the fourth",
                        DemoImageUrls.cardWide("wemby-block"),
                        "video", "0:42", "nba://video/wemby-block-all22"},
                {"lp-4", "Film Room: SGA in the pocket",
                        "How OKC creates late-clock looks",
                        DemoImageUrls.cardWide("sga-film"),
                        "video", "6:05", "nba://video/sga-film-room"},
                {"lp-5", "Classic: 2016 Game 7",
                        "League Pass archive pick",
                        DemoImageUrls.cardWide("classic-2016"),
                        "video", "2:15:00", "nba://video/2016-finals-g7"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildContentRail(
                sectionId, "for_you_lp_picks", null, cards);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildVodPlaylistSection() {
        String contentSourceId = "cms:vod-playlist";
        // [id, title, subtitle, thumbUrl, duration, isLive, targetUri]
        String[][] rows = {
                {"playlist-1", "Knicks close out Cavs in Cleveland", "East Finals · Game 4",
                        DemoImageUrls.cardWide("knicks-cavs"),
                        "2:28:00", "false", "nba://video/nyk-cle-g4-full"},
                {"playlist-2", "Spurs force Game 5 in OKC", "West Finals · Game 4",
                        DemoImageUrls.cardWide("spurs-thunder"),
                        "2:22:00", "false", "nba://video/sas-okc-g4-full"},
                {"playlist-3", "Thunder vs Spurs (Live)", "On now · League Pass",
                        DemoImageUrls.cardWide("okc-sa-live"),
                        null, "true", "nba://video/okc-sas-live"},
                {"playlist-4", "Top 10 Plays: Tuesday night", "Around the league",
                        DemoImageUrls.cardWide("top10"),
                        "3:54", "false", "nba://video/top10-tuesday"},
                {"playlist-5", "All-NBA Teams announcement", "Awards · 3 min recap",
                        DemoImageUrls.cardWide("all-nba"),
                        "3:12", "false", "nba://video/all-nba-announcement"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildVodPlaylist(
                sectionId, "for_you_vod_playlist",
                "More to Watch", rows);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildAroundTheLeagueUtilityGrid() {
        String contentSourceId = "cms:around-the-league";
        // [id, label, subtitle, imageUrl, targetUri]
        String[][] items = {
                {"util-standings", "Standings", "Conference & division",
                        null, "nba://standings"},
                {"util-stats-leaders", "Stats Leaders", "Players & teams",
                        null, "nba://stats/leaders"},
                {"util-players", "Players", "Search the league",
                        null, "nba://players"},
                {"util-teams", "Teams", "All 30 NBA clubs",
                        null, "nba://teams"},
                {"util-live", "Live", "On now",
                        null, "nba://live"},
                {"util-ask-nba", "Ask NBA", "Beta",
                        null, "nba://ask"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildUtilityCardGrid(
                sectionId, "for_you_around_league",
                null, items);
        section.put("contentSourceId", contentSourceId);
        // railSurface (margin-only) — the grid's cells are individually
        // card-chromed, so the section doesn't need an outer card.
        section.set("surface", surfaces.railSurface());
        return section;
    }

    private ObjectNode buildSectionHeaderComposite(String id, String analyticsId,
                                                   String title, String subtitle,
                                                   String actionLabel, String actionUri) {
        String contentSourceId = "feed:for-you";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", id);
        ObjectNode section = atomicBuilder.buildSectionHeaderComposite(
                sectionId, analyticsId, title, subtitle, actionLabel, actionUri);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.sectionHeaderSurface());
        return section;
    }

    /**
     * Builds an ad slot section.
     *
     * @param id          deprecated section ID parameter (not used — derived from position)
     * @param analyticsId analytics identifier
     * @param adUnitPath  GAM ad unit path
     * @param position    ad position identifier (must NOT include "ads:" prefix, e.g. "mid_feed_1")
     */
    private ObjectNode buildAdSlot(String id, String analyticsId,
                                    String adUnitPath, String position) {
        if (position.startsWith("ads:")) {
            throw new IllegalArgumentException(
                    "position must not include 'ads:' prefix (got: " + position + ")");
        }
        String contentSourceId = "ads:gam-" + position;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "AdSlot");
        section.put("analyticsId", analyticsId);
        section.put("contentSourceId", contentSourceId);
        section.set("refreshPolicy", staticPolicy());
        section.set("surface", surfaces.adSlotSurface());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("provider", "gam");
        data.put("adUnitPath", adUnitPath);

        ArrayNode sizes = objectMapper.createArrayNode();
        ArrayNode size320 = objectMapper.createArrayNode();
        size320.add(320);
        size320.add(50);
        sizes.add(size320);
        ArrayNode size728 = objectMapper.createArrayNode();
        size728.add(728);
        size728.add(90);
        sizes.add(size728);
        data.set("sizes", sizes);

        ObjectNode targeting = objectMapper.createObjectNode();
        targeting.put("section", "for_you");
        targeting.put("position", position);
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

    private ObjectNode sseRefreshPolicy(String heroCardId) {
        // heroCardId looks like "hero-game-<gameId>" — the channel keys
        // off the gameId portion so the same SSE subscription drives
        // any composite hosting that game.
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "sse");
        String gameId = heroCardId.startsWith("hero-game-")
                ? heroCardId.substring("hero-game-".length())
                : heroCardId;
        rp.put("channel", gameId + ":linescore");
        return rp;
    }

    private ObjectNode mockHeroSectionPollPolicy(String sectionId) {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "poll");
        rp.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
        rp.put("intervalMs", 60_000);
        rp.put("pauseWhenOffScreen", false);
        return rp;
    }
}
