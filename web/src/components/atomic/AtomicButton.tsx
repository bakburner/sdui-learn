import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { accessibilityProps } from '../../utils/accessibility';

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

const variantStyles: Record<string, React.CSSProperties> = {
  primary:   { ...baseButtonStyle, backgroundColor: 'var(--button)', color: 'var(--button-text)' },
  secondary: { ...baseButtonStyle, backgroundColor: 'transparent', border: '1px solid var(--button)', color: 'var(--button)' },
  tertiary:  { ...baseButtonStyle, backgroundColor: 'transparent', color: 'var(--button)', padding: '10px' },
  text:      { ...baseButtonStyle, backgroundColor: 'transparent', color: 'var(--link)', padding: '10px' },
};

/**
 * AtomicButton — renders a button with variant styling, dispatching actions on click.
 */
export function AtomicButton({ element, onAction }: AtomicProps): React.ReactElement {
  const style = variantStyles[element.buttonVariant ?? 'primary'] ?? variantStyles.primary;

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
