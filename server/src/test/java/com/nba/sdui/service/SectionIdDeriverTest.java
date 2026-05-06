package com.nba.sdui.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link SectionIdDeriver} — validates the two derivation forms
 * (feed-page and named-region) produce stable, position-free IDs.
 */
class SectionIdDeriverTest {

    @Test
    void derive_feedPage_contentSourceIdAndType() {
        String result = SectionIdDeriver.derive("cms:article-42", "AtomicComposite");
        assertEquals("cms:article-42::AtomicComposite", result);
    }

    @Test
    void derive_feedPage_recommendationSource() {
        String result = SectionIdDeriver.derive("rec:game-lal-bos-20260501", "AtomicComposite");
        assertEquals("rec:game-lal-bos-20260501::AtomicComposite", result);
    }

    @Test
    void derive_namedRegion_withSlug() {
        String result = SectionIdDeriver.derive("stats-api:game-0022500123", "AtomicComposite", "hero-card");
        assertEquals("stats-api:game-0022500123::AtomicComposite::hero-card", result);
    }

    @Test
    void derive_namedRegion_boxscoreTable() {
        String result = SectionIdDeriver.derive("stats-api:game-0022500123", "BoxscoreTable", "home");
        assertEquals("stats-api:game-0022500123::BoxscoreTable::home", result);
    }

    @Test
    void derive_adSlot() {
        String result = SectionIdDeriver.derive("ads:gam-mid_feed_1", "AdSlot");
        assertEquals("ads:gam-mid_feed_1::AdSlot", result);
    }

    @Test
    void derive_feedSource_noPositionalComponent() {
        // Two different content sources should produce different IDs even for the same type
        String id1 = SectionIdDeriver.derive("cms:article-1", "AtomicComposite");
        String id2 = SectionIdDeriver.derive("cms:article-2", "AtomicComposite");
        assertEquals("cms:article-1::AtomicComposite", id1);
        assertEquals("cms:article-2::AtomicComposite", id2);
    }

    // ── Entity-identifier patterns ─────────────────────────────────────

    @Test
    void derive_teamEntity() {
        String result = SectionIdDeriver.derive("stats-api:team-1610612738", "AtomicComposite", "roster");
        assertEquals("stats-api:team-1610612738::AtomicComposite::roster", result);
    }

    @Test
    void derive_playerEntity() {
        String result = SectionIdDeriver.derive("stats-api:player-201566", "AtomicComposite", "career-stats");
        assertEquals("stats-api:player-201566::AtomicComposite::career-stats", result);
    }

    @Test
    void derive_capiPostById() {
        String result = SectionIdDeriver.derive("capi:post-12345", "AtomicComposite");
        assertEquals("capi:post-12345::AtomicComposite", result);
    }

    @Test
    void derive_capiPostBySlug() {
        String result = SectionIdDeriver.derive("capi:slug-five-things-to-watch", "AtomicComposite");
        assertEquals("capi:slug-five-things-to-watch::AtomicComposite", result);
    }

    @Test
    void derive_capiCategory() {
        String result = SectionIdDeriver.derive("capi:category-playoffs", "AtomicComposite");
        assertEquals("capi:category-playoffs::AtomicComposite", result);
    }

    @Test
    void derive_gameEntityMultipleSections() {
        // Same game produces multiple sections — slugs disambiguate
        String videoPlayer = SectionIdDeriver.derive("stats-api:game-0022400777", "VideoPlayer");
        String boxscore = SectionIdDeriver.derive("stats-api:game-0022400777", "BoxscoreTable", "home");
        String topPerformers = SectionIdDeriver.derive("stats-api:game-0022400777", "AtomicComposite", "top-performers");

        assertEquals("stats-api:game-0022400777::VideoPlayer", videoPlayer);
        assertEquals("stats-api:game-0022400777::BoxscoreTable::home", boxscore);
        assertEquals("stats-api:game-0022400777::AtomicComposite::top-performers", topPerformers);

        // All are unique
        assertNotEquals(videoPlayer, boxscore);
        assertNotEquals(boxscore, topPerformers);
    }
}
