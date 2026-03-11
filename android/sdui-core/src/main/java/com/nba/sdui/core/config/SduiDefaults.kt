package com.nba.sdui.core.config

/**
 * Library-wide defaults for the SDUI renderer.
 *
 * Centralises magic values so every renderer shares a single source of truth.
 * The server MAY still supply a per-section `fallbackThumbnailUrl` override,
 * but this constant is the universal, client-side safety-net.
 */
object SduiDefaults {
    /** Universal fallback image shown when a section thumbnail fails to load. */
    const val FALLBACK_IMAGE_URL =
        "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
}
