package com.nba.sdui.service;

/**
 * Wire-form color-token constants matched against {@code schema/color-tokens.json}.
 *
 * <p>Every constant is a fully-qualified wire reference (prefixed with
 * {@code token:nba.}) that the registry validates at application startup — see
 * {@link TokenRegistry} and {@link TokenRegistryConsistencyCheck}. Adding a
 * constant that does not exist in the registry is a fail-fast Spring
 * startup error, not a runtime render failure.
 *
 * <p>Composers should prefer these constants over bare hex literals for any
 * color that carries semantic meaning (text, brand, surface, feedback,
 * overlay). Hex literals with embedded alpha (e.g. {@code "#00000014"} for
 * an 8% shadow) remain acceptable where the value is a compositing effect
 * rather than a design-system color.
 *
 * <p>Token names sourced from the Kinetic Design System (Figma export).
 */
public final class ColorTokens {

    private ColorTokens() {}

    // Brand
    public static final String BRAND_NBA  = "token:nba.label.accent.brand";
    public static final String BRAND_LIVE = "token:nba.label.accent.live";

    // Background / Surface
    public static final String SURFACE_CANVAS = "token:nba.bg.primary";
    public static final String SURFACE_RAISED = "token:nba.bg.secondary";
    public static final String SURFACE_SUNKEN = "token:nba.bg.tertiary";
    public static final String SURFACE_PROMO  = "token:nba.bg.splash-screen";

    // Label / Text
    public static final String TEXT_PRIMARY   = "token:nba.label.primary";
    public static final String TEXT_SECONDARY = "token:nba.label.secondary";
    public static final String TEXT_TERTIARY  = "token:nba.label.tertiary";
    public static final String TEXT_INVERSE   = "token:nba.label-inverted.primary";
    public static final String TEXT_ON_BRAND  = "token:nba.label-dark.primary";

    // Divider / Border
    public static final String BORDER_DEFAULT = "token:nba.divider.moderate";
    public static final String BORDER_SUBTLE  = "token:nba.divider.subtle";

    // Effect / Overlay
    public static final String OVERLAY_SCRIM = "token:nba.effect.scrim";

    // Feedback — primary step (50-equivalent)
    public static final String FEEDBACK_SUCCESS = "token:nba.color.feedback.success.50";
    public static final String FEEDBACK_ERROR   = "token:nba.color.feedback.error.50";
    public static final String FEEDBACK_WARNING = "token:nba.color.feedback.warning.50";

    // Palette escape hatch — composers that need to reach a specific palette
    // primitive (e.g. brand-blue deep for a hero gradient) use these.
    public static final String PALETTE_BLUE_30 = "token:nba.color.blue.30";
    public static final String PALETTE_BLUE_50 = "token:nba.color.blue.50";
}
