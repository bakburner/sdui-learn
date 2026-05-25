package com.nba.sdui.core.renderer

import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.tokens.FormFactorMatrix
import com.nba.sdui.core.tokens.LayoutTokenRegistry
import com.nba.sdui.core.tokens.ShadowSpec
import com.nba.sdui.core.models.generated.AspectRatioEnum
import com.nba.sdui.core.models.generated.AspectRatioUnion
import com.nba.sdui.core.models.generated.LayoutScalar
import com.nba.sdui.core.models.generated.Shadow
import com.nba.sdui.core.models.generated.ShadowOrToken
import com.nba.sdui.core.models.generated.ShadowType
import com.nba.sdui.core.request.RequestEnvelopeBuilder

/**
 * Resolves [LayoutScalar] values and token references against the codegen-baked
 * [LayoutTokenRegistry]. Registry data is bundled at build time and never fetched at runtime.
 * Form-factor changes are observed through Compose and trigger recomposition-driven invalidation.
 *
 * Rules of the road:
 *   - `LayoutScalar.IntegerValue` is returned as-is (raw dp/px on the wire).
 *   - `LayoutScalar.StringValue` must start with the `token:` prefix and resolve through
 *     spacing/radius token maps selected by form factor.
 *   - Strings without the `token:` prefix log a debug diagnostic and
 *     resolve to 0 (lenient resolver — strict decode happened upstream).
 *   - Unknown token names log `token_resolver_missing` and return 0.
 */
object LayoutTokenResolver {

    private const val TAG = "LayoutTokenResolver"
    private const val TOKEN_PREFIX = "token:"

    enum class FormFactor {
        PHONE,
        TABLET,
        TV
    }

    data class TypographySpec(
        val familyRef: String,
        val weight: Int,
        val textCase: String,
        val lineHeight: Double,
        val size: Int
    )

    val LocalFormFactor: ProvidableCompositionLocal<FormFactor> = staticCompositionLocalOf { FormFactor.PHONE }

    @Composable
    fun FormFactorProvider(content: @Composable () -> Unit) {
        val configuration = LocalConfiguration.current
        val formFactor = currentFormFactor(configuration)
        CompositionLocalProvider(LocalFormFactor provides formFactor, content = content)
    }

    /** Resolve a [LayoutScalar] to a Compose [Dp]. `null` → 0.dp. */
    fun dp(
        scalar: LayoutScalar?,
        formFactor: String = RequestEnvelopeBuilder.defaultFormFactor()
    ): Dp = intValue(scalar, toFormFactor(formFactor)).dp

    /** Resolve a [LayoutScalar] to a Compose [Dp]. `null` → 0.dp. */
    fun dp(
        scalar: LayoutScalar?,
        formFactor: FormFactor
    ): Dp = intValue(scalar, formFactor).dp

    /** Resolve a [LayoutScalar] to an integer value (dp/px logical). `null` → 0. */
    fun intValue(
        scalar: LayoutScalar?,
        formFactor: String = RequestEnvelopeBuilder.defaultFormFactor()
    ): Int = intValue(scalar, toFormFactor(formFactor))

    /** Resolve a [LayoutScalar] to an integer value (dp/px logical). `null` → 0. */
    fun intValue(
        scalar: LayoutScalar?,
        formFactor: FormFactor
    ): Int {
        return when (scalar) {
            null -> 0
            is LayoutScalar.IntegerValue -> scalar.value.toInt()
            is LayoutScalar.StringValue -> resolveTokenString(scalar.value, formFactor)
        }
    }

    /**
     * Resolve [AspectRatioUnion] (`DoubleValue` or named ratio enum) to a
     * `width / height` ratio suitable for `Modifier.aspectRatio`. `null`
     * passes through so callers can keep their "only apply when non-null"
     * gating semantics.
     */
    fun aspectRatio(union: AspectRatioUnion?): Float? {
        return when (union) {
            null -> null
            is AspectRatioUnion.DoubleValue -> union.value.toFloat()
            is AspectRatioUnion.EnumValue -> when (union.value) {
                AspectRatioEnum.The11 -> 1f
                AspectRatioEnum.The169 -> 16f / 9f
                AspectRatioEnum.The219 -> 21f / 9f
                AspectRatioEnum.The32 -> 3f / 2f
                AspectRatioEnum.The43 -> 4f / 3f
            }
        }
    }

    /**
     * Derives current form factor from platform-native signals.
     *
     * Resolution precedence:
     * 1) TV uiMode
     * 2) screen width dp (`>= 600` => TABLET, otherwise PHONE)
     * 3) Fallback PHONE
     */
    fun currentFormFactor(config: Configuration?): FormFactor {
        val modeType = config?.uiMode?.and(Configuration.UI_MODE_TYPE_MASK)
        if (modeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return FormFactor.TV
        }
        val widthDp = config?.screenWidthDp ?: 0
        return if (widthDp >= 600) FormFactor.TABLET else FormFactor.PHONE
    }

    fun typography(token: String, formFactor: FormFactor): TypographySpec? {
        val tokenName = tokenName(token) ?: return null
        val variant = LayoutTokenRegistry.typographyVariants[tokenName]
        if (variant == null) {
            Log.d(TAG, "token_resolver_missing: $token")
            return null
        }

        val category = LayoutTokenRegistry.typographyCategories[variant.categoryRef]
        if (category == null) {
            Log.d(TAG, "token_resolver_missing: $token")
            return null
        }

        val resolvedSize = when (formFactor) {
            FormFactor.PHONE -> variant.size.phone
            FormFactor.TABLET -> variant.size.tablet
            FormFactor.TV -> variant.size.tv
        }

        return TypographySpec(
            familyRef = category.familyRef,
            weight = category.weight,
            textCase = category.textCase,
            lineHeight = category.lineHeight,
            size = resolvedSize
        )
    }

    fun shadowSpec(token: String): ShadowSpec? {
        val tokenName = tokenName(token) ?: return null
        val spec = LayoutTokenRegistry.shadows[tokenName]
        if (spec == null) {
            Log.d(TAG, "token_resolver_missing: $token")
        }
        return spec
    }

    fun resolveShadowOrToken(value: ShadowOrToken?): Shadow? {
        return when (value) {
            null -> null
            is ShadowOrToken.ShadowValue -> value.value
            is ShadowOrToken.StringValue -> tokenToShadow(value.value)
        }
    }

    fun resolveShadowOrTokens(values: List<ShadowOrToken>?): List<Shadow> {
        return values?.mapNotNull { resolveShadowOrToken(it) } ?: emptyList()
    }

    fun motionDuration(token: String, formFactor: FormFactor): Int? {
        val tokenName = tokenName(token) ?: return null
        val row = LayoutTokenRegistry.motionDuration[tokenName]
        if (row == null) {
            Log.d(TAG, "token_resolver_missing: $token")
            return null
        }
        return valueFor(formFactor, row)
    }

    fun motionEasing(token: String): String? {
        val tokenName = tokenName(token) ?: return null
        val easing = LayoutTokenRegistry.motionEasing[tokenName]
        if (easing == null) {
            Log.d(TAG, "token_resolver_missing: $token")
        }
        return easing
    }

    private fun resolveTokenString(wire: String, formFactor: FormFactor): Int {
        val tokenName = tokenName(wire) ?: return 0
        val spacing = LayoutTokenRegistry.spacing[tokenName]
        if (spacing != null) {
            return valueFor(formFactor, spacing)
        }

        val radius = LayoutTokenRegistry.radius[tokenName]
        if (radius != null) {
            return valueFor(formFactor, radius)
        }

        Log.d(TAG, "token_resolver_missing: $wire")
        return 0
    }

    private fun tokenToShadow(token: String): Shadow? {
        val spec = shadowSpec(token) ?: return null
        return Shadow(
            type = if (spec.type.equals("inner", ignoreCase = true)) ShadowType.Inner else ShadowType.Drop,
            color = spec.color,
            radius = spec.radius.toDouble(),
            offsetX = spec.offsetX.toDouble(),
            offsetY = spec.offsetY.toDouble()
        )
    }

    private fun tokenName(wire: String): String? {
        if (!wire.startsWith(TOKEN_PREFIX)) {
            Log.d(TAG, "token_resolver_missing: $wire")
            return null
        }
        return wire.removePrefix(TOKEN_PREFIX)
    }

    private fun toFormFactor(formFactor: String): FormFactor {
        return when (formFactor) {
            "tablet" -> FormFactor.TABLET
            "tv" -> FormFactor.TV
            else -> FormFactor.PHONE
        }
    }

    private fun valueFor(formFactor: FormFactor, row: FormFactorMatrix<Int>): Int {
        return when (formFactor) {
            FormFactor.PHONE -> row.phone
            FormFactor.TABLET -> row.tablet
            FormFactor.TV -> row.tv
        }
    }

}
