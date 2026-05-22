import { beforeAll, describe, it, expect, vi } from 'vitest';
import { render } from '@testing-library/react';
import { AtomicScrollContainer } from './AtomicScrollContainer';
import type { AtomicElement } from '@sdui/models';
import { Align, BadgeAlignment, Style, UIType } from '@sdui/models';

vi.mock('../../utils/ColorTokenResolver', () => ({
  useColorTokenResolver: () => (token: string | undefined) => token ?? 'rgba(255,255,255,0.45)',
}));

beforeAll(() => {
  global.ResizeObserver = class {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  } as unknown as typeof ResizeObserver;
});

describe('AtomicScrollContainer — page indicators', () => {
  function leafText(id: string, label: string): AtomicElement {
    return {
      id,
      type: UIType.Text,
      content: label,
      variant: 'bodyMedium',
    } as AtomicElement;
  }

  it('renders dot indicators when paging, multi-child, and pageIndicator.style is dots', () => {
    const element: AtomicElement = {
      id: 'scroll-1',
      type: UIType.ScrollContainer,
      direction: 'row',
      paging: true,
      snapAlignment: Align.Center,
      pageIndicator: { style: Style.Dots, alignment: BadgeAlignment.BottomCenter },
      children: [leafText('a', 'A'), leafText('b', 'B')],
    } as AtomicElement;

    const { container } = render(
      <AtomicScrollContainer element={element} state={{}} onAction={() => {}} depth={0} />,
    );

    const dotsRow = container.querySelector('[role="tablist"]');
    expect(dotsRow).not.toBeNull();
    const dots = dotsRow?.querySelectorAll('button[role="tab"]');
    expect(dots?.length).toBe(2);
  });

  it('does not render dots when pageIndicator is omitted, even if paging is true', () => {
    const element: AtomicElement = {
      id: 'scroll-2',
      type: UIType.ScrollContainer,
      direction: 'row',
      paging: true,
      snapAlignment: Align.Start,
      children: [leafText('a', 'A'), leafText('b', 'B')],
    } as AtomicElement;

    const { container } = render(
      <AtomicScrollContainer element={element} state={{}} onAction={() => {}} depth={0} />,
    );

    expect(container.querySelector('[role="tablist"]')).toBeNull();
  });

  it('does not render dots when only one child, even with paging and pageIndicator dots', () => {
    const element: AtomicElement = {
      id: 'scroll-3',
      type: UIType.ScrollContainer,
      direction: 'row',
      paging: true,
      snapAlignment: 'center',
      pageIndicator: { style: 'dots', alignment: 'bottomCenter' },
      children: [leafText('only', 'Solo')],
    } as AtomicElement;

    const { container } = render(
      <AtomicScrollContainer element={element} state={{}} onAction={() => {}} depth={0} />,
    );

    expect(container.querySelector('[role="tablist"]')).toBeNull();
  });
});
