import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';

/**
 * AtomicScrollContainer — renders children in a scrollable row or column.
 * Uses native CSS overflow scroll; paging via CSS scroll-snap when enabled.
 */
export function AtomicScrollContainer({ element, state, onAction, depth = 0 }: AtomicProps): React.ReactElement {
  const isHorizontal = element.direction !== 'column';
  const children = element.children ?? [];

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isHorizontal ? 'row' : 'column',
    flexWrap: 'nowrap',
    gap: element.gap,
    overflowX: isHorizontal ? 'auto' : undefined,
    overflowY: isHorizontal ? undefined : 'auto',
    marginTop: 8,
    paddingBottom: isHorizontal ? 8 : undefined,
    maxWidth: isHorizontal ? '100%' : undefined,
    maxHeight: isHorizontal ? undefined : '100%',
    minWidth: 0,
    ...(element.paging ? {
      scrollSnapType: isHorizontal ? 'x mandatory' : 'y mandatory',
    } : {}),
  };

  const childStyle: React.CSSProperties = {
    flexShrink: 0,
    ...(element.paging ? { scrollSnapAlign: element.snapAlignment ?? 'start' } : {}),
  };

  return (
    <div style={style}>
      {children.map((child, i) => (
        <div key={child.id ?? i} style={childStyle}>
          <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} />
        </div>
      ))}
    </div>
  );
}
