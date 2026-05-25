package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 *   2.  SectionHeaderComposite      – "TONIGHT'S GAMES" + "See Schedule >"
 *   3.  FeaturedLiveGameHero        – paged hero carousel (live + upcoming)
 *   4.  SectionHeaderComposite      – "TOP STORIES"
 *   5.  EditorialOverlayRail        – tall image cards w/ scrim + headline overlay
 *   6.  AdSlot                      – mid_feed_1
 *   7.  LeagueCardRail              – "OTHER LEAGUES" destinations
 *   8.  SectionHeaderComposite      – "TRENDING NOW"
 *   9.  ContentRail                 – trending articles/videos
 *  10.  AdSlot                      – mid_feed_2
 *  11.  SectionHeaderComposite      – "LEAGUE PASS PICKS" + "Browse League Pass >"
 *  12.  ContentRail                 – LP picks
 *  13.  AdSlot                      – mid_feed_3
 *  14.  SectionHeaderComposite      – "AROUND THE LEAGUE"
 *  15.  UtilityCardGrid             – Standings / Stats Leaders / …
 *  16.  AtomicComposite             – VOD playlist (grouped-list rows)
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

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ForYouComposer(ObjectMapper objectMapper,
                          StatsApiClient statsApiClient,
                          SduiUtils utils,
                          SectionSurfaces surfaces) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.surfaces = surfaces;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
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

        sections.add(buildSectionHeaderComposite("tonights-games-header",
                "for_you_tonights_games_header",
                "Tonight's Games", null, "See Schedule", "nba://scoreboard"));
        sections.add(buildTonightsGamesHero());

        sections.add(buildSectionHeaderComposite("top-stories-header",
                "for_you_top_stories_header",
                "Top Stories", null, null, null));
        sections.add(buildTopStoriesEditorialRail());

        sections.add(buildAdSlot("for-you-ad-1", "for_you_ad_1",
                "/21234567/sports/nba/homepage_mid1", "mid_feed_1"));

        sections.add(buildOtherLeaguesRail());

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
                {"story-celtics", "Celtics", SduiUtils.teamLogoUrl("1610612738"),
                        "LIVE", "nba://team/1610612738"},
                {"story-lakers", "Lakers", SduiUtils.teamLogoUrl("1610612747"),
                        "NEW", "nba://team/1610612747"},
                {"story-warriors", "Warriors", SduiUtils.teamLogoUrl("1610612744"),
                        null, "nba://team/1610612744"},
                {"story-thunder", "Thunder", SduiUtils.teamLogoUrl("1610612760"),
                        null, "nba://team/1610612760"},
                {"story-news", "NBA News", FALLBACK_STORY_AVATAR,
                        null, "nba://news"},
                {"story-social", "Social", FALLBACK_STORY_AVATAR,
                        null, "nba://social"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildStoryCircleRail(
                sectionId, "for_you_following", "Following", items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
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

        if (cards.isEmpty()) {
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

        ObjectNode refreshPolicy = liveBindings != null ? sseRefreshPolicy(cards.get(0)[0]) : staticPolicy();

        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "tonights-games-hero");
        ObjectNode section = atomicBuilder.buildFeaturedLiveGameHero(
                sectionId,
                "for_you_tonights_games_hero",
                null,
                cards.toArray(new String[0][0]),
                refreshPolicy,
                liveBindings);
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
     * section's {@code cards.<heroCardId>.*} state — the same keys the
     * rendered Text leaves resolve via {@code bindRef}.
     */
    private ObjectNode buildHeroLinescoreBindings(String heroCardId) {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();
        bindings.add(utils.bindingPath("$.awayTeam.score",
                "cards." + heroCardId + ".awayScore"));
        bindings.add(utils.bindingPath("$.homeTeam.score",
                "cards." + heroCardId + ".homeScore"));
        bindings.add(utils.bindingPath("$.gameStatusText",
                "cards." + heroCardId + ".statusText"));
        dataBinding.set("bindings", bindings);
        return dataBinding;
    }

    private ObjectNode buildTopStoriesEditorialRail() {
        String contentSourceId = "cms:top-stories";
        // [id, headline, subhead, imageUrl, badgeText, targetUri]
        String[][] cards = {
                {"top-1", "Five things to watch tonight",
                        "Rivalries, returns, and playoff stakes",
                        DemoImageUrls.cardTall("playoffs"),
                        "NEW", "nba://article/five-things"},
                {"top-2", "Inside the MVP race",
                        "The numbers behind a tight finish",
                        DemoImageUrls.cardTall("mvp"),
                        null, "nba://article/mvp-race"},
                {"top-3", "Rookie of the Year tracker",
                        "Who's pulling ahead at the wire",
                        DemoImageUrls.cardTall("rookie"),
                        "LIVE", "nba://video/rookies"}
        };
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildEditorialOverlayRail(
                sectionId, "for_you_top_stories", null, cards);
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
                {"tr-1", "MVP Race Heats Up",
                        "Who's leading the charge?",
                        DemoImageUrls.cardWide("mvp"),
                        "article", null, "nba://article/mvp-race"},
                {"tr-2", "Rookie Spotlight",
                        "The first overall pick's breakout game",
                        DemoImageUrls.cardWide("rookie"),
                        "video", "3:12", "nba://video/rookie-spotlight"},
                {"tr-3", "Playoff Picture Update",
                        "Where every team stands right now",
                        DemoImageUrls.cardWide("standings"),
                        "article", null, "nba://article/playoff-picture"},
                {"tr-4", "Coach of the Year Watch",
                        "The frontrunners with two months to go",
                        DemoImageUrls.cardWide("coach"),
                        "article", null, "nba://article/coach-of-year"}
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
                {"lp-1", "Warriors vs. Thunder",
                        "Western Conference clash",
                        DemoImageUrls.cardWide("warriors"),
                        "article", null, "nba://article/gsw-okc-preview"},
                {"lp-2", "Best of League Pass",
                        "Catch the top moments you missed",
                        DemoImageUrls.cardWide("highlights"),
                        "video", "4:30", "nba://video/lp-week-20"},
                {"lp-3", "Hidden Gem: Pacers vs. Magic",
                        "An under-the-radar rivalry renewed",
                        DemoImageUrls.cardWide("pacers"),
                        "video", "1:58", "nba://video/ind-orl-hidden-gem"},
                {"lp-4", "Last Two Minute Report",
                        "How the night's tightest games closed out",
                        DemoImageUrls.cardWide("clutch"),
                        "article", null, "nba://article/l2m-report"}
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
                {"playlist-1", "Giannis goes off for 40", "Last night · Bucks vs Heat",
                        DemoImageUrls.cardWide("giannis"),
                        "0:42", "false", "nba://video/giannis-40"},
                {"playlist-2", "Press conference: Steve Kerr", "Warriors · Postgame",
                        DemoImageUrls.cardWide("press"),
                        "6:21", "false", "nba://video/kerr-pressconf"},
                {"playlist-3", "Lakers vs Nuggets (Live)", "On now · League Pass",
                        DemoImageUrls.cardWide("lakers"),
                        null, "true", "nba://video/lakers-nuggets-live"},
                {"playlist-4", "Top 10 Buzzer Beaters", "Editors' picks",
                        DemoImageUrls.cardWide("buzzer"),
                        "4:08", "false", "nba://video/top10-buzzer-beaters"}
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
}
