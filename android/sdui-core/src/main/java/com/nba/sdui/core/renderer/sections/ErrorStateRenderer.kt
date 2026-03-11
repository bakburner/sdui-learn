package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.state.SduiAction

/**
 * ErrorState Renderer — displays an error message with optional retry.
 *
 * Renders the server-provided ErrorStateData: title, message, icon, and
 * an optional retryAction that triggers a navigate action.
 */
@Composable
fun ErrorStateRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: emptyMap()
    val title = data["title"] as? String ?: "Something went wrong"
    val message = data["message"] as? String ?: ""
    val icon = data["icon"] as? String
    @Suppress("UNCHECKED_CAST")
    val retryAction = data["retryAction"] as? Map<String, Any?>

    val emoji = when (icon) {
        "error" -> "⚠\uFE0F"
        "wifi_off" -> "\uD83D\uDCE1"
        "not_found" -> "\uD83D\uDD0D"
        "timeout" -> "⏱\uFE0F"
        else -> "⚠\uFE0F"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (retryAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val action = SduiAction(
                        type = retryAction["type"] as? String ?: "navigate",
                        trigger = retryAction["trigger"] as? String ?: "tap",
                        targetUri = retryAction["targetUri"] as? String
                    )
                    onAction(action)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Try Again")
            }
        }
    }
}
