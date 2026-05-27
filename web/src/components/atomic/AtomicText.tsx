import React, { useContext, useEffect, memo } from 'react';
import type { Action } from '@sdui/models';
import { ActionTrigger } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import { CompositeContentContext, resolveBindRefString } from '../../utils/BindRefResolver';
import { areAtomicPropsEqual } from './areAtomicPropsEqual';
import { activationKeyboardProps, longPressPointerProps } from './atomicActionHandlers';
import { logUnsupportedAtomicTriggers, selectActions } from './getActivateActions';
import { LayoutTokenRegistry, type WebSize } from '../../tokens/LayoutTokenRegistry';

/**
 * Registry-driven typography map. The wire-level `variant` enum is a
 * presentational shorthand for the full token name `nba.typography.<variant>`;
 * sizing and base weight come from `schema/typography-tokens.json` so iOS,
 * Android, and web all render the same variant at the same size.
 *
 * Family-ref → CSS font-family stack mapping mirrors the production NBA
 * font choices (Knockout/Action NBA fallback to Roboto Condensed for
 * display/headline; Roboto for body; Roboto Mono for data/score numerics).
 */
const FAMILY_REF_TO_FONT: Record<string, string> = {
  'nba.font.knockout':         'var(--font-headline)',
  'nba.font.action.nba':       'var(--font-headline)',
  'nba.font.roboto.condensed': 'var(--font-headline)',
  'nba.font.roboto':           'var(--font-body)',
  'nba.font.roboto.mono':      'var(--font-mono)',
};

function webSizeToCss(size: WebSize): string {
  if (typeof size === 'number') return `${size}px`;
  // Fluid envelope → CSS clamp(min, vw-interpolant, max).
  const { min, max, minVw, maxVw } = size;
  const slope = (max - min) / (maxVw - minVw);
  const intercept = min - slope * minVw;
  return `clamp(${min}px, ${intercept.toFixed(4)}px + ${(slope * 100).toFixed(4)}vw, ${max}px)`;
}

function variantToStyle(variant: string): React.CSSProperties | undefined {
  const spec = LayoutTokenRegistry.typographyVariants[`nba.typography.${variant}`];
  if (!spec) return undefined;
  const category = LayoutTokenRegistry.typographyCategories[spec.categoryRef];
  if (!category) return undefined;
  const fontSize = webSizeToCss(spec.size.web);
  const style: React.CSSProperties = {
    fontSize,
    fontWeight: category.weight,
    lineHeight: category.lineHeight,
    fontFamily: FAMILY_REF_TO_FONT[category.familyRef] ?? 'var(--font-body)',
  };
  if (category.textCase === 'uppercase') {
    style.textTransform = 'uppercase';
  }
  return style;
}

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
  useEffect(() => {
    logUnsupportedAtomicTriggers(element.actions as Action[] | undefined, element.id);
  }, [element.actions, element.id]);

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
    const matched = variantToStyle(element.variant);
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
  const activateActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnActivate);
  const longPressActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnLongPress);
  const handleActivate = activateActions.length > 0
    ? () => { onAction(activateActions); }
    : undefined;
  const handleClick = handleActivate
    ? (e: React.MouseEvent) => {
        e.stopPropagation();
        handleActivate();
      }
    : undefined;

  return (
    <AtomicBox
      element={element}
      onClick={handleClick}
      role={handleActivate ? 'button' : undefined}
      extraProps={{
        ...(handleActivate ? activationKeyboardProps(handleActivate) : {}),
        ...(longPressActions.length > 0
          ? longPressPointerProps(() => onAction(longPressActions))
          : {}),
      }}
      styleOverrides={hasActions ? { cursor: 'pointer' } : undefined}
    >
      {inner}
    </AtomicBox>
  );
}

export const AtomicText = memo(AtomicTextInner, areAtomicPropsEqual);
