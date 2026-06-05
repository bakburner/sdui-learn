package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.models.generated.AdSlot;
import com.nba.sdui.models.generated.AtomicElement;
import com.nba.sdui.models.generated.Placeholder;
import com.nba.sdui.models.generated.DataBinding;
import com.nba.sdui.models.generated.DataBindingPath;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.Targeting;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.DemoImageUrls;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.domain.AccessibilityHelper;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;

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
    private final ScoreboardPort scoreboardPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;
    private final SectionRefreshService sectionRefreshService;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ForYouComposer(ObjectMapper objectMapper,
                          ScoreboardPort scoreboardPort,
                          SduiUtils utils,
                          SectionSurfaces surfaces,
                          Tokens tokens,
                          SectionRefreshService sectionRefreshService) {
        this.objectMapper = objectMapper;
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
        this.sectionRefreshService = sectionRefreshService;
    }

    @PostConstruct
    private void registerSectionResolvers() {
        String sectionId = SectionIdDeriver.derive(
                "stats-api:scoreboard", "AtomicComposite", "tonights-games-hero");
        sectionRefreshService.registerResolver(sectionId, (id, ctx) -> buildTonightsGamesHero());
    }

    public Screen composeForYou(String traceId, String locale) {
        log.info("Composing For You screen, locale={}", locale);

        Screen response = new Screen();
        response.setId("for-you");
        response.setAnalyticsId("for_you");
        response.setSchemaVersion(schemaVersion);
        utils.applyTabDestinationNavigation(response, "for-you");

        List<Section> sections = new ArrayList<>();

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

        response.setSections(sections);
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
    private Section buildFollowingStoryCircleRail() {
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
        Section section = atomicBuilder.buildStoryCircleRail(
                sectionId, "for_you_following", "Following", items);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    /**
     * Full-bleed editorial hero at the top of the feed (NBA app home style).
     * slides: [id, imageUrl, badge, title, subtitle, ctaLabel, targetUri]
     */
    private Section buildFeaturedEditorialHero() {
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
        Section section = atomicBuilder.buildCinematicHeroCarousel(
                sectionId, "for_you_featured_hero", slides);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.flushSurface());
        return section;
    }

    /**
     * Single paged hero carousel that combines the live game (if any)
     * with the next few upcoming games. Replaces the legacy compact
     * GamePanel carousel — the cards are bigger, key-art-led, and use
     * the same FeaturedLiveGameHero composite the Kitchen screen does.
     */
    private Section buildTonightsGamesHero() {
        String contentSourceId = "stats-api:scoreboard";
        List<String[]> cards = new ArrayList<>();
        DataBinding liveBindings = null;

        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();
            if (scoreboard != null) {
                Game liveGame = null;
                List<Game> upcoming = new ArrayList<>();
                for (Game game : scoreboard.getGames()) {
                    int status = game.getGameStatus();
                    if (status == 2 && liveGame == null) liveGame = game;
                    else if (status == 1 && upcoming.size() < 3) upcoming.add(game);
                }
                if (liveGame != null) {
                    cards.add(buildHeroCardFromGame(liveGame, true));
                    liveBindings = buildHeroLinescoreBindings(cards.get(0)[0]);
                }
                for (Game game : upcoming) {
                    cards.add(buildHeroCardFromGame(game, false));
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
        RefreshPolicy refreshPolicy;
        DataBinding dataBindingForHero = liveBindings;
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

        Section section = atomicBuilder.buildFeaturedLiveGameHero(
                sectionId,
                "for_you_tonights_games_hero",
                null,
                cards.toArray(new String[0][0]),
                refreshPolicy,
                dataBindingForHero);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private String[] buildHeroCardFromGame(Game game, boolean live) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        AtomicCompositeBuilder.GamePanelTeam away =
                atomicBuilder.gamePanelTeam(game.getAwayTeam());
        AtomicCompositeBuilder.GamePanelTeam home =
                atomicBuilder.gamePanelTeam(game.getHomeTeam());
        String heroId = "hero-game-" + gameId;
        return new String[]{
                heroId,
                live ? "LIVE" : "UP NEXT",
                away.tricode() + " at " + home.tricode(),
                live ? "Live now on NBA TV" : "Tonight on NBA TV",
                live ? FALLBACK_KEY_ART_LIVE : FALLBACK_KEY_ART_UPCOMING,
                away.tricode(), Integer.toString(away.score()), away.logoUrl(),
                home.tricode(), Integer.toString(home.score()), home.logoUrl(),
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
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
    private DataBinding buildHeroLinescoreBindings(String heroCardId) {
        DataBinding dataBinding = new DataBinding();
        java.util.List<DataBindingPath> bindings = new java.util.ArrayList<>();
        bindings.add(utils.bindingPath("$.awayTeam.score",
                "content.cards." + heroCardId + ".awayScore"));
        bindings.add(utils.bindingPath("$.homeTeam.score",
                "content.cards." + heroCardId + ".homeScore"));
        bindings.add(utils.bindingPath("$.gameStatusText",
                "content.cards." + heroCardId + ".statusText"));
        dataBinding.setBindings(bindings);
        return dataBinding;
    }

    /**
     * Vertical headline list with thumbnails — denser than the editorial overlay rail.
     * articles: [id, headline, imageUrl, targetUri]
     */
    private Section buildTopStoriesArticleList() {
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

        AtomicElement root = atomicBuilder.container("column", null, null);
        atomicBuilder.widthMode(root, "fill");
        List<AtomicElement> children = new ArrayList<>();

        for (int i = 0; i < articles.length; i++) {
            String id = articles[i][0];
            String headline = articles[i][1];
            String imageUrl = articles[i][2];
            String targetUri = articles[i][3];

            AtomicElement row = atomicBuilder.container("row", "start", "center");
            row.setId(id);
            atomicBuilder.widthMode(row, "fill");
            row.setPadding(atomicBuilder.padding(
                    tokens.spacing("lg"),
                    tokens.spacing("lg"),
                    tokens.spacing("md"),
                    tokens.spacing("md")
            ));
            row.setActions(atomicBuilder.singleActionArray(atomicBuilder.tapNavigate(targetUri)));

            List<AtomicElement> rowChildren = new ArrayList<>();
            AtomicElement img = atomicBuilder.image(imageUrl, 120, 68, "cover");
            img.setCornerRadius(tokens.radius("sm"));
            ObjectNode imgNode = (ObjectNode) objectMapper.valueToTree(img);
            AccessibilityHelper.addImage(objectMapper, imgNode, headline);
            img = objectMapper.convertValue(imgNode, AtomicElement.class);
            rowChildren.add(img);

            AtomicElement textCol = atomicBuilder.container("column", null, "start");
            textCol.setPadding(atomicBuilder.padding(
                    tokens.spacing("md"), 0, 0, 0));
            textCol.setFlex(1.0);
            List<AtomicElement> textChildren = new ArrayList<>();
            textChildren.add(atomicBuilder.text(headline, "bodyMedium", "semiBold",
                    tokens.color("nba.label.primary"), 3));
            textCol.setChildren(textChildren);
            rowChildren.add(textCol);

            row.setChildren(rowChildren);
            children.add(row);

            if (i < articles.length - 1) {
                AtomicElement divider = atomicBuilder.container("column", null, null);
                atomicBuilder.widthMode(divider, "fill");
                divider.setPadding(atomicBuilder.padding(
                        tokens.spacing("lg"), tokens.spacing("lg"), 0, 0));
                List<AtomicElement> dividerKids = new ArrayList<>();
                AtomicElement line = atomicBuilder.container("row", null, null);
                atomicBuilder.widthMode(line, "fill");
                line.setHeight(1);
                line.setBackground(tokens.color("nba.divider.subtle"));
                dividerKids.add(line);
                divider.setChildren(dividerKids);
                children.add(divider);
            }
        }

        root.setChildren(children);
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.wrapAsComposite(sectionId, "for_you_top_stories", root);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildOtherLeaguesRail() {
        String contentSourceId = "cms:other-leagues";
        // [id, label, imageUrl, targetUri]
        String[][] items = {
                {"league-gleague", "G League", FALLBACK_STORY_AVATAR, "nba://league/gleague"},
                {"league-bal", "BAL", FALLBACK_STORY_AVATAR, "nba://league/bal"},
                {"league-wnba", "WNBA", FALLBACK_STORY_AVATAR, "nba://league/wnba"},
                {"league-2k", "NBA 2K League", FALLBACK_STORY_AVATAR, "nba://league/2k"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildLeagueCardRail(
                sectionId, "for_you_other_leagues",
                "Other Leagues", items);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildTrendingRail() {
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
        Section section = atomicBuilder.buildContentRail(
                sectionId, "for_you_trending", null, cards);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildLeaguePassPicksRail() {
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
        Section section = atomicBuilder.buildContentRail(
                sectionId, "for_you_lp_picks", null, cards);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildVodPlaylistSection() {
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
        Section section = atomicBuilder.buildVodPlaylist(
                sectionId, "for_you_vod_playlist",
                "More to Watch", rows);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildAroundTheLeagueUtilityGrid() {
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
        Section section = atomicBuilder.buildUtilityCardGrid(
                sectionId, "for_you_around_league",
                null, items);
        section.setContentSourceId(contentSourceId);
        // railSurface (margin-only) — the grid's cells are individually
        // card-chromed, so the section doesn't need an outer card.
        section.setSurface(surfaces.railSurface());
        return section;
    }

    private Section buildSectionHeaderComposite(String id, String analyticsId,
                                                String title, String subtitle,
                                                String actionLabel, String actionUri) {
        String contentSourceId = "feed:for-you";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", id);
        Section section = atomicBuilder.buildSectionHeaderComposite(
                sectionId, analyticsId, title, subtitle, actionLabel, actionUri);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.sectionHeaderSurface());
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
    private Section buildAdSlot(String id, String analyticsId,
                                    String adUnitPath, String position) {
        if (position.startsWith("ads:")) {
            throw new IllegalArgumentException(
                    "position must not include 'ads:' prefix (got: " + position + ")");
        }
        String contentSourceId = "ads:gam-" + position;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");

        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.AD_SLOT);
        section.setAnalyticsId(analyticsId);
        section.setContentSourceId(contentSourceId);
        RefreshPolicy refreshPolicy = new RefreshPolicy();
        refreshPolicy.setType(RefreshPolicy.RefreshType.STATIC);
        section.setRefreshPolicy(refreshPolicy);
        section.setSurface(surfaces.adSlotSurface());

        AdSlot data = new AdSlot();
        data.setProvider("gam");
        data.setAdUnitPath(adUnitPath);
        data.setSizes(List.of(List.of(320, 50), List.of(728, 90)));

        Targeting targeting = new Targeting();
        targeting.setAdditionalProperty("section", "for_you");
        targeting.setAdditionalProperty("position", position);
        data.setTargeting(targeting);

        data.setCollapseOnEmpty(true);
        data.setLabel("Advertisement");

        Placeholder placeholder = new Placeholder();
        placeholder.setBackgroundColor("token:nba.bg.tertiary");
        placeholder.setText("Advertisement");
        data.setPlaceholder(placeholder);

        section.setData(data);
        return section;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private RefreshPolicy staticPolicy() {
        return new RefreshPolicy().withType(RefreshPolicy.RefreshType.STATIC);
    }

    private RefreshPolicy sseRefreshPolicy(String heroCardId) {
        // heroCardId looks like "hero-game-<gameId>" — the channel keys
        // off the gameId portion so the same SSE subscription drives
        // any composite hosting that game.
        String gameId = heroCardId.startsWith("hero-game-")
                ? heroCardId.substring("hero-game-".length())
                : heroCardId;
        return new RefreshPolicy()
                .withType(RefreshPolicy.RefreshType.SSE)
                .withChannel(gameId + ":linescore");
    }

    private RefreshPolicy mockHeroSectionPollPolicy(String sectionId) {
        return new RefreshPolicy()
                .withType(RefreshPolicy.RefreshType.POLL)
                .withSectionEndpoint("/v1/sdui/section/" + sectionId)
                .withIntervalMs(60_000)
                .withPauseWhenOffScreen(false);
    }
}
