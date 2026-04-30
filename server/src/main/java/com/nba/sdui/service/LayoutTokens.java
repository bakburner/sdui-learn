package com.nba.sdui.service;

/**
 * Wire-form layout-token constants matched against {@code schema/spacing-tokens.json}
 * and {@code schema/corner-radius-tokens.json}.
 *
 * <p>Every constant is a fully-qualified wire reference (prefixed with
 * {@code token:nba.}) that is resolved by clients via their form-factor-aware
 * {@code LayoutTokenResolver}. The server emits these strings instead of
 * raw integers so that tablet, TV, and web-wide clients can apply scaled
 * values automatically.
 *
 * <p>Composers should prefer these constants over bare integer literals for
 * any spacing or radius value that maps to the design-system scale. Raw
 * integers remain acceptable for: zero, calculated values (e.g. circle
 * radius = width/2), component-specific fixed dimensions (e.g. card
 * carousel widths), and values with no token mapping.
 *
 * <p>Values sourced from the Kinetic Design System (Figma export).
 */
public final class LayoutTokens {

    private LayoutTokens() {}

    // ─── Spacing (Kinetic gap/padding unified) ──────────────────────────────────
    // 6 tokens. Phone base values shown.

    public static final String SPACING_XS  = "token:nba.spacing.xs";   // phone: 2
    public static final String SPACING_SM  = "token:nba.spacing.sm";   // phone: 4
    public static final String SPACING_MD  = "token:nba.spacing.md";   // phone: 12
    public static final String SPACING_LG  = "token:nba.spacing.lg";   // phone: 16
    public static final String SPACING_XL  = "token:nba.spacing.xl";   // phone: 32
    public static final String SPACING_2XL = "token:nba.spacing.2xl";  // phone: 40

    // ─── Corner Radius (Kinetic) ────────────────────────────────────────────────
    // 7 tokens. Flat across form factors.

    public static final String RADIUS_XS   = "token:nba.radius.xs";    // 2
    public static final String RADIUS_SM   = "token:nba.radius.sm";    // 4
    public static final String RADIUS_MD   = "token:nba.radius.md";    // 12
    public static final String RADIUS_LG   = "token:nba.radius.lg";    // 16
    public static final String RADIUS_XL   = "token:nba.radius.xl";    // 24
    public static final String RADIUS_2XL  = "token:nba.radius.2xl";   // 32
    public static final String RADIUS_FULL = "token:nba.radius.full";  // 9999
}
