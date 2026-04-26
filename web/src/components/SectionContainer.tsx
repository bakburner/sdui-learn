import React, { useMemo } from 'react';
import type { SectionSurface, Spacing, Background } from '@sdui/models';
import { resolveColorToken, usePrefersColorScheme } from '../utils/ColorTokenResolver';

export interface SectionContainerProps {
  surface?: SectionSurface;
  children: React.ReactNode;
}

/**
 * Shared section-surface wrapper applied by `SectionRouter` to every
 * permanent section. Reads `section.surface` (margin, padding,
 * background, cornerRadius, shadow, border) and applies it as
 * CSS, so permanent-section renderers never set their own outer
 * chrome.
 *
 * Shared wrapper enforcing server-driven outer chrome for every section.
 * See `SduiUtils.defaultSurface()` on the server for the default
 * surface values composers emit.
 */
export function SectionContainer({ surface, children }: SectionContainerProps): React.ReactElement {
  const scheme = usePrefersColorScheme();

  if (!surface) {
    return <>{children}</>;
  }

  const marginStyle = spacingToCss(surface.margin, 'margin');
  const paddingStyle = spacingToCss(surface.padding, 'padding');

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
    if (!surface.shadow) return undefined;
    const shadowColor = resolveColorToken(surface.shadow.color, scheme) ?? 'rgba(0,0,0,0.08)';
    return `${surface.shadow.offsetX ?? 0}px ${surface.shadow.offsetY ?? 2}px ${surface.shadow.radius ?? 4}px 0 ${shadowColor}`;
  }, [surface.shadow, scheme]);

  const style: React.CSSProperties = {
    ...marginStyle,
    ...paddingStyle,
    background,
    borderRadius: surface.cornerRadius,
    border: borderStyle,
    boxShadow,
    overflow: surface.cornerRadius ? 'hidden' : undefined,
  };

  return <div style={style}>{children}</div>;
}

function spacingToCss(s: Spacing | undefined, prefix: 'margin' | 'padding'): React.CSSProperties {
  if (!s) return {};
  return {
    [`${prefix}Top`]: s.top,
    [`${prefix}Bottom`]: s.bottom,
    [`${prefix}Left`]: s.start,
    [`${prefix}Right`]: s.end,
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
