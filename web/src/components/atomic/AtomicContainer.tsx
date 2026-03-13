import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';

/**
 * AtomicContainer — renders a flex row or column with gap, padding,
 * background color, and optional gradient.
 */
export function AtomicContainer({ element, state, onAction, depth = 0 }: AtomicProps): React.ReactElement {
  const isRow = element.direction === 'row';
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
  if (element.backgroundGradient) {
    const { colors, direction } = element.backgroundGradient;
    const dir = direction === 'horizontal' ? 'to right'
              : direction === 'diagonal' ? 'to bottom right'
              : 'to bottom';
    style.background = `linear-gradient(${dir}, ${colors.join(', ')})`;
  } else if (element.backgroundColor) {
    style.backgroundColor = element.backgroundColor;
  }

  return (
    <div style={style}>
      {element.children?.map((child, i) => (
        <AtomicRouter key={child.id ?? i} element={child} state={state} onAction={onAction} depth={depth} />
      ))}
    </div>
  );
}
