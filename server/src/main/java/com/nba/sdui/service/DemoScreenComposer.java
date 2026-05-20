package com.nba.sdui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.nba.sdui.service.AccessibilityHelper.*;

/**
 * Composes the kitchen-sink demo screen showcasing all semantic section types,
 * the Season Leaders table, and the parameterised refresh response for the
 * stats-leaders form.
 */
@Component
public class DemoScreenComposer {

    private final ObjectMapper objectMapper;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public DemoScreenComposer(ObjectMapper objectMapper, SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    // ── Public entry points ────────────────────────────────────────────

    /**
     * Compose a kitchen-sink demo screen showcasing all section types (including
     * atomic primitives like DisplayGrid) with static mock data.  No external API calls.
     */
    public ObjectNode composeDemos(String traceId, String deviceClass, String locale) {
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

        // 1. Game Card (scoreboard composite)
        sections.add(buildTypeLabel("Game Card (scoreboard) (Composite)"));
        sections.add(buildDemoGamePanelScoreboard());
        // 2. AdSlot (between header and top performers)
        sections.add(buildTypeLabel("AdSlot (Semantic)"));
        sections.add(buildDemoAdSlot());
        // 3. StatLine
        sections.add(buildTypeLabel("StatLine (Composite)"));
        sections.add(buildDemoStatLine());
        // 4. PromoBanner
        sections.add(buildTypeLabel("PromoBanner (Composite)"));
        sections.add(buildDemoPromoBanner());
        // 5. HeroPanel
        sections.add(buildTypeLabel("HeroPanel (Composite)"));
        sections.add(buildDemoHeroPanel());
        // 6. ContentRail
        sections.add(buildTypeLabel("ContentRail (Composite)"));
        sections.add(buildDemoContentRail());
        // 7. Game Card
        sections.add(buildTypeLabel("Game Card (Composite)"));
        sections.add(buildDemoGamePanel());
        // 8. Responsive Row (AtomicComposite with breakpoint)
        sections.add(buildTypeLabel("Responsive Row (Composite)"));
        sections.add(buildDemoResponsiveRow());
        // 9. TabGroup
        sections.add(buildTypeLabel("TabGroup (Semantic)"));
        sections.add(buildDemoTabGroup());
        // 10. BoxscoreTable
        sections.add(buildTypeLabel("BoxscoreTable (Semantic)"));
        sections.add(buildDemoBoxscoreTable());
        // 11. Form
        sections.add(buildTypeLabel("Form (Semantic)"));
        sections.add(buildDemoForm(deviceClass));
        // 12. SeasonLeadersTable
        sections.add(buildTypeLabel("SeasonLeadersTable (Semantic)"));
        sections.add(buildDemoLeadersTable());
        // 13. Game Card (featured composite)
        sections.add(buildTypeLabel("Game Card (featured) (Composite)"));
        sections.add(buildDemoFeaturedGamePanel());
        // 14. VideoCarousel
        sections.add(buildTypeLabel("VideoCarousel (Composite)"));
        sections.add(buildDemoVideoCarousel());
        // 15. NbaTvSchedule
        sections.add(buildTypeLabel("NbaTvSchedule (Composite)"));
        sections.add(buildDemoNbaTvSchedule());
        // 16. SubscribeBanner
        sections.add(buildTypeLabel("SubscribeBanner (Semantic)"));
        sections.add(buildDemoSubscribeBanner());
        // 17. SubscribeHero
        sections.add(buildTypeLabel("SubscribeHero (Semantic)"));
        sections.add(buildDemoSubscribeHero());
        // 18. FollowingRail
        sections.add(buildTypeLabel("FollowingRail (Composite)"));
        sections.add(buildDemoFollowingRail());
        // 19. DisplayGrid (atomic primitive)
        sections.add(buildTypeLabel("DisplayGrid (Composite)"));
        sections.add(buildDemoDisplayGrid());
        // 20. ErrorState
        sections.add(buildTypeLabel("ErrorState (Composite)"));
        sections.add(buildDemoErrorState());
        // 21. SectionSlot (bidirectional bridge demo)
        sections.add(buildTypeLabel("SectionSlot (AdSlot in atomic tree) (Composite)"));
        sections.add(buildDemoSectionSlot());
        // 22-28. Feed atomic-composite patterns
        sections.add(buildTypeLabel("SectionHeaderComposite"));
        sections.add(buildDemoSectionHeaderComposite());
        sections.add(buildTypeLabel("StoryCircleRail (Composite)"));
        sections.add(buildDemoStoryCircleRail());
        sections.add(buildTypeLabel("FeaturedLiveGameHero (Composite)"));
        sections.add(buildDemoFeaturedLiveGameHero());
        sections.add(buildTypeLabel("EditorialOverlayRail (Composite)"));
        sections.add(buildDemoEditorialOverlayRail());
        sections.add(buildTypeLabel("UtilityCardGrid (Composite)"));
        sections.add(buildDemoUtilityCardGrid());
        sections.add(buildTypeLabel("LeagueCardRail (Composite)"));
        sections.add(buildDemoLeagueCardRail());
        sections.add(buildTypeLabel("GameScheduleList (Composite)"));
        sections.add(buildDemoGameScheduleList());

        screen.set("sections", sections);
        utils.stampStringTableOnSections(screen, locale);
        return screen;
    }

    /**
     * Compose the standalone Season Leaders screen (Form + SeasonLeadersTable).
     */
    public ObjectNode composeLeaders(String traceId, String deviceClass, String locale) {
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
        sections.add(buildDemoForm(deviceClass));
        sections.add(buildLeadersTable("2025-26", "regular", "per_game", "pts"));
        screen.set("sections", sections);

        return screen;
    }

    /**
     * Compose only the leaders-table section for a parameterised refresh.
     * Called from the controller when the form submits.
     */
    public ObjectNode composeLeadersRefresh(String traceId, Map<String, String> params, String locale) {
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

        utils.stampStringTableOnSections(screen, locale);
        return screen;
    }

    /**
     * Build a season leaders table section with data matching the given filters.
     */
    public ObjectNode buildLeadersTable(String season, String seasonType,
                                         String perMode, String statCategory) {
        String contentSourceId = "stats-api:leaders";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SeasonLeadersTable");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "SeasonLeadersTable");
        section.put("contentSourceId", contentSourceId);
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
     * 1. Game Card (scoreboard composite) — Lakers vs Celtics, period 3, 89-94.
     */
    private ObjectNode buildDemoGamePanelScoreboard() {
        String contentSourceId = "demo:game-panel-scoreboard";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        AtomicCompositeBuilder.GameClockSnapshot clock = new AtomicCompositeBuilder.GameClockSnapshot(
                4 * 60 + 32, java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING);
        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                "demo_game_panel_scoreboard",
                "scoreboard",
                "0022400999",
                2,
                "Q3 4:32",
                null,
                new AtomicCompositeBuilder.GamePanelTeam("LAL", 89, DemoImageUrls.teamLogo("1610612747")),
                new AtomicCompositeBuilder.GamePanelTeam("BOS", 94, DemoImageUrls.teamLogo("1610612738")),
                clock,
                "nba://game/0022400999",
                objectMapper.createObjectNode().put("type", "static"),
                null,
                utils.gamePanelSurface());
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 2. StatLine — 3 mock players with PTS/REB/AST.
     */
    private ObjectNode buildDemoStatLine() {
        String contentSourceId = "demo:stat-line";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] stats = {
            {"1628369", "Jayson Tatum", "BOS", "PTS", "32", DemoImageUrls.headshot("1628369")},
            {"203507", "LeBron James", "LAL", "PTS", "28", DemoImageUrls.headshot("203507")},
            {"203076", "Anthony Davis", "LAL", "REB", "14", DemoImageUrls.headshot("203076")}
        };
        ObjectNode section = atomicBuilder.buildStatLine(
                sectionId, "demo_stat_line", "Top Performers", "vertical", stats);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 3. PromoBanner — "Welcome to SDUI" with the same branded
     * gradient + outer surface as the Subscribe-Now treatment.
     * Root container carries no background — `section.surface`
     * (subscribeSurface) owns the gradient, rounded corners,
     * shadow and outer margin via the shared SectionContainer.
     */
    private ObjectNode buildDemoPromoBanner() {
        String contentSourceId = "demo:promo-banner";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildPromoBanner(
                sectionId, "demo_promo_banner",
                "Welcome to SDUI", null,
                "All 20 semantic section types rendered from a single server response.",
                DemoImageUrls.cardWide("nba"),
                "Learn More", "nba://scoreboard");
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.subscribeSurface(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                20));
        return section;
    }

    /**
     * 4. HeroPanel — single highlight card with thumbnail.
     */
    private ObjectNode buildDemoHeroPanel() {
        String contentSourceId = "demo:hero-panel";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildHeroPanel(
                sectionId, "demo_content_card",
                "Celtics Lead Series 3-1",
                "Boston takes commanding lead after Game 4 victory",
                DemoImageUrls.cardWide("celtics"),
                "article", null, "nba://article/celtics-lead-series");
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 5. ContentRail — 4 cards (Top Plays, Player Spotlight, etc.).
     */
    private ObjectNode buildDemoContentRail() {
        String contentSourceId = "demo:content-rail";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] cards = {
            {"rail-1", "Top 10 Plays", "Last night's best moments", DemoImageUrls.cardWide("highlights"), "video", null, "nba://content/rail-1"},
            {"rail-2", "Player Spotlight", "Jayson Tatum's monster game", DemoImageUrls.cardWide("player"), "article", null, "nba://content/rail-2"},
            {"rail-3", "Draft Preview", "Top prospects for 2025 draft", DemoImageUrls.cardWide("draft"), "article", null, "nba://content/rail-3"},
            {"rail-4", "Playoff Bracket", "Updated bracket after today's results", DemoImageUrls.cardWide("playoffs"), "interactive", null, "nba://content/rail-4"}
        };
        ObjectNode section = atomicBuilder.buildContentRail(
                sectionId, "demo_content_rail", "Featured Content", cards);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 6. Game Card — mock game tile for a single game.
     */
    private ObjectNode buildDemoGamePanel() {
        String contentSourceId = "demo:game-panel";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                "demo_game_card",
                "standard",
                "0022400888",
                3,
                "Final",
                null,
                new AtomicCompositeBuilder.GamePanelTeam("HOU", 105, DemoImageUrls.teamLogo("1610612745")),
                new AtomicCompositeBuilder.GamePanelTeam("GSW", 112, DemoImageUrls.teamLogo("1610612744")),
                null,
                "nba://game/0022400888",
                objectMapper.createObjectNode().put("type", "static"),
                null,
                utils.gamePanelSurface());
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 8. Responsive Row — two stat lines side-by-side above 600dp, stacked below.
     * Uses AtomicComposite with Container(direction=row, breakpoint=600) + SectionSlots
     * to replace the old Row section type.
     */
    private ObjectNode buildDemoResponsiveRow() {
        String contentSourceId = "demo:responsive-row";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode scoringLeader = atomicBuilder.buildStatLine(
                "row-scoring-leader", null, "Scoring Leader", "vertical",
                new String[][]{{"203999", "Nikola Jokić", "DEN", "PTS", "26.4",
                        DemoImageUrls.headshot("203999")}});

        ObjectNode assistsLeader = atomicBuilder.buildStatLine(
                "row-assists-leader", null, "Assists Leader", "vertical",
                new String[][]{{"201566", "Trae Young", "ATL", "AST", "11.1",
                        DemoImageUrls.headshot("201566")}});

        ObjectNode root = atomicBuilder.responsiveRow(16, 600);
        root.put("id", "demo-row-container");
        ArrayNode children = objectMapper.createArrayNode();

        ObjectNode leftSlot = atomicBuilder.sectionSlot("row-left", scoringLeader);
        atomicBuilder.setFlex(leftSlot, 1);
        children.add(leftSlot);

        ObjectNode rightSlot = atomicBuilder.sectionSlot("row-right", assistsLeader);
        atomicBuilder.setFlex(rightSlot, 1);
        children.add(rightSlot);

        root.set("children", children);
        ObjectNode section = atomicBuilder.wrapAsComposite(sectionId, "demo_row", root);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 8. TabGroup — two tabs ("Overview", "Stats") each with a HeroPanel child.
     */
    private ObjectNode buildDemoTabGroup() {
        String contentSourceId = "demo:tab-group";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "TabGroup");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "TabGroup");
        section.put("contentSourceId", contentSourceId);
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
        overviewContent.add(atomicBuilder.buildHeroPanel(
                "tab-overview-card", null,
                "Season Overview",
                "The 2024-25 season has been full of surprises",
                DemoImageUrls.cardWide("season"),
                "article", null, null));
        tabContents.set("overview", overviewContent);

        ArrayNode statsContent = objectMapper.createArrayNode();
        statsContent.add(atomicBuilder.buildHeroPanel(
                "tab-stats-card", null,
                "League Statistical Leaders",
                "Points, rebounds, assists and more",
                DemoImageUrls.cardWide("stats"),
                "interactive", null, null));
        tabContents.set("stats", statsContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        return section;
    }

    /**
     * 9. BoxscoreTable — 3 players, 5 stat columns.
     */
    private ObjectNode buildDemoBoxscoreTable() {
        String contentSourceId = "demo:boxscore-table";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "BoxscoreTable");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "BoxscoreTable");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "demo_boxscore_table");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("teamTricode", "BOS");
        data.put("teamName", "Boston Celtics");
        // NBA brand guideline: team primary colors are brand assets owned by the team,
        // not design-system tokens. BOS primary = PMS 342 (#007A33). If Rights
        // & Brand ever changes the mapping, update here — the color registry is
        // not the source of truth for team brand identity.
        data.put("teamColor", "#007A33");
        data.put("teamLogoUrl", DemoImageUrls.teamLogo("1610612738"));

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
    * Layout is device-class-driven: web/desktop gets "horizontal" (single-row),
    * phone/tablet/tv gets "vertical" (stacked) for better touch targets.
     */
    private ObjectNode buildDemoForm(String deviceClass) {
        String contentSourceId = "demo:form";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "Form");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "Form");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "demo_stats_filter_form");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        // Wide viewports (web/desktop) use horizontal form layout; narrow (phone/tablet/tv) use vertical
        String formLayout = ("web".equalsIgnoreCase(deviceClass) || "desktop".equalsIgnoreCase(deviceClass))
            ? "horizontal" : "vertical";
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
        submitAction.put("target", SectionIdDeriver.derive("stats-api:leaders", "SeasonLeadersTable"));
        submitAction.put("endpoint", "/v1/sdui/refresh/stats-leaders");
        submitAction.put("onFailure", "halt");
        ObjectNode submitFeedback = objectMapper.createObjectNode();
        submitFeedback.put("message", "Stats lookup failed — please try again");
        submitFeedback.put("style", "snackbar");
        submitAction.set("failureFeedback", submitFeedback);
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
        String contentSourceId = "ads:gam-demo-top";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "AdSlot");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "demo_ad_slot");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        section.set("surface", utils.adSlotSurface());

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

        ObjectNode placeholder = objectMapper.createObjectNode();
        placeholder.put("backgroundColor", "token:nba.bg.tertiary");
        placeholder.put("text", "Advertisement");
        data.set("placeholder", placeholder);

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
     * 13. Game Card (featured composite) — hero-sized game card with background image and badge.
     */
    private ObjectNode buildDemoFeaturedGamePanel() {
        String contentSourceId = "demo:featured-game-panel";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        AtomicCompositeBuilder.GameClockSnapshot clock = new AtomicCompositeBuilder.GameClockSnapshot(
                2 * 60 + 15, java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING);
        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                "demo_featured_game_panel",
                "featured",
                "0022400777",
                2,
                "Q4 2:15",
                "LIVE",
                new AtomicCompositeBuilder.GamePanelTeam("BKN", 97, DemoImageUrls.teamLogo("1610612751")),
                new AtomicCompositeBuilder.GamePanelTeam("MIA", 101, DemoImageUrls.teamLogo("1610612748")),
                clock,
                "nba://game/0022400777",
                objectMapper.createObjectNode().put("type", "static"),
                null,
                null);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 14. VideoCarousel — 4 video thumbnails in a horizontal carousel.
     */
    private ObjectNode buildDemoVideoCarousel() {
        String contentSourceId = "demo:video-carousel";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] items = {
            {"vid-1", "Dončić No-Look Dime", "Lakers vs Celtics", DemoImageUrls.cardWide("pass"), "1:24", "NEW", "nba://video/vid-1"},
            {"vid-2", "SGA Crossover & Finish", "Thunder vs Nuggets", DemoImageUrls.cardWide("crossover"), "0:48", null, "nba://video/vid-2"},
            {"vid-3", "Edwards Poster Dunk", "Timberwolves vs Suns", DemoImageUrls.cardWide("dunk"), "0:32", "NEW", "nba://video/vid-3"},
            {"vid-4", "Jokić Triple-Double Recap", "Full highlights", DemoImageUrls.cardWide("triple-double"), "3:15", null, "nba://video/vid-4"}
        };
        ObjectNode section = atomicBuilder.buildVideoCarousel(
                sectionId, "demo_video_carousel",
                "Top Highlights", "Today's best plays", items);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 15. NbaTvSchedule — hero promo + 3 time slots.
     */
    private ObjectNode buildDemoNbaTvSchedule() {
        String contentSourceId = "demo:nbatv-schedule";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] slots = {
            {"slot-1", "NBA GameTime", "Pre-game analysis and predictions", "18:00", "false", null},
            {"slot-2", "LAL @ BOS", "Live game broadcast", "19:30", "true", "nba://game/0022400999"},
            {"slot-3", "NBA Inside Stuff", "Post-game interviews and highlights", "22:00", "false", null}
        };
        ObjectNode section = atomicBuilder.buildNbaTvSchedule(
                sectionId, "demo_nbatv_schedule",
                DemoImageUrls.hero("arena"),
                "NBA TV Live",
                "Lakers vs Celtics — Coverage begins at 7:00 PM ET",
                true, slots);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.cardSurface());
        return section;
    }

    /**
     * 16. SubscribeBanner — inline League Pass upsell. Visible surface is
     * expressed as an atomic tree under {@code data.ui}. Reserved SDK
     * integration point.
     */
    private ObjectNode buildDemoSubscribeBanner() {
        String contentSourceId = "demo:subscribe-banner";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeBanner");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "SubscribeBanner");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "demo_subscribe_banner");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        section.set("surface", utils.subscribeSurface(
                ColorTokens.BRAND_NBA,
                "#862633",
                20));

        ObjectNode ctaAction = objectMapper.createObjectNode();
        ctaAction.put("trigger", "onActivate");
        ctaAction.put("type", "navigate");
        ctaAction.put("targetUri", "nba://subscribe");
        ctaAction.put("presentation", "modal");

        ObjectNode root = atomicBuilder.container("column", "start", "start");
        root.put("gap", 4);
        ArrayNode children = objectMapper.createArrayNode();
        ObjectNode bannerTitle = atomicBuilder.text("Never Miss a Game", "titleMedium", "bold",
                ColorTokens.TEXT_INVERSE, null);
        addHeading(objectMapper, bannerTitle, "Never Miss a Game", 2);
        children.add(bannerTitle);
        children.add(atomicBuilder.text(
                "Stream every out-of-market game live with NBA League Pass.",
                "bodySmall", null, ColorTokens.TEXT_INVERSE, null));
        children.add(atomicBuilder.spacer(LayoutTokens.SPACING_MD));
        children.add(atomicBuilder.button("Subscribe Now", "primary", ctaAction.deepCopy()));
        root.set("children", children);

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.set("ctaAction", ctaAction);
        section.set("data", data);
        return section;
    }

    /**
     * 17. SubscribeHero — full-width subscription hero with pricing tiers.
     * Visible surface is expressed as an atomic tree under {@code data.ui}.
     * {@code data.tiers} is retained for the future IAP SDK to bind product
     * identifiers; the renderer reads nothing from it today.
     */
    private ObjectNode buildDemoSubscribeHero() {
        String contentSourceId = "demo:subscribe-hero";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "SubscribeHero");
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "SubscribeHero");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "demo_subscribe_hero");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        section.set("surface", utils.subscribeSurface(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                24));

        ObjectNode root = atomicBuilder.container("column", "start", "center");
        root.put("gap", 8);
        root.put("widthMode", "fill");
        ArrayNode children = objectMapper.createArrayNode();

        children.add(atomicBuilder.image(
                DemoImageUrls.logoWide(), 0, 64, "contain"));
        // Logo is decorative — title below provides the accessible label.
        addHidden(objectMapper, (ObjectNode) children.get(children.size() - 1));
        children.add(atomicBuilder.spacer(LayoutTokens.SPACING_MD));
        ObjectNode heroTitle = atomicBuilder.text("NBA League Pass", "headlineMedium", "bold",
                ColorTokens.TEXT_INVERSE, null);
        addHeading(objectMapper, heroTitle, "NBA League Pass", 2);
        children.add(heroTitle);
        children.add(atomicBuilder.text("Watch every game. Your way.",
                "bodyLarge", null, ColorTokens.TEXT_INVERSE, null));
        children.add(atomicBuilder.spacer(LayoutTokens.SPACING_LG));

        String[] features = {
                "Live & on-demand out-of-market games",
                "Multiple viewing angles and condensed replays",
                "NBA TV included",
                "Compatible with all major devices"
        };
        ObjectNode featuresCol = atomicBuilder.container("column", "start", "start");
        featuresCol.put("gap", 8);
        featuresCol.put("widthMode", "fill");
        ArrayNode featureChildren = objectMapper.createArrayNode();
        for (String feature : features) {
            ObjectNode row = atomicBuilder.container("row", "start", "center");
            row.put("gap", 8);
            ArrayNode rowChildren = objectMapper.createArrayNode();
            rowChildren.add(atomicBuilder.text("✓", "bodyLarge", "bold",
                    "token:nba.color.feedback.success.70", null));
            rowChildren.add(atomicBuilder.text(feature, "bodyLarge", null,
                    ColorTokens.TEXT_INVERSE, null));
            row.set("children", rowChildren);
            featureChildren.add(row);
        }
        featuresCol.set("children", featureChildren);
        children.add(featuresCol);

        children.add(atomicBuilder.spacer(20));

        ObjectNode tiersCol = atomicBuilder.container("column", "start", "stretch");
        tiersCol.put("gap", 16);
        tiersCol.put("widthMode", "fill");
        ArrayNode tierChildren = objectMapper.createArrayNode();
        tierChildren.add(buildDemoTierUi("League Pass", "$14.99/mo", "$22.99/mo",
                "MOST POPULAR",
                new String[]{"All out-of-market games", "3 concurrent streams", "HD quality"},
                "Start Free Trial", "nba://subscribe/standard"));
        tierChildren.add(buildDemoTierUi("League Pass Premium", "$22.99/mo", null,
                "BEST VALUE",
                new String[]{"Everything in League Pass", "No ads on VOD",
                        "Unlimited concurrent streams", "In-arena camera angles"},
                "Go Premium", "nba://subscribe/premium"));
        tiersCol.set("children", tierChildren);
        children.add(tiersCol);

        root.set("children", children);

        ArrayNode tiers = objectMapper.createArrayNode();
        tiers.add(demoTierProductId("tier-standard", "League Pass", "$14.99/mo"));
        tiers.add(demoTierProductId("tier-premium", "League Pass Premium", "$22.99/mo"));

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.set("tiers", tiers);
        section.set("data", data);
        return section;
    }

    /** Build the atomic tree for one tier card in the hero. */
    private ObjectNode buildDemoTierUi(String name, String price, String originalPrice,
                                            String badgeText, String[] features,
                                            String ctaLabel, String ctaUri) {
        ObjectNode card = atomicBuilder.container("column", "start", "start");
        card.put("gap", 6);
        card.put("background", "rgba(255,255,255,0.1)");
        card.put("cornerRadius", 16);
        card.set("padding", atomicBuilder.padding(22, 22, 20, 20));
        card.put("widthMode", "fill");

        ArrayNode cardChildren = objectMapper.createArrayNode();
        if (badgeText != null) {
            cardChildren.add(atomicBuilder.text(badgeText, "labelMedium", "bold",
                    "token:nba.color.secondary.50", null));
        }
        ObjectNode tierName = atomicBuilder.text(name, "titleLarge", "bold",
                ColorTokens.TEXT_INVERSE, null);
        addHeading(objectMapper, tierName, name, 3);
        cardChildren.add(tierName);
        cardChildren.add(atomicBuilder.text(price, "headlineSmall", "bold",
                ColorTokens.TEXT_INVERSE, null));
        if (originalPrice != null) {
            // Translucent white intentionally communicates a strikethrough/dimmed
            // price; alpha compositing is allowed where no semantic token encodes
            // the same translucency.
            cardChildren.add(atomicBuilder.text(originalPrice, "bodyMedium", null,
                    "rgba(255,255,255,0.6)", null));
        }
        if (features != null) {
            for (String f : features) {
                cardChildren.add(atomicBuilder.text("• " + f, "bodyMedium", null,
                        ColorTokens.TEXT_INVERSE, null));
            }
        }
        cardChildren.add(atomicBuilder.spacer(10));

        ObjectNode tierAction = objectMapper.createObjectNode();
        tierAction.put("trigger", "onActivate");
        tierAction.put("type", "navigate");
        tierAction.put("targetUri", ctaUri);
        cardChildren.add(atomicBuilder.button(ctaLabel, "primary", tierAction));

        card.set("children", cardChildren);
        return card;
    }

    private ObjectNode demoTierProductId(String id, String name, String price) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", id);
        n.put("name", name);
        n.put("price", price);
        return n;
    }

    /**
     * 18. FollowingRail — horizontal rail of 5 followed teams/players.
     */
    private ObjectNode buildDemoFollowingRail() {
        String contentSourceId = "demo:following-rail";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] items = {
            {"team-lal", "Lakers", DemoImageUrls.teamLogo("1610612747"), "team", "nba://team/1610612747"},
            {"team-bos", "Celtics", DemoImageUrls.teamLogo("1610612738"), "team", "nba://team/1610612738"},
            {"player-luka", "Luka Dončić", DemoImageUrls.headshot("203999"), "player", "nba://player/203999"},
            {"team-gsw", "Warriors", DemoImageUrls.teamLogo("1610612744"), "team", "nba://team/1610612744"},
            {"player-sga", "Shai Gilgeous-Alexander", DemoImageUrls.headshot("1630175"), "player", "nba://player/1630175"}
        };
        ObjectNode section = atomicBuilder.buildFollowingRail(
                sectionId, "demo_following_rail", "Following", items);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 19. DisplayGrid — display-only standings table via the atomic DisplayGrid primitive.
     */
    private ObjectNode buildDemoDisplayGrid() {
        String contentSourceId = "demo:display-grid";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] columns = {
            {"team", "Team", "start"},
            {"w", "W", "center"},
            {"l", "L", "center"},
            {"pct", "PCT", "center"},
            {"gb", "GB", "center"},
            {"strk", "STRK", "center"}
        };
        String[][] rows = {
            {"BOS", "52", "14", ".788", "-", "W5"},
            {"CLE", "49", "17", ".742", "3.0", "W2"},
            {"NYK", "44", "22", ".667", "8.0", "L1"},
            {"MIL", "42", "24", ".636", "10.0", "W3"},
            {"ORL", "39", "27", ".591", "13.0", "L2"},
            {"IND", "38", "28", ".576", "14.0", "W1"},
            {"PHI", "35", "31", ".530", "17.0", "L3"},
            {"MIA", "33", "33", ".500", "19.0", "W1"}
        };
        ObjectNode section = atomicBuilder.buildDisplayGrid(
                sectionId, "demo_display_grid",
                "Eastern Conference Standings",
                columns, rows,
                false);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * 20. ErrorState — server-driven error with retry action.
     */
    private ObjectNode buildDemoErrorState() {
        String contentSourceId = "demo:error-state";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildErrorState(
                sectionId,
                "Something went wrong",
                "We couldn't load this content. This is a demo of the ErrorState section type.",
                "error",
                "nba://scoreboard");
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    /**
     * Build a demo SectionSlot — an AtomicComposite containing a game card
     * with an embedded AdSlot section via the SectionSlot element type.
     * This demonstrates the bidirectional bridge: AtomicRouter → SectionRouter.
     */
    private ObjectNode buildDemoSectionSlot() {
        // Build the embedded AdSlot section
        String adContentSourceId = "ads:gam-demo-inline";
        String adSectionId = SectionIdDeriver.derive(adContentSourceId, "AdSlot");
        ObjectNode adSection = objectMapper.createObjectNode();
        adSection.put("id", adSectionId);
        adSection.put("type", "AdSlot");
        adSection.put("contentSourceId", adContentSourceId);
        adSection.set("surface", utils.adSlotSurface());
        ObjectNode adData = objectMapper.createObjectNode();
        adData.put("provider", "gam");
        adData.put("adUnitPath", "/nba/game-card-inline");
        ArrayNode adSizes = objectMapper.createArrayNode();
        ArrayNode adSize = objectMapper.createArrayNode();
        adSize.add(320); adSize.add(50);
        adSizes.add(adSize);
        adData.set("sizes", adSizes);
        adData.put("collapseOnEmpty", true);
        adData.put("label", "Advertisement");
        ObjectNode adPlaceholder = objectMapper.createObjectNode();
        adPlaceholder.put("backgroundColor", "token:nba.bg.tertiary");
        adPlaceholder.put("text", "Advertisement");
        adData.set("placeholder", adPlaceholder);
        adSection.set("data", adData);
        ObjectNode adRefresh = objectMapper.createObjectNode();
        adRefresh.put("type", "static");
        adSection.set("refreshPolicy", adRefresh);
        ObjectNode adStates = objectMapper.createObjectNode();
        ObjectNode errorState = objectMapper.createObjectNode();
        errorState.put("hideOnError", true);
        adStates.set("error", errorState);
        adSection.set("sectionStates", adStates);

        // Build the atomic tree: game card with inline ad via SectionSlot
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "Container");
        root.put("direction", "column");
        root.put("background", ColorTokens.SURFACE_CANVAS);
        root.put("cornerRadius", 12);
        ObjectNode padding = objectMapper.createObjectNode();
        padding.put("start", 16); padding.put("end", 16);
        padding.put("top", 12); padding.put("bottom", 12);
        root.set("padding", padding);

        ArrayNode children = objectMapper.createArrayNode();

        ObjectNode title = objectMapper.createObjectNode();
        title.put("type", "Text");
        title.put("content", "LAL vs BOS");
        title.put("variant", "titleMedium");
        title.put("weight", "bold");
        title.put("color", ColorTokens.TEXT_PRIMARY);
        addHeading(objectMapper, title, "LAL vs BOS", 3);
        children.add(title);

        ObjectNode subtitle = objectMapper.createObjectNode();
        subtitle.put("type", "Text");
        subtitle.put("content", "Q3 5:42 \u2022 LAL 87 - BOS 82");
        subtitle.put("variant", "bodySmall");
        subtitle.put("color", ColorTokens.TEXT_TERTIARY);
        children.add(subtitle);

        ObjectNode divider = objectMapper.createObjectNode();
        divider.put("type", "Divider");
        divider.put("color", ColorTokens.BORDER_DEFAULT);
        divider.put("thickness", 1);
        children.add(divider);

        // SectionSlot: embed the AdSlot section
        children.add(atomicBuilder.sectionSlot("inline-ad", adSection));

        root.set("children", children);

        String contentSourceId = "demo:section-slot";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.wrapAsComposite(sectionId, "demo-section-slot", root);
        section.put("contentSourceId", contentSourceId);
        return section;
    }

    private ObjectNode buildDemoSectionHeaderComposite() {
        String contentSourceId = "feed:demo";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "section-header");
        ObjectNode section = atomicBuilder.buildSectionHeaderComposite(
                sectionId,
                "demo_section_header_composite",
                "Top Stories",
                "Fresh from around the league",
                "More",
                "nba://news");
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.sectionHeaderSurface());
        return section;
    }

    private ObjectNode buildDemoStoryCircleRail() {
        String contentSourceId = "demo:story-circle-rail";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] items = {
                {"story-finals", "Finals", DemoImageUrls.teamLogo("1610612738"), "LIVE", "nba://stories/finals"},
                {"story-lakers", "Lakers", DemoImageUrls.teamLogo("1610612747"), "NEW", "nba://stories/lakers"},
                {"story-draft", "Draft", DemoImageUrls.cardWide("draft"), null, "nba://stories/draft"},
                {"story-nbatv", "NBA TV", DemoImageUrls.cardWide("nbatv"), "LIVE", "nba://watch/nbatv"}
        };
        ObjectNode section = atomicBuilder.buildStoryCircleRail(
                sectionId, "demo_story_circle_rail", null, items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildDemoEditorialOverlayRail() {
        String contentSourceId = "demo:editorial-overlay-rail";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] cards = {
                {"editorial-1", "Five things to watch tonight", "Rivalries, returns, and playoff stakes",
                        DemoImageUrls.cardWide("editorial-1"), "NEW", "nba://article/five-things"},
                {"editorial-2", "Inside the MVP race", "The numbers behind a tight finish",
                        DemoImageUrls.cardWide("editorial-2"), null, "nba://article/mvp-race"},
                {"editorial-3", "Rookies making noise", "First-year players changing rotations",
                        DemoImageUrls.cardWide("editorial-3"), "LIVE", "nba://video/rookies"}
        };
        ObjectNode section = atomicBuilder.buildEditorialOverlayRail(
                sectionId, "demo_editorial_overlay_rail", null, cards);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildDemoFeaturedLiveGameHero() {
        String contentSourceId = "demo:featured-live-game-hero";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String bosLogo = DemoImageUrls.teamLogo("1610612738");
        String lalLogo = DemoImageUrls.teamLogo("1610612747");
        String okcLogo = DemoImageUrls.teamLogo("1610612760");
        String denLogo = DemoImageUrls.teamLogo("1610612743");
        String[][] cards = {
                {"hero-game-1", "LIVE", "Lakers at Celtics", "Fourth-quarter finish on NBA TV",
                        DemoImageUrls.cardWide("hero-lal-bos"),
                        "LAL", "89", lalLogo, "BOS", "94", bosLogo, "Q4 2:15", "BOS leads 3-2",
                        DemoImageUrls.logoWide(), "nba://game/0022400777",
                        "nba://game/0022400777/actions"},
                {"hero-game-2", "UP NEXT", "Thunder at Nuggets", "Coverage begins at 10:00 PM ET",
                        DemoImageUrls.cardWide("hero-okc-den"),
                        "OKC", null, okcLogo, "DEN", null, denLogo, "10:00 PM ET", "Season series tied",
                        DemoImageUrls.logoWide(), "nba://game/0022400778", null}
        };
        ObjectNode section = atomicBuilder.buildFeaturedLiveGameHero(
                sectionId, "demo_featured_live_game_hero", null, cards);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildDemoUtilityCardGrid() {
        String contentSourceId = "demo:utility-card-grid";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] items = {
                {"utility-standings", "Standings", "Conference and division tables",
                        DemoImageUrls.cardWide("standings"), "nba://standings"},
                {"utility-stats", "Stats", "League leaders and team ranks",
                        null, "nba://stats"},
                {"utility-tickets", "Tickets", "Find games near you",
                        null, "nba://tickets"},
                {"utility-shop", "Shop", "Jerseys, hats, and more",
                        DemoImageUrls.cardWide("shop"), "nba://shop"}
        };
        ObjectNode section = atomicBuilder.buildUtilityCardGrid(
                sectionId, "demo_utility_card_grid", "Around the League", items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.cardSurface());
        return section;
    }

    private ObjectNode buildDemoLeagueCardRail() {
        String contentSourceId = "demo:league-card-rail";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] items = {
                {"league-wnba", "WNBA", DemoImageUrls.cardWide("league-wnba"), "nba://league/wnba"},
                {"league-gleague", "G League", DemoImageUrls.cardWide("league-gleague"), "nba://league/gleague"},
                {"league-2k", "NBA 2K League", null, "nba://league/2k"}
        };
        ObjectNode section = atomicBuilder.buildLeagueCardRail(
                sectionId, "demo_league_card_rail", "Other Leagues", items);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildDemoGameScheduleList() {
        String contentSourceId = "demo:game-schedule-list";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        String[][] rows = {
                {"schedule-demo-1", "NYK", "Knicks", "3", "109", DemoImageUrls.teamLogo("1610612752"),
                        "BOS", "Celtics", "2", "132", DemoImageUrls.teamLogo("1610612738"),
                        "Final", "BOS leads 1-0", DemoImageUrls.logoWide(), "nba://game/0022400001", "nba://game/0022400001/actions"},
                {"schedule-demo-2", "MIN", "Timberwolves", "6", "103", DemoImageUrls.teamLogo("1610612750"),
                        "LAL", "Lakers", "7", "110", DemoImageUrls.teamLogo("1610612747"),
                        "Final", null, DemoImageUrls.logoWide(), "nba://game/0022400002", null}
        };
        ObjectNode section = atomicBuilder.buildGameScheduleList(
                sectionId, "demo_game_schedule_list", "Today", rows);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.cardSurface());
        return section;
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Build a SectionHeader labelling the section type for the kitchen-sink demo.
     */
    private ObjectNode buildTypeLabel(String sectionType) {
        String contentSourceId = "feed:demo";
        String slug = "label-" + sectionType.toLowerCase().replaceAll("[^a-z0-9\\-]", "-");
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug);
        ObjectNode section = atomicBuilder.buildSectionHeader(
                sectionId, sectionType, null, null, null);
        section.put("contentSourceId", contentSourceId);
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
        row.put("imageUrl", DemoImageUrls.headshot(playerId));
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
