import React from 'react';
import type { AtomicProps } from './AtomicRouter';

/** Map schema variant strings to CSS font sizes / weights. */
const variantStyles: Record<string, React.CSSProperties> = {
  displayLarge:   { fontSize: 57, fontWeight: 400, lineHeight: '64px' },
  displayMedium:  { fontSize: 45, fontWeight: 400, lineHeight: '52px' },
  displaySmall:   { fontSize: 36, fontWeight: 400, lineHeight: '44px' },
  headlineLarge:  { fontSize: 32, fontWeight: 400, lineHeight: '40px' },
  headlineMedium: { fontSize: 28, fontWeight: 400, lineHeight: '36px' },
  headlineSmall:  { fontSize: 24, fontWeight: 400, lineHeight: '32px' },
  titleLarge:     { fontSize: 22, fontWeight: 500, lineHeight: '28px' },
  titleMedium:    { fontSize: 16, fontWeight: 500, lineHeight: '24px' },
  titleSmall:     { fontSize: 14, fontWeight: 500, lineHeight: '20px' },
  bodyLarge:      { fontSize: 16, fontWeight: 400, lineHeight: '24px' },
  bodyMedium:     { fontSize: 14, fontWeight: 400, lineHeight: '20px' },
  bodySmall:      { fontSize: 12, fontWeight: 400, lineHeight: '16px' },
  labelLarge:     { fontSize: 14, fontWeight: 500, lineHeight: '20px' },
  labelMedium:    { fontSize: 12, fontWeight: 500, lineHeight: '16px' },
  labelSmall:     { fontSize: 11, fontWeight: 500, lineHeight: '16px' },
};

const weightMap: Record<string, number> = {
  thin: 100, extraLight: 200, light: 300, normal: 400,
  medium: 500, semiBold: 600, bold: 700, extraBold: 800, black: 900,
};

/**
 * AtomicText — renders a text span with Material-style typography variants.
 */
export function AtomicText({ element }: AtomicProps): React.ReactElement {
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
  };

  return <span style={style}>{element.content ?? ''}</span>;
}
