package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public BoxscoreComposer(ObjectMapper objectMapper, StatsApiClient statsApiClient, SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
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

        JsonNode boxscore = statsApiClient.getBoxscore(gameId);
        JsonNode game = boxscore != null ? boxscore.path("game") : null;

        boolean hasLiveData = game != null && !game.isMissingNode() && game.has("homeTeam");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "boxscore-" + gameId);
        response.put("type", "boxscore");
        response.put("traceId", traceId);
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

        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        String homeTricode = homeTeam.path("teamTricode").asText("HOME");
        String awayTricode = awayTeam.path("teamTricode").asText("AWAY");
        int gameStatus = game.path("gameStatus").asInt(1);

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
        awayTable.set("surface", utils.flushSurface());
        ObjectNode homeTable = buildBoxscoreTableSection(
                homeTeam, gameId, contentSourceId, "home",
                "boxscore_home_sortCol", "boxscore_home_sortDir", gameStatus);
        homeTable.set("surface", utils.flushSurface());

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
        tabGroup.set("surface", utils.stripSurfaceWithoutBackground());

        sections.add(tabGroup);
        response.set("sections", sections);

        log.info("Boxscore screen composed: {} @ {}, gameStatus={}, awayPlayers={}, homePlayers={}",
                awayTricode, homeTricode, gameStatus,
                awayTeam.path("players").size(), homeTeam.path("players").size());

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
    ObjectNode buildBoxscoreTableSection(JsonNode team, String gameId,
                                          String contentSourceId, String slug,
                                          String sortColKey, String sortDirKey,
                                          int gameStatus) {
        String teamTricode = team.path("teamTricode").asText("");
        String teamName = team.path("teamName").asText("");
        String teamCity = team.path("teamCity").asText("");
        int teamId = team.path("teamId").asInt();

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
        ArrayNode players = team.has("players") ? (ArrayNode) team.get("players") : objectMapper.createArrayNode();

        for (JsonNode player : players) {
            ObjectNode row = mapPlayerToBoxscoreRow(player, teamTricode);
            if (row != null) {
                playerRows.add(row);
            }
        }
        data.set("players", playerRows);

        if (team.has("statistics")) {
            data.set("teamTotals", mapTeamStatistics(team.path("statistics")));
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
    private ObjectNode mapPlayerToBoxscoreRow(JsonNode player, String teamTricode) {
        int personId = player.path("personId").asInt();
        String played = player.path("played").asText("0");
        String notPlayingReason = player.path("notPlayingReason").asText("");

        String name = player.path("nameI").asText("");
        if (name.isEmpty()) {
            name = player.path("name").asText("");
        }
        if (name.isEmpty()) {
            name = player.path("firstName").asText("") + " " + player.path("familyName").asText("");
        }

        ObjectNode row = objectMapper.createObjectNode();
        row.put("playerId", String.valueOf(personId));
        row.put("name", name.trim());
        row.put("position", player.path("position").asText(""));
        row.put("jerseyNumber", player.path("jerseyNum").asText(""));
        row.put("imageUrl",
                "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png");
        row.put("starter", "1".equals(player.path("starter").asText("0")));

        JsonNode s = player.path("statistics");
        ObjectNode stats = objectMapper.createObjectNode();

        boolean didPlay = "1".equals(played) && !"PT0M00.00S".equals(s.path("minutes").asText(""));

        if (didPlay) {
            stats.put("min", SduiUtils.formatMinutes(s.path("minutes").asText("")));
            stats.put("pts", s.path("points").asInt());
            stats.put("reb", s.path("reboundsTotal").asInt());
            stats.put("ast", s.path("assists").asInt());
            stats.put("stl", s.path("steals").asInt());
            stats.put("blk", s.path("blocks").asInt());
            stats.put("to",  s.path("turnovers").asInt());
            stats.put("pf",  s.path("foulsPersonal").asInt());
            stats.put("fgm", s.path("fieldGoalsMade").asInt());
            stats.put("fga", s.path("fieldGoalsAttempted").asInt());
            stats.put("fgPct", SduiUtils.formatPct(s.path("fieldGoalsPercentage").asDouble()));
            stats.put("tpm", s.path("threePointersMade").asInt());
            stats.put("tpa", s.path("threePointersAttempted").asInt());
            stats.put("tpPct", SduiUtils.formatPct(s.path("threePointersPercentage").asDouble()));
            stats.put("ftm", s.path("freeThrowsMade").asInt());
            stats.put("fta", s.path("freeThrowsAttempted").asInt());
            stats.put("ftPct", SduiUtils.formatPct(s.path("freeThrowsPercentage").asDouble()));
            stats.put("oreb", s.path("reboundsOffensive").asInt());
            stats.put("dreb", s.path("reboundsDefensive").asInt());
            stats.put("pm",  (int) s.path("plusMinusPoints").asDouble());
        } else {
            stats.put("min", notPlayingReason.isEmpty() ? "DNP" : notPlayingReason);
        }

        row.set("stats", stats);


        return row;
    }

    /**
     * Map the team-level statistics node to a totals map aligned to boxscore columns.
     */
    private ObjectNode mapTeamStatistics(JsonNode s) {
        ObjectNode totals = objectMapper.createObjectNode();
        totals.put("min", SduiUtils.formatMinutes(s.path("minutes").asText("")));
        totals.put("pts", s.path("points").asInt());
        totals.put("reb", s.path("reboundsTotal").asInt());
        totals.put("ast", s.path("assists").asInt());
        totals.put("stl", s.path("steals").asInt());
        totals.put("blk", s.path("blocks").asInt());
        totals.put("to",  s.path("turnovers").asInt());
        totals.put("pf",  s.path("foulsPersonal").asInt());
        totals.put("fgm", s.path("fieldGoalsMade").asInt());
        totals.put("fga", s.path("fieldGoalsAttempted").asInt());
        totals.put("fgPct", SduiUtils.formatPct(s.path("fieldGoalsPercentage").asDouble()));
        totals.put("tpm", s.path("threePointersMade").asInt());
        totals.put("tpa", s.path("threePointersAttempted").asInt());
        totals.put("tpPct", SduiUtils.formatPct(s.path("threePointersPercentage").asDouble()));
        totals.put("ftm", s.path("freeThrowsMade").asInt());
        totals.put("fta", s.path("freeThrowsAttempted").asInt());
        totals.put("ftPct", SduiUtils.formatPct(s.path("freeThrowsPercentage").asDouble()));
        totals.put("oreb", s.path("reboundsOffensive").asInt());
        totals.put("dreb", s.path("reboundsDefensive").asInt());
        totals.put("pm",  (int) s.path("plusMinusPoints").asDouble());
        return totals;
    }
}
