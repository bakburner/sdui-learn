import React, { useMemo } from 'react';
import type { SectionSurface, Spacing, Background } from '@sdui/models';
import { resolveColorToken, usePrefersColorScheme } from '../utils/ColorTokenResolver';
import { currentFormFactor, resolveLayoutScalar, resolveShadowOrToken, resolveSpacingPx } from '../utils/LayoutTokenResolver';

export interface SectionContainerProps {
  surface?: SectionSurface;
  children: React.ReactNode;
}

/**
 * Shared section-surface wrapper applied by `SectionRouter` to every
 * semantic section. Reads `section.surface` (margin, padding,
 * background, cornerRadius, shadow, border) and applies it as
 * CSS, so semantic-section renderers never set their own outer
 * chrome.
 */
export function SectionContainer({ surface, children }: SectionContainerProps): React.ReactElement {
  const scheme = usePrefersColorScheme();
  const formFactor = currentFormFactor();

  if (!surface) {
    return <>{children}</>;
  }

  const marginStyle = spacingToCss(surface.margin, 'margin', formFactor, scheme);
  const paddingStyle = spacingToCss(surface.padding, 'padding', formFactor, scheme);

  const background = useMemo(
    () => resolveBackgroundCss(surface.background, scheme),
    [surface.background, scheme],
  );
  const borderColor = useMemo(
    () => resolveColorToken(surface.border?.color, scheme),
    [surface.border?.color, scheme],
  );
  const borderStyle = useMemo((): string | undefined => {
    if (!surface.border || !borderColor || (surface.border.width ?? 1) <= 0) {
      return undefined;
    }
    return `${surface.border.width ?? 1}px solid ${borderColor}`;
  }, [surface.border, borderColor]);

  const boxShadow = useMemo((): string | undefined => {
    const resolvedShadow = resolveShadowOrToken(surface.shadow);
    if (!resolvedShadow) return undefined;
    const shadowColor = resolveColorToken(resolvedShadow.color, scheme) ?? 'rgba(0,0,0,0.08)';
    return `${resolvedShadow.offsetX ?? 0}px ${resolvedShadow.offsetY ?? 2}px ${resolvedShadow.radius ?? 4}px 0 ${shadowColor}`;
  }, [surface.shadow, scheme]);

  const cornerRadiusPx = surface.cornerRadius != null
    ? resolveLayoutScalar(surface.cornerRadius, formFactor, scheme)
    : undefined;

  const style: React.CSSProperties = {
    ...marginStyle,
    ...paddingStyle,
    background,
    borderRadius: cornerRadiusPx,
    border: borderStyle,
    boxShadow,
    overflow: cornerRadiusPx ? 'hidden' : undefined,
  };

  return <div style={style}>{children}</div>;
}

function spacingToCss(
  s: Spacing | undefined,
  prefix: 'margin' | 'padding',
  formFactor: ReturnType<typeof currentFormFactor>,
  theme: string,
): React.CSSProperties {
  if (!s) return {};
  const resolved = resolveSpacingPx(s, formFactor, theme);
  return {
    [`${prefix}Top`]: resolved.top,
    [`${prefix}Bottom`]: resolved.bottom,
    [`${prefix}Left`]: resolved.left,
    [`${prefix}Right`]: resolved.right,
  };
}

function resolveBackgroundCss(
  bg: Background | string | undefined,
  scheme: 'light' | 'dark',
): string | undefined {
  if (!bg) return undefined;
  if (typeof bg === 'string') {
    return resolveColorToken(bg, scheme);
  }
  if (bg.colors && bg.colors.length > 0) {
    const stops = bg.colors
      .map((c) => resolveColorToken(c, scheme))
      .filter(Boolean)
      .join(', ');
    const direction = bg.direction as string | undefined;
    const angle =
      direction === 'horizontal' ? '90deg'
      : direction === 'diagonal' ? '135deg'
      : '180deg';
    return `linear-gradient(${angle}, ${stops})`;
  }
  return undefined;
}
