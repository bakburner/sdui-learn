package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    private final ObjectMapper objectMapper;
    private final StatsPort statsPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public BoxscoreComposer(ObjectMapper objectMapper, StatsPort statsPort, SduiUtils utils, SectionSurfaces surfaces) {
        this.objectMapper = objectMapper;
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
    public JsonNode composeBoxscore(String gameId, String traceId, String locale) throws IOException {
        log.info("Composing boxscore screen: gameId={}, locale={}", gameId, locale);

        String contentSourceId = "stats-api:game-" + gameId;

        BoxscoreResponse boxscore = statsPort.getBoxscore(gameId);
        BoxscoreGame game = boxscore != null ? boxscore.getGame() : null;

        boolean hasLiveData = game != null && game.getHomeTeam() != null;

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "boxscore-" + gameId);
        response.put("type", "boxscore");
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", utils.buildNavigation("game-detail"));

        if (!hasLiveData) {
            log.warn("No boxscore data available for gameId={}, returning empty screen", gameId);
            response.set("state", objectMapper.createObjectNode());
            ArrayNode sections = objectMapper.createArrayNode();
            ObjectNode emptySection = objectMapper.createObjectNode();
            emptySection.put("id", SectionIdDeriver.derive(contentSourceId, "BoxscoreTable", "empty"));
            emptySection.put("contentSourceId", contentSourceId);
            emptySection.put("type", "BoxscoreTable");
            ObjectNode emptyData = objectMapper.createObjectNode();
            emptyData.put("teamTricode", "");
            emptyData.put("teamName", "");
            emptyData.set("columns", utils.buildBoxscoreColumns());
            emptyData.set("players", objectMapper.createArrayNode());
            emptyData.put("emptyMessage", "Box score will be available once the game begins.");
            emptySection.set("data", emptyData);
            sections.add(emptySection);
            response.set("sections", sections);
            response.put("title", "Box Score");
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
        ObjectNode state = objectMapper.createObjectNode();
        state.put("boxscore_team", awayTricode);
        state.put("boxscore_away_sortCol", "points");
        state.put("boxscore_away_sortDir", "desc");
        state.put("boxscore_home_sortCol", "points");
        state.put("boxscore_home_sortDir", "desc");
        response.set("state", state);

        // ── Sections ───────────────────────────────────────────────────
        ArrayNode sections = objectMapper.createArrayNode();

        ObjectNode awayTable = buildBoxscoreTableSection(
                awayTeam, gameId, contentSourceId, "away",
                "boxscore_away_sortCol", "boxscore_away_sortDir", gameStatus);
        awayTable.set("surface", surfaces.flushSurface());
        ObjectNode homeTable = buildBoxscoreTableSection(
                homeTeam, gameId, contentSourceId, "home",
                "boxscore_home_sortCol", "boxscore_home_sortDir", gameStatus);
        homeTable.set("surface", surfaces.flushSurface());

        // Wrap in TabGroup for team toggling
        ObjectNode tabGroup = objectMapper.createObjectNode();
        tabGroup.put("id", SectionIdDeriver.derive(contentSourceId, "TabGroup", "team-tabs"));
        tabGroup.put("contentSourceId", contentSourceId);
        tabGroup.put("type", "TabGroup");
        tabGroup.put("analyticsId", "boxscore_team_toggle");

        ObjectNode tabData = objectMapper.createObjectNode();
        tabData.put("stateKey", "boxscore_team");
        tabData.put("defaultTab", awayTricode);

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode awayTab = objectMapper.createObjectNode();
        awayTab.put("id", "tab-" + awayTricode.toLowerCase());
        awayTab.put("label", awayTricode);
        awayTab.put("stateKey", "boxscore_team");
        awayTab.put("stateValue", awayTricode);
        tabs.add(awayTab);

        ObjectNode homeTab = objectMapper.createObjectNode();
        homeTab.put("id", "tab-" + homeTricode.toLowerCase());
        homeTab.put("label", homeTricode);
        homeTab.put("stateKey", "boxscore_team");
        homeTab.put("stateValue", homeTricode);
        tabs.add(homeTab);

        tabData.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();
        ArrayNode awayContent = objectMapper.createArrayNode();
        awayContent.add(awayTable);
        tabContents.set(awayTricode, awayContent);

        ArrayNode homeContent = objectMapper.createArrayNode();
        homeContent.add(homeTable);
        tabContents.set(homeTricode, homeContent);

        tabData.set("tabContents", tabContents);
        tabGroup.set("data", tabData);
        tabGroup.set("subsections", utils.tabSelectSubsections(tabs, "boxscore_team"));
        tabGroup.set("surface", surfaces.stripSurfaceWithoutBackground());

        sections.add(tabGroup);
        response.set("sections", sections);

        log.info("Boxscore screen composed: {} @ {}, gameStatus={}, awayPlayers={}, homePlayers={}",
                awayTricode, homeTricode, gameStatus,
                awayTeam != null && awayTeam.getPlayers() != null ? awayTeam.getPlayers().size() : 0,
                homeTeam != null && homeTeam.getPlayers() != null ? homeTeam.getPlayers().size() : 0);

        response.put("title", awayTricode + " @ " + homeTricode);
        utils.prependAppBarHeaderIfNeeded(response);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── BoxscoreTable section builder (package-private for GameDetailComposer) ──

    /**
     * Build a single {@code BoxscoreTable} section for one team.
     */
    public ObjectNode buildBoxscoreTableSection(BoxscoreTeam team, String gameId,
                                          String contentSourceId, String slug,
                                          String sortColKey, String sortDirKey,
                                          int gameStatus) {
        String teamTricode = team != null && team.getTeamTricode() != null ? team.getTeamTricode() : "";
        String teamName = team != null && team.getTeamName() != null ? team.getTeamName() : "";
        String teamCity = team != null && team.getTeamCity() != null ? team.getTeamCity() : "";
        int teamId = team != null && team.getTeamId() != null ? team.getTeamId() : 0;

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "BoxscoreTable", slug));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "BoxscoreTable");
        section.put("analyticsId", "boxscore_table_" + teamTricode.toLowerCase());

        if (gameStatus == 2) {
            ObjectNode refreshPolicy = objectMapper.createObjectNode();
            refreshPolicy.put("type", "poll");
            refreshPolicy.put("intervalMs", 30000);
            refreshPolicy.put("url",
                    "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
            refreshPolicy.put("dataPath", "game");
            section.set("refreshPolicy", refreshPolicy);

            section.set("sectionStates", utils.buildSectionStates(
                    SectionIdDeriver.derive(contentSourceId, "BoxscoreTable", slug),
                    "Unable to load boxscore", "shimmer", 200));
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("teamTricode", teamTricode);
        data.put("teamName", teamCity + " " + teamName);
        data.put("teamColor", SduiUtils.getTeamPrimaryColor(teamTricode));
        data.put("teamLogoUrl", SduiUtils.teamLogoUrl(teamId));

        data.set("columns", utils.buildBoxscoreColumns());

        ArrayNode playerRows = objectMapper.createArrayNode();
        java.util.List<BoxscorePlayer> players = team != null && team.getPlayers() != null
                ? team.getPlayers() : java.util.List.of();

        for (BoxscorePlayer player : players) {
            ObjectNode row = mapPlayerToBoxscoreRow(player, teamTricode);
            if (row != null) {
                playerRows.add(row);
            }
        }
        data.set("players", playerRows);

        if (team != null && team.getStatistics() != null) {
            data.set("teamTotals", mapTeamStatistics(team.getStatistics()));
        }

        data.put("sortStateKey", sortColKey);
        data.put("sortDirectionStateKey", sortDirKey);

        if (playerRows.isEmpty()) {
            data.put("emptyMessage", "Box score will be available once the game begins.");
        }

        section.set("data", data);
        return section;
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Map a single NBA API player node to a {@code BoxscorePlayerRow}.
     */
    private ObjectNode mapPlayerToBoxscoreRow(BoxscorePlayer player, String teamTricode) {
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

        ObjectNode row = objectMapper.createObjectNode();
        row.put("playerId", String.valueOf(personId));
        row.put("name", name.trim());
        row.put("position", player.getPosition() != null ? player.getPosition() : "");
        row.put("jerseyNumber", player.getJerseyNum() != null ? player.getJerseyNum() : "");
        row.put("imageUrl",
                "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png");
        row.put("starter", "1".equals(player.getStarter() != null ? player.getStarter() : "0"));

        BoxscoreStatistics s = player.getStatistics();
        ObjectNode stats = objectMapper.createObjectNode();

        String minutesText = s != null && s.getMinutes() != null ? s.getMinutes() : "";
        boolean didPlay = "1".equals(played) && !"PT0M00.00S".equals(minutesText);

        if (didPlay && s != null) {
            stats.put("min", SduiUtils.formatMinutes(minutesText));
            stats.put("pts", s.getPoints());
            stats.put("reb", s.getReboundsTotal());
            stats.put("ast", s.getAssists());
            stats.put("stl", s.getSteals());
            stats.put("blk", s.getBlocks());
            stats.put("to",  s.getTurnovers());
            stats.put("pf",  s.getFoulsPersonal());
            stats.put("fgm", s.getFieldGoalsMade());
            stats.put("fga", s.getFieldGoalsAttempted());
            stats.put("fgPct", SduiUtils.formatPct(s.getFieldGoalsPercentage()));
            stats.put("tpm", s.getThreePointersMade());
            stats.put("tpa", s.getThreePointersAttempted());
            stats.put("tpPct", SduiUtils.formatPct(s.getThreePointersPercentage()));
            stats.put("ftm", s.getFreeThrowsMade());
            stats.put("fta", s.getFreeThrowsAttempted());
            stats.put("ftPct", SduiUtils.formatPct(s.getFreeThrowsPercentage()));
            stats.put("oreb", s.getReboundsOffensive());
            stats.put("dreb", s.getReboundsDefensive());
            stats.put("pm",  (int) s.getPlusMinusPoints());
        } else {
            stats.put("min", notPlayingReason.isEmpty() ? "DNP" : notPlayingReason);
        }

        row.set("stats", stats);


        return row;
    }

    /**
     * Map the team-level statistics node to a totals map aligned to boxscore columns.
     */
    private ObjectNode mapTeamStatistics(BoxscoreStatistics s) {
        ObjectNode totals = objectMapper.createObjectNode();
        if (s == null) return totals;
        totals.put("min", SduiUtils.formatMinutes(s.getMinutes() != null ? s.getMinutes() : ""));
        totals.put("pts", s.getPoints());
        totals.put("reb", s.getReboundsTotal());
        totals.put("ast", s.getAssists());
        totals.put("stl", s.getSteals());
        totals.put("blk", s.getBlocks());
        totals.put("to",  s.getTurnovers());
        totals.put("pf",  s.getFoulsPersonal());
        totals.put("fgm", s.getFieldGoalsMade());
        totals.put("fga", s.getFieldGoalsAttempted());
        totals.put("fgPct", SduiUtils.formatPct(s.getFieldGoalsPercentage()));
        totals.put("tpm", s.getThreePointersMade());
        totals.put("tpa", s.getThreePointersAttempted());
        totals.put("tpPct", SduiUtils.formatPct(s.getThreePointersPercentage()));
        totals.put("ftm", s.getFreeThrowsMade());
        totals.put("fta", s.getFreeThrowsAttempted());
        totals.put("ftPct", SduiUtils.formatPct(s.getFreeThrowsPercentage()));
        totals.put("oreb", s.getReboundsOffensive());
        totals.put("dreb", s.getReboundsDefensive());
        totals.put("pm",  (int) s.getPlusMinusPoints());
        return totals;
    }
}
