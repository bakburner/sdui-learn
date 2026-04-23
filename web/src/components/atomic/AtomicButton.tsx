import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { accessibilityProps } from '../../utils/accessibility';
import { useColorTokenResolver } from '../../utils/ColorTokenResolver';

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
 * AtomicButton — renders a button with variant styling, dispatching actions on click.
 */
export function AtomicButton({ element, onAction }: AtomicProps): React.ReactElement {
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
  const style: React.CSSProperties = {
    ...variantStyles[resolvedVariant],
    ...(resolvedColor ? { color: resolvedColor } : {}),
  };

  const handleClick = () => {
    if (element.actions?.length) {
      const action = element.actions[0] as unknown as Action;
      onAction(action);
    }
  };

  return (
    <button
      style={style}
      disabled={element.disabled}
      onClick={handleClick}
      aria-label={element.accessibility?.label ?? element.label}
      {...accessibilityProps(element.accessibility)}
    >
      {element.label ?? ''}
    </button>
  );
}
