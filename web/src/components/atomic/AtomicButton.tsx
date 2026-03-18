import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicProps } from './AtomicRouter';
import { accessibilityProps } from '../../utils/accessibility';

const baseButtonStyle: React.CSSProperties = {
  cursor: 'pointer',
  padding: '8px 16px',
  borderRadius: 4,
  fontSize: 14,
  fontWeight: 500,
  border: 'none',
};

const variantStyles: Record<string, React.CSSProperties> = {
  filled:   { ...baseButtonStyle, backgroundColor: '#1565c0', color: '#fff' },
  outlined: { ...baseButtonStyle, backgroundColor: 'transparent', border: '1px solid #1565c0', color: '#1565c0' },
  text:     { ...baseButtonStyle, backgroundColor: 'transparent', color: '#1565c0', padding: '8px' },
};

/**
 * AtomicButton — renders a button with variant styling, dispatching actions on click.
 */
export function AtomicButton({ element, onAction }: AtomicProps): React.ReactElement {
  const style = variantStyles[element.buttonVariant ?? 'filled'] ?? variantStyles.filled;

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
