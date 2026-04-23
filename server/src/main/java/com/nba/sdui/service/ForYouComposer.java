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
 * Composes the "For You" SDUI screen — the personalised home feed.
 *
 * <p>Section ordering follows the iOS ref app {@code FeedView}: games
 * carousel first, then featured event, then VOD carousel rails, then a
 * VOD playlist grouped-list section. Ad slots are woven between major
 * groups.
 *
 * <pre>
 *   1.  FollowingRail        – horizontal avatar strip
 *   2.  SectionHeader        – "Upcoming Games"
 *   3.  AtomicComposite      – horizontal GamePanel carousel (featured lead)
 *   4.  GamePanel (featured) – hero live / next-up game
 *   5.  SectionHeader        – "Top Stories"
 *   6.  ContentRail          – highlights
 *   7.  AdSlot               – mid-feed 1
 *   8.  SectionHeader        – "Trending Now"
 *   9.  ContentRail          – trending content
 *  10.  AdSlot               – mid-feed 2
 *  11.  SectionHeader        – "League Pass Picks"
 *  12.  ContentRail          – LP picks
 *  13.  AdSlot               – mid-feed 3
 *  14.  SectionHeader        – "Around the League"
 *  15.  ContentRail          – league-wide news
 *  16.  AtomicComposite      – VOD playlist (grouped-list rows)
 * </pre>
 */
@Component
public class ForYouComposer {

    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private static final Logger log = LoggerFactory.getLogger(ForYouComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ForYouComposer(ObjectMapper objectMapper,
                          StatsApiClient statsApiClient,
                          SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    public JsonNode composeForYou(String traceId, String locale) {
        log.info("Composing For You screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "for-you");
        response.put("title", "For You");
        response.put("analyticsId", "for_you");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("for-you"));

        ArrayNode sections = objectMapper.createArrayNode();

        sections.add(buildFollowingRail());

        sections.add(buildSectionHeader("upcoming-header", "Upcoming Games",
                "See Full Schedule", "nba://scoreboard"));
        addUpcomingGamePanels(sections);

        ObjectNode featured = buildFeaturedFromLive();
        if (featured == null) {
            featured = buildMockFeaturedGame();
        }
        sections.add(featured);

        sections.add(buildSectionHeader("top-stories-header", "Top Stories", null, null));
        sections.add(buildHighlightsRail());

        sections.add(buildAdSlot("for-you-ad-1", "for_you_ad_1",
                "/21234567/sports/nba/homepage_mid1", "mid_feed_1"));

        sections.add(buildSectionHeader("trending-header", "Trending Now", null, null));
        sections.add(buildTrendingRail());

        sections.add(buildAdSlot("for-you-ad-2", "for_you_ad_2",
                "/21234567/sports/nba/homepage_mid2", "mid_feed_2"));

        sections.add(buildSectionHeader("lp-picks-header", "League Pass Picks",
                "Browse League Pass", "nba://leaguepass"));
        sections.add(buildLeaguePassPicksRail());

        sections.add(buildAdSlot("for-you-ad-3", "for_you_ad_3",
                "/21234567/sports/nba/homepage_mid3", "mid_feed_3"));

        sections.add(buildSectionHeader("around-league-header", "Around the League",
                "More Stories", "nba://news"));
        sections.add(buildAroundTheLeagueRail());

        sections.add(buildVodPlaylistSection());

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildFollowingRail() {
        String[][] items = {
                {"lebron", "L. James",
                        "https://cdn.nba.com/headshots/nba/latest/260x190/2544.png",
                        "player", "nba://player/2544"},
                {"cavaliers", "Cavaliers",
                        SduiUtils.teamLogoUrl("1610612739"),
                        "team", "nba://team/1610612739"},
                {"curry", "S. Curry",
                        "https://cdn.nba.com/headshots/nba/latest/260x190/201939.png",
                        "player", "nba://player/201939"},
                {"nba-news", "NBA News",
                        "https://loremflickr.com/260/190/basketball,news?lock=20",
                        "channel", "nba://news"},
                {"twitter", "Twitter",
                        "https://loremflickr.com/260/190/basketball,social?lock=21",
                        "social", "nba://social/twitter"}
        };
        return atomicBuilder.buildFollowingRail("following-rail", "for_you_following",
                "Following", items);
    }

    private ObjectNode buildFeaturedFromLive() {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard == null) return null;

            JsonNode games = scoreboard.path("scoreboard").path("games");
            if (!games.isArray()) return null;

            // Prefer a live game (status 2), else first upcoming (status 1)
            JsonNode liveGame = null;
            JsonNode upcomingGame = null;
            for (JsonNode game : games) {
                int status = game.path("gameStatus").asInt(1);
                if (status == 2 && liveGame == null) liveGame = game;
                if (status == 1 && upcomingGame == null) upcomingGame = game;
            }
            JsonNode picked = liveGame != null ? liveGame : upcomingGame;
            if (picked == null) return null;

            return buildFeaturedGameSection(picked);
        } catch (Exception e) {
            log.warn("Failed to build featured game from live data: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode buildFeaturedGameSection(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "featured-game-" + gameId);
        section.put("type", "GamePanel");
        section.put("analyticsId", "for_you_featured_game");

        if (gameStatus == 2) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", gameId + ":linescore");
            section.set("refreshPolicy", rp);
            section.set("dataBinding", utils.buildLinescoreBindings());
        } else {
            section.set("refreshPolicy", staticPolicy());
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.put("gameTimeEt", game.path("gameTimeEt").asText(""));
        data.put("clockRunning", gameStatus == 2);
        data.set("displayConfig", atomicBuilder.featuredConfig(null, new String[]{ColorTokens.PALETTE_BLUE_30, ColorTokens.BRAND_LIVE}));
        data.set("homeTeam", mapTeam(game.path("homeTeam")));
        data.set("awayTeam", mapTeam(game.path("awayTeam")));
        data.put("badgeText", gameStatus == 2 ? "LIVE" : "UP NEXT");
        data.put("visualLabel", game.path("homeTeam").path("teamName").asText("").toUpperCase());

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

    private ObjectNode buildMockFeaturedGame() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "featured-game-mock");
        section.put("type", "GamePanel");
        section.put("analyticsId", "for_you_featured_game");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400001");
        data.put("gameStatus", 2);
        data.put("gameStatusText", "Q3 5:42");
        data.put("clockRunning", true);
        data.set("displayConfig", atomicBuilder.featuredConfig(null, new String[]{ColorTokens.PALETTE_BLUE_30, ColorTokens.BRAND_LIVE}));
        data.put("badgeText", "LIVE");
        data.put("visualLabel", "CELTICS");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612738);
        home.put("teamTricode", "BOS");
        home.put("teamName", "Celtics");
        home.put("teamCity", "Boston");
        home.put("score", 87);
        home.put("record", "4-2");
        home.put("logoUrl", SduiUtils.teamLogoUrl("1610612738"));
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612747);
        away.put("teamTricode", "LAL");
        away.put("teamName", "Lakers");
        away.put("teamCity", "Los Angeles");
        away.put("score", 82);
        away.put("record", "3-3");
        away.put("logoUrl", SduiUtils.teamLogoUrl("1610612747"));
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400001");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("display", utils.gamePanelDisplay());
        return section;
    }

    private ObjectNode buildSectionHeader(String id, String title,
                                           String actionLabel, String actionUri) {
        ObjectNode section = atomicBuilder.buildSectionHeader(id, title, null, actionLabel, actionUri);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildHighlightsRail() {
        String[][] cards = {
                {"hl-1", "Top 10 Plays of the Night",
                        "Last night's best highlights",
                        "https://loremflickr.com/480/270/basketball,highlights?lock=22",
                        "video", "2:45", "nba://video/top10-plays"},
                {"hl-2", "Dunk of the Night",
                        "An incredible poster slam",
                        "https://loremflickr.com/480/270/basketball,dunk?lock=23",
                        "video", "0:32", "nba://video/dunk-night"},
                {"hl-3", "Trade Deadline Recap",
                        "All the moves from today",
                        "https://loremflickr.com/480/270/basketball,trade?lock=24",
                        "article", null, "nba://article/trade-recap"}
        };
        // Title omitted on the rail itself — the preceding SectionHeader
        // ("Top Stories") already renders the heading. Emitting a title
        // here would double up as "Top Stories" + "Highlights" (or any
        // future rename drift between the two sites).
        ObjectNode section = atomicBuilder.buildContentRail("for-you-highlights", "for_you_highlights",
                null, cards);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildTrendingRail() {
        String[][] cards = {
                {"tr-1", "MVP Race Heats Up",
                        "Who's leading the charge?",
                        "https://cdn.nba.com/manage/2025/02/jokic-allstar-iso-752x428.jpg",
                        "article", null, "nba://article/mvp-race"},
                {"tr-2", "Rookie Spotlight: Zaccharie Risacher",
                        "The first overall pick's breakout game",
                        "https://cdn.nba.com/manage/2025/02/risacher-hawks-drives-752x428.jpg",
                        "video", "3:12", "nba://video/rookie-spotlight"},
                {"tr-3", "Playoff Picture Update",
                        "Where every team stands right now",
                        "https://cdn.nba.com/manage/2025/02/nba-standings-graphic-752x428.jpg",
                        "article", null, "nba://article/playoff-picture"}
        };
        ObjectNode section = atomicBuilder.buildContentRail("for-you-trending", "for_you_trending",
                null, cards);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildLeaguePassPicksRail() {
        String[][] cards = {
                {"lp-1", "Warriors vs. Thunder Preview",
                        "A must-watch Western Conference clash",
                        "https://cdn.nba.com/manage/2025/02/warriors-thunder-preview-752x428.jpg",
                        "article", null, "nba://article/gsw-okc-preview"},
                {"lp-2", "Best of League Pass: Week 20",
                        "Catch the top moments you missed",
                        "https://cdn.nba.com/manage/2025/02/lp-best-of-week-752x428.jpg",
                        "video", "4:30", "nba://video/lp-week-20"},
                {"lp-3", "Hidden Gem: Pacers vs. Magic",
                        "An under-the-radar rivalry renewed",
                        "https://cdn.nba.com/manage/2025/02/pacers-magic-rivalry-752x428.jpg",
                        "video", "1:58", "nba://video/ind-orl-hidden-gem"}
        };
        ObjectNode section = atomicBuilder.buildContentRail("for-you-lp-picks", "for_you_lp_picks",
                null, cards);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildVodPlaylistSection() {
        // Grouped-list section (iOS `VODPlaylistView`): one surface, N rows
        // with inset-left dividers. Mix of "live" and on-demand items to
        // exercise both badge treatments.
        //   [id, title, subtitle, thumbUrl, duration, isLive, targetUri]
        String[][] rows = {
                {"playlist-1", "Giannis goes off for 40", "Last night · Bucks vs Heat",
                        "https://loremflickr.com/480/270/basketball,giannis?lock=50",
                        "0:42", "false", "nba://video/giannis-40"},
                {"playlist-2", "Press conference: Steve Kerr", "Warriors · Postgame",
                        "https://loremflickr.com/480/270/basketball,press?lock=51",
                        "6:21", "false", "nba://video/kerr-pressconf"},
                {"playlist-3", "Lakers vs Nuggets (Live)", "On now · League Pass",
                        "https://loremflickr.com/480/270/basketball,lakers?lock=52",
                        null, "true", "nba://video/lakers-nuggets-live"},
                {"playlist-4", "Top 10 Buzzer Beaters", "Editors' picks",
                        "https://loremflickr.com/480/270/basketball,buzzer?lock=53",
                        "4:08", "false", "nba://video/top10-buzzer-beaters"}
        };
        return atomicBuilder.buildVodPlaylist(
                "for-you-vod-playlist",
                "for_you_vod_playlist",
                "More to Watch",
                rows);
    }

    private ObjectNode buildAroundTheLeagueRail() {
        String[][] cards = {
                {"al-1", "Injury Report Roundup",
                        "Key players in and out tonight",
                        "https://cdn.nba.com/manage/2025/02/injury-report-graphic-752x428.jpg",
                        "article", null, "nba://article/injury-report"},
                {"al-2", "Power Rankings: March Edition",
                        "Who moved up and who dropped?",
                        "https://cdn.nba.com/manage/2025/02/power-rankings-march-752x428.jpg",
                        "article", null, "nba://article/power-rankings-march"},
                {"al-3", "All-Star Weekend Recap",
                        "Best dunks, assists, and moments",
                        "https://cdn.nba.com/manage/2025/02/allstar-weekend-recap-752x428.jpg",
                        "video", "5:10", "nba://video/allstar-recap"}
        };
        ObjectNode section = atomicBuilder.buildContentRail("for-you-around-league", "for_you_around_league",
                null, cards);
        section.set("display", utils.railDisplay());
        return section;
    }

    private ObjectNode buildAdSlot(String id, String analyticsId,
                                    String adUnitPath, String position) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "AdSlot");
        section.put("analyticsId", analyticsId);
        section.set("refreshPolicy", staticPolicy());
        section.set("display", utils.defaultSectionDisplay());

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
        placeholder.put("backgroundColor", "token:color.surface.sunken");
        placeholder.put("text", "Advertisement");
        data.set("placeholder", placeholder);

        section.set("data", data);
        return section;
    }

    private void addUpcomingGamePanels(ArrayNode sections) {
        List<ObjectNode> carouselGames = new ArrayList<>();

        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard != null) {
                JsonNode games = scoreboard.path("scoreboard").path("games");
                int count = 0;
                for (JsonNode game : games) {
                    // iOS ref app shows 5 cards in its upcoming carousel; cap here so the
                    // composite stays inside the AtomicComposite 20-children budget with
                    // plenty of headroom.
                    if (count >= 5) break;
                    String gameId = game.path("gameId").asText(null);
                    if (gameId == null) continue;
                    carouselGames.add(buildGamePanelSection(game, gameId));
                    count++;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to add upcoming games from live data: {}", e.getMessage());
        }

        if (carouselGames.isEmpty()) {
            carouselGames.add(buildMockGamePanel("0022400010", "PHX", 1610612756, "MIL", 1610612749,
                    "7:30 PM ET", 1));
            carouselGames.add(buildMockGamePanel("0022400011", "DAL", 1610612742, "DEN", 1610612743,
                    "10:00 PM ET", 1));
            carouselGames.add(buildMockGamePanel("0022400012", "GSW", 1610612744, "OKC", 1610612760,
                    "10:30 PM ET", 1));
        }

        sections.add(atomicBuilder.buildGameCarousel(
                "upcoming-games-carousel",
                "for_you_upcoming_carousel",
                carouselGames));
    }

    private ObjectNode buildGamePanelSection(JsonNode game, String gameId) {
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-" + gameId);
        section.put("type", "GamePanel");
        section.put("analyticsId", "for_you_game_" + gameId);
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
        return section;
    }

    private ObjectNode buildMockGamePanel(String gameId, String awayTri, int awayId,
                                          String homeTri, int homeId,
                                          String statusText, int gameStatus) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-" + gameId);
        section.put("type", "GamePanel");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", statusText);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", awayId);
        away.put("teamTricode", awayTri);
        away.put("teamName", awayTri);
        away.put("teamCity", awayTri);
        away.put("score", 0);
        away.put("logoUrl", SduiUtils.teamLogoUrl(awayId));
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", 0);
        home.put("logoUrl", SduiUtils.teamLogoUrl(homeId));
        data.set("homeTeam", home);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + gameId);
        actions.add(action);
        data.set("actions", actions);

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
