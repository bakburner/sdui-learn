import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { Action, Section } from '@sdui/models';
import { TabGroup } from './TabGroup';

function tabSection(): Section {
  return {
    id: 'tg-1',
    type: 'TabGroup',
    data: {
      stateKey: 'active_tab',
      defaultTab: 'a',
      tabs: [
        { id: 'tab-a', label: 'A', stateValue: 'a' },
        { id: 'tab-b', label: 'B', stateValue: 'b' },
      ],
      tabContents: {
        a: [{ id: 'sec-a', type: 'AtomicComposite', data: { ui: { type: 'Text', content: 'A body' } } }],
        b: [{ id: 'sec-b', type: 'AtomicComposite', data: { ui: { type: 'Text', content: 'B body' } } }],
      },
    },
    subsections: [
      {
        id: 'tab-a',
        actions: [{ trigger: 'onActivate', type: 'mutate', target: 'active_tab', value: 'a' }],
      },
      {
        id: 'tab-b',
        actions: [{ trigger: 'onActivate', type: 'mutate', target: 'active_tab', value: 'b' }],
      },
    ],
  } as Section;
}

describe('TabGroup', () => {
  it('dispatches subsection mutate on tab click, not direct state writes', () => {
    const onAction = vi.fn();
    const onStateChange = vi.fn();

    render(
      <TabGroup
        section={tabSection()}
        state={{ active_tab: 'a' }}
        onAction={onAction}
        onStateChange={onStateChange}
      />,
    );

    fireEvent.click(screen.getByRole('tab', { name: 'B' }));

    expect(onAction).toHaveBeenCalledWith({
      trigger: 'onActivate',
      type: 'mutate',
      target: 'active_tab',
      value: 'b',
    } satisfies Action);
    expect(onStateChange).not.toHaveBeenCalled();
  });
});
