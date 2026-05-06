package com.nba.sdui.service;

/**
 * Derives stable section IDs from content-source metadata.
 *
 * <p>Feed pages (For You, Watch, Home): each content item has a unique
 * content-source ID, so {@code contentSourceId + sectionType} is sufficient.
 *
 * <p>Named-region pages (Game Detail, Boxscore): the same content source
 * produces multiple sections of the same type — a slug disambiguates.
 *
 * <p>Position is never part of the ID. IDs are stable across reorders,
 * ad-slot insertions, and A/B test variants.
 */
public final class SectionIdDeriver {

    private static final String SEPARATOR = "::";

    private SectionIdDeriver() {}

    /**
     * Feed-page form: contentSourceId + sectionType is unique.
     *
     * @param contentSourceId origin identifier (e.g. "cms:article-42", "rec:game-lal-bos-20260501")
     * @param sectionType     section type enum value (e.g. "AtomicComposite")
     * @return derived section ID
     */
    public static String derive(String contentSourceId, String sectionType) {
        return contentSourceId + SEPARATOR + sectionType;
    }

    /**
     * Named-region form: slug disambiguates when the same source + type repeats.
     *
     * @param contentSourceId origin identifier (e.g. "stats-api:game-0022500123")
     * @param sectionType     section type enum value
     * @param slug            stable content-internal role name (e.g. "hero-card", "top-performers")
     * @return derived section ID
     */
    public static String derive(String contentSourceId, String sectionType, String slug) {
        return contentSourceId + SEPARATOR + sectionType + SEPARATOR + slug;
    }
}
