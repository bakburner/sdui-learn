package com.nba.sdui.service;

/**
 * Wire-form icon-token constants matched against {@code schema/icon-tokens.json}.
 *
 * <p>Each constant is an {@code sdui:}-prefixed token that clients resolve to
 * platform-native icons (SF Symbols on iOS, Material Icons on Android/Web).
 * Unknown tokens fall back to {@link #WARNING} or are hidden.
 */
public final class IconTokens {

    private IconTokens() {}

    public static final String HOME        = "sdui:home";
    public static final String BASKETBALL  = "sdui:basketball";
    public static final String VIDEO       = "sdui:video";
    public static final String LEADERBOARD = "sdui:leaderboard";
    public static final String GRID        = "sdui:grid";
    public static final String BACK        = "sdui:back";
    public static final String SHARE       = "sdui:share";
    public static final String WARNING     = "sdui:warning";
    public static final String LOCK        = "sdui:lock";
}
