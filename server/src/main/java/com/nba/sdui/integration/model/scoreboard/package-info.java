/**
 * Typed upstream DTOs for NBA scoreboard payloads (CDN
 * {@code todaysScoreboard_00.json} and the normalized core-api
 * {@code gameCardFeed} response).
 *
 * <p>Framework-free (no Spring, no SAF) so that when this package later
 * lifts into a shared {@code integration-models} Gradle module — mirroring
 * the {@code nba-client-backend} layout — no imports change.
 */
package com.nba.sdui.integration.model.scoreboard;
