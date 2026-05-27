import { describe, expect, it } from 'vitest';
import type { Section } from '@sdui/models';
import { ActionTrigger, ActionType } from '@sdui/models';
import { mapCalendarMonthList, mapCalendarStrip, mapTabGroup } from './sectionUiAdapters';

function tabGroupSection(overrides: {
  stateKey?: string;
  defaultTab?: string;
  tabs?: Array<{ id: string; label: string; stateValue?: string }>;
}): Section {
  const tabs = overrides.tabs ?? [
    { id: 'tab-a', label: 'A', stateValue: 'a' },
    { id: 'tab-b', label: 'B', stateValue: 'b' },
  ];
  return {
    id: 'tg-1',
    type: 'TabGroup',
    data: {
      stateKey: overrides.stateKey,
      defaultTab: overrides.defaultTab,
      tabs,
      tabContents: {
        a: [{ id: 'child-a', type: 'AtomicComposite', children: [] }],
        b: [{ id: 'child-b', type: 'AtomicComposite', children: [] }],
      },
    },
  } as Section;
}

describe('mapTabGroup', () => {
  it('returns null when stateKey is omitted', () => {
    expect(mapTabGroup(tabGroupSection({}), {})).toBeNull();
  });

  it('does not invent a stateKey from section id', () => {
    const section = tabGroupSection({});
    expect(section.data?.stateKey).toBeUndefined();
    expect(mapTabGroup(section, {})).toBeNull();
  });

  it('resolves active tab from screen state using stateValue keys', () => {
    const model = mapTabGroup(
      tabGroupSection({ stateKey: 'watch_active_tab', defaultTab: 'a' }),
      { watch_active_tab: 'b' },
    );
    expect(model?.stateKey).toBe('watch_active_tab');
    expect(model?.tabs.find((t) => t.isActive)?.stateValue).toBe('b');
    expect(model?.activeSections[0]?.id).toBe('child-b');
  });
});

describe('mapCalendarStrip', () => {
  it('maps all required and optional fields', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        minDate: '2025-10-01',
        maxDate: '2026-06-30',
        onDateSelected: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Refresh,
          endpoint: '/v1/sdui/screen/games',
          paramBindings: { date: '{{games_selected_date}}' },
        },
      },
    } as Section;

    const model = mapCalendarStrip(section);
    expect(model).not.toBeNull();
    expect(model?.stateKey).toBe('games_selected_date');
    expect(model?.selectedDate).toBe('2026-05-25');
    expect(model?.defaultDate).toBe('2026-05-25');
    expect(model?.minDate).toBe('2025-10-01');
    expect(model?.maxDate).toBe('2026-06-30');
    expect(model?.onDateSelected.type).toBe(ActionType.Refresh);
    expect(model?.onDateSelected.trigger).toBe(ActionTrigger.OnActivate);
    expect(model?.onDateSelected.endpoint).toBe('/v1/sdui/screen/games');
  });

  it('returns null when required field is missing', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        onDateSelected: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Refresh,
          endpoint: '/v1/sdui/screen/games',
        },
      },
    } as Section;

    expect(mapCalendarStrip(section)).toBeNull();
  });

  it('maps expandedAction when present', () => {
    const section = {
      id: 'server:games-calendar~type=CalendarStrip',
      type: 'CalendarStrip',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        expandedAction: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Navigate,
          targetUri: 'nba://calendar',
        },
        onDateSelected: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Refresh,
          endpoint: '/v1/sdui/screen/games',
        },
      },
    } as Section;

    const model = mapCalendarStrip(section);
    expect(model?.expandedAction?.type).toBe(ActionType.Navigate);
    expect(model?.expandedAction?.targetUri).toBe('nba://calendar');
  });
});

describe('mapCalendarMonthList', () => {
  it('maps required and optional fields', () => {
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
          '2026-05-26': { gameCount: 1, hasTeamGame: false },
        },
        onDateSelected: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Navigate,
          targetUri: 'nba://games?date={{games_selected_date}}',
        },
      },
    } as Section;

    const model = mapCalendarMonthList(section);
    expect(model).not.toBeNull();
    expect(model?.stateKey).toBe('games_selected_date');
    expect(model?.dateMetadata['2026-05-25']?.gameCount).toBe(3);
    expect(model?.dateMetadata['2026-05-25']?.hasTeamGame).toBe(true);
    expect(model?.onDateSelected.type).toBe(ActionType.Navigate);
  });

  it('returns null for malformed dateMetadata shape', () => {
    const section = {
      id: 'server:calendar~type=CalendarMonthList',
      type: 'CalendarMonthList',
      data: {
        stateKey: 'games_selected_date',
        selectedDate: '2026-05-25',
        defaultDate: '2026-05-25',
        dateMetadata: 'invalid',
        onDateSelected: {
          trigger: ActionTrigger.OnActivate,
          type: ActionType.Navigate,
          targetUri: 'nba://games?date={{games_selected_date}}',
        },
      },
    } as unknown as Section;

    expect(mapCalendarMonthList(section)).toBeNull();
  });
});
