package com.nba.sdui.core.data

import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Ably Channel Manager - Manages real-time connections to Ably for live data.
 * 
 * Implements:
 * - JWT token authentication via authUrl (NBA identity server)
 * - Channel subscription for linescore updates
 * - Flow-based message delivery to Compose UI
 */
class AblyChannelManager(
    private val tokenUrl: String
) {
    companion object {
        private const val TAG = "AblyChannelManager"
    }
    
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    private var ablyClient: AblyRealtime? = null
    private val activeChannels = mutableMapOf<String, Channel>()
    
    /**
     * Initialize the Ably client with JWT token authentication.
     * 
     * Uses the NBA identity server to fetch JWT tokens which Ably
     * accepts directly for authentication.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (ablyClient != null) {
            Log.d(TAG, "Ably client already initialized")
            return@withContext
        }
        
        try {
            val options = ClientOptions().apply {
                // Use authUrl for JWT-based auth
                // The NBA identity server returns a JWT that Ably accepts directly
                authUrl = tokenUrl
                autoConnect = true
                logLevel = io.ably.lib.util.Log.VERBOSE
            }
            
            ablyClient = AblyRealtime(options)
            
            ablyClient?.connection?.on { state ->
                Log.d(TAG, "Ably connection state: ${state.current}")
                when (state.current) {
                    ConnectionState.connected -> Log.i(TAG, "Ably connected")
                    ConnectionState.disconnected -> Log.w(TAG, "Ably disconnected")
                    ConnectionState.failed -> Log.e(TAG, "Ably connection failed: ${state.reason}")
                    else -> {}
                }
            }
            
            Log.i(TAG, "Ably client initialized with authUrl: $tokenUrl")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Ably client", e)
            throw e
        }
    }
    
    /**
     * Subscribe to a channel and receive messages as a Flow.
     */
    fun subscribeToChannel(channelName: String): Flow<LinescoreUpdate> = callbackFlow {
        Log.d(TAG, "Subscribing to channel: $channelName")
        
        val client = ablyClient ?: throw IllegalStateException("Ably client not initialized")
        
        val channel = client.channels.get(channelName)
        activeChannels[channelName] = channel
        
        // Listen for channel state changes
        channel.on { state ->
            Log.d(TAG, "Channel $channelName state: ${state.current}")
            when (state.current) {
                ChannelState.attached -> Log.i(TAG, "Channel attached: $channelName")
                ChannelState.detached -> Log.w(TAG, "Channel detached: $channelName")
                ChannelState.failed -> Log.e(TAG, "Channel failed: $channelName, reason=${state.reason}")
                else -> {}
            }
        }
        
        // Subscribe to messages
        val messageListener = Channel.MessageListener { message: Message ->
            Log.d(TAG, "Received message on $channelName: ${message.name}")
            
            try {
                val update = parseLinescoreMessage(message)
                if (update != null) {
                    trySend(update)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message", e)
            }
        }
        
        channel.subscribe(messageListener)
        
        awaitClose {
            Log.d(TAG, "Unsubscribing from channel: $channelName")
            channel.unsubscribe(messageListener)
            activeChannels.remove(channelName)
        }
    }
    
    /**
     * Disconnect and clean up.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting Ably client")
        activeChannels.clear()
        ablyClient?.close()
        ablyClient = null
    }
    
    private fun parseLinescoreMessage(message: Message): LinescoreUpdate? {
        val data = message.data ?: return null
        
        return try {
            val json: JsonNode = when (data) {
                is String -> objectMapper.readTree(data)
                is Map<*, *> -> objectMapper.valueToTree(data)
                else -> return null
            }
            
            LinescoreUpdate(
                homeTeamScore = json.path("homeTeam").path("score").asInt(),
                awayTeamScore = json.path("awayTeam").path("score").asInt(),
                homeTeamTricode = json.path("homeTeam").path("teamTricode").asText(),
                awayTeamTricode = json.path("awayTeam").path("teamTricode").asText(),
                period = json.path("period").asInt(),
                gameStatus = json.path("gameStatus").asInt(),
                gameStatusText = json.path("gameStatusText").asText(),
                clock = json.path("clock").asText()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse linescore message", e)
            null
        }
    }
}

/**
 * Parsed linescore update from Ably.
 */
data class LinescoreUpdate(
    val homeTeamScore: Int,
    val awayTeamScore: Int,
    val homeTeamTricode: String,
    val awayTeamTricode: String,
    val period: Int,
    val gameStatus: Int,
    val gameStatusText: String,
    val clock: String?
)
