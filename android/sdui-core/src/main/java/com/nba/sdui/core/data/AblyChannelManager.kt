package com.nba.sdui.core.data

import android.util.Log
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
 * Messages are returned as opaque `Map<String, Any?>` — the client has NO
 * knowledge of the payload structure.  The server's `dataBindings` on each
 * section define how individual fields map into section data.
 *
 * Implements:
 * - JWT token authentication via authUrl (NBA identity server)
 * - Channel subscription for real-time updates
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
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (ablyClient != null) {
            Log.d(TAG, "Ably client already initialized")
            return@withContext
        }

        try {
            val options = ClientOptions().apply {
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
     * Subscribe to a channel and receive messages as opaque Maps.
     *
     * The returned Flow emits the raw JSON payload as `Map<String, Any?>`.
     * Callers use [DataBindingResolver.applyBindings] with the section's
     * `dataBindings` to map fields into section data.
     */
    fun subscribeToChannel(channelName: String): Flow<Map<String, Any?>> = callbackFlow {
        Log.d(TAG, "Subscribing to channel: $channelName")

        val client = ablyClient ?: throw IllegalStateException("Ably client not initialized")

        val channel = client.channels.get(channelName)
        activeChannels[channelName] = channel

        channel.on { state ->
            Log.d(TAG, "Channel $channelName state: ${state.current}")
            when (state.current) {
                ChannelState.attached -> Log.i(TAG, "Channel attached: $channelName")
                ChannelState.detached -> Log.w(TAG, "Channel detached: $channelName")
                ChannelState.failed -> Log.e(TAG, "Channel failed: $channelName, reason=${state.reason}")
                else -> {}
            }
        }

        val messageListener = Channel.MessageListener { message: Message ->
            Log.d(TAG, "Received message on $channelName: ${message.name}")

            try {
                val parsed = parseMessage(message)
                if (parsed != null) {
                    trySend(parsed)
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

    /**
     * Parse an Ably message into an opaque Map.
     * No field-level knowledge of the payload — just raw JSON → Map.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseMessage(message: Message): Map<String, Any?>? {
        val data = message.data ?: return null

        return try {
            when (data) {
                is String -> objectMapper.readValue(data, Map::class.java) as Map<String, Any?>
                is Map<*, *> -> data as Map<String, Any?>
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message payload", e)
            null
        }
    }
}
