import React from 'react';
import type { AtomicProps } from './AtomicRouter';

/**
 * AtomicDivider — renders a horizontal or vertical divider line.
 */
export function AtomicDivider({ element }: AtomicProps): React.ReactElement {
  const thickness = element.thickness ?? 1;
  const color = element.color ?? 'var(--divider)';
  const isVertical = element.orientation === 'vertical';

  const style: React.CSSProperties = isVertical
    ? { width: thickness, alignSelf: 'stretch', backgroundColor: color }
    : { height: thickness, width: '100%', backgroundColor: color };

  return <div style={style} aria-hidden="true" />;
}
