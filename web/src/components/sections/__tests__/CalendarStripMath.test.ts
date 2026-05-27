import { describe, expect, it } from 'vitest';
import { Temporal } from '@js-temporal/polyfill';
import { dayColumn, generateWeeks, startOfWeek } from '../CalendarStrip';

describe('CalendarStrip math helpers', () => {
  it('startOfWeek aligns to locale first-day index', () => {
    const date = Temporal.PlainDate.from('2026-05-26'); // Tuesday

    expect(startOfWeek(date, 1).toString()).toBe('2026-05-25'); // Monday
    expect(startOfWeek(date, 7).toString()).toBe('2026-05-24'); // Sunday
  });

  it('dayColumn rotates with first day of week', () => {
    const date = Temporal.PlainDate.from('2026-05-26'); // Tuesday (2)
    expect(dayColumn(date, 1)).toBe(1); // Mon-first
    expect(dayColumn(date, 7)).toBe(2); // Sun-first
  });

  it('generateWeeks clamps days outside min and max bounds', () => {
    const min = Temporal.PlainDate.from('2026-05-27');
    const max = Temporal.PlainDate.from('2026-05-30');

    const weeks = generateWeeks(min, max, 1);

    expect(weeks.length).toBe(1);
    expect(weeks[0].days.map((d) => d.toString())).toEqual([
      '2026-05-27',
      '2026-05-28',
      '2026-05-29',
      '2026-05-30',
    ]);
  });

  it('generateWeeks includes leap day', () => {
    const min = Temporal.PlainDate.from('2024-02-28');
    const max = Temporal.PlainDate.from('2024-03-01');

    const weeks = generateWeeks(min, max, 1);
    const allDays = weeks.flatMap((w) => w.days.map((d) => d.toString()));

    expect(allDays).toContain('2024-02-29');
    expect(allDays).toContain('2024-03-01');
  });

  it('preserves ISO identity with Temporal PlainDate', () => {
    expect(Temporal.PlainDate.from('2026-05-26').toString()).toBe('2026-05-26');
    expect(
      Temporal.PlainDate.from('2026-05-26').add({ days: 7 }).toString()
    ).toBe('2026-06-02');
  });
});
