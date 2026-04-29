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
        <div key={overlay.element.id ?? index} style={overlayStyle(overlay, overlay.element)}>
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

function overlayStyle(overlay: AtomicOverlay, el?: Record<string, unknown>): React.CSSProperties {
  // Schema contract: `inset` is an OFFSET from the aligned base bounds —
  // not an absolute position. Anchor to the edges implied by `alignment`,
  // then add the matching inset value as the distance from each anchored
  // edge. Edges that aren't anchored by the alignment ignore their inset
  // (e.g. `bottomCenter` ignores `inset.top` since there's no top anchor).
  // This matches iOS (`ZStack(alignment) { ... }.padding(inset)`) and
  // Android (`Modifier.align(...)` + padding) behavior. The previous
  // implementation overrode `left/'50%'` with `inset.start`, which collapsed
  // a `bottomCenter` badge with `inset = 0/0/0/0` onto the corner and
  // produced a cropped triangle on story-circle rails.
  const alignment = overlay.alignment ?? 'bottomCenter';
  const inset = overlay.inset;
  const top = inset?.top ?? 0;
  const end = inset?.end ?? 0;
  const bottom = inset?.bottom ?? 0;
  const start = inset?.start ?? 0;

  const style: React.CSSProperties = { position: 'absolute' };

  switch (alignment) {
    case 'topStart':
      style.top = top;
      style.left = start;
      break;
    case 'topCenter':
      style.top = top;
      style.left = '50%';
      style.transform = 'translateX(-50%)';
      break;
    case 'topEnd':
      style.top = top;
      style.right = end;
      break;
    case 'centerStart':
      style.top = '50%';
      style.left = start;
      style.transform = 'translateY(-50%)';
      break;
    case 'center':
      style.top = '50%';
      style.left = '50%';
      style.transform = 'translate(-50%, -50%)';
      break;
    case 'centerEnd':
      style.top = '50%';
      style.right = end;
      style.transform = 'translateY(-50%)';
      break;
    case 'bottomStart':
      style.bottom = bottom;
      style.left = start;
      break;
    case 'bottomEnd':
      style.bottom = bottom;
      style.right = end;
      break;
    case 'bottomCenter':
    default:
      style.bottom = bottom;
      style.left = '50%';
      style.transform = 'translateX(-50%)';
      break;
  }

  // When the overlay element declares fillWidth/fillHeight, stretch the
  // absolutely-positioned wrapper so the child's percentage sizing resolves
  // against the full parent bounds (e.g. a gradient scrim that must span
  // the entire image width).
  if (el?.fillWidth) {
    style.left = start;
    style.right = end;
    style.width = undefined;
    style.transform = undefined;
  }
  if (el?.fillHeight) {
    style.top = top;
    style.bottom = bottom;
  }

  return style;
}
