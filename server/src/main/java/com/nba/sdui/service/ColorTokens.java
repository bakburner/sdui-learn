package com.nba.sdui.service;

/**
 * Wire-form color-token constants matched against {@code schema/color-tokens.json}.
 *
 * <p>Every constant is a fully-qualified wire reference (prefixed with
 * {@code token:}) that the registry validates at application startup — see
 * {@link TokenRegistry} and {@link TokenRegistryConsistencyCheck}. Adding a
 * constant that does not exist in the registry is a fail-fast Spring
 * startup error, not a runtime render failure.
 *
 * <p>Composers should prefer these constants over bare hex literals for any
 * color that carries semantic meaning (text, brand, surface, feedback,
 * overlay). Hex literals with embedded alpha (e.g. {@code "#00000014"} for
 * an 8% shadow) remain acceptable where the value is a compositing effect
 * rather than a design-system color — the token system does not currently
 * encode alpha in semantic tokens.
 */
public final class ColorTokens {

    private ColorTokens() {}

    // Brand
    public static final String BRAND_NBA = "token:color.brand.nba";
    public static final String BRAND_LIVE = "token:color.brand.live";

    // Surface
    public static final String SURFACE_CANVAS = "token:color.surface.canvas";
    public static final String SURFACE_RAISED = "token:color.surface.raised";
    public static final String SURFACE_SUNKEN = "token:color.surface.sunken";
    public static final String SURFACE_PROMO = "token:color.surface.promo";

    // Text
    public static final String TEXT_PRIMARY = "token:color.text.primary";
    public static final String TEXT_SECONDARY = "token:color.text.secondary";
    public static final String TEXT_TERTIARY = "token:color.text.tertiary";
    public static final String TEXT_INVERSE = "token:color.text.inverse";
    public static final String TEXT_ON_BRAND = "token:color.text.onBrand";

    // Border
    public static final String BORDER_DEFAULT = "token:color.border.default";
    public static final String BORDER_SUBTLE = "token:color.border.subtle";

    // Overlay
    public static final String OVERLAY_SCRIM = "token:color.overlay.scrim";

    // Feedback (success / error / warning) — use 50 as the anchor step; request
    // a different step explicitly by spelling it out if callers need it.
    public static final String FEEDBACK_SUCCESS = "token:color.feedback.success.50";
    public static final String FEEDBACK_ERROR = "token:color.feedback.error.50";
    public static final String FEEDBACK_WARNING = "token:color.feedback.warning.50";

    // Palette escape hatch — composers that need to reach a specific palette
    // primitive (e.g. brand-blue deep for a hero gradient) use these rather
    // than hardcoding hex. New palette uses should prefer a semantic above.
    public static final String PALETTE_BLUE_30 = "token:color.blue.30";
    public static final String PALETTE_BLUE_50 = "token:color.blue.50";
}
