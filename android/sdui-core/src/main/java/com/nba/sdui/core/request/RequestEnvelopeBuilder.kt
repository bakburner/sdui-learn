package com.nba.sdui.core.request

import android.os.Build
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
 * locale=en&schemaVersion=1.0&platform[name]=android&platform[appVersion]=8.3.0
 * &platform[osVersion]=14&platform[deviceClass]=phone&platform[capabilities][sse]=true
 * &device[countryCode]=US&experiments[gd_tab_order_v2]=variant_b
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
    }

    // ── Top-level params ───────────────────────────────────────────────
    private var locale: String = "en"
    private var schemaVersion: String = "1.0"
    private var gameState: String? = null

    // ── Platform ───────────────────────────────────────────────────────
    private var platformName: String = "android"
    private var appVersion: String? = null
    private var osVersion: String = Build.VERSION.SDK_INT.toString()
    private var deviceClass: String = "phone"
    private var sseCapable: Boolean = true
    private var onFocusCapable: Boolean = false

    // ── Device ─────────────────────────────────────────────────────────
    private var deviceId: String? = null
    private var zipCode: String? = null
    private var countryCode: String? = null
    private var region: String? = null

    // ── Experiments (Amplitude assignments) ─────────────────────────────
    private val experiments: MutableMap<String, String> = mutableMapOf()

    // ── Builder methods ────────────────────────────────────────────────

    fun locale(locale: String) = apply { this.locale = locale }
    fun schemaVersion(version: String) = apply { this.schemaVersion = version }
    fun gameState(state: String?) = apply { this.gameState = state }

    fun platformName(name: String) = apply { this.platformName = name }
    fun appVersion(version: String) = apply { this.appVersion = version }
    fun osVersion(version: String) = apply { this.osVersion = version }
    fun deviceClass(cls: String) = apply { this.deviceClass = cls }
    fun sseCapable(capable: Boolean) = apply { this.sseCapable = capable }
    fun onFocusCapable(capable: Boolean) = apply { this.onFocusCapable = capable }

    fun deviceId(id: String?) = apply { this.deviceId = id }
    fun zipCode(zip: String?) = apply { this.zipCode = zip }
    fun countryCode(code: String?) = apply { this.countryCode = code }
    fun region(region: String?) = apply { this.region = region }

    fun experiment(id: String, variant: String) = apply { this.experiments[id] = variant }
    fun experiments(map: Map<String, String>) = apply { this.experiments.putAll(map) }

    /**
     * Build the query string in bracket notation.
     * Does NOT include the leading '?' — caller prepends that.
     */
    fun buildQueryString(): String {
        val params = mutableListOf<Pair<String, String>>()

        // Top-level scalars
        params.add("locale" to locale)
        params.add("schemaVersion" to schemaVersion)
        gameState?.let { params.add("gameState" to it) }

        // Platform (nested)
        params.add("platform[name]" to platformName)
        appVersion?.let { params.add("platform[appVersion]" to it) }
        params.add("platform[osVersion]" to osVersion)
        params.add("platform[deviceClass]" to deviceClass)
        params.add("platform[capabilities][sse]" to sseCapable.toString())
        if (onFocusCapable) {
            params.add("platform[capabilities][onFocus]" to "true")
        }

        // Device (nested, all optional)
        deviceId?.let { params.add("device[deviceId]" to it) }
        zipCode?.let { params.add("device[zipCode]" to it) }
        countryCode?.let { params.add("device[countryCode]" to it) }
        region?.let { params.add("device[region]" to it) }

        // Experiments (nested map)
        for ((id, variant) in experiments) {
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
     * Generate a trace ID for this request.
     */
    fun generateTraceId(): String = "trace-${UUID.randomUUID().toString().substring(0, 8)}"

    private fun encode(value: String): String = percentEncode(value)
}
