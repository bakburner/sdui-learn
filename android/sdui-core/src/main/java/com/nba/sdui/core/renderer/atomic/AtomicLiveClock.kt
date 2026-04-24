package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.Format
import com.nba.sdui.core.models.generated.TickDirection
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction
import kotlinx.coroutines.delay
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * AtomicLiveClock — renders a ticking clock whose anchor is supplied
 * by the server as (`snapshotSeconds`, `snapshotAt`, `isRunning`) and
 * whose per-frame displayed value is interpolated on the client.
 *
 * The coroutine driving the tick loop is scoped to the composable's
 * composition, so navigating away or removing the clock cancels the
 * loop automatically. A coarse ~10Hz cadence keeps the visible "ones"
 * digit fluid without being expensive on battery.
 */
@Composable
fun AtomicLiveClock(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    // Resolve the `(snapshotSeconds, snapshotAt, isRunning)` tuple. When
    // `bindRef` is set it points at an object inside the enclosing
    // composite's `data.content` with those three keys — that lets the
    // server push a single `{clock: {...}}` snapshot on every tick
    // instead of threading three independent binding paths.
    val compositeContent = LocalCompositeContent.current
    val bound = BindRefResolver.resolveDictionary(element.bindRef, compositeContent)
    val snapshotSeconds: Long = bound?.get("snapshotSeconds")?.asLongOrNull() ?: (element.snapshotSeconds ?: 0L)
    val snapshotAtRaw: String? = (bound?.get("snapshotAt") as? String) ?: element.snapshotAt
    val snapshotAtMillis = parseSnapshotAtMillis(snapshotAtRaw)
    val running = (bound?.get("isRunning") as? Boolean) ?: (element.isRunning == true)
    val directionDown = (element.tickDirection ?: TickDirection.Down) == TickDirection.Down
    val stopAt: Long? = element.stopAtSeconds
    val format = element.format ?: Format.MSs

    var nowMillis by remember(element.id, snapshotAtRaw, snapshotSeconds, running) {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(element.id, snapshotAtRaw, snapshotSeconds, running) {
        if (!running) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(100L)
        }
    }

    val display = format(
        computeSeconds(
            snapshotSeconds = snapshotSeconds,
            snapshotAtMillis = snapshotAtMillis,
            nowMillis = nowMillis,
            running = running,
            directionDown = directionDown,
            stopAtSeconds = stopAt
        ),
        format
    )

    val baseStyle = when (element.variant) {
        null, "score" -> MaterialTheme.typography.headlineLarge
        else -> mapTypographyVariant(element.variant)
    }
    val style = baseStyle.copy(fontFeatureSettings = "tnum")
    val textColor = ColorTokenResolver.resolve(element.color)

    AtomicBox(element, screenState, onAction) { boxModifier ->
        Text(
            text = display,
            style = style,
            color = textColor,
            modifier = boxModifier.applyAccessibility(element.accessibility)
        )
    }
}

private fun Any?.asLongOrNull(): Long? = when (this) {
    is Long -> this
    is Int -> this.toLong()
    is Number -> this.toLong()
    is String -> this.toLongOrNull()
    else -> null
}

private fun parseSnapshotAtMillis(raw: String?): Long? {
    if (raw.isNullOrEmpty()) return null
    return try {
        OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (e: DateTimeParseException) {
        Log.w("AtomicLiveClock", "snapshotAt_parse_failed: raw=$raw")
        null
    }
}

private fun computeSeconds(
    snapshotSeconds: Long,
    snapshotAtMillis: Long?,
    nowMillis: Long,
    running: Boolean,
    directionDown: Boolean,
    stopAtSeconds: Long?
): Long {
    val displayed: Double = if (running && snapshotAtMillis != null) {
        val elapsed = maxOf(0L, nowMillis - snapshotAtMillis) / 1000.0
        if (directionDown) snapshotSeconds - elapsed else snapshotSeconds + elapsed
    } else {
        snapshotSeconds.toDouble()
    }

    val clamped = when {
        stopAtSeconds != null && directionDown -> maxOf(displayed, stopAtSeconds.toDouble())
        stopAtSeconds != null && !directionDown -> minOf(displayed, stopAtSeconds.toDouble())
        directionDown -> maxOf(displayed, 0.0)
        else -> displayed
    }
    return maxOf(0L, clamped.toLong())
}

private fun format(totalSeconds: Long, format: Format): String = when (format) {
    Format.HMmSs -> {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        "%d:%02d:%02d".format(h, m, s)
    }
    Format.MmSs -> {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        "%02d:%02d".format(m, s)
    }
    Format.MSs -> {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        "%d:%02d".format(m, s)
    }
}
