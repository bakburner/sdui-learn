package com.nba.sdui.core.request

import android.content.res.Configuration
import android.os.Build
import android.util.Log
import java.net.URLEncoder
import java.util.UUID

/**
 * Builds the SDUI request envelope as bracket-notation query parameters.
 *
 * All composition context travels as query params per plan-request-transport.md D1/D3.
 * If the resulting query string exceeds [MAX_QUERY_LENGTH], the envelope is available
 * as a JSON body for POST fallback.
 *
 * Example output:
 * ```
 * locale=en&schemaVersion=1.0&platform[deviceClass]=phone&platform[capabilities][sse]=true
 * &market[cohort]=US_NY_METRO&experiments[gd_tab_order_v2]=variant_b
 * ```
 */
class RequestEnvelopeBuilder {

    companion object {
        private const val MAX_QUERY_LENGTH = 8192

        /**
         * RFC-3986 percent-encoding, matching the iOS `RequestEnvelope.percentEncode`
         * and the web `encodeURIComponent`-based encoder so the same input produces
         * byte-identical query strings on every platform. Spaces become `%20`
         * (not `+`), and the unreserved set (`A-Z a-z 0-9 - _ . ~`) survives raw.
         *
         * `java.net.URLEncoder.encode(..., "UTF-8")` is form-urlencoding, not
         * RFC-3986, and emits `+` for spaces — using it would silently desync
         * the Android wire format from iOS/web, breaking parity tests and
         * CDN cache keys whenever any value contained whitespace.
         */
        @JvmStatic
        fun percentEncode(value: String): String {
            return URLEncoder.encode(value, Charsets.UTF_8.name())
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~")
        }

        /**
         * Best-effort form factor for layout tokens and the request envelope.
         * Uses [Configuration] from the system resources (no Context required).
         */
        @JvmStatic
        fun defaultFormFactor(): String {
            return try {
                val res = android.content.res.Resources.getSystem()
                val sw = res.configuration.smallestScreenWidthDp
                val land = res.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                when {
                    sw >= 600 -> "tablet"
                    land && sw < 600 -> "phone.landscape"
                    else -> "phone"
                }
            } catch (e: Exception) {
                Log.d("RequestEnvelopeBuilder", "defaultFormFactor fallback: ${e.message}")
                "phone"
            }
        }

        /**
         * Best-effort theme from system UI mode. Returns "dark" or "light".
         */
        @JvmStatic
        fun defaultTheme(): String {
            return try {
                val res = android.content.res.Resources.getSystem()
                val nightMode = res.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                if (nightMode == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
            } catch (e: Exception) {
                "light"
            }
        }
    }

    // ── Top-level params ───────────────────────────────────────────────
    private var locale: String = "en"
    private var schemaVersion: String = "1.0"

    // ── Platform ───────────────────────────────────────────────────────
    private var platformName: String = "android"
    private var appVersion: String? = null
    private var osVersion: String = Build.VERSION.SDK_INT.toString()
    private var deviceClass: String = "phone"
    // TODO(platform-tier): replace per-boolean capability flags with a single
    // server-defined platform tier string (e.g. "tier:full") to reduce CDN
    // cache-key fragmentation. Tier resolution at edge or in client.
    private var sseCapable: Boolean = true
    private var onFocusCapable: Boolean = false

    // ── Device ─────────────────────────────────────────────────────────
    private var deviceId: String? = null

    // ── Market ─────────────────────────────────────────────────────────
    private var marketCohort: String = "MARKET_UNKNOWN"

    // ── Experiments (Amplitude assignments) ─────────────────────────────
    private val experiments: MutableMap<String, String> = mutableMapOf()

    // ── Builder methods ────────────────────────────────────────────────

    fun locale(locale: String) = apply { this.locale = locale }
    fun schemaVersion(version: String) = apply { this.schemaVersion = version }

    fun platformName(name: String) = apply { this.platformName = name }
    fun appVersion(version: String) = apply { this.appVersion = version }
    fun osVersion(version: String) = apply { this.osVersion = version }
    fun deviceClass(cls: String) = apply { this.deviceClass = cls }
    fun sseCapable(capable: Boolean) = apply { this.sseCapable = capable }
    fun onFocusCapable(capable: Boolean) = apply { this.onFocusCapable = capable }

    fun deviceId(id: String?) = apply { this.deviceId = id }

    fun marketCohort(cohort: String) = apply { this.marketCohort = cohort }

    fun experiment(id: String, variant: String) = apply { this.experiments[id] = variant }
    fun experiments(map: Map<String, String>) = apply { this.experiments.putAll(map) }

    /**
     * Build the query string in bracket notation.
     * Does NOT include the leading '?' — caller prepends that.
     */
    fun buildQueryString(): String {
        val params = mutableListOf<Pair<String, String>>()

        // Fixed envelope ordering: locale, schemaVersion, platform, market, experiments.
        // All platforms must emit in this order for byte-identical CDN cache keys.

        // Top-level scalars
        params.add("locale" to locale)
        params.add("schemaVersion" to schemaVersion)

        // Platform (nested)
        params.add("platform[deviceClass]" to deviceClass)
        params.add("platform[capabilities][sse]" to sseCapable.toString())
        if (onFocusCapable) {
            params.add("platform[capabilities][onFocus]" to "true")
        }

        // Market
        params.add("market[cohort]" to marketCohort)

        // Experiments (nested map, sorted for deterministic CDN cache keys)
        for ((id, variant) in experiments.toSortedMap()) {
            params.add("experiments[$id]" to variant)
        }

        return params.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    /**
     * Whether the query string exceeds the safe GET threshold.
     * Caller should switch to POST if true.
     */
    fun exceedsGetThreshold(): Boolean = buildQueryString().length > MAX_QUERY_LENGTH

    /**
     * Build the envelope as a JSON string for POST fallback.
     *
     * Shape matches the server's `SduiRequestContext` field layout and the
     * iOS `RequestEnvelope.jsonBody()` / web `buildJsonBody()` output so the
     * same `BracketParamResolver` POST path deserializes it identically on
     * every platform.
     */
    fun buildJsonBody(): String {
        val platform = mutableMapOf<String, Any>(
            "deviceClass" to deviceClass
        )
        val capabilities = mutableMapOf<String, Any>("sse" to sseCapable)
        if (onFocusCapable) capabilities["onFocus"] = true
        platform["capabilities"] = capabilities

        val body = mutableMapOf<String, Any>(
            "locale" to locale,
            "schemaVersion" to schemaVersion,
            "platform" to platform,
            "market" to mapOf("cohort" to marketCohort),
            "experiments" to experiments
        )

        return org.json.JSONObject(body).toString()
    }

    /**
     * Generate a trace ID for this request.
     */
    fun generateTraceId(): String = "trace-${UUID.randomUUID().toString().substring(0, 8)}"

    /** Returns the deviceId for use as the X-Device-Id header value. */
    fun getDeviceId(): String? = deviceId

    fun getPlatformName(): String = platformName

    fun getAppVersion(): String? = appVersion

    fun getOsVersion(): String = osVersion

    private fun encode(value: String): String = percentEncode(value)
}
