import React from 'react';
import type { SectionDisplay, Spacing, Background } from '@sdui/models';
import { resolveColorToken, usePrefersColorScheme } from '../utils/ColorTokenResolver';

export interface SectionContainerProps {
  display?: SectionDisplay;
  children: React.ReactNode;
}

/**
 * Shared outer-chrome wrapper applied by `SectionRouter` to every
 * permanent section. Reads `section.display` (margin, padding,
 * background, cornerRadius, shadow, border) and applies it as
 * CSS, so permanent-section renderers never set their own outer
 * chrome.
 *
 * See AGENTS.md §15.3 for the governance rule this wrapper enforces,
 * and `SduiUtils.defaultSectionDisplay()` on the server for the
 * default chrome values composers emit.
 */
export function SectionContainer({ display, children }: SectionContainerProps): React.ReactElement {
  const scheme = usePrefersColorScheme();

  if (!display) {
    return <>{children}</>;
  }

  const marginStyle = spacingToCss(display.margin, 'margin');
  const paddingStyle = spacingToCss(display.padding, 'padding');
  const background = resolveBackgroundCss(display.background, scheme);
  const borderColor = resolveColorToken(display.border?.color, scheme);
  const borderStyle =
    display.border && borderColor && (display.border.width ?? 1) > 0
      ? `${display.border.width ?? 1}px solid ${borderColor}`
      : undefined;

  const shadowColor = resolveColorToken(display.shadow?.color, scheme) ?? 'rgba(0,0,0,0.08)';
  const boxShadow = display.shadow
    ? `${display.shadow.offsetX ?? 0}px ${display.shadow.offsetY ?? 2}px ${display.shadow.radius ?? 4}px 0 ${shadowColor}`
    : undefined;

  const style: React.CSSProperties = {
    ...marginStyle,
    ...paddingStyle,
    background,
    borderRadius: display.cornerRadius,
    border: borderStyle,
    boxShadow,
    overflow: display.cornerRadius ? 'hidden' : undefined,
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
