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
 */
class SduiRepository(
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "SduiRepository"
        private const val SCHEMA_VERSION = "1.0"
    }
    
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
    
    /**
     * Fetch the game detail SDUI screen.
     * 
     * @param gameId The game ID to fetch
     * @param gameState The game state (live, pre, final)
     */
    suspend fun getGameDetail(
        gameId: String,
        gameState: String = "live",
        variant: String = "A"
    ): SduiScreen = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sdui/game-detail/$gameId?gameState=$gameState&variant=$variant"
        
        Log.d(TAG, "Fetching SDUI response: $url")
        
        val request = Request.Builder()
            .url(url)
            .header("X-Schema-Version", SCHEMA_VERSION)
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch SDUI response: ${response.code}")
        }
        
        val body = response.body?.string()
            ?: throw SduiException("Empty response body")
        
        val traceId = response.header("X-Trace-Id")
        Log.d(TAG, "SDUI response received: traceId=$traceId")
        
        try {
            objectMapper.readValue(body, SduiScreen::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse SDUI response", e)
            throw SduiException("Failed to parse SDUI response: ${e.message}")
        }
    }

    /**
     * Fetch the scoreboard SDUI screen.
     */
    suspend fun getScoreboard(
        variant: String = "A"
    ): SduiScreen = withContext(Dispatchers.IO) {
        val url = "$baseUrl/sdui/scoreboard?variant=$variant"

        Log.d(TAG, "Fetching scoreboard response: $url")

        val request = Request.Builder()
            .url(url)
            .header("X-Schema-Version", SCHEMA_VERSION)
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch scoreboard response: ${response.code}")
        }

        val body = response.body?.string()
            ?: throw SduiException("Empty response body")

        val traceId = response.header("X-Trace-Id")
        Log.d(TAG, "Scoreboard response received: traceId=$traceId")

        try {
            objectMapper.readValue(body, SduiScreen::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse scoreboard response", e)
            throw SduiException("Failed to parse scoreboard response: ${e.message}")
        }
    }
    
    /**
     * Fetch player stats for a game.
     */
    suspend fun getStats(gameId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stats/$gameId"
        
        Log.d(TAG, "Fetching stats: $url")
        
        val request = Request.Builder()
            .url(url)
            .header("X-Schema-Version", SCHEMA_VERSION)
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw SduiException("Failed to fetch stats: ${response.code}")
        }
        
        val body = response.body?.string()
            ?: throw SduiException("Empty response body")
        
        @Suppress("UNCHECKED_CAST")
        objectMapper.readValue(body, Map::class.java) as Map<String, Any>
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
    
    /**
     * Fetch today's games for selection dropdown.
     * Returns empty list on error.
     */
    suspend fun getTodaysGames(): List<GameOption> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/games/today"
        
        Log.d(TAG, "Fetching today's games: $url")
        
        try {
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to fetch games: ${response.code}")
                return@withContext emptyList()
            }
            
            val body = response.body?.string()
            if (body.isNullOrEmpty()) {
                return@withContext emptyList()
            }
            
            objectMapper.readValue(
                body,
                objectMapper.typeFactory.constructCollectionType(List::class.java, GameOption::class.java)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching today's games", e)
            emptyList()
        }
    }
}

/**
 * Game option for dropdown selection.
 */
data class GameOption(
    val gameId: String,
    val label: String,
    val status: String
)

/**
 * Exception for SDUI-related errors.
 */
class SduiException(message: String, cause: Throwable? = null) : Exception(message, cause)
