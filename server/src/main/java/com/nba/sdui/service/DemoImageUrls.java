package com.nba.sdui.service;

import java.util.Locale;

/**
 * Kitchen-sink / demo image URLs. Uses same-origin static assets
 * {@code /sdui-demo/*.svg} (served from Spring {@code static/} in dev, and
 * from {@code web/public/} in the Vite build) so the web app avoids ORB issues
 * with cross-origin cdn.nba.com. Production compositions should use real CDN
 * URLs; query strings are cache-friendly seeds for list variety.
 */
public final class DemoImageUrls {
    private static final String BASE = "/sdui-demo";

    private DemoImageUrls() {
    }

    public static String teamLogo(String nbaTeamId) {
        return query(BASE + "/team.svg", "t" + nbaTeamId);
    }

    public static String headshot(String playerId) {
        return query(BASE + "/headshot.svg", "hs" + playerId);
    }

    public static String cardWide(String key) {
        return query(BASE + "/card-wide.svg", "kw" + (key == null ? "" : key).replace(' ', '-'));
    }

    public static String logoWide() {
        return BASE + "/logo-wide.svg";
    }

    public static String placeholderTiny() {
        return BASE + "/placeholder-tiny.svg";
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
