package com.nba.sdui.core.network

/**
 * Resolves server-emitted relative asset paths (e.g. `/sdui-demo/card-wide.svg`)
 * against the SDUI composition service [baseUrl] used by [com.nba.sdui.core.data.SduiRepository].
 *
 * Web serves the same paths same-origin via the dev proxy; Android/iOS must
 * absolutize before handing URLs to Coil / Kingfisher.
 */
object WireUrlResolver {
    fun resolve(url: String?, baseUrl: String?): String? {
        if (url.isNullOrBlank()) return url
        val trimmed = url.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val base = baseUrl?.trim()?.trimEnd('/') ?: return trimmed
        val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return base + path
    }
}
