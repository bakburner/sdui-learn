/**
 * Implementations of the typed upstream client interfaces declared in
 * {@link com.nba.sdui.integration.client}.
 *
 * <p>Today the implementations bridge to the legacy {@code StatsApiClient}
 * raw-JSON fetcher and convert at the boundary. As more upstream surfaces
 * migrate, the per-source clients will absorb the HTTP layer directly,
 * matching the {@code nba-client-backend/integration-clients/.../impl}
 * pattern.
 */
package com.nba.sdui.integration.client.impl;
