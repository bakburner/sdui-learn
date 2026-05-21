package com.nba.sdui.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nba.sdui.app.ui.GameDetailScreen
import com.nba.sdui.app.ui.theme.SduiPrototypeTheme
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SDUI_MainActivity"
        // Degraded-connectivity fallback only — primary bootstrap URI comes from /sdui/init.
        private const val FALLBACK_BOOTSTRAP_URI = "nba://for-you"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, ">>> onCreate START")
        enableEdgeToEdge()
        Log.e(TAG, ">>> setContent about to be called")
        setContent {
            val systemDarkTheme = isSystemInDarkTheme()
            var darkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }

            SduiPrototypeTheme(darkTheme = darkTheme) {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var currentConfig by remember { mutableStateOf<SduiConfig?>(null) }

                // Fetch bootstrap URI from /sdui/init on first composition.
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    val uri = fetchBootstrapUri()
                    currentConfig = SduiConfig.fromUri(uri)
                }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    val config = currentConfig ?: return@Scaffold

                    GameDetailScreen(
                        config = config,
                        darkTheme = darkTheme,
                        modifier = Modifier.padding(innerPadding),
                        onNavigateUri = { targetUri ->
                            // All URIs use the same code path — the server
                            // owns refresh policies, Ably config, and layout.
                            currentConfig = SduiConfig.fromUri(targetUri)
                        },
                        onShowToast = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        onVariantChange = { nextVariant ->
                            currentConfig = config.copy(
                                experiments = config.experiments + ("game_detail_variant" to nextVariant)
                            )
                        },
                        onBack = {
                            scope.launch {
                                currentConfig = SduiConfig.fromUri(fetchBootstrapUri())
                            }
                        },
                        onToggleTheme = {
                            darkTheme = !darkTheme
                        }
                    )
                }
            }
        }
    }

    /**
     * Fetch the bootstrap URI from `/sdui/init`. Falls back to
     * [FALLBACK_BOOTSTRAP_URI] on network errors.
     */
    private suspend fun fetchBootstrapUri(): String = try {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val envelope = com.nba.sdui.core.request.RequestEnvelopeBuilder()
            val qs = envelope.buildQueryString()
            val conn = URL("${BuildConfig.SDUI_ANDROID_BASE_URL}/sdui/init?$qs").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            try {
                val body = conn.inputStream.bufferedReader().readText()
                JSONObject(body).optString("bootstrapUri", FALLBACK_BOOTSTRAP_URI)
            } finally {
                conn.disconnect()
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch /sdui/init, using fallback", e)
        FALLBACK_BOOTSTRAP_URI
    }
}
