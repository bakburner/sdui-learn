/**
 * ColorTokenResolver — converts SDUI color values to concrete CSS color
 * strings.
 *
 * Input shapes, per the schema's `ColorToken` definition:
 *   - literal hex: `#RRGGBB` or `#RRGGBBAA` — returned unchanged.
 *   - semantic token: `token:<dot.separated.path>` — looked up against
 *     the bundled registry (palette primitives + semantic aliases) and
 *     resolved to a light or dark hex based on the caller's current
 *     `ColorScheme`.
 *
 * The registry below is a snapshot of `schema/color-tokens.json`.
 * Re-transcribe when that file changes. Do not fetch the JSON at
 * runtime — the snapshot is deliberately inlined so the bundle has
 * no network or file dependency at render time.
 */

import { useEffect, useState } from 'react';

export type ColorScheme = 'light' | 'dark';

const THEME_STORAGE_KEY = 'sdui-color-scheme';
const THEME_CHANGE_EVENT = 'sdui-color-scheme-change';

interface PaletteEntry {
  light: string;
  dark: string;
}

/** Palette primitives — keyed by `color.<family>.<step>`. */
const PALETTE: Record<string, PaletteEntry> = {
  // greys
  'color.grey.0':   { light: '#FFFFFF', dark: '#FFFFFF' },
  'color.grey.5':   { light: '#FAFAFA', dark: '#0D0F12' },
  'color.grey.10':  { light: '#F2F2F7', dark: '#1A1F2E' },
  'color.grey.20':  { light: '#E5E5EA', dark: '#2A2A4A' },
  'color.grey.30':  { light: '#D1D1D6', dark: '#3A3A5A' },
  'color.grey.40':  { light: '#C7C7CC', dark: '#48485F' },
  'color.grey.50':  { light: '#8E8E93', dark: '#7A8BAA' },
  'color.grey.60':  { light: '#636366', dark: '#9999AA' },
  'color.grey.70':  { light: '#48484A', dark: '#AAAAAA' },
  'color.grey.80':  { light: '#3A3A3C', dark: '#CCCCCC' },
  'color.grey.90':  { light: '#1C1C1E', dark: '#E5E5E7' },
  'color.grey.95':  { light: '#0F0F10', dark: '#F2F2F4' },
  'color.grey.99':  { light: '#050506', dark: '#FAFAFB' },
  'color.grey.100': { light: '#000000', dark: '#FFFFFF' },

  // blues
  'color.blue.0':   { light: '#F5F8FF', dark: '#0A1128' },
  'color.blue.10':  { light: '#E0EAFF', dark: '#0E1B3E' },
  'color.blue.20':  { light: '#B6CDFF', dark: '#12295A' },
  'color.blue.30':  { light: '#7FA0F0', dark: '#1D428A' },
  'color.blue.40':  { light: '#3D6BD4', dark: '#2B5AB0' },
  'color.blue.50':  { light: '#17408B', dark: '#5B8DEE' },
  'color.blue.60':  { light: '#1A4FAF', dark: '#7FA8F0' },
  'color.blue.70':  { light: '#3D6DC4', dark: '#A3C1F5' },
  'color.blue.80':  { light: '#6F94DC', dark: '#C7D9F8' },
  'color.blue.90':  { light: '#A8BEE8', dark: '#E0EAFB' },
  'color.blue.95':  { light: '#CFDDF3', dark: '#EEF2FD' },
  'color.blue.99':  { light: '#F3F6FD', dark: '#F9FBFE' },
  'color.blue.100': { light: '#FFFFFF', dark: '#FFFFFF' },

  // reds
  'color.red.0':   { light: '#FFF5F5', dark: '#2C0A0F' },
  'color.red.10':  { light: '#FFE5E8', dark: '#4A0F16' },
  'color.red.20':  { light: '#FFB8C0', dark: '#6B0A18' },
  'color.red.30':  { light: '#FF8091', dark: '#8A0D1E' },
  'color.red.40':  { light: '#FF4D62', dark: '#A81025' },
  'color.red.50':  { light: '#C8102E', dark: '#FF6B6B' },
  'color.red.60':  { light: '#D63848', dark: '#FF8E95' },
  'color.red.70':  { light: '#E06470', dark: '#FFB0B5' },
  'color.red.80':  { light: '#EC9298', dark: '#FFCBCF' },
  'color.red.90':  { light: '#F6C3C6', dark: '#FFE1E3' },
  'color.red.95':  { light: '#FBDEE0', dark: '#FFEFF0' },
  'color.red.99':  { light: '#FEF7F7', dark: '#FFFAFB' },
  'color.red.100': { light: '#FFFFFF', dark: '#FFFFFF' },

  // greens
  'color.green.0':   { light: '#F4FBF5', dark: '#0A1F12' },
  'color.green.10':  { light: '#DCF0DF', dark: '#0D3018' },
  'color.green.20':  { light: '#A8D8B2', dark: '#14502A' },
  'color.green.30':  { light: '#6EBC83', dark: '#1C6D3A' },
  'color.green.40':  { light: '#3D9E5A', dark: '#258A4C' },
  'color.green.50':  { light: '#1F8A3F', dark: '#4CB27A' },
  'color.green.60':  { light: '#3EA05B', dark: '#70C594' },
  'color.green.70':  { light: '#68B87F', dark: '#94D4AE' },
  'color.green.80':  { light: '#97CCA5', dark: '#B8E1C5' },
  'color.green.90':  { light: '#C4E0CB', dark: '#DCEEE0' },
  'color.green.95':  { light: '#DFECE3', dark: '#EDF5EF' },
  'color.green.99':  { light: '#F8FBF9', dark: '#FAFCFB' },
  'color.green.100': { light: '#FFFFFF', dark: '#FFFFFF' },

  // oranges
  'color.orange.0':   { light: '#FFF8F0', dark: '#2C1708' },
  'color.orange.10':  { light: '#FFEAD0', dark: '#4A2610' },
  'color.orange.20':  { light: '#FFCE8B', dark: '#6E3A17' },
  'color.orange.30':  { light: '#FFA94D', dark: '#945121' },
  'color.orange.40':  { light: '#F58420', dark: '#BA6B2E' },
  'color.orange.50':  { light: '#D86E0F', dark: '#F58A3E' },
  'color.orange.60':  { light: '#E0863A', dark: '#FFA35F' },
  'color.orange.70':  { light: '#E9A066', dark: '#FFB988' },
  'color.orange.80':  { light: '#F2BB92', dark: '#FFD0B0' },
  'color.orange.90':  { light: '#F9D9C0', dark: '#FFE5D3' },
  'color.orange.95':  { light: '#FCE8D6', dark: '#FFEFE3' },
  'color.orange.99':  { light: '#FEF9F4', dark: '#FFFBF7' },
  'color.orange.100': { light: '#FFFFFF', dark: '#FFFFFF' },

  // yellows
  'color.yellow.0':   { light: '#FFFDF0', dark: '#2A2408' },
  'color.yellow.10':  { light: '#FFF6CC', dark: '#463B0F' },
  'color.yellow.20':  { light: '#FFEB85', dark: '#695A17' },
  'color.yellow.30':  { light: '#FFDB3F', dark: '#8F7C20' },
  'color.yellow.40':  { light: '#E8C010', dark: '#B69E29' },
  'color.yellow.50':  { light: '#C6A208', dark: '#EACB3B' },
  'color.yellow.60':  { light: '#D1B035', dark: '#F2D85D' },
  'color.yellow.70':  { light: '#DCC165', dark: '#F6E08A' },
  'color.yellow.80':  { light: '#E8D392', dark: '#FAE8B2' },
  'color.yellow.90':  { light: '#F1E4C0', dark: '#FBEED4' },
  'color.yellow.95':  { light: '#F7EFDA', dark: '#FDF4E6' },
  'color.yellow.99':  { light: '#FDFAF4', dark: '#FEFAF2' },
  'color.yellow.100': { light: '#FFFFFF', dark: '#FFFFFF' },
};

/** Semantic aliases — each maps to a palette primitive or another alias. */
const SEMANTIC: Record<string, string> = {
  // primary (grey ramp)
  'color.primary.0':   'color.grey.0',
  'color.primary.10':  'color.grey.10',
  'color.primary.20':  'color.grey.20',
  'color.primary.30':  'color.grey.30',
  'color.primary.40':  'color.grey.40',
  'color.primary.50':  'color.grey.50',
  'color.primary.60':  'color.grey.60',
  'color.primary.70':  'color.grey.70',
  'color.primary.80':  'color.grey.80',
  'color.primary.90':  'color.grey.90',
  'color.primary.95':  'color.grey.95',
  'color.primary.99':  'color.grey.99',
  'color.primary.100': 'color.grey.100',

  // secondary (yellow ramp)
  'color.secondary.0':   'color.yellow.0',
  'color.secondary.10':  'color.yellow.10',
  'color.secondary.20':  'color.yellow.20',
  'color.secondary.30':  'color.yellow.30',
  'color.secondary.40':  'color.yellow.40',
  'color.secondary.50':  'color.yellow.50',
  'color.secondary.60':  'color.yellow.60',
  'color.secondary.70':  'color.yellow.70',
  'color.secondary.80':  'color.yellow.80',
  'color.secondary.90':  'color.yellow.90',
  'color.secondary.95':  'color.yellow.95',
  'color.secondary.99':  'color.yellow.99',
  'color.secondary.100': 'color.yellow.100',

  // tertiary (blue ramp)
  'color.tertiary.0':   'color.blue.0',
  'color.tertiary.10':  'color.blue.10',
  'color.tertiary.20':  'color.blue.20',
  'color.tertiary.30':  'color.blue.30',
  'color.tertiary.40':  'color.blue.40',
  'color.tertiary.50':  'color.blue.50',
  'color.tertiary.60':  'color.blue.60',
  'color.tertiary.70':  'color.blue.70',
  'color.tertiary.80':  'color.blue.80',
  'color.tertiary.90':  'color.blue.90',
  'color.tertiary.95':  'color.blue.95',
  'color.tertiary.99':  'color.blue.99',
  'color.tertiary.100': 'color.blue.100',

  // feedback.success (green ramp)
  'color.feedback.success.0':   'color.green.0',
  'color.feedback.success.10':  'color.green.10',
  'color.feedback.success.20':  'color.green.20',
  'color.feedback.success.30':  'color.green.30',
  'color.feedback.success.40':  'color.green.40',
  'color.feedback.success.50':  'color.green.50',
  'color.feedback.success.60':  'color.green.60',
  'color.feedback.success.70':  'color.green.70',
  'color.feedback.success.80':  'color.green.80',
  'color.feedback.success.90':  'color.green.90',
  'color.feedback.success.95':  'color.green.95',
  'color.feedback.success.99':  'color.green.99',
  'color.feedback.success.100': 'color.green.100',

  // feedback.error (red ramp)
  'color.feedback.error.0':   'color.red.0',
  'color.feedback.error.10':  'color.red.10',
  'color.feedback.error.20':  'color.red.20',
  'color.feedback.error.30':  'color.red.30',
  'color.feedback.error.40':  'color.red.40',
  'color.feedback.error.50':  'color.red.50',
  'color.feedback.error.60':  'color.red.60',
  'color.feedback.error.70':  'color.red.70',
  'color.feedback.error.80':  'color.red.80',
  'color.feedback.error.90':  'color.red.90',
  'color.feedback.error.95':  'color.red.95',
  'color.feedback.error.99':  'color.red.99',
  'color.feedback.error.100': 'color.red.100',

  // feedback.warning (orange ramp)
  'color.feedback.warning.0':   'color.orange.0',
  'color.feedback.warning.10':  'color.orange.10',
  'color.feedback.warning.20':  'color.orange.20',
  'color.feedback.warning.30':  'color.orange.30',
  'color.feedback.warning.40':  'color.orange.40',
  'color.feedback.warning.50':  'color.orange.50',
  'color.feedback.warning.60':  'color.orange.60',
  'color.feedback.warning.70':  'color.orange.70',
  'color.feedback.warning.80':  'color.orange.80',
  'color.feedback.warning.90':  'color.orange.90',
  'color.feedback.warning.95':  'color.orange.95',
  'color.feedback.warning.99':  'color.orange.99',
  'color.feedback.warning.100': 'color.orange.100',

  // brand
  'color.brand.nba':  'color.blue.50',
  'color.brand.live': 'color.red.50',

  // surface
  'color.surface.canvas': 'color.grey.5',
  'color.surface.raised': 'color.grey.10',
  'color.surface.sunken': 'color.grey.5',
  'color.surface.promo':  'color.blue.10',

  // text
  'color.text.primary':   'color.grey.90',
  'color.text.secondary': 'color.grey.60',
  'color.text.tertiary':  'color.grey.50',
  'color.text.inverse':   'color.grey.0',
  'color.text.onBrand':   'color.grey.0',

  // border
  'color.border.default': 'color.grey.20',
  'color.border.subtle':  'color.grey.10',

  // overlay
  'color.overlay.scrim': 'color.grey.100',
};

const TOKEN_PREFIX = 'token:';
const MAX_ALIAS_DEPTH = 8;

const resolvedTokenCache = new Map<string, string | undefined>();

function tokenCacheKey(scheme: ColorScheme, value: string): string {
  return `${scheme}\0${value}`;
}

/**
 * Resolve an SDUI color value to a CSS color string.
 * - `null` / `undefined` / empty → returns `undefined` so the caller can
 *   decide a fallback.
 * - `"#RRGGBB"` / `"#RRGGBBAA"` → returned as-is.
 * - `"token:<path>"` → looked up against the semantic + palette
 *   registry; picks light or dark based on the current `ColorScheme`.
 *   Unknown tokens log `token_resolver_missing` and return `undefined`.
 */
export function resolveColorToken(
  value: string | null | undefined,
  scheme: ColorScheme,
): string | undefined {
  if (!value) return undefined;
  if (!value.startsWith(TOKEN_PREFIX)) {
    const key = tokenCacheKey(scheme, value);
    if (resolvedTokenCache.has(key)) {
      return resolvedTokenCache.get(key);
    }
    resolvedTokenCache.set(key, value);
    return value;
  }

  const key = tokenCacheKey(scheme, value);
  if (resolvedTokenCache.has(key)) {
    return resolvedTokenCache.get(key);
  }

  const name = value.slice(TOKEN_PREFIX.length);
  const entry = followAlias(name);
  if (!entry) {
    if (typeof console !== 'undefined') {
      console.warn('token_resolver_missing', { token: value });
    }
    resolvedTokenCache.set(key, undefined);
    return undefined;
  }
  const resolved = scheme === 'dark' ? entry.dark : entry.light;
  resolvedTokenCache.set(key, resolved);
  return resolved;
}

function followAlias(name: string, depth = 0): PaletteEntry | undefined {
  if (depth > MAX_ALIAS_DEPTH) return undefined;
  const primitive = PALETTE[name];
  if (primitive) return primitive;
  const next = SEMANTIC[name];
  if (!next) return undefined;
  return followAlias(next, depth + 1);
}

function systemColorScheme(): ColorScheme {
  return typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
}

function storedColorScheme(): ColorScheme | undefined {
  if (typeof window === 'undefined') return undefined;
  const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
  return stored === 'light' || stored === 'dark' ? stored : undefined;
}

export function getEffectiveColorScheme(): ColorScheme {
  if (typeof document !== 'undefined') {
    const attr = document.documentElement.dataset.theme;
    if (attr === 'light' || attr === 'dark') return attr;
  }
  return storedColorScheme() ?? systemColorScheme();
}

function applyColorScheme(scheme: ColorScheme): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', scheme);
    document.documentElement.dataset.theme = scheme;
    document.documentElement.style.colorScheme = scheme;
  }
}

export function setColorSchemePreference(scheme: ColorScheme): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', scheme);
  }
  applyColorScheme(scheme);
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(THEME_STORAGE_KEY, scheme);
    window.dispatchEvent(new CustomEvent<ColorScheme>(THEME_CHANGE_EVENT, { detail: scheme }));
  }
}

export function initializeColorSchemePreference(): void {
  applyColorScheme(storedColorScheme() ?? systemColorScheme());
}

/**
 * React hook that tracks the effective app color scheme. The app-level
 * toggle wins over OS `prefers-color-scheme`; without an override it
 * follows the OS setting. SSR-safe: returns `'light'` when unavailable.
 */
export function usePrefersColorScheme(): ColorScheme {
  const getScheme = (): ColorScheme => getEffectiveColorScheme();

  const [scheme, setScheme] = useState<ColorScheme>(getScheme);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const update = () => {
      const next = storedColorScheme() ?? systemColorScheme();
      applyColorScheme(next);
      setScheme(next);
    };
    const mq = typeof window.matchMedia === 'function'
      ? window.matchMedia('(prefers-color-scheme: dark)')
      : undefined;

    window.addEventListener(THEME_CHANGE_EVENT, update);
    mq?.addEventListener?.('change', update);

    update();
    return () => {
      window.removeEventListener(THEME_CHANGE_EVENT, update);
      mq?.removeEventListener?.('change', update);
    };
  }, []);

  return scheme;
}

/**
 * Convenience wrapper — returns a closure bound to the current
 * color scheme so components can call `resolve(value)` without
 * threading `scheme` through every call site.
 */
export function useColorTokenResolver(): (value: string | null | undefined) => string | undefined {
  const scheme = usePrefersColorScheme();
  return (value) => resolveColorToken(value, scheme);
}
