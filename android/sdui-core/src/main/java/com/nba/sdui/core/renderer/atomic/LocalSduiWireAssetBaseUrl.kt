package com.nba.sdui.core.renderer.atomic

import androidx.compose.runtime.compositionLocalOf

/**
 * Composition service origin for resolving root-relative wire asset URLs.
 * Provided at screen scope from [com.nba.sdui.core.screen.SduiScreenContent].
 */
val LocalSduiWireAssetBaseUrl = compositionLocalOf { "" }
