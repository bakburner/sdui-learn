package com.nba.sdui.core.data

import android.util.Log
import com.nba.sdui.core.models.generated.SduiModels
import com.nba.sdui.core.models.generated.mapper
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * SDUI Repository - Fetches SDUI responses from the composition service.
 *
 * All screen fetches route through [fetchScreen].  There are NO endpoint-specific
 * helpers — the server path is resolved upstream (by resolveEndpoint or the
 * server's navigation payload) and passed here as-is.
 *
 * Request context (platform, locale, device, experiments) is sent as
 * bracket-notation query parameters per plan-request-transport.md D1.
 * If the query string exceeds 8192 chars, falls back to POST with JSON body.
 */
class SduiRepository(
    private val baseUrl: String,
    private val httpClient: OkHttpClient = sharedHttpClient,
    private val authorizationToken: String? = null
) {
    companion object {
        private const val TAG = "SduiRepository"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Shared OkHttpClient — connection pool & thread pool reused across screens. */
        val sharedHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        }

        /** Backwards-compat alias for callers that referenced the old name. */
        @Deprecated("Use sharedHttpClient", ReplaceWith("sharedHttpClient"))
        val httpClient: OkHttpClient
            get() = sharedHttpClient
    }

    /**
     * Fetch any SDUI screen by its resolved server path.
     *
     * @param path        Path relative to baseUrl, e.g. "/sdui/game-detail/00423"
     * @param envelope    Request envelope builder with platform/device/experiment context
     * @param userParams  Optional user-supplied filter params (e.g. Form submit
     *                    bindings like `season=2025-26`). Always travel in the
     *                    URL query string regardless of GET vs POST so the
     *                    server reads them through the same `@RequestParam`
     *                    path on either side. They participate in the
     *                    GET/POST length decision.
     * @param traceIdOverride  Optional trace ID to reuse from a parent fetch
     *                    (e.g. parameterized refresh inheriting its screen's
     *                    trace). Falls back to `envelope.generateTraceId()`
     *                    when null.
     */
    suspend fun fetchScreen(
        path: String,
        envelope: RequestEnvelopeBuilder,
        userParams: Map<String, String> = emptyMap(),
        traceIdOverride: String? = null
    ): SduiModels = withContext(Dispatchers.IO) {
        val traceId = traceIdOverride ?: envelope.generateTraceId()
        val envelopeQuery = envelope.buildQueryString()
        val userQuery = encodeUserParams(userParams)
        // Threshold includes both halves so large userParams trigger POST too.
        val combinedLength = envelopeQuery.length + (if (userQuery.isEmpty()) 0 else userQuery.length + 1)

        val request = if (combinedLength > 8192) {
            // POST fallback for oversized envelopes. The envelope travels as
            // a JSON body (same shape as SduiRequestContext) so the server's
            // BracketParamResolver deserializes it identically to the
            // iOS / web POST paths. User params still ride the URL so the
            // server's @RequestParam handler reads them the same way as on GET.
            val url = if (userQuery.isEmpty()) "$baseUrl$path" else "$baseUrl$path?$userQuery"
            Log.d(TAG, "Fetching screen (POST fallback): $url")
            Request.Builder()
                .url(url)
                .header("X-Trace-Id", traceId)
                .header("X-Request-Id", UUID.randomUUID().toString())
                .apply { envelope.getDeviceId()?.let { header("X-Device-Id", it) } }
                .header("X-Platform", envelope.getPlatformName())
                .apply { envelope.getAppVersion()?.let { header("X-App-Version", it) } }
                .header("X-OS-Version", envelope.getOsVersion())
                // TODO(edge): placeholder — edge worker will set these from client IP
                .header("X-Resolved-Country", "US")
                .header("X-Resolved-Market-Cohort", "MARKET_UNKNOWN")
                .apply { authorizationToken?.let { header("Authorization", "Bearer $it") } }
                .post(envelope.buildJsonBody().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } else {
            // Standard GET with bracket-notation query params + user params.
            val combined = if (userQuery.isEmpty()) envelopeQuery else "$userQuery&$envelopeQuery"
            val separator = if (path.contains("?")) "&" else "?"
            val url = "$baseUrl$path${separator}$combined"
            Log.d(TAG, "Fetching screen: $url")
            Request.Builder()
                .url(url)
                .header("X-Trace-Id", traceId)
                .header("X-Request-Id", UUID.randomUUID().toString())
                .apply { envelope.getDeviceId()?.let { header("X-Device-Id", it) } }
                .header("X-Platform", envelope.getPlatformName())
                .apply { envelope.getAppVersion()?.let { header("X-App-Version", it) } }
                .header("X-OS-Version", envelope.getOsVersion())
                // TODO(edge): placeholder — edge worker will set these from client IP
                .header("X-Resolved-Country", "US")
                .header("X-Resolved-Market-Cohort", "MARKET_UNKNOWN")
                .apply { authorizationToken?.let { header("Authorization", "Bearer $it") } }
                .build()
        }

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch screen: ${response.code}")
        }

        // Detect server-signaled schema version mismatch (upgrade-required)
        val versionMismatch = response.header("X-Schema-Version-Mismatch")
        if (versionMismatch == "upgrade-required") {
            Log.w(TAG, "Server signaled schema version mismatch: upgrade-required")
            throw SchemaVersionMismatchException(
                "Client schema version is no longer supported. Please update the app."
            )
        }

        val body = response.body?.string()
            ?: throw SduiException("Empty response body")

        try {
            mapper.readValue(body, SduiModels::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse screen response", e)
            throw SduiException("Failed to parse screen response: ${e.message}")
        }
    }

    /**
     * Percent-encode user params with the same RFC-3986 rule the envelope
     * uses, sorted by key so the encoded URL is stable across calls (matters
     * for parity tests and CDN cache keys).
     */
    private fun encodeUserParams(params: Map<String, String>): String {
        if (params.isEmpty()) return ""
        return params.entries
            .filter { it.value.isNotEmpty() }
            .sortedBy { it.key }
            .joinToString("&") { (k, v) ->
                "${RequestEnvelopeBuilder.percentEncode(k)}=${RequestEnvelopeBuilder.percentEncode(v)}"
            }
    }

    /**
     * Fetch raw JSON from any URL (for direct data polling).
     *
     * @param url The full URL to fetch from
     * @param dataPath Optional JSONPath-like path to extract data (e.g., "game" or "sections[0].data")
     * @param traceId Optional trace ID to send via X-Trace-Id header for log correlation
     */
    suspend fun fetchRawJson(url: String, dataPath: String? = null, traceId: String? = null): Map<String, Any> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching raw JSON from: $url${traceId?.let { " [trace=$it]" } ?: ""}")

        val requestBuilder = Request.Builder().url(url)
        if (traceId != null) {
            requestBuilder.header("X-Trace-Id", traceId)
        }
        val request = requestBuilder.build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch from $url: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw SduiException("Empty response body from $url")

        @Suppress("UNCHECKED_CAST")
        val json = mapper.readValue(body, Map::class.java) as Map<String, Any>

        // Extract nested path if specified (simple dot notation)
        if (dataPath.isNullOrEmpty()) {
            return@withContext json
        }

        var current: Any = json
        for (segment in dataPath.split(".")) {
            current = when (current) {
                is Map<*, *> -> current[segment] ?: throw SduiException("Path '$segment' not found in response")
                else -> throw SduiException("Cannot navigate path '$segment' - current value is not a map")
            }
        }

        @Suppress("UNCHECKED_CAST")
        when (current) {
            is Map<*, *> -> current as Map<String, Any>
            else -> mapOf("data" to current)
        }
    }
}

/**
 * Exception for SDUI-related errors.
 */
class SduiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when the server signals that the client's schema version is below the
 * minimum supported version via `X-Schema-Version-Mismatch: upgrade-required`.
 * The ViewModel should catch this and display an app-update prompt.
 */
class SchemaVersionMismatchException(message: String) : Exception(message)
