package com.nba.sdui.controller;

import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import com.nba.sdui.integration.model.scoreboard.ScoreboardTeam;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for game data endpoints.
 *
 * Provides simplified game data for client consumption.
 */
@RestController
@RequestMapping("/v1/api/games")
public class GamesController {

    private static final Logger log = LoggerFactory.getLogger(GamesController.class);

    private final ScoreboardPort scoreboardPort;

    public GamesController(ScoreboardPort scoreboardPort) {
        this.scoreboardPort = scoreboardPort;
    }

    /** Compact per-game entry returned by {@link #getTodaysGames()}. */
    public record GameItem(String gameId, String label, String status) {}

    /**
     * Get today's games in a simplified format for dropdown selection.
     *
     * Only returns games that are in progress (gameStatus=2) or completed (gameStatus=3).
     * Games in pregame state (gameStatus=1) are excluded as they don't have boxscore data.
     *
     * Returns array of: { gameId, label, status }
     * where label is "{awayTeam} @ {homeTeam}" format
     */
    @GetMapping(value = "/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GameItem>> getTodaysGames() {
        log.info("Fetching today's games for selection");

        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();

            if (scoreboard == null || scoreboard.getGames().isEmpty()) {
                log.warn("No scoreboard data available");
                return ResponseEntity.ok(List.of());
            }

            List<GameItem> result = new ArrayList<>();

            for (Game game : scoreboard.getGames()) {
                // Only include in-progress (2) or completed (3) games
                int gameStatus = game.getGameStatus();
                if (gameStatus < 2) {
                    continue;
                }

                String gameId = game.getGameId() != null ? game.getGameId() : "";
                ScoreboardTeam away = game.getAwayTeam();
                ScoreboardTeam home = game.getHomeTeam();
                String awayTricode = away != null && away.getTeamTricode() != null ? away.getTeamTricode() : "";
                String homeTricode = home != null && home.getTeamTricode() != null ? home.getTeamTricode() : "";
                String status = game.getGameStatusText() != null ? game.getGameStatusText() : "";

                result.add(new GameItem(gameId, awayTricode + " @ " + homeTricode, status));
            }

            log.info("Returning {} games (filtered to in-progress/completed)", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching today's games", e);
            return ResponseEntity.ok(List.of());
        }
    }
}
