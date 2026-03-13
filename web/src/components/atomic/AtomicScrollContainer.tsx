import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';

/**
 * AtomicScrollContainer — renders children in a scrollable row or column.
 * Uses native CSS overflow scroll; paging via CSS scroll-snap when enabled.
 */
export function AtomicScrollContainer({ element, state, onAction }: AtomicProps): React.ReactElement {
  const isHorizontal = element.direction !== 'column';
  const children = element.children ?? [];

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isHorizontal ? 'row' : 'column',
    gap: element.gap,
    overflowX: isHorizontal ? 'auto' : undefined,
    overflowY: isHorizontal ? undefined : 'auto',
    ...(element.paging ? {
      scrollSnapType: isHorizontal ? 'x mandatory' : 'y mandatory',
    } : {}),
  };

  const childSnapStyle: React.CSSProperties | undefined = element.paging
    ? { scrollSnapAlign: element.snapAlignment ?? 'start' }
    : undefined;

  return (
    <div style={style}>
      {children.map((child, i) => (
        <div key={child.id ?? i} style={childSnapStyle}>
          <AtomicRouter element={child} state={state} onAction={onAction} />
        </div>
      ))}
    </div>
  );
}
