package com.nba.sdui.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.nba.sdui.domain.SectionIdDeriver;

/**
 * Unit tests for {@link SectionIdDeriver} — validates the BEM-classic
 * wire format ({@code <sanitized-source>__type-<Type>[__slug-<camelCaseSlug>]})
 * and strict slug validation.
 */
class SectionIdDeriverTest {

    @Test
    void derive_feedPage_contentSourceIdAndType() {
        String result = SectionIdDeriver.derive("cms:article-42", "AtomicComposite");
        assertEquals("cms-article-42__type-AtomicComposite", result);
    }

    @Test
    void derive_feedPage_recommendationSource() {
        String result = SectionIdDeriver.derive("rec:game-lal-bos-20260501", "AtomicComposite");
        assertEquals("rec-game-lal-bos-20260501__type-AtomicComposite", result);
    }

    @Test
    void derive_namedRegion_withSlug() {
        String result = SectionIdDeriver.derive("stats-api:game-0022500123", "AtomicComposite", "heroCard");
        assertEquals("stats-api-game-0022500123__type-AtomicComposite__slug-heroCard", result);
    }

    @Test
    void derive_namedRegion_boxscoreTable() {
        String result = SectionIdDeriver.derive("stats-api:game-0022500123", "BoxscoreTable", "home");
        assertEquals("stats-api-game-0022500123__type-BoxscoreTable__slug-home", result);
    }

    @Test
    void derive_adSlot_underscoresInSourceCollapseToDashes() {
        String result = SectionIdDeriver.derive("ads:gam-mid_feed_1", "AdSlot");
        assertEquals("ads-gam-mid-feed-1__type-AdSlot", result);
    }

    @Test
    void derive_calendarStrip_serverCalendarSource() {
        String result = SectionIdDeriver.derive("server:games-calendar", "CalendarStrip");
        assertEquals("server-games-calendar__type-CalendarStrip", result);
    }

    @Test
    void derive_feedSource_noPositionalComponent() {
        String id1 = SectionIdDeriver.derive("cms:article-1", "AtomicComposite");
        String id2 = SectionIdDeriver.derive("cms:article-2", "AtomicComposite");
        assertEquals("cms-article-1__type-AtomicComposite", id1);
        assertEquals("cms-article-2__type-AtomicComposite", id2);
    }

    // ── Slug validation ────────────────────────────────────────────────

    @Test
    void derive_rejectsKebabSlug() {
        assertThrows(IllegalArgumentException.class,
                () -> SectionIdDeriver.derive("cms:home", "AtomicComposite", "hero-card"));
    }

    @Test
    void derive_rejectsUpperCaseLeadingSlug() {
        assertThrows(IllegalArgumentException.class,
                () -> SectionIdDeriver.derive("cms:home", "AtomicComposite", "HeroCard"));
    }

    @Test
    void derive_rejectsUnderscoreSlug() {
        assertThrows(IllegalArgumentException.class,
                () -> SectionIdDeriver.derive("cms:home", "AtomicComposite", "hero_card"));
    }

    @Test
    void derive_acceptsAllLowerSlug() {
        String result = SectionIdDeriver.derive("cms:home", "AtomicComposite", "scoreboard");
        assertEquals("cms-home__type-AtomicComposite__slug-scoreboard", result);
    }

    // ── Entity-identifier patterns ─────────────────────────────────────

    @Test
    void derive_teamEntity() {
        String result = SectionIdDeriver.derive("stats-api:team-1610612738", "AtomicComposite", "roster");
        assertEquals("stats-api-team-1610612738__type-AtomicComposite__slug-roster", result);
    }

    @Test
    void derive_playerEntity() {
        String result = SectionIdDeriver.derive("stats-api:player-201566", "AtomicComposite", "careerStats");
        assertEquals("stats-api-player-201566__type-AtomicComposite__slug-careerStats", result);
    }

    @Test
    void derive_capiPostById() {
        String result = SectionIdDeriver.derive("capi:post-12345", "AtomicComposite");
        assertEquals("capi-post-12345__type-AtomicComposite", result);
    }

    @Test
    void derive_capiPostBySlug() {
        String result = SectionIdDeriver.derive("capi:slug-five-things-to-watch", "AtomicComposite");
        assertEquals("capi-slug-five-things-to-watch__type-AtomicComposite", result);
    }

    @Test
    void derive_capiCategory() {
        String result = SectionIdDeriver.derive("capi:category-playoffs", "AtomicComposite");
        assertEquals("capi-category-playoffs__type-AtomicComposite", result);
    }

    @Test
    void derive_gameEntityMultipleSections() {
        String videoPlayer = SectionIdDeriver.derive("stats-api:game-0022400777", "VideoPlayer");
        String boxscore = SectionIdDeriver.derive("stats-api:game-0022400777", "BoxscoreTable", "home");
        String topPerformers = SectionIdDeriver.derive("stats-api:game-0022400777", "AtomicComposite", "topPerformers");

        assertEquals("stats-api-game-0022400777__type-VideoPlayer", videoPlayer);
        assertEquals("stats-api-game-0022400777__type-BoxscoreTable__slug-home", boxscore);
        assertEquals("stats-api-game-0022400777__type-AtomicComposite__slug-topPerformers", topPerformers);

        assertNotEquals(videoPlayer, boxscore);
        assertNotEquals(boxscore, topPerformers);
    }

    // ── prefixFor ─────────────────────────────────────────────────────

    @Test
    void prefixFor_returnsSanitizedSource() {
        assertEquals("stats-api-game-", SectionIdDeriver.prefixFor("stats-api:game-"));
        assertEquals("feed-home", SectionIdDeriver.prefixFor("feed:home"));
    }

    // ── Parsed record ────────────────────────────────────────────────

    @Test
    void parse_derivedWithSlug() {
        var p = SectionIdDeriver.parse("stats-api-game-123__type-AtomicComposite__slug-scoreboard");
        assertTrue(p.isDerived());
        assertEquals("stats-api-game-123", p.source());
        assertEquals("AtomicComposite", p.type());
        assertEquals("scoreboard", p.slug());
    }

    @Test
    void parse_derivedWithoutSlug() {
        var p = SectionIdDeriver.parse("cms-article-42__type-AtomicComposite");
        assertTrue(p.isDerived());
        assertEquals("cms-article-42", p.source());
        assertEquals("AtomicComposite", p.type());
        assertNull(p.slug());
    }

    @Test
    void parse_flatId() {
        var p = SectionIdDeriver.parse("content-rail");
        assertFalse(p.isDerived());
        assertEquals("content-rail", p.source());
        assertNull(p.type());
        assertNull(p.slug());
    }

    @Test
    void parse_fieldsAreExtensible() {
        var p = SectionIdDeriver.parse("src-x__type-T__slug-s__version-2");
        assertEquals("2", p.fields().get("version"));
        assertEquals("T", p.type());
        assertEquals("s", p.slug());
    }

    // ── Convenience shortcuts ─────────────────────────────────────────

    @Test
    void extractSlug_fromDerivedId() {
        assertEquals("scoreboard",
                SectionIdDeriver.extractSlug("stats-api-game-123__type-AtomicComposite__slug-scoreboard"));
    }

    @Test
    void extractSlug_fromFeedId_noSlug_returnsFull() {
        assertEquals("cms-article-42__type-AtomicComposite",
                SectionIdDeriver.extractSlug("cms-article-42__type-AtomicComposite"));
    }

    @Test
    void extractSlug_fromFlatId() {
        assertEquals("content-rail", SectionIdDeriver.extractSlug("content-rail"));
    }

    @Test
    void extractSource_fromDerivedId() {
        assertEquals("stats-api-game-123",
                SectionIdDeriver.extractSource("stats-api-game-123__type-AtomicComposite__slug-scoreboard"));
    }

    @Test
    void extractSource_fromFlatId() {
        assertEquals("content-rail", SectionIdDeriver.extractSource("content-rail"));
    }

    @Test
    void isDerived_trueForDerivedIds() {
        assertTrue(SectionIdDeriver.isDerived("stats-api-game-123__type-AtomicComposite__slug-scoreboard"));
        assertTrue(SectionIdDeriver.isDerived("cms-article-42__type-AtomicComposite"));
    }

    @Test
    void isDerived_falseForFlatIds() {
        assertFalse(SectionIdDeriver.isDerived("content-rail"));
        assertFalse(SectionIdDeriver.isDerived("game-tabs"));
    }

    @Test
    void endsWithSlug_matchesSlugSegment() {
        assertTrue(SectionIdDeriver.endsWithSlug(
                "feed-home__type-AtomicComposite__slug-appBar", "appBar"));
        assertFalse(SectionIdDeriver.endsWithSlug(
                "feed-home__type-AtomicComposite__slug-appBar", "scoreboard"));
        assertFalse(SectionIdDeriver.endsWithSlug("content-rail", "appBar"));
    }
}
