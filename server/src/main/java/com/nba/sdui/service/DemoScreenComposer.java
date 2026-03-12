package com.nba.sdui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Composes the kitchen-sink demo screen showcasing all semantic section types,
 * the Season Leaders table, and the parameterised refresh response for the
 * stats-leaders form.
 */
@Component
public class DemoScreenComposer {

    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public DemoScreenComposer(ObjectMapper objectMapper, SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.utils = utils;
    }

    // ── Public entry points ────────────────────────────────────────────

    /**
     * Compose a kitchen-sink demo screen showcasing all 11 semantic section types
     * with static mock data.  No external API calls.
     */
    public ObjectNode composeDemos(String traceId, String platform) {
        ObjectNode screen = objectMapper.createObjectNode();
        screen.put("id", "demos");
        screen.put("schemaVersion", schemaVersion);
        screen.put("title", "SDUI Section Types");
        screen.put("analyticsId", "demos-kitchen-sink");
        screen.put("traceId", traceId);
        screen.put("parentUri", "nba://scoreboard");

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "static");
        screen.set("defaultRefreshPolicy", refreshPolicy);

        screen.set("navigation", utils.buildNavigation("demos"));

        ArrayNode sections = objectMapper.createArrayNode();

        // 1. ScoreboardHeader
        sections.add(buildTypeLabel("ScoreboardHeader"));
        sections.add(buildDemoScoreboardHeader());
        // 2. AdSlot (between header and top performers)
        sections.add(buildTypeLabel("AdSlot"));
        sections.add(buildDemoAdSlot());
        // 3. StatLine
        sections.add(buildTypeLabel("StatLine"));
        sections.add(buildDemoStatLine());
        // 4. PromoBanner
        sections.add(buildTypeLabel("PromoBanner"));
        sections.add(buildDemoPromoBanner());
        // 5. HeroPanel
        sections.add(buildTypeLabel("HeroPanel"));
        sections.add(buildDemoHeroPanel());
        // 6. ContentRail
        sections.add(buildTypeLabel("ContentRail"));
        sections.add(buildDemoContentRail());
        // 7. GamePanel
        sections.add(buildTypeLabel("GamePanel"));
        sections.add(buildDemoGamePanel());
        // 8. Row
        sections.add(buildTypeLabel("Row"));
        sections.add(buildDemoRow());
        // 9. TabGroup
        sections.add(buildTypeLabel("TabGroup"));
        sections.add(buildDemoTabGroup());
        // 10. BoxscoreTable
        sections.add(buildTypeLabel("BoxscoreTable"));
        sections.add(buildDemoBoxscoreTable());
        // 11. Form
        sections.add(buildTypeLabel("Form"));
        sections.add(buildDemoForm(platform));
        // 12. SeasonLeadersTable
        sections.add(buildTypeLabel("SeasonLeadersTable"));
        sections.add(buildDemoLeadersTable());
        // 13. FeaturedGamePanel
        sections.add(buildTypeLabel("FeaturedGamePanel"));
        sections.add(buildDemoFeaturedGamePanel());
        // 14. VideoCarousel
        sections.add(buildTypeLabel("VideoCarousel"));
        sections.add(buildDemoVideoCarousel());
        // 15. NbaTvSchedule
        sections.add(buildTypeLabel("NbaTvSchedule"));
        sections.add(buildDemoNbaTvSchedule());
        // 16. SubscribeBanner
        sections.add(buildTypeLabel("SubscribeBanner"));
        sections.add(buildDemoSubscribeBanner());
        // 17. SubscribeHero
        sections.add(buildTypeLabel("SubscribeHero"));
        sections.add(buildDemoSubscribeHero());
        // 18. FollowingRail
        sections.add(buildTypeLabel("FollowingRail"));
        sections.add(buildDemoFollowingRail());
        // 19. ErrorState
        sections.add(buildTypeLabel("ErrorState"));
        sections.add(buildDemoErrorState());

        screen.set("sections", sections);
        return screen;
    }

    /**
     * Compose the standalone Season Leaders screen (Form + SeasonLeadersTable).
     */
    public ObjectNode composeLeaders(String traceId, String platform) {
        ObjectNode screen = objectMapper.createObjectNode();
        screen.put("id", "leaders");
        screen.put("schemaVersion", schemaVersion);
        screen.put("title", "Season Leaders");
        screen.put("analyticsId", "season-leaders");
        screen.put("traceId", traceId);
        screen.put("parentUri", "nba://scoreboard");

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "static");
        screen.set("defaultRefreshPolicy", refreshPolicy);

        screen.set("navigation", utils.buildNavigation("leaders"));

        ObjectNode state = objectMapper.createObjectNode();
        state.put("form_season", "2025-26");
        state.put("form_season_type", "regular");
        state.put("form_per_mode", "per_game");
        state.put("form_stat_category", "pts");
        screen.set("state", state);

        ArrayNode sections = objectMapper.createArrayNode();
        sections.add(buildDemoForm(platform));
        sections.add(buildLeadersTable("2025-26", "regular", "per_game", "pts"));
        screen.set("sections", sections);

        return screen;
    }

    /**
     * Compose only the leaders-table section for a parameterised refresh.
     * Called from the controller when the form submits.
     */
    public ObjectNode composeLeadersRefresh(String traceId, Map<String, String> params) {
        String season = params.getOrDefault("season", "2025-26");
        String seasonType = params.getOrDefault("seasonType", "regular");
        String perMode = params.getOrDefault("perMode", "per_game");
        String statCategory = params.getOrDefault("statCategory", "pts");

        ObjectNode screen = objectMapper.createObjectNode();
        screen.put("id", "stats-leaders-refresh");
        screen.put("traceId", traceId);
        screen.put("schemaVersion", "1.0");

        ObjectNode state = objectMapper.createObjectNode();
        state.put("form_season", season);
        state.put("form_season_type", seasonType);
        state.put("form_per_mode", perMode);
        state.put("form_stat_category", statCategory);
        screen.set("state", state);

        ArrayNode sections = objectMapper.createArrayNode();
        sections.add(buildLeadersTable(season, seasonType, perMode, statCategory));
        screen.set("sections", sections);

        return screen;
    }

    /**
     * Build a season leaders table section with data matching the given filters.
     */
    public ObjectNode buildLeadersTable(String season, String seasonType,
                                         String perMode, String statCategory) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "leaders-table");
        section.put("type", "SeasonLeadersTable");
        section.put("analyticsId", "season_leaders_table");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();

        String seasonTypeName = "regular".equals(seasonType) ? "Regular Season"
                : "playoffs".equals(seasonType) ? "Playoffs" : "All-Star";
        String perModeName = "per_game".equals(perMode) ? "Per Game"
                : "totals".equals(perMode) ? "Totals" : "Per 36 Min";
        String catLabel = statCategory != null ? statCategory.toUpperCase() : "PTS";

        data.put("title", "Season Leaders");
        data.put("subtitle", season + " " + seasonTypeName + " — " + perModeName + " — sorted by " + catLabel);
        data.put("sortColumn", statCategory != null ? statCategory : "pts");
        data.put("sortDirection", "desc");
        data.put("totalRows", 30);
        data.put("page", 1);
        data.put("pageSize", 15);

        ArrayNode columns = objectMapper.createArrayNode();
        String[][] colDefs = {
            {"gp", "GP"}, {"min", "MIN"}, {"pts", "PTS"}, {"fgm", "FGM"}, {"fga", "FGA"},
            {"fgPct", "FG%"}, {"tpm", "3PM"}, {"tpa", "3PA"}, {"tpPct", "3P%"},
            {"ftm", "FTM"}, {"fta", "FTA"}, {"ftPct", "FT%"},
            {"reb", "REB"}, {"ast", "AST"}, {"stl", "STL"}, {"blk", "BLK"}, {"tov", "TOV"}, {"eff", "EFF"}
        };
        for (String[] cd : colDefs) {
            ObjectNode col = objectMapper.createObjectNode();
            col.put("key", cd[0]);
            col.put("label", cd[1]);
            col.put("sortable", true);
            col.put("highlighted", cd[0].equals(statCategory != null ? statCategory : "pts"));
            columns.add(col);
        }
        data.set("columns", columns);

        ArrayNode players = objectMapper.createArrayNode();
        if ("2025-26".equals(season) && "regular".equals(seasonType)) {
            players.add(leaderRow(1, "203999", "Luka Dončić", "LAL", 49,35.4,32.4,10.3,21.8,47.3,3.7,10.2,35.9, 8.0,10.4,77.3, 7.0,8.6,1.4,0.5,4.0,32.7));
            players.add(leaderRow(2, "1630175", "Shai Gilgeous-Alexander", "OKC", 52,33.4,31.7,10.9,19.8,55.1,1.7,4.5,38.4, 8.2,9.2,89.3, 3.9,6.5,1.4,0.8,2.1,32.7));
            players.add(leaderRow(3, "1630162", "Anthony Edwards", "MIN", 52,35.7,29.7,10.3,20.9,49.4,3.5,8.6,40.2, 5.6,7.1,78.7, 4.6,5.2,1.4,0.8,2.8,25.9));
            players.add(leaderRow(4, "1630178", "Tyrese Maxey", "PHI", 60,38.3,28.9,10.1,21.9,46.0,3.3,8.9,37.2, 5.5,6.2,89.2, 3.9,6.7,2.0,0.8,2.4,27.7));
            players.add(leaderRow(5, "1628369", "Jaylen Brown", "BOS", 55,34.3,28.9,10.7,22.2,48.0,2.1,5.9,34.8, 5.5,7.1,77.9, 6.1,5.0,1.0,0.4,3.6,25.8));
            players.add(leaderRow(6, "203999b", "Nikola Jokić", "DEN", 46,34.6,28.7,10.1,17.7,57.0,1.9,4.8,40.1, 6.5,7.9,82.7, 12.6,10.3,1.3,0.8,3.7,41.1));
            players.add(leaderRow(7, "1628378", "Donovan Mitchell", "CLE", 55,33.5,28.5,9.9,20.6,48.3,3.5,9.4,36.9, 5.2,6.1,85.2, 3.7,5.8,1.6,0.3,3.1,26.1));
            players.add(leaderRow(8, "202695", "Kawhi Leonard", "LAC", 47,32.4,27.9,9.7,19.6,49.7,2.6,7.0,37.9, 5.8,6.4,90.6, 6.4,3.7,2.0,0.5,2.1,27.8));
            players.add(leaderRow(9, "1628973", "Jalen Brunson", "NYK", 58,34.7,26.5,9.4,20.1,46.7,2.8,7.5,37.8, 4.8,5.8,84.1, 2.9,6.3,0.7,0.1,2.3,23.1));
            players.add(leaderRow(10,"201142", "Kevin Durant", "HOU", 57,36.6,26.3,9.2,18.1,51.0,2.4,5.9,40.1, 5.5,6.1,89.1, 4.9,4.5,0.8,0.9,3.2,25.2));
            players.add(leaderRow(11,"1628974", "Jamal Murray", "DEN", 57,35.2,25.7,8.9,18.4,48.4,3.2,7.5,42.9, 4.6,5.2,87.9, 4.0,7.3,1.0,0.4,2.4,26.1));
            players.add(leaderRow(12,"1630595", "Cade Cunningham", "DET", 54,35.1,25.2,8.9,19.5,45.7,1.9,5.9,32.9, 5.4,6.6,81.1, 4.9,9.9,1.5,0.9,3.8,27.7));
            players.add(leaderRow(13,"1626164", "Devin Booker", "PHX", 45,33.3,24.6,8.1,17.9,45.1,1.7,5.5,31.3, 6.6,7.7,86.2, 3.1,6.1,0.9,0.3,3.3,21.6));
            players.add(leaderRow(14,"1631094", "Deni Avdija", "POR", 48,33.5,24.4,7.5,16.1,46.3,2.1,6.2,34.1, 7.4,9.2,80.0, 5.9,6.6,0.8,0.6,3.8,25.1));
            players.add(leaderRow(15,"201935", "James Harden", "CLE", 53,35.0,24.3,7.1,16.7,42.5,3.0,8.4,36.1, 7.1,8.0,89.3, 4.9,8.1,1.2,0.4,3.7,24.8));
        } else if ("2024-25".equals(season)) {
            players.add(leaderRow(1, "1630175", "Shai Gilgeous-Alexander", "OKC", 75,34.1,32.9,11.4,21.2,53.8,2.1,5.5,38.2, 8.0,9.1,88.0, 5.5,6.0,2.0,1.3,2.4,35.1));
            players.add(leaderRow(2, "203999", "Luka Dončić", "DAL", 46,36.7,28.1,9.4,21.0,44.8,3.2,9.8,32.3, 6.1,7.8,78.2, 8.3,7.8,1.3,0.5,4.1,28.2));
            players.add(leaderRow(3, "203507", "Giannis Antetokounmpo", "MIL", 63,35.2,31.5,12.0,20.5,58.4,0.7,2.9,24.1, 6.8,9.8,69.4, 11.5,5.8,0.8,1.2,3.6,33.1));
            players.add(leaderRow(4, "1629029", "Trae Young", "ATL", 72,35.8,24.4,7.9,18.4,43.0,2.7,7.7,35.1, 5.9,6.7,88.1, 3.2,11.4,1.0,0.2,4.0,24.0));
            players.add(leaderRow(5, "1630162", "Anthony Edwards", "MIN", 70,37.1,27.2,9.8,22.3,43.9,3.5,9.8,35.7, 4.1,5.2,78.8, 5.7,4.1,1.3,0.6,3.2,22.2));
            players.add(leaderRow(6, "1628378", "Donovan Mitchell", "CLE", 58,33.2,23.4,8.6,19.0,45.1,3.1,8.1,38.3, 3.1,3.8,81.6, 4.1,4.5,1.4,0.4,2.4,21.5));
            players.add(leaderRow(7, "1628973", "Jalen Brunson", "NYK", 64,34.5,26.4,9.0,19.8,45.5,2.7,7.2,37.5, 5.7,6.7,85.1, 3.0,7.4,0.9,0.2,2.5,24.5));
            players.add(leaderRow(8, "201142", "Kevin Durant", "PHX", 40,36.2,27.0,9.4,18.0,52.2,2.2,5.4,40.7, 6.0,6.7,89.6, 6.4,4.2,0.5,1.3,3.0,27.5));
        } else if ("playoffs".equals(seasonType)) {
            players.add(leaderRow(1, "203507", "Giannis Antetokounmpo", "MIL", 12,39.3,33.2,12.5,22.1,56.6,0.5,2.3,21.7, 7.7,11.2,68.8, 13.1,6.2,1.1,1.8,3.5,36.8));
            players.add(leaderRow(2, "1630175", "Shai Gilgeous-Alexander", "OKC", 16,37.2,31.8,10.8,22.5,48.0,2.3,6.1,37.7, 8.0,9.0,88.9, 6.3,6.3,1.8,0.8,3.1,32.0));
            players.add(leaderRow(3, "203999", "Luka Dončić", "DAL", 7,37.0,29.1,9.3,22.0,42.3,3.6,10.4,34.6, 6.9,8.1,85.2, 9.2,8.1,1.0,0.4,4.5,28.5));
            players.add(leaderRow(4, "1629029", "Trae Young", "ATL", 10,36.8,28.5,9.0,20.2,44.6,3.5,9.1,38.5, 7.0,8.0,87.5, 3.5,12.1,1.2,0.1,4.3,28.7));
            players.add(leaderRow(5, "1628369", "Jaylen Brown", "BOS", 19,38.1,25.0,9.0,21.0,42.9,2.8,7.5,37.3, 4.2,5.5,76.4, 6.9,3.8,1.1,0.6,2.8,22.0));
        } else {
            players.add(leaderRow(1, "203999", "Luka Dončić", "LAL", 40,34.0,30.0,10.0,21.0,47.6,3.5,9.0,38.9, 7.0,9.0,77.8, 8.0,8.0,1.2,0.5,3.5,30.0));
            players.add(leaderRow(2, "1630175", "Shai Gilgeous-Alexander", "OKC", 40,33.0,29.5,10.5,20.0,52.5,2.0,5.0,40.0, 6.5,7.5,86.7, 5.0,6.0,1.8,1.0,2.5,30.0));
            players.add(leaderRow(3, "203507", "Giannis Antetokounmpo", "MIL", 40,34.0,29.0,11.0,19.0,57.9,0.5,2.0,25.0, 6.5,9.5,68.4, 11.0,5.5,1.0,1.5,3.0,31.0));
        }
        data.set("players", players);

        data.put("emptyMessage", "No stats available for " + season + " " + seasonTypeName);

        section.set("data", data);
        return section;
    }

    // ── Demo section builders ──────────────────────────────────────────

    /**
     * 1. ScoreboardHeader — Lakers vs Celtics, period 3, 89-94.
     */
    private ObjectNode buildDemoScoreboardHeader() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-scoreboard-header");
        section.put("type", "ScoreboardHeader");
        section.put("analyticsId", "demo_scoreboard_header");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400999");
        data.put("gameStatus", 2);
        data.put("gameStatusText", "Q3 4:32");
        data.put("period", 3);
        data.put("gameClock", "PT04M32.00S");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612738);
        home.put("teamTricode", "BOS");
        home.put("teamName", "Celtics");
        home.put("teamCity", "Boston");
        home.put("score", 94);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612747);
        away.put("teamTricode", "LAL");
        away.put("teamName", "Lakers");
        away.put("teamCity", "Los Angeles");
        away.put("score", 89);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg");
        data.set("awayTeam", away);

        section.set("data", data);
        return section;
    }

    /**
     * 2. StatLine — 3 mock players with PTS/REB/AST.
     */
    private ObjectNode buildDemoStatLine() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-stat-line");
        section.put("type", "StatLine");
        section.put("analyticsId", "demo_stat_line");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ArrayNode stats = objectMapper.createArrayNode();
        stats.add(utils.createStatLine(1628369, "Jayson Tatum", "BOS", "PTS", "32"));
        stats.add(utils.createStatLine(203507, "LeBron James", "LAL", "PTS", "28"));
        stats.add(utils.createStatLine(203076, "Anthony Davis", "LAL", "REB", "14"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Top Performers");
        data.put("layout", "vertical");
        data.set("stats", stats);
        section.set("data", data);
        return section;
    }

    /**
     * 3. PromoBanner — "Welcome to SDUI" with gradient background.
     */
    private ObjectNode buildDemoPromoBanner() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-promo-banner");
        section.put("type", "PromoBanner");
        section.put("analyticsId", "demo_promo_banner");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Welcome to SDUI");
        data.put("description", "All 20 semantic section types rendered from a single server response.");
        data.put("imageUrl", "https://loremflickr.com/800/200/basketball,nba?lock=1");
        data.put("backgroundColor", "#17408B");
        data.put("textColor", "#FFFFFF");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://scoreboard");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    /**
     * 4. HeroPanel — single highlight card with thumbnail.
     */
    private ObjectNode buildDemoHeroPanel() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-content-card");
        section.put("type", "HeroPanel");
        section.put("analyticsId", "demo_content_card");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("id", "card-highlight");
        data.put("thumbnailUrl", "https://loremflickr.com/480/270/basketball,celtics?lock=2");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);
        data.put("headline", "Celtics Lead Series 3-1");
        data.put("subhead", "Boston takes commanding lead after Game 4 victory");
        data.put("contentType", "article");

        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://article/celtics-lead-series");
        action.put("fallbackUrl", "https://www.nba.com/news");
        data.set("action", action);

        section.set("data", data);
        return section;
    }

    /**
     * 5. ContentRail — 4 cards (Top Plays, Player Spotlight, etc.).
     */
    private ObjectNode buildDemoContentRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-content-rail");
        section.put("type", "ContentRail");
        section.put("analyticsId", "demo_content_rail");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Featured Content");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);

        ArrayNode cards = objectMapper.createArrayNode();

        String[][] cardData = {
            {"rail-1", "Top 10 Plays", "Last night's best moments", "video", "https://loremflickr.com/480/270/basketball,highlights?lock=3"},
            {"rail-2", "Player Spotlight", "Jayson Tatum's monster game", "article", "https://loremflickr.com/480/270/basketball,player?lock=4"},
            {"rail-3", "Draft Preview", "Top prospects for 2025 draft", "article", "https://loremflickr.com/480/270/basketball,draft?lock=5"},
            {"rail-4", "Playoff Bracket", "Updated bracket after today's results", "interactive", "https://loremflickr.com/480/270/basketball,playoffs?lock=6"}
        };

        for (String[] cd : cardData) {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("id", cd[0]);
            card.put("headline", cd[1]);
            card.put("subhead", cd[2]);
            card.put("contentType", cd[3]);
            card.put("thumbnailUrl", cd[4]);

            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", "nba://content/" + cd[0]);
            card.set("action", action);

            cards.add(card);
        }

        data.set("cards", cards);
        section.set("data", data);
        return section;
    }

    /**
     * 6. GamePanel — mock game tile for a single game.
     */
    private ObjectNode buildDemoGamePanel() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-game-card");
        section.put("type", "GamePanel");
        section.put("analyticsId", "demo_game_card");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400888");
        data.put("gameStatus", 3);
        data.put("gameStatusText", "Final");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612744);
        home.put("teamTricode", "GSW");
        home.put("teamName", "Warriors");
        home.put("teamCity", "Golden State");
        home.put("score", 112);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612744/primary/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612745);
        away.put("teamTricode", "HOU");
        away.put("teamName", "Rockets");
        away.put("teamCity", "Houston");
        away.put("score", 105);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612745/primary/L/logo.svg");
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400888");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    /**
     * 7. Row — league leader row (label/value pair with children).
     */
    private ObjectNode buildDemoRow() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-row");
        section.put("type", "Row");
        section.put("analyticsId", "demo_row");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("spacing", 16);
        data.put("breakpoint", 600);

        ArrayNode children = objectMapper.createArrayNode();

        ObjectNode leftChild = objectMapper.createObjectNode();
        leftChild.put("id", "row-scoring-leader");
        leftChild.put("type", "StatLine");
        ObjectNode leftData = objectMapper.createObjectNode();
        leftData.put("title", "Scoring Leader");
        leftData.put("layout", "vertical");
        ArrayNode leftStats = objectMapper.createArrayNode();
        leftStats.add(utils.createStatLine(203999, "Nikola Jokić", "DEN", "PTS", "26.4"));
        leftData.set("stats", leftStats);
        leftChild.set("data", leftData);
        children.add(leftChild);

        ObjectNode rightChild = objectMapper.createObjectNode();
        rightChild.put("id", "row-assists-leader");
        rightChild.put("type", "StatLine");
        ObjectNode rightData = objectMapper.createObjectNode();
        rightData.put("title", "Assists Leader");
        rightData.put("layout", "vertical");
        ArrayNode rightStats = objectMapper.createArrayNode();
        rightStats.add(utils.createStatLine(201566, "Trae Young", "ATL", "AST", "11.1"));
        rightData.set("stats", rightStats);
        rightChild.set("data", rightData);
        children.add(rightChild);

        data.set("children", children);
        section.set("data", data);
        return section;
    }

    /**
     * 8. TabGroup — two tabs ("Overview", "Stats") each with a HeroPanel child.
     */
    private ObjectNode buildDemoTabGroup() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-tab-group");
        section.put("type", "TabGroup");
        section.put("analyticsId", "demo_tab_group");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "demo_active_tab");
        data.put("defaultTab", "overview");

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode overviewTab = objectMapper.createObjectNode();
        overviewTab.put("id", "tab-overview");
        overviewTab.put("label", "Overview");
        overviewTab.put("stateKey", "demo_active_tab");
        overviewTab.put("stateValue", "overview");
        tabs.add(overviewTab);

        ObjectNode statsTab = objectMapper.createObjectNode();
        statsTab.put("id", "tab-stats");
        statsTab.put("label", "Stats");
        statsTab.put("stateKey", "demo_active_tab");
        statsTab.put("stateValue", "stats");
        tabs.add(statsTab);

        data.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();

        ArrayNode overviewContent = objectMapper.createArrayNode();
        ObjectNode overviewCard = objectMapper.createObjectNode();
        overviewCard.put("id", "tab-overview-card");
        overviewCard.put("type", "HeroPanel");
        ObjectNode overviewCardData = objectMapper.createObjectNode();
        overviewCardData.put("id", "overview-highlight");
        overviewCardData.put("headline", "Season Overview");
        overviewCardData.put("subhead", "The 2024-25 season has been full of surprises");
        overviewCardData.put("contentType", "article");
        overviewCardData.put("thumbnailUrl", "https://loremflickr.com/480/270/basketball,season?lock=7");
        overviewCard.set("data", overviewCardData);
        overviewContent.add(overviewCard);
        tabContents.set("overview", overviewContent);

        ArrayNode statsContent = objectMapper.createArrayNode();
        ObjectNode statsCard = objectMapper.createObjectNode();
        statsCard.put("id", "tab-stats-card");
        statsCard.put("type", "HeroPanel");
        ObjectNode statsCardData = objectMapper.createObjectNode();
        statsCardData.put("id", "stats-summary");
        statsCardData.put("headline", "League Statistical Leaders");
        statsCardData.put("subhead", "Points, rebounds, assists and more");
        statsCardData.put("contentType", "interactive");
        statsCardData.put("thumbnailUrl", "https://loremflickr.com/480/270/basketball,stats?lock=8");
        statsCard.set("data", statsCardData);
        statsContent.add(statsCard);
        tabContents.set("stats", statsContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        return section;
    }

    /**
     * 9. BoxscoreTable — 3 players, 5 stat columns.
     */
    private ObjectNode buildDemoBoxscoreTable() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-boxscore-table");
        section.put("type", "BoxscoreTable");
        section.put("analyticsId", "demo_boxscore_table");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("teamTricode", "BOS");
        data.put("teamName", "Boston Celtics");
        data.put("teamColor", "#007A33");
        data.put("teamLogoUrl", "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg");

        ArrayNode columns = objectMapper.createArrayNode();
        columns.add(utils.colDef("min", "MIN", true, false, null));
        columns.add(utils.colDef("pts", "PTS", true, true, null));
        columns.add(utils.colDef("reb", "REB", true, false, null));
        columns.add(utils.colDef("ast", "AST", true, false, null));
        columns.add(utils.colDef("fgPct", "FG%", true, false, null));
        data.set("columns", columns);

        ArrayNode players = objectMapper.createArrayNode();
        players.add(demoPlayerRow("1628369", "J. Tatum", "SF", "0", true,
                Map.of("min", "38:12", "pts", 32, "reb", 8, "ast", 5, "fgPct", ".545")));
        players.add(demoPlayerRow("1627759", "J. Brown", "SG", "7", true,
                Map.of("min", "36:45", "pts", 26, "reb", 5, "ast", 3, "fgPct", ".480")));
        players.add(demoPlayerRow("1629684", "D. White", "PG", "9", false,
                Map.of("min", "32:10", "pts", 18, "reb", 4, "ast", 6, "fgPct", ".500")));
        data.set("players", players);

        data.put("sortStateKey", "demo_boxscore_sortCol");
        data.put("sortDirectionStateKey", "demo_boxscore_sortDir");

        section.set("data", data);
        return section;
    }

    /**
     * 10. Stats Leaders Filter Form — 4 dropdowns.
     * Layout is platform-driven: web gets "horizontal" (single-row),
     * mobile gets "vertical" (stacked) for better touch targets.
     */
    private ObjectNode buildDemoForm(String platform) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-form");
        section.put("type", "Form");
        section.put("analyticsId", "demo_stats_filter_form");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        String formLayout = "web".equalsIgnoreCase(platform) ? "horizontal" : "vertical";
        data.put("layout", formLayout);

        ArrayNode fields = objectMapper.createArrayNode();

        // 1. Season
        ObjectNode seasonField = objectMapper.createObjectNode();
        seasonField.put("fieldId", "season");
        seasonField.put("fieldType", "select");
        seasonField.put("label", "Season");
        seasonField.put("stateKey", "form_season");
        ArrayNode seasonOptions = objectMapper.createArrayNode();
        for (String s : new String[]{"2025-26", "2024-25", "2023-24", "2022-23"}) {
            seasonOptions.add(objectMapper.createObjectNode().put("label", s).put("value", s));
        }
        seasonField.set("options", seasonOptions);
        fields.add(seasonField);

        // 2. Season Type
        ObjectNode typeField = objectMapper.createObjectNode();
        typeField.put("fieldId", "seasonType");
        typeField.put("fieldType", "select");
        typeField.put("label", "Season Type");
        typeField.put("stateKey", "form_season_type");
        ArrayNode typeOptions = objectMapper.createArrayNode();
        for (String[] st : new String[][]{
            {"Regular Season", "regular"},
            {"Playoffs", "playoffs"},
            {"All-Star", "allstar"}
        }) {
            typeOptions.add(objectMapper.createObjectNode().put("label", st[0]).put("value", st[1]));
        }
        typeField.set("options", typeOptions);
        fields.add(typeField);

        // 3. Per Mode
        ObjectNode modeField = objectMapper.createObjectNode();
        modeField.put("fieldId", "perMode");
        modeField.put("fieldType", "select");
        modeField.put("label", "Per Mode");
        modeField.put("stateKey", "form_per_mode");
        ArrayNode modeOptions = objectMapper.createArrayNode();
        for (String[] m : new String[][]{
            {"Per Game", "per_game"},
            {"Totals", "totals"},
            {"Per 36 Min", "per_36"}
        }) {
            modeOptions.add(objectMapper.createObjectNode().put("label", m[0]).put("value", m[1]));
        }
        modeField.set("options", modeOptions);
        fields.add(modeField);

        // 4. Stat Category
        ObjectNode catField = objectMapper.createObjectNode();
        catField.put("fieldId", "statCategory");
        catField.put("fieldType", "select");
        catField.put("label", "Stat Category");
        catField.put("stateKey", "form_stat_category");
        ArrayNode catOptions = objectMapper.createArrayNode();
        for (String[] c : new String[][]{
            {"PTS", "pts"}, {"REB", "reb"}, {"AST", "ast"},
            {"STL", "stl"}, {"BLK", "blk"}, {"FG%", "fgPct"},
            {"3PM", "tpm"}, {"FT%", "ftPct"}
        }) {
            catOptions.add(objectMapper.createObjectNode().put("label", c[0]).put("value", c[1]));
        }
        catField.set("options", catOptions);
        fields.add(catField);

        data.set("fields", fields);

        ObjectNode submitAction = objectMapper.createObjectNode();
        submitAction.put("trigger", "onSubmit");
        submitAction.put("type", "refresh");
        submitAction.put("target", "leaders-table");
        submitAction.put("endpoint", "/sdui/refresh/stats-leaders");
        ObjectNode paramBindings = objectMapper.createObjectNode();
        paramBindings.put("season", "{{form_season}}");
        paramBindings.put("seasonType", "{{form_season_type}}");
        paramBindings.put("perMode", "{{form_per_mode}}");
        paramBindings.put("statCategory", "{{form_stat_category}}");
        submitAction.set("paramBindings", paramBindings);
        data.set("submitAction", submitAction);

        data.put("submitLabel", "Search");

        section.set("data", data);
        return section;
    }

    /**
     * 11. AdSlot — GAM ad placement placeholder (ADR-007).
     */
    private ObjectNode buildDemoAdSlot() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-ad-slot");
        section.put("type", "AdSlot");
        section.put("analyticsId", "demo_ad_slot");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("provider", "gam");
        data.put("adUnitPath", "/21234567/sports/nba/homepage_top");

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
        targeting.put("section", "homepage");
        targeting.put("content_type", "live_game");
        data.set("targeting", targeting);

        data.put("collapseOnEmpty", true);
        data.put("label", "Advertisement");

        section.set("data", data);
        return section;
    }

    /**
     * 12. SeasonLeadersTable — default data (not currently wired into composeDemos).
     */
    private ObjectNode buildDemoLeadersTable() {
        return buildLeadersTable("2025-26", "regular", "per_game", "pts");
    }

    /**
     * 13. FeaturedGamePanel — hero-sized game card with background image and badge.
     */
    private ObjectNode buildDemoFeaturedGamePanel() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-featured-game-panel");
        section.put("type", "FeaturedGamePanel");
        section.put("analyticsId", "demo_featured_game_panel");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400777");
        data.put("gameStatus", 2);
        data.put("gameStatusText", "Q4 2:15");
        data.put("gameTimeEt", "2025-03-11T19:30:00-04:00");
        data.put("backgroundImageUrl", "https://loremflickr.com/1200/600/basketball,arena?lock=9");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);
        data.put("badgeText", "LIVE");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612748);
        home.put("teamTricode", "MIA");
        home.put("teamName", "Heat");
        home.put("teamCity", "Miami");
        home.put("score", 101);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612748/primary/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612751);
        away.put("teamTricode", "BKN");
        away.put("teamName", "Nets");
        away.put("teamCity", "Brooklyn");
        away.put("score", 97);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612751/primary/L/logo.svg");
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400777");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    /**
     * 14. VideoCarousel — 4 video thumbnails in a horizontal carousel.
     */
    private ObjectNode buildDemoVideoCarousel() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-video-carousel");
        section.put("type", "VideoCarousel");
        section.put("analyticsId", "demo_video_carousel");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Top Highlights");
        data.put("subtitle", "Today's best plays");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);

        ArrayNode items = objectMapper.createArrayNode();
        String[][] videos = {
            {"vid-1", "Dončić No-Look Dime", "Lakers vs Celtics", "https://loremflickr.com/480/270/basketball,pass?lock=10", "1:24", "NEW"},
            {"vid-2", "SGA Crossover & Finish", "Thunder vs Nuggets", "https://loremflickr.com/480/270/basketball,crossover?lock=11", "0:48", null},
            {"vid-3", "Edwards Poster Dunk", "Timberwolves vs Suns", "https://loremflickr.com/480/270/basketball,dunk?lock=12", "0:32", "NEW"},
            {"vid-4", "Jokić Triple-Double Recap", "Full highlights", "https://loremflickr.com/480/270/basketball,triple+double?lock=13", "3:15", null}
        };
        for (String[] v : videos) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", v[0]);
            item.put("title", v[1]);
            item.put("subtitle", v[2]);
            item.put("thumbnailUrl", v[3]);
            item.put("duration", v[4]);
            if (v[5] != null) item.put("badgeText", v[5]);
            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", "nba://video/" + v[0]);
            item.set("action", action);
            items.add(item);
        }
        data.set("items", items);

        section.set("data", data);
        return section;
    }

    /**
     * 15. NbaTvSchedule — hero promo + 3 time slots.
     */
    private ObjectNode buildDemoNbaTvSchedule() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-nbatv-schedule");
        section.put("type", "NbaTvSchedule");
        section.put("analyticsId", "demo_nbatv_schedule");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("heroImageUrl", "https://loremflickr.com/800/400/basketball,arena?lock=14");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);
        data.put("heroTitle", "NBA TV Live");
        data.put("heroSubtitle", "Lakers vs Celtics — Coverage begins at 7:00 PM ET");
        data.put("liveNow", true);

        ArrayNode slots = objectMapper.createArrayNode();

        ObjectNode slot1 = objectMapper.createObjectNode();
        slot1.put("id", "slot-1");
        slot1.put("title", "NBA GameTime");
        slot1.put("subtitle", "Pre-game analysis and predictions");
        slot1.put("startTime", "2025-03-11T18:00:00-04:00");
        slot1.put("endTime", "2025-03-11T19:00:00-04:00");
        slot1.put("thumbnailUrl", "https://loremflickr.com/160/90/basketball,pregame?lock=15");
        slot1.put("isLive", false);
        slots.add(slot1);

        ObjectNode slot2 = objectMapper.createObjectNode();
        slot2.put("id", "slot-2");
        slot2.put("title", "LAL @ BOS");
        slot2.put("subtitle", "Live game broadcast");
        slot2.put("startTime", "2025-03-11T19:30:00-04:00");
        slot2.put("endTime", "2025-03-11T22:00:00-04:00");
        slot2.put("thumbnailUrl", "https://loremflickr.com/160/90/basketball,lakers?lock=16");
        slot2.put("isLive", true);
        ObjectNode slot2Action = objectMapper.createObjectNode();
        slot2Action.put("trigger", "onTap");
        slot2Action.put("type", "navigate");
        slot2Action.put("targetUri", "nba://game/0022400999");
        slot2.set("action", slot2Action);
        slots.add(slot2);

        ObjectNode slot3 = objectMapper.createObjectNode();
        slot3.put("id", "slot-3");
        slot3.put("title", "NBA Inside Stuff");
        slot3.put("subtitle", "Post-game interviews and highlights");
        slot3.put("startTime", "2025-03-11T22:00:00-04:00");
        slot3.put("endTime", "2025-03-11T23:00:00-04:00");
        slot3.put("thumbnailUrl", "https://loremflickr.com/160/90/basketball,interview?lock=17");
        slot3.put("isLive", false);
        slots.add(slot3);

        data.set("slots", slots);

        section.set("data", data);
        return section;
    }

    /**
     * 16. SubscribeBanner — inline League Pass upsell.
     */
    private ObjectNode buildDemoSubscribeBanner() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-subscribe-banner");
        section.put("type", "SubscribeBanner");
        section.put("analyticsId", "demo_subscribe_banner");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Never Miss a Game");
        data.put("subtitle", "Stream every out-of-market game live with NBA League Pass.");
        data.put("backgroundImageUrl", "https://loremflickr.com/800/200/basketball,court?lock=18");
        data.put("logoUrl", "https://cdn.nba.com/manage/2025/01/league-pass-logo.png");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);
        data.put("ctaLabel", "Subscribe Now");

        ObjectNode ctaAction = objectMapper.createObjectNode();
        ctaAction.put("trigger", "onTap");
        ctaAction.put("type", "navigate");
        ctaAction.put("targetUri", "nba://subscribe");
        ctaAction.put("presentation", "modal");
        data.set("ctaAction", ctaAction);

        section.set("data", data);
        return section;
    }

    /**
     * 17. SubscribeHero — full-width subscription hero with pricing tiers.
     */
    private ObjectNode buildDemoSubscribeHero() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-subscribe-hero");
        section.put("type", "SubscribeHero");
        section.put("analyticsId", "demo_subscribe_hero");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "NBA League Pass");
        data.put("subtitle", "Watch every game. Your way.");
        data.put("backgroundImageUrl", "https://loremflickr.com/1200/600/basketball,court?lock=19");
        data.put("logoUrl", "https://cdn.nba.com/manage/2025/01/league-pass-logo.png");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);

        ArrayNode features = objectMapper.createArrayNode();
        features.add("Live & on-demand out-of-market games");
        features.add("Multiple viewing angles and condensed replays");
        features.add("NBA TV included");
        features.add("Compatible with all major devices");
        data.set("features", features);

        ArrayNode tiers = objectMapper.createArrayNode();

        ObjectNode standard = objectMapper.createObjectNode();
        standard.put("id", "tier-standard");
        standard.put("name", "League Pass");
        standard.put("price", "$14.99/mo");
        standard.put("originalPrice", "$22.99/mo");
        standard.put("badgeText", "MOST POPULAR");
        ArrayNode stdFeatures = objectMapper.createArrayNode();
        stdFeatures.add("All out-of-market games");
        stdFeatures.add("3 concurrent streams");
        stdFeatures.add("HD quality");
        standard.set("features", stdFeatures);
        standard.put("ctaLabel", "Start Free Trial");
        ObjectNode stdCta = objectMapper.createObjectNode();
        stdCta.put("trigger", "onTap");
        stdCta.put("type", "navigate");
        stdCta.put("targetUri", "nba://subscribe/standard");
        standard.set("ctaAction", stdCta);
        tiers.add(standard);

        ObjectNode premium = objectMapper.createObjectNode();
        premium.put("id", "tier-premium");
        premium.put("name", "League Pass Premium");
        premium.put("price", "$22.99/mo");
        premium.put("badgeText", "BEST VALUE");
        ArrayNode premFeatures = objectMapper.createArrayNode();
        premFeatures.add("Everything in League Pass");
        premFeatures.add("No ads on VOD");
        premFeatures.add("Unlimited concurrent streams");
        premFeatures.add("In-arena camera angles");
        premium.set("features", premFeatures);
        premium.put("ctaLabel", "Go Premium");
        ObjectNode premCta = objectMapper.createObjectNode();
        premCta.put("trigger", "onTap");
        premCta.put("type", "navigate");
        premCta.put("targetUri", "nba://subscribe/premium");
        premium.set("ctaAction", premCta);
        tiers.add(premium);

        data.set("tiers", tiers);

        section.set("data", data);
        return section;
    }

    /**
     * 18. FollowingRail — horizontal rail of 5 followed teams/players.
     */
    private ObjectNode buildDemoFollowingRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-following-rail");
        section.put("type", "FollowingRail");
        section.put("analyticsId", "demo_following_rail");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Following");
        data.put("fallbackThumbnailUrl", FALLBACK_THUMB);

        ArrayNode items = objectMapper.createArrayNode();
        String[][] entities = {
            {"team-lal", "Lakers", "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg", "team", "nba://team/1610612747"},
            {"team-bos", "Celtics", "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg", "team", "nba://team/1610612738"},
            {"player-luka", "Luka Dončić", "https://cdn.nba.com/headshots/nba/latest/1040x760/203999.png", "player", "nba://player/203999"},
            {"team-gsw", "Warriors", "https://cdn.nba.com/logos/nba/1610612744/primary/L/logo.svg", "team", "nba://team/1610612744"},
            {"player-sga", "Shai Gilgeous-Alexander", "https://cdn.nba.com/headshots/nba/latest/1040x760/1630175.png", "player", "nba://player/1630175"}
        };
        for (String[] e : entities) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", e[0]);
            item.put("name", e[1]);
            item.put("imageUrl", e[2]);
            item.put("entityType", e[3]);
            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", e[4]);
            item.set("action", action);
            items.add(item);
        }
        data.set("items", items);

        section.set("data", data);
        return section;
    }

    /**
     * 19. ErrorState — server-driven error with retry action.
     */
    private ObjectNode buildDemoErrorState() {
        return utils.buildErrorSection(
                "demo-error-state",
                "Something went wrong",
                "We couldn't load this content. This is a demo of the ErrorState section type.",
                "error",
                "nba://scoreboard"
        );
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Build a SectionHeader labelling the section type for the kitchen-sink demo.
     */
    private ObjectNode buildTypeLabel(String sectionType) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "label-" + sectionType.toLowerCase());
        section.put("type", "SectionHeader");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", sectionType);
        section.set("data", data);
        return section;
    }

    private ObjectNode demoPlayerRow(String playerId, String name, String position,
                                      String jerseyNumber, boolean starter,
                                      Map<String, Object> stats) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("playerId", playerId);
        row.put("name", name);
        row.put("position", position);
        row.put("jerseyNumber", jerseyNumber);
        row.put("imageUrl", "https://cdn.nba.com/headshots/nba/latest/1040x760/" + playerId + ".png");
        row.put("starter", starter);

        ObjectNode statsNode = objectMapper.createObjectNode();
        stats.forEach((key, value) -> {
            if (value instanceof Integer i) {
                statsNode.put(key, i);
            } else {
                statsNode.put(key, value.toString());
            }
        });
        row.set("stats", statsNode);
        return row;
    }

    private ObjectNode leaderRow(int rank, String pid, String name, String team,
                                  int gp, double min, double pts, double fgm, double fga, double fgPct,
                                  double tpm, double tpa, double tpPct,
                                  double ftm, double fta, double ftPct,
                                  double reb, double ast, double stl, double blk, double tov, double eff) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("rank", rank);
        row.put("playerId", pid);
        row.put("name", name);
        row.put("team", team);
        ObjectNode stats = objectMapper.createObjectNode();
        stats.put("gp", gp); stats.put("min", min); stats.put("pts", pts);
        stats.put("fgm", fgm); stats.put("fga", fga); stats.put("fgPct", fgPct);
        stats.put("tpm", tpm); stats.put("tpa", tpa); stats.put("tpPct", tpPct);
        stats.put("ftm", ftm); stats.put("fta", fta); stats.put("ftPct", ftPct);
        stats.put("reb", reb); stats.put("ast", ast); stats.put("stl", stl);
        stats.put("blk", blk); stats.put("tov", tov); stats.put("eff", eff);
        row.set("stats", stats);
        return row;
    }
}
