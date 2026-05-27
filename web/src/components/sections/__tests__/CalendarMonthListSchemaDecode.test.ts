import { describe, expect, it } from 'vitest';
import type { Section } from '@sdui/models';
import { mapCalendarMonthList } from '../../../adapters/sectionUiAdapters';

describe('CalendarMonthList schema decode contract', () => {
  it('decodes section data with metadata map', () => {
    const section = {
      id: 'server:calendar~type=CalendarMonthList',
      type: 'CalendarMonthList',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        minDate: '2025-10-01',
        maxDate: '2026-06-30',
        dateMetadata: {
          '2026-05-25': { gameCount: 3, hasTeamGame: true },
          '2026-05-26': { gameCount: 0, hasTeamGame: false },
        },
        onDateSelected: {
          trigger: 'onActivate',
          type: 'navigate',
          targetUri: 'nba://games?date={{games_selected_date}}',
        },
      },
    } as Section;

    const model = mapCalendarMonthList(section);
    expect(model).not.toBeNull();
    expect(model?.stateKey).toBe('games_selected_date');
    expect(model?.selectedDate).toBe('2026-05-25');
    expect(model?.defaultDate).toBe('2026-05-25');
    expect(model?.minDate).toBe('2025-10-01');
    expect(model?.maxDate).toBe('2026-06-30');
    expect(model?.dateMetadata['2026-05-25']?.gameCount).toBe(3);
    expect(model?.dateMetadata['2026-05-25']?.hasTeamGame).toBe(true);
    expect(model?.onDateSelected.type).toBe('navigate');
  });

  it('fails decode when onDateSelected is missing', () => {
    const section = {
      id: 'server:calendar~type=CalendarMonthList',
      type: 'CalendarMonthList',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
      },
    } as Section;

    expect(mapCalendarMonthList(section)).toBeNull();
  });
});
