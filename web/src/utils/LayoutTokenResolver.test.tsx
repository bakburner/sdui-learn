import React from 'react';
import { createRoot, type Root } from 'react-dom/client';
import { act } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FormFactorProvider, useFormFactor } from './FormFactorContext';
import {
  currentFormFactor,
  resolveAspectRatio,
  resolveLayoutScalar,
  resolveMotionDuration,
  resolveMotionEasing,
  resolveShadowToken,
  resolveSpacingPx,
  resolveTypography,
} from './LayoutTokenResolver';

type MatchMediaState = {
  minWidth1024: boolean;
  coarsePointer: boolean;
  darkMode: boolean;
};

function installMatchMedia(state: MatchMediaState): {
  update: (next: Partial<MatchMediaState>) => void;
} {
  const listeners = new Map<string, Set<(event: MediaQueryListEvent) => void>>();
  const listenerSet = (query: string): Set<(event: MediaQueryListEvent) => void> => {
    const existing = listeners.get(query);
    if (existing) return existing;
    const created = new Set<(event: MediaQueryListEvent) => void>();
    listeners.set(query, created);
    return created;
  };

  const matchesFor = (query: string): boolean => {
    switch (query) {
      case '(min-width: 1024px)':
        return state.minWidth1024;
      case '(pointer: coarse)':
        return state.coarsePointer;
      case '(prefers-color-scheme: dark)':
        return state.darkMode;
      default:
        return false;
    }
  };

  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      get matches() {
        return matchesFor(query);
      },
      media: query,
      onchange: null,
      addListener: (cb: (event: MediaQueryListEvent) => void) => {
        listenerSet(query).add(cb);
      },
      removeListener: (cb: (event: MediaQueryListEvent) => void) => {
        listenerSet(query).delete(cb);
      },
      addEventListener: (_type: string, cb: (event: MediaQueryListEvent) => void) => {
        listenerSet(query).add(cb);
      },
      removeEventListener: (_type: string, cb: (event: MediaQueryListEvent) => void) => {
        listenerSet(query).delete(cb);
      },
      dispatchEvent: () => false,
    })),
  );

  return {
    update(next: Partial<MatchMediaState>) {
      Object.assign(state, next);
      for (const [query, cbs] of listeners.entries()) {
        const event = { matches: matchesFor(query), media: query } as MediaQueryListEvent;
        cbs.forEach((cb) => cb(event));
      }
    },
  };
}

class MockResizeObserver {
  static instances: MockResizeObserver[] = [];
  private readonly callback: ResizeObserverCallback;

  constructor(callback: ResizeObserverCallback) {
    this.callback = callback;
    MockResizeObserver.instances.push(this);
  }

  observe = vi.fn();
  unobserve = vi.fn();
  disconnect = vi.fn();

  trigger(): void {
    this.callback([], this as unknown as ResizeObserver);
  }
}

function setWindowWidth(width: number): void {
  Object.defineProperty(window, 'innerWidth', { value: width, configurable: true, writable: true });
}

function render(element: React.ReactElement): {
  root: Root;
  container: HTMLDivElement;
  unmount: () => void;
} {
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  act(() => {
    root.render(element);
  });

  return {
    root,
    container,
    unmount() {
      act(() => {
        root.unmount();
      });
      container.remove();
    },
  };
}

describe('resolveLayoutScalar', () => {
  it('passes a numeric scalar through unchanged', () => {
    expect(resolveLayoutScalar(24)).toBe(24);
    expect(resolveLayoutScalar(0)).toBe(0);
  });

  it('returns 0 for undefined', () => {
    expect(resolveLayoutScalar(undefined)).toBe(0);
  });

  it('resolves a semantic spacing token per form factor', () => {
    // nba.spacing.md → phone:12, tablet:15, web:12
    expect(resolveLayoutScalar('token:nba.spacing.md', 'phone')).toBe(12);
    expect(resolveLayoutScalar('token:nba.spacing.md', 'tablet')).toBe(15);
    expect(resolveLayoutScalar('token:nba.spacing.md', 'web')).toBe(12);
    expect(resolveLayoutScalar('token:nba.spacing.md', 'tv')).toBe(18);
  });

  it('resolves a semantic radius token per form factor', () => {
    // nba.radius.lg → nba.radius.raw.16 → phone:16, tablet:16 (flat)
    expect(resolveLayoutScalar('token:nba.radius.lg', 'phone')).toBe(16);
    expect(resolveLayoutScalar('token:nba.radius.lg', 'tablet')).toBe(16);
  });

  it('returns 0 for unknown tokens and logs token_resolver_missing', () => {
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {});
    try {
      expect(resolveLayoutScalar('token:does.not.exist', 'phone')).toBe(0);
      expect(debugSpy).toHaveBeenCalledWith('token_resolver_missing', 'token:does.not.exist');
    } finally {
      debugSpy.mockRestore();
    }
  });

  it('returns 0 for non-token strings without logging', () => {
    const debugSpy = vi.spyOn(console, 'debug').mockImplementation(() => {});
    try {
      expect(resolveLayoutScalar('16px')).toBe(0);
      expect(resolveLayoutScalar('not-a-token')).toBe(0);
      expect(debugSpy).not.toHaveBeenCalled();
    } finally {
      debugSpy.mockRestore();
    }
  });
});

describe('resolveAspectRatio', () => {
  it('passes numeric ratios through', () => {
    expect(resolveAspectRatio(2)).toBe(2);
    expect(resolveAspectRatio(0.5)).toBe(0.5);
  });

  it('returns undefined for undefined', () => {
    expect(resolveAspectRatio(undefined)).toBeUndefined();
  });

  it('resolves the documented enum strings', () => {
    expect(resolveAspectRatio('16:9')).toBeCloseTo(16 / 9, 6);
    expect(resolveAspectRatio('4:3')).toBeCloseTo(4 / 3, 6);
    expect(resolveAspectRatio('1:1')).toBe(1);
    expect(resolveAspectRatio('3:2')).toBeCloseTo(3 / 2, 6);
    expect(resolveAspectRatio('21:9')).toBeCloseTo(21 / 9, 6);
  });

  it('returns undefined for unknown strings', () => {
    expect(resolveAspectRatio('garbage')).toBeUndefined();
    expect(resolveAspectRatio('5:4')).toBeUndefined();
  });
});

describe('resolveSpacingPx', () => {
  it('returns zeros for undefined spacing', () => {
    expect(resolveSpacingPx(undefined)).toEqual({ top: 0, bottom: 0, left: 0, right: 0 });
  });

  it('maps start → left and end → right (LTR; RTL revisited in Phase 5)', () => {
    const out = resolveSpacingPx(
      { top: 4, bottom: 8, start: 12, end: 16 },
      'phone',
    );
    expect(out).toEqual({ top: 4, bottom: 8, left: 12, right: 16 });
  });

  it('resolves token strings on each edge through the form-factor row', () => {
    const out = resolveSpacingPx(
      {
        top:    'token:nba.spacing.xs',  // nba.space.raw.2 → tablet:2
        bottom: 'token:nba.spacing.sm',  // nba.space.raw.4 → tablet:6
        start:  'token:nba.spacing.md',  // nba.space.raw.12 → tablet:15
        end:    'token:nba.spacing.lg',  // nba.space.raw.16 → tablet:20
      },
      'tablet',
    );
    expect(out).toEqual({ top: 2, bottom: 6, left: 15, right: 20 });
  });

  it('treats missing edges as 0 and ignores extra keys', () => {
    const out = resolveSpacingPx({ top: 'token:nba.spacing.md' }, 'phone');
    expect(out).toEqual({ top: 12, bottom: 0, left: 0, right: 0 });
  });
});

describe('currentFormFactor', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns web when matchMedia is unavailable (SSR or missing API)', () => {
    // jsdom does not implement matchMedia; with no stub, the SSR-safe
    // fallback returns 'web'.
    expect(currentFormFactor()).toBe('web');
  });

  it('returns web when min-width matches and pointer is not coarse', () => {
    setWindowWidth(1200);
    installMatchMedia({ minWidth1024: true, coarsePointer: false, darkMode: false });
    expect(currentFormFactor()).toBe('web');
  });

  it('returns tablet for coarse pointer width >= 768', () => {
    setWindowWidth(900);
    installMatchMedia({ minWidth1024: false, coarsePointer: true, darkMode: false });
    expect(currentFormFactor()).toBe('tablet');
  });

  it('returns phone for coarse pointer width < 768', () => {
    setWindowWidth(375);
    installMatchMedia({ minWidth1024: false, coarsePointer: true, darkMode: false });
    expect(currentFormFactor()).toBe('phone');
  });
});

describe('resolveLayoutScalar — form-factor default integration', () => {
  beforeEach(() => {
    setWindowWidth(1200);
    installMatchMedia({ minWidth1024: true, coarsePointer: false, darkMode: false });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uses currentFormFactor() when no form factor is supplied', () => {
    // web: nba.spacing.md → 12
    expect(resolveLayoutScalar('token:nba.spacing.md')).toBe(12);
  });
});

describe('resolveTypography', () => {
  it('resolves headlineLarge for phone', () => {
    expect(resolveTypography('token:nba.typography.headlineLarge', 'phone')).toEqual({
      familyRef: 'nba.font.knockout',
      weight: 360,
      textCase: 'uppercase',
      lineHeight: 0.8,
      size: 32,
    });
  });

  it('returns a web size envelope for bodyMedium', () => {
    expect(resolveTypography('token:nba.typography.bodyMedium', 'web')).toEqual({
      familyRef: 'nba.font.roboto',
      weight: 400,
      textCase: 'none',
      lineHeight: 1.2,
      size: {
        kind: 'envelope',
        min: 14,
        max: 18,
        minVw: 320,
        maxVw: 1440,
      },
    });
  });

  it('returns undefined for unknown tokens', () => {
    expect(resolveTypography('token:nba.typography.notReal', 'phone')).toBeUndefined();
  });
});

describe('shadow token resolution', () => {
  it('resolves nba.shadow.md to a structured drop shadow', () => {
    expect(resolveShadowToken('token:nba.shadow.md')).toEqual({
      type: 'drop',
      color: 'rgba(0,0,0,0.15)',
      radius: 8,
      offsetX: 0,
      offsetY: 2,
    });
  });

  it('returns undefined for unknown shadow token', () => {
    expect(resolveShadowToken('token:nba.shadow.unknown')).toBeUndefined();
  });

  it('returns undefined for non-token strings', () => {
    expect(resolveShadowToken('nba.shadow.md')).toBeUndefined();
  });
});

describe('motion token resolution', () => {
  it('resolves motion duration by form factor', () => {
    expect(resolveMotionDuration('token:nba.motion.duration.fast', 'phone')).toBe(150);
  });

  it('resolves motion easing to CSS curve', () => {
    expect(resolveMotionEasing('token:nba.motion.easing.default')).toBe('cubic-bezier(0.16, 1, 0.3, 1)');
  });
});

describe('FormFactorProvider network behavior', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    setWindowWidth(390);
    installMatchMedia({ minWidth1024: false, coarsePointer: true, darkMode: false });
    vi.stubGlobal('ResizeObserver', MockResizeObserver);
    vi.stubGlobal('fetch', vi.fn());
    vi.stubGlobal('EventSource', vi.fn());
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    MockResizeObserver.instances = [];
  });

  it('resolves after resize without fetch or EventSource calls', () => {
    function Probe(): React.ReactElement {
      const formFactor = useFormFactor();
      const spacing = resolveLayoutScalar('token:nba.spacing.md', formFactor);
      return <div data-testid="probe">{`${formFactor}:${spacing}`}</div>;
    }

    const view = render(
      <FormFactorProvider>
        <Probe />
      </FormFactorProvider>,
    );

    try {
      expect(view.container.textContent).toContain('phone:12');

      setWindowWidth(900);
      act(() => {
        window.dispatchEvent(new Event('resize'));
      });
      act(() => {
        MockResizeObserver.instances.forEach((instance) => instance.trigger());
      });
      act(() => {
        vi.advanceTimersByTime(200);
      });

      expect(view.container.textContent).toContain('tablet:15');
      expect(fetch).not.toHaveBeenCalled();
      expect(EventSource).not.toHaveBeenCalled();
    } finally {
      view.unmount();
    }
  });
});
