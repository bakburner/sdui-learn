package com.nba.sdui.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class SduiApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
    }

    /**
     * Install a process-wide uncaught-exception handler so that hard crashes always
     * surface a stack trace in logcat under the [TAG] tag before the platform's
     * default handler tears the process down. Without this, Compose / Navigation
     * exceptions that happen on a worker thread can disappear with no logcat output
     * at all when running under certain emulator / debug configurations.
     *
     * The previous handler is preserved and invoked after logging so the system's
     * crash dialog and tombstone behavior remain intact.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "UNCAUGHT exception on thread '${thread.name}'", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "sdui_image_disk_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .components { add(SvgDecoder.Factory()) }
            .build()

    companion object {
        private const val TAG = "SDUI_Crash"
    }
}
