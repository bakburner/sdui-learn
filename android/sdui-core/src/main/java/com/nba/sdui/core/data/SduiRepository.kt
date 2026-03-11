package com.nba.sdui.core.data

import android.util.Log
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.SduiScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * SDUI Repository - Fetches SDUI responses from the composition service.
 *
 * All screen fetches route through [fetchScreen].  There are NO endpoint-specific
 * helpers — the server path is resolved upstream (by resolveEndpoint or the
 * server's navigation payload) and passed here as-is.
 */
class SduiRepository(
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "SduiRepository"
        private const val SCHEMA_VERSION = "1.0"

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
     * @param path  Path relative to baseUrl, e.g. "/sdui/game-detail/00423?gameState=live"
     * @param variant A/B testing variant
     */
    suspend fun fetchScreen(
        path: String,
        variant: String = "A"
    ): SduiScreen = withContext(Dispatchers.IO) {
        val separator = if (path.contains("?")) "&" else "?"
        val url = "$baseUrl$path${separator}variant=$variant"

        Log.d(TAG, "Fetching screen: $url")

        val request = Request.Builder()
            .url(url)
            .header("X-Schema-Version", SCHEMA_VERSION)
            .build()

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
