import React from 'react';
import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { AtomicBox, AtomicBoxBadge } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';

/**
 * AtomicContainer — renders a flex row or column with gap, flex children,
 * and optional responsive breakpoint. All box-model concerns (margin,
 * padding, background, cornerRadius, shadow, border, badge, variant,
 * backdrop-filter, width/height/fillWidth, opacity) are applied by
 * AtomicBox so the container only owns flex layout.
 *
 * Flex: When a child has a non-null `flex` value, it receives proportional
 * flex-grow along the main axis (like CSS flex-grow). Children without flex
 * size to content.
 *
 * Breakpoint: When set and direction is "row", the container flips to column
 * below the breakpoint width using a CSS media query.
 */
export function AtomicContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement {
  const isRow = element.direction === 'row';
  const hasBreakpoint = isRow && element.breakpoint != null;
  const className = hasBreakpoint ? `sdui-ac-${element.id ?? depth}` : undefined;

  const layoutStyle: React.CSSProperties = {
    display: 'flex',
    flexDirection: isRow ? 'row' : 'column',
    gap: element.gap,
    ...(element.aspectRatio != null ? { aspectRatio: String(element.aspectRatio) } : {}),
  };

  switch (element.alignment) {
    case 'center':       layoutStyle.justifyContent = 'center'; break;
    case 'end':          layoutStyle.justifyContent = 'flex-end'; break;
    case 'spaceBetween': layoutStyle.justifyContent = 'space-between'; break;
    case 'spaceAround':  layoutStyle.justifyContent = 'space-around'; break;
    case 'spaceEvenly':  layoutStyle.justifyContent = 'space-evenly'; break;
    default:             layoutStyle.justifyContent = 'flex-start'; break;
  }

  switch (element.crossAlignment) {
    case 'center':  layoutStyle.alignItems = 'center'; break;
    case 'end':     layoutStyle.alignItems = 'flex-end'; break;
    case 'stretch': layoutStyle.alignItems = 'stretch'; break;
    default:        layoutStyle.alignItems = 'flex-start'; break;
  }

  return (
    <AtomicBox
      element={element}
      layoutStyle={layoutStyle}
      className={className}
      extraProps={accessibilityProps(element.accessibility) as Record<string, unknown>}
    >
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

      {element.badge && <AtomicBoxBadge badge={element.badge} onAction={onAction as (a: unknown) => void} />}

      {hasBreakpoint && className && (
        <style>{`
          @media (max-width: ${element.breakpoint}px) {
            .${className} {
              flex-direction: column !important;
            }
          }
        `}</style>
      )}
    </AtomicBox>
  );
}
