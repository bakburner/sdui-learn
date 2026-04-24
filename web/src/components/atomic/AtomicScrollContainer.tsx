import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * AtomicScrollContainer — renders children in a scrollable row or column.
 * Uses native CSS overflow scroll; paging via CSS scroll-snap when enabled.
 *
 * AtomicBox owns margin / padding / background / cornerRadius / shadow /
 * border / opacity. This renderer owns only the scroll layout CSS
 * (display/flex/gap/overflow/scroll-snap), passed to AtomicBox as
 * layoutStyle so the box-model and scroll viewport live on the same DOM
 * node.
 */
export function AtomicScrollContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const isHorizontal = element.direction !== 'column';
  const children = element.children ?? [];

  const layoutStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: isHorizontal ? 'row' : 'column',
    flexWrap: 'nowrap',
    gap: element.gap,
    overflowX: isHorizontal ? 'auto' : undefined,
    overflowY: isHorizontal ? undefined : 'auto',
    maxWidth: isHorizontal ? '100%' : undefined,
    maxHeight: isHorizontal ? undefined : '100%',
    minWidth: 0,
    ...(element.paging ? {
      scrollSnapType: isHorizontal ? 'x mandatory' : 'y mandatory',
    } : {}),
    ...(element.showIndicators === false ? { scrollbarWidth: 'none' as const } : {}),
  };

  const hideScrollbarClass = element.showIndicators === false ? 'sdui-hide-scrollbar' : undefined;

  const childStyle: React.CSSProperties = {
    flexShrink: 0,
    ...(element.paging ? { scrollSnapAlign: element.snapAlignment ?? 'start' } : {}),
  };

  return (
    <AtomicBox
      element={element}
      layoutStyle={layoutStyle}
      className={hideScrollbarClass}
      role="list"
      ariaLabel={element.accessibility?.label ?? 'Scrollable content'}
      extraProps={accessibilityProps(element.accessibility) as Record<string, unknown>}
    >
      {children.map((child, i) => (
        <div key={child.id ?? i} style={childStyle} role="listitem">
          <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
        </div>
      ))}
      {hideScrollbarClass && (
        <style>{`.sdui-hide-scrollbar::-webkit-scrollbar { display: none; }`}</style>
      )}
    </AtomicBox>
  );
}
