import { describe, expect, it } from 'vitest';
import type { Section } from '@sdui/models';
import { mapTabGroup } from './sectionUiAdapters';

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
