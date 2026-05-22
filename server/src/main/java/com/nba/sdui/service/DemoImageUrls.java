package com.nba.sdui.service;

import java.util.Locale;

/**
 * Kitchen-sink / demo image URLs. Uses same-origin static assets
 * {@code /sdui-demo/*.png} (served from Spring {@code static/} in dev, and
 * from {@code web/public/} in the Vite build) so the web app avoids ORB issues
 * with cross-origin cdn.nba.com. Production compositions should use real CDN
 * URLs; query strings are cache-friendly seeds for list variety.
 */
public final class DemoImageUrls {
    private static final String BASE = "/sdui-demo";

    private DemoImageUrls() {
    }

    public static String teamLogo(String nbaTeamId) {
        return query(BASE + "/team.png", "t" + nbaTeamId);
    }

    public static String headshot(String playerId) {
        return query(BASE + "/headshot.png", "hs" + playerId);
    }

    public static String cardWide(String key) {
        return query(BASE + "/card-wide.png", "kw" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String hero(String key) {
        return query(BASE + "/card-wide.png", "hero" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String cardTall(String key) {
        return query(BASE + "/card-wide.png", "tall" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String thumb(String key) {
        return query(BASE + "/card-wide.png", "th" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String avatar(String key) {
        return query(BASE + "/headshot.png", "av" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String logoWide() {
        return BASE + "/logo-wide.png";
    }

    public static String placeholderTiny() {
        return BASE + "/placeholder-tiny.png";
    }

    private static String query(String path, String rawSeed) {
        String s = rawSeed == null ? "" : rawSeed;
        s = s.replaceAll("[^a-zA-Z0-9._-]+", "-");
        if (s.isEmpty()) {
            s = "x";
        }
        return path + "?v=" + s.toLowerCase(Locale.ROOT);
    }
}
