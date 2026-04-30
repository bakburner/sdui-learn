import React, { useContext, memo } from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import { CompositeContentContext, resolveBindRefString } from '../../utils/BindRefResolver';
import { areAtomicPropsEqual } from './areAtomicPropsEqual';
import { getActivateActions } from './getActivateActions';

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
  score:          { fontSize: 28, fontWeight: 800, lineHeight: '1em',    fontFamily: 'var(--font-headline)', fontVariantNumeric: 'tabular-nums' },
};

const weightMap: Record<string, number> = {
  thin: 100, extraLight: 200, light: 300, normal: 400,
  medium: 500, semiBold: 600, bold: 700, extraBold: 800, black: 900,
};

/**
 * AtomicText — renders typography-styled text. The text span carries only
 * typography concerns (font / color / alignment / line clamp); margin,
 * padding, background, border, shadow, cornerRadius, opacity, and
 * variant chrome are applied by AtomicBox.
 */
function AtomicTextInner({ element, onAction }: AtomicProps): React.ReactElement {
  const textAlignMap: Record<string, React.CSSProperties['textAlign']> = {
    start: 'left', center: 'center', end: 'right',
  };

  const resolveColor = useColorTokenResolver();
  // Resolve `content` from `bindRef` when present, falling back to the
  // inline `content` property. A leaf with a bindRef but no matching
  // `data.content` entry falls back to its inline value so the first
  // paint is usable while the first real-time update is in flight.
  const compositeContent = useContext(CompositeContentContext);
  const resolvedContent = resolveBindRefString(element.bindRef, compositeContent) ?? element.content ?? '';

  let baseStyle: React.CSSProperties = {};
  if (element.variant != null) {
    const matched = variantStyles[element.variant];
    if (matched) {
      baseStyle = matched;
    } else {
      console.warn('variant_resolver_missing', { variant: element.variant, elementId: element.id });
    }
  }
  const resolvedColor = resolveColor(element.color);
  const textStyle: React.CSSProperties = {
    ...baseStyle,
    ...(element.weight ? { fontWeight: weightMap[element.weight] ?? 400 } : {}),
    ...(resolvedColor ? { color: resolvedColor } : {}),
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
  let inner: React.ReactElement;
  if (a11y?.role === 'heading' && a11y.headingLevel) {
    const Tag = `h${a11y.headingLevel}` as keyof React.JSX.IntrinsicElements;
    inner = <Tag style={textStyle} {...accessibilityProps(a11y)}>{resolvedContent}</Tag>;
  } else {
    inner = <span style={textStyle} {...accessibilityProps(a11y)}>{resolvedContent}</span>;
  }

  const hasActions = element.actions && element.actions.length > 0;
  const handleClick = hasActions
    ? (e: React.MouseEvent) => {
        e.stopPropagation();
        const actions = getActivateActions(element.actions as Action[] | undefined);
        if (actions.length > 0) onAction(actions);
      }
    : undefined;

  return (
    <AtomicBox
      element={element}
      onClick={handleClick}
      styleOverrides={hasActions ? { cursor: 'pointer' } : undefined}
    >
      {inner}
    </AtomicBox>
  );
}

export const AtomicText = memo(AtomicTextInner, areAtomicPropsEqual);
