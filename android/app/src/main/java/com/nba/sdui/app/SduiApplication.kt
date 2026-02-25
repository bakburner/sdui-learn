package com.nba.sdui.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder

class SduiApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .build()
}
