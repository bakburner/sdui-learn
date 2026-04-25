import React from 'react';
import type { AtomicOverlay } from '@sdui/models';

import type { AtomicProps } from './AtomicRouter';
import { AtomicRouter } from './AtomicRouter';
import { AtomicBox } from './AtomicBox';
import { accessibilityProps } from '../../utils/accessibility';

export function AtomicOverlayContainer({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth }: AtomicProps): React.ReactElement | null {
  if (!element.base) {
    console.warn(`[AtomicOverlayContainer] Missing base element; skipping id=${element.id ?? '(none)'}`);
    return null;
  }

  return (
    <AtomicBox
      element={element}
      layoutStyle={{ position: 'relative' }}
      extraProps={accessibilityProps(element.accessibility) as Record<string, unknown>}
    >
      <AtomicRouter
        element={element.base}
        state={state}
        onAction={onAction}
        depth={depth}
        onStateChange={onStateChange}
        sectionSlotDepth={sectionSlotDepth}
      />
      {element.overlays?.map((overlay, index) => (
        <div key={overlay.element.id ?? index} style={overlayStyle(overlay)}>
          <AtomicRouter
            element={overlay.element}
            state={state}
            onAction={onAction}
            depth={depth}
            onStateChange={onStateChange}
            sectionSlotDepth={sectionSlotDepth}
          />
        </div>
      ))}
    </AtomicBox>
  );
}

function overlayStyle(overlay: AtomicOverlay): React.CSSProperties {
  const style: React.CSSProperties = {
    position: 'absolute',
    ...alignmentStyle(overlay.alignment ?? 'bottomCenter'),
  };

  const inset = overlay.inset;
  if (inset?.top != null) style.top = inset.top;
  if (inset?.end != null) style.right = inset.end;
  if (inset?.bottom != null) style.bottom = inset.bottom;
  if (inset?.start != null) style.left = inset.start;

  return style;
}

function alignmentStyle(alignment: string): React.CSSProperties {
  switch (alignment) {
    case 'topStart': return { top: 0, left: 0 };
    case 'topCenter': return { top: 0, left: '50%', transform: 'translateX(-50%)' };
    case 'topEnd': return { top: 0, right: 0 };
    case 'centerStart': return { top: '50%', left: 0, transform: 'translateY(-50%)' };
    case 'center': return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)' };
    case 'centerEnd': return { top: '50%', right: 0, transform: 'translateY(-50%)' };
    case 'bottomStart': return { bottom: 0, left: 0 };
    case 'bottomEnd': return { bottom: 0, right: 0 };
    case 'bottomCenter':
    default:
      return { bottom: 0, left: '50%', transform: 'translateX(-50%)' };
  }
}
