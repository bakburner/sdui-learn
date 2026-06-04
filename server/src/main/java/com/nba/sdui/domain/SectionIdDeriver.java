package com.nba.sdui.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Derives stable section IDs from content-source metadata.
 *
 * <p>IDs use a self-describing key=value format delimited by {@code ~}:
 * <pre>
 *   stats-api:game-123~type=AtomicComposite~slug=scoreboard
 * </pre>
 *
 * <p>The first segment is always the bare content-source ID (no key prefix).
 * Subsequent segments are labeled key=value pairs. The tilde delimiter is
 * URL-unreserved (RFC 3986), so IDs travel safely in path segments and query
 * params without percent-encoding.
 *
 * <p>Feed pages (For You, Watch, Home): each content item has a unique
 * content-source ID, so {@code source~type=...} is sufficient.
 *
 * <p>Named-region pages (Game Detail, Boxscore): the same content source
 * produces multiple sections of the same type — a slug disambiguates.
 *
 * <p>Position is never part of the ID. IDs are stable across reorders,
 * ad-slot insertions, and A/B test variants.
 */
public final class SectionIdDeriver {

    static final String SEPARATOR = "~";
    static final String TYPE_KEY = "type";
    static final String SLUG_KEY = "slug";

    private SectionIdDeriver() {}

    /**
     * Structured parse result of a derived section ID. Provides direct access
     * to each component by name — no substring scanning at call sites.
     *
     * @param source the content-source ID (e.g. "stats-api:game-0022500123")
     * @param type   the section type (e.g. "AtomicComposite"), or null for flat IDs
     * @param slug   the section slug (e.g. "scoreboard"), or null if absent
     * @param fields all key=value fields (for forward-compatible extension)
     */
    public record Parsed(String source, String type, String slug, Map<String, String> fields) {

        /** True when the ID was in derived format (had at least a {@code type} field). */
        public boolean isDerived() {
            return type != null;
        }
    }

    // ── Derivation ─────────────────────────────────────────────────────

    /**
     * Feed-page form: contentSourceId + type is unique.
     */
    public static String derive(String contentSourceId, String sectionType) {
        return contentSourceId + SEPARATOR + TYPE_KEY + "=" + sectionType;
    }

    /**
     * Named-region form: slug disambiguates when the same source + type repeats.
     */
    public static String derive(String contentSourceId, String sectionType, String slug) {
        return contentSourceId + SEPARATOR + TYPE_KEY + "=" + sectionType
                + SEPARATOR + SLUG_KEY + "=" + slug;
    }

    // ── Parsing ────────────────────────────────────────────────────────

    /**
     * Parse a section ID into its structured components. Handles both derived
     * IDs ({@code source~type=T~slug=S}) and flat/legacy IDs ({@code content-rail}).
     *
     * <p>For flat IDs, {@code source} is the full string, and {@code type}/
     * {@code slug} are null. Callers check {@link Parsed#isDerived()} to
     * distinguish the two cases.
     */
    public static Parsed parse(String sectionId) {
        String[] segments = sectionId.split(SEPARATOR);
        String source = segments[0];
        Map<String, String> fields = new HashMap<>();

        for (int i = 1; i < segments.length; i++) {
            int eq = segments[i].indexOf('=');
            if (eq > 0) {
                fields.put(segments[i].substring(0, eq), segments[i].substring(eq + 1));
            }
        }

        return new Parsed(
                source,
                fields.get(TYPE_KEY),
                fields.get(SLUG_KEY),
                fields
        );
    }

    // ── Convenience shortcuts (delegate to parse) ──────────────────────

    /** Extract the slug, or return the raw ID if no slug field exists. */
    public static String extractSlug(String sectionId) {
        Parsed p = parse(sectionId);
        return p.slug() != null ? p.slug() : sectionId;
    }

    /** Extract the content-source ID (the segment before the first {@code ~}). */
    public static String extractSource(String sectionId) {
        return parse(sectionId).source();
    }

    /** Returns true if the ID uses the derived format (contains a {@code type} field). */
    public static boolean isDerived(String sectionId) {
        return sectionId.contains(SEPARATOR + TYPE_KEY + "=");
    }
}
