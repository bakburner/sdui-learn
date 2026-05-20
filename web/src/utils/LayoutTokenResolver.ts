/**
 * LayoutTokenResolver — resolves layout scalars and `token:…` references
 * against the bundled snapshots of the layout-token registries.
 *
 * Palette + semantic maps are an inline snapshot of:
 *   schema/spacing-tokens.json
 *   schema/corner-radius-tokens.json
 *
 * regenerate when schema/*-tokens.json changes
 *
 * Mirrors `ios/Sources/SduiCore/Rendering/LayoutTokenResolver.swift` so iOS,
 * Android, and web resolve the same wire string to the same numeric value
 * for any given form factor.
 */

const TOKEN_PREFIX = 'token:';
const MAX_ALIAS_DEPTH = 8;
const NARROW_BREAKPOINT_PX = 768;

export type FormFactor =
  | 'phone'
  | 'phone.landscape'
  | 'tablet'
  | 'tv'
  | 'web.narrow'
  | 'web.wide';

const KNOWN_FORM_FACTORS: ReadonlySet<FormFactor> = new Set<FormFactor>([
  'phone',
  'phone.landscape',
  'tablet',
  'tv',
  'web.narrow',
  'web.wide',
]);

/**
 * Best-effort form factor for the current browser viewport.
 *
 * Web breakpoint comes from the implementation plan (Phase 3): a single
 * 768px width threshold separates `web.narrow` from `web.wide`. SSR-safe:
 * returns `'web.wide'` when `window` is unavailable so server-rendered
 * markup is stable and matches the typical desktop default.
 */
export function currentFormFactor(): FormFactor {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'web.wide';
  }
  return window.matchMedia(`(min-width: ${NARROW_BREAKPOINT_PX}px)`).matches
    ? 'web.wide'
    : 'web.narrow';
}

/**
 * Returns the current OS color scheme. SSR-safe: defaults to `'light'`
 * when `window` is unavailable.
 */
export function currentTheme(): string {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'light';
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * Resolve a `LayoutScalar` (number | `token:…` string) to a CSS pixel value.
 *
 * Numeric values pass through unchanged. `token:<name>` is resolved through
 * the bundled palette + semantic registries. Unknown tokens log
 * `token_resolver_missing` at debug severity and fall back to 0 so the
 * payload still renders. Plain strings without the `token:` prefix are
 * not valid wire values for layout scalars and resolve to 0.
 */
export function resolveLayoutScalar(
  value: number | string | undefined,
  formFactor: FormFactor = currentFormFactor(),
  theme: string = currentTheme(),
): number {
  if (value == null) return 0;
  if (typeof value === 'number') return value;
  if (!value.startsWith(TOKEN_PREFIX)) return 0;
  const name = value.slice(TOKEN_PREFIX.length);
  if (!(name in REGISTRY) && !(name in SEMANTIC)) {
    if (typeof console !== 'undefined') {
      console.debug('token_resolver_missing', value);
    }
    return 0;
  }
  return followAlias(name, formFactor, theme, 0);
}

/**
 * Resolve an `AspectRatioUnion` (number | enum string) to a numeric ratio.
 * Unknown enum values resolve to `undefined` so the caller can omit the
 * declaration entirely.
 */
export function resolveAspectRatio(value: number | string | undefined): number | undefined {
  if (value == null) return undefined;
  if (typeof value === 'number') return value;
  switch (value) {
    case '16:9': return 16 / 9;
    case '4:3':  return 4 / 3;
    case '1:1':  return 1;
    case '3:2':  return 3 / 2;
    case '21:9': return 21 / 9;
    default:     return undefined;
  }
}

interface SpacingInput {
  top?: number | string;
  bottom?: number | string;
  start?: number | string;
  end?: number | string;
}

export interface ResolvedSpacingPx {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

/**
 * Resolve a `Spacing` block into LTR CSS edges (top/bottom/left/right) in
 * pixels. `start` maps to `left` and `end` maps to `right`.
 *
 * TODO(rtl): Phase 5 introduces locale-aware layout direction; when the
 * document is in an RTL locale the start/end → left/right mapping should
 * flip. Today we assume LTR; revisit when RTL lands.
 */
export function resolveSpacingPx(
  spacing: SpacingInput | undefined,
  formFactor: FormFactor = currentFormFactor(),
  theme: string = currentTheme(),
): ResolvedSpacingPx {
  if (!spacing) return { top: 0, bottom: 0, left: 0, right: 0 };
  return {
    top:    resolveLayoutScalar(spacing.top,    formFactor, theme),
    bottom: resolveLayoutScalar(spacing.bottom, formFactor, theme),
    left:   resolveLayoutScalar(spacing.start,  formFactor, theme),
    right:  resolveLayoutScalar(spacing.end,    formFactor, theme),
  };
}

/** Test-only: surface KNOWN_FORM_FACTORS for callers that want to validate
 * a string before forwarding it as a form-factor override. */
export function isFormFactor(value: string): value is FormFactor {
  return KNOWN_FORM_FACTORS.has(value as FormFactor);
}

// ────────────────────────────────────────────────────────────────────────
// Snapshot: semantic aliases (alias name → palette/alias key)
// ────────────────────────────────────────────────────────────────────────

const SEMANTIC: Record<string, string> = {
  'nba.spacing.xs':  'nba.space.raw.2',
  'nba.spacing.sm':  'nba.space.raw.4',
  'nba.spacing.md':  'nba.space.raw.12',
  'nba.spacing.lg':  'nba.space.raw.16',
  'nba.spacing.xl':  'nba.space.raw.32',
  'nba.spacing.2xl': 'nba.space.raw.40',

  'nba.radius.xs':   'nba.radius.raw.2',
  'nba.radius.sm':   'nba.radius.raw.4',
  'nba.radius.md':   'nba.radius.raw.12',
  'nba.radius.lg':   'nba.radius.raw.16',
  'nba.radius.xl':   'nba.radius.raw.24',
  'nba.radius.2xl':  'nba.radius.raw.32',
  'nba.radius.full': 'nba.radius.raw.9999',
};

// ────────────────────────────────────────────────────────────────────────
// Snapshot: merged palette (palette key → form-factor row)
//
// Tuple ordering: [phone, phone.landscape, tablet, tv, web.narrow, web.wide].
// ────────────────────────────────────────────────────────────────────────

type PaletteRow = readonly [number, number, number, number, number, number];

/** Variable-mode matrix shape: { theme: { formFactor: value } }. */
export type RegistryEntry = Record<string, Record<string, number>>;

const PALETTE_RAW: Record<string, PaletteRow> = {
  // spacing (Kinetic)
  'nba.space.raw.0':   [0,  0,  0,  0,  0,  0 ],
  'nba.space.raw.2':   [2,  2,  2,  4,  2,  2 ],
  'nba.space.raw.4':   [4,  4,  6,  6,  4,  6 ],
  'nba.space.raw.8':   [8,  8,  10, 12, 8,  10],
  'nba.space.raw.12':  [12, 12, 15, 18, 12, 15],
  'nba.space.raw.16':  [16, 16, 20, 24, 16, 20],
  'nba.space.raw.32':  [32, 32, 40, 48, 32, 40],
  'nba.space.raw.40':  [40, 40, 48, 56, 40, 48],

  // corner radius (Kinetic — flat across form factors)
  'nba.radius.raw.0':    [0,    0,    0,    0,    0,    0   ],
  'nba.radius.raw.2':    [2,    2,    2,    2,    2,    2   ],
  'nba.radius.raw.4':    [4,    4,    4,    4,    4,    4   ],
  'nba.radius.raw.8':    [8,    8,    8,    8,    8,    8   ],
  'nba.radius.raw.12':   [12,   12,   12,   12,   12,   12  ],
  'nba.radius.raw.16':   [16,   16,   16,   16,   16,   16  ],
  'nba.radius.raw.24':   [24,   24,   24,   24,   24,   24  ],
  'nba.radius.raw.32':   [32,   32,   32,   32,   32,   32  ],
  'nba.radius.raw.9999': [9999, 9999, 9999, 9999, 9999, 9999],
};

const FORM_FACTOR_NAMES: readonly FormFactor[] = [
  'phone', 'phone.landscape', 'tablet', 'tv', 'web.narrow', 'web.wide',
];

// Build REGISTRY from compact tuples → matrix shape { "*": { formFactor: value } }
const REGISTRY: Record<string, RegistryEntry> = {};
for (const [key, row] of Object.entries(PALETTE_RAW)) {
  const ffMap: Record<string, number> = {};
  for (let i = 0; i < FORM_FACTOR_NAMES.length; i++) {
    ffMap[FORM_FACTOR_NAMES[i]] = row[i];
  }
  REGISTRY[key] = { '*': ffMap };
}

function resolveEntry(entry: RegistryEntry, theme: string, formFactor: string): number {
  // Fallback order:
  // 1. entry[theme][formFactor] — exact match
  // 2. entry[theme]["*"] — theme-specific, any form factor
  // 3. entry["*"][formFactor] — any theme, form-factor-specific
  // 4. entry["*"]["*"] — universal fallback
  const themeRow = entry[theme];
  if (themeRow) {
    if (formFactor in themeRow) return themeRow[formFactor];
    if ('*' in themeRow) return themeRow['*'];
  }
  const wildRow = entry['*'];
  if (wildRow) {
    if (formFactor in wildRow) return wildRow[formFactor];
    if ('*' in wildRow) return wildRow['*'];
  }
  return 0;
}

function followAlias(name: string, formFactor: FormFactor, theme: string, depth: number): number {
  if (depth > MAX_ALIAS_DEPTH) return 0;
  const entry = REGISTRY[name];
  if (entry) {
    return resolveEntry(entry, theme, formFactor);
  }
  const next = SEMANTIC[name];
  if (!next) return 0;
  return followAlias(next, formFactor, theme, depth + 1);
}
