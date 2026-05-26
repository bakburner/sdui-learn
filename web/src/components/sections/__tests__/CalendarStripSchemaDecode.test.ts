import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { Section } from '@sdui/models';
import { mapCalendarStrip } from '../../../adapters/sectionUiAdapters';

function loadFixtureSection(): Section {
  const fixturePath = resolve(__dirname, '../../../../../schema/examples/calendar-strip.json');
  return JSON.parse(readFileSync(fixturePath, 'utf-8')) as Section;
}

describe('CalendarStrip schema decode contract', () => {
  it('decodes fixture with all expected fields', () => {
    const section = loadFixtureSection();
    const model = mapCalendarStrip(section);

    expect(section.id).toBe('server:games-calendar~type=CalendarStrip');
    expect(section.type).toBe('CalendarStrip');
    expect(section.contentSourceId).toBe('server:games-calendar');
    expect(section.analyticsId).toBe('games_calendar_strip');
    expect(section.accessibility?.label).toBe('Games date picker');

    expect(model).not.toBeNull();
    expect(model?.stateKey).toBe('games_selected_date');
    expect(model?.selectedDate).toBe('2026-05-25');
    expect(model?.defaultDate).toBe('2026-05-25');
    expect(model?.minDate).toBe('2025-10-01');
    expect(model?.maxDate).toBe('2026-06-30');
    expect(model?.onDateSelected.trigger).toBe('onActivate');
    expect(model?.onDateSelected.type).toBe('refresh');
    expect(model?.onDateSelected.endpoint).toBe('/v1/sdui/screen/refresh/games');
    expect(model?.onDateSelected.paramBindings?.date).toBe('{{games_selected_date}}');
  });

  it('fails decode when defaultDate is omitted', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        onDateSelected: {
          trigger: 'onActivate',
          type: 'refresh',
          endpoint: '/v1/sdui/screen/refresh/games',
          paramBindings: { date: '{{games_selected_date}}' },
        },
      },
    } as Section;

    expect(mapCalendarStrip(section)).toBeNull();
  });

  it('succeeds decode when minDate and maxDate are omitted', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        onDateSelected: {
          trigger: 'onActivate',
          type: 'refresh',
          endpoint: '/v1/sdui/screen/refresh/games',
          paramBindings: { date: '{{games_selected_date}}' },
        },
      },
    } as Section;

    const model = mapCalendarStrip(section);
    expect(model).not.toBeNull();
    expect(model?.minDate).toBeUndefined();
    expect(model?.maxDate).toBeUndefined();
  });

  it('fails decode when onDateSelected is an array', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        onDateSelected: [
          {
            trigger: 'onActivate',
            type: 'refresh',
            endpoint: '/v1/sdui/screen/refresh/games',
            paramBindings: { date: '{{games_selected_date}}' },
          },
        ],
      },
    } as unknown as Section;

    expect(mapCalendarStrip(section)).toBeNull();
  });
});
