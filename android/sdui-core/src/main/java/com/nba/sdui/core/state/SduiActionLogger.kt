package com.nba.sdui.core.state

import android.util.Log
import com.nba.sdui.core.BuildConfig

/**
 * Debug-only logger for the SDUI action pipeline.
 *
 * The action system fires many actions per session — taps, onVisible
 * beacons, mutates, refreshes — and `fireAndForget` in particular has
 * no on-screen side effect, which makes it impossible to verify visually
 * during local testing without instrumentation.
 *
 * This logger surfaces every action through `logcat` with a consistent
 * `SDUI/Action` tag whenever [enabled] is true. By default that mirrors
 * `BuildConfig.DEBUG` for `sdui-core`, so verbose logs ride debug builds
 * and disappear in release. Hosts can flip [enabled] explicitly to
 * force-enable or force-suppress in non-standard environments (e.g.
 * release-build dogfooding sessions).
 *
 * Mirrors iOS's `os.Logger.debug(...)` calls in
 * [`ActionDispatcher`](../../../../../../../../../../ios/Sources/SduiCore/Runtime/ActionDispatcher.swift)
 * and web's `actionLog(...)` helper.
 */
object SduiActionLogger {
    private const val TAG = "SDUI/Action"

    /**
     * When true, [debug] entries are written to logcat. Defaults to
     * `BuildConfig.DEBUG`. Hosts can override this at startup if they
     * want to capture action telemetry from a release build.
     */
    @JvmStatic
    var enabled: Boolean = BuildConfig.DEBUG

    /** Free-form debug entry. Cheap when [enabled] is false. */
    @JvmStatic
    fun debug(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    /**
     * Action-scoped debug entry. Prefixes the message with the action's
     * trigger/type so logcat filtering by trigger ("onVisible", "onActivate")
     * or type ("fireAndForget") works without bespoke tagging at every
     * call site.
     */
    @JvmStatic
    fun debug(action: SduiAction, message: String) {
        if (enabled) Log.d(TAG, "[${action.trigger}/${action.type}] $message")
    }
}
