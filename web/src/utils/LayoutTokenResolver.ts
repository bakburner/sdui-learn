import {
  LayoutTokenRegistry,
  type FormFactorMatrix,
  type ShadowSpec,
  type TypographyCategorySpec,
  type TypographyVariantSpec,
  type WebSizeEnvelope,
} from '../generated/LayoutTokenRegistry';
import type { Shadow, ShadowType } from '../generated/SduiModels';

const TOKEN_PREFIX = 'token:';
const WEB_BREAKPOINT_PX = 1024;
const TABLET_BREAKPOINT_PX = 768;

export type FormFactor = 'phone' | 'tablet' | 'tv' | 'web';

export type TypographySizeEnvelope = {
  kind: 'envelope';
  min: number;
  max: number;
  minVw: number;
  maxVw: number;
};

export type TypographySpec = {
  familyRef: string;
  weight: number;
  textCase: string;
  lineHeight: number;
  size: number | TypographySizeEnvelope;
};

const KNOWN_FORM_FACTORS: ReadonlySet<FormFactor> = new Set<FormFactor>([
  'phone',
  'tablet',
  'tv',
  'web',
]);

function isTouchDevice(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }
  return window.matchMedia('(pointer: coarse)').matches;
}

function viewportWidth(): number {
  if (typeof window === 'undefined') return WEB_BREAKPOINT_PX;
  if (typeof window.innerWidth === 'number' && window.innerWidth > 0) return window.innerWidth;
  if (typeof document !== 'undefined' && document.documentElement?.clientWidth) {
    return document.documentElement.clientWidth;
  }
  return WEB_BREAKPOINT_PX;
}

/**
 * Best-effort form-factor selection for the active browser environment.
 *
 * SSR-safe: defaults to `web` when `window` is unavailable.
 */
export function currentFormFactor(): FormFactor {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return 'web';
  }

  const width = viewportWidth();
  const isCoarsePointer = isTouchDevice();
  const isDesktopWeb = window.matchMedia(`(min-width: ${WEB_BREAKPOINT_PX}px)`).matches && !isCoarsePointer;

  if (isDesktopWeb) return 'web';
  if (isCoarsePointer) return width >= TABLET_BREAKPOINT_PX ? 'tablet' : 'phone';
  if (width >= WEB_BREAKPOINT_PX) return 'web';
  if (width >= TABLET_BREAKPOINT_PX) return 'tablet';
  return 'phone';
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

function stripTokenPrefix(value: string): string | undefined {
  if (!value.startsWith(TOKEN_PREFIX)) return undefined;
  return value.slice(TOKEN_PREFIX.length);
}

function resolveFormFactorScalar<T extends number>(
  matrix: FormFactorMatrix<T>,
  formFactor: FormFactor,
): number {
  return matrix[formFactor];
}

/**
 * Resolve a `LayoutScalar` (number | `token:…` string) to a CSS pixel value.
 *
 * Numeric values pass through unchanged. `token:<name>` is resolved through
 * the generated spacing/radius token registries. Unknown tokens log
 * `token_resolver_missing` at debug severity and fall back to 0 so the
 * payload still renders. Plain strings without the `token:` prefix are
 * not valid wire values for layout scalars and resolve to 0.
 */
export function resolveLayoutScalar(
  value: number | string | undefined,
  formFactor: FormFactor = currentFormFactor(),
  _theme: string = currentTheme(),
): number {
  if (value == null) return 0;
  if (typeof value === 'number') return value;

  const tokenName = stripTokenPrefix(value);
  if (!tokenName) return 0;

  const spacing = LayoutTokenRegistry.spacing[tokenName as keyof typeof LayoutTokenRegistry.spacing];
  if (spacing) return resolveFormFactorScalar(spacing, formFactor);

  const radius = LayoutTokenRegistry.radius[tokenName as keyof typeof LayoutTokenRegistry.radius];
  if (radius) return resolveFormFactorScalar(radius, formFactor);

  if (typeof console !== 'undefined') {
    console.debug('token_resolver_missing', value);
  }
  return 0;
}

export function resolveSpacing(
  value: number | string | undefined,
  formFactor: FormFactor = currentFormFactor(),
  theme: string = currentTheme(),
): number {
  return resolveLayoutScalar(value, formFactor, theme);
}

function normalizeEnvelope(envelope: WebSizeEnvelope): TypographySizeEnvelope {
  return {
    kind: 'envelope',
    min: envelope.min,
    max: envelope.max,
    minVw: envelope.minVw,
    maxVw: envelope.maxVw,
  };
}

function resolveTypographySize(
  variant: TypographyVariantSpec,
  formFactor: FormFactor,
): number | TypographySizeEnvelope {
  if (formFactor !== 'web') return variant.size[formFactor];
  const webSize = variant.size.web;
  return typeof webSize === 'number' ? webSize : normalizeEnvelope(webSize);
}

export function resolveTypography(
  token: string,
  formFactor: FormFactor = currentFormFactor(),
): TypographySpec | undefined {
  const tokenName = stripTokenPrefix(token);
  if (!tokenName) return undefined;

  const variant = LayoutTokenRegistry.typographyVariants[
    tokenName as keyof typeof LayoutTokenRegistry.typographyVariants
  ] as TypographyVariantSpec | undefined;
  if (!variant) return undefined;

  const category = LayoutTokenRegistry.typographyCategories[
    variant.categoryRef as keyof typeof LayoutTokenRegistry.typographyCategories
  ] as TypographyCategorySpec | undefined;
  if (!category) return undefined;

  return {
    familyRef: category.familyRef,
    weight: category.weight,
    textCase: category.textCase,
    lineHeight: category.lineHeight,
    size: resolveTypographySize(variant, formFactor),
  };
}

export function resolveShadowToken(token: string): ShadowSpec | undefined {
  const tokenName = stripTokenPrefix(token);
  if (!tokenName) return undefined;
  return LayoutTokenRegistry.shadows[tokenName as keyof typeof LayoutTokenRegistry.shadows];
}

export function resolveShadowOrToken(value: Shadow | string | undefined): Shadow | undefined {
  if (value === undefined) return undefined;
  if (typeof value === 'string') return tokenToShadow(value);
  return value;
}

export function resolveShadowOrTokens(values: Array<Shadow | string> | undefined): Shadow[] {
  if (!values) return [];
  return values.map((v) => resolveShadowOrToken(v)).filter((s): s is Shadow => s !== undefined);
}

export function resolveMotionDuration(
  token: string,
  formFactor: FormFactor = currentFormFactor(),
): number | undefined {
  const tokenName = stripTokenPrefix(token);
  if (!tokenName) return undefined;
  const duration = LayoutTokenRegistry.motionDuration[
    tokenName as keyof typeof LayoutTokenRegistry.motionDuration
  ];
  if (!duration) return undefined;
  return resolveFormFactorScalar(duration, formFactor);
}

export function resolveMotionEasing(token: string): string | undefined {
  const tokenName = stripTokenPrefix(token);
  if (!tokenName) return undefined;
  return LayoutTokenRegistry.motionEasing[tokenName as keyof typeof LayoutTokenRegistry.motionEasing];
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
    case '4:3': return 4 / 3;
    case '1:1': return 1;
    case '3:2': return 3 / 2;
    case '21:9': return 21 / 9;
    default: return undefined;
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
 */
export function resolveSpacingPx(
  spacing: SpacingInput | undefined,
  formFactor: FormFactor = currentFormFactor(),
  theme: string = currentTheme(),
): ResolvedSpacingPx {
  if (!spacing) return { top: 0, bottom: 0, left: 0, right: 0 };
  return {
    top: resolveLayoutScalar(spacing.top, formFactor, theme),
    bottom: resolveLayoutScalar(spacing.bottom, formFactor, theme),
    left: resolveLayoutScalar(spacing.start, formFactor, theme),
    right: resolveLayoutScalar(spacing.end, formFactor, theme),
  };
}

/** Test-only: surface KNOWN_FORM_FACTORS for callers that want to validate
 * a string before forwarding it as a form-factor override. */
export function isFormFactor(value: string): value is FormFactor {
  return KNOWN_FORM_FACTORS.has(value as FormFactor);
}

function tokenToShadow(token: string): Shadow | undefined {
  const spec = resolveShadowToken(token);
  if (!spec) return undefined;
  return {
    type: (spec.type === 'inner' ? 'inner' : 'drop') as ShadowType,
    color: spec.color,
    radius: spec.radius,
    offsetX: spec.offsetX,
    offsetY: spec.offsetY,
  };
}
