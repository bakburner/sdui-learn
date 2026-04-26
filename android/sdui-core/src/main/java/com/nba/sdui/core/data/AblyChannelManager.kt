package com.nba.sdui.core.data

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.rest.Auth
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ably Channel Manager - Manages real-time connections to Ably for live data.
 *
 * Messages are returned as opaque `Map<String, Any?>` — the client has NO
 * knowledge of the payload structure.  The server's `dataBinding` on each
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

    /** External listener for connection state changes (connected, disconnected, failed, etc.) */
    var onConnectionStateChange: ((ConnectionState) -> Unit)? = null

    /**
     * Initialize the Ably client with token authentication.
     *
     * Connection is non-blocking — the SDK connects in the background and
     * handles retries automatically. Channels can be subscribed immediately;
     * they attach once the connection is ready.
     *
     * Uses an authCallback instead of authUrl so we control the HTTP request
     * and can log the raw response for diagnostics.
     */
    fun initialize() {
        if (ablyClient != null) {
            Log.d(TAG, "Ably client already initialized")
            return
        }

        try {
            val options = ClientOptions().apply {
                autoConnect = true
                logLevel = io.ably.lib.util.Log.WARN

                authCallback = Auth.TokenCallback { _ ->
                    fetchTokenFromUrl(tokenUrl)
                }
            }

            ablyClient = AblyRealtime(options)

            ablyClient?.connection?.on { state ->
                Log.d(TAG, "Ably connection state: ${state.current}")
                onConnectionStateChange?.invoke(state.current)
                when (state.current) {
                    ConnectionState.connected -> Log.i(TAG, "Ably connected")
                    ConnectionState.disconnected -> Log.w(TAG, "Ably disconnected")
                    ConnectionState.failed -> Log.e(TAG, "Ably connection failed: ${state.reason}")
                    else -> {}
                }
            }

            Log.i(TAG, "Ably client created (tokenUrl: $tokenUrl)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Ably client", e)
            ablyClient = null
        }
    }

    /**
     * Fetch token from the auth URL and extract the JWT.
     *
     * The NBA identity endpoint wraps the token:
     *   {"status":"success","data":{"accessToken":"<JWT>"}}
     *
     * The Ably SDK expects the raw JWT string, not the wrapper.
     */
    private fun fetchTokenFromUrl(url: String): Any {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/json")

            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "(no body)"
                Log.e(TAG, "Token request failed: HTTP $code — $err")
                throw RuntimeException("Token endpoint returned HTTP $code")
            }

            Log.d(TAG, "Token response (HTTP $code, ${body.length} chars): ${body.take(200)}")

            val json = objectMapper.readTree(body)
            val jwt = json.path("data").path("accessToken").asText()
            if (jwt.isNotBlank()) {
                Log.d(TAG, "Extracted JWT (${jwt.length} chars)")
                return jwt
            }

            Log.d(TAG, "No wrapper detected, returning raw body to SDK")
            return body
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Subscribe to a channel and receive messages as opaque Maps.
     *
     * The returned Flow emits the raw JSON payload as `Map<String, Any?>`.
     * Callers use [DataBindingResolver.applyBindings] with the section's
     * `dataBinding` to map fields into section data.
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
            try {
                Log.d(TAG, "Unsubscribing from channel: $channelName")
                channel.unsubscribe(messageListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from channel: $channelName", e)
            } finally {
                activeChannels.remove(channelName)
            }
        }
    }

    /**
     * Disconnect and clean up.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting Ably client")
        for ((name, channel) in activeChannels.toList()) {
            try {
                channel.unsubscribe()
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing channel during disconnect: $name", e)
            }
        }
        activeChannels.clear()
        try {
            ablyClient?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Ably client", e)
        }
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
