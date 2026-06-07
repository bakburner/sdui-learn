import React, { useContext, useEffect, memo } from 'react';
import type { Action } from '@sdui/models';
import { ActionTrigger } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';
import { CompositeContentContext, resolveBindRefString } from '../../utils/BindRefResolver';
import { areAtomicPropsEqual } from './areAtomicPropsEqual';
import { logUnsupportedAtomicTriggers, selectActions } from './getActivateActions';
import { IconTokenResolver } from '../../utils/IconTokenResolver';

const baseButtonStyle: React.CSSProperties = {
  cursor: 'pointer',
  padding: '10px 20px',
  borderRadius: 4,
  fontSize: 14,
  fontWeight: 600,
  fontFamily: 'var(--font-body)',
  border: 'none',
  letterSpacing: '0.01em',
  transition: 'opacity 150ms ease',
};

const KNOWN_BUTTON_VARIANTS = ['primary', 'secondary', 'tertiary', 'text'] as const;
type KnownButtonVariant = typeof KNOWN_BUTTON_VARIANTS[number];

const variantStyles: Record<KnownButtonVariant, React.CSSProperties> = {
  primary:   { ...baseButtonStyle, backgroundColor: 'var(--button)', color: 'var(--button-text)' },
  secondary: { ...baseButtonStyle, backgroundColor: 'transparent', border: '1px solid var(--button)', color: 'var(--button)' },
  tertiary:  { ...baseButtonStyle, backgroundColor: 'transparent', color: 'var(--button)', padding: '10px' },
  text:      { ...baseButtonStyle, backgroundColor: 'transparent', color: 'var(--link)', padding: '10px' },
};

/**
 * AtomicButton — renders a button with variant-driven appearance.
 *
 * The button's visual chrome (padding, bg, border, radius, font) comes
 * from the variant preset. AtomicBox is applied *outside* the <button>
 * so element-level margin / opacity / width / badge etc. sit on a
 * wrapper div and don't interfere with the native button's click
 * surface.
 *
 * Inline `padding` / `background` / `cornerRadius` / `border` on a
 * Button still go to AtomicBox's wrapper (which is rare; typical
 * usage relies entirely on the variant preset). If the server wants
 * to tune the button's own chrome, the primitive variant should be
 * extended rather than using element.padding.
 */
function AtomicButtonInner({ element, onAction }: AtomicProps): React.ReactElement {
  useEffect(() => {
    logUnsupportedAtomicTriggers(element.actions as Action[] | undefined, element.id);
  }, [element.actions, element.id]);

  const resolveColor = useColorTokenResolver();
  const rawVariant = element.variant;
  let resolvedVariant: KnownButtonVariant = 'primary';
  if (rawVariant != null) {
    if ((KNOWN_BUTTON_VARIANTS as readonly string[]).includes(rawVariant)) {
      resolvedVariant = rawVariant as KnownButtonVariant;
    } else {
      console.warn('variant_resolver_missing', { variant: rawVariant, elementId: element.id });
    }
  }
  const resolvedColor = resolveColor(element.color);
  // `backgrounds` is an array of token-or-struct values: each entry is a
  // string (token ref or raw color) or an object with `color` / gradient
  // `colors`. Buttons consume only the bottommost layer for the solid
  // background-color hint; multi-layer backgrounds aren't meaningful here.
  let resolvedBg: string | undefined;
  const firstBg = element.backgrounds && element.backgrounds.length > 0
    ? (element.backgrounds[0] as unknown)
    : undefined;
  if (typeof firstBg === 'string') {
    resolvedBg = resolveColor(firstBg);
  } else if (firstBg && typeof firstBg === 'object' && 'color' in (firstBg as Record<string, unknown>)) {
    resolvedBg = resolveColor((firstBg as Record<string, unknown>).color as string);
  }
  const isTextLike = resolvedVariant === 'text' || resolvedVariant === 'tertiary';
  const buttonStyle: React.CSSProperties = {
    ...variantStyles[resolvedVariant],
    ...(resolvedColor ? { color: resolvedColor } : {}),
    ...(resolvedBg
      ? { backgroundColor: resolvedBg }
      : isTextLike
        ? { backgroundColor: 'transparent' }
        : {}),
  };

  const iconLigature = element.icon ? IconTokenResolver.resolve(element.icon) : undefined;
  if (element.icon && !iconLigature) {
    console.warn('[AtomicButton] icon not mapped', { icon: element.icon, elementId: element.id });
  }

  const activateActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnActivate);
  const focusActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnFocus);
  const blurActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnBlur);
  const submitActions = selectActions(element.actions as Action[] | undefined, ActionTrigger.OnSubmit);

  const handleClick: React.MouseEventHandler<HTMLButtonElement> = (event) => {
    if (submitActions.length > 0 && event.currentTarget.form) {
      event.preventDefault();
      onAction(submitActions);
      return;
    }
    if (activateActions.length > 0) {
      onAction(activateActions);
    }
  };

  // Resolve `label` from `bindRef` when present, falling back to the
  // inline `label`. Lets composers rebind CTA copy without rewriting
  // the ui tree.
  const compositeContent = useContext(CompositeContentContext);
  const resolvedLabel = resolveBindRefString(element.bindRef, compositeContent) ?? element.label ?? '';
  const showLabel = resolvedLabel.trim().length > 0;

  const button = (
    <button
      type={submitActions.length > 0 ? 'submit' : 'button'}
      style={{
        ...buttonStyle,
        display: 'inline-flex',
        alignItems: 'center',
        gap: iconLigature && showLabel ? 8 : 0,
      }}
      disabled={element.disabled}
      onClick={handleClick}
      onFocus={focusActions.length > 0 ? () => onAction(focusActions) : undefined}
      onBlur={blurActions.length > 0 ? () => onAction(blurActions) : undefined}
      aria-label={element.accessibility?.label ?? (showLabel ? resolvedLabel : iconLigature ?? '')}
      {...accessibilityProps(element.accessibility)}
    >
      {iconLigature != null && (
        <span className="material-icons" style={{ fontSize: 20, lineHeight: 1 }}>
          {iconLigature}
        </span>
      )}
      {showLabel ? resolvedLabel : null}
    </button>
  );

  return <AtomicBox element={element}>{button}</AtomicBox>;
}

export const AtomicButton = memo(AtomicButtonInner, areAtomicPropsEqual);
