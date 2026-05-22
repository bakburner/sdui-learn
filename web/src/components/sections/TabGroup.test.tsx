import { describe, expect, it, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import type { Action, Section } from '@sdui/models';
import { ActionTrigger, ActionType } from '@sdui/models';
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
        actions: [{ trigger: ActionTrigger.OnActivate, type: ActionType.Mutate, target: 'active_tab', value: 'a' }],
      },
      {
        id: 'tab-b',
        actions: [{ trigger: ActionTrigger.OnActivate, type: ActionType.Mutate, target: 'active_tab', value: 'b' }],
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
      trigger: ActionTrigger.OnActivate,
      type: ActionType.Mutate,
      target: 'active_tab',
      value: 'b',
    } satisfies Action);
    expect(onStateChange).not.toHaveBeenCalled();
  });

  it('clears the previous active tab underline when selection changes', () => {
    const onAction = vi.fn();
    const onStateChange = vi.fn();

    const { rerender } = render(
      <TabGroup
        section={tabSection()}
        state={{ active_tab: 'a' }}
        onAction={onAction}
        onStateChange={onStateChange}
      />,
    );

    const tabA = screen.getByRole('tab', { name: 'A' });
    const tabB = screen.getByRole('tab', { name: 'B' });
    expect(tabA).toHaveAttribute('aria-selected', 'true');
    expect(tabB).toHaveAttribute('aria-selected', 'false');
    expect((tabA as HTMLButtonElement).style.borderBottomColor).not.toBe('transparent');

    rerender(
      <TabGroup
        section={tabSection()}
        state={{ active_tab: 'b' }}
        onAction={onAction}
        onStateChange={onStateChange}
      />,
    );

    expect(tabA).toHaveAttribute('aria-selected', 'false');
    expect(tabB).toHaveAttribute('aria-selected', 'true');
    expect((tabA as HTMLButtonElement).style.borderBottomColor).toBe('transparent');
    expect((tabB as HTMLButtonElement).style.borderBottomColor).not.toBe('transparent');
  });
});
