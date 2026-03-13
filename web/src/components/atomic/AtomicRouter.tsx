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
  switch (element.type) {
    case 'Container':
      return <AtomicContainer element={element} {...childProps} depth={childDepth} />;
    case 'Text':
      return <AtomicText element={element} state={state} onAction={onAction} />;
    case 'Image':
      return <AtomicImage element={element} state={state} onAction={onAction} />;
    case 'Button':
      return <AtomicButton element={element} state={state} onAction={onAction} />;
    case 'Spacer':
      return <AtomicSpacer element={element} state={state} onAction={onAction} />;
    case 'Divider':
      return <AtomicDivider element={element} state={state} onAction={onAction} />;
    case 'ScrollContainer':
      return <AtomicScrollContainer element={element} {...childProps} depth={childDepth} />;
    case 'Conditional':
      return <AtomicConditional element={element} {...childProps} depth={childDepth} />;
    case 'DisplayGrid':
      return <AtomicDisplayGrid element={element} state={state} onAction={onAction} />;
    case 'SectionSlot':
      return <AtomicSectionSlot element={element} {...childProps} />;
    default:
      console.debug(`[AtomicRouter] Unknown element type: ${element.type}`);
      return null;
  }
}
