/**
 * ContainerVariantResolver — resolves the server-emitted `variant` wire
 * string on a Container atomic element into a web-native style spec.
 *
 * The canonical token catalog lives in `schema/style-tokens.json`; the
 * specs below mirror the `web` tier for each variant. Unknown variants
 * log a diagnostic and fall through to the primitive default (renderer
 * keeps its existing behavior).
 *
 * Each spec carries an `overrideMatrix` describing which inline style
 * axes may override the variant's defaults. Renderers consult this
 * matrix when both a variant and an inline value for the same axis are
 * present: `allow` means inline wins; `lock` means variant wins and the
 * inline attempt is logged via `logVariantOverrideBlocked`.
 *
 * CSS custom properties use `var(--sdui-..., <fallback>)` form so a host
 * app can re-theme the surface family without a code change while still
 * rendering sensibly when no custom properties are supplied.
 */

export type ContainerVariantName = 'hero' | 'grouped';

export type OverridePolicy = 'allow' | 'lock';

export interface ContainerVariantSpec {
  cornerRadius?: number;
  /** Resolved `background` shorthand (color, gradient, or CSS variable reference). */
  backgroundCss?: string;
  /** CSS `box-shadow` value. */
  boxShadow?: string;
  /** CSS `backdrop-filter` value. Applied only when the browser supports it. */
  backdropFilter?: string;
  /** CSS `background-image` value for a layered gradient overlay. */
  gradientOverlay?: string;
  fillWidth?: boolean;
  /** CSS `border` shorthand. */
  border?: string;
  /** Per-axis override policy. Axes not listed default to `allow`. */
  overrideMatrix: Record<string, OverridePolicy>;
}

const SPECS: Record<ContainerVariantName, ContainerVariantSpec> = {
  hero: {
    cornerRadius: 16,
    backgroundCss:
      'linear-gradient(180deg, var(--sdui-surface-raised-top, #ffffff), var(--sdui-surface-raised-bottom, #f8f9fb))',
    boxShadow:
      '0 6px 18px rgba(0,0,0,0.12), 0 12px 24px rgba(0,0,0,0.08)',
    backdropFilter: 'blur(20px) saturate(140%)',
    overrideMatrix: {
      padding: 'allow',
      cornerRadius: 'allow',
      background: 'allow',
      shadow: 'lock',
      color: 'allow',
      gap: 'allow',
      opacity: 'allow',
      border: 'allow',
    },
  },

  grouped: {
    cornerRadius: 12,
    backgroundCss: 'var(--sdui-surface-raised, #ffffff)',
    border: '1px solid var(--sdui-outline, rgba(0,0,0,0.08))',
    overrideMatrix: {
      padding: 'allow',
      cornerRadius: 'allow',
      background: 'allow',
      shadow: 'allow',
      color: 'allow',
      gap: 'allow',
      opacity: 'allow',
      border: 'allow',
    },
  },
};

const KNOWN: ReadonlyArray<ContainerVariantName> = ['hero', 'grouped'];

export function resolveContainerVariant(
  variant?: string | null,
): ContainerVariantSpec | undefined {
  if (!variant) return undefined;
  if (!KNOWN.includes(variant as ContainerVariantName)) {
    if (typeof console !== 'undefined') {
      console.warn('variant_resolver_missing', { variant });
    }
    return undefined;
  }
  return SPECS[variant as ContainerVariantName];
}

/**
 * Emits a debug diagnostic when a locked axis on a variant is overridden
 * by an inline prop. The variant value wins; the inline value is ignored.
 */
export function logVariantOverrideBlocked(
  variant: string,
  axis: string,
  attemptedValue: unknown,
): void {
  if (typeof console !== 'undefined') {
    console.debug('variant_override_blocked', {
      variant,
      axis,
      attemptedValue,
    });
  }
}

/**
 * Returns true when the matrix permits an inline override on `axis`.
 * Axes not listed in the matrix default to `allow`.
 */
export function axisAllowsOverride(
  spec: ContainerVariantSpec,
  axis: string,
): boolean {
  const policy = spec.overrideMatrix[axis];
  return policy !== 'lock';
}

/**
 * Feature-detects `backdrop-filter` support at runtime. Returns true when
 * the browser reports it supports the supplied value (or any common
 * prefix). Safe to call in non-browser environments (returns false).
 */
export function supportsBackdropFilter(value: string): boolean {
  if (typeof CSS === 'undefined' || typeof CSS.supports !== 'function') {
    return false;
  }
  try {
    return (
      CSS.supports('backdrop-filter', value) ||
      CSS.supports('-webkit-backdrop-filter', value)
    );
  } catch {
    return false;
  }
}
