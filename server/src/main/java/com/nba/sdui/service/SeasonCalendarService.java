package com.nba.sdui.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * ET-authoritative season calendar for the NBA league date.
 *
 * <p>The NBA league operates on Eastern Time — all game dates, schedule boundaries,
 * and "today" references throughout SDUI composition use ET as the source of truth.
 * {@link #currentLeagueDate()} returns today's date in ET, which may differ from UTC
 * after midnight ET (i.e. between 00:00–04:00/05:00 UTC depending on DST).
 *
 * <p>Season bounds ({@code SEASON_START}, {@code SEASON_END}) are server-internal
 * constants today. They may move to Spring config properties or a remote source
 * in a future iteration — callers should use the accessors rather than importing
 * the constants directly.
 *
 * <p><strong>Future enhancement:</strong> {@link #currentLeagueDate()} returns the
 * literal ET date today. During the offseason the league calendar may need an
 * "anchor day" computation that returns a future date (e.g. opening night) so the
 * Games screen has meaningful content. That enhancement will replace this method's
 * body with a league-calendar-aware computation once a signal source for the
 * anchor day exists.
 */
@Service
public class SeasonCalendarService {

    private static final ZoneId LEAGUE_ZONE = ZoneId.of("America/New_York");
    private static final LocalDate SEASON_START = LocalDate.parse("2025-10-01");
    private static final LocalDate SEASON_END = LocalDate.parse("2026-06-30");

    private final Clock clock;

    @Autowired
    public SeasonCalendarService() {
        this(Clock.systemUTC());
    }

    SeasonCalendarService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Today's date in Eastern Time — the authoritative "league date".
     *
     * <p>TODO: replace with a league-calendar-aware anchor-day computation
     * during the offseason once a signal source exists.
     */
    public LocalDate currentLeagueDate() {
        return LocalDate.now(clock.withZone(LEAGUE_ZONE));
    }

    public LocalDate seasonStart() {
        return SEASON_START;
    }

    public LocalDate seasonEnd() {
        return SEASON_END;
    }

    /**
     * Whether {@code date} falls within the current season window (inclusive).
     */
    public boolean isInSeason(LocalDate date) {
        return !date.isBefore(SEASON_START) && !date.isAfter(SEASON_END);
    }

    public ZoneId leagueZone() {
        return LEAGUE_ZONE;
    }
}
