package com.nba.sdui.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nba.sdui.app.ui.GameDetailScreen
import com.nba.sdui.app.ui.theme.SduiPrototypeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "SDUI_MainActivity"
        // TODO(Rule 2): Bootstrap URI should come from a /sdui/init endpoint.
        private const val BOOTSTRAP_URI = "nba://for-you"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, ">>> onCreate START")
        enableEdgeToEdge()
        Log.e(TAG, ">>> setContent about to be called")
        setContent {
            SduiPrototypeTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                var currentConfig by remember { mutableStateOf(SduiConfig.fromUri(BOOTSTRAP_URI)) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    val config = currentConfig

                    GameDetailScreen(
                        config = config,
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
                            currentConfig = config.copy(variant = nextVariant)
                        },
                        onBack = {
                            currentConfig = SduiConfig.fromUri(BOOTSTRAP_URI)
                        }
                    )
                }
            }
        }
    }
}
