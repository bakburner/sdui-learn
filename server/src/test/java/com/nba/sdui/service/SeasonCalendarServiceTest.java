package com.nba.sdui.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonCalendarServiceTest {

    @Test
    void currentLeagueDate_returnsEtDateRegardlessOfUtcClock() {
        SeasonCalendarService service = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-27T03:30:00Z"), ZoneOffset.UTC)
        );

        assertEquals(LocalDate.parse("2026-05-26"), service.currentLeagueDate());
    }

    @Test
    void currentLeagueDate_returnsSameEtDateAcrossNoonUtc() {
        SeasonCalendarService service = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T18:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals(LocalDate.parse("2026-05-26"), service.currentLeagueDate());
    }

    @Test
    void seasonStartAndEndAreStableConstants() {
        SeasonCalendarService service = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T18:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals(LocalDate.parse("2025-10-01"), service.seasonStart());
        assertEquals(LocalDate.parse("2026-06-30"), service.seasonEnd());
    }

    @Test
    void isInSeason_returnsTrueForBoundaryDates() {
        SeasonCalendarService service = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T18:00:00Z"), ZoneOffset.UTC)
        );

        assertTrue(service.isInSeason(service.seasonStart()));
        assertTrue(service.isInSeason(service.seasonEnd()));
        assertFalse(service.isInSeason(service.seasonStart().minusDays(1)));
        assertFalse(service.isInSeason(service.seasonEnd().plusDays(1)));
    }

    @Test
    void leagueZone_isAmericaNewYork() {
        SeasonCalendarService service = new SeasonCalendarService(
                Clock.fixed(Instant.parse("2026-05-26T18:00:00Z"), ZoneOffset.UTC)
        );

        assertEquals(ZoneId.of("America/New_York"), service.leagueZone());
    }

    @Test
    void defaultConstructor_usesSystemClock() {
        SeasonCalendarService service = new SeasonCalendarService();
        ZoneId et = ZoneId.of("America/New_York");

        LocalDate expectedNow = LocalDate.now(et);
        LocalDate actualFirst = service.currentLeagueDate();
        if (actualFirst.equals(expectedNow)) {
            return;
        }

        // Tiny race guard around ET midnight.
        LocalDate expectedRetryNow = LocalDate.now(et);
        LocalDate actualSecond = service.currentLeagueDate();
        assertTrue(
                actualSecond.equals(expectedNow)
                        || actualSecond.equals(expectedRetryNow)
                        || actualSecond.equals(expectedRetryNow.minusDays(1))
                        || actualSecond.equals(expectedRetryNow.plusDays(1))
        );
    }
}
