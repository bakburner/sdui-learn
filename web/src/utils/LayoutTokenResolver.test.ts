import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  currentFormFactor,
  resolveAspectRatio,
  resolveLayoutScalar,
  resolveSpacingPx,
} from './LayoutTokenResolver';

/**
 * Contract tests for the inlined token snapshots. The numbers below are
 * pinned to `schema/spacing-tokens.json`, `schema/size-tokens.json`,
 * `schema/corner-radius-tokens.json`, `schema/typography-tokens.json`, and
 * `schema/shadow-tokens.json`. If those files change, regenerate the
 * snapshot in `LayoutTokenResolver.ts` and update these expectations.
 */

function stubMatchMedia(matches: boolean): void {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    })),
  );
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
    // spacing.md → space.raw.12 → phone:12, tablet:14, web.wide:14
    expect(resolveLayoutScalar('token:spacing.md', 'phone')).toBe(12);
    expect(resolveLayoutScalar('token:spacing.md', 'tablet')).toBe(14);
    expect(resolveLayoutScalar('token:spacing.md', 'web.wide')).toBe(14);
    expect(resolveLayoutScalar('token:spacing.md', 'web.narrow')).toBe(12);
  });

  it('resolves a semantic radius token per form factor', () => {
    // radius.lg → radius.raw.12 → phone:12, tablet:14
    expect(resolveLayoutScalar('token:radius.lg', 'phone')).toBe(12);
    expect(resolveLayoutScalar('token:radius.lg', 'tablet')).toBe(14);
  });

  it('resolves palette tokens directly', () => {
    expect(resolveLayoutScalar('token:space.raw.16', 'phone')).toBe(16);
    expect(resolveLayoutScalar('token:size.raw.40', 'tablet')).toBe(48);
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
        top:    'token:spacing.xs',  // space.raw.4 → tablet:6
        bottom: 'token:spacing.sm',  // space.raw.8 → tablet:10
        start:  'token:spacing.md',  // space.raw.12 → tablet:14
        end:    'token:spacing.lg',  // space.raw.16 → tablet:18
      },
      'tablet',
    );
    expect(out).toEqual({ top: 6, bottom: 10, left: 14, right: 18 });
  });

  it('treats missing edges as 0 and ignores extra keys', () => {
    const out = resolveSpacingPx({ top: 'token:spacing.md' }, 'phone');
    expect(out).toEqual({ top: 12, bottom: 0, left: 0, right: 0 });
  });
});

describe('currentFormFactor', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('returns web.wide when matchMedia is unavailable (SSR or missing API)', () => {
    // jsdom does not implement matchMedia; with no stub, the SSR-safe
    // fallback returns 'web.wide'.
    expect(currentFormFactor()).toBe('web.wide');
  });

  it('returns web.wide when (min-width: 768px) matches', () => {
    stubMatchMedia(true);
    expect(currentFormFactor()).toBe('web.wide');
  });

  it('returns web.narrow when (min-width: 768px) does not match', () => {
    stubMatchMedia(false);
    expect(currentFormFactor()).toBe('web.narrow');
  });
});

describe('resolveLayoutScalar — form-factor default integration', () => {
  beforeEach(() => {
    stubMatchMedia(true);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('uses currentFormFactor() when no form factor is supplied', () => {
    // web.wide: spacing.md → 14
    expect(resolveLayoutScalar('token:spacing.md')).toBe(14);
  });
});
