import React from 'react';
import type { Action } from '@sdui/models';
import type { AtomicElement } from './AtomicElement';

import { AtomicContainer } from './AtomicContainer';
import { AtomicText } from './AtomicText';
import { AtomicImage } from './AtomicImage';
import { AtomicButton } from './AtomicButton';
import { AtomicSpacer } from './AtomicSpacer';
import { AtomicDivider } from './AtomicDivider';
import { AtomicScrollContainer } from './AtomicScrollContainer';
import { AtomicConditional } from './AtomicConditional';
import { AtomicDisplayGrid } from './AtomicDisplayGrid';
import { AtomicSectionSlot } from './AtomicSectionSlot';

const MAX_TREE_DEPTH = 6;

export interface AtomicProps {
  element: AtomicElement;
  state: Record<string, unknown>;
  onAction: (action: Action) => void;
  depth?: number;
  onStateChange?: (key: string, value: unknown) => void;
  sectionSlotDepth?: number;
}

/**
 * AtomicRouter — routes an AtomicElement to the correct primitive renderer.
 *
 * A defensive depth guard prevents malformed payloads from causing deep DOM trees
 * or render loops. Server-side validation is the primary enforcement; this is a
 * safety net for stale caches or manual JSON authoring.
 */
export function AtomicRouter({ element, state, onAction, depth = 0, onStateChange, sectionSlotDepth = 0 }: AtomicProps): React.ReactElement | null {
  if (depth > MAX_TREE_DEPTH) {
    console.warn(`[AtomicRouter] Max tree depth (${MAX_TREE_DEPTH}) exceeded — skipping element: ${element.type}`);
    return null;
  }
  const childDepth = depth + 1;
  const childProps = { state, onAction, onStateChange, sectionSlotDepth };
  let rendered: React.ReactElement | null;
  switch (element.type) {
    case 'Container':
      rendered = <AtomicContainer element={element} {...childProps} depth={childDepth} />;
      break;
    case 'Text':
      rendered = <AtomicText element={element} state={state} onAction={onAction} />;
      break;
    case 'Image':
      rendered = <AtomicImage element={element} state={state} onAction={onAction} />;
      break;
    case 'Button':
      rendered = <AtomicButton element={element} state={state} onAction={onAction} />;
      break;
    case 'Spacer':
      rendered = <AtomicSpacer element={element} state={state} onAction={onAction} />;
      break;
    case 'Divider':
      rendered = <AtomicDivider element={element} state={state} onAction={onAction} />;
      break;
    case 'ScrollContainer':
      rendered = <AtomicScrollContainer element={element} {...childProps} depth={childDepth} />;
      break;
    case 'Conditional':
      rendered = <AtomicConditional element={element} {...childProps} depth={childDepth} />;
      break;
    case 'DisplayGrid':
      rendered = <AtomicDisplayGrid element={element} state={state} onAction={onAction} />;
      break;
    case 'SectionSlot':
      rendered = <AtomicSectionSlot element={element} {...childProps} />;
      break;
    default:
      console.debug(`[AtomicRouter] Unknown element type: ${element.type}`);
      return null;
  }

  // Outer margin is applied by a wrapper div so it sits outside the
  // primitive's own background, corner radius, and inner padding —
  // sibling-to-sibling spacing semantics, matching CSS `margin`.
  if (element.margin) {
    const m = element.margin;
    const marginStyle = {
      marginTop: m.top,
      marginRight: m.end,
      marginBottom: m.bottom,
      marginLeft: m.start,
      ...(element.opacity !== undefined ? { opacity: element.opacity } : {}),
    };
    return <div style={marginStyle}>{rendered}</div>;
  }

  if (element.opacity !== undefined && rendered) {
    return <div style={{ opacity: element.opacity }}>{rendered}</div>;
  }
  return rendered;
}
