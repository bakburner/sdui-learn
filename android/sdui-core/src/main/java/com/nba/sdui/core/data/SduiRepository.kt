package com.nba.sdui.core.data

import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.SduiScreen
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "SduiRepository"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Shared ObjectMapper — registerKotlinModule() is expensive (reflection). */
        val objectMapper: ObjectMapper by lazy {
            ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        /** Shared OkHttpClient — connection pool & thread pool reused across screens. */
        val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        }
    }

    /**
     * Fetch any SDUI screen by its resolved server path.
     *
     * @param path    Path relative to baseUrl, e.g. "/sdui/game-detail/00423"
     * @param envelope  Request envelope builder with platform/device/experiment context
     */
    suspend fun fetchScreen(
        path: String,
        envelope: RequestEnvelopeBuilder
    ): SduiScreen = withContext(Dispatchers.IO) {
        val traceId = envelope.generateTraceId()
        val queryString = envelope.buildQueryString()

        val request = if (envelope.exceedsGetThreshold()) {
            // POST fallback for oversized query strings
            val url = "$baseUrl$path"
            Log.d(TAG, "Fetching screen (POST fallback): $url")
            Request.Builder()
                .url(url)
                .header("X-Trace-Id", traceId)
                .post(queryString.toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } else {
            // Standard GET with bracket-notation query params
            val separator = if (path.contains("?")) "&" else "?"
            val url = "$baseUrl$path${separator}$queryString"
            Log.d(TAG, "Fetching screen: $url")
            Request.Builder()
                .url(url)
                .header("X-Trace-Id", traceId)
                .build()
        }

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch screen: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw SduiException("Empty response body")

        try {
            objectMapper.readValue(body, SduiScreen::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse screen response", e)
            throw SduiException("Failed to parse screen response: ${e.message}")
        }
    }

    /**
     * Fetch raw JSON from any URL (for direct data polling).
     *
     * @param url The full URL to fetch from
     * @param dataPath Optional JSONPath-like path to extract data (e.g., "game" or "sections[0].data")
     */
    suspend fun fetchRawJson(url: String, dataPath: String? = null): Map<String, Any> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching raw JSON from: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch from $url: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw SduiException("Empty response body from $url")

        @Suppress("UNCHECKED_CAST")
        val json = objectMapper.readValue(body, Map::class.java) as Map<String, Any>

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
