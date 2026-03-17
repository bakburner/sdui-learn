import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { resolveBackgroundCSS } from '../../utils/background';

/**
 * AtomicContainer — renders a flex row or column with gap, padding,
 * background color, optional gradient, flex children, and responsive breakpoint.
 *
 * Flex: When a child has a non-null `flex` value, it receives proportional
 * flex-grow along the main axis (like CSS flex-grow). Children without flex
 * size to content.
 *
 * Breakpoint: When set and direction is "row", the container flips to column
 * below the breakpoint width using a CSS media query. This replaces the old
 * Row section type with a purely atomic, server-composed primitive.
 */
export function AtomicContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const isRow = element.direction === 'row';
  const hasBreakpoint = isRow && element.breakpoint != null;

  // Unique class for scoped responsive CSS
  const className = hasBreakpoint ? `sdui-ac-${element.id ?? depth}` : undefined;

  const style: React.CSSProperties = {
    display: 'flex',
    flexDirection: isRow ? 'row' : 'column',
    gap: element.gap,
  };

  // alignment → justify-content
  switch (element.alignment) {
    case 'center':       style.justifyContent = 'center'; break;
    case 'end':          style.justifyContent = 'flex-end'; break;
    case 'spaceBetween': style.justifyContent = 'space-between'; break;
    case 'spaceAround':  style.justifyContent = 'space-around'; break;
    case 'spaceEvenly':  style.justifyContent = 'space-evenly'; break;
    default:             style.justifyContent = 'flex-start'; break;
  }

  // crossAlignment → align-items
  switch (element.crossAlignment) {
    case 'center':  style.alignItems = 'center'; break;
    case 'end':     style.alignItems = 'flex-end'; break;
    case 'stretch': style.alignItems = 'stretch'; break;
    default:        style.alignItems = 'flex-start'; break;
  }

  if (element.fillWidth) {
    style.width = '100%';
  }

  // padding
  if (element.padding) {
    const { top, end, bottom, start } = element.padding;
    style.padding = `${top}px ${end}px ${bottom}px ${start}px`;
  }

  // corner radius
  if (element.cornerRadius != null) {
    style.borderRadius = element.cornerRadius;
    style.overflow = 'hidden';
  }

  // background
  if (element.background) {
    Object.assign(style, resolveBackgroundCSS(element.background));
  }

  return (
    <div className={className} style={style}>
      {element.children?.map((child, i) => {
        const childStyle: React.CSSProperties | undefined =
          child.flex != null && child.flex > 0
            ? { flex: `${child.flex} 1 0%`, minWidth: 0 }
            : undefined;

        return childStyle ? (
          <div key={child.id ?? i} style={childStyle}>
            <AtomicRouter element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
          </div>
        ) : (
          <AtomicRouter key={child.id ?? i} element={child} state={state} onAction={onAction} depth={depth} onStateChange={onStateChange} sectionSlotDepth={sectionSlotDepth} />
        );
      })}

      {/* Responsive breakpoint CSS — flip row→column below threshold */}
      {hasBreakpoint && className && (
        <style>{`
          @media (max-width: ${element.breakpoint}px) {
            .${className} {
              flex-direction: column !important;
            }
          }
        `}</style>
      )}
    </div>
  );
}
