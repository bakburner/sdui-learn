import { describe, it, expect, vi } from 'vitest';
import {
  resolveContainerVariant,
  logVariantOverrideBlocked,
  axisAllowsOverride,
} from './ContainerVariantResolver';
import { resolveImageVariant } from './ImageVariantResolver';

describe('ContainerVariantResolver', () => {
  it('returns undefined for null variant', () => {
    expect(resolveContainerVariant(null)).toBeUndefined();
  });

  it('returns undefined for unknown variant', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    expect(resolveContainerVariant('nonexistent')).toBeUndefined();
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });

  it('hero web.narrow returns base spec (phone-equivalent shadow)', () => {
    const spec = resolveContainerVariant('hero', 'web.narrow');
    expect(spec).toBeDefined();
    expect(spec!.cornerRadius).toBe(16);
    expect(spec!.boxShadow).toBe(
      '0 6px 18px rgba(0,0,0,0.12), 0 12px 24px rgba(0,0,0,0.08)',
    );
  });

  it('hero web.wide returns increased shadow', () => {
    const spec = resolveContainerVariant('hero', 'web.wide');
    expect(spec).toBeDefined();
    expect(spec!.cornerRadius).toBe(16);
    expect(spec!.boxShadow).toBe(
      '0 8px 24px rgba(0,0,0,0.14), 0 16px 32px rgba(0,0,0,0.10)',
    );
  });

  it('hero default (no formFactor) returns base spec', () => {
    const spec = resolveContainerVariant('hero');
    expect(spec).toBeDefined();
    expect(spec!.boxShadow).toBe(
      '0 6px 18px rgba(0,0,0,0.12), 0 12px 24px rgba(0,0,0,0.08)',
    );
  });

  it('grouped returns same spec regardless of form factor', () => {
    const narrow = resolveContainerVariant('grouped', 'web.narrow');
    const wide = resolveContainerVariant('grouped', 'web.wide');
    expect(narrow).toBeDefined();
    expect(wide).toBeDefined();
    expect(narrow!.cornerRadius).toBe(wide!.cornerRadius);
  });

  it('shadow axis is locked on hero', () => {
    const spec = resolveContainerVariant('hero')!;
    expect(axisAllowsOverride(spec, 'shadow')).toBe(false);
    expect(axisAllowsOverride(spec, 'padding')).toBe(true);
  });

  it('logVariantOverrideBlocked does not throw', () => {
    expect(() => logVariantOverrideBlocked('hero', 'shadow', '0')).not.toThrow();
  });
});

describe('ImageVariantResolver', () => {
  it('returns undefined for null variant', () => {
    expect(resolveImageVariant(null)).toBeUndefined();
  });

  it('returns undefined for unknown variant', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    expect(resolveImageVariant('nonexistent')).toBeUndefined();
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });

  it('thumbnail returns 8px corner radius regardless of form factor', () => {
    const narrow = resolveImageVariant('thumbnail', 'web.narrow');
    const wide = resolveImageVariant('thumbnail', 'web.wide');
    expect(narrow).toBeDefined();
    expect(narrow!.cornerRadius).toBe(8);
    expect(wide).toBeDefined();
    expect(wide!.cornerRadius).toBe(8);
  });
});
