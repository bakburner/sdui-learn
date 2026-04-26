package com.nba.sdui.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

class SduiApplication : Application(), ImageLoaderFactory {
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
}
