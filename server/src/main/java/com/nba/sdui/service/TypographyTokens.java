package com.nba.sdui.service;

/**
 * Wire-form typography token constants matched against {@code schema/typography-tokens.json}.
 *
 * <p>These constants expose semantic {@code TextVariant}-aligned entries only.
 * Server-internal category specs (family/weight/textCase/lineHeight) do not ship
 * on the wire as direct token names.
 */
public final class TypographyTokens {

    private TypographyTokens() {}

    public static final String DISPLAY_LARGE = "token:nba.typography.displayLarge";
    public static final String DISPLAY_MEDIUM = "token:nba.typography.displayMedium";
    public static final String DISPLAY_SMALL = "token:nba.typography.displaySmall";

    public static final String HEADLINE_LARGE = "token:nba.typography.headlineLarge";
    public static final String HEADLINE_MEDIUM = "token:nba.typography.headlineMedium";
    public static final String HEADLINE_SMALL = "token:nba.typography.headlineSmall";

    public static final String TITLE_LARGE = "token:nba.typography.titleLarge";
    public static final String TITLE_MEDIUM = "token:nba.typography.titleMedium";
    public static final String TITLE_SMALL = "token:nba.typography.titleSmall";

    public static final String BODY_LARGE = "token:nba.typography.bodyLarge";
    public static final String BODY_MEDIUM = "token:nba.typography.bodyMedium";
    public static final String BODY_SMALL = "token:nba.typography.bodySmall";

    public static final String LABEL_LARGE = "token:nba.typography.labelLarge";
    public static final String LABEL_MEDIUM = "token:nba.typography.labelMedium";
    public static final String LABEL_SMALL = "token:nba.typography.labelSmall";

    public static final String SCORE = "token:nba.typography.score";
}
