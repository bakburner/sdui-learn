/**
 * Framework-free boxscore upstream DTOs. Mirrors the public NBA CDN
 * {@code /boxscore/boxscore_<gameId>.json} payload. Lombok-only;
 * no Spring, no SAF dependencies. The classes here are cached as
 * {@link com.nba.sdui.integration.model.boxscore.BoxscoreResponse}
 * (top level) so SAF's
 * {@link com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator}
 * (allowlist {@code com.nba.*}) accepts them.
 */
package com.nba.sdui.integration.model.boxscore;
