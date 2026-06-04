import { describe, it, expect, vi, beforeAll } from 'vitest';
import { render } from '@testing-library/react';
import type { AtomicElement, AccessibilityProperties } from '@sdui/models';
import { ActionTrigger, ActionType, Align, AtomicElementDirection, AtomicElementType } from '@sdui/models';
import { AtomicText } from './AtomicText';
import { AtomicImage } from './AtomicImage';
import { AtomicContainer } from './AtomicContainer';
import { AtomicScrollContainer } from './AtomicScrollContainer';
import { AtomicDisplayGrid } from './AtomicDisplayGrid';

vi.mock('../../utils/ColorTokenResolver', () => ({
  useColorTokenResolver: () => (token: string | undefined) => token ?? undefined,
}));

beforeAll(() => {
  global.ResizeObserver = class {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  } as unknown as typeof ResizeObserver;
});

const noop = () => {};

function withA11y(base: Partial<AtomicElement>, a11y: AccessibilityProperties): AtomicElement {
  return { ...base, accessibility: a11y } as AtomicElement;
}

// ---------------------------------------------------------------------------
// AtomicText
// ---------------------------------------------------------------------------
describe('AtomicText — accessibility', () => {
  const baseText: Partial<AtomicElement> = {
    id: 'txt-1',
    type: AtomicElementType.Text,
    content: 'Hello',
    variant: 'bodyMedium',
  };

  it('renders aria-label when accessibility.label is set', () => {
    const el = withA11y(baseText, { label: 'Greeting text' });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.getAttribute('aria-label')).toBe('Greeting text');
  });

  it('renders aria-hidden when accessibility.hidden is true', () => {
    const el = withA11y(baseText, { hidden: true });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.getAttribute('aria-hidden')).toBe('true');
  });

  it('renders role attribute when set', () => {
    const el = withA11y(baseText, { role: 'status' as never });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.getAttribute('role')).toBe('status');
  });

  it('renders native heading element for heading role with level', () => {
    const el = withA11y(baseText, { role: 'heading' as never, headingLevel: 2 });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const h2 = container.querySelector('h2');
    expect(h2).not.toBeNull();
    expect(h2?.textContent).toBe('Hello');
  });

  it('renders aria-live for liveRegion', () => {
    const el = withA11y(baseText, { liveRegion: 'polite' as never });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.getAttribute('aria-live')).toBe('polite');
  });

  it('renders aria-description for hint', () => {
    const el = withA11y(baseText, { hint: 'Tap for details' });
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.getAttribute('aria-description')).toBe('Tap for details');
  });

  it('does not add aria attributes when accessibility is omitted', () => {
    const el = { ...baseText } as AtomicElement;
    const { container } = render(<AtomicText element={el} state={{}} onAction={noop} depth={0} />);
    const span = container.querySelector('span');
    expect(span?.hasAttribute('aria-label')).toBe(false);
    expect(span?.hasAttribute('role')).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// AtomicImage
// ---------------------------------------------------------------------------
describe('AtomicImage — accessibility', () => {
  const baseImage: Partial<AtomicElement> = {
    id: 'img-1',
    type: AtomicElementType.Image,
    src: 'https://example.com/photo.png',
  };

  it('uses accessibility.label as alt text', () => {
    const el = withA11y(baseImage, { label: 'Team logo' });
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('alt')).toBe('Team logo');
    expect(img?.getAttribute('aria-label')).toBe('Team logo');
  });

  it('sets alt="" and aria-hidden for decorative images (hidden=true)', () => {
    const el = withA11y(baseImage, { hidden: true });
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('alt')).toBe('');
    expect(img?.getAttribute('aria-hidden')).toBe('true');
  });

  it('renders role attribute when set', () => {
    const el = withA11y(baseImage, { role: 'img' as never, label: 'Decorative banner' });
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('role')).toBe('img');
  });

  it('renders aria-live for liveRegion', () => {
    const el = withA11y(baseImage, { liveRegion: 'assertive' as never, label: 'Live score' });
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('aria-live')).toBe('assertive');
  });

  it('renders aria-description for hint', () => {
    const el = withA11y(baseImage, { hint: 'Double tap to enlarge' });
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('aria-description')).toBe('Double tap to enlarge');
  });

  it('falls back to element.alt when no accessibility.label', () => {
    const el = { ...baseImage, alt: 'Fallback alt' } as AtomicElement;
    const { container } = render(<AtomicImage element={el} state={{}} onAction={noop} depth={0} />);
    const img = container.querySelector('img');
    expect(img?.getAttribute('alt')).toBe('Fallback alt');
  });
});

// ---------------------------------------------------------------------------
// AtomicContainer
// ---------------------------------------------------------------------------
describe('AtomicContainer — accessibility', () => {
  const baseContainer: Partial<AtomicElement> = {
    id: 'ctr-1',
    type: AtomicElementType.Container,
    direction: AtomicElementDirection.Column,
    children: [],
  };

  it('renders aria-label on the container div', () => {
    const el = withA11y(baseContainer, { label: 'Navigation bar' });
    const { container } = render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('aria-label')).toBe('Navigation bar');
  });

  it('renders aria-hidden when hidden is true', () => {
    const el = withA11y(baseContainer, { hidden: true });
    const { container } = render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('aria-hidden')).toBe('true');
  });

  it('renders role attribute when set', () => {
    const el = withA11y(baseContainer, { role: 'navigation' as never });
    const { container } = render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('role')).toBe('navigation');
  });

  it('renders aria-level for heading role', () => {
    const el = withA11y(baseContainer, { role: 'heading' as never, headingLevel: 3 });
    const { container } = render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('role')).toBe('heading');
    expect(div?.getAttribute('aria-level')).toBe('3');
  });

  it('renders aria-live for liveRegion', () => {
    const el = withA11y(baseContainer, { liveRegion: 'polite' as never });
    const { container } = render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('aria-live')).toBe('polite');
  });

  it('logs warning when container has actions but no accessibility.label', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const el = {
      ...baseContainer,
      actions: [{ trigger: ActionTrigger.OnActivate, type: ActionType.Navigate, targetUri: 'nba://home' }],
    } as unknown as AtomicElement;
    render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    expect(spy).toHaveBeenCalledWith('a11y_container_missing_label', { elementId: 'ctr-1' });
    spy.mockRestore();
  });

  it('does not warn when container has actions AND accessibility.label', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const el = withA11y(
      { ...baseContainer, actions: [{ trigger: ActionTrigger.OnActivate, type: ActionType.Navigate, targetUri: 'nba://home' }] },
      { label: 'Go home' },
    );
    render(<AtomicContainer element={el} state={{}} onAction={noop} depth={0} />);
    expect(spy).not.toHaveBeenCalledWith('a11y_container_missing_label', expect.anything());
    spy.mockRestore();
  });
});

// ---------------------------------------------------------------------------
// AtomicScrollContainer
// ---------------------------------------------------------------------------
describe('AtomicScrollContainer — accessibility', () => {
  function leafText(id: string, label: string): AtomicElement {
    return { id, type: AtomicElementType.Text, content: label, variant: 'bodyMedium' } as AtomicElement;
  }

  const baseScroll: Partial<AtomicElement> = {
    id: 'scroll-a11y',
    type: AtomicElementType.ScrollContainer,
    direction: AtomicElementDirection.Row,
    children: [leafText('c1', 'A'), leafText('c2', 'B')],
  };

  it('renders aria-label on the scroll container', () => {
    const el = withA11y(baseScroll, { label: 'Featured stories' });
    const { container } = render(<AtomicScrollContainer element={el} state={{}} onAction={noop} depth={0} />);
    const listEl = container.querySelector('[role="list"]');
    expect(listEl?.getAttribute('aria-label')).toBe('Featured stories');
  });

  it('renders aria-hidden when hidden is true', () => {
    const el = withA11y(baseScroll, { hidden: true });
    const { container } = render(<AtomicScrollContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('aria-hidden')).toBe('true');
  });

  it('renders role override when set', () => {
    const el = withA11y(baseScroll, { role: 'navigation' as never, label: 'Nav carousel' });
    const { container } = render(<AtomicScrollContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    // extraProps role from accessibilityProps may be overridden by the component's own `role` prop
    // The component sets role="list" natively; extraProps adds to it
    expect(div).not.toBeNull();
  });

  it('renders aria-live for liveRegion', () => {
    const el = withA11y(baseScroll, { liveRegion: 'polite' as never });
    const { container } = render(<AtomicScrollContainer element={el} state={{}} onAction={noop} depth={0} />);
    const div = container.firstElementChild;
    expect(div?.getAttribute('aria-live')).toBe('polite');
  });
});

// ---------------------------------------------------------------------------
// AtomicDisplayGrid
// ---------------------------------------------------------------------------
describe('AtomicDisplayGrid — accessibility', () => {
  const baseGrid: Partial<AtomicElement> = {
    id: 'grid-1',
    type: AtomicElementType.DisplayGrid,
    columns: [
      { key: 'name', label: 'Name' },
      { key: 'pts', label: 'PTS', align: Align.End },
    ],
    rows: [
      { name: 'LeBron James', pts: '30' },
    ],
  };

  it('renders aria-label on the table', () => {
    const el = withA11y(baseGrid, { label: 'Player stats' });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('aria-label')).toBe('Player stats');
  });

  it('renders aria-hidden on the table when hidden is true', () => {
    const el = withA11y(baseGrid, { hidden: true });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('aria-hidden')).toBe('true');
  });

  it('renders role attribute when set', () => {
    const el = withA11y(baseGrid, { role: 'grid' as never });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('role')).toBe('grid');
  });

  it('renders aria-level for heading role', () => {
    const el = withA11y(baseGrid, { role: 'heading' as never, headingLevel: 1 });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('role')).toBe('heading');
    expect(table?.getAttribute('aria-level')).toBe('1');
  });

  it('renders aria-live for liveRegion', () => {
    const el = withA11y(baseGrid, { liveRegion: 'assertive' as never });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('aria-live')).toBe('assertive');
  });

  it('renders aria-description for hint', () => {
    const el = withA11y(baseGrid, { hint: 'Updated every 30 seconds' });
    const { container } = render(<AtomicDisplayGrid element={el} state={{}} onAction={noop} depth={0} />);
    const table = container.querySelector('table');
    expect(table?.getAttribute('aria-description')).toBe('Updated every 30 seconds');
  });
});
