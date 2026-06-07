package com.nba.sdui.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Derives stable section IDs from content-source metadata.
 *
 * <p>IDs use a self-describing, BEM-flavored key-value format:
 * <pre>
 *   &lt;source&gt;__type-&lt;Type&gt;[__slug-&lt;slug&gt;][__&lt;key&gt;-&lt;value&gt;]*
 *
 *   stats-api-game-0022500123__type-AtomicComposite__slug-scoreboard
 *   feed-home__type-AtomicComposite__slug-row1
 * </pre>
 *
 * <p>The first segment is the sanitized content-source identifier (e.g.
 * {@code feed-home}). Subsequent segments are {@code key-value} pairs joined
 * by {@code __}, where the key is the substring up to the first {@code -}
 * and the value is everything after it (so slug values may themselves
 * contain {@code -}).
 *
 * <h2>Why this format</h2>
 *
 * <p>Section IDs are pasted unescaped into many environments:
 * <ul>
 *   <li>CSS class names ({@code .my-id}) and selectors</li>
 *   <li>{@code querySelector} / {@code data-*} attribute values</li>
 *   <li>URL path segments (e.g. {@code /v1/sdui/section/&lt;id&gt;})</li>
 *   <li>React/SwiftUI/Compose list keys</li>
 *   <li>Log lines and metric labels</li>
 * </ul>
 *
 * <p>The intersection of "safe everywhere without escaping" is roughly
 * {@code [A-Za-z0-9_-]}. Earlier formats used {@code ~}, {@code :}, and
 * {@code =}; each of those is a special character in CSS selectors
 * ({@code ~} = sibling combinator, {@code :} = pseudo-class prefix,
 * {@code =} = only valid inside {@code [attr=...]}). The breakage was
 * silent — selectors simply failed to match and responsive layout rules
 * stopped firing.
 *
 * <p>BEM's {@code block__element-modifier} convention is the
 * battle-tested way to encode structured metadata into a single
 * identifier using only safe characters. {@code __} separates segments,
 * {@code -} binds key and value, and the result is readable in logs,
 * usable in CSS, and never needs percent-encoding.
 *
 * <h2>Source sanitization</h2>
 *
 * <p>Callers may pass legacy-style source identifiers like
 * {@code feed:home} or {@code stats-api:game-0022500123}. The deriver
 * normalizes any character outside {@code [A-Za-z0-9-]} to {@code -}
 * and collapses runs of {@code -} so the resulting id stays in the safe
 * character set. Sanitization is idempotent.
 *
 * <h2>Stability</h2>
 *
 * <p>Position is never part of the ID. IDs are stable across reorders,
 * ad-slot insertions, and A/B test variants.
 *
 * <p>Feed pages (For You, Watch, Home): each content item has a unique
 * content-source ID, so {@code source__type-...} is sufficient.
 *
 * <p>Named-region pages (Game Detail, Boxscore): the same content source
 * produces multiple sections of the same type — a slug disambiguates.
 */
public final class SectionIdDeriver {

    static final String SEPARATOR = "__";
    static final String KV_SEPARATOR = "-";
    static final String TYPE_KEY = "type";
    static final String SLUG_KEY = "slug";

    /**
     * Slugs and key-value values must be lowerCamelCase. Allowing a {@code -}
     * inside the value would collide with {@link #KV_SEPARATOR}; allowing
     * other punctuation would re-open the CSS/URL-escaping cans the format
     * is designed to keep closed.
     */
    private static final java.util.regex.Pattern CAMEL_CASE =
            java.util.regex.Pattern.compile("^[a-z][a-zA-Z0-9]*$");

    private SectionIdDeriver() {}

    /**
     * Structured parse result of a derived section ID. Provides direct access
     * to each component by name — no substring scanning at call sites.
     *
     * @param source the sanitized content-source ID (e.g.
     *               {@code "stats-api-game-0022500123"})
     * @param type   the section type (e.g. {@code "AtomicComposite"}), or
     *               {@code null} for flat/legacy IDs
     * @param slug   the section slug (e.g. {@code "scoreboard"}), or
     *               {@code null} if absent
     * @param fields all key-value fields (for forward-compatible extension)
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
        return sanitizeSource(contentSourceId)
                + SEPARATOR + TYPE_KEY + KV_SEPARATOR + sectionType;
    }

    /**
     * Named-region form: slug disambiguates when the same source + type repeats.
     *
     * <p>{@code slug} must be lowerCamelCase ({@code ^[a-z][a-zA-Z0-9]*$}).
     * A {@code -} inside the slug would collide with the key/value
     * separator and silently corrupt downstream parses.
     */
    public static String derive(String contentSourceId, String sectionType, String slug) {
        requireCamelCase(slug, "slug");
        return sanitizeSource(contentSourceId)
                + SEPARATOR + TYPE_KEY + KV_SEPARATOR + sectionType
                + SEPARATOR + SLUG_KEY + KV_SEPARATOR + slug;
    }

    /**
     * Throws {@link IllegalArgumentException} when {@code value} is not
     * lowerCamelCase. Centralized so every entry point validates the same way.
     */
    static void requireCamelCase(String value, String label) {
        if (value == null || !CAMEL_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "SectionIdDeriver: " + label + " must be lowerCamelCase ([a-z][a-zA-Z0-9]*), got: "
                            + (value == null ? "null" : "'" + value + "'"));
        }
    }

    /**
     * Returns the section-id prefix for a given content source — useful for
     * {@code SectionRefreshService.registerResolver(prefix, ...)} calls that
     * need to match any section emitted from a particular source without
     * hard-coding the post-sanitization form.
     *
     * <p>Returns the sanitized source verbatim (no trailing separator) so
     * callers can register a true {@code startsWith} prefix that matches
     * the {@code <sanitized-source>__type-…} form. Pass a source that
     * already carries the desired prefix boundary (e.g. {@code "stats-api:game-"}
     * → {@code "stats-api-game-"}) so prefix matches do not bleed into
     * unrelated sources.
     */
    public static String prefixFor(String contentSourceId) {
        return sanitizeSource(contentSourceId);
    }

    /**
     * Normalize a content-source identifier into the safe character set
     * ({@code [A-Za-z0-9-]}). Any other character becomes {@code -}, and
     * runs of {@code -} collapse to a single {@code -}. Trailing and
     * leading {@code -} are trimmed.
     *
     * <p>Examples:
     * <pre>
     *   feed:home              -> feed-home
     *   stats-api:game-12      -> stats-api-game-12
     *   ads:gam-mid_feed_1     -> ads-gam-mid-feed-1
     * </pre>
     */
    public static String sanitizeSource(String contentSourceId) {
        if (contentSourceId == null || contentSourceId.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(contentSourceId.length());
        boolean prevDash = false;
        for (int i = 0; i < contentSourceId.length(); i++) {
            char c = contentSourceId.charAt(i);
            boolean safe = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '-';
            if (safe && c != '-') {
                sb.append(c);
                prevDash = false;
            } else {
                if (!prevDash && sb.length() > 0) sb.append('-');
                prevDash = true;
            }
        }
        // Note: we deliberately do NOT strip a trailing dash — callers may
        // pass a prefix form like "stats-api:game-" whose trailing dash is
        // semantically meaningful (it separates the entity-kind segment
        // from the per-entity suffix). Stripping it would make
        // {@link #prefixFor} produce prefixes that do not match the
        // section IDs derived from concrete source variants.
        return sb.toString();
    }

    // ── Parsing ────────────────────────────────────────────────────────

    /**
     * Parse a section ID into its structured components. Handles both derived
     * IDs ({@code source__type-T__slug-S}) and flat/legacy IDs
     * ({@code content-rail}).
     *
     * <p>For flat IDs, {@code source} is the full string, and
     * {@code type}/{@code slug} are {@code null}. Callers check
     * {@link Parsed#isDerived()} to distinguish the two cases.
     */
    public static Parsed parse(String sectionId) {
        String[] segments = sectionId.split(SEPARATOR);
        String source = segments[0];
        Map<String, String> fields = new HashMap<>();

        for (int i = 1; i < segments.length; i++) {
            int dash = segments[i].indexOf(KV_SEPARATOR);
            if (dash > 0) {
                fields.put(segments[i].substring(0, dash), segments[i].substring(dash + 1));
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

    /**
     * Extract the content-source ID (the segment before the first
     * {@link #SEPARATOR}).
     */
    public static String extractSource(String sectionId) {
        return parse(sectionId).source();
    }

    /** Returns true if the ID uses the derived format (contains a {@code type} field). */
    public static boolean isDerived(String sectionId) {
        return sectionId.contains(SEPARATOR + TYPE_KEY + KV_SEPARATOR);
    }

    /**
     * Returns true if {@code sectionId} ends with the slug segment
     * {@code __slug-<slug>}. Convenient for "is this the app-bar section?"
     * style checks without sprinkling separator literals across the codebase.
     */
    public static boolean endsWithSlug(String sectionId, String slug) {
        if (sectionId == null || slug == null) return false;
        return sectionId.endsWith(SEPARATOR + SLUG_KEY + KV_SEPARATOR + slug);
    }
}
