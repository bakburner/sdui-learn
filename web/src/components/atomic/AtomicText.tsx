import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { accessibilityProps } from '../../utils/accessibility';

/** Map schema variant strings to CSS font sizes / weights — NBA typography system.
 *  Display/Headline use Roboto Condensed (approximating Knockout/Action NBA).
 *  Title/Body/Label use Roboto (matching production body font stack). */
const variantStyles: Record<string, React.CSSProperties> = {
  displayLarge:   { fontSize: 57, fontWeight: 800, lineHeight: '0.85em', fontFamily: 'var(--font-headline)', textTransform: 'uppercase' as const, letterSpacing: '-0.01em' },
  displayMedium:  { fontSize: 45, fontWeight: 800, lineHeight: '0.85em', fontFamily: 'var(--font-headline)', textTransform: 'uppercase' as const, letterSpacing: '-0.01em' },
  displaySmall:   { fontSize: 36, fontWeight: 700, lineHeight: '0.9em',  fontFamily: 'var(--font-headline)', textTransform: 'uppercase' as const },
  headlineLarge:  { fontSize: 32, fontWeight: 700, lineHeight: '1.1em',  fontFamily: 'var(--font-headline)' },
  headlineMedium: { fontSize: 28, fontWeight: 700, lineHeight: '1.1em',  fontFamily: 'var(--font-headline)' },
  headlineSmall:  { fontSize: 24, fontWeight: 700, lineHeight: '1.15em', fontFamily: 'var(--font-headline)' },
  titleLarge:     { fontSize: 22, fontWeight: 500, lineHeight: '28px',   fontFamily: 'var(--font-body)' },
  titleMedium:    { fontSize: 16, fontWeight: 500, lineHeight: '24px',   fontFamily: 'var(--font-body)' },
  titleSmall:     { fontSize: 14, fontWeight: 500, lineHeight: '20px',   fontFamily: 'var(--font-body)' },
  bodyLarge:      { fontSize: 16, fontWeight: 400, lineHeight: '24px',   fontFamily: 'var(--font-body)' },
  bodyMedium:     { fontSize: 14, fontWeight: 400, lineHeight: '20px',   fontFamily: 'var(--font-body)' },
  bodySmall:      { fontSize: 12, fontWeight: 400, lineHeight: '16px',   fontFamily: 'var(--font-body)' },
  labelLarge:     { fontSize: 14, fontWeight: 500, lineHeight: '20px',   fontFamily: 'var(--font-body)', letterSpacing: '0.02em' },
  labelMedium:    { fontSize: 12, fontWeight: 500, lineHeight: '16px',   fontFamily: 'var(--font-body)', letterSpacing: '0.02em' },
  labelSmall:     { fontSize: 11, fontWeight: 500, lineHeight: '16px',   fontFamily: 'var(--font-body)', letterSpacing: '0.04em', textTransform: 'uppercase' as const },
  // Score-specific variant: monospaced numerals for live scores
  score:          { fontSize: 28, fontWeight: 800, lineHeight: '1em',    fontFamily: 'var(--font-headline)', fontVariantNumeric: 'tabular-nums' },
};

const weightMap: Record<string, number> = {
  thin: 100, extraLight: 200, light: 300, normal: 400,
  medium: 500, semiBold: 600, bold: 700, extraBold: 800, black: 900,
};

/**
 * AtomicText — renders a text span with Material-style typography variants.
 */
export function AtomicText({ element }: AtomicProps): React.ReactElement {
  const textAlignMap: Record<string, React.CSSProperties['textAlign']> = {
    start: 'left', center: 'center', end: 'right',
  };

  const baseStyle = element.variant ? variantStyles[element.variant] ?? {} : {};
  const style: React.CSSProperties = {
    ...baseStyle,
    ...(element.weight ? { fontWeight: weightMap[element.weight] ?? 400 } : {}),
    ...(element.color ? { color: element.color } : {}),
    ...(element.maxLines ? {
      display: '-webkit-box',
      WebkitLineClamp: element.maxLines,
      WebkitBoxOrient: 'vertical' as const,
      overflow: 'hidden',
    } : {}),
    ...(element.textAlign ? { textAlign: textAlignMap[element.textAlign] } : {}),
    ...(element.monospacedDigits ? { fontVariantNumeric: 'tabular-nums' } : {}),
  };

  const a11y = element.accessibility;
  if (a11y?.role === 'heading' && a11y.headingLevel) {
    const Tag = `h${a11y.headingLevel}` as keyof React.JSX.IntrinsicElements;
    return <Tag style={style} {...accessibilityProps(a11y)}>{element.content ?? ''}</Tag>;
  }
  return <span style={style} {...accessibilityProps(a11y)}>{element.content ?? ''}</span>;
}
