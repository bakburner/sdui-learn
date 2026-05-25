// THIS FILE IS GENERATED. DO NOT EDIT.

package com.nba.sdui.core.generated

data class FormFactorMatrix<T>(
    val phone: T,
    val tablet: T,
    val tv: T,
    val web: T,
)

data class WebSizeEnvelope(
    val min: Int,
    val max: Int,
    val minVw: Int,
    val maxVw: Int,
)

sealed class WebSize {
    data class Scalar(val value: Int) : WebSize()
    data class Envelope(val min: Int, val max: Int, val minVw: Int, val maxVw: Int) : WebSize()
}

data class TypographySize(
    val phone: Int,
    val tablet: Int,
    val tv: Int,
    val web: WebSize,
)

data class TypographyCategorySpec(
    val familyRef: String,
    val weight: Int,
    val textCase: String,
    val lineHeight: Double,
)

data class TypographyVariantSpec(
    val categoryRef: String,
    val size: TypographySize,
)

data class ShadowSpec(
    val type: String,
    val color: String,
    val radius: Int,
    val offsetX: Int,
    val offsetY: Int,
)

object LayoutTokenRegistry {
    val spacing: Map<String, FormFactorMatrix<Int>> = linkedMapOf(
        "nba.spacing.xs" to FormFactorMatrix(phone = 2, tablet = 2, tv = 4, web = 2),
        "nba.spacing.sm" to FormFactorMatrix(phone = 4, tablet = 6, tv = 6, web = 4),
        "nba.spacing.md" to FormFactorMatrix(phone = 12, tablet = 15, tv = 18, web = 12),
        "nba.spacing.lg" to FormFactorMatrix(phone = 16, tablet = 20, tv = 24, web = 16),
        "nba.spacing.xl" to FormFactorMatrix(phone = 32, tablet = 40, tv = 48, web = 32),
        "nba.spacing.2xl" to FormFactorMatrix(phone = 40, tablet = 48, tv = 56, web = 40),
    )

    val radius: Map<String, FormFactorMatrix<Int>> = linkedMapOf(
        "nba.radius.xs" to FormFactorMatrix(phone = 2, tablet = 2, tv = 2, web = 2),
        "nba.radius.sm" to FormFactorMatrix(phone = 4, tablet = 4, tv = 4, web = 4),
        "nba.radius.md" to FormFactorMatrix(phone = 12, tablet = 12, tv = 12, web = 12),
        "nba.radius.lg" to FormFactorMatrix(phone = 16, tablet = 16, tv = 16, web = 16),
        "nba.radius.xl" to FormFactorMatrix(phone = 24, tablet = 24, tv = 24, web = 24),
        "nba.radius.2xl" to FormFactorMatrix(phone = 32, tablet = 32, tv = 32, web = 32),
        "nba.radius.full" to FormFactorMatrix(phone = 9999, tablet = 9999, tv = 9999, web = 9999),
    )

    val typographyCategories: Map<String, TypographyCategorySpec> = linkedMapOf(
        "nba.typography.headline" to TypographyCategorySpec(familyRef = "nba.font.knockout", weight = 360, textCase = "uppercase", lineHeight = 0.8),
        "nba.typography.display" to TypographyCategorySpec(familyRef = "nba.font.knockout", weight = 395, textCase = "uppercase", lineHeight = 0.8),
        "nba.typography.title" to TypographyCategorySpec(familyRef = "nba.font.roboto", weight = 500, textCase = "none", lineHeight = 1.2),
        "nba.typography.body" to TypographyCategorySpec(familyRef = "nba.font.roboto", weight = 400, textCase = "none", lineHeight = 1.2),
        "nba.typography.label" to TypographyCategorySpec(familyRef = "nba.font.roboto", weight = 400, textCase = "uppercase", lineHeight = 1.0),
        "nba.typography.data" to TypographyCategorySpec(familyRef = "nba.font.roboto.condensed", weight = 400, textCase = "uppercase", lineHeight = 1.0),
        "nba.typography.score" to TypographyCategorySpec(familyRef = "nba.font.knockout", weight = 360, textCase = "uppercase", lineHeight = 0.8),
        "nba.typography.button" to TypographyCategorySpec(familyRef = "nba.font.roboto", weight = 700, textCase = "none", lineHeight = 1.0),
        "nba.typography.caption" to TypographyCategorySpec(familyRef = "nba.font.roboto", weight = 400, textCase = "none", lineHeight = 1.2),
    )

    val typographyVariants: Map<String, TypographyVariantSpec> = linkedMapOf(
        "nba.typography.displayLarge" to TypographyVariantSpec(categoryRef = "nba.typography.display", size = TypographySize(phone = 57, tablet = 64, tv = 96, web = WebSize.Envelope(min = 45, max = 96, minVw = 320, maxVw = 1440))),
        "nba.typography.displayMedium" to TypographyVariantSpec(categoryRef = "nba.typography.display", size = TypographySize(phone = 45, tablet = 56, tv = 80, web = WebSize.Envelope(min = 36, max = 80, minVw = 320, maxVw = 1440))),
        "nba.typography.displaySmall" to TypographyVariantSpec(categoryRef = "nba.typography.display", size = TypographySize(phone = 36, tablet = 48, tv = 64, web = WebSize.Envelope(min = 32, max = 64, minVw = 320, maxVw = 1440))),
        "nba.typography.headlineLarge" to TypographyVariantSpec(categoryRef = "nba.typography.headline", size = TypographySize(phone = 32, tablet = 40, tv = 64, web = WebSize.Envelope(min = 28, max = 64, minVw = 320, maxVw = 1440))),
        "nba.typography.headlineMedium" to TypographyVariantSpec(categoryRef = "nba.typography.headline", size = TypographySize(phone = 28, tablet = 32, tv = 48, web = WebSize.Envelope(min = 24, max = 48, minVw = 320, maxVw = 1440))),
        "nba.typography.headlineSmall" to TypographyVariantSpec(categoryRef = "nba.typography.headline", size = TypographySize(phone = 24, tablet = 28, tv = 40, web = WebSize.Envelope(min = 22, max = 40, minVw = 320, maxVw = 1440))),
        "nba.typography.titleLarge" to TypographyVariantSpec(categoryRef = "nba.typography.title", size = TypographySize(phone = 22, tablet = 24, tv = 32, web = WebSize.Envelope(min = 20, max = 28, minVw = 320, maxVw = 1440))),
        "nba.typography.titleMedium" to TypographyVariantSpec(categoryRef = "nba.typography.title", size = TypographySize(phone = 16, tablet = 18, tv = 24, web = WebSize.Scalar(16))),
        "nba.typography.titleSmall" to TypographyVariantSpec(categoryRef = "nba.typography.title", size = TypographySize(phone = 14, tablet = 16, tv = 20, web = WebSize.Scalar(14))),
        "nba.typography.bodyLarge" to TypographyVariantSpec(categoryRef = "nba.typography.body", size = TypographySize(phone = 16, tablet = 18, tv = 24, web = WebSize.Envelope(min = 16, max = 18, minVw = 320, maxVw = 1440))),
        "nba.typography.bodyMedium" to TypographyVariantSpec(categoryRef = "nba.typography.body", size = TypographySize(phone = 14, tablet = 16, tv = 20, web = WebSize.Envelope(min = 14, max = 18, minVw = 320, maxVw = 1440))),
        "nba.typography.bodySmall" to TypographyVariantSpec(categoryRef = "nba.typography.body", size = TypographySize(phone = 12, tablet = 14, tv = 18, web = WebSize.Scalar(12))),
        "nba.typography.labelLarge" to TypographyVariantSpec(categoryRef = "nba.typography.label", size = TypographySize(phone = 14, tablet = 14, tv = 18, web = WebSize.Scalar(14))),
        "nba.typography.labelMedium" to TypographyVariantSpec(categoryRef = "nba.typography.label", size = TypographySize(phone = 12, tablet = 12, tv = 16, web = WebSize.Scalar(12))),
        "nba.typography.labelSmall" to TypographyVariantSpec(categoryRef = "nba.typography.label", size = TypographySize(phone = 11, tablet = 11, tv = 14, web = WebSize.Scalar(11))),
        "nba.typography.score" to TypographyVariantSpec(categoryRef = "nba.typography.score", size = TypographySize(phone = 32, tablet = 48, tv = 80, web = WebSize.Envelope(min = 28, max = 56, minVw = 320, maxVw = 1440))),
    )

    val motionDuration: Map<String, FormFactorMatrix<Int>> = linkedMapOf(
        "nba.motion.duration.fast" to FormFactorMatrix(phone = 150, tablet = 180, tv = 250, web = 200),
        "nba.motion.duration.default" to FormFactorMatrix(phone = 200, tablet = 250, tv = 350, web = 300),
        "nba.motion.duration.slow" to FormFactorMatrix(phone = 400, tablet = 500, tv = 700, web = 600),
        "nba.motion.duration.hero" to FormFactorMatrix(phone = 500, tablet = 600, tv = 900, web = 800),
    )

    val motionEasing: Map<String, String> = linkedMapOf(
        "nba.motion.easing.default" to "cubic-bezier(0.16, 1, 0.3, 1)",
        "nba.motion.easing.linear" to "linear",
    )

    val shadows: Map<String, ShadowSpec> = linkedMapOf(
        "nba.shadow.sm" to ShadowSpec(type = "drop", color = "rgba(0,0,0,0.12)", radius = 3, offsetX = 0, offsetY = 1),
        "nba.shadow.md" to ShadowSpec(type = "drop", color = "rgba(0,0,0,0.15)", radius = 8, offsetX = 0, offsetY = 2),
        "nba.shadow.lg" to ShadowSpec(type = "drop", color = "rgba(0,0,0,0.12)", radius = 16, offsetX = 0, offsetY = 4),
        "nba.shadow.xl" to ShadowSpec(type = "drop", color = "rgba(0,0,0,0.25)", radius = 32, offsetX = 0, offsetY = 8),
    )
}
