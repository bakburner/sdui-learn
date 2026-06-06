package com.nba.sdui.domain.composer;

import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.BoxscorePlayerRow;
import com.nba.sdui.models.generated.BoxscoreTable;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionStates;
import com.nba.sdui.models.generated.State;
import com.nba.sdui.models.generated.Subsection;
import com.nba.sdui.models.generated.TabContents;
import com.nba.sdui.models.generated.TabData;
import com.nba.sdui.models.generated.TabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.port.StatsPort;
import com.nba.sdui.integration.model.boxscore.BoxscoreGame;
import com.nba.sdui.integration.model.boxscore.BoxscorePlayer;
import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;
import com.nba.sdui.integration.model.boxscore.BoxscoreStatistics;
import com.nba.sdui.integration.model.boxscore.BoxscoreTeam;

/**
 * Composes the dedicated Boxscore SDUI screen.
 *
 * <p>Owns the {@code BoxscoreTable} section builder that is also reused by
 * {@link GameDetailComposer} for the boxscore tab group at the bottom of
 * game detail.
 */
@Component
public class BoxscoreComposer {

    private static final Logger log = LoggerFactory.getLogger(BoxscoreComposer.class);

    private final StatsPort statsPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public BoxscoreComposer(StatsPort statsPort, SduiUtils utils, SectionSurfaces surfaces) {
        this.statsPort = statsPort;
        this.utils = utils;
        this.surfaces = surfaces;
    }

    // ── Public entry point ─────────────────────────────────────────────

    /**
     * Compose a dedicated Boxscore SDUI screen.
     *
     * Layout:
     * <pre>
     *   Screen
     *     state: { boxscore_team, boxscore_away_sortCol, boxscore_away_sortDir,
     *              boxscore_home_sortCol, boxscore_home_sortDir }
     *     sections:
     *       └── TabGroup (team toggle — eager, inline)
     *           ├── Tab "{awayTricode}" → BoxscoreTable
     *           └── Tab "{homeTricode}" → BoxscoreTable
     * </pre>
     *
     * Source data: NBA CDN boxscore endpoint → {@code game.homeTeam / game.awayTeam}.
     */
    public Screen composeBoxscore(String gameId, String traceId, String locale) throws IOException {
        log.info("Composing boxscore screen: gameId={}, locale={}", gameId, locale);

        String contentSourceId = "stats-api:game-" + gameId;

        BoxscoreResponse boxscore = statsPort.getBoxscore(gameId);
        BoxscoreGame game = boxscore != null ? boxscore.getGame() : null;

        boolean hasLiveData = game != null && game.getHomeTeam() != null;

        Screen response = new Screen();
        response.setId("boxscore-" + gameId);
        response.setSchemaVersion(schemaVersion);
        response.setParentUri("nba://scoreboard");
        response.setNavigation(utils.buildNavigation("game-detail"));

        if (!hasLiveData) {
            log.warn("No boxscore data available for gameId={}, returning empty screen", gameId);
            response.setState(new State());
            List<Section> sections = new ArrayList<>();
            Section emptySection = new Section();
            emptySection.setId(SectionIdDeriver.derive(contentSourceId, "BoxscoreTable", "empty"));
            emptySection.setContentSourceId(contentSourceId);
            emptySection.setType(Section.Type.BOXSCORE_TABLE);
            BoxscoreTable emptyData = new BoxscoreTable();
            emptyData.setTeamTricode("");
            emptyData.setTeamName("");
            emptyData.setColumns(utils.buildBoxscoreColumns());
            emptyData.setPlayers(java.util.List.of());
            emptyData.setEmptyMessage("Box score will be available once the game begins.");
            emptySection.setData(emptyData);
            sections.add(emptySection);
            response.setSections(sections);
            response.setTitle("Box Score");
            utils.prependAppBarHeaderIfNeeded(response);
            utils.ensureScreenContentInsets(response);
            utils.stampStringTableOnSections(response, locale);
            return response;
        }

        BoxscoreTeam homeTeam = game.getHomeTeam();
        BoxscoreTeam awayTeam = game.getAwayTeam();
        String homeTricode = homeTeam != null && homeTeam.getTeamTricode() != null ? homeTeam.getTeamTricode() : "HOME";
        String awayTricode = awayTeam != null && awayTeam.getTeamTricode() != null ? awayTeam.getTeamTricode() : "AWAY";
        int gameStatus = game.getGameStatus();

        // ── Screen state ───────────────────────────────────────────────
        State state = new State();
        state.setAdditionalProperty("boxscore_team", awayTricode);
        state.setAdditionalProperty("boxscore_away_sortCol", "points");
        state.setAdditionalProperty("boxscore_away_sortDir", "desc");
        state.setAdditionalProperty("boxscore_home_sortCol", "points");
        state.setAdditionalProperty("boxscore_home_sortDir", "desc");
        response.setState(state);

        // ── Sections ───────────────────────────────────────────────────
        List<Section> sections = new ArrayList<>();

        Section awayTable = buildBoxscoreTableSectionInternal(
                awayTeam, gameId, contentSourceId, "away",
                "boxscore_away_sortCol", "boxscore_away_sortDir", gameStatus);
        awayTable.setSurface(surfaces.flushSurface());
        Section homeTable = buildBoxscoreTableSectionInternal(
                homeTeam, gameId, contentSourceId, "home",
                "boxscore_home_sortCol", "boxscore_home_sortDir", gameStatus);
        homeTable.setSurface(surfaces.flushSurface());

        // Wrap in TabGroup for team toggling
        Section tabGroup = new Section();
        tabGroup.setId(SectionIdDeriver.derive(contentSourceId, "TabGroup", "team-tabs"));
        tabGroup.setContentSourceId(contentSourceId);
        tabGroup.setType(Section.Type.TAB_GROUP);
        tabGroup.setAnalyticsId("boxscore_team_toggle");

        TabGroup tabData = new TabGroup();
        tabData.setStateKey("boxscore_team");
        tabData.setDefaultTab(awayTricode);

        TabData awayTab = new TabData();
        awayTab.setId("tab-" + awayTricode.toLowerCase());
        awayTab.setLabel(awayTricode);
        awayTab.setStateKey("boxscore_team");
        awayTab.setStateValue(awayTricode);

        TabData homeTab = new TabData();
        homeTab.setId("tab-" + homeTricode.toLowerCase());
        homeTab.setLabel(homeTricode);
        homeTab.setStateKey("boxscore_team");
        homeTab.setStateValue(homeTricode);

        List<TabData> tabsList = List.of(awayTab, homeTab);
        tabData.setTabs(tabsList);

        TabContents tabContents = new TabContents();
        tabContents.setAdditionalProperty(awayTricode, List.of(awayTable));
        tabContents.setAdditionalProperty(homeTricode, List.of(homeTable));
        tabData.setTabContents(tabContents);

        tabGroup.setData(tabData);
        tabGroup.setSubsections(tabSelectSubsections(tabsList, "boxscore_team"));
        tabGroup.setSurface(surfaces.stripSurfaceWithoutBackground());

        sections.add(tabGroup);
        response.setSections(sections);

        log.info("Boxscore screen composed: {} @ {}, gameStatus={}, awayPlayers={}, homePlayers={}",
                awayTricode, homeTricode, gameStatus,
                awayTeam != null && awayTeam.getPlayers() != null ? awayTeam.getPlayers().size() : 0,
                homeTeam != null && homeTeam.getPlayers() != null ? homeTeam.getPlayers().size() : 0);

        response.setTitle(awayTricode + " @ " + homeTricode);
        utils.prependAppBarHeaderIfNeeded(response);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── BoxscoreTable section builder (package-private for GameDetailComposer) ──

    /**
     * Build a single {@code BoxscoreTable} section for one team.
     */
    public Section buildBoxscoreTableSection(BoxscoreTeam team, String gameId,
                                          String contentSourceId, String slug,
                                          String sortColKey, String sortDirKey,
                                          int gameStatus) {
        return buildBoxscoreTableSectionInternal(team, gameId, contentSourceId, slug,
                sortColKey, sortDirKey, gameStatus);
    }

    private Section buildBoxscoreTableSectionInternal(BoxscoreTeam team, String gameId,
                                          String contentSourceId, String slug,
                                          String sortColKey, String sortDirKey,
                                          int gameStatus) {
        String teamTricode = team != null && team.getTeamTricode() != null ? team.getTeamTricode() : "";
        String teamName = team != null && team.getTeamName() != null ? team.getTeamName() : "";
        String teamCity = team != null && team.getTeamCity() != null ? team.getTeamCity() : "";
        int teamId = team != null && team.getTeamId() != null ? team.getTeamId() : 0;

        Section section = new Section();
        String sectionId = SectionIdDeriver.derive(contentSourceId, "BoxscoreTable", slug);
        section.setId(sectionId);
        section.setContentSourceId(contentSourceId);
        section.setType(Section.Type.BOXSCORE_TABLE);
        section.setAnalyticsId("boxscore_table_" + teamTricode.toLowerCase());

        if (gameStatus == 2) {
            RefreshPolicy refreshPolicy = new RefreshPolicy();
            refreshPolicy.setType(RefreshPolicy.RefreshType.POLL);
            refreshPolicy.setIntervalMs(30000);
            refreshPolicy.setUrl(
                    "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
            refreshPolicy.setDataPath("game");
            section.setRefreshPolicy(refreshPolicy);

            section.setSectionStates(
                    utils.buildSectionStates(sectionId, "Unable to load boxscore", "shimmer", 200));
        }

        BoxscoreTable data = new BoxscoreTable();
        data.setTeamTricode(teamTricode);
        data.setTeamName(teamCity + " " + teamName);
        data.setTeamColor(SduiUtils.getTeamPrimaryColor(teamTricode));
        data.setTeamLogoUrl(SduiUtils.teamLogoUrl(teamId));

        data.setColumns(utils.buildBoxscoreColumns());

        java.util.List<BoxscorePlayerRow> playerRows = new java.util.ArrayList<>();
        java.util.List<BoxscorePlayer> players = team != null && team.getPlayers() != null
                ? team.getPlayers() : java.util.List.of();

        for (BoxscorePlayer player : players) {
            BoxscorePlayerRow row = mapPlayerToBoxscoreRow(player, teamTricode);
            if (row != null) {
                playerRows.add(row);
            }
        }
        data.setPlayers(playerRows);

        if (team != null && team.getStatistics() != null) {
            data.setTeamTotals(mapTeamStatistics(team.getStatistics()));
        }

        data.setSortStateKey(sortColKey);
        data.setSortDirectionStateKey(sortDirKey);

        if (playerRows.isEmpty()) {
            data.setEmptyMessage("Box score will be available once the game begins.");
        }

        section.setData(data);
        return section;
    }

    /**
     * Build one {@link Subsection} per tab carrying an {@code onActivate→mutate}
     * action for tab selection. Mirrors the typed helper in GameDetailComposer.
     */
    private List<Subsection> tabSelectSubsections(List<TabData> tabs, String stateKey) {
        List<Subsection> subsections = new ArrayList<>(tabs.size());
        for (TabData tab : tabs) {
            String stateValue = tab.getStateValue() != null ? tab.getStateValue() : tab.getId();
            Action mutate = new Action();
            mutate.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
            mutate.setType(Action.ActionType.MUTATE);
            mutate.setTarget(stateKey);
            mutate.setValue(stateValue);
            Subsection sub = new Subsection();
            sub.setId(tab.getId());
            sub.setActions(List.of(mutate));
            subsections.add(sub);
        }
        return subsections;
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Map a single NBA API player node to a {@code BoxscorePlayerRow}.
     */
    private BoxscorePlayerRow mapPlayerToBoxscoreRow(BoxscorePlayer player, String teamTricode) {
        int personId = player.getPersonId() != null ? player.getPersonId() : 0;
        String played = player.getPlayed() != null ? player.getPlayed() : "0";
        String notPlayingReason = player.getNotPlayingReason() != null ? player.getNotPlayingReason() : "";

        String name = player.getNameI() != null ? player.getNameI() : "";
        if (name.isEmpty() && player.getName() != null) {
            name = player.getName();
        }
        if (name.isEmpty()) {
            String firstName = player.getFirstName() != null ? player.getFirstName() : "";
            String familyName = player.getFamilyName() != null ? player.getFamilyName() : "";
            name = firstName + " " + familyName;
        }

        BoxscorePlayerRow row = new BoxscorePlayerRow();
        row.setPlayerId(String.valueOf(personId));
        row.setName(name.trim());
        row.setPosition(player.getPosition() != null ? player.getPosition() : "");
        row.setJerseyNumber(player.getJerseyNum() != null ? player.getJerseyNum() : "");
        row.setImageUrl(
                "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png");
        row.setStarter("1".equals(player.getStarter() != null ? player.getStarter() : "0"));

        BoxscoreStatistics s = player.getStatistics();
        com.nba.sdui.models.generated.BoxscorePlayerStatistics stats =
                new com.nba.sdui.models.generated.BoxscorePlayerStatistics();

        String minutesText = s != null && s.getMinutes() != null ? s.getMinutes() : "";
        boolean didPlay = "1".equals(played) && !"PT0M00.00S".equals(minutesText);

        if (didPlay && s != null) {
            stats.setAdditionalProperty("min", SduiUtils.formatMinutes(minutesText));
            stats.setAdditionalProperty("pts", s.getPoints());
            stats.setAdditionalProperty("reb", s.getReboundsTotal());
            stats.setAdditionalProperty("ast", s.getAssists());
            stats.setAdditionalProperty("stl", s.getSteals());
            stats.setAdditionalProperty("blk", s.getBlocks());
            stats.setAdditionalProperty("to",  s.getTurnovers());
            stats.setAdditionalProperty("pf",  s.getFoulsPersonal());
            stats.setAdditionalProperty("fgm", s.getFieldGoalsMade());
            stats.setAdditionalProperty("fga", s.getFieldGoalsAttempted());
            stats.setAdditionalProperty("fgPct", SduiUtils.formatPct(s.getFieldGoalsPercentage()));
            stats.setAdditionalProperty("tpm", s.getThreePointersMade());
            stats.setAdditionalProperty("tpa", s.getThreePointersAttempted());
            stats.setAdditionalProperty("tpPct", SduiUtils.formatPct(s.getThreePointersPercentage()));
            stats.setAdditionalProperty("ftm", s.getFreeThrowsMade());
            stats.setAdditionalProperty("fta", s.getFreeThrowsAttempted());
            stats.setAdditionalProperty("ftPct", SduiUtils.formatPct(s.getFreeThrowsPercentage()));
            stats.setAdditionalProperty("oreb", s.getReboundsOffensive());
            stats.setAdditionalProperty("dreb", s.getReboundsDefensive());
            stats.setAdditionalProperty("pm",  (int) s.getPlusMinusPoints());
        } else {
            stats.setAdditionalProperty("min", notPlayingReason.isEmpty() ? "DNP" : notPlayingReason);
        }

        row.setStats(stats);


        return row;
    }

    /**
     * Map the team-level statistics node to a totals map aligned to boxscore columns.
     */
    private com.nba.sdui.models.generated.BoxscorePlayerStatistics mapTeamStatistics(BoxscoreStatistics s) {
        com.nba.sdui.models.generated.BoxscorePlayerStatistics totals =
                new com.nba.sdui.models.generated.BoxscorePlayerStatistics();
        if (s == null) return totals;
        totals.setAdditionalProperty("min", SduiUtils.formatMinutes(s.getMinutes() != null ? s.getMinutes() : ""));
        totals.setAdditionalProperty("pts", s.getPoints());
        totals.setAdditionalProperty("reb", s.getReboundsTotal());
        totals.setAdditionalProperty("ast", s.getAssists());
        totals.setAdditionalProperty("stl", s.getSteals());
        totals.setAdditionalProperty("blk", s.getBlocks());
        totals.setAdditionalProperty("to",  s.getTurnovers());
        totals.setAdditionalProperty("pf",  s.getFoulsPersonal());
        totals.setAdditionalProperty("fgm", s.getFieldGoalsMade());
        totals.setAdditionalProperty("fga", s.getFieldGoalsAttempted());
        totals.setAdditionalProperty("fgPct", SduiUtils.formatPct(s.getFieldGoalsPercentage()));
        totals.setAdditionalProperty("tpm", s.getThreePointersMade());
        totals.setAdditionalProperty("tpa", s.getThreePointersAttempted());
        totals.setAdditionalProperty("tpPct", SduiUtils.formatPct(s.getThreePointersPercentage()));
        totals.setAdditionalProperty("ftm", s.getFreeThrowsMade());
        totals.setAdditionalProperty("fta", s.getFreeThrowsAttempted());
        totals.setAdditionalProperty("ftPct", SduiUtils.formatPct(s.getFreeThrowsPercentage()));
        totals.setAdditionalProperty("oreb", s.getReboundsOffensive());
        totals.setAdditionalProperty("dreb", s.getReboundsDefensive());
        totals.setAdditionalProperty("pm",  (int) s.getPlusMinusPoints());
        return totals;
    }
}
