package com.nba.sdui.core.renderer

/**
 * Universal client-side image fallback constants.
 *
 * The server MAY supply a per-section `fallbackThumbnailUrl`, but this
 * constant is the final safety net so every `AsyncImage` has a last-resort
 * error model.
 */
object SduiImageDefaults {

    /** NBA logoman — universal last-resort fallback for any broken image. */
    const val LOGOMAN_URL: String =
        "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
}
